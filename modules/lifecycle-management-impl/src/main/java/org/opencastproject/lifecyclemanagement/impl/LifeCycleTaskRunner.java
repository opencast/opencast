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

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.LifeCycleServiceException;
import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.StartWorkflowParameters;
import org.opencastproject.lifecyclemanagement.api.Status;
import org.opencastproject.lifecyclemanagement.api.TargetType;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;

import com.google.gson.Gson;

import org.apache.commons.lang3.NotImplementedException;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically checks on the tasks in the database.
 * If there are scheduled tasks this attempts to run them.
 * If there are running tasks this attempts to check if their status should be changed
 */
@Component(
    immediate = true,
    service = LifeCycleTaskRunner.class,
    property = {
        "service.description=LifeCycle Management Task Runner",
        "service.pid=org.opencastproject.lifecyclemanagement.LifeCycleTaskRunner"
    }
)
public class LifeCycleTaskRunner {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(LifeCycleTaskRunner.class);
  private static final Gson gson = new Gson();

  protected LifeCycleService lifeCycleService;
  protected WorkflowService workflowService;
  protected AssetManager assetManager;
  protected SecurityService securityService;
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The thread pool to use for dispatching queued jobs and checking on phantom services. */
  protected ScheduledExecutorService scheduledExecutor = null;
  private User systemAdminUser;
  private Organization defaultOrganization;


  @Reference(name = "lifecycle-service")
  public void setLifeCycleService(LifeCycleService lifeCycleService) {
    this.lifeCycleService = lifeCycleService;
  }

  @Reference
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
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
    logger.info("Activating LifeCycle Management Task Runner.");
    this.defaultOrganization = new DefaultOrganization();
    String systemAdminUserName = ctx.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    this.systemAdminUser = SecurityUtil.createSystemUser(systemAdminUserName, defaultOrganization);

