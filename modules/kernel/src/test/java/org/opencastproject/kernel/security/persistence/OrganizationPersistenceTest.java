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

package org.opencastproject.kernel.security.persistence;

import static org.opencastproject.kernel.security.persistence.OrganizationDatabaseImpl.PERSISTENCE_UNIT;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
  @Before
  public void setUp() throws Exception {
    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    organizationDatabase = new OrganizationDatabaseImpl();
    organizationDatabase.setEntityManagerFactory(newTestEntityManagerFactory(PERSISTENCE_UNIT));
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

    Assert.assertTrue(organizationDatabase.containsOrganization("newOrg"));

    Organization orgById = organizationDatabase.getOrganization("newOrg");
    try {
      organizationDatabase.getOrganizationByHost("test.org", 8081);
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
    Organization orgByHost = organizationDatabase.getOrganizationByHost("test.org", 8080);
    Assert.assertEquals(orgById, orgByHost);

    Assert.assertEquals("newOrg", orgById.getId());
    Assert.assertEquals("test organization", orgById.getName());
    Assert.assertEquals("ROLE_TEST_ADMIN", orgById.getAdminRole());
    Assert.assertEquals("ROLE_TEST_ANONYMOUS", orgById.getAnonymousRole());
    Map<String, Integer> servers = orgById.getServers();
    Assert.assertEquals(1, servers.size());
    Assert.assertTrue(servers.containsKey("test.org"));
    Assert.assertTrue(servers.containsValue(8080));
    Map<String, String> properties = orgById.getProperties();
    Assert.assertEquals(1, properties.size());
    Assert.assertTrue(properties.containsKey("test"));
    Assert.assertTrue(properties.containsValue("one"));
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

    Assert.assertEquals(2, organizationDatabase.countOrganizations());

    List<Organization> organizations = organizationDatabase.getOrganizations();
    Assert.assertEquals(2, organizations.size());

    Assert.assertEquals(org1, organizations.get(0));
    Assert.assertEquals(org2, organizations.get(1));
  }

  @Test
  public void testDeleting() throws Exception {
    Map<String, String> orgProperties = new HashMap<String, String>();
    orgProperties.put("test", "one");
    JpaOrganization org = new JpaOrganization("newOrg", "test organization", "test.org", 8080, "ROLE_TEST_ADMIN",
            "ROLE_TEST_ANONYMOUS", orgProperties);

    organizationDatabase.storeOrganization(org);

    Assert.assertTrue(organizationDatabase.containsOrganization("newOrg"));

    try {
      organizationDatabase.getOrganization("newOrg");
    } catch (NotFoundException e) {
      Assert.fail("Organization not found");
    }

    organizationDatabase.deleteOrganization("newOrg");

    Assert.assertFalse(organizationDatabase.containsOrganization("newOrg"));

    try {
      organizationDatabase.getOrganization("newOrg");
      Assert.fail("Organization found");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

}
