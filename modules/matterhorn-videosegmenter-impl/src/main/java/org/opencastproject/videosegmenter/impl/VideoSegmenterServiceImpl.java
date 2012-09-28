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

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
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
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.videosegmenter.api.VideoSegmenterException;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;
import org.opencastproject.videosegmenter.impl.jmf.FrameGrabber;
import org.opencastproject.videosegmenter.impl.jmf.ImageComparator;
import org.opencastproject.videosegmenter.impl.jmf.ImageUtils;
import org.opencastproject.videosegmenter.impl.jmf.PlayerListener;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.media.Buffer;
import javax.media.Controller;
import javax.media.Duration;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.NoDataSourceException;
import javax.media.Processor;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

/**
 * Media analysis plugin that takes a video stream and extracts video segments by trying to detect slide and/or scene
 * changes.
 * 
 * Videos that can be used by this segmenter need to be created using this commandline:
 * 
 * <pre>
 * ffmpeg -i &lt;inputfile&gt; -deinterlace -r 1 -vcodec mjpeg -qscale 1 -an &lt;outputfile&gt;
 * </pre>
 */
public class VideoSegmenterServiceImpl extends AbstractJobProducer implements VideoSegmenterService, ManagedService {

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "videosegments";

  /** List of available operations on jobs */
  private enum Operation {
    Segment
  };

  /** Constant used to retreive the frame positioning control */
  public static final String FRAME_POSITIONING = "javax.media.control.FramePositioningControl";

  /** Constant used to retreive the frame grabbing control */
  public static final String FRAME_GRABBING = "javax.media.control.FrameGrabbingControl";

  /** Name of the encoding profile that transcodes video tracks into a segmentable format */
  public static final String MJPEG_ENCODING_PROFILE = "video-segmentation.http";

  /** Name of the constant used to retreive the stability threshold */
  public static final String OPT_STABILITY_THRESHOLD = "stabilitythreshold";

  /** The number of seconds that need to resemble until a scene is considered "stable" */
  public static final int DEFAULT_STABILITY_THRESHOLD = 5;

  /** Name of the constant used to retreive the changes threshold */
  public static final String OPT_CHANGES_THRESHOLD = "changesthreshold";

  /** Default value for the number of pixels that may change between two frames without considering them different */
  public static final float DEFAULT_CHANGES_THRESHOLD = 0.05f; // 5% change

  /** The expected mimetype of the resulting preview encoding */
  public static final MimeType MJPEG_MIMETYPE = MimeTypes.MJPEG;

  /** The logging facility */
  protected static final Logger logger = LoggerFactory.getLogger(VideoSegmenterServiceImpl.class);

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

