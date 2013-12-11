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
package org.opencastproject.kernel.security.persistence;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests organization persistence: storing, merging, retrieving and removing.
 */
public class OrganizationPersistenceTest {

  private ComboPooledDataSource pooledDataSource;
  private OrganizationDatabaseImpl organizationDatabase;
  private String storage;

  private SecurityService securityService;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");

    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + currentTime);
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new User("admin", DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            new String[] { SecurityConstants.GLOBAL_ADMIN_ROLE });
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    organizationDatabase = new OrganizationDatabaseImpl();
    organizationDatabase.setPersistenceProvider(new PersistenceProvider());
    organizationDatabase.setPersistenceProperties(props);
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

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    organizationDatabase.deactivate(null);
    DataSources.destroy(pooledDataSource);
    FileUtils.deleteQuietly(new File(storage));
  }

}
