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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import java.util.List;
import java.util.Map;

/**
 * Manages {@link WorkflowDefinition}s and {@link WorkflowInstance}s.
 */
public interface WorkflowService {
  /**
   * The service registration property we use to identify which workflow operation a {@link WorkflowOperationHandler}
   * should handle.
   */
  String WORKFLOW_OPERATION_PROPERTY = "workflow.operation";

  /** Identifier for workflow jobs */
  String JOB_TYPE = "org.opencastproject.workflow";

  /** Identifier for read permissions */
  String READ_PERMISSION = "read";

  /** Identifier for write permissions */
  String WRITE_PERMISSION = "write";

  /**
   * Adds a workflow listener to be notified when workflows are updated.
   *
   * @param listener
   *          the workflow listener to add
   */
  void addWorkflowListener(WorkflowListener listener);

  /**
   * Removes a workflow listener.
   *
   * @param listener
   *          the workflow listener to remove
   */
  void removeWorkflowListener(WorkflowListener listener);

  /**
   * Registers a new workflow definition. If a workflow definition with the same identifier is already registered, it
   * will be replaced.
   *
   * @param workflow
   *          the new workflow definition
   * @throws WorkflowDatabaseException
   *           if there is a problem registering the workflow definition
   */
  void registerWorkflowDefinition(WorkflowDefinition workflow) throws WorkflowDatabaseException;

  /**
   * Removes the workflow definition with this identifier.
   *
   * @throws NotFoundException
   *           if there is no workflow registered with this identifier
   * @throws WorkflowDatabaseException
   *           if there is a problem unregistering the workflow definition
   */
  void unregisterWorkflowDefinition(String workflowDefinitionId) throws NotFoundException, WorkflowDatabaseException;

  /**
   * Returns the {@link WorkflowDefinition} identified by <code>name</code> or <code>null</code> if no such definition
   * was found.
   *
   * @param id
   *          the workflow definition id
   * @return the workflow
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow definition
   * @throws NotFoundException
   *           if there is no registered workflow definition with this identifier
   *
   */
  WorkflowDefinition getWorkflowDefinitionById(String id) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Gets a {@link WorkflowInstance} by its ID.
   *
   * @return the workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instance from persistence
   * @throws NotFoundException
   *           if there is no workflow instance with this identifier
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  WorkflowInstance getWorkflowById(long workflowId) throws WorkflowDatabaseException, NotFoundException,
          UnauthorizedException;

  /**
   * Finds workflow instances based on the specified query.
   *
   * @param query
   *          The query parameters
   * @return The {@link WorkflowSet} containing the workflow instances matching the query parameters
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances from persistence
   */
  WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException;

  /**
   * Finds workflow instances based on the specified query for administrative access.
   *
   * @param q
   *          The query parameters
   * @return The {@link WorkflowSet} containing the workflow instances matching the query parameters
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances from persistence
   * @throws UnauthorizedException
   *           if the user does not own an administrative role
   */
  WorkflowSet getWorkflowInstancesForAdministrativeRead(WorkflowQuery q) throws WorkflowDatabaseException,
          UnauthorizedException;

  /**
   * Creates a new workflow instance and starts the workflow.
   *
   * @param workflowDefinition
   *          the workflow definition
   * @param mediaPackage
   *          the mediapackage to process
   * @param properties
   *          any properties to apply to the workflow definition
   * @return The new workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   * @throws WorkflowParsingException
   *           if there is a problem parsing or serializing workflow entities
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) throws WorkflowDatabaseException, WorkflowParsingException;

  /**
   * Creates a new workflow instance and starts the workflow.
   *
   * @param workflowDefinition
   *          the workflow definition
   * @param mediaPackage
   *          the mediapackage to process
   * @param parentWorkflowId
   *          An existing workflow to associate with the new workflow instance
   * @param properties
   *          any properties to apply to the workflow definition
   * @return The new workflow instance
   * @throws NotFoundException
   *           if the parent workflow does not exist
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   * @throws WorkflowParsingException
   *           if there is a problem parsing or serializing workflow entities
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage, Long parentWorkflowId,
          Map<String, String> properties) throws WorkflowDatabaseException, WorkflowParsingException, NotFoundException;

  /**
   * Creates a new workflow instance and starts the workflow.
   *
   * @param workflowDefinition
   *          the workflow definition
   * @param mediaPackage
   *          the mediapackage to process
   * @return The new workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   * @throws WorkflowParsingException
   *           if there is a problem parsing or serializing workflow entities
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage)
          throws WorkflowDatabaseException, WorkflowParsingException;

  /**
   * Gets the total number of workflows that have been created to date.
   *
   * @return The number of workflow instances, regardless of their state
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances in persistence
   */
  long countWorkflowInstances() throws WorkflowDatabaseException;

