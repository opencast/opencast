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

import org.opencastproject.util.Checksum
import org.opencastproject.util.MimeType

import com.entwinemedia.fn.data.Opt

import java.io.InputStream

/**
 * An asset is a [org.opencastproject.mediapackage.MediaPackageElement] under the control of the [AssetManager].
 */
interface Asset {
    /** Return the identifier of the asset.  */
    val id: AssetId

    /**
     * Return a stream to the asset data. A client is responsible of closing the stream after consumption.
     * Use the *try with resource* construct which is available from Java 7 onwards if possible.
     *
     *
     * If the asset is currently not [available][.getAvailability] an empty input stream is
     * returned.
     */
    val inputStream: InputStream

    /** Mime type of the asset.  */
    val mimeType: Opt<MimeType>

    /** Size of the asset in bytes.  */
    val size: Long

    /** Tell about the availability of the asset.  */
    val availability: Availability

    /** Get the store ID of the asset store where this snapshot currently lives  */
    val storageId: String

    /** Get the checksum  */
    val checksum: Checksum
}
