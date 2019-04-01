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

import org.opencastproject.security.api.CachingUserProviderMXBean
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserProvider

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.UncheckedExecutionException

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper
import org.springframework.security.ldap.userdetails.LdapUserDetailsService

import java.lang.management.ManagementFactory
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import javax.management.InstanceNotFoundException
import javax.management.MBeanServer
import javax.management.ObjectName

/**
 * A UserProvider that reads user roles from LDAP entries.
 */
class LdapUserProviderInstance
/**
 * Constructs an ldap user provider with the needed settings.
 *
 * @param pid
 * the pid of this service
 * @param organization
 * the organization
 * @param searchBase
 * the ldap search base
 * @param searchFilter
 * the ldap search filter
 * @param url
 * the url of the ldap server
 * @param userDn
 * the user to authenticate as
 * @param password
 * the user credentials
 * @param roleAttributesGlob
 * the comma separate list of ldap attributes to treat as roles
 * @param rolePrefix
 * a prefix to be prepended to all the roles read from the LDAP server
 * @param extraRoles
 * an array of extra roles to add to all the users
 * @param excludePrefixes
 * an array of role prefixes. The roles starting with any of these will not be prepended with the rolePrefix
 * @param convertToUppercase
 * whether or not the role names will be converted to uppercase
 * @param cacheSize
 * the number of users to cache
 * @param cacheExpiration
 * the number of minutes to cache users
 * @param securityService
 * a reference to Opencast's security service
 */
