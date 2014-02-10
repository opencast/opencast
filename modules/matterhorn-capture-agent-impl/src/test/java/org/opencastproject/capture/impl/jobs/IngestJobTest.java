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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.impl.ConfigurationManager;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;

import java.net.HttpURLConnection;
import java.text.ParseException;

public class IngestJobTest {
  private String recordingID = "fake-Recording-ID";
  private String postfix = recordingID;
  private CaptureAgentImpl captureAgentMock;
  private Scheduler scheduler;
  private ConfigurationManager configurationManager;
  private Waiter waiter = new Waiter();

  @Before
  public void before() throws SchedulerException {
    captureAgentMock = createMock(CaptureAgentImpl.class);
    scheduler = new StdSchedulerFactory().getScheduler();
    configurationManager = new ConfigurationManager();
    recordingID = "Fake Recording ID:" + Math.random();
    waiter = new Waiter();
  }

  @After
  public void after() {
    captureAgentMock = null;
    scheduler = null;
    configurationManager = null;
  }

  private void setProperties(String retryInterval, String retryLimit, String pauseTime) {
    configurationManager.setItem(CaptureParameters.INGEST_RETRY_INTERVAL, retryInterval);
    configurationManager.setItem(CaptureParameters.INGEST_RETRY_LIMIT, retryLimit);
    configurationManager.setItem(CaptureParameters.INGEST_PAUSE_TIME, pauseTime);
  }

  class Waiter {
    private int sleepTime = 100;
    private int maxSleepTime = 15000;
    private int sleepAccumulator = 0;
    private boolean done = false;

    public void sleepWait() throws InterruptedException {
      sleepAccumulator = 0;
      while (!done && sleepAccumulator < maxSleepTime) {
        Thread.sleep(sleepTime);
        sleepAccumulator += sleepTime;
      }
      if (sleepAccumulator >= maxSleepTime) {
        Assert.fail("Test Timed Out");
      }
    }
  }

