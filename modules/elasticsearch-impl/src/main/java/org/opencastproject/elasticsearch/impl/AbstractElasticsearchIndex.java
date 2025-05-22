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


package org.opencastproject.elasticsearch.impl;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.elasticsearch.api.SearchIndex;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchMetadata;
import org.opencastproject.elasticsearch.api.SearchQuery;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.util.requests.SortCriterion;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * A search index implementation based on ElasticSearch.
 */
public abstract class AbstractElasticsearchIndex implements SearchIndex {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractElasticsearchIndex.class);

  /** The Elasticsearch maximum results window size */
  private static final int ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW = Integer.MAX_VALUE;

  /** The Elasticsearch term aggregation size */
  private static final int ELASTICSEARCH_TERM_AGGREGATION_SIZE = 10000;

  /** Configuration key defining the hostname of an external Elasticsearch server */
  public static final String ELASTICSEARCH_SERVER_HOSTNAME_KEY = "org.opencastproject.elasticsearch.server.hostname";

  /** Configuration key defining the scheme (http/https) of an external Elasticsearch server */
  public static final String ELASTICSEARCH_SERVER_SCHEME_KEY = "org.opencastproject.elasticsearch.server.scheme";

  /** Configuration key defining the port of an external Elasticsearch server */
  public static final String ELASTICSEARCH_SERVER_PORT_KEY = "org.opencastproject.elasticsearch.server.port";

  /** Configuration key defining the username of an external Elasticsearch server */
  public static final String ELASTICSEARCH_USERNAME_KEY = "org.opencastproject.elasticsearch.username";

  /** Configuration key defining the password of an external Elasticsearch server */
  public static final String ELASTICSEARCH_PASSWORD_KEY = "org.opencastproject.elasticsearch.password";

  /** Default port of an external Elasticsearch server */
  private static final int ELASTICSEARCH_SERVER_PORT_DEFAULT = 9200;

  /** Default hostname of an external Elasticsearch server */
  private static final String ELASTICSEARCH_SERVER_HOSTNAME_DEFAULT = "localhost";

  /** Default scheme of an external Elasticsearch server */
  private static final String ELASTICSEARCH_SERVER_SCHEME_DEFAULT = "http";

  /** Identifier of the root entry */
  private static final String ROOT_ID = "root";

  /** The index identifier */
  private String indexIdentifier = null;
  private static final String INDEX_IDENTIFIER_PROPERTY = "index.identifier";
  private static final String DEFAULT_INDEX_IDENTIFIER = "opencast";

  /** The index name */
  private String indexName = null;
  private static final String INDEX_NAME_PROPERTY = "index.name";
  private static final String DEFAULT_INDEX_NAME = "Elasticsearch";

  /** The high level client */
  private RestHighLevelClient client = null;

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

  /** Username of an external Elasticsearch server to connect to. */
  private String username;

  /** Password of an external Elasticsearch server to connect to. */
  private String password;

  /** Defines how long to wait between retries, when the connection to OpenSearch failed on startup */
  private int retryDelayOnStartup;
  private static final String RETRY_DELAY_ON_STARTUP = "retry.delay.on.startup";
  private static final int DEFAULT_RETRY_DELAY_ON_STARTUP = 10000;

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
   * @param properties
   *          The configuration
   * @param bundleContext
   *          the bundle context
   * @throws ComponentException
   *           if the search index cannot be initialized
   */
  public void activate(Map<String, Object> properties, BundleContext bundleContext) throws ComponentException {
    indexIdentifier = StringUtils.defaultIfBlank((String) properties
            .get(INDEX_IDENTIFIER_PROPERTY), DEFAULT_INDEX_IDENTIFIER);
    logger.info("Index identifier set to {}.", indexIdentifier);

    indexSettingsPath = StringUtils.trimToNull(bundleContext.getProperty("karaf.etc"));
    if (indexSettingsPath == null) {
      throw new ComponentException("Could not determine Karaf configuration path");
    }
    externalServerHostname = StringUtils
            .defaultIfBlank(bundleContext.getProperty(ELASTICSEARCH_SERVER_HOSTNAME_KEY),
                    ELASTICSEARCH_SERVER_HOSTNAME_DEFAULT);
    externalServerScheme = StringUtils
            .defaultIfBlank(bundleContext.getProperty(ELASTICSEARCH_SERVER_SCHEME_KEY),
                    ELASTICSEARCH_SERVER_SCHEME_DEFAULT);
    externalServerPort = Integer.parseInt(StringUtils
            .defaultIfBlank(bundleContext.getProperty(ELASTICSEARCH_SERVER_PORT_KEY),
                    ELASTICSEARCH_SERVER_PORT_DEFAULT + ""));
    username = StringUtils.trimToNull(bundleContext.getProperty(ELASTICSEARCH_USERNAME_KEY));
    password = StringUtils.trimToNull(bundleContext.getProperty(ELASTICSEARCH_PASSWORD_KEY));
  }

  /**
   * OSGi callback for configuration changes.
   *
   * @param properties
   *          The configuration
   */
  public void modified(Map<String, Object> properties) {
    indexName = StringUtils.defaultIfBlank((String) properties.get(INDEX_NAME_PROPERTY),
            DEFAULT_INDEX_NAME);
    logger.info("Index name set to {}.", indexName);

    retryDelayOnStartup = NumberUtils.toInt((String) properties.get(RETRY_DELAY_ON_STARTUP),
        DEFAULT_RETRY_DELAY_ON_STARTUP);
    if (retryDelayOnStartup <= 0) {
      throw new IllegalArgumentException(RETRY_DELAY_ON_STARTUP
          + " was wrongly configured. Value has to be greater than 0.");
    }
    logger.info("Retry delay on startup set to {} ms.", retryDelayOnStartup);
  }

  @Override
  public int getIndexVersion() {
    return indexVersion;
  }

  @Override
  public void clear() throws IOException {
    try {
      final DeleteIndexRequest request = new DeleteIndexRequest(
              Arrays.stream(getDocumentTypes()).map(this::getSubIndexIdentifier).toArray(String[]::new));
      final AcknowledgedResponse delete = client.indices().delete(request, RequestOptions.DEFAULT);
      if (!delete.isAcknowledged()) {
        logger.error("Index '{}' could not be deleted", getIndexName());
      }
      createIndex();
    } catch (ElasticsearchException exception) {
      if (exception.status() == RestStatus.NOT_FOUND) {
        logger.error("Cannot clear non-existing index '{}'", exception.getIndex().getName());
      }
    } catch (SearchIndexException e) {
      logger.error("Unable to re-create the index after a clear", e);
    }
  }

  /**
   * Posts the input document to the search index.
   *
   * @param maxRetryAttempts
   *          How often to retry update in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @param document
   *          The Elasticsearch document
   * @return the query response
   *
   * @throws IOException
   *         If updating the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  protected IndexResponse update(int maxRetryAttempts, int retryWaitingPeriod, ElasticsearchDocument document)
          throws IOException, InterruptedException {

    final IndexRequest indexRequest = new IndexRequest(getSubIndexIdentifier(document.getType())).id(document.getUID())
            .source(document).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

    IndexResponse indexResponse = null;
    int retryAttempts = 0;
    do {
      try {
        indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
      } catch (ElasticsearchStatusException e) {
        retryAttempts++;

        if (retryAttempts <= maxRetryAttempts) {
          logger.warn("Could not update documents in index {}, retrying in {} ms.", getIndexName(),
                  retryWaitingPeriod, e);
          if (retryWaitingPeriod > 0) {
            Thread.sleep(retryWaitingPeriod);
          }
        } else {
          logger.error("Could not update documents in index {}, not retrying.", getIndexName(),
                  e);
          throw e;
        }
      }
    } while (indexResponse == null);

    return indexResponse;
  }

  /**
   * Posts the input documents to the search index.
   *
   * @param maxRetryAttempts
   *          How often to retry update in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @param documents
   *          The Elasticsearch documents
   * @return the query response
   *
   * @throws IOException
   *         If updating the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  protected BulkResponse bulkUpdate(int maxRetryAttempts, int retryWaitingPeriod,
      List<ElasticsearchDocument> documents)
          throws IOException, InterruptedException {
    BulkRequest bulkRequest = new BulkRequest().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

    for (ElasticsearchDocument document: documents) {
      bulkRequest.add(new IndexRequest(getSubIndexIdentifier(document.getType())).id(document.getUID())
          .source(document));
    }

    BulkResponse bulkResponse = null;
    int retryAttempts = 0;
    do {
      try {
        bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
      } catch (ElasticsearchStatusException e) {
        retryAttempts++;

        if (retryAttempts <= maxRetryAttempts) {
          logger.warn("Could not update documents in index {} because of {}, retrying in {} ms.", getIndexName(),
                  e.getMessage(), retryWaitingPeriod);
          if (retryWaitingPeriod > 0) {
            Thread.sleep(retryWaitingPeriod);
          }
        } else {
          logger.error("Could not update documents in index {}, not retrying.", getIndexName(),
                  e);
          throw e;
        }
      }
    } while (bulkResponse == null);

    return bulkResponse;
  }

  /**
   * Delete document from index.
   *
   * @param type
   *         The type of document we want to delete
   * @param id
   *         The identifier of the document
   * @return
   *         The delete response
   *
   * @throws IOException
   *         If deleting from the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  protected DeleteResponse delete(String type, String id, int maxRetryAttempts, int retryWaitingPeriod)
          throws IOException, InterruptedException {
    final DeleteRequest deleteRequest = new DeleteRequest(getSubIndexIdentifier(type), id).setRefreshPolicy(
            WriteRequest.RefreshPolicy.IMMEDIATE);
    DeleteResponse deleteResponse = null;
    int retryAttempts = 0;
    do {
      try {
        deleteResponse = getClient().delete(deleteRequest, RequestOptions.DEFAULT);
      } catch (ElasticsearchStatusException e) {
        retryAttempts++;

        if (retryAttempts <= maxRetryAttempts) {
          logger.warn("Could not remove documents from index {} because of {}, retrying in {} ms.", getIndexName(),
                  e.getMessage(), retryWaitingPeriod);
          if (retryWaitingPeriod > 0) {
            Thread.sleep(retryWaitingPeriod);
          }
        } else {
          logger.error("Could not remove documents from index {}, not retrying.", getIndexName(),
                  e);
          throw e;
        }
      }
    } while (deleteResponse == null);

    return deleteResponse;
  }

  /**
   * Initializes an Elasticsearch node for the given index.
   *
   * @param version
   *          the index version
   * @throws SearchIndexException
   *           if the index configuration cannot be loaded
   * @throws IOException
   *           if loading of settings fails
   * @throws IllegalArgumentException
   *           if the index identifier is blank.
   */
  protected void init(int version)
          throws IOException, IllegalArgumentException, SearchIndexException {
    this.indexVersion = version;

    if (client == null) {
      final RestClientBuilder builder = RestClient
          .builder(new HttpHost(externalServerHostname, externalServerPort, externalServerScheme));

      if (username != null && password != null) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        builder.setHttpClientConfigCallback(
            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
      }

      client = new RestHighLevelClient(builder);
    }

    // Test if opensearch is reachable and continuously retry if it is not
    waitUntilOpensearchIsAvailable();

    // Create the index
    createIndex();
  }

  /**
   * Continuously tries to connect to OpenSearch until it is reachable.
   *
   * @throws RuntimeException if an unexpected exception occurs
   */
  private void waitUntilOpensearchIsAvailable() {
    String openSearchUrl = getOpensearchURL();
    logger.info("Testing connection to OpenSearch at {}", openSearchUrl);
    while (!isOpensearchReachable()) {
      logger.warn("Could not reach OpenSearch at {}. Trying again after {} ms...", openSearchUrl, retryDelayOnStartup);
      try {
        Thread.sleep(retryDelayOnStartup);
      } catch (InterruptedException e) {
        throw new RuntimeException("Could not reach OpenSearch at " + openSearchUrl, e);
      }
    }
    logger.info("Connection to OpenSearch at {} tested successfully", openSearchUrl);
  }

  /**
   * Checks if OpenSearch is reachable.
   *
   * @return true if OpenSearch is reachable, false otherwise
   *
   * @throws ElasticsearchException if an ElasticsearchException occurs, e.g. if the server returns a 4xx or 5xx error
   *
   * @throws RuntimeException if an unexpected exception occurs
   */
  private boolean isOpensearchReachable() {
    try {
      // test connection
      ClusterHealthRequest request = new ClusterHealthRequest();
      request.waitForYellowStatus();
      ClusterHealthResponse resp = client.cluster().health(request, RequestOptions.DEFAULT);
      if (resp.getStatus().equals(ClusterHealthStatus.GREEN)) {
        logger.debug("Connected to OpenSearch, cluster health is {}", resp.getStatus());
        return true;
      } else if (resp.getStatus().equals(ClusterHealthStatus.YELLOW)) {
        logger.warn("Connected to OpenSearch, cluster health is {}", resp.getStatus());
        return true;
      }
      logger.debug("Connected to OpenSearch, but cluster health is {}", resp.getStatus());
      return false;
    } catch (ConnectException | ConnectionClosedException connectException) {
      // Get thrown when we are unable to connect. Normally this should only happen when
      // opensearch is not running or is just starting up, therefore only log the error on debug level
      logger.debug("Unable to connect to OpenSearch", connectException);
      return false;
    } catch (IOException ioException) {
      // Could be thrown when a docker container with opensearch is just starting up,
      // so we check further if the cause is a socket exception
      if (ioException.getCause() instanceof java.net.SocketException) {
        // it seems like a container is starting up, we continue the loop
        logger.debug("Unable to connect to OpenSearch", ioException);
        return false;
      }
      // something different triggered an ioexception, so we fail
      throw new RuntimeException("Couldn't connect to opensearch due to IOExceptionError", ioException);
    } catch (ElasticsearchException elasticsearchException) {
      // An ElasticsearchException is usually thrown in case where the server returns a 4xx or 5xx error code.
      // So for example for an HTTP 401 Unauthorized: In this case we want the startup to fail, so
      // we get an error and have the chance to change the configuration
      logger.error("Error while testing OpenSearch connection", elasticsearchException);
      throw elasticsearchException;
    } catch (Exception e) {
      // When another exception occurs, we throw a runtime exception, so the startup of Opencast will fail
      throw new RuntimeException("Unable to connect to OpenSearch, unexpected exception", e);
    }
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
   * Prepares index to store data for the types (or mappings) as returned by {@link #getDocumentTypes()}.
   *
   *
   * @throws SearchIndexException
   *           if index and type creation fails
   * @throws IOException
   *           if loading of the type definitions fails
   */
  private void createIndex() throws SearchIndexException, IOException {
    if (StringUtils.isBlank(this.indexIdentifier)) {
      throw new IllegalArgumentException("Search index identifier must be set");
    }

    for (String type : getDocumentTypes()) {
      createSubIndex(type, getSubIndexIdentifier(type));
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
      if (getResponse.isExists() && getResponse.getField(IndexSchema.VERSION) != null) {
        final int actualIndexVersion = Integer.parseInt(getResponse.getField(IndexSchema.VERSION).getValue()
                .toString());
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
              .source(Collections.singletonMap(IndexSchema.VERSION, indexVersion + ""));
      logger.debug("Index version of site '{}' is {}", idxName, indexVersion);
      client.index(indexRequest, RequestOptions.DEFAULT);
    }
  }

  /**
   * Load resources from active index class resources if they exist or fall back to these classes resources as default.
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
    logger.debug("Searching for {}", searchSource);

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
    final Map<String, SortCriterion.Order> sortCriteria = query.getSortOrders();
    for (Entry<String, SortCriterion.Order> sortCriterion : sortCriteria.entrySet()) {
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
    return new SearchRequest(Arrays.stream(query.getTypes()).map(this::getSubIndexIdentifier).toArray(String[]::new))
            .searchType(SearchType.QUERY_THEN_FETCH).preference("_local").source(searchSource);
  }

  /**
   * Returns the name of this index.
   *
   * @return the index name
   */
  public String getIndexName() {
    return indexName;
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
  protected String getSubIndexIdentifier(String type) {
    return this.indexIdentifier + "_" + type;
  }

  public RestHighLevelClient getClient() {
    return client;
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
   * @param maxRetryAttempts
   *          How often to retry query in case of ElasticsearchStatusException
   * @param retryWaitingPeriod
   *          How long to wait (in ms) between retries
   * @return A {@link SearchResult} containing the relevant objects.
   *
   * @throws IOException
   *         If querying the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  protected <T> SearchResult<T> executeQuery(SearchQuery query, SearchRequest request,
          Function<SearchMetadataCollection, T> toSearchResult, int maxRetryAttempts, int retryWaitingPeriod)
          throws IOException, InterruptedException {
    // Execute the query and try to get hold of a query response
    SearchResponse searchResponse = null;
    int retryAttempts = 0;
    do {
      try {
        searchResponse = getClient().search(request, RequestOptions.DEFAULT);
      } catch (ElasticsearchStatusException e) {
        retryAttempts++;

        if (retryAttempts <= maxRetryAttempts) {
          logger.warn("Could not query documents from index {} because of {}, retrying in {} ms.", getIndexName(),
                  e.getMessage(), retryWaitingPeriod);
          if (retryWaitingPeriod > 0) {
            Thread.sleep(retryWaitingPeriod);
          }
        } else {
          logger.error("Could not query documents from index {}, not retrying.", getIndexName(),
                  e);
          throw e;
        }
      }
    } while (searchResponse == null);

    // Create and configure the query result
    long hits = getTotalHits(searchResponse.getHits());
    long size = searchResponse.getHits().getHits().length;
    SearchResultImpl<T> result = new SearchResultImpl<>(query, hits, size);
    result.setSearchTime(searchResponse.getTook().millis());

    // Walk through response and create new items with title, creator, etc:
    for (SearchHit doc : searchResponse.getHits()) {

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

  /**
   * Construct the URL to the OpenSearch service.
   *
   * @return the OpenSearch URL
   */
  private String getOpensearchURL() {
    return this.externalServerScheme + "://" + this.externalServerHostname + ":" + this.externalServerPort;
  }

  /**
   * Returns all the known terms for a field (aka facets).
   *
   * @param field
   *          the field name
   * @param type
   *          the document type
   * @return the list of terms
   */
  public List<String> getTermsForField(String field, String type) {
    final String facetName = "terms";
    // Add size to aggregation to return all values (the default is the top ten terms with the most documents).
    // We set it to 10,000, which should be enough (the maximum is 'search.max_buckets', which defaults to 65,536).
    final AggregationBuilder aggBuilder = AggregationBuilders.terms(facetName).field(field)
            .size(ELASTICSEARCH_TERM_AGGREGATION_SIZE);
    final SearchSourceBuilder searchSource = new SearchSourceBuilder().aggregation(aggBuilder);
    final SearchRequest searchRequest = new SearchRequest(this.getSubIndexIdentifier(type)).source(searchSource);
    try {
      final SearchResponse response = getClient().search(searchRequest, RequestOptions.DEFAULT);

      final List<String> terms = new ArrayList<>();
      final Terms aggs = response.getAggregations().get(facetName);

      for (Terms.Bucket bucket : aggs.getBuckets()) {
        terms.add(bucket.getKey().toString());
      }

      return terms;
    } catch (IOException e) {
      return chuck(e);
    }
  }

}
