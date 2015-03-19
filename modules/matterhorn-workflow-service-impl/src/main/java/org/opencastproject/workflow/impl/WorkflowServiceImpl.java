/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.impl;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.INSTANTIATED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.PAUSED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.RUNNING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.STOPPED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.SUCCEEDED;

import org.opencastproject.index.IndexProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.message.broker.api.workflow.WorkflowItem;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.UndispatchableJobException;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.Log;
import org.opencastproject.util.MultiResourceLock;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowOperationResultImpl;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStateException;
import org.opencastproject.workflow.api.WorkflowStatistics;
import org.opencastproject.workflow.impl.jmx.WorkflowsStatistics;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.FnX;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectInstance;

/**
 * Implements WorkflowService with in-memory data structures to hold WorkflowOperations and WorkflowInstances.
 * WorkflowOperationHandlers are looked up in the OSGi service registry based on the "workflow.operation" property. If
 * the WorkflowOperationHandler's "workflow.operation" service registration property matches
 * WorkflowOperation.getName(), then the factory returns a WorkflowOperationRunner to handle that operation. This allows
 * for custom runners to be added or modified without affecting the workflow service itself.
 */
public class WorkflowServiceImpl extends AbstractIndexProducer implements WorkflowService, JobProducer, ManagedService {

  /** The code to use for the incident service to report a failed capture. */
  private static final int FAILED_CAPTURE_CODE = 1;
  /** The code to use for the incident service to report a failed ingest. */
  private static final int FAILED_INGEST_CODE = 2;

  /** The number of milliseconds in a second. */
  public static final long MILLISECONDS_IN_SECONDS = 1000L;

  /** The unique service template that identifies the capture operation. */
  public static final String CAPTURE_OPERATION_TEMPLATE = "capture";
  /** The unique service template that identifies the ingest operation. */

  public static final String INGEST_OPERATION_TEMPLATE = "ingest";

  /** The unique service template that identifies the schedule operation. */
  public static final String SCHEDULE_OPERATION_TEMPLATE = "schedule";

  /** Retry strategy property name */
  private static final String RETRY_STRATEGY = "retryStrategy";

  /** Logging facility */
  private static final Log logger = new Log(LoggerFactory.getLogger(WorkflowServiceImpl.class));

  /** List of available operations on jobs */
  enum Operation {
    START_WORKFLOW, RESUME, START_OPERATION
  }

  /** The pattern used by workfow operation configuration keys * */
  public static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{.+?\\}");

  /** The set of yes values */
  public static final Set<String> YES;

  /** The set of 'no' values */
  public static final Set<String> NO;

  /** The configuration key for setting {@link #maxConcurrentWorkflows} */
  public static final String MAX_CONCURRENT_CONFIG_KEY = "max.concurrent";

  /** Configuration value for the maximum number of parallel workflows based on the number of cores in the cluster */
  public static final String OPT_NUM_CORES = "cores";

  /** Constant value indicating a <code>null</code> parent id */
  private static final String NULL_PARENT_ID = "-";

  /** Workflow statistics JMX type */
  private static final String JMX_WORKFLOWS_STATISTICS_TYPE = "WorkflowsStatistics";

  /** The list of registered JMX beans */
  private final List<ObjectInstance> jmxBeans = new ArrayList<ObjectInstance>();

  /** The JMX business object for workflows statistics */
  private WorkflowsStatistics workflowsStatistics;
  /** Error resolution handler id constant */
  public static final String ERROR_RESOLUTION_HANDLER_ID = "error-resolution";

  /** Remove references to the component context once felix scr 1.2 becomes available */
  protected ComponentContext componentContext = null;

  /** The maximum number of cluster-wide workflows that will cause this service to stop accepting new jobs */
  protected int maxConcurrentWorkflows = -1;

  /** The collection of workflow definitions */
  // protected Map<String, WorkflowDefinition> workflowDefinitions = new HashMap<String, WorkflowDefinition>();

  /** The metadata services */
  private SortedSet<MediaPackageMetadataService> metadataServices;

  /** The data access object responsible for storing and retrieving workflow instances */
  protected WorkflowServiceIndex index;

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

  /** The message broker receiver service */
  protected MessageReceiver messageReceiver;

  /** The message broker sender service */
  protected MessageSender messageSender;

  /** The workflow definition scanner */
  private WorkflowDefinitionScanner workflowDefinitionScanner;

  /** List of initially delayed workflows */
  private final List<Long> delayedWorkflows = new ArrayList<Long>();

  /** Concurrent maps for lock objects */
  private final MultiResourceLock lock = new MultiResourceLock();
  private final MultiResourceLock updateLock = new MultiResourceLock();

  static {
    YES = new HashSet<String>(Arrays.asList(new String[] { "yes", "true", "on" }));
    NO = new HashSet<String>(Arrays.asList(new String[] { "no", "false", "off" }));
  }

  /**
   * Constructs a new workflow service impl, with a priority-sorted map of metadata services
   */
  public WorkflowServiceImpl() {
    metadataServices = new TreeSet<MediaPackageMetadataService>(new Comparator<MediaPackageMetadataService>() {
      @Override
      public int compare(MediaPackageMetadataService o1, MediaPackageMetadataService o2) {
        return o1.getPriority() - o2.getPriority();
      }
    });
  }

