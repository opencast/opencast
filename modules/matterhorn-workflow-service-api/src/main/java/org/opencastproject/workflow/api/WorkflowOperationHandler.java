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

import org.opencastproject.job.api.JobContext;

import java.util.SortedMap;

/**
 * Handler for workflow operations.
 */
public interface WorkflowOperationHandler {

  /**
   * The identifier used to map a workflow operation to its handler
   *
   * @return This handler's identifier
   */
  String getId();

  /**
   * Returns a description of what this handler does.
   *
   * @return The handler's description
   */
  String getDescription();

  /**
   * Returns the configuration keys that this handler accepts, along with a description of their purpose.
   *
   * @return The configuration keys and their meaning
   */
  SortedMap<String, String> getConfigurationOptions();

  /**
   * Runs the workflow operation on this {@link WorkflowInstance}. If the execution fails for some reason, this must
   * throw a {@link WorkflowOperationException} in order to handle the problem gracefully. Runtime exceptions will cause
   * the entire workflow instance to fail.
   *
   * @param workflowInstance
   *          the workflow instance
   * @param context
   *          the job context
   * @return the {@link WorkflowOperationResult} containing a potentially modified MediaPackage and whether to put the
   *         workflow instance into a wait state.
   *
   * @throws WorkflowOperationException
   *           If the workflow operation fails to execute properly, and the default error handling should be invoked.
   */
  WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException;

  /**
   * Skips the workflow operation on this {@link WorkflowInstance}. If the execution fails for some reason, this must
   * throw a {@link WorkflowOperationException} in order to handle the problem gracefully. Runtime exceptions will cause
   * the entire workflow instance to fail.
   *
   * @param workflowInstance
   *          the workflow instance
   * @param context
   *          the job context
   * @return TODO
   * @throws WorkflowOperationException
   *           If the workflow operation fails to execute properly, and the default error handling should be invoked.
   */
  WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException;

  /**
   * Clean up after a workflow operation finishes
   *
   * @param workflowInstance
   *          The workflow instance
   * @param context
   *          the job context
   * @throws WorkflowOperationException
   *           If the workflow operation fails to clean up properly.
   */
  void destroy(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException;

}
