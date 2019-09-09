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

package org.opencastproject.workflow.handler.textanalyzer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.MediaDuration;
import org.opencastproject.metadata.mpeg7.MediaRelTimePointImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocator;
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocatorImpl;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.textanalyzer.api.TextAnalyzerException;
import org.opencastproject.textanalyzer.api.TextAnalyzerService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

/**
 * The <code>TextAnalysisOperationHandler</code> will take an <code>MPEG-7</code> catalog, look for video segments and
 * run a text analysis on the associated still images. The resulting <code>VideoText</code> elements will then be added
 * to the segments.
 */
public class TextAnalysisWorkflowOperationHandler extends AbstractWorkflowOperationHandler implements ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TextAnalysisWorkflowOperationHandler.class);

  /** Name of the encoding profile that extracts a still image from a movie */
  public static final String IMAGE_EXTRACTION_PROFILE = "text-analysis.http";

  /** The threshold for scene stability, in seconds */
  private static final int DEFAULT_STABILITY_THRESHOLD = 5;

  /** Name of the constant used to retreive the stability threshold */
  public static final String OPT_STABILITY_THRESHOLD = "stabilitythreshold";

  /** The stability threshold */
  private int stabilityThreshold = DEFAULT_STABILITY_THRESHOLD;

  /** The local workspace */
  private Workspace workspace = null;

  /** The mpeg7 catalog service */
  private Mpeg7CatalogService mpeg7CatalogService = null;

  /** The text analysis service */
  private TextAnalyzerService analysisService = null;

  /** The composer service */
  protected ComposerService composer = null;

  /**
   * Callback for the OSGi declarative services configuration that will set the text analysis service.
   *
   * @param analysisService
   *          the text analysis service
   */
  protected void setTextAnalyzer(TextAnalyzerService analysisService) {
    this.analysisService = analysisService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param catalogService
   *          the catalog service
   */
  protected void setMpeg7CatalogService(Mpeg7CatalogService catalogService) {
    this.mpeg7CatalogService = catalogService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running segments preview workflow operation on {}", workflowInstance);

    // Check if there is an mpeg-7 catalog containing video segments
    MediaPackage src = (MediaPackage) workflowInstance.getMediaPackage().clone();
    Catalog[] segmentCatalogs = src.getCatalogs(MediaPackageElements.SEGMENTS);
    if (segmentCatalogs.length == 0) {
      logger.info("Media package {} does not contain segment information", src);
      return createResult(Action.CONTINUE);
    }

    try {
      return extractVideoText(src, workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Runs the text analysis service on each of the video segments found.
   *
   * @param mediaPackage
   *          the original mediapackage
   * @param operation
   *          the workflow operation
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws NotFoundException
   * @throws WorkflowOperationException
   */
  protected WorkflowOperationResult extractVideoText(final MediaPackage mediaPackage,
          WorkflowOperationInstance operation) throws EncoderException, InterruptedException, ExecutionException,
          IOException, NotFoundException, MediaPackageException, TextAnalyzerException, WorkflowOperationException,
          ServiceRegistryException {
    long totalTimeInQueue = 0;

    List<String> sourceTagSet = asList(operation.getConfiguration("source-tags"));
    List<String> targetTagSet = asList(operation.getConfiguration("target-tags"));

    // Select the catalogs according to the tags
    Map<Catalog, Mpeg7Catalog> catalogs = loadSegmentCatalogs(mediaPackage, operation);

    // Was there at least one matching catalog
    if (catalogs.size() == 0) {
      logger.debug("Mediapackage {} has no suitable mpeg-7 catalogs based on tags {} to to run text analysis",
              mediaPackage, sourceTagSet);
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Loop over all existing segment catalogs
    for (Entry<Catalog, Mpeg7Catalog> mapEntry : catalogs.entrySet()) {
      Map<VideoSegment, Job> jobs = new HashMap<VideoSegment, Job>();
      List<Attachment> images = new LinkedList<Attachment>();
      Catalog segmentCatalog = mapEntry.getKey();
      try {
        MediaPackageReference catalogRef = segmentCatalog.getReference();

        // Make sure we can figure out the source track
        if (catalogRef == null) {
          logger.info("Skipping catalog {} since we can't determine the source track", segmentCatalog);
        } else if (mediaPackage.getElementByReference(catalogRef) == null) {
          logger.info("Skipping catalog {} since we can't determine the source track", segmentCatalog);
        } else if (!(mediaPackage.getElementByReference(catalogRef) instanceof Track)) {
          logger.info("Skipping catalog {} since it's source was not a track", segmentCatalog);
        }

        logger.info("Analyzing mpeg-7 segments catalog {} for text", segmentCatalog);

        // Create a copy that will contain the segments enriched with the video text elements
        Mpeg7Catalog textCatalog = mapEntry.getValue().clone();
        Track sourceTrack = mediaPackage.getTrack(catalogRef.getIdentifier());

        // Load the temporal decomposition (segments)
        Video videoContent = textCatalog.videoContent().next();
        TemporalDecomposition<? extends Segment> decomposition = videoContent.getTemporalDecomposition();
        Iterator<? extends Segment> segmentIterator = decomposition.segments();

        // For every segment, try to find the still image and run text analysis on it
        List<VideoSegment> videoSegments = new LinkedList<VideoSegment>();
        while (segmentIterator.hasNext()) {
          Segment segment = segmentIterator.next();
          if ((segment instanceof VideoSegment))
            videoSegments.add((VideoSegment) segment);
        }

        // argument array for image extraction
        double[] times = new double[videoSegments.size()];

        for (int i = 0; i < videoSegments.size(); i++) {
          VideoSegment videoSegment = videoSegments.get(i);
          MediaTimePoint segmentTimePoint = videoSegment.getMediaTime().getMediaTimePoint();
          MediaDuration segmentDuration = videoSegment.getMediaTime().getMediaDuration();

          // Choose a time
          MediaPackageReference reference = null;
          if (catalogRef == null)
            reference = new MediaPackageReferenceImpl();
          else
            reference = new MediaPackageReferenceImpl(catalogRef.getType(), catalogRef.getIdentifier());
          reference.setProperty("time", segmentTimePoint.toString());

          // Have the time for ocr image created. To circumvent problems with slowly building slides, we take the image
          // that is
          // almost at the end of the segment, it should contain the most content and is stable as well.
          long startTimeSeconds = segmentTimePoint.getTimeInMilliseconds() / 1000;
          long durationSeconds = segmentDuration.getDurationInMilliseconds() / 1000;
          times[i] = Math.max(startTimeSeconds + durationSeconds - stabilityThreshold + 1, 0);
        }

        // Have the ocr image(s) created.

        Job imageJob = composer.image(sourceTrack, IMAGE_EXTRACTION_PROFILE, times);
        if (!waitForStatus(imageJob).isSuccess())
          throw new WorkflowOperationException("Extracting scene images from " + sourceTrack + " failed");
        if (imageJob.getPayload() == null)
          throw new WorkflowOperationException(
                  "The payload of extracting images job from " + sourceTrack + " was null");

        totalTimeInQueue += imageJob.getQueueTime();
        for (MediaPackageElement imageMpe : MediaPackageElementParser.getArrayFromXml(imageJob.getPayload())) {
          Attachment image = (Attachment) imageMpe;
          images.add(image);
        }
        if (images.isEmpty() || images.size() != times.length)
          throw new WorkflowOperationException(
                  "There are no images produced for " + sourceTrack
                          + " or the images count isn't equal the count of the video segments.");

        // Run text extraction on each of the images
        Iterator<VideoSegment> it = videoSegments.iterator();
        for (MediaPackageElement element : images) {
          Attachment image = (Attachment) element;
          VideoSegment videoSegment = it.next();
          jobs.put(videoSegment, analysisService.extract(image));
        }

        // Wait for all jobs to be finished
        if (!waitForStatus(jobs.values().toArray(new Job[jobs.size()])).isSuccess()) {
          throw new WorkflowOperationException("Text extraction failed on images from " + sourceTrack);
        }

        // Process the text extraction results
        for (Map.Entry<VideoSegment, Job> entry : jobs.entrySet()) {
          Job job = serviceRegistry.getJob(entry.getValue().getId());
          totalTimeInQueue += job.getQueueTime();

          VideoSegment videoSegment = entry.getKey();
          MediaDuration segmentDuration = videoSegment.getMediaTime().getMediaDuration();
          Catalog catalog = (Catalog) MediaPackageElementParser.getFromXml(job.getPayload());
          if (catalog == null) {
            logger.warn("Text analysis did not return a valid mpeg7 for segment {}", videoSegment);
            continue;
          }
          Mpeg7Catalog videoTextCatalog = loadMpeg7Catalog(catalog);
          if (videoTextCatalog == null)
            throw new IllegalStateException("Text analysis service did not return a valid mpeg7");

          // Add the spatiotemporal decompositions from the new catalog to the existing video segments
          Iterator<Video> videoTextContents = videoTextCatalog.videoContent();
          if (videoTextContents == null || !videoTextContents.hasNext()) {
            logger.debug("Text analysis was not able to extract any text from {}", job.getArguments().get(0));
            break;
          }

          try {
            Video textVideoContent = videoTextContents.next();
            VideoSegment textVideoSegment = (VideoSegment) textVideoContent.getTemporalDecomposition().segments()
                    .next();
            VideoText[] videoTexts = textVideoSegment.getSpatioTemporalDecomposition().getVideoText();
            SpatioTemporalDecomposition std = videoSegment.createSpatioTemporalDecomposition(true, false);
            for (VideoText videoText : videoTexts) {
              MediaTime mediaTime = new MediaTimeImpl(new MediaRelTimePointImpl(0), segmentDuration);
              SpatioTemporalLocator locator = new SpatioTemporalLocatorImpl(mediaTime);
              videoText.setSpatioTemporalLocator(locator);
              std.addVideoText(videoText);
            }
          } catch (Exception e) {
            logger.warn("The mpeg-7 structure returned by the text analyzer is not what is expected", e);
            continue;
          }
        }

        // Put the catalog into the workspace and add it to the media package
        MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
        Catalog catalog = (Catalog) builder.newElement(MediaPackageElement.Type.Catalog, MediaPackageElements.TEXTS);
        catalog.setIdentifier(null);
        catalog.setReference(segmentCatalog.getReference());
        mediaPackage.add(catalog); // the catalog now has an ID, so we can store the file properly
        InputStream in = mpeg7CatalogService.serialize(textCatalog);
        String filename = "slidetext.xml";
        URI workspaceURI = workspace
                .put(mediaPackage.getIdentifier().toString(), catalog.getIdentifier(), filename, in);
        catalog.setURI(workspaceURI);

        // Since we've enriched and stored the mpeg7 catalog, remove the original
        try {
          mediaPackage.remove(segmentCatalog);
          workspace.delete(segmentCatalog.getURI());
        } catch (Exception e) {
          logger.warn("Unable to delete segment catalog {}: {}", segmentCatalog.getURI(), e);
        }

        // Add flavor and target tags
        catalog.setFlavor(MediaPackageElements.TEXTS);
        for (String tag : targetTagSet) {
          catalog.addTag(tag);
        }
      } finally {
        // Remove images that were created for text extraction
        logger.debug("Removing temporary images");
        for (Attachment image : images) {
          try {
            workspace.delete(image.getURI());
          } catch (Exception e) {
            logger.warn("Unable to delete temporary image {}: {}", image.getURI(), e);
          }
        }
        // Remove the temporary text
        for (Job j : jobs.values()) {
          Catalog catalog = null;
          try {
            Job job = serviceRegistry.getJob(j.getId());
            if (!Job.Status.FINISHED.equals(job.getStatus()))
              continue;
            catalog = (Catalog) MediaPackageElementParser.getFromXml(job.getPayload());
            if (catalog != null)
              workspace.delete(catalog.getURI());
          } catch (Exception e) {
            if (catalog != null) {
              logger.warn("Unable to delete temporary text file {}: {}", catalog.getURI(), e);
            } else {
              logger.warn("Unable to parse textextraction payload of job {}", j.getId());
            }
          }
        }
      }
    }

    logger.debug("Text analysis completed");
    return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
  }

  /**
   * Loads an mpeg7 catalog from a mediapackage's catalog reference
   *
   * @param catalog
   *          the mediapackage's reference to this catalog
   * @return the mpeg7
   * @throws IOException
   *           if there is a problem loading or parsing the mpeg7 object
   */
  protected Mpeg7Catalog loadMpeg7Catalog(Catalog catalog) throws IOException {
    InputStream in = null;
    try {
      File f = workspace.get(catalog.getURI());
      in = new FileInputStream(f);
      return mpeg7CatalogService.load(in);
    } catch (NotFoundException e) {
      throw new IOException("Unable to open catalog " + catalog + ": " + e.getMessage());
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Extracts the catalogs from the media package that match the requirements of flavor and tags specified in the
   * operation handler.
   *
   * @param mediaPackage
   *          the media package
   * @param operation
   *          the workflow operation
   * @return a map of catalog elements and their mpeg-7 representations
   * @throws IOException
   *           if there is a problem reading the mpeg7
   */
  protected Map<Catalog, Mpeg7Catalog> loadSegmentCatalogs(MediaPackage mediaPackage,
          WorkflowOperationInstance operation) throws IOException {
    HashMap<Catalog, Mpeg7Catalog> catalogs = new HashMap<Catalog, Mpeg7Catalog>();

    String sourceFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    List<String> sourceTagSet = asList(operation.getConfiguration("source-tags"));

    Catalog[] catalogsWithTags = mediaPackage.getCatalogsByTags(sourceTagSet);

    for (Catalog mediaPackageCatalog : catalogsWithTags) {
      if (!MediaPackageElements.SEGMENTS.equals(mediaPackageCatalog.getFlavor())) {
        continue;
      }
      if (sourceFlavor != null) {
        if (mediaPackageCatalog.getReference() == null)
          continue;
        Track t = mediaPackage.getTrack(mediaPackageCatalog.getReference().getIdentifier());
        if (t == null || !t.getFlavor().matches(MediaPackageElementFlavor.parseFlavor(sourceFlavor)))
          continue;
      }

      // Make sure the catalog features at least one of the required tags
      if (!mediaPackageCatalog.containsTag(sourceTagSet))
        continue;

      Mpeg7Catalog mpeg7 = loadMpeg7Catalog(mediaPackageCatalog);

      // Make sure there is video content
      if (mpeg7.videoContent() == null || !mpeg7.videoContent().hasNext()) {
        logger.debug("Mpeg-7 segments catalog {} does not contain any video content", mpeg7);
        continue;
      }

      // Make sure there is a temporal decomposition
      Video videoContent = mpeg7.videoContent().next();
      TemporalDecomposition<? extends Segment> decomposition = videoContent.getTemporalDecomposition();
      if (decomposition == null || !decomposition.hasSegments()) {
        logger.debug("Mpeg-7 catalog {} does not contain a temporal decomposition", mpeg7);
        continue;
      }
      catalogs.put(mediaPackageCatalog, mpeg7);
    }

    return catalogs;
  }

  /**
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties != null && properties.get(OPT_STABILITY_THRESHOLD) != null) {
      String threshold = StringUtils.trimToNull((String)properties.get(OPT_STABILITY_THRESHOLD));
      try {
        stabilityThreshold = Integer.parseInt(threshold);
        logger.info("The videosegmenter's stability threshold has been set to {} frames", stabilityThreshold);
      } catch (Exception e) {
        stabilityThreshold = DEFAULT_STABILITY_THRESHOLD;
        logger.warn("Found illegal value '{}' for the videosegmenter stability threshold. Falling back to default value of {} frames", threshold, DEFAULT_STABILITY_THRESHOLD);
      }
    } else {
      stabilityThreshold = DEFAULT_STABILITY_THRESHOLD;
      logger.info("Using the default value of {} frames for the videosegmenter stability threshold", DEFAULT_STABILITY_THRESHOLD);
    }
  }

  /**
   * Sets the composer service.
   *
   * @param composerService
   */
  void setComposerService(ComposerService composerService) {
    this.composer = composerService;
  }

}
