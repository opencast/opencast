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

import java.io.InputStream;
import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.io.IOUtils;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.videoeditor.silencedetection.api.SilenceDetectionService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * workflowoperationhandler for silencedetection executes the silencedetection
 * and adds a SMIL document to the mediapackage containing the cutting points
 */
public class SilenceDetectionWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectionWorkflowOperationHandler.class);
  /**
   * Name of the configuration option that provides the source flavors we are
   * looking for
   */
  private static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";
  /**
   * Name of the configuration option that provides the target flavor we will
   * produce
   */
  private static final String TARGET_FLAVOR_PROPERTY = "target-flavor";
  /**
   * Name of the configuration option that provides the smil file name
   */
  private static final String TARGET_FILE_NAME = "smil.smil";
  /**
   * The configuration options for this handler
   */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_FLAVOR_PROPERTY, "The flavor for source files (tracks containing audio stream).");
    CONFIG_OPTIONS.put(TARGET_FLAVOR_PROPERTY, "The flavor for smil files.");
  }
  /**
   * The silence detection service.
   */
  private SilenceDetectionService detetionService;
  /**
   * The smil service for smil parsing.
   */
  private SmilService smilService;
  private Workspace workspace;

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mp = workflowInstance.getMediaPackage();
    String flavor = workflowInstance.getCurrentOperation().getConfiguration(SOURCE_FLAVOR_PROPERTY);
    Track[] tracks = mp.getTracks(MediaPackageElementFlavor.parseFlavor(flavor));

    if (tracks.length > 0) {
      try {
        Track t = tracks[0];
        logger.info("Executing silence detection on track: {}", t.getURI());
        Job detectionJob = detetionService.detect(t);
        if (!waitForStatus(detectionJob).isSuccess()) {
          throw new WorkflowOperationException("Silence Detection failed!");
        }
        Smil smil = smilService.fromXml(detectionJob.getPayload()).getSmil();
        String targetFlavor = workflowInstance.getCurrentOperation().getConfiguration(TARGET_FLAVOR_PROPERTY);

        InputStream is = null;
        try {
          is = IOUtils.toInputStream(smil.toXML(), "UTF-8");
          URI smilURI = workspace.put(mp.getIdentifier().compact(), smil.getId(), TARGET_FILE_NAME, is);
          Catalog catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                  .elementFromURI(smilURI, MediaPackageElement.Type.Catalog, MediaPackageElementFlavor.parseFlavor(targetFlavor));
          catalog.setIdentifier(smil.getId());
          mp.add(catalog);
        } finally {
          IOUtils.closeQuietly(is);
        }

        logger.info("Finished silence detection on track: {}", t.getURI());

      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        throw new WorkflowOperationException(e);
      }
    } else {
      throw new WorkflowOperationException("MediaPackage does not contain any Tracks with flavor " + flavor);
    }

    return createResult(mp, Action.CONTINUE);
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering silence detection workflow operation handler");
  }

  public void setDetectionService(SilenceDetectionService detectionService) {
    this.detetionService = detectionService;
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
