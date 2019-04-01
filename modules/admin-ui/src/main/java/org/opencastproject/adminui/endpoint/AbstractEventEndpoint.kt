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
import com.entwinemedia.fn.data.Opt.nul
import com.entwinemedia.fn.data.Opt.some
import com.entwinemedia.fn.data.json.Jsons.BLANK
import com.entwinemedia.fn.data.json.Jsons.NULL
import com.entwinemedia.fn.data.json.Jsons.arr
import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import java.lang.String.format
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_NO_CONTENT
import javax.servlet.http.HttpServletResponse.SC_OK
import javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import org.apache.commons.lang3.StringUtils.trimToNull
import org.opencastproject.index.service.util.RestUtils.conflictJson
import org.opencastproject.index.service.util.RestUtils.notFound
import org.opencastproject.index.service.util.RestUtils.notFoundJson
import org.opencastproject.index.service.util.RestUtils.okJson
import org.opencastproject.index.service.util.RestUtils.okJsonList
import org.opencastproject.index.service.util.RestUtils.serverErrorJson
import org.opencastproject.util.DateTimeSupport.toUTC
import org.opencastproject.util.RestUtil.R.badRequest
import org.opencastproject.util.RestUtil.R.conflict
import org.opencastproject.util.RestUtil.R.forbidden
import org.opencastproject.util.RestUtil.R.noContent
import org.opencastproject.util.RestUtil.R.notFound
import org.opencastproject.util.RestUtil.R.ok
import org.opencastproject.util.RestUtil.R.serverError
import org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING
import org.opencastproject.util.doc.rest.RestParameter.Type.TEXT

import org.opencastproject.adminui.exception.JobEndpointException
import org.opencastproject.adminui.impl.AdminUIConfiguration
import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.adminui.util.BulkUpdateUtil
import org.opencastproject.adminui.util.QueryPreprocessor
import org.opencastproject.authorization.xacml.manager.api.AclService
import org.opencastproject.authorization.xacml.manager.api.AclServiceException
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl
import org.opencastproject.capture.CaptureParameters
import org.opencastproject.capture.admin.api.Agent
import org.opencastproject.capture.admin.api.CaptureAgentStateService
import org.opencastproject.event.comment.EventComment
import org.opencastproject.event.comment.EventCommentException
import org.opencastproject.event.comment.EventCommentReply
import org.opencastproject.event.comment.EventCommentService
import org.opencastproject.index.service.api.IndexService
import org.opencastproject.index.service.api.IndexService.Source
import org.opencastproject.index.service.catalog.adapter.MetadataList
import org.opencastproject.index.service.catalog.adapter.MetadataList.Locked
import org.opencastproject.index.service.exception.IndexServiceException
import org.opencastproject.index.service.impl.index.event.Event
import org.opencastproject.index.service.impl.index.event.EventIndexSchema
import org.opencastproject.index.service.impl.index.event.EventSearchQuery
import org.opencastproject.index.service.impl.index.event.EventUtils
import org.opencastproject.index.service.resources.list.provider.EventCommentsListProvider
import org.opencastproject.index.service.resources.list.provider.EventsListProvider.Comments
import org.opencastproject.index.service.resources.list.query.EventListQuery
import org.opencastproject.index.service.util.AccessInformationUtil
import org.opencastproject.index.service.util.JSONUtils
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.matterhorn.search.SearchResult
import org.opencastproject.matterhorn.search.SearchResultItem
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.AudioStream
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.track.AudioStreamImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter
import org.opencastproject.metadata.dublincore.MetadataCollection
import org.opencastproject.metadata.dublincore.MetadataField
import org.opencastproject.rest.BulkOperationResult
import org.opencastproject.rest.RestConstants
import org.opencastproject.scheduler.api.Recording
import org.opencastproject.scheduler.api.SchedulerException
import org.opencastproject.scheduler.api.SchedulerService
import org.opencastproject.scheduler.api.TechnicalMetadata
import org.opencastproject.scheduler.api.Util
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.service.UrlSigningService
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.Jsons.Val
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.RestUtil
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.data.Tuple3
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService
import org.opencastproject.workflow.api.RetryStrategy
import org.opencastproject.workflow.api.WorkflowDatabaseException
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowQuery
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workflow.api.WorkflowStateException
import org.opencastproject.workflow.api.WorkflowUtil

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JObject
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons
import com.entwinemedia.fn.data.json.Jsons.Functions

import net.fortuna.ical4j.model.property.RRule

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.codehaus.jettison.json.JSONException
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.Optional
import java.util.TimeZone
import java.util.stream.Collectors

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
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
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

/**
 * The event endpoint acts as a facade for WorkflowService and Archive providing a unified query interface and result
 * set.
 *
 *
 * This first implementation uses the [org.opencastproject.assetmanager.api.AssetManager]. In a later iteration
 * the endpoint may abstract over the concrete archive.
 */
