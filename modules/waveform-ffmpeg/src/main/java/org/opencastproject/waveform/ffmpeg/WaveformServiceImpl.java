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
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
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
import java.util.concurrent.TimeUnit;


/**
 * This service creates a waveform image from a media file with at least one audio channel.
 * This will be done using ffmpeg.
 */
public class WaveformServiceImpl extends AbstractJobProducer implements WaveformService, ManagedService {

  /** The logging facility */
  protected static final Logger logger = LoggerFactory.getLogger(WaveformServiceImpl.class);

  /** Path to the executable */
  protected String binary = DEFAULT_FFMPEG_BINARY;

  /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_JOB_LOAD */
  public static final String WAVEFORM_JOB_LOAD_CONFIG_KEY = "job.load.waveform";

  /** The default job load of a waveform job */
  public static final float DEFAULT_WAVEFORM_JOB_LOAD = 0.1f;

  /** The key to look for in the service configuration file to override the DEFAULT_FFMPEG_BINARY */
  public static final String FFMPEG_BINARY_CONFIG_KEY = "org.opencastproject.composer.ffmpeg.path";

  /** The default path to the ffmpeg binary */
  public static final String DEFAULT_FFMPEG_BINARY = "ffmpeg";

  /** The default waveform image scale algorithm */
  public static final String DEFAULT_WAVEFORM_SCALE = "lin";

  /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_SCALE */
  public static final String WAVEFORM_SCALE_CONFIG_KEY = "waveform.scale";

  /** The default value if the waveforms (per audio channel) should be renderen next to each other (if true)
   * or on top of each other (if false) */
  public static final boolean DEFAULT_WAVEFORM_SPLIT_CHANNELS = false;

  /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_SPLIT_CHANNELS */
  public static final String WAVEFORM_SPLIT_CHANNELS_CONFIG_KEY = "waveform.split.channels";

  /** The default waveform colors per audio channel */
  public static final String[] DEFAULT_WAVEFORM_COLOR = { "black" };

  /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_COLOR */
  public static final String WAVEFORM_COLOR_CONFIG_KEY = "waveform.color";

  /** The default filter to be optionally prepended to the showwavespic filter */
  public static final String DEFAULT_WAVEFORM_FILTER_PRE = null;

  /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_FILTER_PRE */
  public static final String WAVEFORM_FILTER_PRE_CONFIG_KEY = "waveform.filter.pre";

  /** The default filter to be optionally appended to the showwavespic filter */
  public static final String DEFAULT_WAVEFORM_FILTER_POST = null;

  /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_FILTER_POST */
  public static final String WAVEFORM_FILTER_POST_CONFIG_KEY = "waveform.filter.post";

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "waveform";

  /** List of available operations on jobs */
  enum Operation {
    Waveform
  };

  /** The waveform job load */
  private float waveformJobLoad = DEFAULT_WAVEFORM_JOB_LOAD;

  /** The waveform image scale algorithm */
  private String waveformScale = DEFAULT_WAVEFORM_SCALE;

  /** The value if the waveforms (per audio channel) should be rendered next to each other (if true)
   * or on top of each other (if false) */
  private boolean waveformSplitChannels = DEFAULT_WAVEFORM_SPLIT_CHANNELS;

  /** The waveform colors per audio channel */
  private String[] waveformColor = DEFAULT_WAVEFORM_COLOR;

  /** Filter to be prepended to the showwavespic filter */
  private String waveformFilterPre = DEFAULT_WAVEFORM_FILTER_PRE;

  /** Filter to be appended to the showwavespic filter */
  private String waveformFilterPost = DEFAULT_WAVEFORM_FILTER_POST;

  /** Reference to the service registry */
  private ServiceRegistry serviceRegistry = null;

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
    logger.info("Activate ffmpeg waveform service");
    final String path = cc.getBundleContext().getProperty(FFMPEG_BINARY_CONFIG_KEY);
    binary = (path == null ? DEFAULT_FFMPEG_BINARY : path);
    logger.debug("ffmpeg binary set to {}", binary);
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    logger.debug("Configuring the waveform service");
    waveformJobLoad = LoadUtil.getConfiguredLoadValue(properties,
            WAVEFORM_JOB_LOAD_CONFIG_KEY, DEFAULT_WAVEFORM_JOB_LOAD, serviceRegistry);

