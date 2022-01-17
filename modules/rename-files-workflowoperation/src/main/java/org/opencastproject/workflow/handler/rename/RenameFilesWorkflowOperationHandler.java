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
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(
   *      org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Read configuration
    // source-flavors
    // source-tags
    String namePattern = operation.getConfiguration("name-pattern");
    if (namePattern == null) {
      throw new WorkflowOperationException("name-pattern must be configured");
    }

    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
            Configuration.many, Configuration.many, Configuration.none, Configuration.none);
    // Select tracks by evaluating source tags and flavors
    List<MediaPackageElementFlavor> sourceFlavors = tagsAndFlavors.getSrcFlavors();
    // ...

    for (var flavor: sourceFlavors) {
      for (var track: mediaPackage.getTracks(flavor)) {
        // rename files in tracks
      }
    }

    // Continue with unmodified media package
    return createResult(mediaPackage, Action.CONTINUE);
  }

}
