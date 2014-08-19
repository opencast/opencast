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
package org.opencastproject.composer.impl;

import static org.junit.Assert.assertTrue;

import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.ffmpeg.FFmpegEncoderEngine;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.StreamHelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test trimming using ffmpeg.
 */
public class VideoConcaternationTest {

  /** the encoding engine */
  private FFmpegEncoderEngine engine;

  /** The temporary directory to store media files */
  private File workingDirectory = null;

  /** Encoding profiles **/
  private Map<String, EncodingProfile> profiles;

  /** True to run the tests */
  private static boolean ffmpegInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoConcaternationTest.class);

  @BeforeClass
  public static void testOcropus() {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    Process p = null;
    try {
      p = new ProcessBuilder(FFmpegEncoderEngine.FFMPEG_BINARY_DEFAULT, "-version").start();
      stdout = new StreamHelper(p.getInputStream());
      stderr = new StreamHelper(p.getErrorStream());
      int status = p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      if (status != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping image conversion tests due to unsatisifed ffmpeg installation");
      ffmpegInstalled = false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    engine = new FFmpegEncoderEngine();
    workingDirectory = FileSupport.getTempDirectory("videoconcatenationtest");
    FileUtils.forceMkdir(workingDirectory);

    URL url = EncodingProfileTest.class.getResource("/encodingprofiles.properties");
    EncodingProfileScanner mgr = new EncodingProfileScanner();
    profiles = mgr.loadFromProperties(new File(url.toURI()));
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.forceDelete(workingDirectory);
  }

  /**
   * Test method for
   * {@link org.opencastproject.composer.impl.ComposerServiceImpl#convertImage(org.opencastproject.mediapackage.Attachment, String)}
   */
  @Test
  @Ignore
  public void testConcat() throws Exception {
    if (!ffmpegInstalled)
      return;
    String videoName1 = "video.mp4";
    String videoName2 = "av.mov";
    URL sourceVideo1Url = getClass().getResource("/" + videoName1);
    URL sourceVideo2Url = getClass().getResource("/" + videoName2);
    File sourceVideo1 = new File(workingDirectory, videoName1);
    File sourceVideo2 = new File(workingDirectory, videoName2);
    File sourceTextFile = new File(workingDirectory, "filesToConcat.txt");
    FileUtils.copyURLToFile(sourceVideo1Url, sourceVideo1);
    FileUtils.copyURLToFile(sourceVideo2Url, sourceVideo2);

    StringBuilder sb = new StringBuilder();
    sb.append("file '").append(sourceVideo1.getAbsolutePath()).append("'").append(IOUtils.LINE_SEPARATOR);
    sb.append("file '").append(sourceVideo2.getAbsolutePath()).append("'").append(IOUtils.LINE_SEPARATOR);
    FileUtils.writeStringToFile(sourceTextFile, sb.toString());

    EncodingProfile imageConversionProfile = profiles.get("concat");
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("videoListFile", FilenameUtils.normalize(sourceTextFile.getAbsolutePath()));
    File concatenedVideo = engine.encode(sourceVideo1, imageConversionProfile, properties).get();

    // These are weak assertions, but anything else would require either integration with another 3rd party tool
    // or manual parsing of ffmpeg output. Instead, we keep this test generic (but weak).
    assertTrue(concatenedVideo.exists());
    assertTrue(concatenedVideo.length() > 0);

  }

}
