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

  /** Configuration key for video resolutions to check */
  static final String OPT_VIDEO_RES_X = "xresolution";
  static final String OPT_VIDEO_RES_Y = "yresolution";

  /** Configuration key for video aspect ratio to check */
  static final String OPT_VIDEO_ASPECT = "aspect-ratio";
  static final String OPT_VIDEO_ASPECT_SNAP = "snap-to-aspect";

  /** The logging facility */
  private static final Logger logger = LoggerFactory
          .getLogger(AnalyzeTracksWorkflowOperationHandler.class);

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    logger.info("Running analyze-tracks workflow operation on workflow {}", workflowInstance.getId());
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final String sourceFlavor = getConfig(workflowInstance, OPT_SOURCE_FLAVOR);

    final Track[] tracks = mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(sourceFlavor));
    if (tracks.length <= 0) {
      logger.info("No tracks with specified flavors ({}) to analyse.", sourceFlavor);
      return createResult(mediaPackage, Action.CONTINUE);
    }

    List<Integer> xresolutions = getResolutions(getConfig(workflowInstance, OPT_VIDEO_RES_X, ""));
    List<Integer> yresolutions = getResolutions(getConfig(workflowInstance, OPT_VIDEO_RES_Y, ""));
    List<Fraction> aspectRatios = getAspectRatio(getConfig(workflowInstance, OPT_VIDEO_ASPECT, ""));
    boolean snapToAspect = BooleanUtils.toBoolean(getConfig(workflowInstance, OPT_VIDEO_ASPECT_SNAP, "false"));

    Map<String, String> properties = new HashMap<String, String>();
    for (Track track : tracks) {
      final String varName = toVariableName(track.getFlavor());
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

          // Check if we should fall back to nearest defined aspect ratio
          if (snapToAspect) {
            trackAspect = getNearestAspectRatio(trackAspect, aspectRatios);
            properties.put(varName + "_aspect_snap", trackAspect.toString());
          }

          // Set boolean variables

          // Probe for resolutions
          for (int res: xresolutions) {
            final String var = varName + "_resolution_x_" + res;
            properties.put(var, Boolean.toString(video.getFrameWidth() >= res));
          }
          for (int res: yresolutions) {
            final String var = varName + "_resolution_y_" + res;
            properties.put(var, Boolean.toString(video.getFrameHeight() >= res));
          }

          // Probe for aspect ratio
          for (Fraction aspect: aspectRatios) {
            final String var = varName + "_aspect_" + aspect.toString().replace('/', '_');
            properties.put(var, Boolean.toString(aspect.equals(trackAspect)));
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
   * Get resolution to probe for from configuration string.
   *
   * @param resolutionsConfig
   *        Configuration string
   * @return List of resolutions to check
   */
  List<Integer> getResolutions(String resolutionsConfig) {
    List<Integer> resolutions = new ArrayList<>();
    for (String res: resolutionsConfig.split(" *, *")) {
      if (StringUtils.isNotBlank(res)) {
        resolutions.add(Integer.parseInt(res));
      }
    }
    return resolutions;
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
