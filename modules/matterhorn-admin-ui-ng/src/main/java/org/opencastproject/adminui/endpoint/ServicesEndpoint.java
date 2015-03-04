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

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private ListProvidersEndpoint listProvidersEndpoint;

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
    return listProvidersEndpoint.getList("services", limit, textFilter, offset, headers);
  }

  /**
   * @param listProvidersEndpoint
   *          the listProvidersEndpoint to set
   */
  public void setListProvidersEndpoint(ListProvidersEndpoint listProvidersEndpoint) {
    this.listProvidersEndpoint = listProvidersEndpoint;
  }

}
