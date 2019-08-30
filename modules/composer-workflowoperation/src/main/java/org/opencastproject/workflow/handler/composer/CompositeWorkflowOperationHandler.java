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

import static org.opencastproject.util.data.Collections.list;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.LaidOutElement;
import org.opencastproject.composer.layout.AbsolutePositionLayoutSpec;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/**
 * The workflow definition for handling "composite" operations
 */
public class CompositeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String COLLECTION = "composite";

  private static final String SOURCE_AUDIO_NAME = "source-audio-name";
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
  private static final String LAYOUT_MULTIPLE = "layout-multiple";
  private static final String LAYOUT_SINGLE = "layout-single";
  private static final String LAYOUT_PREFIX = "layout-";

  private static final String OUTPUT_RESOLUTION = "output-resolution";
  private static final String OUTPUT_BACKGROUND = "output-background";
  private static final String DEFAULT_BG_COLOR = "black";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CompositeWorkflowOperationHandler.class);

  /** The legal options for SOURCE_AUDIO_NAME */
  private static final Pattern sourceAudioOption = Pattern.compile(
          ComposerService.LOWER + "|" + ComposerService.UPPER + "|" + ComposerService.BOTH, Pattern.CASE_INSENSITIVE);

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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
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
    CompositeSettings compositeSettings;
    try {
      compositeSettings = new CompositeSettings(mediaPackage, operation);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to parse composite settings because {}", ExceptionUtils.getStackTrace(e));
      return createResult(mediaPackage, Action.SKIP);
    }
    Option<Attachment> watermarkAttachment = Option.<Attachment> none();
    Collection<Attachment> watermarkElements = compositeSettings.getWatermarkSelector().select(mediaPackage, false);
    if (watermarkElements.size() > 1) {
      logger.warn("More than one watermark attachment has been found for compositing, skipping compositing!: {}",
              watermarkElements);
      return createResult(mediaPackage, Action.SKIP);
    } else if (watermarkElements.size() == 0 && compositeSettings.getSourceUrlWatermark() != null) {
      logger.info("No watermark found from flavor and tags, take watermark from URL {}",
              compositeSettings.getSourceUrlWatermark());
      Attachment urlAttachment = new AttachmentImpl();
      urlAttachment.setIdentifier(compositeSettings.getWatermarkIdentifier());

      if (compositeSettings.getSourceUrlWatermark().startsWith("http")) {
        urlAttachment.setURI(UrlSupport.uri(compositeSettings.getSourceUrlWatermark()));
      } else {
        InputStream in = null;
        try {
          in = UrlSupport.url(compositeSettings.getSourceUrlWatermark()).openStream();
          URI imageUrl = workspace.putInCollection(COLLECTION, compositeSettings.getWatermarkIdentifier() + "."
                  + FilenameUtils.getExtension(compositeSettings.getSourceUrlWatermark()), in);
          urlAttachment.setURI(imageUrl);
        } catch (Exception e) {
          logger.warn("Unable to read watermark source url {}: {}", compositeSettings.getSourceUrlWatermark(), e);
          throw new WorkflowOperationException("Unable to read watermark source url "
                  + compositeSettings.getSourceUrlWatermark(), e);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }
      watermarkAttachment = Option.option(urlAttachment);
    } else if (watermarkElements.size() == 0 && compositeSettings.getSourceUrlWatermark() == null) {
      logger.info("No watermark to composite");
    } else {
      for (Attachment a : watermarkElements)
        watermarkAttachment = Option.option(a);
    }

    Collection<Track> upperElements = compositeSettings.getUpperTrackSelector().select(mediaPackage, false);
    Collection<Track> lowerElements = compositeSettings.getLowerTrackSelector().select(mediaPackage, false);

    // There is only a single track to work with.
    if ((upperElements.size() == 1 && lowerElements.size() == 0)
            || (upperElements.size() == 0 && lowerElements.size() == 1)) {
      for (Track t : upperElements)
        compositeSettings.setSingleTrack(t);
      for (Track t : lowerElements)
        compositeSettings.setSingleTrack(t);
      return handleSingleTrack(mediaPackage, operation, compositeSettings, watermarkAttachment);
    } else {
      // Look for upper elements matching the tags and flavor
      if (upperElements.size() > 1) {
        logger.warn("More than one upper track has been found for compositing, skipping compositing!: {}",
                upperElements);
        return createResult(mediaPackage, Action.SKIP);
      } else if (upperElements.size() == 0) {
        logger.warn("No upper track has been found for compositing, skipping compositing!");
        return createResult(mediaPackage, Action.SKIP);
      }

      for (Track t : upperElements) {
        compositeSettings.setUpperTrack(t);
      }

      // Look for lower elements matching the tags and flavor
      if (lowerElements.size() > 1) {
        logger.warn("More than one lower track has been found for compositing, skipping compositing!: {}",
                lowerElements);
        return createResult(mediaPackage, Action.SKIP);
      } else if (lowerElements.size() == 0) {
        logger.warn("No lower track has been found for compositing, skipping compositing!");
        return createResult(mediaPackage, Action.SKIP);
      }

      for (Track t : lowerElements) {
        compositeSettings.setLowerTrack(t);
      }

      return handleMultipleTracks(mediaPackage, operation, compositeSettings, watermarkAttachment);
    }
  }

  /**
   * This class collects and calculates all of the relevant data for doing a composite whether there is a single or two
   * video tracks.
   */
  private class CompositeSettings {

    /** Use a fixed output resolution */
    public static final String OUTPUT_RESOLUTION_FIXED = "fixed";

    /** Use resolution of lower part as output resolution */
    public static final String OUTPUT_RESOLUTION_LOWER =  "lower";

    /** Use resolution of upper part as output resolution */
    public static final String OUTPUT_RESOLUTION_UPPER = "upper";

    private String sourceAudioName;
    private String sourceTagsUpper;
    private String sourceFlavorUpper;
    private String sourceTagsLower;
    private String sourceFlavorLower;
    private String sourceTagsWatermark;
    private String sourceFlavorWatermark;
    private String sourceUrlWatermark;
    private String targetTagsOption;
    private String targetFlavorOption;
    private String encodingProfile;
    private String layoutMultipleString;
    private String layoutSingleString;
    private String outputResolution;
    private String outputBackground;

    private AbstractMediaPackageElementSelector<Track> upperTrackSelector = new TrackSelector();
    private AbstractMediaPackageElementSelector<Track> lowerTrackSelector = new TrackSelector();
    private AbstractMediaPackageElementSelector<Attachment> watermarkSelector = new AttachmentSelector();

    private String watermarkIdentifier;
    private Option<AbsolutePositionLayoutSpec> watermarkLayout = Option.none();

    private List<HorizontalCoverageLayoutSpec> multiSourceLayouts = new ArrayList<HorizontalCoverageLayoutSpec>();
    private HorizontalCoverageLayoutSpec singleSourceLayout;

    private Track upperTrack;
    private Track lowerTrack;
    private Track singleTrack;

    private String outputResolutionSource;
    private Dimension outputDimension;

    private EncodingProfile profile;

    private List<String> targetTags;

    private MediaPackageElementFlavor targetFlavor = null;

    CompositeSettings(MediaPackage mediaPackage, WorkflowOperationInstance operation)
            throws WorkflowOperationException {

      sourceAudioName = StringUtils.trimToNull(operation.getConfiguration(SOURCE_AUDIO_NAME));
      if (sourceAudioName == null) {
        sourceAudioName = ComposerService.BOTH; // default
      } else if (!sourceAudioOption.matcher(sourceAudioName).matches()) {
        throw new WorkflowOperationException("sourceAudioName if used, must be either upper, lower or both!");
      }

      sourceTagsUpper = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_UPPER));
      sourceFlavorUpper = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_UPPER));
      sourceTagsLower = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_LOWER));
      sourceFlavorLower = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_LOWER));
      sourceTagsWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_WATERMARK));
      sourceFlavorWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_WATERMARK));
      sourceUrlWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_URL_WATERMARK));

      targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS));
      targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR));
      encodingProfile = StringUtils.trimToNull(operation.getConfiguration(ENCODING_PROFILE));

      layoutMultipleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT_MULTIPLE));
      if (layoutMultipleString == null) {
        layoutMultipleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT));
      }

      if (layoutMultipleString != null && !layoutMultipleString.contains(";")) {
        layoutMultipleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT_PREFIX + layoutMultipleString));
      }

      layoutSingleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT_SINGLE));

      outputResolution = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_RESOLUTION));
      outputBackground = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_BACKGROUND));

      watermarkIdentifier = UUID.randomUUID().toString();

      if (outputBackground == null) {
        outputBackground = DEFAULT_BG_COLOR;
      }

      if (layoutMultipleString != null) {
        Tuple<List<HorizontalCoverageLayoutSpec>, Option<AbsolutePositionLayoutSpec>> multipleLayouts = parseMultipleLayouts(layoutMultipleString);
        multiSourceLayouts.addAll(multipleLayouts.getA());
        watermarkLayout = multipleLayouts.getB();
      }

      if (layoutSingleString != null) {
        Tuple<HorizontalCoverageLayoutSpec, Option<AbsolutePositionLayoutSpec>> singleLayouts = parseSingleLayouts(layoutSingleString);
        singleSourceLayout = singleLayouts.getA();
        watermarkLayout = singleLayouts.getB();
      }

      // Find the encoding profile
      if (encodingProfile == null)
        throw new WorkflowOperationException("Encoding profile must be set!");

      profile = composerService.getProfile(encodingProfile);
      if (profile == null)
        throw new WorkflowOperationException("Encoding profile '" + encodingProfile + "' was not found");

      // Target tags
      targetTags = asList(targetTagsOption);

      // Target flavor
      if (targetFlavorOption == null)
        throw new WorkflowOperationException("Target flavor must be set!");

      // Output resolution
      if (outputResolution == null)
        throw new WorkflowOperationException("Output resolution must be set!");

      if (outputResolution.equals(OUTPUT_RESOLUTION_LOWER) || outputResolution.equals(OUTPUT_RESOLUTION_UPPER)) {
        outputResolutionSource = outputResolution;
      } else {
        outputResolutionSource = OUTPUT_RESOLUTION_FIXED;
        try {
          String[] outputResolutionArray = StringUtils.split(outputResolution, "x");
          if (outputResolutionArray.length != 2) {
            throw new WorkflowOperationException("Invalid format of output resolution!");
          }
          outputDimension = Dimension.dimension(Integer.parseInt(outputResolutionArray[0]),
                  Integer.parseInt(outputResolutionArray[1]));
        } catch (Exception e) {
          throw new WorkflowOperationException("Unable to parse output resolution!", e);
        }
      }

      // Make sure either one of tags or flavor for the upper source are provided
      if (sourceTagsUpper == null && sourceFlavorUpper == null) {
        throw new IllegalArgumentException(
                "No source tags or flavor for the upper video have been specified, not matching anything");
      }

      // Make sure either one of tags or flavor for the lower source are provided
      if (sourceTagsLower == null && sourceFlavorLower == null) {
        throw new IllegalArgumentException(
                "No source tags or flavor for the lower video have been specified, not matching anything");
      }

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
    }

    private Tuple<List<HorizontalCoverageLayoutSpec>, Option<AbsolutePositionLayoutSpec>> parseMultipleLayouts(
            String layoutString) throws WorkflowOperationException {
      try {
        String[] layouts = StringUtils.split(layoutString, ";");
        if (layouts.length < 2)
          throw new WorkflowOperationException(
                  "Multiple layout doesn't contain the required layouts for (lower, upper, optional watermark)");

        List<HorizontalCoverageLayoutSpec> multipleLayouts = list(
                Serializer.horizontalCoverageLayoutSpec(JsonObj.jsonObj(layouts[0])),
                Serializer.horizontalCoverageLayoutSpec(JsonObj.jsonObj(layouts[1])));

        AbsolutePositionLayoutSpec watermarkLayout = null;
        if (layouts.length > 2)
          watermarkLayout = Serializer.absolutePositionLayoutSpec(JsonObj.jsonObj(layouts[2]));

        return Tuple.tuple(multipleLayouts, Option.option(watermarkLayout));
      } catch (Exception e) {
        throw new WorkflowOperationException("Unable to parse layout!", e);
      }
    }

    private Tuple<HorizontalCoverageLayoutSpec, Option<AbsolutePositionLayoutSpec>> parseSingleLayouts(
            String layoutString) throws WorkflowOperationException {
      try {
        String[] layouts = StringUtils.split(layoutString, ";");
        if (layouts.length < 1)
          throw new WorkflowOperationException(
                  "Single layout doesn't contain the required layouts for (video, optional watermark)");

        HorizontalCoverageLayoutSpec singleLayout = Serializer
                .horizontalCoverageLayoutSpec(JsonObj.jsonObj(layouts[0]));

        AbsolutePositionLayoutSpec watermarkLayout = null;
        if (layouts.length > 1)
          watermarkLayout = Serializer.absolutePositionLayoutSpec(JsonObj.jsonObj(layouts[1]));

        return Tuple.tuple(singleLayout, Option.option(watermarkLayout));
      } catch (Exception e) {
        throw new WorkflowOperationException("Unable to parse layout!", e);
      }
    }

    public String getSourceUrlWatermark() {
      return sourceUrlWatermark;
    }

    public MediaPackageElementFlavor getTargetFlavor() {
      return targetFlavor;
    }

    public List<String> getTargetTags() {
      return targetTags;
    }

    public String getSourceAudioName() {
      return sourceAudioName;
    }

    public String getOutputBackground() {
      return outputBackground;
    }

    public AbstractMediaPackageElementSelector<Track> getUpperTrackSelector() {
      return upperTrackSelector;
    }

    public AbstractMediaPackageElementSelector<Track> getLowerTrackSelector() {
      return lowerTrackSelector;
    }

    public AbstractMediaPackageElementSelector<Attachment> getWatermarkSelector() {
      return watermarkSelector;
    }

    public String getWatermarkIdentifier() {
      return watermarkIdentifier;
    }

    public Option<AbsolutePositionLayoutSpec> getWatermarkLayout() {
      return watermarkLayout;
    }

    public List<HorizontalCoverageLayoutSpec> getMultiSourceLayouts() {
      return multiSourceLayouts;
    }

    public HorizontalCoverageLayoutSpec getSingleSourceLayout() {
      return singleSourceLayout;
    }

    public Track getUpperTrack() {
      return upperTrack;
    }

    public void setUpperTrack(Track upperTrack) {
      this.upperTrack = upperTrack;
    }

    public Track getLowerTrack() {
      return lowerTrack;
    }

    public void setLowerTrack(Track lowerTrack) {
      this.lowerTrack = lowerTrack;
    }

    public Track getSingleTrack() {
      return singleTrack;
    }

    public void setSingleTrack(Track singleTrack) {
      this.singleTrack = singleTrack;
    }

    public String getOutputResolutionSource() {
      return outputResolutionSource;
    }

    public Dimension getOutputDimension() {
      return outputDimension;
    }

    public EncodingProfile getProfile() {
      return profile;
    }
  }

  private WorkflowOperationResult handleSingleTrack(MediaPackage mediaPackage, WorkflowOperationInstance operation,
          CompositeSettings compositeSettings, Option<Attachment> watermarkAttachment) throws EncoderException,
          IOException, NotFoundException, MediaPackageException, WorkflowOperationException {

    if (compositeSettings.getSingleSourceLayout() == null) {
      throw new WorkflowOperationException("Single video layout must be set! Please verify that you have a "
              + LAYOUT_SINGLE + " property in your composite operation in your workflow definition.");
    }

    try {
      VideoStream[] videoStreams = TrackSupport.byType(compositeSettings.getSingleTrack().getStreams(),
              VideoStream.class);
      if (videoStreams.length == 0) {
        logger.warn("No video stream available to compose! {}", compositeSettings.getSingleTrack());
        return createResult(mediaPackage, Action.SKIP);
      }

      // Read the video dimensions from the mediapackage stream information
      Dimension videoDimension = Dimension.dimension(videoStreams[0].getFrameWidth(), videoStreams[0].getFrameHeight());

      // Create the video layout definitions
      List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>();
      shapes.add(0, Tuple.tuple(videoDimension, compositeSettings.getSingleSourceLayout()));

      // Determine dimension of output
      Dimension outputDimension = null;
      String outputResolutionSource = compositeSettings.getOutputResolutionSource();
      if (outputResolutionSource.equals(CompositeSettings.OUTPUT_RESOLUTION_FIXED)) {
        outputDimension = compositeSettings.getOutputDimension();
      } else if (outputResolutionSource.equals(CompositeSettings.OUTPUT_RESOLUTION_LOWER)) {
        outputDimension = videoDimension;
      } else if (outputResolutionSource.equals(CompositeSettings.OUTPUT_RESOLUTION_UPPER)) {
        outputDimension = videoDimension;
      }

      // Calculate the single layout
      MultiShapeLayout multiShapeLayout = LayoutManager
              .multiShapeLayout(outputDimension, shapes);

      // Create the laid out element for the videos
      LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(compositeSettings.getSingleTrack(),
              multiShapeLayout.getShapes().get(0));

      // Create the optionally laid out element for the watermark
      Option<LaidOutElement<Attachment>> watermarkOption = createWatermarkLaidOutElement(compositeSettings,
              outputDimension, watermarkAttachment);

      Job compositeJob = composerService.composite(outputDimension, Option
              .<LaidOutElement<Track>> none(), lowerLaidOutElement, watermarkOption, compositeSettings.getProfile()
              .getIdentifier(), compositeSettings.getOutputBackground(), compositeSettings.getSourceAudioName());

      // Wait for the jobs to return
      if (!waitForStatus(compositeJob).isSuccess())
        throw new WorkflowOperationException("The composite job did not complete successfully");

      if (compositeJob.getPayload().length() > 0) {

        Track compoundTrack = (Track) MediaPackageElementParser.getFromXml(compositeJob.getPayload());

        compoundTrack.setURI(workspace.moveTo(compoundTrack.getURI(), mediaPackage.getIdentifier().toString(),
                compoundTrack.getIdentifier(),
                "composite." + FilenameUtils.getExtension(compoundTrack.getURI().toString())));

        // Adjust the target tags
        for (String tag : compositeSettings.getTargetTags()) {
          logger.trace("Tagging compound track with '{}'", tag);
          compoundTrack.addTag(tag);
        }

        // Adjust the target flavor.
        compoundTrack.setFlavor(compositeSettings.getTargetFlavor());
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
      if (compositeSettings.getSourceUrlWatermark() != null)
        workspace.deleteFromCollection(
                COLLECTION,
                compositeSettings.getWatermarkIdentifier() + "."
                        + FilenameUtils.getExtension(compositeSettings.getSourceUrlWatermark()));
    }
  }

  private Option<LaidOutElement<Attachment>> createWatermarkLaidOutElement(CompositeSettings compositeSettings,
          Dimension outputDimension, Option<Attachment> watermarkAttachment) throws WorkflowOperationException {
    Option<LaidOutElement<Attachment>> watermarkOption = Option.<LaidOutElement<Attachment>> none();
    if (watermarkAttachment.isSome() && compositeSettings.getWatermarkLayout().isSome()) {
      BufferedImage image;
      try {
        File watermarkFile = workspace.get(watermarkAttachment.get().getURI());
        image = ImageIO.read(watermarkFile);
      } catch (Exception e) {
        logger.warn("Unable to read the watermark image attachment {}: {}", watermarkAttachment.get().getURI(), e);
        throw new WorkflowOperationException("Unable to read the watermark image attachment", e);
      }
      Dimension imageDimension = Dimension.dimension(image.getWidth(), image.getHeight());
      List<Tuple<Dimension, AbsolutePositionLayoutSpec>> watermarkShapes = new ArrayList<Tuple<Dimension, AbsolutePositionLayoutSpec>>();
      watermarkShapes.add(0, Tuple.tuple(imageDimension, compositeSettings.getWatermarkLayout().get()));
      MultiShapeLayout watermarkLayout = LayoutManager.absoluteMultiShapeLayout(outputDimension,
              watermarkShapes);
      watermarkOption = Option.some(new LaidOutElement<Attachment>(watermarkAttachment.get(), watermarkLayout
              .getShapes().get(0)));
    }
    return watermarkOption;
  }

  private WorkflowOperationResult handleMultipleTracks(MediaPackage mediaPackage, WorkflowOperationInstance operation,
          CompositeSettings compositeSettings, Option<Attachment> watermarkAttachment) throws EncoderException,
          IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    if (compositeSettings.getMultiSourceLayouts() == null || compositeSettings.getMultiSourceLayouts().size() == 0) {
      throw new WorkflowOperationException(
              "Multi video layout must be set! Please verify that you have a "
                      + LAYOUT_MULTIPLE
                      + " or "
                      + LAYOUT
                      + " property in your composite operation in your workflow definition to be able to handle multiple videos");
    }

    try {
      Track upperTrack = compositeSettings.getUpperTrack();
      Track lowerTrack = compositeSettings.getLowerTrack();
      List<HorizontalCoverageLayoutSpec> layouts = compositeSettings.getMultiSourceLayouts();

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

      // Determine dimension of output
      Dimension outputDimension = null;
      String outputResolutionSource = compositeSettings.getOutputResolutionSource();
      if (outputResolutionSource.equals(CompositeSettings.OUTPUT_RESOLUTION_FIXED)) {
        outputDimension = compositeSettings.getOutputDimension();
      } else if (outputResolutionSource.equals(CompositeSettings.OUTPUT_RESOLUTION_LOWER)) {
        outputDimension = lowerDimensions;
      } else if (outputResolutionSource.equals(CompositeSettings.OUTPUT_RESOLUTION_UPPER)) {
        outputDimension = upperDimensions;
      }

      // Create the video layout definitions
      List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes = new ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>();
      shapes.add(0, Tuple.tuple(lowerDimensions, layouts.get(0)));
      shapes.add(1, Tuple.tuple(upperDimensions, layouts.get(1)));

      // Calculate the layout
      MultiShapeLayout multiShapeLayout = LayoutManager
              .multiShapeLayout(outputDimension, shapes);

      // Create the laid out element for the videos
      LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>(lowerTrack, multiShapeLayout.getShapes()
              .get(0));
      LaidOutElement<Track> upperLaidOutElement = new LaidOutElement<Track>(upperTrack, multiShapeLayout.getShapes()
              .get(1));

      // Create the optionally laid out element for the watermark
      Option<LaidOutElement<Attachment>> watermarkOption = createWatermarkLaidOutElement(compositeSettings,
              outputDimension, watermarkAttachment);

      Job compositeJob = composerService.composite(outputDimension, Option
              .option(upperLaidOutElement), lowerLaidOutElement, watermarkOption, compositeSettings.getProfile()
              .getIdentifier(), compositeSettings.getOutputBackground(), compositeSettings.getSourceAudioName());

      // Wait for the jobs to return
      if (!waitForStatus(compositeJob).isSuccess())
        throw new WorkflowOperationException("The composite job did not complete successfully");

      if (compositeJob.getPayload().length() > 0) {

        Track compoundTrack = (Track) MediaPackageElementParser.getFromXml(compositeJob.getPayload());

        compoundTrack.setURI(workspace.moveTo(compoundTrack.getURI(), mediaPackage.getIdentifier().toString(),
                compoundTrack.getIdentifier(),
                "composite." + FilenameUtils.getExtension(compoundTrack.getURI().toString())));

        // Adjust the target tags
        for (String tag : compositeSettings.getTargetTags()) {
          logger.trace("Tagging compound track with '{}'", tag);
          compoundTrack.addTag(tag);
        }

        // Adjust the target flavor.
        compoundTrack.setFlavor(compositeSettings.getTargetFlavor());
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
      if (compositeSettings.getSourceUrlWatermark() != null)
        workspace.deleteFromCollection(
                COLLECTION,
                compositeSettings.getWatermarkIdentifier() + "."
                        + FilenameUtils.getExtension(compositeSettings.getSourceUrlWatermark()));
    }
  }
}
