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

package org.opencastproject.security.api

import org.opencastproject.util.EqualsUtil

import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.Map.Entry

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlID
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue

/**
 * An organization that is hosted on this Opencast instance.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "organization", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "organization", namespace = "http://org.opencastproject.security")
open class JaxbOrganization : Organization {

    /** The organizational identifier  */
    /**
     * @see org.opencastproject.security.api.Organization.getId
     */
    @XmlID
    @XmlAttribute
    override var id: String? = null
        protected set

    /** The friendly name of the organization  */
    /**
     * @see org.opencastproject.security.api.Organization.getName
     */
    @XmlElement(name = "name")
    override var name: String? = null
        protected set

    /** Server and port mapping  */
    @XmlElement(name = "server")
    @XmlElementWrapper(name = "servers")
    protected var servers: MutableList<OrgServer>? = null

    /** The local admin role name  */
    /**
     * @see org.opencastproject.security.api.Organization.getId
     */
    @XmlElement(name = "adminRole")
    override var adminRole: String? = null
        protected set

    /** The local anonymous role name  */
    /**
     * @see org.opencastproject.security.api.Organization.getId
     */
    @XmlElement(name = "anonymousRole")
    override var anonymousRole: String? = null
        protected set

    /** Arbitrary string properties associated with this organization  */
    @XmlElement(name = "property")
    @XmlElementWrapper(name = "properties")
    protected var properties: MutableList<OrgProperty>? = null

    /**
     * No-arg constructor needed by JAXB
     */
    constructor() {}

    constructor(orgId: String) {
        this.id = orgId
    }

    /**
     * Constructs an organization with its attributes.
     *
     * @param id
     * the unique identifier
     * @param name
     * the friendly name
     * @param servers
     * the hosts names and ports
     * @param adminRole
     * name of the local admin role
     * @param anonymousRole
     * name of the local anonymous role
     * @param properties
     * arbitrary properties defined for this organization, which might include branding, etc.
     */
    constructor(id: String, name: String, servers: Map<String, Int>?, adminRole: String, anonymousRole: String,
                properties: Map<String, String>?) : this() {
        this.id = id
        this.name = name
        this.servers = ArrayList()
        if (servers != null && !servers.isEmpty()) {
            for ((key, value) in servers) {
                this.servers!!.add(OrgServer(key, value))
            }
        }
        this.adminRole = adminRole
        this.anonymousRole = anonymousRole
        this.properties = ArrayList()
        if (properties != null && !properties.isEmpty()) {
            for ((key, value) in properties) {
                this.properties!!.add(OrgProperty(key, value))
            }
        }
    }

    /**
     * @see org.opencastproject.security.api.Organization.getServers
     */
    override fun getServers(): Map<String, Int> {
        val map = HashMap<String, Int>()
        if (servers != null) {
            for (server in servers!!) {
                map[server.name] = server.port
            }
        }
        return map
    }

    /**
     * @see org.opencastproject.security.api.Organization.getProperties
     */
    override fun getProperties(): Map<String, String> {
        val map = HashMap<String, String>()
        for (prop in properties!!) {
            map[prop.key] = prop.value
        }
        return map
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String? {
        return id
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        return if (obj !is Organization) false else obj.id == id
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return EqualsUtil.hash(id)
    }

    /**
     * An organization property. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "server", namespace = "http://org.opencastproject.security")
    class OrgServer {

        /** The server name  */
        /**
         * @return the server name
         */
        @XmlAttribute
        var name: String
            protected set

        /** The server port  */
        /**
         * @return the port
         */
        @XmlAttribute
        var port: Int = 0
            protected set

        /**
         * No-arg constructor needed by JAXB
         */
        constructor() {}

        /**
         * Constructs an organization server mapping with a server name and a port.
         *
         * @param name
         * the name
         * @param port
         * the port
         */
        constructor(name: String, port: Int) {
            this.name = name
            this.port = port
        }
    }

    /**
     * An organization property. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "property", namespace = "http://org.opencastproject.security")
    class OrgProperty {

        /** The property key  */
        /**
         * @return the key
         */
        @XmlAttribute
        var key: String
            protected set

        /** The property value  */
        /**
         * @return the value
         */
        @XmlValue
        var value: String
            protected set

        /**
         * No-arg constructor needed by JAXB
         */
        constructor() {}

        /**
         * Constructs an organization property with a key and a value.
         *
         * @param key
         * the key
         * @param value
         * the value
         */
        constructor(key: String, value: String) {
            this.key = key
            this.value = value
        }
    }

    companion object {

        /**
         * Constructs an organization from an organization
         *
         * @param org
         * the organization
         */
        fun fromOrganization(org: Organization): JaxbOrganization {
            return org as? JaxbOrganization ?: JaxbOrganization(org.id, org.name, org.servers, org.adminRole,
                    org.anonymousRole, org.properties)
        }
    }

}
