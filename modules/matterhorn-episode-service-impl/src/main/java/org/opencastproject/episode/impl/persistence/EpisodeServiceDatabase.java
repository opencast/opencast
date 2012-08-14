/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.episode.impl.persistence;

import org.opencastproject.episode.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple3;

import java.util.Date;
import java.util.Iterator;

/**
 * API that defines persistent storage of episodes.
 */
public interface EpisodeServiceDatabase {

  /**
   * Returns all episodes in persistence storage
   * 
   * @return {@link Tuple3} iterator representing stored episodes
   * @throws EpisodeServiceDatabaseException
   *           if exception occurs
   */
  Iterator<Tuple3<MediaPackage, Version, String>> getAllEpisodes() throws EpisodeServiceDatabaseException;

  Version claimVersion(String mpId) throws EpisodeServiceDatabaseException;

  /**
   * Returns the organization id of the selected episode
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @param version
   *          the archive version to select
   * @return the organization id
   * @throws NotFoundException
   *           if episode with specified id and version does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  String getOrganizationId(String mediaPackageId, Version version) throws NotFoundException,
          EpisodeServiceDatabaseException;

  /**
   * Returns the media package from the selected episode
   * todo do not return DTO, convert to business object
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @param version
   *          the archive version to select
   * @return the media package
   * @throws NotFoundException
   *           if episode with specified id and version does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  Option<EpisodeDto> getEpisode(String mediaPackageId, Version version) throws EpisodeServiceDatabaseException;

  /**
   * Returns the ACL from the selected episode
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @param version
   *          the archive version to select
   * @return the {@link AccessControlList}
   * @throws NotFoundException
   *           if episode with specified id and version does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  AccessControlList getAccessControlList(String mediaPackageId, Version version) throws NotFoundException,
          EpisodeServiceDatabaseException;

  /**
   * Returns the latest version flag of the selected episode
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @param version
   *          the archive version to select
   * @return the latest version flag
   * @throws NotFoundException
   *           if episode with specified id and version does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  boolean isLatestVersion(String mediaPackageId, Version version) throws NotFoundException, EpisodeServiceDatabaseException;

  /**
   * Returns the lock state of the selected episode
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @return the lock state
   * @throws NotFoundException
   *           if episode with specified id does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  boolean getLockState(String mediaPackageId) throws NotFoundException, EpisodeServiceDatabaseException;

  /**
   * Returns the modification date from the selected episode.
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @param version
   *          the archive version to select
   * @return the modification date
   * @throws NotFoundException
   *           if episode with specified id and version does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  Date getModificationDate(String mediaPackageId, Version version) throws NotFoundException,
          EpisodeServiceDatabaseException;

  /**
   * Returns the deletion date from the selected episode.
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @return the deletion date
   * @throws NotFoundException
   *           if episode with specified id does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  Date getDeletionDate(String mediaPackageId) throws NotFoundException, EpisodeServiceDatabaseException;

  /**
   * Sets a deletion date to the selected episode.
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @param deletionDate
   *          the deletion date to set
   * @throws NotFoundException
   *           if episode with specified id does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  void deleteEpisode(String mediaPackageId, Date deletionDate) throws NotFoundException,
          EpisodeServiceDatabaseException;

  /**
   * Sets a lock state to the selected episode.
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @param lock
   *          the lock state
   * @throws NotFoundException
   *           if episode with specified id does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  void lockEpisode(String mediaPackageId, boolean lock) throws NotFoundException, EpisodeServiceDatabaseException;

  /**
   * Stores a new version of a media package
   * 
   * @param mediaPackage
   *          the media package to store
   * @param acl
   *          the acl of the media package
   * @param now
   *          the store date
   * @param version
   *          the new version from the archive
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  void storeEpisode(MediaPackage mediaPackage, AccessControlList acl, Date now, Version version) throws EpisodeServiceDatabaseException;

  Option<Asset> findAssetByChecksum(String checksum) throws EpisodeServiceDatabaseException;
}
