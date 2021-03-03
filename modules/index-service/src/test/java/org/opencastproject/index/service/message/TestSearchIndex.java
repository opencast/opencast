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
package org.opencastproject.index.service.message;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.impl.SearchResultImpl;
import org.opencastproject.elasticsearch.impl.SearchResultItemImpl;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;

import java.util.HashSet;

public class TestSearchIndex extends AbstractSearchIndex {

  private Event initialEvent = new Event();
  private Series initialSeries = new Series();
  private Event eventResult;
  private Series seriesResult;

  public Event getEventResult() {
    return eventResult;
  }

  public Series getSeriesResult() {
    return seriesResult;
  }

  public void setInitialEvent(Event initialEvent) {
    this.initialEvent = initialEvent;
  }

  public void setInitialSeries(Series initialSeries) {
    this.initialSeries = initialSeries;
  }

  @Override
  public String getIndexName() {
    return "test";
  }

  @Override
  public String[] getDocumentTypes() {
    return null;
  }

  @Override
  public SearchResult<Event> getByQuery(EventSearchQuery query) throws SearchIndexException {
    SearchResultImpl<Event> result = new SearchResultImpl<>(query, 1, 0L);
    result.addResultItem(new SearchResultItemImpl<>(1d, initialEvent));
    return result;
  }

  @Override
  public SearchResult<Series> getByQuery(SeriesSearchQuery query) throws SearchIndexException {
    SearchResultImpl<Series> result = new SearchResultImpl<>(query, 1, 0L);
    result.addResultItem(new SearchResultItemImpl<>(1d, initialSeries));
    return result;
  }

  @Override
  public void addOrUpdate(Event event) throws SearchIndexException {
    this.eventResult = event;
  }

  @Override
  public void addOrUpdate(Series series) throws SearchIndexException {
    this.seriesResult = series;
  }

  public static final SecurityService createSecurityService(DefaultOrganization organization) {
    JaxbUser creator = new JaxbUser("creator", "password", "Creator", null, "test", organization,
            new HashSet<JaxbRole>());
    SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    expect(securityService.getUser()).andReturn(creator).anyTimes();
    replay(securityService);
    return securityService;
  }

}
