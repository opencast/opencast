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

package org.opencastproject.workflow.handler.workflow;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This WOH starts a new workflow for given media package.
 */
public class StartWorkflowWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(StartWorkflowWorkflowOperationHandler.class);

  /** Name of the configuration option that provides the media package ID */
  public static final String MEDIA_PACKAGE_ID = "media-package";

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
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param workflowService
   *          the workflow service
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    final String configuredMediaPackageID = trimToEmpty(operation.getConfiguration(MEDIA_PACKAGE_ID));
    final String configuredWorkflowDefinition = trimToEmpty(operation.getConfiguration(WORKFLOW_DEFINITION));

    // Get media package
    Opt<MediaPackage> mpOpt = assetManager.getMediaPackage(configuredMediaPackageID);
    if (mpOpt.isNone()) {
      throw new WorkflowOperationException(format("Media package %s not found", configuredMediaPackageID));
    }
    final MediaPackage mp = mpOpt.get();

    // Get workflow parameter
    final Map<String, String> properties = new HashMap<>();
    for (String key : operation.getConfigurationKeys()) {
      if (MEDIA_PACKAGE_ID.equals(key) || WORKFLOW_DEFINITION.equals(key)) {
        continue;
      }
      properties.put(key, operation.getConfiguration(key));
    }

    try {
      // Get workflow definition
      final WorkflowDefinition workflowDefinition = workflowService.getWorkflowDefinitionById(
              configuredWorkflowDefinition);

      // Start workflow
      logger.info("Starting '{}' workflow for media package '{}'", configuredWorkflowDefinition,
              configuredMediaPackageID);
      workflowService.start(workflowDefinition, mp, properties);

    } catch (NotFoundException e) {
      throw new WorkflowOperationException(format("Workflow Definition '%s' not found", configuredWorkflowDefinition));
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    return createResult(WorkflowOperationResult.Action.CONTINUE);
  }
}
