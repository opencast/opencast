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

package org.opencastproject.workflow.handler.assetmanager;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Replaces the media package in the current workflow with a previous version from the asset manager. There are two ways
 * to choose the version: by version number or a combination of source-flavors and no-tags (choose the latest version
 * where elements with source-flavor do not have the tags specified in no-tags.
 *
 * This operation should be the first one in a workflow executed from the archive because it REPLACES the media package
 * used by the current workflow.
 */
@Component(immediate = true, service = WorkflowOperationHandler.class, property = {
        "service.description=Selects a mp version from the archive", "workflow.operation=select-version" })
public class SelectVersionWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(SelectVersionWorkflowOperationHandler.class);

  public static final String OPT_VERSION = "version";
  public static final String OPT_NO_TAGS = "no-tags";
  public static final String OPT_SOURCE_FLAVORS = "source-flavors";

  private AssetManager assetManager;

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    final WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    if (currentOperation == null) {
      throw new WorkflowOperationException("Cannot get current workflow operation");
    }
    // Get current media package
    MediaPackage mp = workflowInstance.getMediaPackage();
    MediaPackage resultMp = null;

    // Make sure operation configuration is valid.
    String version = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_VERSION));
    String noTagsOpt = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_NO_TAGS));
    String sourceFlavorsOpt = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_FLAVORS));
    if (version != null && (noTagsOpt != null || sourceFlavorsOpt != null)) {
      throw new WorkflowOperationException(
              String.format("Configuration error: '%s' cannot be used with '%s' and '%s'.",
              OPT_VERSION, OPT_NO_TAGS, OPT_SOURCE_FLAVORS));
    }

    // Specific version informed? If yes, use it.
    if (version != null) {
      try {
        // Validate the number
        Integer.parseInt(version);
        resultMp = findVersion(mp.getIdentifier().toString(), version);
        if (resultMp == null) {
          throw new WorkflowOperationException(
                  String.format("Could not find version %d of mp %s in the archive", mp.getIdentifier(), version));
        }
      } catch (NumberFormatException e) {
        throw new WorkflowOperationException("Invalid version passed: " + version);
      }
    } else {
      if (noTagsOpt == null || sourceFlavorsOpt == null) {
        throw new WorkflowOperationException(String.format("Configuration error: both '%s' and '%s' must be passed.",
                OPT_NO_TAGS, OPT_SOURCE_FLAVORS));
      }
      Collection<String> noTags = Arrays.asList(noTagsOpt.split(","));

      SimpleElementSelector elementSelector = new SimpleElementSelector();
      for (MediaPackageElementFlavor flavor : parseFlavors(sourceFlavorsOpt)) {
        elementSelector.addFlavor(flavor);
      }

      resultMp = findVersionWithNoTags(mp.getIdentifier().toString(), elementSelector, noTags);
      if (resultMp == null) {
        throw new WorkflowOperationException(String.format(
                "Could not find in the archive a version of mp %s that does not have the tags %s in element flavors %s",
                mp.getIdentifier(), noTagsOpt, sourceFlavorsOpt));
      }
    }
    return createResult(resultMp, WorkflowOperationResult.Action.CONTINUE);
  }

  private MediaPackage findVersion(String mpId, String version) throws WorkflowOperationException {
    // Get the specific version from the asset manager
    AQueryBuilder q = assetManager.createQuery();

    AResult r = q.select(q.snapshot())
            .where(q.mediaPackageId(mpId).and(q.version().eq(assetManager.toVersion(version).get()))).run();

    if (r.getSize() == 0) {
      // Version not found
      throw new WorkflowOperationException(
              String.format("Media package %s, version %s not found in the archive.", mpId, version));
    }

    for (ARecord rec : r.getRecords()) {
      // There should be only one
      Optional<Snapshot> optSnap = rec.getSnapshot();
      if (optSnap.isEmpty()) {
        continue;
      }
      logger.info("Replacing current media package with version: {}", version);
      return optSnap.get().getMediaPackage();
    }
    return null;
  }

  private MediaPackage findVersionWithNoTags(String mpId, SimpleElementSelector elementSelector,
          Collection<String> tags) throws WorkflowOperationException {
    // Get all the snapshots from the asset manager
    AQueryBuilder q = assetManager.createQuery();

    AResult r = q.select(q.snapshot()).where(q.mediaPackageId(mpId)).orderBy(q.version().desc()).run();
    if (r.getSize() == 0) {
      // This is strange because it should run from the archive
      throw new WorkflowOperationException("Media package not found in the archive: " + mpId);
    }

    nextVersion: for (ARecord rec : r.getRecords()) {
      Optional<Snapshot> optSnap = rec.getSnapshot();
      if (optSnap.isEmpty()) {
        continue;
      }
      Snapshot snapshot = optSnap.get();
      MediaPackage mp = snapshot.getMediaPackage();
      for (MediaPackageElement el : elementSelector.select(mp, false)) {
        for (String t : el.getTags()) {
          if (tags.contains(t)) {
            continue nextVersion;
          }
        }
      }
      logger.info("Replacing current media package with version: {}", snapshot.getVersion());
      return mp;
    }
    return null;
  }

  private List<MediaPackageElementFlavor> parseFlavors(String flavorStr) {
    List<MediaPackageElementFlavor> flavors = new ArrayList<MediaPackageElementFlavor>();
    if (flavorStr != null) {
      for (String flavor : asList(flavorStr)) {
        flavors.add(MediaPackageElementFlavor.parseFlavor(flavor));
      }
    }
    return flavors;
  }

  @Reference
  public void setAssetManager(AssetManager service) {
    this.assetManager = service;
  }
}
