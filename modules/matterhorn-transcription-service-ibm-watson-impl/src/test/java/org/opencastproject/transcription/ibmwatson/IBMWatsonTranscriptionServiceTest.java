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
package org.opencastproject.transcription.ibmwatson;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.transcription.api.TranscriptionServiceException;
import org.opencastproject.transcription.ibmwatson.IBMWatsonTranscriptionService.WorkflowDispatcher;
import org.opencastproject.transcription.ibmwatson.persistence.TranscriptionDatabase;
import org.opencastproject.transcription.ibmwatson.persistence.TranscriptionJobControl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IBMWatsonTranscriptionServiceTest {
  private static final String MP_ID = "mpId1";
  private static final String TRACK_ID = "audioTrack1";
  private static final String JOB_ID = "jobId1";
  private static final long TRACK_DURATION = 60000;
  private static final String PULLED_TRANSCRIPTION_FILE = "pulled_transcription.json";
  private static final String PUSHED_TRANSCRIPTION_FILE = "pushed_transcription.json";
  private static final String IN_PROGRESS_JOB = "in_progress_job.json";

  private CloseableHttpClient httpClient;
  private MediaPackage mediaPackage;
  private JSONParser jsonParser = new JSONParser();
  // private InputStream resultsStream;
  private File audioFile;

  private IBMWatsonTranscriptionService service;
  private TranscriptionDatabase database;
  private Workspace workspace;
  private AssetManager assetManager;
  private WorkflowService wfService;
  private JaxbOrganization org;
  private SmtpService smtpService;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    URI mediaPackageURI = IBMWatsonTranscriptionServiceTest.class.getResource("/mp.xml").toURI();
    mediaPackage = builder.loadFromXml(mediaPackageURI.toURL().openStream());

    URI audioUrl = IBMWatsonTranscriptionServiceTest.class.getResource("/audio.ogg").toURI();
    audioFile = new File(audioUrl);

    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(IBMWatsonTranscriptionService.ENABLED_CONFIG, "true");
    props.put(IBMWatsonTranscriptionService.IBM_WATSON_USER_CONFIG, "user");
    props.put(IBMWatsonTranscriptionService.IBM_WATSON_PSW_CONFIG, "psw");
    props.put(IBMWatsonTranscriptionService.COMPLETION_CHECK_BUFFER_CONFIG, 0);
    props.put(IBMWatsonTranscriptionService.MAX_PROCESSING_TIME_CONFIG, 0);
    props.put(IBMWatsonTranscriptionService.NOTIFICATION_EMAIL_CONFIG, "anyone@opencast.org");

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getProperties()).andReturn(props).anyTimes();
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(MatterhornConstants.SERVER_URL_PROPERTY)).andReturn("http://THIS_SERVER");
    EasyMock.expect(bc.getProperty("org.opencastproject.security.digest.user")).andReturn("matterhorn_system_account");
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    Map<String, String> orgProps = new HashMap<String, String>();
    orgProps.put(IBMWatsonTranscriptionService.ADMIN_URL_PROPERTY, "http://ADMIN_SERVER");
    org = new JaxbOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            DefaultOrganization.DEFAULT_ORGANIZATION_NAME, null, DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, orgProps);
    User user = new JaxbUser("admin", null, "test", org, new JaxbRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org));
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();

    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(org).anyTimes();
    UserDirectoryService userDirectory = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectory.loadUser("admin")).andReturn(user).anyTimes();

    IncidentService incident = EasyMock.createNiceMock(IncidentService.class);

    smtpService = EasyMock.createNiceMock(SmtpService.class);
    smtpService.send((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject());
    EasyMock.expectLastCall().once();

    EasyMock.replay(bc, cc, securityService, orgDirectory, userDirectory, incident, smtpService);

    // Mocks for WorkflowDispatcher test
    assetManager = EasyMock.createNiceMock(AssetManager.class);
    wfService = EasyMock.createNiceMock(WorkflowService.class);

    workspace = EasyMock.createNiceMock(Workspace.class);

    // Database
    database = new TranscriptionDatabase();
    database.setEntityManagerFactory(
            PersistenceUtil.newTestEntityManagerFactory("org.opencastproject.transcription.ibmwatson.persistence"));
    database.activate(null);

    httpClient = EasyMock.createNiceMock(CloseableHttpClient.class);
    service = new IBMWatsonTranscriptionService() {
      @Override
      protected CloseableHttpClient makeHttpClient() {
        return httpClient;
      }
    };
    ServiceRegistry serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectory,
            orgDirectory, incident);
    service.setOrganizationDirectoryService(orgDirectory);
    service.setSecurityService(securityService);
    service.setServiceRegistry(serviceRegistry);
    service.setUserDirectoryService(userDirectory);
    service.setWorkspace(workspace);
    service.setDatabase(database);
    service.setAssetManager(assetManager);
    service.setWorkflowService(wfService);
    service.setSmtpService(smtpService);
    service.activate(cc);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testRegisterCallback200() throws Exception {
    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status);
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_OK);
    EasyMock.replay(response, status);

    Capture<HttpPost> capturedPost = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedPost))).andReturn(response);
    EasyMock.replay(httpClient);

    service.registerCallback();
    Assert.assertTrue(service.isCallbackAlreadyRegistered());
    Assert.assertEquals(
            "https://stream.watsonplatform.net/speech-to-text/api/v1/register_callback?callback_url=http://ADMIN_SERVER/transcripts/watson/results",
            capturedPost.getValue().getURI().toString());
  }

  @Test
  public void testRegisterCallback201() throws Exception {
    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status);
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_CREATED);
    EasyMock.replay(response, status);

    Capture<HttpPost> capturedPost = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedPost))).andReturn(response);
    EasyMock.replay(httpClient);

    service.registerCallback();
    Assert.assertTrue(service.isCallbackAlreadyRegistered());
    Assert.assertEquals(
            "https://stream.watsonplatform.net/speech-to-text/api/v1/register_callback?callback_url=http://ADMIN_SERVER/transcripts/watson/results",
            capturedPost.getValue().getURI().toString());
  }

  @Test
  public void testRegisterCallbackErrors() throws Exception {
    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_BAD_REQUEST).anyTimes();
    EasyMock.replay(response, status);

    EasyMock.expect(httpClient.execute(EasyMock.anyObject(HttpPost.class))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    service.registerCallback();
    Assert.assertFalse(service.isCallbackAlreadyRegistered());
  }

  @Test
  public void testCreateRecognitionsJob() throws Exception {
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class))).andReturn(audioFile);
    EasyMock.replay(workspace);

    HttpEntity httpEntity = EasyMock.createNiceMock(HttpEntity.class);
    EasyMock.expect(httpEntity.getContent()).andReturn(new ByteArrayInputStream(new String("{\"id\": \"" + JOB_ID
            + "\", \"status\": \"waiting\", \"url\": \"http://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/"
            + JOB_ID + "\"}").getBytes(StandardCharsets.UTF_8)));

    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(response.getEntity()).andReturn(httpEntity).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_CREATED).anyTimes();
    EasyMock.replay(httpEntity, response, status);

    Capture<HttpPost> capturedPost = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedPost))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    service.createRecognitionsJob(MP_ID, mediaPackage.getTrack("audioTrack1"));
    Assert.assertEquals("https://stream.watsonplatform.net/speech-to-text/api/v1/recognitions?user_token=" + MP_ID
            + "&inactivity_timeout=-1&timestamps=true&smart_formatting=true"
            + "&callback_url=http://ADMIN_SERVER/transcripts/watson/results&events=recognitions.completed_with_results,recognitions.failed",
            capturedPost.getValue().getURI().toString());
    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
  }

  @Test(expected = TranscriptionServiceException.class)
  public void testCreateRecognitionsJobErrors() throws Exception {
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class))).andReturn(audioFile);
    EasyMock.replay(workspace);

    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_CREATED).once(); // register callback
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_SERVICE_UNAVAILABLE).once();
    EasyMock.replay(response, status);

    Capture<HttpPost> capturedPost = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedPost))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    service.createRecognitionsJob(MP_ID, mediaPackage.getTrack("audioTrack1"));
  }

  @Test
  public void testTranscriptionDone() throws Exception {
    InputStream stream = IBMWatsonTranscriptionServiceTest.class.getResourceAsStream("/" + PUSHED_TRANSCRIPTION_FILE);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), TRACK_DURATION);
    JSONObject obj = (JSONObject) jsonParser.parse(new InputStreamReader(stream));

    Capture<String> capturedCollection = Capture.newInstance();
    Capture<String> capturedFileName = Capture.newInstance();
    EasyMock.expect(workspace.putInCollection(EasyMock.capture(capturedCollection), EasyMock.capture(capturedFileName),
            EasyMock.anyObject(InputStream.class))).andReturn(new URI("http://anything"));
    EasyMock.replay(workspace);

    long before = System.currentTimeMillis();
    service.transcriptionDone(MP_ID, obj);
    long after = System.currentTimeMillis();
    // Check if status and date in db was updated
    TranscriptionJobControl job = database.findByJob(JOB_ID);
    Assert.assertNotNull(job);
    Assert.assertEquals(TranscriptionJobControl.Status.TranscriptionComplete.name(), job.getStatus());
    Assert.assertNotNull(job.getDateCompleted());
    Assert.assertTrue(before <= job.getDateCompleted().getTime() && job.getDateCompleted().getTime() <= after);
    // Check if results were saved into a collection
    Assert.assertEquals(IBMWatsonTranscriptionService.TRANSCRIPT_COLLECTION, capturedCollection.getValue());
    Assert.assertEquals(JOB_ID + ".json", capturedFileName.getValue());
  }

  @Test
  public void testTranscriptionError() throws Exception {
    InputStream stream = IBMWatsonTranscriptionServiceTest.class.getResourceAsStream("/" + PUSHED_TRANSCRIPTION_FILE);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), TRACK_DURATION);
    JSONObject obj = (JSONObject) jsonParser.parse(new InputStreamReader(stream));

    service.transcriptionError(MP_ID, obj);
    // Check if status and date in db was updated
    TranscriptionJobControl job = database.findByJob(JOB_ID);
    Assert.assertNotNull(job);
    Assert.assertEquals(TranscriptionJobControl.Status.Error.name(), job.getStatus());
    Assert.assertNull(job.getDateCompleted());

    EasyMock.verify(smtpService);
  }

  @Test
  public void testGetAndSaveJobResults() throws Exception {
    InputStream stream = IBMWatsonTranscriptionServiceTest.class.getResourceAsStream("/" + PULLED_TRANSCRIPTION_FILE);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), TRACK_DURATION);

    Capture<String> capturedCollection = Capture.newInstance();
    Capture<String> capturedFileName = Capture.newInstance();
    EasyMock.expect(workspace.putInCollection(EasyMock.capture(capturedCollection), EasyMock.capture(capturedFileName),
            EasyMock.anyObject(InputStream.class))).andReturn(new URI("http://anything"));
    EasyMock.replay(workspace);

    HttpEntity httpEntity = EasyMock.createNiceMock(HttpEntity.class);
    EasyMock.expect(httpEntity.getContent()).andReturn(stream);

    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(response.getEntity()).andReturn(httpEntity).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_OK).anyTimes();
    EasyMock.replay(httpEntity, response, status);

    Capture<HttpGet> capturedGet = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedGet))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    long before = System.currentTimeMillis();
    service.getAndSaveJobResults(JOB_ID);
    long after = System.currentTimeMillis();
    // Check if correct url was invoked
    Assert.assertEquals("https://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/" + JOB_ID,
            capturedGet.getValue().getURI().toString());
    // Check if status and date in db was updated
    TranscriptionJobControl job = database.findByJob(JOB_ID);
    Assert.assertNotNull(job);
    Assert.assertEquals(TranscriptionJobControl.Status.TranscriptionComplete.name(), job.getStatus());
    Assert.assertNotNull(job.getDateCompleted());
    Assert.assertTrue(before <= job.getDateCompleted().getTime() && job.getDateCompleted().getTime() <= after);
    // Check if results were saved into a collection
    Assert.assertEquals(IBMWatsonTranscriptionService.TRANSCRIPT_COLLECTION, capturedCollection.getValue());
    Assert.assertEquals(JOB_ID + ".json", capturedFileName.getValue());
  }

  @Test
  public void testGetAndSaveJobResultsError404() throws Exception {
    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_NOT_FOUND).anyTimes();
    EasyMock.replay(response, status);

    Capture<HttpGet> capturedGet = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedGet))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    try {
      service.getAndSaveJobResults(JOB_ID);
    } catch (TranscriptionServiceException e) {
      Assert.assertEquals(404, e.getCode());
      return;
    }
    Assert.fail("TranscriptionServiceException not thrown");
  }

  @Test(expected = TranscriptionServiceException.class)
  public void testGetAndSaveJobResultsError503() throws Exception {
    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_SERVICE_UNAVAILABLE).anyTimes();
    EasyMock.replay(response, status);

    Capture<HttpGet> capturedGet = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedGet))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    service.getAndSaveJobResults(JOB_ID);
  }

  @Test
  public void testGetGeneratedTranscriptionNoJobId() throws Exception {
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), TRACK_DURATION);
    database.storeJobControl(MP_ID, "audioTrack2", "jobId2", TranscriptionJobControl.Status.Progress.name(),
            TRACK_DURATION);
    database.updateJobControl(JOB_ID, TranscriptionJobControl.Status.TranscriptionComplete.name());

    URI uri = new URI("http://ADMIN_SERVER/collection/" + IBMWatsonTranscriptionService.TRANSCRIPT_COLLECTION + "/"
            + JOB_ID + ".json");
    EasyMock.expect(workspace.getCollectionURI(IBMWatsonTranscriptionService.TRANSCRIPT_COLLECTION, JOB_ID + ".json"))
            .andReturn(uri);
    EasyMock.expect(workspace.get(uri)).andReturn(null); // Doesn't matter what is returned
    EasyMock.replay(workspace);

    MediaPackageElement mpe = service.getGeneratedTranscription(MP_ID, null);
    Assert.assertEquals("captions", mpe.getFlavor().getType());
    Assert.assertEquals("ibm-watson-json", mpe.getFlavor().getSubtype());
    Assert.assertEquals(uri.toString(), mpe.getURI().toString());
  }

  @Test
  public void testGetGeneratedTranscriptionNotInWorkspace() throws Exception {
    InputStream stream = IBMWatsonTranscriptionServiceTest.class.getResourceAsStream("/" + PULLED_TRANSCRIPTION_FILE);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), TRACK_DURATION);

    URI uri = new URI("http://ADMIN_SERVER/collection/" + IBMWatsonTranscriptionService.TRANSCRIPT_COLLECTION + "/"
            + JOB_ID + ".json");
    EasyMock.expect(workspace.getCollectionURI(IBMWatsonTranscriptionService.TRANSCRIPT_COLLECTION, JOB_ID + ".json"))
            .andReturn(uri);
    EasyMock.expect(workspace.get(uri)).andThrow(new NotFoundException());
    EasyMock.expect(workspace.putInCollection(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
            EasyMock.anyObject(InputStream.class))).andReturn(uri);
    EasyMock.replay(workspace);

    HttpEntity httpEntity = EasyMock.createNiceMock(HttpEntity.class);
    EasyMock.expect(httpEntity.getContent()).andReturn(stream);

    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(response.getEntity()).andReturn(httpEntity).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_OK).anyTimes();
    EasyMock.replay(httpEntity, response, status);

    EasyMock.expect(httpClient.execute(EasyMock.anyObject(HttpGet.class))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    MediaPackageElement mpe = service.getGeneratedTranscription(MP_ID, JOB_ID);
    Assert.assertEquals("captions", mpe.getFlavor().getType());
    Assert.assertEquals("ibm-watson-json", mpe.getFlavor().getSubtype());
    Assert.assertEquals(uri.toString(), mpe.getURI().toString());
  }

  @Test
  public void testWorkflowDispatcherRunTranscriptionCompletedState() throws Exception {

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), TRACK_DURATION);
    database.storeJobControl("mpId2", "audioTrack2", "jobId2", TranscriptionJobControl.Status.Progress.name(),
            TRACK_DURATION);
    database.updateJobControl(JOB_ID, TranscriptionJobControl.Status.TranscriptionComplete.name());

    // Mocks for query, result, etc
    Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(snapshot.getOrganizationId()).andReturn(org.getId());
    ARecord aRec = EasyMock.createNiceMock(ARecord.class);
    EasyMock.expect(aRec.getSnapshot()).andReturn(Opt.some(snapshot));
    Stream<ARecord> recStream = Stream.mk(aRec);
    Predicate p = EasyMock.createNiceMock(Predicate.class);
    EasyMock.expect(p.and(p)).andReturn(p);
    AResult r = EasyMock.createNiceMock(AResult.class);
    EasyMock.expect(r.getSize()).andReturn(1L);
    EasyMock.expect(r.getRecords()).andReturn(recStream);
    Target t = EasyMock.createNiceMock(Target.class);
    ASelectQuery selectQuery = EasyMock.createNiceMock(ASelectQuery.class);
    EasyMock.expect(selectQuery.where(EasyMock.anyObject(Predicate.class))).andReturn(selectQuery);
    EasyMock.expect(selectQuery.run()).andReturn(r);
    AQueryBuilder query = EasyMock.createNiceMock(AQueryBuilder.class);
    EasyMock.expect(query.snapshot()).andReturn(t);
    EasyMock.expect(query.mediaPackageId(EasyMock.anyObject(String.class))).andReturn(p);
    EasyMock.expect(query.select(EasyMock.anyObject(Target.class))).andReturn(selectQuery);
    VersionField v = EasyMock.createNiceMock(VersionField.class);
    EasyMock.expect(v.isLatest()).andReturn(p);
    EasyMock.expect(query.version()).andReturn(v);
    EasyMock.expect(assetManager.createQuery()).andReturn(query);

    EasyMock.replay(snapshot, aRec, p, r, t, selectQuery, query, v, assetManager);

    Capture<Set<String>> capturedMpIds = Capture.newInstance();
    WorkflowDefinition wfDef = new WorkflowDefinitionImpl();
    EasyMock.expect(wfService.getWorkflowDefinitionById(IBMWatsonTranscriptionService.DEFAULT_WF_DEF)).andReturn(wfDef);
    List<WorkflowInstance> wfList = new ArrayList<WorkflowInstance>();
    wfList.add(new WorkflowInstanceImpl());
    Stream<WorkflowInstance> wfListStream = Stream.mk(wfList);
    Workflows wfs = EasyMock.createNiceMock(Workflows.class);
    EasyMock.expect(wfs.applyWorkflowToLatestVersion(EasyMock.capture(capturedMpIds),
            EasyMock.anyObject(ConfiguredWorkflow.class))).andReturn(wfListStream);
    service.setWfUtil(wfs);

    EasyMock.replay(wfService, wfs);

    WorkflowDispatcher dispatcher = service.new WorkflowDispatcher();
    dispatcher.run();

    // Check if only one mp has a workflow created for it
    Assert.assertEquals(1, capturedMpIds.getValue().size());
    // And if it was the correct one
    Assert.assertEquals(MP_ID, capturedMpIds.getValue().iterator().next());
    // Check if status in db was updated
    TranscriptionJobControl job = database.findByJob(JOB_ID);
    Assert.assertNotNull(job);
    Assert.assertEquals(TranscriptionJobControl.Status.Closed.name(), job.getStatus());
  }

  @Test
  public void testWorkflowDispatcherRunProgressState() throws Exception {
    InputStream stream = IBMWatsonTranscriptionServiceTest.class.getResourceAsStream("/" + PULLED_TRANSCRIPTION_FILE);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), 0);
    database.storeJobControl("mpId2", "audioTrack2", "jobId2", TranscriptionJobControl.Status.Progress.name(),
            TRACK_DURATION);

    EasyMock.expect(workspace.putInCollection(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
            EasyMock.anyObject(InputStream.class))).andReturn(new URI("http://anything"));
    EasyMock.replay(workspace);

    HttpEntity httpEntity = EasyMock.createNiceMock(HttpEntity.class);
    EasyMock.expect(httpEntity.getContent()).andReturn(stream);

    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(response.getEntity()).andReturn(httpEntity).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_OK).anyTimes();
    EasyMock.replay(httpEntity, response, status);

    Capture<HttpGet> capturedGet = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedGet))).andReturn(response);
    EasyMock.replay(httpClient);
    // enrich(q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest())).run()).getSnapshots();
    // Mocks for query, result, etc
    Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(snapshot.getOrganizationId()).andReturn(org.getId());
    ARecord aRec = EasyMock.createNiceMock(ARecord.class);
    EasyMock.expect(aRec.getSnapshot()).andReturn(Opt.some(snapshot));
    Stream<ARecord> recStream = Stream.mk(aRec);
    Predicate p = EasyMock.createNiceMock(Predicate.class);
    EasyMock.expect(p.and(p)).andReturn(p);
    AResult r = EasyMock.createNiceMock(AResult.class);
    EasyMock.expect(r.getSize()).andReturn(1L);
    EasyMock.expect(r.getRecords()).andReturn(recStream);
    Target t = EasyMock.createNiceMock(Target.class);
    ASelectQuery selectQuery = EasyMock.createNiceMock(ASelectQuery.class);
    EasyMock.expect(selectQuery.where(EasyMock.anyObject(Predicate.class))).andReturn(selectQuery);
    EasyMock.expect(selectQuery.run()).andReturn(r);
    AQueryBuilder query = EasyMock.createNiceMock(AQueryBuilder.class);
    EasyMock.expect(query.snapshot()).andReturn(t);
    EasyMock.expect(query.mediaPackageId(EasyMock.anyObject(String.class))).andReturn(p);
    EasyMock.expect(query.select(EasyMock.anyObject(Target.class))).andReturn(selectQuery);
    VersionField v = EasyMock.createNiceMock(VersionField.class);
    EasyMock.expect(v.isLatest()).andReturn(p);
    EasyMock.expect(query.version()).andReturn(v);
    EasyMock.expect(assetManager.createQuery()).andReturn(query);

    EasyMock.replay(snapshot, aRec, p, r, t, selectQuery, query, v, assetManager);

    Capture<Set<String>> capturedMpIds = Capture.newInstance();
    WorkflowDefinition wfDef = new WorkflowDefinitionImpl();
    EasyMock.expect(wfService.getWorkflowDefinitionById(IBMWatsonTranscriptionService.DEFAULT_WF_DEF)).andReturn(wfDef);
    List<WorkflowInstance> wfList = new ArrayList<WorkflowInstance>();
    wfList.add(new WorkflowInstanceImpl());
    Stream<WorkflowInstance> wfListStream = Stream.mk(wfList);
    Workflows wfs = EasyMock.createNiceMock(Workflows.class);
    EasyMock.expect(wfs.applyWorkflowToLatestVersion(EasyMock.capture(capturedMpIds),
            EasyMock.anyObject(ConfiguredWorkflow.class))).andReturn(wfListStream);
    service.setWfUtil(wfs);

    EasyMock.replay(wfService, wfs);

    WorkflowDispatcher dispatcher = service.new WorkflowDispatcher();
    dispatcher.run();

    // Check if it called the external service to get the results
    Assert.assertEquals("https://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/" + JOB_ID,
            capturedGet.getValue().getURI().toString());
    // Check if only one mp has a workflow created for it
    Assert.assertEquals(1, capturedMpIds.getValue().size());
    // And if it was the correct one
    Assert.assertEquals(MP_ID, capturedMpIds.getValue().iterator().next());
    // Check if status in db was updated
    TranscriptionJobControl job = database.findByJob(JOB_ID);
    Assert.assertNotNull(job);
    Assert.assertEquals(TranscriptionJobControl.Status.Closed.name(), job.getStatus());
  }

  @Test
  public void testWorkflowDispatcherJobNotFound() throws Exception {
    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_NOT_FOUND).anyTimes();
    EasyMock.replay(response, status);

    Capture<HttpGet> capturedGet = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedGet))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), 0);

    EasyMock.replay(workspace);

    WorkflowDispatcher dispatcher = service.new WorkflowDispatcher();
    dispatcher.run();

    // Check if it called the external service to get the results
    Assert.assertEquals("https://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/" + JOB_ID,
            capturedGet.getValue().getURI().toString());

    // Check if the job status was updated and email was sent
    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(TranscriptionJobControl.Status.Canceled.toString(), j.getStatus());
    EasyMock.verify(smtpService);
  }

  @Test
  public void testWorkflowDispatcherJobInProgressTooLong() throws Exception {
    InputStream stream = IBMWatsonTranscriptionServiceTest.class.getResourceAsStream("/" + IN_PROGRESS_JOB);

    HttpEntity httpEntity = EasyMock.createNiceMock(HttpEntity.class);
    EasyMock.expect(httpEntity.getContent()).andReturn(stream);

    CloseableHttpResponse response = EasyMock.createNiceMock(CloseableHttpResponse.class);
    StatusLine status = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(response.getStatusLine()).andReturn(status).anyTimes();
    EasyMock.expect(response.getEntity()).andReturn(httpEntity).anyTimes();
    EasyMock.expect(status.getStatusCode()).andReturn(HttpStatus.SC_OK).anyTimes();
    EasyMock.replay(httpEntity, response, status);

    Capture<HttpGet> capturedGet = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedGet))).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, TranscriptionJobControl.Status.Progress.name(), 0);

    EasyMock.replay(workspace);

    WorkflowDispatcher dispatcher = service.new WorkflowDispatcher();
    dispatcher.run();

    // Check if it called the external service to get the results
    Assert.assertEquals("https://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/" + JOB_ID,
            capturedGet.getValue().getURI().toString());

    // Check if the job status was updated and email was sent
    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(TranscriptionJobControl.Status.Canceled.toString(), j.getStatus());
    EasyMock.verify(smtpService);
  }

}
