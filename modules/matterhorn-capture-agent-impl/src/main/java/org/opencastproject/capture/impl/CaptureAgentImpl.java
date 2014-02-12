/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.impl;

import static org.opencastproject.util.MimeType.mimeType;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.ScheduledEvent;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.jobs.AgentConfigurationJob;
import org.opencastproject.capture.impl.jobs.AgentStateJob;
import org.opencastproject.capture.impl.jobs.JobParameters;
import org.opencastproject.capture.impl.jobs.LoadRecordingsJob;
import org.opencastproject.capture.pipeline.GStreamerCapturePipeline;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.XProperties;
import org.opencastproject.util.ZipUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.component.ComponentContext;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import javax.activation.MimetypesFileTypeMap;
import org.opencastproject.capture.api.ConfidenceMonitor;
import org.opencastproject.capture.pipeline.GStreamerMonitoringPipeline;
import org.opencastproject.capture.pipeline.GStreamerPipeline;

/**
 * Implementation of the Capture Agent: using gstreamer, generates several Pipelines to store several tracks from a
 * certain recording.
 */
public class CaptureAgentImpl implements CaptureAgent, StateService, ManagedService, ConfigurationManagerListener,
        CaptureFailureHandler, ConfidenceMonitor, MonitoringListener {
  // The amount of time to wait until shutting down the pipeline manually.
  // private static final long DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT = 60000L;

  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);

  /** The default maximum length to capture, measured in seconds. */
  public static final long DEFAULT_MAX_CAPTURE_LENGTH = 8 * CaptureParameters.HOURS;

  /** The amount of time between the recording load task running, measured in seconds **/
  private static final int RECORDING_LOAD_TASK_DELAY = 60;

  /** Keeps the recordings which have not been successfully ingested yet. **/
  private Map<String, AgentRecording> pendingRecordings = new ConcurrentHashMap<String, AgentRecording>();

  /** Keeps the recordings which have been successfully ingested. */
  private Map<String, AgentRecording> completedRecordings = new ConcurrentHashMap<String, AgentRecording>();

  /** The agent's name. */
  private String agentName = null;

  /** The agent's current state. Used for logging. */
  private String agentState = null;

  /** A pointer to the scheduler. */
  private SchedulerImpl scheduler = null;

  /** The scheduler the agent will use to schedule any recurring events */
  private org.quartz.Scheduler agentScheduler = null;

  /** The configuration manager for the agent. */
  private ConfigurationManager configService = null;

  /** The (remote) service registry */
  private ServiceRegistry serviceRegistry = null;

  /** The http client used to communicate with the core */
  private TrustedHttpClient client = null;

  /** GStreamer Pipeline builder **/
  private GStreamerPipeline pipeline = null;

  /** Indicates the ID of the recording currently being recorded. **/
  private String currentRecID = null;

  /** Is confidence monitoring enabled? */
  private boolean confidence = false;

  /** Stores the start time of the current recording */
  private long startTime = -1L;

  /** Stores the current bundle context */
  private ComponentContext context = null;

  /** If the CaptureAgentImpl has its updated quartz configuration this is true. **/
  private boolean updated = false;

  /** If the ConfigurationManager has its updated quartz configuration this is true. **/
  private boolean refreshed = false;

  /** The last properties the CaptureAgentImpl was updated by felix with. **/
  private Dictionary<String, String> cachedProperties = new Hashtable<String, String>();

  /** Collection with running monitoring devices and their type (audio, video or av). **/
  protected Map<String, String> monitoringDeviceFriendlyNames = new Hashtable<String, String>();

  protected Map<String, String> monitoringDevicesVideoLocation = new Hashtable<String, String>();
  /** Synchronized collection for audio RMS values at specific time (unix timestamp). **/
  protected Map<String, SortedMap<Long, Double>> monitoringDevicesRmsValues = new Hashtable<String, SortedMap<Long, Double>>();

  private int maxRmsValuesPerDevice = 60;

  /**
   * Sets the configuration service form which this capture agent should draw its configuration data.
   *
   * @param cfg
   *          The configuration service.
   * @throws ConfigurationException
   */
  public void setConfigService(ConfigurationManager cfg) throws ConfigurationException {
    configService = cfg;
    agentName = configService.getItem(CaptureParameters.AGENT_NAME);
    configService.registerListener(this);
  }

  /**
   * Returns the configuration service form which this capture agent should draw its configuration data.
   *
   * @return The configuration service.
   */
  public ConfigurationManager getConfigService() {
    return configService;
  }

  protected SchedulerImpl getSchedulerImpl() {
    return scheduler;
  }

  /**
   * Sets the http client which this service uses to communicate with the core.
   *
   * @param c
   *          The client object.
   */
  void setTrustedClient(TrustedHttpClient c) {
    client = c;
    if (scheduler != null) {
      scheduler.setTrustedClient(c);
    }
  }

  /**
   * OSGi callback that sets a reference to the service registry.
   *
   * @param serviceRegistry
   *          the registry
   */
  void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#startCapture()
   */
  @Override
  public String startCapture() {

    logger.debug("startCapture()");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e);
      return null;
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e);
      return null;
    }

    return startCapture(pack, configService.getAllProperties());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#startCapture(MediaPackage)
   */
  @Override
  public String startCapture(MediaPackage mediaPackage) {
    logger.debug("startCapture(mediaPackage): {}", mediaPackage);
    return startCapture(mediaPackage, configService.getAllProperties());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#startCapture(Properties)
   */
  @Override
  public String startCapture(Properties properties) {
    // Creates default MediaPackage
    // TODO: This is also done in the RecordingImpl class, should this code go away?
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Configuration Exception creating media package: {}.", e);
      return null;
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e);
      return null;
    }

    return startCapture(pack, properties);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#startCapture(MediaPackage, Properties)
   */
  @Override
  public String startCapture(MediaPackage mediaPackage, Properties properties) {
    logger.debug("startCapture(mediaPackage, properties): {} {}", mediaPackage, properties);
    // Stop the confidence monitoring if its pipeline is running.
    // stopMonitoring();

    // Check to make sure we're not already capturing something
    if (currentRecID != null || !agentState.equals(AgentState.IDLE)) {
      logger.warn("Unable to start capture, a different capture is still in progress in {}.",
              pendingRecordings.get(currentRecID).getBaseDir().getAbsolutePath());
      // Set the recording's state to error
      if (properties != null && properties.getProperty(CaptureParameters.RECORDING_ID) != null) {
        setRecordingState(properties.getProperty(CaptureParameters.RECORDING_ID), RecordingState.CAPTURE_ERROR);
      } else {
        setRecordingState("Unscheduled-" + agentName + "-" + System.currentTimeMillis(), RecordingState.CAPTURE_ERROR);
      }
      return null;
    } else {
      setAgentState(AgentState.CAPTURING);
    }

    // Generate a combined properties object for this capture
    Properties capProperties = configService.merge(properties, false);

    // Create the recording
    RecordingImpl newRec = createRecording(mediaPackage, capProperties);
    if (newRec == null) {
      if (capProperties != null && capProperties.contains(CaptureParameters.RECORDING_ID)) {
        resetOnFailure((String) capProperties.get(CaptureParameters.RECORDING_ID));
      } else {
        resetOnFailure("Unscheduled-" + agentName + "-" + System.currentTimeMillis());
      }
      return null;
    }

    String recordingID = newRec.getID();

    try {
      pipeline = new GStreamerCapturePipeline(this);
      if (confidence) {
        pipeline.setMonitoringListener(this);
      }
      ((GStreamerCapturePipeline)pipeline).start(newRec);
    } catch (UnableToStartCaptureException exception) {
      logger.error(exception.getMessage());
      pipeline = null;
      resetOnFailure(newRec.getID());
      return null;
    }

    // Keep track of how long it has been since the capture started.
    startTime = System.currentTimeMillis();

    // Great, capture is running. Set the agent state appropriately.
    setRecordingState(recordingID, RecordingState.CAPTURING);
    // If the recording does *not* have a scheduled endpoint then schedule one.
    if (newRec.getProperty(CaptureParameters.RECORDING_END) == null) {
      if (!scheduleStop(newRec.getID())) {
        // If the attempt to schedule an end to the recording fails then shut everything down.
        stopCapture(newRec.getID(), false);
        resetOnFailure(newRec.getID());
      }
    }

    serializeRecording(recordingID);
    return recordingID;
  }

  private long getStopCaptureTimeout() {
    // Get the timeout value to wait for a capture to start.
    long timeout = GStreamerCapturePipeline.DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT;
    if (configService.getItem(CaptureParameters.RECORDING_SHUTDOWN_TIMEOUT) == null) {
      logger.warn("Unable to find shutdown timeout value.  Assuming 1 minute.  Missing key is {}.",
              CaptureParameters.RECORDING_SHUTDOWN_TIMEOUT);
    } else {
      timeout = Long.parseLong(configService.getItem(CaptureParameters.RECORDING_SHUTDOWN_TIMEOUT)) * 1000L;
    }
    return timeout;
  }

  /**
   * Creates a RecordingImpl instance used in a capture. Also adds the recording to the agent's internal list of
   * upcoming recordings.
   *
   * @param mediaPackage
   *          The media package to create the recording around
   * @param properties
   *          The properties of the recording
   * @return The RecordingImpl instance, or null in the case of an error
   */
  protected RecordingImpl createRecording(MediaPackage mediaPackage, Properties properties) {
    // Creates a new recording object, checking if it was correctly initialized
    RecordingImpl newRec = null;
    try {
      newRec = new RecordingImpl(mediaPackage, configService.merge(properties, false));
    } catch (IllegalArgumentException e) {
      logger.error("Recording not created: {}", e);
      return null;
    } catch (IOException e) {
      logger.error("Recording not created due to an I/O Exception: {}", e);
      return null;
    }
    // Checks there is no duplicate ID
    String recordingID = newRec.getID();
    if (pendingRecordings.containsKey(recordingID)) {
      logger.error("Can't create a recording with ID {}: there is already another recording with such ID", recordingID);
      return null;
    } else {
      pendingRecordings.put(recordingID, newRec);
      currentRecID = recordingID;
      return newRec;
    }
  }

  /**
   * Convenience method to reset an agent when a capture fails to start.
   *
   * @param recordingID
   *          The recordingID of the capture which failed to start.
   */
  @Override
  public void resetOnFailure(String recordingID) {
    setAgentState(AgentState.IDLE);
    setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
    currentRecID = null;
  }

  /**
   * Schedules a stopCapture call for unscheduled captures.
   *
   * @param recordingID
   *          The recordingID to stop.
   * @return true if the stop was scheduled, false otherwise.
   */
  protected boolean scheduleStop(String recordingID) {
    String maxLength = pendingRecordings.get(recordingID).getProperty(CaptureParameters.CAPTURE_MAX_LENGTH);
    long length = 0L;
    if (maxLength != null) {
      // Try and parse the value found, falling back to the agent's hardcoded max on error
      try {
        length = Long.parseLong(maxLength);
      } catch (NumberFormatException e) {
        configService.setItem(CaptureParameters.CAPTURE_MAX_LENGTH,
                String.valueOf(CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH));
        length = CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH;
      }
    } else {
      configService.setItem(CaptureParameters.CAPTURE_MAX_LENGTH,
              String.valueOf(CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH));
      length = CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH;
    }

    // Convert from seconds to milliseconds
    length = length * 1000L;
    Date stop = new Date(length + System.currentTimeMillis());
    if (scheduler != null) {
      logger.debug("Scheduling stop for recording {} at {}.", recordingID, stop);
      return scheduler.scheduleUnscheduledStopCapture(recordingID, stop);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture(boolean)
   */
  @Override
  public boolean stopCapture(boolean immediateIngest) {
    logger.debug("stopCapture() called.");
    setAgentState(AgentState.SHUTTING_DOWN);
    // If pipe is null and no mock capture is on
    if (pipeline == null || pipeline.isPipelineNull()) {
      logger.warn("Pipeline is null, this is normal if running a mock capture.");
      setAgentState(AgentState.IDLE);
    } else {
      long timeout = getStopCaptureTimeout();
      pipeline.stop(timeout);
      pipeline = null;
      cleanupMonitoring();
      // Checks there is a currentRecID defined --should always be
      if (currentRecID == null) {
        logger.warn("There is no currentRecID assigned, but the Pipeline was not null!");
        setAgentState(AgentState.IDLE);
        return false;
      }
    }

    AgentRecording theRec = pendingRecordings.get(currentRecID);
    if (theRec == null) {
      logger.info("Stop capture called, but no capture running. This is normal if running a mock capture.");
      return true;
    }

    // Clears currentRecID to indicate no recording is on
    currentRecID = null;

    // Update the states of everything.
    setRecordingState(theRec.getID(), RecordingState.CAPTURE_FINISHED);
    setAgentState(AgentState.IDLE);
    serializeRecording(theRec.getID());

    theRec.setProperty(CaptureParameters.RECORDING_DURATION, String.valueOf(System.currentTimeMillis() - startTime));
    startTime = -1L;

    logger.info("Recording \"{}\" succesfully stopped", theRec.getID());

    // startMonitoring(); // do not run monitoring while not capturing

    if (immediateIngest) {
      if (scheduler.scheduleSerializationAndIngest(theRec.getID())) {
        logger.info("Ingest scheduled for recording {}.", theRec.getID());
      } else {
        logger.warn("Ingest scheduling failed for recording {}!", theRec.getID());
        setRecordingState(theRec.getID(), RecordingState.UPLOAD_ERROR);
      }
    }

    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture(java.lang.String, boolean)
   */
  @Override
  public boolean stopCapture(String recordingID, boolean immediateIngest) {
    if (currentRecID != null) {
      if (recordingID.equals(currentRecID)) {
        return stopCapture(immediateIngest);
      } else {
        logger.debug("Current capture ID does not match parameter capture ID, ignoring stopCapture call.");
      }
    } else {
      logger.debug("No capture in progress, ignoring stopCapture call.");
    }
    return false;
  }

  /**
   * Generates the manifest.xml file from the files specified in the properties
   *
   * @param recID
   *          The ID for the recording whose manifest will be created
   * @return A state boolean
   * @throws IOException
   * @throws NoSuchAlgorithmException
   */
  public boolean createManifest(String recID) throws NoSuchAlgorithmException, IOException {

    AgentRecording recording = pendingRecordings.get(recID);
    if (recording == null) {
      logger.error("[createManifest] Recording {} not found!", recID);
      setRecordingState(recID, RecordingState.MANIFEST_ERROR);
      return false;
    } else {
      logger.debug("Generating manifest for recording {}", recID);
      setRecordingState(recording.getID(), RecordingState.MANIFEST);
    }

    // Get the list of device names so we can attach all the files appropriately (re: flavours, etc)
    String[] friendlyNames = recording.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES).split(",");
    if (friendlyNames.length == 1 && StringUtils.isBlank(friendlyNames[0])) {
      // Idiot check against blank name lists.
      logger.error("Unable to build mediapackage for recording {} because the device names list is blank!", recID);
      return false;
    }

    MediaPackageElementBuilder elemBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElementFlavor flavor = null;
    // Includes the tracks in the MediaPackage
    try {

      // Adds the files present in the Properties
      for (String friendlyName : friendlyNames) {
        String name = friendlyName.trim();

        String flavorPointer = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_FLAVOR;
        String flavorString = recording.getProperty(flavorPointer);
        flavor = MediaPackageElementFlavor.parseFlavor(flavorString);

        String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_DEST;
        if (null == recording.getProperty(outputProperty)) {
          logger.error(CaptureParameters.CAPTURE_DEVICE_PREFIX
                  + name
                  + CaptureParameters.CAPTURE_DEVICE_DEST
                  + "does not exist in the recording's properties.  Your CA's configuration file, or the configuration "
                  + "received from the core is missing information.  This should be checked ASAP.");
          // FIXME: Is the admin reading the agent logs? (jt)
          // FIXME: Who will find out why one of the tracks is missing from the media package? (jt)
          // FIXME: Think about a notification scheme, this looks like an emergency to me (jt)
          setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
          return false;
        }
        File outputFile = new File(recording.getBaseDir(), recording.getProperty(outputProperty));

        // Adds the file to the MediaPackage
        if (outputFile.exists()) {
          // TODO: This should really be Track rather than TrackImpl
          // but otherwise a bunch of functions we need disappear...
          TrackImpl t = (TrackImpl) elemBuilder.elementFromURI(outputFile.toURI(), MediaPackageElement.Type.Track,
                  flavor);
          t.setSize(outputFile.length());
          String[] detectedMimeType = new MimetypesFileTypeMap().getContentType(outputFile).split("/");
          t.setMimeType(mimeType(detectedMimeType[0], detectedMimeType[1]));
          t.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, outputFile));
          if (recording.getProperty(CaptureParameters.RECORDING_DURATION) != null) {
            t.setDuration(Long.parseLong(recording.getProperty(CaptureParameters.RECORDING_DURATION)));
          }

          // Commented out because this does not work properly with mock captures.
          // Also doesn't do much when it does work...
          /*
           * if (name.contains("hw:")) { AudioStreamImpl stream = new AudioStreamImpl(outputFile.getName());
           * t.addStream(stream); } else { VideoStreamImpl stream = new VideoStreamImpl(outputFile.getName());
           * t.addStream(stream); }
           */
          if (recording.getMediaPackage() == null) {
            logger.error("Recording media package is null!");
            return false;
          }
          recording.getMediaPackage().add(t);
        } else {
          // FIXME: Is the admin reading the agent logs? (jt)
          // FIXME: Who will find out why one of the tracks is missing from the media package? (jt)
          // FIXME: Think about a notification scheme, this looks like an emergency to me (jt)
          logger.error("Required file {} not found, aborting manifest creation!", outputFile.getName());
          setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
          return false;
        }
      }

    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: {}.", e);
      setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
      return false;
    }

    // Serialize the metadata file and the MediaPackage
    FileOutputStream fos = null;
    try {
      logger.debug("Serializing metadata and MediaPackage...");

      // Gets the manifest.xml as a Document object and writes it to a file
      MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl(recording.getBaseDir());
      File manifestFile = new File(recording.getBaseDir(), CaptureParameters.MANIFEST_NAME);

      MediaPackage mp = recording.getMediaPackage();
      for (MediaPackageElement element : mp.elements()) {
        if (element.getURI() != null) {
          element.setURI(new URI(serializer.encodeURI(element.getURI())));
        }
      }
      fos = new FileOutputStream(manifestFile);
      MediaPackageParser.getAsXml(mp, fos, true);

    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: {}.", e);
      setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
      return false;
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e);
      setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
      return false;
    } catch (URISyntaxException e) {
      logger.error("URI Syntax Exception: {}.", e);
      setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
      return false;
    } finally {
      IOUtils.closeQuietly(fos);
    }

    setRecordingState(recording.getID(), RecordingState.MANIFEST_FINISHED);
    return true;
  }

  /**
   * Compresses the files contained in the output directory
   *
   * @param recID
   *          The ID for the recording whose files are going to be zipped
   * @return A File reference to the file zip created
   */
  public File zipFiles(String recID) {

    logger.debug("Compressing files...");
    AgentRecording recording = pendingRecordings.get(recID);
    if (recording == null) {
      logger.error("[zipFiles] Recording {} not found!", recID);
      setRecordingState(recID, RecordingState.COMPRESSING_ERROR);
      return null;
    } else {
      setRecordingState(recording.getID(), RecordingState.COMPRESSING);
    }

    Iterable<MediaPackageElement> mpElements = recording.getMediaPackage().elements();
    Vector<File> filesToZip = new Vector<File>();

    // Now adds the files from the MediaPackage
    for (MediaPackageElement item : mpElements) {
      File tmpFile = null;
      String elementPath = item.getURI().getPath();

      // Relative and absolute paths are mixed
      if (elementPath.startsWith("file:") || elementPath.startsWith(File.separator)) {
        tmpFile = new File(elementPath);
      } else {
        tmpFile = new File(recording.getBaseDir(), elementPath);
      }

      if (!tmpFile.isFile()) {
        // TODO: Is this really a warning or should we fail completely and return an error?
        logger.warn("Required file {} doesn't exist!", tmpFile.getAbsolutePath());
      }
      filesToZip.add(tmpFile);
    }
    filesToZip.add(new File(recording.getBaseDir(), CaptureParameters.MANIFEST_NAME));

    logger.info("Zipping {} files:", filesToZip.size());
    for (File f : filesToZip) {
      logger.debug("--> {}", f.getName());
    }

    // Nuke any existing zipfile, we want to recreate it if it already exists.
    removeZipFile(recording);

    // Return a pointer to the zipped file
    File returnZip;
    try {
      File outputZip = new File(recording.getBaseDir(), CaptureParameters.ZIP_NAME);
      returnZip = ZipUtil.zip(filesToZip.toArray(new File[filesToZip.size()]), outputZip, ZipUtil.NO_COMPRESSION);
    } catch (IOException e) {
      logger.error("An IOException has occurred while zipping the files for recording {}: {}", recID, e);
      return null;
    }

    return returnZip;

  }

  /**
   * Removes the zip file associated with a recording.
   * @param recording
   *          The recording to remove the zip file from.
   */
  public void removeZipFile(AgentRecording recording) {
    File outputZip = new File(recording.getBaseDir(), CaptureParameters.ZIP_NAME);
    FileUtils.deleteQuietly(outputZip);
  }

  // FIXME: Replace HTTP-based ingest with remote implementation of the Ingest Service. (jt)
  // See the ComposerServiceRemoteImpl to get an idea of the approach
  // The idea is to get the details of the HTTP interaction out of the client code
  /**
   * Sends a file to the REST ingest service.
   *
   * @param recID
   *          The ID for the recording to be ingested.
   * @return The status code for the http post, or one of a number of error values. The error values are as follows: -1:
   *         Unable to ingest because the recording id does not exist -2: Invalid ingest url -3: Invalid ingest url -4:
   *         Invalid ingest url -5: Unable to open media package
   */
  public int ingest(String recID) {
    logger.info("Ingesting recording: {}", recID);
    AgentRecording recording = pendingRecordings.get(recID);

    if (recording == null) {
      logger.error("[ingest] Recording {} not found!", recID);
      return -1;
    }

    // Find all the available ingest services
    List<ServiceRegistration> ingestServices = null;
    URL url = null;
    try {
      ingestServices = serviceRegistry.getServiceRegistrationsByLoad("org.opencastproject.ingest");
      if (ingestServices.size() == 0) {
        logger.warn("Unable to ingest media because no ingest service is available");
        return -4;
      }

      // Take the least loaded one (first in line)
      ServiceRegistration ingestService = ingestServices.get(0);
      url = new URL(UrlSupport.concat(ingestService.getHost(), ingestService.getPath() + "/addZippedMediaPackage"));
    } catch (ServiceRegistryException e) {
      logger.warn("Unable to ingest media because communication with the remote service registry failed.", e);
      return -4;
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL for ingest target.");
      return -3;
    }

    File fileDesc = new File(recording.getBaseDir(), CaptureParameters.ZIP_NAME);

    // Set the file as the body of the request
    MultipartEntity entities = new MultipartEntity();
    // Check to see if the properties have an alternate workflow definition attached
    String workflowDefinitionId = recording.getProperty(CaptureParameters.INGEST_WORKFLOW_DEFINITION);
    String workflowInstance = recording.getID();
    // Copy appropriate keys from the properties so they can be passed to the REST endpoint separately
    Set<Object> keys = recording.getProperties().keySet();
    for (Object o : keys) {
      String key = (String) o;
      if (key.contains("org.opencastproject.workflow.config.")) {
        try {
          String configKey = key.replaceFirst("org\\.opencastproject\\.workflow\\.config\\.", "");
          entities.addPart(configKey, new StringBody(recording.getProperty(key)));
        } catch (UnsupportedEncodingException e) {
          logger.warn("Unable to attach property {} to POST.  Exception message: {}.", key, e.getMessage());
        }
      }
    }

    try {
      if (workflowDefinitionId != null) {
        entities.addPart("workflowDefinitionId", new StringBody(workflowDefinitionId, Charset.forName("UTF-8")));
      }
      if (workflowInstance != null) {
        entities.addPart("workflowInstanceId", new StringBody(workflowInstance, Charset.forName("UTF-8")));
      }
      entities.addPart(fileDesc.getName(), new InputStreamBody(new FileInputStream(fileDesc), fileDesc.getName()));
    } catch (FileNotFoundException ex) {
      logger.error("Could not find zipped mediapackage " + fileDesc.getAbsolutePath());
      return -5;
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("This system does not support UTF-8", e);
    }

    logger.debug("Ingest URL is " + url.toString());
    HttpPost postMethod = new HttpPost(url.toString());
    postMethod.setEntity(entities);

    setRecordingState(recID, RecordingState.UPLOADING);

    // Send the file
    HttpResponse response = null;
    int retValue = -1;
    try {
      logger.debug("Sending the file " + fileDesc.getAbsolutePath() + " with a size of " + fileDesc.length());
      response = client.execute(postMethod);
    } catch (TrustedHttpClientException e) {
      logger.error("Unable to ingest recording {}, message reads: {}.", recID, e.getMessage());
    } catch (NullPointerException e) {
      logger.error("Unable to ingest recording {}, null pointer exception!", recID);
    } finally {
      if (response != null) {
        retValue = response.getStatusLine().getStatusCode();
        client.close(response);
      } else {
        retValue = -1;
      }
    }

    if (retValue == HttpURLConnection.HTTP_OK) {
      setRecordingState(recID, RecordingState.UPLOAD_FINISHED);
    } else {
      setRecordingState(recID, RecordingState.UPLOAD_ERROR);
    }

    serializeRecording(recID);
    if (retValue == HttpURLConnection.HTTP_OK) {
      removeZipFile(recording);
      completedRecordings.put(recID, recording);
      pendingRecordings.remove(recID);
    }
    return retValue;
  }

  /**
   * Returns the number of captures that the capture agent is aware of that are upcoming.
   *
   * @return The number of captures that are waiting to fire.
   */
  public int getPendingRecordingSize() {
    return pendingRecordings.size();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.StateService#getAgentName()
   */
  public String getAgentName() {
    // Occasionally we're seeing a null agent name, so this fixes the problem
    if (agentName == null) {
      agentName = configService.getItem(CaptureParameters.AGENT_NAME);
    }
    return agentName;
  }

  /**
   * Sets the state of the agent. Note that this should not change the *actual* state of the agent, only update the
   * StateService's record of its state. This is taking a string so that inter-version compatibility it maintained (eg,
   * a version 2 agent talking to a version 1 core)
   *
   * @param state
   *          The state of the agent. Should be defined in AgentState.
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  protected void setAgentState(String state) {
    agentState = state;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#getAgentState()
   */
  public String getAgentState() {
    return agentState;
  }

  /**
   * Serializes a recording to disk
   *
   * @param newRec
   *          The recording object. The output object will be written to the recording's directory and will be named
   *          $recordingID.recording.
   */
  protected void serializeRecording(String id) {
    if (id == null) {
      logger.error("Unable to serialize recording, bad id parameter: null!");
      return;
    }
    ObjectOutputStream serializer = null;
    try {
      RecordingImpl newRec = (RecordingImpl) pendingRecordings.get(id);
      if (newRec == null) {
        newRec = (RecordingImpl) completedRecordings.get(id);
      }

      if (newRec == null) {
        logger.error("Unable to serialize {} because it does not exist in any recording state table!", id);
        return;
      }
      File output = new File(newRec.getBaseDir(), newRec.getID() + ".recording");
      logger.debug("Serializing recording {} to {}.", newRec.getID(), output.getAbsolutePath());
      serializer = new ObjectOutputStream(new FileOutputStream(output));
      serializer.writeObject(newRec);
    } catch (IOException e) {
      logger.error("Unable to serialize recording {}, IO exception.  Message: {}.", id, e);
    } finally {
      if (serializer != null) {
        IOUtils.closeQuietly(serializer);
      }
    }
  }

  /**
   * Deserializes a recording from disk.
   *
   * @param recording
   *          The directory containing the serialized recording and all of its files.
   * @return The deserialized {@code RecordingImpl}, or null in the case of an error.
   */
  protected RecordingImpl loadRecording(File recording) {
    RecordingImpl rec = null;
    ObjectInputStream stream = null;
    try {
      logger.debug("Loading {}.", recording.getAbsolutePath());
      stream = new ObjectInputStream(new FileInputStream(recording));
      rec = (RecordingImpl) stream.readObject();
      if (context != null) {
        rec.getProperties().setBundleContext(context.getBundleContext());
      }
    } catch (FileNotFoundException e) {
      logger.error("Unable to load recording {}, file not found!", recording.getAbsolutePath());
    } catch (IOException e) {
      logger.error("IOException loading recording {}: {}.", recording.getAbsolutePath(), e);
    } catch (ClassNotFoundException e) {
      logger.error("Unable to load recording {}, file not found!", recording.getAbsolutePath());
    } finally {
      if (stream != null) {
        IOUtils.closeQuietly(stream);
      }
    }
    return rec;
  }

  /**
   * Sets a pending recording's current state.
   *
   * @param recordingID
   *          The ID of the recording.
   * @param state
   *          The state for the recording. Defined in RecordingState.
   * @see org.opencastproject.capture.admin.api.RecordingState
   */
  protected void setRecordingState(String recordingID, String state) {
    if (recordingID != null && state != null) {
      AgentRecording rec = pendingRecordings.get(recordingID);
      if (rec != null) {
        rec.setState(state);
      } else {
        XProperties p = configService.getAllProperties();
        p.put(CaptureParameters.RECORDING_ID, recordingID);
        try {
          rec = new RecordingImpl(null, p);
          rec.setState(state);
        } catch (IOException e) {
          /* Squash this, it's trying to create a directory for a (probably) failed capture */
        }
        pendingRecordings.put(recordingID, rec);
      }
    } else if (recordingID == null) {
      logger.info("Unable to create recording because recordingID parameter was null!");
    } else if (state == null) {
      logger.info("Unable to create recording because state parameter was null!");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.StateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String recID) {
    return pendingRecordings.get(recID);
  }

  /**
   * Removes a recording from the completed recording table.
   *
   * @param recID
   *          The ID of the recording to remove.
   */
  public void removeCompletedRecording(String recID) {
    completedRecordings.remove(recID);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.StateService#getKnownRecordings()
   */
  public Map<String, AgentRecording> getKnownRecordings() {
    HashMap<String, AgentRecording> complete = new HashMap<String, AgentRecording>();
    complete.putAll(pendingRecordings);
    complete.putAll(completedRecordings);
    return complete;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#getAgentCapabilities()
   */
  public Properties getAgentCapabilities() {
    if (configService != null) {
      return configService.getCapabilities();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#getDefaultAgentProperties()
   */
  public Properties getDefaultAgentProperties() {
    if (configService != null) {
      return configService.getAllProperties();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#getDefaultAgentPropertiesAsString()
   */
  @Deprecated
  public String getDefaultAgentPropertiesAsString() {
    Properties p = configService.getAllProperties();
    StringBuffer result = new StringBuffer();
    String[] lines = new String[p.size()];
    int i = 0;
    for (Object k : p.keySet()) {
      String key = (String) k;
      lines[i++] = key + "=" + p.getProperty(key) + "\n";
    }
    Arrays.sort(lines);
    for (String s : lines) {
      result.append(s);
    }

    return result.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#getAgentSchedule()
   */
  public List<ScheduledEvent> getAgentSchedule() {
    if (scheduler != null) {
      return scheduler.getSchedule();
    } else {
      logger.info("Scheduler is null, so the agent cannot have a current schedule.");
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties != null) {
      cachedProperties = properties;
      updated = true;
      if (updated && refreshed) {
        startConfigurationDependantTasks();
      }
    } else {
      throw new ConfigurationException("null", "Null configuration in updated!");
    }
  }

  /** If felix has updated the CaptureAgent properties at least once this will be true. **/
  public boolean isUpdated() {
    return updated;
  }

  /**
   * When the ConfigurationManager is updated refresh is called as a part of the observer pattern of
   * ConfigurationManagerObserver
   **/
  @Override
  public void refresh() {
    refreshed = true;
    if (updated && refreshed) {
      try {
        startConfigurationDependantTasks();
      } catch (ConfigurationException e) {
        logger.error(e.getMessage());
      }
    }

    // update confidence monitoring properties
    confidence = Boolean.valueOf(configService.getItem(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));
    if (confidence) {
      String monitoringMaxRmsValuesPerDeviceStr = configService.getItem(
              CaptureParameters.CAPTURE_CONFIDENCE_AUDIO_LENGTH);

      if (!StringUtils.isBlank(monitoringMaxRmsValuesPerDeviceStr)) {
        try {
          int maxRmsValuesPerDevice = Integer.parseInt(monitoringMaxRmsValuesPerDeviceStr);
          if (maxRmsValuesPerDevice > 0) {
            this.maxRmsValuesPerDevice = maxRmsValuesPerDevice;
          } else {
            logger.warn("Config property value for {} should be positive integer",
                    CaptureParameters.CAPTURE_CONFIDENCE_AUDIO_LENGTH);
          }
        } catch (NumberFormatException ex) {
          logger.warn("Can't parse configuration value for '{}'",
                  CaptureParameters.CAPTURE_CONFIDENCE_AUDIO_LENGTH);
        }
      }
      // (re)start monitoring pipeline if isn't capturing
//      if (pipeline != null && pipeline.isMonitoringOnly()) {
//        stopMonitoring();
//        startMonitoring(); // do not run monitoring while not capturing
//      }
    }
  }

  /** If ConfigurationManager had updated and passed on those properties to the Capture Agent this returns true. **/
  public boolean isRefreshed() {
    return refreshed;
  }

  /**
   * Once the configuration is loaded through the updated method and the ConfigurationManager has loaded its properties
   * then this method is called to start the schedules dependent on configuration properties.
   **/
  public void startConfigurationDependantTasks() throws ConfigurationException {
    synchronized (cachedProperties) {
      if (cachedProperties == null) {
        throw new ConfigurationException("null", "Null configuration in updated!");
      }

      // Update the agent's properties from the parameter
      Properties props = new Properties();
      Enumeration<String> keys = cachedProperties.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        props.put(key, cachedProperties.get(key));
      }
      // Create Agent state push task.
      createScheduler(props, "agentStateUpdate", JobParameters.RECURRING_TYPE);
      // Create recording load task.
      createScheduler(props, "recordingLoad", JobParameters.OTHER_TYPE);
      // Recreate the agent state push tasks
      createPushTask();
      // Setup the task to load the recordings from disk once everything has started (let's be safe and use 60
      // seconds)
      createRecordingLoadTask(RECORDING_LOAD_TASK_DELAY);
      logger.info("CaptureAgentImpl has successfully updated its properties from ConfigurationManager");
      // Create SchedulerImpl
      Hashtable<String, String> schedulerProperties = new Hashtable<String, String>();
      schedulerProperties.put("org.quartz.scheduler.instanceName", "scheduler_sched");
      schedulerProperties.put("org.quartz.scheduler.instanceId", "AUTO");
      schedulerProperties.put("org.quartz.scheduler.rmi.export", "false");
      schedulerProperties.put("org.quartz.scheduler.rmi.proxy", "false");

      schedulerProperties.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
      schedulerProperties.put("org.quartz.threadPool.threadCount", "20");

      schedulerProperties.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
      try {
        scheduler = new SchedulerImpl(schedulerProperties, configService, this);
        if (client != null) {
          scheduler.setTrustedClient(client);
        }
      } catch (ConfigurationException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Callback from the OSGi container once this service is started. This is where we register our shell commands.
   *
   * @param ctx
   *          the component context
   */
  public void activate(ComponentContext ctx) {
    logger.info("Starting CaptureAgentImpl.");

    if (ctx != null) {
      // Setup the shell commands
      Dictionary<String, Object> commands = new Hashtable<String, Object>();
      commands.put(CommandProcessor.COMMAND_SCOPE, "capture");
      commands.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "status", "start", "stop", "ingest", "reset",
              "capture" });
      logger.info("Registering capture agent osgi shell commands");
      ctx.getBundleContext().registerService(CaptureAgentShellCommands.class.getName(),
              new CaptureAgentShellCommands(this), commands);
      this.context = ctx;
    } else {
      logger.warn("Bundle context is null, so this is probably a test."
              + "  If you see this message from Felix please post a bug!");
    }

    setAgentState(AgentState.IDLE);
    if (configService != null && ctx != null) {
      configService.setItem("org.opencastproject.server.url",
              ctx.getBundleContext().getProperty("org.opencastproject.server.url"));
    } else if (configService == null) {
      logger.warn("Config service was null, unable to set local server url!");
    } else if (ctx == null) {
      logger.warn("Context was null, unable to set local server url!");
    }
  }

  /**
   * Shuts down the capture agent.
   */
  public void deactivate() {
    try {
      if (agentScheduler != null) {
        for (String groupname : agentScheduler.getJobGroupNames()) {
          for (String jobname : agentScheduler.getJobNames(groupname)) {
            agentScheduler.deleteJob(jobname, groupname);
          }
        }
        agentScheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      logger.warn("Finalize for scheduler did not execute cleanly: {}.", e);
    }
  }

  /**
   * Loads all of the recordings from the capture cache dir.
   */
  public boolean loadRecordingsFromDisk() {
    if (configService == null) {
      logger.error("Unable to load recordings from disk, configuration service is null!");
      return false;
    } else if (configService.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL) == null) {
      logger.error("Unable to load recordings from disk, configuration service has null cache URL.");
      return false;
    }

    if (scheduler == null) {
      logger.error("Unable to load recordings from disk, scheduler service is null!");
      return false;
    }

    File captureRootDir = new File(configService.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    if (captureRootDir.listFiles() == null) {
      logger.debug("Unable to load recordings from disk, capture root dir is a file...");
      return false;
    }

    for (File f : captureRootDir.listFiles()) {
      if (f != null && f.isDirectory()) {
        for (File r : f.listFiles()) {
          if (r.getName().endsWith(".recording")) {
            RecordingImpl rec = loadRecording(r);
            String state = rec.getState();
            String id = rec.getID();
            logger.info("Loading recording {}...", id);
            if (pendingRecordings.containsKey(id) || completedRecordings.containsKey(id)) {
              logger.debug("Unable to load recording {} with state {}.  The recording already exists.", id, state);
              continue;
            }

            // FIXME: This should be redone when the job refactoring ticket is finished (MH-5235).
            if (state.equals(RecordingState.CAPTURE_ERROR)) {
              logger.debug("Loaded recording {} with state {}."
                      + "This is an error state so placing recording in completed index.", id, state);
              completedRecordings.put(id, rec);
            } else if (state.equals(RecordingState.CAPTURE_FINISHED) || state.equals(RecordingState.CAPTURING)) {
              logger.debug("Loaded recording {} with state {}."
                      + "This is a completed recording so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleSerializationAndIngest(id);
            } else if (state.equals(RecordingState.MANIFEST) || state.equals(RecordingState.MANIFEST_ERROR)) {
              logger.debug("Loaded recording {} with state {}."
                      + "This is ready to ingest so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleSerializationAndIngest(id);
            } else if (state.equals(RecordingState.MANIFEST_FINISHED) || state.equals(RecordingState.COMPRESSING)
                    || state.equals(RecordingState.COMPRESSING_ERROR)) {
              logger.debug("Loaded recording {} with state {}."
                      + "This is partially ingested so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleSerializationAndIngest(id);
            } else if (state.equals(RecordingState.UPLOAD_ERROR) || state.equals(RecordingState.UPLOADING)) {
              logger.debug("Loaded recording {} with state {}."
                      + "This is to upload so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleIngest(id);
            } else if (state.equals(RecordingState.UPLOAD_FINISHED)) {
              logger.debug("Loaded recording {} with state {}."
                      + "This is a completed state so placing recording in completed index.", id, state);
              completedRecordings.put(id, rec);
            } else if (state.equals(RecordingState.UNKNOWN)) {
              logger.debug("Loaded recording {} with state {}." + "This is an unknown state, discarding.", id, state);
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * Creates the agent's scheduler
   *
   * @param schedulerProps
   * @param jobname
   * @param jobtype
   */
  private void createScheduler(Properties schedulerProps, String jobname, String jobtype) {
    // Either create the scheduler or empty out the existing one
    try {
      if (agentScheduler != null) {
        // Clear the existing jobs and reschedule everything
        for (String name : agentScheduler.getJobNames(jobtype)) {
          if (jobname.equals(name)) {
            agentScheduler.deleteJob(name, JobParameters.RECURRING_TYPE);
          }
        }
      } else {
        StdSchedulerFactory scheduleFactory = null;
        if (schedulerProps.size() > 0) {
          scheduleFactory = new StdSchedulerFactory(schedulerProps);
        } else {
          scheduleFactory = new StdSchedulerFactory();
        }

        // Create and start the scheduler
        agentScheduler = scheduleFactory.getScheduler();
        agentScheduler.start();
      }
    } catch (SchedulerException e) {
      logger.error("Scheduler exception in State Service: {}.", e);
      return;
    }
  }

  /**
   * Creates the Quartz task which loads the recordings from disk.
   *
   * @param schedulerProps
   *          The properties of the scheduler, used to create the scheduler if it doesn't already exist
   * @param delay
   *          The delay before firing the job, in seconds
   */
  void createRecordingLoadTask(long delay) {
    if (agentScheduler == null) {
      return;
    } else {
      // Clear out any existing jobs just for fun.
      try {
        agentScheduler.deleteJob("recordingLoad", JobParameters.OTHER_TYPE);
      } catch (SchedulerException e) {
        // We probably don't care about this at all, but hey let's log it anyway
        logger.debug("Exception attempting to delete recording load job before scheduling a new one.", e);
      }
    }

    JobDetail loadJob = new JobDetail("recordingLoad", JobParameters.OTHER_TYPE, LoadRecordingsJob.class);

    loadJob.getJobDataMap().put(JobParameters.CAPTURE_AGENT, this);

    Date start = new Date(System.currentTimeMillis() + delay * CaptureParameters.MILLISECONDS);
    // Create a new trigger Name Group name Start
    SimpleTrigger loadTrigger = new SimpleTrigger("recording_load", JobParameters.OTHER_TYPE, start);
    loadTrigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

    // Schedule the update
    try {
      agentScheduler.scheduleJob(loadJob, loadTrigger);
    } catch (SchedulerException e) {
      logger.warn("Unable to load recordings from disk due to scheduler error: {}.", e);
    }
  }

  /**
   * Creates the Quartz task which pushes the agent's state to the state server.
   *
   * @param schedulerProps
   *          The properties for the Quartz scheduler
   */
  private void createPushTask() {
    if (agentScheduler == null) {
      return;
    }

    // Setup the agent state push jobs
    long statePushTime;
    try {
      statePushTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL))
              * CaptureParameters.MILLISECONDS;
    } catch (NumberFormatException e) {
      logger.warn("Invalid time specified in the \"" + CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL
              + "\" value is \"" + configService.getItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL)
              + "\" and the config service is \"" + configService.toString()
              + "\". Will be using the default polling time of " + CaptureParameters.DEFAULT_STATE_PUSH_TIME);
      // Set the state push time to a default.
      statePushTime = CaptureParameters.DEFAULT_STATE_PUSH_TIME * CaptureParameters.MILLISECONDS;
    }
    // Setup the push job
    JobDetail stateJob = new JobDetail("agentStateUpdate", JobParameters.RECURRING_TYPE, AgentStateJob.class);

    stateJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
    stateJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);
    stateJob.getJobDataMap().put(JobParameters.TRUSTED_CLIENT, client);
    stateJob.getJobDataMap().put("org.opencastproject.server.url",
            configService.getItem("org.opencastproject.server.url"));

    // Create a new trigger Name Group name
    SimpleTrigger stateTrigger = new SimpleTrigger("state_push", JobParameters.RECURRING_TYPE);
    stateTrigger.setStartTime(new Date()); // Start immediately
    stateTrigger.setEndTime(null); // Never end
    stateTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);

    stateTrigger.setRepeatInterval(statePushTime);

    try {
      // Schedule the update
      agentScheduler.scheduleJob(stateJob, stateTrigger);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule state push jobs: {}.", e);
    }

    // Setup the agent capabilities push jobs
    try {
      long capbsPushTime = Long.parseLong(configService
              .getItem(CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL)) * CaptureParameters.MILLISECONDS;

      // Setup the push job
      JobDetail capbsJob = new JobDetail("agentCapabilitiesUpdate", AgentConfigurationJob.class);
      capbsJob.setGroup(JobParameters.RECURRING_TYPE);

      capbsJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      capbsJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);
      capbsJob.getJobDataMap().put(JobParameters.TRUSTED_CLIENT, client);

      // Create a new trigger Name Group name
      SimpleTrigger capbsTrigger = new SimpleTrigger("capabilities_polling", JobParameters.RECURRING_TYPE);
      capbsTrigger.setStartTime(new Date());
      capbsTrigger.setEndTime(null);
      capbsTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
      capbsTrigger.setRepeatInterval(capbsPushTime);

      // Schedule the update
      agentScheduler.scheduleJob(capbsJob, capbsTrigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push capabilities to remote server!",
              CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule capability push jobs: {}.", e);
    }
  }

  /**
   * Determines if the current agent state is idle.
   *
   * @return returns true if the capture agent is idle.
   */
  public boolean isIdle() {
    String state = getAgentState();
    if (state == null) {
      return false;
    } else {
      return state.equals(AgentState.IDLE);
    }
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#updateCalendar()
   */
  public void updateSchedule() {
    if (scheduler != null) {
      scheduler.updateCalendar();
    }
  }

  /**
   *
   * @param friendlyName
   * @return
   */
  @Override
  public byte[] grabFrame(String friendlyName) {
    if (pipeline != null && pipeline.isMonitoringEnabled()
            && monitoringDevicesVideoLocation.get(friendlyName) != null
            && new File(monitoringDevicesVideoLocation.get(friendlyName)).exists()) {
      try {
        return FileUtils.readFileToByteArray(new File(monitoringDevicesVideoLocation.get(friendlyName)));
      } catch (IOException ex) {
        logger.error("Can't read confidence monitoring image for {} device.", friendlyName);
      }
    }
    return null;
  }

  /**
   *
   * @param friendlyName
   * @param timestamp
   * @return
   * @deprecated
   */
  @Override
  @Deprecated
  public List<Double> getRMSValues(String friendlyName, double timestamp) {
    return getRMSValues(friendlyName, Math.round(timestamp));
  }

  /**
   *
   * @param friendlyName
   * @param timestamp
   * @return
   */
  @Override
  public List<Double> getRMSValues(String friendlyName, long timestamp) {
    SortedMap<Long, Double> deviceValues = monitoringDevicesRmsValues.get(friendlyName);
    if (deviceValues == null)
      return new LinkedList<Double>();

    synchronized (deviceValues) {
      return new LinkedList<Double>(deviceValues.tailMap(timestamp).values());
    }
  }

  /**
   *
   * @return
   */
  @Override
  public List<String> getFriendlyNames() {
    LinkedList<String> deviceList = new LinkedList<String>();
    for (String name : monitoringDeviceFriendlyNames.keySet()) {
      deviceList.add(name + "," + monitoringDeviceFriendlyNames.get(name));
    }

    return deviceList;
  }

  /**
   *
   * @return
   */
  @Override
  public String getCoreUrl() {
    if (configService != null)
      return configService.getAllProperties().getProperty(CaptureParameters.CAPTURE_CORE_URL);

    return null;
  }

  /**
   * Remove video monitoring image files and clear audio values.
   */
  protected void cleanupMonitoring() {
    monitoringDeviceFriendlyNames.clear();
    monitoringDevicesRmsValues.clear();
    for (String videoMonitoringFilePath : monitoringDevicesVideoLocation.values()) {
      File videoMonitoringFile = new File(videoMonitoringFilePath);
      if (videoMonitoringFile.exists()) {
        videoMonitoringFile.delete();
      }
    }
    monitoringDevicesVideoLocation.clear();
  }

  /**
   *
   * @return
   */
  @Override
  public boolean startMonitoring() {
    if (confidence) {
      logger.info("Confidence monitoring enabled.");

      if (pipeline != null) {
        logger.warn("Abort start monitoring pipeline, because there is another pipeline running.");
        return false;
      }

      cleanupMonitoring();

      pipeline = new GStreamerMonitoringPipeline();
      pipeline.setMonitoringListener(this);

      try {
        ((GStreamerMonitoringPipeline)pipeline).start(configService.getAllProperties());
        logger.info("Confidence Monitoring is starting");
        return true;
      } catch (UnableToStartMonitoringException ex) {
        logger.warn("Confidence Monitoring failed to start. ", ex);
      }
    }
    return false;
  }

  /**
   *
   */
  @Override
  public void stopMonitoring() {
    if (pipeline != null && pipeline.isMonitoringOnly()) {
     pipeline.stop();
     pipeline = null;

     cleanupMonitoring();
    }
  }

  /**
   *
   * @param deviceFriendlyName
   * @param type
   */
  @Override
  public void registerDevice(String deviceFriendlyName, DeviceType type) {
    if (monitoringDeviceFriendlyNames.get(deviceFriendlyName) == null) {
      monitoringDeviceFriendlyNames.put(deviceFriendlyName, type.toString().toLowerCase());
    }

    if (type == DeviceType.AUDIO || type == DeviceType.AV
            && monitoringDevicesRmsValues.get(deviceFriendlyName) == null) {
      monitoringDevicesRmsValues.put(deviceFriendlyName, new TreeMap<Long, Double>());
    }
  }

  /**
   *
   * @param deviceFriendlyName
   * @param monitoringVideoLocation
   */
  @Override
  public void setMonitoringVideoLocation(String deviceFriendlyName, String monitoringVideoLocation) {
    monitoringDevicesVideoLocation.put(deviceFriendlyName, monitoringVideoLocation);
  }

  /**
   *
   * @param deviceFriendlyName
   * @param timestamp
   * @param rmsValue
   */
  @Override
  public void addRmsValue(String deviceFriendlyName, long timestamp, double rmsValue) {
    for (String name : monitoringDeviceFriendlyNames.keySet()) {
      if (deviceFriendlyName.contains(name)) {
        // get rms values map
        SortedMap<Long, Double> rmsValues = monitoringDevicesRmsValues.get(name);
        if (rmsValues != null) {
          synchronized (rmsValues) {
            // if maximum entries reached, remove latest
            if (rmsValues.keySet().size() >= maxRmsValuesPerDevice) {
              rmsValues.remove(rmsValues.firstKey());
            }
            // store new value
            rmsValues.put(timestamp, rmsValue);
            break;
          }
        }
      }
    }
  }
}
