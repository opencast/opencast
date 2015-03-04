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

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import java.util.Iterator;
import java.util.Map;

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
   * Removes the series property from persistent storage.
   *
   * @param seriesId
   *          ID of the series to be removed.
   * @param propertyName
   *          The unique name of the property to delete.
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   * @throws NotFoundException
   *           if series with specified ID is not found or if there is no property with the property name
   */
  void deleteSeriesProperty(String seriesId, String propertyName) throws SeriesServiceDatabaseException,
          NotFoundException;

  /**
   * Returns all series in persistent storage.
   *
   * @return {@link Tuple} array representing stored series
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   */
  Iterator<Tuple<DublinCoreCatalog, String>> getAllSeries() throws SeriesServiceDatabaseException;

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

  /**
   * Get the properties for particular series
   *
   * @param seriesId
   *          The unique series id to retrieve the properties for.
   * @return A {@link Map} of the properties names and values.
   * @throws NotFoundException
   *           Thrown if the series can't be found.
   * @throws SeriesServiceDatabaseException
   *           If exception occurred
   */
  Map<String, String> getSeriesProperties(String seriesId) throws NotFoundException, SeriesServiceDatabaseException;

  /**
   * Get a series property if it exists
   *
   * @param seriesId
   *          The id used to get the series.
   * @param propertyName
   *          The unique name for the property.
   * @return The property value
   * @throws NotFoundException
   *           Thrown if the series or the property doesn't exist.
   * @throws SeriesServiceDatabaseException
   *           Thrown for all other Exceptions.
   */
  String getSeriesProperty(String seriesId, String propertyName) throws NotFoundException,
          SeriesServiceDatabaseException;

  int countSeries() throws SeriesServiceDatabaseException;

  /**
   * Updates a series' property.
   *
   * @param seriesId
   *          The id of the series to add or update the property for.
   * @param propertyName
   *          A unique name for the property.
   * @param propertyValue
   *          The value for the property.
   * @throws NotFoundException
   *           Thrown if the series cannot be found.
   * @throws SeriesServiceDatabaseException
   *           Thrown if another exception occurred
   */
  void updateSeriesProperty(String seriesId, String propertyName, String propertyValue) throws NotFoundException,
          SeriesServiceDatabaseException;

  /**
   * Returns the opt out status of series with the given series id
   *
   * @param seriesId
   *          the series id
   * @return the opt out status
   * @throws NotFoundException
   *           if there is no series with specified series ID
   * @throws SeriesServiceDatabaseException
   *           if exception occurred
   */
  boolean isOptOut(String seriesId) throws NotFoundException, SeriesServiceDatabaseException;

  /**
   * Updates a series' opt out status.
   *
   * @param seriesId
   *          The id of the series to update the opt out status of.
   * @param optOut
   *          Whether to opt out this series or not.
   * @throws NotFoundException
   *           if there is no series with specified series ID
   * @throws SeriesServiceDatabaseException
   *           if exception occurred
   */
  void updateOptOutStatus(String seriesId, boolean optOut) throws NotFoundException, SeriesServiceDatabaseException;

  /**
   * Returns true if the series with the given identifier contains an element with the given type.
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the element type
   * @return true, if the element exits; false otherwise
   * @throws SeriesServiceDatabaseException
   *           if there was an error while checking if the element exits
   */
  boolean existsSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException;

  /**
   * Adds or updates a series element in the series service database.
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the element type
   * @param data
   *          the element data
   * @return true if the element could be added/updated; false if no such series exists
   * @throws SeriesServiceDatabaseException
   *           if an error occurs while saving the element in the database
   */
  boolean storeSeriesElement(String seriesId, String type, byte[] data) throws SeriesServiceDatabaseException;

  /**
   * Deletes an element of a given type from a series
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the element type
   * @return true if the element could be deleted; false if no such series/element exists
   * @throws SeriesServiceDatabaseException
   *           if an error occurs while removing the element from the database
   */
  boolean deleteSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException;

  /**
   * Returns the data of a series element.
   * 
   * @param seriesId
   *          the series identifier
   * @param type
   *          the element type
   * @return the element data or {@code Opt.none()} if no such series/element exists
   * @throws SeriesServiceDatabaseException
   *           if an error occurs while retrieving the element from the database
   */
  Opt<byte[]> getSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException;

  /**
   * Returns all elements of a series or an empty map if the series does not contain any elements. The key of the map
   * marks the element type.
   * 
   * @param seriesId
   *          the series identifier
   * @return a map of series elements or {@code Opt.none()} if no such series exists
   * @throws SeriesServiceDatabaseException
   *           if an error occurs while retrieving the element from the database
   */
  Opt<Map<String, byte[]>> getSeriesElements(String seriesId) throws SeriesServiceDatabaseException;

}
