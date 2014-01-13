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
package org.opencastproject.workflow.handler;

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
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "concat" operations
 */
public class ConcatWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String SOURCE_TAGS_PREFIX = "source-tags-part-";
  private static final String SOURCE_FLAVOR_PREFIX = "source-flavor-part-";

  private static final String TARGET_TAGS = "target-tags";
  private static final String TARGET_FLAVOR = "target-flavor";

  private static final String ENCODING_PROFILE = "encoding-profile";
  private static final String OUTPUT_RESOLUTION = "output-resolution";

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

    List<String> sourceTags = getConfiguredParamsFromIterativeKey(SOURCE_TAGS_PREFIX, operation);
    List<String> sourceFlavors = getConfiguredParamsFromIterativeKey(SOURCE_FLAVOR_PREFIX, operation);
    String outputResolution = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_RESOLUTION));
    String encodingProfile = StringUtils.trimToNull(operation.getConfiguration(ENCODING_PROFILE));

    // Skip the worklow if no source-flavors or tags has been configured
    if (sourceFlavors.isEmpty() && sourceTags.isEmpty()) {
      logger.warn("No source-tags or source-flavors has been set.");
      return createResult(mediaPackage, Action.SKIP);
    }

    String targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS));
    String targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR));

    List<AbstractMediaPackageElementSelector<Track>> trackSelectors = new ArrayList<AbstractMediaPackageElementSelector<Track>>();

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

    MediaPackageElementFlavor targetFlavor = null;
    try {
      targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption);
      if ("*".equals(targetFlavor.getType()) || "*".equals(targetFlavor.getSubtype()))
        throw new WorkflowOperationException("Target flavor must have a type and a subtype, '*' are not allowed!");
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException("Target flavor '" + targetFlavorOption + "' is malformed");
    }

    // Add flavors to the track selector for each source
    int i = 0;
    for (String flavor : sourceFlavors) {
      AbstractMediaPackageElementSelector<Track> trackSelector = new TrackSelector();
      trackSelectors.add(i++, trackSelector);

      try {
        trackSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        new WorkflowOperationException("Source left flavor '" + flavor + "' is malformed");
      }
    }

    // Add tags to the track selector for each source
    i = 0;
    for (String tag : sourceTags) {
      AbstractMediaPackageElementSelector<Track> trackSelector = trackSelectors.get(i);
      if (trackSelector == null) {
        trackSelector = new TrackSelector();
        trackSelectors.add(i, trackSelector);
      }

      try {
        trackSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(tag));
      } catch (IllegalArgumentException e) {
        new WorkflowOperationException("Source left flavor '" + tag + "' is malformed");
      }
      i++;
    }

    List<Track> tracks = new ArrayList<Track>();

    i = 0;
    for (AbstractMediaPackageElementSelector<Track> trackSelector : trackSelectors) {
      Collection<Track> tracksForFlavor = trackSelector.select(mediaPackage, false);
      String currentFlavor = sourceFlavors.get(i);

      if (tracksForFlavor.size() > 1) {
        logger.warn(
                "More than one left track has been found with flavor {} for concat operation, skipping concatenation!: {}",
                currentFlavor, tracksForFlavor);
        return createResult(mediaPackage, Action.SKIP);
      } else if (tracksForFlavor.size() == 0) {
        logger.warn("No track has been found with flavor {} for concat operation, skipping concatenation!",
                currentFlavor);
        return createResult(mediaPackage, Action.SKIP);
      }

      for (Track t : tracksForFlavor)
        tracks.add(i, t);

      VideoStream[] videoStreams = TrackSupport.byType(tracks.get(i).getStreams(), VideoStream.class);
      if (videoStreams.length == 0) {
        logger.info("No video stream available in the track with flavor {}! {}", currentFlavor, tracks.get(i));
        return createResult(mediaPackage, Action.SKIP);
      }
      i++;
    }

    Dimension outputDimension;
    try {
      if (outputResolution.startsWith("part-")) {
        outputDimension = getOutPutResolutionFromFlavorOrTag(tracks, outputResolution);
      } else {
        String[] outputResolutionArray = StringUtils.split(outputResolution, "x");
        if (outputResolutionArray.length != 2) {
          throw new WorkflowOperationException("Invalid format of output resolution!");
        }
        outputDimension = Dimension.dimension(Integer.parseInt(outputResolutionArray[0]),
                Integer.parseInt(outputResolutionArray[1]));
      }
    } catch (WorkflowOperationException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowOperationException("Unable to parse output resolution!", e);
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

      // Adjust the target tags
      for (String tag : targetTags) {
        logger.trace("Tagging compound track with '{}'", tag);
        concatTrack.addTag(tag);
      }

      // Adjust the target flavor.
      concatTrack.setFlavor(targetFlavor);
      logger.debug("Compound track has flavor '{}'", concatTrack.getFlavor());

      mediaPackage.add(concatTrack);
      WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, concatJob.getQueueTime());
      logger.debug("Concat operation completed");
      return result;
    } else {
      logger.info("concat operation unsuccessful, no payload returned: {}", concatJob);
      return createResult(mediaPackage, Action.SKIP);
    }
  }

  private Dimension getOutPutResolutionFromFlavorOrTag(List<Track> tracks, String outputResolution)
          throws WorkflowOperationException {
    int videoPosition = Integer.parseInt(outputResolution.substring("part-".length()));
    VideoStream[] videoStreams = TrackSupport.byType(tracks.get(videoPosition).getStreams(), VideoStream.class);
    if (videoStreams.length == 0)
      return null;
    return new Dimension(videoStreams[0].getFrameWidth(), videoStreams[0].getFrameHeight());
  }

  /**
   * Returns the list of all the configured keys for the given iterative key
   * 
   * @param keyPrefix
   *          the iterative key prefix. For example "source-flavor-" for the keys "source-flavor-1", "source-flavor-2",
   *          ...
   * @param operation
   *          The workflow operation instance
   * @return a list with all the keys as string
   * @throws WorkflowOperationException
   */
  private List<String> getConfiguredParamsFromIterativeKey(String keyPrefix, WorkflowOperationInstance operation)
          throws WorkflowOperationException {
    List<String> configuredParams = new ArrayList<String>();
    int index = 0;
    String sourceKey;
    while ((sourceKey = StringUtils.trimToNull(operation.getConfiguration(keyPrefix + index))) != null) {
      if (configuredParams.contains(sourceKey))
        throw new WorkflowOperationException("The key " + sourceKey
                + " is duplicated, therefore all the keys with the prefix " + keyPrefix + " are invalid.");
      else
        configuredParams.add(index++, sourceKey);
    }

    logger.debug("Got {} keys for the iterative key  '{}'.", configuredParams.size(), keyPrefix);

    return configuredParams;
  }
}
