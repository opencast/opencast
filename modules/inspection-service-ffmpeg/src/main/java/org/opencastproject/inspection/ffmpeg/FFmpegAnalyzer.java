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
package org.opencastproject.inspection.ffmpeg;

import org.opencastproject.inspection.ffmpeg.api.AudioStreamMetadata;
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzer;
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzerException;
import org.opencastproject.inspection.ffmpeg.api.MediaContainerMetadata;
import org.opencastproject.inspection.ffmpeg.api.VideoStreamMetadata;
import org.opencastproject.util.ProcessRunner;
import org.opencastproject.util.ProcessRunner.ProcessInfo;

import com.entwinemedia.fn.Pred;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This MediaAnalyzer implementation uses the ffprobe binary of FFmpeg for media analysis. Also this implementation does
 * not keep control-, text- or other non-audio or video streams and purposefully ignores them during the
 * <code>postProcess()</code> step.
 */
public class FFmpegAnalyzer implements MediaAnalyzer {

  /** Path to the executable */
  protected String binary;

  public static final String FFPROBE_BINARY_CONFIG = "org.opencastproject.inspection.ffprobe.path";
  public static final String FFPROBE_BINARY_DEFAULT = "ffprobe";

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(FFmpegAnalyzer.class);

  /** Whether the calculation of the frames is accurate or not */
  private boolean accurateFrameCount;

