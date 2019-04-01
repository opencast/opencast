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
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A record of a host.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "host", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "host", namespace = "http://serviceregistry.opencastproject.org")
class JaxbHostRegistration : HostRegistration {

    /**
     * The base URL for this host. The length was chosen this short because MySQL complains when trying to create an index
     * larger than this
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.getBaseUrl
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.setBaseUrl
     */
    @XmlElement(name = "base_url")
    override var baseUrl: String

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.getIpAddress
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.setIpAddress
     */
    @XmlElement(name = "address")
    override var ipAddress: String

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.getMemory
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.setMemory
     */
    @XmlElement(name = "memory")
    override var memory: Long = 0

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.getCores
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.setCores
     */
    @XmlElement(name = "cores")
    override var cores: Int = 0

    /**
     * The maximum load this host can run.  This is not necessarily 1-to-1 with the number of jobs.
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.getMaxLoad
     */
    @XmlElement(name = "max_load")
    override var maxLoad: Float = 0.toFloat()

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.isOnline
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.setOnline
     */
    @XmlElement(name = "online")
    override var isOnline: Boolean = false

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.isActive
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.setActive
     */
    @XmlElement(name = "active")
    override var isActive: Boolean = false

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.isMaintenanceMode
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.serviceregistry.api.HostRegistration.setMaintenanceMode
     */
    @XmlElement(name = "maintenance")
    override var isMaintenanceMode: Boolean = false

    /**
     * Creates a new host registration which is online and not in maintenance mode.
     */
    constructor() {
        this.isOnline = true
        this.isActive = true
        this.isMaintenanceMode = false
    }

    /**
     * Constructs a new HostRegistration with the parameters provided
     *
     * @param baseUrl
     * the base URL for this host
     * @param address
     * the IP address for this host
     * @param memory
     * the allocated memory of this host
     * @param cores
     * the available cores of this host
     * @param maxLoad
     * the maximum load that this host can support
     * @param online
     * whether the host is online and available for service
     * @param online
     * whether the host is in maintenance mode
     */
    constructor(baseUrl: String, address: String, memory: Long, cores: Int, maxLoad: Float, online: Boolean,
                maintenance: Boolean) {
        this.baseUrl = baseUrl
        this.ipAddress = address
        this.memory = memory
        this.cores = cores
        this.maxLoad = maxLoad
        this.isOnline = online
        this.isMaintenanceMode = maintenance
        this.isActive = true
    }

    /**
     * Creates a new JAXB host registration based on an existing host registration
     *
     * @param hostRegistration
     */
    constructor(hostRegistration: HostRegistration) {
        this.baseUrl = hostRegistration.baseUrl
        this.ipAddress = hostRegistration.ipAddress
        this.memory = hostRegistration.memory
        this.cores = hostRegistration.cores
        this.maxLoad = hostRegistration.maxLoad
        this.isOnline = hostRegistration.isOnline
        this.isActive = hostRegistration.isActive
        this.isMaintenanceMode = hostRegistration.isMaintenanceMode
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
        if (obj !is HostRegistration)
            return false
        val registration = obj as HostRegistration?
        return baseUrl == registration!!.baseUrl
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return baseUrl
    }

}
