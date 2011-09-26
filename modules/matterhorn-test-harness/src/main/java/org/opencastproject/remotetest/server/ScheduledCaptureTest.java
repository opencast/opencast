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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.server.resource.AdminResources;
import org.opencastproject.remotetest.server.resource.CaptureAdminResources;
import org.opencastproject.remotetest.server.resource.CaptureResources;
import org.opencastproject.remotetest.server.resource.SchedulerResources;
import org.opencastproject.remotetest.server.resource.SearchResources;
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

import java.util.UUID;

import javax.xml.xpath.XPathConstants;

/**
 * Integration test for scheduled capture
 * 
 * @author jamiehodge
 * 
 */

public class ScheduledCaptureTest {

  /** The http client */
  protected TrustedHttpClient httpClient;

  private static final Logger logger = LoggerFactory.getLogger(ScheduledCaptureTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + ScheduledCaptureTest.class.getName());
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
  public void testScheduledCapture() throws Exception {

    // Agent registered (Capture Admin Agents)
    HttpResponse response = CaptureAdminResources.agents(httpClient);
    assertEquals("Response code (agents):", 200, response.getStatusLine().getStatusCode());
    Document xml = Utils.parseXml(response.getEntity().getContent());
    assertTrue("Agent included? (agents):", Utils.xpathExists(xml, "//ns1:agent-state-update[name=\'" + CaptureResources.AGENT + "\']"));

    // Agent registered (Capture Admin Agent)
    response = CaptureAdminResources.agent(httpClient, CaptureResources.AGENT);
    assertEquals("Response code (agent):", 200, response.getStatusLine().getStatusCode());
    xml = Utils.parseXml(response.getEntity().getContent());
    assertTrue("Agent included? (agent):", Utils.xpathExists(xml, "//ns2:agent-state-update[name=\'" + CaptureResources.AGENT + "\']"));

    // Agent idle (State)
    response = StateResources.getState(httpClient);
    assertEquals("Response code (getState):", 200, response.getStatusLine().getStatusCode());
    assertEquals("Agent idle? (getState):", "idle", EntityUtils.toString(response.getEntity(), "UTF-8"));

    // Agent idle (Capture Admin Agent)
    response = CaptureAdminResources.agent(httpClient, CaptureResources.AGENT);
    assertEquals("Response code (agent):", 200, response.getStatusLine().getStatusCode());
    assertEquals("Agent idle? (agent):", "idle", Utils.xpath(xml, "//ns2:agent-state-update[name=\'" + CaptureResources.AGENT + "\']/state", XPathConstants.STRING));

    // Get initial recording count (Admin Proxy)
    response = AdminResources.countRecordings(httpClient);
    assertEquals("Response code (countRecordings):", 200, response.getStatusLine().getStatusCode());
    JSONObject initialRecordingCount = Utils.parseJson(EntityUtils.toString(response.getEntity(), "UTF-8"));
    System.out.println("Initial: " + initialRecordingCount);

    // Generate unique title and create event XML
    String title = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    String event = Utils.schedulerEvent(10000, title, id);

    // Add event (Scheduler)
    response = SchedulerResources.addEvent(httpClient, event);
    assertEquals("Response code (addEvent):", 200, response.getStatusLine().getStatusCode());

    // Event included? (Scheduler: events)
    response = SchedulerResources.getEvents(httpClient);
    assertEquals("Response code (getEvents):", 200, response.getStatusLine().getStatusCode());
    xml = Utils.parseXml(response.getEntity().getContent());
    assertTrue("Event included? (getEvents):", Utils.xpathExists(xml, "//ns1:SchedulerEvent[id=\'" + id + "\']"));

    // Event included? (Scheduler: upcoming events)
    response = SchedulerResources.getUpcomingEvents(httpClient);
    assertEquals("Response code (getUpcomingEvents):", 200, response.getStatusLine().getStatusCode());
    xml = Utils.parseXml(response.getEntity().getContent());
    assertTrue("Event included? (getUpcomingEvents):", Utils.xpathExists(xml, "//ns1:SchedulerEvent[id=\'" + id + "\']"));

    // Compare event (Scheduler: event)
    response = SchedulerResources.getEvent(httpClient, id);
    assertEquals("Response code (getEvent):", 200, response.getStatusLine().getStatusCode());
    xml = Utils.parseXml(response.getEntity().getContent());
    assertEquals("Event id (getEvent):", title, Utils.xpath(xml, "//item[@key='title']/value", XPathConstants.STRING));
    assertEquals("Event title (getEvent):", id, Utils.xpath(xml, "//id", XPathConstants.STRING));

    // Compare event DC metadata (Scheduler)
    response = SchedulerResources.getDublinCoreMetadata(httpClient, id);
    assertEquals("Response code (getDublinCoreMetadata):", 200, response.getStatusLine().getStatusCode());
    xml = Utils.parseXml(response.getEntity().getContent());
    assertEquals("Event id (getDublinCoreMetadata):", title, Utils.xpath(xml, "//dcterms:title", XPathConstants.STRING));
    assertEquals("Event title (getDublinCoreMetadata):", id, Utils.xpath(xml, "//dcterms:identifier", XPathConstants.STRING));

    // Get post-scheduled recording count (Admin Proxy)
    response = AdminResources.countRecordings(httpClient);
    assertEquals("Response code (countRecordings):", 200, response.getStatusLine().getStatusCode());
    JSONObject scheduledRecordingCount = Utils.parseJson(EntityUtils.toString(response.getEntity(), "UTF-8"));
    System.out.println("Recording Scheduled: " + scheduledRecordingCount);

    // Compare total recording count
    assertEquals("Total recording count increased by one:", (Long) initialRecordingCount.get("total") + 1, scheduledRecordingCount.get("total"));
    // Compare upcoming recording count
    assertEquals("Upcoming recording count increased by one:", (Long) initialRecordingCount.get("upcoming") + 1, scheduledRecordingCount.get("upcoming"));

    // Confirm recording started (State)
    int retries = 0;
    int timeout = 60;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check capture agent status
      response = StateResources.getState(httpClient);
      assertEquals("Response code (workflow instance):", 200, response.getStatusLine().getStatusCode());
      if (response.getEntity().getContent().equals("capturing")) {
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
      if (Utils.xpath(xml, "//ns1:agent-state-update[name=\'" + CaptureResources.AGENT + "\']/state", XPathConstants.STRING).equals("capturing")) {
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
    System.out.println("Recording Started: " + capturingRecordingCount);

    // Compare total recording count
    assertEquals("Total recording count the same (schedule to capture):", (Long) scheduledRecordingCount.get("total"), capturingRecordingCount.get("total"));
    // Compare upcoming recording count
    assertEquals("Upcoming recording count decreased by one:", (Long) scheduledRecordingCount.get("upcoming") - 1, capturingRecordingCount.get("upcoming"));
    // Compare capturing recording count
    assertEquals("Capture recording count increased by one:", (Long) scheduledRecordingCount.get("capturing") + 1, capturingRecordingCount.get("capturing"));

    // Confirm recording stopped (State)
    retries = 0;
    timeout = 10;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check capture agent status
      response = StateResources.getState(httpClient);
      assertEquals("Response code (workflow instance):", 200, response.getStatusLine().getStatusCode());
      if (response.getEntity().getContent().equals("idle")) {
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
      if (Utils.xpath(xml, "//ns1:agent-state-update[name=\'" + CaptureResources.AGENT + "\']/state", XPathConstants.STRING).equals("idle")) {
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
    assertEquals("Total recording count the same (capture to process):", (Long) capturingRecordingCount.get("total"), processingRecordingCount.get("total"));
    // Compare capturing recording count
    assertEquals("Capture recording count decreased by one:", (Long) capturingRecordingCount.get("capturing") - 1, processingRecordingCount.get("capturing"));
    // Compare processing recording count
    assertEquals("Process recording count increased by one:", (Long) capturingRecordingCount.get("processing") + 1, processingRecordingCount.get("processing"));

    // Confirm recording indexed
    retries = 0;
    timeout = 60;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check if recording indexed (Search)
      response = SearchResources.all(httpClient, title);
      assertEquals("Response code (search all):", 200, response.getStatusLine().getStatusCode());
      xml = Utils.parseXml(response.getEntity().getContent());
      if (Utils.xpathExists(xml, "//ns2:mediapackage[title=\'" + title + "\']")) {
        break;
      }

      retries++;
    }

    if (retries == timeout) {
      fail("Search Service failed to index recording.");
    }

    // Get finished recording count (Admin Proxy)
    response = AdminResources.countRecordings(httpClient);
    assertEquals("Response code (countRecordings):", 200, response.getStatusLine().getStatusCode());
    JSONObject finishedRecordingCount = Utils.parseJson(EntityUtils.toString(response.getEntity(), "UTF-8"));
    System.out.println("Finished Recording: " + finishedRecordingCount);

    // Compare total recording count
    assertEquals("Total recording count the same (process to finish):", (Long) processingRecordingCount.get("total"), finishedRecordingCount.get("total"));
    // Compare processing recording count
    assertEquals("Process recording count decreased by one:", (Long) processingRecordingCount.get("processing") - 1, finishedRecordingCount.get("processing"));
    // Compare finished recording count
    assertEquals("Finished recording count increased by one:", (Long) processingRecordingCount.get("finished") + 1, finishedRecordingCount.get("finished"));

  }
}
