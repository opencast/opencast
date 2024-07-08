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

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.transcription.api.TranscriptionServiceException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Start Transcription Workflow Operation Handler (Amberscript)",
        "workflow.operation=amberscript-start-transcription"
    }
)
public class AmberscriptStartTranscriptionOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(AmberscriptStartTranscriptionOperationHandler.class);

  /** Workflow configuration option keys */
  static final String LANGUAGE = "language";
  static final String JOBTYPE = "jobtype";
  static final String SPEAKER = "speaker";
  static final String TRANSCRIPTIONTYPE = "transcriptiontype";
  static final String GLOSSARY = "glossary";
  static final String TRANSCRIPTIONSTYLE = "transcriptionstyle";
  static final String TARGETLANGUAGE = "targetlanguage";
  static final String SKIP_IF_FLAVOR_EXISTS = "skip-if-flavor-exists";

  /** The transcription service */
  private TranscriptionService service = null;

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

    String skipOption = StringUtils.trimToNull(operation.getConfiguration(SKIP_IF_FLAVOR_EXISTS));
    if (skipOption != null) {
      MediaPackageElement[] mpes = mediaPackage.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor(skipOption));
      if (mpes != null && mpes.length > 0) {
        logger.info(
            "Start transcription operation will be skipped because flavor '{}' already exists in the media package.",
            skipOption);
        return createResult(Action.SKIP);
      }
    }

    logger.debug("Start transcription for mediapackage '{}'.", mediaPackage);

    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(
        workflowInstance, Configuration.many, Configuration.many, Configuration.none, Configuration.none);
    List<String> sourceTagOption = tagsAndFlavors.getSrcTags();
    List<MediaPackageElementFlavor> sourceFlavorOption = tagsAndFlavors.getSrcFlavors();
    String language = StringUtils.trimToEmpty(operation.getConfiguration(LANGUAGE));
    String jobType = StringUtils.trimToEmpty(operation.getConfiguration(JOBTYPE));
    String speaker = StringUtils.trimToEmpty(operation.getConfiguration(SPEAKER));
    String transcriptionType = StringUtils.trimToEmpty(operation.getConfiguration(TRANSCRIPTIONTYPE));
    // Note that specifying `""` is different from not specifying a glossary at all!
    // The former will override the default with not using a glossary for this operation,
    // while the latter will fall back to said default. Hence, no `trimToEmpty` here.
    String glossary = StringUtils.trim(operation.getConfiguration(GLOSSARY));
    String transcriptionStyle = StringUtils.trimToEmpty(operation.getConfiguration(TRANSCRIPTIONSTYLE));
    // See glossary comment above
    String targetLanguage = StringUtils.trim(operation.getConfiguration(TARGETLANGUAGE));

    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();

    // Make sure either one of tags or flavors are provided
    if (sourceTagOption.isEmpty() && sourceFlavorOption.isEmpty()) {
      throw new WorkflowOperationException("No source tag or flavor have been specified!");
    }

    if (!sourceFlavorOption.isEmpty()) {
      elementSelector.addFlavor(sourceFlavorOption.get(0));
    }
    if (!sourceTagOption.isEmpty()) {
      elementSelector.addTag(sourceTagOption.get(0));
    }

    Collection<Track> elements = elementSelector.select(mediaPackage, false);
    Job job = null;
    for (Track track : elements) {
      if (!track.hasAudio()) {
        logger.info("Track {} from media package {} doesn't contain audio stream. Skip subtitle generation.",
            track.getFlavor(), mediaPackage.getIdentifier());
        continue;
      }
      try {
        job = service.startTranscription(mediaPackage.getIdentifier().toString(), track, language, jobType, speaker,
            transcriptionType, glossary, transcriptionStyle, targetLanguage);
        // Only one job per media package
        break;
      } catch (TranscriptionServiceException e) {
        throw new WorkflowOperationException(e);
      }
    }

    if (job == null) {
      logger.info("No matching tracks found.");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Wait for the jobs to return
    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException("Transcription job did not complete successfully.");
    }
    // Return OK means that the transcription job was created, but not finished yet

    logger.debug("External transcription job for mediapackage '{}' was created.", mediaPackage);

    // Results are empty, we should get a callback when transcription is done
    return createResult(Action.CONTINUE);
  }

  @Reference(target = "(provider=amberscript)")
  public void setTranscriptionService(TranscriptionService service) {
    this.service = service;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
