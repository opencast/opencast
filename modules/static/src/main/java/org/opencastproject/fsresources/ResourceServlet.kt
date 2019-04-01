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

package org.opencastproject.fsresources

import org.opencastproject.security.api.Role
import org.opencastproject.security.api.SecurityService
import org.opencastproject.util.XProperties

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * Serves static content from a configured path on the filesystem. In production systems, this should be replaced with
 * apache httpd or another web server optimized for serving static content.
 */
class ResourceServlet : HttpServlet {

    protected var root: String? = null
    protected var serverAlias: String? = null
    protected var builder: DocumentBuilder? = null
    var securityService: SecurityService? = null

    constructor() {}

    constructor(alias: String, filesystemDir: String) {
        root = filesystemDir
        serverAlias = alias
    }

    @Throws(ParserConfigurationException::class)
    fun activate(cc: ComponentContext) {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val props = XProperties()
        props.bundleContext = cc.bundleContext

        val rootKey = cc.properties.get("rootKey") as String
        if (rootKey != null) {
            if (root == null)
                root = cc.properties.get(rootKey) as String
            if (root == null) {
                logger.warn("No value for key " + rootKey
                        + " found for this service.  Defaulting to value of org.opencastproject.download.directory.")
            }
        }

        if (root == null) {
            root = cc.bundleContext.getProperty("org.opencastproject.download.directory") as String
        }

        if (root == null) {
            throw IllegalStateException("Unable to find root for servlet, please check your config files.")
        }

        if (serverAlias == null)
            serverAlias = cc.properties.get("alias") as String

        // Get the interpreted values of the keys.
        props["root"] = root!!
        root = props.getProperty("root")
        props["serverAlias"] = serverAlias!!
        serverAlias = props.getProperty("serverAlias")

        if (serverAlias == null || StringUtils.isBlank(serverAlias)) {
            throw IllegalStateException("Unable to create servlet, alias property is null")
        } else if (root == null) {
            throw IllegalStateException("Unable to create servlet, root property is null")
        }

        if (serverAlias!![0] != '/') {
            serverAlias = '/' + serverAlias!!
        }

        val rootDir = File(root!!)
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) {
                logger.error("Unable to create directories for {}!", rootDir.absolutePath)
                return
            }
        }

        if (!(rootDir.isDirectory || rootDir.isFile)) {
            throw IllegalStateException("Unable to create servlet for " + serverAlias + " because "
                    + rootDir.absolutePath + " is not a file or directory!")
        }
        logger.debug("Activating servlet with alias " + serverAlias + " on directory " + rootDir.absolutePath)
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.servlet.http.HttpServlet.doGet
     */
    @Throws(ServletException::class, IOException::class)
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        logger.debug("Looking for static resource '{}'", req.requestURI)
        var path: String? = req.pathInfo
        var normalized: String? = path?.trim { it <= ' ' }?.replace("/+".toRegex(), "/")?.replace("\\.\\.".toRegex(), "")
                ?: "/"
        if (path == null) {
            path = "/"
        } else {
            // Replace duplicate slashes with a single slash, and remove .. from the listing
            path = path.trim { it <= ' ' }.replace("/+".toRegex(), "/").replace("\\.\\.".toRegex(), "")
        }
        if (normalized != null && normalized.startsWith("/") && normalized.length > 1) {
            normalized = normalized.substring(1)
        }

        val f = File(root, normalized!!)
        var allowed = true
        if (f.isFile && f.canRead()) {
            allowed = checkDirectory(f.parentFile)
            if (!allowed) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN)
                return
            }

            logger.debug("Serving static resource '{}'", f.absolutePath)
            val `in` = FileInputStream(f)
            try {
                IOUtils.copyLarge(`in`, resp.outputStream)
            } finally {
                IOUtils.closeQuietly(`in`)
            }
        } else if (f.isDirectory && f.canRead()) {
            allowed = checkDirectory(f)
            if (!allowed) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN)
                return
            }

            logger.debug("Serving index page for '{}'", f.absolutePath)
            val out = resp.writer
            resp.contentType = "text/html;charset=UTF-8"
            out.write("<html>")
            out.write("<head><title>File Index for $normalized</title></head>")
            out.write("<body>")
            out.write("<table>")
            val sdf = SimpleDateFormat()
            sdf.applyPattern(dateFormat)
            for (child in f.listFiles()!!) {

                if (child.isDirectory && !checkDirectory(child)) {
                    continue
                }

                val sb = StringBuffer()
                sb.append("<tr><td>")
                sb.append("<a href=\"")
                if (req.requestURL[req.requestURL.length - 1] != '/') {
                    sb.append(req.requestURL.append("/").append(child.name))
                } else {
                    sb.append(req.requestURL.append(child.name))
                }
                sb.append("\">")
                sb.append(child.name)
                sb.append("</a>")
                sb.append("</td><td>")
                sb.append(formatLength(child.length()))
                sb.append("</td><td>")
                sb.append(sdf.format(child.lastModified()))
                sb.append("</td>")
                sb.append("</tr>")
                out.write(sb.toString())
            }
            out.write("</table>")
            out.write("</body>")
            out.write("</html>")
        } else {
            logger.debug("Error state for '{}', returning HTTP 404", f.absolutePath)
            resp.sendError(HttpServletResponse.SC_NOT_FOUND)
        }
    }

    protected fun checkDirectory(directory: File): Boolean {
        // If security is off then everyone has access!
        if (securityService == null) {
            return true
        }

        var allowed = false
        var aclFile: File? = null
        try {
            val pathBits = directory.absolutePath.split(("" + File.separatorChar).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            aclFile = File(directory, pathBits[pathBits.size - 1] + ".acl")
            allowed = isUserAllowed(aclFile)
        } catch (e: IOException) {
            logger.debug("Unable to read file " + aclFile!!.absolutePath + ", denying access by default")
        } catch (e: SAXException) {
            if (aclFile!!.isFile) {
                logger.warn("Invalid XML in file " + aclFile.absolutePath + ", denying access by default")
            }
        } catch (e: XPathExpressionException) {
            logger.error("Wrong xPath expression:", e)
        }

        return allowed
    }

    @Throws(SAXException::class, IOException::class, XPathExpressionException::class)
    protected fun isUserAllowed(aclFile: File): Boolean {
        val aclDoc = builder!!.parse(aclFile)
        val xPath = XPathFactory.newInstance().newXPath()
        val roles = xPath.evaluate("//*[local-name() = 'role']", aclDoc, XPathConstants.NODESET) as NodeList
        for (i in 0 until roles.length) {
            val role = roles.item(i)
            for (userRole in securityService!!.user.roles) {
                if (userRole.name == role.textContent) {
                    return true
                }
            }
        }
        return false
    }

    protected fun formatLength(length: Long): String {
        // FIXME: Why isn't there a library function for this?!
        // TODO: Make this better
        return if (length > 1073741824.0) {
            (length / 1073741824).toString() + " GB"
        } else if (length > 1048576.0) {
            (length / 1048576).toString() + " MB"
        } else if (length > 1024.0) {
            (length / 1024).toString() + " KB"
        } else {
            "$length B"
        }
    }

    companion object {
        private val serialVersionUID = 1L
        private val logger = LoggerFactory.getLogger(ResourceServlet::class.java)
        private val dateFormat = "yyyy-MM-dd HH:mm:ss Z"
    }
}
