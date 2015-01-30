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
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The <tt>prepare media</tt> operation will make sure that media where audio and video track come in separate files
 * will be muxed prior to further processing.
 */
public class PrepareAVWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);
  private static final String PLUS = "+";
  private static final String MINUS = "-";

  /** Name of the 'encode to a/v work copy' encoding profile */
  public static final String PREPARE_AV_PROFILE = "av.work";

  /** Name of the muxing encoding profile */
  public static final String MUX_AV_PROFILE = "mux-av.work";

  /** Name of the 'encode to audio only work copy' encoding profile */
  public static final String PREPARE_AONLY_PROFILE = "audio-only.work";

  /** Name of the 'encode to video only work copy' encoding profile */
  public static final String PREPARE_VONLY_PROFILE = "video-only.work";

  /** Name of the 'rewrite' configuration key */
  public static final String OPT_REWRITE = "rewrite";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a video source input");
    CONFIG_OPTIONS.put("promiscuous-muxing", "If there is no matching flavor to mux, try other flavors as well");
    CONFIG_OPTIONS.put("encoding-profile", "The encoding profile to use (default is 'mux-av.http')");
    CONFIG_OPTIONS.put("target-flavor", "The flavor to apply to the encoded file");
    CONFIG_OPTIONS.put(OPT_REWRITE, "Indicating whether the container for audio and video tracks should be rewritten");
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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running a/v muxing workflow operation on workflow {}", workflowInstance.getId());
    try {
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
    boolean promiscuousMuxing = "true".equalsIgnoreCase(StringUtils.trimToEmpty(operation
            .getConfiguration("promiscuous-audio-muxing")));

    String[] targetTags = StringUtils.split(targetTrackTags, ",");

    List<String> removeTags = new ArrayList<String>();
    List<String> addTags = new ArrayList<String>();
    List<String> overrideTags = new ArrayList<String>();

    if (targetTags != null) {
      for (String tag : targetTags) {
        if (tag.startsWith(MINUS)) {
          removeTags.add(tag);
        } else if (tag.startsWith(PLUS)) {
          addTags.add(tag);
        } else {
          overrideTags.add(tag);
        }
      }
    }

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

    // Select those tracks that have matching flavors
    Track[] tracks = mediaPackage.getTracks(sourceFlavor);

    Track audioTrack = null;
    Track videoTrack = null;

    switch (tracks.length) {
      case 0:
        logger.info("No audio/video tracks with flavor '{}' found to prepare", sourceFlavor);
        return createResult(mediaPackage, Action.CONTINUE);
      case 1:
        if (!tracks[0].hasAudio() && tracks[0].hasVideo() && promiscuousMuxing) {
          videoTrack = tracks[0];
          audioTrack = findAudioTrack(tracks[0], mediaPackage);
        } else {
          audioTrack = tracks[0];
          videoTrack = tracks[0];
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
        logger.info("Encoding audio only track {} to work version", videoTrack);
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
        logger.info("Encoding audio only track {} to work version", audioTrack);
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
        logger.info("Encoding audiovisual track {} to work version", videoTrack);
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
      logger.info("Muxing audio and video only track {} to work version", videoTrack);

      if (audioTrack.hasVideo()) {
        logger.info("Stripping audio from track {}", audioTrack);
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

    // Add the target tags
    if (overrideTags.size() > 0) {
      composedTrack.clearTags();
      for (String tag : overrideTags) {
        logger.trace("Tagging composed track with '{}'", tag);
        composedTrack.addTag(tag);
      }
    } else {
      for (String tag : removeTags) {
        logger.trace("Remove tagging '{}' from composed track", tag);
        composedTrack.removeTag(tag.substring(MINUS.length()));
      }
      for (String tag : addTags) {
        logger.trace("Add tagging '{}' to composed track", tag);
        composedTrack.addTag(tag.substring(PLUS.length()));
      }
    }
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
    logger.info("Encoding video only track {} to work version", videoTrack);
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
   * Finds a suitable audio track from the mediapackage
   *
   * @param videoTrack
   *          the video track
   * @param mediaPackage
   *          the mediapackage
   * @return the found audio track
   */
  private Track findAudioTrack(Track videoTrack, MediaPackage mediaPackage) {
    MediaPackageElementFlavor flavor = new MediaPackageElementFlavor("*", videoTrack.getFlavor().getSubtype());

    // Try matching subtype first
    for (Track t : mediaPackage.getTracks(flavor)) {
      if (t.hasAudio()) {
        logger.info("Promiscuous audio muxing found audio source {} with flavor {}", t, t.getFlavor());
        return t;
      }
    }

    // Ok, full promiscuous mode now
    for (Track t : mediaPackage.getTracks()) {
      if (t.hasAudio()) {
        logger.info("Promiscuous audio muxing resulted in audio source {}", t);
        return t;
      }
    }

    return null;
  }

}
