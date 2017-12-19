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

package org.opencastproject.serviceregistry.impl;

import static org.junit.Assert.assertEquals;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.DispatchableComparator;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

public class DispatchableComparatorTest {

  private Comparator<JpaJob> dispatchableComparator;
  private Date dt;
  private Date dtPlusOneHour;
  private JpaJob j1;

  @Before
  public void setUp() {
    dispatchableComparator = new DispatchableComparator();

    // A date and time
    Calendar cal = new GregorianCalendar(2016, 1, 1, 1, 0, 0);
    dt = cal.getTime();
    // One hour later
    Calendar calPlusOneHour = new GregorianCalendar();
    calPlusOneHour.setTimeInMillis(cal.getTimeInMillis());
    calPlusOneHour.add(Calendar.HOUR, 1);
    dtPlusOneHour = calPlusOneHour.getTime();

    j1 = createJob(1L, "non-wf", Status.RESTART, dt);
  }

  private JpaJob createJob(long id, String type, Job.Status status, Date created) {
    JpaJob job = EasyMock.createNiceMock(JpaJob.class);
    EasyMock.expect(job.getId()).andReturn(id).anyTimes();
    EasyMock.expect(job.getJobType()).andReturn(type).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(status).anyTimes();
    EasyMock.expect(job.getDateCreated()).andReturn(created).anyTimes();

    EasyMock.replay(job);

    return job;
  }

  @Test
  public void testEquals() {
    // Test equals: same job type, same status, same date
    JpaJob j2 = createJob(2L, "non-wf", Status.RESTART, dt);
    assertEquals("Two jobs with equal type, status and creation date must be considered equal", 0,
            dispatchableComparator.compare(j1, j2));
  }

  @Test
  public void testLessThanByJobStatus() {
    // Test first less than second: same job type, different status, same date
    JpaJob j3 = createJob(3L, "non-wf", Status.QUEUED, dt);
    assertEquals("Jobs with RESTART status should be less than those with QUEUED status", -1,
            dispatchableComparator.compare(j1, j3));
  }

  @Test
  public void testLessThanByJobType() {
    // Test first less than second: different job type, same status, same date
    JpaJob j4 = createJob(4L, ServiceRegistryJpaImpl.TYPE_WORKFLOW, Status.RESTART, dt);
    assertEquals("Non-workflow jobs should be less than workflow jobs", -1, dispatchableComparator.compare(j1, j4));
  }

  @Test
  public void testLessThanByDateCreated() {
    // Test first less than second: same job type, same status, different date
    JpaJob j5 = createJob(5L, "non-wf", Status.RESTART, dtPlusOneHour);
    assertEquals("Jobs with earlier created date should be less than jobs with later created date", -1,
            dispatchableComparator.compare(j1, j5));
  }

  @Test
  public void testGreaterThanByJobStatus() {
    // Test first greater than second: same job type, different status, same date
    JpaJob j3 = createJob(3L, "non-wf", Status.QUEUED, dt);
    assertEquals("Jobs with RESTART status should be less than those with QUEUED status", 1,
            dispatchableComparator.compare(j3, j1));
  }

  @Test
  public void testGreaterThanByJobType() {
    // Test first greater than second: different job type, same status, same date
    JpaJob j4 = createJob(4L, ServiceRegistryJpaImpl.TYPE_WORKFLOW, Status.RESTART, dt);
    assertEquals("Non-workflow jobs should be less than workflow jobs", 1, dispatchableComparator.compare(j4, j1));
  }

  @Test
  public void testGreaterThanByDateCreated() {
    // Test first greater than second: same job type, same status, different date
    JpaJob j5 = createJob(5L, "non-wf", Status.RESTART, dtPlusOneHour);
    assertEquals("Jobs with earlier created date should be less than jobs with later created date", 1,
            dispatchableComparator.compare(j5, j1));
  }

}
