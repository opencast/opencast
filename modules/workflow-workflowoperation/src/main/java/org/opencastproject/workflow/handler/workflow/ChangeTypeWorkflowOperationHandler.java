/*
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
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Workflow operation handler for changing the type of a mediapckage element
 */
@Component(
        immediate = true,
        service = WorkflowOperationHandler.class,
        property = {
                "service.description=Change Type Workflow Operation Handler",
                "workflow.operation=changetype"
        }
)
public class ChangeTypeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CloneWorkflowOperationHandler.class);

  public static final String OPT_SOURCE_FLAVOR = "source-flavors";
  public static final String OPT_SOURCE_TAGS = "source-tags";
  public static final String OPT_TARGET_FLAVOR = "target-flavor";
  public static final String TARGET_TYPE = "target-type";

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running change type workflow operation on workflow {}", workflowInstance.getId());

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
            Configuration.many, Configuration.many, Configuration.none, Configuration.one);
    List<String> sourceTagsOption = tagsAndFlavors.getSrcTags();
    List<MediaPackageElementFlavor> sourceFlavorOptionList = tagsAndFlavors.getSrcFlavors();
    String targetFlavorOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_TARGET_FLAVOR));
    String targetTypeOption = StringUtils.trimToEmpty(currentOperation.getConfiguration(TARGET_TYPE));
    MediaPackageElement.Type type = null;
    if (!targetTypeOption.isEmpty()) {
      // Case insensitive matching between user input (workflow config key) and enum value
      for (MediaPackageElement.Type t : MediaPackageElement.Type.values()) {
        if (t.name().equalsIgnoreCase(targetTypeOption)) {
          type = t;
        }
      }
      if (type == null) {
        throw new IllegalArgumentException(String.format("The given type '%s' for mediapackage %s was illegal. Please"
                + "check the operations' configuration keys.", type, mediaPackage.getIdentifier()));
      }
    } else {
      throw new IllegalArgumentException(String.format("The given type '%s' for mediapackage %s was illegal. Please"
              + "check the operations' configuration keys.", type, mediaPackage.getIdentifier()));
    }

    AbstractMediaPackageElementSelector<MediaPackageElement> elementSelector = new SimpleElementSelector();

    // Make sure either one of tags or flavors are provided
    if (sourceTagsOption.isEmpty() && sourceFlavorOptionList.isEmpty()) {
      logger.info("No source tags or flavors have been specified, not matching anything. Operation will be skipped.");
      return createResult(mediaPackage, Action.SKIP);
    }

    // Select the source flavors
    for (MediaPackageElementFlavor sourceFlavor : sourceFlavorOptionList) {
      elementSelector.addFlavor(sourceFlavor);
    }
    if (sourceFlavorOptionList.isEmpty()) {
      elementSelector.addFlavor(new MediaPackageElementFlavor("*","*"));
    }

    // Select the source tags
    for (String tag : sourceTagsOption) {
      elementSelector.addTag(tag);
    }

    // Look for elements matching the tags and the flavor
    Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, true);

    // Check the number of element returned
    if (elements.size() == 0) {
      // If no one found, we skip the operation
      logger.debug("No matching elements found, skipping operation.");
      return createResult(mediaPackage, Action.SKIP);
    } else {
      logger.debug("Copy" + elements.size() + " elements to new flavor: {}", targetFlavorOption);

      MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption);

      for (MediaPackageElement element : elements) {
        // apply the target flavor to the element
        MediaPackageElementFlavor flavor = targetFlavor.applyTo(element.getFlavor());

        MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
        MediaPackageElement newElement = mpeBuilder.newElement(type, flavor);
        newElement.setIdentifier(element.getIdentifier());
        newElement.setURI(element.getURI());
        newElement.setElementDescription(element.getElementDescription());
        newElement.setMimeType(element.getMimeType());
        newElement.setReference(element.getReference());
        newElement.setSize(element.getSize());
        newElement.setChecksum(element.getChecksum());
        for (String tag : element.getTags()) {
          newElement.addTag(tag);
        }

        // Copy element and set new flavor
        MediaPackageSupport.updateElement(mediaPackage, newElement);
      }
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }
}
