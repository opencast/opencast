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
import org.opencastproject.capture.impl.SchedulerImpl;
import org.opencastproject.mediapackage.MediaPackage;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

public class StartCaptureJobTest {
  private CaptureAgentImpl captureAgentImpl = null;
  private MediaPackage mediaPackage = null;
  private Properties properties = null;
  private Scheduler scheduler = null;
  private JobExecutionContext ctx = null;
  private JobDataMap jobDataMap = null;

  @Before
  public void init() {
    initNeededVariables();
    addNeededVariablesToJobDataMap();
    addNeededProperties();
  }

  public void initNeededVariables() {
    properties = new Properties();
    mediaPackage = EasyMock.createMock(MediaPackage.class);
    captureAgentImpl = EasyMock.createMock(CaptureAgentImpl.class);
    try {
      scheduler = StdSchedulerFactory.getDefaultScheduler();
    } catch (SchedulerException e) {
      Assert.fail();
      e.printStackTrace();
    }
    ctx = EasyMock.createMock(JobExecutionContext.class);
    jobDataMap = new JobDataMap();
  }

  public void addNeededVariablesToJobDataMap() {
    jobDataMap.put(JobParameters.CAPTURE_AGENT, captureAgentImpl);
    jobDataMap.put(JobParameters.MEDIA_PACKAGE, mediaPackage);
    jobDataMap.put(JobParameters.CAPTURE_PROPS, properties);
    jobDataMap.put(JobParameters.SCHEDULER, scheduler);
  }

  public void addNeededProperties() {
    properties.put(JobParameters.JOB_POSTFIX, "StartCaptureJob Test Job");
  }

  @Test
  public void startCaptureFailsToStartIfEndIsRightNow() {
    try {
      properties.put(CaptureParameters.RECORDING_END, SchedulerImpl.getCronString(new Date()).toString());
    } catch (ParseException e) {
      Assert.fail("Could not create a cronjob " + e.getMessage());
    }

    StartCaptureJob job = new StartCaptureJob();
    EasyMock.expect(ctx.getMergedJobDataMap()).andReturn(jobDataMap).anyTimes();
    EasyMock.replay(ctx);
    // Expect captureAgentImpl won't fire startCapture.
    EasyMock.replay(captureAgentImpl);
    try {
      job.execute(ctx);
    } catch (JobExecutionException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void startCaptureFailsToStartIfAfterEnd() {
    try {
      Date end = new Date(System.currentTimeMillis() - 1000000);
      properties.put(CaptureParameters.RECORDING_END, SchedulerImpl.getCronString(end).toString());
    } catch (ParseException e) {
      Assert.fail("Could not create a cronjob " + e.getMessage());
    }

    StartCaptureJob job = new StartCaptureJob();
    EasyMock.expect(ctx.getMergedJobDataMap()).andReturn(jobDataMap).anyTimes();
    EasyMock.replay(ctx);
    // Expect captureAgentImpl won't fire startCapture.
    EasyMock.replay(captureAgentImpl);
    try {
      job.execute(ctx);
    } catch (JobExecutionException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void startCaptureFiresIfEndIsAfterNow() throws SchedulerException {
    try {
      Date end = new Date(System.currentTimeMillis() + 1000000);
      properties.put(CaptureParameters.RECORDING_END, SchedulerImpl.getCronString(end).toString());
    } catch (ParseException e) {
      Assert.fail("Could not create a cronjob " + e.getMessage());
    }

    StartCaptureJob job = new StartCaptureJob();
    EasyMock.expect(ctx.getMergedJobDataMap()).andReturn(jobDataMap).anyTimes();
    EasyMock.expect(ctx.getJobDetail()).andReturn(new JobDetail());
    EasyMock.expect(ctx.getScheduler()).andReturn(scheduler).anyTimes();
    EasyMock.replay(ctx);

    //EasyMock.expect(scheduler.isShutdown()).andReturn(true);

    // Expect captureAgentImpl will fire startCapture.
    EasyMock.expect(captureAgentImpl.startCapture(mediaPackage, properties)).andReturn("This would be an id");
    EasyMock.replay(captureAgentImpl);
    try {
      job.execute(ctx);

    } catch (JobExecutionException e) {
      Assert.fail(e.getMessage());
    }
    EasyMock.verify(captureAgentImpl);
  }
}
