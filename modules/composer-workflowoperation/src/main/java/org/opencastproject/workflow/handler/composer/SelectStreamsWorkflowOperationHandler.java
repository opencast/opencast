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
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationTagUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SelectStreamsWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(SelectStreamsWorkflowOperationHandler.class);

  /** Name of the 'encode to video only work copy' encoding profile */
  private static final String PREPARE_VIDEO_ONLY_PROFILE = "video-only.work";

  /** Name of the 'encode to video only work copy' encoding profile */
  private static final String PREPARE_AUDIO_ONLY_PROFILE = "audio-only.work";

  /** Name of the muxing encoding profile */
  private static final String MUX_AV_PROFILE = "mux-av.work";

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

  private enum AudioMuxing {
    NONE, FORCE, DUPLICATE;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }

    static AudioMuxing fromConfigurationString(final String s) {
      return AudioMuxing.valueOf(s.toUpperCase());
    }
  }

  private static final String CONFIG_AUDIO_MUXING = "audio-muxing";

  private static final String CONFIG_FORCE_TARGET = "force-target";

  private static final String FORCE_TARGET_DEFAULT = "presenter";

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the local composer service
   */
  protected void setComposerService(final ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(final Workspace workspace) {
    this.workspace = workspace;
  }

  private EncodingProfile getProfile(final String identifier) throws WorkflowOperationException {
    final EncodingProfile profile = this.composerService.getProfile(identifier);
    if (profile == null) {
      throw new WorkflowOperationException(String.format("couldn't find encoding profile \"%s\"", identifier));
    }
    return profile;
  }

  private static Optional<String> getConfiguration(final WorkflowInstance instance, final String key) {
    return Optional.ofNullable(instance.getCurrentOperation().getConfiguration(key)).map(StringUtils::trimToNull);
  }

  private enum SubTrack {
    AUDIO, VIDEO
  }

  /**
   * During our operations, we accumulate new tracks and wait times, for which we have this nice helper class
   */
  private static final class MuxResult {
    private long queueTime;
    private final Collection<Track> tracks;

    private MuxResult(final long queueTime, final Collection<Track> tracks) {
      this.queueTime = queueTime;
      this.tracks = tracks;
    }

    static MuxResult empty() {
      return new MuxResult(0L, new ArrayList<>(0));
    }

    void forEachTrack(final Consumer<Track> trackConsumer) {
      tracks.forEach(trackConsumer);
    }

    public void add(final TrackJobResult jobResult) {
      this.queueTime += jobResult.waitTime;
      this.tracks.add(jobResult.track);
    }

    public void add(final MuxResult muxResult) {
      this.queueTime += muxResult.queueTime;
      this.tracks.addAll(muxResult.tracks);
    }
  }

  private static final class AugmentedTrack {
    private final Track track;
    private final boolean hideAudio;
    private final boolean hideVideo;

    private AugmentedTrack(final Track track, final boolean hideAudio, final boolean hideVideo) {
      this.track = track;
      this.hideAudio = hideAudio;
      this.hideVideo = hideVideo;
    }

    boolean has(final SubTrack t) {
      if (t == SubTrack.AUDIO) {
        return hasAudio();
      } else {
        return hasVideo();
      }
    }

    boolean hide(final SubTrack t) {
      if (t == SubTrack.AUDIO) {
        return hideAudio;
      } else {
        return hideVideo;
      }
    }

    boolean hasAudio() {
      return track.hasAudio();
    }

    boolean hasVideo() {
      return track.hasVideo();
    }

    String getFlavorType() {
      return track.getFlavor().getType();
    }

    public String toString() {
      return String.format("ID: %s, Flavor: %s [hasAudio %s, hideAudio %s, hasVideo %s, hideVideo: %s]",
          track.getIdentifier(),track.getFlavor(),
          hasAudio(), hide(SubTrack.AUDIO),
          hasVideo(), hide(SubTrack.VIDEO)
      );
    }
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, final JobContext context)
          throws WorkflowOperationException {
    try {
      return doStart(workflowInstance);
    } catch (final EncoderException | MediaPackageException | IOException | NotFoundException e) {
      throw new WorkflowOperationException(e);
    }
  }

  private WorkflowOperationResult doStart(final WorkflowInstance workflowInstance)
          throws WorkflowOperationException, EncoderException, MediaPackageException, NotFoundException, IOException {
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    final MediaPackageElementFlavor sourceFlavor = getConfiguration(workflowInstance, "source-flavor")
            .map(MediaPackageElementFlavor::parseFlavor)
            .orElseThrow(() -> new IllegalStateException("Source flavor must be specified"));

    final MediaPackageElementFlavor targetTrackFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.trimToNull(
            getConfiguration(workflowInstance, "target-flavor")
                    .orElseThrow(() -> new IllegalStateException("Target flavor not specified"))));

    final Track[] tracks = mediaPackage.getTracks(sourceFlavor);

    if (tracks.length == 0) {
      logger.info("No audio/video tracks with flavor '{}' found to prepare", sourceFlavor);
      return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
    }

    final List<AugmentedTrack> augmentedTracks = createAugmentedTracks(tracks, workflowInstance);

    final MuxResult result = MuxResult.empty();
    // Note that the logic below currently supports at most two input tracks

    if (allNonHidden(augmentedTracks, SubTrack.VIDEO)) {
      // Case 1: We have only tracks with non-hidden video streams. So we keep them all and possibly cut away audio.

      final AudioMuxing audioMuxing = getConfiguration(workflowInstance, CONFIG_AUDIO_MUXING)
              .map(AudioMuxing::fromConfigurationString).orElse(AudioMuxing.NONE);
      final Optional<AugmentedTrack> singleAudioTrackOpt = findSingleAudioTrack(augmentedTracks);
      final boolean multipleVideo = augmentedTracks.size() > 1;

      if (multipleVideo && audioMuxing == AudioMuxing.DUPLICATE && singleAudioTrackOpt.isPresent()) {
        /* Case 1.1: We have more than one track, all tracks have non-hidden video streams and exactly one track has
           an audio stream. Audio muxing is set to DUPLICATE, so duplicate the single audio stream into all tracks */
        logger.debug("Duplicate the audio stream of track {} into all tracks", singleAudioTrackOpt.get());
        final AugmentedTrack singleAudioTrack = singleAudioTrackOpt.get();
        for (final AugmentedTrack t : augmentedTracks) {
          if (t.track != singleAudioTrack.track) {
            final TrackJobResult jobResult = mux(t.track, singleAudioTrack.track, mediaPackage);
            result.add(jobResult);
          } else {
            result.add(copyTrack(t.track));
          }
        }
      } else if (multipleVideo && audioMuxing == AudioMuxing.FORCE && singleAudioTrackOpt.isPresent()) {
        /* Case 1.2: We have more than one track, all tracks have non-hidden video streams and exactly one track has
           an audio stream. Audio muxing is set to FORCE, so we enforce that the audio stream is moved to the track
           specified by CONFIG_FORCE_TARGET if not already there */
        logger.debug("Enforce audio stream to be present in track {} only", singleAudioTrackOpt.get());
        final AugmentedTrack singleAudioTrack = singleAudioTrackOpt.get();
        final String forceTargetOpt = getConfiguration(workflowInstance, CONFIG_FORCE_TARGET)
                .orElse(FORCE_TARGET_DEFAULT);

        final Optional<AugmentedTrack> forceTargetTrackOpt = findTrackByFlavorType(augmentedTracks, forceTargetOpt);

        if (!forceTargetTrackOpt.isPresent()) {
          throw new IllegalStateException(
                  String.format("\"%s\" set to \"%s\", but target flavor \"%s\" not found!",
                          CONFIG_AUDIO_MUXING,
                          AudioMuxing.FORCE, forceTargetOpt));
        }

        final AugmentedTrack forceTargetTrack = forceTargetTrackOpt.get();

        if (singleAudioTrack.track != forceTargetTrack.track) {
          // Copy it over...
          final TrackJobResult muxResult = mux(forceTargetTrack.track, singleAudioTrack.track, mediaPackage);
          result.add(muxResult);

          // ...and remove the original
          final TrackJobResult hideAudioResult = hideAudio(singleAudioTrack.track, mediaPackage);
          result.add(hideAudioResult);
        } else {
          result.add(copyTrack(singleAudioTrack.track));
        }

        // Just copy the rest of the tracks and remove audio where necessary
        for (final AugmentedTrack augmentedTrack : augmentedTracks) {
          if (augmentedTrack.track != singleAudioTrack.track && augmentedTrack.track != forceTargetTrack.track) {
            if (augmentedTrack.hasAudio() && augmentedTrack.hide(SubTrack.AUDIO)) {
              final TrackJobResult hideAudioResult = hideAudio(augmentedTrack.track, mediaPackage);
              result.add(hideAudioResult);
            } else {
              result.add(copyTrack(augmentedTrack.track));
            }
          }
        }
      } else {
        /* Case 1.3: We have one or more tracks and all tracks have non-hidden video streams. Audio muxing is either
           set to NONE or we don't have a single audio streams as required by DUPLICATE and FORCE.
           In this case, simply remove audio streams where requested or copy the track otherwise */
        final MuxResult muxResult = muxMultipleVideoTracks(mediaPackage, augmentedTracks);
        result.add(muxResult);
      }
    } else if (allHidden(augmentedTracks, SubTrack.VIDEO)) {
       /* Case 2: No tracks have non-hidden video streams. In this case, simply remove video streams where
          requested or copy the track otherwise */
       for (final AugmentedTrack t : augmentedTracks) {
         if (t.hasAudio()) {
           if (t.hide(SubTrack.VIDEO)) {
             final TrackJobResult hideVideoResult = hideVideo(t.track, mediaPackage);
             result.add(hideVideoResult);
           } else {
             result.add(copyTrack(t.track));
           }
         }
       }
    } else {
      /* Case 3: We have one or more tracks where exactly one track has a non-hidden video stream (implied as this
         logic assumes at most two input tracks).
         Considering the audio stream, the track with the non-hidden video stream might also contain an audio stream
         or we have to mux the audio stream from another track into that track */
      final MuxResult muxResult = muxSingleVideoTrack(mediaPackage, augmentedTracks);
      result.add(muxResult);
    }

    // Update Flavor and add to media package
    result.forEachTrack(t -> {
      t.setFlavor(new MediaPackageElementFlavor(t.getFlavor().getType(), targetTrackFlavor.getSubtype()));
      mediaPackage.add(t);
    });

    // Update Tags here
    getConfiguration(workflowInstance, "target-tags").ifPresent(tags -> {
      final WorkflowOperationTagUtil.TagDiff tagDiff = WorkflowOperationTagUtil.createTagDiff(tags);
      result.forEachTrack(t -> WorkflowOperationTagUtil.applyTagDiff(tagDiff, t));
    });

    return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE, result.queueTime);
  }

  private Optional<AugmentedTrack> findTrackByFlavorType(final Collection<AugmentedTrack> augmentedTracks,
          final String flavorType) {
    return augmentedTracks.stream().filter(augmentedTrack -> augmentedTrack.getFlavorType().equals(flavorType))
            .findAny();
  }

  private MuxResult muxSingleVideoTrack(final MediaPackage mediaPackage, final Collection<AugmentedTrack> augmentedTracks)
          throws MediaPackageException, EncoderException, WorkflowOperationException, NotFoundException, IOException {
    long queueTime = 0L;

    final Collection<Track> resultingTracks = new ArrayList<>(0);

    // This method expects exactly one track with a non-hidden video stream. Let's find it.
    final AugmentedTrack nonHiddenVideo = findNonHidden(augmentedTracks, SubTrack.VIDEO)
            .orElseThrow(() -> new IllegalStateException("couldn't find a stream with non-hidden video"));
    // Implicit here is the assumption that there's just _one_ other audio stream. It's written so that
    // we can loosen this assumption later on.
    final Optional<AugmentedTrack> nonHiddenAudio = findNonHidden(augmentedTracks, SubTrack.AUDIO);

    // If there's just one non-hidden video stream, and that one has hidden audio, we have to cut that away, too.
    if (nonHiddenVideo.hasAudio() && nonHiddenVideo.hideAudio && (!nonHiddenAudio.isPresent()
            || nonHiddenAudio.get() == nonHiddenVideo)) {
      final TrackJobResult jobResult = hideAudio(nonHiddenVideo.track, mediaPackage);
      resultingTracks.add(jobResult.track);
      queueTime += jobResult.waitTime;
    } else if (!nonHiddenAudio.isPresent() || nonHiddenAudio.get() == nonHiddenVideo) {
      // It could be the case that the non-hidden video stream is also the non-hidden audio stream. In that
      // case, we don't have to mux. But have to clone it.
      final Track clonedTrack = (Track) nonHiddenVideo.track.clone();
      clonedTrack.setIdentifier(null);
      resultingTracks.add(clonedTrack);
    } else {
      // Otherwise, we mux!
      final TrackJobResult jobResult = mux(nonHiddenVideo.track, nonHiddenAudio.get().track, mediaPackage);
      resultingTracks.add(jobResult.track);
      queueTime += jobResult.waitTime;
    }
    return new MuxResult(queueTime, resultingTracks);
  }

  private MuxResult muxMultipleVideoTracks(final MediaPackage mediaPackage, final Iterable<AugmentedTrack> augmentedTracks)
          throws MediaPackageException, EncoderException, WorkflowOperationException, NotFoundException, IOException {
    long queueTime = 0L;
    final List<Track> resultingTracks = new ArrayList<>(0);
    for (final AugmentedTrack t : augmentedTracks) {
      if (t.hasAudio() && t.hideAudio) {
        // The flavor gets "nulled" in the process. Reverse that so we can treat all tracks equally.
        final MediaPackageElementFlavor previousFlavor = t.track.getFlavor();
        final TrackJobResult trackJobResult = hideAudio(t.track, mediaPackage);
        trackJobResult.track.setFlavor(previousFlavor);
        resultingTracks.add(trackJobResult.track);
        queueTime += trackJobResult.waitTime;
      } else {
        // Even if we don't modify the track, we clone and re-add it to the MP (since it will be a new track with a
        // different flavor)
        logger.debug("Add clone of track {} to mediapackage {}", t.track.getIdentifier(),
            mediaPackage.getIdentifier());
        final Track clonedTrack = (Track) t.track.clone();
        clonedTrack.setIdentifier(null);
        resultingTracks.add(clonedTrack);
      }
    }
    return new MuxResult(queueTime, resultingTracks);
  }

  /**
   * Returns the single track that has audio, or an empty {@code Optional} if either more than one audio track exists, or none exists.
   * @param augmentedTracks List of tracks
   * @return See above.
   */
  private Optional<AugmentedTrack> findSingleAudioTrack(final Iterable<AugmentedTrack> augmentedTracks) {
    AugmentedTrack result = null;
    for (final AugmentedTrack augmentedTrack : augmentedTracks) {
      if (augmentedTrack.hasAudio() && !augmentedTrack.hideAudio) {
        // Already got an audio track? Aw, then there's more than one! :(
        if (result != null) {
          return Optional.empty();
        }
        result = augmentedTrack;
      }
    }
    return Optional.ofNullable(result);
  }

  private TrackJobResult mux(final Track videoTrack, final Track audioTrack, final MediaPackage mediaPackage)
          throws MediaPackageException, EncoderException, WorkflowOperationException, NotFoundException, IOException {
    logger.info("Mux video track {} and audio track {}", videoTrack, audioTrack);
    // Find the encoding profile
    final EncodingProfile profile = getProfile(MUX_AV_PROFILE);

    final Job job = composerService.mux(videoTrack, audioTrack, profile.getIdentifier());
    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException(
              String.format("Muxing video track %s and audio track %s failed", videoTrack, audioTrack));
    }
    final MediaPackageElementFlavor previousFlavor = videoTrack.getFlavor();
    final TrackJobResult trackJobResult = processJob(videoTrack, mediaPackage, job);
    trackJobResult.track.setFlavor(previousFlavor);
    return trackJobResult;
  }

  private static final class TrackJobResult {
    private final Track track;
    private final long waitTime;

    private TrackJobResult(final Track track, final long waitTime) {
      this.track = track;
      this.waitTime = waitTime;
    }
  }

  private TrackJobResult hideVideo(final Track track, final MediaPackage mediaPackage)
          throws MediaPackageException, EncoderException, WorkflowOperationException, NotFoundException, IOException {
    logger.info("Remove video streams from track {}", track.getIdentifier());
    return hide(PREPARE_AUDIO_ONLY_PROFILE, track, mediaPackage);
  }

  private TrackJobResult hideAudio(final Track track, final MediaPackage mediaPackage)
          throws MediaPackageException, EncoderException, WorkflowOperationException, NotFoundException, IOException {
    logger.info("Remove audio streams from track {}", track.getIdentifier());
    return hide(PREPARE_VIDEO_ONLY_PROFILE, track, mediaPackage);
  }

  private TrackJobResult hide(String encodingProfile, final Track track, final MediaPackage mediaPackage)
          throws MediaPackageException, EncoderException, WorkflowOperationException, NotFoundException, IOException {
    // Find the encoding profile
    final EncodingProfile profile = getProfile(encodingProfile);
    final Job job = composerService.encode(track, profile.getIdentifier());
    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException(String.format("Rewriting container for video track %s failed", track));
    }
    final MediaPackageElementFlavor previousFlavor = track.getFlavor();
    final TrackJobResult trackJobResult = processJob(track, mediaPackage, job);
    trackJobResult.track.setFlavor(previousFlavor);
    return trackJobResult;
  }

  private TrackJobResult processJob(final Track track, final MediaPackage mediaPackage, final Job job)
          throws MediaPackageException, NotFoundException, IOException {
    final Track composedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
    final String fileName = getFileNameFromElements(track, composedTrack);

    // Note that the composed track must have an ID before being moved to the mediapackage in the working file
    // repository. This ID is generated when the track is added to the mediapackage. So the track must be added
    // to the mediapackage before attempting to move the file.
    composedTrack.setURI(workspace
            .moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(), composedTrack.getIdentifier(),
                    fileName));
    return new TrackJobResult(composedTrack, job.getQueueTime());
  }

  private Optional<AugmentedTrack> findNonHidden(final Collection<AugmentedTrack> augmentedTracks, final SubTrack st) {
    return augmentedTracks.stream().filter(t -> t.has(st) && !t.hide(st)).findAny();
  }

  private boolean allNonHidden(final Collection<AugmentedTrack> augmentedTracks,
          @SuppressWarnings("SameParameterValue") final SubTrack st) {
    return augmentedTracks.stream().noneMatch(t -> !t.has(st) || t.hide(st));
  }

  private boolean allHidden(final Collection<AugmentedTrack> augmentedTracks,
          @SuppressWarnings("SameParameterValue") final SubTrack st) {
    return augmentedTracks.stream().noneMatch(t -> t.has(st) && !t.hide(st));
  }

  private static String constructHideProperty(final String s, final SubTrack st) {
    return "hide_" + s + "_" + st.toString().toLowerCase();
  }

  private boolean trackHidden(final WorkflowInstance instance, final String subtype, final SubTrack st) {
    final String hideProperty = instance.getConfiguration(constructHideProperty(subtype, st));
    return Boolean.parseBoolean(hideProperty);
  }

  private List<AugmentedTrack> createAugmentedTracks(final Track[] tracks, final WorkflowInstance instance) {
    return Arrays.stream(tracks).map(t -> {
      final boolean hideAudio = trackHidden(instance, t.getFlavor().getType(), SubTrack.AUDIO);
      final boolean hideVideo = trackHidden(instance, t.getFlavor().getType(), SubTrack.VIDEO);
      AugmentedTrack result = new AugmentedTrack(t, hideAudio, hideVideo);
      logger.debug("AugmentedTrack {}", result);
      return result;
    }).collect(Collectors.toList());
  }

  private TrackJobResult copyTrack(final Track track) throws WorkflowOperationException {
    logger.debug("Create copy of track {}", track);
    final Track copiedTrack = (Track) track.clone();
    copiedTrack.setIdentifier(UUID.randomUUID().toString());
    try {
      // Generate a new filename
      String targetFilename = copiedTrack.getIdentifier();
      final String extension = FilenameUtils.getExtension(track.getURI().getPath());
      if (!extension.isEmpty()) {
        targetFilename += "." + extension;
      }

      // Copy the files on dis and put them into the working file repository
      final URI newUri = workspace.put(track.getMediaPackage().getIdentifier().toString(), copiedTrack.getIdentifier(),
              targetFilename, workspace.read(track.getURI()));
      copiedTrack.setURI(newUri);
    } catch (IOException | NotFoundException e) {
      throw new WorkflowOperationException(String.format("Error while copying track %s", track.getIdentifier()), e);
    }

    return new TrackJobResult(copiedTrack, 0);
  }
}
