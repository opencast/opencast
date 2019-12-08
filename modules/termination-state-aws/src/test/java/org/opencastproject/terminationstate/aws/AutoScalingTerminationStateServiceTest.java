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
package org.opencastproject.terminationstate.aws;

import org.opencastproject.job.api.Job;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.LifecycleHook;
import com.amazonaws.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class AutoScalingTerminationStateServiceTest {
  private AutoScalingTerminationStateService service;
  private ServiceRegistry serviceRegistry;
  private AmazonAutoScaling autoScaling;
  private AutoScalingGroup autoScalingGroup;
  private LifecycleHook lifecycleHook;
  private AutoScalingInstanceDetails instance;
  private Long nRunningJobs;

  private Scheduler scheduler;

  @Before
  public void setUp() throws Exception {
    serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    autoScaling = EasyMock.createMock(AmazonAutoScaling.class);
    autoScalingGroup = new AutoScalingGroup();
    autoScalingGroup.setAutoScalingGroupName("test-auto-scaling");
    lifecycleHook = new LifecycleHook();
    lifecycleHook.setLifecycleHookName("test-terminate");
    instance = new AutoScalingInstanceDetails();
    instance.setLifecycleState("InService");

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
    List<AutoScalingInstanceDetails> instances = new ArrayList<>();
    instances.add(instance);
    DescribeAutoScalingInstancesResult dasir = EasyMock.createMock(DescribeAutoScalingInstancesResult.class);
    EasyMock.expect(dasir.getAutoScalingInstances()).andReturn(instances).anyTimes();
    EasyMock.replay(dasir);

    EasyMock.expect(autoScaling.describeAutoScalingInstances(EasyMock.anyObject(DescribeAutoScalingInstancesRequest.class)))
            .andReturn(dasir).anyTimes();
    EasyMock.expect(autoScaling.recordLifecycleActionHeartbeat(EasyMock.anyObject(RecordLifecycleActionHeartbeatRequest.class)))
            .andReturn(null).anyTimes();
    EasyMock.expect(autoScaling.completeLifecycleAction(EasyMock.anyObject(CompleteLifecycleActionRequest.class)))
            .andReturn(null).once();
    autoScaling.shutdown();
    EasyMock.expectLastCall();
    EasyMock.replay(autoScaling);

    service = new AutoScalingTerminationStateService();
    service.setServiceRegistry(serviceRegistry);
    service.setAutoScaling(autoScaling);
    service.setAutoScalingGroup(autoScalingGroup);
    service.setLifecycleHook(lifecycleHook);
    service.setScheduler(scheduler);

    Dictionary config = new Hashtable();
    config.put(AutoScalingTerminationStateService.CONFIG_ENABLE, "true");
    config.put(AutoScalingTerminationStateService.CONFIG_LIFECYCLE_POLLING_PERIOD, "2");
    config.put(AutoScalingTerminationStateService.CONFIG_LIFECYCLE_HEARTBEAT_PERIOD, "2");
    service.configure(config);
  }

  @Test
  @Ignore
  public void testLifeCyclePolling() throws Exception {
    service.startPollingLifeCycleHook();
    String[] trigger = scheduler.getTriggerNames(AutoScalingTerminationStateService.SCHEDULE_GROUP);
    Assert.assertEquals(1, trigger.length);
    Assert.assertEquals(AutoScalingTerminationStateService.SCHEDULE_LIFECYCLE_POLLING_TRIGGER, trigger[0]);
    service.stopPollingLifeCycleHook();

    // change Lifecycle state
    instance.setLifecycleState("Terminating:Wait");
    Thread.sleep(3000);
    trigger = scheduler.getTriggerNames(AutoScalingTerminationStateService.SCHEDULE_GROUP);
    Assert.assertEquals(1, trigger.length);
    Assert.assertEquals(AutoScalingTerminationStateService.SCHEDULE_LIFECYCLE_HEARTBEAT_TRIGGER, trigger[0]);

    // complete running jobs
    nRunningJobs = 0L;
    Thread.sleep(3000);
    trigger = scheduler.getTriggerNames(AutoScalingTerminationStateService.SCHEDULE_GROUP);
    Assert.assertEquals(0, trigger.length);
    Assert.assertEquals(AutoScalingTerminationStateService.TerminationState.READY, service.getState());
  }

  @After
  public void tearDown() throws Exception {
    service.deactivate();
  }
}
