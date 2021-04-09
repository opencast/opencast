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

import static org.opencastproject.authorization.xacml.manager.impl.Util.toAcl;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.acl.AclItem;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Organization bound impl. */
public final class AclServiceImpl implements AclService {
  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(AclServiceImpl.class);

  /** Context */
  private final Organization organization;

  /** Service dependencies */
  private final AclDb aclDb;
  private final SeriesService seriesService;
  private final AssetManager assetManager;
  private final AuthorizationService authorizationService;
  private final MessageSender messageSender;

  public AclServiceImpl(Organization organization, AclDb aclDb, SeriesService seriesService, AssetManager assetManager,
          AuthorizationService authorizationService, MessageSender messageSender) {
    this.organization = organization;
    this.aclDb = aclDb;
    this.seriesService = seriesService;
    this.assetManager = assetManager;
    this.authorizationService = authorizationService;
    this.messageSender = messageSender;
  }

  @Override
  public boolean applyAclToEpisode(String episodeId, AccessControlList acl) throws AclServiceException {
    try {
      Opt<MediaPackage> mediaPackage = Opt.none();
      if (assetManager != null) {
        mediaPackage = assetManager.getMediaPackage(episodeId);
      }

      Option<AccessControlList> aclOpt = Option.option(acl);
      // the episode service is the source of authority for the retrieval of media packages
      if (mediaPackage.isSome()) {
        MediaPackage episodeSvcMp = mediaPackage.get();
        aclOpt.fold(new Option.EMatch<AccessControlList>() {
          // set the new episode ACL
          @Override
          public void esome(final AccessControlList acl) {
            // update in episode service
            try {
              MediaPackage mp = authorizationService.setAcl(episodeSvcMp, AclScope.Episode, acl).getA();
              if (assetManager != null) {
                assetManager.takeSnapshot(mp);
              }
            } catch (MediaPackageException e) {
              logger.error("Error getting ACL from media package", e);
            }
          }

          // if none EpisodeACLTransition#isDelete returns true so delete the episode ACL
          @Override
          public void enone() {
            // update in episode service
            MediaPackage mp = authorizationService.removeAcl(episodeSvcMp, AclScope.Episode);
            if (assetManager != null) {
              assetManager.takeSnapshot(mp);
            }
          }

        });
        return true;
      }
      // not found
      return false;
    } catch (Exception e) {
      throw new AclServiceException(e);
    }
  }

  @Override
  public boolean applyAclToEpisode(String episodeId, Option<ManagedAcl> managedAcl) throws AclServiceException {
    return applyAclToEpisode(episodeId, managedAcl.map(toAcl).getOrElseNull());
  }

  @Override
  public boolean applyAclToSeries(String seriesId, AccessControlList acl, boolean override)
          throws AclServiceException {
    try {
      // update in series service
      // this will in turn update the search service by the SeriesUpdatedEventHandler
      // and the episode service by the EpisodesPermissionsUpdatedEventHandler
      try {
        seriesService.updateAccessControl(seriesId, acl, override);
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
  public boolean applyAclToSeries(String seriesId, ManagedAcl managedAcl, boolean override)
          throws AclServiceException {
    return applyAclToSeries(seriesId, managedAcl.getAcl(), override);
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
