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
package org.opencastproject.workflow.handler.assetmanager;

import static org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.Publication;
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

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Workflow operation for taking a snapshot of a media package.
 *
 * @see AssetManager#takeSnapshot(String, MediaPackage)
 */
@Component(
    immediate = true,
    name = "org.opencastproject.workflow.handler.assetmanager.AssetManagerAddWorkflowOperationHandler",
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Asset Manager Take Snapshot Workflow Operation Handler",
        "workflow.operation=snapshot"
    }
)
public class AssetManagerSnapshotWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerSnapshotWorkflowOperationHandler.class);

  /** The asset manager. */
  private AssetManager assetManager;

  @Activate
  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
  }

  /** OSGi DI */
  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance wi, JobContext ctx)
          throws WorkflowOperationException {
    final MediaPackage mpWorkflow = wi.getMediaPackage();
    final WorkflowOperationInstance currentOperation = wi.getCurrentOperation();

    // Check which tags have been configured
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(wi,
        Configuration.many, Configuration.many, Configuration.none, Configuration.none);
    List<String> sourceTagsOption = tagsAndFlavors.getSrcTags();
    List<MediaPackageElementFlavor> sourceFlavorsOption = tagsAndFlavors.getSrcFlavors();


    if (sourceTagsOption.isEmpty() && sourceFlavorsOption.isEmpty()) {
      logger.debug("No source tags have been specified, so everything will be added to the AssetManager");
    }

    final List<String> tagSet;
    // If a set of tags has been specified, use it
    if (!sourceTagsOption.isEmpty()) {
      tagSet = sourceTagsOption;
    } else {
      tagSet = new ArrayList<>();
    }

    try {
      final MediaPackage mpAssetManager = getMediaPackageForArchival(mpWorkflow, tagSet, sourceFlavorsOption);
      if (mpAssetManager != null) {
        logger.info("Take snapshot of media package {}", mpAssetManager);
        // adding media package to the episode service
        assetManager.takeSnapshot(DEFAULT_OWNER, mpAssetManager);
        logger.debug("Snapshot operation complete");
        return createResult(mpWorkflow, Action.CONTINUE);
      } else {
        return createResult(mpWorkflow, Action.CONTINUE);
      }
    } catch (Throwable t) {
      throw new WorkflowOperationException(t);
    }
  }

  protected MediaPackage getMediaPackageForArchival(MediaPackage current, List<String> tags,
                                                    List<MediaPackageElementFlavor> sourceFlavors)
          throws MediaPackageException {
    MediaPackage mp = (MediaPackage) current.clone();

    Collection<MediaPackageElement> keep;

    if (tags.isEmpty() && sourceFlavors.isEmpty()) {
      keep = new ArrayList<>(Arrays.asList(current.getElementsByTags(tags)));
    } else {
      SimpleElementSelector simpleElementSelector = new SimpleElementSelector();
      for (MediaPackageElementFlavor flavor : sourceFlavors) {
        simpleElementSelector.addFlavor(flavor);
      }
      for (String tag : tags) {
        simpleElementSelector.addTag(tag);
      }
      keep = simpleElementSelector.select(current, false);
    }

    // Also archive the publication elements
    for (Publication publication : current.getPublications()) {
      keep.add(publication);
    }

    // Mark everything that is set for removal
    List<MediaPackageElement> removals = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : mp.getElements()) {
      if (!keep.contains(element)) {
        removals.add(element);
      }
    }

    // Fix references and flavors
    for (MediaPackageElement element : mp.getElements()) {

      if (removals.contains(element)) {
        continue;
      }

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference != null) {
        MediaPackageElement referencedElement = mp.getElementByReference(reference);

        // if we are distributing the referenced element, everything is fine. Otherwise...
        if (referencedElement != null && removals.contains(referencedElement)) {

          // Follow the references until we find a flavor
          MediaPackageElement parent;
          while ((parent = current.getElementByReference(reference)) != null) {
            if (parent.getFlavor() != null && element.getFlavor() == null) {
              element.setFlavor(parent.getFlavor());
            }
            if (parent.getReference() == null) {
              break;
            }
            reference = parent.getReference();
          }

          // Done. Let's cut the path but keep references to the mediapackage itself
          if (reference != null && reference.getType().equals(MediaPackageReference.TYPE_MEDIAPACKAGE)) {
            element.setReference(reference);
          } else {
            element.clearReference();
          }
        }
      }
    }

    // Remove everything we don't want to add to publish
    for (MediaPackageElement element : removals) {
      mp.remove(element);
    }
    return mp;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
