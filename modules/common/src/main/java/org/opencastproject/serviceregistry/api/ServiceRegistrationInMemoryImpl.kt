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

import org.opencastproject.job.api.JobProducer

import java.util.Date

/**
 * Simple implementation of a service registration.
 */
class ServiceRegistrationInMemoryImpl : ServiceRegistration {

    /** Service type  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getServiceType
     */
    override var serviceType: String? = null
        protected set

    /** Host that is running the service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getHost
     */
    override var host: String? = null
        protected set

    /** Path to the service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getPath
     */
    override var path: String? = null
        protected set

    /** The service instance  */
    /**
     * Returns the actual service instance.
     *
     * @return the service
     */
    var service: JobProducer? = null
        protected set

    /** True if this service produces jobs  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isJobProducer
     */
    override var isJobProducer = true
        protected set

    /** True if this service is active  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isActive
     */
    override var isActive = true
        protected set

    /** True if this service is online  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isOnline
     */
    override var isOnline = true
        protected set

    /** True if this service in in maintenance mode  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isInMaintenanceMode
     */
    override var isInMaintenanceMode = true
        protected set

    /** Date from the last time the service has been put online  */
    /**
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getOnlineFrom
     */
    override val onlineFrom: Date? = null

    private val serviceState: ServiceState? = null

    override// TODO Auto-generated method stub
    val stateChanged: Date?
        get() = null

    override// TODO Auto-generated method stub
    val errorStateTrigger: Int
        get() = 0

    override// TODO Auto-generated method stub
    val warningStateTrigger: Int
        get() = 0

    /**
     * Creates a new service registration. The service is initially online and not in maintenance mode.
     *
     * @param type
     * the service type
     * @param host
     * the service host
     * @param path
     * the path to the service
     * @param jobProducer
     * `true` if the service is a job producer
     */
    constructor(type: String, host: String, path: String, jobProducer: Boolean) {
        this.serviceType = type
        this.host = host
        this.path = path
        this.isJobProducer = jobProducer
    }

    /**
     * Creates a new service registration. The service is initially online and not in maintenance mode.
     *
     * @param service
     * the local service instance
     * @param host
     * the host that the service is running on
     */
    constructor(service: JobProducer, host: String) {
        this.service = service
        this.serviceType = service.jobType
        this.isJobProducer = true
        this.host = host
    }

    /**
     * Sets the service's maintenance mode.
     *
     * @param maintenance
     * `true` if the service is in maintenance mode
     */
    fun setMaintenance(maintenance: Boolean) {
        this.isInMaintenanceMode = maintenance
    }

    override fun getServiceState(): ServiceState? {
        // TODO Auto-generated method stub
        return null
    }

    override fun toString(): String {
        return "$serviceType@$host"
    }

}
