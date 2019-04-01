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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.series.api

import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.util.NotFoundException

import com.entwinemedia.fn.data.Opt

/**
 * Series service API for creating, removing and searching over series.
 *
 */
interface SeriesService {

    /**
     * Returns a map of series Id to title of all series the user can access
     *
     * @return a map of series Id to title of all series the user can access
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     * @throws SeriesException
     * if query could not be performed
     */
    val idTitleMapOfAllSeries: Map<String, String>

    val seriesCount: Int

    /**
     * Adds or updates series. IllegalArgumentException is thrown if dc argument is null.
     *
     * @param dc
     * [DublinCoreCatalog] representing series
     * @return Dublin Core catalog of newly created series or null if series Dublin Core was just updated
     * @throws SeriesException
     * if adding or updating fails
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     */
    @Throws(SeriesException::class, UnauthorizedException::class)
    fun updateSeries(dc: DublinCoreCatalog): DublinCoreCatalog


    /**
     * Updates access control rules for specified series. Not specifying series ID or trying to update series with null
     * value will throw IllegalArgumentException.
     *
     * @param seriesID
     * series to be updated
     * @param accessControl
     * [AccessControlList] defining access control rules
     * @return true if ACL was updated and false it if was created
     * @throws NotFoundException
     * if series with given ID cannot be found
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     * @throws SeriesException
     * if exception occurred
     */
    @Throws(NotFoundException::class, SeriesException::class, UnauthorizedException::class)
    fun updateAccessControl(seriesID: String, accessControl: AccessControlList): Boolean

    /**
     * Updates access control rules for specified series. Allows to set the override parameter that controls whether the
     * episode ACLs of the contained media packages will be removed on update. Not specifying series ID or trying to update series with null
     * value will throw IllegalArgumentException.
     *
     * @param seriesID
     * series to be updated
     * @param accessControl
     * [AccessControlList] defining access control rules
     * @param overrideEpisodeAcl
     * Whether the new series acl should override the episode acl
     * @return true if ACL was updated and false it if was created
     * @throws NotFoundException
     * if series with given ID cannot be found
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     * @throws SeriesException
     * if exception occurred
     */
    @Throws(NotFoundException::class, SeriesException::class, UnauthorizedException::class)
    fun updateAccessControl(seriesID: String, accessControl: AccessControlList, overrideEpisodeAcl: Boolean): Boolean

    /**
     * Removes series
     *
     * @param seriesID
     * ID of the series to be removed
     * @throws SeriesException
     * if deleting fails
     * @throws NotFoundException
     * if series with specified ID does not exist
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     */
    @Throws(SeriesException::class, NotFoundException::class, UnauthorizedException::class)
    fun deleteSeries(seriesID: String)

    /**
     * Returns Dublin core representing series by series ID.
     *
     * @param seriesID
     * series to be retrieved
     * @return [DublinCoreCatalog] representing series
     * @throws SeriesException
     * if retrieving fails
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     */
    @Throws(SeriesException::class, NotFoundException::class, UnauthorizedException::class)
    fun getSeries(seriesID: String): DublinCoreCatalog

    /**
     * Returns access control rules for series with given ID.
     *
     * @param seriesID
     * ID of the series for which access control rules will be retrieved
     * @return [AccessControlList] defining access control rules
     * @throws NotFoundException
     * if series with given ID cannot be found
     * @throws SeriesException
     * if exception occurred
     */
    @Throws(NotFoundException::class, SeriesException::class)
    fun getSeriesAccessControl(seriesID: String): AccessControlList

    /**
     * Search over series
     *
     * @param query
     * [SeriesQuery] representing query
     * @return List of all matching series
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     * @throws SeriesException
     * if query could not be performed
     */
    @Throws(SeriesException::class, UnauthorizedException::class)
    fun getSeries(query: SeriesQuery): DublinCoreCatalogList

    /**
     * Returns all the elements of a series in a map. The key of the map marks the element type. If the series does not
     * contain any elements, an empty map is returned. If the series does not exist, `Opt.none()` is returned.
     *
     * @param seriesId
     * the series identifier
     * @return the map of elements
     * @throws SeriesException
     * if an error occurred during loading the series elements
     */
    @Throws(SeriesException::class)
    fun getSeriesElements(seriesId: String): Opt<Map<String, ByteArray>>

