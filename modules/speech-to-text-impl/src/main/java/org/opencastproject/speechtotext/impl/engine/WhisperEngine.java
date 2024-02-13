/*
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
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  /** Config key for quantization */
  private static final String WHISPER_QUANTIZATION = "whisper.quantization";

  /** Quantization for whisper-ctranslate2 */
  private enum Quantizations {
    auto, int8, int8_float16, int16, float16, float32
  }
  private Option<Quantizations> quantization = Option.none();

  /** Config key for Voice Activity Detection */
  private static final String WHISPER_VAD = "whisper.vad_enabled";

  /** Enable Voice Activity Detection for whisper-ctranslate2 */
  private Option<Boolean> isVADEnabled = Option.none();

  /** Pattern for whisper output. Searches for timestamps like this for example: [00:00.000 --> 00:06.000] */
  private final Pattern outputPattern = Pattern.compile("\\[\\d{2}:\\d{2}.\\d{3} --> \\d{2}:\\d{2}.\\d{3}]");

  /** Config key for additional Whisper args */
  private static final String WHISPER_ARGS_CONFIG_KEY = "whisper.args";

  /** Currently used Whisper args */
  private String[] whisperArgs = {};

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

    String t = (String) cc.getProperties().get(WHISPER_QUANTIZATION);
    if (!StringUtils.isBlank(t)) {
      quantization = Option.some(Quantizations.valueOf(t));
    }
    logger.debug("Whisper quantization set to {}", quantization);

    isVADEnabled = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPER_VAD);
    logger.debug("Whisper Voice Activity Detection  set to {}", isVADEnabled);

    String whisperArgsString = (String) cc.getProperties().get(WHISPER_ARGS_CONFIG_KEY);
    if (!StringUtils.isBlank(whisperArgsString)) {
      logger.debug("Additional args for Whisper configured: {}", whisperArgsString);
      whisperArgs = whisperArgsString.trim().split("\\s+");
    } else {
      logger.debug("No additional args for Whisper configured.");
    }

    logger.debug("Finished activating/updating speech-to-text service");
  }

  //TODO: Add method for language detection

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextEngine#generateSubtitlesFile(File, File, String, Boolean)
   */

  @Override
  public Result generateSubtitlesFile(File mediaFile,
      File workingDirectory, String language, Boolean translate)
          throws SpeechToTextEngineException {

    String[] baseCommands = { whisperExecutable,
    mediaFile.getAbsolutePath(),
        "--model", whisperModel,
        "--output_dir", workingDirectory.getAbsolutePath()};

    List<String> transcriptionCommand = new ArrayList<>(Arrays.asList(baseCommands));

    if (translate) {
      transcriptionCommand.add("--task");
      transcriptionCommand.add("translate");
      logger.debug("Translation enabled");
      language = "en";
    }

    if (!language.isBlank() && !translate) {
      logger.debug("Using language {} from workflows", language);
      transcriptionCommand.add("--language");
      transcriptionCommand.add(language);
    }

    if (quantization.isSome()) {
      logger.debug("Using quantization {}", quantization.get());
      transcriptionCommand.add("--compute_type");
      transcriptionCommand.add(quantization.get().toString());
    }

    if (isVADEnabled.isSome()) {
      logger.debug("Setting VAD to {}", isVADEnabled.get());
      transcriptionCommand.add("--vad_filter");
      transcriptionCommand.add(isVADEnabled.get().toString());
    }

    transcriptionCommand.addAll(Arrays.asList(whisperArgs));

    logger.info("Executing Whisper's transcription command: {}", transcriptionCommand);

    Process transcriptonProcess = null;
    File output;

    try {
      ProcessBuilder processBuilder = new ProcessBuilder(transcriptionCommand);
      processBuilder.redirectErrorStream(true);
      transcriptonProcess = processBuilder.start();

      try (BufferedReader in = new BufferedReader(new InputStreamReader(transcriptonProcess.getInputStream()))) {
        String line;
        while ((line = in.readLine()) != null) { // consume process output
          handleTranscriptionOutput(line);
        }

        // wait until the task is finished
        int exitCode = transcriptonProcess.waitFor();
        logger.debug("Whisper process finished with exit code {}", exitCode);

        if (exitCode != 0) {
          throw new SpeechToTextEngineException(
              String.format("Whisper exited abnormally with status %d (command: %s)", exitCode, transcriptionCommand));
        }
      }

      // Renaming output whisper filename to the expected output filename
      String outputFileName = FilenameUtils.getBaseName(mediaFile.getAbsolutePath()) + ".vtt";
      output = new File(workingDirectory, outputFileName);
      logger.debug("Whisper output file {}", output);

      if (!output.isFile()) {
        throw new SpeechToTextEngineException("Whisper produced no output");
      }
      logger.info("Subtitles file generated successfully: {}", output);
    } catch (Exception e) {
      logger.debug("Transcription failed closing Whisper transcription process for: {}", mediaFile);
      throw new SpeechToTextEngineException(e);
    } finally {
      if (transcriptonProcess != null) {
        transcriptonProcess.destroy();
        if (transcriptonProcess.isAlive()) {
          transcriptonProcess.destroyForcibly();
        }
      }
    }

    // Detect language if not set
    if (language.isBlank()) {
      var jsonFile = FilenameUtils.removeExtension(output.getAbsolutePath()) + ".json";
      var jsonParser = new JSONParser();
      try {
        FileReader reader = new FileReader(jsonFile);
        Object obj = jsonParser.parse(reader);
        JSONObject jsonObject = (JSONObject) obj;
        language = (String) jsonObject.get("language");
        logger.debug("Language detected by Whisper: {}", language);
      } catch (Exception e) {
        logger.debug("Error reading Whisper JSON file for: {}", mediaFile);
        throw new SpeechToTextEngineException(e);
      }
    }

    return new Result(language, output);
  }

  /**
   * Handles the transcription process output
   *
   * @param message the message returned by the transcription process
   */
  private void handleTranscriptionOutput(String message) {
    message = message.trim();
    if ("".equals(message)) {
      return;
    }
    // We do not want to log output lines starting with timestamps like this: [00:00.000 --> 00:06.000]
    // Lines like this containing transcriptions. It would be too much output (and is probably unnecessary anyway)
    Matcher matcher = outputPattern.matcher(message);
    if (!matcher.find()) {
      logger.debug(message);
    }
  }

}

