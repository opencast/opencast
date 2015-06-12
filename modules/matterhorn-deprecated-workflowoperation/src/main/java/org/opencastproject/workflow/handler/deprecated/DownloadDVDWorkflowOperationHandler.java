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

package org.opencastproject.workflow.handler.deprecated;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase;

/**
 * Operation that holds for download of DVD image
 */
public class DownloadDVDWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory.getLogger(DownloadDVDWorkflowOperationHandler.class);

  /** Path to the hold ui resources */
  private static final String HOLD_UI_PATH = "/ui/operation/download-dvd/index.html";
  private static final String ACTION_TITLE = "download DVD";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
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

  public void activate(ComponentContext cc) {
    super.activate(cc);
    registerHoldStateUserInterface(HOLD_UI_PATH);
    setHoldActionTitle(ACTION_TITLE);
    logger.info("Registering download-DVD hold state ui from classpath {}", HOLD_UI_PATH);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
    logger.info("Holding for download of DVD image...");
    return createResult(Action.PAUSE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context, Map<String, String> properties)
          throws WorkflowOperationException {
    // remove the DVD encoded track from the mediaPackage
    try {
      MediaPackage mp = workflowInstance.getMediaPackage();
      Track[] tracks = mp.getTracks();
      for (int i = 0; i < tracks.length; i++) {
        if ("DVD".equalsIgnoreCase(tracks[i].getFlavor().getSubtype())) {
          logger.info("Deleting {} from MediaPackage {}", tracks[i].toString(), mp.getIdentifier().toString());
          mp.remove(tracks[i]);
        }
      }
    } catch (Exception e) {
      logger.warn("Could not remove dvd encoded track - {}", e.getMessage());
    }
    return createResult(Action.CONTINUE);
  }
}
