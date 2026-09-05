/*
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
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The workflow definition for handling "publish" operations
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=YouTube Publication Workflow Handler",
        "workflow.operation=publish-youtube"
    }
)
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
  @Reference
  public void setPublicationService(YouTubePublicationService publicationService) {
    this.publicationService = publicationService;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
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

    AbstractMediaPackageElementSelector<MediaPackageElement> videoSelector;

    if (sourceTags == null && sourceFlavors == null) {
      logger.warn("No tags or flavor have been specified");
      return createResult(mediaPackage, Action.CONTINUE);
    }
    videoSelector = new SimpleElementSelector();

    if (!sourceFlavors.isEmpty()) {
      for (MediaPackageElementFlavor flavor : sourceFlavors) {
        videoSelector.addFlavor(flavor);
      }
    }
    if (!sourceTags.isEmpty()) {
      for (String tag : sourceTags) {
        videoSelector.addTag(tag);
      }
    }

    // Caption configuration
    List<String> captionTags = asList(StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration("caption-tags")));
    List<MediaPackageElementFlavor> captionFlavors = new ArrayList<>();
    List<String> captionFlavorStrings = asList(StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration("caption-flavors")));
    for (String flavorString : captionFlavorStrings) {
      try {
        captionFlavors.add(MediaPackageElementFlavor.parseFlavor(flavorString));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(flavorString + " is not a valid flavor!");
      }
    }
    AbstractMediaPackageElementSelector<MediaPackageElement> captionSelector = new SimpleElementSelector();
    for (MediaPackageElementFlavor flavor : captionFlavors) {
      captionSelector.addFlavor(flavor);
    }
    for (String tag : captionTags) {
      captionSelector.addTag(tag);
    }

    try {
      // Look for elements matching the tag
      final Collection<MediaPackageElement> videoElements = videoSelector.select(mediaPackage, true);
      if (videoElements.size() > 1) {
        throw new WorkflowOperationException("More than one element has been found for publishing to youtube: "
            + videoElements);
      }

      if (videoElements.size() < 1) {
        logger.info("No mediapackage element was found for publishing");
        return createResult(mediaPackage, Action.SKIP);
      }

      Collection<MediaPackageElement> captionElements = captionSelector.select(mediaPackage, true);

      Job youtubeJob;
      try {
        Track video = mediaPackage.getTrack(videoElements.iterator().next().getIdentifier());
        List<Track> captions = new ArrayList<>();
        for (MediaPackageElement caption : captionElements) {
          captions.add(mediaPackage.getTrack(caption.getIdentifier()));
        }
        youtubeJob = publicationService.publish(mediaPackage, video, captions);
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
        throw new WorkflowOperationException("Publish to youtube failed, no payload from publication job: "
            + job.getId());
      }

      Publication newElement = null;
      try {
        newElement = (Publication) MediaPackageElementParser.getFromXml(job.getPayload());
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException(e);
      }

      if (newElement == null) {
        throw new WorkflowOperationException(String.format(
            "Publication to youtube failed, unable to parse the payload '%s' from job '%d' to a mediapackage element",
            job.getPayload(), job.getId()));
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
