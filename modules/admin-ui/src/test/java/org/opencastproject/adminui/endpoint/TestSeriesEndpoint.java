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

import static org.opencastproject.index.service.util.CatalogAdapterUtil.getCatalogProperties;

import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchQuery.Order;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.elasticsearch.index.theme.Theme;
import org.opencastproject.elasticsearch.index.theme.ThemeSearchQuery;
import org.opencastproject.index.service.catalog.adapter.series.CommonSeriesCatalogUIAdapter;
import org.opencastproject.index.service.impl.IndexServiceImpl;
import org.opencastproject.index.service.resources.list.provider.UsersListProvider;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.impl.ListProvidersServiceImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;
import org.opencastproject.util.data.Option;

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
import java.util.Properties;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestSeriesEndpoint extends SeriesEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(TestSeriesEndpoint.class);

  private SeriesService seriesService;
  private AdminUISearchIndex adminuiSearchIndex;
  private ListProvidersService listProvidersService;

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

    EasyMock.expect(seriesService.getSeries(EasyMock.anyObject(SeriesQuery.class)))
            .andReturn(new DublinCoreCatalogList(catalogs, catalogs.size())).anyTimes();
    EasyMock.expect(seriesService.updateSeries(EasyMock.anyObject(DublinCoreCatalog.class))).andReturn(dc).anyTimes();
    EasyMock.expect(seriesService.getSeriesCount()).andReturn(3).anyTimes();
    EasyMock.expect(seriesService.getSeriesAccessControl(EasyMock.anyString())).andReturn(acl).anyTimes();
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
    EasyMock.expect(aclService.getAcl(EasyMock.anyLong())).andReturn(Option.some(managedAcl1)).anyTimes();
    EasyMock.expect(aclService.applyAclToSeries(EasyMock.anyString(), EasyMock.anyObject(AccessControlList.class),
            EasyMock.anyBoolean())).andReturn(true).anyTimes();
    EasyMock.replay(aclService);

    AclServiceFactory aclServiceFactory = EasyMock.createNiceMock(AclServiceFactory.class);
    EasyMock.expect(aclServiceFactory.serviceFor(defaultOrganization)).andReturn(aclService).anyTimes();
    EasyMock.replay(aclServiceFactory);

    setupIndex();

    CommonSeriesCatalogUIAdapter dublinCoreAdapter = new CommonSeriesCatalogUIAdapter();
    Properties seriesCatalogProperties = getCatalogProperties(getClass(), "/series-catalog.properties");

    dublinCoreAdapter.updated(PropertiesUtil.toDictionary(seriesCatalogProperties));

    dublinCoreAdapter.setSeriesService(seriesService);
    dublinCoreAdapter.setListProvidersService(listProvidersService);
    dublinCoreAdapter.setSecurityService(securityService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.addCatalogUIAdapter(dublinCoreAdapter);
    indexServiceImpl.setCommonSeriesCatalogUIAdapter(dublinCoreAdapter);
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.setSeriesService(seriesService);

    this.setIndex(adminuiSearchIndex);
    this.setSeriesService(seriesService);
    this.setSecurityService(securityService);
    this.setAclServiceFactory(aclServiceFactory);
    this.setIndexService(indexServiceImpl);
    this.activate(null);
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

  @SuppressWarnings("unchecked")
  private SearchResultItem<Event>[] createEvents(int readyCount) {
    SearchResultItem<Event>[] eventitems = new SearchResultItem[readyCount];
    int total = 1;
    String orgId = new DefaultOrganization().getId();
    for (int i = 0; i < readyCount; i++) {
      Event readyEvent = new Event(Integer.toString(i + total), orgId);
      SearchResultItem<Event> eventItem = EasyMock.createMock(SearchResultItem.class);
      EasyMock.expect(eventItem.getSource()).andReturn(readyEvent);
      EasyMock.replay(eventItem);
      eventitems[total - 1] = eventItem;
      total++;
    }

    return eventitems;
  }

  @SuppressWarnings({ "unchecked" })
  private void setupIndex() throws SearchIndexException, IOException, IllegalStateException, ParseException {
    long time = DateTimeSupport.fromUTC("2014-04-27T14:35:50Z");
    Series series1 = createSeries("1", "title 1", "contributor 1", "organizer 1", time, 1L);

    time = DateTimeSupport.fromUTC("2014-04-28T14:35:50Z");
    Series series2 = createSeries("2", "title 2", "contributor 2", "organizer 2", time, null);

    time = DateTimeSupport.fromUTC("2014-04-29T14:35:50Z");
    Series series3 = createSeries("3", "title 3", "contributor 3", "organizer 3", time, null);

    Theme theme1 = new Theme(
            1L, new DefaultOrganization().getId());
    theme1.setName("theme-1-name");
    theme1.setDescription("theme-1-description");

    SearchResultItem<Series> item1 = EasyMock.createMock(SearchResultItem.class);
    EasyMock.expect(item1.getSource()).andReturn(series1).anyTimes();

    SearchResultItem<Series> item2 = EasyMock.createMock(SearchResultItem.class);
    EasyMock.expect(item2.getSource()).andReturn(series2).anyTimes();

    SearchResultItem<Series> item3 = EasyMock.createMock(SearchResultItem.class);
    EasyMock.expect(item3.getSource()).andReturn(series3).anyTimes();

    SearchResultItem<Series>[] ascSeriesItems = new SearchResultItem[3];
    ascSeriesItems[0] = item1;
    ascSeriesItems[1] = item2;
    ascSeriesItems[2] = item3;

    SearchResultItem<Series>[] descSeriesItems = new SearchResultItem[3];
    descSeriesItems[0] = item3;
    descSeriesItems[1] = item2;
    descSeriesItems[2] = item1;

    // final SearchResultItem<Event>[] eventItems1 = new SearchResultItem[0];
    final SearchResultItem<Event>[] eventItems1 = createEvents(1);

    // Setup the events for series 2
    final SearchResultItem<Event>[] eventItems2 = new SearchResultItem[0];

    // Setup the events for series 3
    final SearchResultItem<Event>[] eventItems3 = createEvents(0);

    final SearchResultItem<Theme> themeItem1 = EasyMock
            .createMock(SearchResultItem.class);
    EasyMock.expect(themeItem1.getSource()).andReturn(theme1);

    // Setup series search results
    final SearchResult<Series> ascSeriesSearchResult = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(ascSeriesSearchResult.getItems()).andReturn(ascSeriesItems);
    EasyMock.expect(ascSeriesSearchResult.getHitCount()).andReturn((long) ascSeriesItems.length);
    EasyMock.expect(ascSeriesSearchResult.getDocumentCount()).andReturn((long) ascSeriesItems.length);
    EasyMock.expect(ascSeriesSearchResult.getSearchTime()).andReturn(0L);
    final SearchResult<Series> descSeriesSearchResult = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(descSeriesSearchResult.getItems()).andReturn(descSeriesItems);
    EasyMock.expect(descSeriesSearchResult.getHitCount()).andReturn((long) descSeriesItems.length);
    EasyMock.expect(descSeriesSearchResult.getDocumentCount()).andReturn((long) descSeriesItems.length);
    EasyMock.expect(descSeriesSearchResult.getSearchTime()).andReturn(0L);
    // Create an empty search result.
    final SearchResult<Series> emptySearchResult = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(emptySearchResult.getPageSize()).andReturn(0L).anyTimes();
    EasyMock.expect(emptySearchResult.getDocumentCount()).andReturn(0L);
    EasyMock.expect(emptySearchResult.getSearchTime()).andReturn(0L);
    // Create a single search result for series 1.
    final SearchResult<Series> oneSearchResult = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(oneSearchResult.getPageSize()).andReturn(1L).anyTimes();
    EasyMock.expect(oneSearchResult.getItems()).andReturn(new SearchResultItem[] { item1 }).anyTimes();
    EasyMock.expect(oneSearchResult.getDocumentCount()).andReturn(1L);
    EasyMock.expect(oneSearchResult.getSearchTime()).andReturn(0L);
    // Create a single search result for series 2.
    final SearchResult<Series> twoSearchResult = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(twoSearchResult.getPageSize()).andReturn(1L).anyTimes();
    EasyMock.expect(twoSearchResult.getItems()).andReturn(new SearchResultItem[] { item2 }).anyTimes();
    EasyMock.expect(twoSearchResult.getDocumentCount()).andReturn(2L);
    EasyMock.expect(twoSearchResult.getSearchTime()).andReturn(0L);

    adminuiSearchIndex = EasyMock.createMock(AdminUISearchIndex.class);

    final Capture<SeriesSearchQuery> captureSeriesSearchQuery = EasyMock.newCapture();
    final Capture<EventSearchQuery> captureEventSearchQuery = EasyMock.newCapture();
    final Capture<ThemeSearchQuery> captureThemeSearchQuery = EasyMock.newCapture();

    EasyMock.expect(adminuiSearchIndex.getByQuery(EasyMock.capture(captureSeriesSearchQuery)))
            .andAnswer(new IAnswer<SearchResult<Series>>() {

              @Override
              public SearchResult<Series> answer() throws Throwable {
                if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getIdentifier().length == 1) {
                  if ("1".equals(captureSeriesSearchQuery.getValue().getIdentifier()[0])) {
                    return oneSearchResult;
                  } else if ("2".equals(captureSeriesSearchQuery.getValue().getIdentifier()[0])) {
                    return twoSearchResult;
                  } else {
                    return emptySearchResult;
                  }
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesContributorsSortOrder() == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesContributorsSortOrder() == Order.Descending) {
                  return descSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesDateSortOrder() == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesDateSortOrder() == Order.Descending) {
                  return descSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesOrganizersSortOrder() == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesOrganizersSortOrder() == Order.Descending) {
                  return descSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesTitleSortOrder() == Order.Ascending) {
                  return ascSeriesSearchResult;
                } else if (captureSeriesSearchQuery.hasCaptured()
                        && captureSeriesSearchQuery.getValue().getSeriesTitleSortOrder() == Order.Descending) {
                  return descSeriesSearchResult;
                } else {
                  return ascSeriesSearchResult;
                }
              }

            });

    EasyMock.expect(adminuiSearchIndex.getByQuery(EasyMock.capture(captureEventSearchQuery)))
            .andAnswer(new IAnswer<SearchResult<Event>>() {

              @Override
              public SearchResult<Event> answer() throws Throwable {
                SearchResult<Event> eventsSearchResult = EasyMock.createMock(SearchResult.class);
                if (captureEventSearchQuery.hasCaptured()
                        && "1".equals(captureEventSearchQuery.getValue().getSeriesId())
                        && !("RUNNING".equals(captureEventSearchQuery.getValue().getWorkflowState()))
                        && !("INSTANTIATED".equals(captureEventSearchQuery.getValue().getWorkflowState()))) {
                  // Setup events search results
                  EasyMock.expect(eventsSearchResult.getItems()).andReturn(eventItems1).anyTimes();
                  EasyMock.expect(eventsSearchResult.getHitCount()).andReturn((long) eventItems1.length).anyTimes();
                } else if (captureEventSearchQuery.hasCaptured()
                        && "1".equals(captureEventSearchQuery.getValue().getSeriesId())
                        && "INSTANTIATED".equals(captureEventSearchQuery.getValue().getWorkflowState())) {
                  // Setup events search results
                  EasyMock.expect(eventsSearchResult.getItems()).andReturn(eventItems2).anyTimes();
                  EasyMock.expect(eventsSearchResult.getHitCount()).andReturn((long) eventItems2.length).anyTimes();
                } else if (captureEventSearchQuery.hasCaptured()
                        && "1".equals(captureEventSearchQuery.getValue().getSeriesId())
                        && "RUNNING".equals(captureEventSearchQuery.getValue().getWorkflowState())) {
                  // Setup events search results
                  EasyMock.expect(eventsSearchResult.getItems()).andReturn(eventItems2).anyTimes();
                  EasyMock.expect(eventsSearchResult.getHitCount()).andReturn((long) eventItems2.length).anyTimes();
                } else if (captureEventSearchQuery.hasCaptured()
                        && "2".equals(captureEventSearchQuery.getValue().getSeriesId())
                        && !("RUNNING".equals(captureEventSearchQuery.getValue().getWorkflowState()))) {
                  // Setup events search results
                  EasyMock.expect(eventsSearchResult.getItems()).andReturn(eventItems2).anyTimes();
                  EasyMock.expect(eventsSearchResult.getHitCount()).andReturn((long) eventItems2.length).anyTimes();
                } else if (captureEventSearchQuery.hasCaptured()
                        && "3".equals(captureEventSearchQuery.getValue().getSeriesId())) {
                  // Setup events search results
                  EasyMock.expect(eventsSearchResult.getItems()).andReturn(eventItems3).anyTimes();
                  EasyMock.expect(eventsSearchResult.getHitCount()).andReturn((long) eventItems3.length).anyTimes();
                } else if (captureEventSearchQuery.hasCaptured()
                        && "2".equals(captureEventSearchQuery.getValue().getSeriesId())
                        && "RUNNING".equals(captureEventSearchQuery.getValue().getWorkflowState())) {
                  // Setup events search results
                  EasyMock.expect(eventsSearchResult.getItems()).andReturn(eventItems3).anyTimes();
                  EasyMock.expect(eventsSearchResult.getHitCount()).andReturn((long) eventItems3.length).anyTimes();
                } else {
                  if (!captureEventSearchQuery.hasCaptured()) {
                    Assert.fail("Haven't captured an event search query yet.");
                  } else {
                    logger.info("IDs for search query" + captureEventSearchQuery.getValue().getSeriesId());
                    Assert.fail("Tried to get an event collection that doesn't exist.");
                  }
                }
                EasyMock.replay(eventsSearchResult);
                return eventsSearchResult;
              }
            }).anyTimes();

    EasyMock.expect(adminuiSearchIndex.getByQuery(EasyMock.capture(captureThemeSearchQuery)))
            .andAnswer(new IAnswer<SearchResult<Theme>>() {

              @Override
              public SearchResult<Theme> answer() throws Throwable {
                SearchResult<Theme> themeSearchResult = EasyMock
                        .createMock(SearchResult.class);
                // Setup theme search results
                EasyMock.expect(themeSearchResult.getPageSize()).andReturn(1L).anyTimes();
                EasyMock.expect(themeSearchResult.getItems()).andReturn(new SearchResultItem[] { themeItem1 })
                        .anyTimes();
                EasyMock.replay(themeSearchResult);
                return themeSearchResult;
              }
            }).anyTimes();

    EasyMock.replay(adminuiSearchIndex, item1, item2, item3, themeItem1, ascSeriesSearchResult, descSeriesSearchResult,
            emptySearchResult, oneSearchResult, twoSearchResult);
  }

}