  /**
   * Gets the total number of workflows that have been created to date and that match all of the specified criterias
   * such as the workflow state or the current operation, both of which might be <code>null</code>.
   *
   * @param state
   *          the workflow state
   * @param operation
   *          the current operation identifier
   * @return The number of workflow instances, regardless of their state
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances in persistence
   */
  long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException;

  /**
   * Returns the statistics for the workflow service.
   *
   * @return workflow service statistics
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances in persistence
   */
  WorkflowStatistics getStatistics() throws WorkflowDatabaseException;

  /**
   * Stops a running workflow instance.
   *
   * @param workflowInstanceId
   *          the workflow instance identifier
   * @return the workflow instance
   * @throws NotFoundException
   *           if no running workflow with this identifier exists
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  WorkflowInstance stop(long workflowInstanceId) throws WorkflowException, NotFoundException, UnauthorizedException;

  /**
   * Permanently removes a workflow instance. Only workflow instances with state {@link WorkflowState#SUCCEEDED},
   * {@link WorkflowState#STOPPED} or {@link WorkflowState#FAILED} may be removed.
   *
   * @param workflowInstanceId
   *          the workflow instance identifier
   * @throws WorkflowDatabaseException
   *           if there is a problem writing to the database
   * @throws NotFoundException
   *           if no workflow instance with the given identifier could be found
   * @throws UnauthorizedException
   *           if the current user does not have {@link #WRITE_PERMISSION} on the workflow instance
   * @throws WorkflowStateException
   *           if the workflow instance is in a disallowed state
   */
  void remove(long workflowInstanceId) throws WorkflowDatabaseException, WorkflowParsingException, NotFoundException,
          UnauthorizedException, WorkflowStateException;

  /**
   * Temporarily suspends a started workflow instance.
   *
   * @param workflowInstanceId
   *          the workflow instance identifier
   * @return the workflow instance
   * @throws NotFoundException
   *           if no running workflow with this identifier exists
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  WorkflowInstance suspend(long workflowInstanceId) throws WorkflowException, NotFoundException, UnauthorizedException;

  /**
   * Resumes a suspended workflow instance.
   *
   * @param workflowInstanceId
   *          the workflow instance identifier
   * @return the workflow instance
   * @throws NotFoundException
   *           if no paused workflow with this identifier exists
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  WorkflowInstance resume(long workflowInstanceId) throws NotFoundException, WorkflowException, UnauthorizedException;

  /**
   * Resumes a suspended workflow instance, applying new properties to the workflow.
   *
   * @param workflowInstanceId
   *          the workflow to resume
   * @param properties
   *          the properties to apply to the resumed workflow
   * @return the workflow instance
   * @throws NotFoundException
   *           if no workflow with this identifier exists
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws IllegalStateException
   *           if the workflow with this identifier is not in the paused state
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  WorkflowInstance resume(long workflowInstanceId, Map<String, String> properties) throws NotFoundException,
          WorkflowException, IllegalStateException, UnauthorizedException;

  /**
   * Updates the given workflow instance with regard to the media package, the properties and the operations involved.
   *
   * @param workflowInstance
   *          the workflow instance
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  void update(WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException;

  /**
   * Gets the list of available workflow definitions. In order to be "available", a workflow definition must be
   * registered and must have registered workflow operation handlers for each of the workflow definition's operations.
   *
   * @return The list of currently available workflow definitions, sorted by title
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the registered workflow definitions
   */
  List<WorkflowDefinition> listAvailableWorkflowDefinitions() throws WorkflowDatabaseException;

  /**
   * Starts a cleanup of workflow instances with a given lifetime and a specific state
   *
   * @param lifetime
   *          minimum lifetime of the workflow instances
   * @param state
   *          state of the workflow instances
   */
  void cleanupWorkflowInstances(int lifetime, WorkflowInstance.WorkflowState state) throws WorkflowDatabaseException,
          UnauthorizedException;
}
