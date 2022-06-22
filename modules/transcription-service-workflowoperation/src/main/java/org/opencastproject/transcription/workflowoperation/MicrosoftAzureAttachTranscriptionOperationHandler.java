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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
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

@Component(
        immediate = true,
        service = WorkflowOperationHandler.class,
        property = {
                "service.description=Microsoft Azure Attach Transcription Workflow Operation Handler",
                "workflow.operation=microsoft-azure-attach-transcription"
        }
)
public class MicrosoftAzureAttachTranscriptionOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureAttachTranscriptionOperationHandler.class);

  /** Workflow configuration option keys */
  static final String TRANSCRIPTION_JOB_ID = "transcription-job-id";
  static final String TARGET_FLAVOR = "target-flavor";
  static final String TARGET_TAGS = "target-tags";
  static final String TARGET_CAPTION_FORMAT = "target-caption-format";

  /** The transcription service */
  private TranscriptionService service = null;
  private Workspace workspace;

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

    logger.info("Attach transcription for mediapackage {} started", mediaPackage);

    // Get job id.
    String jobId = StringUtils.trimToNull(operation.getConfiguration(TRANSCRIPTION_JOB_ID));
    if (StringUtils.isBlank(jobId)) {
      throw new WorkflowOperationException(TRANSCRIPTION_JOB_ID + " missing");
    }

    // Check which tags/flavors have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(
            workflowInstance, Configuration.none, Configuration.none, Configuration.many, Configuration.one);
    List<String> targetTagOption = tagsAndFlavors.getTargetTags();
    List<MediaPackageElementFlavor> targetFlavorOption = tagsAndFlavors.getTargetFlavors();
    // Target flavor is mandatory
    MediaPackageElementFlavor targetFlavor = tagsAndFlavors.getSingleTargetFlavor();

    try {
      // Get transcription file from the service
      MediaPackageElement original = service.getGeneratedTranscription(mediaPackage.getIdentifier().toString(), jobId);
      MediaPackageElement transcription = original;

      // Set the target flavor
      transcription.setFlavor(targetFlavor);

      // Add tags
      for (String tag : targetTagOption) {
        transcription.addTag(tag);
      }

      // Add to media package
      mediaPackage.add(transcription);

      String uri = transcription.getURI().toString();
      String ext = uri.substring(uri.lastIndexOf("."));
      transcription.setURI(workspace.moveTo(transcription.getURI(), mediaPackage.getIdentifier().toString(),
              transcription.getIdentifier(), "captions." + ext));
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }

  @Reference(target = "(provider=microsoft.azure)")
  public void setTranscriptionService(TranscriptionService service) {
    this.service = service;
  }

  @Reference
  public void setWorkspace(Workspace service) {
    this.workspace = service;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }
}
