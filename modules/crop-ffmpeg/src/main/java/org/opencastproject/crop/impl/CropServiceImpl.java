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

package org.opencastproject.crop.impl;

import org.opencastproject.crop.api.CropException;
import org.opencastproject.crop.api.CropService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/**
 * Media analysis plugin that takes a video stream and removes black bars on each side
 * <p>
 * This plugin runs
 * <pre>
 *     ffmpeg -i input.file -vf cropdetect=24:16:0 -max_muxing_qurue_size 2000 -f null -
 *
 *     ffmpeg -i input.file -vf startCropping=wi-2*x:hi:x:0 -max_muxing_queue_size 2000 -y output.file
 * </pre>
 */
public class CropServiceImpl extends AbstractJobProducer implements CropService, ManagedService {

  /**
   * Resulting collection in the working file repository
   */
  public static final String COLLECTION_ID = "cropping";

  /**
   * List of available operations on jobs
   */
  private enum Operation {
    Crop
  }

  /**
   * Path to the executable
   */
  protected String binary;

  public static final String FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path";
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /**
   * The load introduced on the system by creating a caption job
   */
  public static final float DEFAULT_CROP_JOB_LOAD = 1.0f;

  /**
   * The key to look for in the service configuration file to override the DEFAULT_CROP_JOB_LOAD
   */
  public static final String CROP_JOB_LOAD_KEY = "org.opencastproject.job.load.cropping";

  /**
   * The load introduced on the system by creating a cropping job
   */
  private float cropJobLoad = DEFAULT_CROP_JOB_LOAD;

  /**
   * The threshold of greyscale to use for cropping
   */
  public static final String CROP_FFMPEG_GREYSCALE_LIMIT = "org.opencastproject.ffmpeg.cropping.greyscale.limit";
  public static final String DEFAULT_CROP_GREYSCALE_LIMIT = "24";

  private String greyScaleLimit = DEFAULT_CROP_GREYSCALE_LIMIT;

  /**
   * The value which the width/height should be divisible by
   */
  public static final String CROP_FFMPEG_ROUND = "org.opencastproject.ffmpeg.cropping.round";
  public static final String DEFAULT_CROP_FFMPEG_ROUND = "16";

  private String round = DEFAULT_CROP_FFMPEG_ROUND;
  /**
   * The counter that determines after how many frames cropdetect will be executed again
   */
  public static final String CROP_FFMPEG_RESET = "org.opencastproject.ffmpeg.cropping.reset";
  public static final String DEFAULT_CROP_FFMPEG_RESET = "240";

  private String reset = DEFAULT_CROP_FFMPEG_RESET;

  /**
   * The logging facility
   */
  private static final Logger logger = LoggerFactory.getLogger(CropServiceImpl.class);

  /**
   * Reference to the receipt service
   */
  private ServiceRegistry serviceRegistry = null;

  /**
   * The mpeg7 service
   */
  private Mpeg7CatalogService mpeg7CatalogService = null;

  /**
   * The workspace to use when retrieving remote media files
   */
  private Workspace workspace = null;

  private SecurityService securityService = null;

  private OrganizationDirectoryService organizationDirectoryService = null;

  private UserDirectoryService userDirectoryService = null;

  /**
   * Creates a new instance of the startCropping service.
   */
  public CropServiceImpl() {
    super(JOB_TYPE);
    this.binary = FFMPEG_BINARY_DEFAULT;
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    final String path = cc.getBundleContext().getProperty(FFMPEG_BINARY_CONFIG);
    this.binary = StringUtils.defaultIfBlank(path, FFMPEG_BINARY_DEFAULT);
    logger.debug("Configuration {}: {}", FFMPEG_BINARY_CONFIG, binary);
  }

  private Track startCropping(Track track) throws CropException {

    if (!track.hasVideo()) {
      throw new CropException("Element is not a video track");
    }

    File mediaFile;
    try {
      mediaFile = workspace.get(track.getURI());
    } catch (NotFoundException | IOException e) {
      throw new CropException("Error loading the video file into the workspace", e);
    }

    logger.info("Starting cropping of {}", track);

    File croppedMedia = cropFFmpeg(mediaFile);

    Track cropTrack = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .newElement(Track.TYPE, track.getFlavor());
    cropTrack.setURI(croppedMedia.toURI());

    logger.info("Finished video cropping of {}", track.getURI());
    return cropTrack;
  }

