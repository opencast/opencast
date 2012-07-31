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
package org.opencastproject.episode.endpoint;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.EpisodeServiceException;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

/**
 * The REST endpoint
 */
@Path("/")
@RestService(name = "episode", title = "Episode Service", notes = {
        "All paths are relative to the REST endpoint base (something like http://your.server/files)",
        "If you notice that this service is not working as expected, there might be a bug! "
                + "You should file an error report with your server logs from the time when the error occurred: "
                + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>" }, abstractText = "This service indexes and queries available (distributed) episodes.")
public class EpisodeRestService {

  private static final Logger logger = LoggerFactory.getLogger(EpisodeRestService.class);

  /**
   * The constant used to switch the direction of the sorting query string parameter.
   */
  public static final String DESCENDING_SUFFIX = "_DESC";

  protected EpisodeService episodeService;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */

  public void activate(ComponentContext cc) {
    // String serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  public void setEpisodeService(EpisodeService episodeService) {
    this.episodeService = episodeService;
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
  @RestQuery(name = "add", description = "Adds a mediapackage to the episode service.", restParameters = { @RestParameter(name = "mediapackage", isRequired = true, type = RestParameter.Type.TEXT, defaultValue = "${this.sampleMediaPackage}", description = "The media package to add to the search index.") }, reponses = {
          @RestResponse(description = "The mediapackage was added, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "There has been an internal error and the mediapackage could not be added", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "No content is returned.")
  public Response add(@FormParam("mediapackage") MediaPackageImpl mediaPackage) throws EpisodeServiceException {
    try {
      episodeService.add(mediaPackage);
      return Response.noContent().build();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
  }

  @DELETE
  @Path("delete/{id}")
  @RestQuery(name = "remove", description = "Remove an episode from the archive.", pathParameters = { @RestParameter(name = "id", isRequired = true, type = RestParameter.Type.STRING, description = "The media package ID to remove from the archive.") }, reponses = {
          @RestResponse(description = "The mediapackage was removed, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "There has been an internal error and the mediapackage could not be deleted", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "No content is returned.")
  public Response delete(@PathParam("id") String mediaPackageId) throws EpisodeServiceException, NotFoundException {
    try {
      if (mediaPackageId != null && episodeService.delete(mediaPackageId))
        return Response.noContent().build();
      else
        throw new NotFoundException();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
  }

  @POST
  @Path("lock/{id}")
  @RestQuery(name = "lock", description = "Flag a mediapackage as locked.", pathParameters = { @RestParameter(name = "id", isRequired = true, type = RestParameter.Type.STRING, description = "The media package to lock.") }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "The mediapackage was locked, no content to return."),
          @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "There has been an internal error and the mediapackage could not be locked") }, returnDescription = "No content is returned.")
  public Response lock(@PathParam("id") String mediaPackageId) {
    try {
      if (mediaPackageId != null && episodeService.lock(mediaPackageId, true))
        return Response.noContent().build();
      else
        throw new NotFoundException();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
  }

  @POST
  @Path("unlock/{id}")
  @RestQuery(name = "unlock", description = "Flag a mediapackage as unlocked.", pathParameters = { @RestParameter(name = "id", isRequired = true, type = RestParameter.Type.STRING, description = "The media package to unlock.") }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "The mediapackage was unlocked, no content to return."),
          @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "There has been an internal error and the mediapackage could not be locked") }, returnDescription = "No content is returned.")
  public Response unlock(@PathParam("id") String mediaPackageId) {
    try {
      if (mediaPackageId != null && episodeService.lock(mediaPackageId, false))
        return Response.noContent().build();
      else
        throw new NotFoundException();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
  }

  // todo implement version of apply that takes a workflow definition as XML.
  // @POST
  // @Path("applyworkflow")
  // @RestQuery(name = "applyworkflow", description =
  // "Apply a workflow to a list of media packages. Choose to either provide "
  // + "a workflow definition or a workflow definition identifier.",
  // restParameters = {
  // @RestParameter(description = "The workflow definition in XML format.",
  // isRequired = false, name = "definition", type = RestParameter.Type.TEXT),
  // @RestParameter(description = "The workflow definition ID.",
  // isRequired = false, name = "definitionId", type = RestParameter.Type.TEXT),
  // @RestParameter(description = "A list of media package ids.",
  // isRequired = true, name = "id", type = RestParameter.Type.STRING)
  // },
  // reponses = {
  // @RestResponse(description = "The workflows have been started.", responseCode = HttpServletResponse.SC_NO_CONTENT)
  // },
  // returnDescription = "No content is returned.")
  // public Response applyWorkflow(@FormParam("definition") String workflowDefinitionXml,
  // @FormParam("definitionId") String workflowDefinitionId,
  // @FormParam("id") List<String> mediaPackageId)
  // throws UnauthorizedException {
  // if (mediaPackageId == null || mediaPackageId.size() == 0)
  // throw new WebApplicationException(Response.Status.BAD_REQUEST);
  // boolean workflowDefinitionXmlPresent = StringUtils.isNotBlank(workflowDefinitionXml);
  // boolean workflowDefinitionIdPresent = StringUtils.isNotBlank(workflowDefinitionId);
  // if (!(workflowDefinitionXmlPresent ^ workflowDefinitionIdPresent))
  // throw new WebApplicationException(Response.Status.BAD_REQUEST);
  // if (workflowDefinitionXmlPresent) {
  // WorkflowDefinition workflowDefinition;
  // try {
  // workflowDefinition = WorkflowParser.parseWorkflowDefinition(workflowDefinitionXml);
  // } catch (WorkflowParsingException e) {
  // throw new WebApplicationException(e);
  // }
  // episodeService.applyWorkflow(workflowDefinition, mediaPackageId);
  // return Response.noContent().build();
  // } else {
  // episodeService.applyWorkflow(workflowDefinitionId, mediaPackageId);
  // return Response.noContent().build();
  // }
  // }

  @POST
  @Path("apply/{wfDefId}")
  public Response applyWorkflow(@PathParam("wfDefId") String wfId, @FormParam("mediaPackageIds") List<String> mpIds,
          @Context HttpServletRequest req) throws UnauthorizedException {
    Map<String, String> wfConf = mlist(((Map<String, String[]>) req.getParameterMap()).entrySet().iterator()).foldl(
            new HashMap<String, String>(),
            new Function2<Map<String, String>, Map.Entry<String, String[]>, Map<String, String>>() {
              @Override
              public Map<String, String> apply(Map<String, String> wfConf, Map.Entry<String, String[]> param) {
                String key = param.getKey();
                if (!"mediaPackageIds".equalsIgnoreCase(key))
                  wfConf.put(key, param.getValue()[0]);
                return wfConf;
              }
            });
    episodeService.applyWorkflow(wfId, mpIds, wfConf);
    return Response.noContent().build();
  }

  // @GET
  // @Path("series.{format:xml|json}")
  // @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  // @RestQuery(name = "series", description = "Search for series matching the query parameters.",
  // pathParameters = {
  // @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name =
  // "format", type = RestParameter.Type.STRING)
  // },
  // restParameters = {
  // @RestParameter(description = "The series ID. If the additional boolean parameter \"episodes\" is \"true\", "
  // + "the result set will include this series episodes.", isRequired = false, name = "id", type =
  // RestParameter.Type.STRING),
  // @RestParameter(description =
  // "Any series that matches this free-text query. If the additional boolean parameter \"episodes\" is \"true\", "
  // + "the result set will include this series episodes.", isRequired = false, name = "q", type =
  // RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "false", description =
  // "Whether to include this series episodes. This can be used in combination with \"id\" or \"q\".", isRequired =
  // false, name = "episodes", type = RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "false", description =
  // "Whether to include this series information itself. This can be used in combination with \"id\" or \"q\".",
  // isRequired = false, name = "series", type = RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired =
  // false, name = "limit", type = RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type =
  // RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false,
  // name = "admin", type = RestParameter.Type.STRING)
  // },
  // reponses = {
  // @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK)
  // },
  // returnDescription = "The search results, expressed as xml or json.")
  // public Response getEpisodeAndSeriesById(@QueryParam("id") String id, @QueryParam("q") String text,
  // @QueryParam("episodes") boolean includeEpisodes, @QueryParam("series") boolean includeSeries,
  // @QueryParam("limit") int limit, @QueryParam("offset") int offset, @PathParam("format") String format) {
  //
  // EpisodeQuery query = new EpisodeQuery();
  //
  // // If id is specified, do a search based on id
  // if (!StringUtils.isBlank(id)) {
  // query.withId(id);
  // }
  //
  // // Include series data in the results?
  // query.includeSeries(includeSeries);
  //
  // // Include episodes in the result?
  // query.includeEpisodes(includeEpisodes);
  //
  // // Include free-text search?
  // if (!StringUtils.isBlank(text)) {
  // query.withText(text);
  // }
  //
  // query.withPublicationDateSort(true);
  // query.withLimit(limit);
  // query.withOffset(offset);
  //
  // // Return the right format
  // if ("json".equals(format))
  // return Response.ok(episodeService.getByQuery(query)).type(MediaType.APPLICATION_JSON).build();
  // else
  // return Response.ok(episodeService.getByQuery(query)).type(MediaType.APPLICATION_XML).build();
  // }

  @GET
  @Path("episode.{format:xml|json}")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "episodes", description = "Search for episodes matching the query parameters.", pathParameters = { @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(description = "The ID of the single episode to be returned, if it exists.", isRequired = false, name = "id", type = RestParameter.Type.STRING),
          @RestParameter(name = "q", description = "Any episode that matches this free-text query.", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "creator", isRequired = false, description = "Filter results by the mediapackage's creator", type = STRING),
          @RestParameter(name = "contributor", isRequired = false, description = "Filter results by the mediapackage's contributor", type = STRING),
          @RestParameter(name = "language", isRequired = false, description = "Filter results by mediapackage's language.", type = STRING),
          @RestParameter(name = "series", isRequired = false, description = "Filter results by mediapackage's series identifier.", type = STRING),
          @RestParameter(name = "license", isRequired = false, description = "Filter results by mediapackage's license.", type = STRING),
          @RestParameter(name = "title", isRequired = false, description = "Filter results by mediapackage's title.", type = STRING),
          @RestParameter(defaultValue = "false", description = "Whether to include this series episodes. This can be used in combination with \"id\" or \"q\".", isRequired = false, name = "episodes", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false, name = "admin", type = RestParameter.Type.STRING),
          @RestParameter(name = "sort", description = "The sort order.  May include any "
                  + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", isRequired = false, type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, expressed as xml or json.")
  // CHECKSTYLE:OFF -- more than 7 parameters
  public Response getEpisode(@QueryParam("id") String id, @QueryParam("q") String text,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("language") String language, @QueryParam("series") String series,
          @QueryParam("license") String license, @QueryParam("title") String title, @QueryParam("tag") String[] tags,
          @QueryParam("flavor") String[] flavors, @QueryParam("limit") int limit, @QueryParam("offset") int offset,
          @QueryParam("sort") String sort, @PathParam("format") String format) throws Exception {
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

    final EpisodeQuery search = new EpisodeQuery().withId(id)
            .withElementFlavors(flavorSet.toArray(new MediaPackageElementFlavor[flavorSet.size()]))
            .withElementTags(tags).withLimit(limit).withOffset(offset).withText(StringUtils.trimToNull(text))
            .withCreator(creator).withContributor(contributor).withLanguage(language).withSeriesId(series)
            .withLicense(license).withTitle(title);

    if (StringUtils.isNotBlank(sort)) {
      // Parse the sort field and direction
      EpisodeQuery.Sort sortField = null;
      if (sort.endsWith(DESCENDING_SUFFIX)) {
        String enumKey = sort.substring(0, sort.length() - DESCENDING_SUFFIX.length()).toUpperCase();
        try {
          sortField = EpisodeQuery.Sort.valueOf(enumKey);
          search.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = EpisodeQuery.Sort.valueOf(sort);
          search.withSort(sortField, true);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }

    // Return the results using the requested format
    final String type = "json".equals(format) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML;
    return Response.ok(episodeService.getByQuery(search)).type(type).build();
  }

  // @GET
  // @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  // @RestQuery(name = "episodesAndSeries", description =
  // "Search for episodes and series matching the query parameters.",
  // restParameters = {
  // @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = false, name =
  // "format", type = RestParameter.Type.STRING),
  // @RestParameter(description = "Any episode or series that matches this free-text query.", isRequired = false, name =
  // "q", type = RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired =
  // false, name = "limit", type = RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type =
  // RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "false", description = "Whether this is an administrative query", isRequired = false,
  // name = "admin", type = RestParameter.Type.STRING)
  // },
  // reponses = {
  // @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK),
  // @RestResponse(description = "Wrong output format specified.", responseCode = HttpServletResponse.SC_NOT_ACCEPTABLE)
  // },
  // returnDescription = "The search results, expressed as xml or json.")
  // public Response getEpisodesAndSeries(@QueryParam("q") String text, @QueryParam("limit") int limit,
  // @QueryParam("offset") int offset, @QueryParam("format") String format, @QueryParam("admin") boolean admin)
  // throws EpisodeServiceException, UnauthorizedException {
  //
  // // format may be null or empty (not specified), or 'json' or 'xml'
  // if ((format == null) || format.matches("(json|xml)?")) {
  // EpisodeQuery query = new EpisodeQuery();
  // query.includeEpisodes(true);
  // query.includeSeries(true);
  // query.withLimit(limit);
  // query.withOffset(offset);
  // if (!StringUtils.isBlank(text))
  // query.withText(text);
  // else
  // query.withPublicationDateSort(true);
  //
  // // Build the response
  // ResponseBuilder rb = Response.ok();
  //
  // if (admin) {
  // rb.entity(episodeService.getForAdministrativeRead(query)).type(MediaType.APPLICATION_JSON);
  // } else {
  // rb.entity(episodeService.getByQuery(query)).type(MediaType.APPLICATION_JSON);
  // }
  //
  // if ("json".equals(format)) {
  // rb.type(MediaType.APPLICATION_JSON);
  // } else {
  // rb.type(MediaType.TEXT_XML);
  // }
  //
  // return rb.build();
  // }
  //
  // return Response.status(Response.Status.NOT_ACCEPTABLE).build();
  // }

  // @GET
  // @Path("lucene.{format:xml|json}")
  // @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  // @RestQuery(name = "lucene", description = "Search a lucene query.",
  // pathParameters = {
  // @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name =
  // "format", type = RestParameter.Type.STRING)
  // },
  // restParameters = {
  // @RestParameter(defaultValue = "", description = "The lucene query.", isRequired = false, name = "q", type =
  // RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired =
  // false, name = "limit", type = RestParameter.Type.STRING),
  // @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type =
  // RestParameter.Type.STRING)
  // },
  // reponses = {
  // @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK)
  // },
  // returnDescription = "The search results, expressed as xml or json")
  // public Response getByLuceneQuery(@QueryParam("q") String q, @QueryParam("limit") int limit,
  // @QueryParam("offset") int offset, @PathParam("format") String format) {
  // EpisodeQuery query = new EpisodeQuery();
  // if (!StringUtils.isBlank(q))
  // query.withQuery(q);
  // else
  // query.withPublicationDateSort(true);
  // query.withLimit(limit);
  // query.withOffset(offset);
  //
  // if ("json".equals(format))
  // return Response.ok(episodeService.getByQuery(query)).type(MediaType.APPLICATION_JSON).build();
  // else
  // return Response.ok(episodeService.getByQuery(query)).type(MediaType.APPLICATION_XML).build();
  // }
}
