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

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.isNotPublication;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetId;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;

/**
 * Utility class to apply workflows to episodes. Removed 'final class' so that we can mock it for
 * unit tests.
 */
public class Workflows {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(Workflows.class);

  private static final String ASSETS_COLLECTION_ID = "assets";

  private final AssetManager am;
  private final Workspace ws;
  private final WorkflowService wfs;

  public Workflows(AssetManager am, Workspace ws, WorkflowService wfs) {
    this.am = am;
    this.ws = ws;
    this.wfs = wfs;
  }

  /**
   * Apply a workflow to each episode contained in the result set of a select query.
   */
  public Stream<WorkflowInstance> applyWorkflow(ASelectQuery q, ConfiguredWorkflow wf) {
    return enrich(q.run()).getSnapshots().map(putInWorkspace).bind(applyWorkflow(wf));
  }

  /**
   * Apply a workflow to the latest version of each media package.
   */
  public Stream<WorkflowInstance> applyWorkflowToLatestVersion(Iterable<String> mpIds, ConfiguredWorkflow wf) {
    return $(mpIds).bind(findLatest).map(putInWorkspace).bind(applyWorkflow(wf));
  }

  /**
   * Put all assets of an episode into the workspace to make them processable.
   * Please note that this may be an expensive operation depending on the size of the assets.
   *
   * @param snapshot
   *         the episode to put into the workspace
   * @return A media package whose element URIs point to the workspace. Please note that the episode's original media package
   * object (see {@link Snapshot#getMediaPackage()}) will not be modified.
   */
  public MediaPackage putInWorkspace(Snapshot snapshot) {
    final String mpId = snapshot.getMediaPackage().getIdentifier().compact();
    // create a copy of the media package
    final MediaPackage mp = MediaPackageSupport.copy(snapshot.getMediaPackage());
    for (final MediaPackageElement mpe : $(mp.getElements()).filter(Pred.mk(isNotPublication.toFn()))) {
      for (final MediaPackageElement mpeRewritten : putInWorkspace(snapshot, mpe)) {
        mp.remove(mpe);
        mp.add(mpeRewritten);
      }
    }
    return mp;
  }

  /**
   * Put a single media package element of an episode into the workspace and return the URI.
   * Please note that the original media package element will not be modified. Instead a copy
   * will be returned.
   *
   * @return some media package element if the media package element could be put into the workspace, none otherwise
   */
  public Opt<MediaPackageElement> putInWorkspace(Snapshot snapshot, MediaPackageElement mpe) {
    final String mpId = snapshot.getMediaPackage().getIdentifier().compact();
    final MediaPackageElement mpeCopy = MediaPackageSupport.copy(mpe);
    if (snapshot.getMediaPackage().contains(mpe)) {
      final Opt<Asset> asset = am.getAsset(snapshot.getVersion(), mpId, mpe.getIdentifier());
      if (asset.isSome() && eq(Availability.ONLINE, asset.get().getAvailability())) {
        // found and available
        // put the asset into the workspace
        try (InputStream in = asset.get().getInputStream()) {
          final String ext = asset.get().getMimeType().bind(suffix).getOr("unknown");
          final URI uri = ws.putInCollection(
                  ASSETS_COLLECTION_ID,
                  mkFileName(asset.get().getId(), mpe.getElementType(), ext),
                  in);
          mpeCopy.setURI(uri);
          return Opt.some(mpeCopy);
        } catch (Exception e) {
          logger.error("Unable to put asset {} into the workspace", asset.get().getId());
          return Opt.none();
        }
      } else if (asset.isSome()) {
        // found, unavailable
        logger.error(format("Asset %s is not available", asset.get().getId()));
        return Opt.none();
      } else {
        // not found
        logger.error(format("Element %s of version %s of media package %s  does not exist",
                            mpe.getIdentifier(),
                            snapshot.getVersion(),
                            mpId));
        return Opt.none();
      }
    } else {
      throw new IllegalArgumentException(format("Media package element %s is not part of episode %s", mpe.getIdentifier(), snapshot));
    }
  }

  /**
   * {@link #putInWorkspace(Snapshot)} as a function.
   */
  // CHECKSTYLE:OFF
  public final Fn<Snapshot, MediaPackage> putInWorkspace = new Fn<Snapshot, MediaPackage>() {
    @Override public MediaPackage apply(Snapshot snapshot) {
      return putInWorkspace(snapshot);
    }
  };
  // CHECKSTYLE:ON

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

  /* ------------------------------------------------------------------------------------------------------------------ */

  private String mkFileName(AssetId id, Type elementType, String ext) {
    // TODO unsafe - escape/encode strings
    return format("%s_%s_%s_%s.%s", id.getMediaPackageId(), id.getMediaPackageElementId(), id.getVersion().toString(), elementType, ext);
  }

  private final Fn<MimeType, Opt<String>> suffix = new Fn<MimeType, Opt<String>>() {
    @Override
    public Opt<String> apply(MimeType mimeType) {
      return mimeType.getSuffix().toOpt();
    }
  };

  private final Fn<String, Iterable<Snapshot>> findLatest = new Fn<String, Iterable<Snapshot>>() {
    @Override public Iterable<Snapshot> apply(String mpId) {
      AQueryBuilder q = am.createQuery();
      return enrich(q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest())).run()).getSnapshots();
    }
  };

  public Iterable<Snapshot> getLatestVersion(String mpId) {
    return $(mpId).bind(findLatest);
  }

}
