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

package org.opencastproject.adminui.endpoint

import java.lang.String.CASE_INSENSITIVE_ORDER
import org.apache.commons.lang3.StringUtils.trimToEmpty
import org.apache.commons.lang3.StringUtils.trimToNull
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.apache.http.HttpStatus.SC_CONFLICT
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.apache.http.HttpStatus.SC_OK
import org.opencastproject.util.RestUtil.getEndpointUrl
import org.opencastproject.util.UrlSupport.uri
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.adminui.util.TextFilter
import org.opencastproject.index.service.resources.list.query.UsersListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchQuery.Order
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.security.impl.jpa.JpaOrganization
import org.opencastproject.security.impl.jpa.JpaRole
import org.opencastproject.security.impl.jpa.JpaUser
import org.opencastproject.userdirectory.JpaUserAndRoleProvider
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.SmartIterator
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

import javax.ws.rs.DELETE
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "users", title = "User service", abstractText = "Provides operations for users", notes = ["This service offers the default users CRUD Operations for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class UsersEndpoint {

    /** The global user directory service  */
    protected var userDirectoryService: UserDirectoryService

    /** The internal role and user provider  */
    private var jpaUserAndRoleProvider: JpaUserAndRoleProvider? = null

    /** The security service  */
    private var securityService: SecurityService? = null

    /** Base url of this endpoint  */
    private var endpointBaseUrl: String? = null

    /**
     * Sets the user directory service
     *
     * @param userDirectoryService
     * the userDirectoryService to set
     */
    fun setUserDirectoryService(userDirectoryService: UserDirectoryService) {
        this.userDirectoryService = userDirectoryService
    }

    /**
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * @param jpaUserAndRoleProvider
     * the user provider to set
     */
    fun setJpaUserAndRoleProvider(jpaUserAndRoleProvider: JpaUserAndRoleProvider) {
        this.jpaUserAndRoleProvider = jpaUserAndRoleProvider
    }

    /** OSGi callback.  */
    protected fun activate(cc: ComponentContext) {
        logger.info("Activate the Admin ui - Users facade endpoint")
        val endpointUrl = getEndpointUrl(cc)
        endpointBaseUrl = UrlSupport.concat(endpointUrl.a, endpointUrl.b)
    }

    @GET
    @Path("users.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "allusers", description = "Returns a list of users", returnDescription = "Returns a JSON representation of the list of user accounts", restParameters = [RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING), RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: STATUS, NAME OR LAST_UPDATED.  Add '_DESC' to reverse the sort order (e.g. STATUS_DESC).", type = STRING), RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING), RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The user accounts.")])
    @Throws(IOException::class)
    fun getUsers(@QueryParam("filter") filter: String, @QueryParam("sort") sort: String?,
                 @QueryParam("limit") limit: Int, @QueryParam("offset") offset: Int): Response {
        var sort = sort
        var limit = limit
        if (limit < 1)
            limit = 100

        sort = trimToNull(sort)
        var filterName: String? = null
        var filterRole: String? = null
        var filterProvider: String? = null
        var filterText: String? = null

        val filters = RestUtils.parseFilter(filter)
        for (name in filters.keys) {
            val value = filters[name]
            if (UsersListQuery.FILTER_NAME_NAME == name) {
                filterName = value
            } else if (UsersListQuery.FILTER_ROLE_NAME == name) {
                filterRole = value
            } else if (UsersListQuery.FILTER_PROVIDER_NAME == name) {
                filterProvider = value
            } else if (UsersListQuery.FILTER_TEXT_NAME == name && StringUtils.isNotBlank(value)) {
                filterText = value
            }
        }

        // Filter users by filter criteria
        var filteredUsers: MutableList<User> = ArrayList()
        val i = userDirectoryService.users
        while (i.hasNext()) {
            val user = i.next()

            // Filter list
            val finalFilterRole = filterRole
            if (filterName != null && filterName != user.name
                    || filterRole != null && user.roles.stream().noneMatch { r -> r.name == finalFilterRole }
                    || filterProvider != null && filterProvider != user.provider
                    || (filterText != null
                            && !TextFilter.match(filterText, user.username, user.name, user.email, user.provider)
                            && !TextFilter.match(filterText,
                            user.roles.stream().map<String>(Function<Role, String> { it.getName() }).collect<String, *>(Collectors.joining(" "))))) {
                continue
            }
            filteredUsers.add(user)
        }
        val total = filteredUsers.size

        // Sort by name, description or role
        if (sort != null) {
            val sortCriteria = RestUtils.parseSortQueryParameter(sort)
            filteredUsers.sort { user1, user2 ->
                for (criterion in sortCriteria) {
                    val order = criterion.order
                    when (criterion.fieldName) {
                        "name" -> {
                            if (order == Order.Descending)
                                return@filteredUsers.sort CASE_INSENSITIVE_ORDER . compare trimToEmpty(user2.name), trimToEmpty(user1.name))
                            return@filteredUsers.sort CASE_INSENSITIVE_ORDER . compare trimToEmpty(user1.name), trimToEmpty(user2.name))
                        }
                        "username" -> {
                            if (order == Order.Descending)
                                return@filteredUsers.sort CASE_INSENSITIVE_ORDER
                                        .compare(trimToEmpty(user2.username), trimToEmpty(user1.username))
                            return@filteredUsers.sort CASE_INSENSITIVE_ORDER
                                    .compare(trimToEmpty(user1.username), trimToEmpty(user2.username))
                        }
                        "email" -> {
                            if (order == Order.Descending)
                                return@filteredUsers.sort CASE_INSENSITIVE_ORDER . compare trimToEmpty(user2.email), trimToEmpty(user1.email))
                            return@filteredUsers.sort CASE_INSENSITIVE_ORDER . compare trimToEmpty(user1.email), trimToEmpty(user2.email))
                        }
                        "roles" -> {
                            val roles1 = user1.roles.stream().map<String>(Function<Role, String> { it.getName() }).collect<String, *>(Collectors.joining(","))
                            val roles2 = user1.roles.stream().map<String>(Function<Role, String> { it.getName() }).collect<String, *>(Collectors.joining(","))
                            if (order == Order.Descending)
                                return@filteredUsers.sort CASE_INSENSITIVE_ORDER . compare trimToEmpty(roles2), trimToEmpty(roles1))
                            return@filteredUsers.sort CASE_INSENSITIVE_ORDER . compare trimToEmpty(roles1), trimToEmpty(roles2))
                        }
                        "provider" -> {
                            if (order == Order.Descending)
                                return@filteredUsers.sort CASE_INSENSITIVE_ORDER
                                        .compare(trimToEmpty(user2.provider), trimToEmpty(user1.provider))
                            return@filteredUsers.sort CASE_INSENSITIVE_ORDER
                                    .compare(trimToEmpty(user1.provider), trimToEmpty(user2.provider))
                        }
                        else -> {
                            logger.info("Unknown sort type: {}", criterion.fieldName)
                            return@filteredUsers.sort 0
                        }
                    }
                }
                0
            }
        }

        // Apply Limit and offset
        filteredUsers = SmartIterator<User>(limit, offset).applyLimitAndOffset(filteredUsers)

        val usersJSON = ArrayList<Map<String, Any>>()
        for (user in filteredUsers) {
            usersJSON.add(generateJsonUser(user))
        }

        val response = HashMap<String, Any>()
        response["results"] = usersJSON
        response["count"] = usersJSON.size
        response["offset"] = offset
        response["limit"] = limit
        response["total"] = total
        return Response.ok(gson.toJson(response)).build()
    }

    @POST
    @Path("/")
    @RestQuery(name = "createUser", description = "Create a new  user", returnDescription = "The location of the new ressource", restParameters = [RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING), RestParameter(description = "The password.", isRequired = true, name = "password", type = STRING), RestParameter(description = "The name.", isRequired = false, name = "name", type = STRING), RestParameter(description = "The email.", isRequired = false, name = "email", type = STRING), RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array")], reponses = [RestResponse(responseCode = SC_CREATED, description = "User has been created."), RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to create a user with a admin role."), RestResponse(responseCode = SC_CONFLICT, description = "An user with this username already exist.")])
    fun createUser(@FormParam("username") username: String, @FormParam("password") password: String,
                   @FormParam("name") name: String, @FormParam("email") email: String, @FormParam("roles") roles: String): Response {

        if (StringUtils.isBlank(username)) {
            return Response.status(SC_BAD_REQUEST).entity("Missing username").build()
        }
        if (StringUtils.isBlank(password)) {
            return Response.status(SC_BAD_REQUEST).entity("Missing password").build()
        }

        val existingUser = jpaUserAndRoleProvider!!.loadUser(username)
        if (existingUser != null) {
            return Response.status(SC_CONFLICT).build()
        }

        val organization = securityService!!.organization as JpaOrganization
        var rolesSet: MutableSet<JpaRole>?
        try {
            rolesSet = parseJsonRoles(roles)
        } catch (e: IllegalArgumentException) {
            logger.debug("Received invalid JSON for roles", e)
            return Response.status(SC_BAD_REQUEST).entity("Invalid JSON for roles").build()
        }

        if (rolesSet == null) {
            rolesSet = HashSet()
            rolesSet.add(JpaRole(organization.getAnonymousRole(), organization))
        }

        val user = JpaUser(username, password, organization, name, email, jpaUserAndRoleProvider!!.getName(), true,
                rolesSet)
        try {
            jpaUserAndRoleProvider!!.addUser(user)
            return Response.created(uri(endpointBaseUrl, user.getUsername() + ".json")).build()
        } catch (e: UnauthorizedException) {
            return Response.status(SC_FORBIDDEN).build()
        }

    }

    @GET
    @Path("{username}.json")
    @RestQuery(name = "getUser", description = "Get an user", returnDescription = "Status ok", pathParameters = [RestParameter(name = "username", type = STRING, isRequired = true, description = "The username")], reponses = [RestResponse(responseCode = SC_OK, description = "User has been found."), RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.")])
    fun getUser(@PathParam("username") username: String): Response {

        val user = userDirectoryService.loadUser(username) ?: return Response.status(SC_NOT_FOUND).build()

        return Response.ok(gson.toJson(generateJsonUser(user))).build()
    }

    @PUT
    @Path("{username}.json")
    @RestQuery(name = "updateUser", description = "Update an user", returnDescription = "Status ok", restParameters = [RestParameter(description = "The password.", isRequired = false, name = "password", type = STRING), RestParameter(description = "The name.", isRequired = false, name = "name", type = STRING), RestParameter(description = "The email.", isRequired = false, name = "email", type = STRING), RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array")], pathParameters = [RestParameter(name = "username", type = STRING, isRequired = true, description = "The username")], reponses = [RestResponse(responseCode = SC_OK, description = "User has been updated."), RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to update a user with admin role."), RestResponse(responseCode = SC_BAD_REQUEST, description = "Invalid data provided.")])
    fun updateUser(@PathParam("username") username: String, @FormParam("password") password: String,
                   @FormParam("name") name: String, @FormParam("email") email: String, @FormParam("roles") roles: String): Response {

        val user = jpaUserAndRoleProvider!!.loadUser(username)
                ?: return createUser(username, password, name, email, roles)

        var rolesSet: MutableSet<JpaRole>?
        try {
            rolesSet = parseJsonRoles(roles)
        } catch (e: IllegalArgumentException) {
            logger.debug("Received invalid JSON for roles", e)
            return Response.status(SC_BAD_REQUEST).build()
        }

        val organization = securityService!!.organization as JpaOrganization
        if (rolesSet == null) {
            //  use the previous roles if no new ones are provided
            rolesSet = HashSet()
            for (role in user.roles) {
                rolesSet.add(JpaRole(role.name, organization, role.description, role.type))
            }
        }

        try {
            jpaUserAndRoleProvider!!.updateUser(JpaUser(username, password, organization, name, email,
                    jpaUserAndRoleProvider!!.getName(), true, rolesSet))
            userDirectoryService.invalidate(username)
            return Response.status(SC_OK).build()
        } catch (ex: UnauthorizedException) {
            return Response.status(Response.Status.FORBIDDEN).build()
        } catch (e: NotFoundException) {
            return Response.serverError().build()
        }

    }

    @DELETE
    @Path("{username}.json")
    @RestQuery(name = "deleteUser", description = "Deleter a new  user", returnDescription = "Status ok", pathParameters = [RestParameter(name = "username", type = STRING, isRequired = true, description = "The username")], reponses = [RestResponse(responseCode = SC_OK, description = "User has been deleted."), RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to delete a user with admin role."), RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.")])
    @Throws(NotFoundException::class)
    fun deleteUser(@PathParam("username") username: String): Response {
        val organization = securityService!!.organization

        try {
            jpaUserAndRoleProvider!!.deleteUser(username, organization.id)
            userDirectoryService.invalidate(username)
        } catch (e: NotFoundException) {
            logger.debug("User {} not found.", username)
            return Response.status(SC_NOT_FOUND).build()
        } catch (e: UnauthorizedException) {
            return Response.status(SC_FORBIDDEN).build()
        } catch (e: Exception) {
            logger.error("Error during deletion of user {}: {}", username, e)
            return Response.status(SC_INTERNAL_SERVER_ERROR).build()
        }

        logger.debug("User {} removed.", username)
        return Response.status(SC_OK).build()
    }

    /**
     * Parse a JSON roles string.
     *
     * @param roles
     * Array of roles as JSON strings.
     * @return Set of roles or null
     * @throws IllegalArgumentException
     * Invalid JSON data
     */
    @Throws(IllegalArgumentException::class)
    private fun parseJsonRoles(roles: String): MutableSet<JpaRole>? {
        val rolesList: List<JsonRole>?
        try {
            rolesList = gson.fromJson<List<JsonRole>>(roles, listType)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException(e)
        }

        if (rolesList == null) {
            return null
        }

        val organization = securityService!!.organization as JpaOrganization
        val rolesSet = HashSet<JpaRole>()
        for (role in rolesList) {
            try {
                rolesSet.add(JpaRole(role.name, organization, null, role.getType()))
            } catch (e: NullPointerException) {
                throw IllegalArgumentException(e)
            }

        }
        return rolesSet
    }

    private fun generateJsonUser(user: User): Map<String, Any> {
        // Prepare the roles
        val userData = HashMap<String, Any>()
        userData["username"] = user.username
        userData["manageable"] = user.isManageable
        userData["name"] = user.name
        userData["email"] = user.email
        userData["provider"] = user.provider
        userData["roles"] = user.roles.stream()
                .sorted(Comparator.comparing<Role, String>(Function<Role, String> { it.getName() }))
                .map { r -> JsonRole(r.name, r.type) }
                .collect<List<JsonRole>, Any>(Collectors.toList())
        return userData
    }

    internal inner class JsonRole(val name: String, type: Role.Type) {
        private val type: String

        init {
            this.type = type.toString()
        }

        fun getType(): Role.Type {
            return Role.Type.valueOf(type)
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(UsersEndpoint::class.java)

        /** For JSON serialization  */
        private val listType = object : TypeToken<ArrayList<JsonRole>>() {

        }.type
        private val gson = Gson()
    }

}
