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

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserProvider
import org.opencastproject.userdirectory.JpaGroupRoleProvider
import org.opencastproject.util.NotFoundException

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedServiceFactory
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator

import java.lang.management.ManagementFactory
import java.util.Dictionary
import java.util.Hashtable
import java.util.concurrent.ConcurrentHashMap

import javax.management.MalformedObjectNameException
import javax.management.ObjectName

/**
 * LDAP implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
class LdapUserProviderFactory : ManagedServiceFactory {

    /** A map of pid to ldap user provider instance  */
    private val providerRegistrations = ConcurrentHashMap<String, ServiceRegistration<*>>()

    /** A map of pid to ldap authorities populator instance  */
    private val authoritiesPopulatorRegistrations = ConcurrentHashMap<String, ServiceRegistration<*>>()

    /** The OSGI bundle context  */
    protected var bundleContext: BundleContext? = null

    /** The organization directory service  */
    private var orgDirectory: OrganizationDirectoryService? = null

    /** The group role provider service  */
    private var groupRoleProvider: JpaGroupRoleProvider? = null

    /** A reference to Opencast's security service  */
    private var securityService: SecurityService? = null

    /** OSGi callback for setting the organization directory service.  */
    fun setOrgDirectory(orgDirectory: OrganizationDirectoryService) {
        this.orgDirectory = orgDirectory
    }

    /** OSGi callback for setting the role group service.  */
    fun setGroupRoleProvider(groupRoleProvider: JpaGroupRoleProvider) {
        this.groupRoleProvider = groupRoleProvider
    }

    /** OSGi callback for setting the security service.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * Callback for activation of this component.
     *
     * @param cc
     * the component context
     */
    fun activate(cc: ComponentContext) {
        logger.debug("Activate LdapUserProviderFactory")
        bundleContext = cc.bundleContext
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedServiceFactory.getName
     */
    override fun getName(): String {
        return PID
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedServiceFactory.updated
     */
    @Throws(ConfigurationException::class)
    override fun updated(pid: String, properties: Dictionary<*, *>) {
        logger.debug("Updating LdapUserProviderFactory")
        val organization = properties.get(ORGANIZATION_KEY) as String
        if (StringUtils.isBlank(organization))
            throw ConfigurationException(ORGANIZATION_KEY, "is not set")
        val searchBase = properties.get(SEARCH_BASE_KEY) as String
        if (StringUtils.isBlank(searchBase))
            throw ConfigurationException(SEARCH_BASE_KEY, "is not set")
        val searchFilter = properties.get(SEARCH_FILTER_KEY) as String
        if (StringUtils.isBlank(searchFilter))
            throw ConfigurationException(SEARCH_FILTER_KEY, "is not set")
        val url = properties.get(LDAP_URL_KEY) as String
        if (StringUtils.isBlank(url))
            throw ConfigurationException(LDAP_URL_KEY, "is not set")
        val instanceId = properties.get(INSTANCE_ID_KEY) as String
        if (StringUtils.isBlank(instanceId))
            throw ConfigurationException(INSTANCE_ID_KEY, "is not set")
        val userDn = properties.get(SEARCH_USER_DN) as String
        val password = properties.get(SEARCH_PASSWORD) as String
        val roleAttributes = properties.get(ROLE_ATTRIBUTES_KEY) as String
        val rolePrefix = properties.get(ROLE_PREFIX_KEY) as String

        var excludePrefixes: Array<String>? = null
        val strExcludePrefixes = properties.get(EXCLUDE_PREFIXES_KEY) as String
        if (StringUtils.isNotBlank(strExcludePrefixes)) {
            excludePrefixes = strExcludePrefixes.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        // Make sure that property convertToUppercase is true by default
        val strUppercase = properties.get(UPPERCASE_KEY) as String
        val convertToUppercase = if (StringUtils.isBlank(strUppercase)) true else java.lang.Boolean.valueOf(strUppercase)

        var extraRoles = arrayOfNulls<String>(0)
        val strExtraRoles = properties.get(EXTRA_ROLES_KEY) as String
        if (StringUtils.isNotBlank(strExtraRoles)) {
            extraRoles = strExtraRoles.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        var cacheSize = 1000
        logger.debug("Using cache size {} for {}", properties.get(CACHE_SIZE), LdapUserProviderFactory::class.java.name)
        try {
            if (properties.get(CACHE_SIZE) != null) {
                val configuredCacheSize = Integer.parseInt(properties.get(CACHE_SIZE).toString())
                if (configuredCacheSize != null) {
                    cacheSize = configuredCacheSize
                }
            }
        } catch (e: Exception) {
            logger.warn("{} could not be loaded, default value is used: {}", CACHE_SIZE, cacheSize)
        }

        var cacheExpiration = 1
        try {
            if (properties.get(CACHE_EXPIRATION) != null) {
                val configuredCacheExpiration = Integer.parseInt(properties.get(CACHE_EXPIRATION).toString())
                if (configuredCacheExpiration != null) {
                    cacheExpiration = configuredCacheExpiration
                }
            }
        } catch (e: Exception) {
            logger.warn("{} could not be loaded, default value is used: {}", CACHE_EXPIRATION, cacheExpiration)
        }

        // Now that we have everything we need, go ahead and activate a new provider, removing an old one if necessary
        val existingRegistration = providerRegistrations.remove(pid)
        existingRegistration?.unregister()

        val org: Organization
        try {
            org = orgDirectory!!.getOrganization(organization)
        } catch (e: NotFoundException) {
            logger.warn("Organization {} not found!", organization)
            throw ConfigurationException(ORGANIZATION_KEY, "not found")
        }

        // Dictionary to include a property to identify this LDAP instance in the security.xml file
        val dict = Hashtable<String, String>()
        dict[INSTANCE_ID_SERVICE_PROPERTY_KEY] = instanceId

        // Instantiate this LDAP instance and register it as such
        val provider = LdapUserProviderInstance(pid, org, searchBase, searchFilter, url, userDn,
                password, roleAttributes, rolePrefix, extraRoles, excludePrefixes, convertToUppercase, cacheSize,
                cacheExpiration, securityService)

        providerRegistrations[pid] = bundleContext!!.registerService(UserProvider::class.java.name, provider, null)

        val authoritiesPopulator = OpencastLdapAuthoritiesPopulator(roleAttributes,
                rolePrefix, excludePrefixes, convertToUppercase, org, securityService, groupRoleProvider, *extraRoles)

        // Also, register this instance as LdapAuthoritiesPopulator so that it can be used within the security.xml file
        authoritiesPopulatorRegistrations[pid] = bundleContext!!.registerService(LdapAuthoritiesPopulator::class.java.name, authoritiesPopulator, dict)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedServiceFactory.deleted
     */
    override fun deleted(pid: String) {
        var providerRegistration: ServiceRegistration<*>? = null
        var authoritiesPopulatorRegistration: ServiceRegistration<*>? = null

        try {
            providerRegistration = providerRegistrations.remove(pid)
            authoritiesPopulatorRegistration = authoritiesPopulatorRegistrations.remove(pid)
            if (providerRegistration != null || authoritiesPopulatorRegistration != null) {
                try {
                    ManagementFactory.getPlatformMBeanServer().unregisterMBean(LdapUserProviderFactory.getObjectName(pid))
                } catch (e: Exception) {
                    logger.warn("Unable to unregister mbean for pid='{}': {}", pid, e.message)
                }

            }
        } finally {
            providerRegistration?.unregister()
            authoritiesPopulatorRegistration?.unregister()
        }
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(LdapUserProviderFactory::class.java)

        /** This service factory's PID  */
        val PID = "org.opencastproject.userdirectory.ldap"

        /** The key to look up the ldap search filter in the service configuration properties  */
        private val SEARCH_FILTER_KEY = "org.opencastproject.userdirectory.ldap.searchfilter"

        /** The key to look up the ldap search base in the service configuration properties  */
        private val SEARCH_BASE_KEY = "org.opencastproject.userdirectory.ldap.searchbase"

        /** The key to look up the ldap server URL in the service configuration properties  */
        private val LDAP_URL_KEY = "org.opencastproject.userdirectory.ldap.url"

        /** The key to look up the role attributes in the service configuration properties  */
        private val ROLE_ATTRIBUTES_KEY = "org.opencastproject.userdirectory.ldap.roleattributes"

        /** The key to look up the organization identifier in the service configuration properties  */
        private val ORGANIZATION_KEY = "org.opencastproject.userdirectory.ldap.org"

        /** The key to look up the user DN to use for performing searches.  */
        private val SEARCH_USER_DN = "org.opencastproject.userdirectory.ldap.userDn"

        /** The key to look up the password to use for performing searches  */
        private val SEARCH_PASSWORD = "org.opencastproject.userdirectory.ldap.password"

        /** The key to look up the number of user records to cache  */
        private val CACHE_SIZE = "org.opencastproject.userdirectory.ldap.cache.size"

        /** The key to look up the number of minutes to cache users  */
        private val CACHE_EXPIRATION = "org.opencastproject.userdirectory.ldap.cache.expiration"

        /** The key to indicate a prefix that will be added to every role read from the LDAP  */
        private val ROLE_PREFIX_KEY = "org.opencastproject.userdirectory.ldap.roleprefix"

        /**
         * The key to indicate a comma-separated list of prefixes.
         * The "role prefix" defined with the ROLE_PREFIX_KEY will not be prepended to the roles starting with any of these
         */
        private val EXCLUDE_PREFIXES_KEY = "org.opencastproject.userdirectory.ldap.exclude.prefixes"

        /** The key to indicate whether or not the roles should be converted to uppercase  */
        private val UPPERCASE_KEY = "org.opencastproject.userdirectory.ldap.uppercase"

        /** The key to indicate a unique identifier for each LDAP connection  */
        private val INSTANCE_ID_KEY = "org.opencastproject.userdirectory.ldap.id"

        /** The key to indicate a comma-separated list of extra roles to add to the authenticated user  */
        private val EXTRA_ROLES_KEY = "org.opencastproject.userdirectory.ldap.extra.roles"

        /** The key to setup a LDAP connection ID as an OSGI service property  */
        private val INSTANCE_ID_SERVICE_PROPERTY_KEY = "instanceId"

        /**
         * Builds a JMX object name for a given PID
         *
         * @param pid
         * the PID
         * @return the object name
         * @throws NullPointerException
         * @throws MalformedObjectNameException
         */
        @Throws(MalformedObjectNameException::class, NullPointerException::class)
        fun getObjectName(pid: String): ObjectName {
            return ObjectName("$pid:type=LDAPRequests")
        }
    }

}
