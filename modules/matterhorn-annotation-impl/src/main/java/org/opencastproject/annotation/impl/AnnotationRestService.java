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

package org.opencastproject.annotation.impl;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.annotation.api.Annotation;
import org.opencastproject.annotation.api.AnnotationService;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The REST endpoint for the annotation service.
 */
@Path("/")
@RestService(name = "annotation", title = "Annotation Service",
  abstractText = "This service is used for managing user generated annotations.",
  notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class AnnotationRestService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AnnotationRestService.class);

  /** The annotation service */
  private AnnotationService annotationService;

  /** This server's base URL */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /** The REST endpoint's base URL */
  // this is the default value, which may be overridden in the OSGI service registration
  protected String serviceUrl = "/annotation";

  /**
   * Method to set the service this REST endpoint uses
   *
   * @param service
   *          the annotation service implementation
   */
  public void setService(AnnotationService service) {
    this.annotationService = service;
  }

  /**
   * The method that is called, when the service is activated
   *
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    }
  }

  /**
   * @return XML with all footprints
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("annotations.xml")
  @RestQuery(name = "annotationsasxml", description = "Get annotations by key and day", returnDescription = "The user annotations.", restParameters = {
          @RestParameter(name = "episode", description = "The episode identifier", isRequired = false, type = Type.STRING),
          @RestParameter(name = "type", description = "The type of annotation", isRequired = false, type = Type.STRING),
          @RestParameter(name = "day", description = "The day of creation (format: YYYYMMDD)", isRequired = false, type = Type.STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "offset", description = "The page number", isRequired = false, type = Type.INTEGER) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the user annotations") })
  public Response getAnnotationsAsXml(@QueryParam("episode") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      return Response.status(Status.BAD_REQUEST).build();

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;

    if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(type))
      return Response.ok(annotationService.getAnnotationsByTypeAndMediapackageId(type, id, offset, limit)).build();
    else if (!StringUtils.isEmpty(id))
      return Response.ok(annotationService.getAnnotationsByMediapackageId(id, offset, limit)).build();
    else if (!StringUtils.isEmpty(type) && !StringUtils.isEmpty(day))
      return Response.ok(annotationService.getAnnotationsByTypeAndDay(type, day, offset, limit)).build();
    else if (!StringUtils.isEmpty(type))
      return Response.ok(annotationService.getAnnotationsByType(type, offset, limit)).build();
    else if (!StringUtils.isEmpty(day))
      return Response.ok(annotationService.getAnnotationsByDay(day, offset, limit)).build();
    else
      return Response.ok(annotationService.getAnnotations(offset, limit)).build();
  }

  /**
   * @return JSON with all footprints
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("annotations.json")
  @RestQuery(name = "annotationsasjson", description = "Get annotations by key and day", returnDescription = "The user annotations.", restParameters = {
          @RestParameter(name = "episode", description = "The episode identifier", isRequired = false, type = Type.STRING),
          @RestParameter(name = "type", description = "The type of annotation", isRequired = false, type = Type.STRING),
          @RestParameter(name = "day", description = "The day of creation (format: YYYYMMDD)", isRequired = false, type = Type.STRING),
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = Type.INTEGER),
          @RestParameter(name = "offset", description = "The page number", isRequired = false, type = Type.INTEGER) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the user annotations") })
  public Response getAnnotationsAsJson(@QueryParam("episode") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {
    return getAnnotationsAsXml(id, type, day, limit, offset); // same logic, different @Produces annotation
  }

  @PUT
  @Path("")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "add", description = "Add an annotation on an episode", returnDescription = "The user annotation.", restParameters = {
          @RestParameter(name = "episode", description = "The episode identifier", isRequired = true, type = Type.STRING),
          @RestParameter(name = "type", description = "The type of annotation", isRequired = true, type = Type.STRING),
          @RestParameter(name = "value", description = "The value of the annotation", isRequired = true, type = Type.TEXT),
          @RestParameter(name = "in", description = "The time, or inpoint, of the annotation", isRequired = true, type = Type.STRING),
          @RestParameter(name = "out", description = "The optional outpoint of the annotation", isRequired = false, type = Type.STRING),
          @RestParameter(name = "isPrivate", description = "True if the annotation is private", isRequired = false, type = Type.BOOLEAN) },
        reponses = { @RestResponse(responseCode = SC_CREATED, description = "The URL to this annotation is returned in the Location header, and an XML representation of the annotation itelf is returned in the response body.") })
  public Response add(@FormParam("episode") String mediapackageId, @FormParam("in") int inpoint,
          @FormParam("out") int outpoint, @FormParam("type") String type, @FormParam("value") String value, @FormParam("isPrivate") boolean isPrivate,
          @Context HttpServletRequest request) {
    String sessionId = request.getSession().getId();
    Annotation a = new AnnotationImpl();
    a.setMediapackageId(mediapackageId);
    a.setSessionId(sessionId);
    a.setInpoint(inpoint);
    a.setOutpoint(outpoint);
    a.setType(type);
    a.setValue(value);
    a.setPrivate(isPrivate);
    a = annotationService.addAnnotation(a);
    URI uri;
    try {
      uri = new URI(
              UrlSupport.concat(new String[] { serverUrl, serviceUrl, Long.toString(a.getAnnotationId()), ".xml" }));
    } catch (URISyntaxException e) {
      throw new WebApplicationException(e);
    }
    return Response.created(uri).entity(a).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "change", description = "Changes the value of an annotation specified by its identifier ", returnDescription = "The user annotation.",
          pathParameters = {@RestParameter(name = "id", description = "The annotation identifier", isRequired = true, type = Type.STRING) },
          restParameters = {@RestParameter(name = "value", description = "The value of the annotation", isRequired = true, type = Type.TEXT) },
          reponses = { @RestResponse(responseCode = SC_CREATED, description = "The URL to this annotation is returned in the Location header, and an XML representation of the annotation itelf is returned in the response body.") })
  public Response change(@PathParam("id") String idAsString, @FormParam("value") String value,
          @Context HttpServletRequest request) throws NotFoundException {
    Long id = null;
    try {
      id = Long.parseLong(idAsString);
    } catch (NumberFormatException e) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
    Annotation a = annotationService.getAnnotation(id);
    a.setValue(value);
    a = annotationService.changeAnnotation(a);
    URI uri;
    try {
      uri = new URI(
              UrlSupport.concat(new String[] { serverUrl, serviceUrl, Long.toString(a.getAnnotationId()), ".xml" }));
    } catch (URISyntaxException e) {
      throw new WebApplicationException(e);
    }
    return Response.created(uri).entity(a).build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{id}.xml")
  @RestQuery(name = "annotationasxml", description = "Gets an annotation by its identifier", returnDescription = "An XML representation of the user annotation.", pathParameters = { @RestParameter(name = "id", description = "The episode identifier", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the user annotation") })
  public AnnotationImpl getAnnotationAsXml(@PathParam("id") String idAsString) throws NotFoundException {
    Long id = null;
    try {
      id = Long.parseLong(idAsString);
    } catch (NumberFormatException e) {
      throw new WebApplicationException(e, Status.BAD_REQUEST);
    }
    return (AnnotationImpl) annotationService.getAnnotation(id);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}.json")
  @RestQuery(name = "annotationasjson", description = "Gets an annotation by its identifier", returnDescription = "A JSON representation of the user annotation.", pathParameters = { @RestParameter(name = "id", description = "The episode identifier", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the user annotation") })
  public AnnotationImpl getAnnotationAsJson(@PathParam("id") String idAsString) throws NotFoundException {
    return getAnnotationAsXml(idAsString);
  }

  @DELETE
  @Path("{id}")
  @RestQuery(name = "remove", description = "Remove an annotation", returnDescription = "Return status code",
  pathParameters = { @RestParameter(name = "id", description = "The annotation identifier", isRequired = false, type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "Annotation deleted."), @RestResponse(responseCode = SC_NO_CONTENT, description = "Annotation not found.") })
  public Response removeAnnotation(@PathParam("id") String idAsString) throws NotFoundException {
    Long id = null;
    Annotation a;
    try {
      id = Long.parseLong(idAsString);
    } catch (NumberFormatException e) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
    a = (AnnotationImpl) annotationService.getAnnotation(id);
    boolean removed = annotationService.removeAnnotation(a);
    if (removed) {
        return Response.status(Status.OK).build();
    }
    else {
        return Response.status(Status.NO_CONTENT).build();
    }
  }

}
