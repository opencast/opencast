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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The workflow definition for handling demux operations.
 */
public class DemuxWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DemuxWorkflowOperationHandler.class);

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
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running demux workflow operation on workflow {}", workflowInstance.getId());

    try {
      return demux(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   *
   * @param src
   *          The source media package
   * @param operation
   *          the current workflow operation
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
  private WorkflowOperationResult demux(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException, IOException, NotFoundException, MediaPackageException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    final String sectionSeparator = ";";
    // Check which tags have been configured
    String sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    String sourceFlavorsOption = StringUtils.trimToNull(operation.getConfiguration("source-flavors"));
    if (sourceFlavorsOption == null)
      sourceFlavorsOption = StringUtils.trimToEmpty(operation.getConfiguration("source-flavor"));
    String targetFlavorsOption = StringUtils.trimToNull(operation.getConfiguration("target-flavors"));
    if (targetFlavorsOption == null)
      targetFlavorsOption = StringUtils.trimToEmpty(operation.getConfiguration("target-flavor"));
    String targetTagsOption = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String encodingProfile = StringUtils.trimToEmpty(operation.getConfiguration("encoding-profile"));

    // Make sure either one of tags or flavors are provided
    if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorsOption)) {
      logger.info("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    List<String> targetFlavors = asList(targetFlavorsOption);
    String[] targetTags = StringUtils.split(targetTagsOption, sectionSeparator);
    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();

    // Select the source flavors
    for (String flavor : asList(sourceFlavorsOption)) {
      try {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(String.format("Source flavor '%s' is malformed", flavor));
      }
    }

    // Select the source tags
    for (String tag : asList(sourceTagsOption)) {
      elementSelector.addTag(tag);
    }

    // Find the encoding profile - should only be one
    EncodingProfile profile = composerService.getProfile(encodingProfile);
    if (profile == null) {
      throw new WorkflowOperationException(String.format("Encoding profile '%s' was not found", encodingProfile));
    }
    // Look for elements matching the tag
    Collection<Track> sourceTracks = elementSelector.select(mediaPackage, false);
    if (sourceTracks.isEmpty()) {
      logger.info("No matching tracks found");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    long totalTimeInQueue = 0;
    Map<Job, Track> encodingJobs = new HashMap<>();
    for (Track track : sourceTracks) {
      logger.info("Demuxing track {} using encoding profile '{}'", track, profile);
      // Start encoding and wait for the result
      encodingJobs.put(composerService.demux(track, profile.getIdentifier()), track);
    }

    // Wait for the jobs to return
    if (!waitForStatus(encodingJobs.keySet().toArray(new Job[encodingJobs.size()])).isSuccess()) {
      throw new WorkflowOperationException("One of the encoding jobs did not complete successfully");
    }

    // Process the result
    for (Map.Entry<Job, Track> entry : encodingJobs.entrySet()) {
      Job job = entry.getKey();
      Track sourceTrack = entry.getValue();

      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();

      // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
      if (job.getPayload().length() <= 0) {
        logger.warn("No output from Demux operation");
        continue;
      }

      List<Track> composedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
      if (composedTracks.size() != targetFlavors.size() && targetFlavors.size() != 1) {
        throw new WorkflowOperationException(String.format("Number of target flavors (%d) and output tracks (%d) do "
                + "not match", targetFlavors.size(), composedTracks.size()));
      }
      if (composedTracks.size() != targetTags.length && targetTags.length != 1 && targetTags.length != 0) {
        throw new WorkflowOperationException(String.format("Number of target tag groups (%d) and output tracks (%d) "
                + "do not match", targetTags.length, composedTracks.size()));
      }

      // Flavor each track in the order read
      int flavorIndex = 0;
      int tagsIndex = 0;
      for (Track composedTrack : composedTracks) {
        // set flavor to the matching flavor in the order listed
        composedTrack.setFlavor(newFlavor(sourceTrack, targetFlavors.get(flavorIndex)));
        if (targetFlavors.size() > 1) {
          flavorIndex++;
        }
        if (targetTags.length > 0) {
          asList(targetTags[tagsIndex]).forEach(composedTrack::addTag);
          logger.trace("Tagging composed track with '{}'", targetTags[tagsIndex]);
          if (targetTags.length > 1) {
            tagsIndex++;
          }
        }
        // store new tracks to mediaPackage
        String fileName = getFileNameFromElements(sourceTrack, composedTrack);
        composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
                composedTrack.getIdentifier(), fileName));
        mediaPackage.addDerived(composedTrack, sourceTrack);
      }
    }

    logger.debug("Demux operation completed");
    return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
  }

  private MediaPackageElementFlavor newFlavor(Track track, String flavor) throws WorkflowOperationException {
    MediaPackageElementFlavor targetFlavor;

    try {
      targetFlavor = MediaPackageElementFlavor.parseFlavor(flavor);
      String flavorType = targetFlavor.getType();
      String flavorSubtype = targetFlavor.getSubtype();
      // Adjust the target flavor. Make sure to account for partial updates
      if ("*".equals(flavorType))
        flavorType = track.getFlavor().getType();
      if ("*".equals(flavorSubtype))
        flavorSubtype = track.getFlavor().getSubtype();
      return (new MediaPackageElementFlavor(flavorType, flavorSubtype));
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException(String.format("Target flavor '%s' is malformed", flavor));
    }
  }

}
