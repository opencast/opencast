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

package org.opencastproject.search.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.opencastproject.elasticsearch.impl.IndexSchema.SEARCH_FIELD_NAME_EXTENSION;
import static org.opencastproject.util.RestUtil.R.forbidden;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultList;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.SearchServiceImpl;
import org.opencastproject.search.impl.SearchServiceIndex;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint
 */
@Path("/search")
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
@JaxrsResource
public class SearchRestService extends AbstractJobProducerEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(SearchRestService.class);

  /** The search service which talks to the database.  Only needed for the JobProducer bits. */
  protected SearchServiceImpl searchService;

  /** The connector to the actual index */
  protected SearchServiceIndex searchIndex;

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  private SecurityService securityService;

  private final Gson gson = new Gson();

  private UrlSigningService urlSigningService;

  @GET
  @Path("series.json")
  @Produces(MediaType.APPLICATION_JSON)
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
              + "identifier, title, contributor, creator, created, modified. "
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

    final var org = securityService.getOrganization().getId();
    final var type = SearchService.IndexEntryType.Series.name();
    final var query = QueryBuilders.boolQuery()
        .filter(QueryBuilders.termQuery(SearchResult.ORG, org))
        .filter(QueryBuilders.termQuery(SearchResult.TYPE, type))
        .filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(SearchResult.DELETED_DATE)));
    final var user = securityService.getUser();
    final var orgAdminRole = securityService.getOrganization().getAdminRole();
    if (!user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE) && !user.hasRole(orgAdminRole)) {
      query.filter(QueryBuilders.termsQuery(
          SearchResult.INDEX_ACL + ".read",
          user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
      ));
    }

    if (StringUtils.isNotEmpty(id)) {
      query.must(QueryBuilders.idsQuery().addIds(id));
    }

    if (StringUtils.isNotEmpty(text)) {
      MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(text);
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION,  1.2f);
      // Search-as-you-type generates useful subfields for autocompletion, we should use it
      // https://docs.opensearch.org/1.3/field-types/supported-field-types/search-as-you-type/
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION + "._2gram", 1.2f);
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION + "._3gram", 1.2f);
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION + "._4gram", 1.2f);
      queryBuilder.type(MultiMatchQueryBuilder.Type.BOOL_PREFIX);
      queryBuilder.operator(Operator.AND);
      queryBuilder.fuzziness(Fuzziness.AUTO);
      queryBuilder.fuzzyTranspositions(true);
      query.minimumShouldMatch(1);
      query.should(queryBuilder);
    }

    var size = NumberUtils.toInt(limit, 20);
    var from = NumberUtils.toInt(offset);
    if (size < 0 || from < 0) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Limit and offset may not be negative.")
          .build();
    }
    var searchSource = new SearchSourceBuilder()
        .query(query)
        .from(from)
        .size(size);

    if (StringUtils.isNotEmpty(sort)) {
      var sortParam = StringUtils.split(sort.toLowerCase());
      var validSort = Arrays.asList("identifier", "title", "contributor", "creator", "created", "modified")
              .contains(sortParam[0]);
      var validOrder = sortParam.length < 2 || Arrays.asList("asc", "desc").contains(sortParam[1]);
      if (sortParam.length > 2 || !validSort || !validOrder) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Invalid sort parameter")
            .build();
      }
      var order = SortOrder.fromString(sortParam.length > 1 ? sortParam[1] : "asc");
      if ("modified".equals(sortParam[0])) {
        searchSource.sort(sortParam[0], order);
      } else {
        searchSource.sort(SearchResult.DUBLINCORE + "." + sortParam[0], order);
      }
    }

    searchSource.sort("_score", SortOrder.DESC);
    // Used to stabilize the sort order of results with the same score.
    searchSource.sort("_doc", SortOrder.ASC);

    var hits = searchIndex.search(searchSource).getHits();
    var result = Arrays.stream(hits.getHits())
        .map(SearchHit::getSourceAsMap)
        .peek(hit -> hit.remove(SearchResult.TYPE))
        .collect(Collectors.toList());

    var total = hits.getTotalHits().value;
    var json = gson.toJsonTree(Map.of(
        "offset", from,
        "total", total,
        "result", result,
        "limit", size));
    return Response.ok(gson.toJson(json)).build();

  }

  @GET
  @Path("episode.json")
  @Produces(MediaType.APPLICATION_JSON)
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
                  + "title, contributor, creator, created, modified. "
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
              isRequired = false,
              type = RestParameter.Type.BOOLEAN,
              defaultValue = "true",
              description = "If results are to be signed"
          ),
          @RestParameter(
              name = "live",
              isRequired = false,
              type = RestParameter.Type.BOOLEAN,
              description = "If the result should only consist of live episodes (true) or not live episodes (false)"
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
      @QueryParam("sname") String seriesName,
      @QueryParam("sort") String sort,
      @QueryParam("limit") String limit,
      @QueryParam("offset") String offset,
      @QueryParam("sign") String sign,
      @QueryParam("live") Boolean live
  ) throws SearchException {

    // There can only be one, sid or sname
    if (StringUtils.isNoneEmpty(seriesName, seriesId)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("invalid request, both 'sid' and 'sname' specified")
          .build();
    }
    final var org = securityService.getOrganization().getId();
    final var type = SearchService.IndexEntryType.Episode.name();

    boolean snameNotFound = false;
    List<String> series = Collections.emptyList();
    if (StringUtils.isNotEmpty(seriesName)) {
      var seriesSearchSource = new SearchSourceBuilder().query(QueryBuilders.boolQuery()
          .filter(QueryBuilders.termQuery(SearchResult.ORG, org))
          .filter(QueryBuilders.termQuery(SearchResult.TYPE, SearchService.IndexEntryType.Series))
          .filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(SearchResult.DELETED_DATE)))
          .must(QueryBuilders.termQuery(SearchResult.DUBLINCORE + ".title", seriesName)));
      series = searchService.search(seriesSearchSource).getHits().stream()
          .map(h -> h.getDublinCore().getFirst(DublinCore.PROPERTY_IDENTIFIER))
          .collect(Collectors.toList());
      //If there is no series matching the sname provided
      if (series.isEmpty()) {
        snameNotFound = true;
      }
    }

    var user = securityService.getUser();
    var orgAdminRole = securityService.getOrganization().getAdminRole();
    var admin = user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE) || user.hasRole(orgAdminRole);
    var query = QueryBuilders.boolQuery()
        .filter(QueryBuilders.termQuery(SearchResult.ORG, org))
        .filter(QueryBuilders.termQuery(SearchResult.TYPE, type))
        .filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(SearchResult.DELETED_DATE)));
    if (!admin) {
      query.filter(QueryBuilders.termsQuery(
          SearchResult.INDEX_ACL + ".read",
          user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
      ));
    }

    if (StringUtils.isNotEmpty(id)) {
      query.must(QueryBuilders.idsQuery().addIds(id));
    }

    if (StringUtils.isNotEmpty(seriesId)) {
      series = Collections.singletonList(seriesId);
    }
    if (!series.isEmpty()) {
      if (series.size() == 1) {
        query.must(QueryBuilders.termQuery(SearchResult.DUBLINCORE + ".isPartOf", series.get(0)));
      } else {
        var seriesQuery = QueryBuilders.boolQuery();
        for (var sid : series) {
          seriesQuery.should(QueryBuilders.termQuery(SearchResult.DUBLINCORE + ".isPartOf", sid));
        }
        query.must(seriesQuery);
      }
    }

    if (live != null) {
      query.must(QueryBuilders.termQuery("live", live));
    }

    if (StringUtils.isNotEmpty(text)) {
      MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(text);
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION,  1.2f);
      // Search-as-you-type generates useful subfields for autocompletion, we should use it
      // https://docs.opensearch.org/1.3/field-types/supported-field-types/search-as-you-type/
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION + "._2gram", 1.2f);
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION + "._3gram", 1.2f);
      queryBuilder.field(SearchResult.DUBLINCORE + ".*" + SEARCH_FIELD_NAME_EXTENSION + "._4gram", 1.2f);
      queryBuilder.type(MultiMatchQueryBuilder.Type.BOOL_PREFIX);
      queryBuilder.operator(Operator.AND);
      queryBuilder.fuzziness(Fuzziness.AUTO);
      queryBuilder.fuzzyTranspositions(true);
      query.minimumShouldMatch(1);
      query.should(queryBuilder);
    }

    logger.debug("limit: {}, offset: {}", limit, offset);

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

    var searchSource = new SearchSourceBuilder()
        .query(query)
        .from(from)
        .size(size);

    if (StringUtils.isNotEmpty(sort)) {
      var sortParam = StringUtils.split(sort.toLowerCase());
      var validSort = Arrays.asList("title", "contributor", "creator", "created", "modified").contains(sortParam[0]);
      var validOrder = sortParam.length < 2 || Arrays.asList("asc", "desc").contains(sortParam[1]);
      if (sortParam.length > 2 || !validSort || !validOrder) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Invalid sort parameter")
            .build();
      }
      var order = SortOrder.fromString(sortParam.length > 1 ? sortParam[1] : "asc");
      if ("modified".equals(sortParam[0])) {
        searchSource.sort(sortParam[0], order);
      } else {
        searchSource.sort(SearchResult.DUBLINCORE + "." + sortParam[0], order);
      }
    }

    searchSource.sort("_score", SortOrder.DESC);
    // Used to stabilize the sort order of results with the same score.
    searchSource.sort("_doc", SortOrder.ASC);

    List<Map<String, Object>> result = null;
    long total = 0;
    if (snameNotFound) {
      result = Collections.emptyList();
    } else {
      SearchResultList hits = searchService.search(searchSource);
      result = hits.getHits().stream()
          .map(SearchResult::dehydrateForREST)
          .collect(Collectors.toList());

      // Sign urls if sign-parameter is not false
      if (!"false".equals(sign) && this.urlSigningService != null) {
        this.findURLsAndSign(result);
      }

      total = hits.getTotalHits();
    }
    var json = gson.toJson(Map.of(
        "offset", from,
        "total", total,
        "result", result,
        "limit", size));

    return Response.ok(json).build();
  }

  @POST
  @Path("updateIndex")
  @RestQuery(name = "updateIndex",
          description = "Trigger search index update for event. The usage of this is limited to global administrators.",
          restParameters = {
                  @RestParameter(
                          name = "id",
                          isRequired = true,
                          type = STRING,
                          description = "The event ID to trigger an index update for.")},
          responses = {
                  @RestResponse(
                          description = "Update successfully triggered.",
                          responseCode = SC_NO_CONTENT),
                  @RestResponse(
                          description = "Not allowed to trigger update.",
                          responseCode = SC_FORBIDDEN),
                  @RestResponse(
                          description = "No such event found.",
                          responseCode = SC_NOT_FOUND)},
          returnDescription = "No content is returned.")
  public Response indexUpdate(@FormParam("id") final String id) {
    try {
      searchIndex.indexMediaPackage(id);
      return noContent();
    } catch (UnauthorizedException e) {
      return forbidden();
    } catch (NotFoundException e) {
      return notFound();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Iterate recursively through Object List and sign all Strings with key=url
   * @param obj
   */
  private void findURLsAndSign(Object obj) {
    if (obj instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) obj;
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        if (entry.getKey().equals("url") && entry.getValue() instanceof String) {
          String urlToSign = (String) entry.getValue();
          if (this.urlSigningService.accepts(urlToSign)) {
            try {
              String signedUrl = this.urlSigningService.sign(
                  urlToSign,
                  UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION,
                  null,
                  null);
              map.put(entry.getKey(), signedUrl);
            } catch (UrlSigningException e) {
              logger.debug("Unable to sign url '{}'.", urlToSign);
            }
          }
        } else {
          findURLsAndSign(entry.getValue());
        }
      }
    } else if (obj instanceof List) {
      for (Object item : (List<?>) obj) {
        findURLsAndSign(item);
      }
    }
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
  public void setSearchIndex(SearchServiceIndex searchIndex) {
    this.searchIndex = searchIndex;
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

  @Reference
  void setUrlSigningService(UrlSigningService service) {
    this.urlSigningService = service;
  }

}
