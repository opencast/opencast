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


package org.opencastproject.opensearch.impl;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.opensearch.api.SearchIndexException;
import org.opencastproject.opensearch.api.SearchResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.opensearch.client.transport.rest_client.RestClientTransport;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A search index implementation based on OpenSearch.
 */
public abstract class AbstractOpenSearchIndex {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractOpenSearchIndex.class);

  /** The OpenSearch term aggregation size */
  private static final int OPENSEARCH_TERM_AGGREGATION_SIZE = 10000;

  /** Configuration key defining the hostname of an external OpenSearch server */
  public static final String OPENSEARCH_SERVER_HOSTNAME_KEY = "org.opencastproject.opensearch.server.hostname";

  /** Configuration key defining the scheme (http/https) of an external OpenSearch server */
  public static final String OPENSEARCH_SERVER_SCHEME_KEY = "org.opencastproject.opensearch.server.scheme";

  /** Configuration key defining the port of an external OpenSearch server */
  public static final String OPENSEARCH_SERVER_PORT_KEY = "org.opencastproject.opensearch.server.port";

  /** Configuration key defining the username of an external OpenSearch server */
  public static final String OPENSEARCH_USERNAME_KEY = "org.opencastproject.opensearch.username";

  /** Configuration key defining the password of an external OpenSearch server */
  public static final String OPENSEARCH_PASSWORD_KEY = "org.opencastproject.opensearch.password";

  /** Default port of an external OpenSearch server */
  private static final int OPENSEARCH_SERVER_PORT_DEFAULT = 9200;

  /** Default hostname of an external OpenSearch server */
  private static final String OPENSEARCH_SERVER_HOSTNAME_DEFAULT = "localhost";

  /** Default scheme of an external OpenSearch server */
  private static final String OPENSEARCH_SERVER_SCHEME_DEFAULT = "http";

  /** The index identifier */
  private String indexIdentifier = null;
  private static final String INDEX_IDENTIFIER_PROPERTY = "index.identifier";
  private static final String DEFAULT_INDEX_IDENTIFIER = "opencast";

  /** The index name */
  private String indexName = null;
  private static final String INDEX_NAME_PROPERTY = "index.name";
  private static final String DEFAULT_INDEX_NAME = "OpenSearch";

  /** The document types */
  protected static final String VERSION_DOCUMENT_TYPE = "version";

  /** The legacy high level client, only used for its connection */
  private RestHighLevelClient legacyClient = null;

  /** The OpenSearch client */
  private OpenSearchClient client = null;

  /** How often to retry failed requests */
  private int maxRetryAttempts;
  private static final String MAX_RETRY_ATTEMPTS_PROPERTY = "max.retry.attempts";
  private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 0;

  /** How long to wait between retries of failed requests */
  private int retryWaitingPeriod;
  private static final String RETRY_WAITING_PERIOD_PROPERTY = "retry.waiting.period";
  private static final int DEFAULT_RETRY_WAITING_PERIOD = 1000;

  /**
   * Configuration keys replaced by {@link #MAX_RETRY_ATTEMPTS_PROPERTY} and {@link #RETRY_WAITING_PERIOD_PROPERTY}.
   * The configuration is rejected if they are still set. This check can be removed with Opencast 24.
   */
  private static final List<String> REMOVED_RETRY_PROPERTIES = List.of(
      "max.retry.attempts.get", "max.retry.attempts.update",
      "retry.waiting.period.get", "retry.waiting.period.update");

  /** The version number */
  private int indexVersion = -1;

  /** The path to the index settings */
  protected String indexSettingsPath;

  /** Hostname of an external OpenSearch server to connect to. */
  private String externalServerHostname = OPENSEARCH_SERVER_HOSTNAME_DEFAULT;

  /** Scheme of an external OpenSearch server to connect to. */
  private String externalServerScheme = OPENSEARCH_SERVER_SCHEME_DEFAULT;

  /** Port of an external OpenSearch server to connect to */
  private int externalServerPort = OPENSEARCH_SERVER_PORT_DEFAULT;

  /** Username of an external OpenSearch server to connect to. */
  private String username;

  /** Password of an external OpenSearch server to connect to. */
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
            .defaultIfBlank(bundleContext.getProperty(OPENSEARCH_SERVER_HOSTNAME_KEY),
                    OPENSEARCH_SERVER_HOSTNAME_DEFAULT);
    externalServerScheme = StringUtils
            .defaultIfBlank(bundleContext.getProperty(OPENSEARCH_SERVER_SCHEME_KEY),
                    OPENSEARCH_SERVER_SCHEME_DEFAULT);
    externalServerPort = Integer.parseInt(StringUtils
            .defaultIfBlank(bundleContext.getProperty(OPENSEARCH_SERVER_PORT_KEY),
                    OPENSEARCH_SERVER_PORT_DEFAULT + ""));
    username = StringUtils.trimToNull(bundleContext.getProperty(OPENSEARCH_USERNAME_KEY));
    password = StringUtils.trimToNull(bundleContext.getProperty(OPENSEARCH_PASSWORD_KEY));
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

    for (String key : REMOVED_RETRY_PROPERTIES) {
      if (properties.containsKey(key)) {
        throw new IllegalArgumentException("Configuration key " + key + " is no longer supported. Remove it and use "
            + MAX_RETRY_ATTEMPTS_PROPERTY + " and " + RETRY_WAITING_PERIOD_PROPERTY + " instead.");
      }
    }
    maxRetryAttempts = Math.max(0, NumberUtils.toInt((String) properties.get(MAX_RETRY_ATTEMPTS_PROPERTY),
        DEFAULT_MAX_RETRY_ATTEMPTS));
    retryWaitingPeriod = Math.max(0, NumberUtils.toInt((String) properties.get(RETRY_WAITING_PERIOD_PROPERTY),
        DEFAULT_RETRY_WAITING_PERIOD));
    logger.info("Failed requests are retried {} times, waiting {} ms between attempts.", maxRetryAttempts,
        retryWaitingPeriod);
  }

  /**
   * A request to OpenSearch.
   *
   * @param <T>
   *          the type of the response
   */
  @FunctionalInterface
  public interface Request<T> {
    T execute(OpenSearchClient client) throws IOException;
  }

  /**
   * Executes a request, retrying it as configured if OpenSearch reports an error. Bad requests are not retried.
   *
   * @param request
   *          the request to execute
   * @return the response
   * @throws IOException
   *          if the request fails because of a connection problem
   * @throws OpenSearchException
   *          if OpenSearch reports an error and all retries are used up
   * @throws InterruptedException
   *          if waiting for the next attempt is interrupted
   */
  public <T> T withRetries(Request<T> request) throws IOException, InterruptedException {
    for (int attempt = 1; ; attempt++) {
      try {
        return request.execute(client);
      } catch (OpenSearchException e) {
        if (e.status() == 400 || attempt > maxRetryAttempts) {
          throw e;
        }
        logger.warn("Request to OpenSearch index {} failed ({}), retrying in {} ms.", getIndexName(),
            e.getMessage(), retryWaitingPeriod);
        Thread.sleep(retryWaitingPeriod);
      }
    }
  }

  /**
   * Deletes all data from the index and creates it anew.
   *
   * @throws IOException
   *          if the request to OpenSearch fails
   */
  public void clear() throws IOException {
    try {
      final List<String> indices = Arrays.stream(getDocumentTypes()).map(this::getSubIndexIdentifier).toList();
      if (!client.indices().delete(d -> d.index(indices)).acknowledged()) {
        logger.error("Index '{}' could not be deleted", getIndexName());
      }
      createIndex();
    } catch (OpenSearchException exception) {
      if (exception.status() != 404) {
        throw exception;
      }
      logger.error("Cannot clear non-existing index '{}'", getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Unable to re-create the index after a clear", e);
    }
  }

  /**
   * Adds or replaces a document in the search index.
   *
   * @param type
   *          The document type
   * @param id
   *          The document identifier
   * @param document
   *          The document
   *
   * @throws IOException
   *         If updating the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  protected void update(String type, String id, Map<String, Object> document)
          throws IOException, InterruptedException {
    withRetries(c -> c.index(i -> i
        .index(getSubIndexIdentifier(type))
        .id(id)
        .document(document)
        .refresh(Refresh.True)));
  }

  /**
   * Adds or replaces documents in the search index.
   *
   * @param type
   *          The document type
   * @param documents
   *          The documents by their identifiers
   *
   * @throws IOException
   *         If updating the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  protected void bulkUpdate(String type, Map<String, ? extends Map<String, Object>> documents)
          throws IOException, InterruptedException {
    final List<BulkOperation> operations = documents.entrySet().stream()
        .map(document -> BulkOperation.of(o -> o.index(i -> i
            .index(getSubIndexIdentifier(type))
            .id(document.getKey())
            .document(document.getValue()))))
        .toList();
    final BulkResponse response = withRetries(c -> c.bulk(b -> b.operations(operations).refresh(Refresh.True)));
    if (response.errors()) {
      response.items().stream()
          .filter(item -> item.error() != null)
          .forEach(item -> logger.error("Could not update document {} in index {}: {}", item.id(), item.index(),
              item.error().reason()));
    }
  }

  /**
   * Delete document from index.
   *
   * @param type
   *         The type of document we want to delete
   * @param id
   *         The identifier of the document
   * @return
   *         Whether the document existed
   *
   * @throws IOException
   *         If deleting from the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  protected boolean delete(String type, String id) throws IOException, InterruptedException {
    return withRetries(c -> c.delete(d -> d.index(getSubIndexIdentifier(type)).id(id).refresh(Refresh.True)))
        .result() != Result.NotFound;
  }

  /**
   * Initializes an OpenSearch node for the given index.
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
      final HttpHost host = new HttpHost(externalServerScheme, externalServerHostname, externalServerPort);
      final RestClientBuilder builder = RestClient.builder(host);

      if (username != null && password != null) {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(host),
            new UsernamePasswordCredentials(username, password.toCharArray()));
        builder.setHttpClientConfigCallback(
            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
      }

      legacyClient = new RestHighLevelClient(builder);
      client = new OpenSearchClient(new RestClientTransport(legacyClient.getLowLevelClient(),
          new JacksonJsonpMapper()));
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
   * @throws OpenSearchException if an OpenSearchException occurs, e.g. if the server returns a 4xx or 5xx error
   *
   * @throws RuntimeException if an unexpected exception occurs
   */
  private boolean isOpensearchReachable() {
    try {
      // test connection
      final HealthResponse resp = client.cluster().health(h -> h.waitForStatus(HealthStatus.Yellow));
      if (resp.status() == HealthStatus.Green) {
        logger.debug("Connected to OpenSearch, cluster health is {}", resp.status());
        return true;
      } else if (resp.status() == HealthStatus.Yellow) {
        logger.warn("Connected to OpenSearch, cluster health is {}", resp.status());
        return true;
      }
      logger.debug("Connected to OpenSearch, but cluster health is {}", resp.status());
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
    } catch (OpenSearchException opensearchException) {
      if (opensearchException.status() >= 500) {
        logger.debug("OpenSearch health request ended with HTTP status code {}. Is OpenSearch service running?",
            opensearchException.status());
        return false;
      }
      // An OpenSearchException is usually thrown in case where the server returns a 4xx or 5xx error code.
      // So for example for an HTTP 401 Unauthorized: In this case we want the startup to fail, so
      // we get an error and have the chance to change the configuration
      logger.error("Error while testing OpenSearch connection", opensearchException);
      throw opensearchException;
    } catch (Exception e) {
      // When another exception occurs, we throw a runtime exception, so the startup of Opencast will fail
      throw new RuntimeException("Unable to connect to OpenSearch, unexpected exception", e);
    }
  }

  /**
   * Closes the client.
   *
   * @throws IOException
   *           if stopping the OpenSearch node fails
   */
  protected void close() throws IOException {
    if (legacyClient != null) {
      // Also closes the transport of the new client, which shares the same connection
      legacyClient.close();
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
    createIndexIfMissing(idxName, loadResources("indexSettings.json"), loadResources(type + "-mapping.json"));

    // See if the index version exists and check if it matches.
    final String versionIndex = getSubIndexIdentifier(VERSION_DOCUMENT_TYPE);
    final GetResponse<Map> version;
    try {
      version = client.get(g -> g.index(versionIndex).id(idxName), Map.class);
    } catch (OpenSearchException e) {
      throw new SearchIndexException("Requesting version of the index '" + idxName + "' failed.", e);
    }
    if (version.found() && version.source() != null && version.source().containsKey(IndexSchema.VERSION)) {
      final int actualIndexVersion = ((Number) version.source().get(IndexSchema.VERSION)).intValue();
      if (indexVersion != actualIndexVersion) {
        throw new SearchIndexException("Search index '" + idxName + "' is at version " + actualIndexVersion
            + ", but codebase expects version " + indexVersion);
      }
      logger.debug("Search index '{}' is at version {}", idxName, indexVersion);
    } else {
      logger.debug("Creating version index for site '{}' at version {}", idxName, indexVersion);
      client.index(i -> i.index(versionIndex).id(idxName).document(Map.of(IndexSchema.VERSION, indexVersion)));
    }
  }

  /**
   * Creates an index unless it already exists.
   *
   * @param name
   *          the name of the index
   * @param settings
   *          the index settings as JSON
   * @param mapping
   *          the index mapping as JSON
   * @throws SearchIndexException
   *          if OpenSearch refuses to create the index
   * @throws IOException
   *          if the request fails
   */
  public void createIndexIfMissing(String name, String settings, String mapping)
          throws SearchIndexException, IOException {
    logger.debug("Trying to create index '{}'", name);
    final String body = "{\"settings\": " + settings + ", \"mappings\": " + mapping + "}";
    try (Response response = client.generic()
        .execute(Requests.builder().method("PUT").endpoint("/" + name).json(body).build())) {
      if (response.getStatus() >= 400) {
        final String error = response.getBody().map(Body::bodyAsString).orElse("");
        if (!error.contains("resource_already_exists_exception")) {
          throw new SearchIndexException("Unable to create index '" + name + "': " + error);
        }
        logger.info("Detected existing index '{}'", name);
      }
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
    final String resourcePath = "/opensearch/" + filename;
    // Try loading from the index implementation first.
    // This allows index implementations to override the defaults
    for (Class cls : Arrays.asList(this.getClass(), AbstractOpenSearchIndex.class)) {
      try (InputStream is = cls.getResourceAsStream(resourcePath)) {
        if (is != null) {
          final String settings = IOUtils.toString(is, StandardCharsets.UTF_8);
          logger.debug("Reading opensearch configuration resources from {}:\n{}", cls, settings);
          return settings;
        }
      }
    }
    return null;
  }

  /**
   * Returns the name of this index.
   *
   * @return the index name
   */
  public String getIndexName() {
    return indexName;
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

  /**
   * Returns the OpenSearch client.
   *
   * @return the client
   */
  public OpenSearchClient getClient() {
    return client;
  }

  /**
   * Executes a search request.
   *
   * @param request
   *          The search request
   * @param toItem
   *          The function to convert the source of the found documents to result items
   * @param offset
   *          The offset of the query, to be returned with the result
   * @param limit
   *          The limit of the query, to be returned with the result
   * @return The search result.
   *
   * @throws IOException
   *         If querying the index fails
   * @throws InterruptedException
   *         If waiting during retry is interrupted
   */
  @SuppressWarnings("unchecked")
  protected <T> SearchResult<T> search(SearchRequest request, Function<Map<String, Object>, T> toItem, int offset,
          int limit) throws IOException, InterruptedException {
    final SearchResponse<Map> response;
    try {
      response = withRetries(c -> c.search(request, Map.class));
    } catch (OpenSearchException e) {
      if (e.status() == 400) {
        logger.warn("Could not query documents from index {} because of a bad request: {}", getIndexName(),
            e.getMessage());
      }
      throw e;
    }

    final List<T> items = new ArrayList<>();
    for (Hit<Map> hit : response.hits().hits()) {
      try {
        items.add(toItem.apply((Map<String, Object>) hit.source()));
      } catch (Throwable t) {
        logger.warn("Error during search result serialization: '{}'. Skipping this search result.", t.getMessage());
      }
    }
    final TotalHits total = response.hits().total();
    return new SearchResult<>(items, total != null ? total.value() : items.size(), offset, limit);
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
    final String name = "terms";
    // Add size to aggregation to return all values (the default is the top ten terms with the most documents).
    // We set it to 10,000, which should be enough (the maximum is 'search.max_buckets', which defaults to 65,536).
    try {
      final SearchResponse<Void> response = withRetries(c -> c.search(s -> s
          .index(getSubIndexIdentifier(type))
          .size(0)
          .aggregations(name, a -> a.terms(t -> t.field(field).size(OPENSEARCH_TERM_AGGREGATION_SIZE))),
          Void.class));
      final Aggregate terms = response.aggregations().get(name);
      if (terms.isSterms()) {
        return terms.sterms().buckets().array().stream().map(StringTermsBucket::key).toList();
      } else if (terms.isLterms()) {
        return terms.lterms().buckets().array().stream()
            .map(bucket -> bucket.keyAsString() != null ? bucket.keyAsString() : String.valueOf(bucket.key()))
            .toList();
      } else if (terms.isDterms()) {
        return terms.dterms().buckets().array().stream()
            .map(bucket -> bucket.keyAsString() != null ? bucket.keyAsString() : Double.toString(bucket.key()))
            .toList();
      }
      return List.of();
    } catch (IOException | InterruptedException e) {
      return chuck(e);
    }
  }
}
