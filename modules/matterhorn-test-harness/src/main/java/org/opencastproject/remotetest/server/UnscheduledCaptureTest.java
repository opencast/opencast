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

package org.opencastproject.remotetest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.server.resource.AdminResources;
import org.opencastproject.remotetest.server.resource.CaptureAdminResources;
import org.opencastproject.remotetest.server.resource.CaptureResources;
import org.opencastproject.remotetest.server.resource.StateResources;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

/**
 * Integration test for unscheduled capture
 */

public class UnscheduledCaptureTest {
  public static String recordingId;

  /** The http client */
  protected TrustedHttpClient httpClient;

  private static final Logger logger = LoggerFactory.getLogger(UnscheduledCaptureTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + UnscheduledCaptureTest.class.getName());
  }

  @Before
  public void setUp() throws Exception {
    httpClient = Main.getClient();
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(httpClient);
  }

  @Ignore
  @Test
  public void testUnscheduledCapture() throws Exception {

    // Agent Registered (Capture Admin Agents)
    HttpResponse response = CaptureAdminResources.agents(httpClient);
    assertEquals("Response code (agents):", 200, response.getStatusLine().getStatusCode());
    Document xml = Utils.parseXml(response.getEntity().getContent());
    assertTrue("Agent included? (agents):",
            Utils.xpathExists(xml, "//*[local-name() = 'agent-state-update'][name=\'" + CaptureResources.AGENT + "\']"));

    // Agent Registered (Capture Admin Agent)
    response = CaptureAdminResources.agent(httpClient, CaptureResources.AGENT);
    assertEquals("Response code (agent):", 200, response.getStatusLine().getStatusCode());
    xml = Utils.parseXml(response.getEntity().getContent());
    assertTrue("Agent included? (agent):",
            Utils.xpathExists(xml, "//*[local-name() = 'agent-state-update'][name=\'" + CaptureResources.AGENT + "\']"));

    // Agent idle (State)
    response = StateResources.getState(httpClient);
    assertEquals("Response code (getState):", 200, response.getStatusLine().getStatusCode());
    assertEquals("Agent idle? (getState):", "idle", EntityUtils.toString(response.getEntity(), "UTF-8"));

    // Get initial recording count (Admin Proxy)
    response = AdminResources.countRecordings(httpClient);
    assertEquals("Response code (countRecordings):", 200, response.getStatusLine().getStatusCode());
    JSONObject initialRecordingCount = Utils.parseJson(EntityUtils.toString(response.getEntity(), "UTF-8"));
    // System.out.println("Initial: " + initialRecordingCount);

    // Start capture (Capture)
    response = CaptureResources.startCaptureGet(httpClient);
    assertEquals("Response code (startCapture):", 200, response.getStatusLine().getStatusCode());

    // Get capture ID (Capture)
    recordingId = CaptureResources.captureId(response);

    // Confirm recording started (State)
    int retries = 0;
    int timeout = 60;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check capture agent status
      response = StateResources.getState(httpClient);
      assertEquals("Response code (workflow instance):", 200, response.getStatusLine().getStatusCode());
      if (EntityUtils.toString(response.getEntity(), "UTF-8").equals("capturing")) {
        break;
      }

      retries++;
    }

    if (retries == timeout) {
      fail("State Service failed to reflect that recording had started.");
    }

    // Confirm recording started (Capture Admin)
    retries = 0;
    timeout = 10;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check capture agent status
      response = CaptureAdminResources.agents(httpClient);
      assertEquals("Response code (agents):", 200, response.getStatusLine().getStatusCode());
      xml = Utils.parseXml(response.getEntity().getContent());
      if (Utils.xpath(
              xml,
              "//*[local-name() = 'agent-state-update'][name=\'" + CaptureResources.AGENT
                      + "\']/*[local-name() = 'state']", XPathConstants.STRING).equals("capturing")) {
        break;
      }

      retries++;
    }

    if (retries == timeout) {
      fail("Capture Admin failed to reflect that recording had started.");
    }

    // Get capturing recording count (Admin Proxy)
    response = AdminResources.countRecordings(httpClient);
    assertEquals("Response code (countRecordings):", 200, response.getStatusLine().getStatusCode());
    JSONObject capturingRecordingCount = Utils.parseJson(EntityUtils.toString(response.getEntity(), "UTF-8"));
    // System.out.println("Recording Started: " + capturingRecordingCount);

    // Compare total recording count
    assertEquals("Total recording count the same (schedule to capture):",
            (Long) initialRecordingCount.get("total") + 1, capturingRecordingCount.get("total"));
    // Compare capturing recording count
    assertEquals("Capture recording count increased by one:", (Long) initialRecordingCount.get("capturing") + 1,
            capturingRecordingCount.get("capturing"));

    // Stop capture (Capture)
    response = CaptureResources.stopCapturePost(httpClient, recordingId);
    assertEquals("Response code (stopCapturePost):", 200, response.getStatusLine().getStatusCode());

    // Confirm recording stopped (State)
    retries = 0;
    timeout = 10;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check capture agent status
      response = StateResources.getState(httpClient);
      assertEquals("Response code (workflow instance):", 200, response.getStatusLine().getStatusCode());
      if (EntityUtils.toString(response.getEntity(), "UTF-8").equals("idle")) {
        break;
      }

      retries++;
    }

    if (retries == timeout) {
      fail("State Service failed to reflect that recording had stopped.");
    }

    // Confirm recording stopped (Capture Admin)
    retries = 0;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check capture agent status
      response = CaptureAdminResources.agents(httpClient);
      assertEquals("Response code (agents):", 200, response.getStatusLine().getStatusCode());
      xml = Utils.parseXml(response.getEntity().getContent());
      if (Utils.xpath(
              xml,
              "//*[local-name() = 'agent-state-update'][name=\'" + CaptureResources.AGENT
                      + "\']/*[local-name() = 'state']", XPathConstants.STRING).equals("idle")) {
        break;
      }

      retries++;
    }

    if (retries == timeout) {
      fail("Capture Admin Service failed to reflect that recording had stopped.");
    }

    // Get processing recording count (Admin Proxy)
    response = AdminResources.countRecordings(httpClient);
    assertEquals("Response code (countRecordings):", 200, response.getStatusLine().getStatusCode());
    JSONObject processingRecordingCount = Utils.parseJson(EntityUtils.toString(response.getEntity(), "UTF-8"));
    System.out.println("Process Recording: " + processingRecordingCount);

    // Compare total recording count
    assertEquals("Total recording count the same (capture to process):", (Long) capturingRecordingCount.get("total"),
            processingRecordingCount.get("total"));
    // Compare capturing recording count
    assertEquals("Capture recording count decreased by one:", (Long) capturingRecordingCount.get("capturing") - 1,
            processingRecordingCount.get("capturing"));
    // Compare processing recording count
    assertEquals("Process recording count increased by one:", (Long) capturingRecordingCount.get("processing") + 1,
            processingRecordingCount.get("processing"));

    // TODO Confirm recording indexed

    Thread.sleep(15000);

    // Get finished recording count (Admin Proxy)
    response = AdminResources.countRecordings(httpClient);
    assertEquals("Response code (countRecordings):", 200, response.getStatusLine().getStatusCode());
    JSONObject finishedRecordingCount = Utils.parseJson(EntityUtils.toString(response.getEntity(), "UTF-8"));
    System.out.println("Finished Recording: " + finishedRecordingCount);

    // Compare total recording count
    assertEquals("Total recording count the same (process to finish):", (Long) processingRecordingCount.get("total"),
            finishedRecordingCount.get("total"));
    // Compare processing recording count
    assertEquals("Process recording count decreased by one:", (Long) processingRecordingCount.get("processing") - 1,
            finishedRecordingCount.get("processing"));
    // Compare finished recording count
    assertEquals("Finished recording count increased by one:", (Long) processingRecordingCount.get("finished") + 1,
            finishedRecordingCount.get("finished"));

  }
}
