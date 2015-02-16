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
package org.opencastproject.kernel.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.kernel.security.persistence.OrganizationDatabase;
import org.opencastproject.kernel.security.persistence.OrganizationDatabaseException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Test class for {@link OrganizationDirectoryServiceImpl}
 */
public class OrganizationDirectoryServiceTest {

  private OrganizationDirectoryServiceImpl orgDirectoryService;

  @Before
  public void setUp() {
    OrganizationDatabase organizationDatabase = new OrganizationDatabase() {

      private int i = 0;
      private Organization organization;

      @Override
      public void storeOrganization(Organization organization) throws OrganizationDatabaseException {
        this.organization = organization;
        if (i == 0) {
          assertNotNull(organization);
          assertEquals("mh_default", organization.getId());
          assertEquals("Opencast Test", organization.getName());
          assertEquals("ROLE_TEST_ADMIN", organization.getAdminRole());
          assertEquals("ROLE_TEST_ANONYMOUS", organization.getAnonymousRole());
          Map<String, Integer> servers = organization.getServers();
          assertEquals(1, servers.size());
          assertTrue(servers.containsKey("localhost"));
          assertTrue(servers.containsValue(8080));
          assertEquals("true", organization.getProperties().get("org.test"));
        } else if (i == 1) {
          assertNotNull(organization);
          assertEquals("mh_default", organization.getId());
          assertEquals("Opencast Test 2", organization.getName());
          assertEquals("ROLE_TEST2_ADMIN", organization.getAdminRole());
          assertEquals("ROLE_TEST2_ANONYMOUS", organization.getAnonymousRole());
          Map<String, Integer> servers = organization.getServers();
          assertEquals(3, servers.size());
          assertTrue(servers.containsKey("localhost"));
          assertTrue(servers.containsValue(8080));
          assertTrue(servers.containsKey("localhost2"));
          assertTrue(servers.containsValue(8081));
          assertTrue(servers.containsKey("another"));
          assertEquals("false", organization.getProperties().get("org.test"));
        } else {
          fail("Too many storeOrganization calls: " + i);
        }
        i++;
      }

      @Override
      public List<Organization> getOrganizations() throws OrganizationDatabaseException {
        return null;
      }

      @Override
      public Organization getOrganizationByHost(String host, int port) throws OrganizationDatabaseException,
              NotFoundException {
        return null;
      }

      @Override
      public Organization getOrganization(String orgId) throws OrganizationDatabaseException, NotFoundException {
        if (organization == null)
          throw new NotFoundException();
        return organization;
      }

      @Override
      public void deleteOrganization(String orgId) throws OrganizationDatabaseException, NotFoundException {

      }

      @Override
      public int countOrganizations() throws OrganizationDatabaseException {
        return 0;
      }

      @Override
      public boolean containsOrganization(String orgId) throws OrganizationDatabaseException {
        return false;
      }
    };

    orgDirectoryService = new OrganizationDirectoryServiceImpl();
    orgDirectoryService.setOrgPersistence(organizationDatabase);
  }

  @Test
  public void testUpdateMethod() {
    Hashtable<String, String> properties = new Hashtable<String, String>();

    // Test wrong configuration
    try {
      orgDirectoryService.updated("mh_default", properties);
      fail("No configuration exception occured");
    } catch (ConfigurationException e) {
      // Should be an exception
    }

    // Add properties
    properties.put(OrganizationDirectoryServiceImpl.ORG_ID_KEY, "mh_default");
    properties.put(OrganizationDirectoryServiceImpl.ORG_NAME_KEY, "Opencast Test");
    properties.put(OrganizationDirectoryServiceImpl.ORG_SERVER_KEY, "localhost");
    properties.put(OrganizationDirectoryServiceImpl.ORG_PORT_KEY, "8080");
    properties.put(OrganizationDirectoryServiceImpl.ORG_ADMIN_ROLE_KEY, "ROLE_TEST_ADMIN");
    properties.put(OrganizationDirectoryServiceImpl.ORG_ANONYMOUS_ROLE_KEY, "ROLE_TEST_ANONYMOUS");
    properties.put("prop.org.test", "true");

    // Test storing
    try {
      orgDirectoryService.updated("mh_default", properties);
    } catch (ConfigurationException e) {
      fail("Configuration exception occured");
    }

    // Update properties
    properties.put(OrganizationDirectoryServiceImpl.ORG_ID_KEY, "mh_default");
    properties.put(OrganizationDirectoryServiceImpl.ORG_NAME_KEY, "Opencast Test 2");
    properties.put(OrganizationDirectoryServiceImpl.ORG_SERVER_KEY, "localhost2,another");
    properties.put(OrganizationDirectoryServiceImpl.ORG_PORT_KEY, "8081");
    properties.put(OrganizationDirectoryServiceImpl.ORG_ADMIN_ROLE_KEY, "ROLE_TEST2_ADMIN");
    properties.put(OrganizationDirectoryServiceImpl.ORG_ANONYMOUS_ROLE_KEY, "ROLE_TEST2_ANONYMOUS");
    properties.put("prop.org.test", "false");

    // Test adding second server
    try {
      orgDirectoryService.updated("mh_default", properties);
    } catch (ConfigurationException e) {
      fail("Configuration exception occured");
    }
  }
}
