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


import java.util.Objects

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "role", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "role", namespace = "http://org.opencastproject.security")
class JaxbRole : Role {

    /** The role name  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.Role.getName
     */
    @XmlElement(name = "name")
    override var name: String
        protected set

    /** The description  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.Role.getDescription
     */
    @XmlElement(name = "description")
    override var description: String
        protected set

    @XmlElement(name = "organization")
    protected var organization: JaxbOrganization? = null

    @XmlElement(name = "organization-id")
    protected var organizationId: String? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.Role.getType
     */
    @XmlElement(name = "type")
    override var type: Role.Type = Role.Type.INTERNAL
        protected set

    /**
     * No-arg constructor needed by JAXB
     */
    constructor() {}

    /**
     * Constructs a role with the specified name and organization.
     *
     * @param name
     * the name
     * @param organization
     * the organization
     */
    @Throws(IllegalArgumentException::class)
    constructor(name: String, organization: JaxbOrganization) : super() {
        this.name = name
        this.organizationId = organization.id
    }

    /**
     * Constructs a role with the specified name, organization and description.
     *
     * @param name
     * the name
     * @param organization
     * the organization
     * @param description
     * the description
     */
    @Throws(IllegalArgumentException::class)
    constructor(name: String, organization: JaxbOrganization, description: String) : this(name, organization) {
        this.description = description
    }

    /**
     * Constructs a role with the specified name, organization, description, and persistence settings.
     *
     * @param name
     * the name
     * @param organization
     * the organization
     * @param description
     * the description
     * @param type
     * the role [type]
     */
    @Throws(IllegalArgumentException::class)
    constructor(name: String, organization: JaxbOrganization, description: String, type: Role.Type) : this(name, organization, description) {
        this.type = type
    }


    /**
     * Constructs a role with the specified name, organization identifier, description, and persistence settings.
     *
     * @param name
     * the name
     * @param organizationId
     * the organization identifier
     * @param description
     * the description
     * @param type
     * the role [type]
     */
    @Throws(IllegalArgumentException::class)
    constructor(name: String, organizationId: String, description: String, type: Role.Type) : super() {
        this.name = name
        this.organizationId = organizationId
        this.description = description
        this.type = type
    }

    /**
     * {@inheritDoc}
     */
    override fun getOrganizationId(): String? {
        if (organizationId != null) {
            return organizationId
        }
        return if (organization != null) {
            organization!!.id
        } else null
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return Objects.hash(name, getOrganizationId())
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (obj !is Role)
            return false
        val other = obj as Role?
        return name == other!!.name && getOrganizationId() == other.organizationId
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return name + ":" + getOrganizationId()
    }

    companion object {

        fun fromRole(role: Role): JaxbRole {
            return role as? JaxbRole ?: JaxbRole(role.name, role.organizationId, role.description, role.type)
        }
    }
}
