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
package org.opencastproject.transcription.microsoftazure;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.fn.Enrichments;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.transcription.api.TranscriptionServiceException;
import org.opencastproject.transcription.persistence.TranscriptionDatabase;
import org.opencastproject.transcription.persistence.TranscriptionDatabaseException;
import org.opencastproject.transcription.persistence.TranscriptionJobControl;
import org.opencastproject.transcription.persistence.TranscriptionProviderControl;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig;
import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageResult;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.PhraseListGrammar;
import com.microsoft.cognitiveservices.speech.ProfanityOption;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamContainerFormat;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component(
        immediate = true,
        service = { TranscriptionService.class, MicrosoftAzureTranscriptionService.class },
        property = {
                "service.description=Microsoft Azure Transcription Service",
                "provider=microsoft.azure"
        }
)
public class MicrosoftAzureTranscriptionService extends AbstractJobProducer implements TranscriptionService {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureTranscriptionService.class);

  private static final String JOB_TYPE = "org.opencastproject.transcription.microsoftazure";

  static final String TRANSCRIPT_COLLECTION = "transcripts";
  static final String TRANSCRIPTION_ERROR = "Transcription ERROR";
  static final String TRANSCRIPTION_JOB_ID_KEY = "transcriptionJobId";
  // Default workflow to attach transcription results to mediapackage
  public static final String DEFAULT_WF_DEF = "microsoft-azure-attach-transcripts";
  private static final long DEFAULT_COMPLETION_BUFFER = 300; // in seconds, default is 5 minutes
  private static final long DEFAULT_DISPATCH_INTERVAL = 60; // in seconds, default is 1 minute
  private static final long DEFAULT_MAX_PROCESSING_TIME = 5 * 60 * 60; // in seconds, default is 5 hours
  // Cleans up results files that are older than 7 days
  private static final int DEFAULT_CLEANUP_RESULTS_DAYS = 7;
  private static final String DEFAULT_LANGUAGE = "en-US";
  private static final String PROVIDER = "Microsoft Azure";
  private static final AudioStreamContainerFormat DEFAULT_ENCODING = AudioStreamContainerFormat.ANY;
  private static final ProfanityOption DEFAULT_PROFANITY_OPTION = ProfanityOption.Raw;
  private static final boolean DEFAULT_IS_AUTO_DETECT_LANGUAGE = false;

  // Cluster name
  private String clusterName = "";

  /** Return values keys */
  private static final String DETECTED_LANGUAGE = "autoDetectedLanguage";

  /**
   * Service dependencies
   */
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;
  private UserDirectoryService userDirectoryService;
  private OrganizationDirectoryService organizationDirectoryService;
  private Workspace workspace;
  private TranscriptionDatabase database;
  private AssetManager assetManager;
  private WorkflowService workflowService;
  private WorkingFileRepository wfr;
  private SmtpService smtpService;

  // Only used by unit tests!
  private Workflows wfUtil;

  private enum Operation {
    StartTranscription
  }

  /**
   * Service configuration options
   */
  public static final String ENABLED_CONFIG = "enabled";
  public static final String LANGUAGE = "language";
  public static final String PROFANITY_OPTION = "profanity.option";
  public static final String USE_SUBRIP_FORMAT = "use.subrip.format";
  public static final String PHRASES_LIST = "phrases.list";
  public static final String SUBSCRIPTION_KEY = "subscription.key";
  public static final String REGION = "region";
  public static final String WORKFLOW_CONFIG = "workflow";
  public static final String DISPATCH_WORKFLOW_INTERVAL_CONFIG = "workflow.dispatch.interval";
  public static final String COMPLETION_CHECK_BUFFER_CONFIG = "completion.check.buffer";
  public static final String MAX_PROCESSING_TIME_CONFIG = "max.processing.time";
  public static final String NOTIFICATION_EMAIL_CONFIG = "notification.email";
  public static final String CLEANUP_RESULTS_DAYS_CONFIG = "cleanup.results.days";
  public static final String ENCODING_EXTENSION = "encoding.extension";
  public static final String IS_AUTO_DETECT_LANGUAGE = "auto.detect.language";
  public static final String AUTO_DETECT_LANGUAGES = "auto.detect.languages";
  public static final String SPLIT_TEXT = "split.text";
  public static final String SPLIT_TEXT_LINE_SIZE = "split.text.line.size";

  /**
   * Service configuration values
   */
  private boolean enabled = false; // Disabled by default
  private String subscriptionKey;
  private String region;
  private String defaultLanguage = DEFAULT_LANGUAGE;
  private String workflowDefinitionId = DEFAULT_WF_DEF;
  private long workflowDispatchInterval = DEFAULT_DISPATCH_INTERVAL;
  private long completionCheckBuffer = DEFAULT_COMPLETION_BUFFER;
  private long maxProcessingSeconds = DEFAULT_MAX_PROCESSING_TIME;
  private String toEmailAddress;
  private int cleanupResultDays = DEFAULT_CLEANUP_RESULTS_DAYS;
  private String systemAccount;
  private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

  private AudioStreamContainerFormat compressedAudioFormat = DEFAULT_ENCODING;
  private ProfanityOption profanityOption = DEFAULT_PROFANITY_OPTION;
  private List<String> phraseList = new ArrayList<>();
  private boolean useSubRipTextCaptionFormat = false;
  private String fileFormat = "vtt";
  private boolean defaultIsAutoDetectLanguage = DEFAULT_IS_AUTO_DETECT_LANGUAGE;
  private List<String> defaultAutoDetectLanguages = new ArrayList<>();
  private boolean splitText = false;
  private int splitTextLineSize = 100;

  public MicrosoftAzureTranscriptionService() {
    super(JOB_TYPE);
  }

  @Activate
  public void activate(ComponentContext cc) {
    // Has this service been enabled?
    enabled = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), ENABLED_CONFIG).get();
    if (!enabled) {
      logger.info("Service disabled. If you want to enable it, please update the service configuration.");
      return;
    }
    // Mandatory API access properties
    subscriptionKey = OsgiUtil.getComponentContextProperty(cc, SUBSCRIPTION_KEY);
    region = OsgiUtil.getComponentContextProperty(cc, REGION);
    if (subscriptionKey == null) {
      logger.error("SubscriptionKey not set, disabling service.");
      return;
    }
    if (region == null) {
      logger.error("Region not set, disabling service.");
      return;
    }

    // profanity filter to use
    Option<String> profanityOpt = OsgiUtil.getOptCfg(cc.getProperties(), PROFANITY_OPTION);
    if (profanityOpt.isSome()) {
      try {
        profanityOption = ProfanityOption.valueOf(profanityOpt.get());
        logger.info("Profanity filter is set to {}", profanityOption);
      } catch (IllegalArgumentException e) {
        logger.error("Profanity filter set to illegal value, disabling service.");
        return;
      }
    } else {
      logger.info("Default profanity filter will be used: {}", profanityOption);
    }

    // Vtt or Subrip
    Option<String> useSubRipOpt = OsgiUtil.getOptCfg(cc.getProperties(), USE_SUBRIP_FORMAT);
    if (useSubRipOpt.isSome()) {
      useSubRipTextCaptionFormat = Boolean.valueOf(useSubRipOpt.get());
      logger.info("Subrip caption format in use: {}", useSubRipTextCaptionFormat);
    } else {
      logger.info("Default '{}' format will be used", useSubRipTextCaptionFormat);
    }

    if (useSubRipTextCaptionFormat) {
      fileFormat = "srt";
    }

    // Phrases to be used
    Option<String> phrasesOpt = OsgiUtil.getOptCfg(cc.getProperties(), PHRASES_LIST);
    if (phrasesOpt.isSome()) {
      phraseList = new ArrayList<>(Arrays.asList(phrasesOpt.get().split(",")));
      logger.info("Phrases added to recognition: {}", phraseList);
    } else {
      logger.info("No additional phrases defined");
    }

    // Language model to be used
    Option<String> languageOpt = OsgiUtil.getOptCfg(cc.getProperties(), LANGUAGE);
    if (languageOpt.isSome()) {
      defaultLanguage = languageOpt.get();
      logger.info("Language used is {}", defaultLanguage);
    } else {
      logger.info("Default '{}' language will be used during recognition.", defaultLanguage);
    }

    // Encoding to be used
    Option<String> encodingOpt = OsgiUtil.getOptCfg(cc.getProperties(), ENCODING_EXTENSION);
    if (encodingOpt.isSome()) {
      compressedAudioFormat = AudioStreamContainerFormat.valueOf(encodingOpt.get());
      logger.info("Audio encoding configured as {}", compressedAudioFormat);
    } else {
      logger.info("Default '{}' audio encoding will be used", compressedAudioFormat);
    }

    // If language should be detected automatically
    Option<String> isAutoDetectLanguageOpt = OsgiUtil.getOptCfg(cc.getProperties(), IS_AUTO_DETECT_LANGUAGE);
    if (isAutoDetectLanguageOpt.isSome()) {
      defaultIsAutoDetectLanguage = Boolean.valueOf(isAutoDetectLanguageOpt.get());
    }
    logger.info("Automatically detecting language is globally enabled: {}", defaultIsAutoDetectLanguage);

    // Which languages should be used in automatic language detection
    Option<String> autoDetectLanguagesOpt = OsgiUtil.getOptCfg(cc.getProperties(), AUTO_DETECT_LANGUAGES);
    if (autoDetectLanguagesOpt.isSome()) {
      defaultAutoDetectLanguages = new ArrayList<>(Arrays.asList(autoDetectLanguagesOpt.get().split(",")));
      if (defaultIsAutoDetectLanguage
              && (defaultAutoDetectLanguages.size() == 0 || defaultAutoDetectLanguages.size() > 4)) {
        throw new ConfigurationException("When using automatic language detection, the list of languages must contain"
                + "at least one language and at most four languages");
      }
      logger.info("Languages for auto detection: {}", defaultAutoDetectLanguages);
    } else {
      logger.info("No languages for auto detection defined");
    }

    // If text should be split into multiple cues
    Option<String> isSplitTextOpt = OsgiUtil.getOptCfg(cc.getProperties(), SPLIT_TEXT);
    if (isSplitTextOpt.isSome()) {
      splitText = Boolean.valueOf(isSplitTextOpt.get());
    }

    // When splitting text into multiple cues, how many characters should each cue have at most
    Option<String> splitTextLineSizeOpt = OsgiUtil.getOptCfg(cc.getProperties(), SPLIT_TEXT_LINE_SIZE);
    if (splitTextLineSizeOpt.isSome()) {
      try {
        splitTextLineSize = Integer.parseInt(splitTextLineSizeOpt.get());
      } catch (NumberFormatException e) {
        throw new ConfigurationException("Invalid configuration for split text line size. Please check your"
                + "configuration");
      }
    }

    if (splitText) {
      logger.info("Long text will be split at {} characters", splitTextLineSize);
    }

    // Workflow to execute when getting callback (optional, with default)
    Option<String> wfOpt = OsgiUtil.getOptCfg(cc.getProperties(), WORKFLOW_CONFIG);
    if (wfOpt.isSome()) {
      workflowDefinitionId = wfOpt.get();
    }
    logger.info("Workflow definition is {}", workflowDefinitionId);
    // Interval to check for completed transcription jobs and start workflows to attach transcripts
    Option<String> intervalOpt = OsgiUtil.getOptCfg(cc.getProperties(), DISPATCH_WORKFLOW_INTERVAL_CONFIG);
    if (intervalOpt.isSome()) {
      try {
        workflowDispatchInterval = Long.parseLong(intervalOpt.get());
      } catch (NumberFormatException e) {
        // Use default
        logger.warn("Invalid configuration for Workflow dispatch interval. Default used instead: {}",
                workflowDispatchInterval);
      }
    }
    logger.info("Workflow dispatch interval is {} seconds", workflowDispatchInterval);
    // How long to wait after a transcription is supposed to finish before starting checking
    Option<String> bufferOpt = OsgiUtil.getOptCfg(cc.getProperties(), COMPLETION_CHECK_BUFFER_CONFIG);
    if (bufferOpt.isSome()) {
      try {
        completionCheckBuffer = Long.parseLong(bufferOpt.get());
      } catch (NumberFormatException e) {
        // Use default
        logger.warn("Invalid configuration for {} : {}. Default used instead: {}",
                new Object[]{COMPLETION_CHECK_BUFFER_CONFIG, bufferOpt.get(), completionCheckBuffer});
      }
    }
    logger.info("Completion check buffer is {} seconds", completionCheckBuffer);
    // How long to wait after a transcription is supposed to finish before marking the job as canceled in the db
    Option<String> maxProcessingOpt = OsgiUtil.getOptCfg(cc.getProperties(), MAX_PROCESSING_TIME_CONFIG);
    if (maxProcessingOpt.isSome()) {
      try {
        maxProcessingSeconds = Long.parseLong(maxProcessingOpt.get());
      } catch (NumberFormatException e) {
        // Use default
        logger.warn("Invalid configuration for maximum processing time. Default used instead: {}",
                maxProcessingSeconds);
      }
    }
    logger.info("Maximum time a job is checked after it should have ended is {} seconds", maxProcessingSeconds);
    // How long to keep result files in the working file repository
    Option<String> cleaupOpt = OsgiUtil.getOptCfg(cc.getProperties(), CLEANUP_RESULTS_DAYS_CONFIG);
    if (cleaupOpt.isSome()) {
      try {
        cleanupResultDays = Integer.parseInt(cleaupOpt.get());
      } catch (NumberFormatException e) {
        // Use default
        logger.warn("Invalid configuration for clean up days. Default used instead: {}", cleanupResultDays);
      }
    }
    logger.info("Cleanup result files after {} days", cleanupResultDays);

    systemAccount = OsgiUtil.getContextProperty(cc, OpencastConstants.DIGEST_USER_PROPERTY);

    // Notification email passed in this service configuration?
    Option<String> optTo = OsgiUtil.getOptCfg(cc.getProperties(), NOTIFICATION_EMAIL_CONFIG);
    if (optTo.isSome()) {
      toEmailAddress = optTo.get();
    } else {
      // Use admin email informed in custom.properties
      optTo = OsgiUtil.getOptContextProperty(cc, OpencastConstants.ADMIN_EMAIL_PROPERTY);
      if (optTo.isSome()) {
        toEmailAddress = optTo.get();
      }
    }
    if (toEmailAddress != null) {
      logger.info("Notification email set to {}", toEmailAddress);
    } else {
      logger.warn("Email notification disabled");
    }

    Option<String> optCluster = OsgiUtil.getOptContextProperty(cc, OpencastConstants.ENVIRONMENT_NAME_PROPERTY);
    if (optCluster.isSome()) {
      clusterName = optCluster.get();
    }
    logger.info("Environment name is {}", clusterName);

    // Schedule the workflow dispatching, starting in 2 minutes
    scheduledExecutor.scheduleWithFixedDelay(new WorkflowDispatcher(), 120, workflowDispatchInterval,
            TimeUnit.SECONDS);

    // Schedule the cleanup of old results jobs from the collection in the wfr once a day
    scheduledExecutor.scheduleWithFixedDelay(new ResultsFileCleanup(), 1, 1, TimeUnit.DAYS);

    logger.info("Activated!");
  }

  private AudioConfig getAudioConfig(String filePath) {
    AudioStreamFormat format = AudioStreamFormat.getCompressedFormat(compressedAudioFormat);
    BinaryFileReader callback = new BinaryFileReader(filePath);
    AudioInputStream stream = AudioInputStream.createPullStream(callback, format);
    return AudioConfig.fromStreamInput(stream);
  }

  private SpeechConfig getSpeechConfig(String languageCode) {
    final SpeechConfig speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);

    speechConfig.setSpeechRecognitionLanguage(languageCode);
    speechConfig.setProfanity(profanityOption);
    speechConfig.setProperty(PropertyId.SpeechServiceResponse_PostProcessingOption, "TrueText");

    return speechConfig;
  }

  @Override
  public Job startTranscription(String mpId, Track track, String... args) throws TranscriptionServiceException {
    if (!enabled) {
      throw new TranscriptionServiceException(
              "This service is disabled. If you want to enable it, please update the service configuration.");
    }

    if (args.length != 3) {
      throw new IllegalArgumentException("Must provide three arguments: language, autoDetect, autoDetectLanguages. Any"
              + "of these arguments may be an empty string.");
    }

    String language = args[0];
    String autoDetect = args[1];
    String autoDetectLanguages = args[2];

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.StartTranscription.name(),
              Arrays.asList(mpId, MediaPackageElementParser.getAsXml(track), language, autoDetect,
                      autoDetectLanguages));
    } catch (ServiceRegistryException e) {
      throw new TranscriptionServiceException("Unable to create a job", e);
    } catch (MediaPackageException e) {
      throw new TranscriptionServiceException("Invalid track " + track.toString(), e);
    }
  }

  @Override
  public Job startTranscription(String mpId, Track track) throws TranscriptionServiceException {
    return startTranscription(
            mpId,
            track,
            defaultLanguage,
            Boolean.toString(defaultIsAutoDetectLanguage),
            String.join(", ", defaultAutoDetectLanguages));
  }

  @Override
  public String getLanguage() {
    return defaultLanguage;
  }

  @Override
  public void transcriptionError(String mpId, Object obj) throws TranscriptionServiceException {
    String jobId = null;
    try {
      jobId = (String) obj;
      // Update state in database
      database.updateJobControl(jobId, TranscriptionJobControl.Status.Error.name());
      TranscriptionJobControl jobControl = database.findByJob(jobId);
      logger.warn("Error received for media package {}, job id {}",
              jobControl.getMediaPackageId(), jobId);
      // Send notification email
      sendEmail(TRANSCRIPTION_ERROR,
              String.format("There was a transcription error for for media package %s, job id %s.",
                      jobControl.getMediaPackageId(), jobId));
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Transcription error. State in db could not be updated to error for mpId {}, jobId {}", mpId, jobId);
      throw new TranscriptionServiceException("Could not update transcription job control db", e);
    }
  }

  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    op = Operation.valueOf(operation);
    switch (op) {
      case StartTranscription:
        String mpId = arguments.get(0);
        Track track = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
        String languageCode = arguments.get(2);
        String autoDetect = arguments.get(3);
        String autoDetectLanguages = arguments.get(4);
        runTranscriptionJob(mpId, track, Long.toString(job.getId()), languageCode, autoDetect, autoDetectLanguages);
        break;
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
    }
    return "";
  }

  /**
   * Setup speech recognition
   */
  void runTranscriptionJob(String mpId, Track track, String jobId, String languageCode, String isAutoDetectString,
          String isAutoDetectLanguagesString)
          throws TranscriptionServiceException {
    boolean isAutoDetectLanguage;
    List<String> autoDetectLanguages;
    if (StringUtils.isBlank(languageCode)) {
      languageCode = defaultLanguage;
    }
    if (StringUtils.isBlank(isAutoDetectString)) {
      isAutoDetectLanguage = defaultIsAutoDetectLanguage;
    } else {
      isAutoDetectLanguage = Boolean.valueOf(isAutoDetectString);
    }
    if (StringUtils.isBlank(isAutoDetectLanguagesString)) {
      autoDetectLanguages = defaultAutoDetectLanguages;
    } else {
      autoDetectLanguages = new ArrayList<>(Arrays.asList(isAutoDetectLanguagesString.split(",")));
      if (defaultIsAutoDetectLanguage
              && (defaultAutoDetectLanguages.size() == 0 || defaultAutoDetectLanguages.size() > 4)) {
        throw new TranscriptionServiceException("When using automatic language detection, the list of languages must"
                + " contain at least one language and at most four languages");
      }
    }

    try {
      final AudioConfig audioConfig = getAudioConfig(workspace.get(track.getURI()).getPath());
      final SpeechConfig speechConfig = getSpeechConfig(languageCode);
      final SpeechRecognizer speechRecognizer;

      if (isAutoDetectLanguage) {
        AutoDetectSourceLanguageConfig autoDetectSourceLanguageConfig = AutoDetectSourceLanguageConfig
                .fromLanguages(autoDetectLanguages);
        speechRecognizer = new SpeechRecognizer(speechConfig, autoDetectSourceLanguageConfig, audioConfig);
      } else {
        speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
      }

      PhraseListGrammar grammar = PhraseListGrammar.fromRecognizer(speechRecognizer);
      for (String phrase : phraseList) {
        grammar.addPhrase(phrase);
      }

      recognizeContinuous(speechRecognizer, jobId, mpId, track, isAutoDetectLanguage);
    } catch (ExecutionException | NotFoundException | IOException e) {
      throw new TranscriptionServiceException(e.getMessage());
    } catch (InterruptedException e) {
      throw new TranscriptionServiceException(e.getMessage());
    }
  }

  /**
   * Track speech recognizing progress through event listeners
   */
  void recognizeContinuous(SpeechRecognizer speechRecognizer, String jobId, String mpId, Track track,
          boolean isAutoDetectLanguage)
          throws ExecutionException, InterruptedException {
    // This lets us modify local variables from inside a lambda.
    final int[] sequenceNumber = new int[] { 0 };
    final boolean[] languageDetected = new boolean[] { false };

    speechRecognizer.sessionStarted.addEventListener((s, e) -> {
      logger.info("Transcription job {} for media package {} started.", jobId, mpId);
      try {
        database.storeJobControl(mpId, track.getIdentifier(), jobId, TranscriptionJobControl.Status.InProgress.name(),
            track.getDuration() == null ? 0 : track.getDuration().longValue(), null, PROVIDER);
        createTranscriptFile(jobId);
      } catch (TranscriptionDatabaseException ex) {
        errorCallback("Could not store job: " + ex, speechRecognizer, jobId, mpId);
      } catch (IOException ex) {
        errorCallback("Unable to create transcription file: " + ex, speechRecognizer, jobId, mpId);
      }
    });

    // Fired when an utterance is completely recognized
    speechRecognizer.recognized.addEventListener((s, e) -> {
      SpeechRecognitionResult result = e.getResult();
      if (ResultReason.RecognizedSpeech == result.getReason() && result.getText().length() > 0) {
        sequenceNumber[0]++;
        List<String> texts = captionFromSpeechRecognitionResult(sequenceNumber[0], e.getResult());
        sequenceNumber[0] += texts.size() - 1;

        for (String text : texts) {
          try {
            writeTranscriptToFile(text, jobId);
          }  catch (NotFoundException ex) {
            errorCallback("Transcription file not found: " + ex, speechRecognizer, jobId, mpId);
          } catch (IOException ex) {
            errorCallback("Unable to write to transcription file: " + ex, speechRecognizer, jobId, mpId);
          }
        }

        if (!languageDetected[0] && isAutoDetectLanguage) {
          AutoDetectSourceLanguageResult autoDetectSourceLanguageResult = AutoDetectSourceLanguageResult.fromResult(
              result);
          if (autoDetectSourceLanguageResult != null
              && StringUtils.isNotBlank(autoDetectSourceLanguageResult.getLanguage())) {
            try {
              workspace.putInCollection(TRANSCRIPT_COLLECTION, getTranscriptLanguageFileName(jobId),
                  new ByteArrayInputStream(autoDetectSourceLanguageResult.getLanguage()
                      .getBytes(StandardCharsets.UTF_8)));
              languageDetected[0] = true;
            } catch (IOException ex) {
              errorCallback("Unable to write to transcription language file: " + ex, speechRecognizer,
                  jobId, mpId);
            }
          }
        }
      }
      else if (ResultReason.NoMatch == e.getResult().getReason()) {
        logger.debug("NOMATCH: Speech could not be recognized by transcription job {}.", jobId);
      }
    });

    // Fired when speech recognizing is cancelled, including due to successfully terminating
    speechRecognizer.canceled.addEventListener((s, e) -> {
      String errorMessage = null;
      if (CancellationReason.EndOfStream == e.getReason()) {
        // This is expected
        logger.debug("End of stream reached for transcription job {}", jobId);
        return;
      }
      else if (CancellationReason.CancelledByUser == e.getReason()) {
        errorMessage = "User canceled request.";
      }
      else if (CancellationReason.Error == e.getReason()) {
        errorMessage = String.format("Encountered error.%sError code: %s%sError details: %s",
                System.lineSeparator(), e.getErrorCode().name(), System.lineSeparator(), e.getErrorDetails());
      }
      else {
        errorMessage = String.format("Request was cancelled for an unrecognized reason: %d.", e.getReason());
      }

      errorCallback(errorMessage, speechRecognizer, jobId, mpId);
    });

    speechRecognizer.sessionStopped.addEventListener((s, e) -> {
      try {
        logger.info("Transcription job {} for media package {} ended.", jobId, mpId);
        speechRecognizer.stopContinuousRecognitionAsync().get();
        // Update state in database
        // If there's an optimistic lock exception here, it's ok because the workflow dispatcher
        // may be doing the same thing
        database.updateJobControl(jobId, TranscriptionJobControl.Status.TranscriptionComplete.name());
      } catch (InterruptedException | TranscriptionDatabaseException | ExecutionException ex) {
        errorCallback("Could not save transcription results file: " + ex, speechRecognizer, jobId, mpId);
      }
    });

    speechRecognizer.startContinuousRecognitionAsync().get();
  }

  /**
   * Create transcription file.
   *
   * @param jobId transcription uniq job ID
   * @return URI to created transcription file
   * @throws IOException unable to create transcript file due to IO error
   */
  private URI createTranscriptFile(String jobId) throws IOException {
    String transcript = "";
    if (!useSubRipTextCaptionFormat) {
      transcript = String.format("WEBVTT%s%s", System.lineSeparator(), System.lineSeparator());
    }
    logger.trace("Create transcript for transcription job {}", jobId);
    return workspace.putInCollection(TRANSCRIPT_COLLECTION, getTranscriptFileName(jobId),
        new ByteArrayInputStream(transcript.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Write transcript to file.
   *
   * @param text transcript
   * @param jobId transcription uniq job ID
   * @throws NotFoundException transcription file could not be created
   * @throws IOException unable to write transcript to file due to IO error
   */
  private void writeTranscriptToFile(String text, String jobId) throws NotFoundException, IOException {
    URI collectionURI = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, getTranscriptFileName(jobId));
    File transcriptFile = workspace.get(collectionURI);
    logger.trace("Write transcript to {}: {}", transcriptFile.getAbsolutePath(), text);
    try (FileWriter transcriptWriter = new FileWriter(transcriptFile, StandardCharsets.UTF_8,true)) {
      transcriptWriter.write(text);
    }
  }

  /**
   * Generate safe file name for a transcript.
   *
   * @param jobId transcription uniq job ID
   * @return safe file name for a transcription file
   */
  private String getTranscriptFileName(String jobId) {
    return workspace.toSafeName("transcript_" + jobId + "." + fileFormat);
  }

  /**
   * Generate safe file name for a transcript language.
   *
   * @param jobId transcription uniq job ID
   * @return safe file name for a transcription language file
   */
  private String getTranscriptLanguageFileName(String jobId) {
    return workspace.toSafeName("transcript_lang_" + jobId + ".txt");
  }

  /**
   * Creates a WebVTT cue string from the given result that can be directly appended to a WebVTT file.
   * If splitText is enabled, will split a given result into multiple cues based on line size.
   *
   * @param sequenceNumber number of the cue. Used for SubRip.
   * @param result A recognized text
   * @return A list containing at least one WebVTT cue as it would be written in a file.
   */
  private List<String> captionFromSpeechRecognitionResult(int sequenceNumber, SpeechRecognitionResult result) {
    List<String> lines;
    List<BigInteger> offsets;
    List<BigInteger> durations;

    if (splitText) {
      lines = splitText(result.getText());
      Map<String, List<BigInteger>> offsetsAndDurations = splitTextTimes(lines, result.getOffset(),
              result.getDuration());
      offsets = offsetsAndDurations.get("offsets");
      durations = offsetsAndDurations.get("durations");
    } else {
      lines = Collections.singletonList(result.getText());
      offsets = Collections.singletonList(result.getOffset());
      durations = Collections.singletonList(result.getDuration());
    }

    List<String> formattedLines = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      StringBuilder caption = new StringBuilder();

      if (useSubRipTextCaptionFormat) {
        caption.append(String.format("%d%s", sequenceNumber + i, System.lineSeparator()));
      }
      caption.append(String.format("%s%s", timestampFromSpeechRecognitionResult(offsets.get(i), durations.get(i)),
              System.lineSeparator()));
      caption.append(String.format("%s%s%s", lines.get(i), System.lineSeparator(), System.lineSeparator()));

      formattedLines.add(caption.toString());
    }

    return formattedLines;
  }

  // Split text into multiple lines if too long
  private List<String> splitText(String text) {
    int previousSplitLength = 0;
    int nextSplitLength = splitTextLineSize;
    List<String> lines = new ArrayList();

    do {
      // Search ahead to next whitespace or end of text
      int index;
      for (index = nextSplitLength; index < text.length(); index++) {
        if (text.charAt(index) == ' ') {
          break;
        }
      }

      if (index >= text.length()) {
        index = text.length() - 1;
      }

      lines.add(text.substring(previousSplitLength, index).trim());

      previousSplitLength = index;
      nextSplitLength = index + splitTextLineSize;
    } while (text.length() > previousSplitLength + 1);

    return lines;
  }

  // If text was split into multiple lines, divide time between them
  private Map<String, List<BigInteger>> splitTextTimes(List<String> lines, BigInteger offset, BigInteger duration) {
    List<BigInteger> offsets = new ArrayList<>();
    List<BigInteger> durations = new ArrayList<>();
    BigInteger percentage = BigInteger.ZERO;
    int lengthOfAllLines = lines.stream()
            .mapToInt(l -> l.length())
            .sum();

    for (int i = 0; i < lines.size(); i++) {
      BigInteger previousDurations = durations.stream().reduce(BigInteger.ZERO, BigInteger::add);
      offsets.add(offset.add(previousDurations));

      // Divide total duration fairly between lines, based on char count
      percentage = BigDecimal.valueOf((double) lines.get(i).length() / lengthOfAllLines)
              .multiply(new BigDecimal(duration))
              .toBigInteger();

      durations.add(percentage);
    }

    Map<String,List<BigInteger>> map = new HashMap();
    map.put("offsets", offsets);
    map.put("durations", durations);
    return map;
  }

  private String timestampFromSpeechRecognitionResult(BigInteger offset, BigInteger duration) {
    final BigInteger ticksPerMillisecond = BigInteger.valueOf(10000);
    final Date startTime = new Date(offset.divide(ticksPerMillisecond).longValue());
    final Date endTime = new Date((offset
            .add(duration)).divide(ticksPerMillisecond).longValue());

    String format = "";
    if (useSubRipTextCaptionFormat) {
      // SRT format requires ',' as decimal separator rather than '.'.
      format = "HH:mm:ss,SSS";
    }
    else {
      format = "HH:mm:ss.SSS";
    }
    SimpleDateFormat formatter = new SimpleDateFormat(format);
    // If we don't do this, the time is adjusted for our local time zone, which we don't want.
    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    return String.format("%s --> %s", formatter.format(startTime), formatter.format(endTime));
  }

  private void errorCallback(String errorMessage, SpeechRecognizer speechRecognizer, String jobId, String mpId) {
    try {
      logger.error(errorMessage);

      if (speechRecognizer != null) {
        speechRecognizer.stopContinuousRecognitionAsync().get();
      }

      transcriptionError(mpId, jobId);
    } catch (InterruptedException | ExecutionException | TranscriptionServiceException interruptedException) {
      // We're already in an error state anyway
      logger.error("Error in error state: " + interruptedException);
    }
  }

  @Override
  public MediaPackageElement getGeneratedTranscription(String mpId, String jobId)
          throws TranscriptionServiceException {
    try {
      // If jobId is unknown, look for all jobs associated to that mpId
      if (jobId == null || "null".equals(jobId)) {
        jobId = getTranscriptionJobId(mpId);
      }

      // Results already saved?
      URI uri = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, getTranscriptFileName(jobId));
      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      return builder.elementFromURI(uri, Attachment.TYPE, new MediaPackageElementFlavor("captions",
              "microsoft-azure"));
    } catch (TranscriptionDatabaseException e) {
      throw new TranscriptionServiceException("Job id not informed and could not find transcription", e);
    }
  }

  public Map<String, Object> getReturnValues(String mpId, String jobId)
          throws TranscriptionServiceException {
    try {
      // If jobId is unknown, look for all jobs associated to that mpId
      if (jobId == null || "null".equals(jobId)) {
        jobId = getTranscriptionJobId(mpId);
      }

      // Results already saved?
      URI uri = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, getTranscriptLanguageFileName(jobId));
      File languageFile = workspace.get(uri);
      String language = null;
      try (BufferedReader reader = new BufferedReader(
          new FileReader(languageFile.getAbsoluteFile(), StandardCharsets.UTF_8))) {
        language = reader.readLine();
      }
      Map<String, Object> returnValues = new HashMap<>();
      returnValues.put(DETECTED_LANGUAGE, language);
      return returnValues;
    } catch (TranscriptionDatabaseException | FileNotFoundException e) {
      throw new TranscriptionServiceException("Job id not informed and could not find transcription", e);
    } catch (IOException | NotFoundException e) {
      throw new TranscriptionServiceException("Error getting file from workspace", e);
    }
  }

  @Override
  public void transcriptionDone(String mpId, Object obj) throws TranscriptionServiceException {
    logger.info("transcriptionDone not implemented");
  }

  private String getTranscriptionJobId(String mpId) throws TranscriptionServiceException,
          TranscriptionDatabaseException {
    // If jobId is unknown, look for all jobs associated to that mpId
    String jobId = null;
    for (TranscriptionJobControl jc : database.findByMediaPackage(mpId)) {
      if (TranscriptionJobControl.Status.Closed.name().equals(jc.getStatus())
              || TranscriptionJobControl.Status.TranscriptionComplete.name().equals(jc.getStatus())) {
        jobId = jc.getTranscriptionJobId();
      }
    }

    if (jobId == null) {
      throw new TranscriptionServiceException(
              "No completed or closed transcription job found in database for media package " + mpId);
    }

    return jobId;
  }

  private void sendEmail(String subject, String body) {
    if (toEmailAddress == null) {
      logger.info("Skipping sending email notification. Message is {}.", body);
      return;
    }
    try {
      logger.debug("Sending e-mail notification to {}", toEmailAddress);
      smtpService.send(toEmailAddress, String.format("%s (%s)", subject, clusterName), body);
      logger.info("Sent e-mail notification to {}", toEmailAddress);
    } catch (Exception e) {
      logger.error("Could not send email: {}\n{}", subject, body, e);
    }
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Reference
  public void setSmtpService(SmtpService service) {
    this.smtpService = service;
  }

  @Reference
  public void setWorkspace(Workspace ws) {
    this.workspace = ws;
  }

  @Reference
  public void setWorkingFileRepository(WorkingFileRepository wfr) {
    this.wfr = wfr;
  }

  @Reference
  public void setDatabase(TranscriptionDatabase service) {
    this.database = service;
  }

  @Reference
  public void setAssetManager(AssetManager service) {
    this.assetManager = service;
  }

  @Reference
  public void setWorkflowService(WorkflowService service) {
    this.workflowService = service;
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  // Only used by unit tests!
  void setWfUtil(Workflows wfUtil) {
    this.wfUtil = wfUtil;
  }

  class WorkflowDispatcher implements Runnable {

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      logger.debug("WorkflowDispatcher waking up...");

      try {
        // Find jobs that are in progress and jobs that had transcription complete
        long providerId;
        TranscriptionProviderControl providerInfo = database.findIdByProvider(PROVIDER);
        if (providerInfo != null) {
          providerId = providerInfo.getId();
        } else {
          logger.debug("No jobs yet for provider {}", PROVIDER);
          return;
        }

        List<TranscriptionJobControl> jobs = database.findByStatus(
                TranscriptionJobControl.Status.TranscriptionComplete.name());
        for (TranscriptionJobControl j : jobs) {

          // Don't process jobs for other services
          if (j.getProviderId() != providerId) {
            continue;
          }

          String mpId = j.getMediaPackageId();
          String jobId = j.getTranscriptionJobId();

          try {
            // Apply workflow to attach transcripts
            Map<String, String> params = new HashMap<String, String>();
            params.put(TRANSCRIPTION_JOB_ID_KEY, jobId);
            String wfId = startWorkflow(mpId, workflowDefinitionId, jobId, params);
            if (wfId == null) {
              logger.warn("Attach transcription workflow could NOT be scheduled for mp {}, microsoft azure job {}",
                      mpId, jobId);
              continue;
            }
            // Update state in the database
            database.updateJobControl(jobId, TranscriptionJobControl.Status.Closed.name());
            logger.info("Attach transcription workflow {} scheduled for mp {}, microsoft azure job {}",
                    wfId, mpId, jobId);
          } catch (Exception e) {
            logger.warn("Attach transcription workflow could NOT be scheduled for mp {},"
                            + "microsoft azure job {}, {}: {}",
                    mpId, jobId, e.getClass().getName(), e.getMessage());
          }
        }
      } catch (TranscriptionDatabaseException e) {
        logger.warn("Could not read transcription job control database: {}", e.getMessage());
      }
    }
  }

  private String startWorkflow(String mpId, String wfDefId, String jobId, Map<String, String> params) {
    DefaultOrganization defaultOrg = new DefaultOrganization();
    securityService.setOrganization(defaultOrg);
    securityService.setUser(SecurityUtil.createSystemUser(systemAccount, defaultOrg));

    // Find the episode
    final AQueryBuilder q = assetManager.createQuery();
    final AResult r = q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest())).run();
    if (r.getSize() == 0) {
      if (!hasTranscriptionRequestExpired(jobId)) {
        // Media package not archived but still within completion time? Skip until next time.
        logger.warn("Media package {} has not been archived yet or has been deleted. Will keep trying for {} "
                + "more minutes before cancelling transcription job {}."
                    ,mpId, getRemainingTranscriptionExpireTimeInMin(jobId), jobId);
      } else {
        // Close transcription job and email admin
        cancelTranscription(jobId, " Microsoft Azure Transcription job canceled, archived media package not found");
        logger.info("Microsoft Azure Transcription job {} has been canceled. Email notification sent", jobId);
      }
      return null;
    }

    String org = Enrichments.enrich(r).getSnapshots().head2().getOrganizationId();
    Organization organization = null;
    try {
      organization = organizationDirectoryService.getOrganization(org);
      if (organization == null) {
        logger.warn("Media package {} has an unknown organization {}.", mpId, org);
        return null;
      }
    } catch (NotFoundException e) {
      logger.warn("Organization {} not found for media package {}.", org, mpId);
      return null;
    }
    securityService.setOrganization(organization);

    try {
      WorkflowDefinition wfDef = workflowService.getWorkflowDefinitionById(wfDefId);
      Workflows workflows;
      if (wfUtil != null) {
        workflows = wfUtil;
      } else {
        workflows = new Workflows(assetManager, workflowService);
      }
      Set<String> mpIds = new HashSet<String>();
      mpIds.add(mpId);
      List<WorkflowInstance> wfList = workflows
              .applyWorkflowToLatestVersion(mpIds, ConfiguredWorkflow.workflow(wfDef, params)).toList();
      return wfList.size() > 0 ? Long.toString(wfList.get(0).getId()) : null;
    } catch (NotFoundException | WorkflowDatabaseException e) {
      logger.warn("Could not get workflow definition: {}", wfDefId);
    }

    return null;
  }

  private boolean hasTranscriptionRequestExpired(String jobId) {
    try {
      // set a time limit based on video duration and maximum processing time
      if (database.findByJob(jobId).getDateCreated().getTime() + database.findByJob(jobId).getTrackDuration()
              + (completionCheckBuffer + maxProcessingSeconds) * 1000 < System.currentTimeMillis()) {
        return true;
      }
    } catch (Exception e) {
      logger.error("ERROR while calculating transcription request expiration for job: %s", jobId, e);
      // to avoid perpetual non-expired state, transcription is set as expired
      return true;
    }
    return false;
  }

  private long getRemainingTranscriptionExpireTimeInMin(String jobId) {
    try {
      long expiredTime = (database.findByJob(jobId).getDateCreated().getTime()
              + database.findByJob(jobId).getTrackDuration()
              + (completionCheckBuffer + maxProcessingSeconds) * 1000)
              - (System.currentTimeMillis());
      // Transcription has expired
      if (expiredTime < 0) {
        expiredTime = 0;
      }
      return TimeUnit.MILLISECONDS.toMinutes(expiredTime);
    } catch (Exception e) {
      logger.error("Unable to calculate remaining transcription expired time for transcription job {}", jobId);
    }
    return 0;
  }

  private void cancelTranscription(String jobId, String message) {
    try {
      database.updateJobControl(jobId, TranscriptionJobControl.Status.Canceled.name());
      String mpId = database.findByJob(jobId).getMediaPackageId();

      sendEmail("Transcription ERROR", String.format("%s(media package %s, job id %s).", message, mpId, jobId));
    } catch (Exception e) {
      logger.error("ERROR while deleting transcription job: %s", jobId, e);
    }
  }

  class ResultsFileCleanup implements Runnable {

    @Override
    public void run() {
      logger.info("ResultsFileCleanup waking up...");
      try {
        // Cleans up results files older than CLEANUP_RESULT_FILES_DAYS days
        wfr.cleanupOldFilesFromCollection(TRANSCRIPT_COLLECTION, cleanupResultDays);
      } catch (IOException e) {
        logger.warn("Could not cleanup old transcript results files", e);
      }
    }
  }

}
