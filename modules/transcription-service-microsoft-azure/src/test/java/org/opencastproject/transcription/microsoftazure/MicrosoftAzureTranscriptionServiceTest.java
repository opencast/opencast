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
package org.opencastproject.transcription.microsoftazure;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

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
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.transcription.microsoftazure.MicrosoftAzureTranscriptionService.WorkflowDispatcher;
import org.opencastproject.transcription.persistence.TranscriptionDatabaseImpl;
import org.opencastproject.transcription.persistence.TranscriptionJobControl;
import org.opencastproject.transcription.persistence.TranscriptionProviderControl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MicrosoftAzureTranscriptionServiceTest {

  private static final String MP_ID = "mpId1";
  private static final String TRACK_ID = "audioTrack1";
  private static final String JOB_ID = "jobId1";
  private static final long TRACK_DURATION = 60000;
  private static final Date DATE_EXPECTED = null;
  private static final String SUBSCRIPTION_KEY = "subscriptionKey";
  private static final String REGION = "region";
  private static final String PULLED_TRANSCRIPTION_FILE = "pulled_google_transcription.json";
  private static final String IN_PROGRESS_JOB = "in_progress_job.json";
  private static final String PROVIDER = "Microsoft Azure";
  private static final long PROVIDER_ID = 1;
  private static final String LANGUAGE_CODE = "en-US";
  private static final String RESULT_URL = "https://speech.googleapis.com/v1/operations/";

  private MediaPackage mediaPackage;
  private File audioFile;

  private MicrosoftAzureTranscriptionService service;
  private TranscriptionDatabaseImpl database;
  private Workspace workspace;
  private AssetManager assetManager;
  private WorkflowService wfService;
  private JaxbOrganization org;
  private SmtpService smtpService;
  private Dictionary<String, Object> props = new Hashtable<String, Object>();
  private ComponentContext cc;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    URI mediaPackageURI = MicrosoftAzureTranscriptionServiceTest.class.getResource("/mp.xml").toURI();
    mediaPackage = builder.loadFromXml(mediaPackageURI.toURL().openStream());

    URI audioUrl = MicrosoftAzureTranscriptionServiceTest.class.getResource("/audio.flac").toURI();
    audioFile = new File(audioUrl);

    props.put(MicrosoftAzureTranscriptionService.ENABLED_CONFIG, "true");
    props.put(MicrosoftAzureTranscriptionService.SUBSCRIPTION_KEY, SUBSCRIPTION_KEY);
    props.put(MicrosoftAzureTranscriptionService.REGION, REGION);
    props.put(MicrosoftAzureTranscriptionService.COMPLETION_CHECK_BUFFER_CONFIG, 0);
    props.put(MicrosoftAzureTranscriptionService.MAX_PROCESSING_TIME_CONFIG, 0);
    props.put(MicrosoftAzureTranscriptionService.NOTIFICATION_EMAIL_CONFIG, "anyone@opencast.org");

    cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getProperties()).andReturn(props).anyTimes();
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(OpencastConstants.SERVER_URL_PROPERTY)).andReturn("http://THIS_SERVER");
    EasyMock.expect(bc.getProperty("org.opencastproject.security.digest.user")).andReturn("matterhorn_system_account");
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    Map<String, String> orgProps = new HashMap<String, String>();
    orgProps.put(OpencastConstants.ADMIN_URL_ORG_PROPERTY, "http://ADMIN_SERVER");
    org = new JaxbOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            DefaultOrganization.DEFAULT_ORGANIZATION_NAME, null, DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, orgProps);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();

    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(org).anyTimes();
    UserDirectoryService userDirectory = EasyMock.createNiceMock(UserDirectoryService.class);

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
    database = new TranscriptionDatabaseImpl() {
      @Override
      public TranscriptionProviderControl findIdByProvider(String provider) {
        return new TranscriptionProviderControl(PROVIDER_ID, PROVIDER);
      }
    };
    database.setEntityManagerFactory(newEntityManagerFactory("org.opencastproject.transcription.persistence"));
    database.setDBSessionFactory(getDbSessionFactory());
    database.activate(null);

    service = new MicrosoftAzureTranscriptionService();

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
  }

  @After
  public void tearDown() throws Exception {
  }

  // TODO: Figure out how to create a SpeechRecognizer without having to connect to microsoft servers
  //  Alternatively figure out how to properly test without a SpeechRecognizer

