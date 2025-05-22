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
package org.opencastproject.transcription.amberscript;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.fn.Enrichments;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
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
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component(
    immediate = true,
    service = { TranscriptionService.class,AmberscriptTranscriptionService.class },
    property = {
        "service.description=AmberScript Transcription Service",
        "provider=amberscript"
    }
)
public class AmberscriptTranscriptionService extends AbstractJobProducer implements TranscriptionService {

  private static final Logger logger = LoggerFactory.getLogger(AmberscriptTranscriptionService.class);

  private static final String JOB_TYPE = "org.opencastproject.transcription.amberscript";

  public static final String SUBMISSION_COLLECTION = "amberscript-submission";
  private static final String TRANSCRIPT_COLLECTION = "amberscript-transcripts";

  private static final int CONNECTION_TIMEOUT = 60000; // ms, 1 minute
  private static final int SOCKET_TIMEOUT = 60000; // ms, 1 minute



  private static final String BASE_URL = "https://qs.amberscript.com";
  private static final String STATUS_OPEN = "OPEN";
  private static final String STATUS_DONE = "DONE";
  private static final String STATUS_ERROR = "ERROR";

  private static final String ERROR_NO_SPEECH = "No speech found";

  private static final String PROVIDER = "amberscript";

  private AssetManager assetManager;
  private OrganizationDirectoryService organizationDirectoryService;
  private ScheduledExecutorService scheduledExecutor;
  private SecurityService securityService;
  private ServiceRegistry serviceRegistry;
  private TranscriptionDatabase database;
  private UserDirectoryService userDirectoryService;
  private WorkflowService workflowService;
  private WorkingFileRepository wfr;
  private Workspace workspace;

  // Only used by unit tests
  private Workflows wfUtil;

  private enum Operation {
    StartTranscription
  }

  // Enum for configuring which metadata field shall be used
  // for determining the number of speakers of an event
  private enum SpeakerMetadataField {
    creator, contributor, both
  }

  // service configuration keys
  private static final String ENABLED_CONFIG = "enabled";
  private static final String CLIENT_KEY = "client.key";
  private static final String LANGUAGE = "language";
  private static final String LANGUAGE_FROM_DUBLINCORE = "language.from.dublincore";
  private static final String LANGUAGE_CODE_MAP = "language.code.map";
  private static final String AMBERSCRIPTJOBTYPE = "jobtype";
  private static final String WORKFLOW_CONFIG = "workflow";
  private static final String DISPATCH_WORKFLOW_INTERVAL_CONFIG = "workflow.dispatch.interval";
  private static final String MAX_PROCESSING_TIME_CONFIG = "max.overdue.time";
  private static final String CLEANUP_RESULTS_DAYS_CONFIG = "cleanup.results.days";
  private static final String SPEAKER = "speaker";
  private static final String SPEAKER_FROM_DUBLINCORE = "speaker.from.dublincore";
  private static final String SPEAKER_METADATA_FIELD = "speaker.metadata.field";
  private static final String TRANSCRIPTIONTYPE = "transcriptiontype";
  private static final String GLOSSARY = "glossary";
  private static final String TRANSCRIPTIONSTYLE = "cleanread";
  private static final String TARGETLANGUAGE = "targetlanguage";

  // service configuration default values
  private boolean enabled = false;
  private String clientKey;
  private String language = "en";
  /** determines if the transcription language should be taken from the dublincore */
  private boolean languageFromDublinCore;
  private String amberscriptJobType = "direct";
  private String workflowDefinitionId = "amberscript-attach-transcripts";
  private long workflowDispatchIntervalSeconds = 60;
  private long maxProcessingSeconds = 8 * 24 * 60 * 60; // maximum runtime for jobType perfect is 8 days
  private int cleanupResultDays = 7;
  private int numberOfSpeakers = 1;
  private boolean speakerFromDublinCore = true;
  private SpeakerMetadataField speakerMetadataField = SpeakerMetadataField.creator;
  private String transcriptionType = "transcription";
  private String glossary = "";
  private String transcriptionStyle = "cleanread";
  private String targetLanguage = "";

  /**
   * Contains mappings from several possible ways of writing a language name/code to the
   * corresponding amberscript language code
   */
  private AmberscriptLangUtil amberscriptLangUtil;

  private String systemAccount;

  public AmberscriptTranscriptionService() {
    super(JOB_TYPE);
  }

