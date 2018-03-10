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


package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.activation.MimetypesFileTypeMap;

/**
 * Abstract base class for encoder engines.
 */
public class EncoderEngine implements AutoCloseable {

  /** The ffmpeg commandline suffix */
  static final String CMD_SUFFIX = "ffmpeg.command";
  /** The trimming start time property name */
  static final String PROP_TRIMMING_START_TIME = "trim.start";
  /** The trimming duration property name */
  static final String PROP_TRIMMING_DURATION = "trim.duration";
  /** If true STDERR and STDOUT of the spawned process will be mixed so that both can be read via STDIN */
  private static final boolean REDIRECT_ERROR_STREAM = true;

  /** the logging facility provided by log4j */
  private static Logger logger = LoggerFactory.getLogger(EncoderEngine.class.getName());
  /** the encoder binary */
  private String binary = "ffmpeg";
  /** Set of processes to clean up */
  private Set<Process> processes = new HashSet<>();

  private final Pattern outputPattern = Pattern.compile("Output .* to '(.*)':");

  /**
   * Creates a new abstract encoder engine with or without support for multiple job submission.
   */
  EncoderEngine(String binary) {
    this.binary = binary;
  }

  /**
   * {@inheritDoc}
   *
   * @see EncoderEngine#encode(File,
   *      EncodingProfile, Map)
   */
  File encode(File mediaSource, EncodingProfile format, Map<String, String> properties)
          throws EncoderException {
    List<File> output = process(Collections.map(Tuple.tuple("video", mediaSource)), format, properties);
    if (output.size() != 1) {
      throw new EncoderException(String.format("Encode expects one output file (%s found)", output.size()));
    }
    return output.get(0);
  }

  /**
   * Extract several images from a video file.
   *
   * @param mediaSource
   *          File to extract images from
   * @param format
   *          Encoding profile to use for extraction
   * @param properties
   * @param times
   *          Times at which to extract the images
   * @return  List of image files
   * @throws EncoderException Something went wrong during image extraction
   */
  List<File> extract(File mediaSource, EncodingProfile format, Map<String, String> properties, double... times)
          throws EncoderException {

    List<File> extractedImages = new LinkedList<>();
    try {
      // Extract one image if no times are specified
      if (times.length == 0) {
        extractedImages.add(encode(mediaSource, format, properties));
      }
      for (double time : times) {
        Map<String, String> params = new HashMap<>();
        if (properties != null) {
          params.putAll(properties);
        }

        DecimalFormatSymbols ffmpegFormat = new DecimalFormatSymbols();
        ffmpegFormat.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0.00000", ffmpegFormat);
        params.put("time", df.format(time));

        extractedImages.add(encode(mediaSource, format, params));
      }
    } catch (Exception e) {
      cleanup(extractedImages);
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException("Image extraction failed", e);
      }
    }

