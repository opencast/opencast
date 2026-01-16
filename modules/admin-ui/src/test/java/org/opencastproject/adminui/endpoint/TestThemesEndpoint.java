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

package org.opencastproject.adminui.endpoint;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.impl.SearchResultImpl;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.series.Series;
import org.opencastproject.elasticsearch.index.objects.series.SeriesSearchQuery;
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

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Optional;

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
    Theme theme = new Theme(Optional.empty(), creationDate, true, user, "The Theme name", "The Theme description",
            true, "uuid1", true, "trailer-file", true, "title,room,date", "title-background-file", true,
            "license-background-file", "The license description", true, "uuid2", "top-left");
    themesServiceDatabaseImpl.updateTheme(theme);

    Theme theme2 = new Theme(Optional.empty(), creationDate, false, user, "theme-2-name", "",
        false, "uuid1", false, "", false, "", "", false,
        "", "", false, "uuid2", "");
    themesServiceDatabaseImpl.updateTheme(theme2);

    Theme theme3 = new Theme(Optional.empty(), creationDate, false, user, "theme-3-name", "",
        false, "uuid1", false, "", false, "", "", false,
        "", "", false, "uuid2", "");
    themesServiceDatabaseImpl.updateTheme(theme3);
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
    ElasticsearchIndex elasticsearchIndex = EasyMock.createNiceMock(ElasticsearchIndex.class);
    final Capture<SeriesSearchQuery> seriesQueryCapture = EasyMock.newCapture();
    EasyMock.expect(elasticsearchIndex.getByQuery(EasyMock.capture(seriesQueryCapture)))
            .andAnswer(() -> createSeriesCaptureResult(seriesQueryCapture));
    EasyMock.expect(elasticsearchIndex.getIndexName()).andReturn("adminui").anyTimes();
    EasyMock.replay(elasticsearchIndex);

    themesServiceDatabaseImpl = new ThemesServiceDatabaseImpl();
    themesServiceDatabaseImpl
            .setEntityManagerFactory(newEntityManagerFactory(ThemesServiceDatabaseImpl.PERSISTENCE_UNIT));
    themesServiceDatabaseImpl.setDBSessionFactory(getDbSessionFactory());
    themesServiceDatabaseImpl.setUserDirectoryService(userDirectoryService);
    themesServiceDatabaseImpl.setSecurityService(securityService);
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
    this.setIndex(elasticsearchIndex);
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
