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
package org.opencastproject.transcription.workflowoperation;

import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.api.TranscriptionService;
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

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Attach Transcription Workflow Operation Handler (Amberscript)",
        "workflow.operation=amberscript-attach-transcription"
    }
)
public class AmberscriptAttachTranscriptionOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(AmberscriptAttachTranscriptionOperationHandler.class);

  /** Workflow configuration option keys */
  static final String TRANSCRIPTION_JOB_ID = "transcription-job-id";
  static final String TARGET_FLAVOR = "target-flavor";
  static final String TARGET_TAGS = "target-tags";
  static final String TARGET_CAPTION_FORMAT = "target-caption-format";

  private TranscriptionService service = null;
  private CaptionService captionService;

  private Workspace workspace;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(TRANSCRIPTION_JOB_ID, "The job id that identifies the file to be attached");
    CONFIG_OPTIONS.put(TARGET_FLAVOR, "The target \"flavor\" of the transcription file");
    CONFIG_OPTIONS.put(TARGET_TAGS, "The target \"tags\" of the transcription file");
    CONFIG_OPTIONS.put(TARGET_CAPTION_FORMAT, "The target caption format of the transcription file (dfxp, etc)");
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

    logger.debug("Attach transcription for mediapackage '{}' started.", mediaPackage);

    String jobId = StringUtils.trimToNull(operation.getConfiguration(TRANSCRIPTION_JOB_ID));
    if (jobId == null) {
      throw new WorkflowOperationException(TRANSCRIPTION_JOB_ID + " missing.");
    }

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(
        workflowInstance, Configuration.none, Configuration.none, Configuration.many, Configuration.many);
    List<MediaPackageElementFlavor> targetFlavorOption = tagsAndFlavors.getTargetFlavors();
    List<String> targetTagOption = tagsAndFlavors.getTargetTags();
    String captionFormatOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_CAPTION_FORMAT));

    MediaPackageElementFlavor flavor = null;
    if (!targetFlavorOption.isEmpty()) {
      flavor = targetFlavorOption.get(0);
    } else {
      // If the target format is not specified, we will leave it as is (srt).
      String format = (captionFormatOption != null) ? captionFormatOption : "srt";
      if (service.getLanguage() != null) {
        flavor = new MediaPackageElementFlavor("captions", format + "+" + service.getLanguage());
      } else {
        flavor = new MediaPackageElementFlavor("captions", format);
      }
    }

    try {
      MediaPackageElement transcription
          = service.getGeneratedTranscription(mediaPackage.getIdentifier().toString(), jobId);

      MediaPackageElement convertedTranscription = null;
      if (captionFormatOption != null) {
        Job job = captionService.convert(transcription, "subrip", captionFormatOption, service.getLanguage());
        if (!waitForStatus(job).isSuccess()) {
          throw new WorkflowOperationException("Transcription format conversion job did not complete successfully.");
        }
        convertedTranscription = MediaPackageElementParser.getFromXml(job.getPayload());
        workspace.delete(transcription.getURI());
      } else {
        convertedTranscription = transcription;
      }
      convertedTranscription.setFlavor(flavor);
      for (String tag : targetTagOption) {
        convertedTranscription.addTag(tag);
      }
      mediaPackage.add(convertedTranscription);
      logger.info("Added transcription to the mediapackage {}: {}",
          mediaPackage.getIdentifier(), convertedTranscription.getURI());

    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }

  @Reference(target = "(provider=amberscript)")
  public void setTranscriptionService(TranscriptionService service) {
    this.service = service;
  }

  @Reference
  public void setWorkspace(Workspace service) {
    this.workspace = service;
  }

  @Reference
  public void setCaptionService(CaptionService service) {
    this.captionService = service;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
