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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.LaidOutElement;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.composer.layout.HorizontalCoverageLayoutSpec;
import org.opencastproject.composer.layout.LayoutManager;
import org.opencastproject.composer.layout.MultiShapeLayout;
import org.opencastproject.composer.layout.Serializer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.AttachmentSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * The workflow definition for handling "composite" operations
 */
public class CompositeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String COLLECTION = "composite";

  private static final String SOURCE_TAGS_UPPER = "source-tags-upper";
  private static final String SOURCE_FLAVOR_UPPER = "source-flavor-upper";
  private static final String SOURCE_TAGS_LOWER = "source-tags-lower";
  private static final String SOURCE_FLAVOR_LOWER = "source-flavor-lower";
  private static final String SOURCE_TAGS_WATERMARK = "source-tags-watermark";
  private static final String SOURCE_FLAVOR_WATERMARK = "source-flavor-watermark";
  private static final String SOURCE_URL_WATERMARK = "source-url-watermark";

  private static final String TARGET_TAGS = "target-tags";
  private static final String TARGET_FLAVOR = "target-flavor";
  private static final String ENCODING_PROFILE = "encoding-profile";

  private static final String LAYOUT = "layout";
  private static final String LAYOUT_PREFIX = "layout-";

  private static final String OUTPUT_RESOLUTION = "output-resolution";
  private static final String OUTPUT_BACKGROUND = "output-background";
  private static final String DEFAULT_BG_COLOR = "black";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CompositeWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_TAGS_UPPER, "The \"tag\" of the upper track to use as a source input");
    CONFIG_OPTIONS.put(SOURCE_FLAVOR_UPPER, "The \"flavor\" of the upper track to use as a source input");
    CONFIG_OPTIONS.put(SOURCE_TAGS_LOWER, "The \"tag\" of the lower track to use as a source input");
    CONFIG_OPTIONS.put(SOURCE_FLAVOR_LOWER, "The \"flavor\" of the lower track to use as a source input");
    CONFIG_OPTIONS.put(SOURCE_TAGS_WATERMARK, "The \"tag\" of the attachement image to use as a source input");
    CONFIG_OPTIONS.put(SOURCE_FLAVOR_WATERMARK, "The \"flavor\" of the attachement image to use as a source input");
    CONFIG_OPTIONS.put(SOURCE_URL_WATERMARK, "The \"URL\" of the fallback image to use as a source input");

    CONFIG_OPTIONS.put(ENCODING_PROFILE, "The encoding profile to use");

    CONFIG_OPTIONS.put(TARGET_TAGS, "The tags to apply to the compound video track");
    CONFIG_OPTIONS.put(TARGET_FLAVOR, "The flavor to apply to the compound video track");

    CONFIG_OPTIONS
            .put(LAYOUT,
                    "The layout name to use or a semi-colon separated JSON layout definition (lower, upper, optional watermark)");
    CONFIG_OPTIONS.put(LAYOUT_PREFIX,
            "Define semi-colon separated JSON layouts (lower, upper, optional watermark) to provide by name");

    CONFIG_OPTIONS.put(OUTPUT_RESOLUTION, "The resulting resolution of the compound video e.g. 1900x1080");
    CONFIG_OPTIONS.put(OUTPUT_BACKGROUND, "The resulting background color of the compound video e.g. black");
  }

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the local composer service
   */
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running composite workflow operation on workflow {}", workflowInstance.getId());

    try {
      return composite(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  private WorkflowOperationResult composite(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    // Check which tags have been configured
    String sourceTagsUpper = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_UPPER));
    String sourceFlavorUpper = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_UPPER));
    String sourceTagsLower = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_LOWER));
    String sourceFlavorLower = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_LOWER));
    String sourceTagsWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_WATERMARK));
    String sourceFlavorWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_WATERMARK));
    String sourceUrlWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_URL_WATERMARK));

    String targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS));
    String targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR));
    String encodingProfile = StringUtils.trimToNull(operation.getConfiguration(ENCODING_PROFILE));

    String layoutString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT));

    String outputResolution = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_RESOLUTION));
    String outputBackground = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_BACKGROUND));

    AbstractMediaPackageElementSelector<Track> upperTrackSelector = new TrackSelector();
    AbstractMediaPackageElementSelector<Track> lowerTrackSelector = new TrackSelector();
    AbstractMediaPackageElementSelector<Attachment> watermarkSelector = new AttachmentSelector();

    String watermarkIdentifier = UUID.randomUUID().toString();

    try {
      if (outputBackground == null)
        outputBackground = DEFAULT_BG_COLOR;

      if (layoutString == null)
        throw new WorkflowOperationException("Layout must be set!");

      if (!layoutString.contains(";")) {
        layoutString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT_PREFIX + layoutString));
        if (layoutString == null)
          throw new WorkflowOperationException("Layout " + layoutString + " not defined!");
      }

      List<HorizontalCoverageLayoutSpec> layouts = new ArrayList<HorizontalCoverageLayoutSpec>();
      try {
        for (String l : StringUtils.split(layoutString, ";")) {
          layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj.jsonObj(l)));
        }
      } catch (Exception e) {
        throw new WorkflowOperationException("Unable to parse layout!", e);
      }

      if (layouts.size() != 3)
        throw new WorkflowOperationException(
                "Layout only doesn't contain the required three layouts for (lower, upper, watermark)");

      // Make sure either one of tags or flavor for the upper source are provided
      if (sourceTagsUpper == null && sourceFlavorUpper == null) {
        logger.warn("No source tags or flavor for the upper video have been specified, not matching anything");
        return createResult(mediaPackage, Action.SKIP);
      }

      // Make sure either one of tags or flavor for the lower source are provided
      if (sourceTagsLower == null && sourceFlavorLower == null) {
        logger.warn("No source tags or flavor for the lower video have been specified, not matching anything");
        return createResult(mediaPackage, Action.SKIP);
      }

      // Find the encoding profile
      if (encodingProfile == null)
        throw new WorkflowOperationException("Encoding profile must be set!");

      EncodingProfile profile = composerService.getProfile(encodingProfile);
      if (profile == null)
        throw new WorkflowOperationException("Encoding profile '" + encodingProfile + "' was not found");

      // Target tags
      List<String> targetTags = asList(targetTagsOption);

      // Target flavor
      if (targetFlavorOption == null)
        throw new WorkflowOperationException("Target flavor must be set!");

      // Output resolution
      if (outputResolution == null)
        throw new WorkflowOperationException("Output resolution must be set!");

      Dimension outputDimension;
      try {
        String[] outputResolutionArray = StringUtils.split(outputResolution, "x");
        if (outputResolutionArray.length != 2)
          throw new WorkflowOperationException("Invalid format of output resolution!");
        outputDimension = Dimension.dimension(Integer.parseInt(outputResolutionArray[0]),
                Integer.parseInt(outputResolutionArray[1]));
      } catch (WorkflowOperationException e) {
        throw e;
      } catch (Exception e) {
        throw new WorkflowOperationException("Unable to parse output resolution!", e);
      }

      MediaPackageElementFlavor targetFlavor = null;
      try {
        targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption);
        if ("*".equals(targetFlavor.getType()) || "*".equals(targetFlavor.getSubtype()))
          throw new WorkflowOperationException("Target flavor must have a type and a subtype, '*' are not allowed!");
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Target flavor '" + targetFlavorOption + "' is malformed");
      }

      // Support legacy "source-flavor-upper" option
      if (sourceFlavorUpper != null) {
        try {
          upperTrackSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(sourceFlavorUpper));
        } catch (IllegalArgumentException e) {
          throw new WorkflowOperationException("Source upper flavor '" + sourceFlavorUpper + "' is malformed");
        }
      }

      // Support legacy "source-flavor-lower" option
      if (sourceFlavorLower != null) {
        try {
          lowerTrackSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(sourceFlavorLower));
        } catch (IllegalArgumentException e) {
          throw new WorkflowOperationException("Source lower flavor '" + sourceFlavorLower + "' is malformed");
        }
      }

      // Support legacy "source-flavor-watermark" option
      if (sourceFlavorWatermark != null) {
        try {
          watermarkSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(sourceFlavorWatermark));
        } catch (IllegalArgumentException e) {
          throw new WorkflowOperationException("Source watermark flavor '" + sourceFlavorWatermark + "' is malformed");
        }
      }

      // Select the source tags upper
      for (String tag : asList(sourceTagsUpper)) {
        upperTrackSelector.addTag(tag);
      }

      // Select the source tags lower
      for (String tag : asList(sourceTagsLower)) {
        lowerTrackSelector.addTag(tag);
      }

      // Select the watermark source tags
      for (String tag : asList(sourceTagsWatermark)) {
        watermarkSelector.addTag(tag);
      }

      // Look for upper elements matching the tags and flavor
      Collection<Track> upperElements = upperTrackSelector.select(mediaPackage, false);
      if (upperElements.size() > 1) {
        logger.warn("More than one upper track has been found for compositing, skipping compositing!: {}",
                upperElements);
        return createResult(mediaPackage, Action.SKIP);
      } else if (upperElements.size() == 0) {
        logger.warn("No upper track has been found for compositing, skipping compositing!");
        return createResult(mediaPackage, Action.SKIP);
      }

      Track upperTrack = null;
      for (Track t : upperElements)
        upperTrack = t;

      // Look for lower elements matching the tags and flavor
      Collection<Track> lowerElements = lowerTrackSelector.select(mediaPackage, false);
      if (lowerElements.size() > 1) {
        logger.warn("More than one lower track has been found for compositing, skipping compositing!: {}",
                lowerElements);
        return createResult(mediaPackage, Action.SKIP);
      } else if (lowerElements.size() == 0) {
        logger.warn("No lower track has been found for compositing, skipping compositing!");
        return createResult(mediaPackage, Action.SKIP);
      }

      Track lowerTrack = null;
      for (Track t : lowerElements)
        lowerTrack = t;

      Option<Attachment> watermarkAttachment = Option.<Attachment> none();
      Collection<Attachment> watermarkElements = watermarkSelector.select(mediaPackage, false);
      if (watermarkElements.size() > 1) {
        logger.warn("More than one watermark attachment has been found for compositing, skipping compositing!: {}",
                watermarkElements);
        return createResult(mediaPackage, Action.SKIP);
      } else if (watermarkElements.size() == 0 && sourceUrlWatermark != null) {
        logger.info("No watermark found from flavor and tags, take watermark from URL {}", sourceUrlWatermark);
        Attachment urlAttachment = new AttachmentImpl();
        urlAttachment.setIdentifier(watermarkIdentifier);

        if (sourceUrlWatermark.startsWith("http")) {
          urlAttachment.setURI(UrlSupport.uri(sourceUrlWatermark));
        } else {
          InputStream in = null;
          try {
            in = UrlSupport.url(sourceUrlWatermark).openStream();
            URI imageUrl = workspace.putInCollection(COLLECTION,
                    watermarkIdentifier + "." + FilenameUtils.getExtension(sourceUrlWatermark), in);
            urlAttachment.setURI(imageUrl);
          } catch (Exception e) {
            logger.warn("Unable to read watermark source url {}: {}", sourceUrlWatermark, e);
            throw new WorkflowOperationException("Unable to read watermark source url " + sourceUrlWatermark, e);
          } finally {
            IOUtils.closeQuietly(in);
          }
        }
        watermarkAttachment = Option.option(urlAttachment);
      } else if (watermarkElements.size() == 0 && sourceUrlWatermark == null) {
        logger.info("No watermark to composite");
      } else {
        for (Attachment a : watermarkElements)
          watermarkAttachment = Option.option(a);
      }

      VideoStream[] upperVideoStreams = TrackSupport.byType(upperTrack.getStreams(), VideoStream.class);
      if (upperVideoStreams.length == 0) {
        logger.warn("No video stream available in the upper track! {}", upperTrack);
        return createResult(mediaPackage, Action.SKIP);
      }

      VideoStream[] lowerVideoStreams = TrackSupport.byType(lowerTrack.getStreams(), VideoStream.class);
      if (lowerVideoStreams.length == 0) {
        logger.warn("No video stream available in the lower track! {}", lowerTrack);
        return createResult(mediaPackage, Action.SKIP);
      }

      // Read the video dimensions from the mediapackage stream information
      Dimension upperDimensions = Dimension.dimension(upperVideoStreams[0].getFrameWidth(),
              upperVideoStreams[0].getFrameHeight());
      Dimension lowerDimensions = Dimension.dimension(lowerVideoStreams[0].getFrameWidth(),
              lowerVideoStreams[0].getFrameHeight());

      // Create the video layout definitions
      List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>();
      shapes.add(0, Tuple.tuple(lowerDimensions, layouts.get(0)));
      shapes.add(1, Tuple.tuple(upperDimensions, layouts.get(1)));

      // Optionally add the watermark layout definitions
      if (watermarkAttachment.isSome()) {
        BufferedImage image;
        try {
          File watermarkFile = workspace.get(watermarkAttachment.get().getURI());
          image = ImageIO.read(watermarkFile);
        } catch (Exception e) {
          logger.warn("Unable to read the watermark image attachment {}: {}", watermarkAttachment.get().getURI(), e);
          throw new WorkflowOperationException("Unable to read the watermark image attachment", e);
        }
        Dimension imageDimension = Dimension.dimension(image.getWidth(), image.getHeight());
        shapes.add(2, Tuple.tuple(imageDimension, layouts.get(2)));
      }

      // Calculate the layout
      MultiShapeLayout multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes);

      // Create the laied out element for the videos
      LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(lowerTrack, multiShapeLayout.getShapes()
              .get(0));
      LaidOutElement<Track> upperLaidOutElement = new LaidOutElement<Track>(upperTrack, multiShapeLayout.getShapes()
              .get(1));

      // Create the optionally laied out element for the watermark
      Option<LaidOutElement<Attachment>> watermarkOption = Option.<LaidOutElement<Attachment>> none();
      if (watermarkAttachment.isSome() && multiShapeLayout.getShapes().size() == 3) {
        watermarkOption = Option.some(new LaidOutElement<Attachment>(watermarkAttachment.get(), multiShapeLayout
                .getShapes().get(2)));
      }

      Job compositeJob = composerService.composite(outputDimension, upperLaidOutElement, lowerLaidOutElement,
              watermarkOption, profile.getIdentifier(), outputBackground);

      // Wait for the jobs to return
      if (!waitForStatus(compositeJob).isSuccess())
        throw new WorkflowOperationException("The composite job did not complete successfully");

      if (compositeJob.getPayload().length() > 0) {

        Track compoundTrack = (Track) MediaPackageElementParser.getFromXml(compositeJob.getPayload());

        compoundTrack.setURI(workspace.moveTo(compoundTrack.getURI(), mediaPackage.getIdentifier().toString(),
                compoundTrack.getIdentifier(),
                "composite." + FilenameUtils.getExtension(compoundTrack.getURI().toString())));

        // Adjust the target tags
        for (String tag : targetTags) {
          logger.trace("Tagging compound track with '{}'", tag);
          compoundTrack.addTag(tag);
        }

        // Adjust the target flavor.
        compoundTrack.setFlavor(targetFlavor);
        logger.debug("Compound track has flavor '{}'", compoundTrack.getFlavor());

        // store new tracks to mediaPackage
        mediaPackage.add(compoundTrack);
        WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, compositeJob.getQueueTime());
        logger.debug("Composite operation completed");
        return result;
      } else {
        logger.info("Composite operation unsuccessful, no payload returned: {}", compositeJob);
        return createResult(mediaPackage, Action.SKIP);
      }
    } finally {
      if (sourceUrlWatermark != null)
        workspace.deleteFromCollection(COLLECTION,
                watermarkIdentifier + "." + FilenameUtils.getExtension(sourceUrlWatermark));
    }
  }
}
