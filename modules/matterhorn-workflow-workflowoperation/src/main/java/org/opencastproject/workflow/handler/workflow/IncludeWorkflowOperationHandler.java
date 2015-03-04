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

import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.Fn2;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Workflow operation handler that will conditionally insert a complete workflow into the current one
 * at its own position.
 */
public final class IncludeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IncludeWorkflowOperationHandler.class);

  /** Configuration value for the workflow operation definition */
  public static final String WORKFLOW_CFG = "workflow-id";

  /** The workflow service instance */
  private WorkflowService workflowService = null;

  /**
   * {@inheritDoc}
   *
   * @see ResumableWorkflowOperationHandlerBase#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  public void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    // Register the supported configuration options
    addConfigurationOption(WORKFLOW_CFG, "Workflow definition identifier");
  }

  /**
   * {@inheritDoc}
   *
   * @see ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance,
   * org.opencastproject.job.api.JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance wi, final JobContext context)
          throws WorkflowOperationException {
    final String workflowDefinitionId = getConfig(wi, WORKFLOW_CFG);
    insertWorkflow(wi, workflowDefinitionId);
    // Return all existing workflow parameters with the result object to
    // make the workflow service replace the variables again. This is
    // necessary to 'propagate' the parameters to the included workflow.
    final Map<String, String> props = $(wi.getConfigurationKeys()).foldl(new HashMap<String, String>(), new Fn2<HashMap<String, String>, String, HashMap<String, String>>() {
      @Override public HashMap<String, String> ap(HashMap<String, String> sum, String key) {
        sum.put(key, wi.getConfiguration(key));
        return sum;
      }
    });
    return createResult(wi.getMediaPackage(), props, Action.CONTINUE, 0);
  }

  /**
   * Adds the operations found in the workflow defined by <code>workflowDefinitionId</code> to the workflow instance and
   * returns <code>true</code> if everything worked fine, <code>false</code> otherwise.
   *
   * @param wi
   *         the instance to insert the workflow identified by <code>workflowDefinitionId</code> into
   * @param workflowDefinitionId
   *         id of the workflow definition to insert
   * @throws WorkflowOperationException
   *         in case of any error
   */
  public void insertWorkflow(final WorkflowInstance wi, final String workflowDefinitionId)
          throws WorkflowOperationException {
    try {
      final WorkflowDefinition definition = workflowService.getWorkflowDefinitionById(workflowDefinitionId);
      if (definition != null) {
        logger.info(format("Insert workflow %s into the current workflow instance", workflowDefinitionId));
        wi.insert(definition, wi.getCurrentOperation());
      } else {
        logger.warn(format("Workflow definition %s cannot be found", workflowDefinitionId));
      }
    } catch (Exception e) {
      throw new WorkflowOperationException("Error inserting workflow " + workflowDefinitionId, e);
    }
  }

  /**
   * OSGi DI.
   */
  public void setWorkflowService(WorkflowService service) {
    this.workflowService = service;
  }
}
