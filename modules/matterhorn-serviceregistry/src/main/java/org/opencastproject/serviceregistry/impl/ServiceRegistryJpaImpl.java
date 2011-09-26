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
package org.opencastproject.serviceregistry.impl;

import static org.apache.commons.lang.StringUtils.isBlank;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;

/**
 * JPA implementation of the {@link ServiceRegistry}
 */
public class ServiceRegistryJpaImpl implements ServiceRegistry {

  static final Logger logger = LoggerFactory.getLogger(ServiceRegistryJpaImpl.class);

  /** Configuration key for the maximum load */
  protected static final String OPT_MAXLOAD = "org.opencastproject.server.maxload";

  /** Configuration key for the dispatch interval in miliseconds */
  protected static final String OPT_DISPATCHINTERVAL = "org.opencastproject.serviceregistry.dispatchinterval";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Minimum delay between job dispatching attempts, in milliseconds */
  static final long MIN_DISPATCH_INTERVAL = 1000;

  /** Default delay between job dispatching attempts, in milliseconds */
  static final long DEFAULT_DISPATCH_PERIOD = 5000;

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /** This host's base URL */
  protected String hostName;

  /** The base URL for job URLs */
  protected String jobHost;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** Tracks services published locally and adds them to the service registry */
  protected RestServiceTracker tracker = null;

  /** The maximum number of parallel jobs possible on this host. */
  protected int maxJobs = 1;

  /** The thread pool to use for dispatching queued jobs and checking on phantom services. */
  protected ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  @SuppressWarnings("unchecked")
  protected Map persistenceProperties;

  /**
   * A static list of statuses that influence how load balancing is calculated
   */
  protected static final List<Status> JOB_STATUSES_INFLUENCING_LOAD_BALANCING;

