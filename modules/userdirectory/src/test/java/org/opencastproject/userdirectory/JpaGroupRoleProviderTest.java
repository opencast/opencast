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

package org.opencastproject.userdirectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.collections4.IteratorUtils;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JpaGroupRoleProviderTest {

  private JpaGroupRoleProvider provider = null;
  private static JpaOrganization org1 = new JpaOrganization("org1", "org1", "localhost", 80, "admin", "anon", null);
  private static JpaOrganization org2 = new JpaOrganization("org2", "org2", "127.0.0.1", 80, "admin", "anon", null);

  @BeforeEach
  public void setUp() throws Exception {
    JpaUser adminUser = new JpaUser("admin", "pass1", org1, "Admin", "admin@localhost", "opencast", true,
            Set.of(new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1)));

    // Set the security sevice
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(adminUser).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);

    provider = new JpaGroupRoleProvider();
    provider.setSecurityService(securityService);
    provider.setEntityManagerFactory(newEntityManagerFactory(JpaGroupRoleProvider.PERSISTENCE_UNIT));
    provider.setDBSessionFactory(getDbSessionFactory());
    provider.activate(null);
  }

  @Test
  public void testAddAndGetGroup() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));
    Set<String> members = new HashSet<String>();
    members.add("admin");

    JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", authorities, members);
    provider.addGroup(group);

    Group loadGroup = provider.loadGroup("test", org1.getId());
    Assertions.assertNotNull(loadGroup);
    Assertions.assertEquals(loadGroup.getGroupId(), loadGroup.getGroupId());
    Assertions.assertEquals(loadGroup.getName(), loadGroup.getName());
    Assertions.assertEquals(loadGroup.getDescription(), loadGroup.getDescription());
    Assertions.assertEquals(loadGroup.getOrganization(), loadGroup.getOrganization());
    Assertions.assertEquals(loadGroup.getRole(), loadGroup.getRole());
    Assertions.assertEquals(loadGroup.getRoles(), loadGroup.getRoles());
    Assertions.assertEquals(loadGroup.getMembers(), loadGroup.getMembers());

    Assertions.assertNull(provider.loadGroup("does not exist", org1.getId()),
            "Loading 'does not exist' should return null");
    Assertions.assertNull(provider.loadGroup("user1", org2.getId()), "Loading 'does not exist' should return null");
  }

  @Test
  public void testAddGroupNotAllowedAsNonAdminUser() {
    assertThrows(UnauthorizedException.class, () -> {
      JpaUser user = new JpaUser("user", "pass1", org1, "User", "user@localhost", "opencast", true,
          Set.of(new JpaRole("ROLE_USER", org1)));

      // Set the security sevice
      SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
      EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
      EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
      EasyMock.replay(securityService);
      provider.setSecurityService(securityService);

      JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", Set.of(
          new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1)));
      provider.addGroup(group);
      fail("The group with admin role should not be created by an non admin user");
    });
  }

  @Test
  public void testUpdateGroupNotAllowedAsNonAdminUser() throws UnauthorizedException {
    JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", Set.of(
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
            Set.of(new JpaRole("ROLE_USER", org1)));

    // Set the security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);
    provider.setSecurityService(securityService);

    assertThrows(UnauthorizedException.class, () ->
        provider.updateGroup(group.getGroupId(), group.getName(), group.getDescription(), "ROLE_USER", null));
  }

  @Test
  public void testRemoveGroupNotAllowedAsNonAdminUser() throws NotFoundException, Exception {
    JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", Set.of(
            new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1)));
    provider.addGroup(group);
    Group loadGroup = provider.loadGroup(group.getGroupId(), group.getOrganization().getId());
    assertNotNull(loadGroup);
    assertEquals(group.getGroupId(), loadGroup.getGroupId());

    JpaUser user = new JpaUser("user", "pass1", org1, "User", "user@localhost", "opencast", true,
            Set.of(new JpaRole("ROLE_USER", org1)));

    // Set the security sevice
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);
    provider.setSecurityService(securityService);

    assertThrows(UnauthorizedException.class, () ->
        provider.removeGroup(group.getGroupId()));
  }

  @Test
  public void testDuplicateGroupCreation() throws IllegalArgumentException, UnauthorizedException, ConflictException {
    // create the group, not exception thrown
    provider.createGroup("Test 1", "Test group", "ROLE_ASTRO_101_SPRING_2011_STUDENT", "admin");
    assertThrows(ConflictException.class, () ->
        provider.createGroup("Test 1", "Test group 2", "ROLE_ASTRO_101_SPRING_2011_STUDENT", "admin"));
  }

  @Test
  public void testDuplicateGroup() throws UnauthorizedException {
    Set<JpaRole> roles1 = Set.of(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));
    Set<JpaRole> roles2 = Set.of(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org2));
    Set<String> members = Set.of("admin");
    provider.addGroup(new JpaGroup("test1", org1, "Test", "Test group", roles1, members));
    provider.addGroup(new JpaGroup("test1", org2, "Test 2", "Test group 2", roles2, members));
    assertEquals("Test", provider.loadGroup("test1", org1.getId()).getName());
    // duplicate group, but add group does an update so it will pass
    provider.addGroup(new JpaGroup("test1", org1, "Test 1", "Test group 1", roles1, members));
    assertEquals("Test 1", provider.loadGroup("test1", org1.getId()).getName());
  }

  @Test
  public void testCreateGroupWithoutName() {
    assertThrows(IllegalArgumentException.class, () ->
        provider.createGroup(null, "Test group", null, null));
  }

  @Test
  public void testRolesForUser() throws UnauthorizedException {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));
    authorities.add(new JpaRole("ROLE_ASTRO_109_SPRING_2012_STUDENT", org1));
    Set<String> members = new HashSet<String>();
    members.add("admin");

    JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", authorities, members);
    provider.addGroup(group);

    authorities.clear();
    authorities.add(new JpaRole("ROLE_ASTRO_122_SPRING_2011_STUDENT", org1));
    authorities.add(new JpaRole("ROLE_ASTRO_124_SPRING_2012_STUDENT", org1));

    JpaGroup group2 = new JpaGroup("test2", org1, "Test2", "Test 2 group", authorities, members);
    provider.addGroup(group2);

    authorities.clear();
    authorities.add(new JpaRole("ROLE_ASTRO_134_SPRING_2011_STUDENT", org2));
    authorities.add(new JpaRole("ROLE_ASTRO_144_SPRING_2012_STUDENT", org2));

    JpaGroup group3 = new JpaGroup("test2", org2, "Test2", "Test 2 group", authorities, members);
    provider.addGroup(group3);

    List<Role> rolesForUser = provider.getRolesForUser("admin");
    Assertions.assertEquals(6, rolesForUser.size(), "There should be four roles");
    rolesForUser.contains(new JpaRole(group.getRole(), org1));
    rolesForUser.contains(new JpaRole(group2.getRole(), org1));
  }

  @Test
  public void testFindRoles() throws UnauthorizedException {
    // findRoles() should return a role per group, not the included roles for each group
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));
    authorities.add(new JpaRole("ROLE_ASTRO_109_SPRING_2012_STUDENT", org1));
    Set<String> members = new HashSet<String>();
    members.add("admin");

    JpaGroup group = new JpaGroup("test", org1, "Test", "Test group", authorities, members);
    provider.addGroup(group);

    Role role = provider.findRoles("%test%", Role.Target.ALL, 0, 0).next();
    Assertions.assertEquals("ROLE_GROUP_TEST", role.getName());

    authorities.clear();
    authorities.add(new JpaRole("ROLE_ASTRO_122_SPRING_2011_STUDENT", org1));
    authorities.add(new JpaRole("ROLE_ASTRO_124_SPRING_2012_STUDENT", org1));

    JpaGroup group2 = new JpaGroup("test2", org1, "Test2", "Test 2 group", authorities, members);
    provider.addGroup(group2);

    authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_134_SPRING_2011_STUDENT", org2));
    authorities.add(new JpaRole("ROLE_ASTRO_144_SPRING_2012_STUDENT", org2));

    JpaGroup group3 = new JpaGroup("test2", org2, "Test2", "Test 2 group", authorities, members);
    provider.addGroup(group3);

    Assertions.assertEquals(0, IteratorUtils.toList(provider.findRoles("%PrIn%", Role.Target.ALL, 0, 0)).size());
    Assertions.assertEquals(0, IteratorUtils.toList(provider.findRoles("%PrIn%", Role.Target.ALL, 0, 1)).size());

    Assertions.assertEquals(2, IteratorUtils.toList(provider.findRoles("%test%", Role.Target.ALL, 0, 2)).size());
  }

  @Test
  public void testRemoveUserFromAllGroups() throws UnauthorizedException {

    Set<String> members = new HashSet<String>();
    members.add("user");

    JpaGroup group1 = new JpaGroup("test1", org1, "Test1", "Test 1 group",
            new HashSet<JpaRole>(), members);
    JpaGroup group2 = new JpaGroup("test2", org1, "Test2", "Test 2 group",
            new HashSet<JpaRole>(), members);
    JpaGroup group3 = new JpaGroup("test3", org1, "Test3", "Test 3 group",
            new HashSet<JpaRole>(), members);

    provider.addGroup(group1);
    provider.addGroup(group2);
    provider.addGroup(group3);

    List<Role> groupRoles = provider.getRolesForUser("user");
    Assertions.assertEquals(3,groupRoles.size(), "There should be three groupRoles added to the user");
    Assertions.assertTrue(groupRoles.contains(new JpaRole(group1.getRole(), org1)),
        "GroupRole for group1 should be added");
    Assertions.assertTrue(groupRoles.contains(new JpaRole(group2.getRole(), org1)),
        "GroupRole for group2 should be added");
    Assertions.assertTrue(groupRoles.contains(new JpaRole(group3.getRole(), org1)),
        "GroupRole for group3 should be added");

    provider.removeMemberFromAllGroups("user", "org1");

    groupRoles = provider.getRolesForUser("user");
    Assertions.assertEquals(0,groupRoles.size(), "There should be no more groupRoles for the user");
    Assertions.assertFalse(groupRoles.contains(new JpaRole(group1.getRole(), org1)),
            "GroupRole for group1 should be removed");
    Assertions.assertFalse(groupRoles.contains(new JpaRole(group2.getRole(), org1)),
            "GroupRole for group2 should be removed");
    Assertions.assertFalse(groupRoles.contains(new JpaRole(group3.getRole(), org1)),
            "GroupRole for group3 should be removed");

    provider.removeMemberFromAllGroups("user", "org1");

    groupRoles = provider.getRolesForUser("user");
    Assertions.assertEquals(0,
            groupRoles.size(), "Make sure there is no issue with users that are not part of any group");
  }

  @Test
  public void testRemoveMemberInvalidatesUserAfterPersisting() throws Exception {
    provider.addGroup(new JpaGroup("test", org1, "Test", "Test group", new HashSet<>(), Set.of("user", "user2")));

    // The removal must already be persisted when the cached user is invalidated
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    userDirectoryService.invalidate("user");
    EasyMock.expectLastCall().andAnswer(() -> {
      assertEquals(Set.of("user2"), provider.loadGroup("test", org1.getId()).getMembers());
      return null;
    }).once();
    EasyMock.replay(userDirectoryService);
    provider.setUserDirectoryService(userDirectoryService);

    Assertions.assertTrue(provider.removeMemberFromGroup("test", "user"));
    EasyMock.verify(userDirectoryService);
  }

  @Test
  public void testAddMemberInvalidatesUserAfterPersisting() throws Exception {
    provider.addGroup(new JpaGroup("test", org1, "Test", "Test group", new HashSet<>(), Set.of("user2")));

    // The new member must already be persisted when the cached user is invalidated
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    userDirectoryService.invalidate("user");
    EasyMock.expectLastCall().andAnswer(() -> {
      assertEquals(Set.of("user", "user2"), provider.loadGroup("test", org1.getId()).getMembers());
      return null;
    }).once();
    EasyMock.replay(userDirectoryService);
    provider.setUserDirectoryService(userDirectoryService);

    Assertions.assertTrue(provider.addMemberToGroup("test", "user"));
    EasyMock.verify(userDirectoryService);
  }

  @Test
  public void testUpdateGroupInvalidatesUsersAfterPersisting() throws Exception {
    provider.addGroup(new JpaGroup("test", org1, "Test", "Test group", new HashSet<>(), Set.of("user", "user2")));

    // The new members must already be persisted when the added and the removed user are invalidated
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    for (String member : List.of("user", "user3")) {
      userDirectoryService.invalidate(member);
      EasyMock.expectLastCall().andAnswer(() -> {
        assertEquals(Set.of("user2", "user3"), provider.loadGroup("test", org1.getId()).getMembers());
        return null;
      }).once();
    }
    EasyMock.replay(userDirectoryService);
    provider.setUserDirectoryService(userDirectoryService);

    provider.updateGroup("test", "Test", "Test group", null, "user2,user3");
    EasyMock.verify(userDirectoryService);
  }

  @Test
  public void testNoUserInvalidationIfGroupChangeIsNotAllowed() throws Exception {
    provider.addGroup(new JpaGroup("test", org1, "Test", "Test group",
            Set.of(new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1)), Set.of("user")));

    JpaUser user = new JpaUser("user", "pass1", org1, "User", "user@localhost", "opencast", true,
            Set.of(new JpaRole("ROLE_USER", org1)));
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);
    provider.setSecurityService(securityService);

    // No call is expected: nothing changed, so no cached user may be invalidated
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.replay(userDirectoryService);
    provider.setUserDirectoryService(userDirectoryService);

    Assertions.assertThrows(UnauthorizedException.class, () -> provider.removeMemberFromGroup("test", "user"));
    Assertions.assertThrows(UnauthorizedException.class, () -> provider.addMemberToGroup("test", "user2"));
    Assertions.assertThrows(UnauthorizedException.class,
        () -> provider.updateGroup("test", "Test", "Test group", SecurityConstants.GLOBAL_ADMIN_ROLE, "user2"));

    assertEquals(Set.of("user"), provider.loadGroup("test", org1.getId()).getMembers());
    EasyMock.verify(userDirectoryService);
  }
}
