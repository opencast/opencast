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

package org.opencastproject.workflow.impl;

import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workflow.api.WorkflowStatistics;

/**
 * Provides persistence services to the workflow service implementation.
 */
public interface WorkflowServiceIndex {

  /**
   * Update the workflow instance, or add it to persistence if it is not already stored.
   *
   * @param instance
   *          The workflow instance to store
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance
   */
  void update(WorkflowInstance instance) throws WorkflowDatabaseException;

  /**
   * Remove the workflow instance with this id.
   *
   * @param id
   *          The workflow instance id
   * @throws WorkflowDatabaseException
   *           if there is a problem removing the workflow instance from persistence
   * @throws NotFoundException
   *           if there is no workflow instance with this identifier
   */
  void remove(long id) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Gets the total number of workflows that have been created to date.
   *
   * @param state
   *          the workflow state
   * @param operation
   *          the current operation identifier
   * @return The number of workflow instances, regardless of their state
   * @throws WorkflowDatabaseException
   *           if there is a problem retrieving the workflow instance count from persistence
   */
  long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException;

  /**
   * Gets a set of workflow instances using a custom query
   *
   * @param query
   *          the query to use in the search for workflow instances
   * @param action
   *          ACL action (e.g. read or write) to check for
   * @param applyPermissions
   *          whether to apply the permissions to the query. Set to false for administrative queries.
   *
   * @return the set of matching workflow instances
   * @throws WorkflowDatabaseException
   *           if there is a problem retrieving the workflow instances from persistence
   */
  WorkflowSetImpl getWorkflowInstances(WorkflowQuery query, String action, boolean applyPermissions) throws WorkflowDatabaseException;

  /**
   * Returns the workflow statistics.
   *
   * @return workflow statistics
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances in persistence
   */
  WorkflowStatistics getStatistics() throws WorkflowDatabaseException;

}
