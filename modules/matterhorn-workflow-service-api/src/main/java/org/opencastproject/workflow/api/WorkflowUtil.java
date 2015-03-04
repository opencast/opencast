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
package org.opencastproject.workflow.api;

import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

public final class WorkflowUtil {
  private WorkflowUtil() {
  }

  /**
   * Checks to see whether a given workflow state is active.
   *
   * @param workflowState
   *          The workflow state to check.
   * @return True if the workflow is currently active, not stopped or failed.
   */
  public static boolean isActive(WorkflowState workflowState) {
    return WorkflowState.INSTANTIATED.equals(workflowState)
            || WorkflowState.RUNNING.equals(workflowState)
            || WorkflowState.PAUSED.equals(workflowState);
  }
}
