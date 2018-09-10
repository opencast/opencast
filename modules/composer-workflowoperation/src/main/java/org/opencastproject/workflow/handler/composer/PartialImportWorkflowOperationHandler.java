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

import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static org.opencastproject.util.JobUtil.getPayload;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSupport.Filters;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.smil.api.util.SmilUtil;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.VCell;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILParElement;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The workflow definition for handling partial import operations
 */
public class PartialImportWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Workflow configuration keys */
  private static final String SOURCE_PRESENTER_FLAVOR = "source-presenter-flavor";
  private static final String SOURCE_PRESENTATION_FLAVOR = "source-presentation-flavor";
  private static final String SOURCE_SMIL_FLAVOR = "source-smil-flavor";

  private static final String TARGET_PRESENTER_FLAVOR = "target-presenter-flavor";
  private static final String TARGET_PRESENTATION_FLAVOR = "target-presentation-flavor";

  private static final String CONCAT_ENCODING_PROFILE = "concat-encoding-profile";
  private static final String CONCAT_OUTPUT_FRAMERATE = "concat-output-framerate";
  private static final String TRIM_ENCODING_PROFILE = "trim-encoding-profile";
  private static final String FORCE_ENCODING_PROFILE = "force-encoding-profile";

  private static final String FORCE_ENCODING = "force-encoding";
  private static final String REQUIRED_EXTENSIONS = "required-extensions";
  private static final String ENFORCE_DIVISIBLE_BY_TWO = "enforce-divisible-by-two";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PartialImportWorkflowOperationHandler.class);

  /** Other constants */
  private static final String EMPTY_VALUE = "";
  private static final String NODE_TYPE_AUDIO = "audio";
  private static final String NODE_TYPE_VIDEO = "video";
  private static final String FLAVOR_AUDIO_SUFFIX = "-audio";
  private static final String COLLECTION_ID = "composer";
  private static final String UNKNOWN_KEY = "unknown";
  private static final String PRESENTER_KEY = "presenter";
  private static final String PRESENTATION_KEY = "presentation";
  private static final String DEFAULT_REQUIRED_EXTENSION = "mp4";

  /** Needed encoding profiles */
  private static final String PREVIEW_PROFILE = "import.preview";
  private static final String IMAGE_FRAME_PROFILE = "import.image-frame";
  private static final String SILENT_AUDIO_PROFILE = "import.silent";
  private static final String IMAGE_MOVIE_PROFILE = "image-movie.work";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_PRESENTER_FLAVOR, "The source flavor of the partial presenter tracks");
    CONFIG_OPTIONS.put(SOURCE_PRESENTATION_FLAVOR, "The source flavor of the partial presentation tracks");
    CONFIG_OPTIONS.put(SOURCE_SMIL_FLAVOR, "The source flavor of the partial smil catalog");

    CONFIG_OPTIONS.put(TARGET_PRESENTER_FLAVOR,
            "The target flavor to apply to the standard media presenter video track");
    CONFIG_OPTIONS.put(TARGET_PRESENTATION_FLAVOR,
            "The target flavor to apply to the standard media presentation video track");
    CONFIG_OPTIONS.put(CONCAT_ENCODING_PROFILE, "The concat encoding profile to use");
    CONFIG_OPTIONS.put(CONCAT_OUTPUT_FRAMERATE, "Output framerate for concat operation");
    CONFIG_OPTIONS.put(FORCE_ENCODING_PROFILE, "The force encoding profile to use");
    CONFIG_OPTIONS.put(TRIM_ENCODING_PROFILE, "The trim encoding profile to use");
    CONFIG_OPTIONS.put(FORCE_ENCODING, "Whether to force the tracks to be encoded");
    CONFIG_OPTIONS.put(REQUIRED_EXTENSIONS,
            "Automatically re-encode a track if its extension doesn't match one in this comma separated list");
    CONFIG_OPTIONS.put(ENFORCE_DIVISIBLE_BY_TWO, "Whether to enforce the track's dimension to be divisible by two");
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
  public void setComposerService(ComposerService composerService) {
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
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running partial import workflow operation on workflow {}", workflowInstance.getId());

    List<MediaPackageElement> elementsToClean = new ArrayList<MediaPackageElement>();

    try {
      return concat(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation(), elementsToClean);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    } finally {
      for (MediaPackageElement elem : elementsToClean) {
        try {
          workspace.delete(elem.getURI());
        } catch (Exception e) {
          logger.warn("Unable to delete element {}: {}", elem, e);
        }
      }
    }
  }

  private WorkflowOperationResult concat(MediaPackage src, WorkflowOperationInstance operation,
          List<MediaPackageElement> elementsToClean) throws EncoderException, IOException, NotFoundException,
          MediaPackageException, WorkflowOperationException, ServiceRegistryException {
    final MediaPackage mediaPackage = (MediaPackage) src.clone();
    final Long operationId = operation.getId();
    //
    // read config options
    final Opt<String> presenterFlavor = getOptConfig(operation, SOURCE_PRESENTER_FLAVOR);
    final Opt<String> presentationFlavor = getOptConfig(operation, SOURCE_PRESENTATION_FLAVOR);
    final MediaPackageElementFlavor smilFlavor = MediaPackageElementFlavor.parseFlavor(getConfig(operation, SOURCE_SMIL_FLAVOR));
    final String concatEncodingProfile = getConfig(operation, CONCAT_ENCODING_PROFILE);
    final Opt<String> concatOutputFramerate = getOptConfig(operation, CONCAT_OUTPUT_FRAMERATE);
    final String trimEncodingProfile = getConfig(operation, TRIM_ENCODING_PROFILE);
    final MediaPackageElementFlavor targetPresenterFlavor = parseTargetFlavor(
            getConfig(operation, TARGET_PRESENTER_FLAVOR), "presenter");
    final MediaPackageElementFlavor targetPresentationFlavor = parseTargetFlavor(
            getConfig(operation, TARGET_PRESENTATION_FLAVOR), "presentation");
    final Opt<EncodingProfile> forceProfile = getForceEncodingProfile(operation);
    final boolean forceEncoding = BooleanUtils.toBoolean(getOptConfig(operation, FORCE_ENCODING).getOr("false"));
    final boolean forceDivisible = BooleanUtils.toBoolean(getOptConfig(operation, ENFORCE_DIVISIBLE_BY_TWO).getOr("false"));
    final List<String> requiredExtensions = getRequiredExtensions(operation);

    //
    // further checks on config options
    // Skip the worklow if no presenter and presentation flavor has been configured
    if (presenterFlavor.isNone() && presentationFlavor.isNone()) {
      logger.warn("No presenter and presentation flavor has been set.");
      return createResult(mediaPackage, Action.SKIP);
    }
    final EncodingProfile concatProfile = composerService.getProfile(concatEncodingProfile);
    if (concatProfile == null) {
      throw new WorkflowOperationException("Concat encoding profile '" + concatEncodingProfile + "' was not found");
    }

    float outputFramerate = -1.0f;
    if (concatOutputFramerate.isSome()) {
      if (NumberUtils.isNumber(concatOutputFramerate.get())) {
        logger.info("Using concat output framerate");
        outputFramerate = NumberUtils.toFloat(concatOutputFramerate.get());
      } else {
        throw new WorkflowOperationException("Unable to parse concat output frame rate!");
      }
    }

    final EncodingProfile trimProfile = composerService.getProfile(trimEncodingProfile);
    if (trimProfile == null) {
      throw new WorkflowOperationException("Trim encoding profile '" + trimEncodingProfile + "' was not found");
    }

    //
    // get tracks
    final TrackSelector presenterTrackSelector = mkTrackSelector(presenterFlavor);
    final TrackSelector presentationTrackSelector = mkTrackSelector(presentationFlavor);
    final List<Track> originalTracks = new ArrayList<Track>();
    final List<Track> presenterTracks = new ArrayList<Track>();
    final List<Track> presentationTracks = new ArrayList<Track>();
    // Collecting presenter tracks
    for (Track t : presenterTrackSelector.select(mediaPackage, false)) {
      logger.info("Found partial presenter track {}", t);
      originalTracks.add(t);
      presenterTracks.add(t);
    }
    // Collecting presentation tracks
    for (Track t : presentationTrackSelector.select(mediaPackage, false)) {
      logger.info("Found partial presentation track {}", t);
      originalTracks.add(t);
      presentationTracks.add(t);
    }

    // flavor_type -> job
    final Map<String, Job> jobs = new HashMap<String, Job>();
    // get SMIL catalog
    final SMILDocument smilDocument;
    try {
      smilDocument = SmilUtil.getSmilDocumentFromMediaPackage(mediaPackage, smilFlavor, workspace);
    } catch (SAXException e) {
      throw new WorkflowOperationException(e);
    }
    final SMILParElement parallel = (SMILParElement) smilDocument.getBody().getChildNodes().item(0);
    final NodeList sequences = parallel.getTimeChildren();
    final float trackDurationInSeconds = parallel.getDur();
    final long trackDurationInMs = Math.round(trackDurationInSeconds * 1000f);
    for (int i = 0; i < sequences.getLength(); i++) {
      final SMILElement item = (SMILElement) sequences.item(i);

      for (final String mediaType : new String[] { NODE_TYPE_AUDIO, NODE_TYPE_VIDEO }) {
        final List<Track> tracks = new ArrayList<Track>();
        final VCell<String> sourceType = VCell.cell(EMPTY_VALUE);

        final long position = processChildren(0, tracks, item.getChildNodes(), originalTracks, sourceType, mediaType,
                elementsToClean, operationId);

        if (tracks.isEmpty()) {
          logger.debug("The tracks list was empty.");
          continue;
        }
        final Track lastTrack = tracks.get(tracks.size() - 1);

        if (position < trackDurationInMs) {
          final double extendingTime = (trackDurationInMs - position) / 1000d;
          if (extendingTime > 0) {
            if (!lastTrack.hasVideo()) {
              logger.info("Extending {} audio track end by {} seconds with silent audio", sourceType.get(),
                      extendingTime);
              tracks.add(getSilentAudio(extendingTime, elementsToClean, operationId));
            } else {
              logger.info("Extending {} track end with last image frame by {} seconds", sourceType.get(), extendingTime);
              Attachment tempLastImageFrame = extractLastImageFrame(lastTrack, elementsToClean);
              tracks.add(createVideoFromImage(tempLastImageFrame, extendingTime, elementsToClean));
            }
          }
        }

        if (tracks.size() < 2) {
          logger.debug("There were less than 2 tracks, copying track...");
          if (sourceType.get().startsWith(PRESENTER_KEY)) {
            createCopyOfTrack(mediaPackage, tracks.get(0), targetPresenterFlavor);
          } else if (sourceType.get().startsWith(PRESENTATION_KEY)) {
            createCopyOfTrack(mediaPackage, tracks.get(0), targetPresentationFlavor);
          } else {
            logger.warn("Can't handle unkown source type '{}' for unprocessed track", sourceType.get());
          }
          continue;
        }

        for (final Track t : tracks) {
          if (!t.hasVideo() && !t.hasAudio()) {
            logger.error("No audio or video stream available in the track with flavor {}! {}", t.getFlavor(), t);
            throw new WorkflowOperationException("No audio or video stream available in the track " + t.toString());
          }
        }

        if (sourceType.get().startsWith(PRESENTER_KEY)) {
          logger.info("Concatenating {} track", PRESENTER_KEY);
          jobs.put(sourceType.get(), startConcatJob(concatProfile, tracks, outputFramerate, forceDivisible));
        } else if (sourceType.get().startsWith(PRESENTATION_KEY)) {
          logger.info("Concatenating {} track", PRESENTATION_KEY);
          jobs.put(sourceType.get(), startConcatJob(concatProfile, tracks, outputFramerate, forceDivisible));
        } else {
          logger.warn("Can't handle unknown source type '{}'!", sourceType.get());
        }
      }
    }

    // Wait for the jobs to return
    if (jobs.size() > 0) {
      if (!JobUtil.waitForJobs(serviceRegistry, jobs.values()).isSuccess()) {
        throw new WorkflowOperationException("One of the concat jobs did not complete successfully");
      }
    } else {
      logger.info("No concatenating needed for presenter and presentation tracks, took partial source elements");
    }

    // All the jobs have passed, let's update the media package
    long queueTime = 0L;
    MediaPackageElementFlavor adjustedTargetPresenterFlavor = targetPresenterFlavor;
    MediaPackageElementFlavor adjustedTargetPresentationFlavor = targetPresentationFlavor;
    for (final Entry<String, Job> job : jobs.entrySet()) {
      final Opt<Job> concatJob = JobUtil.update(serviceRegistry, job.getValue());
      if (concatJob.isSome()) {
        final String concatPayload = concatJob.get().getPayload();
        if (concatPayload != null) {
          final Track concatTrack;
          try {
            concatTrack = (Track) MediaPackageElementParser.getFromXml(concatPayload);
          } catch (MediaPackageException e) {
            throw new WorkflowOperationException(e);
          }

          final String fileName;

          // Adjust the target flavor.
          if (job.getKey().startsWith(PRESENTER_KEY)) {
            if (!concatTrack.hasVideo()) {
              fileName = PRESENTER_KEY.concat(FLAVOR_AUDIO_SUFFIX);
              adjustedTargetPresenterFlavor = deriveAudioFlavor(targetPresenterFlavor);
            } else {
              fileName = PRESENTER_KEY;
              adjustedTargetPresenterFlavor = targetPresenterFlavor;
            }
            concatTrack.setFlavor(adjustedTargetPresenterFlavor);
          } else if (job.getKey().startsWith(PRESENTATION_KEY)) {
            if (!concatTrack.hasVideo()) {
              fileName = PRESENTATION_KEY.concat(FLAVOR_AUDIO_SUFFIX);
              adjustedTargetPresentationFlavor = deriveAudioFlavor(targetPresentationFlavor);
            } else {
              fileName = PRESENTATION_KEY;
              adjustedTargetPresentationFlavor = targetPresentationFlavor;
            }
            concatTrack.setFlavor(adjustedTargetPresentationFlavor);
          } else {
            fileName = UNKNOWN_KEY;
          }

          concatTrack.setURI(workspace.moveTo(concatTrack.getURI(), mediaPackage.getIdentifier().toString(),
                  concatTrack.getIdentifier(),
                  fileName + "." + FilenameUtils.getExtension(concatTrack.getURI().toString())));

          logger.info("Concatenated track {} got flavor '{}'", concatTrack, concatTrack.getFlavor());

          mediaPackage.add(concatTrack);
          queueTime += concatJob.get().getQueueTime();
        } else {
          // If there is no payload, then the item has not been distributed.
          logger.warn("Concat job {} does not contain a payload", concatJob);
        }
      } else {
        logger.warn("Concat job {} could not be updated since it cannot be found", job.getValue());
      }
    }

    // Trim presenter and presentation source track if longer than the duration from the SMIL catalog
    queueTime += checkForTrimming(mediaPackage, trimProfile, targetPresentationFlavor, trackDurationInSeconds,
            elementsToClean);
    queueTime += checkForTrimming(mediaPackage, trimProfile, deriveAudioFlavor(targetPresentationFlavor),
            trackDurationInSeconds, elementsToClean);
    queueTime += checkForTrimming(mediaPackage, trimProfile, targetPresenterFlavor, trackDurationInSeconds,
            elementsToClean);
    queueTime += checkForTrimming(mediaPackage, trimProfile, deriveAudioFlavor(targetPresenterFlavor),
            trackDurationInSeconds, elementsToClean);

    adjustAudioTrackTargetFlavor(mediaPackage, targetPresenterFlavor);
    adjustAudioTrackTargetFlavor(mediaPackage, targetPresentationFlavor);

    queueTime += checkForMuxing(mediaPackage, targetPresenterFlavor, targetPresentationFlavor, false, elementsToClean);

    queueTime += checkForEncodeToStandard(mediaPackage, forceEncoding, forceProfile, requiredExtensions,
            targetPresenterFlavor, targetPresentationFlavor, elementsToClean);

    final WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, queueTime);
    logger.debug("Partial import operation completed");
    return result;
  }

  protected long checkForEncodeToStandard(MediaPackage mediaPackage, boolean forceEncoding,
          Opt<EncodingProfile> forceProfile, List<String> requiredExtensions,
          MediaPackageElementFlavor targetPresenterFlavor, MediaPackageElementFlavor targetPresentationFlavor,
          List<MediaPackageElement> elementsToClean) throws EncoderException, IOException, MediaPackageException,
          NotFoundException, ServiceRegistryException, WorkflowOperationException {
    long queueTime = 0;
    if (forceProfile.isSome()) {
      Track[] targetPresenterTracks = mediaPackage.getTracks(targetPresenterFlavor);
      for (Track track : targetPresenterTracks) {
        if (forceEncoding || trackNeedsTobeEncodedToStandard(track, requiredExtensions)) {
          logger.debug("Encoding '{}' flavored track '{}' with standard encoding profile {}",
                  targetPresenterFlavor, track.getURI(), forceProfile.get());
          queueTime += encodeToStandard(mediaPackage, forceProfile.get(), targetPresenterFlavor, track);
          elementsToClean.add(track);
          mediaPackage.remove(track);
        }
      }
      // Skip presentation target if it is the same as the presenter one.
      if (!targetPresenterFlavor.toString().equalsIgnoreCase(targetPresentationFlavor.toString())) {
        Track[] targetPresentationTracks = mediaPackage.getTracks(targetPresentationFlavor);
        for (Track track : targetPresentationTracks) {
          if (forceEncoding || trackNeedsTobeEncodedToStandard(track, requiredExtensions)) {
            logger.debug("Encoding '{}' flavored track '{}' with standard encoding profile {}",
                    targetPresentationFlavor, track.getURI(), forceProfile.get());
            queueTime += encodeToStandard(mediaPackage, forceProfile.get(), targetPresentationFlavor, track);
            elementsToClean.add(track);
            mediaPackage.remove(track);
          }
        }
      }
    }
    return queueTime;
  }

  /**
   * This function creates a copy of a given track in the media package
   *
   * @param mediaPackage
   *          The media package being processed.
   * @param track
   *          The track we want to create a copy from.
   * @param targetFlavor
   *          The target flavor for the copy of the track.
   */
  private void createCopyOfTrack(MediaPackage mediaPackage, Track track, MediaPackageElementFlavor targetFlavor)
             throws IllegalArgumentException, NotFoundException,IOException {

    MediaPackageElementFlavor targetCopyFlavor = null;
    if (track.hasVideo()) {
      targetCopyFlavor = targetFlavor;
    } else {
      targetCopyFlavor = deriveAudioFlavor(targetFlavor);
    }
    logger.debug("Copying track {} with flavor {} using target flavor {}", track.getURI(), track.getFlavor(), targetCopyFlavor);
    copyPartialToSource(mediaPackage, targetCopyFlavor, track);
  }

  /**
   * This functions adjusts the target flavor for audio tracks.
   * While processing audio tracks, an audio suffix is appended to the type of the audio tracks target flavor.
   * This functions essentially removes that suffix again and therefore ensures that the target flavor of
   * audio tracks is set correctly.
   *
   * @param mediaPackage
   *          The media package to look for audio tracks.
   * @param targetFlavor
   *          The target flavor for the audio tracks.
   */
  private void adjustAudioTrackTargetFlavor(MediaPackage mediaPackage, MediaPackageElementFlavor targetFlavor)
             throws IllegalArgumentException, NotFoundException,IOException {

    Track[] targetAudioTracks = mediaPackage.getTracks(deriveAudioFlavor(targetFlavor));
    for (Track track : targetAudioTracks) {
      logger.debug("Adding {} to finished audio tracks.", track.getURI());
      mediaPackage.remove(track);
      track.setFlavor(targetFlavor);
      mediaPackage.add(track);
    }
  }

  private TrackSelector mkTrackSelector(Opt<String> flavor) throws WorkflowOperationException {
    final TrackSelector s = new TrackSelector();
    for (String fs : flavor) {
      try {
        final MediaPackageElementFlavor f = MediaPackageElementFlavor.parseFlavor(fs);
        s.addFlavor(f);
        s.addFlavor(deriveAudioFlavor(f));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Flavor '" + fs + "' is malformed");
      }
    }
    return s;
  }

  /**
   * Start job to concatenate a list of tracks.
   *
   * @param profile
   *          the encoding profile to use
   * @param tracks
   *          non empty track list
   * @param forceDivisible
   *          Whether to enforce the track's dimension to be divisible by two
   */
  protected Job startConcatJob(EncodingProfile profile, List<Track> tracks, float outputFramerate, boolean forceDivisible)
          throws MediaPackageException, EncoderException {
    final Dimension dim = determineDimension(tracks, forceDivisible);
    if (outputFramerate > 0.0) {
      return composerService.concat(profile.getIdentifier(), dim, outputFramerate, false, Collections.toArray(Track.class, tracks));
    } else {
      return composerService.concat(profile.getIdentifier(), dim, false, Collections.toArray(Track.class, tracks));
    }
  }

  /**
   * Determines if the extension of a track is non-standard and therefore should be re-encoded.
   *
   * @param track
   *          The track to check the extension on.
   */
  protected static boolean trackNeedsTobeEncodedToStandard(Track track, List<String> requiredExtensions) {
    String extension = FilenameUtils.getExtension(track.getURI().toString());
    for (String requiredExtension : requiredExtensions) {
      if (requiredExtension.equalsIgnoreCase(extension)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get the extensions from configuration that don't need to be re-encoded.
   *
   * @param operation
   *          The WorkflowOperationInstance to get the configuration from
   * @return The list of extensions
   */
  protected List<String> getRequiredExtensions(WorkflowOperationInstance operation) {
    List<String> requiredExtensions = new ArrayList<String>();
    String configExtensions = null;
    try {
      configExtensions = StringUtils.trimToNull(getConfig(operation, REQUIRED_EXTENSIONS));
    } catch (WorkflowOperationException e) {
      logger.info(
              "Required extensions configuration key not specified so will be using default '{}'. Any input file not matching this extension will be re-encoded.",
              DEFAULT_REQUIRED_EXTENSION);
    }
    if (configExtensions != null) {
      String[] extensions = configExtensions.split(",");
      for (String extension : extensions) {
        requiredExtensions.add(extension);
      }
    }
    if (requiredExtensions.size() == 0) {
      requiredExtensions.add(DEFAULT_REQUIRED_EXTENSION);
    }
    return requiredExtensions;
  }

  /**
   * Get the force encoding profile from the operations config options.
   *
   * @return the encoding profile if option "force-encoding" is true, none otherwise
   * @throws WorkflowOperationException
   *           if there is no such encoding profile or if no encoding profile is configured but force-encoding is true
   */
  protected Opt<EncodingProfile> getForceEncodingProfile(WorkflowOperationInstance woi)
          throws WorkflowOperationException {
    return getOptConfig(woi, FORCE_ENCODING_PROFILE).map(new Fn<String, EncodingProfile>() {
      @Override
      public EncodingProfile apply(String profileName) {
        for (EncodingProfile profile : Opt.nul(composerService.getProfile(profileName))) {
          return profile;
        }
        return chuck(new WorkflowOperationException("Force encoding profile '" + profileName + "' was not found"));
      }
    }).orError(new WorkflowOperationException("Force encoding profile must be set!"));
  }

  /**
   * @param flavorType
   *          either "presenter" or "presentation", just for error messages
   */
  private MediaPackageElementFlavor parseTargetFlavor(String flavor, String flavorType)
          throws WorkflowOperationException {
    final MediaPackageElementFlavor targetFlavor;
    try {
      targetFlavor = MediaPackageElementFlavor.parseFlavor(flavor);
      if ("*".equals(targetFlavor.getType()) || "*".equals(targetFlavor.getSubtype())) {
        throw new WorkflowOperationException(format(
                "Target %s flavor must have a type and a subtype, '*' are not allowed!", flavorType));
      }
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException(format("Target %s flavor '%s' is malformed", flavorType, flavor));
    }
    return targetFlavor;
  }

  /** Create a derived audio flavor by appending {@link #FLAVOR_AUDIO_SUFFIX} to the flavor type. */
  private MediaPackageElementFlavor deriveAudioFlavor(MediaPackageElementFlavor flavor) {
    return MediaPackageElementFlavor.flavor(flavor.getType().concat(FLAVOR_AUDIO_SUFFIX), flavor.getSubtype());
  }

  /**
   * Determine the largest dimension of the given list of tracks
   *
   * @param tracks
   *          the list of tracks
   * @param forceDivisible
   *          Whether to enforce the track's dimension to be divisible by two
   * @return the largest dimension from the list of track
   */
  private Dimension determineDimension(List<Track> tracks, boolean forceDivisible) {
    Tuple<Track, Dimension> trackDimension = getLargestTrack(tracks);
    if (trackDimension == null)
      return null;

    if (forceDivisible && (trackDimension.getB().getHeight() % 2 != 0 || trackDimension.getB().getWidth() % 2 != 0)) {
      Dimension scaledDimension = Dimension.dimension((trackDimension.getB().getWidth() / 2) * 2, (trackDimension
              .getB().getHeight() / 2) * 2);
      logger.info("Determined output dimension {} scaled down from {} for track {}", scaledDimension,
              trackDimension.getB(), trackDimension.getA());
      return scaledDimension;
    } else {
      logger.info("Determined output dimension {} for track {}", trackDimension.getB(), trackDimension.getA());
      return trackDimension.getB();
    }
  }

  /**
   * Returns the track with the largest resolution from the list of tracks
   *
   * @param tracks
   *          the list of tracks
   * @return a {@link Tuple} with the largest track and it's dimension
   */
  private Tuple<Track, Dimension> getLargestTrack(List<Track> tracks) {
    Track track = null;
    Dimension dimension = null;
    for (Track t : tracks) {
      if (!t.hasVideo())
        continue;

      VideoStream[] videoStreams = TrackSupport.byType(t.getStreams(), VideoStream.class);
      int frameWidth = videoStreams[0].getFrameWidth();
      int frameHeight = videoStreams[0].getFrameHeight();
      if (dimension == null || (frameWidth * frameHeight) > (dimension.getWidth() * dimension.getHeight())) {
        dimension = Dimension.dimension(frameWidth, frameHeight);
        track = t;
      }
    }
    if (track == null || dimension == null)
      return null;

    return Tuple.tuple(track, dimension);
  }

  private long checkForTrimming(MediaPackage mediaPackage, EncodingProfile trimProfile,
          MediaPackageElementFlavor targetFlavor, Float videoDuration, List<MediaPackageElement> elementsToClean)
          throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException,
          ServiceRegistryException, IOException {
    MediaPackageElement[] elements = mediaPackage.getElementsByFlavor(targetFlavor);
    if (elements.length == 0)
      return 0;

    Track trackToTrim = (Track) elements[0];
    if (elements.length == 1 && trackToTrim.getDuration() / 1000 > videoDuration) {
      Long trimSeconds = (long) (trackToTrim.getDuration() / 1000 - videoDuration);
      logger.info("Shorten track {} to target duration {} by {} seconds",
              trackToTrim.toString(), videoDuration.toString(), trimSeconds.toString());
      return trimEnd(mediaPackage, trimProfile, trackToTrim, videoDuration, elementsToClean);
    } else if (elements.length > 1) {
      logger.warn("Multiple tracks with flavor {} found! Trimming not possible!", targetFlavor);
    }
    return 0;
  }

  private List<Track> getPureVideoTracks(MediaPackage mediaPackage, MediaPackageElementFlavor videoFlavor) {
    return $(mediaPackage.getTracks()).filter(Filters.matchesFlavor(videoFlavor).toFn())
            .filter(Filters.hasVideo.toFn()).filter(Filters.hasNoAudio.toFn()).toList();
  }

  private List<Track> getPureAudioTracks(MediaPackage mediaPackage, MediaPackageElementFlavor audioFlavor) {
    return $(mediaPackage.getTracks()).filter(Filters.matchesFlavor(audioFlavor).toFn())
            .filter(Filters.hasAudio.toFn()).filter(Filters.hasNoVideo.toFn()).toList();
  }

  protected long checkForMuxing(MediaPackage mediaPackage, MediaPackageElementFlavor targetPresentationFlavor,
          MediaPackageElementFlavor targetPresenterFlavor, boolean useSuffix, List<MediaPackageElement> elementsToClean)
          throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException,
          ServiceRegistryException, IOException {

    long queueTime = 0L;

    List<Track> videoElements = getPureVideoTracks(mediaPackage, targetPresentationFlavor);
    List<Track> audioElements;
    if (useSuffix) {
      audioElements = getPureAudioTracks(mediaPackage, deriveAudioFlavor(targetPresentationFlavor));
    } else {
      audioElements = getPureAudioTracks(mediaPackage, targetPresentationFlavor);
    }

    Track videoTrack = null;
    Track audioTrack = null;

    if (videoElements.size() == 1 && audioElements.size() == 0) {
      videoTrack = videoElements.get(0);
    } else if (videoElements.size() == 0 && audioElements.size() == 1) {
      audioTrack = audioElements.get(0);
    }

    videoElements = getPureVideoTracks(mediaPackage, targetPresenterFlavor);
    if (useSuffix) {
      audioElements = getPureAudioTracks(mediaPackage, deriveAudioFlavor(targetPresenterFlavor));
    } else {
      audioElements = getPureAudioTracks(mediaPackage, targetPresenterFlavor);
    }

    if (videoElements.size() == 1 && audioElements.size() == 0) {
      videoTrack = videoElements.get(0);
    } else if (videoElements.size() == 0 && audioElements.size() == 1) {
      audioTrack = audioElements.get(0);
    }

    logger.debug("Check for mux between '{}' and '{}' flavors and found video track '{}' and audio track '{}'",
            targetPresentationFlavor, targetPresenterFlavor, videoTrack, audioTrack);
    if (videoTrack != null && audioTrack != null) {
      queueTime += mux(mediaPackage, videoTrack, audioTrack, elementsToClean);
      return queueTime;
    } else {
      return queueTime;
    }
  }

  /**
   * Mux a video and an audio track. Add the result to media package <code>mediaPackage</code> with the same flavor as
   * the <code>video</code>.
   *
   * @return the mux job's queue time
   */
  protected long mux(MediaPackage mediaPackage, Track video, Track audio, List<MediaPackageElement> elementsToClean)
          throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException,
          ServiceRegistryException, IOException {
    logger.debug("Muxing video {} and audio {}", video.getURI(), audio.getURI());
    Job muxJob = composerService.mux(video, audio, PrepareAVWorkflowOperationHandler.MUX_AV_PROFILE);
    if (!waitForStatus(muxJob).isSuccess()) {
      throw new WorkflowOperationException("Muxing of audio " + audio + " and video " + video + " failed");
    }
    muxJob = serviceRegistry.getJob(muxJob.getId());

    final Track muxed = (Track) MediaPackageElementParser.getFromXml(muxJob.getPayload());
    if (muxed == null) {
      throw new WorkflowOperationException("Muxed job " + muxJob + " returned no payload!");
    }
    muxed.setFlavor(video.getFlavor());
    muxed.setURI(workspace.moveTo(muxed.getURI(), mediaPackage.getIdentifier().toString(), muxed.getIdentifier(),
            FilenameUtils.getName(video.getURI().toString())));
    elementsToClean.add(audio);
    mediaPackage.remove(audio);
    elementsToClean.add(video);
    mediaPackage.remove(video);
    mediaPackage.add(muxed);
    return muxJob.getQueueTime();
  }

  private void copyPartialToSource(MediaPackage mediaPackage, MediaPackageElementFlavor targetFlavor, Track track)
          throws NotFoundException, IOException {
    FileInputStream in = null;
    try {
      Track copyTrack = (Track) track.clone();
      File originalFile = workspace.get(copyTrack.getURI());
      in = new FileInputStream(originalFile);

      String elementID = UUID.randomUUID().toString();
      copyTrack.setURI(workspace.put(mediaPackage.getIdentifier().toString(), elementID,
              FilenameUtils.getName(copyTrack.getURI().toString()), in));
      copyTrack.setFlavor(targetFlavor);
      copyTrack.setIdentifier(elementID);
      copyTrack.referTo(track);
      mediaPackage.add(copyTrack);
      logger.info("Copied partial source element {} to {} with target flavor {}", track.toString(),
              copyTrack.toString(), targetFlavor.toString());
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Encode <code>track</code> using encoding profile <code>profile</code> and add the result to media package
   * <code>mp</code> under the given <code>targetFlavor</code>.
   *
   * @return the encoder job's queue time
   */
  private long encodeToStandard(MediaPackage mp, EncodingProfile profile, MediaPackageElementFlavor targetFlavor,
          Track track) throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException,
          ServiceRegistryException, IOException {
    Job encodeJob = composerService.encode(track, profile.getIdentifier());
    if (!waitForStatus(encodeJob).isSuccess()) {
      throw new WorkflowOperationException("Encoding of track " + track + " failed");
    }
    encodeJob = serviceRegistry.getJob(encodeJob.getId());
    Track encodedTrack = (Track) MediaPackageElementParser.getFromXml(encodeJob.getPayload());
    if (encodedTrack == null) {
      throw new WorkflowOperationException("Encoded track " + track + " failed to produce a track");
    }
    URI uri;
    if (FilenameUtils.getExtension(encodedTrack.getURI().toString()).equalsIgnoreCase(
            FilenameUtils.getExtension(track.getURI().toString()))) {
      uri = workspace.moveTo(encodedTrack.getURI(), mp.getIdentifier().compact(), encodedTrack.getIdentifier(),
              FilenameUtils.getName(track.getURI().toString()));
    } else {
      // The new encoded file has a different extension.
      uri = workspace.moveTo(
              encodedTrack.getURI(),
              mp.getIdentifier().compact(),
              encodedTrack.getIdentifier(),
              FilenameUtils.getBaseName(track.getURI().toString()) + "."
                      + FilenameUtils.getExtension(encodedTrack.getURI().toString()));
    }
    encodedTrack.setURI(uri);
    encodedTrack.setFlavor(targetFlavor);
    mp.add(encodedTrack);
    return encodeJob.getQueueTime();
  }

  private long trimEnd(MediaPackage mediaPackage, EncodingProfile trimProfile, Track track, double duration,
          List<MediaPackageElement> elementsToClean) throws EncoderException, MediaPackageException,
          WorkflowOperationException, NotFoundException, ServiceRegistryException, IOException {
    Job trimJob = composerService.trim(track, trimProfile.getIdentifier(), 0, (long) (duration * 1000));
    if (!waitForStatus(trimJob).isSuccess())
      throw new WorkflowOperationException("Trimming of track " + track + " failed");

    trimJob = serviceRegistry.getJob(trimJob.getId());

    Track trimmedTrack = (Track) MediaPackageElementParser.getFromXml(trimJob.getPayload());
    if (trimmedTrack == null)
      throw new WorkflowOperationException("Trimming track " + track + " failed to produce a track");

    URI uri = workspace.moveTo(trimmedTrack.getURI(), mediaPackage.getIdentifier().compact(),
            trimmedTrack.getIdentifier(), FilenameUtils.getName(track.getURI().toString()));
    trimmedTrack.setURI(uri);
    trimmedTrack.setFlavor(track.getFlavor());

    elementsToClean.add(track);
    mediaPackage.remove(track);
    mediaPackage.add(trimmedTrack);

    return trimJob.getQueueTime();
  }

  private long processChildren(long position, List<Track> tracks, NodeList children, List<Track> originalTracks,
          VCell<String> type, String mediaType, List<MediaPackageElement> elementsToClean, Long operationId)
          throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException, IOException {
    for (int j = 0; j < children.getLength(); j++) {
      Node item = children.item(j);
      if (item.hasChildNodes()) {
        position = processChildren(position, tracks, item.getChildNodes(), originalTracks, type, mediaType,
                elementsToClean, operationId);
      } else {
        SMILMediaElement e = (SMILMediaElement) item;
        if (mediaType.equals(e.getNodeName())) {
          Track track = getFromOriginal(e.getId(), originalTracks, type);
          double beginInSeconds = e.getBegin().item(0).getResolvedOffset();
          long beginInMs = Math.round(beginInSeconds * 1000d);
          // Fill out gaps with first or last frame from video
          if (beginInMs > position) {
            double positionInSeconds = position / 1000d;
            if (position == 0) {
              if (NODE_TYPE_AUDIO.equals(e.getNodeName())) {
                logger.info("Extending {} audio track start by {} seconds silent audio", type.get(), beginInSeconds);
                tracks.add(getSilentAudio(beginInSeconds, elementsToClean, operationId));
              } else {
                logger.info("Extending {} track start image frame by {} seconds", type.get(), beginInSeconds);
                Attachment tempFirstImageFrame = extractImage(track, 0, elementsToClean);
                tracks.add(createVideoFromImage(tempFirstImageFrame, beginInSeconds, elementsToClean));
              }
              position += beginInMs;
            } else {
              double fillTime = (beginInMs - position) / 1000d;
              if (NODE_TYPE_AUDIO.equals(e.getNodeName())) {
                logger.info("Fill {} audio track gap from {} to {} with silent audio", type.get(),
                        Double.toString(positionInSeconds), Double.toString(beginInSeconds));
                tracks.add(getSilentAudio(fillTime, elementsToClean, operationId));
              } else {
                logger.info("Fill {} track gap from {} to {} with image frame",
                        type.get(), Double.toString(positionInSeconds), Double.toString(beginInSeconds));
                Track previousTrack = tracks.get(tracks.size() - 1);
                Attachment tempLastImageFrame = extractLastImageFrame(previousTrack, elementsToClean);
                tracks.add(createVideoFromImage(tempLastImageFrame, fillTime, elementsToClean));
              }
              position = beginInMs;
            }
          }
          tracks.add(track);
          position += Math.round(e.getDur() * 1000f);
        }
      }
    }
    return position;
  }

  private Track getFromOriginal(String trackId, List<Track> originalTracks, VCell<String> type) {
    for (Track t : originalTracks) {
      if (t.getIdentifier().contains(trackId)) {
        logger.debug("Track-Id from smil found in Mediapackage ID: " + t.getIdentifier());
        if (EMPTY_VALUE.equals(type.get())) {
          String suffix = (t.hasAudio() && !t.hasVideo()) ? FLAVOR_AUDIO_SUFFIX : "";
          type.set(t.getFlavor().getType() + suffix);
        }
        originalTracks.remove(t);
        return t;
      }
    }
    throw new IllegalStateException("No track matching smil Track-id: " + trackId);
  }

  private Track getSilentAudio(final double time, final List<MediaPackageElement> elementsToClean,
          final Long operationId) throws EncoderException, MediaPackageException, WorkflowOperationException,
          NotFoundException, IOException {
    final URI uri = workspace.putInCollection(COLLECTION_ID, operationId + "-silent", new ByteArrayInputStream(
            EMPTY_VALUE.getBytes()));
    final Attachment emptyAttachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(uri, Type.Attachment, MediaPackageElementFlavor.parseFlavor("audio/silent"));
    elementsToClean.add(emptyAttachment);

    final Job silentAudioJob = composerService.imageToVideo(emptyAttachment, SILENT_AUDIO_PROFILE, time);
    if (!waitForStatus(silentAudioJob).isSuccess())
      throw new WorkflowOperationException("Silent audio job did not complete successfully");

    // Get the latest copy
    try {
      for (final String payload : getPayload(serviceRegistry, silentAudioJob)) {
        final Track silentAudio = (Track) MediaPackageElementParser.getFromXml(payload);
        elementsToClean.add(silentAudio);
        return silentAudio;
      }
      // none
      throw new WorkflowOperationException(format("Job %s has no payload or cannot be updated", silentAudioJob));
    } catch (ServiceRegistryException ex) {
      throw new WorkflowOperationException(ex);
    }
  }

  private Track createVideoFromImage(Attachment image, double time, List<MediaPackageElement> elementsToClean)
          throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException {
    Job imageToVideoJob = composerService.imageToVideo(image, IMAGE_MOVIE_PROFILE, time);
    if (!waitForStatus(imageToVideoJob).isSuccess())
      throw new WorkflowOperationException("Image to video job did not complete successfully");

    // Get the latest copy
    try {
      imageToVideoJob = serviceRegistry.getJob(imageToVideoJob.getId());
    } catch (ServiceRegistryException e) {
      throw new WorkflowOperationException(e);
    }
    Track imageVideo = (Track) MediaPackageElementParser.getFromXml(imageToVideoJob.getPayload());
    elementsToClean.add(imageVideo);
    return imageVideo;
  }

  private Attachment extractImage(Track presentationTrack, double time, List<MediaPackageElement> elementsToClean)
          throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException {
    Job extractImageJob = composerService.image(presentationTrack, PREVIEW_PROFILE, time);
    if (!waitForStatus(extractImageJob).isSuccess())
      throw new WorkflowOperationException("Extract image frame video job did not complete successfully");

    // Get the latest copy
    try {
      extractImageJob = serviceRegistry.getJob(extractImageJob.getId());
    } catch (ServiceRegistryException e) {
      throw new WorkflowOperationException(e);
    }
    Attachment composedImages = (Attachment) MediaPackageElementParser.getArrayFromXml(extractImageJob.getPayload())
            .get(0);
    elementsToClean.add(composedImages);
    return composedImages;
  }

  private Attachment extractLastImageFrame(Track presentationTrack, List<MediaPackageElement> elementsToClean)
          throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException {
    VideoStream[] videoStreams = TrackSupport.byType(presentationTrack.getStreams(), VideoStream.class);
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("frame", Long.toString(videoStreams[0].getFrameCount() - 1));

    Job extractImageJob = composerService.image(presentationTrack, IMAGE_FRAME_PROFILE, properties);
    if (!waitForStatus(extractImageJob).isSuccess())
      throw new WorkflowOperationException("Extract image frame video job did not complete successfully");

    // Get the latest copy
    try {
      extractImageJob = serviceRegistry.getJob(extractImageJob.getId());
    } catch (ServiceRegistryException e) {
      throw new WorkflowOperationException(e);
    }
    Attachment composedImages = (Attachment) MediaPackageElementParser.getArrayFromXml(extractImageJob.getPayload())
            .get(0);
    elementsToClean.add(composedImages);
    return composedImages;
  }
}
