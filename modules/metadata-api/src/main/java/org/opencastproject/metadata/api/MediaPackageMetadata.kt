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

package org.opencastproject.metadata.api

import java.util.Date

/**
 * Provides metadata for a [MediaPackageMetadata]
 *
 */
interface MediaPackageMetadata {

    /**
     * Returns the title for the associated series, if any.
     *
     * @return The series title
     */
    val seriesTitle: String

    /**
     * Returns the title of the episode that this mediapackage represents.
     *
     * @return The episode title
     */
    val title: String

    /**
     * The names of the creators. If no creators were specified, an empty array is returned.
     *
     * @return the creators for this mediapackage
     */
    val creators: Array<String>

    /**
     * The series, if any, that this episode belongs to.
     *
     * @return the series for this mediapackage
     */
    val seriesIdentifier: String

    /**
     * The license under which this episode is available
     *
     * @return the license for this mediapackage
     */
    val license: String

    /**
     * The contributors. If no contributors were specified, an empty array is returned.
     *
     * @return the contributors for this mediapackage
     */
    val contributors: Array<String>

    /**
     * The language spoken in the media
     *
     * @return the language for this mediapackage
     */
    val language: String

    /**
     * The subjects. If no subjects were specified, an empty array is returned.
     *
     * @return the subjects for this mediapackage
     */
    val subjects: Array<String>

    /**
     * Returns the media package start time.
     *
     * @return the start time
     */
    val date: Date

}
