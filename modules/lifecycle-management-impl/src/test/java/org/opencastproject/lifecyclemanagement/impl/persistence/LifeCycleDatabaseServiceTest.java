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
package org.opencastproject.lifecyclemanagement.impl.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicyAccessControlEntry;
import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.Status;
import org.opencastproject.lifecyclemanagement.api.TargetType;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyAccessControlEntryImpl;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyImpl;
import org.opencastproject.lifecyclemanagement.impl.LifeCycleTaskImpl;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LifeCycleDatabaseServiceTest {


  private LifeCycleDatabaseServiceImpl policyDatabaseService;
  private SecurityService securityService;

  private LifeCyclePolicy testPolicy;
  private LifeCycleTask testTask;

  @Before
  public void setUp() throws Exception {
    // Init default policy
    List<LifeCyclePolicyAccessControlEntry> policyAccessControlEntries = new ArrayList<>();
    policyAccessControlEntries.add(new LifeCyclePolicyAccessControlEntryImpl(true, "ROLE_USER_BOB", "READ"));

    testPolicy = new LifeCyclePolicyImpl();
    testPolicy.setOrganization("mh_default_org");
    testPolicy.setTitle("title");
    testPolicy.setTargetType(TargetType.EVENT);
    testPolicy.setAction(Action.START_WORKFLOW);
    testPolicy.setActionParameters("{ workflowId: noop }");
    testPolicy.setActionDate(EncodingSchemeUtils.decodeDate("2023-11-30T16:16:47Z"));
    testPolicy.setTiming(Timing.SPECIFIC_DATE);

    // Init a task
    testTask = new LifeCycleTaskImpl();
    testTask.setOrganization("mh_default_org");
    testTask.setTargetId("1234");
    testTask.setStatus(Status.SCHEDULED);
    testTask.setLifeCyclePolicyId("42");


    // Mock security service
    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
        SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    // Init database
    policyDatabaseService = new LifeCycleDatabaseServiceImpl();
    policyDatabaseService.setEntityManagerFactory(newEntityManagerFactory(
        LifeCycleDatabaseServiceImpl.PERSISTENCE_UNIT));
    policyDatabaseService.setDBSessionFactory(getDbSessionFactory());
    policyDatabaseService.setSecurityService(securityService);
    policyDatabaseService.activate(null);
  }

  @Test
  public void testAdding() throws Exception {
    policyDatabaseService.createLifeCyclePolicy(testPolicy, securityService.getOrganization().getId());
  }

  @Test
  public void testMerging() throws Exception {
    assertFalse(policyDatabaseService.updateLifeCyclePolicy(testPolicy, securityService.getOrganization().getId()));
    LifeCyclePolicy policy = policyDatabaseService.createLifeCyclePolicy(testPolicy,
        securityService.getOrganization().getId());
    assertTrue(policyDatabaseService.updateLifeCyclePolicy(policy, securityService.getOrganization().getId()));
  }

  @Test
  public void testDeleting() throws Exception {
    policyDatabaseService.createLifeCyclePolicy(testPolicy, securityService.getOrganization().getId());
    assertTrue(policyDatabaseService.deleteLifeCyclePolicy(testPolicy, securityService.getOrganization().getId()));
    assertFalse(policyDatabaseService.deleteLifeCyclePolicy(testPolicy, securityService.getOrganization().getId()));
  }

  @Test
  public void testRetrieving() throws Exception {
    policyDatabaseService.createLifeCyclePolicy(testPolicy, securityService.getOrganization().getId());

    policyDatabaseService.getLifeCyclePolicy(testPolicy.getId(), securityService.getOrganization().getId());
    List policies = policyDatabaseService.getLifeCyclePolicies(100, 0, new SortCriterion("", SortCriterion.Order.None));
    assertTrue("Exactly one policy should be returned", policies.size() == 1);

    policyDatabaseService.deleteLifeCyclePolicy(testPolicy, securityService.getOrganization().getId());
    Exception exception = assertThrows(NotFoundException.class, () -> {
      policyDatabaseService.getLifeCyclePolicy(testPolicy.getId(), securityService.getOrganization().getId());
    });
    policies = policyDatabaseService.getLifeCyclePolicies(100, 0, new SortCriterion("", SortCriterion.Order.None));
    assertTrue("Exactly zero policies should be returned", policies.isEmpty());
  }

  @Test
  public void testAddingTask() throws Exception {
    policyDatabaseService.createLifeCycleTask(testTask, securityService.getOrganization().getId());
  }

  @Test
  public void testMergingTask() throws Exception {
    assertFalse(policyDatabaseService.updateLifeCycleTask(testTask, securityService.getOrganization().getId()));
    LifeCycleTask task = policyDatabaseService.createLifeCycleTask(testTask, securityService.getOrganization().getId());
    assertTrue(policyDatabaseService.updateLifeCycleTask(task, securityService.getOrganization().getId()));
  }

  @Test
  public void testDeletingTask() throws Exception {
    policyDatabaseService.createLifeCycleTask(testTask, securityService.getOrganization().getId());
    assertTrue(policyDatabaseService.deleteLifeCycleTask(testTask, securityService.getOrganization().getId()));
    assertFalse(policyDatabaseService.deleteLifeCycleTask(testTask, securityService.getOrganization().getId()));
  }

  @Test
  public void testRetrievingTask() throws Exception {
    policyDatabaseService.createLifeCycleTask(testTask, securityService.getOrganization().getId());

    policyDatabaseService.getLifeCycleTask(testTask.getId());

    policyDatabaseService.deleteLifeCycleTask(testTask, securityService.getOrganization().getId());
    Exception exception = assertThrows(NotFoundException.class, () -> {
      policyDatabaseService.getLifeCycleTask(testTask.getId());
    });
  }
}
