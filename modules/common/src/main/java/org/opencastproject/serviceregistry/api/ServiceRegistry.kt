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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.serviceregistry.api

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.serviceregistry.api.SystemLoad.NodeLoad
import org.opencastproject.util.NotFoundException

/** Manages clustered services and the [Job]s they may create to enable asynchronous job handling.  */
interface ServiceRegistry {

    /**
     * Returns the maximum load that can be handled by the currently registered hosts.
     * Note that this load is *not* 1-to-1 equivalent with number of jobs.  A job may take up more than one load.
     *
     * @return the total load that can be processed concurrently
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    val maxLoads: SystemLoad

    /**
     * Gets a map of hosts to the number of jobs currently loading that host
     *
     * @return the map of hosts to job counts
     */
    val currentHostLoads: SystemLoad

    /**
     * Gets the load value for the current host (ie, the host this service registry lives on
     *
     * @return the load value for this host
     */
    val ownLoad: Float

    /**
     * Gets the current running job
     *
     * @return the current job
     */
    /**
     * Sets the current running job
     *
     * @param job
     * the current job
     */
    var currentJob: Job

    /**
     * Get the list of active jobs.
     *
     * @return list of active jobs
     * @throws ServiceRegistryException if there is a problem accessing the service registry
     */
    val activeJobs: List<Job>

    /**
     * Finds all service registrations, including offline services and those in maintenance mode.
     *
     * @return A list of service registrations
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    val serviceRegistrations: List<ServiceRegistration>

    /**
     * Returns the service regstry's hostname.  This can be used to fetch the list of services on the local host.
     *
     * @return The hostname that the service registry is running on.
     */
    val registryHostname: String

    /**
     * Finds all host registrations, including offline hosts and those in maintenance mode.
     *
     * @return A list of host registrations
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    val hostRegistrations: List<HostRegistration>

    /**
     * Gets performance and runtime statistics for each known service registration.
     *
     * @return the service statistics
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    val serviceStatistics: List<ServiceStatistics>

    /**
     * Registers a host as a provider of Opencast services.
     *
     * @param host
     * The base URL for this server
     * @param address
     * The IP address of this host
     * @param memory
     * The allocated memory of this host
     * @param cores
     * The available cores of this host
     * @param maxLoad
     * the maximum load this host can support
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class)
    fun registerHost(host: String, address: String, memory: Long, cores: Int, maxLoad: Float)

    /**
     * Removes an Opencast server from service.
     *
     * @param host
     * The base URL for this server
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class)
    fun unregisterHost(host: String)

    /**
     * Enable an inactive host as a provider of Opencast services.
     *
     * @param host
     * The base URL for this server
     * @throws NotFoundException
     * if the host does not exist
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class, NotFoundException::class)
    fun enableHost(host: String)

    /**
     * Disables a Opencast server from service.
     *
     * @param host
     * The base URL for this server
     * @throws NotFoundException
     * if the host does not exist
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class, NotFoundException::class)
    fun disableHost(host: String)

    /**
     * Returns the maximum load that can be handled by a given node.
     * Note that this load is *not* 1-to-1 equivalent with number of jobs.  A job may take up more than one load.
     *
     * @param host
     * The base URL for this server
     *
     * @return the total load that can be processed concurrently on that node
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class, NotFoundException::class)
    fun getMaxLoadOnNode(host: String): NodeLoad

    /**
     * Registers a host to handle a specific type of job
     *
     * @param serviceType
     * The job type
     * @param host
     * The base URL where the service that can handle this service type can be found
     * @param path
     * The path to the service endpoint
     * @return the service registration
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class)
    fun registerService(serviceType: String, host: String, path: String): ServiceRegistration

    /**
     * Registers a host to handle a specific type of job
     *
     * @param serviceType
     * The service type
     * @param host
     * The base URL where the service that can handle this job type can be found
     * @param path
     * The path to the service endpoint
     * @param jobProducer
     * Whether this service registration produces [Job]s to track long running operations
     * @return the service registration
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class)
    fun registerService(serviceType: String, host: String, path: String, jobProducer: Boolean): ServiceRegistration

    /**
     * Unregisters a host from handling a specific type of job
     *
     * @param serviceType
     * The service type
     * @param host
     * The base URL where the service that can handle this job type can be found
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     */
    @Throws(ServiceRegistryException::class)
    fun unRegisterService(serviceType: String, host: String)

