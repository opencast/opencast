/*
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

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.index.service.util.RestUtils.okJson;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.util.TextFilter;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.index.service.resources.list.query.AgentsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.util.requests.SortCriterion.Order;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/admin-ng/capture-agents")
@RestService(name = "captureAgents", title = "Capture agents façade service",
  abstractText = "Provides operations for the capture agents",
  notes = { "This service offers the default capture agents CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
@Component(
  immediate = true,
  service = CaptureAgentsEndpoint.class,
  property = {
    "service.description=Admin UI - Capture agents facade Endpoint",
    "opencast.service.type=org.opencastproject.adminui.endpoint.UsersEndpoint",
    "opencast.service.path=/admin-ng/capture-agents"
  }
)
@JaxrsResource
public class CaptureAgentsEndpoint {

  private static final String TRANSLATION_KEY_PREFIX = "CAPTURE_AGENT.DEVICE.";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentsEndpoint.class);

  /** The capture agent service */
  private CaptureAgentStateService service;

  private SecurityService securityService;

  /**
   * Sets the capture agent service
   *
   * @param service
   *          the capture agent service to set
   */
  @Reference
  public void setCaptureAgentService(CaptureAgentStateService service) {
    this.service = service;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("agents.json")
  @RestQuery(name = "getAgents", description = "Return all of the known capture agents on the system", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING),
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Define if the parsed capabilities should or not returned with the capture agent.", isRequired = false, name = "withParsedCapabilities", type = RestParameter.Type.BOOLEAN),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: STATUS, NAME OR LAST_UPDATED.  Add '_DESC' to reverse the sort order (e.g. STATUS_DESC).", type = STRING) }, responses = { @RestResponse(description = "An XML representation of the agent capabilities", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getAgents(@QueryParam("limit") int limit, @QueryParam("offset") int offset,
        @QueryParam("withParsedCapabilities") boolean withParsedCapabilities, @QueryParam("filter") String filter, @QueryParam("sort") String sort) {
    Optional<String> filterName = Optional.empty();
    Optional<String> filterStatus = Optional.empty();
    Optional<Long> filterLastUpdated = Optional.empty();
    Optional<String> filterText = Optional.empty();
    Optional<String> optSort = Optional.ofNullable(trimToNull(sort));

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      if (AgentsListQuery.FILTER_NAME_NAME.equals(name))
        filterName = Optional.of(filters.get(name));
      if (AgentsListQuery.FILTER_STATUS_NAME.equals(name))
        filterStatus = Optional.of(filters.get(name));
      if (AgentsListQuery.FILTER_LAST_UPDATED.equals(name)) {
        try {
          filterLastUpdated = Optional.of(Long.parseLong(filters.get(name)));
        } catch (NumberFormatException e) {
          logger.info("Unable to parse long {}", filters.get(name));
          return Response.status(Status.BAD_REQUEST).build();
        }
      }
      if (AgentsListQuery.FILTER_TEXT_NAME.equals(name) && StringUtils.isNotBlank(filters.get(name)))
        filterText = Optional.of(filters.get(name));
    }

    // Filter agents by filter criteria
    List<Agent> filteredAgents = new ArrayList<>();
    for (Entry<String, Agent> entry : service.getKnownAgents().entrySet()) {
      Agent agent = entry.getValue();

      // Filter list
      if ((filterName.isPresent() && !filterName.get().equals(agent.getName()))
              || (filterStatus.isPresent() && !filterStatus.get().equals(agent.getState()))
              || (filterLastUpdated.isPresent() && filterLastUpdated.get() != agent.getLastHeardFrom())
              || (filterText.isPresent() && !TextFilter.match(filterText.get(), agent.getName(), agent.getState())))
        continue;
      filteredAgents.add(agent);
    }
    int total = filteredAgents.size();

    // Sort by status, name or last updated date
    if (optSort.isPresent()) {
      final ArrayList<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      Collections.sort(filteredAgents, new Comparator<Agent>() {
        @Override
        public int compare(Agent agent1, Agent agent2) {
          for (SortCriterion criterion : sortCriteria) {
            Order order = criterion.getOrder();
            switch (criterion.getFieldName()) {
              case "status":
                if (order.equals(Order.Descending))
                  return agent2.getState().compareTo(agent1.getState());
                return agent1.getState().compareTo(agent2.getState());
              case "name":
                if (order.equals(Order.Descending))
                  return agent2.getName().compareTo(agent1.getName());
                return agent1.getName().compareTo(agent2.getName());
              case "updated":
                if (order.equals(Order.Descending))
                  return agent2.getLastHeardFrom().compareTo(agent1.getLastHeardFrom());
                return agent1.getLastHeardFrom().compareTo(agent2.getLastHeardFrom());
              default:
                logger.info("Unknown sort type: {}", criterion.getFieldName());
                return 0;
            }
          }
          return 0;
        }
      });
    }

    // Apply Limit and offset
    filteredAgents = new SmartIterator<Agent>(limit, offset).applyLimitAndOffset(filteredAgents);

    // Run through and build a map of updates (rather than states)
    List<JsonObject> agentsJSON = new ArrayList<>();
    for (Agent agent : filteredAgents) {
      agentsJSON.add(generateJsonAgent(agent, withParsedCapabilities, false));
    }

    return okJsonList(agentsJSON, offset, limit, total);
  }

  @DELETE
  @Path("{name}")
  @Produces({ MediaType.APPLICATION_JSON })
  @RestQuery(name = "removeAgent", description = "Remove record of a given capture agent", pathParameters = { @RestParameter(name = "name", description = "The name of a given capture agent", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {}, responses = {
          @RestResponse(description = "{agentName} removed", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The agent {agentname} does not exist", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response removeAgent(@PathParam("name") String agentName) throws NotFoundException, UnauthorizedException {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    SecurityUtil.checkAgentAccess(securityService, agentName);

    service.removeAgent(agentName);

    logger.debug("The agent {} was successfully removed", agentName);
    return Response.status(SC_OK).build();
  }

  @GET
  @Path("{name}")
  @Produces({ MediaType.APPLICATION_JSON })
  @RestQuery(
    name = "getAgent",
    description = "Return the capture agent including its configuration and capabilities",
    pathParameters = {
      @RestParameter(description = "Name of the capture agent", isRequired = true, name = "name", type = RestParameter.Type.STRING),
    }, restParameters = {}, responses = {
      @RestResponse(description = "A JSON representation of the capture agent", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "The agent {name} does not exist in the system", responseCode = HttpServletResponse.SC_NOT_FOUND)
    }, returnDescription = "")
  public Response getAgent(@PathParam("name") String agentName)
          throws NotFoundException {
    if (service != null) {
      Agent agent = service.getAgent(agentName);
      if (agent != null) {
        return okJson(generateJsonAgent(agent, true, true));
      } else {
        return Response.status(Status.NOT_FOUND).build();
      }
    } else {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
  }

  /**
   * Generate a JSON Object for the given capture agent
   *
   * @param agent
   *          The target capture agent
   * @param withParsedCapabilities
   *          Add capabilities as individual fields and pre-parse them
   * @param details
   *          Whether the configuration and capabilities should be serialized
   * @return A {@link JsonObject} representing the capture agent
   */
  private JsonObject generateJsonAgent(Agent agent, boolean withParsedCapabilities, boolean details) {
    JsonObject json = new JsonObject();
    String status = AgentState.TRANSLATION_PREFIX + agent.getState().toUpperCase();
    json.addProperty("Status", safeString(status));
    json.addProperty("Name", agent.getName());
    json.addProperty("Update", safeString(toUTC(agent.getLastHeardFrom())));
    json.addProperty("URL", safeString(agent.getUrl()));

    if (withParsedCapabilities) {
      JsonObject parsedCapabilities = new JsonObject();

      String inputs = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_NAMES);
      if (inputs == null || inputs.isEmpty()) {
        parsedCapabilities.add("inputs", new JsonArray());
      } else {
        String[] parsedInputs = Arrays.stream(inputs.split(","))
            .map(String::trim)
            .toArray(String[]::new);
        parsedCapabilities.add("inputs", generateJsonDevice(parsedInputs, TRANSLATION_KEY_PREFIX + "INPUTS."));
      }

      String stream = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_STREAM);
      if (stream == null || stream.isEmpty()) {
        parsedCapabilities.add("stream", new JsonArray());
      } else {
        String[] parsedStream = Arrays.stream(stream.split(","))
            .map(String::trim)
            .toArray(String[]::new);
        parsedCapabilities.add("stream", generateJsonDevice(parsedStream, TRANSLATION_KEY_PREFIX + "STREAM."));
      }

      String record = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_RECORD);
      if (record == null || record.isEmpty()) {
        parsedCapabilities.add("stream", new JsonArray());
      } else {
        String[] parsedRecord = Arrays.stream(record.split(","))
            .map(String::trim)
            .toArray(String[]::new);
        parsedCapabilities.add("record", generateJsonDevice(parsedRecord, TRANSLATION_KEY_PREFIX + "RECORD."));
      }

      String layout = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_LAYOUT);
      if (layout == null || layout.isEmpty()) {
        parsedCapabilities.add("stream", new JsonArray());
      } else {
        String[] parsedLayout = Arrays.stream(layout.split(","))
            .map(String::trim)
            .toArray(String[]::new);
        parsedCapabilities.add("layout", generateJsonDevice(parsedLayout, TRANSLATION_KEY_PREFIX + "LAYOUT."));
      }

      String cameraPosition = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_CAMERA_POSITION);
      if (cameraPosition == null || cameraPosition.isEmpty()) {
        parsedCapabilities.add("cameraPosition", new JsonArray());
      } else {
        String[] parsedCameraPosition = Arrays.stream(cameraPosition.split(","))
            .map(String::trim)
            .toArray(String[]::new);
        parsedCapabilities.add("cameraPosition", generateJsonDevice(parsedCameraPosition, TRANSLATION_KEY_PREFIX + "CAMERA_POSITION."));
      }

      json.add("parsedCapabilities", parsedCapabilities);
    }

    if (details) {
      json.add("configuration", generateJsonProperties(agent.getConfiguration()));
      json.add("capabilities", generateJsonProperties(agent.getCapabilities()));
    }

    return json;
  }

  /**
   * Generate JSON property list
   *
   * @param properties
   *          Java properties to be serialized
   * @return A JSON array containing the Java properties as key/value paris
   */
  private JsonArray generateJsonProperties(Properties properties) {
    JsonArray jsonFields = new JsonArray();

    if (properties != null) {
      for (String key : properties.stringPropertyNames()) {
        JsonObject jsonField = new JsonObject();
        jsonField.addProperty("key", key);
        jsonField.addProperty("value", properties.getProperty(key));
        jsonFields.add(jsonField);
      }
    }

    return jsonFields;
  }

  /**
   * Generate a JSON devices list
   *
   * @param devices
   *          an array of devices String
   * @return A {@link JsonArray} representing the devices
   */
  private JsonArray generateJsonDevice(String[] devices, String translationPrefix) {
    JsonArray jsonDevices = new JsonArray();

    for (String device : devices) {
      JsonObject jsonDevice = new JsonObject();
      jsonDevice.addProperty("id", device);
      jsonDevice.addProperty("value", translationPrefix + device.toUpperCase());
      jsonDevices.add(jsonDevice);
    }

    return jsonDevices;
  }
}
