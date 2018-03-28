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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow operation handler for analyzing tracks and set control variables.
 */
public class AnalyzeTracksWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Configuration key for the "flavor" of the tracks to use as a source input */
  static final String OPT_SOURCE_FLAVOR = "source-flavor";

  /** Configuration key for video aspect ratio to check */
  static final String OPT_VIDEO_ASPECT = "aspect-ratio";

  /** Configuration key to define behavior if no track matches */
  static final String OPT_FAIL_NO_TRACK = "fail-no-track";

  /** The logging facility */
  private static final Logger logger = LoggerFactory
          .getLogger(AnalyzeTracksWorkflowOperationHandler.class);

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    logger.info("Running analyze-tracks workflow operation on workflow {}", workflowInstance.getId());
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final String sourceFlavor = getConfig(workflowInstance, OPT_SOURCE_FLAVOR);
    Map<String, String> properties = new HashMap<>();

    final MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(sourceFlavor);
    final Track[] tracks = mediaPackage.getTracks(flavor);
    if (tracks.length <= 0) {
      if (BooleanUtils.toBoolean(getConfig(workflowInstance, OPT_FAIL_NO_TRACK, "false"))) {
        throw new WorkflowOperationException("No matching tracks for flavor " + sourceFlavor);
      }
      logger.info("No tracks with specified flavors ({}) to analyse.", sourceFlavor);
      return createResult(mediaPackage, properties, Action.CONTINUE, 0);
    }

    List<Fraction> aspectRatios = getAspectRatio(getConfig(workflowInstance, OPT_VIDEO_ASPECT, ""));

    for (Track track : tracks) {
      final String varName = toVariableName(track.getFlavor());
      properties.put(varName + "_media", "true");
      properties.put(varName + "_video", Boolean.toString(track.hasVideo()));
      properties.put(varName + "_audio", Boolean.toString(track.hasAudio()));

      // Check resolution
      if (track.hasVideo()) {
        for (VideoStream video: ((TrackImpl) track).getVideo()) {
          // Set resolution variables
          properties.put(varName + "_resolution_x", video.getFrameWidth().toString());
          properties.put(varName + "_resolution_y", video.getFrameHeight().toString());
          Fraction trackAspect = Fraction.getReducedFraction(video.getFrameWidth(), video.getFrameHeight());
          properties.put(varName + "_aspect", trackAspect.toString());
          properties.put(varName + "_framerate", video.getFrameRate().toString());

          // Check if we should fall back to nearest defined aspect ratio
          if (!aspectRatios.isEmpty()) {
            trackAspect = getNearestAspectRatio(trackAspect, aspectRatios);
            properties.put(varName + "_aspect_snap", trackAspect.toString());
          }
        }
      }
    }
    logger.info("Finished analyze-tracks workflow operation adding the properties: {}", properties);
    return createResult(mediaPackage, properties, Action.CONTINUE, 0);
  }

  /**
   * Get nearest aspect ratio from list
   *
   * @param videoAspect
   *        Aspect ratio of video to check
   * @param aspects
   *        List of aspect ratios to snap to.
   * @return Nearest aspect ratio
   */
  Fraction getNearestAspectRatio(final Fraction videoAspect, final List<Fraction> aspects) {
    Fraction nearestAspect = aspects.get(0);
    for (Fraction aspect: aspects) {
      if (videoAspect.subtract(nearestAspect).abs().compareTo(videoAspect.subtract(aspect).abs()) > 0) {
        nearestAspect = aspect;
      }
    }
    return nearestAspect;
  }

  /**
   * Get aspect ratios to check from configuration string.
   *
   * @param aspectConfig
   *        Configuration string
   * @return List of aspect rations to check
   */
  List<Fraction> getAspectRatio(String aspectConfig) {
    List<Fraction> aspectRatios = new ArrayList<>();
    for (String aspect: aspectConfig.split(" *, *")) {
      if (StringUtils.isNotBlank(aspect)) {
        aspectRatios.add(Fraction.getFraction(aspect).reduce());
      }
    }
    return aspectRatios;
  }

  /** Create a name for a workflow variable from a flavor */
  private static String toVariableName(final MediaPackageElementFlavor flavor) {
    return flavor.getType() + "_" + flavor.getSubtype();
  }
}
