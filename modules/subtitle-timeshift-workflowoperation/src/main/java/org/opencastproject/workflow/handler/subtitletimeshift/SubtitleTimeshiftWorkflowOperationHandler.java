/*
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

package org.opencastproject.workflow.handler.subtitletimeshift;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.subtitleparser.webvttparser.WebVTTParser;
import org.opencastproject.subtitleparser.webvttparser.WebVTTSubtitle;
import org.opencastproject.subtitleparser.webvttparser.WebVTTSubtitleCue;
import org.opencastproject.subtitleparser.webvttparser.WebVTTWriter;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * This workflow operation allows to shift the timestamps of subtitle files.
 * For example: If someone adds a bumper/intro video in front of an already subtitled presenter track
 * the subtitles would start too early. With this operation, you can select a video and a subtitle track and the
 * timestamps of the subtitle file will be shifted backwards by the duration of the selected video.
 */
@Component(
    property = {
        "service.description=subtitle-timeshift Workflow Operation Handler",
        "workflow.operation=subtitle-timeshift"
    },
    immediate = true,
    service = WorkflowOperationHandler.class
)
public class SubtitleTimeshiftWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String SUBTITLE_SOURCE_FLAVOR_CFG_KEY = "subtitle-source-flavor";
  private static final String VIDEO_SOURCE_FLAVOR_CFG_KEY = "video-source-flavor";
  private static final String TARGET_FLAVOR_CFG_KEY = "target-flavor";

  /** The workspace collection name */
  private static final String COLLECTION = "subtitles";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SubtitleTimeshiftWorkflowOperationHandler.class);

  /**
   * Reference to the workspace service
   */
  private Workspace workspace = null;


  /**
   * OSGi setter for the workspace class
   *
   * @param workspace an instance of the workspace
   */
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }


  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    logger.info("Starting subtitle timeshift workflow for mediapackage: {}", mediaPackage.getIdentifier().toString());

    // get flavor from workflow configuration
    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    MediaPackageElementFlavor configuredSubtitleFlavor;
    MediaPackageElementFlavor configuredVideoFlavor;
    MediaPackageElementFlavor configuredTargetFlavor;
    try {
      configuredSubtitleFlavor = MediaPackageElementFlavor.parseFlavor(
          Objects.toString(operation.getConfiguration(SUBTITLE_SOURCE_FLAVOR_CFG_KEY)));
      configuredVideoFlavor = MediaPackageElementFlavor.parseFlavor(
          operation.getConfiguration(VIDEO_SOURCE_FLAVOR_CFG_KEY));
      configuredTargetFlavor = MediaPackageElementFlavor.parseFlavor(
          operation.getConfiguration(TARGET_FLAVOR_CFG_KEY));
    } catch (Exception e) {
      throw new WorkflowOperationException("Couldn't parse subtitle-timeshift workflow configurations.", e);
    }

    // In this block we try to get the subtitle and video track for this workflow
    Track[] originalSubtitleTracks;
    Track videoTrack;
    try {
      // Get the subtitles and videos from the mediapackage
      Track[] subtitleTracks = mediaPackage.getTracks(configuredSubtitleFlavor);
      Track[] videoTracks = mediaPackage.getTracks(configuredVideoFlavor);

      // Check if we found the right amount of subtitles and videos
      // Allowed are exactly 1 video track and at least 1 subtitle track
      if (subtitleTracks.length == 0) {
        // if no subtitle track was found, we skip the workflow operation
        logger.info("No subtitle track found with flavor {}. Skipping subtitle-timeshift workflow operation "
            + "for mediapackage {}", configuredSubtitleFlavor, mediaPackage.getIdentifier());
        return createResult(mediaPackage, Action.SKIP);
      } else if (videoTracks.length != 1) {
        // if the amount of video tracks is not 1, something is configured wrong and we throw an exception
        throw new IllegalStateException(String.format("Found %d video tracks with flavor %s in mediapackage %s "
            + "for subtitle-timeshift operation. Expected exactly 1 video track. Please check you workflow "
            + "configuration.", videoTracks.length, configuredVideoFlavor, mediaPackage.getIdentifier()));
      }

      // these subtitle tracks will be used to create the new subtitle tracks with the shifted timestamps
      originalSubtitleTracks = subtitleTracks;

      // this video track will be used to determine how much the subtitle tracks should be shifted
      videoTrack = videoTracks[0];

      logger.info("Valid tracks found. Start shifting subtitle tracks by duration from video '{}'", videoTrack);

    } catch (Exception e) {
      logger.error("Error in subtitle-timeshift workflow while getting tracks for mediapackage {}",
          mediaPackage.getIdentifier(), e);
      throw new WorkflowOperationException(e);
    }

    // In this block we try to create the new subtitle tracks and add them to the mediapackage
    try {
      for (Track originalSubtitleTrack : originalSubtitleTracks) {

        // load the subtitle file from workspace and parse it into a webvtt object
        WebVTTSubtitle newSubtitleFile = loadAndParseSubtitleFile(originalSubtitleTrack);

        // shift the timestamps of the parsed webvtt object
        shiftTime(newSubtitleFile, videoTrack.getDuration());

        // save the new subtitle file in the workspace to get a URI
        String originalFileName = FilenameUtils.getBaseName(originalSubtitleTrack.getLogicalName());
        String newFileName = "timeshifted-" + originalFileName + ".vtt";
        URI newSubtitleFileUri = saveSubtitleFileToWorkspace(newSubtitleFile, newFileName);

        // create a track object out of the subtitle URI
        Track newSubtitleTrack = createNewTrackFromSubtitleUri(newSubtitleFileUri, configuredTargetFlavor,
            originalSubtitleTrack);

        // save the new subtitle track to the mediapackage
        mediaPackage.add(newSubtitleTrack);
        logger.info("Added subtitle track with URI {} to mediapackage {}", newSubtitleFileUri,
            mediaPackage.getIdentifier());
      }

    } catch (Exception e) {
      logger.error("Error while shifting time of subtitle tracks for mediapackage {}", mediaPackage.getIdentifier(), e);
      throw new WorkflowOperationException(e);
    }

    logger.info("Subtitle-Timeshift workflow operation for media package {} completed", mediaPackage);
    return createResult(mediaPackage, Action.CONTINUE);
  }

  /**
   * Takes several parameter for the new subtitle file in and creates a track object from it.
   *
   * @param subtitleFile The new subtitle file that will be contained in the track.
   * @param targetFlavor The future flavor of the new subtitle track.
   * @param originalSubtitleTrack The original subtitle track.
   * @return The subtitle file as a track.
   */
  private Track createNewTrackFromSubtitleUri(URI subtitleFile, MediaPackageElementFlavor targetFlavor,
      Track originalSubtitleTrack) throws IOException, NotFoundException {

    String id = UUID.randomUUID().toString();
    Track newSubtitleTrack = (Track) originalSubtitleTrack.clone();
    newSubtitleTrack.setIdentifier(id);
    newSubtitleTrack.setFlavor(targetFlavor);
    newSubtitleTrack.setURI(subtitleFile);
    newSubtitleTrack.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, workspace.get(subtitleFile, true)));
    return newSubtitleTrack;
  }

  /**
   * Saves the subtitle object into the workspace and creates a file there.
   *
   * @param webVTTSubtitle The subtitle object.
   * @param fileName The filname of the new subtitle file.
   * @return The URI of the new subtitle file.
   * @throws WorkflowOperationException when something went wrong in the parsing and saving process.
   */
  private URI saveSubtitleFileToWorkspace(WebVTTSubtitle webVTTSubtitle, String fileName)
          throws WorkflowOperationException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      WebVTTWriter writer = new WebVTTWriter();
      writer.write(webVTTSubtitle, outputStream);
      try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
        return workspace.putInCollection(COLLECTION, fileName, inputStream);
      }
    } catch (IOException e) {
      logger.error("An exception occurred while parsing and saving a subtitle file to the workspace", e);
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Loads a subtitle file from the workspace and parses it into a WebVTTSubtitle Object
   *
   * @param subtitleTrack The track we want to load
   * @return The parsed webVTTSubtitle object
   * @throws WorkflowOperationException when something went wrong in the parsing and loading process
   */
  private WebVTTSubtitle loadAndParseSubtitleFile(Track subtitleTrack) throws WorkflowOperationException {
    // Get the subtitle file from workspace
    File subtitleFile;
    try {
      subtitleFile = workspace.get(subtitleTrack.getURI());
    } catch (IOException ex) {
      throw new WorkflowOperationException("Can't read " + subtitleTrack.getURI());
    } catch (NotFoundException ex) {
      throw new WorkflowOperationException("Workspace does not contain a track " + subtitleTrack.getURI());
    }

    // Next try to parse the file into a WebVTT Object
    WebVTTSubtitle subtitle;
    try (FileInputStream fin = new FileInputStream(subtitleFile)) {
      subtitle = new WebVTTParser().parse(fin);
    } catch (Exception e) {
      throw new WorkflowOperationException("Couldn't parse subtitle file " + subtitleTrack.getURI(), e);
    }

    return subtitle;
  }

  /**
   * Shifts all timestamps of a subtitle file by a given time.
   *
   * @param time Time in milliseconds by which all timestamps shall be shifted.
   */
  public void shiftTime(WebVTTSubtitle subtitleFile, long time) {
    for (WebVTTSubtitleCue cue : subtitleFile.getCues()) {
      long start = cue.getStartTime();
      long end = cue.getEndTime();
      cue.setStartTime(start + time);
      cue.setEndTime(end + time);
    }
  }

}
