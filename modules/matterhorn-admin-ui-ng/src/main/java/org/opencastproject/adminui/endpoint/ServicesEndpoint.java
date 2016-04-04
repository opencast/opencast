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

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.exception.JsonCreationException;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.api.Service;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.index.service.util.ListProviderUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "ServicesProxyService", title = "UI Services", notes = "These Endpoints deliver informations about the services required for the UI.", abstractText = "This service provides the services data for the UI.")
public class ServicesEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(ServicesEndpoint.class);
  private ServiceRegistry serviceRegistry;

  public void activate() {
    logger.info("ServicesEndpoint is activated!");
  }

  @GET
  @Path("services.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Returns the list of services", name = "services", restParameters = {
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The offset", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "name", isRequired = false, description = "Filter results by service name", type = STRING),
          @RestParameter(name = "host", isRequired = false, description = "Filter results by host name", type = STRING),
          @RestParameter(name = "q", isRequired = false, description = "Filter results by free text query", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: ???.", type = STRING) }, reponses = { @RestResponse(description = "Returns the list of services from Matterhorn", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The list ")
  public Response getJobs(@QueryParam("limit") final int limit, @QueryParam("offset") final int offset,
          @QueryParam("name") String name, @QueryParam("host") String host, @QueryParam("q") String text,
          @QueryParam("sort") String sort, @Context HttpHeaders headers) throws Exception {

    String textFilter = text == null ? null : "textFilter=" + text;

    ResourceListQueryImpl query = new ResourceListQueryImpl();
    query.setLimit(limit);
    query.setOffset(offset);
    EndpointUtil.addRequestFiltersToQuery(textFilter, query);

    Map<String, Object> result = new HashMap<String, Object>();
    ServiceQueryResult services = getFilteredList(query);

    result.put("results", services.filteredResult);
    result.put("total", String.valueOf(services.totalCount));
    if (query != null) {
      if (query.getLimit().isSome()) {
        result.put("limit", Integer.toString(query.getLimit().get()));
      }
      if (query.getOffset().isSome()) {
        result.put("offset", Integer.toString(query.getOffset().get()));
      }
    }

    JSONObject jsonList;
    try {
      jsonList = EndpointUtil.generateJSONObject(result);
    } catch (JsonCreationException e) {
      logger.error("Not able to generate resources list JSON from the services list: {}", e);
      return Response.serverError().build();
    }

    return Response.ok(jsonList.toString()).build();
  }

  /**
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Query the list of services
   */
  private ServiceQueryResult getFilteredList(ResourceListQuery query) throws ServiceRegistryException {
    ServiceQueryResult result = new ServiceQueryResult();
    List<JSONAware> services = new ArrayList<JSONAware>();

    List<ServiceStatistics> serviceStatistics = serviceRegistry.getServiceStatistics();
    result.totalCount = serviceStatistics.size();
    for (ServiceStatistics stats : serviceStatistics) {
      Service service = new Service(stats);
      if (service.isCompliant(query)) {
        services.add(service);
      }
    }

    result.filteredResult = ListProviderUtil.filterMap(services, query);
    return result;
  }

  class ServiceQueryResult {
    private int totalCount;
    private List<JSONAware> filteredResult;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(int totalCount) {
      this.totalCount = totalCount;
    }

    public List<JSONAware> getFilteredResult() {
      return new ArrayList<JSONAware>(filteredResult);
    }

    public void setFilteredResult(List<JSONAware> filteredResult) {
      this.filteredResult = new ArrayList<JSONAware>(filteredResult);
    }
  }

}
