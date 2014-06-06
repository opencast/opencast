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

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

/**
 * The class to schedule the task of serializing the MediaPackage (this means: obtaining an XML representation) and
 * zipping it.
 *
 */
public class SerializeJob implements Job {

  public static final String JOB_PREFIX = "SerializeJob-";
  public static final String TRIGGER_PREFIX = "SerializeJobTrigger-";

  private static final Logger logger = LoggerFactory.getLogger(SerializeJob.class);

  /**
   * Generates a manifest file then zips everything up so it can be ingested. Also schedules a IngestJob. {@inheritDoc}
   *
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  @Override
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    logger.debug("Initiating serializeJob");

    // Obtains the recordingID
    String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);

    // Obtains the CaptureAgentImpl from the context
    CaptureAgentImpl ca = (CaptureAgentImpl) ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);

    // The scheduler to use when scheduling the next job
    Scheduler sched = (Scheduler) ctx.getMergedJobDataMap().get(JobParameters.SCHEDULER);

    // Creates manifest
    boolean manifestCreated;
    try {
      manifestCreated = ca.createManifest(recordingID);
    } catch (NoSuchAlgorithmException e1) {
      logger.error("Unable to create manifest, NoSuchAlgorithmException was thrown: {}.", e1);
      throw new JobExecutionException("Unable to create manifest, NoSuchAlgorithmException was thrown.");
    } catch (IOException e1) {
      logger.error("Unable to create manifest, IOException was thrown: {}.", e1);
      throw new JobExecutionException("Unable to create manifest, IOException was thrown.");
    }

    if (!manifestCreated) {
      throw new JobExecutionException("Unable to create manifest properly, serialization job failed but will retry.");
    }

    logger.info("Manifest created");

    // Zips files
    ca.zipFiles(recordingID);

    logger.info("Files zipped");

    String postfix = ctx.getMergedJobDataMap().getString(JobParameters.JOB_POSTFIX);

    scheduleIngest(recordingID, ca, sched, postfix);

    removeJob(ctx);
  }

  private void scheduleIngest(String recordingID, CaptureAgentImpl ca, Scheduler sched, String postfix) {
    try {
      long retryInterval;
      try {
        retryInterval = Long.parseLong(ca.getConfigService().getItem(CaptureParameters.INGEST_RETRY_INTERVAL));
      } catch (NullPointerException e) {
        logger.warn(CaptureParameters.INGEST_RETRY_INTERVAL + " was null so the default "
                + IngestJob.DEFAULT_RETRY_INTERVAL + " will be used.", e);
        retryInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
      } catch (NumberFormatException e) {
        logger.warn(CaptureParameters.INGEST_RETRY_INTERVAL + " was an invalid number "
                + ca.getConfigService().getItem(CaptureParameters.INGEST_RETRY_INTERVAL) + "so the default "
                + IngestJob.DEFAULT_RETRY_INTERVAL + " will be used.", e);
        retryInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
      }

      JobDetailTriggerPair jobAndTrigger = JobCreator.createInjestJob(retryInterval, recordingID, postfix, ca, sched,
              ca.getConfigService());
      sched.scheduleJob(jobAndTrigger.getJob(), jobAndTrigger.getTrigger());
    } catch (ParseException e) {
      logger.error("Invalid argument for CronTrigger: {}", e);
      e.printStackTrace();
    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e);
      e.printStackTrace();
    }
  }

  private void removeJob(JobExecutionContext ctx) {
    // Remove this job from the system
    JobDetail mine = ctx.getJobDetail();
    try {
      if (!ctx.getScheduler().isShutdown()) {
        ctx.getScheduler().deleteJob(mine.getName(), mine.getGroup());
      }
    } catch (SchedulerException e) {
      logger.warn("Unable to delete serialize job {}!", mine.getName());
      e.printStackTrace();
    }
  }

}
