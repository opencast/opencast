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

package org.opencastproject.elasticsearch.index;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchMetadata;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.impl.AbstractElasticsearchIndex;
import org.opencastproject.elasticsearch.impl.ElasticsearchDocument;
import org.opencastproject.elasticsearch.impl.SearchMetadataCollection;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventIndexUtils;
import org.opencastproject.elasticsearch.index.objects.event.EventQueryBuilder;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.objects.series.Series;
import org.opencastproject.elasticsearch.index.objects.series.SeriesIndexUtils;
import org.opencastproject.elasticsearch.index.objects.series.SeriesQueryBuilder;
import org.opencastproject.elasticsearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.elasticsearch.index.objects.theme.IndexTheme;
import org.opencastproject.elasticsearch.index.objects.theme.ThemeQueryBuilder;
import org.opencastproject.elasticsearch.index.objects.theme.ThemeSearchQuery;
import org.opencastproject.security.api.User;

import com.google.common.util.concurrent.Striped;

import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import javax.xml.bind.Unmarshaller;

/**
 * An index implementation based on ElasticSearch that serves the Admin UI API and the External API with data
 * aggregated from multiple services.
 */
@Component(
        property = {
                "service.description=Elasticsearch Index"
        },
        immediate = true,
        service = { ElasticsearchIndex.class }
)
public class ElasticsearchIndex extends AbstractElasticsearchIndex {

  /** Retry configuration */
  private int maxRetryAttemptsGet;
  private static final String MAX_RETRY_ATTEMPTS_GET_PROPERTY = "max.retry.attempts.get";
  private static final int DEFAULT_MAX_RETRY_ATTEMPTS_GET = 0;

  private int maxRetryAttemptsUpdate;
  private static final String MAX_RETRY_ATTEMPTS_UPDATE_PROPERTY = "max.retry.attempts.update";
  private static final int DEFAULT_MAX_RETRY_ATTEMPTS_UPDATE = 0;

  private int retryWaitingPeriodGet;
  private static final String RETRY_WAITING_PERIOD_GET_PROPERTY = "retry.waiting.period.get";
  private static final int DEFAULT_RETRY_WAITING_PERIOD_GET = 1000;

  private int retryWaitingPeriodUpdate;
  private static final String RETRY_WAITING_PERIOD_UPDATE_PROPERTY = "retry.waiting.period.update";
  private static final int DEFAULT_RETRY_WAITING_PERIOD_UPDATE = 1000;

  /** The required index version */
  private static final int INDEX_VERSION = 101;

  /** The document types */
  private static final String VERSION_DOCUMENT_TYPE = "version";

