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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.Opt.nul;
import static com.entwinemedia.fn.data.Opt.some;
import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.NULL;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.RestUtils.conflictJson;
import static org.opencastproject.index.service.util.RestUtils.notFound;
import static org.opencastproject.index.service.util.RestUtils.notFoundJson;
import static org.opencastproject.index.service.util.RestUtils.okJson;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.index.service.util.RestUtils.serverErrorJson;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.conflict;
import static org.opencastproject.util.RestUtil.R.forbidden;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.adminui.exception.JobEndpointException;
import org.opencastproject.adminui.impl.AdminUIConfiguration;
import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.adminui.util.BulkUpdateUtil;
import org.opencastproject.adminui.util.QueryPreprocessor;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentException;
import org.opencastproject.event.comment.EventCommentReply;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.api.IndexService.Source;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataList.Locked;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.event.EventUtils;
import org.opencastproject.index.service.resources.list.provider.EventCommentsListProvider;
import org.opencastproject.index.service.resources.list.provider.EventsListProvider.Comments;
import org.opencastproject.index.service.resources.list.query.EventListQuery;
import org.opencastproject.index.service.util.AccessInformationUtil;
import org.opencastproject.index.service.util.JSONUtils;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.rest.BulkOperationResult;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowStateException;
import org.opencastproject.workflow.api.WorkflowUtil;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.Jsons.Functions;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The event endpoint acts as a facade for WorkflowService and Archive providing a unified query interface and result
 * set.
 * <p>
 * This first implementation uses the {@link org.opencastproject.assetmanager.api.AssetManager}. In a later iteration
 * the endpoint may abstract over the concrete archive.
 */
