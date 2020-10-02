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
package org.opencastproject.external.endpoint;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.external.util.AclUtils;
import org.opencastproject.external.util.SchedulingUtils;
import org.opencastproject.index.service.util.RequestUtils;
import org.opencastproject.metadata.dublincore.MetadataJson;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.test.rest.RestServiceTestEnv;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class EventsEndpointTest {
  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(), TestEventsEndpoint.class);

   @BeforeClass
   public static void oneTimeSetUp() {
     env.setUpServer();
   }

   @AfterClass
   public static void oneTimeTearDown() {
     env.tearDownServer();
   }

  @Test
  public void testDeserializationOfAcl() throws IOException, ParseException {
    String emptyAclJson = IOUtils.toString(getClass().getResource("/acl-empty.json"), UTF_8);
    AccessControlList acl = AclUtils.deserializeJsonToAcl(emptyAclJson, false);
    assertEquals(acl.getEntries().size(), 0);

    // Test acl with one entry
    String oneAclJson = IOUtils.toString(getClass().getResource("/acl-one.json"), UTF_8);
    acl = AclUtils.deserializeJsonToAcl(oneAclJson, false);
    assertEquals(acl.getEntries().size(), 1);
    assertEquals(acl.getEntries().get(0).getAction(), "write");
    assertTrue(acl.getEntries().get(0).isAllow());
    assertEquals(acl.getEntries().get(0).getRole(), "ROLE_ADMIN");

    // Test acl with many entries
    String manyAclJson = IOUtils.toString(getClass().getResource("/acl-many.json"), UTF_8);
    acl = AclUtils.deserializeJsonToAcl(manyAclJson, false);
    assertEquals(acl.getEntries().size(), 2);
    assertEquals(acl.getEntries().get(0).getAction(), "write");
    assertTrue(acl.getEntries().get(0).isAllow());
    assertEquals(acl.getEntries().get(0).getRole(), "ROLE_ADMIN");

    assertEquals(acl.getEntries().get(1).getAction(), "read");
    assertTrue(acl.getEntries().get(1).isAllow());
    assertEquals(acl.getEntries().get(1).getRole(), "ROLE_USER");
  }

  @Test
  public void testDeserializeMetadataFields() throws IOException, ParseException {
    String manyAclJson = IOUtils.toString(getClass().getResource("/event-metadata.json"), UTF_8);
    Map<String, String> fields = RequestUtils.getKeyValueMap(manyAclJson);
    assertEquals(3, fields.size());

    assertEquals("Captivating title - edited", fields.get("title"));
    assertEquals("What this is about - edited", fields.get("subject"));
    assertEquals("A great description - edited", fields.get("description"));

    String updateMetadataJson = IOUtils.toString(getClass().getResource("/event-metadata-update.json"), UTF_8);
    fields = RequestUtils.getKeyValueMap(updateMetadataJson);
    assertEquals(5, fields.size());
    assertEquals("Captivating title - edited", fields.get("title"));
    assertEquals("What this is about - edited", fields.get("subject"));
    assertEquals("", fields.get("description"));

    String updateInvalidMetadataJson = IOUtils.toString(getClass().getResource("/event-metadata-update-invalid.json")
            , UTF_8);
    try {
      fields = RequestUtils.getKeyValueMap(updateInvalidMetadataJson);
      fail();
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testUpdateEventMetadata() throws IOException {
    String jsonString = IOUtils.toString(getClass().getResource("/event-update.json"), UTF_8);
    String expectedJson = IOUtils.toString(getClass().getResource("/event-update-expected.json"), UTF_8);
    String eventId = TestEventsEndpoint.UPDATE_EVENT;
    given().multiPart("metadata", jsonString).pathParam("event_id", eventId).expect().statusCode(SC_NO_CONTENT)
            .when().post(env.host("{event_id}"));
    MetadataList actualMetadataList = TestEventsEndpoint.getCapturedMetadataList1().getValue();
    assertThat(MetadataJson.listToJson(actualMetadataList, true).toString(), SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetAllEventMetadata() throws IOException {
    String expectedJson = IOUtils.toString(getClass().getResource("/event-metadata-expected.json"), UTF_8);
    String eventId = TestEventsEndpoint.METADATA_GET_EVENT;
    String result = given().pathParam("event_id", eventId).expect().statusCode(SC_OK).when().get(env.host("{event_id}/metadata")).asString();
    assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  @Test
  public void testUpdateEventMetadataByType() throws IOException {
    String jsonString = IOUtils.toString(getClass().getResource("/event-metadata-update.json"), UTF_8);
    String expectedJson = IOUtils.toString(getClass().getResource("/event-metadata-update-expected.json"), UTF_8);
    String eventId = TestEventsEndpoint.METADATA_UPDATE_EVENT;
    given().formParam("metadata", jsonString).pathParam("event_id", eventId).queryParam("type", "dublincore/episode")
            .expect().statusCode(SC_NO_CONTENT).when().put(env.host("{event_id}/metadata"));
    MetadataList actualMetadataList = TestEventsEndpoint.getCapturedMetadataList2().getValue();
    assertThat(MetadataJson.collectionToJson(actualMetadataList.getMetadataByFlavor("dublincore/episode"), true).toString(),
            SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  /**
   * Test GET /{event_id}/scheduling.
   */
  @Test
  public void testGetEventScheduling() throws IOException {
    String expectedJson = IOUtils.toString(getClass().getResource("/event-scheduling-expected.json"), UTF_8);
    String eventId = TestEventsEndpoint.SCHEDULING_GET_EVENT;
    String result = given().pathParam("event_id", eventId).expect().statusCode(SC_OK).when()
        .get(env.host("{event_id}/scheduling")).asString();
    assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  /**
   * Test PUT /{event_id}/scheduling.
   */
  @Test
  public void testUpdateEventScheduling() throws IOException {
    String jsonString = IOUtils.toString(getClass().getResource("/event-scheduling-update.json"), UTF_8);
    String eventId = TestEventsEndpoint.SCHEDULING_UPDATE_EVENT;
    given().formParam("scheduling", jsonString).pathParam("event_id", eventId)
        .expect().statusCode(SC_NO_CONTENT).when().put(env.host("{event_id}/scheduling"));

    SchedulingUtils.SchedulingInfo schedulingInfo = new SchedulingUtils.SchedulingInfo();
    schedulingInfo.setAgentId(TestEventsEndpoint.getCapturedAgentId().getValue());
    schedulingInfo.setStartDate(TestEventsEndpoint.getCapturedStartDate().getValue());
    schedulingInfo.setEndDate(TestEventsEndpoint.getCapturedEndDate().getValue());
    if (TestEventsEndpoint.getCapturedAgentConfig().getValue().isSome()) {
      schedulingInfo.setInputs(Opt.some(TestEventsEndpoint.getCapturedAgentConfig()
          .getValue().get().get(CaptureParameters.CAPTURE_DEVICE_NAMES)));
    }
    assertThat(schedulingInfo.toJson().toString(), SameJSONAs.sameJSONAs(jsonString).allowingAnyArrayOrdering());
  }

}
