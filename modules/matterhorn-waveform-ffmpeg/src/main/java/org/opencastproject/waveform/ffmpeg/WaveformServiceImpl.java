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
package org.opencastproject.waveform.ffmpeg;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.waveform.api.WaveformService;
import org.opencastproject.waveform.api.WaveformServiceException;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;


/**
 * This service create a waveform image from a media file with at least one audio cannel.
 * This will be done by ffmpeg.
 */
public class WaveformServiceImpl extends AbstractJobProducer implements WaveformService, ManagedService {

  /** The logging facility */
  protected static final Logger logger = LoggerFactory.getLogger(WaveformServiceImpl.class);

  /** Path to the executable */
  protected String binary;

  /** The load introduced on the system by creating a waveform job */
  public static final float DEFAULT_WAVEFORM_JOB_LOAD = 1.0f;

  /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_JOB_LOAD */
  public static final String WAVEFORM_JOB_LOAD_KEY = "job.load.waveform";


  public static final String FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path";

  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "waveform";

  public static final int DEFAULT_WAVEFORM_IMAGE_WIDTH = 5000;

  public static final int DEFAULTWAVEFORM_IMAGE_HEIGHT = 500;

  private static final String WAVEFORM_FLAVOR_SUBTYPE = "waveform";

  /** List of available operations on jobs */
  private enum Operation {
    Waveform
  };

  private float waveformJobLoad = DEFAULT_WAVEFORM_JOB_LOAD;

  private int waveformImageWidth = DEFAULT_WAVEFORM_IMAGE_WIDTH;
  private int waveformImageHeight = DEFAULTWAVEFORM_IMAGE_HEIGHT;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry = null;

  private WorkingFileRepository wfr = null;

  /** The workspace to use when retrieving remote media files */
  private Workspace workspace = null;

  /** The security service */
  private SecurityService securityService = null;

  /** The user directory service */
  private UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService = null;

  public WaveformServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.debug("Activate ffmpeg WaveformService");
    /* Configure segmenter */
    final String path = cc.getBundleContext().getProperty(FFMPEG_BINARY_CONFIG);
    binary = path == null ? FFMPEG_BINARY_DEFAULT : path;
    logger.debug("ffmpeg binary set to {}", binary);
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    logger.debug("Configuring the waveform service");
    waveformJobLoad = LoadUtil.getConfiguredLoadValue(properties,
            WAVEFORM_JOB_LOAD_KEY, DEFAULT_WAVEFORM_JOB_LOAD, serviceRegistry);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.waveform.api.WaveformService#generateWaveformImage(org.opencastproject.mediapackage.Track)
   */
  @Override
  public Job generateWaveformImage(Track sourceTrack) throws MediaPackageException, WaveformServiceException {
    try {
      return serviceRegistry.createJob(jobType, Operation.Waveform.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack)), waveformJobLoad);
    } catch (ServiceRegistryException ex) {
      throw new WaveformServiceException("Unable to create waveform job", ex);
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
      switch (op) {
        case Waveform:
          Track track = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          Attachment waveformMpe = extractWaveform(track);
          return MediaPackageElementParser.getAsXml(waveformMpe);
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (MediaPackageException | WaveformServiceException e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  private Attachment extractWaveform(Track track) throws WaveformServiceException {
    if (!track.hasAudio()) {
      throw new WaveformServiceException("Track has no audio");
    }

    // copy source file into workspace
    File mediaFile = null;
    try {
      mediaFile = workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new WaveformServiceException(
          "Error finding the media file in the workspace", e);
    } catch (IOException e) {
      throw new WaveformServiceException(
          "Error reading the media file in the workspace", e);
    }

    String waveformFileName = FilenameUtils.getBaseName(mediaFile.getName()).concat("_waveform.png");
    URI waveformFileUri = workspace.getCollectionURI(COLLECTION_ID, waveformFileName);

    // create ffmpeg command
    String[] command = new String[] { binary,
      "-nostats", "-i " + mediaFile.getAbsolutePath().replaceAll(" ", "\\ "),
      "-lavfi " + createWaveformFilter(),
      FilenameUtils.getFullPath(waveformFileUri.toString()).replaceAll(" ", "\\ ")
    };

    // run ffmpeg
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process ffmpegProcess = null;
    int exitCode = 1;
    try {
      ffmpegProcess = pb.start();
      exitCode = ffmpegProcess.waitFor();
    } catch (IOException ex) {
      throw new WaveformServiceException("Start ffmpeg process failed", ex);
    } catch (InterruptedException ex) {
      throw new WaveformServiceException("The thread managing encoder process was interrupted", ex);
    } finally {
      if (ffmpegProcess != null)
        IoSupport.closeQuietly(ffmpegProcess);

      if (exitCode != 0) {
        try {
          workspace.delete(waveformFileUri);
        } catch (NotFoundException | IOException e) {
          // it is ok, no output file was generated by ffmpeg
        }
      }
    }

    if (exitCode != 0)
      throw new WaveformServiceException("The encoder process exited abnormally with status " + exitCode);

    // copy waveform image into working file repository
    FileInputStream waveformImageInputStream = null;
    URI waveformWfrUri = null;
    try {
      waveformImageInputStream = new FileInputStream(waveformFileUri.getPath());
      waveformWfrUri = wfr.putInCollection(COLLECTION_ID, waveformFileName, waveformImageInputStream);
    } catch (FileNotFoundException ex) {
      throw new WaveformServiceException("Just created waveform image file not found", ex);
    } catch (IOException ex) {
      throw new WaveformServiceException("Can not read just created waveform image", ex);
    } finally {
      IoSupport.closeQuietly(waveformImageInputStream);
    }

    // delete waveform image in workspace
    try {
      workspace.delete(waveformFileUri);
    } catch (NotFoundException ex) {
      // it is ok, because we want to delete the file
    } catch (IOException ex) {
      logger.warn("deleting waveform image from workspace failed", ex);
      // do not propagate the exception, because the workspace cleaner will delete the file if it is exist
    }

    // create media package element
    MediaPackageElementBuilder mpElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElementFlavor waveformFlavor = MediaPackageElementFlavor.flavor(
            track.getFlavor().getType(), WAVEFORM_FLAVOR_SUBTYPE);
    return (Attachment) mpElementBuilder.elementFromURI(waveformWfrUri, Type.Attachment, waveformFlavor);
  }

  private String createWaveformFilter() {
    StringBuilder filterBuilder = new StringBuilder("showwavespic=");
    filterBuilder.append("split_channels=0");
    filterBuilder.append("s=");
    filterBuilder.append(waveformImageWidth);
    filterBuilder.append("x");
    filterBuilder.append(waveformImageHeight);
    return filterBuilder.toString();
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

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setWorkingFileRepository(WorkingFileRepository workingFileRepository) {
    this.wfr = workingFileRepository;
  }
}
