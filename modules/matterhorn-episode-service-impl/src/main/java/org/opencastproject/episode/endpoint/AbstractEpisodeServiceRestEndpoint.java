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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.opencastproject.episode.api.ArchivedMediaPackageElement;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.EpisodeServiceException;
import org.opencastproject.episode.api.JaxbSearchResult;
import org.opencastproject.episode.api.JaxbSearchResultItem;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
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
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.episode.api.ConfiguredWorkflow.workflow;
import static org.opencastproject.mediapackage.MediaPackageSupport.rewriteUris;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Misc.chuck;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

/** REST endpoint of the episode service. */
// no @Path annotation here since this class cannot be created by JAX-RS. Put it on implementations.
@RestService(name = "episode", title = "Episode Service", notes = {
        "All paths are relative to the REST endpoint base (something like http://your.server/files)",
        "If you notice that this service is not working as expected, there might be a bug! "
                + "You should file an error report with your server logs from the time when the error occurred: "
                + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>" }, abstractText = "This service indexes and queries available (distributed) episodes.")
public abstract class AbstractEpisodeServiceRestEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(AbstractEpisodeServiceRestEndpoint.class);

  /** Path prefix for archive mediapackage elements */
  public static final String ARCHIVE_PATH_PREFIX = "/archive/mediapackage/";

  /** The constant used to switch the direction of the sorting query string parameter. */
  public static final String DESCENDING_SUFFIX = "_DESC";

  public abstract EpisodeService getEpisodeService();

  public abstract WorkflowService getWorkflowService();

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
  public Response add(@FormParam("mediapackage") MediaPackageImpl mediaPackage) throws EpisodeServiceException {
    try {
      getEpisodeService().add(mediaPackage);
      return Response.noContent().build();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
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
  public Response delete(@PathParam("id") String mediaPackageId) throws EpisodeServiceException, NotFoundException {
    try {
      if (mediaPackageId != null && getEpisodeService().delete(mediaPackageId))
        return Response.noContent().build();
      else
        throw new NotFoundException();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
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
//    getEpisodeService().applyWorkflow(workflow(wd), rewriteUri, mediaPackageId);
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
  public Response applyWorkflow(@PathParam("wfDefId") String wfId,
                                @FormParam("mediaPackageIds") List<String> mpIds,
                                @Context HttpServletRequest req) throws Exception {
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
    getEpisodeService().applyWorkflow(workflow(wfd, wfp), rewriteUri, mpIds);
    return Response.noContent().build();
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
                                @RestParameter(name = "limit", type = RestParameter.Type.STRING, defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false),
                                @RestParameter(name = "offset", type = RestParameter.Type.STRING, defaultValue = "0", description = "The page number.", isRequired = false),
                                @RestParameter(name = "admin", type = RestParameter.Type.STRING, defaultValue = "false", description = "Whether this is an administrative query", isRequired = false),
                                @RestParameter(name = "sort", type = RestParameter.Type.STRING, description = "The sort order.  May include any "
                                  + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
                                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", isRequired = false),
                                @RestParameter(name = "onlyLatest", type = Type.BOOLEAN, defaultValue = "false", description = "Filter results by only latest version of the archive", isRequired = false) },
             reponses = { @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK) },
             returnDescription = "The search results, expressed as xml or json.")
  // CHECKSTYLE:OFF -- more than 7 parameters
  public Response findEpisode(@QueryParam("id") String id,
                              @QueryParam("q") String text,
                              @QueryParam("creator") String creator,
                              @QueryParam("contributor") String contributor,
                              @QueryParam("language") String language,
                              @QueryParam("series") String series,
                              @QueryParam("license") String license,
                              @QueryParam("title") String title,
                              @QueryParam("tag") String[] tags,
                              @QueryParam("flavor") String[] flavors,
                              @QueryParam("limit") int limit,
                              @QueryParam("offset") int offset,
                              @QueryParam("sort") String sort,
                              @QueryParam("onlyLatest") @DefaultValue("true") boolean onlyLatest,
                              @PathParam("format") String format) throws Exception {
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

    if (onlyLatest)
      search.withOnlyLastVersion();

    // Return the results using the requested format
    final String type = "json".equals(format) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML;
    final SearchResult sr = rewriteForDelivery(getEpisodeService().find(search));
    return Response.ok(sr).type(type).build();
  }

  @GET
  @Path(ARCHIVE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{version}")
  @RestQuery(name = "getElement",
             description = "Gets the file from the archive under /mediaPackageID/mediaPackageElementID/version",
             returnDescription = "The file",
             pathParameters = { @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
                                @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING),
                                @RestParameter(name = "version", description = "the mediapackage version", isRequired = true, type = STRING) },
             reponses = { @RestResponse(responseCode = SC_OK, description = "File returned"),
                          @RestResponse(responseCode = SC_ACCEPTED, description = "File is not ready"),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response getElement(@PathParam("mediaPackageID") String mediaPackageID,
                             @PathParam("mediaPackageElementID") final String mediaPackageElementID,
                             @PathParam("version") long version,
                             @HeaderParam("If-None-Match") String ifNoneMatch) throws Exception {
    if (StringUtils.isNotBlank(ifNoneMatch))
      return Response.notModified().build();

    return getEpisodeService().get(mediaPackageID, mediaPackageElementID, Version.version(version)).fold(new Option.Match<ArchivedMediaPackageElement, Response>() {
      @Override public Response some(ArchivedMediaPackageElement element) {
        final InputStream inputStream = element.getInputStream();
        MimeType mimeType = element.getMimeType();

        String fileName = mediaPackageElementID.concat(".");
        if (StringUtils.isNotBlank(mimeType.getSuffix())) {
          fileName = fileName.concat(mimeType.getSuffix());
        } else if (mimeType.getSuffixes().length > 0) {
          fileName = fileName.concat(mimeType.getSuffixes()[0]);
        } else {
          fileName = fileName.concat(mimeType.getSubtype());
        }

        // Write the file contents back
        return Response
                .ok(new StreamingOutput() {
                  @Override public void write(OutputStream os) throws IOException, WebApplicationException {
                    try {
                      IOUtils.copy(inputStream, os);
                    } catch (IOException e) {
                      Throwable cause = e.getCause();
                      if (cause == null || !"Broken pipe".equals(cause.getMessage()))
                        logger.warn("Error writing file contents to response", e);
                    } finally {
                      IOUtils.closeQuietly(inputStream);
                    }
                  }
                })
                .header("Content-disposition", "attachment; filename=" + fileName)
                .header("Content-length", element.getSize())
                .type(mimeType.asString())
                .build();
      }

      @Override public Response none() {
        return chuck(new NotFoundException());
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
                          @RestResponse(responseCode = SC_ACCEPTED, description = "Mediapackage is not ready"),
                          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response getMediapackage(@PathParam("mediaPackageID") String mediaPackageId) throws Exception {
    final EpisodeQuery idQuery = new EpisodeQuery().withId(mediaPackageId);
    return mlist(getEpisodeService().find(idQuery).getItems()).head().fold(new Option.Match<SearchResultItem, Response>() {
      @Override public Response some(SearchResultItem item) {
        final MediaPackage rewritten = rewriteUris(item.getMediaPackage(), rewriteUri.curry(item.getOcVersion()));
        return Response.ok(rewritten).build();
      }

      @Override public Response none() {
        return chuck(new NotFoundException());
      }
    });
  }

  /** Function to rewrite media package element URIs so that they point to this REST endpoint. */
  private final Function2<Version, MediaPackageElement, URI> rewriteUri = new Function2<Version, MediaPackageElement, URI>() {
    @Override public URI apply(Version version, MediaPackageElement mpe) {
      return uri(getServerUrl(),
                 getMountPoint(),
                 ARCHIVE_PATH_PREFIX,
                 mpe.getMediaPackage().getIdentifier(),
                 mpe.getIdentifier(),
                 version);
    }
  };

  /** Rewrite all URIs of contained media package elements to point to the EpisodeService. */
  public SearchResult rewriteForDelivery(final SearchResult result) {
    return JaxbSearchResult.create(new SearchResult() {
      @Override public List<SearchResultItem> getItems() {
        return mlist(result.getItems()).map(new Function<SearchResultItem, SearchResultItem>() {
          @Override public SearchResultItem apply(SearchResultItem item) {
            final JaxbSearchResultItem rewritten = JaxbSearchResultItem.create(item);
            rewritten.setMediaPackage(rewriteUris(item.getMediaPackage(), rewriteUri.curry(item.getOcVersion())));
            return rewritten;
          }
        }).value();
      }

      @Override public String getQuery() { return result.getQuery(); }
      @Override public long size() { return result.size(); }
      @Override public long getTotalSize() { return result.getTotalSize(); }
      @Override public long getOffset() { return result.getOffset(); }
      @Override public long getLimit() { return result.getLimit(); }
      @Override public long getSearchTime() { return result.getSearchTime(); }
      @Override public long getPage() { return result.getPage(); }
    });
  }
}
