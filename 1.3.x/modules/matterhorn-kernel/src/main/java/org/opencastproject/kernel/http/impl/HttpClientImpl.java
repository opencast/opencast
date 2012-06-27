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
package org.opencastproject.kernel.http.impl;

import org.opencastproject.kernel.http.api.HttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import java.io.IOException;

/** Implementation of HttpClient that makes http requests. */
public class HttpClientImpl implements HttpClient {
  /** client used for all http requests. */
  private DefaultHttpClient defaultHttpClient = new DefaultHttpClient();

  /** See org.opencastproject.kernel.http.api.HttpClient */
  @Override
  public HttpParams getParams() {
    return defaultHttpClient.getParams();
  }

  /** See org.opencastproject.kernel.http.api.HttpClient */
  @Override
  public CredentialsProvider getCredentialsProvider() {
    return defaultHttpClient.getCredentialsProvider();
  }

  /** See org.opencastproject.kernel.http.api.HttpClient */
  @Override
  public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException {
    return defaultHttpClient.execute(httpUriRequest);
  }

  /** See org.opencastproject.kernel.http.api.HttpClient */
  @Override
  public ClientConnectionManager getConnectionManager() {
    return defaultHttpClient.getConnectionManager();
  }
  
}
