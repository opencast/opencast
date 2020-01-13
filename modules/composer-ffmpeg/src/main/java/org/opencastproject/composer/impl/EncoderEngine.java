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
import org.opencastproject.composer.api.VideoClip;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static Logger logger = LoggerFactory.getLogger(EncoderEngine.class);
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

  /*
   * Runs the raw command string thru the encoder. The string commandopts is ffmpeg specific, it just needs the binary.
   * The calling function is responsible in doing all the appropriate substitutions using the encoding profiles,
   * creating the directory for storage, etc. Encoding profiles and input names are included here for logging and
   * returns
   *
   * @param commandopts - tokenized ffmpeg command
   *
   * @param inputs - input files in the command, used for reporting
   *
   * @param profiles - encoding profiles, used for reporting
   *
   * @return encoded - media as a result of running the command
   *
   * @throws EncoderException if it fails
   */

  protected List<File> process(List<String> commandopts) throws EncoderException {
    logger.trace("Process raw command -  {}", commandopts);
    // create encoder process. using working dir of the
    // current java process
    Process encoderProcess = null;
    BufferedReader in = null;
    List<File> outFiles = new ArrayList<>();
    try {
      List<String> command = new ArrayList<>();
      command.add(binary);
      command.addAll(commandopts);
      logger.info("Executing encoding command: {}", StringUtils.join(command, " "));

      ProcessBuilder pbuilder = new ProcessBuilder(command);
      pbuilder.redirectErrorStream(REDIRECT_ERROR_STREAM);
      encoderProcess = pbuilder.start();
      // tell encoder listeners about output
      in = new BufferedReader(new InputStreamReader(encoderProcess.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        handleEncoderOutput(outFiles, line); // get names of output files
      }
      // wait until the task is finished
      encoderProcess.waitFor();
      int exitCode = encoderProcess.exitValue();
      if (exitCode != 0) {
        throw new EncoderException("Encoder exited abnormally with status " + exitCode);
      }
      logger.info("Video track successfully encoded '{}'",
              new Object[] { StringUtils.join(commandopts, " ") });
      return outFiles; // return output as a list of files
    } catch (Exception e) {
      logger.warn("Error while encoding video tracks using '{}': {}",
              new Object[] {  StringUtils.join(commandopts, " "), e.getMessage() });
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
    command.add("-nostdin");
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
        String outputPath = matcher.group(1);
        if (!StringUtils.equals("NUL", outputPath)
                && !StringUtils.equals("/dev/null", outputPath)
                && !StringUtils.startsWith("pipe:", outputPath)) {
          File outputFile = new File(outputPath);
          logger.info("Identified output file {}", outputFile);
          output.add(outputFile);
        }
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

  /**
   * Rewrite multiple profiles to ffmpeg complex filter filtergraph chains - inputs are passed in as options, eq: [0aa]
   * and [0vv] Any filters in the encoding profiles are moved into a clause in the complex filter chain for each output
   */
  protected class OutputAggregate {
    private final List<EncodingProfile> pf;
    private final ArrayList<String> outputs = new ArrayList<>();
    private final ArrayList<String> outputFiles = new ArrayList<>();
    private final ArrayList<String> vpads; // output pads for each segment
    private final ArrayList<String> apads;
    private final ArrayList<String> vfilter; // filters for each output format
    private final ArrayList<String> afilter;
    private String vInputPad = "";
    private String aInputPad = "";
    private String vsplit = "";
    private String asplit = "";

    /*
     * set the audio filter if there are any in the profiles or identity
     */
    private void setAudioFilters() {
      if (pf.size() == 1) {
        if (afilter.get(0) != null)
          afilter.set(0, aInputPad + afilter.get(0) + apads.get(0)); // Use audio filter on input directly
      } else
        for (int i = 0; i < pf.size(); i++) {
          if (afilter.get(i) != null) {
            afilter.set(i, "[oa0" + i + "]" + afilter.get(i) + apads.get(i)); // Use audio filter on apad
            asplit += "[oa0" + i + "]";
          } else {
            asplit += apads.get(i); // straight to output
          }
        }
      afilter.removeAll(Arrays.asList((String) null));
    }

    /*
     * set the video filter if there are any in the profiles
     */
    private void setVideoFilters() {
      if (pf.size() == 1) {
        if (vfilter.get(0) != null)
          vfilter.set(0, vInputPad + vfilter.get(0) + vpads.get(0)); // send to filter first
      } else
        for (int i = 0; i < pf.size(); i++) {
          if (vfilter.get(i) != null) {
            vfilter.set(i, "[ov0" + i + "]" + vfilter.get(i) + vpads.get(i)); // send to filter first
            vsplit += "[ov0" + i + "]";
          } else {
            vsplit += vpads.get(i);// straight to output
          }
        }

      vfilter.removeAll(Arrays.asList((String) null));
    }

    public List<String> getOutFiles() {
      return outputFiles;
    }

    /**
     *
     * @return output pads - the "-map xyz" clauses
     */
    public List<String> getOutput() {
      return outputs;
    }

    /**
     *
     * @return filter split clause for ffmpeg
     */
    public String getVsplit() {
      return vsplit;
    }

    public String getAsplit() {
      return asplit;
    }

    public String getVideoFilter() {
      if (vfilter.isEmpty())
        return null;
      return StringUtils.join(vfilter, ";");
    }

    public String getAudioFilter() {
      if (afilter.isEmpty())
        return null;
      return StringUtils.join(afilter, ";");
    }

    /**
     * If this is a raw mapping not used with complex filter, strip the square brackets if there are any
     *
     * @param pad
     *          - such as 0:a, [0:v], [1:1],[0:12],[main],[overlay]
     * @return adjusted pad
     */
    public String adjustForNoComplexFilter(String pad) {
      final Pattern outpad = Pattern.compile("\\[(\\d+:[av\\d{1,2}])\\]");
      try {
        Matcher matcher = outpad.matcher(pad); // throws exception if pad is null
        if (matcher.matches()) {
          return matcher.group(1);
        }
      } catch (Exception e) {
      }
      return pad;
    }

    /**
     * Replace all the templates with real values for each profile
     *
     * @param cmd
     *          from profile
     * @param params
     *          from input
     * @return command
     */
    protected String processParameters(String cmd, Map<String, String> params) {
      String r = cmd;
      for (Map.Entry<String, String> e : params.entrySet()) {
        r = r.replace("#{" + e.getKey() + "}", e.getValue());
      }
      return r;
    }

    /**
     * Translate the profiles to work with complex filter clauses in ffmpeg, it splits one output into multiple, one for
     * each encoding profile
     *
     * @param profiles
     *          - list of encoding profiles
     * @param params
     *          - values for substitution
     * @param vInputPad
     *          - name of video pad as input, eg: [0v] null if no video
     * @param aInputPad
     *          - name of audio pad as input, eg [0a], null if no audio
     * @throws EncoderException
     *           - if it fails
     */
    public OutputAggregate(List<EncodingProfile> profiles, Map<String, String> params, String vInputPad,
            String aInputPad) throws EncoderException {
      pf = profiles;
      if (vInputPad == null && aInputPad == null)
        throw new EncoderException("At least one of video or audio input must be specified");
      IdBuilder idbuilder = IdBuilderFactory.newInstance().newIdBuilder();
      int size = profiles.size();
      // Init
      vfilter = new ArrayList<>(java.util.Collections.nCopies(size, null));
      afilter = new ArrayList<>(java.util.Collections.nCopies(size, null));
      // name of output pads to map to files
      apads = new ArrayList<>(java.util.Collections.nCopies(size, null));
      vpads = new ArrayList<>(java.util.Collections.nCopies(size, null));

      vsplit = (size > 1) ? (vInputPad + "split=" + size) : null; // number of splits
      asplit = (size > 1) ? (aInputPad + "asplit=" + size) : null;
      this.vInputPad = vInputPad;
      this.aInputPad = aInputPad;

      int indx = 0; // profiles
      for (EncodingProfile profile : profiles) {
        String cmd = "";
        String outSuffix;
        // generate random name as we only have one base name
        String outFileName = params.get("out.name.base") + "_" + idbuilder.createNew().toString();
        params.put("out.name", outFileName); // Output file name for this profile
        try {
          outSuffix = processParameters(profile.getSuffix(), params);
          params.put("out.suffix", outSuffix); // Add profile suffix
        } catch (Exception e) {
          throw new EncoderException("Missing Encoding Profiles");
        }
        // substitute the output file name
        String ffmpgCmd = profile.getExtension(CMD_SUFFIX); // Get ffmpeg command from profile
        if (ffmpgCmd == null)
          throw new EncoderException("Missing Encoding Profile " + profile.getIdentifier() + " ffmpeg command");
        for (Map.Entry<String, String> e : params.entrySet()) { // replace output filenames
          ffmpgCmd = ffmpgCmd.replace("#{" + e.getKey() + "}", e.getValue());
        }
        ffmpgCmd = ffmpgCmd.replace("#{space}", " ");
        String[] arguments;
        try {
          arguments = CommandLineUtils.translateCommandline(ffmpgCmd);
        } catch (Exception e) {
          throw new EncoderException("Could not parse encoding profile command line", e);
        }
        List<String> cmdToken = Arrays.asList(arguments);
        // Find and remove input and filters from ffmpeg command from the profile
        int i = 0;
        while (i < cmdToken.size()) {
          String opt = cmdToken.get(i);
          if (opt.startsWith("-vf") || opt.startsWith("-filter:v")) { // video filters
            vfilter.set(indx, cmdToken.get(i + 1).replace("\"", "")); // store without quotes
            i++;
          } else if (opt.startsWith("-filter_complex") || opt.startsWith("-lavfi")) { // safer to quit now than to
            // baffle users with strange errors later
            i++;
            logger.error("Command does not support complex filters - only simple -af or -vf filters are supported");
            throw new EncoderException(
                    "Cannot parse complex filters in" + profile.getIdentifier() + " for this operation");
          } else if (opt.startsWith("-af") || opt.startsWith("-filter:a")) { // audio filter
            afilter.set(indx, cmdToken.get(i + 1).replace("\"", "")); // store without quotes
            i++;
          } else if ("-i".equals(opt)) {
            i++; // inputs are now mapped, remove from command
          } else if (opt.startsWith("-c:") || opt.startsWith("-codec:") || opt.contains("-vcodec")
                  || opt.contains("-acodec")) { // cannot copy codec in complex filter
            String str = cmdToken.get(i + 1);
            if (str.contains("copy")) // c
              i++;
            else
              cmd = cmd + " " + opt;
          } else { // keep the rest
            cmd = cmd + " " + opt;
          }
          i++;
        }
        /* Remove unused commandline parts */
        cmd = cmd.replaceAll("#\\{.*?\\}", "");
        // Find the output map based on splits and filters
        if (size == 1) { // no split
          if (afilter.get(indx) == null)
            apads.set(indx, adjustForNoComplexFilter(aInputPad));
          else
            apads.set(indx, "[oa" + indx + "]");
          if (vfilter.get(indx) == null)
            vpads.set(indx, adjustForNoComplexFilter(vInputPad)); // No split, no filter - straight from input
          else
            vpads.set(indx, "[ov" + indx + "]");

        } else { // split
          vpads.set(indx, "[ov" + indx + "]"); // name the output pads from split -> input to final format
          apads.set(indx, "[oa" + indx + "]"); // name the output audio pads
        }
        cmd = StringUtils.trimToNull(cmd); // remove all leading/trailing white spaces
        if (cmd != null) {
          outputFiles.add(cmdToken.get(cmdToken.size() - 1));
          if (vInputPad != null) {
            outputs.add("-map " + vpads.get(indx));
          }
          if (aInputPad != null) {
            outputs.add("-map " + apads.get(indx)); // map video and audio input
          }
          outputs.add(cmd); // profiles appended in order, they are numbered 0,1,2,3...
          indx++; // indx for this profile
        }
      }
      setVideoFilters();
      setAudioFilters();
    }
  }

  /**
   * Clean up the edit points, make sure the gap between consecutive segments are larger than the transition Otherwise
   * it can be very slow to run and output will be ugly because the fades will extend the clip
   *
   * @param edits
   *          - clips to be stitched together
   * @param gap
   *          = transitionDuration / 1000; default gap size - same as fade
   * @return a list of sanitized video clips
   */
  private static List<VideoClip> sortSegments(List<VideoClip> edits, double gap) {
    LinkedList<VideoClip> ll = new LinkedList<VideoClip>();
    Iterator<VideoClip> it = edits.iterator();
    VideoClip clip;
    VideoClip nextclip;
    int lastSrc = -1;
    while (it.hasNext()) { // Skip sort if there are multiple sources
      clip = it.next();
      if (lastSrc < 0) {
        lastSrc = clip.getSrc();
      } else if (lastSrc != clip.getSrc()) {
        return edits;
      }
    }
    java.util.Collections.sort(edits); // Sort clips if all clips are from the same src
    List<VideoClip> clips = new ArrayList<VideoClip>();
    it = edits.iterator();
    while (it.hasNext()) { // Check for legal durations
      clip = it.next();
      if (clip.getDuration() > gap) { // Keep segments at least as long as transition fade
        ll.add(clip);
      }
    }
    clip = ll.pop(); // initialize
    // Clean up segments so that the cut out is at least as long as the transition gap (default is fade out-fade in)
    while (!ll.isEmpty()) { // Check that 2 consecutive segments from same src are at least GAP secs apart
      if (ll.peek() != null) {
        nextclip = ll.pop(); // check next consecutive segment
        if ((nextclip.getSrc() == clip.getSrc()) && (nextclip.getStart() - clip.getEnd()) < gap) { // collapse two
          // segments into one
          clip.setEnd(nextclip.getEnd()); // by using inpt of seg 1 and outpoint of seg 2
        } else {
          clips.add(clip); // keep last segment
          clip = nextclip; // check next segment
        }
      }
    }
    clips.add(clip); // add last segment
    return clips;
  }

  /**
   * Create the trim part of the complex filter and return the clauses for the complex filter. The transition is fade to
   * black then fade from black. The outputs are mapped to [ov] and [oa]
   *
   * @param clips
   *          - video segments as indices into the media files by time
   * @param transitionDuration
   *          - length of transition in MS between each segment
   * @param hasVideo
   *          - has video, from inspection
   * @param hasAudio
   *          - has audio
   * @return complex filter clauses to do editing for ffmpeg
   * @throws Exception
   *           - if it fails
   */
  private List<String> makeEdits(List<VideoClip> clips, int transitionDuration, Boolean hasVideo,
          Boolean hasAudio) throws Exception {
    double fade = transitionDuration / 1000.; // video and audio have the same transition duration
    DecimalFormatSymbols ffmpegFormat = new DecimalFormatSymbols();
    ffmpegFormat.setDecimalSeparator('.');
    DecimalFormat f = new DecimalFormat("0.00", ffmpegFormat);
    List<String> vpads = new ArrayList<>();
    List<String> apads = new ArrayList<>();
    List<String> clauses = new ArrayList<>(); // The clauses are ordered
    int n = 0;
    if (clips != null)
      n = clips.size();
    String outmap = "o";
    if (n > 1) { // Create the input pads if we have multiple segments
      for (int i = 0; i < n; i++) {
        vpads.add("[v" + i + "]"); // post filter
        apads.add("[a" + i + "]");
      }
      outmap = "";
      // Create the trims
      for (int i = 0; i < n; i++) { // Each clip
        // get clip and add fades to each clip
        VideoClip vclip = clips.get(i);
        int fileindx = vclip.getSrc(); // get source file by index
        double inpt = vclip.getStart(); // get in points
        double duration = vclip.getDuration();
        double vend = Math.max(duration - fade, 0);
        double aend = Math.max(duration - fade, 0);
        if (hasVideo) {
          String vvclip;
          vvclip = "[" + fileindx + ":v]trim=" + f.format(inpt) + ":duration=" + f.format(duration)
                  + ",setpts=PTS-STARTPTS"
                  + ((fade > 0) ? ",fade=t=in:st=0:d=" + fade + ",fade=t=out:st=" + f.format(vend) + ":d=" + fade
                          : "")
                  + "[" + outmap + "v" + i + "]";
          clauses.add(vvclip);
        }
        if (hasAudio) {
          String aclip;
          aclip = "[" + fileindx + ":a]atrim=" + f.format(inpt) + ":duration=" + f.format(duration)
                  + ",asetpts=PTS-STARTPTS"
                  + ((fade > 0)
                          ? ",afade=t=in:st=0:d=" + fade + ",afade=t=out:st=" + f.format(aend) + ":d=" + +fade
                          : "")
                  + "[" + outmap + "a" + i + "]";
          clauses.add(aclip);
        }
      }
      // use unsafe because different files may have different SAR/framerate
      if (hasVideo)
        clauses.add(StringUtils.join(vpads, "") + "concat=n=" + n + "[ov]"); // concat video clips
      if (hasAudio)
        clauses.add(StringUtils.join(apads, "") + "concat=n=" + n + ":v=0:a=1[oa]"); // concat audio clips in stream 0,
    } else if (n == 1) { // single segment
      VideoClip vclip = clips.get(0);
      int fileindx = vclip.getSrc(); // get source file by index
      double inpt = vclip.getStart(); // get in points
      double duration = vclip.getDuration();
      double vend = Math.max(duration - fade, 0);
      double aend = Math.max(duration - fade, 0);

      if (hasVideo) {
        String vvclip;

        vvclip = "[" + fileindx + ":v]trim=" + f.format(inpt) + ":duration=" + f.format(duration)
                + ",setpts=PTS-STARTPTS,"
                + ((fade > 0) ? "fade=t=in:st=0:d=" + fade + ",fade=t=out:st=" + f.format(vend) + ":d=" + fade : "")
                + "[ov]";

        clauses.add(vvclip);
      }
      if (hasAudio) {
        String aclip;
        aclip = "[" + fileindx + ":a]atrim=" + f.format(inpt) + ":duration=" + f.format(duration)
                + ",asetpts=PTS-STARTPTS,"
                + ((fade > 0) ? "afade=t=in:st=0:d=" + fade + ",afade=t=out:st=" + f.format(aend) + ":d=" : "")
                + fade + "[oa]";

        clauses.add(aclip);
      }
    }
    return clauses; // if no edits, there are no clauses
  }

  private Map<String, String> getParamsFromFile(File parentFile) {
    Map<String, String> params = new HashMap<>();
    String videoInput = FilenameUtils.normalize(parentFile.getAbsolutePath());
    params.put("in.video.path", videoInput);
    params.put("in.video.name", FilenameUtils.getBaseName(videoInput));
    params.put("in.name", FilenameUtils.getBaseName(videoInput)); // One of the names
    params.put("in.video.suffix", FilenameUtils.getExtension(videoInput));
    params.put("in.video.filename", FilenameUtils.getName(videoInput));
    params.put("in.video.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(videoInput));
    String outDir = parentFile.getAbsoluteFile().getParent(); // Use first file dir
    params.put("out.dir", outDir);
    String outFileName = FilenameUtils.getBaseName(parentFile.getName());
    params.put("out.name.base", outFileName); // Base file name used
    params.put("out.name", outFileName); // file name used - may be replaced
    return params;
  }

  /**
   * Concatenate segments of one or more input tracks specified by trim points into the track the edits are passed in as
   * double so that it is generic. The tracks are assumed to have the same resolution.
   *
   * @param inputs
   *          - input tracks as a list of files
   * @param edits
   *          - edits are a flat list of triplets, each triplet represent one clip: index (int) into input tracks, trim in point(long)
   *          in milliseconds and trim out point (long) in milliseconds for each segment
   * @param profiles
   *          - encoding profiles for the target
   * @param transitionDuration
   *          in ms, transition time between each edited segment
   * @throws EncoderException
   *           - if it fails
   */
  public List<File> multiTrimConcat(List<File> inputs, List<Long> edits, List<EncodingProfile> profiles,
          int transitionDuration) throws EncoderException {
    return multiTrimConcat(inputs, edits, profiles, transitionDuration, true, true);

  }

  public List<File> multiTrimConcat(List<File> inputs, List<Long> edits, List<EncodingProfile> profiles,
          int transitionDuration, boolean hasVideo, boolean hasAudio)
          throws EncoderException, IllegalArgumentException {
    if (inputs == null || inputs.size() < 1) {
      throw new IllegalArgumentException("At least one track must be specified.");
    }
    if (edits == null && inputs.size() > 1) {
      throw new IllegalArgumentException("If there is no editing, only one track can be specified.");
    }
    List<VideoClip> clips = null;
    if (edits != null) {
      clips = new ArrayList<VideoClip>(edits.size() / 3);
      int adjust = 0;
      // When the first clip starts at 0, and there is a fade, lip sync can be off,
      // this adjustment will mitigate the problem
      for (int i = 0; i < edits.size(); i += 3) {
        if (edits.get(i + 1) < transitionDuration) // If taken from the beginning of video
          adjust = transitionDuration / 2000; // add half the fade duration in seconds
        else
          adjust = 0;
        clips.add(new VideoClip(edits.get(i).intValue(), (double) edits.get(i + 1) / 1000 + adjust,
              (double) edits.get(i + 2) / 1000));
      }
      try {
        clips = sortSegments(clips, transitionDuration / 1000); // remove bad edit points
      } catch (Exception e) {
        logger.error("Illegal edits, cannot sort segment", e);
      throw new EncoderException("Cannot understand the edit points", e);
      }
    }
    // Set encoding parameters
    Map<String, String> params = null;
    if (inputs.size() > 0) { // Shared parameters - the rest are profile specific
      params = getParamsFromFile(inputs.get(0));
    }
    if (profiles == null || profiles.size() == 0) {
      logger.error("Missing encoding profiles");
      throw new EncoderException("Missing encoding profile(s)");
    }
    try {
      List<String> command = new ArrayList<>();
      List<String> clauses = makeEdits(clips, transitionDuration, hasVideo, hasAudio); // map inputs into [ov]
                                                                                               // and [oa]
      // Entry point for multiencode here, if edits is empty, then use raw channels instead of output from edits
      String videoOut = (clips == null) ? "[0:v]" : "[ov]";
      String audioOut = (clips == null) ? "[0:a]" : "[oa]";
      OutputAggregate outmaps = new OutputAggregate(profiles, params, (hasVideo ? videoOut : null),
              (hasAudio ? audioOut : null)); // map outputs from ov and oa
      if (hasAudio) {
        clauses.add(outmaps.getAsplit());
        clauses.add(outmaps.getAudioFilter());
      }
      if (hasVideo) {
        clauses.add(outmaps.getVsplit());
        clauses.add(outmaps.getVideoFilter());
      }
      clauses.removeIf(Objects::isNull); // remove all empty filters
      command.add("-nostats"); // no progress report
      for (File o : inputs) {
        command.add("-i"); // Add inputfile in the order of entry
        command.add(o.getCanonicalPath());
      }
      command.add("-filter_complex");
      command.add(StringUtils.join(clauses, ";"));
      for (String outpad : outmaps.getOutput()) {
        command.addAll(Arrays.asList(outpad.split("\\s+")));
      }
      return process(command); // Run the ffmpeg command
    } catch (Exception e) {
      logger.error("MultiTrimConcat failed to run command {} ", e.getMessage());
      throw new EncoderException("Cannot encode the inputs",e);
    }
  }

}
