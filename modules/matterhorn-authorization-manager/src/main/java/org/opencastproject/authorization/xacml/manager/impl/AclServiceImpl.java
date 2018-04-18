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

package org.opencastproject.authorization.xacml.manager.impl;

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;
import static org.opencastproject.authorization.xacml.manager.impl.Util.toAcl;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.workflow.api.ConfiguredWorkflowRef.toConfiguredWorkflow;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.fn.Snapshots;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.acl.AclItem;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Organization bound impl. */
public final class AclServiceImpl implements AclService {
  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(AclServiceImpl.class);

  /** Context */
  private final Organization organization;

  /** Service dependencies */
  private final AclTransitionDb persistence;
  private final AclDb aclDb;
  private final SeriesService seriesService;
  private final AssetManager assetManager;
  private final AuthorizationService authorizationService;
  private final WorkflowService workflowService;
  private final Workspace workspace;
  private final MessageSender messageSender;

  public AclServiceImpl(Organization organization, AclDb aclDb, AclTransitionDb transitionDb,
          SeriesService seriesService, AssetManager assetManager, WorkflowService workflowService,
          AuthorizationService authorizationService, MessageSender messageSender, Workspace workspace) {
    this.organization = organization;
    this.persistence = transitionDb;
    this.aclDb = aclDb;
    this.seriesService = seriesService;
    this.assetManager = assetManager;
    this.workflowService = workflowService;
    this.authorizationService = authorizationService;
    this.messageSender = messageSender;
    this.workspace = workspace;
  }

