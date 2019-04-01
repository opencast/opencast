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

import org.apache.commons.lang3.StringUtils

import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.Objects

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlTransient
import javax.xml.bind.annotation.XmlType

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user", namespace = "http://org.opencastproject.security", propOrder = ["userName", "name", "email", "provider", "isManageable", "roles", "organization"])
@XmlRootElement(name = "user", namespace = "http://org.opencastproject.security")
class JaxbUser : User {

    /** The user name  */
    /**
     * @see org.opencastproject.security.api.User.getUsername
     */
    @XmlElement(name = "username")
    override var username: String
        protected set

    /** The user's name  */
    @XmlElement(name = "name")
    override var name: String? = null
        protected set

    /** The user's email address  */
    @XmlElement(name = "email")
    override var email: String? = null
        protected set

    /** The user's provider  */
    @XmlElement(name = "provider")
    override var provider: String

    @XmlElement(name = "manageable")
    override var isManageable = false

    /** The roles  */
    @XmlElement(name = "role")
    @XmlElementWrapper(name = "roles")
    protected var roles: Set<JaxbRole>

    /** The optional password. Note that this will never be serialized to xml  */
    /**
     * @see org.opencastproject.security.api.User.getPassword
     */
    @XmlTransient
    override var password: String? = null
        protected set

    @XmlTransient
    protected var canLogin = false

    /** The user's home organization identifier  */
    @XmlElement(name = "organization")
    protected var organization: JaxbOrganization

    /**
     * No-arg constructor needed by JAXB
     */
    constructor() {}

    /**
     * Constructs a user which is a member of the given organization that has the specified roles and no password set.
     *
     * @param userName
     * the username
     * @param provider
     * the provider
     * @param organization
     * the organization
     * @param roles
     * the set of roles for this user
     * @throws IllegalArgumentException
     * if `userName` or `organization` is `null`
     */
    @Throws(IllegalArgumentException::class)
    constructor(userName: String, provider: String, organization: JaxbOrganization, vararg roles: JaxbRole) : this(userName, provider, organization, HashSet<JaxbRole>(Arrays.asList<JaxbRole>(*roles))) {
    }

    /**
     * Constructs a user which is a member of the given organization that has the specified roles.
     *
     * @param userName
     * the username
     * @param password
     * the password
     * @param provider
     * the provider
     * @param organization
     * the organization
     * @param roles
     * the set of roles for this user
     * @throws IllegalArgumentException
     * if `userName` or `organization` is `null`
     */
    @Throws(IllegalArgumentException::class)
    constructor(userName: String, password: String, provider: String, organization: JaxbOrganization, vararg roles: JaxbRole) : this(userName, password, provider, organization, HashSet<JaxbRole>(Arrays.asList<JaxbRole>(*roles))) {
    }

    /**
     * Constructs a user which is a member of the given organization that has the specified roles.
     *
     * @param userName
     * the username
     * @param password
     * the password
     * @param provider
     * the provider
     * @param canLogin
     * `true` if able to login
     * @param organization
     * the organization
     * @param roles
     * the set of roles for this user
     * @throws IllegalArgumentException
     * if `userName` or `organization` is `null`
     */
    @Throws(IllegalArgumentException::class)
    constructor(userName: String, password: String, provider: String, canLogin: Boolean, organization: JaxbOrganization,
                vararg roles: JaxbRole) : this(userName, password, null, null, provider, canLogin, organization, HashSet<JaxbRole>(Arrays.asList<JaxbRole>(*roles))) {
    }

    /**
     * Constructs a user which is a member of the given organization that has the specified roles.
     *
     * @param userName
     * the username
     * @param password
     * the password
     * @param provider
     * the provider
     * @param organization
     * the organization
     * @param roles
     * the set of roles for this user
     * @throws IllegalArgumentException
     * if `userName` or `organization` is `null`
     */
    @Throws(IllegalArgumentException::class)
    constructor(userName: String, password: String?, provider: String, organization: JaxbOrganization, roles: Set<JaxbRole>) : this(userName, password, null, null, provider, true, organization, roles) {
    }

