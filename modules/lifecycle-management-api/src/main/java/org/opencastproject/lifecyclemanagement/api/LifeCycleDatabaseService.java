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

  LifeCyclePolicy getLifeCyclePolicy(String id) throws NotFoundException, LifeCycleDatabaseException;

  LifeCyclePolicy getLifeCyclePolicy(String id, String orgId) throws NotFoundException, LifeCycleDatabaseException;

  List<LifeCyclePolicy> getLifeCyclePolicies(int limit, int offset, SortCriterion sortCriterion)
          throws LifeCycleDatabaseException;

  List<LifeCyclePolicy> getActiveLifeCyclePolicies(String orgId)  throws LifeCycleDatabaseException;

  LifeCyclePolicy createLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException;

  boolean updateLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException;

  boolean deleteLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException;



  LifeCycleTask getLifeCycleTask(String id) throws NotFoundException, LifeCycleDatabaseException;

  LifeCycleTask getLifeCycleTaskByTargetId(String targetId) throws NotFoundException, LifeCycleDatabaseException;

  List<LifeCycleTask> getLifeCycleTasksWithStatus(Status status, String orgId) throws LifeCycleDatabaseException;

  LifeCycleTask createLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException;

  boolean updateLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException;

  boolean deleteLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException;
}
