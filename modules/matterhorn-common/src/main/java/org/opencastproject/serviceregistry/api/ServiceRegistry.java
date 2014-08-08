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

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.util.NotFoundException;

import java.util.List;

/**
 * Manages clustered services and the {@link Job}s they may create to enable asynchronous job handling.
 */
public interface ServiceRegistry {

  /**
   * Registers a host as a provider of Matterhorn services.
   *
   * @param host
   *          The base URL for this server
   * @param maxConcurrentJobs
   *          the maximum number of concurrent jobs this server can execute
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  void registerHost(String host, int maxConcurrentJobs) throws ServiceRegistryException;

  /**
   * Removes a Matterhorn server from service.
   *
   * @param host
   *          The base URL for this server
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  void unregisterHost(String host) throws ServiceRegistryException;

  /**
   * Enable an inactive host as a provider of Matterhorn services.
   *
   * @param host
   *          The base URL for this server
   *
   * @throws NotFoundException
   *           if the host does not exist
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  void enableHost(String host) throws ServiceRegistryException, NotFoundException;

  /**
   * Disables a Matterhorn server from service.
   *
   * @param host
   *          The base URL for this server
   * @throws NotFoundException
   *           if the host does not exist
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  void disableHost(String host) throws ServiceRegistryException, NotFoundException;

  /**
   * Returns the total number of jobs that can be handled by the currently registered hosts.
   *
   * @return the total number of jobs that can be processed concurrently
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  int getMaxConcurrentJobs() throws ServiceRegistryException;

  /**
   * Registers a host to handle a specific type of job
   *
   * @param serviceType
   *          The job type
   * @param host
   *          The base URL where the service that can handle this service type can be found
   * @param path
   *          The path to the service endpoint
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   * @return the service registration
   */
  ServiceRegistration registerService(String serviceType, String host, String path) throws ServiceRegistryException;

