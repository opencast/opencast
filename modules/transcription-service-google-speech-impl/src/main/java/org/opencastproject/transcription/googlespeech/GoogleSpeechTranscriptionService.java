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
package org.opencastproject.transcription.googlespeech;

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
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GoogleSpeechTranscriptionService extends AbstractJobProducer implements TranscriptionService {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(GoogleSpeechTranscriptionService.class);

  private static final String JOB_TYPE = "org.opencastproject.transcription.googlespeech";

  static final String TRANSCRIPT_COLLECTION = "transcripts";
  static final String TRANSCRIPTION_ERROR = "Transcription ERROR";
  static final String TRANSCRIPTION_JOB_ID_KEY = "transcriptionJobId";
  static final String ACCESS_TOKEN_NAME = "access_token";
  static final String ACCESS_TOKEN_EXPIRY_NAME = "expires_in";
  private static final int CONNECTION_TIMEOUT = 60000; // ms, 1 minute
  private static final int SOCKET_TIMEOUT = 60000; // ms, 1 minute
  private static final int ACCESS_TOKEN_MINIMUM_TIME = 60000; // ms , 1 minute
  // Default workflow to attach transcription results to mediapackage
  public static final String DEFAULT_WF_DEF = "google-speech-attach-transcripts";
  private static final long DEFAULT_COMPLETION_BUFFER = 300; // in seconds, default is 5 minutes
  private static final long DEFAULT_DISPATCH_INTERVAL = 60; // in seconds, default is 1 minute
  private static final long DEFAULT_MAX_PROCESSING_TIME = 5 * 60 * 60; // in seconds, default is 5 hours
  // Cleans up results files that are older than 7 days
  private static final int DEFAULT_CLEANUP_RESULTS_DAYS = 7;
  private static final boolean DEFAULT_PROFANITY_FILTER = false;
  private static final String DEFAULT_LANGUAGE = "en-US";
  private static final String GOOGLE_SPEECH_URL = "https://speech.googleapis.com/v1";
  private static final String GOOGLE_AUTH2_URL = "https://www.googleapis.com/oauth2/v4/token";
  private static final String REQUEST_METHOD = "speech:longrunningrecognize";
  private static final String RESULT_PATH = "operations";
  private static final String INVALID_TOKEN = "-1";
  private static final String PROVIDER = "Google Speech";
  private static final String DEFAULT_ENCODING = "flac";

  // Cluster name
  private String clusterName = "";

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
  public static final String GOOGLE_SPEECH_LANGUAGE = "google.speech.language";
  public static final String PROFANITY_FILTER = "google.speech.profanity.filter";
  public static final String WORKFLOW_CONFIG = "workflow";
  public static final String DISPATCH_WORKFLOW_INTERVAL_CONFIG = "workflow.dispatch.interval";
  public static final String COMPLETION_CHECK_BUFFER_CONFIG = "completion.check.buffer";
  public static final String MAX_PROCESSING_TIME_CONFIG = "max.processing.time";
  public static final String NOTIFICATION_EMAIL_CONFIG = "notification.email";
  public static final String CLEANUP_RESULTS_DAYS_CONFIG = "cleanup.results.days";
  public static final String GOOGLE_CLOUD_CLIENT_ID = "google.cloud.client.id";
  public static final String GOOGLE_CLOUD_CLIENT_SECRET = "google.cloud.client.secret";
  public static final String GOOGLE_CLOUD_REFRESH_TOKEN = "google.cloud.refresh.token";
  public static final String GOOGLE_CLOUD_BUCKET = "google.cloud.storage.bucket";
  public static final String GOOGLE_CLOUD_TOKEN_ENDPOINT_URL = "google.cloud.token.endpoint.url";
  public static final String ENCODING_EXTENSION = "encoding.extension";

  /**
   * Service configuration values
   */
  private boolean enabled = false; // Disabled by default
  private boolean profanityFilter = DEFAULT_PROFANITY_FILTER;
  private String defaultLanguage = DEFAULT_LANGUAGE;
  private String defaultEncoding = DEFAULT_ENCODING;
  private String workflowDefinitionId = DEFAULT_WF_DEF;
  private long workflowDispatchInterval = DEFAULT_DISPATCH_INTERVAL;
  private long completionCheckBuffer = DEFAULT_COMPLETION_BUFFER;
  private long maxProcessingSeconds = DEFAULT_MAX_PROCESSING_TIME;
  private String toEmailAddress;
  private int cleanupResultDays = DEFAULT_CLEANUP_RESULTS_DAYS;
  private String clientId;
  private String clientSecret;
  private String clientToken;
  private String accessToken = INVALID_TOKEN;
  private String tokenEndpoint = GOOGLE_AUTH2_URL;
  private String storageBucket;
  private long tokenExpiryTime = 0;
  private String systemAccount;
  private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

  public GoogleSpeechTranscriptionService() {
    super(JOB_TYPE);
  }

  public void activate(ComponentContext cc) {
    // Has this service been enabled?
    enabled = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), ENABLED_CONFIG).get();
    if (!enabled) {
      logger.info("Service disabled. If you want to enable it, please update the service configuration.");
      return;
    }
    // Mandatory API access properties
    clientId = OsgiUtil.getComponentContextProperty(cc, GOOGLE_CLOUD_CLIENT_ID);
    clientSecret = OsgiUtil.getComponentContextProperty(cc, GOOGLE_CLOUD_CLIENT_SECRET);
    clientToken = OsgiUtil.getComponentContextProperty(cc, GOOGLE_CLOUD_REFRESH_TOKEN);
    storageBucket = OsgiUtil.getComponentContextProperty(cc, GOOGLE_CLOUD_BUCKET);

    // access token endpoint
    Option<String> tokenOpt = OsgiUtil.getOptCfg(cc.getProperties(), GOOGLE_CLOUD_TOKEN_ENDPOINT_URL);
    if (tokenOpt.isSome()) {
      tokenEndpoint = tokenOpt.get();
      logger.info("Access token endpoint is set to {}", tokenEndpoint);
    } else {
      logger.info("Default access token endpoint will be used");
    }

    // profanity filter to use
    Option<String> profanityOpt = OsgiUtil.getOptCfg(cc.getProperties(), PROFANITY_FILTER);
    if (profanityOpt.isSome()) {
      profanityFilter = Boolean.parseBoolean(profanityOpt.get());
      logger.info("Profanity filter is set to {}", profanityFilter);
    } else {
      logger.info("Default profanity filter will be used");
    }
    // Language model to be used
    Option<String> languageOpt = OsgiUtil.getOptCfg(cc.getProperties(), GOOGLE_SPEECH_LANGUAGE);
    if (languageOpt.isSome()) {
      defaultLanguage = languageOpt.get();
      logger.info("Language used is {}", defaultLanguage);
    } else {
      logger.info("Default language will be used");
    }

    // Encoding to be used
    Option<String> encodingOpt = OsgiUtil.getOptCfg(cc.getProperties(), ENCODING_EXTENSION);
    if (encodingOpt.isSome()) {
      defaultEncoding = encodingOpt.get();
      logger.info("Encoding used is {}", defaultEncoding);
    } else {
      logger.info("Default encoding will be used");
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
        logger.warn("Invalid configuration for Workflow dispatch interval. Default used instead: {}", workflowDispatchInterval);
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
        logger.warn("Invalid configuration for maximum processing time. Default used instead: {}", maxProcessingSeconds);
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

    // Schedule the workflow dispatching, starting in 2 minutes
    scheduledExecutor.scheduleWithFixedDelay(new WorkflowDispatcher(), 120, workflowDispatchInterval,
            TimeUnit.SECONDS);

    // Schedule the cleanup of old results jobs from the collection in the wfr once a day
    scheduledExecutor.scheduleWithFixedDelay(new ResultsFileCleanup(), 1, 1, TimeUnit.DAYS);

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

    logger.info("Activated!");
  }

  @Override
  public Job startTranscription(String mpId, Track track, String language) throws TranscriptionServiceException {
    if (!enabled) {
      throw new TranscriptionServiceException(
              "This service is disabled. If you want to enable it, please update the service configuration.");
    }

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.StartTranscription.name(),
              Arrays.asList(mpId, MediaPackageElementParser.getAsXml(track), language));
    } catch (ServiceRegistryException e) {
      throw new TranscriptionServiceException("Unable to create a job", e);
    } catch (MediaPackageException e) {
      throw new TranscriptionServiceException("Invalid track " + track.toString(), e);
    }
  }

  @Override
  public Job startTranscription(String mpId, Track track) throws TranscriptionServiceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public void transcriptionDone(String mpId, Object obj) throws TranscriptionServiceException {
    JSONObject jsonObj = null;
    String jobId = null;
    String token = INVALID_TOKEN;
    try {
      token = getRefreshAccessToken();
    } catch (IOException ex) {
      logger.error("Unable to create access token, error: {}", ex.toString());
    }
    if (token.equals(INVALID_TOKEN)) {
      throw new TranscriptionServiceException("Invalid access token");
    }
    try {
      jsonObj = (JSONObject) obj;
      jobId = (String) jsonObj.get("name");
      logger.info("Transcription done for mpId {}, jobId {}", mpId, jobId);
      JSONArray resultsArray = getTranscriptionResult(jsonObj);

      // Update state in database
      // If there's an optimistic lock exception here, it's ok because the workflow dispatcher
      // may be doing the same thing
      database.updateJobControl(jobId, TranscriptionJobControl.Status.TranscriptionComplete.name());

      // Delete audio file from Google storage
      deleteStorageFile(mpId, token);

      // Save results in file system if they exist
      if (resultsArray != null) {
        saveResults(jobId, jsonObj);
      }
    } catch (IOException e) {
      if (jsonObj == null) {
        logger.warn("Could not save transcription results file for mpId {}, jobId {}: null",
                mpId, jobId);
      } else {
        logger.warn("Could not save transcription results file for mpId {}, jobId {}: {}",
                mpId, jobId, jsonObj.toJSONString());
      }
      throw new TranscriptionServiceException("Could not save transcription results file", e);
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Transcription results file were saved but state in db not updated for mpId {}, jobId {}", mpId,
              jobId);
      throw new TranscriptionServiceException("Could not update transcription job control db", e);
    }
  }

  @Override
  public String getLanguage() {
    return defaultLanguage;
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
    String result = "";
    op = Operation.valueOf(operation);
    switch (op) {
      case StartTranscription:
        String mpId = arguments.get(0);
        Track track = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
        String languageCode = arguments.get(2);
        createRecognitionsJob(mpId, track, languageCode);
        break;
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
    }
    return result;
  }

  /**
   * Asynchronous Requests and Responses call to Google Speech API
   * https://cloud.google.com/speech-to-text/docs/basics
   */
  void createRecognitionsJob(String mpId, Track track, String languageCode) throws TranscriptionServiceException, IOException {
    // Use default defaultlanguage if not set by workflow
    if (StringUtils.isBlank(languageCode)) {
      languageCode = defaultLanguage;
    }
    String audioUrl;
    audioUrl = uploadAudioFileToGoogleStorage(mpId, track);
    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;
    String token = getRefreshAccessToken();
    if (token.equals(INVALID_TOKEN) || audioUrl == null) {
      throw new TranscriptionServiceException("Could not create recognition job. Audio file or access token invalid");
    }

    // Create json for configuration and audio file 
    JSONObject configValues = new JSONObject();
    JSONObject audioValues = new JSONObject();
    JSONObject container = new JSONObject();
    configValues.put("languageCode", languageCode);
    configValues.put("enableWordTimeOffsets", true);
    configValues.put("profanityFilter", profanityFilter);
    audioValues.put("uri", audioUrl);
    container.put("config", configValues);
    container.put("audio", audioValues);

    try {
      HttpPost httpPost = new HttpPost(UrlSupport.concat(GOOGLE_SPEECH_URL, REQUEST_METHOD));
      logger.debug("Url to invoke Google speech service: {}", httpPost.getURI().toString());
      StringEntity params = new StringEntity(container.toJSONString());
      httpPost.addHeader("Authorization", "Bearer " + token); // add the authorization header to the request;
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
      httpPost.setEntity(params);
      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();
      String jsonString = EntityUtils.toString(response.getEntity());
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);

      switch (code) {
        case HttpStatus.SC_OK: // 200
          logger.info("Recognitions job has been successfully created");

          /**
           * Response returned is a json object: { "name":
           * "7612202767953098924", "metadata": { "@type":
           * "type.googleapis.com/google.cloud.speech.v1.LongRunningRecognizeMetadata",
           * "progressPercent": 90, "startTime": "2017-07-20T16:36:55.033650Z",
           * "lastUpdateTime": "2017-07-20T16:37:17.158630Z" } }
           */
          String jobId = (String) jsonObject.get("name");
          logger.info(
                  "Transcription for mp {} has been submitted. Job id: {}", mpId,
                  jobId);

          database.storeJobControl(mpId, track.getIdentifier(), jobId, TranscriptionJobControl.Status.InProgress.name(),
                  track.getDuration() == null ? 0 : track.getDuration().longValue(), null, PROVIDER);
          EntityUtils.consume(entity);
          return;
        default:
          JSONObject errorObj = (JSONObject) jsonObject.get("error");
          logger.warn("Invalid argument returned, status: {} with message: {}", code, (String) errorObj.get("message"));
          break;
      }
      throw new TranscriptionServiceException("Could not create recognition job. Status returned: " + code);
    } catch (Exception e) {
      logger.warn("Exception when calling the recognitions endpoint", e);
      throw new TranscriptionServiceException("Exception when calling the recognitions endpoint", e);
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

  /**
   * Get transcription job result: GET /v1/operations/{name}
   *
   * "response": { "@type":
   * "type.googleapis.com/google.cloud.speech.v1.LongRunningRecognizeResponse",
   * "results": [ { "alternatives": [ { "transcript": "Four score and
   * twenty...", "confidence": 0.97186122, "words": [ { "startTime": "1.300s",
   * "endTime": "1.400s", "word": "Four" }, { "startTime": "1.400s", "endTime":
   * "1.600s", "word": "score" }, { "startTime": "1.600s", "endTime": "1.600s",
   * "word": "and" }, { "startTime": "1.600s", "endTime": "1.900s", "word":
   * "twenty" }, ] } ] }
   */
  boolean getAndSaveJobResults(String jobId) throws TranscriptionServiceException, IOException {
    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;
    String mpId = "unknown";
    JSONArray resultsArray = null;
    String token = getRefreshAccessToken();
    if (token.equals(INVALID_TOKEN)) {
      return false;
    }
    try {
      HttpGet httpGet = new HttpGet(UrlSupport.concat(GOOGLE_SPEECH_URL, RESULT_PATH, jobId));
      logger.debug("Url to invoke Google speech service: {}", httpGet.getURI().toString());
      // add the authorization header to the request;
      httpGet.addHeader("Authorization", "Bearer " + token);
      response = httpClient.execute(httpGet);
      int code = response.getStatusLine().getStatusCode();

      switch (code) {
        case HttpStatus.SC_OK: // 200
          HttpEntity entity = response.getEntity();
          // Response returned is a json object described above
          String jsonString = EntityUtils.toString(entity);
          JSONParser jsonParser = new JSONParser();
          JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);
          Boolean jobDone = (Boolean) jsonObject.get("done");
          TranscriptionJobControl jc = database.findByJob(jobId);
          if (jc != null) {
            mpId = jc.getMediaPackageId();
          }
          if (jobDone) {
            resultsArray = getTranscriptionResult(jsonObject);
          }
          logger.info("Recognitions job {} has been found, completed status {}", jobId, jobDone.toString());
          EntityUtils.consume(entity);

          if (jobDone && resultsArray != null) {
            transcriptionDone(mpId, jsonObject);
            return true;
          }
          return false;
        case HttpStatus.SC_NOT_FOUND: // 404
          logger.warn("Job not found: {}", jobId);
          break;
        case HttpStatus.SC_SERVICE_UNAVAILABLE: // 503
          logger.warn("Service unavailable returned, status: {}", code);
          break;
        default:
          logger.warn("Error return status: {}.", code);
          break;
      }
      throw new TranscriptionServiceException(
              String.format("Could not check recognition job for media package %s, job id %s. Status returned: %d",
                      mpId, jobId, code), code);
    } catch (TranscriptionServiceException e) {
      throw e;
    } catch (Exception e) {
      if (hasTranscriptionRequestExpired(jobId)) {
        // Cancel the job and inform admin
        cancelTranscription(jobId, "Google Transcription job canceled due to errors");
        logger.info("Google Transcription job {} has been canceled. Email notification sent", jobId);
      }
      String msg = String.format("Exception when calling the recognitions endpoint for media package %s, job id %s",
              mpId, jobId);
      logger.warn(msg, e);
      throw new TranscriptionServiceException(String.format(
              "Exception when calling the recognitions endpoint for media package %s, job id %s", mpId, jobId), e);
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

  /**
   * Get transcription result: GET /v1/operations/{name} Method mainly used by
   * the REST endpoint
   *
   * @param jobId
   * @return job details
   * @throws org.opencastproject.transcription.api.TranscriptionServiceException
   * @throws java.io.IOException
   */
  public String getTranscriptionResults(String jobId)
          throws TranscriptionServiceException, IOException {
    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;
    String token = getRefreshAccessToken();
    if (token.equals(INVALID_TOKEN)) {
      logger.warn("Invalid access token");
      return "No results found";
    }
    try {
      HttpGet httpGet = new HttpGet(UrlSupport.concat(GOOGLE_SPEECH_URL, RESULT_PATH, jobId));
      logger.debug("Url to invoke Google speech service: {}", httpGet.getURI().toString());
      // add the authorization header to the request;
      httpGet.addHeader("Authorization", "Bearer " + token);
      response = httpClient.execute(httpGet);
      int code = response.getStatusLine().getStatusCode();

      switch (code) {
        case HttpStatus.SC_OK: // 200
          HttpEntity entity = response.getEntity();
          logger.info("Retrieved details for transcription with job id: '{}'", jobId);
          return EntityUtils.toString(entity);
        default:
          logger.warn("Error retrieving details for transcription with job id: '{}', return status: {}.", jobId, code);
          break;
      }
    } catch (Exception e) {
      logger.warn("Exception when calling the transcription service for job id: {}", jobId, e);
      throw new TranscriptionServiceException(String.format(
              "Exception when calling the transcription service for job id: %s", jobId), e);
    } finally {
      try {
        httpClient.close();
        if (response != null) {
          response.close();
        }
      } catch (IOException e) {
      }
    }
    return "No results found";
  }

  private void saveResults(String jobId, JSONObject jsonObj) throws IOException {
    JSONArray resultsArray = getTranscriptionResult(jsonObj);
    if (resultsArray != null) {
      // Save the results into a collection
      workspace.putInCollection(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId),
              new ByteArrayInputStream(jsonObj.toJSONString().getBytes()));
    }
  }

  @Override
  public MediaPackageElement getGeneratedTranscription(String mpId, String jobId)
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
      URI uri = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId));
      try {
        workspace.get(uri);
      } catch (Exception e) {
        try {
          // Not saved yet so call the google speech service to get the results
          getAndSaveJobResults(jobId);
        } catch (IOException ex) {
          logger.error("Unable to retrieve transcription job, error: {}", ex.toString());
        }
      }
      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      return builder.elementFromURI(uri, Attachment.TYPE, new MediaPackageElementFlavor("captions", "google-speech-json"));
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

  protected CloseableHttpClient makeHttpClient() throws IOException {
    RequestConfig reqConfig = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(SOCKET_TIMEOUT)
            .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
            .build();
    return HttpClients.custom().setDefaultRequestConfig(reqConfig).build();
  }

  protected String refreshAccessToken(String clientId, String clientSecret, String refreshToken)
          throws TranscriptionServiceException, IOException {
    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;

    try {
      HttpPost httpPost = new HttpPost(tokenEndpoint + String.format(
              "?client_id=%s&client_secret=%s&refresh_token=%s&grant_type=refresh_token",
              clientId, clientSecret, refreshToken));
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();
      String jsonString = EntityUtils.toString(response.getEntity());
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);
      switch (code) {
        case HttpStatus.SC_OK: // 200
          accessToken = (String) jsonObject.get(ACCESS_TOKEN_NAME);
          long duration = (long) jsonObject.get(ACCESS_TOKEN_EXPIRY_NAME); // Duration in second
          tokenExpiryTime = (System.currentTimeMillis() + (duration * 1000)); // time in millisecond
          if (!INVALID_TOKEN.equals(accessToken)) {
            logger.info("Google Cloud Service access token created");
            return accessToken;
          }
          throw new TranscriptionServiceException(
              String.format("Created token is invalid. Status returned: %d", code), code);
        case HttpStatus.SC_BAD_REQUEST: // 400
        case HttpStatus.SC_UNAUTHORIZED: // 401
          String error = (String) jsonObject.get("error");
          String errorDetails = (String) jsonObject.get("error_description");
          logger.warn("Invalid argument returned, status: {}", code);
          logger.warn("Unable to refresh Google Cloud Service token, error: {}, error details: {}", error, errorDetails);
          break;
        default:
          logger.warn("Invalid argument returned, status: {}", code);
      }
      throw new TranscriptionServiceException(
              String.format("Could not create Google access token. Status returned: %d", code), code);
    } catch (TranscriptionServiceException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Unable to generate access token for Google Cloud Services");
      return INVALID_TOKEN;
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

  protected String getRefreshAccessToken() throws TranscriptionServiceException, IOException {
    // Check that token hasn't expired
    if ((!INVALID_TOKEN.equals(accessToken)) && (System.currentTimeMillis() < (tokenExpiryTime - ACCESS_TOKEN_MINIMUM_TIME))) {
      return accessToken;
    }
    return refreshAccessToken(clientId, clientSecret, clientToken);
  }

  protected String uploadAudioFileToGoogleStorage(String mpId, Track track)
          throws TranscriptionServiceException, IOException {
    File audioFile;
    String audioUrl = null;
    String fileExtension;
    int audioResponse;
    CloseableHttpClient httpClientStorage = makeHttpClient();
    GoogleSpeechTranscriptionServiceStorage storage = new GoogleSpeechTranscriptionServiceStorage();
    try {
      audioFile = workspace.get(track.getURI());
      fileExtension = FilenameUtils.getExtension(audioFile.getName());
      long fileSize = audioFile.length();
      String contentType = track.getMimeType().toString();
      String token = getRefreshAccessToken();
      // Upload file to google cloud storage
      audioResponse = storage.startUpload(httpClientStorage, storageBucket, mpId, fileExtension,
              audioFile, String.valueOf(fileSize), contentType, token);
      if (audioResponse == HttpStatus.SC_OK) {
        audioUrl = String.format("gs://%s/%s.%s", storageBucket, mpId, fileExtension);
        return audioUrl;
      }
      logger.error("Error when uploading audio to Google Storage, error code: {}", audioResponse);
      return audioUrl;
    } catch (Exception e) {
      throw new TranscriptionServiceException("Error reading audio track", e);
    }
  }

  private JSONArray getTranscriptionResult(JSONObject jsonObj) {
    JSONObject responseObj = (JSONObject) jsonObj.get("response");
    JSONArray resultsArray = (JSONArray) responseObj.get("results");
    return resultsArray;
  }

  protected void deleteStorageFile(String mpId, String token) throws IOException {
    CloseableHttpClient httpClientDel = makeHttpClient();
    GoogleSpeechTranscriptionServiceStorage storage = new GoogleSpeechTranscriptionServiceStorage();
    storage.deleteGoogleStorageFile(httpClientDel, storageBucket, mpId + "." + defaultEncoding, token);
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

  private String buildResultsFileName(String jobId) {
    return PathSupport.toSafeName(jobId + ".json");
  }

  private void cancelTranscription(String jobId, String message) {
    try {
      database.updateJobControl(jobId, TranscriptionJobControl.Status.Canceled.name());
      String mpId = database.findByJob(jobId).getMediaPackageId();
      try {
        // Delete file stored on Google storage
        String token = getRefreshAccessToken();
        deleteStorageFile(mpId, token);
      } catch (Exception ex) {
        logger.warn(String.format("could not delete file %s.%s from Google cloud storage", mpId, defaultEncoding), ex);
      } finally {
        // Send notification email
        sendEmail("Transcription ERROR", String.format("%s(media package %s, job id %s).", message, mpId, jobId));
      }
    } catch (Exception e) {
      logger.error(String.format("ERROR while deleting transcription job: %s", jobId), e);
    }
  }

  private boolean hasTranscriptionRequestExpired(String jobId) {
    try {
      // set a time limit based on video duration and maximum processing time
      if (database.findByJob(jobId).getDateCreated().getTime() + database.findByJob(jobId).getTrackDuration()
              + (completionCheckBuffer + maxProcessingSeconds) * 1000 < System.currentTimeMillis()) {
        return true;
      }
    } catch (Exception e) {
      logger.error(String.format("ERROR while calculating transcription request expiration for job: %s", jobId), e);
      // to avoid perpetual non-expired state, transcription is set as expired
      return true;
    }
    return false;
  }

  private long getRemainingTranscriptionExpireTimeInMin(String jobId) {
    try {
      long expiredTime = (database.findByJob(jobId).getDateCreated().getTime() + database.findByJob(jobId).getTrackDuration()
              + (completionCheckBuffer + maxProcessingSeconds) * 1000) - (System.currentTimeMillis());
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

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public void setSmtpService(SmtpService service) {
    this.smtpService = service;
  }

  public void setWorkspace(Workspace ws) {
    this.workspace = ws;
  }

  public void setWorkingFileRepository(WorkingFileRepository wfr) {
    this.wfr = wfr;
  }

  public void setDatabase(TranscriptionDatabase service) {
    this.database = service;
  }

  public void setAssetManager(AssetManager service) {
    this.assetManager = service;
  }

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
            // If job should already have been completed, try to get the results. Consider a buffer factor so that we
            // don't try it too early. Results normally should be ready half of the time of the track duration.
            // The completionCheckBuffer can be used to delay results check.
            if (j.getDateCreated().getTime() + (j.getTrackDuration() / 2) + completionCheckBuffer * 1000 < System
                    .currentTimeMillis()) {
              try {
                if (!getAndSaveJobResults(jobId)) {
                  // Job still running, not finished, so check if it should have finished more than N seconds ago
                  if (hasTranscriptionRequestExpired(jobId)) {
                    // Processing for too long, mark job as cancelled and don't check anymore
                    database.updateJobControl(jobId, TranscriptionJobControl.Status.Canceled.name());
                    // Delete file stored on Google storage
                    String token = getRefreshAccessToken();
                    deleteStorageFile(mpId, token);
                    // Send notification email
                    sendEmail(TRANSCRIPTION_ERROR, String.format(
                            "Transcription job was in processing state for too long and was marked as cancelled (media package %s, job id %s).",
                            mpId, jobId));
                  }
                  // else Job still running, not finished
                  continue;
                }
              } catch (TranscriptionServiceException e) {
                if (e.getCode() == 404) {
                  // Job not found there, update job state to canceled
                  database.updateJobControl(jobId, TranscriptionJobControl.Status.Canceled.name());
                  // Send notification email
                  sendEmail(TRANSCRIPTION_ERROR,
                          String.format("Transcription job was not found (media package %s, job id %s).", mpId, jobId));
                }
                continue; // Skip this one, exception was already logged
              } catch (IOException ex) {
                logger.error("Transcription job not found, error: {}", ex.toString());
              }
            } else {
              continue; // Not time to check yet
            }
          }

          // Jobs that get here have state TranscriptionCompleted or had an IOException]
          try {

            // Apply workflow to attach transcripts
            Map<String, String> params = new HashMap<String, String>();
            params.put(TRANSCRIPTION_JOB_ID_KEY, jobId);
            String wfId = startWorkflow(mpId, workflowDefinitionId, jobId, params);
            if (wfId == null) {
              logger.warn("Attach transcription workflow could NOT be scheduled for mp {}, google speech job {}", mpId, jobId);
              continue;
            }
            // Update state in the database
            database.updateJobControl(jobId, TranscriptionJobControl.Status.Closed.name());
            logger.info("Attach transcription workflow {} scheduled for mp {}, google speech job {}",
                    wfId, mpId, jobId);
          } catch (Exception e) {
            logger.warn("Attach transcription workflow could NOT be scheduled for mp {}, google speech job {}, {}: {}",
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
                + "more minutes before cancelling transcription job {}.", mpId, getRemainingTranscriptionExpireTimeInMin(jobId), jobId);
      } else {
        // Close transcription job and email admin
        cancelTranscription(jobId, " Google Transcription job canceled, archived media package not found");
        logger.info("Google Transcription job {} has been canceled. Email notification sent", jobId);
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
