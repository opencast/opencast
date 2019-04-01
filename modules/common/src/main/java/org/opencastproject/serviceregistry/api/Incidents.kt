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

import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.job.api.Incident
import org.opencastproject.job.api.Incident.Severity
import org.opencastproject.job.api.IncidentTree
import org.opencastproject.job.api.Job
import org.opencastproject.util.Log
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Tuple

import org.apache.commons.lang3.exception.ExceptionUtils

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Date

/** Create and record job incidents. Facade for [IncidentService].  */
class Incidents(private val sr: ServiceRegistry, private val `is`: IncidentService) {

    /**
     * Record an incident for a given job. This method is intended to record client incidents, i.e. incidents crafted by
     * the programmer.
     *
     * @param code
     * A code number. This incident factory method enforces an incident code schema of `job_type.code`
     * , e.g. `org.opencastproject.service.1511` . So instead of aligning
     * `job.getJobType()` and the incident's code prefix manually this is done automatically for you
     * by this method. See [org.opencastproject.job.api.Incident.getCode].
     * @see org.opencastproject.job.api.Incident
     */
    @JvmOverloads
    fun record(job: Job, severity: Severity, code: Int, params: Map<String, String> = NO_PARAMS,
               details: List<Tuple<String, String>> = NO_DETAILS) {
        try {
            `is`.storeIncident(job, Date(), job.jobType + "." + code, severity, params, details)
        } catch (e: IncidentServiceException) {
            logException(e)
        }

    }

    /**
     * Record a failure incident for a given job.
     *
     * @see .record
     * @see org.opencastproject.job.api.Incident
     */
    fun recordFailure(job: Job, code: Int) {
        record(job, Severity.FAILURE, code, NO_PARAMS, NO_DETAILS)
    }

    /**
     * Record a failure incident for a given job.
     *
     * @see .record
     * @see org.opencastproject.job.api.Incident
     */
    fun recordFailure(job: Job, code: Int, params: Map<String, String>) {
        record(job, Severity.FAILURE, code, params, NO_DETAILS)
    }

    /**
     * Record a failure incident for a given job.
     *
     * @see .record
     * @see org.opencastproject.job.api.Incident
     */
    fun recordFailure(job: Job, code: Int, details: List<Tuple<String, String>>) {
        record(job, Severity.FAILURE, code, NO_PARAMS, details)
    }

    /**
     * Record a failure incident for a given job.
     *
     * @see .record
     * @see org.opencastproject.job.api.Incident
     */
    fun recordFailure(job: Job, code: Int, params: Map<String, String>, details: List<Tuple<String, String>>) {
        record(job, Severity.FAILURE, code, params, details)
    }

    /**
     * Record a failure incident for a given job.
     *
     * @see .record
     * @see org.opencastproject.job.api.Incident
     */
    fun recordFailure(job: Job, code: Int, t: Throwable, details: List<Tuple<String, String>>) {
        recordFailure(job, code, t, NO_PARAMS, details)
    }

    /**
     * Record a failure incident for a given job.
     *
     * @see .record
     * @see org.opencastproject.job.api.Incident
     */
    fun recordFailure(job: Job, code: Int, t: Throwable, params: Map<String, String>,
                      details: List<Tuple<String, String>>) {
        val detailList = ArrayList(details)
        detailList.add(tuple("stack-trace", ExceptionUtils.getStackTrace(t)))
        record(job, Severity.FAILURE, code, params, detailList)
    }

    fun recordMigrationIncident(job: Job, error: String) {
        try {
            `is`.storeIncident(job, Date(), SYSTEM_MIGRATED_ERROR, Severity.FAILURE, Collections.singletonMap("error", error),
                    NO_DETAILS)
        } catch (e: IncidentServiceException) {
            logException(e)
        }

    }

    fun recordJobCreationIncident(job: Job, t: Throwable) {
        unhandledException(job, SYSTEM_JOB_CREATION_EXCEPTION, Severity.FAILURE, t)
    }

    /**
     * Record an incident for a given job caused by an uncatched exception. This method is intended to record incidents by
     * the job system itself, e.g. the job dispatcher.
     */
    fun unhandledException(job: Job, severity: Severity, t: Throwable) {
        unhandledException(job, SYSTEM_UNHANDLED_EXCEPTION, severity, t)
    }

    /**
     * Record an incident for a given job caused by an uncatched exception. This method is intended to record incidents by
     * the job system itself, e.g. the job dispatcher. Please note that an incident will *only* be recorded if none
     * of severity [org.opencastproject.job.api.Incident.Severity.FAILURE] has already been recorded by the job or
     * one of its child jobs. If no job with the given job id exists nothing happens.
     */
    fun unhandledException(jobId: Long, severity: Severity, t: Throwable) {
        try {
            unhandledException(sr.getJob(jobId), severity, t)
        } catch (ignore: NotFoundException) {
        } catch (e: ServiceRegistryException) {
            logException(e)
        }

    }

    /**
     * Record an incident for a given job caused by an uncatched exception. This method is intended to record incidents by
     * the job system itself, e.g. the job dispatcher.
     */
    private fun unhandledException(job: Job, code: String, severity: Severity, t: Throwable) {
        if (!alreadyRecordedFailureIncident(job.id)) {
            try {
                `is`.storeIncident(
                        job,
                        Date(),
                        code,
                        severity,
                        Collections.singletonMap("exception", ExceptionUtils.getMessage(t)),
                        Arrays.asList(tuple("job-type", job.jobType), tuple("job-operation", job.operation),
                                tuple("stack-trace", ExceptionUtils.getStackTrace(t))))
            } catch (e: IncidentServiceException) {
                logException(e)
            }

        }
    }

    private fun logException(t: Throwable) {
        log.error(t, "Error recording job incident. Log exception and move on.")
    }

    fun alreadyRecordedFailureIncident(jobId: Long): Boolean {
        try {
            return findFailure(`is`.getIncidentsOfJob(jobId, true))
        } catch (e: Exception) {
            return false
        }

    }

    companion object {

        private val log = Log.mk(Incident::class.java)

        /**
         * System error codes
         */
        private val SYSTEM_UNHANDLED_EXCEPTION = "org.opencastproject.system.unhandled-exception"
        private val SYSTEM_JOB_CREATION_EXCEPTION = "org.opencastproject.system.job-creation-exception"
        private val SYSTEM_MIGRATED_ERROR = "org.opencastproject.system.migrated-error"

        val NO_PARAMS = emptyMap<String, String>()
        val NO_DETAILS = emptyList<Tuple<String, String>>()

        internal fun findFailure(r: IncidentTree): Boolean {
            return mlist(r.incidents).exists(isFailure) || mlist(r.descendants).exists(findFailureFn)
        }

        internal val findFailureFn: Function<IncidentTree, Boolean> = object : Function<IncidentTree, Boolean>() {
            override fun apply(r: IncidentTree): Boolean {
                return findFailure(r)
            }
        }

        internal val isFailure: Function<Incident, Boolean> = object : Function<Incident, Boolean>() {
            override fun apply(i: Incident): Boolean {
                return i.severity == Severity.FAILURE
            }
        }
    }
}
/**
 * Record an incident for a given job. This method is intended to record client incidents, i.e. incidents crafted by
 * the programmer.
 *
 * @see .record
 * @see org.opencastproject.job.api.Incident
 */
