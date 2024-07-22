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
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import java.util.List;
import java.util.Map;

public interface LifeCycleService {

  /**
   * Returns a playlist from the database by its id
   * @param id playlist id
   * @return The {@link LifeCyclePolicy} belonging to the id
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have read access for the playlist
   */
  LifeCyclePolicy getLifeCyclePolicyById(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  List<LifeCyclePolicy> getLifeCyclePolicies(int limit, int offset, SortCriterion sortCriterion)
          throws IllegalStateException;

  List<LifeCyclePolicy> getActiveLifeCyclePolicies()
          throws IllegalStateException;

  LifeCyclePolicy createLifeCyclePolicy(LifeCyclePolicy policy) throws UnauthorizedException;

  /**
   * Persist a new playlist in the database or update an existing one
   * @param playlist The {@link LifeCyclePolicy} to create or update with
   * @return The updated {@link LifeCyclePolicy}
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for an existing playlist
   */
  boolean updateLifeCyclePolicy(LifeCyclePolicy playlist)
          throws IllegalStateException, UnauthorizedException, IllegalArgumentException;

  /**
   * Deletes a playlist from the database
   * @param id The playlist identifier
   * @return The removed {@link LifeCyclePolicy}
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the playlist
   */
  boolean deleteLifeCyclePolicy(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  /**
   * Returns a playlist from the database by its id
   * @param id playlist id
   * @return The {@link LifeCyclePolicy} belonging to the id
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have read access for the playlist
   */
  LifeCycleTask getLifeCycleTaskById(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  LifeCycleTask getLifeCycleTaskByTargetId(String targetId)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  List<LifeCycleTask> getLifeCycleTasksWithStatus(Status status) throws IllegalStateException;

  LifeCycleTask createLifeCycleTask(LifeCycleTask task) throws UnauthorizedException;

  /**
   * Persist a new playlist in the database or update an existing one
   * @param playlist The {@link LifeCycleTask} to create or update with
   * @return The updated {@link LifeCycleTask}
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for an existing playlist
   */
  boolean updateLifeCycleTask(LifeCycleTask playlist)
          throws IllegalStateException, UnauthorizedException, IllegalArgumentException;

  /**
   * Deletes a playlist from the database
   * @param id The playlist identifier
   * @return The removed {@link LifeCycleTask}
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the playlist
   */
  boolean deleteLifeCycleTask(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException;

  List<Event> filterForEntities(Map<String, String> filters) throws SearchIndexException;
}
