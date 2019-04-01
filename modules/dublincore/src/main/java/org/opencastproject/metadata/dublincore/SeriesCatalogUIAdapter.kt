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
package org.opencastproject.metadata.dublincore

import com.entwinemedia.fn.data.Opt

/**
 * A [SeriesCatalogUIAdapter] converts between a concrete [org.opencastproject.metadata.api.MetadataCatalog]
 * implementation and a [MetadataCollection] that
 */
interface SeriesCatalogUIAdapter {

    /**
     * Returns the name of the organization (tenant) this catalog UI adapter belongs to or `Opt.none()` if this is a
     * tenant-agnostic adapter.
     *
     * @return The organization name or `Opt.none()`
     */
    val organization: String

    /**
     * Returns the media type of catalogs managed by this catalog UI adapter.
     *
     * @return The media type of the catalog
     */
    val flavor: String

    /**
     * @return Get the human readable title for this catalog ui adapter for various languages.
     */
    val uiTitle: String

    /**
     * Returns all fields of this catalog in a raw data format. This is a good starting point to create a new instance of
     * this catalog.
     *
     * @return The fields with raw data
     */
    val rawFields: MetadataCollection

    /**
     * Returns all fields of this catalog containing the data in an abstract, editable form. If the series cannot be
     * found, `Opt.none()` is returned.
     *
     * @param seriesId
     * The series identifer
     * @return Get the field names and values for this catalog.
     */
    fun getFields(seriesId: String): Opt<MetadataCollection>

    /**
     * Store changes made to the fields of the metadata collection in the catalog and return an updated version of it.
     *
     * @param seriesId
     * The series identifier
     * @param metadata
     * The new metadata to update the mediapackage with
     * @return true, if the metadata could be saved successfully
     */
    fun storeFields(seriesId: String, metadata: MetadataCollection): Boolean

}
