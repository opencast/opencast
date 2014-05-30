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
import org.opencastproject.episode.api.ArchivedMediaPackageElement;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.EpisodeServiceException;
import org.opencastproject.episode.api.HttpMediaPackageElementProvider;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.api.UriRewriter;
import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.Convert;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.episode.api.EpisodeQuery.query;
import static org.opencastproject.util.MimeTypeUtil.suffix;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.workflow.api.ConfiguredWorkflow.workflow;

/** REST endpoint of the episode service. */
// no @Path annotation here since this class cannot be created by JAX-RS. Put it on implementations.
@RestService(name = "episode", title = "Episode Service", notes = {
        "All paths are relative to the REST endpoint base (something like http://your.server/files)",
        "If you notice that this service is not working as expected, there might be a bug! "
                + "You should file an error report with your server logs from the time when the error occurred: "
                + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>" }, abstractText = "This service indexes and queries available (distributed) episodes.")
public abstract class AbstractEpisodeServiceRestEndpoint implements HttpMediaPackageElementProvider {

  private static final Logger logger = LoggerFactory.getLogger(AbstractEpisodeServiceRestEndpoint.class);

  /** Path prefix for archive mediapackage elements */
  public static final String ARCHIVE_PATH_PREFIX = "/archive/mediapackage/";

  /** The constant used to switch the direction of the sorting query string parameter. */
  public static final String DESCENDING_SUFFIX = "_DESC";

  public abstract EpisodeService getEpisodeService();

  public abstract WorkflowService getWorkflowService();

  public abstract SecurityService getSecurityService();

  public abstract String getServerUrl();

  public abstract String getMountPoint();

  public String getSampleMediaPackage() {
    return "<mediapackage xmlns=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\"><title>t1</title>\n"
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
            + "  </metadata>\n" + "</mediapackage>";
  }

  @POST
  @Path("add")
  @RestQuery(name = "add", description = "Adds a mediapackage to the episode service.",
             restParameters = { @RestParameter(name = "mediapackage", isRequired = true,
                                               type = RestParameter.Type.TEXT, defaultValue = "${this.sampleMediaPackage}", description = "The media package to add to the search index.") },
             reponses = { @RestResponse(description = "The mediapackage was added, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "There has been an internal error and the mediapackage could not be added", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
             returnDescription = "No content is returned.")
  public Response add(@FormParam("mediapackage") final MediaPackageImpl mediaPackage) {
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        getEpisodeService().add(mediaPackage);
        return noContent();
      }
    });
  }

  @DELETE
  @Path("delete/{id}")
  @RestQuery(name = "remove",
             description = "Remove an episode from the archive.",
             pathParameters = { @RestParameter(name = "id", isRequired = true,
                                               type = RestParameter.Type.STRING, description = "The media package ID to remove from the archive.") },
             reponses = { @RestResponse(description = "The mediapackage was removed, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "There has been an internal error and the mediapackage could not be deleted", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
             returnDescription = "No content is returned.")
  public Response delete(@PathParam("id") final String mediaPackageId) {
    return handleException(new Function0.X<Response>() {
      @Override public Response xapply() throws NotFoundException {
        if (mediaPackageId != null && getEpisodeService().delete(mediaPackageId))
          return noContent();
        else
          return notFound();
      }
    });
  }

//  @POST
//  @Path("applyworkflow")
//  @RestQuery(name = "applyworkflow",
//             description = "Apply a workflow to a list of media packages. Choose to either provide "
//                         + "a workflow definition or a workflow definition identifier.",
//             restParameters = { @RestParameter(name = "definition", type = RestParameter.Type.TEXT,
//                                               description = "The workflow definition in XML format.", isRequired = false),
//                                @RestParameter(name = "definitionId", type = RestParameter.Type.TEXT,
//                                               description = "The workflow definition ID.", isRequired = false),
//                                @RestParameter(name = "id", type = RestParameter.Type.STRING,
//                                               description = "A list of media package ids.", isRequired = true) },
//             reponses = { @RestResponse(description = "The workflows have been started.", responseCode = HttpServletResponse.SC_NO_CONTENT) },
//             returnDescription = "No content is returned.")
//  public Response applyWorkflow(@FormParam("definition") String workflowDefinitionXml,
//                                @FormParam("definitionId") String workflowDefinitionId,
//                                @FormParam("id") List<String> mediaPackageId) throws Exception {
//    if (mediaPackageId == null || mediaPackageId.size() == 0)
//      throw new WebApplicationException(Response.Status.BAD_REQUEST);
//    boolean workflowDefinitionXmlPresent = StringUtils.isNotBlank(workflowDefinitionXml);
//    boolean workflowDefinitionIdPresent = StringUtils.isNotBlank(workflowDefinitionId);
//    if (!(workflowDefinitionXmlPresent ^ workflowDefinitionIdPresent))
//      throw new WebApplicationException(Response.Status.BAD_REQUEST);
//    final WorkflowDefinition wd = workflowDefinitionXmlPresent
//            ? WorkflowParser.parseWorkflowDefinition(workflowDefinitionXml)
//            : getWorkflowService().getWorkflowDefinitionById(workflowDefinitionId);
//    getEpisodeService().applyWorkflow(workflow(wd), uriCreator, mediaPackageId);
//    return Response.noContent().build();
//  }

  @POST
  @Path("apply/{wfDefId}")
  @RestQuery(name = "apply",
             description = "Apply a workflow to a list of media packages.",
             pathParameters = {@RestParameter(name = "wfDefId", type = RestParameter.Type.STRING,
                                              description = "The ID of the workflow to apply", isRequired = true) },
             restParameters = {@RestParameter(name = "mediaPackageIds", type = RestParameter.Type.STRING,
                                              description = "A list of media package ids.", isRequired = true) },
             reponses = {@RestResponse(description = "The workflows have been started.", responseCode = HttpServletResponse.SC_NO_CONTENT) },
             returnDescription = "No content is returned.")
  public Response applyWorkflow(@PathParam("wfDefId") final String wfId,
                                @FormParam("mediaPackageIds") final List<String> mpIds,
                                @Context final HttpServletRequest req) {
    return handleException(new Function0.X<Response>() {
      @Override public Response xapply() throws Exception {
        final Map<String, String[]> params = (Map<String, String[]>) req.getParameterMap();
        // filter and reduce String[] to String
        final Map<String, String> wfp = mlist(params.entrySet().iterator()).foldl(
                Collections.<String, String>map(),
                new Function2<Map<String, String>, Map.Entry<String, String[]>, Map<String, String>>() {
                  @Override
                  public Map<String, String> apply(Map<String, String> wfConf, Map.Entry<String, String[]> param) {
                    final String key = param.getKey();
                    if (!"mediaPackageIds".equalsIgnoreCase(key))
                      wfConf.put(key, param.getValue()[0]);
                    return wfConf;
                  }
                });
        final WorkflowDefinition wfd = getWorkflowService().getWorkflowDefinitionById(wfId);
        getEpisodeService().applyWorkflow(workflow(wfd, wfp), uriRewriter, mpIds);
        return Response.noContent().build();
      }
    });
  }

  @GET
  @Path("episode.{format:xml|json}")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  @RestQuery(name = "episodes",
             description = "Search for episodes matching the query parameters.",
             pathParameters = { @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) },
             restParameters = { @RestParameter(name = "id", type = RestParameter.Type.STRING, description = "The ID of the single episode to be returned, if it exists.", isRequired = false),
                                @RestParameter(name = "q", description = "Any episode that matches this free-text query.", isRequired = false, type = RestParameter.Type.STRING),
                                @RestParameter(name = "creator", isRequired = false, description = "Filter results by the mediapackage's creator", type = STRING),
                                @RestParameter(name = "contributor", isRequired = false, description = "Filter results by the mediapackage's contributor", type = STRING),
                                @RestParameter(name = "language", isRequired = false, description = "Filter results by mediapackage's language.", type = STRING),
                                @RestParameter(name = "series", isRequired = false, description = "Filter results by mediapackage's series identifier.", type = STRING),
                                @RestParameter(name = "license", isRequired = false, description = "Filter results by mediapackage's license.", type = STRING),
                                @RestParameter(name = "title", isRequired = false, description = "Filter results by mediapackage's title.", type = STRING),
                                @RestParameter(name = "episodes", type = RestParameter.Type.STRING, defaultValue = "false", description = "Whether to include this series episodes. This can be used in combination with \"id\" or \"q\".", isRequired = false),
                                @RestParameter(name = "limit", type = RestParameter.Type.STRING, defaultValue = "10", description = "The maximum number of items to return per page. Values less than 0 set no limit. Non-integer values cause \"not found\".", isRequired = false),
                                @RestParameter(name = "offset", type = RestParameter.Type.STRING, defaultValue = "0", description = "The page number.", isRequired = false),
                                @RestParameter(name = "admin", type = RestParameter.Type.STRING, defaultValue = "false", description = "Whether this is an administrative query", isRequired = false),
                                @RestParameter(name = "sort", type = RestParameter.Type.STRING, description = "The sort order.  May include any "
                                  + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
                                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", isRequired = false),
                                @RestParameter(name = "onlyLatest", type = Type.BOOLEAN, defaultValue = "false", description = "Filter results by only latest version of the archive", isRequired = false) },
             reponses = { @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The request did not find a result.", responseCode = HttpServletResponse.SC_NOT_FOUND) },
             returnDescription = "The search results, expressed as xml or json.")
  // CHECKSTYLE:OFF -- more than 7 parameters
  public Response findEpisode(@QueryParam("id") final String id,
                              @QueryParam("q") final String text,
                              @QueryParam("creator") final String creator,
                              @QueryParam("contributor") final String contributor,
                              @QueryParam("language") final String language,
                              @QueryParam("series") final String series,
                              @QueryParam("license") final String license,
                              @QueryParam("title") final String title,
                              @QueryParam("tag") final String[] tags,
                              @QueryParam("flavor") final String[] flavors,
                              @QueryParam("limit") final Integer limit,
                              @QueryParam("offset") final Integer offset,
                              @QueryParam("sort") final String sort,
                              @QueryParam("onlyLatest") @DefaultValue("true") final boolean onlyLatest,
                              @PathParam("format") final String format) {
    // CHECKSTYLE:ON
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        // Prepare the flavors
        final EpisodeQuery search = query(getSecurityService());
        if (flavors != null) {
          List<MediaPackageElementFlavor> fs = mlist(flavors).map(new Function<String, MediaPackageElementFlavor>() {
            @Override public MediaPackageElementFlavor apply(String f) {
              return MediaPackageElementFlavor.parseFlavor(f.trim());
            }
          }).value();
          search.elementFlavors(fs);
        }

        if ((limit != null) && (limit > 0))
          search.limit(limit);

        if (offset != null)
          search.offset(offset);

        if (tags != null)
          search.elementTags(mlist(tags).bind(Strings.trimToNil).value());

        if (StringUtils.isNotBlank(id))
          search.id(id.trim());

        if (StringUtils.isNotBlank(text))
          search.text(text.trim());

        if (StringUtils.isNotBlank(creator))
          search.creator(creator.trim());

        if (StringUtils.isNotBlank(contributor))
          search.contributor(contributor.trim());

        if (StringUtils.isNotBlank(language))
          search.language(language.trim());

        if (StringUtils.isNotBlank(series))
          search.seriesId(series.trim());

        if (StringUtils.isNotBlank(license))
          search.license(license.trim());

        if (StringUtils.isNotBlank(title))
          search.title(title.trim());

        if (StringUtils.isNotBlank(sort)) {
          // Parse the sort field and direction
          EpisodeQuery.Sort sortField = null;
          if (sort.endsWith(DESCENDING_SUFFIX)) {
            String enumKey = sort.substring(0, sort.length() - DESCENDING_SUFFIX.length()).toUpperCase();
            try {
              sortField = EpisodeQuery.Sort.valueOf(enumKey);
              search.sort(sortField, false);
            } catch (IllegalArgumentException e) {
              logger.warn("No sort enum matches '{}'", enumKey);
            }
          } else {
            try {
              sortField = EpisodeQuery.Sort.valueOf(sort);
              search.sort(sortField, true);
            } catch (IllegalArgumentException e) {
              logger.warn("No sort enum matches '{}'", sort);
            }
          }
        }

        if (onlyLatest)
          search.onlyLastVersion();

        // Return the results using the requested format
        final String type = "json".equals(format) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML;
        final SearchResult sr = getEpisodeService().find(search, uriRewriter);
        return Response.ok(Convert.convert(sr)).type(type).build();
      }
    });
  }

  @GET
  @Path(ARCHIVE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{version}/{ignore}")
  @RestQuery(name = "getElement",
             description = "Gets the file from the archive under /mediaPackageID/mediaPackageElementID/version",
             returnDescription = "The file",
             pathParameters = { @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
                                @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING),
                                @RestParameter(name = "version", description = "the mediapackage version", isRequired = true, type = STRING),
                                @RestParameter(name = "ignore", description = "this value is being ignored. just for documentation purposes", isRequired = false, type = STRING) },
             reponses = { @RestResponse(responseCode = SC_OK, description = "File returned"),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response getElement(@PathParam("mediaPackageID") final String mediaPackageID,
                             @PathParam("mediaPackageElementID") final String mediaPackageElementID,
                             @PathParam("version") final long version,
                             @HeaderParam("If-None-Match") final String ifNoneMatch) {
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        if (StringUtils.isNotBlank(ifNoneMatch))
          return Response.notModified().build();
        for (ArchivedMediaPackageElement element : getEpisodeService().get(mediaPackageID, mediaPackageElementID, Version.version(version))) {
          final InputStream inputStream = element.getInputStream();
          final MimeType mimeType = element.getMimeType();
          final String fileName = mediaPackageElementID.concat(".").concat(mimeType.getSuffix().getOrElse(mimeType.getSubtype()));
          // Write the file contents back
          return RestUtil.streamResponse(inputStream,
                                         mimeType.toString(),
                                         element.getSize() > 0 ? some(element.getSize()) : Option.<Long>none(),
                                         some(fileName)).build();
        }
        // none
        return notFound();
      }
    });
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path(ARCHIVE_PATH_PREFIX + "{mediaPackageID}")
  @RestQuery(name = "getMediaPackage",
             description = "Gets the last version of the mediapackge from the archive under /mediaPackageID",
             returnDescription = "The mediapackage",
             pathParameters = { @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING) },
             reponses = { @RestResponse(responseCode = SC_OK, description = "Mediapackage returned"),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response getMediapackage(@PathParam("mediaPackageID") final String mediaPackageId) {
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        final EpisodeQuery idQuery = query(getSecurityService()).id(mediaPackageId).onlyLastVersion();
        final List<SearchResultItem> result = getEpisodeService().find(idQuery, uriRewriter).getItems();
        if (result.size() > 1)
          return serverError();
        if (result.size() == 0)
          return notFound();
        final SearchResultItem item = result.get(0);
        return Response.ok(item.getMediaPackage()).build();
      }
    });
  }

  @Override public UriRewriter getUriRewriter() {
    return uriRewriter;
  }

  /**
   * Function to rewrite media package element URIs so that they point to this REST endpoint.
   * The created URIs have to correspond with the parameter list of {@link #getElement(String, String, long, String)}.
   */
  private final UriRewriter uriRewriter = new UriRewriter() {
    @Override public URI apply(Version version, MediaPackageElement mpe) {
      final String mimeType = option(mpe.getMimeType()).bind(suffix).getOrElse("unknown");
      return uri(getServerUrl(),
                 getMountPoint(),
                 ARCHIVE_PATH_PREFIX,
                 mpe.getMediaPackage().getIdentifier(),
                 mpe.getIdentifier(),
                 version,
                 mpe.getElementType().toString().toLowerCase() + "." + mimeType);
    }
  };

  /** Unify exception handling. */
  public static <A> A handleException(final Function0<A> f) {
    try {
      return f.apply();
    } catch (EpisodeServiceException e) {
      if (e.isCauseNotAuthorized())
        throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
      if (e.isCauseNotFound())
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      if (e instanceof NotFoundException)
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }
}
