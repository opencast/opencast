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
package org.opencastproject.silencedetection.gstreamer;

import java.util.Properties;
import org.gstreamer.ClockTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.silencedetection.api.MediaSegment;
import org.opencastproject.silencedetection.api.MediaSegments;
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException;
import org.opencastproject.silencedetection.impl.SilenceDetectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wsmirnow
 */
public class SilenceDetectorTest extends GstreamerAbstractTest {

  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectorTest.class);

  private static boolean gstreamerInstalled = false;

  @BeforeClass
  public static void setUpClass() throws Exception {
    try {
      GstreamerAbstractTest.setUpClass();
      gstreamerInstalled = true;
    } catch (Throwable e) {
      gstreamerInstalled = false;
      logger.warn("Skipping silence detection tests due to unsatisifed gstreamer installation: {}",
              e.getMessage());
      return;
    }

    /* gstreamer-core */
    if (!testGstreamerElementInstalled(GstreamerElements.FILESRC)) {
      gstreamerInstalled = false;

      logger.info("Skip tests because gstreamer-base is not installed!");
      return;
    }
    /* gstreamer-plugins-base*/
    if (!testGstreamerElementInstalled(GstreamerElements.DECODEBIN)) {
      gstreamerInstalled = false;
      logger.info("Skip tests because gstreamer-plugins-base is not installed!");
      return;
    }
    /* gstreamer-plugins-good */
    if (!testGstreamerElementInstalled(GstreamerElements.CUTTER)) {
      gstreamerInstalled = false;

      logger.info("Skip tests because gstreamer-plugins-good is not installed!");
      return;
    }
  }

  @Test
  public void detectorTest() {
    if (!gstreamerInstalled) return;

    logger.info("segmenting audio file '{}'...", audioFilePath);
    try {
      GstreamerSilenceDetector silenceDetector = new GstreamerSilenceDetector(new Properties(), "track-1", audioFilePath);
      Assert.assertNull(silenceDetector.getMediaSegments());

      silenceDetector.runDetection();

      MediaSegments segments = silenceDetector.getMediaSegments();
      Assert.assertNotNull(segments);
      Assert.assertTrue(segments.getMediaSegments().size() > 0);

      logger.info("segments found:");
      for (MediaSegment segment : segments.getMediaSegments()) {
        Assert.assertTrue(segment.getSegmentStart() < segment.getSegmentStop());
        logger.info("{} ({}) - {} ({})", new String[] {
          ClockTime.fromMillis(segment.getSegmentStart()).toString(),
          Long.toString(segment.getSegmentStart()),
          ClockTime.fromMillis(segment.getSegmentStop()).toString(),
          Long.toString(segment.getSegmentStop())
        });
      }

    } catch (SilenceDetectionFailedException ex) {
      Assert.fail();
    } catch (PipelineBuildException ex) {
      Assert.fail();
    }
  }

  @Test
  public void detectorSingleSegmentTest() {
    if (!gstreamerInstalled) return;

    logger.info("segmenting audio file '{}' with minimum silence length of 30 sec...", audioFilePath);

    Properties properties = new Properties();
    properties.setProperty(SilenceDetectionProperties.SILENCE_MIN_LENGTH, "30");
    // properties.setProperty(SilenceDetectionProperties.SILENCE_THRESHOLD_DB, "-75");

    try {
      GstreamerSilenceDetector silenceDetector = new GstreamerSilenceDetector(properties, "track-1", audioFilePath);
      Assert.assertNull(silenceDetector.getMediaSegments());

      silenceDetector.runDetection();

      MediaSegments segments = silenceDetector.getMediaSegments();
      Assert.assertNotNull(segments);
      Assert.assertTrue(segments.getMediaSegments().size() == 1);

      MediaSegment segment = segments.getMediaSegments().get(0);
      Assert.assertTrue(segment.getSegmentStart() < segment.getSegmentStop());
      logger.info("segments found:");
      logger.info("{} ({}) - {} ({})", new String[] {
        ClockTime.fromMillis(segment.getSegmentStart()).toString(),
        Long.toString(segment.getSegmentStart()),
        ClockTime.fromMillis(segment.getSegmentStop()).toString(),
        Long.toString(segment.getSegmentStop())
      });

    } catch (SilenceDetectionFailedException ex) {
      Assert.fail();
    } catch (PipelineBuildException ex) {
      Assert.fail();
    }
  }

  @Test(expected = SilenceDetectionFailedException.class)
  public void detectorFailTest() throws SilenceDetectionFailedException {
    if (!gstreamerInstalled)
      throw new SilenceDetectionFailedException("Gstreamer is not installed.");

    String videoFilePath = "foo.bar";
    logger.info("segmenting video only file '{}' should fail...", videoFilePath);
    try {
      GstreamerSilenceDetector silenceDetector = new GstreamerSilenceDetector(new Properties(), "track-1", videoFilePath);
      Assert.assertNull(silenceDetector.getMediaSegments());

      silenceDetector.runDetection();
      Assert.fail();

    } catch (PipelineBuildException ex) {
      Assert.fail();
    }
  }
}
