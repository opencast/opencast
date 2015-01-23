/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Workflow operation handler that will add a number of workflow operations to the current workflow.
 * <p>
 * There are basically two ways that the handler works. First, it will look into the workflow configuration for a
 * property named <code>workflow.definition</code>. If that is found, it will take the operations from that workflow,
 * append them and continue.
 * <p>
 * If not, the operation handler will enter a hold state and ask the user which workflow to use.
 */
public class AppendWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AppendWorkflowOperationHandler.class);

  /** Configuration value for the workflow operation definition */
  public static final String OPT_WORKFLOW = "workflowSelector";

  /** Path to the hold state ui */
  public static final String UI_RESOURCE_PATH = "/ui/operation/append/index.html";

  /** Title shown in Admin UI for Hold action */
  public static final String HOLD_ACTION_TITLE = "Select Workflow";

  /** The workflow service instance */
  protected WorkflowService workflowService = null;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  public void activate(ComponentContext componentContext) {
    super.activate(componentContext);

    // Register the supported configuration options
    addConfigurationOption(OPT_WORKFLOW, "Workflow definition identifier");

    // Add the ui piece that displays the capture information
    registerHoldStateUserInterface(UI_RESOURCE_PATH);

    /* Replace the default title */
    this.setHoldActionTitle(HOLD_ACTION_TITLE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    String workflowDefinitionId = workflowInstance.getConfiguration(OPT_WORKFLOW);
    if (append(workflowInstance, workflowDefinitionId))
      return createResult(Action.CONTINUE);
    else
      logger.info("Entering hold state to ask for workflow");
    return createResult(Action.PAUSE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance,
      JobContext context, Map<String, String> properties) {
    String workflowDefinitionId = properties.get(OPT_WORKFLOW);
    for (Entry<String, String> entry : properties.entrySet()) {
      workflowInstance.setConfiguration(entry.getKey(), entry.getValue());
    }
    if (append(workflowInstance, workflowDefinitionId))
      return createResult(Action.CONTINUE);
    else
      logger.info("Entering hold state to ask for workflow");
    return createResult(Action.PAUSE);
  }

  /**
   * Adds the operations found in the workflow defined by
   * <code>workflowDefintionId</code> to the workflow instance and returns
   * <code>true</code> if everything worked fine, <code>false</code>
   * otherwhise.
   *
   *
   * @param workflowInstance
   *          the instance to update
   * @param workflowDefinitionId
   *          id of the workflow definition
   * @return <code>true</code> if all operations have been added
   */
  protected boolean append(WorkflowInstance workflowInstance, String workflowDefinitionId) {
    if (StringUtils.isBlank(workflowDefinitionId))
      return false;

    try {
      WorkflowDefinition definition = workflowService.getWorkflowDefinitionById(workflowDefinitionId);
      if (definition != null) {
        workflowInstance.extend(definition);
        return true;
      }
    } catch (WorkflowDatabaseException e) {
      logger.warn("Error querying workflow service for '{}'", workflowDefinitionId, e);
    } catch (NotFoundException e) {
      logger.warn("Workflow '{}' not found. Entering hold state to resolve", workflowDefinitionId);
    }

    logger.info("Entering hold state to ask for workflow");
    return false;
  }

  /**
   * Callback from the OSGi environment that will pass a reference to the workflow service upon component acitvation.
   *
   * @param service
   *          the workflow service
   */
  void setWorkflowService(WorkflowService service) {
    this.workflowService = service;
  }

}
