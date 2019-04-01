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

import java.util.Date

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A record of a service that creates and manages receipts.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "service", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "service", namespace = "http://serviceregistry.opencastproject.org")
class JaxbServiceRegistration : ServiceRegistration {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getServiceType
     */
    /**
     * @param serviceType
     * the serviceType to set
     */
    @XmlElement(name = "type")
    override var serviceType: String

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getHost
     */
    /**
     * @param host
     * the host to set
     */
    @XmlElement(name = "host")
    override var host: String

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getPath
     */
    /**
     * @param path
     * the path to set
     */
    @XmlElement(name = "path")
    override var path: String

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isActive
     */
    @XmlElement(name = "active")
    override var isActive: Boolean = false

    @XmlElement(name = "online")
    protected var online: Boolean = false

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isInMaintenanceMode
     */
    /**
     * Sets the maintenance status of this service registration
     *
     * @param maintenanceMode
     */
    @XmlElement(name = "maintenance")
    override var isInMaintenanceMode: Boolean = false

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isJobProducer
     */
    /**
     * Sets whether this service registration is a job producer.
     *
     * @param jobProducer
     * the jobProducer to set
     */
    @XmlElement(name = "jobproducer")
    override var isJobProducer: Boolean = false

    /** The last time the service has been declared online  */
    /**
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getOnlineFrom
     */
    /**
     * Sets the last time the service has been declared online
     *
     * @param onlineFrom
     * the onlineFrom to set
     */
    @XmlElement(name = "onlinefrom")
    override var onlineFrom: Date

    /**
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getServiceState
     */
    /**
     * Sets the current state of the service.
     *
     * @param state
     * current state
     */
    @XmlElement(name = "service_state")
    override var serviceState: ServiceState

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getStateChanged
     */
    /**
     * Sets the last date when the state was changed
     *
     * @param stateChanged
     * last date
     */
    @XmlElement(name = "state_changed")
    override var stateChanged: Date

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getErrorStateTrigger
     */
    /**
     * Sets the job which triggered the last error state
     *
     * @param jobSignature
     * the job
     */
    @XmlElement(name = "error_state_trigger")
    override var errorStateTrigger: Int = 0

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.getWarningStateTrigger
     */
    /**
     * Sets the job which triggered the last warning state
     *
     * @param jobSignature
     * the job
     */
    @XmlElement(name = "warning_state_trigger")
    override var warningStateTrigger: Int = 0

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.ServiceRegistration.isOnline
     */
    /**
     * Sets the online status of this service registration
     *
     * @param online
     */
    override var isOnline: Boolean
        get() = online
        set(online) {
            if (online && !isOnline)
                onlineFrom = Date()
            this.online = online
        }

    /**
     * Creates a new service registration which is online and not in maintenance mode.
     */
    constructor() {
        this.online = true
        this.isActive = true
        this.isInMaintenanceMode = false
        this.onlineFrom = Date()
        this.serviceState = ServiceState.NORMAL
        this.stateChanged = Date()
    }

    /**
     * Creates a new JAXB annotated service registration based on an existing service registration
     *
     * @param serviceRegistration
     */
    constructor(serviceRegistration: ServiceRegistration) {
        this.host = serviceRegistration.host
        this.isJobProducer = serviceRegistration.isJobProducer
        this.isInMaintenanceMode = serviceRegistration.isInMaintenanceMode
        this.isActive = serviceRegistration.isActive
        this.online = serviceRegistration.isOnline
        this.onlineFrom = serviceRegistration.onlineFrom
        this.path = serviceRegistration.path
        this.serviceType = serviceRegistration.serviceType
        this.serviceState = serviceRegistration.serviceState
        this.stateChanged = serviceRegistration.stateChanged
        this.warningStateTrigger = serviceRegistration.warningStateTrigger
        this.errorStateTrigger = serviceRegistration.errorStateTrigger
    }

    /**
     * Creates a new service registration which is online and not in maintenance mode.
     *
     * @param host
     * the host
     * @param serviceType
     * the job type
     */
    constructor(serviceType: String, host: String, path: String) : this() {
        this.serviceType = serviceType
        this.host = host
        this.path = path
    }

    /**
     * Creates a new service registration which is online and not in maintenance mode.
     *
     * @param host
     * the host
     * @param serviceType
     * the job type
     * @param jobProducer
     */
    constructor(serviceType: String, host: String, path: String, jobProducer: Boolean) : this() {
        this.serviceType = serviceType
        this.host = host
        this.path = path
        this.isJobProducer = jobProducer
    }

    /**
     * Sets the current state of the service and the trigger Job. If the state is set to [ServiceState.WARNING] or
     * [ServiceState.ERROR] the triggered job will be set.
     *
     * @param state
     * the service state
     * @param triggerJobSignature
     * the triggered job signature
     */
    fun setServiceState(state: ServiceState, triggerJobSignature: Int) {

        serviceState = state
        stateChanged = Date()
        if (state == ServiceState.WARNING) {
            warningStateTrigger = triggerJobSignature
        } else if (state == ServiceState.ERROR) {
            errorStateTrigger = triggerJobSignature
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return toString().hashCode()
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (obj !is ServiceRegistration)
            return false
        val registration = obj as ServiceRegistration?
        return host == registration!!.host && serviceType == registration.serviceType
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return "$serviceType@$host"
    }

}
