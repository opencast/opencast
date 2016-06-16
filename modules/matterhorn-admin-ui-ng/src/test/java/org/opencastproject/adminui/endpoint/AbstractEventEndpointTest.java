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

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertThat;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.impl.AdminUIConfiguration;
import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.rest.NotFoundExceptionMapper;
import org.opencastproject.rest.RestServiceTestEnv;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.easymock.EasyMock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

// TODO re-ignore tests
@Ignore
public class AbstractEventEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestEventEndpoint.class,
          NotFoundExceptionMapper.class);

  public static TestEnv testEnv() {
    return new TestEnv();
  }

  @Test
  public void testGetEvent() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/event.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{eventId}")).asString();
    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventsRecordingsAndRecipients() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/recordingsAndRecipients.json"));

    String result = given().queryParam("eventIds", "1").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/events/sendmessage")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetRecordingMessages() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventMessages.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/messages")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventGeneralTab() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventGeneral.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/general")).asString();

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

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/comment/{commentId}")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventParticipation() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventParticipation.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/participation")).asString();

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
    String eventString = IOUtils.toString(getClass().getResource("/eventComment.json"));

    given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("{eventId}/comment"));

    String result = given().pathParam("eventId", "asdasd").formParam("text", "Test").expect()
            .header("Location", "http://localhost:8080/asdasd/comment/65").statusCode(HttpStatus.SC_CREATED).when()
            .post(rt.host("{eventId}/comment")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testResolveEventComment() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/eventComment.json"));

    String result = given().pathParam("eventId", "asdasd").pathParam("commentId", 33).expect()
            .statusCode(HttpStatus.SC_OK).when().post(rt.host("{eventId}/comment/{commentId}")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testDeleteEventComment() throws Exception {
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

    given().pathParam("eventId", "asdasd").pathParam("commentId", 33).pathParam("replyId", 77)
            .formParam("text", "Text").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
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
    Assert.assertEquals("true", reply.get("resolvedStatus"));
  }

  @Test
  public void testGetEventMetadata() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventMetadata.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/metadata")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testUpdateEventMetadata() throws Exception {
    String metadataJson = IOUtils.toString(getClass().getResource("/eventMetadata.json"));

    given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .put(rt.host("{eventId}/metadata"));

    given().pathParam("eventId", "asdasd").formParam("metadata", "adfasdf").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().put(rt.host("{eventId}/metadata"));

    given().pathParam("eventId", "asdasd").formParam("metadata", metadataJson).expect().statusCode(HttpStatus.SC_OK)
            .when().put(rt.host("{eventId}/metadata"));
  }

  @Test
  public void testGetAssetList() throws Exception {
    String assetString = IOUtils.toString(getClass().getResource("/assets.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/assets.json")).asString();

    assertThat(assetString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventMedia() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventMedia.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/media/media.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventCatalogs() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventCatalogs.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/catalog/catalogs.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventAttachements() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventAttachments.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/asset/attachment/attachments.json")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventWorkflows() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventWorkflows.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/workflows")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventWorkflow() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventWorkflow.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asdasd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("{eventId}/workflows/{workflowId}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", 23).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/workflows/{workflowId}")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventOperations() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventOperations.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asdasd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("{eventId}/workflows/{workflowId}/operations"));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", 23).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/workflows/{workflowId}/operations")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventOperation() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventOperation.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asdasd").pathParam("operationPosition", "asdasd")
            .expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("{eventId}/workflows/{workflowId}/operations/{operationPosition}"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).pathParam("operationPosition", "asdasd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("{eventId}/workflows/{workflowId}/operations/{operationPosition}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).pathParam("operationPosition", 3)
            .expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/workflows/{workflowId}/operations/{operationPosition}")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventErrors() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventErrors.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("{eventId}/workflows/{workflowId}/errors"));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/workflows/{workflowId}/errors")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventError() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventError.json"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", "asd").pathParam("errorId", "asd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("{eventId}/workflows/{workflowId}/errors/{errorId}"));

    given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).pathParam("errorId", "asd").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("{eventId}/workflows/{workflowId}/errors/{errorId}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("workflowId", 3).pathParam("errorId", 1).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/workflows/{workflowId}/errors/{errorId}"))
            .asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventAccessInformation() throws Exception {
    String eventAccessJson = IOUtils.toString(getClass().getResource("/eventAccess.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/access")).asString();

    assertThat(eventAccessJson, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testAddEventTransition() throws Exception {
    given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("{eventId}/transitions"));

    given().pathParam("eventId", "asdasd").formParam("transition", "adsf").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("{eventId}/transitions"));

    String transition = "{\"id\": 1,\"application_date\": \"2014-06-05T15:00:00Z\", \"done\": false, \"acl_id\": 43, \"is_deleted\": false }";

    given().pathParam("eventId", "asdasd").formParam("transition", transition).expect()
            .statusCode(HttpStatus.SC_NO_CONTENT).when().post(rt.host("{eventId}/transitions"));
  }

  @Test
  public void testUpdateEventTransition() throws Exception {
    given().pathParam("eventId", "asdasd").pathParam("transitionId", "adf").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().put(rt.host("{eventId}/transitions/{transitionId}"));
    given().pathParam("eventId", "asdasd").pathParam("transitionId", 5).expect().statusCode(HttpStatus.SC_BAD_REQUEST)
            .when().put(rt.host("{eventId}/transitions/{transitionId}"));
    given().pathParam("eventId", "asdasd").pathParam("transitionId", 5).formParam("transition", "adsf").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().put(rt.host("{eventId}/transitions/{transitionId}"));

    String transition = "{\"id\": 1,\"application_date\": \"2014-06-05T15:00:00Z\", \"done\": false, \"acl_id\": 43, \"is_deleted\": false }";
    given().pathParam("eventId", "asdasd").pathParam("transitionId", 5).formParam("transition", transition).expect()
            .statusCode(HttpStatus.SC_NO_CONTENT).when().put(rt.host("{eventId}/transitions/{transitionId}"));
  }

  @Test
  public void testDeleteEventTransition() throws Exception {
    given().pathParam("eventId", "asdasd").pathParam("transitionId", "adf").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().delete(rt.host("{eventId}/transitions/{transitionId}"));
    given().pathParam("eventId", "asdasd").pathParam("transitionId", 5).expect().statusCode(HttpStatus.SC_NO_CONTENT)
            .when().delete(rt.host("{eventId}/transitions/{transitionId}"));
  }

  @Test
  public void testGetNewMetadata() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/newEventMetadata.json"));

    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("new/metadata")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetNewProcessing() throws Exception {
    String eventProcessingString = IOUtils.toString(getClass().getResource("/newEventProcessing.json"));

    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("new/processing")).asString();

    assertThat(eventProcessingString, SameJSONAs.sameJSONAs(result));

    eventProcessingString = IOUtils.toString(getClass().getResource("/newEventProcessing2.json"));

    result = given().queryParam("tags", "test,upload").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("new/processing")).asString();

    assertThat(eventProcessingString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetNewAccess() throws Exception {
    String eventAccessString = IOUtils.toString(getClass().getResource("/newEventAccess.json"));

    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("new/access")).asString();

    assertThat(eventAccessString, SameJSONAs.sameJSONAs(result));
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

    expected = IOUtils.toString(getClass().getResource("/conflicts2.json"));
    metadataString = IOUtils.toString(getClass().getResource("/conflictRequest2.json"));

    result = given().formParam("metadata", metadataString).expect().statusCode(HttpStatus.SC_CONFLICT).when()
            .post(rt.host("new/conflicts")).asString();

    assertThat(expected, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testCreateNewTask() throws Exception {
    given().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("task"));
    given().formParam("metadata", "asdt").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("task"));

    String metadataString = IOUtils.toString(getClass().getResource("/createTasksRequest.json"));

    given().formParam("metadata", metadataString).expect().log().all().statusCode(HttpStatus.SC_CREATED).when()
            .post(rt.host("task"));
  }

  // TODO test create event

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
    String result = RestUtils.getJsonString(AbstractEventEndpoint.recordingToJson.ap(Opt.some(recording)));
    String expected = "{\"lastCheckInTimeUTC\":\"2015-09-17T20:46:12Z\",\"id\":\"rec-id\",\"state\":\"capturing\",\"lastCheckInTime\":\"1442522772000\"}";
    assertThat(expected, SameJSONAs.sameJSONAs(result));

    recording = createRecording(null, 0L, null);
    result = RestUtils.getJsonString(AbstractEventEndpoint.recordingToJson.ap(Opt.some(recording)));
    expected = "{\"lastCheckInTimeUTC\":\"1970-01-01T00:00:00Z\",\"id\":\"\",\"state\":\"\",\"lastCheckInTime\":\"0\"}";
    assertThat(expected, SameJSONAs.sameJSONAs(result));

    result = RestUtils.getJsonString(AbstractEventEndpoint.recordingToJson.ap(Opt.<Recording> none()));
    expected = "{}";
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
    private OpencastArchive archive;
    private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
    private JobEndpoint jobService;
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

    public void setWorkflowService(WorkflowService workflowService) {
      this.workflowService = workflowService;
    }

    public OpencastArchive getArchive() {
      return archive;
    }

    public void setArchive(OpencastArchive archive) {
      this.archive = archive;
    }

    public HttpMediaPackageElementProvider getHttpMediaPackageElementProvider() {
      return httpMediaPackageElementProvider;
    }

    public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
      this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
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
