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

package org.opencastproject.opensearch.index;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.opensearch.api.SearchIndexException;
import org.opencastproject.opensearch.api.SearchMetadata;
import org.opencastproject.opensearch.api.SearchResult;
import org.opencastproject.opensearch.impl.AbstractOpenSearchIndex;
import org.opencastproject.opensearch.impl.AbstractOpenSearchQueryBuilder;
import org.opencastproject.opensearch.impl.OpenSearchDocument;
import org.opencastproject.opensearch.impl.SearchMetadataCollection;
import org.opencastproject.opensearch.index.objects.event.Event;
import org.opencastproject.opensearch.index.objects.event.EventIndexSchema;
import org.opencastproject.opensearch.index.objects.event.EventIndexUtils;
import org.opencastproject.opensearch.index.objects.event.EventQueryBuilder;
import org.opencastproject.opensearch.index.objects.event.EventSearchQuery;
import org.opencastproject.opensearch.index.objects.series.Series;
import org.opencastproject.opensearch.index.objects.series.SeriesIndexSchema;
import org.opencastproject.opensearch.index.objects.series.SeriesIndexUtils;
import org.opencastproject.opensearch.index.objects.series.SeriesQueryBuilder;
import org.opencastproject.opensearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;

import com.google.common.util.concurrent.Striped;

import org.apache.commons.lang3.math.NumberUtils;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.core.rest.RestStatus;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.Unmarshaller;

/**
 * An index implementation based on OpenSearch that serves the Admin UI API and the External API with data
 * aggregated from multiple services.
 */
@Component(
        property = {
                "service.description=OpenSearch Index"
        },
        service = { OpenSearchIndex.class }
)
public class OpenSearchIndex extends AbstractOpenSearchIndex {

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

  /** Score boosts of fields when searching events or series by text, by field name */
  private volatile Map<String, Float> eventFieldBoosts = AbstractOpenSearchQueryBuilder.DEFAULT_FIELD_BOOSTS;
  private static final String EVENT_BOOST_PROPERTY = "search.boost.event";

  private volatile Map<String, Float> seriesFieldBoosts = AbstractOpenSearchQueryBuilder.DEFAULT_FIELD_BOOSTS;
  private static final String SERIES_BOOST_PROPERTY = "search.boost.series";

  /** Prefix of the fields of extended metadata, which depend on the configuration */
  private static final String EXTENDED_METADATA_PREFIX = "extended_metadata_";

  /** Matches an entry of the boost configuration: field^boost */
  private static final Pattern BOOST_ENTRY = Pattern.compile("([\\w./-]+)\\s*\\^\\s*(.+)");

  /** The required index version */
  private static final int INDEX_VERSION = 1;

  private static final String[] DOCUMENT_TYPES = new String[] {
      // Version document type should be processed first.
      // It will be used as storage for index versions of all other indexes.
      VERSION_DOCUMENT_TYPE,
      Event.DOCUMENT_TYPE,
      Series.DOCUMENT_TYPE
  };

  private static final Logger logger = LoggerFactory.getLogger(OpenSearchIndex.class);

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
      throw new ComponentException("Error initializing OpenSearch index", t);
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

    eventFieldBoosts = parseFieldBoosts(properties.get(EVENT_BOOST_PROPERTY), EVENT_BOOST_PROPERTY,
            schemaFields(EventIndexSchema.class));
    seriesFieldBoosts = parseFieldBoosts(properties.get(SERIES_BOOST_PROPERTY), SERIES_BOOST_PROPERTY,
            schemaFields(SeriesIndexSchema.class));
    logger.info("Field boosts for text searches set to {} for events and {} for series.", eventFieldBoosts,
            seriesFieldBoosts);

