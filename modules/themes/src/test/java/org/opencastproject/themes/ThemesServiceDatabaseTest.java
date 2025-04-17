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

package org.opencastproject.themes;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

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
import org.opencastproject.util.requests.SortCriterion;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 */
public class ThemesServiceDatabaseTest {

  private ThemesServiceDatabaseImpl themesDatabase;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
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

    themesDatabase = new ThemesServiceDatabaseImpl();
    themesDatabase.setEntityManagerFactory(newEntityManagerFactory(ThemesServiceDatabaseImpl.PERSISTENCE_UNIT));
    themesDatabase.setDBSessionFactory(getDbSessionFactory());
    themesDatabase.setSecurityService(securityService);
    themesDatabase.setUserDirectoryService(userDirectoryService);
    themesDatabase.activate(null);
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

  @Test
  public void testFindQuery() throws Exception {
    JaxbOrganization org = new DefaultOrganization();
    JaxbUser user1 = new JaxbUser("admin", "test", org);
    JaxbUser user2 = new JaxbUser("bdmin", "test", org);
    JaxbUser user3 = new JaxbUser("cdmin", "test", org);

    Theme theme1 = new Theme(Option.<Long> none(), new Date(1), true, user1, "A theme");
    Theme theme2 = new Theme(Option.<Long> none(), new Date(2), true, user2, "B theme");
    Theme theme3 = new Theme(Option.<Long> none(), new Date(2), true, user3, "C theme");

    themesDatabase.updateTheme(theme1);
    themesDatabase.updateTheme(theme2);
    themesDatabase.updateTheme(theme3);

    List<Theme> themes;

    // Empty query
    themes = themesDatabase.findThemesQuery(
        Optional.empty(),
        Optional.empty(),
        new ArrayList<>(),
        Optional.empty(),
        Optional.empty()
    );
    Assert.assertEquals(3, themes.size());

    // With limit
    themes = themesDatabase.findThemesQuery(
        Optional.ofNullable(1),
        Optional.empty(),
        new ArrayList<>(),
        Optional.empty(),
        Optional.empty()
    );
    Assert.assertEquals(1, themes.size());

    themes = themesDatabase.findThemesQuery(
        Optional.ofNullable(Integer.MAX_VALUE),
        Optional.empty(),
        new ArrayList<>(),
        Optional.empty(),
        Optional.empty()
    );
    Assert.assertEquals(3, themes.size());

    // With offset
    themes = themesDatabase.findThemesQuery(
        Optional.ofNullable(Integer.MAX_VALUE),
        Optional.ofNullable(1),
        new ArrayList<>(),
        Optional.empty(),
        Optional.empty()
    );
    Assert.assertEquals(2, themes.size());

    // With sort
    SortCriterion sortCriterion1 = new SortCriterion("creation_date", SortCriterion.Order.Descending);
    SortCriterion sortCriterion2 = new SortCriterion("name", SortCriterion.Order.Ascending);
    ArrayList<SortCriterion> sortCriteria = new ArrayList<>();
    sortCriteria.add(sortCriterion1);
    sortCriteria.add(sortCriterion2);
    themes = themesDatabase.findThemesQuery(
        Optional.ofNullable(Integer.MAX_VALUE),
        Optional.ofNullable(0),
        sortCriteria,
        Optional.empty(),
        Optional.empty()
    );
    Assert.assertEquals("B theme", themes.get(0).getName());
    Assert.assertEquals("C theme", themes.get(1).getName());
    Assert.assertEquals("A theme", themes.get(2).getName());

    // With filter
    themes = themesDatabase.findThemesQuery(
        Optional.ofNullable(Integer.MAX_VALUE),
        Optional.ofNullable(0),
        new ArrayList<>(),
        Optional.ofNullable("admin"),
        Optional.empty()
    );
    Assert.assertEquals(1, themes.size());

    themes = themesDatabase.findThemesQuery(
        Optional.ofNullable(Integer.MAX_VALUE),
        Optional.ofNullable(0),
        new ArrayList<>(),
        Optional.empty(),
        Optional.ofNullable("admin")
    );
    Assert.assertEquals(1, themes.size());
  }
}
