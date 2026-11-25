/*
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
package org.opencastproject.lifecyclemanagement.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class PolicyCheckRunnerTest {

  private static final String CRON = "0 0 0 1 1 ? 2099";
  private static final String UPDATED_CRON = "0 0 0 2 1 ? 2099";
  private static final String POLICY_ID = "test-policy";
  private static final String JOB_GROUP = "mh-repeating-policy-runner-job-group" + POLICY_ID;
  private static final String TRIGGER_NAME = "mh-repeating-policy-runner-trigger" + POLICY_ID;
  private static final String TRIGGER_GROUP = "mh-repeating-policy-runner-trigger-group" + POLICY_ID;

  private Scheduler scheduler;
  private PolicyCheckRunner checker;
  private final List<LifeCyclePolicy> policies = new ArrayList<>();
  private final List<Organization> organizationList = new ArrayList<>();
  private final Map<String, List<LifeCyclePolicy>> policiesByOrganization = new HashMap<>();
  private final AtomicReference<Organization> currentOrganization = new AtomicReference<>();

  @Before
  public void setUp() throws Exception {
    Organization defaultOrg = new DefaultOrganization();
    organizationList.add(defaultOrg);
    policiesByOrganization.put(defaultOrg.getId(), policies);
    scheduler = new StdSchedulerFactory().getScheduler();
    LifeCycleService service = createNiceMock(LifeCycleService.class);
    expect(service.getActiveLifeCyclePolicies()).andAnswer(() ->
        new ArrayList<>(policiesByOrganization.get(currentOrganization.get().getId()))).anyTimes();
    expect(service.getLifeCyclePolicyById(POLICY_ID)).andAnswer(() -> policies.get(0)).anyTimes();
    OrganizationDirectoryService organizations = createNiceMock(OrganizationDirectoryService.class);
    expect(organizations.getOrganizations()).andReturn(organizationList).anyTimes();
    SecurityService security = createNiceMock(SecurityService.class);
    expect(security.getOrganization()).andAnswer(currentOrganization::get).anyTimes();
    security.setOrganization(anyObject(Organization.class));
    expectLastCall().andAnswer(() -> {
      currentOrganization.set((Organization) getCurrentArguments()[0]);
      return null;
    }).anyTimes();
    BundleContext context = createNiceMock(BundleContext.class);
    expect(context.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("opencast_system_account").anyTimes();
    replay(service, organizations, security, context);

    checker = new PolicyCheckRunner();
    checker.setLifeCycleService(service);
    checker.setOrganizationDirectoryService(organizations);
    checker.setSecurityService(security);
    checker.activate(context);
    checker.scheduledExecutor.shutdownNow();
    policies.add(policy(CRON));
  }

  @After
  public void tearDown() throws Exception {
    if (checker != null) {
      checker.deactivate();
    }
    if (scheduler != null) {
      scheduler.shutdown(true);
    }
  }

  @Test
  public void testDeactivateRemovesJobsWithoutStoppingSharedScheduler() throws Exception {
    assertSame(scheduler, new StdSchedulerFactory().getScheduler());
    JobDetail unrelated = new JobDetail("unrelated", "other-component", NoOpJob.class);
    unrelated.setDurability(true);
    scheduler.addJob(unrelated, true);
    checkPolicies();
    assertNotNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));

    checker.deactivate();

    assertEquals(0, scheduler.getJobNames(JOB_GROUP).length);
    assertNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
    assertFalse(scheduler.isShutdown());
    assertNotNull(scheduler.getJobDetail("unrelated", "other-component"));
  }

  @Test
  public void testDeletedPolicyIsUnscheduledAndCanBeScheduledAgain() throws Exception {
    checkPolicies();
    policies.clear();
    checkPolicies();
    assertEquals(0, scheduler.getJobNames(JOB_GROUP).length);

    policies.add(policy(CRON));
    checkPolicies();
    assertNotNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
  }

  @Test
  public void testInactivePolicyIsUnscheduled() throws Exception {
    checkPolicies();
    policies.get(0).setActive(false);
    checkPolicies();
    assertNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
  }

  @Test
  public void testPolicyNoLongerRepeatingIsUnscheduled() throws Exception {
    checkPolicies();
    LifeCyclePolicy changed = policy(CRON);
    changed.setTiming(Timing.SPECIFIC_DATE);
    changed.setActionDate(Date.from(Instant.parse("2099-01-01T00:00:00Z")));
    policies.set(0, changed);
    checkPolicies();
    assertNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
  }

  @Test
  public void testChangedCronIsRescheduled() throws Exception {
    checkPolicies();
    policies.set(0, policy(UPDATED_CRON));
    checkPolicies();
    assertEquals(UPDATED_CRON, ((CronTrigger) scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP)).getCronExpression());
  }

  @Test
  public void testUnchangedPolicyIsNotDuplicated() throws Exception {
    checkPolicies();
    checkPolicies();
    assertEquals(1, scheduler.getJobNames(JOB_GROUP).length);
  }

  @Test
  public void testPollAfterDeactivationDoesNotScheduleJobs() throws Exception {
    checker.deactivate();
    checkPolicies();
    assertNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
  }

  @Test
  public void testReactivationAfterInactiveJobExecutionKeepsSchedule() throws Exception {
    checkPolicies();
    policies.get(0).setActive(false);
    JobExecutionContext context = createNiceMock(JobExecutionContext.class);
    expect(context.getJobDetail()).andReturn(scheduler.getJobDetail(
        "mh-repeating-policy-runner-job" + POLICY_ID, JOB_GROUP));
    replay(context);
    new RepeatingPolicyRunner.Runner().execute(context);

    policies.get(0).setActive(true);
    checkPolicies();
    assertNotNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
  }

  @Test
  public void testReconciliationOnlyRemovesPoliciesOfCurrentOrganization() throws Exception {
    Organization other = new JaxbOrganization("other");
    LifeCyclePolicy otherPolicy = policy(CRON);
    otherPolicy.setId("other-policy");
    organizationList.add(other);
    policiesByOrganization.put(other.getId(), List.of(otherPolicy));
    checkPolicies();
    assertNotNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
    assertNotNull(scheduler.getTrigger("mh-repeating-policy-runner-triggerother-policy",
        "mh-repeating-policy-runner-trigger-groupother-policy"));

    policies.clear();
    checkPolicies();
    assertNull(scheduler.getTrigger(TRIGGER_NAME, TRIGGER_GROUP));
    assertNotNull(scheduler.getTrigger("mh-repeating-policy-runner-triggerother-policy",
        "mh-repeating-policy-runner-trigger-groupother-policy"));
  }

  private void checkPolicies() {
    checker.new Runner().run();
  }

  private LifeCyclePolicy policy(String cron) {
    LifeCyclePolicy policy = new LifeCyclePolicyImpl();
    policy.setId(POLICY_ID);
    policy.setTitle("Test policy");
    policy.setActive(true);
    policy.setTiming(Timing.REPEATING);
    policy.setCronTrigger(cron);
    return policy;
  }

  public static class NoOpJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
    }
  }
}