  /**
   * Registers a host to handle a specific type of job
   *
   * @param serviceType
   *          The service type
   * @param host
   *          The base URL where the service that can handle this job type can be found
   * @param path
   *          The path to the service endpoint
   * @param jobProducer
   *          Whether this service registration produces {@link Job}s to track long running operations
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   * @return the service registration
   */
  ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer)
          throws ServiceRegistryException;

  /**
   * Unregisters a host from handling a specific type of job
   *
   * @param serviceType
   *          The service type
   * @param host
   *          The base URL where the service that can handle this job type can be found
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  void unRegisterService(String serviceType, String host) throws ServiceRegistryException;

  /**
   * Sets a registered host's maintenance status
   *
   * @param host
   *          The base URL where the service that can handle this service type can be found
   * @param maintenance
   *          the new maintenance status for this service
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   * @throws NotFoundException
   *           if the host does not exist
   */
  void setMaintenanceStatus(String host, boolean maintenance) throws ServiceRegistryException, NotFoundException;

  /**
   * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
   * {@link #createJob(String, String, List, String, boolean)} with an empty argument list.
   * <p>
   * Note that this job will be linked to the current job as its parent. Use
   * {@link #createJob(String, String, List, String, boolean, Job)} and pass <code>null</code> as the job if you don't
   * need the link.
   * </p>
   *
   * @param type
   *          the type of service responsible for this job
   * @param operation
   *          the operation for this service to run
   * @return the job
   * @throws ServiceRegistryException
   *           if there is a problem creating the job
   */
  Job createJob(String type, String operation) throws ServiceRegistryException;

  /**
   * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
   * {@link #createJob(String, String, List, String, boolean)}.
   * <p>
   * Note that this job will be linked to the current job as its parent. Use
   * {@link #createJob(String, String, List, String, boolean, Job)} and pass <code>null</code> as the job if you don't
   * need the link.
   * </p>
   *
   * @param type
   *          the type of service responsible for this job
   * @param operation
   *          the operation for this service to run
   * @param arguments
   *          the arguments to the operation
   * @return the job
   * @throws ServiceRegistryException
   *           if there is a problem creating the job
   */
  Job createJob(String type, String operation, List<String> arguments) throws ServiceRegistryException;

  /**
   * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
   * {@link #createJob(String, String, List, String, boolean)}. The job will carry the given payload from the beginning.
   * <p>
   * Note that this job will be linked to the current job as its parent. Use
   * {@link #createJob(String, String, List, String, boolean, Job)} and pass <code>null</code> as the job if you don't
   * need the link.
   * </p>
   *
   * @param type
   *          the type of service responsible for this job
   * @param operation
   *          the operation for this service to run
   * @param arguments
   *          the arguments to the operation
   * @param payload
   *          an optional initial payload
   * @return the job
   * @throws ServiceRegistryException
   *           if there is a problem creating the job
   */
  Job createJob(String type, String operation, List<String> arguments, String payload) throws ServiceRegistryException;

  /**
   * Create and store a new job. If <code>enqueueImmediately</code> is true, the job will be in the
   * {@link Status#QUEUED} state and will be dispatched as soon as possible. Otherwise, it will be
   * {@link Status#INSTANTIATED}.
   * <p>
   * Note that this job will be linked to the current job as its parent. Use
   * {@link #createJob(String, String, List, String, boolean, Job)} and pass <code>null</code> as the job if you don't
   * need the link.
   * </p>
   *
   * @param type
   *          the type of service responsible for this job
   * @param operation
   *          the operation for this service to run
   * @param arguments
   *          the arguments to the operation
   * @param payload
   *          an optional initial payload
   * @param queueable
   *          whether the job can be enqueued for dispatch. If false, the job's initial state will be
   *          {@link Status#INSTANTIATED} and will not be dispatched.
   * @return the job
   * @throws ServiceRegistryException
   *           if there is a problem creating the job
   */
  Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable)
          throws ServiceRegistryException;

  /**
   * Create and store a new job. If <code>enqueueImmediately</code> is true, the job will be in the
   * {@link Status#QUEUED} state and will be dispatched as soon as possible. Otherwise, it will be
   * {@link Status#INSTANTIATED}.
   *
   * @param type
   *          the type of service responsible for this job
   * @param operation
   *          the operation for this service to run
   * @param arguments
   *          the arguments to the operation
   * @param payload
   *          an optional initial payload
   * @param queueable
   *          whether the job can be enqueued for dispatch. If false, the job's initial state will be
   *          {@link Status#INSTANTIATED} and will not be dispatched.
   * @param parentJob
   *          the parent job
   * @return the job
   * @throws ServiceRegistryException
   *           if there is a problem creating the job
   */
  Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable, Job parentJob)
          throws ServiceRegistryException;

  /**
   * Update the job in the database
   *
   * @param job
   * @return the updated job
   * @throws NotFoundException
   *           if the job does not exist
   * @throws ServiceRegistryException
   *           if there is a problem updating the job
   */
  Job updateJob(Job job) throws NotFoundException, ServiceRegistryException;

  /**
   * Gets a receipt by its ID, or null if not found
   *
   * @param id
   *          the job id
   * @return the job or null
   */
  Job getJob(long id) throws NotFoundException, ServiceRegistryException;

  /**
   * Gets the current running job
   *
   * @return the current job
   */
  Job getCurrentJob();

  /**
   * Sets the current running job
   *
   * @param job
   *          the current job
   */
  void setCurrentJob(Job job);

  /**
   * Gets the list of jobs that match the specified parameters.
   *
   * @param serviceType
   *          The jobs run by this type of service. If null, jobs from all hosts will be returned.
   * @param status
   *          The status of the jobs. If null, jobs in all status will be returned.
   * @return the jobs matching these criteria
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<Job> getJobs(String serviceType, Status status) throws ServiceRegistryException;

  /**
   * Get all child jobs from a job
   *
   * @param id
   *          the parent job id
   * @return a list of the child jobs ordered by execution
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<Job> getChildJobs(long id) throws NotFoundException, ServiceRegistryException;

  /**
   * Finds the service registrations for this kind of job, ordered by load (lightest to heaviest).
   *
   * @param serviceType
   *          The type of service that must be handled by the hosts
   * @return A list of hosts that handle this job type, in order of their running and queued job load
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException;

  /**
   * Finds the service registrations for this kind of job, including offline services and those in maintenance mode.
   *
   * @param serviceType
   *          The type of service that must be handled by the hosts
   * @return A list of hosts that handle this job type
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException;

  /**
   * Finds the service registrations on the given host, including offline services and those in maintenance mode.
   *
   * @param host
   *          The host
   * @return A list of service registrations on a single host
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException;

  /**
   * Finds a single service registration by host and type, even if the service is offline or in maintenance mode.
   *
   * @param serviceType
   *          The type of service
   * @param host
   *          the base URL of the host
   * @return The service registration, or null
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  ServiceRegistration getServiceRegistration(String serviceType, String host) throws ServiceRegistryException;

  /**
   * Finds all service registrations, including offline services and those in maintenance mode.
   *
   * @return A list of service registrations
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrations() throws ServiceRegistryException;

  /**
   * Finds all host registrations, including offline hosts and those in maintenance mode.
   *
   * @return A list of host registrations
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<HostRegistration> getHostRegistrations() throws ServiceRegistryException;

  /**
   * Gets performance and runtime statistics for each known service registration.
   *
   * @return the service statistics
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException;

  /**
   * Count the number of jobs of this type in this {@link Status} across all hosts.
   *
   * @param serviceType
   *          The jobs run by this type of service. If null, the returned count will refer to all types of jobs.
   * @param status
   *          The status of the receipts. If null, the returned count will refer to jobs in any status.
   * @return the number of jobs
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  long count(String serviceType, Status status) throws ServiceRegistryException;

  /**
   * Count the number of jobs running the given operation in this {@link Status}.
   *
   * @param serviceType
   *          The jobs run by this type of service
   * @param operation
   *          the operation
   * @param status
   *          The status of the jobs
   * @return the number of jobs
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  long countByOperation(String serviceType, String operation, Status status) throws ServiceRegistryException;

  /**
   * Count the number of jobs in this {@link Status} on this host
   *
   * @param serviceType
   *          The jobs run by this type of service
   * @param host
   *          The server that created and will be handling the job
   * @param status
   *          The status of the jobs
   * @return the number of jobs
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  long countByHost(String serviceType, String host, Status status) throws ServiceRegistryException;

  /**
   * Count the number of jobs executing the given operation in this {@link Status} on this host.
   *
   * @param serviceType
   *          The jobs run by this type of service
   * @param host
   *          The server that created and will be handling the job
   * @param operation
   *          the operation
   * @param status
   *          The status of the jobs
   * @return the number of jobs
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  long count(String serviceType, String host, String operation, Status status) throws ServiceRegistryException;

  /**
   * Get the load factors for each registered node.
   *
   * @return the load values
   *
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  SystemLoad getLoad() throws ServiceRegistryException;

  /**
   * Sets the given service to NORMAL state
   *
   * @param serviceType
   *          the service type
   * @param host
   *          the host
   * @throws NotFoundException
   *           if the service does not exist
   */
  void sanitize(String serviceType, String host) throws NotFoundException;

}
