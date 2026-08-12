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
package org.opencastproject.speechtotext.async.impl;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;
import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.speechtotext.async.api.SpeechToTextAsyncTracker.JOBS_WORKFLOW_CONFIGURATION;
import static org.opencastproject.speechtotext.async.persistence.SpeechToTextTestUtil.createJob;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.speechtotext.async.persistence.SpeechToTextControl;
import org.opencastproject.speechtotext.async.persistence.SpeechToTextDatabase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

public class SpeechToTextWorkflowSchedulerQuartzTest {
  private static final long JOB_ID_1 = 1L;
  private static final long JOB_ID_2 = 2L;
  private static final long JOB_ID_3 = 3L;
  private static final long JOB_ID_4 = 4L;
  private static final long JOB_ID_5 = 5L;
  private static final long JOB_ID_6 = 6L;
  private static final long WF_ID_1 = 1L;
  private static final long WF_ID_2 = 2L;
  private static final long WF_ID_3 = 3L;
  private static final long WF_ID_4 = 4L;
  private static final String MP_ID_1 = "mp_id_1";
  private static final String MP_ID_2 = "mp_id_2";
  private static final String MP_ID_3 = "mp_id_3";
  private static final String MP_ID_4 = "mp_id_4";
  private static final long OLDER_THAN_REFERENCE_MS = 24 * 60 * 60 * 1000; // 1 day ago
  private static final String WORKFLOW_DEF = "attach-subtitles-wf";
  private static final String RETRY_WORKFLOW_DEF = "retry-subtitles-wf";

  private User user;
  private SpeechToTextWorkflowSchedulerQuartz service;
  private SpeechToTextDatabase database;
  private AssetManager assetManager;
  private WorkflowService wfService;
  private ServiceRegistry serviceRegistry;

  private DBSession db;

  private Capture<Set<String>> capturedMpIds;
  private Capture<ConfiguredWorkflow> capturedWf;

  @Before
  public void setUp() throws Exception {
    EntityManagerFactory emf = newEntityManagerFactory(SpeechToTextDatabase.PERSISTENCE_UNIT);
    DBSessionFactory dbSessionFactory = getDbSessionFactory();
    db = dbSessionFactory.createSession(emf);

    database = new SpeechToTextDatabase();
    database.setEntityManagerFactory(emf);
    database.setDBSessionFactory(dbSessionFactory);
    database.activate(null);

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("opencast_user").anyTimes();
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(bc, cc);

    DefaultOrganization org = new DefaultOrganization();
    user = SecurityUtil.createSystemUser("admin", org);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user);
    EasyMock.replay(securityService);

    wfService = EasyMock.createStrictMock(WorkflowService.class);

    assetManager = EasyMock.createNiceMock(AssetManager.class);
    EasyMock.expect(assetManager.snapshotExists(EasyMock.anyString())).andReturn(true).anyTimes();
    EasyMock.replay(assetManager);

    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andAnswer(new IAnswer<Job>() {
      @Override
      public Job answer() throws Throwable {
        JpaJob jpaJob = db.exec(namedQuery.findById(JpaJob.class, (Long) EasyMock.getCurrentArguments()[0]));
        return jpaJob.toJob();
      }
    }).anyTimes();
    EasyMock.replay(serviceRegistry);

    service = new SpeechToTextWorkflowSchedulerQuartz();

    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(SpeechToTextWorkflowSchedulerQuartz.PARAM_KEY_ENABLED, "true");
    props.put(SpeechToTextWorkflowSchedulerQuartz.PARAM_KEY_CRON_EXPR, "0 0 0 1 1 ? 2200"); // Never execute
    props.put(SpeechToTextWorkflowSchedulerQuartz.WORKFLOW, WORKFLOW_DEF);
    props.put(SpeechToTextWorkflowSchedulerQuartz.WORKFLOW_RETRY, RETRY_WORKFLOW_DEF);
    props.put(SpeechToTextWorkflowSchedulerQuartz.MAX_TRIES, "3");
    props.put(SpeechToTextWorkflowSchedulerQuartz.ABANDON_AFTER_SECS, OLDER_THAN_REFERENCE_MS / 1000L); // 24 hours