    /**
     * Sets a registered host's maintenance status
     *
     * @param host
     * The base URL where the service that can handle this service type can be found
     * @param maintenance
     * the new maintenance status for this service
     * @throws ServiceRegistryException
     * if communication with the service registry fails
     * @throws NotFoundException
     * if the host does not exist
     */
    @Throws(ServiceRegistryException::class, NotFoundException::class)
    fun setMaintenanceStatus(host: String, maintenance: Boolean)

    /**
     * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
     * [.createJob] with an empty argument list.
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String): Job

    /**
     * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
     * [.createJob] with an empty argument list.
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param jobLoad
     * the load caused by this job, roughly equivalent to the number of cores used this job
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, jobLoad: Float?): Job

    /**
     * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
     * [.createJob].
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>): Job

    /**
     * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
     * [.createJob].
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @param jobLoad
     * the load caused by this job, roughly equivalent to the number of cores used this job
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>, jobLoad: Float?): Job

    /**
     * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
     * [.createJob]. The job will carry the given payload from the beginning.
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @param payload
     * an optional initial payload
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>, payload: String): Job

    /**
     * Create and store a new job that will be dispatched as soon as possible. This is equivalent to calling
     * [.createJob]. The job will carry the given payload from the beginning.
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @param payload
     * an optional initial payload
     * @param jobLoad
     * the load caused by this job, roughly equivalent to the number of cores used this job
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>, payload: String, jobLoad: Float?): Job

    /**
     * Create and store a new job. If `enqueueImmediately` is true, the job will be in the
     * [Status.QUEUED] state and will be dispatched as soon as possible. Otherwise, it will be
     * [Status.INSTANTIATED].
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @param payload
     * an optional initial payload
     * @param queueable
     * whether the job can be enqueued for dispatch. If false, the job's initial state will be
     * [Status.INSTANTIATED] and will not be dispatched.
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>, payload: String, queueable: Boolean): Job

    /**
     * Create and store a new job. If `enqueueImmediately` is true, the job will be in the
     * [Status.QUEUED] state and will be dispatched as soon as possible. Otherwise, it will be
     * [Status.INSTANTIATED].
     *
     *
     * Note that this job will be linked to the current job as its parent. Use
     * [.createJob] and pass `null` as the job if you don't
     * need the link.
     *
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @param payload
     * an optional initial payload
     * @param queueable
     * whether the job can be enqueued for dispatch. If false, the job's initial state will be
     * [Status.INSTANTIATED] and will not be dispatched.
     * @param jobLoad
     * the load caused by this job, roughly equivalent to the number of cores used this job
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>, payload: String, queueable: Boolean, jobLoad: Float?): Job

    /**
     * Create and store a new job. If `enqueueImmediately` is true, the job will be in the
     * [Status.QUEUED] state and will be dispatched as soon as possible. Otherwise, it will be
     * [Status.INSTANTIATED].
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @param payload
     * an optional initial payload
     * @param queueable
     * whether the job can be enqueued for dispatch. If false, the job's initial state will be
     * [Status.INSTANTIATED] and will not be dispatched.
     * @param parentJob
     * the parent job
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>, payload: String, queueable: Boolean, parentJob: Job): Job

    /**
     * Create and store a new job. If `enqueueImmediately` is true, the job will be in the
     * [Status.QUEUED] state and will be dispatched as soon as possible. Otherwise, it will be
     * [Status.INSTANTIATED].
     *
     * @param type
     * the type of service responsible for this job
     * @param operation
     * the operation for this service to run
     * @param arguments
     * the arguments to the operation
     * @param payload
     * an optional initial payload
     * @param queueable
     * whether the job can be enqueued for dispatch. If false, the job's initial state will be
     * [Status.INSTANTIATED] and will not be dispatched.
     * @param parentJob
     * the parent job
     * @param jobLoad
     * the load caused by this job, roughly equivalent to the number of cores used this job
     * @return the job
     * @throws ServiceRegistryException
     * if there is a problem creating the job
     */
    @Throws(ServiceRegistryException::class)
    fun createJob(type: String, operation: String, arguments: List<String>, payload: String, queueable: Boolean, parentJob: Job,
                  jobLoad: Float?): Job

    /**
     * Update the job in the database
     *
     * @param job
     * @return the updated job
     * @throws NotFoundException
     * if the job does not exist
     * @throws ServiceRegistryException
     * if there is a problem updating the job
     */
    @Throws(NotFoundException::class, ServiceRegistryException::class)
    fun updateJob(job: Job): Job

    /**
     * Gets a receipt by its ID, or null if not found
     *
     * @param id
     * the job id
     * @return the job or null
     */
    @Throws(NotFoundException::class, ServiceRegistryException::class)
    fun getJob(id: Long): Job

