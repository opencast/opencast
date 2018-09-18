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

import static org.opencastproject.util.data.Collections.smap;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.impl.TieredStorageAssetManagerJobProducer;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;

public class AssetManagerStorageMoveOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(AssetManagerStorageMoveOperationHandler.class);

  /** The asset manager. */
  private TieredStorageAssetManagerJobProducer tsamjp;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS = smap(
          tuple("target-storage", "The target storage type"),
          tuple("target-version", "The specific version to move"));

  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /** OSGi DI */
  public void setJobProducer(TieredStorageAssetManagerJobProducer tsamjp) {
    this.tsamjp = tsamjp;
  }


  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mp = workflowInstance.getMediaPackage();
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    logger.debug("Working on mediapackage {}", mp.getIdentifier().toString());

    String targetStorage = StringUtils.trimToNull(operation.getConfiguration("target-storage"));
    if (!tsamjp.datastoreExists(targetStorage)) {
      throw new WorkflowOperationException("Target storage type " + targetStorage + " is not available!");
    }
    logger.debug("Target storage set to {}", targetStorage);

    //A missing version is ok, that just means to select all of them (which is represented as null)
    String targetVersion = StringUtils.trimToNull(operation.getConfiguration("target-version"));
    Version version = null;
    if (null != targetVersion) {
      try {
        version = VersionImpl.mk(Long.parseLong(targetVersion));
      } catch (NumberFormatException e) {
        throw new WorkflowOperationException("Invalid version number", e);
      }
    }

    logger.debug("Target version set to {}", version);

    logger.debug("Beginning moving process");
    //Note that a null version implies *all* versions
    Job job = tsamjp.moveByIdAndVersion(version, mp.getIdentifier().compact(), targetStorage);
    if (waitForStatus(job).isSuccess()) {
      return createResult(WorkflowOperationResult.Action.CONTINUE);
    } else {
      throw new WorkflowOperationException("Archive operation did not complete successfully!");
    }
  }
}
