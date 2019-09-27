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
package org.opencastproject.transcription.nibity;

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
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

public class NibityTranscriptionService extends AbstractJobProducer implements TranscriptionService {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(NibityTranscriptionService.class);

  private static final String JOB_TYPE = "org.opencastproject.transcription.nibity";

  public static final String SUBMISSION_COLLECTION = "nibity-submission";
  private static final String TRANSCRIPT_COLLECTION = "nibity-transcripts";
  private static final String SUBMISSION_PATH = "/transcripts/nibity/submission/";

  // http timeouts
  private static final int CONNECTION_TIMEOUT = 60000; // ms, 1 minute
  private static final int SOCKET_TIMEOUT = 60000; // ms, 1 minute

  // Default wf to attach transcription results to mp
  private static final String DEFAULT_WF_DEF = "nibity-attach-transcripts";
  private static final long DEFAULT_DISPATCH_INTERVAL = 60; // in seconds, default is 1 minute
  private static final long DEFAULT_MAX_PROCESSING_TIME = 48 * 60 * 60; // in seconds, default is 2 days.

  // Cleans up results files that are older than 7 days
  private static final int DEFAULT_CLEANUP_RESULTS_DAYS = 7;
  private static final String DEFAULT_LANGUAGE = "en-US";

  // Nibity API
  private static final String NIBITY_BASE_URL = "https://api.nibity.com/v1";
  private static final long NIBITY_STATUS_SUCCESS = 500;

  private static final String PROVIDER = "nibity";

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
  public static final String NIBITY_LANGUAGE = "nibity.language";
  public static final String WORKFLOW_CONFIG = "workflow";
  public static final String DISPATCH_WORKFLOW_INTERVAL_CONFIG = "workflow.dispatch.interval";
  public static final String MAX_PROCESSING_TIME_CONFIG = "max.overdue.time";
  public static final String NOTIFICATION_EMAIL_CONFIG = "notification.email";
  public static final String CLEANUP_RESULTS_DAYS_CONFIG = "cleanup.results.days";
  public static final String CLEANUP_SUBMISSION = "cleanup.submission";
  public static final String NIBITY_CLIENT_ID = "nibity.client.id";
  public static final String NIBITY_CLIENT_KEY = "nibity.client.key";

  /**
   * Service configuration values
   */
  private boolean enabled = false; // Disabled by default
  private String language = DEFAULT_LANGUAGE;
  private String workflowDefinitionId = DEFAULT_WF_DEF;
  private long workflowDispatchInterval = DEFAULT_DISPATCH_INTERVAL;
  private long maxProcessingSeconds = DEFAULT_MAX_PROCESSING_TIME;
  private String toEmailAddress;
  private int cleanupResultDays = DEFAULT_CLEANUP_RESULTS_DAYS;
  private boolean cleanupSubmission = true; // Remove submissions immediately
  private String nibityClientId;
  private String nibityClientKey;
  private String systemAccount;
  private String serverUrl;
  private ScheduledExecutorService scheduledExecutor = null;

  public NibityTranscriptionService() {
    super(JOB_TYPE);
  }