  /**
   * Activate this service implementation via the OSGI service component runtime.
   *
   * @param componentContext
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    try {
      logger.info("Generating JMX workflow statistics");
      workflowsStatistics = new WorkflowsStatistics(getBeanStatistics(), getHoldWorkflows());
      jmxBeans.add(JmxUtil.registerMXBean(workflowsStatistics, JMX_WORKFLOWS_STATISTICS_TYPE));
    } catch (WorkflowDatabaseException e) {
      logger.error("Error registarting JMX statistic beans", e);
    }
    super.activate();
    logger.info("Activate Workflow service");
  }

  @Override
  public void deactivate() {
    for (ObjectInstance mxbean : jmxBeans) {
      JmxUtil.unregisterMXBean(mxbean);
    }
    super.deactivate();
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
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            try {
              securityService.setUser(currentUser);
              securityService.setOrganization(currentOrganization);
              listener.stateChanged(newWorkflowInstance);
            } finally {
              securityService.setUser(null);
              securityService.setOrganization(null);
            }
          }
        };
        executorService.execute(runnable);
      } else {
        logger.debug("Not notifying %s because the workflow state has not changed", listener);
      }

      if (newWorkflowInstance.getCurrentOperation() != null) {
        if (oldWorkflowInstance == null || oldWorkflowInstance.getCurrentOperation() == null
                || !oldWorkflowInstance.getCurrentOperation().equals(newWorkflowInstance.getCurrentOperation())) {
          Runnable runnable = new Runnable() {
            @Override
            public void run() {
              try {
                securityService.setUser(currentUser);
                securityService.setOrganization(currentOrganization);
                listener.operationChanged(newWorkflowInstance);
              } finally {
                securityService.setUser(null);
                securityService.setOrganization(null);
              }
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
    List<WorkflowDefinition> list = new ArrayList<WorkflowDefinition>();
    for (Entry<String, WorkflowDefinition> entry : workflowDefinitionScanner.getWorkflowDefinitions().entrySet()) {
      list.add(entry.getValue());
    }
    Collections.sort(list, new Comparator<WorkflowDefinition>() {
      @Override
      public int compare(WorkflowDefinition o1, WorkflowDefinition o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });
    return list;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isRunnable(WorkflowDefinition workflowDefinition) {
    List<String> availableOperations = listAvailableOperationNames();
    List<WorkflowDefinition> checkedWorkflows = new ArrayList<WorkflowDefinition>();
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
        } catch (WorkflowDatabaseException e) {
          logger.info("%s is not runnable because we can not load the catch workflow %s on operation %s",
                  workflowDefinition, catchWorkflow, op);
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
    Set<HandlerRegistration> set = new HashSet<HandlerRegistration>();
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
    List<String> list = new ArrayList<String>();
    for (HandlerRegistration reg : getRegisteredHandlers()) {
      list.add(reg.operationName);
    }
    return list;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#registerWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  @Override
  public void registerWorkflowDefinition(WorkflowDefinition workflow) {
    if (workflow == null || workflow.getId() == null) {
      throw new IllegalArgumentException("Workflow must not be null, and must contain an ID");
    }
    String id = workflow.getId();
    if (workflowDefinitionScanner.getWorkflowDefinitions().containsKey(id)) {
      throw new IllegalStateException("A workflow definition with ID '" + id + "' is already registered.");
    }
    workflowDefinitionScanner.putWorkflowDefinition(id, workflow);
    messageSender.sendObjectMessage(WorkflowItem.WORKFLOW_QUEUE, MessageSender.DestinationType.Queue,
            WorkflowItem.addDefinition(workflow));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#unregisterWorkflowDefinition(java.lang.String)
   */
  @Override
  public void unregisterWorkflowDefinition(String workflowDefinitionId) throws NotFoundException,
          WorkflowDatabaseException {
    boolean deleted = workflowDefinitionScanner.removeWorkflowDefinition(workflowDefinitionId) != null;
    messageSender.sendObjectMessage(WorkflowItem.WORKFLOW_QUEUE, MessageSender.DestinationType.Queue,
            WorkflowItem.deleteDefinition(workflowDefinitionId));
    if (deleted)
      throw new NotFoundException("Workflow definition not found");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowById(long)
   */
  @Override
  public WorkflowInstanceImpl getWorkflowById(long id) throws WorkflowDatabaseException, NotFoundException,
          UnauthorizedException {
    try {
      Job job = serviceRegistry.getJob(id);
      if (Status.DELETED.equals(job.getStatus())) {
        throw new NotFoundException("Workflow '" + id + "' has been deleted");
      }
      if (JOB_TYPE.equals(job.getJobType()) && Operation.START_WORKFLOW.toString().equals(job.getOperation())) {
        WorkflowInstanceImpl workflow = WorkflowParser.parseWorkflowInstance(job.getPayload());
        assertPermission(workflow, Permissions.Action.READ.toString());
        return workflow;
      } else {
        throw new NotFoundException("'" + id + "' is a job identifier, but it is not a workflow identifier");
      }
    } catch (WorkflowParsingException e) {
      throw new IllegalStateException("The workflow job payload is malformed");
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Error loading workflow job from the service registry");
    } catch (MediaPackageException e) {
      throw new IllegalStateException("Unable to read mediapackage from workflow " + id, e);
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
          throws WorkflowDatabaseException, WorkflowParsingException {
    return start(workflowDefinition, mediaPackage, new HashMap<String, String>());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) throws WorkflowDatabaseException, WorkflowParsingException {
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
          Long parentWorkflowId, Map<String, String> properties) throws WorkflowDatabaseException,
          WorkflowParsingException, NotFoundException {

    try {
      logger.startUnitOfWork();
      if (workflowDefinition == null)
        throw new IllegalArgumentException("workflow definition must not be null");
      if (sourceMediaPackage == null)
        throw new IllegalArgumentException("mediapackage must not be null");
      for (List<String> errors : MediaPackageSupport.sanityCheck(sourceMediaPackage)) {
        throw new IllegalArgumentException("Insane media package cannot be processed: " + mkString(errors, "; "));
      }
      if (parentWorkflowId != null) {
        try {
          getWorkflowById(parentWorkflowId); // Let NotFoundException bubble up
        } catch (UnauthorizedException e) {
          throw new IllegalArgumentException("Parent workflow " + parentWorkflowId + " not visible to this user");
        }
      }

      // Get the current user
      User currentUser = securityService.getUser();
      if (currentUser == null)
        throw new SecurityException("Current user is unknown");

      // Get the current organization
      Organization organization = securityService.getOrganization();
      if (organization == null)
        throw new SecurityException("Current organization is unknown");

      WorkflowInstance workflowInstance = new WorkflowInstanceImpl(workflowDefinition, sourceMediaPackage,
              parentWorkflowId, currentUser, organization, properties);
      workflowInstance = updateConfiguration(workflowInstance, properties);

      // Create and configure the workflow instance
      try {
        // Create a new job for this workflow instance
        String workflowDefinitionXml = WorkflowParser.toXml(workflowDefinition);
        String workflowInstanceXml = WorkflowParser.toXml(workflowInstance);
        String mediaPackageXml = MediaPackageParser.getAsXml(sourceMediaPackage);

        List<String> arguments = new ArrayList<String>();
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
                workflowInstanceXml, false, null);

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
        throw new WorkflowDatabaseException(t);
      }
    } finally {
      logger.endUnitOfWork();
    }
  }

  protected WorkflowInstance updateConfiguration(WorkflowInstance instance, Map<String, String> properties) {
    try {
      if (properties != null) {
        for (Entry<String, String> entry : properties.entrySet()) {
          instance.setConfiguration(entry.getKey(), entry.getValue());
        }
      }

      Map<String, String> wfProperties = new HashMap<String, String>();
      for (String key : instance.getConfigurationKeys()) {
        wfProperties.put(key, instance.getConfiguration(key));
      }
      String xml = replaceVariables(WorkflowParser.toXml(instance), wfProperties);
      WorkflowInstanceImpl workflow = WorkflowParser.parseWorkflowInstance(xml);
      return workflow;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to replace workflow instance variables", e);
    }
  }

  /**
   * Replaces all occurrences of <code>${.*+}</code> with the property in the provided map, or if not available in the
   * map, from the bundle context properties, if available.
   *
   * @param source
   *          The source string
   * @param properties
   *          The map of properties to replace
   * @return The resulting string
   */
  protected String replaceVariables(String source, Map<String, String> properties) {
    Matcher matcher = PROPERTY_PATTERN.matcher(source);
    StringBuilder result = new StringBuilder();
    int cursor = 0;
    boolean matchFound = matcher.find();
    if (!matchFound)
      return source;
    while (matchFound) {
      int matchStart = matcher.start();
      int matchEnd = matcher.end();
      result.append(source.substring(cursor, matchStart)); // add the content before the match
      String key = source.substring(matchStart + 2, matchEnd - 1);
      String systemProperty = componentContext == null ? null : componentContext.getBundleContext().getProperty(key);
      String providedProperty = null;
      if (properties != null) {
        providedProperty = properties.get(key);
      }
      if (isNotBlank(providedProperty)) {
        result.append(providedProperty);
      } else if (isNotBlank(systemProperty)) {
        result.append(systemProperty);
      } else {
        result.append(source.substring(matchStart, matchEnd)); // retain the original matched value
      }
      cursor = matchEnd;
      matchFound = matcher.find();
      if (!matchFound)
        result.append(source.substring(matchEnd, source.length()));
    }
    return result.toString();
  }

  /**
   * Does a lookup of available operation handlers for the given workflow operation.
   *
   * @param operation
   *          the operation definition
   * @return the handler or <code>null</code>
   */
  protected WorkflowOperationHandler selectOperationHandler(WorkflowOperationInstance operation) {
    List<WorkflowOperationHandler> handlerList = new ArrayList<WorkflowOperationHandler>();
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
   */
  protected Job runWorkflow(WorkflowInstance workflow) throws WorkflowException, UnauthorizedException {
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

    if (operation == null)
      throw new IllegalStateException("Cannot start a workflow without a current operation");

    if (operation.getPosition() != 0)
      throw new IllegalStateException("Current operation expected to be first");

    try {
      logger.info("Scheduling workflow %s for execution", workflow.getId());
      Job job = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
              Arrays.asList(Long.toString(workflow.getId())), null, false, null);
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
      update(workflow);

    } else {

      // Somebody might have set the workflow to "paused" from the outside, so take a look a the database first
      WorkflowState dbWorkflowState = null;
      try {
        dbWorkflowState = getWorkflowById(workflow.getId()).getState();
      } catch (WorkflowDatabaseException e) {
        throw new IllegalStateException("The workflow with ID " + workflow.getId()
                + " can not be accessed in the database", e);
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

      Job job = null;
      switch (workflow.getState()) {
        case FAILED:
          update(workflow);
          break;
        case FAILING:
        case RUNNING:
          try {
            job = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
                    Arrays.asList(Long.toString(workflow.getId())), null, false, null);
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
          throw new IllegalStateException("Unkown workflow state found during processing");
      }

    }

    return processingOperation;
  }

  @Override
  public WorkflowDefinition getWorkflowDefinitionById(String id) throws NotFoundException, WorkflowDatabaseException {
    WorkflowDefinition def = workflowDefinitionScanner.getWorkflowDefinition(id);
    if (def == null)
      throw new NotFoundException("Workflow definition '" + id + "' not found");
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
    return lock.synchronize(workflowInstanceId, new FnX<Long, WorkflowInstance>() {
      @Override
      public WorkflowInstance apx(Long workflowInstanceId) throws Exception {
        WorkflowInstanceImpl instance = getWorkflowById(workflowInstanceId);
        instance.setState(STOPPED);

        // Update the workflow instance
        update(instance);

        removeTempFiles(instance);

        return instance;
      }
    });
  }

  private void removeTempFiles(WorkflowInstance workflowInstance) throws WorkflowDatabaseException,
          UnauthorizedException, NotFoundException {
    logger.info("Removing temporary files for workflow %s", workflowInstance);
    for (MediaPackageElement elem : workflowInstance.getMediaPackage().getElements()) {
      try {
        logger.debug("Removing temporary file %s for workflow %s", elem.getURI(), workflowInstance);
        workspace.delete(elem.getURI());
      } catch (IOException e) {
        logger.warn("Unable to delete mediapackage element ", e);
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
    lock.synchronize(workflowInstanceId, new FnX<Long, Void>() {
      @Override
      public Void apx(Long workflowInstanceId) throws Exception {
        WorkflowQuery query = new WorkflowQuery();
        query.withId(Long.toString(workflowInstanceId));
        WorkflowSet workflows = index.getWorkflowInstances(query, Permissions.Action.READ.toString(), false);
        if (workflows.size() == 1) {
          WorkflowInstance instance = workflows.getItems()[0];

          WorkflowInstance.WorkflowState state = instance.getState();
          if (state != WorkflowState.SUCCEEDED && state != WorkflowState.FAILED && state != WorkflowState.STOPPED)
            throw new WorkflowStateException("Workflow instance with state '" + state
                                                     + "' cannot be removed. Only states SUCCEEDED, FAILED & STOPPED are allowed");

          try {
            assertPermission(instance, Permissions.Action.WRITE.toString());
          } catch (MediaPackageException e) {
            throw new WorkflowParsingException(e);
          }

          // First, remove temporary files DO THIS BEFORE REMOVING FROM INDEX
          try {
            removeTempFiles(instance);
          } catch (NotFoundException e) {
            // If the files aren't their anymore, we don't have to cleanup up them :-)
            logger.debug("Temporary files of workflow instance %d seem to be gone already...", workflowInstanceId);
          }

          // Second, remove jobs related to a operation which belongs to the workflow instance
          List<WorkflowOperationInstance> operations = instance.getOperations();
          for (WorkflowOperationInstance op : operations) {
            if (op.getId() != null) {
              long workflowOpId = op.getId();
              if (workflowOpId != workflowInstanceId) {
                try {
                  serviceRegistry.removeJob(workflowOpId);
                } catch (ServiceRegistryException e) {
                  logger.warn("Problems while removing jobs related to workflow operation '%d': %s", workflowOpId,
                              e.getMessage());
                } catch (NotFoundException e) {
                  logger.debug("No jobs related to the workflow operation '%d' found in the service registry",
                               workflowOpId);
                }
              }
            }
          }

          // Third, remove workflow instance job itself
          try {
            serviceRegistry.removeJob(workflowInstanceId);
            messageSender.sendObjectMessage(WorkflowItem.WORKFLOW_QUEUE, MessageSender.DestinationType.Queue,
                                            WorkflowItem.deleteInstance(workflowInstanceId, instance));
          } catch (ServiceRegistryException e) {
            logger.warn("Problems while removing workflow instance job '%d': %s", workflowInstanceId,
                        ExceptionUtils.getStackTrace(e));
          } catch (NotFoundException e) {
            logger.info("No workflow instance job '%d' found in the service registry", workflowInstanceId);
          }

          // At last, remove workflow instance from the index
          try {
            index.remove(workflowInstanceId);
          } catch (NotFoundException e) {
            // This should never happen, because we got workflow instance by querying the index...
            logger.warn("Workflow instance could not be removed from index: %s", ExceptionUtils.getStackTrace(e));
          }
        } else if (workflows.size() == 0) {
          throw new NotFoundException("Workflow instance with id '" + Long.toString(workflowInstanceId)
                                              + "' could not be found");
        } else {
          throw new WorkflowDatabaseException("More than one workflow found with id: "
                                                      + Long.toString(workflowInstanceId));
        }
        return null;
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(long)
   */
  @Override
  public WorkflowInstance suspend(long workflowInstanceId) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    return lock.synchronize(workflowInstanceId, new FnX<Long, WorkflowInstance>() {
      @Override
      public WorkflowInstance apx(Long workflowInstanceId) throws Exception {
        WorkflowInstanceImpl instance = getWorkflowById(workflowInstanceId);
        instance.setState(PAUSED);
        update(instance);
        return instance;
      }
    });
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
          if (op.isFailWorkflowOnException()) {
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
                Arrays.asList(Long.toString(workflowInstanceId)), null, false, null);

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
      workflowJob.setPayload(WorkflowParser.toXml(workflowInstance));
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
        newArguments.add(new String(out.toByteArray(), "UTF-8"));
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
   * @throws MediaPackageException
   *           if there is an error accessing the workflow's security policy in its mediapackage
   */
  protected void assertPermission(WorkflowInstance workflow, String action) throws UnauthorizedException,
          MediaPackageException {
    User currentUser = securityService.getUser();
    Organization currentOrg = securityService.getOrganization();
    String currentOrgAdminRole = currentOrg.getAdminRole();
    String currentOrgId = currentOrg.getId();

    MediaPackage mediapackage = workflow.getMediaPackage();
    User workflowCreator = workflow.getCreator();
    String workflowOrgId = workflowCreator.getOrganization().getId();

    boolean authorized = currentUser.hasRole(GLOBAL_ADMIN_ROLE)
            || (currentUser.hasRole(currentOrgAdminRole) && currentOrgId.equals(workflowOrgId))
            || currentUser.equals(workflowCreator)
            || (authorizationService.hasPermission(mediapackage, action) && currentOrgId.equals(workflowOrgId));

    if (!authorized) {
      throw new UnauthorizedException(currentUser, action);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(final WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException {
    updateLock.synchronize(workflowInstance.getId(), new FnX<Long, Void>() {
      @Override
      public Void apx(Long a) throws Exception {
        WorkflowInstance originalWorkflowInstance = null;
        try {
          originalWorkflowInstance = getWorkflowById(workflowInstance.getId());
        } catch (NotFoundException e) {
          // That's fine, it's a new workflow instance
        }

        if (originalWorkflowInstance != null) {
          try {
            assertPermission(originalWorkflowInstance, Permissions.Action.WRITE.toString());
          } catch (MediaPackageException e) {
            throw new WorkflowParsingException(e);
          }
        }

        // Before we persist this, extract the metadata
        final MediaPackage updatedMediaPackage = workflowInstance.getMediaPackage();
        populateMediaPackageMetadata(updatedMediaPackage);
        String seriesId = updatedMediaPackage.getSeries();
        if (seriesId != null && workflowInstance.getCurrentOperation() != null) {
          // If the mediapackage contains a series, find the series ACLs and add the security information to the
          // mediapackage
          try {
            AccessControlList acl = seriesService.getSeriesAccessControl(seriesId);
            Option<AccessControlList> activeSeriesAcl = authorizationService.getAcl(updatedMediaPackage,
                    AclScope.Series);
            if (activeSeriesAcl.isNone() || !AccessControlUtil.equals(activeSeriesAcl.get(), acl))
              authorizationService.setAcl(updatedMediaPackage, AclScope.Series, acl);
          } catch (SeriesException e) {
            throw new WorkflowDatabaseException(e);
          } catch (NotFoundException e) {
            logger.warn("Series %s not found, unable to set ACLs", seriesId);
          } catch (Exception e) {
            logger.error("Error reading ACL from series {}: {}", seriesId, e);
          }
        }

        // Synchronize the job status with the workflow
        WorkflowState workflowState = workflowInstance.getState();
        String xml;
        try {
          xml = WorkflowParser.toXml(workflowInstance);
        } catch (Exception e) {
          // Can't happen, since we are converting from an in-memory object
          throw new IllegalStateException("In-memory workflow instance could not be serialized", e);
        }

        Job job = null;
        try {
          job = serviceRegistry.getJob(workflowInstance.getId());
          job.setPayload(xml);

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
              job.setStatus(Status.DELETED);
              break;
            case SUCCEEDED:
              job.setStatus(Status.FINISHED);
              break;
            default:
              throw new IllegalStateException("Found a workflow state that is not handled");
          }
        } catch (ServiceRegistryException e) {
          logger.error(e, "Unable to read workflow job %s from service registry", workflowInstance.getId());
          throw new WorkflowDatabaseException(e);
        } catch (NotFoundException e) {
          logger.error("Job for workflow %s not found in service registry", workflowInstance.getId());
          throw new WorkflowDatabaseException(e);
        }

        // Update both workflow and workflow job
        try {
          serviceRegistry.updateJob(job);
          messageSender.sendObjectMessage(WorkflowItem.WORKFLOW_QUEUE, MessageSender.DestinationType.Queue,
                  WorkflowItem.updateInstance(workflowInstance));
          index(workflowInstance);
        } catch (ServiceRegistryException e) {
          logger.error(
                  "Update of workflow job %s in the service registry failed, service registry and workflow index may be out of sync",
                  workflowInstance.getId());
          throw new WorkflowDatabaseException(e);
        } catch (NotFoundException e) {
          logger.error("Job for workflow %s not found in service registry", workflowInstance.getId());
          throw new WorkflowDatabaseException(e);
        } catch (Exception e) {
          logger.error(
                  "Update of workflow job %s in the service registry failed, service registry and workflow index may be out of sync",
                  job.getId());
          throw new WorkflowException(e);
        }

        workflowsStatistics.updateWorkflow(getBeanStatistics(), getHoldWorkflows());

        try {
          WorkflowInstance clone = WorkflowParser.parseWorkflowInstance(WorkflowParser.toXml(workflowInstance));
          fireListeners(originalWorkflowInstance, clone);
        } catch (Exception e) {
          // Can't happen, since we are converting from an in-memory object
          throw new IllegalStateException("In-memory workflow instance could not be serialized", e);
        }
        return null;
      }
    });
  }

  /**
   * Updates the search index entries for this workflow instance.
   *
   * @param workflowInstance
   *          the workflow
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance
   */
  protected void index(final WorkflowInstance workflowInstance) throws WorkflowDatabaseException {
    // Update the search index
    index.update(workflowInstance);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    return index.countWorkflowInstances(null, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState,
   *      java.lang.String)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
    return index.countWorkflowInstances(state, operation);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getStatistics()
   */
  @Override
  public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {
    return index.getStatistics();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
    return index.getWorkflowInstances(query, Permissions.Action.WRITE.toString(), true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstancesForAdministrativeRead(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstancesForAdministrativeRead(WorkflowQuery query) throws WorkflowDatabaseException,
          UnauthorizedException {
    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole()))
      throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");

    return index.getWorkflowInstances(query, Permissions.Action.WRITE.toString(), false);
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
   * @throws WorkflowParsingException
   */
  protected WorkflowInstance handleOperationException(WorkflowInstance workflow, WorkflowOperationInstance operation)
          throws WorkflowDatabaseException, WorkflowParsingException, UnauthorizedException {
    WorkflowOperationInstanceImpl currentOperation = (WorkflowOperationInstanceImpl) operation;
    int failedAttempt = currentOperation.getFailedAttempts() + 1;
    currentOperation.setFailedAttempts(failedAttempt);
    currentOperation.addToExecutionHistory(currentOperation.getId());

    if (currentOperation.getMaxAttempts() != -1 && failedAttempt == currentOperation.getMaxAttempts()) {
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
                  ERROR_RESOLUTION_HANDLER_ID, "Error Resolution Operation", "error", true);
          WorkflowOperationInstanceImpl errorResolutionInstance = new WorkflowOperationInstanceImpl(
                  errorResolutionDefinition, currentOperation.getPosition());
          errorResolutionInstance.setExceptionHandlingWorkflow(currentOperation.getExceptionHandlingWorkflow());
          operations.add(currentOperation.getPosition(), errorResolutionInstance);
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
   * @throws WorkflowDatabaseException
   *           If the exception handler workflow is not found
   */
  private void handleFailedOperation(WorkflowInstance workflow, WorkflowOperationInstance currentOperation)
          throws WorkflowDatabaseException {
    String errorDefId = currentOperation.getExceptionHandlingWorkflow();

    // Adjust the workflow state according to the setting on the operation
    if (currentOperation.isFailWorkflowOnException()) {
      if (StringUtils.isBlank(errorDefId)) {
        workflow.setState(FAILED);
      } else {
        workflow.setState(FAILING);

        // Remove the rest of the original workflow
        int currentOperationPosition = workflow.getOperations().indexOf(currentOperation);
        List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>();
        operations.addAll(workflow.getOperations().subList(0, currentOperationPosition + 1));
        workflow.setOperations(operations);

        // Determine the current workflow configuration
        Map<String, String> configuration = new HashMap<String, String>();
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
    WorkflowOperationInstanceImpl currentOperation = (WorkflowOperationInstanceImpl) workflow.getCurrentOperation();
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

    // Update the workflow configuration. Update the reference to the current operation as well, since the workflow has
    // been serialized and deserialized in the meantime.
    int currentOperationPosition = currentOperation.getPosition();
    workflow = updateConfiguration(workflow, result.getProperties());
    currentOperation = (WorkflowOperationInstanceImpl) workflow.getOperations().get(currentOperationPosition);

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

        ResumableWorkflowOperationHandler resumableHandler = (ResumableWorkflowOperationHandler) handler;
        try {
          String url = resumableHandler.getHoldStateUserInterfaceURL(workflow);
          if (url != null) {
            String holdActionTitle = resumableHandler.getHoldActionTitle();
            currentOperation.setHoldActionTitle(holdActionTitle);
            currentOperation.setHoldStateUserInterfaceUrl(url);
          }
        } catch (WorkflowOperationException e) {
          logger.warn(e, "unable to replace workflow ID in the hold state URL");
        }

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
          currentOperation = (WorkflowOperationInstanceImpl) workflow.getCurrentOperation();
          currentOperation.setRetryStrategy(RetryStrategy.NONE);
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
      if (metadata != null) {

        // Series identifier
        if (isNotBlank(metadata.getSeriesIdentifier())) {
          mp.setSeries(metadata.getSeriesIdentifier());
        }

        // Series title
        if (isNotBlank(metadata.getSeriesTitle())) {
          mp.setSeriesTitle(metadata.getSeriesTitle());
        }

        // Episode title
        if (isNotBlank(metadata.getTitle())) {
          mp.setTitle(metadata.getTitle());
        }

        // Episode date
        if (metadata.getDate() != null) {
          mp.setDate(metadata.getDate());
        }

        // Episode subjects
        if (metadata.getSubjects().length > 0) {
          if (mp.getSubjects() != null) {
            for (String subject : mp.getSubjects()) {
              mp.removeSubject(subject);
            }
          }
          for (String subject : metadata.getSubjects()) {
            mp.addSubject(subject);
          }
        }

        // Episode contributers
        if (metadata.getContributors().length > 0) {
          if (mp.getContributors() != null) {
            for (String contributor : mp.getContributors()) {
              mp.removeContributor(contributor);
            }
          }
          for (String contributor : metadata.getContributors()) {
            mp.addContributor(contributor);
          }
        }

        // Episode creators
        if (mp.getCreators().length == 0 && metadata.getCreators().length > 0) {
          if (mp.getCreators() != null) {
            for (String creator : mp.getCreators()) {
              mp.removeCreator(creator);
            }
          }
          for (String creator : metadata.getCreators()) {
            mp.addCreator(creator);
          }
        }

        // Episode license
        if (isNotBlank(metadata.getLicense())) {
          mp.setLicense(metadata.getLicense());
        }

        // Episode language
        if (isNotBlank(metadata.getLanguage())) {
          mp.setLanguage(metadata.getLanguage());
        }

      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * If we are already running the maximum number of workflows, don't accept another START_WORKFLOW job.
   *
   * @see org.opencastproject.job.api.JobProducer#isReadyToAcceptJobs(String)
   */
  @Override
  public boolean isReadyToAcceptJobs(String operation) throws ServiceRegistryException {
    if (!Operation.START_WORKFLOW.toString().equals(operation))
      return true;

    long runningWorkflows;
    try {
      runningWorkflows = serviceRegistry.countByOperation(JOB_TYPE, Operation.START_WORKFLOW.toString(),
              Job.Status.RUNNING);
    } catch (ServiceRegistryException e) {
      logger.warn(e, "Unable to determine the number of running workflows");
      return false;
    }

    // If no hard maximum has been configured, ask the service registry for the number of cores in the system
    int maxWorkflows = maxConcurrentWorkflows;
    if (maxWorkflows < 1) {
      maxWorkflows = serviceRegistry.getMaxConcurrentJobs();
    }

    // Reject if there's enough going on already.
    if (runningWorkflows >= maxWorkflows) {
      logger.debug("Refused to accept new workflow. This server is already running %s workflows.", runningWorkflows);
      return false;
    }

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
  public boolean isReadyToAccept(Job job) throws ServiceRegistryException, UndispatchableJobException {
    String operation = job.getOperation();

    // Only restrict execution of new jobs
    if (!Operation.START_WORKFLOW.toString().equals(operation))
      return true;

    // If the first operation is guaranteed to pause, run the job.
    if (job.getArguments().size() > 1 && job.getArguments().get(0) != null) {
      try {
        WorkflowDefinition workflowDef = WorkflowParser.parseWorkflowDefinition(job.getArguments().get(0));
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

    WorkflowInstance workflow = null;
    WorkflowSet workflowInstances = null;
    String mediaPackageId = null;

    // Fetch all workflows that are running with the current mediapackage
    try {
      workflow = getWorkflowById(job.getId());
      mediaPackageId = workflow.getMediaPackage().getIdentifier().toString();
      workflowInstances = getWorkflowInstances(new WorkflowQuery()
              .withMediaPackage(workflow.getMediaPackage().getIdentifier().toString()).withState(RUNNING)
              .withState(PAUSED).withState(FAILING));

    } catch (NotFoundException e) {
      logger.error(
              "Trying to start workflow with id %s but no corresponding instance is available from the workflow service",
              job.getId());
      throw new UndispatchableJobException(e);
    } catch (UnauthorizedException e) {
      logger.error("Authorization denied while requesting to loading workflow instance %s: %s", job.getId(),
              e.getMessage());
      throw new UndispatchableJobException(e);
    } catch (WorkflowDatabaseException e) {
      logger.error("Error loading workflow instance %s: %s", job.getId(), e.getMessage());
      return false;
    }

    // If more than one workflow is running working on this mediapackage, then we don't start this one
    boolean toomany = workflowInstances.size() > 1;

    // Make sure we are not excluding ourselves
    toomany |= workflowInstances.size() == 1 && workflow.getId() != workflowInstances.getItems()[0].getId();

    // Avoid running multiple workflows with same media package id at the same time
    if (toomany) {
      if (!delayedWorkflows.contains(workflow.getId())) {
        logger.info("Delaying start of workflow %s, another workflow on media package %s is still running",
                workflow.getId(), mediaPackageId);
        delayedWorkflows.add(workflow.getId());
      }
      return false;
    }

    return true;
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
      serviceRegistry.updateJob(job);

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
    WorkflowOperationInstance wfo = null;
    String operation = job.getOperation();
    try {
      try {
        op = Operation.valueOf(operation);
        switch (op) {
          case START_WORKFLOW:
            workflowInstance = WorkflowParser.parseWorkflowInstance(job.getPayload());
            logger.debug("Starting new workflow %s", workflowInstance);
            runWorkflow(workflowInstance);
            break;
          case RESUME:
            workflowInstance = getWorkflowById(Long.parseLong(arguments.get(0)));
            wfo = workflowInstance.getCurrentOperation();
            Map<String, String> properties = null;
            if (arguments.size() > 1) {
              Properties props = new Properties();
              props.load(IOUtils.toInputStream(arguments.get(arguments.size() - 1)));
              properties = new HashMap<String, String>();
              for (Entry<Object, Object> entry : props.entrySet()) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
              }
            }
            logger.debug("Resuming %s at %s", workflowInstance, workflowInstance.getCurrentOperation());
            workflowInstance.setState(RUNNING);
            update(workflowInstance);
            wfo = runWorkflowOperation(workflowInstance, properties);
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
        logger.warn(e.getMessage());
        updateOperationJob(job.getId(), OperationState.FAILED);
      }
      return null;
    } catch (Exception e) {
      logger.warn(e, "Exception while accepting job " + job);
      try {
        if (workflowInstance != null) {
          logger.warn("Marking workflow instance %s as failed", workflowInstance);
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

  private WorkflowStatistics getBeanStatistics() throws WorkflowDatabaseException {
    WorkflowStatistics stats = new WorkflowStatistics();
    long total = 0L;
    long failed = 0L;
    long failing = 0L;
    long instantiated = 0L;
    long paused = 0L;
    long running = 0L;
    long stopped = 0L;
    long finished = 0L;

    Organization organization = securityService.getOrganization();
    try {
      for (Organization org : organizationDirectoryService.getOrganizations()) {
        securityService.setOrganization(org);
        WorkflowStatistics statistics = getStatistics();
        total += statistics.getTotal();
        failed += statistics.getFailed();
        failing += statistics.getFailing();
        instantiated += statistics.getInstantiated();
        paused += statistics.getPaused();
        running += statistics.getRunning();
        stopped += statistics.getStopped();
        finished += statistics.getFinished();
      }
    } finally {
      securityService.setOrganization(organization);
    }

    stats.setTotal(total);
    stats.setFailed(failed);
    stats.setFailing(failing);
    stats.setInstantiated(instantiated);
    stats.setPaused(paused);
    stats.setRunning(running);
    stats.setStopped(stopped);
    stats.setFinished(finished);
    return stats;
  }

  private List<WorkflowInstance> getHoldWorkflows() throws WorkflowDatabaseException {
    List<WorkflowInstance> workflows = new ArrayList<WorkflowInstance>();
    Organization organization = securityService.getOrganization();
    try {
      for (Organization org : organizationDirectoryService.getOrganizations()) {
        securityService.setOrganization(org);
        WorkflowQuery workflowQuery = new WorkflowQuery().withState(WorkflowInstance.WorkflowState.PAUSED).withCount(
                Integer.MAX_VALUE);
        WorkflowSet workflowSet = getWorkflowInstances(workflowQuery);
        workflows.addAll(Arrays.asList(workflowSet.getItems()));
      }
    } finally {
      securityService.setOrganization(organization);
    }
    return workflows;
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
   * Callback for the OSGi environment to register with the <code>Workspace</code>.
   *
   * @param workspace
   *          the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi environment to register with the <code>ServiceRegistry</code>.
   *
   * @param registry
   *          the service registry
   */
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
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the authorization service.
   *
   * @param authorizationService
   *          the authorizationService to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Callback for setting the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * Sets the search indexer to use in this service.
   *
   * @param index
   *          The search index
   */
  protected void setDao(WorkflowServiceIndex index) {
    this.index = index;
  }

  /**
   * Sets the series service
   *
   * @param seriesService
   *          the seriesService to set
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * Sets the message receiver
   *
   * @param messageReceiver
   *          the messageReceiver to set
   */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * Sets the message sender
   *
   * @param messageSender
   *          the messageSender to set
   */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /**
   * Callback to set the metadata service
   *
   * @param service
   *          the metadata service
   */
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
  protected void addWorkflowDefinitionScanner(WorkflowDefinitionScanner scanner) {
    workflowDefinitionScanner = scanner;
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
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary properties) throws ConfigurationException {
    String maxConfiguration = StringUtils.trimToNull((String) properties.get(MAX_CONCURRENT_CONFIG_KEY));
    if (maxConfiguration != null) {
      try {
        maxConcurrentWorkflows = Integer.parseInt(maxConfiguration);
        logger.info("Set maximum concurrent workflows to %d", maxConcurrentWorkflows);
      } catch (NumberFormatException e) {
        logger.warn("Can not set max concurrent workflows to %s. %s must be an integer", maxConfiguration,
                MAX_CONCURRENT_CONFIG_KEY);
      }
    }
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
      if (!operationName.equals(other.operationName))
        return false;
      return true;
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

  /**
   * @param workflowQuery
   *          A workflow query to run to retrieve results.
   * @return A WorkflowSet that has all of the workflow instances.
   * @throws WorkflowDatabaseException
   *           Thrown if there is a problem getting the WorkflowInstances.
   */
  public WorkflowSet getAllWorkflowInstances(WorkflowQuery workflowQuery) throws WorkflowDatabaseException {
    WorkflowSet workflowSet = getWorkflowInstances(workflowQuery);
    long total = workflowSet.getTotalCount();
    long sofar = workflowSet.getItems().length;
    if (sofar < total) {
      workflowQuery.withCount(total);
      workflowSet = getWorkflowInstances(workflowQuery);
    }
    return workflowSet;
  }

  /**
   * Find and fail jobs due to their status still being in effect after their start or end capture date and time.
   *
   * @param operation
   *          The type of operation to look for. e.g. schedule, ingest, capturing
   * @param workflowState
   *          The workflow state the given operation needs to be in.
   * @param buffer
   *          The amount of time in seconds to wait after the beginning or ending of a capture before marking that
   *          workflow as failed.
   * @param useCaptureStartTime
   *          Whether to use the start time or the end time of the capture to calculate if it is overdue.
   * @throws WorkflowDatabaseException
   *           if there is a problem reading the workflows from persistence
   * @throws IllegalArgumentException
   *           invalid buffer value, it must be equal or greater than 0
   */
  public void failJobs(String operation, WorkflowState workflowState, long buffer, boolean useCaptureStartTime)
          throws WorkflowDatabaseException, IllegalArgumentException {
    if (buffer < 0) {
      throw new IllegalArgumentException("Buffer '" + buffer
              + "' is not a valid value, it must be equal or greater than 0.");
    }

    Date now = new Date();
    Date cutoffTime;
    int failedInstances = 0;
    int cleaningFailed = 0;

    logger.info(
            "Starting to look for workflows that in operation '%s' that have failed to start if now is %s after their start time.",
            operation, Log.getHumanReadableTimeString(buffer));
    WorkflowQuery query = new WorkflowQuery().withState(workflowState);
    WorkflowInstance[] workflowInstances = getAllWorkflowInstances(query).getItems();
    if (workflowInstances != null) {
      for (WorkflowInstance workflowInstance : workflowInstances) {
        Date startTime = workflowInstance.getMediaPackage().getDate();
        if (useCaptureStartTime) {
          cutoffTime = new Date(startTime.getTime() + (buffer * MILLISECONDS_IN_SECONDS));
        } else {
          cutoffTime = new Date(startTime.getTime() + workflowInstance.getMediaPackage().getDuration()
                  + (buffer * MILLISECONDS_IN_SECONDS));
        }
        logger.debug("%d's cutoff time is %s.", workflowInstance.getId(), cutoffTime);

        if (!cutoffTime.before(now)) {
          logger.debug("Ignoring %d as it is still within the grace period. %d", workflowInstance.getId(),
                  (now.getTime() - cutoffTime.getTime()));
          continue;
        }

        WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

        if (currentOperation == null) {
          currentOperation = findIfFailedState(workflowInstance, operation);
          if (currentOperation == null) {
            logger.debug("Ignoring %d as it seems to have no current operation and it has the wrong failed operation.",
                    workflowInstance.getId());
            continue;
          }
        }

        if (currentOperation != null && !operation.equals(currentOperation.getTemplate())) {
          logger.debug("Ignoring %d as it is the wrong operation. It is '%s' instead of '%s'.",
                  workflowInstance.getId(), currentOperation.getTemplate(), operation);
          continue;
        }

        try {
          if (workflowInstance != null && currentOperation != null) {
            Job job = null;
            if (currentOperation.getId() != null) {
              job = serviceRegistry.getJob(currentOperation.getId());
            }
            if (job != null) {
              job.setStatus(Job.Status.FAILED);
            }
            currentOperation.setState(WorkflowOperationInstance.OperationState.FAILED);
            workflowInstance.setState(WorkflowState.FAILED);
            update(workflowInstance);
            removeTempFiles(workflowInstance);
            recordFailedOperationIncident(operation, buffer, job);
            failedInstances++;
          } else {
            logger.warn("Can't get current operation for workflow %s so it won't be put in a failed status.",
                    workflowInstance);
            cleaningFailed++;
          }
        } catch (WorkflowException e) {
          logger.warn("Unable to update workflow %d: %s", workflowInstance.getId(), ExceptionUtils.getStackTrace(e));
          cleaningFailed++;
        } catch (UnauthorizedException e) {
          logger.warn("Unable to update workflow %d: %s", workflowInstance.getId(), ExceptionUtils.getStackTrace(e));
          cleaningFailed++;
        } catch (IllegalStateException e) {
          logger.warn("Unable to fail job %s: %s", workflowInstance.getCurrentOperation().getId(),
                  ExceptionUtils.getStackTrace(e));
          cleaningFailed++;
        } catch (NotFoundException e) {
          logger.warn("Unable to fail job %d: %s", workflowInstance.getCurrentOperation().getId(),
                  ExceptionUtils.getStackTrace(e));
          cleaningFailed++;
        } catch (ServiceRegistryException e) {
          logger.warn("Unable to fail job %d: %s", workflowInstance.getCurrentOperation().getId(),
                  ExceptionUtils.getStackTrace(e));
          cleaningFailed++;
        } catch (NullPointerException e) {
          logger.warn("Unable to update workflow %d: %s", workflowInstance.getId(), ExceptionUtils.getStackTrace(e));
          cleaningFailed++;
        }
        logger.info("Setting %d to failed as it should have started.", workflowInstance.getId());
      }
    }

    if (failedInstances > 0) {
      logger.info("Cleaned up %d failed workflow instances", failedInstances);
    }
    if (cleaningFailed > 0) {
      logger.warn("Cleaning failed for %d failed capture workflow instances", cleaningFailed);
      throw new WorkflowDatabaseException("Unable to clean all failed capture workflow instances, see logs!");
    }
    logger.info(
            "Finished looking for workflows in operation '%s' that have failed to start after %s their start time.",
            operation, Log.getHumanReadableTimeString(buffer));
  }

  /**
   * Records a failed incident for a capture or ingest operation.
   *
   * @param operation
   *          The operation type the job was in when this occurred
   * @param buffer
   *          The amount of buffer time the job was given before it was expected to move onto the next operation.
   */
  private void recordFailedOperationIncident(String operation, long buffer, Job job) {
    int failedCode;
    if (operation.equalsIgnoreCase(SCHEDULE_OPERATION_TEMPLATE)) {
      failedCode = FAILED_CAPTURE_CODE;
    } else {
      failedCode = FAILED_INGEST_CODE;
    }
    TreeMap<String, String> properties = new TreeMap<String, String>();
    properties.put("state", operation);
    properties.put("buffer", Log.getHumanReadableTimeString(buffer));
    getServiceRegistry().incident().recordFailure(job, failedCode, properties);
  }

  /**
   * Finds a given operation position in a WorkflowInstance if the operation is failed.
   *
   * @param workflowInstance
   *          The workflow instance to search for the failed operation.
   * @param operation
   *          The operation template id to look for.
   * @return The operation if it can be found, null if it is missing.
   */
  protected WorkflowOperationInstance findIfFailedState(WorkflowInstance workflowInstance, String operation) {
    WorkflowOperationInstance result = null;
    WorkflowOperationInstance last = null;
    for (WorkflowOperationInstance current : workflowInstance.getOperations()) {
      if (current.getState().equals(OperationState.PAUSED) && operation.equals(current.getTemplate()) && last != null
              && last.getState().equals(OperationState.FAILED)) {
        result = current;
        break;
      }
      last = current;
    }
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#moveMissingCapturesFromUpcomingToFailedStatus(long)
   */
  @Override
  public synchronized void moveMissingCapturesFromUpcomingToFailedStatus(long buffer) throws WorkflowDatabaseException,
          IllegalArgumentException {
    failJobs(SCHEDULE_OPERATION_TEMPLATE, WorkflowState.PAUSED, buffer, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#moveMissingIngestsFromUpcomingToFailedStatus(long)
   */
  @Override
  public synchronized void moveMissingIngestsFromUpcomingToFailedStatus(long buffer) throws WorkflowDatabaseException,
          IllegalArgumentException {
    failJobs(CAPTURE_OPERATION_TEMPLATE, WorkflowState.PAUSED, buffer, false);
    failJobs(INGEST_OPERATION_TEMPLATE, WorkflowState.PAUSED, buffer, false);
  }

  @Override
  public synchronized void cleanupWorkflowInstances(int buffer, WorkflowState state) throws UnauthorizedException,
          WorkflowDatabaseException {
    logger.info("Start cleaning up workflow instances older than {} days with status '{}'", buffer, state);

    int instancesCleaned = 0;
    int cleaningFailed = 0;

    WorkflowQuery query = new WorkflowQuery().withState(state).withDateBefore(DateUtils.addDays(new Date(), -buffer))
            .withCount(Integer.MAX_VALUE);
    for (WorkflowInstance workflowInstance : getWorkflowInstances(query).getItems()) {
      try {
        remove(workflowInstance.getId());
        instancesCleaned++;
      } catch (WorkflowDatabaseException e) {
        throw e;
      } catch (UnauthorizedException e) {
        throw e;
      } catch (NotFoundException e) {
        // Since we are in a cleanup operation, we don't have to care about NotFoundExceptions
        logger.debug("Workflow instance '{}' could not be removed: {}", workflowInstance.getId(),
                ExceptionUtils.getStackTrace(e));
      } catch (WorkflowParsingException e) {
        logger.warn("Workflow instance '{}' could not be removed: {}", workflowInstance.getId(),
                ExceptionUtils.getStackTrace(e));
        cleaningFailed++;
      } catch (WorkflowStateException e) {
        logger.warn("Workflow instance '{}' could not be removed: {}", workflowInstance.getId(),
                ExceptionUtils.getStackTrace(e));
        cleaningFailed++;
      }
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
  public void repopulate(final String indexName) throws Exception {
    List<Job> jobs = null;
    try {
      jobs = serviceRegistry.getJobs(WorkflowService.JOB_TYPE, null);
      Iterator<Job> ji = jobs.iterator();
      while (ji.hasNext()) {
        Job job = ji.next();
        if (!WorkflowServiceImpl.Operation.START_WORKFLOW.toString().equals(job.getOperation())) {
          logger.debug("Removing unrelated job {} of type {}", job.getId(), job.getOperation());
          ji.remove();
        }
      }
    } catch (ServiceRegistryException e) {
      logger.error("Unable to load the workflows jobs: {}", e.getMessage());
      throw new ServiceException(e.getMessage());
    }

    final String destinationId = WorkflowItem.WORKFLOW_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    if (jobs.size() > 0) {
      logger.info("Populating index '{}' with {} workflows", indexName, jobs.size());
      final int total = jobs.size();
      final int[] errors = new int[1];
      errors[0] = 0;
      final int[] current = new int[1];
      current[0] = 1;
      for (final Job job : jobs) {
        if (job.getPayload() == null) {
          logger.warn("Skipping restoring of workflow {}: Payload is empty", job.getId());
          continue;
        }
        Organization organization = organizationDirectoryService.getOrganization(job.getOrganization());
        boolean erroneousWorkflowJob = SecurityUtil.runAs(securityService, organization,
                SecurityUtil.createSystemUser(componentContext, organization), new Function0<Boolean>() {
                  @Override
                  public Boolean apply() {
                    WorkflowInstance instance;
                    try {
                      instance = WorkflowParser.parseWorkflowInstance(job.getPayload());
                      messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                              WorkflowItem.updateInstance(instance));
                      messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE,
                              MessageSender.DestinationType.Queue, IndexRecreateObject.update(indexName,
                                      IndexRecreateObject.Service.Workflow, total, current[0]));
                      current[0] += 1;
                      return false;
                    } catch (WorkflowParsingException e) {
                      logger.warn("Skipping restoring of workflow job {}: {}", job.getId(),
                              ExceptionUtils.getMessage(e));
                      errors[0] += 1;
                    }
                    return true;
                  }
                });

        // Make sure this job is not being dispatched anymore
        if (erroneousWorkflowJob && JobUtil.isReadyToDispatch(job)) {
          job.setStatus(Job.Status.CANCELED);
          try {
            serviceRegistry.updateJob(job);
            logger.info("Canceled job {} because unable to restore", job);
          } catch (Exception e) {
            logger.error("Error updating erroneous job {}: {}", job.getId(), e.getMessage());
          }
        }
      }
      if (errors[0] > 0)
        logger.warn("Skipped {} erroneous workflows while populating the index", errors[0]);
    }
    logger.info("Finished populating {} index with workflows", indexName);
    Organization organization = new DefaultOrganization();
    SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(componentContext, organization),
            new Effect0() {
              @Override
              protected void run() {
                messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                        IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Workflow));
              }
            });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  @Override
  public Service getService() {
    return Service.Workflow;
  }

  @Override
  public String getClassName() {
    return WorkflowServiceImpl.class.getName();
  }

}
