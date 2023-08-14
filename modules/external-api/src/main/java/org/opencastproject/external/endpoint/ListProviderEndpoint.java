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
package org.opencastproject.external.endpoint;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.impl.ListProviderNotFoundException;
import org.opencastproject.list.impl.ResourceListQueryImpl;
import org.opencastproject.list.query.StringListFilter;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.json.simple.JSONArray;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_10_0 })
@RestService(
        name = "externalapilistproviders",
        title = "External API List Providers Service",
        notes = {},
        abstractText = "Provides resources and operations related to configurable lists"
)
@Component(
        immediate = true,
        service = ListProviderEndpoint.class,
        property = {
                "service.description=External API - List Providers Endpoint",
                "opencast.service.type=org.opencastproject.external.listproviders",
                "opencast.service.path=/api/listproviders"
        }
)
public class ListProviderEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ListProviderEndpoint.class);

  /** The capture agent service */
  private ListProvidersService listProvidersService;

  /** OSGi DI */
  @Reference
  public void setListProvidersService(ListProvidersService listProvidersService) {
    this.listProvidersService = listProvidersService;
  }

  /** OSGi activation method */
  @Activate
  void activate(ComponentContext cc) {
    logger.info("Activating External API - List Providers Endpoint");
  }

  @GET
  @Path("providers.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "availableProviders", description = "Provides the list of the available list providers", responses = { @RestResponse(description = "Returns the availables list providers.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getAvailablesProviders(@Context HttpHeaders headers) {
    JSONArray list = new JSONArray();

    list.add(listProvidersService.getAvailableProviders());

    return Response.ok(list.toString()).build();
  }

  @GET
  @Path("{source}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "list", description = "Provides key-value list from the given source", pathParameters = { @RestParameter(name = "source", description = "The source for the key-value list", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(description = "The maximum number of items to return per page", isRequired = false, name = "limit", type = RestParameter.Type.INTEGER),
          @RestParameter(description = "The offset", isRequired = false, name = "offset", type = RestParameter.Type.INTEGER),
          @RestParameter(description = "Filters", isRequired = false, name = "filter", type = RestParameter.Type.STRING) }, responses = { @RestResponse(description = "Returns the key-value list for the given source.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getList(@PathParam("source") final String source, @QueryParam("limit") final int limit,
          @QueryParam("filter") final String filter, @QueryParam("offset") final int offset,
          @Context HttpHeaders headers) {

    ResourceListQueryImpl query = new ResourceListQueryImpl();
    query.setLimit(limit);
    query.setOffset(offset);
    addRequestFiltersToQuery(filter, query);
    Map<String, String> autocompleteList;
    try {
      autocompleteList = listProvidersService.getList(source, query, false);
    } catch (ListProviderNotFoundException e) {
      logger.debug("No list found for {}", source, e);
      return ApiResponses.notFound("");
    } catch (ListProviderException e) {
      logger.error("Server error when getting list from provider {}", source, e);
      return ApiResponses.serverError("");
    }

    Gson gson = new Gson();
    String jsonList = gson.toJson(autocompleteList);
    return Response.ok(jsonList).build();
  }

  /**
   * Add the string based filters to the given list query.
   *
   * @param filterString
   *          The string based filters
   * @param query
   *          The query to update with the filters
   */
  private void addRequestFiltersToQuery(final String filterString, ResourceListQueryImpl query) {
    if (filterString != null) {
      String[] filters = filterString.split(",");
      for (String filter : filters) {
        String[] splitFilter = filter.split(":", 2);
        if (splitFilter != null && splitFilter.length == 2) {
          String key = splitFilter[0].trim();
          String value = splitFilter[1].trim();
          query.addFilter(new StringListFilter(key, value));
        }
      }
    }
  }

}

