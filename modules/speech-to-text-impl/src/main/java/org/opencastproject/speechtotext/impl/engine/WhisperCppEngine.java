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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** WhisperC++ implementation of the Speech-to-text engine interface. */
@Component(
    property = {
        "service.description=WhisperC++ implementation of the SpeechToTextEngine interface",
        "enginetype=whispercpp"
    }
)

public class WhisperCppEngine implements SpeechToTextEngine {

  private static final Logger logger = LoggerFactory.getLogger(WhisperCppEngine.class);

  /** Name of the engine. */
  private static final String engineName = "WhisperC++";

  /** Config key for setting the path to WhisperC++. */
  private static final String WHISPERCPP_EXECUTABLE_PATH_CONFIG_KEY = "whispercpp.root.path";

  /** Default path to WhisperC++. */
  public static final String WHISPERCPP_EXECUTABLE_DEFAULT_PATH = "whispercpp";

  /** Currently used path of the WhisperC++ installation. */
  private String whispercppExecutable = WHISPERCPP_EXECUTABLE_DEFAULT_PATH;

  /** Config key for setting whispercpp model */
  private static final String WHISPERCPP_MODEL_CONFIG_KEY = "whispercpp.model";

  /** Default whispercpp model */
  public static final String WHISPERCPP_MODEL_DEFAULT = "/usr/share/ggml/ggml-base.bin";

  /** Currently used whispercpp model */
  private String whispercppModel = WHISPERCPP_MODEL_DEFAULT;


  @Override
  public String getEngineName() {
    return engineName;
  }

  @Activate
  @Modified
  public void activate(ComponentContext cc) {
    logger.debug("Activated/Modified WhisperC++ engine service class");
    whispercppExecutable = StringUtils.defaultIfBlank(
        (String) cc.getProperties().get(WHISPERCPP_EXECUTABLE_PATH_CONFIG_KEY), WHISPERCPP_EXECUTABLE_DEFAULT_PATH);
    logger.debug("Set WhisperC++ path to {}", whispercppExecutable);

    whispercppModel = StringUtils.defaultIfBlank(
        (String) cc.getProperties().get(WHISPERCPP_MODEL_CONFIG_KEY), WHISPERCPP_MODEL_DEFAULT);
    logger.debug("WhisperC++ Language model set to {}", whispercppModel);

    logger.debug("Finished activating/updating speech-to-text service");
  }

  //TODO: Add method for language detection

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextEngine#generateSubtitlesFile(File, File, String, Boolean)
   */

  @Override
  public Map<String, Object> generateSubtitlesFile(File mediaFile,
      File preparedOutputFile, String language, Boolean translate)
          throws SpeechToTextEngineException {

    String[] baseCommands = { whispercppExecutable,
        mediaFile.getAbsolutePath(),
        "--model", whispercppModel,
        "-ovtt",
        "-oj",
        "-bs", "5",
        "--output-file", preparedOutputFile.getAbsolutePath().replaceFirst("[.][^.]+$", "")};

    List<String> command = new ArrayList<>(Arrays.asList(baseCommands));

    if (translate) {
      command.add("--translate");
      logger.info("Translation enabled");
      language = "en";
    }

    if (!language.isBlank() && !translate) {
      logger.info("Using language {} from workflows", language);
      command.add("--language");
      command.add(language);
    }

    Process process = null;

    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      process = processBuilder.start();


      // wait until the task is finished
      int exitCode = process.waitFor();
      logger.info("WhisperC++ process finished with exit code {}",exitCode);

      if (exitCode != 0) {
        var error = "";
        try (var errorStream = process.getInputStream()) {
          error = "\n Output:\n" + IOUtils.toString(errorStream, StandardCharsets.UTF_8);
        }
        throw new SpeechToTextEngineException(
            String.format("WhisperC++ exited abnormally with status %d (command: %s)%s", exitCode, command, error));
      }

      if (!preparedOutputFile.isFile()) {
        throw new SpeechToTextEngineException("WhisperC++ produced no output");
      }
      logger.info("Subtitles file generated successfully: {}", preparedOutputFile);
    } catch (Exception e) {
      logger.info("Transcription failed closing WhisperC++ transcription process for: {}", mediaFile);
      throw new SpeechToTextEngineException(e);
    } finally {
      IoSupport.closeQuietly(process);
    }

    // Detect language if not set
    if (language.isBlank()) {
      JSONParser jsonParser = new JSONParser();
      try {
        FileReader reader = new FileReader(preparedOutputFile.getAbsolutePath().replaceFirst("[.][^.]+$", "")
            + ".json");
        Object obj = jsonParser.parse(reader);
        JSONObject jsonObject = (JSONObject) obj;
        language = (String) jsonObject.get("language");
        logger.info("Language detected by WhisperC++: {}", language);
      } catch (Exception e) {
        logger.info("Error reading WhisperC++ JSON file for: {}", mediaFile);
        throw new SpeechToTextEngineException(e);
      }
    }

    Map<String,Object> returnValues = new HashMap<>();
    returnValues.put("subFile",preparedOutputFile);
    returnValues.put("language",language);

    return returnValues; // Subtitles data
  }
}
