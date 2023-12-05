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

package org.opencastproject.workflow.handler.workflow;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This WOH starts a new workflow for given media package.
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Start Workflow Workflow Operation Handler",
        "workflow.operation=start-workflow"
    }
)
public class StartWorkflowWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(StartWorkflowWorkflowOperationHandler.class);

  /** Name of the configuration option that provides the media package ID */
  @Deprecated
  public static final String MEDIA_PACKAGE_ID = "media-package";

  public static final String MEDIA_PACKAGE_IDS = "media-packages";

  /** Name of the configuration option that provides the workflow definition ID */
  public static final String WORKFLOW_DEFINITION = "workflow-definition";

  private AssetManager assetManager;

  private WorkflowService workflowService;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param assetManager
   *          the asset manager
   */
  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param workflowService
   *          the workflow service
   */
  @Reference
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    String mediaPackageIDs = trimToEmpty(operation.getConfiguration(MEDIA_PACKAGE_IDS));
    if ("".equals(mediaPackageIDs)) {
      mediaPackageIDs = trimToEmpty(operation.getConfiguration(MEDIA_PACKAGE_ID));
    }
    final String configuredMediaPackageIDs = mediaPackageIDs;
    final String configuredWorkflowDefinition = trimToEmpty(operation.getConfiguration(WORKFLOW_DEFINITION));
    final Boolean failOnError = operation.isFailOnError();
    // Get workflow parameter
    final Map<String, String> properties = new HashMap<>();
    for (String key : operation.getConfigurationKeys()) {
      if (MEDIA_PACKAGE_ID.equals(key) || MEDIA_PACKAGE_IDS.equals(key) || WORKFLOW_DEFINITION.equals(key)) {
        continue;
      }
      properties.put(key, operation.getConfiguration(key));
    }

    final WorkflowDefinition workflowDefinition;
    try {
      // Get workflow definition
      workflowDefinition = workflowService.getWorkflowDefinitionById(
              configuredWorkflowDefinition);
    } catch (NotFoundException e) {
      throw new WorkflowOperationException(format("Workflow Definition '%s' not found", configuredWorkflowDefinition));
    } catch (WorkflowDatabaseException e) {
      throw new WorkflowOperationException(e);
    }
    String errors = "";
    String delim = "";
    for (String mpId : asList(configuredMediaPackageIDs)) {
      // Get media package
      Opt<MediaPackage> mpOpt = assetManager.getMediaPackage(mpId);
      if (mpOpt.isNone()) {
        String errstr = format("Media package %s not found", mpId);
        if (failOnError) {
          throw new WorkflowOperationException(errstr);
        } else {
          logger.error(errstr);
          errors += delim + errstr;
          delim = "\n";
        }
        continue;
      }
      final MediaPackage mp = mpOpt.get();
      try {
        // Start workflow
        logger.info("Starting '{}' workflow for media package '{}'", configuredWorkflowDefinition,
                mpId);
        workflowService.start(workflowDefinition, mp, properties);
      } catch (WorkflowDatabaseException | WorkflowParsingException | UnauthorizedException e) {
        if (failOnError) {
          throw new WorkflowOperationException(e);
        } else {
          logger.error(e.getMessage(),e);
          errors += delim + e.getMessage();
        }
      }
    }
    if (!errors.isEmpty()) {
      throw new WorkflowOperationException(errors);
    }
    return createResult(WorkflowOperationResult.Action.CONTINUE);
  }
}
