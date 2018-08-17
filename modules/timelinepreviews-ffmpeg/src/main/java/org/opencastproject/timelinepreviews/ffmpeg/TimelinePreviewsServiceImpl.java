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

package org.opencastproject.timelinepreviews.ffmpeg;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.timelinepreviews.api.TimelinePreviewsException;
import org.opencastproject.timelinepreviews.api.TimelinePreviewsService;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.UUID;

/**
 * Media analysis plugin that takes a video stream and generates preview images that can be shown on the timeline.
 * This will be done using FFmpeg.
 */
public class TimelinePreviewsServiceImpl extends AbstractJobProducer implements
TimelinePreviewsService, ManagedService {

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "timelinepreviews";

  /** List of available operations on jobs */
  protected enum Operation {
    TimelinePreview
  };

  /** Path to the executable */
  protected String binary = FFMPEG_BINARY_DEFAULT;

  /** The key to look for in the service configuration file to override the DEFAULT_FFMPEG_BINARY */
  public static final String FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path";

  /** The default path to the FFmpeg binary */
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** Name of the constant used to retrieve the horizontal resolution */
  public static final String OPT_RESOLUTION_X = "resolutionX";

  /** Default value for the horizontal resolution */
  public static final int DEFAULT_RESOLUTION_X = 160;

  /** Name of the constant used to retrieve the vertical resolution */
  public static final String OPT_RESOLUTION_Y = "resolutionY";

  /** Default value for the vertical resolution */
  public static final int DEFAULT_RESOLUTION_Y = -1;

  /** Name of the constant used to retrieve the output file format */
  public static final String OPT_OUTPUT_FORMAT = "outputFormat";

  /** Default value for the format of the output image file */
  public static final String DEFAULT_OUTPUT_FORMAT = ".png";

  /** Name of the constant used to retrieve the mimetype */
  public static final String OPT_MIMETYPE = "mimetype";

  /** Default value for the mimetype of the generated image */
  public static final String DEFAULT_MIMETYPE = "image/png";


  /** The default job load of a timeline previews job */
  public static final float DEFAULT_TIMELINEPREVIEWS_JOB_LOAD = 0.1f;

  /** The key to look for in the service configuration file to override the DEFAULT_TIMELINEPREVIEWS_JOB_LOAD */
  public static final String TIMELINEPREVIEWS_JOB_LOAD_KEY = "job.load.timelinepreviews";

  /** The load introduced on the system by creating a caption job */
  private float timelinepreviewsJobLoad = DEFAULT_TIMELINEPREVIEWS_JOB_LOAD;

  /** The logging facility */
  protected static final Logger logger = LoggerFactory
    .getLogger(TimelinePreviewsServiceImpl.class);

  /** The horizontal resolution of a single preview image */
  protected int resolutionX = DEFAULT_RESOLUTION_X;

  /** The vertical resolution of a single preview image */
  protected int resolutionY = DEFAULT_RESOLUTION_Y;

  /** The file format of the generated preview images file */
  protected String outputFormat = DEFAULT_OUTPUT_FORMAT;

  /** The mimetype that will be set for the generated Attachment containing the timeline previews image */
  protected String mimetype = DEFAULT_MIMETYPE;


  /** Reference to the receipt service */
  protected ServiceRegistry serviceRegistry = null;

  /** The workspace to use when retrieving remote media files */
  protected Workspace workspace = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * Creates a new instance of the timeline previews service.
   */
  public TimelinePreviewsServiceImpl() {
    super(JOB_TYPE);
    this.binary = FFMPEG_BINARY_DEFAULT;
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Activate ffmpeg timeline previews service");
    final String path = cc.getBundleContext().getProperty(FFMPEG_BINARY_CONFIG);
    this.binary = path == null ? FFMPEG_BINARY_DEFAULT : path;
    logger.debug("Configuration {}: {}", FFMPEG_BINARY_CONFIG, FFMPEG_BINARY_DEFAULT);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    logger.debug("Configuring the timeline previews service");

    // Horizontal resolution
    if (properties.get(OPT_RESOLUTION_X) != null) {
      String res = (String) properties.get(OPT_RESOLUTION_X);
      try {
        resolutionX = Integer.parseInt(res);
        logger.info("Horizontal resolution set to {} pixels", resolutionX);
      } catch (Exception e) {
        throw new ConfigurationException(OPT_RESOLUTION_X, "Found illegal value '" + res
                + "' for timeline previews horizontal resolution");
      }
    }
    // Vertical resolution
    if (properties.get(OPT_RESOLUTION_Y) != null) {
      String res = (String) properties.get(OPT_RESOLUTION_Y);
      try {
        resolutionY = Integer.parseInt(res);
        logger.info("Vertical resolution set to {} pixels", resolutionY);
      } catch (Exception e) {
        throw new ConfigurationException(OPT_RESOLUTION_Y, "Found illegal value '" + res
                + "' for timeline previews vertical resolution");
      }
    }
    // Output file format
    if (properties.get(OPT_OUTPUT_FORMAT) != null) {
      String format = (String) properties.get(OPT_OUTPUT_FORMAT);
      try {
        outputFormat = format;
        logger.info("Output file format set to \"{}\"", outputFormat);
      } catch (Exception e) {
        throw new ConfigurationException(OPT_OUTPUT_FORMAT, "Found illegal value '" + format
                + "' for timeline previews output file format");
      }
    }
    // Output mimetype
    if (properties.get(OPT_MIMETYPE) != null) {
      String type = (String) properties.get(OPT_MIMETYPE);
      try {
        mimetype = type;
        logger.info("Mime type set to \"{}\"", mimetype);
      } catch (Exception e) {
        throw new ConfigurationException(OPT_MIMETYPE, "Found illegal value '" + type
                + "' for timeline previews mimetype");
      }
    }

    timelinepreviewsJobLoad = LoadUtil.getConfiguredLoadValue(properties, TIMELINEPREVIEWS_JOB_LOAD_KEY,
            DEFAULT_TIMELINEPREVIEWS_JOB_LOAD, serviceRegistry);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.timelinepreviews.api.TimelinePreviewsService#createTimelinePreviewImages(org.opencastproject.mediapackage.Track, int)
   */
  @Override
  public Job createTimelinePreviewImages(Track track, int imageCount) throws TimelinePreviewsException,
         MediaPackageException {
    try {
      List<String> parameters = Arrays.asList(MediaPackageElementParser.getAsXml(track), Integer.toString(imageCount));

      return serviceRegistry.createJob(JOB_TYPE,
          Operation.TimelinePreview.toString(),
          parameters,
          timelinepreviewsJobLoad);
    } catch (ServiceRegistryException e) {
      throw new TimelinePreviewsException("Unable to create timelinepreviews job", e);
    }
  }

  /**
   * Starts generation of timeline preview images for the given video track
   * and returns an attachment containing one image that contains all the
   * timeline preview images.
   *
   * @param job
   * @param track the element to analyze
   * @param imageCount number of preview images that will be generated
   * @return an attachment containing the resulting timeline previews image
   * @throws TimelinePreviewsException
   * @throws org.opencastproject.mediapackage.MediaPackageException
   */
  protected Attachment generatePreviewImages(Job job, Track track, int imageCount)
    throws TimelinePreviewsException, MediaPackageException {

    // Make sure the element can be analyzed using this analysis implementation
    if (!track.hasVideo()) {
      logger.error("Element {} is not a video track", track.getIdentifier());
      throw new TimelinePreviewsException("Element is not a video track");
    }

    try {

      if (track.getDuration() == null)
        throw new MediaPackageException("Track " + track + " does not have a duration");

      double duration = track.getDuration() / 1000.0;
      double seconds = duration / (double)(imageCount);
      seconds = seconds <= 0.0 ? 1.0 : seconds;

      // calculate number of tiles for row and column in tiled image
      int imageSize = (int) Math.ceil(Math.sqrt(imageCount));

      Attachment composedImage = createPreviewsFFmpeg(track, seconds, resolutionX, resolutionY, imageSize, imageSize,
              duration);


      if (composedImage == null)
        throw new IllegalStateException("Unable to compose image");

      // Set the mimetype
      try {
        composedImage.setMimeType(MimeTypes.parseMimeType(mimetype));
      } catch (IllegalArgumentException e) {
        logger.warn("Invalid mimetype provided for timeline previews image");
        try  {
          composedImage.setMimeType(MimeTypes.fromURI(composedImage.getURI()));
        } catch (UnknownFileTypeException ex) {
          logger.warn("No valid mimetype could be found for timeline previews image");
        }
      }

      composedImage.getProperties().put("imageCount", String.valueOf(imageCount));

      return composedImage;

    } catch (Exception e) {
      logger.warn("Error creating timeline preview images for " + track, e);
      if (e instanceof TimelinePreviewsException) {
        throw (TimelinePreviewsException) e;
      } else {
        throw new TimelinePreviewsException(e);
      }
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
        case TimelinePreview:
          Track track = (Track) MediaPackageElementParser
            .getFromXml(arguments.get(0));
          int imageCount = Integer.parseInt(arguments.get(1));
          Attachment timelinePreviewsMpe = generatePreviewImages(job, track, imageCount);
          return MediaPackageElementParser.getAsXml(timelinePreviewsMpe);
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Executes the FFmpeg command to generate a timeline previews image
   *
   * @param track the track to generate the timeline previews image for
   * @param seconds the length of a segment that one preview image should represent
   * @param width the width of a single preview image
   * @param height the height of a single preview image
   * @param tileX the horizontal number of preview images that are stored in the timeline previews image
   * @param tileY the vertical number of preview images that are stored in the timeline previews image
   * @param duration the duration for which preview images should be generated
   * @return an attachment containing the timeline previews image
   * @throws TimelinePreviewsException
   */
  protected Attachment createPreviewsFFmpeg(Track track, double seconds, int width, int height, int tileX, int tileY,
          double duration) throws TimelinePreviewsException {

    // copy source file into workspace
    File mediaFile;
    try {
      mediaFile = workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new TimelinePreviewsException(
          "Error finding the media file in the workspace", e);
    } catch (IOException e) {
      throw new TimelinePreviewsException(
          "Error reading the media file in the workspace", e);
    }

    String imageFilePath = FilenameUtils.removeExtension(mediaFile.getAbsolutePath()) + '_' + UUID.randomUUID()
                           + "_timelinepreviews" + outputFormat;
    int exitCode = 1;
    String[] command = new String[] {
      binary,
      "-loglevel", "error",
      "-t", String.valueOf(duration - seconds / 2.0),
      "-i", mediaFile.getAbsolutePath(),
      "-vf", "fps=1/" + seconds + ",scale=" + width + ":" + height + ",tile=" + tileX + "x" + tileY,
      imageFilePath
    };

    logger.debug("Start timeline previews ffmpeg process: {}", StringUtils.join(command, " "));
    logger.info("Create timeline preview images file for track '{}' at {}", track.getIdentifier(), imageFilePath);

    ProcessBuilder pbuilder = new ProcessBuilder(command);

    pbuilder.redirectErrorStream(true);
    Process ffmpegProcess = null;
    exitCode = 1;
    BufferedReader errStream = null;
    try {
      ffmpegProcess = pbuilder.start();

      errStream = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
      String line = errStream.readLine();
      while (line != null) {
        logger.error("FFmpeg error: " + line);
        line = errStream.readLine();
      }
      exitCode = ffmpegProcess.waitFor();
    } catch (IOException ex) {
      throw new TimelinePreviewsException("Starting ffmpeg process failed", ex);
    } catch (InterruptedException ex) {
      throw new TimelinePreviewsException("Timeline preview creation was unexpectedly interrupted", ex);
    } finally {
      IoSupport.closeQuietly(ffmpegProcess);
      IoSupport.closeQuietly(errStream);
      if (exitCode != 0) {
        try {
          FileUtils.forceDelete(new File(imageFilePath));
        } catch (IOException e) {
          // it is ok, no output file was generated by ffmpeg
        }
      }
    }

    if (exitCode != 0)
      throw new TimelinePreviewsException("Generating timeline preview for track " + track.getIdentifier()
              + " failed: ffmpeg process exited abnormally with exit code " + exitCode);

    // put timeline previews image into workspace
    FileInputStream timelinepreviewsFileInputStream = null;
    URI previewsFileUri = null;
    try {
      timelinepreviewsFileInputStream = new FileInputStream(imageFilePath);
      previewsFileUri = workspace.putInCollection(COLLECTION_ID,
              FilenameUtils.getName(imageFilePath), timelinepreviewsFileInputStream);
      logger.info("Copied the created timeline preview images file to the workspace {}", previewsFileUri.toString());
    } catch (FileNotFoundException ex) {
      throw new TimelinePreviewsException(
              String.format("Timeline previews image file '%s' not found", imageFilePath), ex);
    } catch (IOException ex) {
      throw new TimelinePreviewsException(
              String.format("Can't write timeline preview images file '%s' to workspace", imageFilePath), ex);
    } catch (IllegalArgumentException ex) {
      throw new TimelinePreviewsException(ex);
    } finally {
      IoSupport.closeQuietly(timelinepreviewsFileInputStream);
      logger.info("Deleted local timeline preview images file at {}", imageFilePath);
      FileUtils.deleteQuietly(new File(imageFilePath));
    }

    // create media package element
    MediaPackageElementBuilder mpElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    // it is up to the workflow operation handler to set the attachment flavor
    Attachment timelinepreviewsMpe = (Attachment) mpElementBuilder.elementFromURI(
            previewsFileUri, MediaPackageElement.Type.Attachment, track.getFlavor());

    // add reference to track
    timelinepreviewsMpe.referTo(track);

    // add additional properties to attachment
    timelinepreviewsMpe.getProperties().put("imageSizeX", String.valueOf(tileX));
    timelinepreviewsMpe.getProperties().put("imageSizeY", String.valueOf(tileY));
    timelinepreviewsMpe.getProperties().put("resolutionX", String.valueOf(resolutionX));
    timelinepreviewsMpe.getProperties().put("resolutionY", String.valueOf(resolutionY));

    // set the flavor and an ID
    timelinepreviewsMpe.setFlavor(track.getFlavor());
    timelinepreviewsMpe.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew().compact());

    return timelinepreviewsMpe;
  }

  /**
   * Sets the workspace
   *
   * @param workspace
   *            an instance of the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   *
   * @param serviceRegistry
   *            the service registry
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
   *            the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *            the userDirectoryService to set
   */
  public void setUserDirectoryService(
      UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *            the organization directory
   */
  public void setOrganizationDirectoryService(
      OrganizationDirectoryService organizationDirectory) {
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
