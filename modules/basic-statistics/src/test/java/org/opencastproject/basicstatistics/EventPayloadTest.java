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

package org.opencastproject.basicstatistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

import org.junit.Test;

public class EventPayloadTest {

  private static EventPayload parse(EventType type, String json) {
    return RawEvent.parsePayload(type, JsonParser.parseString(json)).orElseThrow();
  }

  @Test
  public void testUnknownFieldsAreNotStored() {
    EventPayload payload = parse(EventType.VIDEO_WATCHED, "{\"flux\": 1, \"to\": 9, \"from\": 1, \"x\": {\"y\": [1]}}");

    assertEquals(new EventPayload.VideoWatched(1L, 9L), payload);
    // only the known fields, in the order of the type, without whitespace
    assertEquals("{\"from\":1,\"to\":9}", RawEvent.serializePayload(payload));
  }

  @Test
  public void testOptionalField() {
    EventPayload withoutEnd = parse(EventType.FETCH_FILE, "{\"elem\": \"a.mp4\", \"from\": 0}");
    assertEquals(new EventPayload.FetchFile("a.mp4", 0L, null), withoutEnd);
    assertEquals("{\"elem\":\"a.mp4\",\"from\":0}", RawEvent.serializePayload(withoutEnd));

    assertEquals(new EventPayload.FetchFile("a.mp4", 5L, 5L),
        parse(EventType.FETCH_FILE, "{\"elem\": \"a.mp4\", \"from\": 5, \"to\": 5}"));
  }

  @Test
  public void testSerializedPayloadIsNotHtmlEscaped() {
    EventPayload payload = parse(EventType.PAGE_VISIT, "{\"url\": \"/watch/<b>&x='1'\"}");
    assertEquals("{\"url\":\"/watch/<b>&x='1'\"}", RawEvent.serializePayload(payload));
  }

  @Test
  public void testEventWithoutPayload() {
    assertTrue(RawEvent.parsePayload(EventType.VIDEO_PLAY, null).isEmpty());
    // JSON null is what a null payload in a request turns into, and counts as no payload
    assertTrue(RawEvent.parsePayload(EventType.VIDEO_PLAY, JsonNull.INSTANCE).isEmpty());
    assertNull(RawEvent.serializePayload(null));
    // a payload for an event that has none is an error, not something to ignore
    assertThrows(IllegalArgumentException.class, () -> parse(EventType.VIDEO_PLAY, "{}"));
  }

  @Test
  public void testPayloadIsRequiredIfEventHasOne() {
    for (EventType type : EventType.values()) {
      if (type.getPayloadType().isPresent()) {
        assertThrows(type.getApiName(), IllegalArgumentException.class, () -> RawEvent.parsePayload(type, null));
        assertThrows(type.getApiName(), IllegalArgumentException.class, () -> parse(type, "{}"));
      }
    }
  }

  @Test
  public void testPayloadMustBeAnObject() {
    for (String notAnObject : new String[]{"[]", "[1]", "\"x\"", "5", "true"}) {
      assertThrows(notAnObject, IllegalArgumentException.class,
          () -> parse(EventType.VIDEO_SEEK, notAnObject));
    }
  }

  @Test
  public void testRanges() {
    // timestamps and byte positions are not negative, and a range does not end before it starts
    assertThrows(IllegalArgumentException.class, () -> parse(EventType.VIDEO_PAUSE, "{\"at\": -1}"));
    assertThrows(IllegalArgumentException.class, () -> parse(EventType.VIDEO_SEEK, "{\"to\": -1}"));
    assertThrows(IllegalArgumentException.class,
        () -> parse(EventType.VIDEO_WATCHED, "{\"from\": 5, \"to\": 4}"));
    assertThrows(IllegalArgumentException.class,
        () -> parse(EventType.FETCH_FILE, "{\"elem\": \"a\", \"from\": 5, \"to\": 4}"));
    assertThrows(IllegalArgumentException.class,
        () -> parse(EventType.FETCH_FILE, "{\"elem\": \"a\", \"from\": -1}"));
    // the limits themselves are fine
    assertEquals(new EventPayload.VideoPause(0L), parse(EventType.VIDEO_PAUSE, "{\"at\": 0}"));
  }
}