//  @Test
//  public void testCreateTranscriptionJob() throws Exception {
//    InputStream stream = MicrosoftAzureTranscriptionServiceTest.class.getResourceAsStream("/" + IN_PROGRESS_JOB);
//
//    Capture<String> capturedCollection = Capture.newInstance();
//    Capture<String> capturedFileName = Capture.newInstance();
//    EasyMock.expect(workspace.putInCollection(EasyMock.capture(capturedCollection),
//      EasyMock.capture(capturedFileName),
//            EasyMock.anyObject(InputStream.class))).andReturn(new URI("http://anything"));
//    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class))).andReturn(audioFile);
//    EasyMock.replay(workspace);
//
//    SpeechRecognizer speechRecognizer = new SpeechRecognizer(SpeechConfig.fromAuthorizationToken("adsf", "asdf"));
//    service.recognizeContinuous(speechRecognizer, JOB_ID, MP_ID, mediaPackage.getTrack("audioTrack1"));
//    speechRecognizer.sessionStarted.fireEvent(null, new SessionEventArgs(1));
//
//    TranscriptionJobControl j = database.findByJob(JOB_ID);
//    Assert.assertNotNull(j);
//    Assert.assertEquals(MP_ID, j.getMediaPackageId());
//    Assert.assertEquals(TRACK_ID, j.getTrackId());
//    Assert.assertEquals(TranscriptionJobControl.Status.InProgress.name(), j.getStatus());
//  }

  @Test
  public void testGetGeneratedTranscriptionNoJobId() throws Exception {
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID,
            TranscriptionJobControl.Status.InProgress.name(), TRACK_DURATION, DATE_EXPECTED, PROVIDER);
    database.storeJobControl(MP_ID, "audioTrack2", "jobId2",
            TranscriptionJobControl.Status.InProgress.name(), TRACK_DURATION, DATE_EXPECTED, PROVIDER);
    database.updateJobControl(JOB_ID, TranscriptionJobControl.Status.TranscriptionComplete.name());

    URI uri = new URI("http://ADMIN_SERVER/collection/" + MicrosoftAzureTranscriptionService.TRANSCRIPT_COLLECTION + "/"
            + JOB_ID + ".json");
    EasyMock
            .expect(workspace.getCollectionURI(
                    MicrosoftAzureTranscriptionService.TRANSCRIPT_COLLECTION, JOB_ID + ".json"
            ))
            .andReturn(uri);
    EasyMock.expect(workspace.get(uri)).andReturn(null); // Doesn't matter what is returned
    EasyMock.replay(workspace);

    MediaPackageElement mpe = service.getGeneratedTranscription(MP_ID, null);
    Assert.assertEquals("captions", mpe.getFlavor().getType());
    Assert.assertEquals("microsoft-azure", mpe.getFlavor().getSubtype());
  }

  @Test
  public void testTranscriptionError() throws Exception {
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID,
            TranscriptionJobControl.Status.InProgress.name(), TRACK_DURATION, DATE_EXPECTED, PROVIDER);

    service.transcriptionError(MP_ID, JOB_ID);
    // Check if status and date in db was updated
    TranscriptionJobControl job = database.findByJob(JOB_ID);
    Assert.assertNotNull(job);
    Assert.assertEquals(TranscriptionJobControl.Status.Error.name(), job.getStatus());
    Assert.assertNull(job.getDateCompleted());

  }

  @Test
  public void testWorkflowDispatcherRunTranscriptionCompletedState() throws Exception {
    service.activate(cc);

    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID,
            TranscriptionJobControl.Status.InProgress.name(), TRACK_DURATION, DATE_EXPECTED, PROVIDER);
    database.storeJobControl("mpId2", "audioTrack2", "jobId2",
            TranscriptionJobControl.Status.InProgress.name(), TRACK_DURATION, DATE_EXPECTED, PROVIDER);
    database.updateJobControl(JOB_ID, TranscriptionJobControl.Status.TranscriptionComplete.name());

    Capture<Set<String>> capturedMpIds = mockAssetManagerAndWorkflow(MicrosoftAzureTranscriptionService.DEFAULT_WF_DEF,
            true);

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

  private Capture<Set<String>> mockAssetManagerAndWorkflow(String wfDefId, boolean wfStarted)
          throws NotFoundException, WorkflowDatabaseException {
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
    EasyMock.expect(wfService.getWorkflowDefinitionById(wfDefId)).andReturn(wfDef);
    List<WorkflowInstance> wfList = new ArrayList<WorkflowInstance>();
    if (wfStarted) {
      wfList.add(new WorkflowInstance());
    }
    Stream<WorkflowInstance> wfListStream = Stream.mk(wfList);
    Workflows wfs = EasyMock.createNiceMock(Workflows.class);
    EasyMock.expect(wfs.applyWorkflowToLatestVersion(EasyMock.capture(capturedMpIds),
            EasyMock.anyObject(ConfiguredWorkflow.class))).andReturn(wfListStream);
    service.setWfUtil(wfs);

    EasyMock.replay(wfService, wfs);

    return capturedMpIds;
  }

}
