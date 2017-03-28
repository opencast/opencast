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
import org.opencastproject.util.IoSupport;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_FLAVOR_PROPERTY, "The source media file flavor.");
    CONFIG_OPTIONS.put(SOURCE_TAGS_PROPERTY, "Comma-separated tags of the source media files. "
            + "Any media that match " + SOURCE_FLAVOR_PROPERTY + " or " + SOURCE_TAGS_PROPERTY
            + " will be processed.");
    CONFIG_OPTIONS.put(TARGET_FLAVOR_PROPERTY, "The target waveform image flavor.");
    CONFIG_OPTIONS.put(TARGET_TAGS_PROPERTY, "The waveform image (comma separated) target tags.");
  }

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
   * org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
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
    logger.info("Start waveform workflow operation for mediapackage {}", mediaPackage.getIdentifier().compact());

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

    TrackSelector trackSelector = new TrackSelector();
    for (String flavor : asList(sourceFlavorProperty)) {
      trackSelector.addFlavor(flavor);
    }
    for (String tag : asList(sourceTagsProperty)) {
      trackSelector.addTag(tag);
    }
    Collection<Track> sourceTracks = trackSelector.select(mediaPackage, false);
    if (sourceTracks.isEmpty()) {
      logger.info("No tracks found in mediapackage {} with specified {} {}",
              mediaPackage.getIdentifier().compact(),
              SOURCE_FLAVOR_PROPERTY,
              sourceFlavorProperty);
      createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
    }

    List<Job> waveformJobs = new ArrayList<Job>(sourceTracks.size());
    for (Track sourceTrack : sourceTracks) {
      // Skip over track with no audio stream
      if (!sourceTrack.hasAudio()) {
        logger.info("Skipping waveform extraction of track {} since it has no audio", sourceTrack);
        continue;
      }
      try {
        // generate waveform
        logger.info("Create waveform job for track '{}' in mediapackage '{}'",
                sourceTrack.getIdentifier(), mediaPackage.getIdentifier().compact());

        Job waveformJob = waveformService.createWaveformImage(sourceTrack);
        waveformJobs.add(waveformJob);
      } catch (MediaPackageException | WaveformServiceException ex) {
        logger.error("Creating waveform extraction job for track '{}' in media package '{}' failed with error {}",
                sourceTrack.getIdentifier(), mediaPackage.getIdentifier().compact(), ex.getMessage());
      }
    }

    logger.info("Wait for waveform jobs for media package {}", mediaPackage.getIdentifier().compact());
    if (!waitForStatus(waveformJobs.toArray(new Job[waveformJobs.size()])).isSuccess()) {
      cleanupWorkspace(waveformJobs);
      throw new WorkflowOperationException(
              String.format("Waveform extraction jobs for media mapckage '%s' have not completed successfully",
                      mediaPackage.getIdentifier().compact()));
    }

    try {
      // copy waveform attachments into workspace and add them to the media package
      for (Job job : waveformJobs) {
        String jobPayload = job.getPayload();
        if (StringUtils.isNotEmpty(jobPayload)) {
          MediaPackageElement waveformMpe = null;
          File waveformFile = null;
          try {
            waveformMpe = MediaPackageElementParser.getFromXml(jobPayload);
            waveformFile = workspace.get(waveformMpe.getURI());
          } catch (MediaPackageException ex) {
            // unexpected job payload
            throw new WorkflowOperationException("Can't parse waveform attachment from job " + job.getId());
          } catch (NotFoundException ex) {
            throw new WorkflowOperationException("Waveform image file '" + waveformMpe.getURI() + "' not found", ex);
          } catch (IOException ex) {
            throw new WorkflowOperationException("Can't get workflow image file '" + waveformMpe.getURI()
                    + "' from workspace");
          }

          FileInputStream waveformInputStream = null;
          logger.info("Put waveform image file {} from media package {} to the media package work space",
                  waveformMpe.getURI(), mediaPackage.getIdentifier().compact());
          try {
            waveformInputStream = new FileInputStream(waveformFile);
            URI waveformWfrUri = workspace.put(mediaPackage.getIdentifier().compact(), waveformMpe.getIdentifier(),
                    "waveform.png", waveformInputStream);
            waveformMpe.setURI(waveformWfrUri);
          } catch (FileNotFoundException ex) {
            throw new WorkflowOperationException("Waveform image file " + waveformFile.getPath() + " not found", ex);
          } catch (IOException ex) {
            throw new WorkflowOperationException("Can't read just created waveform image file "
                    + waveformFile.getPath(), ex);
          } catch (IllegalArgumentException ex) {
            throw new WorkflowOperationException(ex);
          } finally {
            IoSupport.closeQuietly(waveformInputStream);
          }

          // set the waveform attachment flavor and add it to the mediapackage
          MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorProperty);
          if ("*".equals(targetFlavor.getType())) {
            targetFlavor = new MediaPackageElementFlavor(waveformMpe.getFlavor().getType(), targetFlavor.getSubtype());
          }
          if ("*".equals(targetFlavor.getSubtype())) {
            targetFlavor = new MediaPackageElementFlavor(targetFlavor.getType(), waveformMpe.getFlavor().getSubtype());
          }
          waveformMpe.setFlavor(targetFlavor);
          if (!StringUtils.isEmpty(targetTagsProperty)) {
            for (String tag : asList(targetTagsProperty)) {
              waveformMpe.addTag(tag);
            }
          }
          mediaPackage.add(waveformMpe);
        }
      }
    } finally {
      cleanupWorkspace(waveformJobs);
    }

    logger.info("Waveform workflow operation for mediapackage {} completed", mediaPackage.getIdentifier().compact());
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
            MediaPackageElement waveformMpe = MediaPackageElementParser.getFromXml(jobPayload);
            URI waveformUri = waveformMpe.getURI();
            workspace.delete(waveformUri);
          } catch (MediaPackageException ex) {
            // unexpected job payload
            logger.error("Can't parse waveform attachment from job {}", job.getId());
          } catch (NotFoundException ex) {
            // this is ok, because we want delete the file
          } catch (IOException ex) {
            logger.warn("Deleting waveform image file from workspace failed: {}", ex.getMessage());
            // this is ok, because workspace cleaner will remove old files if they exist
          }
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
