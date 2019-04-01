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

package org.opencastproject.adminui.userdirectory

import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.Role.Type
import org.opencastproject.security.api.RoleProvider
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserProvider

import org.apache.commons.io.IOUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.TreeSet
import java.util.regex.Pattern

/**
 * The admin UI roles role provider.
 */
class UIRolesRoleProvider : RoleProvider {

    /** The security service  */
    protected var securityService: SecurityService? = null

    private var roles: Set<String>? = null

    /**
     * @see org.opencastproject.security.api.RoleProvider.getOrganization
     */
    override val organization: String
        get() = UserProvider.ALL_ORGANIZATIONS

    /**
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    protected fun activate(cc: ComponentContext) {
        try {
            javaClass.getResourceAsStream("/roles.txt").use { `in` -> roles = TreeSet(IOUtils.readLines(`in`, "UTF-8")) }
        } catch (e: IOException) {
            logger.error("Unable to read available roles", e)
        }

        logger.info("Activated Admin UI roles role provider")
    }

    /**
     * @see org.opencastproject.security.api.RoleProvider.getRoles
     */
    override fun getRoles(): Iterator<Role> {
        val organization = JaxbOrganization.fromOrganization(securityService!!.organization)
        return roles!!.stream().map { role -> toRole(role, organization) }.iterator()
    }

    /**
     * @see org.opencastproject.security.api.RoleProvider.getRolesForUser
     */
    override fun getRolesForUser(userName: String): List<Role> {
        return emptyList()
    }

    /**
     * @see org.opencastproject.security.api.RoleProvider.findRoles
     */
    override fun findRoles(query: String, target: Role.Target, offset: Int, limit: Int): Iterator<Role> {
        if (query == null)
            throw IllegalArgumentException("Query must be set")

        // These roles are not meaningful for use in ACLs
        if (target === Role.Target.ACL) {
            return Collections.emptyIterator()
        }

        val organization = JaxbOrganization.fromOrganization(securityService!!.organization)
        return roles!!.stream()
                .filter { role -> like(role, query) }
                .skip(offset.toLong())
                .limit((if (limit > 0) limit else roles!!.size).toLong())
                .map { role -> toRole(role, organization) }
                .iterator()
    }

    private fun toRole(role: String, organization: JaxbOrganization): Role {
        return JaxbRole(role, organization, "AdminNG UI Role", Type.INTERNAL)
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(UIRolesRoleProvider::class.java)

        private fun like(string: String, query: String): Boolean {
            val regex = query.replace("_", ".").replace("%", ".*?")
            val p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            return p.matcher(string).matches()
        }
    }

}
