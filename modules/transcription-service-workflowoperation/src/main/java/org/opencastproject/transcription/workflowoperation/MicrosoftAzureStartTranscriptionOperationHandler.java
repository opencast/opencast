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

package org.opencastproject.transcription.workflowoperation;

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
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.transcription.api.TranscriptionServiceException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Start Transcription Workflow Operation Handler (Microsoft Azure)",
        "workflow.operation=microsoft-azure-start-transcription"
    }
)
public class MicrosoftAzureStartTranscriptionOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureStartTranscriptionOperationHandler.class);

  /** Workflow configuration option keys */
  static final String LANGUAGE_KEY = "language";
  static final String SKIP_IF_FLAVOR_EXISTS_KEY = "skip-if-flavor-exists";
  static final String EXTRACT_AUDIO_ENCODING_PROFILE_KEY = "audio-extraction-encoding-profile";
  private static final String DEFAULT_EXTRACT_AUDIO_ENCODING_PROFILE = "transcription-azure.audio";

  /** The transcription service */
  private TranscriptionService service = null;
  /** The composer service. */
  private ComposerService composerService;
  /** The workspace. */
  private Workspace workspace;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_FLAVORS, "The flavors of the tracks to use as audio input. "
        + "Only the first available track will be used.");
    CONFIG_OPTIONS.put(SOURCE_TAGS, "The tags of the track to use as audio input.");
    CONFIG_OPTIONS.put(LANGUAGE_KEY, "The language the transcription service should use.");
    CONFIG_OPTIONS.put(SKIP_IF_FLAVOR_EXISTS_KEY,
        "If this \"flavor\" is already in the media package, skip this operation.");
    CONFIG_OPTIONS.put(EXTRACT_AUDIO_ENCODING_PROFILE_KEY,
        "The encoding profile to extract audio for transcription.");
  }

  @Override
  @Activate
  protected void activate(ComponentContext cc) {
    super.activate(cc);
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    String skipOption = StringUtils.trimToNull(operation.getConfiguration(SKIP_IF_FLAVOR_EXISTS_KEY));
    if (skipOption != null) {
      SimpleElementSelector elementSelector = new SimpleElementSelector();
      for (String flavorStr : StringUtils.split(skipOption, ",")) {
        if (StringUtils.trimToNull(flavorStr) == null) {
          continue;
        }
        MediaPackageElementFlavor skipFlavor = MediaPackageElementFlavor.parseFlavor(
            StringUtils.trimToEmpty(flavorStr));
        elementSelector.addFlavor(skipFlavor);
      }
      if (!elementSelector.select(mediaPackage, false).isEmpty()) {
        logger.info("Start transcription operation will be skipped for media package '{}' "
            + "because elements with given flavor already exist.", mediaPackage.getIdentifier());
        return createResult(Action.SKIP);
      }
    }
    String encodingProfile = StringUtils.trimToNull(operation.getConfiguration(EXTRACT_AUDIO_ENCODING_PROFILE_KEY));
    if (encodingProfile == null) {
      encodingProfile = DEFAULT_EXTRACT_AUDIO_ENCODING_PROFILE;
    }
    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(
        workflowInstance, Configuration.many, Configuration.many, Configuration.none, Configuration.none);
    List<MediaPackageElementFlavor> sourceFlavorsOption = tagsAndFlavors.getSrcFlavors();
    List<String> sourceTagsOption = tagsAndFlavors.getSrcTags();
    String language = StringUtils.trimToEmpty(operation.getConfiguration(LANGUAGE_KEY));
    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();
    // Make sure either one of tags or flavors are provided
    if (sourceTagsOption.isEmpty() && sourceFlavorsOption.isEmpty()) {
      throw new WorkflowOperationException("No source tag or flavor have been specified!");
    }
    if (!sourceFlavorsOption.isEmpty()) {
      for (MediaPackageElementFlavor srcFlavor : sourceFlavorsOption) {
        elementSelector.addFlavor(srcFlavor);
      }
    }
    if (!sourceTagsOption.isEmpty()) {
      for (String srcTag : sourceTagsOption) {
        elementSelector.addTag(StringUtils.trimToEmpty(srcTag));
      }
    }
    Collection<Track> elements = elementSelector.select(mediaPackage, false);
    if (elements.isEmpty()) {
      logger.info("Media package {} does not contain elements to transcribe. Skip operation.",
          mediaPackage.getIdentifier());
      return createResult(Action.SKIP);
    }
    logger.info("Start transcription for media package '{}'.", mediaPackage.getIdentifier());
    Track audioTrack = null;
    try {
      for (Track track : elements) {
        if (!track.hasAudio()) {
          continue;
        }
        try {
          EncodingProfile profile = composerService.getProfile(encodingProfile);
          if (profile == null) {
            throw new WorkflowOperationException("Encoding profile '" + encodingProfile + "' was not found.");
          }
          Job encodeJob = composerService.encode(track, encodingProfile);
          if (!waitForStatus(encodeJob).isSuccess()) {
            throw new WorkflowOperationException(String.format(
                "Audio extraction job for track %s did not complete successfully.", track.getURI()));
          }
          audioTrack = (Track) MediaPackageElementParser.getFromXml(encodeJob.getPayload());
        } catch (EncoderException | MediaPackageException e) {
          throw new WorkflowOperationException(
              String.format("Extracting audio for transcription failed for the track %s", track.getURI()), e);
        }
        try {
          Job transcriptionJob = service.startTranscription(mediaPackage.getIdentifier().toString(), audioTrack,
              language);
          // Wait for the jobs to return
          if (!waitForStatus(transcriptionJob).isSuccess()) {
            throw new WorkflowOperationException("Transcription job did not complete successfully.");
          }
          // Return OK means that the transcription job was created, but not finished yet
          logger.debug("External transcription job for media package '{}' was created.", mediaPackage.getIdentifier());
          // Only one job per media package
          // Results are empty, we should get a callback when transcription is done
          return createResult(Action.CONTINUE);
        } catch (TranscriptionServiceException e) {
          throw new WorkflowOperationException(e);
        }
      }
    } finally {
      // We do not need the audio file anymore, delete it...
      deleteTrack(audioTrack);
    }
    // If we are here, no audio tracks are found. Skip operation.
    logger.info("Media package {} does not contain audio stream to transcribe. Skip operation.",
        mediaPackage.getIdentifier());
    return createResult(Action.SKIP);
  }

  protected void deleteTrack(Track track) {
    if (track != null && track.getURI() != null) {
      try {
        workspace.delete(track.getURI());
      } catch (NotFoundException ex) {
        // do nothing
      } catch (IOException ex) {
        logger.warn("Unable to delete file {}", track.getURI());
      }
    }
  }

  @Reference(target = "(provider=microsoft.azure)")
  public void setTranscriptionService(TranscriptionService service) {
    this.service = service;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

  @Reference
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
