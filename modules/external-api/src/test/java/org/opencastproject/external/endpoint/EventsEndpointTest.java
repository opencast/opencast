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

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.external.common.ApiFormat;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.external.util.AclUtils;
import org.opencastproject.external.util.SchedulingUtils;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.impl.index.IndexObject;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.util.RequestUtils;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.test.rest.RestServiceTestEnv;
import org.opencastproject.util.MimeType;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class EventsEndpointTest {
  private static final Logger logger = LoggerFactory.getLogger(EventsEndpointTest.class);
  private Organization defaultOrg = new DefaultOrganization();
  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(), TestEventsEndpoint.class);

   @BeforeClass
   public static void oneTimeSetUp() {
     env.setUpServer();
   }

   @AfterClass
   public static void oneTimeTearDown() {
     env.tearDownServer();
   }

  private List<Publication> getExamplePublications() throws URISyntaxException {
    List<Publication> publications = new ArrayList<Publication>();
    Publication youtubePublication = PublicationImpl.publication("youtube-pub-id", "youtube",
            new URI("http://youtube.com/id"), MimeType.mimeType("you", "tube"));
    publications.add(youtubePublication);
    Publication internalPublication = PublicationImpl.publication("internal-pub-id",
            InternalPublicationChannel.CHANNEL_ID, new URI("http://internal.com/id"),
            MimeType.mimeType("in", "ternal"));
    publications.add(internalPublication);
    return publications;
  }

  @Ignore
  @Test
  public void testGetEventsJsonResponse() throws Exception {
    String eventJson = IOUtils.toString(getClass().getResource("/events.json"), UTF_8);
    String acceptHeader = "application/" + ApiVersion.CURRENT_VERSION.toExternalForm() + "+" + ApiFormat.JSON;
    List<IndexObject> events = new ArrayList<IndexObject>();
    List<String> contributors = new ArrayList<String>();
    contributors.add("Physics Department");

    List<Publication> publications = getExamplePublications();

    Event event1 = new Event("e6aeb8df-a852-46cd-8128-b89de696f20e", defaultOrg.getId());
    event1.setArchiveVersion(2L);
    event1.setCreated("2015-03-12T10:38:54Z");
    event1.setCreator("Opencast Administrator");
    event1.setContributors(contributors);
    event1.setDescription("Cooling without moving parts and using only heat as an input");
    event1.setDuration(7200000L);
    event1.setHasPreview(true);
    event1.setLocation("physics-e-01");
    List<String> presenters = new ArrayList<String>();
    presenters.add("Prof. A. Einstein");
    event1.setPresenters(presenters);
    event1.setPublications(publications);
    event1.setWorkflowState(WorkflowState.SUCCEEDED);
    event1.setRecordingStartDate("2015-03-20T04:00:00Z");
    event1.setSubject("Space Final Frontier");
    event1.setTitle("Einstein refrigerator");
    events.add(event1);

    Event event2 = new Event("f5aeb8df-a852-46cd-8128-b89de696f20e", defaultOrg.getId());
    event2.setArchiveVersion(5L);
    event2.setCreated("2015-03-12T10:38:54Z");
    event2.setCreator("Opencast Administrator");
    event2.setContributors(contributors);
    event2.setDescription("The history of the universe from the big bang to black holes");
    event2.setDuration(7200000L);
    event2.setHasPreview(true);
    event2.setLocation("physics-e-02");
    presenters = new ArrayList<String>();
    presenters.add("Prof. Stephen Hawking");
    event2.setPresenters(presenters);
    event2.setPublications(publications);
    event2.setWorkflowState(WorkflowState.SUCCEEDED);
    event2.setRecordingStartDate("2015-03-20T04:00:00Z");
    event2.setSubject("Space Final Frontier, Mathematics");
    event2.setTitle("The Theory of Everything");
    events.add(event2);

    EventsEndpoint endpoint = new EventsEndpoint();
    Response result = endpoint.getJsonEvents(acceptHeader, events, false, false, false,false, false, ApiVersion.VERSION_1_0_0);
    assertNotNull(result.getMetadata().get("Content-Type"));
    assertEquals("application/v1.0.0+json", result.getMetadata().get("Content-Type").get(0).toString().toLowerCase());
    assertThat(eventJson, SameJSONAs.sameJSONAs(result.getEntity().toString()).allowingAnyArrayOrdering());
  }

  @Ignore
  @Test
  public void testGetEventJsonResponse() throws Exception {
    String eventJson = IOUtils.toString(getClass().getResource("/event-single.json"), UTF_8);
    List<String> contributors = new ArrayList<String>();
    contributors.add("Physics Department");

    List<Publication> publications = getExamplePublications();

    Event event = new Event("e6aeb8df-a852-46cd-8128-b89de696f20e", defaultOrg.getId());
    event.setArchiveVersion(2L);
    event.setCreated("2015-03-12T10:38:54Z");
    event.setCreator("Opencast Administrator");
    event.setContributors(contributors);
    event.setDescription("Cooling without moving parts and using only heat as an input");
    event.setDuration(7200000L);
    event.setHasPreview(true);
    event.setLocation("physics-e-01");
    List<String> presenters = new ArrayList<String>();
    presenters.add("Prof. A. Einstein");
    event.setPresenters(presenters);
    event.setPublications(publications);
    event.setWorkflowState(WorkflowState.SUCCEEDED);
    event.setRecordingStartDate("2015-03-20T04:00:00Z");
    event.setSubject("Space Final Frontier, Chemistry");
    event.setTitle("Einstein refrigerator");

    EventsEndpoint endpoint = new EventsEndpoint();
    Response result = ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0,
            endpoint.eventToJSON(event, false, false, false,false, false, ApiVersion.VERSION_1_0_0));
    assertNotNull(result.getMetadata().get("Content-Type"));
    assertEquals("application/v1.0.0+json", result.getMetadata().get("Content-Type").get(0).toString().toLowerCase());
    assertThat(eventJson, SameJSONAs.sameJSONAs(result.getEntity().toString()).allowingAnyArrayOrdering());
  }

  @Ignore
  @Test
  public void testSerializationOfAcl() throws IOException {
    String emptyAclJson = IOUtils.toString(getClass().getResource("/acl-empty.json"), UTF_8);
    // Test empty acl
    AccessControlList acl = new AccessControlList();
    Event event = new Event();
    event.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
    Response result = ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, arr(AclUtils.serializeAclToJson(acl)));
    assertNotNull(result.getMetadata().get("Content-Type"));
    assertEquals("application/" + ApiVersion.CURRENT_VERSION + "+json",
            result.getMetadata().get("Content-Type").get(0).toString().toLowerCase());
    assertThat(emptyAclJson, SameJSONAs.sameJSONAs(result.getEntity().toString()).allowingAnyArrayOrdering());

    // Test acl with one entry
    String oneAclJson = IOUtils.toString(getClass().getResource("/acl-one.json"), UTF_8);
    AccessControlEntry ace = new AccessControlEntry("ROLE_ADMIN", "write", true);
    acl = new AccessControlList(ace);
    event = new Event();
    event.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
    result = ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, arr(AclUtils.serializeAclToJson(acl)));
    assertNotNull(result.getMetadata().get("Content-Type"));
    assertEquals("application/" + ApiVersion.CURRENT_VERSION + "+json",
            result.getMetadata().get("Content-Type").get(0).toString().toLowerCase());
    assertThat(oneAclJson, SameJSONAs.sameJSONAs(result.getEntity().toString()).allowingAnyArrayOrdering());

    // Test acl with many entries
    String manyAclJson = IOUtils.toString(getClass().getResource("/acl-many.json"), UTF_8);
    AccessControlEntry ace1 = new AccessControlEntry("ROLE_ADMIN", "write", true);
    AccessControlEntry ace2 = new AccessControlEntry("ROLE_USER", "read", true);
    acl = new AccessControlList(ace1, ace2);
    event = new Event();
    event.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
    result = ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, arr(AclUtils.serializeAclToJson(acl)));
    assertNotNull(result.getMetadata().get("Content-Type"));
    assertEquals("application/" + ApiVersion.CURRENT_VERSION + "+json",
            result.getMetadata().get("Content-Type").get(0).toString().toLowerCase());
    assertThat(manyAclJson, SameJSONAs.sameJSONAs(result.getEntity().toString()).allowingAnyArrayOrdering());
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

  /**
   *
   *
   * Testing Events Endpoints
   *
   *
   *
   */
  @Test
  @Ignore
  public void testPutEvent() {
    String result = "";
    String missingEvent = "/missing";
    result = given().log().all().expect().statusCode(SC_NOT_FOUND).when().put(env.host(missingEvent)).asString();
    logger.info("Result: " + result);
  }

  /**
   *
   * Testing Events Metadata Endpoints
   *
   */
  @Test
  @Ignore
  public void testDeleteEventMetadata() {
    String missingEvent = "/" + "missing" + "/metadata/" + TestEventsEndpoint.METADATA_CATALOG_TYPE;
    given().log().all().expect().statusCode(SC_NOT_FOUND).when().delete(env.host(missingEvent)).asString();

    String missingCatalog = "/" + TestEventsEndpoint.DELETE_EVENT_METADATA + "metadata/missing";
    given().log().all().expect().statusCode(SC_NOT_FOUND).when().delete(env.host(missingCatalog)).asString();

    String deleteForbiddenPath = "/" + TestEventsEndpoint.DELETE_EVENT_METADATA + "/metadata";
    given().log().all().queryParam("type", "dublincore/episode").expect().statusCode(SC_FORBIDDEN).when()
            .delete(env.host(deleteForbiddenPath)).asString();

    String deletePath = "/" + TestEventsEndpoint.DELETE_EVENT_METADATA + "/metadata/";
    given().log().all().queryParam("type", TestEventsEndpoint.DELETE_CATALOG_TYPE + "/episode").expect()
            .statusCode(SC_NO_CONTENT).when().delete(env.host(deletePath)).asString();

    String internalErrorPath = "/" + TestEventsEndpoint.DELETE_EVENT_METADATA + "/metadata/";
    given().log().all().queryParam("type", TestEventsEndpoint.INTERNAL_SERVER_ERROR_TYPE + "/episode").expect()
            .statusCode(SC_INTERNAL_SERVER_ERROR).when().delete(env.host(internalErrorPath)).asString();

    String notFoundPath = "/" + TestEventsEndpoint.DELETE_EVENT_METADATA + "/metadata/"
            + TestEventsEndpoint.NOT_FOUND_TYPE;
    given().log().all().expect().statusCode(SC_NOT_FOUND).when().delete(env.host(notFoundPath)).asString();

    String unauthorizedPath = "/" + TestEventsEndpoint.DELETE_EVENT_METADATA + "/metadata/";
    given().log().all().queryParam("type", TestEventsEndpoint.UNAUTHORIZED_TYPE + "/episode").expect()
            .statusCode(SC_UNAUTHORIZED).when().delete(env.host(unauthorizedPath)).asString();
  }

  /**
   *
   *
   * Testing Events Publication Endpoints
   *
   *
   *
   */
  @Ignore
  @Test
  public void testGetPublicationsEndpoint() throws IOException {
    String result = "";
    String expected = "";

    String missingEvent = "/missing/publications";
    given().log().all().expect().statusCode(SC_NOT_FOUND).when().get(env.host(missingEvent)).asString();

    String noPublications = "/" + TestEventsEndpoint.NO_PUBLICATIONS_EVENT + "/publications";
    expected = IOUtils.toString(getClass().getResource("/events/publications/publications-none.json"), UTF_8);
    result = given().log().all().expect().statusCode(SC_OK).when().get(env.host(noPublications)).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());

    String twoPublications = "/" + TestEventsEndpoint.TWO_PUBLICATIONS + "/publications";
    expected = IOUtils.toString(getClass().getResource("/events/publications/publications-two.json"), UTF_8);
    result = given().log().all().expect().statusCode(SC_OK).when().get(env.host(twoPublications)).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());
  }

  @Ignore
  @Test
  public void testGetPublicationEndpoint() throws IOException {
    String result = "";
    String expected = "";

    String missingEvent = "/missing/publications/missing";
    given().log().all().expect().statusCode(SC_NOT_FOUND).when().get(env.host(missingEvent)).asString();

    String noPublications = "/" + TestEventsEndpoint.NO_PUBLICATIONS_EVENT + "/publications/missing";
    expected = IOUtils.toString(getClass().getResource("/events/publications/publications-none.json"), UTF_8);
    given().log().all().expect().statusCode(SC_NOT_FOUND).when().get(env.host(noPublications)).asString();

    String engagePublication = "/" + TestEventsEndpoint.TWO_PUBLICATIONS + "/publications/"
            + TestEventsEndpoint.ENGAGE_PUBLICATION_ID;
    expected = IOUtils.toString(getClass().getResource("/events/publications/publication-engage.json"), UTF_8);
    result = given().log().all().expect().statusCode(SC_OK).when().get(env.host(engagePublication)).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());

    String oaipmhPublication = "/" + TestEventsEndpoint.TWO_PUBLICATIONS + "/publications/"
            + TestEventsEndpoint.OAIPMH_PUBLICATION_ID;
    expected = IOUtils.toString(getClass().getResource("/events/publications/publication-oaipmh.json"), UTF_8);
    result = given().log().all().expect().statusCode(SC_OK).when().get(env.host(oaipmhPublication)).asString();
    assertThat(expected, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());
  }

  @Test
  public void testUpdateEventMetadata() throws IOException {
    String jsonString = IOUtils.toString(getClass().getResource("/event-update.json"), UTF_8);
    String expectedJson = IOUtils.toString(getClass().getResource("/event-update-expected.json"), UTF_8);
    String eventId = TestEventsEndpoint.UPDATE_EVENT;
    given().multiPart("metadata", jsonString).pathParam("event_id", eventId).expect().statusCode(SC_NO_CONTENT)
            .when().post(env.host("{event_id}"));
    MetadataList actualMetadataList = TestEventsEndpoint.getCapturedMetadataList1().getValue();
    assertThat(actualMetadataList.toJSON().toString(), SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
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
    assertThat(actualMetadataList.getMetadataByFlavor("dublincore/episode").get().toJSON().toString(),
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
