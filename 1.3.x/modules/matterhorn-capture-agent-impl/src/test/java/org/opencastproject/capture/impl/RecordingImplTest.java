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
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.util.ConfigurationException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

public class RecordingImplTest {
  private RecordingImpl rec = null;
  private ConfigurationManager configManager = null;

  private File testDir = null;

  @Before
  public void setUp() throws org.osgi.service.cm.ConfigurationException, IOException {
    // Setup the configuration manager
    configManager = new ConfigurationManager();
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    testDir = new File("./target", "recording-test");
    configManager.setItem("org.opencastproject.storage.dir", testDir.getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configManager.updated(sourceProps);
  }

  @After
  public void tearDown() {
    rec = null;
    FileUtils.deleteQuietly(testDir);
  }

  @Test
  public void testEdgeRecording() throws IllegalArgumentException, ConfigurationException, IOException,
          MediaPackageException {
    // Let's test some edge-casey recordings
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(), null);
    Assert.assertNotNull(rec);
    Assert.assertEquals(System.getProperty("java.io.tmpdir"),
            rec.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    Assert.assertNull(rec.setProperty("test", "foo"));
    Assert.assertEquals("foo", rec.setProperty("test", "bar"));
    FileUtils.deleteQuietly(rec.getBaseDir());
  }

  @Test
  public void testUnscheduledRecording() throws IllegalArgumentException, ConfigurationException, IOException,
          MediaPackageException {
    // Create the recording for an unscheduled capture
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(),
            configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL),
            rec.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    Assert.assertTrue(Pattern.matches("Unscheduled-demo_capture_agent-\\d+", rec.getID()));
    Assert.assertTrue(Pattern.matches("Unscheduled-demo_capture_agent-\\d+",
            rec.getProperty(CaptureParameters.RECORDING_ID)));
    Assert.assertTrue(Pattern.matches("Unscheduled-demo_capture_agent-\\d+",
            rec.getProperties().getProperty(CaptureParameters.RECORDING_ID)));
  }

  @Test
  public void testScheduledCaptureWithRecordingID() throws IllegalArgumentException, ConfigurationException,
          IOException, MediaPackageException, org.osgi.service.cm.ConfigurationException {
    configManager.setItem(CaptureParameters.RECORDING_ID, "MyTestRecording");

    // Create the recording for a scheduled capture which only has its recording id set
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(),
            configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL),
            rec.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    Assert.assertEquals("MyTestRecording", rec.getID());
    Assert.assertEquals("MyTestRecording", rec.getProperty(CaptureParameters.RECORDING_ID));
    Assert.assertEquals("MyTestRecording", rec.getProperties().getProperty(CaptureParameters.RECORDING_ID));
    configManager.setItem(CaptureParameters.RECORDING_ID, null);
  }

  @Test
  public void testScheduledCaptureWithRootURL() throws IllegalArgumentException, ConfigurationException, IOException,
          MediaPackageException {
    File baseDir = new File(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, new File(baseDir, "MyTestRecording").getAbsolutePath());

    // Create the recording for a scheduled capture which only has its root url set
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(),
            configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(new File(baseDir, "MyTestRecording"), rec.getBaseDir());
    Assert.assertEquals("MyTestRecording", rec.getID());
    Assert.assertEquals("MyTestRecording", rec.getProperty(CaptureParameters.RECORDING_ID));
    Assert.assertEquals("MyTestRecording", rec.getProperties().getProperty(CaptureParameters.RECORDING_ID));
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, null);
  }

  @Test
  public void testFullySpecCapture() throws IllegalArgumentException, ConfigurationException, IOException,
          MediaPackageException {
    File baseDir = new File(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    String recordingID = "MyTestRecording";
    configManager.setItem(CaptureParameters.RECORDING_ID, recordingID);
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, new File(baseDir, recordingID).getAbsolutePath());

    // Create the recording for a scheduled capture which only has its root url set
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(),
            configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(new File(baseDir, recordingID), rec.getBaseDir());
    Assert.assertEquals(recordingID, rec.getID());
    Assert.assertEquals(recordingID, rec.getProperty(CaptureParameters.RECORDING_ID));
    Assert.assertEquals(recordingID, rec.getProperties().getProperty(CaptureParameters.RECORDING_ID));
    configManager.setItem(CaptureParameters.RECORDING_ID, null);
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, null);
  }
}
