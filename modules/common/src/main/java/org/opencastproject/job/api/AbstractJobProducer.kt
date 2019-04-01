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

package org.opencastproject.job.api

import com.entwinemedia.fn.data.Opt.none
import com.entwinemedia.fn.data.Opt.some
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace
import org.opencastproject.util.OsgiUtil.getOptContextProperty

import org.opencastproject.job.api.Incident.Severity
import org.opencastproject.job.api.Job.Status
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.Incidents
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.serviceregistry.api.SystemLoad.NodeLoad
import org.opencastproject.serviceregistry.api.UndispatchableJobException
import org.opencastproject.util.JobCanceledException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.functions.Strings

import com.entwinemedia.fn.data.Opt

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DecimalFormat
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This class serves as a convenience for services that implement the [JobProducer] api to deal with handling long
 * running, asynchronous operations.
 */
abstract class AbstractJobProducer
/**
 * Creates a new abstract job producer for jobs of the given type.
 *
 * @param jobType
 * the job type
 */
(jobType: String) : JobProducer {

    /** Whether to accept a job whose load exceeds the host’s max load  */
    protected var acceptJobLoadsExeedingMaxLoad = DEFAULT_ACCEPT_JOB_LOADS_EXCEEDING

    /** The types of job that this producer can handle  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.JobProducer.getJobType
     */
    override var jobType: String? = null
        protected set

    /** To enable threading when dispatching jobs  */
    protected var executor = Executors.newCachedThreadPool()

    /**
     * Returns a reference to the service registry.
     *
     * @return the service registry
     */
    abstract val serviceRegistry: ServiceRegistry

    /**
     * Returns a reference to the security service
     *
     * @return the security service
     */
    protected abstract val securityService: SecurityService

    /**
     * Returns a reference to the user directory service
     *
     * @return the user directory service
     */
    protected abstract val userDirectoryService: UserDirectoryService

    /**
     * Returns a reference to the organization directory service.
     *
     * @return the organization directory service
     */
    protected abstract val organizationDirectoryService: OrganizationDirectoryService

    /**
     * OSGI activate method.
     *
     * @param cc
     * OSGI component context
     */
    open fun activate(cc: ComponentContext) {
        acceptJobLoadsExeedingMaxLoad = getOptContextProperty(cc, ACCEPT_JOB_LOADS_EXCEEDING_PROPERTY).map(Strings.toBool)
                .getOrElse(DEFAULT_ACCEPT_JOB_LOADS_EXCEEDING)
    }

    init {
        this.jobType = jobType
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.JobProducer.countJobs
     */
    @Throws(ServiceRegistryException::class)
    override fun countJobs(status: Status?): Long {
        if (status == null)
            throw IllegalArgumentException("Status must not be null")
        return serviceRegistry.count(jobType, status)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.JobProducer.acceptJob
     */
    @Throws(ServiceRegistryException::class)
    override fun acceptJob(job: Job) {
        val runningJob: Job
        try {
            job.status = Job.Status.RUNNING
            runningJob = serviceRegistry.updateJob(job)
        } catch (e: NotFoundException) {
            throw IllegalStateException(e)
        }

        executor.submit(JobRunner(runningJob, serviceRegistry.currentJob))
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.JobProducer.isReadyToAcceptJobs
     */
    @Throws(ServiceRegistryException::class)
    override fun isReadyToAcceptJobs(operation: String): Boolean {
        return true
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.JobProducer.isReadyToAccept
     */
    @Throws(ServiceRegistryException::class, UndispatchableJobException::class)
    override fun isReadyToAccept(job: Job): Boolean {
        if (jobType != job.jobType) {
            logger.debug("Invalid job type submitted: {}", job.jobType)
            return false
        }
        val maxload: NodeLoad
        try {
            maxload = serviceRegistry.getMaxLoadOnNode(serviceRegistry.registryHostname)
        } catch (e: NotFoundException) {
            throw ServiceRegistryException(e)
        }

        // Note: We are not adding the job load in the next line because it is already accounted for in the load values we
        // get back from the service registry.
        var currentLoad = serviceRegistry.ownLoad
        logger.debug("{} Current load on this host: {}, job's load: {}, job's status: {}, max load: {}",
                Thread.currentThread().id, currentLoad, job.jobLoad, job.status.name,
                maxload.loadFactor)
        // Add the current job load to compare below
        currentLoad += job.jobLoad!!

        /* Note that this first clause looks at the *job's*, the other two look at the *node's* load
     * We're assuming that if this case is true, then we're also the most powerful node in the system for this service,
     * per the current job dispatching code in ServiceRegistryJpaImpl */
        if (job.jobLoad > maxload.loadFactor && acceptJobLoadsExeedingMaxLoad) {
            logger.warn(
                    "{} Accepting job {} of type {} with load {} even though load of {} is above this node's limit of {}.",
                    Thread.currentThread().id, job.id, job.jobType, df.format(job.jobLoad),
                    df.format(currentLoad.toDouble()), df.format(maxload.loadFactor.toDouble()))
            logger.warn("This is a configuration issue that you should resolve in a production system!")
            return true
        } else if (currentLoad > maxload.loadFactor) {
            logger.debug(
                    "{} Declining job {} of type {} with load {} because load of {} would exceed this node's limit of {}.",
                    Thread.currentThread().id, job.id, job.jobType, df.format(job.jobLoad),
                    df.format(currentLoad.toDouble()), df.format(maxload.loadFactor.toDouble()))
            return false
        } else {
            logger.debug("{} Accepting job {} of type {} with load {} because load of {} is within this node's limit of {}.",
                    Thread.currentThread().id, job.id, job.jobType, df.format(job.jobLoad),
                    df.format(currentLoad.toDouble()), df.format(maxload.loadFactor.toDouble()))
            return true
        }
    }


    /**
     * Private utility to update and optionally fail job, called from a finally block.
     *
     * @param job
     * to be updated, may be null
     */
    protected fun finallyUpdateJob(job: Job?) {
        if (job == null) {
            return
        }

        if (Job.Status.FINISHED != job.status) {
            job.status = Job.Status.FAILED
        }
        try {
            serviceRegistry.updateJob(job)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    /** Shorthand for [.getServiceRegistry].incident()  */
    fun incident(): Incidents {
        return serviceRegistry.incident()
    }

    /**
     * Asks the overriding class to process the arguments using the given operation. The result will be added to the
     * associated job as the payload.
     *
     * @param job
     * the job to process
     * @return the operation result
     * @throws Exception
     */
    @Throws(Exception::class)
    protected abstract fun process(job: Job): String

    /** A utility class to run jobs  */
    internal inner class JobRunner
    /**
     * Constructs a new job runner
     *
     * @param job
     * the job to run
     * @param currentJob
     * the current running job
     */
    (job: Job, currentJob: Job?) : Callable<Void> {

        /** The job to dispatch  */
        private val jobId: Long

        /** The current job  */
        private val currentJobId: Opt<Long>

        init {
            jobId = job.id
            if (currentJob != null) {
                currentJobId = some(currentJob.id)
            } else {
                currentJobId = none()
            }
        }

        @Throws(Exception::class)
        override fun call(): Void? {
            val securityService = securityService
            val serviceRegistry = serviceRegistry
            val jobBeforeProcessing = serviceRegistry.getJob(jobId)

            if (currentJobId.isSome)
                serviceRegistry.currentJob = serviceRegistry.getJob(currentJobId.get())

            val organization = organizationDirectoryService
                    .getOrganization(jobBeforeProcessing.organization)
            securityService.organization = organization
            val user = userDirectoryService.loadUser(jobBeforeProcessing.creator)
            securityService.user = user

            try {
                val payload = process(jobBeforeProcessing)
                handleSuccessfulProcessing(payload)
            } catch (t: Throwable) {
                handleFailedProcessing(t)
            } finally {
                serviceRegistry.currentJob = null
                securityService.user = null
                securityService.organization = null
            }

            return null
        }

        @Throws(Exception::class)
        private fun handleSuccessfulProcessing(payload: String) {
            // The job may gets updated internally during processing. It therefore needs to be reload from the service
            // registry in order to prevent inconsistencies.
            val jobAfterProcessing = serviceRegistry.getJob(jobId)
            jobAfterProcessing.payload = payload
            jobAfterProcessing.status = Status.FINISHED
            serviceRegistry.updateJob(jobAfterProcessing)
        }

        @Throws(Exception::class)
        private fun handleFailedProcessing(t: Throwable) {
            if (t is JobCanceledException) {
                logger.info(t.message)
            } else {
                var jobAfterProcessing = serviceRegistry.getJob(jobId)
                jobAfterProcessing.status = Status.FAILED
                jobAfterProcessing = serviceRegistry.updateJob(jobAfterProcessing)
                serviceRegistry.incident().unhandledException(jobAfterProcessing, Severity.FAILURE, t)
                logger.error("Error handling operation '{}': {}", jobAfterProcessing.operation, getStackTrace(t))
                if (t is ServiceRegistryException)
                    throw t
            }
        }

    }

    companion object {

        /** The logger  */
        internal val logger = LoggerFactory.getLogger(AbstractJobProducer::class.java!!)

        /** The default value whether to accept a job whose load exceeds the host’s max load  */
        val DEFAULT_ACCEPT_JOB_LOADS_EXCEEDING = true

        /**
         * The key to look for in the service configuration file to override the [DEFAULT_ACCEPT_JOB_LOADS_EXCEEDING]
         */
        val ACCEPT_JOB_LOADS_EXCEEDING_PROPERTY = "org.opencastproject.job.load.acceptexceeding"

        /** The formatter for load values  */
        private val df = DecimalFormat("#.#")
    }
}
