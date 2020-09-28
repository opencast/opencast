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
package org.opencastproject.lti;

import org.opencastproject.lti.service.api.LtiFileUpload;
import org.opencastproject.lti.service.api.LtiService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The endpoint for the LTI gui
 */
@Path("/")
@RestService(name = "ltirestserviceendpoint", title = "LTI Service", notes = {}, abstractText = "Provides operations to LTI clients")
public class LtiServiceGuiEndpoint {
  /* OSGi service references */
  private LtiService service;

  /** OSGi DI */
  public void setService(LtiService service) {
    this.service = service;
  }

  @GET
  @Path("/new/metadata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getNewEventMetadata() {
    return Response.ok(this.service.getNewEventMetadata(), MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/{eventId}/metadata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEventMetadata(@PathParam("eventId") final String eventId) {
    try {
      return Response.ok(this.service.getEventMetadata(eventId), MediaType.APPLICATION_JSON).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(Status.UNAUTHORIZED).build();
    }
  }

  @POST
  @Path("/{eventId}/metadata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response setEventMetadata(@PathParam("eventId") final String eventId, @FormParam("metadataJson") final String metadataJson) {
    try {
      this.service.setEventMetadataJson(eventId, metadataJson);
      return Response.ok().build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(Status.UNAUTHORIZED).build();
    }
  }

  @POST
  @Path("/{eventId}/copy")
  @Produces(MediaType.APPLICATION_JSON)
  public Response copyEventToSeries(@PathParam("eventId") final String eventId, @QueryParam("target_series") final String targetSeries) {
    this.service.copyEventToSeries(eventId, targetSeries);
    return Response.ok().build();
  }

  @GET
  @Path("/jobs")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listJobs(@QueryParam("seriesId") String seriesId) {
    final List<Map<String, String>> results = service.listJobs(seriesId).stream().map(e -> {
      Map<String, String> eventMap = new HashMap<>();
      eventMap.put("title", e.getTitle());
      eventMap.put("status", e.getStatus());
      return eventMap;
    }).collect(Collectors.toList());
    return Response.status(Status.OK).entity(new Gson().toJson(results, List.class)).build();
  }

  @POST
  @Path("/")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "uploadevent", description = "Creates an event in a multipart request.", returnDescription = "", restParameters = {
          @RestParameter(name = "presenter", description = "Presenter movie track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "license", description = "License chosen", isRequired = false, type = Type.STRING),
          @RestParameter(name = "seriesName", description = "Series name", isRequired = false, type = Type.STRING),
          @RestParameter(name = "isPartOf", description = "Series ID", isRequired = false, type = Type.STRING),
          @RestParameter(name = "processing", description = "Processing instructions task configuration", isRequired = false, type = Type.STRING) }, reponses = {
                  @RestResponse(description = "A new event is created and its identifier is returned in the Location header.", responseCode = HttpServletResponse.SC_CREATED),
                  @RestResponse(description = "The event could not be created due to a scheduling conflict.", responseCode = HttpServletResponse.SC_CONFLICT),
                  @RestResponse(description = "The request is invalid or inconsistent..", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response createNewEvent(@HeaderParam("Accept") String acceptHeader, @Context HttpServletRequest request) {
    String seriesId = "";
    try {
      String captions = null;
      String metadata = null;
      String eventId = null;
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        final FileItemStream item = iter.next();
        final String fieldName = item.getFieldName();
        if ("eventId".equals(fieldName)) {
          eventId = Streams.asString(item.openStream());
        } else if ("metadata".equals(fieldName)) {
          metadata = Streams.asString(item.openStream());
        } else if ("captions".equals(fieldName)) {
          captions = Streams.asString(item.openStream());
        } else if ("seriesId".equals(fieldName)) {
          final String fieldValue = Streams.asString(item.openStream());
          if (!fieldValue.isEmpty()) {
            seriesId = fieldValue;
          }
        } else {
          service.upsertEvent(
                  new LtiFileUpload(item.openStream(), item.getName()),
                  captions,
                  eventId,
                  seriesId,
                  metadata);
          return Response.ok().build();
        }
      }
      service.upsertEvent(
              null,
              captions,
              eventId,
              seriesId,
              metadata);
      return Response.ok().build();
    } catch (FileUploadException | IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error while uploading").build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(Status.UNAUTHORIZED).build();
    }
  }

  @DELETE
  @Path("{eventId}")
  @RestQuery(name = "deleteevent", description = "Deletes an event.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = Type.STRING) }, reponses = {
          @RestResponse(description = "The event has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEvent(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id) {
    service.delete(id);
    return Response.noContent().build();
  }
}