@Path("/")
@RestService(name = "eventservice", title = "Event Service",
  abstractText = "Provides resources and operations related to the events",
  notes = { "This service offers the event CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public abstract class AbstractEventEndpoint {

  /**
   * Scheduling JSON keys
   */
  public static final String SCHEDULING_AGENT_ID_KEY = "agentId";
  public static final String SCHEDULING_START_KEY = "start";
  public static final String SCHEDULING_END_KEY = "end";
  private static final String SCHEDULING_AGENT_CONFIGURATION_KEY = "agentConfiguration";
  private static final String SCHEDULING_OPT_OUT_KEY = "optOut";

  private static final String WORKFLOW_ACTION_STOP = "STOP";

  /** The logging facility */
  static final Logger logger = LoggerFactory.getLogger(AbstractEventEndpoint.class);

  protected static final String URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds";

  /** The configuration key that defines the default workflow definition */
  //TODO Move to a constants file instead of declaring it at the top of multiple files?
  protected static final String WORKFLOW_DEFINITION_DEFAULT = "org.opencastproject.workflow.default.definition";

  /** The default time before a piece of signed content expires. 2 Hours. */
  protected static final long DEFAULT_URL_SIGNING_EXPIRE_DURATION = 2 * 60 * 60;

  public abstract WorkflowService getWorkflowService();

  public abstract AdminUISearchIndex getIndex();

  public abstract JobEndpoint getJobService();

  public abstract AclService getAclService();

  public abstract EventCommentService getEventCommentService();

  public abstract SecurityService getSecurityService();

  public abstract IndexService getIndexService();

  public abstract AuthorizationService getAuthorizationService();

  public abstract SchedulerService getSchedulerService();

  public abstract CaptureAgentStateService getCaptureAgentStateService();

  public abstract AdminUIConfiguration getAdminUIConfiguration();

  public abstract long getUrlSigningExpireDuration();

  public abstract UrlSigningService getUrlSigningService();

  public abstract Boolean signWithClientIP();

  /** Default server URL */
  protected String serverUrl = "http://localhost:8080";

  /** Service url */
  protected String serviceUrl = null;

  /** The default workflow identifier, if one is configured */
  protected String defaultWorkflowDefinionId = null;

  /**
   * Activates REST service.
   *
   * @param cc
   *          ComponentContext
   */
  public void activate(ComponentContext cc) {
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
      if (StringUtils.isNotBlank(ccServerUrl))
        this.serverUrl = ccServerUrl;

      this.serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);

      String ccDefaultWorkflowDefinionId = StringUtils.trimToNull(cc.getBundleContext().getProperty(WORKFLOW_DEFINITION_DEFAULT));

      if (StringUtils.isNotBlank(ccDefaultWorkflowDefinionId))
        this.defaultWorkflowDefinionId = ccDefaultWorkflowDefinionId;
    }

  }

  /* As the list of event ids can grow large, we use a POST request to avoid problems with too large query strings */
  @POST
  @Path("workflowProperties")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "workflowProperties", description = "Returns workflow properties for the specified events",
             returnDescription = "The workflow properties for every event as JSON", restParameters = {
                @RestParameter(name = "eventIds", description = "A JSON array of ids of the events", isRequired = true, type = RestParameter.Type.STRING)},
             reponses = {
                @RestResponse(description = "Returns the workflow properties for the events as JSON", responseCode = HttpServletResponse.SC_OK),
                @RestResponse(description = "The list of ids could not be parsed into a json list.", responseCode = HttpServletResponse.SC_BAD_REQUEST)
              })
  public Response getEventWorkflowProperties(@FormParam("eventIds") String eventIds) throws UnauthorizedException {
    if (StringUtils.isBlank(eventIds)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    JSONParser parser = new JSONParser();
    List<String> ids;
    try {
      ids = (List<String>) parser.parse(eventIds);
    } catch (org.json.simple.parser.ParseException e) {
      logger.error("Unable to parse '{}'", eventIds, e);
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (ClassCastException e) {
      logger.error("Unable to cast '{}'", eventIds, e);
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    final Map<String, Map<String, String>> eventWithProperties = getIndexService().getEventWorkflowProperties(ids);
    final Map<String, Field> jsonEvents = new HashMap<>();
    for (Entry<String, Map<String, String>> event : eventWithProperties.entrySet()) {
      final Collection<Field> jsonProperties = new ArrayList<>();
      for (Entry<String, String> property : event.getValue().entrySet()) {
        jsonProperties.add(f(property.getKey(),property.getValue()));
      }
      jsonEvents.put(event.getKey(), f(event.getKey(), obj(jsonProperties)));
    }
    return okJson(obj(jsonEvents));
  }


  @GET
  @Path("catalogAdapters")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getcataloguiadapters", description = "Returns the available catalog UI adapters as JSON", returnDescription = "The catalog UI adapters as JSON", reponses = {
          @RestResponse(description = "Returns the available catalog UI adapters as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response getCatalogAdapters() {
    List<JValue> adapters = new ArrayList<>();
    for (EventCatalogUIAdapter adapter : getIndexService().getEventCatalogUIAdapters()) {
      List<Field> fields = new ArrayList<>();
      fields.add(f("flavor", v(adapter.getFlavor().toString())));
      fields.add(f("title", v(adapter.getUITitle())));
      adapters.add(obj(fields));
    }
    return okJson(arr(adapters));
  }

  @GET
  @Path("{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getevent", description = "Returns the event by the given id as JSON", returnDescription = "The event as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns the event as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventResponse(@PathParam("eventId") String id) throws Exception {
    for (final Event event : getIndexService().getEvent(id, getIndex())) {
      event.updatePreview(getAdminUIConfiguration().getPreviewSubtype());
      return okJson(eventToJSON(event));
    }
    return notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteevent", description = "Delete a single event.", returnDescription = "Ok if the event has been deleted.", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The id of the event to delete.", type = STRING), }, reponses = {
                  @RestResponse(responseCode = SC_OK, description = "The event has been deleted."),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response deleteEvent(@PathParam("eventId") String id) throws UnauthorizedException, SearchIndexException {
    try {
      checkAgentAccessForEvent(id);
      if (!getIndexService().removeEvent(id))
        return Response.serverError().build();
    } catch (NotFoundException e) {
      // If we couldn't find any trace of the event in the underlying database(s), we can get rid of it
      // entirely in the index.
      try {
        getIndex().delete(Event.DOCUMENT_TYPE,id.concat(getSecurityService().getOrganization().getId()));
      } catch (SearchIndexException e1) {
        logger.error("error removing event {}: {}",id,e1);
        return Response.serverError().build();
      }
    }

    return Response.ok().build();
  }

  @POST
  @Path("deleteEvents")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteevents", description = "Deletes a json list of events by their given ids e.g. [\"1dbe7255-e17d-4279-811d-a5c7ced689bf\", \"04fae22b-0717-4f59-8b72-5f824f76d529\"]", returnDescription = "Returns a JSON object containing a list of event ids that were deleted, not found or if there was a server error.", reponses = {
          @RestResponse(description = "Events have been deleted", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The list of ids could not be parsed into a json list.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "If the current user is not authorized to perform this action", responseCode = HttpServletResponse.SC_UNAUTHORIZED) })
  public Response deleteEvents(String eventIdsContent) throws UnauthorizedException, SearchIndexException {
    if (StringUtils.isBlank(eventIdsContent)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    JSONParser parser = new JSONParser();
    JSONArray eventIdsJsonArray;
    try {
      eventIdsJsonArray = (JSONArray) parser.parse(eventIdsContent);
    } catch (org.json.simple.parser.ParseException e) {
      logger.error("Unable to parse '{}' because: {}", eventIdsContent, ExceptionUtils.getStackTrace(e));
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (ClassCastException e) {
      logger.error("Unable to cast '{}' because: {}", eventIdsContent, ExceptionUtils.getStackTrace(e));
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    BulkOperationResult result = new BulkOperationResult();

    for (Object eventIdObject : eventIdsJsonArray) {
      String eventId = eventIdObject.toString();
      try {
        checkAgentAccessForEvent(eventId);
        if (!getIndexService().removeEvent(eventId)) {
          result.addServerError(eventId);
        } else {
          result.addOk(eventId);
        }
      } catch (NotFoundException e) {
        result.addNotFound(eventId);
      } catch (UnauthorizedException e) {
        result.addUnauthorized(eventId);
      }
    }
    return Response.ok(result.toJson()).build();
  }

  @GET
  @Path("{eventId}/hasSnapshots.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "hassnapshots", description = "Returns a JSON object containing a boolean indicating if snapshots exist for this event", returnDescription = "A JSON object", pathParameters = {
    @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
    @RestResponse(description = "A JSON object containing a property \"hasSnapshots\"", responseCode = HttpServletResponse.SC_OK) })
  public Response hasEventSnapshots(@PathParam("eventId") String id) throws Exception {
    return okJson(obj(f("hasSnapshots",this.getIndexService().hasSnapshots(id))));
  }

  @GET
  @Path("{eventId}/publications.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventpublications", description = "Returns all the data related to the publications tab in the event details modal as JSON", returnDescription = "All the data related to the event publications tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id (mediapackage id).", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event publications tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventPublicationsTab(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

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

    Event event = optEvent.get();
    List<JValue> pubJSON = eventPublicationsToJson(event);

    return okJson(obj(f("publications", arr(pubJSON)), f("optout", v(event.getOptedOut(), Jsons.BLANK)),
            f("blacklisted", v(event.getBlacklisted(), Jsons.BLANK)),
            f("review-status", v(event.getReviewStatus(), Jsons.BLANK)),
            f("start-date", v(event.getRecordingStartDate(), Jsons.BLANK)),
            f("end-date", v(event.getRecordingEndDate(), Jsons.BLANK))));
  }

  private List<JValue> eventPublicationsToJson(Event event) {
    List<JValue> pubJSON = new ArrayList<>();
    for (JObject json : Stream.$(event.getPublications()).filter(EventUtils.internalChannelFilter)
            .map(publicationToJson)) {
      pubJSON.add(json);
    }
    return pubJSON;
  }

  @GET
  @Path("{eventId}/scheduling.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEventSchedulingMetadata", description = "Returns all of the scheduling metadata for an event", returnDescription = "All the technical metadata related to scheduling as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id (mediapackage id).", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event scheduling tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventScheduling(@PathParam("eventId") String eventId)
          throws NotFoundException, UnauthorizedException, SearchIndexException {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      TechnicalMetadata technicalMetadata = getSchedulerService().getTechnicalMetadata(eventId);
      return okJson(technicalMetadataToJson.apply(technicalMetadata));
    } catch (SchedulerException e) {
      logger.error("Unable to get technical metadata for event with id {}", eventId);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("scheduling.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEventsScheduling", description = "Returns all of the scheduling metadata for a list of events", returnDescription = "All the technical metadata related to scheduling as JSON", restParameters = {
    @RestParameter(name = "eventIds", description = "An array of event IDs (mediapackage id)", isRequired = true, type = RestParameter.Type.STRING),
    @RestParameter(name = "ignoreNonScheduled", description = "Whether events that are not really scheduled events should be ignored or produce an error", isRequired = true, type = RestParameter.Type.BOOLEAN) }, reponses = {
    @RestResponse(description = "Returns all the data related to the event scheduling tab as JSON", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventsScheduling(@FormParam("eventIds") final List<String> eventIds, @FormParam("ignoreNonScheduled") final boolean ignoreNonScheduled) {
    final List<JValue> fields = new ArrayList<>(eventIds.size());
    for (final String eventId : eventIds) {
      try {
        fields.add(technicalMetadataToJson.apply(getSchedulerService().getTechnicalMetadata(eventId)));
      } catch (final NotFoundException e) {
        if (!ignoreNonScheduled) {
          logger.warn("Unable to find id {}", eventId, ExceptionUtils.getStackTrace(e));
          return notFound("Cannot find an event with id '%s'.", eventId);
        }
      } catch (final UnauthorizedException e) {
        logger.warn("Unauthorized access to event ID {}", eventId, ExceptionUtils.getStackTrace(e));
        return Response.status(Status.BAD_REQUEST).build();
      } catch (final SchedulerException e) {
        logger.warn("Scheduler exception accessing event ID {}", eventId, ExceptionUtils.getStackTrace(e));
        return Response.status(Status.BAD_REQUEST).build();
      }
    }
    return okJson(arr(fields));
  }

  @PUT
  @Path("{eventId}/scheduling")
  @RestQuery(name = "updateEventScheduling", description = "Updates the scheduling information of an event", returnDescription = "The method doesn't return any content", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING) }, restParameters = {
                  @RestParameter(name = "scheduling", isRequired = true, description = "The updated scheduling (JSON object)", type = RestParameter.Type.TEXT) }, reponses = {
                          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required params were missing in the request."),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."),
                          @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content") })
  public Response updateEventScheduling(@PathParam("eventId") String eventId,
          @FormParam("scheduling") String scheduling)
          throws NotFoundException, UnauthorizedException, SearchIndexException {
    if (StringUtils.isBlank(scheduling))
      return RestUtil.R.badRequest("Missing parameters");

    try {
      final Event event = getEventOrThrowNotFoundException(eventId);
      updateEventScheduling(scheduling, event);
      return Response.noContent().build();
    } catch (JSONException e) {
      return RestUtil.R.badRequest("The scheduling object is not valid");
    } catch (ParseException e) {
      return RestUtil.R.badRequest("The UTC dates in the scheduling object is not valid");
    } catch (SchedulerException e) {
      logger.error("Unable to update scheduling technical metadata of event {}: {}", eventId,
              ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    } catch (IllegalStateException e) {
      return RestUtil.R.badRequest(e.getMessage());
    }
  }

  private void updateEventScheduling(String scheduling, Event event) throws NotFoundException, UnauthorizedException,
    SchedulerException, JSONException, ParseException {
    final TechnicalMetadata technicalMetadata = getSchedulerService().getTechnicalMetadata(event.getIdentifier());
    final org.codehaus.jettison.json.JSONObject schedulingJson = new org.codehaus.jettison.json.JSONObject(
            scheduling);
    Opt<String> agentId = Opt.none();
    if (schedulingJson.has(SCHEDULING_AGENT_ID_KEY)) {
      agentId = Opt.some(schedulingJson.getString(SCHEDULING_AGENT_ID_KEY));
      logger.trace("Updating agent id of event '{}' from '{}' to '{}'",
        event.getIdentifier(), technicalMetadata.getAgentId(), agentId);
    }

    // Check if we are allowed to re-schedule on this agent
    checkAgentAccessForAgent(technicalMetadata.getAgentId());
    if (agentId.isSome()) {
      checkAgentAccessForAgent(agentId.get());
    }

    Opt<Date> start = Opt.none();
    if (schedulingJson.has(SCHEDULING_START_KEY)) {
      start = Opt.some(new Date(DateTimeSupport.fromUTC(schedulingJson.getString(SCHEDULING_START_KEY))));
      logger.trace("Updating start time of event '{}' id from '{}' to '{}'",
        event.getIdentifier(), DateTimeSupport.toUTC(technicalMetadata.getStartDate().getTime()),
                      DateTimeSupport.toUTC(start.get().getTime()));
    }

    Opt<Date> end = Opt.none();
    if (schedulingJson.has(SCHEDULING_END_KEY)) {
      end = Opt.some(new Date(DateTimeSupport.fromUTC(schedulingJson.getString(SCHEDULING_END_KEY))));
      logger.trace("Updating end time of event '{}' id from '{}' to '{}'",
        event.getIdentifier(), DateTimeSupport.toUTC(technicalMetadata.getEndDate().getTime()),
                      DateTimeSupport.toUTC(end.get().getTime()));
    }

    Opt<Map<String, String>> agentConfiguration = Opt.none();
    if (schedulingJson.has(SCHEDULING_AGENT_CONFIGURATION_KEY)) {
      agentConfiguration = Opt.some(JSONUtils.toMap(schedulingJson.getJSONObject(SCHEDULING_AGENT_CONFIGURATION_KEY)));
      logger.trace("Updating agent configuration of event '{}' id from '{}' to '{}'",
        event.getIdentifier(), technicalMetadata.getCaptureAgentConfiguration(), agentConfiguration);
    }

    Opt<Opt<Boolean>> optOut = Opt.none();
    if (schedulingJson.has(SCHEDULING_OPT_OUT_KEY)) {
      optOut = Opt.some(Opt.some(schedulingJson.getBoolean(SCHEDULING_OPT_OUT_KEY)));
      logger.trace("Updating optout status of event '{}' id from '{}' to '{}'",
        event.getIdentifier(), event.getOptedOut(), optOut);
    }

    if ((start.isSome() || end.isSome())
            && end.getOr(technicalMetadata.getEndDate()).before(start.getOr(technicalMetadata.getStartDate()))) {
      throw new IllegalStateException("The end date is before the start date");
    }

    if (!start.isNone() || !end.isNone() || !agentId.isNone() || !agentConfiguration.isNone() || !optOut.isNone()) {
      getSchedulerService()
        .updateEvent(event.getIdentifier(), start, end, agentId, Opt.none(), Opt.none(), Opt.none(), agentConfiguration, optOut, SchedulerService.ORIGIN);
    }
  }

  private Event getEventOrThrowNotFoundException(final String eventId) throws NotFoundException, SearchIndexException {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isSome()) {
      return optEvent.get();
    } else {
      throw new NotFoundException(format("Cannot find an event with id '%s'.", eventId));
    }
  }

  @GET
  @Path("{eventId}/comments")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventcomments", description = "Returns all the data related to the comments tab in the event details modal as JSON", returnDescription = "All the data related to the event comments tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event comments tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventComments(@PathParam("eventId") String eventId) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      List<Val> commentArr = new ArrayList<>();
      for (EventComment c : comments) {
        commentArr.add(c.toJson());
      }
      return Response.ok(org.opencastproject.util.Jsons.arr(commentArr).toJson(), MediaType.APPLICATION_JSON_TYPE)
              .build();
    } catch (EventCommentException e) {
      logger.error("Unable to get comments from event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("{eventId}/hasActiveTransaction")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "hasactivetransaction", description = "Returns whether there is currently a transaction in progress for the given event", returnDescription = "Whether there is currently a transaction in progress for the given event", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns whether there is currently a transaction in progress for the given event", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response hasActiveTransaction(@PathParam("eventId") String eventId) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    JSONObject json = new JSONObject();

    if (WorkflowUtil.isActive(optEvent.get().getWorkflowState())) {
      json.put("active", true);
    } else {
      json.put("active", false);
    }

    return Response.ok(json.toJSONString()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{eventId}/comment/{commentId}")
  @RestQuery(name = "geteventcomment", description = "Returns the comment with the given identifier", returnDescription = "Returns the comment as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, reponses = {
                  @RestResponse(responseCode = SC_OK, description = "The comment as JSON."),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "No event or comment with this identifier was found.") })
  public Response getEventComment(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId)
          throws NotFoundException, Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      EventComment comment = getEventCommentService().getComment(commentId);
      return Response.ok(comment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve comment {}: {}", commentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("{eventId}/comment/{commentId}")
  @RestQuery(name = "updateeventcomment", description = "Updates an event comment", returnDescription = "The updated comment as JSON.", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, restParameters = {
                  @RestParameter(name = "text", isRequired = false, description = "The comment text", type = TEXT),
                  @RestParameter(name = "reason", isRequired = false, description = "The comment reason", type = STRING),
                  @RestParameter(name = "resolved", isRequired = false, description = "The comment resolved status", type = RestParameter.Type.BOOLEAN) }, reponses = {
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to update has not been found."),
                          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response updateEventComment(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId,
          @FormParam("text") String text, @FormParam("reason") String reason, @FormParam("resolved") Boolean resolved)
                  throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      EventComment dto = getEventCommentService().getComment(commentId);

      if (StringUtils.isNotBlank(text)) {
        text = text.trim();
      } else {
        text = dto.getText();
      }

      if (StringUtils.isNotBlank(reason)) {
        reason = reason.trim();
      } else {
        reason = dto.getReason();
      }

      if (resolved == null)
        resolved = dto.isResolvedStatus();

      EventComment updatedComment = EventComment.create(dto.getId(), eventId,
              getSecurityService().getOrganization().getId(), text, dto.getAuthor(), reason, resolved,
              dto.getCreationDate(), new Date(), dto.getReplies());

      updatedComment = getEventCommentService().updateComment(updatedComment);
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      getIndexService().updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to update the comments catalog on event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("{eventId}/access")
  @RestQuery(name = "applyAclToEvent", description = "Immediate application of an ACL to an event", returnDescription = "Status code", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event ID", type = STRING) }, restParameters = {
                  @RestParameter(name = "acl", isRequired = true, description = "The ACL to apply", type = STRING) }, reponses = {
                          @RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"),
                          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the given ACL"),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "The the event has not been found"),
                          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action"),
                          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error") })
  public Response applyAclToEvent(@PathParam("eventId") String eventId, @FormParam("acl") String acl)
          throws NotFoundException, UnauthorizedException, SearchIndexException, IndexServiceException {
    final AccessControlList accessControlList;
    try {
      accessControlList = AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      logger.warn("Unable to parse ACL '{}'", acl);
      return badRequest();
    }

    try {
      final Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
      if (optEvent.isNone()) {
        logger.warn("Unable to find the event '{}'", eventId);
        return notFound();
      }

      Source eventSource = getIndexService().getEventSource(optEvent.get());
      if (eventSource == Source.ARCHIVE) {
        if (getAclService().applyAclToEpisode(eventId, accessControlList, Option.<ConfiguredWorkflowRef> none())) {
          return ok();
        } else {
          logger.warn("Unable to find the event '{}'", eventId);
          return notFound();
        }
      } else if (eventSource == Source.WORKFLOW) {
        logger.warn("An ACL cannot be edited while an event is part of a current workflow because it might"
                + " lead to inconsistent ACLs i.e. changed after distribution so that the old ACL is still "
                + "being used by the distribution channel.");
        JSONObject json = new JSONObject();
        json.put("Error", "Unable to edit an ACL for a current workflow.");
        return conflict(json.toJSONString());
      } else {
        MediaPackage mediaPackage = getIndexService().getEventMediapackage(optEvent.get());
        mediaPackage = getAuthorizationService().setAcl(mediaPackage, AclScope.Episode, accessControlList).getA();
        // We could check agent access here if we want to forbid updating ACLs for users without access.
        getSchedulerService().updateEvent(eventId, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
                Opt.<Set<String>> none(), some(mediaPackage), Opt.<Map<String, String>> none(),
                Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
        return ok();
      }
    } catch (AclServiceException e) {
      logger.error("Error applying acl '{}' to event '{}' because: {}",
              accessControlList, eventId, ExceptionUtils.getStackTrace(e));
      return serverError();
    } catch (SchedulerException e) {
      logger.error("Error applying ACL to scheduled event {} because {}", eventId, ExceptionUtils.getStackTrace(e));
      return serverError();
    }
  }

  @POST
  @Path("{eventId}/comment")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createeventcomment", description = "Creates a comment related to the event given by the identifier", returnDescription = "The comment related to the event as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {
                  @RestParameter(name = "text", isRequired = true, description = "The comment text", type = TEXT),
                  @RestParameter(name = "resolved", isRequired = false, description = "The comment resolved status", type = RestParameter.Type.BOOLEAN),
                  @RestParameter(name = "reason", isRequired = false, description = "The comment reason", type = STRING) }, reponses = {
                          @RestResponse(description = "The comment has been created.", responseCode = HttpServletResponse.SC_CREATED),
                          @RestResponse(description = "If no text ist set.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response createEventComment(@PathParam("eventId") String eventId, @FormParam("text") String text,
          @FormParam("reason") String reason, @FormParam("resolved") Boolean resolved) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    if (StringUtils.isBlank(text))
      return Response.status(Status.BAD_REQUEST).build();

    User author = getSecurityService().getUser();
    try {
      EventComment createdComment = EventComment.create(Option.<Long> none(), eventId,
              getSecurityService().getOrganization().getId(), text, author, reason, BooleanUtils.toBoolean(reason));
      createdComment = getEventCommentService().updateComment(createdComment);
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      getIndexService().updateCommentCatalog(optEvent.get(), comments);
      return Response.created(getCommentUrl(eventId, createdComment.getId().get()))
              .entity(createdComment.toJson().toJson()).build();
    } catch (Exception e) {
      logger.error("Unable to create a comment on the event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("{eventId}/comment/{commentId}")
  @RestQuery(name = "resolveeventcomment", description = "Resolves an event comment", returnDescription = "The resolved comment.", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, reponses = {
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to resolve has not been found."),
                  @RestResponse(responseCode = SC_OK, description = "The resolved comment as JSON.") })
  public Response resolveEventComment(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId)
          throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      EventComment dto = getEventCommentService().getComment(commentId);
      EventComment updatedComment = EventComment.create(dto.getId(), dto.getEventId(), dto.getOrganization(),
              dto.getText(), dto.getAuthor(), dto.getReason(), true, dto.getCreationDate(), new Date(),
              dto.getReplies());

      updatedComment = getEventCommentService().updateComment(updatedComment);
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      getIndexService().updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not resolve comment {}: {}", commentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Path("{eventId}/comment/{commentId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteeventcomment", description = "Deletes a event related comment by its identifier", returnDescription = "No content", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", description = "The comment id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "The event related comment has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "No event or comment with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEventComment(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId)
          throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      getEventCommentService().deleteComment(commentId);
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      getIndexService().updateCommentCatalog(optEvent.get(), comments);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to delete comment {} on event {}: {}",
              commentId, eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Path("{eventId}/comment/{commentId}/{replyId}")
  @RestQuery(name = "deleteeventreply", description = "Delete an event comment reply", returnDescription = "The updated comment as JSON.", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING),
          @RestParameter(name = "replyId", isRequired = true, description = "The comment reply identifier", type = STRING) }, reponses = {
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "No event comment or reply with this identifier was found."),
                  @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response deleteEventCommentReply(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId,
          @PathParam("replyId") long replyId) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    EventComment comment = null;
    EventCommentReply reply = null;
    try {
      comment = getEventCommentService().getComment(commentId);
      for (EventCommentReply r : comment.getReplies()) {
        if (r.getId().isNone() || replyId != r.getId().get().longValue())
          continue;
        reply = r;
        break;
      }

      if (reply == null)
        throw new NotFoundException("Reply with id " + replyId + " not found!");

      comment.removeReply(reply);

      EventComment updatedComment = getEventCommentService().updateComment(comment);
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      getIndexService().updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not remove event comment reply {} from comment {}: {}",
              replyId, commentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("{eventId}/comment/{commentId}/{replyId}")
  @RestQuery(name = "updateeventcommentreply", description = "Updates an event comment reply", returnDescription = "The updated comment as JSON.", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING),
          @RestParameter(name = "replyId", isRequired = true, description = "The comment reply identifier", type = STRING) }, restParameters = {
                  @RestParameter(name = "text", isRequired = true, description = "The comment reply text", type = TEXT) }, reponses = {
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to extend with a reply or the reply has not been found."),
                          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "If no text is set."),
                          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response updateEventCommentReply(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId,
          @PathParam("replyId") long replyId, @FormParam("text") String text) throws Exception {
    if (StringUtils.isBlank(text))
      return Response.status(Status.BAD_REQUEST).build();

    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    EventComment comment = null;
    EventCommentReply reply = null;
    try {
      comment = getEventCommentService().getComment(commentId);
      for (EventCommentReply r : comment.getReplies()) {
        if (r.getId().isNone() || replyId != r.getId().get().longValue())
          continue;
        reply = r;
        break;
      }

      if (reply == null)
        throw new NotFoundException("Reply with id " + replyId + " not found!");

      EventCommentReply updatedReply = EventCommentReply.create(reply.getId(), text.trim(), reply.getAuthor(),
              reply.getCreationDate(), new Date());
      comment.removeReply(reply);
      comment.addReply(updatedReply);

      EventComment updatedComment = getEventCommentService().updateComment(comment);
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      getIndexService().updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not update event comment reply {} from comment {}: {}",
              replyId, commentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("{eventId}/comment/{commentId}/reply")
  @RestQuery(name = "createeventcommentreply", description = "Creates an event comment reply", returnDescription = "The updated comment as JSON.", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, restParameters = {
                  @RestParameter(name = "text", isRequired = true, description = "The comment reply text", type = TEXT),
                  @RestParameter(name = "resolved", isRequired = false, description = "Flag defining if this reply solve or not the comment.", type = BOOLEAN) }, reponses = {
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to extend with a reply has not been found."),
                          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "If no text is set."),
                          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response createEventCommentReply(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId,
          @FormParam("text") String text, @FormParam("resolved") Boolean resolved) throws Exception {
    if (StringUtils.isBlank(text))
      return Response.status(Status.BAD_REQUEST).build();

    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    EventComment comment = null;
    try {
      comment = getEventCommentService().getComment(commentId);
      EventComment updatedComment;

      if (resolved != null && resolved) {
        // If the resolve flag is set to true, change to comment to resolved
        updatedComment = EventComment.create(comment.getId(), comment.getEventId(), comment.getOrganization(),
                comment.getText(), comment.getAuthor(), comment.getReason(), true, comment.getCreationDate(),
                new Date(), comment.getReplies());
      } else {
        updatedComment = comment;
      }

      User author = getSecurityService().getUser();
      EventCommentReply reply = EventCommentReply.create(Option.<Long> none(), text, author);
      updatedComment.addReply(reply);

      updatedComment = getEventCommentService().updateComment(updatedComment);
      List<EventComment> comments = getEventCommentService().getComments(eventId);
      getIndexService().updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (Exception e) {
      logger.warn("Could not create event comment reply on comment {}: {}", comment, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("{eventId}/participation.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventparticipationinformation", description = "Get the particition information of a event", returnDescription = "The participation information", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."),
                  @RestResponse(responseCode = SC_OK, description = "The access information ") })
  public Response getEventParticipation(@PathParam("eventId") String eventId) throws Exception {
    final Event event = getEventOrThrowNotFoundException(eventId);

    Date startDate = new DateTime(event.getTechnicalStartTime()).toDateTime(DateTimeZone.UTC).toDate();
    Date currentDate = new DateTime().toDateTime(DateTimeZone.UTC).toDate();
    boolean readOnly = false;

    if (currentDate.after(startDate)) {
      readOnly = true;
    }

    Boolean optedOut = event.getOptedOut();

    return okJson(obj(f("opt_out", v(optedOut != null ? optedOut : false)),
            f("review_status", v(event.getReviewStatus(), BLANK)), f("read_only", v(readOnly))));
  }

  @GET
  @Path("{eventId}/metadata.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventmetadata", description = "Returns all the data related to the metadata tab in the event details modal as JSON", returnDescription = "All the data related to the event metadata tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event metadata tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventMetadata(@PathParam("eventId") String eventId) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    MetadataList metadataList = new MetadataList();
    List<EventCatalogUIAdapter> catalogUIAdapters = getIndexService().getEventCatalogUIAdapters();
    catalogUIAdapters.remove(getIndexService().getCommonEventCatalogUIAdapter());
    MediaPackage mediaPackage = getIndexService().getEventMediapackage(optEvent.get());
    for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
      metadataList.add(catalogUIAdapter, catalogUIAdapter.getFields(mediaPackage));
    }
    metadataList.add(getIndexService().getCommonEventCatalogUIAdapter(),
            EventUtils.getEventMetadata(optEvent.get(), getIndexService().getCommonEventCatalogUIAdapter()));

    final String wfState = optEvent.get().getWorkflowState();
    if (wfState != null && WorkflowUtil.isActive(WorkflowInstance.WorkflowState.valueOf(wfState)))
      metadataList.setLocked(Locked.WORKFLOW_RUNNING);

    return okJson(metadataList.toJSON());
  }

  @PUT
  @Path("bulk/update")
  @RestQuery(name = "bulkupdate", description = "Update all of the given events at once", restParameters = {
    @RestParameter(name = "update", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of events and fields to update.")}, reponses = {
    @RestResponse(description = "All events have been updated successfully.", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "Could not parse update instructions.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
    @RestResponse(description = "Faield updating metadata or scheduling information. Some events may have been updated. Details are available in the response body.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
    @RestResponse(description = "The events in the response body were not found. No events were updated.", responseCode = HttpServletResponse.SC_NOT_FOUND)},
    returnDescription = "In case of success, no content is returned. In case of errors while updating the metadata or scheduling information, the errors are returned. In case events were not found, their ids are returned")
  public Response bulkUpdate(@FormParam("update") String updateJson) {

    final BulkUpdateUtil.BulkUpdateInstructions instructions;
    try {
      instructions = new BulkUpdateUtil.BulkUpdateInstructions(updateJson);
    } catch (IllegalArgumentException e) {
      return badRequest("Cannot parse bulk update instructions");
    }

    // Get all the events to edit
    final Map<String, Optional<Event>> events = instructions.getEventIds().stream()
      .collect(Collectors.toMap(id -> id, id -> BulkUpdateUtil.getEvent(getIndexService(), getIndex(), id)));

    // Check for invalid (non-existing) event ids
    final Set<String> notFoundIds = events.entrySet().stream()
      .filter(e -> !e.getValue().isPresent())
      .map(Entry::getKey)
      .collect(Collectors.toSet());
    if (!notFoundIds.isEmpty()) {
      return notFoundJson(JSONUtils.setToJSON(notFoundIds));
    }

    final Map<String, String> metadataUpdateFailures = new HashMap<>();
    final Map<String, String> schedulingUpdateFailures = new HashMap<>();

    events.values().forEach(e -> e.ifPresent(event -> {

      JSONObject metadata = null;

      // Update the scheduling information
      try {
        if (instructions.getScheduling() != null) {
          // Since we only have the start/end time, we have to add the correct date(s) for this event.
          final JSONObject scheduling = BulkUpdateUtil.addSchedulingDates(event, instructions.getScheduling());
          updateEventScheduling(scheduling.toJSONString(), event);
          // We have to update the non-technical metadata as well to keep them in sync with the technical ones.
          metadata = BulkUpdateUtil.toNonTechnicalMetadataJson(scheduling);
        }
      } catch (Exception exception) {
        schedulingUpdateFailures.put(event.getIdentifier(), exception.getMessage());
      }

      // Update the event metadata
      try {
        if (instructions.getMetadata() != null || metadata != null) {
          metadata = BulkUpdateUtil.mergeMetadataFields(metadata, instructions.getMetadata());
          getIndexService().updateAllEventMetadata(event.getIdentifier(),
            JSONArray.toJSONString(Collections.singletonList(metadata)), getIndex());
        }
      } catch (Exception exception) {
        metadataUpdateFailures.put(event.getIdentifier(), exception.getMessage());
      }
    }));

    // Check if there were any errors updating the metadata or scheduling information
    if (!metadataUpdateFailures.isEmpty() || !schedulingUpdateFailures.isEmpty()) {
      return serverErrorJson(obj(
        f("metadataFailures", JSONUtils.mapToJSON(metadataUpdateFailures)),
        f("schedulingFailures", JSONUtils.mapToJSON(schedulingUpdateFailures))
      ));
    }
    return ok();
  }

  @POST
  @Path("bulk/conflicts")
  @RestQuery(name = "getBulkConflicts", description = "Checks if the current bulk update scheduling settings are in a conflict with another event", returnDescription = "Returns NO CONTENT if no event are in conflict within specified period or list of conflicting recordings in JSON", restParameters = {
    @RestParameter(name = "update", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of events and fields to update.")}, reponses = {
    @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
    @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "The events in the response body were not found. No events were updated."),
    @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "There is a conflict"),
    @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters")})
  public Response getBulkConflicts(@FormParam("update") final String updateJson) throws NotFoundException {
    final BulkUpdateUtil.BulkUpdateInstructions instructions;
    try {
      instructions = new BulkUpdateUtil.BulkUpdateInstructions(updateJson);
    } catch (IllegalArgumentException e) {
      return badRequest("Cannot parse bulk update instructions");
    }

    // Get all the events to check
    final Map<String, Optional<Event>> events = instructions.getEventIds().stream()
      .collect(Collectors.toMap(id -> id, id -> BulkUpdateUtil.getEvent(getIndexService(), getIndex(), id)));

    // Check for invalid (non-existing) event ids
    final Set<String> notFoundIds = events.entrySet().stream()
      .filter(e -> !e.getValue().isPresent())
      .map(Entry::getKey)
      .collect(Collectors.toSet());
    if (!notFoundIds.isEmpty()) {
      return notFoundJson(JSONUtils.setToJSON(notFoundIds));
    }

    final Map<String, List<JValue>> conflicts = new HashMap<>();
    events.values().forEach(e -> e.ifPresent(event -> {
      try {
        if (instructions.getScheduling() != null) {
          // Since we only have the start/end time, we have to add the correct date(s) for this event.
          final JSONObject scheduling = BulkUpdateUtil.addSchedulingDates(event, instructions.getScheduling());
          final Date start = Date.from(Instant.parse((String) scheduling.get(SCHEDULING_START_KEY)));
          final Date end = Date.from(Instant.parse((String) scheduling.get(SCHEDULING_END_KEY)));
          final String agentId = Optional.ofNullable((String) scheduling.get(SCHEDULING_AGENT_ID_KEY))
            .orElse(event.getAgentId());

          final List<JValue> currentConflicts = new ArrayList<>();

          // Check for conflicts between the events themselves
          events.values().forEach(oe -> oe.ifPresent(otherEvent -> {
            if (otherEvent.getIdentifier().equals(event.getIdentifier())) {
              // don't check event against itself
              return;
            }
            final JSONObject otherScheduling = BulkUpdateUtil.addSchedulingDates(otherEvent, instructions.getScheduling());
            final Date otherStart = Date.from(Instant.parse((String) otherScheduling.get(SCHEDULING_START_KEY)));
            final Date otherEnd = Date.from(Instant.parse((String) otherScheduling.get(SCHEDULING_END_KEY)));
            final String otherAgentId = Optional.ofNullable((String) otherScheduling.get(SCHEDULING_AGENT_ID_KEY))
              .orElse(otherEvent.getAgentId());
            if (!otherAgentId.equals(agentId)) {
              // different agent -> no conflict
              return;
            }
            if (start.before(otherEnd) && end.after(otherStart)) {
              // conflict
              currentConflicts.add(convertEventToConflictingObject(DateTimeSupport.toUTC(otherStart.getTime()),
                DateTimeSupport.toUTC(otherEnd.getTime()), otherEvent.getTitle()));
            }
          }));

          // Check for conflicts with other events from the database
          final List<MediaPackage> conflicting = getSchedulerService().findConflictingEvents(agentId, start, end)
            .stream()
            .filter(mp -> !events.keySet().contains(mp.getIdentifier().toString()))
            .collect(Collectors.toList());
          if (conflicting != null && !conflicting.isEmpty()) {
            currentConflicts.addAll(convertToConflictObjects(event.getIdentifier(), conflicting));
          }
          conflicts.put(event.getIdentifier(), currentConflicts);
        }
      } catch (SchedulerException | UnauthorizedException | SearchIndexException exception) {
        throw new RuntimeException(exception);
      }
    }));

    if (!conflicts.isEmpty()) {
      final List<JValue> responseJson = new ArrayList<>();
      conflicts.forEach((eventId, conflictingEvents) -> {
        if (!conflictingEvents.isEmpty()) {
          responseJson.add(obj(f("eventId", eventId), f("conflicts", arr(conflictingEvents))));
        }
      });
      if (!responseJson.isEmpty()) {
        return conflictJson(arr(responseJson));
      }
    }

    return noContent();
  }

  @PUT
  @Path("{eventId}/metadata")
  @RestQuery(name = "updateeventmetadata", description = "Update the passed metadata for the event with the given Id", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {
                  @RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of metadata to update") }, reponses = {
                          @RestResponse(description = "The metadata have been updated.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "Could not parse metadata.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "No content is returned.")
  public Response updateEventMetadata(@PathParam("eventId") String id, @FormParam("metadata") String metadataJSON)
          throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

    try {
      MetadataList metadataList = getIndexService().updateAllEventMetadata(id, metadataJSON, getIndex());
      return okJson(metadataList.toJSON());
    } catch (IllegalArgumentException e) {
      return badRequest(String.format("Event %s metadata can't be updated.: %s", id, e.getMessage()));
    }
  }

  @GET
  @Path("{eventId}/asset/assets.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getAssetList", description = "Returns the number of assets from each types as JSON", returnDescription = "The number of assets from each types as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the number of assets from each types as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getAssetList(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);
    MediaPackage mp = getIndexService().getEventMediapackage(optEvent.get());
    int attachments = mp.getAttachments().length;
    int catalogs = mp.getCatalogs().length;
    int media = mp.getTracks().length;
    int publications = mp.getPublications().length;
    return okJson(obj(f("attachments", v(attachments)), f("catalogs", v(catalogs)), f("media", v(media)),
            f("publications", v(publications))));
  }

  @GET
  @Path("{eventId}/asset/attachment/attachments.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getAttachmentsList", description = "Returns a list of attachments from the given event as JSON", returnDescription = "The list of attachments from the given event as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns a list of attachments from the given event as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getAttachmentsList(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);
    MediaPackage mp = getIndexService().getEventMediapackage(optEvent.get());
    return okJson(arr(getEventMediaPackageElements(mp.getAttachments())));
  }

  @GET
  @Path("{eventId}/asset/attachment/{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getAttachment", description = "Returns the details of an attachment from the given event and attachment id as JSON", returnDescription = "The details of an attachment from the given event and attachment id as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "id", description = "The attachment id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the details of an attachment from the given event and attachment id as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event or attachment with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getAttachment(@PathParam("eventId") String eventId, @PathParam("id") String id)
          throws NotFoundException, SearchIndexException, IndexServiceException {
    MediaPackage mp = getMediaPackageByEventId(eventId);

    Attachment attachment = mp.getAttachment(id);
    if (attachment == null)
      return notFound("Cannot find an attachment with id '%s'.", id);
    return okJson(attachmentToJSON(attachment));
  }

  @GET
  @Path("{eventId}/asset/catalog/catalogs.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getCatalogList", description = "Returns a list of catalogs from the given event as JSON", returnDescription = "The list of catalogs from the given event as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns a list of catalogs from the given event as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getCatalogList(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);
    MediaPackage mp = getIndexService().getEventMediapackage(optEvent.get());
    return okJson(arr(getEventMediaPackageElements(mp.getCatalogs())));
  }

  @GET
  @Path("{eventId}/asset/catalog/{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getCatalog", description = "Returns the details of a catalog from the given event and catalog id as JSON", returnDescription = "The details of a catalog from the given event and catalog id as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "id", description = "The catalog id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the details of a catalog from the given event and catalog id as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event or catalog with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getCatalog(@PathParam("eventId") String eventId, @PathParam("id") String id)
          throws NotFoundException, SearchIndexException, IndexServiceException {
    MediaPackage mp = getMediaPackageByEventId(eventId);

    Catalog catalog = mp.getCatalog(id);
    if (catalog == null)
      return notFound("Cannot find a catalog with id '%s'.", id);
    return okJson(catalogToJSON(catalog));
  }

  @GET
  @Path("{eventId}/asset/media/media.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getMediaList", description = "Returns a list of media from the given event as JSON", returnDescription = "The list of media from the given event as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns a list of media from the given event as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getMediaList(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);
    MediaPackage mp = getIndexService().getEventMediapackage(optEvent.get());
    return okJson(arr(getEventMediaPackageElements(mp.getTracks())));
  }

  @GET
  @Path("{eventId}/asset/media/{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getMedia", description = "Returns the details of a media from the given event and media id as JSON", returnDescription = "The details of a media from the given event and media id as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "id", description = "The media id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns the media of a catalog from the given event and media id as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event or media with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getMedia(@PathParam("eventId") String eventId, @PathParam("id") String id)
          throws NotFoundException, SearchIndexException, IndexServiceException {
    MediaPackage mp = getMediaPackageByEventId(eventId);

    Track track = mp.getTrack(id);
    if (track == null)
      return notFound("Cannot find media with id '%s'.", id);
    return okJson(trackToJSON(track));
  }

  @GET
  @Path("{eventId}/asset/publication/publications.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getPublicationList", description = "Returns a list of publications from the given event as JSON", returnDescription = "The list of publications from the given event as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns a list of publications from the given event as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getPublicationList(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);
    MediaPackage mp = getIndexService().getEventMediapackage(optEvent.get());
    return okJson(arr(getEventPublications(mp.getPublications())));
  }

  @GET
  @Path("{eventId}/asset/publication/{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getPublication", description = "Returns the details of a publication from the given event and publication id as JSON", returnDescription = "The details of a publication from the given event and publication id as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "id", description = "The publication id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the publication of a catalog from the given event and publication id as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event or publication with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getPublication(@PathParam("eventId") String eventId, @PathParam("id") String id)
          throws NotFoundException, SearchIndexException, IndexServiceException {
    MediaPackage mp = getMediaPackageByEventId(eventId);

    Publication publication = null;
    for (Publication p : mp.getPublications()) {
      if (id.equals(p.getIdentifier())) {
        publication = p;
        break;
      }
    }

    if (publication == null)
      return notFound("Cannot find publication with id '%s'.", id);
    return okJson(publicationToJSON(publication));
  }

  @GET
  @Path("{eventId}/workflows.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventworkflows", description = "Returns all the data related to the workflows tab in the event details modal as JSON", returnDescription = "All the data related to the event workflows tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event workflows tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventWorkflows(@PathParam("eventId") String id)
          throws UnauthorizedException, SearchIndexException, JobEndpointException {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

    try {
      if (!optEvent.get().hasRecordingStarted()) {
        List<Field> fields = new ArrayList<Field>();
        Map<String, String> workflowConfig = getSchedulerService().getWorkflowConfig(id);
        for (Entry<String, String> entry : workflowConfig.entrySet()) {
          fields.add(f(entry.getKey(), v(entry.getValue(), Jsons.BLANK)));
        }

        Map<String, String> agentConfiguration = getSchedulerService().getCaptureAgentConfiguration(id);
        return okJson(obj(f("workflowId", v(agentConfiguration.get(CaptureParameters.INGEST_WORKFLOW_DEFINITION), Jsons.BLANK)),
                f("configuration", obj(fields))));
      } else {
        return okJson(getJobService().getTasksAsJSON(new WorkflowQuery().withMediaPackage(id)));
      }
    } catch (NotFoundException e) {
      return notFound("Cannot find workflows for event %s", id);
    } catch (SchedulerException e) {
      logger.error("Unable to get workflow data for event with id {}", id);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{eventId}/workflows")
  @RestQuery(name = "updateEventWorkflow", description = "Update the workflow configuration for the scheduled event with the given id", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {
                  @RestParameter(name = "configuration", isRequired = true, description = "The workflow configuration as JSON", type = RestParameter.Type.TEXT) }, reponses = {
                          @RestResponse(description = "Request executed succesfully", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "The method does not retrun any content.")
  public Response updateEventWorkflow(@PathParam("eventId") String id, @FormParam("configuration") String configuration)
          throws SearchIndexException, UnauthorizedException {
    Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

    if (!optEvent.get().hasRecordingStarted()) {
      try {

        JSONObject configJSON;
        try {
          configJSON = (JSONObject) new JSONParser().parse(configuration);
        } catch (Exception e) {
          logger.warn("Unable to parse the workflow configuration {}", configuration);
          return badRequest();
        }

        Opt<Map<String, String>> caMetadataOpt = Opt.none();
        Opt<Map<String, String>> workflowConfigOpt = Opt.none();

        String workflowId = (String) configJSON.get("id");
        Map<String, String> caMetadata = new HashMap<>(getSchedulerService().getCaptureAgentConfiguration(id));
        if (!workflowId.equals(caMetadata.get(CaptureParameters.INGEST_WORKFLOW_DEFINITION))) {
          caMetadata.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, workflowId);
          caMetadataOpt = Opt.some(caMetadata);
        }

        Map<String, String> workflowConfig = new HashMap<>((JSONObject) configJSON.get("configuration"));
        Map<String, String> oldWorkflowConfig = new HashMap<>(getSchedulerService().getWorkflowConfig(id));
        if (!oldWorkflowConfig.equals(workflowConfig))
          workflowConfigOpt = Opt.some(workflowConfig);

        if (caMetadataOpt.isNone() && workflowConfigOpt.isNone())
          return Response.noContent().build();

        checkAgentAccessForAgent(optEvent.get().getAgentId());

        getSchedulerService().updateEvent(id, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
                Opt.<Set<String>> none(), Opt.<MediaPackage> none(), workflowConfigOpt, caMetadataOpt,
                Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
        return Response.noContent().build();
      } catch (NotFoundException e) {
        return notFound("Cannot find event %s in scheduler service", id);
      } catch (SchedulerException e) {
        logger.error("Unable to update scheduling workflow data for event with id {}", id);
        throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
      }
    } else {
      return badRequest(String.format("Event %s workflow can not be updated as the recording already started.", id));
    }
  }

  @GET
  @Path("{eventId}/workflows/{workflowId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventworkflow", description = "Returns all the data related to the single workflow tab in the event details modal as JSON", returnDescription = "All the data related to the event singe workflow tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event single workflow tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventWorkflow(@PathParam("eventId") String eventId, @PathParam("workflowId") String workflowId)
          throws JobEndpointException, SearchIndexException {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    long workflowInstanceId;
    try {
      workflowId = StringUtils.remove(workflowId, ".json");
      workflowInstanceId = Long.parseLong(workflowId);
    } catch (Exception e) {
      logger.warn("Unable to parse workflow id {}", workflowId);
      return RestUtil.R.badRequest();
    }

    try {
      return okJson(getJobService().getTasksAsJSON(workflowInstanceId));
    } catch (NotFoundException e) {
      return notFound("Cannot find workflow  %s", workflowId);
    }
  }

  @GET
  @Path("{eventId}/workflows/{workflowId}/operations.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventoperations", description = "Returns all the data related to the workflow/operations tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/opertations tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event workflow/operations tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventOperations(@PathParam("eventId") String eventId, @PathParam("workflowId") String workflowId)
          throws JobEndpointException, SearchIndexException {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    long workflowInstanceId;
    try {
      workflowInstanceId = Long.parseLong(workflowId);
    } catch (Exception e) {
      logger.warn("Unable to parse workflow id {}", workflowId);
      return RestUtil.R.badRequest();
    }

    try {
      return okJson(getJobService().getOperationsAsJSON(workflowInstanceId));
    } catch (NotFoundException e) {
      return notFound("Cannot find workflow %s", workflowId);
    }
  }

  @GET
  @Path("{eventId}/workflows/{workflowId}/operations/{operationPosition}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventoperation", description = "Returns all the data related to the workflow/operation tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/opertation tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "operationPosition", description = "The operation position", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event workflow/operation tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Unable to parse workflowId or operationPosition", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "No operation with these identifiers was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventOperation(@PathParam("eventId") String eventId, @PathParam("workflowId") String workflowId,
          @PathParam("operationPosition") Integer operationPosition) throws JobEndpointException, SearchIndexException {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    long workflowInstanceId;
    try {
      workflowInstanceId = Long.parseLong(workflowId);
    } catch (Exception e) {
      logger.warn("Unable to parse workflow id {}", workflowId);
      return RestUtil.R.badRequest();
    }

    try {
      return okJson(getJobService().getOperationAsJSON(workflowInstanceId, operationPosition));
    } catch (NotFoundException e) {
      return notFound("Cannot find workflow %s", workflowId);
    }
  }

  @GET
  @Path("{eventId}/workflows/{workflowId}/errors.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventerrors", description = "Returns all the data related to the workflow/errors tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/errors tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event workflow/errors tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventErrors(@PathParam("eventId") String eventId, @PathParam("workflowId") String workflowId,
          @Context HttpServletRequest req) throws JobEndpointException, SearchIndexException {
    // the call to #getEvent should make sure that the calling user has access rights to the workflow
    // FIXME since there is no dependency between the event and the workflow (the fetched event is
    // simply ignored) an attacker can get access by using an event he owns and a workflow ID of
    // someone else.
    for (final Event ignore : getIndexService().getEvent(eventId, getIndex())) {
      final long workflowIdLong;
      try {
        workflowIdLong = Long.parseLong(workflowId);
      } catch (Exception e) {
        logger.warn("Unable to parse workflow id {}", workflowId);
        return RestUtil.R.badRequest();
      }
      try {
        return okJson(getJobService().getIncidentsAsJSON(workflowIdLong, req.getLocale(), true));
      } catch (NotFoundException e) {
        return notFound("Cannot find the incident for the workflow %s", workflowId);
      }
    }
    return notFound("Cannot find an event with id '%s'.", eventId);
  }

  @GET
  @Path("{eventId}/workflows/{workflowId}/errors/{errorId}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventerror", description = "Returns all the data related to the workflow/error tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/error tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "errorId", description = "The error id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Returns all the data related to the event workflow/error tab as JSON", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Unable to parse workflowId", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventError(@PathParam("eventId") String eventId, @PathParam("workflowId") String workflowId,
          @PathParam("errorId") String errorId, @Context HttpServletRequest req)
                  throws JobEndpointException, SearchIndexException {
    // the call to #getEvent should make sure that the calling user has access rights to the workflow
    // FIXME since there is no dependency between the event and the workflow (the fetched event is
    // simply ignored) an attacker can get access by using an event he owns and a workflow ID of
    // someone else.
    for (Event ignore : getIndexService().getEvent(eventId, getIndex())) {
      final long errorIdLong;
      try {
        errorIdLong = Long.parseLong(errorId);
      } catch (Exception e) {
        logger.warn("Unable to parse error id {}", errorId);
        return RestUtil.R.badRequest();
      }
      try {
        return okJson(getJobService().getIncidentAsJSON(errorIdLong, req.getLocale()));
      } catch (NotFoundException e) {
        return notFound("Cannot find the incident %s", errorId);
      }
    }
    return notFound("Cannot find an event with id '%s'.", eventId);
  }

  @GET
  @Path("{eventId}/access.json")
  @SuppressWarnings("unchecked")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEventAccessInformation", description = "Get the access information of an event", returnDescription = "The access information", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."),
                  @RestResponse(responseCode = SC_OK, description = "The access information ") })
  public Response getEventAccessInformation(@PathParam("eventId") String eventId) throws Exception {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    // Add all available ACLs to the response
    JSONArray systemAclsJson = new JSONArray();
    List<ManagedAcl> acls = getAclService().getAcls();
    for (ManagedAcl acl : acls) {
      systemAclsJson.add(AccessInformationUtil.serializeManagedAcl(acl));
    }

    // Get the episode ACL
    final TransitionQuery q = TransitionQuery.query().withId(eventId).withScope(AclScope.Episode);
    List<EpisodeACLTransition> episodeTransistions;
    JSONArray transitionsJson = new JSONArray();
    try {
      episodeTransistions = getAclService().getTransitions(q).getEpisodeTransistions();
      for (EpisodeACLTransition trans : episodeTransistions) {
        transitionsJson.add(AccessInformationUtil.serializeEpisodeACLTransition(trans));
      }
    } catch (AclServiceException e) {
      logger.error(
              "There was an error while trying to get the ACL transitions for series '{}' from the ACL service: {}",
              eventId, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    AccessControlList activeAcl = new AccessControlList();
    try {
      if (optEvent.get().getAccessPolicy() != null)
        activeAcl = AccessControlParser.parseAcl(optEvent.get().getAccessPolicy());
    } catch (Exception e) {
      logger.error("Unable to parse access policy because: {}", ExceptionUtils.getStackTrace(e));
    }
    Option<ManagedAcl> currentAcl = AccessInformationUtil.matchAcls(acls, activeAcl);

    JSONObject episodeAccessJson = new JSONObject();
    episodeAccessJson.put("current_acl", currentAcl.isSome() ? currentAcl.get().getId() : 0L);
    episodeAccessJson.put("acl", AccessControlParser.toJsonSilent(activeAcl));
    episodeAccessJson.put("privileges", AccessInformationUtil.serializePrivilegesByRole(activeAcl));
    episodeAccessJson.put("transitions", transitionsJson);
    if (StringUtils.isNotBlank(optEvent.get().getWorkflowState())
            && WorkflowUtil.isActive(WorkflowInstance.WorkflowState.valueOf(optEvent.get().getWorkflowState())))
      episodeAccessJson.put("locked", true);

    JSONObject jsonReturnObj = new JSONObject();
    jsonReturnObj.put("episode_access", episodeAccessJson);
    jsonReturnObj.put("system_acls", systemAclsJson);

    return Response.ok(jsonReturnObj.toString()).build();
  }

  @POST
  @Path("{eventId}/transitions")
  @RestQuery(name = "addEventTransition", description = "Adds an ACL transition to an event", returnDescription = "The method doesn't return any content", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING) }, restParameters = {
                  @RestParameter(name = "transition", isRequired = true, description = "The transition (JSON object) to add", type = RestParameter.Type.TEXT) }, reponses = {
                          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required params were missing in the request."),
                          @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content"),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found.") })
  public Response addEventTransition(@PathParam("eventId") String eventId,
          @FormParam("transition") String transitionStr) throws SearchIndexException {
    if (StringUtils.isBlank(eventId) || StringUtils.isBlank(transitionStr))
      return RestUtil.R.badRequest("Missing parameters");

    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      final org.codehaus.jettison.json.JSONObject t = new org.codehaus.jettison.json.JSONObject(transitionStr);
      Option<ConfiguredWorkflowRef> workflowRef;
      if (t.has("workflow_id"))
        workflowRef = Option.some(ConfiguredWorkflowRef.workflow(t.getString("workflow_id")));
      else
        workflowRef = Option.none();

      Option<Long> managedAclId;
      if (t.has("acl_id"))
        managedAclId = Option.some(t.getLong("acl_id"));
      else
        managedAclId = Option.none();

      getAclService().addEpisodeTransition(eventId, managedAclId,
              new Date(DateTimeSupport.fromUTC(t.getString("application_date"))), workflowRef);
      return Response.noContent().build();
    } catch (AclServiceException e) {
      logger.error("Error while trying to get ACL transitions for event '{}' from ACL service: {}", eventId, e);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    } catch (JSONException e) {
      return RestUtil.R.badRequest("The transition object is not valid");
    } catch (IllegalStateException e) {
      // That should never happen
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    } catch (ParseException e) {
      return RestUtil.R.badRequest("The date could not be parsed");
    }
  }

  @PUT
  @Path("{eventId}/transitions/{transitionId}")
  @RestQuery(name = "updateEventTransition", description = "Updates an ACL transition of an event", returnDescription = "The method doesn't return any content", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING),
          @RestParameter(name = "transitionId", isRequired = true, description = "The transition identifier", type = RestParameter.Type.INTEGER) }, restParameters = {
                  @RestParameter(name = "transition", isRequired = true, description = "The updated transition (JSON object)", type = RestParameter.Type.TEXT) }, reponses = {
                          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required params were missing in the request."),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event or transtion has not been found."),
                          @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content") })
  public Response updateEventTransition(@PathParam("eventId") String eventId,
          @PathParam("transitionId") long transitionId, @FormParam("transition") String transitionStr)
                  throws NotFoundException, SearchIndexException {
    if (StringUtils.isBlank(transitionStr))
      return RestUtil.R.badRequest("Missing parameters");

    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      final org.codehaus.jettison.json.JSONObject t = new org.codehaus.jettison.json.JSONObject(transitionStr);
      Option<ConfiguredWorkflowRef> workflowRef;
      if (t.has("workflow_id"))
        workflowRef = Option.some(ConfiguredWorkflowRef.workflow(t.getString("workflow_id")));
      else
        workflowRef = Option.none();

      Option<Long> managedAclId;
      if (t.has("acl_id"))
        managedAclId = Option.some(t.getLong("acl_id"));
      else
        managedAclId = Option.none();

      getAclService().updateEpisodeTransition(transitionId, managedAclId,
              new Date(DateTimeSupport.fromUTC(t.getString("application_date"))), workflowRef);
      return Response.noContent().build();
    } catch (JSONException e) {
      return RestUtil.R.badRequest("The transition object is not valid");
    } catch (IllegalStateException e) {
      // That should never happen
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    } catch (AclServiceException e) {
      logger.error("Unable to update transtion {} of event {}: {}",
              transitionId, eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    } catch (ParseException e) {
      // That should never happen
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  // MH-12085 Add manually uploaded assets, multipart file upload has to be a POST
  @POST
  @Path("{eventId}/assets")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "updateAssets", description = "Update or create an asset for the eventId by the given metadata as JSON and files in the body",
  pathParameters = {
  @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) },
  restParameters = {
  @RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of asset metadata") },
  reponses = {
  @RestResponse(description = "The asset has been added.", responseCode = HttpServletResponse.SC_OK),
  @RestResponse(description = "Could not add asset, problem with the metadata or files.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
  @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) },
  returnDescription = "The workflow identifier")
  public Response updateAssets(@PathParam("eventId") final String eventId,
          @Context HttpServletRequest request)  throws Exception {
    try {
      MediaPackage mp = getMediaPackageByEventId(eventId);
      String result = getIndexService().updateEventAssets(mp, request);
      return Response.status(Status.CREATED).entity(result).build();
    }  catch (NotFoundException e) {
      return notFound("Cannot find an event with id '%s'.", eventId);
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest(e.getMessage());
    } catch (Exception e) {
      return RestUtil.R.serverError();
    }
  }

  @PUT
  @Path("{eventId}/optout/{optout}")
  @RestQuery(name = "updateEventOptoutStatus", description = "Updates an event's opt out status.", returnDescription = "The method doesn't return any content", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING),
          @RestParameter(name = "optout", isRequired = true, description = "True or false, true to opt out of this recording.", type = RestParameter.Type.BOOLEAN) }, restParameters = {}, reponses = {
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "The event has not been found"),
                  @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action"),
                  @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content") })
  public Response updateEventOptOut(@PathParam("eventId") String eventId, @PathParam("optout") boolean optout)
          throws NotFoundException, UnauthorizedException {
    try {
      checkAgentAccessForEvent(eventId);
      getIndexService().changeOptOutStatus(eventId, optout, getIndex());
      return Response.noContent().build();
    } catch (SchedulerException e) {
      logger.error("Unable to updated opt out status for event with id {}", eventId);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    } catch (SearchIndexException e) {
      logger.error("Unable to get event with id {}", eventId);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("optouts")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "changeOptOuts", description = "Change the opt out status of many events", returnDescription = "A JSON array listing which events were or were not opted out.", restParameters = {
          @RestParameter(name = "eventIds", description = "A JSON array of ids of the events to opt out or in", defaultValue = "[]", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "optout", description = "Whether to opt out or not either true or false.", defaultValue = "false", isRequired = true, type = RestParameter.Type.BOOLEAN), }, reponses = {
                  @RestResponse(description = "Returns a JSON object with the results for the different opted out or in elements such as ok, notFound or error.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Unable to parse boolean value to opt out, or parse JSON array of opt out events", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response changeOptOuts(@FormParam("optout") boolean optout, @FormParam("eventIds") String eventIds) {
    JSONParser parser = new JSONParser();
    JSONArray eventIdsArray;
    try {
      eventIdsArray = (JSONArray) parser.parse(eventIds);
    } catch (org.json.simple.parser.ParseException e) {
      logger.warn("Unable to parse event ids {} : {}", eventIds, ExceptionUtils.getStackTrace(e));
      return Response.status(Status.BAD_REQUEST).build();
    } catch (NullPointerException e) {
      logger.warn("Unable to parse event ids because it was null {}", eventIds);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (ClassCastException e) {
      logger.warn("Unable to parse event ids because it was the wrong class {} : {}", eventIds,
              ExceptionUtils.getStackTrace(e));
      return Response.status(Status.BAD_REQUEST).build();
    }

    BulkOperationResult result = new BulkOperationResult();

    for (Object idObject : eventIdsArray) {
      String eventId = idObject.toString();

      try {
        getIndexService().changeOptOutStatus(eventId, optout, getIndex());
        result.addOk(eventId);
      } catch (NotFoundException e) {
        result.addNotFound(eventId);
      } catch (Exception e) {
        logger.error("Could not update opt out status of event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
        result.addServerError(eventId);
      }
    }
    return Response.ok(result.toJson()).build();
  }

  @DELETE
  @Path("{eventId}/transitions/{transitionId}")
  @RestQuery(name = "deleteEventTransition", description = "Deletes an ACL transition from an event", returnDescription = "The method doesn't return any content", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The series identifier", type = RestParameter.Type.STRING),
          @RestParameter(name = "transitionId", isRequired = true, description = "The transition identifier", type = RestParameter.Type.INTEGER) }, reponses = {
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event or the transition has not been found."),
                  @RestResponse(responseCode = SC_NO_CONTENT, description = "The method does not return any content") })
  public Response deleteEventTransition(@PathParam("eventId") String eventId,
          @PathParam("transitionId") long transitionId) throws NotFoundException, SearchIndexException {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      getAclService().deleteEpisodeTransition(transitionId);
      return Response.noContent().build();
    } catch (AclServiceException e) {
      logger.error("Error while trying to delete transition '{}' from event '{}': {}",
              transitionId, eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("new/metadata")
  @RestQuery(name = "getNewMetadata", description = "Returns all the data related to the metadata tab in the new event modal as JSON", returnDescription = "All the data related to the event metadata tab as JSON", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Returns all the data related to the event metadata tab as JSON") })
  public Response getNewMetadata() {
    MetadataList metadataList = getIndexService().getMetadataListWithAllEventCatalogUIAdapters();
    Opt<MetadataCollection> optMetadataByAdapter = metadataList
            .getMetadataByAdapter(getIndexService().getCommonEventCatalogUIAdapter());
    if (optMetadataByAdapter.isSome()) {
      MetadataCollection collection = optMetadataByAdapter.get();
      if (collection.getOutputFields().containsKey(DublinCore.PROPERTY_CREATED.getLocalName()))
        collection.removeField(collection.getOutputFields().get(DublinCore.PROPERTY_CREATED.getLocalName()));
      if (collection.getOutputFields().containsKey("duration"))
        collection.removeField(collection.getOutputFields().get("duration"));
      if (collection.getOutputFields().containsKey(DublinCore.PROPERTY_IDENTIFIER.getLocalName()))
        collection.removeField(collection.getOutputFields().get(DublinCore.PROPERTY_IDENTIFIER.getLocalName()));
      if (collection.getOutputFields().containsKey(DublinCore.PROPERTY_SOURCE.getLocalName()))
        collection.removeField(collection.getOutputFields().get(DublinCore.PROPERTY_SOURCE.getLocalName()));
      if (collection.getOutputFields().containsKey("startDate"))
        collection.removeField(collection.getOutputFields().get("startDate"));
      if (collection.getOutputFields().containsKey("startTime"))
        collection.removeField(collection.getOutputFields().get("startTime"));
      if (collection.getOutputFields().containsKey("location"))
        collection.removeField(collection.getOutputFields().get("location"));
      metadataList.add(getIndexService().getCommonEventCatalogUIAdapter(), collection);
    }
    return okJson(metadataList.toJSON());
  }

  @GET
  @Path("new/processing")
  @RestQuery(name = "getNewProcessing", description = "Returns all the data related to the processing tab in the new event modal as JSON", returnDescription = "All the data related to the event processing tab as JSON", restParameters = {
          @RestParameter(name = "tags", isRequired = false, description = "A comma separated list of tags to filter the workflow definitions", type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(responseCode = SC_OK, description = "Returns all the data related to the event processing tab as JSON") })
  public Response getNewProcessing(@QueryParam("tags") String tagsString) {
    List<String> tags = RestUtil.splitCommaSeparatedParam(Option.option(tagsString)).value();

    List<JValue> workflows = new ArrayList<>();
    try {
      List<WorkflowDefinition> workflowsDefinitions = getWorkflowService().listAvailableWorkflowDefinitions();
      for (WorkflowDefinition wflDef : workflowsDefinitions) {
        if (wflDef.containsTag(tags)) {

          workflows.add(obj(f("id", v(wflDef.getId())), f("title", v(nul(wflDef.getTitle()).getOr(""))),
                  f("description", v(nul(wflDef.getDescription()).getOr(""))),
                  f("displayOrder", v(wflDef.getDisplayOrder())),
                  f("configuration_panel", v(nul(wflDef.getConfigurationPanel()).getOr("")))));
        }
      }
    } catch (WorkflowDatabaseException e) {
      logger.error("Unable to get available workflow definitions: {}", ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    JValue data = obj(f("workflows",arr(workflows)), f("default_workflow_id",v(defaultWorkflowDefinionId,Jsons.NULL)));

    return okJson(data);
  }

  @POST
  @Path("new/conflicts")
  @RestQuery(name = "checkNewConflicts", description = "Checks if the current scheduler parameters are in a conflict with another event", returnDescription = "Returns NO CONTENT if no event are in conflict within specified period or list of conflicting recordings in JSON", restParameters = {
          @RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "There is a conflict"),
                  @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters") })
  public Response getNewConflicts(@FormParam("metadata") String metadata) throws NotFoundException {
    if (StringUtils.isBlank(metadata)) {
      logger.warn("Metadata is not specified");
      return Response.status(Status.BAD_REQUEST).build();
    }

    JSONParser parser = new JSONParser();
    JSONObject metadataJson;
    try {
      metadataJson = (JSONObject) parser.parse(metadata);
    } catch (Exception e) {
      logger.warn("Unable to parse metadata {}", metadata);
      return RestUtil.R.badRequest("Unable to parse metadata");
    }

    String device;
    String startDate;
    String endDate;
    try {
      device = (String) metadataJson.get("device");
      startDate = (String) metadataJson.get("start");
      endDate = (String) metadataJson.get("end");
    } catch (Exception e) {
      logger.warn("Unable to parse metadata {}", metadata);
      return RestUtil.R.badRequest("Unable to parse metadata");
    }

    if (StringUtils.isBlank(device) || StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)) {
      logger.warn("Either device, start date or end date were not specified");
      return Response.status(Status.BAD_REQUEST).build();
    }

    Date start;
    try {
      start = new Date(DateTimeSupport.fromUTC(startDate));
    } catch (Exception e) {
      logger.warn("Unable to parse start date {}", startDate);
      return RestUtil.R.badRequest("Unable to parse start date");
    }

    Date end;
    try {
      end = new Date(DateTimeSupport.fromUTC(endDate));
    } catch (Exception e) {
      logger.warn("Unable to parse end date {}", endDate);
      return RestUtil.R.badRequest("Unable to parse end date");
    }

    String rruleString = (String) metadataJson.get("rrule");

    RRule rrule = null;
    TimeZone timeZone = TimeZone.getDefault();
    String durationString = null;
    if (StringUtils.isNotEmpty(rruleString)) {
      try {
        rrule = new RRule(rruleString);
        rrule.validate();
      } catch (Exception e) {
        logger.warn("Unable to parse rrule {}: {}", rruleString, e.getMessage());
        return Response.status(Status.BAD_REQUEST).build();
      }

      durationString = (String) metadataJson.get("duration");
      if (StringUtils.isBlank(durationString)) {
        logger.warn("If checking recurrence, must include duration.");
        return Response.status(Status.BAD_REQUEST).build();
      }

      Agent agent = getCaptureAgentStateService().getAgent(device);
      String timezone = agent.getConfiguration().getProperty("capture.device.timezone");
      if (StringUtils.isBlank(timezone)) {
        timezone = TimeZone.getDefault().getID();
        logger.warn("No 'capture.device.timezone' set on agent {}. The default server timezone {} will be used.",
                device, timezone);
      }
      timeZone = TimeZone.getTimeZone(timezone);
    }

    String eventId = (String) metadataJson.get("id");

    try {
      List<MediaPackage> events = null;
      if (StringUtils.isNotEmpty(rruleString)) {
        events = getSchedulerService().findConflictingEvents(device, rrule, start, end, Long.parseLong(durationString),
                timeZone);
      } else {
        events = getSchedulerService().findConflictingEvents(device, start, end);
      }
      if (!events.isEmpty()) {
        final List<JValue> eventsJSON = convertToConflictObjects(eventId, events);
        if (!eventsJSON.isEmpty())
          return conflictJson(arr(eventsJSON));
      }
      return Response.noContent().build();
    } catch (Exception e) {
      logger.error("Unable to find conflicting events for {}, {}, {}: {}",
              device, startDate, endDate, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }
  }

  private List<JValue> convertToConflictObjects(final String eventId, final List<MediaPackage> events) throws SearchIndexException {
    final List<JValue> eventsJSON = new ArrayList<>();
    for (final MediaPackage event : events) {
      final Opt<Event> eventOpt = getIndexService().getEvent(event.getIdentifier().compact(), getIndex());
      if (eventOpt.isSome()) {
        final Event e = eventOpt.get();
        if (StringUtils.isNotEmpty(eventId) && eventId.equals(e.getIdentifier())) {
          continue;
        }
        eventsJSON.add(convertEventToConflictingObject(e.getTechnicalStartTime(), e.getTechnicalEndTime(), e.getTitle()));
      } else {
        logger.warn("Index out of sync! Conflicting event catalog {} not found on event index!",
          event.getIdentifier().compact());
      }
    }
    return eventsJSON;
  }

  private JValue convertEventToConflictingObject(final String start, final String end, final String title) {
    return obj(
      f("start", v(start)),
      f("end", v(end)),
      f("title", v(title))
    );
  }

  @POST
  @Path("/new")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "createNewEvent", description = "Creates a new event by the given metadata as JSON and the files in the body", returnDescription = "The workflow identifier", restParameters = {
          @RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Event sucessfully added"),
                  @RestResponse(responseCode = SC_BAD_REQUEST, description = "If the metadata is not set or couldn't be parsed") })
  public Response createNewEvent(@Context HttpServletRequest request) {
    try {
      String result = getIndexService().createEvent(request);
      return Response.status(Status.CREATED).entity(result).build();
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest(e.getMessage());
    } catch (Exception e) {
      return RestUtil.R.serverError();
    }
  }

  @GET
  @Path("events.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getevents", description = "Returns all the events as JSON", returnDescription = "All the events as JSON", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING),
          @RestParameter(name = "sort", description = "The order instructions used to sort the query result. Must be in the form '<field name>:(ASC|DESC)'", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page.", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The page number.", isRequired = false, type = RestParameter.Type.INTEGER) }, reponses = {
                  @RestResponse(description = "Returns all events as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response getEvents(@QueryParam("id") String id, @QueryParam("commentReason") String reasonFilter,
          @QueryParam("commentResolution") String resolutionFilter, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {

    Option<Integer> optLimit = Option.option(limit);
    Option<Integer> optOffset = Option.option(offset);
    Option<String> optSort = Option.option(trimToNull(sort));
    ArrayList<JValue> eventsList = new ArrayList<>();
    EventSearchQuery query = new EventSearchQuery(getSecurityService().getOrganization().getId(),
            getSecurityService().getUser());

    // If the limit is set to 0, this is not taken into account
    if (optLimit.isSome() && limit == 0) {
      optLimit = Option.none();
    }

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      if (EventListQuery.FILTER_PRESENTERS_BIBLIOGRAPHIC_NAME.equals(name))
        query.withPresenter(filters.get(name));
      if (EventListQuery.FILTER_PRESENTERS_TECHNICAL_NAME.equals(name))
        query.withTechnicalPresenters(filters.get(name));
      if (EventListQuery.FILTER_CONTRIBUTORS_NAME.equals(name))
        query.withContributor(filters.get(name));
      if (EventListQuery.FILTER_LOCATION_NAME.equals(name))
        query.withLocation(filters.get(name));
      if (EventListQuery.FILTER_AGENT_NAME.equals(name))
        query.withAgentId(filters.get(name));
      if (EventListQuery.FILTER_TEXT_NAME.equals(name))
        query.withText(QueryPreprocessor.sanitize(filters.get(name)));
      if (EventListQuery.FILTER_SERIES_NAME.equals(name))
        query.withSeriesId(filters.get(name));
      if (EventListQuery.FILTER_STATUS_NAME.equals(name))
        query.withEventStatus(filters.get(name));
      if (EventListQuery.FILTER_OPTEDOUT_NAME.equals(name))
        query.withOptedOut(Boolean.parseBoolean(filters.get(name)));
      if (EventListQuery.FILTER_REVIEW_STATUS_NAME.equals(name))
        query.withReviewStatus(filters.get(name));
      if (EventListQuery.FILTER_COMMENTS_NAME.equals(name)) {
        switch (Comments.valueOf(filters.get(name))) {
          case NONE:
            query.withComments(false);
            break;
          case OPEN:
            query.withOpenComments(true);
            break;
          case RESOLVED:
            query.withComments(true);
            query.withOpenComments(false);
            break;
          default:
            logger.info("Unknown comment {}", filters.get(name));
            return Response.status(SC_BAD_REQUEST).build();
        }
      }
      if (EventListQuery.FILTER_STARTDATE_NAME.equals(name)) {
        try {
          Tuple<Date, Date> fromAndToCreationRange = RestUtils.getFromAndToDateRange(filters.get(name));
          query.withTechnicalStartFrom(fromAndToCreationRange.getA());
          query.withTechnicalStartTo(fromAndToCreationRange.getB());
        } catch (IllegalArgumentException e) {
          return RestUtil.R.badRequest(e.getMessage());
        }
      }
    }

    if (optSort.isSome()) {
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      for (SortCriterion criterion : sortCriteria) {
        switch (criterion.getFieldName()) {
          case EventIndexSchema.TITLE:
            query.sortByTitle(criterion.getOrder());
            break;
          case EventIndexSchema.PRESENTER:
            query.sortByPresenter(criterion.getOrder());
            break;
          case EventIndexSchema.TECHNICAL_START:
          case "technical_date":
            query.sortByTechnicalStartDate(criterion.getOrder());
            break;
          case EventIndexSchema.TECHNICAL_END:
            query.sortByTechnicalEndDate(criterion.getOrder());
            break;
          case EventIndexSchema.PUBLICATION:
            query.sortByPublicationIgnoringInternal(criterion.getOrder());
            break;
          case EventIndexSchema.START_DATE:
          case "date":
            query.sortByStartDate(criterion.getOrder());
            break;
          case EventIndexSchema.END_DATE:
            query.sortByEndDate(criterion.getOrder());
            break;
          case EventIndexSchema.SERIES_NAME:
            query.sortBySeriesName(criterion.getOrder());
            break;
          case EventIndexSchema.LOCATION:
            query.sortByLocation(criterion.getOrder());
            break;
          case EventIndexSchema.EVENT_STATUS:
            query.sortByEventStatus(criterion.getOrder());
            break;
          default:
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
      }
    }

    // TODO: Add the comment resolution filter to the query
    EventCommentsListProvider.RESOLUTION resolution = null;
    if (StringUtils.isNotBlank(resolutionFilter)) {
      try {
        resolution = EventCommentsListProvider.RESOLUTION.valueOf(resolutionFilter);
      } catch (Exception e) {
        logger.warn("Unable to parse comment resolution filter {}", resolutionFilter);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    if (optLimit.isSome())
      query.withLimit(optLimit.get());
    if (optOffset.isSome())
      query.withOffset(offset);
    // TODO: Add other filters to the query

    SearchResult<Event> results = null;
    try {
      results = getIndex().getByQuery(query);
    } catch (SearchIndexException e) {
      logger.error("The admin UI Search Index was not able to get the events list:", e);
      return RestUtil.R.serverError();
    }

    // If the results list if empty, we return already a response.
    if (results.getPageSize() == 0) {
      logger.debug("No events match the given filters.");
      return okJsonList(eventsList, nul(offset).getOr(0), nul(limit).getOr(0), 0);
    }

    for (SearchResultItem<Event> item : results.getItems()) {
      Event source = item.getSource();
      source.updatePreview(getAdminUIConfiguration().getPreviewSubtype());
      eventsList.add(eventToJSON(source));
    }

    return okJsonList(eventsList, nul(offset).getOr(0), nul(limit).getOr(0), results.getHitCount());
  }

  // --

  private MediaPackage getMediaPackageByEventId(String eventId)
          throws SearchIndexException, NotFoundException, IndexServiceException {
    Opt<Event> optEvent = getIndexService().getEvent(eventId, getIndex());
    if (optEvent.isNone())
      throw new NotFoundException(format("Cannot find an event with id '%s'.", eventId));
    return getIndexService().getEventMediapackage(optEvent.get());
  }

  private URI getCommentUrl(String eventId, long commentId) {
    return UrlSupport.uri(serverUrl, eventId, "comment", Long.toString(commentId));
  }

  private JValue eventToJSON(Event event) {
    List<Field> fields = new ArrayList<>();

    fields.add(f("id", v(event.getIdentifier())));
    fields.add(f("title", v(event.getTitle(), BLANK)));
    fields.add(f("source", v(event.getSource(), BLANK)));
    fields.add(f("presenters", arr($(event.getPresenters()).map(Functions.stringToJValue))));
    if (StringUtils.isNotBlank(event.getSeriesId())) {
      String seriesTitle = event.getSeriesName();
      String seriesID = event.getSeriesId();

      fields.add(f("series", obj(f("id", v(seriesID, BLANK)), f("title", v(seriesTitle, BLANK)))));
    }
    fields.add(f("location", v(event.getLocation(), BLANK)));
    fields.add(f("start_date", v(event.getRecordingStartDate(), BLANK)));
    fields.add(f("end_date", v(event.getRecordingEndDate(), BLANK)));

    String schedulingStatus = event.getSchedulingStatus() == null ? null
            : "EVENTS.EVENTS.SCHEDULING_STATUS." + event.getSchedulingStatus();
    fields.add(f("managedAcl", v(event.getManagedAcl(), BLANK)));
    fields.add(f("scheduling_status", v(schedulingStatus, BLANK)));
    fields.add(f("workflow_state", v(event.getWorkflowState(), BLANK)));
    fields.add(f("review_status", v(event.getReviewStatus(), BLANK)));
    fields.add(f("event_status", v(event.getEventStatus())));
    fields.add(f("source", v(getIndexService().getEventSource(event).toString())));
    fields.add(f("has_comments", v(event.hasComments())));
    fields.add(f("has_open_comments", v(event.hasOpenComments())));
    fields.add(f("needs_cutting", v(event.needsCutting())));
    fields.add(f("has_preview", v(event.hasPreview())));
    fields.add(f("agent_id", v(event.getAgentId(), BLANK)));
    fields.add(f("technical_start", v(event.getTechnicalStartTime(), BLANK)));
    fields.add(f("technical_end", v(event.getTechnicalEndTime(), BLANK)));
    fields.add(f("technical_presenters", arr($(event.getTechnicalPresenters()).map(Functions.stringToJValue))));
    fields.add(f("publications", arr(eventPublicationsToJson(event))));
    return obj(fields);
  }

  private JValue attachmentToJSON(Attachment attachment) {
    List<Field> fields = new ArrayList<>();
    fields.addAll(getEventMediaPackageElementFields(attachment));
    fields.addAll(getCommonElementFields(attachment));
    return obj(fields);
  }

  private JValue catalogToJSON(Catalog catalog) {
    List<Field> fields = new ArrayList<>();
    fields.addAll(getEventMediaPackageElementFields(catalog));
    fields.addAll(getCommonElementFields(catalog));
    return obj(fields);
  }

  private JValue trackToJSON(Track track) {
    List<Field> fields = new ArrayList<>();
    fields.addAll(getEventMediaPackageElementFields(track));
    fields.addAll(getCommonElementFields(track));
    fields.add(f("duration", v(track.getDuration(), BLANK)));
    fields.add(f("has_audio", v(track.hasAudio())));
    fields.add(f("has_video", v(track.hasVideo())));
    fields.add(f("streams", obj(streamsToJSON(track.getStreams()))));
    return obj(fields);
  }

  private List<Field> streamsToJSON(org.opencastproject.mediapackage.Stream[] streams) {
    List<Field> fields = new ArrayList<>();
    List<JValue> audioList = new ArrayList<>();
    List<JValue> videoList = new ArrayList<>();
    for (org.opencastproject.mediapackage.Stream stream : streams) {
      // TODO There is a bug with the stream ids, see MH-10325
      if (stream instanceof AudioStreamImpl) {
        List<Field> audio = new ArrayList<>();
        AudioStream audioStream = (AudioStream) stream;
        audio.add(f("id", v(audioStream.getIdentifier(), BLANK)));
        audio.add(f("type", v(audioStream.getFormat(), BLANK)));
        audio.add(f("channels", v(audioStream.getChannels(), BLANK)));
        audio.add(f("bitrate", v(audioStream.getBitRate(), BLANK)));
        audio.add(f("bitdepth", v(audioStream.getBitDepth(), BLANK)));
        audio.add(f("samplingrate", v(audioStream.getSamplingRate(), BLANK)));
        audio.add(f("framecount", v(audioStream.getFrameCount(), BLANK)));
        audio.add(f("peakleveldb", v(audioStream.getPkLevDb(), BLANK)));
        audio.add(f("rmsleveldb", v(audioStream.getRmsLevDb(), BLANK)));
        audio.add(f("rmspeakdb", v(audioStream.getRmsPkDb(), BLANK)));
        audioList.add(obj(audio));
      } else if (stream instanceof VideoStreamImpl) {
        List<Field> video = new ArrayList<>();
        VideoStream videoStream = (VideoStream) stream;
        video.add(f("id", v(videoStream.getIdentifier(), BLANK)));
        video.add(f("type", v(videoStream.getFormat(), BLANK)));
        video.add(f("bitrate", v(videoStream.getBitRate(), BLANK)));
        video.add(f("framerate", v(videoStream.getFrameRate(), BLANK)));
        video.add(f("resolution", v(videoStream.getFrameWidth() + "x" + videoStream.getFrameHeight(), BLANK)));
        video.add(f("framecount", v(videoStream.getFrameCount(), BLANK)));
        video.add(f("scantype", v(videoStream.getScanType(), BLANK)));
        video.add(f("scanorder", v(videoStream.getScanOrder(), BLANK)));
        videoList.add(obj(video));
      } else {
        throw new IllegalArgumentException("Stream must be either audio or video");
      }
    }
    fields.add(f("audio", arr(audioList)));
    fields.add(f("video", arr(videoList)));
    return fields;
  }

  private JValue publicationToJSON(Publication publication) {
    List<Field> fields = new ArrayList<>();
    fields.add(f("id", v(publication.getIdentifier(), BLANK)));
    fields.add(f("channel", v(publication.getChannel(), BLANK)));
    fields.add(f("mimetype", v(publication.getMimeType(), BLANK)));
    fields.add(f("tags", arr($(publication.getTags()).map(toStringJValue))));
    fields.add(f("url", v(signUrl(publication.getURI()), BLANK)));
    fields.addAll(getCommonElementFields(publication));
    return obj(fields);
  }

  private List<Field> getCommonElementFields(MediaPackageElement element) {
    List<Field> fields = new ArrayList<>();
    fields.add(f("size", v(element.getSize(), BLANK)));
    fields.add(f("checksum", v(element.getChecksum() != null ? element.getChecksum().getValue() : null, BLANK)));
    fields.add(f("reference", v(element.getReference() != null ? element.getReference().getIdentifier() : null, BLANK)));
    return fields;
  }

  /**
   * Render an array of {@link Publication}s into a list of JSON values.
   *
   * @param publications
   *          The elements to pull the data from to create the list of {@link JValue}s
   * @return {@link List} of {@link JValue}s that represent the {@link Publication}
   */
  private List<JValue> getEventPublications(Publication[] publications) {
    List<JValue> publicationJSON = new ArrayList<>();
    for (Publication publication : publications) {
      publicationJSON.add(obj(f("id", v(publication.getIdentifier(), BLANK)),
              f("channel", v(publication.getChannel(), BLANK)), f("mimetype", v(publication.getMimeType(), BLANK)),
              f("tags", arr($(publication.getTags()).map(toStringJValue))),
              f("url", v(signUrl(publication.getURI()), BLANK))));
    }
    return publicationJSON;
  }

  private URI signUrl(URI url) {
    if (getUrlSigningService().accepts(url.toString())) {
      try {
        String clientIP = null;
        if (signWithClientIP()) {
          clientIP = getSecurityService().getUserIP();
        }
        return URI.create(getUrlSigningService().sign(url.toString(), getUrlSigningExpireDuration(), null, clientIP));
      } catch (UrlSigningException e) {
        logger.warn("Unable to sign url '{}': {}", url, ExceptionUtils.getStackTrace(e));
      }
    }
    return url;
  }

  /**
   * Render an array of {@link MediaPackageElement}s into a list of JSON values.
   *
   * @param elements
   *          The elements to pull the data from to create the list of {@link JValue}s
   * @return {@link List} of {@link JValue}s that represent the {@link MediaPackageElement}
   */
  private List<JValue> getEventMediaPackageElements(MediaPackageElement[] elements) {
    List<JValue> elementJSON = new ArrayList<>();
    for (MediaPackageElement element : elements) {
      elementJSON.add(obj(getEventMediaPackageElementFields(element)));
    }
    return elementJSON;
  }

  private List<Field> getEventMediaPackageElementFields(MediaPackageElement element) {
    List<Field> fields = new ArrayList<>();
    fields.add(f("id", v(element.getIdentifier(), BLANK)));
    fields.add(f("type", v(element.getFlavor(), BLANK)));
    fields.add(f("mimetype", v(element.getMimeType(), BLANK)));
    List<JValue> tags = Stream.$(element.getTags()).map(toStringJValue).toList();
    fields.add(f("tags", arr(tags)));
    fields.add(f("url", v(signUrl(element.getURI()), BLANK)));
    return fields;
  }

  private static final Fn<String, JValue> toStringJValue = new Fn<String, JValue>() {
    @Override
    public JValue apply(String stringValue) {
      return v(stringValue, BLANK);
    }
  };

  private final Fn<Publication, JObject> publicationToJson = new Fn<Publication, JObject>() {
    @Override
    public JObject apply(Publication publication) {
      final Opt<String> channel = Opt.nul(EventUtils.PUBLICATION_CHANNELS.get(publication.getChannel()));
      String url = publication.getURI() == null ? "" : signUrl(publication.getURI()).toString();
      return obj(f("id", v(publication.getChannel())),
              f("name", v(channel.getOr("EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM"))), f("url", v(url, NULL)));
    }
  };

  protected static final Fn<TechnicalMetadata, JObject> technicalMetadataToJson = new Fn<TechnicalMetadata, JObject>() {
    @Override
    public JObject apply(TechnicalMetadata technicalMetadata) {
      JValue agentConfig = technicalMetadata.getCaptureAgentConfiguration() == null ? v("")
              : JSONUtils.mapToJSON(technicalMetadata.getCaptureAgentConfiguration());
      JValue start = technicalMetadata.getStartDate() == null ? v("")
              : v(DateTimeSupport.toUTC(technicalMetadata.getStartDate().getTime()));
      JValue end = technicalMetadata.getEndDate() == null ? v("")
              : v(DateTimeSupport.toUTC(technicalMetadata.getEndDate().getTime()));
      return obj(f("agentId", v(technicalMetadata.getAgentId(), BLANK)), f("agentConfiguration", agentConfig),
              f("start", start), f("end", end), f("eventId", v(technicalMetadata.getEventId(), BLANK)),
              f("presenters", JSONUtils.setToJSON(technicalMetadata.getPresenters())),
              f("optOut", v(technicalMetadata.isOptOut())),
              f("recording", recordingToJson.apply(technicalMetadata.getRecording())));
    }
  };

  protected static final Fn<Opt<Recording>, JObject> recordingToJson = new Fn<Opt<Recording>, JObject>() {
    @Override
    public JObject apply(Opt<Recording> recording) {
      if (recording.isNone()) {
        return obj();
      }
      return obj(f("id", v(recording.get().getID(), BLANK)),
              f("lastCheckInTime", v(recording.get().getLastCheckinTime(), BLANK)),
              f("lastCheckInTimeUTC", v(toUTC(recording.get().getLastCheckinTime()), BLANK)),
              f("state", v(recording.get().getState(), BLANK)));
    }
  };

  @PUT
  @Path("{eventId}/workflows/{workflowId}/action/{action}")
  @RestQuery(name = "workflowAction", description = "Performs the given action for the given workflow.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "workflowId", description = "The id of the workflow", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "action", description = "The action to take: STOP, RETRY or NONE (abort processing)", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(responseCode = SC_OK, description = "Workflow resumed."),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "Event or workflow instance not found."),
                  @RestResponse(responseCode = SC_BAD_REQUEST, description = "Invalid action entered."),
                  @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to perform the action. Maybe you need to authenticate."),
                  @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "An exception occurred.") })
  public Response workflowAction(@PathParam("eventId") String id, @PathParam("workflowId") long wfId,
          @PathParam("action") String action) {
    if (StringUtils.isEmpty(id) || StringUtils.isEmpty(action)) {
      return badRequest();
    }

    try {
      final Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
      if (optEvent.isNone()) {
        return notFound("Cannot find an event with id '%s'.", id);
      }

      final WorkflowInstance wfInstance = getWorkflowService().getWorkflowById(wfId);
      if (!wfInstance.getMediaPackage().getIdentifier().toString().equals(id)) {
        return badRequest(String.format("Workflow %s is not associated to event %s", wfId, id));
      }

      if (RetryStrategy.NONE.toString().equalsIgnoreCase(action)
        || RetryStrategy.RETRY.toString().equalsIgnoreCase(action)) {
        getWorkflowService().resume(wfId, Collections.singletonMap("retryStrategy", action));
        return ok();
      }

      if (WORKFLOW_ACTION_STOP.equalsIgnoreCase(action)) {
        getWorkflowService().stop(wfId);
        return ok();
      }

      return badRequest("Action not supported: " + action);
    } catch (NotFoundException e) {
      return notFound("Workflow not found: '%d'.", wfId);
    } catch (IllegalStateException e) {
      return badRequest(String.format("Action %s not allowed for current workflow state. EventId: %s", action, id));
    } catch (UnauthorizedException e) {
      return forbidden();
    } catch (Exception e) {
      return serverError();
    }
  }

  @DELETE
  @Path("{eventId}/workflows/{workflowId}")
  @RestQuery(name = "deleteWorkflow", description = "Deletes a workflow", returnDescription = "The method doesn't return any content", pathParameters = {
    @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING),
    @RestParameter(name = "workflowId", isRequired = true, description = "The workflow identifier", type = RestParameter.Type.INTEGER) }, reponses = {
    @RestResponse(responseCode = SC_BAD_REQUEST, description = "When trying to delete the latest workflow of the event."),
    @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event or the workflow has not been found."),
    @RestResponse(responseCode = SC_NO_CONTENT, description = "The method does not return any content") })
  public Response deleteWorkflow(@PathParam("eventId") String id, @PathParam("workflowId") long wfId)
    throws SearchIndexException {
    final Opt<Event> optEvent = getIndexService().getEvent(id, getIndex());
    try {
      if (optEvent.isNone()) {
        return notFound("Cannot find an event with id '%s'.", id);
      }

      final WorkflowInstance wfInstance = getWorkflowService().getWorkflowById(wfId);
      if (!wfInstance.getMediaPackage().getIdentifier().toString().equals(id)) {
        return badRequest(String.format("Workflow %s is not associated to event %s", wfId, id));
      }

      if (wfId == optEvent.get().getWorkflowId()) {
        return badRequest(String.format("Cannot delete current workflow %s from event %s."
          + " Only older workflows can be deleted.", wfId, id));
      }

      getWorkflowService().remove(wfId);

      return Response.noContent().build();
    } catch (WorkflowStateException e) {
      return badRequest("Deleting is not allowed for current workflow state. EventId: " + id);
    } catch (NotFoundException e) {
      return notFound("Workflow not found: '%d'.", wfId);
    } catch (UnauthorizedException e) {
      return forbidden();
    } catch (Exception e) {
      return serverError();
    }
  }

  private void checkAgentAccessForEvent(final String eventId) throws UnauthorizedException, SearchIndexException {
    final Opt<Event> event = getIndexService().getEvent(eventId, getIndex());
    if (event.isNone() || !event.get().getEventStatus().contains("SCHEDULE")) {
      return;
    }
    SecurityUtil.checkAgentAccess(getSecurityService(), event.get().getAgentId());
  }

  private void checkAgentAccessForAgent(final String agentId) throws UnauthorizedException {
    SecurityUtil.checkAgentAccess(getSecurityService(), agentId);
  }

}
