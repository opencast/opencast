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

import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.metadata.api.CatalogService
import org.opencastproject.metadata.api.MediaPackageMetadata
import org.opencastproject.metadata.api.MediaPackageMetadataService
import org.opencastproject.metadata.api.MediapackageMetadataImpl
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Stream

import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Comparator

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Parses [DublinCoreCatalog]s from serialized DC representations.
 */
class DublinCoreCatalogService : CatalogService<DublinCoreCatalog>, MediaPackageMetadataService {

    protected var priority = 0

    protected var workspace: Workspace? = null

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    fun activate(properties: Map<String, *>?) {
        logger.debug("activate()")
        if (properties != null) {
            val priorityString = properties[MetadataService.PRIORITY_KEY] as String
            if (priorityString != null) {
                try {
                    priority = Integer.parseInt(priorityString)
                } catch (e: NumberFormatException) {
                    logger.warn("Unable to set priority to {}", priorityString)
                    throw e
                }

            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.metadata.api.CatalogService.serialize
     */
    @Throws(IOException::class)
    override fun serialize(catalog: DublinCoreCatalog): InputStream {
        try {
            val tf = TransformerFactory.newInstance().newTransformer()
            val xmlSource = DOMSource(catalog.toXml())
            val out = ByteArrayOutputStream()
            tf.transform(xmlSource, StreamResult(out))
            return ByteArrayInputStream(out.toByteArray())
        } catch (e: Exception) {
            throw IOException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.metadata.api.MetadataService.getMetadata
     */
    override fun getMetadata(mp: MediaPackage): MediaPackageMetadata {
        val metadata = MediapackageMetadataImpl()
        for (catalog in Stream.`$`(*mp.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE)).sort(COMPARE_BY_FLAVOR)) {
            val dc = DublinCoreUtil.loadDublinCore(workspace, catalog)
            if (MediaPackageElements.EPISODE.equals(catalog.flavor)) {
                // Title
                metadata.title = dc.getFirst(DublinCore.PROPERTY_TITLE)

                // use started date as created date (see MH-12250)
                if (dc.hasValue(DublinCore.PROPERTY_TEMPORAL) && dc.getFirst(PROPERTY_TEMPORAL) != null) {
                    val period = EncodingSchemeUtils
                            .decodeMandatoryPeriod(dc.getFirst(PROPERTY_TEMPORAL))
                    metadata.date = period.start
                } else {
                    // ...and only if started date is not available the created date
                    if (dc.hasValue(DublinCore.PROPERTY_CREATED))
                        metadata.date = EncodingSchemeUtils.decodeDate(dc.get(DublinCore.PROPERTY_CREATED)[0])
                }
                // Series id
                if (dc.hasValue(DublinCore.PROPERTY_IS_PART_OF))
                    metadata.seriesIdentifier = dc.get(DublinCore.PROPERTY_IS_PART_OF)[0].value

                // Creator
                if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
                    val creators = ArrayList<String>()
                    for (creator in dc.get(DublinCore.PROPERTY_CREATOR)) {
                        creators.add(creator.value)
                    }
                    metadata.creators = creators.toTypedArray()
                }

                // Contributor
                if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
                    val contributors = ArrayList<String>()
                    for (contributor in dc.get(DublinCore.PROPERTY_CONTRIBUTOR)) {
                        contributors.add(contributor.value)
                    }
                    metadata.contributors = contributors.toTypedArray()
                }

                // Subject
                if (dc.hasValue(DublinCore.PROPERTY_SUBJECT)) {
                    val subjects = ArrayList<String>()
                    for (subject in dc.get(DublinCore.PROPERTY_SUBJECT)) {
                        subjects.add(subject.value)
                    }
                    metadata.subjects = subjects.toTypedArray()
                }

                // License
                metadata.license = dc.getFirst(DublinCore.PROPERTY_LICENSE)

                // Language
                metadata.language = dc.getFirst(DublinCore.PROPERTY_LANGUAGE)
            } else if (MediaPackageElements.SERIES.equals(catalog.flavor)) {
                // Series Title and Identifier
                metadata.seriesTitle = dc.getFirst(DublinCore.PROPERTY_TITLE)
                metadata.seriesIdentifier = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER)
            } else {
                logger.debug("Excluding unknown catalog flavor '{}' from the top level metadata of mediapackage '{}'",
                        catalog.flavor, mp.identifier)
            }
        }
        return metadata
    }

    /**
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.metadata.api.CatalogService.load
     */
    @Throws(IOException::class)
    override fun load(`in`: InputStream?): DublinCoreCatalog {
        if (`in` == null)
            throw IllegalArgumentException("Stream must not be null")
        return DublinCores.read(`in`)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.metadata.api.CatalogService.accepts
     */
    override fun accepts(catalog: Catalog?): Boolean {
        if (catalog == null)
            throw IllegalArgumentException("Catalog must not be null")
        val flavor = catalog.flavor
        return flavor != null && flavor == DublinCoreCatalog.ANY_DUBLINCORE
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.metadata.api.CatalogService.newInstance
     */
    override fun newInstance(): DublinCoreCatalog {
        return DublinCores.mkOpencastEpisode().catalog
    }

    /**
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.metadata.api.MetadataService.getPriority
     */
    override fun getPriority(): Int {
        return priority
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DublinCoreCatalogService::class.java)

        val COMPARE_BY_FLAVOR: Comparator<Catalog> = Comparator { c1, c2 -> if (MediaPackageElements.EPISODE.equals(c1.flavor)) 1 else -1 }
    }

}
