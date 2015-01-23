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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Workflow operation handler for choosing the retry strategy after a failing operation
 */
public class ErrorResolutionWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ErrorResolutionWorkflowOperationHandler.class);

  /** Path to the caption upload ui resources */
  private static final String HOLD_UI_PATH = "/ui/operation/retry-strategy/index.html";

  /** Parameter name */
  private static final String OPT_STRATEGY = "retryStrategy";

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.ResumableWorkflowOperationHandlerBase#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  public void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    setHoldActionTitle("Select retry strategy");
    registerHoldStateUserInterface(HOLD_UI_PATH);
    logger.info("Registering retry strategy failover hold state ui from classpath {}", HOLD_UI_PATH);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context,
          Map<String, String> properties) throws WorkflowOperationException {

    String strategyValue = properties.get(OPT_STRATEGY);
    if (StringUtils.isBlank(strategyValue)) {
      logger.warn("No retry strategy submitted for workflow '{}', holding again", workflowInstance);
      return createResult(null, properties, Action.PAUSE, 0);
    }

    try {
      RetryStrategy s = RetryStrategy.valueOf(strategyValue);
      switch (s) {
        case NONE:
          logger.info("Error resolution 'fail' was triggered for workflow '{}'", workflowInstance);
          return createResult(null, properties, Action.CONTINUE, 0);
        case RETRY:
          logger.info("Error resolution 'retry' was triggered for workflow '{}'", workflowInstance);
          return createResult(null, properties, Action.CONTINUE, 0);
        default:
          logger.warn("Unknown retry strategy '{}' submitted for workflow '{}'", strategyValue, workflowInstance);
          return createResult(null, properties, Action.PAUSE, 0);
      }
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown retry strategy '{}' submitted for workflow '{}'", strategyValue, workflowInstance);
      return createResult(null, properties, Action.PAUSE, 0);
    }
  }

}
