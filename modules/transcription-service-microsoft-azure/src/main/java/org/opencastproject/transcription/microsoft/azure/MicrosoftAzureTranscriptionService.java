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
package org.opencastproject.transcription.microsoft.azure;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.fn.Enrichments;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElement;
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
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscription;
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscriptionFile;
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscriptionFiles;
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscriptionJson;
import org.opencastproject.transcription.persistence.TranscriptionDatabase;
import org.opencastproject.transcription.persistence.TranscriptionDatabaseException;
import org.opencastproject.transcription.persistence.TranscriptionJobControl;
import org.opencastproject.transcription.persistence.TranscriptionProviderControl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component(immediate = true, service = {
    TranscriptionService.class, MicrosoftAzureTranscriptionService.class }, property = {
    "service.description=Microsoft Azure Transcription Service", "provider=microsoft.azure" })
public class MicrosoftAzureTranscriptionService extends AbstractJobProducer implements TranscriptionService {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureTranscriptionService.class);

  private static final String JOB_TYPE = "org.opencastproject.transcription.microsoft.azure";
  private static final String PROVIDER = "microsoft-azure-speech-services";
  private static final String DEFAULT_WORKFLOW_DEFINITION_ID = "microsoft-azure-attach-transcription";
  private static final String DEFAULT_LANGUAGE = "en-GB";

  private static final String DEFAULT_AZURE_BLOB_PATH = "";
  private static final String DEFAULT_AZURE_CONTAINER_NAME = "opencast-transcriptions";
  private static final float DEFAULT_MIN_CONFIDENCE = 1.0f;
  private static final int DEFAULT_SPLIT_TEXT_LINE_LENGTH = 100;
  private static final String KEY_ENABLED = "enabled";
  private static final String KEY_LANGUAGE = "language";
  private static final String KEY_AUTO_DETECT_LANGUAGES = "auto.detect.languages";
  private static final String KEY_WORKFLOW = "workflow";
  private static final String KEY_AZURE_STORAGE_ACCOUNT_NAME = "azure_storage_account_name";
  private static final String KEY_AZURE_ACCOUNT_ACCESS_KEY = "azure_account_access_key";
  private static final String KEY_AZURE_BOLB_PATH = "azure_blob_path";
  private static final String KEY_AZURE_CONTAINER_NAME = "azure_container_name";
  private static final String KEY_AZURE_SPEECH_SERVICES_ENDPOINT = "azure_speech_services_endpoint";
  private static final String KEY_COGNITIVE_SERVICES_SUBSCRIPTION_KEY = "azure_cognitive_services_subscription_key";
  private static final String KEY_AZURE_SPEECH_RECOGNITION_MIN_CONFIDENCE = "azure_speech_recognition_min_confidence";
  private static final String KEY_SPLIT_TEXT_LINE_LENGTH = "split.text.line.length";


  private AssetManager assetManager;
  private OrganizationDirectoryService organizationDirectoryService;
  private SecurityService securityService;
  private ServiceRegistry serviceRegistry;
  private TranscriptionDatabase database;
  private UserDirectoryService userDirectoryService;
  private WorkflowService workflowService;
  private Workspace workspace;
  private ScheduledExecutorService scheduledExecutorService;
  private Workflows wfUtil;
  private String systemAccount;
  private boolean enabled;
  private String language;
  private List<String> autodetectLanguages;
  private String workflowDefinitionId;
  private String azureStorageAccountName;
  private String azureAccountAccessKey;
  private String azureBlobPath;
  private String azureContainerName;
  private String azureSpeechServicesEndpoint;
  private String azureCognitiveServicesSubscriptionKey;
  private MicrosoftAzureAuthorization azureAuthorization;
  private MicrosoftAzureStorageClient azureStorageClient;
  private MicrosoftAzureSpeechServicesClient azureSpeechServicesClient;
  private Float azureSpeechRecognitionMinConfidence;
  private Integer splitTextLineLength;

  private enum Operation {
    StartTranscription
  }

  /**
   * A public constructor, required by OSGi.
   */
  public MicrosoftAzureTranscriptionService() {
    super(JOB_TYPE);
  }

  @Activate
  public void activate(ComponentContext cc) {
    super.activate(cc);
    systemAccount = OsgiUtil.getContextProperty(cc, OpencastConstants.DIGEST_USER_PROPERTY);
    logger.debug("Activating...");
    modified(cc);
  }

  @Modified
  public void modified(ComponentContext cc) {
    logger.debug("Updating config...");
    Option<Boolean> enabledOpt = OsgiUtil.getOptCfgAsBoolean(cc.getProperties(), KEY_ENABLED);
    if (enabledOpt.isSome()) {
      enabled = enabledOpt.get();
    } else {
      deactivate();
    }

    if (!enabled) {
      logger.info("Microsoft Azure Transcription service disabled."
          + " If you want to enable it, please update the service configuration.");
      return;
    }

    Option<String> azureStorageAccountNameKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(),
        KEY_AZURE_STORAGE_ACCOUNT_NAME);
    if (azureStorageAccountNameKeyOpt.isSome()) {
      azureStorageAccountName = azureStorageAccountNameKeyOpt.get();
    } else {
      logger.warn("Azure storage account name key was not set. Disabling Microsoft Azure transcription service.");
      deactivate();
      return;
    }

    Option<String> azureAccountAccessKeyKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(), KEY_AZURE_ACCOUNT_ACCESS_KEY);
    if (azureAccountAccessKeyKeyOpt.isSome()) {
      azureAccountAccessKey = azureAccountAccessKeyKeyOpt.get();
    } else {
      logger.warn("Azure storage account access key was not set. Disabling Microsoft Azure transcription service.");
      deactivate();
      return;
    }

    Option<String> azureSpeechServicesKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(),
        KEY_AZURE_SPEECH_SERVICES_ENDPOINT);
    if (azureSpeechServicesKeyOpt.isSome()) {
      azureSpeechServicesEndpoint = azureSpeechServicesKeyOpt.get();
    } else {
      logger.warn("Azure speech services endpoint was not set. Disabling Microsoft Azure transcription service.");
      deactivate();
      return;
    }

    Option<String> azureCognitiveServicesSubscriptionKeyKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(),
        KEY_COGNITIVE_SERVICES_SUBSCRIPTION_KEY);
    if (azureCognitiveServicesSubscriptionKeyKeyOpt.isSome()) {
      azureCognitiveServicesSubscriptionKey = azureCognitiveServicesSubscriptionKeyKeyOpt.get();
    } else {
      logger.warn("Azure cognitive services subscription key was not set. "
          + "Disabling Microsoft Azure transcription service.");
      deactivate();
      return;
    }

    // optional values
    Option<String> workflowKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(), KEY_WORKFLOW);
    if (workflowKeyOpt.isSome()) {
      workflowDefinitionId = workflowKeyOpt.get();
      logger.info("Workflow is set to '{}'.", workflowDefinitionId);
    } else {
      workflowDefinitionId = DEFAULT_WORKFLOW_DEFINITION_ID;
      logger.info("Default workflow '{}' will be used.", workflowDefinitionId);
    }

    Option<String> azureBlobPathKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(), KEY_AZURE_BOLB_PATH);
    if (azureBlobPathKeyOpt.isSome()) {
      azureBlobPath = azureBlobPathKeyOpt.get();
    } else {
      logger.debug("Azure blob path was not set, using default path.");
      azureBlobPath = DEFAULT_AZURE_BLOB_PATH;
    }

    Option<String> languageOpt = OsgiUtil.getOptCfg(cc.getProperties(), KEY_LANGUAGE);
    if (languageOpt.isSome()) {
      language = languageOpt.get();
      logger.info("Default Language is set to '{}'.", language);
    } else {
      language = DEFAULT_LANGUAGE;
      logger.info("Default language '{}' will be used.", language);
    }

    autodetectLanguages = new ArrayList<>();
    Option<String> autoDetectLanguagesOpt = OsgiUtil.getOptCfg(cc.getProperties(), KEY_AUTO_DETECT_LANGUAGES);
    if (languageOpt.isSome()) {
      for (String lang : StringUtils.split(autoDetectLanguagesOpt.get(), ",")) {
        if (StringUtils.isNotBlank(lang)) {
          autodetectLanguages.add(StringUtils.trimToEmpty(lang));
        }
      }
    }

    Option<String> azureContainerNameKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(), KEY_AZURE_CONTAINER_NAME);
    if (azureContainerNameKeyOpt.isSome()) {
      azureContainerName = azureContainerNameKeyOpt.get();
    } else {
      logger.debug("Azure storage container name was not set, using default path.");
      azureContainerName = DEFAULT_AZURE_CONTAINER_NAME;
    }

    Option<String> azureSpeechRecognitionMinConfidenceKeyOpt = OsgiUtil.getOptCfg(cc.getProperties(),
        KEY_AZURE_SPEECH_RECOGNITION_MIN_CONFIDENCE);
    if (azureSpeechRecognitionMinConfidenceKeyOpt.isSome()) {
      String azureSpeechRecognitionMinConfidenceStr = azureSpeechRecognitionMinConfidenceKeyOpt.get();
      try {
        azureSpeechRecognitionMinConfidence = Float.valueOf(azureSpeechRecognitionMinConfidenceStr);
      } catch (NumberFormatException e) {
        logger.error("Azure speech recognition min confidence value is not valid. "
            + "Please set a value between 0.0 and 1.0. "
            + "Setting to default value of {}.", DEFAULT_MIN_CONFIDENCE);
        azureSpeechRecognitionMinConfidence = DEFAULT_MIN_CONFIDENCE;
      }
    } else {
      logger.debug("Azure speech recognition min confidence value was not set. Setting to default value of {}.",
          DEFAULT_MIN_CONFIDENCE);
      azureSpeechRecognitionMinConfidence = DEFAULT_MIN_CONFIDENCE;
    }

    Option<String> splitTextLineLengthOpt = OsgiUtil.getOptCfg(cc.getProperties(), KEY_SPLIT_TEXT_LINE_LENGTH);
    if (splitTextLineLengthOpt.isSome()) {
      try {
        splitTextLineLength = Integer.parseInt(splitTextLineLengthOpt.get());
      } catch (NumberFormatException e) {
        splitTextLineLength = DEFAULT_SPLIT_TEXT_LINE_LENGTH;
        logger.error("Invalid configuration value for '{}'. Set default value {}.", KEY_SPLIT_TEXT_LINE_LENGTH,
            DEFAULT_SPLIT_TEXT_LINE_LENGTH);
      }
    } else {
      logger.debug("Configuration value for '{}' was not set. Setting to default value of {}.",
          KEY_SPLIT_TEXT_LINE_LENGTH, DEFAULT_MIN_CONFIDENCE);
      splitTextLineLength = DEFAULT_SPLIT_TEXT_LINE_LENGTH;
    }

    //// create Azure storage client
    try {
      azureAuthorization = new MicrosoftAzureAuthorization(azureStorageAccountName, azureAccountAccessKey);
      azureStorageClient = new MicrosoftAzureStorageClient(azureAuthorization);
    } catch (MicrosoftAzureStorageClientException e) {
      logger.error("Unable to create Microsoft Azure storage client. "
          + "Deactivating Microsoft Azure Transcription service.", e);
      deactivate();
      return;
    }

    // create Azure Speech Services client
    azureSpeechServicesClient = new MicrosoftAzureSpeechServicesClient(
        azureSpeechServicesEndpoint, azureCognitiveServicesSubscriptionKey);

    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
      try {
        scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // pending task took to long
        // pending task will be restarted on next run
      }
    }
    scheduledExecutorService = Executors.newScheduledThreadPool(2);
    scheduledExecutorService.scheduleWithFixedDelay(new WorkflowDispatcher(), 120, 120, TimeUnit.SECONDS);
    logger.info("Activated.");
  }

  @Deactivate
  public void deactivate() {
    enabled = false;
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
      try {
        scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // pending task took to long
        // pending task will be restarted on next run
      }
    }
    azureAuthorization = null;
    azureStorageClient = null;
    azureSpeechServicesClient = null;
    logger.info("Deactivated.");
  }

  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    op = Operation.valueOf(operation);
    switch (op) {
      case StartTranscription:
        long jobId = job.getId();
        String mpId = arguments.get(0);
        Track track = (Track) MediaPackageElementParser.getFromXml(arguments.get(1));
        String languageCode = arguments.get(2);
        if (StringUtils.isBlank(languageCode)) {
          languageCode = getLanguage();
        }
        return createTranscriptionJob(jobId, mpId, track, languageCode);
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
    }
  }

  @Override
  public Job startTranscription(String mpId, Track track) throws TranscriptionServiceException {
    return startTranscription(mpId, track, getLanguage());
  }

  @Override
  public Job startTranscription(String mpId, Track track, String... args) throws TranscriptionServiceException {
    try {
      List<String> jobArgs = new ArrayList<>(2 + args.length);
      jobArgs.add(mpId);
      jobArgs.add(MediaPackageElementParser.getAsXml(track));
      jobArgs.addAll(Arrays.asList(args));
      return serviceRegistry.createJob(JOB_TYPE, Operation.StartTranscription.toString(), jobArgs);
    } catch (ServiceRegistryException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to create transcription job for media package '%s'.", mpId), e);
    } catch (MediaPackageException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to to parse track from media package '%s'.", mpId), e);
    }
  }

  @Override
  public MediaPackageElement getGeneratedTranscription(String mpId, String jobId)
          throws TranscriptionServiceException {
    MicrosoftAzureSpeechTranscription transcription;
    try {
      transcription = azureSpeechServicesClient.getTranscriptionById(jobId);
    } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureSpeechClientException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to get transcription '%s' for media package '%s'.", jobId, mpId), e);
    }
    URI transcriptionFileUri;
    try {
      MicrosoftAzureSpeechTranscriptionJson transcriptionJson = getTranscriptionJson(mpId, transcription);
      transcriptionFileUri = MicrosoftAzureSpeechServicesClient.writeTranscriptionFile(transcriptionJson,
          workspace, "webvtt", azureSpeechRecognitionMinConfidence, splitTextLineLength);
    } catch (IOException | MicrosoftAzureNotFoundException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to download transcription file for media package '%s'.", mpId), e);
    }
    return MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
        transcriptionFileUri, MediaPackageElement.Type.Attachment, new MediaPackageElementFlavor("captions", "vtt"));
  }

  @Override
  public void transcriptionDone(String mpId, Object results) throws TranscriptionServiceException {
    MicrosoftAzureSpeechTranscription transcription = (MicrosoftAzureSpeechTranscription) results;
    logger.info("Transcription job {} for media package {} done.", transcription.getID(), mpId);
    // delete audio source files in Azure storage
    try {
      deleteTranscriptionSourceFiles(mpId, transcription.getID());
    } catch (TranscriptionServiceException e) {
      logger.warn("Unable to delete transcription source files for media package {} after transcription job done.",
          mpId, e);
    }
    try {
      database.updateJobControl(transcription.getID(), TranscriptionJobControl.Status.TranscriptionComplete.name());
    } catch (TranscriptionDatabaseException e) {
      throw new TranscriptionServiceException(String.format(
          "Transcription job for media package '%s' succeeded but storing job status in the database failed."
          , mpId), e);
    }
  }

  @Override
  public void transcriptionError(String mpId, Object results) throws TranscriptionServiceException {
    MicrosoftAzureSpeechTranscription transcription = (MicrosoftAzureSpeechTranscription) results;
    String message = "";
    if (transcription != null && transcription.properties != null && transcription.properties.containsKey("error")) {
      Map<String, Object> errorInfo = (Map<String, Object>) transcription.properties.get("error");
      message = String.format(" Microsoft error code %s: %s", errorInfo.getOrDefault("code", "UNKNOWN"),
          errorInfo.getOrDefault("message", "No info"));
    }
    logger.info("Transcription job {} for media package {} failed.{}", transcription.getID(), mpId, message);
    // delete audio source files in Azure storage
    try {
      deleteTranscriptionSourceFiles(mpId, transcription.getID());
    } catch (TranscriptionServiceException e) {
      logger.warn("Unable to delete transcription source files for media package {} after transcription kob failure.",
          mpId, e);
    }
    try {
      database.updateJobControl(transcription.getID(), TranscriptionJobControl.Status.Error.name());
    } catch (TranscriptionDatabaseException e) {
      throw new TranscriptionServiceException(String.format(
          "Transcription job for media package '%s' failed and storing job status in the database failed too."
          , mpId), e);
    }
  }

  @Override
  public String getLanguage() {
    return language;
  }

  @Override
  public Map<String, Object> getReturnValues(String mpId, String jobId) throws TranscriptionServiceException {
    throw new NotImplementedException();
  }

  public String createTranscriptionJob(long jobId, String mpId, Track track, String language)
          throws TranscriptionServiceException {
    // load media file into workspace
    File trackFile;
    try {
      trackFile = workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new TranscriptionServiceException(String.format("Track %s not found.", track.getURI()), e);
    } catch (IOException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to get track %s for transcription.", track.getURI()), e);
    }
    // upload media file to azure storage
    //// assure azure storage container exists
    try {
      azureStorageClient.createContainer(azureContainerName);
    } catch (IOException | MicrosoftAzureStorageClientException | MicrosoftAzureNotAllowedException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to query or create a storage container '%s' on Microsoft Azure.", azureContainerName), e);
    }
    //// upload file to azure storage container
    String azureBlobUrl;
    try {
      String filename = String.format("%d-%s.%s", jobId, mpId, FilenameUtils.getExtension(trackFile.getName()));
      azureBlobUrl = azureStorageClient.uploadFile(trackFile, azureContainerName, azureBlobPath, filename);
    } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureStorageClientException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to upload track %s from media package '%s' to Microsoft Azure storage container '%s'.",
          track.getURI(), mpId, azureContainerName), e);
    }
    // start azure transcription job
    List<String> contentUrls = Arrays.asList(azureBlobUrl);
    String azureDestContainerUrl = String.format("%s?%s", azureStorageClient.getContainerUrl(azureContainerName),
        azureAuthorization.generateServiceSasToken("cw", null, null, azureContainerName, "c"));
    MicrosoftAzureSpeechTranscription transcription;
    try {
      transcription = azureSpeechServicesClient.createTranscription(contentUrls,
          azureDestContainerUrl, String.format("Transcription job %d", jobId), language, autodetectLanguages,
          null, null);
      logger.info("Started transcription of {} from media package '{}' on Microsoft Azure Speech Services at {}",
          track.getURI(), mpId, transcription.self);
    } catch (MicrosoftAzureNotAllowedException | IOException | MicrosoftAzureSpeechClientException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to create transcription of track %s from media package '%s' "
              + "in Microsoft Azure storage container '%s'.",
          track.getURI(), mpId, azureContainerName), e);
    }
    // store transcription job ID and status
    try {
      database.storeJobControl(mpId, track.getIdentifier(), transcription.getID(),
          TranscriptionJobControl.Status.InProgress.name(),
          track.getDuration() == null ? 0 : track.getDuration(), new Date(), PROVIDER);
    } catch (TranscriptionDatabaseException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to store transcription job of track %s from media package '%s' in the database.",
          track.getURI(), mpId), e);
    }
    // return transcription job ID
    return transcription.getID();
  }

  MicrosoftAzureSpeechTranscriptionJson getTranscriptionJson(String mpId,
      MicrosoftAzureSpeechTranscription transcription)
          throws TranscriptionServiceException, MicrosoftAzureNotFoundException {
    if (!transcription.isSucceeded()) {
      if (transcription.isRunning()) {
        throw new TranscriptionServiceException(String.format("Unable to get generated transcription. "
            + "Transcription job '%s' for media package '%s' is currently running.", transcription.getID(), mpId));
      } else if (transcription.isFailed()) {
        throw new TranscriptionServiceException(String.format("Unable to get generated transcription. "
            + "Transcription job '%s' for media package '%s' is failed.", transcription.getID(), mpId));
      }
    }
    // query transcription files
    MicrosoftAzureSpeechTranscriptionFiles transcriptionFiles;
    try {
      transcriptionFiles = azureSpeechServicesClient.getTranscriptionFilesById(transcription.getID());
    } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureSpeechClientException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to get transcription files '%s' for media package '%s'.", transcription.getID(), mpId), e);
    }
    // download transcription file to workspace
    MicrosoftAzureSpeechTranscriptionFile transcriptionFile = null;
    for (MicrosoftAzureSpeechTranscriptionFile tf : transcriptionFiles.values) {
      if (tf.isTranscriptionFile()) {
        transcriptionFile = tf;
        break;
      }
    }
    if (transcriptionFile == null) {
      // get more files with transcriptionFiles.nextLink
      // TODO
      throw new NotImplementedException("At least one transcription file should be provided.");
    }

    try {
      return MicrosoftAzureSpeechServicesClient
          .getTranscriptionJson(transcriptionFile);
    } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureSpeechClientException
             | MicrosoftAzureNotFoundException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to download transcription file '%s' for media package '%s'.", transcriptionFile.self, mpId), e);
    }
  }

  String startWorkflow(String mpId, MicrosoftAzureSpeechTranscription transcription)
          throws TranscriptionDatabaseException, NotFoundException, WorkflowDatabaseException,
          TranscriptionServiceException, MicrosoftAzureNotFoundException {
    MicrosoftAzureSpeechTranscriptionJson transcriptionJson = getTranscriptionJson(mpId, transcription);
    String transcriptionLocale = transcriptionJson.getRecognizedLocale();

    DefaultOrganization defaultOrg = new DefaultOrganization();
    securityService.setOrganization(defaultOrg);
    securityService.setUser(SecurityUtil.createSystemUser(systemAccount, defaultOrg));

    // Find the episode
    final AQueryBuilder q = assetManager.createQuery();
    final AResult r = q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest())).run();
    if (r.getSize() == 0) {
      // Media package not archived yet? Skip until next time.
      logger.warn("Media package {} has not been archived yet. Skipped.", mpId);
      return null;
    }

    String org = Enrichments.enrich(r).getSnapshots().head2().getOrganizationId();
    Organization organization = organizationDirectoryService.getOrganization(org);
    if (organization == null) {
      logger.warn("Media package {} has an unknown organization {}. Skipped.", mpId, org);
      return null;
    }
    securityService.setOrganization(organization);

    // Build workflow
    Map<String, String> params = new HashMap<>();
    params.put("transcriptionJobId", transcription.getID());
    String locale = "";
    String language = "";
    if (StringUtils.isNotBlank(transcriptionLocale)) {
      locale = transcriptionLocale;
      language = Locale.forLanguageTag(transcriptionLocale).getLanguage();
    }
    params.put("transcriptionLocale", locale);
    params.put("transcriptionLocaleSet", Boolean.toString(!StringUtils.isEmpty(locale)));
    params.put("transcriptionLocaleSubtypeSuffix", !StringUtils.isEmpty(locale) ? "+" + locale : "");
    params.put("transcriptionLanguage", language);
    params.put("transcriptionLanguageSet", Boolean.toString(!StringUtils.isEmpty(language)));
    params.put("transcriptionLanguageSubtypeSuffix", !StringUtils.isEmpty(language) ? "+" + language : "");
    WorkflowDefinition wfDef = workflowService.getWorkflowDefinitionById(workflowDefinitionId);

    // Apply workflow
    // wfUtil is only used by unit tests
    Workflows workflows = wfUtil != null ? wfUtil : new Workflows(assetManager, workflowService);
    Set<String> mpIds = new HashSet<>();
    mpIds.add(mpId);
    List<WorkflowInstance> wfList = workflows
        .applyWorkflowToLatestVersion(mpIds, ConfiguredWorkflow.workflow(wfDef, params)).toList();
    return wfList.size() > 0 ? Long.toString(wfList.get(0).getId()) : null;
  }

  public void deleteTranscription(String mpId, String transcriptionId)
          throws TranscriptionServiceException, TranscriptionDatabaseException {
    TranscriptionJobControl transcriptionJobControl = database.findByJob(transcriptionId);
    TranscriptionJobControl.Status transcriptionJobControlStatus = TranscriptionJobControl.Status.valueOf(
        transcriptionJobControl.getStatus());
    if (transcriptionJobControlStatus != TranscriptionJobControl.Status.Closed
        && transcriptionJobControlStatus != TranscriptionJobControl.Status.Canceled
        && transcriptionJobControlStatus != TranscriptionJobControl.Status.Error) {
      throw new TranscriptionServiceException(String.format("Abort deleting transcription %s with invalid status '%s'.",
          transcriptionId, transcriptionJobControl.getStatus()));
    }
    deleteTranscriptionSourceFiles(mpId, transcriptionId);
    try {
      azureSpeechServicesClient.deleteTranscription(transcriptionId);
    } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureSpeechClientException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to delete transcription '%s' for media package '%s'.", transcriptionId, mpId), e);
    }
    database.deleteJobControl(transcriptionJobControl.getTranscriptionJobId());
  }

  public void deleteTranscriptionSourceFiles(String mpId, String transcriptionId)
          throws TranscriptionServiceException {
    MicrosoftAzureSpeechTranscriptionFiles transcriptionFiles;
    try {
      transcriptionFiles = azureSpeechServicesClient.getTranscriptionFilesById(transcriptionId);
    } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureSpeechClientException e) {
      throw new TranscriptionServiceException(String.format(
          "Unable to get for transcription '%s' from media package '%s'.", transcriptionId, mpId), e);
    } catch (MicrosoftAzureNotFoundException e) {
      // catch deleting non-existing file
      logger.debug("Failed to get non existing transcription files from media package {} for deleting.", mpId, e);
      return;
    }
    for (MicrosoftAzureSpeechTranscriptionFile transcriptionFile : transcriptionFiles.values) {
      if (!transcriptionFile.isTranscriptionFile()) {
        continue;
      }
      MicrosoftAzureSpeechTranscriptionJson transcriptionJson;
      try {
        transcriptionJson = MicrosoftAzureSpeechServicesClient
            .getTranscriptionJson(transcriptionFile);
      } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureSpeechClientException e) {
        throw new TranscriptionServiceException(String.format(
            "Unable to download transcription file '%s' for media package '%s'.", transcriptionFile.self, mpId), e);
      } catch (MicrosoftAzureNotFoundException e) {
        // catch deleting non-existing file
        logger.debug("Failed to get non existing transcription file {} from media package {} for deleting.",
            transcriptionFile.self, mpId, e);
        continue;
      }
      if (StringUtils.isNotBlank(transcriptionJson.source)) {
        try {
          azureStorageClient.deleteFile(new URL(transcriptionJson.source));
        } catch (IOException | MicrosoftAzureNotAllowedException | MicrosoftAzureStorageClientException e) {
          throw new TranscriptionServiceException(String.format(
              "Unable to delete audio source file for media package %s.", mpId, e));
        }
      }
    }
  }

  class WorkflowDispatcher implements Runnable {

    @Override
    public void run() {
      if (!enabled) {
        logger.debug("Service disabled, cancel processing.");
        return;
      }
      logger.debug("Run jobs handling loop for transcription provider {}.", PROVIDER);
      long providerId;
      try {
        TranscriptionProviderControl providerInfo = database.findIdByProvider(PROVIDER);
        if (providerInfo != null) {
          providerId = providerInfo.getId();
        } else {
          logger.debug("No jobs yet for provider {}.", PROVIDER);
          return;
        }
        // handle jobs in progress
        for (TranscriptionJobControl jobControl : database.findByStatus(
            TranscriptionJobControl.Status.InProgress.name())) {
          if (providerId != jobControl.getProviderId()) {
            continue;
          }
          String mpId = jobControl.getMediaPackageId();
          String transcriptionId = jobControl.getTranscriptionJobId();
          try {
            MicrosoftAzureSpeechTranscription transcription = azureSpeechServicesClient.getTranscriptionById(
                transcriptionId);
            // check and update job status
            if (!transcription.isRunning()) {
              if (transcription.isFailed()) {
                transcriptionError(mpId, transcription);
              } else if (transcription.isSucceeded()) {
                transcriptionDone(mpId, transcription);
              }
            }
          } catch (MicrosoftAzureNotAllowedException | IOException | MicrosoftAzureSpeechClientException e) {
            logger.error("Unable to get or update transcription {} or transcription file from media package {}.",
                transcriptionId, mpId, e);
          } catch (TranscriptionServiceException e) {
            logger.error(e.getMessage(), e);
          }
        }
        // handle completed jobs
        for (TranscriptionJobControl jobControl : database.findByStatus(
            TranscriptionJobControl.Status.TranscriptionComplete.name())) {
          if (providerId != jobControl.getProviderId()) {
            continue;
          }
          String mpId = jobControl.getMediaPackageId();
          String transcriptionId = jobControl.getTranscriptionJobId();
          try {
            MicrosoftAzureSpeechTranscription transcription = azureSpeechServicesClient.getTranscriptionById(
                transcriptionId);
            // start workflow
            String workflowId = startWorkflow(mpId, transcription);
            // update db
            if (workflowId != null) {
              database.updateJobControl(transcriptionId, TranscriptionJobControl.Status.Closed.name());
              logger.info("Attach transcription workflow {} scheduled for mp {}, microsoft azure transcription job {}",
                  workflowId, mpId, transcriptionId);
            }
          } catch (MicrosoftAzureNotAllowedException | MicrosoftAzureNotFoundException | IOException
                   | MicrosoftAzureSpeechClientException e) {
            logger.warn("Unable to get transcription {} or transcription file from media package {}.",
                transcriptionId, mpId, e);
          } catch (TranscriptionServiceException e) {
            logger.warn(e.getMessage(), e);
          } catch (NotFoundException e) {
            logger.warn("Unable to load organization.", e);
          }
        }
        // cleanup all old jobs
        for (TranscriptionJobControl jobControl : database.findByStatus(
            TranscriptionJobControl.Status.Closed.name(), TranscriptionJobControl.Status.Error.name())) {
          if (providerId != jobControl.getProviderId()) {
            continue;
          }
          String mpId = jobControl.getMediaPackageId();
          String transcriptionId = jobControl.getTranscriptionJobId();
          if (Instant.now().minus(7, ChronoUnit.DAYS).isAfter(jobControl.getDateCreated().toInstant())) {
            try {
              deleteTranscription(jobControl.getMediaPackageId(), jobControl.getTranscriptionJobId());
            } catch (TranscriptionServiceException e) {
              logger.error("Unable to delete transcription {} or transcription files from media package {}.",
                  transcriptionId, mpId, e);
            }
          }
        }
      } catch (TranscriptionDatabaseException e) {
        logger.warn("Could not read or update transcription job control database", e);
      } catch (WorkflowDatabaseException e) {
        logger.warn("Unable to get workflow definition.", e);
      } catch (Throwable e) {
        // catch all
        logger.error("Something went wrong in transcription job processing loop. Exception unhandled!!!", e);
      }
    }
  }

  // Only used by unit tests!
  void setWfUtil(Workflows wfUtil) {
    this.wfUtil = wfUtil;
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

//  @Reference
//  public void setWorkingFileRepository(WorkingFileRepository wfr) {
//    this.wfr = wfr;
//  }

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
}
