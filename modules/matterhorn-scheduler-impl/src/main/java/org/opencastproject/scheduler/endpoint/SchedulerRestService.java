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
package org.opencastproject.scheduler.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PRECONDITION_FAILED;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

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
  private SchedulerService service;
  private DublinCoreCatalogService dcService;

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

  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * The method that will be called, if the service will be activated
   *
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty(MatterhornConstants.SERVER_URL_PROPERTY);
      logger.debug("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
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
  @Path("{id:.+}.xml")
  @RestQuery(name = "recordingsasxml", description = "Retrieves DublinCore for specified event", returnDescription = "DublinCore in XML", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of event for which DublinCore will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "DublinCore of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist") })
  public Response getDublinCoreMetadataXml(@PathParam("id") long eventId) {
    try {
      DublinCoreCatalog result = service.getEventDublinCore(eventId);
      String dcXML = serializeDublinCore(result);
      return Response.ok(dcXML).build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with id '{}': {}", eventId, e.getMessage());
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
  @Path("{id:.+}.json")
  @RestQuery(name = "recordingsasjson", description = "Retrieves DublinCore for specified event", returnDescription = "DublinCore in JSON", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of event for which DublinCore will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "DublinCore of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist") })
  public Response getDublinCoreMetadataJSON(@PathParam("id") long eventId) {
    try {
      DublinCoreCatalog result = service.getEventDublinCore(eventId);
      return Response.ok(result.toJson()).build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with id '{}': {}", eventId, e.getMessage());
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
  @RestQuery(name = "recordingsagentproperties", description = "Retrieves Capture Agent properties for specified event", returnDescription = "Capture Agent properties in the form of key, value pairs", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of event for which agent properties will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Capture Agent properties of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist") })
  public Response getCaptureAgentMetadata(@PathParam("id") long eventId) {
    try {
      Properties result = service.getEventCaptureAgentConfiguration(eventId);
      String serializedProperties = serializeProperties(result);
      return Response.ok(serializedProperties).build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with id '{}': {}", eventId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Creates new event(s) based on parameters. All times and dates are in milliseconds.
   */
  @POST
  @Path("/")
  @RestQuery(name = "newrecordings", description = "Creates new event or group of event with specified parameters", returnDescription = "If events were successfully generated, status CREATED is returned, otherwise BAD REQUEST", restParameters = {
          @RestParameter(name = "dublincore", isRequired = true, type = Type.TEXT, description = "Dublin Core describing event", defaultValue = "${this.sampleDublinCore}"),
          @RestParameter(name = "agentparameters", isRequired = true, type = Type.TEXT, description = "Capture agent properties for event", defaultValue = "${this.sampleCAProperties}"),
          @RestParameter(name = "wfproperties", isRequired = false, type = Type.TEXT, description = "Workflow configuration properties"),
          @RestParameter(name = "event", isRequired = false, type = Type.TEXT, description = "Catalog containing information about the event that doesn't exist in DC (IE: Recurrence rule)") }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Event or events were successfully created"),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to create the event. Maybe you need to authenticate."),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid information for this request") })
  public Response addEvent(@FormParam("dublincore") String dublinCoreXml,
          @FormParam("agentparameters") String agentParameters, @FormParam("wfproperties") String workflowProperties,
          @FormParam("event") String event) throws UnauthorizedException {

    if (StringUtils.isBlank(dublinCoreXml)) {
      logger.warn("Cannot add event without dublin core catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (StringUtils.isBlank(agentParameters)) {
      logger.warn("Cannot add event without capture agent parameters catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }

    DublinCoreCatalog eventCatalog;
    try {
      logger.debug("DublinCore catalog found.");
      eventCatalog = parseDublinCore(dublinCoreXml);
      logger.debug(eventCatalog.toXmlString());
    } catch (Exception e) {
      logger.warn("Could not parse Dublin core catalog: {}", e);
      return Response.status(Status.BAD_REQUEST).build();
    }

    Properties caProperties;
    try {
      caProperties = parseProperties(agentParameters);
    } catch (Exception e) {
      logger.warn("Could not parse capture agent properties: {}", agentParameters);
      return Response.status(Status.BAD_REQUEST).build();
    }

    Map<String, String> wfProperties = new HashMap<String, String>();
    if (StringUtils.isNotBlank(workflowProperties)) {
      try {
        Properties prop = parseProperties(workflowProperties);
        wfProperties.putAll((Map) prop);
      } catch (IOException e) {
        logger.warn("Could not parse workflow configuration properties: {}", workflowProperties);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    try {
      if (eventCatalog.hasValue(DublinCores.OC_PROPERTY_RECURRENCE)) {
        // try to create event and it's recurrences
        Long[] createdIDs = service.addReccuringEvent(eventCatalog, wfProperties);
        for (long id : createdIDs) {
          service.updateCaptureAgentMetadata(caProperties, tuple(id, service.getEventDublinCore(id)));
        }
        return Response.status(Status.CREATED).entity(StringUtils.join(createdIDs, ",")).build();
      } else {
        Long id = service.addEvent(eventCatalog, wfProperties);
        service.updateCaptureAgentMetadata(caProperties, tuple(id, eventCatalog));
        return Response.status(Status.CREATED)
                .header("Location", PathSupport.concat(new String[] { this.serverUrl, this.serviceUrl, id + ".xml" }))
                .build();
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Unable to create new event: {}", e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  @Path("bulkaction")
  @RestQuery(name = "bulkaction", description = "Updates the dublin core catalog of a set of recordings.", returnDescription = "No body returned.", restParameters = {
          @RestParameter(name = "idlist", description = "JSON Array of ids.", isRequired = true, type = Type.STRING),
          @RestParameter(name = "dublinecore", description = "The dublin core catalog of updated fields", isRequired = true, type = Type.STRING) }, reponses = { @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "Events were updated successfully.") })
  public Response bulkUpdate(@FormParam("idlist") String idList, @FormParam("dublincore") String dublinCore) {
    JSONParser parser = new JSONParser();
    JSONArray ids = new JSONArray();
    DublinCoreCatalog eventCatalog;
    try {
      if (idList != null && !idList.isEmpty()) {
        ids = (JSONArray) parser.parse(idList);
      }
    } catch (ParseException e) {
      logger.warn("Unable to parse json id list: {}", e);
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (StringUtils.isNotEmpty(dublinCore)) {
      try {
        eventCatalog = parseDublinCore(dublinCore);
      } catch (Exception e) {
        logger.warn("Could not parse Dublin core catalog: {}", e);
        return Response.status(Status.BAD_REQUEST).build();
      }
    } else {
      logger.warn("Cannot add event without dublin core catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (!ids.isEmpty() && eventCatalog != null) {
      try {
        service.updateEvents(mlist(ids).map(Misc.<Object, Long> cast()).value(), eventCatalog);
        return Response.noContent().type("").build(); // remove content-type, no message-body.
      } catch (Exception e) {
        logger.warn("Unable to update event with id " + ids.toString() + ": {}", e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   *
   * Removes the specified event from the database. Returns true if the event was found and could be removed.
   *
   * @param eventId
   *          The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @DELETE
  @Path("{id:.+}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "deleterecordings", description = "Removes scheduled event with specified ID.", returnDescription = "OK if event were successfully removed or NOT FOUND if event with specified ID does not exist", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "Event ID", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully removed"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "You do not have permission to remove the event. Maybe you need to authenticate.") })
  public Response deleteEvent(@PathParam("id") long eventId) throws UnauthorizedException {
    try {
      service.removeEvent(eventId);
      return Response.status(Response.Status.OK).build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Unable to delete event with id '{}': {}", eventId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Updates an existing event in the database. The event-id has to be stored in the database already. Will return OK,
   * if the event was found and could be updated.
   *
   * @param eventID
   *          id of event to be updated
   *
   * @param catalogs
   *          serialized DC representing event
   * @return
   */
  @PUT
  @Path("{id:[0-9]+}")
  @RestQuery(name = "updaterecordings", description = "Updates Dublin Core of specified event", returnDescription = "Status OK is returned if event was successfully updated, NOT FOUND if specified event does not exist or BAD REQUEST if data is missing or invalid", pathParameters = { @RestParameter(name = "id", description = "ID of event to be updated", isRequired = true, type = Type.STRING) }, restParameters = {
          @RestParameter(name = "dublincore", isRequired = false, description = "Updated Dublin Core for event", type = Type.TEXT),
          @RestParameter(name = "agentparameters", isRequired = false, description = "Updated Capture Agent properties", type = Type.TEXT),
          @RestParameter(name = "wfproperties", isRequired = false, description = "Workflow configuration properties", type = Type.TEXT) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_FORBIDDEN, description = "Event with specified ID cannot be updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Data is missing or invalid") })
  public Response updateEvent(@PathParam("id") String eventID, @FormParam("dublincore") String dublinCoreXml,
          @FormParam("agentparameters") String agentParameters, @FormParam("wfproperties") String workflowProperties)
          throws UnauthorizedException {

    // Update CA properties from dublin core (event.title, etc)
    Long id;
    try {
      id = Long.parseLong(eventID);
    } catch (Exception e) {
      logger.warn("Invalid eventID (non-numerical): {}", eventID);
      return Response.status(Status.BAD_REQUEST).build();
    }

    DublinCoreCatalog eventCatalog = null;
    if (StringUtils.isNotBlank(dublinCoreXml)) {
      try {
        logger.debug("DublinCore catalog found.");
        eventCatalog = parseDublinCore(dublinCoreXml);
        logger.debug(eventCatalog.toXmlString());
      } catch (Exception e) {
        logger.warn("Could not parse Dublin core catalog: {}", e);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    Properties caProperties = null;
    if (StringUtils.isNotBlank(agentParameters)) {
      try {
        caProperties = parseProperties(agentParameters);
        if (caProperties.size() == 0)
          logger.info("Empty form param 'agentparameters'. This resets all CA parameters. Please make sure this is intended behaviour.");

      } catch (Exception e) {
        logger.warn("Could not parse capture agent properties: {}", agentParameters);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    Map<String, String> wfProperties = new HashMap<String, String>();
    if (StringUtils.isNotBlank(workflowProperties)) {
      try {
        Properties prop = parseProperties(workflowProperties);
        wfProperties.putAll((Map) prop);
      } catch (IOException e) {
        logger.warn("Could not parse workflow configuration properties: {}", workflowProperties);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    try {
      if (eventCatalog != null)
        service.updateEvent(id, eventCatalog, wfProperties);

      if (caProperties != null)
        service.updateCaptureAgentMetadata(caProperties, tuple(id, eventCatalog));

      return Response.ok().build();
    } catch (SchedulerException e) {
      logger.warn("{}", e.getMessage());
      // TODO: send the reason message in response body
      return Response.status(Status.FORBIDDEN).build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", id);
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Unable to update event with id '{}': {}", id, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns Dublin Core list as XML based on search parameters.
   *
   * @param text
   *          full test search
   * @param eventId
   *          by event id
   * @param eventTitle
   *          by event title
   * @param seriesId
   *          by series id
   * @param creator
   *          by creator
   * @param contributor
   *          by contributor
   * @param publisher
   *          by publisher
   * @param rightsHolder
   *          by right's holder
   * @param createdFrom
   *          by created from
   * @param createdTo
   *          by created to
   * @param startsFrom
   *          by starts from
   * @param startsTo
   *          by starts to
   * @param endsFrom
   *          by ends from
   * @param endsTo
   *          by ends to
   * @param language
   *          by language
   * @param license
   *          by license
   * @param subject
   *          by subject
   * @param eventAbstract
   *          by event's abstract
   * @param description
   *          by description
   * @param device
   *          by device
   * @param sort
   *          sort parameter
   * @return
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings.xml")
  @RestQuery(name = "recordingsasxml", description = "Searches recordings and returns result as XML", returnDescription = "XML formated results", restParameters = {
          @RestParameter(name = "q", description = "Free text search", isRequired = false, type = Type.STRING),
          @RestParameter(name = "eventid", description = "Search by event ID", isRequired = false, type = Type.STRING),
          @RestParameter(name = "eventtitle", description = "Search by event title", isRequired = false, type = Type.STRING),
          @RestParameter(name = "seriesid", description = "Search by series ID", isRequired = false, type = Type.STRING),
          @RestParameter(name = "creator", description = "Search by creator", isRequired = false, type = Type.STRING),
          @RestParameter(name = "contributor", description = "Search by contributor", isRequired = false, type = Type.STRING),
          @RestParameter(name = "publisher", description = "Search by publisher", isRequired = false, type = Type.STRING),
          @RestParameter(name = "createdfrom", description = "Search by when was event created", isRequired = false, type = Type.STRING),
          @RestParameter(name = "createdto", description = "Search by when was event created", isRequired = false, type = Type.STRING),
          @RestParameter(name = "startsfrom", description = "Search by when does event start", isRequired = false, type = Type.STRING),
          @RestParameter(name = "startsto", description = "Search by when does event start", isRequired = false, type = Type.STRING),
          @RestParameter(name = "endsfrom", description = "Search by when does event finish", isRequired = false, type = Type.STRING),
          @RestParameter(name = "endsto", description = "Search by when does event finish", isRequired = false, type = Type.STRING),
          @RestParameter(name = "language", description = "Search by language", isRequired = false, type = Type.STRING),
          @RestParameter(name = "license", description = "Search by license", isRequired = false, type = Type.STRING),
          @RestParameter(name = "subject", description = "Search by subject", isRequired = false, type = Type.STRING),
          @RestParameter(name = "abstract", description = "Search by abstract", isRequired = false, type = Type.STRING),
          @RestParameter(name = "description", description = "Search by description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "spatial", description = "Search by device", isRequired = false, type = Type.STRING),
          @RestParameter(name = "sort", description = "The sort order.  May include any of the following: TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE, EVENT_START.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Search completed, results returned in body") })
  // CHECKSTYLE:OFF
  public Response getEventsAsXml(@QueryParam("q") String text, @QueryParam("eventid") String eventId,
          @QueryParam("eventtitle") String eventTitle, @QueryParam("seriesid") String seriesId,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("publisher") String publisher, @QueryParam("rightsholder") String rightsHolder,
          @QueryParam("createdfrom") String createdFrom, @QueryParam("createdto") String createdTo,
          @QueryParam("startsfrom") String startsFrom, @QueryParam("startsto") String startsTo,
          @QueryParam("endsfrom") String endsFrom, @QueryParam("endsto") String endsTo,
          @QueryParam("language") String language, @QueryParam("license") String license,
          @QueryParam("subject") String subject, @QueryParam("abstract") String eventAbstract,
          @QueryParam("description") String description, @QueryParam("spatial") String device,
          @QueryParam("sort") String sort) {
    // CHECKSTYLE:ON
    try {
      DublinCoreCatalogList result = getEvents(text, eventId, eventTitle, seriesId, creator, contributor, publisher,
              rightsHolder, createdFrom, createdTo, startsFrom, startsTo, endsFrom, endsTo, language, license, subject,
              eventAbstract, description, device, sort);
      return Response.ok(result.getResultsAsXML()).build();
    } catch (Exception e) {
      logger.error("Unable to perform search: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns Dublin Core list as JSON based on search parameters.
   *
   * @param text
   *          full test search
   * @param eventId
   *          by event id
   * @param eventTitle
   *          by event title
   * @param seriesId
   *          by series id
   * @param creator
   *          by creator
   * @param contributor
   *          by contributor
   * @param publisher
   *          by publisher
   * @param rightsHolder
   *          by right's holder
   * @param createdFrom
   *          by created from
   * @param createdTo
   *          by created to
   * @param startsFrom
   *          by starts from
   * @param startsTo
   *          by starts to
   * @param endsFrom
   *          by ends from
   * @param endsTo
   *          by ends to
   * @param language
   *          by language
   * @param license
   *          by license
   * @param subject
   *          by subject
   * @param eventAbstract
   *          by event's abstract
   * @param description
   *          by description
   * @param device
   *          by device
   * @param sort
   *          sort parameter
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("recordings.json")
  @RestQuery(name = "recordingsasxml", description = "Searches recordings and returns result as JSON", returnDescription = "JSON formated results", restParameters = {
          @RestParameter(name = "q", description = "Free text search", isRequired = false, type = Type.STRING),
          @RestParameter(name = "eventid", description = "Search by event ID", isRequired = false, type = Type.STRING),
          @RestParameter(name = "eventtitle", description = "Search by event title", isRequired = false, type = Type.STRING),
          @RestParameter(name = "seriesid", description = "Search by series ID", isRequired = false, type = Type.STRING),
          @RestParameter(name = "creator", description = "Search by creator", isRequired = false, type = Type.STRING),
          @RestParameter(name = "contributor", description = "Search by contributor", isRequired = false, type = Type.STRING),
          @RestParameter(name = "publisher", description = "Search by publisher", isRequired = false, type = Type.STRING),
          @RestParameter(name = "createdfrom", description = "Search by when was event created", isRequired = false, type = Type.STRING),
          @RestParameter(name = "createdto", description = "Search by when was event created", isRequired = false, type = Type.STRING),
          @RestParameter(name = "startsfrom", description = "Search by when does event start", isRequired = false, type = Type.STRING),
          @RestParameter(name = "startsto", description = "Search by when does event start", isRequired = false, type = Type.STRING),
          @RestParameter(name = "endsfrom", description = "Search by when does event finish", isRequired = false, type = Type.STRING),
          @RestParameter(name = "endsto", description = "Search by when does event finish", isRequired = false, type = Type.STRING),
          @RestParameter(name = "language", description = "Search by language", isRequired = false, type = Type.STRING),
          @RestParameter(name = "license", description = "Search by license", isRequired = false, type = Type.STRING),
          @RestParameter(name = "subject", description = "Search by subject", isRequired = false, type = Type.STRING),
          @RestParameter(name = "abstract", description = "Search by abstract", isRequired = false, type = Type.STRING),
          @RestParameter(name = "description", description = "Search by description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "spatial", description = "Search by device", isRequired = false, type = Type.STRING),
          @RestParameter(name = "sort", description = "The sort order.  May include any of the following: TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE, EVENT_START.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Search completed, results returned in body") })
  // CHECKSTYLE:OFF
  public Response getEventsAsJson(@QueryParam("q") String text, @QueryParam("eventid") String eventId,
          @QueryParam("eventtitle") String eventTitle, @QueryParam("seriesid") String seriesId,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("publisher") String publisher, @QueryParam("rightsholder") String rightsHolder,
          @QueryParam("createdfrom") String createdFrom, @QueryParam("createdto") String createdTo,
          @QueryParam("startsfrom") String startsFrom, @QueryParam("startsto") String startsTo,
          @QueryParam("endsfrom") String endsFrom, @QueryParam("endsto") String endsTo,
          @QueryParam("language") String language, @QueryParam("license") String license,
          @QueryParam("subject") String subject, @QueryParam("abstract") String eventAbstract,
          @QueryParam("description") String description, @QueryParam("spatial") String device,
          @QueryParam("sort") String sort) {
    // CHECKSTYLE:ON
    try {
      DublinCoreCatalogList result = getEvents(text, eventId, eventTitle, seriesId, creator, contributor, publisher,
              rightsHolder, createdFrom, createdTo, startsFrom, startsTo, endsFrom, endsTo, language, license, subject,
              eventAbstract, description, device, sort);
      return Response.ok(result.getResultsAsJson()).build();
    } catch (Exception e) {
      logger.error("Unable to perform search: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns Dublin Core list based on search parameters.
   *
   * @param text
   *          full test search
   * @param eventId
   *          by event id
   * @param eventTitle
   *          by event title
   * @param seriesId
   *          by series id
   * @param creator
   *          by creator
   * @param contributor
   *          by contributor
   * @param publisher
   *          by publisher
   * @param rightsHolder
   *          by right's holder
   * @param createdFrom
   *          by created from
   * @param createdTo
   *          by created to
   * @param startsFrom
   *          by starts from
   * @param startsTo
   *          by starts to
   * @param endsFrom
   *          by ends from
   * @param endsTo
   *          by ends to
   * @param language
   *          by language
   * @param license
   *          by license
   * @param subject
   *          by subject
   * @param eventAbstract
   *          by event's abstract
   * @param description
   *          by description
   * @param device
   *          by device
   * @param sort
   *          sort parameter
   * @return
   */
  // CHECKSTYLE:OFF
  private DublinCoreCatalogList getEvents(String text, String eventId, String eventTitle, String seriesId,
          String creator, String contributor, String publisher, String rightsHolder, String createdFrom,
          String createdTo, String startsFrom, String startsTo, String endsFrom, String endsTo, String language,
          String license, String subject, String eventAbstract, String description, String device, String sort)
          throws SchedulerException {
    // CHECKSTYLE:ON
    SchedulerQuery q = new SchedulerQuery();
    q.setText(text);
    q.setIdentifier(eventId);
    q.setIdentifier(eventTitle);
    q.setSeriesId(seriesId);
    q.setCreator(creator);
    q.setContributor(contributor);
    q.setLanguage(language);
    q.setLicense(license);
    q.setSubject(subject);
    q.setPublisher(publisher);
    q.setEventAbstract(eventAbstract);
    q.setDescription(description);
    q.setRightsHolder(rightsHolder);
    q.setSpatial(device);
    try {
      q.setCreatedFrom(SolrUtils.parseDate(createdFrom));
      q.setCreatedTo(SolrUtils.parseDate(createdTo));
      q.setStartsFrom(SolrUtils.parseDate(startsFrom));
      q.setStartsTo(SolrUtils.parseDate(startsTo));
      q.setEndsFrom(SolrUtils.parseDate(endsFrom));
      q.setEndsTo(SolrUtils.parseDate(endsTo));
    } catch (java.text.ParseException e1) {
      logger.warn("Could not parse date parameter: {}", e1);
    }

    if (StringUtils.isNotBlank(sort)) {
      SchedulerQuery.Sort sortField = null;
      if (sort.endsWith("_DESC")) {
        String enumKey = sort.substring(0, sort.length() - "_DESC".length()).toUpperCase();
        try {
          sortField = SchedulerQuery.Sort.valueOf(enumKey);
          q.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = SchedulerQuery.Sort.valueOf(sort);
          q.withSort(sortField);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }

    return service.search(q);
  }

  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   *
   * @param device
   *          device that will be checked for conflicts
   * @param startDate
   *          start date of conflict
   * @param endDate
   *          end date of conflict
   * @param duration
   *          duration of conflict (only used if recurrence rule is specified, otherwise duration is determined from
   *          start and end date)
   * @param rrule
   *          recurrence rule for conflict
   * @return An XML with the list of conflicting events
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("conflicts.xml")
  @RestQuery(name = "conflictingrecordingsasxml", description = "Searches for conflicting recordings based on parameters", returnDescription = "Returns NO CONTENT if no recordings are in conflict within specified period or list of conflicting recordings in XML", restParameters = {
          @RestParameter(name = "device", description = "Device identifier for which conflicts will be searched", isRequired = true, type = Type.TEXT),
          @RestParameter(name = "start", description = "Start time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
          @RestParameter(name = "end", description = "End time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
          @RestParameter(name = "duration", description = "If recurrence rule is specified duration of each conflicting period, in milliseconds", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "rrule", description = "Rule for recurrent conflicting, specified as: \"FREQ=WEEKLY;BYDAY=day(s);BYHOUR=hour;BYMINUTE=minute\". FREQ is required. BYDAY may include one or more (separated by commas) of the following: SU,MO,TU,WE,TH,FR,SA.", isRequired = false, type = Type.STRING),
          @RestParameter(name = "timezone", description = "The timezone of the capture device", isRequired = false, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Found conflicting events, returned in body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters") })
  public Response getConflictingEventsXml(@QueryParam("device") String device, @QueryParam("start") Long startDate,
          @QueryParam("end") Long endDate, @QueryParam("duration") Long duration, @QueryParam("rrule") String rrule,
          @QueryParam("timezone") String timezone) {
    return getConflictingEvents(device, startDate, endDate, duration, rrule, timezone, false);
  }

  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   *
   * @param device
   *          device that will be checked for conflicts
   * @param startDate
   *          start date of conflict
   * @param endDate
   *          end date of conflict
   * @param duration
   *          duration of conflict (only used if recurrence rule is specified, otherwise duration is determined from
   *          start and end date)
   * @param rrule
   *          recurrence rule for conflict
   * @param timezone
   *          The timezone of the capture device
   * @return A JSON object with the list of conflicting events
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("conflicts.json")
  @RestQuery(name = "conflictingrecordingsasxml", description = "Searches for conflicting recordings based on parameters", returnDescription = "Returns NO CONTENT if no recordings are in conflict within specified period or list of conflicting recordings in JSON", restParameters = {
          @RestParameter(name = "device", description = "Device identifier for which conflicts will be searched", isRequired = true, type = Type.TEXT),
          @RestParameter(name = "start", description = "Start time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
          @RestParameter(name = "end", description = "End time of conflicting period, in milliseconds", isRequired = true, type = Type.INTEGER),
          @RestParameter(name = "duration", description = "If recurrence rule is specified duration of each conflicting period, in milliseconds", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "rrule", description = "Rule for recurrent conflicting, specified as: \"FREQ=WEEKLY;BYDAY=day(s);BYHOUR=hour;BYMINUTE=minute\". FREQ is required. BYDAY may include one or more (separated by commas) of the following: SU,MO,TU,WE,TH,FR,SA.", isRequired = false, type = Type.STRING),
          @RestParameter(name = "timezone", description = "The timezone of the capture device", isRequired = false, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Found conflicting events, returned in body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters") })
  public Response getConflictingEventsJSON(@QueryParam("device") String device, @QueryParam("start") Long startDate,
          @QueryParam("end") Long endDate, @QueryParam("duration") Long duration, @QueryParam("rrule") String rrule,
          @QueryParam("timezone") String timezone) {
    return getConflictingEvents(device, startDate, endDate, duration, rrule, timezone, true);
  }

  private Response getConflictingEvents(String device, Long startDate, Long endDate, Long duration, String rrule,
          String timezone, boolean asJson) {
    if (StringUtils.isEmpty(device) || startDate == null || endDate == null) {
      logger.warn("Either device, start date or end date were not specified");
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (StringUtils.isNotEmpty(rrule)) {
      try {
        RRule rule = new RRule(rrule);
        rule.validate();
      } catch (Exception e) {
        logger.warn("Unable to parse rrule {}: {}", rrule, e.getMessage());
        return Response.status(Status.BAD_REQUEST).build();
      }

      if (duration == null) {
        logger.warn("If checking recurrence, must include duration.");
        return Response.status(Status.BAD_REQUEST).build();
      }

      if (StringUtils.isNotEmpty(timezone) && !Arrays.asList(TimeZone.getAvailableIDs()).contains(timezone)) {
        logger.warn("Unable to parse timezone: {}", timezone);
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    try {
      DublinCoreCatalogList events = null;
      if (StringUtils.isNotEmpty(rrule)) {
        events = service.findConflictingEvents(device, rrule, new Date(startDate), new Date(endDate), duration,
                timezone);
      } else {
        events = service.findConflictingEvents(device, new Date(startDate), new Date(endDate));
      }
      if (!events.getCatalogList().isEmpty()) {
        if (asJson) {
          return Response.ok(events.getResultsAsJson()).build();
        } else {
          return Response.ok(events.getResultsAsXML()).build();
        }
      } else {
        return Response.noContent().build();
      }
    } catch (Exception e) {
      logger.error(
              "Unable to find conflicting events for " + device + ", " + startDate.toString() + ", "
                      + endDate.toString() + ", " + duration.toString() + ":", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets the iCalendar with all (even old) events for the specified filter.
   *
   * @param captureAgentID
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
          @RestParameter(name = "cutoff", description = "A cutoff date at which the number of events returned in the calendar are limited.", isRequired = false, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "No calendar for agent found"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_MODIFIED, description = "Events were not modified since last request"),
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Events were modified, new calendar is in the body") })
  public Response getCalendar(@QueryParam("agentid") String captureAgentId, @QueryParam("seriesid") String seriesId,
          @QueryParam("cutoff") String cutoff, @Context HttpServletRequest request) throws NotFoundException {

    Date endDate = null;
    if (StringUtils.isNotEmpty(cutoff)) {
      try {
        endDate = new Date(Long.valueOf(cutoff));
      } catch (NumberFormatException e) {
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    try { // If the etag matches the if-not-modified header,return a 304
      String lastModified = service.getScheduleLastModified(captureAgentId);
      String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
      if (StringUtils.isNotBlank(ifNoneMatch) && ifNoneMatch.equals(lastModified)) {
        return Response.notModified(lastModified).expires(null).build();
      }
      SchedulerQuery filter = new SchedulerQuery().setSpatial(captureAgentId).setSeriesId(seriesId);
      if (endDate != null)
        filter.setEndsFrom(DateTime.now().minusHours(1).toDate()).setStartsTo(endDate);

      String result = service.getCalendar(filter);
      if (!result.isEmpty()) {
        return Response.ok(result).header(HttpHeaders.ETAG, lastModified)
                .header(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=UTF-8").build();
      } else {
        throw new NotFoundException("No calendar for agent " + captureAgentId + " found!");
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to get calendar for capture agent '{}': {}", captureAgentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{id}/acl")
  @RestQuery(name = "updateaccesscontrollist", description = "Updates the access control list of the event with the given id", returnDescription = "Status OK is returned if event was successfully updated", pathParameters = { @RestParameter(name = "id", description = "ID of event", isRequired = true, type = Type.STRING) }, restParameters = { @RestParameter(name = "acl", isRequired = false, description = "The access control list", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "access control list could not be parsed") })
  public Response updateAccessControlList(@PathParam("id") long eventId, @FormParam("acl") String aclString) {
    AccessControlList acl;
    try {
      acl = AccessControlParser.parseAcl(aclString);
    } catch (Exception e) {
      logger.debug("Unable to parse acl '{}': {}", aclString, ExceptionUtils.getStackTrace(e));
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      service.updateAccessControlList(eventId, acl);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update access control list of event with id '{}': {}", eventId,
              ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}/acl")
  @RestQuery(name = "getaccesscontrollist", description = "Retrieves the access control list for specified event", returnDescription = "The access control list", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of event for which the access control list will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The access control list as JSON "),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist") })
  public Response getAccessControlList(@PathParam("id") long eventId) {
    try {
      AccessControlList accessControlList = service.getAccessControlList(eventId);
      return Response.ok(AccessControlParser.toJson(accessControlList)).type(MediaType.APPLICATION_JSON_TYPE).build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve access control list of event with id '{}': {}", eventId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/mediapackageId")
  @RestQuery(name = "recordingmediapackageid", description = "Retrieves the mediapackage identifier for specified event", returnDescription = "The mediapackage identifier", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of event for which the mediapackage identifier will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The mediapackage identifier of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist") })
  public Response getMediaPackageId(@PathParam("id") long eventId) {
    try {
      String mediaPackageId = service.getMediaPackageId(eventId);
      return Response.ok(mediaPackageId).build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with id '{}': {}", eventId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/eventId")
  @RestQuery(name = "recordingeventid", description = "Retrieves the event identifier of the event with the given mediapackage", returnDescription = "The event identifier", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage identifier", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The mediapackage identifier of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist") })
  public Response getEventId(@PathParam("id") String mediaPackageId) {
    try {
      long eventId = service.getEventId(mediaPackageId);
      return Response.ok(Long.toString(eventId)).build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/optOut")
  @RestQuery(name = "recordingoptoutstatus", description = "Retrieves the opt out status for specified event", returnDescription = "The opt out status", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage id for which the opt out status will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The opt out status of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist") })
  public Response getOptOut(@PathParam("id") String mediaPackageId) {
    try {
      boolean optOut = service.isOptOut(mediaPackageId);
      return Response.ok(Boolean.toString(optOut)).build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{id}/optOut")
  @RestQuery(name = "updateoptoutstatus", description = "Updates the opt out status of the event with the given mediapackage id", returnDescription = "Status OK is returned if event was successfully updated", pathParameters = { @RestParameter(name = "id", description = "ID of events mediapackage", isRequired = true, type = Type.STRING) }, restParameters = { @RestParameter(name = "optOut", isRequired = false, description = "The opt out status to set", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "opt out value could not be parsed") })
  public Response updateOptOut(@PathParam("id") String mpId, @FormParam("optOut") String optOutString) {
    Boolean optedOut = BooleanUtils.toBooleanObject(optOutString);
    if (optedOut == null)
      return Response.status(Status.BAD_REQUEST).build();

    try {
      service.updateOptOutStatus(mpId, optedOut);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mpId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update event with mediapackage id '{}': {}", mpId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/reviewStatus")
  @RestQuery(name = "recordingreviewstatus", description = "Retrieves the review status for specified event", returnDescription = "The review status", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage id for which the review status will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The review status of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist") })
  public Response getReviewStatus(@PathParam("id") String mediaPackageId) {
    try {
      ReviewStatus reviewStatus = service.getReviewStatus(mediaPackageId);
      return Response.ok(reviewStatus.toString()).build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{id}/reviewStatus")
  @RestQuery(name = "updatereviewstatus", description = "Updates the review status of the event with the given mediapackage id", returnDescription = "Status OK is returned if event was successfully updated", pathParameters = { @RestParameter(name = "id", description = "ID of events mediapackage", isRequired = true, type = Type.STRING) }, restParameters = { @RestParameter(name = "reviewStatus", isRequired = false, description = "The review status to set: [UNSENT, UNCONFIRMED, CONFIRMED]", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "review status could not be parsed") })
  public Response updateReviewStatus(@PathParam("id") String mpId, @FormParam("reviewStatus") String reviewStatusString) {

    ReviewStatus reviewStatus;
    try {
      reviewStatus = ReviewStatus.valueOf(reviewStatusString);
    } catch (Exception e) {
      logger.warn("Unable to parse review status {}", reviewStatusString);
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      service.updateReviewStatus(mpId, reviewStatus);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mpId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update event with mediapackage id '{}': {}", mpId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/blacklisted")
  @RestQuery(name = "recordingblackliststatus", description = "Retrieves the blacklist status for specified event", returnDescription = "The blacklist status", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "ID of events mediapackage id for which the blacklist status will be retrieved", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The blacklist status of event is in the body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist") })
  public Response getBlacklistStatus(@PathParam("id") String mediaPackageId) {
    try {
      boolean blacklisted = service.isBlacklisted(mediaPackageId);
      return Response.ok(Boolean.toString(blacklisted)).build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mediaPackageId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to retrieve event with mediapackage id '{}': {}", mediaPackageId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{id}/blacklisted")
  @RestQuery(name = "updateblackliststatus", description = "Updates the blacklist status of the event with the given mediapackage id", returnDescription = "Status OK is returned if event was successfully updated", pathParameters = { @RestParameter(name = "id", description = "ID of events mediapackage", isRequired = true, type = Type.STRING) }, restParameters = { @RestParameter(name = "blacklisted", isRequired = false, description = "The blacklist status to set", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "blacklist status could not be parsed") })
  public Response updateBlacklistStatus(@PathParam("id") String mpId, @FormParam("blacklisted") String blacklistString) {
    Boolean blacklisted = BooleanUtils.toBooleanObject(blacklistString);
    if (blacklisted == null)
      return Response.status(Status.BAD_REQUEST).build();

    try {
      service.updateBlacklistStatus(mpId, blacklisted);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mpId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update event with mediapackage id '{}': {}", mpId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{id}/workflowConfig")
  @RestQuery(name = "updateworkflowconfig", description = "Updates the worklfow config of the event with the given mediapackage id", returnDescription = "Status OK is returned if event was successfully updated", pathParameters = { @RestParameter(name = "id", description = "ID of events mediapackage", isRequired = true, type = Type.STRING) }, restParameters = { @RestParameter(name = "workflowConfig", isRequired = false, description = "The workflow config to add", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified mediapackage ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "workflow config could not be parsed") })
  public Response updateWorkflowConfig(@PathParam("id") String mpId,
          @FormParam("workflowConfig") String workflowConfigString) {
    Map<String, String> wfProperties = new HashMap<String, String>();
    try {
      Properties prop = parseProperties(workflowConfigString);
      wfProperties.putAll((Map) prop);
    } catch (IOException e) {
      logger.warn("Could not parse workflow configuration properties: {}", workflowConfigString);
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      service.updateWorkflowConfig(mpId, wfProperties);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn("Event with mediapackage id '{}' does not exist.", mpId);
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update event with mediapackage id '{}': {}", mpId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/removeOldScheduledRecordings")
  @RestQuery(name = "removeOldScheduledRecordings", description = "This will find and remove any scheduled events before the buffer time to keep performance in the scheduler optimum.", returnDescription = "No return value", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Removed old scheduled recordings."),
          @RestResponse(responseCode = SC_PRECONDITION_FAILED, description = "Unable to parse buffer.") }, restParameters = { @RestParameter(name = "buffer", type = RestParameter.Type.INTEGER, defaultValue = "604800", isRequired = true, description = "The amount of seconds before now that a capture has to have stopped capturing. It must be 0 or greater.") })
  public Response removeOldScheduledRecordings(@FormParam("buffer") long buffer) {
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
   * Generates event Dublin Core without identifier set.
   *
   * @return
   */
  public String getSampleDublinCore() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><dcterms:creator>demo</dcterms:creator><dcterms:contributor>demo</dcterms:contributor><dcterms:created xsi:type=\"dcterms:W3CDTF\">2011-04-06T08:03Z</dcterms:created><dcterms:temporal xsi:type=\"dcterms:Period\">start=2011-04-06T08:03:45Z; end=2011-04-06T08:13:45Z; scheme=W3C-DTF;</dcterms:temporal><dcterms:description>demo</dcterms:description><dcterms:subject>demo</dcterms:subject><dcterms:language>demo</dcterms:language><dcterms:spatial>recorder</dcterms:spatial><dcterms:title>Demo event</dcterms:title></dublincore>";
  }

  /**
   * Generates event capture agent properties.
   *
   * @return
   */
  public String getSampleCAProperties() {
    return "#Capture Agent specific data\n" + "#Wed Apr 06 10:16:19 CEST 2011\n" + "event.title=Demotitle\n"
            + "event.location=testdevice\n" + "capture.device.id=testdevice\n" + "";
  }

  /**
   * Serializes Dublin core and returns serialized string.
   *
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized
   *
   * @return String representation of serialized Dublin core
   *
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  /**
   * Parses Dublin core stored as string.
   *
   * @param dcXML
   *          string representation of Dublin core
   * @return parsed {@link DublinCoreCatalog}
   * @throws IOException
   *           if parsing fails
   */
  private DublinCoreCatalog parseDublinCore(String dcXML) throws IOException {
    // Trim XML string because parsing will fail if there are any chars before XML processing instruction
    String trimmedDcXml = StringUtils.trim(dcXML);
    /*
     * Warn the user if trimming was necessary as this meant that the XML string was technically invalid.
     */
    if (!trimmedDcXml.equals(dcXML)) {
      logger.warn("Detected invalid XML data. Trying to fix this by removing spaces from beginning/end.");
    }
    return dcService.load(new ByteArrayInputStream(trimmedDcXml.getBytes("UTF-8")));
  }

  /**
   * Serializes Properties to String.
   *
   * @param caProperties
   *          Properties to be serialized
   * @return serialized properties
   * @throws IOException
   *           if serialization fails
   */
  private String serializeProperties(Properties caProperties) throws IOException {
    StringWriter writer = new StringWriter();
    caProperties.store(writer, "Capture Agent specific data");
    return writer.toString();
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
}
