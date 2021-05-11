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
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Replaces the media package in the current workflow with a previous version from the asset manager. There are two ways
 * to choose the version: by version number or a combination of source-flavors and no-tags (choose the latest version
 * where elements with source-flavor do not have the tags specified in no-tags.
 *
 * This operation should be the first one in a workflow executed from the archive because it REPLACES the media package
 * used by the current workflow.
 */
public class SelectVersionWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(SelectVersionWorkflowOperationHandler.class);

  public static final String OPT_VERSION = "version";
  public static final String OPT_NO_TAGS = "no-tags";
  public static final String OPT_SOURCE_FLAVORS = "source-flavors";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  private AssetManager assetManager;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(OPT_VERSION,
            "Select the specific version. If informed, takes precedence over the other options.");
    CONFIG_OPTIONS.put(OPT_NO_TAGS, "Select first version where elements of the flavor passed do not have these tags.");
    CONFIG_OPTIONS.put(OPT_SOURCE_FLAVORS, "The flavor of elements to compare.");
  }

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

    // Specific version informed? If yes, use it.
    String version = null;
    if (!StringUtils.isEmpty(currentOperation.getConfiguration(OPT_VERSION))) {
      try {
        version = currentOperation.getConfiguration(OPT_VERSION).trim();
        // Validate the number
        Integer.parseInt(version);
      } catch (NumberFormatException e) {
        throw new WorkflowOperationException("Invalid version passed: " + version);
      }
    }
    if (version != null) {
      resultMp = findVersion(mp.getIdentifier().toString(), version);
      if (resultMp == null) {
        throw new WorkflowOperationException(
                String.format("Could not find version %d of mp %s in the archive", mp.getIdentifier(), version));
      }
    } else {
      if (StringUtils.isEmpty(currentOperation.getConfiguration(OPT_NO_TAGS))) {
        throw new WorkflowOperationException("Configuration missing: " + OPT_NO_TAGS);
      }
      String noTagsOpt = currentOperation.getConfiguration(OPT_NO_TAGS);
      Collection<String> noTags = Arrays.asList(noTagsOpt.split(","));

      String flavorStr = currentOperation.getConfiguration(OPT_SOURCE_FLAVORS);
      SimpleElementSelector elementSelector = new SimpleElementSelector();
      for (MediaPackageElementFlavor flavor : parseFlavors(flavorStr)) {
        elementSelector.addFlavor(flavor);
      }

      resultMp = findVersionWithNoTags(mp.getIdentifier().toString(), elementSelector, noTags);
      if (resultMp == null) {
        throw new WorkflowOperationException(String.format(
                "Could not find in the archive a version of mp %s that does not have the tags %s in element flavors %s",
                mp.getIdentifier(), noTagsOpt, flavorStr));
      }
    }
    return createResult(resultMp, Action.CONTINUE);
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
      Opt<Snapshot> optSnap = rec.getSnapshot();
      if (optSnap.isNone()) {
        continue;
      }
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
      Opt<Snapshot> optSnap = rec.getSnapshot();
      if (optSnap.isNone()) {
        continue;
      }
      MediaPackage mp = optSnap.get().getMediaPackage();
      for (MediaPackageElement el : elementSelector.select(mp, false)) {
        for (String t : el.getTags()) {
          if (tags.contains(t)) {
            continue nextVersion;
          }
        }
      }
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

  public void setAssetManager(AssetManager service) {
    this.assetManager = service;
  }
}
