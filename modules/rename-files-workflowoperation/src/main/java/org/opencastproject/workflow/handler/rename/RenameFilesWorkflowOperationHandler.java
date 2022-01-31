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

package org.opencastproject.workflow.handler.rename;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * The <code>RenameFilesWorkflowOperationHandler</code> will rename files referenced in tracks based on metadata
 * contained in the media package.
 */
@Component(
    property = {
        "service.description=Rename Files Workflow Operation Handler",
        "workflow.operation=rename-files"
    },
    immediate = true,
    service = WorkflowOperationHandler.class
)
public class RenameFilesWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RenameFilesWorkflowOperationHandler.class);

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(
   *      org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    final var operation = workflowInstance.getCurrentOperation();
    final var mediaPackage = workflowInstance.getMediaPackage();
    final var mediaPackageId = mediaPackage.getIdentifier().toString();

    logger.info("Running rename files workflow operation on workflow {}", workflowInstance.getId());

    // Read configuration
    // source-flavors source-tags
    String namePattern = operation.getConfiguration("name-pattern");
    if (namePattern == null) {
      throw new WorkflowOperationException("name-pattern must be configured");
    }
    logger.info("name-pattern {}", namePattern);


    var tagsAndFlavors = getTagsAndFlavors(
            workflowInstance,
            Configuration.none,  // source-tags
            Configuration.many,  // source-flavors
            Configuration.none,  // target-tags
            Configuration.none); // target-flavors

    // Select tracks by evaluating source tags and flavors
    List<MediaPackageElementFlavor> sourceFlavors = tagsAndFlavors.getSrcFlavors();

    for (var flavor: sourceFlavors) {
      for (var track: mediaPackage.getTracks(flavor)) {
        var uri = track.getURI();
        var extension = FilenameUtils.getExtension(uri.toString());
        var newElementId = UUID.randomUUID().toString();
        var filename = mediaPackage.getTitle() + '.' + extension;

        // Put updated filename in working file repository and update the track.
        // Make sure it has a new identifier to prevent conflicts with the old files.
        try (var in = workspace.read(uri)) {
          var newUri = workspace.put(mediaPackageId, newElementId, filename, in);
          logger.info("Renaming {} to {}", uri, newUri);
          track.setIdentifier(newElementId);
          track.setURI(newUri);
        } catch (NotFoundException | IOException e) {
          throw new WorkflowOperationException("Failed moving track file", e);
        }

        // Delete the old files from the working file repository and workspace if they were in there
        logger.debug("Removing old track file {}", uri);
        try {
          workspace.delete(uri);
        } catch (NotFoundException | IOException e) {
          logger.debug("Could not remove track from workspace. Could be it was never there.");
        }

      }
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }

  @Activate
  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
  }
}
