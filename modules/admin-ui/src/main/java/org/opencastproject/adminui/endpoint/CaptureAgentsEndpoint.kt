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
import org.apache.commons.lang3.StringUtils.trimToNull
import org.apache.http.HttpStatus.SC_OK
import org.opencastproject.index.service.util.RestUtils.okJson
import org.opencastproject.index.service.util.RestUtils.okJsonList
import org.opencastproject.util.DateTimeSupport.toUTC
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.adminui.util.TextFilter
import org.opencastproject.capture.CaptureParameters
import org.opencastproject.capture.admin.api.Agent
import org.opencastproject.capture.admin.api.AgentState
import org.opencastproject.capture.admin.api.CaptureAgentStateService
import org.opencastproject.index.service.resources.list.query.AgentsListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchQuery.Order
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.SmartIterator
import org.opencastproject.util.data.Option
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import kotlin.collections.Map.Entry
import java.util.Properties

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

@Path("/")
@RestService(name = "captureAgents", title = "Capture agents façade service", abstractText = "Provides operations for the capture agents", notes = ["This service offers the default capture agents CRUD Operations for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class CaptureAgentsEndpoint {

    /** The capture agent service  */
    private var service: CaptureAgentStateService? = null

    private var securityService: SecurityService? = null

    /**
     * Sets the capture agent service
     *
     * @param service
     * the capture agent service to set
     */
    fun setCaptureAgentService(service: CaptureAgentStateService) {
        this.service = service
    }

    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("agents.json")
    @RestQuery(name = "getAgents", description = "Return all of the known capture agents on the system", restParameters = [RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING), RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING), RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING), RestParameter(defaultValue = "false", description = "Define if the inputs should or not returned with the capture agent.", isRequired = false, name = "inputs", type = RestParameter.Type.BOOLEAN), RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: STATUS, NAME OR LAST_UPDATED.  Add '_DESC' to reverse the sort order (e.g. STATUS_DESC).", type = STRING)], reponses = [RestResponse(description = "An XML representation of the agent capabilities", responseCode = HttpServletResponse.SC_OK)], returnDescription = "")
    fun getAgents(@QueryParam("limit") limit: Int, @QueryParam("offset") offset: Int,
                  @QueryParam("inputs") inputs: Boolean, @QueryParam("filter") filter: String, @QueryParam("sort") sort: String): Response {
        var filterName = Option.none()
        var filterStatus = Option.none()
        var filterLastUpdated = Option.none()
        var filterText = Option.none()
        val optSort = Option.option(trimToNull(sort))

        val filters = RestUtils.parseFilter(filter)
        for (name in filters.keys) {
            if (AgentsListQuery.FILTER_NAME_NAME == name)
                filterName = Option.some(filters[name])
            if (AgentsListQuery.FILTER_STATUS_NAME == name)
                filterStatus = Option.some(filters[name])
            if (AgentsListQuery.FILTER_LAST_UPDATED == name) {
                try {
                    filterLastUpdated = Option.some(java.lang.Long.parseLong(filters[name]))
                } catch (e: NumberFormatException) {
                    logger.info("Unable to parse long {}", filters[name])
                    return Response.status(Status.BAD_REQUEST).build()
                }

            }
            if (AgentsListQuery.FILTER_TEXT_NAME == name && StringUtils.isNotBlank(filters[name]))
                filterText = Option.some(filters[name])
        }

        // Filter agents by filter criteria
        var filteredAgents: MutableList<Agent> = ArrayList()
        for ((_, agent) in service!!.knownAgents) {

            // Filter list
            if (filterName.isSome && filterName.get() != agent.name
                    || filterStatus.isSome && filterStatus.get() != agent.state
                    || filterLastUpdated.isSome && filterLastUpdated.get() !== agent.lastHeardFrom
                    || filterText.isSome && !TextFilter.match(filterText.get(), agent.name, agent.state))
                continue
            filteredAgents.add(agent)
        }
        val total = filteredAgents.size

        // Sort by status, name or last updated date
        if (optSort.isSome) {
            val sortCriteria = RestUtils.parseSortQueryParameter(optSort.get())
            Collections.sort(filteredAgents, Comparator { agent1, agent2 ->
                for (criterion in sortCriteria) {
                    val order = criterion.order
                    when (criterion.fieldName) {
                        "status" -> {
                            return@Comparator if (order == Order.Descending) agent2.state.compareTo(agent1.state) else agent1.state.compareTo(agent2.state)
                        }
                        "name" -> {
                            return@Comparator if (order == Order.Descending) agent2.name.compareTo(agent1.name) else agent1.name.compareTo(agent2.name)
                        }
                        "updated" -> {
                            return@Comparator if (order == Order.Descending) agent2.lastHeardFrom!!.compareTo(agent1.lastHeardFrom!!) else agent1.lastHeardFrom!!.compareTo(agent2.lastHeardFrom!!)
                        }
                        else -> {
                            logger.info("Unknown sort type: {}", criterion.fieldName)
                            return@Comparator 0
                        }
                    }
                }
                0
            })
        }

        // Apply Limit and offset
        filteredAgents = SmartIterator<Agent>(limit, offset).applyLimitAndOffset(filteredAgents)

        // Run through and build a map of updates (rather than states)
        val agentsJSON = ArrayList<JValue>()
        for (agent in filteredAgents) {
            agentsJSON.add(generateJsonAgent(agent, inputs, false))
        }

        return okJsonList(agentsJSON, offset, limit, total.toLong())
    }

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "removeAgent", description = "Remove record of a given capture agent", pathParameters = [RestParameter(name = "name", description = "The name of a given capture agent", isRequired = true, type = RestParameter.Type.STRING)], restParameters = [], reponses = [RestResponse(description = "{agentName} removed", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "The agent {agentname} does not exist", responseCode = HttpServletResponse.SC_NOT_FOUND)], returnDescription = "")
    @Throws(NotFoundException::class, UnauthorizedException::class)
    fun removeAgent(@PathParam("name") agentName: String): Response {
        if (service == null)
            return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build()

        SecurityUtil.checkAgentAccess(securityService!!, agentName)

        service!!.removeAgent(agentName)

        logger.debug("The agent {} was successfully removed", agentName)
        return Response.status(SC_OK).build()
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getAgent", description = "Return the capture agent including its configuration and capabilities", pathParameters = [RestParameter(description = "Name of the capture agent", isRequired = true, name = "name", type = RestParameter.Type.STRING)], restParameters = [], reponses = [RestResponse(description = "A JSON representation of the capture agent", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "The agent {name} does not exist in the system", responseCode = HttpServletResponse.SC_NOT_FOUND)], returnDescription = "")
    @Throws(NotFoundException::class)
    fun getAgent(@PathParam("name") agentName: String): Response {
        if (service != null) {
            val agent = service!!.getAgent(agentName)
            return if (agent != null) {
                okJson(generateJsonAgent(agent, true, true))
            } else {
                Response.status(Status.NOT_FOUND).build()
            }
        } else {
            return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build()
        }
    }

    /**
     * Generate a JSON Object for the given capture agent
     *
     * @param agent
     * The target capture agent
     * @param withInputs
     * Whether the agent has inputs
     * @param details
     * Whether the configuration and capabilities should be serialized
     * @return A [JValue] representing the capture agent
     */
    private fun generateJsonAgent(agent: Agent, withInputs: Boolean, details: Boolean): JValue {
        val fields = ArrayList<Field>()
        fields.add(f("Status", v(AgentState.TRANSLATION_PREFIX + agent.state.toUpperCase(), Jsons.BLANK)))
        fields.add(f("Name", v(agent.name)))
        fields.add(f("Update", v(toUTC(agent.lastHeardFrom), Jsons.BLANK)))
        fields.add(f("URL", v(agent.url, Jsons.BLANK)))

        if (withInputs) {
            val devices = agent.capabilities[CaptureParameters.CAPTURE_DEVICE_NAMES] as String
            fields.add(f("inputs", if (StringUtils.isEmpty(devices)) arr() else generateJsonDevice(devices.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())))
        }

        if (details) {
            fields.add(f("configuration", generateJsonProperties(agent.configuration)))
            fields.add(f("capabilities", generateJsonProperties(agent.capabilities)))
        }

        return obj(fields)
    }

    /**
     * Generate JSON property list
     *
     * @param properties
     * Java properties to be serialized
     * @return A JSON array containing the Java properties as key/value paris
     */
    private fun generateJsonProperties(properties: Properties?): JValue {
        val fields = ArrayList<JValue>()
        if (properties != null) {
            for (key in properties.stringPropertyNames()) {
                fields.add(obj(f("key", v(key)), f("value", v(properties.getProperty(key)))))
            }
        }
        return arr(fields)
    }

    /**
     * Generate a JSON devices list
     *
     * @param devices
     * an array of devices String
     * @return A [JValue] representing the devices
     */
    private fun generateJsonDevice(devices: Array<String>): JValue {
        val jsonDevices = ArrayList<JValue>()
        for (device in devices) {
            jsonDevices.add(obj(f("id", v(device)), f("value", v(TRANSLATION_KEY_PREFIX + device.toUpperCase()))))
        }
        return arr(jsonDevices)
    }

    companion object {

        private val TRANSLATION_KEY_PREFIX = "CAPTURE_AGENT.DEVICE."

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CaptureAgentsEndpoint::class.java)
    }
}