    scheduledExecutor = Executors.newScheduledThreadPool(1);
    scheduledExecutor.scheduleWithFixedDelay(new LifeCycleTaskRunner.Runner(), 10, 5,
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
      logger.debug("LifeCycleTaskRunner runs");

      List<Organization> orgs = organizationDirectoryService.getOrganizations();

      for (Organization org : orgs) {
        SecurityUtil.runAs(securityService, org, systemAdminUser, () -> {

          // Get tasks that are ready to be run
          List<LifeCycleTask> tasks;
          try {
            tasks = lifeCycleService.getLifeCycleTasksWithStatus(Status.SCHEDULED);
          } catch (NullPointerException e) {
            logger.info("Could not get lifecycle tasks in status 'SCHEDULED' from service: ", e);
            return;
          }

          // For each task
          for (LifeCycleTask task : tasks) {
            // Check action and do action related things
            try {
              logger.debug("SCHEDULED Task " + task.getId());
              LifeCyclePolicy policy = lifeCycleService.getLifeCyclePolicyById(task.getLifeCyclePolicyId());
              switch(policy.getAction()) {
                case START_WORKFLOW -> startWorkflow((LifeCycleTaskStartWorkflow)task, policy);
                default -> throw new NotImplementedException();
              }
            } catch (NotFoundException e) {
              logger.warn("Could not start action for task with id " + task.getId() + ". Lifecycle Policy with id "
                  + task.getLifeCyclePolicyId() + " was not found.");
            } catch (UnauthorizedException e) {
              logger.warn("Could not start action for task with id " + task.getId() + ". User "
                  + securityService.getUser().getUsername() + " was not authorized to access "
                  + "lifecycle policy " + task.getLifeCyclePolicyId());
            } catch (LifeCycleServiceException e) {
              logger.warn("Could not start action for task with id " + task.getId() + " based of policy "
                  + task.getLifeCyclePolicyId());
              logger.warn(e.toString());
            }
          }

          // Get tasks that are running
          try {
            tasks = lifeCycleService.getLifeCycleTasksWithStatus(Status.STARTED);
          } catch (NullPointerException e) {
            logger.info("Could not get lifecycle tasks in status 'STARTED' from service: ", e);
            return;
          }

          // For each task
          for (LifeCycleTask task : tasks) {
            // Check action and do action related things
            try {
              logger.debug("STARTED Task " + task.getId());
              LifeCyclePolicy policy = lifeCycleService.getLifeCyclePolicyById(task.getLifeCyclePolicyId());
              switch(policy.getAction()) {
                case START_WORKFLOW -> checkIfWorkflowDone((LifeCycleTaskStartWorkflow)task);
                default -> throw new NotImplementedException();
              }
            } catch (NotFoundException e) {
              logger.warn("Could not check on running task with id " + task.getId() + ". Lifecycle Policy with id "
                  + task.getLifeCyclePolicyId() + " was not found.");
            } catch (UnauthorizedException e) {
              logger.warn("Could not check on running task with id " + task.getId() + ". User "
                  + securityService.getUser().getUsername() + " was not authorized to access "
                  + "lifecycle policy " + task.getLifeCyclePolicyId());
            }
          }

        });
      }
    }
  }

  /**
   * For a given task based on the START_WORKFLOW action, start a workflow
   * @param task the life cycle task
   * @param policy the life cycle policy
   * @throws LifeCycleServiceException If something went wrong
   */
  private void startWorkflow(LifeCycleTaskStartWorkflow task, LifeCyclePolicy policy)
          throws LifeCycleServiceException {
    String mediaPackageId = task.getTargetId();
    String actionParameters = policy.getActionParameters();

    if (policy.getTargetType() != TargetType.EVENT) {
      throw new IllegalArgumentException(
          "The action START_WORKFLOW is only supported for the targetType EVENT. Given target type was: "
              + policy.getAction()
      );
    }

    // Parse Parameters
    StartWorkflowParameters actionParametersParsed = gson.fromJson(actionParameters, StartWorkflowParameters.class);
    if (actionParametersParsed.getWorkflowId() == null) {
      task.setStatus(Status.FAILED);
      throw new IllegalArgumentException(
          "No workflowId found. Should have been at least the workflowId."
      );
    }
    String workflowId = actionParametersParsed.getWorkflowId();
    Map<String, String> workflowParameters;
    if (actionParametersParsed.getWorkflowParameters() == null) {
      workflowParameters = WorkflowPropertiesUtil.getLatestWorkflowProperties(assetManager, mediaPackageId);
    }
    workflowParameters = actionParametersParsed.getWorkflowParameters();

    // Get mediapackage
    Optional<MediaPackage> optMediaPackage = assetManager.getMediaPackage(mediaPackageId);
    if (optMediaPackage.isEmpty()) {
      throw new IllegalArgumentException(
          "Mediapackage for given id " + mediaPackageId  + " does not exist."
      );
    }
    MediaPackage mediaPackage = optMediaPackage.get();

    try {
      // If there is currently an active workflow on the mediapackage, postpone this lifecycle task
      Optional<WorkflowInstance> runningWfi = workflowService.getRunningWorkflowInstanceByMediaPackage(
          mediaPackageId, Permissions.Action.READ.toString());
      if (runningWfi.isPresent()) {
        logger.info("Workflow running on " + mediaPackageId + ". Postponing execution of task " + task.getId());
        return;
      }

      // Start workflow
      WorkflowInstance wfi = workflowService.start(
          workflowService.getWorkflowDefinitionById(workflowId),
          mediaPackage,
          workflowParameters
      );

      // Set task started when workflow is started
      logger.info("Started task " + task.getId() + ". Starting " + workflowId + " on " + mediaPackageId);
      task.setStatus(Status.STARTED);
      task.setWorkflowInstanceId(wfi.getId());
      lifeCycleService.updateLifeCycleTask(task);
    } catch (WorkflowDatabaseException | WorkflowParsingException e) {
      task.setStatus(Status.FAILED);
      throw new LifeCycleServiceException(
          "Unable to load workflow" + workflowId + " for mediapackage " + mediaPackageId + ". An error occurred.",
          e);
    } catch (NotFoundException e) {
      task.setStatus(Status.FAILED);
      throw new LifeCycleServiceException(
          "Unable to load workflow" + workflowId + " for mediapackage " + mediaPackageId
              + ". Workflow could not be found.",
          e);
    } catch (UnauthorizedException e) {
      task.setStatus(Status.FAILED);
      throw new LifeCycleServiceException(
          "Not authorized to start workflow with id " + workflowId + " for mediapackage " + mediaPackageId,
          e);
    } catch (WorkflowException e) {
      task.setStatus(Status.FAILED);
      throw new LifeCycleServiceException(
          "Unable to check if there is currently a workflow running on mediapackage " + mediaPackageId,
          e);
    }
  }

  /**
   * For a given task that started a workflow, update the task status based on the workflow status
   * @param task the life cycle task
   */
  private void checkIfWorkflowDone(LifeCycleTaskStartWorkflow task) {
    try {
      WorkflowInstance instance = workflowService.getWorkflowById(task.getWorkflowInstanceId());
      WorkflowInstance.WorkflowState wfState = instance.getState();

      if (wfState == WorkflowInstance.WorkflowState.SUCCEEDED) {
        task.setStatus(Status.FINISHED);
        lifeCycleService.updateLifeCycleTask(task);
      }
      if (wfState == WorkflowInstance.WorkflowState.FAILED) {
        task.setStatus(Status.FAILED);
        lifeCycleService.updateLifeCycleTask(task);
      }
    } catch (WorkflowDatabaseException e) {
      throw new RuntimeException(e);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    } catch (UnauthorizedException e) {
      throw new RuntimeException(e);
    }
  }
}
