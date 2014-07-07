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
package org.opencastproject.composer.gstreamer;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test class for GStreamer Encoding Engine.
 */
public class GStreamerEncoderEngineTest {

  /** Factory for creating GStreamerEncoderEngine */
  private static GStreamerFactory factory;
  /** Files used for testing */
  private File audioFile1;
  private File audioFile2;
  private File videoFile;
  private File[] resultingFiles;

  /** Logger utility */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerEncoderEngineTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // rapid initializing and deinitializing of Gstreamer (such as after each test) may lead to unexpected behavior and
    // second test would always fail with random error. That's why GStreamer is initialized and deinitialized only once
    // in whole suit.
    factory = new GStreamerFactory();
    factory.activate(null);
  }

  @Before
  public void setUp() throws Exception {
    audioFile1 = new File(GStreamerEncoderEngineTest.class.getResource("/audio_1.mp3").toURI());
    audioFile2 = new File(GStreamerEncoderEngineTest.class.getResource("/audio_2.mp3").toURI());
    videoFile = new File(GStreamerEncoderEngineTest.class.getResource("/camera.mpg").toURI());
  }

  @Test
  public void testSingleThread() throws Exception {

    // create properties for this test
    Map<String, String> properties = new HashMap<String, String>();
    properties
            .put("gstreamer.pipeline",
                    "filesrc location=#{in.video.path} ! decodebin ! audioconvert ! audioresample ! lame bitrate=#{out.bitrate} ! filesink location=#{out.file.path}");
    properties.put("out.bitrate", "320");

    EncoderEngine engine = factory.newEncoderEngine(null);
    EncodingProfile profile = createEncodingProfile("SingleThreadTest", ".mp3", properties);
    File encodedFile = engine.encode(audioFile1, profile, properties);
    resultingFiles = new File[] { encodedFile };

    Assert.assertTrue("Invalid file created!", encodedFile.exists() && encodedFile.length() > 0);
  }

  @Test
  public void testMultithreading() throws Exception {

    final Map<String, String> properties = new HashMap<String, String>();
    properties
            .put("gstreamer.pipeline",
                    "filesrc location=#{in.video.path} ! decodebin ! audioconvert ! audioresample ! lame bitrate=#{out.bitrate} ! filesink location=#{out.file.path}");
    properties.put("out.bitrate", "320");

    final AtomicBoolean error1 = new AtomicBoolean(false);
    final AtomicBoolean error2 = new AtomicBoolean(false);
    resultingFiles = new File[2];

    Runnable task1 = new Runnable() {
      @Override
      public void run() {
        EncoderEngine engine = factory.newEncoderEngine(null);
        EncodingProfile profile = createEncodingProfile("Thread1Test", ".mp3", properties);
        try {
          resultingFiles[0] = engine.encode(audioFile1, profile, properties);
        } catch (EncoderException e) {
          error1.set(true);
        }
      }
    };
    Runnable task2 = new Runnable() {
      @Override
      public void run() {
        EncoderEngine engine = factory.newEncoderEngine(null);
        EncodingProfile profile = createEncodingProfile("Thread2Test", ".mp3", properties);
        try {
          resultingFiles[1] = engine.encode(audioFile2, profile, properties);
        } catch (EncoderException e) {
          error2.set(true);
        }
      }
    };
    Thread th1 = new Thread(task1);
    Thread th2 = new Thread(task2);
    th1.start();
    th2.start();
    th1.join();
    th2.join();

    Assert.assertTrue("Error in first processing pipeline", !error1.get());
    Assert.assertTrue("Invalid file created in first pipeline!",
            resultingFiles[0].exists() && resultingFiles[0].length() > 0);
    Assert.assertTrue("Error in second processing pipeline", !error2.get());
    Assert.assertTrue("Invalid file created in second pipeline!",
            resultingFiles[1].exists() && resultingFiles[1].length() > 0);
  }

  @Test
  public void testImageExtraction() throws Exception {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("gstreamer.image.dimensions", "640x480");

    EncoderEngine engine = factory.newEncoderEngine(null);
    EncodingProfile profile = createEncodingProfile("ImageExtractionTest", ".jpg", properties);
    List<File> result = engine.extract(videoFile, profile, properties, 4);
    resultingFiles = result.toArray(new File[result.size()]);
    for (File image : result) {
      Assert.assertTrue("Invalid file: " + image.getAbsolutePath(), image.exists() && image.length() > 0);
    }
  }

  @Test
  public void multipleImageExtraction() throws Exception {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("gstreamer.image.dimensions", "0x0");

    EncoderEngine engine = factory.newEncoderEngine(null);
    EncodingProfile profile = createEncodingProfile("ImageExtractionTest", "_#{time}.jpg", properties);
    List<File> result = engine.extract(videoFile, profile, properties, 3, 1, 7);
    resultingFiles = result.toArray(new File[result.size()]);
    for (File image : result) {
      Assert.assertTrue("Invalid file: " + image.getAbsolutePath(), image.exists() && image.length() > 0);
    }
  }

  @After
  public void tearDown() throws Exception {
    if (resultingFiles != null) {
      for (File f : resultingFiles) {
        if (f != null && !f.delete()) {
          logger.warn("Could not delete file {}", f.getAbsolutePath());
        }
      }
    }
  }

  @AfterClass
  public static void destroy() throws Exception {
    factory.deactivate(null);
  }

  /**
   * Creates EncodingProfile.
   *
   * @param name
   * @param suffix
   * @param properties
   * @return
   */
  private EncodingProfile createEncodingProfile(final String name, final String suffix,
          final Map<String, String> properties) {
    EncodingProfile profile = new EncodingProfile() {
      @Override
      public boolean isApplicableTo(MediaType type) {
        return false;
      }

      @Override
      public boolean hasExtensions() {
        return false;
      }

      @Override
      public String getSuffix() {
        return suffix;
      }

      @Override
      public Object getSource() {
        return null;
      }

      @Override
      public MediaType getOutputType() {
        return null;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getMimeType() {
        return null;
      }

      @Override
      public String getIdentifier() {
        return name;
      }

      @Override
      public Map<String, String> getExtensions() {
        return properties;
      }

      @Override
      public String getExtension(String key) {
        return properties.get(key);
      }

      @Override
      public MediaType getApplicableMediaType() {
        return null;
      }
    };
    return profile;
  }
}
