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
package org.opencastproject.series.impl;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

/**
 * API that defines persistent storage of series.
 * 
 */
public interface SeriesServiceDatabase {

  /**
   * Store (or update) series.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} representing series
   * @return Dublin Core catalog representing newly created series or null if series Dublin Core was updated
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   */
  DublinCoreCatalog storeSeries(DublinCoreCatalog dc) throws SeriesServiceDatabaseException, UnauthorizedException;

  /**
   * Store access control associated with specified series. IllegalArgumentException is thrown if accessControl
   * parameter is null.
   * 
   * @param seriesID
   *          ID of series to associate access control with
   * @param accessControl
   *          {@link AccessControlList} representing access control rules for specified series
   * @return true if update happened, false if there was no previous entry
   * @throws NotFoundException
   *           if series with specified ID does not exist
   * @throws SeriesServiceDatabaseException
   *           if exception occurred
   */
  boolean storeSeriesAccessControl(String seriesID, AccessControlList accessControl) throws NotFoundException,
          SeriesServiceDatabaseException;

  /**
   * Removes series from persistent storage.
   * 
   * @param seriesId
   *          ID of the series to be removed
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   * @throws NotFoundException
   *           if series with specified ID is not found
   */
  void deleteSeries(String seriesId) throws SeriesServiceDatabaseException, NotFoundException;

  /**
   * Returns all series in persistent storage.
   * 
   * @return {@link DublinCoreCatalog} array representing stored series
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   */
  DublinCoreCatalog[] getAllSeries() throws SeriesServiceDatabaseException;

  /**
   * Retrieves ACL for series with given ID.
   * 
   * @param seriesID
   *          series for which ACL will be retrieved
   * @return {@link AccessControlList} of series or null if series does not have ACL associated with it
   * @throws NotFoundException
   *           if series with given ID does not exist
   * @throws SeriesServiceDatabaseException
   *           if exception occurred
   */
  AccessControlList getAccessControlList(String seriesID) throws NotFoundException, SeriesServiceDatabaseException;

  /**
   * Gets a single series by its identifier.
   * 
   * @param seriesId
   *          the series identifier
   * @return the dublin core catalog for this series
   * @throws NotFoundException
   *           if there is no series with this identifier
   * @throws SeriesServiceDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  DublinCoreCatalog getSeries(String seriesId) throws NotFoundException, SeriesServiceDatabaseException;

  int countSeries() throws SeriesServiceDatabaseException;
}
