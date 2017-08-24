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
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;

public class AttachTranscriptionOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AttachTranscriptionOperationHandler.class);

  /** Workflow configuration option keys */
  static final String TRANSCRIPTION_JOB_ID = "transcription-job-id";
  static final String TARGET_FLAVOR = "target-flavor";
  static final String TARGET_TAG = "target-tag";
  static final String TARGET_CAPTION_FORMAT = "target-caption-format";

  /** The transcription service */
  private TranscriptionService service = null;
  private Workspace workspace;
  private CaptionService captionService;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(TRANSCRIPTION_JOB_ID, "The job id that identifies the file to be attached");
    CONFIG_OPTIONS.put(TARGET_FLAVOR, "The target \"flavor\" of the transcription file");
    CONFIG_OPTIONS.put(TARGET_TAG, "The target \"tag\" of the transcription file");
    CONFIG_OPTIONS.put(TARGET_CAPTION_FORMAT, "The target caption format of the transcription file (dfxp, etc)");
  }

  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
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
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    logger.debug("Attach transcription for mediapackage {} started", mediaPackage);

    // Get job id.
    String jobId = StringUtils.trimToNull(operation.getConfiguration(TRANSCRIPTION_JOB_ID));
    if (jobId == null)
      throw new WorkflowOperationException(TRANSCRIPTION_JOB_ID + " missing");

    // Check which tags/flavors have been configured
    String targetTagOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAG));
    String targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR));
    // Target flavor is mandatory
    if (targetFlavorOption == null)
      throw new WorkflowOperationException(TARGET_FLAVOR + " missing");
    MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption);
    String captionFormatOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_CAPTION_FORMAT));

    try {
      // Get transcription file from the service
      MediaPackageElement original = service.getGeneratedTranscription(mediaPackage.getIdentifier().compact(), jobId);
      MediaPackageElement transcription = original;

      // If caption format passed, convert to desired format
      if (captionFormatOption != null) {
        Job job = captionService.convert(transcription, "ibm-watson", captionFormatOption, "en");
        if (!waitForStatus(job).isSuccess()) {
          throw new WorkflowOperationException("Transcription format conversion job did not complete successfully");
        }
        transcription = MediaPackageElementParser.getFromXml(job.getPayload());
      }

      // Set the target flavor
      transcription.setFlavor(flavor);

      // Add tags
      if (targetTagOption != null) {
        for (String tag : asList(targetTagOption)) {
          if (StringUtils.trimToNull(tag) != null)
            transcription.addTag(tag);
        }
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

  public void setTranscriptionService(TranscriptionService service) {
    this.service = service;
  }

  public void setWorkspace(Workspace service) {
    this.workspace = service;
  }

  public void setCaptionService(CaptionService service) {
    this.captionService = service;
  }

}
