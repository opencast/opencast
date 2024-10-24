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
package org.opencastproject.workflow.handler.speechtotext;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.speechtotext.api.SpeechToTextService;
import org.opencastproject.speechtotext.api.SpeechToTextServiceException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Workflow operation for the speech-to-text service.
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Speech-to-Text Workflow Operation Handler",
        "workflow.operation=speechtotext"
    }
)
public class
    SpeechToTextWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextWorkflowOperationHandler.class);

  /** Speech to Text language configuration property name. */
  private static final String LANGUAGE_CODE = "language-code";

  /** Speech to Text language fallback configuration property name. */
  private static final String LANGUAGE_FALLBACK = "language-fallback";

  /** Property name for configuring the place where the subtitles shall be appended. */
  private static final String TARGET_ELEMENT = "target-element";

  /** Language placeholder */
  private static final String PLACEHOLDER_LANG = "#{lang}";

  /** Translation mode */
  private static final String TRANSLATE_MODE = "translate";

  /** Configuration: Track Selection Strategy (Control which tracks shall be transcribed) */
  private static final String TRACK_SELECTION_STRATEGY = "track-selection-strategy";

  /** Configuration: Limit to One (If true, max 1 subtitle file will be generated) */
  private static final String LIMIT_TO_ONE = "limit-to-one";

  /** Configuration: Synchronous or asynchronous mode */
  private static final String ASYNCHRONOUS = "async";

  /** Workflow configuration name to store jobs in */
  private static final String JOBS_WORKFLOW_CONFIGURATION = "speech-to-text-jobs";

  private enum TrackSelectionStrategy {
    PRESENTER_OR_NOTHING,
    PRESENTATION_OR_NOTHING,
    TRY_PRESENTER_FIRST,
    TRY_PRESENTATION_FIRST,
    EVERYTHING;

    private static TrackSelectionStrategy fromString(String value) {
      for (TrackSelectionStrategy strategy : values()) {
        if (strategy.name().equalsIgnoreCase(value)) {
          return strategy;
        }
      }
      throw new IllegalArgumentException(
          "No TrackSelectionStrategy enum constant " + TrackSelectionStrategy.class.getCanonicalName() + "." + value);
    }
  }

  private enum AppendSubtitleAs {
    attachment, track
  }

  /** The speech-to-text service. */
  private SpeechToTextService speechToTextService = null;

  /** The workspace service. */
  private Workspace workspace;

  /** The inspection service. */
  private MediaInspectionService mediaInspectionService;

  /** The dublin core catalog service. */
  private DublinCoreCatalogService dublinCoreCatalogService;

  @Override
  @Activate
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering speech-to-text workflow operation handler");
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   * org.opencastproject.job.api.JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    logger.info("Start speech-to-text workflow operation for media package {}", mediaPackage);

    // Defaults to `false` if `null`
    var async = BooleanUtils.toBoolean(workflowInstance.getCurrentOperation().getConfiguration(ASYNCHRONOUS));

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
            Configuration.none, Configuration.one,
            Configuration.many, Configuration.one);
    MediaPackageElementFlavor sourceFlavor = tagsAndFlavors.getSingleSrcFlavor();

    TrackSelector trackSelector = new TrackSelector();
    trackSelector.addFlavor(sourceFlavor);
    Collection<Track> tracks = trackSelector.select(mediaPackage, false);

    if (tracks.isEmpty()) {
      throw new WorkflowOperationException(
              String.format("No tracks with source flavor '%s' found for transcription", sourceFlavor));
    }

    logger.info("Found {} track(s) with source flavor '{}'.", tracks.size(), sourceFlavor);

    // Get the information in which language the audio track should be
    String languageCode = getMediaPackageLanguage(mediaPackage, workflowInstance);

    // How to save the subtitle file? (as attachment, as track...)
    AppendSubtitleAs appendSubtitleAs = howToAppendTheSubtitles(workflowInstance);

    // Translate to english
    Boolean translate = getTranslationMode(workflowInstance);

    // Create sublist that includes only the tracks that has audio
    List<Track> tracksWithAudio = tracks.stream().filter(Track::hasAudio).collect(Collectors.toList());

    // Get the track selection strategy from the workflow configuration
    // If nothing is set, all tracks (with audio) will be transcribed
    TrackSelectionStrategy trackSelectionStrategy = getTrackSelectionStrategy(mediaPackage, workflowInstance);

    // Use the selection strategy from the workflow config to get the tracks we want to transcribe
    List<Track> tracksToTranscribe = filterTracksByStrategy(tracksWithAudio, trackSelectionStrategy);
    if (tracksToTranscribe.isEmpty()) {
      logger.info("No subtitles were created for media package {}. "
          + "Workflow Configuration 'track-selection-strategy' is set to {}", mediaPackage, trackSelectionStrategy);
      return createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
    }

    // Load the 'limit-to-one' configuration from the workflow operation.
    // This configuration sets the limit of generated subtitle files to one
    boolean limitToOne = BooleanUtils.toBoolean(workflowInstance.getCurrentOperation().getConfiguration(LIMIT_TO_ONE));
    if (limitToOne) {
      tracksToTranscribe = List.of(tracksToTranscribe.get(0));
    }

    if (async) {
      createSubtitleAsync(workflowInstance, tracksToTranscribe, languageCode, translate);
    } else {
      for (Track track : tracksToTranscribe) {
        createSubtitle(track, languageCode, mediaPackage, tagsAndFlavors, appendSubtitleAs, translate);
      }
    }

    logger.info("Speech-To-Text workflow operation for media package {} completed", mediaPackage);
    return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
  }

  /**
   * Filters the tracks by the strategy configured in the workflow operation
   * @param tracksWithAudio        List of the tracks that includes audio
   * @param trackSelectionStrategy The strategy configured in the workflow operation
   * @return The filtered tracks
   */
  private List<Track> filterTracksByStrategy(List<Track> tracksWithAudio,
      TrackSelectionStrategy trackSelectionStrategy) {

    List<Track> tracksToTranscribe = new ArrayList<>();
    if (!tracksWithAudio.isEmpty()) {

      String presenterTypeConstant = MediaPackageElements.PRESENTER_SOURCE.getType();
      String presentationTypeConstant = MediaPackageElements.PRESENTATION_SOURCE.getType();

      // Creates a sublist only including the presenter tracks
      List<Track> presenterTracksWithAudio = tracksWithAudio.stream()
          .filter(track -> Objects.equals(track.getFlavor().getType(), presenterTypeConstant))
          .collect(Collectors.toList());

      // Creates a sublist only including the presentation tracks
      List<Track> presentationTracksWithAudio = tracksWithAudio.stream()
          .filter(track -> Objects.equals(track.getFlavor().getType(), presentationTypeConstant))
          .collect(Collectors.toList());

      if (TrackSelectionStrategy.PRESENTER_OR_NOTHING.equals(trackSelectionStrategy)) {
        tracksToTranscribe.addAll(presenterTracksWithAudio);
      }

      if (TrackSelectionStrategy.PRESENTATION_OR_NOTHING.equals(trackSelectionStrategy)) {
        tracksToTranscribe.addAll(presentationTracksWithAudio);
      }

      if (TrackSelectionStrategy.TRY_PRESENTER_FIRST.equals(trackSelectionStrategy)) {
        tracksToTranscribe.addAll(presenterTracksWithAudio);
        if (tracksToTranscribe.isEmpty()) {
          tracksToTranscribe.addAll(tracksWithAudio);
        }
      }

      if (TrackSelectionStrategy.TRY_PRESENTATION_FIRST.equals(trackSelectionStrategy)) {
        tracksToTranscribe.addAll((presentationTracksWithAudio));
        if (tracksToTranscribe.isEmpty()) {
          tracksToTranscribe.addAll(tracksWithAudio);
        }
      }

      if (TrackSelectionStrategy.EVERYTHING.equals(trackSelectionStrategy)) {
        tracksToTranscribe.addAll(tracksWithAudio);
      }
    }
    return tracksToTranscribe;
  }

  /**
   * Creates the subtitle file for a track and appends it to the media package.
   *
   * @param track The track from which the subtitles are created.
   * @param languageCode The language of the track.
   * @param parentMediaPackage The media package where the track is located.
   * @param tagsAndFlavors Tags and flavors instance (to get target flavor information)
   * @param appendSubtitleAs Tells how the subtitles file has to be appended.
   * @param translate Enable translation to english.
   * @throws WorkflowOperationException Get thrown if an error occurs.
   */
  private void createSubtitle(Track track, String languageCode, MediaPackage parentMediaPackage,
          ConfiguredTagsAndFlavors tagsAndFlavors, AppendSubtitleAs appendSubtitleAs, Boolean translate)
          throws WorkflowOperationException {

    // Start the transcription job, create subtitles file
    URI trackURI = track.getURI();

    Job job;
    logger.info("Generating subtitle for '{}'...", trackURI);
    try {
      job = speechToTextService.transcribe(trackURI, languageCode, translate);
    } catch (SpeechToTextServiceException e) {
      throw new WorkflowOperationException(
              String.format("Generating subtitles for '%s' in media package '%s' failed",
                      trackURI, parentMediaPackage), e);
    }

    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException(
              String.format("Speech-to-Text job for media package '%s' failed", parentMediaPackage));
    }

    // subtitles file is generated now, put it into the media package
    try {
      String[] jobOutput = job.getPayload().split(",");
      URI output = new URI(jobOutput[0]);
      String outputLanguage = jobOutput[1];
      String engineType = jobOutput[2];

      String mediaPackageIdentifier = UUID.randomUUID().toString();

      MediaPackageElement subtitleMediaPackageElement;
      switch (appendSubtitleAs) {
        case attachment:
          subtitleMediaPackageElement = new AttachmentImpl();
          break;
        case track:
        default:
          subtitleMediaPackageElement = new TrackImpl();
      }

      subtitleMediaPackageElement.setIdentifier(mediaPackageIdentifier);
      try (InputStream in = workspace.read(output)) {
        URI uri = workspace.put(parentMediaPackage.getIdentifier().toString(), mediaPackageIdentifier,
                FilenameUtils.getName(output.getPath()), in);
        subtitleMediaPackageElement.setURI(uri);
      }
      MediaPackageElementFlavor targetFlavor = tagsAndFlavors.getSingleTargetFlavor().applyTo(track.getFlavor());
      subtitleMediaPackageElement.setFlavor(targetFlavor);

      List<String> targetTags = tagsAndFlavors.getTargetTags();
      targetTags.add("lang:" + outputLanguage);
      targetTags.add("generator-type:auto");
      targetTags.add("generator:" + engineType.toLowerCase());

      // this is used to set some values automatically, like the correct mimetype
      Job inspection = mediaInspectionService.enrich(subtitleMediaPackageElement, true);
      if (!waitForStatus(inspection).isSuccess()) {
        throw new SpeechToTextServiceException(String.format(
                "Transcription for '%s' failed at enriching process", trackURI));
      }

      subtitleMediaPackageElement = MediaPackageElementParser.getFromXml(inspection.getPayload());

      for (String tag : targetTags) {
        subtitleMediaPackageElement.addTag(tag);
      }

      parentMediaPackage.add(subtitleMediaPackageElement);

      workspace.delete(output);
    } catch (Exception e) {
      throw new WorkflowOperationException("Error handling text-to-speech service output", e);
    }

    try {
      workspace.cleanup(parentMediaPackage.getIdentifier());
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Start the transcription, but don't actually wait for the process to finish. Instead, let the jobs run
   * asynchronously and just store the launched jobs in the workflow configuration.
   * @param workflow Workflow instance to store the jobs in
   * @param tracks Tracks to run the transcription on
   * @param languageCode Language to use
   * @param translate If the transcription should be translated
   * @throws WorkflowOperationException
   */
  private void createSubtitleAsync(WorkflowInstance workflow, List<Track> tracks, String languageCode,
      Boolean translate) throws WorkflowOperationException {

    logger.info("Asynchronously generating subtitles");
    StringBuilder jobs = new StringBuilder();
    try {
      for (var track: tracks) {
        var job = speechToTextService.transcribe(track.getURI(), languageCode, translate);
        jobs.append(",").append(job.getId());
      }
    } catch (SpeechToTextServiceException e) {
      throw new WorkflowOperationException(
          String.format("Starting subtitle job in media package '%s' failed",
              workflow.getMediaPackage().getIdentifier()), e);
    }

    var config = Objects.toString(workflow.getConfiguration(JOBS_WORKFLOW_CONFIGURATION), "") + jobs;
    workflow.setConfiguration(JOBS_WORKFLOW_CONFIGURATION, config.replaceFirst("^,", ""));
  }

  /**
   * Get the config for the "track selection strategy". It's used to determine which tracks shall be transcribed.
   * If there are 2 Videos and both has audio for example, what audio shall be transcribed?
   *
   * @param workflowInstance Contains the workflow configuration.
   * @return Which strategy to use
   * @throws WorkflowOperationException Get thrown if an error occurs.
   */
  private TrackSelectionStrategy getTrackSelectionStrategy(MediaPackage mediaPackage, WorkflowInstance workflowInstance)
          throws WorkflowOperationException {

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    String strategyCfg = StringUtils.trimToEmpty(operation.getConfiguration(TRACK_SELECTION_STRATEGY)).toLowerCase();

    if (strategyCfg.isEmpty()) {
      return TrackSelectionStrategy.EVERYTHING; // "transcribe everything" is the default/fallback
    }
    try {
      return TrackSelectionStrategy.fromString(strategyCfg);
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException(String.format(
          "Speech-to-Text job for media package '%s' failed, because of wrong workflow configuration. "
              + "track-selection-strategy of type '%s' does not exist.", mediaPackage, strategyCfg));
    }
  }


  /**
   * Get the information how to append the subtitles file to the media package.
   *
   * @param workflowInstance Contains the workflow configuration.
   * @return How to append the subtitles file to the media package.
   * @throws WorkflowOperationException Get thrown if an error occurs.
   */
  private AppendSubtitleAs howToAppendTheSubtitles(WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    String targetElement = StringUtils.trimToEmpty(operation.getConfiguration(TARGET_ELEMENT)).toLowerCase();
    if (targetElement.isEmpty()) {
      return AppendSubtitleAs.track;
    }
    try {
      return AppendSubtitleAs.valueOf(targetElement);
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException(String.format(
          "Speech-to-Text job for media package '%s' failed, because of wrong workflow configuration. "
              + "target-element of type '%s' does not exist.", workflowInstance.getMediaPackage(), targetElement));
    }
  }

  /**
   * Get if the subtitle needs to be translated into english
   *
   * @param workflowInstance Contains the workflow configuration
   * @return Boolean to enable english translation
   */
  private Boolean getTranslationMode(WorkflowInstance workflowInstance) {
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    return BooleanUtils.toBoolean(StringUtils.trimToEmpty(operation.getConfiguration(TRANSLATE_MODE)));
  }

  /**
   * Searches some places to get the right language of the media package / track.
   *
   * @param mediaPackage The media package from which the subtitles are generated.
   * @param workflowInstance Contains the workflow configuration.
   * @return The language of the media package / track.
   */
  private String getMediaPackageLanguage(MediaPackage mediaPackage, WorkflowInstance workflowInstance) {

    // First look if there is a fixed language configured in the operation
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    String language = StringUtils.trimToEmpty(operation.getConfiguration(LANGUAGE_CODE));

    if (language.isEmpty()) {
      // If not we look in the dublin core metadata if the language is available
      MediaPackageMetadata dublinCoreMetadata = dublinCoreCatalogService.getMetadata(mediaPackage);
      language = StringUtils.trimToEmpty(dublinCoreMetadata.getLanguage());
    }

    if (language.isEmpty()) {
      // If there is still no language, we look in the media package itself
      language = StringUtils.trimToEmpty(mediaPackage.getLanguage());
    }

    if (language.isEmpty()) {
      // If nothing helps, use the fallback language
      language = Objects.toString(operation.getConfiguration(LANGUAGE_FALLBACK), "en");
    }

    return language;
  }


  //================================================================================
  // OSGi setter
  //================================================================================

  @Reference
  public void setSpeechToTextService(SpeechToTextService speechToTextService) {
    this.speechToTextService = speechToTextService;
  }

  @Reference
  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.mediaInspectionService = mediaInspectionService;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  public void setDublinCoreCatalogService(DublinCoreCatalogService dublinCoreCatalogService) {
    this.dublinCoreCatalogService = dublinCoreCatalogService;
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }
}
