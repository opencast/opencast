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

package org.opencastproject.scheduler.remote;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class SchedulerServiceRemoteImplTest {

  /**
   * Returns a fixed response instead of sending the request, so the proxy's response handling can be tested without
   * a remote service registry or any HTTP traffic.
   */
  private static final class CannedResponseSchedulerServiceRemoteImpl extends SchedulerServiceRemoteImpl {
    private int responseStatus = SC_CREATED;
    private String responseBody = "[]";

    @Override
    protected HttpResponse getResponse(HttpRequestBase httpRequest, Integer... expectedHttpStatus) {
      BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, responseStatus, "");
      response.setEntity(new StringEntity(responseBody, UTF_8));
      return response;
    }

    @Override
    protected void closeConnection(HttpResponse response) {
    }
  }

  @Test
  public void testAddMultipleEventsReturnsEventIdsAndPeriodsFromResponseBody() throws Exception {
    CannedResponseSchedulerServiceRemoteImpl service = new CannedResponseSchedulerServiceRemoteImpl();
    service.responseBody = "["
            + "{\"id\": \"event-1\", \"start\": \"2024-01-01T10:00:00Z\", \"end\": \"2024-01-01T11:00:00Z\"},"
            + "{\"id\": \"event-2\", \"start\": \"2024-01-02T10:00:00Z\", \"end\": \"2024-01-02T11:00:00Z\"}"
            + "]";

    Map<String, Period<ZonedDateTime>> scheduled = service.addMultipleEvents(
            new RRule<>("FREQ=DAILY;COUNT=2"),
            new Date(),
            new Date(),
            60000L,
            TimeZone.getDefault(),
            "Device A",
            Collections.<String>emptySet(),
            emptyMediaPackage(),
            Collections.<String, String>emptyMap(),
            Collections.<String, String>emptyMap(),
            java.util.Optional.empty());

    assertEquals(Set.of("event-1", "event-2"), scheduled.keySet());
    assertEquals(Instant.parse("2024-01-01T10:00:00Z"), scheduled.get("event-1").getStart().toInstant());
    assertEquals(Instant.parse("2024-01-01T11:00:00Z"), scheduled.get("event-1").getEnd().toInstant());
    assertEquals(Instant.parse("2024-01-02T10:00:00Z"), scheduled.get("event-2").getStart().toInstant());
    assertEquals(Instant.parse("2024-01-02T11:00:00Z"), scheduled.get("event-2").getEnd().toInstant());
  }

  @Test
  public void testAddMultipleEventsWithNoEventsReturnsEmptyMap() throws Exception {
    CannedResponseSchedulerServiceRemoteImpl service = new CannedResponseSchedulerServiceRemoteImpl();
    service.responseBody = "[]";

    Map<String, Period<ZonedDateTime>> scheduled = service.addMultipleEvents(
            new RRule<>("FREQ=DAILY;COUNT=2"),
            new Date(),
            new Date(),
            60000L,
            TimeZone.getDefault(),
            "Device A",
            Collections.<String>emptySet(),
            emptyMediaPackage(),
            Collections.<String, String>emptyMap(),
            Collections.<String, String>emptyMap(),
            java.util.Optional.empty());

    assertTrue(scheduled.isEmpty());
  }

  private MediaPackage emptyMediaPackage() throws Exception {
    return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
  }
}
