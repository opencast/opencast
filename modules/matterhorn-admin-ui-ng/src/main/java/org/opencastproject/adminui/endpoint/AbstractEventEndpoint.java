/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.jsonArrayFromList;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.RestUtils.conflictJson;
import static org.opencastproject.index.service.util.RestUtils.notFound;
import static org.opencastproject.index.service.util.RestUtils.okJson;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.Jsons.arr;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.RestUtil.splitCommaSeparatedParam;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.forbidden;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.adminui.exception.JobEndpointException;
import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.adminui.util.ParticipationUtils;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.archive.opencast.OpencastQueryBuilder;
import org.opencastproject.archive.opencast.OpencastResultSet;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.comments.Comment;
import org.opencastproject.comments.CommentException;
import org.opencastproject.comments.CommentParser;
import org.opencastproject.comments.CommentReply;
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.api.IndexService.Source;
import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.MetadataField;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataList.Locked;
import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.index.service.exception.InternalServerErrorException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.provider.CommentsListProvider.RESOLUTION;
import org.opencastproject.index.service.resources.list.provider.EventsListProvider.Comments;
import org.opencastproject.index.service.resources.list.query.EventListQuery;
import org.opencastproject.index.service.util.AccessInformationUtil;
import org.opencastproject.index.service.util.JSONUtils;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase.SortType;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.rest.BulkOperationResult;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.systems.MatterhornConstans;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowUtil;
import org.opencastproject.workflow.handler.distribution.EngagePublicationChannel;
import org.opencastproject.workflow.handler.distribution.PublishInternalWorkflowOperationHandler;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JField;
import com.entwinemedia.fn.data.json.JObjectWrite;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.codehaus.jettison.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * <p/>
 * This first implementation uses the {@link org.opencastproject.archive.opencast.OpencastArchive}. In a later iteration
 * the endpoint may abstract over the concrete archive.
 */
@Path("/")
@RestService(name = "eventservice", title = "Event Service", notes = "", abstractText = "Provides resources and operations related to the events")
public abstract class AbstractEventEndpoint {

  /** The logging facility */
  static final Logger logger = LoggerFactory.getLogger(AbstractEventEndpoint.class);

  private static final int CREATED_BY_UI_ORDER = 14;

  public abstract WorkflowService getWorkflowService();

  public abstract AdminUISearchIndex getIndex();

  public abstract DublinCoreCatalogService getDublinCoreService();

  public abstract JobEndpoint getJobService();

  public abstract ListProvidersService getListProviderService();

  public abstract OpencastArchive getArchive();

  /** A media package element provider used by the archive. */
  public abstract HttpMediaPackageElementProvider getHttpMediaPackageElementProvider();

  public abstract Workspace getWorkspace();

  public abstract AclService getAclService();

  public abstract SeriesService getSeriesService();

  public abstract ParticipationManagementDatabase getPMPersistence();

  public abstract EventCommentService getEventCommentService();

  public abstract SecurityService getSecurityService();

  public abstract IndexService getIndexService();

  public abstract IngestService getIngestService();

  public abstract AuthorizationService getAuthorizationService();

  public abstract SchedulerService getSchedulerService();

  public abstract CaptureAgentStateService getCaptureAgentStateService();

