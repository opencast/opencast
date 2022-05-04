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

import org.opencastproject.util.NotFoundException;

import java.util.List;

/**
 * API that defines persistent storage of workflows
 *
 */
public interface WorkflowServiceDatabase {

  /**
   * Gets a single workflow by its identifier.
   *
   * @param workflowId
   *          the series identifier
   * @return the {@link WorkflowInstance} for this workflow
   * @throws NotFoundException
   *           if there is no workflow with this identifier
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  WorkflowInstance getWorkflow(long workflowId) throws NotFoundException, WorkflowDatabaseException;

  /**
   * Gets all workflow instances.
   * Unlike other queries this one does NOT care about the current organization, but will really return ALL workflow
   * instances.
   * Warning: Potentially very resource intensive. Only used for populating Solr index.
   *
   * @return list of all {@link WorkflowInstance}s
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  List<WorkflowInstance> getAllWorkflowInstancesOrganizationIndependent() throws WorkflowDatabaseException;

  /**
   * Gets all workflow instances.
   * Warning: Potentially very resource intensive. Only used for populating Solr index.
   *
   * @param limit
   *          max number of workflows to be returned
   * @param offset
   *          only return workflows from this point onwards
   * @return list of all {@link WorkflowInstance}s
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  List<WorkflowInstance> getWorkflowInstances(int limit, int offset) throws WorkflowDatabaseException;

  /**
   * Gets all workflow instances for a mediapackage.
   *
   * @param mediaPackageId
   *          the mediapackage id
   * @return list of all {@link WorkflowInstance}s for the given mediapackage id
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  List<WorkflowInstance> getWorkflowInstancesByMediaPackage(String mediaPackageId) throws WorkflowDatabaseException;

  /**
   * Gets all workflow instances that are currently running on the mediapackage.
   *
   * @param mediaPackageId
   *          the mediapackage id
   * @return list of all {@link WorkflowInstance}s for the given mediapackage id
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  List<WorkflowInstance> getRunningWorkflowInstancesByMediaPackage(String mediaPackageId) throws WorkflowDatabaseException;

  /**
   * Returns true if the mediapackage with the given identifier currently has a workflow running on it.
   *
   * @param mediaPackageId
   *          the mediapackage identifier
   * @return true, if a workflow is running; false otherwise
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  boolean mediaPackageHasActiveWorkflows(String mediaPackageId) throws WorkflowDatabaseException;

  /**
   * Updates a single workflow.
   *
   * @param instance
   *          the workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  void updateInDatabase(WorkflowInstance instance) throws WorkflowDatabaseException;

  /**
   * Removes a single workflow.
   *
   * @param instance
   *          the workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  void removeFromDatabase(WorkflowInstance instance) throws WorkflowDatabaseException;
}
