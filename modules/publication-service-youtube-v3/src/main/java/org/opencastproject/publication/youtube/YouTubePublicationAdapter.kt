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

package org.opencastproject.publication.youtube

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Strategy to determine destination of publication. Instances of this class are not thread-safe.
 */
class YouTubePublicationAdapter
/**
 * Create a single-use strategy instance for publication to youtube
 *
 * @param mp
 * the mediapackage identifier
 * @param workspace
 * the workspace service
 * @throws PublicationException
 */
@Throws(PublicationException::class)
constructor(
        /** Media package containing publication metadata  */
        private val mediaPackage: MediaPackage?, workspace: Workspace) {

    /** Dublincore metadata catalog for the episode  */
    private val dcEpisode: DublinCoreCatalog?

    /** Dublincore metadata catalog for the series  */
    private val dcSeries: DublinCoreCatalog?

    /**
     * Gets the name for a context within a publication channel.
     *
     * @return The playlist ID
     */
    val contextName: String
        get() = mediaPackage.seriesTitle

    /**
     * Gets the name for a context within a publication channel.
     *
     * @return Context description
     */
    val contextDescription: String?
        get() = dcSeries?.getFirst(DublinCore.PROPERTY_DESCRIPTION)

    /**
     * Gets the name for the episode of the media package
     *
     * @return the title of the episode
     */
    val episodeName: String?
        get() = dcEpisode?.getFirst(DublinCore.PROPERTY_TITLE)

    /**
     * Gets the description for the episode of the media package
     *
     * @return the description of the episode
     */
    val episodeDescription: String?
        get() {
            if (dcEpisode == null)
                return null

            var description = ""
            if (dcSeries != null)
                description = StringUtils.trimToEmpty(dcSeries.getFirst(DublinCore.PROPERTY_TITLE))

            val episodeDescription = dcEpisode.getFirst(DublinCore.PROPERTY_DESCRIPTION)
            if (episodeDescription != null)
                description += '\n' + episodeDescription

            val episodeLicense = dcEpisode.getFirst(DublinCore.PROPERTY_LICENSE)
            if (episodeLicense != null)
                description += '\n' + episodeLicense

            return description
        }

    init {
        if (mediaPackage == null) {
            throw PublicationException("Media package is null")
        }

        val episodeCatalogs = mediaPackage.getCatalogs(MediaPackageElements.EPISODE)
        if (episodeCatalogs.size == 0) {
            dcEpisode = null
        } else {
            dcEpisode = parseDublinCoreCatalog(episodeCatalogs[0], workspace)
        }

        val seriesCatalogs = mediaPackage.getCatalogs(MediaPackageElements.SERIES)
        if (seriesCatalogs.size == 0) {
            dcSeries = null
        } else {
            dcSeries = parseDublinCoreCatalog(seriesCatalogs[0], workspace)
        }
    }

    /**
     * Parse Dublincore metadata from the workspace
     *
     * @param catalog
     * A mediapackage's catalog file
     * @return Catalog parse from XML
     */
    private fun parseDublinCoreCatalog(catalog: Catalog, workspace: Workspace): DublinCoreCatalog? {
        var `is`: InputStream? = null
        try {
            val dcFile = workspace.get(catalog.getURI())
            `is` = FileInputStream(dcFile)
            return DublinCores.read(`is`)
        } catch (e: Exception) {
            logger.error("Error loading Dublin Core metadata: {}", e.message)
        } finally {
            IOUtils.closeQuietly(`is`)
        }
        return null
    }

    companion object {

        /** logger instance  */
        private val logger = LoggerFactory.getLogger(YouTubePublicationAdapter::class.java)
    }

}
