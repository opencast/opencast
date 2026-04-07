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

package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.provider.AgentsListProvider;
import org.opencastproject.index.service.resources.list.provider.ContributorsListProvider;
import org.opencastproject.index.service.resources.list.provider.EventsListProvider;
import org.opencastproject.index.service.resources.list.provider.SeriesListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListFilter.SourceType;
import org.opencastproject.list.impl.ResourceListQueryImpl;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.Optional;

/**
 * Query for the events list.
 *
 * The following filters can be used:
 * <ul>
 * <li>series</li>
 * <li>presenters</li>
 * <li>presenter's usernames</li>
 * <li>contributors</li>
 * <li>location</li>
 * <li>agent</li>
 * <li>language</li>
 * <li>startDate</li>
 * <li>status</li>
 * </ul>
 */
public class EventListQuery extends ResourceListQueryImpl {

  public static final String FILTER_SERIES_NAME = "series";
  private static final String FILTER_SERIES_LABEL = "FILTERS.EVENTS.SERIES.LABEL";

  public static final String FILTER_PRESENTERS_BIBLIOGRAPHIC_NAME = "presentersBibliographic";
  private static final String FILTER_PRESENTERS_BIBLIOGRAPHIC_LABEL = "FILTERS.EVENTS.PRESENTERS_BIBLIOGRAPHIC.LABEL";

  public static final String FILTER_PRESENTERS_TECHNICAL_NAME = "presentersTechnical";
  private static final String FILTER_PRESENTERS_TECHNICAL_LABEL = "FILTERS.EVENTS.PRESENTERS_TECHNICAL.LABEL";

  public static final String FILTER_CONTRIBUTORS_NAME = "contributors";
  private static final String FILTER_CONTRIBUTORS_LABEL = "FILTERS.EVENTS.CONTRIBUTORS.LABEL";

  public static final String FILTER_LOCATION_NAME = "location";
  private static final String FILTER_LOCATION_LABEL = "FILTERS.EVENTS.LOCATION.LABEL";

  public static final String FILTER_AGENT_NAME = "agent";
  private static final String FILTER_AGENT_LABEL = "FILTERS.EVENTS.AGENT_ID.LABEL";

  public static final String FILTER_LANGUAGE_NAME = "language";
  private static final String FILTER_LANGUAGE_LABEL = "FILTERS.EVENTS.LANGUAGE.LABEL";

  public static final String FILTER_STARTDATE_NAME = "startDate";
  private static final String FILTER_STARTDATE_LABEL = "FILTERS.EVENTS.START_DATE.LABEL";

  public static final String FILTER_STATUS_NAME = "status";
  private static final String FILTER_STATUS_LABEL = "FILTERS.EVENTS.STATUS.LABEL";

  public static final String FILTER_COMMENTS_NAME = "comments";
  private static final String FILTER_COMMENTS_LABEL = "FILTERS.EVENTS.COMMENTS.LABEL";

  public static final String FILTER_PUBLISHER_NAME = "publisher";
  private static final String FILTER_PUBLISHER_LABEL = "FILTERS.EVENTS.PUBLISHER.LABEL";

  public static final String FILTER_TEXT_NAME = "textFilter";

  public static final String FILTER_IS_PUBLISHED_NAME = "isPublished";
  public static final String FILTER_IS_PUBLISHED_LABEL = "FILTERS.EVENTS.IS_PUBLISHED.LABEL";

  public static final String FILTER_READ_ACCESS_NAME = "readAccess";
  public static final String FILTER_READ_ACCESS_LABEL = "FILTERS.EVENTS.READ_ACCESS.LABEL";

  public static final String FILTER_WRITE_ACCESS_NAME = "writeAccess";
  public static final String FILTER_WRITE_ACCESS_LABEL = "FILTERS.EVENTS.WRITE_ACCESS.LABEL";

