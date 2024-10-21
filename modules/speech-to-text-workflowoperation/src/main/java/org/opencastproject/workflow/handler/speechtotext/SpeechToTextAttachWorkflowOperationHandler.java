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
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.speechtotext.api.SpeechToTextServiceException;
import org.opencastproject.util.NotFoundException;
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
import java.util.Objects;

/**
 * Workflow operation for attaching results from asynchronously running speech-to-text service jobs.
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Speech-to-Text Attach Workflow Operation Handler",
        "workflow.operation=speechtotext-attach"
    }
)
public class SpeechToTextAttachWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextAttachWorkflowOperationHandler.class);

  /** Property name for configuring the place where the subtitles shall be appended. */
  private static final String TARGET_ELEMENT = "target-element";

  /** Workflow configuration name to store jobs in */
  private static final String JOBS_WORKFLOW_CONFIGURATION = "speech-to-text-jobs";

  private enum AppendSubtitleAs {
    attachment, track
  }

  /** The workspace service. */
  private Workspace workspace;

  /** The inspection service. */
  private MediaInspectionService mediaInspectionService;

  @Override
  @Activate
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering speechtotext-attach workflow operation handler");
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * WorkflowOperationHandler#start(WorkflowInstance,
   * JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    logger.info("Start speechtotext-attach workflow operation for media package {}", mediaPackage);

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
            Configuration.none, Configuration.none,
            Configuration.many, Configuration.one);

    // How to save the subtitle file? (as attachment, as track...)
    AppendSubtitleAs appendSubtitleAs = howToAppendTheSubtitles(workflowInstance);

    // get previously started speech-to-text jobs
    var jobIds = Objects.toString(workflowInstance.getConfiguration(JOBS_WORKFLOW_CONFIGURATION), "");
    if (jobIds.isEmpty()) {
      logger.info("No speechtotext jobs to attach. Skipping.");
      return createResult(mediaPackage, WorkflowOperationResult.Action.SKIP);
    }

    for (var jobId: jobIds.split(",")) {
      attachSubtitle(Long.parseLong(jobId), mediaPackage, tagsAndFlavors, appendSubtitleAs);
    }

    // Remove tracked jobs from workflow
    workflowInstance.getConfigurations().remove(JOBS_WORKFLOW_CONFIGURATION);

    logger.info("Speech-To-Text workflow operation for media package {} completed", mediaPackage);
    return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
  }

  /**
   * Creates the subtitle file for a track and appends it to the media package.
   *
   * @param jobId Identifier of the speectotext job
   * @param mediaPackage The media package where the track is located.
   * @param tagsAndFlavors Tags and flavors instance (to get target flavor information)
   * @param appendSubtitleAs Tells how the subtitles file has to be appended.
   * @throws WorkflowOperationException Get thrown if an error occurs.
   */
  private void attachSubtitle(long jobId, MediaPackage mediaPackage, ConfiguredTagsAndFlavors tagsAndFlavors,
      AppendSubtitleAs appendSubtitleAs) throws WorkflowOperationException {

    logger.info("Attaching subtitle from job '{}' to media package {}", jobId, mediaPackage);
    Job job;
    try {
      job = serviceRegistry.getJob(jobId);
    } catch (NotFoundException | ServiceRegistryException e) {
      throw new WorkflowOperationException(
              String.format("Could not find speechtotext job %s", jobId), e);
    }
    if (!"speechtotext".equals(job.getOperation())) {
      throw  new WorkflowOperationException(String.format(
          "Job %s is on type %s. Expected `speechtotext`", jobId, job.getOperation()));
    }

    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException(
              String.format("Speechtotext job for media package '%s' failed", mediaPackage));
    }

    // add subtitle to media package
    try {
      String[] jobOutput = job.getPayload().split(",");
      URI output = new URI(jobOutput[0]);
      String outputLanguage = jobOutput[1];
      String engineType = jobOutput[2];

      var subtitleElement = appendSubtitleAs == AppendSubtitleAs.attachment ? new AttachmentImpl() : new TrackImpl();
      var elementId = subtitleElement.generateIdentifier();

      try (InputStream in = workspace.read(output)) {
        URI uri = workspace.put(mediaPackage.getIdentifier().toString(), elementId,
                FilenameUtils.getName(output.getPath()), in);
        subtitleElement.setURI(uri);
      }
      subtitleElement.setFlavor(tagsAndFlavors.getSingleTargetFlavor());

      List<String> targetTags = tagsAndFlavors.getTargetTags();
      targetTags.add("lang:" + outputLanguage);
      targetTags.add("generator-type:auto");
      targetTags.add("generator:" + engineType.toLowerCase());
      for (String tag : targetTags) {
        subtitleElement.addTag(tag);
      }

      // this is used to set some values automatically, like the correct mimetype
      Job inspection = mediaInspectionService.enrich(subtitleElement, true);
      if (!waitForStatus(inspection).isSuccess()) {
        throw new SpeechToTextServiceException(String.format(
            "Transcription for '%s' failed at enriching process", mediaPackage));
      }

      mediaPackage.add(MediaPackageElementParser.getFromXml(inspection.getPayload()));

      workspace.delete(output);
    } catch (Exception e) {
      throw new WorkflowOperationException("Error handling text-to-speech service output", e);
    }

    try {
      workspace.cleanup(mediaPackage.getIdentifier());
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
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

  //================================================================================
  // OSGi setter
  //================================================================================

  @Reference
  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.mediaInspectionService = mediaInspectionService;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }
}
