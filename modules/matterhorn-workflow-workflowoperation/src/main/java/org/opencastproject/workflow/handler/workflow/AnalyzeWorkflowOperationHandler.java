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

import static java.lang.String.format;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation handler for analyzing tracks and set control variables.
 */
public class AnalyzeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Configuration key for the "flavor" of the tracks to use as a source input */
  public static final String OPT_SOURCE_FLAVOR = "source-flavor";

  /** The configuration options for this handler */
  public static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(OPT_SOURCE_FLAVOR, "The \"flavor\" of the tracks to use as a source input");
  }

  /** The logging facility */
  private static final Logger logger = LoggerFactory
          .getLogger(AnalyzeWorkflowOperationHandler.class);

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    logger.info("Running analyze workflow operation on workflow {}", workflowInstance.getId());
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final String sourceFlavorName = getConfig(workflowInstance, OPT_SOURCE_FLAVOR);
    final MediaPackageElementFlavor sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorName);

    final Track[] tracks = mediaPackage.getTracks(sourceFlavor);
    if (tracks.length > 0) {
      Map<String, String> properties = new HashMap<String, String>();
      for (Track track : tracks) {
        final String varName = toVariableName(track.getFlavor());
        properties.put(varName + "_video", Boolean.toString(track.hasVideo()));
        properties.put(varName + "_audio", Boolean.toString(track.hasAudio()));
      }
      logger.info("Finished analyze workflow operation adding the properties: {}",
                  propertiesAsString(properties));
      return createResult(mediaPackage, properties, Action.CONTINUE, 0);
    } else {
      return fail(format("Invalid media package: Does not contain any tracks matching flavor %s", sourceFlavor));
    }
  }

  /** Create a name for a workflow variable from a flavor */
  private static String toVariableName(final MediaPackageElementFlavor flavor) {
    return flavor.getType() + "_" + flavor.getSubtype();
  }

  /** Log a warning and fail by throwing a {@link org.opencastproject.workflow.api.WorkflowOperationException}. */
  private static <A> A fail(String msg) throws WorkflowOperationException {
    logger.warn(msg);
    throw new WorkflowOperationException(msg);
  }

  /** Serialize a properties map into string. */
  private String propertiesAsString(Map<String, String> map) {
    Properties prop = new Properties();
    prop.putAll(map);
    StringWriter writer = new StringWriter();
    prop.list(new PrintWriter(writer));
    return writer.getBuffer().toString();
  }
}
