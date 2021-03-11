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

package org.opencastproject.editor.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.editor.api.EditingData;
import org.opencastproject.editor.api.EditorService;
import org.opencastproject.editor.api.EditorServiceException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link EditorService} service
 */
@Path("/")
@RestService(name = "EditorServiceEndpoint",
        title = "Editor Service Endpoint",
        abstractText = "This is the editor service.",
        notes = {"All paths above are relative to the REST endpoint base (something like http://your.server/files)",
                "If the service is down or not working it will return a status 503, this means the the underlying "
                        + "service is not working and is either restarting or has failed",
                "A status code 500 means a general failure has occurred which is not recoverable and was not "
                        + "anticipated. In other words, there is a bug! You should file an error report with your "
                        + "server logs from the time when the error occurred: "
                        + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class EditorRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(EditorRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The service */
  protected EditorService editorService;

  public void setEditorService(EditorService service) {
    this.editorService = service;
  }

  @GET
  @Path("{mediapackageId}/edit.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEditorData",
          description = "Returns all the information required to get the editor tool started",
          returnDescription = "JSON object",
          pathParameters = { @RestParameter(name = "mediapackageId", description = "The id of the media package",
                  isRequired = true, type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(description = "Media package found", responseCode = SC_OK),
          @RestResponse(description = "Media package not found", responseCode = SC_NOT_FOUND) })
  public Response getEditorData(@PathParam("mediapackageId") final String mediaPackageId) {
    try {
      EditingData response = editorService.getEditData(mediaPackageId);
      if (response != null) {
        return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE).build();
      } else {
        logger.error("EditorService returned empty response");
        return RestUtil.R.serverError();
      }
    } catch (EditorServiceException e) {
      return checkErrorState(mediaPackageId, e);
    }
  }

  @POST
  @Path("{mediapackageId}/edit.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @RestQuery(name = "editVideo", description = "Takes editing information from the client side and processes it",
          returnDescription = "",
          pathParameters = {
          @RestParameter(name = "mediapackageId", description = "The id of the media package", isRequired = true,
                  type = RestParameter.Type.STRING) },
          responses = {
          @RestResponse(description = "Editing information saved and processed", responseCode = SC_OK),
          @RestResponse(description = "Media package not found", responseCode = SC_NOT_FOUND),
          @RestResponse(description = "The editing information cannot be parsed",
                  responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response editVideo(@PathParam("mediapackageId") final String mediaPackageId,
          @Context HttpServletRequest request) {
    String details = null;
    try {
      details = readInputStream(request);
      logger.debug("Editor POST-Request received: {}", details);
      EditingData editingInfo = EditingData.parse(details);
      editorService.setEditData(mediaPackageId, editingInfo);
    } catch (IOException e) {
      return RestUtil.R.serverError();
    } catch (EditorServiceException e) {
      return checkErrorState(mediaPackageId, e);
    } catch (Exception e) {
      logger.debug("Unable to parse editing information ({})", details, e);
      return RestUtil.R.badRequest("Unable to parse details");
    }

    return RestUtil.R.ok();
  }

  @POST
  @Path("{eventId}/metadata.json")
  @RestQuery(name = "updateeventmetadata", description = "Update the passed metadata for the event with the given Id",
          pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true,
                  type = RestParameter.Type.STRING) },
          responses = {
          @RestResponse(description = "The metadata have been updated.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Could not parse metadata.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "No event with this identifier was found.",
                  responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "No content is returned.")
  public Response updateEventMetadata(@PathParam("eventId") String eventId, @Context HttpServletRequest request) {
    try {
      String details = readInputStream(request);
      editorService.setMetadata(eventId, details);
    } catch (IOException e) {
      return RestUtil.R.serverError();
    } catch (EditorServiceException e) {
      return checkErrorState(eventId, e);
    }
    return RestUtil.R.ok();
  }

  protected String readInputStream(HttpServletRequest request) throws IOException {
    String details;
    try (InputStream is = request.getInputStream()) {
      details = IOUtils.toString(is, request.getCharacterEncoding());
    } catch (IOException e) {
      logger.error("Error reading request body:", e);
      return null;
    }
    return details;
  }

  protected Response checkErrorState(@PathParam("eventId") String eventId, EditorServiceException e) {
    switch (e.getErrorStatus()) {
      case MEDIAPACKAGE_NOT_FOUND:
        return RestUtil.R.notFound(String.format("Event '{%s}' not Found", eventId), MediaType.TEXT_PLAIN_TYPE);
      case WORKFLOW_ACTIVE:
        return RestUtil.R.locked();
      case WORKFLOW_NOT_FOUND:
      case NO_INTERNAL_PUBLICATION:
        return RestUtil.R.badRequest(e.getMessage());
      case UNABLE_TO_CREATE_CATALOG:
      case WORKFLOW_ERROR:
      case UNKNOWN:
      default:
        return RestUtil.R.serverError();
    }
  }

  @GET
  @Path("{eventId}/metadata.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventmetadata",
          description = "Returns all the data related to the metadata tab in the event details modal as JSON",
          returnDescription = "All the data related to the event metadata tab as JSON",
          pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true,
                  type = RestParameter.Type.STRING) },
          responses = {
          @RestResponse(description = "Returns all the data related to the event metadata tab as JSON",
                  responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.",
                  responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventMetadata(@PathParam("eventId") String eventId) {
    try {
      String response = editorService.getMetadata(eventId);
      if (response != null) {
        return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
      } else {
        logger.error("EditorService returned empty response");
        return RestUtil.R.serverError();
      }
    } catch (EditorServiceException e) {
      return checkErrorState(eventId, e);
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }
}
