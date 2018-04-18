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
package org.opencastproject.transciption.ibmwatson.persistence;

import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.transcription.ibmwatson.persistence.TranscriptionDatabase;
import org.opencastproject.transcription.ibmwatson.persistence.TranscriptionJobControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

public class TranscriptionDatabaseTest {
  private TranscriptionDatabase database;

  private static final String MP_ID = "mp1";
  private static final String TRACK_ID = "track1";
  private static final String JOB_ID = "job1";
  private static final String STATUS = "status1";
  private static final long TRACK_DURATION = 60000;
  private static final String MP_ID2 = "mp2";
  private static final String TRACK_ID2 = "track2";
  private static final String JOB_ID2 = "job2";
  private static final String STATUS2 = "status2";
  private static final String MP_ID3 = "mp3";
  private static final String TRACK_ID3 = "track3";
  private static final String JOB_ID3 = "job3";

  @Before
  public void setUp() throws Exception {
    database = new TranscriptionDatabase();
    database.setEntityManagerFactory(
            newTestEntityManagerFactory("org.opencastproject.transcription.ibmwatson.persistence"));
    database.activate(null);
  }

  @After
  public void tearDown() throws SQLException {
  }

  @Test
  public void testStoreJobControl() throws Exception {
    long dt1 = System.currentTimeMillis();
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);
    long dt2 = System.currentTimeMillis();

    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
    Assert.assertEquals(STATUS, j.getStatus());
    Assert.assertEquals(TRACK_DURATION, j.getTrackDuration());
    long created = j.getDateCreated().getTime();
    Assert.assertTrue(dt1 <= created && created <= dt2);
  }

  @Test
  public void testFindByJob() throws Exception {
    long dt1 = System.currentTimeMillis();
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);
    long dt2 = System.currentTimeMillis();

    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
    Assert.assertEquals(STATUS, j.getStatus());
    long created = j.getDateCreated().getTime();
    Assert.assertTrue(dt1 <= created && created <= dt2);
  }

  @Test
  public void testFindByMediaPackage() throws Exception {
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);
    database.storeJobControl(MP_ID, TRACK_ID2, JOB_ID2, STATUS, TRACK_DURATION);
    database.storeJobControl("another_mp_id", "track3", "job3", STATUS, TRACK_DURATION);

    List<TranscriptionJobControl> list = database.findByMediaPackage(MP_ID);
    Assert.assertEquals(2, list.size());
    Assert.assertTrue(TRACK_ID.equals(list.get(0).getTrackId()) || TRACK_ID.equals(list.get(1).getTrackId()));
    Assert.assertTrue(TRACK_ID2.equals(list.get(0).getTrackId()) || TRACK_ID2.equals(list.get(1).getTrackId()));
  }

  @Test
  public void testFindByOneStatus() throws Exception {
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);
    database.storeJobControl(MP_ID2, TRACK_ID2, JOB_ID2, STATUS2, TRACK_DURATION);
    database.storeJobControl(MP_ID3, TRACK_ID3, JOB_ID3, STATUS, TRACK_DURATION);

    List<TranscriptionJobControl> list = database.findByStatus(STATUS);
    Assert.assertEquals(2, list.size());
    Assert.assertTrue(MP_ID.equals(list.get(0).getMediaPackageId()) || MP_ID.equals(list.get(1).getMediaPackageId()));
    Assert.assertTrue(MP_ID3.equals(list.get(0).getMediaPackageId()) || MP_ID3.equals(list.get(1).getMediaPackageId()));
  }

  @Test
  public void testFindByManyStatus() throws Exception {
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);
    database.storeJobControl(MP_ID2, TRACK_ID2, JOB_ID2, STATUS2, TRACK_DURATION);
    database.storeJobControl(MP_ID3, TRACK_ID3, JOB_ID3, STATUS, TRACK_DURATION);

    List<TranscriptionJobControl> list = database.findByStatus(STATUS, STATUS2);
    Assert.assertEquals(3, list.size());
  }

  @Test
  public void testDeleteJobControl() throws Exception {
    long dt1 = System.currentTimeMillis();
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);
    long dt2 = System.currentTimeMillis();

    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
    Assert.assertEquals(STATUS, j.getStatus());
    long created = j.getDateCreated().getTime();
    Assert.assertTrue(dt1 <= created && created <= dt2);

    database.deleteJobControl(JOB_ID);
    Assert.assertNull(database.findByJob(JOB_ID));
  }

  @Test
  public void testUpdateJobControl() throws Exception {
    long dt1 = System.currentTimeMillis();
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);
    long dt2 = System.currentTimeMillis();

    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
    Assert.assertEquals(STATUS, j.getStatus());
    long created = j.getDateCreated().getTime();
    Assert.assertTrue(dt1 <= created && created <= dt2);

    database.updateJobControl(JOB_ID, STATUS2);

    j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
    Assert.assertEquals(STATUS2, j.getStatus());
  }

  @Test
  public void testUpdateJobControlToTranscriptionComplete() throws Exception {
    database.storeJobControl(MP_ID, TRACK_ID, JOB_ID, STATUS, TRACK_DURATION);

    TranscriptionJobControl j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
    Assert.assertEquals(STATUS, j.getStatus());
    Assert.assertNull(j.getDateCompleted());

    long dt1 = System.currentTimeMillis();
    database.updateJobControl(JOB_ID, TranscriptionJobControl.Status.TranscriptionComplete.name());
    long dt2 = System.currentTimeMillis();

    j = database.findByJob(JOB_ID);
    Assert.assertNotNull(j);
    Assert.assertEquals(MP_ID, j.getMediaPackageId());
    Assert.assertEquals(TRACK_ID, j.getTrackId());
    Assert.assertEquals(TranscriptionJobControl.Status.TranscriptionComplete.name(), j.getStatus());
    long completed = j.getDateCompleted().getTime();
    Assert.assertTrue(dt1 <= completed && completed <= dt2);
  }

}
