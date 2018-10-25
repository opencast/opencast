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

package org.opencastproject.userdirectory.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.userdirectory.JpaUserAndRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;

import org.apache.http.HttpStatus;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import javax.ws.rs.core.Response;

public class GroupRoleEndpointTest {

  private JpaGroupRoleProvider provider = null;
  private GroupRoleEndpoint endpoint = null;
  private static JpaOrganization org1 = new JpaOrganization("org1", "org1", "localhost", 80, "admin", "anon", null);
  private static JpaOrganization org2 = new JpaOrganization("org2", "org2", "127.0.0.1", 80, "admin", "anon", null);

  @Before
  public void setUp() throws Exception {
    JpaUser adminUser = new JpaUser("admin", "pass1", org1, "Admin", "admin@localhost", "opencast", true,
            Collections.set(new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1)));

    // Set the security sevice
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(adminUser).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);

    // Create the message sender service
    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    messageSender.sendObjectMessage(EasyMock.anyObject(String.class),
            EasyMock.anyObject(MessageSender.DestinationType.class), EasyMock.anyObject(Serializable.class));
    EasyMock.expectLastCall();
    EasyMock.replay(messageSender);

    provider = new JpaGroupRoleProvider();
    provider.setSecurityService(securityService);
    provider.setMessageSender(messageSender);
    provider.setEntityManagerFactory(newTestEntityManagerFactory(JpaUserAndRoleProvider.PERSISTENCE_UNIT));
    provider.activate(null);

    endpoint = new GroupRoleEndpoint();
    endpoint.setJpaGroupRoleProvider(provider);

  }

  @After
  public void tearDown() throws Exception {
    provider.deactivate();
  }

  @Test
  public void testUpdateGroupNotAllowedAsNonAdminUser() throws UnauthorizedException {
    JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", Collections.set(
            new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1)));
    try {
      provider.addGroup(group);
      Group loadGroup = provider.loadGroup(group.getGroupId(), group.getOrganization().getId());
      assertNotNull(loadGroup);
      assertEquals(loadGroup.getGroupId(), loadGroup.getGroupId());
    } catch (Exception e) {
      fail("The group schould be added");
    }

    JpaUser user = new JpaUser("user", "pass1", org1, "User", "user@localhost", "opencast", true,
            Collections.set(new JpaRole("ROLE_USER", org1)));

    // Set the security sevice
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);
    provider.setSecurityService(securityService);

    try {
      // try add ROLE_USER
      Response updateGroupResponse = endpoint.updateGroup(group.getGroupId(), group.getName(), group.getDescription(),
              "ROLE_USER, " + SecurityConstants.GLOBAL_ADMIN_ROLE, null);
      assertNotNull(updateGroupResponse);
      assertEquals(HttpStatus.SC_FORBIDDEN, updateGroupResponse.getStatus());

      // try remove ROLE_ADMIN
      updateGroupResponse = endpoint.updateGroup(group.getGroupId(), group.getName(), group.getDescription(),
              "ROLE_USER", null);
      assertNotNull(updateGroupResponse);
      assertEquals(HttpStatus.SC_FORBIDDEN, updateGroupResponse.getStatus());
    } catch (NotFoundException e) {
      fail("The existing group isn't found");
    }
  }

  @Test
  public void testRemoveGroupNotAllowedAsNonAdminUser() throws UnauthorizedException {
    JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", Collections.set(
            new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1)));
    try {
      provider.addGroup(group);
      Group loadGroup = provider.loadGroup(group.getGroupId(), group.getOrganization().getId());
      assertNotNull(loadGroup);
      assertEquals(group.getGroupId(), loadGroup.getGroupId());
    } catch (Exception e) {
      fail("The group should be added");
    }

    JpaUser user = new JpaUser("user", "pass1", org1, "User", "user@localhost", "opencast", true,
            Collections.set(new JpaRole("ROLE_USER", org1)));

    // Set the security sevice
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);
    provider.setSecurityService(securityService);

    Response removeGroupResponse = endpoint.removeGroup(group.getGroupId());
    assertNotNull(removeGroupResponse);
    assertEquals(HttpStatus.SC_FORBIDDEN, removeGroupResponse.getStatus());
  }

  @Test
  public void testDuplicateGroupCreation() {
    Response response = endpoint.createGroup("Test 1", "Test group", "ROLE_ASTRO_101_SPRING_2011_STUDENT", "admin");
    assertEquals(HttpStatus.SC_CREATED, response.getStatus());
    response = endpoint.createGroup("Test 1", "Test group 2", "ROLE_ASTRO_101_SPRING_2011_STUDENT", "admin");
    assertEquals(HttpStatus.SC_CONFLICT, response.getStatus());
  }
}
