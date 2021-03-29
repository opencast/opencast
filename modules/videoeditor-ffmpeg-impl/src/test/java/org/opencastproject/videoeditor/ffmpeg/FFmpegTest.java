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

package org.opencastproject.videoeditor.ffmpeg;

import org.opencastproject.videoeditor.impl.VideoClip;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

import junit.framework.Assert;

/**
 * Tests the ffmpeg concatenation service
 */
public class FFmpegTest {

  /** The logging instance */
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FFmpegTest.class);
  private static final String FFMPEG_BINARY = "ffmpeg";

  protected String inputFilePath;
  protected String outputFilePath;
  protected static boolean ffmpegInstalled = true;

  public FFmpegTest() throws URISyntaxException {
    inputFilePath = new File(getClass().getResource("/testresources/testvideo_320x180.mp4").toURI()).getAbsolutePath();
    outputFilePath = new File("target/testoutput/mux.mp4").getAbsolutePath();
  }

  @BeforeClass
  public static void testForFFmpeg() {
    try {
      Process p = new ProcessBuilder(FFMPEG_BINARY, "-version").start();
      if (p.waitFor() != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping composer tests due to missing FFmpeg");
      ffmpegInstalled = false;
    }
  }

  @Before
  public void setUp() {
    if (new File(outputFilePath).exists())  {
      new File(outputFilePath).delete();
    } else if (!new File(outputFilePath).getParentFile().exists()) {
      new File(outputFilePath).getParentFile().mkdir();
    }
  }


  /**
   * Test if ffmpeg can split and join 1 file
   * Skip if no ffmpeg
   */
  @Test
  public void ffmpegEditTest() throws Exception {

    if (!ffmpegInstalled) {
      return;
    }
    ArrayList<String> input = new ArrayList<String>();
    ArrayList<VideoClip> clips = new ArrayList<VideoClip>();
    clips.add(new VideoClip(0, 0.0, 10.0));
    clips.add(new VideoClip(0, 25.0, 44.0));
    input.add(inputFilePath);
    FFmpegEdit fmp = new FFmpegEdit();
    fmp.processEdits(input, outputFilePath, null, clips);
    Assert.assertTrue("Edited File is nonzero",new File(outputFilePath).length() > 0);
    logger.info("Ffmpeg concat 2 clips from 1 file OK!");

  }

  /**
   * Test if ffmpeg can split and join 2 files of the same size and frame rate
   * Different sizes will fail - need to add scale=WxH
   * Skip if no ffmpeg
   */
  @Test
  public void ffmpegEditTest2Sources() throws Exception {

    if (!ffmpegInstalled) {
      return;
    }
    ArrayList<String> input = new ArrayList<String>();
    ArrayList<VideoClip> clips = new ArrayList<VideoClip>();
    clips.add(new VideoClip(0, 0.0, 10.0));
    clips.add(new VideoClip(1, 25.0, 44.0));
    input.add(inputFilePath);
    input.add(inputFilePath);
    FFmpegEdit fmp = new FFmpegEdit();
    fmp.processEdits(input, outputFilePath, "320x180", clips);    // Both clips are scaled to 320x180
    Assert.assertTrue("Edited File is nonzero",new File(outputFilePath).length() > 0);
    logger.info("Ffmpeg concat 2 clips from 2 files OK!");
  }
}
