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

package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter.SourceType;
import org.opencastproject.index.service.resources.list.provider.AgentsListProvider;
import org.opencastproject.index.service.resources.list.provider.ContributorsListProvider;
import org.opencastproject.index.service.resources.list.provider.EventsListProvider;
import org.opencastproject.index.service.resources.list.provider.SeriesListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import java.util.Date;

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

  public EventListQuery() {
    super();
    this.availableFilters.add(createSeriesFilter(Option.<String> none()));
    this.availableFilters.add(createPresentersFilter(Option.<String> none()));
    this.availableFilters.add(createTechnicalPresentersFilter(Option.<String> none()));
    this.availableFilters.add(createContributorsFilter(Option.<String> none()));
    this.availableFilters.add(createLocationFilter(Option.<String> none()));
    this.availableFilters.add(createAgentFilter(Option.<String> none()));
    this.availableFilters.add(createStartDateFilter(Option.<Tuple<Date, Date>> none()));
    this.availableFilters.add(createStatusFilter(Option.<String> none()));
    this.availableFilters.add(createCommentsFilter(Option.<String> none()));
    this.availableFilters.add(createPublisherFilter(Option.<String> none()));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given seriesId
   *
   * @param seriesId
   *          the seriesId to filter with
   */
  public void withSeriesId(String seriesId) {
    this.addFilter(createSeriesFilter(Option.option(seriesId)));
  }

  /**
   * Returns an {@link Option} containing the seriesId used to filter if set
   *
   * @return an {@link Option} containing the seriesId or none.
   */
  public Option<String> getSeriesId() {
    return this.getFilterValue(FILTER_SERIES_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given presenter
   *
   * @param presenter
   *          the presenter to filter for
   */
  public void withPresenter(String presenter) {
    this.addFilter(createPresentersFilter(Option.option(presenter)));
  }

  /**
   * Returns an {@link Option} containing the presenter used to filter if set
   *
   * @return an {@link Option} containing the presenter or none.
   */
  public Option<String> getPresenter() {
    return this.getFilterValue(FILTER_PRESENTERS_BIBLIOGRAPHIC_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given technical presenter's username
   *
   * @param presenter
   *          presenter's username the presenter's username to filter for
   */
  public void withTechnicalPresenter(String presenter) {
    this.addFilter(createTechnicalPresentersFilter(Option.option(presenter)));
  }

  /**
   * Returns an {@link Option} containing the technical presenter's username used to filter if set
   *
   * @return an {@link Option} containing the presenter or none.
   */
  public Option<String> getTechnicalPresenter() {
    return this.getFilterValue(FILTER_PRESENTERS_TECHNICAL_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given contributor
   *
   * @param contributor
   *          the contributor to filter for
   */
  public void withContributor(String contributor) {
    this.addFilter(createContributorsFilter(Option.option(contributor)));
  }

  /**
   * Returns an {@link Option} containing the contributor used to filter if set
   *
   * @return an {@link Option} containing the contributor or none.
   */
  public Option<String> getContributor() {
    return this.getFilterValue(FILTER_CONTRIBUTORS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param location
   *          the subject to filter for
   */
  public void withLocation(String location) {
    this.addFilter(createLocationFilter(Option.option(location)));
  }

  /**
   * Returns an {@link Option} containing the location used to filter if set
   *
   * @return an {@link Option} containing the location or none.
   */
  public Option<String> getLocation() {
    return this.getFilterValue(FILTER_LOCATION_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param agent
   *          the agent to filter for
   */
  public void withAgent(String agent) {
    this.addFilter(createAgentFilter(Option.option(agent)));
  }

  /**
   * Returns an {@link Option} containing the agent used to filter if set
   *
   * @return an {@link Option} containing the agent or none.
   */
  public Option<String> getAgent() {
    return this.getFilterValue(FILTER_AGENT_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param language
   *          the subject to filter for
   */
  public void withLanguage(String language) {
    this.addFilter(createLanguageFilter(Option.option(language)));
  }

  /**
   * Returns an {@link Option} containing the language used to filter if set
   *
   * @return an {@link Option} containing the language or none.
   */
  public Option<String> getLanguage() {
    return this.getFilterValue(FILTER_LANGUAGE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given start date period
   *
   * @param startDate
   *          the start date period as {@link Tuple} with two {@link Date}.
   */
  public void withStartDate(Tuple<Date, Date> startDate) {
    this.addFilter(createStartDateFilter(Option.option(startDate)));
  }

  /**
   * Returns an {@link Option} containing the start date period used to filter if set
   *
   * @return an {@link Option} containing the start date period or none.
   */
  public Option<Tuple<Date, Date>> getStartDate() {
    return this.getFilterValue(FILTER_STARTDATE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given status
   *
   * @param status
   *          the status to filter for
   */
  public void withStatus(String status) {
    this.addFilter(createStatusFilter(Option.option(status)));
  }

  /**
   * Returns an {@link Option} containing the status used to filter if set
   *
   * @return an {@link Option} containing the status or none.
   */
  public Option<String> getStatus() {
    return this.getFilterValue(FILTER_STATUS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given comments
   *
   * @param comments
   *          the comments to filter for
   */
  public void withComments(String comments) {
    this.addFilter(createCommentsFilter(Option.option(comments)));
  }

  /**
   * Returns an {@link Option} containing the comments used to filter if set
   *
   * @return an {@link Option} containing the comments or none.
   */
  public Option<String> getComments() {
    return this.getFilterValue(FILTER_COMMENTS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given publishers
   *
   * @param publishers
   *          the publishers to filter for
   */
  public void withPublishers(String publishers) {
    this.addFilter(createPublisherFilter(Option.option(publishers)));
  }

  /**
   * Returns an {@link Option} containing the publisher used to filter if set
   *
   * @return an {@link Option} containing the publisher or none.
   */
  public Option<String> getPublisher() {
    return this.getFilterValue(FILTER_PUBLISHER_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on the Series id
   *
   * @param seriesId
   *          the series id to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for the Series name based query
   */
  public static ResourceListFilter<String> createSeriesFilter(Option<String> seriesId) {
    return FiltersUtils.generateFilter(seriesId, FILTER_SERIES_NAME, FILTER_SERIES_LABEL, SourceType.SELECT,
            Option.some(SeriesListProvider.PROVIDER_PREFIX));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a presenter's full name
   *
   * @param presenter's
   *          name the presenters to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a presenters based query
   */
  public static ResourceListFilter<String> createPresentersFilter(Option<String> presenter) {
    return FiltersUtils.generateFilter(presenter, FILTER_PRESENTERS_BIBLIOGRAPHIC_NAME,
            FILTER_PRESENTERS_BIBLIOGRAPHIC_LABEL, SourceType.SELECT, Option.some(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a presenter's user name
   *
   * @param presenter
   *          the presenters to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a presenters based query
   */
  public static ResourceListFilter<String> createTechnicalPresentersFilter(Option<String> presenter) {
    return FiltersUtils.generateFilter(presenter, FILTER_PRESENTERS_TECHNICAL_NAME, FILTER_PRESENTERS_TECHNICAL_LABEL,
            SourceType.SELECT, Option.some(ContributorsListProvider.USERNAMES));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a contributor
   *
   * @param contributor
   *          the series id to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a contributor based query
   */
  public static ResourceListFilter<String> createContributorsFilter(Option<String> contributor) {
    return FiltersUtils.generateFilter(contributor, FILTER_CONTRIBUTORS_NAME, FILTER_CONTRIBUTORS_LABEL,
            SourceType.SELECT, Option.some(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a location
   *
   * @param location
   *          the location to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a location based query
   */
  public static ResourceListFilter<String> createLocationFilter(Option<String> location) {
    return FiltersUtils.generateFilter(location, FILTER_LOCATION_NAME, FILTER_LOCATION_LABEL, SourceType.SELECT,
            Option.some(EventsListProvider.LOCATION));
  }

  /**
   * Create a new {@link ResourceListFilter} based on an agent
   *
   * @param agent
   *          the agent to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a location based query
   */
  public static ResourceListFilter<String> createAgentFilter(Option<String> agent) {
    return FiltersUtils.generateFilter(agent, FILTER_AGENT_NAME, FILTER_AGENT_LABEL, SourceType.SELECT,
            Option.some(AgentsListProvider.NAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a language
   *
   * @param language
   *          the language to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a language based query
   */
  public static ResourceListFilter<String> createLanguageFilter(Option<String> language) {
    return FiltersUtils.generateFilter(language, FILTER_LANGUAGE_NAME, FILTER_LANGUAGE_LABEL, SourceType.SELECT,
            Option.some("LANGUAGES"));
  }

  /**
   * Create a new {@link ResourceListFilter} based on start date period
   *
   * @param period
   *          the period to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for the given period
   */
  public static ResourceListFilter<Tuple<Date, Date>> createStartDateFilter(Option<Tuple<Date, Date>> period) {
    return FiltersUtils.generateFilter(period, FILTER_STARTDATE_NAME, FILTER_STARTDATE_LABEL, SourceType.PERIOD,
            Option.some(EventsListProvider.START_DATE));
  }

  /**
   * Create a new {@link ResourceListFilter} based on stats
   *
   * @param status
   *          the status to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for progress based query
   */
  public static ResourceListFilter<String> createStatusFilter(Option<String> status) {
    return FiltersUtils.generateFilter(status, FILTER_STATUS_NAME, FILTER_STATUS_LABEL, SourceType.SELECT,
            Option.some(EventsListProvider.STATUS));
  }

  /**
   * Create a new {@link ResourceListFilter} based on comments
   *
   * @param comments
   *          the comments to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for progress based query
   */
  public static ResourceListFilter<String> createCommentsFilter(Option<String> comments) {
    return FiltersUtils.generateFilter(comments, FILTER_COMMENTS_NAME, FILTER_COMMENTS_LABEL, SourceType.SELECT,
            Option.some(EventsListProvider.COMMENTS));
  }

  /**
   * Create a new {@link ResourceListFilter} based on publishers
   *
   * @param publisher
   *          the publisher to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for progress based query
   */
  public static ResourceListFilter<String> createPublisherFilter(Option<String> publisher) {
    return FiltersUtils.generateFilter(publisher, FILTER_PUBLISHER_NAME, FILTER_PUBLISHER_LABEL, SourceType.SELECT,
            Option.some(EventsListProvider.PUBLISHER));
  }
}
