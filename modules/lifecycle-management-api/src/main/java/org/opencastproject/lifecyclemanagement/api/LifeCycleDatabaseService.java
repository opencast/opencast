/*
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
package org.opencastproject.lifecyclemanagement.api;

import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import java.util.List;

/**
 * API that defines persistent storage of lifecycle elements
 */
public interface LifeCycleDatabaseService {

  /**
   * Gets a single life cycle policy in the current organization context by its identifier.
   * @param id the life cycle policy identifier
   * @return the {@link LifeCyclePolicy} with the given identifier
   * @throws NotFoundException if there is no life cycle policy with this identifier
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  LifeCyclePolicy getLifeCyclePolicy(String id) throws NotFoundException, LifeCycleDatabaseException;

  /**
   * Gets a single life cycle policy by its identifier.
   * @param id the life cycle policy identifier
   * @param orgId the organisation identifier
   * @return the {@link LifeCyclePolicy} with the given identifier
   * @throws NotFoundException if there is no life cycle policy  with this identifier
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  LifeCyclePolicy getLifeCyclePolicy(String id, String orgId) throws NotFoundException, LifeCycleDatabaseException;

  /**
   * Get the total number of policies
   * @param orgId the organisation identifier
   * @return the total number of policies
   * @throws LifeCycleDatabaseException
   */
  long getLifeCyclePoliciesTotal(String orgId) throws LifeCycleDatabaseException;

  /**
   * Get several life cycle policies based on their order in the database
   * @param limit Maximum amount of life cycle policys to return
   * @param offset The index of the first result to return
   * @return a list of {@link LifeCyclePolicy}s
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  List<LifeCyclePolicy> getLifeCyclePolicies(int limit, int offset, SortCriterion sortCriterion)
          throws LifeCycleDatabaseException;

  /**
   * Get currently active life cycle policies
   * @param orgId the organisation identifier
   * @return a list of {@link LifeCyclePolicy}s
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  List<LifeCyclePolicy> getActiveLifeCyclePolicies(String orgId)  throws LifeCycleDatabaseException;

  /**
   * Creates a single life cycle policy in the database.
   * @param policy The {@link LifeCyclePolicy}
   * @param orgId the organisation identifier
   * @return The created {@link LifeCyclePolicy}
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  LifeCyclePolicy createLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException;

  /**
   * Updates a single life cycle policy in the database.
   * @param policy The {@link LifeCyclePolicy}
   * @param orgId the organisation identifier
   * @return If the life cycle policy was successfully updated
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  boolean updateLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException;

  /**
   * Removes a single life cycle policy in the database.
   * @param policy The {@link LifeCyclePolicy}
   * @param orgId the organisation identifier
   * @return If the life cycle policy was successfully updated
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  boolean deleteLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException;

  /**
   * Remove all life cycle policies that were created through configuration files from the database
   * @param orgId
   * @throws LifeCycleDatabaseException
   */
  void deleteAllLifeCyclePoliciesCreatedByConfig(String orgId) throws LifeCycleDatabaseException;

  /**
   * Gets a single life cycle task in the current organization context by its identifier.
   * @param id the life cycle task identifier
   * @return the {@link LifeCycleTask} with the given identifier
   * @throws NotFoundException if there is no life cycle task with this identifier
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  LifeCycleTask getLifeCycleTask(String id) throws NotFoundException, LifeCycleDatabaseException;

  /**
   * Gets a single life cycle task in the current organization context by its target identifier.
   * @param targetId the id of the entity the task will act on
   * @return the {@link LifeCycleTask} with the given identifier
   * @throws NotFoundException if there is no life cycle task with this identifier
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  LifeCycleTask getLifeCycleTaskByTargetId(String targetId) throws NotFoundException, LifeCycleDatabaseException;

  /**
   * Get several life cycle policies based on their status
   * @param status The state the tasks should be in
   * @param orgId the organisation identifier
   * @return a list of {@link LifeCycleTask}s
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  List<LifeCycleTask> getLifeCycleTasksWithStatus(Status status, String orgId) throws LifeCycleDatabaseException;

  /**
   * Creates a single life cycle task.
   * @param task The {@link LifeCycleTask}
   * @param orgId the organisation identifier
   * @return The created {@link LifeCycleTask}
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  LifeCycleTask createLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException;

  /**
   * Updates a single life cycle task.
   * @param task The {@link LifeCycleTask}
   * @param orgId the organisation identifier
   * @return If the life cycle task was successfully updated
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  boolean updateLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException;

  /**
   * Removes a single life cycle task.
   * @param task The {@link LifeCycleTask}
   * @param orgId the organisation identifier
   * @return If the life cycle task was successfully updated
   * @throws LifeCycleDatabaseException if there is a problem communicating with the underlying data store
   */
  boolean deleteLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException;
}
