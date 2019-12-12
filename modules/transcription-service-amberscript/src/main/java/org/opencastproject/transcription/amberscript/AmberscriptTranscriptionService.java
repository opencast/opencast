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
package org.opencastproject.transcription.amberscript;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.fn.Enrichments;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

  // service configuration keys
  private static final String ENABLED_CONFIG = "enabled";
  private static final String LANGUAGE = "language";
  private static final String AMBERSCRIPTJOBTYPE = "jobtype";
  private static final String CLIENT_KEY = "client.key";
  private static final String WORKFLOW_CONFIG = "workflow";
  private static final String DISPATCH_WORKFLOW_INTERVAL_CONFIG = "workflow.dispatch.interval";
  private static final String MAX_PROCESSING_TIME_CONFIG = "max.overdue.time";
  private static final String CLEANUP_RESULTS_DAYS_CONFIG = "cleanup.results.days";

  // service configuration default values
  private boolean enabled = false;
  private String language = "en";
  private String amberscriptJobType = "direct";
  private String clientKey;
  private String workflowDefinitionId = "amberscript-attach-transcripts";
  private long workflowDispatchIntervalSeconds = 60;
  private long maxProcessingSeconds = 48 * 60 * 60;
  private int cleanupResultDays = 7;

  private String systemAccount;

  public AmberscriptTranscriptionService() {
    super(JOB_TYPE);
  }

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
      logger.info("Default Language is set to '{}'.", language);
    } else {
      logger.info("Default language '{}' will be used.", language);
    }

    Option<String> amberscriptJobTypeOpt = OsgiUtil.getOptCfg(cc.getProperties(), AMBERSCRIPTJOBTYPE);
    if (amberscriptJobTypeOpt.isSome()) {
      amberscriptJobType = amberscriptJobTypeOpt.get();
      logger.info("Default AmberScript JobType is set to '{}'.", amberscriptJobType);
    } else {
      logger.info("Default AmberScript JobType '{}' will be used.", amberscriptJobType);
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

    Option<String> cleaupOpt = OsgiUtil.getOptCfg(cc.getProperties(), CLEANUP_RESULTS_DAYS_CONFIG);
    if (cleaupOpt.isSome()) {
      try {
        cleanupResultDays = Integer.parseInt(cleaupOpt.get());
      } catch (NumberFormatException e) {
        logger.warn("Configured '{}' is invalid. Using default.", CLEANUP_RESULTS_DAYS_CONFIG);
      }
    }
    logger.info("Cleanup result files after {} days.", cleanupResultDays);

    systemAccount = OsgiUtil.getContextProperty(cc, OpencastConstants.DIGEST_USER_PROPERTY);

    scheduledExecutor = Executors.newScheduledThreadPool(2);

    scheduledExecutor.scheduleWithFixedDelay(new WorkflowDispatcher(), 120, workflowDispatchIntervalSeconds,
            TimeUnit.SECONDS);

    scheduledExecutor.scheduleWithFixedDelay(new ResultsFileCleanup(), 1, 1, TimeUnit.DAYS);

   logger.info("Activated.");
  }

  public void deactivate(ComponentContext cc) {
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdown();
    }
  }

  @Override
  public Job startTranscription(String mpId, Track track) throws TranscriptionServiceException {
    return startTranscription(mpId, track, getLanguage(), getAmberscriptJobType());
  }

  @Override
  public Job startTranscription(String mpId, Track track, String language) throws TranscriptionServiceException {
    return startTranscription(mpId, track, language, getAmberscriptJobType());
  }

  public Job startTranscription(String mpId, Track track, String language, String jobtype) throws TranscriptionServiceException {
    if (StringUtils.isBlank(language)) {
      language = getLanguage();
    }
    if (StringUtils.isBlank(jobtype)) {
      jobtype = getAmberscriptJobType();
    }

    if (!enabled) {
      throw new TranscriptionServiceException("AmberScript Transcription Service disabled."
              + " If you want to enable it, please update the service configuration.");
    }

    logger.info("New transcription job for mpId '{}' language '{}' JobType '{}'.", mpId, language, jobtype);

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.StartTranscription.name(),
              Arrays.asList(mpId, MediaPackageElementParser.getAsXml(track), language, jobtype));
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
    } catch (IOException e) {
      logger.warn("Could not save transcription results file for mpId '{}': {}", mpId, e.toString());
    } catch (TranscriptionServiceException e) {
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
  public String getLanguage() {
    return language;
  }

  public String getAmberscriptJobType() {
    return amberscriptJobType;
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
        String jobtype = arguments.get(3);
        createRecognitionsJob(mpId, track, languageCode, jobtype);
        break;
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
    }
    return result;
  }

  void createRecognitionsJob(String mpId, Track track, String languageCode, String jobtype) throws TranscriptionServiceException, IOException {
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY,
      new UsernamePasswordCredentials(clientKey, ""));

    // Timeout 3 hours (needs to include the time for the remote service to fetch the media URL before sending final response)
    RequestConfig config = RequestConfig.custom()
     .setConnectTimeout(CONNECTION_TIMEOUT)
     .setSocketTimeout(3 * 3600 * 1000).build();

    CloseableHttpClient httpClient = HttpClientBuilder.create()
     .setDefaultCredentialsProvider(credentialsProvider)
     .setDefaultRequestConfig(config)
     .build();

    CloseableHttpResponse response = null;

    String submitUrl = BASE_URL + "/jobs/upload-media?transcriptionType=transcription&jobType="
            + jobtype + "&language=" + languageCode + "&apiKey=" + clientKey;

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
          String msg = String.format("Unable to submit job: API returned {} - {}: {}", code, error, message);
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
              logger.warn("Captions job '{}' failed.", jobId);
              throw new TranscriptionServiceException(
                      String.format("Captions job '%s' failed: Return Code %d", jobId, code), code);
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
    } catch (TranscriptionDatabaseException e) {
      logger.warn("Error while checking status: ", e.toString());
    } catch (IOException e) {
      logger.warn("Error while checking status: ", e.toString());
    } catch (ParseException e) {
      logger.warn("Error while checking status: ", e.toString());
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
          workspace.putInCollection(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId, "srt"), entity.getContent());
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
      URI uri = workspace.getCollectionURI(TRANSCRIPT_COLLECTION, buildResultsFileName(jobId, "srt"));

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
      return builder.elementFromURI(uri, Attachment.TYPE, new MediaPackageElementFlavor("captions", "srt"));
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

  protected CloseableHttpClient makeHttpClient() {

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    RequestConfig reqConfig = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(SOCKET_TIMEOUT).setConnectionRequestTimeout(CONNECTION_TIMEOUT).build();

    CloseableHttpClient httpClient = HttpClientBuilder.create()
     .setDefaultCredentialsProvider(credentialsProvider)
     .setDefaultRequestConfig(reqConfig)
     .build();

    return httpClient;
  }

  // Called when a transcription job has been submitted
  protected void deleteStorageFile(String filename) throws IOException {
    try {
      logger.debug("Removing {} from collection {}.", filename, SUBMISSION_COLLECTION);
      wfr.deleteFromCollection(SUBMISSION_COLLECTION, filename, false);
    } catch (IOException e) {
      logger.warn("Unable to remove submission file {} from collection {}", filename, SUBMISSION_COLLECTION);
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
            logger.info("Attach transcription workflow {} scheduled for mp {}, transcription service job {}",
                    new String[]{wfId, mpId, jobId});
          } catch (Exception e) {
            logger.warn("Attach transcription workflow could NOT be scheduled for mp {}, amberscript job {}, {}: {}",
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

  private String removePrivateInfo(String unsafeString) {
    String safeString = unsafeString.replace(clientKey, "__api-key-was-hidden__");
    return safeString;
  }
}
