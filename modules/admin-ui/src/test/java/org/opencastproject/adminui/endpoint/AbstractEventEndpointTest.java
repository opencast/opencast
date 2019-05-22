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

package org.opencastproject.adminui.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.impl.AdminUIConfiguration;
import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.RecordingState;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.test.rest.NotFoundExceptionMapper;
import org.opencastproject.test.rest.RestServiceTestEnv;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.easymock.EasyMock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;

import io.restassured.http.ContentType;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class AbstractEventEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestEventEndpoint.class,
          NotFoundExceptionMapper.class);

  public static TestEnv testEnv() {
    return new TestEnv();
  }

  @Test
  public void testGetEvent() throws Exception {
    given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK)
            // TODO: add all serialised props to mock and check here
            .body("id", equalTo("asdasd")).body("title", equalTo("title"))
            .body("event_status", equalTo("EVENTS.EVENTS.STATUS.ARCHIVE")).body("has_preview", equalTo(false))
            .body("has_open_comments", equalTo(false))
            .body("series.id", equalTo("seriesId")).body("technical_start", equalTo("2013-03-20T04:00:00Z"))
            .body("start_date", equalTo("2013-03-20T04:00:00Z")).when().get(rt.host("/{eventId}"));

  }

  @Test
  public void testGetEventPublicationsTab() throws Exception {
    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/publications.json"));

    String eventString = IOUtils.toString(getClass().getResource("/eventPublications.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/publications.json")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventComments() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventComments.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/comments")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventComment() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventComment.json"));

    // wrong event id
    given().pathParam("eventId", "notExists").pathParam("commentId", 33).expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host("{eventId}/comment/{commentId}"));

    // not existing comment
    given().pathParam("eventId", "asdasd").pathParam("commentId", 99999).expect()
            .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).when().get(rt.host("{eventId}/comment/{commentId}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/comment/{commentId}")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testUpdateEventComment() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventComment.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).expect()
            .statusCode(HttpStatus.SC_OK).when().put(rt.host("{eventId}/comment/{commentId}")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testCreateEventComment() throws Exception {

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .post(rt.host("{eventId}/comment"));

    given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("{eventId}/comment"));

    given().pathParam("eventId", "asdasd").formParam("text", "Comment 2").given().formParam("reason", "Defect").expect()
            .header("Location", "http://localhost:8080/asdasd/comment/65").statusCode(HttpStatus.SC_CREATED)
            .body("reason", equalTo("Defect")).body("id", equalTo(65)).body("text", equalTo("Comment 2")).when()
            .post(rt.host("{eventId}/comment"));

  }

  @Test
  public void testResolveEventComment() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventCommentResolved.json"));

    given().pathParam("eventId", "notExists").pathParam("commentId", 33).expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().post(rt.host("{eventId}/comment/{commentId}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).expect()
            .statusCode(HttpStatus.SC_OK).when().post(rt.host("{eventId}/comment/{commentId}")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testDeleteEventComment() throws Exception {
    given().pathParam("eventId", "notExists").pathParam("commentId", 33).expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().delete(rt.host("{eventId}/comment/{commentId}"));

    given().pathParam("eventId", "asdasd").pathParam("commentId", 33).expect().statusCode(HttpStatus.SC_NO_CONTENT)
            .when().delete(rt.host("{eventId}/comment/{commentId}"));
  }

  @Test
  public void testDeleteEventCommentReply() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventCommentNoReply.json"));

    given().pathParam("eventId", "asdasd").pathParam("commentId", 33).pathParam("replyId", 77).expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().delete(rt.host("{eventId}/comment/{commentId}/{replyId}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).pathParam("replyId", 78).expect()
            .statusCode(HttpStatus.SC_OK).when().delete(rt.host("{eventId}/comment/{commentId}/{replyId}")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testUpdateEventCommentReply() throws Exception {
    given().pathParam("eventId", "asdasd").pathParam("commentId", 33).pathParam("replyId", 78).expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().put(rt.host("{eventId}/comment/{commentId}/{replyId}"));

    given().pathParam("eventId", "asdasd").pathParam("commentId", 33).pathParam("replyId", 77).formParam("text", "Text")
            .expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .put(rt.host("{eventId}/comment/{commentId}/{replyId}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).pathParam("replyId", 78)
            .formParam("text", "Text").expect().statusCode(HttpStatus.SC_OK).when()
            .put(rt.host("{eventId}/comment/{commentId}/{replyId}")).asString();

    JSONObject parse = (JSONObject) new JSONParser().parse(result);
    JSONArray replies = (JSONArray) parse.get("replies");
    JSONObject reply = (JSONObject) replies.get(0);
    Assert.assertEquals("Text", reply.get("text"));
  }

  @Test
  public void testCreateEventCommentReply() throws Exception {
    given().pathParam("eventId", "asdasd").pathParam("commentId", 33).expect().statusCode(HttpStatus.SC_BAD_REQUEST)
            .when().post(rt.host("{eventId}/comment/{commentId}/reply"));

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).formParam("text", "Text")
            .formParam("resolved", true).expect().statusCode(HttpStatus.SC_OK).when()
            .post(rt.host("{eventId}/comment/{commentId}/reply")).asString();

    JSONObject parse = (JSONObject) new JSONParser().parse(result);
    JSONArray replies = (JSONArray) parse.get("replies");
    JSONObject reply = (JSONObject) replies.get(1);
    Assert.assertEquals("Text", reply.get("text"));
    Assert.assertEquals(true, parse.get("resolvedStatus"));
  }

  @Test
  public void testGetEventMetadata() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventMetadata.json"));

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/metadata.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/metadata.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testUpdateEventMetadata() throws Exception {
    String metadataJson = IOUtils.toString(getClass().getResource("/eventMetadata.json"));

    given().pathParam("eventId", "notExists").formParam("metadata", "metadata").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().put(rt.host("{eventId}/metadata"));

    given().pathParam("eventId", "asdasd").formParam("metadata", metadataJson).expect().statusCode(HttpStatus.SC_OK)
            .when().put(rt.host("{eventId}/metadata"));
  }

  @Test
  public void testGetEventScheduling() throws Exception {
    String eventSchedulingString = IOUtils.toString(getClass().getResource("/eventScheduling.json"));

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/scheduling.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/scheduling.json")).asString();

    assertThat(eventSchedulingString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventSchedulingBulk() throws Exception {
    final String eventSchedulingBulkString = IOUtils
      .toString(getClass().getResource("/eventSchedulingBulk.json"), StandardCharsets.UTF_8);

    // Event that does not exist, and we are not ignoring that fact.
    given().formParam("eventIds", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
      .post(rt.host("scheduling.json"));

    // Event that does not exist, and we are ignoring that.
    given().formParam("eventIds", "notExists").formParam("ignoreNonScheduled", "true").expect().statusCode(HttpStatus.SC_OK).when()
      .post(rt.host("scheduling.json"));

    // Check if the actual result is what we expect.
    final String result = given().formParam("eventIds", "exists").expect().statusCode(HttpStatus.SC_OK).when()
      .post(rt.host("scheduling.json")).asString();

    assertThat(eventSchedulingBulkString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetAssetList() throws Exception {
    String assetString = IOUtils.toString(getClass().getResource("/assets.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/assets.json")).asString();

    assertThat(assetString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetMediaList() throws Exception {
    String expected = IOUtils.toString(getClass().getResource("/eventMedia.json"));

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/asset/media/media.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/media/media.json")).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetMedia() throws Exception {
    String expected = IOUtils.toString(getClass().getResource("/eventMediaDetail.json"));

    given().pathParam("eventId", "asdasd").pathParam("id", "notFound").expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host("{eventId}/asset/media/{id}.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("id", "publish-track-1").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/asset/media/{id}.json")).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetEventCatalogs() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventCatalogs.json"));

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/asset/catalog/catalogs.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/catalog/catalogs.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetCatalog() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventCatalog.json"));

    given().pathParam("eventId", "notExists").pathParam("id", "publish-catalog-1").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host("{eventId}/asset/catalog/{id}.json"));

    given().pathParam("eventId", "asdasd").pathParam("id", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host("{eventId}/asset/catalog/{id}.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("id", "publish-catalog-1").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/asset/catalog/{id}.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetAttachmentsList() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventAttachments.json"));

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/asset/attachment/attachments.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/attachment/attachments.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetAttachment() throws Exception {
    String expected = IOUtils.toString(getClass().getResource("/eventAttachmentSingle.json"));

    given().pathParam("eventId", "notExists").pathParam("id", "publish-attachment-2").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host("{eventId}/asset/attachment/{id}.json"));

    given().pathParam("eventId", "notExists").pathParam("id", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host("{eventId}/asset/attachment/{id}.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("id", "publish-attachment-2").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/asset/attachment/{id}.json")).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventWorkflows() throws Exception {

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/workflows.json")).asString();

    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventWorkflows.json"));

    String result = given().pathParam("eventId", "workflowid").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/workflows.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventWorkflow() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventWorkflow.json"));

    String path = "{eventId}/workflows/{workflowId}";
    given().pathParam("eventId", "notExists").pathParam("workflowId", "asdasd").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host(path));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asdasd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host(path));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "9999").expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host(path));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", "1").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host(path)).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventOperations() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventOperations.json"));

    String path = "{eventId}/workflows/{workflowId}/operations.json";
    given().pathParam("eventId", "notExists").pathParam("workflowId", "notExists").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host(path));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "xxxx").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host(path));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "9999").expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host(path));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", "1").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host(path)).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventOperation() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventOperation.json"));

    String path = "{eventId}/workflows/{workflowId}/operations/{operationPosition}";

    given().pathParam("eventId", "notExists").pathParam("workflowId", "notExists").pathParam("operationPosition", "1")
            .expect().statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host(path));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "xxxx").pathParam("operationPosition", "1").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host(path));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "9999").pathParam("operationPosition", "1").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host(path));

    // found but invalid operation position - its null
    given().pathParam("eventId", "asdasd").pathParam("workflowId", "9999").pathParam("operationPosition", "99").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host(path));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", "1")
            .pathParam("operationPosition", "1").expect().statusCode(HttpStatus.SC_OK).when().get(rt.host(path))
            .asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventErrors() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventErrors.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asd").expect().statusCode(HttpStatus.SC_BAD_REQUEST)
            .when().get(rt.host("{eventId}/workflows/{workflowId}/errors.json"));

    given().pathParam("eventId", "notExists").pathParam("workflowId", 3).expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host("{eventId}/workflows/{workflowId}/errors.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/workflows/{workflowId}/errors.json"))
            .asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  @Ignore
  public void testGetEventError() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventError.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asd").pathParam("errorId", "asd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("{eventId}/workflows/{workflowId}/errors/{errorId}.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).pathParam("errorId", "asd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("{eventId}/workflows/{workflowId}/errors/{errorId}.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).pathParam("errorId", 1).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/workflows/{workflowId}/errors/{errorId}.json"))
            .asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventAccessInformation() throws Exception {
    String eventAccessJson = IOUtils.toString(getClass().getResource("/eventAccess.json"));

    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/access.json"));

    JSONObject result = (JSONObject) new JSONParser().parse(given().pathParam("eventId", "asdasd").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/access.json")).asString());

    // Fix ordering for embedded acl json string
    String expectedAclString = getAclString(eventAccessJson);
    String aclString = getAclString(result.toJSONString());
    assertThat(expectedAclString, SameJSONAs.sameJSONAs(aclString));

    JSONObject episodeAccess = (JSONObject) result.get("episode_access");
    episodeAccess.replace("acl", expectedAclString);

    assertThat(eventAccessJson, SameJSONAs.sameJSONAs(result.toJSONString()));
  }

  private String getAclString(String accessJsonString) throws ParseException {
    JSONObject accessJson = (JSONObject) new JSONParser().parse(accessJsonString);
    JSONObject episodeAccess = (JSONObject) accessJson.get("episode_access");
    return (String) episodeAccess.get("acl");
  }

  @Test
  @Ignore
  public void testGetNewMetadata() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/newEventMetadata.json"));

    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("new/metadata")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetNewProcessing() throws Exception {
    String eventProcessingString = IOUtils.toString(getClass().getResource("/newEventProcessing.json"));

    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("new/processing")).asString();

    assertThat(result, SameJSONAs.sameJSONAs(eventProcessingString));

    eventProcessingString = IOUtils.toString(getClass().getResource("/newEventProcessing2.json"));

    result = given().queryParam("tags", "test,upload").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("new/processing")).asString();

    assertThat(result, SameJSONAs.sameJSONAs(eventProcessingString));
  }

  @Test
  public void testApplyAclToEvent() throws Exception {
    // post nothing
    given().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("notExists/access"));

    // invalid acl format
    given().formParam("acl", "INVALID").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("asdasd/access"));

    // post an acl update
    String acl = "{\"acl\":{\"ace\":[{\"allow\":true,\"role\":\"ROLE_ADMIN\",\"action\":\"read\"},{\"allow\":true,\"role\":\"ROLE_ADMIN\",\"action\":\"write\"}]}}";
    given().formParam("acl", acl).expect().statusCode(HttpStatus.SC_OK).when().post(rt.host("asdasd/access"));

    // post an acl update for an scheduled event
    given().formParam("acl", acl).expect().statusCode(HttpStatus.SC_OK).when().post(rt.host("asdasd/access"));

    // post an acl update for an archived event
    given().formParam("acl", acl).expect().statusCode(HttpStatus.SC_OK).when().post(rt.host("archivedid/access"));

    // post an acl update for an workflow event - conflict
    given().formParam("acl", acl).expect().statusCode(HttpStatus.SC_CONFLICT).when()
            .post(rt.host("workflowid/access"));
  }

  @Test
  public void testGetNewConflicts() throws Exception {
    given().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("new/conflicts"));
    given().formParam("metadata", "asdt").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("new/conflicts"));

    String expected = IOUtils.toString(getClass().getResource("/conflicts.json"));
    String metadataString = IOUtils.toString(getClass().getResource("/conflictRequest.json"));

    String result = given().formParam("metadata", metadataString).expect().statusCode(HttpStatus.SC_CONFLICT).when()
            .post(rt.host("new/conflicts")).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result));

  }

  @Test
  @Ignore
  public void testCreateNewTask() throws Exception {
    given().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("task"));
    given().formParam("metadata", "asdt").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("task"));

    String metadataString = IOUtils.toString(getClass().getResource("/createTasksRequest.json"));

    given().formParam("metadata", metadataString).expect().statusCode(HttpStatus.SC_CREATED).when()
            .post(rt.host("task"));
  }

  @Test
  public void testCreateNewEvent() throws Exception {
    // no multipart
    given().expect().statusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE).when().post(rt.host("new"));

    // TODO: finish this test
    given().multiPart("metadata", "some content").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("new"));
  }

  @Test
  public void testGetEvents() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/events.json"));

    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("events.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  private Recording createRecording(String id, long checkin, String state) {
    Recording recording = EasyMock.createMock(Recording.class);
    EasyMock.expect(recording.getID()).andStubReturn(id);
    EasyMock.expect(recording.getLastCheckinTime()).andStubReturn(checkin);
    EasyMock.expect(recording.getState()).andStubReturn(state);
    EasyMock.replay(recording);
    return recording;
  }

  @Test
  public void testRecordingToJson() throws WebApplicationException, IOException {
    String id = "rec-id";
    // 09/17/2015 @ 8:46pm UTC
    long lastCheckinTime = 1442522772000L;
    Recording recording = createRecording(id, lastCheckinTime, RecordingState.CAPTURING);
    String result = RestUtils.getJsonString(AbstractEventEndpoint.recordingToJson.apply(Opt.some(recording)));
    String expected = "{\"lastCheckInTimeUTC\":\"2015-09-17T20:46:12Z\",\"id\":\"rec-id\",\"state\":\"capturing\",\"lastCheckInTime\":1442522772000}";
    assertThat(expected, SameJSONAs.sameJSONAs(result));

    recording = createRecording(null, 0L, null);
    result = RestUtils.getJsonString(AbstractEventEndpoint.recordingToJson.apply(Opt.some(recording)));
    expected = "{\"lastCheckInTimeUTC\":\"1970-01-01T00:00:00Z\",\"id\":\"\",\"state\":\"\",\"lastCheckInTime\":0}";
    assertThat(expected, SameJSONAs.sameJSONAs(result));

    result = RestUtils.getJsonString(AbstractEventEndpoint.recordingToJson.apply(Opt.<Recording> none()));
    expected = "{}";
    assertThat(expected, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetCatalogAdapters() throws Exception {
    given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("catalogAdapters")).then()
            .body("title", hasItems("name 1", "name 2")).contentType(ContentType.JSON).body("", hasSize(2));
  }

  @Test
  public void testGetPublication() throws Exception {
    String expected = IOUtils.toString(getClass().getResource("/publication.json"));

    given().pathParam("eventId", "notExists").pathParam("id", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host("{eventId}/asset/publication/{id}.json"));

    given().pathParam("eventId", "asdasd").pathParam("id", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND)
            .when().get(rt.host("{eventId}/asset/publication/{id}.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("id", "presentation-1").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/asset/publication/{id}.json")).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetPublicationList() throws Exception {
    given().pathParam("eventId", "notExists").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("{eventId}/asset/publication/publications.json"));

    String expected = IOUtils.toString(getClass().getResource("/publications.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
      .get(rt.host("{eventId}/asset/publication/publications.json")).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result));

  }

  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

  public static final class TestEnv {
    private AdminUIConfiguration adminUIConfiguration;
    private WorkflowService workflowService;
    private AssetManager assetManager;
    private JobEndpoint jobService;
    private SeriesEndpoint seriesService;
    private AclService aclService;
    private EventCommentService eventCommentService;
    private SecurityService securityService;
    private IndexService indexService;
    private AuthorizationService authorizationService;
    private SchedulerService schedulerService;
    private CaptureAgentStateService captureAgentStateService;
    private AdminUISearchIndex index;
    private UrlSigningService urlSigningService;

    public WorkflowService getWorkflowService() {
      return workflowService;
    }

    public void setJobService(JobEndpoint jobService) {
      this.jobService = jobService;
    }

    public JobEndpoint getJobService() {
      return jobService;
    }

    public void setSeriesService(SeriesEndpoint seriesService) {
      this.seriesService = seriesService;
    }

    public SeriesEndpoint getSeriesService() {
      return seriesService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
      this.workflowService = workflowService;
    }

    public AssetManager getAssetManager() {
      return assetManager;
    }

    public void setAssetManager(AssetManager assetManager) {
      this.assetManager = assetManager;
    }

    public AclService getAclService() {
      return aclService;
    }

    public void setAclService(AclService aclService) {
      this.aclService = aclService;
    }

    public EventCommentService getEventCommentService() {
      return eventCommentService;
    }

    public void setEventCommentService(EventCommentService eventCommentService) {
      this.eventCommentService = eventCommentService;
    }

    public SecurityService getSecurityService() {
      return securityService;
    }

    public void setSecurityService(SecurityService securityService) {
      this.securityService = securityService;
    }

    public IndexService getIndexService() {
      return indexService;
    }

    public void setIndexService(IndexService indexService) {
      this.indexService = indexService;
    }

    public AuthorizationService getAuthorizationService() {
      return authorizationService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
      this.authorizationService = authorizationService;
    }

    public SchedulerService getSchedulerService() {
      return schedulerService;
    }

    public void setSchedulerService(SchedulerService schedulerService) {
      this.schedulerService = schedulerService;
    }

    public CaptureAgentStateService getCaptureAgentStateService() {
      return captureAgentStateService;
    }

    public void setCaptureAgentStateService(CaptureAgentStateService captureAgentStateService) {
      this.captureAgentStateService = captureAgentStateService;
    }

    public void setIndex(AdminUISearchIndex index) {
      this.index = index;
    }

    public AdminUISearchIndex getIndex() {
      return index;
    }

    public void setAdminUIConfiguration(AdminUIConfiguration adminUIConfiguration) {
      this.adminUIConfiguration = adminUIConfiguration;
    }

    public AdminUIConfiguration getAdminUIConfiguration() {
      return adminUIConfiguration;
    }

    public void setUrlSigningService(UrlSigningService urlSigningService) {
      this.urlSigningService = urlSigningService;
    }

    public UrlSigningService getUrlSigningService() {
      return urlSigningService;
    }

  }
}
