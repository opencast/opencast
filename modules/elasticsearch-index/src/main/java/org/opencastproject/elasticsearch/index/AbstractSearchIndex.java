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
import org.opencastproject.elasticsearch.index.group.Group;
import org.opencastproject.elasticsearch.index.group.GroupIndexUtils;
import org.opencastproject.elasticsearch.index.group.GroupQueryBuilder;
import org.opencastproject.elasticsearch.index.group.GroupSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesIndexUtils;
import org.opencastproject.elasticsearch.index.series.SeriesQueryBuilder;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.elasticsearch.index.theme.Theme;
import org.opencastproject.elasticsearch.index.theme.ThemeIndexUtils;
import org.opencastproject.elasticsearch.index.theme.ThemeQueryBuilder;
import org.opencastproject.elasticsearch.index.theme.ThemeSearchQuery;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

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
import java.util.function.Function;

import javax.xml.bind.Unmarshaller;

public abstract class AbstractSearchIndex extends AbstractElasticsearchIndex {

  private static final Logger logger = LoggerFactory.getLogger(AbstractSearchIndex.class);

  @Override
  public abstract String getIndexName();

  /**
   * Adds the recording event to the search index or updates it accordingly if it is there.
   *
   * @param event
   *          the recording event
   * @throws SearchIndexException
   *           if the event cannot be added or updated
   */
  public void addOrUpdate(Event event) throws SearchIndexException {
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
   * Adds or updates the group in the search index.
   *
   * @param group
   *          The group to add
   * @throws SearchIndexException
   *           Thrown if unable to add or update the group.
   */
  public void addOrUpdate(Group group) throws SearchIndexException {
    logger.debug("Adding group {} to search index", group.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = GroupIndexUtils.toSearchMetadata(group);
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);
    try {
      update(doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write resource " + group + " to index", t);
    }
  }

  /**
   * Add or update a series in the search index.
   *
   * @param series
   * @throws SearchIndexException
   */
  public void addOrUpdate(Series series) throws SearchIndexException {
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
   * Adds or updates the theme in the search index.
   *
   * @param theme
   *          The theme to add
   * @throws SearchIndexException
   *           Thrown if unable to add or update the theme.
   */
  public void addOrUpdate(Theme theme) throws SearchIndexException {
    logger.debug("Adding theme {} to search index", theme.getIdentifier());

    // Add the resource to the index
    SearchMetadataCollection inputDocument = ThemeIndexUtils.toSearchMetadata(theme);
    List<SearchMetadata<?>> resourceMetadata = inputDocument.getMetadata();
    ElasticsearchDocument doc = new ElasticsearchDocument(inputDocument.getIdentifier(),
            inputDocument.getDocumentType(), resourceMetadata);
    try {
      update(doc);
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot write resource " + theme + " to index", t);
    }
  }

  @Override
  public boolean delete(String type, String uid) throws SearchIndexException {
    logger.debug("Removing element with id '{}' from searching index '{}'", uid, getIndexName(type));
    final DeleteRequest deleteRequest = new DeleteRequest(getIndexName(type), uid)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
    try {
      final DeleteResponse delete = getClient().delete(deleteRequest, RequestOptions.DEFAULT);
      if (delete.getResult().equals(DocWriteResponse.Result.NOT_FOUND)) {
        logger.trace("Document {} to delete was not found on index '{}'", uid, getIndexName(type));
        return false;
      }
    } catch (IOException e) {
      throw new SearchIndexException(e);
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
    Event event = EventIndexUtils.getEvent(uid, organization, user, this);
    if (event == null) {
      throw new NotFoundException("No event with id " + uid + " found.");
    }

    event.setArchiveVersion(null);

    if (toDelete(event)) {
      delete(Event.DOCUMENT_TYPE, uid.concat(organization));
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
    Event event = EventIndexUtils.getEvent(uid, organization, user, this);
    if (event == null) {
      throw new NotFoundException("No event with id " + uid + " found.");
    }

    event.setAgentId(null);

    if (toDelete(event)) {
      delete(Event.DOCUMENT_TYPE, uid.concat(organization));
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
    Event event = EventIndexUtils.getEvent(uid, organization, user, this);
    if (event == null) {
      throw new NotFoundException("No event with id " + uid + " found.");
    }

    if (event.getWorkflowId() != null && event.getWorkflowId().equals(workflowId)) {
      logger.debug("Workflow {} is the current workflow of event {}. Removing it from event.", uid, workflowId);
      event.setWorkflowId(null);
      event.setWorkflowDefinitionId(null);
      event.setWorkflowState(null);
    }

    if (toDelete(event)) {
      delete(Event.DOCUMENT_TYPE, uid.concat(organization));
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
   *          The query to use to retrieve the groups that match the query
   * @return {@link SearchResult} collection of {@link Group} from a query.
   * @throws SearchIndexException
   *           Thrown if there is an error getting the results.
   */
  public SearchResult<Group> getByQuery(GroupSearchQuery query) throws SearchIndexException {

    logger.debug("Searching index using group query '{}'", query);

    // Create the request
    final SearchRequest searchRequest = getSearchRequest(query, new GroupQueryBuilder(query));

    try {
      final Unmarshaller unmarshaller = Group.createUnmarshaller();
      return executeQuery(query, searchRequest, metadata -> {
        try {
          return GroupIndexUtils.toGroup(metadata, unmarshaller);
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
   * @return {@link SearchResult} collection of {@link Theme} from a query.
   * @throws SearchIndexException
   *           Thrown if there is an error getting the results.
   */
  public SearchResult<Theme> getByQuery(ThemeSearchQuery query) throws SearchIndexException {
    logger.debug("Searching index using theme query '{}'", query);
    // Create the request
    final SearchRequest searchRequest = getSearchRequest(query, new ThemeQueryBuilder(query));

    try {
      return executeQuery(query, searchRequest, metadata -> {
        try {
          return ThemeIndexUtils.toTheme(metadata);
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
