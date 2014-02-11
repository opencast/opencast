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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.util.PropertiesResponse;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

/**
 * The REST endpoint for the capture agent service on the capture device
 */
@Path("/")
@RestService(name = "captureagent", title = "Capture Agent",
  abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata "
               + "catalogs and attachments.",
  notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class CaptureRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureRestService.class);

  private CaptureAgent service;

  protected String docs = null;

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
  }

  /**
   * Set {@link org.opencastproject.capture.api.CaptureAgent} service.
   *
   * @param service
   *          Service implemented {@link org.opencastproject.capture.api.CaptureAgent}
   */
  public void setService(CaptureAgent service) {
    this.service = service;
  }

  /**
   * Unset {@link org.opencastproject.capture.api.CaptureAgent} service.
   *
   * @param service
   *          Service implemented {@link org.opencastproject.capture.api.CaptureAgent}
   */
  public void unsetService(CaptureAgent service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  @RestQuery(name = "startNP", description = "Starts a capture with the default parameters", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "valid request, results returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "couldn't start capture with default parameters", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "The recording ID for the capture started")
  public Response startCapture() {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Capture Agent is unavailable, please wait...").build();
    }

    String out;
    try {
      out = service.startCapture();
      if (out != null) {
        return Response.ok("Start Capture OK. OUT: " + out).build();
      } else {
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("There was a problem starting your capture, please check the logs.").build();
      }
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to start capture: " + e.getMessage() + ".").build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  @RestQuery(name = "startMP", description = "Starts a capture with the default properties and a provided MediaPackage", pathParameters = { }, restParameters = { @RestParameter(description = "The properties to set for this recording. "
          + "Those are specified in key-value pairs as described in "
          + "<a href=\"http://java.sun.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)\"> "
          + "this JavaDoc</a>. The current default properties can be found at "
          + "<a href=\"http://opencast.jira.com/svn/MH/trunk/docs/felix/conf/"
          + "services/org.opencastproject.capture.impl.ConfigurationManager.properties\"> " + "this location</a>", isRequired = true, name = "config", type = Type.STRING) }, reponses = {
          @RestResponse(description = "valid request, results returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "couldn't start capture with default parameters", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "The recording ID for the capture started")
  public Response startCapture(@FormParam("config") String config) {
    logger.debug("Capture configuration received:");
    logger.debug("{}.", config);
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Capture Agent is unavailable, please wait...").build();
    }

    Properties configuration = new Properties();
    try {
      configuration.load(new StringReader(config));
    } catch (IOException e1) {
      logger.warn("Unable to parse configuration string into valid capture config. Continuing with default settings.");
    }

    String out;
    try {
      out = service.startCapture(configuration);
      if (out != null)
        return Response.ok("Started capture " + out).build();
      else
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("There was a problem starting your capture, please check the logs.").build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to start capture: " + e.getMessage() + ".").build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("stopCapture")
  @RestQuery(name = "stopNP", description = "Stops the current capture", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "recording properly stopped", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "failed to stop the capture, or no current active capture", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "The recording ID for the capture started")
  public Response stopCapture() {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Capture Agent is unavailable, please wait...").build();
    }

    boolean out;
    try {
      out = service.stopCapture(true);
      if (out)
        return Response.ok("Stop Capture OK. OUT: " + out).build();
      else
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("There was a problem stopping your capture, please check the logs.").build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to stop capture: " + e.getMessage() + ".").build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("stopCapture")
  @RestQuery(name = "stopID", description = "Stops the current capture if its ID matches the argument", pathParameters = { }, restParameters = { @RestParameter(description = "The ID for the recording to stop", isRequired = true, name = "recordingID", type = Type.STRING) }, reponses = {
          @RestResponse(description = "current capture with the specified ID stopped succesfully", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "couldn't start capture with default parameters", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
  // TODO: check if this can be returned
  // stopIDEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("A workflow instance with this ID was not found"));
  }, returnDescription = "The recording ID for the capture started")
  public Response stopCapture(@FormParam("recordingID") String recordingID) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Capture Agent is unavailable, please wait...").build();
    }

    boolean out;
    try {
      out = service.stopCapture(recordingID, true);
      if (out)
        return Response.ok("Stopped Capture").build();
      else
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("There was a problem stopping your capture, please check the logs.").build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to stop capture: " + e.getMessage() + ".").build();
    }
  }

  @GET
  @Produces({ MediaType.TEXT_XML, MediaType.TEXT_PLAIN })
  @Path("configuration.{type:xml|txt}")
  @RestQuery(name = "config", description = "Returns a list with the default agent configuration properties.  This is in the same format as the startCapture endpoint.", pathParameters = {
          @RestParameter(name = "type", description = "The Document Type", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = { }, reponses = {
          @RestResponse(description = "the configuration values are returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the configuration properties could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Capture Agent is unavailable", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getConfiguration(@PathParam("type") String type) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Capture Agent is unavailable, please wait...").build();
    }

    try {
      if ("xml".equals(type)) {
        return Response.ok(new PropertiesResponse(service.getDefaultAgentProperties())).type(MediaType.TEXT_XML).build();
      } else {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        service.getDefaultAgentProperties().store(baos, null);
        String props = baos.toString();
        baos.close();
        return Response.ok(props).type(MediaType.TEXT_PLAIN).build();
      }
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("schedule")
  @RestQuery(name = "schedule", description = "Returns an XML formatted list of the capture agent's current schedule", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "the agent's schedule is returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the agent's schedule could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Capture Agent is unavailable", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getSchedule() throws JAXBException {
    if (service == null) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Capture Agent is unavailable, please wait...").build();
    }

    try {
      ScheduledEventList eventList = new ScheduledEventList(service.getAgentSchedule());
      return Response.ok(eventList).build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("capabilities")
  @RestQuery(name = "capabilities", description = "Returns the capture capabilities of the agent.", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "the agent's capabilities are returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the agent's capabilities could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Capture Agent is unavailable", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getCapabilities() throws IOException {
    if (service == null) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Capture Agent is unavailable, please wait...").build();
    }

    return Response.ok(new PropertiesResponse(service.getAgentCapabilities())).build();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("update")
  @RestQuery(name = "update", description = "Triggers a schedule data update from the agent.", pathParameters = { }, restParameters = { }, reponses = {
    @RestResponse(description = "the request was sent", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "Capture Agent is unavailable", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response updateCalendar() {
    if (service == null) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Capture Agent is unavailable, please wait...").build();
    }

    service.updateSchedule();
    return Response.ok("Calendar update commencing.").build();
  }
}
