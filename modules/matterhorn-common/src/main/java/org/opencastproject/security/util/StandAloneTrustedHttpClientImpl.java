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
package org.opencastproject.security.util;

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An http client that executes secure (though not necessarily encrypted) http requests. Unlike the original
 * TrustedHttpClientImpl this version is not bound to an OSGi environment.
 */
public final class StandAloneTrustedHttpClientImpl implements TrustedHttpClient {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(StandAloneTrustedHttpClientImpl.class);

  /** Header name used to request a new nonce from a server a request is sent to. */
  public static final String AUTHORIZATION_HEADER_NAME = "Authorization";

  public static final String REQUESTED_AUTH_HEADER = "X-Requested-Auth";
  public static final String DIGEST_AUTH = "Digest";

  /** The default time until a connection attempt fails */
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;

  /** The default time between packets that causes a connection to fail */
  public static final int DEFAULT_SOCKET_TIMEOUT = DEFAULT_CONNECTION_TIMEOUT;

  /** The default number of times to attempt a request after it has failed due to a nonce expiring. */
  public static final int DEFAULT_NONCE_TIMEOUT_RETRIES = 3;

  /** The number of milliseconds in a single second. */
  private static final int MILLISECONDS_IN_SECONDS = 1000;

  /** The default amount of time to wait after a nonce timeout. */
  public static final int DEFAULT_RETRY_BASE_DELAY = 300;

  /** Default maximum amount of time in a random range between 0 and this value to add to the base time. */
  public static final int DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME = 300;

  /** The configured username to send as part of the digest authenticated request */
  private final String user;

  /** The configured password to send as part of the digest authenticated request */
  private final String pass;

  /** The number of times to retry a request after a nonce timeout. */
  private final int nonceTimeoutRetries;

  /** The map of open responses to their http clients, which need to be closed after we are finished with the response */
  private final Map<HttpResponse, HttpClient> responseMap = new ConcurrentHashMap<HttpResponse, HttpClient>();

  /** Used to add a random amount of time up to retryMaximumVariableTime to retry a request after a nonce timeout. */
  private final Random generator = new Random();

  /** The amount of time in seconds to wait until trying the request again. */
  private final int retryBaseDelay;

  /** The maximum amount of time in seconds to wait in addition to the RETRY_BASE_DELAY. */
  private final int retryMaximumVariableTime;

  public StandAloneTrustedHttpClientImpl(String user, String pass, Option<Integer> nonceTimeoutRetries,
                                         Option<Integer> retryBaseDelay, Option<Integer> retryMaximumVariableTime) {
    this.user = user;
    this.pass = pass;
    this.nonceTimeoutRetries = nonceTimeoutRetries.getOrElse(DEFAULT_NONCE_TIMEOUT_RETRIES);
    this.retryBaseDelay = retryBaseDelay.getOrElse(DEFAULT_RETRY_BASE_DELAY);
    this.retryMaximumVariableTime = retryMaximumVariableTime.getOrElse(DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME);
  }

  @Override
  public <A> Function<Function<HttpResponse, A>, Either<Exception, A>> run(final HttpUriRequest httpUriRequest) {
    return run(this, httpUriRequest);
  }

  public static <A> Function<Function<HttpResponse, A>, Either<Exception, A>> run(final TrustedHttpClient client,
                                                                                  final HttpUriRequest httpUriRequest) {
    return new Function<Function<HttpResponse, A>, Either<Exception, A>>() {
      @Override
      public Either<Exception, A> apply(Function<HttpResponse, A> responseHandler) {
        HttpResponse response = null;
        try {
          response = client.execute(httpUriRequest);
          return right(responseHandler.apply(response));
        } catch (Exception e) {
          return left(e);
        } finally {
          client.close(response);
        }
      }
    };
  }

  @Override
  public HttpResponse execute(HttpUriRequest httpUriRequest) throws TrustedHttpClientException {
    return execute(httpUriRequest, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
  }

  @Override
  public HttpResponse execute(HttpUriRequest httpUriRequest, int connectionTimeout, int socketTimeout)
          throws TrustedHttpClientException {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    httpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
    // Add the request header to elicit a digest auth response
    httpUriRequest.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);

    // if (serviceRegistry != null && serviceRegistry.getCurrentJob() != null)
    // httpUriRequest.setHeader(CURRENT_JOB_HEADER, Long.toString(serviceRegistry.getCurrentJob().getId()));

    if ("GET".equalsIgnoreCase(httpUriRequest.getMethod()) || "HEAD".equalsIgnoreCase(httpUriRequest.getMethod())) {
      // Set the user/pass
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);
      httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

      // Run the request (the http client handles the multiple back-and-forth requests)
      HttpResponse response = null;
      try {
        response = new HttpResponseWrapper(httpClient.execute(httpUriRequest));
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
      response = new HttpResponseWrapper(httpClient.execute(httpUriRequest));
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
   *         The request to be made that isn't a GET, those are handled automatically.
   * @param response
   *         The response with the bad nonce timeout in it.
   * @return A new response for the request if it was successful without the nonce timing out again or just the same
   * response it got if it ran out of attempts.
   * @throws org.opencastproject.security.api.TrustedHttpClientException
   * @throws java.io.IOException
   * @throws org.apache.http.client.ClientProtocolException
   */
  private HttpResponse retryAuthAndRequestAfterNonceTimeout(HttpUriRequest httpUriRequest, HttpResponse response)
          throws TrustedHttpClientException, IOException, ClientProtocolException {
    // Get rid of old security headers with the old nonce.
    httpUriRequest.removeHeaders(AUTHORIZATION_HEADER_NAME);

    for (int i = 0; i < nonceTimeoutRetries; i++) {
      DefaultHttpClient httpClient = new DefaultHttpClient();
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
      response = new HttpResponseWrapper(httpClient.execute(httpUriRequest));
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
   *         The response to test to see if it has timed out.
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
   *         The request location to get the digest authentication for.
   * @param httpClient
   *         The client to send the request through.
   * @throws org.opencastproject.security.api.TrustedHttpClientException
   *         Thrown if the client cannot be shutdown.
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
   * org.apache.http.client.ResponseHandler)
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
   *         The request to execute in order to obtain the realm and nonce
   * @return A String[] containing the {realm, nonce}
   */
  private String[] getRealmAndNonce(HttpRequestBase request) throws TrustedHttpClientException {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpResponse response;
    try {
      response = new HttpResponseWrapper(httpClient.execute(request));
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
    return new String[]{realm, nonce};
  }
}
