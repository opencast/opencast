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
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.sox.api.SoxException;
import org.opencastproject.sox.api.SoxService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "sox" operations
 */
public class AnalyzeAudioWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AnalyzeAudioWorkflowOperationHandler.class);

  /** Name of the 'encode to SoX audio only work copy' encoding profile */
  public static final String SOX_AONLY_PROFILE = "sox-audio-only.work";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavors", "The \"flavors\" of the track to use as a source input");
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a source input");
    CONFIG_OPTIONS.put("source-tags", "The \"tag\" of the track to use as a source input");
    CONFIG_OPTIONS.put("force-transcode", "Whether to force transcoding the audio stream");
  }

  /** The SoX service */
  private SoxService soxService = null;

  /** The composer service */
  private ComposerService composerService = null;

  /** The workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param soxService
   *          the SoX service
   */
  protected void setSoxService(SoxService soxService) {
    this.soxService = soxService;
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
   * @param workspace
   *          the workspace
   */
  protected void setWorkspace(Workspace workspace) {
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
    logger.debug("Running analyze audio workflow operation on workflow {}", workflowInstance.getId());

    try {
      return analyze(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  private WorkflowOperationResult analyze(MediaPackage src, WorkflowOperationInstance operation) throws SoxException,
          IOException, NotFoundException, MediaPackageException, WorkflowOperationException, EncoderException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    // Check which tags have been configured
    String sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    String sourceFlavorOption = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String sourceFlavorsOption = StringUtils.trimToNull(operation.getConfiguration("source-flavors"));
    boolean forceTranscode = BooleanUtils.toBoolean(operation.getConfiguration("force-transcode"));

    AbstractMediaPackageElementSelector<Track> elementSelector = new TrackSelector();

    // Make sure either one of tags or flavors are provided
    if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorOption)
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

    // Support legacy "source-flavor" option
    if (StringUtils.isNotBlank(sourceFlavorOption)) {
      String flavor = StringUtils.trim(sourceFlavorOption);
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

    // Look for elements matching the tag
    Collection<Track> elements = elementSelector.select(mediaPackage, false);

    // Analyze audio for all tracks found
    long totalTimeInQueue = 0;
    List<URI> cleanupURIs = new ArrayList<URI>();
    Map<Job, Track> analyzeJobs = new HashMap<Job, Track>();
    try {
      for (Track track : elements) {

        TrackImpl audioTrack = (TrackImpl) track;
        // Skip video only mismatches
        if (!track.hasAudio()) {
          logger.info("Skipping audio analysis of '{}', since it contains no audio stream", track);
          continue;
        } else if (track.hasVideo() || forceTranscode) {
          audioTrack = (TrackImpl) extractAudioTrack(track);
          audioTrack.setAudio(((TrackImpl) track).getAudio());
          cleanupURIs.add(audioTrack.getURI());
        }

        analyzeJobs.put(soxService.analyze(audioTrack), track);
      }

      if (analyzeJobs.isEmpty()) {
        logger.info("No matching tracks found");
        return createResult(mediaPackage, Action.CONTINUE);
      }

      // Wait for the jobs to return
      if (!waitForStatus(analyzeJobs.keySet().toArray(new Job[analyzeJobs.size()])).isSuccess())
        throw new WorkflowOperationException("One of the analyze jobs did not complete successfully");

      // Process the result
      for (Map.Entry<Job, Track> entry : analyzeJobs.entrySet()) {
        Job job = entry.getKey();
        TrackImpl origTrack = (TrackImpl) entry.getValue();

        // add this receipt's queue time to the total
        totalTimeInQueue += job.getQueueTime();

        if (job.getPayload().length() > 0) {
          TrackImpl analyzed = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());

          // Set metadata on track
          origTrack.setAudio(analyzed.getAudio());
        } else {
          logger.warn("Analyze audio job {} for track {} has no result!", job, origTrack);
        }
      }
    } finally {
      // Clean up temporary audio files from workspace
      for (URI uri : cleanupURIs) {
        workspace.delete(uri);
      }
    }

    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
    logger.debug("Analyze audio operation completed");
    return result;
  }

  /**
   * Extract the audio track from the given video track.
   *
   * @param videoTrack
   *          the track containing the audio
   * @return the extracted audio track
   * @throws WorkflowOperationException
   * @throws NotFoundException
   * @throws EncoderException
   * @throws MediaPackageException
   */
  private Track extractAudioTrack(Track videoTrack) throws WorkflowOperationException, EncoderException,
          MediaPackageException {
    logger.info("Extract audio stream from track {}", videoTrack);
    Job job = composerService.encode(videoTrack, SOX_AONLY_PROFILE);
    if (!waitForStatus(job).isSuccess())
      throw new WorkflowOperationException("Extracting audio track from video track " + videoTrack + " failed");

    return (Track) MediaPackageElementParser.getFromXml(job.getPayload());
  }

}
