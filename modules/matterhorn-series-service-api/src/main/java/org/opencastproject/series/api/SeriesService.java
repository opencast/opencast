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

package org.opencastproject.series.api;

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import java.util.Map;

/**
 * Series service API for creating, removing and searching over series.
 *
 */
public interface SeriesService {

  /**
   * Identifier for service registration and location
   */
  String JOB_TYPE = "org.opencastproject.series";

  /**
   * Adds or updates series. IllegalArgumentException is thrown if dc argument is null.
   *
   * @param dc
   *          {@link DublinCoreCatalog} representing series
   * @return Dublin Core catalog of newly created series or null if series Dublin Core was just updated
   * @throws SeriesException
   *           if adding or updating fails
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   */
  DublinCoreCatalog updateSeries(DublinCoreCatalog dc) throws SeriesException, UnauthorizedException;

  /**
   * Updates access control rules for specified series. Not specifying series ID or trying to update series with null
   * value will throw IllegalArgumentException.
   *
   * @param seriesID
   *          series to be updated
   * @param accessControl
   *          {@link AccessControlList} defining access control rules
   * @return true if ACL was updated and false it if was created
   * @throws NotFoundException
   *           if series with given ID cannot be found
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   * @throws SeriesException
   *           if exception occurred
   */
  boolean updateAccessControl(String seriesID, AccessControlList accessControl) throws NotFoundException,
          SeriesException, UnauthorizedException;

  /**
   * Removes series
   *
   * @param seriesID
   *          ID of the series to be removed
   * @throws SeriesException
   *           if deleting fails
   * @throws NotFoundException
   *           if series with specified ID does not exist
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   */
  void deleteSeries(String seriesID) throws SeriesException, NotFoundException, UnauthorizedException;

  /**
   * Returns Dublin core representing series by series ID.
   *
   * @param seriesID
   *          series to be retrieved
   * @return {@link DublinCoreCatalog} representing series
   * @throws SeriesException
   *           if retrieving fails
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   */
  DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException, UnauthorizedException;

  /**
   * Returns access control rules for series with given ID.
   *
   * @param seriesID
   *          ID of the series for which access control rules will be retrieved
   * @return {@link AccessControlList} defining access control rules
   * @throws NotFoundException
   *           if series with given ID cannot be found
   * @throws SeriesException
   *           if exception occurred
   */
  AccessControlList getSeriesAccessControl(String seriesID) throws NotFoundException, SeriesException;

  /**
   * Search over series
   *
   * @param query
   *          {@link SeriesQuery} representing query
   * @return List of all matching series
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   * @throws SeriesException
   *           if query could not be performed
   */
  DublinCoreCatalogList getSeries(SeriesQuery query) throws SeriesException, UnauthorizedException;

  /**
   * Returns all the elements of a series in a map. The key of the map marks the element type. If the series does not
   * contain any elements, an empty map is returned. If the series does not exist, {@code Opt.none()} is returned.
   * 
   * @param seriesId
   *          the series identifier
   * @return the map of elements
   * @throws SeriesException
   *           if an error occurred during loading the series elements
   */
  Opt<Map<String, byte[]>> getSeriesElements(String seriesId) throws SeriesException;

  /**
   * Returns the element data of the series with the given type. If the series or the element with the given type do not
   * exist, {@code Opt.none()} is returned.
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the element type
   * @return the series element data
   * @throws SeriesException
   *           if an error occurred during loading the series element
   */
  Opt<byte[]> getSeriesElementData(String seriesId, String type) throws SeriesException;

  /**
   * Adds a new element to a series.
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the type of the new element
   * @param data
   *          the data of the new element
   * @return true, if the element could be added; false if an element with the given key already exists or if there is
   *         no series with the given identifier.
   * @throws SeriesException
   *           if an error occurs while saving the element
   */
  boolean addSeriesElement(String seriesId, String type, byte[] data) throws SeriesException;

  /**
   * Updates an existing element of a series.
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the type of the new element
   * @param data
   *          the data of the new element
   * @return true if the element could be updated; false if no such element/series exists
   * @throws SeriesException
   *           if an error occurs while updating the element
   */
  boolean updateSeriesElement(String seriesId, String type, byte[] data) throws SeriesException;

  /**
   * Deletes an element from a series.
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the element type
   * @return true if the element could be deleted; false if no such element/series exists
   * @throws SeriesException
   *           if an error occurs while deleting the element
   */
  boolean deleteSeriesElement(String seriesId, String type) throws SeriesException;

  int getSeriesCount() throws SeriesException;

  /**
   * Returns the opt out status of series with the given series id
   *
   * @param seriesId
   *          the series id
   * @return the opt out status
   * @throws NotFoundException
   *           if there is no series with specified series ID
   * @throws SeriesException
   *           if exception occurred
   */
  boolean isOptOut(String seriesId) throws NotFoundException, SeriesException;

  /**
   * Updates a series' opt out status.
   *
   * @param seriesId
   *          The id of the series to update the opt out status of.
   * @param optOut
   *          Whether to opt out this series or not.
   * @throws NotFoundException
   *           if there is no series with specified series ID
   * @throws SeriesException
   *           if exception occurred
   */
  void updateOptOutStatus(String seriesId, boolean optOut) throws NotFoundException, SeriesException;

  /**
   * Returns the properties for a series.
   *
   * @param seriesID
   *          series to be retrieved
   * @return {@link Map<String, String>} representing series properties
   * @throws SeriesException
   *           if retrieving fails
   * @throws NotFoundException
   *           Thrown if the series or property cannot be found.
   * @throws UnauthorizedException
   *           if the current user is not authorized to perform this action
   */
  Map<String, String> getSeriesProperties(String seriesID) throws SeriesException, NotFoundException,
          UnauthorizedException;

  /**
   * Get a series property.
   *
   * @param seriesID
   *          The id of the series to get the property from.
   * @param propertyName
   *          The name of the property to retrieve
   * @return The string value of the property.
   * @throws SeriesException
   *           Thrown for all other exceptions.
   * @throws NotFoundException
   *           Thrown if the series or property cannot be found.
   * @throws UnauthorizedException
   *           Thrown if the user is not able to read the series' properties.
   */
  String getSeriesProperty(String seriesID, String propertyName) throws SeriesException, NotFoundException,
          UnauthorizedException;

  /**
   * Update a series property or create a new one if it doesn't exist.
   *
   * @param seriesID
   *          The series to attach the property to.
   * @param propertyName
   *          The unique name of the series property
   * @param propertyValue
   *          The value to assign the series property
   * @throws SeriesException
   *           Thrown for all other exceptions.
   * @throws NotFoundException
   *           Thrown if the series or property cannot be found.
   * @throws UnauthorizedException
   *           Thrown if the user is not able to write the series' properties.
   */
  void updateSeriesProperty(String seriesID, String propertyName, String propertyValue) throws SeriesException,
          NotFoundException, UnauthorizedException;

  /**
   *
   * @param seriesID
   *          The series to attach the property to.
   * @param propertyName
   *          The unique name of the series property
   * @throws SeriesException
   *           Thrown for all other exceptions.
   * @throws NotFoundException
   *           Thrown if the series or property cannot be found.
   * @throws UnauthorizedException
   *           Thrown if the user is not able to write the series' properties.
   */
  void deleteSeriesProperty(String seriesID, String propertyName) throws SeriesException, NotFoundException,
          UnauthorizedException;
}
