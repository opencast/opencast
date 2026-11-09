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

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.security.api.UnauthorizedException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class WorkflowServiceRemoteImplTest {

  /**
   * Captures the request instead of sending it, so the URI built by the service under test can be inspected
   * without a remote service registry or any HTTP traffic.
   */
  private static final class CapturingWorkflowServiceRemoteImpl extends WorkflowServiceRemoteImpl {
    private String requestUri;
    private List<Integer> expectedStatuses;
    private int responseStatus = SC_NO_CONTENT;

    @Override
    protected HttpResponse getResponse(HttpRequestBase httpRequest, Integer... expectedHttpStatus) {
      requestUri = httpRequest.getURI().toString();
      expectedStatuses = Arrays.asList(expectedHttpStatus);
      return new BasicHttpResponse(HttpVersion.HTTP_1_1, responseStatus, "");
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

  @Test
  public void testGetRunningWorkflowInstanceSendsAction() throws Exception {
    CapturingWorkflowServiceRemoteImpl service = new CapturingWorkflowServiceRemoteImpl();
    service.responseStatus = SC_NOT_FOUND;

    service.getRunningWorkflowInstanceByMediaPackage("mp-1", "write");

    assertEquals("/mediaPackage/mp-1/currentInstance.xml?action=write", service.requestUri);
  }

  // The remote service would use the default action (read) if there was none, and so check a weaker permission
  @Test(expected = IllegalArgumentException.class)
  public void testGetRunningWorkflowInstanceWithoutActionIsRejected() throws Exception {
    new CapturingWorkflowServiceRemoteImpl().getRunningWorkflowInstanceByMediaPackage("mp-1", null);
  }

  @Test
  public void testGetRunningWorkflowInstanceNotFoundIsEmpty() throws Exception {
    CapturingWorkflowServiceRemoteImpl service = new CapturingWorkflowServiceRemoteImpl();
    service.responseStatus = SC_NOT_FOUND;

    assertFalse(service.getRunningWorkflowInstanceByMediaPackage("mp-1", "read").isPresent());
  }

  @Test
  public void testGetRunningWorkflowInstanceForbiddenIsUnauthorized() throws Exception {
    CapturingWorkflowServiceRemoteImpl service = new CapturingWorkflowServiceRemoteImpl();
    service.responseStatus = SC_FORBIDDEN;

    try {
      service.getRunningWorkflowInstanceByMediaPackage("mp-1", "read");
      fail("Expected an UnauthorizedException");
    } catch (UnauthorizedException e) {
      // expected
    }
    assertTrue("403 must be accepted as a response", service.expectedStatuses.contains(SC_FORBIDDEN));
  }

  @Test
  public void testGetWorkflowInstancesForbiddenIsUnauthorized() throws Exception {
    CapturingWorkflowServiceRemoteImpl service = new CapturingWorkflowServiceRemoteImpl();
    service.responseStatus = SC_FORBIDDEN;

    try {
      service.getWorkflowInstancesByMediaPackage("mp-1");
      fail("Expected an UnauthorizedException");
    } catch (UnauthorizedException e) {
      // expected
    }
    assertEquals("/mediaPackage/mp-1/instances.xml", service.requestUri);
    assertTrue("403 must be accepted as a response", service.expectedStatuses.contains(SC_FORBIDDEN));
  }
}
