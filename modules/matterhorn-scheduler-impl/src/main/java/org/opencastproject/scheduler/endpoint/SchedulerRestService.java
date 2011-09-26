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

//import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST Endpoint for Scheduler Service
 */
@Path("/")
@RestService(name = "schedulerservice", title = "Scheduler Service", abstractText = "This service creates, edits and retrieves and helps managing scheduled capture events.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>" })
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
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
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
   * @param eventID
   *          The unique ID of the event.
   * @return Dublin Core XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{id}.xml")
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
   * @param eventID
   *          The unique ID of the event.
   * @return Dublin Core JSON for the event
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}.json")
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
   * @param eventID
   *          The unique ID of the event.
   * @return Java Properties File with the metadata for the event
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{id}/agent.properties")
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
   * Creates new event(s) based on parameters. ALl times and dates are in milliseconds.
   * 
   * @param event
   *          serialized Dublin Core for event
   * @param properties
   *          Capture agent properties (optional)
   * @param recurrence
   *          recurrence pattern (optionally)
   * @param start
   *          start of event, used if recurrence is specified (if null is sent, current time will be asumed)
   * @param end
   *          end of event, required for recurrent events
   * @param duration
   *          duration of each event, required for recurrent events
   * @param agentTimeZone
   *          time zone of the agent if it's different than scheduler's
   * @return
   */
  @POST
  @Path("")
  
  @RestQuery(name = "newrecordings", description = "Creates new event or group of event with specified parameters", returnDescription = "If events were successfully generated, status CREATED is returned, otherwise BAD REQUEST",
          restParameters = {
          @RestParameter(name = "dublincore", isRequired = true, type = Type.TEXT, description = "Dublin Core describing event", defaultValue = "${this.sampleDublinCore}"),
          @RestParameter(name = "caproperties", isRequired = true, type = Type.TEXT, description = "Capture agent properties for event", defaultValue = "${this.sampleCAProperties}"),
          @RestParameter(name = "event", isRequired = false, type = Type.TEXT, description = "Catalog containing information about the event that doesn't exist in DC (IE: Recurrence rule)") }, 
          reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Event or events were successfully created"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid information for this request") })
  public Response addEvent(MultivaluedMap<String,String> catalogs) {
    DublinCoreCatalog eventCatalog;
    Properties recordingProperties;
    Properties caProperties = null;
    String start;
    String end;
    String duration;
    String recurrence;
    String timezone;
    
    if (catalogs.containsKey("dublincore")) {
      try {
        eventCatalog = parseDublinCore(catalogs.getFirst("dublincore"));
      } catch (Exception e) {
        logger.warn("Could not parse Dublin core catalog: {}",e);
        return Response.status(Status.BAD_REQUEST).build();
      }
    } else {
      logger.warn("Cannot add event without dublin core catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }
    
    if (catalogs.containsKey("event")) {
      try {
        recordingProperties = parseProperties(catalogs.getFirst("event"));
      } catch (Exception e) {
        logger.warn("Could not parse event catalog: {}", catalogs.getFirst("event"));
        return Response.status(Status.BAD_REQUEST).build();
      }
      start = recordingProperties.getProperty("start");
      end = recordingProperties.getProperty("end");
      duration = recordingProperties.getProperty("duration");
      recurrence = recordingProperties.getProperty("recurrence");
      timezone = recordingProperties.getProperty("timezone");
    } else {
      logger.warn("Cannot add event without event catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }
    
    Long startAsLong = null;
    Long endAsLong = null;
    Long durationAsLong = null;
    if (StringUtils.isNotEmpty(recurrence)) {
      if (end == null || duration == null) {
        logger.warn("For creating recurrent event end date and duration of each event must be specified.");
        return Response.status(Status.BAD_REQUEST).build();
      }
      try {
        if (start == null) {
          startAsLong = System.currentTimeMillis();
        } else {
          startAsLong = Long.parseLong(start);
        }
        endAsLong = Long.parseLong(end);
        durationAsLong = Long.parseLong(duration);
      } catch (NumberFormatException e) {
        logger.warn("Expected time in milliseconds for start, end and duration: {}", e.getMessage());
        return Response.status(Status.BAD_REQUEST).build();
      }
    }
    
    if (catalogs.containsKey("agentparameters")) {
      try {
        caProperties = parseProperties(catalogs.getFirst("agentparameters"));
      } catch (Exception e) {
        logger.warn("Could not parse capture agent properties: {}", catalogs.getFirst("agentparameters"));
        return Response.status(Status.BAD_REQUEST).build();
      }
    } else {
      logger.warn("Cannot add event without capture agent parameters catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }
    
    try {
      if (StringUtils.isNotEmpty(recurrence)) {
        // try to create event and it's recurrences
        Long[] createdIDs = service.addReccuringEvent(eventCatalog, recurrence, new Date(startAsLong), new Date(
                endAsLong), durationAsLong, timezone);
        if (caProperties != null) {
          service.updateCaptureAgentMetadata(caProperties, createdIDs);
        }
        return Response.status(Status.CREATED).build();
      } else {
        Long id = service.addEvent(eventCatalog);
        if (caProperties != null) {
          service.updateCaptureAgentMetadata(caProperties, id);
        }
        return Response
                .status(Status.CREATED)
                .header("Location",
                        PathSupport.concat(new String[] { this.serverUrl, this.serviceUrl, "recordings", id + ".xml" }))
                .build();
      }
    } catch (Exception e) {
      logger.warn("Unable to create new event: {}", e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 
   * Removes the specified event from the database. Returns true if the event was found and could be removed.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @DELETE
  @Path("{id}")
  @RestQuery(name = "deleterecordings", description = "Removes scheduled event with specified ID.", returnDescription = "OK if event were successfully removed or NOT FOUND if event with specified ID does not exist", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "Event ID", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully removed"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist") })
  public Response deleteEvent(@PathParam("id") long eventId) {
    try {
      service.removeEvent(eventId);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", eventId);
      return Response.status(Status.NOT_FOUND).build();
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
   * @param event
   *          serialized DC representing event
   * @return
   */
  @PUT
  @Path("{id}")
  @RestQuery(name = "updaterecordings", description = "Updates Dublin Core of specified event", returnDescription = "Status OK is returned if event was successfully updated, NOT FOUND if specified event does not exist or BAD REQUEST if data is missing or invalid", pathParameters = { @RestParameter(name = "id", description = "ID of event to be updated", isRequired = true, type = Type.STRING) }, restParameters = {
          @RestParameter(name = "event", isRequired = false, description = "Updated Dublin Core for event", type = Type.TEXT),
          @RestParameter(name = "agentproperties", isRequired = false, description = "Updated Capture Agent properties", type = Type.TEXT) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Event was successfully updated"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Event with specified ID does not exist"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Data is missing or invalid") })
  public Response updateEvent(@PathParam("id") String eventID, MultivaluedMap<String,String> catalogs) {

    Long id;
    try {
      id = Long.parseLong(eventID);
    } catch (Exception e) {
      logger.warn("Invalid eventID (non-numerical): {}", eventID);
      return Response.status(Status.BAD_REQUEST).build();
    }

    DublinCoreCatalog eventCatalog = null;
    if (catalogs.containsKey("dublincore")) {
      try {
        eventCatalog = parseDublinCore(catalogs.getFirst("dublincore"));
      } catch (Exception e) {
        logger.warn("Could not parse Dublin core catalog: {}",e);
        return Response.status(Status.BAD_REQUEST).build();
      }
    } else {
      logger.warn("Cannot add event without dublin core catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }

    Properties caProperties = null;
    if (catalogs.containsKey("agentparameters")) {
      try {
        caProperties = parseProperties(catalogs.getFirst("agentparameters"));
      } catch (Exception e) {
        logger.warn("Could not parse capture agent properties: {}", catalogs.getFirst("agentparameters"));
        return Response.status(Status.BAD_REQUEST).build();
      }
    } else {
      logger.warn("Cannot add event without capture agent parameters catalog.");
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      if (eventCatalog != null) {
        service.updateEvent(eventCatalog);
      }
      if (caProperties != null) {
        service.updateCaptureAgentMetadata(caProperties, id);
      }
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn("Event with id '{}' does not exist.", id);
      return Response.status(Status.NOT_FOUND).build();
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
          @RestParameter(name = "device", description = "Device identifier for which conflicts will be searched", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "start", description = "Start time of conflicting period, in milliseconds", isRequired = false, type = Type.STRING),
          @RestParameter(name = "end", description = "End time of conflicting period, in milliseconds", isRequired = false, type = Type.STRING),
          @RestParameter(name = "duration", description = "If recurrence rule is specified duration of each conflicting period, in milliseconds", isRequired = false, type = Type.STRING),
          @RestParameter(name = "rrule", description = "Rule for recurrent conflicting, specified as: \"FREQ=WEEKLY;BYDAY=day(s);BYHOUR=hour;BYMINUTE=minute\". FREQ is required. BYDAY may include one or more (separated by commas) of the following: SU,MO,TU,WE,TH,FR,SA.", isRequired = false, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Found conflicting events, returned in body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters") })
  public Response getConflictingEventsXml(@QueryParam("device") String device, @QueryParam("start") String startDate,
          @QueryParam("end") String endDate, @QueryParam("duration") String duration, @QueryParam("rrule") String rrule) {
    if (StringUtils.isEmpty(device) || startDate == null || endDate == null) {
      logger.warn("Either device, start date, end date or duration were not specified");
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (StringUtils.isNotEmpty(rrule) && duration == null) {
      logger.warn("If checking recurrence, must include duration.");
      return Response.status(Status.BAD_REQUEST).build();
    }
    Long startDateAsLong;
    Long endDateAsLong;
    Long durationAsLong;
    try {
      startDateAsLong = Long.parseLong(startDate);
      endDateAsLong = Long.parseLong(endDate);
      durationAsLong = Long.parseLong(duration);
    } catch (NumberFormatException e) {
      logger.warn("Invalid number parameter: {}", e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      DublinCoreCatalogList events = null;
      if (StringUtils.isNotEmpty(rrule)) {
        events = service.findConflictingEvents(device, rrule, new Date(startDateAsLong), new Date(endDateAsLong),
                durationAsLong);
      } else {
        events = service.findConflictingEvents(device, new Date(startDateAsLong), new Date(endDateAsLong));
      }
      if (!events.getCatalogList().isEmpty()) {
        return Response.ok(events.getResultsAsXML()).build();
      } else {
        return Response.noContent().type("").build();
      }
    } catch (Exception e) {
      logger.error(
              "Unable to find conflicting events for " + device + ", " + startDate.toString() + ", "
                      + endDate.toString() + ", " + String.valueOf(duration) + ":", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
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
  @Produces(MediaType.APPLICATION_JSON)
  @Path("conflicts.json")
  @RestQuery(name = "conflictingrecordingsasxml", description = "Searches for conflicting recordings based on parameters", returnDescription = "Returns NO CONTENT if no recordings are in conflict within specified period or list of conflicting recordings in JSON", restParameters = {
          @RestParameter(name = "device", description = "Device identifier for which conflicts will be searched", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "start", description = "Start time of conflicting period, in milliseconds", isRequired = false, type = Type.STRING),
          @RestParameter(name = "end", description = "End time of conflicting period, in milliseconds", isRequired = false, type = Type.STRING),
          @RestParameter(name = "duration", description = "If recurrence rule is specified duration of each conflicting period, in milliseconds", isRequired = false, type = Type.STRING),
          @RestParameter(name = "rrule", description = "Rule for recurrent conflicting, specified as: \"FREQ=WEEKLY;BYDAY=day(s);BYHOUR=hour;BYMINUTE=minute\". FREQ is required. BYDAY may include one or more (separated by commas) of the following: SU,MO,TU,WE,TH,FR,SA.", isRequired = false, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "No conflicting events found"),
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Found conflicting events, returned in body of response"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Missing or invalid parameters") })
  public Response getConflictingEventsJSON(@QueryParam("device") String device, @QueryParam("start") String startDate,
          @QueryParam("end") String endDate, @QueryParam("duration") String duration, @QueryParam("rrule") String rrule) {
    if (StringUtils.isEmpty(device) || startDate == null || endDate == null || duration == null) {
      logger.warn("Either device, start date, end date or duration were not specified");
      return Response.status(Status.BAD_REQUEST).build();
    }
    Long startDateAsLong;
    Long endDateAsLong;
    Long durationAsLong;
    try {
      startDateAsLong = Long.parseLong(startDate);
      endDateAsLong = Long.parseLong(endDate);
      durationAsLong = Long.parseLong(duration);
    } catch (NumberFormatException e) {
      logger.warn("Invalid number parameter: {}", e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      DublinCoreCatalogList events = null;
      if (StringUtils.isNotEmpty(rrule)) {
        events = service.findConflictingEvents(device, rrule, new Date(startDateAsLong), new Date(endDateAsLong),
                durationAsLong);
      } else {
        events = service.findConflictingEvents(device, new Date(startDateAsLong), new Date(endDateAsLong));
      }
      if (!events.getCatalogList().isEmpty()) {
        return Response.ok(events.getResultsAsJson()).build();
      } else {
        return Response.noContent().type("").build();
      }
    } catch (Exception e) {
      logger.error(
              "Unable to find conflicting events for " + device + ", " + startDate.toString() + ", "
                      + endDate.toString() + ", " + String.valueOf(duration) + ":", e.getMessage());
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
  @Produces(MediaType.TEXT_PLAIN)
  @Path("calendars")
  @RestQuery(name = "getcalendar", description = "Returns iCalendar for specified set of events", returnDescription = "ICalendar for events", restParameters = {
          @RestParameter(name = "agentid", description = "Filter events by capture agent", isRequired = false, type = Type.STRING),
          @RestParameter(name = "seriesid", description = "Filter events by series", isRequired = false, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_MODIFIED, description = "Events were not modified since last request"),
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Events were modified, new calendar is in the body") })
  public Response getCalendar(@QueryParam("agentid") String captureAgentId, @QueryParam("seriesid") String seriesId,
          @Context HttpServletRequest request) {
    SchedulerQuery filter = new SchedulerQuery().setSpatial(captureAgentId).setSeriesId(seriesId);
    try { // If the etag matches the if-not-modified header,return a 304
      Date lastModified = service.getScheduleLastModified(filter);
      if (lastModified == null) {
        lastModified = new Date();
      }
      String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
      if (StringUtils.isNotBlank(ifNoneMatch) && ifNoneMatch.equals("mod" + Long.toString(lastModified.getTime()))) {
        return Response.notModified("mod" + Long.toString(lastModified.getTime())).expires(null).build();
      }
      String result = service.getCalendar(filter);
      if (!result.isEmpty()) {
        return Response.ok(result).header(HttpHeaders.ETAG, "mod" + Long.toString(lastModified.getTime())).build();
      } else {
        throw new NotFoundException();
      }
    } catch (Exception e) {
      logger.error("Unable to get calendar for capture agent '{}': {}", captureAgentId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
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
    DublinCoreCatalog dc = dcService.load(new ByteArrayInputStream(dcXML.getBytes("UTF-8")));
    return dc;
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