  public EventListQuery() {
    super();
    this.availableFilters.add(createSeriesFilter(Optional.empty()));
    this.availableFilters.add(createLocationFilter(Optional.empty()));
    this.availableFilters.add(createAgentFilter(Optional.empty()));
    this.availableFilters.add(createLanguageFilter(Optional.empty()));
    this.availableFilters.add(createStartDateFilter(Optional.empty()));
    this.availableFilters.add(createStatusFilter(Optional.empty()));
    this.availableFilters.add(createCommentsFilter(Optional.empty()));
    this.availableFilters.add(createIsPublishedFilter(Optional.empty()));
    this.availableFilters.add(createReadAccessFilter(Optional.empty()));
    this.availableFilters.add(createWriteAccessFilter(Optional.empty()));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given seriesId
   *
   * @param seriesId
   *          the seriesId to filter with
   */
  public void withSeriesId(String seriesId) {
    this.addFilter(createSeriesFilter(Optional.ofNullable(seriesId)));
  }

  /**
   * Returns an {@link Optional} containing the seriesId used to filter if set
   *
   * @return an {@link Optional} containing the seriesId or none.
   */
  public Optional<String> getSeriesId() {
    return this.getFilterValue(FILTER_SERIES_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given presenter
   *
   * @param presenter
   *          the presenter to filter for
   */
  public void withPresenter(String presenter) {
    this.addFilter(createPresentersFilter(Optional.ofNullable(presenter)));
  }

  /**
   * Returns an {@link Optional} containing the presenter used to filter if set
   *
   * @return an {@link Optional} containing the presenter or none.
   */
  public Optional<String> getPresenter() {
    return this.getFilterValue(FILTER_PRESENTERS_BIBLIOGRAPHIC_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given technical presenter's username
   *
   * @param presenter
   *          presenter's username the presenter's username to filter for
   */
  public void withTechnicalPresenter(String presenter) {
    this.addFilter(createTechnicalPresentersFilter(Optional.ofNullable(presenter)));
  }

  /**
   * Returns an {@link Optional} containing the technical presenter's username used to filter if set
   *
   * @return an {@link Optional} containing the presenter or none.
   */
  public Optional<String> getTechnicalPresenter() {
    return this.getFilterValue(FILTER_PRESENTERS_TECHNICAL_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given contributor
   *
   * @param contributor
   *          the contributor to filter for
   */
  public void withContributor(String contributor) {
    this.addFilter(createContributorsFilter(Optional.ofNullable(contributor)));
  }

  /**
   * Returns an {@link Optional} containing the contributor used to filter if set
   *
   * @return an {@link Optional} containing the contributor or none.
   */
  public Optional<String> getContributor() {
    return this.getFilterValue(FILTER_CONTRIBUTORS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param location
   *          the subject to filter for
   */
  public void withLocation(String location) {
    this.addFilter(createLocationFilter(Optional.ofNullable(location)));
  }

  /**
   * Returns an {@link Optional} containing the location used to filter if set
   *
   * @return an {@link Optional} containing the location or none.
   */
  public Optional<String> getLocation() {
    return this.getFilterValue(FILTER_LOCATION_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param agent
   *          the agent to filter for
   */
  public void withAgent(String agent) {
    this.addFilter(createAgentFilter(Optional.ofNullable(agent)));
  }

  /**
   * Returns an {@link Optional} containing the agent used to filter if set
   *
   * @return an {@link Optional} containing the agent or none.
   */
  public Optional<String> getAgent() {
    return this.getFilterValue(FILTER_AGENT_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param language
   *          the subject to filter for
   */
  public void withLanguage(String language) {
    this.addFilter(createLanguageFilter(Optional.ofNullable(language)));
  }

  /**
   * Returns an {@link Optional} containing the language used to filter if set
   *
   * @return an {@link Optional} containing the language or none.
   */
  public Optional<String> getLanguage() {
    return this.getFilterValue(FILTER_LANGUAGE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given start date period
   *
   * @param startDate
   *          the start date period as {@link Tuple} with two {@link Date}.
   */
  public void withStartDate(Tuple<Date, Date> startDate) {
    this.addFilter(createStartDateFilter(Optional.ofNullable(startDate)));
  }

  /**
   * Returns an {@link Optional} containing the start date period used to filter if set
   *
   * @return an {@link Optional} containing the start date period or none.
   */
  public Optional<Tuple<Date, Date>> getStartDate() {
    return this.getFilterValue(FILTER_STARTDATE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given status
   *
   * @param status
   *          the status to filter for
   */
  public void withStatus(String status) {
    this.addFilter(createStatusFilter(Optional.ofNullable(status)));
  }

  /**
   * Returns an {@link Optional} containing the status used to filter if set
   *
   * @return an {@link Optional} containing the status or none.
   */
  public Optional<String> getStatus() {
    return this.getFilterValue(FILTER_STATUS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given comments
   *
   * @param comments
   *          the comments to filter for
   */
  public void withComments(String comments) {
    this.addFilter(createCommentsFilter(Optional.ofNullable(comments)));
  }

  /**
   * Returns an {@link Optional} containing the comments used to filter if set
   *
   * @return an {@link Optional} containing the comments or none.
   */
  public Optional<String> getComments() {
    return this.getFilterValue(FILTER_COMMENTS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given publishers
   *
   * @param publishers
   *          the publishers to filter for
   */
  public void withPublishers(String publishers) {
    this.addFilter(createPublisherFilter(Optional.ofNullable(publishers)));
  }

  /**
   * Returns an {@link Optional} containing the publisher used to filter if set
   *
   * @return an {@link Optional} containing the publisher or none.
   */
  public Optional<String> getPublisher() {
    return this.getFilterValue(FILTER_PUBLISHER_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on the Series id
   *
   * @param seriesId
   *          the series id to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for the Series name based query
   */
  public static ResourceListFilter<String> createSeriesFilter(Optional<String> seriesId) {
    return FiltersUtils.generateFilter(seriesId, FILTER_SERIES_NAME, FILTER_SERIES_LABEL, SourceType.SELECT,
            Optional.of(SeriesListProvider.PROVIDER_PREFIX));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a presenter's full name
   *
   * @param presenter's
   *          name the presenters to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a presenters based query
   */
  public static ResourceListFilter<String> createPresentersFilter(Optional<String> presenter) {
    return FiltersUtils.generateFilter(presenter, FILTER_PRESENTERS_BIBLIOGRAPHIC_NAME,
            FILTER_PRESENTERS_BIBLIOGRAPHIC_LABEL, SourceType.SELECT, Optional.of(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a presenter's user name
   *
   * @param presenter
   *          the presenters to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a presenters based query
   */
  public static ResourceListFilter<String> createTechnicalPresentersFilter(Optional<String> presenter) {
    return FiltersUtils.generateFilter(presenter, FILTER_PRESENTERS_TECHNICAL_NAME, FILTER_PRESENTERS_TECHNICAL_LABEL,
            SourceType.SELECT, Optional.of(ContributorsListProvider.USERNAMES));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a contributor
   *
   * @param contributor
   *          the series id to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a contributor based query
   */
  public static ResourceListFilter<String> createContributorsFilter(Optional<String> contributor) {
    return FiltersUtils.generateFilter(contributor, FILTER_CONTRIBUTORS_NAME, FILTER_CONTRIBUTORS_LABEL,
            SourceType.SELECT, Optional.of(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a location
   *
   * @param location
   *          the location to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a location based query
   */
  public static ResourceListFilter<String> createLocationFilter(Optional<String> location) {
    return FiltersUtils.generateFilter(location, FILTER_LOCATION_NAME, FILTER_LOCATION_LABEL, SourceType.SELECT,
            Optional.of(EventsListProvider.LOCATION));
  }

  /**
   * Create a new {@link ResourceListFilter} based on an agent
   *
   * @param agent
   *          the agent to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a location based query
   */
  public static ResourceListFilter<String> createAgentFilter(Optional<String> agent) {
    return FiltersUtils.generateFilter(agent, FILTER_AGENT_NAME, FILTER_AGENT_LABEL, SourceType.SELECT,
            Optional.of(AgentsListProvider.NAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a language
   *
   * @param language
   *          the language to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a language based query
   */
  public static ResourceListFilter<String> createLanguageFilter(Optional<String> language) {
    return FiltersUtils.generateFilter(language, FILTER_LANGUAGE_NAME, FILTER_LANGUAGE_LABEL, SourceType.SELECT,
            Optional.of("LANGUAGES"));
  }

  /**
   * Create a new {@link ResourceListFilter} based on start date period
   *
   * @param period
   *          the period to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for the given period
   */
  public static ResourceListFilter<Tuple<Date, Date>> createStartDateFilter(Optional<Tuple<Date, Date>> period) {
    return FiltersUtils.generateFilter(period, FILTER_STARTDATE_NAME, FILTER_STARTDATE_LABEL, SourceType.PERIOD,
            Optional.empty());
  }

  /**
   * Create a new {@link ResourceListFilter} based on stats
   *
   * @param status
   *          the status to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for progress based query
   */
  public static ResourceListFilter<String> createStatusFilter(Optional<String> status) {
    return FiltersUtils.generateFilter(status, FILTER_STATUS_NAME, FILTER_STATUS_LABEL, SourceType.SELECT,
            Optional.of(EventsListProvider.STATUS));
  }

  /**
   * Create a new {@link ResourceListFilter} based on comments
   *
   * @param comments
   *          the comments to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for progress based query
   */
  public static ResourceListFilter<String> createCommentsFilter(Optional<String> comments) {
    return FiltersUtils.generateFilter(comments, FILTER_COMMENTS_NAME, FILTER_COMMENTS_LABEL, SourceType.SELECT,
            Optional.of(EventsListProvider.COMMENTS));
  }

  /**
   * Create a new {@link ResourceListFilter} based on publishers
   *
   * @param publisher
   *          the publisher to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for progress based query
   */
  public static ResourceListFilter<String> createPublisherFilter(Optional<String> publisher) {
    return FiltersUtils.generateFilter(publisher, FILTER_PUBLISHER_NAME, FILTER_PUBLISHER_LABEL, SourceType.SELECT,
            Optional.of(EventsListProvider.PUBLISHER));
  }

  /**
   * Create a new {@link ResourceListFilter} based on is published
   * @param isPublished
   *          the is published status to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for progress based query
   */
  public static ResourceListFilter<String> createIsPublishedFilter(Optional<String> isPublished) {
    return FiltersUtils.generateFilter(isPublished, FILTER_IS_PUBLISHED_NAME, FILTER_IS_PUBLISHED_LABEL,
        SourceType.SELECT, Optional.of(EventsListProvider.ISPUBLISHED));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a read role
   *
   * @param readAccess
   *          the read role to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a read role based query
   */
  public static ResourceListFilter<String> createReadAccessFilter(Optional<String> readAccess) {
    return FiltersUtils.generateFilter(readAccess, FILTER_READ_ACCESS_NAME, FILTER_READ_ACCESS_LABEL,
        SourceType.FREETEXT, Optional.of("ROLES.TARGET.ACL"));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a write role
   *
   * @param writeAccess
   *          the write role to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a write role based query
   */
  public static ResourceListFilter<String> createWriteAccessFilter(Optional<String> writeAccess) {
    return FiltersUtils.generateFilter(writeAccess, FILTER_WRITE_ACCESS_NAME, FILTER_WRITE_ACCESS_LABEL,
        SourceType.FREETEXT, Optional.of("ROLES.TARGET.ACL"));
  }

}
