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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.INSTANTIATED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.PAUSED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.RUNNING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.STOPPED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.SUCCEEDED;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.rebuild.AbstractIndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildException;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.metadata.api.MetadataService;
import org.opencastproject.metadata.api.util.MediaPackageMetadataSupport;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.UndispatchableJobException;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.ReadinessIndicator;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowIdentifier;
import org.opencastproject.workflow.api.WorkflowIndexData;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowOperationResultImpl;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowServiceDatabase;
import org.opencastproject.workflow.api.WorkflowStateException;
import org.opencastproject.workflow.api.WorkflowStateMapping;
import org.opencastproject.workflow.api.XmlWorkflowParser;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;
import com.google.common.util.concurrent.Striped;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements WorkflowService with in-memory data structures to hold WorkflowOperations and WorkflowInstances.
 * WorkflowOperationHandlers are looked up in the OSGi service registry based on the "workflow.operation" property. If
 * the WorkflowOperationHandler's "workflow.operation" service registration property matches
 * WorkflowOperation.getName(), then the factory returns a WorkflowOperationRunner to handle that operation. This allows
 * for custom runners to be added or modified without affecting the workflow service itself.
 */
@Component(
  property = {
    "service.description=Workflow Service",
    "service.pid=org.opencastproject.workflow.impl.WorkflowServiceImpl"
  },
  immediate = true,
  service = { WorkflowService.class, WorkflowServiceImpl.class, IndexProducer.class }
)
public class WorkflowServiceImpl extends AbstractIndexProducer implements WorkflowService, JobProducer {

  /** Retry strategy property name */
  private static final String RETRY_STRATEGY = "retryStrategy";

  /** Logging facility */
  private static final Log logger = new Log(LoggerFactory.getLogger(WorkflowServiceImpl.class));

  /** List of available operations on jobs */
  enum Operation {
    START_WORKFLOW, RESUME, START_OPERATION
  }

  /** Constant value indicating a <code>null</code> parent id */
  private static final String NULL_PARENT_ID = "-";

  /** The load imposed on the system by a workflow job.
   *  We are keeping this hardcoded because otherwise bad things will likely happen,
   *  like an inability to process a workflow past a certain point in high-load conditions
   */
  private static final float WORKFLOW_JOB_LOAD = 0.0f;

  /** Error resolution handler id constant */
  public static final String ERROR_RESOLUTION_HANDLER_ID = "error-resolution";

  /** Remove references to the component context once felix scr 1.2 becomes available */
  protected ComponentContext componentContext = null;

  /** The metadata services */
  private SortedSet<MediaPackageMetadataService> metadataServices;

  /** Persistent storage */
  protected WorkflowServiceDatabase persistence;

  /** The list of workflow listeners */
  private final List<WorkflowListener> listeners = new CopyOnWriteArrayList<WorkflowListener>();

  /** The thread pool to use for firing listeners and handling dispatched jobs */
  protected ThreadPoolExecutor executorService;

  /** The workspace */
  protected Workspace workspace = null;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The series service */
  protected SeriesService seriesService;

  /** The asset manager */
  protected AssetManager assetManager = null;

  /** The workflow definition scanner */
  private WorkflowDefinitionScanner workflowDefinitionScanner;

  /** List of initially delayed workflows */
  private final List<Long> delayedWorkflows = new ArrayList<Long>();

  /** Striped locks for synchronization */
  private final Striped<Lock> lock = Striped.lazyWeakLock(1024);
  private final Striped<Lock> updateLock = Striped.lazyWeakLock(1024);
  private final Striped<Lock> mediaPackageLocks = Striped.lazyWeakLock(1024);

  /** The Elasticsearch indices */
  private ElasticsearchIndex index;

  /**
   * Constructs a new workflow service impl, with a priority-sorted map of metadata services
   */
  public WorkflowServiceImpl() {
    metadataServices = new TreeSet<>(Comparator.comparingInt(MetadataService::getPriority));
  }

  /**
   * Activate this service implementation via the OSGI service component runtime.
   *
   * @param componentContext
   *          the component context
   */
  @Activate
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    logger.info("Activate Workflow service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#addWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void addWorkflowListener(WorkflowListener listener) {
    listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#removeWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void removeWorkflowListener(WorkflowListener listener) {
    listeners.remove(listener);
  }

