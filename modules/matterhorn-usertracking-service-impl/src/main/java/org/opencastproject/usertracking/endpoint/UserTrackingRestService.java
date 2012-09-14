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
package org.opencastproject.usertracking.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.usertracking.api.UserTrackingException;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.usertracking.impl.UserActionImpl;
import org.opencastproject.usertracking.impl.UserActionListImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
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
 * REST Endpoint for User Tracking Service
 */
@Path("")
@RestService(name = "usertracking", title = "User Tracking Service", notes = { UserTrackingRestService.NOTES }, abstractText = "This service is used for tracking user interaction creates, edits and retrieves user actions and viewing statistics.")
public class UserTrackingRestService {

  public static final String NOTES = "All paths above are relative to the REST endpoint base (something like "
          + "http://your.server/files). If the service is down or not working it will return a status 503, this means the "
          + "underlying service is not working and is either restarting or has failed. A status code 500 means a general "
          + "failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You "
          + "should file an error report with your server logs from the time when the error occurred: "
          + "<a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>";

  private static final Logger logger = LoggerFactory.getLogger(UserTrackingRestService.class);

  private UserTrackingService usertrackingService;

  protected SecurityService securityService;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  protected String serviceUrl = "/usertracking"; // set this to the default value initially

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   */
  public void setService(UserTrackingService service) {
    this.usertrackingService = service;
  }

