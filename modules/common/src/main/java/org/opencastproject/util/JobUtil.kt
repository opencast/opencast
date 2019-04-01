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

package org.opencastproject.util

import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.util.data.Collections.map
import org.opencastproject.util.data.Collections.toArray
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Pred
import com.entwinemedia.fn.data.Opt

import org.apache.http.HttpResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Job related utility functions.  */
object JobUtil {
    /** The logger  */
    private val logger = LoggerFactory.getLogger(JobUtil::class.java!!)

    val jobFromHttpResponse: Function<HttpResponse, Option<Job>> = object : Function<HttpResponse, Option<Job>>() {
        override fun apply(response: HttpResponse): Option<Job> {
            try {
                return some(JobParser.parseJob(response.entity.content))
            } catch (e: Exception) {
                logger.error("Error parsing Job from HTTP response", e)
                return none()
            }

        }
    }

    /**
     * Update the job from the service registry and get its payload.
     *
     * @return the payload or none, if either to job cannot be found or if the job has no or an empty payload
     */
    @Throws(NotFoundException::class, ServiceRegistryException::class)
    fun getPayload(reg: ServiceRegistry, job: Job): Opt<String> {
        for (updated in update(reg, job)) {
            return Opt.nul(updated.payload)
        }
        return Opt.none()
    }