  @Override
  public EpisodeACLTransition addEpisodeTransition(String episodeId, Option<Long> managedAclId, Date at,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException {
    return persistence.storeEpisodeAclTransition(organization, episodeId, at, managedAclId, workflow);
  }

  @Override
  public SeriesACLTransition addSeriesTransition(String seriesId, long managedAclId, Date at, boolean override,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException {
    return persistence.storeSeriesAclTransition(organization, seriesId, at, managedAclId, override, workflow);
  }

  @Override
  public EpisodeACLTransition updateEpisodeTransition(long transitionId, Option<Long> managedAclId, Date at,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException, NotFoundException {
    return persistence.updateEpisodeAclTransition(organization, transitionId, at, managedAclId, workflow);
  }

  @Override
  public SeriesACLTransition updateSeriesTransition(long transitionId, long managedAclId, Date at,
          Option<ConfiguredWorkflowRef> workflow, boolean override) throws AclServiceException, NotFoundException {
    return persistence.updateSeriesAclTransition(organization, transitionId, at, managedAclId, override, workflow);
  }

  @Override
  public void deleteEpisodeTransition(long transitionId) throws NotFoundException, AclServiceException {
    persistence.deleteEpisodeAclTransition(organization, transitionId);
  }

  @Override
  public void deleteSeriesTransition(long transitionId) throws NotFoundException, AclServiceException {
    persistence.deleteSeriesAclTransition(organization, transitionId);
  }

  @Override
  public void deleteEpisodeTransitions(String episodeId) throws AclServiceException, NotFoundException {
    List<EpisodeACLTransition> transitions = persistence.getEpisodeAclTransitions(organization, episodeId);
    for (EpisodeACLTransition transition : transitions) {
      persistence.deleteEpisodeAclTransition(organization, transition.getTransitionId());
    }
  }

  @Override
  public void deleteSeriesTransitions(String seriesId) throws AclServiceException, NotFoundException {
    List<SeriesACLTransition> transitions = persistence.getSeriesAclTransitions(organization, seriesId);
    for (SeriesACLTransition transition : transitions) {
      persistence.deleteSeriesAclTransition(organization, transition.getTransitionId());
    }
  }

  @Override
  public TransitionResult getTransitions(TransitionQuery query) throws AclServiceException {
    return persistence.getByQuery(organization, query);
  }

  @Override
  public SeriesACLTransition markSeriesTransitionAsCompleted(long transitionId) throws AclServiceException,
  NotFoundException {
    return persistence.markSeriesTransitionAsCompleted(organization, transitionId);
  }

  @Override
  public EpisodeACLTransition markEpisodeTransitionAsCompleted(long transitionId) throws AclServiceException,
  NotFoundException {
    return persistence.markEpisodeTransitionAsCompleted(organization, transitionId);
  }

  @Override
  public boolean applyAclToEpisode(String episodeId, AccessControlList acl, Option<ConfiguredWorkflowRef> workflow)
          throws AclServiceException {
    try {
      Option<MediaPackage> mediaPackage = Option.none();
      if (assetManager != null)
        mediaPackage = getFromAssetManagerByMpId(episodeId);

      Option<AccessControlList> aclOpt = Option.option(acl);
      // the episode service is the source of authority for the retrieval of media packages
      for (final MediaPackage episodeSvcMp : mediaPackage) {
        aclOpt.fold(new Option.EMatch<AccessControlList>() {
          // set the new episode ACL
          @Override
          public void esome(final AccessControlList acl) {
            // update in episode service
            MediaPackage mp = authorizationService.setAcl(episodeSvcMp, AclScope.Episode, acl).getA();
            if (assetManager != null)
              assetManager.takeSnapshot(mp);
          }

          // if none EpisodeACLTransition#isDelete returns true so delete the episode ACL
          @Override
          public void enone() {
            // update in episode service
            MediaPackage mp = authorizationService.removeAcl(episodeSvcMp, AclScope.Episode);
            if (assetManager != null)
              assetManager.takeSnapshot(mp);
          }

        });
        // apply optional workflow
        for (ConfiguredWorkflowRef workflowRef : workflow)
          applyWorkflow(list(episodeSvcMp), workflowRef);
        return true;
      }
      // not found
      return false;
    } catch (Exception e) {
      logger.error("Error applying episode ACL", e);
      throw new AclServiceException(e);
    }
  }

  @Override
  public boolean applyAclToEpisode(String episodeId, Option<ManagedAcl> managedAcl,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException {
    return applyAclToEpisode(episodeId, managedAcl.map(toAcl).getOrElseNull(), workflow);
  }

  /** Update the ACL of an episode. */
  @Override
  public void applyEpisodeAclTransition(final EpisodeACLTransition t) throws AclServiceException {
    applyAclToEpisode(t.getEpisodeId(), t.getAccessControlList(), t.getWorkflow());
    try {
      // mark as done
      // todo If acl application fails the transition will not be marked as done. Depending on the
      // failure cause the application of the transition will be tried forever.
      markEpisodeTransitionAsCompleted(t.getTransitionId());
    } catch (NotFoundException e) {
      throw new AclServiceException(e);
    }
  }

  @Override
  public boolean applyAclToSeries(String seriesId, AccessControlList acl, boolean override,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException {
    try {
      if (override) {
        // delete acls before calling seriesService.updateAccessControl to avoid
        // possible interference since a call to this method triggers update event handlers
        // which run on a separate thread. This must be considered a design smell since it
        // requires knowledge of the services implementation.
        //
        // delete in episode service
        List<MediaPackage> mediaPackages = new ArrayList<>();
        if (assetManager != null)
          mediaPackages = getFromAssetManagerBySeriesId(seriesId);

        for (MediaPackage mp : mediaPackages) {
          // remove episode xacml and update in archive service
          if (assetManager != null)
            assetManager.takeSnapshot(authorizationService.removeAcl(mp, AclScope.Episode));
        }
      }
      // update in series service
      // this will in turn update the search service by the SeriesUpdatedEventHandler
      // and the episode service by the EpisodesPermissionsUpdatedEventHandler
      try {
        seriesService.updateAccessControl(seriesId, acl);
      } catch (NotFoundException e) {
        return false;
      }
      return true;
    } catch (Exception e) {
      logger.error("Error applying series ACL", e);
      throw new AclServiceException(e);
    }
  }

  @Override
  public boolean applyAclToSeries(String seriesId, ManagedAcl managedAcl, boolean override,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException {
    return applyAclToSeries(seriesId, managedAcl.getAcl(), override, workflow);
  }

  /** Update the ACL of a series. */
  @Override
  public void applySeriesAclTransition(SeriesACLTransition t) throws AclServiceException {
    applyAclToSeries(t.getSeriesId(), t.getAccessControlList(), t.isOverride(), t.getWorkflow());
    try {
      // mark as done
      // todo If acl application fails the transition will not be marked as done. Depending on the
      // failure cause the application of the transition will be tried forever.
      markSeriesTransitionAsCompleted(t.getTransitionId());
    } catch (NotFoundException e) {
      throw new AclServiceException(e);
    }
  }

  /**
   * Return media package with id <code>mpId</code> from asset manager.
   *
   * @return single element list or empty list
   */
  private Option<MediaPackage> getFromAssetManagerByMpId(String mpId) {
    final AQueryBuilder q = assetManager.createQuery();
    final Opt<MediaPackage> mp = enrich(
            q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest())).run()).getSnapshots()
                    .head().map(Snapshots.getMediaPackage);
    return Option.fromOpt(mp);
  }

  /** Return all media packages of a series from the asset manager. */
  private List<MediaPackage> getFromAssetManagerBySeriesId(String seriesId) {
    final AQueryBuilder q = assetManager.createQuery();
    return enrich(q.select(q.snapshot()).where(q.seriesId().eq(seriesId).and(q.version().isLatest())).run())
            .getSnapshots().map(Snapshots.getMediaPackage).toList();
  }

  /** Apply a workflow to a list of media packages. */
  private void applyWorkflow(final List<MediaPackage> mps, final ConfiguredWorkflowRef workflowRef) {
    toConfiguredWorkflow(workflowService, workflowRef).fold(new Option.Match<ConfiguredWorkflow, Void>() {
      @Override
      public Void some(ConfiguredWorkflow workflow) {
        logger.info("Apply optional workflow {}", workflow.getWorkflowDefinition().getId());
        if (assetManager != null) {
          new Workflows(assetManager, workspace, workflowService)
                  .applyWorkflowToLatestVersion($(mps).map(MediaPackageSupport.Fn.getId.toFn()), workflow);
        }
        return null;
      }

      @Override
      public Void none() {
        logger.warn("{} does not exist", workflowRef.getWorkflowId());
        return null;
      }
    });
  }

  @Override
  public List<ManagedAcl> getAcls() {
    return aclDb.getAcls(organization);
  }

  @Override
  public Option<ManagedAcl> getAcl(long id) {
    return aclDb.getAcl(organization, id);
  }

  @Override
  public boolean updateAcl(ManagedAcl acl) {
    Option<ManagedAcl> oldName = getAcl(acl.getId());
    boolean updateAcl = aclDb.updateAcl(acl);
    if (updateAcl) {
      if (oldName.isSome() && !(oldName.get().getName().equals(acl.getName()))) {
        AclItem aclItem = AclItem.update(oldName.get().getName(), acl.getName());
        messageSender.sendObjectMessage(AclItem.ACL_QUEUE, MessageSender.DestinationType.Queue, aclItem);
      }
    }
    return updateAcl;
  }

  @Override
  public Option<ManagedAcl> createAcl(AccessControlList acl, String name) {
    Option<ManagedAcl> createAcl = aclDb.createAcl(organization, acl, name);
    if (createAcl.isSome()) {
      AclItem aclItem = AclItem.create(createAcl.get().getName());
      messageSender.sendObjectMessage(AclItem.ACL_QUEUE, MessageSender.DestinationType.Queue, aclItem);
    }
    return createAcl;
  }

  @Override
  public boolean deleteAcl(long id) throws AclServiceException, NotFoundException {
    final TransitionQuery query = TransitionQuery.query().withDone(false).withAclId(id);
    final TransitionResult result = persistence.getByQuery(organization, query);
    if (result.getEpisodeTransistions().size() > 0 || result.getSeriesTransistions().size() > 0)
      return false;
    Option<ManagedAcl> deletedAcl = getAcl(id);
    if (aclDb.deleteAcl(organization, id)) {
      if (deletedAcl.isSome()) {
        AclItem aclItem = AclItem.delete(deletedAcl.get().getName());
        messageSender.sendObjectMessage(AclItem.ACL_QUEUE, MessageSender.DestinationType.Queue, aclItem);
      }
      return true;
    }
    throw new NotFoundException("Managed acl with id " + id + " not found.");
  }
}
