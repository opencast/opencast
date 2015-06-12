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

package org.opencastproject.workflow.handler.ingest;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;

import org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase;

/**
 * Workflow operation handler that signifies a workflow that is currently in the process of ingesting a recording.
 * <p>
 * The operation registers a ui that displays the ingest status.
 */
public class IngestWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  /** Path to the hold state ui */
  public static final String UI_RESOURCE_PATH = "/ui/operation/ingest/index.html";

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  public void activate(ComponentContext componentContext) {
    super.activate(componentContext);

    // Set the operation's action link title
    setHoldActionTitle("Monitor ingest");

    // Add the ui piece that displays the ingest information
    registerHoldStateUserInterface(UI_RESOURCE_PATH);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
    WorkflowOperationResult result = createResult(Action.PAUSE);
    result.setAllowsContinue(false);
    result.setAllowsAbort(false);
    return result;
  }

}
