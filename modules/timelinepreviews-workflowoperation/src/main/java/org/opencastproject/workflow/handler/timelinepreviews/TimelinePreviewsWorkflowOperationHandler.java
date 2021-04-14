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
package org.opencastproject.workflow.handler.timelinepreviews;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.timelinepreviews.api.TimelinePreviewsException;
import org.opencastproject.timelinepreviews.api.TimelinePreviewsService;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Workflow operation for the timeline previews service.
 */
public class TimelinePreviewsWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TimelinePreviewsWorkflowOperationHandler.class);

  /** Source flavor configuration property name. */
  private static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";

  /** Source tags configuration property name. */
  private static final String SOURCE_TAGS_PROPERTY = "source-tags";

  /** Target flavor configuration property name. */
  private static final String TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** Target tags configuration property name. */
  private static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** Process first match only */
  private static final String PROCCESS_FIRST_MATCH = "process-first-match-only";

  /** Image size configuration property name. */
  private static final String IMAGE_SIZE_PROPERTY = "image-count";

  /** Default value for image size. */
  private static final int DEFAULT_IMAGE_SIZE = 10;

  /** The timeline previews service. */
  private TimelinePreviewsService timelinePreviewsService = null;

  /** The workspace service. */
  private Workspace workspace = null;

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering timeline previews workflow operation handler");
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   * org.opencastproject.job.api.JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    logger.info("Start timeline previews workflow operation for mediapackage {}", mediaPackage.getIdentifier().toString());

    String sourceFlavorProperty = StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration(SOURCE_FLAVOR_PROPERTY));
    String sourceTagsProperty = StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration(SOURCE_TAGS_PROPERTY));
    if (StringUtils.isEmpty(sourceFlavorProperty) && StringUtils.isEmpty(sourceTagsProperty)) {
      throw new WorkflowOperationException(String.format("Required property %s or %s not set",
              SOURCE_FLAVOR_PROPERTY, SOURCE_TAGS_PROPERTY));
    }

    String targetFlavorProperty = StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration(TARGET_FLAVOR_PROPERTY));
    if (targetFlavorProperty == null) {
      throw new WorkflowOperationException(String.format("Required property %s not set", TARGET_FLAVOR_PROPERTY));
    }

    String targetTagsProperty = StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration(TARGET_TAGS_PROPERTY));

    String imageSizeArg = StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration(IMAGE_SIZE_PROPERTY));
    int imageSize;
    if (imageSizeArg != null) {
      try {
        imageSize = Integer.parseInt(imageSizeArg);
      } catch (NumberFormatException e) {
        imageSize = DEFAULT_IMAGE_SIZE;
        logger.info("No valid integer given for property {}, using default value: {}",
                IMAGE_SIZE_PROPERTY, DEFAULT_IMAGE_SIZE);
      }
    } else {
      imageSize = DEFAULT_IMAGE_SIZE;
      logger.info("Property {} not set, using default value: {}", IMAGE_SIZE_PROPERTY, DEFAULT_IMAGE_SIZE);
    }

    boolean processOnlyOne = BooleanUtils.toBoolean(StringUtils.trimToNull(
            workflowInstance.getCurrentOperation().getConfiguration(PROCCESS_FIRST_MATCH)));

    TrackSelector trackSelector = new TrackSelector();
    for (String flavor : asList(sourceFlavorProperty)) {
      trackSelector.addFlavor(flavor);
    }
    for (String tag : asList(sourceTagsProperty)) {
      trackSelector.addTag(tag);
    }
    Collection<Track> sourceTracks = trackSelector.select(mediaPackage, true);
    if (sourceTracks.isEmpty()) {
      logger.info("No tracks found in mediapackage {} with specified {} {}", mediaPackage.getIdentifier().toString(),
              SOURCE_FLAVOR_PROPERTY,
              sourceFlavorProperty);
      createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
    }

    List<Job> timelinepreviewsJobs = new ArrayList<Job>(sourceTracks.size());
    for (Track sourceTrack : sourceTracks) {
      try {
        // generate timeline preview images
        logger.info("Create timeline previews job for track '{}' in mediapackage '{}'",
                sourceTrack.getIdentifier(), mediaPackage.getIdentifier().toString());

        Job timelinepreviewsJob = timelinePreviewsService.createTimelinePreviewImages(sourceTrack, imageSize);
        timelinepreviewsJobs.add(timelinepreviewsJob);

        if (processOnlyOne) {
          break;
        }

      } catch (MediaPackageException | TimelinePreviewsException ex) {
        logger.error("Creating timeline previews job for track '{}' in media package '{}' failed with error {}",
                sourceTrack.getIdentifier(), mediaPackage.getIdentifier().toString(), ex.getMessage());
      }
    }

    logger.info("Wait for timeline previews jobs for media package {}", mediaPackage.getIdentifier().toString());
    if (!waitForStatus(timelinepreviewsJobs.toArray(new Job[timelinepreviewsJobs.size()])).isSuccess()) {
      cleanupWorkspace(timelinepreviewsJobs);
      throw new WorkflowOperationException(
              String.format("Timeline previews jobs for media package '%s' have not completed successfully",
                      mediaPackage.getIdentifier().toString()));
    }


    try {
      // copy timeline previews attachments into workspace and add them to the media package
      for (Job job : timelinepreviewsJobs) {
        String jobPayload = job.getPayload();
        if (StringUtils.isNotEmpty(jobPayload)) {
          MediaPackageElement timelinePreviewsMpe = null;
          File timelinePreviewsFile = null;
          try {
            timelinePreviewsMpe = MediaPackageElementParser.getFromXml(jobPayload);
            timelinePreviewsFile = workspace.get(timelinePreviewsMpe.getURI());
          } catch (MediaPackageException ex) {
            // unexpected job payload
            throw new WorkflowOperationException("Can't parse timeline previews attachment from job " + job.getId());
          } catch (NotFoundException ex) {
            throw new WorkflowOperationException("Timeline preview images file '" + timelinePreviewsMpe.getURI()
                    + "' not found", ex);
          } catch (IOException ex) {
            throw new WorkflowOperationException("Can't get workflow image file '" + timelinePreviewsMpe.getURI()
                    + "' from workspace");
          }

          FileInputStream timelinePreviewsInputStream = null;
          logger.info("Put timeline preview images file {} from media package {} to the media package work space",
                  timelinePreviewsMpe.getURI(), mediaPackage.getIdentifier().toString());

          try {
            timelinePreviewsInputStream = new FileInputStream(timelinePreviewsFile);
            String fileName = FilenameUtils.getName(timelinePreviewsMpe.getURI().getPath());
            URI timelinePreviewsWfrUri = workspace.put(mediaPackage.getIdentifier().toString(),
                    timelinePreviewsMpe.getIdentifier(), fileName, timelinePreviewsInputStream);
            timelinePreviewsMpe.setURI(timelinePreviewsWfrUri);
          } catch (FileNotFoundException ex) {
            throw new WorkflowOperationException("Timeline preview images file " + timelinePreviewsFile.getPath()
                    + " not found", ex);
          } catch (IOException ex) {
            throw new WorkflowOperationException("Can't read just created timeline preview images file "
                    + timelinePreviewsFile.getPath(), ex);
          } catch (IllegalArgumentException ex) {
            throw new WorkflowOperationException(ex);
          } finally {
            IoSupport.closeQuietly(timelinePreviewsInputStream);
          }

          // set the timeline previews attachment flavor and add it to the mediapackage
          MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorProperty);
          if ("*".equals(targetFlavor.getType())) {
            targetFlavor = new MediaPackageElementFlavor(timelinePreviewsMpe.getFlavor().getType(), targetFlavor.getSubtype());
          }
          if ("*".equals(targetFlavor.getSubtype())) {
            targetFlavor = new MediaPackageElementFlavor(targetFlavor.getType(), timelinePreviewsMpe.getFlavor().getSubtype());
          }
          timelinePreviewsMpe.setFlavor(targetFlavor);
          if (!StringUtils.isEmpty(targetTagsProperty)) {
            for (String tag : asList(targetTagsProperty)) {
              timelinePreviewsMpe.addTag(tag);
            }
          }

          mediaPackage.add(timelinePreviewsMpe);
        }
      }
    } finally {
      cleanupWorkspace(timelinepreviewsJobs);
    }


    logger.info("Timeline previews workflow operation for mediapackage {} completed", mediaPackage.getIdentifier().toString());
    return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
  }

  /**
   * Remove all files created by the given jobs
   * @param jobs
   */
  private void cleanupWorkspace(List<Job> jobs) {
    for (Job job : jobs) {
        String jobPayload = job.getPayload();
        if (StringUtils.isNotEmpty(jobPayload)) {
          try {
            MediaPackageElement timelinepreviewsMpe = MediaPackageElementParser.getFromXml(jobPayload);
            URI timelinepreviewsUri = timelinepreviewsMpe.getURI();
            workspace.delete(timelinepreviewsUri);
          } catch (MediaPackageException ex) {
            // unexpected job payload
            logger.error("Can't parse timeline previews attachment from job {}", job.getId());
          } catch (NotFoundException ex) {
            // this is ok, because we want delete the file
          } catch (IOException ex) {
            logger.warn("Deleting timeline previews image file from workspace failed: {}", ex.getMessage());
            // this is ok, because workspace cleaner will remove old files if they exist
          }
        }
      }
  }

  public void setTimelinePreviewsService(TimelinePreviewsService timelinePreviewsService) {
    this.timelinePreviewsService = timelinePreviewsService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
