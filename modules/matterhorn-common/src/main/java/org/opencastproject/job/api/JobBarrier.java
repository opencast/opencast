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
package org.opencastproject.job.api;

import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.JobCanceledException;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is a utility implementation that will wait for one or more jobs to change their status to either one of:
 * <ul>
 * <li>{@link Job.Status#FINISHED}</li>
 * <li>{@link Job.Status#FAILED}</li>
 * <li>{@link Job.Status#DELETED}</li>
 * </ul>
 */
public class JobBarrier {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(JobBarrier.class);

  /** Default polling interval is 5 seconds */
  protected static final long DEFAULT_POLLING_INTERVAL = 5000L;

  /** The service registry used to do the polling */
  protected ServiceRegistry serviceRegistry = null;

  /** Time in milliseconds between two pools for the job status */
  protected long pollingInterval = DEFAULT_POLLING_INTERVAL;

  /** An exception that might have been thrown while polling */
  protected Throwable pollingException = null;

  /** The jobs to wait on */
  protected List<Job> jobs = null;

  /** The status map */
  protected Result status = null;

  /**
   * Creates a barrier without any jobs, using <code>registry</code> to poll for the outcome of the monitored jobs using
   * the default polling interval {@link #DEFAULT_POLLING_INTERVAL}. Use {@link #addJob(Job)} to add jobs to monitor.
   * 
   * @param registry
   *          the registry
   */
  public JobBarrier(ServiceRegistry registry) {
    this(registry, DEFAULT_POLLING_INTERVAL, new Job[] {});
  }

  /**
   * Creates a barrier for <code>jobs</code>, using <code>registry</code> to poll for the outcome of the monitored jobs
   * using the default polling interval {@link #DEFAULT_POLLING_INTERVAL}.
   * 
   * @param registry
   *          the registry
   * @param jobs
   *          the jobs to monitor
   */
  public JobBarrier(ServiceRegistry registry, Job... jobs) {
    this(registry, DEFAULT_POLLING_INTERVAL, jobs);
  }

  /**
   * Creates a wrapper for <code>job</code>, using <code>registry</code> to poll for the job outcome.
   * 
   * @param registry
   *          the registry
   * @param pollingInterval
   *          the time in miliseconds between two polling operations
   */
  public JobBarrier(ServiceRegistry registry, long pollingInterval) {
    this(registry, pollingInterval, new Job[] {});
  }

  /**
   * Creates a wrapper for <code>job</code>, using <code>registry</code> to poll for the job outcome.
   * 
   * @param job
   *          the job to poll
   * @param registry
   *          the registry
   * @param pollingInterval
   *          the time in miliseconds between two polling operations
   */
  public JobBarrier(ServiceRegistry registry, long pollingInterval, Job... jobs) {
    if (registry == null)
      throw new IllegalArgumentException("Service registry must not be null");
    if (jobs == null)
      throw new IllegalArgumentException("Jobs must not be null");
    if (pollingInterval < 0)
      throw new IllegalArgumentException("Polling interval must be a positive number");
    this.serviceRegistry = registry;
    this.pollingInterval = pollingInterval;
    this.jobs = new ArrayList<Job>(Arrays.asList(jobs));
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
   */
  public Result waitForJobs(long timeout) throws IllegalStateException {
    if (jobs.size() == 0)
      throw new IllegalArgumentException("No jobs have been submitted");
    synchronized (this) {
      JobStatusUpdater updater = new JobStatusUpdater(timeout);
      try {
        updater.start();
        wait();
      } catch (InterruptedException e) {
        logger.debug("Interrupted while waiting for job");
      }
    }
    if (pollingException != null)
      throw new IllegalStateException(pollingException);
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
   * Sets the outcome of the variuos jobs that were monitored.
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

  /**
   * Thread that keeps polling for status changes.
   */
  class JobStatusUpdater extends Thread {

    /** Maximum wait in milliseconds or 0 for unlimited waiting */
    private long workTime = 0;

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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

      long endTime = 0;
      if (workTime > 0) {
        endTime = System.currentTimeMillis() + workTime;
      }

      while (true) {
        boolean allDone = true;
        boolean failedOrDeleted = false;
        Map<Job, Job.Status> status = new HashMap<Job, Job.Status>();

        long time = System.currentTimeMillis();

        // Wait a little..
        try {
          long timeToSleep = Math.min(pollingInterval, Math.abs(endTime - time));
          Thread.sleep(timeToSleep);
        } catch (InterruptedException e) {
          logger.debug("Job polling thread was interrupted");
          return;
        }

        // Look at all jobs and make sure all of them have reached the expected status
        for (Job job : jobs) {

          // Don't ask if we already know
          if (status.containsKey(job))
            continue;

          boolean jobFinished = false;

          // Get the job status from the service registry
          try {
            Job processedJob = serviceRegistry.getJob(job.getId());
            Job.Status jobStatus = processedJob.getStatus();
            switch (jobStatus) {
              case CANCELED:
                throw new JobCanceledException(processedJob);
              case DELETED:
              case FAILED:
                failedOrDeleted = true;
                jobFinished = true;
                break;
              case FINISHED:
                jobFinished = true;
                break;
              case PAUSED:
              case QUEUED:
              case RESTART:
              case DISPATCHING:
              case RUNNING:
                logger.trace("{} is still in the works", job);
                allDone = false;
                if (workTime == 0 || endTime < time)
                  continue;
                break;
              default:
                logger.error("Unhandled job status '{}' found", jobStatus);
                break;
            }

            // Are we done with this job?
            if (jobFinished) {
              job.setStatus(jobStatus);
              job.setPayload(processedJob.getPayload());
              status.put(job, jobStatus);
            }

          } catch (NotFoundException e) {
            pollingException = e;
            break;
          } catch (ServiceRegistryException e) {
            logger.warn("Error polling service registry for the status of {}: {}", job, e.getMessage());
            allDone = false;
          } catch (Throwable t) {
            logger.error("An unexpected error occured while waiting for jobs", t);
            pollingException = t;
            updateAndNotify(status);
            return;
          }

        }

        // Are we done already?
        if (allDone || failedOrDeleted || endTime >= time) {
          updateAndNotify(status);
          return;
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

  /**
   * Result of a waiting operation on a certain number of jobs.
   */
  public class Result {

    /** The outcome of this barrier */
    private Map<Job, Job.Status> status = null;

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
      for (Job.Status state : status.values()) {
        if (!state.equals(Job.Status.FINISHED))
          return false;
      }
      return true;
    }

  }

}
