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
package org.opencastproject.kernel.http.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;

import java.io.IOException;

/** This interface dictates the required methods for an HttpClient that executes requests. */
public interface HttpClient {

  /** Get the parameters of the http request. */
  HttpParams getParams();

  /** Return the CredentialsProvider that is taking care of this http request. */
  CredentialsProvider getCredentialsProvider();

  /** Executes a http request and returns the response. */
  HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException;

  /** Returns the client connection manager responsible for this request. */
  ClientConnectionManager getConnectionManager();

}
