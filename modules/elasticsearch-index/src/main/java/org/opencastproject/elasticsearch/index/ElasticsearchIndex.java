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
import org.opencastproject.util.NotFoundException;

import com.google.common.util.concurrent.Striped;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
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

  /** The name of this index */
  private static final String INDEX_NAME_PROPERTY = "index.name";
  private static final String DEFAULT_INDEX_NAME = "opencast";

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
   * @param ctx
   *          the component context
   * @throws ComponentException
   *           if the search index cannot be initialized
   */
  @Activate
  public void activate(ComponentContext ctx) throws ComponentException {
    super.activate(ctx);

    String indexName = StringUtils.defaultIfBlank(Objects.toString(ctx.getProperties().get(INDEX_NAME_PROPERTY)),
            DEFAULT_INDEX_NAME);
    try {
      init(indexName, INDEX_VERSION);
    } catch (Throwable t) {
      throw new ComponentException("Error initializing elastic search index", t);
    }
  }

  /**
   * OSGi callback to deactivate this component.
   *
   * @param ctx
   *          the component context
   * @throws IOException
   */
  @Deactivate
  public void deactivate(ComponentContext ctx) throws IOException {
    close();
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
   * @param mediapackageId
   *          the mediapackage identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @return the event (optional)
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple events with the same identifier are found
   */
  public Optional<Event> getEvent(String mediapackageId, String organization, User user) throws SearchIndexException {
    EventSearchQuery query = new EventSearchQuery(organization, user).withoutActions().withIdentifier(mediapackageId);
    SearchResult<Event> searchResult = getByQuery(query);
    if (searchResult.getDocumentCount() == 0) {
      return Optional.empty();
    } else if (searchResult.getDocumentCount() == 1) {
      return Optional.of(searchResult.getItems()[0].getSource());
    } else {
      throw new IllegalStateException(
              "Multiple events with identifier " + mediapackageId + " found in search index");
    }
  }

  /**
   * Loads the series from the search index if it exists.
   *
   * @param seriesId
   *          the series identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @return the series (optional)
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple series with the same identifier are found
   */
  public Optional<Series> getSeries(String seriesId, String organization, User user)
          throws SearchIndexException {
    SeriesSearchQuery query = new SeriesSearchQuery(organization, user).withoutActions().withIdentifier(seriesId);
    SearchResult<Series> searchResult = getByQuery(query);
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
   *          the theme identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @return the theme wrapped in an optional
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple themes with the same identifier are found
   */
  protected Optional<IndexTheme> getTheme(long themeId, String organization, User user)
          throws SearchIndexException {
    ThemeSearchQuery query = new ThemeSearchQuery(organization, user).withIdentifier(themeId);
    SearchResult<IndexTheme> searchResult = getByQuery(query);
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
   *           the organization the event belongs to
   * @param user
   *           the user
   * @throws SearchIndexException
   *           Thrown if unable to update the event.
   */
  public Optional<Event> addOrUpdateEvent(String id, Function<Optional<Event>, Optional<Event>> updateFunction,
          String orgId, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked event '{}'", id);

    try {
      Optional<Event> eventOpt = getEvent(id, orgId, user);
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
   *          the recording event
   * @throws SearchIndexException
   *           if the event cannot be added or updated
   */
  private void update(Event event) throws SearchIndexException {
    logger.debug("Adding event {} to search index", event.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = EventIndexUtils.toSearchMetadata(event);
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);
    try {
      update(doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write resource " + event + " to index", t);
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
   *           the organization the series belongs to
   * @param user
   *           the user
   * @throws SearchIndexException
   *           Thrown if unable to add or update the series.
   */
  public Optional<Series> addOrUpdateSeries(String id, Function<Optional<Series>, Optional<Series>> updateFunction,
          String orgId, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked series '{}'", id);

    try {
      Optional<Series> seriesOpt = getSeries(id, orgId, user);
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
   * @throws SearchIndexException
   */
  private void update(Series series) throws SearchIndexException {
    logger.debug("Adding series {} to search index", series.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = SeriesIndexUtils.toSearchMetadata(series);
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);
    try {
      update(doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write resource " + series + " to index", t);
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
   *           the organization the theme belongs to
   * @param user
   *           the user
   * @throws SearchIndexException
   *           Thrown if unable to update the theme.
   */
  public Optional<IndexTheme> addOrUpdateTheme(long id, Function<Optional<IndexTheme>,
          Optional<IndexTheme>> updateFunction, String orgId, User user) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked theme '{}'", id);

    try {
      Optional<IndexTheme> themeOpt = getTheme(id, orgId, user);
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
   *          The theme to add
   * @throws SearchIndexException
   *           Thrown if unable to add or update the theme.
   */
  private void update(IndexTheme theme) throws SearchIndexException {
    logger.debug("Adding theme {} to search index", theme.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = theme.toSearchMetadata();
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);
    try {
      update(doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write resource " + theme + " to index", t);
    }
  }

  /*
   * Delete index objects
   */

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
   *         true if it was deleted, false if it couldn't be found
   *
   * @throws SearchIndexException
   */
  public boolean delete(String type, String id, String orgId) throws SearchIndexException {
    final Lock lock = this.locks.get(id);
    lock.lock();
    logger.debug("Locked {} '{}'.", type, id);
    try {
      String idWithOrgId = id.concat(orgId);
      logger.debug("Removing element with id '{}' from search index '{}'", idWithOrgId, getIndexName(type));
      final DeleteRequest deleteRequest = new DeleteRequest(getIndexName(type), idWithOrgId)
              .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

      final DeleteResponse delete = getClient().delete(deleteRequest, RequestOptions.DEFAULT);
      if (delete.getResult().equals(DocWriteResponse.Result.NOT_FOUND)) {
        logger.trace("Document {} to delete was not found on index '{}'", idWithOrgId, getIndexName(type));
        return false;
      }
    } catch (IOException e) {
      throw new SearchIndexException(e);
    } finally {
      lock.unlock();
      logger.debug("Released locked {} '{}'.", type, id);
    }
    return true;
  }

  /**
   * @param event
   *          The event to check if it
   * @return If an event has a record of being a schedule, workflow or archive event.
   */
  private boolean toDelete(Event event) {
    boolean hasScheduling = event.isScheduledEvent();
    boolean hasWorkflow = event.getWorkflowId() != null;
    boolean hasArchive = event.getArchiveVersion() != null;
    return !hasScheduling && !hasWorkflow && !hasArchive;
  }

  /**
   * Delete an event from the asset manager.
   *
   * @param organization
   *          The organization the event is a part of.
   * @param user
   *          The user that is requesting to delete the event.
   * @param uid
   *          The identifier of the event.
   * @throws SearchIndexException
   *           Thrown if there is an issue with deleting the event.
   * @throws NotFoundException
   *           Thrown if the event cannot be found.
   */
  public void deleteAssets(String organization, User user, String uid) throws SearchIndexException, NotFoundException {
    Optional<Event> eventOpt = getEvent(uid, organization, user);
    if (!eventOpt.isPresent()) {
      throw new NotFoundException("No event with id " + uid + " found.");
    }
    Event event = eventOpt.get();
    event.setArchiveVersion(null);

    if (toDelete(event)) {
      delete(Event.DOCUMENT_TYPE, uid, organization);
    } else {
      update(event);
    }
  }

  /**
   * Delete an event from the scheduling service
   *
   * @param organization
   *          The organization the event is a part of.
   * @param user
   *          The user that is requesting to delete the event.
   * @param uid
   *          The identifier of the event.
   * @throws SearchIndexException
   *           Thrown if there is an issue with deleting the event.
   * @throws NotFoundException
   *           Thrown if the event cannot be found.
   */
  public void deleteScheduling(String organization, User user, String uid)
          throws SearchIndexException, NotFoundException {
    Optional<Event> eventOpt = getEvent(uid, organization, user);
    if (!eventOpt.isPresent()) {
      throw new NotFoundException("No event with id " + uid + " found.");
    }
    Event event = eventOpt.get();
    event.setAgentId(null);

    if (toDelete(event)) {
      delete(Event.DOCUMENT_TYPE, uid, organization);
    } else {
      update(event);
    }
  }

  /**
   * Delete an event from the workflow service
   *
   * @param organization
   *          The organization the event is a part of.
   * @param user
   *          The user that is requesting to delete the event.
   * @param uid
   *          The identifier of the event.
   * @param workflowId
   *          The identifier of the workflow.
   * @throws SearchIndexException
   *           Thrown if there is an issue with deleting the event.
   * @throws NotFoundException
   *           Thrown if the event cannot be found.
   */
  public void deleteWorkflow(String organization, User user, String uid, Long workflowId)
          throws SearchIndexException, NotFoundException {
    Optional<Event> eventOpt = getEvent(uid, organization, user);
    if (!eventOpt.isPresent()) {
      throw new NotFoundException("No event with id " + uid + " found.");
    }
    Event event = eventOpt.get();
    if (event.getWorkflowId() != null && event.getWorkflowId().equals(workflowId)) {
      logger.debug("Workflow {} is the current workflow of event {}. Removing it from event.", uid, workflowId);
      event.setWorkflowId(null);
      event.setWorkflowDefinitionId(null);
      event.setWorkflowState(null);
    }

    if (toDelete(event)) {
      delete(Event.DOCUMENT_TYPE, uid, organization);
    } else {
      update(event);
    }
  }

  /*
   * Get index objects by query
   */

  /**
   * @param query
   *          The query to use to retrieve the events that match the query
   * @return {@link SearchResult} collection of {@link Event} from a query.
   * @throws SearchIndexException
   *           Thrown if there is an error getting the results.
   */
  public SearchResult<Event> getByQuery(EventSearchQuery query) throws SearchIndexException {
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
      });
    } catch (Throwable t) {
      throw new SearchIndexException("Error querying event index", t);
    }
  }

  /**
   * @param query
   *          The query to use to retrieve the series that match the query
   * @return {@link SearchResult} collection of {@link Series} from a query.
   * @throws SearchIndexException
   *           Thrown if there is an error getting the results.
   */
  public SearchResult<Series> getByQuery(SeriesSearchQuery query) throws SearchIndexException {
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
      });
    } catch (Throwable t) {
      throw new SearchIndexException("Error querying series index", t);
    }
  }

  /**
   * @param query
   *          The query to use to retrieve the themes that match the query
   * @return {@link SearchResult} collection of {@link IndexTheme} from a query.
   * @throws SearchIndexException
   *           Thrown if there is an error getting the results.
   */
  public SearchResult<IndexTheme> getByQuery(ThemeSearchQuery query) throws SearchIndexException {
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
      });
    } catch (Throwable t) {
      throw new SearchIndexException("Error querying theme index", t);
    }
  }
}
