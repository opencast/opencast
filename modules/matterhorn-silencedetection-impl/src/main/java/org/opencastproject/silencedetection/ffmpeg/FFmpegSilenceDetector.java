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
package org.opencastproject.silencedetection.ffmpeg;

import com.google.common.io.LineReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.silencedetection.api.MediaSegment;
import org.opencastproject.silencedetection.api.MediaSegments;
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException;
import org.opencastproject.silencedetection.impl.SilenceDetectionProperties;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find silent sequences in audio stream using Gstreamer.
 */
public class FFmpegSilenceDetector {

  private static final Logger logger = LoggerFactory.getLogger(FFmpegSilenceDetector.class);

  public static final String FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path";
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  private static final String DEFAULT_SILENCE_MIN_LENGTH = "5000";
  private static final String DEFAULT_SILENCE_PRE_LENGTH = "2000";
  private static final String DEFAULT_THRESHOLD_DB = "-40dB";
  private static final String DEFAULT_VOICE_MIN_LENGTH = "60000";

  private String filePath;
  private String trackId;

  private List<MediaSegment> segments = null;

  /**
   * Create nonsilent sequences detection pipeline.
   * Parse audio stream and store all positions, where the volume level fall under the threshold.
   *
   * @param properties
   * @param track source track
   */
  public FFmpegSilenceDetector(Properties properties, Track track, Workspace workspace)
    throws SilenceDetectionFailedException, MediaPackageException, IOException {

    long minSilenceLength = Long.parseLong(properties.getProperty(SilenceDetectionProperties.SILENCE_MIN_LENGTH,
          DEFAULT_SILENCE_MIN_LENGTH));
    long minVoiceLength = Long.parseLong(properties.getProperty(SilenceDetectionProperties.VOICE_MIN_LENGTH,
          DEFAULT_VOICE_MIN_LENGTH));
    long preSilenceLength = Long.parseLong(properties.getProperty(SilenceDetectionProperties.SILENCE_PRE_LENGTH,
          DEFAULT_SILENCE_PRE_LENGTH));
    String thresholdDB = properties.getProperty(SilenceDetectionProperties.SILENCE_THRESHOLD_DB, DEFAULT_THRESHOLD_DB);

    String binary = properties.getProperty(FFMPEG_BINARY_CONFIG, FFMPEG_BINARY_DEFAULT);

    trackId = track.getIdentifier();

    /* Make sure the element can be analyzed using this analysis implementation */
    if (!track.hasAudio()) {
      logger.warn("Track {} has no audio stream to run a silece detection on", trackId);
      throw new SilenceDetectionFailedException("Element has no audio stream");
    }

    /* Make sure we are not allowed to move the beginning of a segment into the last segment */
    if (preSilenceLength > minSilenceLength) {
      logger.error("Pre silence length ({}) is configured to be greater than minimun silence length ({})",
          preSilenceLength, minSilenceLength);
      throw new SilenceDetectionFailedException("preSilenceLength > minSilenceLength");
    }

    try {
      File mediaFile = workspace.get(track.getURI());
      filePath = mediaFile.getAbsolutePath();
    } catch (NotFoundException e) {
      throw new SilenceDetectionFailedException("Error finding the media file in workspace", e);
    } catch (IOException e) {
      throw new SilenceDetectionFailedException("Error reading media file in workspace", e);
    }

    if (track.getDuration() == null) {
      throw new MediaPackageException("Track " + trackId + " does not have a duration");
    }
    logger.info("Track {} loaded, duration is {} s", filePath, track.getDuration() / 1000);

    logger.info("Starting silence detection of {}", filePath);
    String mediaPath = filePath.replaceAll(" ", "\\ ");
    DecimalFormat decimalFmt = new DecimalFormat("0.000", new DecimalFormatSymbols(Locale.US));
    String minSilenceLengthInSeconds = decimalFmt.format((double) minSilenceLength / 1000.0);
    String filter = "silencedetect=noise=" + thresholdDB + ":duration=" + minSilenceLengthInSeconds;
    String[] command = new String[] {binary, "-nostats", "-i", mediaPath, "-filter:a", filter, "-f", "null", "-"};
    String commandline = StringUtils.join(command, " ");

    logger.info("Running {}", commandline);

    ProcessBuilder pbuilder = new ProcessBuilder(command);
    List<String> segmentsStrings = new LinkedList<String>();
    Process process = pbuilder.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    try {
      LineReader lr = new LineReader(reader);
      String line = lr.readLine();
      while (null != line) {
        /* We want only lines from the silence detection filter */
        logger.debug("FFmpeg output: {}", line);
        if (line.startsWith("[silencedetect ")) {
          segmentsStrings.add(line);
        }
        line = lr.readLine();
      }
    } catch (IOException e) {
      logger.error("Error executing ffmpeg: {}", e.getMessage());
    } finally {
      reader.close();
    }

    /**
     * Example output:
     * [silencedetect @ 0x2968e40] silence_start: 466.486
     * [silencedetect @ 0x2968e40] silence_end: 469.322 | silence_duration: 2.83592
     */

    LinkedList<MediaSegment> segmentsTmp = new LinkedList<MediaSegment>();
    if (segmentsStrings.size() == 0) {
      /* No silence found -> Add one segment for the whole track */
      logger.info("No silence found. Adding one large segment.");
      segmentsTmp.add(new MediaSegment(0, track.getDuration()));
    } else {
      long lastSilenceEnd = 0;
      long lastSilenceStart = 0;
      Pattern patternStart = Pattern.compile("silence_start\\:\\ \\d+\\.\\d+");
      Pattern patternEnd = Pattern.compile("silence_end\\:\\ \\d+\\.\\d+");
      for (String seginfo : segmentsStrings) {
        /* Match silence ends */
        Matcher matcher = patternEnd.matcher(seginfo);
        String time = "";
        while (matcher.find()) {
          time = matcher.group().substring(13);
        }
        if (!"".equals(time)) {
          long silenceEnd = (long) (Double.parseDouble(time) * 1000);
          if (silenceEnd > lastSilenceEnd) {
            logger.debug("Found silence end at {}", silenceEnd);
            lastSilenceEnd = silenceEnd;
          }
          continue;
        }

        /* Match silence start -> End of segments */
        matcher = patternStart.matcher(seginfo);
        time = "";
        while (matcher.find()) {
          time = matcher.group().substring(15);
        }
        if (!"".equals(time)) {
          lastSilenceStart = (long) (Double.parseDouble(time) * 1000);
          logger.debug("Found silence start at {}", lastSilenceStart);
          if (lastSilenceStart - lastSilenceEnd > minVoiceLength) {
            /* Found a valid segment */
            long segmentStart = java.lang.Math.max(0, lastSilenceEnd - preSilenceLength);
            logger.info("Adding segment from {} to {}", segmentStart, lastSilenceStart);
            segmentsTmp.add(new MediaSegment(segmentStart, lastSilenceStart));
          }
        }
      }
      /* Add last segment if it is no silence and the segment is long enough */
      if (lastSilenceStart < lastSilenceEnd && track.getDuration() - lastSilenceEnd > minVoiceLength) {
        long segmentStart = java.lang.Math.max(0, lastSilenceEnd - preSilenceLength);
        logger.info("Adding final segment from {} to {}", segmentStart, track.getDuration());
        segmentsTmp.add(new MediaSegment(segmentStart, track.getDuration()));
      }
    }

    logger.info("Segmentation of track {} yielded {} segments", trackId, segmentsTmp.size());
    segments = segmentsTmp;

  }


  /**
   * Returns found media segments.
   * @return nonsilent media segments
   */
  public MediaSegments getMediaSegments() {
    if (segments == null)
      return null;

    return new MediaSegments(trackId, filePath, segments);
  }
}
