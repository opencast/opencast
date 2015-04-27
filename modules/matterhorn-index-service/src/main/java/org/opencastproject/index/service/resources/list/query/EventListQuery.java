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
package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter.SourceType;
import org.opencastproject.index.service.resources.list.provider.EventsListProvider;
import org.opencastproject.index.service.resources.list.provider.LanguagesListProvider;
import org.opencastproject.index.service.resources.list.provider.SeriesListProvider;
import org.opencastproject.index.service.resources.list.provider.UsersListProvider;
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
 * <li>contributors</li>
 * <li>location</li>
 * <li>language</li>
 * <li>startDate</li>
 * <li>status</li>
 * </ul>
 */
public class EventListQuery extends ResourceListQueryImpl {

  public static final String FILTER_SERIES_NAME = "series";
  private static final String FILTER_SERIES_LABEL = "FILTERS.EVENTS.SERIES.LABEL";

  public static final String FILTER_PRESENTERS_NAME = "presenters";
  private static final String FILTER_PRESENTERS_LABEL = "FILTERS.EVENTS.PRESENTERS.LABEL";

  public static final String FILTER_CONTRIBUTORS_NAME = "contributors";
  private static final String FILTER_CONTRIBUTORS_LABEL = "FILTERS.EVENTS.CONTRIBUTORS.LABEL";

  public static final String FILTER_LOCATION_NAME = "location";
  private static final String FILTER_LOCATION_LABEL = "FILTERS.EVENTS.LOCATION.LABEL";

  public static final String FILTER_LANGUAGE_NAME = "language";
  private static final String FILTER_LANGUAGE_LABEL = "FILTERS.EVENTS.LANGUAGE.LABEL";

  public static final String FILTER_STARTDATE_NAME = "startDate";
  private static final String FILTER_STARTDATE_LABEL = "FILTERS.EVENTS.START_DATE.LABEL";

  public static final String FILTER_STATUS_NAME = "status";
  private static final String FILTER_STATUS_LABEL = "FILTERS.EVENTS.STATUS.LABEL";

  public static final String FILTER_COMMENTS_NAME = "comments";
  private static final String FILTER_COMMENTS_LABEL = "FILTERS.EVENTS.COMMENTS.LABEL";

  public static final String FILTER_TEXT_NAME = "textFilter";

  public EventListQuery() {
    super();
    this.availableFilters.add(createSeriesFilter(Option.<String> none()));
    this.availableFilters.add(createPresentersFilter(Option.<String> none()));
    this.availableFilters.add(createContributorsFilter(Option.<String> none()));
    this.availableFilters.add(createLocationFilter(Option.<String> none()));
    this.availableFilters.add(createStartDateFilter(Option.<Tuple<Date, Date>> none()));
    this.availableFilters.add(createStatusFilter(Option.<String> none()));
    this.availableFilters.add(createCommentsFilter(Option.<String> none()));
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
    return this.getFilterValue(FILTER_PRESENTERS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given contributor
   *
   * @param presenter
   *          the presenter to filter for
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
   * Create a new {@link ResourceListFilter} based on a presenter
   *
   * @param presenter
   *          the presenters to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a presenters based query
   */
  public static ResourceListFilter<String> createPresentersFilter(Option<String> presenter) {
    return FiltersUtils.generateFilter(presenter, FILTER_PRESENTERS_NAME, FILTER_PRESENTERS_LABEL, SourceType.SELECT,
            Option.some(UsersListProvider.NAME));
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
            SourceType.SELECT, Option.some(UsersListProvider.NAME));
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
   * Create a new {@link ResourceListFilter} based on a language
   *
   * @param language
   *          the language to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a language based query
   */
  public static ResourceListFilter<String> createLanguageFilter(Option<String> language) {
    return FiltersUtils.generateFilter(language, FILTER_LANGUAGE_NAME, FILTER_LANGUAGE_LABEL, SourceType.SELECT,
            Option.some(LanguagesListProvider.DEFAULT));
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

}
