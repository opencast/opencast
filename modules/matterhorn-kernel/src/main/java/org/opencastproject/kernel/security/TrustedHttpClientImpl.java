/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.kernel.security;

import static org.opencastproject.kernel.rest.CurrentJobFilter.CURRENT_JOB_HEADER;
import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.DIGEST_AUTH;
import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER;

import org.opencastproject.kernel.http.api.HttpClient;
import org.opencastproject.kernel.http.impl.HttpClientFactory;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.StandAloneTrustedHttpClientImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.params.CoreConnectionPNames;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * An http client that executes secure (though not necessarily encrypted) http requests.
 */
public class TrustedHttpClientImpl implements TrustedHttpClient, HttpConnectionMXBean {
  /** Header name used to request a new nonce from a server a request is sent to. */
  public static final String AUTHORIZATION_HEADER_NAME = "Authorization";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TrustedHttpClientImpl.class);

  /** The configuration property specifying the digest authentication user */
  public static final String DIGEST_AUTH_USER_KEY = "org.opencastproject.security.digest.user";

  /** The configuration property specifying the digest authentication password */
  public static final String DIGEST_AUTH_PASS_KEY = "org.opencastproject.security.digest.pass";

  /** The configuration property specifying the number of times to retry after the nonce timesouts on a request. */
  public static final String NONCE_TIMEOUT_RETRY_KEY = "org.opencastproject.security.digest.nonce.retries";

  /**
   * The configuration property specifying the minimum amount of time in seconds wait before retrying a request after a
   * nonce timeout.
   */
  public static final String NONCE_TIMEOUT_RETRY_BASE_TIME_KEY = "org.opencastproject.security.digest.nonce.base.time";

  /**
   * The configuration property specifying the maximum for a random amount of time in seconds above the base time to
   * wait.
   */
  public static final String NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY = "org.opencastproject.security.digest.nonce.variable.time";

  /** The default time until a connection attempt fails */
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;

  /** The default time between packets that causes a connection to fail */
  public static final int DEFAULT_SOCKET_TIMEOUT = DEFAULT_CONNECTION_TIMEOUT;

  /** The default number of times to attempt a request after it has failed due to a nonce expiring. */
  public static final int DEFAULT_NONCE_TIMEOUT_RETRIES = 12;

  /** The number of milliseconds in a single second. */
  private static final int MILLISECONDS_IN_SECONDS = 1000;

  /** The default amount of time to wait after a nonce timeout. */
  public static final int DEFAULT_RETRY_BASE_TIME = 300;

  /** Default maximum amount of time in a random range between 0 and this value to add to the base time. */
  public static final int DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME = 300;

  /** The configured username to send as part of the digest authenticated request */
  protected String user = null;

  /** The configured password to send as part of the digest authenticated request */
  protected String pass = null;

  /** The number of times to retry a request after a nonce timeout. */
  private int nonceTimeoutRetries = DEFAULT_NONCE_TIMEOUT_RETRIES;

  /** The map of open responses to their http clients, which need to be closed after we are finished with the response */
  protected Map<HttpResponse, HttpClient> responseMap = new ConcurrentHashMap<HttpResponse, HttpClient>();

  /** Used to add a random amount of time up to retryMaximumVariableTime to retry a request after a nonce timeout. */
  private Random generator = new Random();

  /** Used to create HttpClients that are used to make http requests. */
  private HttpClientFactory httpClientFactory = null;

  /** The amount of time in seconds to wait until trying the request again. */
  private int retryBaseDelay = 300;

  /** The maximum amount of time in seconds to wait in addition to the RETRY_BASE_DELAY. */
  private int retryMaximumVariableTime = 300;

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  /** The security service */
  protected SecurityService securityService = null;

  public void activate(ComponentContext cc) {
    logger.debug("activate");
    user = cc.getBundleContext().getProperty(DIGEST_AUTH_USER_KEY);
    pass = cc.getBundleContext().getProperty(DIGEST_AUTH_PASS_KEY);
    if (user == null || pass == null)
      throw new IllegalStateException("trusted communication is not properly configured");

    getRetryNumber(cc);
    getRetryBaseTime(cc);
    getRetryMaximumVariableTime(cc);

    // register with jmx
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName name;
      name = new ObjectName("org.opencastproject.security.api.TrustedHttpClient:type=HttpConnections");
      Object mbean = this;
      mbs.registerMBean(mbean, name);
    } catch (Exception e) {
      logger.warn("Unable to register {} as an mbean: {}", this, e);
    }
  }

  /**
   * Sets the service registry.
   * 
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the security service.
   * 
   * @param securityService
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Extracts the number of times to retry a request after a nonce timeout.
   * 
   * @param cc
   *          The ComponentContent to extract this property from.
   */
  private void getRetryNumber(ComponentContext cc) {
    nonceTimeoutRetries = getIntFromComponentContext(cc, NONCE_TIMEOUT_RETRY_KEY, DEFAULT_NONCE_TIMEOUT_RETRIES);
  }

  /**
   * Extracts the minimum amount of time in seconds to wait if there is a nonce timeout before retrying.
   * 
   * @param cc
   *          The ComponentContent to extract this property from.
   */
  private void getRetryBaseTime(ComponentContext cc) {
    retryBaseDelay = getIntFromComponentContext(cc, NONCE_TIMEOUT_RETRY_BASE_TIME_KEY, DEFAULT_RETRY_BASE_TIME);
  }

  /**
   * Extracts the maximum amount of time in seconds that is added to the base time after a nonce timeout.
   * 
   * @param cc
   *          The ComponentContent to extract this property from.
   */
  private void getRetryMaximumVariableTime(ComponentContext cc) {
    retryMaximumVariableTime = getIntFromComponentContext(cc, NONCE_TIMEOUT_RETRY_MAXIMUM_VARIABLE_TIME_KEY,
            DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME);
  }

  /**
   * Gets a property from the ComponentContext that is the base type int.
   * 
   * @param cc
   *          The ComponentContext to get the property from.
   * @param key
   *          The key to search the properties for to get the value back.
   * @param defaultValue
   *          The default value to set if the property is malformed or non-existant.
   * @return The int property either as the value from the properties collection or the default value.
   */
  private int getIntFromComponentContext(ComponentContext cc, String key, int defaultValue) {
    int result;
    try {
      String stringValue = cc.getBundleContext().getProperty(key);
      result = Integer.parseInt(StringUtils.trimToNull(stringValue));
    } catch (Exception e) {
      if (cc != null && cc.getBundleContext() != null && cc.getBundleContext().getProperty(key) != null) {
        logger.warn("Unable to get property with key " + key + " with value " + cc.getBundleContext().getProperty(key)
                + " so using default of " + defaultValue + " because of " + e.getMessage());
      } else {
        logger.warn("Unable to get property with key " + key + " so using default of " + defaultValue + " because of "
                + e.getMessage());
      }
      result = defaultValue;
    }

    return result;
  }

  public void deactivate() {
    logger.debug("deactivate");
  }

  public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
    this.httpClientFactory = httpClientFactory;
  }

  public void unsetHttpClientFactory(HttpClientFactory defaultHttpClientFactory) {
    defaultHttpClientFactory = null;
  }

  public TrustedHttpClientImpl() {
  }

  public TrustedHttpClientImpl(String user, String pass) {
    this.user = user;
    this.pass = pass;
  }

  /** Creates a new HttpClient to use to make requests. */
  public HttpClient makeHttpClient() throws TrustedHttpClientException {
    if (httpClientFactory == null) {
      throw new TrustedHttpClientException(new NullPointerException(
              "There is no DefaultHttpClientFactory service available so we cannot make a request"));
    }
    return httpClientFactory.makeHttpClient();
  }

  @Override
  public <A> Function<Function<HttpResponse, A>, Either<Exception, A>> run(final HttpUriRequest httpUriRequest) {
    return StandAloneTrustedHttpClientImpl.run(this, httpUriRequest);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.TrustedHttpClient#execute(org.apache.http.client.methods.HttpUriRequest)
   */
  @Override
  public HttpResponse execute(HttpUriRequest httpUriRequest) throws TrustedHttpClientException {
    return execute(httpUriRequest, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
  }

  @Override
  public HttpResponse execute(HttpUriRequest httpUriRequest, int connectionTimeout, int socketTimeout)
          throws TrustedHttpClientException {
    HttpClient httpClient = makeHttpClient();
    httpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
    // Add the request header to elicit a digest auth response
    httpUriRequest.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
    httpUriRequest.setHeader(SecurityConstants.AUTHORIZATION_HEADER, "true");

    if (serviceRegistry != null && serviceRegistry.getCurrentJob() != null)
      httpUriRequest.setHeader(CURRENT_JOB_HEADER, Long.toString(serviceRegistry.getCurrentJob().getId()));

    // If a security service has been set, use it to pass the current security context on
    logger.debug("Adding security context to request");
    Organization organization = securityService.getOrganization();
    if (organization != null) {
      httpUriRequest.setHeader(SecurityConstants.ORGANIZATION_HEADER, organization.getId());
      User currentUser = securityService.getUser();
      if (currentUser != null)
        httpUriRequest.setHeader(SecurityConstants.USER_HEADER, currentUser.getUsername());
    }

    if ("GET".equalsIgnoreCase(httpUriRequest.getMethod()) || "HEAD".equalsIgnoreCase(httpUriRequest.getMethod())) {
      // Set the user/pass
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);
      httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

      // Run the request (the http client handles the multiple back-and-forth requests)
      HttpResponse response = null;
      try {
        response = httpClient.execute(httpUriRequest);
        responseMap.put(response, httpClient);
        return response;
      } catch (IOException e) {
        // close the http connection(s)
        httpClient.getConnectionManager().shutdown();
        throw new TrustedHttpClientException(e);
      }
    }

    // HttpClient doesn't handle the request dynamics for other verbs (especially when sending a streamed multipart
    // request), so we need to handle the details of the digest auth back-and-forth manually
    manuallyHandleDigestAuthentication(httpUriRequest, httpClient);

    HttpResponse response = null;
    try {
      response = httpClient.execute(httpUriRequest);
      if (nonceTimeoutRetries > 0 && hadNonceTimeoutResponse(response)) {
        httpClient.getConnectionManager().shutdown();
        response = retryAuthAndRequestAfterNonceTimeout(httpUriRequest, response);
      }
      responseMap.put(response, httpClient);
      return response;
    } catch (Exception e) {
      // if we have a response, remove it from the map
      if (response != null) {
        responseMap.remove(response);
      }
      // close the http connection(s)
      httpClient.getConnectionManager().shutdown();
      throw new TrustedHttpClientException(e);
    }
  }

  /**
   * Retries a request if the nonce timed out during the request.
   * 
   * @param httpUriRequest
   *          The request to be made that isn't a GET, those are handled automatically.
   * @param response
   *          The response with the bad nonce timeout in it.
   * @return A new response for the request if it was successful without the nonce timing out again or just the same
   *         response it got if it ran out of attempts.
   * @throws TrustedHttpClientException
   * @throws IOException
   * @throws ClientProtocolException
   */
  private HttpResponse retryAuthAndRequestAfterNonceTimeout(HttpUriRequest httpUriRequest, HttpResponse response)
          throws TrustedHttpClientException, IOException, ClientProtocolException {
    // Get rid of old security headers with the old nonce.
    httpUriRequest.removeHeaders(AUTHORIZATION_HEADER_NAME);

    for (int i = 0; i < nonceTimeoutRetries; i++) {
      HttpClient httpClient = makeHttpClient();
      int variableDelay = 0;
      // Make sure that we have a variable delay greater than 0.
      if (retryMaximumVariableTime > 0) {
        variableDelay = generator.nextInt(retryMaximumVariableTime * MILLISECONDS_IN_SECONDS);
      }

      long totalDelay = (retryBaseDelay * MILLISECONDS_IN_SECONDS + variableDelay);
      if (totalDelay > 0) {
        logger.info("Sleeping " + totalDelay + "ms before trying request " + httpUriRequest.getURI()
                + " again due to a " + response.getStatusLine());
        try {
          Thread.sleep(totalDelay);
        } catch (InterruptedException e) {
          logger.error("Suffered InteruptedException while trying to sleep until next retry.", e);
        }
      }
      manuallyHandleDigestAuthentication(httpUriRequest, httpClient);
      response = httpClient.execute(httpUriRequest);
      if (!hadNonceTimeoutResponse(response)) {
        responseMap.put(response, httpClient);
        break;
      }
      httpClient.getConnectionManager().shutdown();
    }
    return response;
  }

  /**
   * Determines if the nonce has timed out before a request could be performed.
   * 
   * @param response
   *          The response to test to see if it has timed out.
   * @return true if it has time out, false if it hasn't
   */
  private boolean hadNonceTimeoutResponse(HttpResponse response) {
    return (401 == response.getStatusLine().getStatusCode())
            && ("Nonce has expired/timed out".equals(response.getStatusLine().getReasonPhrase()));
  }

  /**
   * Handles the necessary handshake for digest authenticaion in the case where it isn't a GET operation.
   * 
   * @param httpUriRequest
   *          The request location to get the digest authentication for.
   * @param httpClient
   *          The client to send the request through.
   * @throws TrustedHttpClientException
   *           Thrown if the client cannot be shutdown.
   */
  private void manuallyHandleDigestAuthentication(HttpUriRequest httpUriRequest, HttpClient httpClient)
          throws TrustedHttpClientException {
    HttpRequestBase digestRequest;
    try {
      digestRequest = (HttpRequestBase) httpUriRequest.getClass().newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Can not create a new " + httpUriRequest.getClass().getName());
    }
    digestRequest.setURI(httpUriRequest.getURI());
    digestRequest.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
    String[] realmAndNonce = getRealmAndNonce(digestRequest);

    if (realmAndNonce != null) {
      // Set the user/pass
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);

      // Set up the digest authentication with the required values
      DigestScheme digestAuth = new DigestScheme();
      digestAuth.overrideParamter("realm", realmAndNonce[0]);
      digestAuth.overrideParamter("nonce", realmAndNonce[1]);

      // Add the authentication header
      try {
        httpUriRequest.setHeader(digestAuth.authenticate(creds, httpUriRequest));
      } catch (Exception e) {
        // close the http connection(s)
        httpClient.getConnectionManager().shutdown();
        throw new TrustedHttpClientException(e);
      }
    }
  }

  @Override
  public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler, int connectionTimeout,
          int socketTimeout) throws TrustedHttpClientException {
    try {
      return responseHandler.handleResponse(execute(httpUriRequest, connectionTimeout, socketTimeout));
    } catch (IOException e) {
      throw new TrustedHttpClientException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.TrustedHttpClient#close(org.apache.http.HttpResponse)
   */
  @Override
  public void close(HttpResponse response) {
    if (response == null) {
      logger.debug("Can not close a null response");
    } else {
      HttpClient httpClient = responseMap.remove(response);
      if (httpClient != null) {
        httpClient.getConnectionManager().shutdown();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.TrustedHttpClient#execute(org.apache.http.client.methods.HttpUriRequest,
   *      org.apache.http.client.ResponseHandler)
   */
  @Override
  public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler)
          throws TrustedHttpClientException {
    return execute(httpUriRequest, responseHandler, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
  }

  /**
   * Perform a request, and extract the realm and nonce values
   * 
   * @param request
   *          The request to execute in order to obtain the realm and nonce
   * @return A String[] containing the {realm, nonce}
   */
  protected String[] getRealmAndNonce(HttpRequestBase request) throws TrustedHttpClientException {
    HttpClient httpClient = makeHttpClient();
    HttpResponse response;
    try {
      response = httpClient.execute(request);
    } catch (IOException e) {
      httpClient.getConnectionManager().shutdown();
      throw new TrustedHttpClientException(e);
    }
    Header[] headers = response.getHeaders("WWW-Authenticate");
    if (headers == null || headers.length == 0) {
      logger.warn("URI {} does not support digest authentication", request.getURI());
      httpClient.getConnectionManager().shutdown();
      return null;
    }
    Header authRequiredResponseHeader = headers[0];
    String nonce = null;
    String realm = null;
    for (HeaderElement element : authRequiredResponseHeader.getElements()) {
      if ("nonce".equals(element.getName())) {
        nonce = element.getValue();
      } else if ("Digest realm".equals(element.getName())) {
        realm = element.getValue();
      }
    }
    httpClient.getConnectionManager().shutdown();
    return new String[] { realm, nonce };
  }

  @Override
  public int getOpenConnections() {
    return responseMap.size();
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

}
