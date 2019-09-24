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

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.impl.SearchServiceImpl;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
@RestService(name = "search", title = "Search Service", abstractText = "This service indexes and queries available (distributed) episodes.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class SearchRestService extends AbstractJobProducerEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(SearchRestService.class);

  /** The constant used to switch the direction of the sorting querystring parameter. */
  public static final String DESCENDING_SUFFIX = "_DESC";

  /** The search service */
  protected SearchServiceImpl searchService;

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  private static final String SAMPLE_MEDIA_PACKAGE = "<mediapackage xmlns=\"http://mediapackage.opencastproject.org\""
          + "start=\"2007-12-05T13:40:00\" duration=\"1004400000\">\n"
          + "  <title>t1</title>\n"
          + "  <metadata>\n"
          + "    <catalog id=\"catalog-1\" type=\"dublincore/episode\">\n"
          + "      <mimetype>text/xml</mimetype>\n"
          + "      <url>https://opencast.jira.com/svn/MH/trunk/modules/kernel/src/test/resources/dublincore.xml</url>\n"
          + "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n"
          + "    </catalog>\n"
          + "    <catalog id=\"catalog-3\" type=\"metadata/mpeg-7\" ref=\"track:track-1\">\n"
          + "      <mimetype>text/xml</mimetype>\n"
          + "      <url>https://opencast.jira.com/svn/MH/trunk/modules/kernel/src/test/resources/mpeg7.xml</url>\n"
          + "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n"
          + "    </catalog>\n"
          + "  </metadata>\n"
          + "</mediapackage>";

  @POST
  @Path("add")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "add", description = "Adds a mediapackage to the search index.",
    restParameters = {
      @RestParameter(description = "The media package to add to the search index.", isRequired = true, name = "mediapackage", type = RestParameter.Type.TEXT, defaultValue = SAMPLE_MEDIA_PACKAGE)
    }, reponses = {
      @RestResponse(description = "XML encoded receipt is returned", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "There has been an internal error and the mediapackage could not be added", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "The job receipt")
  public Response add(@FormParam("mediapackage") MediaPackageImpl mediaPackage) throws SearchException {
    try {
      Job job = searchService.add(mediaPackage);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.warn("Unable to add mediapackage to search index: {}", e.getMessage());
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("{id}")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "remove", description = "Removes a mediapackage from the search index.", pathParameters = { @RestParameter(description = "The media package ID to remove from the search index.", isRequired = true, name = "id", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "The removing job.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "There has been an internal error and the mediapackage could not be deleted", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "The job receipt")
  public Response remove(@PathParam("id") String mediaPackageId) throws SearchException {
    try {
      Job job = searchService.delete(mediaPackageId);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.info("Unable to remove mediapackage {} from search index: {}", mediaPackageId, e.getMessage());
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
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: DATE_CREATED, DATE_PUBLISHED, TITLE, SERIES_ID, MEDIA_PACKAGE_ID, CREATOR, "
                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT, DESCRIPTION, PUBLISHER.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "20", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false, name = "admin", type = RestParameter.Type.BOOLEAN),
          @RestParameter(defaultValue = "true", description = "If results are to be signed", isRequired = false,
              name = "sign", type = RestParameter.Type.BOOLEAN)
    }, reponses = { @RestResponse(description = "The request was processed successfully.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, formatted as XML or JSON.")
  public Response getEpisodeAndSeriesById(
      @QueryParam("id")       String  id,
      @QueryParam("q")        String  text,
      @QueryParam("episodes") boolean includeEpisodes,
      @QueryParam("sort")     String sort,
      @QueryParam("limit")    int     limit,
      @QueryParam("offset")   int     offset,
      @QueryParam("admin")    boolean admin,
      @QueryParam("sign")     String  sign,
      @PathParam("format")    String  format
      ) throws SearchException, UnauthorizedException {

    final boolean signURLs = BooleanUtils.toBoolean(Objects.toString(sign, "true"));
    SearchQuery query = new SearchQuery().signURLs(signURLs);

    // If id is specified, do a search based on id
    if (StringUtils.isNotBlank(id))
      query.withId(id);

    // Include series data in the results?
    query.includeSeries(true);

    // Include episodes in the result?
    query.includeEpisodes(includeEpisodes);

    // Include free-text search?
    if (StringUtils.isNotBlank(text))
      query.withText(text);

    query.withSort(SearchQuery.Sort.DATE_CREATED, false);
    if (StringUtils.isNotBlank(sort)) {
      // Parse the sort field and direction
      SearchQuery.Sort sortField = null;
      if (sort.endsWith(DESCENDING_SUFFIX)) {
        String enumKey = sort.substring(0, sort.length() - DESCENDING_SUFFIX.length()).toUpperCase();
        try {
          sortField = SearchQuery.Sort.valueOf(enumKey);
          query.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = SearchQuery.Sort.valueOf(sort);
          query.withSort(sortField);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }
    query.withLimit(limit);
    query.withOffset(offset);

    // Build the response
    ResponseBuilder rb = Response.ok();

    if (admin) {
      rb.entity(searchService.getForAdministrativeRead(query));
    } else {
      rb.entity(searchService.getByQuery(query));
    }

    if ("json".equals(format)) {
      rb.type(MediaType.APPLICATION_JSON);
    } else {
      rb.type(MediaType.TEXT_XML);
    }

    return rb.build();
  }

  // CHECKSTYLE:OFF
  @GET
  @Path("episode.{format:xml|json}")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "episodes", description = "Search for episodes matching the query parameters.", pathParameters = { @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(description = "The ID of the single episode to be returned, if it exists.", isRequired = false, name = "id", type = RestParameter.Type.STRING),
          @RestParameter(description = "Any episode that matches this free-text query.", isRequired = false, name = "q", type = RestParameter.Type.STRING),
          @RestParameter(description = "Any episode that belongs to specified series id.", isRequired = false, name = "sid", type = RestParameter.Type.STRING),
          // @RestParameter(defaultValue = "false", description =
          // "Whether to include this series episodes. This can be used in combination with \"id\" or \"q\".",
          // isRequired = false, name = "episodes", type = RestParameter.Type.STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: DATE_CREATED, DATE_PUBLISHED, TITLE, SERIES_ID, MEDIA_PACKAGE_ID, CREATOR, "
                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT, DESCRIPTION, PUBLISHER.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", type = RestParameter.Type.STRING),          
          @RestParameter(defaultValue = "20", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false, name = "admin", type = RestParameter.Type.BOOLEAN),
          @RestParameter(defaultValue = "true", description = "If results are to be signed", isRequired = false,
              name = "sign", type = RestParameter.Type.BOOLEAN)
  }, reponses = { @RestResponse(description = "The request was processed successfully.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, formatted as xml or json.")
  public Response getEpisode(@QueryParam("id") String id, @QueryParam("q") String text,
          @QueryParam("sid") String seriesId, @QueryParam("sort") String sort, @QueryParam("tag") String[] tags, @QueryParam("flavor") String[] flavors,
          @QueryParam("limit") int limit, @QueryParam("offset") int offset, @QueryParam("admin") boolean admin,
          @QueryParam("sign") String sign, @PathParam("format") String format) throws SearchException, UnauthorizedException {
    // CHECKSTYLE:ON
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

    final boolean signURLs = BooleanUtils.toBoolean(Objects.toString(sign, "true"));

    SearchQuery search = new SearchQuery();
    search.withId(id)
        .withSeriesId(seriesId)
        .withElementFlavors(flavorSet.toArray(new MediaPackageElementFlavor[0]))
        .withElementTags(tags)
        .withLimit(limit)
        .withOffset(offset)
        .signURLs(signURLs);

    if (StringUtils.isNotBlank(text)) {
      search.withText(text);
    }

    search.withSort(SearchQuery.Sort.DATE_CREATED, false);
    if (StringUtils.isNotBlank(sort)) {
      // Parse the sort field and direction
      SearchQuery.Sort sortField = null;
      if (sort.endsWith(DESCENDING_SUFFIX)) {
        String enumKey = sort.substring(0, sort.length() - DESCENDING_SUFFIX.length()).toUpperCase();
        try {
          sortField = SearchQuery.Sort.valueOf(enumKey);
          search.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = SearchQuery.Sort.valueOf(sort);
          search.withSort(sortField);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }

    // Build the response
    ResponseBuilder rb = Response.ok();

    if (admin) {
      rb.entity(searchService.getForAdministrativeRead(search));
    } else {
      rb.entity(searchService.getByQuery(search));
    }

    if ("json".equals(format)) {
      rb.type(MediaType.APPLICATION_JSON);
    } else {
      rb.type(MediaType.TEXT_XML);
    }

    return rb.build();
  }

  @GET
  @Path("lucene.{format:xml|json}")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "lucene", description = "Search a lucene query.", pathParameters = { @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(defaultValue = "", description = "The lucene query.", isRequired = false, name = "q", type = RestParameter.Type.STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: DATE_CREATED, DATE_PUBLISHED, TITLE, SERIES_ID, MEDIA_PACKAGE_ID, CREATOR, "
                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT, DESCRIPTION, PUBLISHER.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "20", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false, name = "admin", type = RestParameter.Type.BOOLEAN),
          @RestParameter(defaultValue = "true", description = "If results are to be signed", isRequired = false,
              name = "sign", type = RestParameter.Type.BOOLEAN)
    }, reponses = { @RestResponse(description = "The request was processed successfully.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, formatted as xml or json")
  public Response getByLuceneQuery(@QueryParam("q") String q, @QueryParam("sort") String sort, @QueryParam("limit") int limit,
          @QueryParam("offset") int offset, @QueryParam("admin") boolean admin,
          @QueryParam("sign") String sign, @PathParam("format") String format)
          throws SearchException, UnauthorizedException {
    final boolean signURLs = BooleanUtils.toBoolean(Objects.toString(sign, "true"));
    SearchQuery query = new SearchQuery().signURLs(signURLs);
    if (!StringUtils.isBlank(q))
      query.withQuery(q);

    query.withSort(SearchQuery.Sort.DATE_CREATED, false);
    if (StringUtils.isNotBlank(sort)) {
      // Parse the sort field and direction
      SearchQuery.Sort sortField = null;
      if (sort.endsWith(DESCENDING_SUFFIX)) {
        String enumKey = sort.substring(0, sort.length() - DESCENDING_SUFFIX.length()).toUpperCase();
        try {
          sortField = SearchQuery.Sort.valueOf(enumKey);
          query.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = SearchQuery.Sort.valueOf(sort);
          query.withSort(sortField);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }
    query.withLimit(limit);
    query.withOffset(offset);

    // Build the response
    ResponseBuilder rb = Response.ok();

    if (admin) {
      rb.entity(searchService.getForAdministrativeRead(query));
    } else {
      rb.entity(searchService.getByQuery(query));
    }

    if ("json".equals(format)) {
      rb.type(MediaType.APPLICATION_JSON);
    } else {
      rb.type(MediaType.TEXT_XML);
    }

    return rb.build();
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
  public void setSearchService(SearchServiceImpl searchService) {
    this.searchService = searchService;
  }

  /**
   * Callback from OSGi to set the service registry implementation.
   *
   * @param serviceRegistry
   *          the service registry
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getServiceRegistry()
   */
  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

}
