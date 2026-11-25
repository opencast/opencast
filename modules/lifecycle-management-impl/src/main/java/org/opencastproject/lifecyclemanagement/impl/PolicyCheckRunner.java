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

import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.LifeCycleServiceException;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.opensearch.api.SearchIndexException;
import org.opencastproject.opensearch.index.objects.event.Event;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

  private final Set<RepeatingPolicyRunner> repeatingPolicyRunners = new HashSet<>();
  private boolean stopping;

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

    synchronized (repeatingPolicyRunners) {
      stopping = false;
    }
    scheduledExecutor = Executors.newScheduledThreadPool(1);
    scheduledExecutor.scheduleWithFixedDelay(new Runner(), 10, 5,
        TimeUnit.SECONDS);
  }

  @Deactivate
  public void deactivate() {
    synchronized (repeatingPolicyRunners) {
      stopping = true;
      repeatingPolicyRunners.forEach(RepeatingPolicyRunner::unschedule);
      repeatingPolicyRunners.clear();
    }
    if (scheduledExecutor != null) {
      try {
        scheduledExecutor.shutdownNow();
        if (!scheduledExecutor.isTerminated()) {
          logger.info("Waiting for Dispatcher to terminate");
          scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.error("Error shutting down the Dispatcher", e);
      }
    }
  }

  private void reconcileRepeatingPolicies(Organization org, List<LifeCyclePolicy> policies) {
    synchronized (repeatingPolicyRunners) {
      if (stopping) {
        return;
      }
      Map<String, LifeCyclePolicy> repeatingPolicies = policies.stream()
          .filter(policy -> policy.isActive() && policy.getTiming() == Timing.REPEATING)
          .collect(Collectors.toMap(LifeCyclePolicy::getId, policy -> policy));

      repeatingPolicyRunners.removeIf(runner -> {
        if (!runner.getOrganization().getId().equals(org.getId())) {
          return false;
        }
        LifeCyclePolicy policy = repeatingPolicies.get(runner.getLifeCyclePolicy().getId());
        if (policy == null || !Objects.equals(policy.getCronTrigger(), runner.getLifeCyclePolicy().getCronTrigger())) {
          runner.unschedule();
          return true;
        }
        return false;
      });

      for (LifeCyclePolicy policy : repeatingPolicies.values()) {
        boolean scheduled = repeatingPolicyRunners.stream().anyMatch(runner ->
            runner.getOrganization().getId().equals(org.getId())
                && runner.getLifeCyclePolicy().getId().equals(policy.getId()));
        if (!scheduled) {
          logger.debug("Start repeating policy runner for policy {}", policy.getTitle());
          RepeatingPolicyRunner runner = new RepeatingPolicyRunner(
              policy, org, lifeCycleService, securityService, systemAdminUser);
          runner.schedule();
          repeatingPolicyRunners.add(runner);
        }
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
            } catch (NullPointerException | LifeCycleServiceException e) {
              logger.info("Could not get active policies ", e);
              return;
            }

            reconcileRepeatingPolicies(org, policies);

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
                    // Get events this policy applies to
                    List<String> entityIds = new ArrayList<>();
                    switch(policy.getTargetType()) {
                      case EVENT -> {
                        for (Event event : lifeCycleService.filterForEvents(policy.getTargetFilters())) {
                          entityIds.add(event.getIdentifier());
                        }
                      }
                      default -> throw new NotImplementedException();
                    }

                    // and create tasks (if not already exists)
                    for (String entityId : entityIds) {
                      LifeCycleTaskUtils.createTaskIfNotExists(lifeCycleService, policy, entityId);
                    }

                    // Deactivate policy
                    policy.setActive(false);
                    lifeCycleService.updateLifeCyclePolicy(policy);
                  } catch (SearchIndexException e) {
                    logger.warn(e.toString());
                  } catch (UnauthorizedException e) {
                    logger.warn(e.toString());
                  } catch (NotFoundException e) {
                    logger.warn(e.toString());
                  } catch (LifeCycleServiceException e) {
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
                      for (Event event : lifeCycleService.filterForEvents(policy.getTargetFilters())) {
                        entityIds.add(event.getIdentifier());
                      }
                    }
                    default -> throw new NotImplementedException();
                  }
                  // For every entity, create a task if it doesn't already have one for this policy
                  for (String entityId : entityIds) {
                    LifeCycleTaskUtils.createTaskIfNotExists(lifeCycleService, policy, entityId);
                  }
                } catch (SearchIndexException e) {
                  logger.warn(e.toString());
                } catch (NotFoundException e) {
                  logger.warn(e.toString());
                } catch (LifeCycleServiceException e) {
                  logger.warn(e.toString());
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