  @Test
  public void createIngestJobWithSuccessStops() throws ParseException, SchedulerException, InterruptedException {
    String pausetime = "5";
    String intervaltime = "1";
    String retrytimes = "3";
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_OK).times(1);
    replay(captureAgentMock);
    setProperties(intervaltime, retrytimes, pausetime);
    scheduler.addGlobalTriggerListener(new VerifyCaptureAgentMockTriggerListener(1));
    JobDetailTriggerPair jobDetailTriggerPair = JobCreator.createInjestJob(Long.parseLong(intervaltime), recordingID,
            postfix, captureAgentMock, scheduler, configurationManager);
    verifyJobDetails(intervaltime, jobDetailTriggerPair);
    //jobDetailTriggerPair.getTrigger().addTriggerListener(VerifyCaptureAgentMockTriggerListener.NAME);
    scheduler.start();
    scheduler.scheduleJob(jobDetailTriggerPair.getJob(), jobDetailTriggerPair.getTrigger());
    waiter.sleepWait();
    Assert.assertEquals("IngestJob hasn't removed itself.", 0, scheduler.getCurrentlyExecutingJobs().size());
  }

  private void verifyJobDetails(String intervaltime, JobDetailTriggerPair jobDetailTriggerPair) {
    Assert.assertEquals("JobDetail doesn't have  the correct name.", IngestJob.JOB_PREFIX + postfix,
            jobDetailTriggerPair.getJob().getName());
    Assert.assertEquals("Trigger doesn't have  the correct name", IngestJob.TRIGGER_PREFIX + postfix,
            jobDetailTriggerPair.getTrigger().getName());
    Assert.assertEquals("Trigger doesn't have the correct cron expression", "0/" + intervaltime + " * * * * ?",
            jobDetailTriggerPair.getTrigger().getCronExpression());
    Assert.assertEquals("CaptureAgent is the same", captureAgentMock, jobDetailTriggerPair.getTrigger().getJobDataMap()
            .get(JobParameters.CAPTURE_AGENT));
    Assert.assertEquals("Scheduler is the same", scheduler,
            jobDetailTriggerPair.getTrigger().getJobDataMap().get(JobParameters.SCHEDULER));
  }

  class VerifyCaptureAgentMockTriggerListener implements TriggerListener {
    public static final String NAME = "Verify Mock Capture Agent Failed to Ingest";
    private int runCount = 1;

    public VerifyCaptureAgentMockTriggerListener(int triggers) {
      this.runCount = triggers;
    }

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public void triggerComplete(Trigger arg0, JobExecutionContext arg1, int arg2) {
      runCount--;
      // System.out.println("Complete: " + runCount);
      if (runCount <= 0) {
        verify(captureAgentMock);
        waiter.done = true;
      }
    }

    @Override
    public void triggerFired(Trigger arg0, JobExecutionContext arg1) {
      // System.out.println("Fired: " + runCount);
    }

    @Override
    public void triggerMisfired(Trigger arg0) {
      // System.out.println("Oh no!");
    }

    @Override
    public boolean vetoJobExecution(Trigger arg0, JobExecutionContext arg1) {
      return false;
    }

  }

  @Test
  public void createIngestJobWithOneFailureAndOneSuccessStops() throws ParseException, SchedulerException,
          InterruptedException {
    String pausetime = "5";
    String intervaltime = "1";
    String retrytimes = "3";
    setProperties(intervaltime, retrytimes, pausetime);
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_FORBIDDEN).times(1);
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_OK).times(1);
    replay(captureAgentMock);
    scheduler.addGlobalTriggerListener(new VerifyCaptureAgentMockTriggerListener(2));
    JobDetailTriggerPair jobDetailTriggerPair = JobCreator.createInjestJob(Long.parseLong(intervaltime), recordingID,
            postfix, captureAgentMock, scheduler, configurationManager);
    verifyJobDetails(intervaltime, jobDetailTriggerPair);
    // jobDetailTriggerPair.getTrigger().addTriggerListener(VerifyCaptureAgentMockTriggerListener.NAME);
    scheduler.start();
    scheduler.scheduleJob(jobDetailTriggerPair.getJob(), jobDetailTriggerPair.getTrigger());
    waiter.sleepWait();
    Assert.assertEquals("IngestJob hasn't removed itself.", 0, scheduler.getCurrentlyExecutingJobs().size());
  }

  @Test
  public void createIngestJobTestWith3TriesPausesSuccessfully() throws ParseException, SchedulerException,
          InterruptedException {
    String pausetime = "5";
    String intervaltime = "1";
    String retrytimes = "3";
    setProperties(intervaltime, retrytimes, pausetime);

    scheduler.addGlobalTriggerListener(new VerifyCaptureAgentMockTriggerListener(5));

    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_FORBIDDEN).times(
            Integer.parseInt(retrytimes));
    expect(captureAgentMock.getConfigService()).andReturn(configurationManager).times(1);
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_OK).times(1);
    replay(captureAgentMock);

    JobDetailTriggerPair jobDetailTriggerPair = JobCreator.createInjestJob(Long.parseLong(intervaltime), recordingID,
            postfix, captureAgentMock, scheduler, configurationManager);
    // Verify that the job has been created properly.
    verifyJobDetails(intervaltime, jobDetailTriggerPair);
    // jobDetailTriggerPair.getTrigger().addTriggerListener(VerifyCaptureAgentMockTriggerListener.NAME);
    scheduler.start();
    scheduler.scheduleJob(jobDetailTriggerPair.getJob(), jobDetailTriggerPair.getTrigger());
    waiter.sleepWait();
    Assert.assertEquals("IngestJob hasn't removed itself.", 0, scheduler.getCurrentlyExecutingJobs().size());
  }

  @Test
  public void createIngestJobTestWith1Try() throws ParseException, SchedulerException, InterruptedException {
    String pausetime = "2";
    String intervaltime = "1";
    String retrytimes = "1";
    setProperties(intervaltime, retrytimes, pausetime);
    scheduler.addGlobalTriggerListener(new VerifyCaptureAgentMockTriggerListener(3));
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_FORBIDDEN).times(
            Integer.parseInt(retrytimes));
    // Should grab the config service to try to schedule itself again.
    expect(captureAgentMock.getConfigService()).andReturn(configurationManager).times(1);
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_OK).times(1);
    replay(captureAgentMock);

    JobDetailTriggerPair jobDetailTriggerPair = JobCreator.createInjestJob(Long.parseLong(intervaltime), recordingID,
            postfix, captureAgentMock, scheduler, configurationManager);
    verifyJobDetails(intervaltime, jobDetailTriggerPair);
    scheduler.start();
    scheduler.scheduleJob(jobDetailTriggerPair.getJob(), jobDetailTriggerPair.getTrigger());
    waiter.sleepWait();
    Assert.assertEquals("IngestJob hasn't removed itself.", 0, scheduler.getCurrentlyExecutingJobs().size());
  }

  @Test
  public void createIngestJobTestWith0Tries() throws ParseException, SchedulerException, InterruptedException {
    String pausetime = "5";
    String intervaltime = "1";
    String retrytimes = "0";
    setProperties(intervaltime, retrytimes, pausetime);
    scheduler.addGlobalTriggerListener(new VerifyCaptureAgentMockTriggerListener(3));
    // If the value of retry times is 0 or less the default is set to 1.
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_FORBIDDEN).times(1);
    expect(captureAgentMock.getConfigService()).andReturn(configurationManager).times(1);
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_OK).times(1);
    replay(captureAgentMock);

    JobDetailTriggerPair jobDetailTriggerPair = JobCreator.createInjestJob(Long.parseLong(intervaltime), recordingID,
            postfix, captureAgentMock, scheduler, configurationManager);
    verifyJobDetails(intervaltime, jobDetailTriggerPair);
    scheduler.start();
    scheduler.scheduleJob(jobDetailTriggerPair.getJob(), jobDetailTriggerPair.getTrigger());
    waiter.sleepWait();
    Assert.assertEquals("IngestJob hasn't removed itself.", 0, scheduler.getCurrentlyExecutingJobs().size());
  }

  @Test
  public void createIngestJobWorksWithNullParameters() throws ParseException, SchedulerException, InterruptedException {
    String pausetime = null;
    String intervaltime = null;
    String retrytimes = null;
    setProperties(intervaltime, retrytimes, pausetime);
    expect(captureAgentMock.ingest(recordingID)).andReturn(HttpURLConnection.HTTP_OK).times(1);
    replay(captureAgentMock);

    JobDetailTriggerPair jobDetailTriggerPair = JobCreator.createInjestJob(-20, recordingID, postfix, captureAgentMock,
            scheduler, configurationManager);
    Assert.assertEquals("JobDetail has the correct name.", IngestJob.JOB_PREFIX + postfix, jobDetailTriggerPair
            .getJob().getName());
    Assert.assertEquals("Trigger has the correct name", IngestJob.TRIGGER_PREFIX + postfix, jobDetailTriggerPair
            .getTrigger().getName());
    Assert.assertEquals("Trigger has the correct cron expression", "0/" + IngestJob.DEFAULT_RETRY_INTERVAL
            + " * * * * ?", jobDetailTriggerPair.getTrigger().getCronExpression());
    Assert.assertEquals("CaptureAgent is the same", captureAgentMock, jobDetailTriggerPair.getTrigger().getJobDataMap()
            .get(JobParameters.CAPTURE_AGENT));
    Assert.assertEquals("Scheduler is the same", scheduler,
            jobDetailTriggerPair.getTrigger().getJobDataMap().get(JobParameters.SCHEDULER));
  }
}
