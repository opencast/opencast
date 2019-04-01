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
 * Provides metadata for a [MediaPackageMetadata].
 */
class MediapackageMetadataImpl : MediaPackageMetadata {

    /**
     * {@inheritDoc}
     */
    /**
     * Sets the media package's title.
     *
     * @param title
     * the title
     */
    override var title: String
    /**
     * {@inheritDoc}
     */
    /**
     * Sets the series title.
     *
     * @param seriesTitle
     * the series title
     */
    override var seriesTitle: String
    /**
     * {@inheritDoc}
     */
    /**
     * Sets the series identifier
     *
     * @param seriesIdentifier
     * the series identifier
     */
    override var seriesIdentifier: String
    protected var creators: Array<String>? = null
    protected var contributors: Array<String>? = null
    protected var subjects: Array<String>? = null
    /**
     * {@inheritDoc}
     */
    /**
     * Sets the mediapackage's language.
     *
     * @param language
     * the language
     */
    override var language: String
    /**
     * {@inheritDoc}
     */
    /**
     * Sets the mediapackage license.
     *
     * @param license
     * the license
     */
    override var license: String
    /**
     * {@inheritDoc}
     */
    /**
     * Sets the mediapackage's creation date.
     *
     * @param date
     * the creation date
     */
    override var date: Date

    /**
     * {@inheritDoc}
     */
    override fun getCreators(): Array<String> {
        return if (creators == null) arrayOf() else creators
    }

    /**
     * Sets the list of creators.
     *
     * @param creators
     * the creators
     */
    fun setCreators(creators: Array<String>) {
        this.creators = creators
    }

    override fun getContributors(): Array<String> {
        return if (contributors == null) arrayOf() else contributors
    }

    /**
     * Sets the mediapackage's contributors.
     *
     * @param contributors
     * the contributors
     */
    fun setContributors(contributors: Array<String>) {
        this.contributors = contributors
    }

    /**
     * {@inheritDoc}
     */
    override fun getSubjects(): Array<String> {
        return if (subjects == null) arrayOf() else subjects
    }

    /**
     * Sets the mediapackage's subjects.
     *
     * @param subjects
     * the subjects
     */
    fun setSubjects(subjects: Array<String>) {
        this.subjects = subjects
    }

}
