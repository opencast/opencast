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
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.index.service.resources.list.query.ServicesListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceState
import org.opencastproject.serviceregistry.api.ServiceStatistics
import org.opencastproject.util.SmartIterator
import org.opencastproject.util.data.Option
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons

import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONAware
import org.json.simple.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.concurrent.TimeUnit

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "ServicesProxyService", title = "UI Services", abstractText = "This service provides the services data for the UI.", notes = ["These Endpoints deliver informations about the services required for the UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class ServicesEndpoint {
    private var serviceRegistry: ServiceRegistry? = null


    @GET
    @Path("services.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(description = "Returns the list of services", name = "services", restParameters = [RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = RestParameter.Type.INTEGER), RestParameter(name = "offset", description = "The offset", isRequired = false, type = RestParameter.Type.INTEGER), RestParameter(name = "filter", description = "Filter results by name, host, actions, status or free text query", isRequired = false, type = STRING), RestParameter(name = "sort", description = "The sort order.  May include any "
            + "of the following: host, name, running, queued, completed,  meanRunTime, meanQueueTime, "
            + "status. The sort suffix must be :asc for ascending sort order and :desc for descending.", isRequired = false, type = STRING)], reponses = [RestResponse(description = "Returns the list of services from Opencast", responseCode = HttpServletResponse.SC_OK)], returnDescription = "The list of services")
    @Throws(Exception::class)
    fun getServices(@QueryParam("limit") limit: Int, @QueryParam("offset") offset: Int,
                    @QueryParam("filter") filter: String, @QueryParam("sort") sort: String): Response {

        val sortOpt = Option.option(StringUtils.trimToNull(sort))
        val query = ServicesListQuery()
        EndpointUtil.addRequestFiltersToQuery(filter, query)

        var fName: String? = null
        if (query.name.isSome)
            fName = StringUtils.trimToNull(query.name.get())
        var fHostname: String? = null
        if (query.hostname.isSome)
            fHostname = StringUtils.trimToNull(query.hostname.get())
        var fStatus: String? = null
        if (query.status.isSome)
            fStatus = StringUtils.trimToNull(query.status.get())
        var fFreeText: String? = null
        if (query.freeText.isSome)
            fFreeText = StringUtils.trimToNull(query.freeText.get())

        val services = ArrayList<Service>()
        for (stats in serviceRegistry!!.serviceStatistics) {
            val service = Service(stats)
            if (fName != null && !StringUtils.equalsIgnoreCase(service.name, fName))
                continue

            if (fHostname != null && !StringUtils.equalsIgnoreCase(service.host, fHostname))
                continue

            if (fStatus != null && !StringUtils.equalsIgnoreCase(service.status.toString(), fStatus))
                continue

            if (query.actions.isSome) {
                val serviceState = service.status

                if (query.actions.get()) {
                    if (ServiceState.NORMAL === serviceState)
                        continue
                } else {
                    if (ServiceState.NORMAL !== serviceState)
                        continue
                }
            }

            if (fFreeText != null && !StringUtils.containsIgnoreCase(service.name, fFreeText)
                    && !StringUtils.containsIgnoreCase(service.host, fFreeText)
                    && !StringUtils.containsIgnoreCase(service.status.toString(), fFreeText))
                continue

            services.add(service)
        }
        val total = services.size

        if (sortOpt.isSome) {
            val sortCriteria = RestUtils.parseSortQueryParameter(sortOpt.get())
            if (!sortCriteria.isEmpty()) {
                try {
                    val sortCriterion = sortCriteria.iterator().next()
                    Collections.sort(services, ServiceStatisticsComparator(
                            sortCriterion.fieldName,
                            sortCriterion.order == SearchQuery.Order.Ascending))
                } catch (ex: Exception) {
                    logger.warn("Failed to sort services collection.", ex)
                }

            }
        }

        val jsonList = ArrayList<JValue>()
        for (s in SmartIterator<Service>(limit, offset).applyLimitAndOffset(services)) {
            jsonList.add(s.toJSON())
        }
        return RestUtils.okJsonList(jsonList, offset, limit, total.toLong())
    }

    /**
     * Service UI model. Wrapper class for a `ServiceStatistics` class.
     */
    internal inner class Service
    /** Constructor, set `ServiceStatistics` instance to a final private property.  */
    (
            /** Wrapped `ServiceStatistics` instance.  */
            private val serviceStatistics: ServiceStatistics) : JSONAware {

        /**
         * Returns completed jobs count.
         * @return completed jobs count
         */
        val completedJobs: Int
            get() = serviceStatistics.finishedJobs

        /**
         * Returns service host name.
         * @return service host name
         */
        val host: String
            get() = serviceStatistics.serviceRegistration.host

        /**
         * Returns service mean queue time in seconds.
         * @return service mean queue time in seconds
         */
        val meanQueueTime: Long
            get() = TimeUnit.MILLISECONDS.toSeconds(serviceStatistics.meanQueueTime)

        /**
         * Returns service mean run time in seconds.
         * @return service mean run time in seconds
         */
        val meanRunTime: Long
            get() = TimeUnit.MILLISECONDS.toSeconds(serviceStatistics.meanRunTime)

        /**
         * Returns service name.
         * @return service name
         */
        val name: String
            get() = serviceStatistics.serviceRegistration.serviceType

        /**
         * Returns queued jobs count.
         * @return queued jobs count
         */
        val queuedJobs: Int
            get() = serviceStatistics.queuedJobs

        /**
         * Returns running jobs count.
         * @return running jobs count
         */
        val runningJobs: Int
            get() = serviceStatistics.runningJobs

        /**
         * Returns service status.
         * @return service status
         */
        val status: ServiceState
            get() = serviceStatistics.serviceRegistration.serviceState

        /**
         * Returns a map of all service fields.
         * @return a map of all service fields
         */
        fun toMap(): Map<String, String> {
            val serviceMap = HashMap<String, String>()
            serviceMap[COMPLETED_NAME] = Integer.toString(completedJobs)
            serviceMap[HOST_NAME] = host
            serviceMap[MEAN_QUEUE_TIME_NAME] = java.lang.Long.toString(meanQueueTime)
            serviceMap[MEAN_RUN_TIME_NAME] = java.lang.Long.toString(meanRunTime)
            serviceMap[NAME_NAME] = name
            serviceMap[QUEUED_NAME] = Integer.toString(queuedJobs)
            serviceMap[RUNNING_NAME] = Integer.toString(runningJobs)
            serviceMap[STATUS_NAME] = status.name
            return serviceMap
        }

        /**
         * Returns a json representation of a service as `String`.
         * @return a json representation of a service as `String`
         */
        override fun toJSONString(): String {
            return JSONObject.toJSONString(toMap())
        }

        /**
         * Returns a json representation of a service as `JValue`.
         * @return a json representation of a service as `JValue`
         */
        fun toJSON(): JValue {
            return obj(f(COMPLETED_NAME, v(completedJobs)), f(HOST_NAME, v(host, Jsons.BLANK)),
                    f(MEAN_QUEUE_TIME_NAME, v(meanQueueTime)), f(MEAN_RUN_TIME_NAME, v(meanRunTime)),
                    f(NAME_NAME, v(name, Jsons.BLANK)), f(QUEUED_NAME, v(queuedJobs)),
                    f(RUNNING_NAME, v(runningJobs)),
                    f(STATUS_NAME, v(SERVICE_STATUS_TRANSLATION_PREFIX + status.name, Jsons.BLANK)))
        }

        companion object {
            /** Completed model field name.  */
            val COMPLETED_NAME = "completed"
            /** Host model field name.  */
            val HOST_NAME = "hostname"
            /** MeanQueueTime model field name.  */
            val MEAN_QUEUE_TIME_NAME = "meanQueueTime"
            /** MeanRunTime model field name.  */
            val MEAN_RUN_TIME_NAME = "meanRunTime"
            /** (Service-) Name model field name.  */
            val NAME_NAME = "name"
            /** Queued model field name.  */
            val QUEUED_NAME = "queued"
            /** Running model field name.  */
            val RUNNING_NAME = "running"
            /** Status model field name.  */
            val STATUS_NAME = "status"
        }
    }

    /**
     * `Service` comparator. Can compare service instances based on the given sort criterion and sort order.
     */
    internal inner class ServiceStatisticsComparator
    /** Constructor.  */
    (sortBy: String,
     /** Sort order (true if ascending, false otherwise).  */
     private val ascending: Boolean) : Comparator<Service> {

        /** Sort criterion.  */
        private val sortBy: String

        init {
            if (StringUtils.equalsIgnoreCase(Service.COMPLETED_NAME, sortBy)) {
                this.sortBy = Service.COMPLETED_NAME
            } else if (StringUtils.equalsIgnoreCase(Service.HOST_NAME, sortBy)) {
                this.sortBy = Service.HOST_NAME
            } else if (StringUtils.equalsIgnoreCase(Service.MEAN_QUEUE_TIME_NAME, sortBy)) {
                this.sortBy = Service.MEAN_QUEUE_TIME_NAME
            } else if (StringUtils.equalsIgnoreCase(Service.MEAN_RUN_TIME_NAME, sortBy)) {
                this.sortBy = Service.MEAN_RUN_TIME_NAME
            } else if (StringUtils.equalsIgnoreCase(Service.NAME_NAME, sortBy)) {
                this.sortBy = Service.NAME_NAME
            } else if (StringUtils.equalsIgnoreCase(Service.QUEUED_NAME, sortBy)) {
                this.sortBy = Service.QUEUED_NAME
            } else if (StringUtils.equalsIgnoreCase(Service.RUNNING_NAME, sortBy)) {
                this.sortBy = Service.RUNNING_NAME
            } else if (StringUtils.equalsIgnoreCase(Service.STATUS_NAME, sortBy)) {
                this.sortBy = Service.STATUS_NAME
            } else {
                throw IllegalArgumentException(String.format("Can't sort services by %s.", sortBy))
            }
        }

        /**
         * Compare two service instances.
         * @param s1 first `Service` instance to compare
         * @param s2 second `Service` instance to compare
         * @return
         */
        override fun compare(s1: Service, s2: Service): Int {
            var result = 0
            when (sortBy) {
                Service.COMPLETED_NAME -> result = s1.completedJobs - s2.completedJobs
                Service.HOST_NAME -> result = s1.host.compareTo(s2.host, ignoreCase = true)
                Service.MEAN_QUEUE_TIME_NAME -> result = (s1.meanQueueTime - s2.meanQueueTime).toInt()
                Service.MEAN_RUN_TIME_NAME -> result = (s1.meanRunTime - s2.meanRunTime).toInt()
                Service.QUEUED_NAME -> result = s1.queuedJobs - s2.queuedJobs
                Service.RUNNING_NAME -> result = s1.runningJobs - s2.runningJobs
                Service.STATUS_NAME -> result = s1.status.compareTo(s2.status)
                Service.NAME_NAME // default sorting criterium
                -> result = s1.name.compareTo(s2.name, ignoreCase = true)
                else -> result = s1.name.compareTo(s2.name, ignoreCase = true)
            }
            return if (ascending) result else 0 - result
        }
    }

    /** OSGI activate method.  */
    fun activate() {
        logger.info("ServicesEndpoint is activated!")
    }

    /**
     * @param serviceRegistry
     * the serviceRegistry to set
     */
    fun setServiceRegistry(serviceRegistry: ServiceRegistry) {
        this.serviceRegistry = serviceRegistry
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServicesEndpoint::class.java)

        private val SERVICE_STATUS_TRANSLATION_PREFIX = "SYSTEMS.SERVICES.STATUS."
    }
}
