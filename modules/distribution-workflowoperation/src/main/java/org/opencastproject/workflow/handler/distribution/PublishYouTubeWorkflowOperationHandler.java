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

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.publication.api.YouTubePublicationService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * The workflow definition for handling "publish" operations
 */
public class PublishYouTubeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PublishYouTubeWorkflowOperationHandler.class);

  /** The publication service */
  private YouTubePublicationService publicationService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param publicationService
   *          the publication service
   */
  public void setPublicationService(YouTubePublicationService publicationService) {
    this.publicationService = publicationService;
  }

  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running youtube publication workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
        Configuration.many, Configuration.many, Configuration.none, Configuration.none);
    List<String> sourceTags = tagsAndFlavors.getSrcTags();
    List<MediaPackageElementFlavor> sourceFlavors = tagsAndFlavors.getSrcFlavors();

    AbstractMediaPackageElementSelector<MediaPackageElement> elementSelector;

    if (sourceTags == null && sourceFlavors == null) {
      logger.warn("No tags or flavor have been specified");
      return createResult(mediaPackage, Action.CONTINUE);
    }
    elementSelector = new SimpleElementSelector();

    if (!sourceFlavors.isEmpty()) {
      for (MediaPackageElementFlavor flavor : sourceFlavors) {
        elementSelector.addFlavor(flavor);
      }
    }
    if (!sourceTags.isEmpty()) {
      for (String tag : sourceTags) {
        elementSelector.addTag(tag);
      }
    }

    try {
      // Look for elements matching the tag
      final Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, true);
      if (elements.size() > 1) {
        logger.warn("More than one element has been found for publishing to youtube: {}", elements);
        return createResult(mediaPackage, Action.SKIP);
      }

      if (elements.size() < 1) {
        logger.info("No mediapackage element was found for publishing");
        return createResult(mediaPackage, Action.CONTINUE);
      }

      Job youtubeJob;
      try {
        Track track = mediaPackage.getTrack(elements.iterator().next().getIdentifier());
        youtubeJob = publicationService.publish(mediaPackage, track);
      } catch (PublicationException e) {
        throw new WorkflowOperationException(e);
      }

      // Wait until the youtube publication job has returned
      if (!waitForStatus(youtubeJob).isSuccess()) {
        throw new WorkflowOperationException("The youtube publication jobs did not complete successfully");
      }

      // All the jobs have passed
      Job job = serviceRegistry.getJob(youtubeJob.getId());

      // If there is no payload, then the item has not been published.
      if (job.getPayload() == null) {
        logger.warn("Publish to youtube failed, no payload from publication job: {}", job);
        return createResult(mediaPackage, Action.CONTINUE);
      }

      Publication newElement = null;
      try {
        newElement = (Publication) MediaPackageElementParser.getFromXml(job.getPayload());
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException(e);
      }

      if (newElement == null) {
        logger.warn(
            "Publication to youtube failed, unable to parse the payload '{}' from job '{}' to a mediapackage element",
            job.getPayload(), job);
        return createResult(mediaPackage, Action.CONTINUE);
      }
      mediaPackage.add(newElement);

      logger.debug("Publication to youtube operation completed");
    } catch (Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException) e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }
}
