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

package org.opencastproject.runtimeinfo

import org.opencastproject.rest.RestConstants.SERVICES_FILTER

import org.opencastproject.rest.RestConstants
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.userdirectory.UserIdRoleProvider
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.google.gson.Gson

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.osgi.framework.Constants
import org.osgi.framework.InvalidSyntaxException
import org.osgi.framework.ServiceReference
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.MalformedURLException
import java.net.URL
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.Objects
import java.util.SortedSet
import java.util.TreeSet
import java.util.stream.Collectors

import javax.servlet.Servlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

/**
 * This REST endpoint provides information about the runtime environment, including the services and user interfaces
 * deployed and the current login context.
 *
 * If the 'org.opencastproject.anonymous.feedback.url' is set in config.properties, this service will also update the
 * opencast project with the contents of the getRuntimeInfo() json feed.
 */
@Path("/")
@RestService(name = "RuntimeInfo", title = "Runtime Information", abstractText = "This service provides information about the runtime environment, including the services that are " + "deployed and the current user context.", notes = [])
class RuntimeInfo {

    private var userIdRoleProvider: UserIdRoleProvider? = null
    private var securityService: SecurityService? = null
    private var bundleContext: BundleContext? = null
    private var serverUrl: URL? = null

    private val restServiceReferences: Array<ServiceReference<*>>?
        @Throws(InvalidSyntaxException::class)
        get() = bundleContext!!.getAllServiceReferences(null, Companion.getSERVICES_FILTER())

    private val userInterfaceServiceReferences: Array<ServiceReference<*>>?
        @Throws(InvalidSyntaxException::class)
        get() = bundleContext!!.getAllServiceReferences(Servlet::class.java.name, "(&(alias=*)(classpath=*))")

    // Add the current user's roles
    // Add the current user's organizational information
    val myInfo: String
        @GET
        @Path("me.json")
        @Produces(MediaType.APPLICATION_JSON)
        @RestQuery(name = "me", description = "Information about the curent user", reponses = [RestResponse(description = "Returns information about the current user", responseCode = HttpServletResponse.SC_OK)], returnDescription = "")
        get() {
            val result = HashMap<String, Any>()

            val user = securityService!!.user
            val jsonUser = HashMap<String, String>()
            jsonUser["username"] = user.username
            jsonUser["name"] = user.name
            jsonUser["email"] = user.email
            jsonUser["provider"] = user.provider
            result["user"] = jsonUser
            if (userIdRoleProvider != null) {
                result["userRole"] = UserIdRoleProvider.getUserIdRole(user.username)
            }
            result["roles"] = user.roles.stream()
                    .map<String>(Function<Role, String> { it.getName() })
                    .collect<List<String>, Any>(Collectors.toList())
            val org = securityService!!.organization

            val jsonOrg = HashMap<String, Any>()
            jsonOrg["id"] = org.id
            jsonOrg["name"] = org.name
            jsonOrg["adminRole"] = org.adminRole
            jsonOrg["anonymousRole"] = org.anonymousRole
            jsonOrg["properties"] = org.properties
            result["org"] = jsonOrg

            return gson.toJson(result)
        }

    private val userInterfacesAsJson: List<Map<String, String>>
        @Throws(InvalidSyntaxException::class)
        get() {
            val result = ArrayList<Map<String, String>>()
            val serviceRefs = userInterfaceServiceReferences ?: return result
            for (ref in sort(serviceRefs)) {
                val description = ref.getProperty(Constants.SERVICE_DESCRIPTION) as String
                val version = ref.bundle.version.toString()
                val alias = ref.getProperty("alias") as String
                val welcomeFile = ref.getProperty("welcome.file") as String
                val welcomePath = if ("/" == alias) alias + welcomeFile else "$alias/$welcomeFile"
                val endpoint = HashMap<String, String>()
                endpoint["description"] = description
                endpoint["version"] = version
                endpoint["welcomepage"] = serverUrl!!.toString() + welcomePath
                result.add(endpoint)
            }
            return result
        }

    protected fun setUserIdRoleProvider(userIdRoleProvider: UserIdRoleProvider) {
        this.userIdRoleProvider = userIdRoleProvider
    }

