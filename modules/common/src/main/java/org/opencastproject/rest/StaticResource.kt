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

package org.opencastproject.rest

import org.opencastproject.util.MimeTypes

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URL

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * A static resource for registration with the http service.
 */
class StaticResource
/**
 * Constructs a static resources.
 *
 * @param classpath
 * the classpath to the static resources
 * @param alias
 * the URL alias
 * @param welcomeFile
 * the default welcome file
 */
(classloader: ClassLoader, classpath: String, alias: String, welcomeFile: String) : HttpServlet() {

    /** The classpath to search for the static resources  */
    protected var classpath: String? = null

    /** The base URL for these static resources  */
    var defaultUrl: String? = null
        protected set

    /** The welcome file to redirect to, if only the alias is specified in the request  */
    protected var welcomeFile: String? = null

    /** The classloader to use to search for the static resources.  */
    protected var classloader: ClassLoader? = null

    init {
        this.classpath = classpath
        this.defaultUrl = alias
        this.welcomeFile = welcomeFile
        this.classloader = classloader
    }

    /**
     * Activates the static resource when it is instantiated using Declarative Services.
     *
     * @param componentProperties
     * the DS component context
     */
    fun activate(componentProperties: Map<*, *>) {
        if (welcomeFile == null)
            welcomeFile = componentProperties.get("welcome.file") as String
        var welcomeFileSpecified = true
        if (welcomeFile == null) {
            welcomeFileSpecified = false
            welcomeFile = "index.html"
        }
        if (defaultUrl == null)
            defaultUrl = componentProperties.get("alias") as String
        if (classpath == null)
            classpath = componentProperties.get("classpath") as String
        logger.info("registering classpath:{} at {} with welcome file {} {}", classpath, defaultUrl, welcomeFile,
                if (welcomeFileSpecified) "" else "(via default)")
    }

    override fun toString(): String {
        return "StaticResource [alias=$defaultUrl, classpath=$classpath, welcome file=$welcomeFile]"
    }

    @Throws(ServletException::class, IOException::class)
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val pathInfo = req.pathInfo
        val servletPath = req.servletPath
        val path = if (pathInfo == null) servletPath else servletPath + pathInfo
        logger.debug("handling path {}, pathInfo={}, servletPath={}", path, pathInfo, servletPath)

        // If the URL points to a "directory", redirect to the welcome file
        if ("/" == path || defaultUrl == path || defaultUrl!! + "/" == path) {
            val redirectPath: String
            if ("/" == defaultUrl) {
                redirectPath = "/" + welcomeFile!!
            } else {
                redirectPath = "$defaultUrl/$welcomeFile"
            }
            logger.debug("redirecting {} to {}", path, redirectPath)
            resp.sendRedirect(redirectPath)
            return
        }

        // Find and deliver the resource
        var classpathToResource: String
        if (pathInfo == null) {
            if (servletPath != defaultUrl) {
                classpathToResource = classpath!! + servletPath
            } else {
                classpathToResource = "$classpath/$welcomeFile"
            }
        } else {
            classpathToResource = classpath!! + pathInfo
        }

        // Make sure we are using an absolute path
        if (!classpathToResource.startsWith("/"))
            classpathToResource = "/$classpathToResource"

        // Try to load the resource from the classloader
        val url = classloader!!.getResource(classpathToResource)

        if (url == null) {
            resp.sendError(404)
            return
        }
        logger.debug("opening url {} {}", classpathToResource, url)
        var `in`: InputStream? = null
        try {
            `in` = url.openStream()
            val md5 = DigestUtils.md5Hex(`in`!!)
            if (md5 == req.getHeader("If-None-Match")) {
                resp.status = 304
                return
            }
            resp.setHeader("ETag", md5)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
        val contentType = MimeTypes.getMimeType(url.path)
        if (MimeTypes.DEFAULT_TYPE != contentType) {
            resp.setHeader("Content-Type", contentType)
        }
        try {
            `in` = url.openStream()
            IOUtils.copy(`in`!!, resp.outputStream)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    companion object {
        /** The java.io.serialization uid  */
        private val serialVersionUID = 1L

        /** The logger  */
        private val logger = LoggerFactory.getLogger(StaticResource::class.java!!)
    }
}
