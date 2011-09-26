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

import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.DIGEST_AUTH;
import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * An http client that executes secure (though not necessarily encrypted) http requests.
 */
public class TrustedHttpClientImpl implements TrustedHttpClient, HttpConnectionMXBean {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TrustedHttpClientImpl.class);

  /** The configuration property specifying the digest authentication user */
  public static final String DIGEST_AUTH_USER_KEY = "org.opencastproject.security.digest.user";

  /** The configuration property specifying the digest authentication password */
  public static final String DIGEST_AUTH_PASS_KEY = "org.opencastproject.security.digest.pass";

  /** The default time until a connection attempt fails */
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;

  /** The default time between packets that causes a connection to fail */
  public static final int DEFAULT_SOCKET_TIMEOUT = DEFAULT_CONNECTION_TIMEOUT;

  /** The configured username to send as part of the digest authenticated request */
  protected String user = null;

  /** The configured password to send as part of the digest authenticated request */
  protected String pass = null;

  /** The map of open responses to their http clients, which need to be closed after we are finished with the response */
  protected Map<HttpResponse, HttpClient> responseMap = new ConcurrentHashMap<HttpResponse, HttpClient>();

  public void activate(ComponentContext cc) {
    logger.debug("activate");
    user = cc.getBundleContext().getProperty(DIGEST_AUTH_USER_KEY);
    pass = cc.getBundleContext().getProperty(DIGEST_AUTH_PASS_KEY);
    if (user == null || pass == null)
      throw new IllegalStateException("trusted communication is not properly configured");

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

  public void deactivate() {
    logger.debug("deactivate");
  }

  public TrustedHttpClientImpl() {
  }

  public TrustedHttpClientImpl(String user, String pass) {
    this.user = user;
    this.pass = pass;
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
    DefaultHttpClient httpClient = new DefaultHttpClient();
    httpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
    // Add the request header to elicit a digest auth response
    httpUriRequest.addHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);

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
    HttpRequestBase digestRequest;
    try {
      digestRequest = (HttpRequestBase) httpUriRequest.getClass().newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Can not create a new " + httpUriRequest.getClass().getName());
    }
    digestRequest.setURI(httpUriRequest.getURI());
    digestRequest.addHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
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
        httpUriRequest.addHeader(digestAuth.authenticate(creds, httpUriRequest));
      } catch (Exception e) {
        // close the http connection(s)
        httpClient.getConnectionManager().shutdown();
        throw new TrustedHttpClientException(e);
      }
    }
    HttpResponse response = null;
    try {
      response = httpClient.execute(httpUriRequest);
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
    DefaultHttpClient httpClient = new DefaultHttpClient();
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.HttpConnectionMXBean#getOpenConnections()
   */
  @Override
  public int getOpenConnections() {
    return responseMap.size();
  }

}
