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

package org.opencastproject.index.service.impl.index;

import static java.lang.String.format;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.index.IndexProducer;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexUtils;
import org.opencastproject.index.service.impl.index.event.EventQueryBuilder;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.index.service.impl.index.group.GroupIndexUtils;
import org.opencastproject.index.service.impl.index.group.GroupQueryBuilder;
import org.opencastproject.index.service.impl.index.group.GroupSearchQuery;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesIndexUtils;
import org.opencastproject.index.service.impl.index.series.SeriesQueryBuilder;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.index.service.impl.index.theme.Theme;
import org.opencastproject.index.service.impl.index.theme.ThemeIndexUtils;
import org.opencastproject.index.service.impl.index.theme.ThemeQueryBuilder;
import org.opencastproject.index.service.impl.index.theme.ThemeSearchQuery;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchMetadata;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex;
import org.opencastproject.matterhorn.search.impl.ElasticsearchDocument;
import org.opencastproject.matterhorn.search.impl.SearchMetadataCollection;
import org.opencastproject.matterhorn.search.impl.SearchMetadataImpl;
import org.opencastproject.matterhorn.search.impl.SearchResultImpl;
import org.opencastproject.matterhorn.search.impl.SearchResultItemImpl;
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.Fn;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.xml.bind.Unmarshaller;

public abstract class AbstractSearchIndex extends AbstractElasticsearchIndex {

  private static final Logger logger = LoggerFactory.getLogger(AbstractSearchIndex.class);

  /** The message sender */
  private MessageSender messageSender;

  /** The message receiver */
  private MessageReceiver messageReceiver;

  /** An Executor to get messages */
  private ExecutorService executor = Executors.newSingleThreadExecutor();

  @Override
  public abstract String getIndexName();

  /** OSGi DI. */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /** OSGi DI. */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * Recreate the index from all of the services that provide data.
   *
   * @throws InterruptedException
   *           Thrown if the process is interupted.
   * @throws CancellationException
   *           Thrown if listeing to messages has been canceled.
   * @throws ExecutionException
   *           Thrown if there is a problem executing the process.
   * @throws IOException
   *           Thrown if the index cannot be cleared.
   * @throws IndexServiceException
   *           Thrown if there was a problem adding some of the data back into the index.
   */
  public synchronized void recreateIndex()
          throws InterruptedException, CancellationException, ExecutionException, IOException, IndexServiceException {
    // Clear index first
    clear();
    recreateService(IndexRecreateObject.Service.Groups);
    recreateService(IndexRecreateObject.Service.Acl);
    recreateService(IndexRecreateObject.Service.Themes);
    recreateService(IndexRecreateObject.Service.Series);
    recreateService(IndexRecreateObject.Service.Scheduler);
    recreateService(IndexRecreateObject.Service.Workflow);
    recreateService(IndexRecreateObject.Service.AssetManager);
    recreateService(IndexRecreateObject.Service.Comments);
  }

