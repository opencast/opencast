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

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * Statistics for a service registration.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "statistic", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "statistic", namespace = "http://serviceregistry.opencastproject.org")
class JaxbServiceStatistics : ServiceStatistics {

    /** The service registration  */
    @XmlElement
    protected var serviceRegistration: JaxbServiceRegistration

    /** The mean run time for jobs  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceStatistics.getMeanRunTime
     */
    /**
     * Sets the mean run time.
     *
     * @param meanRunTime
     * the mean run time.
     */
    @XmlAttribute(name = "meanruntime")
    override var meanRunTime: Long = 0

    /** The mean queue time for jobs  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceStatistics.getMeanQueueTime
     */
    /**
     * Sets the mean queue time.
     *
     * @param meanQueueTime
     * the mean queue time
     */
    @XmlAttribute(name = "meanqueuetime")
    override var meanQueueTime: Long = 0

    /** The number of finished jobs  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceStatistics.getFinishedJobs
     */
    /**
     * Sets the number of finished jobs
     *
     * @param finishedJobs
     * the number of finished jobs
     */
    @XmlAttribute(name = "finished")
    override var finishedJobs: Int = 0

    /** The number of currently running jobs  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceStatistics.getRunningJobs
     */
    /**
     * Sets the number of running jobs
     *
     * @param runningJobs
     * the number of running jobs
     */
    @XmlAttribute(name = "running")
    override var runningJobs: Int = 0

    /** The number of currently queued jobs  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceStatistics.getQueuedJobs
     */
    /**
     * Sets the number of queued jobs
     *
     * @param queuedJobs
     * the number of queued jobs
     */
    @XmlAttribute(name = "queued")
    override var queuedJobs: Int = 0

    /**
     * No-arg constructor needed by JAXB
     */
    constructor() {}

    /**
     * Constructs a new service statistics instance without statistics.
     *
     * @param serviceRegistration
     * the service registration
     */
    constructor(serviceRegistration: JaxbServiceRegistration) : super() {
        this.serviceRegistration = serviceRegistration
    }

    /**
     * Constructs a new service statistics instance without statistics.
     *
     * @param serviceRegistration
     * the service registration
     */
    constructor(serviceRegistration: ServiceRegistration) : super() {
        this.serviceRegistration = JaxbServiceRegistration(serviceRegistration)
    }

    /**
     * Constructs a new service statistics instance with statistics.
     *
     * @param serviceRegistration
     * the service registration
     * @param meanRunTime
     * @param meanQueueTime
     * @param runningJobs
     * @param queuedJobs
     */
    constructor(serviceRegistration: JaxbServiceRegistration, meanRunTime: Long, meanQueueTime: Long,
                runningJobs: Int, queuedJobs: Int, finishedJobs: Int) : this(serviceRegistration) {
        this.meanRunTime = meanRunTime
        this.meanQueueTime = meanQueueTime
        this.runningJobs = runningJobs
        this.finishedJobs = finishedJobs
        this.queuedJobs = queuedJobs
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceStatistics.getServiceRegistration
     */
    override fun getServiceRegistration(): ServiceRegistration {
        return serviceRegistration
    }

}
