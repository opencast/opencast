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

package org.opencastproject.inspection.ffmpeg;

import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzer;
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzerException;
import org.opencastproject.inspection.ffmpeg.api.MediaContainerMetadata;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

//import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
//import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.ffmpeg.api.AudioStreamMetadata;
import org.opencastproject.inspection.ffmpeg.api.VideoStreamMetadata;
//import org.opencastproject.mediapackage.track.BitRateMode;
//import org.opencastproject.mediapackage.track.FrameRateMode;
//import org.opencastproject.mediapackage.track.ScanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Iterator;
import java.util.Map;
import java.util.List;
//import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.Iterator;

//import static org.opencastproject.util.data.Arrays.array;

/**
 * This MediaAnalyzer implementation uses the ffprobe binary of FFmpeg for
 * media analysis.  Also this implementation does not keep control-, text- or
 * other non-audio or video streams and purposefully ignores them during the
 * <code>postProcess()</code> step.
 */
public class FFmpegAnalyzer implements MediaAnalyzer {

  /** Path to the executable */
  protected String binary;

  public static final String FFPROBE_BINARY_CONFIG = "org.opencastproject.inspection.ffprobe.path";
  public static final String FFPROBE_BINARY_DEFAULT = "ffprobe";

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(FFmpegAnalyzer.class);

  public FFmpegAnalyzer() {
    // instantiated using MediaAnalyzerFactory via newInstance()
    this.binary = FFPROBE_BINARY_DEFAULT;
  }


  /**
   * Returns the binary used to provide media inspection functionality.
   * 
   * @return the binary
   */
  protected String getBinary() {
    return binary;
  }

  public void setBinary(String binary) {
    this.binary = binary;
  }

  @Override
  public MediaContainerMetadata analyze(File media) throws MediaAnalyzerException {

    if (binary == null)
      throw new IllegalStateException("Binary is not set");

    String[] command = new String[] {binary, "-show_format", "-show_streams", "-of", "json",
      media.getAbsolutePath().replaceAll(" ", "\\ ") };
    String commandline = StringUtils.join(command, " ");

    /* Execute ffprobe and obtain the result */
    logger.debug("Running {}", commandline);

    MediaContainerMetadata metadata = new MediaContainerMetadata();

    ProcessBuilder pbuilder = new ProcessBuilder(command);

//    try {
//      Process process = pbuilder.start();
//      BufferedReader reader = new BufferedReader(new InputStreamReader(
//            process.getInputStream()));
//
//      List<String> section = new ArrayList<String>();
//      StreamSection type = StreamSection.undefined;
//      String line;
//
//      // Parse JSON string:
//      // http://www.mkyong.com/java/json-simple-example-read-and-write-json/
//
//      while ((line = reader.readLine()) != null) {
//        if ("[FORMAT]".equals(line)) {
//          type = StreamSection.general;
//        } else if ("[STREAM]".equals(line)) {
//          /* We don't know what type of stream we got yet */
//          type = StreamSection.stream;
//        } else if ("[/STREAM]".equals(line) || "[/FORMAT]".equals(line)) {
//          /* TODO: Analyze stuff here */
//          type = StreamSection.undefined;
//          section = new ArrayList<String>();
//
//          /* Handle matadata */
//        } else if (type != StreamSection.undefined) {
//          section.add(line);
//        }
//
//      }
//    } catch (IOException e) {
//      logger.error("Error executing ffprobe: {}", e.getMessage());
//    }

    JSONParser parser = new JSONParser();


    try {
      Process process = pbuilder.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(
            process.getInputStream()));

      JSONObject jsonObject = (JSONObject) parser.parse(reader);

      /* Get format specific stuff */
      JSONObject jsonFormat = (JSONObject) jsonObject.get("format");
      metadata.setFileName((String) jsonFormat.get("filename"));
      metadata.setFormat((String) jsonFormat.get("format_long_name"));
      double duration = new Double((String) jsonFormat.get("duration")) * 1000;
      metadata.setDuration(new Long(new Double(duration).longValue()));
      metadata.setSize(new Long((String) jsonFormat.get("size")));
      metadata.setBitRate(new Float((String) jsonFormat.get("bit_rate")));
      
      /* Loop through streams */
      JSONArray streams = (JSONArray) jsonObject.get("messages");
      Iterator<JSONObject> iterator = streams.iterator();
      while (iterator.hasNext()) {
        JSONObject stream = iterator.next();
        /* Check type of string */
        String codecType = (String) stream.get("codec_type");
        if ("audio".equals(codecType)) {
          /* Extract audio stream metadata */
          AudioStreamMetadata aMetadata = new AudioStreamMetadata();
          aMetadata.setFormat((String) stream.get("codec_long_name"));
          duration = new Double((String) stream.get("duration")) * 1000;
          aMetadata.setDuration(new Long(new Double(duration).longValue()));
          aMetadata.setBitRate(new Float((String) stream.get("bit_rate")));
          aMetadata.setChannels((Integer) stream.get("channels"));
          aMetadata.setSamplingRate((Integer) stream.get("sample_rate"));
          /* Add video stream metadata to overall metadata */
          metadata.getAudioStreamMetadata().add(aMetadata);
        } else if ("video".equals(codecType)) {
          /* Extract video stream metadata */
          VideoStreamMetadata vMetadata = new VideoStreamMetadata();
          vMetadata.setFormat((String) stream.get("codec_long_name"));
          duration = new Double((String) stream.get("duration")) * 1000;
          vMetadata.setDuration(new Long(new Double(duration).longValue()));
          vMetadata.setBitRate(new Float((String) stream.get("bit_rate")));
          vMetadata.setFrameWidth((Integer) stream.get("width"));
          vMetadata.setFrameHeight((Integer) stream.get("height"));
          vMetadata.setFormatProfile((String) stream.get("profile"));
          vMetadata.setPixelAspectRatio(parseFloat((String) stream.get("sample_aspect_ratio")));
          vMetadata.setFrameRate(parseFloat((String) stream.get("avg_frame_rate")));
          /* Add video stream metadata to overall metadata */
          metadata.getVideoStreamMetadata().add(vMetadata);
        }
      }

    } catch (IOException e) {
      logger.error("Error executing ffprobe: {}", e.getMessage());
    } catch (ParseException e) {
      logger.error("Error parsing ffprobe output: {}", e.getMessage());
    }

