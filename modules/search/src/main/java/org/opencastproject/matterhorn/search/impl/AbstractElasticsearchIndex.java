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


package org.opencastproject.matterhorn.search.impl;

import static org.opencastproject.matterhorn.search.impl.IndexSchema.VERSION;

import org.opencastproject.matterhorn.search.SearchIndex;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SearchQuery.Order;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * A search index implementation based on ElasticSearch.
 */
public abstract class AbstractElasticsearchIndex implements SearchIndex {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractElasticsearchIndex.class);

  /** The Elasticsearch maximum results window size */
  private static final int ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW = Integer.MAX_VALUE;

  /** Configuration key defining the hostname of an external Elasticsearch server */
  public static final String ELASTICSEARCH_SERVER_HOSTNAME_KEY = "org.opencastproject.elasticsearch.server.hostname";

  /** Configuration key defining the scheme (http/https) of an external Elasticsearch server */
  public static final String ELASTICSEARCH_SERVER_SCHEME_KEY = "org.opencastproject.elasticsearch.server.scheme";

  /** Configuration key defining the port of an external Elasticsearch server */
  public static final String ELASTICSEARCH_SERVER_PORT_KEY = "org.opencastproject.elasticsearch.server.port";

  /** Default port of an external Elasticsearch server */
  private static final int ELASTICSEARCH_SERVER_PORT_DEFAULT = 9200;

  /** Default hostname of an external Elasticsearch server */
  private static final String ELASTICSEARCH_SERVER_HOSTNAME_DEFAULT = "localhost";

  /** Default scheme of an external Elasticsearch server */
  private static final String ELASTICSEARCH_SERVER_SCHEME_DEFAULT = "http";

  /** Identifier of the root entry */
  private static final String ROOT_ID = "root";

  /** Type of the document containing the index version information */
  private static final String VERSION_TYPE = "version";

  /** The index identifier */
  private String index = null;

  /** The high level client */
  private RestHighLevelClient client = null;

  /** List of sites with prepared index */
  private final List<String> preparedIndices = new ArrayList<>();

  /** The version number */
  private int indexVersion = -1;

  /** The path to the index settings */
  protected String indexSettingsPath;

  /** Hostname of an external Elasticsearch server to connect to. */
  private String externalServerHostname = ELASTICSEARCH_SERVER_HOSTNAME_DEFAULT;

  /** Scheme of an external Elasticsearch server to connect to. */
  private String externalServerScheme = ELASTICSEARCH_SERVER_SCHEME_DEFAULT;

  /** Port of an external Elasticsearch server to connect to */
  private int externalServerPort = ELASTICSEARCH_SERVER_PORT_DEFAULT;

  /**
   * Returns an array of document types for the index. For every one of these, the corresponding document type
   * definition will be loaded.
   *
   * @return the document types
   */
  public abstract String[] getDocumentTypes();

  /**
   * OSGi callback to activate this component instance.
   *
   * @param ctx
   *          the component context
   * @throws ComponentException
   *           if the search index cannot be initialized
   */
  public void activate(ComponentContext ctx) throws ComponentException {
    indexSettingsPath = StringUtils.trimToNull(ctx.getBundleContext().getProperty("karaf.etc"));
    if (indexSettingsPath == null) {
      throw new ComponentException("Could not determine Karaf configuration path");
    }
    externalServerHostname = StringUtils
            .defaultIfBlank(ctx.getBundleContext().getProperty(ELASTICSEARCH_SERVER_HOSTNAME_KEY),
                    ELASTICSEARCH_SERVER_HOSTNAME_DEFAULT);
    externalServerScheme = StringUtils
            .defaultIfBlank(ctx.getBundleContext().getProperty(ELASTICSEARCH_SERVER_SCHEME_KEY),
                    ELASTICSEARCH_SERVER_SCHEME_DEFAULT);
    externalServerPort = Integer.parseInt(StringUtils
            .defaultIfBlank(ctx.getBundleContext().getProperty(ELASTICSEARCH_SERVER_PORT_KEY),
                    ELASTICSEARCH_SERVER_PORT_DEFAULT + ""));
  }

  @Override
  public int getIndexVersion() {
    return indexVersion;
  }

  @Override
  public void clear() throws IOException {
    try {
      final DeleteIndexRequest request = new DeleteIndexRequest(
              Arrays.stream(getDocumentTypes()).map(this::getIndexName).toArray(String[]::new));
      final AcknowledgedResponse delete = client.indices().delete(request, RequestOptions.DEFAULT);
      if (!delete.isAcknowledged()) {
        logger.error("Index '{}' could not be deleted", getIndexName());
      }
      preparedIndices
              .removeAll(Arrays.stream(getDocumentTypes()).map(this::getIndexName).collect(Collectors.toList()));
      createIndex(getIndexName());
    } catch (ElasticsearchException exception) {
      if (exception.status() == RestStatus.NOT_FOUND) {
        logger.error("Cannot clear non-existing index '{}'", exception.getIndex().getName());
      }
    } catch (SearchIndexException e) {
      logger.error("Unable to re-create the index after a clear", e);
    }
  }

  /**
   * Removes the given document from the index.
   *
   * @param type
   *          the document type
   * @param uid
   *          the identifier
   * @return <code>true</code> if the element was found and deleted
   * @throws SearchIndexException
   *           if deletion fails
   */
  protected boolean delete(String type, String uid) throws SearchIndexException {
    try {
      if (!preparedIndices.contains(getIndexName(type))) {
        createSubIndex(type, getIndexName(type));
      }
      logger.debug("Removing element with id '{}' from searching index", uid);
      final DeleteRequest deleteRequest = new DeleteRequest(getIndexName(type), uid)
              .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
      final DeleteResponse delete = client.delete(deleteRequest, RequestOptions.DEFAULT);
      if (delete.getResult().equals(DocWriteResponse.Result.NOT_FOUND)) {
        logger.trace("Document {} to delete was not found", uid);
        return false;
      }
    } catch (IOException e) {
      throw new SearchIndexException(e);
    }
    return true;
  }

  /**
   * Posts the input document to the search index.
   *
   * @param documents
   *          the input documents
   * @return the query response
   * @throws SearchIndexException
   *           if posting to the index fails
   */
  protected BulkResponse update(ElasticsearchDocument... documents) throws SearchIndexException {

    final BulkRequest bulkRequest = new BulkRequest().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
    for (ElasticsearchDocument doc : documents) {
      bulkRequest.add(new IndexRequest(getIndexName(doc.getType())).id(doc.getUID()).source(doc));
    }

    try {
      final BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);

      // Check for errors
      if (bulkResponse.hasFailures()) {
        for (BulkItemResponse item : bulkResponse) {
          if (item.isFailed()) {
            logger.warn("Error updating {}: {}", item, item.getFailureMessage());
            throw new SearchIndexException(item.getFailureMessage());
          }
        }
      }

      return bulkResponse;
    } catch (Throwable t) {
      throw new SearchIndexException("Cannot update documents in index " + getIndexName(), t);
    }
  }

  /**
   * Initializes an Elasticsearch node for the given index.
   *
   * @param index
   *          the index identifier
   * @param version
   *          the index version
   * @throws SearchIndexException
   *           if the index configuration cannot be loaded
   * @throws IOException
   *           if loading of settings fails
   * @throws IllegalArgumentException
   *           if the index identifier is blank.
   */
  protected void init(String index, int version) throws IOException, IllegalArgumentException, SearchIndexException {
    if (StringUtils.isBlank(index)) {
      throw new IllegalArgumentException("Search index identifier must be set");
    }

    this.index = index;
    this.indexVersion = version;

    if (client == null) {
      client = new RestHighLevelClient(
              RestClient.builder(new HttpHost(externalServerHostname, externalServerPort, externalServerScheme)));
    }

    // Create the index
    createIndex(index);
  }

  /**
   * Closes the client.
   *
   * @throws IOException
   *           if stopping the Elasticsearch node fails
   */
  protected void close() throws IOException {
    if (client != null) {
      client.close();
    }
  }

  /**
   * Prepares Elasticsearch index to store data for the types (or mappings) as returned by {@link #getDocumentTypes()}.
   *
   * @param idx
   *          the index name
   *
   * @throws SearchIndexException
   *           if index and type creation fails
   * @throws IOException
   *           if loading of the type definitions fails
   */
  private void createIndex(String idx) throws SearchIndexException, IOException {
    for (String type : getDocumentTypes()) {
      createSubIndex(type, getIndexName(type));
    }
  }

  private void createSubIndex(String type, String idxName) throws SearchIndexException, IOException {
    try {
      logger.debug("Trying to create index for '{}'", idxName);
      final CreateIndexRequest request = new CreateIndexRequest(idxName)
              .settings(loadResources("indexSettings.json"), XContentType.JSON)
              .mapping(loadResources(type + "-mapping.json"), XContentType.JSON);

      final CreateIndexResponse siteIdxResponse = client.indices().create(request, RequestOptions.DEFAULT);
      if (!siteIdxResponse.isAcknowledged()) {
        throw new SearchIndexException("Unable to create index for '" + idxName + "'");
      }
    } catch (ElasticsearchStatusException e) {
      if (e.getDetailedMessage().contains("already_exists_exception")) {
        logger.info("Detected existing index '{}'", idxName);
      } else {
        throw e;
      }
    }

    // See if the index version exists and check if it matches. The request will
    // fail if there is no version index
    boolean versionIndexExists = false;
    final GetRequest getRequest = new GetRequest(idxName, ROOT_ID);
    try {
      final GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
      if (getResponse.isExists() && getResponse.getField(VERSION) != null) {
        final int actualIndexVersion = Integer.parseInt(getResponse.getField(VERSION).getValue().toString());
        if (indexVersion != actualIndexVersion) {
          throw new SearchIndexException(
                  "Search index is at version " + actualIndexVersion + ", but codebase expects " + indexVersion);
        }
        versionIndexExists = true;
        logger.debug("Search index version is {}", indexVersion);
      }
    } catch (ElasticsearchException e) {
      logger.debug("Version index has not been created");
    }

    // The index does not exist, let's create it
    if (!versionIndexExists) {
      logger.debug("Creating version index for site '{}'", idxName);
      final IndexRequest indexRequest = new IndexRequest(idxName).id(ROOT_ID)
              .source(Collections.singletonMap(VERSION, indexVersion + ""));
      logger.debug("Index version of site '{}' is {}", idxName, indexVersion);
      client.index(indexRequest, RequestOptions.DEFAULT);
    }

    preparedIndices.add(idxName);
  }

  /**
   * Load resources from active index class resources if they exist or fall back to this classes resources as default.
   *
   * @return the string containing the resource
   * @throws IOException
   *           if reading the resources fails
   */
  private String loadResources(final String filename) throws IOException {
    final String resourcePath = "/elasticsearch/" + filename;
    // Try loading from the index implementation first.
    // This allows index implementations to override the defaults
    for (Class cls : Arrays.asList(this.getClass(), AbstractElasticsearchIndex.class)) {
      try (InputStream is = cls.getResourceAsStream(resourcePath)) {
        if (is != null) {
          final String settings = IOUtils.toString(is, StandardCharsets.UTF_8);
          logger.debug("Reading elasticsearch configuration resources from {}:\n{}", cls, settings);
          return settings;
        }
      }
    }
    return null;
  }

  /**
   * Creates a request for a search query based on the properties known by the search query.
   * <p>
   * Once this query builder has been created, support for ordering needs to be configured as needed.
   *
   * @param query
   *          the search query
   * @return the request builder
   */
  protected SearchRequest getSearchRequest(SearchQuery query, QueryBuilder queryBuilder) {

    final SearchSourceBuilder searchSource = new SearchSourceBuilder()
        .query(queryBuilder)
        .trackTotalHits(true);

    // Create the actual search query
    logger.debug("Searching for {}", searchSource.toString());

    // Make sure all fields are being returned
    if (query.getFields().length > 0) {
      searchSource.storedFields(Arrays.asList(query.getFields()));
    } else {
      searchSource.storedFields(Collections.singletonList("*"));
    }

    // Pagination
    if (query.getOffset() >= 0) {
      searchSource.from(query.getOffset());
    }

    int limit = ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW;
    if (query.getLimit() > 0) {
      if (query.getOffset() > 0
              && (long) query.getOffset() + (long) query.getLimit() > ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW) {
        limit = ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW - query.getOffset();
      } else {
        limit = query.getLimit();
      }
    }
    searchSource.size(limit);

    // Sort orders
    final Map<String, Order> sortCriteria = query.getSortOrders();
    for (Entry<String, Order> sortCriterion : sortCriteria.entrySet()) {
      ScriptSortBuilder sortBuilder = null;
      logger.debug("Event sort criteria: {}", sortCriterion.getKey());
      if ("publication".equals(sortCriterion.getKey())) {
        sortBuilder = SortBuilders.scriptSort(
            new Script("params._source.publication.length"),
            ScriptSortBuilder.ScriptSortType.NUMBER);
      }
      switch (sortCriterion.getValue()) {
        case Ascending:
          if (sortBuilder != null) {
            sortBuilder.order(SortOrder.ASC);
            searchSource.sort(sortBuilder);
          } else {
            searchSource.sort(sortCriterion.getKey(), SortOrder.ASC);
          }
          break;
        case Descending:
          if (sortBuilder != null) {
            sortBuilder.order(SortOrder.DESC);
            searchSource.sort(sortBuilder);
          } else {
            searchSource.sort(sortCriterion.getKey(), SortOrder.DESC);
          }
          break;
        default:
          break;
      }
    }
    return new SearchRequest(Arrays.stream(query.getTypes()).map(this::getIndexName).toArray(String[]::new))
            .searchType(SearchType.QUERY_THEN_FETCH).preference("_local").source(searchSource);
  }

  /**
   * Returns the name of this index.
   *
   * @return the index name
   */
  public String getIndexName() {
    return index;
  }

  /*
   * This method is a workaround to avoid accessing org.apache.lucene.search.TotalHits outside this bundle.
   * Doing so would cause OSGi dependency problems. It seems to be a bug anyway that ES exposes this
   * class.
   */
  protected long getTotalHits(SearchHits hits) {
    return hits.getTotalHits().value;
  }

  /**
   * Returns the name of the sub index for the given type.
   *
   * @param type
   *          The type to get the sub index for.
   * @return the index name
   */
  public String getIndexName(String type) {
    return getIndexName() + "_" + type;
  }

  protected RestHighLevelClient getClient() {
    return client;
  }

}