  public FFmpegAnalyzer(boolean accurateFrameCount) {
    this.accurateFrameCount = accurateFrameCount;
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

    List<String> command = new ArrayList<>();
    command.add("-show_format");
    command.add("-show_streams");
    if (accurateFrameCount)
      command.add("-count_frames");
    command.add("-of");
    command.add("json");
    command.add(media.getAbsolutePath().replaceAll(" ", "\\ "));

    String commandline = StringUtils.join(command, " ");

    /* Execute ffprobe and obtain the result */
    logger.debug("Running {} {}", binary, commandline);

    MediaContainerMetadata metadata = new MediaContainerMetadata();

    final StringBuilder sb = new StringBuilder();
    try {
      ProcessInfo info = ProcessRunner.mk(binary, command.toArray(new String[command.size()]));
      int exitCode = ProcessRunner.run(info, new Pred<String>() {
        @Override
        public Boolean apply(String s) {
          logger.debug(s);
          sb.append(s);
          sb.append(System.getProperty("line.separator"));
          return true;
        }
      }, fnLogError);
      // Windows binary will return -1 when queried for options
      if (exitCode != -1 && exitCode != 0 && exitCode != 255)
        throw new MediaAnalyzerException("Frame analyzer " + binary + " exited with code " + exitCode);
    } catch (IOException e) {
      logger.error("Error executing ffprobe", e);
      throw new MediaAnalyzerException("Error while running ffprobe " + binary, e);
    }

    JSONParser parser = new JSONParser();

    try {
      JSONObject jsonObject = (JSONObject) parser.parse(sb.toString());
      Object obj;
      Double duration;

      /* Get format specific stuff */
      JSONObject jsonFormat = (JSONObject) jsonObject.get("format");

      /* File Name */
      obj = jsonFormat.get("filename");
      if (obj != null) {
        metadata.setFileName((String) obj);
      }

      /* Format */
      obj = jsonFormat.get("format_long_name");
      if (obj != null) {
        metadata.setFormat((String) obj);
      }

      /*
       * Mediainfo does not return a duration if there is no stream but FFprobe will return 0. For compatibility
       * reasons, check if there are any streams before reading the duration:
       */
      obj = jsonFormat.get("nb_streams");
      if (obj != null && (Long) obj > 0) {
        obj = jsonFormat.get("duration");
        if (obj != null) {
          duration = new Double((String) obj) * 1000;
          metadata.setDuration(duration.longValue());
        }
      }

      /* File Size */
      obj = jsonFormat.get("size");
      if (obj != null) {
        metadata.setSize(new Long((String) obj));
      }

      /* Bitrate */
      obj = jsonFormat.get("bit_rate");
      if (obj != null) {
        metadata.setBitRate(new Float((String) obj));
      }

      /* Loop through streams */
      /*
       * FFprobe will return an empty stream array if there are no streams. Thus we do not need to check.
       */
      JSONArray streams = (JSONArray) jsonObject.get("streams");
      Iterator<JSONObject> iterator = streams.iterator();
      while (iterator.hasNext()) {
        JSONObject stream = iterator.next();
        /* Check type of string */
        String codecType = (String) stream.get("codec_type");

        /* Handle audio streams ----------------------------- */

        if ("audio".equals(codecType)) {
          /* Extract audio stream metadata */
          AudioStreamMetadata aMetadata = new AudioStreamMetadata();

          /* Codec */
          obj = stream.get("codec_long_name");
          if (obj != null) {
            aMetadata.setFormat((String) obj);
          }

          /* Duration */
          obj = stream.get("duration");
          if (obj != null) {
            duration = new Double((String) obj) * 1000;
            aMetadata.setDuration(duration.longValue());
          } else {
            /*
             * If no duration for this stream is specified assume the duration of the file for this as well.
             */
            aMetadata.setDuration(metadata.getDuration());
          }

          /* Bitrate */
          obj = stream.get("bit_rate");
          if (obj != null) {
            aMetadata.setBitRate(new Float((String) obj));
          }

          /* Channels */
          obj = stream.get("channels");
          if (obj != null) {
            aMetadata.setChannels(((Long) obj).intValue());
          }

          /* Sample Rate */
          obj = stream.get("sample_rate");
          if (obj != null) {
            aMetadata.setSamplingRate(Integer.parseInt((String) obj));
          }

          /* Frame Count */
          obj = stream.get("nb_read_frames");
          if (obj != null) {
            aMetadata.setFrames(Long.parseLong((String) obj));
          } else {

            /* alternate JSON element if accurate frame count is not requested from ffmpeg */
            obj = stream.get("nb_frames");
            if (obj != null) {
              aMetadata.setFrames(Long.parseLong((String) obj));
            }
          }

          /* Add video stream metadata to overall metadata */
          metadata.getAudioStreamMetadata().add(aMetadata);

          /* Handle video streams ----------------------------- */

        } else if ("video".equals(codecType)) {
          /* Extract video stream metadata */
          VideoStreamMetadata vMetadata = new VideoStreamMetadata();

          /* Codec */
          obj = stream.get("codec_long_name");
          if (obj != null) {
            vMetadata.setFormat((String) obj);
          }

          /* Duration */
          obj = stream.get("duration");
          if (obj != null) {
            duration = new Double((String) obj) * 1000;
            vMetadata.setDuration(duration.longValue());
          } else {
            /*
             * If no duration for this stream is specified assume the duration of the file for this as well.
             */
            vMetadata.setDuration(metadata.getDuration());
          }

          /* Bitrate */
          obj = stream.get("bit_rate");
          if (obj != null) {
            vMetadata.setBitRate(new Float((String) obj));
          }

          /* Width */
          obj = stream.get("width");
          if (obj != null) {
            vMetadata.setFrameWidth(((Long) obj).intValue());
          }

          /* Height */
          obj = stream.get("height");
          if (obj != null) {
            vMetadata.setFrameHeight(((Long) obj).intValue());
          }

          /* Profile */
          obj = stream.get("profile");
          if (obj != null) {
            vMetadata.setFormatProfile((String) obj);
          }

          /* Aspect Ratio */
          obj = stream.get("sample_aspect_ratio");
          if (obj != null) {
            vMetadata.setPixelAspectRatio(parseFloat((String) obj));
          }

          /* Frame Rate */
          obj = stream.get("avg_frame_rate");
          if (obj != null) {
            vMetadata.setFrameRate(parseFloat((String) obj));
          }

          /* Frame Count */
          obj = stream.get("nb_read_frames");
          if (obj != null) {
            vMetadata.setFrames(Long.parseLong((String) obj));
          } else {

            /* alternate JSON element if accurate frame count is not requested from ffmpeg */
            obj = stream.get("nb_frames");
            if (obj != null) {
              vMetadata.setFrames(Long.parseLong((String) obj));
            } else if (vMetadata.getDuration() != null && vMetadata.getFrameRate() != null) {
              long framesEstimation = Double.valueOf(vMetadata.getDuration() / 1000.0 * vMetadata.getFrameRate()).longValue();
              if (framesEstimation >= 1) {
                vMetadata.setFrames(framesEstimation);
              }
            }
          }

          /* Add video stream metadata to overall metadata */
          metadata.getVideoStreamMetadata().add(vMetadata);
        }
      }

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

  private float parseFloat(String val) {
    if (val.contains("/")) {
      String[] v = val.split("/");
      if (Float.parseFloat(v[1]) == 0) {
        return 0;
      } else {
        return Float.parseFloat(v[0]) / Float.parseFloat(v[1]);
      }
    } else if (val.contains(":")) {
      String[] v = val.split(":");
      if (Float.parseFloat(v[1]) == 0) {
        return 0;
      } else {
        return Float.parseFloat(v[0]) / Float.parseFloat(v[1]);
      }
    } else {
      return Float.parseFloat(val);
    }
  }

  private static final Pred<String> fnLogError = new Pred<String>() {
    @Override
    public Boolean apply(String s) {
      logger.debug(s);
      return true;
    }
  };

}