    /**
     * Deletes the given jobs from the service registry
     *
     * @param ids
     * the job ids
     */
    @Throws(NotFoundException::class, ServiceRegistryException::class)
    fun removeJobs(ids: List<Long>)

    /**
     * Removes all jobs which do not have a parent job (except workflow instance jobs) and which have passed their
     * lifetime.
     *
     * @param lifetime
     * lifetime in days
     * @throws ServiceRegistryException
     * if removing the jobs fails
     */
    @Throws(ServiceRegistryException::class)
    fun removeParentlessJobs(lifetime: Int)

    /**
     * Gets the list of jobs that match the specified parameters.
     *
     * @param serviceType
     * The jobs run by this type of service. If null, jobs from all hosts will be returned.
     * @param status
     * The status of the jobs. If null, jobs in all status will be returned.
     * @return the jobs matching these criteria
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun getJobs(serviceType: String, status: Status): List<Job>

    /**
     * Return the payload of all jobs for a specified operation type.
     *
     * @param operation
     * Operation type to get payload for
     * @return Serialized workflows
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun getJobPayloads(operation: String): List<String>

    /**
     * Get all child jobs from a job
     *
     * @param id
     * the parent job id
     * @return a list of the child jobs ordered by execution
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun getChildJobs(id: Long): List<Job>

    /**
     * Return a facility to record job incidents.
     *
     * @see org.opencastproject.job.api.Incident
     */
    fun incident(): Incidents

    /**
     * Finds the service registrations for this kind of job, ordered by load (lightest to heaviest).
     *
     * @param serviceType
     * The type of service that must be handled by the hosts
     * @return A list of hosts that handle this job type, in order of their running and queued job load
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun getServiceRegistrationsByLoad(serviceType: String): List<ServiceRegistration>

    /**
     * Finds the service registrations for this kind of job, including offline services and those in maintenance mode.
     *
     * @param serviceType
     * The type of service that must be handled by the hosts
     * @return A list of hosts that handle this job type
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun getServiceRegistrationsByType(serviceType: String): List<ServiceRegistration>

    /**
     * Finds the service registrations on the given host, including offline services and those in maintenance mode.
     *
     * @param host
     * The host
     * @return A list of service registrations on a single host
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun getServiceRegistrationsByHost(host: String): List<ServiceRegistration>

    /**
     * Finds a single service registration by host and type, even if the service is offline or in maintenance mode.
     *
     * @param serviceType
     * The type of service
     * @param host
     * the base URL of the host
     * @return The service registration, or null
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun getServiceRegistration(serviceType: String, host: String): ServiceRegistration

    /**
     * Gets the count of the number of abnormal services across the whole system.
     *
     * @return the count of abnormal services
     * @throws ServiceRegistryException
     */
    @Throws(ServiceRegistryException::class)
    fun countOfAbnormalServices(): Long

    /**
     * Count the number of jobs that match the specified parameters.
     *
     * @param serviceType
     * The jobs run by this type of service. If null, the returned count will refer to all types of jobs.
     * @param status
     * The status of the receipts. If null, the returned count will refer to jobs in any status.
     * @return the number of jobs
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun count(serviceType: String, status: Status): Long

    /**
     * Count the number of jobs running the given operation in this [Status].
     *
     * @param serviceType
     * The jobs run by this type of service
     * @param operation
     * the operation
     * @param status
     * The status of the jobs
     * @return the number of jobs
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun countByOperation(serviceType: String, operation: String, status: Status): Long

    /**
     * Count the number of jobs in this [Status] on this host
     *
     * @param serviceType
     * The jobs run by this type of service
     * @param host
     * The server that created and will be handling the job
     * @param status
     * The status of the jobs
     * @return the number of jobs
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun countByHost(serviceType: String, host: String, status: Status): Long

    /**
     * Count the number of jobs executing the given operation in this [Status] on this host.
     *
     * @param serviceType
     * The jobs run by this type of service
     * @param host
     * The server that created and will be handling the job
     * @param operation
     * the operation
     * @param status
     * The status of the jobs
     * @return the number of jobs
     * @throws ServiceRegistryException
     * if there is a problem accessing the service registry
     */
    @Throws(ServiceRegistryException::class)
    fun count(serviceType: String, host: String, operation: String, status: Status): Long

    /**
     * Sets the given service to NORMAL state
     *
     * @param serviceType
     * the service type
     * @param host
     * the host
     * @throws NotFoundException
     * if the service does not exist
     */
    @Throws(NotFoundException::class)
    fun sanitize(serviceType: String, host: String)

}
