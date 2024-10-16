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
import static org.opencastproject.mediapackage.MediaPackageElements.EPISODE;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_SERIES;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

  /** The workspace */
  private Workspace workspace;

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

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final String mpId = mediaPackage.getIdentifier().toString();

    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    boolean keepLastSnapshot = BooleanUtils.toBoolean(currentOperation.getConfiguration(OPT_LAST_SNAPSHOT));
    long rollbackVersion = NumberUtils.toLong(currentOperation.getConfiguration(OPT_ROLL_BACK_TO), -1);

    if (rollbackVersion >= 0 && keepLastSnapshot) {
      throw new WorkflowOperationException("Operation cannot roll back to an old version and keep the latest version.");
    }

    try {
      final AQueryBuilder q = assetManager.createQuery();
      final long deleted;

      // Unless keepLatestSnapshot is true, we will probably delete files we are working on.
      // This leads to errors like this in the subsequent workflow execution:
      //   ERROR | (DublinCoreUtil:79) - Unable to load metadata from catalog
      //   'http://localhost:8080/assets/assets/01/.../4/dublincore.xml
      //   org.opencastproject.util.NotFoundException: null
      // To avoid this, we load the necessary files into the working file repository.
      // Necessary files are the episode metadata catalog and the ACLs.
      if (!keepLastSnapshot) {
        for (var flavor: List.of(EPISODE, XACML_POLICY_EPISODE, XACML_POLICY_SERIES)) {
          for (var element : mediaPackage.getElementsByFlavor(flavor)) {
            var filename = FilenameUtils.getName(element.getURI().getPath());
            var uri = workspace.put(mpId, element.getIdentifier(), filename, workspace.read(element.getURI()));
            element.setURI(uri);
          }
        }
      }

      if (keepLastSnapshot) {
        logger.info("Deleting all but latest snapshot of episode {}", mpId);
        deleted = q.delete(DEFAULT_OWNER, q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest().not()))
            .run();
      } else if (rollbackVersion >= 0) {
        logger.info("Rolling back to version {} in the asset manager", rollbackVersion);
        deleted = q.delete(DEFAULT_OWNER, q.snapshot())
            .where(q.mediaPackageId(mpId)
            .and(q.version().gt(new VersionImpl(rollbackVersion)))).run();
      } else {
        logger.info("Deleting all snapshots of episode {}", mpId);
        deleted = q.delete(DEFAULT_OWNER, q.snapshot())
                .where(q.mediaPackageId(mpId)).run();
      }

      logger.info("Successfully deleted {} version/s episode {} from the asset manager", deleted, mpId);
    } catch (Exception e) {
      var errorMessage = String.format("Error deleting episode %s from the asset manager", mpId);
      throw new WorkflowOperationException(errorMessage, e);
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
