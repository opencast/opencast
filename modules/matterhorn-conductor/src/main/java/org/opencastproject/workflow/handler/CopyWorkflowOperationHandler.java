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

import static java.lang.String.format;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation handler for copying video data through NFS
 */
public class CopyWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Configuration key for the \"tag\" of the track to use as a source input */
  public static final String OPT_SOURCE_TAGS = "source-tags";

  /** Configuration key for the \"flavor\" of the track to use as a source input */
  public static final String OPT_SOURCE_FLAVORS = "source-flavors";

  /** Configuration key for the directory where the file must be delivered */
  public static final String OPT_TARGET_DIRECTORY = "target-directory";

  /** Configuration key for the name of the target file */
  public static final String OPT_TARGET_FILENAME = "target-filename";

  /** The configuration options for this handler */
  public static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(OPT_SOURCE_TAGS, "The \"tag\" of the track to use as a source input");
    CONFIG_OPTIONS.put(OPT_SOURCE_FLAVORS, "The \"flavor\" of the track to use as a source input");
    CONFIG_OPTIONS.put(OPT_TARGET_DIRECTORY, "The directory where the file must be delivered");
    CONFIG_OPTIONS.put(OPT_TARGET_FILENAME, "The optional name of the target file");
  }

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CopyWorkflowOperationHandler.class);

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
   *
   * @see org.opencastproject.workflow.handler.ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running copy workflow operation on workflow {}", workflowInstance.getId());

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    String sourceTagsOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_TAGS));
    String sourceFlavorsOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_FLAVORS));
    String targetDirectoryOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_TARGET_DIRECTORY));
    Option<String> targetFilenameOption = Option.option(StringUtils.trimToNull(currentOperation
            .getConfiguration(OPT_TARGET_FILENAME)));

    StringBuilder sb = new StringBuilder();
    sb.append("Parameters passed to copy workflow operation:");
    sb.append("\n source-tags: ").append(sourceTagsOption);
    sb.append("\n source-flavors: ").append(sourceFlavorsOption);
    sb.append("\n target-directory: ").append(targetDirectoryOption);
    sb.append("\n target-filename: ").append(targetFilenameOption);
    logger.debug(sb.toString());

    AbstractMediaPackageElementSelector<MediaPackageElement> elementSelector = new SimpleElementSelector();

    // Make sure either one of tags or flavors are provided
    if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorsOption)) {
      logger.info("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Make the target filename and directory are provided
    if (StringUtils.isBlank(targetDirectoryOption))
      throw new WorkflowOperationException("No target directory has been set for the copy operation!");

    // Select the source flavors
    for (String flavor : asList(sourceFlavorsOption)) {
      try {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Source flavor '" + flavor + "' is malformed");
      }
    }

    // Select the source tags
    for (String tag : asList(sourceTagsOption)) {
      elementSelector.addTag(tag);
    }

    // Look for elements matching the tag
    Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, true);

    // Check the the number of element returned
    if (elements.size() == 0) {
      // If no one found, we skip the operation
      return createResult(workflowInstance.getMediaPackage(), Action.SKIP);
    } else if (elements.size() == 1) {
      for (MediaPackageElement element : elements) {
        logger.debug("Copy single element to: {}", targetDirectoryOption);
        final String fileName;
        if (targetFilenameOption.isSome()) {
          fileName = targetFilenameOption.get();
        } else {
          fileName = FilenameUtils.getBaseName(element.getURI().toString());
        }

        String ext = FilenameUtils.getExtension(element.getURI().toString());
        ext = ext.length() > 0 ? ".".concat(ext) : "";
        File targetFile = new File(UrlSupport.concat(targetDirectoryOption, fileName.concat(ext)));

        copyElement(element, targetFile);
      }
    } else {
      logger.debug("Copy multiple elements to: {}", targetDirectoryOption);
      int i = 1;
      for (MediaPackageElement element : elements) {
        final String fileName;
        if (targetFilenameOption.isSome()) {
          fileName = String.format(targetFilenameOption.get(), i);
        } else {
          fileName = FilenameUtils.getBaseName(element.getURI().toString());
        }

        String ext = FilenameUtils.getExtension(element.getURI().toString());
        ext = ext.length() > 0 ? ".".concat(ext) : "";
        File targetFile = new File(UrlSupport.concat(targetDirectoryOption, fileName + ext));

        copyElement(element, targetFile);
        i++;
      }
    }

    return createResult(workflowInstance.getMediaPackage(), Action.CONTINUE);
  }

  private void copyElement(MediaPackageElement element, File targetFile) throws WorkflowOperationException {
    File sourceFile;
    try {
      sourceFile = workspace.get(element.getURI());
    } catch (NotFoundException e) {
      throw new WorkflowOperationException("Unable to find " + element.getURI() + " in the workspace", e);
    } catch (IOException e) {
      throw new WorkflowOperationException("Error loading " + element.getURI() + " from the workspace", e);
    }

    logger.debug("Start copying element {} to target {}.", sourceFile.getPath(), targetFile.getPath());
    try {
      FileSupport.copy(sourceFile, targetFile);
    } catch (IOException e) {
      throw new WorkflowOperationException(format("Unable to copy %s to %s: %s", sourceFile.getPath(),
              targetFile.getPath(), e.getMessage()));
    }
    logger.debug("Element {} copied to target {}.", sourceFile.getPath(), targetFile.getPath());
  }

}