  private static final String[] DOCUMENT_TYPES = new String[] {
      Event.DOCUMENT_TYPE,
      Series.DOCUMENT_TYPE,
      IndexTheme.DOCUMENT_TYPE,
      VERSION_DOCUMENT_TYPE
  };

  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndex.class);

  private final Striped<Lock> locks = Striped.lazyWeakLock(1024);

  /**
   * OSGi callback to activate this component instance.
   *
   * @param bundleContext
   *          The bundle context
   * @param properties
   *          The configuration
   * @throws ComponentException
   *          If the search index cannot be initialized
   */
  @Activate
  public void activate(BundleContext bundleContext, Map<String, Object> properties) throws ComponentException {
    super.activate(properties, bundleContext);
    modified(properties);

    try {
      init(INDEX_VERSION);
    } catch (Throwable t) {
      throw new ComponentException("Error initializing elastic search index", t);
    }
  }

  /**
   * OSGi callback to deactivate this component.
   *
   * @throws IOException
   *          If closing the index fails
   */
  @Deactivate
  public void deactivate() throws IOException {
    close();
  }

  /**
   * OSGi callback for configuration changes.
   *
   * @param properties
   *          The configuration
   */
  @Modified
  public void modified(Map<String, Object> properties) {
    super.modified(properties);

    maxRetryAttemptsGet = NumberUtils.toInt((String) properties.get(MAX_RETRY_ATTEMPTS_GET_PROPERTY),
            DEFAULT_MAX_RETRY_ATTEMPTS_GET);
    retryWaitingPeriodGet = NumberUtils.toInt((String) properties.get(RETRY_WAITING_PERIOD_GET_PROPERTY),
            DEFAULT_RETRY_WAITING_PERIOD_GET);
    logger.info("Max retry attempts for get requests set to {}, timeout set to {} ms.", maxRetryAttemptsGet,
            retryWaitingPeriodGet);

    maxRetryAttemptsUpdate = NumberUtils.toInt((String) properties.get(MAX_RETRY_ATTEMPTS_UPDATE_PROPERTY),
            DEFAULT_MAX_RETRY_ATTEMPTS_UPDATE);
    retryWaitingPeriodUpdate = NumberUtils.toInt((String) properties.get(RETRY_WAITING_PERIOD_UPDATE_PROPERTY),
            DEFAULT_RETRY_WAITING_PERIOD_UPDATE);
    logger.info("Max retry attempts for update requests set to {}, timeout set to {} ms.", maxRetryAttemptsUpdate,
            retryWaitingPeriodUpdate);

    if (maxRetryAttemptsGet < 0 || maxRetryAttemptsUpdate < 0 || retryWaitingPeriodGet < 0
            || retryWaitingPeriodUpdate < 0) {
      logger.warn("You have configured negative values for max attempts or retry periods. Is this intended? This is "
              + "equivalent to setting those values to 0.");
    }
  }

  /**
   * @see AbstractElasticsearchIndex#getDocumentTypes()
   */
  @Override
  public String[] getDocumentTypes() {
    return DOCUMENT_TYPES;
  }

  /*
   * Get index objects
   */

  /**
   * Loads the event from the search index if it exists.
   *
   * @param mediaPackageId
   *          The media package identifier
   * @param organization
   *          The organization
   * @param user
   *          The user
   * @return the event (optional)
   *
   * @throws SearchIndexException
   *          If querying the search index fails
   * @throws IllegalStateException
   *          If multiple events with the same identifier are found
   */
  public Optional<Event> getEvent(String mediaPackageId, String organization, User user) throws SearchIndexException {
    return getEvent(mediaPackageId, organization, user, maxRetryAttemptsGet, retryWaitingPeriodGet);
  }

  /**
   * Loads the event from the search index if it exists.
   *
   * @param mediaPackageId
   *          The media package identifier
   * @param organization
   *          The organization
   * @param user
   *          The user
   * @param maxRetryAttempts
   *          How often to retry query in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @return the event (optional)
   *
   * @throws SearchIndexException
   *           If querying the search index fails
   * @throws IllegalStateException
   *           If multiple events with the same identifier are found
   */
  private Optional<Event> getEvent(String mediaPackageId, String organization, User user, int maxRetryAttempts,
          int retryWaitingPeriod) throws SearchIndexException {
    EventSearchQuery query = new EventSearchQuery(organization, user).withoutActions().withIdentifier(mediaPackageId);
    SearchResult<Event> searchResult = getByQuery(query, maxRetryAttempts, retryWaitingPeriod);
    if (searchResult.getDocumentCount() == 0) {
      return Optional.empty();
    } else if (searchResult.getDocumentCount() == 1) {
      return Optional.of(searchResult.getItems()[0].getSource());
    } else {
      throw new IllegalStateException(
              "Multiple events with identifier " + mediaPackageId + " found in search index");
    }
  }

  /**
   * Loads the series from the search index if it exists.
   *
   * @param seriesId
   *          The series identifier
   * @param organization
   *          The organization
   * @param user
   *          The user
   * @return the series (optional)
   *
   * @throws SearchIndexException
   *          If querying the search index fails
   * @throws IllegalStateException
   *          If multiple series with the same identifier are found
   */
  public Optional<Series> getSeries(String seriesId, String organization, User user)
          throws SearchIndexException {
    return getSeries(seriesId, organization, user, maxRetryAttemptsGet, retryWaitingPeriodGet);
  }

  /**
   * Loads the series from the search index if it exists.
   *
   * @param seriesId
   *          The series identifier
   * @param organization
   *          The organization
   * @param user
   *          The user
   * @param maxRetryAttempts
   *          How often to retry query in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @return the series (optional)
   *
   * @throws SearchIndexException
   *           If querying the search index fails
   * @throws IllegalStateException
   *           If multiple series with the same identifier are found
   */
  private Optional<Series> getSeries(String seriesId, String organization, User user, int maxRetryAttempts,
          int retryWaitingPeriod) throws SearchIndexException {
    SeriesSearchQuery query = new SeriesSearchQuery(organization, user).withoutActions().withIdentifier(seriesId);
    SearchResult<Series> searchResult = getByQuery(query, maxRetryAttempts, retryWaitingPeriod);
    if (searchResult.getDocumentCount() == 0) {
      return Optional.empty();
    } else if (searchResult.getDocumentCount() == 1) {
      return Optional.of(searchResult.getItems()[0].getSource());
    } else {
      throw new IllegalStateException("Multiple series with identifier " + seriesId + " found in search index");
    }
  }

  /**
   * Loads the theme from the search index if it exists.
   *
   * @param themeId
   *          The theme identifier
   * @param organization
   *          The organization
   * @param user
   *          The user
   * @return the theme wrapped in an optional
   *
   * @throws SearchIndexException
   *          If querying the search index fails
   * @throws IllegalStateException
   *          If multiple themes with the same identifier are found
   */
  public Optional<IndexTheme> getTheme(long themeId, String organization, User user)
          throws SearchIndexException {
    return getTheme(themeId, organization, user, maxRetryAttemptsGet, retryWaitingPeriodGet);
  }

  /**
   * Loads the theme from the search index if it exists.
   *
   * @param themeId
   *          The theme identifier
   * @param organization
   *          The organization
   * @param user
   *          The user
   * @param maxRetryAttempts
   *          How often to retry query in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @return the theme wrapped in an optional
   *
   * @throws SearchIndexException
   *          If querying the search index fails
   * @throws IllegalStateException
   *          If multiple themes with the same identifier are found
   */
  private Optional<IndexTheme> getTheme(long themeId, String organization, User user, int maxRetryAttempts,
          int retryWaitingPeriod)
          throws SearchIndexException {
    ThemeSearchQuery query = new ThemeSearchQuery(organization, user).withIdentifier(themeId);
    SearchResult<IndexTheme> searchResult = getByQuery(query, maxRetryAttempts, retryWaitingPeriod);
    if (searchResult.getDocumentCount() == 0) {
      return Optional.empty();
    } else if (searchResult.getDocumentCount() == 1) {
      return Optional.of(searchResult.getItems()[0].getSource());
    } else {
      throw new IllegalStateException("Multiple themes with identifier " + themeId + " found in search index");
    }
  }

  /*
   * Add or update index objects
   */

  /**
   * Adds or updates the event in the search index. Uses a locking mechanism to avoid issues like Lost Update.
   *
   * @param id
   *          The id of the event to update
   * @param updateFunction
   *          The function that does the actual updating
   * @param orgId
   *          The organization the event belongs to
   * @param user
   *          The user
   *
   * @throws SearchIndexException
   *          Thrown if unable to update the event.
   */
  public Optional<Event> addOrUpdateEvent(String id, Function<Optional<Event>, Optional<Event>> updateFunction,
          String orgId, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked event '{}'", id);

    try {
      Optional<Event> eventOpt = getEvent(id, orgId, user, maxRetryAttemptsUpdate, retryWaitingPeriodUpdate);
      Optional<Event> updatedEventOpt = updateFunction.apply(eventOpt);

      if (updatedEventOpt.isPresent()) {
        update(updatedEventOpt.get());
      }
      return updatedEventOpt;
    } finally {
      lock.unlock();
      logger.debug("Released locked event '{}'", id);
    }
  }

  /**
   * Adds the recording event to the search index or updates it accordingly if it is there.
   *
   * @param event
   *          The event to update
   *
   * @throws SearchIndexException
   *          If the event cannot be added or updated
   */
  private void update(Event event) throws SearchIndexException {
    logger.debug("Adding event {} to search index", event.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = EventIndexUtils.toSearchMetadata(event);
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);

    try {
      update(maxRetryAttemptsUpdate, retryWaitingPeriodUpdate, doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write event " + event + " to index", t);
    }
  }

  /**
   * Adds the recording events to the search index or updates it accordingly if it is there.
   *
   * @param eventList
   *          The events to update
   *
   * @throws SearchIndexException
   *          If the events cannot be added or updated
   */
  public void bulkEventUpdate(List<Event> eventList) throws SearchIndexException {
    List<ElasticsearchDocument> docs = new ArrayList<>();
    for (Event event: eventList) {
      logger.debug("Adding event {} to search index", event.getIdentifier());
      // Add the resource to the index
      SearchMetadataCollection inputDocument = EventIndexUtils.toSearchMetadata(event);
      List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
      docs.add(new ElasticsearchDocument(inputDocument.getIdentifier(),
              inputDocument.getDocumentType(), resourceMetadata));
    }
    try {
      bulkUpdate(maxRetryAttemptsUpdate, retryWaitingPeriodUpdate, docs);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write events " + eventList + " to index", t);
    }
  }

  /**
   * Adds or updates the series in the search index. Uses a locking mechanism to avoid issues like Lost Update.
   *
   * @param id
   *          The id of the series to add
   * @param updateFunction
   *          The function that does the actual updating
   * @param orgId
   *          The organization the series belongs to
   * @param user
   *          The user
   *
   * @throws SearchIndexException
   *          Thrown if unable to add or update the series.
   */
  public Optional<Series> addOrUpdateSeries(String id, Function<Optional<Series>, Optional<Series>> updateFunction,
          String orgId, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked series '{}'", id);

    try {
      Optional<Series> seriesOpt = getSeries(id, orgId, user, maxRetryAttemptsUpdate, retryWaitingPeriodUpdate);
      Optional<Series> updatedSeriesOpt = updateFunction.apply(seriesOpt);
      if (updatedSeriesOpt.isPresent()) {
        update(updatedSeriesOpt.get());
      }
      return updatedSeriesOpt;
    } finally {
      lock.unlock();
      logger.debug("Released locked series '{}'", id);
    }
  }

  /**
   * Add or update a series in the search index.
   *
   * @param series
   *          The series to update
   *
   * @throws SearchIndexException
   *          If the series cannot be added or updated
   */
  private void update(Series series) throws SearchIndexException {
    logger.debug("Adding series {} to search index", series.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = SeriesIndexUtils.toSearchMetadata(series);
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);

    try {
      update(maxRetryAttemptsUpdate, retryWaitingPeriodUpdate, doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write series " + series + " to index", t);
    }
  }

  /**
   * Add or update a range of series in the search index.
   *
   * @param seriesList
   *          The series to update
   *
   * @throws SearchIndexException
   *          If the series cannot be added or updated
   */
  public void bulkSeriesUpdate(List<Series> seriesList) throws SearchIndexException {
    List<ElasticsearchDocument> docs = new ArrayList<>();
    for (Series series: seriesList) {
      logger.debug("Adding series {} to search index", series.getIdentifier());
      // Add the resource to the index
      SearchMetadataCollection inputDocument = SeriesIndexUtils.toSearchMetadata(series);
      List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
      docs.add(new ElasticsearchDocument(inputDocument.getIdentifier(),
              inputDocument.getDocumentType(), resourceMetadata));
    }
    try {
      bulkUpdate(maxRetryAttemptsUpdate, retryWaitingPeriodUpdate, docs);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write series " + seriesList + " to index", t);
    }
  }

  /**
   * Adds or updates the theme in the search index. Uses a locking mechanism to avoid issues like Lost Update.
   *
   * @param id
   *          The id of the theme to update
   * @param updateFunction
   *          The function that does the actual updating
   * @param orgId
   *          The organization the theme belongs to
   * @param user
   *          The user
   *
   * @throws SearchIndexException
   *          Thrown if unable to update the theme.
   */
  public Optional<IndexTheme> addOrUpdateTheme(long id, Function<Optional<IndexTheme>,
          Optional<IndexTheme>> updateFunction, String orgId, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked theme '{}'", id);

    try {
      Optional<IndexTheme> themeOpt = getTheme(id, orgId, user, maxRetryAttemptsUpdate, retryWaitingPeriodUpdate);
      Optional<IndexTheme> updatedThemeOpt = updateFunction.apply(themeOpt);
      if (updatedThemeOpt.isPresent()) {
        update(updatedThemeOpt.get());
      }
      return updatedThemeOpt;
    } finally {
      lock.unlock();
      logger.debug("Released locked theme '{}'", id);
    }
  }

  /**
   * Adds or updates the theme in the search index.
   *
   * @param theme
   *          The theme to update
   *
   * @throws SearchIndexException
   *          Thrown if unable to add or update the theme.
   */
  private void update(IndexTheme theme) throws SearchIndexException {
    logger.debug("Adding theme {} to search index", theme.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = theme.toSearchMetadata();
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);

    try {
      update(maxRetryAttemptsUpdate, retryWaitingPeriodUpdate, doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write theme " + theme + " to index", t);
    }
  }

  /**
   * Adds or updates the themes in the search index.
   *
   * @param themeList
   *          The themes to update
   *
   * @throws SearchIndexException
   *          Thrown if unable to add or update the themes.
   */
  public void bulkThemeUpdate(List<IndexTheme> themeList) throws SearchIndexException {
    List<ElasticsearchDocument> docs = new ArrayList<>();
    for (IndexTheme theme: themeList) {
      logger.debug("Adding theme {} to search index", theme.getIdentifier());

      // Add the resource to the index
      SearchMetadataCollection inputDocument = theme.toSearchMetadata();
      List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
      docs.add(new ElasticsearchDocument(inputDocument.getIdentifier(),
              inputDocument.getDocumentType(), resourceMetadata));
    }
    try {
      bulkUpdate(maxRetryAttemptsUpdate, retryWaitingPeriodUpdate, docs);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write themes " + themeList + " to index", t);
    }
  }

  /*
   * Delete index objects
   */

  /**
   * Delete event from index.
   *
   * @param eventId
   *         The event identifier
   * @param orgId
   *         The organization id
   * @return
   *         true if it was deleted, false if it couldn't be found
   * @throws SearchIndexException
   *         If there was an error during deletion
   */
  public boolean deleteEvent(String eventId, String orgId) throws SearchIndexException {
    return delete(Event.DOCUMENT_TYPE, eventId, orgId);
  }

  /**
   * Delete series from index.
   *
   * @param seriesId
   *         The series identifier
   * @param orgId
   *         The organization id
   * @return
   *         true if it was deleted, false if it couldn't be found
   * @throws SearchIndexException
   *         If there was an error during deletion
   */
  public boolean deleteSeries(String seriesId, String orgId) throws SearchIndexException {
    return delete(Series.DOCUMENT_TYPE, seriesId, orgId);
  }

  /**
   * Delete theme from index.
   *
   * @param themeId
   *         The theme identifier
   * @param orgId
   *         The organization id
   * @return
   *         true if it was deleted, false if it couldn't be found
   * @throws SearchIndexException
   *         If there was an error during deletion
   */
  public boolean deleteTheme(String themeId, String orgId) throws SearchIndexException {
    return delete(IndexTheme.DOCUMENT_TYPE, themeId, orgId);
  }

  /**
   * Delete object from this index.
   *
   * @param type
   *         The type of object we want to delete
   * @param id
   *         The identifier of this object
   * @param orgId
   *         The organization id
   * @return
   *         True if it was deleted, false if it couldn't be found
   *
   * @throws SearchIndexException
   *         If deleting from the index fails
   */
  private boolean delete(String type, String id, String orgId) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked {} '{}'.", type, id);
    try {
      String idWithOrgId = id.concat(orgId);
      logger.debug("Removing element with id '{}' from search index '{}'", idWithOrgId, getSubIndexIdentifier(type));

      DeleteResponse deleteResponse = delete(type, idWithOrgId, maxRetryAttemptsUpdate, retryWaitingPeriodUpdate);
      if (deleteResponse.getResult().equals(DocWriteResponse.Result.NOT_FOUND)) {
        logger.trace("Document {} to delete was not found on index '{}'", idWithOrgId, getSubIndexIdentifier(type));
        return false;
      }
    } catch (Throwable e) {
      throw new SearchIndexException("Cannot remove " + type + " " + id + " from index", e);
    } finally {
      lock.unlock();
      logger.debug("Released locked {} '{}'.", type, id);
    }
    return true;
  }

  /*
   * Get index objects by query
   */

  /**
   * @param query
   *          The query to use to retrieve the events that match the query
   * @return {@link SearchResult} collection of {@link Event} from a query.
   *
   * @throws SearchIndexException
   *          Thrown if there is an error getting the results.
   */
  public SearchResult<Event> getByQuery(EventSearchQuery query) throws SearchIndexException {
    return getByQuery(query, maxRetryAttemptsGet, retryWaitingPeriodGet);
  }

  /**
   * @param query
   *          The query to use to retrieve the events that match the query
   * @param maxRetryAttempts
   *          How often to retry query in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @return {@link SearchResult} collection of {@link Event} from a query.
   *
   * @throws SearchIndexException
   *          Thrown if there is an error getting the results.
   */
  private SearchResult<Event> getByQuery(EventSearchQuery query, int maxRetryAttempts, int retryWaitingPeriod)
          throws SearchIndexException {
    logger.debug("Searching index using event query '{}'", query);
    // Create the request
    final SearchRequest searchRequest = getSearchRequest(query, new EventQueryBuilder(query));

    try {
      final Unmarshaller unmarshaller = Event.createUnmarshaller();
      return executeQuery(query, searchRequest, metadata -> {
        try {
          return EventIndexUtils.toRecordingEvent(metadata, unmarshaller);
        } catch (IOException e) {
          return chuck(e);
        }
      }, maxRetryAttempts, retryWaitingPeriod);
    } catch (Throwable t) {
      throw new SearchIndexException("Error querying event index", t);
    }
  }


  /**
   * @param query
   *          The query to use to retrieve the series that match the query
   * @return {@link SearchResult} collection of {@link Series} from a query.
   *
   * @throws SearchIndexException
   *          Thrown if there is an error getting the results.
   */
  public SearchResult<Series> getByQuery(SeriesSearchQuery query) throws SearchIndexException {
    return getByQuery(query, maxRetryAttemptsGet, retryWaitingPeriodGet);
  }

  /**
   * @param query
   *          The query to use to retrieve the series that match the query
   * @param maxRetryAttempts
   *          How often to retry query in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   *
   * @return {@link SearchResult} collection of {@link Series} from a query.
   *
   * @throws SearchIndexException
   *          Thrown if there is an error getting the results.
   */
  private SearchResult<Series> getByQuery(SeriesSearchQuery query, int maxRetryAttempts, int retryWaitingPeriod)
          throws SearchIndexException {
    logger.debug("Searching index using series query '{}'", query);
    // Create the request
    final SearchRequest searchRequest = getSearchRequest(query, new SeriesQueryBuilder(query));
    try {
      final Unmarshaller unmarshaller = Series.createUnmarshaller();
      return executeQuery(query, searchRequest, metadata -> {
        try {
          return SeriesIndexUtils.toSeries(metadata, unmarshaller);
        } catch (IOException e) {
          return chuck(e);
        }
      }, maxRetryAttempts, retryWaitingPeriod);
    } catch (Throwable t) {
      throw new SearchIndexException("Error querying series index", t);
    }
  }

  /**
   * @param query
   *          The query to use to retrieve the themes that match the query
   * @return {@link SearchResult} collection of {@link IndexTheme} from a query.
   *
   * @throws SearchIndexException
   *          Thrown if there is an error getting the results.
   */
  public SearchResult<IndexTheme> getByQuery(ThemeSearchQuery query) throws SearchIndexException {
    return getByQuery(query, maxRetryAttemptsGet, retryWaitingPeriodGet);
  }

  /**
   * @param query
   *          The query to use to retrieve the themes that match the query
   * @param maxRetryAttempts
   *          How often to retry query in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   *
   * @return {@link SearchResult} collection of {@link IndexTheme} from a query.
   *
   * @throws SearchIndexException
   *          Thrown if there is an error getting the results.
   */
  private SearchResult<IndexTheme> getByQuery(ThemeSearchQuery query, int maxRetryAttempts, int retryWaitingPeriod)
          throws SearchIndexException {
    logger.debug("Searching index using theme query '{}'", query);
    // Create the request
    final SearchRequest searchRequest = getSearchRequest(query, new ThemeQueryBuilder(query));

    try {
      return executeQuery(query, searchRequest, metadata -> {
        try {
          return IndexTheme.fromSearchMetadata(metadata);
        } catch (IOException e) {
          return chuck(e);
        }
      }, maxRetryAttempts, retryWaitingPeriod);
    } catch (Throwable t) {
      throw new SearchIndexException("Error querying theme index", t);
    }
  }
}
