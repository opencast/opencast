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

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.ExecutionError
import com.google.common.util.concurrent.UncheckedExecutionException

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.StringReader
import java.lang.management.ManagementFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.PatternSyntaxException

import javax.management.InstanceNotFoundException
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A UserProvider that reads user roles from Sakai.
 */
class SakaiUserProviderInstance
/**
 * Constructs an Sakai user provider with the needed settings.
 *
 * @param pid
 * the pid of this service
 * @param organization
 * the organization
 * @param url
 * the url of the Sakai server
 * @param userName
 * the user to authenticate as
 * @param password
 * the user credentials
 * @param cacheSize
 * the number of users to cache
 * @param cacheExpiration
 * the number of minutes to cache users
 */
(pid: String, organization: Organization, url: String, userName: String, password: String,
 /** Regular expression for matching valid sites  */
 private var sitePattern: String?,
 /** Regular expression for matching valid users  */
 private var userPattern: String?,
 /** A map of roles which are regarded as Instructor roles  */
 private val instructorRoles: Set<String>, cacheSize: Int, cacheExpiration: Int) : UserProvider, RoleProvider, CachingUserProviderMXBean {

    /** The organization  */
    private val organization: Organization? = null

    /** Total number of requests made to load users  */
    private var requests: AtomicLong? = null

    /** The number of requests made to Sakai  */
    private var sakaiLoads: AtomicLong? = null

    /** A cache of users, which lightens the load on Sakai  */
    private var cache: LoadingCache<String, Any>? = null

    /** A token to store in the miss cache  */
    protected var nullToken = Any()

    /** The URL of the Sakai instance  */
    private val sakaiUrl: String? = null

    /** The username used to call Sakai REST webservices  */
    private val sakaiUsername: String? = null

    /** The password of the user used to call Sakai REST webservices  */
    private val sakaiPassword: String? = null

    override val name: String
        get() = PROVIDER_NAME

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.CachingUserProviderMXBean.getCacheHitRatio
     */
    override val cacheHitRatio: Float
        get() = if (requests!!.get() == 0L) {
            0f
        } else (requests!!.get() - sakaiLoads!!.get()).toFloat() / requests!!.get()

    override// We never enumerate all users
    val users: Iterator<User>
        get() = Collections.emptyIterator()

    // RoleProvider methods

    override// We won't ever enumerate all Sakai sites, so return an empty list here
    val roles: Iterator<Role>
        get() = Collections.emptyIterator()

    init {

        this.organization = organization
        this.sakaiUrl = url
        this.sakaiUsername = userName
        this.sakaiPassword = password

        val jaxbOrganization = JaxbOrganization.fromOrganization(organization)

        logger.info("Creating new SakaiUserProviderInstance(pid={}, url={}, cacheSize={}, cacheExpiration={})",
                pid, url, cacheSize, cacheExpiration)

        // Setup the caches
        cache = CacheBuilder.newBuilder().maximumSize(cacheSize.toLong()).expireAfterWrite(cacheExpiration.toLong(), TimeUnit.MINUTES)
                .build(object : CacheLoader<String, Any>() {
                    @Throws(Exception::class)
                    override fun load(id: String): Any? {
                        val user = loadUserFromSakai(id)
                        return user ?: nullToken
                    }
                })

        registerMBean(pid)
    }

    /**
     * Registers an MXBean.
     */
    protected fun registerMBean(pid: String) {
        // register with jmx
        requests = AtomicLong()
        sakaiLoads = AtomicLong()
        try {
            val name: ObjectName
            name = SakaiUserProviderFactory.getObjectName(pid)
            val mbean = this
            val mbs = ManagementFactory.getPlatformMBeanServer()
            try {
                mbs.unregisterMBean(name)
            } catch (e: InstanceNotFoundException) {
                logger.debug("$name was not registered")
            }

            mbs.registerMBean(mbean, name)
        } catch (e: Exception) {
            logger.error("Unable to register {} as an mbean: {}", this, e)
        }

    }

    // UserProvider methods

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.getOrganization
     */
    override fun getOrganization(): String {
        return organization!!.id
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.UserProvider.loadUser
     */
    override fun loadUser(userName: String): User? {
        logger.debug("loaduser($userName)")
        requests!!.incrementAndGet()
        try {
            val user = cache!!.getUnchecked(userName)
            if (user === nullToken) {
                logger.debug("Returning null user from cache")
                return null
            } else {
                logger.debug("Returning user $userName from cache")
                return user as JaxbUser
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
     * Loads a user from Sakai.
     *
     * @param userName
     * the username
     * @return the user
     */
    protected fun loadUserFromSakai(userName: String): User? {

        if (cache == null) {
            throw IllegalStateException("The Sakai user detail service has not yet been configured")
        }

        // Don't answer for admin, anonymous or empty user
        if ("admin" == userName || "" == userName || "anonymous" == userName) {
            cache!!.put(userName, nullToken)
            logger.debug("we don't answer for: $userName")
            return null
        }

        logger.debug("In loadUserFromSakai, currently processing user : {}", userName)

        val jaxbOrganization = JaxbOrganization.fromOrganization(organization)

        // update cache statistics
        sakaiLoads!!.incrementAndGet()

        val currentThread = Thread.currentThread()
        val originalClassloader = currentThread.contextClassLoader
        try {

            // Sakai userId (internal id), email address and display name
            val sakaiUser = getSakaiUser(userName)

            if (sakaiUser == null) {
                // user not known to this provider
                logger.debug("User {} not found in Sakai system", userName)
                cache!!.put(userName, nullToken)
                return null
            }

            val userId = sakaiUser[0]
            val email = sakaiUser[1]
            val displayName = sakaiUser[2]

            // Get the set of Sakai roles for the user
            val sakaiRoles = getRolesFromSakai(userId)

            // if Sakai doesn't know about this user we need to return
            if (sakaiRoles == null) {
                cache!!.put(userName, nullToken)
                return null
            }

            logger.debug("Sakai roles for eid " + userName + " id " + userId + ": " + Arrays.toString(sakaiRoles))

            val roles = HashSet<JaxbRole>()

            var isInstructor = false

            for (r in sakaiRoles) {
                roles.add(JaxbRole(r, jaxbOrganization, "Sakai external role", Role.Type.EXTERNAL))

                if (r.endsWith(LTI_INSTRUCTOR_ROLE))
                    isInstructor = true
            }

            // Group role for all Sakai users
            roles.add(JaxbRole(Group.ROLE_PREFIX + "SAKAI", jaxbOrganization, "Sakai Users", Role.Type.EXTERNAL_GROUP))

            // Group role for Sakai users who are an instructor in one more sites
            if (isInstructor)
                roles.add(JaxbRole(Group.ROLE_PREFIX + "SAKAI_INSTRUCTOR", jaxbOrganization, "Sakai Instructors", Role.Type.EXTERNAL_GROUP))

            logger.debug("Returning JaxbRoles: $roles")

            // JaxbUser(String userName, String password, String name, String email, String provider, boolean canLogin, JaxbOrganization organization, Set<JaxbRole> roles)
            val user = JaxbUser(userName, null, displayName, email, PROVIDER_NAME, true, jaxbOrganization, roles)

            cache!!.put(userName, user)
            logger.debug("Returning user {}", userName)

            return user

        } finally {
            currentThread.contextClassLoader = originalClassloader
        }

    }

    /*
   ** Verify that the user exists
   ** Query with /direct/user/:ID:/exists
   */
    private fun verifySakaiUser(userId: String): Boolean {

        logger.debug("verifySakaiUser({})", userId)

        try {
            if (userPattern != null && !userId.matches(userPattern.toRegex())) {
                logger.debug("verify user {} failed regexp {}", userId, userPattern)
                return false
            }
        } catch (e: PatternSyntaxException) {
            logger.warn("Invalid regular expression for user pattern {} - disabling checks", userPattern)
            userPattern = null
        }

        val code: Int

        try {
            // This webservice does not require authentication
            val url = URL("$sakaiUrl/direct/user/$userId/exists")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", OC_USERAGENT)

            connection.connect()
            code = connection.responseCode
        } catch (e: Exception) {
            logger.warn("Exception verifying Sakai user " + userId + " at " + sakaiUrl + ": " + e.message)
            return false
        }

        // HTTP OK 200 for site exists, return false for everything else (typically 404 not found)
        return code == 200
    }

    /*
   ** Verify that the site exists
   ** Query with /direct/site/:ID:/exists
   */
    private fun verifySakaiSite(siteId: String): Boolean {

        // We could additionally cache positive and negative siteId lookup results here

        logger.debug("verifySakaiSite($siteId)")

        try {
            if (sitePattern != null && !siteId.matches(sitePattern.toRegex())) {
                logger.debug("verify site {} failed regexp {}", siteId, sitePattern)
                return false
            }
        } catch (e: PatternSyntaxException) {
            logger.warn("Invalid regular expression for site pattern {} - disabling checks", sitePattern)
            sitePattern = null
        }

        val code: Int

        try {
            // This webservice does not require authentication
            val url = URL("$sakaiUrl/direct/site/$siteId/exists")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", OC_USERAGENT)

            connection.connect()
            code = connection.responseCode
        } catch (e: Exception) {
            logger.warn("Exception verifying Sakai site " + siteId + " at " + sakaiUrl + ": " + e.message)
            return false
        }

        // HTTP OK 200 for site exists, return false for everything else (typically 404 not found)
        return code == 200
    }

    private fun getRolesFromSakai(userId: String): Array<String>? {
        logger.debug("getRolesFromSakai($userId)")
        try {

            val url = URL("$sakaiUrl/direct/membership/fastroles/$userId.xml?__auth=basic")
            val encoded = Base64.encodeBase64String("$sakaiUsername:$sakaiPassword".toByteArray(charset("utf8")))

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Basic $encoded")
            connection.setRequestProperty("User-Agent", OC_USERAGENT)

            val xml = IOUtils.toString(BufferedInputStream(connection.inputStream))
            logger.debug(xml)

            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            val parser = documentBuilderFactory.newDocumentBuilder()

            val document = parser.parse(org.xml.sax.InputSource(StringReader(xml)))

            val root = document.documentElement
            val nodes = root.getElementsByTagName("membership")
            val roleList = ArrayList<String>()
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as Element
                // The Role in sakai
                val sakaiRole = getTagValue("memberRole", element)

                // the location in sakai e.g. /site/admin
                val sakaiLocationReference = getTagValue("locationReference", element)
                // we don't do the sakai admin role
                if ("/site/!admin" == sakaiLocationReference) {
                    continue
                }

                val opencastRole = buildOpencastRole(sakaiLocationReference!!, sakaiRole)
                roleList.add(opencastRole)
            }

            return roleList.toTypedArray()

        } catch (fnf: FileNotFoundException) {
            // if the return is 404 it means the user wasn't found
            logger.debug("user id $userId not found on $sakaiUrl")
        } catch (e: Exception) {
            logger.warn("Exception getting site/role membership for Sakai user {} at {}: {}", userId, sakaiUrl, e.message)
        }

        return null
    }

    /**
     * Get the internal Sakai user Id for the supplied user. If the user exists, set the user's email address.
     *
     * @param eid
     * @return
     */
    private fun getSakaiUser(eid: String): Array<String>? {

        try {

            val url = URL("$sakaiUrl/direct/user/$eid.xml?__auth=basic")
            logger.debug("Sakai URL: " + sakaiUrl!!)
            val encoded = Base64.encodeBase64String("$sakaiUsername:$sakaiPassword".toByteArray(charset("utf8")))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Basic $encoded")
            connection.setRequestProperty("User-Agent", OC_USERAGENT)

            val xml = IOUtils.toString(BufferedInputStream(connection.inputStream))
            logger.debug(xml)

            // Parse the document
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            val parser = documentBuilderFactory.newDocumentBuilder()
            val document = parser.parse(org.xml.sax.InputSource(StringReader(xml)))
            val root = document.documentElement

            val sakaiID = getTagValue("id", root)
            val sakaiEmail = getTagValue("email", root)
            val sakaiDisplayName = getTagValue("displayName", root)

            return arrayOf<String>(sakaiID, sakaiEmail, sakaiDisplayName)

        } catch (fnf: FileNotFoundException) {
            logger.debug("user {} does not exist on Sakai system: {}", eid, fnf)
        } catch (e: Exception) {
            logger.warn("Exception getting Sakai user information for user {} at {}: {}", eid, sakaiUrl, e)
        }

        return null
    }

    /**
     * Build a Opencast role "foo_user" from the given Sakai locations
     *
     * @param sakaiLocationReference
     * @param sakaiRole
     * @return
     */
    private fun buildOpencastRole(sakaiLocationReference: String, sakaiRole: String?): String {

        // we need to parse the site id from the reference
        val siteId = sakaiLocationReference.substring(sakaiLocationReference.indexOf("/", 2) + 1)

        // map Sakai role to LTI role
        val ltiRole = if (instructorRoles.contains(sakaiRole)) LTI_INSTRUCTOR_ROLE else LTI_LEARNER_ROLE

        return siteId + "_" + ltiRole
    }

    override fun findUsers(query: String, offset: Int, limit: Int): Iterator<User> {
        var query: String = query ?: throw IllegalArgumentException("Query must be set")

        if (query.endsWith("%")) {
            query = query.substring(0, query.length - 1)
        }

        if (query.isEmpty()) {
            return Collections.emptyIterator()
        }

        // Verify if a user exists (non-wildcard searches only)
        if (!verifySakaiUser(query)) {
            return Collections.emptyIterator()
        }

        val users = LinkedList<User>()
        val jaxbOrganization = JaxbOrganization.fromOrganization(organization)
        val queryUser = JaxbUser(query, PROVIDER_NAME, jaxbOrganization, HashSet())
        users.add(queryUser)

        return users.iterator()
    }

    override fun invalidate(userName: String) {
        cache!!.invalidate(userName)
    }

    override fun countUsers(): Long {
        // Not meaningful, as we never enumerate users
        return 0
    }

    override fun getRolesForUser(userName: String): List<Role> {

        val roles = LinkedList<Role>()

        // Don't answer for admin, anonymous or empty user
        if ("admin" == userName || "" == userName || "anonymous" == userName) {
            logger.debug("we don't answer for: $userName")
            return roles
        }

        logger.debug("getRolesForUser($userName)")

        val user = loadUser(userName)
        if (user != null) {
            logger.debug("Returning cached roleset for {}", userName)
            return ArrayList(user.roles)
        }

        // Not found
        logger.debug("Return empty roleset for {} - not found on Sakai")
        return LinkedList()
    }

    override fun findRoles(query: String, target: Role.Target, offset: Int, limit: Int): Iterator<Role> {
        var query = query

        // We search for SITEID, SITEID_Learner, SITEID_Instructor

        logger.debug("findRoles(query=$query offset=$offset limit=$limit)")

        // Don't return roles for users or groups
        if (target === Role.Target.USER) {
            return Collections.emptyIterator()
        }

        var exact = true
        var ltirole = false

        if (query.endsWith("%")) {
            exact = false
            query = query.substring(0, query.length - 1)
        }

        if (query.isEmpty()) {
            return Collections.emptyIterator()
        }

        // Verify that role name ends with LTI_LEARNER_ROLE or LTI_INSTRUCTOR_ROLE
        if (exact && !query.endsWith("_$LTI_LEARNER_ROLE") && !query.endsWith("_$LTI_INSTRUCTOR_ROLE")) {
            return Collections.emptyIterator()
        }

        var sakaiSite: String? = null

        if (query.endsWith("_$LTI_LEARNER_ROLE")) {
            sakaiSite = query.substring(0, query.lastIndexOf("_$LTI_LEARNER_ROLE"))
            ltirole = true
        } else if (query.endsWith("_$LTI_INSTRUCTOR_ROLE")) {
            sakaiSite = query.substring(0, query.lastIndexOf("_$LTI_INSTRUCTOR_ROLE"))
            ltirole = true
        }

        if (!ltirole) {
            sakaiSite = query
        }

        if (!verifySakaiSite(sakaiSite)) {
            return Collections.emptyIterator()
        }

        // Roles list
        val roles = LinkedList<Role>()

        val jaxbOrganization = JaxbOrganization.fromOrganization(organization)

        if (ltirole) {
            // Query is for a Site ID and an LTI role (Instructor/Learner)
            roles.add(JaxbRole(query, jaxbOrganization, "Sakai Site Role", Role.Type.EXTERNAL))
        } else {
            // Site ID - return both roles
            roles.add(JaxbRole(sakaiSite + "_" + LTI_INSTRUCTOR_ROLE, jaxbOrganization, "Sakai Site Instructor Role", Role.Type.EXTERNAL))
            roles.add(JaxbRole(sakaiSite + "_" + LTI_LEARNER_ROLE, jaxbOrganization, "Sakai Site Learner Role", Role.Type.EXTERNAL))
        }

        return roles.iterator()
    }

    companion object {

        private val LTI_LEARNER_ROLE = "Learner"

        private val LTI_INSTRUCTOR_ROLE = "Instructor"

        val PROVIDER_NAME = "sakai"

        private val OC_USERAGENT = "Opencast"

        /** The logger  */
        private val logger = LoggerFactory.getLogger(SakaiUserProviderInstance::class.java)

        /**
         * Get a value for for a tag in the element
         *
         * @param sTag
         * @param eElement
         * @return
         */
        private fun getTagValue(sTag: String, eElement: Element): String? {
            if (eElement.getElementsByTagName(sTag) == null)
                return null

            val nlList = eElement.getElementsByTagName(sTag).item(0).childNodes
            val nValue = nlList.item(0)
            return nValue?.nodeValue
        }
    }

}
