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

import org.opencastproject.security.api.CachingUserProviderMXBean
import org.opencastproject.security.api.Group
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.RoleProvider
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserProvider
import org.opencastproject.userdirectory.moodle.MoodleWebService.CoreUserGetUserByFieldFilters

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.ExecutionError
import com.google.common.util.concurrent.UncheckedExecutionException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.PatternSyntaxException

import javax.management.InstanceNotFoundException
import javax.management.MBeanServer
import javax.management.ObjectName

/**
 * A UserProvider that reads user roles from Moodle.
 */
class MoodleUserProviderInstance
/**
 * Constructs an Moodle user provider with the needed settings.
 *
 * @param pid             The pid of this service.
 * @param client          The Moodle web serivce client.
 * @param organization    The organization.
 * @param coursePattern   The pattern of a Moodle course ID.
 * @param userPattern     The pattern of a Moodle user ID.
 * @param groupRoles      Whether to activate groupRoles
 * @param cacheSize       The number of users to cache.
 * @param cacheExpiration The number of minutes to cache users.
 */
(pid: String,
 /**
  * The Moodle web service client.
  */
 private val client: MoodleWebService,
 /**
  * The organization.
  */
 private val organization: Organization,
 /**
  * Regular expression for matching valid courses.
  */
 private var coursePattern: String?,
 /**
  * Regular expression for matching valid users.
  */
 private var userPattern: String?,
 /**
  * Regular expression for matching valid groups.
  */
 private var groupPattern: String?,
 /**
  * Whether to create group roles.
  */
 private val groupRoles: Boolean, cacheSize: Int, cacheExpiration: Int) : UserProvider, RoleProvider, CachingUserProviderMXBean {

    /**
     * A cache of users, which lightens the load on Moodle.
     */
    private val cache: LoadingCache<String, Any>?

    /**
     * A token to store in the miss cache.
     */
    private val nullToken = Any()

    /**
     * The total number of requests made to load users.
     */
    private var loadUserRequests: AtomicLong? = null

    /**
     * The number of requests made to Moodle.
     */
    private var moodleWebServiceRequests: AtomicLong? = null

    ////////////////////////////
    // CachingUserProviderMXBean

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.CachingUserProviderMXBean.getCacheHitRatio
     */
    override val cacheHitRatio: Float
        get() = if (loadUserRequests!!.get() == 0L) 0f else (loadUserRequests!!.get() - moodleWebServiceRequests!!.get()).toFloat() / loadUserRequests!!.get()

    ///////////////////////
    // UserProvider methods

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.getName
     */
    override val name: String
        get() = PROVIDER_NAME

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.getUsers
     */
    override// We never enumerate all users
    val users: Iterator<User>
        get() = Collections.emptyIterator()

    ///////////////////////
    // RoleProvider methods

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.RoleProvider.getRoles
     */
    override// We won't ever enumerate all Moodle courses, so return an empty list here
    val roles: Iterator<Role>
        get() = Collections.emptyIterator()

    init {

        logger.info("Creating new MoodleUserProviderInstance(pid={}, url={}, cacheSize={}, cacheExpiration={})", pid,
                client.url, cacheSize, cacheExpiration)

        // Setup the caches
        cache = CacheBuilder.newBuilder().maximumSize(cacheSize.toLong()).expireAfterWrite(cacheExpiration.toLong(), TimeUnit.MINUTES)
                .build(object : CacheLoader<String, Any>() {
                    override fun load(username: String): Any? {
                        val user = loadUserFromMoodle(username)
                        return user ?: nullToken
                    }
                })

        registerMBean(pid)
    }

    /**
     * Registers an MXBean.
     */
    private fun registerMBean(pid: String) {
        // register with jmx
        loadUserRequests = AtomicLong()
        moodleWebServiceRequests = AtomicLong()
        try {
            val name: ObjectName
            name = MoodleUserProviderFactory.getObjectName(pid)
            val mbean = this
            val mbs = ManagementFactory.getPlatformMBeanServer()
            try {
                mbs.unregisterMBean(name)
            } catch (e: InstanceNotFoundException) {
                logger.debug("{} was not registered", name)
            }

            mbs.registerMBean(mbean, name)
        } catch (e: Exception) {
            logger.error("Unable to register {} as an mbean: {}", this, e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.loadUser
     */
    override fun loadUser(userName: String): User? {
        loadUserRequests!!.incrementAndGet()
        try {
            val user = cache!!.getUnchecked(userName)
            if (user === nullToken) {
                logger.debug("Returning null user from cache")
                return null
            } else {
                logger.debug("Returning user {} from cache", userName)
                return user as User
            }
        } catch (e: ExecutionError) {
            logger.warn("Exception while loading user {}", userName, e)
            return null
        } catch (e: UncheckedExecutionException) {
            logger.warn("Exception while loading user {}", userName, e)
            return null
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.countUsers
     */
    override fun countUsers(): Long {
        // Not meaningful, as we never enumerate users
        return 0
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.getOrganization
     */
    override fun getOrganization(): String {
        return organization.id
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.findUsers
     */
    override fun findUsers(query: String, offset: Int, limit: Int): Iterator<User> {
        var query: String = query ?: throw IllegalArgumentException("Query must be set")

        if (query.endsWith("%"))
            query = query.substring(0, query.length - 1)

        if (query.isEmpty())
            return Collections.emptyIterator()

        // Check if user matches pattern
        try {
            if (userPattern != null && !query.matches(userPattern.toRegex())) {
                logger.debug("verify user {} failed regexp {}", query, userPattern)
                return Collections.emptyIterator()
            }
        } catch (e: PatternSyntaxException) {
            logger.warn("Invalid regular expression for user pattern {} - disabling checks", userPattern)
            userPattern = null
        }

        // Load User
        val users = LinkedList<User>()

        val user = loadUser(query)
        if (user != null)
            users.add(user)

        return users.iterator()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.invalidate
     */
    override fun invalidate(userName: String) {
        cache!!.invalidate(userName)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.RoleProvider.getRolesForUser
     */
    override fun getRolesForUser(username: String): List<Role> {
        val roles = LinkedList<Role>()

        // Don't answer for admin, anonymous or empty user
        if ("admin" == username || "" == username || "anonymous" == username) {
            logger.debug("we don't answer for: {}", username)
            return roles
        }

        val user = loadUser(username)
        if (user != null) {
            logger.debug("Returning cached role set for {}", username)
            return ArrayList(user.roles)
        }

        // Not found
        logger.debug("Return empty role set for {} - not found in Moodle", username)
        return LinkedList()
    }

    /**
     * {@inheritDoc}
     *
     *
     * We search for COURSEID, COURSEID_Learner, COURSEID_Instructor
     *
     * @see org.opencastproject.security.api.RoleProvider.findRoles
     */
    override fun findRoles(query: String, target: Role.Target, offset: Int, limit: Int): Iterator<Role> {
        var query = query
        // Don't return roles for users or groups
        if (target === Role.Target.USER)
            return Collections.emptyIterator()

        var exact = true
        var ltirole = false

        if (query.endsWith("%")) {
            exact = false
            query = query.substring(0, query.length - 1)
        }

        if (query.isEmpty())
            return Collections.emptyIterator()

        // Verify that role name ends with LEARNER_ROLE_SUFFIX or INSTRUCTOR_ROLE_SUFFIX
        if (exact
                && !query.endsWith("_$LEARNER_ROLE_SUFFIX")
                && !query.endsWith("_$INSTRUCTOR_ROLE_SUFFIX")
                && !query.endsWith("_$GROUP_ROLE_SUFFIX"))
            return Collections.emptyIterator()

        val findGroupRole = groupRoles && query.startsWith(GROUP_ROLE_PREFIX)

        // Extract Moodle id
        var moodleId = if (findGroupRole) query.substring(GROUP_ROLE_PREFIX.length) else query
        if (query.endsWith("_$LEARNER_ROLE_SUFFIX")) {
            moodleId = query.substring(0, query.lastIndexOf("_$LEARNER_ROLE_SUFFIX"))
            ltirole = true
        } else if (query.endsWith("_$INSTRUCTOR_ROLE_SUFFIX")) {
            moodleId = query.substring(0, query.lastIndexOf("_$INSTRUCTOR_ROLE_SUFFIX"))
            ltirole = true
        } else if (query.endsWith("_$GROUP_ROLE_SUFFIX")) {
            moodleId = query.substring(0, query.lastIndexOf("_$GROUP_ROLE_SUFFIX"))
            ltirole = true
        }

        // Check if matches patterns
        val pattern = if (findGroupRole) groupPattern else coursePattern
        try {
            if (pattern != null && !moodleId.matches(pattern.toRegex())) {
                logger.debug("Verify Moodle ID {} failed regexp {}", moodleId, pattern)
                return Collections.emptyIterator()
            }
        } catch (e: PatternSyntaxException) {
            logger.warn("Invalid regular expression for pattern {} - disabling checks", pattern)
            if (findGroupRole) {
                groupPattern = null
            } else {
                coursePattern = null
            }
        }

        // Roles list
        val roles = LinkedList<Role>()
        val jaxbOrganization = JaxbOrganization.fromOrganization(organization)
        if (ltirole) {
            // Query is for a Moodle ID and an LTI role (Instructor/Learner/Group)
            roles.add(JaxbRole(query, jaxbOrganization, "Moodle Site Role", Role.Type.EXTERNAL))
        } else if (findGroupRole) {
            // Group ID
            roles.add(JaxbRole(GROUP_ROLE_PREFIX + moodleId + "_" + GROUP_ROLE_SUFFIX, jaxbOrganization,
                    "Moodle Group Learner Role", Role.Type.EXTERNAL))
        } else {
            // Course ID - return both roles
            roles.add(JaxbRole(moodleId + "_" + INSTRUCTOR_ROLE_SUFFIX, jaxbOrganization,
                    "Moodle Course Instructor Role", Role.Type.EXTERNAL))
            roles.add(JaxbRole(moodleId + "_" + LEARNER_ROLE_SUFFIX, jaxbOrganization, "Moodle Course Learner Role",
                    Role.Type.EXTERNAL))
        }

        return roles.iterator()
    }

    /////////////////
    // Helper methods

    /**
     * Loads a user from Moodle.
     *
     * @param username The username.
     * @return The user.
     */
    private fun loadUserFromMoodle(username: String): User? {
        logger.debug("loadUserFromMoodle({})", username)

        if (cache == null)
            throw IllegalStateException("The Moodle user detail service has not yet been configured")

        // Don't answer for admin, anonymous or empty user
        if ("admin" == username || "" == username || "anonymous" == username) {
            logger.debug("We don't answer for: $username")
            return null
        }

        val jaxbOrganization = JaxbOrganization.fromOrganization(organization)

        // update cache statistics
        moodleWebServiceRequests!!.incrementAndGet()

        val currentThread = Thread.currentThread()
        val originalClassloader = currentThread.contextClassLoader

        try {
            // Load user
            val moodleUsers = client
                    .coreUserGetUsersByField(CoreUserGetUserByFieldFilters.username, listOf(username))

            if (moodleUsers.isEmpty()) {
                logger.debug("User {} not found in Moodle system", username)
                return null
            }
            val moodleUser = moodleUsers[0]

            // Load Roles
            val courseIdsInstructor = client.toolOpencastGetCoursesForInstructor(username)
            val courseIdsLearner = client.toolOpencastGetCoursesForLearner(username)
            val groupIdsLearner = if (groupRoles) client.toolOpencastGetGroupsForLearner(username) else emptyList()

            // Create Opencast Objects
            val roles = HashSet<JaxbRole>()
            roles.add(JaxbRole(Group.ROLE_PREFIX + "MOODLE", jaxbOrganization, "Moodle Users", Role.Type.EXTERNAL_GROUP))
            for (courseId in courseIdsInstructor) {
                roles.add(JaxbRole(courseId + "_" + INSTRUCTOR_ROLE_SUFFIX, jaxbOrganization, "Moodle Course Instructor Role",
                        Role.Type.EXTERNAL))
            }
            for (courseId in courseIdsLearner) {
                roles.add(JaxbRole(courseId + "_" + LEARNER_ROLE_SUFFIX, jaxbOrganization, "Moodle Course Learner Role",
                        Role.Type.EXTERNAL))
            }
            for (groupId in groupIdsLearner) {
                roles.add(JaxbRole(GROUP_ROLE_PREFIX + groupId + "_" + GROUP_ROLE_SUFFIX, jaxbOrganization,
                        "Moodle Group Learner Role", Role.Type.EXTERNAL))
            }

            return JaxbUser(moodleUser.username!!, null, moodleUser.fullname, moodleUser.email,
                    this.name, true, jaxbOrganization, roles)
        } catch (e: Exception) {
            logger.warn("Exception loading Moodle user {} at {}: {}", username, client.url, e.message)
        } finally {
            currentThread.contextClassLoader = originalClassloader
        }

        return null
    }

    companion object {
        /**
         * User and role provider name.
         */
        private val PROVIDER_NAME = "moodle"

        /**
         * The logger.
         */
        private val logger = LoggerFactory.getLogger(MoodleUserProviderInstance::class.java)

        /**
         * Suffix for Moodle roles with the learner capability.
         */
        private val LEARNER_ROLE_SUFFIX = "Learner"

        /**
         * Suffix for Moodle roles with the instructor capability.
         */
        private val INSTRUCTOR_ROLE_SUFFIX = "Instructor"

        /**
         * Prefix for Moodle group roles.
         */
        private val GROUP_ROLE_PREFIX = "G"

        /**
         * Suffix for Moodle group roles.
         */
        private val GROUP_ROLE_SUFFIX = "Learner"
    }
}
