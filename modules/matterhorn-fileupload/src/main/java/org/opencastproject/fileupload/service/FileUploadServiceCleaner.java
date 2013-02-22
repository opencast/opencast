package org.opencastproject.fileupload.service;

import org.opencastproject.fileupload.api.FileUploadService;
import org.opencastproject.fileupload.api.job.FileUploadJob;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

/** Clear for outdated Job of type {@link FileUploadJob}. */
public class FileUploadServiceCleaner {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(FileUploadServiceCleaner.class);

  private static final String JOB_NAME = "mh-file-upload-cleaner-job";
  private static final String JOB_GROUP = "mh-file-upload-cleaner-job-group";
  private static final String TRIGGER_NAME = "mh-file-upload-cleaner-trigger";
  private static final String TRIGGER_GROUP = "mh-file-upload-cleaner-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  private final org.quartz.Scheduler quartz;

  private FileUploadService fileUploadService;

  protected FileUploadServiceCleaner(FileUploadService fileUploadService) {
    this.fileUploadService = fileUploadService;
    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Runner.class);
      job.setDurability(false);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public FileUploadService getFileUploadService() {
    return fileUploadService;
  }

  /**
   * Set the schedule and start or restart the scheduler.
   */
  public void schedule() {
    logger.info("Run file upload cleaner job every four hours");
    try {
      final Trigger trigger = TriggerUtils.makeHourlyTrigger();
      trigger.setStartTime(new Date());
      trigger.setName(TRIGGER_NAME);
      trigger.setGroup(TRIGGER_GROUP);
      trigger.setJobName(JOB_NAME);
      trigger.setJobGroup(JOB_GROUP);
      if (quartz.getTriggersOfJob(JOB_NAME, JOB_GROUP).length == 0) {
        quartz.scheduleJob(trigger);
      } else {
        quartz.rescheduleJob(TRIGGER_NAME, TRIGGER_GROUP, trigger);
      }
    } catch (Exception e) {
      logger.error("Error scheduling Quartz job", e);
    }
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    try {
      quartz.shutdown();
    } catch (org.quartz.SchedulerException ignore) {
    }
  }

  /** Trigger the scheduler once independent of it's actual schedule. */
  public void trigger() {
    try {
      quartz.triggerJobWithVolatileTrigger(JOB_NAME, JOB_GROUP);
    } catch (Exception e) {
      logger.error("Error triggering Quartz job", e);
    }
  }

  // just to make sure Quartz is being shut down...
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

  // --

  /** Quartz work horse. */
  public static class Runner implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      logger.info("Start file upload service cleaner");
      try {
        execute((FileUploadServiceCleaner) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT));
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while cleaning file upload jobs", e);
      }
      logger.info("Finished file upload service cleaner");
    }

    private void execute(FileUploadServiceCleaner fileUploadServiceCleaner) {
      try {
        fileUploadServiceCleaner.getFileUploadService().cleanOutdatedJobs();
      } catch (IOException e) {
        logger.warn(e.getMessage());
      }
    }

  }

}