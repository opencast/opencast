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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;
import static org.opencastproject.index.service.util.CatalogAdapterUtil.getCatalogProperties;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.index.service.catalog.adapter.series.CommonSeriesCatalogUIAdapter;
import org.opencastproject.index.service.impl.IndexServiceImpl;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.common.provider.UsersListProvider;
import org.opencastproject.list.impl.ListProvidersServiceImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.opensearch.api.SearchIndexException;
import org.opencastproject.opensearch.api.SearchResult;
import org.opencastproject.opensearch.index.OpenSearchIndex;
import org.opencastproject.opensearch.index.objects.event.Event;
import org.opencastproject.opensearch.index.objects.event.EventIndexSchema;
import org.opencastproject.opensearch.index.objects.event.EventSearchQuery;
import org.opencastproject.opensearch.index.objects.series.Series;
import org.opencastproject.opensearch.index.objects.series.SeriesIndexSchema;
import org.opencastproject.opensearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.persistence.ThemesServiceDatabaseImpl;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;
import org.opencastproject.util.requests.SortCriterion.Order;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestSeriesEndpoint extends SeriesEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(TestSeriesEndpoint.class);

  private SeriesService seriesService;
  private OpenSearchIndex opensearchIndex;
  private ListProvidersService listProvidersService;
  private ThemesServiceDatabaseImpl themesServiceDatabaseImpl;

  private ListProvidersService createListProviderService(List<User> users) {
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.findUsers("%", 0, 0)).andReturn(users.iterator()).anyTimes();
    EasyMock.replay(userDirectoryService);

    UsersListProvider userListProvider = new UsersListProvider();
    userListProvider.setUserDirectoryService(userDirectoryService);

    ListProvidersServiceImpl listProvidersServiceImpl = new ListProvidersServiceImpl();
    listProvidersServiceImpl.addProvider(userListProvider);
    listProvidersServiceImpl.addProvider(new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return new String[] { "LANGUAGES" };
      }

      @Override
      public Map<String, String> getList(String listName, ResourceListQuery query) {
        return new HashMap<>();
      }

      @Override
      public boolean isTranslatable(String listName) {
        return false;
      }

      @Override
      public String getDefault() {
        return null;
      }
    });
    return listProvidersServiceImpl;
  }

  public TestSeriesEndpoint() throws Exception {
    User user1 = new JaxbUser("test@email.ch", null, "test", "test1@email.ch", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());
    User user2 = new JaxbUser("test2@email.ch", null, "test2", "test2@email.ch", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());
    User user3 = new JaxbUser("test3@email.ch", null, "test3", "test3@email.ch", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());

    List<User> users = new ArrayList<>();
    users.add(user1);
    users.add(user2);
    users.add(user3);
    this.listProvidersService = createListProviderService(users);

    this.seriesService = EasyMock.createNiceMock(SeriesService.class);

    DublinCoreCatalog catalog1 = DublinCoreXmlFormat.read(getClass().getResourceAsStream("/dublincore.xml"));
    DublinCoreCatalog catalog2 = DublinCoreXmlFormat.read(getClass().getResourceAsStream("/dublincore2.xml"));
    DublinCoreCatalog catalog3 = DublinCoreXmlFormat.read(getClass().getResourceAsStream("/dublincore3.xml"));
    List<DublinCoreCatalog> catalogs = new ArrayList<>();
    catalogs.add(catalog1);
    catalogs.add(catalog2);
    catalogs.add(catalog3);

    Map<String, String> optionsMap = new HashMap<>();
    optionsMap.put("annotations", "true");
    DublinCoreCatalog dc = DublinCores.mkOpencastEpisode().getCatalog();
    Date date = new Date(DateTimeSupport.fromUTC("2014-06-05T09:15:56Z"));
    dc.set(DublinCore.PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(date, Precision.Second));
    dc.set(DublinCore.PROPERTY_IDENTIFIER, "23");

    JaxbOrganization defaultOrganization = new DefaultOrganization();

    User userWithoutPermissions = new JaxbUser("sample", null, "WithoutPermissions", "without@permissions.com", "test",
            defaultOrganization, new HashSet<>(Arrays.asList(new JaxbRole("ROLE_NOTHING", defaultOrganization))));

    // security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(userWithoutPermissions).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(defaultOrganization).anyTimes();
    EasyMock.replay(securityService);

    String anonymousRole = securityService.getOrganization().getAnonymousRole();
    final AccessControlList acl = new AccessControlList(
            new AccessControlEntry(anonymousRole, Permissions.Action.READ.toString(), true));

    EasyMock.expect(seriesService.updateSeries(EasyMock.anyObject(DublinCoreCatalog.class))).andReturn(dc).anyTimes();
    EasyMock.expect(seriesService.getSeriesCount()).andReturn(3).anyTimes();
    EasyMock.expect(seriesService.getSeriesAccessControl(anyString())).andReturn(acl).anyTimes();
    seriesService.deleteSeries(Long.toString(1L));
    EasyMock.expectLastCall();
    seriesService.deleteSeries(Long.toString(2L));
    EasyMock.expectLastCall();
    seriesService.deleteSeries(Long.toString(3L));
    EasyMock.expectLastCall();
    seriesService.deleteSeries(Long.toString(4L));
    EasyMock.expectLastCall().andThrow(new NotFoundException());
    seriesService.deleteSeries(Long.toString(5L));
    EasyMock.expectLastCall().andThrow(new SeriesException());
    seriesService.updateSeriesProperty("11", SeriesEndpoint.THEME_KEY, "1");
    EasyMock.expectLastCall().andThrow(new NotFoundException());
    EasyMock.replay(seriesService);

    List<ManagedAcl> managedAcls = new ArrayList<>();
    ManagedAcl managedAcl1 = new ManagedAclImpl(43L, "Public", defaultOrganization.getId(), acl);
    managedAcls.add(managedAcl1);
    managedAcls.add(new ManagedAclImpl(44L, "Private", defaultOrganization.getId(), acl));

    AclService aclService = EasyMock.createNiceMock(AclService.class);
    EasyMock.expect(aclService.getAcls()).andReturn(managedAcls).anyTimes();
    EasyMock.expect(aclService.getAcl(EasyMock.anyLong())).andReturn(Optional.of(managedAcl1)).anyTimes();
    EasyMock.replay(aclService);

    AclServiceFactory aclServiceFactory = EasyMock.createNiceMock(AclServiceFactory.class);
    EasyMock.expect(aclServiceFactory.serviceFor(defaultOrganization)).andReturn(aclService).anyTimes();
    EasyMock.replay(aclServiceFactory);

    setupIndex(defaultOrganization, userWithoutPermissions);

    CommonSeriesCatalogUIAdapter dublinCoreAdapter = new CommonSeriesCatalogUIAdapter();
    Properties seriesCatalogProperties = getCatalogProperties(getClass(), "/series-catalog.properties");

    dublinCoreAdapter.updated(PropertiesUtil.toDictionary(seriesCatalogProperties));

    dublinCoreAdapter.setSeriesService(seriesService);
    dublinCoreAdapter.setListProvidersService(listProvidersService);
    dublinCoreAdapter.setSecurityService(securityService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.addCatalogUIAdapter(dublinCoreAdapter);
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.setSeriesService(seriesService);

    // setup theme database
    User user = new JaxbUser("test", null, "Test User", "test@test.com", "test", new DefaultOrganization(),
        new HashSet<>());
    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    themesServiceDatabaseImpl = new ThemesServiceDatabaseImpl();
    themesServiceDatabaseImpl
        .setEntityManagerFactory(newEntityManagerFactory(ThemesServiceDatabaseImpl.PERSISTENCE_UNIT));
    themesServiceDatabaseImpl.setDBSessionFactory(getDbSessionFactory());
    themesServiceDatabaseImpl.setUserDirectoryService(userDirectoryService);
    themesServiceDatabaseImpl.setSecurityService(securityService);
    themesServiceDatabaseImpl.activate(null);

    // We only really care about id, name and description here
    Theme theme = new Theme(Optional.ofNullable(1L), new Date(1421064000000L), true, user, "theme-1-name",
        "theme-1-description", true, "bumper-file", true, "trailer-file", true, "title,room,date",
        "title-background-file", true, "license-background-file", "The license description", true, "watermark-file",
        "top-left");
    themesServiceDatabaseImpl.updateTheme(theme);

    this.setIndex(opensearchIndex);
    this.setSeriesService(seriesService);
    this.setThemesServiceDatabase(themesServiceDatabaseImpl);
    this.setSecurityService(securityService);
    this.setAclServiceFactory(aclServiceFactory);
    this.setIndexService(indexServiceImpl);
    this.activate(null, null);
  }

  private Series createSeries(String id, String title, String contributor, String organizer, long time, Long themeId) {
    Series series = new Series(id, new DefaultOrganization().getId());
    series.setCreatedDateTime(new Date(time));
    series.addContributor(contributor);
    series.addOrganizer(organizer);
    series.setTitle(title);
    if (themeId != null) {
      series.setTheme(themeId);
    }
    return series;
  }

  private List<Event> createEvents(int readyCount) {
    List<Event> events = new ArrayList<>();
    String orgId = new DefaultOrganization().getId();
    for (int i = 0; i < readyCount; i++) {
      events.add(new Event(Integer.toString(i + 1), orgId));
    }
    return events;
  }

  @SuppressWarnings({ "unchecked" })
  private void setupIndex(Organization defaultOrganization, User user)
          throws SearchIndexException, IOException, IllegalStateException, ParseException {
    long time = DateTimeSupport.fromUTC("2014-04-27T14:35:50Z");
    Series series1 = createSeries("1", "title 1", "contributor 1", "organizer 1", time, 1L);

    time = DateTimeSupport.fromUTC("2014-04-28T14:35:50Z");
    Series series2 = createSeries("2", "title 2", "contributor 2", "organizer 2", time, null);

    time = DateTimeSupport.fromUTC("2014-04-29T14:35:50Z");
    Series series3 = createSeries("3", "title 3", "contributor 3", "organizer 3", time, null);

    // Setup the events for series 1, 2 and 3
    final List<Event> eventItems1 = createEvents(1);
    final List<Event> eventItems2 = List.of();
    final List<Event> eventItems3 = createEvents(0);

    // Setup series search results
    final SearchResult<Series> ascSeriesSearchResult = new SearchResult<>(List.of(series1, series2, series3), 3, 0, 0);
    final SearchResult<Series> descSeriesSearchResult = new SearchResult<>(List.of(series3, series2, series1), 3, 0, 0);
    final SearchResult<Series> emptySearchResult = SearchResult.empty();
    final SearchResult<Series> oneSearchResult = new SearchResult<>(List.of(series1), 1, 0, 0);
    final SearchResult<Series> twoSearchResult = new SearchResult<>(List.of(series2), 1, 0, 0);

    opensearchIndex = EasyMock.createMock(OpenSearchIndex.class);

    EasyMock.expect(opensearchIndex.getSeries("1", defaultOrganization, user))
        .andReturn(Optional.of(series1));
    EasyMock.expect(opensearchIndex.getSeries("2", defaultOrganization, user))
        .andReturn(Optional.of(series2));
    EasyMock.expect(opensearchIndex.getSeries("3", defaultOrganization, user))
        .andReturn(Optional.of(series3));
    EasyMock.expect(opensearchIndex.getSeries(anyString(), anyObject(Organization.class), anyObject()))
        .andReturn(Optional.empty());

    final Capture<SeriesSearchQuery> captureSeriesSearchQuery = EasyMock.newCapture();
    final Capture<EventSearchQuery> captureEventSearchQuery = EasyMock.newCapture();

    EasyMock.expect(opensearchIndex.getByQuery(EasyMock.capture(captureSeriesSearchQuery)))
            .andAnswer(new IAnswer<SearchResult<Series>>() {

              @Override
              public SearchResult<Series> answer() throws Throwable {
                final SeriesSearchQuery q = captureSeriesSearchQuery.getValue();
                if (captureSeriesSearchQuery.hasCaptured()
                        && q.getFilterValues(SeriesIndexSchema.UID).size() == 1) {
                  if ("1".equals(q.getFilterValues(SeriesIndexSchema.UID).iterator().next())) {
                    return oneSearchResult;
                  } else if ("2".equals(q.getFilterValues(SeriesIndexSchema.UID).iterator().next())) {
                    return twoSearchResult;
                  } else {
                    return emptySearchResult;
                  }
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.CONTRIBUTORS) == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.CONTRIBUTORS) == Order.Descending) {
                  return descSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.CREATED_DATE_TIME) == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.CREATED_DATE_TIME) == Order.Descending) {
                  return descSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.ORGANIZERS) == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.ORGANIZERS) == Order.Descending) {
                  return descSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.TITLE) == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && q.getSortOrder(SeriesIndexSchema.TITLE) == Order.Descending) {

                  return descSeriesSearchResult;
                } else {
                  return ascSeriesSearchResult;
                }
              }

            });

    EasyMock.expect(opensearchIndex.getByQuery(EasyMock.capture(captureEventSearchQuery)))
            .andAnswer(new IAnswer<SearchResult<Event>>() {

              @Override
              public SearchResult<Event> answer() throws Throwable {
                final EventSearchQuery q = captureEventSearchQuery.getValue();
                List<Event> events = List.of();
                if (captureEventSearchQuery.hasCaptured()
                        && 1 == (q.getFilterValues(EventIndexSchema.SERIES_ID).size())
                        && "1".equals(q.getFilterValues(EventIndexSchema.SERIES_ID).iterator().next())
                        && !(q.getFilterValues(EventIndexSchema.WORKFLOW_STATE).contains("RUNNING"))
                        && !(q.getFilterValues(EventIndexSchema.WORKFLOW_STATE).contains("INSTANTIATED"))) {
                  // Setup events search results
                  events = eventItems1;
                } else if (captureEventSearchQuery.hasCaptured()
                        && 1 == (q.getFilterValues(EventIndexSchema.SERIES_ID).size())
                        && "1".equals(q.getFilterValues(EventIndexSchema.SERIES_ID).iterator().next())
                        && q.getFilterValues(EventIndexSchema.WORKFLOW_STATE).contains("INSTANTIATED")) {
                  // Setup events search results
                  events = eventItems2;
                } else if (captureEventSearchQuery.hasCaptured()
                        && 1 == (q.getFilterValues(EventIndexSchema.SERIES_ID).size())
                        && "1".equals(q.getFilterValues(EventIndexSchema.SERIES_ID).iterator().next())
                        && q.getFilterValues(EventIndexSchema.WORKFLOW_STATE).contains("RUNNING")) {
                  // Setup events search results
                  events = eventItems2;
                } else if (captureEventSearchQuery.hasCaptured()
                        && 1 == (q.getFilterValues(EventIndexSchema.SERIES_ID).size())
                        && "2".equals(q.getFilterValues(EventIndexSchema.SERIES_ID).iterator().next())
                        && !(q.getFilterValues(EventIndexSchema.WORKFLOW_STATE).contains("RUNNING"))) {
                  // Setup events search results
                  events = eventItems2;
                } else if (captureEventSearchQuery.hasCaptured()
                    && 1 == (q.getFilterValues(EventIndexSchema.SERIES_ID).size())
                        && "3".equals(q.getFilterValues(EventIndexSchema.SERIES_ID).iterator().next())) {
                  // Setup events search results
                  events = eventItems3;
                } else if (captureEventSearchQuery.hasCaptured()
                    && 1 == (q.getFilterValues(EventIndexSchema.SERIES_ID).size())
                        && "2".equals(q.getFilterValues(EventIndexSchema.SERIES_ID).iterator().next())
                        && q.getFilterValues(EventIndexSchema.WORKFLOW_STATE).contains("RUNNING")) {
                  // Setup events search results
                  events = eventItems3;
                } else {
                  if (!captureEventSearchQuery.hasCaptured()) {
                    Assert.fail("Haven't captured an event search query yet.");
                  } else {
                    logger.info("IDs for search query" + q.getFilterValues(EventIndexSchema.SERIES_ID));
                    Assert.fail("Tried to get an event collection that doesn't exist.");
                  }
                }
                return new SearchResult<>(events, events.size(), 0, 0);
              }
            }).anyTimes();

    EasyMock.replay(opensearchIndex);
  }

}
