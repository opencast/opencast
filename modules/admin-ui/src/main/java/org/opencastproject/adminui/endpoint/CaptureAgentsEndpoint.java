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

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.index.service.util.RestUtils.okJson;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.util.TextFilter;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentAdminRoleProvider;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.index.service.resources.list.query.AgentsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchQuery.Order;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

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

@Path("/")
@RestService(name = "captureAgents", title = "Capture agents façade service",
  abstractText = "Provides operations for the capture agents",
  notes = { "This service offers the default capture agents CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class CaptureAgentsEndpoint {

  private static final String TRANSLATION_KEY_PREFIX = "CAPTURE_AGENT.DEVICE.";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentsEndpoint.class);

  /** The capture agent service */
  private CaptureAgentStateService service;

  private CaptureAgentAdminRoleProvider roleProvider;

  private SecurityService securityService;

  /**
   * Sets the capture agent service
   *
   * @param service
   *          the capture agent service to set
   */
  public void setCaptureAgentService(CaptureAgentStateService service) {
    this.service = service;
  }

  public void setRoleProvider(CaptureAgentAdminRoleProvider roleProvider) {
    this.roleProvider = roleProvider;
  }

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
          @RestParameter(defaultValue = "false", description = "Define if the inputs should or not returned with the capture agent.", isRequired = false, name = "inputs", type = RestParameter.Type.BOOLEAN),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: STATUS, NAME OR LAST_UPDATED.  Add '_DESC' to reverse the sort order (e.g. STATUS_DESC).", type = STRING) }, reponses = { @RestResponse(description = "An XML representation of the agent capabilities", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getAgents(@QueryParam("limit") int limit, @QueryParam("offset") int offset,
          @QueryParam("inputs") boolean inputs, @QueryParam("filter") String filter, @QueryParam("sort") String sort) {
    Option<String> filterName = Option.none();
    Option<String> filterStatus = Option.none();
    Option<Long> filterLastUpdated = Option.none();
    Option<String> filterText = Option.none();
    Option<String> optSort = Option.option(trimToNull(sort));

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      if (AgentsListQuery.FILTER_NAME_NAME.equals(name))
        filterName = Option.some(filters.get(name));
      if (AgentsListQuery.FILTER_STATUS_NAME.equals(name))
        filterStatus = Option.some(filters.get(name));
      if (AgentsListQuery.FILTER_LAST_UPDATED.equals(name)) {
        try {
          filterLastUpdated = Option.some(Long.parseLong(filters.get(name)));
        } catch (NumberFormatException e) {
          logger.info("Unable to parse long {}", filters.get(name));
          return Response.status(Status.BAD_REQUEST).build();
        }
      }
      if (AgentsListQuery.FILTER_TEXT_NAME.equals(name) && StringUtils.isNotBlank(filters.get(name)))
        filterText = Option.some(filters.get(name));
    }

    // Filter agents by filter criteria
    List<Agent> filteredAgents = new ArrayList<>();
    for (Entry<String, Agent> entry : service.getKnownAgents().entrySet()) {
      Agent agent = entry.getValue();

      // Filter list
      if ((filterName.isSome() && !filterName.get().equals(agent.getName()))
              || (filterStatus.isSome() && !filterStatus.get().equals(agent.getState()))
              || (filterLastUpdated.isSome() && filterLastUpdated.get() != agent.getLastHeardFrom())
              || (filterText.isSome() && !TextFilter.match(filterText.get(), agent.getName(), agent.getState())))
        continue;
      filteredAgents.add(agent);
    }
    int total = filteredAgents.size();

    // Sort by status, name or last updated date
    if (optSort.isSome()) {
      final Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
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
    List<JValue> agentsJSON = new ArrayList<>();
    for (Agent agent : filteredAgents) {
      agentsJSON.add(generateJsonAgent(agent, /* Option.option(room), blacklist, */ inputs, false));
    }

    return okJsonList(agentsJSON, offset, limit, total);
  }

  @DELETE
  @Path("{name}")
  @Produces({ MediaType.APPLICATION_JSON })
  @RestQuery(name = "removeAgent", description = "Remove record of a given capture agent", pathParameters = { @RestParameter(name = "name", description = "The name of a given capture agent", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {}, reponses = {
          @RestResponse(description = "{agentName} removed", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The agent {agentname} does not exist", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response removeAgent(@PathParam("name") String agentName) throws NotFoundException, UnauthorizedException {
    if (service == null)
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();

    SecurityUtil.checkAgentAccess(securityService, agentName);

    service.removeAgent(agentName);

    // Remove the corresponding capture agent roles
    this.roleProvider.removeRole(agentName);

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
    }, restParameters = {}, reponses = {
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
   * Generate a JSON Object for the given capture agent with its related blacklist periods
   *
   * @param agent
   *          The target capture agent
   * @param withInputs
   *          Whether the agent has inputs
   * @param details
   *          Whether the configuration and capabilities should be serialized
   * @return A {@link JValue} representing the capture agent
   */
  private JValue generateJsonAgent(Agent agent, boolean withInputs, boolean details) {
    List<Field> fields = new ArrayList<>();
    fields.add(f("Status", v(agent.getState(), Jsons.BLANK)));
    fields.add(f("Name", v(agent.getName())));
    fields.add(f("Update", v(toUTC(agent.getLastHeardFrom()), Jsons.BLANK)));
    fields.add(f("URL", v(agent.getUrl(), Jsons.BLANK)));

    if (withInputs) {
      String devices = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_NAMES);
      fields.add(f("inputs", (StringUtils.isEmpty(devices)) ? arr() : generateJsonDevice(devices.split(","))));
    }

    if (details) {
      fields.add(f("configuration", generateJsonProperties(agent.getConfiguration())));
      fields.add(f("capabilities", generateJsonProperties(agent.getCapabilities())));
    }

    return obj(fields);
  }

  /**
   * Generate JSON property list
   *
   * @param properties
   *          Java properties to be serialized
   * @return A JSON array containing the Java properties as key/value paris
   */
  private JValue generateJsonProperties(Properties properties) {
    List<JValue> fields = new ArrayList<>();
    if (properties != null) {
      for (String key : properties.stringPropertyNames()) {
        fields.add(obj(f("key", v(key)), f("value", v(properties.getProperty(key)))));
      }
    }
    return arr(fields);
  }

  /**
   * Generate a JSON devices list
   *
   * @param devices
   *          an array of devices String
   * @return A {@link JValue} representing the devices
   */
  private JValue generateJsonDevice(String[] devices) {
    List<JValue> jsonDevices = new ArrayList<>();
    for (String device : devices) {
      jsonDevices.add(obj(f("id", v(device)), f("value", v(TRANSLATION_KEY_PREFIX + device.toUpperCase()))));
    }
    return arr(jsonDevices);
  }
}
