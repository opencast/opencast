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

package org.opencastproject.dataloader

import org.opencastproject.security.api.DefaultOrganization.Companion.DEFAULT_ORGANIZATION_ID

import org.opencastproject.kernel.security.OrganizationDirectoryServiceImpl
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.security.api.AccessControlEntry
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.Permissions
import org.opencastproject.security.api.SecurityConstants
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.impl.jpa.JpaGroup
import org.opencastproject.security.impl.jpa.JpaOrganization
import org.opencastproject.security.impl.jpa.JpaRole
import org.opencastproject.security.impl.jpa.JpaUser
import org.opencastproject.series.api.SeriesException
import org.opencastproject.series.api.SeriesService
import org.opencastproject.userdirectory.JpaGroupRoleProvider
import org.opencastproject.userdirectory.JpaUserAndRoleProvider
import org.opencastproject.util.NotFoundException

import org.apache.commons.lang3.BooleanUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Arrays
import java.util.HashSet

/**
 * A data loader to populate the series and JPA user provider with sample data.
 */
class UserAndSeriesLoader {

    /** The series service  */
    protected var seriesService: SeriesService? = null

    /** The JPA-based user provider, which includes an addUser() method  */
    protected var jpaUserProvider: JpaUserAndRoleProvider? = null

    protected var jpaGroupRoleProvider: JpaGroupRoleProvider? = null

    /** The security service  */
    protected var securityService: SecurityService? = null

    /** The organization directory  */
    protected var organizationDirectoryService: OrganizationDirectoryServiceImpl? = null

    /**
     * Callback on component activation.
     */
    protected fun activate(cc: ComponentContext) {

        // Load the demo users, if necessary
        if (BooleanUtils.toBoolean(cc.bundleContext.getProperty(PROP_DEMO_LOAD_USER))) {
            // Load 100 series and 1000 users, but don't block activation
            Loader().start()
        }
    }

    protected inner class Loader : Thread() {
        override fun run() {
            logger.info("Adding sample series...")

            for (i in 1..NUM_SERIES) {
                val seriesId = SERIES_PREFIX + i
                val dc = DublinCores.mkOpencastEpisode().catalog
                val acl = AccessControlList()

                // Add read permissions for viewing the series content in engage
                acl.entries!!.add(AccessControlEntry(SERIES_PREFIX + i + "_" + STUDENT_PREFIX, READ, true))
                acl.entries!!.add(AccessControlEntry(SERIES_PREFIX + i + "_" + INSTRUCTOR_PREFIX, READ, true))
                acl.entries!!.add(AccessControlEntry(SERIES_PREFIX + i + "_" + ADMIN_PREFIX, READ, true))

                // Add contribute permissions for adding recordings to these series
                acl.entries!!.add(AccessControlEntry(SERIES_PREFIX + i + "_" + INSTRUCTOR_PREFIX, CONTRIBUTE, true))
                acl.entries!!.add(AccessControlEntry(SERIES_PREFIX + i + "_" + ADMIN_PREFIX, CONTRIBUTE, true))

                // Add write permissions for the instructors and admins to make changes to the series themselves
                acl.entries!!.add(AccessControlEntry(SERIES_PREFIX + i + "_" + INSTRUCTOR_PREFIX, WRITE, true))
                acl.entries!!.add(AccessControlEntry(SERIES_PREFIX + i + "_" + ADMIN_PREFIX, WRITE, true))

                try {
                    dc.set(DublinCore.PROPERTY_IDENTIFIER, seriesId)
                    dc.set(DublinCore.PROPERTY_TITLE, "Series #$i")
                    dc.set(DublinCore.PROPERTY_CREATOR, "Creator #$i")
                    dc.set(DublinCore.PROPERTY_CONTRIBUTOR, "Contributor #$i")

                    val org = organizationDirectoryService!!.getOrganization(DEFAULT_ORGANIZATION_ID)
                    try {
                        val jaxbOrganization = JaxbOrganization.fromOrganization(org)
                        securityService!!.user = JaxbUser("userandseriesloader", "demo", jaxbOrganization,
                                JaxbRole(SecurityConstants.GLOBAL_ADMIN_ROLE, jaxbOrganization))
                        securityService!!.organization = org

                        try {
                            // Test if the serie already exist, it does not overwrite it.
                            if (seriesService!!.getSeries(seriesId) != null)
                                continue
                        } catch (e: NotFoundException) {
                            // If the series does not exist, we create it.
                            seriesService!!.updateSeries(dc)
                            seriesService!!.updateAccessControl(seriesId, acl)
                        }

                    } catch (e: UnauthorizedException) {
                        logger.warn(e.message)
                    } catch (e: SeriesException) {
                        logger.warn("Unable to create series {}", dc)
                    } catch (e: NotFoundException) {
                        logger.warn("Unable to find series {}", dc)
                    } finally {
                        securityService!!.organization = null
                        securityService!!.user = null
                    }
                    logger.debug("Added series {}", dc)
                } catch (e: NotFoundException) {
                    logger.warn("Unable to find organization {}", e.message)
                }

            }

            load(STUDENT_PREFIX, 20, arrayOf(USER_ROLE), DEFAULT_ORGANIZATION_ID)

            load(INSTRUCTOR_PREFIX, 2, arrayOf(USER_ROLE, INSTRUCTOR_ROLE), DEFAULT_ORGANIZATION_ID)

            load(ADMIN_PREFIX, 1, arrayOf(USER_ROLE, COURSE_ADMIN_ROLE), DEFAULT_ORGANIZATION_ID)

            loadLdapUser(DEFAULT_ORGANIZATION_ID)

            logger.info("Finished loading sample series and users")

            loadGroup("admin", DEFAULT_ORGANIZATION_ID, "Admins", "Admin group",
                    arrayOf(COURSE_ADMIN_ROLE, INSTRUCTOR_ROLE, INSTRUCTOR_ROLE),
                    arrayOf("admin1", "admin2", "admin3", "admin4"))
            loadGroup("instructor", DEFAULT_ORGANIZATION_ID, "Instructors", "Instructors group",
                    arrayOf(USER_ROLE, INSTRUCTOR_ROLE),
                    arrayOf("instructor1", "instructor2", "instructor3", "instructor4"))
            loadGroup("student", DEFAULT_ORGANIZATION_ID, "Students", "Students group", arrayOf(USER_ROLE),
                    arrayOf("student1", "student2", "student3", "student4"))

            logger.info("Finished loading sample groups")
        }

    }