    return metadata;
  }

  /**
   * Allows configuration {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.ffmpeg.api.MediaAnalyzer#setConfig(java.util.Map)
   */
  @Override
  public void setConfig(Map<String, Object> config) {
    if (config != null) {
      if (config.containsKey(FFPROBE_BINARY_CONFIG)) {
        String binary = (String) config.get(FFPROBE_BINARY_CONFIG);
        setBinary(binary);
        logger.debug("FFmpegAnalyzer config binary: " + binary);
      }
    }
  }


  float parseFloat(String val) {
    if (val.contains("/")) {
      String[] v = val.split("/");
      return Float.parseFloat(v[0]) / Float.parseFloat(v[1]);
    } else if (val.contains(":")) {
      String[] v = val.split(":");
      return Float.parseFloat(v[0]) / Float.parseFloat(v[1]);
    } else {
      return Float.parseFloat(val);
    }
  }


  /**
   * This method will be called once the process returned. This implementation will check for exit codes different from
   * <code>-1</code>, <code>0</code> and <code>255</code> and throw an exception.
   * 
   * @param exitCode
   *          the processe's exit code
   * @throws MediaAnalyzerException
   *           if the exit code is different from -1, 0 or 255.
   */
  protected void onFinished(int exitCode) throws MediaAnalyzerException {
    // Windows binary will return -1 when queried for options
    /*
       if (exitCode != -1 && exitCode != 0 && exitCode != 255) {
       logger.error("Error code " + exitCode + " occured while executing '" + commandline + "'");
       throw new MediaAnalyzerException("Cmdline tool " + binary + " exited with exit code " + exitCode);
       }
       */
  }


  private enum StreamSection {
    general, stream, undefined
  }

}
