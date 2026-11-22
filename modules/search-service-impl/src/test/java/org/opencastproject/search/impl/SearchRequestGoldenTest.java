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

package org.opencastproject.search.impl;

import static org.opencastproject.search.impl.QueryJson.assertMatchesGolden;

import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.api.SearchServiceQuery;
import org.opencastproject.search.endpoint.SearchRestService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.requests.SortCriterion.Order;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.Date;

/**
 * Makes sure the requests sent to OpenSearch by the search service and its users do not change unintentionally.
 */
public class SearchRequestGoldenTest {

  private static final JaxbOrganization ORG = new DefaultOrganization();

  private static final User ADMIN = new JaxbUser("admin", "test", ORG,
      new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, ORG));

  private static final User USER = new JaxbUser("user", "test", ORG,
      new JaxbRole("ROLE_USER_A", ORG), new JaxbRole("ROLE_USER_B", ORG));

  /** Thrown by the mocked index to stop processing once the request has been captured. */
  private static final class Captured extends RuntimeException {
  }

  private final Capture<SearchServiceQuery> capture = Capture.newInstance();

  private SearchRestService endpoint(User user) throws Exception {
    final SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(ORG).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();

    final SearchServiceImpl searchService = EasyMock.createNiceMock(SearchServiceImpl.class);
    EasyMock.expect(searchService.search(EasyMock.capture(capture))).andThrow(new Captured()).anyTimes();

    final SearchServiceIndex searchIndex = EasyMock.createNiceMock(SearchServiceIndex.class);
    EasyMock.expect(searchIndex.search(EasyMock.capture(capture))).andThrow(new Captured()).anyTimes();

    EasyMock.replay(securityService, searchService, searchIndex);

    final SearchRestService endpoint = new SearchRestService();
    endpoint.setSecurityService(securityService);
    endpoint.setSearchService(searchService);
    endpoint.setSearchIndex(searchIndex);
    return endpoint;
  }

  private interface Request {
    void run() throws Exception;
  }

  /** Run the request and return the body of the search request it sent. */
  private String captured(Request request) throws Exception {
    try {
      request.run();
    } catch (Captured e) {
      return SearchServiceIndex.searchRequestBody(capture.getValue());
    }
    throw new AssertionError("No search request was sent");
  }

  @Test
  public void seriesAdminDefault() throws Exception {
    final SearchRestService endpoint = endpoint(ADMIN);
    assertMatchesGolden("rest-series-admin-default",
        captured(() -> endpoint.getSeries(null, null, null, null, null)));
  }

  @Test
  public void seriesUserAllParameters() throws Exception {
    final SearchRestService endpoint = endpoint(USER);
    assertMatchesGolden("rest-series-user-all",
        captured(() -> endpoint.getSeries("series-1", "intro", "title desc", "5", "10")));
  }

  @Test
  public void seriesSortModified() throws Exception {
    final SearchRestService endpoint = endpoint(ADMIN);
    assertMatchesGolden("rest-series-sort-modified",
        captured(() -> endpoint.getSeries(null, null, "modified", null, null)));
  }

  @Test
  public void episodeAdminDefault() throws Exception {
    final SearchRestService endpoint = endpoint(ADMIN);
    assertMatchesGolden("rest-episode-admin-default",
        captured(() -> endpoint.getEpisodes(null, null, null, null, null, null, null, null, null)));
  }

  @Test
  public void episodeUserAllParameters() throws Exception {
    final SearchRestService endpoint = endpoint(USER);
    assertMatchesGolden("rest-episode-user-all",
        captured(() -> endpoint.getEpisodes("event-1", "intro", "series-1", null, "created asc", "7", "3", "false",
            true)));
  }

  @Test
  public void episodeSortModified() throws Exception {
    final SearchRestService endpoint = endpoint(ADMIN);
    assertMatchesGolden("rest-episode-sort-modified",
        captured(() -> endpoint.getEpisodes(null, null, null, null, "modified desc", null, null, null, false)));
  }

  @Test
  public void episodeSeriesNameLookup() throws Exception {
    final SearchRestService endpoint = endpoint(ADMIN);
    assertMatchesGolden("rest-episode-series-name-lookup",
        captured(() -> endpoint.getEpisodes(null, null, null, "Series", null, null, null, null, null)));
  }

  /** The query the Tobira harvest API sends (see {@code org.opencastproject.tobira.impl.Harvest}). */
  @Test
  public void tobiraHarvest() throws Exception {
    final SearchServiceQuery query = new SearchServiceQuery(ORG.getId(), SearchService.IndexEntryType.Episode)
        .withDeleted()
        .withModifiedSince(new Date(1700000000000L))
        .sortBy(SearchServiceQuery.SORT_MODIFIED, Order.Ascending)
        .withLimit(51);
    assertMatchesGolden("tobira-harvest", SearchServiceIndex.searchRequestBody(query));
  }

  /** The query the adopter registration sends (see {@code AdopterRegistrationServiceImpl}). */
  @Test
  public void adopterStatistics() throws Exception {
    final SearchServiceQuery query = new SearchServiceQuery(ORG.getId(), SearchService.IndexEntryType.Episode);
    assertMatchesGolden("adopter-statistics", SearchServiceIndex.searchRequestBody(query));
  }
}
