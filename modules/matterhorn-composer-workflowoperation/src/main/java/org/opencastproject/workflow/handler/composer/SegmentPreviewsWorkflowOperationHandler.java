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
package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * The workflow definition for creating segment preview images from an segment mpeg-7 catalog.
 */
public class SegmentPreviewsWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SegmentPreviewsWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a video source input");
    CONFIG_OPTIONS.put("source-tags",
            "The required tags that must exist on the track for the track to be used as a video source");
    CONFIG_OPTIONS.put("encoding-profile", "The encoding profile to use for generating the image");
    CONFIG_OPTIONS.put("reference-flavor", "The \"flavor\" of the track to used as the reference");
    CONFIG_OPTIONS.put("reference-tags", "The \"tags\" of the track to used as the reference");
    CONFIG_OPTIONS.put("target-flavor", "The flavor to apply to the extracted images");
    CONFIG_OPTIONS.put("target-tags", "The tags to apply to the extracted images");
  }

  /** The composer service */
  private ComposerService composerService = null;

  /** The mpeg7 catalog service */
  private Mpeg7CatalogService mpeg7CatalogService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
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
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running segments preview workflow operation on {}", workflowInstance);

    // Check if there is an mpeg-7 catalog containing video segments
    MediaPackage src = (MediaPackage) workflowInstance.getMediaPackage().clone();
    Catalog[] segmentCatalogs = src.getCatalogs(MediaPackageElements.SEGMENTS);
    if (segmentCatalogs.length == 0) {
      logger.info("Media package {} does not contain segment information", src);
      return createResult(Action.CONTINUE);
    }

    // Create the images
    try {
      return createPreviews(src, workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   *
   * @param mediaPackage
   * @param properties
   * @return the operation result containing the updated mediapackage
   * @throws EncoderException
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws IOException
   * @throws NotFoundException
   * @throws WorkflowOperationException
   */
  private WorkflowOperationResult createPreviews(final MediaPackage mediaPackage, WorkflowOperationInstance operation)
          throws EncoderException, InterruptedException, ExecutionException, NotFoundException, MediaPackageException,
          IOException, WorkflowOperationException {
    long totalTimeInQueue = 0;

    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String sourceTags = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    String targetImageTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetImageFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));
    String referenceFlavor = StringUtils.trimToNull(operation.getConfiguration("reference-flavor"));
    String referenceTags = StringUtils.trimToNull(operation.getConfiguration("reference-tags"));

    // Find the encoding profile
    EncodingProfile profile = composerService.getProfile(encodingProfileName);
    if (profile == null)
      throw new IllegalStateException("Encoding profile '" + encodingProfileName + "' was not found");

    List<String> sourceTagSet = asList(sourceTags);

    // Select the tracks based on the tags and flavors
    Set<Track> videoTrackSet = new HashSet<Track>();
    for (Track track : mediaPackage.getTracksByTags(sourceTagSet)) {
      if (sourceVideoFlavor == null
              || (track.getFlavor() != null && sourceVideoFlavor.equals(track.getFlavor().toString()))) {
        if (!track.hasVideo())
          continue;
        videoTrackSet.add(track);
      }
    }

    if (videoTrackSet.size() == 0) {
      logger.debug("Mediapackage {} has no suitable tracks to extract images based on tags {} and flavor {}",
              new Object[] { mediaPackage, sourceTags, sourceVideoFlavor });
      return createResult(mediaPackage, Action.CONTINUE);
    } else {

      // Determine the tagset for the reference
      List<String> referenceTagSet = asList(referenceTags);

      // Determine the reference master
      for (Track t : videoTrackSet) {

        // Try to load the segments catalog
        MediaPackageReference trackReference = new MediaPackageReferenceImpl(t);
        Catalog[] segmentCatalogs = mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS, trackReference);
        Mpeg7Catalog mpeg7 = null;
        if (segmentCatalogs.length > 0) {
          mpeg7 = loadMpeg7Catalog(segmentCatalogs[0]);
          if (segmentCatalogs.length > 1)
            logger.warn("More than one segments catalog found for track {}. Resuming with the first one ({})", t, mpeg7);
        } else {
          logger.debug("No segments catalog found for track {}", t);
          continue;
        }

        // Check the catalog's consistency
        if (mpeg7.videoContent() == null || mpeg7.videoContent().next() == null) {
          logger.info("Segments catalog {} contains no video content", mpeg7);
          continue;
        }

        Video videoContent = mpeg7.videoContent().next();
        TemporalDecomposition<? extends Segment> decomposition = videoContent.getTemporalDecomposition();

        // Are there any segments?
        if (decomposition == null || !decomposition.hasSegments()) {
          logger.info("Segments catalog {} contains no video content", mpeg7);
          continue;
        }

        // Is a derived track with the configured reference flavor available?
        MediaPackageElement referenceMaster = getReferenceMaster(mediaPackage, t, referenceFlavor, referenceTagSet);

        // Create the preview images according to the mpeg7 segments
        if (t.hasVideo() && mpeg7 != null) {

          Iterator<? extends Segment> segmentIterator = decomposition.segments();

          List<MediaTimePoint> timePointList = new LinkedList<MediaTimePoint>();
          while (segmentIterator.hasNext()) {
            Segment segment = segmentIterator.next();
            MediaTimePoint tp = segment.getMediaTime().getMediaTimePoint();
            timePointList.add(tp);
          }

          // convert to time array
          double[] timeArray = new double[timePointList.size()];
          for (int i = 0; i < timePointList.size(); i++)
            timeArray[i] = (double) timePointList.get(i).getTimeInMilliseconds() / 1000;

          Job job = composerService.image(t, profile.getIdentifier(), timeArray);
          if (!waitForStatus(job).isSuccess()) {
            throw new WorkflowOperationException("Extracting preview image from " + t + " failed");
          }

          // Get the latest copy
          try {
            job = serviceRegistry.getJob(job.getId());
          } catch (ServiceRegistryException e) {
            throw new WorkflowOperationException(e);
          }

          // add this receipt's queue time to the total
          totalTimeInQueue += job.getQueueTime();

          List<? extends MediaPackageElement> composedImages = MediaPackageElementParser.getArrayFromXml(job
                  .getPayload());
          Iterator<MediaTimePoint> it = timePointList.iterator();

          for (MediaPackageElement element : composedImages) {
            Attachment composedImage = (Attachment) element;
            if (composedImage == null)
              throw new IllegalStateException("Unable to compose image");

            // Add the flavor, either from the operation configuration or from the composer
            if (targetImageFlavor != null) {
              composedImage.setFlavor(MediaPackageElementFlavor.parseFlavor(targetImageFlavor));
              logger.debug("Preview image has flavor '{}'", composedImage.getFlavor());
            }

            // Set the mimetype
            if (profile.getMimeType() != null)
              composedImage.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

            // Add tags
            for (String tag : asList(targetImageTags)) {
              logger.trace("Tagging image with '{}'", tag);
              composedImage.addTag(tag);
            }

            // Refer to the original track including a timestamp
            MediaPackageReferenceImpl ref = new MediaPackageReferenceImpl(referenceMaster);
            ref.setProperty("time", it.next().toString());
            composedImage.setReference(ref);

            // store new image in the mediaPackage
            mediaPackage.add(composedImage);
            String fileName = getFileNameFromElements(t, composedImage);
            composedImage.setURI(workspace.moveTo(composedImage.getURI(), mediaPackage.getIdentifier().toString(),
                    composedImage.getIdentifier(), fileName));
          }
        }
      }
    }

    return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
  }

  /**
   * Returns the track that is used as the reference for the segment previews. It is either identified by flavor and tag
   * set and being derived from <code>t</code> or <code>t</code> itself.
   *
   * @param mediaPackage
   *          the media package
   * @param t
   *          the source track for the images
   * @param referenceFlavor
   *          the required flavor
   * @param referenceTagSet
   *          the required tagset
   * @return the reference master
   */
  private MediaPackageElement getReferenceMaster(MediaPackage mediaPackage, Track t, String referenceFlavor,
          Collection<String> referenceTagSet) {
    MediaPackageElement referenceMaster = t;
    if (referenceFlavor != null) {
      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(referenceFlavor);
      // Find a track with the given flavor that is (indirectly) derived from t?
      locateReferenceMaster: for (Track e : mediaPackage.getTracks(flavor)) {
        MediaPackageReference ref = e.getReference();
        while (ref != null) {
          MediaPackageElement tr = mediaPackage.getElementByReference(ref);
          if (tr == null)
            break locateReferenceMaster;
          if (tr.equals(t)) {
            boolean matches = true;
            for (String tag : referenceTagSet) {
              if (!e.containsTag(tag))
                matches = false;
            }
            if (matches) {
              referenceMaster = e;
              break locateReferenceMaster;
            }
          }
          ref = tr.getReference();
        }
      }
    }
    return referenceMaster;
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

}