  /** The composer service */
  protected ComposerService composer = null;

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
  public Job segment(Track track) throws VideoSegmenterException, MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Segment.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(track)));
    } catch (ServiceRegistryException e) {
      throw new VideoSegmenterException("Unable to create a job", e);
    }
  }

  /**
   * Starts segmentation on the video track identified by <code>mediapackageId</code> and <code>elementId</code> and
   * returns a receipt containing the final result in the form of anMpeg7Catalog.
   * 
   * @param track
   *          the element to analyze
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws VideoSegmenterException
   */
  protected Catalog segment(Job job, Track track) throws VideoSegmenterException, MediaPackageException {

    // Make sure the element can be analyzed using this analysis implementation
    if (!track.hasVideo()) {
      logger.warn("Element {} is not a video track", track);
      throw new VideoSegmenterException("Element is not a video track");
    }

    try {
      PlayerListener processorListener = null;
      Track mjpegTrack = null;
      Mpeg7Catalog mpeg7 = mpeg7CatalogService.newInstance();

      logger.info("Encoding {} to {}", track, MJPEG_MIMETYPE);
      try {
        mjpegTrack = prepare(track);
      } catch (EncoderException encoderException) {
        throw new VideoSegmenterException("Error creating a mjpeg", encoderException);
      }

      // Create a player
      File mediaFile = null;
      URL mediaUrl = null;
      try {
        mediaFile = workspace.get(mjpegTrack.getURI());
        mediaUrl = mediaFile.toURI().toURL();
      } catch (NotFoundException e) {
        throw new VideoSegmenterException("Error finding the mjpeg in the workspace", e);
      } catch (IOException e) {
        throw new VideoSegmenterException("Error reading the mjpeg in the workspace", e);
      }

      DataSource ds;
      try {
        ds = Manager.createDataSource(mediaUrl);
      } catch (NoDataSourceException e) {
        throw new VideoSegmenterException("Error obtaining a JMF datasource", e);
      } catch (IOException e) {
        throw new VideoSegmenterException("Problem creating a JMF datasource", e);
      }
      Processor processor = null;
      try {
        processor = Manager.createProcessor(ds);
        processorListener = new PlayerListener(processor);
        processor.addControllerListener(processorListener);
      } catch (Exception e) {
        throw new VideoSegmenterException(e);
      }

      // Configure the processor
      processor.configure();
      if (!processorListener.waitForState(Processor.Configured)) {
        throw new VideoSegmenterException("Unable to configure processor");
      }

      // Set the processor to RAW content
      processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));

      // Realize the processor
      processor.realize();
      if (!processorListener.waitForState(Processor.Realized)) {
        throw new VideoSegmenterException("Unable to realize the processor");
      }

      // Get the output DataSource from the processor and
      // hook it up to the DataSourceHandler.
      DataSource outputDataSource = processor.getDataOutput();
      FrameGrabber dsh = new FrameGrabber();

      try {
        dsh.setSource(outputDataSource);
      } catch (IncompatibleSourceException e) {
        throw new VideoSegmenterException("Cannot handle the output data source from the processor: "
                + outputDataSource);
      }

      // Load the movie and change the processor to prefetched state
      processor.prefetch();
      if (!processorListener.waitForState(Controller.Prefetched)) {
        throw new VideoSegmenterException("Unable to switch player into 'prefetch' state");
      }

      // Get the movie duration
      Time duration = processor.getDuration();
      if (duration == Duration.DURATION_UNKNOWN) {
        throw new VideoSegmenterException("Java media framework is unable to detect movie duration");
      }

      if (track.getDuration() == null)
        throw new MediaPackageException("Track " + track + " does not have a duration");
      long durationInSeconds = Math.min(track.getDuration() / 1000, (long) duration.getSeconds());
      logger.info("Track {} loaded, duration is {} s", mediaUrl, duration.getSeconds());

      MediaTime contentTime = new MediaRelTimeImpl(0, (long) durationInSeconds * 1000);
      MediaLocator contentLocator = new MediaLocatorImpl(mjpegTrack.getURI());
      Video videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator);

      logger.info("Starting video segmentation of {}", mediaUrl);

      processor.setRate(1.0f);
      processor.start();
      dsh.start();
      List<Segment> segments;
      try {
        segments = segment(videoContent, dsh);
      } catch (IOException e) {
        throw new VideoSegmenterException("Unable to access a frame in the mjpeg", e);
      }

      logger.info("Segmentation of {} yields {} segments", mediaUrl, segments.size());

      Catalog mpeg7Catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .newElement(Catalog.TYPE, MediaPackageElements.SEGMENTS);
      URI uri;
      try {
        uri = workspace.putInCollection(COLLECTION_ID, job.getId() + ".xml", mpeg7CatalogService.serialize(mpeg7));
      } catch (IOException e) {
        throw new VideoSegmenterException("Unable to put the mpeg7 catalog into the workspace", e);
      }
      mpeg7Catalog.setURI(uri);

      try {
        ds.disconnect();
        workspace.delete(mjpegTrack.getURI());
      } catch (NotFoundException e) {
        throw new VideoSegmenterException("Unable to find the mjpeg in the workspace", e);
      } catch (IOException e) {
        throw new VideoSegmenterException("Unable to delete the mjpeg from the workspace", e);
      }

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
          Track track = (Track) MediaPackageElementParser.getFromXml(arguments.get(0));
          Catalog catalog = segment(job, track);
          return MediaPackageElementParser.getAsXml(catalog);
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
   * Returns the segments for the movie accessible through the frame grabbing control.
   * 
   * @param video
   *          the mpeg-7 video representation
   * @param dsh
   *          the data source handler
   * @return the list of segments
   * @throws IOException
   *           if accessing a frame fails
   * @throws VideoSegmenterException
   *           if segmentation of the video fails
   */
  protected List<Segment> segment(Video video, FrameGrabber dsh) throws IOException, VideoSegmenterException {
    List<Segment> segments = new ArrayList<Segment>();

    int t = 1;
    int lastStableImageTime = 0;
    long startOfSegment = 0;
    int currentSceneStabilityCount = 1;
    boolean sceneChangeImminent = true;
    boolean luckyPunchRecovery = false;
    int segmentCount = 1;
    BufferedImage previousImage = null;
    BufferedImage lastStableImage = null;
    BlockingQueue<Buffer> bufferQueue = new ArrayBlockingQueue<Buffer>(stabilityThreshold + 1);
    long durationInSeconds = video.getMediaTime().getMediaDuration().getDurationInMilliseconds() / 1000;
    Segment contentSegment = video.getTemporalDecomposition().createSegment("segment-" + segmentCount);
    ImageComparator icomp = new ImageComparator(changesThreshold);

    // icomp.setStatistics(true);
    // String imagesPath = PathSupport.concat(new String[] {
    // System.getProperty("java.io.tmpdir"),
    // "videosegments",
    // video.getMediaLocator().getMediaURI().toString().replaceAll("\\W", "-")
    // });
    // icomp.saveImagesTo(new File(imagesPath));

    Buffer buf = dsh.getBuffer();
    while (t < durationInSeconds && buf != null && !buf.isEOM()) {
      BufferedImage bufferedImage = ImageUtils.createImage(buf);
      if (bufferedImage == null)
        throw new VideoSegmenterException("Unable to extract image at time " + t);

      logger.trace("Analyzing video at {} s", t);

      // Compare the new image with our previous sample
      boolean differsFromPreviousImage = icomp.isDifferent(previousImage, bufferedImage, t);

      // We found an image that is different compared to the previous one. Let's see if this image remains stable
      // for some time (STABILITY_THRESHOLD) so we can declare a new scene
      if (differsFromPreviousImage) {
        logger.debug("Found differing image at {} seconds", t);

        // If this is the result of a lucky punch (looking ahead STABILITY_THRESHOLD seconds), then we should
        // really start over an make sure we get the correct beginning of the new scene
        if (!sceneChangeImminent && t - lastStableImageTime > 1) {
          luckyPunchRecovery = true;
          previousImage = lastStableImage;
          bufferQueue.add(buf);
          t = lastStableImageTime;
        } else {
          lastStableImageTime = t - 1;
          lastStableImage = previousImage;
          previousImage = bufferedImage;
          currentSceneStabilityCount = 1;
          t++;
        }
        sceneChangeImminent = true;
      }

      // We are looking ahead and everyhting seems to be fine.
      else if (!sceneChangeImminent) {
        fillLookAheadBuffer(bufferQueue, buf, dsh);
        lastStableImageTime = t;
        t += stabilityThreshold;
        previousImage = bufferedImage;
        lastStableImage = bufferedImage;
      }

      // Seems to be the same image. If we have just recently detected a new scene, let's see if we are able to
      // confirm that this is scene is stable (>= STABILITY_THRESHOLD)
      else if (currentSceneStabilityCount < stabilityThreshold) {
        currentSceneStabilityCount++;
        previousImage = bufferedImage;
        t++;
      }

      // Did we find a new scene?
      else if (currentSceneStabilityCount == stabilityThreshold) {
        lastStableImageTime = t;

        long endOfSegment = t - stabilityThreshold - 1;
        long durationms = (endOfSegment - startOfSegment) * 1000L;

        // Create a new segment if this wasn't the first one
        if (endOfSegment > stabilityThreshold) {
          contentSegment.setMediaTime(new MediaRelTimeImpl(startOfSegment * 1000L, durationms));
          contentSegment = video.getTemporalDecomposition().createSegment("segment-" + ++segmentCount);
          segments.add(contentSegment);
          startOfSegment = endOfSegment;
        }

        // After finding a new segment, likelihood of a stable image is good, let's take a look ahead. Since
        // a processor can't seek, we need to store the buffers in between, in case we need to come back.
        fillLookAheadBuffer(bufferQueue, buf, dsh);
        t += stabilityThreshold;
        previousImage = bufferedImage;
        lastStableImage = bufferedImage;
        currentSceneStabilityCount++;
        sceneChangeImminent = false;
        logger.info("Found new scene at {} s", startOfSegment);
      }

      // Did we find a new scene by looking ahead?
      else if (sceneChangeImminent) {
        // We found a scene change by looking ahead. Now we want to get to the exact position
        lastStableImageTime = t;
        previousImage = bufferedImage;
        lastStableImage = bufferedImage;
        currentSceneStabilityCount++;
        t++;
      }

      // Nothing special, business as usual
      else {
        // If things look stable, then let's look ahead as much as possible without loosing information (which is
        // equal to looking ahead STABILITY_THRESHOLD seconds.
        lastStableImageTime = t;
        fillLookAheadBuffer(bufferQueue, buf, dsh);
        t += stabilityThreshold;
        lastStableImage = bufferedImage;
        previousImage = bufferedImage;
      }

      if (luckyPunchRecovery) {
        buf = bufferQueue.poll();
        luckyPunchRecovery = !bufferQueue.isEmpty();
      } else
        buf = dsh.getBuffer();
    }

    // Finish off the last segment
    long startOfSegmentms = startOfSegment * 1000L;
    long durationms = ((long) durationInSeconds - startOfSegment) * 1000;
    contentSegment.setMediaTime(new MediaRelTimeImpl(startOfSegmentms, durationms));
    segments.add(contentSegment);

    // Print summary
    if (icomp.hasStatistics()) {
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMaximumFractionDigits(2);
      logger.info("Image comparison finished with an average change of {}% in {} comparisons",
              nf.format(icomp.getAvgChange()), icomp.getComparisons());
    }

    // Cleanup
    if (icomp.getSavedImagesDirectory() != null) {
      FileUtils.deleteQuietly(icomp.getSavedImagesDirectory());
    }

    return segments;
  }

  /**
   * Fills the look ahead buffer with the next <code>STABILITY_THRESHOLD</code> images.
   * 
   * @param queue
   *          the buffer
   * @param currentBuffer
   *          the current buffer
   * @param dsh
   *          the data source handler
   * @throws IOException
   *           if reading from the data source fails
   */
  private void fillLookAheadBuffer(BlockingQueue<Buffer> queue, Buffer currentBuffer, FrameGrabber dsh)
          throws IOException {
    queue.clear();
    queue.add(currentBuffer);
    for (int i = 0; i < stabilityThreshold - 1; i++) {
      Buffer b = dsh.getBuffer();
      if (b != null && !b.isEOM())
        queue.add(b);
      else
        return;
    }
  }

  /**
   * Makes sure that there is version of the track that is compatible with this service implementation. Currently, with
   * the usage of the <code>JMF 2.1.1e</code> framework, the list is rather limited, see Sun's <a
   * href="http://java.sun.com/javase/technologies/desktop/media/jmf/2.1.1/formats.html">supported formats in JMF
   * 2.1.1</a>.
   * 
   * @param track
   *          the track identifier
   * @return the encoded track
   * @throws EncoderException
   *           if encoding fails
   * @throws IllegalStateException
   *           if the track is not connected to a media package and is not in the correct format
   */
  protected Track prepare(Track track) throws EncoderException, MediaPackageException {
    if (MJPEG_MIMETYPE.equals(track.getMimeType()))
      return track;

    MediaPackageReference original = new MediaPackageReferenceImpl(track);

    // See if encoding has already taken place
    if (track.getMediaPackage() != null) {
      List<Track> derivedTracks = new ArrayList<Track>();
      derivedTracks.add(track);
      derivedTracks.addAll(Arrays.asList(track.getMediaPackage().getTracks(original)));
      for (Track t : derivedTracks) {
        if (MJPEG_MIMETYPE.equals(t.getMimeType())) {
          logger.info("Using existing mjpeg track {}", t);
          return t;
        }
      }
    }

    // Looks like we need to do the work ourselves
    logger.info("Requesting {} version of track {}", MJPEG_MIMETYPE, track);
    final Job receipt = composer.encode(track, MJPEG_ENCODING_PROFILE);
    JobBarrier barrier = new JobBarrier(serviceRegistry, receipt);
    if (!barrier.waitForJobs().isSuccess()) {
      throw new EncoderException("Unable to create motion jpeg version of " + track);
    }

    Track composedTrack = (Track) MediaPackageElementParser.getFromXml(receipt.getPayload());
    composedTrack.setReference(original);
    composedTrack.setMimeType(MJPEG_MIMETYPE);
    composedTrack.addTag("segmentation");

    return composedTrack;
  }

  /**
   * Sets the composer service.
   * 
   * @param composerService
   */
  protected void setComposerService(ComposerService composerService) {
    this.composer = composerService;
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
   * Sets the mpeg7CatalogService
   * 
   * @param mpeg7CatalogService
   *          an instance of the mpeg7 catalog service
   */
  protected void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
  }

  /**
   * Sets the receipt service
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
