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

package org.opencastproject.workflow.handler.sox;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.sox.api.SoxException;
import org.opencastproject.sox.api.SoxService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The workflow definition for handling "sox" operations
 */
public class NormalizeAudioWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(NormalizeAudioWorkflowOperationHandler.class);

  /** Name of the 'encode to SoX audio only work copy' encoding profile */
  public static final String SOX_AONLY_PROFILE = "sox-audio-only.work";

  /** Name of the muxing encoding profile */
  public static final String SOX_AREPLACE_PROFILE = "sox-audio-replace.work";

  /** The SoX service */
  private SoxService soxService = null;

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
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
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running sox workflow operation on workflow {}", workflowInstance.getId());

    try {
      return normalize(workflowInstance.getMediaPackage(), workflowInstance);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  private WorkflowOperationResult normalize(MediaPackage src, WorkflowInstance workflowInstance) throws SoxException,
          IOException, NotFoundException, MediaPackageException, WorkflowOperationException, EncoderException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance, Configuration.many,
        Configuration.many, Configuration.many, Configuration.many);
    List<String> targetTagsOption = tagsAndFlavors.getTargetTags();
    List<String> sourceTagsOption = tagsAndFlavors.getSrcTags();
    List<MediaPackageElementFlavor> sourceFlavorsOption = tagsAndFlavors.getSrcFlavors();
    List<MediaPackageElementFlavor> targetFlavorOption = tagsAndFlavors.getTargetFlavors();

    String targetDecibelString = StringUtils.trimToNull(operation.getConfiguration("target-decibel"));
    if (targetDecibelString == null) {
      throw new IllegalArgumentException("target-decibel must be specified");
    }
    boolean forceTranscode = BooleanUtils.toBoolean(operation.getConfiguration("force-transcode"));
    Float targetDecibel;
    try {
      targetDecibel = new Float(targetDecibelString);
    } catch (NumberFormatException e1) {
      throw new WorkflowOperationException("Unable to parse target-decibel " + targetDecibelString);
    }

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

    //select target flavor
    MediaPackageElementFlavor targetFlavor = null;
    if (!targetFlavorOption.isEmpty()) {
      targetFlavor = targetFlavorOption.get(0);
    }

    // Look for elements matching the tag
    Collection<Track> elements = elementSelector.select(mediaPackage, false);

    // Encode all tracks found
    long totalTimeInQueue = 0;
    List<URI> cleanupURIs = new ArrayList<URI>();
    Map<Job, Track> normalizeJobs = new HashMap<Job, Track>();
    try {
      for (Track track : elements) {

        TrackImpl audioTrack = (TrackImpl) track;
        // Skip video only mismatches
        if (!track.hasAudio()) {
          logger.info("Skipping audio normalization of '{}', since it contains no audio stream", track);
          continue;
        } else if (track.hasVideo() || forceTranscode) {
          audioTrack = (TrackImpl) extractAudioTrack(track);
          audioTrack.setAudio(((TrackImpl) track).getAudio());
          cleanupURIs.add(audioTrack.getURI());
        }

        // Analyze audio track
        if (audioTrack.getAudio().size() < 1 || audioTrack.getAudio().get(0).getRmsLevDb() == null) {
          logger.info("Audio track {} has no RMS Lev dB metadata, analyze it first", audioTrack);
          Job analyzeJob = soxService.analyze(audioTrack);
          if (!waitForStatus(analyzeJob).isSuccess()) {
            throw new WorkflowOperationException("Unable to analyze the audio track " + audioTrack);
          }
          audioTrack = (TrackImpl) MediaPackageElementParser.getFromXml(analyzeJob.getPayload());
          cleanupURIs.add(audioTrack.getURI());
        }

        normalizeJobs.put(soxService.normalize(audioTrack, targetDecibel), track);
      }

      if (normalizeJobs.isEmpty()) {
        logger.info("No matching tracks found");
        return createResult(mediaPackage, Action.CONTINUE);
      }

      // Wait for the jobs to return
      if (!waitForStatus(normalizeJobs.keySet().toArray(new Job[normalizeJobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("One of the normalize jobs did not complete successfully");
      }

      // Process the result
      for (Map.Entry<Job, Track> entry : normalizeJobs.entrySet()) {
        Job job = entry.getKey();
        TrackImpl origTrack = (TrackImpl) entry.getValue();

        // add this receipt's queue time to the total
        totalTimeInQueue += job.getQueueTime();

        if (job.getPayload().length() > 0) {
          TrackImpl normalizedAudioTrack = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());

          TrackImpl resultTrack = normalizedAudioTrack;
          if (origTrack.hasVideo() || forceTranscode) {
            cleanupURIs.add(normalizedAudioTrack.getURI());

            logger.info("Mux normalized audio track {} to video track {}", normalizedAudioTrack, origTrack);
            Job muxAudioVideo = composerService.mux(origTrack, normalizedAudioTrack, SOX_AREPLACE_PROFILE);
            if (!waitForStatus(muxAudioVideo).isSuccess()) {
              throw new WorkflowOperationException("Muxing normalized audio track " + normalizedAudioTrack
                      + " to video container " + origTrack + " failed");
            }

            resultTrack = (TrackImpl) MediaPackageElementParser.getFromXml(muxAudioVideo.getPayload());

            // Set metadata on track
            extendAudioStream(resultTrack, normalizedAudioTrack);
          }

          adjustFlavorAndTags(targetTagsOption, targetFlavor, origTrack, resultTrack);

          mediaPackage.addDerived(resultTrack, origTrack);
          String fileName = getFileNameFromElements(origTrack, resultTrack);
          resultTrack.setURI(workspace.moveTo(resultTrack.getURI(), mediaPackage.getIdentifier().toString(),
                  resultTrack.getIdentifier(), fileName));
        } else {
          logger.warn("Normalize audio job {} for track {} has no result!", job, origTrack);
        }
      }
    } finally {
      // Clean up temporary audio and video files from workspace
      for (URI uri : cleanupURIs) {
        workspace.delete(uri);
      }
    }

    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
    logger.debug("Normalize audio operation completed");
    return result;
  }

  private void extendAudioStream(TrackImpl trackToExtend, TrackImpl audioTrackSource) {
    AudioStreamImpl extendStream = (AudioStreamImpl) trackToExtend.getAudio().get(0);
    AudioStream sourceStream = audioTrackSource.getAudio().get(0);
    extendStream.setPkLevDb(sourceStream.getPkLevDb());
    extendStream.setRmsLevDb(sourceStream.getRmsLevDb());
    extendStream.setRmsPkDb(sourceStream.getRmsPkDb());
  }

  private void adjustFlavorAndTags(List<String> targetTags, MediaPackageElementFlavor targetFlavor, Track origTrack,
          Track normalized) {
    // Adjust the target tags
    for (String tag : targetTags) {
      logger.trace("Tagging normalized track with '{}'", tag);
      normalized.addTag(tag);
    }

    // Adjust the target flavor. Make sure to account for partial updates
    if (targetFlavor != null) {
      String flavorType = targetFlavor.getType();
      String flavorSubtype = targetFlavor.getSubtype();
      if ("*".equals(flavorType)) {
        flavorType = origTrack.getFlavor().getType();
      }
      if ("*".equals(flavorSubtype)) {
        flavorSubtype = origTrack.getFlavor().getSubtype();
      }
      normalized.setFlavor(new MediaPackageElementFlavor(flavorType, flavorSubtype));
      logger.debug("Normalized track has flavor '{}'", normalized.getFlavor());
    }
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
    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException("Extracting audio track from video track " + videoTrack + " failed");
    }

    return (Track) MediaPackageElementParser.getFromXml(job.getPayload());
  }

}
