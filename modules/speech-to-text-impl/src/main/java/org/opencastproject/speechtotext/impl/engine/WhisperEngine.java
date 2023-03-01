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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Whisper implementation of the Speech-to-text engine interface. */
@Component(
    property = {
        "service.description=Whisper implementation of the SpeechToTextEngine interface",
        "enginetype=whisper"
    }
)

public class WhisperEngine implements SpeechToTextEngine {

  private static final Logger logger = LoggerFactory.getLogger(WhisperEngine.class);

  /** Name of the engine. */
  private static final String engineName = "Whisper";

  /** Config key for setting the path to Whisper. */
  private static final String WHISPER_EXECUTABLE_PATH_CONFIG_KEY = "whisper.root.path";

  /** Default path to Whisper. */
  public static final String WHISPER_EXECUTABLE_DEFAULT_PATH = "whisper";

  /** Currently used path of the Whisper installation. */
  private String whisperExecutable = WHISPER_EXECUTABLE_DEFAULT_PATH;

  /** Config key for setting whisper model */
  private static final String WHISPER_MODEL_CONFIG_KEY = "whisper.model";

  /** Default whisper model */
  public static final String WHISPER_MODEL_DEFAULT = "base";

  /** Currently used whisper model */
  private String whisperModel = WHISPER_MODEL_DEFAULT;


  @Override
  public String getEngineName() {
    return engineName;
  }

  @Activate
  @Modified
  public void activate(ComponentContext cc) {
    logger.debug("Activated/Modified Whisper engine service class");
    whisperExecutable = StringUtils.defaultIfBlank(
        (String) cc.getProperties().get(WHISPER_EXECUTABLE_PATH_CONFIG_KEY), WHISPER_EXECUTABLE_DEFAULT_PATH);
    logger.debug("Set Whisper path to {}", whisperExecutable);

    whisperModel = StringUtils.defaultIfBlank(
        (String) cc.getProperties().get(WHISPER_MODEL_CONFIG_KEY), WHISPER_MODEL_DEFAULT);
    logger.debug("Whisper Language model set to {}", whisperModel);

    logger.debug("Finished activating/updating speech-to-text service");
  }

  //TODO: Add method for language detection
  //TODO: Add optional language translation to english

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextEngine#generateSubtitlesFile(File, File, String, Boolean)
   */

  @Override
  public List<Object> generateSubtitlesFile(File mediaFile, File preparedOutputFile, String language, Boolean translate)
          throws SpeechToTextEngineException {


    List<String> command = Arrays.asList(
        whisperExecutable,
        mediaFile.getAbsolutePath(),
        "--model", whisperModel,
        "--output_dir", preparedOutputFile.getParent()
    );

    if (translate) {
      command.add("--task translate");
      logger.debug("Translation enabled");
      language = "en";
    }

    if (!language.isBlank()) {
      logger.debug("Using language {} from workflows", language);
      String lang = "--language " + language;
      command.add(lang);
    }

    logger.info("Executing Whisper's transcription command: {}", command);

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
            String.format("Whisper exited abnormally with status %d (command: %s)%s", exitCode, command, error));
      }

      File whisperVTT = new File((preparedOutputFile.getParent() + "/" + mediaFile.getName() + ".vtt"));
      preparedOutputFile.renameTo(whisperVTT);

      if (!preparedOutputFile.isFile()) {
        throw new SpeechToTextEngineException("Whisper produced no output");
      }
      logger.info("Subtitles file generated successfully: {}", preparedOutputFile);
    } catch (Exception e) {
      logger.debug("Transcription failed closing Whisper transcription process for: {}", mediaFile);
      throw new SpeechToTextEngineException(e);
    } finally {
      IoSupport.closeQuietly(process);
    }

    // Detect language if not set
    if (language.isBlank()) {
      JSONParser jsonParser = new JSONParser();
      try {
        String jsonLanguage;
        FileReader reader = new FileReader((preparedOutputFile.getParent() + "/" + mediaFile.getName() + ".json"));
        Object obj = jsonParser.parse(reader);
        JSONObject jsonObject = (JSONObject) obj;
        jsonLanguage = (String) jsonObject.get("language");
        language = jsonLanguage;
      } catch (Exception e) {
        logger.debug("Error reading Whisper JSON file for: {}", mediaFile);
        throw new SpeechToTextEngineException(e);
      }
    }
    List<Object> returnValues = new ArrayList<>();
    returnValues.add(preparedOutputFile);
    returnValues.add(language);
    return returnValues; // Subtitles data
  }
}

