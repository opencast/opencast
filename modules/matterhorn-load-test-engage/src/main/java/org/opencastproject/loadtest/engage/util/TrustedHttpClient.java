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
package org.opencastproject.loadtest.engage.util;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An http client that executes secure (though not necessarily encrypted) http requests for integration testing.
 * This client wraps the apache commons http client (version 4).  Unlike the TrustedHttpClient that is part of the
 * Matterhorn core software, each thread accessing this class should obtain its own instance.
 */
public class TrustedHttpClient {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TrustedHttpClient.class);

  /** The header key used to request digest auth */
  public static final String REQUESTED_AUTH_HEADER = "X-Requested-Auth";

  /** The header value used to request digest auth */
  public static final String DIGEST_AUTH = "Digest";

  /** The configured username to send as part of the digest authenticated request */
  protected String user = null;

  /** The configured password to send as part of the digest authenticated request */
  protected String pass = null;

  /** The apache http client */
  protected DefaultHttpClient httpClient = null;

  private TrustedHttpClient() {
    httpClient = new DefaultHttpClient();
  }

  public TrustedHttpClient(String user, String pass) {
    this();
    this.user = user;
    this.pass = pass;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.loadtest.engage.util.remotetest.util.security.api.TrustedHttpClient#execute(org.apache.http.client.methods.HttpUriRequest)
   */
  public HttpResponse execute(HttpUriRequest httpUriRequest) {
    // Add the request header to elicit a digest auth response
    httpUriRequest.addHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);

    if("GET".equalsIgnoreCase(httpUriRequest.getMethod()) || "HEAD".equalsIgnoreCase(httpUriRequest.getMethod())) {
      // Set the user/pass
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);
      httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

      // Run the request (the http client handles the multiple back-and-forth requests)
      try {
        return httpClient.execute(httpUriRequest);
      } catch (IOException e) {
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

    if(realmAndNonce != null) {
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
    try {
      return httpClient.execute(httpUriRequest);
    } catch (Exception e) {
      // close the http connection(s)
      httpClient.getConnectionManager().shutdown();
      throw new TrustedHttpClientException(e);
    }
  }

  public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler) {
    try {
      return responseHandler.handleResponse(execute(httpUriRequest));
    } catch (IOException e) {
      throw new TrustedHttpClientException(e);
    }
  }

  /**
   * Shuts down this http client.
   */
  public void shutdown() {
    this.httpClient.getConnectionManager().shutdown();
  }

  /**
   * Perform a request, and extract the realm and nonce values
   *
   * @param request The request to execute in order to obtain the realm and nonce
   * @return A String[] containing the {realm, nonce}
   */
  protected String[] getRealmAndNonce(HttpRequestBase request) {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpResponse response;
    try {
      response = httpClient.execute(request);
    } catch (IOException e) {
      httpClient.getConnectionManager().shutdown();
      throw new TrustedHttpClientException(e);
    }
    Header[] headers = response.getHeaders("WWW-Authenticate");
    if(headers == null || headers.length == 0) {
      logger.warn("URI {} does not support digest authentication", request.getURI());
      httpClient.getConnectionManager().shutdown();
      return null;
    }
    Header authRequiredResponseHeader = headers[0];
    String nonce = null;
    String realm = null;
    for(HeaderElement element : authRequiredResponseHeader.getElements()) {
      if("nonce".equals(element.getName())) {
        nonce = element.getValue();
      } else if("Digest realm".equals(element.getName())) {
        realm = element.getValue();
      }
    }
    httpClient.getConnectionManager().shutdown();
    return new String[] {realm, nonce};
  }
}