    service.setDatabase(database);
    service.setAssetManager(assetManager);
    service.setWorkflowService(wfService);
    service.bindServiceRegistry(serviceRegistry);
    service.bindSecurityService(securityService);
    service.activate(cc);
    service.updated(props);
  }

  @Test
  public void testHandleTranscriptionInProgressAllCompleted() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for the same media package with stt status running, all jobs done: one finished, the other failed
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_2);
    SpeechToTextControl stt2 = database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);
    // 1 job, same mp, but different workflow, not done yet
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_2, jpaJob3);
    // 1 job, different mp, not done yet
    JpaJob jpaJob4 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_4);
    database.storeSpeechToTextControl(MP_ID_2, WF_ID_3, jpaJob4);

    service.handleTranscriptionInProgress();

    // Check that stt status was correctly updated for jobs 1 and 2
    SpeechToTextControl stt = database.findByJob(jpaJob1);
    Assert.assertEquals(SpeechToTextControl.Status.TranscriptionDone, stt.getStatus());
    stt = database.findByJob(jpaJob2);
    Assert.assertEquals(SpeechToTextControl.Status.TranscriptionError, stt.getStatus());
    // Check that stt status was NOT updated for jobs 3 and 4
    stt = database.findByJob(jpaJob3);
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.getStatus());
    stt = database.findByJob(jpaJob4);
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.getStatus());
  }

  @Test
  public void testHandleTranscriptionInProgressNotAllCompleted() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for the same media package with stt status running, one job finished, the other still running
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_2);
    SpeechToTextControl stt2 = database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);

    service.handleTranscriptionInProgress();

    // Check that stt status was NOT updated for job 1
    SpeechToTextControl stt = database.findByJob(jpaJob1);
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.getStatus());
    // Check that stt status was updated for job 2
    stt = database.findByJob(jpaJob2);
    Assert.assertEquals(SpeechToTextControl.Status.TranscriptionError, stt.getStatus());
  }

  @Test
  public void testExpireOldTranscriptionNotDone() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for the same media package with stt status running, date older than configured interval
    Date olderDate = Date.from(Instant.now().minusMillis(OLDER_THAN_REFERENCE_MS + 5000L));
    JpaJob jpaJob1 = createJob(db, olderDate, Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    JpaJob jpaJob2 = createJob(db, olderDate, Job.Status.RUNNING, JOB_ID_2);
    SpeechToTextControl stt2 = database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);
    // 1 job for another media package running but with a recent date
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID_2, WF_ID_2, jpaJob3);
    // 1 job for another media package with stt status running, date older than configured interval
    JpaJob jpaJob4 = createJob(db, olderDate, Job.Status.FAILED, JOB_ID_4);
    database.storeSpeechToTextControl(MP_ID_3, WF_ID_3, jpaJob4);
    // 1 job for another media package with stt status workflow started, date older than configured interval
    JpaJob jpaJob5 = createJob(db, olderDate, Job.Status.FINISHED, JOB_ID_5);
    database.storeSpeechToTextControl(MP_ID_4, WF_ID_4, jpaJob5);
    database.updateStatusByJob(SpeechToTextControl.Status.WorkflowInProgress, jpaJob5);

    service.expireOldTranscriptionNotDone();

    // Check that stt status was updated for jobs 1, 2, 4, 5
    SpeechToTextControl stt = database.findByJob(jpaJob1);
    Assert.assertEquals(SpeechToTextControl.Status.TranscriptionError, stt.getStatus());
    stt = database.findByJob(jpaJob2);
    Assert.assertEquals(SpeechToTextControl.Status.TranscriptionError, stt.getStatus());
    stt = database.findByJob(jpaJob4);
    Assert.assertEquals(SpeechToTextControl.Status.TranscriptionError, stt.getStatus());
    stt = database.findByJob(jpaJob5);
    Assert.assertEquals(SpeechToTextControl.Status.Done, stt.getStatus());
    // Check that sst status was NOT updated for job 3
    stt = database.findByJob(jpaJob3);
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.getStatus());
  }

  @Test
  public void testHandleTranscriptionFinishedStartAttachWorkflow() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for a media package, both finished successfully
    // Workflow will be started
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob2);

    mockWorkflowService(true, WORKFLOW_DEF);

    service.handleTranscriptionFinished();

    // Expect that a workflow was started for that mp
    Assert.assertEquals(1, capturedMpIds.getValue().size());
    Assert.assertEquals(MP_ID_1, capturedMpIds.getValue().iterator().next());
    // Expect that the job ids were passed as workflow configuration
    Assert.assertEquals(WORKFLOW_DEF, capturedWf.getValue().getWorkflowDefinition().getId());
    Map<String, String> config = capturedWf.getValue().getParameters();
    Set<String> jobIds = Arrays.stream(config.get(JOBS_WORKFLOW_CONFIGURATION).split(",")).collect(Collectors.toSet());
    Assert.assertEquals(2, jobIds.size());
    Assert.assertTrue(jobIds.contains(Long.toString(JOB_ID_1)));
    Assert.assertTrue(jobIds.contains(Long.toString(JOB_ID_2)));

    // Expect that the status was updated in the database
    SpeechToTextControl stt = database.findByJob(jpaJob1);
    Assert.assertEquals(SpeechToTextControl.Status.WorkflowInProgress, stt.getStatus());
    stt = database.findByJob(jpaJob2);
    Assert.assertEquals(SpeechToTextControl.Status.WorkflowInProgress, stt.getStatus());
  }

  @Test
  public void testHandleTranscriptionFinishedStartRetryWorkflow() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for a media package, one finished, one failed
    // Retry workflow will be started
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionError, jpaJob2);

    mockWorkflowService(true, RETRY_WORKFLOW_DEF);

    service.handleTranscriptionFinished();

    // Expect that a workflow was started for that mp
    Assert.assertEquals(1, capturedMpIds.getValue().size());
    Assert.assertEquals(MP_ID_1, capturedMpIds.getValue().iterator().next());
    // Expect that the job ids were not passed as workflow configuration
    Assert.assertEquals(RETRY_WORKFLOW_DEF, capturedWf.getValue().getWorkflowDefinition().getId());
    Map<String, String> config = capturedWf.getValue().getParameters();
    Assert.assertNull(config.get(JOBS_WORKFLOW_CONFIGURATION));

    // Expect that the status was updated in the database
    SpeechToTextControl stt = database.findByJob(jpaJob1);
    Assert.assertEquals(SpeechToTextControl.Status.Canceled, stt.getStatus());
    stt = database.findByJob(jpaJob2);
    Assert.assertEquals(SpeechToTextControl.Status.Canceled, stt.getStatus());
  }

  @Test
  public void testHandleTranscriptionFinishedRetryWorkflowThirdAttempt() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for a media package, one finished, one failed
    // Retry workflow will be started
    // This was the first attempt
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    database.updateStatusByJob(SpeechToTextControl.Status.Canceled, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);
    database.updateStatusByJob(SpeechToTextControl.Status.Canceled, jpaJob2);

    // This was the second attempt
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_2, jpaJob3);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob3);
    JpaJob jpaJob4 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_4);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_2, jpaJob4);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionError, jpaJob4);

    mockWorkflowService(true, RETRY_WORKFLOW_DEF);

    service.handleTranscriptionFinished();

    // Expect that a workflow was started for that mp
    Assert.assertEquals(1, capturedMpIds.getValue().size());
    Assert.assertEquals(MP_ID_1, capturedMpIds.getValue().iterator().next());
    // Expect that the job ids were not passed as workflow configuration
    Assert.assertEquals(RETRY_WORKFLOW_DEF, capturedWf.getValue().getWorkflowDefinition().getId());
    Map<String, String> config = capturedWf.getValue().getParameters();
    Assert.assertNull(config.get(JOBS_WORKFLOW_CONFIGURATION));

    // Expect that the status was updated in the database
    SpeechToTextControl stt = database.findByJob(jpaJob3);
    Assert.assertEquals(SpeechToTextControl.Status.Canceled, stt.getStatus());
    stt = database.findByJob(jpaJob4);
    Assert.assertEquals(SpeechToTextControl.Status.Canceled, stt.getStatus());
  }

  @Test
  public void testHandleTranscriptionFinishedRetryWorkflowMaxTriesExceeded() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for a media package, one finished, one failed
    // This was the first attempt
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    database.updateStatusByJob(SpeechToTextControl.Status.Canceled, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);
    database.updateStatusByJob(SpeechToTextControl.Status.Canceled, jpaJob2);

    // This was the second attempt
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_2, jpaJob3);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob3);
    JpaJob jpaJob4 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_4);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_2, jpaJob4);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionError, jpaJob4);

    // This was the third attempt
    JpaJob jpaJob5 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_5);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_3, jpaJob5);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob5);
    JpaJob jpaJob6 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_6);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_3, jpaJob6);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionError, jpaJob6);

    mockWorkflowService(false, null);

    service.handleTranscriptionFinished();

    // Expect no workflow service method was called (it's a strict mock)

    // Expect that the status was updated in the database
    SpeechToTextControl stt = database.findByJob(jpaJob5);
    Assert.assertEquals(SpeechToTextControl.Status.Canceled, stt.getStatus());
    stt = database.findByJob(jpaJob6);
    Assert.assertEquals(SpeechToTextControl.Status.Canceled, stt.getStatus());
  }

  @Test
  public void testHandleTranscriptionFinishedDoNotStartWorkflow() throws Exception {
    // Create jobs and stts in database
    // 2 jobs for a media package with stt status running, one finished, one still running
    // Workflow will NOT be started
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.FINISHED, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob1);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID_1, WF_ID_1, jpaJob2);
    database.updateStatusByJob(SpeechToTextControl.Status.InProgress, jpaJob2);

    mockWorkflowService(false, null);

    service.handleTranscriptionFinished();

    // Expect no workflow service method was called (it's a strict mock)

    // Expect that the status remained the same in the database
    SpeechToTextControl stt = database.findByJob(jpaJob1);
    Assert.assertEquals(SpeechToTextControl.Status.TranscriptionDone, stt.getStatus());
    stt = database.findByJob(jpaJob2);
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.getStatus());
  }

  private void mockWorkflowService(boolean startWf, String wfDefId)
          throws NotFoundException, WorkflowDatabaseException, UnauthorizedException {
    capturedMpIds = Capture.newInstance();
    capturedWf = Capture.newInstance();

    List<WorkflowInstance> wfList = new ArrayList<WorkflowInstance>();
    if (startWf) {
      WorkflowDefinition wfDef = new WorkflowDefinitionImpl();
      wfDef.setId(wfDefId);
      EasyMock.expect(wfService.getWorkflowDefinitionById(EasyMock.anyObject(String.class))).andReturn(wfDef);
      wfList.add(new WorkflowInstance());
    }
    Workflows wfs = EasyMock.createNiceMock(Workflows.class);
    EasyMock.expect(wfs.applyWorkflowToLatestVersion(EasyMock.capture(capturedMpIds), EasyMock.capture(capturedWf)))
            .andReturn(wfList);
    service.setWfUtil(wfs);

    EasyMock.replay(wfService, wfs);
  }

}
