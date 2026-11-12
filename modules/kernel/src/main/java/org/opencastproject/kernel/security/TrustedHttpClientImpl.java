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


package org.opencastproject.kernel.security;

import static org.opencastproject.kernel.rest.CurrentJobFilter.CURRENT_JOB_HEADER;
import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.DIGEST_AUTH;
import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.urlsigning.utils.ResourceRequestUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hc.client5.http.auth.AuthChallenge;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.AuthenticationException;
import org.apache.hc.client5.http.auth.ChallengeType;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.MalformedChallengeException;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.auth.AuthChallengeParser;
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder;
import org.apache.hc.client5.http.impl.auth.DigestScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.ParserCursor;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An http client that executes secure (though not necessarily encrypted) http requests.
 */
@Component(
    property = {
        "service.description=Provides Trusted Http Clients (for use with digest authentication)"
    },
    immediate = true,
    service = { TrustedHttpClient.class }
)
public class TrustedHttpClientImpl implements TrustedHttpClient {
  /** Header name used to request a new nonce from a server a request is sent to. */
  public static final String AUTHORIZATION_HEADER_NAME = HttpHeaders.AUTHORIZATION;

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TrustedHttpClientImpl.class);

  /** The configuration property specifying the digest authentication user */
  public static final String DIGEST_AUTH_USER_KEY = "org.opencastproject.security.digest.user";

  /** The configuration property specifying the digest authentication password */
  public static final String DIGEST_AUTH_PASS_KEY = "org.opencastproject.security.digest.pass";

  /** The configuration property specifying the number of times to retry after the nonce timesouts on a request. */
  public static final String NONCE_TIMEOUT_RETRY_KEY = "org.opencastproject.security.digest.nonce.retries";

  /** The configuration property specifying the duration a signed url will remain valid for. */
  protected static final String INTERNAL_URL_SIGNING_DURATION_KEY =
      "org.opencastproject.security.internal.url.signing.duration";

  /**
   * The configuration property specifying the minimum amount of time in seconds wait before retrying a request after a
   * nonce timeout.
   */
  public static final String NONCE_TIMEOUT_RETRY_BASE_TIME_KEY = "org.opencastproject.security.digest.nonce.base.time";

  /**
   * The configuration property specifying the maximum for a random amount of time in seconds above the base time to
   * wait.
   */
  public static final String NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY =
      "org.opencastproject.security.digest.nonce.variable.time";

  /** The configuration property specifying the maximum number of pooled connections per target host. */
  public static final String MAX_CONNECTIONS_PER_ROUTE_KEY =
      "org.opencastproject.security.http.max.connections.per.route";

  /** The configuration property specifying the maximum number of pooled connections in total. */
  public static final String MAX_CONNECTIONS_TOTAL_KEY = "org.opencastproject.security.http.max.connections";

  /** The default time until a connection attempt fails */
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;

  /** The default time between packets that causes a connection to fail */
  public static final int DEFAULT_SOCKET_TIMEOUT = 300 * 1000;

  /** The default number of times to attempt a request after it has failed due to a nonce expiring. */
  public static final int DEFAULT_NONCE_TIMEOUT_RETRIES = 12;

  /** The number of milliseconds in a single second. */
  private static final int MILLISECONDS_IN_SECONDS = 1000;

  /** The default amount of time to wait after a nonce timeout. */
  public static final int DEFAULT_RETRY_BASE_TIME = 300;

  /** Default maximum amount of time in a random range between 0 and this value to add to the base time. */
  public static final int DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME = 300;

  /** The default maximum number of pooled connections per target host. */
  public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 100;

  /** The default maximum number of pooled connections in total. */
  public static final int DEFAULT_MAX_CONNECTIONS_TOTAL = 400;

  /**
   * The default time before a piece of signed content expires. 1 Minute. These are internal calls to another server, if
   * we can't make the request in under a minute something has gone horribly wrong.
   */
  protected static final long DEFAULT_URL_SIGNING_EXPIRES_DURATION = 60;

  /** The configured username to send as part of the digest authenticated request */
  protected String user = null;

  /** The configured password to send as part of the digest authenticated request */
  protected String pass = null;

  /** The number of times to retry a request after a nonce timeout. */
  private int nonceTimeoutRetries = DEFAULT_NONCE_TIMEOUT_RETRIES;

  /** Used to add a random amount of time up to retryMaximumVariableTime to retry a request after a nonce timeout. */
  private final Random generator = new Random();

  /** The amount of time in seconds to wait until trying the request again. */
  private int retryBaseDelay = DEFAULT_RETRY_BASE_TIME;

  /** The maximum amount of time in seconds to wait in addition to the RETRY_BASE_DELAY. */
  private int retryMaximumVariableTime = DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME;

  /** The maximum number of pooled connections per target host. */
  private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;

  /** The maximum number of pooled connections in total. */
  private int maxConnectionsTotal = DEFAULT_MAX_CONNECTIONS_TOTAL;

  /** The duration a signed url will remain valid for. */
  private long signedUrlExpiresDuration = DEFAULT_URL_SIGNING_EXPIRES_DURATION;

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The url signing service */
  protected UrlSigningService urlSigningService = null;

  /** A regularly emptying cache of hosts in the cluster */
  private HostCache hosts = null;

  /** The connection pool backing the shared http client */
  protected PoolingHttpClientConnectionManager connectionManager = null;

  /** The shared http client used for all requests */
  private CloseableHttpClient httpClient = null;

  /** The digest credentials of this node, handed to the client per request when talking to cluster nodes */
  private CredentialsProvider digestCredentials = null;

  @Activate
  public void activate(ComponentContext cc) {
    logger.debug("activate");
    user = cc.getBundleContext().getProperty(DIGEST_AUTH_USER_KEY);
    pass = cc.getBundleContext().getProperty(DIGEST_AUTH_PASS_KEY);
    if (user == null || pass == null) {
      throw new IllegalStateException("trusted communication is not properly configured");
    }

    getRetryNumber(cc);
    getRetryBaseTime(cc);
    getRetryMaximumVariableTime(cc);
    maxConnectionsPerRoute = getIntFromComponentContext(cc, MAX_CONNECTIONS_PER_ROUTE_KEY,
        DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
    maxConnectionsTotal = getIntFromComponentContext(cc, MAX_CONNECTIONS_TOTAL_KEY, DEFAULT_MAX_CONNECTIONS_TOTAL);

    final Long expiration = NumberUtils.createLong(StringUtils.trimToNull(
        cc.getBundleContext().getProperty(INTERNAL_URL_SIGNING_DURATION_KEY)));
    if (expiration != null) {
      signedUrlExpiresDuration = expiration;
    } else {
      signedUrlExpiresDuration = DEFAULT_URL_SIGNING_EXPIRES_DURATION;
    }
    logger.debug("Expire signed URLs in {} seconds.", signedUrlExpiresDuration);

    getHttpClient();
  }

  @Deactivate
  public synchronized void deactivate() {
    logger.debug("deactivate");
    if (httpClient != null) {
      httpClient.close(CloseMode.GRACEFUL);
      httpClient = null;
      connectionManager = null;
    }
  }

  public TrustedHttpClientImpl() {

  }

  public TrustedHttpClientImpl(String user, String pass) {
    this.user = user;
    this.pass = pass;
  }

  private HostCache getHostCache() {
    if (null == hosts) {
      hosts = new HostCache(60000, this.organizationDirectoryService, this.serviceRegistry);
    }
    return hosts;
  }

  /**
   * Sets the service registry.
   *
   * @param serviceRegistry
   *         the serviceRegistry to set
   */
  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy =  ReferencePolicy.DYNAMIC,
      unbind = "unsetServiceRegistry")
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Unsets the service registry.
   *
   * @param serviceRegistry
   *         the serviceRegistry to unset (unused, but needed for OSGI)
   */
  public void unsetServiceRegistry(ServiceRegistry serviceRegistry) {
    if (this.serviceRegistry == serviceRegistry) {
      this.serviceRegistry = null;
    }
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *         the security service
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets the organization directory service.
   *
   * @param organizationDirectoryService
   *         the organization directory service
   */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * Sets the url signing service.
   *
   * @param urlSigningService
   *        The signing service to sign urls with.
   */
  @Reference
  public void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  /**
   * Extracts the number of times to retry a request after a nonce timeout.
   *
   * @param cc
   *         The ComponentContent to extract this property from.
   */
  private void getRetryNumber(ComponentContext cc) {
    nonceTimeoutRetries = getIntFromComponentContext(cc, NONCE_TIMEOUT_RETRY_KEY, DEFAULT_NONCE_TIMEOUT_RETRIES);
  }

  /**
   * Extracts the minimum amount of time in seconds to wait if there is a nonce timeout before retrying.
   *
   * @param cc
   *         The ComponentContent to extract this property from.
   */
  private void getRetryBaseTime(ComponentContext cc) {
    retryBaseDelay = getIntFromComponentContext(cc, NONCE_TIMEOUT_RETRY_BASE_TIME_KEY, DEFAULT_RETRY_BASE_TIME);
  }

  /**
   * Extracts the maximum amount of time in seconds that is added to the base time after a nonce timeout.
   *
   * @param cc
   *         The ComponentContent to extract this property from.
   */
  private void getRetryMaximumVariableTime(ComponentContext cc) {
    retryMaximumVariableTime = getIntFromComponentContext(cc, NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY,
                                                          DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME);
  }

  /**
   * Gets a property from the ComponentContext that is the base type int.
   *
   * @param cc
   *         The ComponentContext to get the property from.
   * @param key
   *         The key to search the properties for to get the value back.
   * @param defaultValue
   *         The default value to set if the property is malformed or non-existant.
   * @return The int property either as the value from the properties collection or the default value.
   */
  private int getIntFromComponentContext(ComponentContext cc, String key, int defaultValue) {
    int result;
    try {
      String stringValue = cc.getBundleContext().getProperty(key);
      result = Integer.parseInt(StringUtils.trimToNull(stringValue));
    } catch (Exception e) {
      if (cc != null && cc.getBundleContext() != null && cc.getBundleContext().getProperty(key) != null) {
        logger.info("Unable to get property with key " + key + " with value " + cc.getBundleContext().getProperty(key)
                            + " so using default of " + defaultValue + " because of " + e.getMessage());
      } else {
        logger.info("Unable to get property with key " + key + " so using default of " + defaultValue + " because of "
                            + e.getMessage());
      }
      result = defaultValue;
    }

    return result;
  }

  /**
   * Creates the builder for the shared http client. This is called exactly once; the timeouts of individual requests
   * are configured per request.
   */
  public HttpClientBuilder makeHttpClientBuilder() {
    connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .useSystemProperties()
        .setMaxConnPerRoute(maxConnectionsPerRoute)
        .setMaxConnTotal(maxConnectionsTotal)
        .setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(DEFAULT_CONNECTION_TIMEOUT))
            .setSocketTimeout(Timeout.ofMilliseconds(DEFAULT_SOCKET_TIMEOUT))
            .setValidateAfterInactivity(TimeValue.ofSeconds(2))
            .setTimeToLive(TimeValue.ofMinutes(5))
            .build())
        .build();
    return HttpClientBuilder.create()
        .setConnectionManager(connectionManager)
        .evictIdleConnections(TimeValue.ofMinutes(1))
        .evictExpiredConnections()
        .disableCookieManagement()
        // The default strategy also retries requests answered with 429 or 503 after a delay. Remote services rely on
        // seeing a 503 immediately, and a non-idempotent request must never be sent twice.
        .setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, TimeValue.ZERO_MILLISECONDS) {
          @Override
          public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
            return false;
          }
        })
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(DEFAULT_CONNECTION_TIMEOUT))
            .setResponseTimeout(Timeout.ofMilliseconds(DEFAULT_SOCKET_TIMEOUT))
            .build());
  }

  /** Returns the shared http client, creating it on first use. */
  protected synchronized CloseableHttpClient getHttpClient() {
    if (httpClient == null) {
      digestCredentials = CredentialsProviderBuilder.create()
          .add(new AuthScope(null, null, -1, null, StandardAuthScheme.DIGEST), user, pass.toCharArray())
          .build();
      httpClient = makeHttpClientBuilder().build();
    }
    return httpClient;
  }

  @Override
  public ClassicHttpResponse execute(ClassicHttpRequest request) throws TrustedHttpClientException {
    return execute(request, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
  }

  @Override
  public <T> T execute(ClassicHttpRequest request, HttpClientResponseHandler<? extends T> handler)
          throws TrustedHttpClientException {
    try (ClassicHttpResponse response = execute(request)) {
      return handler.handleResponse(response);
    } catch (IOException | HttpException e) {
      throw new TrustedHttpClientException(e);
    }
  }

  @Override
  public ClassicHttpResponse execute(ClassicHttpRequest request, int connectionTimeout, int socketTimeout)
          throws TrustedHttpClientException {
    if (serviceRegistry != null && serviceRegistry.getCurrentJob() != null) {
      request.setHeader(CURRENT_JOB_HEADER, Long.toString(serviceRegistry.getCurrentJob().getId()));
    }

    final URIAuthority authority = request.getAuthority();
    final boolean enableDigest = authority != null && getHostCache().contains(authority.getHostName());
    logger.debug("Digest auth enabled for this request: {}", enableDigest);
    if (enableDigest) {
      // Add the request header to elicit a digest auth response
      request.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
    }

    // If a security service has been set, use it to pass the current security context on
    logger.debug("Adding security context to request");
    final Organization organization = securityService.getOrganization();
    if (organization != null) {
      request.setHeader(SecurityConstants.ORGANIZATION_HEADER, organization.getId());
      final User currentUser = securityService.getUser();
      if (enableDigest && currentUser != null) {
        request.setHeader(SecurityConstants.USER_HEADER, currentUser.getUsername());
      }
    }

    final HttpClientContext context = HttpClientContext.create();
    context.setRequestConfig(RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeout))
        .setResponseTimeout(Timeout.ofMilliseconds(socketTimeout))
        .build());

    try {
      if (Method.isSafe(request.getMethod())) {
        // GET and HEAD carry no body, so the http client can handle the digest challenge itself
        request = getSignedUrl(request);
        if (enableDigest) {
          context.setCredentialsProvider(digestCredentials);
        }
        return send(request, context);
      }

      if (!enableDigest) {
        return send(request, context);
      }

      // The http client can not repeat a request with a streamed body after a challenge, so the digest handshake is
      // done manually before sending the actual request
      addDigestAuthorization(request, context);
      ClassicHttpResponse response = send(request, context);
      for (int i = 0; i < nonceTimeoutRetries && isStaleNonceResponse(response) && isRepeatable(request); i++) {
        response = retryAfterNonceTimeout(request, context, response);
      }
      return response;
    } catch (IOException e) {
      throw new TrustedHttpClientException(e);
    }
  }

  /**
   * Executes the request on the shared client. This is the single place requests are sent from.
   *
   * @param request
   *         The request to send
   * @param context
   *         The context holding the request configuration and credentials
   * @return The response, which must be closed by the caller
   */
  protected ClassicHttpResponse send(ClassicHttpRequest request, HttpClientContext context) throws IOException {
    return getHttpClient().executeOpen(null, request, context);
  }

  @Override
  public void close(ClassicHttpResponse response) throws IOException {
    if (response != null) {
      response.close();
    } else {
      logger.debug("Can not close a null response");
    }
  }

  /**
   * If the request is a GET or HEAD, sign the URL and return a new request that is signed.
   *
   * @param request
   *          The possible request to sign.
   * @return The signed request if the request is a GET or HEAD and is configured to be signed, the request otherwise.
   * @throws TrustedHttpClientException
   *           Thrown if there is a problem signing the URL.
   */
  protected ClassicHttpRequest getSignedUrl(ClassicHttpRequest request) throws TrustedHttpClientException {
    final URI uri;
    try {
      uri = request.getUri();
    } catch (URISyntaxException e) {
      throw new TrustedHttpClientException(e);
    }
    if (Method.isSafe(request.getMethod())
            && ResourceRequestUtil.isNotSigned(uri)
            && urlSigningService.accepts(uri.toString())) {
      logger.trace("Signing request with method: {} and URI: {}", request.getMethod(), uri);
      try {
        final String signedUrl = urlSigningService.sign(uri.toString(), signedUrlExpiresDuration, null, null);
        final HttpUriRequestBase signedRequest;
        if (Method.GET.isSame(request.getMethod())) {
          signedRequest = new HttpGet(signedUrl);
        } else {
          signedRequest = new HttpHead(signedUrl);
        }
        signedRequest.setVersion(request.getVersion());
        signedRequest.setHeaders(request.getHeaders());
        if (request instanceof Configurable) {
          signedRequest.setConfig(((Configurable) request).getConfig());
        }
        return signedRequest;
      } catch (UrlSigningException e) {
        throw new TrustedHttpClientException(e);
      }
    }
    return request;
  }

  /**
   * Retries a request once after the nonce timed out during the request, waiting the configured delay first.
   *
   * @param request
   *         The request to be made that isn't a GET or HEAD, those are handled automatically.
   * @param context
   *         The request context
   * @param response
   *         The response with the stale nonce in it. It is closed by this method.
   * @return The new response
   */
  private ClassicHttpResponse retryAfterNonceTimeout(ClassicHttpRequest request, HttpClientContext context,
          ClassicHttpResponse response) throws TrustedHttpClientException, IOException {
    final String status = new StatusLine(response).toString();
    close(response);

    int variableDelay = 0;
    // Make sure that we have a variable delay greater than 0.
    if (retryMaximumVariableTime > 0) {
      variableDelay = generator.nextInt(retryMaximumVariableTime * MILLISECONDS_IN_SECONDS);
    }
    final long totalDelay = (retryBaseDelay * (long) MILLISECONDS_IN_SECONDS + variableDelay);
    if (totalDelay > 0) {
      logger.info("Sleeping {}ms before trying request {} again due to a stale nonce ({})", totalDelay,
          request.getRequestUri(), status);
      try {
        Thread.sleep(totalDelay);
      } catch (InterruptedException e) {
        logger.error("Suffered InteruptedException while trying to sleep until next retry.", e);
      }
    }

    // Get rid of old security headers with the old nonce.
    request.removeHeaders(AUTHORIZATION_HEADER_NAME);
    addDigestAuthorization(request, context);
    return send(request, context);
  }

  /**
   * Determines if the nonce has timed out before a request could be performed, i.e. if the server answered with a
   * digest challenge marked as stale.
   *
   * @param response
   *         The response to test to see if it has timed out.
   * @return true if it has time out, false if it hasn't
   */
  private static boolean isStaleNonceResponse(ClassicHttpResponse response) {
    if (response.getCode() != HttpStatus.SC_UNAUTHORIZED) {
      return false;
    }
    for (AuthChallenge challenge : parseDigestChallenges(response)) {
      for (NameValuePair param : challenge.getParams()) {
        if ("stale".equalsIgnoreCase(param.getName()) && "true".equalsIgnoreCase(param.getValue())) {
          return true;
        }
      }
    }
    return false;
  }

  /** Whether the request can be sent a second time. */
  private static boolean isRepeatable(ClassicHttpRequest request) {
    final HttpEntity entity = request.getEntity();
    return entity == null || entity.isRepeatable();
  }

  /** Extracts all digest challenges from the WWW-Authenticate headers of a response. */
  private static List<AuthChallenge> parseDigestChallenges(HttpResponse response) {
    final List<AuthChallenge> challenges = new ArrayList<>();
    for (Header header : response.getHeaders(HttpHeaders.WWW_AUTHENTICATE)) {
      final String value = header.getValue();
      if (value == null) {
        continue;
      }
      try {
        for (AuthChallenge challenge : AuthChallengeParser.INSTANCE.parse(ChallengeType.TARGET, value,
                new ParserCursor(0, value.length()))) {
          if (StandardAuthScheme.DIGEST.equalsIgnoreCase(challenge.getSchemeName())) {
            challenges.add(challenge);
          }
        }
      } catch (ParseException e) {
        logger.warn("Ignoring malformed authentication challenge '{}'", value);
      }
    }
    return challenges;
  }

  /**
   * Handles the necessary handshake for digest authentication in the case where it isn't a GET or HEAD operation: a
   * request without body is sent to obtain the challenge, from which the authorization header of the actual request
   * is computed.
   *
   * @param request
   *         The request to add the authorization header to.
   * @param context
   *         The request context.
   */
  private void addDigestAuthorization(ClassicHttpRequest request, HttpClientContext context)
          throws TrustedHttpClientException {
    final AuthChallenge challenge = probeDigestChallenge(request);
    if (challenge == null) {
      return;
    }
    try {
      final HttpHost host = HttpHost.create(request.getUri());
      final DigestScheme digestAuth = new DigestScheme();
      digestAuth.processChallenge(challenge, context);
      if (!digestAuth.isResponseReady(host, digestCredentials, context)) {
        throw new TrustedHttpClientException(new IllegalStateException("No digest credentials available for " + host));
      }
      request.setHeader(AUTHORIZATION_HEADER_NAME, digestAuth.generateAuthResponse(host, request, context));
    } catch (URISyntaxException | MalformedChallengeException | AuthenticationException e) {
      throw new TrustedHttpClientException(e);
    }
  }

  /**
   * Perform a request without body in order to obtain the digest challenge of the server.
   *
   * @param request
   *         The request whose target to obtain the challenge for
   * @return The digest challenge, or null if the server did not ask for digest authentication
   */
  protected AuthChallenge probeDigestChallenge(ClassicHttpRequest request) throws TrustedHttpClientException {
    final BasicClassicHttpRequest probe;
    try {
      probe = new BasicClassicHttpRequest(request.getMethod(), request.getUri());
    } catch (URISyntaxException e) {
      throw new TrustedHttpClientException(e);
    }
    probe.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
    try (ClassicHttpResponse response = send(probe, HttpClientContext.create())) {
      EntityUtils.consumeQuietly(response.getEntity());
      final List<AuthChallenge> challenges = parseDigestChallenges(response);
      if (challenges.isEmpty()) {
        logger.warn("URI {} does not support digest authentication", probe.getRequestUri());
        return null;
      }
      return challenges.get(0);
    } catch (IOException e) {
      throw new TrustedHttpClientException(e);
    }
  }

  /**
   * @return Returns the number of times the TrustedHttpClient will retry a request if nonce timeouts are occuring.
   */
  public int getNonceTimeoutRetries() {
    return nonceTimeoutRetries;
  }

  /**
   * @return The minimum amount of time to wait in seconds after a nonce timeout before retrying.
   */
  public int getRetryBaseDelay() {
    return retryBaseDelay;
  }

  /**
   * @return The maximum amount of time to wait in seconds after a nonce timeout in addition to the base delay.
   */
  public int getRetryMaximumVariableTime() {
    return retryMaximumVariableTime;
  }

  /**
   * Very simple cache that does a <em>complete</em> refresh after a given interval. This type of cache is only suitable
   * for small sets.
   */
  private static final class HostCache {
    private final Object lock = new Object();

    // A simple hash map is sufficient here.
    // No need to deal with soft references or an LRU map since the number of organizations
    // will be quite low.
    private final Set<String> hosts = new HashSet<>();
    private final long refreshInterval;
    private long lastRefresh;

    private final OrganizationDirectoryService organizationDirectoryService;
    private final ServiceRegistry serviceRegistry;

    HostCache(long refreshInterval, OrganizationDirectoryService orgDirSrv, ServiceRegistry serviceReg) {
      this.refreshInterval = refreshInterval;
      this.organizationDirectoryService = orgDirSrv;
      this.serviceRegistry = serviceReg;
      invalidate();
    }

    public boolean contains(String host) {
      synchronized (lock) {
        try {
          refresh();
        } catch (ServiceRegistryException e) {
          logger.error("Unable to update host cache due to service registry exception!", e);
        }
        return hosts.contains(host);
      }
    }
    public void invalidate() {
      this.lastRefresh = System.currentTimeMillis() - 2 * refreshInterval;
    }

    private void refresh() throws ServiceRegistryException {
      final long now = System.currentTimeMillis();
      if (now - lastRefresh > refreshInterval) {
        hosts.clear();
        //The hosts from the SR come back as protocol://hostname:port, use the URI class to get just the host
        hosts.addAll(serviceRegistry.getHostRegistrations().stream()
            .map(HostRegistration::getBaseUrl)
            .map(URI::create)
            .map(URI::getHost)
            .collect(Collectors.toSet()));
        //This is the *org's* server urls, as defined by in the org config with
        // something like prop.org.opencastproject.host.$SERVER_URL=$TENANT_URL
        // You're getting just the hostname of the tenant URL
        hosts.addAll(organizationDirectoryService.getOrganizations().stream()
            .map(Organization::getServers)
            .map(Map::keySet)
            .flatMap(Collection::stream).collect(Collectors.toSet()));
        lastRefresh = now;
      }
    }
  }
}
