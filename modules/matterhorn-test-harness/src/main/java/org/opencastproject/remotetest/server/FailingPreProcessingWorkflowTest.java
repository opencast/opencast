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
package org.opencastproject.remotetest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.CaptureUtils;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;
import org.opencastproject.remotetest.util.WorkflowUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.xpath.XPathConstants;

/**
 * This test case simulates adding a recording event, waiting for the capture agent to pick it up and make sure failure
 * during capture is handled correctly (reflected in the workflow instance).
 */
public class FailingPreProcessingWorkflowTest {

  private static final Logger logger = LoggerFactory.getLogger(FailingPreProcessingWorkflowTest.class);

  private TrustedHttpClient client;

  /** The workflow to append after preprocessing */
  private static final String WORKFLOW_DEFINITION_PATH = "/workflow-postprocessing.xml";

  /** The postprocessing workflow definition identifier */
  private static final String WORKFLOW_DEFINITION_ID = "postprocess_integrationtest";

  /** Id of the demo capture agent */
  private static final String CAPTURE_AGENT_ID = "fake_demo_capture_agent";

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + FailingPreProcessingWorkflowTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    client = Main.getClient();
    WorkflowUtils.registerWorkflowDefinition(getSampleWorkflowDefinition());
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
    WorkflowUtils.unregisterWorkflowDefinition(WORKFLOW_DEFINITION_ID);
  }

  @Test
  public void test() throws Exception {
    String workflowId = null;
    long TIMEOUT = 1000L;

    // Specify start end end time for capture
    Calendar c = Calendar.getInstance();
    c.roll(Calendar.MINUTE, 2);
    Date start = c.getTime();
    c.roll(Calendar.MINUTE, 1);
    Date end = c.getTime();

    // Schedule and event and make sure the workflow is in "schedule" operation
    workflowId = scheduleEvent(start, end);
    Thread.sleep(1000);
    if (!WorkflowUtils.isWorkflowInOperation(workflowId, "schedule")
            || !WorkflowUtils.isWorkflowInState(workflowId, "PAUSED")) {
      fail("Workflow " + workflowId + " should be on hold in 'schedule'");
    }

    // Pretend to be the capture agent and report a failed capture
    CaptureUtils.setState(workflowId, "capture_error");
    Thread.sleep(TIMEOUT);

    // Make sure both the scheduled operation and the workflow are in "failed" state
    assertTrue(WorkflowUtils.isWorkflowInState(workflowId, "FAILED"));
  }

  /**
   * Adds a new recording event to the scheduling service and returns the event id.
   * 
   * @param start
   *          start date
   * @param end
   *          end date
   * @return the event identifier
   */
  private String scheduleEvent(Date start, Date end) throws Exception {
    HttpPut request = new HttpPut(BASE_URL + "/scheduler");

    // Create the request body
    Calendar c = Calendar.getInstance();
    c.roll(Calendar.MINUTE, 1);
    long startTime = c.getTimeInMillis();
    c.roll(Calendar.MINUTE, 1);
    long endTime = c.getTimeInMillis();
    String eventXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><event>"
            + "<contributor>demo contributor</contributor><creator>demo creator</creator>"
            + "<startDate>{start}</startDate><endDate>{stop}</endDate><device>{device}</device>"
            + "<language>en</language><license>creative commons</license><resources>vga, audio</resources>"
            + "<title>demo title</title><additionalMetadata><metadata id=\"0\"><key>location</key>"
            + "<value>demo location</value></metadata><metadata id=\"0\"><key>org.opencastproject.workflow.definition</key>"
            + "<value>" + WORKFLOW_DEFINITION_ID + "</value></metadata></additionalMetadata></event>";
    eventXml = eventXml.replace("{device}", CAPTURE_AGENT_ID).replace("{start}", Long.toString(startTime))
            .replace("{stop}", Long.toString(endTime));

    // Prepare the request
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("event", eventXml));
    request.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Submit and check the response
    HttpResponse response = client.execute(request);
    assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
    String responseBody = StringUtils.trimToNull(EntityUtils.toString(response.getEntity()));
    assertNotNull(responseBody);
    String eventId = StringUtils.trimToNull((String) Utils.xpath(responseBody, "/*[local-name() = 'event']/@id",
            XPathConstants.STRING));
    assertNotNull("No event id found", eventId);
    return eventId;
  }

  private String getSampleWorkflowDefinition() throws Exception {
    InputStream in = null;
    try {
      in = FailingPreProcessingWorkflowTest.class.getResourceAsStream(WORKFLOW_DEFINITION_PATH);
      return IOUtils.toString(in, "UTF-8");
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
