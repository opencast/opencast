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

package org.opencastproject.capture.admin.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.opencastproject.capture.admin.api.AgentState.KNOWN_STATES;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentStateUpdate;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.impl.RecordingStateUpdate;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesResponse;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the capture agent service on the capture device
 */
@Path("/")
@RestService(name = "captureadminservice",
  title = "Capture Admin Service",
  abstractText = "This service is a registry of capture agents and their recordings.",
  notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is "
      + "not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
      + "other words, there is a bug! You should file an error report with your server logs from the time when the "
      + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class CaptureAgentStateRestService {

  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStateRestService.class);
  private CaptureAgentStateService service;
  private SchedulerService schedulerService;

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
  }

  public void setService(CaptureAgentStateService service) {
    this.service = service;
  }

  public void unsetService(CaptureAgentStateService service) {
    this.service = null;
  }

  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  public CaptureAgentStateRestService() {
  }

  @GET
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @Path("agents/{name}.{format:xml|json}")
  @RestQuery(
    name = "getAgent",
    description = "Return the state of a given capture agent",
    pathParameters = {
      @RestParameter(name = "name", description = "Name of the capture agent", isRequired = true, type = Type.STRING),
      @RestParameter(name = "format", description = "The output format (json or xml) of the response body.",
        isRequired = true, type = RestParameter.Type.STRING)
    }, restParameters = {}, reponses = {
      @RestResponse(description = "{agentState}", responseCode = SC_OK),
      @RestResponse(description = "The agent {agentName} does not exist", responseCode = SC_NOT_FOUND),
      @RestResponse(description = "If the {format} is not xml or json", responseCode = SC_METHOD_NOT_ALLOWED),
      @RestResponse(description = "iCapture agent state service unavailable", responseCode = SC_SERVICE_UNAVAILABLE)
    }, returnDescription = "")
  public Response getAgentState(@PathParam("name") String agentName, @PathParam("format") String format)
          throws NotFoundException {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    Agent ret = service.getAgent(agentName);
    logger.debug("Returning agent state for {}", agentName);
    if ("json".equals(format)) {
      return Response.ok(new AgentStateUpdate(ret)).type(MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(new AgentStateUpdate(ret)).type(MediaType.APPLICATION_XML).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("agents/{name}")
  // Todo: Capture agent may send an optional FormParam containing it's configured address.
  // If this exists don't use request.getRemoteHost() for the URL
  @RestQuery(
    name = "setAgentState",
    description = "Set the status of a given capture agent",
    pathParameters = {
      @RestParameter(name = "name", isRequired = true, type = Type.STRING, description = "Name of the capture agent")
    }, restParameters = {
      @RestParameter(name = "address", isRequired = false, type = Type.STRING, description = "Address of the agent"),
      @RestParameter(name = "state", isRequired = true, type = Type.STRING, description = "The state of the capture "
        + "agent. Known states are: idle, shutting_down, capturing, uploading, unknown, offline, error")
    }, reponses = {
      @RestResponse(description = "{agentName} set to {state}", responseCode = SC_OK),
      @RestResponse(description = "{state} is empty or not known", responseCode = SC_BAD_REQUEST),
      @RestResponse(description = "Capture agent state service not available", responseCode = SC_SERVICE_UNAVAILABLE)
    }, returnDescription = "")
  public Response setAgentState(@Context HttpServletRequest request, @FormParam("address") String address,
          @PathParam("name") String agentName, @FormParam("state") String state) throws NotFoundException {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    if (!KNOWN_STATES.contains(state)) {
      logger.debug("'{}' is not a valid state", state);
      return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
    }

    if (StringUtils.isEmpty(address)) {
      address = request.getRemoteHost();
    }

    logger.debug("Agents URL: {}", address);

    boolean agentStateUpdated = service.setAgentState(agentName, state);
    boolean agentUrlUpdated = service.setAgentUrl(agentName, address);

    if (!agentStateUpdated && !agentUrlUpdated) {
      logger.debug("{}'s state '{}' and url '{}' has not changed, nothing has been updated", agentName, state, address);
      return Response.ok().build();
    }
    logger.debug("{}'s state successfully set to {}", agentName, state);
    return Response.ok(agentName + " set to " + state).build();
  }

  @DELETE
  @Path("agents/{name}")
  @Produces(MediaType.TEXT_HTML)
  @RestQuery(
    name = "removeAgent",
    description = "Remove record of a given capture agent",
    pathParameters = {
      @RestParameter(name = "name", description = "Name of the capture agent", isRequired = true, type = Type.STRING)
    }, restParameters = {}, reponses = {
      @RestResponse(description = "{agentName} removed", responseCode = SC_OK),
      @RestResponse(description = "The agent {agentname} does not exist", responseCode = SC_NOT_FOUND)
    }, returnDescription = "")
  public Response removeAgent(@PathParam("name") String agentName) throws NotFoundException {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    service.removeAgent(agentName);

    logger.debug("The agent {} was successfully removed", agentName);
    return Response.ok(agentName + " removed").build();
  }

  @GET
  @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
  @Path("agents.{type:xml|json}")
  @RestQuery(
    name = "getKnownAgents",
    description = "Return all of the known capture agents on the system",
    pathParameters = {
      @RestParameter(description = "The Document type", isRequired = true, name = "type", type = Type.STRING)
    }, restParameters = {}, reponses = {
      @RestResponse(description = "An XML representation of the agent capabilities", responseCode = SC_OK)
    }, returnDescription = "")
  public Response getKnownAgents(@PathParam("type") String type) {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    logger.debug("Returning list of known agents...");
    LinkedList<AgentStateUpdate> update = new LinkedList<AgentStateUpdate>();
    Map<String, Agent> data = service.getKnownAgents();
    logger.debug("Agents: {}", data);
    // Run through and build a map of updates (rather than states)
    for (Entry<String, Agent> e : data.entrySet()) {
      update.add(new AgentStateUpdate(e.getValue()));
    }

    if ("json".equals(type)) {
      return Response.ok(new AgentStateUpdateList(update)).type(MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(new AgentStateUpdateList(update)).type(MediaType.TEXT_XML).build();
    }
  }

  @GET
  @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
  @Path("agents/{name}/capabilities.{type:xml|json}")
  @RestQuery(
    name = "getAgentCapabilities",
    description = "Return the capabilities of a given capture agent",
    pathParameters = {
      @RestParameter(description = "Name of the capture agent", isRequired = true, name = "name", type = Type.STRING),
      @RestParameter(description = "The Document type", isRequired = true, name = "type", type = Type.STRING)
    }, restParameters = {}, reponses = {
      @RestResponse(description = "An XML representation of the agent capabilities", responseCode = SC_OK),
      @RestResponse(description = "The agent {name} does not exist in the system", responseCode = SC_NOT_FOUND)
    }, returnDescription = "")
  public Response getCapabilities(@PathParam("name") String agentName, @PathParam("type") String type)
          throws NotFoundException {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    PropertiesResponse r = new PropertiesResponse(service.getAgentCapabilities(agentName));
    if ("json".equals(type)) {
      return Response.ok(r).type(MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(r).type(MediaType.TEXT_XML).build();
    }
  }

  @GET
  @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
  @Path("agents/{name}/configuration.{type:xml|json}")
  @RestQuery(
    name = "getAgentConfiguration",
    description = "Return the configuration of a given capture agent",
    pathParameters = {
      @RestParameter(description = "Name of the capture agent", isRequired = true, name = "name", type = Type.STRING),
      @RestParameter(description = "The Document type", isRequired = true, name = "type", type = Type.STRING)
    }, restParameters = {}, reponses = {
      @RestResponse(description = "An XML or JSON representation of the agent configuration", responseCode = SC_OK),
      @RestResponse(description = "The agent {name} does not exist in the system", responseCode = SC_NOT_FOUND)
    }, returnDescription = "")
  public Response getConfiguration(@PathParam("name") String agentName, @PathParam("type") String type)
          throws NotFoundException {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    PropertiesResponse r = new PropertiesResponse(service.getAgentConfiguration(agentName));
    logger.debug("Returning configuration for the agent {}", agentName);

    if ("json".equals(type)) {
      return Response.ok(r).type(MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(r).type(MediaType.TEXT_XML).build();
    }
  }

  @POST
  @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
  @Path("agents/{name}/configuration")
  @RestQuery(
    name = "setAgentStateConfiguration",
    description = "Set the configuration of a given capture agent, registering it if it does not exist",
    pathParameters = {
      @RestParameter(description = "Name of the capture agent", isRequired = true, name = "name", type = Type.STRING)
    }, restParameters = {
      @RestParameter(description = "An XML or JSON representation of the capabilities. XML as specified in "
        + "http://java.sun.com/dtd/properties.dtd (friendly names as keys, device locations as corresponding values)",
        type = Type.TEXT, isRequired = true, name = "configuration")
    }, reponses = {
      @RestResponse(description = "An XML or JSON representation of the agent configuration", responseCode = SC_OK),
      @RestResponse(description = "The configuration format is incorrect OR the agent name is blank or null",
        responseCode = SC_BAD_REQUEST)
    }, returnDescription = "")
  public Response setConfiguration(@PathParam("name") String agentName, @FormParam("configuration") String configuration) {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    if (StringUtils.isBlank(configuration)) {
      logger.debug("The configuration data cannot be blank");
      return Response.serverError().status(Response.Status.BAD_REQUEST).build();
    }

    Properties caps;

    if (StringUtils.startsWith(configuration, "{")) {
      // JSON
      Gson gson = new Gson();
      try {
        caps = gson.fromJson(configuration, Properties.class);
        if (!service.setAgentConfiguration(agentName, caps)) {
          logger.debug("'{}''s configuration has not been updated because nothing has been changed", agentName);
        }
        return Response.ok(gson.toJson(caps)).type(MediaType.APPLICATION_JSON).build();
      } catch (JsonSyntaxException e) {
        logger.debug("Exception when deserializing capabilities: {}", e.getMessage());
        return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
      }

    } else {
      // XML
      caps = new Properties();
      ByteArrayInputStream bais = null;
      try {
        bais = new ByteArrayInputStream(configuration.getBytes());
        caps.loadFromXML(bais);
        if (!service.setAgentConfiguration(agentName, caps)) {
          logger.debug("'{}''s configuration has not been updated because nothing has been changed", agentName);
        }

        // Prepares the value to return
        PropertiesResponse r = new PropertiesResponse(caps);
        logger.debug("{}'s configuration updated", agentName);
        return Response.ok(r).type(MediaType.TEXT_XML).build();
      } catch (IOException e) {
        logger.debug("Unexpected I/O Exception when unmarshalling the capabilities: {}", e.getMessage());
        return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
      } finally {
        IOUtils.closeQuietly(bais);
      }
    }
  }

  @GET
  @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
  @Path("recordings/{id}.{type:xml|json|}")
  @RestQuery(
    name = "getRecordingState",
    description = "Return the state of a given recording",
    pathParameters = {
      @RestParameter(description = "The ID of a given recording", isRequired = true, name = "id", type = Type.STRING),
      @RestParameter(description = "The Documenttype", isRequired = true, name = "type", type = Type.STRING)
    }, restParameters = {}, reponses = {
      @RestResponse(description = "Returns the state of the recording with the correct id", responseCode = SC_OK),
      @RestResponse(description = "The recording with the specified ID does not exist", responseCode = SC_NOT_FOUND)
    }, returnDescription = "")
  public Response getRecordingState(@PathParam("id") String id, @PathParam("type") String type)
          throws NotFoundException {
    try {
      Recording rec = schedulerService.getRecordingState(id);

      logger.debug("Submitting state for recording {}", id);
      if ("json".equals(type)) {
        return Response.ok(new RecordingStateUpdate(rec)).type(MediaType.APPLICATION_JSON).build();
      } else {
        return Response.ok(new RecordingStateUpdate(rec)).type(MediaType.TEXT_XML).build();
      }
    } catch (SchedulerException e) {
      logger.debug("Unable to get recording state of {}", id, e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("recordings/{id}")
  @RestQuery(
    name = "setRecordingState",
    description = "Set the status of a given recording, registering it if it is new",
    pathParameters = {
      @RestParameter(description = "The ID of a given recording", isRequired = true, name = "id", type = Type.STRING)
    }, restParameters = {
      @RestParameter(description = "The state of the recording. Known states: unknown, capturing, capture_finished, "
        + "capture_error, manifest, manifest_error, manifest_finished, compressing, compressing_error, uploading, "
        + "upload_finished, upload_error.", isRequired = true, name = "state", type = Type.STRING)
    }, reponses = {
      @RestResponse(description = "{id} set to {state}", responseCode = SC_OK),
      @RestResponse(description = "{id} or {state} is empty or {state} is not known", responseCode = SC_BAD_REQUEST),
      @RestResponse(description = "Recording with {id} could not be found", responseCode = HttpServletResponse.SC_NOT_FOUND)
    }, returnDescription = "")
  public Response setRecordingState(@PathParam("id") String id, @FormParam("state") String state) throws NotFoundException {
    if (StringUtils.isEmpty(id) || StringUtils.isEmpty(state))
      return Response.serverError().status(Response.Status.BAD_REQUEST).build();

    try {
      if (schedulerService.updateRecordingState(id, state)) {
        return Response.ok(id + " set to " + state).build();
      } else {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
    } catch (SchedulerException e) {
      logger.debug("Unable to set recording state of {}", id, e);
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("recordings/{id}")
  @RestQuery(
    name = "removeRecording",
    description = "Remove record of a given recording",
    pathParameters = {
      @RestParameter(description = "The ID of a given recording", isRequired = true, name = "id", type = Type.STRING)
    }, restParameters = {}, reponses = {
      @RestResponse(description = "{id} removed", responseCode = SC_OK),
      @RestResponse(description = "{id} is empty", responseCode = SC_BAD_REQUEST),
      @RestResponse(description = "Recording with {id} could not be found", responseCode = SC_NOT_FOUND),
    }, returnDescription = "")
  public Response removeRecording(@PathParam("id") String id) throws NotFoundException {
    if (StringUtils.isEmpty(id))
      return Response.serverError().status(Response.Status.BAD_REQUEST).build();

    try {
      schedulerService.removeRecording(id);
      return Response.ok(id + " removed").build();
    } catch (SchedulerException e) {
      logger.debug("Unable to remove recording with id '{}'", id, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings")
  @RestQuery(name = "getAllRecordings", description = "Return all registered recordings and their state",
    pathParameters = {}, restParameters = {}, reponses = {
      @RestResponse(description = "Returns all known recordings.", responseCode = SC_OK) },
    returnDescription = "")
  public List<RecordingStateUpdate> getAllRecordings() {
    try {
      LinkedList<RecordingStateUpdate> update = new LinkedList<RecordingStateUpdate>();
      Map<String, Recording> data = schedulerService.getKnownRecordings();
      // Run through and build a map of updates (rather than states)
      for (Entry<String, Recording> e : data.entrySet()) {
        update.add(new RecordingStateUpdate(e.getValue()));
      }
      return update;
    } catch (SchedulerException e) {
      logger.debug("Unable to get all recordings", e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

}
