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

package org.opencastproject.util;

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.toArray;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.data.Opt;

import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/** Job related utility functions. */
public final class JobUtil {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JobUtil.class);

  private JobUtil() {
  }

  /**
   * Update the job from the service registry and get its payload.
   *
   * @return the payload or none, if either to job cannot be found or if the job has no or an empty payload
   */
  public static Opt<String> getPayload(ServiceRegistry reg, Job job)
          throws NotFoundException, ServiceRegistryException {
    for (Job updated : update(reg, job)) {
      return Opt.nul(updated.getPayload());
    }
    return Opt.none();
  }

  /**
   * Get the latest state of a job. Does not modify the <code>job</code> parameter.
   *
   * @return the updated job or none, if it cannot be found
   */
  public static Opt<Job> update(ServiceRegistry reg, Job job) throws ServiceRegistryException {
    try {
      return Opt.some(reg.getJob(job.getId()));
    } catch (NotFoundException e) {
      return Opt.none();
    }
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval. The
   * <code>waiter</code> is the job which is waiting for the other jobs to finish.
   *
   * @param waiter
   *          the job waiting for the other jobs to finish
   * @param reg
   *          the service registry
   * @param pollingInterval
   *          the time in miliseconds between two polling operations
   * @param timeout
   *          the maximum amount of time to wait
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(Job waiter, ServiceRegistry reg, long pollingInterval, long timeout,
          Job... jobs) {
    JobBarrier barrier = new JobBarrier(waiter, reg, pollingInterval, jobs);
    return barrier.waitForJobs(timeout);
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval. The
   * <code>waiter</code> is the job which is waiting for the other jobs to finish.
   *
   * @param waiter
   *          the job waiting for the other jobs to finish
   * @param reg
   *          the service registry
   * @param timeout
   *          the maximum amount of time to wait
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(Job waiter, ServiceRegistry reg, long timeout, Job... jobs) {
    return waitForJobs(waiter, reg, JobBarrier.DEFAULT_POLLING_INTERVAL, timeout, jobs);
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval. The
   * <code>waiter</code> is the job which is waiting for the other jobs to finish.
   *
   * @param waiter
   *          the job waiting for the other jobs to finish
   * @param reg
   *          the service registry
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(Job waiter, ServiceRegistry reg, Job... jobs) {
    return waitForJobs(waiter, reg, 0L, jobs);
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval.
   *
   * @param reg
   *          the service registry
   * @param timeout
   *          the maximum amount of time to wait
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, long timeout, Job... jobs) {
    return waitForJobs(null, reg, timeout, jobs);
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval.
   *
   * @param reg
   *          the service registry
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, Job... jobs) {
    return waitForJobs(null, reg, jobs);
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval. The
   * <code>waiter</code> is the job which is waiting for the other jobs to finish.
   *
   * @param waiter
   *          the job waiting for the other jobs to finish
   * @param reg
   *          the service registry
   * @param timeout
   *          the maximum amount of time to wait
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(Job waiter, ServiceRegistry reg, long timeout, Collection<Job> jobs) {
    return waitForJobs(waiter, reg, timeout, toArray(Job.class, jobs));
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval.
   *
   * @param reg
   *          the service registry
   * @param timeout
   *          the maximum amount of time to wait
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, long timeout, Collection<Job> jobs) {
    return waitForJobs(null, reg, timeout, toArray(Job.class, jobs));
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval. The
   * <code>waiter</code> is the job which is waiting for the other jobs to finish.
   *
   * @param waiter
   *          the job waiting for the other jobs to finish
   * @param reg
   *          the service registry
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(Job waiter, ServiceRegistry reg, Collection<Job> jobs) {
    return waitForJobs(waiter, reg, toArray(Job.class, jobs));
  }

  /**
   * Waits for the result of a created barrier for <code>jobs</code>, using <code>registry</code> to poll for the
   * outcome of the monitored jobs using the default polling interval.
   *
   * @param reg
   *          the service registry
   * @param jobs
   *          the jobs to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, Collection<Job> jobs) {
    return waitForJobs(null, reg, jobs);
  }

  /** Check if <code>job</code> is not done yet and wait in case. */
  public static JobBarrier.Result waitForJob(Job waiter, ServiceRegistry reg, Option<Long> timeout, Job job) {
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
          return waitForJobs(waiter, reg, t, job);
        return waitForJobs(waiter, reg, job);
    }
  }

  /** Check if <code>job</code> is not done yet and wait in case. */
  public static JobBarrier.Result waitForJob(ServiceRegistry reg, Option<Long> timeout, Job job) {
    return waitForJob(null, reg, timeout, job);
  }

  /**
   * Check if <code>job</code> is not done yet and wait in case.
   *
   * @param waiter
   *          the job waiting for the other jobs to finish
   * @param reg
   *          the service registry
   * @param job
   *          the job to monitor
   * @return the job barrier result
   */
  public static JobBarrier.Result waitForJob(Job waiter, ServiceRegistry reg, Job job) {
    return waitForJob(waiter, reg, none(0L), job);
  }

  /** Check if <code>job</code> is not done yet and wait in case. */
  public static JobBarrier.Result waitForJob(ServiceRegistry reg, Job job) {
    return waitForJob(null, reg, none(0L), job);
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
      case WAITING:
        return true;
      default:
        throw new IllegalStateException("Found job in unknown state '" + job.getStatus() + "'");
    }
  }

  /**
   * {@link #waitForJob(org.opencastproject.serviceregistry.api.ServiceRegistry, org.opencastproject.util.data.Option, org.opencastproject.job.api.Job)}
   * as a function.
   */
  public static Function<Job, JobBarrier.Result> waitForJob(final ServiceRegistry reg, final Option<Long> timeout) {
    return waitForJob(null, reg, timeout);
  }

  /**
   * {@link #waitForJob(org.opencastproject.job.api.Job, org.opencastproject.serviceregistry.api.ServiceRegistry, org.opencastproject.util.data.Option, org.opencastproject.job.api.Job)}
   * as a function.
   */
  public static Function<Job, JobBarrier.Result> waitForJob(final Job waiter, final ServiceRegistry reg,
          final Option<Long> timeout) {
    return new Function<Job, JobBarrier.Result>() {
      @Override
      public JobBarrier.Result apply(Job job) {
        return waitForJob(waiter, reg, timeout, job);
      }
    };
  }

  /** Wait for the job to complete and return the success value. */
  public static Function<Job, Boolean> waitForJobSuccess(final Job waiter, final ServiceRegistry reg,
          final Option<Long> timeout) {
    return new Function<Job, Boolean>() {
      @Override
      public Boolean apply(Job job) {
        return waitForJob(waiter, reg, timeout, job).isSuccess();
      }
    };
  }

  /** Wait for the job to complete and return the success value. */
  public static Function<Job, Boolean> waitForJobSuccess(final ServiceRegistry reg, final Option<Long> timeout) {
    return waitForJobSuccess(null, reg, timeout);
  }

  /**
   * Interpret the payload of a completed {@link Job} as a {@link MediaPackageElement}. Wait for the job to complete if
   * necessary.
   *
   */
  public static Function<Job, MediaPackageElement> payloadAsMediaPackageElement(final Job waiter,
          final ServiceRegistry reg) {
    return new Function.X<Job, MediaPackageElement>() {
      @Override
      public MediaPackageElement xapply(Job job) throws MediaPackageException {
        waitForJob(waiter, reg, none(0L), job);
        return MediaPackageElementParser.getFromXml(job.getPayload());
      }
    };
  }

  /**
   * Interpret the payload of a completed {@link Job} as a {@link MediaPackageElement}. Wait for the job to complete if
   * necessary.
   */
  public static Function<Job, MediaPackageElement> payloadAsMediaPackageElement(final ServiceRegistry reg) {
    return payloadAsMediaPackageElement(null, reg);
  }

  public static final Function<HttpResponse, Option<Job>> jobFromHttpResponse = new Function<HttpResponse, Option<Job>>() {
    @Override
    public Option<Job> apply(HttpResponse response) {
      try {
        return some(JobParser.parseJob(response.getEntity().getContent()));
      } catch (Exception e) {
        logger.error("Error parsing Job from HTTP response", e);
        return none();
      }
    }
  };

  /** Sum up the queue time of a list of jobs. */
  public static long sumQueueTime(List<Job> jobs) {
    return $(jobs).foldl(0L, new Fn2<Long, Job, Long>() {
      @Override
      public Long apply(Long sum, Job job) {
        return sum + job.getQueueTime();
      }
    });
  }

  /** Get all jobs that are not in state {@link org.opencastproject.job.api.Job.Status#FINISHED}. */
  public static List<Job> getNonFinished(List<Job> jobs) {
    return $(jobs).filter(new Pred<Job>() {
      @Override
      public Boolean apply(Job job) {
        return !job.getStatus().equals(Status.FINISHED);
      }
    }).toList();
  }

}
