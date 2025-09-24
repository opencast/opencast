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

package org.opencastproject.util;

import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Strings.split;
import static org.opencastproject.util.data.functions.Strings.trimToNil;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.rest.ErrorCodeException;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** Utility functions for REST endpoints. */
public final class RestUtil {

  private RestUtil() {
  }

  /**
   * Return the endpoint's server URL and the service path by extracting the relevant parameters from the
   * ComponentContext.
   *
   * @param cc
   *          ComponentContext to get configuration from
   * @return (serverUrl, servicePath)
   * @throws Error
   *           if the service path is not configured for this component
   */
  public static Tuple<String, String> getEndpointUrl(ComponentContext cc) {
    return getEndpointUrl(cc, OpencastConstants.SERVER_URL_PROPERTY, RestConstants.SERVICE_PATH_PROPERTY);
  }

  /**
   * Return the endpoint's server URL and the service path by extracting the relevant parameters from the
   * ComponentContext.
   *
   * @param cc
   *          ComponentContext to get configuration from
   * @param serverUrlKey
   *          Configuration key for the server URL
   * @param servicePathKey
   *          Configuration key for the service path
   * @return (serverUrl, servicePath)
   * @throws Error
   *           if the service path is not configured for this component
   */
  public static Tuple<String, String> getEndpointUrl(ComponentContext cc, String serverUrlKey, String servicePathKey) {
    final String serverUrl = Optional.ofNullable(cc.getBundleContext().getProperty(serverUrlKey)).orElse(
            UrlSupport.DEFAULT_BASE_URL);
    final String servicePath = Optional.ofNullable((String) cc.getProperties().get(servicePathKey)).orElseThrow(
        () -> new Error(RestConstants.SERVICE_PATH_PROPERTY + " property not configured"));
    return tuple(serverUrl, servicePath);
  }

  /** Create a file response. */
  public static Response.ResponseBuilder fileResponse(File f, String contentType, Optional<String> fileName) {
    final Response.ResponseBuilder b = Response.ok(f).header("Content-Type", contentType)
            .header("Content-Length", f.length());
    if (fileName.isPresent())
      b.header("Content-Disposition", "attachment; filename=" + fileName.get());
    return b;
  }

  /**
   * create a partial file response
   *
   * @param f
   *          the requested file
   * @param contentType
   *          the contentType to send
   * @param fileName
   *          the filename to send
   * @param rangeHeader
   *          the range header
   * @return the Responsebuilder
   * @throws IOException
   *           if something goes wrong
   */
  public static Response.ResponseBuilder partialFileResponse(File f, String contentType, Optional<String> fileName,
          String rangeHeader) throws IOException {

    String rangeValue = rangeHeader.trim().substring("bytes=".length());
    long fileLength = f.length();
    long start;
    long end;
    if (rangeValue.startsWith("-")) {
      end = fileLength - 1;
      start = fileLength - 1 - Long.parseLong(rangeValue.substring("-".length()));
    } else {
      String[] range = rangeValue.split("-");
      start = Long.parseLong(range[0]);
      end = range.length > 1 ? Long.parseLong(range[1]) : fileLength - 1;
    }
    if (end > fileLength - 1) {
      end = fileLength - 1;
    }

    // send partial response status code
    Response.ResponseBuilder response = Response.status(206);

    if (start <= end) {
      long contentLength = end - start + 1;
      response.header("Accept-Ranges", "bytes");
      response.header("Connection", "Close");
      response.header("Content-Length", contentLength + "");
      response.header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
      response.header("Content-Type", contentType);
      response.entity(new ChunkedFileInputStream(f, start, end));
    }

    return response;
  }

  /**
   * Create a stream response.
   *
   * @deprecated use
   *             {@link org.opencastproject.util.RestUtil.R#ok(java.io.InputStream, String, java.util.Optional, java.util.Optional)}
   *             instead
   */
  @Deprecated
  public static Response.ResponseBuilder streamResponse(InputStream in, String contentType, Optional<Long> streamLength,
          Optional<String> fileName) {
    final Response.ResponseBuilder b = Response.ok(in).header("Content-Type", contentType);
    if (streamLength.isPresent())
      b.header("Content-Length", streamLength.get());
    if (fileName.isPresent())
      b.header("Content-Disposition", "attachment; filename=" + fileName.get());
    return b;
  }

  /**
   * Return JSON if <code>format</code> == json, XML else.
   *
   * @deprecated use {@link #getResponseType(String)}
   */
  @Deprecated
  public static MediaType getResponseFormat(String format) {
    return "json".equalsIgnoreCase(format) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
  }

  /** Return JSON if <code>type</code> == json, XML else. */
  public static MediaType getResponseType(String type) {
    return "json".equalsIgnoreCase(type) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
  }

