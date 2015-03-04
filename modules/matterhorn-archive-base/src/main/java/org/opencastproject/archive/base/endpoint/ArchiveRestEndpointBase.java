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
package org.opencastproject.archive.base.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.MimeTypeUtil.suffix;
import static org.opencastproject.util.RestUtil.getResponseFormat;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.workflow.api.ConfiguredWorkflow.workflow;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.ArchivedMediaPackageElement;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.QueryBuilder;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypeUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.data.Collections;
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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

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

/**
 * A base REST endpoint for the archive. It leaves out all metadata related methods like find.
 * <p/>
 * No @Path annotation here since this class cannot be created by JAX-RS. Put it on the concrete implementations.
 */
@RestService(name = "archive", title = "Archive",
             notes = {
                     "All paths are relative to the REST endpoint base (something like http://your.server/files)",
                     "If you notice that this service is not working as expected, there might be a bug! "
                             + "You should file an error report with your server logs from the time when the error occurred: "
                             + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>"
             },
             abstractText = "This service indexes and queries available (distributed) episodes.")
public abstract class ArchiveRestEndpointBase<RS extends ResultSet> implements HttpMediaPackageElementProvider {

  private static final Logger logger = LoggerFactory.getLogger(ArchiveRestEndpointBase.class);

  /** Default path prefix for archive mediapackage elements */
  public static final String ARCHIVE_PATH_PREFIX = "/archive/mediapackage/";

  /** The constant used to switch the direction of the sorting query string parameter. */
  public static final String DESCENDING_SUFFIX = "_DESC";

  public abstract Archive<RS> getArchive();

  public abstract WorkflowService getWorkflowService();

  public abstract SecurityService getSecurityService();

  /** Return the server url. */
  public abstract String getServerUrl();

  /** Return the mount point of the endpoint, e.g. <code>/archive</code>.  */
  public abstract String getMountPoint();

  /** Convert a result set into a JAXB annotated response object. */
  public abstract Object convert(RS source);

