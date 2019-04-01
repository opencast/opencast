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
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_OK
import org.apache.commons.lang3.StringUtils.trimToNull
import org.opencastproject.index.service.util.RestUtils.okJsonList
import org.opencastproject.util.RestUtil.R.conflict
import org.opencastproject.util.RestUtil.R.noContent
import org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.adminui.util.TextFilter
import org.opencastproject.authorization.xacml.manager.api.AclService
import org.opencastproject.authorization.xacml.manager.api.AclServiceException
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl
import org.opencastproject.authorization.xacml.manager.endpoint.JsonConv
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl
import org.opencastproject.index.service.resources.list.query.AclsListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchQuery.Order
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.security.api.AccessControlEntry
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Option
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.StreamOp
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JObject
import com.entwinemedia.fn.data.json.JValue

import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

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

@Path("/")
@RestService(name = "acl", title = "Acl service", abstractText = "Provides operations for acl", notes = ["This service offers the default acl CRUD Operations for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class AclEndpoint {

    /** The acl service factory  */
    private var aclServiceFactory: AclServiceFactory? = null

    /** The security service  */
    private var securityService: SecurityService? = null

    private val fullAccessControlEntry = object : Fn<AccessControlEntry, JValue>() {
        override fun apply(ace: AccessControlEntry): JValue {
            return full(ace)
        }
    }

    private val fullManagedAcl = object : Fn<ManagedAcl, JValue>() {
        override fun apply(acl: ManagedAcl): JValue {
            return full(acl)
        }
    }

    /**
     * @param aclServiceFactory
     * the aclServiceFactory to set
     */
    fun setAclServiceFactory(aclServiceFactory: AclServiceFactory) {
        this.aclServiceFactory = aclServiceFactory
    }

    /**
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback.  */
    protected fun activate(cc: ComponentContext) {
        logger.info("Activate the Admin ui - Acl facade endpoint")
    }

    private fun aclService(): AclService {
        return aclServiceFactory!!.serviceFor(securityService!!.organization)
    }

    @GET
    @Path("acls.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "allaclasjson", description = "Returns a list of acls", returnDescription = "Returns a JSON representation of the list of acls available the current user's organization", restParameters = [RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING), RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: NAME. Add '_DESC' to reverse the sort order (e.g. NAME_DESC).", type = STRING), RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING), RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The list of ACL's has successfully been returned")])
    @Throws(IOException::class)
    fun getAclsAsJson(@QueryParam("filter") filter: String, @QueryParam("sort") sort: String,
                      @QueryParam("offset") offset: Int, @QueryParam("limit") limit: Int): Response {
        var limit = limit
        if (limit < 1)
            limit = 100
        val optSort = Opt.nul(trimToNull(sort))
        var filterName = Option.none()
        var filterText = Option.none()

        val filters = RestUtils.parseFilter(filter)
        for (name in filters.keys) {
            val value = filters[name]
            if (AclsListQuery.FILTER_NAME_NAME == name) {
                filterName = Option.some(value)
            } else if (AclsListQuery.FILTER_TEXT_NAME == name && StringUtils.isNotBlank(value)) {
                filterText = Option.some(value)
            }
        }

        // Filter acls by filter criteria
        val filteredAcls = ArrayList<ManagedAcl>()
        for (acl in aclService().acls) {
            // Filter list
            if (filterName.isSome && filterName.get() != acl.name || filterText.isSome && !TextFilter.match(filterText.get(), acl.name)) {
                continue
            }
            filteredAcls.add(acl)
        }
        val total = filteredAcls.size

        // Sort by name, description or role
        if (optSort.isSome) {
            val sortCriteria = RestUtils.parseSortQueryParameter(optSort.get())
            Collections.sort(filteredAcls, Comparator { acl1, acl2 ->
                for (criterion in sortCriteria) {
                    val order = criterion.order
                    when (criterion.fieldName) {
                        "name" -> {
                            return@Comparator if (order == Order.Descending) ObjectUtils.compare(acl2.name, acl1.name) else ObjectUtils.compare(acl1.name, acl2.name)
                        }
                        else -> {
                            logger.info("Unkown sort type: {}", criterion.fieldName)
                            return@Comparator 0
                        }
                    }
                }
                0
            })
        }

        // Apply Limit and offset
        val aclJSON = Stream.`$`(filteredAcls).drop(offset)
                .apply(if (limit > 0) StreamOp.id<ManagedAcl>().take(limit) else StreamOp.id()).map(fullManagedAcl)
                .toList()
        return okJsonList(aclJSON, offset, limit, total.toLong())
    }

    @DELETE
    @Path("{id}")
    @RestQuery(name = "deleteacl", description = "Delete an ACL", returnDescription = "Delete an ACL", pathParameters = [RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER)], reponses = [RestResponse(responseCode = SC_OK, description = "The ACL has successfully been deleted"), RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"), RestResponse(responseCode = SC_CONFLICT, description = "The ACL could not be deleted, there are still references on it")])
    @Throws(NotFoundException::class)
    fun deleteAcl(@PathParam("id") aclId: Long): Response {
        try {
            if (!aclService().deleteAcl(aclId))
                return conflict()
        } catch (e: AclServiceException) {
            logger.warn("Error deleting manged acl with id '{}': {}", aclId, e)
            throw WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR)
        }

        return noContent()
    }

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "createacl", description = "Create an ACL", returnDescription = "Create an ACL", restParameters = [RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING), RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The ACL has successfully been added"), RestResponse(responseCode = SC_CONFLICT, description = "An ACL with the same name already exists"), RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL")])
    fun createAcl(@FormParam("name") name: String, @FormParam("acl") accessControlList: String): Response {
        val acl = parseAcl.apply(accessControlList)
        val managedAcl = aclService().createAcl(acl, name).toOpt()
        if (managedAcl.isNone) {
            logger.info("An ACL with the same name '{}' already exists", name)
            throw WebApplicationException(Response.Status.CONFLICT)
        }
        return RestUtils.okJson(full(managedAcl.get()))
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "updateacl", description = "Update an ACL", returnDescription = "Update an ACL", pathParameters = [RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER)], restParameters = [RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING), RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The ACL has successfully been updated"), RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"), RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL")])
    @Throws(NotFoundException::class)
    fun updateAcl(@PathParam("id") aclId: Long, @FormParam("name") name: String,
                  @FormParam("acl") accessControlList: String): Response {
        val org = securityService!!.organization
        val acl = parseAcl.apply(accessControlList)
        val managedAcl = ManagedAclImpl(aclId, name, org.id, acl)
        if (!aclService().updateAcl(managedAcl)) {
            logger.info("No ACL with id '{}' could be found under organization '{}'", aclId, org.id)
            throw NotFoundException()
        }
        return RestUtils.okJson(full(managedAcl))
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getacl", description = "Return the ACL by the given id", returnDescription = "Return the ACL by the given id", pathParameters = [RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER)], reponses = [RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"), RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found")])
    @Throws(NotFoundException::class)
    fun getAcl(@PathParam("id") aclId: Long): Response {
        for (managedAcl in aclService().getAcl(aclId)) {
            return RestUtils.okJson(full(managedAcl))
        }
        logger.info("No ACL with id '{}' could by found", aclId)
        throw NotFoundException()
    }

    fun full(ace: AccessControlEntry): JObject {
        return obj(f(JsonConv.KEY_ROLE, v(ace.role!!)), f(JsonConv.KEY_ACTION, v(ace.action!!)),
                f(JsonConv.KEY_ALLOW, v(ace.isAllow)))
    }

    fun full(acl: AccessControlList): JObject {
        return obj(f(JsonConv.KEY_ACE, arr(Stream.`$`(acl.entries).map(fullAccessControlEntry))))
    }

    fun full(acl: ManagedAcl): JObject {
        val fields = ArrayList<Field>()
        fields.add(f(JsonConv.KEY_ID, v(acl.id!!)))
        fields.add(f(JsonConv.KEY_NAME, v(acl.name)))
        fields.add(f(JsonConv.KEY_ORGANIZATION_ID, v(acl.organizationId)))
        fields.add(f(JsonConv.KEY_ACL, full(acl.acl)))
        return obj(fields)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(AclEndpoint::class.java)

        private val parseAcl = object : Fn<String, AccessControlList>() {
            override fun apply(acl: String): AccessControlList {
                try {
                    return AccessControlParser.parseAcl(acl)
                } catch (e: Exception) {
                    logger.warn("Unable to parse ACL")
                    throw WebApplicationException(Response.Status.BAD_REQUEST)
                }

            }
        }
    }

}
