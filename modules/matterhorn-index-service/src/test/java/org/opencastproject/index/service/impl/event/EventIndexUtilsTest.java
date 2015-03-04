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
package org.opencastproject.index.service.impl.event;

import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexUtils;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.User;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

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
    @SuppressWarnings("unchecked")
    SearchResult<Series> emptyResult = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(emptyResult.getHitCount()).andReturn(0L).anyTimes();

    Event event = EasyMock.createMock(Event.class);
    EasyMock.expect(event.getSeriesId()).andReturn(seriesId).anyTimes();
    EasyMock.expect(event.getSeriesName()).andReturn(null).anyTimes();
    EasyMock.expect(event.getIdentifier()).andReturn(eventId).anyTimes();

    AbstractSearchIndex searchIndex = EasyMock.createMock(AbstractSearchIndex.class);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(emptyResult);

    EasyMock.replay(emptyResult, event, searchIndex);
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
    SearchResultItem<Series> seriesResult = EasyMock.createMock(SearchResultItem.class);
    EasyMock.expect(seriesResult.getSource()).andReturn(series);
    ArrayList<SearchResultItem<Series>> seriesCollection = new ArrayList<SearchResultItem<Series>>();
    seriesCollection.add(seriesResult);

    SearchResult<Series> eventuallyResult = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(eventuallyResult.getHitCount()).andReturn(0L);
    EasyMock.expect(eventuallyResult.getHitCount()).andReturn(0L);
    EasyMock.expect(eventuallyResult.getHitCount()).andReturn(1L);
    EasyMock.expect(eventuallyResult.getItems()).andReturn(seriesCollection.toArray(new SearchResultItem[1]));

    Event event = EasyMock.createMock(Event.class);
    EasyMock.expect(event.getSeriesId()).andReturn(seriesId).anyTimes();
    EasyMock.expect(event.getSeriesName()).andReturn(null).anyTimes();
    EasyMock.expect(event.getIdentifier()).andReturn(eventId).anyTimes();
    event.setSeriesName(seriesId);
    EasyMock.expectLastCall();

    AbstractSearchIndex searchIndex = EasyMock.createMock(AbstractSearchIndex.class);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(eventuallyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(eventuallyResult);
    EasyMock.expect(searchIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(eventuallyResult);

    EasyMock.replay(eventuallyResult, event, searchIndex, series, seriesResult);
    // Run test
    EventIndexUtils.updateSeriesName(event, defaultOrganization.getId(), user, searchIndex, 3, 50L);
  }

}
