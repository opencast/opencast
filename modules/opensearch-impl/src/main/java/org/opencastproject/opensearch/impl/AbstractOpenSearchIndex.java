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
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.NoHttpResponseException;
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
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.client.transport.httpclient5.ResponseException;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A search index implementation based on OpenSearch.
 */
public abstract class AbstractOpenSearchIndex {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractOpenSearchIndex.class);

  /** The OpenSearch term aggregation size */
  private static final int OPENSEARCH_TERM_AGGREGATION_SIZE = 10000;

  /**
   * Configuration key defining the URLs of the OpenSearch nodes to connect to, separated by commas. All nodes have to
   * belong to the same cluster. Requests are distributed over the nodes, and a node which cannot be reached is skipped
   * until it is back.
   */
  public static final String OPENSEARCH_SERVER_URL_KEY = "org.opencastproject.opensearch.server.url";

  /**
   * Configuration keys replaced by {@link #OPENSEARCH_SERVER_URL_KEY}. Opencast refuses to start if they are still set.
   * This check can be removed with Opencast 24.
   */
  private static final List<String> REMOVED_SERVER_KEYS = List.of(
      "org.opencastproject.opensearch.server.hostname",
      "org.opencastproject.opensearch.server.scheme",
      "org.opencastproject.opensearch.server.port");

  /** Configuration key defining the username of an external OpenSearch server */
  public static final String OPENSEARCH_USERNAME_KEY = "org.opencastproject.opensearch.username";

  /** Configuration key defining the password of an external OpenSearch server */
  public static final String OPENSEARCH_PASSWORD_KEY = "org.opencastproject.opensearch.password";

  /** Default URL of the OpenSearch server */
  private static final String OPENSEARCH_SERVER_URL_DEFAULT = "http://localhost:9200";

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

  /** The OpenSearch nodes to connect to */
  private List<HttpHost> hosts = List.of();

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

    for (String key : REMOVED_SERVER_KEYS) {
      if (bundleContext.getProperty(key) != null) {
        throw new ComponentException("Configuration key " + key + " is no longer supported. Remove it and use "
            + OPENSEARCH_SERVER_URL_KEY + " instead.");
      }
    }
    final String urls = StringUtils.defaultIfBlank(bundleContext.getProperty(OPENSEARCH_SERVER_URL_KEY),
        OPENSEARCH_SERVER_URL_DEFAULT);
    final List<HttpHost> configuredHosts = new ArrayList<>();
    for (String url : StringUtils.split(urls, ", ")) {
      try {
        configuredHosts.add(HttpHost.create(StringUtils.stripEnd(url, "/")));
      } catch (URISyntaxException e) {
        throw new ComponentException("Invalid OpenSearch URL '" + url + "' in " + OPENSEARCH_SERVER_URL_KEY
            + ". Use scheme, host and port only, paths are not supported.", e);
      }
    }
    hosts = List.copyOf(configuredHosts);
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
   * Executes a request, retrying it as configured if it fails temporarily (see {@link #isTemporaryFailure(Exception)}).
   * Other errors, like bad requests or missing documents, are not retried. All requests Opencast sends are idempotent,
   * so retrying them is safe.
   *
   * @param request
   *          the request to execute
   * @return the response
   * @throws IOException
   *          if the request fails because of a connection problem, or OpenSearch reports an error without a
   *          structured error response (e.g. too many requests or a server error), and all retries are used up
   * @throws OpenSearchException
   *          if OpenSearch reports an error with a structured error response (e.g. bad request, not found)
   * @throws InterruptedException
   *          if waiting for the next attempt is interrupted
   */
  public <T> T withRetries(Request<T> request) throws IOException, InterruptedException {
    for (int attempt = 1; ; attempt++) {
      try {
        return request.execute(client);
      } catch (OpenSearchException | IOException e) {
        if (attempt > maxRetryAttempts || !isTemporaryFailure(e)) {
          throw e;
        }
        logger.warn("Request to OpenSearch index {} failed ({}), retrying in {} ms.", getIndexName(),
            e.getMessage(), retryWaitingPeriod);
        Thread.sleep(retryWaitingPeriod);
      }
    }
  }

  /**
   * The HTTP status code of a failed request to OpenSearch.
   * <p>
   * The client reports errors in different ways depending on the status code: errors with a structured error response
   * (400, 404, 405) as {@link OpenSearchException}, all others as {@link ResponseException}. Some of them, e.g. 401
   * and 403, are additionally wrapped in other exceptions.
   *
   * @param e
   *          the exception thrown by the client
   * @return the HTTP status code, or -1 if the exception does not stem from an HTTP error response
   */
  protected static int httpStatus(Exception e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof OpenSearchException openSearchException) {
        return openSearchException.status();
      }
      if (t instanceof ResponseException responseException) {
        return responseException.status();
      }
    }
    return -1;
  }

  /**
   * Whether a request failed for a reason which may go away by itself, so that trying again makes sense: OpenSearch
   * cannot be reached, the connection breaks or times out, or OpenSearch reports a timeout, too many requests or a
   * server error.
   *
   * @param e
   *          the exception thrown by the client
   * @return whether the failure is temporary
   */
  static boolean isTemporaryFailure(Exception e) {
    final int status = httpStatus(e);
    if (status >= 0) {
      return status == 408 || status == 429 || status >= 500;
    }
    for (Throwable t = e; t != null; t = t.getCause()) {
      // SocketException includes refused and reset connections
      if (t instanceof SocketException || t instanceof SocketTimeoutException || t instanceof ConnectTimeoutException
          || t instanceof ConnectionClosedException || t instanceof NoHttpResponseException) {
        return true;
      }
    }
    return false;
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
      final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder
          .builder(hosts.toArray(new HttpHost[0]))
          .setMapper(new JacksonJsonpMapper());
      if (username != null && password != null) {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        for (HttpHost host : hosts) {
          credentialsProvider.setCredentials(new AuthScope(host),
              new UsernamePasswordCredentials(username, password.toCharArray()));
        }
        builder.setHttpClientConfigCallback(
            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
      }
      client = new OpenSearchClient(builder.build());
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
   * @return true if OpenSearch is reachable, false if it is not (yet) available
   *
   * @throws RuntimeException if OpenSearch rejects the request, e.g. because of wrong credentials, or an unexpected
   *           exception occurs
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
    } catch (IOException | OpenSearchException e) {
      if (isTemporaryFailure(e)) {
        // Normally this should only happen when OpenSearch is not running or is just starting up, therefore only log
        // the error on debug level and continue the loop
        logger.debug("OpenSearch is not reachable or not ready yet", e);
        return false;
      }
      final int status = httpStatus(e);
      if (status == 401 || status == 403) {
        // We want the startup to fail, so that the configuration can be fixed
        logger.error("OpenSearch rejected the connection with HTTP status code {}. Check {} and {}.", status,
            OPENSEARCH_USERNAME_KEY, OPENSEARCH_PASSWORD_KEY);
        throw new RuntimeException("OpenSearch rejected the connection with HTTP status code " + status, e);
      }
      // Something else went wrong, e.g. OpenSearch rejected the request, so we fail
      logger.error("Error while testing OpenSearch connection", e);
      throw new RuntimeException("Unable to connect to OpenSearch", e);
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
    if (client != null) {
      client._transport().close();
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
    return hosts.stream().map(HttpHost::toURI).collect(Collectors.joining(", "));
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
