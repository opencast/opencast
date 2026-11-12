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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
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
import org.opencastproject.lifecyclemanagement.impl.persistence.LifeCycleDatabaseServiceImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LifeCycleServiceTest {

  private LifeCycleServiceImpl service;
  private SecurityService securityService;

  private DefaultOrganization organization = null;

  private LifeCyclePolicy policy;
  private LifeCycleTask task;

  private AccessControlList acl = new AccessControlList();

  @Before
  public void setUp() throws Exception {
    // Init default policy
    List<LifeCyclePolicyAccessControlEntry> policyAccessControlEntries = new ArrayList<>();
    policyAccessControlEntries.add(new LifeCyclePolicyAccessControlEntryImpl(true, "ROLE_USER_BOB", "READ"));

    policy = new LifeCyclePolicyImpl();
    policy.setOrganization("mh_default_org");
    policy.setTitle("title");
    policy.setTargetType(TargetType.EVENT);
    policy.setAction(Action.START_WORKFLOW);
    policy.setActionParameters("{ workflowId: noop }");
    policy.setActionDate(EncodingSchemeUtils.decodeDate("2023-11-30T16:16:47Z"));
    policy.setTiming(Timing.SPECIFIC_DATE);

    // Init a task
    task = new LifeCycleTaskImpl();
    task.setOrganization("mh_default_org");
    task.setTargetId("1234");
    task.setStatus(Status.SCHEDULED);
    task.setLifeCyclePolicyId("42");


    // LifeCycle Service
    service = new LifeCycleServiceImpl();

    organization = new DefaultOrganization();
    securityService = createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
        SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    expect(securityService.getUser()).andReturn(user).anyTimes();
    replay(securityService);
    service.setSecurityService(securityService);

    AuthorizationService authorizationService = createNiceMock(AuthorizationService.class);
    expect(authorizationService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
        .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    replay(authorizationService);
    service.setAuthorizationService(authorizationService);

    // Init database
    LifeCycleDatabaseServiceImpl policyDatabaseService = new LifeCycleDatabaseServiceImpl();
    policyDatabaseService.setEntityManagerFactory(newEntityManagerFactory(
        LifeCycleDatabaseServiceImpl.PERSISTENCE_UNIT));
    policyDatabaseService.setDBSessionFactory(getDbSessionFactory());
    policyDatabaseService.setSecurityService(securityService);
    policyDatabaseService.activate(null);
    service.setPersistence(policyDatabaseService);

    service.activate(null);
  }

  @Test
  public void testPolicyManagement() throws Exception {
    policy = service.createLifeCyclePolicy(policy);
    LifeCyclePolicy policyFromDb = service.getLifeCyclePolicyById(policy.getId());
    Assert.assertNotNull(policyFromDb);
    Assert.assertEquals(policy.getTitle(), policyFromDb.getTitle());

    policy.setTitle("different title");
    assertTrue(service.updateLifeCyclePolicy(policy));
    policyFromDb = service.getLifeCyclePolicyById(policy.getId());
    Assert.assertEquals(policy.getTitle(), policyFromDb.getTitle());

    service.deleteLifeCyclePolicy(policy.getId());
    Exception exception = assertThrows(NotFoundException.class, () -> {
      service.getLifeCyclePolicyById(policy.getId());
    });
  }

  @Test
  public void testTaskManagement() throws Exception {
    task = service.createLifeCycleTask(task);
    LifeCycleTask taskFromDb = service.getLifeCycleTaskById(task.getId());
    Assert.assertNotNull(taskFromDb);
    Assert.assertEquals(task.getStatus(), taskFromDb.getStatus());

    task.setStatus(Status.FINISHED);
    assertTrue(service.updateLifeCycleTask(task));
    taskFromDb = service.getLifeCycleTaskById(task.getId());
    Assert.assertEquals(task.getStatus(), taskFromDb.getStatus());

    service.deleteLifeCycleTask(task.getId());
    Exception exception = assertThrows(NotFoundException.class, () -> {
      service.getLifeCycleTaskById(task.getId());
    });
  }

}
