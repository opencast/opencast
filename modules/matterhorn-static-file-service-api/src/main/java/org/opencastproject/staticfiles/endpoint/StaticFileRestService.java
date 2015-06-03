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
package org.opencastproject.staticfiles.endpoint;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.staticfiles.api.StaticFileService;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.ProgressInputStream;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Stores and serves static files via HTTP.
 */
@Path("/")
@RestService(name = "StaticResourceService", title = "Static Resources Service", abstractText = "This service allows the uploading of static resources such as videos and images.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
                "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class StaticFileRestService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(StaticFileRestService.class);

  /** The default URL path of the static files */
  public static final String STATICFILES_URL_PATH = "staticfiles";

  /** The key to find whether the static file webserver is enabled or not. */
  public static final String STATICFILES_WEBSERVER_ENABLED_KEY = "org.opencastproject.staticfiles.webserver.enabled";

  /** The key to find the URL for where a webserver is hosting the static files. */
  public static final String STATICFILES_WEBSERVER_URL_KEY = "org.opencastproject.staticfiles.webserver.url";

  /** The key to find the maximum sized file to accept as an upload. */
  public static final String STATICFILES_UPLOAD_MAX_SIZE_KEY = "org.opencastproject.staticfiles.upload.max.size";

  /** The security service */
  private SecurityService securityService = null;

  /** The static file service */
  private StaticFileService staticFileService;

  /** The URL of the current server */
  private String serverUrl;

  /** The URL to serve static files from a webserver instead of Matterhorn. */
  private Option<String> webserverURL = Option.none();

  /** The maximum file size to allow to be uploaded in bytes, default 1GB */
  private long maxUploadSize = 1000000000;

  /**
   * Whether to provide urls to download the static files from a webserver without organization and security protection,
   * or use Matterhorn to provide the files.
   */
  protected boolean useWebserver = false;

  /** OSGi callback to bind service instance. */
  public void setStaticFileService(StaticFileService staticFileService) {
    this.staticFileService = staticFileService;
  }

  /** OSGi callback to bind service instance. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGI callback for activating this component
   *
   * @param cc
   *          the osgi component context
   */
  public void activate(ComponentContext cc) throws ConfigurationException {
    logger.info("Static File REST Service started.");
    serverUrl = OsgiUtil.getContextProperty(cc, MatterhornConstants.SERVER_URL_PROPERTY);
    useWebserver = BooleanUtils.toBoolean(OsgiUtil.getOptCfg(cc.getProperties(), STATICFILES_WEBSERVER_ENABLED_KEY)
            .getOrElse("false"));
    webserverURL = OsgiUtil.getOptCfg(cc.getProperties(), STATICFILES_WEBSERVER_URL_KEY);

    Option<String> cfgMaxUploadSize = OsgiUtil.getOptContextProperty(cc, STATICFILES_UPLOAD_MAX_SIZE_KEY);
    if (cfgMaxUploadSize.isSome())
      maxUploadSize = Long.parseLong(cfgMaxUploadSize.get());
  }

  @GET
  @Path("{uuid}")
  @RestQuery(name = "getStaticFile", description = "Returns a static file resource", pathParameters = { @RestParameter(description = "Static File Universal Unique Id", isRequired = true, name = "uuid", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns a static file resource", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No file by the given uuid found", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response getStaticFile(@PathParam("uuid") String uuid) throws NotFoundException {
    try (InputStream file = staticFileService.getFile(uuid)) {
      final String filename = staticFileService.getFileName(uuid);
      return RestUtil.R.ok(file, getMimeType(filename), Option.none(Long.class), Option.some(filename));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Unable to retrieve file with uuid {} because: {}", uuid, ExceptionUtils.getStackTrace(e));
      return Response.serverError().build();
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @Path("")
  @RestQuery(name = "postStaticFile", description = "Post a new static resource", bodyParameter = @RestParameter(description = "The static resource file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
    @RestResponse(description = "Returns the id of the uploaded static resource", responseCode = HttpServletResponse.SC_CREATED),
    @RestResponse(description = "No filename or file to upload found", responseCode = HttpServletResponse.SC_BAD_REQUEST),
    @RestResponse(description = "The upload size is too big", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response postStaticFile(@Context HttpServletRequest request) {
    if (maxUploadSize > 0 && request.getContentLength() > maxUploadSize) {
      logger.warn("Preventing upload of static file as its size {} is larger than the max size allowed {}",
              request.getContentLength(), maxUploadSize);
      return Response.status(Status.BAD_REQUEST).build();
    }
    ProgressInputStream inputStream = null;
    try {
      String filename = null;
      if (ServletFileUpload.isMultipartContent(request)) {
        boolean isDone = false;
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            continue;
          } else {
            logger.debug("Processing file item");
            filename = item.getName();
            inputStream = new ProgressInputStream(item.openStream());
            inputStream.addPropertyChangeListener(new PropertyChangeListener() {
              @Override
              public void propertyChange(PropertyChangeEvent evt) {
                long totalNumBytesRead = (Long) evt.getNewValue();
                if (totalNumBytesRead > maxUploadSize) {
                  logger.warn("Upload limit of {} bytes reached, returning a bad request.", maxUploadSize);
                  throw new WebApplicationException(Status.BAD_REQUEST);
                }
              }
            });
            isDone = true;
          }
          if (isDone)
            break;
        }
      } else {
        logger.warn("Request is not multi part request, returning a bad request.");
        return Response.status(Status.BAD_REQUEST).build();
      }

      if (filename == null) {
        logger.warn("Request was missing the filename, returning a bad request.");
        return Response.status(Status.BAD_REQUEST).build();
      }

      if (inputStream == null) {
        logger.warn("Request was missing the file, returning a bad request.");
        return Response.status(Status.BAD_REQUEST).build();
      }

      String uuid = staticFileService.storeFile(filename, inputStream);
      try {
        return Response.created(getStaticFileURL(uuid)).entity(uuid).build();
      } catch (NotFoundException e) {
        logger.error("Previous stored file with uuid {} couldn't beren found: {}", uuid,
                ExceptionUtils.getStackTrace(e));
        return Response.serverError().build();
      }
    } catch (WebApplicationException e) {
      return e.getResponse();
    } catch (Exception e) {
      logger.error("Unable to store file because: {}", ExceptionUtils.getStackTrace(e));
      return Response.serverError().build();
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  @POST
  @Path("{uuid}/persist")
  @RestQuery(name = "persistFile", description = "Persists a recently uploaded file to the permanent storage", pathParameters = { @RestParameter(description = "File UUID", isRequired = true, name = "uuid", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "The file has been persisted", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No file by the given UUID found", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response persistFile(@PathParam("uuid") String uuid) throws NotFoundException {
    try {
      staticFileService.persistFile(uuid);
      return R.ok();
    } catch (IOException e) {
      logger.error("Unable to persist file '{}': {}", uuid, getStackTrace(e));
      return R.serverError();
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{uuid}/url")
  @RestQuery(name = "getStaticFileUrl", description = "Returns a static file resource's URL", pathParameters = { @RestParameter(description = "Static File Universal Unique Id", isRequired = true, name = "uuid", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns a static file resource's URL", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No file by the given uuid found", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response getStaticFileUrl(@PathParam("uuid") String uuid) throws NotFoundException {
    try {
      return Response.ok(getStaticFileURL(uuid).toString()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to retrieve static file URL from {} because: {}", uuid, ExceptionUtils.getStackTrace(e));
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("{uuid}")
  @RestQuery(name = "deleteStaticFile", description = "Remove the static file", returnDescription = "No content", pathParameters = { @RestParameter(name = "uuid", description = "Static File Universal Unique Id", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "File deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No file by the given uuid found") })
  public Response deleteStaticFile(@PathParam("uuid") String uuid) throws NotFoundException {
    try {
      staticFileService.deleteFile(uuid);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to delete static file {} because: {}", uuid, ExceptionUtils.getStackTrace(e));
      return Response.serverError().build();
    }
  }

  /**
   * Get the URI for a static file resource depending on whether to get it direct from Matterhorn or from a webserver.
   *
   * @param uuid
   *          The unique identifier for the static file.
   * @return The URL for the static file resource.
   * @throws NotFoundException
   *           if the resource couldn't been found
   */
  public URI getStaticFileURL(String uuid) throws NotFoundException {
    if (useWebserver && webserverURL.isSome()) {
      return URI.create(UrlSupport.concat(webserverURL.get(), securityService.getOrganization().getId(), uuid,
              staticFileService.getFileName(uuid)));
    } else {
      return URI.create(UrlSupport.concat(serverUrl, STATICFILES_URL_PATH, uuid));
    }
  }

  private Option<String> getMimeType(final String filename) {
    Option<String> mimeType;
    try {
      mimeType = Option.some(MimeTypes.fromString(filename).toString());
    } catch (Exception e) {
      logger.warn("Unable to detect the mime type of file {}", filename);
      mimeType = Option.<String> none();
    }
    return mimeType;
  }

}