  static {
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING = new ArrayList<Status>();
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.DISPATCHING);
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.RUNNING);
  }

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  @SuppressWarnings("unchecked")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public void activate(ComponentContext cc) {
    logger.debug("activate");

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.serviceregistry", persistenceProperties);

    // Find this host's url
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty("org.opencastproject.server.url"))) {
      hostName = UrlSupport.DEFAULT_BASE_URL;
    } else {
      hostName = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }

    // Find the jobs URL
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty("org.opencastproject.jobs.url"))) {
      jobHost = hostName;
    } else {
      jobHost = cc.getBundleContext().getProperty("org.opencastproject.jobs.url");
    }

    // Register this host
    try {
      if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(OPT_MAXLOAD))) {
        maxJobs = Runtime.getRuntime().availableProcessors();
      } else {
        try {
          maxJobs = Integer.parseInt(cc.getBundleContext().getProperty(OPT_MAXLOAD));
        } catch (NumberFormatException e) {
          maxJobs = Runtime.getRuntime().availableProcessors();
          logger.warn("Configuration key '{}' is not an integer. Falling back to the number of cores ({})",
                  OPT_MAXLOAD, maxJobs);
        }
      }
      registerHost(hostName, maxJobs);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Unable to register host " + hostName + " in the service registry", e);
    }

    // Track any services from this host that need to be added to the service registry
    if (cc != null) {
      try {
        tracker = new RestServiceTracker(cc.getBundleContext());
        tracker.open(true);
      } catch (InvalidSyntaxException e) {
        logger.error("Invlid filter syntax: {}", e);
        throw new IllegalStateException(e);
      }
    }

    long dispatchInterval = DEFAULT_DISPATCH_PERIOD;
    if (cc != null) {
      String dispatchIntervalString = StringUtils.trimToNull(cc.getBundleContext().getProperty(OPT_DISPATCHINTERVAL));
      if (dispatchIntervalString != null) {
        try {
          dispatchInterval = Long.parseLong(dispatchIntervalString);
        } catch (Exception e) {
          logger.warn("Dispatch interval '{}' is malformed, setting to {}", dispatchIntervalString,
                  MIN_DISPATCH_INTERVAL);
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
    }

    // Schedule the job dispatching.
    if (dispatchInterval > 0)
      scheduledExecutor.scheduleWithFixedDelay(new JobDispatcher(), dispatchInterval, dispatchInterval,
              TimeUnit.MILLISECONDS);

    // Schedule the service heartbeat
    scheduledExecutor.scheduleWithFixedDelay(new JobProducerHearbeat(), 1, 1, TimeUnit.MINUTES);
  }

  public void deactivate() {
    logger.debug("deactivate");
    if (tracker != null) {
      tracker.close();
    }
    try {
      unregisterHost(hostName);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Unable to unregister host " + hostName + " from the service registry", e);
    }
    if (emf != null) {
      emf.close();
    }

    // Stop the job dispatcher
    if (scheduledExecutor != null)
      scheduledExecutor.shutdown();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, null, null, true);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, null, true);
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
    return createJob(this.hostName, type, operation, arguments, payload, true);
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
    return createJob(this.hostName, type, operation, arguments, payload, dispatchable);
  }

  /**
   * Creates a job on a remote host.
   */
  public Job createJob(String host, String serviceType, String operation, List<String> arguments, String payload,
          boolean dispatchable) throws ServiceRegistryException {
    if (StringUtils.isBlank(host))
      throw new IllegalArgumentException("Host can't be null");
    if (StringUtils.isBlank(serviceType))
      throw new IllegalArgumentException("Service type can't be null");
    if (StringUtils.isBlank(operation))
      throw new IllegalArgumentException("Operation can't be null");

    User currentUser = securityService.getUser();
    Organization currentOrganization = securityService.getOrganization();

    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationJpaImpl creatingService = getServiceRegistration(em, serviceType, host);
      if (creatingService == null) {
        throw new ServiceRegistryException("No service registration exists for type '" + serviceType + "' on host '"
                + host + "'");
      }
      if (creatingService.getHostRegistration().isMaintenanceMode()) {
        logger.warn("Creating a job from {}, which is currently in maintenance mode.", creatingService.getHost());
      }
      JobJpaImpl job = new JobJpaImpl(currentUser, currentOrganization, creatingService, operation, arguments, payload,
              dispatchable);

      creatingService.creatorJobs.add(job);

      // if this job is not dispatchable, it must be handled by the host that has created it
      if (dispatchable) {
        job.setStatus(Status.QUEUED);
      } else {
        creatingService.processorJobs.add(job);
      }

      em.persist(job);
      tx.commit();
      setJobUri(job);
      return job;
    } catch (RollbackException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * Creates a job for a specific service registration.
   * 
   * @return the new job
   */
  protected JobJpaImpl createJob(ServiceRegistrationJpaImpl serviceRegistration, String operation,
          List<String> arguments, String payload, boolean dispatchable) {
    User currentUser = securityService.getUser();
    Organization currentOrganization = securityService.getOrganization();
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobJpaImpl job = new JobJpaImpl(currentUser, currentOrganization, serviceRegistration, operation, arguments,
              payload, dispatchable);
      serviceRegistration.creatorJobs.add(job);
      // if this job is not dispatchable, it must be handled by the host that has created it
      if (dispatchable) {
        job.setStatus(Status.QUEUED);
      } else {
        serviceRegistration.processorJobs.add(job);
      }
      em.persist(job);
      tx.commit();
      setJobUri(job);
      return job;
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(long)
   */
  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      JobJpaImpl job = em.find(JobJpaImpl.class, id);
      if (job == null) {
        throw new NotFoundException("Job " + id + " not found");
      }
      // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
      // this row in the database
      em.refresh(job);
      job.getArguments();
      setJobUri(job);
      return job;
    } catch (Exception e) {
      if (e instanceof NotFoundException) {
        throw (NotFoundException) e;
      } else {
        throw new ServiceRegistryException(e);
      }
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public Job updateJob(Job job) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      return updateInternal(em, job);
    } catch (PersistenceException e) {
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  protected Job setJobUri(Job job) {
    if (job instanceof JaxbJob) {
      try {
        ((JaxbJob) job).setUri(new URI(jobHost + "/services/job/" + job.getId() + ".xml"));
      } catch (URISyntaxException e) {
        logger.warn("Can not set the job URI", e);
      }
    } else {
      logger.warn("Can not set the job URI on a " + job.getClass().getName());
    }
    return job;
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
   */
  protected Job updateInternal(EntityManager em, Job job) throws PersistenceException {
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobJpaImpl fromDb;
      fromDb = em.find(JobJpaImpl.class, job.getId());
      if (fromDb == null)
        throw new NoResultException();
      update(fromDb, (JaxbJob) job);
      em.merge(fromDb);
      tx.commit();
      ((JaxbJob) job).setVersion(fromDb.getVersion());
      setJobUri(job);
      return job;
    } catch (PersistenceException e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    }
  }

  /**
   * Sets the queue and runtimes and other elements of a persistent job based on a job that's been modified in memory.
   * Times on both the objects must be modified, since the in-memory job must not be stale.
   * 
   * @param fromDb
   *          The job from the database
   * @param job
   *          The in-memory job
   */
  private void update(JobJpaImpl fromDb, JaxbJob job) {
    Date now = new Date();
    Status status = job.getStatus();
    fromDb.setPayload(job.getPayload());
    fromDb.setStatus(job.getStatus());
    fromDb.setDispatchable(job.isDispatchable());
    fromDb.setVersion(job.getVersion());
    fromDb.setOperation(job.getOperation());
    fromDb.setArguments(job.getArguments());
    if (job.getDateCreated() == null) {
      job.setDateCreated(now);
      fromDb.setDateCreated(now);
    }
    if (job.getProcessingHost() != null) {
      ServiceRegistrationJpaImpl processingService = (ServiceRegistrationJpaImpl) getServiceRegistration(
              job.getJobType(), job.getProcessingHost());
      fromDb.setProcessorServiceRegistration(processingService);
    }
    if (Status.RUNNING.equals(status)) {
      job.setDateStarted(now);
      job.setQueueTime(now.getTime() - job.getDateCreated().getTime());
      fromDb.setDateStarted(now);
      fromDb.setQueueTime(now.getTime() - job.getDateCreated().getTime());
    } else if (Status.FAILED.equals(status)) {
      // failed jobs may not have even started properly
      if (job.getDateStarted() != null) {
        job.setDateCompleted(now);
        job.setRunTime(now.getTime() - job.getDateStarted().getTime());
        fromDb.setDateCompleted(now);
        fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
      }
    } else if (Status.FINISHED.equals(status)) {
      if (job.getDateStarted() == null) {
        // Some services (e.g. ingest) don't use job dispatching, since they start immediately and handle their own
        // lifecycle. In these cases, if the start date isn't set, use the date created as the start date
        job.setDateStarted(job.getDateCreated());
      }
      job.setDateCompleted(now);
      job.setRunTime(now.getTime() - job.getDateStarted().getTime());
      fromDb.setDateCompleted(now);
      fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
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
  protected HostRegistration fetchHostRegistration(EntityManager em, String host) {
    Query query = em.createNamedQuery("HostRegistration.byHostName");
    query.setParameter("host", host);
    try {
      return (HostRegistration) query.getSingleResult();
    } catch (NoResultException e) {
      logger.debug("No existing host registration for {}", host);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(java.lang.String, int)
   */
  @Override
  public void registerHost(String host, int maxJobs) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Find the existing registrations for this host and if it exists, update it
      HostRegistration existingHostRegistration = fetchHostRegistration(em, host);
      if (existingHostRegistration == null) {
        em.persist(new HostRegistration(host, maxJobs, true, false));
      } else {
        existingHostRegistration.setMaxJobs(maxJobs);
        existingHostRegistration.setOnline(true);
        em.merge(existingHostRegistration);
      }
      logger.info("Registering {} with a maximum load of {}", host, maxJobs);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
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
      HostRegistration existingHostRegistration = fetchHostRegistration(em, host);
      if (existingHostRegistration == null) {
        throw new ServiceRegistryException("Host '" + host
                + "' is not currently registered, so it can not be unregistered");
      } else {
        existingHostRegistration.setOnline(false);
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          unRegisterService(serviceRegistration.getServiceType(), serviceRegistration.getHost());
        }
        em.merge(existingHostRegistration);
      }
      logger.info("Unregistering {}", host, maxJobs);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
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
    return setOnlineStatus(serviceType, baseUrl, path, true, false);
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
    return setOnlineStatus(serviceType, baseUrl, path, true, jobProducer);
  }

  protected ServiceRegistrationJpaImpl getServiceRegistration(EntityManager em, String serviceType, String host) {
    try {
      Query q = em.createNamedQuery("ServiceRegistration.getRegistration");
      q.setParameter("serviceType", serviceType);
      q.setParameter("host", host);
      return (ServiceRegistrationJpaImpl) q.getSingleResult();
    } catch (NoResultException e) {
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
      throw new IllegalArgumentException("serviceType and baseUrl must not be blank");
    }
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      HostRegistration hostRegistration = fetchHostRegistration(em, baseUrl);
      if (hostRegistration == null) {
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
      return registration;
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
    } finally {
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
    // TODO: create methods that accept an entity manager, so we can execute multiple queries using the same em and tx
    setOnlineStatus(serviceType, baseUrl, null, false, null);

    // Find all jobs running on this service, and set them back to QUEUED.
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Query query = em.createNamedQuery("Job.processinghost.status");
      query.setParameter("status", Status.RUNNING);
      query.setParameter("host", baseUrl);
      query.setParameter("serviceType", serviceType);
      @SuppressWarnings("unchecked")
      List<JobJpaImpl> unregisteredJobs = query.getResultList();
      for (JobJpaImpl job : unregisteredJobs) {
        if (job.isDispatchable()) {
          job.setStatus(Status.QUEUED);
          job.setProcessorServiceRegistration(null);
        } else {
          job.setStatus(Status.FAILED);
        }
        em.merge(job);
      }
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String baseUrl, boolean maintenance) throws IllegalStateException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      HostRegistration reg = fetchHostRegistration(em, baseUrl);
      if (reg == null) {
        throw new IllegalArgumentException("Can not set maintenance mode on a host that has not been registered");
      }
      reg.setMaintenanceMode(maintenance);
      em.merge(reg);
      tx.commit();
    } catch (RollbackException e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
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
    EntityManager em = emf.createEntityManager();
    try {
      return getServiceRegistrations(em);
    } finally {
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<Job> getJobs(String type, Status status) throws ServiceRegistryException {
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      if (type == null && status == null) {
        query = em.createNamedQuery("Job.all");
      } else if (type == null) {
        query = em.createNamedQuery("Job.status");
        query.setParameter("status", status);
      } else if (status == null) {
        query = em.createNamedQuery("Job.type");
        query.setParameter("serviceType", type);
      } else {
        query = em.createNamedQuery("Job");
        query.setParameter("status", status);
        query.setParameter("serviceType", type);
      }
      List<Job> jobs = query.getResultList();
      for (Job job : jobs) {
        setJobUri(job);
      }
      return jobs;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * Gets jobs of all types that are in the {@value Status#QUEUED} state and are dispatchable.
   * 
   * @param em
   *          the entity manager
   * @return the list of jobs waiting for dispatch
   * @throws ServiceRegistryException
   *           if there is a problem communicating with the jobs database
   */
  @SuppressWarnings("unchecked")
  protected List<Job> getDispatchableJobs(EntityManager em) throws ServiceRegistryException {
    Query query = null;
    try {
      query = em.createNamedQuery("Job.dispatchable.status");
      query.setParameter("status", Status.QUEUED);
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      Query query;
      if (status == null) {
        query = em.createNamedQuery("Job.count.nullStatus");
      } else {
        query = em.createNamedQuery("Job.count");
        query.setParameter("status", status);
      }
      query.setParameter("serviceType", serviceType);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByHost(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByHost(String serviceType, String host, Status status) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("Job.countByHost");
      query.setParameter("status", status);
      query.setParameter("serviceType", serviceType);
      query.setParameter("host", host);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByOperation(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByOperation(String serviceType, String operation, Status status) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("Job.countByOperation");
      query.setParameter("status", status);
      query.setParameter("serviceType", serviceType);
      query.setParameter("operation", operation);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, java.lang.String,
   *      java.lang.String, org.opencastproject.job.api.Job.Status)
   */
  public long count(String serviceType, String host, String operation, Status status) throws ServiceRegistryException {
    if (StringUtils.isBlank(serviceType) || StringUtils.isBlank(host) || StringUtils.isBlank(operation)
            || status == null)
      throw new IllegalArgumentException("service type, host, operation, and status must be provided");
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("Job.fullMonty");
      query.setParameter("status", status);
      query.setParameter("serviceType", serviceType);
      query.setParameter("operation", operation);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
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
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("ServiceRegistration.statistics");
      Map<ServiceRegistration, JaxbServiceStatistics> statsMap = new HashMap<ServiceRegistration, JaxbServiceStatistics>();
      @SuppressWarnings("unchecked")
      List queryResults = query.getResultList();
      for (Object result : queryResults) {
        Object[] oa = (Object[]) result;
        ServiceRegistrationJpaImpl serviceRegistration = ((ServiceRegistrationJpaImpl) oa[0]);
        Status status = ((Status) oa[1]);
        Number count = (Number) oa[2];
        Number meanQueueTime = (Number) oa[3];
        Number meanRunTime = (Number) oa[4];

        // The statistics query returns a cartesian product, so we need to iterate over them to build up the objects
        JaxbServiceStatistics stats = statsMap.get(serviceRegistration);
        if (stats == null) {
          stats = new JaxbServiceStatistics(serviceRegistration);
          statsMap.put(serviceRegistration, stats);
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
    EntityManager em = emf.createEntityManager();
    try {
      Map<String, Integer> loadByHost = getHostLoads(em, true);
      List<ServiceRegistration> serviceRegistrations = getServiceRegistrationsByType(serviceType);
      return filterAndSortServiceRegistrations(serviceRegistrations, serviceType, loadByHost);
    } finally {
      em.close();
    }
  }

  /**
   * Gets a map of hosts to the number of jobs currently loading that host
   * 
   * @param em
   *          the entity manager
   * @param activeOnly
   *          if true, the map will include only hosts that are online and have non-maintenance mode services
   * @return the map of hosts to job counts
   */
  protected Map<String, Integer> getHostLoads(EntityManager em, boolean activeOnly) {
    Query q = em.createNamedQuery("ServiceRegistration.hostload");
    Map<String, Integer> loadByHost = new HashMap<String, Integer>();

    // Accumulate the numbers for relevant job statuses per host
    for (Object result : q.getResultList()) {
      Object[] resultArray = (Object[]) result;
      ServiceRegistrationJpaImpl service = (ServiceRegistrationJpaImpl) resultArray[0];
      Job.Status status = (Status) resultArray[1];
      int count = ((Number) resultArray[2]).intValue();

      if (activeOnly && (service.isInMaintenanceMode() || !service.isOnline())) {
        continue;
      }

      // Only queued and running jobs are adding to the load, so every other status is discarded
      if (status == null || !JOB_STATUSES_INFLUENCING_LOAD_BALANCING.contains(status)) {
        count = 0;
      }

      // Add the service registration
      if (loadByHost.containsKey(service.getHost())) {
        Integer previousServiceLoad = loadByHost.get(service.getHost());
        loadByHost.put(service.getHost(), previousServiceLoad + count);
      } else {
        loadByHost.put(service.getHost(), count);
      }
    }
    return loadByHost;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createNamedQuery("ServiceRegistration.getByType").setParameter("serviceType", serviceType)
              .getResultList();
    } finally {
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
    EntityManager em = emf.createEntityManager();
    try {
      return em.createNamedQuery("ServiceRegistration.getByHost").setParameter("host", host).getResultList();
    } finally {
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
      if (em != null) {
        em.close();
      }
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
      boolean jobProducer = (Boolean) reference.getProperty(RestConstants.SERVICE_JOBPRODUCER_PROPERTY);
      try {
        registerService(serviceType, hostName, servicePath, jobProducer);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to register job producer of type " + serviceType + " on host " + hostName);
      }
      return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      try {
        unRegisterService(serviceType, hostName);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to unregister job producer of type " + serviceType + " on host " + hostName);
      }
      super.removedService(reference, service);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getLoad()
   */
  @Override
  public SystemLoad getLoad() throws ServiceRegistryException {
    throw new UnsupportedOperationException();
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

  /**
   * Dispatches the job to the least loaded service that will accept the job, or throws a
   * <code>ServiceUnavailableException</code> if there is no such service.
   * 
   * @param em
   *          the current entity manager
   * @param job
   *          the job to dispatch
   * @param hostLoads
   *          a map containint each host and the number of jobs
   * @return the host that accepted the dispatched job, or <code>null</code> if no services took the job.
   * @throws ServiceRegistryException
   *           if the service registrations are unavailable
   */
  protected String dispatchJob(EntityManager em, Job job, List<ServiceRegistration> services)
          throws ServiceRegistryException {

    if (services.size() == 0) {
      logger.debug("No service is available to handle jobs of type '" + job.getJobType() + "'");
      return null;
    }

    // Try the service registrations, after the first one finished, we quit
    JobJpaImpl jpaJob = ((JobJpaImpl) job);
    jpaJob.setStatus(Status.DISPATCHING);
    boolean triedDispatching = false;

    for (ServiceRegistration registration : services) {
      jpaJob.setProcessorServiceRegistration((ServiceRegistrationJpaImpl) registration);
      try {
        updateInternal(em, jpaJob);
      } catch (Exception e) {
        // In theory, we should catch javax.persistence.OptimisticLockException. Unfortunately, eclipselink throws
        // org.eclipse.persistence.exceptions.OptimisticLockException. In order to avoid importing the implementation
        // specific APIs, we just catch Exception.
        logger.debug("Unable to dispatch {}.  This is likely caused by another service registry dispatching the job",
                job);
        return null;
      }

      triedDispatching = true;

      String serviceUrl = UrlSupport
              .concat(new String[] { registration.getHost(), registration.getPath(), "dispatch" });
      HttpPost post = new HttpPost(serviceUrl);
      try {
        String jobXml = JobParser.toXml(jpaJob);
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("job", jobXml));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
      } catch (IOException e) {
        logger.warn("Job parsing error on job {}", job, e);
        jpaJob.setStatus(Status.FAILED);
        jpaJob.setProcessorServiceRegistration(null);
        updateJob(jpaJob);
        throw new ServiceRegistryException("Can not serialize job " + jpaJob, e);
      }

      // Post the request
      HttpResponse response = null;
      int responseStatusCode;
      try {
        logger.debug("Trying to dispatch job {} of type '{}' to {}", new String[] { Long.toString(jpaJob.getId()),
                jpaJob.getJobType(), registration.getHost() });
        response = client.execute(post);
        responseStatusCode = response.getStatusLine().getStatusCode();
        if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
          return registration.getHost();
        } else if (responseStatusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
          logger.debug("Service {} refused to accept {}", registration, job);
          continue;
        } else {
          logger.warn("Service {} failed ({}) accepting {}", new Object[] { registration, responseStatusCode, job });
          continue;
        }

      } catch (Exception e) {
        logger.warn("Unable to dispatch job {}", jpaJob.getId(), e);
      } finally {
        client.close(response);
      }
    }

    // We've tried dispatching to every online service that can handle this type of job, with no luck.
    if (triedDispatching) {
      try {
        jpaJob.setStatus(Status.QUEUED);
        jpaJob.setProcessorServiceRegistration(null);
        updateJob(jpaJob);
      } catch (Exception e) {
        logger.error("Unable to put job back into queue", e);
      }
    }

    return null;
  }

  /**
   * Returns a filtered list of service registrations, containing only those that are online, not in maintenance mode,
   * and with a specific service type, ordered by load.
   * 
   * @param serviceRegistrations
   *          the complete list of service registrations.
   * @param serviceType
   *          the service type to filter by
   * @param loadByHost
   *          the map of hosts to the number of running jobs
   */
  protected List<ServiceRegistration> filterAndSortServiceRegistrations(List<ServiceRegistration> serviceRegistrations,
          String serviceType, final Map<String, Integer> loadByHost) {
    List<ServiceRegistration> filteredList = new ArrayList<ServiceRegistration>();
    for (ServiceRegistration reg : serviceRegistrations) {
      if (reg.isOnline() && !reg.isInMaintenanceMode() && serviceType.equals(reg.getServiceType())) {
        filteredList.add(reg);
      }
    }
    Comparator<ServiceRegistration> comparator = new Comparator<ServiceRegistration>() {
      @Override
      public int compare(ServiceRegistration reg1, ServiceRegistration reg2) {
        String host1 = reg1.getHost();
        String host2 = reg2.getHost();
        return loadByHost.get(host1) - loadByHost.get(host2);
      }
    };

    Collections.sort(filteredList, comparator);
    return filteredList;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxConcurrentJobs()
   */
  @Override
  public int getMaxConcurrentJobs() throws ServiceRegistryException {
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("HostRegistration.cores");
      return ((Number) query.getSingleResult()).intValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * This dispatcher implementation will check for jobs in the QUEUED {@link #org.opencastproject.job.api.Job.Status}. If new jobs are found, the
   * dispatcher will attempt to dispatch each job to the least loaded service.
   */
  class JobDispatcher implements Runnable {

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      EntityManager em = emf.createEntityManager();
      try {
        List<Job> jobsToDispatch = getDispatchableJobs(em);
        Map<String, Integer> hostLoads = getHostLoads(em, true);
        List<ServiceRegistration> serviceRegistrations = getServiceRegistrations();

        for (Job job : jobsToDispatch) {
          String creator = job.getCreator();
          String creatorOrganization = job.getOrganization();
          Organization organization = organizationDirectoryService.getOrganization(creatorOrganization);
          securityService.setOrganization(organization);
          User user = userDirectoryService.loadUser(creator);
          if (user == null)
            throw new IllegalStateException("Creator '" + creator + "' is not available");
          if (organization == null)
            throw new IllegalStateException("Organization '" + organization + "' is not available");
          try {
            securityService.setUser(user);
            securityService.setOrganization(organization);
            String hostAcceptingJob = dispatchJob(em, job,
                    filterAndSortServiceRegistrations(serviceRegistrations, job.getJobType(), hostLoads));
            if (hostAcceptingJob == null) {
              ServiceRegistryJpaImpl.logger.debug("Job {} could not be dispatched and is put back into queue",
                      job.getId());
            } else {
              ServiceRegistryJpaImpl.logger.debug("Job {} dispatched to {}", job.getId(), hostAcceptingJob);
              if (hostLoads.containsKey(hostAcceptingJob)) {
                Integer previousServiceLoad = hostLoads.get(hostAcceptingJob);
                hostLoads.put(hostAcceptingJob, ++previousServiceLoad);
              } else {
                hostLoads.put(hostAcceptingJob, 1);
              }
            }
          } catch (ServiceRegistryException e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            ServiceRegistryJpaImpl.logger.error("Error dispatching job " + job, cause);
          } finally {
            securityService.setUser(null);
            securityService.setOrganization(null);
          }
        }
      } catch (Throwable t) {
        ServiceRegistryJpaImpl.logger.warn("Error dispatching jobs", t);
      } finally {
        em.close();
      }
    }
  }

  /**
   * A periodic check on each service registration to ensure that it is still alive.
   */
  class JobProducerHearbeat implements Runnable {

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      logger.debug("Checking for unresponsive services");
      int unresponsiveCount = 0;
      List<ServiceRegistration> serviceRegistrations = getServiceRegistrations();
      for (ServiceRegistration registration : serviceRegistrations) {
        // We think this service is online and available. Prove it.
        if (registration.isOnline() && !registration.isInMaintenanceMode() && registration.isJobProducer()) {
          String[] urlParts = new String[] { registration.getHost(), registration.getPath(), "dispatch" };
          String serviceUrl = UrlSupport.concat(urlParts);
          HttpOptions options = new HttpOptions(serviceUrl);
          HttpResponse response = null;
          try {
            try {
              response = client.execute(options);
              if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // this service is reachable, continue checking other services
                continue;
              } else {
                unresponsiveCount++;
              }
            } catch (TrustedHttpClientException e) {
              // We can not reach this host. Update the registration to indicate that the service is offline.
              logger.warn("Unable to reach {} : {}", registration, e);
            }
            try {
              unRegisterService(registration.getServiceType(), registration.getHost());
              logger.warn("Set {} to offline, since it is not accessible from this host", registration);
            } catch (ServiceRegistryException e) {
              logger.warn("Unable to unregister unreachable service: {} : {}", registration, e);
            }
          } finally {
            client.close(response);
          }
        }
      }
      logger.debug("Finished checking for unresponsive services. Found {} to be unresponsive.", unresponsiveCount);
    }
  }

}
