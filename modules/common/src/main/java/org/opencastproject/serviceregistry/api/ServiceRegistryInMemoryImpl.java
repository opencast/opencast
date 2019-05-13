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

package org.opencastproject.serviceregistry.api;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.SystemLoad.NodeLoad;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Simple and in-memory implementation of a the service registry intended for testing scenarios. */
public class ServiceRegistryInMemoryImpl implements ServiceRegistry {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryInMemoryImpl.class);

  /** Default dispatcher timeout (1 second) */
  public static final long DEFAULT_DISPATCHER_TIMEOUT = 100;

  /** Hostname for localhost */
  private static final String LOCALHOST = "localhost";

  /** The hosts */
  protected Map<String, HostRegistrationInMemory> hosts = new HashMap<String, HostRegistrationInMemory>();

  /** The service registrations */
  protected Map<String, List<ServiceRegistrationInMemoryImpl>> services = new HashMap<String, List<ServiceRegistrationInMemoryImpl>>();

  /** The serialized jobs */
  protected Map<Long, String> jobs = new HashMap<Long, String>();

  /** A mapping of services to jobs */
  protected Map<ServiceRegistrationInMemoryImpl, Set<Job>> jobHosts = new HashMap<ServiceRegistrationInMemoryImpl, Set<Job>>();

  /** The thread pool to use for dispatching queued jobs. */
  protected ScheduledExecutorService dispatcher = Executors.newScheduledThreadPool(1);

  /** The job identifier */
  protected AtomicLong idCounter = new AtomicLong();

  /** Holds the current running job */
  protected Job currentJob = null;

  /**
   * An (optional) security service. If set to a non-null value, this will be used to obtain the current user when
   * creating new jobs.
   */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  protected Incidents incidents;

  /**
   * A static list of statuses that influence how load balancing is calculated
   */
  protected static final List<Status> JOB_STATUSES_INFLUENCING_LOAD_BALANCING;

  static {
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING = new ArrayList<Status>();
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.QUEUED);
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.RUNNING);
  }

  public ServiceRegistryInMemoryImpl(JobProducer service, float maxLoad, SecurityService securityService,
          UserDirectoryService userDirectoryService, OrganizationDirectoryService organizationDirectoryService,
          IncidentService incidentService) throws ServiceRegistryException {
    //Note: total memory here isn't really the correct value, but we just need something (preferably non-zero)
    registerHost(LOCALHOST, LOCALHOST, Runtime.getRuntime().totalMemory(), Runtime.getRuntime().availableProcessors(), maxLoad);
    if (service != null)
      registerService(service, maxLoad);
    this.securityService = securityService;
    this.userDirectoryService = userDirectoryService;
    this.organizationDirectoryService = organizationDirectoryService;
    this.incidents = new Incidents(this, incidentService);
    this.dispatcher.scheduleWithFixedDelay(new JobDispatcher(), DEFAULT_DISPATCHER_TIMEOUT, DEFAULT_DISPATCHER_TIMEOUT,
            TimeUnit.MILLISECONDS);
  }

  public ServiceRegistryInMemoryImpl(JobProducer service, SecurityService securityService,
          UserDirectoryService userDirectoryService, OrganizationDirectoryService organizationDirectoryService,
          IncidentService incidentService)
          throws ServiceRegistryException {
    this(service, Runtime.getRuntime().availableProcessors(), securityService, userDirectoryService, organizationDirectoryService, incidentService);
  }

  /**
   * This method shuts down the service registry.
   */
  public void dispose() {
    dispatcher.shutdownNow();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#enableHost(String)
   */
  @Override
  public void enableHost(String host) throws ServiceRegistryException, NotFoundException {
    if (hosts.containsKey(host)) {
      hosts.get(host).setActive(true);
    } else {
      throw new NotFoundException("The host named " + host + " was not found");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#disableHost(String)
   */
  @Override
  public void disableHost(String host) throws ServiceRegistryException, NotFoundException {
    if (hosts.containsKey(host)) {
      hosts.get(host).setActive(false);
    } else {
      throw new NotFoundException("The host named " + host + " was not found");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(String, String, long, int, float)
   */
  @Override
  public void registerHost(String host, String address, long memory, int cores, float maxLoad)
          throws ServiceRegistryException {
    HostRegistrationInMemory hrim = new HostRegistrationInMemory(address, address, maxLoad, cores, memory);
    hosts.put(host, hrim);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unregisterHost(java.lang.String)
   */
  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
    hosts.remove(host);
    services.remove(host);
  }

  /**
   * Method to register locally running services.
   *
   * @param localService
   *          the service instance
   * @return the service registration
   * @throws ServiceRegistryException
   */
  public ServiceRegistration registerService(JobProducer localService) throws ServiceRegistryException {
    return registerService(localService, Runtime.getRuntime().availableProcessors());
  }

  /**
   * Method to register locally running services.
   *
   * @param localService
   *          the service instance
   * @param maxLoad
   *          the maximum load the host can support
   * @return the service registration
   * @throws ServiceRegistryException
   */
  public ServiceRegistration registerService(JobProducer localService, float maxLoad) throws ServiceRegistryException {
    HostRegistrationInMemory hrim = hosts.get(LOCALHOST);

    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(LOCALHOST);
    if (servicesOnHost == null) {
      servicesOnHost = new ArrayList<ServiceRegistrationInMemoryImpl>();
      services.put(LOCALHOST, servicesOnHost);
    }

    ServiceRegistrationInMemoryImpl registration = new ServiceRegistrationInMemoryImpl(localService, hrim.getBaseUrl());
    registration.setMaintenance(false);
    servicesOnHost.add(registration);
    return registration;
  }

  /**
   * Removes the job producer from the service registry.
   *
   * @param localService
   *          the service
   * @throws ServiceRegistryException
   *           if removing the service fails
   */
  public void unregisterService(JobProducer localService) throws ServiceRegistryException {
    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(LOCALHOST);
    if (servicesOnHost != null) {
      ServiceRegistrationInMemoryImpl s = (ServiceRegistrationInMemoryImpl) localService;
      servicesOnHost.remove(s);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path)
          throws ServiceRegistryException {
    return registerService(serviceType, host, path, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer)
          throws ServiceRegistryException {

    HostRegistrationInMemory hostRegistration = hosts.get(host);
    if (hostRegistration == null) {
      throw new ServiceRegistryException(new NotFoundException("Host " + host + " was not found"));
    }

    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(host);
    if (servicesOnHost == null) {
      servicesOnHost = new ArrayList<ServiceRegistrationInMemoryImpl>();
      services.put(host, servicesOnHost);
    }

    ServiceRegistrationInMemoryImpl registration = new ServiceRegistrationInMemoryImpl(serviceType, host, path,
            jobProducer);
    servicesOnHost.add(registration);
    return registration;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String host) throws ServiceRegistryException {
    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(host);
    if (servicesOnHost != null) {
      Iterator<ServiceRegistrationInMemoryImpl> ri = servicesOnHost.iterator();
      while (ri.hasNext()) {
        ServiceRegistration registration = ri.next();
        if (serviceType.equals(registration.getServiceType()))
          ri.remove();
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String host, boolean maintenance) throws NotFoundException {
    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(host);
    if (!hosts.containsKey(host)) {
      throw new NotFoundException("Host " + host + " was not found");
    }
    hosts.get(host).setMaintenanceMode(maintenance);
    if (servicesOnHost != null) {
      for (ServiceRegistrationInMemoryImpl r : servicesOnHost) {
        r.setMaintenance(maintenance);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation) throws ServiceRegistryException {
    return createJob(type, operation, null, null, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          Float)
   */
  @Override
  public Job createJob(String type, String operation, Float jobLoad) throws ServiceRegistryException {
    return createJob(type, operation, null, null, true, 1.0f);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceRegistryException {
    return createJob(type, operation, arguments, null, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          java.util.List, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, Float jobLoad)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, null, true, jobLoad);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          java.util.List, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          java.util.List, java.lang.String, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, Float jobLoad)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, true, jobLoad);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          java.util.List, java.lang.String, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, queueable, null, 1.0f);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          java.util.List, java.lang.String, boolean, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable,
          Float jobLoad) throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, queueable, null, jobLoad);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          java.util.List, java.lang.String, boolean, org.opencastproject.job.api.Job)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable,
          Job parentJob) throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, queueable, parentJob, 1.0f);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
          java.util.List, java.lang.String, boolean, org.opencastproject.job.api.Job, Float)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable,
          Job parentJob, Float jobLoad) throws ServiceRegistryException {
    if (getServiceRegistrationsByType(type).size() == 0)
      logger.warn("Service " + type + " not available");

    Job job = null;
    synchronized (this) {
      job = new JobImpl(idCounter.addAndGet(1));
      if (securityService != null) {
        job.setCreator(securityService.getUser().getUsername());
        job.setOrganization(securityService.getOrganization().getId());
      }
      job.setDateCreated(new Date());
      job.setJobType(type);
      job.setOperation(operation);
      job.setArguments(arguments);
      job.setPayload(payload);
      if (queueable)
        job.setStatus(Status.QUEUED);
      else
        job.setStatus(Status.INSTANTIATED);
      if (parentJob != null)
        job.setParentJobId(parentJob.getId());
      job.setJobLoad(jobLoad);
    }

    synchronized (jobs) {
      try {
        jobs.put(job.getId(), JobParser.toXml(new JaxbJob(job)));
      } catch (IOException e) {
        throw new IllegalStateException("Error serializing job " + job, e);
      }
    }
    return job;
  }

  private void removeJob(long id) throws NotFoundException, ServiceRegistryException {
    synchronized (jobs) {
      if (!jobs.containsKey(id))
        throw new NotFoundException("No job with ID '" + id + "' found");

      jobs.remove(id);
    }
  }

  @Override
  public void removeJobs(List<Long> ids) throws NotFoundException, ServiceRegistryException {
    synchronized (jobs) {
      for (long id : ids) {
        removeJob(id);
      }
    }
  }

  /**
   * Dispatches the job to the least loaded service or throws a <code>ServiceUnavailableException</code> if there is no
   * such service.
   *
   * @param job
   *          the job to dispatch
   * @return whether the job was dispatched
   * @throws ServiceUnavailableException
   *           if no service is available to dispatch the job
   * @throws ServiceRegistryException
   *           if the service registrations are unavailable or dispatching of the job fails
   */
  protected boolean dispatchJob(Job job) throws ServiceUnavailableException, ServiceRegistryException,
          UndispatchableJobException {
    List<ServiceRegistration> registrations = getServiceRegistrationsByLoad(job.getJobType());
    if (registrations.size() == 0)
      throw new ServiceUnavailableException("No service is available to handle jobs of type '" + job.getJobType() + "'");
    job.setStatus(Status.DISPATCHING);
    try {
      job = updateJob(job);
    } catch (NotFoundException e) {
      throw new ServiceRegistryException("Job not found!", e);
    }
    for (ServiceRegistration registration : registrations) {
      if (registration.isJobProducer() && !registration.isInMaintenanceMode()) {
        ServiceRegistrationInMemoryImpl inMemoryRegistration = (ServiceRegistrationInMemoryImpl) registration;
        JobProducer service = inMemoryRegistration.getService();

        // Add the job to the list of jobs so that it gets counted in the load.
        // This is the same way that the JPA impl does it
        Set<Job> jobs = jobHosts.get(inMemoryRegistration);
        if (jobs == null) {
          jobs = new LinkedHashSet<Job>();
        }
        jobs.add(job);
        jobHosts.put(inMemoryRegistration, jobs);

        if (!service.isReadyToAcceptJobs(job.getOperation())) {
          jobs.remove(job);
          jobHosts.put(inMemoryRegistration, jobs);
          continue;
        }
        if (!service.isReadyToAccept(job)) {
          jobs.remove(job);
          jobHosts.put(inMemoryRegistration, jobs);
          continue;
        }
        try {
          job = updateJob(job);
        } catch (NotFoundException e) {
          jobs.remove(job);
          jobHosts.put(inMemoryRegistration, jobs);
          throw new ServiceRegistryException("Job not found!", e);
        }
        service.acceptJob(job);
        return true;
      } else if (!registration.isJobProducer()) {
        logger.warn("This implementation of the service registry doesn't support dispatching to remote services");
        // TODO: Add remote dispatching
      } else {
        logger.warn("Service " + registration + " is in maintenance mode");
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public Job updateJob(Job job) throws NotFoundException, ServiceRegistryException {
    if (job == null)
      throw new IllegalArgumentException("Job cannot be null");
    Job updatedJob = null;
    synchronized (jobs) {
      try {
        updatedJob = updateInternal(job);
        jobs.put(updatedJob.getId(), JobParser.toXml(new JaxbJob(updatedJob)));
      } catch (IOException e) {
        throw new IllegalStateException("Error serializing job", e);
      }
    }
    return updatedJob;
  }

  private Job updateInternal(Job job) {
    Date now = new Date();
    Status status = job.getStatus();
    if (job.getDateCreated() == null) {
      job.setDateCreated(now);
    }
    if (Status.RUNNING.equals(status)) {
      if (job.getDateStarted() == null) {
        job.setDateStarted(now);
        job.setQueueTime(now.getTime() - job.getDateCreated().getTime());
      }
    } else if (Status.FAILED.equals(status)) {
      // failed jobs may not have even started properly
      job.setDateCompleted(now);
      if (job.getDateStarted() != null) {
        job.setRunTime(now.getTime() - job.getDateStarted().getTime());
      }
    } else if (Status.FINISHED.equals(status)) {
      if (job.getDateStarted() == null) {
        // Some services (e.g. ingest) don't use job dispatching, since they start immediately and handle their own
        // lifecycle. In these cases, if the start date isn't set, use the date created as the start date
        job.setDateStarted(job.getDateCreated());
      }
      job.setDateCompleted(now);
      job.setRunTime(now.getTime() - job.getDateStarted().getTime());

      // Cleanup local list of jobs assigned to a specific service
      for (Entry<String, List<ServiceRegistrationInMemoryImpl>> service : services.entrySet()) {
        for (ServiceRegistrationInMemoryImpl srv : service.getValue()) {
          Set<Job> jobs = jobHosts.get(srv);
          if (jobs != null) {
            Set<Job> updatedJobs = new HashSet<>();
            for (Job savedJob : jobs) {
              if (savedJob.getId() != job.getId())
                updatedJobs.add(savedJob);
            }
            jobHosts.put(srv, updatedJobs);
          }
        }
      }
    }
    return job;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(long)
   */
  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    synchronized (jobs) {
      String serializedJob = jobs.get(id);
      if (serializedJob == null)
        throw new NotFoundException(Long.toString(id));
      try {
        return JobParser.parseJob(serializedJob);
      } catch (IOException e) {
        throw new IllegalStateException("Error unmarshaling job", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getChildJobs(long)
   */
  @Override
  public List<Job> getChildJobs(long id) throws ServiceRegistryException {
    List<Job> result = new ArrayList<Job>();
    synchronized (jobs) {
      for (String serializedJob : jobs.values()) {
        Job job = null;
        try {
          job = JobParser.parseJob(serializedJob);
        } catch (IOException e) {
          throw new IllegalStateException("Error unmarshaling job", e);
        }
        if (job.getParentJobId() == null)
          continue;
        if (job.getParentJobId().equals(id) || job.getRootJobId().equals(id))
          result.add(job);

        Long parentJobId = job.getParentJobId();
        while (parentJobId != null && parentJobId > 0) {
          try {
            Job parentJob = getJob(job.getParentJobId());
            if (parentJob.getParentJobId().equals(id)) {
              result.add(job);
              break;
            }
            parentJobId = parentJob.getParentJobId();
          } catch (NotFoundException e) {
            throw new ServiceRegistryException("Job from parent job id was not found!", e);
          }
        }
      }
    }
    Collections.sort(result, new Comparator<Job>() {
      @Override
      public int compare(Job job1, Job job2) {
        return job1.getDateCreated().compareTo(job1.getDateCreated());
      }
    });
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public List<Job> getJobs(String serviceType, Status status) throws ServiceRegistryException {
    List<Job> result = new ArrayList<Job>();
    synchronized (jobs) {
      for (String serializedJob : jobs.values()) {
        Job job = null;
        try {
          job = JobParser.parseJob(serializedJob);
        } catch (IOException e) {
          throw new IllegalStateException("Error unmarshaling job", e);
        }
        if (serviceType.equals(job.getJobType()) && status.equals(job.getStatus()))
          result.add(job);
      }
    }
    return result;
  }

  @Override
  public List<String> getJobPayloads(String operation) throws ServiceRegistryException {
    List<String> result = new ArrayList<>();
    for (String serializedJob : jobs.values()) {
      try {
        Job job = JobParser.parseJob(serializedJob);
        if (operation.equals(job.getOperation())) {
          result.add(job.getPayload());
        }
      } catch (IOException e) {
        throw new IllegalStateException("Error unmarshaling job", e);
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getActiveJobs()
   */
  @Override
  public List<Job> getActiveJobs() throws ServiceRegistryException {
    List<Job> result = new ArrayList<Job>();
    synchronized (jobs) {
      for (String serializedJob : jobs.values()) {
        Job job = null;
        try {
          job = JobParser.parseJob(serializedJob);
        } catch (IOException e) {
          throw new IllegalStateException("Error unmarshaling job", e);
        }
        if (job.getStatus().isActive())
          result.add(job);
      }
    }
    return result;
  }

  @Override
  public Incidents incident() {
    return incidents;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByLoad(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException {
    return getServiceRegistrationsByType(serviceType);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    List<ServiceRegistration> result = new ArrayList<ServiceRegistration>();
    for (List<ServiceRegistrationInMemoryImpl> servicesPerHost : services.values()) {
      for (ServiceRegistrationInMemoryImpl r : servicesPerHost) {
        if (serviceType.equals(r.getServiceType()))
          result.add(r);
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByHost(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    List<ServiceRegistration> result = new ArrayList<ServiceRegistration>();
    List<ServiceRegistrationInMemoryImpl> servicesPerHost = services.get(host);
    if (servicesPerHost != null) {
      result.addAll(servicesPerHost);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) throws ServiceRegistryException {
    List<ServiceRegistrationInMemoryImpl> servicesPerHost = services.get(host);
    if (servicesPerHost != null) {
      for (ServiceRegistrationInMemoryImpl r : servicesPerHost) {
        if (serviceType.equals(r.getServiceType()))
          return r;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() throws ServiceRegistryException {
    List<ServiceRegistration> result = new ArrayList<ServiceRegistration>();
    for (List<ServiceRegistrationInMemoryImpl> servicesPerHost : services.values()) {
      result.addAll(servicesPerHost);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @Override
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
    throw new UnsupportedOperationException("Operation not yet implemented");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countOfAbnormalServices()
   */
  @Override
  public long countOfAbnormalServices() throws ServiceRegistryException {
    throw new UnsupportedOperationException("Operation not yet implemented");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    return count(serviceType, null, null, status);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByOperation(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByOperation(String serviceType, String operation, Status status) throws ServiceRegistryException {
    return count(serviceType, null, operation, status);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByHost(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByHost(String serviceType, String host, Status status) throws ServiceRegistryException {
    return count(serviceType, host, null, status);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, java.lang.String,
   *      java.lang.String, org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, String host, String operation, Status status) throws ServiceRegistryException {
    int count = 0;
    synchronized (jobs) {
      for (String serializedJob : jobs.values()) {
        Job job = null;
        try {
          job = JobParser.parseJob(serializedJob);
        } catch (IOException e) {
          throw new IllegalStateException("Error unmarshaling job", e);
        }
        if (serviceType != null && !serviceType.equals(job.getJobType()))
          continue;
        if (host != null && !host.equals(job.getProcessingHost()))
          continue;
        if (operation != null && !operation.equals(job.getOperation()))
          continue;
        if (status != null && !status.equals(job.getStatus()))
          continue;
        count++;
      }
    }
    return count;
  }

  /**
   * This dispatcher implementation will wake from time to time and check for new jobs. If new jobs are found, it will
   * dispatch them to the services as appropriate.
   */
  class JobDispatcher implements Runnable {

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

      // Go through the jobs and find those that have not yet been dispatched
      synchronized (jobs) {
        for (String serializedJob : jobs.values()) {
          Job job = null;
          try {
            job = JobParser.parseJob(serializedJob);
            User creator = userDirectoryService.loadUser(job.getCreator());
            Organization organization = organizationDirectoryService.getOrganization(job.getOrganization());
            securityService.setUser(creator);
            securityService.setOrganization(organization);
            if (Status.QUEUED.equals(job.getStatus())) {
              job.setStatus(Status.DISPATCHING);
              if (!dispatchJob(job)) {
                job.setStatus(Status.QUEUED);
              }
            }
          } catch (ServiceUnavailableException e) {
            job.setStatus(Status.FAILED);
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            logger.error("Unable to find a service for job " + job, cause);
          } catch (ServiceRegistryException e) {
            job.setStatus(Status.FAILED);
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            logger.error("Error dispatching job " + job, cause);
          } catch (IOException e) {
            throw new IllegalStateException("Error unmarshaling job", e);
          } catch (NotFoundException e) {
            throw new IllegalStateException("Creator organization not found", e);
          } catch (Throwable e) {
            logger.error("Error dispatching job " + job, e);
          } finally {
            try {
              jobs.put(job.getId(), JobParser.toXml(new JaxbJob(job)));
            } catch (IOException e) {
              throw new IllegalStateException("Error unmarshaling job", e);
            }
            securityService.setUser(null);
            securityService.setOrganization(null);
          }
        }
      }
    }
  }

  /** Shuts down this service registry, logging all jobs and their statuses. */
  public void deactivate() {
    dispatcher.shutdownNow();
    Map<Status, AtomicInteger> counts = new HashMap<Job.Status, AtomicInteger>();
    synchronized (jobs) {
      for (String serializedJob : jobs.values()) {
        Job job = null;
        try {
          job = JobParser.parseJob(serializedJob);
        } catch (IOException e) {
          throw new IllegalStateException("Error unmarshaling job", e);
        }
        if (counts.containsKey(job.getStatus())) {
          counts.get(job.getStatus()).incrementAndGet();
        } else {
          counts.put(job.getStatus(), new AtomicInteger(1));
        }
      }
    }
    StringBuilder sb = new StringBuilder("Abandoned:");
    for (Entry<Status, AtomicInteger> entry : counts.entrySet()) {
      sb.append(" " + entry.getValue() + " " + entry.getKey() + " jobs");
    }
    logger.info(sb.toString());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxLoads()
   */
  @Override
  public SystemLoad getMaxLoads() throws ServiceRegistryException {
    SystemLoad systemLoad = new SystemLoad();
    systemLoad.addNodeLoad(new NodeLoad(LOCALHOST, 0.0f, Runtime.getRuntime().availableProcessors()));
    return systemLoad;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxLoadOnNode(java.lang.String)
   */
  @Override
  public NodeLoad getMaxLoadOnNode(String host) throws ServiceRegistryException {
    if (hosts.containsKey(host)) {
      return new NodeLoad(host, 0.0f, hosts.get(host).getMaxLoad());
    }
    throw new ServiceRegistryException("Unable to find host " + host + " in service registry");
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public void sanitize(String serviceType, String host) {
    // TODO Auto-generated method stub
  }

  @Override
  public Job getCurrentJob() {
    return this.currentJob;
  }

  @Override
  public void setCurrentJob(Job job) {
    this.currentJob = job;
  }

  @Override
  public List<HostRegistration> getHostRegistrations() throws ServiceRegistryException {
    List<HostRegistration> hostList = new LinkedList<HostRegistration>();
    hostList.addAll(hosts.values());
    return hostList;
  }

  @Override
  public SystemLoad getCurrentHostLoads() {
    SystemLoad systemLoad = new SystemLoad();

    for (String host : hosts.keySet()) {
      NodeLoad node = new NodeLoad();
      node.setHost(host);
      for (ServiceRegistration service : services.get(host)) {
        if (service.isInMaintenanceMode() || !service.isOnline()) {
          continue;
        }
        Set<Job> hostJobs = jobHosts.get(service);
        float loadSum = 0.0f;
        if (hostJobs != null) {
          for (Job job : hostJobs) {
            if (job.getStatus() != null && JOB_STATUSES_INFLUENCING_LOAD_BALANCING.contains(job.getStatus())) {
              loadSum += job.getJobLoad();
            }
          }
        }
        node.setCurrentLoad(loadSum);
      }
      systemLoad.addNodeLoad(node);
    }
    return systemLoad;
  }

  @Override
  public void removeParentlessJobs(int lifetime) throws ServiceRegistryException {
    synchronized (jobs) {
      for (String serializedJob : jobs.values()) {
        Job job = null;
        try {
          job = JobParser.parseJob(serializedJob);
        } catch (IOException e) {
          throw new IllegalStateException("Error unmarshaling job", e);
        }

        Long parentJobId = job.getParentJobId();
        if (parentJobId == null | parentJobId < 1)
          jobs.remove(job.getId());
      }
    }
  }

  @Override
  public float getOwnLoad() {
    return getCurrentHostLoads().get(getRegistryHostname()).getCurrentLoad();
  }

  @Override
  public String getRegistryHostname() {
    return LOCALHOST;
  }

}