    /**
     * Get the latest state of a job. Does not modify the `job` parameter.
     *
     * @return the updated job or none, if it cannot be found
     */
    @Throws(ServiceRegistryException::class)
    fun update(reg: ServiceRegistry, job: Job): Opt<Job> {
        try {
            return Opt.some(reg.getJob(job.id))
        } catch (e: NotFoundException) {
            return Opt.none()
        }

    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval. The
     * `waiter` is the job which is waiting for the other jobs to finish.
     *
     * @param waiter
     * the job waiting for the other jobs to finish
     * @param reg
     * the service registry
     * @param pollingInterval
     * the time in miliseconds between two polling operations
     * @param timeout
     * the maximum amount of time to wait
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(waiter: Job?, reg: ServiceRegistry, pollingInterval: Long, timeout: Long,
                    vararg jobs: Job): JobBarrier.Result? {
        val barrier = JobBarrier(waiter, reg, pollingInterval, *jobs)
        return barrier.waitForJobs(timeout)
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval. The
     * `waiter` is the job which is waiting for the other jobs to finish.
     *
     * @param waiter
     * the job waiting for the other jobs to finish
     * @param reg
     * the service registry
     * @param timeout
     * the maximum amount of time to wait
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(waiter: Job?, reg: ServiceRegistry, timeout: Long, vararg jobs: Job): JobBarrier.Result? {
        return waitForJobs(waiter, reg, JobBarrier.DEFAULT_POLLING_INTERVAL, timeout, *jobs)
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval. The
     * `waiter` is the job which is waiting for the other jobs to finish.
     *
     * @param waiter
     * the job waiting for the other jobs to finish
     * @param reg
     * the service registry
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(waiter: Job?, reg: ServiceRegistry, vararg jobs: Job): JobBarrier.Result? {
        return waitForJobs(waiter, reg, 0L, *jobs)
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval.
     *
     * @param reg
     * the service registry
     * @param timeout
     * the maximum amount of time to wait
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(reg: ServiceRegistry, timeout: Long, vararg jobs: Job): JobBarrier.Result? {
        return waitForJobs(null, reg, timeout, *jobs)
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval.
     *
     * @param reg
     * the service registry
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(reg: ServiceRegistry, vararg jobs: Job): JobBarrier.Result? {
        return waitForJobs(null, reg, *jobs)
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval. The
     * `waiter` is the job which is waiting for the other jobs to finish.
     *
     * @param waiter
     * the job waiting for the other jobs to finish
     * @param reg
     * the service registry
     * @param timeout
     * the maximum amount of time to wait
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(waiter: Job, reg: ServiceRegistry, timeout: Long, jobs: Collection<Job>): JobBarrier.Result? {
        return waitForJobs(waiter, reg, timeout, *toArray(Job::class.java, jobs))
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval.
     *
     * @param reg
     * the service registry
     * @param timeout
     * the maximum amount of time to wait
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(reg: ServiceRegistry, timeout: Long, jobs: Collection<Job>): JobBarrier.Result? {
        return waitForJobs(null, reg, timeout, *toArray(Job::class.java, jobs))
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval. The
     * `waiter` is the job which is waiting for the other jobs to finish.
     *
     * @param waiter
     * the job waiting for the other jobs to finish
     * @param reg
     * the service registry
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(waiter: Job?, reg: ServiceRegistry, jobs: Collection<Job>): JobBarrier.Result? {
        return waitForJobs(waiter, reg, *toArray(Job::class.java, jobs))
    }

    /**
     * Waits for the result of a created barrier for `jobs`, using `registry` to poll for the
     * outcome of the monitored jobs using the default polling interval.
     *
     * @param reg
     * the service registry
     * @param jobs
     * the jobs to monitor
     * @return the job barrier result
     */
    fun waitForJobs(reg: ServiceRegistry, jobs: Collection<Job>): JobBarrier.Result? {
        return waitForJobs(null, reg, jobs)
    }

    /** Check if `job` is not done yet and wait in case.  */
    fun waitForJob(waiter: Job?, reg: ServiceRegistry, timeout: Option<Long>, job: Job): JobBarrier.Result? {
        val status = job.status
        // only create a barrier if the job is not done yet
        when (status) {
            Job.Status.CANCELED, Job.Status.DELETED, Job.Status.FAILED, Job.Status.FINISHED -> return JobBarrier.Result(map(tuple(job, status)))
            else -> {
                for (t in timeout)
                    return waitForJobs(waiter, reg, t!!, job)
                return waitForJobs(waiter, reg, job)
            }
        }
    }

    /** Check if `job` is not done yet and wait in case.  */
    fun waitForJob(reg: ServiceRegistry, timeout: Option<Long>, job: Job): JobBarrier.Result? {
        return waitForJob(null, reg, timeout, job)
    }

    /**
     * Check if `job` is not done yet and wait in case.
     *
     * @param waiter
     * the job waiting for the other jobs to finish
     * @param reg
     * the service registry
     * @param job
     * the job to monitor
     * @return the job barrier result
     */
    fun waitForJob(waiter: Job, reg: ServiceRegistry, job: Job): JobBarrier.Result? {
        return waitForJob(waiter, reg, none(0L), job)
    }

    /** Check if `job` is not done yet and wait in case.  */
    fun waitForJob(reg: ServiceRegistry, job: Job): JobBarrier.Result? {
        return waitForJob(null, reg, none(0L), job)
    }

    /**
     * Returns `true` if the job is ready to be dispatched.
     *
     * @param job
     * the job
     * @return `true` whether the job is ready to be dispatched
     * @throws IllegalStateException
     * if the job status is unknown
     */
    @Throws(IllegalStateException::class)
    fun isReadyToDispatch(job: Job): Boolean {
        when (job.status) {
            Job.Status.CANCELED, Job.Status.DELETED, Job.Status.FAILED, Job.Status.FINISHED -> return false
            Job.Status.DISPATCHING, Job.Status.INSTANTIATED, Job.Status.PAUSED, Job.Status.QUEUED, Job.Status.RESTART, Job.Status.RUNNING, Job.Status.WAITING -> return true
            else -> throw IllegalStateException("Found job in unknown state '" + job.status + "'")
        }
    }

    /**
     * [.waitForJob]
     * as a function.
     */
    fun waitForJob(reg: ServiceRegistry, timeout: Option<Long>): Function<Job, JobBarrier.Result> {
        return waitForJob(null, reg, timeout)
    }

    /**
     * [.waitForJob]
     * as a function.
     */
    fun waitForJob(waiter: Job?, reg: ServiceRegistry,
                   timeout: Option<Long>): Function<Job, JobBarrier.Result> {
        return object : Function<Job, JobBarrier.Result>() {
            override fun apply(job: Job): JobBarrier.Result? {
                return waitForJob(waiter, reg, timeout, job)
            }
        }
    }

    /** Wait for the job to complete and return the success value.  */
    fun waitForJobSuccess(waiter: Job?, reg: ServiceRegistry,
                          timeout: Option<Long>): Function<Job, Boolean> {
        return object : Function<Job, Boolean>() {
            override fun apply(job: Job): Boolean? {
                return waitForJob(waiter, reg, timeout, job)!!.isSuccess
            }
        }
    }

    /** Wait for the job to complete and return the success value.  */
    fun waitForJobSuccess(reg: ServiceRegistry, timeout: Option<Long>): Function<Job, Boolean> {
        return waitForJobSuccess(null, reg, timeout)
    }

    /**
     * Interpret the payload of a completed [Job] as a [MediaPackageElement]. Wait for the job to complete if
     * necessary.
     *
     */
    fun payloadAsMediaPackageElement(waiter: Job?,
                                     reg: ServiceRegistry): Function<Job, MediaPackageElement> {
        return object : Function.X<Job, MediaPackageElement>() {
            @Throws(MediaPackageException::class)
            public override fun xapply(job: Job): MediaPackageElement {
                waitForJob(waiter, reg, none(0L), job)
                return MediaPackageElementParser.getFromXml(job.payload)
            }
        }
    }

    /**
     * Interpret the payload of a completed [Job] as a [MediaPackageElement]. Wait for the job to complete if
     * necessary.
     */
    fun payloadAsMediaPackageElement(reg: ServiceRegistry): Function<Job, MediaPackageElement> {
        return payloadAsMediaPackageElement(null, reg)
    }

    /** Sum up the queue time of a list of jobs.  */
    fun sumQueueTime(jobs: List<Job>): Long {
        return `$`(jobs).foldl(0L, object : Fn2<Long, Job, Long>() {
            override fun apply(sum: Long?, job: Job): Long? {
                return sum!! + job.queueTime!!
            }
        })
    }

    /** Get all jobs that are not in state [org.opencastproject.job.api.Job.Status.FINISHED].  */
    fun getNonFinished(jobs: List<Job>): List<Job> {
        return `$`(jobs).filter(object : Pred<Job>() {
            override fun apply(job: Job): Boolean? {
                return job.status != Status.FINISHED
            }
        }).toList()
    }

}
