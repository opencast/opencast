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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowOperationTagUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The <tt>prepare media</tt> operation will make sure that media where audio and video track come in separate files
 * will be muxed prior to further processing.
 */
public class PrepareAVWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);
  private static final String QUESTION_MARK = "?";

  /** Name of the 'encode to a/v prepared copy' encoding profile */
  public static final String PREPARE_AV_PROFILE = "av.prepared";

  /** Name of the muxing encoding profile */
  public static final String MUX_AV_PROFILE = "mux-av.prepared";

  /** Name of the 'encode to audio only prepared copy' encoding profile */
  public static final String PREPARE_AONLY_PROFILE = "audio-only.prepared";

  /** Name of the 'encode to video only prepared copy' encoding profile */
  public static final String PREPARE_VONLY_PROFILE = "video-only.prepared";

  /** Name of the 'rewrite' configuration key */
  public static final String OPT_REWRITE = "rewrite";

  /** Name of audio muxing configuration key */
  public static final String OPT_AUDIO_MUXING_SOURCE_FLAVORS = "audio-muxing-source-flavors";

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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    try {
      logger.debug("Running a/v muxing workflow operation on workflow {}", workflowInstance.getId());
      return mux(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Merges audio and video track of the selected flavor and adds it to the media package. If there is nothing to mux, a
   * new track with the target flavor is created (pointing to the original url).
   *
   * @param src
   *          The source media package
   * @param operation
   *          the mux workflow operation
   * @return the operation result containing the updated mediapackage
   * @throws EncoderException
   *           if encoding fails
   * @throws IOException
   *           if read/write operations from and to the workspace fail
   * @throws NotFoundException
   *           if the workspace does not contain the requested element
   */
  private WorkflowOperationResult mux(MediaPackage src, WorkflowOperationInstance operation) throws EncoderException,
          WorkflowOperationException, NotFoundException, MediaPackageException, IOException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    // Read the configuration properties
    String sourceFlavorName = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavorName = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String muxEncodingProfileName = StringUtils.trimToNull(operation.getConfiguration("mux-encoding-profile"));
    String audioVideoEncodingProfileName = StringUtils.trimToNull(operation.getConfiguration("audio-video-encoding-profile"));
    String videoOnlyEncodingProfileName = StringUtils.trimToNull(operation.getConfiguration("video-encoding-profile"));
    String audioOnlyEncodingProfileName = StringUtils.trimToNull(operation.getConfiguration("audio-encoding-profile"));

    final WorkflowOperationTagUtil.TagDiff tagDiff = WorkflowOperationTagUtil.createTagDiff(targetTrackTags);

    // Make sure the source flavor is properly set
    if (sourceFlavorName == null)
      throw new IllegalStateException("Source flavor must be specified");
    MediaPackageElementFlavor sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorName);

    // Make sure the target flavor is properly set
    if (targetTrackFlavorName == null)
      throw new IllegalStateException("Target flavor must be specified");
    MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetTrackFlavorName);

    // Reencode when there is no need for muxing?
    boolean rewrite = true;
    if (StringUtils.trimToNull(operation.getConfiguration(OPT_REWRITE)) != null) {
      rewrite = Boolean.parseBoolean(operation.getConfiguration(OPT_REWRITE));
    }

    String audioMuxingSourceFlavors = StringUtils.trimToNull(operation.getConfiguration(OPT_AUDIO_MUXING_SOURCE_FLAVORS));

    // Select those tracks that have matching flavors
    Track[] tracks = mediaPackage.getTracks(sourceFlavor);

    Track audioTrack = null;
    Track videoTrack = null;

    switch (tracks.length) {
      case 0:
        logger.info("No audio/video tracks with flavor '{}' found to prepare", sourceFlavor);
        return createResult(mediaPackage, Action.CONTINUE);
      case 1:
        videoTrack = tracks[0];
        if (!tracks[0].hasAudio() && tracks[0].hasVideo() && (audioMuxingSourceFlavors != null)) {
          audioTrack = findAudioTrack(tracks[0], mediaPackage, audioMuxingSourceFlavors);
        } else {
          audioTrack = tracks[0];
        }
        break;
      case 2:
        for (Track track : tracks) {
          if (track.hasAudio() && !track.hasVideo()) {
            audioTrack = track;
          } else if (!track.hasAudio() && track.hasVideo()) {
            videoTrack = track;
          } else {
            throw new WorkflowOperationException("Multiple tracks with competing audio/video streams and flavor '"
                    + sourceFlavor + "' found");
          }
        }
        break;
      default:
        logger.error("More than two tracks with flavor {} found. No idea what we should be doing", sourceFlavor);
        throw new WorkflowOperationException("More than two tracks with flavor '" + sourceFlavor + "' found");
    }

    Job job = null;
    Track composedTrack = null;

    // Make sure we have a matching combination
    if (audioTrack == null && videoTrack != null) {
      if (rewrite) {
        logger.info("Encoding video only track {} to prepared version", videoTrack);
        if (videoOnlyEncodingProfileName == null)
          videoOnlyEncodingProfileName = PREPARE_VONLY_PROFILE;
        // Find the encoding profile to make sure the given profile exists
        EncodingProfile profile = composerService.getProfile(videoOnlyEncodingProfileName);
        if (profile == null)
        throw new IllegalStateException("Encoding profile '" + videoOnlyEncodingProfileName + "' was not found");
        composedTrack = prepare(videoTrack, mediaPackage, videoOnlyEncodingProfileName);
      } else {
        composedTrack = (Track) videoTrack.clone();
        composedTrack.setIdentifier(null);
        mediaPackage.add(composedTrack);
      }
    } else if (videoTrack == null && audioTrack != null) {
      if (rewrite) {
        logger.info("Encoding audio only track {} to prepared version", audioTrack);
        if (audioOnlyEncodingProfileName == null)
          audioOnlyEncodingProfileName = PREPARE_AONLY_PROFILE;
        // Find the encoding profile to make sure the given profile exists
        EncodingProfile profile = composerService.getProfile(audioOnlyEncodingProfileName);
        if (profile == null)
        throw new IllegalStateException("Encoding profile '" + audioOnlyEncodingProfileName + "' was not found");
        composedTrack = prepare(audioTrack, mediaPackage, audioOnlyEncodingProfileName);
      } else {
        composedTrack = (Track) audioTrack.clone();
        composedTrack.setIdentifier(null);
        mediaPackage.add(composedTrack);
      }
    } else if (audioTrack == videoTrack) {
      if (rewrite) {
        logger.info("Encoding audiovisual track {} to prepared version", videoTrack);
        if (audioVideoEncodingProfileName == null)
          audioVideoEncodingProfileName = PREPARE_AV_PROFILE;
        // Find the encoding profile to make sure the given profile exists
        EncodingProfile profile = composerService.getProfile(audioVideoEncodingProfileName);
        if (profile == null)
        throw new IllegalStateException("Encoding profile '" + audioVideoEncodingProfileName + "' was not found");
        composedTrack = prepare(videoTrack, mediaPackage, audioVideoEncodingProfileName);
      } else {
        composedTrack = (Track) videoTrack.clone();
        composedTrack.setIdentifier(null);
        mediaPackage.add(composedTrack);
      }
    } else {
      logger.info("Muxing audio and video only track {} to prepared version", videoTrack);

      if (audioTrack.hasVideo()) {
        logger.info("Stripping video from track {}", audioTrack);
        audioTrack = prepare(audioTrack, null, PREPARE_AONLY_PROFILE);
      }

      if (muxEncodingProfileName == null)
        muxEncodingProfileName = MUX_AV_PROFILE;

      // Find the encoding profile
      EncodingProfile profile = composerService.getProfile(muxEncodingProfileName);
      if (profile == null)
      throw new IllegalStateException("Encoding profile '" + muxEncodingProfileName + "' was not found");

      job = composerService.mux(videoTrack, audioTrack, profile.getIdentifier());
      if (!waitForStatus(job).isSuccess()) {
        throw new WorkflowOperationException("Muxing video track " + videoTrack + " and audio track " + audioTrack
                + " failed");
      }
      composedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
      mediaPackage.add(composedTrack);
      String fileName = getFileNameFromElements(videoTrack, composedTrack);
      composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
              composedTrack.getIdentifier(), fileName));
    }

    long timeInQueue = 0;
    if (job != null) {
      // add this receipt's queue time to the total
      timeInQueue = job.getQueueTime();
    }

    // Update the track's flavor
    composedTrack.setFlavor(targetFlavor);
    logger.debug("Composed track has flavor '{}'", composedTrack.getFlavor());

    WorkflowOperationTagUtil.applyTagDiff(tagDiff, composedTrack);
    return createResult(mediaPackage, Action.CONTINUE, timeInQueue);
  }

  /**
   * Prepares a video track. If the mediapackage is specified, the prepared track will be added to it.
   *
   * @param videoTrack
   *          the track containing the video
   * @param mediaPackage
   *          the mediapackage
   * @return the rewritten track
   * @throws WorkflowOperationException
   * @throws NotFoundException
   * @throws IOException
   * @throws EncoderException
   * @throws MediaPackageException
   */
  private Track prepare(Track videoTrack, MediaPackage mediaPackage, String encodingProfile)
          throws WorkflowOperationException, NotFoundException, IOException, EncoderException, MediaPackageException {
    Track composedTrack = null;
    logger.info("Encoding video only track {} to prepared version", videoTrack);
    Job job = composerService.encode(videoTrack, encodingProfile);
    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException("Rewriting container for video track " + videoTrack + " failed");
    }
    composedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
    if (mediaPackage != null) {
      mediaPackage.add(composedTrack);
      String fileName = getFileNameFromElements(videoTrack, composedTrack);

      // Note that the composed track must have an ID before being moved to the mediapackage in the working file
      // repository. This ID is generated when the track is added to the mediapackage. So the track must be added
      // to the mediapackage before attempting to move the file.
      composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
              composedTrack.getIdentifier(), fileName));
    }
    return composedTrack;
  }

  /**
   * Finds a suitable audio track from the mediapackage by scanning a source flavor sequence
   *
   * @param videoTrack
   *          the video track
   * @param mediaPackage
   *          the mediapackage
   * @param audioMuxingSourceFlavors
   *          sequence of source flavors where an audio track should be searched for
   * @return the found audio track
   */
  private Track findAudioTrack(Track videoTrack, MediaPackage mediaPackage, String audioMuxingSourceFlavors) {

    if (audioMuxingSourceFlavors != null) {
      String type;
      String subtype;
      for (String flavorStr : audioMuxingSourceFlavors.split("[\\s,]")) {
        if (!flavorStr.isEmpty()) {
          MediaPackageElementFlavor flavor = null;
          try {
            flavor = MediaPackageElementFlavor.parseFlavor(flavorStr);
          } catch (IllegalArgumentException e) {
            logger.error("The parameter {} contains an invalid flavor: {}", OPT_AUDIO_MUXING_SOURCE_FLAVORS, flavorStr);
            throw e;
          }
          type = (QUESTION_MARK.equals(flavor.getType())) ? videoTrack.getFlavor().getType() : flavor.getType();
          subtype = (QUESTION_MARK.equals(flavor.getSubtype())) ? videoTrack.getFlavor().getSubtype() : flavor.getSubtype();
          // Recreate the (possibly) modified flavor
          flavor = new MediaPackageElementFlavor(type, subtype);
          for (Track track : mediaPackage.getTracks(flavor)) {
            if (track.hasAudio()) {
              logger.info("Audio muxing found audio source {} with flavor {}", track, track.getFlavor());
              return track;
            }
          }
        }
      }
    }
    return null;
  }

}
