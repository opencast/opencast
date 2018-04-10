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
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;

/**
 * Workflow operation handler for cloning tracks from a flavor
 */
public class CloneWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Configuration key for the \"source-flavor\" of the track to use as a source input */
  public static final String OPT_SOURCE_FLAVOR = "source-flavor";

  /** Configuration key for the \"source-tag\" of the track to use as a source input */
  public static final String OPT_SOURCE_TAGS = "source-tags";

  /** Configuration key for the target-flavor */
  public static final String OPT_TARGET_FLAVOR = "target-flavor";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CloneWorkflowOperationHandler.class);

  /** The workspace reference */
  protected Workspace workspace = null;

  /**
   * Callback for the OSGi environment to set the workspace reference.
   *
   * @param workspace
   *          the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running clone workflow operation on workflow {}", workflowInstance.getId());

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    String sourceTagsOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_TAGS));
    String sourceFlavorOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_FLAVOR));
    String targetFlavorOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_TARGET_FLAVOR));

    AbstractMediaPackageElementSelector<MediaPackageElement> elementSelector = new SimpleElementSelector();

    // Make sure either one of tags or flavors are provided
    if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorOption)) {
      logger.info("No source tags or flavors have been specified, not matching anything. Operation will be skipped.");
      return createResult(mediaPackage, Action.SKIP);
    }

    // if no source-favor is specified, all flavors will be checked for given tags
    if (sourceFlavorOption == null) {
      sourceFlavorOption = "*/*";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Parameters passed to clone workflow operation:");
    sb.append("\n source-tags: ").append(sourceTagsOption);
    sb.append("\n source-flavor: ").append(sourceFlavorOption);
    sb.append("\n target-flavor: ").append(targetFlavorOption);
    logger.debug(sb.toString());

    // Select the source flavors
    MediaPackageElementFlavor sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorOption);
    elementSelector.addFlavor(sourceFlavor);

    // Select the source tags
    for (String tag : asList(sourceTagsOption)) {
      elementSelector.addTag(tag);
    }

    // Look for elements matching the tags and the flavor
    Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, true);

    // Check the the number of element returned
    if (elements.size() == 0) {
      // If no one found, we skip the operation
      logger.debug("No matching elements found, skipping operation.");
      return createResult(workflowInstance.getMediaPackage(), Action.SKIP);
    } else {
      logger.debug("Copy " + elements.size() + " elements to new flavor: {}", targetFlavorOption);

      MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption);

      for (MediaPackageElement element : elements) {
        // apply the target flavor to the element
        MediaPackageElementFlavor flavor = targetFlavor.applyTo(element.getFlavor());

        // Copy element and set new flavor
        MediaPackageElement newElement = copyElement(element);
        newElement.setFlavor(flavor);
        mediaPackage.add(newElement);
      }
    }

    return createResult(workflowInstance.getMediaPackage(), Action.CONTINUE);
  }

  private MediaPackageElement copyElement(MediaPackageElement element) throws WorkflowOperationException {
    String elementId = UUID.randomUUID().toString();
    MediaPackageElement newElement = (MediaPackageElement) element.clone();
    newElement.setIdentifier(elementId);

    File sourceFile = null;
    String toFileName = null;
    try {
      URI sourceURI = element.getURI();
      sourceFile = workspace.get(sourceURI);

      toFileName = elementId;
      String extension = FilenameUtils.getExtension(sourceFile.getName());
      if (!"".equals(extension))
        toFileName += "." + extension;

      logger.debug("Start copying element {} to target {}.", sourceFile.getPath(), toFileName);

      URI newUri = workspace.put(element.getMediaPackage().getIdentifier().toString(), newElement.getIdentifier(),
              toFileName, workspace.read(sourceURI));
      newElement.setURI(newUri);
      newElement.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, workspace.get(newUri)));

      logger.debug("Element {} copied to target {}.", sourceFile.getPath(), toFileName);
    } catch (IOException e) {
      throw new WorkflowOperationException("Unable to copy " + sourceFile.getPath() + " to " + toFileName + ".", e);
    } catch (NotFoundException e) {
      throw new WorkflowOperationException("Unable to find " + element.getURI() + " in the workspace", e);
    }

    return newElement;
  }

}
