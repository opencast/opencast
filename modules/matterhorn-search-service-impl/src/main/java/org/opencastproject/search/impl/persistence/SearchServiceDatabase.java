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
package org.opencastproject.search.impl.persistence;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.Iterator;

/**
 * API that defines persistent storage of series.
 * 
 */
public interface SearchServiceDatabase {

  /**
   * Returns all search entries in persistent storage.
   * 
   * @return {@link Tuple} array representing stored media packages
   * @throws SearchServiceDatabaseException
   *           if exception occurs
   */
  Iterator<Tuple<MediaPackage, String>> getAllMediaPackages() throws SearchServiceDatabaseException;

  /**
   * Returns the organization id of the selected media package
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @return the organization id
   * @throws NotFoundException
   *           if media package with specified id and version does not exist
   * @throws SearchServiceDatabaseException
   *           if an error occurs
   */
  String getOrganizationId(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException;

  /**
   * Gets a single media package by its identifier.
   * 
   * @param mediaPackageId
   *          the media package identifier
   * @return the media package
   * @throws NotFoundException
   *           if there is no media package with this identifier
   * @throws SearchServiceDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  MediaPackage getMediaPackage(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException;

  /**
   * Retrieves ACL for series with given ID.
   * 
   * @param mediaPackageId
   *          media package for which ACL will be retrieved
   * @return {@link AccessControlList} of media package or null if media package does not have ACL associated with it
   * @throws NotFoundException
   *           if media package with given ID does not exist
   * @throws SearchServiceDatabaseException
   *           if exception occurred
   */
  AccessControlList getAccessControlList(String mediaPackageId) throws NotFoundException,
          SearchServiceDatabaseException;

  /**
   * Returns the modification date from the selected media package.
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @return the modification date
   * @throws NotFoundException
   *           if media package with specified id and version does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  Date getModificationDate(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException;

  /**
   * Returns the deletion date from the selected media package.
   * 
   * @param mediaPackageId
   *          the media package id to select
   * @return the deletion date
   * @throws NotFoundException
   *           if media package with specified id does not exist
   * @throws EpisodeServiceDatabaseException
   *           if an error occurs
   */
  Date getDeletionDate(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException;

  /**
   * Removes media package from persistent storage.
   * 
   * @param mediaPackageId
   *          id of the media package to be removed
   * @param deletionDate
   *          the deletion date to set
   * @throws SearchServiceDatabaseException
   *           if exception occurs
   * @throws NotFoundException
   *           if media package with specified id is not found
   */
  void deleteMediaPackage(String mediaPackageId, Date deletionDate) throws SearchServiceDatabaseException,
          NotFoundException;

  /**
   * Store (or update) media package.
   * 
   * @param mediaPackage
   *          {@link MediaPackage} to store
   * @param acl
   *          the acl of the media package
   * @param now
   *          the store date
   * @throws SearchServiceDatabaseException
   *           if exception occurs
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   */
  void storeMediaPackage(MediaPackage mediaPackage, AccessControlList acl, Date now)
          throws SearchServiceDatabaseException, UnauthorizedException;

}
