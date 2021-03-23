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
package org.opencastproject.assetmanager.util;

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public Stream<WorkflowInstance> applyWorkflowToLatestVersion(Iterable<String> mpIds, ConfiguredWorkflow wf) {
    return $(mpIds).bind(findLatest).map(getMediapackage).bind(applyWorkflow(wf));
  }

  /**
   * Apply a workflow to a media package. The function returns some workflow instance if the
   * workflow could be started successfully, none otherwise.
   */
  public Fn<MediaPackage, Opt<WorkflowInstance>> applyWorkflow(final ConfiguredWorkflow wf) {
    return new Fn<MediaPackage, Opt<WorkflowInstance>>() {
      @Override public Opt<WorkflowInstance> apply(MediaPackage mp) {
        try {
          return Opt.some(wfs.start(wf.getWorkflowDefinition(), mp, wf.getParameters()));
        } catch (WorkflowDatabaseException | WorkflowParsingException e) {
          logger.error("Cannot start workflow on media package " + mp.getIdentifier().toString(), e);
          return Opt.none();
        }
      }
    };
  }

  // CHECKSTYLE:OFF
  private final Fn<Snapshot, MediaPackage> getMediapackage = new Fn<Snapshot, MediaPackage>() {
    @Override public MediaPackage apply(Snapshot snapshot) {
      return snapshot.getMediaPackage();
    }
  };
  // CHECKSTYLE:ON

  private final Fn<String, Iterable<Snapshot>> findLatest = new Fn<String, Iterable<Snapshot>>() {
    @Override public Iterable<Snapshot> apply(String mpId) {
      AQueryBuilder q = am.createQuery();
      return enrich(q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest())).run())
          .getSnapshots();
    }
  };
}
