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
package org.opencastproject.serviceregistry.api;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple and in-memory implementation of a the service registry intended for testing scenarios.
 */
public class ServiceRegistryInMemoryImpl implements ServiceRegistry {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryInMemoryImpl.class);

  /** Default dispatcher timeout (1 second) */
  public static final long DEFAULT_DISPATCHER_TIMEOUT = 100;

  /** Hostname for localhost */
  private static final String LOCALHOST = "localhost";

  /** The hosts */
  protected Map<String, Long> hosts = new HashMap<String, Long>();

  /** The service registrations */
  protected Map<String, List<ServiceRegistrationInMemoryImpl>> services = new HashMap<String, List<ServiceRegistrationInMemoryImpl>>();

  /** The serialized jobs */
  protected Map<Long, String> jobs = new HashMap<Long, String>();

  /** The thread pool to use for dispatching queued jobs. */
  protected ScheduledExecutorService dispatcher = Executors.newScheduledThreadPool(1);

  /** The job identifier */
  protected AtomicLong idCounter = new AtomicLong();

  /**
   * An (optional) security service. If set to a non-null value, this will be used to obtain the current user when
   * creating new jobs.
   */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  public ServiceRegistryInMemoryImpl(JobProducer service, SecurityService securityService,
          UserDirectoryService userDirectoryService, OrganizationDirectoryService organizationDirectoryService)
          throws ServiceRegistryException {
    if (service != null)
      registerService(service);
    this.securityService = securityService;
    this.userDirectoryService = userDirectoryService;
    this.organizationDirectoryService = organizationDirectoryService;
    this.dispatcher.scheduleWithFixedDelay(new JobDispatcher(), DEFAULT_DISPATCHER_TIMEOUT, DEFAULT_DISPATCHER_TIMEOUT,
            TimeUnit.MILLISECONDS);
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(java.lang.String, int)
   */
  @Override
  public void registerHost(String host, int maxConcurrentJobs) throws ServiceRegistryException {
    hosts.put(host, new Long(maxConcurrentJobs));
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
   * @param serviceType
   *          the service type
   * @return the service registration
   * @throws ServiceRegistryException
   */
  public ServiceRegistration registerService(JobProducer localService) throws ServiceRegistryException {

    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(LOCALHOST);
    if (servicesOnHost == null) {
      servicesOnHost = new ArrayList<ServiceRegistrationInMemoryImpl>();
      services.put(LOCALHOST, servicesOnHost);
    }

    ServiceRegistrationInMemoryImpl registration = new ServiceRegistrationInMemoryImpl(localService);
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
   *      java.util.List, java.lang.String)
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
   *      java.util.List, String, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, queueable, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(String, String, List, String, boolean, Job)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable,
          Job parentJob) throws ServiceRegistryException {
    if (getServiceRegistrationsByType(type).size() == 0)
      logger.warn("Service " + type + " not available");

    JaxbJob job = null;
    synchronized (this) {
      job = new JaxbJob(idCounter.addAndGet(1));
      if (securityService != null) {
        job.setCreator(securityService.getUser().getUserName());
        job.setOrganization(securityService.getOrganization().getId());
      }
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
    }

    synchronized (jobs) {
      try {
        jobs.put(job.getId(), JobParser.toXml(job));
      } catch (IOException e) {
        throw new IllegalStateException("Error serializing job " + job, e);
      }
    }
    return job;
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
  protected boolean dispatchJob(Job job) throws ServiceUnavailableException, ServiceRegistryException {
    List<ServiceRegistration> registrations = getServiceRegistrationsByLoad(job.getJobType());
    if (registrations.size() == 0)
      throw new ServiceUnavailableException("No service is available to handle jobs of type '" + job.getJobType() + "'");
    for (ServiceRegistration registration : registrations) {
      if (registration.isJobProducer()) {
        ServiceRegistrationInMemoryImpl inMemoryRegistration = (ServiceRegistrationInMemoryImpl) registration;
        JobProducer service = inMemoryRegistration.getService();
        if (!service.isReadyToAccept(job))
          continue;
        else if (service.acceptJob(job)) {
          return true;
        } else {
          continue;
        }
      } else {
        logger.warn("This implementation of the service registry doesn't support dispatching to remote services");
        // TODO: Add remote dispatching
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
    synchronized (jobs) {
      try {
        jobs.put(job.getId(), JobParser.toXml(job));
      } catch (IOException e) {
        throw new IllegalStateException("Error serializing job", e);
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
    throw new IllegalStateException("Operation not yet implemented");
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
  public long countByOperation(String serviceType, String operation, Status status) throws ServiceRegistryException {
    return count(serviceType, null, operation, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByHost(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  public long countByHost(String serviceType, String host, Status status) throws ServiceRegistryException {
    return count(serviceType, host, null, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, java.lang.String,
   *      java.lang.String, org.opencastproject.job.api.Job.Status)
   */
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getLoad()
   */
  @Override
  public SystemLoad getLoad() throws ServiceRegistryException {
    throw new IllegalStateException("Not yet implemented");
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
              jobs.put(job.getId(), JobParser.toXml(job));
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

  /**
   * Shuts down this service registry, logging all jobs and their statuses.
   */
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxConcurrentJobs()
   */
  @Override
  public int getMaxConcurrentJobs() throws ServiceRegistryException {
    return Integer.MAX_VALUE;
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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setCurrentJob(Job job) {
    // TODO Auto-generated method stub

  }

}
