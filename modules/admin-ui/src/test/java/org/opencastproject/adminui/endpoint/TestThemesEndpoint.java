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

package org.opencastproject.adminui.endpoint;

import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.impl.SearchResultImpl;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.elasticsearch.index.theme.IndexTheme;
import org.opencastproject.elasticsearch.index.theme.ThemeSearchQuery;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.staticfiles.api.StaticFileService;
import org.opencastproject.staticfiles.endpoint.StaticFileRestService;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.persistence.ThemesServiceDatabaseException;
import org.opencastproject.themes.persistence.ThemesServiceDatabaseImpl;
import org.opencastproject.util.data.Option;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestThemesEndpoint extends ThemesEndpoint {

  private User user;
  private Long theme1Id = 1L;
  private Long theme2Id = 2L;
  private Long theme3Id = 3L;
  private ThemesServiceDatabaseImpl themesServiceDatabaseImpl;
  private Date creationDate = new Date(1421064000000L);
  private Organization defaultOrg = new DefaultOrganization();

  public TestThemesEndpoint() throws Exception {
    setupServices();
    addData();
  }

  private void addData() throws ThemesServiceDatabaseException {
    Theme theme = new Theme(Option.some(theme1Id), creationDate, true, user, "The Theme name", "The Theme description",
            true, "bumper-file", true, "trailer-file", true, "title,room,date", "title-background-file", true,
            "license-background-file", "The license description", true, "watermark-file", "top-left");
    themesServiceDatabaseImpl.updateTheme(theme);
  }

  private void setupServices() throws Exception {
    user = new JaxbUser("test", null, "Test User", "test@test.com", "test", new DefaultOrganization(), new HashSet<>());

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    SeriesService seriesService = EasyMock.createNiceMock(SeriesService.class);
    EasyMock.replay(seriesService);

    // Create AdminUI Search Index
    AdminUISearchIndex adminUISearchIndex = EasyMock.createNiceMock(AdminUISearchIndex.class);
    final Capture<ThemeSearchQuery> themeQueryCapture = EasyMock.newCapture();
    EasyMock.expect(adminUISearchIndex.getByQuery(EasyMock.capture(themeQueryCapture)))
            .andAnswer(() -> createThemeCaptureResult(themeQueryCapture)).anyTimes();
    final Capture<SeriesSearchQuery> seriesQueryCapture = EasyMock.newCapture();
    EasyMock.expect(adminUISearchIndex.getByQuery(EasyMock.capture(seriesQueryCapture)))
            .andAnswer(() -> createSeriesCaptureResult(seriesQueryCapture));
    EasyMock.expect(adminUISearchIndex.getIndexName()).andReturn("adminui").anyTimes();
    EasyMock.replay(adminUISearchIndex);

    themesServiceDatabaseImpl = new ThemesServiceDatabaseImpl();
    themesServiceDatabaseImpl
            .setEntityManagerFactory(newTestEntityManagerFactory(ThemesServiceDatabaseImpl.PERSISTENCE_UNIT));
    themesServiceDatabaseImpl.setUserDirectoryService(userDirectoryService);
    themesServiceDatabaseImpl.setSecurityService(securityService);
    themesServiceDatabaseImpl.setAdminUiIndex(adminUISearchIndex);
    themesServiceDatabaseImpl.activate(null);

    StaticFileService staticFileService = EasyMock.createNiceMock(StaticFileService.class);
    EasyMock.expect(staticFileService.getFile(EasyMock.anyString()))
            .andReturn(new ByteArrayInputStream("test".getBytes("utf-8"))).anyTimes();
    EasyMock.expect(staticFileService.getFileName(EasyMock.anyString())).andStubReturn("test.mp4");
    EasyMock.replay(staticFileService);

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.server.url")).andReturn("http://localhost:8080")
            .anyTimes();
    EasyMock.replay(bundleContext);

    ComponentContext componentContext = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.expect(componentContext.getProperties()).andReturn(new Hashtable<>()).anyTimes();
    EasyMock.replay(componentContext);

    StaticFileRestService staticFileRestService = new StaticFileRestService();
    staticFileRestService.setStaticFileService(staticFileService);
    staticFileRestService.activate(componentContext);

    this.setThemesServiceDatabase(themesServiceDatabaseImpl);
    this.setSecurityService(securityService);
    this.setSeriesService(seriesService);
    this.setStaticFileService(staticFileService);
    this.setStaticFileRestService(staticFileRestService);
    this.setIndex(adminUISearchIndex);
  }

  private SearchResult<IndexTheme> createThemeCaptureResult(
          final Capture<ThemeSearchQuery> myCapture) {
    SearchResultImpl<IndexTheme> searchResults = new SearchResultImpl<IndexTheme>(
            myCapture.getValue(), 0, 0);
    if (myCapture.hasCaptured()) {
      if (myCapture.getValue().getIdentifiers().length == 1 && myCapture.getValue().getIdentifiers()[0] == theme1Id) {
        SearchResultItem<IndexTheme> searchResultItem = getThemeSearchResultItem(
                theme1Id, "theme-1-name");
        searchResults.addResultItem(searchResultItem);
      } else if (myCapture.getValue().getIdentifiers().length == 0) {
        SearchResultItem<IndexTheme> searchResultItem1 = getThemeSearchResultItem(
                theme1Id, "theme-1-name");
        searchResults.addResultItem(searchResultItem1);
        SearchResultItem<IndexTheme> searchResultItem2 = getThemeSearchResultItem(
                theme2Id, "theme-2-name");
        searchResults.addResultItem(searchResultItem2);
        SearchResultItem<IndexTheme> searchResultItem3 = getThemeSearchResultItem(
                theme3Id, "theme-3-name");
        searchResults.addResultItem(searchResultItem3);
      }
    }
    return searchResults;
  }

  @SuppressWarnings("unchecked")
  private SearchResultItem<IndexTheme> getThemeSearchResultItem(Long id,
          String name) {
    IndexTheme theme = new IndexTheme(
            id, defaultOrg.getId());
    theme.setCreationDate(creationDate);
    theme.setName(name);
    theme.setCreator("Test User");
    theme.setBumperFile("uuid1");
    theme.setWatermarkFile("uuid2");
    SearchResultItem<IndexTheme> searchResultItem = EasyMock
            .createMock(SearchResultItem.class);
    EasyMock.expect(searchResultItem.getSource()).andReturn(theme);
    EasyMock.expect(searchResultItem.compareTo(EasyMock.anyObject(SearchResultItem.class))).andReturn(1);
    EasyMock.replay(searchResultItem);
    return searchResultItem;
  }

  private SearchResult<Series> createSeriesCaptureResult(Capture<SeriesSearchQuery> myCapture) {
    SearchResultImpl<Series> searchResults = new SearchResultImpl<Series>(myCapture.getValue(), 0, 0);
    if (myCapture.hasCaptured()) {
      SearchResultItem<Series> searchResultItem1 = getSeriesSearchResultItem("Series1Id", "Series 1 Title");
      searchResults.addResultItem(searchResultItem1);
      SearchResultItem<Series> searchResultItem2 = getSeriesSearchResultItem("Series2Id", "Series 2 Title");
      searchResults.addResultItem(searchResultItem2);
      SearchResultItem<Series> searchResultItem3 = getSeriesSearchResultItem("Series3Id", "Series 3 Title");
      searchResults.addResultItem(searchResultItem3);
    }
    return searchResults;
  }

  private SearchResultItem<Series> getSeriesSearchResultItem(String seriesId, String title) {
    Series series = new Series(seriesId, defaultOrg.getId());
    series.setTitle(title);
    SearchResultItem<Series> searchResultItem = EasyMock.createNiceMock(SearchResultItem.class);
    EasyMock.expect(searchResultItem.getSource()).andReturn(series);
    EasyMock.expect(searchResultItem.compareTo(EasyMock.anyObject(SearchResultItem.class))).andReturn(1);
    EasyMock.replay(searchResultItem);
    return searchResultItem;
  }

}
