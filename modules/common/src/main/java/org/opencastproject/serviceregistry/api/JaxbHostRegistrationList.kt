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
 * A wrapper for host registration collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "hosts", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "hosts", namespace = "http://serviceregistry.opencastproject.org")
class JaxbHostRegistrationList {

    /** A list of search items.  */
    @XmlElement(name = "host")
    protected var registrations: MutableList<JaxbHostRegistration> = ArrayList()

    constructor() {}

    constructor(registration: JaxbHostRegistration) {
        this.registrations.add(registration)
    }

    constructor(registrations: Collection<JaxbHostRegistration>) {
        for (reg in registrations)
            this.registrations.add(reg)
    }

    /**
     * @return the registrations
     */
    fun getRegistrations(): List<JaxbHostRegistration> {
        return registrations
    }

    /**
     * @param registrations
     * the registrations to set
     */
    fun setRegistrations(registrations: MutableList<JaxbHostRegistration>) {
        this.registrations = registrations
    }

    fun add(registration: HostRegistration) {
        if (registration is JaxbHostRegistration) {
            registrations.add(registration)
        } else {
            throw IllegalArgumentException("Service registrations must be an instance of JaxbHostRegistration")
        }
    }

}
