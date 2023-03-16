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

package org.opencastproject.workflow.handler.analyzemediapackage;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Analyze Mediapackage Workflow Operation Handler set workflow properties based on media package content.
 */
@Component(
    property = {
        "service.description=Analyze Mediapackage Workflow Operation Handler",
        "workflow.operation=analyze-mediapackage"
    },
    immediate = true,
    service = WorkflowOperationHandler.class
)
public class AnalyzeMediapackageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AnalyzeMediapackageWorkflowOperationHandler.class);

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance, Configuration.many,
        Configuration.many, Configuration.none, Configuration.none);

    Collection<MediaPackageElement> elements = null;
    if (tagsAndFlavors.getSrcFlavors().isEmpty() && tagsAndFlavors.getSrcTags().isEmpty()) {
      elements = Arrays.asList(mediaPackage.getElements());
    } else {
      SimpleElementSelector mpeSelector = new SimpleElementSelector();
      for (MediaPackageElementFlavor flavor : tagsAndFlavors.getSrcFlavors()) {
        mpeSelector.addFlavor(flavor);
      }
      for (String tag : tagsAndFlavors.getSrcTags()) {
        mpeSelector.addTag(tag);
      }
      elements = mpeSelector.select(mediaPackage, false);
    }

    Map<String, String> properties = new HashMap<>();
    for (MediaPackageElement mpe : elements) {
      if (MediaPackageElement.Type.Publication == mpe.getElementType()) {
        continue;
      }
      String flavorPrefix = mpe.getFlavor().toString().replaceAll("[\/]", "_").toLowerCase();
      properties.put(flavorPrefix + "_exists", "true");
      properties.put(flavorPrefix + "_type", mpe.getElementType().toString());
    }
    return createResult(mediaPackage, properties, WorkflowOperationResult.Action.CONTINUE, 0L);
  }

  @Activate
  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Analyze Mediapackage WOH activated.");
  }
}
