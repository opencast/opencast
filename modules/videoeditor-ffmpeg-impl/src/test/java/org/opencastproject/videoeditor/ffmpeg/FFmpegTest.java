/*
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the ffmpeg concatenation service
 */
public class FFmpegTest {

  /** The logging instance */
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FFmpegTest.class);
  private static final String FFMPEG_BINARY = "ffmpeg";

  protected String inputFilePath;
  /** 2 s without color properties followed by 2 s with BT.601 TV range, joined without re-encoding */
  protected String colorChangeFilePath;
  protected String outputFilePath;
  protected static boolean ffmpegInstalled = true;

  public FFmpegTest() throws URISyntaxException {
    inputFilePath = new File(getClass().getResource("/testresources/testvideo_320x180.mp4").toURI()).getAbsolutePath();
    colorChangeFilePath = new File(getClass().getResource("/testresources/testvideo_colorchange.mp4").toURI())
        .getAbsolutePath();
    outputFilePath = new File("target/testoutput/mux.mp4").getAbsolutePath();
  }

  @BeforeClass
  public static void testForFFmpeg() {
    try {
      Process p = new ProcessBuilder(FFMPEG_BINARY, "-version").start();
      if (p.waitFor() != 0) {
        throw new IllegalStateException();
      }
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
    ArrayList<String> input = new ArrayList<>();
    ArrayList<VideoClip> clips = new ArrayList<>();
    clips.add(new VideoClip(0, 0, 10000));
    clips.add(new VideoClip(0, 25000, 44000));
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
    ArrayList<String> input = new ArrayList<>();
    ArrayList<VideoClip> clips = new ArrayList<>();
    clips.add(new VideoClip(0, 0, 10000));
    clips.add(new VideoClip(1, 25000, 44000));
    input.add(inputFilePath);
    input.add(inputFilePath);
    FFmpegEdit fmp = new FFmpegEdit();
    fmp.processEdits(input, outputFilePath, "320x180", clips);    // Both clips are scaled to 320x180
    Assert.assertTrue("Edited File is nonzero",new File(outputFilePath).length() > 0);
    logger.info("Ffmpeg concat 2 clips from 2 files OK!");
  }

  /**
   * Test that the filters are not reinitialized if the frame properties change mid-stream
   */
  @Test
  public void ffmpegEditNoFilterReinitTest() throws Exception {
    ArrayList<String> input = new ArrayList<>();
    input.add(inputFilePath);
    input.add(inputFilePath);
    ArrayList<VideoClip> clips = new ArrayList<>();
    clips.add(new VideoClip(0, 0, 10000));
    clips.add(new VideoClip(1, 25000, 44000));
    List<String> command = new FFmpegEdit().makeEdits(input, outputFilePath, null, clips, true, true);
    for (int i = 0; i < command.size(); i++) {
      if ("-i".equals(command.get(i))) {
        Assert.assertEquals("-reinit_filter:v", command.get(i - 2));
        Assert.assertEquals("0", command.get(i - 1));
      }
    }
  }

  /**
   * Test that a clip of a video whose color properties change mid-stream gets the full duration.
   * Reinitializing the filters at the change would reset the timestamps and drop frames.
   * Skip if no ffmpeg
   */
  @Test
  public void ffmpegEditColorChangeTest() throws Exception {
    if (!ffmpegInstalled) {
      return;
    }
    ArrayList<String> input = new ArrayList<>();
    ArrayList<VideoClip> clips = new ArrayList<>();
    clips.add(new VideoClip(0, 500, 3500));
    input.add(colorChangeFilePath);
    new FFmpegEdit().processEdits(input, outputFilePath, null, clips);

    Process p = new ProcessBuilder("ffprobe", "-v", "error", "-select_streams", "v", "-show_entries",
        "stream=duration", "-of", "csv=p=0", outputFilePath).start();
    String duration = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    Assert.assertEquals(0, p.waitFor());
    Assert.assertEquals(3.0, Double.parseDouble(duration), 0.1);
  }
}
