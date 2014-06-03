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
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.impl.ConfigurationManager;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicLong;

public final class JobCreator {

  private static final Logger logger = LoggerFactory.getLogger(JobCreator.class);

  /**
   * This constructor prevents instantiating this utility class.
   */
  private JobCreator() {
    // Nothing to be done here
  }

  /**
   * Creates a job and trigger that tries to ingest captures to the core. Please see the classes IngestJob and
   * IngestJobRetrier for more details.
   *
   * @throws ParseException
   *           if the cron expression can't be parsed, usually caused by an invalid pauseInterval.
   * @param cronSeconds
   *          How many seconds to run the cron job.
   * @param recordingID
   *          The unique identifier of the recording
   * @param postfix
   *          The unique identifier ending of the job so that multiple ingests can occur at once. Is usually the
   *          recordingID
   * @param captureAgent
   *          The captureAgent to call ingest on when it is time to ingest the recording.
   * @param sched
   *          The quartz scheduler that this job will be scheduled into. This function doesn't schedule the job, but
   *          will need the job scheduler in the future to remove itself.
   * @return Returns a JobDetailTriggerPair that is the JobDetails of the ingest job and the Trigger to create the
   *         ingest job.
   **/
  public static JobDetailTriggerPair createInjestJob(long cronSeconds, String recordingID, String postfix,
          CaptureAgent captureAgent, Scheduler sched, ConfigurationManager configurationManager) throws ParseException {
    long cronInterval = -1;
    if (cronSeconds <= 0) {
      cronInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
    } else {
      cronInterval = cronSeconds;
    }
    // Create job
    JobDetail job = new JobDetail(IngestJob.JOB_PREFIX + postfix, JobParameters.SUPPORT_TYPE, IngestJob.class);
    // Create trigger .
    CronTrigger trigger;
    trigger = new CronTrigger();
    trigger.setGroup(JobParameters.SUPPORT_TYPE);
    trigger.setName(IngestJob.TRIGGER_PREFIX + postfix);
    trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    trigger.setCronExpression("0/" + cronInterval + " * * * * ?");
    trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgent);
    trigger.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);
    trigger.getJobDataMap().put(JobParameters.JOB_POSTFIX, postfix);
    trigger.getJobDataMap().put(JobParameters.SCHEDULER, sched);
    trigger.getJobDataMap().put(CaptureParameters.INGEST_PAUSE_TIME, getPauseInterval(configurationManager));
    trigger.getJobDataMap().put(CaptureParameters.INGEST_RETRY_INTERVAL, getRetryInterval(configurationManager));
    // Get the number of retries to allow.
    long retryLimit = getRetryLimit(configurationManager);
    trigger.getJobDataMap().put(CaptureParameters.INGEST_RETRY_LIMIT, retryLimit);
    // Set the number of retries to start with.
    AtomicLong retriesLeft = new AtomicLong(retryLimit);
    trigger.getJobDataMap().put(IngestJob.RETRIES_LEFT, retriesLeft);
    return new JobDetailTriggerPair(job, trigger);
  }

  /**
   * Try to get the pause interval between trying to ingest media to the core from the configuration manager. If it
   * can't it will use the IngestJob default pause time.
   *
   * @return Returns the amount of time to pause between trying to ingest.
   **/
  private static long getPauseInterval(ConfigurationManager configurationManager) {
    long pauseInterval;
    try {
      pauseInterval = Long.parseLong(configurationManager.getItem(CaptureParameters.INGEST_PAUSE_TIME));
    } catch (NumberFormatException e) {
      logger.warn("Unable to get pause interval for ingestion. Using default pause time of: "
              + IngestJob.DEFAULT_PAUSE_TIME);
      pauseInterval = Long.parseLong(IngestJob.DEFAULT_PAUSE_TIME);
    } catch (NullPointerException e) {
      logger.warn("Unable to get pause interval for ingestion. Using default pause time of: "
              + IngestJob.DEFAULT_PAUSE_TIME);
      pauseInterval = Long.parseLong(IngestJob.DEFAULT_PAUSE_TIME);
    }
    return pauseInterval;
  }

  /**
   * Returns the amount of time between retries to ingest to the core. Will use IngestJobRetrier Default Retry Interval
   * if it can't load the setting from the configuration manager.
   *
   * @return Returns the amount of time to wait between retrying to ingest to the core in case of a failure.
   **/
  private static long getRetryInterval(ConfigurationManager configurationManager) {
    long retryInterval;
    try {
      retryInterval = Long.parseLong(configurationManager.getItem(CaptureParameters.INGEST_RETRY_INTERVAL));
    } catch (NullPointerException e) {
      retryInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
    } catch (NumberFormatException e) {
      retryInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
    }
    return retryInterval;
  }

  /**
   * Returns the number of times to try ingesting to the core before sleeping until the next set of retries. It uses the
   * IngestJobRetrier's DEFAULT_RETRIES if it can't load this number from the ConfigurationManager.
   *
   * @return The number of times to retry ingesting to the core before sleeping.
   **/
  private static long getRetryLimit(ConfigurationManager configurationManager) {
    // Pass in the amount of times we want to retry ingesting
    long retryLimit;
    try {
      retryLimit = Long.parseLong(configurationManager.getItem(CaptureParameters.INGEST_RETRY_LIMIT));
      if (retryLimit < 1) {
        retryLimit = 1;
      }
    } catch (NumberFormatException e) {
      retryLimit = Long.parseLong(IngestJob.DEFAULT_RETRIES);
    }
    return retryLimit;
  }
}
