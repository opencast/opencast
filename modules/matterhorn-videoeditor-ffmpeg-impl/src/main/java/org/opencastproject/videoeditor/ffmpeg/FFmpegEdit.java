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

package org.opencastproject.videoeditor.ffmpeg;


import org.opencastproject.util.IoSupport;
import org.opencastproject.videoeditor.impl.VideoClip;
import org.opencastproject.videoeditor.impl.VideoEditorProperties;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * FFmpeg wrappers:
 * processEdits:    process SMIL definitions of segments into one consecutive video
 *                  There is a fade in and a fade out at the beginning and end of each clip
 *
 */
public class FFmpegEdit {

  private static final Logger logger = LoggerFactory.getLogger(FFmpegEdit.class);
  private static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";
  private static final String CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpegpath";

  private static final String DEFAULT_FFMPEG_PROPERTIES = "-strict -2 -preset faster -crf 18";
  public static final String DEFAULT_OUTPUT_FILE_EXTENSION = ".mp4";
  private static final String DEFAULT_AUDIO_FADE = "2.0";
  private static final String DEFAULT_VIDEO_FADE = "2.0";
  private static String binary = FFMPEG_BINARY_DEFAULT;

  protected float vfade;
  protected float afade;
  protected String ffmpegProperties = DEFAULT_FFMPEG_PROPERTIES;
  protected String videoCodec = null;  // By default, use the same codec as source
  protected String audioCodec = null;

  public static void init(BundleContext bundleContext) {
    String path = bundleContext.getProperty(CONFIG_FFMPEG_PATH);

    if (path != null) {
      binary = path;
    }
  }

  public FFmpegEdit()
  {
    this.afade = Float.parseFloat(DEFAULT_AUDIO_FADE);
    this.vfade = Float.parseFloat(DEFAULT_VIDEO_FADE);
    this.ffmpegProperties = DEFAULT_FFMPEG_PROPERTIES;
  }

  /*
   * Init with properties
   */
  public FFmpegEdit(Properties properties)
  {
    String fade = properties.getProperty(VideoEditorProperties.AUDIO_FADE, DEFAULT_AUDIO_FADE);
    try {
      this.afade = Float.parseFloat(fade);
    } catch (Exception e) {
      logger.error("Unable to parse audio fade duration {}. Falling back to default value.", DEFAULT_AUDIO_FADE);
      this.afade = Float.parseFloat(DEFAULT_AUDIO_FADE);
    }
    fade = properties.getProperty(VideoEditorProperties.VIDEO_FADE, DEFAULT_VIDEO_FADE);
    try {
      this.vfade = Float.parseFloat(fade);
    } catch (Exception e) {
      logger.error("Unable to parse video fade duration {}. Falling back to default value.", DEFAULT_VIDEO_FADE);
      this.vfade = Float.parseFloat(DEFAULT_VIDEO_FADE);
    }
    this.ffmpegProperties = properties.getProperty(VideoEditorProperties.FFMPEG_PROPERTIES, DEFAULT_FFMPEG_PROPERTIES);
    this.videoCodec = properties.getProperty(VideoEditorProperties.VIDEO_CODEC, null);
    this.audioCodec = properties.getProperty(VideoEditorProperties.AUDIO_CODEC, null);
  }


  public String processEdits(List<String> inputfiles, String dest, String outputSize, List<VideoClip> cleanclips)
          throws Exception {
    List<String> cmd = makeEdits(inputfiles, dest, outputSize, cleanclips);
    return run(cmd);
  }

  /* Run the ffmpeg command with the params
   * Takes a list of words as params, the output is logged
   */
  private String run(List<String> params) {
    BufferedReader in = null;
    Process encoderProcess = null;
    try {
      params.add(0, binary);
      logger.info("executing command: " + StringUtils.join(params, " "));
      ProcessBuilder pbuilder = new ProcessBuilder(params);
      pbuilder.redirectErrorStream(true);
      encoderProcess = pbuilder.start();
      in = new BufferedReader(new InputStreamReader(
              encoderProcess.getInputStream()));
      String line;
      int n = 5;
      while ((line = in.readLine()) != null) {
    if (n-- > 0)
      logger.info(line);
      }

      // wait until the task is finished
      encoderProcess.waitFor();
      int exitCode = encoderProcess.exitValue();
      if (exitCode != 0) {
        throw new Exception("Ffmpeg exited abnormally with status " + exitCode);
      }

    } catch (Exception ex) {
      logger.error("VideoEditor ffmpeg failed", ex);
      return ex.toString();
    } finally {
      IoSupport.closeQuietly(in);
      IoSupport.closeQuietly(encoderProcess);
    }
    return null;
  }

