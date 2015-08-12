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

import org.opencastproject.composer.impl.AbstractCmdlineEmbedderEngine;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

public class FFmpegEmbedderEngine extends AbstractCmdlineEmbedderEngine {

  /** Default location of the ffmepg binary */
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** Parameter name for retrieving ffmpeg path */
  private static final String CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpeg.path";

  /** Command line template for executing job */
  private static final String CMD_TEMPLATE = "#{-i in.media.path} -acodec copy -vcodec copy #<-i #{in.captions.path} -scodec mov_text -metadata:s:s:#{param.index} language=#{param.lang}> #{out.media.path}";

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

  /*@Override
  public File embed(File mediaSource, File[] captionSources, String[] captionLanguages, Map<String, String> properties)
          throws EmbedderException {
    // TODO Auto-generated method stub
    return null;
  }*/

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
