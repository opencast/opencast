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

package org.opencastproject.security.shibboleth

import org.opencastproject.security.api.UserDirectoryService

import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter
import org.springframework.util.Assert

import java.util.Enumeration

import javax.servlet.http.HttpServletRequest

/**
 * Handles for Shibboleth request headers to create Authorization ids. Optional operations can be assigned by setting
 * the ShibbolethLoginHandler; for example, to create corresponding user accounts if the user doesn't exist or update
 * user information on seeing Shibboleth attribute data.
 */
class ShibbolethRequestHeaderAuthenticationFilter : RequestHeaderAuthenticationFilter() {

    /** Spring security's user details manager  */
    private var userDetailsService: UserDetailsService? = null

    /** The user directory service  */
    private var userDirectoryService: UserDirectoryService? = null

    /** The implementation that is taking care of extracting user attributes from the request  */
    private var loginHandler: ShibbolethLoginHandler? = null

    /** If set to true, all request headers will be logged  */
    private var debug = false

    override fun afterPropertiesSet() {
        super.afterPropertiesSet()
        Assert.notNull(userDetailsService, "A UserDetailsService must be set")
        Assert.notNull(loginHandler, "A ShibbolethLoginHandler must be set")
        Assert.notNull(userDirectoryService, "A UserDirectoryService must be set")
    }

    /**
     * This is called when a request is made, the returned object identifies the user and will either be Null or a String.
     * This method will throw an exception if exceptionIfHeaderMissing is set to true (default) and the required header is
     * missing.
     *
     * @param request
     * the incoming request
     */
    override fun getPreAuthenticatedPrincipal(request: HttpServletRequest): Any? {
        val o = super.getPreAuthenticatedPrincipal(request) as String
        if (debug)
            debug(request)
        if (o != null && "" != o.trim { it <= ' ' }) {
            try {
                if (userDetailsService!!.loadUserByUsername(o) != null) {
                    loginHandler!!.existingUserLogin(o, request)
                }
            } catch (e: UsernameNotFoundException) {
                loginHandler!!.newUserLogin(o, request)
                userDirectoryService!!.invalidate(o)
            }

        }
        return o
    }

    /**
     * Logs all request headers to the logging facility.
     *
     * @param request
     * the request
     */
    protected fun debug(request: HttpServletRequest) {
        val he = request.headerNames
        while (he.hasMoreElements()) {
            val headerName = he.nextElement()
            val buf = StringBuffer(headerName).append(": ")
            val hv = request.getHeaders(headerName)
            var first = true
            while (hv.hasMoreElements()) {
                if (!first)
                    buf.append(", ")
                buf.append(hv.nextElement())
                first = false
            }
            logger.info(buf.toString())
        }
    }

    /**
     * If set to `true`, the filter will log all request headers to the logging facility.
     *
     * @param debug
     * `true` to log request headers
     */
    fun setDebug(debug: Boolean) {
        this.debug = debug
    }

    /**
     * Sets the user details service which allows to check whether a user is already known by the system or not.
     *
     * @param userDetailsService
     * the user details service
     */
    fun setUserDetailsService(userDetailsService: UserDetailsService) {
        this.userDetailsService = userDetailsService
    }

    /**
     * Sets the user directory service which allows to invalidate the cache of a new created user.
     *
     * @param userDirectoryService
     * the user directory service
     */
    fun setUserDirectoryService(userDirectoryService: UserDirectoryService) {
        this.userDirectoryService = userDirectoryService
    }

    /**
     * Required. Used to handle creation and update of user accounts.
     *
     * @param loginHandler
     * the handler
     */
    fun setShibbolethLoginHandler(loginHandler: ShibbolethLoginHandler) {
        this.loginHandler = loginHandler
    }

}
