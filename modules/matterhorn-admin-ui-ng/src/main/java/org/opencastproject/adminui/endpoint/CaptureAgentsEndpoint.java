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
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.opencastproject.index.service.util.JSONUtils.blacklistToJSON;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.index.service.resources.list.query.AgentsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchQuery.Order;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase.SortType;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.JField;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "captureAgents", title = "Capture agents façade service", notes = "This service offers the default capture agents CRUD Operations for the admin UI.", abstractText = "Provides operations for the capture agents")
public class CaptureAgentsEndpoint {

  private static final String TRANSLATION_KEY_PREFIX = "CAPTURE_AGENT.DEVICE.";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentsEndpoint.class);

  /** The capture agent service */
  private CaptureAgentStateService service;

  /** The participation persistence */
  private ParticipationManagementDatabase participationPersistence;

  /**
   * Sets the capture agent service
   *
   * @param service
   *          the capture agent service to set
   */
  public void setCaptureAgentService(CaptureAgentStateService service) {
    this.service = service;
  }

  /** OSGi callback for participation persistence. */
  public void setParticipationPersistence(ParticipationManagementDatabase participationPersistence) {
    this.participationPersistence = participationPersistence;
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
    }

    // Get list of agents from the PM
    Map<String, CaptureAgent> captureAgents = new HashMap<String, CaptureAgent>();
    if (participationPersistence != null) {
      try {
        for (CaptureAgent agent : participationPersistence.getCaptureAgents()) {
          captureAgents.put(agent.getMhAgent(), agent);
        }
      } catch (ParticipationManagementDatabaseException e) {
        logger.warn("Not able to get the capture agents from the participation management persistence service: {}", e);
        return Response.status(SC_INTERNAL_SERVER_ERROR).build();
      }
    }

    // Filter agents by filter criteria
    List<Agent> filteredAgents = new ArrayList<Agent>();
    for (Entry<String, Agent> entry : service.getKnownAgents().entrySet()) {
      Agent agent = entry.getValue();

      // Filter list
      if ((filterName.isSome() && !filterName.get().equals(agent.getName()))
              || (filterStatus.isSome() && !filterStatus.get().equals(agent.getState()))
              || (filterLastUpdated.isSome() && filterLastUpdated.get() != agent.getLastHeardFrom()))
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
                logger.info("Unkown sort type: {}", criterion.getFieldName());
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
    List<JValue> agentsJSON = new ArrayList<JValue>();
    for (Agent agent : filteredAgents) {
      List<Blacklist> blacklist = new ArrayList<Blacklist>();

      Room room = null;
      CaptureAgent captureAgent = captureAgents.get(agent.getName());
      try {
        if (captureAgent != null) {
          room = participationPersistence.getRoom(captureAgent.getRoom().getId());
          blacklist.addAll(participationPersistence.findBlacklists(room));
        }
      } catch (ParticipationManagementDatabaseException e) {
        logger.warn("Not able to find the blacklist for the agent {} {}:", agent.getName(), e);
        return Response.status(SC_INTERNAL_SERVER_ERROR).build();
      } catch (NotFoundException e) {
        logger.debug("Not able to find the capture agent in the room {}.", captureAgent.getRoom());
      }
      agentsJSON.add(generateJsonAgent(agent, Option.option(room), blacklist, inputs));
    }

    return okJsonList(agentsJSON, offset, limit, total);
  }

  /**
   * Generate a JSON Object for the given capture agent with its related blacklist periods
   *
   * @param agent
   *          The target capture agent
   * @param room
   *          the participation room
   * @param blacklist
   *          The blacklist periods related to the capture agent
   * @param withInputs
   *          Whether the agent has inputs
   * @return A {@link JValue} representing the capture agent
   */
  private JValue generateJsonAgent(Agent agent, Option<Room> room, List<Blacklist> blacklist, boolean withInputs) {
    JValue blacklistJSON = blacklistToJSON(blacklist);

    List<JField> fields = new ArrayList<JField>();
    fields.add(f("Status", vN(agent.getState())));
    fields.add(f("Name", v(agent.getName())));
    fields.add(f("Update", vN(DateTimeSupport.toUTC(agent.getLastHeardFrom()))));
    if (room.isSome()) {
      fields.add(f("roomId", v(room.get().getId())));
    } else {
      fields.add(f("roomId", v(-1)));
    }
    fields.add(f("blacklist", blacklistJSON));

    if (withInputs) {
      String devices = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_NAMES);
      fields.add(f("inputs", (StringUtils.isEmpty(devices)) ? a() : generateJsonDevice(devices.split(","))));
    }

    return j(fields);
  }

  /**
   * Generate a JSON devices list
   *
   * @param devices
   *          an array of devices String
   * @return A {@link JValue} representing the devices
   */
  private JValue generateJsonDevice(String[] devices) {
    List<JValue> jsonDevices = new ArrayList<JValue>();
    for (String device : devices) {
      jsonDevices.add(j(f("id", v(device)), f("value", v(TRANSLATION_KEY_PREFIX + device.toUpperCase()))));
    }
    return a(jsonDevices);
  }

  private Option<SortType> getAgentSortField(String input) {
    if (StringUtils.isNotBlank(input)) {
      String upperCase = input.toUpperCase();
      SortType sortType = null;
      try {
        sortType = SortType.valueOf(upperCase);
      } catch (IllegalArgumentException e) {
        return Option.<SortType> none();
      }
      return Option.option(sortType);
    }
    return Option.<SortType> none();
  }

}