  /**
   * Sets the security service
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * The method that is called, when the service is activated
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
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    }
  }

  /**
   * @return XML with all footprints
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/actions.xml")
  @RestQuery(name = "actionsasxml", description = "Get user actions by type and day", returnDescription = "The user actions.", restParameters = {
          @RestParameter(name = "type", description = "The type of the user action", isRequired = false, type = Type.STRING),
          @RestParameter(name = "day", description = "The day of creation (format: YYYYMMDD)", isRequired = false, type = Type.STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = Type.STRING),
          @RestParameter(name = "offset", description = "The page number", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the user actions") })
  public UserActionListImpl getUserActionsAsXml(@QueryParam("id") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      throw new WebApplicationException(Status.BAD_REQUEST);

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;
    try {
      if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(type))
        return (UserActionListImpl) usertrackingService.getUserActionsByTypeAndMediapackageId(type, id, offset, limit);
      else if (!StringUtils.isEmpty(type) && !StringUtils.isEmpty(day))
        return (UserActionListImpl) usertrackingService.getUserActionsByTypeAndDay(type, day, offset, limit);
      else if (!StringUtils.isEmpty(type))
        return (UserActionListImpl) usertrackingService.getUserActionsByType(type, offset, limit);
      else if (!StringUtils.isEmpty(day))
        return (UserActionListImpl) usertrackingService.getUserActionsByDay(day, offset, limit);
      else
        return (UserActionListImpl) usertrackingService.getUserActions(offset, limit);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * @return JSON with all footprints
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/actions.json")
  @RestQuery(name = "actionsasjson", description = "Get user actions by type and day", returnDescription = "The user actions.", restParameters = {
          @RestParameter(name = "type", description = "The type of the user action", isRequired = false, type = Type.STRING),
          @RestParameter(name = "day", description = "The day of creation (format: YYYYMMDD)", isRequired = false, type = Type.STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = Type.STRING),
          @RestParameter(name = "offset", description = "The page number", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the user actions") })
  public UserActionListImpl getUserActionsAsJson(@QueryParam("id") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {
    return getUserActionsAsXml(id, type, day, limit, offset); // same logic, different @Produces annotation
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/stats.xml")
  @RestQuery(name = "statsasxml", description = "Get the statistics for an episode", returnDescription = "The statistics.", restParameters = { @RestParameter(name = "id", description = "The ID of the single episode to return the statistics for, if it exists", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the episode's statistics") })
  public StatsImpl statsAsXml(@QueryParam("id") String mediapackageId) {
    StatsImpl s = new StatsImpl();
    s.setMediapackageId(mediapackageId);
    try {
      s.setViews(usertrackingService.getViews(mediapackageId));
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
    return s;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/stats.json")
  @RestQuery(name = "statsasjson", description = "Get the statistics for an episode", returnDescription = "The statistics.", restParameters = { @RestParameter(name = "id", description = "The ID of the single episode to return the statistics for, if it exists", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the episode's statistics") })
  public StatsImpl statsAsJson(@QueryParam("id") String mediapackageId) {
    return statsAsXml(mediapackageId); // same logic, different @Produces annotation
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/report.xml")
  @RestQuery(name = "reportasxml", description = "Get a report for a time range", returnDescription = "The report.", restParameters = {
          @RestParameter(name = "from", description = "The beginning of the time range", isRequired = false, type = Type.STRING),
          @RestParameter(name = "to", description = "The end of the time range", isRequired = false, type = Type.STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = Type.STRING),
          @RestParameter(name = "offset", description = "The page number", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the report") })
  public ReportImpl reportAsXml(@QueryParam("from") String from, @QueryParam("to") String to,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      throw new WebApplicationException(Status.BAD_REQUEST);

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;

    try {
      if (from == null && to == null)
        return (ReportImpl) usertrackingService.getReport(offset, limit);
      else
        return (ReportImpl) usertrackingService.getReport(from, to, offset, limit);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/report.json")
  @RestQuery(name = "reportasjson", description = "Get a report for a time range", returnDescription = "The report.", restParameters = {
          @RestParameter(name = "from", description = "The beginning of the time range", isRequired = false, type = Type.STRING),
          @RestParameter(name = "to", description = "The end of the time range", isRequired = false, type = Type.STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = Type.STRING),
          @RestParameter(name = "offset", description = "The page number", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the report") })
  public ReportImpl reportAsJson(@QueryParam("from") String from, @QueryParam("to") String to,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) {
    return reportAsXml(from, to, offset, limit); // same logic, different @Produces annotation
  }

  @PUT
  @Path("")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "add", description = "Record a user action", returnDescription = "An XML representation of the user action", restParameters = {
          @RestParameter(name = "id", description = "The episode identifier", isRequired = true, type = Type.STRING),
          @RestParameter(name = "type", description = "The episode identifier", isRequired = true, type = Type.STRING),
          @RestParameter(name = "in", description = "The beginning of the time range", isRequired = false, type = Type.STRING),
          @RestParameter(name = "out", description = "The end of the time range", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_CREATED, description = "An XML representation of the user action") })
  public Response addFootprint(@FormParam("id") String mediapackageId, @FormParam("in") String inString,
          @FormParam("out") String outString, @FormParam("type") String type, @FormParam("playing") String isPlaying,
          @Context HttpServletRequest request) {

    String sessionId = request.getSession().getId();
    String userId = securityService.getUser().getUserName();

    // Parse the in and out strings, which might be empty (hence, we can't let jax-rs handle them properly)
    if (StringUtils.isEmpty(inString)) {
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("in must be a non null integer")
              .build());
    }
    Integer in = null;
    try {
      in = Integer.parseInt(StringUtils.trim(inString));
    } catch (NumberFormatException e) {
      throw new WebApplicationException(e, Response.status(Status.BAD_REQUEST).entity("in must be a non null integer").build());
    }

    Integer out = null;
    if (StringUtils.isEmpty(outString)) {
      out = in;
    } else {
      try {
        out = Integer.parseInt(StringUtils.trim(outString));
      } catch (NumberFormatException e) {
        throw new WebApplicationException(e);
      }
    }

    UserActionImpl a = new UserActionImpl();
    a.setMediapackageId(mediapackageId);
    a.setUserId(userId);
    a.setSessionId(sessionId);
    a.setInpoint(in);
    a.setOutpoint(out);
    a.setType(type);
    a.setIsPlaying(Boolean.valueOf(isPlaying));
    
    //MH-8616 the connection might be via a proxy
    String clientIP = request.getHeader("X-FORWARDED-FOR");
    
    if (clientIP == null) {
      clientIP = request.getRemoteAddr();
    }
    logger.debug("Got client ip: {}", clientIP);
    a.setUserIp(clientIP);
    
    try {
      if ("FOOTPRINT".equals(type)) {
        a = (UserActionImpl) usertrackingService.addUserFootprint(a);
      } else {
        a = (UserActionImpl) usertrackingService.addUserTrackingEvent(a);
      }
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }

    URI uri;
    try {
      uri = new URI(UrlSupport.concat(new String[] { serverUrl, serviceUrl, "action", a.getId().toString(), ".xml" }));
    } catch (URISyntaxException e) {
      throw new WebApplicationException(e);
    }
    return Response.created(uri).entity(a).build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/action/{id}.xml")
  @RestQuery(name = "add", description = "Record a user action", returnDescription = "An XML representation of the user action", pathParameters = { @RestParameter(name = "id", description = "The episode identifier", isRequired = true, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the user action") })
  public UserActionImpl getActionAsXml(@PathParam("id") String actionId) {
    Long id = null;
    try {
      id = Long.parseLong(actionId);
    } catch (NumberFormatException e) {
      throw new WebApplicationException(e);
    }
    try {
      return (UserActionImpl) usertrackingService.getUserAction(id);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      return null;
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/action/{id}.json")
  @RestQuery(name = "add", description = "Record a user action", returnDescription = "A JSON representation of the user action", pathParameters = { @RestParameter(name = "id", description = "The episode identifier", isRequired = true, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the user action") })
  public UserActionImpl getActionAsJson(@PathParam("id") String actionId) {
    return getActionAsXml(actionId);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/footprint.xml")
  @RestQuery(name = "footprintasxml", description = "Gets the 'footprint' action for an episode", returnDescription = "An XML representation of the footprints", restParameters = { @RestParameter(name = "id", description = "The episode identifier", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the footprints") })
  public FootprintsListImpl getFootprintAsXml(@QueryParam("id") String mediapackageId) {
    String userId = securityService.getUser().getUserName();

    // Is the mediapackageId passed
    if (mediapackageId == null)
      throw new WebApplicationException(Status.BAD_REQUEST);

    try {
      return (FootprintsListImpl) usertrackingService.getFootprints(mediapackageId, userId);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/footprint.json")
  @RestQuery(name = "footprintasxml", description = "Gets the 'footprint' action for an episode", returnDescription = "A JSON representation of the footprints", restParameters = { @RestParameter(name = "id", description = "The episode identifier", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the footprints") })
  public FootprintsListImpl getFootprintAsJson(@QueryParam("id") String mediapackageId) {
    return getFootprintAsXml(mediapackageId); // this is the same logic... it's just annotated differently
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/detailenabled")
  public Response getUserTrackingEnabled() {
    return Response.ok(usertrackingService.getUserTrackingEnabled()).build();
  }
}
