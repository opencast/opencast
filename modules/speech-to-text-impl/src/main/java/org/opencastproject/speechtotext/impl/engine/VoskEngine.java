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

package org.opencastproject.speechtotext.impl.engine;

import org.opencastproject.speechtotext.api.SpeechToTextEngine;
import org.opencastproject.speechtotext.api.SpeechToTextEngineException;
import org.opencastproject.util.IoSupport;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Vosk implementation of the Speech-to-text engine interface. */
@Component(
    property = {
        "service.description=Vosk implementation of the SpeechToTextEngine interface",
        "enginetype=vosk"
    }
)
public class VoskEngine implements SpeechToTextEngine {

  private static final Logger logger = LoggerFactory.getLogger(VoskEngine.class);

  /** Name of the engine. */
  private static final String engineName = "Vosk";

  /** Config key for setting the path to the vosk. */
  private static final String VOSK_EXECUTABLE_PATH_CONFIG_KEY = "vosk.root.path";

  /** Default path to vosk. */
  public static final String VOSK_EXECUTABLE_DEFAULT_PATH = "vosk-cli";

  /** Currently used path of the vosk installation. */
  private String voskExecutable = VOSK_EXECUTABLE_DEFAULT_PATH;

  /** Config key to set default language */
  private static final String VOSK_DEFAULT_LANGUAGE_KEY = "vosk.default.language";

  /** Default Language */
  public static final String VOSK_DEFAULT_LANGUAGE = "eng";

  /** Currently used default language for Vosk */
  private  String voskLanguage = VOSK_DEFAULT_LANGUAGE;


  @Override
  public String getEngineName() {
    return engineName;
  }

  @Activate
  @Modified
  public void activate(ComponentContext cc) {
    logger.debug("Activated/Modified Vosk engine service class");
    voskExecutable = StringUtils.defaultIfBlank(
            (String) cc.getProperties().get(VOSK_EXECUTABLE_PATH_CONFIG_KEY), VOSK_EXECUTABLE_DEFAULT_PATH);
    voskLanguage = StringUtils.defaultIfBlank(
        (String)  cc.getProperties().get(VOSK_DEFAULT_LANGUAGE_KEY), VOSK_DEFAULT_LANGUAGE);
    logger.debug("Set vosk path to {}", voskExecutable);
    logger.debug("Set default vosk language to {}", voskLanguage);
    logger.debug("Finished activating/updating speech-to-text service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextEngine#generateSubtitlesFile(File, File, String, Boolean)
   */
  @Override
  public Result generateSubtitlesFile(File mediaFile, File workingDirectory,
      String language, Boolean translate)
          throws SpeechToTextEngineException {

    if (language.isBlank()) {
      logger.debug("Language field empty, using {} as default language", voskLanguage);
      language = voskLanguage;
    }

    var output = new File(workingDirectory, FilenameUtils.getBaseName(mediaFile.getAbsolutePath()) + ".vtt");
    final List<String> command = Arrays.asList(
            voskExecutable,
            "-i", mediaFile.getAbsolutePath(),
            "-o", output.getAbsolutePath(),
            "-l", language);
    logger.info("Executing Vosk's transcription command: {}", command);

    Process process = null;
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      process = processBuilder.start();

      // wait until the task is finished
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        var error = "";
        try (var errorStream = process.getInputStream()) {
          error = "\n Output:\n" + IOUtils.toString(errorStream, StandardCharsets.UTF_8);
        }
        throw new SpeechToTextEngineException(
                String.format("Vosk exited abnormally with status %d (command: %s)%s", exitCode, command, error));
      }
      if (!output.isFile()) {
        throw new SpeechToTextEngineException("Vosk produced no output");
      }
      logger.info("Subtitles file generated successfully: {}", output);
    } catch (Exception e) {
      logger.debug("Transcription failed closing Vosk transcription process for: {}", mediaFile);
      throw new SpeechToTextEngineException(e);
    } finally {
      IoSupport.closeQuietly(process);
    }
    return new Result(language, output);
  }

}
