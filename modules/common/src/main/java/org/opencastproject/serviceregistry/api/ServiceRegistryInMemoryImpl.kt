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

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.job.api.JobImpl
import org.opencastproject.job.api.JobParser
import org.opencastproject.job.api.JobProducer
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.SystemLoad.NodeLoad
import org.opencastproject.util.NotFoundException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.LinkedList
import kotlin.collections.Map.Entry
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Simple and in-memory implementation of a the service registry intended for testing scenarios.  */
class ServiceRegistryInMemoryImpl @Throws(ServiceRegistryException::class)
constructor(service: JobProducer?, maxLoad: Float, securityService: SecurityService,
            userDirectoryService: UserDirectoryService, organizationDirectoryService: OrganizationDirectoryService,
            incidentService: IncidentService) : ServiceRegistry {

    /** The hosts  */
    protected var hosts: MutableMap<String, HostRegistrationInMemory> = HashMap()

    /** The service registrations  */
    protected var services: MutableMap<String, List<ServiceRegistrationInMemoryImpl>> = HashMap()

    /** The serialized jobs  */
    protected var jobs: MutableMap<Long, String> = HashMap()

    /** A mapping of services to jobs  */
    protected var jobHosts: MutableMap<ServiceRegistrationInMemoryImpl, Set<Job>> = HashMap()

    /** The thread pool to use for dispatching queued jobs.  */
    protected var dispatcher = Executors.newScheduledThreadPool(1)

    /** The job identifier  */
    protected var idCounter = AtomicLong()

    /** Holds the current running job  */
    override var currentJob: Job? = null

    /**
     * An (optional) security service. If set to a non-null value, this will be used to obtain the current user when
     * creating new jobs.
     */
    protected var securityService: SecurityService? = null

    /** The user directory service  */
    protected var userDirectoryService: UserDirectoryService? = null

    /** The organization directory service  */
    protected var organizationDirectoryService: OrganizationDirectoryService? = null

    protected var incidents: Incidents

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getActiveJobs
     */
    override val activeJobs: List<Job>
        @Throws(ServiceRegistryException::class)
        get() {
            val result = ArrayList<Job>()
            synchronized(jobs) {
                for (serializedJob in jobs.values) {
                    var job: Job? = null
                    try {
                        job = JobParser.parseJob(serializedJob)
                    } catch (e: IOException) {
                        throw IllegalStateException("Error unmarshaling job", e)
                    }

                    if (job!!.status.isActive)
                        result.add(job)
                }
            }
            return result
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getServiceRegistrations
     */
    override val serviceRegistrations: List<ServiceRegistration>
        @Throws(ServiceRegistryException::class)
        get() {
            val result = ArrayList<ServiceRegistration>()
            for (servicesPerHost in services.values) {
                result.addAll(servicesPerHost)
            }
            return result
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getServiceStatistics
     */
    override val serviceStatistics: List<ServiceStatistics>
        @Throws(ServiceRegistryException::class)
        get() = throw UnsupportedOperationException("Operation not yet implemented")

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getMaxLoads
     */
    override val maxLoads: SystemLoad
        @Throws(ServiceRegistryException::class)
        get() {
            val systemLoad = SystemLoad()
            systemLoad.addNodeLoad(NodeLoad(LOCALHOST, Runtime.getRuntime().availableProcessors().toFloat()))
            return systemLoad
        }

    override val hostRegistrations: List<HostRegistration>
        @Throws(ServiceRegistryException::class)
        get() {
            val hostList = LinkedList<HostRegistration>()
            hostList.addAll(hosts.values)
            return hostList
        }

    override val currentHostLoads: SystemLoad
        get() {
            val systemLoad = SystemLoad()

            for (host in hosts.keys) {
                val node = NodeLoad()
                node.host = host
                for (service in services[host]) {
                    if (service.isInMaintenanceMode || !service.isOnline) {
                        continue
                    }
                    val hostJobs = jobHosts[service]
                    var loadSum = 0.0f
                    if (hostJobs != null) {
                        for (job in hostJobs) {
                            if (job.status != null && JOB_STATUSES_INFLUENCING_LOAD_BALANCING.contains(job.status)) {
                                loadSum += job.jobLoad!!
                            }
                        }
                    }
                    node.loadFactor = node.loadFactor + loadSum
                }
                systemLoad.addNodeLoad(node)
            }
            return systemLoad
        }

    override val ownLoad: Float
        get() = currentHostLoads.get(registryHostname)!!.loadFactor

    override val registryHostname: String
        get() = LOCALHOST

    init {
        //Note: total memory here isn't really the correct value, but we just need something (preferably non-zero)
        registerHost(LOCALHOST, LOCALHOST, Runtime.getRuntime().totalMemory(), Runtime.getRuntime().availableProcessors(), maxLoad)
        if (service != null)
            registerService(service, maxLoad)
        this.securityService = securityService
        this.userDirectoryService = userDirectoryService
        this.organizationDirectoryService = organizationDirectoryService
        this.incidents = Incidents(this, incidentService)
        this.dispatcher.scheduleWithFixedDelay(JobDispatcher(), DEFAULT_DISPATCHER_TIMEOUT, DEFAULT_DISPATCHER_TIMEOUT,
                TimeUnit.MILLISECONDS)
    }

    @Throws(ServiceRegistryException::class)
    constructor(service: JobProducer, securityService: SecurityService,
                userDirectoryService: UserDirectoryService, organizationDirectoryService: OrganizationDirectoryService,
                incidentService: IncidentService) : this(service, Runtime.getRuntime().availableProcessors().toFloat(), securityService, userDirectoryService, organizationDirectoryService, incidentService) {
    }

    /**
     * This method shuts down the service registry.
     */
    fun dispose() {
        dispatcher.shutdownNow()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.enableHost
     */
    @Throws(ServiceRegistryException::class, NotFoundException::class)
    override fun enableHost(host: String) {
        if (hosts.containsKey(host)) {
            hosts[host].isActive = true
        } else {
            throw NotFoundException("The host named $host was not found")
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.disableHost
     */
    @Throws(ServiceRegistryException::class, NotFoundException::class)
    override fun disableHost(host: String) {
        if (hosts.containsKey(host)) {
            hosts[host].isActive = false
        } else {
            throw NotFoundException("The host named $host was not found")
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.registerHost
     */
    @Throws(ServiceRegistryException::class)
    override fun registerHost(host: String, address: String, memory: Long, cores: Int, maxLoad: Float) {
        val hrim = HostRegistrationInMemory(address, address, maxLoad, cores, memory)
        hosts[host] = hrim
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.unregisterHost
     */
    @Throws(ServiceRegistryException::class)
    override fun unregisterHost(host: String) {
        hosts.remove(host)
        services.remove(host)
    }

    /**
     * Method to register locally running services.
     *
     * @param localService
     * the service instance
     * @param maxLoad
     * the maximum load the host can support
     * @return the service registration
     * @throws ServiceRegistryException
     */
    @Throws(ServiceRegistryException::class)
    @JvmOverloads
    fun registerService(localService: JobProducer, maxLoad: Float = Runtime.getRuntime().availableProcessors().toFloat()): ServiceRegistration {
        val hrim = hosts[LOCALHOST]

        var servicesOnHost: MutableList<ServiceRegistrationInMemoryImpl>? = services[LOCALHOST]
        if (servicesOnHost == null) {
            servicesOnHost = ArrayList()
            services[LOCALHOST] = servicesOnHost
        }

        val registration = ServiceRegistrationInMemoryImpl(localService, hrim.baseUrl)
        registration.setMaintenance(false)
        servicesOnHost.add(registration)
        return registration
    }

    /**
     * Removes the job producer from the service registry.
     *
     * @param localService
     * the service
     * @throws ServiceRegistryException
     * if removing the service fails
     */
    @Throws(ServiceRegistryException::class)
    fun unregisterService(localService: JobProducer) {
        val servicesOnHost = services[LOCALHOST]
        if (servicesOnHost != null) {
            val s = localService as ServiceRegistrationInMemoryImpl
            servicesOnHost.remove(s)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.registerService
     */
    @Throws(ServiceRegistryException::class)
    override fun registerService(serviceType: String, host: String, path: String): ServiceRegistration {
        return registerService(serviceType, host, path, false)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.registerService
     */
    @Throws(ServiceRegistryException::class)
    override fun registerService(serviceType: String, host: String, path: String, jobProducer: Boolean): ServiceRegistration {

        val hostRegistration = hosts[host]
                ?: throw ServiceRegistryException(NotFoundException("Host $host was not found"))

        var servicesOnHost: MutableList<ServiceRegistrationInMemoryImpl>? = services[host]
        if (servicesOnHost == null) {
            servicesOnHost = ArrayList()
            services[host] = servicesOnHost
        }

        val registration = ServiceRegistrationInMemoryImpl(serviceType, host, path,
                jobProducer)
        servicesOnHost.add(registration)
        return registration
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.unRegisterService
     */
    @Throws(ServiceRegistryException::class)
    override fun unRegisterService(serviceType: String, host: String) {
        val servicesOnHost = services[host]
        if (servicesOnHost != null) {
            val ri = servicesOnHost.iterator()
            while (ri.hasNext()) {
                val registration = ri.next()
                if (serviceType == registration.serviceType)
                    ri.remove()
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.setMaintenanceStatus
     */
    @Throws(NotFoundException::class)
    override fun setMaintenanceStatus(host: String, maintenance: Boolean) {
        val servicesOnHost = services[host]
        if (!hosts.containsKey(host)) {
            throw NotFoundException("Host $host was not found")
        }
        hosts[host].isMaintenanceMode = maintenance
        if (servicesOnHost != null) {
            for (r in servicesOnHost) {
                r.setMaintenance(maintenance)
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String): Job {
        return createJob(type, operation, null, null, true)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, jobLoad: Float?): Job {
        return createJob(type, operation, null, null, true, 1.0f)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>): Job {
        return createJob(type, operation, arguments, null, true)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>, jobLoad: Float?): Job {
        return createJob(type, operation, arguments, null, true, jobLoad)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>, payload: String): Job {
        return createJob(type, operation, arguments, payload, true)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>, payload: String, jobLoad: Float?): Job {
        return createJob(type, operation, arguments, payload, true, jobLoad)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>?, payload: String?, queueable: Boolean): Job {
        return createJob(type, operation, arguments, payload, queueable, null, 1.0f)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>?, payload: String?, queueable: Boolean,
                           jobLoad: Float?): Job {
        return createJob(type, operation, arguments, payload, queueable, null, jobLoad)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>, payload: String, queueable: Boolean,
                           parentJob: Job): Job {
        return createJob(type, operation, arguments, payload, queueable, parentJob, 1.0f)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.createJob
     */
    @Throws(ServiceRegistryException::class)
    override fun createJob(type: String, operation: String, arguments: List<String>?, payload: String?, queueable: Boolean,
                           parentJob: Job?, jobLoad: Float?): Job {
        if (getServiceRegistrationsByType(type).size == 0)
            logger.warn("Service $type not available")

        var job: Job? = null
        synchronized(this) {
            job = JobImpl(idCounter.addAndGet(1))
            if (securityService != null) {
                job!!.creator = securityService!!.user.username
                job!!.organization = securityService!!.organization.id
            }
            job!!.dateCreated = Date()
            job!!.jobType = type
            job!!.operation = operation
            job!!.arguments = arguments
            job!!.payload = payload
            if (queueable)
                job!!.status = Status.QUEUED
            else
                job!!.status = Status.INSTANTIATED
            if (parentJob != null)
                job!!.parentJobId = parentJob.id
            job!!.jobLoad = jobLoad
        }

        synchronized(jobs) {
            try {
                jobs.put(job!!.id, JobParser.toXml(JaxbJob(job!!)))
            } catch (e: IOException) {
                throw IllegalStateException("Error serializing job " + job!!, e)
            }

        }
        return job
    }

    @Throws(NotFoundException::class, ServiceRegistryException::class)
    private fun removeJob(id: Long) {
        synchronized(jobs) {
            if (!jobs.containsKey(id))
                throw NotFoundException("No job with ID '$id' found")

            jobs.remove(id)
        }
    }

    @Throws(NotFoundException::class, ServiceRegistryException::class)
    override fun removeJobs(ids: List<Long>) {
        synchronized(jobs) {
            for (id in ids) {
                removeJob(id)
            }
        }
    }

    /**
     * Dispatches the job to the least loaded service or throws a `ServiceUnavailableException` if there is no
     * such service.
     *
     * @param job
     * the job to dispatch
     * @return whether the job was dispatched
     * @throws ServiceUnavailableException
     * if no service is available to dispatch the job
     * @throws ServiceRegistryException
     * if the service registrations are unavailable or dispatching of the job fails
     */
    @Throws(ServiceUnavailableException::class, ServiceRegistryException::class, UndispatchableJobException::class)
    protected fun dispatchJob(job: Job): Boolean {
        var job = job
        val registrations = getServiceRegistrationsByLoad(job.jobType)
        if (registrations.size == 0)
            throw ServiceUnavailableException("No service is available to handle jobs of type '" + job.jobType + "'")
        job.status = Status.DISPATCHING
        try {
            job = updateJob(job)
        } catch (e: NotFoundException) {
            throw ServiceRegistryException("Job not found!", e)
        }

        for (registration in registrations) {
            if (registration.isJobProducer && !registration.isInMaintenanceMode) {
                val inMemoryRegistration = registration as ServiceRegistrationInMemoryImpl
                val service = inMemoryRegistration.service

                // Add the job to the list of jobs so that it gets counted in the load.
                // This is the same way that the JPA impl does it
                var jobs: MutableSet<Job>? = jobHosts[inMemoryRegistration]
                if (jobs == null) {
                    jobs = LinkedHashSet()
                }
                jobs.add(job)
                jobHosts[inMemoryRegistration] = jobs

                if (!service!!.isReadyToAcceptJobs(job.operation)) {
                    jobs.remove(job)
                    jobHosts[inMemoryRegistration] = jobs
                    continue
                }
                if (!service.isReadyToAccept(job)) {
                    jobs.remove(job)
                    jobHosts[inMemoryRegistration] = jobs
                    continue
                }
                try {
                    job = updateJob(job)
                } catch (e: NotFoundException) {
                    jobs.remove(job)
                    jobHosts[inMemoryRegistration] = jobs
                    throw ServiceRegistryException("Job not found!", e)
                }

                service.acceptJob(job)
                return true
            } else if (!registration.isJobProducer) {
                logger.warn("This implementation of the service registry doesn't support dispatching to remote services")
                // TODO: Add remote dispatching
            } else {
                logger.warn("Service $registration is in maintenance mode")
            }
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.updateJob
     */
    @Throws(NotFoundException::class, ServiceRegistryException::class)
    override fun updateJob(job: Job?): Job {
        if (job == null)
            throw IllegalArgumentException("Job cannot be null")
        var updatedJob: Job? = null
        synchronized(jobs) {
            try {
                updatedJob = updateInternal(job)
                jobs.put(updatedJob!!.id, JobParser.toXml(JaxbJob(updatedJob!!)))
            } catch (e: IOException) {
                throw IllegalStateException("Error serializing job", e)
            }

        }
        return updatedJob
    }

    private fun updateInternal(job: Job): Job {
        val now = Date()
        val status = job.status
        if (job.dateCreated == null) {
            job.dateCreated = now
        }
        if (Status.RUNNING == status) {
            if (job.dateStarted == null) {
                job.dateStarted = now
                job.queueTime = now.time - job.dateCreated.time
            }
        } else if (Status.FAILED == status) {
            // failed jobs may not have even started properly
            job.dateCompleted = now
            if (job.dateStarted != null) {
                job.runTime = now.time - job.dateStarted.time
            }
        } else if (Status.FINISHED == status) {
            if (job.dateStarted == null) {
                // Some services (e.g. ingest) don't use job dispatching, since they start immediately and handle their own
                // lifecycle. In these cases, if the start date isn't set, use the date created as the start date
                job.dateStarted = job.dateCreated
            }
            job.dateCompleted = now
            job.runTime = now.time - job.dateStarted.time

            // Cleanup local list of jobs assigned to a specific service
            for ((_, value) in services) {
                for (srv in value) {
                    val jobs = jobHosts[srv]
                    if (jobs != null) {
                        val updatedJobs = HashSet<Job>()
                        for (savedJob in jobs) {
                            if (savedJob.id != job.id)
                                updatedJobs.add(savedJob)
                        }
                        jobHosts[srv] = updatedJobs
                    }
                }
            }
        }
        return job
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getJob
     */
    @Throws(NotFoundException::class, ServiceRegistryException::class)
    override fun getJob(id: Long): Job {
        synchronized(jobs) {
            val serializedJob = jobs[id] ?: throw NotFoundException(java.lang.Long.toString(id))
            try {
                return JobParser.parseJob(serializedJob)
            } catch (e: IOException) {
                throw IllegalStateException("Error unmarshaling job", e)
            }

        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getChildJobs
     */
    @Throws(ServiceRegistryException::class)
    override fun getChildJobs(id: Long): List<Job> {
        val result = ArrayList<Job>()
        synchronized(jobs) {
            for (serializedJob in jobs.values) {
                var job: Job? = null
                try {
                    job = JobParser.parseJob(serializedJob)
                } catch (e: IOException) {
                    throw IllegalStateException("Error unmarshaling job", e)
                }

                if (job!!.parentJobId == null)
                    continue
                if (job.parentJobId == id || job.rootJobId == id)
                    result.add(job)

                var parentJobId = job.parentJobId
                while (parentJobId != null && parentJobId > 0) {
                    try {
                        val parentJob = getJob(job.parentJobId!!)
                        if (parentJob.parentJobId == id) {
                            result.add(job)
                            break
                        }
                        parentJobId = parentJob.parentJobId
                    } catch (e: NotFoundException) {
                        throw ServiceRegistryException("Job from parent job id was not found!", e)
                    }

                }
            }
        }
        Collections.sort(result) { job1, job2 -> job1.dateCreated.compareTo(job1.dateCreated) }
        return result
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getJobs
     */
    @Throws(ServiceRegistryException::class)
    override fun getJobs(serviceType: String, status: Status): List<Job> {
        val result = ArrayList<Job>()
        synchronized(jobs) {
            for (serializedJob in jobs.values) {
                var job: Job? = null
                try {
                    job = JobParser.parseJob(serializedJob)
                } catch (e: IOException) {
                    throw IllegalStateException("Error unmarshaling job", e)
                }

                if (serviceType == job!!.jobType && status == job.status)
                    result.add(job)
            }
        }
        return result
    }

    @Throws(ServiceRegistryException::class)
    override fun getJobPayloads(operation: String): List<String> {
        val result = ArrayList<String>()
        for (serializedJob in jobs.values) {
            try {
                val job = JobParser.parseJob(serializedJob)
                if (operation == job.operation) {
                    result.add(job.payload)
                }
            } catch (e: IOException) {
                throw IllegalStateException("Error unmarshaling job", e)
            }

        }
        return result
    }

    override fun incident(): Incidents {
        return incidents
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getServiceRegistrationsByLoad
     */
    @Throws(ServiceRegistryException::class)
    override fun getServiceRegistrationsByLoad(serviceType: String): List<ServiceRegistration> {
        return getServiceRegistrationsByType(serviceType)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getServiceRegistrationsByType
     */
    @Throws(ServiceRegistryException::class)
    override fun getServiceRegistrationsByType(serviceType: String): List<ServiceRegistration> {
        val result = ArrayList<ServiceRegistration>()
        for (servicesPerHost in services.values) {
            for (r in servicesPerHost) {
                if (serviceType == r.serviceType)
                    result.add(r)
            }
        }
        return result
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getServiceRegistrationsByHost
     */
    @Throws(ServiceRegistryException::class)
    override fun getServiceRegistrationsByHost(host: String): List<ServiceRegistration> {
        val result = ArrayList<ServiceRegistration>()
        val servicesPerHost = services[host]
        if (servicesPerHost != null) {
            result.addAll(servicesPerHost)
        }
        return result
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getServiceRegistration
     */
    @Throws(ServiceRegistryException::class)
    override fun getServiceRegistration(serviceType: String, host: String): ServiceRegistration? {
        val servicesPerHost = services[host]
        if (servicesPerHost != null) {
            for (r in servicesPerHost) {
                if (serviceType == r.serviceType)
                    return r
            }
        }
        return null
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.countOfAbnormalServices
     */
    @Throws(ServiceRegistryException::class)
    override fun countOfAbnormalServices(): Long {
        throw UnsupportedOperationException("Operation not yet implemented")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.count
     */
    @Throws(ServiceRegistryException::class)
    override fun count(serviceType: String, status: Status): Long {
        return count(serviceType, null, null, status)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.countByOperation
     */
    @Throws(ServiceRegistryException::class)
    override fun countByOperation(serviceType: String, operation: String, status: Status): Long {
        return count(serviceType, null, operation, status)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.countByHost
     */
    @Throws(ServiceRegistryException::class)
    override fun countByHost(serviceType: String, host: String, status: Status): Long {
        return count(serviceType, host, null, status)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.count
     */
    @Throws(ServiceRegistryException::class)
    override fun count(serviceType: String?, host: String?, operation: String?, status: Status?): Long {
        var count = 0
        synchronized(jobs) {
            for (serializedJob in jobs.values) {
                var job: Job? = null
                try {
                    job = JobParser.parseJob(serializedJob)
                } catch (e: IOException) {
                    throw IllegalStateException("Error unmarshaling job", e)
                }

                if (serviceType != null && serviceType != job!!.jobType)
                    continue
                if (host != null && host != job!!.processingHost)
                    continue
                if (operation != null && operation != job!!.operation)
                    continue
                if (status != null && status != job!!.status)
                    continue
                count++
            }
        }
        return count.toLong()
    }

    /**
     * This dispatcher implementation will wake from time to time and check for new jobs. If new jobs are found, it will
     * dispatch them to the services as appropriate.
     */
    internal inner class JobDispatcher : Runnable {

        /**
         * {@inheritDoc}
         *
         * @see java.lang.Thread.run
         */
        override fun run() {

            // Go through the jobs and find those that have not yet been dispatched
            synchronized(jobs) {
                for (serializedJob in jobs.values) {
                    var job: Job? = null
                    try {
                        job = JobParser.parseJob(serializedJob)
                        val creator = userDirectoryService!!.loadUser(job!!.creator)
                        val organization = organizationDirectoryService!!.getOrganization(job.organization)
                        securityService!!.user = creator
                        securityService!!.organization = organization
                        if (Status.QUEUED == job.status) {
                            job.status = Status.DISPATCHING
                            if (!dispatchJob(job)) {
                                job.status = Status.QUEUED
                            }
                        }
                    } catch (e: ServiceUnavailableException) {
                        job!!.status = Status.FAILED
                        val cause = if (e.cause != null) e.cause else e
                        logger.error("Unable to find a service for job $job", cause)
                    } catch (e: ServiceRegistryException) {
                        job!!.status = Status.FAILED
                        val cause = if (e.cause != null) e.cause else e
                        logger.error("Error dispatching job $job", cause)
                    } catch (e: IOException) {
                        throw IllegalStateException("Error unmarshaling job", e)
                    } catch (e: NotFoundException) {
                        throw IllegalStateException("Creator organization not found", e)
                    } catch (e: Throwable) {
                        logger.error("Error dispatching job " + job!!, e)
                    } finally {
                        try {
                            jobs[job!!.id] = JobParser.toXml(JaxbJob(job))
                        } catch (e: IOException) {
                            throw IllegalStateException("Error unmarshaling job", e)
                        }

                        securityService!!.user = null
                        securityService!!.organization = null
                    }
                }
            }
        }
    }

    /** Shuts down this service registry, logging all jobs and their statuses.  */
    fun deactivate() {
        dispatcher.shutdownNow()
        val counts = HashMap<Job.Status, AtomicInteger>()
        synchronized(jobs) {
            for (serializedJob in jobs.values) {
                var job: Job? = null
                try {
                    job = JobParser.parseJob(serializedJob)
                } catch (e: IOException) {
                    throw IllegalStateException("Error unmarshaling job", e)
                }

                if (counts.containsKey(job!!.status)) {
                    counts[job.status].incrementAndGet()
                } else {
                    counts[job.status] = AtomicInteger(1)
                }
            }
        }
        val sb = StringBuilder("Abandoned:")
        for ((key, value) in counts) {
            sb.append(" $value $key jobs")
        }
        logger.info(sb.toString())
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistry.getMaxLoadOnNode
     */
    @Throws(ServiceRegistryException::class)
    override fun getMaxLoadOnNode(host: String): NodeLoad {
        if (hosts.containsKey(host)) {
            return NodeLoad(host, hosts[host].maxLoad)
        }
        throw ServiceRegistryException("Unable to find host $host in service registry")
    }

    /**
     * Sets the security service.
     *
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    override fun sanitize(serviceType: String, host: String) {
        // TODO Auto-generated method stub
    }

    @Throws(ServiceRegistryException::class)
    override fun removeParentlessJobs(lifetime: Int) {
        synchronized(jobs) {
            for (serializedJob in jobs.values) {
                var job: Job? = null
                try {
                    job = JobParser.parseJob(serializedJob)
                } catch (e: IOException) {
                    throw IllegalStateException("Error unmarshaling job", e)
                }

                val parentJobId = job!!.parentJobId
                if ((parentJobId == null) or (parentJobId < 1))
                    jobs.remove(job.id)
            }
        }
    }

    companion object {

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(ServiceRegistryInMemoryImpl::class.java!!)

        /** Default dispatcher timeout (1 second)  */
        val DEFAULT_DISPATCHER_TIMEOUT: Long = 100

        /** Hostname for localhost  */
        private val LOCALHOST = "localhost"

        /**
         * A static list of statuses that influence how load balancing is calculated
         */
        protected val JOB_STATUSES_INFLUENCING_LOAD_BALANCING: MutableList<Status>

        init {
            JOB_STATUSES_INFLUENCING_LOAD_BALANCING = ArrayList()
            JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.QUEUED)
            JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.RUNNING)
        }
    }

}
/**
 * Method to register locally running services.
 *
 * @param localService
 * the service instance
 * @return the service registration
 * @throws ServiceRegistryException
 */
