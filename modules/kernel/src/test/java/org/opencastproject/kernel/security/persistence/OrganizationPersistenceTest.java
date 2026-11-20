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

package org.opencastproject.kernel.security.persistence;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;
import static org.opencastproject.kernel.security.persistence.OrganizationDatabaseImpl.PERSISTENCE_UNIT;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.util.NotFoundException;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests organization persistence: storing, merging, retrieving and removing.
 */
public class OrganizationPersistenceTest {

  private OrganizationDatabaseImpl organizationDatabase;

  private SecurityService securityService;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    organizationDatabase = new OrganizationDatabaseImpl();
    organizationDatabase.setEntityManagerFactory(newEntityManagerFactory(PERSISTENCE_UNIT));
    organizationDatabase.setDBSessionFactory(getDbSessionFactory());
    organizationDatabase.setSecurityService(securityService);
    organizationDatabase.activate(null);
  }

  @Test
  public void testAdding() throws Exception {
    Map<String, String> orgProperties = new HashMap<String, String>();
    orgProperties.put("test", "one");
    JpaOrganization org = new JpaOrganization("newOrg", "test organization", "test.org", 8080, "ROLE_TEST_ADMIN",
            "ROLE_TEST_ANONYMOUS", orgProperties);

    organizationDatabase.storeOrganization(org);

    Assertions.assertTrue(organizationDatabase.containsOrganization("newOrg"));

    Organization orgById = organizationDatabase.getOrganization("newOrg");
    try {
      organizationDatabase.getOrganizationByHost("test.org", 8081);
      Assertions.fail();
    } catch (NotFoundException e) {
      Assertions.assertNotNull(e);
    }
    Organization orgByHost = organizationDatabase.getOrganizationByHost("test.org", 8080);
    Assertions.assertEquals(orgById, orgByHost);

    Assertions.assertEquals("newOrg", orgById.getId());
    Assertions.assertEquals("test organization", orgById.getName());
    Assertions.assertEquals("ROLE_TEST_ADMIN", orgById.getAdminRole());
    Assertions.assertEquals("ROLE_TEST_ANONYMOUS", orgById.getAnonymousRole());
    Map<String, Integer> servers = orgById.getServers();
    Assertions.assertEquals(1, servers.size());
    Assertions.assertTrue(servers.containsKey("test.org"));
    Assertions.assertTrue(servers.containsValue(8080));
    Map<String, String> properties = orgById.getProperties();
    Assertions.assertEquals(1, properties.size());
    Assertions.assertTrue(properties.containsKey("test"));
    Assertions.assertTrue(properties.containsValue("one"));
  }

  @Test
  public void testList() throws Exception {
    Map<String, String> orgProperties = new HashMap<String, String>();
    orgProperties.put("test", "one");
    JpaOrganization org1 = new JpaOrganization("newOrg", "test organization", "test.org", 8080, "ROLE_TEST_ADMIN",
            "ROLE_TEST_ANONYMOUS", orgProperties);

    organizationDatabase.storeOrganization(org1);

    orgProperties.put("test", "one");
    orgProperties.put("test2", "two");
    JpaOrganization org2 = new JpaOrganization("newOrg2", "test organization 2", "test2.org", 8081, "ROLE_TEST2_ADMIN",
            "ROLE_TEST2_ANONYMOUS", orgProperties);

    organizationDatabase.storeOrganization(org2);

    Assertions.assertEquals(2, organizationDatabase.countOrganizations());

    List<Organization> organizations = organizationDatabase.getOrganizations();
    Assertions.assertEquals(2, organizations.size());

    Assertions.assertEquals(org1, organizations.get(0));
    Assertions.assertEquals(org2, organizations.get(1));
  }

  @Test
  public void testDeleting() throws Exception {
    Map<String, String> orgProperties = new HashMap<String, String>();
    orgProperties.put("test", "one");
    JpaOrganization org = new JpaOrganization("newOrg", "test organization", "test.org", 8080, "ROLE_TEST_ADMIN",
            "ROLE_TEST_ANONYMOUS", orgProperties);

    organizationDatabase.storeOrganization(org);

    Assertions.assertTrue(organizationDatabase.containsOrganization("newOrg"));

    try {
      organizationDatabase.getOrganization("newOrg");
    } catch (NotFoundException e) {
      Assertions.fail("Organization not found");
    }

    organizationDatabase.deleteOrganization("newOrg");

    Assertions.assertFalse(organizationDatabase.containsOrganization("newOrg"));

    try {
      organizationDatabase.getOrganization("newOrg");
      Assertions.fail("Organization found");
    } catch (NotFoundException e) {
      Assertions.assertNotNull(e);
    }
  }

}
