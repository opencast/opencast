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

import org.junit.Test;

import java.net.URI;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class DispatchableComparatorTest {

  @Test
  public void testDispatchableComparator() {
    Comparator<JpaJob> dispatchableComparator = new DispatchableComparator();

    // A date and time
    Calendar dt = new GregorianCalendar(2016, 1, 1, 1, 0, 0);
    // One hour later
    Calendar dtPlusOneHour = new GregorianCalendar();
    dtPlusOneHour.setTimeInMillis(dt.getTimeInMillis());
    dtPlusOneHour.add(Calendar.HOUR, 1);

    // Test equal: same job type, same status, same date
    JpaJob j1 = JpaJob.from(new TestJob(1, "non-wf", Status.RESTART, dt.getTime()));
    JpaJob j2 = JpaJob.from(new TestJob(2, "non-wf", Status.RESTART, dt.getTime()));
    assertEquals(0, dispatchableComparator.compare(j1, j2));

    // Test first less than second
    // Another status
    JpaJob j3 = JpaJob.from(new TestJob(3, "non-wf", Status.QUEUED, dt.getTime()));
    assertEquals(-1, dispatchableComparator.compare(j1, j3));
    // Another job type
    JpaJob j4 = JpaJob.from(new TestJob(4, ServiceRegistryJpaImpl.TYPE_WORKFLOW, Status.RESTART, dt.getTime()));
    assertEquals(-1, dispatchableComparator.compare(j1, j4));
    // Another date
    JpaJob j5 = JpaJob.from(new TestJob(5, "non-wf", Status.RESTART, dtPlusOneHour.getTime()));
    assertEquals(-1, dispatchableComparator.compare(j1, j5));

    // Test first greater than second
    assertEquals(1, dispatchableComparator.compare(j3, j1));
    assertEquals(1, dispatchableComparator.compare(j4, j1));
    assertEquals(1, dispatchableComparator.compare(j5, j1));
  }

  private class TestJob implements Job {
    /** The job ID */
    protected long id;

    /** The job type */
    protected String jobType;

    /** The job status */
    protected Status status;

    /** The date this job was created */
    protected Date dateCreated;

    TestJob(long id, String jobType, Status status, Date dateCreated) {
      this.id = id;
      this.jobType = jobType;
      this.status = status;
      this.dateCreated = dateCreated;
    }

    @Override
    public long getId() {
      return this.id;
    }

    @Override
    public String getCreator() {
      return null;
    }

    @Override
    public void setCreator(String creator) {
    }

    @Override
    public String getOrganization() {
      return null;
    }

    @Override
    public void setOrganization(String organization) {
    }

    @Override
    public long getVersion() {
      return 0;
    }

    @Override
    public String getJobType() {
      return this.jobType;
    }

    @Override
    public void setJobType(String jobType) {
      this.jobType = jobType;
    }

    @Override
    public String getOperation() {
      return null;
    }

    @Override
    public void setOperation(String operation) {
    }

    @Override
    public List<String> getArguments() {
      return null;
    }

    @Override
    public void setArguments(List<String> arguments) {
    }

    @Override
    public Status getStatus() {
      return this.status;
    }

    @Override
    public void setStatus(Status status) {
      this.status = status;
    }

    @Override
    public void setStatus(Status status, FailureReason reason) {
      this.status = status;
    }

    @Override
    public FailureReason getFailureReason() {
      return null;
    }

    @Override
    public String getCreatedHost() {
      return null;
    }

    @Override
    public String getProcessingHost() {
      return null;
    }

    @Override
    public void setProcessingHost(String processingHost) {
    }

    @Override
    public Date getDateCreated() {
      return this.dateCreated;
    }

    @Override
    public void setDateCreated(Date created) {
      this.dateCreated = created;
    }

    @Override
    public Date getDateStarted() {
      return null;
    }

    @Override
    public void setDateStarted(Date started) {
    }

    @Override
    public Long getQueueTime() {
      return null;
    }

    @Override
    public void setQueueTime(Long queueTime) {
    }

    @Override
    public Long getRunTime() {
      return null;
    }

    @Override
    public void setRunTime(Long runTime) {
    }

    @Override
    public Date getDateCompleted() {
      return null;
    }

    @Override
    public void setDateCompleted(Date dateCompleted) {
    }

    @Override
    public String getPayload() {
      return null;
    }

    @Override
    public void setPayload(String payload) {
    }

    @Override
    public int getSignature() {
      return 0;
    }

    @Override
    public Long getParentJobId() {
      return null;
    }

    @Override
    public void setParentJobId(Long parentJobId) {
    }

    @Override
    public Long getRootJobId() {
      return 0L;
    }

    @Override
    public boolean isDispatchable() {
      return true;
    }

    @Override
    public void setDispatchable(boolean dispatchable) {
    }

    @Override
    public URI getUri() {
      return null;
    }

    @Override
    public Float getJobLoad() {
      return null;
    }

    @Override
    public void setJobLoad(Float load) {
    }

    @Override
    public List<Long> getBlockedJobIds() {
      return null;
    }

    @Override
    public void setBlockedJobIds(List<Long> list) {
    }

    @Override
    public void removeBlockedJobsIds() {
    }

    @Override
    public Long getBlockingJobId() {
      return null;
    }

    @Override
    public void setBlockingJobId(Long jobId) {
    }

    @Override
    public void removeBlockingJobId() {
    }
  }


}
