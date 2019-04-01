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

import com.entwinemedia.fn.data.json.Jsons.arr
import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.HttpServletResponse.SC_CONFLICT
import javax.servlet.http.HttpServletResponse.SC_CREATED
import javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_OK
import org.apache.commons.lang3.StringUtils.trimToNull
import org.opencastproject.index.service.util.RestUtils.okJsonList
import org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING
import org.opencastproject.util.doc.rest.RestParameter.Type.TEXT

import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.adminui.util.QueryPreprocessor
import org.opencastproject.index.service.api.IndexService
import org.opencastproject.index.service.impl.index.group.Group
import org.opencastproject.index.service.impl.index.group.GroupIndexSchema
import org.opencastproject.index.service.impl.index.group.GroupSearchQuery
import org.opencastproject.index.service.resources.list.query.GroupsListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.matterhorn.search.SearchResult
import org.opencastproject.matterhorn.search.SearchResultItem
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.userdirectory.ConflictException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.RestUtil
import org.opencastproject.util.data.Option
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.Objects
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
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

@Path("/")
@RestService(name = "groups", title = "Group service", abstractText = "Provides operations for groups", notes = ["This service offers the default groups CRUD operations for the admin interface.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
class GroupsEndpoint {

    /** The admin UI search index  */
    private var searchIndex: AdminUISearchIndex? = null

    /** The security service  */
    private var securityService: SecurityService? = null

    /** The user directory service  */
    private var userDirectoryService: UserDirectoryService? = null

    /** The index service  */
    private var indexService: IndexService? = null

    /** OSGi callback for the security service.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback for the index service.  */
    fun setIndexService(indexService: IndexService) {
        this.indexService = indexService
    }

    /** OSGi callback for users services.  */
    fun setUserDirectoryService(userDirectoryService: UserDirectoryService) {
        this.userDirectoryService = userDirectoryService
    }

    /** OSGi callback for the search index.  */
    fun setSearchIndex(searchIndex: AdminUISearchIndex) {
        this.searchIndex = searchIndex
    }

    /** OSGi callback.  */
    protected fun activate(cc: ComponentContext) {
        logger.info("Activate the Admin ui - Groups facade endpoint")
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("groups.json")
    @RestQuery(name = "allgroupsasjson", description = "Returns a list of groups", returnDescription = "List of groups for the current user's organization as JSON.", restParameters = [RestParameter(name = "filter", isRequired = false, type = STRING, description = "Filter used for the query, formatted like: 'filter1:value1,filter2:value2'"), RestParameter(name = "sort", isRequired = false, type = STRING, description = "The sort order. May include any of the following: NAME, DESCRIPTION, ROLE. " + "Add '_DESC' to reverse the sort order (e.g. NAME_DESC)."), RestParameter(name = "limit", isRequired = false, type = INTEGER, defaultValue = "100", description = "The maximum number of items to return per page."), RestParameter(name = "offset", isRequired = false, type = INTEGER, defaultValue = "0", description = "The page number.")], reponses = [RestResponse(responseCode = SC_OK, description = "The groups.")])
    @Throws(IOException::class)
    fun getGroups(@QueryParam("filter") filter: String, @QueryParam("sort") sort: String,
                  @QueryParam("offset") offset: Int, @QueryParam("limit") limit: Int): Response {

        val query = GroupSearchQuery(securityService!!.organization.id,
                securityService!!.user)

        val optSort = Opt.nul(trimToNull(sort))
        val optOffset = Option.option(offset)
        var optLimit = Option.option(limit)
        // If the limit is set to 0, this is not taken into account
        if (optLimit.isSome && limit == 0) {
            optLimit = Option.none()
        }

        val filters = RestUtils.parseFilter(filter)
        for (name in filters.keys) {
            if (GroupsListQuery.FILTER_NAME_NAME == name) {
                query.withName(filters[name])
            } else if (GroupsListQuery.FILTER_TEXT_NAME == name) {
                query.withText(QueryPreprocessor.sanitize(filters[name]))
            }
        }

        if (optSort.isSome) {
            val sortCriteria = RestUtils.parseSortQueryParameter(optSort.get())
            for (criterion in sortCriteria) {
                when (criterion.fieldName) {
                    GroupIndexSchema.NAME -> query.sortByName(criterion.order)
                    GroupIndexSchema.DESCRIPTION -> query.sortByDescription(criterion.order)
                    GroupIndexSchema.ROLE -> query.sortByRole(criterion.order)
                    GroupIndexSchema.MEMBERS -> query.sortByMembers(criterion.order)
                    GroupIndexSchema.ROLES -> query.sortByRoles(criterion.order)
                    else -> throw WebApplicationException(Status.BAD_REQUEST)
                }
            }
        }

        if (optLimit.isSome)
            query.withLimit(optLimit.get())
        if (optOffset.isSome)
            query.withOffset(optOffset.get())

        val results: SearchResult<Group>
        try {
            results = searchIndex!!.getByQuery(query)
        } catch (e: SearchIndexException) {
            logger.error("The External Search Index was not able to get the groups list.", e)
            return RestUtil.R.serverError()
        }

        val userNames = Arrays.stream(results.items).flatMap { item -> item.source.members.stream() }
                .collect<List<String>, Any>(Collectors.toList())

        val users = HashMap<String, User>(userNames.size)
        userDirectoryService!!.loadUsers(userNames).forEachRemaining { user -> users[user.username] = user }

        val groupsJSON = ArrayList<JValue>()
        for (item in results.items) {
            val group = item.source
            val fields = ArrayList<Field>()
            fields.add(f("id", v(group.identifier)))
            fields.add(f("name", v(group.name, Jsons.BLANK)))
            fields.add(f("description", v(group.description, Jsons.BLANK)))
            fields.add(f("role", v(group.role)))
            fields.add(
                    f("users", membersToJSON(group.members.stream().map<User>(Function<String, User> { users[it] }).filter(Predicate<User> { Objects.nonNull(it) }).iterator())))
            groupsJSON.add(obj(fields))
        }

        return okJsonList(groupsJSON, offset, limit, results.hitCount)
    }

    @DELETE
    @Path("{id}")
    @RestQuery(name = "removegrouop", description = "Remove a group", returnDescription = "Returns no content", pathParameters = [RestParameter(name = "id", description = "The group identifier", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "Group deleted"), RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to delete the group with admin role."), RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found."), RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "An internal server error occured.")])
    @Throws(NotFoundException::class)
    fun removeGroup(@PathParam("id") groupId: String): Response {
        try {
            indexService!!.removeGroup(groupId)
            return Response.noContent().build()
        } catch (e: NotFoundException) {
            return Response.status(SC_NOT_FOUND).build()
        } catch (e: UnauthorizedException) {
            return Response.status(SC_FORBIDDEN).build()
        } catch (e: Exception) {
            logger.error("Unable to delete group {}", groupId, e)
            throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
        }

    }

    @POST
    @Path("")
    @RestQuery(name = "createGroup", description = "Add a group", returnDescription = "Returns Created (201) if the group has been created", restParameters = [RestParameter(name = "name", description = "The group name", isRequired = true, type = STRING), RestParameter(name = "description", description = "The group description", isRequired = false, type = STRING), RestParameter(name = "roles", description = "Comma seperated list of roles", isRequired = false, type = TEXT), RestParameter(name = "users", description = "Comma seperated list of members", isRequired = false, type = TEXT)], reponses = [RestResponse(responseCode = SC_CREATED, description = "Group created"), RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long"), RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to create a group with admin role."), RestResponse(responseCode = SC_CONFLICT, description = "An group with this name already exists.")])
    fun createGroup(@FormParam("name") name: String, @FormParam("description") description: String,
                    @FormParam("roles") roles: String, @FormParam("users") users: String): Response {
        try {
            indexService!!.createGroup(name, description, roles, users)
        } catch (e: IllegalArgumentException) {
            logger.warn(e.message)
            return Response.status(Status.BAD_REQUEST).build()
        } catch (e: UnauthorizedException) {
            return Response.status(SC_FORBIDDEN).build()
        } catch (e: ConflictException) {
            return Response.status(SC_CONFLICT).build()
        }

        return Response.status(Status.CREATED).build()
    }

    @PUT
    @Path("{id}")
    @RestQuery(name = "updateGroup", description = "Update a group", returnDescription = "Return the status codes", pathParameters = [RestParameter(name = "id", description = "The group identifier", isRequired = true, type = STRING)], restParameters = [RestParameter(name = "name", description = "The group name", isRequired = true, type = STRING), RestParameter(name = "description", description = "The group description", isRequired = false, type = STRING), RestParameter(name = "roles", description = "Comma seperated list of roles", isRequired = false, type = TEXT), RestParameter(name = "users", description = "Comma seperated list of members", isRequired = false, type = TEXT)], reponses = [RestResponse(responseCode = SC_OK, description = "Group updated"), RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to update the group with admin role."), RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found"), RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long")])
    @Throws(NotFoundException::class)
    fun updateGroup(@PathParam("id") groupId: String, @FormParam("name") name: String,
                    @FormParam("description") description: String, @FormParam("roles") roles: String,
                    @FormParam("users") users: String): Response {
        try {
            indexService!!.updateGroup(groupId, name, description, roles, users)
        } catch (e: IllegalArgumentException) {
            logger.warn(e.message)
            return Response.status(Status.BAD_REQUEST).build()
        } catch (ex: UnauthorizedException) {
            return Response.status(SC_FORBIDDEN).build()
        }

        return Response.ok().build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @RestQuery(name = "getGroup", description = "Get a single group", returnDescription = "Return the status codes", pathParameters = [RestParameter(name = "id", description = "The group identifier", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "Group found and returned as JSON"), RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found")])
    @Throws(NotFoundException::class, SearchIndexException::class)
    fun getGroup(@PathParam("id") groupId: String): Response {
        val groupOpt = indexService!!.getGroup(groupId, searchIndex)
        if (groupOpt.isNone)
            throw NotFoundException("Group $groupId does not exist.")

        val group = groupOpt.get()
        val users = userDirectoryService!!.loadUsers(group.members)
        return RestUtils.okJson(obj(f("id", v(group.identifier)), f("name", v(group.name, Jsons.BLANK)),
                f("description", v(group.description, Jsons.BLANK)), f("role", v(group.role, Jsons.BLANK)),
                f("roles", rolesToJSON(group.roles)), f("users", membersToJSON(users))))
    }

    /**
     * Generate a JSON array based on the given set of roles
     *
     * @param roles
     * the roles source
     * @return a JSON array ([JValue]) with the given roles
     */
    private fun rolesToJSON(roles: Set<String>): JValue {
        val rolesJSON = ArrayList<JValue>()

        for (role in roles) {
            rolesJSON.add(v(role))
        }
        return arr(rolesJSON)
    }

    /**
     * Generate a JSON array based on the given set of members
     *
     * @param members
     * the members source
     * @return a JSON array ([JValue]) with the given members
     */
    private fun membersToJSON(members: Iterator<User>): JValue {
        val membersJSON = ArrayList<JValue>()

        while (members.hasNext()) {
            val user = members.next()
            var name = user.username

            if (StringUtils.isNotBlank(user.name)) {
                name = user.name
            }

            membersJSON.add(obj(f("username", v(user.username)), f("name", v(name))))
        }

        return arr(membersJSON)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(GroupsEndpoint::class.java)
    }
}
