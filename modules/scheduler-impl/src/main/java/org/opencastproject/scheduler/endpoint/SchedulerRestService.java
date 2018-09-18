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

package org.opencastproject.scheduler.endpoint;

import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Stream.$;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.capture.CaptureParameters.AGENT_REGISTRATION_TYPE;
import static org.opencastproject.capture.CaptureParameters.AGENT_REGISTRATION_TYPE_ADHOC;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SPATIAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.util.Jsons.arr;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.Jsons.v;
import static org.opencastproject.util.RestUtil.generateErrorResponse;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.SchedulerConflictException;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.scheduler.api.SchedulerService.SchedulerTransaction;
import org.opencastproject.scheduler.api.SchedulerTransactionLockException;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.impl.CaptureNowProlongingService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Arr;
import org.opencastproject.util.Jsons.Prop;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

/**
 * REST Endpoint for Scheduler Service
 */
@Path("/")
@RestService(name = "schedulerservice", title = "Scheduler Service", abstractText = "This service creates, edits and retrieves and helps managing scheduled capture events.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class SchedulerRestService {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerRestService.class);

  /** Key for the default workflow definition in config.properties */
  private static final String DEFAULT_WORKFLOW_DEFINITION = "org.opencastproject.workflow.default.definition";

  private SchedulerService service;
  private CaptureAgentStateService agentService;
  private CaptureNowProlongingService prolongingService;
  private Workspace workspace;

  private String defaultWorkflowDefinitionId;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;
  protected String serviceUrl = null;

  /**
   * Method to set the service this REST endpoint uses
   *
   * @param service
   */
  public void setService(SchedulerService service) {
    this.service = service;
  }

  /**
   * Method to unset the service this REST endpoint uses
   *
   * @param service
   */
  public void unsetService(SchedulerService service) {
    this.service = null;
  }

  /**
   * Method to set the prolonging service this REST endpoint uses
   *
   * @param prolongingService
   */
  public void setProlongingService(CaptureNowProlongingService prolongingService) {
    this.prolongingService = prolongingService;
  }

  /**
   * Method to unset the prolonging service this REST endpoint uses
   *
   * @param prolongingService
   */
  public void unsetProlongingService(CaptureNowProlongingService prolongingService) {
    this.prolongingService = null;
  }

  /**
   * Method to set the capture agent state service this REST endpoint uses
   *
   * @param agentService
   */
  public void setCaptureAgentStateService(CaptureAgentStateService agentService) {
    this.agentService = agentService;
  }

  /**
   * Method to unset the capture agent state service this REST endpoint uses
   *
   * @param agentService
   */
  public void unsetCaptureAgentStateService(CaptureAgentStateService agentService) {
    this.agentService = null;
  }

  /**
   * Method to set the workspace this REST endpoint uses
   *
   * @param workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * The method that will be called, if the service will be activated
   *
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
      logger.debug("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
      defaultWorkflowDefinitionId = StringUtils
              .trimToNull(cc.getBundleContext().getProperty(DEFAULT_WORKFLOW_DEFINITION));
      if (defaultWorkflowDefinitionId == null)
        defaultWorkflowDefinitionId = "schedule-and-upload";
    }
  }

  /**
   * Gets a XML with the media package for the specified event.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return media package XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{id:.+}/mediapackage.xml")
  @RestQuery(name = "getmediapackagexml", description = "Retrieves media package for specified event", returnDescription = "media package in XML", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of event for which media package will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "DublinCore of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getMediaPackageXml(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      MediaPackage result = service.getMediaPackage(eventId);
      return Response.ok(MediaPackageParser.getAsXml(result)).build();
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets a XML with the Dublin Core metadata for the specified event.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return Dublin Core XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{id:.+}/dublincore.xml")
  @RestQuery(name = "recordingsasxml", description = "Retrieves DublinCore for specified event", returnDescription = "DublinCore in XML", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of event for which DublinCore will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "DublinCore of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getDublinCoreMetadataXml(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      DublinCoreCatalog result = service.getDublinCore(eventId);
      return Response.ok(result.toXmlString()).build();
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to retrieve event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets a Dublin Core metadata for the specified event as JSON.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return Dublin Core JSON for the event
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id:.+}/dublincore.json")
  @RestQuery(name = "recordingsasjson", description = "Retrieves DublinCore for specified event", returnDescription = "DublinCore in JSON", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of event for which DublinCore will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "DublinCore of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getDublinCoreMetadataJSON(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      DublinCoreCatalog result = service.getDublinCore(eventId);
      return Response.ok(result.toJson()).build();
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to retrieve event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets a XML with the media package for the specified event.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return media package XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{id:.+}/technical.json")
  @RestQuery(name = "gettechnicalmetadatajson", description = "Retrieves the technical metadata for specified event", returnDescription = "technical metadata as JSON", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of event for which the technical metadata will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "technical metadata of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getTechnicalMetadataJSON(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      TechnicalMetadata metadata = service.getTechnicalMetadata(eventId);

      Val state = v("");
      Val lastHeard = v("");
      if (metadata.getRecording().isSome()) {
        state = v(metadata.getRecording().get().getState());
        lastHeard = v(DateTimeSupport.toUTC(metadata.getRecording().get().getLastCheckinTime()));
      }

      Arr presenters = arr(mlist(metadata.getPresenters()).map(Jsons.stringVal));
      List<Prop> wfProperties = new ArrayList<>();
      for (Entry<String, String> entry : metadata.getWorkflowProperties().entrySet()) {
        wfProperties.add(p(entry.getKey(), entry.getValue()));
      }
      List<Prop> agentConfig = new ArrayList<>();
      for (Entry<String, String> entry : metadata.getCaptureAgentConfiguration().entrySet()) {
        agentConfig.add(p(entry.getKey(), entry.getValue()));
      }
      return RestUtil.R.ok(obj(p("id", metadata.getEventId()), p("location", metadata.getAgentId()),
              p("start", DateTimeSupport.toUTC(metadata.getStartDate().getTime())),
              p("end", DateTimeSupport.toUTC(metadata.getEndDate().getTime())), p("optOut", metadata.isOptOut()),
              p("presenters", presenters), p("wfProperties", obj(wfProperties.toArray(new Prop[wfProperties.size()]))),
              p("agentConfig", obj(agentConfig.toArray(new Prop[agentConfig.size()]))), p("state", state),
              p("lastHeardFrom", lastHeard)));
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}/acl")
  @RestQuery(name = "getaccesscontrollist", description = "Retrieves the access control list for specified event", returnDescription = "The access control list", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of event for which the access control list will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The access control list as JSON "),
                  @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "The event has no access control list"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getAccessControlList(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      AccessControlList accessControlList = service.getAccessControlList(eventId);
      if (accessControlList != null) {
        return Response.ok(AccessControlParser.toJson(accessControlList)).type(MediaType.APPLICATION_JSON_TYPE).build();
      } else {
        return Response.noContent().build();
      }
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to retrieve access control list of event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets the workflow configuration for the specified event.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return the workflow configuration
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id:.+}/workflow.properties")
  @RestQuery(name = "recordingsagentproperties", description = "Retrieves workflow configuration for specified event", returnDescription = "workflow configuration in the form of key, value pairs", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of event for which workflow configuration will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "workflow configuration of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getWorkflowConfiguration(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      Map<String, String> result = service.getWorkflowConfig(eventId);
      String serializedProperties = serializeProperties(result);
      return Response.ok(serializedProperties).build();
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve workflow configuration for event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets java Properties file with technical metadata for the specified event.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return Java Properties File with the metadata for the event
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id:.+}/agent.properties")
  @RestQuery(name = "recordingsagentproperties", description = "Retrieves Capture Agent properties for specified event", returnDescription = "Capture Agent properties in the form of key, value pairs", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of event for which agent properties will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Capture Agent properties of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getCaptureAgentMetadata(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      Map<String, String> result = service.getCaptureAgentConfiguration(eventId);
      String serializedProperties = serializeProperties(result);
      return Response.ok(serializedProperties).build();
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/optOut")
  @RestQuery(name = "recordingoptoutstatus", description = "Retrieves the opt out status for specified event", returnDescription = "The opt out status", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage id for which the opt out status will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The opt out status of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to get the event opt out status. Maybe you need to authenticate.") })
  public Response getOptOut(@PathParam("id") String mediaPackageId) throws UnauthorizedException {
    try {
      boolean optOut = service.isOptOut(mediaPackageId);
      return Response.ok(Boolean.toString(optOut)).build();
    } catch (NotFoundException e) {
      logger.info("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   *
   * Removes the specified event. Returns true if the event was found and could be removed.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @DELETE
  @Path("{id:.+}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "deleterecordings", description = "Removes scheduled event with specified ID.", returnDescription = "OK if event were successfully removed or NOT FOUND if event with specified ID does not exist", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Event ID", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully removed"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate."),
                  @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "Event with specified ID is locked by a transaction, unable to delete event.") })
  public Response deleteEvent(@PathParam("id") String eventId) throws UnauthorizedException {
    try {
      service.removeEvent(eventId);
      return Response.status(Response.Status.OK).build();
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (SchedulerTransactionLockException e) {
      return Response.status(Status.CONFLICT).build();
    } catch (Exception e) {
      logger.error("Unable to delete event with id '{}': {}", eventId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets the iCalendar with all (even old) events for the specified filter.
   *
   * @param captureAgentId
   *          The ID that specifies the capture agent.
   * @param seriesId
   *          The ID that specifies series.
   *
   * @return an iCalendar
   */
  @GET
  @Produces("text/calendar")
  // NOTE: charset not supported by current jaxrs impl (is ignored), set explicitly in response
  @Path("calendars")
  @RestQuery(name = "getcalendar", description = "Returns iCalendar for specified set of events", returnDescription = "ICalendar for events", restParameters = {
          @RestParameter(name = "agentid", description = "Filter events by capture agent", isRequired = false, type = Type.STRING),
          @RestParameter(name = "seriesid", description = "Filter events by series", isRequired = false, type = Type.STRING),
          @RestParameter(name = "cutoff", description = "A cutoff date in UNIX milliseconds to limit the number of events returned in the calendar.", isRequired = false, type = Type.INTEGER) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_MODIFIED, description = "Events were not modified since last request"),
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Events were modified, new calendar is in the body") })
  public Response getCalendar(@QueryParam("agentid") String captureAgentId, @QueryParam("seriesid") String seriesId,
          @QueryParam("cutoff") Long cutoff, @Context HttpServletRequest request) {
    Date endDate = null;
    if (cutoff != null) {
      try {
        endDate = new Date(cutoff);
      } catch (NumberFormatException e) {
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    try {
      String lastModified = null;
      // If the etag matches the if-not-modified header,return a 304
      if (StringUtils.isNotBlank(captureAgentId)) {
        lastModified = service.getScheduleLastModified(captureAgentId);
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (StringUtils.isNotBlank(ifNoneMatch) && ifNoneMatch.equals(lastModified)) {
          return Response.notModified(lastModified).expires(null).build();
        }
      }

      String result = service.getCalendar(Opt.nul(StringUtils.trimToNull(captureAgentId)),
              Opt.nul(StringUtils.trimToNull(seriesId)), Opt.nul(endDate));

      ResponseBuilder response = Response.ok(result).header(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=UTF-8");
      if (StringUtils.isNotBlank(lastModified))
        response.header(HttpHeaders.ETAG, lastModified);
      return response.build();
    } catch (Exception e) {
      logger.error("Unable to get calendar for capture agent '{}': {}", captureAgentId, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/reviewStatus")
  @RestQuery(name = "recordingreviewstatus", description = "Retrieves the review status for specified event", returnDescription = "The review status", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage id for which the review status will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The review status of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getReviewStatus(@PathParam("id") String mediaPackageId) throws UnauthorizedException {
    try {
      ReviewStatus reviewStatus = service.getReviewStatus(mediaPackageId);
      return Response.ok(reviewStatus.toString()).build();
    } catch (NotFoundException e) {
      logger.info("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{id}/reviewStatus")
  @RestQuery(name = "updatereviewstatus", description = "Updates the review status of the event with the given mediapackage id", returnDescription = "Status OK is returned if event was successfully updated", pathParameters = {
          @RestParameter(name = "id", description = "ID of events mediapackage", isRequired = true, type = Type.STRING) }, restParameters = {
                  @RestParameter(name = "reviewStatus", isRequired = false, description = "The review status to set: [UNSENT, UNCONFIRMED, CONFIRMED]", type = Type.STRING) }, reponses = {
                          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
                          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
                          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "review status could not be parsed"),
                          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response updateReviewStatus(@PathParam("id") String mpId, @FormParam("reviewStatus") String reviewStatusString)
          throws UnauthorizedException {
    ReviewStatus reviewStatus;
    try {
      reviewStatus = ReviewStatus.valueOf(reviewStatusString);
    } catch (Exception e) {
      logger.info("Unable to parse review status {}", reviewStatusString);
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      service.updateReviewStatus(mpId, reviewStatus);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.info("Event with mediapackage id '{}' does not exist.", mpId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to update event with mediapackage id '{}': {}", mpId, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/blacklisted")
  @RestQuery(name = "recordingblackliststatus", description = "Retrieves the blacklist status for specified event", returnDescription = "The blacklist status", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage id for which the blacklist status will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The blacklist status of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response getBlacklistStatus(@PathParam("id") String mediaPackageId) throws UnauthorizedException {
    try {
      boolean blacklisted = service.isBlacklisted(mediaPackageId);
      return Response.ok(Boolean.toString(blacklisted)).build();
    } catch (NotFoundException e) {
      logger.info("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/lastmodified")
  @RestQuery(name = "agentlastmodified", description = "Retrieves the last modified hash for specified agent", returnDescription = "The last modified hash", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of capture agent for which the last modified hash will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The last modified hash of agent is in the body of response") })
  public Response getLastModified(@PathParam("id") String agentId) {
    try {
      String lastModified = service.getScheduleLastModified(agentId);
      return Response.ok(lastModified).build();
    } catch (Exception e) {
      logger.error("Unable to retrieve agent last modified hash of agent id '{}': {}", agentId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/removeOldScheduledRecordings")
  @RestQuery(name = "removeOldScheduledRecordings", description = "This will find and remove any scheduled events before the buffer time to keep performance in the scheduler optimum.", returnDescription = "No return value", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Removed old scheduled recordings."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse buffer."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to remove old schedulings. Maybe you need to authenticate.") }, restParameters = {
                  @RestParameter(name = "buffer", type = RestParameter.Type.INTEGER, defaultValue = "604800", isRequired = true, description = "The amount of seconds before now that a capture has to have stopped capturing. It must be 0 or greater.") })
  public Response removeOldScheduledRecordings(@FormParam("buffer") long buffer) throws UnauthorizedException {
    if (buffer < 0) {
      return Response.status(SC_BAD_REQUEST).build();
    }

    try {
      service.removeScheduledRecordingsBeforeBuffer(buffer);
    } catch (SchedulerException e) {
      logger.error("Error while trying to remove old scheduled recordings", e);
      throw new WebApplicationException(e);
    }
    return Response.ok().build();
  }


  /**
   * Creates new event based on parameters. All times and dates are in milliseconds.
   */
  @POST
  @Path("/")
  @RestQuery(name = "newrecording", description = "Creates new event with specified parameters",
          returnDescription = "If an event was successfully created",
          restParameters = {
          @RestParameter(name = "start", isRequired = true, type = Type.INTEGER, description = "The start date of the event in milliseconds from 1970-01-01T00:00:00Z"),
          @RestParameter(name = "end", isRequired = true, type = Type.INTEGER, description = "The end date of the event in milliseconds from 1970-01-01T00:00:00Z"),
          @RestParameter(name = "agent", isRequired = true, type = Type.STRING, description = "The agent of the event"),
          @RestParameter(name = "users", isRequired = false, type = Type.STRING, description = "Comma separated list of user ids (speakers/lecturers) for the event"),
          @RestParameter(name = "mediaPackage", isRequired = true, type = Type.TEXT, description = "The media package of the event"),
          @RestParameter(name = "wfproperties", isRequired = false, type = Type.TEXT, description = "Workflow "
                  + "configuration keys for the event. Each key will be prefixed by 'org.opencastproject.workflow"
                  + ".config.' and added to the capture agent parameters."),
          @RestParameter(name = "agentparameters", isRequired = false, type = Type.TEXT, description = "The capture agent properties for the event"),
          @RestParameter(name = "optOut", isRequired = false, type = Type.BOOLEAN, description = "The opt out status of the event"),
          @RestParameter(name = "source", isRequired = false, type = Type.STRING, description = "The scheduling source of the event"),
          @RestParameter(name = "origin", isRequired = false, type = Type.STRING, description = "The origin")
          }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Event is successfully created"),
          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "Unable to create event, conflicting events found (ConflicsFound)"),
          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "Unable to create event, event locked by a transaction  (TransactionLock)"),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to create the event. Maybe you need to authenticate."),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid information for this request") })
  public Response addEvent(@FormParam("start") long startTime, @FormParam("end") long endTime,
          @FormParam("agent") String agentId, @FormParam("users") String users,
          @FormParam("mediaPackage") String mediaPackageXml, @FormParam("wfproperties") String workflowProperties,
          @FormParam("agentparameters") String agentParameters, @FormParam("optOut") Boolean optOut,
          @FormParam("source") String schedulingSource, @FormParam("origin") String origin)
                  throws UnauthorizedException {
    if (StringUtils.isBlank(origin))
      origin = SchedulerService.ORIGIN;

    if (endTime <= startTime || startTime < 0) {
      logger.debug("Cannot add event without proper start and end time");
      return RestUtil.R.badRequest("Cannot add event without proper start and end time");
    }

    if (StringUtils.isBlank(agentId)) {
      logger.debug("Cannot add event without agent identifier");
      return RestUtil.R.badRequest("Cannot add event without agent identifier");
    }

    if (StringUtils.isBlank(mediaPackageXml)) {
      logger.debug("Cannot add event without media package");
      return RestUtil.R.badRequest("Cannot add event without media package");
    }

    MediaPackage mediaPackage;
    try {
      mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
    } catch (MediaPackageException e) {
      logger.debug("Could not parse media package", e);
      return RestUtil.R.badRequest("Could not parse media package");
    }

    String eventId = mediaPackage.getIdentifier().compact();

    Map<String, String> caProperties = new HashMap<>();
    if (StringUtils.isNotBlank(agentParameters)) {
      try {
        Properties prop = parseProperties(agentParameters);
        caProperties.putAll((Map) prop);
      } catch (Exception e) {
        logger.info("Could not parse capture agent properties: {}", agentParameters);
        return RestUtil.R.badRequest("Could not parse capture agent properties");
      }
    }

    Map<String, String> wfProperties = new HashMap<>();
    if (StringUtils.isNotBlank(workflowProperties)) {
      try {
        Properties prop = parseProperties(workflowProperties);
        wfProperties.putAll((Map) prop);
      } catch (IOException e) {
        logger.info("Could not parse workflow configuration properties: {}", workflowProperties);
        return RestUtil.R.badRequest("Could not parse workflow configuration properties");
      }
    }
    Set<String> userIds = new HashSet<>();
    String[] ids = StringUtils.split(users, ",");
    if (ids != null)
      userIds.addAll(Arrays.asList(ids));

    DateTime startDate = new DateTime(startTime).toDateTime(DateTimeZone.UTC);
    DateTime endDate = new DateTime(endTime).toDateTime(DateTimeZone.UTC);

    try {
      service.addEvent(startDate.toDate(), endDate.toDate(), agentId, userIds, mediaPackage, wfProperties, caProperties,
              Opt.nul(optOut), Opt.nul(schedulingSource), origin);
      return Response.status(Status.CREATED)
              .header("Location", serverUrl + serviceUrl + '/' + eventId + "/mediapackage.xml").build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (SchedulerTransactionLockException | SchedulerConflictException e) {
      return Response.status(Status.CONFLICT).entity(generateErrorResponse(e)).type(MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      logger.error("Unable to create new event with id '{}'", eventId, e);
      return Response.serverError().build();
    }
  }

  /**
   * Creates new event based on parameters. All times and dates are in milliseconds.
   */
  @POST
  @Path("/multiple")
  @RestQuery(name = "newrecordings", description = "Creates new event with specified parameters",
          returnDescription = "If an event was successfully created",
          restParameters = {
                  @RestParameter(name = "rrule", isRequired = true, type = Type.STRING, description = "The recurrence rule for the events"),
                  @RestParameter(name = "start", isRequired = true, type = Type.INTEGER, description = "The start date of the event in milliseconds from 1970-01-01T00:00:00Z"),
                  @RestParameter(name = "end", isRequired = true, type = Type.INTEGER, description = "The end date of the event in milliseconds from 1970-01-01T00:00:00Z"),
                  @RestParameter(name = "duration", isRequired = true, type = Type.INTEGER, description = "The duration of the events in milliseconds"),
                  @RestParameter(name = "tz", isRequired = true, type = Type.INTEGER, description = "The timezone of the events"),
                  @RestParameter(name = "agent", isRequired = true, type = Type.STRING, description = "The agent of the event"),
                  @RestParameter(name = "users", isRequired = false, type = Type.STRING, description = "Comma separated list of user ids (speakers/lecturers) for the event"),
                  @RestParameter(name = "templateMp", isRequired = true, type = Type.TEXT, description = "The template mediapackage for the events"),
                  @RestParameter(name = "wfproperties", isRequired = false, type = Type.TEXT, description = "Workflow "
                          + "configuration keys for the event. Each key will be prefixed by 'org.opencastproject.workflow"
                          + ".config.' and added to the capture agent parameters."),
                  @RestParameter(name = "agentparameters", isRequired = false, type = Type.TEXT, description = "The capture agent properties for the event"),
                  @RestParameter(name = "optOut", isRequired = false, type = Type.BOOLEAN, description = "The opt out status of the event"),
                  @RestParameter(name = "source", isRequired = false, type = Type.STRING, description = "The scheduling source of the event"),
                  @RestParameter(name = "origin", isRequired = false, type = Type.STRING, description = "The origin")
          }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Event is successfully created"),
          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "Unable to create event, conflicting events found (ConflicsFound)"),
          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "Unable to create event, event locked by a transaction  (TransactionLock)"),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to create the event. Maybe you need to authenticate."),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid information for this request") })
  public Response addMultipleEvents(@FormParam("rrule") String rruleString, @FormParam("start") long startTime,
          @FormParam("end") long endTime, @FormParam("duration") long duration, @FormParam("tz") String tzString,
          @FormParam("agent") String agentId, @FormParam("users") String users,
          @FormParam("templateMp") MediaPackage templateMp, @FormParam("wfproperties") String workflowProperties,
          @FormParam("agentparameters") String agentParameters, @FormParam("optOut") Boolean optOut,
          @FormParam("source") String schedulingSource, @FormParam("origin") String origin)
          throws UnauthorizedException {
    if (StringUtils.isBlank(origin))
      origin = SchedulerService.ORIGIN;

    if (endTime <= startTime || startTime < 0) {
      logger.debug("Cannot add event without proper start and end time");
      return RestUtil.R.badRequest("Cannot add event without proper start and end time");
    }

    RRule rrule;
    try {
      rrule = new RRule(rruleString);
    } catch (ParseException e) {
      logger.debug("Could not parse recurrence rule");
      return RestUtil.R.badRequest("Could not parse recurrence rule");
    }

    if (duration < 1) {
      logger.debug("Cannot schedule events with durations less than 1");
      return RestUtil.R.badRequest("Cannot schedule events with durations less than 1");
    }

    if (StringUtils.isBlank(tzString)) {
      logger.debug("Cannot schedule events with blank timezone");
      return RestUtil.R.badRequest("Cannot schedule events with blank timezone");
    }
    TimeZone tz = TimeZone.getTimeZone(tzString);

    if (StringUtils.isBlank(agentId)) {
      logger.debug("Cannot add event without agent identifier");
      return RestUtil.R.badRequest("Cannot add event without agent identifier");
    }

    Map<String, String> caProperties = new HashMap<>();
    if (StringUtils.isNotBlank(agentParameters)) {
      try {
        Properties prop = parseProperties(agentParameters);
        caProperties.putAll((Map) prop);
      } catch (Exception e) {
        logger.info("Could not parse capture agent properties: {}", agentParameters);
        return RestUtil.R.badRequest("Could not parse capture agent properties");
      }
    }

    Map<String, String> wfProperties = new HashMap<>();
    if (StringUtils.isNotBlank(workflowProperties)) {
      try {
        Properties prop = parseProperties(workflowProperties);
        wfProperties.putAll((Map) prop);
      } catch (IOException e) {
        logger.info("Could not parse workflow configuration properties: {}", workflowProperties);
        return RestUtil.R.badRequest("Could not parse workflow configuration properties");
      }
    }
    Set<String> userIds = new HashSet<>();
    String[] ids = StringUtils.split(users, ",");
    if (ids != null)
      userIds.addAll(Arrays.asList(ids));

    DateTime startDate = new DateTime(startTime).toDateTime(DateTimeZone.UTC);
    DateTime endDate = new DateTime(endTime).toDateTime(DateTimeZone.UTC);

    try {
      service.addMultipleEvents(rrule, startDate.toDate(), endDate.toDate(), duration, tz, agentId, userIds, templateMp, wfProperties, caProperties,
              Opt.nul(optOut), Opt.nul(schedulingSource), origin);
      return Response.status(Status.CREATED).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (SchedulerTransactionLockException | SchedulerConflictException e) {
      return Response.status(Status.CONFLICT).entity(generateErrorResponse(e)).type(MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      logger.error("Unable to create new events", e);
      return Response.serverError().build();
    }
  }

  @PUT
  @Path("{id}")
  @RestQuery(name = "updaterecordings", description = "Updates specified event", returnDescription = "Status OK is returned if event was successfully updated, NOT FOUND if specified event does not exist or BAD REQUEST if data is missing or invalid", pathParameters = {
          @RestParameter(name = "id", description = "ID of event to be updated", isRequired = true, type = Type.STRING) }, restParameters = {
                  @RestParameter(name = "start", isRequired = false, description = "Updated start date for event", type = Type.INTEGER),
                  @RestParameter(name = "end", isRequired = false, description = "Updated end date for event", type = Type.INTEGER),
                  @RestParameter(name = "agent", isRequired = false, description = "Updated agent for event", type = Type.STRING),
                  @RestParameter(name = "users", isRequired = false, type = Type.STRING, description = "Updated comma separated list of user ids (speakers/lecturers) for the event"),
                  @RestParameter(name = "mediaPackage", isRequired = false, description = "Updated media package for event", type = Type.TEXT),
                  @RestParameter(name = "wfproperties", isRequired = false, description = "Workflow configuration properties", type = Type.TEXT),
                  @RestParameter(name = "agentparameters", isRequired = false, description = "Updated Capture Agent properties", type = Type.TEXT),
                  @RestParameter(name = "updateOptOut", isRequired = true, defaultValue = "false", description = "Whether to update the opt out status", type = Type.BOOLEAN),
                  @RestParameter(name = "optOut", isRequired = false, description = "Update opt out status", type = Type.BOOLEAN),
                  @RestParameter(name = "origin", isRequired = false, description = "The origin", type = Type.STRING) }, reponses = {
                          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
                          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
                          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "Unable to update event, conflicting events found (ConflicsFound)"),
                          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "Unable to update event, event locked by a transaction (TransactionLock)"),
                          @RestResponse(responseCode = HttpServletResponse.SC_FORBIDDEN, description = "Event with specified ID cannot be updated"),
                          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to update the event. Maybe you need to authenticate."),
                          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Data is missing or invalid") })
  public Response updateEvent(@PathParam("id") String eventID, @FormParam("start") Long startTime,
          @FormParam("end") Long endTime, @FormParam("agent") String agentId, @FormParam("users") String users,
          @FormParam("mediaPackage") String mediaPackageXml, @FormParam("wfproperties") String workflowProperties,
          @FormParam("agentparameters") String agentParameters, @FormParam("updateOptOut") boolean updateOptOut,
          @FormParam("optOut") Boolean optOutBoolean, @FormParam("origin") String origin) throws UnauthorizedException {
    if (StringUtils.isBlank(origin))
      origin = SchedulerService.ORIGIN;

    if (startTime != null) {
      if (startTime < 0) {
        logger.debug("Cannot add event with negative start time ({} < 0)", startTime);
        return RestUtil.R.badRequest("Cannot add event with negative start time");
      }
      if (endTime != null && endTime <= startTime) {
        logger.debug("Cannot add event without proper end time ({} <= {})", startTime, endTime);
        return RestUtil.R.badRequest("Cannot add event without proper end time");
      }
    }

    MediaPackage mediaPackage = null;
    if (StringUtils.isNotBlank(mediaPackageXml)) {
      try {
        mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      } catch (Exception e) {
        logger.debug("Could not parse media packagey", e);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    Map<String, String> caProperties = null;
    if (StringUtils.isNotBlank(agentParameters)) {
      try {
        Properties prop = parseProperties(agentParameters);
        caProperties = new HashMap<>();
        caProperties.putAll((Map) prop);
      } catch (Exception e) {
        logger.debug("Could not parse capture agent properties: {}", agentParameters, e);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    Map<String, String> wfProperties = null;
    if (StringUtils.isNotBlank(workflowProperties)) {
      try {
        Properties prop = parseProperties(workflowProperties);
        wfProperties = new HashMap<>();
        wfProperties.putAll((Map) prop);
      } catch (IOException e) {
        logger.debug("Could not parse workflow configuration properties: {}", workflowProperties, e);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    Set<String> userIds = null;
    String[] ids = StringUtils.split(StringUtils.trimToNull(users), ",");
    if (ids != null) {
      userIds = new HashSet<>(Arrays.asList(ids));
    }

    Date startDate = null;
    if (startTime != null) {
      startDate = new DateTime(startTime).toDateTime(DateTimeZone.UTC).toDate();
    }

    Date endDate = null;
    if (endTime != null) {
      endDate = new DateTime(endTime).toDateTime(DateTimeZone.UTC).toDate();
    }

    final Opt<Opt<Boolean>> optOut;
    if (updateOptOut) {
      optOut = Opt.some(Opt.nul(optOutBoolean));
    } else {
      optOut = Opt.none();
    }
    try {
      service.updateEvent(eventID, Opt.nul(startDate), Opt.nul(endDate), Opt.nul(StringUtils.trimToNull(agentId)),
              Opt.nul(userIds), Opt.nul(mediaPackage), Opt.nul(wfProperties), Opt.nul(caProperties), optOut, origin);
      return Response.ok().build();
    } catch (SchedulerTransactionLockException | SchedulerConflictException e) {
      return Response.status(Status.CONFLICT).entity(generateErrorResponse(e)).type(MediaType.APPLICATION_JSON).build();
    } catch (SchedulerException e) {
      logger.warn("Error updating event with id '{}'", eventID, e);
      return Response.status(Status.FORBIDDEN).build();
    } catch (NotFoundException e) {
      logger.info("Event with id '{}' does not exist.", eventID);
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to update event with id '{}'", eventID, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @Path("recordings.{type:xml|json}")
  @RestQuery(name = "recordingsaslist", description = "Searches recordings and returns result as XML or JSON", returnDescription = "XML or JSON formated results",
       pathParameters = {
          @RestParameter(name = "type", isRequired = true, description = "The media type of the response [xml|json]", type = Type.STRING) },
       restParameters = {
          @RestParameter(name = "agent", description = "Search by device", isRequired = false, type = Type.STRING),
          @RestParameter(name = "startsfrom", description = "Search by when does event start", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "startsto", description = "Search by when does event start", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "endsfrom", description = "Search by when does event finish", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "endsto", description = "Search by when does event finish", isRequired = false, type = Type.INTEGER) },
       reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Search completed, results returned in body") })
  public Response getEventsAsList(@PathParam("type") final String type, @QueryParam("agent") String device,
          @QueryParam("startsfrom") Long startsFromTime,
          @QueryParam("startsto") Long startsToTime, @QueryParam("endsfrom") Long endsFromTime,
          @QueryParam("endsto") Long endsToTime) throws UnauthorizedException {
    Date startsfrom = null;
    Date startsTo = null;
    Date endsFrom = null;
    Date endsTo = null;
    if (startsFromTime != null)
      startsfrom = new DateTime(startsFromTime).toDateTime(DateTimeZone.UTC).toDate();
    if (startsToTime != null)
      startsTo = new DateTime(startsToTime).toDateTime(DateTimeZone.UTC).toDate();
    if (endsFromTime != null)
      endsFrom = new DateTime(endsFromTime).toDateTime(DateTimeZone.UTC).toDate();
    if (endsToTime != null)
      endsTo = new DateTime(endsToTime).toDateTime(DateTimeZone.UTC).toDate();

    try {
      List<MediaPackage> events = service.search(Opt.nul(StringUtils.trimToNull(device)), Opt.nul(startsfrom),
              Opt.nul(startsTo), Opt.nul(endsFrom), Opt.nul(endsTo));
      if ("json".equalsIgnoreCase(type)) {
        return Response.ok(getEventListAsJsonString(events)).build();
      } else {
        return Response.ok(MediaPackageParser.getArrayAsXml(events)).build();
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to perform search: {}", getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("conflicts.json")
  @RestQuery(name = "conflictingrecordingsasjson", description = "Searches for conflicting recordings based on parameters", returnDescription = "Returns NO CONTENT if no recordings are in conflict within specified period or list of conflicting recordings in JSON", restParameters = {
          @RestParameter(name = "agent", description = "Device identifier for which conflicts will be searched", isRequired = true, type = Type.STRING),
          @RestParameter(name = "start", description = "Start time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
          @RestParameter(name = "end", description = "End time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
          @RestParameter(name = "rrule", description = "Rule for recurrent conflicting, specified as: \"FREQ=WEEKLY;BYDAY=day(s);BYHOUR=hour;BYMINUTE=minute\". FREQ is required. BYDAY may include one or more (separated by commas) of the following: SU,MO,TU,WE,TH,FR,SA.", isRequired = false, type = Type.STRING),
          @RestParameter(name = "duration", description = "If recurrence rule is specified duration of each conflicting period, in milliseconds", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "timezone", description = "The timezone of the capture device", isRequired = false, type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Found conflicting events, returned in body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "Not authorized to make this request"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "A detailed stack track of the internal issue.")})
  public Response getConflictingEventsJson(@QueryParam("agent") String device, @QueryParam("rrule") String rrule,
          @QueryParam("start") Long startDate, @QueryParam("end") Long endDate, @QueryParam("duration") Long duration,
          @QueryParam("timezone") String timezone) throws UnauthorizedException {
    try {
      List<MediaPackage> events = getConflictingEvents(device, rrule, startDate, endDate, duration, timezone);
      if (!events.isEmpty()) {
        String eventsJsonString = getEventListAsJsonString(events);
        return Response.ok(eventsJsonString).build();
      } else {
        return Response.noContent().build();
      }
    } catch (IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to find conflicting events for {}, {}, {}, {}, {}: {}",
              device, rrule, startDate, endDate, duration, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @Path("conflicts.{type:xml|json}")
  @RestQuery(name = "conflictingrecordings", description = "Searches for conflicting recordings based on parameters and returns result as XML or JSON", returnDescription = "Returns NO CONTENT if no recordings are in conflict within specified period or list of conflicting recordings in XML or JSON",
       pathParameters = {
           @RestParameter(name = "type", isRequired = true, description = "The media type of the response [xml|json]", type = Type.STRING) },
       restParameters = {
           @RestParameter(name = "agent", description = "Device identifier for which conflicts will be searched", isRequired = true, type = Type.STRING),
           @RestParameter(name = "start", description = "Start time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
           @RestParameter(name = "end", description = "End time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
           @RestParameter(name = "rrule", description = "Rule for recurrent conflicting, specified as: \"FREQ=WEEKLY;BYDAY=day(s);BYHOUR=hour;BYMINUTE=minute\". FREQ is required. BYDAY may include one or more (separated by commas) of the following: SU,MO,TU,WE,TH,FR,SA.", isRequired = false, type = Type.STRING),
           @RestParameter(name = "duration", description = "If recurrence rule is specified duration of each conflicting period, in milliseconds", isRequired = false, type = Type.INTEGER),
           @RestParameter(name = "timezone", description = "The timezone of the capture device", isRequired = false, type = Type.STRING) }, reponses = {
           @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
           @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Found conflicting events, returned in body of response"),
           @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters"),
           @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "Not authorized to make this request"),
           @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "A detailed stack track of the internal issue.")})
  public Response getConflicts(@PathParam("type") final String type, @QueryParam("agent") String device, @QueryParam("rrule") String rrule,
          @QueryParam("start") Long startDate, @QueryParam("end") Long endDate, @QueryParam("duration") Long duration,
          @QueryParam("timezone") String timezone) throws UnauthorizedException {
    try {
      List<MediaPackage> events = getConflictingEvents(device, rrule, startDate, endDate, duration, timezone);
      if (!events.isEmpty()) {
        if ("json".equalsIgnoreCase(type)) {
          return Response.ok(getEventListAsJsonString(events)).build();
        } else {
          return Response.ok(MediaPackageParser.getArrayAsXml(events)).build();
        }
      } else {
        return Response.noContent().build();
      }
    } catch (IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to find conflicting events for {}, {}, {}, {}, {}: {}",
              device, rrule, startDate, endDate, duration, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{id}/recordingStatus")
  @RestQuery(name = "updateRecordingState", description = "Set the status of a given recording, registering it if it is new", pathParameters = {
          @RestParameter(description = "The ID of a given recording", isRequired = true, name = "id", type = Type.STRING) }, restParameters = {
                  @RestParameter(description = "The state of the recording. Must be one of the following: unknown, capturing, capture_finished, capture_error, manifest, manifest_error, manifest_finished, compressing, compressing_error, uploading, upload_finished, upload_error.", isRequired = true, name = "state", type = Type.STRING) }, reponses = {
                          @RestResponse(description = "{id} set to {state}", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "{id} or state {state} is empty or the {state} is not known", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                          @RestResponse(description = "Recording with {id} could not be found", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response updateRecordingState(@PathParam("id") String id, @FormParam("state") String state)
          throws NotFoundException {
    if (StringUtils.isEmpty(id) || StringUtils.isEmpty(state))
      return Response.serverError().status(Response.Status.BAD_REQUEST).build();

    try {
      if (service.updateRecordingState(id, state)) {
        return Response.ok(id + " set to " + state).build();
      } else {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
    } catch (SchedulerException e) {
      logger.debug("Unable to set recording state of {}: {}", id, getStackTrace(e));
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}/recordingStatus")
  @RestQuery(name = "getRecordingState", description = "Return the state of a given recording", pathParameters = {
          @RestParameter(description = "The ID of a given recording", isRequired = true, name = "id", type = Type.STRING) }, restParameters = {}, reponses = {
                  @RestResponse(description = "Returns the state of the recording with the correct id", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The recording with the specified ID does not exist", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response getRecordingState(@PathParam("id") String id) throws NotFoundException {
    try {
      Recording rec = service.getRecordingState(id);
      return RestUtil.R
              .ok(obj(p("id", rec.getID()), p("state", rec.getState()), p("lastHeardFrom", rec.getLastCheckinTime())));
    } catch (SchedulerException e) {
      logger.debug("Unable to get recording state of {}: {}", id, getStackTrace(e));
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("{id}/recordingStatus")
  @RestQuery(name = "removeRecording", description = "Remove record of a given recording", pathParameters = {
          @RestParameter(description = "The ID of a given recording", isRequired = true, name = "id", type = Type.STRING) }, restParameters = {}, reponses = {
                  @RestResponse(description = "{id} removed", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "{id} is empty", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "Recording with {id} could not be found", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response removeRecording(@PathParam("id") String id) throws NotFoundException {
    if (StringUtils.isEmpty(id))
      return Response.serverError().status(Response.Status.BAD_REQUEST).build();

    try {
      service.removeRecording(id);
      return Response.ok(id + " removed").build();
    } catch (SchedulerException e) {
      logger.debug("Unable to remove recording with id '{}': {}", id, getStackTrace(e));
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("recordingStatus")
  @RestQuery(name = "getAllRecordings", description = "Return all registered recordings and their state", pathParameters = {}, restParameters = {}, reponses = {
          @RestResponse(description = "Returns all known recordings.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getAllRecordings() {
    try {
      List<Val> update = new ArrayList<>();
      for (Entry<String, Recording> e : service.getKnownRecordings().entrySet()) {
        update.add(obj(p("id", e.getValue().getID()), p("state", e.getValue().getState()),
                p("lastHeardFrom", e.getValue().getLastCheckinTime())));
      }
      return RestUtil.R.ok(arr(update).toJson());
    } catch (SchedulerException e) {
      logger.debug("Unable to get all recordings: {}", getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   *
   *
   *
   * Transaction API
   *
   *
   *
   *
   */

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("transaction/{id}")
  @RestQuery(name = "gettransaction", description = "Retrieves scheduler transaction for specified id", returnDescription = "The scheduler transaction as JSON", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of scheduler transaction", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Scheduler transaction is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Scheduler transaction with specified ID does not exist") })
  public Response getTransaction(@PathParam("id") String transactionId) {
    try {
      SchedulerTransaction transaction = service.getTransaction(transactionId);
      return RestUtil.R.ok(obj(p("id", transaction.getId()), p("source", transaction.getSource())));
    } catch (NotFoundException e) {
      logger.info("Scheduler transaction with id '{}' does not exist.", transactionId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve scheduler transaction with id '{}': {}", transactionId, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("transaction/source/{source}")
  @RestQuery(name = "gettransactionbysource", description = "Retrieves scheduler transaction for specified source", returnDescription = "The scheduler transaction as JSON", pathParameters = {
          @RestParameter(name = "source", isRequired = true, description = "Source of scheduler transaction", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Scheduler transaction is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Scheduler transaction with specified ID does not exist") })
  public Response getTransactionBySource(@PathParam("source") String source) {
    try {
      SchedulerTransaction transaction = service.getTransactionBySource(source);
      return RestUtil.R.ok(obj(p("id", transaction.getId()), p("source", transaction.getSource())));
    } catch (NotFoundException e) {
      logger.info("Scheduler transaction with source '{}' does not exist.", source);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve scheduler transaction with source '{}': {}", source, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("transaction/event/{id}")
  @RestQuery(name = "gettransactionstatusbyevent", description = "Retrieves the active transaction status for specified event", returnDescription = "The active transaction status", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage id for which the active transaction status will be retrieved", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The active transaction status of event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Scheduler transaction with specified ID does not exist"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to get the event transaction status. Maybe you need to authenticate.") })
  public Response getTransactionStatusByEvent(@PathParam("id") String mediaPackageId) throws UnauthorizedException {
    try {
      return Response.ok(Boolean.toString(service.hasActiveTransaction(mediaPackageId))).build();
    } catch (NotFoundException e) {
      logger.info("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (SchedulerException e) {
      logger.error("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, getMessage(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/transaction")
  @RestQuery(name = "createtransaction", description = "Creates a new scheduler transaction with specified source", returnDescription = "The scheduler transaction as JSON", reponses = {
          @RestResponse(responseCode = SC_OK, description = "New scheduler transaction created"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to create a scheduler transaction. Maybe you need to authenticate."),
          @RestResponse(responseCode = SC_CONFLICT, description = "New scheduler transaction created"), }, restParameters = {
                  @RestParameter(name = "source", type = RestParameter.Type.STRING, isRequired = true, description = "The scheduling source") })
  public Response createTransaction(@FormParam("source") String schedulingSource) throws UnauthorizedException {
    try {
      SchedulerTransaction transaction = service.createTransaction(schedulingSource);
      return RestUtil.R.ok(obj(p("id", transaction.getId()), p("source", transaction.getSource())));
    } catch (SchedulerConflictException e) {
      return Response.status(Status.CONFLICT).build();
    } catch (SchedulerException e) {
      logger.error("Unable to create transaction: {}", getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/transaction/cleanup")
  @RestQuery(name = "cleanuptransaction", description = "Cleanup scheduler transactions", returnDescription = "The scheduler transactions has been cleaned up", reponses = {
          @RestResponse(responseCode = SC_OK, description = "The scheduler transactions has been cleaned up"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to cleanup scheduler transactions. Maybe you need to authenticate.") })
  public Response cleanupTransactions() throws UnauthorizedException {
    try {
      service.cleanupTransactions();
      return RestUtil.R.ok();
    } catch (SchedulerException e) {
      logger.error("Unable to cleanup transactions: {}", getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/transaction/{id}/commit")
  @RestQuery(name = "committransaction", description = "Commits the scheduler transaction with specified id", returnDescription = "Successfully committed scheduler transaction", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Successfully committed scheduler transaction"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Scheduler transaction with specified ID does not exist"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to commit the scheduler transaction. Maybe you need to authenticate.") }, pathParameters = {
                  @RestParameter(name = "id", type = RestParameter.Type.STRING, isRequired = true, description = "ID of scheduler transaction") })
  public Response commitTransaction(@PathParam("id") String transactionId)
          throws UnauthorizedException, NotFoundException {
    try {
      SchedulerTransaction transaction = service.getTransaction(transactionId);
      transaction.commit();
      return Response.ok().build();
    } catch (SchedulerException e) {
      logger.error("Unable to commit scheduler transaction '{}': {}", transactionId, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/transaction/{id}/rollback")
  @RestQuery(name = "rollbacktransaction", description = "Rolls back the scheduler transaction with specified id", returnDescription = "Successfully rolled back scheduler transaction", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Successfully rolled back scheduler transaction"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Scheduler transaction with specified ID does not exist"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to rollback the scheduler transaction. Maybe you need to authenticate.") }, pathParameters = {
                  @RestParameter(name = "id", type = RestParameter.Type.STRING, isRequired = true, description = "ID of scheduler transaction") })
  public Response rollbackTransaction(@PathParam("id") String transactionId)
          throws UnauthorizedException, NotFoundException {
    try {
      SchedulerTransaction transaction = service.getTransaction(transactionId);
      transaction.rollback();
      return Response.ok().build();
    } catch (SchedulerException e) {
      logger.error("Unable to rollback scheduler transaction '{}': {}", transactionId, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("/transaction/{id}/add")
  @RestQuery(name = "transactionaddevent", description = "Commits the scheduler transaction with specified id", returnDescription = "Successfully committed scheduler transaction", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Successfully committed scheduler transaction"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Scheduler transaction with specified ID does not exist"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to commit the scheduler transaction. Maybe you need to authenticate.") }, pathParameters = {
                  @RestParameter(name = "id", type = RestParameter.Type.STRING, isRequired = true, description = "ID of scheduler transaction") }, restParameters = {
                          @RestParameter(name = "start", isRequired = true, type = Type.INTEGER, description = "The start date of the event"),
                          @RestParameter(name = "end", isRequired = true, type = Type.INTEGER, description = "The end date of the event"),
                          @RestParameter(name = "agent", isRequired = true, type = Type.STRING, description = "The agent of the event"),
                          @RestParameter(name = "users", isRequired = false, type = Type.STRING, description = "Comma separated list of user ids (speakers/lecturers) for the event"),
                          @RestParameter(name = "mediaPackage", isRequired = true, type = Type.TEXT, description = "The media package of the event"),
                          @RestParameter(name = "wfproperties", isRequired = false, type = Type.TEXT, description = "The workflow properties for the event"),
                          @RestParameter(name = "agentparameters", isRequired = false, type = Type.TEXT, description = "The capture agent properties for the event"),
                          @RestParameter(name = "optOut", isRequired = false, type = Type.BOOLEAN, description = "The opt out status of the event") })
  public Response addTransactionEvent(@PathParam("id") String transactionId, @FormParam("start") long startTime,
          @FormParam("end") long endTime, @FormParam("agent") String agentId, @FormParam("users") String users,
          @FormParam("mediaPackage") String mediaPackageXml, @FormParam("wfproperties") String workflowProperties,
          @FormParam("agentparameters") String agentParameters, @FormParam("optOut") Boolean optOut)
                  throws UnauthorizedException, NotFoundException {
    if (startTime < 0 || endTime < 0) {
      logger.info("Cannot add event without proper start and/or end time");
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (StringUtils.isBlank(agentId)) {
      logger.info("Cannot add event without agent identifier");
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (StringUtils.isBlank(mediaPackageXml)) {
      logger.info("Cannot add event without media package");
      return Response.status(Status.BAD_REQUEST).build();
    }

    MediaPackage mediaPackage;
    try {
      mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
    } catch (Exception e) {
      logger.info("Could not parse media package:", e);
      return Response.status(Status.BAD_REQUEST).build();
    }

    Map<String, String> caProperties = new HashMap<>();
    if (StringUtils.isNotBlank(agentParameters)) {
      try {
        Properties prop = parseProperties(agentParameters);
        caProperties.putAll((Map) prop);
      } catch (Exception e) {
        logger.info("Could not parse capture agent properties: {}", agentParameters);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    Map<String, String> wfProperties = new HashMap<>();
    if (StringUtils.isNotBlank(workflowProperties)) {
      try {
        Properties prop = parseProperties(workflowProperties);
        wfProperties.putAll((Map) prop);
      } catch (IOException e) {
        logger.info("Could not parse workflow configuration properties: {}", workflowProperties);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }
    Set<String> userIds = new HashSet<>();
    String[] ids = StringUtils.split(users, ",");
    if (ids != null)
      userIds.addAll(Arrays.asList(ids));

    DateTime startDate = new DateTime(startTime).toDateTime(DateTimeZone.UTC);
    DateTime endDate = new DateTime(endTime).toDateTime(DateTimeZone.UTC);

    try {
      SchedulerTransaction transaction = service.getTransaction(transactionId);
      transaction.addEvent(startDate.toDate(), endDate.toDate(), agentId, userIds, mediaPackage, wfProperties,
              caProperties, Opt.nul(optOut));
      return Response.ok().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to commit scheduler transaction '{}': {}", transactionId, getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   *
   *
   *
   * Prolonging service
   *
   *
   *
   *
   */

  @GET
  @Path("capture/{agent}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "currentcapture", description = "Get the current capture event catalog as JSON", returnDescription = "The current capture event catalog as JSON", pathParameters = {
          @RestParameter(name = "agent", isRequired = true, type = Type.STRING, description = "The agent identifier") }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "DublinCore of current capture event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "There is no ongoing recording"),
                  @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "The agent is not ready to communicate") })
  public Response currentCapture(@PathParam("agent") String agentId) throws NotFoundException {
    if (service == null || agentService == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Scheduler service is unavailable, please wait...").build();

    try {
      List<MediaPackage> search = service.search(Opt.some(agentId), Opt.<Date> none(), Opt.some(new Date()),
              Opt.some(new Date()), Opt.<Date> none());
      if (search.isEmpty()) {
        logger.info("No recording to stop found for agent '{}'!", agentId);
        throw new NotFoundException("No recording to stop found for agent: " + agentId);
      } else {
        DublinCoreCatalog catalog = DublinCoreUtil.loadEpisodeDublinCore(workspace, search.get(0)).get();
        return Response.ok(catalog.toJson()).build();
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to get the immediate recording for agent '{}': {}", agentId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("capture/{agent}/upcoming")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "upcomingcapture", description = "Get the upcoming capture event catalog as JSON", returnDescription = "The upcoming capture event catalog as JSON", pathParameters = {
          @RestParameter(name = "agent", isRequired = true, type = Type.STRING, description = "The agent identifier") }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "DublinCore of the upcomfing capture event is in the body of response"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "There is no upcoming recording"),
                  @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "The agent is not ready to communicate") })
  public Response upcomingCapture(@PathParam("agent") String agentId) throws NotFoundException {
    if (service == null || agentService == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Scheduler service is unavailable, please wait...").build();

    try {
      List<MediaPackage> search = service.search(Opt.some(agentId), Opt.some(new Date()), Opt.<Date> none(),
              Opt.<Date> none(), Opt.<Date> none());
      if (search.isEmpty()) {
        logger.info("No recording to stop found for agent '{}'!", agentId);
        throw new NotFoundException("No recording to stop found for agent: " + agentId);
      } else {
        DublinCoreCatalog catalog = DublinCoreUtil.loadEpisodeDublinCore(workspace, search.get(0)).get();
        return Response.ok(catalog.toJson()).build();
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to get the immediate recording for agent '{}': {}", agentId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("capture/{agent}")
  @RestQuery(name = "startcapture", description = "Create an immediate event", returnDescription = "If events were successfully generated, status CREATED is returned", pathParameters = {
          @RestParameter(name = "agent", isRequired = true, type = Type.STRING, description = "The agent identifier") }, restParameters = {
                  @RestParameter(name = "workflowDefinitionId", isRequired = false, type = Type.STRING, description = "The workflow definition id to use") }, reponses = {
                          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Recording started"),
                          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "There is no such agent"),
                          @RestResponse(responseCode = HttpServletResponse.SC_CONFLICT, description = "The agent is already recording"),
                          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to start this immediate capture. Maybe you need to authenticate."),
                          @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "The agent is not ready to communicate") })
  public Response startCapture(@PathParam("agent") String agentId, @FormParam("workflowDefinitionId") String wfId)
          throws NotFoundException, UnauthorizedException {
    if (service == null || agentService == null || prolongingService == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Scheduler service is unavailable, please wait...").build();

    // Lookup the agent. If it doesn't exist, add a temporary registration
    boolean adHocRegistration = false;
    try {
      agentService.getAgent(agentId);
    } catch (NotFoundException e) {
      Properties adHocProperties = new Properties();
      adHocProperties.put(AGENT_REGISTRATION_TYPE, AGENT_REGISTRATION_TYPE_ADHOC);
      agentService.setAgentConfiguration(agentId, adHocProperties);
      agentService.setAgentState(agentId, AgentState.CAPTURING);
      adHocRegistration = true;
      logger.info("Temporarily registered agent '{}' for ad-hoc recording", agentId);
    }

    try {
      Date now = new Date();
      Date temporaryEndDate = DateTime.now().plus(prolongingService.getInitialTime()).toDate();
      try {
        List<MediaPackage> events = service.findConflictingEvents(agentId, now, temporaryEndDate);
        if (!events.isEmpty()) {
          logger.info("An already existing event is in a conflict with the the one to be created on the agent {}!",
                  agentId);
          return Response.status(Status.CONFLICT).build();
        }
      } catch (SchedulerException e) {
        logger.error("Unable to create immediate event on agent {}: {}", agentId, e);
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
      }

      String workflowId = defaultWorkflowDefinitionId;
      if (StringUtils.isNotBlank(wfId))
        workflowId = wfId;

      Map<String, String> caProperties = new HashMap<>();
      caProperties.put("org.opencastproject.workflow.definition", workflowId);
      caProperties.put("event.location", agentId);
      caProperties.put("event.title", "Capture now event");
      // caProperties.put("org.opencastproject.workflow.config.captionHold", "false");
      // caProperties.put("org.opencastproject.workflow.config.archiveOp", "true");
      // caProperties.put("org.opencastproject.workflow.config.trimHold", "false");

      // TODO default metadata? configurable?
      // A temporal with start and end period is needed! As well PROPERTY_SPATIAL is needed
      DublinCoreCatalog eventCatalog = DublinCores.mkOpencastEpisode().getCatalog();
      eventCatalog.set(PROPERTY_TITLE, "Capture now event");
      eventCatalog.set(PROPERTY_TEMPORAL,
              EncodingSchemeUtils.encodePeriod(new DCMIPeriod(now, temporaryEndDate), Precision.Second));
      eventCatalog.set(PROPERTY_SPATIAL, agentId);
      eventCatalog.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
      // eventCatalog.set(PROPERTY_CREATOR, "demo");
      // eventCatalog.set(PROPERTY_SUBJECT, "demo");
      // eventCatalog.set(PROPERTY_LANGUAGE, "demo");
      // eventCatalog.set(PROPERTY_CONTRIBUTOR, "demo");
      // eventCatalog.set(PROPERTY_DESCRIPTION, "demo");

      // TODO workflow properties
      Map<String, String> wfProperties = new HashMap<>();

      MediaPackage mediaPackage = null;
      try {
        mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
        mediaPackage = addCatalog(workspace, IOUtils.toInputStream(eventCatalog.toXmlString(), "UTF-8"),
                "dublincore.xml", MediaPackageElements.EPISODE, mediaPackage);

        prolongingService.schedule(agentId);
        service.addEvent(now, temporaryEndDate, agentId, Collections.<String> emptySet(), mediaPackage, wfProperties,
                caProperties, Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
        return Response.status(Status.CREATED)
                .header("Location", serverUrl + serviceUrl + '/' + mediaPackage.getIdentifier().compact() + ".xml")
                .build();
      } catch (Exception e) {
        prolongingService.stop(agentId);
        if (e instanceof UnauthorizedException)
          throw (UnauthorizedException) e;
        logger.error("Unable to create immediate event on agent {}: {}", agentId, e);
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
      } finally {
        if (mediaPackage != null) {
          for (MediaPackageElement elem : $(mediaPackage.getElements())
                  .bind(MediaPackageSupport.Filters.byFlavor(MediaPackageElements.EPISODE).toFn())) {
            try {
              workspace.delete(elem.getURI());
            } catch (NotFoundException e) {
              logger.warn("Unable to find (and hence, delete), this mediapackage '{}' element '{}'",
                      mediaPackage.getIdentifier(), elem.getIdentifier());
            } catch (IOException e) {
              chuck(e);
            }
          }
        }
      }
    } catch (Throwable t) {
      throw t;
    } finally {
      if (adHocRegistration) {
        agentService.removeAgent(agentId);
        logger.info("Removed temporary registration for agent '{}'", agentId);
      }
    }
  }

  @DELETE
  @Path("capture/{agent}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "stopcapture", description = "Stops an immediate capture.", returnDescription = "OK if event were successfully stopped", pathParameters = {
          @RestParameter(name = "agent", isRequired = true, description = "The agent identifier", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Recording stopped"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_MODIFIED, description = "The recording was already stopped"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "There is no such agent"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to stop this immediate capture. Maybe you need to authenticate."),
                  @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "The agent is not ready to communicate") })
  public Response stopCapture(@PathParam("agent") String agentId) throws NotFoundException, UnauthorizedException {
    if (service == null || agentService == null || prolongingService == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Scheduler service is unavailable, please wait...").build();

    boolean isAdHoc = false;
    try {
      Agent agent = agentService.getAgent(agentId);
      String registrationType = (String) agent.getConfiguration().get(AGENT_REGISTRATION_TYPE);
      isAdHoc = AGENT_REGISTRATION_TYPE_ADHOC.equals(registrationType);
    } catch (NotFoundException e) {
      logger.debug("Temporarily registered agent '{}' for ad-hoc recording already removed", agentId);
    }

    try {
      String eventId;
      MediaPackage mp;
      DublinCoreCatalog eventCatalog;
      try {
        List<MediaPackage> search = service.search(Opt.some(agentId), Opt.<Date> none(), Opt.some(new Date()),
                Opt.some(new Date()), Opt.<Date> none());
        if (search.isEmpty()) {
          logger.info("No recording to stop found for agent '{}'!", agentId);
          return Response.notModified().build();
        } else {
          mp = search.get(0);
          eventCatalog = DublinCoreUtil.loadEpisodeDublinCore(workspace, search.get(0)).get();
          eventId = search.get(0).getIdentifier().compact();
        }
      } catch (Exception e) {
        logger.error("Unable to get the immediate recording for agent '{}': {}", agentId, e);
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
      }

      try {
        DCMIPeriod period = EncodingSchemeUtils
                .decodeMandatoryPeriod(eventCatalog.getFirst(DublinCore.PROPERTY_TEMPORAL));
        eventCatalog.set(PROPERTY_TEMPORAL,
                EncodingSchemeUtils.encodePeriod(new DCMIPeriod(period.getStart(), new Date()), Precision.Second));

        mp = addCatalog(workspace, IOUtils.toInputStream(eventCatalog.toXmlString(), "UTF-8"), "dublincore.xml",
                MediaPackageElements.EPISODE, mp);

        service.updateEvent(eventId, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
                Opt.<Set<String>> none(), Opt.some(mp), Opt.<Map<String, String>> none(),
                Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
        prolongingService.stop(agentId);
        return Response.ok().build();
      } catch (UnauthorizedException e) {
        throw e;
      } catch (Exception e) {
        logger.error("Unable to update the temporal of event '{}': {}", eventId, e);
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
      }
    } catch (Throwable t) {
      throw t;
    } finally {
      if (isAdHoc) {
        agentService.removeAgent(agentId);
        logger.info("Removed temporary agent registration '{}'", agentId);
      }
    }
  }

  @PUT
  @Path("capture/{agent}/prolong")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "prolongcapture", description = "Prolong an immediate capture.", returnDescription = "OK if event were successfully prolonged", pathParameters = {
          @RestParameter(name = "agent", isRequired = true, description = "The agent identifier", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Recording prolonged"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "No recording found for prolonging"),
                  @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to prolong this immediate capture. Maybe you need to authenticate."),
                  @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "The agent is not ready to communicate") })
  public Response prolongCapture(@PathParam("agent") String agentId) throws NotFoundException, UnauthorizedException {
    if (service == null || agentService == null || prolongingService == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Scheduler service is unavailable, please wait...").build();
    try {
      MediaPackage event = prolongingService.getCurrentRecording(agentId);
      Opt<DublinCoreCatalog> dc = DublinCoreUtil.loadEpisodeDublinCore(workspace, event);
      prolongingService.prolongEvent(event, dc.get(), agentId);
      return Response.ok().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to prolong the immediate recording for agent '{}': {}", agentId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private List<MediaPackage> getConflictingEvents(String device, String rrule,
          Long startDate, Long endDate, Long duration, String timezone)
                  throws IllegalArgumentException, UnauthorizedException, SchedulerException {

    List<MediaPackage> events = null;

    if (StringUtils.isBlank(device) || startDate == null || endDate == null) {
      logger.info("Either agent, start date or end date were not specified");
      throw new IllegalArgumentException();
    }

    RRule rule = null;
    if (StringUtils.isNotBlank(rrule)) {
      if (duration == null || StringUtils.isBlank(timezone)) {
        logger.info("Either duration or timezone were not specified");
        throw new IllegalArgumentException();
      }

      try {
        rule = new RRule(rrule);
        rule.validate();
      } catch (Exception e) {
        logger.info("Unable to parse rrule {}: {}", rrule, getMessage(e));
        throw new IllegalArgumentException();
      }

      if (!Arrays.asList(TimeZone.getAvailableIDs()).contains(timezone)) {
        logger.info("Unable to parse timezone: {}", timezone);
        throw new IllegalArgumentException();
      }
    }

    Date start = new DateTime(startDate).toDateTime(DateTimeZone.UTC).toDate();

    Date end = new DateTime(endDate).toDateTime(DateTimeZone.UTC).toDate();

    if (StringUtils.isNotBlank(rrule)) {
      events = service.findConflictingEvents(device, rule, start, end, duration, TimeZone.getTimeZone(timezone));
    } else {
      events = service.findConflictingEvents(device, start, end);
    }
    return events;
  }

  private MediaPackage addCatalog(Workspace workspace, InputStream in, String fileName,
          MediaPackageElementFlavor flavor, MediaPackage mediaPackage) throws IOException {
    Catalog[] catalogs = mediaPackage.getCatalogs(flavor);
    Catalog c = null;
    if (catalogs.length == 1)
      c = catalogs[0];

    // If catalog found, create a new one
    if (c == null) {
      c = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .newElement(MediaPackageElement.Type.Catalog, flavor);
      c.setIdentifier(UUID.randomUUID().toString());
      logger.info("Adding catalog with flavor {} to mediapackage {}", flavor, mediaPackage);
      mediaPackage.add(c);
    }

    // Update comments catalog
    try {
      URI catalogUrl = workspace.put(mediaPackage.getIdentifier().compact(), c.getIdentifier(), fileName, in);
      c.setURI(catalogUrl);
      // setting the URI to a new source so the checksum will most like be invalid
      c.setChecksum(null);
    } finally {
      IOUtils.closeQuietly(in);
    }
    return mediaPackage;
  }

  private String serializeProperties(Map<String, String> properties) {
    StringBuilder wfPropertiesString = new StringBuilder();
    for (Map.Entry<String, String> entry : properties.entrySet())
      wfPropertiesString.append(entry.getKey() + "=" + entry.getValue() + "\n");
    return wfPropertiesString.toString();
  }

  /**
   * Parses Properties represented as String.
   *
   * @param serializedProperties
   *          properties to be parsed.
   * @return parsed properties
   * @throws IOException
   *           if parsing fails
   */
  private Properties parseProperties(String serializedProperties) throws IOException {
    Properties caProperties = new Properties();
    logger.debug("properties: {}", serializedProperties);
    caProperties.load(new StringReader(serializedProperties));
    return caProperties;
  }

  /**
   * Serializes mediapackage schedule metadata into JSON array string.
   *
   * @return serialized array as json array string
   * @throws SchedulerException
   *           if parsing list into JSON format fails
   */
  public String getEventListAsJsonString(List<MediaPackage> mpList) throws SchedulerException {
    JSONParser parser = new JSONParser();
    JSONObject jsonObj = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    for (MediaPackage mp: mpList) {
      JSONObject mpJson;
      try {
        mpJson = (JSONObject) parser.parse(MediaPackageParser.getAsJSON(mp));
        mpJson = (JSONObject) mpJson.get("mediapackage");
        jsonArray.add(mpJson);
      } catch (org.json.simple.parser.ParseException e) {
        logger.warn("Unexpected JSON parse exception for getAsJSON on mp {}", mp.getIdentifier().compact(), e);
        throw new SchedulerException(e);
      }
    }
    jsonObj.put("totalCount", String.valueOf(mpList.size()));
    jsonObj.put("events", jsonArray);
    return jsonObj.toJSONString();
  }
}

