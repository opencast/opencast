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
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The workflow definition for handling "concat" operations
 */
public class ConcatWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String SOURCE_TAGS_PREFIX = "source-tags-part-";
  private static final String SOURCE_FLAVOR_PREFIX = "source-flavor-part-";
  private static final String MANDATORY_SUFFIX = "-mandatory";

  private static final String ENCODING_PROFILE = "encoding-profile";
  private static final String OUTPUT_RESOLUTION = "output-resolution";
  private static final String OUTPUT_FRAMERATE = "output-framerate";
  private static final String OUTPUT_PART_PREFIX = "part-";

  /** Concatenate flavored media by lexicographical order -eg v01.mp4, v02.mp4, etc */
  private static final String SOURCE_FLAVOR_NUMBERED_FILES = "source-flavor-numbered-files";
  /**
  * If codec and dimension are the same in all the src files, do not scale and transcode, just put all the content into
  * the container
  */
  private static final String SAME_CODEC = "same-codec";
  enum SourceType {
    None, PrefixedFile, NumberedFile
  };


  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ConcatWorkflowOperationHandler.class);

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
    logger.debug("Running concat workflow operation on workflow {}", workflowInstance.getId());

    try {
      return concat(workflowInstance.getMediaPackage(), workflowInstance);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  private WorkflowOperationResult concat(MediaPackage src, WorkflowInstance workflowInstance)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    Map<Integer, Tuple<TrackSelector, Boolean>> trackSelectors = getTrackSelectors(operation);
    String outputResolution = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_RESOLUTION));
    String outputFrameRate = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_FRAMERATE));
    String encodingProfile = StringUtils.trimToNull(operation.getConfiguration(ENCODING_PROFILE));
    boolean sameCodec = BooleanUtils.toBoolean(operation.getConfiguration(SAME_CODEC));

    // Skip the worklow if no source-flavors or tags has been configured
    if (trackSelectors.isEmpty()) {
      logger.warn("No source-tags or source-flavors has been set.");
      return createResult(mediaPackage, Action.SKIP);
    }

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance, Configuration.none, Configuration.none, Configuration.many, Configuration.one);
    List<String> targetTagsOption = tagsAndFlavors.getTargetTags();
    List<MediaPackageElementFlavor> targetFlavorOption = tagsAndFlavors.getTargetFlavors();

    // Target flavor
    if (targetFlavorOption.isEmpty())
      throw new WorkflowOperationException("Target flavor must be set!");

    // Find the encoding profile
    if (encodingProfile == null)
      throw new WorkflowOperationException("Encoding profile must be set!");

    EncodingProfile profile = composerService.getProfile(encodingProfile);
    if (profile == null)
      throw new WorkflowOperationException("Encoding profile '" + encodingProfile + "' was not found");

    // Output resolution - if not keeping dimensions the same, it must be set
    if (!sameCodec && outputResolution == null)
        throw new WorkflowOperationException("Output resolution must be set!");

    Dimension outputDimension = null;
    if (!sameCodec) { // Ignore resolution if same Codec - no scaling
      if (outputResolution.startsWith(OUTPUT_PART_PREFIX)) {
        if (!trackSelectors.keySet().contains(
            Integer.parseInt(outputResolution.substring(OUTPUT_PART_PREFIX.length())))) {
          throw new WorkflowOperationException("Output resolution part not set!");
        }
      } else {
        try {
          String[] outputResolutionArray = StringUtils.split(outputResolution, "x");
          if (outputResolutionArray.length != 2) {
            throw new WorkflowOperationException("Invalid format of output resolution!");
          }
          outputDimension = Dimension.dimension(Integer.parseInt(outputResolutionArray[0]),
            Integer.parseInt(outputResolutionArray[1]));
        } catch (WorkflowOperationException e) {
          throw e;
        } catch (Exception e) {
          throw new WorkflowOperationException("Unable to parse output resolution!", e);
        }
      }
    }

    float fps = -1.0f;
    // Ignore fps if same Codec - no scaling
    if (!sameCodec && StringUtils.isNotEmpty(outputFrameRate)) {
      if (StringUtils.startsWith(outputFrameRate, OUTPUT_PART_PREFIX)) {
        if (!NumberUtils.isCreatable(outputFrameRate.substring(OUTPUT_PART_PREFIX.length()))
                || !trackSelectors.keySet().contains(Integer.parseInt(
                        outputFrameRate.substring(OUTPUT_PART_PREFIX.length())))) {
          throw new WorkflowOperationException("Output frame rate part not set or invalid!");
        }
      } else if (NumberUtils.isCreatable(outputFrameRate)) {
        fps = NumberUtils.toFloat(outputFrameRate);
      } else {
        throw new WorkflowOperationException("Unable to parse output frame rate!");
      }
    }

    MediaPackageElementFlavor targetFlavor = null;
    try {
      targetFlavor = targetFlavorOption.get(0);
      if ("*".equals(targetFlavor.getType()) || "*".equals(targetFlavor.getSubtype()))
        throw new WorkflowOperationException("Target flavor must have a type and a subtype, '*' are not allowed!");
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException("Target flavor '" + targetFlavorOption + "' is malformed");
    }

    List<Track> tracks = new ArrayList<Track>();
    for (Entry<Integer, Tuple<TrackSelector, Boolean>> trackSelector : trackSelectors.entrySet()) {
      Collection<Track> tracksForSelector = trackSelector.getValue().getA().select(mediaPackage, false);
      String currentFlavor = StringUtils.join(trackSelector.getValue().getA().getFlavors());
      String currentTag = StringUtils.join(trackSelector.getValue().getA().getTags());

      // Cannot mix prefix-number tracks with numbered files
      // PREFIXED_FILES must have multiple files, but numbered file can skip the operation if there is only one
      if (trackSelectors.size() == 1) {
        // NUMBERED FILES will have one trackSelector only and multiple sorted files in it
        List<Track> list = new ArrayList<>(tracksForSelector);
        list.sort((left, right) -> {
            String l = (new File(left.getURI().getPath())).getName(); // Get and compare basename only, getPath() for mock
            String r = (new File(right.getURI().getPath())).getName();
            return (l.compareTo(r));
          });
        tracksForSelector = list;
      } else if (tracksForSelector.size() > 1) {
        logger.warn("More than one track has been found with flavor '{}' and/or tag '{}' for concat operation, "
                        + "skipping concatenation!", currentFlavor, currentTag);
        return createResult(mediaPackage, Action.SKIP);
      } else if (tracksForSelector.size() == 0 && trackSelector.getValue().getB()) {
        logger.warn("No track has been found with flavor '{}' and/or tag '{}' for concat operation, "
                        + "skipping concatenation!", currentFlavor, currentTag);
        return createResult(mediaPackage, Action.SKIP);
      } else if (tracksForSelector.size() == 0 && !trackSelector.getValue().getB()) {
        logger.info("No track has been found with flavor '{}' and/or tag '{}' for concat operation, skipping track!",
                currentFlavor, currentTag);
        continue;
      }

      for (Track t : tracksForSelector) {
        tracks.add(t);
        VideoStream[] videoStreams = TrackSupport.byType(t.getStreams(), VideoStream.class);
        if (videoStreams.length == 0) {
          logger.info("No video stream available in the track with flavor {}! {}", currentFlavor, t);
          return createResult(mediaPackage, Action.SKIP);
        }
        if (StringUtils.startsWith(outputResolution, OUTPUT_PART_PREFIX)
                && NumberUtils.isCreatable(outputResolution.substring(OUTPUT_PART_PREFIX.length()))
                && trackSelector.getKey() == Integer.parseInt(outputResolution.substring(OUTPUT_PART_PREFIX.length()))) {
          outputDimension = new Dimension(videoStreams[0].getFrameWidth(), videoStreams[0].getFrameHeight());
          if (!trackSelector.getValue().getB()) {
            logger.warn("Output resolution track {} must be mandatory, skipping concatenation!", outputResolution);
            return createResult(mediaPackage, Action.SKIP);
          }
        }
        if (fps <= 0 && StringUtils.startsWith(outputFrameRate, OUTPUT_PART_PREFIX)
                && NumberUtils.isCreatable(outputFrameRate.substring(OUTPUT_PART_PREFIX.length()))
                && trackSelector.getKey() == Integer.parseInt(outputFrameRate.substring(OUTPUT_PART_PREFIX.length()))) {
          fps = videoStreams[0].getFrameRate();
        }
      }
    }

    if (tracks.size() == 0) {
      logger.warn("No tracks found for concating operation, skipping concatenation!");
      return createResult(mediaPackage, Action.SKIP);
    } else if (tracks.size() == 1) {
      Track track = (Track) tracks.get(0).clone();
      track.setIdentifier(null);
      addNewTrack(mediaPackage, track, targetTagsOption, targetFlavor);
      logger.info("At least two tracks are needed for the concating operation, skipping concatenation!");
      return createResult(mediaPackage, Action.SKIP);
    }

    Job concatJob;
    if (fps > 0) {
      concatJob = composerService.concat(profile.getIdentifier(), outputDimension,
              fps, sameCodec, tracks.toArray(new Track[tracks.size()]));
    } else {
      concatJob = composerService.concat(profile.getIdentifier(), outputDimension,
              sameCodec,tracks.toArray(new Track[tracks.size()]));
    }

    // Wait for the jobs to return
    if (!waitForStatus(concatJob).isSuccess())
      throw new WorkflowOperationException("The concat job did not complete successfully");

    if (concatJob.getPayload().length() > 0) {

      Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concatJob.getPayload());

      concatTrack.setURI(workspace.moveTo(concatTrack.getURI(), mediaPackage.getIdentifier().toString(),
              concatTrack.getIdentifier(), "concat." + FilenameUtils.getExtension(concatTrack.getURI().toString())));

      addNewTrack(mediaPackage, concatTrack, targetTagsOption, targetFlavor);

      WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, concatJob.getQueueTime());
      logger.debug("Concat operation completed");
      return result;
    } else {
      logger.info("concat operation unsuccessful, no payload returned: {}", concatJob);
      return createResult(mediaPackage, Action.SKIP);
    }
  }

  private void addNewTrack(MediaPackage mediaPackage, Track track, List<String> targetTags,
          MediaPackageElementFlavor targetFlavor) {
    // Adjust the target tags
    for (String tag : targetTags) {
      logger.trace("Tagging compound track with '{}'", tag);
      track.addTag(tag);
    }

    // Adjust the target flavor.
    track.setFlavor(targetFlavor);
    logger.debug("Compound track has flavor '{}'", track.getFlavor());

    mediaPackage.add(track);
  }

  private Map<Integer, Tuple<TrackSelector, Boolean>> getTrackSelectors(WorkflowOperationInstance operation)
          throws WorkflowOperationException {
    Map<Integer, Tuple<TrackSelector, Boolean>> trackSelectors = new HashMap<Integer, Tuple<TrackSelector, Boolean>>();
    SourceType flavorType = SourceType.None;
    String srcFlavor = null;

    // Search config for SOURCE_FLAVOR_NUMBERED_FILES and SOURCE_FLAVOR_PREFIX
    for (String key : operation.getConfigurationKeys()) {
      if (key.startsWith(SOURCE_FLAVOR_PREFIX) || key.startsWith(SOURCE_TAGS_PREFIX)) {
        if (flavorType == SourceType.None) {
          flavorType = SourceType.PrefixedFile;
        } else if (flavorType != SourceType.PrefixedFile) {
          throw new WorkflowOperationException(
                  "Cannot mix source prefix flavor/tags with source numbered files - use one type of selector only");
        }
      }

      if (key.equals(SOURCE_FLAVOR_NUMBERED_FILES)) { // Search config for SOURCE_FLAVORS_NUMBERED_FILES
        srcFlavor = operation.getConfiguration(key);
        if (flavorType == SourceType.None) {
          flavorType = SourceType.NumberedFile;
          srcFlavor = operation.getConfiguration(key);
        } else if (flavorType != SourceType.NumberedFile) {
          throw new WorkflowOperationException(
                  "Cannot mix source prefix flavor/tags with source numbered files - use one type of selector only");
        }
      }
    }

    // if is SOURCE_FLAVOR_NUMBERED_FILES, do not use prefixed (tags or flavor)
    if (srcFlavor != null) { // Numbered files has only one selector
      int number = 0;
      Tuple<TrackSelector, Boolean> selectorTuple = trackSelectors.get(number);
      selectorTuple = Tuple.tuple(new TrackSelector(), true);
      TrackSelector trackSelector = selectorTuple.getA();
      trackSelector.addFlavor(srcFlavor);
      trackSelectors.put(number, selectorTuple);
      return trackSelectors;
    }

    // Prefix only
    for (String key : operation.getConfigurationKeys()) {
      String tags = null;
      String flavor = null;
      Boolean mandatory = true;
      int number = -1;
      if (key.startsWith(SOURCE_TAGS_PREFIX) && !key.endsWith(MANDATORY_SUFFIX)) {
        number = NumberUtils.toInt(key.substring(SOURCE_TAGS_PREFIX.length()), -1);
        tags = operation.getConfiguration(key);
        mandatory = BooleanUtils.toBooleanObject(operation.getConfiguration(SOURCE_TAGS_PREFIX.concat(
                Integer.toString(number)).concat(MANDATORY_SUFFIX)));
      } else if (key.startsWith(SOURCE_FLAVOR_PREFIX) && !key.endsWith(MANDATORY_SUFFIX)) {
        number = NumberUtils.toInt(key.substring(SOURCE_FLAVOR_PREFIX.length()), -1);
        flavor = operation.getConfiguration(key);
        mandatory = BooleanUtils.toBooleanObject(operation.getConfiguration(SOURCE_FLAVOR_PREFIX.concat(
                Integer.toString(number)).concat(MANDATORY_SUFFIX)));
      }

      if (number < 0)
        continue;

      Tuple<TrackSelector, Boolean> selectorTuple = trackSelectors.get(number);
      if (selectorTuple == null) {
        selectorTuple = Tuple.tuple(new TrackSelector(), BooleanUtils.toBooleanDefaultIfNull(mandatory, false));
      } else {
        selectorTuple = Tuple.tuple(selectorTuple.getA(),
                selectorTuple.getB() || BooleanUtils.toBooleanDefaultIfNull(mandatory, false));
      }
      TrackSelector trackSelector = selectorTuple.getA();
      if (StringUtils.isNotBlank(tags)) {
        for (String tag : StringUtils.split(tags, ",")) {
          trackSelector.addTag(tag);
        }
      }
      if (StringUtils.isNotBlank(flavor)) {
        try {
          trackSelector.addFlavor(flavor);
        } catch (IllegalArgumentException e) {
          throw new WorkflowOperationException("Source flavor '" + flavor + "' is malformed");
        }
      }

      trackSelectors.put(number, selectorTuple);
    }
    return trackSelectors;
  }
}
