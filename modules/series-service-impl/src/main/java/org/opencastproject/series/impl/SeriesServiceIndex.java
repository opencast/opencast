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

package org.opencastproject.series.impl;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.util.NotFoundException;

import java.util.Map;

/**
 * Defines methods for indexing, retrieving and searching through index.
 *
 */
public interface SeriesServiceIndex {
  /**
   * Performs any necessary setup and activates index.
   */
  void activate();

  /**
   * Deactivates index and performs any necessary cleanup.
   */
  void deactivate();

  /**
   * Index (new or existing) Dublin core representing series.
   *
   * @param dublinCore
   *          {@link DublinCoreCatalog} representing series
   * @throws SeriesServiceDatabaseException
   *           if indexing fails and synchronous indexing is enabled
   */
  void updateIndex(DublinCoreCatalog dublinCore) throws SeriesServiceDatabaseException;

  /**
   * Index access control for existing series entry.
   *
   * @param seriesId
   *          ID of series for which access control will be associated with
   * @param accessControl
   *          {@link AccessControlList} defining access control rules
   * @throws NotFoundException
   *           if series with specified ID does not exist
   * @throws SeriesServiceDatabaseException
   *           if exception occurred and synchronous indexing is enabled
   */
  void updateSecurityPolicy(String seriesId, AccessControlList accessControl) throws NotFoundException,
          SeriesServiceDatabaseException;

  /**
   * Removes series from index.
   *
   * @param seriesID
   *          ID of the series to be removed
   * @throws SeriesServiceDatabaseException
   *           if removing fails and synchronous indexing is enabled
   */
  void delete(String seriesID) throws SeriesServiceDatabaseException;

  /**
   * Gets Dublin core representing series.
   *
   * @param seriesID
   *          series to be retrieved
   * @return {@link DublinCoreCatalog} representing series
   * @throws SeriesServiceDatabaseException
   *           if retrieval fails
   * @throws NotFoundException
   *           if no such series exists
   */
  DublinCoreCatalog getDublinCore(String seriesID) throws SeriesServiceDatabaseException, NotFoundException;

  /**
   * Retrieves access control for series with specified ID.
   *
   * @param seriesID
   *          ID of the series for which access control will be retrieved
   * @return {@link AccessControlList} for series with specified ID
   * @throws NotFoundException
   *           if no such series exists
   * @throws SeriesServiceDatabaseException
   *           if exception occurred
   */
  AccessControlList getAccessControl(String seriesID) throws NotFoundException, SeriesServiceDatabaseException;

  /**
   * Search over indexed series with query.
   *
   * @param query
   *          {@link SeriesQuery} object storing query parameters
   * @return List of all matching series
   * @throws SeriesServiceDatabaseException
   *           if query cannot be executed
   */
  DublinCoreCatalogList search(SeriesQuery query) throws SeriesServiceDatabaseException;

  /**
   * Query Id and title of all series
   *
   * @return Map of series Id to series title for all series
   * @throws SeriesServiceDatabaseException
   *           if query can not be executed
   */
  Map<String, String> queryIdTitleMap() throws SeriesServiceDatabaseException;

  /**
   * Returns number of series in search index, across all organizations.
   *
   * @return number of series in search index
   * @throws SeriesServiceDatabaseException
   *           if count cannot be retrieved
   */
  long count() throws SeriesServiceDatabaseException;

}
