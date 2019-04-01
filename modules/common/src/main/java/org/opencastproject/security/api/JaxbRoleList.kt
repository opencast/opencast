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

import java.util.ArrayList

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A wrapper for role collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "roles", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "roles", namespace = "http://org.opencastproject.security")
class JaxbRoleList {

    /** A list of roles.  */
    @XmlElement(name = "role")
    protected var roles: MutableList<JaxbRole> = ArrayList()

    constructor() {}

    constructor(role: JaxbRole) {
        roles.add(role)
    }

    constructor(roles: Collection<JaxbRole>) {
        for (role in roles)
            this.roles.add(role)
    }

    /**
     * @return the roles
     */
    fun getRoles(): List<JaxbRole> {
        return roles
    }

    /**
     * @param roles
     * the roles to set
     */
    fun setRoles(roles: MutableList<JaxbRole>) {
        this.roles = roles
    }

    fun add(role: Role) {
        if (role is JaxbRole) {
            roles.add(role)
        } else {
            roles.add(JaxbRole.fromRole(role))
        }
    }

}
