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
package org.opencastproject.inspection.ffmpeg;

import org.opencastproject.inspection.ffmpeg.api.AudioStreamMetadata;
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzer;
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzerException;
import org.opencastproject.inspection.ffmpeg.api.MediaContainerMetadata;
import org.opencastproject.inspection.ffmpeg.api.SubtitleStreamMetadata;
import org.opencastproject.inspection.ffmpeg.api.VideoStreamMetadata;
import org.opencastproject.util.GsonUtil;
import org.opencastproject.util.IoSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
    if (binary == null) {
      throw new IllegalStateException("Binary is not set");
    }

    List<String> command = new ArrayList<>();
    command.add(binary);
    command.add("-show_format");
    command.add("-show_streams");
    if (accurateFrameCount) {
      command.add("-count_frames");
    }
    command.add("-of");
    command.add("json");
    command.add(media.getAbsolutePath().replaceAll(" ", "\\ "));

    /* Execute ffprobe and obtain the result */
    logger.debug("Running {} {}", binary, command);

    MediaContainerMetadata metadata = new MediaContainerMetadata();

    final StringBuilder sb = new StringBuilder();
    Process encoderProcess = null;
    try {
      encoderProcess = new ProcessBuilder(command)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();

      // tell encoder listeners about output
      try (var in = new BufferedReader(new InputStreamReader(encoderProcess.getInputStream()))) {
        String line;
        while ((line = in.readLine()) != null) {
          logger.debug(line);
          sb.append(line).append(System.getProperty("line.separator"));
        }
      }
      // wait until the task is finished
      int exitCode = encoderProcess.waitFor();
      if (exitCode != 0) {
        throw new MediaAnalyzerException("Frame analyzer " + binary + " exited with code " + exitCode);
      }
    } catch (IOException | InterruptedException e) {
      logger.error("Error executing ffprobe", e);
      throw new MediaAnalyzerException("Error while running " + binary, e);
    } finally {
      IoSupport.closeQuietly(encoderProcess);
    }

    try {
      JsonObject jsonObject = GsonUtil.gson().fromJson(sb.toString(), JsonObject.class);
      JsonElement obj;
      Double duration;

      /* Get format specific stuff */
      JsonObject jsonFormat = jsonObject.getAsJsonObject("format");

      /* File Name */
      obj = member(jsonFormat, "filename");
      if (obj != null) {
        metadata.setFileName(obj.getAsString());
      }

      /* Format */
      obj = member(jsonFormat, "format_long_name");
      if (obj != null) {
        metadata.setFormat(obj.getAsString());
      }

      /*
       * Mediainfo does not return a duration if there is no stream but FFprobe will return 0. For compatibility
       * reasons, check if there are any streams before reading the duration:
       */
      obj = member(jsonFormat, "nb_streams");
      if (obj != null && obj.getAsLong() > 0) {
        obj = member(jsonFormat, "duration");
        if (obj != null) {
          duration = Double.parseDouble(obj.getAsString()) * 1000;
          metadata.setDuration(duration.longValue());
        }
      }

      /* File Size */
      obj = member(jsonFormat, "size");
      if (obj != null) {
        metadata.setSize(Long.parseLong(obj.getAsString()));
      }

      /* Bitrate */
      obj = member(jsonFormat, "bit_rate");
      if (obj != null) {
        metadata.setBitRate(Float.parseFloat(obj.getAsString()));
      }

      /* Loop through streams */
      /*
       * FFprobe will return an empty stream array if there are no streams. Thus we do not need to check.
       */
      JsonArray streams = jsonObject.getAsJsonArray("streams");
      for (JsonElement streamElement : streams) {
        JsonObject stream = streamElement.getAsJsonObject();
        /* Check type of string */
        String codecType = GsonUtil.getStringOrNull(stream, "codec_type");

        /* Handle audio streams ----------------------------- */

        if ("audio".equals(codecType)) {
          /* Extract audio stream metadata */
          AudioStreamMetadata aMetadata = new AudioStreamMetadata();

          /* Codec */
          obj = member(stream, "codec_long_name");
          if (obj != null) {
            aMetadata.setFormat(obj.getAsString());
          }

          /* Duration */
          obj = member(stream, "duration");
          if (obj != null) {
            duration = new Double(obj.getAsString()) * 1000;
            aMetadata.setDuration(duration.longValue());
          } else {
            /*
             * If no duration for this stream is specified assume the duration of the file for this as well.
             */
            aMetadata.setDuration(metadata.getDuration());
          }

          /* Bitrate */
          obj = member(stream, "bit_rate");
          if (obj != null) {
            aMetadata.setBitRate(new Float(obj.getAsString()));
          }

          /* Channels */
          obj = member(stream, "channels");
          if (obj != null) {
            aMetadata.setChannels(obj.getAsInt());
          }

          /* Sample Rate */
          obj = member(stream, "sample_rate");
          if (obj != null) {
            aMetadata.setSamplingRate(Integer.parseInt(obj.getAsString()));
          }

          /* Frame Count */
          obj = member(stream, "nb_read_frames");
          if (obj != null) {
            aMetadata.setFrames(Long.parseLong(obj.getAsString()));
          } else {

            /* alternate JSON element if accurate frame count is not requested from ffmpeg */
            obj = member(stream, "nb_frames");
            if (obj != null) {
              aMetadata.setFrames(Long.parseLong(obj.getAsString()));
            }
          }

          /* Add video stream metadata to overall metadata */
          metadata.getAudioStreamMetadata().add(aMetadata);

          /* Handle video streams ----------------------------- */

        } else if ("video".equals(codecType)) {
          /* Extract video stream metadata */
          VideoStreamMetadata vMetadata = new VideoStreamMetadata();

          /* Codec */
          obj = member(stream, "codec_long_name");
          if (obj != null) {
            vMetadata.setFormat(obj.getAsString());
          }

          /* Duration */
          obj = member(stream, "duration");
          if (obj != null) {
            duration = new Double(obj.getAsString()) * 1000;
            vMetadata.setDuration(duration.longValue());
          } else {
            /*
             * If no duration for this stream is specified assume the duration of the file for this as well.
             */
            vMetadata.setDuration(metadata.getDuration());
          }

          /* Bitrate */
          obj = member(stream, "bit_rate");
          if (obj != null) {
            vMetadata.setBitRate(new Float(obj.getAsString()));
          }

          /* Width */
          obj = member(stream, "width");
          if (obj != null) {
            vMetadata.setFrameWidth(obj.getAsInt());
          }

          /* Height */
          obj = member(stream, "height");
          if (obj != null) {
            vMetadata.setFrameHeight(obj.getAsInt());
          }

          /* Profile */
          obj = member(stream, "profile");
          if (obj != null) {
            vMetadata.setFormatProfile(obj.getAsString());
          }

          /* Aspect Ratio */
          obj = member(stream, "sample_aspect_ratio");
          if (obj != null) {
            vMetadata.setPixelAspectRatio(parseFloat(obj.getAsString()));
          }

          /* Frame Rate */
          obj = member(stream, "avg_frame_rate");
          if (obj != null) {
            vMetadata.setFrameRate(parseFloat(obj.getAsString()));
          }

          /* Frame Count */
          obj = member(stream, "nb_read_frames");
          if (obj != null) {
            vMetadata.setFrames(Long.parseLong(obj.getAsString()));
          } else {

            /* alternate JSON element if accurate frame count is not requested from ffmpeg */
            obj = member(stream, "nb_frames");
            if (obj != null) {
              vMetadata.setFrames(Long.parseLong(obj.getAsString()));
            } else if (vMetadata.getDuration() != null && vMetadata.getFrameRate() != null) {
              long framesEstimation = Double.valueOf(vMetadata.getDuration() / 1000.0 * vMetadata.getFrameRate())
                  .longValue();
              if (framesEstimation >= 1) {
                vMetadata.setFrames(framesEstimation);
              }
            }
          }

          /* Add video stream metadata to overall metadata */
          metadata.getVideoStreamMetadata().add(vMetadata);

          /* Handle subtitle streams ----------------------------- */

        } else if ("subtitle".equals(codecType)) {
          /* Extract subtitle stream metadata */
          SubtitleStreamMetadata sMetadata = new SubtitleStreamMetadata();

          /* Codec */
          obj = member(stream, "codec_long_name");
          if (obj != null) {
            sMetadata.setFormat(obj.getAsString());
          }

          metadata.getSubtitleStreamMetadata().add(sMetadata);
        }
      }

    } catch (JsonParseException e) {
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

  /** Read a member, returning null when it is absent or JSON null, as json-simple's get() did. */
  private static JsonElement member(JsonObject json, String key) {
    JsonElement value = json.get(key);
    return value == null || value.isJsonNull() ? null : value;
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

}
