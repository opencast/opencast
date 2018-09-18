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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static org.opencastproject.external.util.CaptureAgentUtils.generateJsonAgent;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.JValue;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_1_0 })
@RestService(
    name = "externalapicaptureagents",
    title = "External API Capture Agents Service",
    notes = "",
    abstractText = "Provides resources and operations related to the capture agents"
)
public class CaptureAgentsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentsEndpoint.class);

  /** The capture agent service */
  private CaptureAgentStateService agentStateService;

  /** OSGi DI */
  public CaptureAgentStateService getAgentStateService() {
    return agentStateService;
  }

  /** OSGi DI */
  public void setAgentStateService(CaptureAgentStateService agentStateService) {
    this.agentStateService = agentStateService;
  }


  /** OSGi activation method */
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Capture Agents Endpoint");
  }

  @GET
  @Path("{agentId}")
  @RestQuery(
      name = "getagent",
      description = "Returns a single capture agent.",
      returnDescription = "",
      pathParameters = {
          @RestParameter(name = "agentId", description = "The agent id", isRequired = true, type = STRING)
      },
      reponses = {
          @RestResponse(description = "The agent is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The specified agent does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND)
      }
  )
  public Response getAgent(
      @HeaderParam("Accept") String acceptHeader,
      @PathParam("agentId") String id) throws Exception {
    final Agent agent = agentStateService.getAgent(id);

    if (agent == null) {
      return ApiResponses.notFound("Cannot find an agent with id '%s'.", id);
    }

    return ApiResponses.Json.ok(acceptHeader, generateJsonAgent(agent));
  }

  @GET
  @Path("/")
  @RestQuery(
      name = "getagents",
      description = "Returns a list of agents.",
      returnDescription = "",
      restParameters = {
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = Type.INTEGER)
      },
      reponses = {
          @RestResponse(description = "A (potentially empty) list of agents is returned.", responseCode = HttpServletResponse.SC_OK)
      }
  )
  public Response getAgents(
      @HeaderParam("Accept") String acceptHeader,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit) {

    List<Agent> agents = new ArrayList<>(agentStateService.getKnownAgents().values());

    // Apply offset
    if (offset != null && offset > 0) {
      agents = agents.subList(Math.min(offset, agents.size()), agents.size());
    }

    // Apply limit
    if (limit != null && limit > 0) {
      agents = agents.subList(0, Math.min(limit, agents.size()));
    }

    final List<JValue> agentsJSON = agents.stream()
        .map(a -> generateJsonAgent(a))
        .collect(Collectors.toList());

    return ApiResponses.Json.ok(acceptHeader, arr(agentsJSON));
  }


}
