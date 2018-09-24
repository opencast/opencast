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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.selector.AttachmentSelector;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ImageConvertWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(ImageConvertWorkflowOperationHandler.class);

  /** Configuration key for the source flavor */
  private static final String CONFIG_KEY_SOURCE_FLAVOR = "source-flavor";
  /** Configuration key for source flavors (comma seperated values) */
  private static final String CONFIG_KEY_SOURCE_FLAVORS = "source-flavors";
  /** Configuration key for source tags (comma separated values) */
  private static final String CONFIG_KEY_SOURCE_TAGS = "source-tags";
  /** Configuration key for the target flavor */
  private static final String CONFIG_KEY_TARGET_FLAVOR = "target-flavor";
  /** Configuration key for target flavor. The value of this configuration will be used, if target-flavor isn't set */
  private static final String CONFIG_KEY_TARGET_FLAVORS = "target-flavors";
  /** Configuration key for target tags */
  private static final String CONFIG_KEY_TARGET_TAGS = "target-tags";
  /** Configuration key for encoding profile */
  private static final String CONFIG_KEY_ENCODING_PROFILE = "encoding-profile";
  /** Configuration key for encoding profile. The value of this configuration will be used,
   * if encoding-profile isn't set  */
  private static final String CONFIG_KEY_ENCODING_PROFILES = "encoding-profiles";
  /** Boolean configuration key for value, wether to use flavors and tags for selection of the source
   *  attachments (set to true) or flavors or tags (set to false) */
  private static final String CONFIG_KEY_TAGS_AND_FLAVORS = "tags-and-flavors";


  /** The composer service */
  private ComposerService composerService = null;

  /** The workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param composerService
   *          the composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    String sourceFlavorOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_SOURCE_FLAVOR));
    String sourceFlavorsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_SOURCE_FLAVORS));
    String sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_SOURCE_TAGS));
    String targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TARGET_FLAVOR));
    if (targetFlavorOption == null)
      targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TARGET_FLAVORS));
    String targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TARGET_TAGS));
    String encodingProfileOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_ENCODING_PROFILE));
    if (encodingProfileOption == null)
      encodingProfileOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_ENCODING_PROFILES));
    String tagsAndFlavorsOption = StringUtils.trimToNull(operation.getConfiguration(CONFIG_KEY_TAGS_AND_FLAVORS));
    boolean tagsAndFlavors = BooleanUtils.toBoolean(tagsAndFlavorsOption);


    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Make sure either one of tags or flavors are provided
    if (StringUtils.isBlank(sourceFlavorOption) && StringUtils.isBlank(sourceFlavorsOption)
            && StringUtils.isBlank(sourceTagsOption)) {
      logger.info("No source tags or flavors have been specified, not matching anything");
      return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
    }

    // Target flavor
    MediaPackageElementFlavor targetFlavor = null;
    if (StringUtils.isNotBlank(targetFlavorOption)) {
      try {
        targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Target flavor '" + targetFlavorOption + "' is malformed");
      }
    }
    // check target-tags configuration
    List<String> fixedTags = new ArrayList<>();
    List<String> additionalTags = new ArrayList<>();
    List<String> removingTags = new ArrayList<>();
    for (String targetTag : asList(targetTagsOption)) {
      if (!StringUtils.startsWithAny(targetTag, "+", "-")) {
        if (additionalTags.size() > 0 || removingTags.size() > 0) {
          logger.warn("You may not mix fixed tags and tag changes. "
                  + "Please review target-tags option on image-convert operation of your workflow definition. "
                  + "The tag {} is not prefixed with '+' or '-'.", targetTag);
        }
        fixedTags.add(targetTag);
      } else if (StringUtils.startsWith(targetTag, "+")) {
        additionalTags.add(StringUtils.substring(targetTag, 1));
      } else if (StringUtils.startsWith(targetTag, "-")) {
        removingTags.add(StringUtils.substring(targetTag, 1));
      }
    }

    List<String> profiles = new ArrayList<>();
    for (String encodingProfileId : asList(encodingProfileOption)) {
      EncodingProfile profile = composerService.getProfile(encodingProfileId);
      if (profile == null)
        throw new WorkflowOperationException("Encoding profile '" + encodingProfileId + "' was not found");
      // just test if the profile exists, we only need the profile id for further work
      profiles.add(encodingProfileId);
    }

    // Make sure there is at least one profile
    if (profiles.isEmpty())
      throw new WorkflowOperationException("No encoding profile was specified");

    AttachmentSelector attachmentSelector = new AttachmentSelector();
    for (String sourceFlavor : asList(sourceFlavorsOption)) {
      attachmentSelector.addFlavor(sourceFlavor);
    }
    for (String sourceFlavor : asList(sourceFlavorOption)) {
      attachmentSelector.addFlavor(sourceFlavor);
    }
    for (String sourceTag : asList(sourceTagsOption)) {
      attachmentSelector.addTag(sourceTag);
    }

    // Look for elements matching the tag
    Collection<Attachment> sourceElements = attachmentSelector.select(mediaPackage, tagsAndFlavors);

    Map<Job, Attachment> jobs = new Hashtable<>();
    try {
      for (Attachment sourceElement : sourceElements) {
        Job job = composerService.convertImage(sourceElement, profiles.toArray(new String[profiles.size()]));
        jobs.put(job, sourceElement);
      }
      if (!waitForStatus(jobs.keySet().toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("At least one image conversation job did not succeed.");
      }
      for (Map.Entry<Job, Attachment> jobEntry : jobs.entrySet()) {
        Job job = jobEntry.getKey();
        Attachment sourceElement = jobEntry.getValue();
        List<Attachment> targetElements =
                (List<Attachment>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
        for (Attachment targetElement : targetElements) {
          String targetFileName = PathSupport.toSafeName(FilenameUtils.getName(targetElement.getURI().getPath()));
          URI newTargetElementUri = workspace.moveTo(targetElement.getURI(), mediaPackage.getIdentifier().compact(),
                  targetElement.getIdentifier(), targetFileName);
          targetElement.setURI(newTargetElementUri);
          targetElement.setChecksum(null);

          // set flavor on target element
          if (targetFlavor != null) {
            targetElement.setFlavor(targetFlavor);
            if (StringUtils.equalsAny("*", targetFlavor.getType())) {
              targetElement.setFlavor(MediaPackageElementFlavor.flavor(
                      sourceElement.getFlavor().getType(), targetElement.getFlavor().getSubtype()));
            }
            if (StringUtils.equalsAny("*", targetFlavor.getSubtype())) {
              targetElement.setFlavor(MediaPackageElementFlavor.flavor(
                      targetElement.getFlavor().getType(), sourceElement.getFlavor().getSubtype()));
            }
          }
          // set tags on target element
          targetElement.clearTags();
          if (fixedTags.isEmpty() && (!additionalTags.isEmpty() || !removingTags.isEmpty())) {
            for (String tag : sourceElement.getTags()) {
              targetElement.addTag(tag);
            }
          }
          for (String targetTag : fixedTags) {
            targetElement.addTag(targetTag);
          }
          for (String additionalTag : additionalTags) {
            targetElement.addTag(additionalTag);
          }
          for (String removingTag : removingTags) {
            targetElement.removeTag(removingTag);
          }
          mediaPackage.addDerived(targetElement, sourceElement);
        }
      }
      return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE);
    } catch (WorkflowOperationException ex) {
      throw ex;
    } catch (Throwable t) {
      throw new WorkflowOperationException("Convert image operation failed", t);
    } finally {
      cleanupWorkspace(jobs.keySet());
    }
  }

  private void cleanupWorkspace(Collection<Job> jobs) {
    for (Job job : jobs) {
      try {
        List<Attachment> targetElements =
                (List<Attachment>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
        for (Attachment targetElement : targetElements) {
          try {
            workspace.delete(targetElement.getURI());
          } catch (NotFoundException ex) {
            logger.trace("The image file {} not found", targetElement, ex);
          } catch (IOException ex) {
            logger.warn("Unable to delete image file {} from workspace", targetElement, ex);
          }
        }
      } catch (MediaPackageException ex) {
        logger.debug("Unable to parse job payload from job {}", job.getId(), ex);
      }
    }
  }
}
