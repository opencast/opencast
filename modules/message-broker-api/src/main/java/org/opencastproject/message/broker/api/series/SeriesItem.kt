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

package org.opencastproject.message.broker.api.series

import org.opencastproject.message.broker.api.MessageItem
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser

import java.io.IOException
import java.io.Serializable

/**
 * [Serializable] class that represents all of the possible messages sent through a SeriesService queue.
 */
class SeriesItem
/**
 * Constructor to build an [SeriesItem] with given parameters.
 *
 * @param type
 * The update type.
 * @param seriesId
 * The series ID to update. If you provide both, the seriesId and the series DublinCore catalog,
 * the seriesId must match the value of [DublinCore.PROPERTY_IDENTIFIER].
 * @param series
 * The series DublinCore catalog to update. The value of [DublinCore.PROPERTY_IDENTIFIER] must match
 * the value of seriesId if you provide both.
 * @param acl
 * The series ACL to update. Note: the series ID must be also provided.
 * @param propertyName
 * The name of the series property to update. Note: the series ID and property value must be also provided.
 * @param propertyValue
 * The value of the series property to update. Note: the series ID and property name must be also provided.
 * @param elementType
 * The type of the series element to update. Note: the series ID and element must be also provided.
 * @param element
 * The series element to update. Note: the series ID and element type must be also provided.
 *
 * @throws IllegalStateException
 * If the series ID and the series are not provided or the series ID and the value of
 * [DublinCore.PROPERTY_IDENTIFIER] in the series catalog does not match.
 */
private constructor(val type: Type, seriesId: String?, series: DublinCoreCatalog?, private val acl: String?, val propertyName: String,
                    val propertyValue: String, val elementType: String, val element: String, overrideEpisodeAcl: Boolean?) : MessageItem, Serializable {
    override val id: String?
    private val series: String?
    private val overrideEpisodeAcl: String?

    val metadata: DublinCoreCatalog
        get() = DublinCoreXmlFormat.readOpt(series).orNull()

    val extendedMetadata: DublinCoreCatalog?
        get() {
            try {
                return DublinCoreXmlFormat.read(element)
            } catch (ex: Exception) {
                return null
            }

        }

    enum class Type {
        UpdateCatalog, UpdateElement, UpdateAcl, UpdateProperty, Delete
    }

    init {
        if (seriesId != null && series != null && seriesId != series.getFirst(DublinCore.PROPERTY_IDENTIFIER))
            throw IllegalStateException("Provided series ID and dublincore series ID does not match")
        if (series != null) {
            try {
                this.series = series.toXmlString()
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }

        } else {
            this.series = null
        }
        if (seriesId != null)
            this.id = seriesId
        else if (series != null)
            this.id = series.getFirst(DublinCore.PROPERTY_IDENTIFIER)
        else
            throw IllegalStateException("Neither series nor series ID is provided")
        this.overrideEpisodeAcl = overrideEpisodeAcl?.toString()
    }

    fun getSeriesId(): String? {
        return id
    }

    fun getAcl(): AccessControlList? {
        try {
            return if (acl == null) null else AccessControlParser.parseAcl(acl)
        } catch (e: Exception) {
            throw IllegalStateException()
        }

    }

    fun getOverrideEpisodeAcl(): Boolean? {
        return if (overrideEpisodeAcl == null) null else java.lang.Boolean.parseBoolean(overrideEpisodeAcl)
    }

    companion object {

        private const val serialVersionUID = 3275142857854793612L

        val SERIES_QUEUE_PREFIX = "SERIES."

        val SERIES_QUEUE = SERIES_QUEUE_PREFIX + "QUEUE"

        /**
         * @param series
         * The series to update.
         * @return Builds [SeriesItem] for updating a series.
         */
        fun updateCatalog(series: DublinCoreCatalog): SeriesItem {
            return SeriesItem(Type.UpdateCatalog, null, series, null, null, null, null, null, null)
        }

        /**
         * @param seriesId
         * The unique id for the series to update.
         * @param type
         * The type of series element.
         * @param data
         * The series element data.
         * @return Builds [SeriesItem] for updating series element.
         */
        fun updateElement(seriesId: String, type: String, data: String): SeriesItem {
            return SeriesItem(Type.UpdateElement, seriesId, null, null, null, null, type, data, null)
        }

        /**
         * @param seriesId
         * The unique id for the series to update.
         * @param acl
         * The new access control list to update to.
         * @param overrideEpisodeAcl
         * Whether to override the episode ACL.
         * @return Builds [SeriesItem] for updating the access control list of a series.
         */
        fun updateAcl(seriesId: String, acl: AccessControlList, overrideEpisodeAcl: Boolean): SeriesItem {
            return SeriesItem(Type.UpdateAcl, seriesId, null, AccessControlParser.toJsonSilent(acl), null, null, null, null, overrideEpisodeAcl)
        }

        /**
         * @param seriesId
         * The unique id for the series to update.
         * @param propertyName
         * the property name
         * @param propertyValue
         * the property value
         * @return Builds [SeriesItem] for updating a series property.
         */
        fun updateProperty(seriesId: String, propertyName: String, propertyValue: String): SeriesItem {
            return SeriesItem(Type.UpdateProperty, seriesId, null, null, propertyName, propertyValue, null, null, null)
        }

        /**
         * @param seriesId
         * The unique id of the series to delete.
         * @return Builds [SeriesItem] for deleting a series.
         */
        fun delete(seriesId: String): SeriesItem {
            return SeriesItem(Type.Delete, seriesId, null, null, null, null, null, null, null)
        }
    }
}