@Path("/")
@RestService(name = "eventservice", title = "Event Service", abstractText = "Provides resources and operations related to the events", notes = ["This service offers the event CRUD Operations for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
abstract class AbstractEventEndpoint {

    abstract val workflowService: WorkflowService

    abstract val index: AdminUISearchIndex

    abstract val jobService: JobEndpoint

    abstract val seriesService: SeriesEndpoint

    abstract val aclService: AclService

    abstract val eventCommentService: EventCommentService

    abstract val securityService: SecurityService

    abstract val indexService: IndexService

    abstract val authorizationService: AuthorizationService

    abstract val schedulerService: SchedulerService

    abstract val captureAgentStateService: CaptureAgentStateService

    abstract val adminUIConfiguration: AdminUIConfiguration

    abstract val urlSigningExpireDuration: Long

    abstract val urlSigningService: UrlSigningService

    abstract val onlySeriesWithWriteAccessEventModal: Boolean?

    /** Default server URL  */
    protected var serverUrl = "http://localhost:8080"

    /** Service url  */
    protected var serviceUrl: String? = null

    /** The default workflow identifier, if one is configured  */
    protected var defaultWorkflowDefinionId: String? = null


    val catalogAdapters: Response
        @GET
        @Path("catalogAdapters")
        @Produces(MediaType.APPLICATION_JSON)
        @RestQuery(name = "getcataloguiadapters", description = "Returns the available catalog UI adapters as JSON", returnDescription = "The catalog UI adapters as JSON", reponses = [RestResponse(description = "Returns the available catalog UI adapters as JSON", responseCode = HttpServletResponse.SC_OK)])
        get() {
            val adapters = ArrayList<JValue>()
            for (adapter in indexService.eventCatalogUIAdapters) {
                val fields = ArrayList<Field>()
                fields.add(f("flavor", v(adapter.flavor.toString())))
                fields.add(f("title", v(adapter.uiTitle)))
                adapters.add(obj(fields))
            }
            return okJson(arr(adapters))
        }

    val newMetadata: Response
        @GET
        @Path("new/metadata")
        @RestQuery(name = "getNewMetadata", description = "Returns all the data related to the metadata tab in the new event modal as JSON", returnDescription = "All the data related to the event metadata tab as JSON", reponses = [RestResponse(responseCode = SC_OK, description = "Returns all the data related to the event metadata tab as JSON")])
        get() {
            val metadataList = indexService.metadataListWithAllEventCatalogUIAdapters
            val optMetadataByAdapter = metadataList
                    .getMetadataByAdapter(indexService.commonEventCatalogUIAdapter)
            if (optMetadataByAdapter.isSome) {
                val collection = optMetadataByAdapter.get()
                if (collection.outputFields.containsKey(DublinCore.PROPERTY_CREATED.localName))
                    collection.removeField(collection.outputFields[DublinCore.PROPERTY_CREATED.localName])
                if (collection.outputFields.containsKey("duration"))
                    collection.removeField(collection.outputFields["duration"])
                if (collection.outputFields.containsKey(DublinCore.PROPERTY_IDENTIFIER.localName))
                    collection.removeField(collection.outputFields[DublinCore.PROPERTY_IDENTIFIER.localName])
                if (collection.outputFields.containsKey(DublinCore.PROPERTY_SOURCE.localName))
                    collection.removeField(collection.outputFields[DublinCore.PROPERTY_SOURCE.localName])
                if (collection.outputFields.containsKey("startDate"))
                    collection.removeField(collection.outputFields["startDate"])
                if (collection.outputFields.containsKey("startTime"))
                    collection.removeField(collection.outputFields["startTime"])
                if (collection.outputFields.containsKey("location"))
                    collection.removeField(collection.outputFields["location"])

                if (collection.outputFields.containsKey(DublinCore.PROPERTY_PUBLISHER.localName)) {
                    val publisher = collection.outputFields[DublinCore.PROPERTY_PUBLISHER.localName] as MetadataField<String>
                    var users: MutableMap<String, String> = HashMap()
                    if (!publisher.collection.isNone) {
                        users = publisher.collection.get()
                    }
                    val loggedInUser = securityService.user.name
                    if (!users.containsKey(loggedInUser)) {
                        users[loggedInUser] = loggedInUser
                    }
                    publisher.setValue(loggedInUser)
                }

                val seriesAccessEventModal = seriesService.getUserSeriesByAccess(onlySeriesWithWriteAccessEventModal!!)
                val map = Opt.some<Map<String, String>>(seriesAccessEventModal)
                collection.outputFields[DublinCore.PROPERTY_IS_PART_OF.localName].setCollection(map)

                metadataList.add(indexService.commonEventCatalogUIAdapter, collection)
            }
            return okJson(metadataList.toJSON())
        }

    private val publicationToJson = object : Fn<Publication, JObject>() {
        override fun apply(publication: Publication): JObject {
            val channel = Opt.nul(EventUtils.PUBLICATION_CHANNELS[publication.channel])
            val url = if (publication.getURI() == null) "" else signUrl(publication.getURI()).toString()
            return obj(f("id", v(publication.channel)),
                    f("name", v(channel.getOr("EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM"))), f("url", v(url, NULL)))
        }
    }

    abstract fun signWithClientIP(): Boolean?

    /**
     * Activates REST service.
     *
     * @param cc
     * ComponentContext
     */
    fun activate(cc: ComponentContext?) {
        if (cc != null) {
            val ccServerUrl = cc.bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)
            if (StringUtils.isNotBlank(ccServerUrl))
                this.serverUrl = ccServerUrl

            this.serviceUrl = cc.properties.get(RestConstants.SERVICE_PATH_PROPERTY) as String

            val ccDefaultWorkflowDefinionId = StringUtils.trimToNull(cc.bundleContext.getProperty(WORKFLOW_DEFINITION_DEFAULT))

            if (StringUtils.isNotBlank(ccDefaultWorkflowDefinionId))
                this.defaultWorkflowDefinionId = ccDefaultWorkflowDefinionId
        }

    }

    /* As the list of event ids can grow large, we use a POST request to avoid problems with too large query strings */
    @POST
    @Path("workflowProperties")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "workflowProperties", description = "Returns workflow properties for the specified events", returnDescription = "The workflow properties for every event as JSON", restParameters = [RestParameter(name = "eventIds", description = "A JSON array of ids of the events", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the workflow properties for the events as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "The list of ids could not be parsed into a json list.", responseCode = HttpServletResponse.SC_BAD_REQUEST)])
    @Throws(UnauthorizedException::class)
    fun getEventWorkflowProperties(@FormParam("eventIds") eventIds: String): Response {
        if (StringUtils.isBlank(eventIds)) {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val parser = JSONParser()
        val ids: List<String>
        try {
            ids = parser.parse(eventIds) as List<String>
        } catch (e: org.json.simple.parser.ParseException) {
            logger.error("Unable to parse '{}'", eventIds, e)
            return Response.status(Response.Status.BAD_REQUEST).build()
        } catch (e: ClassCastException) {
            logger.error("Unable to cast '{}'", eventIds, e)
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val eventWithProperties = indexService.getEventWorkflowProperties(ids)
        val jsonEvents = HashMap<String, Field>()
        for ((key, value) in eventWithProperties) {
            val jsonProperties = ArrayList<Field>()
            for ((key1, value1) in value) {
                jsonProperties.add(f(key1, value1))
            }
            jsonEvents[key] = f(key, obj(jsonProperties))
        }
        return okJson(obj(jsonEvents))
    }

    @GET
    @Path("{eventId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getevent", description = "Returns the event by the given id as JSON", returnDescription = "The event as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the event as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getEventResponse(@PathParam("eventId") id: String): Response {
        for (event in indexService.getEvent(id, index)) {
            event.updatePreview(adminUIConfiguration.previewSubtype)
            return okJson(eventToJSON(event))
        }
        return notFound("Cannot find an event with id '%s'.", id)
    }

    @DELETE
    @Path("{eventId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "deleteevent", description = "Delete a single event.", returnDescription = "Ok if the event has been deleted.", pathParameters = [RestParameter(name = "eventId", isRequired = true, description = "The id of the event to delete.", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The event has been deleted."), RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, SearchIndexException::class)
    fun deleteEvent(@PathParam("eventId") id: String): Response {
        try {
            checkAgentAccessForEvent(id)
            if (!indexService.removeEvent(id))
                return Response.serverError().build()
        } catch (e: NotFoundException) {
            // If we couldn't find any trace of the event in the underlying database(s), we can get rid of it
            // entirely in the index.
            try {
                index.delete(Event.DOCUMENT_TYPE, id + securityService.organization.id)
            } catch (e1: SearchIndexException) {
                logger.error("error removing event {}: {}", id, e1)
                return Response.serverError().build()
            }

        }

        return Response.ok().build()
    }

    @POST
    @Path("deleteEvents")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "deleteevents", description = "Deletes a json list of events by their given ids e.g. [\"1dbe7255-e17d-4279-811d-a5c7ced689bf\", \"04fae22b-0717-4f59-8b72-5f824f76d529\"]", returnDescription = "Returns a JSON object containing a list of event ids that were deleted, not found or if there was a server error.", reponses = [RestResponse(description = "Events have been deleted", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "The list of ids could not be parsed into a json list.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "If the current user is not authorized to perform this action", responseCode = HttpServletResponse.SC_UNAUTHORIZED)])
    @Throws(UnauthorizedException::class, SearchIndexException::class)
    fun deleteEvents(eventIdsContent: String): Response {
        if (StringUtils.isBlank(eventIdsContent)) {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val parser = JSONParser()
        val eventIdsJsonArray: JSONArray
        try {
            eventIdsJsonArray = parser.parse(eventIdsContent) as JSONArray
        } catch (e: org.json.simple.parser.ParseException) {
            logger.error("Unable to parse '{}'", eventIdsContent, e)
            return Response.status(Response.Status.BAD_REQUEST).build()
        } catch (e: ClassCastException) {
            logger.error("Unable to cast '{}'", eventIdsContent, e)
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val result = BulkOperationResult()

        for (eventIdObject in eventIdsJsonArray) {
            val eventId = eventIdObject.toString()
            try {
                checkAgentAccessForEvent(eventId)
                if (!indexService.removeEvent(eventId)) {
                    result.addServerError(eventId)
                } else {
                    result.addOk(eventId)
                }
            } catch (e: NotFoundException) {
                result.addNotFound(eventId)
            } catch (e: UnauthorizedException) {
                result.addUnauthorized(eventId)
            }

        }
        return Response.ok(result.toJson()).build()
    }

    @GET
    @Path("{eventId}/hasSnapshots.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "hassnapshots", description = "Returns a JSON object containing a boolean indicating if snapshots exist for this event", returnDescription = "A JSON object", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "A JSON object containing a property \"hasSnapshots\"", responseCode = HttpServletResponse.SC_OK)])
    @Throws(Exception::class)
    fun hasEventSnapshots(@PathParam("eventId") id: String): Response {
        return okJson(obj(f("hasSnapshots", this.indexService.hasSnapshots(id))))
    }

    @GET
    @Path("{eventId}/publications.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventpublications", description = "Returns all the data related to the publications tab in the event details modal as JSON", returnDescription = "All the data related to the event publications tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id (mediapackage id).", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event publications tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getEventPublicationsTab(@PathParam("eventId") id: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)

        // Quick actions have been temporally removed from the publications tab
        // ---------------------------------------------------------------
        // List<JValue> actions = new ArrayList<JValue>();
        // List<WorkflowDefinition> workflowsDefinitions = getWorkflowService().listAvailableWorkflowDefinitions();
        // for (WorkflowDefinition wflDef : workflowsDefinitions) {
        // if (wflDef.containsTag(WORKFLOWDEF_TAG)) {
        //
        // actions.add(obj(f("id", v(wflDef.getId())), f("title", v(Opt.nul(wflDef.getTitle()).or(""))),
        // f("description", v(Opt.nul(wflDef.getDescription()).or(""))),
        // f("configuration_panel", v(Opt.nul(wflDef.getConfigurationPanel()).or("")))));
        // }
        // }

        val event = optEvent.get()
        val pubJSON = eventPublicationsToJson(event)

        return okJson(obj(f("publications", arr(pubJSON)),
                f("start-date", v(event.recordingStartDate, Jsons.BLANK)),
                f("end-date", v(event.recordingEndDate, Jsons.BLANK))))
    }

    private fun eventPublicationsToJson(event: Event): List<JValue> {
        val pubJSON = ArrayList<JValue>()
        for (json in Stream.`$`(event.publications).filter(EventUtils.internalChannelFilter)
                .map(publicationToJson)) {
            pubJSON.add(json)
        }
        return pubJSON
    }

    @GET
    @Path("{eventId}/scheduling.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getEventSchedulingMetadata", description = "Returns all of the scheduling metadata for an event", returnDescription = "All the technical metadata related to scheduling as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id (mediapackage id).", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event scheduling tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(NotFoundException::class, UnauthorizedException::class, SearchIndexException::class)
    fun getEventScheduling(@PathParam("eventId") eventId: String): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        try {
            val technicalMetadata = schedulerService.getTechnicalMetadata(eventId)
            return okJson(technicalMetadataToJson.apply(technicalMetadata))
        } catch (e: SchedulerException) {
            logger.error("Unable to get technical metadata for event with id {}", eventId)
            throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
        }

    }

    @POST
    @Path("scheduling.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getEventsScheduling", description = "Returns all of the scheduling metadata for a list of events", returnDescription = "All the technical metadata related to scheduling as JSON", restParameters = [RestParameter(name = "eventIds", description = "An array of event IDs (mediapackage id)", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "ignoreNonScheduled", description = "Whether events that are not really scheduled events should be ignored or produce an error", isRequired = true, type = RestParameter.Type.BOOLEAN)], reponses = [RestResponse(description = "Returns all the data related to the event scheduling tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    fun getEventsScheduling(@FormParam("eventIds") eventIds: List<String>, @FormParam("ignoreNonScheduled") ignoreNonScheduled: Boolean): Response {
        val fields = ArrayList<JValue>(eventIds.size)
        for (eventId in eventIds) {
            try {
                fields.add(technicalMetadataToJson.apply(schedulerService.getTechnicalMetadata(eventId)))
            } catch (e: NotFoundException) {
                if (!ignoreNonScheduled) {
                    logger.warn("Unable to find id {}", eventId, e)
                    return notFound("Cannot find an event with id '%s'.", eventId)
                }
            } catch (e: UnauthorizedException) {
                logger.warn("Unauthorized access to event ID {}", eventId, e)
                return Response.status(Status.BAD_REQUEST).build()
            } catch (e: SchedulerException) {
                logger.warn("Scheduler exception accessing event ID {}", eventId, e)
                return Response.status(Status.BAD_REQUEST).build()
            }

        }
        return okJson(arr(fields))
    }

    @PUT
    @Path("{eventId}/scheduling")
    @RestQuery(name = "updateEventScheduling", description = "Updates the scheduling information of an event", returnDescription = "The method doesn't return any content", pathParameters = [RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING)], restParameters = [RestParameter(name = "scheduling", isRequired = true, description = "The updated scheduling (JSON object)", type = RestParameter.Type.TEXT)], reponses = [RestResponse(responseCode = SC_BAD_REQUEST, description = "The required params were missing in the request."), RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."), RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content")])
    @Throws(NotFoundException::class, UnauthorizedException::class, SearchIndexException::class, IndexServiceException::class)
    fun updateEventScheduling(@PathParam("eventId") eventId: String,
                              @FormParam("scheduling") scheduling: String): Response {
        if (StringUtils.isBlank(scheduling))
            return RestUtil.R.badRequest("Missing parameters")

        try {
            val event = getEventOrThrowNotFoundException(eventId)
            updateEventScheduling(scheduling, event)
            return Response.noContent().build()
        } catch (e: JSONException) {
            return RestUtil.R.badRequest("The scheduling object is not valid")
        } catch (e: ParseException) {
            return RestUtil.R.badRequest("The UTC dates in the scheduling object is not valid")
        } catch (e: SchedulerException) {
            logger.error("Unable to update scheduling technical metadata of event {}", eventId, e)
            throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
        } catch (e: IllegalStateException) {
            return RestUtil.R.badRequest(e.message)
        }

    }

    @Throws(NotFoundException::class, UnauthorizedException::class, SchedulerException::class, JSONException::class, ParseException::class, SearchIndexException::class, IndexServiceException::class)
    private fun updateEventScheduling(scheduling: String, event: Event) {
        val technicalMetadata = schedulerService.getTechnicalMetadata(event.identifier)
        val schedulingJson = org.codehaus.jettison.json.JSONObject(
                scheduling)
        var agentId = Opt.none<String>()
        if (schedulingJson.has(SCHEDULING_AGENT_ID_KEY)) {
            agentId = Opt.some(schedulingJson.getString(SCHEDULING_AGENT_ID_KEY))
            logger.trace("Updating agent id of event '{}' from '{}' to '{}'",
                    event.identifier, technicalMetadata.agentId, agentId)
        }

        // Check if we are allowed to re-schedule on this agent
        checkAgentAccessForAgent(technicalMetadata.agentId)
        if (agentId.isSome) {
            checkAgentAccessForAgent(agentId.get())
        }

        var start = Opt.none<Date>()
        if (schedulingJson.has(SCHEDULING_START_KEY)) {
            start = Opt.some(Date(DateTimeSupport.fromUTC(schedulingJson.getString(SCHEDULING_START_KEY))))
            logger.trace("Updating start time of event '{}' id from '{}' to '{}'",
                    event.identifier, DateTimeSupport.toUTC(technicalMetadata.startDate.time),
                    DateTimeSupport.toUTC(start.get().time))
        }

        var end = Opt.none<Date>()
        if (schedulingJson.has(SCHEDULING_END_KEY)) {
            end = Opt.some(Date(DateTimeSupport.fromUTC(schedulingJson.getString(SCHEDULING_END_KEY))))
            logger.trace("Updating end time of event '{}' id from '{}' to '{}'",
                    event.identifier, DateTimeSupport.toUTC(technicalMetadata.endDate.time),
                    DateTimeSupport.toUTC(end.get().time))
        }

        var agentConfiguration = Opt.none<Map<String, String>>()
        if (schedulingJson.has(SCHEDULING_AGENT_CONFIGURATION_KEY)) {
            agentConfiguration = Opt.some(JSONUtils.toMap(schedulingJson.getJSONObject(SCHEDULING_AGENT_CONFIGURATION_KEY)))
            logger.trace("Updating agent configuration of event '{}' id from '{}' to '{}'",
                    event.identifier, technicalMetadata.captureAgentConfiguration, agentConfiguration)
        }

        if ((start.isSome || end.isSome) && end.getOr(technicalMetadata.endDate).before(start.getOr(technicalMetadata.startDate))) {
            throw IllegalStateException("The end date is before the start date")
        }

        if (!start.isNone || !end.isNone || !agentId.isNone || !agentConfiguration.isNone) {
            schedulerService
                    .updateEvent(event.identifier, start, end, agentId, Opt.none(), Opt.none(), Opt.none(), agentConfiguration)
            // We want to keep the bibliographic meta data in sync
            updateBibliographicMetadata(event, agentId, start, end)
        }
    }

    @Throws(IndexServiceException::class, SearchIndexException::class, NotFoundException::class, UnauthorizedException::class)
    private fun updateBibliographicMetadata(event: Event, agentId: Opt<String>, start: Opt<Date>, end: Opt<Date>) {
        val metadataList = indexService.metadataListWithAllEventCatalogUIAdapters
        val optMetadataByAdapter = metadataList
                .getMetadataByAdapter(indexService.commonEventCatalogUIAdapter)
        if (optMetadataByAdapter.isSome) {
            val collection = optMetadataByAdapter.get()
            if (start.isSome && collection.outputFields.containsKey("startDate")) {
                val pattern = collection.outputFields["startDate"].getPattern()
                if (pattern.isSome) {
                    val sdf = SimpleDateFormat(pattern.get())
                    sdf.timeZone = TimeZone.getTimeZone(ZoneId.of("UTC"))
                    val type = collection.outputFields["startDate"].getType()
                    collection.updateStringField(collection.outputFields["startDate"], sdf.format(start.get()))
                    collection.outputFields["startDate"].setPattern(pattern)
                    collection.outputFields["startDate"].setType(type)
                }
            }
            if (start.isSome && end.isSome && collection.outputFields.containsKey("duration")) {
                val type = collection.outputFields["duration"].getType()
                val duration = end.get().time - start.get().time
                collection.updateStringField(collection.outputFields["duration"], duration.toString() + "")
                collection.outputFields["duration"].setType(type)
            }
            if (agentId.isSome && collection.outputFields.containsKey("location")) {
                collection.updateStringField(collection.outputFields["location"], agentId.get())
            }
            indexService.updateEventMetadata(event.identifier, metadataList, index)
        }
    }

    @Throws(NotFoundException::class, SearchIndexException::class)
    private fun getEventOrThrowNotFoundException(eventId: String): Event {
        val optEvent = indexService.getEvent(eventId, index)
        return if (optEvent.isSome) {
            optEvent.get()
        } else {
            throw NotFoundException(format("Cannot find an event with id '%s'.", eventId))
        }
    }

    @GET
    @Path("{eventId}/comments")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventcomments", description = "Returns all the data related to the comments tab in the event details modal as JSON", returnDescription = "All the data related to the event comments tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event comments tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getEventComments(@PathParam("eventId") eventId: String): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        try {
            val comments = eventCommentService.getComments(eventId)
            val commentArr = ArrayList<Val>()
            for (c in comments) {
                commentArr.add(c.toJson())
            }
            return Response.ok(org.opencastproject.util.Jsons.arr(commentArr).toJson(), MediaType.APPLICATION_JSON_TYPE)
                    .build()
        } catch (e: EventCommentException) {
            logger.error("Unable to get comments from event {}", eventId, e)
            throw WebApplicationException(e)
        }

    }

    @GET
    @Path("{eventId}/hasActiveTransaction")
    @Produces(MediaType.TEXT_PLAIN)
    @RestQuery(name = "hasactivetransaction", description = "Returns whether there is currently a transaction in progress for the given event", returnDescription = "Whether there is currently a transaction in progress for the given event", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns whether there is currently a transaction in progress for the given event", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun hasActiveTransaction(@PathParam("eventId") eventId: String): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        val json = JSONObject()

        if (WorkflowUtil.isActive(optEvent.get().workflowState)) {
            json["active"] = true
        } else {
            json["active"] = false
        }

        return Response.ok(json.toJSONString()).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{eventId}/comment/{commentId}")
    @RestQuery(name = "geteventcomment", description = "Returns the comment with the given identifier", returnDescription = "Returns the comment as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The comment as JSON."), RestResponse(responseCode = SC_NOT_FOUND, description = "No event or comment with this identifier was found.")])
    @Throws(NotFoundException::class, Exception::class)
    fun getEventComment(@PathParam("eventId") eventId: String, @PathParam("commentId") commentId: Long): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        try {
            val comment = eventCommentService.getComment(commentId)
            return Response.ok(comment.toJson().toJson()).build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Could not retrieve comment {}", commentId, e)
            throw WebApplicationException(e)
        }

    }

    @PUT
    @Path("{eventId}/comment/{commentId}")
    @RestQuery(name = "updateeventcomment", description = "Updates an event comment", returnDescription = "The updated comment as JSON.", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING)], restParameters = [RestParameter(name = "text", isRequired = false, description = "The comment text", type = TEXT), RestParameter(name = "reason", isRequired = false, description = "The comment reason", type = STRING), RestParameter(name = "resolved", isRequired = false, description = "The comment resolved status", type = RestParameter.Type.BOOLEAN)], reponses = [RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to update has not been found."), RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.")])
    @Throws(Exception::class)
    fun updateEventComment(@PathParam("eventId") eventId: String, @PathParam("commentId") commentId: Long,
                           @FormParam("text") text: String, @FormParam("reason") reason: String, @FormParam("resolved") resolved: Boolean?): Response {
        var text = text
        var reason = reason
        var resolved = resolved
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        try {
            val dto = eventCommentService.getComment(commentId)

            if (StringUtils.isNotBlank(text)) {
                text = text.trim { it <= ' ' }
            } else {
                text = dto.text
            }

            if (StringUtils.isNotBlank(reason)) {
                reason = reason.trim { it <= ' ' }
            } else {
                reason = dto.reason
            }

            if (resolved == null)
                resolved = dto.isResolvedStatus

            var updatedComment = EventComment.create(dto.id, eventId,
                    securityService.organization.id, text, dto.author, reason, resolved,
                    dto.creationDate, Date(), dto.replies)

            updatedComment = eventCommentService.updateComment(updatedComment)
            val comments = eventCommentService.getComments(eventId)
            indexService.updateCommentCatalog(optEvent.get(), comments)
            return Response.ok(updatedComment.toJson().toJson()).build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unable to update the comments catalog on event {}", eventId, e)
            throw WebApplicationException(e)
        }

    }

    @POST
    @Path("{eventId}/access")
    @RestQuery(name = "applyAclToEvent", description = "Immediate application of an ACL to an event", returnDescription = "Status code", pathParameters = [RestParameter(name = "eventId", isRequired = true, description = "The event ID", type = STRING)], restParameters = [RestParameter(name = "acl", isRequired = true, description = "The ACL to apply", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"), RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the given ACL"), RestResponse(responseCode = SC_NOT_FOUND, description = "The the event has not been found"), RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action"), RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error")])
    @Throws(NotFoundException::class, UnauthorizedException::class, SearchIndexException::class, IndexServiceException::class)
    fun applyAclToEvent(@PathParam("eventId") eventId: String, @FormParam("acl") acl: String): Response {
        val accessControlList: AccessControlList
        try {
            accessControlList = AccessControlParser.parseAcl(acl)
        } catch (e: Exception) {
            logger.warn("Unable to parse ACL '{}'", acl)
            return badRequest()
        }

        try {
            val optEvent = indexService.getEvent(eventId, index)
            if (optEvent.isNone) {
                logger.warn("Unable to find the event '{}'", eventId)
                return notFound()
            }

            val eventSource = indexService.getEventSource(optEvent.get())
            if (eventSource == Source.ARCHIVE) {
                if (aclService.applyAclToEpisode(eventId, accessControlList)) {
                    return ok()
                } else {
                    logger.warn("Unable to find the event '{}'", eventId)
                    return notFound()
                }
            } else if (eventSource == Source.WORKFLOW) {
                logger.warn("An ACL cannot be edited while an event is part of a current workflow because it might"
                        + " lead to inconsistent ACLs i.e. changed after distribution so that the old ACL is still "
                        + "being used by the distribution channel.")
                val json = JSONObject()
                json["Error"] = "Unable to edit an ACL for a current workflow."
                return conflict(json.toJSONString())
            } else {
                var mediaPackage = indexService.getEventMediapackage(optEvent.get())
                mediaPackage = authorizationService.setAcl(mediaPackage, AclScope.Episode, accessControlList).a
                // We could check agent access here if we want to forbid updating ACLs for users without access.
                schedulerService.updateEvent(eventId, Opt.none(), Opt.none(), Opt.none(),
                        Opt.none(), some(mediaPackage), Opt.none(),
                        Opt.none())
                return ok()
            }
        } catch (e: AclServiceException) {
            if (e.cause is UnauthorizedException) {
                return forbidden()
            }
            logger.error("Error applying acl '{}' to event '{}'", accessControlList, eventId, e)
            return serverError()
        } catch (e: MediaPackageException) {
            if (e.cause is UnauthorizedException) {
                return forbidden()
            }
            logger.error("Error applying acl '{}' to event '{}'", accessControlList, eventId, e)
            return serverError()
        } catch (e: SchedulerException) {
            logger.error("Error applying ACL to scheduled event {}", eventId, e)
            return serverError()
        }

    }

    @POST
    @Path("{eventId}/comment")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "createeventcomment", description = "Creates a comment related to the event given by the identifier", returnDescription = "The comment related to the event as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], restParameters = [RestParameter(name = "text", isRequired = true, description = "The comment text", type = TEXT), RestParameter(name = "resolved", isRequired = false, description = "The comment resolved status", type = RestParameter.Type.BOOLEAN), RestParameter(name = "reason", isRequired = false, description = "The comment reason", type = STRING)], reponses = [RestResponse(description = "The comment has been created.", responseCode = HttpServletResponse.SC_CREATED), RestResponse(description = "If no text ist set.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun createEventComment(@PathParam("eventId") eventId: String, @FormParam("text") text: String,
                           @FormParam("reason") reason: String, @FormParam("resolved") resolved: Boolean?): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        if (StringUtils.isBlank(text))
            return Response.status(Status.BAD_REQUEST).build()

        val author = securityService.user
        try {
            var createdComment = EventComment.create(Option.none(), eventId,
                    securityService.organization.id, text, author, reason, BooleanUtils.toBoolean(reason))
            createdComment = eventCommentService.updateComment(createdComment)
            val comments = eventCommentService.getComments(eventId)
            indexService.updateCommentCatalog(optEvent.get(), comments)
            return Response.created(getCommentUrl(eventId, createdComment.id.get()))
                    .entity(createdComment.toJson().toJson()).build()
        } catch (e: Exception) {
            logger.error("Unable to create a comment on the event {}", eventId, e)
            throw WebApplicationException(e)
        }

    }

    @POST
    @Path("{eventId}/comment/{commentId}")
    @RestQuery(name = "resolveeventcomment", description = "Resolves an event comment", returnDescription = "The resolved comment.", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING)], reponses = [RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to resolve has not been found."), RestResponse(responseCode = SC_OK, description = "The resolved comment as JSON.")])
    @Throws(Exception::class)
    fun resolveEventComment(@PathParam("eventId") eventId: String, @PathParam("commentId") commentId: Long): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        try {
            val dto = eventCommentService.getComment(commentId)
            var updatedComment = EventComment.create(dto.id, dto.eventId, dto.organization,
                    dto.text, dto.author, dto.reason, true, dto.creationDate, Date(),
                    dto.replies)

            updatedComment = eventCommentService.updateComment(updatedComment)
            val comments = eventCommentService.getComments(eventId)
            indexService.updateCommentCatalog(optEvent.get(), comments)
            return Response.ok(updatedComment.toJson().toJson()).build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Could not resolve comment {}", commentId, e)
            throw WebApplicationException(e)
        }

    }

    @DELETE
    @Path("{eventId}/comment/{commentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "deleteeventcomment", description = "Deletes a event related comment by its identifier", returnDescription = "No content", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "commentId", description = "The comment id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "The event related comment has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT), RestResponse(description = "No event or comment with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun deleteEventComment(@PathParam("eventId") eventId: String, @PathParam("commentId") commentId: Long): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        try {
            eventCommentService.deleteComment(commentId)
            val comments = eventCommentService.getComments(eventId)
            indexService.updateCommentCatalog(optEvent.get(), comments)
            return Response.noContent().build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unable to delete comment {} on event {}", commentId, eventId, e)
            throw WebApplicationException(e)
        }

    }

    @DELETE
    @Path("{eventId}/comment/{commentId}/{replyId}")
    @RestQuery(name = "deleteeventreply", description = "Delete an event comment reply", returnDescription = "The updated comment as JSON.", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING), RestParameter(name = "replyId", isRequired = true, description = "The comment reply identifier", type = STRING)], reponses = [RestResponse(responseCode = SC_NOT_FOUND, description = "No event comment or reply with this identifier was found."), RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.")])
    @Throws(Exception::class)
    fun deleteEventCommentReply(@PathParam("eventId") eventId: String, @PathParam("commentId") commentId: Long,
                                @PathParam("replyId") replyId: Long): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        var comment: EventComment? = null
        var reply: EventCommentReply? = null
        try {
            comment = eventCommentService.getComment(commentId)
            for (r in comment!!.replies) {
                if (r.id.isNone || replyId != r.id.get().toLong())
                    continue
                reply = r
                break
            }

            if (reply == null)
                throw NotFoundException("Reply with id $replyId not found!")

            comment.removeReply(reply)

            val updatedComment = eventCommentService.updateComment(comment)
            val comments = eventCommentService.getComments(eventId)
            indexService.updateCommentCatalog(optEvent.get(), comments)
            return Response.ok(updatedComment.toJson().toJson()).build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not remove event comment reply {} from comment {}", replyId, commentId, e)
            throw WebApplicationException(e)
        }

    }

    @PUT
    @Path("{eventId}/comment/{commentId}/{replyId}")
    @RestQuery(name = "updateeventcommentreply", description = "Updates an event comment reply", returnDescription = "The updated comment as JSON.", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING), RestParameter(name = "replyId", isRequired = true, description = "The comment reply identifier", type = STRING)], restParameters = [RestParameter(name = "text", isRequired = true, description = "The comment reply text", type = TEXT)], reponses = [RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to extend with a reply or the reply has not been found."), RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "If no text is set."), RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.")])
    @Throws(Exception::class)
    fun updateEventCommentReply(@PathParam("eventId") eventId: String, @PathParam("commentId") commentId: Long,
                                @PathParam("replyId") replyId: Long, @FormParam("text") text: String): Response {
        if (StringUtils.isBlank(text))
            return Response.status(Status.BAD_REQUEST).build()

        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        var comment: EventComment? = null
        var reply: EventCommentReply? = null
        try {
            comment = eventCommentService.getComment(commentId)
            for (r in comment!!.replies) {
                if (r.id.isNone || replyId != r.id.get().toLong())
                    continue
                reply = r
                break
            }

            if (reply == null)
                throw NotFoundException("Reply with id $replyId not found!")

            val updatedReply = EventCommentReply.create(reply.id, text.trim { it <= ' ' }, reply.author,
                    reply.creationDate, Date())
            comment.removeReply(reply)
            comment.addReply(updatedReply)

            val updatedComment = eventCommentService.updateComment(comment)
            val comments = eventCommentService.getComments(eventId)
            indexService.updateCommentCatalog(optEvent.get(), comments)
            return Response.ok(updatedComment.toJson().toJson()).build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not update event comment reply {} from comment {}", replyId, commentId, e)
            throw WebApplicationException(e)
        }

    }

    @POST
    @Path("{eventId}/comment/{commentId}/reply")
    @RestQuery(name = "createeventcommentreply", description = "Creates an event comment reply", returnDescription = "The updated comment as JSON.", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING)], restParameters = [RestParameter(name = "text", isRequired = true, description = "The comment reply text", type = TEXT), RestParameter(name = "resolved", isRequired = false, description = "Flag defining if this reply solve or not the comment.", type = BOOLEAN)], reponses = [RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to extend with a reply has not been found."), RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "If no text is set."), RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.")])
    @Throws(Exception::class)
    fun createEventCommentReply(@PathParam("eventId") eventId: String, @PathParam("commentId") commentId: Long,
                                @FormParam("text") text: String, @FormParam("resolved") resolved: Boolean?): Response {
        if (StringUtils.isBlank(text))
            return Response.status(Status.BAD_REQUEST).build()

        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        var comment: EventComment? = null
        try {
            comment = eventCommentService.getComment(commentId)
            var updatedComment: EventComment?

            if (resolved != null && resolved) {
                // If the resolve flag is set to true, change to comment to resolved
                updatedComment = EventComment.create(comment!!.id, comment.eventId, comment.organization,
                        comment.text, comment.author, comment.reason, true, comment.creationDate,
                        Date(), comment.replies)
            } else {
                updatedComment = comment
            }

            val author = securityService.user
            val reply = EventCommentReply.create(Option.none(), text, author)
            updatedComment!!.addReply(reply)

            updatedComment = eventCommentService.updateComment(updatedComment)
            val comments = eventCommentService.getComments(eventId)
            indexService.updateCommentCatalog(optEvent.get(), comments)
            return Response.ok(updatedComment!!.toJson().toJson()).build()
        } catch (e: Exception) {
            logger.warn("Could not create event comment reply on comment {}", comment, e)
            throw WebApplicationException(e)
        }

    }

    @GET
    @Path("{eventId}/metadata.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventmetadata", description = "Returns all the data related to the metadata tab in the event details modal as JSON", returnDescription = "All the data related to the event metadata tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event metadata tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getEventMetadata(@PathParam("eventId") eventId: String): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        val metadataList = MetadataList()
        val catalogUIAdapters = indexService.eventCatalogUIAdapters
        catalogUIAdapters.remove(indexService.commonEventCatalogUIAdapter)
        val mediaPackage = indexService.getEventMediapackage(optEvent.get())
        for (catalogUIAdapter in catalogUIAdapters) {
            metadataList.add(catalogUIAdapter, catalogUIAdapter.getFields(mediaPackage))
        }

        val mc = EventUtils.getEventMetadata(optEvent.get(), indexService.commonEventCatalogUIAdapter)
        if (onlySeriesWithWriteAccessEventModal!!) {
            val series = mc.outputFields[DublinCore.PROPERTY_IS_PART_OF.localName]
            mc.removeField(series)
            val seriesAccessEventModal = seriesService.getUserSeriesByAccess(true)
            val map = Opt.some<Map<String, String>>(seriesAccessEventModal)
            val newSeries = MetadataField(series)
            newSeries.collection = map
            newSeries.setValue(optEvent.get().seriesId)
            mc.addField(newSeries)
        }
        metadataList.add(indexService.commonEventCatalogUIAdapter, mc)

        val wfState = optEvent.get().workflowState
        if (wfState != null && WorkflowUtil.isActive(WorkflowInstance.WorkflowState.valueOf(wfState)))
            metadataList.setLocked(Locked.WORKFLOW_RUNNING)

        return okJson(metadataList.toJSON())
    }

    @PUT
    @Path("bulk/update")
    @RestQuery(name = "bulkupdate", description = "Update all of the given events at once", restParameters = [RestParameter(name = "update", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of groups with events and fields to update.")], reponses = [RestResponse(description = "All events have been updated successfully.", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Could not parse update instructions.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "Field updating metadata or scheduling information. Some events may have been updated. Details are available in the response body.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR), RestResponse(description = "The events in the response body were not found. No events were updated.", responseCode = HttpServletResponse.SC_NOT_FOUND)], returnDescription = "In case of success, no content is returned. In case of errors while updating the metadata or scheduling information, the errors are returned. In case events were not found, their ids are returned")
    fun bulkUpdate(@FormParam("update") updateJson: String): Response {

        val instructions: BulkUpdateUtil.BulkUpdateInstructions
        try {
            instructions = BulkUpdateUtil.BulkUpdateInstructions(updateJson)
        } catch (e: IllegalArgumentException) {
            return badRequest("Cannot parse bulk update instructions")
        }

        val metadataUpdateFailures = HashMap<String, String>()
        val schedulingUpdateFailures = HashMap<String, String>()

        for (groupInstructions in instructions.groups) {
            // Get all the events to edit
            val events = groupInstructions.eventIds.stream()
                    .collect<Map<String, Optional<Event>>, Any>(Collectors.toMap({ id -> id }, { id -> BulkUpdateUtil.getEvent(indexService, index, id) }))

            // Check for invalid (non-existing) event ids
            val notFoundIds = events.entries.stream().filter { e -> !e.value.isPresent }.map<String>(Function<Entry<String, Optional<Event>>, String> { it.key }).collect<Set<String>, Any>(Collectors.toSet())
            if (!notFoundIds.isEmpty()) {
                return notFoundJson(JSONUtils.setToJSON(notFoundIds))
            }


            events.values.forEach { e ->
                e.ifPresent { event ->

                    var metadata: JSONObject? = null

                    // Update the scheduling information
                    try {
                        if (groupInstructions.scheduling != null) {
                            // Since we only have the start/end time, we have to add the correct date(s) for this event.
                            val scheduling = BulkUpdateUtil.addSchedulingDates(event, groupInstructions.scheduling)
                            updateEventScheduling(scheduling.toJSONString(), event)
                            // We have to update the non-technical metadata as well to keep them in sync with the technical ones.
                            metadata = BulkUpdateUtil.toNonTechnicalMetadataJson(scheduling)
                        }
                    } catch (exception: Exception) {
                        schedulingUpdateFailures[event.identifier] = exception.message
                    }

                    // Update the event metadata
                    try {
                        if (groupInstructions.metadata != null || metadata != null) {
                            metadata = BulkUpdateUtil.mergeMetadataFields(metadata, groupInstructions.metadata)
                            indexService.updateAllEventMetadata(event.identifier, JSONArray.toJSONString(listOf(metadata)), index)
                        }
                    } catch (exception: Exception) {
                        metadataUpdateFailures[event.identifier] = exception.message
                    }
                }
            }
        }

        // Check if there were any errors updating the metadata or scheduling information
        return if (!metadataUpdateFailures.isEmpty() || !schedulingUpdateFailures.isEmpty()) {
            serverErrorJson(obj(
                    f("metadataFailures", JSONUtils.mapToJSON(metadataUpdateFailures)),
                    f("schedulingFailures", JSONUtils.mapToJSON(schedulingUpdateFailures))
            ))
        } else ok()
    }

    @POST
    @Path("bulk/conflicts")
    @RestQuery(name = "getBulkConflicts", description = "Checks if the current bulk update scheduling settings are in a conflict with another event", returnDescription = "Returns NO CONTENT if no event are in conflict within specified period or list of conflicting recordings in JSON", restParameters = [RestParameter(name = "update", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of events and fields to update.")], reponses = [RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"), RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "The events in the response body were not found. No events were updated."), RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "There is a conflict"), RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters")])
    @Throws(NotFoundException::class)
    fun getBulkConflicts(@FormParam("update") updateJson: String): Response {
        val instructions: BulkUpdateUtil.BulkUpdateInstructions
        try {
            instructions = BulkUpdateUtil.BulkUpdateInstructions(updateJson)
        } catch (e: IllegalArgumentException) {
            return badRequest("Cannot parse bulk update instructions")
        }

        val conflicts = HashMap<String, List<JValue>>()
        val eventsWithSchedulingOpt = instructions.groups.stream()
                .flatMap { group ->
                    group.eventIds.stream().map<Any> { eventId ->
                        Tuple3
                                .tuple3(eventId, BulkUpdateUtil.getEvent(indexService, index, eventId), group.scheduling)
                    }
                }
                .collect<List<Tuple3<String, Optional<Event>, JSONObject>>, Any>(Collectors.toList<Any>())
        // Check for invalid (non-existing) event ids
        val notFoundIds = eventsWithSchedulingOpt.stream().filter { e -> !e.b.isPresent }
                .map<String>(Function<Tuple3<String, Optional<Event>, JSONObject>, String> { it.getA() }).collect<Set<String>, Any>(Collectors.toSet())
        if (!notFoundIds.isEmpty()) {
            return notFoundJson(JSONUtils.setToJSON(notFoundIds))
        }
        val eventsWithScheduling = eventsWithSchedulingOpt.stream()
                .map<Any> { e -> Tuple.tuple(e.b.get(), e.c) }.collect<List<Tuple<Event, JSONObject>>, Any>(Collectors.toList<Any>())
        val changedIds = eventsWithScheduling.stream().map { e -> e.a.identifier }
                .collect<Set<String>, Any>(Collectors.toSet())
        for (eventWithGroup in eventsWithScheduling) {
            val event = eventWithGroup.a
            val groupScheduling = eventWithGroup.b
            try {
                if (groupScheduling != null) {
                    // Since we only have the start/end time, we have to add the correct date(s) for this event.
                    val scheduling = BulkUpdateUtil.addSchedulingDates(event, groupScheduling)
                    val start = Date.from(Instant.parse(scheduling[SCHEDULING_START_KEY] as String))
                    val end = Date.from(Instant.parse(scheduling[SCHEDULING_END_KEY] as String))
                    val agentId = Optional.ofNullable(scheduling[SCHEDULING_AGENT_ID_KEY] as String)
                            .orElse(event.agentId)

                    val currentConflicts = ArrayList<JValue>()

                    // Check for conflicts between the events themselves
                    eventsWithScheduling.stream()
                            .filter { otherEvent -> otherEvent.a.identifier != event.identifier }
                            .forEach { otherEvent ->
                                val otherScheduling = BulkUpdateUtil.addSchedulingDates(otherEvent.a, otherEvent.b)
                                val otherStart = Date.from(Instant.parse(otherScheduling[SCHEDULING_START_KEY] as String))
                                val otherEnd = Date.from(Instant.parse(otherScheduling[SCHEDULING_END_KEY] as String))
                                val otherAgentId = Optional.ofNullable(otherScheduling[SCHEDULING_AGENT_ID_KEY] as String)
                                        .orElse(otherEvent.a.agentId)
                                if (otherAgentId != agentId) {
                                    // different agent -> no conflict
                                    return@eventsWithScheduling.stream()
                                            .filter(otherEvent ->!otherEvent.getA().getIdentifier().equals(event.getIdentifier()))
                                    .forEach
                                }
                                if (Util.schedulingIntervalsOverlap(start, end, otherStart, otherEnd)) {
                                    // conflict
                                    currentConflicts.add(convertEventToConflictingObject(DateTimeSupport.toUTC(otherStart.time),
                                            DateTimeSupport.toUTC(otherEnd.time), otherEvent.a.title))
                                }
                            }

                    // Check for conflicts with other events from the database
                    val conflicting = schedulerService.findConflictingEvents(agentId, start, end)
                            .stream()
                            .filter { mp -> !changedIds.contains(mp.identifier.toString()) }
                            .collect<List<MediaPackage>, Any>(Collectors.toList())
                    if (!conflicting.isEmpty()) {
                        currentConflicts.addAll(convertToConflictObjects(event.identifier, conflicting))
                    }
                    conflicts[event.identifier] = currentConflicts
                }
            } catch (exception: SchedulerException) {
                throw RuntimeException(exception)
            } catch (exception: UnauthorizedException) {
                throw RuntimeException(exception)
            } catch (exception: SearchIndexException) {
                throw RuntimeException(exception)
            }

        }

        if (!conflicts.isEmpty()) {
            val responseJson = ArrayList<JValue>()
            conflicts.forEach { eventId, conflictingEvents ->
                if (!conflictingEvents.isEmpty()) {
                    responseJson.add(obj(f("eventId", eventId), f("conflicts", arr(conflictingEvents))))
                }
            }
            if (!responseJson.isEmpty()) {
                return conflictJson(arr(responseJson))
            }
        }

        return noContent()
    }

    @PUT
    @Path("{eventId}/metadata")
    @RestQuery(name = "updateeventmetadata", description = "Update the passed metadata for the event with the given Id", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], restParameters = [RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of metadata to update")], reponses = [RestResponse(description = "The metadata have been updated.", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Could not parse metadata.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)], returnDescription = "No content is returned.")
    @Throws(Exception::class)
    fun updateEventMetadata(@PathParam("eventId") id: String, @FormParam("metadata") metadataJSON: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)

        try {
            val metadataList = indexService.updateAllEventMetadata(id, metadataJSON, index)
            return okJson(metadataList.toJSON())
        } catch (e: IllegalArgumentException) {
            return badRequest(String.format("Event %s metadata can't be updated.: %s", id, e.message))
        }

    }

    @GET
    @Path("{eventId}/asset/assets.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getAssetList", description = "Returns the number of assets from each types as JSON", returnDescription = "The number of assets from each types as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the number of assets from each types as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getAssetList(@PathParam("eventId") id: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)
        val mp = indexService.getEventMediapackage(optEvent.get())
        val attachments = mp.attachments.size
        val catalogs = mp.catalogs.size
        val media = mp.tracks.size
        val publications = mp.publications.size
        return okJson(obj(f("attachments", v(attachments)), f("catalogs", v(catalogs)), f("media", v(media)),
                f("publications", v(publications))))
    }

    @GET
    @Path("{eventId}/asset/attachment/attachments.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getAttachmentsList", description = "Returns a list of attachments from the given event as JSON", returnDescription = "The list of attachments from the given event as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns a list of attachments from the given event as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getAttachmentsList(@PathParam("eventId") id: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)
        val mp = indexService.getEventMediapackage(optEvent.get())
        return okJson(arr(getEventMediaPackageElements(mp.attachments)))
    }

    @GET
    @Path("{eventId}/asset/attachment/{id}.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getAttachment", description = "Returns the details of an attachment from the given event and attachment id as JSON", returnDescription = "The details of an attachment from the given event and attachment id as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "id", description = "The attachment id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the details of an attachment from the given event and attachment id as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event or attachment with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(NotFoundException::class, SearchIndexException::class, IndexServiceException::class)
    fun getAttachment(@PathParam("eventId") eventId: String, @PathParam("id") id: String): Response {
        val mp = getMediaPackageByEventId(eventId)

        val attachment = mp.getAttachment(id) ?: return notFound("Cannot find an attachment with id '%s'.", id)
        return okJson(attachmentToJSON(attachment))
    }

    @GET
    @Path("{eventId}/asset/catalog/catalogs.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getCatalogList", description = "Returns a list of catalogs from the given event as JSON", returnDescription = "The list of catalogs from the given event as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns a list of catalogs from the given event as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getCatalogList(@PathParam("eventId") id: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)
        val mp = indexService.getEventMediapackage(optEvent.get())
        return okJson(arr(getEventMediaPackageElements(mp.catalogs)))
    }

    @GET
    @Path("{eventId}/asset/catalog/{id}.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getCatalog", description = "Returns the details of a catalog from the given event and catalog id as JSON", returnDescription = "The details of a catalog from the given event and catalog id as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "id", description = "The catalog id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the details of a catalog from the given event and catalog id as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event or catalog with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(NotFoundException::class, SearchIndexException::class, IndexServiceException::class)
    fun getCatalog(@PathParam("eventId") eventId: String, @PathParam("id") id: String): Response {
        val mp = getMediaPackageByEventId(eventId)

        val catalog = mp.getCatalog(id) ?: return notFound("Cannot find a catalog with id '%s'.", id)
        return okJson(catalogToJSON(catalog))
    }

    @GET
    @Path("{eventId}/asset/media/media.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getMediaList", description = "Returns a list of media from the given event as JSON", returnDescription = "The list of media from the given event as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns a list of media from the given event as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getMediaList(@PathParam("eventId") id: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)
        val mp = indexService.getEventMediapackage(optEvent.get())
        return okJson(arr(getEventMediaPackageElements(mp.tracks)))
    }

    @GET
    @Path("{eventId}/asset/media/{id}.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getMedia", description = "Returns the details of a media from the given event and media id as JSON", returnDescription = "The details of a media from the given event and media id as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "id", description = "The media id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the media of a catalog from the given event and media id as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event or media with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(NotFoundException::class, SearchIndexException::class, IndexServiceException::class)
    fun getMedia(@PathParam("eventId") eventId: String, @PathParam("id") id: String): Response {
        val mp = getMediaPackageByEventId(eventId)

        val track = mp.getTrack(id) ?: return notFound("Cannot find media with id '%s'.", id)
        return okJson(trackToJSON(track))
    }

    @GET
    @Path("{eventId}/asset/publication/publications.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getPublicationList", description = "Returns a list of publications from the given event as JSON", returnDescription = "The list of publications from the given event as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns a list of publications from the given event as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getPublicationList(@PathParam("eventId") id: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)
        val mp = indexService.getEventMediapackage(optEvent.get())
        return okJson(arr(getEventPublications(mp.publications)))
    }

    @GET
    @Path("{eventId}/asset/publication/{id}.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getPublication", description = "Returns the details of a publication from the given event and publication id as JSON", returnDescription = "The details of a publication from the given event and publication id as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "id", description = "The publication id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the publication of a catalog from the given event and publication id as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event or publication with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(NotFoundException::class, SearchIndexException::class, IndexServiceException::class)
    fun getPublication(@PathParam("eventId") eventId: String, @PathParam("id") id: String): Response {
        val mp = getMediaPackageByEventId(eventId)

        var publication: Publication? = null
        for (p in mp.publications) {
            if (id == p.identifier) {
                publication = p
                break
            }
        }

        return if (publication == null) notFound("Cannot find publication with id '%s'.", id) else okJson(publicationToJSON(publication))
    }

    @GET
    @Path("{eventId}/workflows.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventworkflows", description = "Returns all the data related to the workflows tab in the event details modal as JSON", returnDescription = "All the data related to the event workflows tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event workflows tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(UnauthorizedException::class, SearchIndexException::class, JobEndpointException::class)
    fun getEventWorkflows(@PathParam("eventId") id: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)

        try {
            if (optEvent.get().isScheduledEvent && !optEvent.get().hasRecordingStarted()) {
                val fields = ArrayList<Field>()
                val workflowConfig = schedulerService.getWorkflowConfig(id)
                for ((key, value) in workflowConfig) {
                    fields.add(f(key, v(value, Jsons.BLANK)))
                }

                val agentConfiguration = schedulerService.getCaptureAgentConfiguration(id)
                return okJson(obj(f("workflowId", v(agentConfiguration[CaptureParameters.INGEST_WORKFLOW_DEFINITION], Jsons.BLANK)),
                        f("configuration", obj(fields))))
            } else {
                return okJson(jobService.getTasksAsJSON(WorkflowQuery().withMediaPackage(id)))
            }
        } catch (e: NotFoundException) {
            return notFound("Cannot find workflows for event %s", id)
        } catch (e: SchedulerException) {
            logger.error("Unable to get workflow data for event with id {}", id)
            throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
        }

    }

    @PUT
    @Path("{eventId}/workflows")
    @RestQuery(name = "updateEventWorkflow", description = "Update the workflow configuration for the scheduled event with the given id", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], restParameters = [RestParameter(name = "configuration", isRequired = true, description = "The workflow configuration as JSON", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Request executed succesfully", responseCode = HttpServletResponse.SC_NO_CONTENT), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)], returnDescription = "The method does not retrun any content.")
    @Throws(SearchIndexException::class, UnauthorizedException::class)
    fun updateEventWorkflow(@PathParam("eventId") id: String, @FormParam("configuration") configuration: String): Response {
        val optEvent = indexService.getEvent(id, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", id)

        return if (optEvent.get().isScheduledEvent && !optEvent.get().hasRecordingStarted()) {
            try {

                val configJSON: JSONObject
                try {
                    configJSON = JSONParser().parse(configuration) as JSONObject
                } catch (e: Exception) {
                    logger.warn("Unable to parse the workflow configuration {}", configuration)
                    return badRequest()
                }

                var caMetadataOpt = Opt.none<Map<String, String>>()
                var workflowConfigOpt = Opt.none<Map<String, String>>()

                val workflowId = configJSON["id"] as String
                val caMetadata = HashMap(schedulerService.getCaptureAgentConfiguration(id))
                if (workflowId != caMetadata[CaptureParameters.INGEST_WORKFLOW_DEFINITION]) {
                    caMetadata[CaptureParameters.INGEST_WORKFLOW_DEFINITION] = workflowId
                    caMetadataOpt = Opt.some(caMetadata)
                }

                val workflowConfig = HashMap(configJSON["configuration"] as JSONObject)
                val oldWorkflowConfig = HashMap(schedulerService.getWorkflowConfig(id))
                if (oldWorkflowConfig != workflowConfig)
                    workflowConfigOpt = Opt.some<Map<String, String>>(workflowConfig)

                if (caMetadataOpt.isNone && workflowConfigOpt.isNone)
                    return Response.noContent().build()

                checkAgentAccessForAgent(optEvent.get().agentId)

                schedulerService.updateEvent(id, Opt.none(), Opt.none(), Opt.none(),
                        Opt.none(), Opt.none(), workflowConfigOpt, caMetadataOpt)
                Response.noContent().build()
            } catch (e: NotFoundException) {
                notFound("Cannot find event %s in scheduler service", id)
            } catch (e: SchedulerException) {
                logger.error("Unable to update scheduling workflow data for event with id {}", id)
                throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
            }

        } else {
            badRequest(String.format("Event %s workflow can not be updated as the recording already started.", id))
        }
    }

    @GET
    @Path("{eventId}/workflows/{workflowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventworkflow", description = "Returns all the data related to the single workflow tab in the event details modal as JSON", returnDescription = "All the data related to the event singe workflow tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event single workflow tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(JobEndpointException::class, SearchIndexException::class)
    fun getEventWorkflow(@PathParam("eventId") eventId: String, @PathParam("workflowId") workflowId: String): Response {
        var workflowId = workflowId
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        val workflowInstanceId: Long
        try {
            workflowId = StringUtils.remove(workflowId, ".json")
            workflowInstanceId = java.lang.Long.parseLong(workflowId)
        } catch (e: Exception) {
            logger.warn("Unable to parse workflow id {}", workflowId)
            return RestUtil.R.badRequest()
        }

        try {
            return okJson(jobService.getTasksAsJSON(workflowInstanceId))
        } catch (e: NotFoundException) {
            return notFound("Cannot find workflow  %s", workflowId)
        }

    }

    @GET
    @Path("{eventId}/workflows/{workflowId}/operations.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventoperations", description = "Returns all the data related to the workflow/operations tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/opertations tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event workflow/operations tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(JobEndpointException::class, SearchIndexException::class)
    fun getEventOperations(@PathParam("eventId") eventId: String, @PathParam("workflowId") workflowId: String): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        val workflowInstanceId: Long
        try {
            workflowInstanceId = java.lang.Long.parseLong(workflowId)
        } catch (e: Exception) {
            logger.warn("Unable to parse workflow id {}", workflowId)
            return RestUtil.R.badRequest()
        }

        try {
            return okJson(jobService.getOperationsAsJSON(workflowInstanceId))
        } catch (e: NotFoundException) {
            return notFound("Cannot find workflow %s", workflowId)
        }

    }

    @GET
    @Path("{eventId}/workflows/{workflowId}/operations/{operationPosition}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventoperation", description = "Returns all the data related to the workflow/operation tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/opertation tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "operationPosition", description = "The operation position", isRequired = true, type = RestParameter.Type.INTEGER)], reponses = [RestResponse(description = "Returns all the data related to the event workflow/operation tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Unable to parse workflowId or operationPosition", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No operation with these identifiers was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(JobEndpointException::class, SearchIndexException::class)
    fun getEventOperation(@PathParam("eventId") eventId: String, @PathParam("workflowId") workflowId: String,
                          @PathParam("operationPosition") operationPosition: Int?): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        val workflowInstanceId: Long
        try {
            workflowInstanceId = java.lang.Long.parseLong(workflowId)
        } catch (e: Exception) {
            logger.warn("Unable to parse workflow id {}", workflowId)
            return RestUtil.R.badRequest()
        }

        try {
            return okJson(jobService.getOperationAsJSON(workflowInstanceId, operationPosition!!))
        } catch (e: NotFoundException) {
            return notFound("Cannot find workflow %s", workflowId)
        }

    }

    @GET
    @Path("{eventId}/workflows/{workflowId}/errors.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventerrors", description = "Returns all the data related to the workflow/errors tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/errors tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event workflow/errors tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(JobEndpointException::class, SearchIndexException::class)
    fun getEventErrors(@PathParam("eventId") eventId: String, @PathParam("workflowId") workflowId: String,
                       @Context req: HttpServletRequest): Response {
        // the call to #getEvent should make sure that the calling user has access rights to the workflow
        // FIXME since there is no dependency between the event and the workflow (the fetched event is
        // simply ignored) an attacker can get access by using an event he owns and a workflow ID of
        // someone else.
        for (ignore in indexService.getEvent(eventId, index)) {
            val workflowIdLong: Long
            try {
                workflowIdLong = java.lang.Long.parseLong(workflowId)
            } catch (e: Exception) {
                logger.warn("Unable to parse workflow id {}", workflowId)
                return RestUtil.R.badRequest()
            }

            try {
                return okJson(jobService.getIncidentsAsJSON(workflowIdLong, req.locale, true))
            } catch (e: NotFoundException) {
                return notFound("Cannot find the incident for the workflow %s", workflowId)
            }

        }
        return notFound("Cannot find an event with id '%s'.", eventId)
    }

    @GET
    @Path("{eventId}/workflows/{workflowId}/errors/{errorId}.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "geteventerror", description = "Returns all the data related to the workflow/error tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/error tab as JSON", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "errorId", description = "The error id", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns all the data related to the event workflow/error tab as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(JobEndpointException::class, SearchIndexException::class)
    fun getEventError(@PathParam("eventId") eventId: String, @PathParam("workflowId") workflowId: String,
                      @PathParam("errorId") errorId: String, @Context req: HttpServletRequest): Response {
        // the call to #getEvent should make sure that the calling user has access rights to the workflow
        // FIXME since there is no dependency between the event and the workflow (the fetched event is
        // simply ignored) an attacker can get access by using an event he owns and a workflow ID of
        // someone else.
        for (ignore in indexService.getEvent(eventId, index)) {
            val errorIdLong: Long
            try {
                errorIdLong = java.lang.Long.parseLong(errorId)
            } catch (e: Exception) {
                logger.warn("Unable to parse error id {}", errorId)
                return RestUtil.R.badRequest()
            }

            try {
                return okJson(jobService.getIncidentAsJSON(errorIdLong, req.locale))
            } catch (e: NotFoundException) {
                return notFound("Cannot find the incident %s", errorId)
            }

        }
        return notFound("Cannot find an event with id '%s'.", eventId)
    }

    @GET
    @Path("{eventId}/access.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getEventAccessInformation", description = "Get the access information of an event", returnDescription = "The access information", pathParameters = [RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING)], reponses = [RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."), RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."), RestResponse(responseCode = SC_OK, description = "The access information ")])
    @Throws(Exception::class)
    fun getEventAccessInformation(@PathParam("eventId") eventId: String): Response {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            return notFound("Cannot find an event with id '%s'.", eventId)

        // Add all available ACLs to the response
        val systemAclsJson = JSONArray()
        val acls = aclService.acls
        for (acl in acls) {
            systemAclsJson.add(AccessInformationUtil.serializeManagedAcl(acl))
        }

        var activeAcl = AccessControlList()
        try {
            if (optEvent.get().accessPolicy != null)
                activeAcl = AccessControlParser.parseAcl(optEvent.get().accessPolicy)
        } catch (e: Exception) {
            logger.error("Unable to parse access policy", e)
        }

        val currentAcl = AccessInformationUtil.matchAcls(acls, activeAcl)

        val episodeAccessJson = JSONObject()
        episodeAccessJson["current_acl"] = if (currentAcl.isSome) currentAcl.get().id else 0L
        episodeAccessJson["acl"] = AccessControlParser.toJsonSilent(activeAcl)
        episodeAccessJson["privileges"] = AccessInformationUtil.serializePrivilegesByRole(activeAcl)
        if (StringUtils.isNotBlank(optEvent.get().workflowState) && WorkflowUtil.isActive(WorkflowInstance.WorkflowState.valueOf(optEvent.get().workflowState)))
            episodeAccessJson["locked"] = true

        val jsonReturnObj = JSONObject()
        jsonReturnObj["episode_access"] = episodeAccessJson
        jsonReturnObj["system_acls"] = systemAclsJson

        return Response.ok(jsonReturnObj.toString()).build()
    }

    // MH-12085 Add manually uploaded assets, multipart file upload has to be a POST
    @POST
    @Path("{eventId}/assets")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RestQuery(name = "updateAssets", description = "Update or create an asset for the eventId by the given metadata as JSON and files in the body", pathParameters = [RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)], restParameters = [RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of asset metadata")], reponses = [RestResponse(description = "The asset has been added.", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Could not add asset, problem with the metadata or files.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)], returnDescription = "The workflow identifier")
    @Throws(Exception::class)
    fun updateAssets(@PathParam("eventId") eventId: String,
                     @Context request: HttpServletRequest): Response {
        try {
            val mp = getMediaPackageByEventId(eventId)
            val result = indexService.updateEventAssets(mp, request)
            return Response.status(Status.CREATED).entity(result).build()
        } catch (e: NotFoundException) {
            return notFound("Cannot find an event with id '%s'.", eventId)
        } catch (e: IllegalArgumentException) {
            return RestUtil.R.badRequest(e.message)
        } catch (e: Exception) {
            return RestUtil.R.serverError()
        }

    }

    @GET
    @Path("new/processing")
    @RestQuery(name = "getNewProcessing", description = "Returns all the data related to the processing tab in the new event modal as JSON", returnDescription = "All the data related to the event processing tab as JSON", restParameters = [RestParameter(name = "tags", isRequired = false, description = "A comma separated list of tags to filter the workflow definitions", type = RestParameter.Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "Returns all the data related to the event processing tab as JSON")])
    fun getNewProcessing(@QueryParam("tags") tagsString: String): Response {
        val tags = RestUtil.splitCommaSeparatedParam(Option.option(tagsString)).value()

        val workflows = ArrayList<JValue>()
        try {
            val workflowsDefinitions = workflowService.listAvailableWorkflowDefinitions()
            for (wflDef in workflowsDefinitions) {
                if (wflDef.containsTag(tags)) {

                    workflows.add(obj(f("id", v(wflDef.id)), f("tags", arr(*wflDef.tags)),
                            f("title", v(nul(wflDef.title).getOr(""))),
                            f("description", v(nul(wflDef.description).getOr(""))),
                            f("displayOrder", v(wflDef.displayOrder)),
                            f("configuration_panel", v(nul(wflDef.configurationPanel).getOr("")))))
                }
            }
        } catch (e: WorkflowDatabaseException) {
            logger.error("Unable to get available workflow definitions", e)
            return RestUtil.R.serverError()
        }

        val data = obj(f("workflows", arr(workflows)), f("default_workflow_id", v(defaultWorkflowDefinionId, Jsons.NULL)))

        return okJson(data)
    }

    @POST
    @Path("new/conflicts")
    @RestQuery(name = "checkNewConflicts", description = "Checks if the current scheduler parameters are in a conflict with another event", returnDescription = "Returns NO CONTENT if no event are in conflict within specified period or list of conflicting recordings in JSON", restParameters = [RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT)], reponses = [RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"), RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "There is a conflict"), RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters")])
    @Throws(NotFoundException::class)
    fun getNewConflicts(@FormParam("metadata") metadata: String): Response {
        if (StringUtils.isBlank(metadata)) {
            logger.warn("Metadata is not specified")
            return Response.status(Status.BAD_REQUEST).build()
        }

        val parser = JSONParser()
        val metadataJson: JSONObject
        try {
            metadataJson = parser.parse(metadata) as JSONObject
        } catch (e: Exception) {
            logger.warn("Unable to parse metadata {}", metadata)
            return RestUtil.R.badRequest("Unable to parse metadata")
        }

        val device: String
        val startDate: String
        val endDate: String
        try {
            device = metadataJson["device"] as String
            startDate = metadataJson["start"] as String
            endDate = metadataJson["end"] as String
        } catch (e: Exception) {
            logger.warn("Unable to parse metadata {}", metadata)
            return RestUtil.R.badRequest("Unable to parse metadata")
        }

        if (StringUtils.isBlank(device) || StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)) {
            logger.warn("Either device, start date or end date were not specified")
            return Response.status(Status.BAD_REQUEST).build()
        }

        val start: Date
        try {
            start = Date(DateTimeSupport.fromUTC(startDate))
        } catch (e: Exception) {
            logger.warn("Unable to parse start date {}", startDate)
            return RestUtil.R.badRequest("Unable to parse start date")
        }

        val end: Date
        try {
            end = Date(DateTimeSupport.fromUTC(endDate))
        } catch (e: Exception) {
            logger.warn("Unable to parse end date {}", endDate)
            return RestUtil.R.badRequest("Unable to parse end date")
        }

        val rruleString = metadataJson["rrule"] as String

        var rrule: RRule? = null
        var timeZone = TimeZone.getDefault()
        var durationString: String? = null
        if (StringUtils.isNotEmpty(rruleString)) {
            try {
                rrule = RRule(rruleString)
                rrule.validate()
            } catch (e: Exception) {
                logger.warn("Unable to parse rrule {}: {}", rruleString, e.message)
                return Response.status(Status.BAD_REQUEST).build()
            }

            durationString = metadataJson["duration"] as String
            if (StringUtils.isBlank(durationString)) {
                logger.warn("If checking recurrence, must include duration.")
                return Response.status(Status.BAD_REQUEST).build()
            }

            val agent = captureAgentStateService.getAgent(device)
            var timezone = agent.configuration.getProperty("capture.device.timezone")
            if (StringUtils.isBlank(timezone)) {
                timezone = TimeZone.getDefault().id
                logger.warn("No 'capture.device.timezone' set on agent {}. The default server timezone {} will be used.",
                        device, timezone)
            }
            timeZone = TimeZone.getTimeZone(timezone)
        }

        val eventId = metadataJson["id"] as String

        try {
            var events: List<MediaPackage>? = null
            if (StringUtils.isNotEmpty(rruleString)) {
                events = schedulerService.findConflictingEvents(device, rrule, start, end, java.lang.Long.parseLong(durationString!!),
                        timeZone)
            } else {
                events = schedulerService.findConflictingEvents(device, start, end)
            }
            if (!events!!.isEmpty()) {
                val eventsJSON = convertToConflictObjects(eventId, events)
                if (!eventsJSON.isEmpty())
                    return conflictJson(arr(eventsJSON))
            }
            return Response.noContent().build()
        } catch (e: Exception) {
            logger.error("Unable to find conflicting events for {}, {}, {}",
                    device, startDate, endDate, e)
            return RestUtil.R.serverError()
        }

    }

    @Throws(SearchIndexException::class)
    private fun convertToConflictObjects(eventId: String, events: List<MediaPackage>): List<JValue> {
        val eventsJSON = ArrayList<JValue>()
        for (event in events) {
            val eventOpt = indexService.getEvent(event.identifier.compact(), index)
            if (eventOpt.isSome) {
                val e = eventOpt.get()
                if (StringUtils.isNotEmpty(eventId) && eventId == e.identifier) {
                    continue
                }
                eventsJSON.add(convertEventToConflictingObject(e.technicalStartTime, e.technicalEndTime, e.title))
            } else {
                logger.warn("Index out of sync! Conflicting event catalog {} not found on event index!",
                        event.identifier.compact())
            }
        }
        return eventsJSON
    }

    private fun convertEventToConflictingObject(start: String, end: String, title: String): JValue {
        return obj(
                f("start", v(start)),
                f("end", v(end)),
                f("title", v(title))
        )
    }

    @POST
    @Path("/new")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RestQuery(name = "createNewEvent", description = "Creates a new event by the given metadata as JSON and the files in the body", returnDescription = "The workflow identifier", restParameters = [RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT)], reponses = [RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Event sucessfully added"), RestResponse(responseCode = SC_BAD_REQUEST, description = "If the metadata is not set or couldn't be parsed")])
    fun createNewEvent(@Context request: HttpServletRequest): Response {
        try {
            val result = indexService.createEvent(request)
            return Response.status(Status.CREATED).entity(result).build()
        } catch (e: IllegalArgumentException) {
            return RestUtil.R.badRequest(e.message)
        } catch (e: Exception) {
            return RestUtil.R.serverError()
        }

    }

    @GET
    @Path("events.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getevents", description = "Returns all the events as JSON", returnDescription = "All the events as JSON", restParameters = [RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING), RestParameter(name = "sort", description = "The order instructions used to sort the query result. Must be in the form '<field name>:(ASC|DESC)'", isRequired = false, type = STRING), RestParameter(name = "limit", description = "The maximum number of items to return per page.", isRequired = false, type = RestParameter.Type.INTEGER), RestParameter(name = "offset", description = "The page number.", isRequired = false, type = RestParameter.Type.INTEGER)], reponses = [RestResponse(description = "Returns all events as JSON", responseCode = HttpServletResponse.SC_OK)])
    fun getEvents(@QueryParam("id") id: String, @QueryParam("commentReason") reasonFilter: String,
                  @QueryParam("commentResolution") resolutionFilter: String, @QueryParam("filter") filter: String,
                  @QueryParam("sort") sort: String, @QueryParam("offset") offset: Int?, @QueryParam("limit") limit: Int?): Response {

        var optLimit = Option.option(limit)
        val optOffset = Option.option(offset)
        val optSort = Option.option(trimToNull(sort))
        val eventsList = ArrayList<JValue>()
        val query = EventSearchQuery(securityService.organization.id,
                securityService.user)

        // If the limit is set to 0, this is not taken into account
        if (optLimit.isSome && limit == 0) {
            optLimit = Option.none()
        }

        val filters = RestUtils.parseFilter(filter)
        for (name in filters.keys) {
            if (EventListQuery.FILTER_PRESENTERS_BIBLIOGRAPHIC_NAME == name)
                query.withPresenter(filters[name])
            if (EventListQuery.FILTER_PRESENTERS_TECHNICAL_NAME == name)
                query.withTechnicalPresenters(filters[name])
            if (EventListQuery.FILTER_CONTRIBUTORS_NAME == name)
                query.withContributor(filters[name])
            if (EventListQuery.FILTER_LOCATION_NAME == name)
                query.withLocation(filters[name])
            if (EventListQuery.FILTER_AGENT_NAME == name)
                query.withAgentId(filters[name])
            if (EventListQuery.FILTER_TEXT_NAME == name)
                query.withText(QueryPreprocessor.sanitize(filters[name]))
            if (EventListQuery.FILTER_SERIES_NAME == name)
                query.withSeriesId(filters[name])
            if (EventListQuery.FILTER_STATUS_NAME == name)
                query.withEventStatus(filters[name])
            if (EventListQuery.FILTER_PUBLISHER_NAME == name)
                query.withPublisher(filters[name])
            if (EventListQuery.FILTER_COMMENTS_NAME == name) {
                when (Comments.valueOf(filters[name])) {
                    EventsListProvider.Comments.NONE -> query.withComments(false)
                    EventsListProvider.Comments.OPEN -> query.withOpenComments(true)
                    EventsListProvider.Comments.RESOLVED -> {
                        query.withComments(true)
                        query.withOpenComments(false)
                    }
                    else -> {
                        logger.info("Unknown comment {}", filters[name])
                        return Response.status(SC_BAD_REQUEST).build()
                    }
                }
            }
            if (EventListQuery.FILTER_STARTDATE_NAME == name) {
                try {
                    val fromAndToCreationRange = RestUtils.getFromAndToDateRange(filters[name])
                    query.withTechnicalStartFrom(fromAndToCreationRange.a)
                    query.withTechnicalStartTo(fromAndToCreationRange.b)
                } catch (e: IllegalArgumentException) {
                    return RestUtil.R.badRequest(e.message)
                }

            }
        }

        if (optSort.isSome) {
            val sortCriteria = RestUtils.parseSortQueryParameter(optSort.get())
            for (criterion in sortCriteria) {
                when (criterion.fieldName) {
                    EventIndexSchema.TITLE -> query.sortByTitle(criterion.order)
                    EventIndexSchema.PRESENTER -> query.sortByPresenter(criterion.order)
                    EventIndexSchema.TECHNICAL_START, "technical_date" -> query.sortByTechnicalStartDate(criterion.order)
                    EventIndexSchema.TECHNICAL_END -> query.sortByTechnicalEndDate(criterion.order)
                    EventIndexSchema.PUBLICATION -> query.sortByPublicationIgnoringInternal(criterion.order)
                    EventIndexSchema.START_DATE, "date" -> query.sortByStartDate(criterion.order)
                    EventIndexSchema.END_DATE -> query.sortByEndDate(criterion.order)
                    EventIndexSchema.SERIES_NAME -> query.sortBySeriesName(criterion.order)
                    EventIndexSchema.LOCATION -> query.sortByLocation(criterion.order)
                    EventIndexSchema.EVENT_STATUS -> query.sortByEventStatus(criterion.order)
                    else -> throw WebApplicationException(Status.BAD_REQUEST)
                }
            }
        }

        // TODO: Add the comment resolution filter to the query
        var resolution: EventCommentsListProvider.RESOLUTION? = null
        if (StringUtils.isNotBlank(resolutionFilter)) {
            try {
                resolution = EventCommentsListProvider.RESOLUTION.valueOf(resolutionFilter)
            } catch (e: Exception) {
                logger.warn("Unable to parse comment resolution filter {}", resolutionFilter)
                return Response.status(Status.BAD_REQUEST).build()
            }

        }

        if (optLimit.isSome)
            query.withLimit(optLimit.get())
        if (optOffset.isSome)
            query.withOffset(offset!!)
        // TODO: Add other filters to the query

        var results: SearchResult<Event>? = null
        try {
            results = index.getByQuery(query)
        } catch (e: SearchIndexException) {
            logger.error("The admin UI Search Index was not able to get the events list:", e)
            return RestUtil.R.serverError()
        }

        // If the results list if empty, we return already a response.
        if (results!!.pageSize == 0L) {
            logger.debug("No events match the given filters.")
            return okJsonList(eventsList, nul(offset).getOr(0), nul(limit).getOr(0), 0)
        }

        for (item in results.items) {
            val source = item.source
            source.updatePreview(adminUIConfiguration.previewSubtype)
            eventsList.add(eventToJSON(source))
        }

        return okJsonList(eventsList, nul(offset).getOr(0), nul(limit).getOr(0), results.hitCount)
    }

    // --

    @Throws(SearchIndexException::class, NotFoundException::class, IndexServiceException::class)
    private fun getMediaPackageByEventId(eventId: String): MediaPackage {
        val optEvent = indexService.getEvent(eventId, index)
        if (optEvent.isNone)
            throw NotFoundException(format("Cannot find an event with id '%s'.", eventId))
        return indexService.getEventMediapackage(optEvent.get())
    }

    private fun getCommentUrl(eventId: String, commentId: Long): URI {
        return UrlSupport.uri(serverUrl, eventId, "comment", java.lang.Long.toString(commentId))
    }

    private fun eventToJSON(event: Event): JValue {
        val fields = ArrayList<Field>()

        fields.add(f("id", v(event.identifier)))
        fields.add(f("title", v(event.title, BLANK)))
        fields.add(f("source", v(event.source, BLANK)))
        fields.add(f("presenters", arr(`$`(event.presenters).map(Functions.stringToJValue))))
        if (StringUtils.isNotBlank(event.seriesId)) {
            val seriesTitle = event.seriesName
            val seriesID = event.seriesId

            fields.add(f("series", obj(f("id", v(seriesID, BLANK)), f("title", v(seriesTitle, BLANK)))))
        }
        fields.add(f("location", v(event.location, BLANK)))
        fields.add(f("start_date", v(event.recordingStartDate, BLANK)))
        fields.add(f("end_date", v(event.recordingEndDate, BLANK)))
        fields.add(f("managedAcl", v(event.managedAcl, BLANK)))
        fields.add(f("workflow_state", v(event.workflowState, BLANK)))
        fields.add(f("event_status", v(event.eventStatus)))
        fields.add(f("source", v(indexService.getEventSource(event).toString())))
        fields.add(f("has_comments", v(event.hasComments())))
        fields.add(f("has_open_comments", v(event.hasOpenComments())))
        fields.add(f("needs_cutting", v(event.needsCutting())))
        fields.add(f("has_preview", v(event.hasPreview())))
        fields.add(f("agent_id", v(event.agentId, BLANK)))
        fields.add(f("technical_start", v(event.technicalStartTime, BLANK)))
        fields.add(f("technical_end", v(event.technicalEndTime, BLANK)))
        fields.add(f("technical_presenters", arr(`$`(event.technicalPresenters).map(Functions.stringToJValue))))
        fields.add(f("publications", arr(eventPublicationsToJson(event))))
        return obj(fields)
    }

    private fun attachmentToJSON(attachment: Attachment): JValue {
        val fields = ArrayList<Field>()
        fields.addAll(getEventMediaPackageElementFields(attachment))
        fields.addAll(getCommonElementFields(attachment))
        return obj(fields)
    }

    private fun catalogToJSON(catalog: Catalog): JValue {
        val fields = ArrayList<Field>()
        fields.addAll(getEventMediaPackageElementFields(catalog))
        fields.addAll(getCommonElementFields(catalog))
        return obj(fields)
    }

    private fun trackToJSON(track: Track): JValue {
        val fields = ArrayList<Field>()
        fields.addAll(getEventMediaPackageElementFields(track))
        fields.addAll(getCommonElementFields(track))
        fields.add(f("duration", v(track.duration, BLANK)))
        fields.add(f("has_audio", v(track.hasAudio())))
        fields.add(f("has_video", v(track.hasVideo())))
        fields.add(f("streams", obj(streamsToJSON(track.streams))))
        return obj(fields)
    }

    private fun streamsToJSON(streams: Array<org.opencastproject.mediapackage.Stream>): List<Field> {
        val fields = ArrayList<Field>()
        val audioList = ArrayList<JValue>()
        val videoList = ArrayList<JValue>()
        for (stream in streams) {
            // TODO There is a bug with the stream ids, see MH-10325
            if (stream is AudioStreamImpl) {
                val audio = ArrayList<Field>()
                val audioStream = stream as AudioStream
                audio.add(f("id", v(audioStream.identifier, BLANK)))
                audio.add(f("type", v(audioStream.format, BLANK)))
                audio.add(f("channels", v(audioStream.channels, BLANK)))
                audio.add(f("bitrate", v(audioStream.bitRate, BLANK)))
                audio.add(f("bitdepth", v(audioStream.bitDepth, BLANK)))
                audio.add(f("samplingrate", v(audioStream.samplingRate, BLANK)))
                audio.add(f("framecount", v(audioStream.frameCount, BLANK)))
                audio.add(f("peakleveldb", v(audioStream.pkLevDb, BLANK)))
                audio.add(f("rmsleveldb", v(audioStream.rmsLevDb, BLANK)))
                audio.add(f("rmspeakdb", v(audioStream.rmsPkDb, BLANK)))
                audioList.add(obj(audio))
            } else if (stream is VideoStreamImpl) {
                val video = ArrayList<Field>()
                val videoStream = stream as VideoStream
                video.add(f("id", v(videoStream.identifier, BLANK)))
                video.add(f("type", v(videoStream.format, BLANK)))
                video.add(f("bitrate", v(videoStream.bitRate, BLANK)))
                video.add(f("framerate", v(videoStream.frameRate, BLANK)))
                video.add(f("resolution", v(videoStream.frameWidth.toString() + "x" + videoStream.frameHeight, BLANK)))
                video.add(f("framecount", v(videoStream.frameCount, BLANK)))
                video.add(f("scantype", v(videoStream.scanType, BLANK)))
                video.add(f("scanorder", v(videoStream.scanOrder, BLANK)))
                videoList.add(obj(video))
            } else {
                throw IllegalArgumentException("Stream must be either audio or video")
            }
        }
        fields.add(f("audio", arr(audioList)))
        fields.add(f("video", arr(videoList)))
        return fields
    }

    private fun publicationToJSON(publication: Publication): JValue {
        val fields = ArrayList<Field>()
        fields.add(f("id", v(publication.identifier, BLANK)))
        fields.add(f("channel", v(publication.channel, BLANK)))
        fields.add(f("mimetype", v(publication.mimeType, BLANK)))
        fields.add(f("tags", arr(`$`(*publication.tags).map(toStringJValue))))
        fields.add(f("url", v(signUrl(publication.getURI()), BLANK)))
        fields.addAll(getCommonElementFields(publication))
        return obj(fields)
    }

    private fun getCommonElementFields(element: MediaPackageElement): List<Field> {
        val fields = ArrayList<Field>()
        fields.add(f("size", v(element.size, BLANK)))
        fields.add(f("checksum", v(if (element.checksum != null) element.checksum.value else null, BLANK)))
        fields.add(f("reference", v(if (element.reference != null) element.reference.identifier else null, BLANK)))
        return fields
    }

    /**
     * Render an array of [Publication]s into a list of JSON values.
     *
     * @param publications
     * The elements to pull the data from to create the list of [JValue]s
     * @return [List] of [JValue]s that represent the [Publication]
     */
    private fun getEventPublications(publications: Array<Publication>): List<JValue> {
        val publicationJSON = ArrayList<JValue>()
        for (publication in publications) {
            publicationJSON.add(obj(f("id", v(publication.identifier, BLANK)),
                    f("channel", v(publication.channel, BLANK)), f("mimetype", v(publication.mimeType, BLANK)),
                    f("tags", arr(`$`(*publication.tags).map(toStringJValue))),
                    f("url", v(signUrl(publication.getURI()), BLANK))))
        }
        return publicationJSON
    }

    private fun signUrl(url: URI): URI {
        if (urlSigningService.accepts(url.toString())) {
            try {
                var clientIP: String? = null
                if (signWithClientIP()!!) {
                    clientIP = securityService.userIP
                }
                return URI.create(urlSigningService.sign(url.toString(), urlSigningExpireDuration, null, clientIP))
            } catch (e: UrlSigningException) {
                logger.warn("Unable to sign url '{}'", url, e)
            }

        }
        return url
    }

    /**
     * Render an array of [MediaPackageElement]s into a list of JSON values.
     *
     * @param elements
     * The elements to pull the data from to create the list of [JValue]s
     * @return [List] of [JValue]s that represent the [MediaPackageElement]
     */
    private fun getEventMediaPackageElements(elements: Array<MediaPackageElement>): List<JValue> {
        val elementJSON = ArrayList<JValue>()
        for (element in elements) {
            elementJSON.add(obj(getEventMediaPackageElementFields(element)))
        }
        return elementJSON
    }

    private fun getEventMediaPackageElementFields(element: MediaPackageElement): List<Field> {
        val fields = ArrayList<Field>()
        fields.add(f("id", v(element.identifier, BLANK)))
        fields.add(f("type", v(element.flavor, BLANK)))
        fields.add(f("mimetype", v(element.mimeType, BLANK)))
        val tags = Stream.`$`(*element.tags).map(toStringJValue).toList()
        fields.add(f("tags", arr(tags)))
        fields.add(f("url", v(signUrl(element.getURI()), BLANK)))
        return fields
    }

    @PUT
    @Path("{eventId}/workflows/{workflowId}/action/{action}")
    @RestQuery(name = "workflowAction", description = "Performs the given action for the given workflow.", returnDescription = "", pathParameters = [RestParameter(name = "eventId", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "workflowId", description = "The id of the workflow", isRequired = true, type = RestParameter.Type.STRING), RestParameter(name = "action", description = "The action to take: STOP, RETRY or NONE (abort processing)", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "Workflow resumed."), RestResponse(responseCode = SC_NOT_FOUND, description = "Event or workflow instance not found."), RestResponse(responseCode = SC_BAD_REQUEST, description = "Invalid action entered."), RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to perform the action. Maybe you need to authenticate."), RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "An exception occurred.")])
    fun workflowAction(@PathParam("eventId") id: String, @PathParam("workflowId") wfId: Long,
                       @PathParam("action") action: String): Response {
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(action)) {
            return badRequest()
        }

        try {
            val optEvent = indexService.getEvent(id, index)
            if (optEvent.isNone) {
                return notFound("Cannot find an event with id '%s'.", id)
            }

            val wfInstance = workflowService.getWorkflowById(wfId)
            if (wfInstance.mediaPackage.identifier.toString() != id) {
                return badRequest(String.format("Workflow %s is not associated to event %s", wfId, id))
            }

            if (RetryStrategy.NONE.toString().equals(action, ignoreCase = true) || RetryStrategy.RETRY.toString().equals(action, ignoreCase = true)) {
                workflowService.resume(wfId, Collections.singletonMap("retryStrategy", action))
                return ok()
            }

            if (WORKFLOW_ACTION_STOP.equals(action, ignoreCase = true)) {
                workflowService.stop(wfId)
                return ok()
            }

            return badRequest("Action not supported: $action")
        } catch (e: NotFoundException) {
            return notFound("Workflow not found: '%d'.", wfId)
        } catch (e: IllegalStateException) {
            return badRequest(String.format("Action %s not allowed for current workflow state. EventId: %s", action, id))
        } catch (e: UnauthorizedException) {
            return forbidden()
        } catch (e: Exception) {
            return serverError()
        }

    }

    @DELETE
    @Path("{eventId}/workflows/{workflowId}")
    @RestQuery(name = "deleteWorkflow", description = "Deletes a workflow", returnDescription = "The method doesn't return any content", pathParameters = [RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING), RestParameter(name = "workflowId", isRequired = true, description = "The workflow identifier", type = RestParameter.Type.INTEGER)], reponses = [RestResponse(responseCode = SC_BAD_REQUEST, description = "When trying to delete the latest workflow of the event."), RestResponse(responseCode = SC_NOT_FOUND, description = "If the event or the workflow has not been found."), RestResponse(responseCode = SC_NO_CONTENT, description = "The method does not return any content")])
    @Throws(SearchIndexException::class)
    fun deleteWorkflow(@PathParam("eventId") id: String, @PathParam("workflowId") wfId: Long): Response {
        val optEvent = indexService.getEvent(id, index)
        try {
            if (optEvent.isNone) {
                return notFound("Cannot find an event with id '%s'.", id)
            }

            val wfInstance = workflowService.getWorkflowById(wfId)
            if (wfInstance.mediaPackage.identifier.toString() != id) {
                return badRequest(String.format("Workflow %s is not associated to event %s", wfId, id))
            }

            if (wfId == optEvent.get().workflowId) {
                return badRequest(String.format("Cannot delete current workflow %s from event %s." + " Only older workflows can be deleted.", wfId, id))
            }

            workflowService.remove(wfId)

            return Response.noContent().build()
        } catch (e: WorkflowStateException) {
            return badRequest("Deleting is not allowed for current workflow state. EventId: $id")
        } catch (e: NotFoundException) {
            return notFound("Workflow not found: '%d'.", wfId)
        } catch (e: UnauthorizedException) {
            return forbidden()
        } catch (e: Exception) {
            return serverError()
        }

    }

    @Throws(UnauthorizedException::class, SearchIndexException::class)
    private fun checkAgentAccessForEvent(eventId: String) {
        val event = indexService.getEvent(eventId, index)
        if (event.isNone || !event.get().eventStatus.contains("SCHEDULE")) {
            return
        }
        SecurityUtil.checkAgentAccess(securityService, event.get().agentId)
    }

    @Throws(UnauthorizedException::class)
    private fun checkAgentAccessForAgent(agentId: String) {
        SecurityUtil.checkAgentAccess(securityService, agentId)
    }

    companion object {

        /**
         * Scheduling JSON keys
         */
        val SCHEDULING_AGENT_ID_KEY = "agentId"
        val SCHEDULING_START_KEY = "start"
        val SCHEDULING_END_KEY = "end"
        private val SCHEDULING_AGENT_CONFIGURATION_KEY = "agentConfiguration"

        private val WORKFLOW_ACTION_STOP = "STOP"

        /** The logging facility  */
        internal val logger = LoggerFactory.getLogger(AbstractEventEndpoint::class.java)

        protected val URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds"

        /** The configuration key that defines the default workflow definition  */
        //TODO Move to a constants file instead of declaring it at the top of multiple files?
        protected val WORKFLOW_DEFINITION_DEFAULT = "org.opencastproject.workflow.default.definition"

        /** The default time before a piece of signed content expires. 2 Hours.  */
        protected val DEFAULT_URL_SIGNING_EXPIRE_DURATION = (2 * 60 * 60).toLong()

        private val toStringJValue = object : Fn<String, JValue>() {
            override fun apply(stringValue: String): JValue {
                return v(stringValue, BLANK)
            }
        }

        protected val technicalMetadataToJson: Fn<TechnicalMetadata, JObject> = object : Fn<TechnicalMetadata, JObject>() {
            override fun apply(technicalMetadata: TechnicalMetadata): JObject {
                val agentConfig = if (technicalMetadata.captureAgentConfiguration == null)
                    v("")
                else
                    JSONUtils.mapToJSON(technicalMetadata.captureAgentConfiguration)
                val start = if (technicalMetadata.startDate == null)
                    v("")
                else
                    v(DateTimeSupport.toUTC(technicalMetadata.startDate.time))
                val end = if (technicalMetadata.endDate == null)
                    v("")
                else
                    v(DateTimeSupport.toUTC(technicalMetadata.endDate.time))
                return obj(f("agentId", v(technicalMetadata.agentId, BLANK)), f("agentConfiguration", agentConfig),
                        f("start", start), f("end", end), f("eventId", v(technicalMetadata.eventId, BLANK)),
                        f("presenters", JSONUtils.setToJSON(technicalMetadata.presenters)),
                        f("recording", recordingToJson.apply(technicalMetadata.recording)))
            }
        }

        val recordingToJson: Fn<Opt<Recording>, JObject> = object : Fn<Opt<Recording>, JObject>() {
            override fun apply(recording: Opt<Recording>): JObject {
                return if (recording.isNone) {
                    obj()
                } else obj(f("id", v(recording.get().id, BLANK)),
                        f("lastCheckInTime", v(recording.get().lastCheckinTime, BLANK)),
                        f("lastCheckInTimeUTC", v(INSTANCE.toUTC(recording.get().lastCheckinTime), BLANK)),
                        f("state", v(recording.get().state, BLANK)))
            }
        }
    }

}
