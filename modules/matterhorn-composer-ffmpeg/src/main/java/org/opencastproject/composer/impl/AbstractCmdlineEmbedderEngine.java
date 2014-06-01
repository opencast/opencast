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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EmbedderEngine;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.util.IoSupport;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract class for command line embedder engines.
 *
 */
public abstract class AbstractCmdlineEmbedderEngine implements EmbedderEngine {

  /** the encoder binary */
  private String binary = null;

  /** the command line options */
  private String cmdTemplate = "";

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(AbstractCmdlineEmbedderEngine.class.getName());

  /**
   * Creates embedder engine with given binary.
   *
   * @param binary
   *          path to the binary of specific embedder
   */
  public AbstractCmdlineEmbedderEngine(String binary) {
    if (binary == null) {
      throw new IllegalArgumentException("Binary is null.");
    }

    this.binary = binary;
  }

  /**
   *
   * {@inheritDoc} Language attribute is normalized via <code>normalizeLanguage</code> method even if it is not present.
   * If normalized language returned is <code>null</code>, exception will be thrown.
   *
   * @see org.opencastproject.composer.api.EmbedderEngine#embed(java.io.File, java.io.File, java.util.Map)
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
    }

    // execute command
    List<String> commandList = buildCommandFromTemplate(embedderProperties);

    logger.debug("Executing embedding command {}", commandList);

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
    commandList.add(binary);

    // process command line
    StringBuffer buffer = new StringBuffer();
    // process array parameters
    Pattern pattern = Pattern.compile("#<.+?>");
    Matcher matcher = pattern.matcher(cmdTemplate);
    while (matcher.find()) {
      String processedArray = buildArrayCommandFromTemplate(
              cmdTemplate.substring(matcher.start() + 2, matcher.end() - 1), properties);
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

  /**
   * Set binary for embedder engine.
   *
   * @param binary
   */
  protected void setBinary(String binary) {
    if (binary == null) {
      throw new IllegalArgumentException("Binary is null.");
    }
    this.binary = binary;
  }

  /**
   * Set template command for embedder engine. Variables are specified in one of two ways: #{&lt;switch&gt; &lt;key&gt;}
   * or #{&lt;key&gt;}. For array parameters (those parameters that can be set multiple times) the following form is
   * used: #&lt; one_or_more_variables &gt;
   *
   * @param cmdTemplate
   *          template for given command line embedder engine
   */
  protected void setCmdTemplate(String cmdTemplate) {
    this.cmdTemplate = cmdTemplate;
  }

  /**
   * Function that normalizes language attribute to valid language code for specific embedder engine. Should be able to
   * process any input, even <code>null</code> string and substitute it for reasonable code or return null. In this case
   * exception will be thrown.
   *
   * @param language
   * @return
   */
  protected abstract String normalizeLanguage(String language);

  /**
   * Method to which embedder output is directed.
   *
   * @param output
   *          embedder output
   * @param sourceFiles
   *          source files used in operation
   */
  protected abstract void handleEmbedderOutput(String output, File... sourceFiles);

  /**
   * Returns file resulting from embedding.
   *
   * @param properties
   *          properties used for initiating embedding job
   * @return resulting file
   */
  protected abstract File getOutputFile(Map<String, String> properties);
}
