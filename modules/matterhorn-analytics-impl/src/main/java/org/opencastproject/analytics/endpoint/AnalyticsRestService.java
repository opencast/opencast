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
package org.opencastproject.analytics.endpoint;

import org.opencastproject.analytics.impl.AnalyticsServiceImpl;
import org.opencastproject.analytics.impl.ViewCollection;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The REST endpoint for the analytics service.
 */
@Path("/")
@RestService(name = "analytics", title = "Data Provider for Learning Analytics", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is not working and "
                + "is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! "
                + "You should file an error report with your server logs from the time when the error occurred: "
                + "<a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>" }, abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and "
        + "attachments.")
public class AnalyticsRestService {
  private static final Logger logger = LoggerFactory.getLogger(AnalyticsRestService.class);

  private AnalyticsServiceImpl analyticsServiceImpl;

  protected String docs = null;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
	  logger.debug("Activating " + AnalyticsRestService.class.getName());
  }

  /**
   * Set {@link org.opencastproject.analytics.impl.AnalyticsImpl} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.analytics.impl.AnalyticsImpl}
   */
  public void setService(AnalyticsServiceImpl analyticsServiceImpl) {
    this.analyticsServiceImpl = analyticsServiceImpl;
  }

  /**
   * Unset {@link org.opencastproject.analytics.impl.AnalyticsImpl} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.analytics.impl.AnalyticsImpl}
   */
  public void unsetService(AnalyticsServiceImpl analyticsServiceImpl) {
    this.analyticsServiceImpl = null;
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("series.xml")
  @RestQuery(name = "config", description = "Returns the list of series available for analysis for this user.", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "The list of series is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The list of series could'nt be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics information is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getSeriesAsXml() {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getSeriesAsXml();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("series.json")
  @RestQuery(name = "config", description = "Returns the list of series available for analysis for this user.", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "The list of series is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The list of series could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getSeriesAsJson() {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getSeriesAsJson();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("episodes.xml")
  @RestQuery(name = "episodes", description = "Returns all the episodes from a particular series if the user can analyze that series.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the series to pull the episodes from.", isRequired = false, name = "seriesID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The list of available episodes is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The list of available episodes couldn't be retrieved.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getEpisodesBySeriesAsXml(@FormParam("seriesID")String seriesID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }

    try {
      return analyticsServiceImpl.getEpisodesBySeriesAsXml(seriesID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("episodes.json")
  @RestQuery(name = "episodes", description = "Returns all the episodes from a particular series if the user can analyze that series.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the series to pull the episodes from.", isRequired = false, name = "seriesID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The list of available episodes is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The list of available episodes couldn't be retrieved.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getEpisodesBySeriesAsJson(@FormParam("seriesID")String seriesID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getEpisodesBySeriesAsJson(seriesID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("episode.xml")
  @RestQuery(name = "episodes", description = "Returns the details of a particular episode if the user has access to analyze that episode.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the media package.", isRequired = false, name = "episodeID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The episode metadata is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The episode metadata couldn't be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getEpisodeAsXml(@FormParam("episodeID") String episodeID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getEpisodeAsXml(episodeID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("episode.json")
  @RestQuery(name = "episodes", description = "Returns the details of a particular episode if the user has access to analyze that episode.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the media package.", isRequired = false, name = "episodeID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The episode metadata is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The episode metadata couldn't be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getEpisodeAsJson(@FormParam("episodeID")String episodeID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getEpisodeAsJson(episodeID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  } 
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("firstview.xml")
  @RestQuery(name = "firstview", description = "Returns the details of the first view that a user had of a particular series.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the series.", isRequired = false, name = "seriesID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The details of the first view is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The details of the first view couldn't be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getFirstViewAsXml(@FormParam("seriesID") String seriesID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getFirstUserActionsAsXml(seriesID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("firstview.json")
  @RestQuery(name = "firstview", description = "Returns the details of the first view that a user had of a particular series.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the series.", isRequired = false, name = "seriesID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The details of the first view is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The details of the first view couldn't be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getFirstViewAsJson(@FormParam("seriesID")String seriesID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getFirstUserActionsAsJson(seriesID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("lastview.xml")
  @RestQuery(name = "lastview", description = "Returns the details of the most recent view that a user had of a particular series.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the series.", isRequired = false, name = "seriesID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The details of the most recent view is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The details of the most recent view couldn't be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getLastViewAsXml(@FormParam("seriesID") String seriesID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getLastUserActionForSeriesAsXml(seriesID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("lastview.json")
  @RestQuery(name = "lastview", description = "Returns the details of the most recent view that a user had of a particular series.", pathParameters = { }, restParameters = { @RestParameter(description = "The id of the series.", isRequired = false, name = "seriesID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "The details of the most recent view is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The details of the most recent view couldn't be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics service is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getLastViewAsJson(@FormParam("seriesID")String seriesID) {
    if (analyticsServiceImpl == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Analytics service is unavailable, please wait...").build();
    }
    try {
      return analyticsServiceImpl.getLastUserActionForSeriesAsJson(seriesID);
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("views.xml")
	@RestQuery(name = "views", description = "Returns the statistics for all of the intervals between two dates for a given episode.", pathParameters = { }, restParameters = {
			@RestParameter(description = "The id of the media package.", isRequired = false, name = "episodeID", type = Type.STRING),
			@RestParameter(description = "The start time of the range that the user is interested in in format YYYYMMDDHHMM e.g. 201202250830 which would be February 25, 2012 at 8:30 am.", isRequired = false, name = "start", type = Type.STRING), 
			@RestParameter(description = "The end time of the range that the user is interested in in format YYYYMMDDHHMM e.g. 201210252330 which would be October 25, 2012 at 11:30 pm.", isRequired = false, name = "end", type = Type.STRING), 
			@RestParameter(description = "The interval time in seconds e.g. 631 is 10 minutes, 31 seconds per interval.", isRequired = false, name = "interval", type = Type.STRING) }, reponses = {
          @RestResponse(description = "the user based data is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the user based data could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics information is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public ViewCollection getViewsAsXml(@FormParam("episodeID")String episodeID, @FormParam("start")String start, @FormParam("end")String end, @FormParam("interval")String interval) {
    if (analyticsServiceImpl == null) {
      throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
    }
    try {
      return analyticsServiceImpl.getViews(episodeID, start, end, interval);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("views.json")
	@RestQuery(name = "views", description = "Returns the statistics for all of the intervals between two dates for a given episode.", pathParameters = { }, restParameters = {
			@RestParameter(description = "The id of the media package.", isRequired = false, name = "episodeID", type = Type.STRING),
			@RestParameter(description = "The start time of the range that the user is interested in in format YYYYMMDDHHMM e.g. 201202250830 which would be February 25, 2012 at 8:30 am.", isRequired = false, name = "start", type = Type.STRING), 
			@RestParameter(description = "The end time of the range that the user is interested in in format YYYYMMDDHHMM e.g. 201210252330 which would be October 25, 2012 at 11:30 pm.", isRequired = false, name = "end", type = Type.STRING), 
			@RestParameter(description = "The interval time in seconds e.g. 631 is 10 minutes, 31 seconds per interval.", isRequired = false, name = "interval", type = Type.STRING) }, reponses = {
          @RestResponse(description = "the user based data is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the user based data could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics information is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
	public ViewCollection getViewsAsJson(@FormParam("episodeID") String episodeID, @FormParam("start") String start, @FormParam("end") String end, @FormParam("interval") String interval) {
		return getViewsAsXml(episodeID, start, end, interval);
	}
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("users.xml")
  @RestQuery(name = "users", description = "Returns the statistics for all of the users for a given date.", pathParameters = { }, restParameters = {
      @RestParameter(description = "The type of the user action you are interested in e.g. HEARTBEAT, FOOTPRINT etc.", isRequired = false, name = "type", type = Type.STRING),
      @RestParameter(description = "The day to collect data from YYYYMMDD e.g. 20120225 which would be February 25, 2012.", isRequired = false, name = "day", type = Type.STRING), 
      @RestParameter(description = "The maximum number of results to return.", isRequired = false, name = "limit", type = Type.STRING), 
      @RestParameter(description = "The number of collections past the limit to return (offset of 0 is the first set of results up to the limit, 1 is the next set up to the limit etc.)", isRequired = false, name = "offset", type = Type.STRING) }, 
      reponses = {
          @RestResponse(description = "the user based data is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the user based data could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics information is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getUsersAsXML(@FormParam("type")String type, @FormParam("day")String day, @FormParam("limit")String limit, @FormParam("offset")String offset) {
    if (analyticsServiceImpl == null) {
      throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
    }
    try {
      return analyticsServiceImpl.getUserActionsAsXml(type, day, limit, offset);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("users.json")
  @RestQuery(name = "users", description = "Returns the statistics for all of the users for a given date.", pathParameters = { }, restParameters = {
          @RestParameter(description = "The type of the user action you are interested in e.g. HEARTBEAT, FOOTPRINT etc.", isRequired = false, name = "type", type = Type.STRING),
          @RestParameter(description = "The day to collect data from YYYYMMDD e.g. 20120225 which would be February 25, 2012.", isRequired = false, name = "day", type = Type.STRING), 
          @RestParameter(description = "The maximum number of results to return.", isRequired = false, name = "limit", type = Type.STRING), 
          @RestParameter(description = "The number of collections past the limit to return (offset of 0 is the first set of results up to the limit, 1 is the next set up to the limit etc.)", isRequired = false, name = "offset", type = Type.STRING) }, 
      reponses = {
          @RestResponse(description = "the user based data is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the user based data could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics information is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getUsersAsJson(@FormParam("type")String type, @FormParam("day")String day, @FormParam("limit")String limit, @FormParam("offset")String offset) {
    return analyticsServiceImpl.getUserActionsAsJson(type, day, limit, offset);
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("userSummary.xml")
  @RestQuery(name = "users", description = "Return a summary of the activity of users from a particular series in a particular type of activity.", pathParameters = { }, restParameters = {
      @RestParameter(description = "The type of the user action you are interested in e.g. HEARTBEAT, FOOTPRINT etc.", isRequired = false, name = "type", type = Type.STRING),
      @RestParameter(description = "The series id to get the summary from.", isRequired = false, name = "seriesID", type = Type.STRING) }, 
      reponses = {
          @RestResponse(description = "the user based data is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the user based data could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics information is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getUserSummaryAsXML(@FormParam("type")String type, @FormParam("seriesID")String seriesID) {
    if (analyticsServiceImpl == null) {
      throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
    }
    try {
      return analyticsServiceImpl.getUserSummaryForSeriesAsXml(type, seriesID);
    } catch (Exception e) {
      logger.error("Had a problem: ", e);
      throw new WebApplicationException(e);
    }
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("userSummary.json")
  @RestQuery(name = "users", description = "Return a summary of the activity of users from a particular series in a particular type of activity.", pathParameters = { }, restParameters = {
          @RestParameter(description = "The type of the user action you are interested in e.g. HEARTBEAT, FOOTPRINT etc.", isRequired = false, name = "type", type = Type.STRING),
          @RestParameter(description = "The series id to get the summary from.", isRequired = false, name = "seriesID", type = Type.STRING) },
      reponses = {
          @RestResponse(description = "the user based data is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the user based data could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Analytics information is not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getUserSummaryAsJson(@FormParam("type")String type, @FormParam("seriesID")String seriesID) {
    return analyticsServiceImpl.getUserSummaryForSeriesAsJson(type, seriesID);
  }
}