  /*
   * Construct the ffmpeg command from  src, in-out points and output resolution
   * Inputfile is an ordered list of video src
   * clips is a list of edit points indexing into the video src list
   * outputResolution when specified is the size to which all the clips will scale
   * NOTE: This command will fail if the sizes are mismatched or if some of the clips lacked audio
   */
  public List<String> makeEdits(List<String> inputfiles, String dest, String outputResolution,
          List<VideoClip> clips) throws Exception {

    DecimalFormat f = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    int n = clips.size();
    int i;
    String outmap = "";
    String scale = "";
    List<String> command = new ArrayList<String>();
    List<String> vpads = new ArrayList<String>();
    List<String> apads = new ArrayList<String>();
    List<String> clauses = new ArrayList<String>(); // The clauses are ordered

    if (n > 1) { // Create the input pads if we have multiple segments
      for (i = 0; i < n ; i++) {
        vpads.add("[v" + i + "]");  // post filter
        apads.add("[a" + i + "]");
      }
    }
    if (outputResolution != null && outputResolution.length() > 3) // format is "<width>x<height>"
      scale = ",scale=" + outputResolution;    // scale each clip to the same size

    for (i = 0; i < n; i++) { // Examine each clip
      // get clip and add fades to each clip
      VideoClip vclip = clips.get(i);
      int fileindx = vclip.getSrc();   // get source file by index
      double inpt = vclip.getStart();     // get in points
      double duration = vclip.getDuration();

      String vfadeFilter = "";
      /* Only include fade into the filter graph if necessary */
      if (vfade > 0.00001) {
        double vend = duration - vfade;
        vfadeFilter = ",fade=t=in:st=0:d=" + vfade + ",fade=t=out:st=" + f.format(vend) + ":d=" + vfade;
      }
      /* Add filters for video */
      String clip = "[" + fileindx + ":v]trim=" + f.format(inpt) + ":duration=" + f.format(duration)
                + scale + ",setpts=PTS-STARTPTS" + vfadeFilter + "[v" + i + "]";

      clauses.add(clip);

      String afadeFilter = "";
      /* Only include fade into the filter graph if necessary */
      if (afade > 0.00001) {
        double aend = duration - afade;
        afadeFilter = ",afade=t=in:st=0:d=" + afade + ",afade=t=out:st=" + f.format(aend) + ":d=" + afade;
      }
      /* Add filters for audio */
      clip = "[" + fileindx + ":a]atrim=" + f.format(inpt) + ":duration=" + f.format(duration)
                + ",asetpts=PTS-STARTPTS" + afadeFilter + "[a"
                + i + "]";
      clauses.add(clip);
    }
    if (n > 1) { // concat the outpads when there are more then 1 per stream
                  // use unsafe because different files may have different SAR/framerate
      clauses.add(StringUtils.join(vpads, "") + "concat=n=" + n + ":unsafe=1[ov0]"); // concat video clips
      clauses.add(StringUtils.join(apads, "") + "concat=n=" + n
                + ":v=0:a=1[oa0]"); // concat audio clips in stream 0, video in stream 1
      outmap = "o";                 // if more than one clip
    }
    command.add("-y");      // overwrite old pathname
    for (String o : inputfiles) {
      command.add("-i");   // Add inputfile in the order of entry
      command.add(o);
    }
    command.add("-filter_complex");
    command.add(StringUtils.join(clauses, ";"));
    String[] options = ffmpegProperties.split(" ");
    command.addAll(Arrays.asList(options));
    command.add("-map");
    command.add("[" + outmap + "a0]");
    command.add("-map");
    command.add("[" + outmap + "v0]");
    if (videoCodec != null) { // If using different codecs from source, add them here
      command.add("-c:v");
      command.add(videoCodec);
    }
    if (audioCodec != null) {
      command.add("-c:a");
      command.add(audioCodec);
    }
    command.add(dest);

    return command;
  }
}