// CHECKSTYLE:OFF
internal constructor(pid: String, organization: Organization, searchBase: String, searchFilter: String, url: String,
                     userDn: String, password: String, roleAttributesGlob: String, rolePrefix: String, extraRoles: Array<String>?,
                     excludePrefixes: Array<String>?, convertToUppercase: Boolean, cacheSize: Int, cacheExpiration: Int,
                     /** Opencast's security service  */
                     private val securityService: SecurityService) : UserProvider, CachingUserProviderMXBean {

    /** The spring ldap userdetails service delegate  */
    private var delegate: LdapUserDetailsService? = null

    /** The organization id  */
    private val organization: Organization? = null

    /** Total number of requests made to load users  */
    private var requests: AtomicLong? = null

    /** The number of requests made to ldap  */
    private var ldapLoads: AtomicLong? = null

    /** A cache of users, which lightens the load on the LDAP server  */
    private var cache: LoadingCache<String, Any>? = null

    /** A token to store in the miss cache  */
    protected var nullToken = Any()

    /** The general role prefix, to be added to all the LDAP roles that do not start by one of the exclude prefixes  */
    private var rolePrefix: String? = null

    /** A Set of roles to be added to all the users authenticated using this LDAP instance  */
    private val setExtraRoles = HashSet<GrantedAuthority>()

    /** A Set of prefixes. When a role starts with any of these, the role prefix defined above will not be prepended  */
    private val setExcludePrefixes = HashSet<String>()

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
        } else (requests!!.get() - ldapLoads!!.get()).toFloat() / requests!!.get()

    override// TODO implement LDAP get all users
    // FIXME We return the current user, rather than an empty list, to make sure the current user's role is displayed in
    // the admin UI (MH-12526).
    val users: Iterator<User>
        get() {
            val currentUser = securityService.user
            if (loadUser(currentUser.username) != null) {
                val retVal = ArrayList<User>()
                retVal.add(securityService.user)
                return retVal.iterator()
            }
            return emptyList<User>().iterator()
        }

    init {
        // CHECKSTYLE:ON
        this.organization = organization
        logger.debug("Creating LdapUserProvider instance with pid=" + pid + ", and organization=" + organization
                + ", to LDAP server at url:  " + url)

        val contextSource = DefaultSpringSecurityContextSource(url)
        if (StringUtils.isNotBlank(userDn)) {
            contextSource.setPassword(password)
            contextSource.setUserDn(userDn)
            // Required so that authentication will actually be used
            contextSource.isAnonymousReadOnly = false
        } else {
            // No password set so try to connect anonymously.
            contextSource.isAnonymousReadOnly = true
        }

        try {
            contextSource.afterPropertiesSet()
        } catch (e: Exception) {
            throw org.opencastproject.util.ConfigurationException("Unable to create a spring context source", e)
        }

        val userSearch = FilterBasedLdapUserSearch(searchBase, searchFilter, contextSource)
        userSearch.setReturningAttributes(roleAttributesGlob.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        delegate = LdapUserDetailsService(userSearch)

        if (StringUtils.isNotBlank(roleAttributesGlob)) {
            val mapper = LdapUserDetailsMapper()

            mapper.setConvertToUpperCase(convertToUppercase)

            mapper.setRoleAttributes(roleAttributesGlob.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

            if (convertToUppercase)
                this.rolePrefix = StringUtils.trimToEmpty(rolePrefix).toUpperCase()
            else
                this.rolePrefix = StringUtils.trimToEmpty(rolePrefix)

            logger.debug("Role prefix set to: \"{}\"", this.rolePrefix)

            // The default prefix value is "ROLE_", so we must explicitly set it to "" by default
            // Because of the parameters extraRoles and excludePrefixes, we must add the prefix manually
            mapper.setRolePrefix("")
            delegate!!.setUserDetailsMapper(mapper)

            // Process the excludePrefixes if needed
            if (!this.rolePrefix!!.isEmpty()) {
                if (excludePrefixes != null) {
                    // "Clean" the list of exclude prefixes
                    for (excludePrefix in excludePrefixes) {
                        val cleanPrefix = excludePrefix.trim { it <= ' ' }
                        if (!cleanPrefix.isEmpty()) {
                            if (convertToUppercase)
                                setExcludePrefixes.add(cleanPrefix.toUpperCase())
                            else
                                setExcludePrefixes.add(cleanPrefix)
                        }
                    }

                    if (logger.isDebugEnabled) {
                        if (setExcludePrefixes.size > 0) {
                            logger.debug("Exclude prefixes set to:")
                            for (prefix in excludePrefixes) {
                                logger.debug("\t* {}", prefix)
                            }
                        } else {
                            logger.debug("No exclude prefixes defined")
                        }
                    }
                }
            }
        }

        // Process extra roles
        if (extraRoles != null) {
            for (extraRole in extraRoles) {
                val finalRole = StringUtils.trimToEmpty(extraRole)
                if (!finalRole.isEmpty()) {
                    if (convertToUppercase) {
                        setExtraRoles.add(SimpleGrantedAuthority(finalRole.toUpperCase()))
                    } else {
                        setExtraRoles.add(SimpleGrantedAuthority(finalRole))
                    }
                }
            }
        }

        // Setup the caches
        cache = CacheBuilder.newBuilder().maximumSize(cacheSize.toLong()).expireAfterWrite(cacheExpiration.toLong(), TimeUnit.MINUTES)
                .build(object : CacheLoader<String, Any>() {
                    @Throws(Exception::class)
                    override fun load(id: String): Any? {
                        val user = loadUserFromLdap(id)
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
        ldapLoads = AtomicLong()
        try {
            val name: ObjectName
            name = LdapUserProviderFactory.getObjectName(pid)
            val mbean = this
            val mbs = ManagementFactory.getPlatformMBeanServer()
            try {
                mbs.unregisterMBean(name)
            } catch (e: InstanceNotFoundException) {
                logger.debug("$name was not registered")
            }

            mbs.registerMBean(mbean, name)
        } catch (e: Exception) {
            logger.warn("Unable to register {} as an mbean: {}", this, e)
        }

    }

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
        logger.debug("LdapUserProvider is loading user $userName")
        requests!!.incrementAndGet()
        try {
            // use #getUnchecked since the loader does not throw any checked exceptions
            val user = cache!!.getUnchecked(userName)
            return if (user === nullToken) {
                null
            } else {
                user as JaxbUser
            }
        } catch (e: UncheckedExecutionException) {
            logger.warn("Exception while loading user $userName", e)
            return null
        }

    }

    /**
     * Loads a user from LDAP.
     *
     * @param userName
     * the username
     * @return the user
     */
    protected fun loadUserFromLdap(userName: String): User? {
        if (delegate == null || cache == null) {
            throw IllegalStateException("The LDAP user detail service has not yet been configured")
        }
        ldapLoads!!.incrementAndGet()
        var userDetails: UserDetails? = null

        val currentThread = Thread.currentThread()
        val originalClassloader = currentThread.contextClassLoader
        try {
            currentThread.contextClassLoader = LdapUserProviderFactory::class.java.classLoader
            try {
                userDetails = delegate!!.loadUserByUsername(userName)
            } catch (e: UsernameNotFoundException) {
                cache!!.put(userName, nullToken)
                return null
            }

            val jaxbOrganization = JaxbOrganization.fromOrganization(organization)

            // Get the roles and add the extra roles
            val authorities = HashSet<GrantedAuthority>()
            authorities.addAll(userDetails!!.authorities)
            authorities.addAll(setExtraRoles)

            val roles = HashSet<JaxbRole>()
            if (authorities != null) {
                /*
         * Please note the prefix logic for roles:
         *
         * - Roles that start with any of the "exclude prefixes" are left intact
         * - In any other case, the "role prefix" is prepended to the roles read from LDAP
         *
         * This only applies to the prefix addition. The conversion to uppercase is independent from these
         * considerations
         */
                for (authority in authorities) {
                    var strAuthority = authority.authority

                    var hasExcludePrefix = false
                    for (excludePrefix in setExcludePrefixes) {
                        if (strAuthority.startsWith(excludePrefix)) {
                            hasExcludePrefix = true
                            break
                        }
                    }
                    if (!hasExcludePrefix) {
                        strAuthority = rolePrefix!! + strAuthority
                    }

                    // Finally, add the role itself
                    roles.add(JaxbRole(strAuthority, jaxbOrganization))
                }
            }
            val user = JaxbUser(userDetails.username, PROVIDER_NAME, jaxbOrganization, roles)
            cache!!.put(userName, user)
            return user
        } finally {
            currentThread.contextClassLoader = originalClassloader
        }
    }

    override fun findUsers(query: String, offset: Int, limit: Int): Iterator<User> {
        if (query == null)
            throw IllegalArgumentException("Query must be set")
        // TODO implement a LDAP wildcard search
        // FIXME We return the current user, rather than an empty list, to make sure the current user's role is displayed in
        // the admin UI (MH-12526).
        val currentUser = securityService.user
        if (loadUser(currentUser.username) != null) {
            val retVal = ArrayList<User>()
            retVal.add(securityService.user)
            return retVal.iterator()
        }
        return emptyList<User>().iterator()
    }

    override fun countUsers(): Long {
        // TODO implement LDAP count users
        // FIXME Because of MH-12526, we return conditionally 1 when the previous methods return the current user
        return if (loadUser(securityService.user.username) != null) {
            1
        } else 0
    }

    override fun invalidate(userName: String) {
        cache!!.invalidate(userName)
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(LdapUserProviderInstance::class.java)

        val PROVIDER_NAME = "ldap"
    }
}
