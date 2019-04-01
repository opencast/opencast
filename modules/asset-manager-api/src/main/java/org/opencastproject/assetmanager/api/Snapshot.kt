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
package org.opencastproject.assetmanager.api

import org.opencastproject.mediapackage.MediaPackage

import java.util.Date

/**
 * A versioned snapshot of a [MediaPackage] under the control of the [AssetManager].
 */
interface Snapshot {
    /** Get the version.  */
    val version: Version

    /** Get the ID of the organization where this media package belongs to.  */
    val organizationId: String

    /** Tell about when this version of the episode has been stored in the AssetManager.  */
    val archivalDate: Date

    /** Get the availability of the media package's assets.  */
    val availability: Availability

    /** Get the store ID of the asset store where this snapshot currently lives  */
    val storageId: String

    /** Get the owner of the snapshot.  */
    val owner: String

    /**
     * Get the media package.
     *
     *
     * Implementations are required to provide media package element URIs that point to some valid HTTP endpoint.
     */
    val mediaPackage: MediaPackage
}
