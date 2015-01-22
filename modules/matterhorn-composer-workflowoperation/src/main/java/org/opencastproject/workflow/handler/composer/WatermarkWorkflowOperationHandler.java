/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.workflow.handler;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class WatermarkWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WatermarkWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a video source input");
    CONFIG_OPTIONS.put("watermark", "The path of the image used as a watermark");
    CONFIG_OPTIONS.put("encoding-profile", "The encoding profile to use");
    CONFIG_OPTIONS.put("target-flavor", "The flavor to apply to the encoded file");
    CONFIG_OPTIONS.put("target-tags", "The tags to apply to the encoded file");
  }

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the local composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
    logger.debug("Running watermark workflow operation on workflow {}", workflowInstance.getId());

    try {
      return watermark(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   *
   * @param src
   *          The source media package
   * @param operation
   *          the current workflow operation
   * @return the operation result containing the updated media package
   * @throws EncoderException
   *           if encoding fails
   * @throws WorkflowOperationException
   *           if errors occur during processing
   * @throws IOException
   *           if the workspace operations fail
   * @throws NotFoundException
   *           if the workspace doesn't contain the requested file
   */
  private WorkflowOperationResult watermark(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    // Read the configuration properties
    String sourceFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String watermark = StringUtils.trimToNull(operation.getConfiguration("watermark"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));

    if (sourceFlavor == null)
      throw new IllegalStateException("Source flavor must be specified");

    if (watermark == null)
      throw new IllegalStateException("Watermark image must be specified");


    // Find the encoding profile
    EncodingProfile profile = composerService.getProfile(encodingProfileName);
    if (profile == null) {
      throw new IllegalStateException("Encoding profile '" + encodingProfileName + "' was not found");
    }

    // Depending on the input type of the profile and the configured flavors and
    // tags, make sure we have the required tracks:
    Track[] tracks = mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(sourceFlavor));

    // Did we get the set of tracks that we need?
    if (tracks.length == 0) {
      logger.info("Skipping encoding of media package to '{}': no suitable input tracks found", profile);
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Encode all found tracks
    long totalTimeInQueue = 0;
    for (Track t : tracks) {

      // Check if the track supports the output type of the profile
      MediaType outputType = profile.getOutputType();
      if (outputType.equals(MediaType.Visual) && !t.hasVideo()) {
        logger.info("Skipping encoding of '{}', since it lacks a video stream", t);
        continue;
      }

      logger.info("Encoding track {} using encoding profile '{}'", t, profile);

      // Start encoding and wait for the result
      Job job = composerService.watermark(t, watermark, profile.getIdentifier());
      if (!waitForStatus(job).isSuccess()) {
        throw new WorkflowOperationException("Watermarking failed");
      }

      Track composedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());

      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();

      updateTrackMetadata(composedTrack, operation, profile);

      // store new tracks to mediaPackage
      mediaPackage.addDerived(composedTrack, t);
      String fileName = getFileNameFromElements(t, composedTrack);
      composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
              composedTrack.getIdentifier(), fileName));
    }

    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
    logger.debug("Watermarking operation completed");
    return result;
  }

  // Update the newly composed track with metadata
  private void updateTrackMetadata(Track composedTrack, WorkflowOperationInstance operation, EncodingProfile profile) {
    // Read the configuration properties
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));

    if (composedTrack == null)
      throw new IllegalStateException("unable to retrieve watermarked track");

    // Add the flavor, either from the operation configuration or from the
    // composer
    if (targetTrackFlavor != null)
      composedTrack.setFlavor(MediaPackageElementFlavor.parseFlavor(targetTrackFlavor));
    logger.debug("Composed track has flavor '{}'", composedTrack.getFlavor());

    // Set the mimetype
    if (profile.getMimeType() != null)
      composedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

    // Add tags
    List<String> targetTags = asList(targetTrackTags);
    for (String tag : targetTags) {
      logger.trace("Tagging composed track with '{}'", tag);
      composedTrack.addTag(tag);
    }
  }


}
