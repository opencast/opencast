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

    try {

      String command = "curl -X POST -i --digest -H \"Content-Type: applications/json\" "
              + "-u 'admin:ncast' "
              + agentURL + "rest/operations/remove_recording"
              + " -d '{\"filename\":\"" + trackname + "\"}'";
      logger.info("Commandline: \n " + command);

      Process p = null;
      int result = 0;

      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectErrorStream(true);

      p = pb.start();
      result = p.waitFor();

//      Process process = Runtime.getRuntime().exec(command);
//      process.waitFor();
//      logger.info("command result: " + process.exitValue());
//      BufferedReader reader =
//              new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//      String output="";
//      String line = "";
//      while ((line = reader.readLine())!= null) {
//        output += line + "\n";
//      }
//     logger.info("Output" + output);

    //send delete
//    HttpPost post = new HttpPost(URI.create(agentURL + "rest/operations/remove_recording"));
//    Gson gson = new Gson();
//    String jsonString = "{'filename':'" + trackname + "'}";
//
//
//
//    StringEntity postingString = new StringEntity(jsonString);
//    post.setEntity(postingString);
//    post.setHeader("Content-type", "applications/json");
//
//    TrustedHttpClient httpClientStandAlone;
//
//    httpClientStandAlone = new StandAloneTrustedHttpClientImpl("admin","ncast", Option.none(),Option.none(),Option.none());
//    logger.info("Removing File: %s ", post.getURI().getPath().toString());
//
//      HttpResponse response = httpClientStandAlone.execute(post);
//      logger.info("HTTP Response %i", response.getStatusLine().getStatusCode());

    }
    catch (Exception e) {
      logger.error("httpclient error %s",e);
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }
}
