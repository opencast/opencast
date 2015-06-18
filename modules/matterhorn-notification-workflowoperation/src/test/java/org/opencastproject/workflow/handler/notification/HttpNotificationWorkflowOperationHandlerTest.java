/**
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

package org.opencastproject.workflow.handler.notification;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpNotificationWorkflowOperationHandlerTest {

  private HttpNotificationWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;

  // mock services and objects
  private TrustedHttpClient client = null;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = getClass().getResource("/concat_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    // set up mock trusted http client
    client = EasyMock.createNiceMock(TrustedHttpClient.class);
    HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
            HttpStatus.SC_ACCEPTED, ""));
    EasyMock.expect(client.execute((HttpUriRequest) EasyMock.anyObject(), EasyMock.anyInt(), EasyMock.anyInt()))
            .andReturn(response);
    EasyMock.replay(client);

    // set up service
    operationHandler = new HttpNotificationWorkflowOperationHandler();
  }

  @Test
  @Ignore
  // todo test does not pass
  public void testSuccessfulNotification() throws Exception {

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_URL_PATH, "http://www.host-does-not-exist.com");
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_NOTIFICATION_SUBJECT, "test");
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_NOTIFICATION_SUBJECT, "test message");
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_MAX_RETRY, "2");
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_TIMEOUT, Integer.toString(10 * 1000));

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(result.getAction(), Action.CONTINUE);
  }

  @Test
  public void testNotificationFailedAfterOneTry() throws Exception {
    client = EasyMock.createNiceMock(TrustedHttpClient.class);
    HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
            HttpStatus.SC_NOT_FOUND, ""));
    EasyMock.expect(client.execute((HttpPut) EasyMock.anyObject(), EasyMock.anyInt(), EasyMock.anyInt())).andReturn(
            response);
    EasyMock.replay(client);

    // set up service
    operationHandler = new HttpNotificationWorkflowOperationHandler();

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_URL_PATH, "http://www.host-does-not-exist.com");
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_NOTIFICATION_SUBJECT, "test");
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_MAX_RETRY, "0");
    configurations.put(HttpNotificationWorkflowOperationHandler.OPT_TIMEOUT, Integer.toString(10 * 1000));

    // run the operation handler
    try {
      getWorkflowOperationResult(mp, configurations);
      Assert.fail("Operation handler should have thrown an exception!");
    } catch (WorkflowOperationException e) {
      Assert.assertTrue("Exception thrown as expected by the operation handler", true);
    }

  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("http-notify");
    operation.setState(OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    return operationHandler.start(workflowInstance, null);
  }

}