  private static final Function<String, String[]> CSV_SPLIT = split(Pattern.compile(","));

  /**
   * Split a comma separated request param into a list of trimmed strings discarding any blank parts.
   * <p>
   * x=comma,separated,,%20value -&gt; ["comma", "separated", "value"]
   */
  public static List<String> splitCommaSeparatedParam(Optional<String> param) {
    return param.map(s -> Arrays.stream(CSV_SPLIT.apply(s))
            .map(trimToNil::apply)
            .flatMap(List::stream)
            .toList())
        .orElse(List.of());
  }

  public static String generateErrorResponse(ErrorCodeException e) {
    Obj json = obj(p("error", obj(p("code", e.getErrorCode()), p("message", StringUtils.trimToEmpty(e.getMessage())))));
    return json.toJson();
  }

  /** Response builder functions. */
  public static final class R {
    private R() {
    }

    public static Response ok() {
      return Response.ok().build();
    }

    public static Response ok(Object entity) {
      return Response.ok().entity(entity).build();
    }

    public static Response ok(boolean entity) {
      return Response.ok().entity(Boolean.toString(entity)).build();
    }

    public static Response ok(Jsons.Obj json) {
      return Response.ok().entity(json.toJson()).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    public static Response ok(Job job) {
      return Response.ok().entity(new JaxbJob(job)).build();
    }

    public static Response ok(MediaType type, Object entity) {
      return Response.ok(entity, type).build();
    }

    /**
     * Create a response with status OK from a stream.
     *
     * @param in
     *          the input stream to read from
     * @param contentType
     *          the content type to set the Content-Type response header to
     * @param streamLength
     *          an optional value for the Content-Length response header
     * @param fileName
     *          an optional file name for the Content-Disposition response header
     */
    public static Response ok(InputStream in, String contentType, Optional<Long> streamLength, Optional<String> fileName) {
      return ok(in, Optional.ofNullable(contentType), streamLength, fileName);
    }

    /**
     * Create a response with status OK from a stream.
     *
     * @param in
     *          the input stream to read from
     * @param contentType
     *          the content type to set the Content-Type response header to
     * @param streamLength
     *          an optional value for the Content-Length response header
     * @param fileName
     *          an optional file name for the Content-Disposition response header
     */
    public static Response ok(InputStream in, Optional<String> contentType, Optional<Long> streamLength,
          Optional<String> fileName) {
      final Response.ResponseBuilder b = Response.ok(in);
      if (contentType.isPresent())
        b.header("Content-Type", contentType.get());
      if (streamLength.isPresent())
        b.header("Content-Length", streamLength.get());
      if (fileName.isPresent())
        b.header("Content-Disposition", "attachment; filename=" + fileName.get());
      return b.build();
    }

    public static Response created(URI location) {
      return Response.created(location).build();
    }

    public static Response notFound() {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static Response notFound(Object entity) {
      return Response.status(Response.Status.NOT_FOUND).entity(entity).build();
    }

    public static Response notFound(Object entity, MediaType type) {
      return Response.status(Response.Status.NOT_FOUND).entity(entity).type(type).build();
    }

    public static Response locked() {
      return Response.status(423).build();
    }

    public static Response serverError() {
      return Response.serverError().build();
    }

    public static Response conflict() {
      return Response.status(Response.Status.CONFLICT).build();
    }

    public static Response noContent() {
      return Response.noContent().build();
    }

    public static Response badRequest() {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    public static Response badRequest(String msg) {
      return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
    }

    public static Response forbidden() {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    public static Response forbidden(String msg) {
      return Response.status(Response.Status.FORBIDDEN).entity(msg).build();
    }

    public static Response unauthorized(Object entity) {
      return Response.status(Response.Status.UNAUTHORIZED).entity(entity).build();
    }

    public static Response conflict(String msg) {
      return Response.status(Response.Status.CONFLICT).entity(msg).build();
    }

    /**
     * create a partial file response
     *
     * @param f
     *          the requested file
     * @param contentType
     *          the contentType to send
     * @param fileName
     *          the filename to send
     * @param rangeHeader
     *          the range header
     * @return the Responsebuilder
     * @throws IOException
     *           if something goes wrong
     */

    /**
     * Creates a precondition failed status response
     *
     * @return a precondition failed status response
     */
    public static Response preconditionFailed() {
      return Response.status(Response.Status.PRECONDITION_FAILED).build();
    }

    /**
     * Creates a precondition failed status response with a message
     *
     * @param message
     *          The message body
     * @return a precondition failed status response with a message
     */
    public static Response preconditionFailed(String message) {
      return Response.status(Response.Status.PRECONDITION_FAILED).entity(message).build();
    }

  }
}
