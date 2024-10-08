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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.util.doc.DocUtil;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Attach Transcription Workflow Operation Handler (Microsoft Azure)",
        "workflow.operation=microsoft-azure-attach-transcription"
    }
)
public class MicrosoftAzureAttachTranscriptionOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureAttachTranscriptionOperationHandler.class);

  /** Workflow configuration option keys */
  static final String TRANSCRIPTION_JOB_ID = "transcription-job-id";
  static final String TARGET_TYPE = "target-element-type";

  private TranscriptionService service = null;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(TRANSCRIPTION_JOB_ID, "The job id that identifies the file to be attached");
    CONFIG_OPTIONS.put(TARGET_FLAVOR, "The flavor of the transcription file");
    CONFIG_OPTIONS.put(TARGET_TAGS, "The tags of the transcription file");
    CONFIG_OPTIONS.put(TARGET_TYPE, "Define where to append the subtitles file. "
        + "Accepted values: \"track\", \"attachment\". Default value is \"track\".");
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
    Map<String, Object> wfProps = Collections.unmodifiableMap(workflowInstance.getConfigurations());

    logger.debug("Attach transcription for media package '{}' started.", mediaPackage.getIdentifier());

    String jobId = StringUtils.trimToNull(operation.getConfiguration(TRANSCRIPTION_JOB_ID));
    if (jobId == null) {
      throw new WorkflowOperationException(TRANSCRIPTION_JOB_ID + " missing.");
    }

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(
        workflowInstance, Configuration.none, Configuration.none, Configuration.many, Configuration.one);
    List<String> targetTagOption = tagsAndFlavors.getTargetTags();
    MediaPackageElementFlavor targetFlavor;
    try {

      targetFlavor = MediaPackageElementFlavor.parseFlavor(DocUtil.processTextTemplate(
          "Replacing variables in target-flavor",
          operation.getConfiguration(TARGET_FLAVOR), // we can't use tagsAndFlavors.getSingleTargetFlavor() here
                                                     // because the flavor will be all lower case
                                                     // and templating may not work due to keys mismatch
          wfProps));
    } catch (IllegalStateException ex) {
      throw new WorkflowOperationException(TARGET_FLAVOR + " option not set or used correctly.", ex);
    }

    String typeUnparsed = StringUtils.trimToEmpty(operation.getConfiguration(TARGET_TYPE));
    MediaPackageElement.Type type = null;
    if (!typeUnparsed.isEmpty()) {
      // Case insensitive matching between user input (workflow config key) and enum value
      for (MediaPackageElement.Type t : MediaPackageElement.Type.values()) {
        if (t.name().equalsIgnoreCase(typeUnparsed)) {
          type = t;
        }
      }
      if (type == null || (type != Track.TYPE && type != Attachment.TYPE)) {
        throw new WorkflowOperationException(new IllegalArgumentException(
            String.format("The given type '%s' for mediapackage %s was illegal. Please"
            + "check the operations' configuration keys.", type, mediaPackage.getIdentifier())));
      }
    } else {
      type = Track.TYPE;
    }

    try {
      MediaPackageElement transcription
          = service.getGeneratedTranscription(mediaPackage.getIdentifier().toString(), jobId, type);
      transcription.setFlavor(targetFlavor);
      for (String tag : targetTagOption) {
        String templatedTag = DocUtil.processTextTemplate("Replacing variables in tag", tag, wfProps);
        if (StringUtils.isNotEmpty(templatedTag)) {
          transcription.addTag(templatedTag);
        }
      }
      mediaPackage.add(transcription);
      logger.info("Added transcription to the media package {}: {}",
          mediaPackage.getIdentifier(), transcription.getURI());

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
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
