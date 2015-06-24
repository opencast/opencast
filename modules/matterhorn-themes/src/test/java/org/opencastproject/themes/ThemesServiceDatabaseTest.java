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

package org.opencastproject.themes;

import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.themes.persistence.ThemesServiceDatabaseImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 */
public class ThemesServiceDatabaseTest {

  private ComboPooledDataSource pooledDataSource;
  private ThemesServiceDatabaseImpl themesDatabase;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();

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

    // Mock up a security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyString())).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    messageSender.sendObjectMessage(EasyMock.anyObject(String.class),
            EasyMock.anyObject(MessageSender.DestinationType.class), EasyMock.anyObject(Serializable.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(messageSender);

    themesDatabase = new ThemesServiceDatabaseImpl();
    themesDatabase.setPersistenceProvider(new PersistenceProvider());
    themesDatabase.setPersistenceProperties(props);
    themesDatabase.setSecurityService(securityService);
    themesDatabase.setUserDirectoryService(userDirectoryService);
    themesDatabase.setMessageSender(messageSender);
    themesDatabase.activate(null);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    themesDatabase.deactivate(null);
    DataSources.destroy(pooledDataSource);
  }

  @Test
  public void testStoreUpdateAndDelete() throws Exception {
    JaxbOrganization org = new DefaultOrganization();
    JaxbUser creator = new JaxbUser("admin", "test", org);

    Theme theme = new Theme(Option.<Long> none(), new Date(), true, creator, "New");
    Theme updateTheme = themesDatabase.updateTheme(theme);
    Assert.assertEquals("New", updateTheme.getName());
    Assert.assertEquals(1, themesDatabase.countThemes());
    theme = new Theme(updateTheme.getId(), new Date(), true, creator, "Updated");

    updateTheme = themesDatabase.updateTheme(theme);
    Assert.assertEquals("Updated", updateTheme.getName());
    Assert.assertEquals(1, themesDatabase.countThemes());

    try {
      theme = themesDatabase.getTheme(updateTheme.getId().get());
      Assert.assertNotNull(theme);
    } catch (NotFoundException e) {
      Assert.fail("Existing theme has not been found");
    }

    themesDatabase.deleteTheme(updateTheme.getId().get());
    Assert.assertEquals(0, themesDatabase.countThemes());

    try {
      themesDatabase.getTheme(updateTheme.getId().get());
      Assert.fail("Deleted theme has been found");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

}
