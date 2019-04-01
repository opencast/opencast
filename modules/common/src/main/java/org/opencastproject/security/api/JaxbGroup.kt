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

import java.util.HashSet

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "group", namespace = "http://org.opencastproject.security")
class JaxbGroup : Group {

    /**
     * @see org.opencastproject.security.api.Group.getGroupId
     */
    @XmlElement(name = "id")
    override var groupId: String
        protected set

    @XmlElement(name = "organization")
    protected var organization: JaxbOrganization

    /**
     * @see org.opencastproject.security.api.Group.getName
     */
    @XmlElement(name = "name")
    override var name: String
        protected set

    /**
     * @see org.opencastproject.security.api.Group.getDescription
     */
    @XmlElement(name = "description")
    override var description: String
        protected set

    /**
     * @see org.opencastproject.security.api.Group.getRole
     */
    @XmlElement(name = "role")
    override var role: String
        protected set

    /**
     * @see org.opencastproject.security.api.Group.getMembers
     */
    @XmlElement(name = "member")
    @XmlElementWrapper(name = "members")
    override var members: Set<String>
        protected set

    @XmlElement(name = "role")
    @XmlElementWrapper(name = "roles")
    protected var roles: Set<JaxbRole>

    /**
     * No-arg constructor needed by JAXB
     */
    constructor() {}

    /**
     * Constructs a group with the specified groupId, name, description and group role.
     *
     * @param groupId
     * the group id
     * @param organization
     * the organization
     * @param name
     * the name
     * @param description
     * the description
     */
    constructor(groupId: String, organization: JaxbOrganization, name: String, description: String) : super() {
        this.groupId = groupId
        this.organization = organization
        this.name = name
        this.description = description
        this.role = Group.ROLE_PREFIX + groupId.toUpperCase()
        this.roles = HashSet()
    }

    /**
     * Constructs a group with the specified groupId, name, description, group role and roles.
     *
     * @param groupId
     * the group id
     * @param organization
     * the organization
     * @param name
     * the name
     * @param description
     * the description
     * @param roles
     * the additional group roles
     */
    constructor(groupId: String, organization: JaxbOrganization, name: String, description: String, roles: Set<JaxbRole>) : this(groupId, organization, name, description) {
        this.roles = roles
    }

    /**
     * Constructs a group with the specified groupId, name, description, group role and roles.
     *
     * @param groupId
     * the group id
     * @param organization
     * the organization
     * @param name
     * the name
     * @param description
     * the description
     * @param roles
     * the additional group roles
     * @param members
     * the group members
     */
    constructor(groupId: String, organization: JaxbOrganization, name: String, description: String, roles: Set<JaxbRole>,
                members: Set<String>) : this(groupId, organization, name, description, roles) {
        this.members = members
    }

    /**
     * @see org.opencastproject.security.api.Group.getOrganization
     */
    override fun getOrganization(): Organization {
        return organization
    }

    /**
     * @see org.opencastproject.security.api.Group.getRoles
     */
    override fun getRoles(): Set<Role> {
        return HashSet<Role>(roles)
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return EqualsUtil.hash(groupId, organization)
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (obj !is Group)
            return false
        val other = obj as Group?
        return groupId == other!!.groupId && organization == other.organization
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return StringBuilder(groupId).append(":").append(organization).toString()
    }

    companion object {

        fun fromGroup(group: Group): JaxbGroup {
            val organization = JaxbOrganization.fromOrganization(group.organization)
            val roles = HashSet<JaxbRole>()
            for (role in group.roles) {
                if (role is JaxbRole) {
                    roles.add(role)
                } else {
                    roles.add(JaxbRole.fromRole(role))
                }
            }
            return JaxbGroup(group.groupId, organization, group.name, group.description, roles,
                    group.members)
        }
    }

}
