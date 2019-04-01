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

import com.entwinemedia.fn.Stream

import java.util.ArrayList

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A wrapper for job collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "jobs", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "jobs", namespace = "http://job.opencastproject.org")
class JaxbJobList {
    /** A list of jobs  */
    @XmlElement(name = "job")
    protected var jobs: MutableList<JaxbJob> = ArrayList()

    constructor() {}

    constructor(job: JaxbJob) {
        this.jobs.add(job)
    }

    constructor(jobs: Collection<Job>?) {
        if (jobs != null) {
            for (job in jobs) {
                add(JaxbJob(job))
            }
        }
    }

    /**
     * @return the jobs
     */
    fun getJobs(): List<JaxbJob> {
        return jobs
    }

    /**
     * @param jobs
     * the jobs to set
     */
    fun setJobs(jobs: List<Job>) {
        this.jobs = Stream.`$`(jobs).map(JaxbJob.fnFromJob()).toList()
    }

    fun add(job: JaxbJob) {
        jobs.add(job)
    }
}
