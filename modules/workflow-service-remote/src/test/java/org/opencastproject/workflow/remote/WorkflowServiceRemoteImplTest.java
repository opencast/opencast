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

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;

public class WorkflowServiceRemoteImplTest {

  /**
   * Captures the request instead of sending it, so the URI built by the service under test can be inspected
   * without a remote service registry or any HTTP traffic.
   */
  private static final class CapturingWorkflowServiceRemoteImpl extends WorkflowServiceRemoteImpl {
    private String requestUri;

    @Override
    protected HttpResponse getResponse(HttpRequestBase httpRequest, Integer... expectedHttpStatus) {
      requestUri = httpRequest.getURI().toString();
      return new BasicHttpResponse(HttpVersion.HTTP_1_1, SC_NO_CONTENT, "No Content");
    }

    @Override
    protected void closeConnection(HttpResponse response) {
    }
  }

  @Test
  public void testRemoveWithForceBuildsQueryString() throws Exception {
    CapturingWorkflowServiceRemoteImpl service = new CapturingWorkflowServiceRemoteImpl();

    service.remove(42, true);

    assertEquals("/remove/42?force=true", service.requestUri);
  }

  @Test
  public void testRemoveWithoutForceOmitsQueryString() throws Exception {
    CapturingWorkflowServiceRemoteImpl service = new CapturingWorkflowServiceRemoteImpl();

    service.remove(42, false);

    assertEquals("/remove/42", service.requestUri);
  }
}
