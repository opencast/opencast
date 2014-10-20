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

package org.opencastproject.videoeditor.gstreamer;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wsmirnow
 */
public class GstreamerTypeFinderTest extends GstreamerAbstractTest {

  /**
   * The logging instance
   */
  private static final Logger logger = LoggerFactory.getLogger(GstreamerTypeFinderTest.class);

  private static boolean gstreamerInstalled = false;

  @BeforeClass
  public static void setUpClass() throws Exception {
    try {
      GstreamerAbstractTest.setUpClass();
      gstreamerInstalled = true;
    } catch (Throwable e) {
      gstreamerInstalled = false;
      logger.warn("Skipping video editor type finder tests due to unsatisifed gstreamer installation: {}",
              e.getMessage());
      return;
    }

    /* gstreamer-core */
    if (!testGstreamerElementInstalled(GstreamerElements.FILESRC)) {
      gstreamerInstalled = false;

      logger.info("Skip tests because gstreamer-base is not installed!");
      return;
    }
    /* gstreamer-plugins-base */
    if (!testGstreamerElementInstalled(GstreamerElements.DECODEBIN2)) {
      gstreamerInstalled = false;

      logger.info("Skip tests because gstreamer-plugins-base is not installed!");
      return;
    }
  }

  @Test
  public void typefinderAudioTest() {
    if (!gstreamerInstalled) return;

    GstreamerTypeFinder typeFinder = new GstreamerTypeFinder(audioFilePath);
    Assert.assertTrue(typeFinder.isAudioFile());
    Assert.assertFalse(typeFinder.isVideoFile());

    logger.info("audiocaps: " + typeFinder.getAudioCaps().toString());
  }

  @Test
  public void typefinderVideoTest() {
    if (!gstreamerInstalled) return;

    GstreamerTypeFinder typeFinder = new GstreamerTypeFinder(videoFilePath);
    Assert.assertTrue(typeFinder.isVideoFile());
    Assert.assertFalse(typeFinder.isAudioFile());

    logger.info("videocaps: " + typeFinder.getVideoCaps().toString());
  }

  @Test
  public void typefinderMuxedTest() {
    if (!gstreamerInstalled) return;

    GstreamerTypeFinder typeFinder = new GstreamerTypeFinder(muxedFilePath);
    Assert.assertTrue(typeFinder.isAudioFile());
    Assert.assertTrue(typeFinder.isVideoFile());

    logger.info("audiocaps: " + typeFinder.getAudioCaps().toString());
    logger.info("videocaps: " + typeFinder.getVideoCaps().toString());
  }

  @Test
  public void typefinderFailTest() {
    if (!gstreamerInstalled) return;

    GstreamerTypeFinder typeFinder = new GstreamerTypeFinder("foo");
    Assert.assertFalse(typeFinder.isAudioFile());
    Assert.assertFalse(typeFinder.isVideoFile());
  }
}
