/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.workflow.handler.distribution;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WOH that retracts elements from an internal distribution channel and removes the reflective publication elements from
 * the media package.
 */
public class ConfigurableRetractWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurableRetractWorkflowOperationHandler.class);
  private static final String CHANNEL_ID_KEY = "channel-id";
  // service references
  private DownloadDistributionService distributionService;

  /** OSGi DI */
  void setDownloadDistributionService(DownloadDistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /**
   * Adds all of the {@link Publication}'s {@link MediaPackageElement}s that would normally have not been in the
   * {@link MediaPackage}.
   *
   * @param publication
   *          The {@link Publication} with the {@link MediaPackageElement}s to add.
   * @param mp
   *          The {@link MediaPackage} to add the {@link MediaPackageElement}s to.
   */
  private void addPublicationElementsToMediaPackage(Publication publication, MediaPackage mp) {
    for (Attachment attachment : publication.getAttachments()) {
      mp.add(attachment);
    }

    for (Catalog catalog : publication.getCatalogs()) {
      mp.add(catalog);
    }

    for (Track track : publication.getTracks()) {
      mp.add(track);
    }
  }

  /**
   * Retracts a {@link MediaPackageElement} from a download channel.
   *
   * @param channelId
   *          The id of the channel to remove the {@link MediaPackageElement} from.
   * @param element
   *          The {@link MediaPackageElement} to remove.
   * @param mp
   *          The {@link MediaPackage} that contains the {@link MediaPackageElement}.
   * @return The retraction {@link Job} that will retract the {@link MediaPackageElement}.
   * @throws WorkflowOperationException
   *           Thrown if unable to retract the {@link MediaPackageElement}.
   */
  private Job retractPublicationElement(String channelId, MediaPackageElement element, MediaPackage mp)
          throws WorkflowOperationException {
    try {
      return distributionService.retract(channelId, mp, element.getIdentifier());
    } catch (DistributionException e) {
      logger.error("Error while retracting element '{}' from channel '{}' of distribution '{}': {}", new Object[] {
              element.getIdentifier(), channelId, distributionService, getStackTrace(e) });
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Remove the {@link Publication}'s {@link MediaPackageElement}s from a given channel.
   *
   * @param channelId
   *          The channel to remove the {@link MediaPackageElement}s from.
   * @param publication
   *          The {@link Publication} that is being removed.
   * @param mp
   *          The {@link MediaPackage} that the {@link Publication} is part of.
   * @return A list of {@link Job}s that are all of the retract operations for the different {@link MediaPackageElement}s.
   * @throws WorkflowOperationException
   *           Thrown if unable to retract the {@link MediaPackageElement}s.
   */
  private int retractPublicationElements(String channelId, Publication publication, MediaPackage mp)
          throws WorkflowOperationException {
    // Add the publications to the mediapackage so that we can use the standard retract
    addPublicationElementsToMediaPackage(publication, mp);

    Set<String> elementIds = new HashSet<String>();

    List<Job> jobs = new ArrayList<Job>();
    for (Attachment attachment : publication.getAttachments()) {
      elementIds.add(attachment.getIdentifier());
    }
    for (Catalog catalog : publication.getCatalogs()) {
      elementIds.add(catalog.getIdentifier());
    }
    for (Track track : publication.getTracks()) {
      elementIds.add(track.getIdentifier());
    }

    if (elementIds.size() > 0) {
      Job job = null;
      try {
        job = distributionService.retract(channelId, mp, elementIds);
      } catch (DistributionException e) {
        logger.error("Error while retracting '{}' elements from channel '{}' of distribution '{}': {}", new Object[] {
                elementIds.size(), channelId, distributionService, getStackTrace(e) });
        throw new WorkflowOperationException(e);
      }
      // Wait until all retraction jobs have returned
      if (!waitForStatus(job).isSuccess()) {
        throw new WorkflowOperationException("One of the retraction jobs did not complete successfully");
      }
    } else {
      logger.debug("No publication elements were found for retraction");
    }

    return elementIds.size();
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    notNull(workflowInstance, "workflowInstance");

    final MediaPackage mp = workflowInstance.getMediaPackage();
    MediaPackage mediapackageWithPublicationElements = (MediaPackage) mp.clone();

    final String channelId = StringUtils.trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(
            CHANNEL_ID_KEY));
    if (StringUtils.isBlank((channelId))) {
      throw new WorkflowOperationException("Unable to publish this mediapackage as the configuration key "
              + CHANNEL_ID_KEY + " is missing. Unable to determine where to publish these elements.");
    }

    logger.info("Start unpublishing elements of media package '{}' from publication channel '{}'", mp, channelId);

    final List<Publication> publications = Stream.mk(mp.getPublications()).filter(new Fn<Publication, Boolean>() {
      @Override
      public Boolean ap(Publication a) {
        return channelId.equals(a.getChannel());
      }
    }).toList();
    int retractedElementsCount = 0;
    for (Publication publication : publications) {
      retractedElementsCount += retractPublicationElements(channelId, publication, mediapackageWithPublicationElements);
      mp.remove(publication);
    }

    logger.info("Successfully retracted {} publications and retracted {} elements from publication channel '{}'",
            new Object[] { publications.size(), retractedElementsCount, channelId });
    return createResult(mp, Action.CONTINUE);
  }

}
