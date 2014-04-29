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

/**
 * A listener that is notified when the workflow service updates a workflow instance.
 */
public interface WorkflowListener {

  /**
   * Called when the current operation of a workflow has changed.
   *
   * @param workflow the workflow instance
   */
  void operationChanged(WorkflowInstance workflow);

  /**
   * Called when the state of a workflow instance has changed.
   *
   * @param workflow
   */
  void stateChanged(WorkflowInstance workflow);

}