    /**
     * Loads demo users into persistence.
     *
     * @param rolePrefix
     * the role prefix
     * @param numPerSeries
     * the number of users to load per series
     * @param additionalRoles
     * any additional roles to add for each user
     * @param orgId
     * the organization id
     */
    protected fun load(rolePrefix: String, numPerSeries: Int, additionalRoles: Array<String>, orgId: String) {
        val lowerCasePrefix = rolePrefix.toLowerCase()
        val totalUsers = numPerSeries * NUM_SERIES

        logger.info("Adding sample {}s, usernames and passwords are {}1/{}1... {}{}/{}{}", lowerCasePrefix, lowerCasePrefix,
                lowerCasePrefix, lowerCasePrefix, totalUsers, lowerCasePrefix, totalUsers)

        for (i in 1..totalUsers) {
            if (jpaUserProvider!!.loadUser(lowerCasePrefix + i, orgId) == null) {
                val roleSet = HashSet<JpaRole>()
                for (additionalRole in additionalRoles) {
                    roleSet.add(JpaRole(additionalRole, getOrganization(orgId)))
                }
                roleSet.add(
                        JpaRole(SERIES_PREFIX + ((i - 1) % NUM_SERIES + 1) + "_" + rolePrefix, getOrganization(orgId)))
                val user = JpaUser(lowerCasePrefix + i, lowerCasePrefix + i, getOrganization(orgId),
                        jpaUserProvider!!.getName(), true, roleSet)
                try {
                    jpaUserProvider!!.addUser(user)
                    logger.debug("Added {}", user)
                } catch (e: Exception) {
                    logger.warn("Can not add {}: {}", user, e)
                }

            }
        }
    }

