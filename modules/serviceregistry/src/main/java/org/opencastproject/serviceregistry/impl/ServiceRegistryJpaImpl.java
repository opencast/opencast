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

package org.opencastproject.serviceregistry.impl;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.job.api.AbstractJobProducer.ACCEPT_JOB_LOADS_EXCEEDING_PROPERTY;
import static org.opencastproject.job.api.AbstractJobProducer.DEFAULT_ACCEPT_JOB_LOADS_EXCEEDING;
import static org.opencastproject.job.api.Job.FailureReason.DATA;
import static org.opencastproject.job.api.Job.Status.FAILED;
import static org.opencastproject.serviceregistry.api.ServiceState.ERROR;
import static org.opencastproject.serviceregistry.api.ServiceState.NORMAL;
import static org.opencastproject.serviceregistry.api.ServiceState.WARNING;
import static org.opencastproject.util.OsgiUtil.getOptContextProperty;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.HostStatistics;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.Incidents;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.serviceregistry.api.SystemLoad.NodeLoad;
import org.opencastproject.serviceregistry.impl.jmx.HostsStatistics;
import org.opencastproject.serviceregistry.impl.jmx.JobsStatistics;
import org.opencastproject.serviceregistry.impl.jmx.ServicesStatistics;
import org.opencastproject.serviceregistry.impl.jpa.HostRegistrationJpaImpl;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.util.function.ThrowingConsumer;
import org.opencastproject.util.jmx.JmxUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.management.ObjectInstance;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/** JPA implementation of the {@link ServiceRegistry} */
@Component(
  property = {
    "service.description=Service registry"
  },
  immediate = true,
  service = { ManagedService.class, ServiceRegistry.class, ServiceRegistryJpaImpl.class }
)
public class ServiceRegistryJpaImpl implements ServiceRegistry, ManagedService {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.common";

  /** Id of the workflow's start operation operation, need to match the corresponding enum value in WorkflowServiceImpl */
  public static final String START_OPERATION = "START_OPERATION";

  /** Id of the workflow's start workflow operation, need to match the corresponding enum value in WorkflowServiceImpl */
  public static final String START_WORKFLOW = "START_WORKFLOW";

  /** Id of the workflow's resume operation, need to match the corresponding enum value in WorkflowServiceImpl */
  public static final String RESUME = "RESUME";

  /** Identifier for the workflow service */
  public static final String TYPE_WORKFLOW = "org.opencastproject.workflow";

  static final Logger logger = LoggerFactory.getLogger(ServiceRegistryJpaImpl.class);

  /** The list of registered JMX beans */
  protected List<ObjectInstance> jmxBeans = new ArrayList<>();

  /** Hosts statistics JMX type */
  private static final String JMX_HOSTS_STATISTICS_TYPE = "HostsStatistics";

  /** Services statistics JMX type */
  private static final String JMX_SERVICES_STATISTICS_TYPE = "ServicesStatistics";

  /** Jobs statistics JMX type */
  private static final String JMX_JOBS_STATISTICS_TYPE = "JobsStatistics";

  /** The JMX business object for hosts statistics */
  private HostsStatistics hostsStatistics;

  /** The JMX business object for services statistics */
  private ServicesStatistics servicesStatistics;

  /** The JMX business object for jobs statistics */
  private JobsStatistics jobsStatistics;

  /** Current job used to process job in the service registry */
  private static final ThreadLocal<Job> currentJob = new ThreadLocal<>();

  /** Configuration key for the maximum load */
  protected static final String OPT_MAXLOAD = "org.opencastproject.server.maxload";

  /** Configuration key for the interval to check whether the hosts in the service registry are still alive, in seconds */
  protected static final String OPT_HEARTBEATINTERVAL = "heartbeat.interval";

  /** Configuration key for the collection of job statistics */
  protected static final String OPT_JOBSTATISTICS = "jobstats.collect";

  /** Configuration key for the retrieval of service statistics: Do not consider jobs older than max_job_age (in days) */
  protected static final String OPT_SERVICE_STATISTICS_MAX_JOB_AGE = "org.opencastproject.statistics.services.max_job_age";

  /** Configuration key for the encoding preferred worker nodes */
  protected static final String OPT_ENCODING_WORKERS = "org.opencastproject.encoding.workers";

  /** Configuration key for the encoding workers load threshold */
  protected static final String OPT_ENCODING_THRESHOLD = "org.opencastproject.encoding.workers.threshold";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Default jobs limit during dispatching
   * (larger value will fetch more entries from the database at the same time and increase RAM usage) */
  static final int DEFAULT_DISPATCH_JOBS_LIMIT = 100;

  /** Default setting on job statistics collection */
  static final boolean DEFAULT_JOB_STATISTICS = false;

  /** Default setting on service statistics retrieval */
  static final int DEFAULT_SERVICE_STATISTICS_MAX_JOB_AGE = 14;

  static final List<String>  DEFAULT_ENCODING_WORKERS = new ArrayList<String>();

  static final double DEFAULT_ENCODING_THRESHOLD = 0.0;

  /** The configuration key for setting {@link #maxAttemptsBeforeErrorState} */
  static final String MAX_ATTEMPTS_CONFIG_KEY = "max.attempts";

  /** The configuration key for setting {@link #noErrorStateServiceTypes} */
  static final String NO_ERROR_STATE_SERVICE_TYPES_CONFIG_KEY = "no.error.state.service.types";

  /** Default value for {@link #maxAttemptsBeforeErrorState} */
  private static final int DEFAULT_MAX_ATTEMPTS_BEFORE_ERROR_STATE = 10;

  /** Default value for {@link #errorStatesEnabled} */
  private static final boolean DEFAULT_ERROR_STATES_ENABLED = true;

  /** Number of failed jobs on a service before to set it in error state. -1 will disable error states completely. */
  protected int maxAttemptsBeforeErrorState = DEFAULT_MAX_ATTEMPTS_BEFORE_ERROR_STATE;
  private boolean errorStatesEnabled = DEFAULT_ERROR_STATES_ENABLED;

  /** Services for which error state is disabled */
  private List<String> noErrorStateServiceTypes = new ArrayList<>();

  /** Default delay between checking if hosts are still alive in seconds * */
  static final long DEFAULT_HEART_BEAT = 60;

  /** Default job load when not passed by service creating the job * */
  static final float DEFAULT_JOB_LOAD = 0.1f;

  /** This host's base URL */
  protected String hostName;

  /** This host's descriptive node name eg admin, worker01 */
  protected String nodeName;

  /** The base URL for job URLs */
  protected String jobHost;

  /** Comma-seperate list with URLs of encoding specialised workers*/
  protected static List<String> encodingWorkers = DEFAULT_ENCODING_WORKERS;

  /** Threshold value under which defined workers get preferred when dispatching encoding jobs */
  protected static double encodingThreshold = DEFAULT_ENCODING_THRESHOLD;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** Tracks services published locally and adds them to the service registry */
  protected RestServiceTracker tracker = null;

  /** The thread pool to use for dispatching queued jobs and checking on phantom services. */
  protected ScheduledExecutorService scheduledExecutor = null;

  /** The security service */
  protected SecurityService securityService = null;

  protected Incidents incidents;

  /** Whether to collect detailed job statistics */
  protected boolean collectJobstats = DEFAULT_JOB_STATISTICS;

  /** Maximum age of jobs being considering for service statistics */
  protected int maxJobAge = DEFAULT_SERVICE_STATISTICS_MAX_JOB_AGE;

  /** A static list of statuses that influence how load balancing is calculated */
  protected static final List<Status> JOB_STATUSES_INFLUENCING_LOAD_BALANCING;

  private static final Status[] activeJobStatus =
      Arrays.stream(Status.values()).filter(Status::isActive).collect(Collectors.toList()).toArray(new Status[0]);

  protected static HashMap<Long, Float> jobCache = new HashMap<>();

