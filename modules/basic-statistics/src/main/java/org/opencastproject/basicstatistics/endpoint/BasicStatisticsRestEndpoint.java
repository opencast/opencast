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
import org.opencastproject.basicstatistics.ClientPushRateLimiter;
import org.opencastproject.basicstatistics.EventPayload;
import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretService;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretServiceException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
        "opencast.service.path=/basic-statistics",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = BasicStatisticsRestEndpoint.class
)
@Path("/basic-statistics")
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
      description = "Get raw events. This endpoint is internal and mainly meant for testing and debugging. It may "
          + "change or be removed without notice, so external tools should not rely on it.",
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
      dto.setTimestamp(formatTimestamp(event.getTimestamp()));
      dto.setItemType(event.getItemType().getApiName());
      dto.setItemId(event.getItemId());
      dto.setEventType(event.getEventType().getApiName());
      dto.setEventPayload(event.getEventPayload() == null ? null : JsonParser.parseString(event.getEventPayload()));

      clientEvents.add(dto);
    }

    return Response.ok(
        GSON.toJson(clientEvents),
        MediaType.APPLICATION_JSON
    ).build();
  }

  @POST
  @Path("clientPush")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "clientPush",
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
    // Behind a reverse proxy, the connection comes from the proxy, so the client address is taken from the
    // X-Forwarded-For header. The IP is only used for the session has so even should the client forge it that causes
    // no real harm.
    if (StringUtils.isNotBlank(request.getHeader(X_FORWARDED_FOR))) {
      logger.trace("Found '{}' header for client IP '{}'", X_FORWARDED_FOR, request.getHeader(X_FORWARDED_FOR));
      ipString = request.getHeader(X_FORWARDED_FOR);
      ipString = ipString.split(",")[0].trim();
    } else {
      logger.trace("Using client IP from request '{}'", request.getRemoteAddr());
      ipString = request.getRemoteAddr();
    }
    try {
      ip = parseIpAddress(ipString);
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("could not determine a valid client IP address")
          .build();
    }

    // Parse user agent
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("Could not find User-Agent header")
          .build();
    }

    // Parse request body, but only if it is within the limits. Requests that are not are ignored, and the client is not
    // told, so that it cannot find out what the limits are.
    ClientPushRateLimiter rateLimiter = basicStatisticsService.getClientPushRateLimiter();
    try {
      byte[] body = readInputStream(request, rateLimiter.getMaxRequestBytes());
      if (body != null && !rateLimiter.allow(ip, body.length)) {
        logger.debug("Ignoring a client push of at least {} bytes, as a rate limit was reached", body.length);
        return Response.ok(GSON.toJson(new PushResponse(0, List.of())), MediaType.APPLICATION_JSON).build();
      }
      String json = body == null ? null : new String(body, StandardCharsets.UTF_8);

      ClientPushRequest pushRequest = json == null ? null : GSON.fromJson(json, ClientPushRequest.class);
      if (pushRequest == null || pushRequest.getEvents() == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN)
            .entity("Request body must be a JSON object with an 'events' array")
            .build();
      }

      List<RawEvent> accepted = new ArrayList<>();
      List<RejectedEvent> rejected = new ArrayList<>();
      Instant now = Instant.now();
      byte[] dailySecret;
      try {
        dailySecret = secretService.getCurrentSecret();
      } catch (BasicStatisticsSecretServiceException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.TEXT_PLAIN)
            .entity(e.getMessage())
            .build();
      }

      for (int index = 0; index < pushRequest.getEvents().size(); index++) {
        ClientEventDto dto = pushRequest.getEvents().get(index);
        if (dto == null) {
          rejected.add(new RejectedEvent(index, "Event must be a JSON object, not null"));
          continue;
        }

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
          rejected.add(new RejectedEvent(index,
              "EventType '" + EventType.FETCH_FILE.getApiName() + "' is disallowed on clientPush"));
          continue;
        }
        if (event.getTimestamp().isBefore(now.minus(MAX_CLIENT_PUSH_DELAY))) {
          rejected.add(new RejectedEvent(index, "Timestamp is too old."));
          continue;
        }

        // Calculate/Add session hash
        long sessionHash = basicStatisticsService.generateSessionHash(dailySecret, dto.getItemId(), ip, userAgent);
        event.setSession(sessionHash);

        accepted.add(event);
      }

      basicStatisticsService.create(accepted);

      PushResponse response = new PushResponse(accepted.size(), rejected);

      return Response.ok(GSON.toJson(response), MediaType.APPLICATION_JSON).build();

    } catch (JsonSyntaxException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity(e.getMessage())
          .build();
    }
  }

  @POST
  @Path("trustedPush")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "trustedPush",
      description = "Push raw events from trusted servers/nodes, e.g. octoka. "
          + "Requires a special role, see the security config.",
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
      byte[] body = readInputStream(request, Long.MAX_VALUE);
      String json = body == null ? null : new String(body, StandardCharsets.UTF_8);

      TrustedPushRequest pushRequest = json == null ? null : GSON.fromJson(json, TrustedPushRequest.class);
      if (pushRequest == null || pushRequest.getEvents() == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN)
            .entity("Request body must be a JSON object with an 'events' array")
            .build();
      }

      List<RawEvent> accepted = new ArrayList<>();
      List<RejectedEvent> rejected = new ArrayList<>();
      Instant now = Instant.now();
      byte[] dailySecret;
      try {
        dailySecret = secretService.getCurrentSecret();
      } catch (BasicStatisticsSecretServiceException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.TEXT_PLAIN)
            .entity(e.getMessage())
            .build();
      }

      for (int index = 0; index < pushRequest.getEvents().size(); index++) {
        TrustedEventDto dto = pushRequest.getEvents().get(index);
        if (dto == null) {
          rejected.add(new RejectedEvent(index, "Event must be a JSON object, not null"));
          continue;
        }

        // Parsing dto into event
        RawEvent event;
        try {
          event = parseDto(dto);
        } catch (IllegalArgumentException e) {
          rejected.add(new RejectedEvent(index, e.getMessage()));
          continue;
        }
        // Additional parsing
        if (dto.getAddr() == null || dto.getAddr().isBlank()) {
          rejected.add(new RejectedEvent(index, "addr (IP address) field must be set on trusted push"));
          continue;
        }
        InetAddress ip;
        try {
          ip = parseIpAddress(dto.getAddr());
        } catch (IllegalArgumentException e) {
          rejected.add(new RejectedEvent(index, "addr '" + dto.getAddr() + "' is not a valid IP address"));
          continue;
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
        long sessionHash = basicStatisticsService.generateSessionHash(
            dailySecret,
            dto.getItemId(),
            ip,
            dto.getUa()
        );
        event.setSession(sessionHash);

        accepted.add(event);
      }

      basicStatisticsService.create(accepted);

      PushResponse response = new PushResponse(accepted.size(), rejected);

      return Response.ok(GSON.toJson(response), MediaType.APPLICATION_JSON).build();
    } catch (JsonSyntaxException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity(e.getMessage())
          .build();
    }
  }

  private RawEvent parseDto(ClientEventDto dto) throws IllegalArgumentException {
    ItemType itemType = ItemType.fromApiName(dto.getItemType())
        .orElseThrow(() -> new IllegalArgumentException("Unknown itemType '" + dto.getItemType() + "'"));
    EventType eventType = EventType.fromApiName(dto.getEventType())
        .orElseThrow(() -> new IllegalArgumentException("Unknown eventType '" + dto.getEventType() + "'"));

    if (dto.getTimestamp() == null) {
      throw new IllegalArgumentException("Timestamp was not specified");
    }
    Instant timestamp;
    try {
      timestamp = Instant.parse(dto.getTimestamp());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid timestamp '" + dto.getTimestamp() + "'. "
          + "Expected RFC3339 format, e.g. 2026-04-27T14:56:38.415Z.");
    }

    // Parse the payload into its type, and store that, so that unknown fields from the request are not stored
    EventPayload payload;
    try {
      payload = RawEvent.parsePayload(eventType, dto.getEventPayload()).orElse(null);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Event payload is malformed: " + e.getMessage());
    }

    RawEvent event = new RawEvent();
    event.setTimestamp(timestamp);
    event.setItemType(itemType);
    event.setItemId(dto.getItemId());
    event.setEventType(eventType);
    event.setEventPayload(RawEvent.serializePayload(payload));

    return event;
  }

  /**
   * Parse an IP address, i.e. an IPv4 or IPv6 address literal.
   *
   * This is deliberately not {@link InetAddress#getByName(String)}: that method resolves host names through DNS, which
   * can be slow, and which we must not let clients trigger (the client IP can come from a request header), and it
   * accepts unusual forms like an empty string, which it turns into the loopback address.
   *
   * @throws IllegalArgumentException if the string is not an IP address literal
   */
  private static InetAddress parseIpAddress(String address) throws IllegalArgumentException {
    return InetAddresses.forString(address);
  }

  /**
   * Format a timestamp as RFC 3339 date-time for the API.
   *
   * {@link Instant#toString()} is documented as ISO-8601, which is more permissive than RFC 3339, but the format it
   * produces for an instant is within RFC 3339: always UTC with the designator "Z", and fractional seconds in groups
   * of three digits or omitted. The only deviation are years outside 0000 to 9999, which are printed with a sign or
   * more than four digits. Events are not expected to have such timestamps.
   */
  private static String formatTimestamp(Instant timestamp) {
    return timestamp.toString();
  }

  private RejectedEvent validate(RawEvent event, int index, Instant now) {
    if (StringUtils.isBlank(event.getItemId())) {
      return new RejectedEvent(index, "Item id was not specified");
    }
    if (event.getTimestamp().isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
      return new RejectedEvent(index, "Timestamp is too far in the future.");
    }

    return null;
  }

  @GET
  @Path("completenessThreshold")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getCompletenessThreshold",
      description = "Get the date before which statistical data may be incomplete or missing entirely.",
      returnDescription = "The threshold as a string containing an ISO date, or null if it could not "
          + "yet be determined.",
      responses = {
          @RestResponse(description = "Returns the threshold.", responseCode = SC_OK),
      })
  public Response getCompletenessThreshold() {
    LocalDate threshold = basicStatisticsService.getCompletenessThreshold();
    return Response.ok(
        GSON.toJson(threshold != null ? threshold.toString() : null),
        MediaType.APPLICATION_JSON
    ).build();
  }

  /**
   * Read the body of a request, but not all of it if it is longer than allowed.
   *
   * @param maxBytes the allowed size of the body
   * @return the bytes that were read, or null if the body could not be read
   */
  protected byte[] readInputStream(HttpServletRequest request, long maxBytes) {
    try (InputStream is = request.getInputStream()) {
      // One byte more than allowed, to be able to tell that the body is longer. Not more than an int can hold.
      int limit = (int) Math.min(maxBytes, Integer.MAX_VALUE - 1);
      return is.readNBytes(limit + 1);
    } catch (IOException e) {
      logger.error("Error reading request body:", e);
      return null;
    }
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
