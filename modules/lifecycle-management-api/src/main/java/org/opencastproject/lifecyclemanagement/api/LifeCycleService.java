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

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQueryField;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import java.util.List;
import java.util.Map;

public interface LifeCycleService {

  /**
   * Returns a life cycle policy from the database by its id
   * @param id life cycle policy id
   * @return The {@link LifeCyclePolicy} belonging to the id
   * @throws NotFoundException If no life cycle policy with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have read access for the life cycle policy
   */
  LifeCyclePolicy getLifeCyclePolicyById(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  /**
   * Returns a life cycle policy from the database by its id
   * @param id life cycle policy id
   * @param orgId life cycle policy organization
   * @return The {@link LifeCyclePolicy} belonging to the id
   * @throws NotFoundException If no life cycle policy with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have read access for the life cycle policy
   */
  LifeCyclePolicy getLifeCyclePolicyById(String id, String orgId)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  /**
   * Get multiple life cycle policies from the database
   * @param limit The maximum amount of life cycle policies to get with one request.
   * @param offset The index of the first result to return.
   * @param sortCriterion In what manner results should be sorted
   * @return A list of {@link LifeCyclePolicy}s
   * @throws IllegalStateException If something went wrong in the database service
   */
  List<LifeCyclePolicy> getLifeCyclePolicies(int limit, int offset, SortCriterion sortCriterion)
          throws IllegalStateException;

  /**
   * Get currently active life cycle policies
   * @return A list of {@link LifeCyclePolicy}s
   * @throws IllegalStateException If something went wrong in the database service
   */
  List<LifeCyclePolicy> getActiveLifeCyclePolicies()
          throws IllegalStateException;

  /**
   * Creates a single life cycle policy in the database.
   * @param policy The {@link LifeCyclePolicy} to create
   * @return The created {@link LifeCyclePolicy}
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for an existing life cycle policy
   */
  LifeCyclePolicy createLifeCyclePolicy(LifeCyclePolicy policy) throws UnauthorizedException;

  /**
   * Updates a single life cycle policy in the database.
   * @param policy The {@link LifeCyclePolicy} to create or update with
   * @return The updated {@link LifeCyclePolicy}
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for an existing life cycle policy
   */
  boolean updateLifeCyclePolicy(LifeCyclePolicy policy)
          throws IllegalStateException, UnauthorizedException, IllegalArgumentException;

  /**
   * Deletes a life cycle policy from the database
   * @param id The life cycle policy identifier
   * @return The removed {@link LifeCyclePolicy}
   * @throws NotFoundException If no life cycle policy with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the life cycle policy
   */
  boolean deleteLifeCyclePolicy(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  /**
   * Delete all policies created from configuration files from the database.
   * Only to be called as admin from the policy scanner.
   * @param orgId
   */
  void deleteAllLifeCyclePoliciesCreatedByConfig(String orgId);

  /**
   * Returns a life cycle task from the database by its id
   * @param id life cycle task id
   * @return The {@link LifeCyclePolicy} belonging to the id
   * @throws NotFoundException If no life cycle task with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have read access for the life cycle task
   */
  LifeCycleTask getLifeCycleTaskById(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  /**
   * Returns a life cycle task from the database by its target identifier
   * @param targetId the id of the entity the task will act on
   * @return The {@link LifeCyclePolicy} belonging to the id
   * @throws NotFoundException If no life cycle task with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have read access for the life cycle task
   */
  LifeCycleTask getLifeCycleTaskByTargetId(String targetId)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  /**
   * Get multiple life cycle tasks based on their status
   * @param status The state the tasks should be in
   * @return A list of {@link LifeCycleTask}s
   * @throws IllegalStateException If something went wrong in the database service
   */
  List<LifeCycleTask> getLifeCycleTasksWithStatus(Status status) throws IllegalStateException;

  /**
   * Creates a new life cycle task in the database
   * @param task The {@link LifeCycleTask} to create
   * @return The created {@link LifeCycleTask}
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for an existing life cycle task
   */
  LifeCycleTask createLifeCycleTask(LifeCycleTask task) throws UnauthorizedException;

  /**
   * Updates a life cycle task
   * @param task The {@link LifeCycleTask} to create or update with
   * @return The updated {@link LifeCycleTask}
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for an existing life cycle task
   */
  boolean updateLifeCycleTask(LifeCycleTask task)
          throws IllegalStateException, UnauthorizedException, IllegalArgumentException;

  /**
   * Deletes a life cycle task from the database
   * @param id The life cycle task identifier
   * @return The removed {@link LifeCycleTask}
   * @throws NotFoundException If no life cycle task with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the life cycle task
   */
  boolean deleteLifeCycleTask(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  /**
   * Checks if the given policy has all required fields filled in
   * @param policy The lifecycle policy to be checked
   * @return True if the policy fulfills the criteria set by the function, else false
   */
  boolean checkValidity(LifeCyclePolicy policy);

  /**
   * Get a list of events based on a set of filters.
   * Currently only runs on metadata.
   * Events are returned from the index.
   * @param filters Key value pairs that the events must match.
   * @return The list of events
   * @throws SearchIndexException If something went wrong in the index service
   */
  List<Event> filterForEvents(Map<String, EventSearchQueryField<String>> filters) throws SearchIndexException;
}