    protected fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    @Throws(MalformedURLException::class)
    fun activate(cc: ComponentContext) {
        logger.debug("start()")
        this.bundleContext = cc.bundleContext
        serverUrl = URL(bundleContext!!.getProperty(OpencastConstants.SERVER_URL_PROPERTY))
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("components.json")
    @RestQuery(name = "services", description = "List the REST services and user interfaces running on this host", reponses = [RestResponse(description = "The components running on this host", responseCode = HttpServletResponse.SC_OK)], returnDescription = "")
    @Throws(MalformedURLException::class, InvalidSyntaxException::class)
    fun getRuntimeInfo(@Context request: HttpServletRequest): String {
        val organization = securityService!!.organization

        // Get request protocol and port
        val targetScheme = request.scheme

        // Create the engage target URL
        var targetEngageBaseUrl: URL? = null
        val orgEngageBaseUrl = organization.properties[ENGAGE_URL_PROPERTY]
        if (StringUtils.isNotBlank(orgEngageBaseUrl)) {
            try {
                targetEngageBaseUrl = URL(orgEngageBaseUrl)
            } catch (e: MalformedURLException) {
                logger.warn("Engage url '{}' of organization '{}' is malformed", orgEngageBaseUrl, organization.id)
            }

        }

        if (targetEngageBaseUrl == null) {
            logger.debug(
                    "Using 'org.opencastproject.server.url' as a fallback for the non-existing organization level key '{}' for the components.json response",
                    ENGAGE_URL_PROPERTY)
            targetEngageBaseUrl = URL(targetScheme, serverUrl!!.host, serverUrl!!.port, serverUrl!!.file)
        }

        // Create the admin target URL
        var targetAdminBaseUrl: URL? = null
        val orgAdminBaseUrl = organization.properties[ADMIN_URL_PROPERTY]
        if (StringUtils.isNotBlank(orgAdminBaseUrl)) {
            try {
                targetAdminBaseUrl = URL(orgAdminBaseUrl)
            } catch (e: MalformedURLException) {
                logger.warn("Admin url '{}' of organization '{}' is malformed", orgAdminBaseUrl, organization.id)
            }

        }

        if (targetAdminBaseUrl == null) {
            logger.debug(
                    "Using 'org.opencastproject.server.url' as a fallback for the non-existing organization level key '{}' for the components.json response",
                    ADMIN_URL_PROPERTY)
            targetAdminBaseUrl = URL(targetScheme, serverUrl!!.host, serverUrl!!.port, serverUrl!!.file)
        }


        val json = HashMap<String, Any>()
        json["engage"] = targetEngageBaseUrl.toString()
        json["admin"] = targetAdminBaseUrl.toString()
        json["rest"] = getRestEndpointsAsJson(request)
        json["ui"] = userInterfacesAsJson

        return gson.toJson(json)
    }

    @Throws(MalformedURLException::class, InvalidSyntaxException::class)
    private fun getRestEndpointsAsJson(request: HttpServletRequest): List<Map<String, String>> {
        val result = ArrayList<Map<String, String>>()
        val serviceRefs = restServiceReferences ?: return result
        for (servletRef in sort(serviceRefs)) {
            val servletContextPath = servletRef.getProperty(RestConstants.SERVICE_PATH_PROPERTY) as String
            val endpoint = HashMap<String, String>()
            endpoint["description"] = servletRef.getProperty(Constants.SERVICE_DESCRIPTION) as String
            endpoint["version"] = servletRef.bundle.version.toString()
            endpoint["type"] = servletRef.getProperty(RestConstants.SERVICE_TYPE_PROPERTY) as String
            val url = URL(request.scheme, request.serverName, request.serverPort, servletContextPath)
            endpoint["path"] = servletContextPath
            endpoint["docs"] = UrlSupport.concat(url.toExternalForm(), "/docs") // This is a Opencast convention
            result.add(endpoint)
        }
        return result
    }

    companion object {

        private val logger = LoggerFactory.getLogger(RuntimeInfo::class.java)

        /** Configuration properties id  */
        private val ADMIN_URL_PROPERTY = "org.opencastproject.admin.ui.url"
        private val ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url"

        private val gson = Gson()

        /**
         * The rest publisher looks for any non-servlet with the 'opencast.service.path' property
         */
        val SERVICE_FILTER = ("(&(!(objectClass=javax.servlet.Servlet))("
                + RestConstants.SERVICE_PATH_PROPERTY + "=*))")

        /**
         * Returns the array of references sorted by their Constants.SERVICE_DESCRIPTION property.
         *
         * @param references
         * the referencens
         * @return the sorted set of references
         */
        private fun sort(references: Array<ServiceReference<*>>): SortedSet<ServiceReference<*>> {
            val sortedServiceRefs = TreeSet<ServiceReference<*>> { o1, o2 ->
                val o1Description = Objects.toString(o1.getProperty(Constants.SERVICE_DESCRIPTION), o1.toString())
                val o2Description = Objects.toString(o2.getProperty(Constants.SERVICE_DESCRIPTION), o2.toString())
                o1Description.compareTo(o2Description)
            }
            sortedServiceRefs.addAll(Arrays.asList<ServiceReference>(*references))
            return sortedServiceRefs
        }
    }
}
