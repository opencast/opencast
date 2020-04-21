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

package org.opencastproject.serviceregistry.impl;

import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.opencastproject.job.api.AbstractJobProducer.ACCEPT_JOB_LOADS_EXCEEDING_PROPERTY;
import static org.opencastproject.job.api.AbstractJobProducer.DEFAULT_ACCEPT_JOB_LOADS_EXCEEDING;
import static org.opencastproject.job.api.Job.FailureReason.DATA;
import static org.opencastproject.job.api.Job.Status.FAILED;
import static org.opencastproject.job.jpa.JpaJob.fnToJob;
import static org.opencastproject.security.api.SecurityConstants.ORGANIZATION_HEADER;
import static org.opencastproject.security.api.SecurityConstants.USER_HEADER;
import static org.opencastproject.serviceregistry.api.ServiceState.ERROR;
import static org.opencastproject.serviceregistry.api.ServiceState.NORMAL;
import static org.opencastproject.serviceregistry.api.ServiceState.WARNING;
import static org.opencastproject.util.OsgiUtil.getOptContextProperty;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.HostRegistration;
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
import org.opencastproject.util.jmx.JmxUtil;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectInstance;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

/** JPA implementation of the {@link ServiceRegistry} */
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
  protected List<ObjectInstance> jmxBeans = new ArrayList<ObjectInstance>();

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
  private static final ThreadLocal<Job> currentJob = new ThreadLocal<Job>();

  /** Configuration key for the maximum load */
  protected static final String OPT_MAXLOAD = "org.opencastproject.server.maxload";

  /** Configuration key for the dispatch interval in milliseconds */
  protected static final String OPT_DISPATCHINTERVAL = "dispatchinterval";

  /** Configuration key for the interval to check whether the hosts in the service registry are still alive [sec] * */
  protected static final String OPT_HEARTBEATINTERVAL = "heartbeat.interval";

  /** Configuration key for the collection of job statistics */
  protected static final String OPT_JOBSTATISTICS = "jobstats.collect";

  /** Configuration key for the retrieval of service statistics: Do not consider jobs older than max_job_age (in days) */
  protected static final String OPT_SERVICE_STATISTICS_MAX_JOB_AGE = "org.opencastproject.statistics.services.max_job_age";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Minimum delay between job dispatching attempts, in milliseconds */
  static final long MIN_DISPATCH_INTERVAL = 1000;

  /** Default delay between job dispatching attempts, in milliseconds */
  static final long DEFAULT_DISPATCH_INTERVAL = 5000;

  /** Default delay before starting job dispatching, in milliseconds */
  static final long DEFAULT_DISPATCH_START_DELAY = 60000;

  /** Default jobs limit during dispatching
   * (larger value will fetch more entries from the database at the same time and increase RAM usage) */
  static final int DEFAULT_DISPATCH_JOBS_LIMIT = 100;

  /** Default setting on job statistics collection */
  static final boolean DEFAULT_JOB_STATISTICS = true;

  /** Default setting on service statistics retrieval */
  static final int DEFAULT_SERVICE_STATISTICS_MAX_JOB_AGE = 14;

  /** Default value for {@link #maxAttemptsBeforeErrorState} */
  private static final int MAX_FAILURE_BEFORE_ERROR_STATE = 1;

  /** The configuration key for setting {@link #maxAttemptsBeforeErrorState} */
  private static final String MAX_ATTEMPTS_CONFIG_KEY = "max.attempts";

  /** Number of failed jobs on a service before to set it in error state */
  protected int maxAttemptsBeforeErrorState = MAX_FAILURE_BEFORE_ERROR_STATE;

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

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** Tracks services published locally and adds them to the service registry */
  protected RestServiceTracker tracker = null;

  /** The thread pool to use for dispatching queued jobs and checking on phantom services. */
  protected ScheduledExecutorService scheduledExecutor = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  protected Incidents incidents;

  /** Whether to collect detailed job statistics */
  protected boolean collectJobstats = DEFAULT_JOB_STATISTICS;

  /** Maximum age of jobs being considering for service statistics */
  protected int maxJobAge = DEFAULT_SERVICE_STATISTICS_MAX_JOB_AGE;

  /** A static list of statuses that influence how load balancing is calculated */
  protected static final List<Status> JOB_STATUSES_INFLUENCING_LOAD_BALANCING;

  protected static HashMap<Long, Float> jobCache = new HashMap<Long, Float>();

  static {
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING = new ArrayList<Status>();
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.RUNNING);
  }

  /** The dispatcher priority list */
  protected final Map<Long, String> dispatchPriorityList = new HashMap<>();

  /** Whether to accept a job whose load exceeds the host’s max load */
  protected Boolean acceptJobLoadsExeedingMaxLoad = true;

  // Current system load
  protected float localSystemLoad = 0.0f;

  /** OSGi DI */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  public void activate(ComponentContext cc) {
    logger.info("Activate service registry");

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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String, Float)
   */
  @Override
  public Job createJob(String type, String operation, Float jobLoad) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, null, null, true, getCurrentJob(), jobLoad);
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
   *      java.util.List, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload)
          throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, true, getCurrentJob(), DEFAULT_JOB_LOAD);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, java.lang.String, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, Float jobLoad)
          throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, true, getCurrentJob(), jobLoad);
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

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(String, String, List, String, boolean, Job)
   */
  @Override
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
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      ServiceRegistrationJpaImpl creatingService = getServiceRegistration(em, serviceType, host);
      if (creatingService == null) {
        throw new ServiceRegistryException("No service registration exists for type '" + serviceType + "' on host '"
                + host + "'");
      }
      if (creatingService.getHostRegistration().isMaintenanceMode()) {
        logger.warn("Creating a job from {}, which is currently in maintenance mode.", creatingService.getHost());
      } else if (!creatingService.getHostRegistration().isActive()) {
        logger.warn("Creating a job from {}, which is currently inactive.", creatingService.getHost());
      }

      User currentUser = securityService.getUser();
      Organization currentOrganization = securityService.getOrganization();

      JpaJob jpaJob = new JpaJob(currentUser, currentOrganization, creatingService, operation, arguments, payload,
              dispatchable, jobLoad);

      // Bind the given parent job to the new job
      if (parentJob != null) {

        // Get the JPA instance of the parent job
        JpaJob jpaParentJob;
        try {
          jpaParentJob = getJpaJob(parentJob.getId());
        } catch (NotFoundException e) {
          logger.error("{} not found in the persistence context", parentJob);
          // We don't want to leave the deleted job in the cache if there
          removeFromLoadCache(parentJob.getId());
          throw new ServiceRegistryException(e);
        }

        jpaJob.setParentJob(jpaParentJob);

        // Get the JPA instance of the root job
        JpaJob jpaRootJob = jpaParentJob;
        if (parentJob.getRootJobId() != null) {
          try {
            jpaRootJob = getJpaJob(parentJob.getRootJobId());
          } catch (NotFoundException e) {
            logger.error("job with id {} not found in the persistence context", parentJob.getRootJobId());
            // We don't want to leave the deleted job in the cache if there
            removeFromLoadCache(parentJob.getId());
            throw new ServiceRegistryException(e);
          }
        }
        jpaJob.setRootJob(jpaRootJob);
      }

      // if this job is not dispatchable, it must be handled by the host that has created it
      if (dispatchable) {
        logger.trace("Queuing dispatchable '{}'", jpaJob);
        jpaJob.setStatus(Status.QUEUED);
      } else {
        logger.trace("Giving new non-dispatchable '{}' its creating service as processor '{}'", jpaJob, creatingService);
        jpaJob.setProcessorServiceRegistration(creatingService);
      }

      em.persist(jpaJob);
      tx.commit();

      setJobUri(jpaJob);
      Job job = jpaJob.toJob();
      return job;
    } catch (RollbackException e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void removeJobs(List<Long> jobIds) throws NotFoundException, ServiceRegistryException {

    for (long jobId: jobIds) {
      if (jobId < 1)
        throw new NotFoundException("Job ID must be greater than zero (0)");
    }

    logger.debug("Start deleting jobs with IDs '{}'", jobIds);

    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();

      for (long jobId: jobIds) {
        JpaJob job = em.find(JpaJob.class, jobId);
        if (job == null) {
          logger.error("Job with Id {} cannot be deleted: Not found.", jobId);
          tx.rollback();
          removeFromLoadCache(jobId);
          throw new NotFoundException("Job with ID '" + jobId + "' not found");
        }
        deleteChildJobs(em, tx, jobId);
        em.remove(job);
        removeFromLoadCache(jobId);
      }

      tx.commit();
      logger.info("Jobs with IDs '{}' deleted", jobIds);
    } finally {
      if (em != null)
        em.close();
    }
  }

  private void deleteChildJobs(EntityManager em, EntityTransaction tx, long jobId) throws ServiceRegistryException {
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
      logger.error("Unable to remove child jobs from {}: {}", jobId, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    }
  }

  @Override
  public void removeParentlessJobs(int lifetime) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;

    Date d = DateUtils.addDays(new Date(), -lifetime);
    int count = 0;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      TypedQuery<JpaJob> query = em.createNamedQuery("Job.withoutParent", JpaJob.class);
      List<JpaJob> jobs = query.getResultList();

      tx.begin();
      for (JpaJob jpaJob : jobs) {
        Job job = jpaJob.toJob();
        if (job.getDateCreated().after(d))
          continue;

        // DO NOT DELETE workflow instances and operations!
        if (START_OPERATION.equals(job.getOperation()) || START_WORKFLOW.equals(job.getOperation())
                || RESUME.equals(job.getOperation()))
          continue;

        if (job.getStatus().isTerminated()) {
          try {
            removeJobs(Collections.singletonList(job.getId()));
            logger.debug("Parentless '{}' removed", job);
            count++;
          } catch (NotFoundException e) {
            logger.debug("Parentless '{} ' not found in database: {}", job, e);
          }
        }

      }
      tx.commit();
      if (count > 0)
        logger.info("Successfully removed {} parentless jobs", count);
      else
        logger.trace("No parentless jobs found to remove");
    } finally {
      if (em != null)
        em.close();
    }
    return;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary properties) throws ConfigurationException {

    logger.info("Updating service registry properties");

    String maxAttempts = StringUtils.trimToNull((String) properties.get(MAX_ATTEMPTS_CONFIG_KEY));
    if (maxAttempts != null) {
      try {
        maxAttemptsBeforeErrorState = Integer.parseInt(maxAttempts);
        logger.info("Set max attempts before error state to {}", maxAttempts);
      } catch (NumberFormatException e) {
        logger.warn("Can not set max attempts before error state to {}. {} must be an integer", maxAttempts,
                MAX_ATTEMPTS_CONFIG_KEY);
      }
    }

    long dispatchInterval = DEFAULT_DISPATCH_INTERVAL;
    String dispatchIntervalString = StringUtils.trimToNull((String) properties.get(OPT_DISPATCHINTERVAL));
    if (StringUtils.isNotBlank(dispatchIntervalString)) {
      try {
        dispatchInterval = Long.parseLong(dispatchIntervalString);
      } catch (Exception e) {
        logger.warn("Dispatch interval '{}' is malformed, setting to {}", dispatchIntervalString, MIN_DISPATCH_INTERVAL);
        dispatchInterval = MIN_DISPATCH_INTERVAL;
      }
      if (dispatchInterval == 0) {
        logger.info("Dispatching disabled");
      } else if (dispatchInterval < MIN_DISPATCH_INTERVAL) {
        logger.warn("Dispatch interval {} ms too low, adjusting to {}", dispatchInterval, MIN_DISPATCH_INTERVAL);
        dispatchInterval = MIN_DISPATCH_INTERVAL;
      } else {
        logger.info("Dispatch interval set to {} ms", dispatchInterval);
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
        collectJobstats = Boolean.valueOf(jobStatsString);
      } catch (Exception e) {
        logger.warn("Job statistics collection flag '{}' is malformed, setting to {}", jobStatsString,
                DEFAULT_JOB_STATISTICS);
        collectJobstats = DEFAULT_JOB_STATISTICS;
      }
    }

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

    long dispatchDelay = DEFAULT_DISPATCH_START_DELAY;

    // Stop the current scheduled executors so we can configure new ones
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdown();
      dispatchDelay = dispatchInterval;
    }

    scheduledExecutor = Executors.newScheduledThreadPool(2);

    // Schedule the service heartbeat if the interval is > 0
    if (heartbeatInterval > 0) {
      logger.debug("Starting service heartbeat at a custom interval of {}s", heartbeatInterval);
      scheduledExecutor.scheduleWithFixedDelay(new JobProducerHeartbeat(), heartbeatInterval, heartbeatInterval,
              TimeUnit.SECONDS);
    }

    // Schedule the job dispatching.
    if (dispatchInterval > 0) {
      logger.debug("Starting job dispatching at a custom interval of {}s", dispatchInterval / 1000);
      scheduledExecutor.scheduleWithFixedDelay(new JobDispatcher(), dispatchDelay, dispatchInterval,
              TimeUnit.MILLISECONDS);
    }
  }

  /**
   * OSGI callback when the configuration is updated. This method is only here to prevent the
   * configuration admin service from calling the service deactivate and activate methods
   * for a config update. It does not have to do anything as the updates are handled by updated().
   */
  public void modified(Map<String, Object> config)
     throws ConfigurationException {
    logger.debug("Modified serviceregistry");
  }

  private JpaJob getJpaJob(long id) throws NotFoundException, ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      JpaJob jpaJob = em.find(JpaJob.class, id);
      if (jpaJob == null) {
        throw new NotFoundException("Job " + id + " not found");
      }
      // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
      // this row in the database
      em.refresh(jpaJob);
      setJobUri(jpaJob);
      return jpaJob;
    } catch (Exception e) {
      if (e instanceof NotFoundException) {
        throw (NotFoundException) e;
      } else {
        throw new ServiceRegistryException(e);
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    return getJpaJob(id).toJob();
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

  private JpaJob updateJob(JpaJob job) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Job oldJob = getJob(job.getId());
      JpaJob jpaJob = updateInternal(em, job);
      if (!TYPE_WORKFLOW.equals(job.getJobType()) && job.getJobLoad() > 0.0f
              && job.getProcessorServiceRegistration() != null
              && job.getProcessorServiceRegistration().getHost().equals(getRegistryHostname())) {
        processCachedLoadChange(job);
      }

      // All WorkflowService Jobs will be ignored
      if (oldJob.getStatus() != job.getStatus() && !TYPE_WORKFLOW.equals(job.getJobType())) {
        updateServiceForFailover(em, job);
      }

      return jpaJob;
    } catch (PersistenceException e) {
      throw new ServiceRegistryException(e);
    } catch (NotFoundException e) {
      // Just in case, remove from cache if there
      removeFromLoadCache(job.getId());
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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

    if (jobCache.isEmpty() && Math.abs(localSystemLoad) > 0.01f) {
      logger.warn("No jobs in the job load cache, but load is {}: setting job load to 0",
              format("%.2f", localSystemLoad));
      localSystemLoad = 0;
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

  private Fn<JpaJob, JpaJob> fnSetJobUri() {
    return new Fn<JpaJob, JpaJob>() {
      @Override
      public JpaJob apply(JpaJob job) {
        return setJobUri(job);
      }
    };
  }

  /**
   * Internal method to update a job, throwing unwrapped JPA exceptions.
   *
   * @param em
   *          the current entity manager
   * @param job
   *          the job to update
   * @return the updated job
   * @throws PersistenceException
   *           if there is an exception thrown while persisting the job via JPA
   * @throws IllegalArgumentException
   */
  protected JpaJob updateInternal(EntityManager em, JpaJob job) throws PersistenceException {
    EntityTransaction tx = em.getTransaction();
    JpaJob fromDb = null;
    try {
      tx.begin();
      fromDb = em.find(JpaJob.class, job.getId());
      if (fromDb == null) {
        throw new NoResultException();
      }
      update(fromDb, job);

      em.merge(fromDb);
      tx.commit();
      job.setVersion(fromDb.toJob().getVersion());
      setJobUri(job);
      return job;
    } catch (PersistenceException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  private void dumpJobs(JpaJob originalJob, JpaJob fromDb) {
    try {
      if (originalJob == null) {
        logger.error("originalJob is null");
        return;
      }
      if (originalJob.getStatus() == null) {
        logger.error("originalJob.getStatus() is null");
        return;
      }
      if (!originalJob.getStatus().equals(fromDb.getStatus()))
        logger.error("JPA status mismatch: " + originalJob.getStatus() + " vs " + fromDb.getStatus());
      if (originalJob.getProcessorServiceRegistration() == null) {
        logger.error("originalJob.getProcessorServiceRegistration() is null");
        return;
      }
      if (fromDb.getProcessorServiceRegistration() == null) {
        logger.error("fromDb.getProcessorServiceRegistration() is null");
        return;
      }
      if (!originalJob.getProcessorServiceRegistration().getId().equals(fromDb.getProcessorServiceRegistration().getId()))
        logger.error("JPA processor service mismatch: " + originalJob.getProcessorServiceRegistration().getId() + " vs " + fromDb.getProcessorServiceRegistration().getId());
      if (!originalJob.getDateStarted().equals(fromDb.getDateStarted()))
        logger.error("JPA date started mismatch: " + originalJob.getDateStarted() + " vs " + fromDb.getDateStarted());
      if (!originalJob.getChildJobsString().equals(fromDb.getChildJobsString()))
        logger.error("JPA child job id mismatch: " + originalJob.getChildJobsString() + " vs " + fromDb.getChildJobsString());
    } catch (Exception e) {
      logger.error("Error logging job state information in dumpJobs()", e);
    }
  }

  /**
   * Internal method to update the service registration state, throwing unwrapped JPA exceptions.
   *
   * @param em
   *          the current entity manager
   * @param registration
   *          the service registration to update
   * @return the updated service registration
   * @throws PersistenceException
   *           if there is an exception thrown while persisting the job via JPA
   * @throws IllegalArgumentException
   */
  private ServiceRegistration updateServiceState(EntityManager em, ServiceRegistrationJpaImpl registration)
          throws PersistenceException {
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationJpaImpl fromDb;
      fromDb = em.find(ServiceRegistrationJpaImpl.class, registration.getId());
      if (fromDb == null) {
        throw new NoResultException();
      }
      fromDb.setServiceState(registration.getServiceState());
      fromDb.setStateChanged(registration.getStateChanged());
      fromDb.setWarningStateTrigger(registration.getWarningStateTrigger());
      fromDb.setErrorStateTrigger(registration.getErrorStateTrigger());
      tx.commit();
      servicesStatistics.updateService(registration);
      return registration;
    } catch (PersistenceException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
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
   * @param em
   *          an active entity manager
   * @param host
   *          the host name
   * @return the host registration, or null if none exists
   */
  protected HostRegistrationJpaImpl fetchHostRegistration(EntityManager em, String host) {
    Query query = em.createNamedQuery("HostRegistration.byHostName");
    query.setParameter("host", host);
    try {
      return (HostRegistrationJpaImpl) query.getSingleResult();
    } catch (NoResultException e) {
      logger.debug("No existing host registration for {}", host);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(String, String, String, long, int, float)
   */
  @Override
  public void registerHost(String host, String address, String nodeName, long memory, int cores, float maxLoad)
          throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Find the existing registrations for this host and if it exists, update it
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, host);
      if (hostRegistration == null) {
        hostRegistration = new HostRegistrationJpaImpl(host, address, nodeName, memory, cores, maxLoad, true, false);
        em.persist(hostRegistration);
      } else {
        hostRegistration.setIpAddress(address);
        hostRegistration.setMemory(memory);
        hostRegistration.setCores(cores);
        hostRegistration.setMaxLoad(maxLoad);
        hostRegistration.setOnline(true);
        hostRegistration.setNodeName(nodeName);
        em.merge(hostRegistration);
      }
      logger.info("Registering {} with a maximum load of {}", host, maxLoad);
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unregisterHost(java.lang.String)
   */
  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl existingHostRegistration = fetchHostRegistration(em, host);
      if (existingHostRegistration == null) {
        throw new IllegalArgumentException("Host '" + host + "' is not registered, so it can not be unregistered");
      }
      existingHostRegistration.setOnline(false);
      for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
        unRegisterService(serviceRegistration.getServiceType(), serviceRegistration.getHost());
      }
      em.merge(existingHostRegistration);
      logger.info("Unregistering {}", host);
      tx.commit();
      logger.info("Host {} unregistered", host);
      hostsStatistics.updateHost(existingHostRegistration);
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#enableHost(String)
   */
  @Override
  public void enableHost(String host) throws ServiceRegistryException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Find the existing registrations for this host and if it exists, update it
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, host);
      if (hostRegistration == null) {
        throw new NotFoundException("Host '" + host + "' is currently not registered, so it can not be enabled");
      } else {
        hostRegistration.setActive(true);
        em.merge(hostRegistration);
      }
      logger.info("Enabling {}", host);
      tx.commit();
      tx.begin();
      for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
        ServiceRegistrationJpaImpl registration = (ServiceRegistrationJpaImpl) serviceRegistration;
        registration.setActive(true);
        em.merge(registration);
        servicesStatistics.updateService(registration);
      }
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#disableHost(String)
   */
  @Override
  public void disableHost(String host) throws ServiceRegistryException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, host);
      if (hostRegistration == null) {
        throw new NotFoundException("Host '" + host + "' is not currently registered, so it can not be disabled");
      } else {
        hostRegistration.setActive(false);
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          ServiceRegistrationJpaImpl registration = (ServiceRegistrationJpaImpl) serviceRegistration;
          registration.setActive(false);
          em.merge(registration);
          servicesStatistics.updateService(registration);
        }
        em.merge(hostRegistration);
      }
      logger.info("Disabling {}", host);
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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

  protected ServiceRegistrationJpaImpl getServiceRegistration(EntityManager em, String serviceType, String host) {
    try {
      Query q = em.createNamedQuery("ServiceRegistration.getRegistration");
      q.setParameter("serviceType", serviceType);
      q.setParameter("host", host);
      logger.trace("Looking for: host {} service {}", host, serviceType);
      return (ServiceRegistrationJpaImpl) q.getSingleResult();
    } catch (NoResultException e) {
      logger.debug("Not Found: host {} service {}", host, serviceType);
      return null;
    }
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
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, baseUrl);
      if (hostRegistration == null) {
        logger.info("No associated host registration for '{}' or service '{}' (path '{}')", baseUrl, serviceType,path);
        throw new IllegalStateException(
                "A service registration can not be updated when it has no associated host registration");
      }
      ServiceRegistrationJpaImpl registration = getServiceRegistration(em, serviceType, baseUrl);
      if (registration == null) {
        if (isBlank(path)) {
          // we can not create a new registration without a path
          throw new IllegalArgumentException("path must not be blank when registering new services");
        }
        if (jobProducer == null) { // if we are not provided a value, consider it to be false
          registration = new ServiceRegistrationJpaImpl(hostRegistration, serviceType, path, false);

        } else {
          registration = new ServiceRegistrationJpaImpl(hostRegistration, serviceType, path, jobProducer);
        }
        em.persist(registration);
      } else {
        if (StringUtils.isNotBlank(path))
          registration.setPath(path);
        registration.setOnline(online);
        if (jobProducer != null) { // if we are not provided a value, don't update the persistent value
          registration.setJobProducer(jobProducer);
        }
        em.merge(registration);
      }
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
      servicesStatistics.updateService(registration);
      return registration;
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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
    setOnlineStatus(serviceType, baseUrl, null, false, null);

    cleanRunningJobs(serviceType, baseUrl);
  }

  /** Find all undispatchable jobs that were orphaned when this host was last deactivated and set them to CANCELED. */
  private void cleanUndispatchableJobs(String hostName) {
    EntityManager em = null;
    EntityTransaction tx = null;
    logger.debug("Starting check for undispatchable jobs for host {}", hostName);
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Query query = em.createNamedQuery("Job.undispatchable.status");
      List<Integer> statuses = new ArrayList<Integer>();
      statuses.add(Status.INSTANTIATED.ordinal());
      statuses.add(Status.RUNNING.ordinal());
      query.setParameter("statuses", statuses);
      @SuppressWarnings("unchecked")
      List<JpaJob> undispatchableJobs = query.getResultList();
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
          logger.debug("Will not cancel undispatchable job {} for host {}, it is running on a different host ({})", job, hostName, jobHost);

        } else {
          logger.info("Cancelling the running undispatchable job {}, it was orphaned on this host ({})", job, hostName);
          job.setStatus(Status.CANCELED);
          em.merge(job);
        }
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Unable to clean undispatchable jobs for host {}! {}", hostName, e.getMessage());
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Find all running jobs on this service and set them to RESET or CANCELED.
   *
   * @param serviceType
   *          the service type
   * @param baseUrl
   *          the base url
   * @throws ServiceRegistryException
   *           if there is a problem communicating with the jobs database
   */
  private void cleanRunningJobs(String serviceType, String baseUrl) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      TypedQuery<JpaJob> query = em.createNamedQuery("Job.processinghost.status", JpaJob.class);
      query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
      List<Integer> statuses = new ArrayList<Integer>();
      statuses.add(Status.RUNNING.ordinal());
      statuses.add(Status.DISPATCHING.ordinal());
      statuses.add(Status.WAITING.ordinal());
      query.setParameter("statuses", statuses);
      query.setParameter("host", baseUrl);
      query.setParameter("serviceType", serviceType);

      List<JpaJob> unregisteredJobs = query.getResultList();
      if (unregisteredJobs.size() > 0) {
        logger.info("Found {} jobs to clean for {}@{}", unregisteredJobs.size(), serviceType, baseUrl);
      }
      for (JpaJob job : unregisteredJobs) {
        if (job.isDispatchable()) {
          em.refresh(job);
          // If this job has already been treated
          if (Status.CANCELED.equals(job.getStatus()) || Status.RESTART.equals(job.getStatus()))
            continue;
          if (job.getRootJob() != null && Status.PAUSED.equals(job.getRootJob().getStatus())) {
            JpaJob rootJob = job.getRootJob();
            cancelAllChildren(rootJob, em);
            rootJob.setStatus(Status.RESTART);
            rootJob.setOperation(START_OPERATION);
            em.merge(rootJob);
            continue;
          }

          logger.info("Marking child jobs from {} as canceled", job);
          cancelAllChildren(job, em);
          logger.info("Rescheduling lost {}", job);
          job.setStatus(Status.RESTART);
          job.setProcessorServiceRegistration(null);
        } else {
          logger.info("Marking lost {} as failed", job);
          job.setStatus(Status.FAILED);
        }
        em.merge(job);
      }
      tx.commit();
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Go through all the children recursively to set them in {@link Status#CANCELED} status
   *
   * @param job
   *          the parent job
   * @param em
   *          the entity manager
   */
  private void cancelAllChildren(JpaJob job, EntityManager em) {
    for (JpaJob child : job.getChildJobs()) {
      em.refresh(child);
      if (Status.CANCELED.equals(job.getStatus()))
        continue;
      cancelAllChildren(child, em);
      child.setStatus(Status.CANCELED);
      em.merge(child);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String baseUrl, boolean maintenance) throws NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    logger.info("Setting maintenance mode on host '{}'", baseUrl);
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl reg = fetchHostRegistration(em, baseUrl);
      if (reg == null) {
        logger.warn("Can not set maintenance mode because host '{}' was not registered", baseUrl);
        throw new NotFoundException("Can not set maintenance mode on a host that has not been registered");
      }
      reg.setMaintenanceMode(maintenance);
      em.merge(reg);
      tx.commit();
      hostsStatistics.updateHost(reg);
      logger.info("Finished setting maintenance mode on host '{}'", baseUrl);
    } catch (RollbackException e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getServiceRegistrations(em);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Incidents incident() {
    return incidents;
  }

  @SuppressWarnings("unchecked")
  private List<ServiceRegistration> getOnlineServiceRegistrations() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return em.createNamedQuery("ServiceRegistration.getAllOnline").getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets all service registrations.
   *
   * @param em
   *          the current entity manager
   * @return the list of service registrations
   */
  @SuppressWarnings("unchecked")
  protected List<ServiceRegistration> getServiceRegistrations(EntityManager em) {
    return em.createNamedQuery("ServiceRegistration.getAll").getResultList();
  }

  /**
   * Gets all host registrations
   *
   * @return the list of host registrations
   */
  @Override
  public List<HostRegistration> getHostRegistrations() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getHostRegistrations(em);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets all host registrations
   *
   * @param em
   *          the current entity manager
   * @return the list of host registrations
   */
  @SuppressWarnings("unchecked")
  protected List<HostRegistration> getHostRegistrations(EntityManager em) {
    return em.createNamedQuery("HostRegistration.getAll").getResultList();
  }

  @Override
    public HostRegistration getHostRegistration(String hostname) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getHostRegistration(em, hostname);
    } finally {
      if (em != null)
        em.close();
    }
  }

  protected HostRegistration getHostRegistration(EntityManager em, String hostname) {
    return (HostRegistration) em.createNamedQuery("HostRegistration.byHostName").setParameter("host", hostname).getSingleResult();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getChildJobs(long)
   */
  @Override
  public List<Job> getChildJobs(long id) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      TypedQuery<JpaJob> query = em.createNamedQuery("Job.root.children", JpaJob.class);
      query.setParameter("id", id);
      List<JpaJob> jobs = query.getResultList();
      if (jobs.size() == 0) {
        jobs = getChildren(em, id);
      }
      return $(jobs).sort(new Comparator<JpaJob>() {
        @Override
        public int compare(JpaJob job1, JpaJob job2) {
          if (job1.getDateCreated() == null || job2.getDateCreated() == null) {
            return 0;
          } else {
            return job1.getDateCreated().compareTo(job2.getDateCreated());
          }
        }
      }).map(fnSetJobUri()).map(fnToJob()).toList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  private List<JpaJob> getChildren(EntityManager em, long id) throws Exception {
    Query query = em.createNamedQuery("Job.children");
    query.setParameter("id", id);
    List<JpaJob> childJobs = query.getResultList();
    List<JpaJob> resultJobs = new ArrayList<>(childJobs);
    for (JpaJob childJob : childJobs) {
      resultJobs.addAll(getChildren(em, childJob.getId()));
    }
    return resultJobs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String, Status)
   */
  @Override
  public List<Job> getJobs(String type, Status status) throws ServiceRegistryException {
    TypedQuery<JpaJob> query = null;
    EntityManager em = null;
    logger.trace("Getting jobs '{}' and '{}'", type, status);
    try {
      em = emf.createEntityManager();
      if (type == null && status == null) {
        query = em.createNamedQuery("Job.all", JpaJob.class);
      } else if (type == null) {
        query = em.createNamedQuery("Job.status", JpaJob.class);
        query.setParameter("status", status.ordinal());
      } else if (status == null) {
        query = em.createNamedQuery("Job.type", JpaJob.class);
        query.setParameter("serviceType", type);
      } else {
        query = em.createNamedQuery("Job", JpaJob.class);
        query.setParameter("status", status.ordinal());
        query.setParameter("serviceType", type);
      }
      List<JpaJob> jobs = query.getResultList();
      for (JpaJob job : jobs) {
        setJobUri(job);
      }

      return $(jobs).map(fnToJob()).toList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }

  }

  @Override
  public List<String> getJobPayloads(String operation) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      TypedQuery<String> query = em.createNamedQuery("Job.payload", String.class);
      query.setParameter("operation", operation);
      logger.debug("Requesting job payloads using query: {}", query);
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @Override
  public List<String> getJobPayloads(String operation, int limit, int offset) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      TypedQuery<String> query = em.createNamedQuery("Job.payload", String.class);
      query.setParameter("operation", operation);
      query.setMaxResults(limit);
      query.setFirstResult(offset);
      logger.debug("Requesting job payloads using query: {}", query);
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @Override
  public int getJobCount(final String operation) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Job.countByOperationOnly");
      query.setParameter("operation", operation);
      final Number countResult = (Number) query.getSingleResult();
      return countResult.intValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getActiveJobs()
   */
  @Override
  public List<Job> getActiveJobs() throws ServiceRegistryException {
    List<Status> statuses = new ArrayList<Status>();
    for (Status status : Status.values()) {
      if (status.isActive())
        statuses.add(status);
    }
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      List<JpaJob> jpaJobs = getJobsByStatus(em, statuses.toArray(new Status[statuses.size()]));
      List<Job> jobs = new ArrayList<Job>(jpaJobs.size());
      for (JpaJob jpaJob : jpaJobs) {
        jobs.add(jpaJob.toJob());
      }
      return jobs;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Get the list of jobs with status from the given statuses.
   *
   * @param em
   *          the entity manager
   * @param statuses
   *          variable sized array of status values to test on jobs
   * @return list of jobs with status from statuses
   * @throws ServiceRegistryException
   *           if there is a problem communicating with the jobs database
   */
  public List<JpaJob> getJobsByStatus(EntityManager em, Status... statuses) throws ServiceRegistryException {
    if (statuses == null || statuses.length < 1)
      throw new IllegalArgumentException("At least one job status must be given.");

    List<Integer> ordinalStatuses = new ArrayList<>();
    for (Status status : statuses) {
      ordinalStatuses.add(status.ordinal());
    }
    TypedQuery<JpaJob> query = null;
    try {
      query = em.createNamedQuery("Job.statuses", JpaJob.class);
      query.setParameter("statuses", ordinalStatuses);
      List<JpaJob> jpaJobs = query.getResultList();
      for (JpaJob jpaJob : jpaJobs) {
        setJobUri(jpaJob);
      }
      return jpaJobs;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * Gets jobs of all types that are in the given state.
   *
   * @param em the entity manager
   * @param offset apply offset to the db query if offset &gt; 0
   * @param limit apply limit to the db query if limit &gt; 0
   * @param statuses the job status should be one from the given statuses
   * @return the list of jobs waiting for dispatch
   * @throws ServiceRegistryException if there is a problem communicating with the jobs database
   */
  protected List<JpaJob> getDispatchableJobsWithStatus(EntityManager em, int offset, int limit, Status... statuses)
          throws ServiceRegistryException {
    if (statuses == null)
      return Collections.EMPTY_LIST;

    List<Integer> statusesOrdinal = new ArrayList<>(statuses.length);
    for (Status status : statuses) {
      statusesOrdinal.add(status.ordinal());
    }

    TypedQuery<JpaJob> query = null;
    try {
      query = em.createNamedQuery("Job.dispatchable.status", JpaJob.class);
      query.setParameter("statuses", statusesOrdinal);
      if (offset > 0)
        query.setFirstResult(offset);
      if (limit > 0)
        query.setMaxResults(limit);
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * Return dispatchable job ids, where the job status is RESTART or QUEUED and the job id is listed in the given set.
   *
   * @param em the entity manager
   * @param jobIds set with job id's interested in
   * @return list with dispatchable job id's from the given set, with job status RESTART or QUEUED
   * @throws ServiceRegistryException if there is a problem communicating with the jobs database
   */
  protected List<Long> getDispatchableJobsWithIdFilter(EntityManager em, Set<Long> jobIds)
          throws ServiceRegistryException {
    if (jobIds == null || jobIds.isEmpty())
      return Collections.EMPTY_LIST;

    Query query = null;
    try {
      query = em.createNamedQuery("Job.dispatchable.status.idfilter");
      query.setParameter("jobids", dispatchPriorityList.keySet());
      query.setParameter("statuses", Arrays.asList(Status.RESTART.ordinal(), Status.QUEUED.ordinal()));
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @SuppressWarnings("unchecked")
  protected List<Object[]> getAvgOperations(EntityManager em) throws ServiceRegistryException {
    Query query = null;
    try {
      query = em.createNamedQuery("Job.avgOperation");
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @SuppressWarnings("unchecked")
  List<Object[]> getCountPerHostService(EntityManager em) throws ServiceRegistryException {
    Query query = null;
    try {
      query = em.createNamedQuery("Job.countPerHostService");
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query;
      if (serviceType == null && status == null) {
        query = em.createNamedQuery("Job.count.all");
      } else if (serviceType == null) {
        query = em.createNamedQuery("Job.count.nullType");
        query.setParameter("status", status.ordinal());
      } else if (status == null) {
        query = em.createNamedQuery("Job.count.nullStatus");
        query.setParameter("serviceType", serviceType);
      } else {
        query = em.createNamedQuery("Job.count");
        query.setParameter("status", status.ordinal());
        query.setParameter("serviceType", serviceType);
      }
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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
    EntityManager em = null;
    Query query;
    try {
      em = emf.createEntityManager();
      if (serviceType != null && !serviceType.isEmpty()) {
        query = em.createNamedQuery("Job.countByHost");
        query.setParameter("serviceType", serviceType);
      } else {
        query = em.createNamedQuery("Job.countByHost.nullType");
      }
      query.setParameter("status", status.ordinal());
      query.setParameter("host", host);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Job.countByOperation");
      query.setParameter("status", status.ordinal());
      query.setParameter("serviceType", serviceType);
      query.setParameter("operation", operation);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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
            || status == null)
      throw new IllegalArgumentException("service type, host, operation, and status must be provided");
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("Job.fullMonty");
      query.setParameter("status", status.ordinal());
      query.setParameter("serviceType", serviceType);
      query.setParameter("operation", operation);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countOfAbnormalServices()
   */
  @Override
  public long countOfAbnormalServices() throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("ServiceRegistration.countNotNormal");
      Number count = (Number) query.getSingleResult();
      return count.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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
    return getServiceStatistics(
             DateUtils.addDays(now, -maxJobAge),
             DateUtils.addDays(now, 1)); // Avoid glitches around 'now' by setting the endDate to 'tomorrow'
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
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  private List<ServiceStatistics> getServiceStatistics(Date startDate, Date endDate) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Map<Long, JaxbServiceStatistics> statsMap = new HashMap<Long, JaxbServiceStatistics>();

      // Make sure we also include the services that have no processing history so far
      List<ServiceRegistrationJpaImpl> services = em.createNamedQuery("ServiceRegistration.getAll").getResultList();
      for (ServiceRegistrationJpaImpl s : services) {
        statsMap.put(s.getId(), new JaxbServiceStatistics(s));
      }

      Query query = em.createNamedQuery("ServiceRegistration.statistics");
      query.setParameter("minDateCreated", startDate, TemporalType.TIMESTAMP);
      query.setParameter("maxDateCreated", endDate, TemporalType.TIMESTAMP);

      List queryResults = query.getResultList();
      for (Object result : queryResults) {
        Object[] oa = (Object[]) result;
        Number serviceRegistrationId = ((Number) oa[0]);
        if (serviceRegistrationId == null || serviceRegistrationId.longValue() == 0)
          continue;
        Status status = Status.values()[((Number) oa[1]).intValue()];
        Number count = (Number) oa[2];
        Number meanQueueTime = (Number) oa[3];
        Number meanRunTime = (Number) oa[4];

        // The statistics query returns a cartesian product, so we need to iterate over them to build up the objects
        JaxbServiceStatistics stats = statsMap.get(serviceRegistrationId.longValue());
        if (stats == null)
          continue;

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
      }

      List<ServiceStatistics> stats = new ArrayList<ServiceStatistics>(statsMap.values());
      Collections.sort(stats, new Comparator<ServiceStatistics>() {
        @Override
        public int compare(ServiceStatistics o1, ServiceStatistics o2) {
          ServiceRegistration reg1 = o1.getServiceRegistration();
          ServiceRegistration reg2 = o2.getServiceRegistration();
          int typeComparison = reg1.getServiceType().compareTo(reg2.getServiceType());
          return typeComparison == 0 ? reg1.getHost().compareTo(reg2.getHost()) : typeComparison;
        }
      });

      return stats;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
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
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      SystemLoad loadByHost = getHostLoads(em);
      List<HostRegistration> hostRegistrations = getHostRegistrations();
      List<ServiceRegistration> serviceRegistrations = getServiceRegistrationsByType(serviceType);
      return getServiceRegistrationsByLoad(serviceType, serviceRegistrations, hostRegistrations, loadByHost);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getCurrentHostLoads()
   */
  @Override
  public SystemLoad getCurrentHostLoads() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getHostLoads(em);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets a map of hosts to the number of jobs currently loading that host
   *
   * @param em
   *          the entity manager
   *
   * @return the map of hosts to job counts
   */
  SystemLoad getHostLoads(EntityManager em) {
    final SystemLoad systemLoad = new SystemLoad();

    // Find all jobs that are currently running on any given host, or get all of them
    Query q = em.createNamedQuery("ServiceRegistration.hostloads");
    List<Integer> statuses = new LinkedList<Integer>();
    for (Status status : JOB_STATUSES_INFLUENCING_LOAD_BALANCING) {
      statuses.add(status.ordinal());
    }
    q.setParameter("statuses", statuses);
    //Note: This is used in the query to filter out workflow jobs.
    //These jobs are load balanced by the workflow service directly.
    q.setParameter("workflow_type", TYPE_WORKFLOW);

    // Accumulate the numbers for relevant job statuses per host
    for (Object result : q.getResultList()) {
      Object[] resultArray = (Object[]) result;
      String host = String.valueOf(resultArray[0]);

      Status status = Status.values()[(int) resultArray[1]];
      float currentLoad = ((Number) resultArray[2]).floatValue();
      float maxLoad = ((Number) resultArray[3]).floatValue();

      // Only queued, and running jobs are adding to the load, so every other status is discarded
      if (status == null || !JOB_STATUSES_INFLUENCING_LOAD_BALANCING.contains(status)) {
        currentLoad = 0.0f;
      }

      // Add the service registration
      NodeLoad serviceLoad = new NodeLoad(host, currentLoad, maxLoad);
      systemLoad.addNodeLoad(serviceLoad);
    }

    // This is important, otherwise services which have no current load are not listed in the output!
    for (HostRegistration h : getHostRegistrations(em)) {
      if (!systemLoad.containsHost(h.getBaseUrl())) {
        systemLoad.addNodeLoad(new NodeLoad(h.getBaseUrl(), 0.0f, h.getMaxLoad()));
      }
    }

    return systemLoad;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return em.createNamedQuery("ServiceRegistration.getByType").setParameter("serviceType", serviceType)
              .getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByHost(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return em.createNamedQuery("ServiceRegistration.getByHost").setParameter("host", host).getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getServiceRegistration(em, serviceType, host);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * A custom ServiceTracker that registers all locally published servlets so clients can find the most appropriate
   * service on the network to handle new jobs.
   */
  class RestServiceTracker extends ServiceTracker {
    protected static final String FILTER = "(&(objectClass=javax.servlet.Servlet)("
            + RestConstants.SERVICE_PATH_PROPERTY + "=*))";

    protected BundleContext bundleContext = null;

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
      boolean publishFlag = (Boolean) reference.getProperty(RestConstants.SERVICE_PUBLISH_PROPERTY);
      boolean jobProducer = (Boolean) reference.getProperty(RestConstants.SERVICE_JOBPRODUCER_PROPERTY);

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
  void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
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
   * Callback for setting the user directory service.
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

  /** OSGi DI. */
  public void setIncidentService(IncidentService incidentService) {
    // Manually resolve the cyclic dependency between the incident service and the service registry
    ((OsgiIncidentService) incidentService).setServiceRegistry(this);
    this.incidents = new Incidents(this, incidentService);
  }

  /**
   * Update the jobs failure history and the service status with the given information. All these data are then use for
   * the jobs failover strategy. Only the terminated job (with FAILED or FINISHED status) are taken into account.
   *
   * @param em
   *          the current entity manager
   * @param job
   *          the current job that failed/succeeded
   * @throws ServiceRegistryException
   * @throws IllegalArgumentException
   */
  private void updateServiceForFailover(EntityManager em, JpaJob job)
          throws IllegalArgumentException, ServiceRegistryException {
    if (job.getStatus() != Status.FAILED && job.getStatus() != Status.FINISHED)
      return;

    job.setStatus(job.getStatus(), job.getFailureReason());

    // At this point, the only possible states for the current service are NORMAL and WARNING,
    // the services in ERROR state will not be chosen by the dispatcher
    ServiceRegistrationJpaImpl currentService = job.getProcessorServiceRegistration();
    if (currentService == null)
      return;

    // Job is finished with a failure
    if (job.getStatus() == FAILED && !DATA.equals(job.getFailureReason())) {

      // Services in WARNING or ERROR state triggered by current job
      List<ServiceRegistrationJpaImpl> relatedWarningOrErrorServices = getRelatedWarningErrorServices(job);

      // Before this job failed there was at least one job failed with this job signature on any service
      if (relatedWarningOrErrorServices.size() > 0) {

        for (ServiceRegistrationJpaImpl relatedService : relatedWarningOrErrorServices) {
          // Skip current service from the list
          if (currentService.equals(relatedService))
            continue;

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

          updateServiceState(em, relatedService);
        }

      }

      // This is the first job with this signature failing on any service
      else {

        // Set the current service to WARNING state
        if (currentService.getServiceState() == NORMAL) {
          logger.info("State set to WARNING for current service {} on host {}", currentService.getServiceType(),
                  currentService.getHost());
          currentService.setServiceState(WARNING, job.toJob().getSignature());
          updateServiceState(em, currentService);
        }

        // The current service already is in WARNING state and max attempts is reached
        else if (getHistorySize(currentService) >= maxAttemptsBeforeErrorState) {
          logger.info("State set to ERROR for current service {} on host {}", currentService.getServiceType(),
                  currentService.getHost());
          currentService.setServiceState(ERROR, job.toJob().getSignature());
          updateServiceState(em, currentService);
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
        updateServiceState(em, currentService);
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
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      ServiceRegistrationJpaImpl service = getServiceRegistration(em, serviceType, host);
      if (service == null)
        throw new NotFoundException("");
      logger.info("State reset to NORMAL for service {} on host {} through santize method", service.getServiceType(),
              service.getHost());
      service.setServiceState(NORMAL);
      updateServiceState(em, service);
    } finally {
      if (em != null)
        em.close();
    }
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
  private int getHistorySize(ServiceRegistration serviceRegistration)
          throws IllegalArgumentException, ServiceRegistryException {
    if (serviceRegistration == null)
      throw new IllegalArgumentException("serviceRegistration must not be null!");

    Query query = null;
    EntityManager em = null;
    logger.debug("Calculating count of jobs who failed due to service {}", serviceRegistration.toString());
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("Job.count.history.failed");
      query.setParameter("serviceType", serviceRegistration.getServiceType());
      query.setParameter("host", serviceRegistration.getHost());
      Number number = (Number) query.getSingleResult();
      return number.intValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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
  private List<ServiceRegistrationJpaImpl> getRelatedWarningErrorServices(JpaJob job)
          throws ServiceRegistryException {
    if (job == null)
      throw new IllegalArgumentException("job must not be null!");

    Query query = null;
    EntityManager em = null;
    logger.debug("Try to get the services in WARNING or ERROR state triggered by {} failed",
            job);
    try {
      em = emf.createEntityManager();

      // TODO: modify the query to avoid to go through the list here
      query = em.createNamedQuery("ServiceRegistration.relatedservices.warning_error");
      query.setParameter("serviceType", job.getJobType());

      List<ServiceRegistrationJpaImpl> jpaServices = new ArrayList<ServiceRegistrationJpaImpl>();

      @SuppressWarnings("unchecked")
      List<ServiceRegistrationJpaImpl> serviceResults = query.getResultList();
      for (ServiceRegistrationJpaImpl relatedService : serviceResults) {
        if (relatedService.getServiceState() == WARNING
                && relatedService.getWarningStateTrigger() == job.toJob().getSignature()) {
          jpaServices.add(relatedService);
        }

        if (relatedService.getServiceState() == ERROR && relatedService.getErrorStateTrigger() == job.toJob().getSignature()) {
          jpaServices.add(relatedService);
        }
      }
      return jpaServices;
    } catch (NoResultException e) {
      return null;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
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

    final List<String> hostBaseUrls = $(hostRegistrations).map(toBaseUrl).toList();
    final List<ServiceRegistration> filteredList = new ArrayList<ServiceRegistration>();

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
      if (hostLoadMax == null)
        logger.warn("Unable to determine max load for host {}", service.getHost());

      // Determine the current load for this host
      Float hostLoad = systemLoad.get(service.getHost()).getLoadFactor();
      if (hostLoad == null)
        logger.warn("Unable to determine current load for host {}", service.getHost());

      // Is this host suited for processing?
      if (hostLoad == null || hostLoadMax == null || hostLoad < hostLoadMax) {
        logger.debug("Adding candidate service {} for processing of jobs of type '{}' (host load is {} of max {})",
           service, jobType, hostLoad, hostLoadMax);
        filteredList.add(service);
      }

    }

    // Sort the list by capacity
    Collections.sort(filteredList, new LoadComparator(systemLoad));

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

    final List<String> hostBaseUrls = $(hostRegistrations).map(toBaseUrl).toList();
    final List<ServiceRegistration> filteredList = new ArrayList<ServiceRegistration>();

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

    // Sort the list by capacity
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
    for (HostRegistration host : getHostRegistrations()) {
      NodeLoad load = new NodeLoad(host.getBaseUrl(), 0.0f, host.getMaxLoad());
      loads.addNodeLoad(load);
    }
    return loads;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxLoadOnNode(java.lang.String)
   */
  @Override
  public NodeLoad getMaxLoadOnNode(String host) throws ServiceRegistryException, NotFoundException {
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("HostRegistration.getMaxLoadByHostName");
      query.setParameter("host", host);
      return new NodeLoad(host, 0.0f, ((Number) query.getSingleResult()).floatValue());
    } catch (NoResultException e) {
      throw new NotFoundException(e);
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  private final Fn<HostRegistration, String> toBaseUrl = new Fn<HostRegistration, String>() {
    @Override
    public String apply(HostRegistration h) {
      return h.getBaseUrl();
    }
  };

  /**
   * This dispatcher implementation will check for jobs in the QUEUED {@link Status}. If
   * new jobs are found, the dispatcher will attempt to dispatch each job to the least loaded service.
   */
  class JobDispatcher implements Runnable {

    /** A list with job types that cannot be dispatched in each interation */
    private List<String> undispatchableJobTypes = null;

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

      logger.debug("Starting job dispatching");

      undispatchableJobTypes = new ArrayList<String>();
      EntityManager em = null;
      try {
        em = emf.createEntityManager();

        // FIXME: the stats are not currently used and the queries are very
        // expense in database time.
        if (collectJobstats) {
          jobsStatistics.updateAvg(getAvgOperations(em));
          jobsStatistics.updateJobCount(getCountPerHostService(em));
        }

        if (!dispatchPriorityList.isEmpty()) {
          logger.trace("Checking for outdated jobs in dispatchPriorityList's '{}' jobs", dispatchPriorityList.size());
          // Remove outdated jobs from priority list
          List<Long> jobIds = getDispatchableJobsWithIdFilter(em, dispatchPriorityList.keySet());
          for (Long jobId : new HashSet<>(dispatchPriorityList.keySet())) {
            if (!jobIds.contains(jobId)) {
              logger.debug("Removing outdated dispatchPriorityList job '{}'", jobId);
              dispatchPriorityList.remove(jobId);
            }
          }
        }

        int jobsOffset = 0;
        List<JpaJob> dispatchableJobs = null;
        List<JpaJob> workflowJobs = new ArrayList();
        boolean jobsFound = false;
        do {
          // dispatch all dispatchable jobs with status restarted
          dispatchableJobs = getDispatchableJobsWithStatus(em, jobsOffset, DEFAULT_DISPATCH_JOBS_LIMIT, Status.RESTART);
          jobsOffset += DEFAULT_DISPATCH_JOBS_LIMIT;
          jobsFound = !dispatchableJobs.isEmpty();

          // skip all jobs of type workflow, we will handle them next
          for (JpaJob job : dispatchableJobs) {
            if (TYPE_WORKFLOW.equals(job.getJobType())) {
              workflowJobs.add(job);
            }
          }
          if (dispatchableJobs.removeAll(workflowJobs) && dispatchableJobs.isEmpty())
            continue;

          dispatchDispatchableJobs(em, dispatchableJobs);
        } while (jobsFound);

        jobsOffset = 0;
        jobsFound = false;
        do {
          // dispatch all dispatchable jobs with status queued
          dispatchableJobs = getDispatchableJobsWithStatus(em, jobsOffset, DEFAULT_DISPATCH_JOBS_LIMIT, Status.QUEUED);
          jobsOffset += DEFAULT_DISPATCH_JOBS_LIMIT;
          jobsFound = !dispatchableJobs.isEmpty();

          // skip all jobs of type workflow, we will handle them next
          for (JpaJob job : dispatchableJobs) {
            if (TYPE_WORKFLOW.equals(job.getJobType())) {
              workflowJobs.add(job);
            }
          }
          if (dispatchableJobs.removeAll(workflowJobs) && dispatchableJobs.isEmpty())
            continue;

          dispatchDispatchableJobs(em, dispatchableJobs);
        } while (jobsFound);

        if (!workflowJobs.isEmpty())
          dispatchDispatchableJobs(em, workflowJobs);

      } catch (Throwable t) {
        logger.warn("Error dispatching jobs", t);
      } finally {
        undispatchableJobTypes = null;
        if (em != null)
          em.close();
      }

      logger.debug("Finished job dispatching");
    }

    /**
     * Dispatch the given jobs.
     *
     * @param em the entity manager
     * @param jobsToDispatch list with dispatchable jobs to dispatch
     */
    private void dispatchDispatchableJobs(EntityManager em, List<JpaJob> jobsToDispatch) {
      //Get the current system load
      SystemLoad systemLoad = getHostLoads(em);

      for (JpaJob job : jobsToDispatch) {

        // Remember the job type
        String jobType = job.getJobType();

        // Skip jobs that we already know can't be dispatched except of jobs in the priority list
        String jobSignature = new StringBuilder(jobType).append('@').append(job.getOperation()).toString();
        if (undispatchableJobTypes.contains(jobSignature) && !dispatchPriorityList.keySet().contains(job.getId())) {
          logger.trace("Skipping dispatching of {} with type '{}' for this round of dispatching", job,
                  jobType);
          continue;
        }

        // Set the job's user and organization prior to dispatching
        String creator = job.getCreator();
        String creatorOrganization = job.getOrganization();

        // Try to load the organization.
        Organization organization = null;
        try {
          organization = organizationDirectoryService.getOrganization(creatorOrganization);
          securityService.setOrganization(organization);
        } catch (NotFoundException e) {
          logger.debug("Skipping dispatching of job for non-existing organization '{}'", creatorOrganization);
          continue;
        }

        // Try to load the user
        User user = userDirectoryService.loadUser(creator);
        if (user == null) {
          logger.warn("Unable to dispatch {}: creator '{}' is not available", job, creator);
          continue;
        }
        securityService.setUser(user);

        // Start dispatching
        try {
          List<ServiceRegistration> services = getServiceRegistrations(em);
          List<HostRegistration> hosts = $(getHostRegistrations(em)).filter(filterOutPriorityHosts._2(job.getId()))
                  .toList();
          List<ServiceRegistration> candidateServices = null;

          // Depending on whether this running job is trying to reach out to other services or whether this is an
          // attempt to execute the next operation in a workflow, choose either from a limited or from the full list
          // of services
          Job parentJob = null;
          try {
            if (job.getParentJob() != null)
              parentJob = getJob(job.getParentJob().getId());
          } catch (NotFoundException e) {
            // That's ok
          }

          // When a job A starts a series of child jobs, then those child jobs should only be dispatched at the
          // same time if there is processing capacity available.
          boolean parentHasRunningChildren = false;
          if (parentJob != null) {
            for (Job child : getChildJobs(parentJob.getId())) {
              if (Status.RUNNING.equals(child.getStatus())) {
                parentHasRunningChildren = true;
                break;
              }
            }
          }

          // If this is a root job (a new workflow or a new workflow operation), then only dispatch if there is
          // capacity, i. e. the workflow service is ok dispatching the next workflow or the next workflow operation.
          if (parentJob == null || TYPE_WORKFLOW.equals(jobType) || parentHasRunningChildren) {
            logger.trace("Using available capacity only for dispatching of {} to a service of type '{}'", job,
                    jobType);
            candidateServices = getServiceRegistrationsWithCapacity(jobType, services, hosts, systemLoad);
          } else {
            logger.trace("Using full list of services for dispatching of {} to a service of type '{}'", job, jobType);
            candidateServices = getServiceRegistrationsByLoad(jobType, services, hosts, systemLoad);
          }

          // Try to dispatch the job
          String hostAcceptingJob = null;
          try {
            hostAcceptingJob = dispatchJob(em, job, candidateServices);
            try {
              systemLoad.updateNodeLoad(hostAcceptingJob, job.getJobLoad());
            } catch (NotFoundException e) {
              logger.info("Host {} not found in load list, cannot dispatch {} to it", hostAcceptingJob, job);
            }

            dispatchPriorityList.remove(job.getId());
          } catch (ServiceUnavailableException e) {
            logger.debug("Jobs of type {} currently cannot be dispatched", job.getOperation());
            // Don't mark workflow jobs as undispatchable to not impact worklfow operations
            if (!TYPE_WORKFLOW.equals(jobType))
              undispatchableJobTypes.add(jobSignature);
            continue;
          } catch (UndispatchableJobException e) {
            logger.debug("{} currently cannot be dispatched", job);
            continue;
          }

          logger.debug("{} dispatched to {}", job, hostAcceptingJob);
        } catch (ServiceRegistryException e) {
          Throwable cause = (e.getCause() != null) ? e.getCause() : e;
          logger.error("Error dispatching {}: {}", job, cause);
        } finally {
          securityService.setUser(null);
          securityService.setOrganization(null);
        }
      }
    }

    /**
     * Dispatches the job to the least loaded service that will accept the job, or throws a
     * <code>ServiceUnavailableException</code> if there is no such service.
     *
     * @param em
     *          the current entity manager
     * @param job
     *          the job to dispatch
     * @param services
     *          a list of service registrations
     * @return the host that accepted the dispatched job, or <code>null</code> if no services took the job.
     * @throws ServiceRegistryException
     *           if the service registrations are unavailable
     * @throws ServiceUnavailableException
     *           if no service is available or if all available services refuse to take on more work
     * @throws UndispatchableJobException
     *           if the current job cannot be processed
     */
    protected String dispatchJob(EntityManager em, JpaJob job, List<ServiceRegistration> services)
            throws ServiceRegistryException, ServiceUnavailableException, UndispatchableJobException {

      if (services.size() == 0) {
        logger.debug("No service is currently available to handle jobs of type '" + job.getJobType() + "'");
        throw new ServiceUnavailableException("No service of type " + job.getJobType() + " available");
      }

      // Try the service registrations, after the first one finished, we quit;
      job.setStatus(Status.DISPATCHING);

      boolean triedDispatching = false;

      boolean jobLoadExceedsMaximumLoads = false;
      final Float highestMaxLoad = $(services).map(toHostRegistration).map(toMaxLoad).sort(sortFloatValuesDesc).head2();
      if (job.getJobLoad() > highestMaxLoad) {
        // None of the available hosts is able to accept the job because the largest max load value is less than this job's load value
        jobLoadExceedsMaximumLoads = true;
      }

      for (ServiceRegistration registration : services) {
        job.setProcessorServiceRegistration((ServiceRegistrationJpaImpl) registration);

        // Skip registration of host with less max load than highest available max load
        // Note: This service registration may or may not live on a node which is set to accept jobs exceeding its max load
        if (jobLoadExceedsMaximumLoads
                && job.getProcessorServiceRegistration().getHostRegistration().getMaxLoad() != highestMaxLoad) {
          continue;
        }

        try {
          job = updateInternal(em, job);
        } catch (Exception e) {
          // In theory, we should catch javax.persistence.OptimisticLockException. Unfortunately, eclipselink throws
          // org.eclipse.persistence.exceptions.OptimisticLockException. In order to avoid importing the implementation
          // specific APIs, we just catch Exception.
          logger.debug("Unable to dispatch {}.  This is likely caused by another service registry dispatching the job",
                  job);
          throw new UndispatchableJobException(job + " is already being dispatched");
        }

        triedDispatching = true;

        String serviceUrl = UrlSupport.concat(registration.getHost(), registration.getPath(), "dispatch");
        HttpPost post = new HttpPost(serviceUrl);

        // Add current organization and user so they can be used during execution at the remote end
        post.addHeader(ORGANIZATION_HEADER, securityService.getOrganization().getId());
        post.addHeader(USER_HEADER, securityService.getUser().getUsername());

        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("id", Long.toString(job.getId())));
        params.add(new BasicNameValuePair("operation", job.getOperation()));
        post.setEntity(new UrlEncodedFormEntity(params, UTF_8));

        // Post the request
        HttpResponse response = null;
        int responseStatusCode;
        try {
          logger.debug("Trying to dispatch {} type '{}' load {} to {}",
                  job, job.getJobType(), job.getJobLoad(), registration.getHost());
          if (!START_WORKFLOW.equals(job.getOperation()))
            setCurrentJob(job.toJob());
          response = client.execute(post);
          responseStatusCode = response.getStatusLine().getStatusCode();
          if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
            return registration.getHost();
          } else if (responseStatusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
            logger.debug("Service {} is currently refusing to accept jobs of type {}", registration,
                    job.getOperation());
            continue;
          } else if (responseStatusCode == HttpStatus.SC_PRECONDITION_FAILED) {
            job.setStatus(Status.FAILED);
            job = updateJob(job);
            logger.debug("Service {} refused to accept {}", registration, job);
            throw new UndispatchableJobException(IOUtils.toString(response.getEntity().getContent()));
          } else if (responseStatusCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
            logger.debug("Service {} is not yet reachable", registration);
            continue;
          } else {
            logger.warn("Service {} failed ({}) accepting {}", registration, responseStatusCode, job);
            continue;
          }
        } catch (UndispatchableJobException e) {
          throw e;
        } catch (TrustedHttpClientException e) {
          // Will try another node. If no other node, it will be re-queued
          logger.warn("Unable to dispatch {}", job, e);
          continue;
        } catch (Exception e) {
          logger.warn("Unable to dispatch {}", job, e);
        } finally {
          client.close(response);
          setCurrentJob(null);
        }
      }

      // We've tried dispatching to every online service that can handle this type of job, with no luck.
      if (triedDispatching) {
        // Workflow type jobs are not set to priority list, because they handle accepting jobs not based on the job load
        // If the system don't accepts jobs whose load exceeds the host's max load we can't make use of the priority
        // list
        if (acceptJobLoadsExeedingMaxLoad && !dispatchPriorityList.containsKey(job.getId())
                && !TYPE_WORKFLOW.equals(job.getJobType()) && job.getProcessorServiceRegistration() != null) {
          String host = job.getProcessorServiceRegistration().getHost();
          logger.debug("About to add {} to dispatchPriorityList with processor host {}", job, host);
          dispatchPriorityList.put(job.getId(), host);
        }

        try {
          job.setStatus(Status.QUEUED);
          job.setProcessorServiceRegistration(null);
          job = updateJob(job);
        } catch (Exception e) {
          logger.error("Unable to put {} back into queue", job, e);
        }
      }

      logger.debug("Unable to dispatch {}, no service is currently ready to accept the job", job);
      throw new UndispatchableJobException(job + " is currently undispatchable");
    }

    private final Fn2<HostRegistration, Long, Boolean> filterOutPriorityHosts = new Fn2<HostRegistration, Long, Boolean>() {
      @Override
      public Boolean apply(HostRegistration host, Long jobId) {
        if (dispatchPriorityList.values().contains(host.getBaseUrl())
                && !host.getBaseUrl().equals(dispatchPriorityList.get(jobId))) {
          return false;
        }
        return true;
      }
    };

    private final Fn<ServiceRegistration, HostRegistration> toHostRegistration = new Fn<ServiceRegistration, HostRegistration>() {
      @Override
      public HostRegistration apply(ServiceRegistration s) {
        return ((ServiceRegistrationJpaImpl) s).getHostRegistration();
      }
    };

    private final Fn<HostRegistration, Float> toMaxLoad = new Fn<HostRegistration, Float>() {
      @Override
      public Float apply(HostRegistration h) {
        return h.getMaxLoad();
      }
    };

    private final Comparator<Float> sortFloatValuesDesc = new Comparator<Float>() {
      @Override
      public int compare(Float o1, Float o2) {
        return o2.compareTo(o1);
      }
    };

  }

  /** A periodic check on each service registration to ensure that it is still alive. */
  class JobProducerHeartbeat implements Runnable {

    /** List of service registrations that have been found unresponsive last time we checked */
    private final List<ServiceRegistration> unresponsive = new ArrayList<ServiceRegistration>();

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
          if (!service.isJobProducer())
            continue;
          if (service.isInMaintenanceMode())
            continue;

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
                    logger.trace("Service " + service.toString() + " is responsive: " + response.getStatusLine());
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
                    if (!service.isOnline())
                      continue;
                    logger.warn("Service {} is not working as expected: {}", service, response.getStatusLine());
                }
              } else {
                logger.warn("Service {} does not respond: {}", service.toString());
              }
            } catch (TrustedHttpClientException e) {
              if (!service.isOnline())
                continue;
              logger.warn("Unable to reach {} : {}", service, e);
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
              logger.warn("Unable to unregister unreachable service: {} : {}", service, e);
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
   * the service's host is already running. The lower that number, the bigger the capacity.
   */
  private static final class LoadComparator implements Comparator<ServiceRegistration> {

    private SystemLoad loadByHost = null;

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
      //If the load factors are about the same, sort based on maximum load
      if (Math.abs(nodeA.getLoadFactor() - nodeB.getLoadFactor()) <= 0.01) {
        //NOTE: The sort order below is *reversed* from what you'd expect
        //When we're comparing the load factors we want the node with the lowest factor to be first
        //When we're comparing the maximum load value, we want the node with the highest max to be first
        return Float.compare(nodeB.getMaxLoad(), nodeA.getMaxLoad());
      }
      return Float.compare(nodeA.getLoadFactor(), nodeB.getLoadFactor());

    }

  }

  /**
   * Comparator that will sort jobs according to their status. Those that were restarted are on top, those that are
   * queued are next.
   */
  static final class DispatchableComparator implements Comparator<JpaJob> {

    @Override
    public int compare(JpaJob jobA, JpaJob jobB) {

      // Jobs that are in "restart" mode should be handled first
      if (Status.RESTART.equals(jobA.getStatus()) && !Status.RESTART.equals(jobB.getStatus())) {
        return -1;
      } else if (Status.RESTART.equals(jobB.getStatus()) && !Status.RESTART.equals(jobA.getStatus())) {
        return 1;
      }

      // Regular jobs should be processed prior to workflow and workflow operation jobs
      if (TYPE_WORKFLOW.equals(jobA.getJobType()) && !TYPE_WORKFLOW.equals(jobB.getJobType())) {
        return 1;
      } else if (TYPE_WORKFLOW.equals(jobB.getJobType()) && !TYPE_WORKFLOW.equals(jobA.getJobType())) {
        return -1;
      }

      // Use created date
      if (jobA.getDateCreated() != null && jobB.getDateCreated() != null) {
        if (jobA.getDateCreated().getTime() < jobB.getDateCreated().getTime())
          return -1;
        else if (jobA.getDateCreated().getTime() > jobB.getDateCreated().getTime())
          return 1;
      }

      // undecided
      return 0;
    }

  }

}