  @POST
  @Path("add")
  @RestQuery(name = "add", description = "Adds a mediapackage to the episode service.",
             restParameters = {@RestParameter(name = "mediapackage", isRequired = true,
                                              type = RestParameter.Type.TEXT, defaultValue = "${this.sampleMediaPackage}", description = "The media package to add to the search index.")},
             reponses = {@RestResponse(description = "The mediapackage was added, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                     @RestResponse(description = "There has been an internal error and the mediapackage could not be added", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)},
             returnDescription = "No content is returned.")
  public Response add(@FormParam("mediapackage") final MediaPackageImpl mediaPackage) {
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        getArchive().add(mediaPackage);
        return noContent();
      }
    });
  }

  @DELETE
  @Path("delete/{id}")
  @RestQuery(name = "remove",
             description = "Remove an episode from the archive.",
             pathParameters = {@RestParameter(name = "id", isRequired = true,
                                              type = RestParameter.Type.STRING, description = "The media package ID to remove from the archive.")},
             reponses = {@RestResponse(description = "The mediapackage was removed, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                     @RestResponse(description = "There has been an internal error and the mediapackage could not be deleted", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)},
             returnDescription = "No content is returned.")
  public Response delete(@PathParam("id") final String mediaPackageId) {
    return handleException(new Function0.X<Response>() {
      @Override public Response xapply() throws NotFoundException {
        if (mediaPackageId != null && getArchive().delete(mediaPackageId))
          return noContent();
        else
          return notFound();
      }
    });
  }

  @POST
  @Path("apply/{wfDefId}")
  @RestQuery(name = "apply",
             description = "Apply a workflow to a list of media packages.",
             pathParameters = {@RestParameter(name = "wfDefId", type = RestParameter.Type.STRING,
                                              description = "The ID of the workflow to apply", isRequired = true)},
             restParameters = {@RestParameter(name = "mediaPackageIds", type = RestParameter.Type.STRING,
                                              description = "A list of media package ids.", isRequired = true)},
             reponses = {@RestResponse(description = "The workflows have been started.", responseCode = HttpServletResponse.SC_NO_CONTENT)},
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
        getArchive().applyWorkflow(workflow(wfd, wfp), uriRewriter, mpIds);
        return Response.noContent().build();
      }
    });
  }

  @GET
  @Path("episode.{format:xml|json}")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @RestQuery(name = "episodes",
             description = "Search for episodes matching the query parameters.",
             pathParameters = {
                     @RestParameter(name = "format",
                                    description = "The output format (json or xml) of the response body.",
                                    isRequired = true, type = RestParameter.Type.STRING)
             },
             restParameters = {
                     @RestParameter(name = "id", type = RestParameter.Type.STRING, description = "The ID of the single episode to be returned, if it exists.", isRequired = false),
                     @RestParameter(name = "series", isRequired = false, description = "Filter results by media package's series identifier.", type = STRING),
                     @RestParameter(name = "limit", type = RestParameter.Type.STRING, defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false),
                     @RestParameter(name = "offset", type = RestParameter.Type.STRING, defaultValue = "0", description = "The page number.", isRequired = false),
                     @RestParameter(name = "onlyLatest", type = Type.BOOLEAN, defaultValue = "false", description = "Filter results by only latest version of the archive", isRequired = false)},
             reponses = {
                     @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK)
             },
             returnDescription = "The search results, expressed as xml or json.")
  public Response find(@QueryParam("id") final String id,
                       @QueryParam("series") final String series,
                       @QueryParam("limit") final Integer limit,
                       @QueryParam("offset") final Integer offset,
                       @QueryParam("onlyLatest") @DefaultValue("true") final boolean onlyLatest,
                       @PathParam("format") final String format) {
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        final Query q = QueryBuilder.query()
                .currentOrganization(getSecurityService())
                .mediaPackageId(option(id).bind(Strings.trimToNone))
                .seriesId(option(series).bind(Strings.trimToNone))
                .limit(option(limit))
                .offset(option(offset))
                .onlyLastVersion(onlyLatest);
        // Return the results using the requested format
        final RS rs = getArchive().find(q, uriRewriter);
        return Response.ok(convert(rs)).type(getResponseFormat(format)).build();
      }
    });
  }

  @GET
  @Path(ARCHIVE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{version}/{ignore}")
  @RestQuery(name = "getElement",
             description = "Gets the file from the archive under /mediaPackageID/mediaPackageElementID/version",
             returnDescription = "The file",
             pathParameters = {@RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
                     @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING),
                     @RestParameter(name = "version", description = "the mediapackage version", isRequired = true, type = STRING),
                     @RestParameter(name = "ignore", description = "this value is being ignored. just for documentation purposes", isRequired = false, type = STRING)},
             reponses = {@RestResponse(responseCode = SC_OK, description = "File returned"),
                     @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found")})
  public Response getElement(@PathParam("mediaPackageID") final String mediaPackageID,
                             @PathParam("mediaPackageElementID") final String mediaPackageElementID,
                             @PathParam("version") final long version,
                             @HeaderParam("If-None-Match") final String ifNoneMatch) {
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        if (StringUtils.isNotBlank(ifNoneMatch))
          return Response.notModified().build();
        for (ArchivedMediaPackageElement element : getArchive().get(mediaPackageID, mediaPackageElementID, Version.version(version))) {
          final InputStream inputStream = element.getInputStream();
          final Option<MimeType> mimeType = option(element.getMimeType());
          final String fileName = mediaPackageElementID.concat(".").concat(mimeType.bind(suffix).getOrElse("unknown"));
          // Write the file contents back
          return RestUtil.R.ok(inputStream, mimeType.map(MimeTypeUtil.toString),
                  element.getSize() > 0 ? some(element.getSize()) : Option.<Long> none(), some(fileName));
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
             pathParameters = {
                     @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING)
             },
             reponses = {
                     @RestResponse(responseCode = SC_OK, description = "Mediapackage returned"),
                     @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found")
             })
  public Response getMediapackage(@PathParam("mediaPackageID") final String mediaPackageId) {
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        final Query idQuery = QueryBuilder.query()
                .currentOrganization(getSecurityService())
                .mediaPackageId(mediaPackageId)
                .onlyLastVersion(true);
        final ResultSet result = getArchive().find(idQuery, uriRewriter);
        if (result.size() > 1)
          return serverError();
        if (result.size() == 0)
          return notFound();
        final ResultItem item = result.getItems().get(0);
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
    } catch (ArchiveException e) {
      if (e.isCauseNotAuthorized())
        throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
      if (e.isCauseNotFound())
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      logger.error("Error calling archive REST method", e);
      if (e instanceof NotFoundException)
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }
}
