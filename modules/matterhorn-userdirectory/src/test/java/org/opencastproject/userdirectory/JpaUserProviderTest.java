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
package org.opencastproject.userdirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.util.data.Collections.set;

import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PasswordEncoder;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.apache.commons.collections.IteratorUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JpaUserProviderTest {

  private ComboPooledDataSource pooledDataSource = null;
  private JpaUserAndRoleProvider provider = null;
  private JpaOrganization org1 = null;
  private JpaOrganization org2 = null;

  @Before
  public void setUp() throws Exception {
    org1 = new JpaOrganization("org1", "org1", "localhost", 80, "admin", "anon", null);
    org2 = new JpaOrganization("org2", "org2", "127.0.0.1", 80, "admin", "anon", null);

    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    // Set the security sevice
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);

    provider = new JpaUserAndRoleProvider();
    provider.setSecurityService(securityService);
    provider.setPersistenceProperties(props);
    provider.setPersistenceProvider(new PersistenceProvider());
    provider.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    provider.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testAddAndGetUser() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));

    JpaUser user = new JpaUser("user1", "pass1", org1, authorities);
    provider.addUser(user);

    User loadUser = provider.loadUser("user1");
    assertNotNull(loadUser);

    assertEquals(user.getUsername(), loadUser.getUsername());
    assertEquals(PasswordEncoder.encode(user.getPassword(), user.getUsername()), loadUser.getPassword());
    assertEquals(user.getOrganization(), loadUser.getOrganization());
    assertEquals(user.getRoles(), loadUser.getRoles());

    assertNull("Loading 'does not exist' should return null", provider.loadUser("does not exist"));
    assertNull("Loading 'does not exist' should return null", provider.loadUser("user1", org2.getId()));

    loadUser = provider.loadUser("user1", org1.getId());
    assertNotNull(loadUser);

    assertEquals(user.getUsername(), loadUser.getUsername());
    assertEquals(PasswordEncoder.encode(user.getPassword(), user.getUsername()), loadUser.getPassword());
    assertEquals(user.getOrganization(), loadUser.getOrganization());
    assertEquals(user.getRoles(), loadUser.getRoles());
  }

  @Test
  public void testAddAndGetRole() throws Exception {
    JpaRole astroRole = new JpaRole("ROLE_ASTRO_105_SPRING_2013_STUDENT", org1, "Astro role");

    provider.addRole(astroRole);

    Iterator<Role> roles = provider.getRoles();
    assertTrue("There should be one role", roles.hasNext());

    Role role = roles.next();
    assertEquals(astroRole.getName(), role.getName());
    assertEquals(astroRole.getOrganization(), role.getOrganization());
    assertEquals(astroRole.getDescription(), role.getDescription());

    assertFalse("There should onyl be one role", roles.hasNext());
  }

  @Test
  public void testDeleteUser() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));

    JpaUser user1 = new JpaUser("user1", "pass1", org1, authorities);
    JpaUser user2 = new JpaUser("user2", "pass1", org1, authorities);
    JpaUser user3 = new JpaUser("user3", "pass1", org1, authorities);
    JpaUser user4 = new JpaUser("user4", "pass1", org1, authorities);
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

  @Test
  public void testUpdateUser() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2011_STUDENT", org1));

    JpaUser user = new JpaUser("user1", "pass1", org1, authorities);
    provider.addUser(user);

    User loadUser = provider.loadUser("user1");

    assertNotNull(loadUser);

    authorities.add(new JpaRole("ROLE_ASTRO_101_SPRING_2013_STUDENT", org1));
    String newPassword = "newPassword";
    JpaUser updateUser = new JpaUser(user.getUsername(), newPassword, org1, authorities);

    User loadUpdatedUser = provider.updateUser(updateUser);
    // User loadUpdatedUser = provider.loadUser(user.getUsername());

    assertNotNull(loadUpdatedUser);
    assertEquals(user.getUsername(), loadUpdatedUser.getUsername());
    assertEquals(PasswordEncoder.encode(newPassword, user.getUsername()), loadUpdatedUser.getPassword());
    assertEquals(authorities.size(), loadUpdatedUser.getRoles().size());

    updateUser.username = "unknown";

    try {
      provider.updateUser(updateUser);
      fail("Should throw a NotFoundException");
    } catch (NotFoundException e) {
      assertTrue("User not found.", true);
    }

  }

  @Test
  public void testRoles() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ONE", org1));

    JpaUser userOne = new JpaUser("user1", "pass1", org1, authorities);
    provider.addUser(userOne);

    Set<JpaRole> authoritiesTwo = new HashSet<JpaRole>();
    authoritiesTwo.add(new JpaRole("ROLE_ONE", org1));
    authoritiesTwo.add(new JpaRole("ROLE_TWO", org1));
    JpaUser userTwo = new JpaUser("user2", "pass2", org1, authoritiesTwo);
    provider.addUser(userTwo);

    assertEquals("There should be two roles", 2, IteratorUtils.toList(provider.getRoles()).size());
  }

  @Test
  public void testUsers() throws Exception {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_COOL_ONE", org1));

    JpaUser userOne = new JpaUser("user_test_1", "pass1", org1, authorities);
    JpaUser userTwo = new JpaUser("user2", "pass2", org1, authorities);
    JpaUser userThree = new JpaUser("user3", "pass3", org1, authorities);
    JpaUser userFour = new JpaUser("user_test_4", "pass4", org1, authorities);
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
    provider.addUser(new JpaUser("user1", "pass1", org1, authorities1));
    provider.addUser(new JpaUser("user2", "pass2", org1, authorities1));
    provider.addUser(new JpaUser("user1", "pass3", org2, authorities2));
    try {
      provider.addUser(new JpaUser("user1", "pass4", org1, authorities1));
      fail("Duplicate user");
    } catch (Exception ignore) {
    }
  }

  @Test
  public void testRolesForUser() {
    JpaRole astroRole = new JpaRole("ROLE_ASTRO_105_SPRING_2013_STUDENT", org1, "Astro role");

    provider.addRole(astroRole);

    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_ONE", org1));
    authorities.add(new JpaRole("ROLE_TWO", org1));

    JpaUser userOne = new JpaUser("user1", "pass1", org1, authorities);
    provider.addUser(userOne);

    assertEquals("There should be three roles", 3, IteratorUtils.toList(provider.getRoles()).size());

    List<Role> rolesForUser = provider.getRolesForUser("user1");
    assertEquals("There should be two roles", 2, rolesForUser.size());
    assertEquals("ROLE_ONE", rolesForUser.get(0).getName());
    assertEquals("ROLE_TWO", rolesForUser.get(1).getName());
  }

  @Test
  public void testFindUsers() {
    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_COOL_ONE", org1));

    JpaUser userOne = new JpaUser("user_test_1", "pass1", org1, authorities);
    JpaUser userTwo = new JpaUser("user2", "pass2", org1, authorities);
    JpaUser userThree = new JpaUser("user3", "pass3", org1, authorities);
    JpaUser userFour = new JpaUser("user_test_4", "pass4", org1, authorities);
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
  public void testFindRoles() {
    JpaRole astroRole = new JpaRole("ROLE_ASTRO_105_SPRING_2013_STUDENT", org1, "Astro role");

    provider.addRole(astroRole);

    Set<JpaRole> authorities = new HashSet<JpaRole>();
    authorities.add(new JpaRole("ROLE_COOL_ONE", org1));
    authorities.add(new JpaRole("ROLE_COOL_TWO", org1));

    JpaUser userOne = new JpaUser("user1", "pass1", org1, authorities);
    provider.addUser(userOne);

    assertEquals(2, IteratorUtils.toList(provider.findRoles("%coOL%", 0, 0)).size());
    assertEquals(1, IteratorUtils.toList(provider.findRoles("%cOoL%", 0, 1)).size());

    assertEquals(3, IteratorUtils.toList(provider.findRoles("%oLe%", 0, 0)).size());
    assertEquals(2, IteratorUtils.toList(provider.findRoles("%olE%", 1, 2)).size());
  }

}