    /**
     * Returns the element data of the series with the given type. If the series or the element with the given type do not
     * exist, `Opt.none()` is returned.
     *
     * @param seriesId
     * the series identifier
     * @param type
     * the element type
     * @return the series element data
     * @throws SeriesException
     * if an error occurred during loading the series element
     */
    @Throws(SeriesException::class)
    fun getSeriesElementData(seriesId: String, type: String): Opt<ByteArray>

    /**
     * Adds a new element to a series.
     *
     * @param seriesId
     * the series identifier
     * @param type
     * the type of the new element
     * @param data
     * the data of the new element
     * @return true, if the element could be added; false if an element with the given key already exists or if there is
     * no series with the given identifier.
     * @throws SeriesException
     * if an error occurs while saving the element
     */
    @Throws(SeriesException::class)
    fun addSeriesElement(seriesId: String, type: String, data: ByteArray): Boolean

    /**
     * Updates an existing element of a series.
     *
     * @param seriesId
     * the series identifier
     * @param type
     * the type of the new element
     * @param data
     * the data of the new element
     * @return true if the element could be updated; false if no such element/series exists
     * @throws SeriesException
     * if an error occurs while updating the element
     */
    @Throws(SeriesException::class)
    fun updateSeriesElement(seriesId: String, type: String, data: ByteArray): Boolean

    /**
     * Deletes an element from a series.
     *
     * @param seriesId
     * the series identifier
     * @param type
     * the element type
     * @return true if the element could be deleted; false if no such element/series exists
     * @throws SeriesException
     * if an error occurs while deleting the element
     */
    @Throws(SeriesException::class)
    fun deleteSeriesElement(seriesId: String, type: String): Boolean

    /**
     * Returns the properties for a series.
     *
     * @param seriesID
     * series to be retrieved
     * @return representing series properties
     * @throws SeriesException
     * if retrieving fails
     * @throws NotFoundException
     * Thrown if the series or property cannot be found.
     * @throws UnauthorizedException
     * if the current user is not authorized to perform this action
     */
    @Throws(SeriesException::class, NotFoundException::class, UnauthorizedException::class)
    fun getSeriesProperties(seriesID: String): Map<String, String>

    /**
     * Get a series property.
     *
     * @param seriesID
     * The id of the series to get the property from.
     * @param propertyName
     * The name of the property to retrieve
     * @return The string value of the property.
     * @throws SeriesException
     * Thrown for all other exceptions.
     * @throws NotFoundException
     * Thrown if the series or property cannot be found.
     * @throws UnauthorizedException
     * Thrown if the user is not able to read the series' properties.
     */
    @Throws(SeriesException::class, NotFoundException::class, UnauthorizedException::class)
    fun getSeriesProperty(seriesID: String, propertyName: String): String

    /**
     * Update a series property or create a new one if it doesn't exist.
     *
     * @param seriesID
     * The series to attach the property to.
     * @param propertyName
     * The unique name of the series property
     * @param propertyValue
     * The value to assign the series property
     * @throws SeriesException
     * Thrown for all other exceptions.
     * @throws NotFoundException
     * Thrown if the series or property cannot be found.
     * @throws UnauthorizedException
     * Thrown if the user is not able to write the series' properties.
     */
    @Throws(SeriesException::class, NotFoundException::class, UnauthorizedException::class)
    fun updateSeriesProperty(seriesID: String, propertyName: String, propertyValue: String)

    /**
     *
     * @param seriesID
     * The series to attach the property to.
     * @param propertyName
     * The unique name of the series property
     * @throws SeriesException
     * Thrown for all other exceptions.
     * @throws NotFoundException
     * Thrown if the series or property cannot be found.
     * @throws UnauthorizedException
     * Thrown if the user is not able to write the series' properties.
     */
    @Throws(SeriesException::class, NotFoundException::class, UnauthorizedException::class)
    fun deleteSeriesProperty(seriesID: String, propertyName: String)

    companion object {

        /**
         * Identifier for service registration and location
         */
        val JOB_TYPE = "org.opencastproject.series"
    }
}