    Object val = properties.get(WAVEFORM_SCALE_CONFIG_KEY);
    if (val != null) {
      if (StringUtils.isNotEmpty((String) val)) {
        if (!"lin".equals(val) && !"log".equals(val)) {
          logger.warn("Waveform scale configuration value '{}' is not in set of predefined values (lin, log). "
                  + "The waveform image extraction job may fail.", val);
        }
        waveformScale = (String) val;
      }
    }

    val = properties.get(WAVEFORM_SPLIT_CHANNELS_CONFIG_KEY);
    if (val != null) {
      waveformSplitChannels = Boolean.parseBoolean((String) val);
    }

    val = properties.get(WAVEFORM_COLOR_CONFIG_KEY);
    if (val != null && StringUtils.isNotEmpty((String) val)) {
      String colorValue = (String) val;
      if (StringUtils.isNotEmpty(colorValue) && StringUtils.isNotBlank(colorValue)) {
        waveformColor = StringUtils.split(colorValue, ", |:;");
      }
    }

    val = properties.get(WAVEFORM_FILTER_PRE_CONFIG_KEY);
    if (val != null) {
      waveformFilterPre = StringUtils.trimToNull((String) val);
    } else {
      waveformFilterPre = null;
    }

    val = properties.get(WAVEFORM_FILTER_POST_CONFIG_KEY);
    if (val != null) {
      waveformFilterPost = StringUtils.trimToNull((String) val);
    } else {
      waveformFilterPost = null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.waveform.api.WaveformService#createWaveformImage(org.opencastproject.mediapackage.Track,
   *         int, int, int, int, String)
   */
  @Override
  public Job createWaveformImage(Track sourceTrack, int pixelsPerMinute, int minWidth, int maxWidth, int height, String color)
      throws MediaPackageException, WaveformServiceException {
    try {
      return serviceRegistry.createJob(jobType, Operation.Waveform.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), Integer.toString(pixelsPerMinute),
                Integer.toString(minWidth), Integer.toString(maxWidth), Integer.toString(height), color), waveformJobLoad);
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
          int pixelsPerMinute = Integer.parseInt(arguments.get(1));
          int minWidth = Integer.parseInt(arguments.get(2));
          int maxWidth = Integer.parseInt(arguments.get(3));
          int height = Integer.parseInt(arguments.get(4));
          String color = arguments.get(5);
          Attachment waveformMpe = extractWaveform(track, pixelsPerMinute, minWidth, maxWidth, height, color);
          return MediaPackageElementParser.getAsXml(waveformMpe);
        default:
          throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'");
      }
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (MediaPackageException | WaveformServiceException e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Create and run waveform extraction ffmpeg command.
   *
   * @param track source audio/video track with at least one audio channel
   * @param pixelsPerMinute width of waveform image in pixels per minute
   * @param minWidth minimum width of waveform image
   * @param maxWidth maximum width of waveform image
   * @param height height of waveform image
   * @param color color of waveform image
   * @return waveform image attachment
   * @throws WaveformServiceException if processing fails
   */
  private Attachment extractWaveform(Track track, int pixelsPerMinute, int minWidth, int maxWidth, int height, String color)
    throws WaveformServiceException {
    if (!track.hasAudio()) {
      throw new WaveformServiceException("Track has no audio");
    }

    // copy source file into workspace
    File mediaFile;
    try {
      mediaFile = workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new WaveformServiceException(
          "Error finding the media file in the workspace", e);
    } catch (IOException e) {
      throw new WaveformServiceException(
          "Error reading the media file in the workspace", e);
    }

    String waveformFilePath = FilenameUtils.removeExtension(mediaFile.getAbsolutePath())
            .concat('-' + track.getIdentifier()).concat("-waveform.png");

    int width = getWaveformImageWidth(track, pixelsPerMinute, minWidth, maxWidth);

    // create ffmpeg command
    String[] command = new String[] {
      binary,
      "-nostats", "-nostdin", "-hide_banner",
      "-i", mediaFile.getAbsolutePath(),
      "-lavfi", createWaveformFilter(track, width, height, color),
      "-frames:v", "1",
      "-an", "-vn", "-sn",
      waveformFilePath
    };
    logger.debug("Start waveform ffmpeg process: {}", StringUtils.join(command, " "));
    logger.info("Create waveform image file for track '{}' at {}", track.getIdentifier(), waveformFilePath);

    // run ffmpeg
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process ffmpegProcess = null;
    int exitCode = 1;
    BufferedReader errStream = null;
    try {
      ffmpegProcess = pb.start();

      errStream = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
      String line = errStream.readLine();
      while (line != null) {
        logger.debug(line);
        line = errStream.readLine();
      }

      exitCode = ffmpegProcess.waitFor();
    } catch (IOException ex) {
      throw new WaveformServiceException("Start ffmpeg process failed", ex);
    } catch (InterruptedException ex) {
      throw new WaveformServiceException("Waiting for encoder process exited was interrupted unexpectedly", ex);
    } finally {
      IoSupport.closeQuietly(ffmpegProcess);
      IoSupport.closeQuietly(errStream);
      if (exitCode != 0) {
        try {
          FileUtils.forceDelete(new File(waveformFilePath));
        } catch (IOException e) {
          // it is ok, no output file was generated by ffmpeg
        }
      }
    }

    if (exitCode != 0)
      throw new WaveformServiceException(String.format("The encoder process exited abnormally with exit code %s "
              + "using command\n%s", exitCode, String.join(" ", command)));

    // put waveform image into workspace
    FileInputStream waveformFileInputStream = null;
    URI waveformFileUri;
    try {
      waveformFileInputStream = new FileInputStream(waveformFilePath);
      waveformFileUri = workspace.putInCollection(COLLECTION_ID,
              FilenameUtils.getName(waveformFilePath), waveformFileInputStream);
      logger.info("Copied the created waveform to the workspace {}", waveformFileUri);
    } catch (FileNotFoundException ex) {
      throw new WaveformServiceException(String.format("Waveform image file '%s' not found", waveformFilePath), ex);
    } catch (IOException ex) {
      throw new WaveformServiceException(String.format(
              "Can't write waveform image file '%s' to workspace", waveformFilePath), ex);
    } catch (IllegalArgumentException ex) {
      throw new WaveformServiceException(ex);
    } finally {
      IoSupport.closeQuietly(waveformFileInputStream);
      logger.info("Deleted local waveform image file at {}", waveformFilePath);
      FileUtils.deleteQuietly(new File(waveformFilePath));
    }

    // create media package element
    MediaPackageElementBuilder mpElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    // it is up to the workflow operation handler to set the attachment flavor
    Attachment waveformMpe = (Attachment) mpElementBuilder.elementFromURI(
            waveformFileUri, Type.Attachment, track.getFlavor());
    waveformMpe.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew().compact());
    return waveformMpe;
  }

  /**
   * Create an ffmpeg waveform filter with parameters based on input track and service configuration.
   *
   * @param track source audio/video track with at least one audio channel
   * @param width width of waveform image
   * @param height height of waveform image
   * @param color color of waveform image
   * @return ffmpeg filter parameter
   */
  private String createWaveformFilter(Track track, int width, int height, String color) {
    StringBuilder filterBuilder = new StringBuilder("");
    if (waveformFilterPre != null) {
      filterBuilder.append(waveformFilterPre);
      filterBuilder.append(",");
    }
    String[] waveformOperationColors = null;
    //If the color was set, override the defaults
    if (StringUtils.isNotBlank(color)) {
      waveformOperationColors = StringUtils.split(color, "|");
    } else {
      waveformOperationColors = waveformColor;
    }
    filterBuilder.append("showwavespic=");
    filterBuilder.append("split_channels=");
    filterBuilder.append(waveformSplitChannels ? 1 : 0);
    filterBuilder.append(":s=");
    filterBuilder.append(width);
    filterBuilder.append("x");
    filterBuilder.append(height);
    filterBuilder.append(":scale=");
    filterBuilder.append(waveformScale);
    filterBuilder.append(":colors=");
    filterBuilder.append(StringUtils.join(Arrays.asList(waveformOperationColors), "|"));
    if (waveformFilterPost != null) {
      filterBuilder.append(",");
      filterBuilder.append(waveformFilterPost);
    }
    return filterBuilder.toString();
  }

  /**
   * Return the waveform image width build from input track and service configuration.
   *
   * @param track source audio/video track with at least one audio channel
   * @param pixelsPerMinute width of waveform image in pixels per minute
   * @param minWidth minimum width of waveform image
   * @param maxWidth maximum width of waveform image
   * @return waveform image width
   */
  private int getWaveformImageWidth(Track track, int pixelsPerMinute, int minWidth, int maxWidth) {
    int imageWidth = minWidth;
    if (track.getDuration() > 0) {
      int trackDurationMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(track.getDuration());
      if (pixelsPerMinute > 0 && trackDurationMinutes > 0) {
        imageWidth = Math.max(minWidth, trackDurationMinutes * pixelsPerMinute);
        imageWidth = Math.min(maxWidth, imageWidth);
      }
    }
    return imageWidth;
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
}
