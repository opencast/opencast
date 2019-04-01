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

package org.opencastproject.message.broker.api.theme

import java.io.Serializable
import java.util.Date

/**
 * Business object of themes class
 */
class SerializableTheme(val id: Long?, creationDate: Date, val isDefault: Boolean, val creator: String, val name: String,
                        val description: String, val isBumperActive: Boolean, val bumperFile: String, val isTrailerActive: Boolean, val trailerFile: String,
                        val isTitleSlideActive: Boolean, val titleSlideMetadata: String, val titleSlideBackground: String, val isLicenseSlideActive: Boolean,
                        val licenseSlideBackground: String, val licenseSlideDescription: String, val isWatermarkActive: Boolean, val watermarkFile: String,
                        val watermarkPosition: String) : Serializable {
    private val creationDate: Long

    init {
        this.creationDate = creationDate.time
    }

    fun getCreationDate(): Date {
        return Date(creationDate)
    }

    override fun toString(): String {
        return StringBuilder(java.lang.Long.toString(id)).append(":").append(name).toString()
    }

    companion object {

        private const val serialVersionUID = 618342361307578393L
    }

}