  public abstract List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization);

  public abstract EventCatalogUIAdapter getEpisodeCatalogUIAdapter();

  public abstract String getPreviewSubtype();

  /** Default server URL */
  protected String serverUrl = "http://localhost:8080";

  /** Service url */
  protected String serviceUrl = null;

  /** A parser for handling JSON documents inside the body of a request. **/
  private final JSONParser parser = new JSONParser();

  /** The single thread executor service */
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  private static final Map<String, String> PUBLICATION_CHANNELS = new HashMap<String, String>();

  static {
    PUBLICATION_CHANNELS.put(EngagePublicationChannel.CHANNEL_ID, "EVENTS.EVENTS.DETAILS.GENERAL.ENGAGE");
    PUBLICATION_CHANNELS.put("youtube", "EVENTS.EVENTS.DETAILS.GENERAL.YOUTUBE");
  }

  /**
   * Activates REST service.
   *
   * @param cc
   *          ComponentContext
   */
  public void activate(ComponentContext cc) {
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(MatterhornConstans.SERVER_URL_PROPERTY);
      if (StringUtils.isNotBlank(ccServerUrl))
        this.serverUrl = ccServerUrl;
    }
    serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  @GET
  @Path("catalogAdapters")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getcataloguiadapters", description = "Returns the available catalog UI adapters as JSON", returnDescription = "The catalog UI adapters as JSON", reponses = { @RestResponse(description = "Returns the available catalog UI adapters as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response getCatalogAdapters() {
    List<JValue> adapters = new ArrayList<JValue>();
    for (EventCatalogUIAdapter adapter : getEventCatalogUIAdapters()) {
      List<JField> fields = new ArrayList<JField>();
      fields.add(f("flavor", v(adapter.getFlavor().toString())));
      fields.add(f("title", v(adapter.getUITitle())));
      adapters.add(j(fields));
    }
    return okJson(a(adapters));
  }

  @GET
  @Path("{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getevent", description = "Returns the event by the given id as JSON", returnDescription = "The event as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the event as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventResponse(@PathParam("eventId") String id) throws Exception {
    for (final Event event : getEvent(id)) {
      return okJson(eventToJSON(event));
    }
    return notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteevent", description = "Delete a single event.", returnDescription = "Ok if the event has been deleted.", pathParameters = { @RestParameter(name = "eventId", isRequired = true, description = "The id of the event to delete.", type = STRING), }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The event has been deleted."),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "The event could not be found."),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response deleteEvent(@PathParam("eventId") String id) throws NotFoundException, UnauthorizedException {
    if (!removeEvent(id))
      return Response.serverError().build();

    return Response.ok().build();
  }

  @POST
  @Path("deleteEvents")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteevents", description = "Deletes a json list of events by their given ids e.g. [\"1dbe7255-e17d-4279-811d-a5c7ced689bf\", \"04fae22b-0717-4f59-8b72-5f824f76d529\"]", returnDescription = "Returns a JSON object containing a list of event ids that were deleted, not found or if there was a server error.", reponses = {
          @RestResponse(description = "Events have been deleted", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The list of ids could not be parsed into a json list.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "If the current user is not authorized to perform this action", responseCode = HttpServletResponse.SC_UNAUTHORIZED) })
  public Response deleteEvents(String eventIdsContent) throws UnauthorizedException {
    if (StringUtils.isBlank(eventIdsContent)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

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
        if (!removeEvent(eventId)) {
          result.addServerError(eventId);
        } else {
          result.addOk(eventId);
        }
      } catch (NotFoundException e) {
        result.addNotFound(eventId);
      }
    }
    return Response.ok(result.toJson()).build();
  }

  /**
   * Removes an event.
   *
   * @param id
   *          The id for the event to remove.
   * @return true if the event was found and removed.
   */
  private boolean removeEvent(String id) throws NotFoundException, UnauthorizedException {
    boolean unauthorizedScheduler = false;
    boolean notFoundScheduler = false;
    boolean removedScheduler = true;
    try {
      getSchedulerService().removeEvent(getSchedulerService().getEventId(id));
    } catch (NotFoundException e) {
      notFoundScheduler = true;
    } catch (SchedulerException e) {
      removedScheduler = false;
      logger.error("Unable to remove the event '{}' from scheduler service: {}", id, ExceptionUtils.getStackTrace(e));
    } catch (UnauthorizedException e) {
      unauthorizedScheduler = true;
    }

    boolean unauthorizedWorkflow = false;
    boolean notFoundWorkflow = false;
    boolean removedWorkflow = true;
    try {
      WorkflowQuery workflowQuery = new WorkflowQuery().withMediaPackage(id);
      WorkflowSet workflowSet = getWorkflowService().getWorkflowInstances(workflowQuery);
      if (workflowSet.size() == 0)
        notFoundWorkflow = true;
      for (WorkflowInstance instance : workflowSet.getItems()) {
        getWorkflowService().stop(instance.getId());
        getWorkflowService().remove(instance.getId());
      }
    } catch (NotFoundException e) {
      notFoundWorkflow = true;
    } catch (WorkflowDatabaseException e) {
      removedWorkflow = false;
      logger.error("Unable to remove the event '{}' because removing workflow failed: {}", id,
              ExceptionUtils.getStackTrace(e));
    } catch (WorkflowException e) {
      removedWorkflow = false;
      logger.error("Unable to remove the event '{}' because removing workflow failed: {}", id,
              ExceptionUtils.getStackTrace(e));
    } catch (UnauthorizedException e) {
      unauthorizedWorkflow = true;
    }

    boolean unauthorizedArchive = false;
    boolean notFoundArchive = false;
    boolean removedArchive = true;
    try {
      OpencastResultSet archiveRes = getArchive().find(
              OpencastQueryBuilder.query().mediaPackageId(id).onlyLastVersion(true),
              getHttpMediaPackageElementProvider().getUriRewriter());
      if (archiveRes.size() > 0) {
        getArchive().delete(id);
      } else {
        notFoundArchive = true;
      }
    } catch (ArchiveException e) {
      if (e.isCauseNotAuthorized()) {
        unauthorizedArchive = true;
      } else if (e.isCauseNotFound()) {
        notFoundArchive = true;
      } else {
        removedArchive = false;
        logger.error("Unable to remove the event '{}' from the archive: {}", id, ExceptionUtils.getStackTrace(e));
      }
    }

    if (notFoundScheduler && notFoundWorkflow && notFoundArchive)
      throw new NotFoundException("Event id " + id + " not found.");

    if (unauthorizedScheduler || unauthorizedWorkflow || unauthorizedArchive)
      throw new UnauthorizedException("Not authorized to remove event id " + id);

    return removedScheduler && removedWorkflow && removedArchive;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("events/recipients")
  @RestQuery(name = "geteventsrecipients", description = "Returns the events recipients as JSON", returnDescription = "Returns the events recipients as JSON", restParameters = { @RestParameter(name = "eventIds", isRequired = true, description = "A list of comma separated event IDs", type = STRING), }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The events recipients as JSON."),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "At least one event id must be set.") })
  public Response getEventsRecordingsAndRecipients(@QueryParam("eventIds") String eventIds) {
    if (getPMPersistence() == null)
      return Response.status(Status.SERVICE_UNAVAILABLE).build();

    final Monadics.ListMonadic<String> eIds = splitCommaSeparatedParam(option(eventIds));
    if (eIds.value().isEmpty())
      return badRequest();

    List<Recording> recordings = ParticipationUtils.getRecordingsByEventId(getSchedulerService(), getPMPersistence(),
            eIds.value());
    List<Val> recipientsArr = mlist(new HashSet<Person>(mlist(recordings).flatMap(getRecipients).value())).map(
            JSONUtils.personToJsonVal).value();
    return Response.ok(obj(p("recipients", arr(recipientsArr))).toJson()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{eventId}/messages")
  @RestQuery(name = "geteventmessages", description = "Returns the event messages as JSON", returnDescription = "Returns the event messages as JSON", pathParameters = { @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = STRING) }, restParameters = { @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any of the following: DATE OR SENDER.  Add '_DESC' to reverse the sort order (e.g. DATE_DESC).", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The event messages as JSON."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The event has not been found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Invalid SORT type, it was not DATE, DATE_DESC SENDER or SENDER_DESC"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response getRecordingMessages(@PathParam("eventId") String eventId, @QueryParam("sort") String sort)
          throws NotFoundException {
    if (getPMPersistence() == null)
      return Response.status(Status.SERVICE_UNAVAILABLE).build();

    Option<SortType> sortType = Option.<SortType> none();
    sortType = ParticipationUtils.getMessagesSortField(sort);
    if (StringUtils.isNotBlank(sort) && sortType.isNone()) {
      return Response.status(SC_BAD_REQUEST).build();
    }

    Long scheduledEventId;
    try {
      scheduledEventId = getSchedulerService().getEventId(eventId);
    } catch (SchedulerException e) {
      logger.error("Unable to get scheduled event id by event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }

    try {
      Long recordingId = getPMPersistence().getRecordingByEvent(scheduledEventId).getId().getOrElse(-1L);
      List<Val> jsonArr = new ArrayList<org.opencastproject.util.Jsons.Val>();
      for (Message m : getPMPersistence().getMessagesByRecordingId(recordingId, sortType)) {
        jsonArr.add(m.toJson());
      }
      return Response.ok(org.opencastproject.util.Jsons.arr(jsonArr).toJson()).build();
    } catch (ParticipationManagementDatabaseException e) {
      logger.error("Unable to get messages by recording {}: {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("{eventId}/general.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventgeneral", description = "Returns all the data related to the general tab in the event details modal as JSON", returnDescription = "All the data related to the event general tab as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id (mediapackage id).", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the event general tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventGeneralTab(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getEvent(id);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

    // Quick actions have been temporally removed from the general tab
    // ---------------------------------------------------------------
    // List<JValue> actions = new ArrayList<JValue>();
    // List<WorkflowDefinition> workflowsDefinitions = getWorkflowService().listAvailableWorkflowDefinitions();
    // for (WorkflowDefinition wflDef : workflowsDefinitions) {
    // if (wflDef.containsTag(WORKFLOWDEF_TAG)) {
    //
    // actions.add(j(f("id", v(wflDef.getId())), f("title", v(Opt.nul(wflDef.getTitle()).or(""))),
    // f("description", v(Opt.nul(wflDef.getDescription()).or(""))),
    // f("configuration_panel", v(Opt.nul(wflDef.getConfigurationPanel()).or("")))));
    // }
    // }

    Event event = optEvent.get();
    Opt<MediaPackage> mpOpt = getIndexService().getEventMediapackage(event);
    List<JValue> pubJSON = new ArrayList<JValue>();
    if (mpOpt.isSome()) {
      for (JObjectWrite json : Stream.$(mpOpt.get().getPublications()).filter(internalChannelFilter)
              .map(publicationToJson)) {
        pubJSON.add(json);
      }
    }

    return okJson(j(f("publications", a(pubJSON)), f("optout", vN(event.getOptedOut())),
            f("blacklisted", vN(event.getBlacklisted())), f("review-status", vN(event.getReviewStatus()))));
  }

  @GET
  @Path("{eventId}/comments")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventcomments", description = "Returns all the data related to the comments tab in the event details modal as JSON", returnDescription = "All the data related to the event comments tab as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the event comments tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventComments(@PathParam("eventId") String eventId) throws Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      List<Comment> comments = getEventCommentService().getComments(eventId);
      List<Val> commentArr = new ArrayList<Val>();
      for (Comment c : comments) {
        commentArr.add(c.toJson());
      }
      return Response.ok(org.opencastproject.util.Jsons.arr(commentArr).toJson(), MediaType.APPLICATION_JSON_TYPE)
              .build();
    } catch (CommentException e) {
      logger.error("Unable to get comments from event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
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
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      Comment comment = getEventCommentService().getComment(eventId, commentId);
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
          throws NotFoundException, Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      Comment dto = getEventCommentService().getComment(eventId, commentId);

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

      Comment updatedComment = Comment.create(dto.getId(), text, dto.getAuthor(), reason, resolved,
              dto.getCreationDate(), new Date(), dto.getReplies());

      updatedComment = getEventCommentService().updateComment(eventId, updatedComment);
      List<Comment> comments = getEventCommentService().getComments(eventId);
      updateCommentCatalog(optEvent.get(), comments);
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
  @RestQuery(name = "applyAclToEvent", description = "Immediate application of an ACL to an event", returnDescription = "Status code", pathParameters = { @RestParameter(name = "eventId", isRequired = true, description = "The event ID", type = STRING) }, restParameters = { @RestParameter(name = "acl", isRequired = true, description = "The ACL to apply", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the given ACL"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The the event has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error") })
  public Response applyAclToEvent(@PathParam("eventId") String eventId, @FormParam("acl") String acl)
          throws NotFoundException, InternalServerErrorException {
    final AccessControlList accessControlList;
    try {
      accessControlList = AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      logger.warn("Unable to parse ACL '{}'", acl);
      return badRequest();
    }

    try {
      final Opt<Event> optEvent = getEvent(eventId);
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
        return forbidden("Unable to edit an ACL for a current workflow.");
      } else {
        // The event doesn't exist as a mediapackage yet so use the scheduler service to update the ACL
        getSchedulerService().updateAccessControlList(getSchedulerService().getEventId(eventId), accessControlList);
        return ok();
      }
    } catch (AclServiceException e) {
      logger.error("Error applying acl '{}' to event '{}' because: {}", new Object[] { accessControlList, eventId,
              ExceptionUtils.getStackTrace(e) });
      return serverError();
    } catch (SchedulerException e) {
      logger.error("Error applying ACL to scheduled event {} because {}", eventId, ExceptionUtils.getStackTrace(e));
      return serverError();
    } catch (SearchIndexException e) {
      logger.error("Error finding event {} to apply ACL because: {}", eventId, ExceptionUtils.getStackTrace(e));
      return serverError();
    } catch (NotFoundException e) {
      throw e;
    }
  }

  @POST
  @Path("{eventId}/comment")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createeventcomment", description = "Creates a comment related to the event given by the identifier", returnDescription = "The comment related to the event as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(name = "text", isRequired = true, description = "The comment text", type = TEXT),
          @RestParameter(name = "resolved", isRequired = false, description = "The comment resolved status", type = RestParameter.Type.BOOLEAN),
          @RestParameter(name = "reason", isRequired = false, description = "The comment reason", type = STRING) }, reponses = {
          @RestResponse(description = "The comment has been created.", responseCode = HttpServletResponse.SC_CREATED),
          @RestResponse(description = "If no text ist set.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response createEventComment(@PathParam("eventId") String eventId, @FormParam("text") String text,
          @FormParam("reason") String reason, @FormParam("resolved") Boolean resolved) throws Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    if (StringUtils.isBlank(text))
      return Response.status(Status.BAD_REQUEST).build();

    User author = getSecurityService().getUser();
    try {
      Comment createdComment = Comment.create(Option.<Long> none(), text, author, reason,
              BooleanUtils.toBoolean(reason));
      createdComment = getEventCommentService().updateComment(eventId, createdComment);
      List<Comment> comments = getEventCommentService().getComments(eventId);
      updateCommentCatalog(optEvent.get(), comments);
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
          throws NotFoundException, Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      Comment dto = getEventCommentService().getComment(eventId, commentId);
      Comment updatedComment = Comment.create(dto.getId(), dto.getText(), dto.getAuthor(), dto.getReason(), true,
              dto.getCreationDate(), new Date(), dto.getReplies());

      updatedComment = getEventCommentService().updateComment(eventId, updatedComment);
      List<Comment> comments = getEventCommentService().getComments(eventId);
      updateCommentCatalog(optEvent.get(), comments);
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
          throws NotFoundException, Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      getEventCommentService().deleteComment(eventId, commentId);
      List<Comment> comments = getEventCommentService().getComments(eventId);
      updateCommentCatalog(optEvent.get(), comments);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to delete comment {} on event {}: {}", new String[] { Long.toString(commentId), eventId,
              ExceptionUtils.getStackTrace(e) });
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
          @PathParam("replyId") long replyId) throws NotFoundException, Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    Comment comment = null;
    CommentReply reply = null;
    try {
      comment = getEventCommentService().getComment(eventId, commentId);
      for (CommentReply r : comment.getReplies()) {
        if (r.getId().isNone() || replyId != r.getId().get().longValue())
          continue;
        reply = r;
        break;
      }

      if (reply == null)
        throw new NotFoundException("Reply with id " + replyId + " not found!");

      comment.removeReply(reply);

      Comment updatedComment = getEventCommentService().updateComment(eventId, comment);
      List<Comment> comments = getEventCommentService().getComments(eventId);
      updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not remove event comment reply {} from comment {}: {}", new String[] { Long.toString(replyId),
              Long.toString(commentId), ExceptionUtils.getStackTrace(e) });
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("{eventId}/comment/{commentId}/{replyId}")
  @RestQuery(name = "updateeventcommentreply", description = "Updates an event comment reply", returnDescription = "The updated comment as JSON.", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING),
          @RestParameter(name = "replyId", isRequired = true, description = "The comment reply identifier", type = STRING) }, restParameters = { @RestParameter(name = "text", isRequired = true, description = "The comment reply text", type = TEXT) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The event or comment to extend with a reply or the reply has not been found."),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "If no text is set."),
          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response updateEventCommentReply(@PathParam("eventId") String eventId, @PathParam("commentId") long commentId,
          @PathParam("replyId") long replyId, @FormParam("text") String text) throws NotFoundException, Exception {
    if (StringUtils.isBlank(text))
      return Response.status(Status.BAD_REQUEST).build();

    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    Comment comment = null;
    CommentReply reply = null;
    try {
      comment = getEventCommentService().getComment(eventId, commentId);
      for (CommentReply r : comment.getReplies()) {
        if (r.getId().isNone() || replyId != r.getId().get().longValue())
          continue;
        reply = r;
        break;
      }

      if (reply == null)
        throw new NotFoundException("Reply with id " + replyId + " not found!");

      CommentReply updatedReply = CommentReply.create(reply.getId(), text.trim(), reply.getAuthor(),
              reply.getCreationDate(), new Date());
      comment.removeReply(reply);
      comment.addReply(updatedReply);

      Comment updatedComment = getEventCommentService().updateComment(eventId, comment);
      List<Comment> comments = getEventCommentService().getComments(eventId);
      updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not update event comment reply {} from comment {}: {}", new String[] { Long.toString(replyId),
              Long.toString(commentId), ExceptionUtils.getStackTrace(e) });
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
          @FormParam("text") String text, @FormParam("resolved") Boolean resolved) throws NotFoundException, Exception {
    if (StringUtils.isBlank(text))
      return Response.status(Status.BAD_REQUEST).build();

    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    Comment comment = null;
    try {
      comment = getEventCommentService().getComment(eventId, commentId);
      Comment updatedComment;

      if (resolved != null && resolved) {
        // If the resolve flag is set to true, change to comment to resolved
        updatedComment = Comment.create(comment.getId(), comment.getText(), comment.getAuthor(), comment.getReason(),
                true, comment.getCreationDate(), new Date(), comment.getReplies());
      } else {
        updatedComment = comment;
      }

      User author = getSecurityService().getUser();
      CommentReply reply = CommentReply.create(Option.<Long> none(), text, author);
      updatedComment.addReply(reply);

      updatedComment = getEventCommentService().updateComment(eventId, updatedComment);
      List<Comment> comments = getEventCommentService().getComments(eventId);
      updateCommentCatalog(optEvent.get(), comments);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (Exception e) {
      logger.warn("Could not create event comment reply on comment {}: {}", comment, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("{eventId}/metadata.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventmetadata", description = "Returns all the data related to the metadata tab in the event details modal as JSON", returnDescription = "All the data related to the event metadata tab as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the event metadata tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventMetadata(@PathParam("eventId") String eventId) throws Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    MetadataList metadataList = new MetadataList();
    List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
    catalogUIAdapters.remove(getEpisodeCatalogUIAdapter());
    Opt<MediaPackage> optMediaPackage = getIndexService().getEventMediapackage(optEvent.get());
    if (catalogUIAdapters.size() > 0) {
      if (optMediaPackage.isSome()) {
        for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
          metadataList.add(catalogUIAdapter, catalogUIAdapter.getFields(optMediaPackage.get()));
        }
      }
    }
    metadataList.add(getEpisodeCatalogUIAdapter(), getEventMetadata(optEvent.get()));

    if (WorkflowInstance.WorkflowState.RUNNING.toString().equals(optEvent.get().getWorkflowState()))
      metadataList.setLocked(Locked.WORKFLOW_RUNNING);

    return okJson(metadataList.toJSON());
  }

  /**
   * Loads the metadata for the given event
   *
   * @param event
   *          the source {@link Event}
   * @return a {@link AbstractMetadataCollection} instance with all the event metadata
   */
  @SuppressWarnings("unchecked")
  private AbstractMetadataCollection getEventMetadata(Event event) throws Exception {
    AbstractMetadataCollection metadata = getEpisodeCatalogUIAdapter().getRawFields();

    MetadataField<?> title = metadata.getOutputFields().get("title");
    metadata.removeField(title);
    MetadataField<String> newTitle = MetadataUtils.copyMetadataField(title);
    newTitle.setValue(event.getTitle());
    metadata.addField(newTitle);

    MetadataField<?> subject = metadata.getOutputFields().get("subject");
    metadata.removeField(subject);
    MetadataField<String> newSubject = MetadataUtils.copyMetadataField(subject);
    newSubject.setValue(event.getSubject());
    metadata.addField(newSubject);

    MetadataField<?> description = metadata.getOutputFields().get("description");
    metadata.removeField(description);
    MetadataField<String> newDescription = MetadataUtils.copyMetadataField(description);
    newDescription.setValue(event.getDescription());
    metadata.addField(newDescription);

    MetadataField<?> language = metadata.getOutputFields().get("language");
    metadata.removeField(language);
    MetadataField<String> newLanguage = MetadataUtils.copyMetadataField(language);
    newLanguage.setValue(event.getLanguage());
    metadata.addField(newLanguage);

    MetadataField<?> rightsHolder = metadata.getOutputFields().get("rightsHolder");
    metadata.removeField(rightsHolder);
    MetadataField<String> newRightsHolder = MetadataUtils.copyMetadataField(rightsHolder);
    newRightsHolder.setValue(event.getRights());
    metadata.addField(newRightsHolder);

    MetadataField<?> license = metadata.getOutputFields().get("license");
    metadata.removeField(license);
    MetadataField<String> newLicense = MetadataUtils.copyMetadataField(license);
    newLicense.setValue(event.getLicense());
    metadata.addField(newLicense);

    MetadataField<?> series = metadata.getOutputFields().get("isPartOf");
    metadata.removeField(series);
    MetadataField<String> newSeries = MetadataUtils.copyMetadataField(series);
    newSeries.setValue(event.getSeriesId());
    metadata.addField(newSeries);

    MetadataField<?> presenters = metadata.getOutputFields().get("creator");
    metadata.removeField(presenters);
    MetadataField<String> newPresenters = MetadataUtils.copyMetadataField(presenters);
    newPresenters.setValue(StringUtils.join(event.getPresenters(), ", "));
    metadata.addField(newPresenters);

    MetadataField<?> contributors = metadata.getOutputFields().get("contributor");
    metadata.removeField(contributors);
    MetadataField<String> newContributors = MetadataUtils.copyMetadataField(contributors);
    newContributors.setValue(StringUtils.join(event.getContributors(), ", "));
    metadata.addField(newContributors);

    String recordingStartDate = event.getRecordingStartDate();
    if (StringUtils.isNotBlank(recordingStartDate)) {
      Date startDateTime = new Date(DateTimeSupport.fromUTC(recordingStartDate));

      MetadataField<?> startDate = metadata.getOutputFields().get("startDate");
      metadata.removeField(startDate);
      MetadataField<String> newStartDate = MetadataUtils.copyMetadataField(startDate);
      SimpleDateFormat sdf = new SimpleDateFormat(startDate.getPattern().get());
      newStartDate.setValue(sdf.format(startDateTime));
      metadata.addField(newStartDate);

      MetadataField<?> startTime = metadata.getOutputFields().get("startTime");
      metadata.removeField(startTime);
      MetadataField<String> newStartTime = MetadataUtils.copyMetadataField(startTime);
      sdf = new SimpleDateFormat(startTime.getPattern().get());
      newStartTime.setValue(sdf.format(startDateTime));
      metadata.addField(newStartTime);
    }

    if (event.getDuration() != null) {
      MetadataField<?> duration = metadata.getOutputFields().get("duration");
      metadata.removeField(duration);
      MetadataField<String> newDuration = MetadataUtils.copyMetadataField(duration);
      newDuration.setValue(event.getDuration().toString());
      metadata.addField(newDuration);
    }

    MetadataField<?> agent = metadata.getOutputFields().get("agent");
    metadata.removeField(agent);
    MetadataField<String> newAgent = MetadataUtils.copyMetadataField(agent);
    newAgent.setValue(event.getLocation());
    metadata.addField(newAgent);

    MetadataField<?> source = metadata.getOutputFields().get("source");
    metadata.removeField(source);
    MetadataField<String> newSource = MetadataUtils.copyMetadataField(source);
    newSource.setValue(event.getSource());
    metadata.addField(newSource);

    // Admin UI only field
    MetadataField<String> createdBy = MetadataField.createTextMetadataField("createdBy", Opt.<String> none(),
            "EVENTS.EVENTS.DETAILS.METADATA.CREATED_BY", true, false, Opt.<Map<String, Object>> none(),
            Opt.<String> none(), Opt.some(CREATED_BY_UI_ORDER), Opt.<String> none());
    createdBy.setValue(event.getCreator());
    metadata.addField(createdBy);

    MetadataField<?> created = metadata.getOutputFields().get("created");
    metadata.removeField(created);
    MetadataField<Date> newCreated = MetadataUtils.copyMetadataField(created);
    newCreated.setValue(new Date(DateTimeSupport.fromUTC(event.getCreated())));
    metadata.addField(newCreated);

    MetadataField<?> uid = metadata.getOutputFields().get("uid");
    metadata.removeField(uid);
    MetadataField<String> newUID = MetadataUtils.copyMetadataField(uid);
    newUID.setValue(event.getIdentifier());
    metadata.addField(newUID);

    return metadata;
  }

  private List<EventCatalogUIAdapter> getEventCatalogUIAdapters() {
    return new ArrayList<EventCatalogUIAdapter>(getEventCatalogUIAdapters(getSecurityService().getOrganization()
            .getId()));
  }

  private MetadataList getMetadatListWithAllEventCatalogUIAdapters() {
    MetadataList metadataList = new MetadataList();
    for (EventCatalogUIAdapter catalogUIAdapter : getEventCatalogUIAdapters()) {
      metadataList.add(catalogUIAdapter, catalogUIAdapter.getRawFields());
    }
    return metadataList;
  }

  @PUT
  @Path("{eventId}/metadata")
  @RestQuery(name = "updateeventmetadata", description = "Update the passed metadata for the event with the given Id", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = { @RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of metadata to update") }, reponses = {
          @RestResponse(description = "The metadata have been updated.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Could not parse metadata.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "No content is returned.")
  public Response updateEventMetadata(@PathParam("eventId") String id, @FormParam("metadata") String metadataJSON)
          throws UnauthorizedException, Exception {
    MetadataList metadataList = getIndexService().updateAllEventMetadata(id, metadataJSON, getIndex());
    return okJson(metadataList.toJSON());
  }

  private void updateMediaPackageCommentCatalog(MediaPackage mediaPackage, List<Comment> comments)
          throws CommentException, IOException {
    // Get the comments catalog
    Catalog[] commentCatalogs = mediaPackage.getCatalogs(MediaPackageElements.COMMENTS);
    Catalog c = null;
    if (commentCatalogs.length == 1)
      c = commentCatalogs[0];

    if (comments.size() > 0) {
      // If no comments catalog found, create a new one
      if (c == null) {
        c = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .newElement(Type.Catalog, MediaPackageElements.COMMENTS);
        c.setIdentifier(UUID.randomUUID().toString());
        mediaPackage.add(c);
      }

      // Update comments catalog
      InputStream in = null;
      try {
        String commentCatalog = CommentParser.getAsXml(comments);
        in = IOUtils.toInputStream(commentCatalog, "UTF-8");
        URI uri = getWorkspace().put(mediaPackage.getIdentifier().toString(), c.getIdentifier(), "comments.xml", in);
        c.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        c.setChecksum(null);
      } finally {
        IOUtils.closeQuietly(in);
      }
    } else {
      // Remove comments catalog
      if (c != null) {
        mediaPackage.remove(c);
        try {
          getWorkspace().delete(c.getURI());
        } catch (NotFoundException e) {
          logger.warn("Comments catalog {} not found to delete!", c.getURI());
        }
      }
    }
  }

  @GET
  @Path("{eventId}/media.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventmedia", description = "Returns all the data related to the media tab in the event details modal as JSON", returnDescription = "All the data related to the event media tab as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the event media tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventMedia(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getEvent(id);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

    Opt<MediaPackage> mpOpt = getIndexService().getEventMediapackage(optEvent.get());

    List<JValue> tracksJSON = new ArrayList<JValue>();
    if (mpOpt.isSome()) {
      for (Track track : mpOpt.get().getTracks()) {
        tracksJSON.add(j(f("id", vN(track.getIdentifier())), f("type", vN(track.getFlavor().toString())),
                f("mimetype", vN(track.getMimeType())), f("url", vN(track.getURI()))));
      }
    }

    return okJson(a(tracksJSON));
  }

  @GET
  @Path("{eventId}/media/{trackId}.json")
  @RestQuery(name = "geteventtrack", description = "Returns all the data related to the media/track tab in the event details modal as JSON", returnDescription = "All the data related to the given track for the media tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "trackId", description = "The track id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the given track for the media tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventTrack(@PathParam("eventId") String eventId, @PathParam("trackId") String trackId)
          throws Exception {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    JValue result = Jsons.jz;
    Opt<MediaPackage> mpOpt = getIndexService().getEventMediapackage(optEvent.get());
    if (mpOpt.isSome()) {

      Track track = mpOpt.get().getTrack(trackId);
      if (track == null)
        return notFound("Cannot find a track with id '%s' on event with id '%s'.", trackId, eventId);

      org.opencastproject.mediapackage.Stream[] streams = track.getStreams();
      List<JValue> audioStreamsJSON = new ArrayList<JValue>();
      List<JValue> videoStreamsJSON = new ArrayList<JValue>();
      for (org.opencastproject.mediapackage.Stream stream : streams) {

        if (stream instanceof AudioStreamImpl) {
          AudioStream audioStream = (AudioStream) stream;
          // TODO There is a bug with the stream ids, see MH-10325, so ignoring for now
          JField id = f("id", vN(audioStream.getIdentifier()));

          audioStreamsJSON.add(j(f("type", vN(audioStream.getFormat())), f("channels", vN(audioStream.getChannels())),
                  f("bitrate", vN(audioStream.getBitRate()))));
        } else if (stream instanceof VideoStreamImpl) {
          VideoStream videoStream = (VideoStream) stream;
          // TODO There is a bug with the stream ids, see MH-10325, so ignoring for now
          JField id = f("id", vN(videoStream.getIdentifier()));

          videoStreamsJSON.add(j(f("type", vN(videoStream.getFormat())), f("bitrate", v(videoStream.getBitRate())),
                  f("framerate", vN(videoStream.getFrameRate())),
                  f("resolution", vN(videoStream.getFrameWidth() + "x" + videoStream.getFrameHeight()))));
        } else {
          throw new IllegalArgumentException("stream must be either audio or video");
        }
      }
      result = j(f("id", vN(track.getIdentifier())), f("type", vN(track.getElementType())),
              f("duration", vN(track.getDuration())), f("mimetype", vN(track.getMimeType())),
              f("flavor", vN(track.getFlavor())), f("url", vN(track.getURI())),
              f("description", vN(track.getDescription())), f("tags", vN(StringUtils.join(track.getTags(), ","))),
              f("streams", j(f("audio", a(audioStreamsJSON)), f("video", a(videoStreamsJSON)))));
    }
    return okJson(result);
  }

  @GET
  @Path("{eventId}/attachments.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventattachements", description = "Returns all the data related to the attachements tab in the event details modal as JSON", returnDescription = "All the data related to the event attachements tab as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the event attachements tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventAttachements(@PathParam("eventId") String id) throws Exception {
    Opt<Event> optEvent = getEvent(id);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

    Opt<MediaPackage> mp = getIndexService().getEventMediapackage(optEvent.get());

    List<JValue> attachementsJSON = new ArrayList<JValue>();
    if (mp.isSome()) {
      for (Attachment attachement : mp.get().getAttachments()) {
        attachement.getMediaPackage();
        attachementsJSON.add(j(f("id", vN(attachement.getIdentifier())),
                f("type", vN(attachement.getFlavor().toString())), f("mimetype", vN(attachement.getMimeType())),
                f("tags", vN(StringUtils.join(attachement.getTags(), ","))), f("url", vN(attachement.getURI()))));
      }
    }

    return okJson(a(attachementsJSON));
  }

  @GET
  @Path("{eventId}/workflows.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventworkflows", description = "Returns all the data related to the workflows tab in the event details modal as JSON", returnDescription = "All the data related to the event workflows tab as JSON", pathParameters = { @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the event workflows tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventWorkflows(@PathParam("eventId") String id) throws WorkflowDatabaseException,
          JobEndpointException, SearchIndexException {
    Opt<Event> optEvent = getEvent(id);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", id);

    WorkflowQuery query = new WorkflowQuery().withMediaPackage(id);

    try {
      return okJson(getJobService().getTasksAsJSON(query));
    } catch (NotFoundException e) {
      return notFound("Not able to found workflows for event %s", id);
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
          throws WorkflowDatabaseException, JobEndpointException, SearchIndexException {
    Opt<Event> optEvent = getEvent(eventId);
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
      return notFound("Not able to found workflow  %s", workflowId);
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
          throws WorkflowDatabaseException, JobEndpointException, SearchIndexException {
    Opt<Event> optEvent = getEvent(eventId);
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
      return notFound("Not able to found workflow  %s", workflowId);
    }
  }

  @GET
  @Path("{eventId}/workflows/{workflowId}/operations/{operationId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventoperation", description = "Returns all the data related to the workflow/operation tab in the event details modal as JSON", returnDescription = "All the data related to the event workflow/opertation tab as JSON", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "workflowId", description = "The workflow id", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "operationId", description = "The operation id", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns all the data related to the event workflow/operation tab as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Unable to parse workflowId or operationId", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventOperation(@PathParam("eventId") String eventId, @PathParam("workflowId") String workflowId,
          @PathParam("operationId") String operationId) throws WorkflowDatabaseException, JobEndpointException,
          SearchIndexException {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    long workflowInstanceId;
    try {
      workflowInstanceId = Long.parseLong(workflowId);
    } catch (Exception e) {
      logger.warn("Unable to parse workflow id {}", workflowId);
      return RestUtil.R.badRequest();
    }
    long operationInstanceId;
    try {
      operationId = StringUtils.remove(operationId, ".json");
      operationInstanceId = Long.parseLong(operationId);
    } catch (Exception e) {
      logger.warn("Unable to parse operation id {}", operationId);
      return RestUtil.R.badRequest();
    }

    try {
      return okJson(getJobService().getOperationAsJSON(workflowInstanceId, operationInstanceId));
    } catch (NotFoundException e) {
      return notFound("Not able to found workflow  %s", workflowId);
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
          @Context HttpServletRequest req) throws WorkflowDatabaseException, JobEndpointException, SearchIndexException {
    // the call to #getEvent should make sure that the calling user has access rights to the workflow
    // FIXME since there is no dependency between the event and the workflow (the fetched event is
    // simply ignored) an attacker can get access by using an event he owns and a workflow ID of
    // someone else.
    for (final Event ignore : getEvent(eventId)) {
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
        return notFound("Not able to find the incident for the workflow %s", workflowId);
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
          @PathParam("errorId") String errorId, @Context HttpServletRequest req) throws WorkflowDatabaseException,
          JobEndpointException, SearchIndexException {
    // the call to #getEvent should make sure that the calling user has access rights to the workflow
    // FIXME since there is no dependency between the event and the workflow (the fetched event is
    // simply ignored) an attacker can get access by using an event he owns and a workflow ID of
    // someone else.
    for (Event ignore : getEvent(eventId)) {
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
        return notFound("Not able to find the incident %s", errorId);
      }
    }
    return notFound("Cannot find an event with id '%s'.", eventId);
  }

  @GET
  @Path("{eventId}/access.json")
  @SuppressWarnings("unchecked")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEventAccessInformation", description = "Get the access information of an event", returnDescription = "The access information", pathParameters = { @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."),
          @RestResponse(responseCode = SC_OK, description = "The access information ") })
  public Response getEventAccessInformation(@PathParam("eventId") String eventId) throws Exception {
    Opt<Event> optEvent = getEvent(eventId);
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
            && WorkflowUtil.isActive(WorkflowState.valueOf(optEvent.get().getWorkflowState())))
      episodeAccessJson.put("locked", true);

    JSONObject jsonReturnObj = new JSONObject();
    jsonReturnObj.put("episode_access", episodeAccessJson);
    jsonReturnObj.put("system_acls", systemAclsJson);

    return Response.ok(jsonReturnObj.toString()).build();
  }

  @POST
  @Path("{eventId}/transitions")
  @RestQuery(name = "addEventTransition", description = "Adds an ACL transition to an event", returnDescription = "The method doesn't return any content", pathParameters = { @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING) }, restParameters = { @RestParameter(name = "transition", isRequired = true, description = "The transition (JSON object) to add", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required params were missing in the request."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found.") })
  public Response addEventTransition(@PathParam("eventId") String eventId, @FormParam("transition") String transitionStr)
          throws SearchIndexException {
    if (StringUtils.isBlank(eventId) || StringUtils.isBlank(transitionStr))
      return RestUtil.R.badRequest("Missing parameters");

    Opt<Event> optEvent = getEvent(eventId);
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
          @RestParameter(name = "transitionId", isRequired = true, description = "The transition identifier", type = RestParameter.Type.INTEGER) }, restParameters = { @RestParameter(name = "transition", isRequired = true, description = "The updated transition (JSON object)", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required params were missing in the request."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event or transtion has not been found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content") })
  public Response updateEventTransition(@PathParam("eventId") String eventId,
          @PathParam("transitionId") long transitionId, @FormParam("transition") String transitionStr)
          throws NotFoundException, SearchIndexException {
    if (StringUtils.isBlank(transitionStr))
      return RestUtil.R.badRequest("Missing parameters");

    Opt<Event> optEvent = getEvent(eventId);
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
      logger.error("Unable to update transtion {} of event {}: {}", new String[] { Long.toString(transitionId),
              eventId, ExceptionUtils.getStackTrace(e) });
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    } catch (ParseException e) {
      // That should never happen
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{eventId}/optout/{optout}")
  @RestQuery(name = "updateEventOptoutStatus", description = "Updates an event's opt out status.", returnDescription = "The method doesn't return any content", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING),
          @RestParameter(name = "optout", isRequired = true, description = "True or false, true to opt out of this recording.", type = RestParameter.Type.BOOLEAN) }, restParameters = {}, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The event has not been found"),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content") })
  public Response updateEventOptOut(@PathParam("eventId") String eventId, @PathParam("optout") boolean optout)
          throws NotFoundException {
    try {
      return changeOptOutStatus(eventId, optout);
    } catch (NotFoundException e) {
      throw e;
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
        Response response = changeOptOutStatus(eventId, optout);
        if (response.getStatus() == HttpStatus.SC_NO_CONTENT) {
          result.addOk(eventId);
        } else if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
          result.addNotFound(eventId);
        }
      } catch (NotFoundException e) {
        result.addNotFound(idObject.toString());
      } catch (Exception e) {
        logger.error("Could not update opt out status of event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
        result.addServerError(idObject.toString());
      }

    }
    return Response.ok(result.toJson()).build();
  }

  /**
   * Changes the opt out status of a single event (by its mediapackage id)
   *
   * @param eventId
   *          The event's unique id formally the mediapackage id
   * @param optout
   *          Whether the event should be moved into optted out.
   * @return A HTTP Response of no content if everything is okay, not found or server error if not.
   */
  private Response changeOptOutStatus(String eventId, boolean optout) throws NotFoundException, SchedulerException,
          SearchIndexException {
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    getSchedulerService().updateOptOutStatus(eventId, optout);
    logger.debug("Setting event {} to opt out status of {}", eventId, optout);
    return Response.noContent().build();
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
    Opt<Event> optEvent = getEvent(eventId);
    if (optEvent.isNone())
      return notFound("Cannot find an event with id '%s'.", eventId);

    try {
      getAclService().deleteEpisodeTransition(transitionId);
      return Response.noContent().build();
    } catch (AclServiceException e) {
      logger.error("Error while trying to delete transition '{}' from event '{}': {}",
              new String[] { Long.toString(transitionId), eventId, ExceptionUtils.getStackTrace(e) });
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("new/metadata")
  @RestQuery(name = "getNewMetadata", description = "Returns all the data related to the metadata tab in the new event modal as JSON", returnDescription = "All the data related to the event metadata tab as JSON", reponses = { @RestResponse(responseCode = SC_OK, description = "Returns all the data related to the event metadata tab as JSON") })
  public Response getNewMetadata() {
    MetadataList metadataList = getMetadatListWithAllEventCatalogUIAdapters();
    Opt<AbstractMetadataCollection> optMetadataByAdapter = metadataList
            .getMetadataByAdapter(getEpisodeCatalogUIAdapter());
    if (optMetadataByAdapter.isSome()) {
      AbstractMetadataCollection collection = optMetadataByAdapter.get();
      collection.removeField(collection.getOutputFields().get("created"));
      collection.removeField(collection.getOutputFields().get("duration"));
      collection.removeField(collection.getOutputFields().get("uid"));
      collection.removeField(collection.getOutputFields().get("source"));
      collection.removeField(collection.getOutputFields().get("startDate"));
      collection.removeField(collection.getOutputFields().get("startTime"));
      collection.removeField(collection.getOutputFields().get("agent"));
      metadataList.add(getEpisodeCatalogUIAdapter(), collection);
    }
    return okJson(metadataList.toJSON());
  }

  @GET
  @Path("new/processing")
  @RestQuery(name = "getNewProcessing", description = "Returns all the data related to the processing tab in the new event modal as JSON", returnDescription = "All the data related to the event processing tab as JSON", restParameters = { @RestParameter(name = "tags", isRequired = false, description = "A comma separated list of tags to filter the workflow definitions", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "Returns all the data related to the event processing tab as JSON") })
  public Response getNewProcessing(@QueryParam("tags") String tagsString) {
    List<String> tags = RestUtil.splitCommaSeparatedParam(Option.option(tagsString)).value();

    // This is the JSON Object which will be returned by this request
    List<JValue> actions = new ArrayList<JValue>();
    try {
      List<WorkflowDefinition> workflowsDefinitions = getWorkflowService().listAvailableWorkflowDefinitions();
      for (WorkflowDefinition wflDef : workflowsDefinitions) {
        if (wflDef.containsTag(tags)) {

          actions.add(j(f("id", v(wflDef.getId())), f("title", v(Opt.nul(wflDef.getTitle()).or(""))),
                  f("description", v(Opt.nul(wflDef.getDescription()).or(""))),
                  f("configuration_panel", v(Opt.nul(wflDef.getConfigurationPanel()).or("")))));
        }
      }
    } catch (WorkflowDatabaseException e) {
      logger.error("Unable to get available workflow definitions: {}", ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    return okJson(a(actions));
  }

  @POST
  @Path("new/conflicts")
  @RestQuery(name = "checkNewConflicts", description = "Checks if the current scheduler parameters are in a conflict with another event", returnDescription = "Returns NO CONTENT if no event are in conflict within specified period or list of conflicting recordings in JSON", restParameters = { @RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "There is a conflict"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters") })
  public Response getNewConflicts(@FormParam("metadata") String metadata) throws NotFoundException {
    if (StringUtils.isBlank(metadata)) {
      logger.warn("Metadata is not specified");
      return Response.status(Status.BAD_REQUEST).build();
    }

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

    String rrule = (String) metadataJson.get("rrule");

    String timezone = null;
    String durationString = null;
    if (StringUtils.isNotEmpty(rrule)) {
      try {
        RRule rule = new RRule(rrule);
        rule.validate();
      } catch (Exception e) {
        logger.warn("Unable to parse rrule {}: {}", rrule, e.getMessage());
        return Response.status(Status.BAD_REQUEST).build();
      }

      durationString = (String) metadataJson.get("duration");
      if (StringUtils.isBlank(durationString)) {
        logger.warn("If checking recurrence, must include duration.");
        return Response.status(Status.BAD_REQUEST).build();
      }

      Agent agent = getCaptureAgentStateService().getAgent(device);
      timezone = agent.getConfiguration().getProperty("capture.device.timezone");
      if (StringUtils.isBlank(timezone)) {
        timezone = TimeZone.getDefault().getID();
        logger.warn("No 'capture.device.timezone' set on agent {}. The default server timezone {} will be used.",
                device, timezone);
      }
    }

    try {
      DublinCoreCatalogList events = null;
      if (StringUtils.isNotEmpty(rrule)) {
        events = getSchedulerService().findConflictingEvents(device, rrule, start, end, Long.parseLong(durationString),
                timezone);
      } else {
        events = getSchedulerService().findConflictingEvents(device, start, end);
      }
      if (!events.getCatalogList().isEmpty()) {
        List<JValue> eventsJSON = new ArrayList<JValue>();
        for (DublinCoreCatalog event : events.getCatalogList()) {
          eventsJSON.add(j(f("time", v(event.getFirst(DublinCoreCatalog.PROPERTY_TEMPORAL))),
                  f("title", v(event.getFirst(DublinCoreCatalog.PROPERTY_TITLE)))));
        }

        return conflictJson(a(eventsJSON));
      } else {
        return Response.noContent().build();
      }
    } catch (Exception e) {
      logger.error("Unable to find conflicting events for {}, {}, {}: {}", new String[] { device, startDate, endDate,
              ExceptionUtils.getStackTrace(e) });
      return RestUtil.R.serverError();
    }
  }

  @POST
  @Path("/new")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "createNewEvent", description = "Creates a new event by the given metadata as JSON and the files in the body", returnDescription = "The workflow identifier", restParameters = { @RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT) }, reponses = {
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

  /**
   * Get a single event
   *
   * @param id
   *          the mediapackage id
   * @return an event or none if not found wrapped in an option
   * @throws SearchIndexException
   */
  public Opt<Event> getEvent(String id) throws SearchIndexException {
    SearchResult<Event> result = getIndex().getByQuery(
            new EventSearchQuery(getSecurityService().getOrganization().getId(), getSecurityService().getUser())
                    .withIdentifier(id));
    // If the results list if empty, we return already a response.
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find event with id {}", id);
      return Opt.<Event> none();
    }
    Event event = result.getItems()[0].getSource();
    event.updatePreview(getPreviewSubtype());
    return Opt.some(event);
  }

  @GET
  @Path("events.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getevents", description = "Returns all the events as JSON", returnDescription = "All the events as JSON", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING),
          @RestParameter(name = "sort", description = "The order instructions used to sort the query result. Must be in the form '<field name>:(ASC|DESC)'", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page.", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The page number.", isRequired = false, type = RestParameter.Type.INTEGER) }, reponses = { @RestResponse(description = "Returns all events as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response getEvents(@QueryParam("id") String id, @QueryParam("commentReason") String reasonFilter,
          @QueryParam("commentResolution") String resolutionFilter, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {

    Option<Integer> optLimit = Option.option(limit);
    Option<Integer> optOffset = Option.option(offset);
    Option<String> optSort = Option.option(trimToNull(sort));
    ArrayList<JValue> eventsList = new ArrayList<JValue>();
    EventSearchQuery query = new EventSearchQuery(getSecurityService().getOrganization().getId(), getSecurityService()
            .getUser());

    // If the limit is set to 0, this is not taken into account
    if (optLimit.isSome() && limit == 0) {
      optLimit = Option.none();
    }

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      if (EventListQuery.FILTER_PRESENTERS_NAME.equals(name))
        query.withPresenter(filters.get(name));
      if (EventListQuery.FILTER_CONTRIBUTORS_NAME.equals(name))
        query.withContributor(filters.get(name));
      if (EventListQuery.FILTER_LOCATION_NAME.equals(name))
        query.withLocation(filters.get(name));
      if (EventListQuery.FILTER_TEXT_NAME.equals(name))
        query.withText("*" + filters.get(name) + "*");
      if (EventListQuery.FILTER_SERIES_NAME.equals(name))
        query.withSeriesId(filters.get(name));
      if (EventListQuery.FILTER_STATUS_NAME.equals(name))
        query.withEventStatus(filters.get(name));
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
          query.withStartFrom(fromAndToCreationRange.getA());
          query.withStartTo(fromAndToCreationRange.getB());
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
    RESOLUTION resolution = null;
    if (StringUtils.isNotBlank(resolutionFilter)) {
      try {
        resolution = RESOLUTION.valueOf(resolutionFilter);
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
      logger.error("The admin UI Search Index was not able to get the events list: {}", e);
      return RestUtil.R.serverError();
    }

    // If the results list if empty, we return already a response.
    if (results.getPageSize() == 0) {
      logger.debug("No events match the given filters.");
      return okJsonList(eventsList, Opt.nul(offset).or(0), Opt.nul(limit).or(0), 0);
    }

    for (SearchResultItem<Event> item : results.getItems()) {
      Event source = item.getSource();
      source.updatePreview(getPreviewSubtype());
      eventsList.add(eventToJSON(source));
    }

    return okJsonList(eventsList, Opt.nul(offset).or(0), Opt.nul(limit).or(0), results.getHitCount());
  }

  // --

  private void updateCommentCatalog(final Event event, final List<Comment> comments) throws Exception {
    final Opt<MediaPackage> mpOpt = getIndexService().getEventMediapackage(event);
    if (mpOpt.isNone())
      return;

    final SecurityContext securityContext = new SecurityContext(getSecurityService(), getSecurityService()
            .getOrganization(), getSecurityService().getUser());
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        securityContext.runInContext(new Effect0() {
          @Override
          protected void run() {
            try {
              MediaPackage mediaPackage = mpOpt.get();
              updateMediaPackageCommentCatalog(mediaPackage, comments);
              switch (getIndexService().getEventSource(event)) {
                case WORKFLOW:
                  logger.info("Update workflow mediapacakge {} with updated comments catalog.", event.getIdentifier());
                  WorkflowInstance workflowInstance = getIndexService().getCurrentWorkflowInstance(
                          event.getIdentifier());
                  workflowInstance.setMediaPackage(mediaPackage);
                  getIndexService().updateWorkflowInstance(workflowInstance);
                  break;
                case ARCHIVE:
                  logger.info("Update archive mediapacakge {} with updated comments catalog.", event.getIdentifier());
                  getArchive().add(mediaPackage);
                  break;
                default:
                  logger.error("Unkown event source {}!", event.getSource().toString());
              }
            } catch (Exception e) {
              logger.error("Unable to update event {} comment catalog: {}", event.getIdentifier(),
                      ExceptionUtils.getStackTrace(e));
            }
          }
        });
      }
    });
  }

  private URI getCommentUrl(String eventId, long commentId) {
    return UrlSupport.uri(serverUrl, eventId, "comment", Long.toString(commentId));
  }

  private JValue eventToJSON(Event event) {
    List<JField> fields = new ArrayList<JField>();

    fields.add(f("id", v(event.getIdentifier())));
    fields.add(f("title", vN(event.getTitle())));
    fields.add(f("source", vN(event.getSource())));
    fields.add(f("presenters", jsonArrayFromList(event.getPresenters())));
    if (StringUtils.isNotBlank(event.getSeriesId())) {
      String seriesTitle = event.getSeriesName();
      String seriesID = event.getSeriesId();

      fields.add(f("series", j(f("id", vN(seriesID)), f("title", vN(seriesTitle)))));
    }
    fields.add(f("location", vN(event.getLocation())));

    fields.add(f("start_date", vN(event.getRecordingStartDate())));
    if (event.getDuration() != null) {
      try {
        long endTime = DateTimeSupport.fromUTC(event.getRecordingStartDate()) + event.getDuration();
        fields.add(f("end_date", v(DateTimeSupport.toUTC(endTime))));
      } catch (Exception e) {
        logger.error("Unable to parse start time {}", event.getRecordingStartDate());
      }
    }

    String schedulingStatus = event.getSchedulingStatus() == null ? null : "EVENTS.EVENTS.SCHEDULING_STATUS."
            + event.getSchedulingStatus();
    fields.add(f("managedAcl", vN(event.getManagedAcl())));
    fields.add(f("scheduling_status", vN(schedulingStatus)));
    fields.add(f("workflow_state", vN(event.getWorkflowState())));
    fields.add(f("review_status", vN(event.getReviewStatus())));
    fields.add(f("event_status", v(event.getEventStatus())));
    fields.add(f("source", v(getIndexService().getEventSource(event).toString())));
    fields.add(f("has_comments", v(event.hasComments())));
    fields.add(f("has_open_comments", v(event.hasOpenComments())));
    fields.add(f("has_preview", v(event.hasPreview())));
    fields.add(f("publications", j(f("Engage", v("http://engage.localdomain")))));

    return j(fields);
  }

  private static final Fn<Publication, JObjectWrite> publicationToJson = new Fn<Publication, JObjectWrite>() {
    @Override
    public JObjectWrite ap(Publication publication) {
      Opt<String> channel = Opt.nul(PUBLICATION_CHANNELS.get(publication.getChannel()));
      return j(f("name", v(channel.or("EVENTS.EVENTS.DETAILS.GENERAL.CUSTOM"))),
              f("url", v(publication.getURI().toString())));
    }
  };

  private final Function<Recording, List<Person>> getRecipients = new Function<Recording, List<Person>>() {
    @Override
    public List<Person> apply(Recording a) {
      return a.getStaff();
    }
  };

  private final Fn<Publication, Boolean> internalChannelFilter = new Fn<Publication, Boolean>() {
    @Override
    public Boolean ap(Publication a) {
      if (PublishInternalWorkflowOperationHandler.CHANNEL_ID.equals(a.getChannel()))
        return false;
      return true;
    }
  };

}
