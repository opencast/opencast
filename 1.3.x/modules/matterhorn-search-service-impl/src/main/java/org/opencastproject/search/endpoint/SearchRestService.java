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
package org.opencastproject.search.endpoint;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * The REST endpoint
 */
@Path("/")
@RestService(name = "search", title = "Search Service", notes = {
        "All paths are relative to the REST endpoint base (something like http://your.server/files)",
        "If you notice that this service is not working as expected, there might be a bug! "
                + "You should file an error report with your server logs from the time when the error occurred: "
                + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>" }, abstractText = "This service indexes and queries available (distributed) episodes.")
public class SearchRestService {

  private static final Logger logger = LoggerFactory.getLogger(SearchRestService.class);

  protected SearchService searchService;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */

  public void activate(ComponentContext cc) {
    // String serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  public String getSampleMediaPackage() {
    return "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\"><title>t1</title>\n"
            + "  <metadata>\n"
            + "    <catalog id=\"catalog-1\" type=\"dublincore/episode\">\n"
            + "      <mimetype>text/xml</mimetype>\n"
            + "      <url>https://opencast.jira.com/svn/MH/trunk/modules/matterhorn-kernel/src/test/resources/dublincore.xml</url>\n"
            + "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n"
            + "    </catalog>\n"
            + "    <catalog id=\"catalog-3\" type=\"metadata/mpeg-7\" ref=\"track:track-1\">\n"
            + "      <mimetype>text/xml</mimetype>\n"
            + "      <url>https://opencast.jira.com/svn/MH/trunk/modules/matterhorn-kernel/src/test/resources/mpeg7.xml</url>\n"
            + "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n"
            + "    </catalog>\n"
            + "  </metadata>\n" + "</ns2:mediapackage>";
  }

  @POST
  @Path("add")
  @RestQuery(name = "add", description = "Adds a mediapackage to the search index.", restParameters = { @RestParameter(description = "The media package to add to the search index.", isRequired = true, name = "mediapackage", type = RestParameter.Type.TEXT, defaultValue = "${this.sampleMediaPackage}") }, reponses = {
          @RestResponse(description = "The mediapackage was added, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "There has been an internal error and the mediapackage could not be added", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "No content is returned.")
  public Response add(@FormParam("mediapackage") MediaPackageImpl mediaPackage) throws SearchException {
    try {
      searchService.add(mediaPackage);
      return Response.noContent().build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("{id}")
  @RestQuery(name = "remove", description = "Removes a mediapackage from the search index.", pathParameters = { @RestParameter(description = "The media package ID to remove from the search index.", isRequired = true, name = "id", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "The mediapackage was removed, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "There has been an internal error and the mediapackage could not be added", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "No content is returned.")
  public Response remove(@PathParam("id") String mediaPackageId) throws SearchException, NotFoundException {
    try {
      if (searchService.delete(mediaPackageId))
        return Response.noContent().build();
      throw new NotFoundException();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("series.{format:xml|json}")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "series", description = "Search for series matching the query parameters.", pathParameters = { @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(description = "The series ID. If the additional boolean parameter \"episodes\" is \"true\", "
                  + "the result set will include this series episodes.", isRequired = false, name = "id", type = RestParameter.Type.STRING),
          @RestParameter(description = "Any series that matches this free-text query. If the additional boolean parameter \"episodes\" is \"true\", "
                  + "the result set will include this series episodes.", isRequired = false, name = "q", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether to include this series episodes. This can be used in combination with \"id\" or \"q\".", isRequired = false, name = "episodes", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether to include this series information itself. This can be used in combination with \"id\" or \"q\".", isRequired = false, name = "series", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false, name = "admin", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, expressed as xml or json.")
  public Response getEpisodeAndSeriesById(@QueryParam("id") String id, @QueryParam("q") String text,
          @QueryParam("episodes") boolean includeEpisodes, @QueryParam("series") boolean includeSeries,
          @QueryParam("limit") int limit, @QueryParam("offset") int offset, @PathParam("format") String format) {

    SearchQuery query = new SearchQuery();

    // If id is specified, do a search based on id
    if (!StringUtils.isBlank(id)) {
      query.withId(id);
    }

    // Include series data in the results?
    query.includeSeries(includeSeries);

    // Include episodes in the result?
    query.includeEpisodes(includeEpisodes);

    // Include free-text search?
    if (!StringUtils.isBlank(text)) {
      query.withText(text);
    }

    query.withPublicationDateSort(true);
    query.withLimit(limit);
    query.withOffset(offset);

    // Return the right format
    if ("json".equals(format))
      return Response.ok(searchService.getByQuery(query)).type(MediaType.APPLICATION_JSON).build();
    else
      return Response.ok(searchService.getByQuery(query)).type(MediaType.APPLICATION_XML).build();
  }

  @GET
  @Path("episode.{format:xml|json}")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "episodes", description = "Search for episodes matching the query parameters.", pathParameters = { @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(description = "The ID of the single episode to be returned, if it exists.", isRequired = false, name = "id", type = RestParameter.Type.STRING),
          @RestParameter(description = "Any episode that matches this free-text query.", isRequired = false, name = "q", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether to include this series episodes. This can be used in combination with \"id\" or \"q\".", isRequired = false, name = "episodes", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false, name = "admin", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, expressed as xml or json.")
  public Response getEpisode(@QueryParam("id") String id, @QueryParam("q") String text,
          @QueryParam("tag") String[] tags, @QueryParam("flavor") String[] flavors, @QueryParam("limit") int limit,
          @QueryParam("offset") int offset, @PathParam("format") String format) {

    // Prepare the flavors
    List<MediaPackageElementFlavor> flavorSet = new ArrayList<MediaPackageElementFlavor>();
    if (flavors != null) {
      for (String f : flavors) {
        try {
          flavorSet.add(MediaPackageElementFlavor.parseFlavor(f));
        } catch (IllegalArgumentException e) {
          logger.debug("invalid flavor '{}' specified in query", f);
        }
      }
    }

    SearchQuery search = new SearchQuery();
    search.withId(id).withElementFlavors(flavorSet.toArray(new MediaPackageElementFlavor[flavorSet.size()]))
            .withElementTags(tags).withLimit(limit).withOffset(offset);
    if (!StringUtils.isBlank(text))
      search.withText(text);
    else
      search.withPublicationDateSort(true);

    // Return the results using the requested format
    if ("json".equals(format))
      return Response.ok(searchService.getByQuery(search)).type(MediaType.APPLICATION_JSON).build();
    else
      return Response.ok(searchService.getByQuery(search)).type(MediaType.APPLICATION_XML).build();
  }

  @GET
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "episodesAndSeries", description = "Search for episodes and series matching the query parameters.", restParameters = {
          @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = false, name = "format", type = RestParameter.Type.STRING),
          @RestParameter(description = "Any episode or series that matches this free-text query.", isRequired = false, name = "q", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false, name = "admin", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Wrong output format specified.", responseCode = HttpServletResponse.SC_NOT_ACCEPTABLE) }, returnDescription = "The search results, expressed as xml or json.")
  public Response getEpisodesAndSeries(@QueryParam("q") String text, @QueryParam("limit") int limit,
          @QueryParam("offset") int offset, @QueryParam("format") String format, @QueryParam("admin") boolean admin)
          throws SearchException, UnauthorizedException {

    // format may be null or empty (not specified), or 'json' or 'xml'
    if ((format == null) || format.matches("(json|xml)?")) {
      SearchQuery query = new SearchQuery();
      query.includeEpisodes(true);
      query.includeSeries(true);
      query.withLimit(limit);
      query.withOffset(offset);
      if (!StringUtils.isBlank(text))
        query.withText(text);
      else
        query.withPublicationDateSort(true);

      // Build the response
      ResponseBuilder rb = Response.ok();

      if (admin) {
        rb.entity(searchService.getForAdministrativeRead(query)).type(MediaType.APPLICATION_JSON);
      } else {
        rb.entity(searchService.getByQuery(query)).type(MediaType.APPLICATION_JSON);
      }

      if ("json".equals(format)) {
        rb.type(MediaType.APPLICATION_JSON);
      } else {
        rb.type(MediaType.TEXT_XML);
      }

      return rb.build();
    }

    return Response.status(Response.Status.NOT_ACCEPTABLE).build();
  }

  @GET
  @Path("lucene.{format:xml|json}")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "lucene", description = "Search a lucene query.", pathParameters = { @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(defaultValue = "", description = "The lucene query.", isRequired = false, name = "q", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, expressed as xml or json")
  public Response getByLuceneQuery(@QueryParam("q") String q, @QueryParam("limit") int limit,
          @QueryParam("offset") int offset, @PathParam("format") String format) {
    SearchQuery query = new SearchQuery();
    if (!StringUtils.isBlank(q))
      query.withQuery(q);
    else
      query.withPublicationDateSort(true);
    query.withLimit(limit);
    query.withOffset(offset);

    if ("json".equals(format))
      return Response.ok(searchService.getByQuery(query)).type(MediaType.APPLICATION_JSON).build();
    else
      return Response.ok(searchService.getByQuery(query)).type(MediaType.APPLICATION_XML).build();
  }
}
