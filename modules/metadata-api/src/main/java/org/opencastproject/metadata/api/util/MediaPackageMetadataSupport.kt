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


package org.opencastproject.metadata.api.util

import org.apache.commons.lang3.StringUtils.isNotBlank

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.metadata.api.MediaPackageMetadata

/**
 * This class contains useful methods to work with MediaPackageMetadata.
 */
object MediaPackageMetadataSupport {

    /**
     * Updates all of the given MediaPackage's meta data with the given MediaPackageMetadata content.
     */
    fun populateMediaPackageMetadata(mp: MediaPackage, metadata: MediaPackageMetadata?) {
        if (metadata == null) {
            return
        }

        // Series identifier
        if (isNotBlank(metadata.seriesIdentifier)) {
            mp.series = metadata.seriesIdentifier
        }

        // Series title
        if (isNotBlank(metadata.seriesTitle)) {
            mp.seriesTitle = metadata.seriesTitle
        }

        // Episode title
        if (isNotBlank(metadata.title)) {
            mp.title = metadata.title
        }

        // Episode date
        if (metadata.date != null) {
            mp.date = metadata.date
        }

        // Episode subjects
        if (metadata.subjects.size > 0) {
            if (mp.subjects != null) {
                for (subject in mp.subjects) {
                    mp.removeSubject(subject)
                }
            }
            for (subject in metadata.subjects) {
                mp.addSubject(subject)
            }
        }

        // Episode contributers
        if (metadata.contributors.size > 0) {
            if (mp.contributors != null) {
                for (contributor in mp.contributors) {
                    mp.removeContributor(contributor)
                }
            }
            for (contributor in metadata.contributors) {
                mp.addContributor(contributor)
            }
        }

        // Episode creators
        if (mp.creators.size == 0 && metadata.creators.size > 0) {
            if (mp.creators != null) {
                for (creator in mp.creators) {
                    mp.removeCreator(creator)
                }
            }
            for (creator in metadata.creators) {
                mp.addCreator(creator)
            }
        }

        // Episode license
        if (isNotBlank(metadata.license)) {
            mp.license = metadata.license
        }

        // Episode language
        if (isNotBlank(metadata.language)) {
            mp.language = metadata.language
        }

    }

}
