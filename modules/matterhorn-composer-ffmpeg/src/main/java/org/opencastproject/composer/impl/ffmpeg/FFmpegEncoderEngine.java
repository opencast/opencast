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

package org.opencastproject.composer.impl.ffmpeg;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.AbstractCmdlineEncoderEngine;
import org.opencastproject.util.data.Option;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for the encoder engine backed by ffmpeg.
 */
public class FFmpegEncoderEngine extends AbstractCmdlineEncoderEngine {

  /** Default location of the ffmepg binary (resembling the installer) */
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** The ffmpeg commandline suffix */
  public static final String CMD_SUFFIX = "ffmpeg.command";

  private static final String CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpegpath";

  /** Format for trim times */
  private static final String TIME_FORMAT = "%02d:%02d:";

  /** The trimming start time property name */
  public static final String PROP_TRIMMING_START_TIME = "trim.start";

  /** The trimming duration property name */
  public static final String PROP_TRIMMING_DURATION = "trim.duration";

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(FFmpegEncoderEngine.class);

  /**
   * Creates the ffmpeg encoder engine.
   */
  public FFmpegEncoderEngine() {
    super(FFMPEG_BINARY_DEFAULT);
  }

  public void activate(ComponentContext cc) {
    // Configure ffmpeg
    String path = (String) cc.getBundleContext().getProperty(CONFIG_FFMPEG_PATH);
    if (path == null) {
      logger.debug("DEFAULT " + CONFIG_FFMPEG_PATH + ": " + FFmpegEncoderEngine.FFMPEG_BINARY_DEFAULT);
    } else {
      setBinary(path);
      logger.debug("FFmpegEncoderEngine config binary: {}", path);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.impl.AbstractCmdlineEncoderEngine#trim(java.io.File,
   *      org.opencastproject.composer.api.EncodingProfile, long, long, java.util.Map)
   */
  @Override
  public Option<File> trim(File mediaSource, EncodingProfile format, long start, long duration,
          Map<String, String> properties) throws EncoderException {
    if (properties == null)
      properties = new HashMap<String, String>();
    double startD = (double) start / 1000;
    double durationD = (double) duration / 1000;
    DecimalFormatSymbols ffmpegFormat = new DecimalFormatSymbols();
    ffmpegFormat.setDecimalSeparator('.');
    DecimalFormat df = new DecimalFormat("00.000", ffmpegFormat);
    properties.put(
            PROP_TRIMMING_START_TIME,
            String.format(TIME_FORMAT, (long) Math.floor(startD / 3600), (long) (startD % 3600) / 60)
                    + df.format(startD % 60));
    properties.put(
            PROP_TRIMMING_DURATION,
            String.format(TIME_FORMAT, (long) Math.floor(durationD / 3600), (long) (durationD % 3600) / 60)
                    + df.format(durationD % 60));
    return super.trim(mediaSource, format, start, duration, properties);
  }

  /**
   * Creates the arguments for the commandline.
   * 
   * @param format
   *          the format
   * @return the argument list
   */
  @Override
  protected List<String> buildArgumentList(EncodingProfile format) throws EncoderException {
    String commandline = format.getExtension(CMD_SUFFIX);
    if (commandline == null)
      throw new EncoderException(this, "No commandline configured for " + format);

    // Process the commandline. The variables in that commandline might either
    // be replaced by commandline parts from the configuration or commandline
    // parameters as specified at runtime.
    List<String> argumentList = new ArrayList<String>();
    for (Map.Entry<String, String> entry : format.getExtensions().entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(CMD_SUFFIX) && key.length() > CMD_SUFFIX.length()) {
        String value = processParameters(entry.getValue());
        String partName = "#\\{" + key.substring(CMD_SUFFIX.length() + 1) + "\\}";
        if (!value.matches(".*#\\{.*\\}.*"))
          commandline = commandline.replaceAll(partName, value);
      }
    }

    // Replace the commandline parameters passed in at compile time
    commandline = processParameters(commandline);

    // Remove unused commandline parts
    commandline = commandline.replaceAll("#\\{.*?\\}", "");

    String[] args = commandline.split(" ");
    for (String a : args)
      if (!"".equals(a.trim()))
        argumentList.add(a);
    return argumentList;
  }

  /**
   * Handles the encoder output by analyzing it first and then firing it off to the registered listeners.
   * 
   * @param sourceFiles
   *          the source files that are currently being encoded
   * @param format
   *          the target media format
   * @param message
   *          the message returned by the encoder
   */
  @Override
  protected void handleEncoderOutput(EncodingProfile format, String message, File... sourceFiles) {
    super.handleEncoderOutput(format, message, sourceFiles);
    message = message.trim();
    if ("".equals(message))
      return;

    // Completely skip these messages
    if (message.startsWith("Press ["))
      return;

    // Others go to trace logging
    if (message.startsWith("FFmpeg version") || message.startsWith("configuration") || message.startsWith("lib")
            || message.startsWith("size=") || message.startsWith("frame=") || message.startsWith("built on"))

      logger.trace(message);

    // Some to debug
    else if (message.startsWith("Input #") || message.startsWith("Duration:") || message.startsWith("Stream #")
            || message.startsWith("Stream mapping") || message.startsWith("Output #") || message.startsWith("video:")
            || message.startsWith("Metadata") || message.startsWith("Program")
            || message.startsWith("Last message repeated")
            || message.startsWith("PIX_FMT_YUV420P will be used as an intermediate format for rescaling"))

      logger.debug(message);

    // And the rest is likely to deserve at least info
    else
      logger.info(message);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ffmpeg";
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.impl.AbstractEncoderEngine#getOutputFile(java.io.File,
   *      org.opencastproject.composer.api.EncodingProfile)
   */
  @Override
  protected File getOutputFile(File source, EncodingProfile profile) {
    File outputFile = null;
    try {
      List<String> arguments = buildArgumentList(profile);
      // TODO: Very unsafe! Improve!
      outputFile = new File(arguments.get(arguments.size() - 1));
    } catch (EncoderException e) {
      // Unlikely. We checked that before
    }
    return outputFile;
  }
}