    if (maxRetryAttemptsGet < 0 || maxRetryAttemptsUpdate < 0 || retryWaitingPeriodGet < 0
            || retryWaitingPeriodUpdate < 0) {
      logger.warn("You have configured negative values for max attempts or retry periods. Is this intended? This is "
              + "equivalent to setting those values to 0.");
    }
  }

  /**
   * Reads a comma separated list of <code>field^boost</code> entries. Invalid entries are logged and ignored, unknown
   * fields are logged but kept, as the extended metadata fields depend on the configuration. Returns the default if
   * the value is not set, and no boosts if it is empty.
   */
  static Map<String, Float> parseFieldBoosts(Object value, String property, Set<String> knownFields) {
    if (value == null) {
      return AbstractOpenSearchQueryBuilder.DEFAULT_FIELD_BOOSTS;
    }

    Map<String, Float> boosts = new LinkedHashMap<>();
    for (String listed : value.toString().split(",")) {
      String entry = listed.trim();
      if (entry.isEmpty()) {
        continue;
      }
      Matcher matcher = BOOST_ENTRY.matcher(entry);
      float boost = Float.NaN;
      if (matcher.matches()) {
        boost = NumberUtils.toFloat(matcher.group(2).trim(), Float.NaN);
      }
      if (Float.isNaN(boost) || Float.isInfinite(boost) || boost <= 0) {
        logger.warn("Ignoring invalid entry '{}' of {}, it has to be of the form field^boost with a boost greater "
                + "than 0, e.g. title^3.0.", entry, property);
        continue;
      }
      String field = matcher.group(1);
      if (!knownFields.contains(field) && !field.startsWith(EXTENDED_METADATA_PREFIX)) {
        logger.warn("The field '{}' of {} is not a field of the index, the boost has no effect if no such field "
                + "exists. Known fields are: {}.", field, property, new TreeSet<>(knownFields));
      }
      if (boosts.put(field, boost) != null) {
        logger.warn("The field '{}' is listed more than once in {}, using the last boost of {}.", field, property,
                boost);
      }
    }
    return Collections.unmodifiableMap(boosts);
  }

  /** Returns the names of the fields defined by an index schema. */
  static Set<String> schemaFields(Class<?> schema) {
    Set<String> fields = new HashSet<>();
    for (Field field : schema.getFields()) {
      if (field.getType() == String.class && Modifier.isStatic(field.getModifiers())) {
        try {
          fields.add((String) field.get(null));
        } catch (IllegalAccessException e) {
          logger.debug("Could not read the field name from {}", field, e);
        }
      }
    }
    return fields;
  }

  /**
   * @see AbstractOpenSearchIndex#getDocumentTypes()
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
  public Optional<Event> getEvent(String mediaPackageId, Organization organization, User user)
          throws SearchIndexException {
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
   *          How often to retry query in case of OpenSearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @return the event (optional)
   *
   * @throws SearchIndexException
   *           If querying the search index fails
   * @throws IllegalStateException
   *           If multiple events with the same identifier are found
   */
  private Optional<Event> getEvent(String mediaPackageId, Organization organization, User user, int maxRetryAttempts,
          int retryWaitingPeriod) throws SearchIndexException {
    EventSearchQuery query = new EventSearchQuery(organization.getId(), user)
        .withoutActions()
        .withIdentifier(mediaPackageId);
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
  public Optional<Series> getSeries(String seriesId, Organization organization, User user)
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
   *          How often to retry query in case of OpenSearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @return the series (optional)
   *
   * @throws SearchIndexException
   *           If querying the search index fails
   * @throws IllegalStateException
   *           If multiple series with the same identifier are found
   */
  private Optional<Series> getSeries(String seriesId, Organization organization, User user, int maxRetryAttempts,
          int retryWaitingPeriod) throws SearchIndexException {
    SeriesSearchQuery query = new SeriesSearchQuery(organization.getId(), user)
        .withoutActions()
        .withIdentifier(seriesId);
    SearchResult<Series> searchResult = getByQuery(query, maxRetryAttempts, retryWaitingPeriod);
    if (searchResult.getDocumentCount() == 0) {
      return Optional.empty();
    } else if (searchResult.getDocumentCount() == 1) {
      return Optional.of(searchResult.getItems()[0].getSource());
    } else {
      throw new IllegalStateException("Multiple series with identifier " + seriesId + " found in search index");
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
   * @param organization
   *          The organization the event belongs to
   * @param user
   *          The user
   *
   * @throws SearchIndexException
   *          Thrown if unable to update the event.
   */
  public Optional<Event> addOrUpdateEvent(String id, Function<Optional<Event>, Optional<Event>> updateFunction,
          Organization organization, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked event '{}'", id);

    try {
      Optional<Event> eventOpt = getEvent(id, organization, user, maxRetryAttemptsUpdate, retryWaitingPeriodUpdate);
      Optional<Event> updatedEventOpt = updateFunction.apply(eventOpt);

      if (updatedEventOpt.isPresent()) {
        update(updatedEventOpt.get(), organization);
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
  private void update(Event event, Organization organization) throws SearchIndexException {
    logger.debug("Adding event {} to search index", event.getIdentifier());



    // Add the resource to the index
    SearchMetadataCollection inputDocument = EventIndexUtils.toSearchMetadata(
        event,
        SecurityUtil.additionalAclActions(organization)
    );
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    OpenSearchDocument doc = new OpenSearchDocument(inputDocument.getIdentifier(),
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
  public void bulkEventUpdate(List<Event> eventList, Organization organization) throws SearchIndexException {
    List<OpenSearchDocument> docs = new ArrayList<>();
    for (Event event: eventList) {
      logger.debug("Adding event {} to search index", event.getIdentifier());
      // Add the resource to the index
      SearchMetadataCollection inputDocument = EventIndexUtils.toSearchMetadata(
          event,
          SecurityUtil.additionalAclActions(organization)
      );
      List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
      docs.add(new OpenSearchDocument(inputDocument.getIdentifier(),
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
   * @param organization
   *          The organization the series belongs to
   * @param user
   *          The user
   *
   * @throws SearchIndexException
   *          Thrown if unable to add or update the series.
   */
  public Optional<Series> addOrUpdateSeries(String id, Function<Optional<Series>, Optional<Series>> updateFunction,
          Organization organization, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked series '{}'", id);

    try {
      Optional<Series> seriesOpt = getSeries(id, organization, user, maxRetryAttemptsUpdate, retryWaitingPeriodUpdate);
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
    OpenSearchDocument doc = new OpenSearchDocument(inputDocument.getIdentifier(),
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
    List<OpenSearchDocument> docs = new ArrayList<>();
    for (Series series: seriesList) {
      logger.debug("Adding series {} to search index", series.getIdentifier());
      // Add the resource to the index
      SearchMetadataCollection inputDocument = SeriesIndexUtils.toSearchMetadata(series);
      List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
      docs.add(new OpenSearchDocument(inputDocument.getIdentifier(),
              inputDocument.getDocumentType(), resourceMetadata));
    }
    try {
      bulkUpdate(maxRetryAttemptsUpdate, retryWaitingPeriodUpdate, docs);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write series " + seriesList + " to index", t);
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
   * @param organization
   *         The organization id
   * @return
   *         true if it was deleted, false if it couldn't be found
   * @throws SearchIndexException
   *         If there was an error during deletion
   */
  public boolean deleteEvent(String eventId, Organization organization) throws SearchIndexException {
    return delete(Event.DOCUMENT_TYPE, eventId, organization);
  }

  /**
   * Delete series from index.
   *
   * @param seriesId
   *         The series identifier
   * @param organization
   *         The organization id
   * @return
   *         true if it was deleted, false if it couldn't be found
   * @throws SearchIndexException
   *         If there was an error during deletion
   */
  public boolean deleteSeries(String seriesId, Organization organization) throws SearchIndexException {
    return delete(Series.DOCUMENT_TYPE, seriesId, organization);
  }

  /**
   * Delete object from this index.
   *
   * @param type
   *         The type of object we want to delete
   * @param id
   *         The identifier of this object
   * @param organization
   *         The organization id
   * @return
   *         True if it was deleted, false if it couldn't be found
   *
   * @throws SearchIndexException
   *         If deleting from the index fails
   */
  private boolean delete(String type, String id, Organization organization) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked {} '{}'.", type, id);
    try {
      String idWithOrgId = id.concat(organization.getId());
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
   *          How often to retry query in case of OpenSearchStatusException
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
    final SearchRequest searchRequest = getSearchRequest(query, new EventQueryBuilder(query, eventFieldBoosts));

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
      throw new SearchIndexException("Error querying event index", t, isBadRequest(t));
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
   *          How often to retry query in case of OpenSearchStatusException
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
    final SearchRequest searchRequest = getSearchRequest(query, new SeriesQueryBuilder(query, seriesFieldBoosts));
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
      throw new SearchIndexException("Error querying series index", t, isBadRequest(t));
    }
  }

  /**
   * Whether the given failure means the query itself was rejected as malformed, rather than indicating an
   * operational problem such as a missing index or an unreachable search service.
   */
  private static boolean isBadRequest(Throwable t) {
    return t instanceof OpenSearchStatusException
            && ((OpenSearchStatusException) t).status() == RestStatus.BAD_REQUEST;
  }
}
