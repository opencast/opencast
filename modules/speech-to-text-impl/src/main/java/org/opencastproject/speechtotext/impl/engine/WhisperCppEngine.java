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
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;

import org.apache.commons.io.FilenameUtils;
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

  /** Config key for setting whispercpp beam size */
  private static final String WHISPERCPP_BEAM_SIZE_CONFIG_KEY = "whispercpp.beam-size";

  /** Currently used whispercpp beam size */
  private Option<Integer> whispercppBeamSize;

  /** Config key for setting whispercpp maximum segment length */
  private static final String WHISPERCPP_MAX_LENGTH_CONFIG_KEY = "whispercpp.max-len";

  /** Currently used whispercpp maximum segment length */
  private Option<Integer> whispercppMaxLength;

  /** Config key for setting whispercpp number of threads */
  private static final String WHISPERCPP_THREADS_CONFIG_KEY = "whispercpp.threads";

  /** Currently used whispercpp number of threads */
  private Option<Integer> whispercppThreads;

  /** Config key for setting whispercpp number of processors */
  private static final String WHISPERCPP_PROCESSORS_CONFIG_KEY = "whispercpp.processors";

  /** Currently used whispercpp number of processors */
  private Option<Integer> whispercppProcessors;

  /** Config key for setting whispercpp maximum context */
  private static final String WHISPERCPP_MAX_CONTEXT_CONFIG_KEY = "whispercpp.max-context";

  /** Currently used whispercpp maximum context */
  private Option<Integer> whispercppMaxContext;

  /** Config key for setting whispercpp split on word */
  private static final String WHISPERCPP_SPLIT_ON_WORD_CONFIG_KEY = "whispercpp.split-on-word";

  /** Currently used whispercpp split on word */
  private Option<Boolean> whispercppSplitOnWord;

  /** Config key for setting whispercpp number of best candidates to keep */
  private static final String WHISPERCPP_BEST_OF_CONFIG_KEY = "whispercpp.best-of";

  /** Currently used whispercpp number of best candidates to keep */
  private Option<Integer> whispercppBestOf;

  /** Config key for setting whispercpp word probability threshold */
  private static final String WHISPERCPP_WORD_THRESHOLD_CONFIG_KEY = "whispercpp.word-thold";

  /** Currently used whispercpp word probability threshold */
  private Option<Double> whispercppWordThreshold;

  /** Config key for setting whispercpp entropy threshold for decoder fail */
  private static final String WHISPERCPP_ENTROPY_THRESHOLD_CONFIG_KEY = "whispercpp.entropy-thold";

  /** Currently used whispercpp entropy threshold for decoder fail */
  private Option<Double> whispercppEntropyThreshold;

  /** Config key for setting whispercpp log probability threshold for decoder fail */
  private static final String WHISPERCPP_LOG_PROB_THRESHOLD_CONFIG_KEY = "whispercpp.logprob-thold";

  /** Currently used whispercpp log probability threshold for decoder fail */
  private Option<Double> whispercppLogProbThreshold;

  /** Config key for setting whispercpp diarization */
  private static final String WHISPERCPP_DIARIZATION_CONFIG_KEY = "whispercpp.diarize";

  /** Currently used whispercpp diarization */
  private Option<Boolean> whispercppDiarization;

  /** Config key for setting whispercpp tinydiarization */
  private static final String WHISPERCPP_TINY_DIARIZATION_CONFIG_KEY = "whispercpp.tinydiarize";

  /** Currently used whispercpp tinydiarization */
  private Option<Boolean> whispercppTinyDiarization;

  /** Config key for setting whispercpp no fallback */
  private static final String WHISPERCPP_NO_FALLBACK_CONFIG_KEY = "whispercpp.no-fallback";

  /** Currently used whispercpp no fallback */
  private Option<Boolean> whispercppNoFallback;


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

    whispercppBeamSize = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_BEAM_SIZE_CONFIG_KEY);
    if (whispercppBeamSize.isSome()) {
      logger.debug("WhisperC++ beam size set to {}", whispercppBeamSize);
    }

    whispercppMaxLength = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_MAX_LENGTH_CONFIG_KEY);
    if (whispercppMaxLength.isSome()) {
      logger.debug("WhisperC++ maximum segment length set to {}", whispercppMaxLength);
    }

    whispercppThreads = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_THREADS_CONFIG_KEY);
    if (whispercppThreads.isSome()) {
      logger.debug("WhisperC++ number of threads set to {}", whispercppThreads);
    }

    whispercppProcessors = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_PROCESSORS_CONFIG_KEY);
    if (whispercppProcessors.isSome()) {
      logger.debug("WhisperC++ number of processors set to {}", whispercppProcessors);
    }

    whispercppMaxContext = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_MAX_CONTEXT_CONFIG_KEY);
    if (whispercppMaxContext.isSome()) {
      logger.debug("WhisperC++ max context set to {}", whispercppMaxContext);
    }

    whispercppSplitOnWord = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_SPLIT_ON_WORD_CONFIG_KEY);
    if (whispercppSplitOnWord.isSome()) {
      logger.debug("WhisperC++ split on word set to {}", whispercppSplitOnWord);
    }

    whispercppBestOf = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_BEST_OF_CONFIG_KEY);
    if (whispercppBestOf.isSome()) {
      logger.debug("WhisperC++ best of set to {}", whispercppBestOf);
    }

    whispercppWordThreshold = OsgiUtil.getOptCfg(cc.getProperties(), WHISPERCPP_WORD_THRESHOLD_CONFIG_KEY).bind(
        Strings.toDouble);
    if (whispercppWordThreshold.isSome()) {
      logger.debug("WhisperC++ word threshold set to {}", whispercppWordThreshold);
    }

    whispercppEntropyThreshold = OsgiUtil.getOptCfg(cc.getProperties(), WHISPERCPP_ENTROPY_THRESHOLD_CONFIG_KEY).bind(
        Strings.toDouble);
    if (whispercppEntropyThreshold.isSome()) {
      logger.debug("WhisperC++ entropy threshold set to {}", whispercppEntropyThreshold);
    }

    whispercppLogProbThreshold = OsgiUtil.getOptCfg(cc.getProperties(), WHISPERCPP_LOG_PROB_THRESHOLD_CONFIG_KEY).bind(
        Strings.toDouble);
    if (whispercppLogProbThreshold.isSome()) {
      logger.debug("WhisperC++ log prob threshold set to {}", whispercppLogProbThreshold);
    }

    whispercppDiarization = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_DIARIZATION_CONFIG_KEY);
    if (whispercppDiarization.isSome()) {
      logger.debug("WhisperC++ diarization set to {}", whispercppDiarization);
    }

    whispercppTinyDiarization = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_TINY_DIARIZATION_CONFIG_KEY);
    if (whispercppTinyDiarization.isSome()) {
      logger.debug("WhisperC++ tiny diarization set to {}", whispercppTinyDiarization);
    }

    whispercppNoFallback = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_NO_FALLBACK_CONFIG_KEY);
    if (whispercppNoFallback.isSome()) {
      logger.debug("WhisperC++ no fallback set to {}", whispercppNoFallback);
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
  public Map<String, Object> generateSubtitlesFile(File mediaFile,
      File preparedOutputFile, String language, Boolean translate)
          throws SpeechToTextEngineException {

    if (!FilenameUtils.isExtension(mediaFile.getPath(), "wav")) {
      throw new SpeechToTextEngineException("WhisperC++ currently doesn't support any media extension other than wav");
    }

    String[] baseCommands = { whispercppExecutable,
        mediaFile.getAbsolutePath(),
        "--model", whispercppModel,
        "-ovtt",
        "-oj",
        "--output-file", preparedOutputFile.getAbsolutePath().replaceFirst("[.][^.]+$", "")};

    List<String> command = new ArrayList<>(Arrays.asList(baseCommands));

    if (whispercppBeamSize.isSome()) {
      command.add("-bs");
      command.add(Integer.toString(whispercppBeamSize.get()));
    }
    if (whispercppMaxLength.isSome()) {
      command.add("-ml");
      command.add(Integer.toString(whispercppMaxLength.get()));
    }
    if (whispercppThreads.isSome()) {
      command.add("-t");
      command.add(Integer.toString(whispercppThreads.get()));
    }
    if (whispercppProcessors.isSome()) {
      command.add("-p");
      command.add(Integer.toString(whispercppProcessors.get()));
    }
    if (whispercppMaxContext.isSome()) {
      command.add("-mc");
      command.add(Integer.toString(whispercppMaxContext.get()));
    }
    if (whispercppSplitOnWord.isSome() && whispercppSplitOnWord.get()) {
      command.add("-sow");
    }
    if (whispercppBestOf.isSome()) {
      command.add("-bo");
      command.add(Integer.toString(whispercppBestOf.get()));
    }
    if (whispercppWordThreshold.isSome()) {
      command.add("-wt");
      command.add(String.format("%f", whispercppWordThreshold.get()));
    }
    if (whispercppEntropyThreshold.isSome()) {
      command.add("-et");
      command.add(String.format("%f", whispercppEntropyThreshold.get()));
    }
    if (whispercppLogProbThreshold.isSome()) {
      command.add("-lpt");
      command.add(String.format("%f", whispercppLogProbThreshold.get()));
    }
    if (whispercppDiarization.isSome() && whispercppDiarization.get()) {
      command.add("-di");
    }
    if (whispercppTinyDiarization.isSome() && whispercppTinyDiarization.get()) {
      command.add("-tdrz");
    }
    if (whispercppNoFallback.isSome() && whispercppNoFallback.get()) {
      command.add("-nf");
    }

    String subtitleLanguage;

    // set language of the source audio if known
    if (!language.isBlank()) {
      logger.info("Using language {} from workflows", language);
      command.add("--language");
      command.add(language);
    } else {
      logger.debug("Auto-detecting language");
      command.add("--language");
      command.add("auto");
    }

    if (translate) {
      command.add("--translate");
      logger.info("Translation enabled");
      subtitleLanguage = "en";
    } else {
      subtitleLanguage = language;
    }


    logger.info("Executing WhisperC++'s transcription command: {}", command);

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
    if (subtitleLanguage.isBlank()) {
      JSONParser jsonParser = new JSONParser();
      try {
        FileReader reader = new FileReader(preparedOutputFile.getAbsolutePath().replaceFirst("[.][^.]+$", "")
            + ".json");
        Object obj = jsonParser.parse(reader);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject result = (JSONObject) jsonObject.get("result");
        subtitleLanguage = (String) result.get("language");
        logger.info("Language detected by WhisperC++: {}", subtitleLanguage);
      } catch (Exception e) {
        logger.info("Error reading WhisperC++ JSON file for: {}", mediaFile);
        throw new SpeechToTextEngineException(e);
      }
    }

    Map<String,Object> returnValues = new HashMap<>();
    returnValues.put("subFile",preparedOutputFile);
    returnValues.put("language",subtitleLanguage);

    return returnValues; // Subtitles data
  }
}
