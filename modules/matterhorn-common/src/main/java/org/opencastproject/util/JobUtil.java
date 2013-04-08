/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.util;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.util.List;

import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.toArray;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Tuple.tuple;

/** Job related utility functions. */
public final class JobUtil {
  private JobUtil() {
  }

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, long timeout, Job... jobs) {
    JobBarrier barrier = new JobBarrier(reg, jobs);
    return barrier.waitForJobs(timeout);
  }

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, Job... jobs) {
    JobBarrier barrier = new JobBarrier(reg, jobs);
    return barrier.waitForJobs();
  }

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, long timeout, List<Job> jobs) {
    JobBarrier barrier = new JobBarrier(reg, toArray(jobs));
    return barrier.waitForJobs(timeout);
  }

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, List<Job> jobs) {
    JobBarrier barrier = new JobBarrier(reg, toArray(jobs));
    return barrier.waitForJobs();
  }

  /**
   * Returns <code>true</code> if the job is ready to be dispatched.
   * 
   * @param job
   *          the job
   * @return <code>true</code> whether the job is ready to be dispatched
   * @throws IllegalStateException
   *           if the job status is unknown
   */
  public static boolean isReadyToDispatch(Job job) throws IllegalStateException {
    switch (job.getStatus()) {
      case CANCELED:
      case DELETED:
      case FAILED:
      case FINISHED:
        return false;
      case DISPATCHING:
      case INSTANTIATED:
      case PAUSED:
      case QUEUED:
      case RESTART:
      case RUNNING:
        return true;
      default:
        throw new IllegalStateException("Found job in unknown state '" + job.getStatus() + "'");
    }
  }

  /** Check if <code>job</code> is not done yet and wait in case. */
  public static JobBarrier.Result waitForJob(ServiceRegistry reg, Option<Long> timeout, Job job) {
    final Job.Status status = job.getStatus();
    // only create a barrier if the job is not done yet
    switch (status) {
      case CANCELED:
      case DELETED:
      case FAILED:
      case FINISHED:
        return new JobBarrier.Result(map(tuple(job, status)));
      default:
        for (Long t : timeout)
          return waitForJobs(reg, t, job);
        return waitForJobs(reg, job);
    }
  }

  /**
   * Interpret the payload of a completed {@link Job} as a {@link MediaPackageElement}. Wait for the job to complete if necessary.
   *
   * @throws MediaPackageException in case the payload is not a mediapackage element
   */
  public static Function<Job, MediaPackageElement> payloadAsMediaPackageElement(final ServiceRegistry reg) {
    return new Function.X<Job, MediaPackageElement>() {
      @Override
      public MediaPackageElement xapply(Job job) throws MediaPackageException {
        waitForJob(reg, none(0L), job);
        return MediaPackageElementParser.getFromXml(job.getPayload());
      }
    };
  }
}
