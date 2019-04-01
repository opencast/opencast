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

package org.opencastproject.userdirectory.sakai

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.RoleProvider
import org.opencastproject.security.api.UserProvider
import org.opencastproject.util.NotFoundException

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedServiceFactory
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory
import java.util.Arrays
import java.util.Dictionary
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

import javax.management.MalformedObjectNameException
import javax.management.ObjectName

/**
 * Sakai implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
class SakaiUserProviderFactory : ManagedServiceFactory {

    /** A map of pid to sakai user provider instance  */
    private val providerRegistrations = ConcurrentHashMap<String, ServiceRegistration<*>>()

    /** The OSGI bundle context  */
    protected var bundleContext: BundleContext? = null

    /** The organization directory service  */
    private var orgDirectory: OrganizationDirectoryService? = null

    /** OSGi callback for setting the organization directory service.  */
    fun setOrgDirectory(orgDirectory: OrganizationDirectoryService) {
        this.orgDirectory = orgDirectory
    }

    /**
     * Callback for the activation of this component
     *
     * @param cc
     * the component context
     */
    fun activate(cc: ComponentContext) {
        logger.debug("Activate SakaiUserProviderFactory")
        this.bundleContext = cc.bundleContext
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

        logger.debug("updated SakaiUserProviderFactory")

        val organization = properties.get(ORGANIZATION_KEY) as String
        if (StringUtils.isBlank(organization)) throw ConfigurationException(ORGANIZATION_KEY, "is not set")

        val url = properties.get(SAKAI_URL_KEY) as String
        if (StringUtils.isBlank(url)) throw ConfigurationException(SAKAI_URL_KEY, "is not set")

        val userDn = properties.get(SAKAI_SEARCH_USER) as String
        val password = properties.get(SEARCH_PASSWORD) as String

        val sitePattern = properties.get(SITE_PATTERN_KEY) as String
        val userPattern = properties.get(USER_PATTERN_KEY) as String

        var cacheSize = 1000
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


        var cacheExpiration = 60
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

        // Instructor roles
        val instructorRoles: MutableSet<String>
        val instructorRoleList = properties.get(SAKAI_INSTRUCTOR_ROLES_KEY) as String

        if (!StringUtils.isEmpty(instructorRoleList)) {
            val trimmedRoles = StringUtils.trim(instructorRoleList)
            val roles = trimmedRoles.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            instructorRoles = HashSet(Arrays.asList(*roles))
            logger.info("Sakai instructor roles: {}", Arrays.toString(roles))
        } else {
            // Default instructor roles
            instructorRoles = HashSet()
            instructorRoles.add("Site owner")
            instructorRoles.add("Instructor")
            instructorRoles.add("maintain")
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

        logger.debug("creating new SakaiUserProviderInstance for pid=$pid")
        val provider = SakaiUserProviderInstance(pid,
                org, url, userDn, password, sitePattern, userPattern, instructorRoles, cacheSize, cacheExpiration)

        providerRegistrations[pid] = bundleContext!!.registerService(UserProvider::class.java.name, provider, null)
        providerRegistrations[pid] = bundleContext!!.registerService(RoleProvider::class.java.name, provider, null)

    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedServiceFactory.deleted
     */
    override fun deleted(pid: String) {
        logger.debug("delete SakaiUserProviderInstance for pid=$pid")
        val registration = providerRegistrations.remove(pid)
        if (registration != null) {
            registration.unregister()
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(SakaiUserProviderFactory.getObjectName(pid))
            } catch (e: Exception) {
                logger.warn("Unable to unregister mbean for pid='{}': {}", pid, e.message)
            }

        }
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(SakaiUserProviderFactory::class.java)

        /** This service factory's PID  */
        val PID = "org.opencastproject.userdirectory.sakai"

        /** The key to look up the organization identifer in the service configuration properties  */
        private val ORGANIZATION_KEY = "org.opencastproject.userdirectory.sakai.org"

        /** The key to look up the user DN to use for performing searches.  */
        private val SAKAI_SEARCH_USER = "org.opencastproject.userdirectory.sakai.user"

        /** The key to look up the password to use for performing searches  */
        private val SEARCH_PASSWORD = "org.opencastproject.userdirectory.sakai.password"

        /** The key to look up the regular expression used to validate sites  */
        private val SITE_PATTERN_KEY = "org.opencastproject.userdirectory.sakai.site.pattern"

        /** The key to look up the regular expression used to validate users  */
        private val USER_PATTERN_KEY = "org.opencastproject.userdirectory.sakai.user.pattern"

        /** The key to look up the number of user records to cache  */
        private val CACHE_SIZE = "org.opencastproject.userdirectory.sakai.cache.size"

        /** The key to look up the URL of the Sakai instance  */
        private val SAKAI_URL_KEY = "org.opencastproject.userdirectory.sakai.url"

        /** The key to look up the list of Instructor roles on the Sakai instance  */
        private val SAKAI_INSTRUCTOR_ROLES_KEY = "org.opencastproject.userdirectory.sakai.instructor.roles"

        /** The key to look up the number of minutes to cache users  */
        private val CACHE_EXPIRATION = "org.opencastproject.userdirectory.sakai.cache.expiration"

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
            return ObjectName("$pid:type=SakaiRequests")
        }
    }

}
