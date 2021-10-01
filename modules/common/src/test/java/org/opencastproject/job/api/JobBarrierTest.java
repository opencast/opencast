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

package org.opencastproject.job.api;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Collections.toArray;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;

import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JobBarrierTest {

  private static final Logger logger = LoggerFactory.getLogger(JobBarrierTest.class);

  @Test
  public void testWaitForJobsAllFinish() throws Exception {
    for (int x = 10; x > 0; x--) {
      runWaitForJobsTest(alwaysFinish);
    }
  }

  @Test
  public void testWaitForJobsSomeFail() throws Exception {
    for (int x = 10; x > 0; x--) {
      runWaitForJobsTest(sometimesFail);
    }
  }

  private void runWaitForJobsTest(Function<Long, TestJob> jobCreator) throws Exception {
    // create a bunch of jobs
    final Map<Long, TestJob> jobs = new HashMap<Long, TestJob>();
    for (long i = (long) (Math.random() * 100.0D); i > 0; i--) {
      jobs.put(i, jobCreator.apply(i));
    }
    logger.info("Waiting for " + jobs.size() + " jobs");
    // create a service registry mock returning those jobs
    final ServiceRegistry sr = createNiceMock(ServiceRegistry.class);
    EasyMock.expect(sr.getJob(EasyMock.anyLong())).andAnswer(() -> {
      final long jobId = (Long) (EasyMock.getCurrentArguments()[0]);
      return jobs.get(jobId);
    }).anyTimes();
    EasyMock.replay(sr);
    // wait for all jobs to complete
    new JobBarrier(null, sr, 10, toArray(Job.class, jobs.values())).waitForJobs();
    // check if there are still running jobs
    final boolean noRunningJobs = mlist(jobs.values()).foldl(true, new Function2<Boolean, TestJob, Boolean>() {
      @Override
      public Boolean apply(Boolean sum, TestJob job) {
        return sum && hasJobTerminated(job.getLastReportedStatus());
      }
    });
    assertTrue("There are still some jobs running", noRunningJobs);
  }

  private static boolean hasJobTerminated(Job.Status status) {
    switch (status) {
      case CANCELLED:
      case DELETED:
      case FAILED:
      case FINISHED:
        return true;
      default:
        return false;
    }
  }

  private static Function<Long, TestJob> alwaysFinish = new Function<Long, TestJob>() {
    @Override
    public TestJob apply(Long id) {
      return new TestJob(id, System.currentTimeMillis() + 300L + (long) (Math.random() * 700.0D), Job.Status.FINISHED);
    }
  };

  private static Function<Long, TestJob> sometimesFail = new Function<Long, TestJob>() {
    @Override
    public TestJob apply(Long id) {
      return new TestJob(id, System.currentTimeMillis() + 300L + (long) (Math.random() * 700.0D),
              Math.random() < 0.95 ? Job.Status.FINISHED : Job.Status.FAILED);
    }
  };

  public static class TestJob extends JobImpl {
    private final Status endStatus;
    private long finishTime;
    private Status lastReportedStatus;

    public TestJob(long id, long finishTime, Status endStatus) {
      super(id);
      this.finishTime = finishTime;
      this.endStatus = endStatus;
    }

    @Override
    public Status getStatus() {
      lastReportedStatus = getStatusInternal();
      return lastReportedStatus;
    }

    public Status getLastReportedStatus() {
      return lastReportedStatus;
    }

    private Status getStatusInternal() {
      if (System.currentTimeMillis() > finishTime) {
        return endStatus;
      } else {
        if (lastReportedStatus == null) {
          return Status.INSTANTIATED;
        } else {
          return Status.RUNNING;
        }
      }
    }
  }
}
