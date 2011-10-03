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
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.util.FileSupport;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

/**
 * The class which cleans up captures if the capture has been successfully ingested and the remaining diskspace is below
 * a minimum threshold or above a maximum archive days threshold.
 */
public class CleanCaptureJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(CleanCaptureJob.class);

  /** File signifying ingest of media has been completed */
  public static final String CAPTURE_INGESTED = "captured.ingested";

  /** The length of one day represented in milliseconds */
  public static final long DAY_LENGTH_MILLIS = 86400000;

  private long minDiskSpace = 0;
  private long maxArchivalDays = Long.MAX_VALUE;
  private boolean checkArchivalDays = true;
  private boolean checkDiskSpace = true;
  private boolean underMinSpace = false;
  private CaptureAgentImpl service = null;

  /**
   * Cleans up lectures which no longer need to be stored on the capture agent itself. {@inheritDoc}
   * 
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    ConfigurationManager cm = (ConfigurationManager) ctx.getMergedJobDataMap().get(JobParameters.CONFIG_SERVICE);
    service = (CaptureAgentImpl) ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);
    Properties p = cm.getAllProperties();

    if (service != null) {
      doCleaning(p, service);
    } else {
      logger.error("Unable to run clean capture job, service pointer is null!");
    }
  }

  public void doCleaning(Properties p, CaptureAgentImpl service) {
    doCleaning(p, service.getKnownRecordings().values());
  }

  /**
   * This method is public to allow an easy testing without having to schedule anything
   * 
   * @param p
   *          {@code Properties} including the keys for maximum archival days and minimum disk space
   * @param recordings
   *          The {@code Collection} of {@code AgentRecording}s
   */
  public void doCleaning(Properties p, Collection<AgentRecording> recordings) {
    // Parse the necessary values for minimum disk space and maximum archival days.
    // Note that if some of those is not specified, the corresponding cleaning is not done.

    try {
      maxArchivalDays = Long.parseLong(p.getProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS));
      // If the value is < 0 (no matter if it's because of an overflow), it's invalid.
      // MAX_VALUE is considered infinity, and therefore there is no limit for archiving the recordings
      if ((maxArchivalDays < 0) || (maxArchivalDays == Long.MAX_VALUE))
        checkArchivalDays = false;
    } catch (NumberFormatException e) {
      logger.warn("No maximum archival days value specified in properties");
      checkArchivalDays = false;
    }

    try {
      minDiskSpace = Long.parseLong(p.getProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE));
      if (minDiskSpace <= 0)
        checkDiskSpace = false;
    } catch (NumberFormatException e) {
      logger.warn("No minimum disk space value specified in properties");
      checkDiskSpace = false;
    }

    // If none of this parameters has been specified, the cleanup cannot be performed
    if (!checkArchivalDays && !checkDiskSpace) {
      logger.info("No capture cleaning was made, according to the parameters");
      return;
    }

    // Gets all the recording IDs for this agent, and iterates over them
    for (AgentRecording theRec : recordings) {
      File recDir = theRec.getBaseDir();

      // If the capture.ingested file does not exist we cannot delete the data
      if (!theRec.getState().equals(RecordingState.UPLOAD_FINISHED)) {
        logger.info("Skipped cleaning for {}. Ingest has not been completed.", theRec.getID());
        continue;
      }

      // Clean up if we are running out of disk space
      if (checkDiskSpace) {
        long freeSpace = recDir.getFreeSpace();
        if (freeSpace < minDiskSpace) {
          underMinSpace = true;
          logger.info("Removing capture {} archives in {}. Under minimum free disk space.", theRec.getID(),
                  recDir.getAbsolutePath());
          FileSupport.delete(recDir, true);
          if (service != null) {
            service.removeCompletedRecording(theRec.getID());
          }
          continue;
        } else {
          underMinSpace = false;
          logger.debug("Archive: recording {} not removed, enough disk space remains for archive.", theRec.getID());
        }
      }

      // Clean up capture if its age of ingest is higher than max archival days property
      if (checkArchivalDays) {
        long age = theRec.getLastCheckinTime();
        long currentTime = System.currentTimeMillis();
        if (currentTime - age > maxArchivalDays * DAY_LENGTH_MILLIS) {
          logger.info("Removing capture {} archives at {}.\nExceeded the maximum archival days.", theRec.getID(),
                  recDir.getAbsolutePath());
          FileSupport.delete(recDir, true);
          if (service != null) {
            service.removeCompletedRecording(theRec.getID());
          }
          continue;
        } else {
          logger.debug("Recording {} has NOT yet exceeded the maximum archival days. Keeping {}", theRec.getID(),
                  recDir.getAbsolutePath());
        }
      }
      logger.debug("Recording {} ({}) not deleted.", theRec.getID(), recDir.getAbsolutePath());
    }

    if (underMinSpace)
      logger.warn("Free space is under the minimum disk space limit!");

  }
}
