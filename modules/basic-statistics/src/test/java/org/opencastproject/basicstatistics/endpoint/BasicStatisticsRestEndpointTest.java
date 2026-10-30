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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.basicstatistics.BasicStatisticsService;
import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

/**
 * Test class for the basic statistics REST endpoint: what happens to the events that are sent to it
 */
public class BasicStatisticsRestEndpointTest {

  private static final long SESSION = 42L;

  private final List<RawEvent> stored = new ArrayList<>();
  private final List<InetAddress> hashedIps = new ArrayList<>();
  private final List<String> hashedUserAgents = new ArrayList<>();

  @Before
  public void setUp() {
    stored.clear();
    hashedIps.clear();
    hashedUserAgents.clear();
  }

  private BasicStatisticsRestEndpoint endpoint(String requestBody) {
    BasicStatisticsService service = new BasicStatisticsService() {
      @Override
      public void create(List<RawEvent> events) {
        stored.addAll(events);
      }

      @Override
      public long generateSessionHash(byte[] dailySecret, String itemId, InetAddress ip, String userAgent) {
        hashedIps.add(ip);
        hashedUserAgents.add(userAgent);
        return SESSION;
      }
    };
    BasicStatisticsRestEndpoint endpoint = new BasicStatisticsRestEndpoint() {
      @Override
      protected String readInputStream(HttpServletRequest request) {
        return requestBody;
      }
    };
    endpoint.setBasicStatisticsService(service);
    endpoint.setBasicStatisticsSecretService((BasicStatisticsSecretService) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[]{BasicStatisticsSecretService.class},
        (proxy, method, args) -> new byte[]{1}));
    return endpoint;
  }

  private static HttpServletRequest request(String forwardedFor, String remoteAddr, String userAgent) {
    return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
        new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
          switch (method.getName()) {
            case "getHeader":
              return "X-Forwarded-For".equals(args[0]) ? forwardedFor : "User-Agent".equals(args[0]) ? userAgent : null;
            case "getRemoteAddr":
              return remoteAddr;
            default:
              return null;
          }
        });
  }

  private static String body(JsonObject... events) {
    JsonArray array = new JsonArray();
    for (JsonObject event : events) {
      array.add(event);
    }
    JsonObject body = new JsonObject();
    body.add("events", array);
    return body.toString();
  }

  /** An event that is fine to send to the client endpoint */
  private static JsonObject event() {
    return event(Instant.now().minusSeconds(5));
  }

  private static JsonObject event(Instant timestamp) {
    JsonObject event = new JsonObject();
    event.addProperty("timestamp", timestamp.toString());
    event.addProperty("itemType", "video");
    event.addProperty("itemId", "abc");
    event.addProperty("eventType", "video:play");
    return event;
  }

  /** An event that is fine to send to the trusted endpoint */
  private static JsonObject trustedEvent() {
    JsonObject event = event();
    event.addProperty("addr", "9.8.7.6");
    event.addProperty("ua", "trusted-ua");
    return event;
  }

  private static JsonObject withPayload(JsonObject event, String eventType, String payloadJson) {
    event.addProperty("eventType", eventType);
    event.add("eventPayload", JsonParser.parseString(payloadJson));
    return event;
  }

  private Response clientPush(String forwardedFor, JsonObject... events) {
    return endpoint(body(events)).clientPush(request(forwardedFor, "10.0.0.7", "client-ua"));
  }

  private Response trustedPush(JsonObject... events) {
    return endpoint(body(events)).trustedPush(request(null, null, null));
  }

  private static JsonObject responseBody(Response response) {
    assertEquals(200, response.getStatus());
    return JsonParser.parseString((String) response.getEntity()).getAsJsonObject();
  }

  /** @return the error message for each rejected event, by index of the event */
  private static Map<Integer, String> rejected(JsonObject responseBody) {
    Map<Integer, String> rejected = new TreeMap<>();
    responseBody.getAsJsonArray("rejected").forEach(e ->
        rejected.put(e.getAsJsonObject().get("index").getAsInt(), e.getAsJsonObject().get("error").getAsString()));
    return rejected;
  }

  private static void assertRejected(Map<Integer, String> rejected, int index, String messagePart) {
    String message = rejected.get(index);
    assertTrue("event " + index + " should be rejected, rejected were " + rejected, message != null);
    assertTrue("'" + message + "' should contain '" + messagePart + "'", message.contains(messagePart));
  }

  @Test
  public void testClientPushStoresValidEvent() {
    Instant timestamp = Instant.now().minusSeconds(5);

    JsonObject response = responseBody(clientPush(null, event(timestamp)));

    assertEquals(1, response.get("accepted").getAsInt());
    assertEquals(0, response.getAsJsonArray("rejected").size());
    assertEquals(1, stored.size());
    RawEvent event = stored.get(0);
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(ItemType.VIDEO, event.getItemType());
    assertEquals("abc", event.getItemId());
    assertEquals(EventType.VIDEO_PLAY, event.getEventType());
    assertNull(event.getEventPayload());
    assertEquals(Long.valueOf(SESSION), event.getSession());
  }

  @Test
  public void testClientPushRejectsInvalidEventsOneByOne() {
    JsonObject noTimestamp = event();
    noTimestamp.remove("timestamp");
    JsonObject badTimestamp = event();
    badTimestamp.addProperty("timestamp", "banana");
    JsonObject noItemType = event();
    noItemType.remove("itemType");
    JsonObject badItemType = event();
    badItemType.addProperty("itemType", "VIDEO");
    JsonObject noEventType = event();
    noEventType.remove("eventType");
    JsonObject badEventType = event();
    badEventType.addProperty("eventType", "VIDEO_PLAY");
    JsonObject noItemId = event();
    noItemId.remove("itemId");
    JsonObject emptyItemId = event();
    emptyItemId.addProperty("itemId", "");
    JsonObject blankItemId = event();
    blankItemId.addProperty("itemId", "   ");

    JsonObject response = responseBody(clientPush(null, event(), noTimestamp, badTimestamp, noItemType, badItemType,
        noEventType, badEventType, noItemId, emptyItemId, blankItemId, event()));

    // the valid events are stored, even though others in the same request are not
    assertEquals(2, response.get("accepted").getAsInt());
    assertEquals(2, stored.size());
    Map<Integer, String> rejected = rejected(response);
    assertEquals(9, rejected.size());
    assertRejected(rejected, 1, "Timestamp was not specified");
    assertRejected(rejected, 2, "Invalid timestamp 'banana'");
    assertRejected(rejected, 3, "Unknown itemType");
    assertRejected(rejected, 4, "Unknown itemType 'VIDEO'");
    assertRejected(rejected, 5, "Unknown eventType");
    assertRejected(rejected, 6, "Unknown eventType 'VIDEO_PLAY'");
    assertRejected(rejected, 7, "Item id was not specified");
    assertRejected(rejected, 8, "Item id was not specified");
    assertRejected(rejected, 9, "Item id was not specified");
  }

  @Test
  public void testClientPushStoresOnlyKnownPayloadFields() {
    JsonObject seek = withPayload(event(), "video:seek",
        "{\"to\": 42856, \"flux\": \"compensated\", \"nested\": {\"a\": [1, 2]}}");

    responseBody(clientPush(null, seek));

    assertEquals(1, stored.size());
    assertEquals("{\"to\":42856}", stored.get(0).getEventPayload());
  }

  @Test
  public void testClientPushRejectsInvalidPayloads() {
    JsonObject asString = withPayload(event(), "video:seek", "\"{\\\"to\\\": 5}\"");
    JsonObject asArray = withPayload(event(), "video:seek", "[5]");
    JsonObject missingField = withPayload(event(), "video:seek", "{}");
    JsonObject missing = event();
    missing.addProperty("eventType", "video:seek");
    JsonObject negative = withPayload(event(), "video:seek", "{\"to\": -1}");
    JsonObject unexpected = withPayload(event(), "video:play", "{\"to\": 1}");
    JsonObject emptyRange = withPayload(event(), "video:watched", "{\"from\": 9, \"to\": 1}");

    JsonObject response = responseBody(clientPush(null, asString, asArray, missingField, missing, negative,
        unexpected, emptyRange));

    assertEquals(0, response.get("accepted").getAsInt());
    assertTrue(stored.isEmpty());
    Map<Integer, String> rejected = rejected(response);
    assertEquals(7, rejected.size());
    assertRejected(rejected, 0, "must be a JSON object");
    assertRejected(rejected, 1, "must be a JSON object");
    assertRejected(rejected, 2, "invalid 'to'");
    assertRejected(rejected, 3, "missing payload");
    assertRejected(rejected, 4, "invalid 'to'");
    assertRejected(rejected, 5, "unexpected payload");
    assertRejected(rejected, 6, "invalid 'to'");
  }

  @Test
  public void testClientPushTimestampLimits() {
    Instant now = Instant.now();

    JsonObject response = responseBody(clientPush(null,
        event(now.plus(Duration.ofSeconds(5))),     // clocks are not exactly in sync
        event(now.plus(Duration.ofMinutes(2))),     // too far in the future
        event(now.minus(Duration.ofMinutes(5))),    // buffered for a while
        event(now.minus(Duration.ofMinutes(30)))));  // buffered for too long

    assertEquals(2, response.get("accepted").getAsInt());
    Map<Integer, String> rejected = rejected(response);
    assertEquals(2, rejected.size());
    assertRejected(rejected, 1, "too far in the future");
    assertRejected(rejected, 3, "too old");
  }

  @Test
  public void testClientPushDoesNotAcceptFileFetchedEvents() {
    JsonObject fetch = withPayload(event(), "fetch-file", "{\"elem\": \"video.mp4\", \"from\": 0}");

    JsonObject response = responseBody(clientPush(null, fetch));

    assertEquals(0, response.get("accepted").getAsInt());
    assertRejected(rejected(response), 0, "disallowed on clientPush");
  }

  @Test
  public void testClientPushUsesForwardedClientIp() throws Exception {
    responseBody(clientPush("1.2.3.4, 5.6.7.8", event()));
    assertEquals(List.of(InetAddress.getByName("1.2.3.4")), hashedIps);
    assertEquals(List.of("client-ua"), hashedUserAgents);

    hashedIps.clear();
    responseBody(clientPush(null, event()));
    assertEquals(List.of(InetAddress.getByName("10.0.0.7")), hashedIps);
  }

  @Test
  public void testClientPushRejectsRequestWithoutValidClientIp() {
    for (String forwardedFor : new String[]{"example.org", "not an ip", "127.1"}) {
      Response response = endpoint(body(event())).clientPush(request(forwardedFor, "10.0.0.7", "client-ua"));
      assertEquals(forwardedFor, 400, response.getStatus());
    }
    assertTrue(stored.isEmpty());
  }

  @Test
  public void testClientPushRejectsRequestWithoutUserAgent() {
    Response response = endpoint(body(event())).clientPush(request(null, "10.0.0.7", null));

    assertEquals(400, response.getStatus());
    assertTrue(stored.isEmpty());
  }

  @Test
  public void testTrustedPushAcceptsWhatClientsAreNotAllowedToSend() throws Exception {
    JsonObject fetch = withPayload(trustedEvent(), "fetch-file", "{\"elem\": \"video.mp4\", \"from\": 0}");
    JsonObject old = trustedEvent();
    old.addProperty("timestamp", Instant.now().minus(Duration.ofHours(5)).toString());

    JsonObject response = responseBody(trustedPush(fetch, old));

    assertEquals(2, response.get("accepted").getAsInt());
    assertEquals(EventType.FETCH_FILE, stored.get(0).getEventType());
    assertEquals("{\"elem\":\"video.mp4\",\"from\":0}", stored.get(0).getEventPayload());
    // the address and user agent are those of the user, as reported by the trusted server
    assertEquals(List.of(InetAddress.getByName("9.8.7.6"), InetAddress.getByName("9.8.7.6")), hashedIps);
    assertEquals(List.of("trusted-ua", "trusted-ua"), hashedUserAgents);
  }

  @Test
  public void testTrustedPushValidatesAddressAndUserAgentOneByOne() {
    JsonObject noAddr = trustedEvent();
    noAddr.remove("addr");
    JsonObject blankAddr = trustedEvent();
    blankAddr.addProperty("addr", "  ");
    JsonObject hostName = trustedEvent();
    hostName.addProperty("addr", "example.org");
    JsonObject noUa = trustedEvent();
    noUa.remove("ua");

    JsonObject response = responseBody(trustedPush(trustedEvent(), noAddr, blankAddr, hostName, noUa,
        trustedEvent()));

    assertEquals(2, response.get("accepted").getAsInt());
    assertEquals(2, stored.size());
    Map<Integer, String> rejected = rejected(response);
    assertEquals(4, rejected.size());
    assertRejected(rejected, 1, "addr (IP address) field must be set");
    assertRejected(rejected, 2, "addr (IP address) field must be set");
    assertRejected(rejected, 3, "'example.org' is not a valid IP address");
    assertRejected(rejected, 4, "ua (user agent) field must be set");
  }
}
