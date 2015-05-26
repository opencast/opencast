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

import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.rest.NotFoundExceptionMapper;
import org.opencastproject.rest.RestServiceTestEnv;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.util.ArrayList;
import java.util.List;

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
  public void testGetEventMedia() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventMedia.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/media")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventTrack() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventTrack.json"));

    given().pathParam("eventId", "asdasd").pathParam("trackId", "publish-track-4").expect()
    .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host("{eventId}/media/{trackId}"));

    String result = given().pathParam("eventId", "asdasd").pathParam("trackId", "publish-track-2").expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("{eventId}/media/{trackId}")).asString();

    assertThat(eventMetadataString, SameJSONAs.sameJSONAs(result));
  }

  @Test
  public void testGetEventAttachements() throws Exception {
    String eventMetadataString = IOUtils.toString(getClass().getResource("/eventAttachment.json"));

    String result = given().pathParam("eventId", "asdasd").expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("{eventId}/attachments")).asString();

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

  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

  public static final class TestEnv {
    private Workspace workspace;
    private WorkflowService workflowService;
    private OpencastArchive archive;
    private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
    private JobEndpoint jobService;
    private ListProvidersService listProviderService;
    private AclService aclService;
    private SeriesService seriesService;
    private ParticipationManagementDatabase participationManagementDatabase;
    private DublinCoreCatalogService dublinCoreCatalogService;
    private EventCommentService eventCommentService;
    private SecurityService securityService;
    private IndexService indexService;
    private IngestService ingestService;
    private AuthorizationService authorizationService;
    private SchedulerService schedulerService;
    private CaptureAgentStateService captureAgentStateService;
    private AdminUISearchIndex index;
    private final List<EventCatalogUIAdapter> catalogUIAdapters = new ArrayList<EventCatalogUIAdapter>();
    private CommonEventCatalogUIAdapter episodeCatalogUIAdapter;
    private String previewSubtype;

    public Workspace getWorkspace() {
      return workspace;
    }

    public void setWorkspace(Workspace workspace) {
      this.workspace = workspace;
    }

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

    public ListProvidersService getListProviderService() {
      return listProviderService;
    }

    public void setListProviderService(ListProvidersService listProviderService) {
      this.listProviderService = listProviderService;
    }

    public AclService getAclService() {
      return aclService;
    }

    public void setAclService(AclService aclService) {
      this.aclService = aclService;
    }

    public SeriesService getSeriesService() {
      return seriesService;
    }

    public void setSeriesService(SeriesService seriesService) {
      this.seriesService = seriesService;
    }

    public ParticipationManagementDatabase getPmPersistence() {
      return participationManagementDatabase;
    }

    public void setParticipationManagementDatabase(ParticipationManagementDatabase participationManagementDatabase) {
      this.participationManagementDatabase = participationManagementDatabase;
    }

    public DublinCoreCatalogService getDublinCoreService() {
      return dublinCoreCatalogService;
    }

    public void setDublinCoreCatalogService(DublinCoreCatalogService dublinCoreCatalogService) {
      this.dublinCoreCatalogService = dublinCoreCatalogService;
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

    public IngestService getIngestService() {
      return ingestService;
    }

    public void setIngestService(IngestService ingestService) {
      this.ingestService = ingestService;
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

    public void setPreviewSubtype(String subtype) {
      this.previewSubtype = subtype;
    }

    public String getPreviewSubtype() {
      return previewSubtype;
    }

    public EventCatalogUIAdapter getEpisodeCatalogUIAdapter() {
      return episodeCatalogUIAdapter;
    }

    public void setEpisodeCatalogUIAdapter(CommonEventCatalogUIAdapter episodeCatalogUIAdapter) {
      this.episodeCatalogUIAdapter = episodeCatalogUIAdapter;
    }

    public List<EventCatalogUIAdapter> getCatalogUIAdapters() {
      return catalogUIAdapters;
    }

    public void setCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
      catalogUIAdapters.add(catalogUIAdapter);
    }

    public void unsetCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
      catalogUIAdapters.remove(catalogUIAdapter);
    }

  }
}
