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
package org.opencastproject.assetmanager.util;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to apply workflows to episodes. Removed 'final class' so that we can mock it for
 * unit tests.
 */
public class Workflows {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(Workflows.class);

  private final AssetManager am;
  private final WorkflowService wfs;

  public Workflows(AssetManager am, WorkflowService wfs) {
    this.am = am;
    this.wfs = wfs;
  }

  /**
   * Apply a workflow to the latest version of each media package.
   */
  public List<WorkflowInstance> applyWorkflowToLatestVersion(Iterable<String> mpIds, ConfiguredWorkflow wf) {
    List<WorkflowInstance> result = new ArrayList<>();

    for (String mpId : mpIds) {
      List<Snapshot> snapshots = findLatestSnapshots(mpId);
      for (Snapshot snapshot : snapshots) {
        MediaPackage mp = snapshot.getMediaPackage();
        Optional<WorkflowInstance> optWorkflow = applyWorkflow(wf, mp);
        optWorkflow.ifPresent(result::add);
      }
    }

    return result;
  }

  /**
   * Apply a workflow to a media package. The function returns some workflow instance if the
   * workflow could be started successfully, none otherwise.
   */
  private Optional<WorkflowInstance> applyWorkflow(ConfiguredWorkflow wf, MediaPackage mp) {
    try {
      WorkflowInstance instance = wfs.start(wf.getWorkflowDefinition(), mp, wf.getParameters());
      return Optional.of(instance);
    } catch (WorkflowDatabaseException | WorkflowParsingException | UnauthorizedException e) {
      logger.error("Cannot start workflow on media package {}", mp.getIdentifier(), e);
      return Optional.empty();
    }
  }

  private List<Snapshot> findLatestSnapshots(String mpId) {
    List<Snapshot> list = new ArrayList<>();
    Optional<Snapshot> snapshot = am.getLatestSnapshot(mpId);
    if (snapshot.isPresent()) {
      list.add(snapshot.get());
    }
    return list;
  }
}
