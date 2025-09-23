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
import org.opencastproject.speechtotext.util.LangCodeUtil;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.OsgiUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
  public static final String WHISPERCPP_EXECUTABLE_DEFAULT_PATH = "whisper-cli";

  /** Currently used path of the WhisperC++ installation. */
  private String whispercppExecutable = WHISPERCPP_EXECUTABLE_DEFAULT_PATH;

  /** Config key for setting whispercpp model */
  private static final String WHISPERCPP_MODEL_CONFIG_KEY = "whispercpp.model";

  /** Default whispercpp model */
  public static final String WHISPERCPP_MODEL_DEFAULT = "/usr/share/whisper.cpp/models/ggml-base.bin";

  /** Currently used whispercpp model */
  private String whispercppModel = WHISPERCPP_MODEL_DEFAULT;

  /** Config key for additional Whisper args */
  private static final String WHISPERCPP_ARGS_CONFIG_KEY = "whispercpp.args";

  /** Currently used Whisper args */
  private String[] whispercppArgs;

  /** Config key for setting whispercpp beam size */
  private static final String WHISPERCPP_BEAM_SIZE_CONFIG_KEY = "whispercpp.beam-size";

  /** Currently used whispercpp beam size */
  private Optional<Integer> whispercppBeamSize;

  /** Config key for setting whispercpp maximum segment length */
  private static final String WHISPERCPP_MAX_LENGTH_CONFIG_KEY = "whispercpp.max-len";

  /** Currently used whispercpp maximum segment length */
  private Optional<Integer> whispercppMaxLength;

  /** Config key for setting whispercpp number of threads */
  private static final String WHISPERCPP_THREADS_CONFIG_KEY = "whispercpp.threads";

  /** Currently used whispercpp number of threads */
  private Optional<Integer> whispercppThreads;

  /** Config key for setting whispercpp number of processors */
  private static final String WHISPERCPP_PROCESSORS_CONFIG_KEY = "whispercpp.processors";

  /** Currently used whispercpp number of processors */
  private Optional<Integer> whispercppProcessors;

  /** Config key for setting whispercpp maximum context */
  private static final String WHISPERCPP_MAX_CONTEXT_CONFIG_KEY = "whispercpp.max-context";

  /** Currently used whispercpp maximum context */
  private Optional<Integer> whispercppMaxContext;

  /** Config key for setting whispercpp split on word */
  private static final String WHISPERCPP_SPLIT_ON_WORD_CONFIG_KEY = "whispercpp.split-on-word";

  /** Currently used whispercpp split on word */
  private Optional<Boolean> whispercppSplitOnWord;

  /** Config key for setting whispercpp number of best candidates to keep */
  private static final String WHISPERCPP_BEST_OF_CONFIG_KEY = "whispercpp.best-of";

  /** Currently used whispercpp number of best candidates to keep */
  private Optional<Integer> whispercppBestOf;

  /** Config key for setting whispercpp word probability threshold */
  private static final String WHISPERCPP_WORD_THRESHOLD_CONFIG_KEY = "whispercpp.word-thold";

  /** Currently used whispercpp word probability threshold */
  private Optional<Double> whispercppWordThreshold;

  /** Config key for setting whispercpp entropy threshold for decoder fail */
  private static final String WHISPERCPP_ENTROPY_THRESHOLD_CONFIG_KEY = "whispercpp.entropy-thold";

  /** Currently used whispercpp entropy threshold for decoder fail */
  private Optional<Double> whispercppEntropyThreshold;

  /** Config key for setting whispercpp log probability threshold for decoder fail */
  private static final String WHISPERCPP_LOG_PROB_THRESHOLD_CONFIG_KEY = "whispercpp.logprob-thold";

  /** Currently used whispercpp log probability threshold for decoder fail */
  private Optional<Double> whispercppLogProbThreshold;

  /** Config key for setting whispercpp diarization */
  private static final String WHISPERCPP_DIARIZATION_CONFIG_KEY = "whispercpp.diarize";

  /** Currently used whispercpp diarization */
  private Optional<Boolean> whispercppDiarization;

  /** Config key for setting whispercpp tinydiarization */
  private static final String WHISPERCPP_TINY_DIARIZATION_CONFIG_KEY = "whispercpp.tinydiarize";

  /** Currently used whispercpp tinydiarization */
  private Optional<Boolean> whispercppTinyDiarization;

  /** Config key for setting whispercpp no fallback */
  private static final String WHISPERCPP_NO_FALLBACK_CONFIG_KEY = "whispercpp.no-fallback";

  /** Currently used whispercpp no fallback */
  private Optional<Boolean> whispercppNoFallback;

  /** Config key for setting whispercpp Voice Activity Detection (VAD) */
  private static final String WHISPERCPP_VAD_CONFIG_KEY = "whispercpp.vad";

  /** Currently used whispercpp Voice Activity Detection (VAD) */
  private Optional<Boolean> whispercppVad;

  /** Config key for setting whispercpp VAD model */
  private static final String WHISPERCPP_VAD_MODEL_CONFIG_KEY = "whispercpp.vad-model";

  /** Currently used whispercpp VAD model */
  private Optional<String> whispercppVadModel;

  /** Config key for setting whispercpp VAD threshold */
  private static final String WHISPERCPP_VAD_THRESHOLD_CONFIG_KEY = "whispercpp.vad-thold";

  /** Currently used whispercpp VAD threshold */
  private Optional<Double> whispercppVadThreshold;

  /** Config key for setting whispercpp VAD min speech duration */
  private static final String WHISPERCPP_VAD_MIN_SPEECH_CONFIG_KEY = "whispercpp.vad-min-speech-dur";

  /** Currently used whispercpp VAD min speech duration */
  private Optional<Integer> whispercppVadMinSpeech;

  /** Config key for setting whispercpp VAD min silence duration */
  private static final String WHISPERCPP_VAD_MIN_SILENCE_CONFIG_KEY = "whispercpp.vad-min-silence-dur";

  /** Currently used whispercpp VAD min silence duration */
  private Optional<Integer> whispercppVadMinSilence;

  /** Config key for setting whispercpp VAD max speech duration */
  private static final String WHISPERCPP_VAD_MAX_SPEECH_CONFIG_KEY = "whispercpp.vad-max-speech-dur";

  /** Currently used whispercpp VAD max speech duration */
  private Optional<Double> whispercppVadMaxSpeech;

  /** Config key for setting whispercpp VAD speech padding */
  private static final String WHISPERCPP_VAD_SPEECH_PADDING_CONFIG_KEY = "whispercpp.vad-speech-pad";

  /** Currently used whispercpp VAD speech padding */
  private Optional<Integer> whispercppVadSpeechPadding;

  /** Config key for setting whispercpp VAD samples overlap */
  private static final String WHISPERCPP_VAD_SAMPLES_OVERLAP_CONFIG_KEY = "whispercpp.vad-samples-overlap";

  /** Currently used whispercpp samples overlap */
  private Optional<Double> whispercppVadSamplesOverlap;

  /** Config key for automatic audio encoding */
  private static final String AUTO_ENCODING_CONFIG_KEY = "whispercpp.auto-encode";

  /** Default value for automatic audio encoding */
  private static final Boolean AUTO_ENCODING_DEFAULT = true;

  /** If Opencast should automatically re-encode tracks so that they are compatible with Whisper.cpp */
  private boolean autoEncode = AUTO_ENCODING_DEFAULT;

  /** The key to look for in the service configuration file to override the DEFAULT_FFMPEG_BINARY */
  public static final String FFMPEG_BINARY_CONFIG_KEY = "org.opencastproject.composer.ffmpeg.path";

  /** The default path to the ffmpeg binary */
  public static final String DEFAULT_FFMPEG_BINARY = "ffmpeg";

  /** Path to the executable */
  protected String ffmpegBinary = DEFAULT_FFMPEG_BINARY;

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
    if (whispercppBeamSize.isPresent()) {
      logger.debug("WhisperC++ beam size set to {}", whispercppBeamSize);
    }

    whispercppMaxLength = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_MAX_LENGTH_CONFIG_KEY);
    if (whispercppMaxLength.isPresent()) {
      logger.debug("WhisperC++ maximum segment length set to {}", whispercppMaxLength);
    }

    whispercppThreads = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_THREADS_CONFIG_KEY);
    if (whispercppThreads.isPresent()) {
      logger.debug("WhisperC++ number of threads set to {}", whispercppThreads);
    }

    whispercppProcessors = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_PROCESSORS_CONFIG_KEY);
    if (whispercppProcessors.isPresent()) {
      logger.debug("WhisperC++ number of processors set to {}", whispercppProcessors);
    }

    whispercppMaxContext = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_MAX_CONTEXT_CONFIG_KEY);
    if (whispercppMaxContext.isPresent()) {
      logger.debug("WhisperC++ max context set to {}", whispercppMaxContext);
    }

    whispercppSplitOnWord = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_SPLIT_ON_WORD_CONFIG_KEY);
    if (whispercppSplitOnWord.isPresent()) {
      logger.debug("WhisperC++ split on word set to {}", whispercppSplitOnWord);
    }

    whispercppBestOf = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_BEST_OF_CONFIG_KEY);
    if (whispercppBestOf.isPresent()) {
      logger.debug("WhisperC++ best of set to {}", whispercppBestOf);
    }

    whispercppWordThreshold = OsgiUtil.getOptCfgAsDouble(cc.getProperties(), WHISPERCPP_WORD_THRESHOLD_CONFIG_KEY);
    if (whispercppWordThreshold.isPresent()) {
      logger.debug("WhisperC++ word threshold set to {}", whispercppWordThreshold);
    }

    whispercppEntropyThreshold = OsgiUtil.getOptCfgAsDouble(
        cc.getProperties(), WHISPERCPP_ENTROPY_THRESHOLD_CONFIG_KEY);
    if (whispercppEntropyThreshold.isPresent()) {
      logger.debug("WhisperC++ entropy threshold set to {}", whispercppEntropyThreshold);
    }

    whispercppLogProbThreshold = OsgiUtil.getOptCfgAsDouble(
        cc.getProperties(), WHISPERCPP_LOG_PROB_THRESHOLD_CONFIG_KEY);
    if (whispercppLogProbThreshold.isPresent()) {
      logger.debug("WhisperC++ log prob threshold set to {}", whispercppLogProbThreshold);
    }

    whispercppDiarization = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_DIARIZATION_CONFIG_KEY);
    if (whispercppDiarization.isPresent()) {
      logger.debug("WhisperC++ diarization set to {}", whispercppDiarization);
    }

    whispercppTinyDiarization = OsgiUtil.getOptCfgAsBoolean(
        cc.getProperties(), WHISPERCPP_TINY_DIARIZATION_CONFIG_KEY);
    if (whispercppTinyDiarization.isPresent()) {
      logger.debug("WhisperC++ tiny diarization set to {}", whispercppTinyDiarization);
    }

    whispercppNoFallback = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_NO_FALLBACK_CONFIG_KEY);
    if (whispercppNoFallback.isPresent()) {
      logger.debug("WhisperC++ no fallback set to {}", whispercppNoFallback);
    }

    whispercppVad = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), WHISPERCPP_VAD_CONFIG_KEY);
    if (whispercppVad.isPresent()) {
      logger.debug("WhisperC++ VAD set to {}", whispercppVad);
    }

    whispercppVadModel = OsgiUtil.getOptCfg(cc.getProperties(), WHISPERCPP_VAD_MODEL_CONFIG_KEY);
    if (whispercppVadModel.isPresent()) {
      logger.debug("WhisperC++ VAD model set to {}", whispercppVadModel);
    }

    whispercppVadThreshold = OsgiUtil.getOptCfgAsDouble(cc.getProperties(), WHISPERCPP_VAD_THRESHOLD_CONFIG_KEY);
    if (whispercppVadThreshold.isPresent()) {
      logger.debug("WhisperC++ VAD threshold set to {}", whispercppVadThreshold);
    }

    whispercppVadMinSpeech = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_VAD_MIN_SPEECH_CONFIG_KEY);
    if (whispercppVadMinSpeech.isPresent()) {
      logger.debug("WhisperC++ VAD min speech set to {}", whispercppVadMinSpeech);
    }

    whispercppVadMinSilence = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_VAD_MIN_SILENCE_CONFIG_KEY);
    if (whispercppVadMinSilence.isPresent()) {
      logger.debug("WhisperC++ VAD min silence set to {}", whispercppVadMinSilence);
    }

    whispercppVadMaxSpeech = OsgiUtil.getOptCfgAsDouble(cc.getProperties(), WHISPERCPP_VAD_MAX_SPEECH_CONFIG_KEY);
    if (whispercppVadMaxSpeech.isPresent()) {
      logger.debug("WhisperC++ VAD max speech set to {}", whispercppVadMaxSpeech);
    }

    whispercppVadSpeechPadding = OsgiUtil.getOptCfgAsInt(cc.getProperties(), WHISPERCPP_VAD_SPEECH_PADDING_CONFIG_KEY);
    if (whispercppVadSpeechPadding.isPresent()) {
      logger.debug("WhisperC++ VAD speech padding set to {}", whispercppVadSpeechPadding);
    }

    whispercppVadSamplesOverlap = OsgiUtil.getOptCfgAsDouble(
        cc.getProperties(), WHISPERCPP_VAD_SAMPLES_OVERLAP_CONFIG_KEY);
    if (whispercppVadSamplesOverlap.isPresent()) {
      logger.debug("WhisperC++ VAD samples overlap set to {}", whispercppVadSamplesOverlap);
    }

    whispercppArgs = Objects.toString(cc.getProperties().get(WHISPERCPP_ARGS_CONFIG_KEY), "").trim().split("\\s+");
    logger.debug("Additional args for WhisperC++: {}", (Object) whispercppArgs);

    autoEncode = BooleanUtils.toBoolean(Objects.toString(
        cc.getProperties().get(AUTO_ENCODING_CONFIG_KEY),
        AUTO_ENCODING_DEFAULT.toString()));
    logger.debug("Automatically convert input media: {}", autoEncode);

    ffmpegBinary = Objects.toString(cc.getBundleContext().getProperty(FFMPEG_BINARY_CONFIG_KEY), DEFAULT_FFMPEG_BINARY);
    logger.debug("ffmpeg binary set to {}", ffmpegBinary);

    logger.debug("Finished activating/updating speech-to-text service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextEngine#generateSubtitlesFile(File, File, String, Boolean)
   */
  @Override
  public Result generateSubtitlesFile(File mediaFile, File workingDirectory, String language, Boolean translate)
          throws SpeechToTextEngineException {

    var whisperInput = mediaFile.getAbsolutePath();
    if (autoEncode) {
      whisperInput = FilenameUtils.concat(workingDirectory.getAbsolutePath(), UUID.randomUUID() + ".wav");
      var ffmpegCommand = List.of(
          ffmpegBinary,
          "-i", mediaFile.getAbsolutePath(),
          "-ar", "16000",
          "-ac", "1",
          "-c:a", "pcm_s16le",
          whisperInput);
      try {
        execCommand(ffmpegCommand);
      } catch (IOException e) {
        throw new SpeechToTextEngineException("Failed to convert audio file", e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    if (!whisperInput.toLowerCase().endsWith(".wav")) {
      throw new SpeechToTextEngineException("WhisperC++ currently doesn't support any media extension other than wav");
    }

    String outputName = FilenameUtils.getBaseName(mediaFile.getAbsolutePath());

    List<String> command = new ArrayList<>(List.of(
        whispercppExecutable,
        whisperInput,
        "--model", whispercppModel,
        "-ovtt",
        "-oj",
        "--output-file", FilenameUtils.concat(workingDirectory.getAbsolutePath(), outputName)));
    if (whispercppBeamSize.isPresent()) {
      command.add("-bs");
      command.add(Integer.toString(whispercppBeamSize.get()));
    }
    if (whispercppMaxLength.isPresent()) {
      command.add("-ml");
      command.add(Integer.toString(whispercppMaxLength.get()));
    }
    if (whispercppThreads.isPresent()) {
      command.add("-t");
      command.add(Integer.toString(whispercppThreads.get()));
    }
    if (whispercppProcessors.isPresent()) {
      command.add("-p");
      command.add(Integer.toString(whispercppProcessors.get()));
    }
    if (whispercppMaxContext.isPresent()) {
      command.add("-mc");
      command.add(Integer.toString(whispercppMaxContext.get()));
    }
    if (whispercppSplitOnWord.isPresent() && whispercppSplitOnWord.get()) {
      command.add("-sow");
    }
    if (whispercppBestOf.isPresent()) {
      command.add("-bo");
      command.add(Integer.toString(whispercppBestOf.get()));
    }
    if (whispercppWordThreshold.isPresent()) {
      command.add("-wt");
      command.add(String.format(Locale.US, "%f", whispercppWordThreshold.get()));
    }
    if (whispercppEntropyThreshold.isPresent()) {
      command.add("-et");
      command.add(String.format(Locale.US, "%f", whispercppEntropyThreshold.get()));
    }
    if (whispercppLogProbThreshold.isPresent()) {
      command.add("-lpt");
      command.add(String.format(Locale.US, "%f", whispercppLogProbThreshold.get()));
    }
    if (whispercppDiarization.isPresent() && whispercppDiarization.get()) {
      command.add("-di");
    }
    if (whispercppTinyDiarization.isPresent() && whispercppTinyDiarization.get()) {
      command.add("-tdrz");
    }
    if (whispercppNoFallback.isPresent() && whispercppNoFallback.get()) {
      command.add("-nf");
    }

    // Optional VAD parameters
    if (whispercppVad.isPresent() && whispercppVad.get()) {
      command.add("--vad");
    }
    if (whispercppVadModel.isPresent()) {
      command.add("-vm");
      command.add(whispercppVadModel.get());
    }
    if (whispercppVadThreshold.isPresent()) {
      command.add("-vt");
      command.add(String.format(Locale.US, "%f", whispercppVadThreshold.get()));
    }
    if (whispercppVadMinSpeech.isPresent()) {
      command.add("-vspd");
      command.add(Integer.toString(whispercppVadMinSpeech.get()));
    }
    if (whispercppVadMinSilence.isPresent()) {
      command.add("-vsd");
      command.add(Integer.toString(whispercppVadMinSilence.get()));
    }
    if (whispercppVadMaxSpeech.isPresent()) {
      command.add("-vmsd");
      command.add(String.format(Locale.US, "%f", whispercppVadMaxSpeech.get()));
    }
    if (whispercppVadSpeechPadding.isPresent()) {
      command.add("-vp");
      command.add(Integer.toString(whispercppVadSpeechPadding.get()));
    }
    if (whispercppVadSamplesOverlap.isPresent()) {
      command.add("-vo");
      command.add(String.format(Locale.US, "%f", whispercppVadSamplesOverlap.get()));
    }

    String subtitleLanguage;

    // set language of the source audio if known
    if (!language.isBlank()) {
      // Convert ISO3 language code to ISO2 if possible, as WhisperC++ expects ISO2 codes.
      // If the conversion is not possible, retain the original language code.
      logger.info("Found language '{}'", language);
      language = LangCodeUtil.iso3ToIso2(language, language);
      logger.info("Using language code '{}' for transcription process", language);
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

    command.addAll(Arrays.asList(whispercppArgs));

    logger.info("Executing WhisperC++'s transcription command: {}", command);

    File vtt;

    try {
      execCommand(command);

      vtt = new File(workingDirectory, outputName + ".vtt");
      if (!vtt.isFile()) {
        throw new SpeechToTextEngineException("WhisperC++ produced no output");
      }
      logger.info("Subtitles file generated successfully: {}", vtt);
    } catch (Exception e) {
      logger.info("Transcription failed closing WhisperC++ transcription process for: {}", whisperInput);
      throw new SpeechToTextEngineException(e);
    }

    // Detect language if not set
    if (subtitleLanguage.isBlank()) {
      JSONParser jsonParser = new JSONParser();
      File json = new File(workingDirectory, outputName + ".json");
      try {
        FileReader reader = new FileReader(json);
        Object obj = jsonParser.parse(reader);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject result = (JSONObject) jsonObject.get("result");
        subtitleLanguage = (String) result.get("language");
        // convert language name to iso3 if necessary or take default
        subtitleLanguage = LangCodeUtil.getIso2FromLang(subtitleLanguage, subtitleLanguage);
        logger.info("Language detected by WhisperC++: {}", subtitleLanguage);
      } catch (Exception e) {
        logger.info("Error reading WhisperC++ JSON file for: {}", mediaFile);
        throw new SpeechToTextEngineException(e);
      } finally {
        FileUtils.deleteQuietly(json);
      }
    }

    return new Result(subtitleLanguage, vtt);
  }

  private void execCommand(List<String> command) throws IOException, InterruptedException, SpeechToTextEngineException {
    logger.info("Executing command: {}", command);
    Process process = null;

    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE)
          .redirectError(ProcessBuilder.Redirect.PIPE)
          .redirectOutput(ProcessBuilder.Redirect.PIPE);
      process = processBuilder.start();

      try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = in.readLine()) != null) { // consume process output
          logger.debug(line);
        }
      }

      // wait until the task is finished
      int exitCode = process.waitFor();
      logger.info("Process finished with exit code {}", exitCode);

      if (exitCode != 0) {
        var error = "";
        try (var errorStream = process.getInputStream()) {
          error = "\n Output:\n" + IOUtils.toString(errorStream, StandardCharsets.UTF_8);
        }
        throw new SpeechToTextEngineException(
            String.format("Process exited abnormally with status %d (command: %s) %s", exitCode, command, error));
      }
    } finally {
      IoSupport.closeQuietly(process);
    }
  }
}
