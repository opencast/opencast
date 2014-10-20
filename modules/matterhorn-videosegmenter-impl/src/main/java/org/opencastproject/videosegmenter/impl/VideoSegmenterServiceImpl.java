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
package org.opencastproject.videosegmenter.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.MediaLocator;
import org.opencastproject.metadata.mpeg7.MediaLocatorImpl;
import org.opencastproject.metadata.mpeg7.MediaRelTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.videosegmenter.api.VideoSegmenterException;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.LineReader;

/**
 * Media analysis plugin that takes a video stream and extracts video segments
 * by trying to detect slide and/or scene changes.
 *
 * This plugin runs
 *
 * <pre>
 * ffmpeg -nostats -i in.mp4 -filter:v 'select=gt(scene\,0.04),showinfo' -f null - 2>&1 | grep Parsed_showinfo_1
 * </pre>
 */
public class VideoSegmenterServiceImpl extends AbstractJobProducer implements
VideoSegmenterService, ManagedService {

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "videosegments";

  /** List of available operations on jobs */
  private enum Operation {
    Segment
  };

  /** Path to the executable */
  protected String binary;

  public static final String FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path";
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** Name of the constant used to retreive the stability threshold */
  public static final String OPT_STABILITY_THRESHOLD = "stabilitythreshold";

  /** The number of seconds that need to resemble until a scene is considered "stable" */
  public static final int DEFAULT_STABILITY_THRESHOLD = 5;

  /** Name of the constant used to retreive the changes threshold */
  public static final String OPT_CHANGES_THRESHOLD = "changesthreshold";

  /** Default value for the number of pixels that may change between two frames without considering them different */
  public static final float DEFAULT_CHANGES_THRESHOLD = 0.05f; // 5% change

  /** The logging facility */
  protected static final Logger logger = LoggerFactory
    .getLogger(VideoSegmenterServiceImpl.class);

  /** Number of pixels that may change between two frames without considering them different */
  protected float changesThreshold = DEFAULT_CHANGES_THRESHOLD;

  /** The number of seconds that need to resemble until a scene is considered "stable" */
  protected int stabilityThreshold = DEFAULT_STABILITY_THRESHOLD;

  /** Reference to the receipt service */
  protected ServiceRegistry serviceRegistry = null;

  /** The mpeg-7 service */
  protected Mpeg7CatalogService mpeg7CatalogService = null;

  /** The workspace to ue when retrieving remote media files */
  protected Workspace workspace = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * Creates a new instance of the video segmenter service.
   */
  public VideoSegmenterServiceImpl() {
    super(JOB_TYPE);
    this.binary = FFMPEG_BINARY_DEFAULT;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    logger.debug("Configuring the videosegmenter");

    // Stability threshold
    if (properties.get(OPT_STABILITY_THRESHOLD) != null) {
      String threshold = (String) properties.get(OPT_STABILITY_THRESHOLD);
      try {
        stabilityThreshold = Integer.parseInt(threshold);
        logger.info("Stability threshold set to {} consecutive frames", stabilityThreshold);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's stability threshold", threshold);
      }
    }

    // Changes threshold
    if (properties.get(OPT_CHANGES_THRESHOLD) != null) {
      String threshold = (String) properties.get(OPT_CHANGES_THRESHOLD);
      try {
        changesThreshold = Float.parseFloat(threshold);
        logger.info("Changes threshold set to {}", changesThreshold);
      } catch (Exception e) {
        logger.warn("Found illegal value '{}' for videosegmenter's changes threshold", threshold);
      }
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.videosegmenter.api.VideoSegmenterService#segment(org.opencastproject.mediapackage.Track)
   */
  public Job segment(Track track) throws VideoSegmenterException,
         MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE,
          Operation.Segment.toString(),
          Arrays.asList(MediaPackageElementParser.getAsXml(track)));
    } catch (ServiceRegistryException e) {
      throw new VideoSegmenterException("Unable to create a job", e);
    }
  }

  /**
   * Starts segmentation on the video track identified by
   * <code>mediapackageId</code> and <code>elementId</code> and returns a
   * receipt containing the final result in the form of anMpeg7Catalog.
   *
   * @param track
   *            the element to analyze
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws VideoSegmenterException
   */
  protected Catalog segment(Job job, Track track)
    throws VideoSegmenterException, MediaPackageException {

    // Make sure the element can be analyzed using this analysis
    // implementation
    if (!track.hasVideo()) {
      logger.warn("Element {} is not a video track", track);
      throw new VideoSegmenterException("Element is not a video track");
    }

    try {
      Mpeg7Catalog mpeg7 = mpeg7CatalogService.newInstance();

      File mediaFile = null;
      URL mediaUrl = null;
      try {
        mediaFile = workspace.get(track.getURI());
        mediaUrl = mediaFile.toURI().toURL();
      } catch (NotFoundException e) {
        throw new VideoSegmenterException(
            "Error finding the mjpeg in the workspace", e);
      } catch (IOException e) {
        throw new VideoSegmenterException(
            "Error reading the mjpeg in the workspace", e);
      }

      if (track.getDuration() == null)
        throw new MediaPackageException("Track " + track
            + " does not have a duration");
      long durationInSeconds = Math.min(track.getDuration() / 1000,
          (long) track.getDuration());
      logger.info("Track {} loaded, duration is {} s", mediaUrl,
          durationInSeconds);

      MediaTime contentTime = new MediaRelTimeImpl(0,
          (long) durationInSeconds * 1000);
      MediaLocator contentLocator = new MediaLocatorImpl(track.getURI());
      Video videoContent = mpeg7.addVideoContent("videosegment",
          contentTime, contentLocator);

      logger.info("Starting video segmentation of {}", mediaUrl);
      String[] command = new String[] { binary, "-nostats", "-i",
        mediaFile.getAbsolutePath().replaceAll(" ", "\\ "),
        "-filter:v", "select=gt(scene\\," + changesThreshold + "),showinfo",
        "-f", "null", "-"
      };
      String commandline = StringUtils.join(command, " ");

      logger.info("Running {}", commandline);

      ProcessBuilder pbuilder = new ProcessBuilder(command);
      List<String> segmentsStrings = new LinkedList<String>();
      try {
        Process process = pbuilder.start();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getErrorStream()));
        LineReader lr = new LineReader(reader);
        String line = lr.readLine();
        while (null != line) {
          if (line.startsWith("[Parsed_showinfo")) {
            segmentsStrings.add(line);
          }
          line = lr.readLine();
        }
      } catch (IOException e) {
        logger.error("Error executing ffmpeg: {}", e.getMessage());
      }

      // [Parsed_showinfo_1 @ 0x157fb40] n:0 pts:12 pts_time:12 pos:227495
      // fmt:rgb24 sar:0/1 s:320x240 i:P iskey:1 type:I checksum:8DF39EA9
      // plane_checksum:[8DF39EA9]

      int segmentcount = 1;
      List<Segment> segments = new LinkedList<Segment>();
      if (segmentsStrings.size() == 0) {
        Segment s = videoContent.getTemporalDecomposition()
          .createSegment("segement-" + segmentcount);
        s.setMediaTime(new MediaRelTimeImpl(0, track.getDuration()));
        segments.add(s);
      } else {
        long starttime = 0;
        long endtime = 0;
        for (String seginfo : segmentsStrings) {
          Pattern pattern = Pattern.compile("pts_time\\:\\d+");
          Matcher matcher = pattern.matcher(seginfo);
          String time = "0";
          while (matcher.find()) {
            time = matcher.group().substring(9);
          }
          endtime = Long.parseLong(time) * 1000;
          long segmentLength = endtime - starttime;
          if (1000 * stabilityThreshold < segmentLength) {
            Segment segement = videoContent.getTemporalDecomposition()
              .createSegment("segement-" + segmentcount);
            segement.setMediaTime(new MediaRelTimeImpl(starttime,
              endtime - starttime));
            segments.add(segement);
            segmentcount++;
            starttime = endtime;
          }
        }
        // Add last segment
        Segment s = videoContent.getTemporalDecomposition()
          .createSegment("segement-" + segmentcount);
        s.setMediaTime(new MediaRelTimeImpl(endtime, track
              .getDuration() - endtime));
        segments.add(s);
      }

      logger.info("Segmentation of {} yields {} segments", mediaUrl,
          segments.size());

      Catalog mpeg7Catalog = (Catalog) MediaPackageElementBuilderFactory
        .newInstance().newElementBuilder()
        .newElement(Catalog.TYPE, MediaPackageElements.SEGMENTS);
      URI uri;
      try {
        uri = workspace.putInCollection(COLLECTION_ID, job.getId()
            + ".xml", mpeg7CatalogService.serialize(mpeg7));
      } catch (IOException e) {
        throw new VideoSegmenterException(
            "Unable to put the mpeg7 catalog into the workspace", e);
      }
      mpeg7Catalog.setURI(uri);

      logger.info("Finished video segmentation of {}", mediaUrl);
      return mpeg7Catalog;
    } catch (Exception e) {
      logger.warn("Error segmenting " + track, e);
      if (e instanceof VideoSegmenterException) {
        throw (VideoSegmenterException) e;
      } else {
        throw new VideoSegmenterException(e);
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
        case Segment:
          Track track = (Track) MediaPackageElementParser
            .getFromXml(arguments.get(0));
          Catalog catalog = segment(job, track);
          return MediaPackageElementParser.getAsXml(catalog);
        default:
          throw new IllegalStateException(
              "Don't know how to handle operation '" + operation
              + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException(
          "This service can't handle operations of type '" + op + "'",
          e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException(
          "This argument list for operation '" + op
          + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '"
          + op + "'", e);
    }
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
   * Sets the mpeg7CatalogService
   *
   * @param mpeg7CatalogService
   *            an instance of the mpeg7 catalog service
   */
  protected void setMpeg7CatalogService(
      Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
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
