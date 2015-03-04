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
package org.opencastproject.archive.base.persistence;

import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.PartialMediaPackage;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Option;

import java.util.Date;
import java.util.Iterator;

/** API that defines persistent storage of episodes. */
public interface ArchiveDb {

  /**
   * Returns the number of episodes in persistent storage.
   *
   * @return The count of episodes.
   * @throws ArchiveDbException
   *           if exception occurs
   */
  int countAllEpisodes() throws ArchiveDbException;

  /**
   * Returns all episodes in persistence storage
   *
   * @return {@link Episode} iterator representing stored episodes
   * @throws ArchiveDbException
   *         if exception occurs
   */
  Iterator<Episode> getAllEpisodes() throws ArchiveDbException;

  Version claimVersion(String mpId) throws ArchiveDbException;

  /**
   * Returns the media package from the selected episode
   *
   * @param mediaPackageId
   *         the media package id to select
   * @param version
   *         the archive version to select
   * @return the media package
   * @throws ArchiveDbException
   *         if an error occurs
   */
  Option<Episode> getEpisode(String mediaPackageId, Version version) throws ArchiveDbException;

  Option<Episode> getLatestEpisode(String mediaPackageId) throws ArchiveDbException;

  /**
   * Returns the latest version flag of the selected episode
   *
   * @param mediaPackageId
   *         the media package id to select
   * @param version
   *         the archive version to select
   * @return the latest version flag
   * @throws ArchiveDbException
   *         if an error occurs
   */
  Option<Boolean> isLatestVersion(String mediaPackageId, Version version) throws ArchiveDbException;

  /**
   * Returns the deletion date from the selected episode.
   *
   * @param mediaPackageId
   *         the media package id to select
   * @return the deletion date
   * @throws ArchiveDbException
   *         if an error occurs
   */
  Option<Date> getDeletionDate(String mediaPackageId) throws ArchiveDbException;

  /**
   * Sets a deletion date to the selected episode.
   *
   * @param mediaPackageId
   *         the media package id to select
   * @param deletionDate
   *         the deletion date to set
   * @return <code>false</code> if the requested media package does not exist
   * @throws ArchiveDbException
   *         if an error occurs
   */
  boolean deleteEpisode(String mediaPackageId, Date deletionDate) throws ArchiveDbException;

  /**
   * Stores a new version of a media package
   *
   * @param pmp
   *         the media package to store
   * @param acl
   *         the acl of the media package
   * @param now
   *         the store date
   * @param version
   *         the new version from the archive
   * @throws ArchiveDbException
   *         if an error occurs
   */
  void storeEpisode(PartialMediaPackage pmp,
                    AccessControlList acl,
                    Date now,
                    Version version) throws ArchiveDbException;

  Option<Asset> findAssetByChecksum(String checksum) throws ArchiveDbException;
}