    return extractedImages;
  }

  /**
   * Executes the command line encoder with the given set of files and properties and using the provided encoding
   * profile.
   *
   * @param source
   *          the source files for encoding
   * @param profile
   *          the profile identifier
   * @param properties
   *          the encoding properties to be interpreted by the actual encoder implementation
   * @return the processed file
   * @throws EncoderException
   *           if processing fails
   */
  List<File> process(Map<String, File> source, EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {
    // Fist, update the parameters
    Map<String, String> params = new HashMap<>();
    if (properties != null)
      params.putAll(properties);
    // build command
    if (source.isEmpty()) {
      throw new IllegalArgumentException("At least one track must be specified.");
    }
    // Set encoding parameters
    for (Map.Entry<String, File> f: source.entrySet()) {
      final String input = FilenameUtils.normalize(f.getValue().getAbsolutePath());
      final String pre = "in." + f.getKey();
      params.put(pre + ".path", input);
      params.put(pre + ".name", FilenameUtils.getBaseName(input));
      params.put(pre + ".suffix", FilenameUtils.getExtension(input));
      params.put(pre + ".filename", FilenameUtils.getName(input));
      params.put(pre + ".mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(input));
    }
    final File parentFile = source.getOrDefault("video", source.get("audio"));

    final String outDir = parentFile.getAbsoluteFile().getParent();
    final String outFileName = FilenameUtils.getBaseName(parentFile.getName())
            + "_" + UUID.randomUUID().toString();
    params.put("out.dir", outDir);
    params.put("out.name", outFileName);
    if (profile.getSuffix() != null) {
      final String outSuffix = processParameters(profile.getSuffix(), params);
      params.put("out.suffix", outSuffix);
    }

    for (String tag : profile.getTags()) {
      final String suffix = processParameters(profile.getSuffix(tag), params);
      params.put("out.suffix." + tag, suffix);
    }

    // create encoder process.
    final List<String> command = buildCommand(profile, params);
    logger.info("Executing encoding command: {}", command);

    List<File> outFiles = new ArrayList<>();
    BufferedReader in = null;
    Process encoderProcess = null;
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(REDIRECT_ERROR_STREAM);
      encoderProcess = processBuilder.start();
      processes.add(encoderProcess);

      // tell encoder listeners about output
      in = new BufferedReader(new InputStreamReader(encoderProcess.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        handleEncoderOutput(outFiles, line);
      }

      // wait until the task is finished
      int exitCode = encoderProcess.waitFor();
      if (exitCode != 0) {
        throw new EncoderException("Encoder exited abnormally with status " + exitCode);
      }

      logger.info("Tracks {} successfully encoded using profile '{}'", source, profile.getIdentifier());
      return outFiles;
    } catch (Exception e) {
      logger.warn("Error while encoding {}  using profile '{}'",
              source, profile.getIdentifier(), e);

      // Ensure temporary data are removed
      for (File outFile : outFiles) {
        if (FileUtils.deleteQuietly(outFile)) {
          logger.debug("Removed output file of failed encoding process: {}", outFile);
        }
      }
      throw new EncoderException(e);
    } finally {
      IoSupport.closeQuietly(in);
      IoSupport.closeQuietly(encoderProcess);
    }
  }


  /**
   * #DCE OPC-29 : Encode the videoSource with an encoding profile that produces multiple outputs Care is taken that the
   * files are returned in the order listed in the profile
   *
   * @param videoSource
   *          - source recording file
   * @param profile
   *          - encoding profile
   * @param properties
   *          - for the ffmpeg command
   * @return demuxed files
   * @throws EncoderException
   *           - Fails to encode
   */
  public List<File> demux(File videoSource, EncodingProfile profile) throws EncoderException {
    List<File> inputs = new ArrayList<>();
    Map<String, String> params = new HashMap<>();
    // build command
    if (videoSource == null) {
      throw new IllegalArgumentException("sourcetrack must be specified.");
    }

    // Set encoding parameters
    final String videoInput = FilenameUtils.normalize(videoSource.getAbsolutePath());
    params.put("in.video.path", videoInput);
    params.put("in.video.name", FilenameUtils.getBaseName(videoInput));
    params.put("in.video.suffix", FilenameUtils.getExtension(videoInput));
    params.put("in.video.filename", FilenameUtils.getName(videoInput));
    params.put("in.video.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(videoInput));
    inputs.add(videoSource.getAbsoluteFile());
    final String outDir = videoSource.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(videoSource.getName()) + "_" + UUID.randomUUID().toString();
    final String outSuffix = processParameters(profile.getSuffix(), params);

    params.put("out.dir", outDir);
    params.put("out.name", outFileName);
    params.put("out.suffix", outSuffix);

    // create encoder process.
    final List<String> command = buildCommand(profile, params);
    List<String> outputFiles = new ArrayList<>();
    // Look for output name in command
    boolean skip = false;
    for (String argument : command) {
      if (skip) { // input file may use the same outDir
        skip = false;
        continue;
      }
      skip = "-i".equals(argument);
      // Use 'or' in case one of the two wildcards is not used, outname is more precise
      if (argument.contains(outFileName) || argument.contains(outDir)) { // is probably output name
        outputFiles.add(argument); // in the order listed in the command
      }
    }
    List<EncodingProfile> profiles = java.util.Collections.singletonList(profile);
    return process(command, inputs, outputFiles, profiles);
  }

  /**
   * #DCE OPC-29- Runs the raw command string through the encoder. The string commandopts is ffmpeg specific, it just
   * needs the binary. The calling function is responsible in doing all the appropriate substitutions using the encoding
   * profiles, creating the directory for storage, etc Encoding profiles and output names are included here for output
   * listeners and returns
   *
   * @param command ffmpeg command to run
   *
   * @param inputs input files in the command, used for reporting
   *
   * @param outputs output file name for reporting
   *
   * @param profiles encoding profiles
   *
   * @return encoded media as a result of running the command
   *
   * @throws EncoderException if it fails
   */
  private List<File> process(List<String> command, List<File> inputs, List<String> outputs,
          List<EncodingProfile> profiles) throws EncoderException {
    // create encoder process. using working dir of the
    // current java process
    Process encoderProcess = null;
    BufferedReader in = null;
    EncodingProfile profile = profiles.get(0);
    File videoSource = null;
    try { // May not be empty
      videoSource = inputs.get(0);
    } catch (Exception e) {
      logger.info("No inputs, ouputs {}", profiles);
    }

    if (videoSource == null) {
      throw new IllegalArgumentException("At least one track must be specified.");
    }
    try {
      logger.info("Executing encoding command: {}", command);

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(REDIRECT_ERROR_STREAM);
      encoderProcess = processBuilder.start();
      // tell encoder listeners about output
      in = new BufferedReader(new InputStreamReader(encoderProcess.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        handleEncoderOutput(line);
      }
      // wait until the task is finished
      encoderProcess.waitFor();
      int exitCode = encoderProcess.exitValue();
      if (exitCode != 0) {
        throw new EncoderException("Encoder exited abnormally with status " + exitCode);
      }
      StringBuilder sb = new StringBuilder(); // report on input
      for (File videoInput : inputs) {
        sb.append(videoInput.getName());
        sb.append(",");
      }
      StringBuilder sbp = new StringBuilder(); // profile
      for (EncodingProfile p : profiles) {
        sbp.append(p.getIdentifier());
        sbp.append(",");
      }
      logger.info("Video track successfully encoded '{}'", sb.toString(), sbp.toString(),
              StringUtils.join(outputs, ","));
      // return output as a list of files
      return outputs.stream().map(File::new).collect(Collectors.toList());
    } catch (EncoderException e) {
      StringBuilder sb = new StringBuilder();
      for (File videoInput : inputs) {
        sb.append(videoInput.getName());
        sb.append(",");
      }
      logger.warn("Error while encoding video track {} using '{}'", sb, profile.getIdentifier(), e);

      throw e;
    } catch (Exception e) {
      logger.warn("Error while encoding track {} to {}", videoSource.getName(), profile.getName(), e);
      throw new EncoderException(e.getMessage(), e);
    } finally {
      IoSupport.closeQuietly(in);
      IoSupport.closeQuietly(encoderProcess);
    }
  }

  /**
   * Deletes all valid files found in a list
   *
   * @param outputFiles
   *          list containing files
   */
  private void cleanup(List<File> outputFiles) {
    for (File file : outputFiles) {
      if (file != null && file.isFile()) {
        String path = file.getAbsolutePath();
        if (file.delete()) {
          logger.info("Deleted file {}", path);
        } else {
          logger.warn("Could not delete file {}", path);
        }
      }
    }
  }

  /**
   * Creates the command that is sent to the commandline encoder.
   *
   * @return the commandline
   * @throws EncoderException
   *           in case of any error
   */
  private List<String> buildCommand(final EncodingProfile profile, final Map<String, String> argumentReplacements)
          throws EncoderException {
    List<String> command = new ArrayList<>();
    command.add(binary);
    command.add("-nostats");

    String commandline = profile.getExtension(CMD_SUFFIX);

    // Handle command line extensions before parsing:
    // Example:
    //   ffmpeg.command = #{concatCmd} -c copy out.mp4
    //   ffmpeg.command.concatCmd = -i ...
    for (String key: argumentReplacements.keySet()) {
      if (key.startsWith(CMD_SUFFIX + '.')) {
        final String shortKey = key.substring(CMD_SUFFIX.length() + 1);
        commandline = commandline.replace("#{" + shortKey + "}", argumentReplacements.get(key));
      }
    }

    String[] arguments;
    try {
      arguments = CommandLineUtils.translateCommandline(commandline);
    } catch (Exception e) {
      throw new EncoderException("Could not parse encoding profile command line", e);
    }

    for (String arg: arguments) {
      String result = processParameters(arg, argumentReplacements);
      if (StringUtils.isNotBlank(result)) {
        command.add(result);
      }
    }
    return command;
  }

  /**
   * {@inheritDoc}
   *
   * @see EncoderEngine#trim(File,
   *      EncodingProfile, long, long, Map)
   */
  File trim(File mediaSource, EncodingProfile format, long start, long duration, Map<String, String> properties) throws EncoderException {
    if (properties == null)
      properties = new HashMap<>();
    double startD = (double) start / 1000;
    double durationD = (double) duration / 1000;
    DecimalFormatSymbols ffmpegFormat = new DecimalFormatSymbols();
    ffmpegFormat.setDecimalSeparator('.');
    DecimalFormat df = new DecimalFormat("00.00000", ffmpegFormat);
    properties.put(PROP_TRIMMING_START_TIME, df.format(startD));
    properties.put(PROP_TRIMMING_DURATION, df.format(durationD));
    return encode(mediaSource, format, properties);
  }

  /**
   * Processes the command options by replacing the templates with their actual values.
   *
   * @return the commandline
   */
  private String processParameters(String cmd, final Map<String, String> args) {
    for (Map.Entry<String, String> e: args.entrySet()) {
      cmd = cmd.replace("#{" + e.getKey() + "}", e.getValue());
    }

    // Also replace spaces
    cmd = cmd.replace("#{space}", " ");

    /* Remove unused commandline parts */
    return cmd.replaceAll("#\\{.*?\\}", "");
  }

  @Override
  public void close() {
    for (Process process: processes) {
      if (process.isAlive()) {
        logger.debug("Destroying encoding process {}", process);
        process.destroy();
      }
    }
  }

  /**
   * Handles the encoder output by analyzing it first and then firing it off to the registered listeners.
   *
   * @param message
   *          the message returned by the encoder
   */
  private void handleEncoderOutput(List<File> output, String message) {
    message = message.trim();
    if ("".equals(message))
      return;

    // Others go to trace logging
    if (StringUtils.startsWithAny(message.toLowerCase(),
          "ffmpeg version", "configuration", "lib", "size=", "frame=", "built with")) {
      logger.trace(message);

    // Handle output files
    } else if (StringUtils.startsWith(message, "Output #")) {
      logger.debug(message);
      Matcher matcher = outputPattern.matcher(message);
      if (matcher.find()) {
        File outputFile = new File(matcher.group(1));
        logger.info("Identified output file {}", outputFile);
        output.add(outputFile);
      }

    // Some to debug
    } else if (StringUtils.startsWithAny(message.toLowerCase(),
          "artist", "compatible_brands", "copyright", "creation_time", "description", "duration",
            "encoder", "handler_name", "input #", "last message repeated", "major_brand", "metadata", "minor_version",
            "output #", "program", "side data:", "stream #", "stream mapping", "title", "video:", "[libx264 @ ")) {
      logger.debug(message);

    // And the rest is likely to deserve at least info
    } else {
      logger.info(message);
    }
  }

}
