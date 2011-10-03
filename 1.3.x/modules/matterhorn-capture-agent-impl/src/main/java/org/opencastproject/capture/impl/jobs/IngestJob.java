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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.impl.CaptureAgentImpl;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicLong;

public class IngestJob implements StatefulJob {
  /**
   * The class to create the manifest, then attempt to ingest the media to the remote server.
   */
  /** The prefix for the job name so that it can be found and removed easily. **/
  public static final String JOB_PREFIX = "IngestJob-";
  /** The prefix for the trigger so that it can be found and removed easily. **/
  public static final String TRIGGER_PREFIX = "IngestJobTrigger-";
  /** The default amount of time before restarting the ingestion retries in seconds. **/
  public static final String DEFAULT_PAUSE_TIME = "3600";
  /** The default number of times to try ingesting to the core before sleeping to try again. **/
  public static final String DEFAULT_RETRIES = "5";
  /** The default amount of time in seconds to wait between retrying to ingest to the core. **/
  public static final long DEFAULT_RETRY_INTERVAL = 1;
  /** The current number of retries left before a pause. **/
  public static final String RETRIES_LEFT = "Retries.Left";

  private static final Logger logger = LoggerFactory.getLogger(IngestJob.class);

  /**
   * This IngestJob creates an IngestJobRetrier that will try to ingest a configurable amount of times and the remove
   * itself waiting for IngestJob to create a new IngestJobRetrier. In this way we can have a set number of times to try
   * an ingest with pauses between them, yet if it fails this number of times we can easily have it sleep for a set
   * amount of time before attempting to retry again. {@inheritDoc}
   * 
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    logger.info("Running IngestJob");
    // Get the unique identifier that will allow this job to be found uniquely in the scheduler.
    String postfix = ctx.getMergedJobDataMap().getString(JobParameters.JOB_POSTFIX);
    // Obtain the quartz scheduler that will be used to remove this job and reschedule it.
    Scheduler scheduler = (Scheduler) ctx.getMergedJobDataMap().get(JobParameters.SCHEDULER);
    // Obtains the recordingID
    String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);
    // Obtains the CaptureAgentImpl from the context
    CaptureAgentImpl captureAgentImpl = (CaptureAgentImpl) ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);
    // Get number of retries
    AtomicLong retriesLeft = (AtomicLong) ctx.getMergedJobDataMap().get(IngestJob.RETRIES_LEFT);
    // If retries left are 0 we have to start a pause, less than 0 is the end of a pause and anything else is a retry.
    if (retriesLeft.get() == 0) {
      startPause(ctx, postfix, scheduler, recordingID, captureAgentImpl);
    } else if (retriesLeft.get() < 0) {
      endPause(ctx, postfix, scheduler, recordingID, captureAgentImpl);
    } else {
      retryIngest(ctx, recordingID, captureAgentImpl, retriesLeft);
    }
  }

  private void startPause(JobExecutionContext ctx, String postfix, Scheduler scheduler, String recordingID,
          CaptureAgentImpl captureAgentImpl) {
    logger.info("Pausing until next ingestion.");
    // Remove This Job
    removeJob(ctx, ctx.getJobDetail());
    // Reschedule it for a pause duration
    rescheduleJob(ctx.getMergedJobDataMap().getLong(CaptureParameters.INGEST_PAUSE_TIME), ctx, postfix, scheduler,
            recordingID, captureAgentImpl);
    // Decrease the retry amount
    AtomicLong retriesLeft = (AtomicLong) ctx.getMergedJobDataMap().get(IngestJob.RETRIES_LEFT);
    retriesLeft.set(retriesLeft.get() - 1);
  }

  private void rescheduleJob(long interval, JobExecutionContext ctx, String postfix, Scheduler scheduler,
          String recordingID, CaptureAgentImpl captureAgentImpl) {
    try {
      JobDetailTriggerPair jobAndTrigger = JobCreator.createInjestJob(interval, recordingID, postfix, captureAgentImpl,
              scheduler, captureAgentImpl.getConfigService());
      scheduler.scheduleJob(jobAndTrigger.getJob(), jobAndTrigger.getTrigger());
    } catch (ParseException e) {
      logger.error("Could not reschedule ingest because cron expression could not be parsed. ", e);
    } catch (SchedulerException e) {
      logger.error("Could not reschedule ingest because there was a Scheduler Exception. ", e);
    }
  }

  private void endPause(JobExecutionContext ctx, String postfix, Scheduler scheduler, String recordingID,
          CaptureAgentImpl captureAgentImpl) {
    // Remove this job
    removeJob(ctx, ctx.getJobDetail());
    // Reset the retry amount
    AtomicLong retriesLeft = (AtomicLong) ctx.getMergedJobDataMap().get(IngestJob.RETRIES_LEFT);
    retriesLeft.set(ctx.getMergedJobDataMap().getLong(CaptureParameters.INGEST_RETRY_LIMIT));
    // Schedule the job with the retry interval again
    try {
      long retryInterval;
      try {
        retryInterval = ctx.getMergedJobDataMap().getLong(CaptureParameters.INGEST_RETRY_INTERVAL);
      } catch (NullPointerException e) {
        retryInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
      }
      JobDetailTriggerPair jobAndTrigger = JobCreator.createInjestJob(retryInterval, recordingID, postfix,
              captureAgentImpl, scheduler, captureAgentImpl.getConfigService());
      scheduler.scheduleJob(jobAndTrigger.getJob(), jobAndTrigger.getTrigger());
    } catch (ParseException e) {
      logger.error("Could not reschedule ingest because cron expression could not be parsed. ", e);
    } catch (SchedulerException e) {
      logger.error("Could not reschedule ingest because there was a Scheduler Exception. ", e);
    }
    // Retry to Ingest
    retryIngest(ctx, recordingID, captureAgentImpl, retriesLeft);
  }

  private void retryIngest(JobExecutionContext ctx, String recordingID, CaptureAgentImpl captureAgentImpl,
          AtomicLong retriesLeft) {
    // Try to Ingest
    logger.info("Proceeding to try ingest");
    int result = captureAgentImpl.ingest(recordingID);
    // Decrease the Number of Retries
    retriesLeft.set(retriesLeft.get() - 1);
    if (result != HttpURLConnection.HTTP_OK) {
      // Wait until the next fire to try again.
      logger.error("Ingestion failed with a value of {}", result);
    } else {
      logger.info("Ingestion finished");
      // Remove this job from the system because we are finished.
      removeJob(ctx, ctx.getJobDetail());
    }
  }

  /**
   * Removes a job from the schedule.
   * 
   * @param ctx
   *          Used to remove the job from the correct scheduler
   * @param jobDetail
   *          Used to find all of the jobs and triggers and remove them from the scheduler.
   **/
  private void removeJob(JobExecutionContext ctx, JobDetail jobDetail) {

    try {
      if (jobDetail != null) {
        ctx.getScheduler().deleteJob(jobDetail.getName(), jobDetail.getGroup());
      } else {
        logger.warn("IngestJob: jobDetail was null - This is normal for tests but if it happens in felix please"
                + " file a bug.");
      }
    } catch (SchedulerException e) {
      logger.warn("Unable to delete ingest job {}!", jobDetail.getName(), e);
    }
  }
}
