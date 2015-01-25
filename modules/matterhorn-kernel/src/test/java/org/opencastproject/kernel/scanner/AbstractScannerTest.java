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
package org.opencastproject.kernel.scanner;

import static org.junit.Assert.assertEquals;

import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.easymock.EasyMock;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.util.Date;

public class AbstractScannerTest {
  private static final String JOB_NAME = "scheduled-jobs";
  private static final String JOB_GROUP = "scheduled-group";
  private static final String TRIGGER_NAME = "scheduled-jobs";
  private static final String TRIGGER_GROUP = "scheduled-group";

  private Scheduler mockQuartz;
  private String jobName = null;
  private String jobGroup = null;
  private OrganizationDirectoryService directoryService = null;
  private SecurityContext securityContext = null;
  private String systemUserName = null;
  private ServiceRegistry serviceRegistry = null;
  private String triggerName = null;
  private String triggerGroupName = null;

  private AbstractScanner abstractScanner = new AbstractScanner() {
    @Override
    public String getTriggerName() {
      return triggerName;
    }

    @Override
    public String getTriggerGroupName() {
      return triggerGroupName;
    }

    @Override
    public String getSystemUserName() {
      return systemUserName;
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
      return serviceRegistry;
    }

    @Override
    public String getScannerName() {
      return "Test Scanner Name";
    }

    @Override
    public OrganizationDirectoryService getOrganizationDirectoryService() {
      return directoryService;
    }

    @Override
    public String getJobName() {
      return jobName;
    }

    @Override
    public String getJobGroup() {
      return jobGroup;
    }

    @Override
    public SecurityContext getAdminContextFor(String orgId) {
      return securityContext;
    }

    @Override
    public void scan() {
      // Empty
    }
  };

  @Test
  public void defaultIsDisabled() {
    assertEquals(false, abstractScanner.isEnabled());
  }

  @Test
  public void canSetDisabled() {
    abstractScanner.setEnabled(false);
    assertEquals(false, abstractScanner.isEnabled());
  }

  @Test
  public void canSetEnabled() {
    abstractScanner.setEnabled(true);
    assertEquals(true, abstractScanner.isEnabled());
  }

  @Test
  public void scheduleInputDefaultsExpectDisabled() {
    abstractScanner.schedule();
  }

  @Test
  public void scheduleInputEnabledExpectNoExceptions() {
    abstractScanner.setEnabled(true);
    abstractScanner.schedule();
  }

  @Test
  public void scheduleInputBadCronExpectsNoException() throws SchedulerException {
    Trigger[] triggers = {};
    jobName = JOB_NAME;
    jobGroup = JOB_GROUP;
    triggerName = TRIGGER_NAME;
    triggerGroupName = TRIGGER_GROUP;
    mockQuartz = EasyMock.createMock(Scheduler.class);
    EasyMock.expect(mockQuartz.getTriggersOfJob(jobName, jobGroup)).andReturn(triggers);
    EasyMock.expect(mockQuartz.scheduleJob(EasyMock.anyObject(Trigger.class))).andReturn(new Date());
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.setEnabled(true);
    abstractScanner.setCronExpression("This is not a valid cron expression");
    abstractScanner.schedule();
  }

  @Test
  public void scheduleInputQuartzExceptionExpectsNoExceptionThrown() throws SchedulerException {
    jobName = JOB_NAME;
    jobGroup = JOB_GROUP;
    triggerName = TRIGGER_NAME;
    triggerGroupName = TRIGGER_GROUP;
    mockQuartz = EasyMock.createMock(Scheduler.class);
    EasyMock.expect(mockQuartz.getTriggersOfJob(jobName, jobGroup)).andThrow(new IllegalArgumentException("Mock Quartz Exception"));
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.setEnabled(true);
    abstractScanner.schedule();
  }

  @Test
  public void scheduleInputNoExistingJobsExpectsSchedulesTheJob() throws SchedulerException {
    Trigger[] triggers = {};
    jobName = JOB_NAME;
    jobGroup = JOB_GROUP;
    triggerName = TRIGGER_NAME;
    triggerGroupName = TRIGGER_GROUP;
    mockQuartz = EasyMock.createMock(Scheduler.class);
    EasyMock.expect(mockQuartz.getTriggersOfJob(jobName, jobGroup)).andReturn(triggers);
    EasyMock.expect(mockQuartz.scheduleJob(EasyMock.anyObject(Trigger.class))).andReturn(new Date());
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.setEnabled(true);
    abstractScanner.schedule();
  }

  @Test
  public void scheduleInputExistingJobsExpectsReschedulesTheJob() throws SchedulerException {
    Trigger trigger = EasyMock.createNiceMock(Trigger.class);
    Trigger[] triggers = {trigger};
    jobName = JOB_NAME;
    jobGroup = JOB_GROUP;
    triggerName = TRIGGER_NAME;
    triggerGroupName = TRIGGER_GROUP;
    mockQuartz = EasyMock.createMock(Scheduler.class);
    EasyMock.expect(mockQuartz.getTriggersOfJob(jobName, jobGroup)).andReturn(triggers);
    EasyMock.expect(mockQuartz.rescheduleJob(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class), EasyMock.anyObject(Trigger.class))).andReturn(new Date());
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.setEnabled(true);
    abstractScanner.schedule();
  }

  @Test
  public void unscheduleExpectsUnscheduleOfJobNameAndGroup() throws SchedulerException {
    mockQuartz = EasyMock.createMock(Scheduler.class);
    EasyMock.expect(mockQuartz.unscheduleJob(jobName, jobGroup)).andReturn(true);
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.unschedule();
  }

  @Test
  public void unscheduleInputSchedulerexceptionExpectsNoExceptionThrow() throws SchedulerException {
    mockQuartz = EasyMock.createMock(Scheduler.class);
    EasyMock.expect(mockQuartz.unscheduleJob(jobName, jobGroup)).andThrow(new SchedulerException("Mock scheduler exception."));
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.unschedule();
  }

  @Test
  public void shutdownExpectsShutdownOfQuartz() throws SchedulerException {
    mockQuartz = EasyMock.createMock(Scheduler.class);
    mockQuartz.shutdown();
    EasyMock.expectLastCall();
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.shutdown();
  }

  @Test
  public void shutdownInputSchedulerExceptionExpectsNoExceptionThrown() throws SchedulerException {
    mockQuartz = EasyMock.createMock(Scheduler.class);
    mockQuartz.shutdown();
    EasyMock.expectLastCall().andThrow(new SchedulerException("Mock scheduler exception"));
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.shutdown();
  }

  @Test
  public void finalizeExpectsShutdownOfQuartz() throws Throwable {
    mockQuartz = EasyMock.createMock(Scheduler.class);
    mockQuartz.shutdown();
    EasyMock.expectLastCall();
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.finalize();
  }

  @Test
  public void triggerExpectsTriggerOfQuartz() throws SchedulerException {
    mockQuartz = EasyMock.createMock(Scheduler.class);
    mockQuartz.triggerJobWithVolatileTrigger(jobName, jobGroup);
    EasyMock.expectLastCall();
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.trigger();
  }

  @Test
  public void triggerInputExceptionExpectsNoExceptionThrown() throws SchedulerException {
    mockQuartz = EasyMock.createMock(Scheduler.class);
    mockQuartz.triggerJobWithVolatileTrigger(jobName, jobGroup);
    EasyMock.expectLastCall().andThrow(new SchedulerException("Mock Scheduler Exception"));
    EasyMock.replay(mockQuartz);
    abstractScanner.setQuartz(mockQuartz);
    abstractScanner.trigger();
  }
}
