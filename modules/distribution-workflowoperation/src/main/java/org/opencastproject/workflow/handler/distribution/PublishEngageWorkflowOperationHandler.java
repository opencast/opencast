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

package org.opencastproject.workflow.handler.distribution;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.publication.api.EngagePublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The workflow definition for handling "engage publication" operations
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Engage Publication Workflow Handler",
        "workflow.operation=publish-engage"
    }
)
public class PublishEngageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PublishEngageWorkflowOperationHandler.class);

  /** Workflow configuration option keys */
  static final String DOWNLOAD_SOURCE_FLAVORS = "download-source-flavors";
  static final String DOWNLOAD_TARGET_SUBFLAVOR = "download-target-subflavor";
  static final String DOWNLOAD_SOURCE_TAGS = "download-source-tags";
  static final String DOWNLOAD_TARGET_TAGS = "download-target-tags";
  static final String STREAMING_SOURCE_TAGS = "streaming-source-tags";
  static final String STREAMING_TARGET_TAGS = "streaming-target-tags";
  static final String STREAMING_SOURCE_FLAVORS = "streaming-source-flavors";
  static final String STREAMING_TARGET_SUBFLAVOR = "streaming-target-subflavor";
  static final String CHECK_AVAILABILITY = "check-availability";
  static final String STRATEGY = "strategy";
  static final String MERGE_FORCE_FLAVORS = "merge-force-flavors";
  static final String ADD_FORCE_FLAVORS = "add-force-flavors";

  private EngagePublicationService publicationService;

  private OrganizationDirectoryService organizationDirectoryService;

  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Reference(target = "(distribution.channel=download)")
  public void setPublicationService(EngagePublicationService publicationService) {
    this.publicationService = publicationService;
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running engage publication workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance op = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    // TODO Don't trim, etc. also in indexservice
    String checkAvailability = op.getConfiguration(CHECK_AVAILABILITY);
    String strategy = op.getConfiguration(STRATEGY);
    String downloadSourceFlavors = op.getConfiguration(DOWNLOAD_SOURCE_FLAVORS);
    String downloadSourceTags = op.getConfiguration(DOWNLOAD_SOURCE_TAGS);
    String downloadTargetSubflavor = op.getConfiguration(DOWNLOAD_TARGET_SUBFLAVOR);
    String downloadTargetTags = op.getConfiguration(DOWNLOAD_TARGET_TAGS);
    String streamingSourceFlavors = op.getConfiguration(STREAMING_SOURCE_FLAVORS);
    String streamingSourceTags = op.getConfiguration(STREAMING_SOURCE_TAGS);
    String streamingTargetSubflavor = op.getConfiguration(STREAMING_TARGET_SUBFLAVOR);
    String streamingTargetTags = op.getConfiguration(STREAMING_TARGET_TAGS);
    String mergeForceFlavors = op.getConfiguration(MERGE_FORCE_FLAVORS);
    String addForceFlavors = op.getConfiguration(ADD_FORCE_FLAVORS);

    try {
      Job publishJob = publicationService.publish(mediaPackage, checkAvailability, strategy, downloadSourceFlavors,
          downloadSourceTags, downloadTargetSubflavor, downloadTargetTags, streamingSourceFlavors, streamingSourceTags,
          streamingTargetSubflavor, streamingTargetTags, mergeForceFlavors, addForceFlavors);

      if (!waitForStatus(publishJob).isSuccess()) {
        throw new WorkflowOperationException(
            "Mediapackage " + mediaPackage.getIdentifier() + " could not be published to Engage");
      }

      Job resolvedJob = serviceRegistry.getJob(publishJob.getId());
      String payload = resolvedJob.getPayload();
      if (payload == null) {
        logger.warn("Publish to Engage failed, no payload from publication job: {}", resolvedJob);
        return createResult(mediaPackage, Action.SKIP);
      }

      MediaPackage newMediaPackage = MediaPackageParser.getFromXml(payload);
      return createResult(newMediaPackage, Action.CONTINUE);
    } catch (NotFoundException | ServiceRegistryException | MediaPackageException e) {
      throw new WorkflowOperationException(e);
    } catch (PublicationException e) {
      throw new WorkflowOperationException(e.getCause());
    }
  }
}
