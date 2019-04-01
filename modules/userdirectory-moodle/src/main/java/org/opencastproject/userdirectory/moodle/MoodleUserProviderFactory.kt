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

package org.opencastproject.userdirectory.moodle

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
import java.net.URI
import java.net.URISyntaxException
import java.util.Dictionary
import java.util.concurrent.ConcurrentHashMap

import javax.management.MalformedObjectNameException
import javax.management.ObjectName

/**
 * Moodle implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
class MoodleUserProviderFactory : ManagedServiceFactory {

    /**
     * The OSGI bundle context
     */
    protected var bundleContext: BundleContext? = null

    /**
     * A map of pid to moodle user provider instance
     */
    private val providerRegistrations = ConcurrentHashMap<String, ServiceRegistration<*>>()

    /**
     * The organization directory service
     */
    private var orgDirectory: OrganizationDirectoryService? = null

    /**
     * OSGi callback for setting the organization directory service.
     */
    fun setOrgDirectory(orgDirectory: OrganizationDirectoryService) {
        this.orgDirectory = orgDirectory
    }

    /**
     * Callback for the activation of this component
     *
     * @param cc the component context
     */
    fun activate(cc: ComponentContext) {
        logger.debug("Activate MoodleUserProviderFactory")
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
        logger.debug("updated MoodleUserProviderFactory")

        val organization = properties.get(ORGANIZATION_KEY) as String
        if (StringUtils.isBlank(organization))
            throw ConfigurationException(ORGANIZATION_KEY, "is not set")

        val urlStr = properties.get(MOODLE_URL_KEY) as String
        val url: URI
        if (StringUtils.isBlank(urlStr))
            throw ConfigurationException(MOODLE_URL_KEY, "is not set")
        try {
            url = URI(urlStr)
        } catch (e: URISyntaxException) {
            throw ConfigurationException(MOODLE_URL_KEY, "not a URL")
        }

        val token = properties.get(MOODLE_TOKEN_KEY) as String
        if (StringUtils.isBlank(token))
            throw ConfigurationException(MOODLE_TOKEN_KEY, "is not set")

        var groupRoles = false
        val groupRolesStr = properties.get(GROUP_ROLES_KEY) as String
        if ("true" == groupRolesStr)
            groupRoles = true

        val coursePattern = properties.get(COURSE_PATTERN_KEY) as String
        val userPattern = properties.get(USER_PATTERN_KEY) as String
        val groupPattern = properties.get(GROUP_PATTERN_KEY) as String

        var cacheSize = 1000
        try {
            if (properties.get(CACHE_SIZE) != null)
                cacheSize = Integer.parseInt(properties.get(CACHE_SIZE).toString())
        } catch (e: NumberFormatException) {
            logger.warn("{} could not be loaded, default value is used: {}", CACHE_SIZE, cacheSize)
        }

        var cacheExpiration = 60
        try {
            if (properties.get(CACHE_EXPIRATION) != null)
                cacheExpiration = Integer.parseInt(properties.get(CACHE_EXPIRATION).toString())
        } catch (e: NumberFormatException) {
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

        logger.debug("creating new MoodleUserProviderInstance for pid=$pid")
        val provider = MoodleUserProviderInstance(pid, MoodleWebServiceImpl(url, token), org,
                coursePattern, userPattern, groupPattern, groupRoles, cacheSize, cacheExpiration)

        providerRegistrations[pid] = bundleContext!!.registerService(UserProvider::class.java.name, provider, null)
        providerRegistrations[pid] = bundleContext!!.registerService(RoleProvider::class.java.name, provider, null)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedServiceFactory.deleted
     */
    override fun deleted(pid: String) {
        logger.debug("delete MoodleUserProviderInstance for pid=$pid")
        val registration = providerRegistrations.remove(pid)
        if (registration != null) {
            registration.unregister()
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(MoodleUserProviderFactory.getObjectName(pid))
            } catch (e: Exception) {
                logger.warn("Unable to unregister mbean for pid='{}': {}", pid, e.message)
            }

        }
    }

    companion object {
        /**
         * This service factory's PID
         */
        val PID = "org.opencastproject.userdirectory.moodle"

        /**
         * The logger
         */
        private val logger = LoggerFactory.getLogger(MoodleUserProviderFactory::class.java)

        /**
         * The key to look up the organization identifer in the service configuration properties
         */
        private val ORGANIZATION_KEY = "org.opencastproject.userdirectory.moodle.org"

        /**
         * The key to look up the REST webservice URL of the Moodle instance
         */
        private val MOODLE_URL_KEY = "org.opencastproject.userdirectory.moodle.url"

        /**
         * The key to look up the user token to use for performing searches.
         */
        private val MOODLE_TOKEN_KEY = "org.opencastproject.userdirectory.moodle.token"

        /**
         * The key to look up the number of user records to cache
         */
        private val CACHE_SIZE = "org.opencastproject.userdirectory.moodle.cache.size"

        /**
         * The key to look up the number of minutes to cache users
         */
        private val CACHE_EXPIRATION = "org.opencastproject.userdirectory.moodle.cache.expiration"

        /**
         * The key to look up whether to activate group roles
         */
        private val GROUP_ROLES_KEY = "org.opencastproject.userdirectory.moodle.group.roles.enabled"

        /**
         * The key to look up the regular expression used to validate courses
         */
        private val COURSE_PATTERN_KEY = "org.opencastproject.userdirectory.moodle.course.pattern"

        /**
         * The key to look up the regular expression used to validate users
         */
        private val USER_PATTERN_KEY = "org.opencastproject.userdirectory.moodle.user.pattern"

        /**
         * The key to look up the regular expression used to validate groups
         */
        private val GROUP_PATTERN_KEY = "org.opencastproject.userdirectory.moodle.group.pattern"

        /**
         * Builds a JMX object name for a given PID
         *
         * @param pid the PID
         * @return the object name
         * @throws NullPointerException
         * @throws MalformedObjectNameException
         */
        @Throws(MalformedObjectNameException::class, NullPointerException::class)
        fun getObjectName(pid: String): ObjectName {
            return ObjectName("$pid:type=MoodleRequests")
        }
    }
}
