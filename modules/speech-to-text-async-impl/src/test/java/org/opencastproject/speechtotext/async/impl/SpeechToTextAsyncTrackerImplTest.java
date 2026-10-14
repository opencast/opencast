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
import static org.opencastproject.speechtotext.async.persistence.SpeechToTextTestUtil.createJob;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.speechtotext.async.persistence.SpeechToTextControl;
import org.opencastproject.speechtotext.async.persistence.SpeechToTextDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;

public class SpeechToTextAsyncTrackerImplTest {
  private static final long JOB_ID_1 = 1L;
  private static final long JOB_ID_2 = 2L;
  private static final long WF_ID = 1L;
  private static final String MP_ID = "media_package_id";

  private DBSession db;

  private SpeechToTextDatabase database;
  private SpeechToTextAsyncTrackerImpl tracker;

  @Before
  public void setUp() {
    EntityManagerFactory emf = newEntityManagerFactory(SpeechToTextDatabase.PERSISTENCE_UNIT);
    DBSessionFactory dbSessionFactory = getDbSessionFactory();
    db = dbSessionFactory.createSession(emf);

    database = new SpeechToTextDatabase();
    database.setEntityManagerFactory(emf);
    database.setDBSessionFactory(dbSessionFactory);
    database.activate(null);

    tracker = new SpeechToTextAsyncTrackerImpl();
    tracker.setSpeechToTextDatabase(database);
  }

  @Test
  public void testTrack() throws Exception {
    JpaJob job1 = createJob(db, new Date(), Job.Status.INSTANTIATED, JOB_ID_1);
    tracker.track(job1.toJob(), MP_ID, WF_ID);
    JpaJob job2 = createJob(db, new Date(), Job.Status.INSTANTIATED, JOB_ID_2);
    tracker.track(job2.toJob(), MP_ID, WF_ID);

    List<SpeechToTextControl> stt = database.findByWorkflowId(WF_ID);
    Assert.assertEquals(2, stt.size());
    Assert.assertEquals(MP_ID, stt.get(0).getMediaPackageId());
    Assert.assertEquals(MP_ID, stt.get(1).getMediaPackageId());
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.get(0).getStatus());
    Assert.assertEquals(SpeechToTextControl.Status.InProgress, stt.get(1).getStatus());
  }

  @Test
  public void testUntrack() throws Exception {
    JpaJob job1 = createJob(db, new Date(), Job.Status.INSTANTIATED, JOB_ID_1);
    tracker.track(job1.toJob(), MP_ID, WF_ID);

    tracker.untrack(job1.toJob());

    SpeechToTextControl stt = database.findByJob(job1);
    Assert.assertNotNull(stt);
    Assert.assertEquals(MP_ID, stt.getMediaPackageId());
    Assert.assertEquals(SpeechToTextControl.Status.Done, stt.getStatus());
  }

}
