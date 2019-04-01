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

package org.opencastproject.metadata

import org.opencastproject.util.data.Collections.map
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.metadata.api.MetadataValue
import org.opencastproject.metadata.api.StaticMetadata
import org.opencastproject.metadata.api.StaticMetadataService
import org.opencastproject.metadata.api.util.Interval
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.NonEmptyList
import org.opencastproject.util.data.Option
import org.opencastproject.workspace.api.Workspace

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Date

/**
 * This service provides [org.opencastproject.metadata.api.StaticMetadata] for a given mediapackage, based on the
 * information in the media package itself.
 *
 * todo unit tests will follow
 */
class StaticMetadataServiceMediaPackageImpl : StaticMetadataService {

    // a low default priority
    protected var priority = 99

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
        return object : StaticMetadata {
            override fun getId(): Option<String> {
                return option(mp.identifier.toString())
            }

            override fun getCreated(): Option<Date> {
                return none()
            }

            override fun getExtent(): Option<Long> {
                return option(mp.duration)
            }

            override fun getLanguage(): Option<String> {
                return none()
            }

            override fun getIsPartOf(): Option<String> {
                return option(mp.series)
            }

            override fun getReplaces(): Option<String> {
                return none()
            }

            override fun getType(): Option<String> {
                return none()
            }

            override fun getAvailable(): Option<Interval> {
                return none()
            }

            override fun getTemporalPeriod(): Option<Array<Date>> {
                return none()
            }

            override fun getTemporalInstant(): Option<Date> {
                return none()
            }

            override fun getTemporalDuration(): Option<Long> {
                return none()
            }

            override fun getTitles(): NonEmptyList<MetadataValue<String>> {
                return if (mp.title != null)
                    NonEmptyList(MetadataValue(mp.title, "title"))
                else
                    throw IllegalArgumentException("MediaPackage $mp does not contain a title")
            }

            override fun getSubjects(): List<MetadataValue<String>> {
                return emptyList()
            }

            override fun getCreators(): List<MetadataValue<String>> {
                return strings2MetadataValues(mp.creators, "creator")
            }

            override fun getPublishers(): List<MetadataValue<String>> {
                return emptyList()
            }

            override fun getContributors(): List<MetadataValue<String>> {
                return strings2MetadataValues(mp.contributors, "contributor")
            }

            override fun getDescription(): List<MetadataValue<String>> {
                return emptyList()
            }

            override fun getRightsHolders(): List<MetadataValue<String>> {
                return emptyList()
            }

            override fun getSpatials(): List<MetadataValue<String>> {
                return emptyList()
            }

            override fun getAccessRights(): List<MetadataValue<String>> {
                return emptyList()
            }

            override fun getLicenses(): List<MetadataValue<String>> {
                return if (mp.license != null)
                    Arrays.asList(MetadataValue(mp.license, "license"))
                else
                    emptyList()
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.metadata.api.MetadataService.getPriority
     */
    override fun getPriority(): Int {
        return priority
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StaticMetadataServiceMediaPackageImpl::class.java)

        /**
         * @param values
         * may be null
         * @param valueName
         * the name of the returned [MetadataValue]
         */
        private fun strings2MetadataValues(values: Array<String>?, valueName: String): List<MetadataValue<String>> {
            return if (values != null) {
                map(Arrays.asList<T>(*values), ArrayList(),
                        object : Function<String, MetadataValue<String>>() {
                            override fun apply(s: String): MetadataValue<String> {
                                return MetadataValue(s, valueName)
                            }
                        })
            } else {
                emptyList()
            }

        }
    }

}