  public void activate(ComponentContext cc) {

    // Has this service been enabled?
    enabled = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), ENABLED_CONFIG).get();

    if (!enabled) {
      logger.info("Nibity Transcription Service disabled. If you want to enable it, please update the service configuration.");
      return;
    }

    // Nibity API client ID and key (mandatory)
    nibityClientId = OsgiUtil.getComponentContextProperty(cc, NIBITY_CLIENT_ID);
    nibityClientKey = OsgiUtil.getComponentContextProperty(cc, NIBITY_CLIENT_KEY);
    logger.info("Nibity Transcription Service enabled with client id {}", nibityClientId);

    // Cleanup submissions
    Option<Boolean> cleanupOpt = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), CLEANUP_SUBMISSION);
    if (cleanupOpt.isSome()) {
      cleanupSubmission = cleanupOpt.get();
    }

    if (cleanupSubmission) {
      logger.info("Temporary media files will be removed immediately after submission");
    } else {
      logger.info("Temporary submission media files will NOT be removed immediately");
    }

    // Language model to be used (not used by Nibity)
    Option<String> languageOpt = OsgiUtil.getOptCfg(cc.getProperties(), NIBITY_LANGUAGE);
    if (languageOpt.isSome()) {
      language = languageOpt.get();
      logger.info("Language used is {}", language);
    } else {
      logger.info("Default language will be used");
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
      }
    }
    logger.info("Workflow dispatch interval is {} seconds", workflowDispatchInterval);

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

    serverUrl = OsgiUtil.getContextProperty(cc, OpencastConstants.SERVER_URL_PROPERTY);
    systemAccount = OsgiUtil.getContextProperty(cc, OpencastConstants.DIGEST_USER_PROPERTY);

    // New execute service
    scheduledExecutor = Executors.newScheduledThreadPool(2);

    // Schedule the workflow dispatching, starting in 2 minutes
    scheduledExecutor.scheduleWithFixedDelay(new WorkflowDispatcher(), 120, workflowDispatchInterval,
            TimeUnit.SECONDS);

    // Schedule the cleanup of old results jobs from the collection in the WFR once a day
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
      logger.info("Environment name is {}", clusterName);
    }

   logger.info("Activated!");
  }

  public void deactivate(ComponentContext cc) {
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdown();
    }
  }

  // Called by Submission WOH
  @Override
  public Job startTranscription(String mpId, Track track) throws TranscriptionServiceException {
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
  public Job startTranscription(String mpId, Track track, String language) throws TranscriptionServiceException {
    // Nibity API does not support language
    return startTranscription(mpId, track);
  }

  // Could be called by the REST callback endpoint (which the Nibity API doesn't support at present)
  @Override
  public void transcriptionDone(String mpId, Object results) throws TranscriptionServiceException {
    // Nibity API does not support callbacks, so this is never called
    throw new TranscriptionServiceException("Not supported");
  }

  // Called from checkJobResults()
  private void transcriptionDone(String mpId, String jobId, Long transcriptId) throws TranscriptionServiceException {
    JSONObject jsonObj = null;

    try {
      // Expected: {"auth":504,"transcript_id":2227,"types":["transcript","srt","vtt"]}

      if (transcriptId != null) {
        logger.info("Transcription done for mpId {}, transcript_id {}", mpId, transcriptId);

        // Save results in file system
        if (getAndSaveJobResult(jobId, transcriptId)) {
          // Update state in database
          // If there's an optimistic lock exception here, it's ok because the workflow dispatcher
          // may be doing the same thing
          database.updateJobControl(jobId, TranscriptionJobControl.Status.TranscriptionComplete.name());
        } else {
          logger.debug("Unable to get and save the transcription result for mpId {}", mpId);
        }

      } else {
        logger.debug("No transcription available yet for mpId {}", mpId);
      }
    } catch (IOException e) {
      logger.warn("Could not save transcription results file for mpId {}", mpId);
      throw new TranscriptionServiceException("Could not save transcription results file", e);
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Transcription results file were saved but state in db not updated for mpId {}", mpId, e);
      throw new TranscriptionServiceException("Could not update transcription job control db", e);
    }
  }

  // Could be called by REST callback endpoint (not supported by Nibity at present)
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
      sendEmail("Transcription ERROR",
              String.format("There was a transcription error for for media package %s, job id %s.",
                      jobControl.getMediaPackageId(), jobId));
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Transcription error. State in db could not be updated to error for mpId {}, jobId {}", mpId, jobId);
      throw new TranscriptionServiceException("Could not update transcription job control db", e);
    }
  }

  @Override
  public String getLanguage() {
    return language;
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
        createRecognitionsJob(mpId, track, languageCode);
        break;
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
    }
    return result;
  }

  /**
   * Asynchronous Requests and Responses call to Nibity API
   * https://documenter.getpostman.com/view/5815470/RznFpyb6
   * https://api.nibity.com/v1/{id}/submit
   *
   * Called by process(Job job)
   */
  void createRecognitionsJob(String mpId, Track track, String languageCode) throws TranscriptionServiceException, IOException {

    String filename = addMediaFileToLocalStorage(mpId, track);
    String mediaUrl = serverUrl + SUBMISSION_PATH + filename;

    logger.info("Media URL in intermediate storage: {}", mediaUrl);

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY,
      new UsernamePasswordCredentials(nibityClientKey, ""));

    // Timeout 3 hours (needs to include the time for the remote service to fetch the media URL before sending final response)
    RequestConfig config = RequestConfig.custom()
     .setConnectTimeout(CONNECTION_TIMEOUT)
     .setSocketTimeout(3 * 3600 * 1000).build();

    CloseableHttpClient httpClient = HttpClientBuilder.create()
     .setDefaultCredentialsProvider(credentialsProvider)
     .setDefaultRequestConfig(config)
     .build();

    CloseableHttpResponse response = null;

    String submitUrl = NIBITY_BASE_URL + "/" + nibityClientId + "/submit";

    logger.debug("Submitting new transcription job to Nibity API at {}", submitUrl);

    List <NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("media[0][name]", mpId));
    nvps.add(new BasicNameValuePair("media[0][url]", mediaUrl));
    // nvps.add(new BasicNameValuePair("ref", "Test submission reference"));

    // TODO possibly add a series and lecture title here
    nvps.add(new BasicNameValuePair("notes", "Opencast captions job"));
    nvps.add(new BasicNameValuePair("retain", "168"));

    try {
      HttpPost httpPost = new HttpPost(submitUrl);
      httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();

      String jsonString = EntityUtils.toString(response.getEntity());
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);

      logger.debug("Nibity API {} http response {}, JSON response: {}", submitUrl, code, jsonString);

      switch (code) {
        case HttpStatus.SC_OK: // 200

          /**
           * Response returned is a json object: {"test-submission":{"file_id":"3074","file_type":"mp4","seconds":2633.677,"status":500,"deadline":"2019-02-25 11:28:42"}}
           * Status codes other than 500 are an error.
           */
          JSONObject result = (JSONObject) jsonObject.get(mpId);

          String jobId = (String) result.get("file_id");
          String fileType = (String) result.get("file_type");
          String deadline = (String) result.get("deadline");
          long jobStatus = (Long) result.get("status");

          if (jobStatus != NIBITY_STATUS_SUCCESS) {
            throw new TranscriptionServiceException("Unable to submit job: API returned status " + jobStatus);
          }

          SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

          Date expectedDate;

          try {
            expectedDate = format.parse(deadline);
          } catch (ParseException e) {
            logger.warn("Unable to parse deadline date string: {}", deadline);
            expectedDate = new Date();
          }

          // TODO how does this handle timezones?

          logger.info("mp {} has been submitted to nibity: file id: {} status {} type {}", mpId, jobId, jobStatus, fileType);

          if (jobStatus == 500) {
              database.storeJobControl(mpId, track.getIdentifier(), jobId, TranscriptionJobControl.Status.InProgress.name(),
                  track.getDuration() == null ? 0 : track.getDuration().longValue(), expectedDate, PROVIDER);
              EntityUtils.consume(entity);
          } else {
              logger.warn("Unknown job status {} in JSON response: {}", jsonString);
          }
          return;

        default:
          logger.warn("Invalid argument returned, status: {} with message: {}", code, jsonString);
          break;
      }
      throw new TranscriptionServiceException("Could not create caption job. Status returned: " + code);
    } catch (Exception e) {
      logger.warn("Exception when calling the captions endpoint", e);
      throw new TranscriptionServiceException("Exception when calling the captions endpoint", e);
    } finally {
      try {
        httpClient.close();
        if (response != null) {
          response.close();
        }
        // Clean up the submission file (if immediate cleanup)
        if (cleanupSubmission) {
          deleteStorageFile(filename);
        }
      } catch (IOException e) {
      }
    }
  }

  /**
   * Get transcription job result:
   * POST https://api.nibity.com/v1/{id}/check/ with files[0]=jobId
   *   response: { "3765": { "auth": 504, "transcript_id": 7645 }, "3766": { "auth": 504, "transcript_id": 7735 } }
   *
   * POST https://api.nibity.com/v1/{id}/transcript/ with transcripts[0]=transcript_id
   *   response: the transcript itself
   *
   * Called by WorkflowDispatcher.run() every WorkflowDispatchInterval
   */
  boolean checkJobResults(String jobId) throws TranscriptionServiceException, IOException {

    String mpId = "unknown";
    String captionsVtt = null;

    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;

    String checkUrl = NIBITY_BASE_URL + "/" + nibityClientId + "/check";

    List <NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("files[0]", jobId));

    try {
      HttpPost httpPost = new HttpPost(checkUrl);
      httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();

      HttpEntity entity = response.getEntity();
      // Response returned is a json object described above
      String jsonString = EntityUtils.toString(entity);
      EntityUtils.consume(entity);

      logger.debug("Nibity API {} http response {}, JSON response: {}", checkUrl, code, jsonString);

      // Expected for not-ready: {"3074":{"auth":504,"transcript_id":null}}
      // Expected for ready: {"3074":{"auth":504,"transcript_id":2227,"types":["transcript","srt","vtt"]}}

      switch (code) {
        case HttpStatus.SC_OK: // 200
          boolean jobDone = false;

          logger.info("JSON from checkjob: {}", jsonString);

          JSONParser jsonParser = new JSONParser();
          JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);

          JSONObject result = (JSONObject) jsonObject.get(jobId);

          long auth = (Long) result.get("auth");
          Long transcriptId = (Long) result.get("transcript_id");

          if ((auth == 504) && (transcriptId != null)) {
            logger.info("Captions job {} has finished, auth {}, transcript id {}", jobId, auth, transcriptId);

            TranscriptionJobControl jc = database.findByJob(jobId);
            if (jc != null) {
              mpId = jc.getMediaPackageId();
            }

            // Notify that captions are ready (types array includes "transcript" and "vtt")
            // TODO only if we have at least a transcript and captions.
            // What would happen if caption alignment failed because of suboptimal audio?
            transcriptionDone(mpId, jobId, transcriptId);

            return true;
          }

          // Job is not ready yet
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
              String.format("Could not check caption job for media package %s, job id %s. Status returned: %d",
                      mpId, jobId, code), code);
    } catch (TranscriptionServiceException e) {
      throw e;
    } catch (Exception e) {
      String msg = String.format("Exception when calling the captions endpoint for media package %s, job id %s",
              mpId, jobId);
      logger.warn(String.format(msg, mpId, jobId), e);
      throw new TranscriptionServiceException(String.format(
              "Exception when calling the captions endpoint for media package %s, job id %s", mpId, jobId), e);
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
   * Get transcription result: https://api.nibity.com/v1/{id}/transcript/
   *
   * @param jobId
   * @return job details
   * @throws org.opencastproject.transcription.api.TranscriptionServiceException
   * @throws java.io.IOException
   */
  private boolean getAndSaveJobResult(String jobId, Long transcriptId) throws TranscriptionServiceException, IOException {

    CloseableHttpClient httpClient = makeHttpClient();
    CloseableHttpResponse response = null;

    String transcriptUrl = NIBITY_BASE_URL + "/" + nibityClientId + "/transcript";

    List <NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("transcripts[0][transcript_id]", Long.toString(transcriptId)));
    nvps.add(new BasicNameValuePair("transcripts[0][type]", "transcript"));
    nvps.add(new BasicNameValuePair("transcripts[1][transcript_id]", Long.toString(transcriptId)));
    nvps.add(new BasicNameValuePair("transcripts[1][type]", "txt"));
    nvps.add(new BasicNameValuePair("transcripts[2][transcript_id]", Long.toString(transcriptId)));
    nvps.add(new BasicNameValuePair("transcripts[2][type]", "vtt"));

    boolean done = false;

    try {
      HttpPost httpPost = new HttpPost(transcriptUrl);
      httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

      response = httpClient.execute(httpPost);
      int code = response.getStatusLine().getStatusCode();

      logger.debug("Nibity API {} http response {}", transcriptUrl, code);

      switch (code) {
        case HttpStatus.SC_OK: // 200
          HttpEntity entity = response.getEntity();
          logger.info("Retrieved details for transcription with transcript id: '{}'", transcriptId);

          // Save the result zip file into a collection
          workspace.putInCollection(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId, "zip"), entity.getContent());
          done = true;
          break;

        default:
          logger.warn("Error retrieving details for transcription with transcript id: '{}', return status: {}.", transcriptId, code);
          break;
      }
    } catch (Exception e) {
      throw new TranscriptionServiceException(String.format(
              "Exception when calling the transcription service for transcript id: %s", transcriptId), e);
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
      URI uri = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId, "zip"));

      logger.info("Looking for transcript at URI: {}", uri);

      try {
        workspace.get(uri);
        logger.info("Found captions at URI: {}", uri);
      } catch (Exception e) {
        try {
          logger.info("Results not saved: getting from service for jobId {}", jobId);
          // Not saved yet so call the transcription service to get the results
          checkJobResults(jobId);
        } catch (IOException ex) {
          logger.error("Unable to retrieve transcription job, error: {}", ex.toString());
        }
      }
      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      logger.debug("Returning MPE with results file URI: {}", uri);
      return builder.elementFromURI(uri, Attachment.TYPE, new MediaPackageElementFlavor("application", "zip"));
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

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    credentialsProvider.setCredentials(AuthScope.ANY,
      new UsernamePasswordCredentials(nibityClientKey, ""));

    RequestConfig reqConfig = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(SOCKET_TIMEOUT).setConnectionRequestTimeout(CONNECTION_TIMEOUT).build();

    CloseableHttpClient httpClient = HttpClientBuilder.create()
     .setDefaultCredentialsProvider(credentialsProvider)
     .setDefaultRequestConfig(reqConfig)
     .build();

    return httpClient;
  }

  /**
   * Add the media file to local storage.
   * @return the filename in the collection
   */
  protected String addMediaFileToLocalStorage(String mpId, Track track)
          throws TranscriptionServiceException, IOException {
    try {
      String filename = mpId + "_media." + FilenameUtils.getExtension(track.getURI().toString());

      // Seems unnecessary to have to read & write this rather than just hardlink it
      wfr.putInCollection(SUBMISSION_COLLECTION, filename, workspace.read(track.getURI()));

      return filename;
    } catch (Exception e) {
      throw new TranscriptionServiceException("Error adding file to collection", e);
    }
  }

  // Called when a transcription job has been submitted
  protected void deleteStorageFile(String filename) throws IOException {
    try {
      wfr.deleteFromCollection(SUBMISSION_COLLECTION, filename, false);
    } catch (IOException e) {
      logger.warn("Unable to remove submission file {} from collection {}", filename, SUBMISSION_COLLECTION);
    }
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

  private String buildResultsFileName(String jobId, String extension) {
    return PathSupport.toSafeName(jobId + "." + extension);
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
        // The Nibity API lacks callbacks at present, so we only expect to find jobs in progress

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
            // If job should already have been completed, try to get the results.
            if (j.getDateExpected().getTime() < System.currentTimeMillis()) {
              try {
                if (!checkJobResults(jobId)) {
                  // Job still running, not finished, so check if it should have finished more than N seconds ago
                  if (j.getDateExpected().getTime() + maxProcessingSeconds * 1000 < System.currentTimeMillis()) {
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
              } catch (IOException ex) {
                logger.error("Transcription job not found, error: {}", ex.toString());
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
            Workflows workflows = wfUtil != null ? wfUtil : new Workflows(assetManager, workflowService);
            Set<String> mpIds = new HashSet<String>();
            mpIds.add(mpId);
            List<WorkflowInstance> wfList = workflows
                    .applyWorkflowToLatestVersion(mpIds, ConfiguredWorkflow.workflow(wfDef, params)).toList();
            String wfId = wfList.size() > 0 ? Long.toString(wfList.get(0).getId()) : "Unknown";

            // Update state in the database
            database.updateJobControl(jobId, TranscriptionJobControl.Status.Closed.name());
            logger.info("Attach transcription workflow {} scheduled for mp {}, transcription service job {}", new String[]{wfId,
              mpId, jobId});
          } catch (Exception e) {
            logger.warn("Attach transcription workflow could NOT be scheduled for mp {}, nibity job {}, {}: {}",
                    new String[]{mpId, jobId, e.getClass().getName(), e.getMessage()});
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

}
