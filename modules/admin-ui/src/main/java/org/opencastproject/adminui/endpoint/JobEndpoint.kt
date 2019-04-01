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

import com.entwinemedia.fn.Stream.`$`
import com.entwinemedia.fn.data.json.Jsons.arr
import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import org.opencastproject.index.service.util.RestUtils.stream
import org.opencastproject.util.DateTimeSupport.toUTC
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.adminui.exception.JobEndpointException
import org.opencastproject.index.service.resources.list.query.JobsListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.job.api.Incident
import org.opencastproject.job.api.IncidentTree
import org.opencastproject.job.api.Job
import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.security.api.User
import org.opencastproject.serviceregistry.api.IncidentL10n
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.IncidentServiceException
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.RestUtil
import org.opencastproject.util.SmartIterator
import org.opencastproject.util.SolrUtils
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService
import org.opencastproject.workflow.api.WorkflowDatabaseException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowQuery
import org.opencastproject.workflow.api.WorkflowQuery.Sort
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workflow.api.WorkflowSet

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JObject
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons
import com.entwinemedia.fn.data.json.SimpleSerializer

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.ParseException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.Locale

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "JobProxyService", title = "UI Jobs", abstractText = "This service provides the job data for the UI.", notes = ["These Endpoints deliver informations about the job required for the UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class JobEndpoint {

    private var workflowService: WorkflowService? = null
    private var serviceRegistry: ServiceRegistry? = null
    private var incidentService: IncidentService? = null

    private val errorDetailToJson = object : Fn<Tuple<String, String>, JObject>() {
        override fun apply(detail: Tuple<String, String>): JObject {
            return obj(f("name", v(detail.a, Jsons.BLANK)), f("value", v(detail.b, Jsons.BLANK)))
        }
    }

    private val removeWorkflowJobs = object : Fn<Job, Boolean>() {
        override fun apply(job: Job): Boolean {
            return if (WorkflowService.JOB_TYPE == job.jobType && ("START_WORKFLOW" == job.operation || "START_OPERATION" == job.operation)) false else true
        }
    }

    private enum class JobSort {
        CREATOR, OPERATION, PROCESSINGHOST, STATUS, STARTED, SUBMITTED, TYPE, ID
    }

    /** OSGi callback for the workflow service.  */
    fun setWorkflowService(workflowService: WorkflowService) {
        this.workflowService = workflowService
    }

    /** OSGi callback for the service registry.  */
    fun setServiceRegistry(serviceRegistry: ServiceRegistry) {
        this.serviceRegistry = serviceRegistry
    }

    /** OSGi callback for the incident service.  */
    fun setIncidentService(incidentService: IncidentService) {
        this.incidentService = incidentService
    }

    fun activate(bundleContext: BundleContext) {
        logger.info("Activate job endpoint")
    }

    @GET
    @Path("jobs.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(description = "Returns the list of active jobs", name = "jobs", restParameters = [RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = RestParameter.Type.INTEGER), RestParameter(name = "offset", description = "The offset", isRequired = false, type = RestParameter.Type.INTEGER), RestParameter(name = "filter", description = "Filter results by hostname, status or free text query", isRequired = false, type = RestParameter.Type.STRING), RestParameter(name = "sort", description = "The sort order. May include any of the following: CREATOR, OPERATION, PROCESSINGHOST, STATUS, STARTED, SUBMITTED or TYPE. " + "The suffix must be :ASC for ascending or :DESC for descending sort order (e.g. OPERATION:DESC)", isRequired = false, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the list of active jobs from Opencast", responseCode = HttpServletResponse.SC_OK)], returnDescription = "The list of jobs as JSON")
    fun getJobs(@QueryParam("limit") limit: Int, @QueryParam("offset") offset: Int,
                @QueryParam("filter") filter: String, @QueryParam("sort") sort: String): Response {
        val query = JobsListQuery()
        EndpointUtil.addRequestFiltersToQuery(filter, query)
        query.setLimit(limit)
        query.setOffset(offset)

        var fHostname: String? = null
        if (query.hostname.isSome)
            fHostname = StringUtils.trimToNull(query.hostname.get())
        var fStatus: String? = null
        if (query.status.isSome)
            fStatus = StringUtils.trimToNull(query.status.get())
        var fFreeText: String? = null
        if (query.freeText.isSome)
            fFreeText = StringUtils.trimToNull(query.freeText.get())

        val jobs = ArrayList<Job>()
        try {
            for (job in serviceRegistry!!.activeJobs) {
                // filter workflow jobs
                if (StringUtils.equals(WorkflowService.JOB_TYPE, job.jobType) && StringUtils.equals("START_WORKFLOW", job.operation))
                    continue

                // filter by hostname
                if (fHostname != null && !StringUtils.equalsIgnoreCase(job.processingHost, fHostname))
                    continue

                // filter by status
                if (fStatus != null && !StringUtils.equalsIgnoreCase(job.status.toString(), fStatus))
                    continue

                // fitler by user free text
                if (fFreeText != null
                        && !StringUtils.equalsIgnoreCase(job.processingHost, fFreeText)
                        && !StringUtils.equalsIgnoreCase(job.jobType, fFreeText)
                        && !StringUtils.equalsIgnoreCase(job.operation, fFreeText)
                        && !StringUtils.equalsIgnoreCase(job.creator, fFreeText)
                        && !StringUtils.equalsIgnoreCase(job.status.toString(), fFreeText)
                        && !StringUtils.equalsIgnoreCase(java.lang.Long.toString(job.id), fFreeText)
                        && job.rootJobId != null && !StringUtils.equalsIgnoreCase(java.lang.Long.toString(job.rootJobId!!), fFreeText))
                    continue
                jobs.add(job)
            }
        } catch (ex: ServiceRegistryException) {
            logger.error("Failed to retrieve jobs list from service registry.", ex)
            return RestUtil.R.serverError()
        }

        var sortKey = JobSort.SUBMITTED
        var ascending = true
        if (StringUtils.isNotBlank(sort)) {
            try {
                val sortCriterion = RestUtils.parseSortQueryParameter(sort).iterator().next()
                sortKey = JobSort.valueOf(sortCriterion.fieldName.toUpperCase())
                ascending = SearchQuery.Order.Ascending == sortCriterion.order || SearchQuery.Order.None == sortCriterion.order
            } catch (ex: WebApplicationException) {
                logger.warn("Failed to parse sort criterion \"{}\", invalid format.", sort)
            } catch (ex: IllegalArgumentException) {
                logger.warn("Can not apply sort criterion \"{}\", no field with this name.", sort)
            }

        }

        val comparator = JobComparator(sortKey, ascending)
        Collections.sort(jobs, comparator)
        val json = getJobsAsJSON(SmartIterator(
                query.limit.getOrElse(0),
                query.offset.getOrElse(0))
                .applyLimitAndOffset(jobs))

        return RestUtils.okJsonList(json, offset, limit, jobs.size.toLong())
    }

    @GET
    @Path("tasks.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(description = "Returns the list of tasks", name = "tasks", restParameters = [RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = RestParameter.Type.INTEGER), RestParameter(name = "offset", description = "The offset", isRequired = false, type = RestParameter.Type.INTEGER), RestParameter(name = "status", isRequired = false, description = "Filter results by workflows' current state", type = STRING), RestParameter(name = "q", isRequired = false, description = "Filter results by free text query", type = STRING), RestParameter(name = "seriesId", isRequired = false, description = "Filter results by series identifier", type = STRING), RestParameter(name = "seriesTitle", isRequired = false, description = "Filter results by series title", type = STRING), RestParameter(name = "creator", isRequired = false, description = "Filter results by the mediapackage's creator", type = STRING), RestParameter(name = "contributor", isRequired = false, description = "Filter results by the mediapackage's contributor", type = STRING), RestParameter(name = "fromdate", isRequired = false, description = "Filter results by workflow start date.", type = STRING), RestParameter(name = "todate", isRequired = false, description = "Filter results by workflow start date.", type = STRING), RestParameter(name = "language", isRequired = false, description = "Filter results by mediapackage's language.", type = STRING), RestParameter(name = "title", isRequired = false, description = "Filter results by mediapackage's title.", type = STRING), RestParameter(name = "subject", isRequired = false, description = "Filter results by mediapackage's subject.", type = STRING), RestParameter(name = "workflow", isRequired = false, description = "Filter results by workflow definition.", type = STRING), RestParameter(name = "operation", isRequired = false, description = "Filter results by workflows' current operation.", type = STRING), RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
            + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
            + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  The suffix must be :ASC for ascending or :DESC for descending sort order (e.g. TITLE:DESC).", type = STRING)], reponses = [RestResponse(description = "Returns the list of tasks from Opencast", responseCode = HttpServletResponse.SC_OK)], returnDescription = "The list of tasks as JSON")
    @Throws(JobEndpointException::class)
    fun getTasks(@QueryParam("limit") limit: Int, @QueryParam("offset") offset: Int,
                 @QueryParam("status") states: List<String>?, @QueryParam("q") text: String,
                 @QueryParam("seriesId") seriesId: String, @QueryParam("seriesTitle") seriesTitle: String,
                 @QueryParam("creator") creator: String, @QueryParam("contributor") contributor: String,
                 @QueryParam("fromdate") fromDate: String, @QueryParam("todate") toDate: String,
                 @QueryParam("language") language: String, @QueryParam("title") title: String,
                 @QueryParam("subject") subject: String, @QueryParam("workflowdefinition") workflowDefinitionId: String,
                 @QueryParam("mp") mediapackageId: String, @QueryParam("operation") currentOperations: List<String>?,
                 @QueryParam("sort") sort: String, @Context headers: HttpHeaders): Response {
        val query = WorkflowQuery()
        query.withStartPage(offset.toLong())
        query.withCount(limit.toLong())

        // Add filters
        query.withText(text)
        query.withSeriesId(seriesId)
        query.withSeriesTitle(seriesTitle)
        query.withSubject(subject)
        query.withMediaPackage(mediapackageId)
        query.withCreator(creator)
        query.withContributor(contributor)
        try {
            query.withDateAfter(SolrUtils.parseDate(fromDate))
        } catch (e: ParseException) {
            logger.error("Not able to parse the date {}: {}", fromDate, e.message)
        }

        try {
            query.withDateBefore(SolrUtils.parseDate(toDate))
        } catch (e: ParseException) {
            logger.error("Not able to parse the date {}: {}", fromDate, e.message)
        }

        query.withLanguage(language)
        query.withTitle(title)
        query.withWorkflowDefintion(workflowDefinitionId)

        if (states != null && states.size > 0) {
            try {
                for (state in states) {
                    if (StringUtils.isBlank(state)) {
                        continue
                    } else if (state.startsWith(NEGATE_PREFIX)) {
                        query.withoutState(WorkflowState.valueOf(state.substring(1).toUpperCase()))
                    } else {
                        query.withState(WorkflowState.valueOf(state.toUpperCase()))
                    }
                }
            } catch (e: IllegalArgumentException) {
                logger.debug("Unknown workflow state.", e)
            }

        }

        if (currentOperations != null && currentOperations.size > 0) {
            for (op in currentOperations) {
                if (StringUtils.isBlank(op)) {
                    continue
                }
                if (op.startsWith(NEGATE_PREFIX)) {
                    query.withoutCurrentOperation(op.substring(1))
                } else {
                    query.withCurrentOperation(op)
                }
            }
        }

        // Sorting
        if (StringUtils.isNotBlank(sort)) {
            try {
                val sortCriterion = RestUtils.parseSortQueryParameter(sort).iterator().next()
                val sortKey = Sort.valueOf(sortCriterion.fieldName.toUpperCase())
                val ascending = SearchQuery.Order.Ascending == sortCriterion.order || SearchQuery.Order.None == sortCriterion.order

                query.withSort(sortKey, ascending)
            } catch (ex: WebApplicationException) {
                logger.warn("Failed to parse sort criterion \"{}\", invalid format.", sort)
            } catch (ex: IllegalArgumentException) {
                logger.warn("Can not apply sort criterion \"{}\", no field with this name.", sort)
            }

        }

        val json: JObject
        try {
            json = getTasksAsJSON(query)
        } catch (e: NotFoundException) {
            return NOT_FOUND
        }

        return Response.ok(stream(serializer.fn.toJson(json)), MediaType.APPLICATION_JSON_TYPE).build()
    }

    fun getJobsAsJSON(jobs: List<Job>): List<JValue> {
        val jsonList = ArrayList<JValue>()
        for (job in jobs) {
            val id = job.id
            val jobType = job.jobType
            val operation = job.operation
            val status = job.status
            val dateCreated = job.dateCreated
            var created: String? = null
            if (dateCreated != null)
                created = DateTimeSupport.toUTC(dateCreated.time)
            val dateStarted = job.dateStarted
            var started: String? = null
            if (dateStarted != null)
                started = DateTimeSupport.toUTC(dateStarted.time)
            val creator = job.creator
            val processingHost = job.processingHost

            jsonList.add(obj(f("id", v(id)),
                    f("type", v(jobType)),
                    f("operation", v(operation)),
                    f("status", v(JOB_STATUS_TRANSLATION_PREFIX + status.toString())),
                    f("submitted", v(created, Jsons.BLANK)),
                    f("started", v(started, Jsons.BLANK)),
                    f("creator", v(creator, Jsons.BLANK)),
                    f("processingHost", v(processingHost, Jsons.BLANK))))
        }

        return jsonList
    }

    /**
     * Returns the list of tasks matching the given query as JSON Object
     *
     * @param query
     * The worklfow query
     * @return The list of matching tasks as JSON Object
     * @throws JobEndpointException
     * @throws NotFoundException
     */
    @Throws(JobEndpointException::class, NotFoundException::class)
    fun getTasksAsJSON(query: WorkflowQuery): JObject {
        // Get results
        var workflowInstances: WorkflowSet? = null
        var totalWithoutFilters: Long = 0
        val jsonList = ArrayList<JValue>()

        try {
            workflowInstances = workflowService!!.getWorkflowInstances(query)
            totalWithoutFilters = workflowService!!.countWorkflowInstances()
        } catch (e: WorkflowDatabaseException) {
            throw JobEndpointException(String.format("Not able to get the list of job from the database: %s", e),
                    e.cause)
        }

        val items = workflowInstances!!.items

        for (instance in items) {
            val instanceId = instance.id
            // Retrieve submission date with the workflow instance main job
            val created: Date?
            try {
                created = serviceRegistry!!.getJob(instanceId).dateCreated
            } catch (e: ServiceRegistryException) {
                throw JobEndpointException(String.format("Error when retrieving job %s from the service registry: %s",
                        instanceId, e), e.cause)
            }

            var creatorName: String? = null
            val creator = instance.creator
            if (creator != null) {
                creatorName = creator.name
            }

            jsonList.add(obj(f("id", v(instanceId)), f("title", v(instance.title, Jsons.BLANK)),
                    f("status", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + instance.state.toString())),
                    f("submitted", v(if (created != null) DateTimeSupport.toUTC(created.time) else "", Jsons.BLANK)),
                    f("submitter", v(creatorName, Jsons.BLANK))))
        }

        return obj(f("results", arr(jsonList)), f("count", v(workflowInstances.totalCount)),
                f("offset", v(query.startPage)), f("limit", v(jsonList.size)), f("total", v(totalWithoutFilters)))
    }

    /**
     * Returns the single task with the given Id as JSON Object
     *
     * @param id
     * @return The job as JSON Object
     * @throws JobEndpointException
     * @throws NotFoundException
     */
    @Throws(JobEndpointException::class, NotFoundException::class)
    fun getTasksAsJSON(id: Long): JObject {
        val instance = getWorkflowById(id)
        // Gather user information
        val user = instance.creator
        val userInformation = ArrayList<Field>()
        if (user != null) {
            userInformation.add(f("username", v(user.username)))
            userInformation.add(f("name", v(user.name, Jsons.BLANK)))
            userInformation.add(f("email", v(user.email, Jsons.BLANK)))
        }
        // Retrieve submission date with the workflow instance main job
        val created: Date?
        var executionTime: Long = 0
        try {
            val job = serviceRegistry!!.getJob(id)
            created = job.dateCreated
            var completed: Date? = job.dateCompleted
            if (completed == null)
                completed = Date()

            executionTime = completed.time - created.time
        } catch (e: ServiceRegistryException) {
            throw JobEndpointException(
                    String.format("Error when retrieving job %s from the service registry: %s", id, e), e.cause)
        }

        val mp = instance.mediaPackage

        val fields = ArrayList<Field>()
        for (key in instance.configurationKeys) {
            fields.add(f(key, v(instance.getConfiguration(key), Jsons.BLANK)))
        }

        return obj(f("status", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + instance.state, Jsons.BLANK)),
                f("description", v(instance.description, Jsons.BLANK)), f("executionTime", v(executionTime, Jsons.BLANK)),
                f("wiid", v(instance.id, Jsons.BLANK)), f("title", v(instance.title, Jsons.BLANK)),
                f("wdid", v(instance.template, Jsons.BLANK)), f("configuration", obj(fields)),
                f("submittedAt", v(if (created != null) toUTC(created.time) else "", Jsons.BLANK)),
                f("creator", obj(userInformation)))
    }

    /**
     * Returns the list of operations for a given workflow instance
     *
     * @param jobId
     * the workflow instance id
     * @return the list of workflow operations as JSON object
     * @throws JobEndpointException
     * @throws NotFoundException
     */
    @Throws(JobEndpointException::class, NotFoundException::class)
    fun getOperationsAsJSON(jobId: Long): JValue {
        val instance = getWorkflowById(jobId)

        val operations = instance.operations
        val operationsJSON = ArrayList<JValue>()

        for (wflOp in operations) {
            val fields = ArrayList<Field>()
            for (key in wflOp.configurationKeys) {
                fields.add(f(key, v(wflOp.getConfiguration(key), Jsons.BLANK)))
            }
            operationsJSON.add(obj(f("status", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + wflOp.state, Jsons.BLANK)), f("title", v(wflOp.template, Jsons.BLANK)),
                    f("description", v(wflOp.description, Jsons.BLANK)), f("id", v(wflOp.id, Jsons.BLANK)), f("configuration", obj(fields))))
        }

        return arr(operationsJSON)
    }

    /**
     * Returns the operation with the given id from the given workflow instance
     *
     * @param jobId
     * the workflow instance id
     * @param operationPosition
     * the operation position
     * @return the operation as JSON object
     * @throws JobEndpointException
     * @throws NotFoundException
     */
    @Throws(JobEndpointException::class, NotFoundException::class)
    fun getOperationAsJSON(jobId: Long, operationPosition: Int): JObject? {
        val instance = getWorkflowById(jobId)

        val operations = instance.operations

        if (operations.size > operationPosition) {
            val wflOp = operations[operationPosition]
            return obj(f("retry_strategy", v(wflOp.retryStrategy, Jsons.BLANK)),
                    f("execution_host", v(wflOp.executionHost, Jsons.BLANK)),
                    f("failed_attempts", v(wflOp.failedAttempts)),
                    f("max_attempts", v(wflOp.maxAttempts)),
                    f("exception_handler_workflow", v(wflOp.exceptionHandlingWorkflow, Jsons.BLANK)),
                    f("fail_on_error", v(wflOp.isFailWorkflowOnException)),
                    f("description", v(wflOp.description, Jsons.BLANK)),
                    f("state", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + wflOp.state, Jsons.BLANK)),
                    f("job", v(wflOp.id, Jsons.BLANK)),
                    f("name", v(wflOp.template, Jsons.BLANK)),
                    f("time_in_queue", v(wflOp.timeInQueue, v(0))),
                    f("started", if (wflOp.dateStarted != null) v(toUTC(wflOp.dateStarted.time)) else Jsons.BLANK),
                    f("completed", if (wflOp.dateCompleted != null) v(toUTC(wflOp.dateCompleted.time)) else Jsons.BLANK)
            )
        }

        return null
    }

    /**
     * Returns the list of incidents for a given workflow instance
     *
     * @param jobId
     * the workflow instance id
     * @param locale
     * the language in which title and description shall be returned
     * @param cascade
     * if true, return the incidents of the given job and those of of its descendants
     * @return the list incidents as JSON array
     * @throws JobEndpointException
     * @throws NotFoundException
     */
    @Throws(JobEndpointException::class, NotFoundException::class)
    fun getIncidentsAsJSON(jobId: Long, locale: Locale, cascade: Boolean): JValue {
        val incidents: List<Incident>
        try {
            val it = incidentService!!.getIncidentsOfJob(jobId, cascade)
            incidents = if (cascade) flatten(it) else it.incidents
        } catch (e: IncidentServiceException) {
            throw JobEndpointException(String.format(
                    "Not able to get the incidents for the job %d from the incident service : %s", jobId, e), e.cause)
        }

        val json = `$`(incidents).map(object : Fn<Incident, JValue>() {
            override fun apply(i: Incident): JValue {
                return obj(f("id", v(i.id)), f("severity", v(i.severity, Jsons.BLANK)),
                        f("timestamp", v(toUTC(i.timestamp.time), Jsons.BLANK))).merge(
                        localizeIncident(i, locale))
            }
        })
        return arr(json)
    }

    /**
     * Flatten a tree of incidents.
     *
     * @return a list of incidents
     */
    private fun flatten(incidentsTree: IncidentTree): List<Incident> {
        val incidents = ArrayList<Incident>()
        incidents.addAll(incidentsTree.incidents)
        for (descendantTree in incidentsTree.descendants) {
            incidents.addAll(flatten(descendantTree))
        }
        return incidents
    }

    /**
     * Return localized title and description of an incident as JSON.
     *
     * @param incident
     * the incident to localize
     * @param locale
     * the locale to be used to create title and description
     * @return JSON object
     */
    private fun localizeIncident(incident: Incident, locale: Locale): JObject {
        try {
            val loc = incidentService!!.getLocalization(incident.id, locale)
            return obj(f("title", v(loc.title, Jsons.BLANK)), f("description", v(loc.description, Jsons.BLANK)))
        } catch (e: Exception) {
            return obj(f("title", v("")), f("description", v("")))
        }

    }

    /**
     * Returns the workflow by the given identifier. This also returns STOPPED workflows, which is the reason for not
     * using the existing [:getWorkflowById()][WorkflowService] method.
     *
     * @param id
     * the workflow identifier
     * @return the workflow instance
     * @throws NotFoundException
     * it the workflow was not found
     * @throws JobEndpointException
     * if there was an issue reading the workflow from the database
     */
    @Throws(NotFoundException::class, JobEndpointException::class)
    private fun getWorkflowById(id: Long): WorkflowInstance {
        try {
            val workflowInstances = workflowService!!
                    .getWorkflowInstances(WorkflowQuery().withId(java.lang.Long.toString(id)))
            if (workflowInstances.items.size == 0)
                throw NotFoundException()

            return workflowInstances.items[0]
        } catch (e: WorkflowDatabaseException) {
            throw JobEndpointException(String.format("Not able to get the list of job from the database: %s", e),
                    e.cause)
        }

    }

    /**
     * Return an incident serialized as JSON.
     *
     * @param id
     * incident id
     * @param locale
     * the locale to be used to create title and description
     * @return JSON object
     */
    @Throws(JobEndpointException::class, NotFoundException::class)
    fun getIncidentAsJSON(id: Long, locale: Locale): JValue {
        val incident: Incident
        try {
            incident = incidentService!!.getIncident(id)
        } catch (e: IncidentServiceException) {
            throw JobEndpointException(String.format("Not able to get the incident %d: %s", id, e), e.cause)
        }

        return obj(f("id", v(incident.id, Jsons.BLANK)), f("job_id", v(incident.jobId, Jsons.BLANK)),
                f("severity", v(incident.severity, Jsons.BLANK)),
                f("timestamp", v(toUTC(incident.timestamp.time), Jsons.BLANK)),
                f("processing_host", v(incident.processingHost, Jsons.BLANK)), f("service_type", v(incident.serviceType, Jsons.BLANK)),
                f("technical_details", v(incident.descriptionParameters, Jsons.BLANK)),
                f("details", arr(`$`(incident.details).map(errorDetailToJson))))
                .merge(localizeIncident(incident, locale))
    }

    private inner class JobComparator internal constructor(private val sortType: JobSort, private val ascending: Boolean) : Comparator<Job> {

        override fun compare(job1: Job, job2: Job): Int {
            var result = 0
            var value1: Any? = null
            var value2: Any? = null
            when (sortType) {
                JobEndpoint.JobSort.CREATOR -> {
                    value1 = job1.creator
                    value2 = job2.creator
                }
                JobEndpoint.JobSort.OPERATION -> {
                    value1 = job1.operation
                    value2 = job2.operation
                }
                JobEndpoint.JobSort.PROCESSINGHOST -> {
                    value1 = job1.processingHost
                    value2 = job2.processingHost
                }
                JobEndpoint.JobSort.STARTED -> {
                    value1 = job1.dateStarted
                    value2 = job2.dateStarted
                }
                JobEndpoint.JobSort.STATUS -> {
                    value1 = job1.status
                    value2 = job2.status
                }
                JobEndpoint.JobSort.SUBMITTED -> {
                    value1 = job1.dateCreated
                    value2 = job2.dateCreated
                }
                JobEndpoint.JobSort.TYPE -> {
                    value1 = job1.jobType
                    value2 = job2.jobType
                }
                JobEndpoint.JobSort.ID -> {
                    value1 = job1.id
                    value2 = job2.id
                }
            }

            if (value1 == null) {
                return if (value2 == null) 0 else 1
            }
            if (value2 == null) {
                return -1
            }
            try {
                result = (value1 as Comparable<*>).compareTo(value2)
            } catch (ex: ClassCastException) {
                logger.debug("Can not compare \"{}\" with \"{}\": {}",
                        value1, value2, ex)
            }

            return if (ascending) result else -1 * result
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(JobEndpoint::class.java)
        private val serializer = SimpleSerializer()

        val UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build()
        val NOT_FOUND = Response.status(Response.Status.NOT_FOUND).build()
        val SERVER_ERROR = Response.serverError().build()

        private val NEGATE_PREFIX = "-"
        private val WORKFLOW_STATUS_TRANSLATION_PREFIX = "EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS."
        private val JOB_STATUS_TRANSLATION_PREFIX = "SYSTEMS.JOBS.STATUS."
    }
}
