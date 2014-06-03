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
import static org.junit.Assert.fail;
import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.CaptureUtils;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.WorkflowUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This test case simulates adding a recording event, waiting for the capture agent to pick it up, do the recording and
 * then ingest it. At the same time, it monitors the associated workflow and makes sure the operation and state
 * transitions happen as expected.
 */
public class PreProcessingWorkflowTest {

  private static final Logger logger = LoggerFactory.getLogger(PreProcessingWorkflowTest.class);

  private TrustedHttpClient client;

  /** The workflow to append after preprocessing */
  private static final String WORKFLOW_DEFINITION_PATH = "/workflow-postprocessing.xml";

  /** The postprocessing workflow definition identifier */
  private static final String WORKFLOW_DEFINITION_ID = "postprocess_integrationtest";

  /** Id of the demo capture agent */
  private static final String CAPTURE_AGENT_ID = "demo_capture_agent";

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + PreProcessingWorkflowTest.class.getName());
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
    long waiting = 0;
    long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);
    long GRACE_PERIOD = 30000L;

    // Make sure the demo capture agent is online
    if (!CaptureUtils.isOnline(CAPTURE_AGENT_ID)) {
      logger.warn("Demo capture agent with id " + CAPTURE_AGENT_ID + " is not online, skipping this test");
      return;
    }

    // Specify start end end time for capture
    Calendar c = Calendar.getInstance();
    c.roll(Calendar.MINUTE, 2);
    Date start = c.getTime();
    c.roll(Calendar.MINUTE, 1);
    Date end = c.getTime();

    // TODO: How do we submit the workflow definition?

    // Schedule an event and make sure that, once the workflow is dispatched, it's on hold in the "schedule" operation
    workflowId = scheduleEvent(start, end);
    while (WorkflowUtils.isWorkflowInState(workflowId, "INSTANTIATED")) {
      logger.info("Waiting for scheduled recording workflow {} to be dispatched", workflowId);
      Thread.sleep(TIMEOUT);
    }
    if (!WorkflowUtils.isWorkflowInState(workflowId, "PAUSED")) {
      fail("Workflow " + workflowId + " should be on hold in 'schedule', or ");
    }

    logger.info("Scheduled recording {} is now paused, waiting for capture", workflowId);

    // Wait for the capture agent to start the recording and make sure the workflow enters the "capture" operation
    waiting = 120 * 1000L + GRACE_PERIOD; // 2 min +
    boolean agentIsCapturing = false;
    boolean inCaptureOperation = false;
    while (waiting > 0) {
      logger.info("Waiting for capture agent to start capture");
      String workflowXml = WorkflowUtils.getWorkflowById(workflowId);
      if (CaptureUtils.recordingExists(workflowId)) {
        agentIsCapturing |= CaptureUtils.isInState(workflowId, "capturing");
        inCaptureOperation |= WorkflowUtils.isWorkflowInOperation(workflowId, "capture");
        if (agentIsCapturing && inCaptureOperation)
          break;
      } else if (WorkflowUtils.getWorkflowState(workflowXml).contains("_error")) {
        fail("Recording failed");
      }

      waiting -= TIMEOUT;
      Thread.sleep(TIMEOUT);
    }

    // Are we already past the grace period?
    if (waiting <= 0) {
      if (!agentIsCapturing) {
        logger.info("Capture agent failed to start capture");
        fail("Agent '" + CAPTURE_AGENT_ID + "' did not start recording '" + workflowId + "'");
      } else if (!inCaptureOperation) {
        logger.info("Workflow is not in the capturing state");
        fail("Workflow '" + workflowId + "' never entered the 'capture' hold state");
      }
    }

    // Wait for capture agent to stop capturing
    Thread.sleep(Math.max(end.getTime() - System.currentTimeMillis(), 0));

    // Make sure workflow advanced to "ingest" operation
    waiting = 60 * 1000L + GRACE_PERIOD; // 1 min +
    boolean agentIsIngesting = false;
    boolean inIngestOperation = false;
    boolean workflowSucceeded = false;
    while (waiting > 0) {
      agentIsIngesting |= CaptureUtils.isInState(workflowId, "uploading");
      inIngestOperation |= WorkflowUtils.isWorkflowInOperation(workflowId, "capture");
      if (agentIsIngesting && inIngestOperation)
        break;
      // we may have missed the ingest step, since it happens so quickly. if the workflow has already succeeded, we're
      // done
      if (WorkflowUtils.isWorkflowInState(workflowId, "SUCCEEDED")) {
        workflowSucceeded = true;
        System.out.println("Workflow " + workflowId + " succeeded before we could catch it in the ingesting state");
        break;
      }
      waiting -= TIMEOUT;
      Thread.sleep(TIMEOUT);
    }

    // Are we already past the grace period?
    if (waiting <= 0) {
      if (!agentIsIngesting && !workflowSucceeded)
        fail("Agent '" + CAPTURE_AGENT_ID + "' did not start ingesting '" + workflowId + "'");
      else if (!inIngestOperation && !workflowSucceeded)
        fail("Workflow '" + workflowId + "' never entered the 'ingest' hold state");
    }

    // Wait for ingest and make sure workflow executes "cleanup", then finishes successfully
    if (!workflowSucceeded) {
      waiting = 60 * 1000L + GRACE_PERIOD; // 1 min +
      while (waiting > 0) {
        if (WorkflowUtils.isWorkflowInState(workflowId, "SUCCEEDED")) {
          workflowSucceeded = true;
          break;
        }
        waiting -= TIMEOUT;
        Thread.sleep(TIMEOUT);
      }
    }

    if (!workflowSucceeded) {
      fail("Workflow '" + workflowId + "' did not succeed");
    }

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
    HttpPost request = new HttpPost(BASE_URL + "/recordings");

    // Create the request body
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    f.setTimeZone(TimeZone.getTimeZone("UTC"));

    String dublinCoreXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><dcterms:creator>demo creator</dcterms:creator>"
            + "<dcterms:contributor>demo contributor</dcterms:contributor>"
            + "<dcterms:temporal xsi:type=\"dcterms:Period\">start={startTime}; end={endTime}; scheme=W3C-DTF;</dcterms:temporal>"
            + "<dcterms:language>en</dcterms:language><dcterms:spatial>{device}</dcterms:spatial><dcterms:title>demo title</dcterms:title>"
            + "<dcterms:license>creative commons</dcterms:license></dublincore>";
    dublinCoreXml = dublinCoreXml.replace("{device}", CAPTURE_AGENT_ID).replace("{startTime}", f.format(start))
            .replace("{endTime}", f.format(end));

    String captureAgentMetadata = "event.location=testdevice\norg.opencastproject.workflow.definition="
            + WORKFLOW_DEFINITION_ID + "\n";

    // Prepare the request
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("dublincore", dublinCoreXml));
    formParams.add(new BasicNameValuePair("agentparameters", captureAgentMetadata));
    request.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Submit and check the response
    HttpResponse response = client.execute(request);
    assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
    String responseHeader = StringUtils.trimToNull(response.getFirstHeader("Location").getValue());
    assertNotNull(responseHeader);
    Pattern p = Pattern.compile(".*?(\\d+)\\.xml");
    Matcher m = p.matcher(responseHeader);
    String eventId = null;
    if (m.find()) {
      eventId = m.group(1);
      assertNotNull("No event id found", eventId);
    } else {
      fail("Unknown location returned");
    }
    return eventId;
  }

  private String getSampleWorkflowDefinition() throws Exception {
    InputStream in = null;
    try {
      in = PreProcessingWorkflowTest.class.getResourceAsStream(WORKFLOW_DEFINITION_PATH);
      return IOUtils.toString(in, "UTF-8");
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