  static {
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING = new ArrayList<>();
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.RUNNING);
  }

  /** Whether to accept a job whose load exceeds the host’s max load */
  protected Boolean acceptJobLoadsExeedingMaxLoad = true;

  // Current system load
  protected float localSystemLoad = 0.0f;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.common)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activate service registry");

    db = dbSessionFactory.createSession(emf);

    // Find this host's url
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY))) {
      hostName = UrlSupport.DEFAULT_BASE_URL;
    } else {
      hostName = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
    }

    // Check hostname for sanity. It should be the hosts URL with protocol but without any part of the service paths.
    if (hostName.endsWith("/")) {
      logger.warn("The configured value of {} ends with '/'. This is very likely a configuration error which could "
              + "lead to services not working properly. Note that this configuration should not contain any part of "
              + "the service paths.", OpencastConstants.SERVER_URL_PROPERTY);
    }

    // Clean all undispatchable jobs that were orphaned when this host was last deactivated
    cleanUndispatchableJobs(hostName);

    // Register JMX beans with statistics
    try {
      List<ServiceStatistics> serviceStatistics = getServiceStatistics();
      hostsStatistics = new HostsStatistics(serviceStatistics);
      servicesStatistics = new ServicesStatistics(hostName, serviceStatistics);
      jobsStatistics = new JobsStatistics(hostName);
      jmxBeans.add(JmxUtil.registerMXBean(hostsStatistics, JMX_HOSTS_STATISTICS_TYPE));
      jmxBeans.add(JmxUtil.registerMXBean(servicesStatistics, JMX_SERVICES_STATISTICS_TYPE));
      jmxBeans.add(JmxUtil.registerMXBean(jobsStatistics, JMX_JOBS_STATISTICS_TYPE));
    } catch (ServiceRegistryException e) {
      logger.error("Error registering JMX statistic beans", e);
    }

    // Find the jobs URL
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty("org.opencastproject.jobs.url"))) {
      jobHost = hostName;
    } else {
      jobHost = cc.getBundleContext().getProperty("org.opencastproject.jobs.url");
    }

    // Register this host
    try {
      if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(OpencastConstants.NODE_NAME_PROPERTY))) {
        nodeName = null;
      } else {
        nodeName = cc.getBundleContext().getProperty(OpencastConstants.NODE_NAME_PROPERTY);
      }

      float maxLoad = Runtime.getRuntime().availableProcessors();
      if (cc != null && StringUtils.isNotBlank(cc.getBundleContext().getProperty(OPT_MAXLOAD))) {
        try {
          maxLoad = Float.parseFloat(cc.getBundleContext().getProperty(OPT_MAXLOAD));
          logger.info("Max load has been set manually to {}", maxLoad);
        } catch (NumberFormatException e) {
          logger.warn("Configuration key '{}' is not an integer. Falling back to the number of cores ({})",
                  OPT_MAXLOAD, maxLoad);
        }
      }

      logger.info("Node maximum load set to {}", maxLoad);

      String address = InetAddress.getByName(URI.create(hostName).getHost()).getHostAddress();
      long maxMemory = Runtime.getRuntime().maxMemory();
      int cores = Runtime.getRuntime().availableProcessors();

      registerHost(hostName, address, nodeName, maxMemory, cores, maxLoad);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to register host " + hostName + " in the service registry", e);
    }

    // Track any services from this host that need to be added to the service registry
    if (cc != null) {
      try {
        tracker = new RestServiceTracker(cc.getBundleContext());
        tracker.open(true);
      } catch (InvalidSyntaxException e) {
        logger.error("Invalid filter syntax:", e);
        throw new IllegalStateException(e);
      }
    }

    // Whether a service accepts a job whose load exceeds the host’s max load
    if (cc != null) {
      acceptJobLoadsExeedingMaxLoad = getOptContextProperty(cc, ACCEPT_JOB_LOADS_EXCEEDING_PROPERTY).map(Strings.toBool)
              .getOrElse(DEFAULT_ACCEPT_JOB_LOADS_EXCEEDING);
    }

    localSystemLoad = 0;
    logger.info("Activated");
  }

  @Override
  public float getOwnLoad() {
    return localSystemLoad;
  }

  @Override
  public String getRegistryHostname() {
    return hostName;
  }

  @Deactivate
  public void deactivate() {
    logger.info("deactivate service registry");

    // Wait for job dispatcher to stop before unregistering hosts and requeuing jobs
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

    for (ObjectInstance mbean : jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }

    if (tracker != null) {
      tracker.close();
    }
    try {
      unregisterHost(hostName);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Unable to unregister host " + hostName + " from the service registry", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, null, null, true, getCurrentJob(), DEFAULT_JOB_LOAD);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, null, true, getCurrentJob(), DEFAULT_JOB_LOAD);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, Float jobLoad)
          throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, null, true, getCurrentJob(), jobLoad);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, String, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean dispatchable)
          throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, dispatchable, getCurrentJob(),
            DEFAULT_JOB_LOAD);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, java.lang.String, boolean, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean dispatchable,
          Float jobLoad) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, dispatchable, getCurrentJob(), jobLoad);
  }

  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean dispatchable,
          Job parentJob) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, dispatchable, parentJob, DEFAULT_JOB_LOAD);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, java.lang.String, boolean, org.opencastproject.job.api.Job, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean dispatchable,
          Job parentJob, Float jobLoad) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, dispatchable, parentJob, jobLoad);
  }

  /**
   * Creates a job on a remote host with a jobLoad of 1.0.
   */
  public Job createJob(String host, String serviceType, String operation, List<String> arguments, String payload,
          boolean dispatchable, Job parentJob) throws ServiceRegistryException {
    return createJob(host, serviceType, operation, arguments, payload, dispatchable, parentJob, DEFAULT_JOB_LOAD);
  }

  /**
   * Creates a job on a remote host.
   */
  public Job createJob(String host, String serviceType, String operation, List<String> arguments, String payload,
          boolean dispatchable, Job parentJob, float jobLoad) throws ServiceRegistryException {
    if (StringUtils.isBlank(host)) {
      throw new IllegalArgumentException("Host can't be null");
    }
    if (StringUtils.isBlank(serviceType)) {
      throw new IllegalArgumentException("Service type can't be null");
    }
    if (StringUtils.isBlank(operation)) {
      throw new IllegalArgumentException("Operation can't be null");
    }

    JpaJob jpaJob = db.execTxChecked(em -> {
      ServiceRegistrationJpaImpl creatingService = getServiceRegistrationQuery(serviceType, host).apply(em)
          .orElseThrow(() -> new ServiceRegistryException("No service registration exists for type '" + serviceType
              + "' on host '" + host + "'"));

      if (creatingService.getHostRegistration().isMaintenanceMode()) {
        logger.warn("Creating a job from {}, which is currently in maintenance mode.", creatingService.getHost());
      } else if (!creatingService.getHostRegistration().isActive()) {
        logger.warn("Creating a job from {}, which is currently inactive.", creatingService.getHost());
      }

      User currentUser = securityService.getUser();
      Organization currentOrganization = securityService.getOrganization();

      JpaJob job = new JpaJob(currentUser, currentOrganization, creatingService, operation, arguments, payload,
              dispatchable, jobLoad);

      // Bind the given parent job to the new job
      if (parentJob != null) {
        // Get the JPA instance of the parent job
        JpaJob jpaParentJob = getJpaJobQuery(parentJob.getId()).apply(em).orElseThrow(() -> {
          logger.error("job with id {} not found in the persistence context", parentJob);
          // We don't want to leave the deleted job in the cache if there
          removeFromLoadCache(parentJob.getId());
          return new ServiceRegistryException(new NotFoundException());
        });
        job.setParentJob(jpaParentJob);

        // Get the JPA instance of the root job
        JpaJob jpaRootJob = jpaParentJob;
        if (parentJob.getRootJobId() != null) {
          jpaRootJob = getJpaJobQuery(parentJob.getRootJobId()).apply(em).orElseThrow(() -> {
            logger.error("job with id {} not found in the persistence context", parentJob.getRootJobId());
            // We don't want to leave the deleted job in the cache if there
            removeFromLoadCache(parentJob.getRootJobId());
            return new ServiceRegistryException(new NotFoundException());
          });
        }
        job.setRootJob(jpaRootJob);
      }

      // if this job is not dispatchable, it must be handled by the host that has created it
      if (dispatchable) {
        logger.trace("Queuing dispatchable '{}'", job);
        job.setStatus(Status.QUEUED);
      } else {
        logger.trace("Giving new non-dispatchable '{}' its creating service as processor '{}'", job, creatingService);
        job.setProcessorServiceRegistration(creatingService);
      }

      em.persist(job);
      return job;
    });

    setJobUri(jpaJob);
    return jpaJob.toJob();
  }

  @Override
  public void removeJobs(List<Long> jobIds) throws NotFoundException, ServiceRegistryException {
    for (long jobId: jobIds) {
      if (jobId < 1) {
        throw new NotFoundException("Job ID must be greater than zero (0)");
      }
    }

    logger.debug("Start deleting jobs with IDs '{}'", jobIds);
    try {
      db.execTxChecked(em -> {
        for (long jobId : jobIds) {
          JpaJob job = em.find(JpaJob.class, jobId);
          if (job == null) {
            logger.error("Job with Id {} cannot be deleted: Not found.", jobId);
            removeFromLoadCache(jobId);
            throw new NotFoundException("Job with ID '" + jobId + "' not found");
          }
          deleteChildJobsQuery(jobId).accept(em);
          em.remove(job);
          removeFromLoadCache(jobId);
        }
      });
    } catch (NotFoundException | ServiceRegistryException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }

    logger.info("Jobs with IDs '{}' deleted", jobIds);
  }

  private ThrowingConsumer<EntityManager, Exception> deleteChildJobsQuery(long jobId) {
    return em -> {
      List<Job> childJobs = getChildJobs(jobId);
      if (childJobs.isEmpty()) {
        logger.trace("No child jobs of job '{}' found to delete.", jobId);
        return;
      }

      logger.debug("Start deleting child jobs of job '{}'", jobId);

      try {
        for (int i = childJobs.size() - 1; i >= 0; i--) {
          Job job = childJobs.get(i);
          JpaJob jobToDelete = em.find(JpaJob.class, job.getId());
          em.remove(jobToDelete);
          removeFromLoadCache(job.getId());
          logger.debug("{} deleted", job);
        }
        logger.debug("Deleted all child jobs of job '{}'", jobId);
      } catch (Exception e) {
        throw new ServiceRegistryException("Unable to remove child jobs from " + jobId, e);
      }
    };
  }

  @Override
  public void removeParentlessJobs(int lifetime) throws ServiceRegistryException {
    int count = db.execTxChecked(em -> {
      int c = 0;

      List<Job> jobs = namedQuery.findAll("Job.withoutParent", JpaJob.class).apply(em).stream()
          .map(JpaJob::toJob)
          .filter(j -> j.getDateCreated().before(DateUtils.addDays(new Date(), -lifetime)))
          // DO NOT DELETE workflow instances and operations!
          .filter(j -> !START_OPERATION.equals(j.getOperation())
              && !START_WORKFLOW.equals(j.getOperation())
              && !RESUME.equals(j.getOperation()))
          .filter(j -> j.getStatus().isTerminated())
          .collect(Collectors.toList());

      for (Job job : jobs) {
        try {
          removeJobs(Collections.singletonList(job.getId()));
          logger.debug("Parentless '{}' removed", job);
          c++;
        } catch (NotFoundException e) {
          logger.debug("Parentless '{} ' not found in database", job, e);
        }
      }

      return c;
    });


    if (count > 0) {
      logger.info("Successfully removed {} parentless jobs", count);
    } else {
      logger.trace("No parentless jobs found to remove");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    logger.info("Updating service registry properties");

    maxAttemptsBeforeErrorState = DEFAULT_MAX_ATTEMPTS_BEFORE_ERROR_STATE;
    errorStatesEnabled = DEFAULT_ERROR_STATES_ENABLED;
    String maxAttempts = StringUtils.trimToNull((String) properties.get(MAX_ATTEMPTS_CONFIG_KEY));
    if (maxAttempts != null) {
      try {
        maxAttemptsBeforeErrorState = Integer.parseInt(maxAttempts);
        if (maxAttemptsBeforeErrorState < 0) {
          errorStatesEnabled = false;
          logger.info("Error states of services disabled");
        } else {
          errorStatesEnabled = true;
          logger.info("Set max attempts before error state to {}", maxAttempts);
        }
      } catch (NumberFormatException e) {
        logger.warn("Can not set max attempts before error state to {}. {} must be an integer", maxAttempts,
                MAX_ATTEMPTS_CONFIG_KEY);
      }
    }

    noErrorStateServiceTypes = new ArrayList<>();
    String noErrorStateServiceTypesStr = StringUtils.trimToNull((String) properties.get(
            NO_ERROR_STATE_SERVICE_TYPES_CONFIG_KEY));
    if (noErrorStateServiceTypesStr != null) {
      noErrorStateServiceTypes = Arrays.asList(noErrorStateServiceTypesStr.split("\\s*,\\s*"));
      if (!noErrorStateServiceTypes.isEmpty()) {
        logger.info("Set service types without error state to {}", String.join(", ", noErrorStateServiceTypes));
      }
    }

    long heartbeatInterval = DEFAULT_HEART_BEAT;
    String heartbeatIntervalString = StringUtils.trimToNull((String) properties.get(OPT_HEARTBEATINTERVAL));
    if (StringUtils.isNotBlank(heartbeatIntervalString)) {
      try {
        heartbeatInterval = Long.parseLong(heartbeatIntervalString);
      } catch (Exception e) {
        logger.warn("Heartbeat interval '{}' is malformed, setting to {}", heartbeatIntervalString, DEFAULT_HEART_BEAT);
        heartbeatInterval = DEFAULT_HEART_BEAT;
      }
      if (heartbeatInterval == 0) {
        logger.info("Heartbeat disabled");
      } else if (heartbeatInterval < 0) {
        logger.warn("Heartbeat interval {} seconds too low, adjusting to {}", heartbeatInterval, DEFAULT_HEART_BEAT);
        heartbeatInterval = DEFAULT_HEART_BEAT;
      } else {
        logger.info("Heartbeat interval set to {} seconds", heartbeatInterval);
      }
    }

    String jobStatsString = StringUtils.trimToNull((String) properties.get(OPT_JOBSTATISTICS));
    if (StringUtils.isNotBlank(jobStatsString)) {
      try {
        collectJobstats = Boolean.parseBoolean(jobStatsString);
      } catch (Exception e) {
        logger.warn("Job statistics collection flag '{}' is malformed, setting to {}", jobStatsString,
                DEFAULT_JOB_STATISTICS);
        collectJobstats = DEFAULT_JOB_STATISTICS;
      }
    }

    // get the encoding worker nodes defined in the configuration file and parse the comma-separated list
    String encodingWorkersString = (String) properties.get(OPT_ENCODING_WORKERS);
    if (StringUtils.isNotBlank(encodingWorkersString)) {
      encodingWorkers = Arrays.asList(encodingWorkersString.split("\\s*,\\s*"));
    } else
      encodingWorkers = DEFAULT_ENCODING_WORKERS;

    // get the encoding worker load threshold defined in the configuration file and parse the double
    String encodingThersholdString = StringUtils.trimToNull((String) properties.get(OPT_ENCODING_THRESHOLD));
    if (StringUtils.isNotBlank(encodingThersholdString) && encodingThersholdString != null) {
        try {
          double encodingThresholdTmp = Double.parseDouble(encodingThersholdString);
          if (encodingThresholdTmp >= 0 && encodingThresholdTmp <= 1)
            encodingThreshold = encodingThresholdTmp;
          else {
            encodingThreshold = DEFAULT_ENCODING_THRESHOLD;
            logger.warn("org.opencastproject.encoding.workers.threshold is not between 0 and 1");
          }
        } catch (NumberFormatException e) {
          logger.warn("Can not set encoding threshold to {}. {} must be an parsable double", encodingThersholdString,
              OPT_ENCODING_THRESHOLD);
        }
    } else
      encodingThreshold = DEFAULT_ENCODING_THRESHOLD;


    String maxJobAgeString = StringUtils.trimToNull((String) properties.get(OPT_SERVICE_STATISTICS_MAX_JOB_AGE));
    if (maxJobAgeString != null) {
      try {
        maxJobAge = Integer.parseInt(maxJobAgeString);
        logger.info("Set service statistics max job age to {}", maxJobAgeString);
      } catch (NumberFormatException e) {
        logger.warn("Can not set service statistics max job age to {}. {} must be an integer", maxJobAgeString,
                OPT_SERVICE_STATISTICS_MAX_JOB_AGE);
      }
    }

    scheduledExecutor = Executors.newScheduledThreadPool(1);

    // Schedule the service heartbeat if the interval is > 0
    if (heartbeatInterval > 0) {
      logger.debug("Starting service heartbeat at a custom interval of {}s", heartbeatInterval);
      scheduledExecutor.scheduleWithFixedDelay(new JobProducerHeartbeat(), heartbeatInterval, heartbeatInterval,
              TimeUnit.SECONDS);
    }
  }

  /**
   * OSGI callback when the configuration is updated. This method is only here to prevent the
   * configuration admin service from calling the service deactivate and activate methods
   * for a config update. It does not have to do anything as the updates are handled by updated().
   */
  @Modified
  public void modified(Map<String, Object> config) throws ConfigurationException {
    logger.debug("Modified serviceregistry");
  }

  private Function<EntityManager, Optional<JpaJob>> getJpaJobQuery(long id) {
    return em -> namedQuery.findByIdOpt(JpaJob.class, id)
        .apply(em)
        .map(jpaJob -> {
          // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
          // this row in the database
          em.refresh(jpaJob);
          setJobUri(jpaJob);
          return jpaJob;
        });
  }

  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    try {
      return db.exec(getJpaJobQuery(id))
          .map(JpaJob::toJob)
          .orElseThrow(NotFoundException::new);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getCurrentJob()
   */
  @Override
  public Job getCurrentJob() {
    return currentJob.get();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setCurrentJob(Job)
   */
  @Override
  public void setCurrentJob(Job job) {
    currentJob.set(job);
  }

  JpaJob updateJob(JpaJob job) throws ServiceRegistryException {
    try {
      // tx context is opened in
      //   updateInternal
      //   updateServiceForFailover
      return db.execChecked(em -> {
        Job oldJob = getJob(job.getId());
        JpaJob jpaJob = updateInternal(job);
        if (!TYPE_WORKFLOW.equals(job.getJobType()) && job.getJobLoad() > 0.0f
            && job.getProcessorServiceRegistration() != null
            && job.getProcessorServiceRegistration().getHost().equals(getRegistryHostname())) {
          processCachedLoadChange(job);
        }

        // All WorkflowService Jobs will be ignored
        if (oldJob.getStatus() != job.getStatus() && !TYPE_WORKFLOW.equals(job.getJobType())) {
          updateServiceForFailover(job);
        }

        return jpaJob;
      });
    } catch (ServiceRegistryException e) {
      throw e;
    } catch (NotFoundException e) {
      // Just in case, remove from cache if there
      removeFromLoadCache(job.getId());
      throw new ServiceRegistryException(e);
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @Override
  public Job updateJob(Job job) throws ServiceRegistryException {
    JpaJob jpaJob = JpaJob.from(job);
    jpaJob.setProcessorServiceRegistration(
            (ServiceRegistrationJpaImpl) getServiceRegistration(job.getJobType(), job.getProcessingHost()));
    return updateJob(jpaJob).toJob();
  }

  /**
   * Processes the job load changes for the *local* load cache
   *
   * @param job
   *   The job to apply to the load cache
   */
  private synchronized void processCachedLoadChange(JpaJob job) {
    if (JOB_STATUSES_INFLUENCING_LOAD_BALANCING.contains(job.getStatus()) && jobCache.get(job.getId()) == null) {
      logger.debug("Adding to load cache: {}, type {}, load {}, status {}",
              job, job.getJobType(), job.getJobLoad(), job.getStatus());
      localSystemLoad += job.getJobLoad();
      jobCache.put(job.getId(), job.getJobLoad());
    } else if (jobCache.get(job.getId()) != null && Status.FINISHED.equals(job.getStatus())
            || Status.FAILED.equals(job.getStatus()) || Status.WAITING.equals(job.getStatus())) {
      logger.debug("Removing from load cache: {}, type {}, load {}, status {}",
              job, job.getJobType(), job.getJobLoad(), job.getStatus());
      localSystemLoad -= job.getJobLoad();
      jobCache.remove(job.getId());
    } else {
      logger.debug("Ignoring for load cache: {}, type {}, status {}",
              job, job.getJobType(), job.getStatus());
    }
    logger.debug("Current host load: {}, job load cache size: {}", format("%.1f", localSystemLoad), jobCache.size());

    if (jobCache.isEmpty()) {
      if (Math.abs(localSystemLoad) > 0.0000001F) {
        logger.warn("No jobs in the job load cache, but load is {}: setting job load to 0", localSystemLoad);
      }
      localSystemLoad = 0.0F;
    }
  }

  private synchronized void removeFromLoadCache(Long jobId) {
    if (jobCache.get(jobId) != null) {
      float jobLoad = jobCache.get(jobId);
      logger.debug("Removing deleted job from load cache: Job {}, load {}", jobId, jobLoad);
      localSystemLoad -= jobLoad;
      jobCache.remove(jobId);
    }
  }

  protected JpaJob setJobUri(JpaJob job) {
    try {
      job.setUri(new URI(jobHost + "/services/job/" + job.getId() + ".xml"));
    } catch (URISyntaxException e) {
      logger.warn("Can not set the job URI", e);
    }
    return job;
  }

  /**
   * Internal method to update a job, throwing unwrapped JPA exceptions.
   *
   * @param job
   *          the job to update
   * @return the updated job
   */
  protected JpaJob updateInternal(JpaJob job) throws NotFoundException {
    JpaJob fromDb = db.execTxChecked(em -> {
      JpaJob j = em.find(JpaJob.class, job.getId());
      if (j == null) {
        throw new NotFoundException();
      }

      update(j, job);
      em.merge(j);
      return j;
    });

    job.setVersion(fromDb.toJob().getVersion());
    setJobUri(job);
    return job;
  }

  public void updateStatisticsJobData() {
    jobsStatistics.updateAvg(db.exec(getAvgOperationsQuery()));
    jobsStatistics.updateJobCount(db.exec(getCountPerHostServiceQuery()));
  }

  /**
   * Internal method to update the service registration state, throwing unwrapped JPA exceptions.
   *
   * @param registration
   *          the service registration to update
   * @return the updated service registration
   */
  private ServiceRegistration updateServiceState(ServiceRegistrationJpaImpl registration) throws NotFoundException {
    db.execTxChecked(em -> {
      ServiceRegistrationJpaImpl fromDb = em.find(ServiceRegistrationJpaImpl.class, registration.getId());
      if (fromDb == null) {
        throw new NotFoundException();
      }
      fromDb.setServiceState(registration.getServiceState());
      fromDb.setStateChanged(registration.getStateChanged());
      fromDb.setWarningStateTrigger(registration.getWarningStateTrigger());
      fromDb.setErrorStateTrigger(registration.getErrorStateTrigger());
    });

    servicesStatistics.updateService(registration);
    return registration;
  }

  /**
   * Sets the queue and runtimes and other elements of a persistent job based on a job that's been modified in memory.
   * Times on both the objects must be modified, since the in-memory job must not be stale.
   *
   * @param fromDb
   *          The job from the database
   * @param jpaJob
   *          The in-memory job
   */
  private void update(JpaJob fromDb, JpaJob jpaJob) {
    final Job job = jpaJob.toJob();
    final Date now = new Date();
    final Status status = job.getStatus();
    final Status fromDbStatus = fromDb.getStatus();

    fromDb.setPayload(job.getPayload());
    fromDb.setStatus(job.getStatus());
    fromDb.setDispatchable(job.isDispatchable());
    fromDb.setVersion(job.getVersion());
    fromDb.setOperation(job.getOperation());
    fromDb.setArguments(job.getArguments());

    if (job.getDateCreated() == null) {
      jpaJob.setDateCreated(now);
      fromDb.setDateCreated(now);
      job.setDateCreated(now);
    }
    if (job.getProcessingHost() != null) {
      ServiceRegistrationJpaImpl processingService = (ServiceRegistrationJpaImpl) getServiceRegistration(
              job.getJobType(), job.getProcessingHost());
      logger.debug("{} has host '{}': setting processor service to '{}'", job, job.getProcessingHost(), processingService);
      fromDb.setProcessorServiceRegistration(processingService);
    } else {
      logger.debug("Unsetting previous processor service registration for {}", job);
      fromDb.setProcessorServiceRegistration(null);
    }
    if (Status.RUNNING.equals(status) && !Status.WAITING.equals(fromDbStatus)) {
      if (job.getDateStarted() == null) {
        jpaJob.setDateStarted(now);
        jpaJob.setQueueTime(now.getTime() - job.getDateCreated().getTime());
        fromDb.setDateStarted(now);
        fromDb.setQueueTime(now.getTime() - job.getDateCreated().getTime());
        job.setDateStarted(now);
        job.setQueueTime(now.getTime() - job.getDateCreated().getTime());
      }
    } else if (Status.FAILED.equals(status)) {
      // failed jobs may not have even started properly
      if (job.getDateCompleted() == null) {
        fromDb.setDateCompleted(now);
        jpaJob.setDateCompleted(now);
        job.setDateCompleted(now);
        if (job.getDateStarted() != null) {
          jpaJob.setRunTime(now.getTime() - job.getDateStarted().getTime());
          fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
          job.setRunTime(now.getTime() - job.getDateStarted().getTime());
        }
      }
    } else if (Status.FINISHED.equals(status)) {
      if (job.getDateStarted() == null) {
        // Some services (e.g. ingest) don't use job dispatching, since they start immediately and handle their own
        // lifecycle. In these cases, if the start date isn't set, use the date created as the start date
        jpaJob.setDateStarted(job.getDateCreated());
        job.setDateStarted(job.getDateCreated());
      }
      if (job.getDateCompleted() == null) {
        jpaJob.setDateCompleted(now);
        jpaJob.setRunTime(now.getTime() - job.getDateStarted().getTime());
        fromDb.setDateCompleted(now);
        fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
        job.setDateCompleted(now);
        job.setRunTime(now.getTime() - job.getDateStarted().getTime());
      }
    }
  }

  /**
   * Fetches a host registration from persistence.
   *
   * @param host
   *          the host name
   * @return the host registration, or null if none exists
   */
  protected Function<EntityManager, Optional<HostRegistrationJpaImpl>> fetchHostRegistrationQuery(String host) {
    return namedQuery.findOpt(
        "HostRegistration.byHostName",
        HostRegistrationJpaImpl.class,
        Pair.of("host", host)
    );
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(String, String, String, long, int, float)
   */
  @Override
  public void registerHost(String host, String address, String nodeName, long memory, int cores, float maxLoad)
          throws ServiceRegistryException {
    try {
      HostRegistrationJpaImpl hostRegistration = db.execTxChecked(em -> {
        // Find the existing registrations for this host and if it exists, update it
        Optional<HostRegistrationJpaImpl> hostRegistrationOpt = fetchHostRegistrationQuery(host).apply(em);
        HostRegistrationJpaImpl hr;

        if (hostRegistrationOpt.isEmpty()) {
          hr = new HostRegistrationJpaImpl(host, address, nodeName, memory, cores, maxLoad, true, false);
          em.persist(hr);
        } else {
          hr = hostRegistrationOpt.get();
          hr.setIpAddress(address);
          hr.setNodeName(nodeName);
          hr.setMemory(memory);
          hr.setCores(cores);
          hr.setMaxLoad(maxLoad);
          hr.setOnline(true);
          em.merge(hr);
        }
        logger.info("Registering {} with a maximum load of {}", host, maxLoad);
        return hr;
      });

      hostsStatistics.updateHost(hostRegistration);
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unregisterHost(java.lang.String)
   */
  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
    try {
      HostRegistrationJpaImpl existingHostRegistration = db.execTxChecked(em -> {
        HostRegistrationJpaImpl hr = fetchHostRegistrationQuery(host).apply(em).orElseThrow(
            () -> new IllegalArgumentException("Host '" + host + "' is not registered, so it can not be unregistered"));

        hr.setOnline(false);
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          unRegisterService(serviceRegistration.getServiceType(), serviceRegistration.getHost());
        }
        em.merge(hr);

        logger.info("Unregistering {}", host);
        return hr;
      });

      logger.info("Host {} unregistered", host);
      hostsStatistics.updateHost(existingHostRegistration);
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#enableHost(String)
   */
  @Override
  public void enableHost(String host) throws ServiceRegistryException, NotFoundException {
    try {
      HostRegistrationJpaImpl hostRegistration = db.execTxChecked(em -> {
        // Find the existing registrations for this host and if it exists, update it
        HostRegistrationJpaImpl hr = fetchHostRegistrationQuery(host).apply(em).orElseThrow(
            () -> new NotFoundException("Host '" + host + "' is currently not registered, so it can not be enabled"));
        hr.setActive(true);
        em.merge(hr);
        logger.info("Enabling {}", host);
        return hr;
      });

      db.execTxChecked(em -> {
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          ((ServiceRegistrationJpaImpl) serviceRegistration).setActive(true);
          em.merge(serviceRegistration);
          servicesStatistics.updateService(serviceRegistration);
        }
      });

      hostsStatistics.updateHost(hostRegistration);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#disableHost(String)
   */
  @Override
  public void disableHost(String host) throws ServiceRegistryException, NotFoundException {
    try {
      HostRegistrationJpaImpl hostRegistration = db.execTxChecked(em -> {
        HostRegistrationJpaImpl hr = fetchHostRegistrationQuery(host).apply(em).orElseThrow(
            () -> new NotFoundException("Host '" + host + "' is not currently registered, so it can not be disabled"));

        hr.setActive(false);
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          ((ServiceRegistrationJpaImpl) serviceRegistration).setActive(false);
          em.merge(serviceRegistration);
          servicesStatistics.updateService(serviceRegistration);
        }
        em.merge(hr);

        logger.info("Disabling {}", host);
        return hr;
      });

      hostsStatistics.updateHost(hostRegistration);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path)
          throws ServiceRegistryException {
    return registerService(serviceType, baseUrl, path, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path, boolean jobProducer)
          throws ServiceRegistryException {
    cleanRunningJobs(serviceType, baseUrl);
    return setOnlineStatus(serviceType, baseUrl, path, true, jobProducer);
  }

  protected Function<EntityManager, Optional<ServiceRegistrationJpaImpl>> getServiceRegistrationQuery(
      String serviceType, String host) {
    return namedQuery.findOpt(
        "ServiceRegistration.getRegistration",
        ServiceRegistrationJpaImpl.class,
        Pair.of("serviceType", serviceType),
        Pair.of("host", host)
    );
  }

  /**
   * Sets the online status of a service registration.
   *
   * @param serviceType
   *          The job type
   * @param baseUrl
   *          the host URL
   * @param online
   *          whether the service is online or off
   * @param jobProducer
   *          whether this service produces jobs for long running operations
   * @return the service registration
   */
  protected ServiceRegistration setOnlineStatus(String serviceType, String baseUrl, String path, boolean online,
          Boolean jobProducer) throws ServiceRegistryException {
    if (isBlank(serviceType) || isBlank(baseUrl)) {
      logger.info("Uninformed baseUrl '{}' or service '{}' (path '{}')", baseUrl, serviceType, path);
      throw new IllegalArgumentException("serviceType and baseUrl must not be blank");
    }

    try {
      AtomicReference<HostRegistrationJpaImpl> hostRegistration = new AtomicReference<>();
      AtomicReference<ServiceRegistrationJpaImpl> registration = new AtomicReference<>();

      db.execTxChecked(em -> {
        HostRegistrationJpaImpl hr = fetchHostRegistrationQuery(baseUrl).apply(em).orElseThrow(() -> {
          logger.info("No associated host registration for '{}' or service '{}' (path '{}')", baseUrl, serviceType,path);
          return new IllegalStateException(
              "A service registration can not be updated when it has no associated host registration");
        });
        hostRegistration.set(hr);

        ServiceRegistrationJpaImpl sr;
        Optional<ServiceRegistrationJpaImpl> srOpt = getServiceRegistrationQuery(serviceType, baseUrl).apply(em);
        if (srOpt.isEmpty()) {
          if (isBlank(path)) {
            // we can not create a new registration without a path
            throw new IllegalArgumentException("path must not be blank when registering new services");
          }

          // if we are not provided a value, consider it to be false
          sr = new ServiceRegistrationJpaImpl(hr, serviceType, path, Objects.requireNonNullElse(jobProducer, false));
          em.persist(sr);
        } else {
          sr = srOpt.get();
          if (StringUtils.isNotBlank(path)) {
            sr.setPath(path);
          }
          sr.setOnline(online);
          if (jobProducer != null) { // if we are not provided a value, don't update the persistent value
            sr.setJobProducer(jobProducer);
          }
          em.merge(sr);
        }
        registration.set(sr);
      });

      hostsStatistics.updateHost(hostRegistration.get());
      servicesStatistics.updateService(registration.get());
      return registration.get();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String baseUrl) throws ServiceRegistryException {
    logger.info("Unregistering Service {}@{} and cleaning its running jobs", serviceType, baseUrl);
    // TODO: create methods that accept an entity manager, so we can execute multiple queries using the same em and tx
    //       (em and tx are reused if using nested db.execTx)
    setOnlineStatus(serviceType, baseUrl, null, false, null);
    cleanRunningJobs(serviceType, baseUrl);
  }

  /**
   * Find all undispatchable jobs that were orphaned when this host was last deactivated and set them to CANCELLED.
   */
  private void cleanUndispatchableJobs(String hostName) {
    logger.debug("Starting check for undispatchable jobs for host {}", hostName);

    try {
      db.execTxChecked(em -> {
        List<JpaJob> undispatchableJobs = namedQuery.findAll(
            "Job.undispatchable.status",
            JpaJob.class,
            Pair.of("statuses", List.of(
                Status.INSTANTIATED.ordinal(),
                Status.RUNNING.ordinal()
            ))
        ).apply(em);

        if (undispatchableJobs.size() > 0) {
          logger.info("Found {} undispatchable jobs on host {}", undispatchableJobs.size(), hostName);
        }

        for (JpaJob job : undispatchableJobs) {
          // Make sure the job was processed on this host
          String jobHost = "";
          if (job.getProcessorServiceRegistration() != null) {
            jobHost = job.getProcessorServiceRegistration().getHost();
          }

          if (!jobHost.equals(hostName)) {
            logger.debug("Will not cancel undispatchable job {} for host {}, it is running on a different host ({})",
                job, hostName, jobHost);
            continue;
          }

          logger.info("Cancelling the running undispatchable job {}, it was orphaned on this host ({})", job, hostName);
          job.setStatus(Status.CANCELLED);
          em.merge(job);
        }
      });
    } catch (Exception e) {
      logger.error("Unable to clean undispatchable jobs for host {}! {}", hostName, e.getMessage());
    }
  }

  /**
   * Find all running jobs on this service and set them to RESET or CANCELLED.
   *
   * @param serviceType
   *          the service type
   * @param baseUrl
   *          the base url
   * @throws ServiceRegistryException
   *           if there is a problem communicating with the jobs database
   */
  private void cleanRunningJobs(String serviceType, String baseUrl) throws ServiceRegistryException {
    try {
      db.execTxChecked(em -> {
        TypedQuery<JpaJob> query = em.createNamedQuery("Job.processinghost.status", JpaJob.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setParameter("statuses", List.of(
                Status.RUNNING.ordinal(),
                Status.DISPATCHING.ordinal(),
                Status.WAITING.ordinal()
            ))
            .setParameter("host", baseUrl)
            .setParameter("serviceType", serviceType);

        List<JpaJob> unregisteredJobs = query.getResultList();
        if (unregisteredJobs.size() > 0) {
          logger.info("Found {} jobs to clean for {}@{}", unregisteredJobs.size(), serviceType, baseUrl);
        }

        for (JpaJob job : unregisteredJobs) {
          if (job.isDispatchable()) {
            em.refresh(job);
            // If this job has already been treated
            if (Status.CANCELLED.equals(job.getStatus()) || Status.RESTART.equals(job.getStatus())) {
              continue;
            }

            if (job.getRootJob() != null && Status.PAUSED.equals(job.getRootJob().getStatus())) {
              JpaJob rootJob = job.getRootJob();
              cancelAllChildrenQuery(rootJob).accept(em);
              rootJob.setStatus(Status.RESTART);
              rootJob.setOperation(START_OPERATION);
              em.merge(rootJob);
              continue;
            }

            logger.info("Marking child jobs from {} as canceled", job);
            cancelAllChildrenQuery(job).accept(em);

            logger.info("Rescheduling lost {}", job);
            job.setStatus(Status.RESTART);
            job.setProcessorServiceRegistration(null);
          } else {
            logger.info("Marking lost {} as failed", job);
            job.setStatus(Status.FAILED);
          }

          em.merge(job);
        }
      });
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * Go through all the children recursively to set them in {@link Status#CANCELLED} status
   *
   * @param job
   *          the parent job
   */
  private Consumer<EntityManager> cancelAllChildrenQuery(JpaJob job) {
    return em -> job.getChildJobs().stream()
        .peek(em::refresh)
        .filter(child -> Status.CANCELLED.equals(child.getStatus()))
        .forEach(child -> {
          cancelAllChildrenQuery(child).accept(em);
          child.setStatus(Status.CANCELLED);
          em.merge(child);
        });
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String baseUrl, boolean maintenance) throws NotFoundException {
    logger.info("Setting maintenance mode on host '{}'", baseUrl);
    HostRegistrationJpaImpl reg = db.execTxChecked(em -> {
      HostRegistrationJpaImpl hr = fetchHostRegistrationQuery(baseUrl).apply(em).orElseThrow(() -> {
            logger.warn("Can not set maintenance mode because host '{}' was not registered", baseUrl);
        return new NotFoundException("Can not set maintenance mode on a host that has not been registered");
      });
      hr.setMaintenanceMode(maintenance);
      em.merge(hr);
      return hr;
    });

    hostsStatistics.updateHost(reg);
    logger.info("Finished setting maintenance mode on host '{}'", baseUrl);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() {
    return db.exec(getServiceRegistrationsQuery());
  }

  @Override
  public Incidents incident() {
    return incidents;
  }

  private List<ServiceRegistration> getOnlineServiceRegistrations() {
    return db.exec(namedQuery.findAll("ServiceRegistration.getAllOnline", ServiceRegistration.class));
  }

  /**
   * Gets all service registrations.
   *
   * @return the list of service registrations
   */
  protected Function<EntityManager, List<ServiceRegistration>> getServiceRegistrationsQuery() {
    return namedQuery.findAll("ServiceRegistration.getAll", ServiceRegistration.class);
  }

  /**
   * Gets all host registrations
   *
   * @return the list of host registrations
   */
  @Override
  public List<HostRegistration> getHostRegistrations() {
    return db.exec(getHostRegistrationsQuery());
  }

  @Override
  public HostStatistics getHostStatistics() {
    HostStatistics statistics = new HostStatistics();

    db.exec(namedQuery.findAll(
        "HostRegistration.jobStatistics",
        Object[].class,
        Pair.of("status", List.of(Status.QUEUED.ordinal(), Status.RUNNING.ordinal()))
    )).forEach(row -> {
      final long host = ((Number) row[0]).longValue();
      final int status = ((Number) row[1]).intValue();
      final long count = ((Number) row[2]).longValue();

      if (status == Status.RUNNING.ordinal()) {
        statistics.addRunning(host, count);
      } else {
        statistics.addQueued(host, count);
      }
    });

    return statistics;
  }

  /**
   * Gets all host registrations
   *
   * @return the list of host registrations
   */
  protected Function<EntityManager, List<HostRegistration>> getHostRegistrationsQuery() {
    return namedQuery.findAll("HostRegistration.getAll", HostRegistration.class);
  }

  @Override
  public HostRegistration getHostRegistration(String hostname) throws ServiceRegistryException {
    return db.exec(getHostRegistrationQuery(hostname));
  }

  protected Function<EntityManager, HostRegistration> getHostRegistrationQuery(String hostname) {
    return namedQuery.find(
        "HostRegistration.byHostName",
        HostRegistration.class,
        Pair.of("host", hostname)
    );
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getChildJobs(long)
   */
  @Override
  public List<Job> getChildJobs(long id) throws ServiceRegistryException {
    try {
      List<JpaJob> jobs = db.exec(namedQuery.findAll(
          "Job.root.children",
          JpaJob.class,
          Pair.of("id", id)
      ));

      if (jobs.size() == 0) {
        jobs = db.exec(getChildrenQuery(id));
      }

      return jobs.stream()
          .map(this::setJobUri)
          .map(JpaJob::toJob)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  private Function<EntityManager, List<JpaJob>> getChildrenQuery(long id) {
    return em -> {
      TypedQuery<JpaJob> query = em
          .createNamedQuery("Job.children", JpaJob.class)
          .setParameter("id", id);

      List<JpaJob> childJobs = query.getResultList();

      List<JpaJob> result = new ArrayList<>(childJobs);
      childJobs.stream()
          .map(j -> getChildrenQuery(j.getId()).apply(em))
          .forEach(result::addAll);

      return result;
    };
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String, Status)
   */
  @Override
  public List<Job> getJobs(String type, Status status) throws ServiceRegistryException {
    logger.trace("Getting jobs '{}' and '{}'", type, status);

    Function<EntityManager, List<JpaJob>> jobsQuery;
    if (type == null && status == null) {
      jobsQuery = namedQuery.findAll("Job.all", JpaJob.class);
    } else if (type == null) {
      jobsQuery = namedQuery.findAll(
          "Job.status",
          JpaJob.class,
          Pair.of("status", status.ordinal())
      );
    } else if (status == null) {
      jobsQuery = namedQuery.findAll(
          "Job.type",
          JpaJob.class,
          Pair.of("serviceType", type)
      );
    } else {
      jobsQuery = namedQuery.findAll(
          "Job",
          JpaJob.class,
          Pair.of("status", status.ordinal()),
          Pair.of("serviceType", type)
      );
    }

    try {
      return db.exec(jobsQuery).stream()
          .peek(this::setJobUri)
          .map(JpaJob::toJob)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @Override
  public List<String> getJobPayloads(String operation) throws ServiceRegistryException {
    try {
      return db.exec(namedQuery.findAll(
          "Job.payload",
          String.class,
          Pair.of("operation", operation)
      ));
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @Override
  public List<String> getJobPayloads(String operation, int limit, int offset) throws ServiceRegistryException {
    try {
      return db.exec(em -> {
        return em.createNamedQuery("Job.payload", String.class)
            .setParameter("operation", operation)
            .setMaxResults(limit)
            .setFirstResult(offset)
            .getResultList();
      });
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @Override
  public int getJobCount(final String operation) throws ServiceRegistryException {
    try {
      return db.exec(namedQuery.find(
          "Job.countByOperationOnly",
          Number.class,
          Pair.of("operation", operation)
      )).intValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getActiveJobs()
   */
  @Override
  public List<Job> getActiveJobs() throws ServiceRegistryException {
    try {
      return db.exec(getJobsByStatusQuery(activeJobStatus)).stream()
          .map(JpaJob::toJob)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * Get the list of jobs with status from the given statuses.
   *
   * @param statuses
   *          variable sized array of status values to test on jobs
   * @return list of jobs with status from statuses
   */
  public Function<EntityManager, List<JpaJob>> getJobsByStatusQuery(Status... statuses) {
    if (statuses == null || statuses.length < 1) {
      throw new IllegalArgumentException("At least one job status must be given.");
    }

    return namedQuery.findAll(
        "Job.statuses",
        JpaJob.class,
        Pair.of("statuses", Arrays.stream(statuses).map(Enum::ordinal).collect(Collectors.toList()))
    ).andThen(jobs -> jobs.stream()
        .peek(this::setJobUri)
        .collect(Collectors.toList()));
  }

  /**
   * Gets jobs of all types that are in the given state.
   *
   * @param offset apply offset to the db query if offset &gt; 0
   * @param limit apply limit to the db query if limit &gt; 0
   * @param statuses the job status should be one from the given statuses
   * @return the list of jobs waiting for dispatch
   */
  protected Function<EntityManager, List<JpaJob>> getDispatchableJobsWithStatusQuery(int offset, int limit,
      Status... statuses) {
    return em -> {
      if (statuses == null) {
        return Collections.emptyList();
      }

      TypedQuery<JpaJob> query = em
          .createNamedQuery("Job.dispatchable.status", JpaJob.class)
          .setParameter("statuses", Arrays.stream(statuses).map(Enum::ordinal).collect(Collectors.toList()));
      if (offset > 0) {
        query.setFirstResult(offset);
      }
      if (limit > 0) {
        query.setMaxResults(limit);
      }
      return query.getResultList();
    };
  }

  Function<EntityManager, List<Object[]>> getAvgOperationsQuery() {
    return namedQuery.findAll("Job.avgOperation", Object[].class);
  }

  Function<EntityManager, List<Object[]>> getCountPerHostServiceQuery() {
    return namedQuery.findAll("Job.countPerHostService", Object[].class);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    Function<EntityManager, Number> countQuery;
    if (serviceType == null && status == null) {
      countQuery = namedQuery.find(
          "Job.count.all",
          Number.class
      );
    } else if (serviceType == null) {
      countQuery = namedQuery.find(
          "Job.count.nullType",
          Number.class,
          Pair.of("status", status.ordinal())
      );
    } else if (status == null) {
      countQuery = namedQuery.find(
          "Job.count.nullStatus",
          Number.class,
          Pair.of("serviceType", serviceType)
      );
    } else {
      countQuery = namedQuery.find(
          "Job.count",
          Number.class,
          Pair.of("status", status.ordinal()),
          Pair.of("serviceType", serviceType)
      );
    }

    try {
      return db.exec(countQuery).longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByHost(java.lang.String, java.lang.String,
   *      Status)
   */
  @Override
  public long countByHost(String serviceType, String host, Status status) throws ServiceRegistryException {
    Function<EntityManager, Number> countQuery;
    if (serviceType != null && !serviceType.isEmpty()) {
      countQuery = namedQuery.find(
          "Job.countByHost",
          Number.class,
          Pair.of("serviceType", serviceType),
          Pair.of("status", status.ordinal()),
          Pair.of("host", host)
      );
    } else {
      countQuery = namedQuery.find(
          "Job.countByHost.nullType",
          Number.class,
          Pair.of("status", status.ordinal()),
          Pair.of("host", host)
      );
    }

    try {
      return db.exec(countQuery).longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByOperation(java.lang.String, java.lang.String,
   *      Status)
   */

  @Override
  public long countByOperation(String serviceType, String operation, Status status) throws ServiceRegistryException {
    try {
      return db.exec(namedQuery.find(
          "Job.countByOperation",
          Number.class,
          Pair.of("status", status.ordinal()),
          Pair.of("serviceType", serviceType),
          Pair.of("operation", operation)
      )).longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, java.lang.String,
   *      java.lang.String, Status)
   */
  @Override
  public long count(String serviceType, String host, String operation, Status status) throws ServiceRegistryException {
    if (StringUtils.isBlank(serviceType) || StringUtils.isBlank(host) || StringUtils.isBlank(operation)
            || status == null) {
      throw new IllegalArgumentException("service type, host, operation, and status must be provided");
    }

    try {
      return db.exec(namedQuery.find(
          "Job.fullMonty",
          Number.class,
          Pair.of("status", status.ordinal()),
          Pair.of("serviceType", serviceType),
          Pair.of("operation", operation)
      )).longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @Override
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
    Date now = new Date();
    try {
      return db.exec(getServiceStatisticsQuery(
          DateUtils.addDays(now, -maxJobAge),
          DateUtils.addDays(now, 1) // Avoid glitches around 'now' by setting the endDate to 'tomorrow'
      ));
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * Gets performance and runtime statistics for each known service registration.
   * For the statistics, only jobs created within the time interval [startDate, endDate] are being considered
   *
   * @param startDate
   *          Only jobs created after this data are considered for statistics
   * @param endDate
   *          Only jobs created before this data are considered for statistics
   * @return the service statistics
   */
  private Function<EntityManager, List<ServiceStatistics>> getServiceStatisticsQuery(Date startDate, Date endDate) {
    return em -> {
      Map<Long, JaxbServiceStatistics> statsMap = new HashMap<>();

      // Make sure we also include the services that have no processing history so far
      namedQuery.findAll("ServiceRegistration.getAll", ServiceRegistrationJpaImpl.class).apply(em).forEach(s ->
        statsMap.put(s.getId(), new JaxbServiceStatistics(s))
      );

      if (collectJobstats) {
        // Build stats map
        namedQuery.findAll(
            "ServiceRegistration.statistics",
            Object[].class,
            Pair.of("minDateCreated", startDate),
            Pair.of("maxDateCreated", endDate)
        ).apply(em).forEach(row -> {
          Number serviceRegistrationId = (Number) row[0];
          if (serviceRegistrationId == null || serviceRegistrationId.longValue() == 0) {
            return;
          }
          Status status = Status.values()[((Number) row[1]).intValue()];
          Number count = (Number) row[2];
          Number meanQueueTime = (Number) row[3];
          Number meanRunTime = (Number) row[4];

          // The statistics query returns a cartesian product, so we need to iterate over them to build up the objects
          JaxbServiceStatistics stats = statsMap.get(serviceRegistrationId.longValue());
          if (stats == null) {
            return;
          }

          // the status will be null if there are no jobs at all associated with this service registration
          if (status != null) {
            switch (status) {
              case RUNNING:
                stats.setRunningJobs(count.intValue());
                break;
              case QUEUED:
              case DISPATCHING:
                stats.setQueuedJobs(count.intValue());
                break;
              case FINISHED:
                stats.setMeanRunTime(meanRunTime.longValue());
                stats.setMeanQueueTime(meanQueueTime.longValue());
                stats.setFinishedJobs(count.intValue());
                break;
              default:
                break;
            }
          }
        });
      }

      List<ServiceStatistics> stats = new ArrayList<>(statsMap.values());
      stats.sort((o1, o2) -> {
        ServiceRegistration reg1 = o1.getServiceRegistration();
        ServiceRegistration reg2 = o2.getServiceRegistration();
        int typeComparison = reg1.getServiceType().compareTo(reg2.getServiceType());
        return typeComparison == 0
            ? reg1.getHost().compareTo(reg2.getHost())
            : typeComparison;
      });

      return stats;
    };
  }

  /**
   * Do not look at this, it will burn your eyes! This is due to JPA's inability to do a left outer join with join
   * conditions.
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByLoad(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException {
    SystemLoad loadByHost = getCurrentHostLoads();
    List<HostRegistration> hostRegistrations = getHostRegistrations();
    List<ServiceRegistration> serviceRegistrations = getServiceRegistrationsByType(serviceType);
    return getServiceRegistrationsByLoad(serviceType, serviceRegistrations, hostRegistrations, loadByHost);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getCurrentHostLoads()
   */
  @Override
  public SystemLoad getCurrentHostLoads() {
    return db.exec(getHostLoadsQuery());
  }

  /**
   * Gets a map of hosts to the number of jobs currently loading that host
   *
   * @return the map of hosts to job counts
   */
  Function<EntityManager, SystemLoad> getHostLoadsQuery() {
    return em -> {
      final SystemLoad systemLoad = new SystemLoad();

      // Find all jobs that are currently running on any given host, or get all of them
      List<Integer> statuses = JOB_STATUSES_INFLUENCING_LOAD_BALANCING.stream()
          .map(Enum::ordinal)
          .collect(Collectors.toList());
      List<Object[]> rows = namedQuery.findAll(
          "ServiceRegistration.hostloads",
          Object[].class,
          Pair.of("statuses", statuses),
          // Note: This is used in the query to filter out workflow jobs.
          // These jobs are load balanced by the workflow service directly.
          Pair.of("workflow_type", TYPE_WORKFLOW)
      ).apply(em);

      // Accumulate the numbers for relevant job statuses per host
      for (Object[] row : rows) {
        String host = String.valueOf(row[0]);
        Status status = Status.values()[(int) row[1]];
        float currentLoad = ((Number) row[2]).floatValue();
        float maxLoad = ((Number) row[3]).floatValue();

        // Only queued, and running jobs are adding to the load, so every other status is discarded
        if (status == null || !JOB_STATUSES_INFLUENCING_LOAD_BALANCING.contains(status)) {
          currentLoad = 0.0f;
        }
        // Add the service registration
        NodeLoad serviceLoad = new NodeLoad(host, currentLoad, maxLoad);
        systemLoad.addNodeLoad(serviceLoad);
      }

      // This is important, otherwise services which have no current load are not listed in the output!
      getHostRegistrationsQuery().apply(em).stream()
          .filter(h -> !systemLoad.containsHost(h.getBaseUrl()))
          .forEach(h -> systemLoad.addNodeLoad(new NodeLoad(h.getBaseUrl(), 0.0f, h.getMaxLoad())));
      return systemLoad;
    };
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    return db.exec(namedQuery.findAll(
        "ServiceRegistration.getByType",
        ServiceRegistration.class,
        Pair.of("serviceType", serviceType)
    ));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByHost(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    return db.exec(getServiceRegistrationsByHostQuery(host));
  }

  private Function<EntityManager, List<ServiceRegistration>> getServiceRegistrationsByHostQuery(String host) {
    return namedQuery.findAll(
        "ServiceRegistration.getByHost",
        ServiceRegistration.class,
        Pair.of("host", host)
    );
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) {
    return db.exec(getServiceRegistrationQuery(serviceType, host))
        .orElse(null);
  }

  /**
   * A custom ServiceTracker that registers all locally published servlets so clients can find the most appropriate
   * service on the network to handle new jobs.
   */
  class RestServiceTracker extends ServiceTracker {
    protected static final String FILTER = "(" +  JaxrsWhiteboardConstants.JAX_RS_RESOURCE + "=true)";

    protected BundleContext bundleContext;

    RestServiceTracker(BundleContext bundleContext) throws InvalidSyntaxException {
      super(bundleContext, bundleContext.createFilter(FILTER), null);
      this.bundleContext = bundleContext;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.util.tracker.ServiceTracker#open(boolean)
     */
    @Override
    public void open(boolean trackAllServices) {
      super.open(trackAllServices);
      try {
        ServiceReference[] references = bundleContext.getAllServiceReferences(null, FILTER);
        if (references != null) {
          for (ServiceReference ref : references) {
            addingService(ref);
          }
        }
      } catch (InvalidSyntaxException e) {
        throw new IllegalStateException("The tracker filter '" + FILTER + "' has syntax errors", e);
      }
    }

    @Override
    public Object addingService(ServiceReference reference) {
      String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      String servicePath = (String) reference.getProperty(RestConstants.SERVICE_PATH_PROPERTY);

      boolean publishFlag = Boolean.parseBoolean((String) reference.getProperty(RestConstants.SERVICE_PUBLISH_PROPERTY));
      boolean jobProducer = Boolean.parseBoolean(
          (String) reference.getProperty(RestConstants.SERVICE_JOBPRODUCER_PROPERTY)
      );

      // Only register services that have the "publish" flag set to "true"
      if (publishFlag) {
        try {
          registerService(serviceType, hostName, servicePath, jobProducer);
        } catch (ServiceRegistryException e) {
          logger.warn("Unable to register job producer of type " + serviceType + " on host " + hostName);
        }
      } else {
        logger.debug("Not registering service " + serviceType + " in service registry by configuration");
      }

      return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      boolean publishFlag = (Boolean) reference.getProperty(RestConstants.SERVICE_PUBLISH_PROPERTY);

      // Services that have the "publish" flag set to "true" have been registered before.
      if (publishFlag) {
        try {
          unRegisterService(serviceType, hostName);
        } catch (ServiceRegistryException e) {
          logger.warn("Unable to unregister job producer of type " + serviceType + " on host " + hostName);
        }
      } else {
        logger.trace("Service " + reference + " was never registered");
      }
      super.removedService(reference, service);
    }
  }

  /**
   * Sets the trusted http client.
   *
   * @param client
   *          the trusted http client
   */
  @Reference
  void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
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

  /** OSGi DI. */
  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy =  ReferencePolicy.DYNAMIC, unbind = "unsetIncidentService")
  public void setIncidentService(IncidentService incidentService) {
    // Manually resolve the cyclic dependency between the incident service and the service registry
    ((OsgiIncidentService) incidentService).setServiceRegistry(this);
    this.incidents = new Incidents(this, incidentService);
  }

  public void unsetIncidentService(IncidentService incidentService) {
    this.incidents = null;
  }

  /**
   * Update the jobs failure history and the service status with the given information. All these data are then use for
   * the jobs failover strategy. Only the terminated job (with FAILED or FINISHED status) are taken into account.
   *
   * @param job
   *          the current job that failed/succeeded
   * @throws ServiceRegistryException
   * @throws NotFoundException
   */
  private void updateServiceForFailover(JpaJob job) throws ServiceRegistryException, NotFoundException {
    if (job.getStatus() != Status.FAILED && job.getStatus() != Status.FINISHED) {
      return;
    }

    job.setStatus(job.getStatus(), job.getFailureReason());

    // At this point, the only possible states for the current service are NORMAL and WARNING,
    // the services in ERROR state will not be chosen by the dispatcher
    ServiceRegistrationJpaImpl currentService = job.getProcessorServiceRegistration();
    if (currentService == null) {
      return;
    }

    // Job is finished with a failure
    if (job.getStatus() == FAILED && !DATA.equals(job.getFailureReason())) {

      // Services in WARNING or ERROR state triggered by current job
      List<ServiceRegistrationJpaImpl> relatedWarningOrErrorServices = getRelatedWarningErrorServices(job);

      // Before this job failed there was at least one job failed with this job signature on any service
      if (relatedWarningOrErrorServices.size() > 0) {
        for (ServiceRegistrationJpaImpl relatedService : relatedWarningOrErrorServices) {
          // Skip current service from the list
          if (currentService.equals(relatedService)) {
            continue;
          }

          // De-escalate the state of related services as the issue is most likely with the job not the service
          // Reset the WARNING job to NORMAL
          if (relatedService.getServiceState() == WARNING) {
            logger.info("State reset to NORMAL for related service {} on host {}", relatedService.getServiceType(),
                    relatedService.getHost());
            relatedService.setServiceState(NORMAL, job.toJob().getSignature());
          }

          // Reset the ERROR job to WARNING
          else if (relatedService.getServiceState() == ERROR) {
            logger.info("State reset to WARNING for related service {} on host {}", relatedService.getServiceType(),
                    relatedService.getHost());
            relatedService.setServiceState(WARNING, relatedService.getWarningStateTrigger());
          }

          updateServiceState(relatedService);
        }
      }

      // This is the first job with this signature failing on any service
      else {
        // Set the current service to WARNING state
        if (currentService.getServiceState() == NORMAL) {
          logger.info("State set to WARNING for current service {} on host {}", currentService.getServiceType(),
                  currentService.getHost());
          currentService.setServiceState(WARNING, job.toJob().getSignature());
          updateServiceState(currentService);
        }

        // The current service already is in WARNING state and max attempts is reached
        else if (errorStatesEnabled && !noErrorStateServiceTypes.contains(currentService.getServiceType())
                && getHistorySize(currentService) >= maxAttemptsBeforeErrorState) {
          logger.info("State set to ERROR for current service {} on host {}", currentService.getServiceType(),
                  currentService.getHost());
          currentService.setServiceState(ERROR, job.toJob().getSignature());
          updateServiceState(currentService);
        }
      }
    }

    // Job is finished without failure
    else if (job.getStatus() == Status.FINISHED) {
      // If the service was in warning state reset to normal state
      if (currentService.getServiceState() == WARNING) {
        logger.info("State reset to NORMAL for current service {} on host {}", currentService.getServiceType(),
                currentService.getHost());
        currentService.setServiceState(NORMAL);
        updateServiceState(currentService);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#sanitize(java.lang.String, java.lang.String)
   */
  @Override
  public void sanitize(String serviceType, String host) throws NotFoundException {
    db.execChecked(em -> {
      ServiceRegistrationJpaImpl service = getServiceRegistrationQuery(serviceType, host).apply(em)
          .orElseThrow(NotFoundException::new);

      logger.info("State reset to NORMAL for service {} on host {} through sanitize method", service.getServiceType(),
          service.getHost());
      service.setServiceState(NORMAL);
      updateServiceState(service);
    });
  }

  /**
   * Gets the failed jobs history for the given service registration
   *
   * @param serviceRegistration
   * @return the failed jobs history size
   * @throws IllegalArgumentException
   *           if parameter is null
   * @throws ServiceRegistryException
   */
  private int getHistorySize(ServiceRegistration serviceRegistration) throws ServiceRegistryException {
    if (serviceRegistration == null) {
      throw new IllegalArgumentException("serviceRegistration must not be null!");
    }

    logger.debug("Calculating count of jobs who failed due to service {}", serviceRegistration);

    try {
      return db.exec(namedQuery.find(
          "Job.count.history.failed",
          Number.class,
          Pair.of("serviceType", serviceRegistration.getServiceType()),
          Pair.of("host", serviceRegistration.getHost())
      )).intValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * Gets the services in WARNING or ERROR state triggered by this job
   *
   * @param job
   *          the given job to get the related services
   * @return a list of services triggered by the job
   * @throws IllegalArgumentException
   *           if the given job was null
   * @throws ServiceRegistryException
   *           if the there was a problem with the query
   */
  private List<ServiceRegistrationJpaImpl> getRelatedWarningErrorServices(JpaJob job) throws ServiceRegistryException {
    if (job == null) {
      throw new IllegalArgumentException("job must not be null!");
    }

    logger.debug("Try to get the services in WARNING or ERROR state triggered by {} failed", job);

    try {
      return db.exec(namedQuery.findAll(
          "ServiceRegistration.relatedservices.warning_error",
          ServiceRegistrationJpaImpl.class,
          Pair.of("serviceType", job.getJobType())
      )).stream()
          // TODO: modify the query to avoid to go through the list here
          .filter(rs ->
              (rs.getServiceState() == WARNING && rs.getWarningStateTrigger() == job.toJob().getSignature())
              || (rs.getServiceState() == ERROR && rs.getErrorStateTrigger() == job.toJob().getSignature())
          ).collect(Collectors.toList());
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * Returns a filtered list of service registrations, containing only those that are online, not in maintenance mode,
   * and with a specific service type that are running on a host which is not already maxed out.
   *
   * @param serviceRegistrations
   *          the complete list of service registrations
   * @param hostRegistrations
   *          the complete list of available host registrations
   * @param systemLoad
   *          the map of hosts to the number of running jobs
   * @param jobType
   *          the job type for which the services registrations are filtered
   */
  protected List<ServiceRegistration> getServiceRegistrationsWithCapacity(String jobType,
          List<ServiceRegistration> serviceRegistrations, List<HostRegistration> hostRegistrations,
          final SystemLoad systemLoad) {
    final List<String> hostBaseUrls = hostRegistrations.stream()
                                                       .map(HostRegistration::getBaseUrl)
                                                       .collect(Collectors.toUnmodifiableList());
    final List<ServiceRegistration> filteredList = new ArrayList<>();

    for (ServiceRegistration service : serviceRegistrations) {
      // Skip service if host not available
      if (!hostBaseUrls.contains(service.getHost())) {
        logger.trace("Not considering {} because it's host {} is not available for dispatching", service,
                service.getHost());
        continue;
      }

      // Skip services that are not of the requested type
      if (!jobType.equals(service.getServiceType())) {
        logger.trace("Not considering {} because it is of the wrong job type", service);
        continue;
      }

      // Skip services that are in error state
      if (service.getServiceState() == ERROR) {
        logger.trace("Not considering {} because it is in error state", service);
        continue;
      }

      // Skip services that are in maintenance mode
      if (service.isInMaintenanceMode()) {
        logger.trace("Not considering {} because it is in maintenance mode", service);
        continue;
      }

      // Skip services that are marked as offline
      if (!service.isOnline()) {
        logger.trace("Not considering {} because it is currently offline", service);
        continue;
      }

      // Determine the maximum load for this host
      Float hostLoadMax = null;
      for (HostRegistration host : hostRegistrations) {
        if (host.getBaseUrl().equals(service.getHost())) {
          hostLoadMax = host.getMaxLoad();
          break;
        }
      }
      if (hostLoadMax == null) {
        logger.warn("Unable to determine max load for host {}", service.getHost());
      }

      // Determine the current load for this host
      Float hostLoad = systemLoad.get(service.getHost()).getLoadFactor();
      if (hostLoad == null) {
        logger.warn("Unable to determine current load for host {}", service.getHost());
      }

      // Is this host suited for processing?
      if (hostLoad == null || hostLoadMax == null || hostLoad < hostLoadMax) {
        logger.debug("Adding candidate service {} for processing of jobs of type '{}' (host load is {} of max {})",
           service, jobType, hostLoad, hostLoadMax);
        filteredList.add(service);
      }
    }

    // Sort the list by capacity
    filteredList.sort(new LoadComparator(systemLoad));

    return filteredList;
  }

  /**
   * Returns a filtered list of service registrations, containing only those that are online, not in maintenance mode,
   * and with a specific service type, ordered by load.
   *
   * @param jobType
   *          the job type for which the services registrations are filtered
   * @param serviceRegistrations
   *          the complete list of service registrations
   * @param hostRegistrations
   *          the complete list of available host registrations
   * @param systemLoad
   *
   */
  protected List<ServiceRegistration> getServiceRegistrationsByLoad(String jobType,
          List<ServiceRegistration> serviceRegistrations, List<HostRegistration> hostRegistrations,
          final SystemLoad systemLoad) {
    final List<String> hostBaseUrls = hostRegistrations.stream()
                                                       .map(HostRegistration::getBaseUrl)
                                                       .collect(Collectors.toUnmodifiableList());
    final List<ServiceRegistration> filteredList = new ArrayList<>();

    logger.debug("Finding services to dispatch job of type {}", jobType);

    for (ServiceRegistration service : serviceRegistrations) {
      // Skip service if host not available
      if (!hostBaseUrls.contains(service.getHost())) {
        logger.trace("Not considering {} because it's host {} is not available for dispatching", service,
                service.getHost());
        continue;
      }

      // Skip services that are not of the requested type
      if (!jobType.equals(service.getServiceType())) {
        logger.trace("Not considering {} because it is of the wrong job type", service);
        continue;
      }

      // Skip services that are in error state
      if (service.getServiceState() == ERROR) {
        logger.trace("Not considering {} because it is in error state", service);
        continue;
      }

      // Skip services that are in maintenance mode
      if (service.isInMaintenanceMode()) {
        logger.trace("Not considering {} because it is in maintenance mode", service);
        continue;
      }

      // Skip services that are marked as offline
      if (!service.isOnline()) {
        logger.trace("Not considering {} because it is currently offline", service);
        continue;
      }

      // We found a candidate service
      logger.debug("Adding candidate service {} for processing of job of type '{}'", service, jobType);
      filteredList.add(service);
    }

    // Sort the list by capacity and distinguish between composer jobs and other jobs
    if ("org.opencastproject.composer".equals(jobType))
      Collections.sort(filteredList, new LoadComparatorEncoding(systemLoad));
    else
      Collections.sort(filteredList, new LoadComparator(systemLoad));

    return filteredList;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxLoads()
   */
  @Override
  public SystemLoad getMaxLoads() throws ServiceRegistryException {
    final SystemLoad loads = new SystemLoad();
    getHostRegistrations().stream()
        .map(host -> new NodeLoad(host.getBaseUrl(), 0.0f, host.getMaxLoad()))
        .forEach(loads::addNodeLoad);
    return loads;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxLoadOnNode(java.lang.String)
   */
  @Override
  public NodeLoad getMaxLoadOnNode(String host) throws ServiceRegistryException, NotFoundException {
    try {
      float maxLoad = db.exec(namedQuery.find(
          "HostRegistration.getMaxLoadByHostName",
          Number.class,
          Pair.of("host", host)
      )).floatValue();
      return new NodeLoad(host, 0.0f, maxLoad);
    } catch (NoResultException e) {
      throw new NotFoundException(e);
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /** A periodic check on each service registration to ensure that it is still alive. */
  class JobProducerHeartbeat implements Runnable {

    /** List of service registrations that have been found unresponsive last time we checked */
    private final List<ServiceRegistration> unresponsive = new ArrayList<>();

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      logger.debug("Checking for unresponsive services");

      try {
        List<ServiceRegistration> serviceRegistrations = getOnlineServiceRegistrations();

        for (ServiceRegistration service : serviceRegistrations) {
          hostsStatistics.updateHost(((ServiceRegistrationJpaImpl) service).getHostRegistration());
          servicesStatistics.updateService(service);
          if (!service.isJobProducer()) {
            continue;
          }
          if (service.isInMaintenanceMode()) {
            continue;
          }

          // We think this service is online and available. Prove it.
          String serviceUrl = UrlSupport.concat(service.getHost(), service.getPath(), "dispatch");

          HttpHead options = new HttpHead(serviceUrl);
          HttpResponse response = null;
          try {
            try {
              response = client.execute(options);
              if (response != null) {
                switch (response.getStatusLine().getStatusCode()) {
                  case HttpStatus.SC_OK:
                    // this service is reachable, continue checking other services
                    logger.trace("Service " + service + " is responsive: " + response.getStatusLine());
                    if (unresponsive.remove(service)) {
                      logger.info("Service {} is still online", service);
                    } else if (!service.isOnline()) {
                      try {
                        setOnlineStatus(service.getServiceType(), service.getHost(), service.getPath(), true, true);
                        logger.info("Service {} is back online", service);
                      } catch (ServiceRegistryException e) {
                        logger.warn("Error setting online status for {}", service);
                      }
                    }
                    continue;
                  default:
                    if (!service.isOnline()) {
                      continue;
                    }
                    logger.warn("Service {} is not working as expected: {}", service, response.getStatusLine());
                }
              } else {
                logger.warn("Service {} does not respond", service);
              }
            } catch (TrustedHttpClientException e) {
              if (!service.isOnline()) {
                continue;
              }
              logger.warn("Unable to reach {}", service, e);
            }

            // If we get here, the service did not respond as expected
            try {
              if (unresponsive.contains(service)) {
                unRegisterService(service.getServiceType(), service.getHost());
                unresponsive.remove(service);
                logger.warn("Marking {} as offline", service);
              } else {
                unresponsive.add(service);
                logger.warn("Added {} to the watch list", service);
              }
            } catch (ServiceRegistryException e) {
              logger.warn("Unable to unregister unreachable service: {}", service, e);
            }
          } finally {
            client.close(response);
          }
        }
      } catch (Throwable t) {
        logger.warn("Error while checking for unresponsive services", t);
      }

      logger.debug("Finished checking for unresponsive services");
    }
  }

  /**
   * Comparator that will sort service registrations depending on their capacity, wich is defined by the number of jobs
   * the service's host is already running divided by the MaxLoad of the Server. The lower that number, the bigger the capacity.
   */
  private class LoadComparator implements Comparator<ServiceRegistration> {

    protected SystemLoad loadByHost = null;

    /**
     * Creates a new comparator which is using the given map of host names and loads.
     *
     * @param loadByHost
     *          the current work load by host
     */
    LoadComparator(SystemLoad loadByHost) {
      this.loadByHost = loadByHost;
    }

    @Override
    public int compare(ServiceRegistration serviceA, ServiceRegistration serviceB) {
      String hostA = serviceA.getHost();
      String hostB = serviceB.getHost();
      NodeLoad nodeA = loadByHost.get(hostA);
      NodeLoad nodeB = loadByHost.get(hostB);
      // If the load factors are about the same, sort based on maximum load
      if (Math.abs(nodeA.getLoadFactor() - nodeB.getLoadFactor()) <= 0.01) {
        // NOTE: The sort order below is *reversed* from what you'd expect
        // When we're comparing the load factors we want the node with the lowest factor to be first
        // When we're comparing the maximum load value, we want the node with the highest max to be first
        return Float.compare(nodeB.getMaxLoad(), nodeA.getMaxLoad());
      }
      return Float.compare(nodeA.getLoadFactor(), nodeB.getLoadFactor());
    }
  }

  /**
   * Comparator that will sort service registrations depending on their capacity, which is defined by the number of jobs
   * the service's host is already running divided by the MaxLoad of the Server. The lower that number, the bigger the capacity.
   * This Comparator will preferre encoding workers, if none are defined in the configuration file it will act like the LoadComparator.
   */
  private class LoadComparatorEncoding extends LoadComparator implements Comparator<ServiceRegistration> {

    /**
     * Creates a new comparator which is using the given map of host names and loads.
     *
     * @param loadByHost
     */
    LoadComparatorEncoding(SystemLoad loadByHost) {
      super(loadByHost);
    }

    @Override
    public int compare(ServiceRegistration serviceA, ServiceRegistration serviceB) {
      String hostA = serviceA.getHost();
      String hostB = serviceB.getHost();
      NodeLoad nodeA = loadByHost.get(hostA);
      NodeLoad nodeB = loadByHost.get(hostB);

      if (encodingWorkers != null) {
        if (encodingWorkers.contains(hostA) && !encodingWorkers.contains(hostB)) {
          if (nodeA.getLoadFactor() <= encodingThreshold) {
            return -1;
          }
          return Float.compare(nodeA.getLoadFactor(), nodeB.getLoadFactor());
        }
        if (encodingWorkers.contains(hostB) && !encodingWorkers.contains(hostA)) {
          if (nodeB.getLoadFactor() <= encodingThreshold) {
            return 1;
          }
          return Float.compare(nodeA.getLoadFactor(), nodeB.getLoadFactor());
        }
      }
        return super.compare(serviceA, serviceB);
    }
  }
}
