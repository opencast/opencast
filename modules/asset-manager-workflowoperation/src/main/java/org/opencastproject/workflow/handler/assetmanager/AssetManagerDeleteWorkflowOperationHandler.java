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

import static org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow operation for deleting an episode from the asset manager.
 *
 * @see AssetManager
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Asset Manager Delete Workflow Operation Handler",
        "workflow.operation=asset-delete"
    }
)
public class AssetManagerDeleteWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerDeleteWorkflowOperationHandler.class);

  /** The archive */
  private AssetManager assetManager;

  /** Configuration if last snapshot should not be deleted */
  private static final String OPT_LAST_SNAPSHOT = "keep-last-snapshot";
  private static final String OPT_ROLL_BACK_TO = "roll-back-to";

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
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    final String mpId = workflowInstance.getMediaPackage().getIdentifier().toString();

    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    boolean keepLastSnapshot = BooleanUtils.toBoolean(currentOperation.getConfiguration(OPT_LAST_SNAPSHOT));
    long rollbackVersion = NumberUtils.toLong(currentOperation.getConfiguration(OPT_ROLL_BACK_TO), -1);

    if (rollbackVersion >= 0 && keepLastSnapshot) {
      throw new WorkflowOperationException("Operation cannot roll back to an old version and keep the latest version.");
    } else if (rollbackVersion < 0 && !keepLastSnapshot) {
      throw new WorkflowOperationException("Operation needs a version to roll back to or keep the latest version.");
    }

    try {
      final AQueryBuilder q = assetManager.createQuery();
      final long deleted;

      if (keepLastSnapshot) {
        logger.info("Deleting all but latest snapshot of episode {}", mpId);
        deleted = q.delete(DEFAULT_OWNER, q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest().not()))
            .run();
      } else {
        logger.info("Rolling back to version {} in the asset manager", rollbackVersion);
        deleted = q.delete(DEFAULT_OWNER, q.snapshot())
            .where(q.mediaPackageId(mpId)
            .and(q.version().gt(new VersionImpl(rollbackVersion)))).run();
      }

      logger.info("Successfully deleted {} version/s episode {} from the asset manager", deleted, mpId);
    } catch (Exception e) {
      var errorMessage = String.format("Error deleting episode %s from the asset manager", mpId);
      throw new WorkflowOperationException(errorMessage, e);
    }

    // Get the media package we rolled back to from asset manager
    final AQueryBuilder q = assetManager.createQuery();
    var record = q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest()))
        .run().getRecords().stream().findFirst();
    if (record.isEmpty()) {
      throw new WorkflowOperationException("Could not get any results from asset manager. Expected at least one.");
    }
    var snapshot = record.get().getSnapshot();
    if (snapshot.isEmpty()) {
      throw new WorkflowOperationException("Asset manager record contains no snapshot.");
    }
    var mediaPackage = snapshot.get().getMediaPackage();

    // Update media package from asset manager with publications from workflow media package
    for (var publication: mediaPackage.getPublications()) {
      mediaPackage.remove(publication);
    }
    for (var publication: workflowInstance.getMediaPackage().getPublications()) {
      mediaPackage.add(publication);
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
