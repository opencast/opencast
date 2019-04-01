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

import com.entwinemedia.fn.Fn

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

import java.net.URI
import java.util.Date

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/** 1:1 serialization of a [Job].  */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "job", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "job", namespace = "http://job.opencastproject.org")
class JaxbJob
/** Default constructor needed by jaxb  */
() {

    @XmlAttribute
    private var id: Long = 0

    @XmlElement(name = "creator")
    private val creator: String

    @XmlElement(name = "organization")
    private val organization: String

    @XmlAttribute
    private val version: Long

    @XmlAttribute(name = "type")
    private val jobType: String

    @XmlElement(name = "url")
    private val uri: URI

    @XmlElement
    private val operation: String

    @XmlElement(name = "arg")
    @XmlElementWrapper(name = "args")
    private var arguments: List<String>? = null

    @XmlElement
    private val createdHost: String

    @XmlElement
    private val processingHost: String

    @XmlAttribute
    private val status: Job.Status

    @XmlElement
    private val dateCreated: Date

    @XmlElement
    private val dateStarted: Date

    @XmlElement
    private val dateCompleted: Date

    @XmlElement
    private val parentJobId: Long?

    @XmlElement
    private val rootJobId: Long?

    @XmlElement
    private val queueTime: Long?

    @XmlElement
    private val runTime: Long?

    @XmlElement
    private val payload: String

    @XmlElement
    private val dispatchable: Boolean

    @XmlElement(name = "jobLoad")
    private val jobLoad: Float?

    constructor(job: Job) : this() {
        this.id = job.id
        this.dateCompleted = job.dateCompleted
        this.dateCreated = job.dateCreated
        this.dateStarted = job.dateStarted
        this.queueTime = job.queueTime
        this.runTime = job.runTime
        this.version = job.version
        this.payload = job.payload
        this.processingHost = job.processingHost
        this.createdHost = job.createdHost
        this.id = job.id
        this.jobType = job.jobType
        this.operation = job.operation
        if (job.arguments != null)
            this.arguments = unmodifiableList(job.arguments)
        this.status = job.status
        this.parentJobId = job.parentJobId
        this.rootJobId = job.rootJobId
        this.dispatchable = job.isDispatchable
        this.uri = job.uri
        this.creator = job.creator
        this.organization = job.organization
        this.jobLoad = job.jobLoad
    }

    fun toJob(): Job {
        return JobImpl(id, creator, organization, version, jobType, operation, arguments, status, createdHost,
                processingHost, dateCreated, dateStarted, dateCompleted, queueTime, runTime, payload, parentJobId,
                rootJobId, dispatchable, uri, jobLoad)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o)
            return true

        if (o == null || javaClass != o.javaClass)
            return false

        val jaxbJob = o as JaxbJob?

        return EqualsBuilder().append(id, jaxbJob!!.id).append(version, jaxbJob.version)
                .append(dispatchable, jaxbJob.dispatchable).append(creator, jaxbJob.creator)
                .append(organization, jaxbJob.organization).append(jobType, jaxbJob.jobType).append(uri, jaxbJob.uri)
                .append(operation, jaxbJob.operation).append(arguments, jaxbJob.arguments)
                .append(createdHost, jaxbJob.createdHost).append(processingHost, jaxbJob.processingHost)
                .append(status, jaxbJob.status).append(dateCreated, jaxbJob.dateCreated)
                .append(dateStarted, jaxbJob.dateStarted).append(dateCompleted, jaxbJob.dateCompleted)
                .append(parentJobId, jaxbJob.parentJobId).append(rootJobId, jaxbJob.rootJobId)
                .append(queueTime, jaxbJob.queueTime).append(runTime, jaxbJob.runTime)
                .append(payload, jaxbJob.payload).append(jobLoad, jaxbJob.jobLoad)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder(17, 37).append(id).append(creator).append(organization).append(version).append(jobType)
                .append(uri).append(operation).append(arguments).append(createdHost).append(processingHost).append(status)
                .append(dateCreated).append(dateStarted).append(dateCompleted).append(parentJobId).append(rootJobId)
                .append(queueTime).append(runTime).append(payload).append(dispatchable).append(jobLoad)
                .toHashCode()
    }

    companion object {

        fun fnToJob(): Fn<JaxbJob, Job> {
            return object : Fn<JaxbJob, Job>() {
                override fun apply(jaxbJob: JaxbJob): Job {
                    return jaxbJob.toJob()
                }
            }
        }

        fun fnFromJob(): Fn<Job, JaxbJob> {
            return object : Fn<Job, JaxbJob>() {
                override fun apply(job: Job): JaxbJob {
                    return JaxbJob(job)
                }
            }
        }
    }
}
