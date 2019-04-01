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

import java.util.ArrayList

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A wrapper for service registration collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "services", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "services", namespace = "http://serviceregistry.opencastproject.org")
class JaxbServiceRegistrationList {
    /** A list of search items.  */
    @XmlElement(name = "service")
    protected var registrations: MutableList<JaxbServiceRegistration> = ArrayList()

    constructor() {}

    constructor(registration: JaxbServiceRegistration) {
        this.registrations.add(registration)
    }

    constructor(registrations: Collection<JaxbServiceRegistration>) {
        for (stat in registrations)
            this.registrations.add(stat)
    }

    /**
     * @return the registrations
     */
    fun getRegistrations(): List<JaxbServiceRegistration> {
        return registrations
    }

    /**
     * @param registrations
     * the registrations to set
     */
    fun setStats(registrations: MutableList<JaxbServiceRegistration>) {
        this.registrations = registrations
    }

    fun add(registration: ServiceRegistration) {
        if (registration is JaxbServiceRegistration) {
            registrations.add(registration)
        } else {
            throw IllegalArgumentException("Service registrations must be an instance of JaxbServiceRegistration")
        }
    }
}
