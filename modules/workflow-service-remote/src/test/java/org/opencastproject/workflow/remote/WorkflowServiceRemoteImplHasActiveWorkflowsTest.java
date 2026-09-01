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

package org.opencastproject.workflow.remote;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class WorkflowServiceRemoteImplHasActiveWorkflowsTest {

  /**
   * Answers every request with a fixed body, the way the workflow service endpoint does, so the response
   * handling of the service under test can be inspected without a remote service registry or any HTTP traffic.
   */
  private static final class StubWorkflowServiceRemoteImpl extends WorkflowServiceRemoteImpl {
    private final String responseBody;
    private String requestUri;

    StubWorkflowServiceRemoteImpl(String responseBody) {
      this.responseBody = responseBody;
    }

    @Override
    protected HttpResponse getResponse(HttpRequestBase httpRequest, Integer... expectedHttpStatus) {
      requestUri = httpRequest.getURI().toString();
      HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, SC_OK, "OK");
      response.setEntity(new StringEntity(responseBody, StandardCharsets.UTF_8));
      return response;
    }

    @Override
    protected void closeConnection(HttpResponse response) {
    }
  }

  @Test
  public void testMediaPackageHasActiveWorkflowsReadsTrueFromResponseBody() throws Exception {
    StubWorkflowServiceRemoteImpl service = new StubWorkflowServiceRemoteImpl("true");

    assertTrue(service.mediaPackageHasActiveWorkflows("mp-1"));
    assertEquals("/mediaPackage/mp-1/hasActiveWorkflows", service.requestUri);
  }

  @Test
  public void testMediaPackageHasActiveWorkflowsReadsFalseFromResponseBody() throws Exception {
    StubWorkflowServiceRemoteImpl service = new StubWorkflowServiceRemoteImpl("false");

    assertFalse(service.mediaPackageHasActiveWorkflows("mp-1"));
  }

  @Test
  public void testUserHasActiveWorkflowsReadsTrueFromResponseBody() throws Exception {
    StubWorkflowServiceRemoteImpl service = new StubWorkflowServiceRemoteImpl("true");

    assertTrue(service.userHasActiveWorkflows("alice"));
    assertEquals("/user/alice/hasActiveWorkflows", service.requestUri);
  }

  @Test
  public void testUserHasActiveWorkflowsReadsFalseFromResponseBody() throws Exception {
    StubWorkflowServiceRemoteImpl service = new StubWorkflowServiceRemoteImpl("false");

    assertFalse(service.userHasActiveWorkflows("alice"));
  }
}
