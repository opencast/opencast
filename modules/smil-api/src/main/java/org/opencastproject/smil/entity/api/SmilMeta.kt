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

package org.opencastproject.smil.entity.api

/**
 * Represent a SMIL meta element.
 */
interface SmilMeta : SmilObject {

    /**
     * Returns meta content.
     *
     * @return the content
     */
    val content: String

    /**
     * Returns meta name.
     *
     * @return the name
     */
    val name: String

    companion object {

        val SMIL_META_NAME_WORKFLOW_ID = "workflow-id"
        val SMIL_META_NAME_MEDIA_PACKAGE_ID = "media-package-id"
        val SMIL_META_NAME_CATALOG_ID = "catalog-id"
        val SMIL_META_NAME_TRACK_ID = "track-id"
        val SMIL_META_NAME_TRACK_DURATION = "track-duration"
    }
}
