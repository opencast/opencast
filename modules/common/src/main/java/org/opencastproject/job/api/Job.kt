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

import java.net.URI
import java.util.Date

/**
 * Represents a long running, asynchronous process. A Job may be used to track any task, whether it is queued to run in
 * the future, currently running, or has run in the past.
 */
interface Job {

    /**
     * Gets the job identifier.
     *
     * @return the identifier
     */
    val id: Long

    /**
     * Gets the username of the subject responsible for creating the job initially. This job will execute with this user's
     * roles and permissions.
     *
     * @return the username that created the job
     */
    var creator: String

    /**
     * Returns the identifier of the organization that the creator is associated with.
     *
     * @return the organization
     */
    var organization: String

    /**
     * Gets the version of this job. Each time the job is updated, the version number is incremented. If a process
     * attempts to save a job that has been updated in another thread or on another host while the job was in memory, an
     * optimistic locking exception will be thrown.
     *
     * @return the version number of this job
     */
    val version: Long

    /**
     * Gets the job type, which determines the type of service that runs the job.
     *
     * @return the job type
     */
    var jobType: String

    /**
     * The operation type, which can be used by the service responsible for the job to determine the service method to
     * execute.
     *
     * @return The operation
     */
    var operation: String

    /**
     * The arguments passed to the service and operation. Each argument must be serializable to a string.
     *
     * @return the arguments passed to the service operation
     */
    var arguments: List<String>

    /**
     * Gets the receipt's current [Status]
     *
     * @return the current status
     */
    var status: Status

    /**
     * In the case of failure, returns whether the failure had to do with data or with processing. Depending on the
     * reason, processing services might be marked not to accept new jobs.
     *
     * @return the failure reason
     */
    val failureReason: FailureReason

    /**
     * Gets the host that created this job.
     *
     * @return the server that originally created the job
     */
    val createdHost: String

    /**
     * Gets the host responsible for running this job.
     *
     * @return the server running the job, or null if the job hasn't yet started
     */
    var processingHost: String

    /**
     * The date this job was created.
     *
     * @return the date the job was created
     */
    var dateCreated: Date

    /**
     * The date this job was started. If the job was queued, this can be significantly later than the date created.
     *
     * @return the date the job was started
     */
    var dateStarted: Date

    /**
     * The number of milliseconds that this job has waited in a queue before execution. This value will be null if the job
     * has not yet started execution.
     *
     * @return the total run time
     */
    var queueTime: Long?

    /**
     * The number of milliseconds that this job took to execute. This value will be null if the job has not yet finished
     * execution.
     *
     * @return the total run time
     */
    var runTime: Long?

    /**
     * The date this job was completed
     *
     * @return the date completed
     */
    var dateCompleted: Date

    /**
     * Gets the serialized output that was produced by this job, or null if nothing was produced, or if it has yet to be
     * produced.
     *
     * @return the output of the job
     */
    var payload: String

    /**
     *
     * @return
     */
    val signature: Int

    /**
     * Gets the parent job identifier, or null if there is no parent.
     *
     * @return the parent identifier
     */
    var parentJobId: Long?

    /**
     * Gets the root job identifier, or null if this is the root job.
     *
     * @return the root job identifier
     */
    val rootJobId: Long?

    /**
     * Gets whether this job may be dispatched.
     *
     * @return whether the job can be queued for dispatch or not
     */
    var isDispatchable: Boolean

    /**
     * Gets the URI of this job, which can be used to check its status.
     *
     * @return the job's URI
     */
    val uri: URI

    /**
     * Gets the job's load.  For example, a job which uses four cores would have a load of 4.0. This should be roughly the
     * number of cores that this job occupies while running.
     *
     * @return the job's load
     */
    var jobLoad: Float?

    /** The status of the job that this receipt represents  */
    enum class Status {
        QUEUED, PAUSED, RUNNING, FINISHED, FAILED, DELETED, INSTANTIATED, DISPATCHING, RESTART, CANCELED, WAITING;

        /** Return if the job is terminated.  */
        val isTerminated: Boolean
            get() {
                when (this) {
                    CANCELED, DELETED, FAILED, FINISHED -> return true
                    else -> return false
                }
            }

        /** Check if the job is still active, e.g. RUNNING or QUEUED. This is the negation of [.isTerminated].  */
        val isActive: Boolean
            get() = !isTerminated
    }

    /** Reason for failure  */
    enum class FailureReason {
        NONE, DATA, PROCESSING
    }

    /**
     * Sets the receipt's current [Status] along with the [FailureReason] to indicate why - in the case of
     * failure - the job failed.
     *
     * @param status
     * the status to set
     * @param reason
     * failure reason
     */
    fun setStatus(status: Status, reason: FailureReason)

}
