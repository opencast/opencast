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

package org.opencastproject.kernel.security;

import org.opencastproject.kernel.security.persistence.OrganizationDatabase;
import org.opencastproject.kernel.security.persistence.OrganizationDatabaseException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.NotFoundException;

import junit.framework.Assert;

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
          Assert.assertNotNull(organization);
          Assert.assertEquals("mh_default", organization.getId());
          Assert.assertEquals("Opencast Test", organization.getName());
          Assert.assertEquals("ROLE_TEST_ADMIN", organization.getAdminRole());
          Assert.assertEquals("ROLE_TEST_ANONYMOUS", organization.getAnonymousRole());
          Map<String, Integer> servers = organization.getServers();
          Assert.assertEquals(1, servers.size());
          Assert.assertTrue(servers.containsKey("localhost"));
          Assert.assertTrue(servers.containsValue(8080));
          Assert.assertEquals("true", organization.getProperties().get("org.test"));
        } else if (i == 1) {
          Assert.assertNotNull(organization);
          Assert.assertEquals("mh_default", organization.getId());
          Assert.assertEquals("Opencast Test 2", organization.getName());
          Assert.assertEquals("ROLE_TEST2_ADMIN", organization.getAdminRole());
          Assert.assertEquals("ROLE_TEST2_ANONYMOUS", organization.getAnonymousRole());
          Map<String, Integer> servers = organization.getServers();
          Assert.assertEquals(2, servers.size());
          Assert.assertTrue(servers.containsKey("localhost"));
          Assert.assertTrue(servers.containsValue(8080));
          Assert.assertTrue(servers.containsKey("localhost2"));
          Assert.assertTrue(servers.containsValue(8081));
          Assert.assertEquals("false", organization.getProperties().get("org.test"));
        } else {
          Assert.fail("Too much storeOrganization calls: " + i);
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
      Assert.fail("No configuration exception occured");
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
      Assert.fail("Configuration exception occured");
    }

    // Update properties
    properties.put(OrganizationDirectoryServiceImpl.ORG_ID_KEY, "mh_default");
    properties.put(OrganizationDirectoryServiceImpl.ORG_NAME_KEY, "Opencast Test 2");
    properties.put(OrganizationDirectoryServiceImpl.ORG_SERVER_KEY, "localhost2");
    properties.put(OrganizationDirectoryServiceImpl.ORG_PORT_KEY, "8081");
    properties.put(OrganizationDirectoryServiceImpl.ORG_ADMIN_ROLE_KEY, "ROLE_TEST2_ADMIN");
    properties.put(OrganizationDirectoryServiceImpl.ORG_ANONYMOUS_ROLE_KEY, "ROLE_TEST2_ANONYMOUS");
    properties.put("prop.org.test", "false");

    // Test adding second server
    try {
      orgDirectoryService.updated("mh_default", properties);
    } catch (ConfigurationException e) {
      Assert.fail("Configuration exception occured");
    }
  }
}