  /**
   * Fires the workflow listeners on workflow updates.
   */
  protected void fireListeners(final WorkflowInstance oldWorkflowInstance, final WorkflowInstance newWorkflowInstance) {
    final User currentUser = securityService.getUser();
    final Organization currentOrganization = securityService.getOrganization();
    for (final WorkflowListener listener : listeners) {
      if (oldWorkflowInstance == null || !oldWorkflowInstance.getState().equals(newWorkflowInstance.getState())) {
        Runnable runnable = () -> {
          try {
            securityService.setUser(currentUser);
            securityService.setOrganization(currentOrganization);
            listener.stateChanged(newWorkflowInstance);
          } finally {
            securityService.setUser(null);
            securityService.setOrganization(null);
          }
        };
        executorService.execute(runnable);
      } else {
        logger.debug("Not notifying %s because the workflow state has not changed", listener);
      }

      if (newWorkflowInstance.getCurrentOperation() != null) {
        if (oldWorkflowInstance == null || oldWorkflowInstance.getCurrentOperation() == null
                || !oldWorkflowInstance.getCurrentOperation().equals(newWorkflowInstance.getCurrentOperation())) {
          Runnable runnable = () -> {
            try {
              securityService.setUser(currentUser);
              securityService.setOrganization(currentOrganization);
              listener.operationChanged(newWorkflowInstance);
            } finally {
              securityService.setUser(null);
              securityService.setOrganization(null);
            }
          };
          executorService.execute(runnable);
        }
      } else {
        logger.debug("Not notifying %s because the workflow operation has not changed", listener);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
   */
  @Override
  public List<WorkflowDefinition> listAvailableWorkflowDefinitions() {
    return workflowDefinitionScanner
            .getAvailableWorkflowDefinitions(securityService.getOrganization(), securityService.getUser())
            .sorted()
            .collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   */
  public boolean isRunnable(WorkflowDefinition workflowDefinition) {
    List<String> availableOperations = listAvailableOperationNames();
    List<WorkflowDefinition> checkedWorkflows = new ArrayList<>();
    boolean runnable = isRunnable(workflowDefinition, availableOperations, checkedWorkflows);
    int wfCount = checkedWorkflows.size() - 1;
    if (runnable)
      logger.info("Workflow %s, containing %d derived workflows, is runnable", workflowDefinition, wfCount);
    else
      logger.warn("Workflow %s, containing %d derived workflows, is not runnable", workflowDefinition, wfCount);
    return runnable;
  }

  /**
   * Tests the workflow definition for its runnability. This method is a helper for
   * {@link #isRunnable(WorkflowDefinition)} that is suited for recursive calling.
   *
   * @param workflowDefinition
   *          the definition to test
   * @param availableOperations
   *          list of currently available operation handlers
   * @param checkedWorkflows
   *          list of checked workflows, used to avoid circular checking
   * @return <code>true</code> if all bits and pieces used for executing <code>workflowDefinition</code> are in place
   */
  private boolean isRunnable(WorkflowDefinition workflowDefinition, List<String> availableOperations,
          List<WorkflowDefinition> checkedWorkflows) {
    if (checkedWorkflows.contains(workflowDefinition))
      return true;

    // Test availability of operation handler and catch workflows
    for (WorkflowOperationDefinition op : workflowDefinition.getOperations()) {
      if (!availableOperations.contains(op.getId())) {
        logger.info("%s is not runnable due to missing operation %s", workflowDefinition, op);
        return false;
      }
      String catchWorkflow = op.getExceptionHandlingWorkflow();
      if (catchWorkflow != null) {
        WorkflowDefinition catchWorkflowDefinition;
        try {
          catchWorkflowDefinition = getWorkflowDefinitionById(catchWorkflow);
        } catch (NotFoundException e) {
          logger.info("%s is not runnable due to missing catch workflow %s on operation %s", workflowDefinition,
                  catchWorkflow, op);
          return false;
        }
        if (!isRunnable(catchWorkflowDefinition, availableOperations, checkedWorkflows))
          return false;
      }
    }

    // Add the workflow to the list of checked workflows
    if (!checkedWorkflows.contains(workflowDefinition))
      checkedWorkflows.add(workflowDefinition);
    return true;
  }

  /**
   * Gets the currently registered workflow operation handlers.
   *
   * @return All currently registered handlers
   */
  public Set<HandlerRegistration> getRegisteredHandlers() {
    Set<HandlerRegistration> set = new HashSet<>();
    ServiceReference[] refs;
    try {
      refs = componentContext.getBundleContext().getServiceReferences(WorkflowOperationHandler.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      throw new IllegalStateException(e);
    }
    if (refs != null) {
      for (ServiceReference ref : refs) {
        WorkflowOperationHandler handler = (WorkflowOperationHandler) componentContext.getBundleContext().getService(
                ref);
        set.add(new HandlerRegistration((String) ref.getProperty(WORKFLOW_OPERATION_PROPERTY), handler));
      }
    } else {
      logger.warn("No registered workflow operation handlers found");
    }
    return set;
  }

  protected WorkflowOperationHandler getWorkflowOperationHandler(String operationId) {
    for (HandlerRegistration reg : getRegisteredHandlers()) {
      if (reg.operationName.equals(operationId))
        return reg.handler;
    }
    return null;
  }

  /**
   * Lists the names of each workflow operation. Operation names are availalbe for use if there is a registered
   * {@link WorkflowOperationHandler} with an equal {@link WorkflowServiceImpl#WORKFLOW_OPERATION_PROPERTY} property.
   *
   * @return The {@link List} of available workflow operation names
   */
  protected List<String> listAvailableOperationNames() {
    return getRegisteredHandlers().parallelStream().map(op -> op.operationName).collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowById(long)
   */
  @Override
  public WorkflowInstance getWorkflowById(long id) throws NotFoundException,
          UnauthorizedException {
    try {
      WorkflowInstance workflow = persistence.getWorkflow(id);
      assertPermission(workflow, Permissions.Action.READ.toString(), workflow.getOrganizationId());
      return workflow;
    } catch (WorkflowDatabaseException e) {
      throw new IllegalStateException("Got not get workflow from database with id ");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage)
          throws WorkflowDatabaseException, UnauthorizedException, WorkflowParsingException {
    return start(workflowDefinition, mediaPackage, new HashMap<>());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties)
          throws WorkflowDatabaseException, UnauthorizedException, WorkflowParsingException {
    try {
      return start(workflowDefinition, mediaPackage, null, properties);
    } catch (NotFoundException e) {
      // should never happen
      throw new IllegalStateException("a null workflow ID caused a NotFoundException.  This is a programming error.");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage, Long, java.util.Map)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage sourceMediaPackage,
          Long parentWorkflowId, Map<String, String> originalProperties) throws WorkflowDatabaseException,
          NotFoundException, UnauthorizedException, WorkflowParsingException, IllegalStateException {
    final String mediaPackageId = sourceMediaPackage.getIdentifier().toString();
    Map<String, String> properties = null;

    // WorkflowPropertiesUtil.storeProperties will take a snapshot if there isn't one
    // and we want the mp in the snapshot to have all the metadata populated.
    populateMediaPackageMetadata(sourceMediaPackage);

    if (originalProperties != null) {
      WorkflowPropertiesUtil.storeProperties(assetManager, sourceMediaPackage, originalProperties);
      properties = WorkflowPropertiesUtil.getLatestWorkflowProperties(assetManager, mediaPackageId);
    }

    // We have to synchronize per media package to avoid starting multiple simultaneous workflows for one media package.
    final Lock lock = mediaPackageLocks.get(mediaPackageId);
    lock.lock();

    try {
      logger.startUnitOfWork();
      if (workflowDefinition == null)
        throw new IllegalArgumentException("workflow definition must not be null");
      for (List<String> errors : MediaPackageSupport.sanityCheck(sourceMediaPackage)) {
        throw new IllegalArgumentException("Insane media package cannot be processed: " + String.join("; ", errors));
      }
      if (parentWorkflowId != null) {
        try {
          getWorkflowById(parentWorkflowId); // Let NotFoundException bubble up
        } catch (UnauthorizedException e) {
          throw new IllegalArgumentException("Parent workflow " + parentWorkflowId + " not visible to this user");
        }
      } else {
        if (persistence.mediaPackageHasActiveWorkflows(mediaPackageId)) {
          throw new IllegalStateException(String.format(
                  "Can't start workflow '%s' for media package '%s' because another workflow is currently active.",
                  workflowDefinition.getTitle(),
                  sourceMediaPackage.getIdentifier().toString()));
        }
      }

      // Get the current user
      User currentUser = securityService.getUser();
      validUserOrThrow(currentUser);

      // Get the current organization
      Organization organization = securityService.getOrganization();
      if (organization == null)
        throw new SecurityException("Current organization is unknown");

      WorkflowInstance workflowInstance = new WorkflowInstance(workflowDefinition, sourceMediaPackage,
              currentUser, organization, properties);
      workflowInstance = updateConfiguration(workflowInstance, properties);

      // Create and configure the workflow instance
      try {
        // Create a new job for this workflow instance
        String workflowDefinitionXml = XmlWorkflowParser.toXml(workflowDefinition);
        String mediaPackageXml = MediaPackageParser.getAsXml(sourceMediaPackage);

        List<String> arguments = new ArrayList<>();
        arguments.add(workflowDefinitionXml);
        arguments.add(mediaPackageXml);
        if (parentWorkflowId != null || properties != null) {
          String parentWorkflowIdString = (parentWorkflowId != null) ? parentWorkflowId.toString() : NULL_PARENT_ID;
          arguments.add(parentWorkflowIdString);
        }
        if (properties != null) {
          arguments.add(mapToString(properties));
        }

        Job job = serviceRegistry.createJob(JOB_TYPE, Operation.START_WORKFLOW.toString(), arguments,
                null, false, null, WORKFLOW_JOB_LOAD);

        // Have the workflow take on the job's identity
        workflowInstance.setId(job.getId());

        // Add the workflow to the search index and have the job enqueued for dispatch.
        // Update also sets ACL and mediapackage metadata
        update(workflowInstance);

        return workflowInstance;
      } catch (Throwable t) {
        try {
          workflowInstance.setState(FAILED);
          update(workflowInstance);
        } catch (Exception failureToFail) {
          logger.warn(failureToFail, "Unable to update workflow to failed state");
        }
        try {
          throw t;
        } catch (ServiceRegistryException e) {
          throw new WorkflowDatabaseException(e);
        }
      }
    } finally {
      logger.endUnitOfWork();
      lock.unlock();
    }
  }

  protected WorkflowInstance updateConfiguration(WorkflowInstance instance, Map<String, String> properties) {
    if (properties != null) {
      for (Entry<String, String> entry : properties.entrySet()) {
        instance.setConfiguration(entry.getKey(), entry.getValue());
      }
    }
    return instance;
  }

  /**
   * Does a lookup of available operation handlers for the given workflow operation.
   *
   * @param operation
   *          the operation definition
   * @return the handler or <code>null</code>
   */
  protected WorkflowOperationHandler selectOperationHandler(WorkflowOperationInstance operation) {
    List<WorkflowOperationHandler> handlerList = new ArrayList<>();
    for (HandlerRegistration handlerReg : getRegisteredHandlers()) {
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
   * Executes the workflow.
   *
   * @param workflow
   *          the workflow instance
   * @throws WorkflowException
   *           if there is a problem processing the workflow
   * @throws UnauthorizedException
   */
  protected Job runWorkflow(WorkflowInstance workflow) throws WorkflowException, UnauthorizedException {
    if (INSTANTIATED != workflow.getState()) {

      // If the workflow is "running", we need to determine if there is an operation being executed or not.
      // When a workflow has been restarted, this might not be the case and the status might not have been
      // updated accordingly.
      if (RUNNING == workflow.getState()) {
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
                operationJob.setStatus(Status.QUEUED);
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
    update(workflow);

    WorkflowOperationInstance operation = workflow.getCurrentOperation();

    if (operation == null) {
      throw new IllegalStateException("Cannot start a workflow without a current operation");
    }

    if (!operation.equals(workflow.getOperations().get(0))) {
      throw new IllegalStateException("Current operation expected to be first");
    }

    try {
      logger.info("Scheduling workflow %s for execution", workflow.getId());
      Job job = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
              Collections.singletonList(Long.toString(workflow.getId())), null, false, null, WORKFLOW_JOB_LOAD);
      operation.setId(job.getId());
      update(workflow);
      job.setStatus(Status.QUEUED);
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
  protected WorkflowOperationInstance runWorkflowOperation(WorkflowInstance workflow, Map<String, String> properties)
          throws WorkflowException, UnauthorizedException {
    WorkflowOperationInstance processingOperation = workflow.getCurrentOperation();
    if (processingOperation == null)
      throw new IllegalStateException("Workflow '" + workflow + "' has no operation to run");

    // Keep the current state for later reference, it might have been changed from the outside
    WorkflowState initialState = workflow.getState();

    // Execute the operation handler
    WorkflowOperationHandler operationHandler = selectOperationHandler(processingOperation);
    WorkflowOperationWorker worker = new WorkflowOperationWorker(operationHandler, workflow, properties, this);
    workflow = worker.execute();

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
            if (op.isFailOnError()) {
              workflow.setState(FAILED);
              break;
            }
          }
        }
      }

      // Save the updated workflow to the database
      logger.debug("%s has %s", workflow, workflow.getState());
      update(workflow);

    } else {

      // Somebody might have set the workflow to "paused" from the outside, so take a look a the database first
      WorkflowState dbWorkflowState;
      try {
        dbWorkflowState = getWorkflowById(workflow.getId()).getState();
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
          update(workflow);
          break;
        case FAILING:
        case RUNNING:
          try {
            job = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
                    Collections.singletonList(Long.toString(workflow.getId())), null, false, null, WORKFLOW_JOB_LOAD);
            currentOperation.setId(job.getId());
            update(workflow);
            job.setStatus(Status.QUEUED);
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
          update(workflow);
          break;
        case INSTANTIATED:
          update(workflow);
          throw new IllegalStateException("Impossible workflow state found during processing");
        default:
          throw new IllegalStateException("Unknown workflow state found during processing");
      }

    }

    return processingOperation;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowDefinitionById(String)
   */
  @Override
  public WorkflowDefinition getWorkflowDefinitionById(String id) throws NotFoundException {
    final WorkflowIdentifier workflowIdentifier = new WorkflowIdentifier(id, securityService.getOrganization().getId());
    final WorkflowDefinition def = workflowDefinitionScanner
            .getWorkflowDefinition(securityService.getUser(), workflowIdentifier);
    if (def == null) {
      throw new NotFoundException("Workflow definition '" + workflowIdentifier + "' not found or inaccessible");
    }
    return def;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#stop(long)
   */
  @Override
  public WorkflowInstance stop(long workflowInstanceId) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    final Lock lock = this.lock.get(workflowInstanceId);
    lock.lock();
    try {
      WorkflowInstance instance = getWorkflowById(workflowInstanceId);

      if (instance.getState() != STOPPED) {
        // Update the workflow instance
        instance.setState(STOPPED);
        update(instance);
      }

      try {
        removeTempFiles(instance);
      } catch (Exception e) {
        logger.warn("Cannot remove temp files for workflow instance {}: {}", workflowInstanceId, e.getMessage());
      }

      return instance;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Checks whether user is set and is known to the userDirectoryService
   */
  private void validUserOrThrow(User user) {
      if (user == null)
        throw new SecurityException("Current user is unknown");

      if (userDirectoryService.loadUser(user.getUsername()) == null)
        throw new SecurityException(String.format("Current user '%s' can not be loaded", user.getUsername()));
  }

  private void removeTempFiles(WorkflowInstance workflowInstance) {
    logger.info("Removing temporary files for workflow {}", workflowInstance);
    MediaPackage mp = workflowInstance.getMediaPackage();
    if (null == mp) {
      logger.warn("Workflow instance {} does not have an media package set", workflowInstance.getId());
      return;
    }
    for (MediaPackageElement elem : mp.getElements()) {
      if (null == elem.getURI()) {
        logger.warn("Mediapackage element {} from the media package {} does not have an URI set",
                elem.getIdentifier(), mp.getIdentifier().toString());
        continue;
      }
      try {
        logger.debug("Removing temporary file {} for workflow {}", elem.getURI(), workflowInstance);
        workspace.delete(elem.getURI());
      } catch (IOException e) {
        logger.warn("Unable to delete mediapackage element", e);
      } catch (NotFoundException e) {
        // File was probably already deleted before...
      }
    }
  }


  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#remove(long)
   */
  @Override
  public void remove(long workflowInstanceId) throws WorkflowDatabaseException, NotFoundException,
          UnauthorizedException, WorkflowParsingException, WorkflowStateException {
    remove(workflowInstanceId, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#remove(long,boolean)
   */
  @Override
  public void remove(long workflowInstanceId, boolean force) throws WorkflowDatabaseException, NotFoundException,
          UnauthorizedException, WorkflowStateException {
    final Lock lock = this.lock.get(workflowInstanceId);
    lock.lock();
    try {
      WorkflowInstance instance = getWorkflowById(workflowInstanceId);
      WorkflowInstance.WorkflowState state = instance.getState();
      if (!state.isTerminated() && !force) {
        throw new WorkflowStateException("Workflow instance with state '" + state + "' cannot be removed "
            + "since it is not yet terminated.");
      }

      assertPermission(instance, Permissions.Action.WRITE.toString(), instance.getOrganizationId());

      // First, remove temporary files
      removeTempFiles(instance);

      // Second, remove jobs related to operations which belong to the workflow instance
      List<WorkflowOperationInstance> operations = instance.getOperations();
      List<Long> jobsToDelete = new ArrayList<>();
      for (WorkflowOperationInstance op : operations) {
        if (op.getId() != null) {
          long workflowOpId = op.getId();
          if (workflowOpId != workflowInstanceId) {
            jobsToDelete.add(workflowOpId);
          }
        }
      }
      try {
        serviceRegistry.removeJobs(jobsToDelete);
      } catch (ServiceRegistryException e) {
        logger.warn("Problems while removing jobs related to workflow operations '%s'", jobsToDelete, e);
      } catch (NotFoundException e) {
        logger.debug("No jobs related to one of the workflow operations '%s' found in the service registry",
                jobsToDelete);
      }

      // Third, remove workflow instance job itself
      try {
        serviceRegistry.removeJobs(Collections.singletonList(workflowInstanceId));
        removeWorkflowInstanceFromIndex(instance.getId());
      } catch (ServiceRegistryException e) {
        logger.warn("Problems while removing workflow instance job '%d'", workflowInstanceId, e);
      } catch (NotFoundException e) {
        logger.info("No workflow instance job '%d' found in the service registry", workflowInstanceId);
      }

      // Remove workflow from database
      persistence.removeFromDatabase(instance);
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(long)
   */
  @Override
  public WorkflowInstance suspend(long workflowInstanceId) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    final Lock lock = this.lock.get(workflowInstanceId);
    lock.lock();
    try {
      WorkflowInstance instance = getWorkflowById(workflowInstanceId);
      instance.setState(PAUSED);
      update(instance);
      return instance;
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#resume(long)
   */
  @Override
  public WorkflowInstance resume(long id) throws WorkflowException, NotFoundException, IllegalStateException,
          UnauthorizedException {
    return resume(id, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#resume(long, Map)
   */
  @Override
  public WorkflowInstance resume(long workflowInstanceId, Map<String, String> properties) throws WorkflowException,
          NotFoundException, IllegalStateException, UnauthorizedException {
    WorkflowInstance workflowInstance = getWorkflowById(workflowInstanceId);
    if (!WorkflowState.PAUSED.equals(workflowInstance.getState()))
      throw new IllegalStateException("Can not resume a workflow where the current state is not in paused");

    workflowInstance = updateConfiguration(workflowInstance, properties);
    update(workflowInstance);

    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    // Is the workflow done?
    if (currentOperation == null) {
      // Let's make sure we didn't miss any failed operation, since the workflow state could have been
      // switched to paused while processing the error handling workflow extension
      workflowInstance.setState(SUCCEEDED);
      for (WorkflowOperationInstance op : workflowInstance.getOperations()) {
        if (op.getState().equals(WorkflowOperationInstance.OperationState.FAILED)) {
          if (op.isFailOnError()) {
            workflowInstance.setState(FAILED);
            break;
          }
        }
      }

      // Save the resumed workflow to the database
      logger.debug("%s has %s", workflowInstance, workflowInstance.getState());
      update(workflowInstance);
      return workflowInstance;
    }

    // We can resume workflows when they are in either the paused state, or they are being advanced manually passed
    // certain operations. In the latter case, there is no current paused operation.
    if (OperationState.INSTANTIATED.equals(currentOperation.getState())) {
      try {
        // the operation has its own job. Update that too.
        Job operationJob = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
                Collections.singletonList(Long.toString(workflowInstanceId)), null, false, null, WORKFLOW_JOB_LOAD);

        // this method call is publicly visible, so it doesn't necessarily go through the accept method. Set the
        // workflow state manually.
        workflowInstance.setState(RUNNING);
        currentOperation.setId(operationJob.getId());

        // update the workflow and its associated job
        update(workflowInstance);

        // Now set this job to be queued so it can be dispatched
        operationJob.setStatus(Status.QUEUED);
        operationJob.setDispatchable(true);
        serviceRegistry.updateJob(operationJob);

        return workflowInstance;
      } catch (ServiceRegistryException e) {
        throw new WorkflowDatabaseException(e);
      }
    }

    Long operationJobId = workflowInstance.getCurrentOperation().getId();
    if (operationJobId == null)
      throw new IllegalStateException("Can not resume a workflow where the current operation has no associated id");

    // Set the current operation's job to queued, so it gets picked up again
    Job workflowJob;
    try {
      workflowJob = serviceRegistry.getJob(workflowInstanceId);
      workflowJob.setStatus(Status.RUNNING);
      persistence.updateInDatabase(workflowInstance);
      serviceRegistry.updateJob(workflowJob);

      Job operationJob = serviceRegistry.getJob(operationJobId);
      operationJob.setStatus(Status.QUEUED);
      operationJob.setDispatchable(true);
      if (properties != null) {
        Properties props = new Properties();
        props.putAll(properties);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        List<String> newArguments = new ArrayList<String>(operationJob.getArguments());
        newArguments.add(new String(out.toByteArray(), StandardCharsets.UTF_8));
        operationJob.setArguments(newArguments);
      }
      serviceRegistry.updateJob(operationJob);
    } catch (ServiceRegistryException e) {
      throw new WorkflowDatabaseException(e);
    } catch (IOException e) {
      throw new WorkflowParsingException("Unable to parse workflow and/or workflow properties");
    }

    return workflowInstance;
  }

  /**
   * Asserts that the current user has permission to take the provided action on a workflow instance.
   *
   * @param workflow
   *          the workflow instance
   * @param action
   *          the action to ensure is permitted
   * @throws UnauthorizedException
   *           if the action is not authorized
   */
  protected void assertPermission(WorkflowInstance workflow, String action, String workflowOrgId) throws UnauthorizedException {
    User currentUser = securityService.getUser();
    Organization currentOrg = securityService.getOrganization();
    String currentOrgAdminRole = currentOrg.getAdminRole();
    String currentOrgId = currentOrg.getId();

    MediaPackage mediapackage = workflow.getMediaPackage();

    WorkflowState state = workflow.getState();
    if (state != INSTANTIATED && state != RUNNING && workflow.getState() != FAILING) {
      Opt<MediaPackage> assetMediapackage = assetManager.getMediaPackage(mediapackage.getIdentifier().toString());
      if (assetMediapackage.isSome()) {
        mediapackage = assetMediapackage.get();
      }
    }

    User workflowCreator = userDirectoryService.loadUser(workflow.getCreatorName());
    boolean authorized = currentUser.hasRole(GLOBAL_ADMIN_ROLE)
            || (currentUser.hasRole(currentOrgAdminRole) && currentOrgId.equals(workflowOrgId))
            || (workflowCreator != null && currentUser.equals(workflowCreator))
            || (authorizationService.hasPermission(mediapackage, action) && currentOrgId.equals(workflowOrgId));

    if (!authorized) {
      throw new UnauthorizedException(currentUser, action);
    }
  }

  protected boolean assertMediaPackagePermission(String mediaPackageId, String action) {
    var currentUser = securityService.getUser();
    Opt<MediaPackage> mp = assetManager.getMediaPackage(mediaPackageId);

    // asset manager already checks if user is admin, org admin for same org as mp, or has explicit read rights
    // global admins can still get workflow instances if mp is gone from asset manager
    // org admins can't because then we don't know if mp belonged to same org as user
    return currentUser.hasRole(GLOBAL_ADMIN_ROLE)
            || mp.isSome() && currentUser.hasRole(securityService.getOrganization().getAdminRole())
            || mp.isSome() && authorizationService.hasPermission(mp.get(), action);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(final WorkflowInstance workflowInstance) throws WorkflowDatabaseException, UnauthorizedException {
    final Lock lock = updateLock.get(workflowInstance.getId());
    lock.lock();

    try {
      WorkflowInstance originalWorkflowInstance = null;
      try {
        // get workflow and assert permissions
        originalWorkflowInstance = getWorkflowById(workflowInstance.getId());
      } catch (NotFoundException e) {
        // That's fine, it's a new workflow instance
      }

      MediaPackage updatedMediaPackage = null;
      try {

        // Before we persist this, extract the metadata
        updatedMediaPackage = workflowInstance.getMediaPackage();

        populateMediaPackageMetadata(updatedMediaPackage);

        String seriesId = updatedMediaPackage.getSeries();
        if (seriesId != null && workflowInstance.getCurrentOperation() != null) {
          // If the mediapackage contains a series, find the series ACLs and add the security information to the
          // mediapackage

          try {
            AccessControlList acl = seriesService.getSeriesAccessControl(seriesId);
            Tuple<AccessControlList, AclScope> activeAcl = authorizationService.getAcl(
                updatedMediaPackage, AclScope.Series);
            // Update series ACL if it differs from the active series ACL on the media package
            if (!AclScope.Series.equals(activeAcl.getB()) || !AccessControlUtil.equals(activeAcl.getA(), acl)) {
              authorizationService.setAcl(updatedMediaPackage, AclScope.Series, acl);
            }
          } catch (NotFoundException e) {
            logger.debug("Not updating series ACL on event {} since series {} has no ACL set",
                updatedMediaPackage, seriesId, e);
          }
        }

        workflowInstance.setMediaPackage(updatedMediaPackage);
      } catch (SeriesException e) {
        throw new WorkflowDatabaseException(e);
      } catch (Exception e) {
        logger.error("Metadata for mediapackage {} could not be updated", updatedMediaPackage, e);
      }

      // Synchronize the job status with the workflow
      WorkflowState workflowState = workflowInstance.getState();

      Job job;
      try {
        job = serviceRegistry.getJob(workflowInstance.getId());
        job.setPayload(Long.toString(workflowInstance.getId()));

        // Synchronize workflow and job state
        switch (workflowState) {
          case FAILED:
            job.setStatus(Status.FAILED);
            break;
          case FAILING:
            break;
          case INSTANTIATED:
            job.setDispatchable(true);
            job.setStatus(Status.QUEUED);
            break;
          case PAUSED:
            job.setStatus(Status.PAUSED);
            break;
          case RUNNING:
            job.setStatus(Status.RUNNING);
            break;
          case STOPPED:
            job.setStatus(Status.CANCELLED);
            break;
          case SUCCEEDED:
            job.setStatus(Status.FINISHED);
            break;
          default:
            throw new IllegalStateException("Found a workflow state that is not handled");
        }
      } catch (ServiceRegistryException e) {
        throw new WorkflowDatabaseException(
            "Unable to read workflow job " + workflowInstance.getId() + " from service registry", e);
      } catch (NotFoundException e) {
        throw new WorkflowDatabaseException(
            "Job for workflow " + workflowInstance.getId() + " not found in service registry", e);
      }

      // Update both workflow and workflow job
      try {
        //Update the database
        persistence.updateInDatabase(workflowInstance);

        job = serviceRegistry.updateJob(job);

        WorkflowOperationInstance op = workflowInstance.getCurrentOperation();

        // Update index used for UI. Note that we only need certain metadata and we can safely filter out workflow
        // updates for running operations since we updated the metadata right before these operations and will do so
        // again right after those operations.
        if (op == null || op.getState() != OperationState.RUNNING) {
          // Collect necessary information only for index update
          long id = workflowInstance.getId();
          int state = workflowInstance.getState().ordinal();
          String mpId = workflowInstance.getMediaPackage().getIdentifier().toString();
          String orgId = workflowInstance.getOrganizationId();

          updateWorkflowInstanceInIndex(id, state, mpId, orgId);
        }
      } catch (ServiceRegistryException e) {
        throw new WorkflowDatabaseException("Update of workflow job " + workflowInstance.getId()
            + " in the service registry failed, service registry and workflow table may be out of sync", e);
      } catch (NotFoundException e) {
        throw new WorkflowDatabaseException("Job for workflow " + workflowInstance.getId()
            + " not found in service registry", e);
      } catch (Exception e) {
        throw new WorkflowDatabaseException("Update of workflow job " + job.getId() + " in the service registry failed, "
            + "service registry and workflow table may be out of sync", e);
      }

      try {
        fireListeners(originalWorkflowInstance, workflowInstance);
      } catch (Exception e) {
        // Can't happen, since we are converting from an in-memory object
        throw new IllegalStateException("In-memory workflow instance could not be serialized", e);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    return countWorkflowInstances(null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state) throws WorkflowDatabaseException {
    return persistence.countWorkflows(state);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstancesByMediaPackage(String)
   */
  @Override
  public List<WorkflowInstance> getWorkflowInstancesByMediaPackage(String mediaPackageId)
      throws WorkflowDatabaseException, UnauthorizedException {
    // If we have read permission to the media package, return all workflows
    if (!assertMediaPackagePermission(mediaPackageId, Permissions.Action.READ.toString())) {
      throw new UnauthorizedException("Not allowed to access event");
    }
    return persistence.getWorkflowInstancesByMediaPackage(mediaPackageId);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getRunningWorkflowInstanceByMediaPackage(String, String)
   */
  @Override
  public Optional<WorkflowInstance> getRunningWorkflowInstanceByMediaPackage(String mediaPackageId, String action)
          throws WorkflowException, UnauthorizedException, WorkflowDatabaseException {
    List<WorkflowInstance> workflowInstances = persistence.getRunningWorkflowInstancesByMediaPackage(mediaPackageId);

    // If there is more than workflow running something is very wrong
    if (workflowInstances.size() > 1) {
      throw new WorkflowException("Multiple workflows are active on mediapackage " + mediaPackageId);
    }

    Optional<WorkflowInstance> optWorkflowInstance = Optional.empty();
    if (workflowInstances.size() == 1) {
      WorkflowInstance wfInstance = workflowInstances.get(0);
      optWorkflowInstance = Optional.of(wfInstance);
      assertPermission(wfInstance, action, wfInstance.getOrganizationId());
    }

    return optWorkflowInstance;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#mediaPackageHasActiveWorkflows(String)
   */
  @Override
  public boolean mediaPackageHasActiveWorkflows(String mediaPackageId) throws WorkflowDatabaseException {
    return persistence.mediaPackageHasActiveWorkflows(mediaPackageId);
  }

  /**
   * Callback for workflow operations that were throwing an exception. This implementation assumes that the operation
   * worker has already adjusted the current operation's state appropriately.
   *
   * @param workflow
   *          the workflow instance
   * @param operation
   *          the current workflow operation
   * @return the workflow instance
   */
  protected WorkflowInstance handleOperationException(
      WorkflowInstance workflow,
      WorkflowOperationInstance currentOperation) {
    int failedAttempt = currentOperation.getFailedAttempts() + 1;
    currentOperation.setFailedAttempts(failedAttempt);

    // Operation was aborted by the user, after going into hold state
    if (ERROR_RESOLUTION_HANDLER_ID.equals(currentOperation.getTemplate())
            && OperationState.FAILED.equals(currentOperation.getState())) {
      int position = workflow.getOperations().indexOf(currentOperation);
      // Advance to operation that actually failed
      if (workflow.getOperations().size() > position + 1) { // This should always be true...
        currentOperation = workflow.getOperations().get(position + 1);
        // It's currently in RETRY state, change to FAILED
        currentOperation.setState(OperationState.FAILED);
      }
      handleFailedOperation(workflow, currentOperation);
    } else if (currentOperation.getMaxAttempts() != -1 && failedAttempt == currentOperation.getMaxAttempts()) {
      handleFailedOperation(workflow, currentOperation);
    } else {
      switch (currentOperation.getRetryStrategy()) {
        case NONE:
          handleFailedOperation(workflow, currentOperation);
          break;
        case RETRY:
          currentOperation.setState(OperationState.RETRY);
          break;
        case HOLD:
          currentOperation.setState(OperationState.RETRY);
          List<WorkflowOperationInstance> operations = workflow.getOperations();
          WorkflowOperationDefinitionImpl errorResolutionDefinition = new WorkflowOperationDefinitionImpl(
                  ERROR_RESOLUTION_HANDLER_ID, "Error Resolution Operation", "error", false);
          var errorResolutionInstance = new WorkflowOperationInstance(errorResolutionDefinition);
          errorResolutionInstance.setExceptionHandlingWorkflow(currentOperation.getExceptionHandlingWorkflow());
          var index = workflow.getOperations().indexOf(currentOperation);
          operations.add(index, errorResolutionInstance);
          workflow.setOperations(operations);
          break;
        default:
          break;
      }
    }
    return workflow;
  }

  /**
   * Handles the workflow for a failing operation.
   *
   * @param workflow
   *          the workflow
   * @param currentOperation
   *          the failing workflow operation instance
   */
  private void handleFailedOperation(WorkflowInstance workflow, WorkflowOperationInstance currentOperation) {
    String errorDefId = currentOperation.getExceptionHandlingWorkflow();

    // Adjust the workflow state according to the setting on the operation
    if (currentOperation.isFailOnError()) {
      if (StringUtils.isBlank(errorDefId)) {
        workflow.setState(FAILED);
      } else {
        workflow.setState(FAILING);

        // Remove the rest of the original workflow
        int currentOperationPosition = workflow.getOperations().indexOf(currentOperation);
        List<WorkflowOperationInstance> operations = new ArrayList<>(
                workflow.getOperations().subList(0, currentOperationPosition + 1));
        workflow.setOperations(operations);

        // Determine the current workflow configuration
        Map<String, String> configuration = new HashMap<>();
        for (String configKey : workflow.getConfigurationKeys()) {
          configuration.put(configKey, workflow.getConfiguration(configKey));
        }

        // Append the operations
        WorkflowDefinition errorDef = null;
        try {
          errorDef = getWorkflowDefinitionById(errorDefId);
          workflow.extend(errorDef);
          workflow.setOperations(updateConfiguration(workflow, configuration).getOperations());
        } catch (NotFoundException notFoundException) {
          throw new IllegalStateException("Unable to find the error workflow definition '" + errorDefId + "'");
        }
      }
    }

    // Fail the current operation
    currentOperation.setState(OperationState.FAILED);
  }

  /**
   * Callback for workflow operation handlers that executed and finished without exception. This implementation assumes
   * that the operation worker has already adjusted the current operation's state appropriately.
   *
   * @param workflow
   *          the workflow instance
   * @param result
   *          the workflow operation result
   * @return the workflow instance
   * @throws WorkflowDatabaseException
   *           if updating the workflow fails
   */
  protected WorkflowInstance handleOperationResult(WorkflowInstance workflow, WorkflowOperationResult result)
          throws WorkflowDatabaseException {

    // Get the operation and its handler
    WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
    WorkflowOperationHandler handler = getWorkflowOperationHandler(currentOperation.getTemplate());

    // Create an operation result for the lazy or else update the workflow's media package
    if (result == null) {
      logger.warn("Handling a null operation result for workflow %s in operation %s", workflow.getId(),
              currentOperation.getTemplate());
      result = new WorkflowOperationResultImpl(workflow.getMediaPackage(), null, Action.CONTINUE, 0);
    } else {
      MediaPackage mp = result.getMediaPackage();
      if (mp != null) {
        workflow.setMediaPackage(mp);
      }
    }

    // The action to take
    Action action = result.getAction();

    // Update the workflow configuration.
    workflow = updateConfiguration(workflow, result.getProperties());

    // Adjust workflow statistics
    currentOperation.setTimeInQueue(result.getTimeInQueue());

    // Adjust the operation state
    switch (action) {
      case CONTINUE:
        currentOperation.setState(OperationState.SUCCEEDED);
        break;
      case PAUSE:
        if (!(handler instanceof ResumableWorkflowOperationHandler)) {
          throw new IllegalStateException("Operation " + currentOperation.getTemplate() + " is not resumable");
        }

        // Set abortable and continuable to default values
        currentOperation.setContinuable(result.allowsContinue());
        currentOperation.setAbortable(result.allowsAbort());

        workflow.setState(PAUSED);
        currentOperation.setState(OperationState.PAUSED);
        break;
      case SKIP:
        currentOperation.setState(OperationState.SKIPPED);
        break;
      default:
        throw new IllegalStateException("Unknown action '" + action + "' returned");
    }

    if (ERROR_RESOLUTION_HANDLER_ID.equals(currentOperation.getTemplate()) && result.getAction() == Action.CONTINUE) {

      Map<String, String> resultProperties = result.getProperties();
      if (resultProperties == null || StringUtils.isBlank(resultProperties.get(RETRY_STRATEGY)))
        throw new WorkflowDatabaseException("Retry strategy not present in properties!");

      RetryStrategy retryStrategy = RetryStrategy.valueOf(resultProperties.get(RETRY_STRATEGY));
      switch (retryStrategy) {
        case NONE:
          handleFailedOperation(workflow, workflow.getCurrentOperation());
          break;
        case RETRY:
          break;
        default:
          throw new WorkflowDatabaseException("Retry strategy not implemented yet!");
      }
    }

    return workflow;
  }

  /**
   * Reads the available metadata from the dublin core catalog (if there is one) and updates the mediapackage.
   *
   * @param mp
   *          the media package
   */
  protected void populateMediaPackageMetadata(MediaPackage mp) {
    if (metadataServices.size() == 0) {
      logger.warn("No metadata services are registered, so no media package metadata can be extracted from catalogs");
      return;
    }
    for (MediaPackageMetadataService metadataService : metadataServices) {
      MediaPackageMetadata metadata = metadataService.getMetadata(mp);
      MediaPackageMetadataSupport.populateMediaPackageMetadata(mp, metadata);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#isReadyToAcceptJobs(String)
   */
  @Override
  public boolean isReadyToAcceptJobs(String operation) {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * If we are already running the maximum number of workflows, don't accept another START_WORKFLOW job
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#isReadyToAccept(org.opencastproject.job.api.Job)
   */
  @Override
  public boolean isReadyToAccept(Job job) throws UndispatchableJobException {
    String operation = job.getOperation();

    // Only restrict execution of new jobs
    if (!Operation.START_WORKFLOW.toString().equals(operation))
      return true;

    // If the first operation is guaranteed to pause, run the job.
    if (job.getArguments().size() > 1 && job.getArguments().get(0) != null) {
      try {
        WorkflowDefinition workflowDef = XmlWorkflowParser.parseWorkflowDefinition(job.getArguments().get(0));
        if (workflowDef.getOperations().size() > 0) {
          String firstOperationId = workflowDef.getOperations().get(0).getId();
          WorkflowOperationHandler handler = getWorkflowOperationHandler(firstOperationId);
          if (handler instanceof ResumableWorkflowOperationHandler) {
            if (((ResumableWorkflowOperationHandler) handler).isAlwaysPause()) {
              return true;
            }
          }
        }
      } catch (WorkflowParsingException e) {
        throw new UndispatchableJobException(job + " is not a proper job to start a workflow", e);
      }
    }

    WorkflowInstance workflow;
    Optional<WorkflowInstance> workflowInstance;
    String mediaPackageId;

    // Fetch all workflows that are running with the current media package
    try {
      workflow = getWorkflowById(job.getId());
      mediaPackageId = workflow.getMediaPackage().getIdentifier().toString();
    } catch (NotFoundException e) {
      throw new UndispatchableJobException("Trying to start workflow with job id " + job.getId()
          + " but no corresponding instance is available from the workflow service", e);
    } catch (UnauthorizedException e) {
      throw new UndispatchableJobException(
          "Authorization denied while requesting to loading workflow instance. Job: " + job.getId(), e);
    }

    try {
      workflowInstance = getRunningWorkflowInstanceByMediaPackage(
              workflow.getMediaPackage().getIdentifier().toString(), Permissions.Action.READ.toString());
    } catch (UnauthorizedException e) {
      throw new UndispatchableJobException("Authorization denied while requesting to loading workflow instance " + workflow.getId(), e);
    } catch (WorkflowDatabaseException e) {
      throw new UndispatchableJobException("An database error occurred while checking if a workflow is already active "
          + "(job: " + job.getId() + ")", e);
    } catch (WorkflowException e) {
      // Avoid running multiple workflows with same media package id at the same time
      delayWorkflow(workflow, mediaPackageId);
      return false;
    }

    // Make sure we are not excluding ourselves
    if (workflow.getId() != workflowInstance.get().getId()) {
      delayWorkflow(workflow, mediaPackageId);
      return false;
    }

    return true;
  }

  private void delayWorkflow(WorkflowInstance workflow, String mediaPackageId) {
    if (!delayedWorkflows.contains(workflow.getId())) {
      logger.info("Delaying start of workflow %s, another workflow on media package %s is still running",
              workflow.getId(), mediaPackageId);
      delayedWorkflows.add(workflow.getId());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#acceptJob(org.opencastproject.job.api.Job)
   */
  @Override
  public synchronized void acceptJob(Job job) throws ServiceRegistryException {
    User originalUser = securityService.getUser();
    Organization originalOrg = securityService.getOrganization();
    try {
      Organization organization = organizationDirectoryService.getOrganization(job.getOrganization());
      securityService.setOrganization(organization);
      User user = userDirectoryService.loadUser(job.getCreator());
      securityService.setUser(user);
      job.setStatus(Job.Status.RUNNING);
      job = serviceRegistry.updateJob(job);

      // Check if this workflow was initially delayed
      if (delayedWorkflows.contains(job.getId())) {
        delayedWorkflows.remove(job.getId());
        logger.info("Starting initially delayed workflow %s, %d more waiting", job.getId(), delayedWorkflows.size());
      }

      executorService.submit(new JobRunner(job, serviceRegistry.getCurrentJob()));
    } catch (Exception e) {
      if (e instanceof ServiceRegistryException)
        throw (ServiceRegistryException) e;
      throw new ServiceRegistryException(e);
    } finally {
      securityService.setUser(originalUser);
      securityService.setOrganization(originalOrg);
    }
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
  protected String process(Job job) throws Exception {
    List<String> arguments = job.getArguments();
    Operation op = null;
    WorkflowInstance workflowInstance = null;
    WorkflowOperationInstance wfo;
    String operation = job.getOperation();
    try {
      try {
        op = Operation.valueOf(operation);
        switch (op) {
          case START_WORKFLOW:
            workflowInstance = persistence.getWorkflow(Long.parseLong(job.getPayload()));
            logger.debug("Starting new workflow %s", workflowInstance);
            runWorkflow(workflowInstance);
            break;
          case RESUME:
            workflowInstance = getWorkflowById(Long.parseLong(arguments.get(0)));
            Map<String, String> properties = null;
            if (arguments.size() > 1) {
              Properties props = new Properties();
              props.load(IOUtils.toInputStream(arguments.get(arguments.size() - 1), StandardCharsets.UTF_8));
              properties = new HashMap<>();
              for (Entry<Object, Object> entry : props.entrySet()) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
              }
            }
            logger.debug("Resuming %s at %s", workflowInstance, workflowInstance.getCurrentOperation());
            workflowInstance.setState(RUNNING);
            update(workflowInstance);
            runWorkflowOperation(workflowInstance, properties);
            break;
          case START_OPERATION:
            workflowInstance = getWorkflowById(Long.parseLong(arguments.get(0)));
            wfo = workflowInstance.getCurrentOperation();

            if (OperationState.RUNNING.equals(wfo.getState()) || OperationState.PAUSED.equals(wfo.getState())) {
              logger.info("Reset operation state %s %s to INSTANTIATED due to job restart", workflowInstance, wfo);
              wfo.setState(OperationState.INSTANTIATED);
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
        logger.warn("Not found processing job {}: {}", job, e.getMessage());
        updateOperationJob(job.getId(), OperationState.FAILED);
      }
      return null;
    } catch (Exception e) {
      logger.warn("Exception while accepting job {}", job, e);
      try {
        if (workflowInstance != null) {
          logger.warn("Marking job {} and workflow instance {} as failed", job, workflowInstance);
          updateOperationJob(job.getId(), OperationState.FAILED);
          workflowInstance.setState(FAILED);
          update(workflowInstance);
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
  private Job updateOperationJob(Long jobId, OperationState state) throws NotFoundException, ServiceRegistryException {
    if (jobId == null)
      return null;
    Job job = serviceRegistry.getJob(jobId);
    switch (state) {
      case FAILED:
      case RETRY:
        job.setStatus(Status.FAILED);
        break;
      case PAUSED:
        job.setStatus(Status.PAUSED);
        job.setOperation(Operation.RESUME.toString());
        break;
      case SKIPPED:
      case SUCCEEDED:
        job.setStatus(Status.FINISHED);
        break;
      default:
        throw new IllegalStateException("Unexpected state '" + state + "' found");
    }
    return serviceRegistry.updateJob(job);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countJobs(Status status) throws ServiceRegistryException {
    return serviceRegistry.count(JOB_TYPE, status);
  }

  /**
   * Converts a Map<String, String> to s key=value\n string, suitable for the properties form parameter expected by the
   * workflow rest endpoint.
   *
   * @param props
   *          The map of strings
   * @return the string representation
   */
  private String mapToString(Map<String, String> props) {
    if (props == null)
      return null;
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : props.entrySet()) {
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * Dummy callback for osgi
   *
   * @param unused
   *          the unused ReadinessIndicator
   */
  @Reference(target = "(artifact=workflowdefinition)")
  protected void setProfilesReadyIndicator(ReadinessIndicator unused) { }

  /**
   * Callback for the OSGi environment to register with the <code>Workspace</code>.
   *
   * @param workspace
   *          the workspace
   */
  @Reference
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi environment to register with the <code>ServiceRegistry</code>.
   *
   * @param registry
   *          the service registry
   */
  @Reference
  protected void setServiceRegistry(ServiceRegistry registry) {
    this.serviceRegistry = registry;
  }

  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the authorization service.
   *
   * @param authorizationService
   *          the authorizationService to set
   */
  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Callback for setting the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * Sets the series service
   *
   * @param seriesService
   *          the seriesService to set
   */
  @Reference
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * Sets the asset manager
   *
   * @param assetManager
   *          the assetManager to set
   */
  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * Callback to set the metadata service
   *
   * @param service
   *          the metadata service
   */
  @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, unbind = "removeMetadataService")
  protected void addMetadataService(MediaPackageMetadataService service) {
    metadataServices.add(service);
  }

  /**
   * Callback to remove a mediapackage metadata service.
   *
   * @param service
   *          the mediapackage metadata service to remove
   */
  protected void removeMetadataService(MediaPackageMetadataService service) {
    metadataServices.remove(service);
  }

  /**
   * Callback to set the workflow definition scanner
   *
   * @param scanner
   *          the workflow definition scanner
   */
  @Reference
  protected void addWorkflowDefinitionScanner(WorkflowDefinitionScanner scanner) {
    workflowDefinitionScanner = scanner;
  }

  /**
   * Callback to set the Admin UI index.
   *
   * @param index
   *          the admin UI index.
   */
  @Reference
  public void setIndex(ElasticsearchIndex index) {
    this.index = index;
  }

  /**
   * Callback to set the workflow database
   *
   * @param persistence
   *          the workflow database
   */
  @Reference(name = "workflow-persistence")
  public void setPersistence(WorkflowServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#getJobType()
   */
  @Override
  public String getJobType() {
    return JOB_TYPE;
  }

  /**
   * A tuple of a workflow operation handler and the name of the operation it handles
   */
  public static class HandlerRegistration {

    protected WorkflowOperationHandler handler;
    protected String operationName;

    public HandlerRegistration(String operationName, WorkflowOperationHandler handler) {
      if (operationName == null)
        throw new IllegalArgumentException("Operation name cannot be null");
      if (handler == null)
        throw new IllegalArgumentException("Handler cannot be null");
      this.operationName = operationName;
      this.handler = handler;
    }

    public WorkflowOperationHandler getHandler() {
      return handler;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + handler.hashCode();
      result = prime * result + operationName.hashCode();
      return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      HandlerRegistration other = (HandlerRegistration) obj;
      if (!handler.equals(other.handler))
        return false;
      return operationName.equals(other.operationName);
    }
  }

  /**
   * A utility class to run jobs
   */
  class JobRunner implements Callable<Void> {

    /** The job */
    private Job job = null;

    /** The current job */
    private final Job currentJob;

    /**
     * Constructs a new job runner
     *
     * @param job
     *          the job to run
     * @param currentJob
     *          the current running job
     */
    JobRunner(Job job, Job currentJob) {
      this.job = job;
      this.currentJob = currentJob;
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
  }

  @Override
  public synchronized void cleanupWorkflowInstances(int buffer, WorkflowState state) throws UnauthorizedException,
          WorkflowDatabaseException {
    logger.info("Start cleaning up workflow instances older than {} days with status '{}'", buffer, state);

    int instancesCleaned = 0;
    int cleaningFailed = 0;

    Date priorTo = DateUtils.addDays(new Date(), -buffer);

    try {
      for (WorkflowInstance workflowInstance : persistence.getWorkflowInstancesForCleanup(state, priorTo)) {
        try {
          remove(workflowInstance.getId());
          instancesCleaned++;
        } catch (WorkflowDatabaseException | UnauthorizedException e) {
          throw e;
        } catch (NotFoundException e) {
          // Since we are in a cleanup operation, we don't have to care about NotFoundExceptions
          logger.debug("Workflow instance '{}' could not be removed", workflowInstance.getId(), e);
        } catch (WorkflowParsingException | WorkflowStateException e) {
          logger.warn("Workflow instance '{}' could not be removed", workflowInstance.getId(), e);
          cleaningFailed++;
        }
      }
    } catch (WorkflowDatabaseException e) {
      throw new WorkflowDatabaseException(e);
    }

    if (instancesCleaned == 0 && cleaningFailed == 0) {
      logger.info("No workflow instances found to clean up");
      return;
    }

    if (instancesCleaned > 0)
      logger.info("Cleaned up '%d' workflow instances", instancesCleaned);
    if (cleaningFailed > 0) {
      logger.warn("Cleaning failed for '%d' workflow instances", cleaningFailed);
      throw new WorkflowDatabaseException("Unable to clean all workflow instances, see logs!");
    }
  }

  @Override
  public Map<String, Map<String, String>> getWorkflowStateMappings() {
    return workflowDefinitionScanner.workflowStateMappings.entrySet().stream().collect(Collectors.toMap(
            Entry::getKey, e -> e.getValue().stream()
                    .collect(Collectors.toMap(m -> m.getState().name(), WorkflowStateMapping::getValue))
    ));
  }

  @Override
  public void repopulate() throws IndexRebuildException {
    try {
      final int total;
      try {
        total = persistence.countMediaPackages();
      } catch (WorkflowDatabaseException e) {
        logIndexRebuildError(logger.getSlf4jLogger(), index.getIndexName(), e);
        throw new IndexRebuildException(index.getIndexName(), getService(), e);
      }

      if (total > 0) {
        logIndexRebuildBegin(logger.getSlf4jLogger(), index.getIndexName(), total, "workflows");
        int current = 0;
        int n = 20;
        List<WorkflowIndexData> workflowIndexData;

        int limit = 1000;
        int offset = 0;
        String currentMediapackageId;
        String lastMediapackageId = "";
        do {
          try {
            workflowIndexData = persistence.getWorkflowIndexData(limit, offset);
          } catch (WorkflowDatabaseException e) {
            logIndexRebuildError(logger.getSlf4jLogger(), index.getIndexName(), e);
            throw new IndexRebuildException(index.getIndexName(), getService(), e);
          }
          if (workflowIndexData.size() > 0) {
            offset += limit;
            logger.debug("Got {} workflows for re-indexing", workflowIndexData.size());
            var updatedWorkflowRange = new ArrayList<Event>();

            for (WorkflowIndexData indexData : workflowIndexData) {
              currentMediapackageId = indexData.getMediaPackageId();
              if (currentMediapackageId.equals(lastMediapackageId)) {
                continue;
              }
              current++;

              var updatedWorkflowData = index.getEvent(indexData.getMediaPackageId(), indexData.getOrganizationId(),
                        securityService.getUser());
              updatedWorkflowData = getStateUpdateFunction(indexData).apply(updatedWorkflowData);
              updatedWorkflowRange.add(updatedWorkflowData.get());

              if (updatedWorkflowRange.size() >= n || current >= total) {
                index.bulkEventUpdate(updatedWorkflowRange);
                logIndexRebuildProgress(logger.getSlf4jLogger(), index.getIndexName(), total, current, n);
                updatedWorkflowRange.clear();
              }

              lastMediapackageId = currentMediapackageId;
            }
          }
        } while (workflowIndexData.size() > 0);
      }
    } catch (Exception e) {
      logIndexRebuildError(logger.getSlf4jLogger(), index.getIndexName(), e);
      throw new IndexRebuildException(index.getIndexName(), getService(), e);
    }
  }

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Workflow;
  }

  /**
   * Remove a workflow instance from the Elasticsearch index.
   *
   * @param workflowInstanceId
   *         the identifier of the workflow instance to remove
   * @param index
   *         the index to update
   */
  private void removeWorkflowInstanceFromIndex(long workflowInstanceId) {
    final String orgId = securityService.getOrganization().getId();
    final User user = securityService.getUser();

    // find events
    SearchResult<Event> results;
    try {
      results = index.getByQuery(new EventSearchQuery(orgId, user).withWorkflowId(workflowInstanceId));
    } catch (SearchIndexException e) {
      logger.error("Error retrieving the events for workflow instance {} from the {} index.", workflowInstanceId,
              index.getIndexName(), e);
      return;
    }

    if (results.getItems().length == 0) {
      logger.warn("No events for workflow instance {} found in the {} index.", workflowInstanceId,
              index.getIndexName());
      return;
    }

    // should be only one event, but better safe than sorry
    for (SearchResultItem<Event> item: results.getItems()) {
      String eventId = item.getSource().getIdentifier();
      logger.debug("Removing workflow instance {} of event {} from the {} index.", workflowInstanceId, eventId,
              index.getIndexName());

      Function<Optional<Event>, Optional<Event>> updateFunction = (Optional<Event> eventOpt) -> {
        if (!eventOpt.isPresent()) {
          logger.warn("Event {} of workflow instance {} not found in the {} index.", workflowInstanceId,
                  eventId, index.getIndexName());
          return Optional.empty();
        }
        Event event = eventOpt.get();
        if (event.getWorkflowId() != null && event.getWorkflowId().equals(workflowInstanceId)) {
          logger.debug("Workflow {} is the current workflow of event {}. Removing it from event.", eventId,
                  workflowInstanceId);
          event.setWorkflowId(null);
          event.setWorkflowDefinitionId(null);
          event.setWorkflowState(null);
          return Optional.of(event);
        }
        return Optional.empty();
      };

      try {
        index.addOrUpdateEvent(eventId, updateFunction, orgId, user);
        logger.debug("Workflow instance {} of event {} removed from the {} index.", workflowInstanceId, eventId,
                index.getIndexName());
      } catch (SearchIndexException e) {
        logger.error("Error removing the workflow instance {} of event {} from the {} index.", workflowInstanceId,
                eventId, index.getIndexName(), e);
      }
    }
  }

  /**
   * Update a workflow instance in the Elasticsearch index.
   *
   * @param id
   *         workflow id
   * @param state
   *         workflow state as int
   * @param mpId
   *         corresponding mediapackage id
   * @param orgId
   *         workflow organization id
   */
  private void updateWorkflowInstanceInIndex(long id, int state, String mpId, String orgId) {
    final WorkflowState workflowState = WorkflowState.values()[state];
    final User user = securityService.getUser();

    logger.debug("Updating workflow instance {} of event {} in the {} index.", id, mpId,
            index.getIndexName());
    Function<Optional<Event>, Optional<Event>> updateFunction = (Optional<Event> eventOpt) -> {
      Event event = eventOpt.orElse(new Event(mpId, orgId));
      event.setWorkflowId(id);
      event.setWorkflowState(workflowState);
      return Optional.of(event);
    };

    try {
      index.addOrUpdateEvent(mpId, updateFunction, orgId, user);
      logger.debug("Workflow instance {} of event {} updated in the {} index.", id, mpId,
              index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error updating the workflow instance {} of event {} in the {} index.", id, mpId,
              index.getIndexName(), e);
    }
  }

  /**
   * Get the function to update the workflow state for an event in the Elasticsearch index.
   *
   * @param wfData
   *          The workflow index data package
   * @return the function to do the update
   */
  private Function<Optional<Event>, Optional<Event>> getStateUpdateFunction(WorkflowIndexData wfData) {
    return (Optional<Event> eventOpt) -> {
      Event event = eventOpt.orElse(new Event(wfData.getMediaPackageId(), wfData.getOrganizationId()));
      event.setWorkflowId(wfData.getId());
      event.setWorkflowState(WorkflowState.values()[wfData.getState()]);
      return Optional.of(event);
    };
  }
}