    /**
     * Constructs a user which is a member of the given organization that has the specified roles.
     *
     * @param userName
     * the username
     * @param password
     * the password
     * @param name
     * the name
     * @param email
     * the email
     * @param provider
     * the provider
     * @param organization
     * the organization
     * @param roles
     * the set of roles for this user
     * @throws IllegalArgumentException
     * if `userName` or `organization` is `null`
     */
    @Throws(IllegalArgumentException::class)
    constructor(userName: String, password: String, name: String, email: String, provider: String,
                organization: JaxbOrganization, roles: Set<JaxbRole>) : this(userName, password, name, email, provider, true, organization, roles) {
    }

    /**
     * Constructs a user which is a member of the given organization that has the specified roles and no password set.
     *
     * @param userName
     * the username
     * @param provider
     * the provider
     * @param organization
     * the organization
     * @param roles
     * the set of roles for this user
     * @throws IllegalArgumentException
     * if `userName` or `organization` is `null`
     */
    @Throws(IllegalArgumentException::class)
    constructor(userName: String, provider: String, organization: JaxbOrganization, roles: Set<JaxbRole>) : this(userName, null, provider, organization, roles) {
    }

    /**
     * Constructs a user which is a member of the given organization that has the specified roles.
     *
     * @param userName
     * the username
     * @param password
     * the password
     * @param name
     * the name
     * @param email
     * the email
     * @param provider
     * the provider
     * @param canLogin
     * `true` if able to login
     * @param organization
     * the organization
     * @param roles
     * the set of roles for this user
     * @throws IllegalArgumentException
     * if `userName` or `organization` is `null`
     */
    @Throws(IllegalArgumentException::class)
    constructor(userName: String, password: String?, name: String?, email: String?, provider: String, canLogin: Boolean,
                organization: JaxbOrganization?, roles: Set<JaxbRole>?) {
        if (StringUtils.isBlank(userName))
            throw IllegalArgumentException("Username must be set")
        if (organization == null)
            throw IllegalArgumentException("Organization must be set")
        this.username = userName
        this.password = password
        this.name = name
        this.email = email
        this.canLogin = canLogin
        this.provider = provider
        this.organization = organization
        if (roles == null) {
            this.roles = HashSet()
        } else {
            for (role in roles) {
                if (organization.id != role.organizationId) {
                    throw IllegalArgumentException("Role $role is not from the same organization!")
                }
            }
            this.roles = roles
        }
    }

    /**
     * @see org.opencastproject.security.api.User.canLogin
     */
    override fun canLogin(): Boolean {
        return canLogin
    }

    /**
     * @see org.opencastproject.security.api.User.getOrganization
     */
    override fun getOrganization(): Organization {
        return organization
    }

    /**
     * @see org.opencastproject.security.api.User.getRoles
     */
    override fun getRoles(): Set<Role> {
        return HashSet<Role>(roles)
    }

    /**
     * @see org.opencastproject.security.api.User.hasRole
     */
    override fun hasRole(roleName: String): Boolean {
        for (role in roles) {
            if (role.name == roleName)
                return true
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (obj !is User)
            return false
        val other = obj as User?
        return username == other!!.username && organization == other.organization
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return EqualsUtil.hash(username, organization, provider)
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return String.format("%s:%s:%s", username, organization, provider)
    }

    companion object {

        /**
         * Creates a JAXB user from a regular user object.
         *
         * @param user
         * the user
         * @return the JAXB user
         */
        fun fromUser(user: User): JaxbUser {
            return user as? JaxbUser ?: fromUser(user, emptySet<JaxbRole>())
        }

        /**
         * Creates a JAXB user from a regular user object with an additional set of roles.
         *
         * @param user
         * the user
         * @return the JAXB user
         */
        fun fromUser(user: User, extraRoles: Collection<Role>): JaxbUser {
            val roles = HashSet<JaxbRole>()
            for (role in user.roles) {
                roles.add(JaxbRole.fromRole(role))
            }
            for (role in extraRoles) {
                roles.add(JaxbRole.fromRole(role))
            }

            val jaxbUser = JaxbUser(user.username, user.password, user.name, user.email,
                    user.provider, user.canLogin(), JaxbOrganization.fromOrganization(user.organization), roles)
            jaxbUser.isManageable = user.isManageable
            return jaxbUser
        }
    }

}
