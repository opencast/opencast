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
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
  private final SecurityService securityService;

  /** The Elasticsearch indices */
  protected AbstractSearchIndex adminUiIndex;
  protected AbstractSearchIndex externalApiIndex;

  public AclServiceImpl(Organization organization, AclDb aclDb, SeriesService seriesService, AssetManager assetManager,
          AuthorizationService authorizationService, AbstractSearchIndex adminUiIndex,
          AbstractSearchIndex externalApiIndex, SecurityService securityService) {
    this.organization = organization;
    this.aclDb = aclDb;
    this.seriesService = seriesService;
    this.assetManager = assetManager;
    this.authorizationService = authorizationService;
    this.adminUiIndex = adminUiIndex;
    this.externalApiIndex = externalApiIndex;
    this.securityService = securityService;
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
        User user = securityService.getUser();
        updateAclInIndex(oldName.get().getName(), acl.getName(), adminUiIndex, organization.getId(), user);
        updateAclInIndex(oldName.get().getName(), acl.getName(), externalApiIndex, organization.getId(), user);
      }
    }
    return updateAcl;
  }

  @Override
  public Option<ManagedAcl> createAcl(AccessControlList acl, String name) {
    // we don't need to update the Elasticsearch indices in this case
    return aclDb.createAcl(organization, acl, name);
  }

  @Override
  public boolean deleteAcl(long id) throws AclServiceException, NotFoundException {
    Option<ManagedAcl> deletedAcl = getAcl(id);
    if (aclDb.deleteAcl(organization, id)) {
      if (deletedAcl.isSome()) {
        User user = securityService.getUser();
        removeAclFromIndex(deletedAcl.get().getName(), adminUiIndex, organization.getId(), user);
        removeAclFromIndex(deletedAcl.get().getName(), externalApiIndex, organization.getId(), user);
      }
      return true;
    }
    throw new NotFoundException("Managed acl with id " + id + " not found.");
  }

  /**
   * Update the Managed ACL in the events and series in the Elasticsearch index.
   *
   * @param currentAclName
   *         the current name of the managed acl
   * @param newAclName
   *         the new name of the managed acl
   * @param index
   *         the index to update
   * @param orgId
   *         the organization the managed acl belongs to
   * @param user
   *         the current user
   */
  private void updateAclInIndex(String currentAclName, String newAclName, AbstractSearchIndex index, String orgId,
          User user) {
    logger.debug("Update the events to change the managed acl name from '{}' to '{}'.", currentAclName, newAclName);
    updateManagedAclForEvents(currentAclName, Optional.of(newAclName), index, orgId, user);

    logger.debug("Update the series to change the managed acl name from '{}' to '{}'.", currentAclName, newAclName);
    updateManagedAclForSeries(currentAclName, Optional.of(newAclName), index, orgId, user);
  }

  /**
   * Remove the Managed ACL from the events and series in the Elasticsearch index.
   *
   * @param currentAclName
   *         the current name of the managed acl
   * @param index
   *         the index to update
   * @param orgId
   *         the organization the managed acl belongs to
   * @param user
   *         the current user
   */
  private void removeAclFromIndex(String currentAclName, AbstractSearchIndex index, String orgId,
          User user) {
    logger.debug("Update the events to remove the managed acl name '{}'.", currentAclName);
    updateManagedAclForEvents(currentAclName, Optional.empty(), index, orgId, user);

    logger.debug("Update the series to remove the managed acl name '{}'.", currentAclName);
    updateManagedAclForSeries(currentAclName, Optional.empty(), index, orgId, user);
  }

  /**
   * Update or remove the Managed Acl for the series in the Elasticsearch index.
   *
   * @param currentAclName
   *         the current name of the managed acl
   * @param newAclNameOpt
   * @param index
   *         the index to update
   * @param orgId
   *         the organization the managed acl belongs to
   * @param user
   *         the current user
   */
  private void updateManagedAclForSeries(String currentAclName, Optional<String> newAclNameOpt,
          AbstractSearchIndex index, String orgId, User user) {
    SearchResult<Series> result;
    try {
      result = index.getByQuery(new SeriesSearchQuery(orgId, user).withoutActions()
              .withManagedAcl(currentAclName));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the series in org '{}' with current managed acl name '{}'", orgId, currentAclName,
              e);
      return;
    }

    for (SearchResultItem<Series> seriesItem : result.getItems()) {
      String seriesId = seriesItem.getSource().getIdentifier();

      Function<Optional<Series>, Optional<Series>> updateFunction = (Optional<Series> seriesOpt) -> {
        if (seriesOpt.isPresent() && seriesOpt.get().getManagedAcl().equals(currentAclName)) {
          Series series = seriesOpt.get();
          series.setManagedAcl(newAclNameOpt.orElse(null));
          return Optional.of(series);
        }
        return Optional.empty();
      };

      try {
        index.addOrUpdateSeries(seriesId, updateFunction, orgId, user);
      } catch (SearchIndexException e) {
        if (newAclNameOpt.isPresent()) {
          logger.warn("Unable to update series'{}' from current managed acl '{}' to new managed acl name '{}'",
                  seriesId, currentAclName, newAclNameOpt.get(), e);
        } else {
          logger.warn("Unable to update series '{}' to remove managed acl '{}'", seriesId, currentAclName, e);
        }
      }
    }
  }

  /**
   * Update or remove the Managed Acl for the events in the Elasticsearch index.
   *
   * @param currentAclName
   *         the current name of the managed acl
   * @param newAclNameOpt
   * @param index
   *         the index to update
   * @param orgId
   *         the organization the managed acl belongs to
   * @param user
   *         the current user
   */
  private void updateManagedAclForEvents(String currentAclName, Optional<String> newAclNameOpt,
          AbstractSearchIndex index, String orgId, User user) {
    SearchResult<Event> result;
    try {
      result = index.getByQuery(new EventSearchQuery(orgId, user).withoutActions()
              .withManagedAcl(currentAclName));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the events in org '{}' with current managed acl name '{}' for event",
              orgId, currentAclName, e);
      return;
    }

    for (SearchResultItem<Event> eventItem : result.getItems()) {
      String eventId = eventItem.getSource().getIdentifier();

      Function<Optional<Event>, Optional<Event>> updateFunction = (Optional<Event> eventOpt) -> {
        if (eventOpt.isPresent() && eventOpt.get().getManagedAcl().equals(currentAclName)) {
          Event event = eventOpt.get();
          event.setManagedAcl(newAclNameOpt.orElse(null));
          return Optional.of(event);
        }
        return Optional.empty();
      };

      try {
        index.addOrUpdateEvent(eventId, updateFunction, orgId, user);
      } catch (SearchIndexException e) {
        if (newAclNameOpt.isPresent()) {
          logger.warn(
                  "Unable to update event '{}' from current managed acl '{}' to new managed acl name '{}'",
                  eventId, currentAclName, newAclNameOpt.get(), e);
        } else {
          logger.warn("Unable to update event '{}' to remove managed acl '{}'", eventId, currentAclName, e);
        }
      }
    }
  }
}
