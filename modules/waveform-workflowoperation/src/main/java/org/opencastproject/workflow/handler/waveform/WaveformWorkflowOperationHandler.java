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
package org.opencastproject.workflow.handler.waveform;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.waveform.api.WaveformService;
import org.opencastproject.waveform.api.WaveformServiceException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Workflow operation for the waveform service.
 */
public class WaveformWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(WaveformWorkflowOperationHandler.class);

  /** Source flavor configuration property name. */
  private static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";

  /** Source tags configuration property name. */
  private static final String SOURCE_TAGS_PROPERTY = "source-tags";

  /** Target flavor configuration property name. */
  private static final String TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** Target tags configuration property name. */
  private static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** The waveform service. */
  private WaveformService waveformService = null;

  /** The workspace service. */
  private Workspace workspace = null;

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering waveform workflow operation handler");
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
    logger.info("Start waveform workflow operation for mediapackage {}", mediaPackage);

    try {

      String sourceFlavorProperty = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(SOURCE_FLAVOR_PROPERTY));
      String sourceTagsProperty = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(SOURCE_TAGS_PROPERTY));
      if (StringUtils.isEmpty(sourceFlavorProperty) && StringUtils.isEmpty(sourceTagsProperty)) {
        throw new WorkflowOperationException(
                String.format("Required property %s or %s not set", SOURCE_FLAVOR_PROPERTY, SOURCE_TAGS_PROPERTY));
      }

      String targetFlavorProperty = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(TARGET_FLAVOR_PROPERTY));
      if (targetFlavorProperty == null) {
        throw new WorkflowOperationException(String.format("Required property %s not set", TARGET_FLAVOR_PROPERTY));
      }

      String targetTagsProperty = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(TARGET_TAGS_PROPERTY));

      TrackSelector trackSelector = new TrackSelector();
      for (String flavor : asList(sourceFlavorProperty)) {
        trackSelector.addFlavor(flavor);
      }
      for (String tag : asList(sourceTagsProperty)) {
        trackSelector.addTag(tag);
      }
      Collection<Track> sourceTracks = trackSelector.select(mediaPackage, false);
      if (sourceTracks.isEmpty()) {
        logger.info("No tracks found in mediapackage {} with specified {} = {}", mediaPackage, SOURCE_FLAVOR_PROPERTY,
                sourceFlavorProperty);
        return createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
      }

      List<Job> waveformJobs = new ArrayList<>(sourceTracks.size());
      for (Track sourceTrack : sourceTracks) {
        // Skip over track with no audio stream
        if (!sourceTrack.hasAudio()) {
          logger.info("Skipping waveform extraction of track {} since it has no audio", sourceTrack.getIdentifier());
          continue;
        }
        try {
          // generate waveform
          logger.info("Creating waveform extraction job for track '{}' in mediapackage '{}'", sourceTrack.getIdentifier(), mediaPackage);

          Job waveformJob = waveformService.createWaveformImage(sourceTrack);
          waveformJobs.add(waveformJob);
        } catch (MediaPackageException | WaveformServiceException e) {
          logger.error("Creating waveform extraction job for track '{}' in media package '{}' failed", sourceTrack.getIdentifier(), mediaPackage, e);
        }
      }

      logger.debug("Waiting for waveform jobs for media package {}", mediaPackage);
      if (!waitForStatus(waveformJobs.toArray(new Job[waveformJobs.size()])).isSuccess()) {
        throw new WorkflowOperationException(String.format("Waveform extraction jobs for media package '%s' have not completed successfully",
                mediaPackage.getIdentifier()));
      }

      // copy waveform attachments into workspace and add them to the media package
      for (Job job : waveformJobs) {
        String jobPayload = job.getPayload();
        if (StringUtils.isEmpty(jobPayload)) {
          continue;
        }
        MediaPackageElement waveformMpe = null;
        try {
          waveformMpe = MediaPackageElementParser.getFromXml(jobPayload);
          URI newURI = workspace.moveTo(waveformMpe.getURI(), mediaPackage.getIdentifier().toString(), waveformMpe.getIdentifier(),
                  "waveform.png");
          waveformMpe.setURI(newURI);
        } catch (MediaPackageException ex) {
          // unexpected job payload
          throw new WorkflowOperationException("Can't parse waveform attachment from job " + job.getId());
        } catch (NotFoundException ex) {
          throw new WorkflowOperationException("Waveform image file '" + waveformMpe.getURI() + "' not found", ex);
        } catch (IOException ex) {
          throw new WorkflowOperationException("Can't get workflow image file '" + waveformMpe.getURI() + "' from workspace");
        }

        // set the waveform attachment flavor and add it to the media package
        MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorProperty);
        if ("*".equals(targetFlavor.getType())) {
          targetFlavor = new MediaPackageElementFlavor(waveformMpe.getFlavor().getType(), targetFlavor.getSubtype());
        }
        if ("*".equals(targetFlavor.getSubtype())) {
          targetFlavor = new MediaPackageElementFlavor(targetFlavor.getType(), waveformMpe.getFlavor().getSubtype());
        }
        waveformMpe.setFlavor(targetFlavor);
        for (String tag : asList(targetTagsProperty)) {
          waveformMpe.addTag(tag);
        }
        mediaPackage.add(waveformMpe);
      }

      logger.info("Waveform workflow operation for mediapackage {} completed", mediaPackage);
      return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);

    } finally {
      try {
        workspace.cleanup(mediaPackage.getIdentifier(), true);
      } catch (IOException e) {
        throw new WorkflowOperationException(e);
      }
    }
  }

  public void setWaveformService(WaveformService waveformService) {
    this.waveformService = waveformService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
