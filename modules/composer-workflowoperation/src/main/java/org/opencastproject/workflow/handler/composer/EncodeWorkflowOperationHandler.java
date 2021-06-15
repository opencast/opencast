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
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The workflow definition for handling "compose" operations
 */
public class EncodeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EncodeWorkflowOperationHandler.class);

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
  protected void setComposerService(ComposerService composerService) {
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
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running parallel encoding workflow operation on workflow {}", workflowInstance.getId());

    try {
      return encode(workflowInstance);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   *
   * @param workflowInstance
   *          the current workflow instance
   * @return the operation result containing the updated media package
   * @throws EncoderException
   *           if encoding fails
   * @throws WorkflowOperationException
   *           if errors occur during processing
   * @throws IOException
   *           if the workspace operations fail
   * @throws NotFoundException
   *           if the workspace doesn't contain the requested file
   */
  private WorkflowOperationResult encode(WorkflowInstance workflowInstance)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage src = workflowInstance.getMediaPackage();
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
        Configuration.many, Configuration.many, Configuration.many, Configuration.one);
    List<String> sourceTagsOption = tagsAndFlavors.getSrcTags();
    List<String> targetTagsOption = tagsAndFlavors.getTargetTags();
    List<MediaPackageElementFlavor> sourceFlavorsOption = tagsAndFlavors.getSrcFlavors();
    MediaPackageElementFlavor targetFlavor = tagsAndFlavors.getSingleTargetFlavor();

    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();

    // Make sure either one of tags or flavors are provided
    if (sourceTagsOption.isEmpty() && sourceFlavorsOption.isEmpty()) {
      logger.info("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Select the source flavors
    for (MediaPackageElementFlavor flavor : sourceFlavorsOption) {
        elementSelector.addFlavor(flavor);
    }

    // Select the source tags
    for (String tag : sourceTagsOption) {
      elementSelector.addTag(tag);
    }

    // Find the encoding profile
    String profilesOption = StringUtils.trimToNull(operation.getConfiguration("encoding-profiles"));
    List<EncodingProfile> profiles = new ArrayList<EncodingProfile>();
    for (String profileName : asList(profilesOption)) {
      EncodingProfile profile = composerService.getProfile(profileName);
      if (profile == null)
        throw new WorkflowOperationException("Encoding profile '" + profileName + "' was not found");
      profiles.add(profile);
    }

    // Support legacy "encoding-profile" option
    String profileOption = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));
    if (StringUtils.isNotBlank(profileOption)) {
      String profileId = StringUtils.trim(profileOption);
      EncodingProfile profile = composerService.getProfile(profileId);
      if (profile == null)
        throw new WorkflowOperationException("Encoding profile '" + profileId + "' was not found");
      profiles.add(profile);
    }

    // Make sure there is at least one profile
    if (profiles.isEmpty())
      throw new WorkflowOperationException("No encoding profile was specified");

    // Look for elements matching the tag
    Collection<Track> elements = elementSelector.select(mediaPackage, false);

    // Encode all tracks found
    long totalTimeInQueue = 0;
    Map<Job, JobInformation> encodingJobs = new HashMap<Job, JobInformation>();
    for (Track track : elements) {

      // Encode the track with all profiles
      for (EncodingProfile profile : profiles) {

        // Check if the track supports the output type of the profile
        MediaType outputType = profile.getOutputType();
        if (outputType.equals(MediaType.Audio) && !track.hasAudio()) {
          logger.info("Skipping encoding of '{}', since it lacks an audio stream", track);
          continue;
        } else if (outputType.equals(MediaType.Visual) && !track.hasVideo()) {
          logger.info("Skipping encoding of '{}', since it lacks a video stream", track);
          continue;
        }

        logger.info("Encoding track {} using encoding profile '{}'", track, profile);

        // Start encoding and wait for the result
        encodingJobs.put(composerService.parallelEncode(track, profile.getIdentifier()), new JobInformation(track, profile));
      }
    }

    if (encodingJobs.isEmpty()) {
      logger.info("No matching tracks found");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Wait for the jobs to return
    if (!waitForStatus(encodingJobs.keySet().toArray(new Job[encodingJobs.size()])).isSuccess()) {
      throw new WorkflowOperationException("One of the encoding jobs did not complete successfully");
    }

    // Process the result
    for (Map.Entry<Job, JobInformation> entry : encodingJobs.entrySet()) {
      Job job = entry.getKey();
      Track track = entry.getValue().getTrack();

      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();
      // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
      if (job.getPayload().length() > 0) {
        List <Track> composedTracks = (List <Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());

        // Adjust the target tags
        for (Track encodedTrack : composedTracks) {
          for (String tag : targetTagsOption) {
            logger.trace("Tagging composed track {} with '{}'", encodedTrack.toString(), tag);
            encodedTrack.addTag(tag);
          }
        }

        // Adjust the target flavor. Make sure to account for partial updates
        if (targetFlavor != null) {
          String flavorType = targetFlavor.getType();
          String flavorSubtype = targetFlavor.getSubtype();
          if ("*".equals(flavorType))
            flavorType = track.getFlavor().getType();
          if ("*".equals(flavorSubtype))
            flavorSubtype = track.getFlavor().getSubtype();
          for (Track encodedTrack : composedTracks) {
            encodedTrack.setFlavor(new MediaPackageElementFlavor(flavorType, flavorSubtype));
            logger.debug("Composed track {} has flavor '{}'", encodedTrack.toString(), encodedTrack.getFlavor());
          }
        }

        // store new tracks to mediaPackage
        for (Track encodedTrack : composedTracks) {
          mediaPackage.addDerived(encodedTrack, track);
          String fileName = getFileNameFromElements(track, encodedTrack);
          encodedTrack.setURI(workspace.moveTo(encodedTrack.getURI(), mediaPackage.getIdentifier().toString(),
                                                encodedTrack.getIdentifier(), fileName));
        }
      }
    }

    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
    logger.debug("Parallel encode operation completed");
    return result;
  }

  /**
   * This class is used to store context information for the jobs.
   */
  private static final class JobInformation {

    private Track track = null;
    private EncodingProfile profile = null;

    JobInformation(Track track, EncodingProfile profile) {
      this.track = track;
      this.profile = profile;
    }

    /**
     * Returns the track.
     *
     * @return the track
     */
    public Track getTrack() {
      return track;
    }

    /**
     * Returns the profile.
     *
     * @return the profile
     */
    public EncodingProfile getProfile() {
      return profile;
    }

  }

}
