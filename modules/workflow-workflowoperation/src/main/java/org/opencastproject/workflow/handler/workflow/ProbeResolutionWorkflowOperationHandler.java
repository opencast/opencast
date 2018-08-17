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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Workflow operation handler for setting variables based on video resolutions
 */

public class ProbeResolutionWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Configuration key for the "flavor" of the tracks to use as a source input */
  static final String OPT_SOURCE_FLAVOR = "source-flavor";

  /** Configuration key for video resolutions to check */
  static final String OPT_VAR_PREFIX = "var:";

  /** Configuration key for value to set */
  static final String OPT_VAL_PREFIX = "val:";

  /** The logging facility */
  private static final Logger logger = LoggerFactory
          .getLogger(ProbeResolutionWorkflowOperationHandler.class);

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    logger.info("Running probe-resolution workflow operation");
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final String sourceFlavorName = getConfig(workflowInstance, OPT_SOURCE_FLAVOR);
    final MediaPackageElementFlavor sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorName);

    // Ensure we have a matching track
    final Track[] tracks = mediaPackage.getTracks(sourceFlavor);
    if (tracks.length <= 0) {
      logger.info("No tracks with specified flavor ({}).", sourceFlavorName);
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Create mapping:  resolution -> [varNames]
    Map<Fraction, Set<String>> resolutionMapping = new HashMap<>();
    for (String key: workflowInstance.getCurrentOperation().getConfigurationKeys()) {
      if (key.startsWith(OPT_VAR_PREFIX)) {
        String varName = key.substring(OPT_VAR_PREFIX.length());
        for (Fraction resolution: getResolutions(getConfig(workflowInstance, key))) {
          if (!resolutionMapping.containsKey(resolution)) {
            resolutionMapping.put(resolution, new HashSet<String>());
          }
          resolutionMapping.get(resolution).add(varName);
        }
      }
    }

    // Create mapping:  varName -> value
    Map<String, String> valueMapping = new HashMap<>();
    for (String key: workflowInstance.getCurrentOperation().getConfigurationKeys()) {
      if (key.startsWith(OPT_VAL_PREFIX)) {
        String varName = key.substring(OPT_VAL_PREFIX.length());
        valueMapping.put(varName, getConfig(workflowInstance, key));
      }
    }

    Map<String, String> properties = new HashMap<String, String>();
    for (Track track : tracks) {
      final String flavor = toVariableName(track.getFlavor());

      // Check if resolution fits
      if (track.hasVideo()) {
        for (VideoStream video: ((TrackImpl) track).getVideo()) {
          Fraction resolution = Fraction.getFraction(video.getFrameWidth(), video.getFrameHeight());
          if (resolutionMapping.containsKey(resolution)) {
            for (String varName : resolutionMapping.get(resolution)) {
              String value = valueMapping.containsKey(varName) ? valueMapping.get(varName) : "true";
              properties.put(flavor + varName, value);
            }
          }
        }
      }
    }
    logger.info("Finished workflow operation adding the properties: {}", properties);
    return createResult(mediaPackage, properties, Action.CONTINUE, 0);
  }

  /**
   * Get resolution to probe for from configuration string.
   *
   * @param resolutionsConfig
   *        Configuration string
   * @return List of resolutions to check
   */
  List<Fraction> getResolutions(String resolutionsConfig) {
    List<Fraction> resolutions = new ArrayList<>();
    for (String res: resolutionsConfig.split(" *, *")) {
      if (StringUtils.isNotBlank(res)) {
        resolutions.add(Fraction.getFraction(res.replace('x', '/')));
      }
    }
    return resolutions;
  }

  /** Create a name for a workflow variable from a flavor */
  private static String toVariableName(final MediaPackageElementFlavor flavor) {
    return flavor.getType() + "_" + flavor.getSubtype() + "_";
  }
}
