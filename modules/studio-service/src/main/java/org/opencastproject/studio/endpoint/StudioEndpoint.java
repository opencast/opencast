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

package org.opencastproject.studio.endpoint;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.studio.endpoint.assembler.SeriesAssembler;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "seriesservice", title = "Series Service",
    abstractText = "This service creates, edits and retrieves and helps managing series.", notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the "
        + "underlying service is not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was "
        + "not anticipated. In other words, there is a bug! You should file an error report "
        + "with your server logs from the time when the error occurred: "
        + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
@Component(immediate = true, service = StudioEndpoint.class, property = { "service.description=Studio REST Endpoint",
    "opencast.service.type=org.opencastproject.studio", "opencast.service.path=/studio-api" })
public class StudioEndpoint {

  private final Gson gson;

  private ElasticsearchIndex elasticsearchIndex;
  private SecurityService securityService;

  {
    gson = new GsonBuilder().serializeNulls().create();
  }

  @GET
  @Path("/series.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getSeries", description = "Returns series where the current user has write permissions.",
      returnDescription = "A list of JSON series objects",
      responses = {
      @RestResponse(description = "Returns a list of series.",
          responseCode = HttpServletResponse.SC_OK)
  })
  public Response getSeries() {
    SeriesSearchQuery query = new SeriesSearchQuery(securityService.getOrganization().getId(),
        securityService.getUser());
    query.withoutActions();
    query.withAction(Permissions.Action.WRITE);

    try {
      return Response.ok(gson.toJson(Arrays.stream(elasticsearchIndex.getByQuery(query).getItems())
              .map(SearchResultItem::getSource)
              .map(SeriesAssembler::toDto)
              .collect(Collectors.toList())
          )).build();
    } catch (SearchIndexException e) {
      throw new WebApplicationException(e);
    }

  }

  @Reference
  public void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}
