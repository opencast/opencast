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

package org.opencastproject.basicstatistics.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;

import org.opencastproject.basicstatistics.BasicStatisticsService;
import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link BasicStatisticsService} service
 */
@Component(
    property = {
        "service.description=Basic Statistics REST Endpoint",
        "opencast.service.type=org.opencastproject.basicstatistics",
        "opencast.service.path=/basicstatistics",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = BasicStatisticsRestEndpoint.class
)
@Path("/basicstatistics")
@RestService(
    name = "BasicStatisticsServiceEndpoint",
    title = "Basic Statistics Service Endpoint",
    abstractText = "This service offers the endpoints for Opencasts basic statistics",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the "
            + "underlying service is not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was "
            + "not anticipated. In other words, there is a bug! You should file an error report "
            + "with your server logs from the time when the error occurred: "
            + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"
    }
)
@JaxrsResource
public class BasicStatisticsRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsRestEndpoint.class);

  /** (duration): specifies how far in the past raw events are allowed to be when added by a non-trusted source. */
  private static final Duration MAX_CLIENT_PUSH_DELAY = Duration.ofMinutes(15);
  /** duration): specifies the maximum allowed/expected clock skew between different devices
   * (user client, file server, Opencast). */
  private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofSeconds(30);

  /** The service */
  protected BasicStatisticsService basicStatisticsService;

  private static final Gson GSON = new Gson();

  @GET
  @Path("")
  @RestQuery(
      name = "getRawEvents",
      description = "Get raw events.",
      returnDescription = "A JSON object containing an array of raw events.",
      restParameters = {
          @RestParameter(name = "limit", isRequired = false, type = INTEGER,
              description = "The maximum number of results to return for a single request.", defaultValue = "100"),
          @RestParameter(name = "offset", isRequired = false, type = INTEGER,
              description = "The index of the first result to return.", defaultValue = "0"),
      },
      responses = {
          @RestResponse(description = "Returns the raw events.", responseCode = SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = SC_BAD_REQUEST)
      })
  public Response getRawEvents(
      @HeaderParam("Accept") String acceptHeader,
      @QueryParam("limit") Integer limit,
      @QueryParam("offset") Integer offset) {

    Optional<Integer> optLimit = Optional.ofNullable(limit);
    Optional<Integer> optOffset = Optional.ofNullable(offset);

    // If the limit is set to 0, this is not taken into account.
    if (optLimit.isPresent() && limit == 0) {
      optLimit = Optional.empty();
    }

    // Apply pagination
    int effectiveOffset = optOffset.orElse(0);
    int effectiveLimit = optLimit.orElse(100);

    List<RawEvent> allEvents
        = basicStatisticsService.getRawEvents(effectiveLimit, effectiveOffset);

    List<ClientEventDto> clientEvents = new ArrayList<>();

    for (RawEvent event : allEvents) {

      ClientEventDto dto = new ClientEventDto();
      dto.setTimestamp(event.getTimestamp().toString());
      dto.setItemType(event.getItemType().toString());
      dto.setItemId(event.getItemId());
      dto.setEventType(event.getEventType().toString());
      dto.setEventPayload(event.getEventPayload());

      clientEvents.add(dto);
    }

    return Response.ok(
        GSON.toJson(clientEvents),
        MediaType.APPLICATION_JSON
    ).build();
  }

  /**
   * the client (user browser) directly sends a request to that API, authentication is not required.
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @POST
  @Path("clientpush")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
      name = "clientpush",
      description = "example service call",
      responses = {
          @RestResponse(
              responseCode = HttpServletResponse.SC_OK,
              description = "Hello World"
          ),
          @RestResponse(
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              description = "The underlying service could not output something."
          )
      },
      returnDescription = "The text that the service returns."
  )
  public Response clientPush(@Context HttpServletRequest request) {

    try {
      String json = readInputStream(request);

      ClientPushRequest pushRequest = GSON.fromJson(json, ClientPushRequest.class);

      for (ClientEventDto event : pushRequest.getEvents()) {
        logger.info(event.getTimestamp().toString());
      }

      List<RawEvent> accepted = new ArrayList<>();
      List<RejectedEvent> rejected = new ArrayList<>();
      Instant now = Instant.now();

      for (int index = 0; index < pushRequest.getEvents().size(); index++) {
        ClientEventDto dto = pushRequest.getEvents().get(index);

        // Validation
        if (dto.getItemId() == null) {
          rejected.add(new RejectedEvent(index, "Item id was not specified"));
          continue;
        }
        ItemType itemType;
        try {
          itemType = ItemType.valueOf(dto.getItemType());
        } catch (IllegalArgumentException e) {
          rejected.add(new RejectedEvent(index, "Unknown itemType '" + dto.getItemType() + "'"));
          continue;
        }
        EventType eventType;
        try {
          eventType = EventType.valueOf(dto.getEventType());
        } catch (IllegalArgumentException e) {
          rejected.add(new RejectedEvent(index, "Unknown eventType '" + dto.getEventType() + "'"));
          continue;
        }
        if (eventType.equals(EventType.FETCH_FILE)) {
          rejected.add(new RejectedEvent(index, "EventType 'FETCH_FILE' is disallowed on client-push"));
          continue;
        }
        if (!RawEvent.payloadValidator(eventType, dto.getEventPayload())) {
          rejected.add(new RejectedEvent(index, "Event payload is malformed"));
          continue;
        }
        Instant timestamp;
        try {
          timestamp = Instant.parse(dto.getTimestamp());
        } catch (DateTimeParseException e) {
          rejected.add(new RejectedEvent(index, "Invalid timestamp '" + dto.getTimestamp() + "'. "
              + "Expected RFC3339 format, e.g. 2026-04-27T14:56:38.415Z."));
          continue;
        }
        if (timestamp.isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
          rejected.add(new RejectedEvent(index, "Timestamp is too far in the future."));
          continue;
        }
        if (timestamp.isBefore(now.minus(MAX_CLIENT_PUSH_DELAY))) {
          rejected.add(new RejectedEvent(index, "Timestamp is too old."));
          continue;
        }

        // Create event
        RawEvent event = new RawEvent();
        event.setTimestamp(timestamp);
        event.setItemType(itemType);
        event.setItemId(dto.getItemId());
        event.setEventType(eventType);
        event.setEventPayload(dto.getEventPayload());

        // TODO: Calculate/Add session hash here?

        accepted.add(event);
      }

      basicStatisticsService.create(accepted);

      ClientPushResponse response = new ClientPushResponse(accepted.size(), rejected);

      return Response.ok(GSON.toJson(response), MediaType.APPLICATION_JSON).build();

    } catch (JsonSyntaxException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(e.getMessage())
          .build();
    }
  }

  protected String readInputStream(HttpServletRequest request) {
    String details;
    try (InputStream is = request.getInputStream()) {
      details = IOUtils.toString(is, request.getCharacterEncoding());
    } catch (IOException e) {
      logger.error("Error reading request body:", e);
      return null;
    }
    return details;
  }

  @Reference
  public void setBasicStatisticsService(BasicStatisticsService service) {
    this.basicStatisticsService = service;
  }
}