  /**
   * Recreate the index from a specific service that provide data.
   *
   * @param service
   *           The service name. The available services are:
   *           Groups, Acl, Themes, Series, Scheduler, Workflow, AssetManager, Comments
   *
   * @throws IllegalArgumentException
   *           Thrown if the service name is invalid
   * @throws InterruptedException
   *           Thrown if the process is interupted.
   * @throws ExecutionException
   *           Thrown if there is a problem executing the process.
   * @throws IndexServiceException
   *           Thrown if there was a problem adding some of the data back into the index.
   */
  public synchronized void recreateIndex(String service)
          throws IllegalArgumentException, InterruptedException, ExecutionException, IndexServiceException {
    if (StringUtils.equalsIgnoreCase("Groups", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.Groups);
    else if (StringUtils.equalsIgnoreCase("Acl", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.Acl);
    else if (StringUtils.equalsIgnoreCase("Themes", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.Themes);
    else if (StringUtils.equalsIgnoreCase("Series", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.Series);
    else if (StringUtils.equalsIgnoreCase("Scheduler", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.Scheduler);
    else if (StringUtils.equalsIgnoreCase("Workflow", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.Workflow);
    else if (StringUtils.equalsIgnoreCase("AssetManager", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.AssetManager);
    else if (StringUtils.equalsIgnoreCase("Comments", StringUtils.trim(service)))
      recreateService(IndexRecreateObject.Service.Comments);
    else
      throw new IllegalArgumentException("Unknown service " + service);
  }

  /**
   * Ask for data to be rebuilt from a service.
   *
   * @param service
   *          The {@link IndexRecreateObject.Service} representing the service to start re-sending the data from.
   * @throws IndexServiceException
   *           Thrown if there is a problem re-sending the data from the service.
   * @throws InterruptedException
   *           Thrown if the process of re-sending the data is interupted.
   * @throws CancellationException
   *           Thrown if listening to messages has been canceled.
   * @throws ExecutionException
   *           Thrown if the process of re-sending the data has an error.
   */
  private void recreateService(IndexRecreateObject.Service service)
          throws IndexServiceException, InterruptedException, CancellationException, ExecutionException {
    logger.info("Starting to recreate index for service '{}'", service);
    messageSender.sendObjectMessage(IndexProducer.RECEIVER_QUEUE + "." + service, MessageSender.DestinationType.Queue,
            IndexRecreateObject.start(getIndexName(), service));
    boolean done = false;
    // TODO Add a timeout for services that are not going to respond.
    while (!done) {
      FutureTask<Serializable> future = messageReceiver.receiveSerializable(IndexProducer.RESPONSE_QUEUE,
              MessageSender.DestinationType.Queue);
      executor.execute(future);
      BaseMessage message = (BaseMessage) future.get();
      if (message.getObject() instanceof IndexRecreateObject) {
        IndexRecreateObject indexRecreateObject = (IndexRecreateObject) message.getObject();
        switch (indexRecreateObject.getStatus()) {
          case Update:
            logger.info("Updating service: '{}' with {}/{} finished, {}% complete.", indexRecreateObject.getService(),
                    indexRecreateObject.getCurrent(), indexRecreateObject.getTotal(), (int) (indexRecreateObject.getCurrent() * 100 / indexRecreateObject.getTotal()));
            if (indexRecreateObject.getCurrent() == indexRecreateObject.getTotal()) {
              logger.info("Waiting for service '{}' indexing to complete", indexRecreateObject.getService());
            }
            break;
          case End:
            done = true;
            logger.info("Finished re-creating data for service '{}'", indexRecreateObject.getService());
            break;
          case Error:
            logger.error("Error updating service '{}' with {}/{} finished.",
                    indexRecreateObject.getService(), indexRecreateObject.getCurrent(),
                            indexRecreateObject.getTotal());
            throw new IndexServiceException(
                    format("Error updating service '%s' with %s/%s finished.", indexRecreateObject.getService(),
                            indexRecreateObject.getCurrent(), indexRecreateObject.getTotal()));
          default:
            logger.error("Unable to handle the status '{}' for service '{}'", indexRecreateObject.getStatus(),
                    indexRecreateObject.getService());
            throw new IllegalArgumentException(format("Unable to handle the status '%s' for service '%s'",
                    indexRecreateObject.getStatus(), indexRecreateObject.getService()));

        }
      }
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
  public void addOrUpdate(Event event) throws SearchIndexException {
    logger.debug("Adding event {} to search index", event.getIdentifier());

    // if (!preparedIndices.contains(resource.getURI().getSite().getIdentifier())) {
    // try {
    // createIndex(resource.getURI().getSite());
    // } catch (IOException e) {
    // throw new SearchIndexException(e);
    // }
    // }

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

    // if (!preparedIndices.contains(resource.getURI().getSite().getIdentifier())) {
    // try {
    // createIndex(resource.getURI().getSite());
    // } catch (IOException e) {
    // throw new SearchIndexException(e);
    // }
    // }

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

    // if (!preparedIndices.contains(resource.getURI().getSite().getIdentifier())) {
    // try {
    // createIndex(resource.getURI().getSite());
    // } catch (IOException e) {
    // throw new SearchIndexException(e);
    // }
    // }

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

    // if (!preparedIndices.contains(resource.getURI().getSite().getIdentifier())) {
    // try {
    // createIndex(resource.getURI().getSite());
    // } catch (IOException e) {
    // throw new SearchIndexException(e);
    // }
    // }

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
  public boolean delete(String documentType, String uid) throws SearchIndexException {
    logger.debug("Removing element with id '{}' from searching index '{}'", uid, getIndexName());

    DeleteRequestBuilder deleteRequest = getSearchClient().prepareDelete(getIndexName(), documentType, uid);
    deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
    DeleteResponse delete = deleteRequest.execute().actionGet();
    if (delete.getResult() == DocWriteResponse.Result.NOT_FOUND) {
      logger.trace("Document {} to delete was not found on index '{}'", uid, getIndexName());
      return false;
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
    if (event == null)
      throw new NotFoundException("No event with id " + uid + " found.");

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
    if (event == null)
      throw new NotFoundException("No event with id " + uid + " found.");

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
    if (event == null)
      throw new NotFoundException("No event with id " + uid + " found.");

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
    // Create the request builder
    SearchRequestBuilder requestBuilder = getSearchRequestBuilder(query, new EventQueryBuilder(query));

    try {
      Unmarshaller unmarshaller = Event.createUnmarshaller();
      return executeQuery(query, requestBuilder, new Fn<SearchMetadataCollection, Event>() {
        @Override
        public Event apply(SearchMetadataCollection metadata) {
          try {
            return EventIndexUtils.toRecordingEvent(metadata, unmarshaller);
          } catch (IOException e) {
            return chuck(e);
          }
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

    // Create the request builder
    SearchRequestBuilder requestBuilder = getSearchRequestBuilder(query, new GroupQueryBuilder(query));

    try {
      Unmarshaller unmarshaller = Group.createUnmarshaller();
      return executeQuery(query, requestBuilder, new Fn<SearchMetadataCollection, Group>() {
        @Override
        public Group apply(SearchMetadataCollection metadata) {
          try {
            return GroupIndexUtils.toGroup(metadata, unmarshaller);
          } catch (IOException e) {
            return chuck(e);
          }
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
    // Create the request builder
    SearchRequestBuilder requestBuilder = getSearchRequestBuilder(query, new SeriesQueryBuilder(query));
    try {
      Unmarshaller unmarshaller = Series.createUnmarshaller();
      return executeQuery(query, requestBuilder, new Fn<SearchMetadataCollection, Series>() {
        @Override
        public Series apply(SearchMetadataCollection metadata) {
          try {
            return SeriesIndexUtils.toSeries(metadata, unmarshaller);
          } catch (IOException e) {
            return chuck(e);
          }
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
    // Create the request builder
    SearchRequestBuilder requestBuilder = getSearchRequestBuilder(query, new ThemeQueryBuilder(query));

    try {
      return executeQuery(query, requestBuilder, new Fn<SearchMetadataCollection, Theme>() {
        @Override
        public Theme apply(SearchMetadataCollection metadata) {
          try {
            return ThemeIndexUtils.toTheme(metadata);
          } catch (IOException e) {
            return chuck(e);
          }
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
    AggregationBuilder aggBuilder = AggregationBuilders.terms(facetName).field(field);
    SearchRequestBuilder search = getSearchClient().prepareSearch(getIndexName()).addAggregation(aggBuilder);

    if (types.isSome())
      search = search.setTypes(types.get());

    SearchResponse response = search.execute().actionGet();

    List<String> terms = new ArrayList<>();
    Terms aggs = response.getAggregations().get(facetName);

    for (Bucket bucket : aggs.getBuckets()) {
      terms.add(bucket.getKey().toString());
    }

    return terms;
  }

  /**
   * Execute a query on the index.
   *
   * @param query
   *          The query to use to find the results
   * @param requestBuilder
   *          The builder to use to create the query.
   * @param toSearchResult
   *          The function to convert the results to a {@link SearchResult}
   * @return A {@link SearchResult} containing the relevant objects.
   * @throws SearchIndexException
   */
  protected <T> SearchResult<T> executeQuery(SearchQuery query, SearchRequestBuilder requestBuilder,
          Fn<SearchMetadataCollection, T> toSearchResult) throws SearchIndexException {
    // Execute the query and try to get hold of a query response
    SearchResponse response = null;
    try {
      response = getSearchClient().search(requestBuilder.request()).actionGet();
    } catch (Throwable t) {
      throw new SearchIndexException(t);
    }

    // Create and configure the query result
    long hits = response.getHits().getTotalHits();
    long size = response.getHits().getHits().length;
    SearchResultImpl<T> result = new SearchResultImpl<>(query, hits, size);
    result.setSearchTime(response.getTookInMillis());

    // Walk through response and create new items with title, creator, etc:
    for (SearchHit doc : response.getHits()) {

      // Wrap the search resulting metadata
      SearchMetadataCollection metadata = new SearchMetadataCollection(doc.getType());
      metadata.setIdentifier(doc.getId());

      for (SearchHitField field : doc.getFields().values()) {
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
        continue;
      }
    }

    // Set the number of resulting documents
    result.setDocumentCount(size);

    return result;
  }
}
