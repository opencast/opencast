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

import org.opencastproject.job.api.Job.Status;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.JobCanceledException;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is a utility implementation that will wait for all given jobs to change their status to either one of:
 * <ul>
 * <li>{@link Job.Status#FINISHED}</li>
 * <li>{@link Job.Status#FAILED}</li>
 * <li>{@link Job.Status#DELETED}</li>
 * </ul>
 */
public final class JobBarrier {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(JobBarrier.class);

  /** Default polling interval is 5 seconds */
  public static final long DEFAULT_POLLING_INTERVAL = 5000L;

  /** The service registry used to do the polling */
  private final ServiceRegistry serviceRegistry;

  /** Time in milliseconds between two pools for the job status */
  private final long pollingInterval;

  /** The job that's waiting */
  private final Opt<Long> waiterJobId;

  /** The jobs to wait on */
  private final List<Job> jobs;

  /** An exception that might have been thrown while polling */
  private volatile Throwable pollingException = null;

  /** The status map */
  private volatile Result status = null;

  /**
   * Creates a barrier without any jobs, using <code>registry</code> to poll for the outcome of the monitored jobs using
   * the default polling interval {@link #DEFAULT_POLLING_INTERVAL}. The <code>waiter</code> is the job which is waiting
   * for the other jobs to finish. Use {@link #addJob(Job)} to add jobs to monitor.
   *
   * @param registry
   *          the registry
   * @param waiter
   *          the job waiting for the other jobs to finish
   */
  public JobBarrier(Job waiter, ServiceRegistry registry) {
    this(waiter, registry, DEFAULT_POLLING_INTERVAL, new Job[] {});
  }

  /**
   * Creates a barrier for <code>jobs</code>, using <code>registry</code> to poll for the outcome of the monitored jobs
   * using the default polling interval {@link #DEFAULT_POLLING_INTERVAL}. The <code>waiter</code> is the job which is
   * waiting for the other jobs to finish.
   *
   * @param registry
   *          the registry
   * @param jobs
   *          the jobs to monitor
   * @param waiter
   *          the job waiting for the other jobs to finish
   */
  public JobBarrier(Job waiter, ServiceRegistry registry, Job... jobs) {
    this(waiter, registry, DEFAULT_POLLING_INTERVAL, jobs);
  }

  /**
   * Creates a wrapper for <code>job</code>, using <code>registry</code> to poll for the job outcome. The
   * <code>waiter</code> is the job which is waiting for the other jobs to finish.
   *
   * @param registry
   *          the registry
   * @param pollingInterval
   *          the time in miliseconds between two polling operations
   * @param waiter
   *          the job waiting for the other jobs to finish
   */
  public JobBarrier(Job waiter, ServiceRegistry registry, long pollingInterval) {
    this(waiter, registry, pollingInterval, new Job[] {});
  }

  /**
   * Creates a wrapper for <code>job</code>, using <code>registry</code> to poll for the job outcome. The
   * <code>waiter</code> is the job which is waiting for the other jobs to finish.
   *
   * @param jobs
   *          the job to poll
   * @param registry
   *          the registry
   * @param pollingInterval
   *          the time in miliseconds between two polling operations
   * @param waiter
   *          the job waiting for the other jobs to finish
   */
  public JobBarrier(Job waiter, ServiceRegistry registry, long pollingInterval, Job... jobs) {
    if (registry == null)
      throw new IllegalArgumentException("Service registry must not be null");
    if (jobs == null)
      throw new IllegalArgumentException("Jobs must not be null");
    if (pollingInterval < 0)
      throw new IllegalArgumentException("Polling interval must be a positive number");
    this.serviceRegistry = registry;
    this.pollingInterval = pollingInterval;
    if (waiter != null)
      this.waiterJobId = Opt.some(waiter.getId());
    else
      this.waiterJobId = Opt.none();
    this.jobs = new ArrayList<Job>(Arrays.asList(jobs));
  }

  private void suspendWaiterJob() {
    if (this.waiterJobId.isSome()) {
      try {
        final Job waiter = serviceRegistry.getJob(waiterJobId.get());
        waiter.setStatus(Job.Status.WAITING);
        logger.debug("Job {} set to WAITING state.", waiter.getId());
        this.serviceRegistry.updateJob(waiter);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to put {} into a waiting state, this may cause a deadlock: {}", waiterJobId, e.getMessage());
      } catch (NotFoundException e) {
        logger.warn("Unable to put {} into a waiting state, job not found by the service registry.  This may cause a deadlock: {}", waiterJobId, e.getMessage());
      }
    } else {
      logger.debug("No waiting job set, unable to put waiting job into waiting state");
    }
  }

  private void wakeWaiterJob() {
    if (this.waiterJobId.isSome()) {
      try {
        final Job waiter = serviceRegistry.getJob(waiterJobId.get());
        waiter.setStatus(Job.Status.RUNNING);
        logger.debug("Job {} wakened and set back to RUNNING state.", waiter.getId());
        this.serviceRegistry.updateJob(waiter);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to put {} into a waiting state, this may cause a deadlock: {}", waiterJobId, e.getMessage());
      } catch (NotFoundException e) {
        logger.warn("Unable to put {} into a waiting state, job not found by the service registry.  This may cause a deadlock: {}", waiterJobId, e.getMessage());
      }
    } else {
      logger.debug("No waiting job set, unable to put waiting job into waiting state");
    }
  }

  /**
   * Waits for a status change and returns the new status.
   *
   * @return the status
   */
  public Result waitForJobs() {
    return waitForJobs(0);
  }

  /**
   * Waits for a status change on all jobs and returns. If waiting for the status exceeds a certain limit, the method
   * returns even if some or all of the jobs are not yet finished. The same is true if at least one of the jobs fails or
   * gets stopped or deleted.
   *
   * @param timeout
   *          the maximum amount of time to wait
   * @throws IllegalStateException
   *           if there are no jobs to wait for
   * @throws JobCanceledException
   *           if one of the jobs was canceled
   */
  public Result waitForJobs(long timeout) throws JobCanceledException, IllegalStateException {
    if (jobs.size() == 0)
      return new Result(new HashMap<Job, Status>());
    this.suspendWaiterJob();
    synchronized (this) {
      JobStatusUpdater updater = new JobStatusUpdater(timeout);
      try {
        updater.start();
        wait();
      } catch (InterruptedException e) {
        logger.debug("Interrupted while waiting for job");
      }
    }
    if (pollingException != null) {
      if (pollingException instanceof JobCanceledException)
        throw (JobCanceledException) pollingException;
      throw new IllegalStateException(pollingException);
    }
    this.wakeWaiterJob();
    return getStatus();
  }

  /**
   * Adds the job to the list of jobs to wait for. An {@link IllegalStateException} is thrown if the barrier has already
   * been asked to wait for jobs by calling {@link #waitForJobs()}.
   *
   * @param job
   *          the job
   * @throws IllegalStateException
   *           if the barrier already started waiting
   */
  public void addJob(Job job) throws IllegalStateException {
    if (job == null)
      throw new IllegalArgumentException("Job must not be null");
    jobs.add(job);
  }

  /**
   * Sets the outcome of the various jobs that were monitored.
   *
   * @param status
   *          the status
   */
  void setStatus(Result status) {
    this.status = status;
  }

  /**
   * Returns the resulting status map.
   *
   * @return the status of the individual jobs
   */
  public Result getStatus() {
    return status;
  }

  /** Thread that keeps polling for status changes. */
  class JobStatusUpdater extends Thread {
    /** Maximum wait in milliseconds or 0 for unlimited waiting */
    private final long workTime;

    /**
     * Creates a new status updater that will wait for finished jobs. If <code>0</code> is passed in as the work time,
     * the updater will wait as long as it takes. Otherwise, it will stop after the indicated amount of time has passed.
     *
     * @param workTime
     *          the work time
     */
    JobStatusUpdater(long workTime) {
      this.workTime = workTime;
    }

    @Override
    public void run() {
      final long endTime = workTime > 0 ? System.currentTimeMillis() + workTime : 0;
      final Map<Job, Job.Status> finishedJobs = new HashMap<Job, Job.Status>();
      while (true) {
        final long time = System.currentTimeMillis();
        // Wait a little..
        try {
          final long timeToSleep = Math.min(pollingInterval, Math.abs(endTime - time));
          Thread.sleep(timeToSleep);
        } catch (InterruptedException e) {
          logger.debug("Job polling thread was interrupted");
          return;
        }
        // Look at all jobs and make sure all of them have reached the expected status
        for (final Job job : jobs) {
          // Don't ask if we already know
          if (!finishedJobs.containsKey(job)) {
            // Get the job status from the service registry
            try {
              final Job processedJob = serviceRegistry.getJob(job.getId());
              final Job.Status jobStatus = processedJob.getStatus();
              switch (jobStatus) {
                case CANCELED:
                  throw new JobCanceledException(processedJob);
                case DELETED:
                case FAILED:
                case FINISHED:
                  job.setStatus(jobStatus);
                  job.setPayload(processedJob.getPayload());
                  finishedJobs.put(job, jobStatus);
                  break;
                case PAUSED:
                case QUEUED:
                case RESTART:
                case DISPATCHING:
                case INSTANTIATED:
                case RUNNING:
                  logger.trace("{} is still in the works", job);
                  break;
                case WAITING:
                  logger.trace("{} is waiting", job);
                  break;
                default:
                  logger.error("Unhandled job status '{}' found", jobStatus);
                  break;
              }
            } catch (NotFoundException e) {
              logger.warn("Error polling job {}: Not found!", job);
              finishedJobs.put(job, Job.Status.DELETED);
              pollingException = e;
              break;
            } catch (ServiceRegistryException e) {
              logger.warn("Error polling service registry for the status of {}: {}", job, e.getMessage());
            } catch (JobCanceledException e) {
              logger.warn("Job {} got canceled", job);
              pollingException = e;
              updateAndNotify(finishedJobs);
              return;
            } catch (Throwable t) {
              logger.error("An unexpected error occured while waiting for jobs", t);
              pollingException = t;
              updateAndNotify(finishedJobs);
              return;
            }
          }

          // Are we done already?
          if (finishedJobs.size() == jobs.size()) {
            updateAndNotify(finishedJobs);
            return;
          } else if (workTime > 0 && endTime >= time) {
            pollingException = new InterruptedException("Timeout waiting for job processing");
            updateAndNotify(finishedJobs);
            return;
          }
        }
      }
    }

    /**
     * Notifies listeners about the status change.
     *
     * @param status
     *          the status
     */
    private void updateAndNotify(Map<Job, Job.Status> status) {
      JobBarrier.this.setStatus(new Result(status));
      synchronized (JobBarrier.this) {
        JobBarrier.this.notifyAll();
      }
    }

  }

  /** Result of a waiting operation on a certain number of jobs. */
  public static class Result {
    /** The outcome of this barrier */
    private final Map<Job, Job.Status> status;

    /**
     * Creates a new job barrier result.
     *
     * @param status
     *          the barrier outcome
     */
    public Result(Map<Job, Job.Status> status) {
      this.status = status;
    }

    /**
     * Returns the status details.
     *
     * @return the status details
     */
    public Map<Job, Job.Status> getStatus() {
      return status;
    }

    /**
     * Returns <code>true</code> if all jobs are in the <code>{@link Job.Status#FINISHED}</code> state.
     *
     * @return <code>true</code> if all jobs are finished
     */
    public boolean isSuccess() {
      for (final Job.Status state : status.values()) {
        if (!state.equals(Job.Status.FINISHED))
          return false;
      }
      return true;
    }
  }
}