    /**
     * Loads demo group into persistence
     *
     * @param groupId
     * the group id
     * @param orgId
     * the organization id
     * @param name
     * the group name
     * @param description
     * the group description
     * @param additionalRoles
     * any additional roles to the group
     * @param members
     * the members associated to this group
     */
    protected fun loadGroup(groupId: String, orgId: String, name: String, description: String, additionalRoles: Array<String>,
                            members: Array<String>) {
        if (jpaGroupRoleProvider!!.loadGroup(groupId, orgId) == null) {
            val roles = HashSet<JpaRole>()
            for (additionalRole in additionalRoles) {
                roles.add(JpaRole(additionalRole, getOrganization(orgId)))
            }
            val group = JpaGroup(groupId, getOrganization(orgId), name, description, roles,
                    HashSet(Arrays.asList(*members)))
            try {
                jpaGroupRoleProvider!!.addGroup(group)
            } catch (e: Exception) {
                logger.warn("Can not add {}: {}", group, e)
            }

        }
    }

    /**
     * Load a user for testing the ldap provider
     *
     * @param organizationId
     * the organization
     */
    protected fun loadLdapUser(organizationId: String) {
        val ldapUserRoles = HashSet<JpaRole>()
        ldapUserRoles.add(JpaRole(USER_ROLE, getOrganization(organizationId)))
        // This is the public identifier for Josh Holtzman in the UC Berkeley Directory, which is available for anonymous
        // binding.
        val ldapUserId = "231693"

        if (jpaUserProvider!!.loadUser(ldapUserId, organizationId) == null) {
            try {
                jpaUserProvider!!.addUser(JpaUser(ldapUserId, "ldap", getOrganization(organizationId),
                        jpaUserProvider!!.getName(), true, ldapUserRoles))
                logger.debug("Added ldap user '{}' into organization '{}'", ldapUserId, organizationId)
            } catch (ex: UnauthorizedException) {
                logger.error("Unable to add an administrative user because you have not enough permissions.")
            }

        }
    }

    /**
     * Create a new organization from a default organization
     *
     * @param orgId
     * the organization identifier
     * @return the created organization
     */
    protected fun getOrganization(orgId: String): JpaOrganization {
        val org = DefaultOrganization()
        return JpaOrganization(orgId, org.name, org.getServers(), org.adminRole, org.anonymousRole,
                org.getProperties())
    }

    /**
     * @param jpaUserProvider
     * the jpaUserProvider to set
     */
    fun setJpaUserProvider(jpaUserProvider: JpaUserAndRoleProvider) {
        this.jpaUserProvider = jpaUserProvider
    }

    /**
     * @param jpaGroupRoleProvider
     * the jpaGroupRoleProvider to set
     */
    fun setJpaGroupRoleProvider(jpaGroupRoleProvider: JpaGroupRoleProvider) {
        this.jpaGroupRoleProvider = jpaGroupRoleProvider
    }

    /**
     * @param seriesService
     * the seriesService to set
     */
    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    /**
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * @param organizationDirectoryService
     * the organizationDirectoryService to set
     */
    fun setOrganizationDirectoryService(organizationDirectoryService: OrganizationDirectoryService) {
        this.organizationDirectoryService = organizationDirectoryService as OrganizationDirectoryServiceImpl
    }

    companion object {

        /** The number of series to load  */
        val NUM_SERIES = 10

        /** The number of students per series to load  */
        val STUDENTS_PER_SERIES = 20

        /** The number of instructors per series to load  */
        val INSTRUCTORS_PER_SERIES = 2

        /** The number of admins per series to load  */
        val ADMINS_PER_SERIES = 1

        /** The series prefix  */
        val SERIES_PREFIX = "SERIES_"

        /** The user role  */
        val USER_ROLE = "ROLE_USER"

        /** The instructor role  */
        val INSTRUCTOR_ROLE = "ROLE_INSTRUCTOR"

        /** The course admin role  */
        val COURSE_ADMIN_ROLE = "ROLE_COURSE_ADMIN"

        /** The student role suffix  */
        val STUDENT_PREFIX = "STUDENT"

        /** The instructor role suffix  */
        val INSTRUCTOR_PREFIX = "INSTRUCTOR"

        /** The departmental admin (not the super admin) role suffix  */
        val ADMIN_PREFIX = "ADMIN"

        /** Configuration property to set if to load default users  */
        val PROP_DEMO_LOAD_USER = "org.opencastproject.security.demo.loadusers"

        /** The read permission  */
        val READ = Permissions.Action.READ.toString()

        /** The write permission  */
        val WRITE = Permissions.Action.WRITE.toString()

        /** The contribute permission  */
        val CONTRIBUTE = Permissions.Action.CONTRIBUTE.toString()

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(UserAndSeriesLoader::class.java)
    }

}
