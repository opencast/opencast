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
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * The class responsible for stopping a capture.
 */
public class StopCaptureJob implements Job {

  public static final String JOB_PREFIX = "StopCapture-";
  public static final String TRIGGER_PREFIX = "StopCaptureTrigger-";

  private static final Logger logger = LoggerFactory.getLogger(StopCaptureJob.class);

  /**
   * Stops the capture. Also schedules a SerializeJob. {@inheritDoc}
   *
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    logger.debug("Initiating stopCaptureJob");

    try {
      // Extract the Capture Agent to stop the capture ASAP
      CaptureAgentImpl ca = (CaptureAgentImpl) ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);

      // The scheduler to use when scheduling the next job
      Scheduler sched = (Scheduler) ctx.getMergedJobDataMap().get(JobParameters.SCHEDULER);

      // Extract the recording ID
      String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);

      // This needs to specify which job to stop
      // otherwise we could end up stopping something else if the expected job failed earlier.
      ca.stopCapture(recordingID, false);

      String postfix = ctx.getMergedJobDataMap().getString(JobParameters.JOB_POSTFIX);
      // Create job and trigger
      JobDetail job = new JobDetail(SerializeJob.JOB_PREFIX + postfix, JobParameters.SUPPORT_TYPE, SerializeJob.class);

      // Setup the trigger. The serialization job will automatically refire if it fails
      SimpleTrigger trigger = new SimpleTrigger(SerializeJob.TRIGGER_PREFIX + postfix, JobParameters.SUPPORT_TYPE);
      trigger.setStartTime(new Date());
      trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

      trigger.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);
      trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, ca);
      trigger.getJobDataMap().put(JobParameters.JOB_POSTFIX, postfix);
      trigger.getJobDataMap().put(JobParameters.SCHEDULER, sched);

      // Schedule the serializeJob
      sched.scheduleJob(job, trigger);

      logger.info("stopCaptureJob complete");

      // Remove this job from the system
      JobDetail mine = ctx.getJobDetail();
      try {
        if (!ctx.getScheduler().isShutdown()) {
          ctx.getScheduler().deleteJob(mine.getName(), mine.getGroup());
        }
      } catch (SchedulerException e) {
        logger.warn("Unable to delete stop capture job {}!", mine.getName());
        e.printStackTrace();
      }

    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e);
      e.printStackTrace();
    } catch (Exception e) {
      logger.error("Unexpected exception: {}", e);
      e.printStackTrace();
    }
  }

}
