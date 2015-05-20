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
  public static Opt<String> getPayload(ServiceRegistry reg, Job job) throws NotFoundException, ServiceRegistryException {
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

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, long timeout, Job... jobs) {
    JobBarrier barrier = new JobBarrier(reg, jobs);
    return barrier.waitForJobs(timeout);
  }

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, Job... jobs) {
    JobBarrier barrier = new JobBarrier(reg, jobs);
    return barrier.waitForJobs();
  }

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, long timeout, Collection<Job> jobs) {
    JobBarrier barrier = new JobBarrier(reg, toArray(Job.class, jobs));
    return barrier.waitForJobs(timeout);
  }

  public static JobBarrier.Result waitForJobs(ServiceRegistry reg, Collection<Job> jobs) {
    JobBarrier barrier = new JobBarrier(reg, toArray(Job.class, jobs));
    return barrier.waitForJobs();
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

  public static JobBarrier.Result waitForJob(ServiceRegistry reg, Job job) {
    return waitForJob(reg, none(0L), job);
  }

  public interface JobEnv {
    JobBarrier.Result waitForJobs(long timeout, Job... jobs);

    JobBarrier.Result waitForJobs(Job... jobs);

    JobBarrier.Result waitForJobs(long tomeout, List<Job> jobs);

    JobBarrier.Result waitForJobs(List<Job> jobs);

    JobBarrier.Result waitForJob(Option<Long> timeout, Job job);

    JobBarrier.Result waitForJob(long timeout, Job job);

    JobBarrier.Result waitForJob(Job job);
  }

  /** Create a job environment which encapsulates the needed service registry. */
  public static JobEnv jobEnv(final ServiceRegistry reg) {
    return new JobEnv() {
      @Override
      public JobBarrier.Result waitForJobs(long timeout, Job... jobs) {
        return JobUtil.waitForJobs(reg, timeout, jobs);
      }

      @Override
      public JobBarrier.Result waitForJobs(Job... jobs) {
        return JobUtil.waitForJobs(reg, jobs);
      }

      @Override
      public JobBarrier.Result waitForJobs(long timeout, List<Job> jobs) {
        return JobUtil.waitForJobs(reg, timeout, jobs);
      }

      @Override
      public JobBarrier.Result waitForJobs(List<Job> jobs) {
        return JobUtil.waitForJobs(reg, jobs);
      }

      @Override
      public JobBarrier.Result waitForJob(Option<Long> timeout, Job job) {
        return JobUtil.waitForJob(reg, timeout, job);
      }

      @Override
      public JobBarrier.Result waitForJob(long timeout, Job job) {
        return JobUtil.waitForJob(reg, some(timeout), job);
      }

      @Override
      public JobBarrier.Result waitForJob(Job job) {
        return JobUtil.waitForJob(reg, job);
      }
    };
  }

  /**
   * {@link #waitForJob(org.opencastproject.serviceregistry.api.ServiceRegistry, org.opencastproject.util.data.Option, org.opencastproject.job.api.Job)}
   * as a function.
   */
  public static Function<Job, JobBarrier.Result> waitForJob(final ServiceRegistry reg, final Option<Long> timeout) {
    return new Function<Job, JobBarrier.Result>() {
      @Override
      public JobBarrier.Result apply(Job job) {
        return waitForJob(reg, timeout, job);
      }
    };
  }

  /** Wait for the job to complete and return the success value. */
  public static Function<Job, Boolean> waitForJobSuccess(final ServiceRegistry reg, final Option<Long> timeout) {
    return new Function<Job, Boolean>() {
      @Override
      public Boolean apply(Job job) {
        return waitForJob(reg, timeout, job).isSuccess();
      }
    };
  }

  /**
   * Interpret the payload of a completed {@link Job} as a {@link MediaPackageElement}. Wait for the job to complete if
   * necessary.
   *
   * @throws MediaPackageException
   *           in case the payload is not a mediapackage element
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
      public Long ap(Long sum, Job job) {
        return sum + job.getQueueTime();
      }
    });
  }

  /** Get all jobs that are not in state {@link org.opencastproject.job.api.Job.Status#FINISHED}. */
  public static List<Job> getNonFinished(List<Job> jobs) {
    return $(jobs).filter(new Pred<Job>() {
      @Override
      public Boolean ap(Job job) {
        return !job.getStatus().equals(Status.FINISHED);
      }
    }).toList();
  }

}
