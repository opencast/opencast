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

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor

/**
 * An interface class to support the creation of data providers.
 */
interface EventCatalogUIAdapter {

    /**
     * @return Return the tenant organization for this catalog ui adapter.
     */
    val organization: String

    /**
     * @return Return the flavor for this catalog ui adapter.
     */
    val flavor: MediaPackageElementFlavor

    /**
     * @return Get the human readable title for this catalog ui adapter for various languages.
     */
    val uiTitle: String

    /**
     * @return All of the fields with empty values for populating a new object.
     */
    val rawFields: MetadataCollection

    /**
     * @return Get the field names and values for this catalog.
     */
    fun getFields(mediapackage: MediaPackage): MetadataCollection

    /**
     * Store a change in the metadata into the mediapackage as a [Catalog]
     *
     * @param mediapackage
     * The mediapackage to update
     * @param metadataCollection
     * The new metadata to update the mediapackage with
     * @return the stored catalog
     */
    fun storeFields(mediapackage: MediaPackage, metadataCollection: MetadataCollection): Catalog

}
