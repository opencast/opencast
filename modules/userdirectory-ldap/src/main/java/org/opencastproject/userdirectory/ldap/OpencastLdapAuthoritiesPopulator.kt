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
package org.opencastproject.userdirectory.ldap

import java.lang.String.format

import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.SecurityService
import org.opencastproject.userdirectory.JpaGroupRoleProvider

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator
import java.util.Collections
import java.util.HashSet

/** Map a series of LDAP attributes to user authorities in Opencast  */
class OpencastLdapAuthoritiesPopulator
/**
 * Activate component
 */
(attributeNames: String?, prefix: String, aExcludedPrefixes: Array<String>?,
 uppercase: Boolean, private val organization: Organization?, private var securityService: SecurityService?,
 private var groupRoleProvider: JpaGroupRoleProvider?, vararg additionalAuthorities: String) : LdapAuthoritiesPopulator {

    private val attributeNames: MutableSet<String>
    private var additionalAuthorities: Array<String>? = null
    /**
     * Get the role prefix being used by this object. Please note that such prefix can be empty.
     *
     * @return the role prefix in use.
     */
    var rolePrefix = ""
        private set
    private val excludedPrefixes = HashSet<String>()
    /**
     * Get the property that defines whether or not the role names should be converted to uppercase.
     *
     * @return `true` if this class converts the role names to uppercase. `false` otherwise.
     */
    val convertToUpperCase = true

    /**
     * Get the exclude prefixes being used by this object.
     *
     * @return the role prefix in use.
     */
    val excludePrefixes: Array<String>
        get() = excludedPrefixes.toTypedArray()

    init {

        debug("Creating new instance")

        if (attributeNames == null) {
            throw IllegalArgumentException("The attribute list cannot be null")
        }

        if (securityService == null) {
            throw IllegalArgumentException("The security service cannot be null")
        }

        if (organization == null) {
            throw IllegalArgumentException("The organization cannot be null")
        }

        this.attributeNames = HashSet()
        for (attributeName in attributeNames.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val temp = attributeName.trim { it <= ' ' }
            if (!temp.isEmpty())
                this.attributeNames.add(temp)
        }
        if (this.attributeNames.size == 0) {
            throw IllegalArgumentException("At least one valid attribute must be provided")
        }

        if (logger.isDebugEnabled) {
            debug("Roles will be read from the LDAP attributes:")
            for (attribute in this.attributeNames) {
                logger.debug("\t* {}", attribute)
            }
        }

        if (groupRoleProvider == null) {
            info("Provided GroupRoleProvider was null. Group roles will therefore not be expanded")
        }

        this.convertToUpperCase = uppercase
        if (uppercase)
            debug("Roles will be converted to uppercase")
        else
            debug("Roles will NOT be converted to uppercase")

        if (uppercase)
            this.rolePrefix = StringUtils.trimToEmpty(prefix).replace(ROLE_CLEAN_REGEXP.toRegex(), ROLE_CLEAN_REPLACEMENT).toUpperCase()
        else
            this.rolePrefix = StringUtils.trimToEmpty(prefix).replace(ROLE_CLEAN_REGEXP.toRegex(), ROLE_CLEAN_REPLACEMENT)
        debug("Role prefix set to: {}", this.rolePrefix)

        if (aExcludedPrefixes != null)
            for (origExcludedPrefix in aExcludedPrefixes) {
                val excludedPrefix: String
                if (uppercase)
                    excludedPrefix = StringUtils.trimToEmpty(origExcludedPrefix).toUpperCase()
                else
                    excludedPrefix = StringUtils.trimToEmpty(origExcludedPrefix)
                if (!excludedPrefix.isEmpty()) {
                    excludedPrefixes.add(excludedPrefix)
                }
            }

        if (additionalAuthorities == null)
            this.additionalAuthorities = arrayOfNulls(0)
        else
            this.additionalAuthorities = additionalAuthorities

        if (logger.isDebugEnabled) {
            debug("Authenticated users will receive the following extra roles:")
            for (role in this.additionalAuthorities!!) {
                logger.debug("\t* {}", role)
            }
        }
    }

    override fun getGrantedAuthorities(userData: DirContextOperations, username: String): Collection<GrantedAuthority> {

        val authorities = HashSet<GrantedAuthority>()
        for (attributeName in attributeNames) {
            try {
                val attributeValues = userData.getStringAttributes(attributeName)
                // Should the attribute not be defined, the returned array is null
                if (attributeValues != null) {
                    for (attributeValue in attributeValues) {
                        // The attribute value may be a single authority (a single role) or a list of roles
                        addAuthorities(authorities, attributeValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    }
                } else {
                    debug("({}) Could not find any attribute named '{}' in user '{}'", attributeName, userData.dn)
                }
            } catch (e: ClassCastException) {
                error("Specified attribute containing user roles ('{}') was not of expected type String: {}", attributeName, e)
            }

        }

        // Add the list of additional roles
        addAuthorities(authorities, additionalAuthorities)

        if (logger.isDebugEnabled) {
            debug("Returning user {} with authorities:", username)
            for (authority in authorities) {
                logger.error("\t{}", authority)
            }
        }

        // Update the user in the security service if it matches the user whose authorities are being returned
        if (securityService!!.organization == organization && (securityService!!.user == null || securityService!!.user.username == username)) {
            val roles = HashSet<JaxbRole>()
            // Get the current roles
            for (existingRole in securityService!!.user.roles) {
                authorities.add(SimpleGrantedAuthority(existingRole.name))
            }
            // Convert GrantedAuthority's into JaxbRole's
            for (authority in authorities)
                roles.add(JaxbRole(authority.authority, JaxbOrganization.fromOrganization(organization)))
            val user = JaxbUser(username, LdapUserProviderInstance.PROVIDER_NAME,
                    JaxbOrganization.fromOrganization(organization), roles.toTypedArray<T>())

            securityService!!.user = user
        }

        return authorities
    }

    /**
     * Return the attributes names this object will search for
     *
     * @return a [Collection] containing such attribute names
     */
    fun getAttributeNames(): Collection<String> {
        return HashSet(attributeNames)
    }

    /**
     * Get the extra roles to be added to any user returned by this authorities populator
     *
     * @return A [Collection] of [String]s representing the additional roles
     */
    fun getAdditionalAuthorities(): Array<String> {
        return additionalAuthorities!!.clone()
    }

    /**
     * Add the specified authorities to the provided set
     *
     * @param authorities
     * a set containing the authorities
     * @param values
     * the values to add to the set
     */
    private fun addAuthorities(authorities: MutableSet<GrantedAuthority>, values: Array<String>?) {

        if (values != null) {
            val org = securityService!!.organization
            if (organization != org) {
                throw SecurityException(String.format("Current request belongs to the organization \"%s\". Expected \"%s\"",
                        org.id, organization.id))
            }

            for (value in values) {
                /*
         * Please note the prefix logic for roles:
         *
         * - Roles that start with any of the "exclude prefixes" are left intact
         * - In any other case, the "role prefix" is prepended to the roles read from LDAP
         *
         * This only applies to the prefix addition. The conversion to uppercase is independent from these
         * considerations
         */
                var authority: String
                if (convertToUpperCase)
                    authority = StringUtils.trimToEmpty(value).replace(ROLE_CLEAN_REGEXP.toRegex(), ROLE_CLEAN_REPLACEMENT)
                            .toUpperCase()
                else
                    authority = StringUtils.trimToEmpty(value).replace(ROLE_CLEAN_REGEXP.toRegex(), ROLE_CLEAN_REPLACEMENT)

                // Ignore the empty parts
                if (!authority.isEmpty()) {
                    // Check if this role is a group role and assign the groups appropriately
                    val groupRoles: List<Role>
                    if (groupRoleProvider != null)
                        groupRoles = groupRoleProvider!!.getRolesForGroup(authority)
                    else
                        groupRoles = emptyList()

                    // Try to add the prefix if appropriate
                    var prefix = this.rolePrefix

                    if (!prefix.isEmpty()) {
                        var hasExcludePrefix = false
                        for (excludePrefix in excludedPrefixes) {
                            if (authority.startsWith(excludePrefix)) {
                                hasExcludePrefix = true
                                break
                            }
                        }
                        if (hasExcludePrefix)
                            prefix = ""
                    }

                    authority = (prefix + authority).replace(ROLE_CLEAN_REGEXP.toRegex(), ROLE_CLEAN_REPLACEMENT)

                    debug("Parsed LDAP role \"{}\" to role \"{}\"", value, authority)

                    if (!groupRoles.isEmpty()) {
                        // The authority is a group role
                        debug("Found group for the group with group role \"{}\"", authority)
                        for (role in groupRoles) {
                            authorities.add(SimpleGrantedAuthority(role.name))
                            logger.debug("\tAdded role from role \"{}\"'s group: {}", authority, role)
                        }
                    }

                    // Finally, add the authority itself
                    authorities.add(SimpleGrantedAuthority(authority))

                } else {
                    debug("Found empty authority. Ignoring...")
                }
            }
        }
    }

    /**
     * Utility class to print this instance's hash code before the debug messages
     *
     * @param message
     * @param params
     */
    private fun debug(message: String, vararg params: Any) {
        logger.debug(format("(%s) %s", hashCode(), message), *params)
    }

    /**
     * Utility class to print this instance's hash code before the error messages
     *
     * @param message
     * @param params
     */
    private fun error(message: String, vararg params: Any) {
        logger.error(format("(%s) %s", hashCode(), message), *params)
    }

    /**
     * Utility class to print this instance's hash code before INFO messages
     *
     * @param message
     * @param params
     */
    private fun info(message: String, vararg params: Any) {
        logger.info(format("(%s) %s", hashCode(), message), *params)
    }

    /** OSGi callback for setting the role group service.  */
    fun setOrgDirectory(groupRoleProvider: JpaGroupRoleProvider) {
        this.groupRoleProvider = groupRoleProvider
    }

    /** OSGi callback for setting the security service.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    companion object {

        val ROLE_CLEAN_REGEXP = "[\\s_]+"
        val ROLE_CLEAN_REPLACEMENT = "_"
        private val logger = LoggerFactory.getLogger(OpencastLdapAuthoritiesPopulator::class.java)
    }

}
