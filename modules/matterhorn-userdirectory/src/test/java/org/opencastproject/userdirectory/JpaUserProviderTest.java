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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import junit.framework.Assert;
import org.apache.commons.collections.IteratorUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.PasswordEncoder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.opencastproject.util.data.Collections.set;

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
    Assert.assertNotNull(loadUser);

    Assert.assertEquals(user.getUsername(), loadUser.getUsername());
    Assert.assertEquals(PasswordEncoder.encode(user.getPassword(), user.getUsername()), loadUser.getPassword());
    Assert.assertEquals(user.getOrganization(), loadUser.getOrganization());
    Assert.assertEquals(user.getRoles(), loadUser.getRoles());

    Assert.assertNull("Loading 'does not exist' should return null", provider.loadUser("does not exist"));
    Assert.assertNull("Loading 'does not exist' should return null", provider.loadUser("user1", org2.getId()));

    loadUser = provider.loadUser("user1", org1.getId());
    Assert.assertNotNull(loadUser);

    Assert.assertEquals(user.getUsername(), loadUser.getUsername());
    Assert.assertEquals(PasswordEncoder.encode(user.getPassword(), user.getUsername()), loadUser.getPassword());
    Assert.assertEquals(user.getOrganization(), loadUser.getOrganization());
    Assert.assertEquals(user.getRoles(), loadUser.getRoles());
  }

  @Test
  public void testAddAndGetRole() throws Exception {
    JpaRole astroRole = new JpaRole("ROLE_ASTRO_105_SPRING_2013_STUDENT", org1, "Astro role");

    provider.addRole(astroRole);

    Iterator<Role> roles = provider.getRoles();
    Assert.assertTrue("There should be one role", roles.hasNext());

    Role role = roles.next();
    Assert.assertEquals(astroRole.getName(), role.getName());
    Assert.assertEquals(astroRole.getOrganization(), role.getOrganization());
    Assert.assertEquals(astroRole.getDescription(), role.getDescription());

    Assert.assertFalse("There should onyl be one role", roles.hasNext());
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

    Assert.assertEquals("There should be two roles", 2, IteratorUtils.toList(provider.getRoles()).size());
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

    Assert.assertEquals("There should be two roles", 4, IteratorUtils.toList(provider.getUsers()).size());
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

    Assert.assertEquals("There should be three roles", 3, IteratorUtils.toList(provider.getRoles()).size());

    List<Role> rolesForUser = provider.getRolesForUser("user1");
    Assert.assertEquals("There should be two roles", 2, rolesForUser.size());
    Assert.assertEquals("ROLE_ONE", rolesForUser.get(0).getName());
    Assert.assertEquals("ROLE_TWO", rolesForUser.get(1).getName());
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

    Assert.assertEquals(2, IteratorUtils.toList(provider.findUsers("%tEsT%", 0, 0)).size());
    Assert.assertEquals(1, IteratorUtils.toList(provider.findUsers("%tEsT%", 0, 1)).size());
    User user = provider.findUsers("%tEsT%", 1, 1).next();
    Assert.assertEquals(userFour, user);
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

    Assert.assertEquals(2, IteratorUtils.toList(provider.findRoles("%coOL%", 0, 0)).size());
    Assert.assertEquals(1, IteratorUtils.toList(provider.findRoles("%cOoL%", 0, 1)).size());

    Assert.assertEquals(3, IteratorUtils.toList(provider.findRoles("%oLe%", 0, 0)).size());
    Assert.assertEquals(2, IteratorUtils.toList(provider.findRoles("%olE%", 1, 2)).size());
  }

}
