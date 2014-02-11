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
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.util.XProperties;

import org.apache.commons.io.FileUtils;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Test the implementation of the Capture Agent, which uses gstreamer to generate pipelines that capture the media.
 */
public class CaptureAgentImplTest {

  /** The single instance of CaptureAgentImpl needed */
  private CaptureAgentImpl captureAgentImpl = null;

  /** The configuration manager for these tests */
  private ConfigurationManager config = null;

  /** Properties specifically designed for unit testing */
  private Properties properties = null;

  /** Define a recording ID for the test */
  private static final String recordingID = "UnitTest1";

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImplTest.class);

  /** Waits for a particular state to occur or times out waiting. **/
  private WaitForState waiter;

  private File manifestFile;

  private File zippedMedia;

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @Before
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    File testDir = new File("./target", "capture-agent-test" + File.separator + "cache" + File.separator + "captures"
            + File.separator + recordingID);
    if (testDir.exists()) {
      FileUtils.deleteQuietly(testDir);
      logger.info("Removing  " + testDir.getAbsolutePath());
    } else {
      logger.info("Didn't Delete " + testDir.getAbsolutePath());
    }

    Properties p = loadProperties("config/capture.properties");
    p.put("org.opencastproject.storage.dir",
            new File("./target", "capture-agent-test").getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
    p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    config.updated(p);
    // creates agent, initially idle
    captureAgentImpl = new CaptureAgentImpl();
    captureAgentImpl.setConfigService(config);
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    Assert.assertNull(captureAgentImpl.getAgentState());
    captureAgentImpl.activate(null);
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    p.clear();
    //NB:  agent's .updated() function takes a Quartz properties file, *NOT* the agent's.  Hence the load here.
    p = loadProperties("config/scheduler.properties");
    captureAgentImpl.updated(p);
    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.RECORDING_ID, recordingID);
    properties.setProperty(CaptureParameters.RECORDING_END, "something");
    waiter = new WaitForState();
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    captureAgentImpl.deactivate();
    captureAgentImpl = null;
    config.deactivate();
    config = null;
    properties = null;
  }

  private XProperties loadProperties(String location) throws IOException {
    XProperties props = new XProperties();
    InputStream s = getClass().getClassLoader().getResourceAsStream(location);
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file from " + location);
    }
    props.load(s);
    return props;
  }

  @Test
  public void testCaptureAgentImpl() throws InterruptedException {
    if (!gstreamerInstalled)
      return;

    // start the capture, assert the recording id is correct
    String id = captureAgentImpl.startCapture(properties);
    Assert.assertEquals(recordingID, id);

    File outputdir = new File(config.getItem("capture.filesystem.cache.capture.url"), id);

    // even with a mock capture, the state should remain capturing until stopCapture has been called
    Assert.assertEquals(AgentState.CAPTURING, captureAgentImpl.getAgentState());

    // affirm the captured media exists in the appropriate location
    String[] devnames = config.getItem(CaptureParameters.CAPTURE_DEVICE_NAMES).split(",");
    Assert.assertTrue(devnames.length >= 1);
    Assert.assertFalse("".equals(devnames[0]));

    for (String devname : devnames) {
      File outputfile = new File(outputdir, config.getItem(CaptureParameters.CAPTURE_DEVICE_PREFIX + devname
              + CaptureParameters.CAPTURE_DEVICE_DEST));
      Assert.assertTrue("Output file " + outputfile.getAbsolutePath() + " doesn't exist.", outputfile.exists());
    }

    // the appropriate files exists, so the capture can be stopped. The agent's state should return to idle.
    Assert.assertTrue(captureAgentImpl.stopCapture(recordingID, true));
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    Assert.assertEquals(RecordingState.CAPTURE_FINISHED,
            captureAgentImpl.loadRecording(new File(captureAgentImpl.getKnownRecordings().get(id).getBaseDir(), id + ".recording"))
                    .getState());

    // Test to make sure the manifest file gets created correctly.
    manifestFile = new File(outputdir.getAbsolutePath(), CaptureParameters.MANIFEST_NAME);
    // Wait and see if the manifest file is actually there.
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (manifestFile != null) {
          return manifestFile.exists();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("Manifest file does not exist!", manifestFile.exists());

    // Test to make sure that the zipped media is created.
    zippedMedia = new File(outputdir.getAbsolutePath(), CaptureParameters.ZIP_NAME);
    // Wait and see if the zipped media file is actually there.
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (zippedMedia != null) {
          return zippedMedia.exists();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("Zipped media file does not exist!", zippedMedia.exists());
  }

  private void buildRecordingState(String id, String state) throws IOException {
    if (!gstreamerInstalled)
      return;

    captureAgentImpl.setRecordingState(id, state);
    Assert.assertEquals(state, captureAgentImpl.getRecordingState(id).getState());
    RecordingImpl rec = (RecordingImpl) captureAgentImpl.getKnownRecordings().get(id);
    String newID = rec.getID().split("-")[0] + "-" + rec.getState();
    rec.id = newID;
    Assert.assertEquals(newID, captureAgentImpl.getRecordingState(id).getID());
    Assert.assertEquals(state, captureAgentImpl.getRecordingState(id).getState());
    captureAgentImpl.serializeRecording(id);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    RecordingImpl r = captureAgentImpl.loadRecording(new File(rec.getBaseDir(), newID + ".recording"));
    Assert.assertEquals(state, r.getState());
    Assert.assertEquals(newID, r.getID());
  }

  @Test
  public void testRecordingLoadJob() throws ConfigurationException, IOException, InterruptedException {
    if (!gstreamerInstalled)
      return;

    // Put a recording into the agent, then kill it mid-capture
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());
    String id = captureAgentImpl.startCapture(properties);
    Assert.assertEquals(1, captureAgentImpl.getKnownRecordings().size());
    Thread.sleep(20000);
    captureAgentImpl.deactivate();
    captureAgentImpl = null;
    // Bring the agent back up and check to make sure it reloads the recording
    captureAgentImpl = new CaptureAgentImpl();
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    captureAgentImpl.setConfigService(config);
    captureAgentImpl.activate(null);
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());
    captureAgentImpl.updated(loadProperties("config/scheduler.properties"));
    captureAgentImpl.getSchedulerImpl().setCaptureAgent(captureAgentImpl);
    captureAgentImpl.getSchedulerImpl().stopScheduler();
    captureAgentImpl.createRecordingLoadTask(1);
    System.out.println("Waiting 5 seconds to make sure the scheduler has time to load...");
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null && captureAgentImpl.getKnownRecordings() != null) {
          return captureAgentImpl.getKnownRecordings().size() >= 1;
        } else {
          return false;
        }
      }
    });
    Assert.assertEquals(id, captureAgentImpl.getKnownRecordings().get(id).getID());
  }

  @Test
  public void testRecordingLoadMethod() throws IOException, ConfigurationException {
    if (!gstreamerInstalled)
      return;
    captureAgentImpl.deactivate();
    captureAgentImpl = null;

    // Create the agent and verify some of the error handling logic
    captureAgentImpl = new CaptureAgentImpl();
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());
    captureAgentImpl.loadRecordingsFromDisk();
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());

    captureAgentImpl.setConfigService(config);

    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());
    captureAgentImpl.loadRecordingsFromDisk();
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());

    captureAgentImpl.activate(null);

    // More error handling tests
    String backup = config.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL);
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, null);
    captureAgentImpl.loadRecordingsFromDisk();
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL,
            getClass().getClassLoader().getResource("config/capture.properties").getFile());
    captureAgentImpl.loadRecordingsFromDisk();
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, backup);
    captureAgentImpl.loadRecordingsFromDisk();
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());

    String id = captureAgentImpl.startCapture(properties);
    Assert.assertEquals(recordingID, id);

    // even with a mock capture, the state should remain capturing until stopCapture has been called
    Assert.assertEquals(AgentState.CAPTURING, captureAgentImpl.getAgentState());
    buildRecordingState(id, RecordingState.CAPTURE_ERROR);
    buildRecordingState(id, RecordingState.CAPTURE_FINISHED);
    buildRecordingState(id, RecordingState.CAPTURING);
    buildRecordingState(id, RecordingState.MANIFEST);
    buildRecordingState(id, RecordingState.MANIFEST_FINISHED);
    buildRecordingState(id, RecordingState.MANIFEST_ERROR);
    buildRecordingState(id, RecordingState.COMPRESSING);
    buildRecordingState(id, RecordingState.COMPRESSING_ERROR);
    buildRecordingState(id, RecordingState.UPLOAD_ERROR);
    buildRecordingState(id, RecordingState.UPLOADING);
    buildRecordingState(id, RecordingState.UPLOAD_FINISHED);
    buildRecordingState(id, RecordingState.UNKNOWN);

    captureAgentImpl.deactivate();
    captureAgentImpl = null;

    captureAgentImpl = new CaptureAgentImpl();
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    captureAgentImpl.setConfigService(config);
    captureAgentImpl.activate(null);
    captureAgentImpl.updated(loadProperties("config/scheduler.properties"));
    Assert.assertEquals(0, captureAgentImpl.getKnownRecordings().size());
    captureAgentImpl.getSchedulerImpl().stopScheduler();
    captureAgentImpl.loadRecordingsFromDisk();

    Assert.assertEquals(12, captureAgentImpl.getKnownRecordings().size());
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.CAPTURE_ERROR));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.CAPTURE_FINISHED));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.CAPTURING));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.MANIFEST));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.MANIFEST_FINISHED));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.MANIFEST_ERROR));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.COMPRESSING));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.COMPRESSING_ERROR));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.UPLOADING));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.UPLOAD_ERROR));
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id)); //This is the recording that was actually started
    Assert.assertNotNull(captureAgentImpl.getKnownRecordings().get(id + "-" + RecordingState.UPLOAD_FINISHED));

    captureAgentImpl.loadRecordingsFromDisk();
    Assert.assertEquals(12, captureAgentImpl.getKnownRecordings().size());
  }

  @Test
  public void captureAgentImplWillWaitForConfigurationManagerUpdate() throws IOException, ConfigurationException,
          InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    Properties p = setupConfigurationManagerProperties();
    captureAgentImpl = new CaptureAgentImpl();
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    captureAgentImpl.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return !captureAgentImpl.isRefreshed() && !captureAgentImpl.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertFalse("The configuration manager is just created it shouldn't be updated yet.", captureAgentImpl.isRefreshed());
    Assert.assertFalse("The agent is just created it shouldn't be updated either", captureAgentImpl.isUpdated());
    captureAgentImpl.updated(loadProperties("config/scheduler.properties"));
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return !captureAgentImpl.isRefreshed() && captureAgentImpl.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertFalse("The config manager hasn't been updated so should not be refreshed.", captureAgentImpl.isRefreshed());
    Assert.assertTrue("The agent has been updated, so updated should be true.", captureAgentImpl.isUpdated());
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return captureAgentImpl.isUpdated() && captureAgentImpl.isRefreshed() && (captureAgentImpl.getSchedulerImpl() != null);
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is now updated so refreshed should be true.", captureAgentImpl.isRefreshed());
    Assert.assertTrue("The agent should still be updated.", captureAgentImpl.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", captureAgentImpl.getSchedulerImpl());
  }

  @Test
  public void configurationManagerRefreshWillWaitForCaptureAgentUpdate() throws IOException, ConfigurationException,
          InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    Properties p = setupConfigurationManagerProperties();
    captureAgentImpl = new CaptureAgentImpl();
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    captureAgentImpl.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return !captureAgentImpl.isRefreshed() && !captureAgentImpl.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertFalse("The configuration manager is just created it shouldn't be updated yet.", captureAgentImpl.isRefreshed());
    Assert.assertFalse("The agent is just created it shouldn't be updated either", captureAgentImpl.isUpdated());
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return captureAgentImpl.isRefreshed() && !captureAgentImpl.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is now updated so refreshed should be true.", captureAgentImpl.isRefreshed());
    Assert.assertFalse("The agent should still not be updated.", captureAgentImpl.isUpdated());
    captureAgentImpl.updated(loadProperties("config/scheduler.properties"));
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return captureAgentImpl.isRefreshed() && captureAgentImpl.isUpdated() && captureAgentImpl.getSchedulerImpl() != null;
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is now updated so refreshed should be true.", captureAgentImpl.isRefreshed());
    Assert.assertTrue("The agent should still be updated.", captureAgentImpl.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", captureAgentImpl.getSchedulerImpl());
  }

  @Test
  public void configurationManagerComingUpCompletelyBeforeCaptureAgentImplOkay() throws IOException,
          ConfigurationException, InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    Properties p = setupConfigurationManagerProperties();
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (config != null) {
          return config.isInitialized();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue(config.isInitialized());

    captureAgentImpl = new CaptureAgentImpl();
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    captureAgentImpl.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return captureAgentImpl.isRefreshed();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The configuration manager is fully up, so it should refresh the agent.", captureAgentImpl.isRefreshed());
    Assert.assertFalse("The agent is just created it shouldn't be updated either", captureAgentImpl.isUpdated());
    captureAgentImpl.updated(loadProperties("config/scheduler.properties"));
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return captureAgentImpl.isRefreshed() && captureAgentImpl.isUpdated() && captureAgentImpl.getSchedulerImpl() != null;
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is still updated so refreshed should be true.", captureAgentImpl.isRefreshed());
    Assert.assertTrue("The agent should be updated.", captureAgentImpl.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", captureAgentImpl.getSchedulerImpl());
  }

  @Test
  public void captureAgentImplComingUpFullyBeforeConfigurationManagerOkay() throws IOException,
          ConfigurationException, InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    captureAgentImpl = new CaptureAgentImpl();
    //captureAgentImpl.setCaptureFramework(new GStreamerCaptureFramework());
    captureAgentImpl.updated(loadProperties("config/scheduler.properties"));
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return !captureAgentImpl.isRefreshed() && captureAgentImpl.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertFalse("The config manager should still be waiting for an update.", captureAgentImpl.isRefreshed());
    Assert.assertTrue("The agent should be updated.", captureAgentImpl.isUpdated());
    Properties p = setupConfigurationManagerProperties();
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (config != null) {
          return config.isInitialized();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue(config.isInitialized());
    captureAgentImpl.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (captureAgentImpl != null) {
          return captureAgentImpl.isRefreshed() && captureAgentImpl.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager should be updated.", captureAgentImpl.isRefreshed());
    Assert.assertTrue("The agent should be updated.", captureAgentImpl.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", captureAgentImpl.getSchedulerImpl());
  }

  private Properties setupConfigurationManagerProperties() throws IOException {
    Properties p = loadProperties("config/capture.properties");
    p.put("org.opencastproject.storage.dir",
            new File("./target/", "capture-agent-test").getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
    p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    return p;
  }
}
