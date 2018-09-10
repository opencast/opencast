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

  /**
   * Checks to see whether a given workflow state is active.
   *
   * @param workflowState
   *          The workflow state to check.
   * @return True if the workflow is currently active, not stopped or failed.
   */
  public static boolean isActive(String workflowState) {
    return WorkflowState.INSTANTIATED.toString().equalsIgnoreCase(workflowState)
        || WorkflowState.RUNNING.toString().equalsIgnoreCase(workflowState)
        || WorkflowState.PAUSED.toString().equalsIgnoreCase(workflowState);
  }
}
