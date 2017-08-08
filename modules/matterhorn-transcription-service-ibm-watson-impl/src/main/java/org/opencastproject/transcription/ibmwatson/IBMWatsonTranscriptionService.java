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
package org.opencastproject.transcription.ibmwatson;

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
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.transcription.api.TranscriptionServiceException;
import org.opencastproject.transcription.ibmwatson.persistence.TranscriptionDatabase;
import org.opencastproject.transcription.ibmwatson.persistence.TranscriptionDatabaseException;
import org.opencastproject.transcription.ibmwatson.persistence.TranscriptionJobControl;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

public class IBMWatsonTranscriptionService extends AbstractJobProducer implements TranscriptionService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IBMWatsonTranscriptionService.class);

  private static final String JOB_TYPE = "org.opencastproject.transcription.ibmwatson";

  static final String TRANSCRIPT_COLLECTION = "transcripts";
  private static final int CONNECTION_TIMEOUT = 60000; // ms, 1 minute
  private static final int SOCKET_TIMEOUT = 60000; // ms, 1 minute
  // Default wf to attach transcription results to mp
  public static final String DEFAULT_WF_DEF = "attach-watson-transcripts";
  private static final long DEFAULT_COMPLETION_BUFFER = 600; // in seconds, default is 10 minutes
  private static final long DEFAULT_DISPATCH_INTERVAL = 60; // in seconds, default is 1 minute
  private static final long DEFAULT_MAX_PROCESSING_TIME = 2 * 60 * 60; // in seconds, default is 2 hours
  // Cleans up results files that are older than 7 days (which is how long the IBM watson
  // speech-to-text-service keeps the jobs by default)
  private static final int DEFAULT_CLEANUP_RESULTS_DAYS = 7;

  // Global configuration (custom.properties)
  public static final String ADMIN_URL_PROPERTY = "org.opencastproject.admin.ui.url";
  private static final String ADMIN_EMAIL_PROPERTY = "org.opencastproject.admin.email";
  private static final String DIGEST_USER_PROPERTY = "org.opencastproject.security.digest.user";

  // Cluster name
  private static final String CLUSTER_NAME_PROPERTY = "org.opencastproject.environment.name";
  private String clusterName = "";

  // The events we are interested in receiving notifications
  public interface JobEvent {
    String COMPLETED_WITH_RESULTS = "recognitions.completed_with_results";
    String FAILED = "recognitions.failed";
  }

  /** Service dependencies */
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

  private static final String IBM_WATSON_SERVICE_URL = "https://stream.watsonplatform.net/speech-to-text/api/v1/";
  private static final String REGISTER_CALLBACK = "register_callback";
  private static final String RECOGNITIONS = "recognitions";
  private static final String CALLBACK_PATH = "/transcripts/watson/results";

  /** Service configuration options */
  public static final String ENABLED_CONFIG = "enabled";
  public static final String IBM_WATSON_USER_CONFIG = "ibm.watson.user";
  public static final String IBM_WATSON_PSW_CONFIG = "ibm.watson.password";
  public static final String IBM_WATSON_MODEL = "ibm.watson.model";
  public static final String WORKFLOW_CONFIG = "workflow";
  public static final String DISPATCH_WORKFLOW_INTERVAL_CONFIG = "workflow.dispatch.interval";
  public static final String COMPLETION_CHECK_BUFFER_CONFIG = "completion.check.buffer";
  public static final String MAX_PROCESSING_TIME_CONFIG = "max.processing.time";
  public static final String NOTIFICATION_EMAIL_CONFIG = "notification.email";
  public static final String CLEANUP_RESULTS_DAYS_CONFIG = "cleanup.results.days";

  /** Service configuration values */
  private boolean enabled = false; // Disabled by default
  private String user;
  private String psw;
  private String model;
  private String workflowDefinitionId = DEFAULT_WF_DEF;
  private long workflowDispatchInterval = DEFAULT_DISPATCH_INTERVAL;
  private long completionCheckBuffer = DEFAULT_COMPLETION_BUFFER;
  private long maxProcessingSeconds = DEFAULT_MAX_PROCESSING_TIME;
  private String toEmailAddress;
  private int cleanupResultDays = DEFAULT_CLEANUP_RESULTS_DAYS;

  private String systemAccount;
  private String serverUrl;
  private String callbackUrl;
  private boolean callbackAlreadyRegistered = false;
  private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

  public IBMWatsonTranscriptionService() {
    super(JOB_TYPE);
  }

  public void activate(ComponentContext cc) {
    if (cc != null) {
      // Has this service been enabled?
      enabled = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), ENABLED_CONFIG).get();

      if (enabled) {
        // User name (mandatory)
        user = OsgiUtil.getComponentContextProperty(cc, IBM_WATSON_USER_CONFIG);
        // Password (mandatory)
        psw = OsgiUtil.getComponentContextProperty(cc, IBM_WATSON_PSW_CONFIG);
        logger.info("Using transcription service at {} with username {}", IBM_WATSON_SERVICE_URL, user);

        // Language model to be used
        Option<String> modelOpt = OsgiUtil.getOptCfg(cc.getProperties(), IBM_WATSON_MODEL);
        if (modelOpt.isSome()) {
          model = modelOpt.get();
          logger.info("Model is {}", model);
        } else {
          logger.info("Default model will be used");
        }

        // Workflow to execute when getting callback (optional, with default)
        Option<String> wfOpt = OsgiUtil.getOptCfg(cc.getProperties(), WORKFLOW_CONFIG);
        if (wfOpt.isSome())
          workflowDefinitionId = wfOpt.get();
        logger.info("Workflow definition is {}", workflowDefinitionId);
        // Interval to check for completed transcription jobs and start workflows to attach transcripts
        Option<String> intervalOpt = OsgiUtil.getOptCfg(cc.getProperties(), DISPATCH_WORKFLOW_INTERVAL_CONFIG);
        if (intervalOpt.isSome()) {
          try {
            workflowDispatchInterval = Long.parseLong(intervalOpt.get());
          } catch (NumberFormatException e) {
            // Use default
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
                    new Object[] { COMPLETION_CHECK_BUFFER_CONFIG, bufferOpt.get(), completionCheckBuffer });
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
          }
        }
        logger.info("Cleanup result files after {} days", cleanupResultDays);

        serverUrl = OsgiUtil.getContextProperty(cc, MatterhornConstants.SERVER_URL_PROPERTY);
        systemAccount = OsgiUtil.getContextProperty(cc, DIGEST_USER_PROPERTY);

        // Schedule the workflow dispatching, starting in 2 minutes
        scheduledExecutor.scheduleWithFixedDelay(new WorkflowDispatcher(), 120, workflowDispatchInterval,
                TimeUnit.SECONDS);

        // Schedule the cleanup of old results jobs from the collection in the wfr once a day
        scheduledExecutor.scheduleWithFixedDelay(new ResultsFileCleanup(), 1, 1, TimeUnit.DAYS);

        // Notification email passed in this service configuration?
        Option<String> optTo = OsgiUtil.getOptCfg(cc.getProperties(), NOTIFICATION_EMAIL_CONFIG);
        if (optTo.isSome())
          toEmailAddress = optTo.get();
        else {
          // Use admin email informed in custom.properties
          optTo = OsgiUtil.getOptContextProperty(cc, ADMIN_EMAIL_PROPERTY);
          if (optTo.isSome())
            toEmailAddress = optTo.get();
        }
        if (toEmailAddress != null)
          logger.info("Notification email set to {}", toEmailAddress);
        else
          logger.warn("Email notification disabled");

        Option<String> optCluster = OsgiUtil.getOptContextProperty(cc, CLUSTER_NAME_PROPERTY);
        if (optCluster.isSome())
          clusterName = optCluster.get();
        logger.info("Environment name is {}", clusterName);

        logger.info("Activated!");
        // Cannot call registerCallback here because of the REST service dependency on this service
      } else {
        logger.info("Service disabled. If you want to enable it, please update the service configuration.");
      }
    } else
      throw new IllegalArgumentException("Missing component context");
  }

  @Override
  public Job startTranscription(String mpId, Track track) throws TranscriptionServiceException {
    if (!enabled)
      throw new TranscriptionServiceException(
              "This service is disabled. If you want to enable it, please update the service configuration.");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.StartTranscription.name(),
              Arrays.asList(mpId, MediaPackageElementParser.getAsXml(track)));
    } catch (ServiceRegistryException e) {
      throw new TranscriptionServiceException("Unable to create a job", e);
    } catch (MediaPackageException e) {
      throw new TranscriptionServiceException("Invalid track " + track.toString(), e);
    }
  }

  @Override
  public void transcriptionDone(String mpId, Object obj) throws TranscriptionServiceException {
    JSONObject jsonObj = null;
    String jobId = null;
    try {
      jsonObj = (JSONObject) obj;
      jobId = (String) jsonObj.get("id");
      logger.info("Transcription done for mpId {}, jobId {}", mpId, jobId);

      // Update state in database
      // If there's an optimistic lock exception here, it's ok because the workflow dispatcher
      // may be doing the same thing
      database.updateJobControl(jobId, TranscriptionJobControl.Status.TranscriptionComplete.name());

      // Save results in file system if there
      if (jsonObj.get("results") != null)
        saveResults(jobId, jsonObj);
    } catch (IOException e) {
      logger.warn("Could not save transcription results file for mpId {}, jobId {}: {}",
              new Object[] { mpId, jobId, jsonObj == null ? "null" : jsonObj.toJSONString() });
      throw new TranscriptionServiceException("Could not save transcription results file", e);
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Transcription results file were saved but state in db not updated for mpId {}, jobId {}", mpId,
              jobId);
      throw new TranscriptionServiceException("Could not update transcription job control db", e);
    }
  }

  @Override
  public void transcriptionError(String mpId, Object obj) throws TranscriptionServiceException {
    JSONObject jsonObj = null;
    String jobId = null;
    try {
      jsonObj = (JSONObject) obj;
      jobId = (String) jsonObj.get("id");
      // Update state in database
      database.updateJobControl(jobId, TranscriptionJobControl.Status.Error.name());
      TranscriptionJobControl jobControl = database.findByJob(jobId);
      logger.warn(String.format("Error callback received for media package %s, job id %s",
              jobControl.getMediaPackageId(), jobId));
      // Send notification email
      sendEmail("Transcription ERROR",
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
        createRecognitionsJob(mpId, track);
        break;
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
    }

    return result;
  }

  /**
   * Register the callback url with the Speech-to-text service. From:
   * https://www.ibm.com/watson/developercloud/speech-to-text/api/v1/#register_callback
   *
   * curl -X POST -u "{username}":"{password}" --data "{}"
   * "https://stream.watsonplatform.net/speech-to-text/api/v1/register_callback?callback_url=http://{user_callback_path}/results&user_secret=ThisIsMySecret"
   * Response looks like: { "status": "created", "url": "http://{user_callback_path}/results" }
   */
  void registerCallback() throws TranscriptionServiceException {
    if (callbackAlreadyRegistered)
      return;

    Organization org = securityService.getOrganization();
    String adminUrl = StringUtils.trimToNull(org.getProperties().get(ADMIN_URL_PROPERTY));
    if (adminUrl != null)
      callbackUrl = adminUrl + CALLBACK_PATH;
    else
      callbackUrl = serverUrl + CALLBACK_PATH;
    logger.info("Callback url is {}", callbackUrl);

    CloseableHttpClient httpClient = makeHttpClient();
    HttpPost httpPost = new HttpPost(
            IBM_WATSON_SERVICE_URL + REGISTER_CALLBACK + String.format("?callback_url=%s", callbackUrl));
    CloseableHttpResponse response = null;

    try {
      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();

      switch (code) {
        case HttpStatus.SC_OK: // 200
          logger.info("Callback url: {} had already already been registered", callbackUrl);
          callbackAlreadyRegistered = true;
          EntityUtils.consume(response.getEntity());
          break;
        case HttpStatus.SC_CREATED: // 201
          logger.info("Callback url: {} has been successfully registered", callbackUrl);
          callbackAlreadyRegistered = true;
          EntityUtils.consume(response.getEntity());
          break;
        case HttpStatus.SC_BAD_REQUEST: // 400
          logger.warn("Callback url {} could not be verified, status: {}", callbackUrl, code);
          break;
        case HttpStatus.SC_SERVICE_UNAVAILABLE: // 503
          logger.warn("Service unavailable when registering callback url {} status: {}", callbackUrl, code);
          break;
        default:
          logger.warn("Unknown status when registering callback url {}, status: {}", callbackUrl, code);
          break;
      }
    } catch (Exception e) {
      logger.warn("Exception when calling the the register callback endpoint", e);
    } finally {
      try {
        httpClient.close();
        if (response != null)
          response.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * From: https://www.ibm.com/watson/developercloud/speech-to-text/api/v1/#register_callback:
   *
   * curl -X POST -u "{username}":"{password}" --header "Content-Type: audio/flac" --data-binary @audio-file.flac
   * "https://stream.watsonplatform.net/speech-to-text/api/v1/recognitions?callback_url=http://{user_callback_path}/results&user_token=job25&continuous=true&timestamps=true"
   *
   * Response: { "id": "4bd734c0-e575-21f3-de03-f932aa0468a0", "status": "waiting", "url":
   * "http://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/4bd734c0-e575-21f3-de03-f932aa0468a0" }
   */
  void createRecognitionsJob(String mpId, Track track) throws TranscriptionServiceException {
    if (!callbackAlreadyRegistered)
      registerCallback();

    // Get audio track file
    File audioFile = null;
    try {
      audioFile = workspace.get(track.getURI());
    } catch (Exception e) {
      throw new TranscriptionServiceException("Error reading audio track", e);
    }

    CloseableHttpClient httpClient = makeHttpClient();
    String additionalParms = "";
    if (callbackAlreadyRegistered) {
      additionalParms = String.format("&callback_url=%s&events=%s,%s", callbackUrl, JobEvent.COMPLETED_WITH_RESULTS,
              JobEvent.FAILED);
    }
    if (!StringUtils.isEmpty(model)) {
      additionalParms += String.format("&model=%s", model);
    }
    CloseableHttpResponse response = null;
    try {
      HttpPost httpPost = new HttpPost(IBM_WATSON_SERVICE_URL + RECOGNITIONS
              + String.format(
                      "?user_token=%s&inactivity_timeout=-1&timestamps=true&smart_formatting=true%s",
                      mpId, additionalParms));
      logger.debug("Url to invoke ibm watson service: {}", httpPost.getURI().toString());
      httpPost.setHeader(HttpHeaders.CONTENT_TYPE, track.getMimeType().toString());
      httpPost.setEntity(new FileEntity(audioFile));
      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();

      switch (code) {
        case HttpStatus.SC_CREATED: // 201
          logger.info("Recognitions job has been successfully created");

          HttpEntity entity = response.getEntity();
          // Response returned is a json object:
          // {
          // "id": "4bd734c0-e575-21f3-de03-f932aa0468a0",
          // "status": "waiting",
          // "url":
          // "http://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/4bd734c0-e575-21f3-de03-f932aa0468a0"
          // }
          String jsonString = EntityUtils.toString(response.getEntity());
          JSONParser jsonParser = new JSONParser();
          JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);
          String jobId = (String) jsonObject.get("id");
          String jobStatus = (String) jsonObject.get("status");
          String jobUrl = (String) jsonObject.get("url");
          logger.info(String.format(
                  "Transcription for mp %s has been submitted. Job id: %s, job status: %s, job url: %s", mpId,
                  jobId, jobStatus, jobUrl));

          database.storeJobControl(mpId, track.getIdentifier(), jobId, TranscriptionJobControl.Status.Progress.name(),
                  track.getDuration() == null ? 0 : track.getDuration().longValue());
          EntityUtils.consume(entity);
          return;
        case HttpStatus.SC_BAD_REQUEST: // 400
          logger.info("Invalid argument returned, status: {}", code);
          break;
        case HttpStatus.SC_SERVICE_UNAVAILABLE: // 503
          logger.info("Service unavailable returned, status: {}", code);
          break;
        default:
          logger.info("Unknown return status: {}.", code);
          break;
      }
      throw new TranscriptionServiceException("Could not create recognition job. Status returned: " + code);
    } catch (Exception e) {
      logger.warn("Exception when calling the recognitions endpoint", e);
      throw new TranscriptionServiceException("Exception when calling the recognitions endpoint", e);
    } finally {
      try {
        httpClient.close();
        if (response != null)
          response.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * From: https://www.ibm.com/watson/developercloud/speech-to-text/api/v1 Check a job: GET /v1/recognitions/{id}
   *
   * curl -X GET -u "{username}":"{password}"
   * "https://stream.watsonplatform.net/speech-to-text/api/v1/recognitions/{id}"
   *
   * Response: { "results": [ { "result_index": 0, "results": [ { "final": true, "alternatives": [ { "transcript":
   * "several tornadoes touch down as a line of severe thunderstorms swept through Colorado on Sunday ", "timestamps": [
   * [ "several", 1, 1.52 ], [ "tornadoes", 1.52, 2.15 ], . . . [ "Sunday", 5.74, 6.33 ] ], "confidence": 0.885 } ] } ]
   * } ], "created": "2016-08-17T19:11:04.298Z", "updated": "2016-08-17T19:11:16.003Z", "status": "completed" }
   */
  boolean getAndSaveJobResults(String jobId) throws TranscriptionServiceException {
    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;
    String mpId = "unknown";
    try {
      HttpGet httpGet = new HttpGet(IBM_WATSON_SERVICE_URL + RECOGNITIONS + "/" + jobId);
      response = httpClient.execute(httpGet);
      int code = response.getStatusLine().getStatusCode();

      switch (code) {
        case HttpStatus.SC_OK: // 200
          HttpEntity entity = response.getEntity();

          // Response returned is a json object described above
          String jsonString = EntityUtils.toString(entity);
          JSONParser jsonParser = new JSONParser();
          JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);
          String jobStatus = (String) jsonObject.get("status");
          mpId = (String) jsonObject.get("user_token");
          // user_token doesn't come back if this is not in the context of a callback so get the mpId from the db
          if (mpId == null) {
            TranscriptionJobControl jc = database.findByJob(jobId);
            if (jc != null)
              mpId = jc.getMediaPackageId();
          }
          logger.info("Recognitions job {} has been found, status {}", jobId, jobStatus);
          EntityUtils.consume(entity);

          if (jobStatus.indexOf("completed") > -1 && jsonObject.get("results") != null) {
            transcriptionDone(mpId, jsonObject);
            return true;
          }
          return false;
        case HttpStatus.SC_NOT_FOUND: // 404
          logger.info("Job not found: {}", jobId);
          break;
        case HttpStatus.SC_SERVICE_UNAVAILABLE: // 503
          logger.info("Service unavailable returned, status: {}", code);
          break;
        default:
          logger.info("Unknown return status: {}.", code);
          break;
      }
      throw new TranscriptionServiceException(
              String.format("Could not check recognition job for media package %s, job id %s. Status returned: %d",
                      mpId, jobId, code),
              code);
    } catch (TranscriptionServiceException e) {
      throw e;
    } catch (Exception e) {
      String msg = String.format("Exception when calling the recognitions endpoint for media package %s, job id %s",
              mpId, jobId);
      logger.warn(String.format(msg, mpId, jobId), e);
      throw new TranscriptionServiceException(String.format(
              "Exception when calling the recognitions endpoint for media package %s, job id %s", mpId, jobId), e);
    } finally {
      try {
        httpClient.close();
        if (response != null)
          response.close();
      } catch (IOException e) {
      }
    }
  }

  private void saveResults(String jobId, JSONObject jsonObj) throws IOException {
    if (jsonObj.get("results") != null) {
      // Save the results into a collection
      workspace.putInCollection(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId),
              new ByteArrayInputStream(jsonObj.toJSONString().getBytes()));
    }
  }

  @Override
  public MediaPackageElement getGeneratedTranscription(String mpId, String jobId) throws TranscriptionServiceException {
    try {
      // If jobId is unknown, look for all jobs associated to that mpId
      if (jobId == null || "null".equals(jobId)) {
        jobId = null;
        for (TranscriptionJobControl jc : database.findByMediaPackage(mpId)) {
          if (TranscriptionJobControl.Status.Closed.name().equals(jc.getStatus())
                  || TranscriptionJobControl.Status.TranscriptionComplete.name().equals(jc.getStatus()))
            jobId = jc.getTranscriptionJobId();
        }
      }

      if (jobId == null)
        throw new TranscriptionServiceException(
                "No completed or closed transcription job found in database for media package " + mpId);

      // Results already saved?
      URI uri = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId));
      try {
        workspace.get(uri);
      } catch (Exception e) {
        // Not saved yet so call the ibm watson service to get the results
        getAndSaveJobResults(jobId);
      }
      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      return builder.elementFromURI(uri, Attachment.TYPE, new MediaPackageElementFlavor("captions", "ibm-watson-json"));
    } catch (TranscriptionDatabaseException e) {
      throw new TranscriptionServiceException("Job id not informed and could not find transcription", e);
    }
  }

  protected CloseableHttpClient makeHttpClient() {
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, psw));
    RequestConfig reqConfig = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(SOCKET_TIMEOUT).setConnectionRequestTimeout(CONNECTION_TIMEOUT).build();
    return HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).setDefaultRequestConfig(reqConfig)
            .build();
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
      logger.error(String.format("Could not send email: %s\n%s", subject, body), e);
    }
  }

  private String buildResultsFileName(String jobId) {
    return PathSupport.toSafeName(jobId + ".json");
  }

  public boolean isCallbackAlreadyRegistered() {
    return callbackAlreadyRegistered;
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
        // Find jobs that are in progress and jobs that had transcription complete i.e. got the callback
        List<TranscriptionJobControl> jobs = database.findByStatus(TranscriptionJobControl.Status.Progress.name(),
                TranscriptionJobControl.Status.TranscriptionComplete.name());

        for (TranscriptionJobControl j : jobs) {
          String mpId = j.getMediaPackageId();
          String jobId = j.getTranscriptionJobId();

          // If the job in progress, check if it should already have finished and we didn't get the callback for some
          // reason. This can happen if the admin server was offline when the callback came.
          if (TranscriptionJobControl.Status.Progress.name().equals(j.getStatus())) {
            // If job should already have been completed, try to get the results. Consider a buffer factor so that we
            // don't try it too early.
            if (j.getDateCreated().getTime() + j.getTrackDuration() + completionCheckBuffer * 1000 < System
                    .currentTimeMillis()) {
              try {
                if (!getAndSaveJobResults(jobId)) {
                  // Job still running, not finished, so check if it should have finished more than N seconds ago
                  if (j.getDateCreated().getTime() + j.getTrackDuration()
                          + (completionCheckBuffer + maxProcessingSeconds) * 1000 < System.currentTimeMillis()) {
                    // Processing for too long, mark job as canceled and don't check anymore
                    database.updateJobControl(jobId, TranscriptionJobControl.Status.Canceled.name());
                    // Send notification email
                    sendEmail("Transcription ERROR", String.format(
                            "Transcription job was in processing state for too long and was marked as canceled (media package %s, job id %s).",
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
                  sendEmail("Transcription ERROR",
                          String.format("Transcription job was not found (media package %s, job id %s).", mpId, jobId));
                }
                continue; // Skip this one, exception was already logged
              }
            } else
              continue; // Not time to check yet
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
              // Media package not archived yet? Skip until next time.
              logger.warn("Media package {} has not been archived yet. Skipped.", mpId);
              continue;
            }

            String org = Enrichments.enrich(r).getSnapshots().head2().getOrganizationId();
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
            Workflows workflows = wfUtil != null ? wfUtil : new Workflows(assetManager, workspace, workflowService);
            Set<String> mpIds = new HashSet<String>();
            mpIds.add(mpId);
            List<WorkflowInstance> wfList = workflows
                    .applyWorkflowToLatestVersion(mpIds, ConfiguredWorkflow.workflow(wfDef, params)).toList();
            String wfId = wfList.size() > 0 ? Long.toString(wfList.get(0).getId()) : "Unknown";

            // Update state in the database
            database.updateJobControl(jobId, TranscriptionJobControl.Status.Closed.name());
            logger.info("Attach transcription workflow {} scheduled for mp {}, watson job {}",
                    new Object[] { wfId, mpId, jobId });
          } catch (Exception e) {
            logger.warn("Attach transcription workflow could NOT be scheduled for mp {}, watson job {}, {}: {}",
                    new Object[] { mpId, jobId, e.getClass().getName(), e.getMessage() });
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
      } catch (IOException e) {
        logger.warn("Could not cleanup old transcript results files", e);
      }
    }
  }

}
