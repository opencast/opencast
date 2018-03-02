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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "Demux from epiphan CA" operations This allows to demux and tagging to be done
 * in one operation and save 5-60 mins from each wf
 */
public class DemuxWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DemuxWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavors", "The \"flavor\" of the track to use as a source input");
    CONFIG_OPTIONS.put("source-tags", "The \"tag\" of the track to use as a source input");
    CONFIG_OPTIONS.put("encoding-profile",
            "The encoding profile to use, this is one profile with multiple outputs listed");
    CONFIG_OPTIONS.put("target-flavors",
            "The flavors to apply to the encoded file in the same order as in the encoding profile,sections separated by \";\"");
    CONFIG_OPTIONS.put("target-tags",
            "The tags to apply to the encoded files, sections ordered as in the encoding profile and separated by \";\", each tag separated by \",\"");
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
    logger.debug("Running DCE DEmux workflow operation on workflow {}", workflowInstance.getId());

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
    final String separator = ",";
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
    if (encodingProfile == null)
      encodingProfile = StringUtils.trimToEmpty(operation.getConfiguration("encoding-profiles"));
    String[] targetFlavors = (targetFlavorsOption != null) ? targetFlavorsOption.split(separator) : null;
    String[] targetTags = (targetTagsOption != null) ? targetTagsOption.split(sectionSeparator) : null;
    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();

    // Make sure either one of tags or flavors are provided
    if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorsOption)
            && StringUtils.isBlank(sourceFlavorsOption)) {
      logger.info("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Select the source flavors
    for (String flavor : asList(sourceFlavorsOption)) {
      try {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Source flavor '" + flavor + "' is malformed");
      }
    }

    // Select the source tags
    for (String tag : asList(sourceTagsOption)) {
      elementSelector.addTag(tag);
    }

    // Find the encoding profile - should only be one
    EncodingProfile profile = composerService.getProfile(encodingProfile);
    if (profile == null)
      throw new WorkflowOperationException("Encoding profile '" + encodingProfile + "' was not found");
    // Look for elements matching the tag
    Collection<Track> elements = elementSelector.select(mediaPackage, false);

    long totalTimeInQueue = 0;
    Map<Job, JobInformation> encodingJobs = new HashMap<Job, JobInformation>();
    for (Track track : elements) { // For each source
      logger.info("Encoding track {} using encoding profile '{}'", track, profile);
      // Start encoding and wait for the result
      encodingJobs.put(composerService.demux(track, profile.getIdentifier()), new JobInformation(track, profile));
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
      Track track = entry.getValue().getTrack(); // source

      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();

      // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
      if (job.getPayload().length() > 0) {
        List<Track> composedTracks = null;
        composedTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
        if (targetFlavors != null && composedTracks.size() != targetFlavors.length && targetFlavors.length != 1) {
          logger.info("Encoded {} tracks, with {} flavors", composedTracks.size(), targetFlavors.length);
          throw new WorkflowOperationException("Number of Flavors does not match encoding profile outputs");
        }
        if (targetTags != null && composedTracks.size() != targetTags.length && targetTags.length != 1) {
          logger.info("Encoded {} tracks, with {} tags", composedTracks.size(), targetTags.length);
          throw new WorkflowOperationException("Number of Tag Sections does not match encoding profile outputs");
        }

        // Flavor each track in the order read
        int i = 0;
        int j = 0;
        for (Track composedTrack : composedTracks) {
          if (targetFlavors != null && targetFlavors.length > 0) { // Has Flavors
            // set it to the matching flavor in the order listed
            composedTrack.setFlavor(newFlavor(track, targetFlavors[i]));
            if (targetFlavors.length > 1)
              i++;
          }
          if (targetTags != null && targetTags.length > 0) { // Has tags
            for (String tag : asList(targetTags[j])) {
              composedTrack.addTag(tag);
            }
            logger.trace("Tagging composed track with '{}'", targetTags[j].toString());
            if (targetTags.length > 1) {
              j++;
            }
          }
          // store new tracks to mediaPackage
          String fileName = getFileNameFromElements(track, composedTrack);
          composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
                  composedTrack.getIdentifier(), fileName));
          mediaPackage.addDerived(composedTrack, track);
        }
      } else {
        logger.warn("No output from Demux operation");
      }
    }

    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
    logger.debug("Demux operation completed");
    return result;
  }

  private MediaPackageElementFlavor newFlavor(Track track, String flavor) throws WorkflowOperationException {
    MediaPackageElementFlavor targetFlavor = null;

    if (StringUtils.isNotBlank(flavor)) {
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
        throw new WorkflowOperationException("Target flavor '" + flavor + "' is malformed");
      }
    }
    return null;
  }

  /**
   * This class is used to store context information for the jobs.
   */
  private static final class JobInformation {

    private Track track = null;

    JobInformation(Track track, EncodingProfile profile) {
      this.track = track;
    }

    /**
     * Returns the track.
     *
     * @return the track
     */
    public Track getTrack() {
      return track;
    }

  }

}
