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
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.util.ConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * The unit test for scheduling the workflow
 *
 */
public class SchedulingTest {
  private static final Logger logger = LoggerFactory.getLogger(SchedulingTest.class);
  private Scheduler sched;
  private final CaptureAgentImpl captAg = new CaptureAgentImpl();
  private Properties props = null;;
  private final File outDir = new File(this.getClass().getResource("/.").getFile(), "capture_tmp");
  private MediaPackage mp;

  @Before
  public void init() throws ConfigurationException, MediaPackageException {
    try {
      sched = new StdSchedulerFactory().getScheduler();
      sched.start();
    } catch (SchedulerException e) {
      logger.error("Error creating scheduler");
      e.printStackTrace();
    }

    mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    long time = TriggerUtils.getNextGivenSecondDate(null, 10).getTime();

    props = new Properties();
    props.setProperty(CaptureParameters.RECORDING_ID, "TestID");
    props.setProperty(CaptureParameters.RECORDING_END, DateFormat.getDateInstance().format(new Date(time)));
    props.setProperty(CaptureParameters.RECORDING_ROOT_URL, outDir.getAbsolutePath());
    props.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "SCREEN,PRESENTER,AUDIO");
    props.setProperty("capture.device.PRESENTER.src", this.getClass().getResource("/capture/camera.mpg").getFile());
    props.setProperty("capture.device.PRESENTER.outputfile", "professor.mpg");
    props.setProperty("capture.device.SCREEN.src", this.getClass().getResource("/capture/screen.mpg").getFile());
    props.setProperty("capture.device.SCREEN.outputfile", "screen.mpg");
    props.setProperty("capture.device.AUDIO.src", this.getClass().getResource("/capture/audio.mp3").getFile());
    props.setProperty("capture.device.AUDIO.outputfile", "microphone.mp3");
    props.setProperty(CaptureParameters.INGEST_ENDPOINT_URL,
            "http://nightly.opencastproject.org/ingest/addZippedMediaPackage");
  }

  @After
  public void tearDown() {
    try {
      sched.shutdown(true);
    } catch (SchedulerException e) {
      logger.error("Scheduler did not shut down cleanly");
      e.printStackTrace();
    }
  }

  @Test
  @Ignore
  public void testTest() {

    // Setup the job
    JobDetail job = new JobDetail("starting_capture", Scheduler.DEFAULT_GROUP, StartCaptureJob.class);

    long time = TriggerUtils.getNextGivenSecondDate(null, 5).getTime();
    // Create a new trigger Name Group name Start End # of times to repeat Repeat interval
    SimpleTrigger trigger = new SimpleTrigger("starting_capture", Scheduler.DEFAULT_GROUP, new Date(time));

    trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captAg);
    trigger.getJobDataMap().put(JobParameters.CAPTURE_PROPS, props);
    trigger.getJobDataMap().put(JobParameters.MEDIA_PACKAGE, mp);

    // Schedule the update
    try {
      sched.scheduleJob(job, trigger);
      Thread.sleep(60000);
    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e);
      e.printStackTrace();
    } catch (InterruptedException e) {
      logger.error("Interrupted Exception: {}", e);
      e.printStackTrace();
    }
  }
}
