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
package org.opencastproject.workflow.impl;

import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.INSTANTIATED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.RUNNING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.SUCCEEDED;

import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowParser;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * A utility class to run workflow jobs
 */
public class WorkflowRunner implements Callable<Void> {

  /** The job */
  private Job job = null;

  /** Logging facility */
  private static final Log logger = new Log(LoggerFactory.getLogger(WorkflowRunner.class));

  /** The current job */
  private final Job currentJob;

  private final ServiceRegistry serviceRegistry;

  private final OrganizationDirectoryService organizationDirectoryService;

  private final SecurityService securityService;

  private final UserDirectoryService userDirectoryService;

  private final WorkflowServiceImpl workflowServiceImpl;

  /**
   * Constructs a new job runner
   *
   * @param job
   *          the job to run
   * @param currentJob
   *          the current running job
   */
  WorkflowRunner(Job job, Job currentJob, ServiceRegistry serviceRegistry,
          OrganizationDirectoryService organizationDirectoryService, SecurityService securityService,
          UserDirectoryService userDirectoryService, WorkflowServiceImpl workflowServiceImpl) {
    this.job = job;
    this.currentJob = currentJob;
    this.securityService = securityService;
    this.organizationDirectoryService = organizationDirectoryService;
    this.serviceRegistry = serviceRegistry;
    this.userDirectoryService = userDirectoryService;
    this.workflowServiceImpl = workflowServiceImpl;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public Void call() throws Exception {
    Organization jobOrganization = organizationDirectoryService.getOrganization(job.getOrganization());
    try {
      serviceRegistry.setCurrentJob(currentJob);
      securityService.setOrganization(jobOrganization);
      User jobUser = userDirectoryService.loadUser(job.getCreator());
      securityService.setUser(jobUser);
      process(job);
    } finally {
      serviceRegistry.setCurrentJob(null);
      securityService.setUser(null);
      securityService.setOrganization(null);
    }
    return null;
  }

  /**
   * Processes the workflow job.
   *
   * @param job
   *          the job
   * @return the job payload
   * @throws Exception
   *           if job processing fails
   */
  private String process(Job job) throws Exception {
    List<String> arguments = job.getArguments();
    WorkflowServiceImpl.Operation op = null;
    WorkflowInstance workflowInstance = null;
    WorkflowOperationInstance wfo;
    String operation = job.getOperation();
    try {
      try {
        op = WorkflowServiceImpl.Operation.valueOf(operation);
        switch (op) {
          case START_WORKFLOW:
            workflowInstance = WorkflowParser.parseWorkflowInstance(job.getPayload());
            logger.debug("Starting new workflow %s", workflowInstance);
            runWorkflow(workflowInstance);
            break;
          case RESUME:
            workflowInstance = workflowServiceImpl.getWorkflowById(Long.parseLong(arguments.get(0)));
            Map<String, String> properties = null;
            if (arguments.size() > 1) {
              Properties props = new Properties();
              props.load(IOUtils.toInputStream(arguments.get(arguments.size() - 1), StandardCharsets.UTF_8));
              properties = new HashMap<>();
              for (Map.Entry<Object, Object> entry : props.entrySet()) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
              }
            }
            logger.debug("Resuming %s at %s", workflowInstance, workflowInstance.getCurrentOperation());
            workflowInstance.setState(RUNNING);
            workflowServiceImpl.update(workflowInstance);
            runWorkflowOperation(workflowInstance, properties);
            break;
          case START_OPERATION:
            workflowInstance = workflowServiceImpl.getWorkflowById(Long.parseLong(arguments.get(0)));
            wfo = workflowInstance.getCurrentOperation();

            if (WorkflowOperationInstance.OperationState.RUNNING.equals(wfo.getState())
                    || WorkflowOperationInstance.OperationState.PAUSED.equals(wfo.getState())) {
              logger.info("Reset operation state %s %s to INSTANTIATED due to job restart", workflowInstance, wfo);
              wfo.setState(WorkflowOperationInstance.OperationState.INSTANTIATED);
            }

            wfo.setExecutionHost(job.getProcessingHost());
            logger.debug("Running %s %s", workflowInstance, wfo);
            wfo = runWorkflowOperation(workflowInstance, null);
            updateOperationJob(job.getId(), wfo.getState());
            break;
          default:
            throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
        }
      } catch (IllegalArgumentException e) {
        throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
      } catch (IndexOutOfBoundsException e) {
        throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations",
                e);
      } catch (NotFoundException e) {
        logger.warn(e.getMessage());
        updateOperationJob(job.getId(), WorkflowOperationInstance.OperationState.FAILED);
      }
      return null;
    } catch (Exception e) {
      logger.warn(e, "Exception while accepting job " + job);
      try {
        if (workflowInstance != null) {
          logger.warn("Marking job {} and workflow instance {} as failed", job, workflowInstance);
          updateOperationJob(job.getId(), WorkflowOperationInstance.OperationState.FAILED);
          workflowInstance.setState(FAILED);
          workflowServiceImpl.update(workflowInstance);
        } else {
          logger.warn(e, "Unable to parse workflow instance");
        }
      } catch (WorkflowDatabaseException e1) {
        throw new ServiceRegistryException(e1);
      }
      if (e instanceof ServiceRegistryException)
        throw e;
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Executes the workflow.
   *
   * @param workflow
   *          the workflow instance
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws UnauthorizedException
   */
  private Job runWorkflow(WorkflowInstance workflow) throws WorkflowException, UnauthorizedException {
    if (!INSTANTIATED.equals(workflow.getState())) {

      // If the workflow is "running", we need to determine if there is an operation being executed or not.
      // When a workflow has been restarted, this might not be the case and the status might not have been
      // updated accordingly.
      if (RUNNING.equals(workflow.getState())) {
        WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
        if (currentOperation != null) {
          if (currentOperation.getId() != null) {
            try {
              Job operationJob = serviceRegistry.getJob(currentOperation.getId());
              if (Job.Status.RUNNING.equals(operationJob.getStatus())) {
                logger.debug("Not starting workflow %s, it is already in running state", workflow);
                return null;
              } else {
                logger.info("Scheduling next operation of workflow %s", workflow);
                operationJob.setStatus(Job.Status.QUEUED);
                operationJob.setDispatchable(true);
                return serviceRegistry.updateJob(operationJob);
              }
            } catch (Exception e) {
              logger.warn("Error determining status of current workflow operation in {}: {}", workflow, e.getMessage());
              return null;
            }
          }
        } else {
          throw new IllegalStateException("Cannot start a workflow '" + workflow + "' with no current operation");
        }
      } else {
        throw new IllegalStateException("Cannot start a workflow in state '" + workflow.getState() + "'");
      }
    }

    // If this is a new workflow, move to the first operation
    workflow.setState(RUNNING);
    workflowServiceImpl.update(workflow);

    WorkflowOperationInstance operation = workflow.getCurrentOperation();

    if (operation == null)
      throw new IllegalStateException("Cannot start a workflow without a current operation");

    if (operation.getPosition() != 0)
      throw new IllegalStateException("Current operation expected to be first");

    try {
      logger.info("Scheduling workflow %s for execution", workflow.getId());
      Job job = serviceRegistry.createJob(workflowServiceImpl.JOB_TYPE,
              WorkflowServiceImpl.Operation.START_OPERATION.toString(),
              Collections.singletonList(Long.toString(workflow.getId())), null, false, null,
              workflowServiceImpl.WORKFLOW_JOB_LOAD);
      operation.setId(job.getId());
      workflowServiceImpl.update(workflow);
      job.setStatus(Job.Status.QUEUED);
      job.setDispatchable(true);
      return serviceRegistry.updateJob(job);
    } catch (ServiceRegistryException e) {
      throw new WorkflowDatabaseException(e);
    } catch (NotFoundException e) {
      // this should be impossible
      throw new IllegalStateException("Unable to find a job that was just created");
    }
  }

  /**
   * Executes the workflow's current operation.
   *
   * @param workflow
   *          the workflow
   * @param properties
   *          the properties that are passed in on resume
   * @return the processed workflow operation
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws UnauthorizedException
   */
  private WorkflowOperationInstance runWorkflowOperation(WorkflowInstance workflow, Map<String, String> properties)
          throws WorkflowException, UnauthorizedException {
    WorkflowOperationInstance processingOperation = workflow.getCurrentOperation();
    if (processingOperation == null)
      throw new IllegalStateException("Workflow '" + workflow + "' has no operation to run");

    // Keep the current state for later reference, it might have been changed from the outside
    WorkflowInstance.WorkflowState initialState = workflow.getState();

    // Execute the operation handler
    WorkflowOperationHandler operationHandler = selectOperationHandler(processingOperation);
    WorkflowOperationWorker worker = new WorkflowOperationWorker(operationHandler, workflow, properties,
            workflowServiceImpl);
    workflow = worker.execute();

    // The workflow has been serialized/deserialized in between, so we need to refresh the reference
    int currentOperationPosition = processingOperation.getPosition();
    processingOperation = workflow.getOperations().get(currentOperationPosition);

    Long currentOperationJobId = processingOperation.getId();
    try {
      updateOperationJob(currentOperationJobId, processingOperation.getState());
    } catch (NotFoundException e) {
      throw new IllegalStateException("Unable to find a job that has already been running");
    } catch (ServiceRegistryException e) {
      throw new WorkflowDatabaseException(e);
    }

    // Move on to the next workflow operation
    WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();

    // Is the workflow done?
    if (currentOperation == null) {

      // If we are in failing mode, we were simply working off an error handling workflow
      if (FAILING.equals(workflow.getState())) {
        workflow.setState(FAILED);
      }

      // Otherwise, let's make sure we didn't miss any failed operation, since the workflow state could have been
      // switched to paused while processing the error handling workflow extension
      else if (!FAILED.equals(workflow.getState())) {
        workflow.setState(SUCCEEDED);
        for (WorkflowOperationInstance op : workflow.getOperations()) {
          if (op.getState().equals(WorkflowOperationInstance.OperationState.FAILED)) {
            if (op.isFailWorkflowOnException()) {
              workflow.setState(FAILED);
              break;
            }
          }
        }
      }

      // Save the updated workflow to the database
      logger.debug("%s has %s", workflow, workflow.getState());
      workflowServiceImpl.update(workflow);

    } else {

      // Somebody might have set the workflow to "paused" from the outside, so take a look a the database first
      WorkflowInstance.WorkflowState dbWorkflowState;
      try {
        dbWorkflowState = workflowServiceImpl.getWorkflowById(workflow.getId()).getState();
      } catch (NotFoundException e) {
        throw new IllegalStateException("The workflow with ID " + workflow.getId()
                + " can not be found in the database", e);
      } catch (UnauthorizedException e) {
        throw new IllegalStateException("The workflow with ID " + workflow.getId() + " can not be read", e);
      }

      // If somebody changed the workflow state from the outside, that state should take precedence
      if (!dbWorkflowState.equals(initialState)) {
        logger.info("Workflow state for %s was changed to '%s' from the outside", workflow, dbWorkflowState);
        workflow.setState(dbWorkflowState);
      }

      // Save the updated workflow to the database

      Job job;
      switch (workflow.getState()) {
        case FAILED:
          workflowServiceImpl.update(workflow);
          break;
        case FAILING:
        case RUNNING:
          try {
            job = serviceRegistry.createJob(workflowServiceImpl.JOB_TYPE,
                    WorkflowServiceImpl.Operation.START_OPERATION.toString(),
                    Collections.singletonList(Long.toString(workflow.getId())), null, false, null,
                    workflowServiceImpl.WORKFLOW_JOB_LOAD);
            currentOperation.setId(job.getId());
            workflowServiceImpl.update(workflow);
            job.setStatus(Job.Status.QUEUED);
            job.setDispatchable(true);
            serviceRegistry.updateJob(job);
          } catch (ServiceRegistryException e) {
            throw new WorkflowDatabaseException(e);
          } catch (NotFoundException e) {
            // this should be impossible
            throw new IllegalStateException("Unable to find a job that was just created");
          }
          break;
        case PAUSED:
        case STOPPED:
        case SUCCEEDED:
          workflowServiceImpl.update(workflow);
          break;
        case INSTANTIATED:
          workflowServiceImpl.update(workflow);
          throw new IllegalStateException("Impossible workflow state found during processing");
        default:
          throw new IllegalStateException("Unknown workflow state found during processing");
      }

    }
    return processingOperation;
  }

  /**
   * Does a lookup of available operation handlers for the given workflow operation.
   *
   * @param operation
   *          the operation definition
   * @return the handler or <code>null</code>
   */
  private WorkflowOperationHandler selectOperationHandler(WorkflowOperationInstance operation) {
    List<WorkflowOperationHandler> handlerList = new ArrayList<>();
    for (HandlerRegistration handlerReg : workflowServiceImpl.getRegisteredHandlers()) {
      if (handlerReg.operationName != null && handlerReg.operationName.equals(operation.getTemplate())) {
        handlerList.add(handlerReg.handler);
      }
    }
    if (handlerList.size() > 1) {
      throw new IllegalStateException("Multiple operation handlers found for operation '" + operation.getTemplate()
              + "'");
    } else if (handlerList.size() == 1) {
      return handlerList.get(0);
    }
    logger.warn("No workflow operation handlers found for operation '%s'", operation.getTemplate());
    return null;
  }

  /**
   * Synchronizes the workflow operation's job with the operation status if the operation has a job associated with it,
   * which is determined by looking at the operation's job id.
   *
   * @param state
   *          the operation state
   * @param jobId
   *          the associated job
   * @return the updated job or <code>null</code> if there is no job for this operation
   * @throws ServiceRegistryException
   *           if the job can't be updated in the service registry
   * @throws NotFoundException
   *           if the job can't be found
   */
  private Job updateOperationJob(Long jobId, WorkflowOperationInstance.OperationState state) throws NotFoundException,
          ServiceRegistryException {
    if (jobId == null)
      return null;
    Job job = serviceRegistry.getJob(jobId);
    switch (state) {
      case FAILED:
      case RETRY:
        job.setStatus(Job.Status.FAILED);
        break;
      case PAUSED:
        job.setStatus(Job.Status.PAUSED);
        job.setOperation(WorkflowServiceImpl.Operation.RESUME.toString());
        break;
      case SKIPPED:
      case SUCCEEDED:
        job.setStatus(Job.Status.FINISHED);
        break;
      default:
        throw new IllegalStateException("Unexpected state '" + state + "' found");
    }
    return serviceRegistry.updateJob(job);
  }
}
