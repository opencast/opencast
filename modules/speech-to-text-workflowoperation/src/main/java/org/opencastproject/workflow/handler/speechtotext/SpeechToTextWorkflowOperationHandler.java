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
package org.opencastproject.workflow.handler.speechtotext;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
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
import java.util.List;
import java.util.UUID;

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
public class SpeechToTextWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextWorkflowOperationHandler.class);

  /** Speech to Text language configuration property name. */
  private static final String LANGUAGE_CODE = "language-code";

  /** Property name for configuring the place where the subtitles shall be appended. */
  private static final String TARGET_ELEMENT = "target-element";

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

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
            Configuration.none, Configuration.one,
            Configuration.many, Configuration.one);
    MediaPackageElementFlavor sourceFlavor = tagsAndFlavors.getSingleSrcFlavor();

    Track[] tracks = mediaPackage.getTracks(sourceFlavor);
    if (tracks.length == 0) {
      throw new WorkflowOperationException(
              String.format("No tracks with source flavor '%s' found for transcription", sourceFlavor));
    }

    logger.info("Found {} track(s) with source flavor '{}'.", tracks.length, sourceFlavor);

    // Get the information in which language the audio track should be
    String languageCode = getMediaPackageLanguage(mediaPackage, workflowInstance);

    // How to save the subtitle file? (as attachment, as track...)
    AppendSubtitleAs appendSubtitleAs = howToAppendTheSubtitles(mediaPackage, workflowInstance);

    for (Track track : tracks) {
      createSubtitle(track, languageCode, mediaPackage, tagsAndFlavors, appendSubtitleAs);
    }

    logger.info("Text-to-Speech workflow operation for media package {} completed", mediaPackage);
    return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
  }

  /**
   * Creates the subtitle file for a track and appends it to the media package.
   *
   * @param track The track from which the subtitles are created.
   * @param languageCode The language of the track.
   * @param parentMediaPackage The media package where the track is located.
   * @param tagsAndFlavors Tags and flavors instance (to get target flavor information)
   * @param appendSubtitleAs Tells how the subtitles file has to be appended.
   * @throws WorkflowOperationException Get thrown if an error occurs.
   */
  private void createSubtitle(Track track, String languageCode, MediaPackage parentMediaPackage,
          ConfiguredTagsAndFlavors tagsAndFlavors, AppendSubtitleAs appendSubtitleAs)
          throws WorkflowOperationException {

    // Start the transcription job, create subtitles file
    URI trackURI = track.getURI();
    Job job;
    logger.info("Generating subtitle for '{}'...", trackURI);
    try {
      job = speechToTextService.transcribe(trackURI, languageCode);
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
      URI output = new URI(job.getPayload());
      String id = UUID.randomUUID().toString();
      InputStream in = workspace.read(output);
      URI uri = workspace.put(parentMediaPackage.getIdentifier().toString(), id,
              FilenameUtils.getName(output.getPath()), in);

      MediaPackageElement subtitleMediaPackage;
      switch (appendSubtitleAs) {
        case track:
          subtitleMediaPackage = new TrackImpl();
          break;
        case attachment:
        default:
          subtitleMediaPackage = new AttachmentImpl();
      }

      subtitleMediaPackage.setIdentifier(id);
      subtitleMediaPackage.setURI(uri);
      MediaPackageElementFlavor targetFlavor = tagsAndFlavors.getSingleTargetFlavor().applyTo(track.getFlavor());
      subtitleMediaPackage.setFlavor(targetFlavor);

      List<String> targetTags = tagsAndFlavors.getTargetTags();

      // this is used to set some values automatically, like the correct mimetype
      Job inspection = mediaInspectionService.enrich(subtitleMediaPackage, true);
      if (!waitForStatus(inspection).isSuccess()) {
        throw new SpeechToTextServiceException(String.format(
                "Transcription for '%s' failed at enriching process", trackURI));
      }

      subtitleMediaPackage = MediaPackageElementParser.getFromXml(inspection.getPayload());

      for (String tag : targetTags) {
        subtitleMediaPackage.addTag(tag);
      }

      parentMediaPackage.add(subtitleMediaPackage);

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
   * Get the information how to append the subtitles file to the media package.
   *
   * @param mediaPackage Media package of for logging reasons.
   * @param workflowInstance Contains the workflow configuration.
   * @return How to append the subtitles file to the media package.
   * @throws WorkflowOperationException Get thrown if an error occurs.
   */
  private AppendSubtitleAs howToAppendTheSubtitles(MediaPackage mediaPackage, WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    String targetElement = StringUtils.trimToEmpty(operation.getConfiguration(TARGET_ELEMENT)).toLowerCase();
    AppendSubtitleAs appendSubtitleAs;
    if (targetElement.isEmpty()) {
      appendSubtitleAs = AppendSubtitleAs.attachment; // attachment is default/fallback
    } else {
      try {
        appendSubtitleAs = AppendSubtitleAs.valueOf(targetElement);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(String.format(
                "Speech-to-Text job for media package '%s' failed, because of wrong workflow xml configuration. "
                        + "target-element of type '%s' does not exist.", mediaPackage, targetElement));
      }
    }
    return appendSubtitleAs;
  }

  /**
   * Searches some places to get the right language of the media package / track.
   *
   * @param mediaPackage The media package from which the subtitles are generated.
   * @param workflowInstance Contains the workflow configuration.
   * @return The language of the media package / track.
   */
  private String getMediaPackageLanguage(MediaPackage mediaPackage, WorkflowInstance workflowInstance) {

    // First look if there is a fixed language configured in the workflow xml
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
      // default value when nothing worked
      language = "eng";
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