  private File cropFFmpeg(File mediaFile) throws CropException {
    String[] command = new String[] { binary, "-i", mediaFile.getAbsolutePath(), "-vf", "cropdetect="
            + greyScaleLimit + ":" + round + ":" + reset,
            "-max_muxing_queue_size", "2000", "-f", "null", "-"};
    String commandline = StringUtils.join(command, " ");

    logger.info("Running {}", commandline);

    ProcessBuilder pbuilder = new ProcessBuilder(command);
    int cropValue = 0;
    int widthVideo = 0;
    String crop = null;
    pbuilder.redirectErrorStream(true);
    int exitCode = 1;
    Process process;
    try {
      process = pbuilder.start();
      try (BufferedReader errStream = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line = errStream.readLine();
        while (null != line) {
          if (line.startsWith("[Parsed_cropdetect")) {
            // [Parsed_cropdetect_0 @ 0x8f6620] x1:120 x2:759 y1:0 y2:399 w:640 h:400 x:120 y:0 pts:639356 t:39.023193 startCropping=640:400:120:0
            String[] lineSplitted = line.split(" ");
            widthVideo = Integer.valueOf(lineSplitted[7].substring(2));
            int x = Integer.valueOf(lineSplitted[9].substring(2));
            if (cropValue == 0 || cropValue > x) {
              cropValue = x;
              crop = lineSplitted[13];
            }

          }
          line = errStream.readLine();
        }
      }
      exitCode = process.waitFor();

    } catch (IOException e) {
      logger.error("Error executing FFmpeg", e);
    } catch (InterruptedException e) {
      logger.error("Waiting for encoder process exited was interrupted unexpected", e);
    }

    if (exitCode != 0) {
      throw new CropException("The encoder process exited abnormally with exit code " + exitCode);
    }
    if (cropValue > widthVideo / 3) {
      return mediaFile;
    }
    // FFmpeg command for cropping video
    logger.info("String for startCropping command: {}", crop);
    String croppedOutputPath = FilenameUtils.removeExtension(mediaFile.getAbsolutePath()).concat(RandomStringUtils
            .randomAlphanumeric(8) + ".mp4");
    String[] cropCommand = new String[] { binary, "-i", mediaFile.getAbsolutePath(), "-vf", crop,
            "-max_muxing_queue_size", "2000", croppedOutputPath };
    String cropCommandline = StringUtils.join(cropCommand, " ");

    logger.info("Running {}", cropCommandline);

    try {
      pbuilder = new ProcessBuilder(cropCommand);
      process = pbuilder.start();
      //wait until the task is finished
      exitCode = process.waitFor();
    } catch (InterruptedException | IOException e) {
      throw new CropException("Ffmpeg process interrupted", e);
    }
    if (exitCode != 0) {
      throw new CropException("Ffmpeg exited abnormally with status " + exitCode);
    }

    return new File(croppedOutputPath);
  }

  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      switch (op) {
        case Crop:
          Track track = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          Track croppedTrack = startCropping(track);
          return MediaPackageElementParser.getAsXml(croppedTrack);
        default:
          throw new IllegalStateException("Don't know how to handle operations '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type: " + op, e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operations '" + op + "' does not meet expectations",
              e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operations: " + op, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see CropService#crop(org.opencastproject.mediapackage.Track)
   */
  @Override
  public Job crop(Track track) throws CropException, MediaPackageException {
    try {
      return serviceRegistry
              .createJob(JOB_TYPE, Operation.Crop.toString(), Arrays.asList(MediaPackageElementParser.getAsXml(track)),
                      cropJobLoad);
    } catch (ServiceRegistryException e) {
      throw new CropException("Unable to create a job", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
    if (dictionary == null) {
      return;
    }
    logger.debug("Configuring the cropper");

    if (dictionary.get(CROP_FFMPEG_GREYSCALE_LIMIT) != null) {
      String limit = (String) dictionary.get(CROP_FFMPEG_GREYSCALE_LIMIT);
      try {
        greyScaleLimit = limit;
        logger.info("Changes greyscale limit to {}", greyScaleLimit);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for greyscale limit", limit);
      }
    }

    if (dictionary.get(CROP_FFMPEG_ROUND) != null) {
      String r = (String) dictionary.get(CROP_FFMPEG_ROUND);
      try {
        round = r;
        logger.info("Changes round to {}", round);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for round", r);
      }
    }

    if (dictionary.get(CROP_FFMPEG_RESET) != null) {
      String re = (String) dictionary.get(CROP_FFMPEG_RESET);
      try {
        reset = re;
        logger.info("Changes reset to {}", reset);
      } catch (Exception e) {
        logger.warn("Found illegal value {} for reset", re);
      }
    }

    cropJobLoad = LoadUtil
            .getConfiguredLoadValue(dictionary, CROP_JOB_LOAD_KEY, DEFAULT_CROP_JOB_LOAD, serviceRegistry);

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

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
  }

  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}