  @Activate
  public void activate(ComponentContext cc) {

    Option<Boolean> enabledOpt = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), ENABLED_CONFIG);
    if (enabledOpt.isSome()) {
      enabled = enabledOpt.get();
    }

    if (!enabled) {
      logger.info("Amberscript Transcription Service disabled."
              + " If you want to enable it, please update the service configuration.");
      return;
    }

    Option<String> clientKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(), CLIENT_KEY);
    if (clientKeyOpt.isSome()) {
      clientKey = clientKeyOpt.get();
    } else {
      logger.warn("API key was not set.");
      return;
    }

    Option<String> languageOpt = OsgiUtil.getOptCfg(cc.getProperties(), LANGUAGE);
    if (languageOpt.isSome()) {
      language = languageOpt.get();
      logger.info("Default language is set to '{}'.", language);
    } else {
      logger.info("Default language '{}' will be used.", language);
    }

    Option<String> languageFromDublinCoreOpt = OsgiUtil.getOptCfg(cc.getProperties(), LANGUAGE_FROM_DUBLINCORE);
    if (languageFromDublinCoreOpt.isSome()) {
      try {
        languageFromDublinCore = Boolean.parseBoolean(languageFromDublinCoreOpt.get());
      } catch (Exception e) {
        logger.warn("Configuration value for '{}' is invalid, defaulting to false.", LANGUAGE_FROM_DUBLINCORE);
      }
    }
    logger.info("Configuration value for '{}' is set to '{}'.", LANGUAGE_FROM_DUBLINCORE, languageFromDublinCore);

    amberscriptLangUtil = AmberscriptLangUtil.getInstance();
    int customMapEntriesCount = 0;
    Option<String> langCodeMapOpt = OsgiUtil.getOptCfg(cc.getProperties(), LANGUAGE_CODE_MAP);
    if (langCodeMapOpt.isSome()) {
      try {
        String langCodeMapStr = langCodeMapOpt.get();
        if (langCodeMapStr != null) {
          for (String mapping : langCodeMapStr.split(",")) {
            String[] mapEntries = mapping.split(":");
            amberscriptLangUtil.addCustomMapping(mapEntries[0], mapEntries[1]);
            customMapEntriesCount += 1;
          }
        }
      } catch (Exception e) {
        logger.warn("Configuration '{}' is invalid. Using just default mapping.", LANGUAGE_CODE_MAP);
      }
    }
    logger.info("Language code map was set. Added '{}' additional entries.", customMapEntriesCount);

    Option<String> amberscriptJobTypeOpt = OsgiUtil.getOptCfg(cc.getProperties(), AMBERSCRIPTJOBTYPE);
    if (amberscriptJobTypeOpt.isSome()) {
      amberscriptJobType = amberscriptJobTypeOpt.get();
      logger.info("Default Amberscript job type is set to '{}'.", amberscriptJobType);
    } else {
      logger.info("Default Amberscript job type '{}' will be used.", amberscriptJobType);
    }

    Option<String> wfOpt = OsgiUtil.getOptCfg(cc.getProperties(), WORKFLOW_CONFIG);
    if (wfOpt.isSome()) {
      workflowDefinitionId = wfOpt.get();
      logger.info("Workflow is set to '{}'.", workflowDefinitionId);
    } else {
      logger.info("Default workflow '{}' will be used.", workflowDefinitionId);
    }

    Option<String> intervalOpt = OsgiUtil.getOptCfg(cc.getProperties(), DISPATCH_WORKFLOW_INTERVAL_CONFIG);
    if (intervalOpt.isSome()) {
      try {
        workflowDispatchIntervalSeconds = Long.parseLong(intervalOpt.get());
      } catch (NumberFormatException e) {
        logger.warn("Configured '{}' is invalid. Using default.", DISPATCH_WORKFLOW_INTERVAL_CONFIG);
      }
    }
    logger.info("Workflow dispatch interval is {} seconds.", workflowDispatchIntervalSeconds);

    Option<String> maxProcessingOpt = OsgiUtil.getOptCfg(cc.getProperties(), MAX_PROCESSING_TIME_CONFIG);
    if (maxProcessingOpt.isSome()) {
      try {
        maxProcessingSeconds = Long.parseLong(maxProcessingOpt.get());
      } catch (NumberFormatException e) {
        logger.warn("Configured '{}' is invalid. Using default.", MAX_PROCESSING_TIME_CONFIG);
      }
    }
    logger.info("Maximum processing time for transcription job is {} seconds.", maxProcessingSeconds);

    Option<String> cleanupOpt = OsgiUtil.getOptCfg(cc.getProperties(), CLEANUP_RESULTS_DAYS_CONFIG);
    if (cleanupOpt.isSome()) {
      try {
        cleanupResultDays = Integer.parseInt(cleanupOpt.get());
      } catch (NumberFormatException e) {
        logger.warn("Configured '{}' is invalid. Using default.", CLEANUP_RESULTS_DAYS_CONFIG);
      }
    }
    logger.info("Cleanup result files after {} days.", cleanupResultDays);

    Option<String> speakerOpt = OsgiUtil.getOptCfg(cc.getProperties(), SPEAKER);
    if (speakerOpt.isSome()) {
      try {
        numberOfSpeakers = Integer.parseInt(speakerOpt.get());
      } catch (NumberFormatException e) {
        logger.warn("Configured '{}' is invalid. Using default.", SPEAKER);
      }
    }
    logger.info("Default number of speakers is set to '{}'.", numberOfSpeakers);

    Option<String> speakerFromDublinCoreOpt = OsgiUtil.getOptCfg(cc.getProperties(), SPEAKER_FROM_DUBLINCORE);
    if (speakerFromDublinCoreOpt.isSome()) {
      try {
        speakerFromDublinCore = Boolean.parseBoolean(speakerFromDublinCoreOpt.get());
      } catch (Exception e) {
        logger.warn("Configuration value for '{}' is invalid, defaulting to true.", SPEAKER_FROM_DUBLINCORE);
      }
    }
    logger.info("Configuration value for '{}' is set to '{}'.", SPEAKER_FROM_DUBLINCORE, speakerFromDublinCore);

    Option<String> speakerMetadataFieldOpt = OsgiUtil.getOptCfg(cc.getProperties(), SPEAKER_METADATA_FIELD);
    if (speakerMetadataFieldOpt.isSome()) {
      try {
        speakerMetadataField = SpeakerMetadataField.valueOf(speakerMetadataFieldOpt.get());
      } catch (IllegalArgumentException e) {
        logger.warn("Value '{}' is invalid for configuration '{}'. Using default: '{}'.",
            speakerMetadataFieldOpt.get(), SPEAKER_METADATA_FIELD, speakerMetadataField);
      }
    }
    logger.info("Default metadata field for calculating the amount of speakers is set to '{}'.", speakerMetadataField);

    Option<String> transcriptionTypeOpt = OsgiUtil.getOptCfg(cc.getProperties(), TRANSCRIPTIONTYPE);
    if (transcriptionTypeOpt.isSome()) {
      if (List.of("transcription", "captions", "translatedSubtitles").contains(transcriptionType)) {
        transcriptionType = transcriptionTypeOpt.get();
        logger.info("Default transcription type is set to '{}'.", transcriptionType);
      } else {
        logger.warn("Value '{}' is invalid for configuration '{}'. Using default: '{}'.",
            transcriptionTypeOpt.get(), TRANSCRIPTIONTYPE, transcriptionType);
      }
    } else {
      logger.info("Default transcription type '{}' will be used.", transcriptionType);
    }

    Option<String> glossaryOpt = OsgiUtil.getOptCfg(cc.getProperties(), GLOSSARY);
    if (glossaryOpt.isSome()) {
      glossary = glossaryOpt.get();
      logger.info("Default glossary is set to '{}'.", glossary);
    } else {
      logger.info("No glossary will be used by default");
    }

    Option<String> transcriptionStyleOpt = OsgiUtil.getOptCfg(cc.getProperties(), TRANSCRIPTIONSTYLE);
    if (transcriptionStyleOpt.isSome()) {
      if (List.of("cleanread", "verbatim").contains(transcriptionStyle)) {
        transcriptionStyle = transcriptionStyleOpt.get();
        logger.info("Default transcription style is set to '{}'.", transcriptionStyle);
      } else {
        logger.warn("Value '{}' is invalid for configuration '{}'. Using default: '{}'.",
            transcriptionStyleOpt.get(), TRANSCRIPTIONSTYLE, transcriptionStyle);
      }
    } else {
      logger.info("Default transcription style '{}' will be used.", transcriptionStyle);
    }

    Option<String> targetLanguageOpt = OsgiUtil.getOptCfg(cc.getProperties(), TARGETLANGUAGE);
    if (targetLanguageOpt.isSome()) {
      targetLanguage = targetLanguageOpt.get();
      logger.info("Default target language is set to '{}'.", targetLanguage);
    } else {
      logger.info("Transcriptions won't be translated");
    }

    systemAccount = OsgiUtil.getContextProperty(cc, OpencastConstants.DIGEST_USER_PROPERTY);

    scheduledExecutor = Executors.newScheduledThreadPool(2);

    scheduledExecutor.scheduleWithFixedDelay(new WorkflowDispatcher(), 120, workflowDispatchIntervalSeconds,
            TimeUnit.SECONDS);

    scheduledExecutor.scheduleWithFixedDelay(new ResultsFileCleanup(), 1, 1, TimeUnit.DAYS);

    logger.info("Activated.");
  }

  @Deactivate
  public void deactivate() {
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdown();
    }
  }

  @Override
  public Job startTranscription(String mpId, Track track) throws TranscriptionServiceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Job startTranscription(String mpId, Track track, String... args) throws TranscriptionServiceException {
    if (!enabled) {
      throw new TranscriptionServiceException("AmberScript Transcription Service disabled."
              + " If you want to enable it, please update the service configuration.");
    }

    String language = null;

    if (languageFromDublinCore) {
      for (Catalog catalog : track.getMediaPackage().getCatalogs(MediaPackageElements.EPISODE)) {
        try (InputStream in = workspace.read(catalog.getURI())) {
          DublinCoreCatalog dublinCatalog = DublinCores.read(in);
          String dublinCoreLang = dublinCatalog.getFirst(DublinCore.PROPERTY_LANGUAGE);
          if (dublinCoreLang != null) {
            language = amberscriptLangUtil.getLanguageCodeOrNull(dublinCoreLang);
          }
          if (language != null) {
            break;
          }
        } catch (IOException | NotFoundException e) {
          logger.error(String.format("Unable to load dublin core catalog for event '%s'",
              track.getMediaPackage().getIdentifier()), e);
        }
      }
    }

    if (language == null) {
      if (args.length > 0 && StringUtils.isNotBlank(args[0])) {
        language = args[0];
      } else {
        language = getLanguage();
      }
    }

    String jobType;
    if (args.length > 1 && StringUtils.isNotBlank(args[1])) {
      jobType = args[1];
    } else {
      jobType = getAmberscriptJobType();
    }

    int numberOfSpeakers = 0;
    if (speakerFromDublinCore) {
      Set<String> speakers = new HashSet<>();
      for (Catalog catalog : track.getMediaPackage().getCatalogs(MediaPackageElements.EPISODE)) {
        try (InputStream in = workspace.read(catalog.getURI())) {
          DublinCoreCatalog dublinCatalog = DublinCores.read(in);
          if (speakerMetadataField.equals(SpeakerMetadataField.creator)
                  || speakerMetadataField.equals(SpeakerMetadataField.both)) {
            dublinCatalog.get(DublinCore.PROPERTY_CREATOR).stream()
                    .map(DublinCoreValue::getValue).forEach(speakers::add);
          }
          if (speakerMetadataField.equals(SpeakerMetadataField.contributor)
                  || speakerMetadataField.equals(SpeakerMetadataField.both)) {
            dublinCatalog.get(DublinCore.PROPERTY_CONTRIBUTOR).stream()
                    .map(DublinCoreValue::getValue).forEach(speakers::add);
          }

        } catch (IOException | NotFoundException e) {
          logger.error("Unable to load dublin core catalog for event '{}'",
                  track.getMediaPackage().getIdentifier(), e);
        }
      }

      if (speakers.size() >= 1) {
        numberOfSpeakers = speakers.size();
      }
    }

    if (numberOfSpeakers == 0) {
      if (args.length > 2 && StringUtils.isNotBlank(args[2])) {
        numberOfSpeakers = Integer.parseInt(args[2]);
      } else {
        numberOfSpeakers = getNumberOfSpeakers();
      }
    }

    String transcriptionType;
    if (args.length > 3 && StringUtils.isNotBlank(args[3])) {
      transcriptionType = args[3];
    } else {
      transcriptionType = getTranscriptionType();
    }

    String glossary;
    if (args.length > 4 && args[4] != null) {
      glossary = args[4];
    } else {
      glossary = getGlossary();
    }

    String transcriptionStyle;
    if (args.length > 5 && StringUtils.isNotBlank(args[5])) {
      transcriptionStyle = args[5];
    } else {
      transcriptionStyle = getTranscriptionStyle();
    }

    String targetLanguage;
    if (args.length > 6 && args[6] != null) {
      targetLanguage = args[6];
    } else {
      targetLanguage = getTargetLanguage();
    }

    logger.info("New transcription job for mpId '{}' language '{}' job type '{}' speakers '{}' transcription type '{}'"
            + "glossary '{}'.", mpId, language, jobType, numberOfSpeakers, transcriptionType, glossary);

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.StartTranscription.name(), Arrays.asList(
          mpId, MediaPackageElementParser.getAsXml(track), language, jobType, Integer.toString(numberOfSpeakers),
          transcriptionType, glossary, transcriptionStyle, targetLanguage));
    } catch (ServiceRegistryException e) {
      throw new TranscriptionServiceException("Unable to create a job", e);
    } catch (MediaPackageException e) {
      throw new TranscriptionServiceException("Invalid track '" + track.toString() + "'", e);
    }
  }

  @Override
  public void transcriptionDone(String mpId, Object results) { }

  private void transcriptionDone(String mpId, String jobId) {
    try {
      logger.info("Transcription done for mpId '{}'.", mpId);
      if (getAndSaveJobResult(jobId)) {
        database.updateJobControl(jobId, TranscriptionJobControl.Status.TranscriptionComplete.name());
      } else {
        logger.debug("Unable to get and save the transcription result for mpId '{}'.", mpId);
      }
    } catch (IOException | TranscriptionServiceException e) {
      logger.warn("Could not save transcription results file for mpId '{}': {}", mpId, e.toString());
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Transcription results file were saved but state in db not updated for mpId '{}': ", mpId, e);
    }
  }

  @Override
  public void transcriptionError(String mpId, Object obj) throws TranscriptionServiceException {
    JSONObject jsonObj = null;
    String jobId = null;
    try {
      jsonObj = (JSONObject) obj;
      jobId = (String) jsonObj.get("name");
      // Update state in database
      database.updateJobControl(jobId, TranscriptionJobControl.Status.Error.name());
      TranscriptionJobControl jobControl = database.findByJob(jobId);
      logger.warn(String.format("Error received for media package %s, job id %s",
              jobControl.getMediaPackageId(), jobId));
      // Send notification email
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Transcription error. State in db could not be updated to error for mpId {}, jobId {}", mpId, jobId);
      throw new TranscriptionServiceException("Could not update transcription job control db", e);
    }
  }

  @Override
  public Map<String, Object> getReturnValues(String mpId, String jobId) throws TranscriptionServiceException {
    throw new TranscriptionServiceException("Method not implemented");
  }

  @Override
  public String getLanguage() {
    return language;
  }

  public String getAmberscriptJobType() {
    return amberscriptJobType;
  }

  public int getNumberOfSpeakers() {
    return numberOfSpeakers;
  }

  public String getTranscriptionType() {
    return transcriptionType;
  }

  public String getGlossary() {
    return glossary;
  }

  public String getTranscriptionStyle() {
    return transcriptionStyle;
  }

  public String getTargetLanguage() {
    return targetLanguage;
  }

  // Called by workflow
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    String result = "";
    op = Operation.valueOf(operation);
    switch (op) {
      case StartTranscription:
        String mpId = arguments.get(0);
        Track track = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
        String languageCode = arguments.get(2);
        String jobType = arguments.get(3);
        String numberOfSpeakers = arguments.get(4);
        String transcriptionType = arguments.get(5);
        String glossary = arguments.get(6);
        String transcriptionStyle = arguments.get(7);
        String targetLanguage = arguments.get(8);
        createRecognitionsJob(mpId, track, languageCode, jobType, numberOfSpeakers, transcriptionType, glossary,
                transcriptionStyle, targetLanguage);
        break;
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
    }
    return result;
  }

  void createRecognitionsJob(String mpId, Track track, String languageCode, String jobType, String numberOfSpeakers,
          String transcriptionType, String glossary, String transcriptionStyle, String targetLanguage)
          throws TranscriptionServiceException {
    // Timeout 3 hours (needs to include the time for the remote service to
    // fetch the media URL before sending final response)
    CloseableHttpClient httpClient = makeHttpClient(3 * 3600 * 1000);
    CloseableHttpResponse response = null;

    String submitUrl = BASE_URL + "/jobs/upload-media"
            + "?apiKey=" + clientKey
            + "&language=" + languageCode
            + "&jobType=" + jobType
            + "&numberOfSpeakers=" + numberOfSpeakers
            + "&transcriptionType=" + transcriptionType
            + "&transcriptionStyle=" + transcriptionStyle;
    if (StringUtils.isNotBlank(glossary)) {
      submitUrl += "&glossary=" + glossary;
    }
    if (StringUtils.isNotBlank(targetLanguage)) {
      submitUrl += "&targetLanguage=" + targetLanguage;
    }

    try {
      FileBody fileBody = new FileBody(workspace.get(track.getURI()), ContentType.DEFAULT_BINARY);
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      builder.addPart("file", fileBody);
      HttpEntity multipartEntity = builder.build();

      HttpPost httpPost = new HttpPost(submitUrl);
      httpPost.setEntity(multipartEntity);

      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();

      String jsonString = EntityUtils.toString(response.getEntity());
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);

      logger.debug("Submitting new transcription job: {}" + System.lineSeparator()
              + "Response: {}", removePrivateInfo(submitUrl), jsonString);

      JSONObject result = (JSONObject) jsonObject.get("jobStatus");
      String jobId = (String) result.get("jobId");

      switch (code) {
        case HttpStatus.SC_OK: // 200
          logger.info("mp {} has been submitted to AmberScript service with jobId {}.", mpId, jobId);
          database.storeJobControl(mpId, track.getIdentifier(), jobId, TranscriptionJobControl.Status.InProgress.name(),
                  track.getDuration() == null ? 0 : track.getDuration().longValue(), new Date(), PROVIDER);
          EntityUtils.consume(entity);
          return;
        default:
          String error = (String) result.get("error");
          String message = (String) result.get("message");
          String msg = String.format("Unable to submit job: API returned %s - %s: %s", code, error, message);
          logger.warn(msg);
          throw new TranscriptionServiceException(msg);
      }
    } catch (Exception e) {
      logger.warn("Exception when calling the captions endpoint", e);
      throw new TranscriptionServiceException("Exception when calling the captions endpoint", e);
    } finally {
      try {
        httpClient.close();
        if (response != null) {
          response.close();
        }
      } catch (IOException e) {
      }
    }
  }

  boolean checkJobResults(String jobId) throws TranscriptionServiceException {

    String mpId = "unknown";

    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;

    String checkUrl = BASE_URL + "/jobs/status?jobId=" + jobId + "&apiKey=" + clientKey;

    try {
      HttpGet httpGet = new HttpGet(checkUrl);
      response = httpClient.execute(httpGet);
      int code = response.getStatusLine().getStatusCode();

      HttpEntity entity = response.getEntity();
      String jsonString = EntityUtils.toString(entity);
      EntityUtils.consume(entity);

      logger.debug("AmberScript API call was '{}'." + System.lineSeparator() + "Response: {}",
              removePrivateInfo(checkUrl), jsonString);

      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);

      switch (code) {
        case HttpStatus.SC_OK:
          JSONObject result = (JSONObject) jsonObject.get("jobStatus");
          String status = (String) result.get("status");
          switch (status) {
            case STATUS_OPEN:
              logger.debug("Captions job '{}' has not finished yet.", jobId);
              return false;
            case STATUS_ERROR:
              var errorMsg = (String) result.get("errorMsg");
              throw new TranscriptionServiceException(
                      String.format("Captions job '%s' failed: %s", jobId, errorMsg),
                      code,
                      ERROR_NO_SPEECH.equals(errorMsg));
            case STATUS_DONE:
              logger.info("Captions job '{}' has finished.", jobId);
              TranscriptionJobControl jc = database.findByJob(jobId);
              if (jc != null) {
                mpId = jc.getMediaPackageId();
              }
              transcriptionDone(mpId, jobId);
              return true;
            default:
              return false; // only here to obey checkstyle
          }
        default:
          String error = (String) jsonObject.get("error");
          String errorMessage = (String) jsonObject.get("errorMessage");
          logger.warn("Error while checking status: {}."
                  + System.lineSeparator() + "{}: {}", code, error, errorMessage);
          throw new TranscriptionServiceException(
                  String.format("Captions job '%s' failed: Return Code %d", jobId, code), code);
      }
    } catch (TranscriptionDatabaseException | IOException | ParseException e) {
      logger.warn("Error while checking status: {}", e.toString());
    } finally {
      try {
        httpClient.close();
        if (response != null) {
          response.close();
        }
      } catch (IOException e) {
      }
    }
    return false;
  }

  private boolean getAndSaveJobResult(String jobId) throws TranscriptionServiceException, IOException {

    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;

    String transcriptUrl = BASE_URL + "/jobs/export?format=srt&jobId=" + jobId + "&apiKey=" + clientKey;

    boolean done = false;

    try {
      HttpGet httpGet = new HttpGet(transcriptUrl);

      response = httpClient.execute(httpGet);
      int code = response.getStatusLine().getStatusCode();

      logger.debug("AmberScript API {} http response {}", removePrivateInfo(transcriptUrl), code);

      switch (code) {
        case HttpStatus.SC_OK: // 200
          HttpEntity entity = response.getEntity();
          logger.info("Retrieved details for transcription with jobid: '{}'", jobId);

          // Save the result subrip (srt) file into a collection
          workspace.putInCollection(TRANSCRIPT_COLLECTION, jobId + ".srt", entity.getContent());
          done = true;
          break;

        default:
          logger.warn("Error retrieving details for transcription with jobid: '{}', return status: {}.", jobId, code);
          break;
      }
    } catch (Exception e) {
      throw new TranscriptionServiceException(String.format(
              "Exception when calling the transcription service for jobid: %s", jobId), e);
    } finally {
      try {
        httpClient.close();
        if (response != null) {
          response.close();
        }
      } catch (IOException e) {
      }
    }

    return done;
  }

  @Override
  // Called by the attach workflow operation
  public MediaPackageElement getGeneratedTranscription(String mpId, String jobId, MediaPackageElement.Type type)
          throws TranscriptionServiceException {
    try {
      // If jobId is unknown, look for all jobs associated to that mpId
      if (jobId == null || "null".equals(jobId)) {
        jobId = null;
        for (TranscriptionJobControl jc : database.findByMediaPackage(mpId)) {
          if (TranscriptionJobControl.Status.Closed.name().equals(jc.getStatus())
                  || TranscriptionJobControl.Status.TranscriptionComplete.name().equals(jc.getStatus())) {
            jobId = jc.getTranscriptionJobId();
          }
        }
      }

      if (jobId == null) {
        throw new TranscriptionServiceException(
                "No completed or closed transcription job found in database for media package " + mpId);
      }

      // Results already saved?
      URI uri = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, jobId + ".srt");

      logger.info("Looking for transcript at URI: {}", uri);

      try {
        workspace.get(uri);
        logger.info("Found captions at URI: {}", uri);
      } catch (Exception e) {
        logger.info("Results not saved: getting from service for jobId {}", jobId);
        // Not saved yet so call the transcription service to get the results
        checkJobResults(jobId);
      }
      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      logger.debug("Returning MPE with results file URI: {}", uri);
      return builder.elementFromURI(uri, type, new MediaPackageElementFlavor("captions", "srt"));
    } catch (TranscriptionDatabaseException e) {
      throw new TranscriptionServiceException("Job id not informed and could not find transcription", e);
    }
  }

  /**
   * Get mediapackage transcription status
   *
   * @param mpId, mediapackage id
   * @return transcription status
   * @throws TranscriptionServiceException
   */
  public String getTranscriptionStatus(String mpId) throws TranscriptionServiceException {
    try {
      for (TranscriptionJobControl jc : database.findByMediaPackage(mpId)) {
        return jc.getStatus();
      }
    } catch (TranscriptionDatabaseException e) {
      throw new TranscriptionServiceException("Mediapackage id transcription status unknown", e);
    }
    return "Unknown";
  }

  /**
   * Creates a closable http client with default configuration.
   *
   * @return closable http client
   */
  protected CloseableHttpClient makeHttpClient() {
    return makeHttpClient(SOCKET_TIMEOUT);
  }

  /**
   * Creates a closable http client.
   *
   * @param socketTimeout http socket timeout value in milliseconds
   */
  protected CloseableHttpClient makeHttpClient(int socketTimeout) {
    RequestConfig reqConfig = RequestConfig.custom()
        .setConnectTimeout(AmberscriptTranscriptionService.CONNECTION_TIMEOUT)
        .setSocketTimeout(socketTimeout)
        .setConnectionRequestTimeout(AmberscriptTranscriptionService.CONNECTION_TIMEOUT)
        .build();
    CloseableHttpClient httpClient = HttpClientBuilder.create()
        .useSystemProperties()
        .setDefaultRequestConfig(reqConfig)
        .build();
    return httpClient;
  }

  // Called when a transcription job has been submitted
  protected void deleteStorageFile(String filename) {
    try {
      logger.debug("Removing {} from collection {}.", filename, SUBMISSION_COLLECTION);
      wfr.deleteFromCollection(SUBMISSION_COLLECTION, filename, false);
    } catch (IOException e) {
      logger.warn("Unable to remove submission file {} from collection {}", filename, SUBMISSION_COLLECTION);
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
          logger.debug("No jobs yet for provider {}.", PROVIDER);
          return;
        }

        List<TranscriptionJobControl> jobs = database.findByStatus(TranscriptionJobControl.Status.InProgress.name(),
                TranscriptionJobControl.Status.TranscriptionComplete.name());

        for (TranscriptionJobControl j : jobs) {

          // Don't process jobs for other services
          if (j.getProviderId() != providerId) {
            continue;
          }

          String mpId = j.getMediaPackageId();
          String jobId = j.getTranscriptionJobId();

          // If the job in progress, check if it should already have finished.
          if (TranscriptionJobControl.Status.InProgress.name().equals(j.getStatus())) {
            // If job should already have been completed, try to get the results.
            if (j.getDateExpected().getTime() < System.currentTimeMillis()) {
              try {
                if (!checkJobResults(jobId)) {
                  // Job still running, not finished, so check if it should have finished more than N seconds ago
                  if (j.getDateExpected().getTime() + maxProcessingSeconds * 1000 < System.currentTimeMillis()) {
                    // Processing for too long, mark job as canceled and don't check anymore
                    database.updateJobControl(jobId, TranscriptionJobControl.Status.Canceled.name());
                  }
                  // else Job still running, not finished
                  continue;
                }
              } catch (TranscriptionServiceException e) {
                try {
                  database.updateJobControl(jobId, TranscriptionJobControl.Status.Canceled.name());
                  continue;
                } catch (TranscriptionDatabaseException ex) {
                  logger.warn("Could not cancel job '{}'.", jobId);
                }
              }
            } else {
              continue; // Not time to check yet
            }
          }

          // Jobs that get here have state TranscriptionCompleted
          try {
            DefaultOrganization defaultOrg = new DefaultOrganization();
            securityService.setOrganization(defaultOrg);
            securityService.setUser(SecurityUtil.createSystemUser(systemAccount, defaultOrg));

            // Find the episode
            final AQueryBuilder q = assetManager.createQuery();
            final AResult r = q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest())).run();
            if (r.getSize() == 0) {
              logger.warn("Media package {} no longer exists in the asset manager. It was likely deleted. "
                  + "Dropping the generated transcription.", mpId);
              database.updateJobControl(jobId, TranscriptionJobControl.Status.Error.name());
              continue;
            }

            String org = Enrichments.enrich(r).getSnapshots().stream().findFirst().get().getOrganizationId();
            Organization organization = organizationDirectoryService.getOrganization(org);
            if (organization == null) {
              logger.warn("Media package {} has an unknown organization {}. Skipped.", mpId, org);
              continue;
            }
            securityService.setOrganization(organization);

            // Build workflow
            Map<String, String> params = new HashMap<String, String>();
            params.put("transcriptionJobId", jobId);
            WorkflowDefinition wfDef = workflowService.getWorkflowDefinitionById(workflowDefinitionId);

            // Apply workflow
            // wfUtil is only used by unit tests
            Workflows workflows = wfUtil != null ? wfUtil : new Workflows(assetManager, workflowService);
            Set<String> mpIds = new HashSet<String>();
            mpIds.add(mpId);
            List<WorkflowInstance> wfList = workflows
                    .applyWorkflowToLatestVersion(mpIds, ConfiguredWorkflow.workflow(wfDef, params)).toList();
            String wfId = wfList.size() > 0 ? Long.toString(wfList.get(0).getId()) : "Unknown";

            // Update state in the database
            database.updateJobControl(jobId, TranscriptionJobControl.Status.Closed.name());
            logger.info("Attach transcription workflow {} scheduled for mp {}, transcription service job {}",
                    wfId, mpId, jobId);
          } catch (Exception e) {
            logger.warn("Attach transcription workflow could NOT be scheduled for mp {}, amberscript job {}, {}: {}",
                    mpId, jobId, e.getClass().getName(), e.getMessage());
          }
        }
      } catch (TranscriptionDatabaseException e) {
        logger.warn("Could not read transcription job control database: {}", e.getMessage());
      }
    }
  }

  class ResultsFileCleanup implements Runnable {

    @Override
    public void run() {
      logger.info("ResultsFileCleanup waking up...");
      try {
        // Cleans up results files older than CLEANUP_RESULT_FILES_DAYS days
        wfr.cleanupOldFilesFromCollection(TRANSCRIPT_COLLECTION, cleanupResultDays);
        wfr.cleanupOldFilesFromCollection(SUBMISSION_COLLECTION, cleanupResultDays);
      } catch (IOException e) {
        logger.warn("Could not cleanup old submission and transcript results files", e);
      }
    }
  }

  private String removePrivateInfo(String unsafeString) {
    String safeString = unsafeString.replace(clientKey, "__api-key-was-hidden__");
    return safeString;
  }
}
