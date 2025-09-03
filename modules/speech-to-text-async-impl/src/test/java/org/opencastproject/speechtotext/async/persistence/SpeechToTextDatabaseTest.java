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
package org.opencastproject.speechtotext.async.persistence;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;
import static org.opencastproject.speechtotext.async.persistence.SpeechToTextTestUtil.createJob;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.jpa.JpaJob;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;

public class SpeechToTextDatabaseTest {
  private static final long JOB_ID_1 = 1L;
  private static final long JOB_ID_2 = 2L;
  private static final long JOB_ID_3 = 3L;
  private static final long JOB_ID_4 = 4L;
  private static final long WF_ID_1 = 1L;
  private static final long WF_ID_2 = 2L;
  private static final long WF_ID_3 = 3L;
  private static final String MP_ID = "media_package_id";

  private static final long REFERENCE_MS = 3 * 24 * 60 * 60 * 1000; // 3 days ago in ms
  private static final long OLDER_THAN_REFERENCE_MS = 4 * 24 * 60 * 60 * 1000; // 4 days ago in ms

  private DBSession db;

  private SpeechToTextDatabase database;

  @Before
  public void setUp() {
    EntityManagerFactory emf = newEntityManagerFactory(SpeechToTextDatabase.PERSISTENCE_UNIT);
    DBSessionFactory dbSessionFactory = getDbSessionFactory();
    db = dbSessionFactory.createSession(emf);

    database = new SpeechToTextDatabase();
    database.setEntityManagerFactory(emf);
    database.setDBSessionFactory(dbSessionFactory);
    database.activate(null);
  }

  @Test
  public void testFindByJob() throws Exception {
    JpaJob jpaJob = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob);

    SpeechToTextControl stt = database.findByJob(jpaJob);
    Assert.assertNotNull(stt);
    Assert.assertEquals(MP_ID, stt.getMediaPackageId());
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.getStatus());
  }

  @Test
  public void testFindByStatus() throws Exception {
    JpaJob jpaJob = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob);

    List<SpeechToTextControl> stts = database.findByStatus(SpeechToTextControl.Status.InProgress);
    Assert.assertEquals(1, stts.size());
    SpeechToTextControl stt = stts.get(0);
    Assert.assertEquals(MP_ID, stt.getMediaPackageId());
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.getStatus());
  }

  @Test
  public void testFindByWorkflowId() throws Exception {
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob2);
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob3);
    // Another workflow id
    JpaJob jpaJob4 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_4);
    database.storeSpeechToTextControl(MP_ID, WF_ID_2, jpaJob4);

    List<SpeechToTextControl> stts = database.findByWorkflowId(WF_ID_1);
    Assert.assertEquals(3, stts.size());
    Assert.assertTrue(stts.stream().noneMatch(stt -> stt.getJob().getId() == JOB_ID_4));
  }

  @Test
  public void testFindDistinctWorkflowIdByStatus() throws Exception {
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob1);
    // Same workflow id
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob2);
    // Another workflow id
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID, WF_ID_2, jpaJob3);
    // All above have the same status
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob1, jpaJob2, jpaJob3);
    // Another workflow id, another status
    JpaJob jpaJob4 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_4);
    database.storeSpeechToTextControl(MP_ID, WF_ID_3, jpaJob4);

    List<Long> wfIds = database.findDistinctWorkflowIdByStatus(SpeechToTextControl.Status.TranscriptionDone);
    Assert.assertEquals(2, wfIds.size());
    Assert.assertTrue(wfIds.contains(WF_ID_1));
    Assert.assertTrue(wfIds.contains(WF_ID_2));
  }

  @Test
  public void testTransitionStatusByDate() throws Exception {
    Date olderDate = Date.from(Instant.now().minusMillis(OLDER_THAN_REFERENCE_MS));
    Date referenceDate = Date.from(Instant.now().minusMillis(REFERENCE_MS));

    // In progress state
    JpaJob jpaJob1 = createJob(db, olderDate, Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob1);
    // Transcription done state
    JpaJob jpaJob2 = createJob(db, olderDate, Job.Status.RUNNING, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID, WF_ID_2, jpaJob2);
    database.updateStatusByJob(SpeechToTextControl.Status.TranscriptionDone, jpaJob2);
    // In progress state, current date
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID, WF_ID_3, jpaJob3);

    // Assert changed
    database.transitionStatusByDate(SpeechToTextControl.Status.Done, referenceDate,
            SpeechToTextControl.Status.TranscriptionDone, SpeechToTextControl.Status.InProgress);
    SpeechToTextControl stt1 = database.findByJob(jpaJob1);
    Assert.assertEquals(SpeechToTextControl.Status.Done, stt1.getStatus());
    SpeechToTextControl stt2 = database.findByJob(jpaJob2);
    Assert.assertEquals(SpeechToTextControl.Status.Done, stt2.getStatus());
    // Assert unchanged
    SpeechToTextControl stt3 = database.findByJob(jpaJob3);
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt3.getStatus());
  }

  @Test
  public void testDeleteByWorkflow() throws Exception {
    // Add some jobs and stt controls
    JpaJob jpaJob1 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_1);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob1);
    JpaJob jpaJob2 = createJob(db, new Date(), Job.Status.FAILED, JOB_ID_2);
    database.storeSpeechToTextControl(MP_ID, WF_ID_1, jpaJob2);
    // Add one for same media package, different workflow
    JpaJob jpaJob3 = createJob(db, new Date(), Job.Status.RUNNING, JOB_ID_3);
    database.storeSpeechToTextControl(MP_ID, WF_ID_2, jpaJob3);

    database.deleteByWorkflow(WF_ID_1);

    // Assert that the stt controls were deleted
    List<SpeechToTextControl> stts = database.findByWorkflowId(WF_ID_1);
    Assert.assertEquals(0, stts.size());
    // Assert that the stt controls for the other workflow was kept
    stts = database.findByWorkflowId(WF_ID_2);
    Assert.assertEquals(1, stts.size());
    Assert.assertEquals(jpaJob3.getId(), stts.get(0).getId());
  }

}
