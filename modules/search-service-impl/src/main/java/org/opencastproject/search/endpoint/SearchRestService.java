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

package org.opencastproject.search.endpoint;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.impl.SearchServiceImpl;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint
 */
@Path("/")
@RestService(
    name = "search",
    title = "Search Service",
    abstractText = "This service indexes and queries available (distributed) episodes.",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the "
            + "underlying service is not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was "
            + "not anticipated. In other words, there is a bug! You should file an error report "
            + "with your server logs from the time when the error occurred: "
            + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"
    }
)
@Component(
    immediate = true,
    service = SearchRestService.class,
    property = {
        "service.description=Search REST Endpoint",
        "opencast.service.type=org.opencastproject.search",
        "opencast.service.path=/search",
        "opencast.service.jobproducer=true"
    }
)
public class SearchRestService extends AbstractJobProducerEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(SearchRestService.class);

  /** The search service */
  protected SearchServiceImpl searchService;

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  private SecurityService securityService;

  private SeriesService seriesService;

  private final Gson gson = new Gson();

  @GET
  @Path("series.json")
  @Produces( MediaType.APPLICATION_JSON )
  @RestQuery(
      name = "get_series",
      description = "Search for series matching the query parameters.",
      restParameters = {
          @RestParameter(
              name = "id",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "The series ID. If the additional boolean parameter \"episodes\" is \"true\", "
                  + "the result set will include this series episodes."
          ),
          @RestParameter(
              name = "q",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "Any series that matches this free-text query."
          ),
          @RestParameter(
              name = "sort",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "The sort order.  May include any of the following dublin core metadata: "
              + "title, contributor, creator, modified. "
              + "Add ' asc' or ' desc' to specify the sort order (e.g. 'title desc')."
          ),
          @RestParameter(
              name = "limit",
              isRequired = false,
              type = RestParameter.Type.INTEGER,
              defaultValue = "20",
              description = "The maximum number of items to return per page."
          ),
          @RestParameter(
              name = "offset",
              isRequired = false,
              type = RestParameter.Type.INTEGER,
              defaultValue = "0",
              description = "The page number."
          )
      },
      responses = {
          @RestResponse(
              description = "The request was processed successfully.",
              responseCode = HttpServletResponse.SC_OK
          )
      },
      returnDescription = "The search results, formatted as XML or JSON."
  )
  public Response getSeries(
      @QueryParam("id")       String  id,
      @QueryParam("q")        String  text,
      @QueryParam("sort")     String  sort,
      @QueryParam("limit")    String  limit,
      @QueryParam("offset")   String  offset
  ) throws SearchException {

    var json = gson.toJsonTree(Map.of(
        "offset", offset,
        "total", 0,
        "result", Collections.emptyList(),
        "limit", limit));
    return Response.ok(gson.toJson(json)).build();

  }

  @GET
  @Path("episode.json")
  @Produces( MediaType.APPLICATION_JSON )
  @RestQuery(
      name = "search_episodes",
      description = "Search for episodes matching the query parameters.",
      restParameters = {
          @RestParameter(
              name = "id",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "The ID of the single episode to be returned, if it exists."
          ),
          @RestParameter(
              name = "q",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "Any episode that matches this free-text query."
          ),
          @RestParameter(
              name = "sid",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "Any episode that belongs to specified series id."
          ),
          @RestParameter(
              name = "sname",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "Any episode that belongs to specified series name (note that the "
                  + "specified series name must be unique)."
          ),
          @RestParameter(
              name = "sort",
              isRequired = false,
              type = RestParameter.Type.STRING,
              description = "The sort order.  May include any of the following dublin core metadata: "
                  + "title, contributor, creator, modified. "
                  + "Add ' asc' or ' desc' to specify the sort order (e.g. 'title desc')."
          ),
          @RestParameter(
              name = "limit",
              isRequired = false,
              type = RestParameter.Type.INTEGER,
              defaultValue = "20",
              description = "The maximum number of items to return per page. Limited to 250 for non-admins."
          ),
          @RestParameter(
              name = "offset",
              isRequired = false,
              type = RestParameter.Type.INTEGER,
              defaultValue = "0",
              description = "The page number."
          ),
          @RestParameter(
              name = "sign",
              type = RestParameter.Type.BOOLEAN,
              isRequired = false,
              defaultValue = "false",
              description = "If results are to be signed"
          )
      },
      responses = {
          @RestResponse(
              description = "The request was processed successfully.",
              responseCode = HttpServletResponse.SC_OK
          )
      },
      returnDescription = "The search results, formatted as xml or json."
  )
  public Response getEpisodes(
      @QueryParam("id") String id,
      @QueryParam("q") String text,
      @QueryParam("sid") String seriesId,
      @QueryParam("sname") String seriesName, // TODO
      @QueryParam("sort") String sort,
      @QueryParam("limit") String limit,
      @QueryParam("offset") String offset,
      @QueryParam("sign") String sign // TODO
  ) throws SearchException {

    // There can only be one, sid or sname
    if (StringUtils.isNoneEmpty(seriesName, seriesId)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("invalid request, both 'sid' and 'sname' specified")
          .build();
    }

    if (StringUtils.isNotEmpty(id)) {
      // TODO: searchService.get(id);
    }

    var user = securityService.getUser();
    var org = securityService.getOrganization();
    var admin = user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE) || user.hasRole(org.getAdminRole());
    var size = NumberUtils.toInt(limit, 20);
    var from = NumberUtils.toInt(offset);
    if (size < 0 || from < 0) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Limit and offset may not be negative.")
          .build();
    }
    if (!admin && size > 250) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Only admins are allowed to request more than 250 items.")
          .build();
    }

    var sortMapping = Map.of(
        "created", "creationDate",
        "modified", "modificationDate");
    sort = StringUtils.defaultIfBlank(sort, "created");
    var sortParam = StringUtils.split(sort);
    var ascending = sortParam.length < 2 || sortParam[1].equals("asc");
    if (!sortMapping.containsKey(sortParam[0])) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Invalid sort parameter: " + sortParam[0])
          .build();
    }
    sort = sortMapping.get(sortParam[0]);

    var result = searchService.search(
            StringUtils.trimToNull(seriesId),
            StringUtils.trimToNull(seriesName),
            StringUtils.trimToNull(text),
            sort,
            ascending,
            size,
            from).stream()
        .map(MediaPackageParser::getAsJSON)
        .reduce((mp1, mp2) -> mp1 + "," + mp2)
        .orElse("");

    var total = 0;

    var body = String.format("{"
            + "\"offset\": %s,"
            + "\"limit\": %s,"
            + "\"total\": %s,"
            + "\"result\": [%s]}",
        from, size, total, result);

    return Response.ok(body).build();
  }

  /**
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    return searchService;
  }

  /**
   * Callback from OSGi to set the search service implementation.
   *
   * @param searchService
   *          the service implementation
   */
  @Reference
  public void setSearchService(SearchServiceImpl searchService) {
    this.searchService = searchService;
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
