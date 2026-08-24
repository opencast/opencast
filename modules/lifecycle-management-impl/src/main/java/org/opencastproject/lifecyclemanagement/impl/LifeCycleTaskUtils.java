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

package org.opencastproject.lifecyclemanagement.impl;

import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.LifeCycleServiceException;
import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.Status;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared helper for {@link PolicyCheckRunner} and {@link RepeatingPolicyRunner}. */
final class LifeCycleTaskUtils {

  private static final Logger logger = LoggerFactory.getLogger(LifeCycleTaskUtils.class);

  private LifeCycleTaskUtils() {
  }

  /**
   * Creates a scheduled {@link LifeCycleTask} for the given policy and target entity, unless a task for that
   * policy and entity already exists.
   *
   * @param lifeCycleService the service used to look up and persist tasks
   * @param policy the policy the task is created for
   * @param entityId the id of the entity the task will act on
   * @throws LifeCycleServiceException If something went wrong in the lifecycle service
   */
  static void createTaskIfNotExists(LifeCycleService lifeCycleService, LifeCyclePolicy policy, String entityId)
          throws LifeCycleServiceException {
    try {
      LifeCycleTask existingTask = lifeCycleService.getLifeCycleTaskByTargetId(entityId);
      if (existingTask.getLifeCyclePolicyId().equals(policy.getId())) {
        // Task already exists, skip creating one
        return;
      }
    } catch (NotFoundException e) {
      // Task does not exist yet, so create one
    }

    LifeCycleTask task;
    if (policy.getAction() == Action.START_WORKFLOW) {
      task = new LifeCycleTaskStartWorkflow();
    } else {
      task = new LifeCycleTaskImpl();
    }

    task.setLifeCyclePolicyId(policy.getId());
    task.setTargetId(entityId);
    task.setStatus(Status.SCHEDULED);

    lifeCycleService.createLifeCycleTask(task);
    logger.debug("Created task based on policy " + policy.getTitle());
  }
}
