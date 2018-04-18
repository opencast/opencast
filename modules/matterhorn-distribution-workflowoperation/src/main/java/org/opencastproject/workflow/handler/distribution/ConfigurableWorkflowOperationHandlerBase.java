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

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationException;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class of ConfigurablePublishWorkflerOperationHandler and ConfigurableRectractWorkflowOperationHandler.
 *
 * Both the ConfigurablePublishWorkflowOperationHandler and ConfigurableRetractWorkflowOperationHanlder are capable of
 * retracting publications created by the ConfigurablePublishWorkflowOperationHandler.
 * To avoid code duplication, this commonly used functionaly has been factored out into this class.
 */
public abstract class ConfigurableWorkflowOperationHandlerBase extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurableWorkflowOperationHandlerBase.class);

  abstract DownloadDistributionService getDistributionService();

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
    assert ((publication != null) && (mp != null));
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
   * Remove the {@link Publication}'s {@link MediaPackageElement}s from a given channel.
   *
   * @param channelId
   *          The channel to remove the {@link MediaPackageElement}s from.
   * @param publication
   *          The {@link Publication} that is being removed.
   * @param mp
   *          The {@link MediaPackage} that the {@link Publication} is part of.
   * @return the number of {@link MediaPackageElement}s that have been retracted
   * @throws WorkflowOperationException
   *           Thrown if unable to retract the {@link MediaPackageElement}s.
   */
  private int retractPublicationElements(String channelId, Publication publication, MediaPackage mp)
          throws WorkflowOperationException {
    assert ((channelId != null) && (publication != null) && (mp != null));
    MediaPackage mediapackageWithPublicationElements = (MediaPackage) mp.clone();

    // Add the publications to the mediapackage so that we can use the standard retract
    addPublicationElementsToMediaPackage(publication, mediapackageWithPublicationElements);

    Set<String> elementIds = new HashSet<>();

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
      logger.info("Retracting {} elements of media package {} from publication channel {}", elementIds.size(), mp,
              channelId);
      Job job = null;
      try {
        job = getDistributionService().retract(channelId, mediapackageWithPublicationElements, elementIds);
      } catch (DistributionException e) {
        logger.error("Error while retracting '{}' elements from channel '{}' of distribution '{}': {}",
                elementIds.size(), channelId, getDistributionService(), getStackTrace(e));
        throw new WorkflowOperationException("The retraction job did not complete successfully");
      }
      if (!waitForStatus(job).isSuccess()) {
        throw new WorkflowOperationException("The retraction job did not complete successfully");
      }
    } else {
      logger.debug("No publication elements were found for retraction");
    }

    return elementIds.size();
  }

  public List<Publication> getPublications(final MediaPackage mp, final String channelId) {
    assert ((mp != null) && (channelId != null));
    final List<Publication> publications = Stream.mk(mp.getPublications()).filter(new Fn<Publication, Boolean>() {
      @Override
      public Boolean apply(Publication a) {
        return channelId.equals(a.getChannel());
      }
    }).toList();
    assert (publications != null);
    return publications;
  }

  public void retract(MediaPackage mp, final String channelId) throws WorkflowOperationException {
    assert ((mp != null) && (channelId != null));

    final List<Publication> publications = getPublications(mp, channelId);

    if (publications.size() > 0) {
      int retractedElementsCount = 0;
      for (Publication publication : publications) {
        retractedElementsCount += retractPublicationElements(channelId, publication, mp);
        mp.remove(publication);
      }
      logger.info("Successfully retracted {} publications and retracted {} elements from publication channel '{}'",
              publications.size(), retractedElementsCount, channelId);
    } else {
      logger.info("No publications for channel {} found for media package {}", channelId, mp.getIdentifier());
    }
  }

}
