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

package org.opencastproject.security.urlsigning.filter

import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.verifier.UrlSigningVerifier
import org.opencastproject.urlsigning.common.ResourceRequest
import org.opencastproject.util.OsgiUtil
import org.opencastproject.util.data.Option

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.Dictionary
import java.util.Enumeration
import java.util.LinkedList
import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UrlSigningFilter : Filter, ManagedService {

    private var urlSigningVerifier: UrlSigningVerifier? = null

    private val urlRegularExpressions = LinkedList<String>()

    private var enabled = true

    private var strict = true

    private val name: String
        get() = this.javaClass.simpleName

    /** OSGi DI  */
    fun setUrlSigningVerifier(urlSigningVerifier: UrlSigningVerifier) {
        this.urlSigningVerifier = urlSigningVerifier
    }

    /**
     * @see javax.servlet.Filter.doFilter
     */
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (!enabled) {
            chain.doFilter(request, response)
            return
        }

        if (urlRegularExpressions.size == 0) {
            logger.debug("There are no regular expressions configured to protect endpoints, skipping filter.")
            chain.doFilter(request, response)
            return
        }

        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (!("GET".equals(httpRequest.method, ignoreCase = true) || "HEAD".equals(httpRequest.method, ignoreCase = true))) {
            logger.debug("The request '{}' is not a GET or HEAD request so skipping the filter.",
                    httpRequest.requestURL)
            chain.doFilter(request, response)
            return
        }

        var matches = false
        for (urlRegularExpression in urlRegularExpressions) {
            val p = Pattern.compile(urlRegularExpression)
            val m = p.matcher(httpRequest.requestURL)
            if (m.matches()) {
                matches = true
                break
            }
        }

        if (!matches) {
            logger.debug("The request '{}' doesn't match any of the configured regular expressions so skipping the filter.",
                    httpRequest.requestURL)
            chain.doFilter(request, response)
            return
        }

        val resourceRequest: ResourceRequest?
        try {
            resourceRequest = urlSigningVerifier!!.verify(httpRequest.queryString, httpRequest.remoteAddr,
                    httpRequest.requestURL.toString(), strict)

            if (resourceRequest == null) {
                logger.error("Unable to process httpRequest '{}' because we got a null object as the verification.",
                        httpRequest.requestURL)
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to process http request because we got a null object as the verification.")
                return
            }

            when (resourceRequest.status) {
                ResourceRequest.Status.Ok -> {
                    logger.trace("The request '{}' matched a regular expression path and was accepted as a properly signed url.",
                            httpRequest.requestURL)
                    chain.doFilter(httpRequest, response)
                    return
                }
                ResourceRequest.Status.BadRequest -> {
                    logger.debug(
                            "Unable to process httpRequest '{}' because it was rejected as a Bad Request, usually a problem with query string: {}",
                            httpRequest.requestURL, resourceRequest.rejectionReason)
                    httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST)
                    return
                }
                ResourceRequest.Status.Forbidden -> {
                    logger.debug(
                            "Unable to process httpRequest '{}' because is was rejected as Forbidden, usually a problem with making policy matching the signature: {}",
                            httpRequest.requestURL, resourceRequest.rejectionReason)
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    return
                }
                ResourceRequest.Status.Gone -> {
                    logger.debug("Unable to process httpRequest '{}' because is was rejected as Gone: {}",
                            httpRequest.requestURL, resourceRequest.rejectionReason)
                    httpResponse.sendError(HttpServletResponse.SC_GONE)
                    return
                }
                else -> {
                    logger.error(
                            "Unable to process httpRequest '{}' because is was rejected as status {} which is not a status we should be handling here. This must be due to a code change and is a bug.: {}",
                            httpRequest.requestURL, resourceRequest.status, resourceRequest.rejectionReason)
                    httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    return
                }
            }
        } catch (e: UrlSigningException) {
            logger.error("Unable to verify request for '{}' with query string '{}' from host '{}' because: {}",
                    httpRequest.requestURL, httpRequest.queryString, httpRequest.remoteAddr,
                    ExceptionUtils.getStackTrace(e))
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    String.format("%s is unable to verify request for '%s' with query string '%s' from host '%s' because: %s",
                            name, httpRequest.requestURL, httpRequest.queryString, httpRequest.remoteAddr,
                            ExceptionUtils.getStackTrace(e)))
            return
        }

    }

    @Throws(ServletException::class)
    override fun init(filterConfig: FilterConfig) {

    }

    override fun destroy() {}

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        logger.info("Updating UrlSigningFilter")

        val enableFilterConfig = OsgiUtil.getOptCfg(properties!!, ENABLE_FILTER_CONFIG_KEY)
        if (enableFilterConfig.isSome) {
            enabled = java.lang.Boolean.parseBoolean(enableFilterConfig.get())
            if (enabled) {
                logger.info("The UrlSigningFilter is configured to be enabled.")
            } else {
                logger.info("The UrlSigningFilter is configured to be disabled.")
            }
        } else {
            enabled = true
            logger.info(
                    "The UrlSigningFilter is enabled by default. Use the '{}' property in its properties file to enable or disable it.",
                    ENABLE_FILTER_CONFIG_KEY)
        }

        val strictFilterConfig = OsgiUtil.getOptCfg(properties, STRICT_FILTER_CONFIG_KEY)
        if (strictFilterConfig.isSome) {
            strict = java.lang.Boolean.parseBoolean(strictFilterConfig.get())
            if (strict) {
                logger.info("The UrlSigningFilter is configured to use strict checking of resource URLs.")
            } else {
                logger.info("The UrlSigningFilter is configured to not use strict checking of resource URLs.")
            }
        } else {
            strict = true
            logger.info(
                    "The UrlSigningFilter is using strict checking of resource URLs by default. Use the '{}' property in its properties file to enable or disable it.",
                    STRICT_FILTER_CONFIG_KEY)
        }

        // Clear the current set of keys
        urlRegularExpressions.clear()

        if (properties == null) {
            logger.warn("UrlSigningFilter has no paths to match")
            return
        }

        val propertyKeys = properties.keys()
        while (propertyKeys.hasMoreElements()) {
            val propertyKey = propertyKeys.nextElement()
            if (!propertyKey.startsWith(URL_REGEX_PREFIX)) continue

            val urlRegularExpression = StringUtils.trimToNull(properties.get(propertyKey) as String)
            logger.debug("Looking for configuration of {} and found '{}'", propertyKey, urlRegularExpression)
            // Has the url signing provider been fully configured
            if (urlRegularExpression == null) {
                logger.debug(
                        "Unable to configure url regular expression with id '{}' because it is missing. Stopping to look for new keys.",
                        propertyKey)
                break
            }

            urlRegularExpressions.add(urlRegularExpression)
        }

        if (urlRegularExpressions.size == 0) {
            logger.info("UrlSigningFilter configured to not verify any urls.")
            return
        }
        logger.info("Finished updating UrlSigningFilter")
    }

    companion object {
        /** The prefix in the configuration file to define the regex that will match a url path.  */
        val URL_REGEX_PREFIX = "url.regex"
        /** The property in the configuration file to enable or disable this filter.  */
        val ENABLE_FILTER_CONFIG_KEY = "enabled"

        /** The property in the configuration file to enable or disable strict checking of the resource.  */
        val STRICT_FILTER_CONFIG_KEY = "strict"

        private val logger = LoggerFactory.getLogger(UrlSigningFilter::class.java)
    }

}
