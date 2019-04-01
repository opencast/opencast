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

import org.opencastproject.job.api.Job.Status
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.JobCanceledException
import org.opencastproject.util.NotFoundException

import com.entwinemedia.fn.data.Opt

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

/**
 * This class is a utility implementation that will wait for all given jobs to change their status to either one of:
 *
 *  * [Job.Status.FINISHED]
 *  * [Job.Status.FAILED]
 *  * [Job.Status.DELETED]
 *
 */
class JobBarrier
/**
 * Creates a wrapper for `job`, using `registry` to poll for the job outcome. The
 * `waiter` is the job which is waiting for the other jobs to finish.
 *
 * @param jobs
 * the job to poll
 * @param registry
 * the registry
 * @param pollingInterval
 * the time in miliseconds between two polling operations
 * @param waiter
 * the job waiting for the other jobs to finish
 */
@JvmOverloads constructor(waiter: Job?,
                          /** The service registry used to do the polling  */
                          private val serviceRegistry: ServiceRegistry?,
                          /** Time in milliseconds between two pools for the job status  */
                          private val pollingInterval: Long = DEFAULT_POLLING_INTERVAL, vararg jobs: Job = arrayOf()) {

    /** The job that's waiting  */
    private val waiterJobId: Opt<Long>

    /** The jobs to wait on  */
    private val jobs: MutableList<Job>

    /** An exception that might have been thrown while polling  */
    @Volatile
    private var pollingException: Throwable? = null

    /** The status map  */
    /**
     * Returns the resulting status map.
     *
     * @return the status of the individual jobs
     */
    /**
     * Sets the outcome of the various jobs that were monitored.
     *
     * @param status
     * the status
     */
    @Volatile
    var status: Result? = null
        internal set

    /**
     * Creates a barrier for `jobs`, using `registry` to poll for the outcome of the monitored jobs
     * using the default polling interval [.DEFAULT_POLLING_INTERVAL]. The `waiter` is the job which is
     * waiting for the other jobs to finish.
     *
     * @param registry
     * the registry
     * @param jobs
     * the jobs to monitor
     * @param waiter
     * the job waiting for the other jobs to finish
     */
    constructor(waiter: Job, registry: ServiceRegistry, vararg jobs: Job) : this(waiter, registry, DEFAULT_POLLING_INTERVAL, *jobs) {}

    init {
        if (serviceRegistry == null)
            throw IllegalArgumentException("Service registry must not be null")
        if (jobs == null)
            throw IllegalArgumentException("Jobs must not be null")
        if (pollingInterval < 0)
            throw IllegalArgumentException("Polling interval must be a positive number")
        if (waiter != null)
            this.waiterJobId = Opt.some(waiter.id)
        else
            this.waiterJobId = Opt.none()
        this.jobs = ArrayList(Arrays.asList(*jobs))
    }

    private fun suspendWaiterJob() {
        if (this.waiterJobId.isSome) {
            try {
                val waiter = serviceRegistry.getJob(waiterJobId.get())
                waiter.status = Job.Status.WAITING
                logger.debug("Job {} set to WAITING state.", waiter.id)
                this.serviceRegistry.updateJob(waiter)
            } catch (e: ServiceRegistryException) {
                logger.warn("Unable to put {} into a waiting state, this may cause a deadlock: {}", waiterJobId, e.message)
            } catch (e: NotFoundException) {
                logger.warn("Unable to put {} into a waiting state, job not found by the service registry.  This may cause a deadlock: {}", waiterJobId, e.message)
            }

        } else {
            logger.debug("No waiting job set, unable to put waiting job into waiting state")
        }
    }

    private fun wakeWaiterJob() {
        if (this.waiterJobId.isSome) {
            try {
                val waiter = serviceRegistry.getJob(waiterJobId.get())
                waiter.status = Job.Status.RUNNING
                logger.debug("Job {} wakened and set back to RUNNING state.", waiter.id)
                this.serviceRegistry.updateJob(waiter)
            } catch (e: ServiceRegistryException) {
                logger.warn("Unable to put {} into a waiting state, this may cause a deadlock: {}", waiterJobId, e.message)
            } catch (e: NotFoundException) {
                logger.warn("Unable to put {} into a waiting state, job not found by the service registry.  This may cause a deadlock: {}", waiterJobId, e.message)
            }

        } else {
            logger.debug("No waiting job set, unable to put waiting job into waiting state")
        }
    }

    /**
     * Waits for a status change on all jobs and returns. If waiting for the status exceeds a certain limit, the method
     * returns even if some or all of the jobs are not yet finished. The same is true if at least one of the jobs fails or
     * gets stopped or deleted.
     *
     * @param timeout
     * the maximum amount of time to wait
     * @throws IllegalStateException
     * if there are no jobs to wait for
     * @throws JobCanceledException
     * if one of the jobs was canceled
     */
    @Throws(JobCanceledException::class, IllegalStateException::class)
    @JvmOverloads
    fun waitForJobs(timeout: Long = 0): Result? {
        if (jobs.size == 0)
            return Result(HashMap())
        this.suspendWaiterJob()
        synchronized(this) {
            val updater = JobStatusUpdater(timeout)
            try {
                updater.start()
                wait()
            } catch (e: InterruptedException) {
                logger.debug("Interrupted while waiting for job")
            }

        }
        if (pollingException != null) {
            if (pollingException is JobCanceledException)
                throw pollingException as JobCanceledException?
            throw IllegalStateException(pollingException)
        }
        this.wakeWaiterJob()
        return status
    }

    /**
     * Adds the job to the list of jobs to wait for. An [IllegalStateException] is thrown if the barrier has already
     * been asked to wait for jobs by calling [.waitForJobs].
     *
     * @param job
     * the job
     * @throws IllegalStateException
     * if the barrier already started waiting
     */
    @Throws(IllegalStateException::class)
    fun addJob(job: Job?) {
        if (job == null)
            throw IllegalArgumentException("Job must not be null")
        jobs.add(job)
    }

    /** Thread that keeps polling for status changes.  */
    internal inner class JobStatusUpdater
    /**
     * Creates a new status updater that will wait for finished jobs. If `0` is passed in as the work time,
     * the updater will wait as long as it takes. Otherwise, it will stop after the indicated amount of time has passed.
     *
     * @param workTime
     * the work time
     */
    (
            /** Maximum wait in milliseconds or 0 for unlimited waiting  */
            private val workTime: Long) : Thread() {

        override fun run() {
            val endTime = if (workTime > 0) System.currentTimeMillis() + workTime else 0
            val finishedJobs = HashMap<Job, Job.Status>()
            while (true) {
                val time = System.currentTimeMillis()
                // Wait a little..
                try {
                    val timeToSleep = Math.min(pollingInterval, Math.abs(endTime - time))
                    Thread.sleep(timeToSleep)
                } catch (e: InterruptedException) {
                    logger.debug("Job polling thread was interrupted")
                    return
                }

                // Look at all jobs and make sure all of them have reached the expected status
                for (job in jobs) {
                    // Don't ask if we already know
                    if (!finishedJobs.containsKey(job)) {
                        // Get the job status from the service registry
                        try {
                            val processedJob = serviceRegistry.getJob(job.id)
                            val jobStatus = processedJob.status
                            when (jobStatus) {
                                Job.Status.CANCELED -> throw JobCanceledException(processedJob)
                                Job.Status.DELETED, Job.Status.FAILED, Job.Status.FINISHED -> {
                                    job.status = jobStatus
                                    job.payload = processedJob.payload
                                    finishedJobs[job] = jobStatus
                                }
                                Job.Status.PAUSED, Job.Status.QUEUED, Job.Status.RESTART, Job.Status.DISPATCHING, Job.Status.INSTANTIATED, Job.Status.RUNNING -> logger.trace("{} is still in the works", job)
                                Job.Status.WAITING -> logger.trace("{} is waiting", job)
                                else -> logger.error("Unhandled job status '{}' found", jobStatus)
                            }
                        } catch (e: NotFoundException) {
                            logger.warn("Error polling job {}: Not found!", job)
                            finishedJobs[job] = Job.Status.DELETED
                            pollingException = e
                            break
                        } catch (e: ServiceRegistryException) {
                            logger.warn("Error polling service registry for the status of {}: {}", job, e.message)
                        } catch (e: JobCanceledException) {
                            logger.warn("Job {} got canceled", job)
                            pollingException = e
                            updateAndNotify(finishedJobs)
                            return
                        } catch (t: Throwable) {
                            logger.error("An unexpected error occured while waiting for jobs", t)
                            pollingException = t
                            updateAndNotify(finishedJobs)
                            return
                        }

                    }

                    // Are we done already?
                    if (finishedJobs.size == jobs.size) {
                        updateAndNotify(finishedJobs)
                        return
                    } else if (workTime > 0 && endTime >= time) {
                        pollingException = InterruptedException("Timeout waiting for job processing")
                        updateAndNotify(finishedJobs)
                        return
                    }
                }
            }
        }

        /**
         * Notifies listeners about the status change.
         *
         * @param status
         * the status
         */
        private fun updateAndNotify(status: Map<Job, Job.Status>) {
            this@JobBarrier.status = Result(status)
            synchronized(this@JobBarrier) {
                this@JobBarrier.notifyAll()
            }
        }

    }

    /** Result of a waiting operation on a certain number of jobs.  */
    class Result
    /**
     * Creates a new job barrier result.
     *
     * @param status
     * the barrier outcome
     */
    (
            /** The outcome of this barrier  */
            /**
             * Returns the status details.
             *
             * @return the status details
             */
            val status: Map<Job, Job.Status>) {

        /**
         * Returns `true` if all jobs are in the `[Job.Status.FINISHED]` state.
         *
         * @return `true` if all jobs are finished
         */
        val isSuccess: Boolean
            get() {
                for (state in status.values) {
                    if (state != Job.Status.FINISHED)
                        return false
                }
                return true
            }
    }

    companion object {
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(JobBarrier::class.java!!)

        /** Default polling interval is 5 seconds  */
        val DEFAULT_POLLING_INTERVAL = 5000L
    }
}
/**
 * Creates a barrier without any jobs, using `registry` to poll for the outcome of the monitored jobs using
 * the default polling interval [.DEFAULT_POLLING_INTERVAL]. The `waiter` is the job which is waiting
 * for the other jobs to finish. Use [.addJob] to add jobs to monitor.
 *
 * @param registry
 * the registry
 * @param waiter
 * the job waiting for the other jobs to finish
 */
/**
 * Creates a wrapper for `job`, using `registry` to poll for the job outcome. The
 * `waiter` is the job which is waiting for the other jobs to finish.
 *
 * @param registry
 * the registry
 * @param pollingInterval
 * the time in miliseconds between two polling operations
 * @param waiter
 * the job waiting for the other jobs to finish
 */
/**
 * Waits for a status change and returns the new status.
 *
 * @return the status
 */
