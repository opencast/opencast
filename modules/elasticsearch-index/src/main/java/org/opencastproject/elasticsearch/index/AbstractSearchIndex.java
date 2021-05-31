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
import org.opencastproject.elasticsearch.api.SearchQuery;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.impl.AbstractElasticsearchIndex;
import org.opencastproject.elasticsearch.impl.ElasticsearchDocument;
import org.opencastproject.elasticsearch.impl.SearchMetadataCollection;
import org.opencastproject.elasticsearch.impl.SearchMetadataImpl;
import org.opencastproject.elasticsearch.impl.SearchResultImpl;
import org.opencastproject.elasticsearch.impl.SearchResultItemImpl;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventIndexUtils;
import org.opencastproject.elasticsearch.index.event.EventQueryBuilder;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesIndexUtils;
import org.opencastproject.elasticsearch.index.series.SeriesQueryBuilder;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.elasticsearch.index.theme.IndexTheme;
import org.opencastproject.elasticsearch.index.theme.ThemeQueryBuilder;
import org.opencastproject.elasticsearch.index.theme.ThemeSearchQuery;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.google.common.util.concurrent.Striped;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import javax.xml.bind.Unmarshaller;

public abstract class AbstractSearchIndex extends AbstractElasticsearchIndex {

  private static final Logger logger = LoggerFactory.getLogger(AbstractSearchIndex.class);

  private final Striped<Lock> locks = Striped.lazyWeakLock(1024);

  @Override
  public abstract String getIndexName();

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
        addOrUpdate(updatedEventOpt.get());
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
  protected void addOrUpdate(Event event) throws SearchIndexException {
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
        addOrUpdate(updatedSeriesOpt.get());
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
  protected void addOrUpdate(Series series) throws SearchIndexException {
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
        addOrUpdate(updatedThemeOpt.get());
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
  protected void addOrUpdate(IndexTheme theme) throws SearchIndexException {
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
  protected boolean toDelete(Event event) {
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
      addOrUpdate(event);
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
      addOrUpdate(event);
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
      addOrUpdate(event);
    }
  }

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

  /**
   * Returns all the known terms for a field (aka facets).
   *
   * @param field
   *          the field name
   * @param types
   *          an optional array of document types, if none is set all types are searched
   * @return the list of terms
   */
  public List<String> getTermsForField(String field, Option<String[]> types) {
    final String facetName = "terms";
    final AggregationBuilder aggBuilder = AggregationBuilders.terms(facetName).field(field);
    final SearchSourceBuilder searchSource = new SearchSourceBuilder().aggregation(aggBuilder);
    final List<String> indices = new ArrayList<>();
    if (types.isSome()) {
      Arrays.stream(types.get()).forEach(t -> indices.add(this.getIndexName(t)));
    } else {
      Arrays.stream(getDocumentTypes()).forEach(t->indices.add(this.getIndexName(t)));
    }
    final SearchRequest searchRequest = new SearchRequest(indices.toArray(new String[0])).source(searchSource);
    try {
      final SearchResponse response = getClient().search(searchRequest, RequestOptions.DEFAULT);

      final List<String> terms = new ArrayList<>();
      final Terms aggs = response.getAggregations().get(facetName);

      for (Bucket bucket : aggs.getBuckets()) {
        terms.add(bucket.getKey().toString());
      }

      return terms;
    } catch (IOException e) {
      return chuck(e);
    }
  }

  /**
   * Execute a query on the index.
   *
   * @param query
   *          The query to use to find the results
   * @param request
   *          The builder to use to create the query.
   * @param toSearchResult
   *          The function to convert the results to a {@link SearchResult}
   * @return A {@link SearchResult} containing the relevant objects.
   * @throws SearchIndexException
   */
  protected <T> SearchResult<T> executeQuery(SearchQuery query, SearchRequest request,
          Function<SearchMetadataCollection, T> toSearchResult) throws SearchIndexException {
    // Execute the query and try to get hold of a query response
    SearchResponse response = null;
    try {
      response = getClient().search(request, RequestOptions.DEFAULT);
    } catch (Throwable t) {
      throw new SearchIndexException(t);
    }

    // Create and configure the query result
    long hits = getTotalHits(response.getHits());
    long size = response.getHits().getHits().length;
    SearchResultImpl<T> result = new SearchResultImpl<>(query, hits, size);
    result.setSearchTime(response.getTook().millis());

    // Walk through response and create new items with title, creator, etc:
    for (SearchHit doc : response.getHits()) {

      // Wrap the search resulting metadata
      SearchMetadataCollection metadata = new SearchMetadataCollection(doc.getType());
      metadata.setIdentifier(doc.getId());

      for (DocumentField field : doc.getFields().values()) {
        String name = field.getName();
        SearchMetadata<Object> m = new SearchMetadataImpl<>(name);
        // TODO: Add values with more care (localized, correct type etc.)

        // Add the field values
        if (field.getValues().size() > 1) {
          for (Object v : field.getValues()) {
            m.addValue(v);
          }
        } else {
          m.addValue(field.getValue());
        }

        // Add the metadata
        metadata.add(m);
      }

      // Get the score for this item
      float score = doc.getScore();

      // Have the serializer in charge create a type-specific search result
      // item
      try {
        T document = toSearchResult.apply(metadata);
        SearchResultItem<T> item = new SearchResultItemImpl<>(score, document);
        result.addResultItem(item);
      } catch (Throwable t) {
        logger.warn("Error during search result serialization: '{}'. Skipping this search result.", t.getMessage());
        size--;
      }
    }

    // Set the number of resulting documents
    result.setDocumentCount(size);

    return result;
  }
}
