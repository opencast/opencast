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
package org.opencastproject.sox.impl;

import static org.opencastproject.util.data.Option.some;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.sox.api.SoxException;
import org.opencastproject.sox.api.SoxService;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SoxServiceImpl extends AbstractJobProducer implements SoxService {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(SoxServiceImpl.class);

  /** Default location of the SoX binary (resembling the installer) */
  public static final String SOX_BINARY_DEFAULT = "sox";

  public static final String CONFIG_SOX_PATH = "org.opencastproject.sox.path";

  /** List of available operations on jobs */
  private enum Operation {
    Analyze, Normalize
  }

  /** The collection name */
  public static final String COLLECTION = "sox";

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry;

  /** Id builder used to create ids for encoded tracks */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  private String binary = SOX_BINARY_DEFAULT;

  /** Creates a new composer service instance. */
  public SoxServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * OSGi callback on component activation.
   *
   * @param cc
   *          the component context
   */
  void activate(ComponentContext cc) {
    logger.info("Activating sox service");
    // Configure sox
    String path = (String) cc.getBundleContext().getProperty(CONFIG_SOX_PATH);
    if (path == null) {
      logger.debug("DEFAULT " + CONFIG_SOX_PATH + ": " + SOX_BINARY_DEFAULT);
    } else {
      binary = path;
      logger.debug("SoX config binary: {}", path);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.sox.api.SoxService#analyze(Track)
   */
  @Override
  public Job analyze(Track sourceAudioTrack) throws MediaPackageException, SoxException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Analyze.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceAudioTrack)));
    } catch (ServiceRegistryException e) {
      throw new SoxException("Unable to create a job", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.sox.api.SoxService#normalize(Track, Float)
   */
  @Override
  public Job normalize(Track sourceAudioTrack, Float targetRmsLevDb) throws MediaPackageException, SoxException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Normalize.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceAudioTrack), targetRmsLevDb.toString()));
    } catch (ServiceRegistryException e) {
      throw new SoxException("Unable to create a job", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      TrackImpl audioTrack = null;

      final String serialized;
      switch (op) {
        case Analyze:
          audioTrack = (TrackImpl) MediaPackageElementParser.getFromXml(arguments.get(0));
          serialized = analyze(job, audioTrack).map(MediaPackageElementParser.<Track> getAsXml()).getOrElse("");
          break;
        case Normalize:
          audioTrack = (TrackImpl) MediaPackageElementParser.getFromXml(arguments.get(0));
          Float targetRmsLevDb = new Float(arguments.get(1));
          serialized = normalize(job, audioTrack, targetRmsLevDb).map(MediaPackageElementParser.<Track> getAsXml())
                  .getOrElse("");
          break;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }

      return serialized;
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  protected Option<Track> analyze(Job job, Track audioTrack) throws SoxException {
    if (!audioTrack.hasAudio())
      throw new SoxException("No audio stream available");
    if (audioTrack.hasVideo())
      throw new SoxException("It must not have a video stream");

    try {
      // Get the tracks and make sure they exist
      final File audioFile;
      try {
        audioFile = workspace.get(audioTrack.getURI());
      } catch (NotFoundException e) {
        throw new SoxException("Requested audio track " + audioTrack + " is not found");
      } catch (IOException e) {
        throw new SoxException("Unable to access audio track " + audioTrack);
      }

      logger.info("Analyzing audio track {}", audioTrack.getIdentifier());

      // Do the work
      ArrayList<String> command = new ArrayList<String>();
      command.add(binary);
      command.add(audioFile.getAbsolutePath());
      command.add("-n");
      command.add("remix");
      command.add("-");
      command.add("stats");
      List<String> analyzeResult = launchSoxProcess(command);

      // Add audio metadata and return audio track
      return some(addAudioMetadata(audioTrack, analyzeResult));
    } catch (Exception e) {
      logger.warn("Error analyzing {}: {}", audioTrack, e.getMessage());
      if (e instanceof SoxException) {
        throw (SoxException) e;
      } else {
        throw new SoxException(e);
      }
    }
  }

  private Track addAudioMetadata(Track audioTrack, List<String> metadata) {
    TrackImpl track = (TrackImpl) audioTrack;
    List<AudioStream> audio = track.getAudio();

    if (audio.size() == 0) {
      audio.add(new AudioStreamImpl());
      logger.info("No audio streams found created new audio stream");
    }

    AudioStreamImpl audioStream = (AudioStreamImpl) audio.get(0);
    if (audio.size() > 1)
      logger.info("Multiple audio streams found, take first audio stream {}", audioStream);

    for (String value : metadata) {
      if (value.startsWith("Pk lev dB")) {
        Float pkLevDb = new Float(StringUtils.substringAfter(value, "Pk lev dB").trim());
        audioStream.setPkLevDb(pkLevDb);
      } else if (value.startsWith("RMS lev dB")) {
        Float rmsLevDb = new Float(StringUtils.substringAfter(value, "RMS lev dB").trim());
        audioStream.setRmsLevDb(rmsLevDb);
      } else if (value.startsWith("RMS Pk dB")) {
        Float rmsPkDb = new Float(StringUtils.substringAfter(value, "RMS Pk dB").trim());
        audioStream.setRmsPkDb(rmsPkDb);
      }
    }
    return track;
  }

  private List<String> launchSoxProcess(List<String> command) throws SoxException {
    Process process = null;
    BufferedReader in = null;
    try {
      logger.info("Start sox process {}", command);
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectErrorStream(true); // Unfortunately merges but necessary for deadlock prevention
      process = pb.start();
      in = new BufferedReader(new InputStreamReader(process.getInputStream()));
      process.waitFor();
      String line = null;
      List<String> stats = new ArrayList<String>();
      while ((line = in.readLine()) != null) {
        logger.info(line);
        stats.add(line);
      }
      if (process.exitValue() != 0)
        throw new SoxException("Sox process failed with error code: " + process.exitValue());
      logger.info("Sox process finished");
      return stats;
    } catch (IOException e) {
      throw new SoxException("Could not start sox process: " + command + "\n" + e.getMessage());
    } catch (InterruptedException e) {
      throw new SoxException("Could not start sox process: " + command + "\n" + e.getMessage());
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  private Option<Track> normalize(Job job, TrackImpl audioTrack, Float targetRmsLevDb) throws SoxException {
    if (!audioTrack.hasAudio())
      throw new SoxException("No audio stream available");
    if (audioTrack.hasVideo())
      throw new SoxException("It must not have a video stream");
    if (audioTrack.getAudio().size() < 1)
      throw new SoxException("No audio stream metadata available");
    if (audioTrack.getAudio().get(0).getRmsLevDb() == null)
      throw new SoxException("No RMS Lev dB metadata available");

    final String targetTrackId = idBuilder.createNew().toString();

    Float rmsLevDb = audioTrack.getAudio().get(0).getRmsLevDb();

    // Get the tracks and make sure they exist
    final File audioFile;
    try {
      audioFile = workspace.get(audioTrack.getURI());
    } catch (NotFoundException e) {
      throw new SoxException("Requested audio track " + audioTrack + " is not found");
    } catch (IOException e) {
      throw new SoxException("Unable to access audio track " + audioTrack);
    }

    String outDir = audioFile.getAbsoluteFile().getParent();
    String outFileName = FilenameUtils.getBaseName(audioFile.getName()) + "_" + UUID.randomUUID().toString();
    String suffix = "-norm." + FilenameUtils.getExtension(audioFile.getName());

    File normalizedFile = new File(outDir, outFileName + suffix);

    logger.info("Normalizing audio track {} to {}", audioTrack.getIdentifier(), targetTrackId);

    // Do the work
    ArrayList<String> command = new ArrayList<String>();
    command.add(binary);
    command.add(audioFile.getAbsolutePath());
    command.add(normalizedFile.getAbsolutePath());
    command.add("remix");
    command.add("-");
    command.add("gain");
    if (targetRmsLevDb > rmsLevDb)
      command.add("-l");
    command.add(new Float(targetRmsLevDb - rmsLevDb).toString());
    command.add("stats");

    List<String> normalizeResult = launchSoxProcess(command);

    if (normalizedFile.length() == 0)
      throw new SoxException("Normalization failed: Output file is empty!");

    // Put the file in the workspace
    URI returnURL = null;
    InputStream in = null;
    try {
      in = new FileInputStream(normalizedFile);
      returnURL = workspace.putInCollection(COLLECTION,
              job.getId() + "." + FilenameUtils.getExtension(normalizedFile.getAbsolutePath()), in);
      logger.info("Copied the normalized file to the workspace at {}", returnURL);
      if (normalizedFile.delete()) {
        logger.info("Deleted the local copy of the normalized file at {}", normalizedFile.getAbsolutePath());
      } else {
        logger.warn("Unable to delete the normalized output at {}", normalizedFile);
      }
    } catch (Exception e) {
      throw new SoxException("Unable to put the normalized file into the workspace", e);
    } finally {
      IOUtils.closeQuietly(in);
      FileSupport.delete(normalizedFile);
    }

    Track normalizedTrack = (Track) audioTrack.clone();
    normalizedTrack.setURI(returnURL);
    normalizedTrack.setIdentifier(targetTrackId);
    // Add audio metadata and return audio track
    normalizedTrack = addAudioMetadata(normalizedTrack, normalizeResult);

    return some(normalizedTrack);
  }

  /**
   * Sets the workspace
   *
   * @param workspace
   *          an instance of the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the service registry
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

}
