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

import java.util.Collections.unmodifiableList
import org.opencastproject.job.api.Job.FailureReason.NONE

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

import java.net.URI
import java.util.ArrayList
import java.util.Date

open class JobImpl : Job {

    override var id: Long = 0
        private set
    override var creator: String? = null
    override var organization: String? = null
    override val version: Long
    override var jobType: String? = null
    override var operation: String? = null
    private var arguments: MutableList<String>? = ArrayList()
    override var status: Job.Status? = null
    override var failureReason: Job.FailureReason = NONE
        private set
    override val createdHost: String
    override var processingHost: String? = null
    override var dateCreated: Date? = null
    override var dateStarted: Date? = null
    override var dateCompleted: Date? = null
    override var queueTime: Long? = 0L
    override var runTime: Long? = 0L
    override var payload: String? = null
    override var parentJobId: Long? = null
    override val rootJobId: Long? = null
    override var isDispatchable = true
    override val uri: URI
    override var jobLoad: Float? = 1.0f

    override val signature: Int
        get() = if (arguments == null) jobType!!.hashCode() else jobType!!.hashCode() + arguments!!.hashCode()

    constructor() {}

    constructor(id: Long) {
        this.id = id
    }

    constructor(
            id: Long,
            creator: String,
            organization: String,
            version: Long,
            jobType: String,
            operation: String,
            arguments: List<String>?,
            status: Job.Status,
            createdHost: String,
            processingHost: String,
            dateCreated: Date,
            dateStarted: Date,
            dateCompleted: Date,
            queueTime: Long?,
            runTime: Long?,
            payload: String,
            parentJobId: Long?,
            rootJobId: Long?,
            dispatchable: Boolean,
            uri: URI,
            load: Float?) {
        this.id = id
        this.creator = creator
        this.organization = organization
        this.version = version
        this.jobType = jobType
        this.operation = operation
        if (arguments != null)
            this.arguments!!.addAll(arguments)
        this.status = status
        this.createdHost = createdHost
        this.processingHost = processingHost
        this.dateCreated = dateCreated
        this.dateStarted = dateStarted
        this.dateCompleted = dateCompleted
        this.queueTime = queueTime
        this.runTime = runTime
        this.payload = payload
        this.parentJobId = parentJobId
        this.rootJobId = rootJobId
        this.isDispatchable = dispatchable
        this.uri = uri
        this.jobLoad = load
    }

    override fun getArguments(): List<String> {
        return unmodifiableList(arguments!!)
    }

    override fun setArguments(arguments: List<String>?) {
        if (arguments != null)
            this.arguments = unmodifiableList(arguments)
    }

    override fun setStatus(status: Job.Status, reason: Job.FailureReason) {
        status = status
        this.failureReason = reason
    }

    override fun equals(o: Any?): Boolean {
        if (this === o)
            return true

        if (o == null || javaClass != o.javaClass)
            return false

        val job = o as JobImpl?

        return EqualsBuilder().append(id, job!!.id).append(isDispatchable, job.isDispatchable)
                .append(creator, job.creator).append(organization, job.organization)
                .append(jobType, job.jobType).append(operation, job.operation).append(arguments, job.arguments)
                .append(status, job.status).append(failureReason, job.failureReason).append(createdHost, job.createdHost)
                .append(processingHost, job.processingHost).append(dateCreated, job.dateCreated)
                .append(dateStarted, job.dateStarted).append(dateCompleted, job.dateCompleted)
                .append(queueTime, job.queueTime).append(runTime, job.runTime).append(payload, job.payload)
                .append(parentJobId, job.parentJobId).append(rootJobId, job.rootJobId)
                .append(uri, job.uri).append(jobLoad, job.jobLoad).isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder(17, 37).append(id).append(creator).append(organization).append(jobType)
                .append(operation).append(arguments).append(status).append(failureReason).append(createdHost)
                .append(processingHost).append(dateCreated).append(dateStarted).append(dateCompleted).append(queueTime)
                .append(runTime).append(payload).append(parentJobId).append(rootJobId).append(isDispatchable).append(uri)
                .append(jobLoad).toHashCode()
    }

    override fun toString(): String {
        return String.format("Job {id:%d, operation:%s, status:%s}", id, operation, status!!.toString())
    }
}
