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

import org.opencastproject.basicstatistics.BasicStatisticsSecretService;
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
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

  /** Daily secret service */
  protected BasicStatisticsSecretService secretService;

  private static final Gson GSON = new Gson();

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

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

  @POST
  @Path("client-push")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
      name = "clientpush",
      description = "Unauthenticated request for clients, i.e. user browers",
      responses = {
          @RestResponse(
              responseCode = HttpServletResponse.SC_OK,
              description = "The server was able to process the request"
          ),
          @RestResponse(
              responseCode = SC_BAD_REQUEST,
              description = "The request could not be accepted for some reason"
          ),
          @RestResponse(
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              description = "The underlying service could not output something."
          )
      },
      returnDescription = "Returns an object of the form { int accepted, List<RejectedEvent> rejected }. \n"
          + "accepted: number of events stored in the database \n"
          + "rejected: { index: index of the invalid item in the request array (0 indexed) \n"
          + "error: string with short, developer focussed error message }"
  )
  public Response clientPush(@Context HttpServletRequest request) {
    // Parse request header
    // Parse client IP
    InetAddress ip;
    String ipString;
    if (StringUtils.isNotBlank(request.getHeader(X_FORWARDED_FOR))) {
      logger.trace("Found '{}' header for client IP '{}'", X_FORWARDED_FOR, request.getHeader(X_FORWARDED_FOR));
      ipString = request.getHeader(X_FORWARDED_FOR);
      ipString = ipString.split(",")[0].trim();
    } else {
      logger.trace("Using client IP from request '{}'", request.getRemoteAddr());
      ipString = request.getRemoteAddr();
    }
    try {
      ip = InetAddress.getByName(ipString);
    } catch (UnknownHostException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("no IP address for the host could be found")
          .build();
    }

    // Parse user agent
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Could not find User-Agent header")
          .build();
    }

    // Parse request body
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

        // Parsing dto into event
        RawEvent event;
        try {
          event = parseDto(dto);
        } catch (IllegalArgumentException e) {
          rejected.add(new RejectedEvent(index, e.getMessage()));
          continue;
        }

        // Validation
        RejectedEvent rejectedEvent = validate(event, index, now);
        if (rejectedEvent != null) {
          rejected.add(rejectedEvent);
          continue;
        }
        // Additional Validation
        if (event.getEventType().equals(EventType.FETCH_FILE)) {
          rejected.add(new RejectedEvent(index, "EventType 'FETCH_FILE' is disallowed on client-push"));
          continue;
        }
        if (event.getTimestamp().isBefore(now.minus(MAX_CLIENT_PUSH_DELAY))) {
          rejected.add(new RejectedEvent(index, "Timestamp is too old."));
          continue;
        }

        // Calculate/Add session hash
        String sessionHash = basicStatisticsService.generateSessionHash(dto.getItemId(), ip, userAgent);
        event.setSession(sessionHash);

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

  @POST
  @Path("trusted-push")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
      name = "trustedpush",
      description = "Authenticated request for servers/nodes i.e. octoka",
      responses = {
          @RestResponse(
              responseCode = HttpServletResponse.SC_OK,
              description = "The server was able to process the request"
          ),
          @RestResponse(
              responseCode = SC_BAD_REQUEST,
              description = "The request could not be accepted for some reason"
          ),
          @RestResponse(
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              description = "The underlying service could not output something."
          )
      },
      returnDescription = "Returns an object of the form { int accepted, List<RejectedEvent> rejected }. \n"
          + "accepted: number of events stored in the database \n"
          + "rejected: { index: index of the invalid item in the request array (0 indexed) \n"
          + "error: string with short, developer focussed error message }"
  )
  public Response trustedPush(@Context HttpServletRequest request) {
    try {
      String json = readInputStream(request);

      TrustedPushRequest pushRequest = GSON.fromJson(json, TrustedPushRequest.class);

      for (TrustedEventDto event : pushRequest.getEvents()) {
        logger.info(event.getTimestamp().toString());
      }

      List<RawEvent> accepted = new ArrayList<>();
      List<RejectedEvent> rejected = new ArrayList<>();
      Instant now = Instant.now();
      byte[] dailySecret = secretService.getCurrentSecret();

      for (int index = 0; index < pushRequest.getEvents().size(); index++) {
        TrustedEventDto dto = pushRequest.getEvents().get(index);

        // Parsing dto into event
        RawEvent event;
        try {
          event = parseDto(dto);
        } catch (IllegalArgumentException e) {
          rejected.add(new RejectedEvent(index, e.getMessage()));
          continue;
        }
        // Additional parsing
        InetAddress ip;
        try {
          ip = InetAddress.getByName(dto.getAddr());
        } catch (UnknownHostException e) {
          return Response.status(Response.Status.BAD_REQUEST)
              .entity("no IP address for the host could be found")
              .build();
        }

        // Validation
        RejectedEvent rejectedEvent = validate(event, index, now);
        if (rejectedEvent != null) {
          rejected.add(rejectedEvent);
          continue;
        }
        // Additional Validation
        if (dto.getUa() == null) {
          rejected.add((new RejectedEvent(index, "ua (user agent) field must be set on trusted push")));
          continue;
        }

        // Calculate/Add session hash
        String sessionHash = basicStatisticsService.generateSessionHash(
            dailySecret,
            dto.getItemId(),
            ip,
            dto.getUa()
        );
        event.setSession(sessionHash);

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

  private RawEvent parseDto(ClientEventDto dto) throws IllegalArgumentException {
    ItemType itemType;
    try {
      itemType = ItemType.valueOf(dto.getItemType());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown itemType '" + dto.getItemType() + "'");
    }

    EventType eventType;
    try {
      eventType = EventType.valueOf(dto.getEventType());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown eventType '" + dto.getEventType() + "'");
    }

    Instant timestamp;
    try {
      timestamp = Instant.parse(dto.getTimestamp());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid timestamp '" + dto.getTimestamp() + "'. "
          + "Expected RFC3339 format, e.g. 2026-04-27T14:56:38.415Z.");
    }

    RawEvent event = new RawEvent();
    event.setTimestamp(timestamp);
    event.setItemType(itemType);
    event.setItemId(dto.getItemId());
    event.setEventType(eventType);
    event.setEventPayload(dto.getEventPayload());

    return event;
  }

  private RejectedEvent validate(RawEvent event, int index, Instant now) {
    if (event.getItemId() == null) {
      return new RejectedEvent(index, "Item id was not specified");
    }
    if (!RawEvent.payloadValidator(event.getEventType(), event.getEventPayload())) {
      return new RejectedEvent(index, "Event payload is malformed");
    }
    if (event.getTimestamp().isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
      return new RejectedEvent(index, "Timestamp is too far in the future.");
    }

    return null;
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
  @Reference()
  public void setBasicStatisticsSecretService(BasicStatisticsSecretService secretService) {
    this.secretService = secretService;
  }
}
