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

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.Status;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.NotImplementedException;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically checks on the policies in the database.
 * If policy timing is met, gets a list of entities based on policy filters and creates life cycle tasks for them.
 */
@Component(
    immediate = true,
    service = PolicyCheckRunner.class,
    property = {
        "service.description=LifeCycle Management Policy Checker",
        "service.pid=org.opencastproject.lifecyclemanagement.PolicyCheckRunner"
    }
)
public class PolicyCheckRunner {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PolicyCheckRunner.class);

  protected LifeCycleService lifeCycleService;
  protected SecurityService securityService;
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The thread pool to use for dispatching queued jobs and checking on phantom services. */
  protected ScheduledExecutorService scheduledExecutor = null;
  private User systemAdminUser;
  private Organization defaultOrganization;

  private Set<RepeatingPolicyRunner> repeatingPolicyRunners = new HashSet<>();

  @Reference
  public void setLifeCycleService(LifeCycleService lifeCycleService) {
    this.lifeCycleService = lifeCycleService;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService orgDirServ) {
    this.organizationDirectoryService = orgDirServ;
  }

  @Activate
  public void activate(BundleContext ctx) {
    logger.info("Activating LifeCycle Management Policy Checker.");
    this.defaultOrganization = new DefaultOrganization();
    String systemAdminUserName = ctx.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    this.systemAdminUser = SecurityUtil.createSystemUser(systemAdminUserName, defaultOrganization);

    scheduledExecutor = Executors.newScheduledThreadPool(1);
    scheduledExecutor.scheduleWithFixedDelay(new Runner(), 10, 5,
        TimeUnit.SECONDS);
  }

  @Deactivate
  public void deactivate() {
    if (scheduledExecutor != null) {
      try {
        scheduledExecutor.shutdownNow();
        if (!scheduledExecutor.isShutdown()) {
          logger.info("Waiting for Dispatcher to terminate");
          scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
        }
      } catch (InterruptedException e) {
        logger.error("Error shutting down the Dispatcher", e);
      }
    }
  }

  class Runner implements Runnable {
    @Override
    public void run() {
      try {
        logger.debug("PolicyCheck runs");

        List<Organization> orgs = organizationDirectoryService.getOrganizations();

        for (Organization org : orgs) {
          SecurityUtil.runAs(securityService, org, systemAdminUser, () -> {

            // Get all (active) policies
            List<LifeCyclePolicy> policies;
            try {
              policies = lifeCycleService.getActiveLifeCyclePolicies();
            } catch (NullPointerException e) {
              logger.info("NPE: ", e);
              return;
            }

            for (LifeCyclePolicy policy : policies) {
              if (!policy.isActive()) {
                logger.debug("Policy " + policy.getTitle() + " is deactivated. Skipping.");
                continue;
              }
              // If isSpecificDate
              if (policy.getTiming() == Timing.SPECIFIC_DATE) {
                logger.debug("Policy " + policy.getTitle() + " with specific date " + policy.getActionDate());
                // If current date > action date
                if (policy.getActionDate().before(new Date())) {

                  try {
                    // Deactivate policy
                    policy.setActive(false);
                    lifeCycleService.updateLifeCyclePolicy(policy);

                    // Get events this policy applies to
                    List<String> entityIds = new ArrayList<>();
                    switch(policy.getTargetType()) {
                      case EVENT -> {
                        List<Event> events = lifeCycleService.filterForEvents(policy.getTargetFilters());
                        for (Event event : events) {
                          entityIds.add(event.getIdentifier());
                        }
                      }
                      default -> throw new NotImplementedException();
                    }

                    // and create tasks
                    for (String entityId : entityIds) {
                      // If entity does not yet have a task for this policy
                      try {
                        lifeCycleService.getLifeCycleTaskByTargetId(entityId);
                        // Task does exist, skip creating one
                        continue;
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
                      logger.info("Created task based on policy " + policy.getTitle());
                    }
                  } catch (SearchIndexException e) {
                    logger.warn(e.toString());
                  } catch (UnauthorizedException e) {
                    logger.warn(e.toString());
                  }
                }
                // If Always (is new entity eligible NOW? How about NOW?)
              } else if (policy.getTiming() == Timing.ALWAYS) {
                logger.debug("Policy " + policy.getTitle() + " is always checked");

                try {
                  // Filter for entities
                  List<String> entityIds = new ArrayList<>();
                  switch(policy.getTargetType()) {
                    case EVENT -> {
                      List<Event> events = lifeCycleService.filterForEvents(policy.getTargetFilters());
                      for (Event event : events) {
                        entityIds.add(event.getIdentifier());
                      }
                    }
                    default -> throw new NotImplementedException();
                  }
                  // For every entity
                  for (String entityId : entityIds) {
                    // If entity does not yet have a task for this policy
                    try {
                      lifeCycleService.getLifeCycleTaskByTargetId(entityId);
                      // Task does exist, skip creating one
                      continue;
                    } catch (NotFoundException e) {
                      // Task does not exist yet, so create one
                    }

                    // Create task
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
                    logger.info("Created task based on policy " + policy.getTitle());
                  }
                } catch (SearchIndexException e) {
                  logger.warn(e.toString());
                } catch (UnauthorizedException e) {
                  logger.warn(e.toString());
                }

              } else if (policy.getTiming() == Timing.REPEATING) {

                // Start a timer for the policy
                // If there is not already a timer running
                Optional optRunner = repeatingPolicyRunners.stream().parallel()
                    .filter(r -> r.getLifeCyclePolicy().getId().equals(policy.getId()))
                    .findAny();
                if (optRunner.isEmpty()) {
                  logger.debug("Start repeating policy runner for policy " + policy.getTitle());
                  RepeatingPolicyRunner runner = new RepeatingPolicyRunner(
                      policy,
                      org,
                      lifeCycleService,
                      securityService,
                      systemAdminUser);
                  runner.schedule();
                  repeatingPolicyRunners.add(runner);
                }
              }
            }
          });
        }
        // Prevent Runner from crashing due to exceptions
        // TODO: Is this reasonable?
        // TODO: Can we implement stop/restart for this somehow?
      } catch (Exception e) {
        logger.error("Unknown exception: " + e);
      }
    }
  }
}
