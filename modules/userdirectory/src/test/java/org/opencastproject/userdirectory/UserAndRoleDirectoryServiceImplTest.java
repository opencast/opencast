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

package org.opencastproject.userdirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import org.apache.commons.collections4.IteratorUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tests the combined user and role directory service.
 */
public class UserAndRoleDirectoryServiceImplTest {

  /** The user and role directory */
  private UserAndRoleDirectoryServiceImpl directory = null;

  /** A username */
  private String userName = null;

  /** An organization */
  private JaxbOrganization org = null;

  private JaxbRole role1;
  private JaxbRole role2;
  private JaxbRole role3;

  @Before
  public void setUp() throws Exception {
    org = new DefaultOrganization();
    userName = "sampleUser";

    role1 = new JaxbRole("role1", org);
    role2 = new JaxbRole("role2", org);
    role3 = new JaxbRole("role3", org);

    JaxbUser user1 = new JaxbUser(userName, "matterhorn", org, role1, role2);
    user1.setManageable(true);
    User user2 = new JaxbUser(userName, "secret", "test", org, role2, role3);
    User user3 = new JaxbUser("userSample", "test", org, role2, role3);

    List<User> users = new ArrayList<User>();
    users.add(user1);

    UserProvider provider1 = EasyMock.createNiceMock(UserProvider.class);
    EasyMock.expect(provider1.getOrganization()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(provider1.loadUser((String) EasyMock.anyObject())).andReturn(user1).anyTimes();
    EasyMock.expect(provider1.findUsers("%mple%", 0, 0)).andReturn(users.iterator()).once();
    EasyMock.expect(provider1.findUsers("%mple%", 0, 0)).andReturn(users.iterator()).once();
    EasyMock.expect(provider1.getUsers()).andReturn(users.iterator()).once();
    EasyMock.expect(provider1.getName()).andReturn("test").once();

    List<User> users2 = new ArrayList<User>();
    users2.add(user3);

    UserProvider provider2 = EasyMock.createNiceMock(UserProvider.class);
    EasyMock.expect(provider2.getOrganization()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(provider2.loadUser((String) EasyMock.anyObject())).andReturn(user2).anyTimes();
    EasyMock.expect(provider2.findUsers("%mple%", 0, 0)).andReturn(users2.iterator()).once();
    EasyMock.expect(provider2.findUsers("%mple%", 0, 0)).andReturn(users2.iterator()).once();
    EasyMock.expect(provider2.getUsers()).andReturn(users2.iterator()).once();
    EasyMock.expect(provider2.getName()).andReturn("matterhorn").once();

    List<Role> roles1 = new ArrayList<Role>();
    roles1.add(new JaxbRole("ROLE_ASTRO_2011", org));
    roles1.add(new JaxbRole("ROLE_ASTRO_2012", org));

    List<Role> rolesForUser1 = new ArrayList<Role>();
    rolesForUser1.add(new JaxbRole("ROLE_ASTRO_2012", org));

    List<Role> findRoles1 = new ArrayList<Role>();
    findRoles1.add(new JaxbRole("ROLE_ASTRO_2012", org));

    RoleProvider roleProvider1 = EasyMock.createNiceMock(RoleProvider.class);
    EasyMock.expect(roleProvider1.getOrganization()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(roleProvider1.getRolesForUser((String) EasyMock.anyObject())).andReturn(rolesForUser1).anyTimes();
    EasyMock.expect(roleProvider1.findRoles("%", Role.Target.ALL, 0, 0)).andReturn(roles1.iterator()).anyTimes();
    EasyMock.expect(roleProvider1.findRoles("%2012%", Role.Target.ALL, 0, 0)).andReturn(findRoles1.iterator()).once();
    EasyMock.expect(roleProvider1.findRoles("%2012%", Role.Target.ALL, 0, 0)).andReturn(findRoles1.iterator()).once();

    List<Role> roles2 = new ArrayList<Role>();
    roles2.add(new JaxbRole("ROLE_MATH_2011", org));
    roles2.add(new JaxbRole("ROLE_MATH_2012", org));

    List<Role> rolesForUser2 = new ArrayList<Role>();
    rolesForUser2.add(new JaxbRole("ROLE_MATH_2012", org));

    List<Role> findRoles2 = new ArrayList<Role>();
    findRoles2.add(new JaxbRole("ROLE_MATH_2012", org));

    RoleProvider roleProvider2 = EasyMock.createNiceMock(RoleProvider.class);
    EasyMock.expect(roleProvider2.getOrganization()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(roleProvider2.getRolesForUser((String) EasyMock.anyObject())).andReturn(rolesForUser2).anyTimes();
    EasyMock.expect(roleProvider2.findRoles("%", Role.Target.ALL, 0, 0)).andReturn(roles2.iterator()).anyTimes();
    EasyMock.expect(roleProvider2.findRoles("%2012%", Role.Target.ALL, 0, 0)).andReturn(findRoles2.iterator()).once();
    EasyMock.expect(roleProvider2.findRoles("%2012%", Role.Target.ALL, 0, 0)).andReturn(findRoles2.iterator()).once();

    RoleProvider otherOrgRoleProvider = EasyMock.createNiceMock(RoleProvider.class);
    EasyMock.expect(otherOrgRoleProvider.getOrganization()).andReturn("otherOrg").anyTimes();
    EasyMock.expect(otherOrgRoleProvider.getRolesForUser((String) EasyMock.anyObject())).andReturn(rolesForUser2)
            .anyTimes();

    EasyMock.expect(otherOrgRoleProvider.findRoles("%", Role.Target.ALL, 0, 0)).andReturn(roles2.iterator())
            .anyTimes();
    EasyMock.expect(otherOrgRoleProvider.findRoles("%2012%", Role.Target.ALL, 0, 0)).andReturn(new ArrayList<Role>().iterator())
            .anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();

    EasyMock.replay(provider1, provider2, roleProvider1, roleProvider2, otherOrgRoleProvider, securityService);

    directory = new UserAndRoleDirectoryServiceImpl();
    directory.activate(null);
    directory.setSecurityService(securityService);
    directory.addUserProvider(provider1);
    directory.addUserProvider(provider2);
    directory.addRoleProvider(roleProvider1);
    directory.addRoleProvider(roleProvider2);
    directory.addRoleProvider(otherOrgRoleProvider);
  }

  @Test
  public void testUserMerge() throws Exception {
    User mergedUser = directory.loadUser(userName);
    Set<Role> roles = mergedUser.getRoles();
    assertTrue(roles.contains(role1));
    assertTrue(roles.contains(role2));
    assertTrue(roles.contains(role3));
    assertNotNull(mergedUser.getPassword());
    assertEquals(org.getId(), mergedUser.getOrganization().getId());
    assertEquals(userName, mergedUser.getUsername());
    assertEquals("matterhorn,test", mergedUser.getProvider());
    assertTrue(mergedUser.isManageable());
    assertTrue(((JaxbUser) mergedUser).isManageable());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetRoles() {
    List<Role> roles = IteratorUtils.toList(directory.findRoles("%", Role.Target.ALL, 0, 0));
    Assert.assertEquals(4, roles.size());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetUsers() {
    List<User> users = IteratorUtils.toList(directory.getUsers());
    Assert.assertEquals(2, users.size());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFindUsers() {
    List<User> users = IteratorUtils.toList(directory.findUsers("%mple%", 0, 0));
    Assert.assertEquals(2, users.size());
    Assert.assertTrue(userName.equals(users.get(0).getUsername()) || userName.equals(users.get(1).getUsername()));
    Assert.assertTrue("userSample".equals(users.get(0).getUsername())
            || "userSample".equals(users.get(1).getUsername()));

    // Test limit and offset
    users = IteratorUtils.toList(directory.findUsers("%mple%", 1, 1));
    Assert.assertEquals(1, users.size());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFindRoles() {
    List<Role> roles = IteratorUtils.toList(directory.findRoles("%2012%", Role.Target.ALL, 0, 0));
    Assert.assertEquals(2, roles.size());
    Assert.assertTrue("ROLE_MATH_2012".equals(roles.get(0).getName())
            || "ROLE_MATH_2012".equals(roles.get(1).getName()));
    Assert.assertTrue("ROLE_ASTRO_2012".equals(roles.get(0).getName())
            || "ROLE_ASTRO_2012".equals(roles.get(1).getName()));

    // Test limit and offset
    roles = IteratorUtils.toList(directory.findRoles("%2012%", Role.Target.ALL, 1, 1));
    Assert.assertEquals(1, roles.size());
    Assert.assertTrue("ROLE_ASTRO_2012".equals(roles.get(0).getName())
            || "ROLE_MATH_2012".equals(roles.get(0).getName()));
  }

}
