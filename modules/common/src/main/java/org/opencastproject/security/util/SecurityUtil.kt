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

package org.opencastproject.security.util

import org.apache.commons.lang3.StringUtils.isBlank
import org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE
import org.opencastproject.security.api.SecurityConstants.GLOBAL_ANONYMOUS_USERNAME
import org.opencastproject.security.api.SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLE
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityConstants
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext

import java.net.URL
import java.util.regex.Pattern

/** Opencast security helpers.  */
object SecurityUtil {
    private val SANITIZING_PATTERN = Pattern.compile("[^a-zA-Z0-9_]")

    /** The name of the key used to store the name of the system user in the global config.  */
    val PROPERTY_KEY_SYS_USER = "org.opencastproject.security.digest.user"

    /**
     * Run function `f` in the context described by the given organization and user.
     *
     * @param sec
     * Security service to use for getting data
     * @param org
     * Organization to switch to
     * @param user
     * User to switch to
     * @param fn
     * Function to execute
     */
    fun runAs(sec: SecurityService, org: Organization, user: User, fn: Runnable) {
        val prevOrg = sec.organization
        val prevUser = if (prevOrg != null) sec.user else null
        sec.organization = org
        sec.user = user
        try {
            fn.run()
        } finally {
            sec.organization = prevOrg
            sec.user = prevUser
        }
    }

    /**
     * Create a system user for the given organization with global and organization local admin role. Get the
     * `systemUserName` from the global config where it is stored under [.PROPERTY_KEY_SYS_USER]. In an
     * OSGi environment this is typically done calling
     * `componentContext.getBundleContext().getProperty(PROPERTY_KEY_SYS_USER)`.
     *
     * @see .createSystemUser
     */
    fun createSystemUser(systemUserName: String, org: Organization): User {
        val jaxbOrganization = JaxbOrganization.fromOrganization(org)
        return JaxbUser(systemUserName, null, jaxbOrganization, JaxbRole(GLOBAL_ADMIN_ROLE, jaxbOrganization),
                JaxbRole(org.adminRole, jaxbOrganization))
    }

    /**
     * Create the global anonymous user with the given organization.
     *
     * @param org
     * the organization
     * @return the global anonymous user
     */
    fun createAnonymousUser(org: Organization): User {
        val jaxbOrganization = JaxbOrganization.fromOrganization(org)
        return JaxbUser(GLOBAL_ANONYMOUS_USERNAME, null, jaxbOrganization, JaxbRole(
                jaxbOrganization.anonymousRole, jaxbOrganization))
    }

    /**
     * Create a system user for the given organization with global admin role. The system user name is fetched from the
     * global OSGi config.
     *
     * @see .createSystemUser
     */
    fun createSystemUser(cc: ComponentContext, org: Organization): User {
        val systemUserName = cc.bundleContext.getProperty(PROPERTY_KEY_SYS_USER)
        return createSystemUser(systemUserName, org)
    }

    /**
     * Fetch the system user name from the configuration.
     *
     * @see .PROPERTY_KEY_SYS_USER
     */
    fun getSystemUserName(cc: ComponentContext): String {
        val systemUserName = cc.bundleContext.getProperty(PROPERTY_KEY_SYS_USER)
        return systemUserName ?: throw ConfigurationException(
                "An Opencast installation always needs a system user name. Please configure one under the key $PROPERTY_KEY_SYS_USER")
    }

    /** Get the organization `orgId`.  */
    fun getOrganization(orgDir: OrganizationDirectoryService, orgId: String): Option<Organization> {
        try {
            return some(orgDir.getOrganization(orgId))
        } catch (e: NotFoundException) {
            return none()
        }

    }

    /** Get a user of a certain organization by its ID.  */
    fun getUserOfOrganization(sec: SecurityService, orgDir: OrganizationDirectoryService,
                              orgId: String, userDir: UserDirectoryService, userId: String): Option<User> {
        val prevOrg = sec.organization
        try {
            val org = orgDir.getOrganization(orgId)
            sec.organization = org
            return option(userDir.loadUser(userId))
        } catch (e: NotFoundException) {
            return none()
        } finally {
            sec.organization = prevOrg
        }
    }

    /**
     * Get a user and an organization. Only returns something if both elements can be determined.
     */
    fun getUserAndOrganization(sec: SecurityService,
                               orgDir: OrganizationDirectoryService, orgId: String, userDir: UserDirectoryService, userId: String): Option<Tuple<User, Organization>> {
        val prevOrg = sec.organization
        try {
            val org = orgDir.getOrganization(orgId)
            sec.organization = org
            return option(userDir.loadUser(userId)).fmap(object : Function<User, Tuple<User, Organization>>() {
                override fun apply(user: User): Tuple<User, Organization> {
                    return tuple(user, org)
                }
            })
        } catch (e: NotFoundException) {
            return none()
        } finally {
            sec.organization = prevOrg
        }
    }

    /** Extract hostname and port number from a URL.  */
    fun hostAndPort(url: URL): Tuple<String, Int> {
        return tuple(StringUtils.strip(url.host, "/"), url.port)
    }

    /**
     * Check if the current user has access to the capture agent with the given id.
     * @param agentId
     * The agent id to check.
     * @throws UnauthorizedException
     * If the user doesn't have access.
     */
    @Throws(UnauthorizedException::class)
    fun checkAgentAccess(securityService: SecurityService, agentId: String) {
        if (isBlank(agentId)) {
            return
        }
        val user = securityService.user
        if (user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE) || user.hasRole(user.organization.adminRole)) {
            return
        }
        if (!user.hasRole(SecurityUtil.getCaptureAgentRole(agentId))) {
            throw UnauthorizedException(user, "schedule")
        }
    }

    private fun sanitizeCaName(ca: String): String {
        return SANITIZING_PATTERN.matcher(ca).replaceAll("").toUpperCase()
    }

    /**
     * Get the role name of the role required to access the capture agent with the given agent id.
     *
     * @param
     * agentId The id of the agent to get the role for.
     * @return
     * The role name.
     */
    fun getCaptureAgentRole(agentId: String): String {
        return GLOBAL_CAPTURE_AGENT_ROLE + "_" + sanitizeCaName(agentId)
    }
}
