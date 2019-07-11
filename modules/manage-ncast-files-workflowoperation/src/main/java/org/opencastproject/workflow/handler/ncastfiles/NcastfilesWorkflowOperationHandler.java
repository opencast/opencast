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

package org.opencastproject.workflow.handler.ncastfiles;

import static org.opencastproject.mediapackage.MediaPackageElementFlavor.parseFlavor;

import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The workflow definition for handling "series" operations
 */
public class NcastfilesWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(NcastfilesWorkflowOperationHandler.class);


  /** The authorization service */
  private AuthorizationService authorizationService;

  private CaptureAgentStateService captureagenStateservice;

  /**
   * The workspace to use in retrieving and storing files.
   */
  protected Workspace workspace;


  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param authorizationService
   *          the authorization service
   */
  protected void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  protected void setCaptureAgentService(CaptureAgentStateService captureAgentStateService) {
    this.captureagenStateservice = captureAgentStateService;
  }

  /**
   * Sets the workspace to use.
   *
   * @param workspace
   *          the workspace
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
    logger.debug("Running Filename to ACL Role Workflowoperation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // get the attachment
    final String sourceFlavorName = "presenter/source";

    final MediaPackageElementFlavor sourceFlavor = parseFlavor(sourceFlavorName);
    final Track[] mediaPackageTracks = mediaPackage.getTracks(sourceFlavor);
    if (mediaPackageTracks.length == 0) {
      return createResult(mediaPackage, Action.CONTINUE);
    }
    String trackname = null;
    for (Track track : mediaPackageTracks) {
      if (track.getURI() != null) {
        // get name for the ACL from filename
        trackname = track.getURI().getPath();
        trackname = trackname.substring(trackname.lastIndexOf("/") + 1, trackname.lastIndexOf("."));
        trackname = trackname.replace("_","-");
        break;
      }
    }
    //get the Agent name
    String agentName = "";

    Opt<DublinCoreCatalog> dublinCoreCatalog = DublinCoreUtil.loadEpisodeDublinCore(this.workspace,mediaPackage);
    if (dublinCoreCatalog.isSome()) {
      agentName = dublinCoreCatalog.get().getFirst(DublinCore.PROPERTY_SPATIAL,DublinCore.LANGUAGE_ANY);
    }
    //get the agent url
    String agentURL = "";
    try {
      agentURL = captureagenStateservice.getAgent(agentName).getUrl();
    }
    catch (NotFoundException e) {
      logger.error("Agent not found %s",e);
    }

    final List<String> stderrFilter = java.util.Arrays.asList(
            "Page",
            "something went wring with the Task",
            "Try Again");

    try {

      List<String> command = new ArrayList<>();

      command.add("curl");
      command.add("-X");
      command.add("POST");
      command.add("-i");
      command.add("--digest");
      command.add("-H");
      command.add("Content-Type: applications/json");
      command.add("-u");
      command.add("admin:ncast");
      command.add((agentURL + "rest/operations/remove_recording"));
      command.add("-d");
      command.add("{\"filename\":" + "\"" +  trackname + "\"}");

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      Process ps = processBuilder.start();
      logger.info("Running Command", command);


      try (BufferedReader in = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
        String line;
        while ((line = in.readLine()) != null) {
          final String trimmedLine = line.trim();
          if (stderrFilter.parallelStream().noneMatch(trimmedLine::startsWith)) {
            logger.info(line);
          } else {
            logger.debug(line);
          }
        }
      }

      // wait until the task is finished
      int exitCode = ps.waitFor();
      if (exitCode != 0) {
        throw new Exception("Task exited abnormally with status " + exitCode);
      }

    }
    catch (Exception e) {
      logger.error("httpclient error %s",e);
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }
}
