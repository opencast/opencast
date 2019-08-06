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
package org.opencastproject.lti.service.endpoint;

import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.lti.service.api.LtiService;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "ltirestservice", title = "LTI Service", notes = {}, abstractText = "Provides operations to LTI clients")
public class LtiServiceRestEndpoint {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(LtiServiceRestEndpoint.class);

  /** Base URL of this endpoint */
  private String endpointBaseUrl;

  /* OSGi service references */
  private LtiService service;

  /** OSGi DI */
  public void setService(LtiService service) {
    this.service = service;
  }

  /** OSGi activation method */
  void activate(ComponentContext cc) {
    logger.info("Activating LTI service Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
            RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
    logger.debug("Configured service endpoint is {}", endpointBaseUrl);
  }

  @GET
  @Path("/jobs")
  public Response listJobs(@QueryParam("series_name") final String seriesName, @QueryParam("series") String seriesId) {
    final List<Map<String, String>> results = service.listJobs(seriesName, seriesId).stream().map(e -> {
      Map<String, String> eventMap = new HashMap<>();
      eventMap.put("title", e.getTitle());
      eventMap.put("status", e.getEventStatus());
      return eventMap;
    }).collect(Collectors.toList());
    return Response.status(Status.OK).entity(new Gson().toJson(results, List.class)).build();
  }

  @POST
  @Path("/")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "createevent", description = "Creates an event by sending metadata, access control list, processing instructions and files in a multipart request.", returnDescription = "", restParameters = {
          @RestParameter(name = "acl", isRequired = false, description = "A collection of roles with their possible action", type = STRING),
          @RestParameter(name = "metadata", description = "Event metadata as Form param", isRequired = false, type = STRING),
          @RestParameter(name = "scheduling", description = "Scheduling information as Form param", isRequired = false, type = STRING),
          @RestParameter(name = "presenter", description = "Presenter movie track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "presentation", description = "Presentation movie track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "audio", description = "Audio track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "processing", description = "Processing instructions task configuration", isRequired = false, type = STRING) }, reponses = {
                  @RestResponse(description = "A new event is created and its identifier is returned in the Location header.", responseCode = HttpServletResponse.SC_CREATED),
                  @RestResponse(description = "The event could not be created due to a scheduling conflict.", responseCode = HttpServletResponse.SC_CONFLICT),
                  @RestResponse(description = "The request is invalid or inconsistent..", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response createNewEvent(@HeaderParam("Accept") String acceptHeader, @Context HttpServletRequest request) {
    String seriesName = "";
    String seriesId = "";
    Map<String, String> metadata = new HashMap<>();
    try {
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        final FileItemStream item = iter.next();
        final String fieldName = item.getFieldName();
        if ("hidden_series_name".equals(fieldName)) {
          seriesName = Streams.asString(item.openStream());
        } else if ("isPartOf".equals(fieldName)) {
          final String fieldValue = Streams.asString(item.openStream());
          if (!fieldValue.isEmpty()) {
            seriesId = fieldValue;
            metadata.put(fieldName, fieldValue);
          }
        } else if (item.isFormField()) {
          final String fieldValue = Streams.asString(item.openStream());
          metadata.put(fieldName, fieldValue);
        } else {
          final InputStream stream = item.openStream();
          final String streamName = item.getName();
          final String resultSeriesId = service.upload(stream, streamName, seriesId, seriesName, metadata);
          return Response.ok().entity(resultSeriesId).build();
        }
      }
      return Response.status(Status.BAD_REQUEST).entity("No file given").build();
    } catch (FileUploadException | IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error while uploading").build();
    }
  }

  @DELETE
  @Path("{eventId}")
  @RestQuery(name = "deleteevent", description = "Deletes an event.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(description = "The event has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEvent(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id) {
    service.delete(id);
    return Response.noContent().build();
  }
}
