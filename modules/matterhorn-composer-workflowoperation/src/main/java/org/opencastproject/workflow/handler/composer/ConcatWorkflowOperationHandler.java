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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "concat" operations
 */
public class ConcatWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String SOURCE_TAGS_PREFIX = "source-tags-part-";
  private static final String SOURCE_FLAVOR_PREFIX = "source-flavor-part-";
  private static final String MANDATORY_SUFFIX = "-mandatory";

  private static final String TARGET_TAGS = "target-tags";
  private static final String TARGET_FLAVOR = "target-flavor";

  private static final String ENCODING_PROFILE = "encoding-profile";
  private static final String OUTPUT_RESOLUTION = "output-resolution";
  private static final String OUTPUT_RESOLUTION_PART_PREFIX = "part-";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ConcatWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_TAGS_PREFIX,
            "The prefix of the iterative \"tags\" used to specify the order of the source input tracks");
    CONFIG_OPTIONS.put(SOURCE_FLAVOR_PREFIX,
            "The prefix of the iterative \"flavors\" used to specify the order of the source input tracks");

    CONFIG_OPTIONS.put(TARGET_TAGS, "The tags to apply to the compound video track");
    CONFIG_OPTIONS.put(TARGET_FLAVOR, "The flavor to apply to the compound video track");
    CONFIG_OPTIONS
            .put(OUTPUT_RESOLUTION,
                    "The resulting resolution of the concat video e.g. 1900x1080 or the part name to take as the output resolution");
    CONFIG_OPTIONS.put(ENCODING_PROFILE, "The encoding profile to use");
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
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running concat workflow operation on workflow {}", workflowInstance.getId());

    try {
      return concat(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  private WorkflowOperationResult concat(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    Map<Integer, Tuple<TrackSelector, Boolean>> trackSelectors = getTrackSelectors(operation);
    String outputResolution = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_RESOLUTION));
    String encodingProfile = StringUtils.trimToNull(operation.getConfiguration(ENCODING_PROFILE));

    // Skip the worklow if no source-flavors or tags has been configured
    if (trackSelectors.isEmpty()) {
      logger.warn("No source-tags or source-flavors has been set.");
      return createResult(mediaPackage, Action.SKIP);
    }

    String targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS));
    String targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR));

    // Target tags
    List<String> targetTags = asList(targetTagsOption);

    // Target flavor
    if (targetFlavorOption == null)
      throw new WorkflowOperationException("Target flavor must be set!");

    // Find the encoding profile
    if (encodingProfile == null)
      throw new WorkflowOperationException("Encoding profile must be set!");

    EncodingProfile profile = composerService.getProfile(encodingProfile);
    if (profile == null)
      throw new WorkflowOperationException("Encoding profile '" + encodingProfile + "' was not found");

    // Output resolution
    if (outputResolution == null)
      throw new WorkflowOperationException("Output resolution must be set!");

    Dimension outputDimension = null;
    if (outputResolution.startsWith(OUTPUT_RESOLUTION_PART_PREFIX)) {
      if (!trackSelectors.keySet().contains(
              Integer.parseInt(outputResolution.substring(OUTPUT_RESOLUTION_PART_PREFIX.length()))))
        throw new WorkflowOperationException("Output resolution part not set!");
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

    MediaPackageElementFlavor targetFlavor = null;
    try {
      targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption);
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

      if (tracksForSelector.size() > 1) {
        logger.warn(
                "More than one track has been found with flavor '{}' and/or tag '{}' for concat operation, skipping concatenation!",
                currentFlavor, currentTag);
        return createResult(mediaPackage, Action.SKIP);
      } else if (tracksForSelector.size() == 0 && trackSelector.getValue().getB()) {
        logger.warn(
                "No track has been found with flavor '{}' and/or tag '{}' for concat operation, skipping concatenation!",
                currentFlavor, currentTag);
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
        if (outputResolution.startsWith(OUTPUT_RESOLUTION_PART_PREFIX)
                && trackSelector.getKey() == Integer.parseInt(outputResolution.substring(OUTPUT_RESOLUTION_PART_PREFIX
                        .length()))) {
          outputDimension = new Dimension(videoStreams[0].getFrameWidth(), videoStreams[0].getFrameHeight());
          if (!trackSelector.getValue().getB()) {
            logger.warn("Output resolution track {} must be mandatory, skipping concatenation!", outputResolution);
            return createResult(mediaPackage, Action.SKIP);
          }
        }
      }
    }

    if (tracks.size() == 0) {
      logger.warn("No tracks found for concating operation, skipping concatenation!");
      return createResult(mediaPackage, Action.SKIP);
    } else if (tracks.size() == 1) {
      Track track = (Track) tracks.get(0).clone();
      track.setIdentifier(null);
      addNewTrack(mediaPackage, track, targetTags, targetFlavor);
      logger.info("At least two tracks are needed for the concating operation, skipping concatenation!");
      return createResult(mediaPackage, Action.SKIP);
    }

    Job concatJob = composerService.concat(profile.getIdentifier(), outputDimension,
            tracks.toArray(new Track[tracks.size()]));

    // Wait for the jobs to return
    if (!waitForStatus(concatJob).isSuccess())
      throw new WorkflowOperationException("The concat job did not complete successfully");

    if (concatJob.getPayload().length() > 0) {

      Track concatTrack = (Track) MediaPackageElementParser.getFromXml(concatJob.getPayload());

      concatTrack.setURI(workspace.moveTo(concatTrack.getURI(), mediaPackage.getIdentifier().toString(),
              concatTrack.getIdentifier(), "concat." + FilenameUtils.getExtension(concatTrack.getURI().toString())));

      addNewTrack(mediaPackage, concatTrack, targetTags, targetFlavor);

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
