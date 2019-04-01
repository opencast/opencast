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

import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_ACCESS_RIGHTS
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_AVAILABLE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_EXTENT
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IS_PART_OF
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_REPLACES
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_RIGHTS_HOLDER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SPATIAL
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SUBJECT
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TYPE
import org.opencastproject.util.data.Collections.head
import org.opencastproject.util.data.Collections.list
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.Option.some

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.metadata.api.MetadataValue
import org.opencastproject.metadata.api.StaticMetadata
import org.opencastproject.metadata.api.StaticMetadataService
import org.opencastproject.metadata.api.util.Interval
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.NonEmptyList
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Predicate
import org.opencastproject.util.data.functions.Misc
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Date

/**
 * This service provides [org.opencastproject.metadata.api.StaticMetadata] for a given mediapackage,
 * based on a contained dublin core catalog describing the episode.
 */
class StaticMetadataServiceDublinCoreImpl : StaticMetadataService {

    // Catalog loader function
    private val loader = object : Function<Catalog, Option<DublinCoreCatalog>>() {
        override fun apply(catalog: Catalog): Option<DublinCoreCatalog> {
            return load(catalog)
        }
    }

    protected var priority = 0

    protected var workspace: Workspace? = null

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    fun activate(properties: Map<*, *>?) {
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
     * @see org.opencastproject.metadata.api.MetadataService.getMetadata
     */
    override fun getMetadata(mp: MediaPackage): StaticMetadata {
        return mlist(list(mp.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE)))
                .find(flavorPredicate(MediaPackageElements.EPISODE))
                .flatMap(loader)
                .map(object : Function<DublinCoreCatalog, StaticMetadata>() {
                    override fun apply(episode: DublinCoreCatalog): StaticMetadata {
                        return newStaticMetadataFromEpisode(episode)
                    }
                })
                .getOrElse(null as StaticMetadata?)
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

    private fun load(catalog: Catalog): Option<DublinCoreCatalog> {
        var `in`: InputStream? = null
        try {
            val f = workspace!!.get(catalog.getURI())
            `in` = FileInputStream(f)
            return some(DublinCores.read(`in`))
        } catch (e: Exception) {
            logger.warn("Unable to load metadata from catalog '{}'", catalog)
            return Option.none()
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StaticMetadataServiceDublinCoreImpl::class.java)

        private fun newStaticMetadataFromEpisode(episode: DublinCoreCatalog): StaticMetadata {
            // Ensure that the mandatory properties are present
            val id = option(episode.getFirst(PROPERTY_IDENTIFIER))
            val created = option(episode.getFirst(PROPERTY_CREATED)).map(object : Function<String, Date>() {
                override fun apply(a: String): Date? {
                    val date = EncodingSchemeUtils.decodeDate(a)
                    return date ?: Misc.chuck<Date>(RuntimeException("$a does not conform to W3C-DTF encoding scheme."))
                }
            })
            val temporalOpt = option(episode.getFirstVal(PROPERTY_TEMPORAL)).map(dc2temporalValueOption())
            val start: Option<Date>
            if (episode.getFirst(PROPERTY_TEMPORAL) != null) {
                val period = EncodingSchemeUtils
                        .decodeMandatoryPeriod(episode.getFirst(PROPERTY_TEMPORAL))
                start = option(period.start)
            } else {
                start = created
            }
            val language = option(episode.getFirst(PROPERTY_LANGUAGE))
            val extent = head(episode.get(PROPERTY_EXTENT)).map(object : Function<DublinCoreValue, Long>() {
                override fun apply(a: DublinCoreValue): Long? {
                    val extent = EncodingSchemeUtils.decodeDuration(a)
                    return extent
                            ?: Misc.chuck<Long>(RuntimeException("$a does not conform to ISO8601 encoding scheme for durations."))
                }
            })
            val type = option(episode.getFirst(PROPERTY_TYPE))

            val isPartOf = option(episode.getFirst(PROPERTY_IS_PART_OF))
            val replaces = option(episode.getFirst(PROPERTY_REPLACES))
            val available = head(episode.get(PROPERTY_AVAILABLE)).flatMap(
                    object : Function<DublinCoreValue, Option<Interval>>() {
                        override fun apply(v: DublinCoreValue): Option<Interval> {
                            val p = EncodingSchemeUtils.decodePeriod(v)
                            return if (p != null)
                                some(Interval.fromValues(p.start, p.end))
                            else
                                Misc.chuck(RuntimeException("$v does not conform to W3C-DTF encoding scheme for periods"))
                        }
                    })
            val titles = NonEmptyList(
                    mlist(episode.get(PROPERTY_TITLE)).map(dc2mvString(PROPERTY_TITLE.localName)).value())
            val subjects = mlist(episode.get(PROPERTY_SUBJECT)).map(dc2mvString(PROPERTY_SUBJECT.localName)).value()
            val creators = mlist(episode.get(PROPERTY_CREATOR)).map(dc2mvString(PROPERTY_CREATOR.localName)).value()
            val publishers = mlist(episode.get(PROPERTY_PUBLISHER)).map(dc2mvString(PROPERTY_PUBLISHER.localName)).value()
            val contributors = mlist(episode.get(PROPERTY_CONTRIBUTOR)).map(dc2mvString(PROPERTY_CONTRIBUTOR.localName)).value()
            val description = mlist(episode.get(PROPERTY_DESCRIPTION)).map(dc2mvString(PROPERTY_DESCRIPTION.localName)).value()
            val rightsHolders = mlist(episode.get(PROPERTY_RIGHTS_HOLDER)).map(dc2mvString(PROPERTY_RIGHTS_HOLDER.localName)).value()
            val spatials = mlist(episode.get(PROPERTY_SPATIAL)).map(dc2mvString(PROPERTY_SPATIAL.localName)).value()
            val accessRights = mlist(episode.get(PROPERTY_ACCESS_RIGHTS)).map(dc2mvString(PROPERTY_ACCESS_RIGHTS.localName)).value()
            val licenses = mlist(episode.get(PROPERTY_LICENSE)).map(dc2mvString(PROPERTY_LICENSE.localName)).value()

            return object : StaticMetadata {
                override fun getId(): Option<String> {
                    return id
                }

                override fun getCreated(): Option<Date> {
                    // Compatibility patch with SOLR search service, where DC_CREATED stores the time the recording has been recorded
                    // in Admin UI and external API this is stored in DC_TEMPORAL as a DC Period. DC_CREATED is the date on which the
                    // event was created there.
                    // Admin UI and External UI do not use this Class, that only seems to be used by the old SOLR modules,
                    // so data will be kept correctly there, and only be "exported" to DC_CREATED for compatibility reasons here.
                    return start
                }

                override fun getTemporalPeriod(): Option<Array<Date>> {
                    if (temporalOpt.isSome) {
                        if (temporalOpt.get() is DCMIPeriod) {
                            val p = temporalOpt.get() as DCMIPeriod
                            return option(arrayOf(p.start, p.end))
                        }
                    }
                    return Option.none()
                }

                override fun getTemporalInstant(): Option<Date> {
                    if (temporalOpt.isSome) {
                        if (temporalOpt.get() is Date) {
                            return temporalOpt
                        }
                    }
                    return Option.none()
                }

                override fun getTemporalDuration(): Option<Long> {
                    if (temporalOpt.isSome) {
                        if (temporalOpt.get() is Long) {
                            return temporalOpt
                        }
                    }
                    return Option.none()
                }

                override fun getExtent(): Option<Long> {
                    return extent
                }

                override fun getLanguage(): Option<String> {
                    return language
                }

                override fun getIsPartOf(): Option<String> {
                    return isPartOf
                }

                override fun getReplaces(): Option<String> {
                    return replaces
                }

                override fun getType(): Option<String> {
                    return type
                }

                override fun getAvailable(): Option<Interval> {
                    return available
                }

                override fun getTitles(): NonEmptyList<MetadataValue<String>> {
                    return titles
                }

                override fun getSubjects(): List<MetadataValue<String>> {
                    return subjects
                }

                override fun getCreators(): List<MetadataValue<String>> {
                    return creators
                }

                override fun getPublishers(): List<MetadataValue<String>> {
                    return publishers
                }

                override fun getContributors(): List<MetadataValue<String>> {
                    return contributors
                }

                override fun getDescription(): List<MetadataValue<String>> {
                    return description
                }

                override fun getRightsHolders(): List<MetadataValue<String>> {
                    return rightsHolders
                }

                override fun getSpatials(): List<MetadataValue<String>> {
                    return spatials
                }

                override fun getAccessRights(): List<MetadataValue<String>> {
                    return accessRights
                }

                override fun getLicenses(): List<MetadataValue<String>> {
                    return licenses
                }
            }
        }

        /**
         * Return a function that creates a Option with the value of temporal from a DublinCoreValue.
         */
        private fun dc2temporalValueOption(): Function<DublinCoreValue, Any> {
            return object : Function<DublinCoreValue, Any>() {
                override fun apply(dcv: DublinCoreValue): Any {
                    val temporal = EncodingSchemeUtils.decodeTemporal(dcv)
                    return if (temporal != null) {
                        temporal.fold(object : Temporal.Match<Any> {
                            override fun period(period: DCMIPeriod?): Any? {
                                return period
                            }

                            override fun instant(instant: Date?): Any? {
                                return instant
                            }

                            override fun duration(duration: Long): Any {
                                return duration
                            }
                        })
                    } else Misc.chuck(RuntimeException("$dcv does not conform to ISO8601 encoding scheme for temporal."))
                }
            }
        }

        /**
         * Return a function that creates a MetadataValue[String] from a DublinCoreValue setting its name to `name`.
         */
        private fun dc2mvString(name: String): Function<DublinCoreValue, MetadataValue<String>> {
            return object : Function<DublinCoreValue, MetadataValue<String>>() {
                override fun apply(dcv: DublinCoreValue): MetadataValue<String> {
                    return MetadataValue(dcv.value, name, dcv.language)
                }
            }
        }

        private fun flavorPredicate(flavor: MediaPackageElementFlavor): Predicate<Catalog> {
            return object : Predicate<Catalog>() {
                override fun apply(catalog: Catalog): Boolean? {
                    return flavor == catalog.flavor
                }
            }
        }
    }
}
