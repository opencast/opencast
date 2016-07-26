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

package org.opencastproject.composer.impl.ffmpeg;

import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.impl.AbstractCmdlineEmbedderEngine;
import org.opencastproject.util.IoSupport;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegEmbedderEngine extends AbstractCmdlineEmbedderEngine {

  /** Default location of the ffmepg binary */
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** Parameter name for retrieving ffmpeg path */
  private static final String CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpeg.path";

  /** Command line template for executing job */
  private static final String CMD_TEMPLATE = "#{-i in.media.path} #<-i #{in.captions.path}> -c copy #<-map #{param.input}:0> #<-map #{param.map}:0 -metadata:s:s:#{param.index} language=#{param.lang}> -scodec mov_text #{out.media.path}";

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(FFmpegEmbedderEngine.class);

  /**
   * Creates the ffmpeg embedder engine.
   */
  public FFmpegEmbedderEngine() {
    super(FFMPEG_BINARY_DEFAULT);
    setCmdTemplate(CMD_TEMPLATE);
  }

  public void activate(ComponentContext cc) {
    // Configure ffmpeg
    String path = (String) cc.getBundleContext().getProperty(CONFIG_FFMPEG_PATH);
    if (path == null) {
      logger.debug("DEFAULT " + CONFIG_FFMPEG_PATH + ": " + FFmpegEncoderEngine.FFMPEG_BINARY_DEFAULT);
    } else {
      setBinary(path);
      logger.debug("FFmpegEmbedderEngine config binary: {}", path);
    }
  }

  /**
   *
   * {@inheritDoc} Language attribute is normalized via <code>normalizeLanguage</code> method even if it is not present.
   * If normalized language returned is <code>null</code>, exception will be thrown.
   */
  @Override
  public File embed(File mediaSource, File[] captionSources, String[] captionLanguages, Map<String, String> properties)
          throws EmbedderException {

    if (mediaSource == null) {
      logger.error("Media source is missing");
      throw new EmbedderException("Missing media source.");
    }
    if (captionSources == null || captionSources.length == 0) {
      logger.error("Captions are missing");
      throw new EmbedderException("Missing captions.");
    }
    if (captionLanguages == null || captionLanguages.length == 0) {
      logger.error("Caption languages are missing");
      throw new EmbedderException("Missing caption language codes.");
    }

    // add all properties
    Map<String, String> embedderProperties = new HashMap<String, String>();
    embedderProperties.putAll(properties);

    // add file properties
    embedderProperties.put("in.media.path", mediaSource.getAbsolutePath());
    embedderProperties.put("out.media.path",
            mediaSource.getAbsoluteFile().getParent() + File.separator + UUID.randomUUID() + "-caption."
                    + FilenameUtils.getExtension(mediaSource.getAbsolutePath()));

    int inputStreamCount;
    try {
      inputStreamCount = Integer.valueOf(properties.get("param.input.stream.count"));
    } catch (NumberFormatException e) {
      logger.info("No stream count found, assuming input file is single-stream");
      inputStreamCount = 1;
    }
    for (int i = 0; i < inputStreamCount; i++) {
      embedderProperties.put("param.input." + i, String.valueOf(i));
    }

    for (int i = 0; i < ((captionSources.length > captionLanguages.length) ? captionSources.length
            : captionLanguages.length); i++) {
      embedderProperties.put("in.captions.path." + i, captionSources[i].getAbsolutePath());
      // check/normalize language property
      String language = normalizeLanguage(captionLanguages[i]);
      if (language == null) {
        logger.error("Language property was set to null.");
        throw new EmbedderException("Captions language has not been set.");
      }
      embedderProperties.put("param.lang." + i, language);
      embedderProperties.put("param.index." + i, String.valueOf(i));
      embedderProperties.put("param.map." + i, String.valueOf(i + inputStreamCount));
    }

    // execute command
    List<String> commandList = buildCommandFromTemplate(embedderProperties);

    logger.debug("Executing embedding command {}", StringUtils.join(commandList, " "));

    Process embedderProcess = null;
    BufferedReader processReader = null;
    // create and start process
    try {
      ProcessBuilder pb = new ProcessBuilder(commandList);
      pb.redirectErrorStream(true);

      embedderProcess = pb.start();

      // process embedder output
      processReader = new BufferedReader(new InputStreamReader(embedderProcess.getInputStream()));
      String line = null;
      while ((line = processReader.readLine()) != null) {
        handleEmbedderOutput(line);
      }

      embedderProcess.waitFor();
      int exitCode = embedderProcess.exitValue();
      if (exitCode != 0) {
        throw new EmbedderException("Embedder exited abnormally with error code: " + exitCode);
      }

      return getOutputFile(embedderProperties);

    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new EmbedderException(e);
    } finally {
      IoSupport.closeQuietly(processReader);
      IoSupport.closeQuietly(embedderProcess);
    }
  }

  /**
   * Builds command list out of template command by substituting input parameters in form #{&lt;parameter&gt;} with
   * values from properties. If for some parameter there is no matching value, parameter is removed. Parameters that are
   * set via switches are represented as #{&lt;switch&gt; &lt;key&gt;}. Arrays of parameters are represented #&lt;
   * parameters(s) &gt;
   *
   * @param properties
   *          map that contains key/values pairs for building command. Unused pairs are ignored.
   * @return built list that represents command
   */
  protected List<String> buildCommandFromTemplate(Map<String, String> properties) {

    // add binary
    List<String> commandList = new LinkedList<String>();
    commandList.add(super.getBinary());

    // process command line
    StringBuffer buffer = new StringBuffer();
    // process array parameters
    Pattern pattern = Pattern.compile("#<.+?>");
    Matcher matcher = pattern.matcher(CMD_TEMPLATE);
    while (matcher.find()) {
      String processedArray = buildArrayCommandFromTemplate(
              CMD_TEMPLATE.substring(matcher.start() + 2, matcher.end() - 1), properties);
      matcher.appendReplacement(buffer, processedArray);
    }
    matcher.appendTail(buffer);
    String arrayProcessedCmd = buffer.toString();
    // process normal parameters
    buffer = new StringBuffer();
    pattern = Pattern.compile("#\\{.+?\\}");
    matcher = pattern.matcher(arrayProcessedCmd);
    while (matcher.find()) {
      String match = arrayProcessedCmd.substring(matcher.start() + 2, matcher.end() - 1);
      if (match.contains(" ")) {
        String value = properties.get(match.split(" ")[1].trim());
        if (value == null) {
          matcher.appendReplacement(buffer, "");
        } else {
          matcher.appendReplacement(buffer, match.split(" ")[0] + " " + value);
        }
      } else {
        String value = properties.get(match.trim());
        if (value == null) {
          matcher.appendReplacement(buffer, "");
        } else {
          matcher.appendReplacement(buffer, value);
        }
      }
    }
    matcher.appendTail(buffer);

    // split and convert to array list
    String[] cmdArray = buffer.toString().split(" ");
    for (String e : cmdArray) {
      if (!"".equals(e))
        commandList.add(e);
    }

    return commandList;
  }

  /**
   * Given array template, it will process all parameters in form #{&lt;param_name&gt;} and will try to substitute them
   * with values from properties as long as there are proper values. Substitution is performed in the following way: if
   * parameter name is "param.example" properties are searched for keys that starts with parameter name. If such key is
   * found, suffix is extracted and applied to other parameters in attempt to retrieve corresponding values for other
   * parameters. Substitution is successful if all parameters are substituted. If this is not the case this array
   * element is ignored.
   *
   * @param template
   * @param properties
   * @return
   */
  protected String buildArrayCommandFromTemplate(String template, Map<String, String> properties) {

    List<String> arrayParameters = new LinkedList<String>();
    StringBuffer buffer = new StringBuffer();

    // get all parameters for array
    Pattern pattern = Pattern.compile("#\\{.+?\\}");
    Matcher matcher = pattern.matcher(template);
    while (matcher.find()) {
      arrayParameters.add(template.substring(matcher.start() + 2, matcher.end() - 1));
    }

    if (arrayParameters.isEmpty()) {
      return "";
    }

    for (Map.Entry<String, String> e : properties.entrySet()) {
      //The index is based on the *other* keys, so we skip this if we hit it.  It gets handled below.
      if (e.getKey().startsWith(arrayParameters.get(0)) && e.getKey().length() > arrayParameters.get(0).length()) {
        // got element that can be inserted in array - find all corresponding elements
        String suffix = e.getKey().substring(arrayParameters.get(0).length());
        String arrayElement = template.replace("#{" + arrayParameters.get(0) + "}", e.getValue());
        for (int i = 1; i < arrayParameters.size() && properties.containsKey(arrayParameters.get(i) + suffix); i++) {
          arrayElement = arrayElement.replace("#{" + arrayParameters.get(i) + "}",
                  properties.get(arrayParameters.get(i) + suffix));
        }
        if (!arrayElement.matches("^#\\{.+?\\}$")) {
          buffer.append(arrayElement);
          buffer.append(" ");
        }
      }
    }

    return buffer.toString();
  }

  @Override
  protected String normalizeLanguage(String language) {

    if (language == null) {
      logger.warn("Language code attribute is null. Language set to: {}", "und");
      return "und";
    }

    // truncating if necessary
    if (language.length() > 2) {
      logger.warn("Language code {} too long, truncating...", language);
      language = language.substring(0, 2);
    }

    // set to lower case
    language = language.toLowerCase();

    // constructing locale
    Locale loc = new Locale(language);
    try {
      return loc.getISO3Language();
    } catch (MissingResourceException e) {
      logger.warn("Could not determine language. Language set to: {}", "und");
      return "und";
    }
  }

  @Override
  protected void handleEmbedderOutput(String output, File... sourceFiles) {
    if (output.startsWith("Info:")) {
      logger.info(output);
    } else if (output.startsWith("Warning:")) {
      logger.warn(output);
    } else if (output.startsWith("Error:")) {
      logger.error(output);
    }
  }

  @Override
  protected File getOutputFile(Map<String, String> properties) {
    // check if output file property has been set
    if (properties.containsKey("out.media.path")) {
      return new File(properties.get("out.media.path"));
    }
    return new File(properties.get("in.media.path"));
  }

}
