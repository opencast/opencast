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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.util.data.Collections.set;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.kernel.security.CustomPasswordEncoder;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;

import org.apache.commons.collections4.IteratorUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JpaUserProviderTest {

  private JpaUserAndRoleProvider provider = null;
  private JpaOrganization org1 = null;
  private JpaOrganization org2 = null;
  private CustomPasswordEncoder passwordEncoder = new CustomPasswordEncoder();

  @Before
  public void setUp() throws Exception {
    org1 = new JpaOrganization("org1", "org1", "localhost", 80, "admin", "anon", null);
    org2 = new JpaOrganization("org2", "org2", "127.0.0.1", 80, "admin", "anon", null);

    SecurityService securityService = mockSecurityServiceWithUser(
            createUserWithRoles(org1, "admin", SecurityConstants.GLOBAL_SYSTEM_ROLES));

    JpaGroupRoleProvider groupRoleProvider = EasyMock.createNiceMock(JpaGroupRoleProvider.class);

    provider = new JpaUserAndRoleProvider();
    provider.setSecurityService(securityService);
    provider.setEntityManagerFactory(newTestEntityManagerFactory(JpaUserAndRoleProvider.PERSISTENCE_UNIT));
    provider.setGroupRoleProvider(groupRoleProvider);
    provider.activate(null);
  }

  @Test
  public void testAddAndGetUser() throws Exception {
    JpaUser user = createUserWithRoles(org1, "user1", "ROLE_ASTRO_101_SPRING_2011_STUDENT");
    provider.addUser(user);

    User loadUser = provider.loadUser("user1");
    assertNotNull(loadUser);

    assertEquals(user.getUsername(), loadUser.getUsername());
    assertTrue(passwordEncoder.isPasswordValid(loadUser.getPassword(), user.getPassword(), null));
    assertEquals(user.getOrganization(), loadUser.getOrganization());
    assertEquals(user.getRoles(), loadUser.getRoles());

    assertNull("Loading 'does not exist' should return null", provider.loadUser("does not exist"));
    assertNull("Loading 'does not exist' should return null", provider.loadUser("user1", org2.getId()));

    loadUser = provider.loadUser("user1", org1.getId());
    assertNotNull(loadUser);

    assertEquals(user.getUsername(), loadUser.getUsername());
    assertTrue(passwordEncoder.isPasswordValid(loadUser.getPassword(), user.getPassword(), null));
    assertEquals(user.getOrganization(), loadUser.getOrganization());
    assertEquals(user.getRoles(), loadUser.getRoles());
  }

  @Test
  public void testAddUserWithGlobalAdminRole() throws Exception {
    JpaUser adminUser = createUserWithRoles(org1, "admin1", SecurityConstants.GLOBAL_ADMIN_ROLE);
    provider.addUser(adminUser);
    User loadedUser = provider.loadUser(adminUser.getUsername());
    assertNotNull("The currently added user isn't loaded as expected", loadedUser);
    assertEquals(adminUser.getUsername(), loadedUser.getUsername());
    assertEquals(adminUser.getRoles(), loadedUser.getRoles());
  }

  @Test
  public void testAddUserWithOrgAdminRoleAsGlobalAdmin() throws Exception {
    JpaUser newUser = createUserWithRoles(org1, "org_admin2", org1.getAdminRole());
    provider.addUser(newUser);
    User loadedUser = provider.loadUser(newUser.getUsername());
    assertNotNull("The currently added user isn't loaded as expected", loadedUser);
    assertEquals(newUser.getUsername(), loadedUser.getUsername());
    assertEquals(newUser.getRoles(), loadedUser.getRoles());
  }

  @Test
  public void testAddUserWithOrgAdminRoleAsOrgAdmin() throws Exception {
    provider.setSecurityService(mockSecurityServiceWithUser(
            createUserWithRoles(org1, "org_admin", org1.getAdminRole())));
    JpaUser newUser = createUserWithRoles(org1, "org_admin2", org1.getAdminRole());
    provider.addUser(newUser);
    User loadedUser = provider.loadUser(newUser.getUsername());
    assertNotNull("The currently added user isn't loaded as expected", loadedUser);
    assertEquals(newUser.getUsername(), loadedUser.getUsername());
    assertEquals(newUser.getRoles(), loadedUser.getRoles());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddUserWithGlobalAdminRoleNotAllowedAsNonAdminUser() throws Exception {
    provider.setSecurityService(mockSecurityServiceWithUser(
            createUserWithRoles(org1, "user1", "ROLE_USER")));
    JpaUser newUser = createUserWithRoles(org1, "admin2", SecurityConstants.GLOBAL_ADMIN_ROLE);
    provider.addUser(newUser);
    fail("The current user shouldn't able to create an global admin user.");
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddUserWithGlobalAdminRoleNotAllowedAsOrgAdmin() throws Exception {
    provider.setSecurityService(mockSecurityServiceWithUser(
            createUserWithRoles(org1, "org1_admin", org1.getAdminRole())));
    JpaUser newUser = createUserWithRoles(org1, "admin2", SecurityConstants.GLOBAL_ADMIN_ROLE);
    provider.addUser(newUser);
    fail("The current user shouldn't able to create an global admin user.");
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddUserWithOrgAdminRoleNotAllowedAsNonAdminUser() throws Exception {
    provider.setSecurityService(mockSecurityServiceWithUser(
            createUserWithRoles(org1, "user1", "ROLE_USER")));
    JpaUser newUser = createUserWithRoles(org1, "org_admin2", org1.getAdminRole());
    provider.addUser(newUser);
    fail("The current user shouldn't able to create an global admin user.");
   }

  @Test
  public void testDeleteUser() throws Exception {
    JpaUser user1 = createUserWithRoles(org1, "user1", "ROLE_ASTRO_101_SPRING_2011_STUDENT");
    JpaUser user2 = createUserWithRoles(org1, "user2", "ROLE_ASTRO_101_SPRING_2011_STUDENT");
    JpaUser user3 = createUserWithRoles(org1, "user3", "ROLE_ASTRO_101_SPRING_2011_STUDENT");
    JpaUser user4 = createUserWithRoles(org1, "user4", "ROLE_ASTRO_101_SPRING_2011_STUDENT");
    provider.addUser(user1);
    provider.addUser(user2);
    provider.addUser(user3);
    provider.addUser(user4);

    User loadUser = provider.loadUser("user1");

    assertNotNull(loadUser);

    provider.deleteUser("user1", user1.getOrganization().getId());
    provider.deleteUser("user2", user1.getOrganization().getId());
    provider.deleteUser("user3", user1.getOrganization().getId());

    assertNull(provider.loadUser("user1", org1.getId()));
    assertNull(provider.loadUser("user2", org1.getId()));
    assertNull(provider.loadUser("user3", org1.getId()));
    assertNotNull(provider.loadUser("user4", org1.getId()));

    try {
      provider.deleteUser("user1", user1.getOrganization().getId());
      fail("Should throw a NotFoundException");
    } catch (NotFoundException e) {
      assertTrue("User not found.", true);
    }
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteUserNotAllowedAsNonAdmin() throws UnauthorizedException, Exception {
    JpaUser adminUser = createUserWithRoles(org1, "admin", "ROLE_ADMIN");
    JpaUser nonAdminUser = createUserWithRoles(org1, "user1", "ROLE_USER");
    try {
      provider.addUser(adminUser);
      provider.addUser(nonAdminUser);
    } catch (UnauthorizedException ex) {
      fail("The user shuld be created");
    }

    provider.setSecurityService(mockSecurityServiceWithUser(nonAdminUser));
    provider.deleteUser(adminUser.getUsername(), org1.getId());
    fail("An non admin user may not delete an admin user");
  }

  @Test
  public void testUpdateUser() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));

    JpaUser user = new JpaUser("user1", "pass1", org1, provider.getName(), true, authorities);
    provider.addUser(user);

    User loadUser = provider.loadUser("user1");

    assertNotNull(loadUser);

    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2013_STUDENT", org1));
    String newPassword = "newPassword";
    JpaUser updateUser = new JpaUser(user.getUsername(), newPassword, org1, provider.getName(), true, authorities);

    User loadUpdatedUser = provider.updateUser(updateUser);
    // User loadUpdatedUser = provider.loadUser(user.getUsername());

    assertNotNull(loadUpdatedUser);
    assertEquals(user.getUsername(), loadUpdatedUser.getUsername());
    assertTrue(passwordEncoder.isPasswordValid(loadUpdatedUser.getPassword(), newPassword, null));
    assertEquals(authorities.size(), loadUpdatedUser.getRoles().size());

    updateUser = new JpaUser("unknown", newPassword, org1, provider.getName(), true, authorities);

    try {
      provider.updateUser(updateUser);
      fail("Should throw a NotFoundException");
    } catch (NotFoundException e) {
      assertTrue("User not found.", true);
    }

  }

  @Test
  public void testUpdateUserForbiddenForNonAdminUsers() throws Exception {
    JpaUser adminUser = createUserWithRoles(org1, "admin", SecurityConstants.GLOBAL_ADMIN_ROLE);
    JpaUser user = createUserWithRoles(org1, "user", "ROLE_USER");

    provider.addUser(adminUser);
    provider.addUser(user);
    provider.setSecurityService(mockSecurityServiceWithUser(user));

    // try to add ROLE_USER
    Set<JpaRole> updatedRoles = Collections.set(
            new JpaRole("ROLE_USER", org1),
            new JpaRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org1));

    try {
      provider.updateUser(new JpaUser(adminUser.getUsername(), adminUser.getPassword(), org1,
              adminUser.getName(), true, updatedRoles));
      fail("The current user may not edit an admin user");
    } catch (UnauthorizedException e) {
      // pass
    }

    // try to remove ROLE_ADMIN
    updatedRoles = Collections.set(new JpaRole("ROLE_USER", org1));
    try {
      provider.updateUser(new JpaUser(adminUser.getUsername(), adminUser.getPassword(), org1,
              adminUser.getName(), true, updatedRoles));
      fail("The current user may not remove the admin role on other user");
    } catch (UnauthorizedException e) {
      // pass
    }
  }

  @Test
  public void testRoles() throws Exception {
    JpaUser userOne = createUserWithRoles(org1, "user1", "ROLE_ONE");
    provider.addUser(userOne);

    Set<JpaRole> authoritiesTwo = new HashSet<JpaRole>();
    authoritiesTwo.add(new JpaRole("ROLE_ONE", org1));
    authoritiesTwo.add(new JpaRole("ROLE_TWO", org1));
    JpaUser userTwo = createUserWithRoles(org1, "user2", "ROLE_ONE", "ROLE_TWO");
    provider.addUser(userTwo);

    // The provider is not authoritative for these roles
    assertEquals("There should be no roles", 0, IteratorUtils.toList(provider.findRoles("%", Role.Target.ALL, 0, 0)).size());
  }

  @Test
  public void testUsers() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_COOL_ONE", org1));

    JpaUser userOne = createUserWithRoles(org1, "user_test_1", "ROLE_COOL_ONE");
    JpaUser userTwo = createUserWithRoles(org1, "user2", "ROLE_CCOL_ONE");
    JpaUser userThree = createUserWithRoles(org1, "user3", "ROLE_COOL_ONE");
    JpaUser userFour = createUserWithRoles(org1, "user_test_4", "ROLE_COOL_ONE");
    provider.addUser(userOne);
    provider.addUser(userTwo);
    provider.addUser(userThree);
    provider.addUser(userFour);

    assertEquals("There should be two roles", 4, IteratorUtils.toList(provider.getUsers()).size());
  }

  @Test
  public void testDuplicateUser() {
    Set<JpaRole> authorities1 = set(new JpaRole("ROLE_COOL_ONE", org1));
    Set<JpaRole> authorities2 = set(new JpaRole("ROLE_COOL_ONE", org2));
    try {
      provider.addUser(createUserWithRoles(org1, "user1", "ROLE_COOL_ONE"));
      provider.addUser(createUserWithRoles(org1, "user2", "ROLE_COOL_ONE"));
      provider.addUser(createUserWithRoles(org2, "user1", "ROLE_COOL_ONE"));
    } catch (UnauthorizedException e) {
      fail("User should be created");
    }
    try {
      provider.addUser(createUserWithRoles(org1, "user1", "ROLE_COOL_ONE"));
      fail("Duplicate user");
    } catch (Exception ignore) {
    }
  }

  @Test
  public void testRolesForUser() {
    JpaRole astroRole = new JpaRole("ROLE_ASTRO_105_SPRING_2013_STUDENT", org1, "Astro role");

    provider.addRole(astroRole);

    JpaUser userOne = createUserWithRoles(org1, "user1", "ROLE_ONE", "ROLE_TWO");
    try {
      provider.addUser(userOne);
    } catch (UnauthorizedException e) {
      fail("User should be created");
    }

    // Provider not authoritative for these roles
    assertEquals("There should be zero roles", 0, IteratorUtils.toList(provider.findRoles("%", Role.Target.ALL, 0, 0)).size());

    List<String> rolesForUser = provider.getRolesForUser("user1").stream()
            .map(Role::getName)
            .sorted()
            .collect(Collectors.toList());
    assertEquals("There should be two roles", 2, rolesForUser.size());
    assertEquals("ROLE_ONE", rolesForUser.get(0));
    assertEquals("ROLE_TWO", rolesForUser.get(1));
  }

  @Test
  public void testFindUsers() throws UnauthorizedException {
    JpaUser userOne = createUserWithRoles(org1, "user_test_1", "ROLE_COOL_ONE");
    JpaUser userTwo = createUserWithRoles(org1, "user2", "ROLE_COOL_ONE");
    JpaUser userThree = createUserWithRoles(org1, "user3", "ROLE_COOL_ONE");
    JpaUser userFour = createUserWithRoles(org1, "user_test_4", "ROLE_COOL_ONE");
    provider.addUser(userOne);
    provider.addUser(userTwo);
    provider.addUser(userThree);
    provider.addUser(userFour);

    assertEquals(2, IteratorUtils.toList(provider.findUsers("%tEsT%", 0, 0)).size());
    assertEquals(1, IteratorUtils.toList(provider.findUsers("%tEsT%", 0, 1)).size());
    User user = provider.findUsers("%tEsT%", 1, 1).next();
    assertEquals(userFour, user);
  }

  @Test
  public void testFindRoles() throws UnauthorizedException {
    JpaRole astroRole = new JpaRole("ROLE_ASTRO_105_SPRING_2013_STUDENT", org1, "Astro role");

    provider.addRole(astroRole);

    JpaUser userOne = createUserWithRoles(org1, "user1", "ROLE_COOL_ONE", "ROLE_COOL_TWO");
    provider.addUser(userOne);

    // We expect findRoles() for this provider to return an empty set,
    // as it is not authoritative for roles that it persists.
    assertEquals(0, IteratorUtils.toList(provider.findRoles("%coOL%", Role.Target.ALL, 0, 0)).size());
    assertEquals(0, IteratorUtils.toList(provider.findRoles("%cOoL%", Role.Target.ALL, 0, 1)).size());

    assertEquals(0, IteratorUtils.toList(provider.findRoles("%oLe%", Role.Target.ALL, 0, 0)).size());
    assertEquals(0, IteratorUtils.toList(provider.findRoles("%olE%", Role.Target.ALL, 1, 2)).size());
  }

  private static SecurityService mockSecurityServiceWithUser(User currentUser) {
    // Set the security sevice
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(currentUser.getOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(currentUser).anyTimes();
    EasyMock.replay(securityService);
    return securityService;
  }

  private static JpaUser createUserWithRoles(JpaOrganization org, String username, String... roles) {
    Set<JpaRole> userRoles = new HashSet<>();
    for (String adminRole : roles) {
      userRoles.add(new JpaRole(adminRole, org));
    }
    return new JpaUser(username, "pass1", org, "opencast", true, userRoles);
  }
}
