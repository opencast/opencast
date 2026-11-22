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

package org.opencastproject.opensearch.index.objects.event;

import org.opencastproject.opensearch.api.SearchIndexException;
import org.opencastproject.opensearch.api.SearchResult;
import org.opencastproject.opensearch.index.OpenSearchIndex;
import org.opencastproject.opensearch.index.objects.series.Series;
import org.opencastproject.opensearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.User;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class EventIndexUtilsTest {

  private User user;
  private final JaxbOrganization defaultOrganization = new DefaultOrganization();

  @Before
  public void setUp() {
    user = new JaxbUser("test", "test", defaultOrganization, new JaxbRole(
            DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, defaultOrganization));
  }

  @Test
  public void testUpdateSeriesNameInputSeriesNotAddedToIndexExpectsGivesUp() throws SearchIndexException {
    // Input data
    String seriesId = "my_series";
    String eventId = "my_event";

    // Mocks
    SearchResult<Series> emptyResult = SearchResult.empty();

    Event event = EasyMock.createMock(Event.class);
    EasyMock.expect(event.getSeriesId()).andReturn(seriesId).anyTimes();
    EasyMock.expect(event.getSeriesName()).andReturn(null).anyTimes();
    EasyMock.expect(event.getIdentifier()).andReturn(eventId).anyTimes();

    OpenSearchIndex searchIndex = EasyMock.createMock(OpenSearchIndex.class);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);

    EasyMock.replay(event, searchIndex);
    // Run test
    EventIndexUtils.updateSeriesName(event, defaultOrganization.getId(), user, searchIndex, 3, 50L);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateSeriesNameInputSeriesEventuallyAddedToIndexExpectsSetsName() throws SearchIndexException {
    // Input data
    String seriesId = "my_series";
    String eventId = "my_event";

    // Mocks
    Series series = EasyMock.createMock(Series.class);
    EasyMock.expect(series.getTitle()).andReturn(seriesId);
    SearchResult<Series> emptyResult = SearchResult.empty();
    SearchResult<Series> seriesResult = new SearchResult<>(List.of(series), 1, 0, 0);

    Event event = EasyMock.createMock(Event.class);
    EasyMock.expect(event.getSeriesId()).andReturn(seriesId).anyTimes();
    EasyMock.expect(event.getSeriesName()).andReturn(null).anyTimes();
    EasyMock.expect(event.getIdentifier()).andReturn(eventId).anyTimes();
    event.setSeriesName(seriesId);
    EasyMock.expectLastCall();

    OpenSearchIndex searchIndex = EasyMock.createMock(OpenSearchIndex.class);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(seriesResult);

    EasyMock.replay(event, searchIndex, series);
    // Run test
    EventIndexUtils.updateSeriesName(event, defaultOrganization.getId(), user, searchIndex, 3, 50L);
  }

}
