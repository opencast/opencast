/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.terminationstate.impl;

import org.opencastproject.job.api.Job;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

public class TerminationStateServiceImplTest {
  private TerminationStateServiceImpl service;
  private ServiceRegistry serviceRegistry;
  private Long nRunningJobs;

  private Scheduler scheduler;

  @Before
  public void setUp() throws Exception {
    serviceRegistry = EasyMock.createMock(ServiceRegistry.class);

    try {
      scheduler = new StdSchedulerFactory().getScheduler();
    } catch (SchedulerException e) {
      throw new Exception(e);
    }

    nRunningJobs = 1L;
    EasyMock.expect(serviceRegistry.getRegistryHostname()).andReturn("localhost").anyTimes();
    EasyMock.expect(serviceRegistry.countByHost(null, "localhost", Job.Status.RUNNING))
            .andAnswer(new IAnswer<Long>() {
              @Override
              public Long answer() throws Throwable {
                return nRunningJobs;
              }
            }).anyTimes();
    serviceRegistry.setMaintenanceStatus("localhost", true);
    EasyMock.expectLastCall().atLeastOnce();
    EasyMock.replay(serviceRegistry);

    service = new TerminationStateServiceImpl();
    service.setServiceRegistry(serviceRegistry);
    service.setScheduler(scheduler);

    Dictionary config = new Hashtable();
    config.put(TerminationStateServiceImpl.CONFIG_JOB_POLLING_PERIOD, "2");
    service.configure(config);
  }

  @Test
  public void testLifeCyclePolling() throws Exception {
    // change termination state
    service.setState(TerminationStateServiceImpl.TerminationState.WAIT);
    Assert.assertEquals(TerminationStateServiceImpl.TerminationState.WAIT, service.getState());
    Thread.sleep(3000);
    String[] trigger = scheduler.getTriggerNames(TerminationStateServiceImpl.SCHEDULE_GROUP);
    Assert.assertEquals(1, trigger.length);
    Assert.assertEquals(TerminationStateServiceImpl.SCHEDULE_JOB_POLLING_TRIGGER, trigger[0]);

    // complete running jobs
    nRunningJobs = 0L;
    Thread.sleep(3000);
    trigger = scheduler.getTriggerNames(TerminationStateServiceImpl.SCHEDULE_GROUP);
    Assert.assertEquals(0, trigger.length);
    Assert.assertEquals(TerminationStateServiceImpl.TerminationState.READY, service.getState());
  }

  @After
  public void tearDown() throws Exception {
    service.deactivate();
  }
}
