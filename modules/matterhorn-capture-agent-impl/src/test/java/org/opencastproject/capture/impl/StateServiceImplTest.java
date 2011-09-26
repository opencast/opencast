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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class StateServiceImplTest {
  private CaptureAgentImpl service = null;
  private ConfigurationManager cfg = null;

  @AfterClass
  public static void after() {
    FileUtils.deleteQuietly(new File("./target", "capture-state-test"));
  }

  @Before
  public void setUp() {
    service = new CaptureAgentImpl();
    Assert.assertNotNull(service);

    cfg = new ConfigurationManager();
    Assert.assertNotNull(cfg);
    cfg.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, new File("./target",
            "capture-state-test").getAbsolutePath());
    cfg.setItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL, "1");
    cfg.setItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL, "http://localhost");
    try {
      service.setConfigService(cfg);
    } catch (ConfigurationException e) {
     Assert.fail(e.getMessage());
    }
  }

  @After
  public void tearDown() {
    service.deactivate();
    service = null;
  }

  public void deleteDir(String dirName) {
    File dir = new File(cfg.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), dirName);
    if (dir.exists()) {
      try {
        FileUtils.deleteDirectory(dir);
      } catch (IOException e) {
      }
    }
  }

  // Note: This test is meant to test that the code handles weird cases in the polling, *not* the functionality itself
  @Test
  public void testValidPolling() throws ConfigurationException {
    InputStream s = getClass().getClassLoader().getResourceAsStream("config/scheduler.properties");
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file for scheduler!");
    }

    Properties props = new Properties();
    try {
      props.load(s);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for scheduler!");
    }

    service.updated(props);

  }

  @Test
  public void testUnpreparedImpl() {
    Assert.assertNull(service.getAgentState());
    Assert.assertNull(service.getAgentName());
    service.setAgentState("TEST");
    Assert.assertEquals("TEST", service.getAgentState());
    service.setRecordingState(null, "won't work");
    service.setRecordingState("somethign", null);
    service.setRecordingState("works", "working");
    Assert.assertNotNull(service.getKnownRecordings());
    Assert.assertEquals(1, service.getKnownRecordings().size());
    Assert.assertEquals("working", service.getRecordingState("works").getState());

    service.activate(null);
    Assert.assertEquals(AgentState.IDLE, service.getAgentState());
    service.setAgentState(AgentState.CAPTURING);
    Assert.assertEquals(AgentState.CAPTURING, service.getAgentState());

    deleteDir("works");
  }

  @Test
  public void testRecordings() {
    Assert.assertNotNull(service.getKnownRecordings());
    Assert.assertEquals(0, service.getKnownRecordings().size());
    service.activate(null);
    Assert.assertNotNull(service.getKnownRecordings());
    Assert.assertEquals(0, service.getKnownRecordings().size());

    Assert.assertNull(service.getRecordingState("abc"));
    Assert.assertNull(service.getRecordingState("123"));
    Assert.assertNull(service.getRecordingState("doesnotexist"));
    service.setRecordingState("abc", RecordingState.CAPTURING);
    service.setRecordingState("123", RecordingState.UPLOADING);
    Assert.assertEquals(2, service.getKnownRecordings().size());
    verifyRecording(service.getRecordingState("abc"), "abc", RecordingState.CAPTURING);
    verifyRecording(service.getRecordingState("123"), "123", RecordingState.UPLOADING);
    Assert.assertNull(service.getRecordingState("doesnotexist"));
    deleteDir("abc");
    deleteDir("123");
  }

  @Test
  public void testInvalidRecording() {
    Assert.assertNotNull(service.getKnownRecordings());
    Assert.assertEquals(0, service.getKnownRecordings().size());
    service.activate(null);
    Assert.assertNotNull(service.getKnownRecordings());
    Assert.assertEquals(0, service.getKnownRecordings().size());

    service.setRecordingState(null, "won't work");
    service.setRecordingState("something", null);
    Assert.assertEquals(0, service.getKnownRecordings().size());
  }

  private void verifyRecording(Recording r, String id, String state) {
    Assert.assertEquals(id, r.getID());
    Assert.assertEquals(state, r.getState());
  }
}
