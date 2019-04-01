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

import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.index.service.resources.list.provider.ServersListProvider
import org.opencastproject.index.service.resources.list.query.ServersListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.serviceregistry.api.HostRegistration
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceStatistics
import org.opencastproject.util.SmartIterator
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.entwinemedia.fn.data.json.JValue

import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashSet
import java.util.concurrent.TimeUnit

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "ServerProxyService", title = "UI Servers", abstractText = "This service provides the server data for the UI.", notes = ["These Endpoints deliver informations about the server required for the UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class ServerEndpoint {

    private var serviceRegistry: ServiceRegistry? = null

    private enum class Sort {
        COMPLETED, CORES, HOSTNAME, MAINTENANCE, MEANQUEUETIME, MEANRUNTIME, ONLINE, QUEUED, RUNNING
    }

    /**
     * Comparator for the servers list
     */
    private inner class ServerComparator internal constructor(private val sortType: Sort, ascending: Boolean?) : Comparator<JSONObject> {
        private val ascending = true

        init {
            this.ascending = ascending
        }

        override fun compare(host1: JSONObject, host2: JSONObject): Int {
            val result: Int

            when (sortType) {
                ServerEndpoint.Sort.ONLINE -> {
                    val status1 = host1[KEY_ONLINE] as Boolean
                    val status2 = host2[KEY_ONLINE] as Boolean
                    result = status1.compareTo(status2)
                }
                ServerEndpoint.Sort.CORES -> result = (host1[KEY_CORES] as Int).compareTo(host2[KEY_CORES] as Int)
                ServerEndpoint.Sort.COMPLETED -> result = (host1[KEY_COMPLETED] as Long).compareTo(host2[KEY_COMPLETED] as Long)
                ServerEndpoint.Sort.QUEUED -> result = (host1[KEY_QUEUED] as Int).compareTo(host2[KEY_QUEUED] as Int)
                ServerEndpoint.Sort.MAINTENANCE -> {
                    val mtn1 = host1[KEY_MAINTENANCE] as Boolean
                    val mtn2 = host2[KEY_MAINTENANCE] as Boolean
                    result = mtn1.compareTo(mtn2)
                }
                ServerEndpoint.Sort.RUNNING -> result = (host1[KEY_RUNNING] as Int).compareTo(host2[KEY_RUNNING] as Int)
                ServerEndpoint.Sort.MEANQUEUETIME -> result = (host1[KEY_MEAN_QUEUE_TIME] as Long).compareTo(host2[KEY_MEAN_QUEUE_TIME] as Long)
                ServerEndpoint.Sort.MEANRUNTIME -> result = (host1[KEY_MEAN_RUN_TIME] as Long).compareTo(host2[KEY_MEAN_RUN_TIME] as Long)
                ServerEndpoint.Sort.HOSTNAME -> {
                    val name1 = host1[KEY_HOSTNAME] as String
                    val name2 = host2[KEY_HOSTNAME] as String
                    result = name1.compareTo(name2)
                }
                else -> {
                    val name1 = host1[KEY_HOSTNAME] as String
                    val name2 = host2[KEY_HOSTNAME] as String
                    result = name1.compareTo(name2)
                }
            }

            return if (ascending) result else -1 * result
        }
    }

    /** OSGi callback for the service registry.  */
    fun setServiceRegistry(serviceRegistry: ServiceRegistry) {
        this.serviceRegistry = serviceRegistry
    }

    protected fun activate(bundleContext: BundleContext) {
        logger.info("Activate job endpoint")
    }

    @GET
    @Path("servers.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(description = "Returns the list of servers", name = "servers", restParameters = [RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = INTEGER), RestParameter(name = "offset", description = "The offset", isRequired = false, type = INTEGER), RestParameter(name = "filter", description = "Filter results by hostname, status or free text query", isRequired = false, type = STRING), RestParameter(name = "sort", description = "The sort order.  May include any "
            + "of the following: COMPLETED (jobs), CORES, HOSTNAME, MAINTENANCE, MEANQUEUETIME (mean for jobs), "
            + "MEANRUNTIME (mean for jobs), ONLINE, QUEUED (jobs), RUNNING (jobs)."
            + "The suffix must be :ASC for ascending or :DESC for descending sort order (e.g. HOSTNAME:DESC).", isRequired = false, type = STRING)], reponses = [RestResponse(description = "Returns the list of jobs from Opencast", responseCode = HttpServletResponse.SC_OK)], returnDescription = "The list of servers")
    @Throws(Exception::class)
    fun getServers(@QueryParam("limit") limit: Int, @QueryParam("offset") offset: Int,
                   @QueryParam("filter") filter: String, @QueryParam("sort") sort: String): Response {

        val query = ServersListQuery()
        EndpointUtil.addRequestFiltersToQuery(filter, query)
        query.setLimit(limit)
        query.setOffset(offset)

        val servers = ArrayList<JSONObject>()
        // Get service statistics for all hosts and services
        val servicesStatistics = serviceRegistry!!.serviceStatistics
        for (server in serviceRegistry!!.hostRegistrations) {
            // Calculate statistics per server
            var jobsCompleted: Long = 0
            var jobsRunning = 0
            var jobsQueued = 0
            var sumMeanRuntime: Long = 0
            var sumMeanQueueTime: Long = 0
            var totalServiceOnHost = 0
            var offlineJobProducerServices = 0
            var totalJobProducerServices = 0
            val serviceTypes = HashSet<String>()
            for (serviceStat in servicesStatistics) {
                if (server.baseUrl == serviceStat.serviceRegistration.host) {
                    totalServiceOnHost++
                    jobsCompleted += serviceStat.finishedJobs.toLong()
                    jobsRunning += serviceStat.runningJobs
                    jobsQueued += serviceStat.queuedJobs
                    // mean time values are given in milliseconds,
                    // we should convert them to seconds,
                    // because the adminNG UI expect it in this format
                    sumMeanRuntime += TimeUnit.MILLISECONDS.toSeconds(serviceStat.meanRunTime)
                    sumMeanQueueTime += TimeUnit.MILLISECONDS.toSeconds(serviceStat.meanQueueTime)
                    if (!serviceStat.serviceRegistration.isOnline && serviceStat.serviceRegistration.isJobProducer) {
                        offlineJobProducerServices++
                        totalJobProducerServices++
                    } else if (serviceStat.serviceRegistration.isJobProducer) {
                        totalJobProducerServices++
                    }
                    serviceTypes.add(serviceStat.serviceRegistration.serviceType)
                }
            }
            val meanRuntime = if (totalServiceOnHost > 0) Math.round(sumMeanRuntime.toDouble() / totalServiceOnHost) else 0L
            val meanQueueTime = if (totalServiceOnHost > 0) Math.round(sumMeanQueueTime.toDouble() / totalServiceOnHost) else 0L

            val vOnline = server.isOnline
            val vMaintenance = server.isMaintenanceMode
            val vHostname = server.baseUrl
            val vCores = server.cores

            if (query.hostname.isSome && !StringUtils.equalsIgnoreCase(vHostname, query.hostname.get()))
                continue

            if (query.status.isSome) {
                if (StringUtils.equalsIgnoreCase(
                                ServersListProvider.SERVER_STATUS_ONLINE,
                                query.status.get()) && !vOnline)
                    continue
                if (StringUtils.equalsIgnoreCase(
                                ServersListProvider.SERVER_STATUS_OFFLINE,
                                query.status.get()) && vOnline)
                    continue
                if (StringUtils.equalsIgnoreCase(
                                ServersListProvider.SERVER_STATUS_MAINTENANCE,
                                query.status.get()) && !vMaintenance)
                    continue
            }

            if (query.freeText.isSome
                    && !StringUtils.containsIgnoreCase(vHostname, query.freeText.get())
                    && !StringUtils.containsIgnoreCase(server.ipAddress, query.freeText.get()))
                continue

            val jsonServer = JSONObject()
            jsonServer[KEY_ONLINE] = vOnline && offlineJobProducerServices <= totalJobProducerServices / 2
            jsonServer[KEY_MAINTENANCE] = vMaintenance
            jsonServer[KEY_HOSTNAME] = vHostname
            jsonServer[KEY_CORES] = vCores
            jsonServer[KEY_RUNNING] = jobsRunning
            jsonServer[KEY_QUEUED] = jobsQueued
            jsonServer[KEY_COMPLETED] = jobsCompleted
            jsonServer[KEY_MEAN_RUN_TIME] = meanRuntime
            jsonServer[KEY_MEAN_QUEUE_TIME] = meanQueueTime
            servers.add(jsonServer)
        }

        // Sorting
        var sortKey = Sort.HOSTNAME
        var ascending: Boolean? = true
        if (StringUtils.isNotBlank(sort)) {
            try {
                val sortCriterion = RestUtils.parseSortQueryParameter(sort).iterator().next()
                sortKey = Sort.valueOf(sortCriterion.fieldName.toUpperCase())
                ascending = SearchQuery.Order.Ascending == sortCriterion.order || SearchQuery.Order.None == sortCriterion.order
            } catch (ex: WebApplicationException) {
                logger.warn("Failed to parse sort criterion \"{}\", invalid format.", sort)
            } catch (ex: IllegalArgumentException) {
                logger.warn("Can not apply sort criterion \"{}\", no field with this name.", sort)
            }

        }

        val jsonList = JSONArray()
        if (!servers.isEmpty()) {
            Collections.sort(servers, ServerComparator(sortKey, ascending))
            jsonList.addAll(SmartIterator(
                    query.limit.getOrElse(0),
                    query.offset.getOrElse(0))
                    .applyLimitAndOffset(servers))
        }

        return RestUtils.okJsonList(
                getServersListAsJson(jsonList),
                query.offset.getOrElse(0),
                query.limit.getOrElse(0),
                servers.size.toLong())
    }

    /**
     * Transform each list item to JValue representation.
     * @param servers list with servers JSONObjects
     * @return servers list
     */
    private fun getServersListAsJson(servers: List<JSONObject>): List<JValue> {
        val jsonServers = ArrayList<JValue>()
        for (server in servers) {
            val vOnline = server[KEY_ONLINE] as Boolean
            val vMaintenance = server[KEY_MAINTENANCE] as Boolean
            val vHostname = server[KEY_HOSTNAME] as String
            val vCores = server[KEY_CORES] as Int
            val vRunning = server[KEY_RUNNING] as Int
            val vQueued = server[KEY_QUEUED] as Int
            val vCompleted = server[KEY_COMPLETED] as Long
            val vMeanRunTime = server[KEY_MEAN_RUN_TIME] as Long
            val vMeanQueueTime = server[KEY_MEAN_QUEUE_TIME] as Long

            jsonServers.add(obj(f(KEY_ONLINE, v(vOnline)),
                    f(KEY_MAINTENANCE, v(vMaintenance)),
                    f(KEY_HOSTNAME, v(vHostname)),
                    f(KEY_CORES, v(vCores)),
                    f(KEY_RUNNING, v(vRunning)),
                    f(KEY_QUEUED, v(vQueued)),
                    f(KEY_COMPLETED, v(vCompleted)),
                    f(KEY_MEAN_RUN_TIME, v(vMeanRunTime)),
                    f(KEY_MEAN_QUEUE_TIME, v(vMeanQueueTime))))
        }
        return jsonServers
    }

    companion object {

        // List of property keys for the JSON job object
        private val KEY_ONLINE = "online"
        private val KEY_MAINTENANCE = "maintenance"
        private val KEY_HOSTNAME = "hostname"
        private val KEY_CORES = "cores"
        private val KEY_RUNNING = "running"
        private val KEY_COMPLETED = "completed"
        private val KEY_QUEUED = "queued"
        private val KEY_MEAN_RUN_TIME = "meanRunTime"
        private val KEY_MEAN_QUEUE_TIME = "meanQueueTime"

        private val logger = LoggerFactory.getLogger(ServerEndpoint::class.java)

        val UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build()
        val NOT_FOUND = Response.status(Response.Status.NOT_FOUND).build()
        val SERVER_ERROR = Response.serverError().build()
    }
}
