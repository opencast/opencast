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
package org.opencastproject.message.broker.api.assetmanager

import com.entwinemedia.fn.Prelude.chuck

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.message.broker.api.MessageItem
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser
import org.opencastproject.util.RequireUtil
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.IOUtils

import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.Date

import javax.annotation.ParametersAreNonnullByDefault

/**
 * [Serializable] class that represents all of the possible messages sent through an AssetManager queue.
 */
@ParametersAreNonnullByDefault
abstract class AssetManagerItem private constructor(mediaPackageId: String, date: Date) : MessageItem, Serializable {

    // common fields

    override val id: String
    private val date: Long

    abstract val type: Type

    enum class Type {
        Update, Delete
    }

    init {
        this.id = RequireUtil.notNull(mediaPackageId, "mediaPackageId")
        this.date = RequireUtil.notNull(date, "date").time
    }

    abstract fun <A> decompose(takeSnapshot: Fn<in TakeSnapshot, out A>,
                               deleteSnapshot: Fn<in DeleteSnapshot, out A>, deleteEpisode: Fn<in DeleteEpisode, out A>): A

    fun getDate(): Date {
        return Date(date)
    }

    /*
   * ------------------------------------------------------------------------------------------------------------------
   */

    /**
     * An event for taking a snapshot of a media package.
     */
    class TakeSnapshot private constructor(mediaPackageId: String, private val mediapackage: String, private val episodeDublincore: String?, private val acl: String, val version: Long,
                                           date: Date) : AssetManagerItem(mediaPackageId, date) {

        override val type: Type
            get() = Type.Update

        override fun <A> decompose(takeSnapshot: Fn<in TakeSnapshot, out A>,
                                   deleteSnapshot: Fn<in DeleteSnapshot, out A>,
                                   deleteEpisode: Fn<in DeleteEpisode, out A>): A {
            return takeSnapshot.apply(this)
        }

        fun getMediapackage(): MediaPackage {
            try {
                return MediaPackageParser.getFromXml(mediapackage)
            } catch (e: MediaPackageException) {
                return chuck(e)
            }

        }

        fun getAcl(): AccessControlList {
            return AccessControlParser.parseAclSilent(acl)
        }

        fun getEpisodeDublincore(): Opt<DublinCoreCatalog> {
            if (episodeDublincore == null)
                return Opt.none()

            try {
                IOUtils.toInputStream(episodeDublincore, "UTF-8").use { `is` -> return Opt.some(DublinCores.read(`is`)) }
            } catch (e: IOException) {
                return chuck(e)
            }

        }

        companion object {
            private val serialVersionUID = 3530625835200867594L

            //

            val getMediaPackage: Fn<TakeSnapshot, MediaPackage> = object : Fn<TakeSnapshot, MediaPackage>() {
                override fun apply(a: TakeSnapshot): MediaPackage {
                    return a.getMediapackage()
                }
            }

            val getEpisodeDublincore: Fn<TakeSnapshot, Opt<DublinCoreCatalog>> = object : Fn<TakeSnapshot, Opt<DublinCoreCatalog>>() {
                override fun apply(a: TakeSnapshot): Opt<DublinCoreCatalog> {
                    return a.getEpisodeDublincore()
                }
            }

            val getAcl: Fn<TakeSnapshot, AccessControlList> = object : Fn<TakeSnapshot, AccessControlList>() {
                override fun apply(a: TakeSnapshot): AccessControlList {
                    return a.getAcl()
                }
            }

            val getVersion: Fn<TakeSnapshot, Long> = object : Fn<TakeSnapshot, Long>() {
                override fun apply(a: TakeSnapshot): Long {
                    return a.version
                }
            }
        }

    }

    /*
   * ------------------------------------------------------------------------------------------------------------------
   */

    /**
     * An event for deleting a single version of a media package (aka snapshot).
     */
    class DeleteSnapshot private constructor(mediaPackageId: String, val version: Long, date: Date) : AssetManagerItem(mediaPackageId, date) {

        override val type: Type
            get() = Type.Delete

        val mediaPackageId: String
            get() = id

        override fun <A> decompose(takeSnapshot: Fn<in TakeSnapshot, out A>,
                                   deleteSnapshot: Fn<in DeleteSnapshot, out A>,
                                   deleteEpisode: Fn<in DeleteEpisode, out A>): A {
            return deleteSnapshot.apply(this)
        }

        companion object {
            private val serialVersionUID = 4797196156230502250L

            val getMediaPackageId: Fn<DeleteSnapshot, String> = object : Fn<DeleteSnapshot, String>() {
                override fun apply(a: DeleteSnapshot): String {
                    return a.mediaPackageId
                }
            }

            val getVersion: Fn<DeleteSnapshot, Long> = object : Fn<DeleteSnapshot, Long>() {
                override fun apply(a: DeleteSnapshot): Long {
                    return a.version
                }
            }
        }
    }

    /*
   * ------------------------------------------------------------------------------------------------------------------
   */

    /**
     * A event that will be sent when all versions of a media package (aka the whole episode) have been deleted.
     */
    class DeleteEpisode private constructor(mediaPackageId: String, date: Date) : AssetManagerItem(mediaPackageId, date) {

        override val type: Type
            get() = Type.Delete

        val mediaPackageId: String
            get() = id

        override fun <A> decompose(takeSnapshot: Fn<in TakeSnapshot, out A>,
                                   deleteSnapshot: Fn<in DeleteSnapshot, out A>,
                                   deleteEpisode: Fn<in DeleteEpisode, out A>): A {
            return deleteEpisode.apply(this)
        }

        companion object {
            private val serialVersionUID = -4906056424740181256L

            val getMediaPackageId: Fn<DeleteEpisode, String> = object : Fn<DeleteEpisode, String>() {
                override fun apply(a: DeleteEpisode): String {
                    return a.mediaPackageId
                }
            }
        }
    }

    companion object {
        private const val serialVersionUID = 5440420510139202434L

        val ASSETMANAGER_QUEUE_PREFIX = "ASSETMANAGER."

        val ASSETMANAGER_QUEUE = ASSETMANAGER_QUEUE_PREFIX + "QUEUE"

        /*
   * ------------------------------------------------------------------------------------------------------------------
   */

        //
        // constructor methods
        //

        /**
         * @param workspace
         * The workspace
         * @param mp
         * The media package to update.
         * @param acl
         * The access control list of the media package to update.
         * @param version
         * The version of the media package.
         * @param date
         * The modification date.
         * @return Builds a [AssetManagerItem] for taking a media package snapshot.
         */
        fun add(workspace: Workspace, mp: MediaPackage, acl: AccessControlList, version: Long, date: Date): TakeSnapshot {
            var dcXml: String? = null
            for (catalog in mp.getCatalogs(MediaPackageElements.EPISODE)) {
                try {
                    workspace.read(catalog.getURI()).use { `in` -> dcXml = IOUtils.toString(`in`, StandardCharsets.UTF_8) }
                } catch (e: Exception) {
                    throw IllegalStateException(String.format("Unable to load dublin core catalog for event '%s'",
                            mp.identifier), e)
                }

            }
            return TakeSnapshot(mp.identifier.compact(), MediaPackageParser.getAsXml(mp), dcXml!!,
                    AccessControlParser.toJsonSilent(acl), version, date)
        }

        /**
         * @param mediaPackageId
         * The unique id of the media package to delete.
         * @param version
         * The episode's version.
         * @param date
         * The modification date.
         * @return Builds [AssetManagerItem] for deleting a snapshot from the asset manager.
         */
        fun deleteSnapshot(mediaPackageId: String, version: Long, date: Date): AssetManagerItem {
            return DeleteSnapshot(mediaPackageId, version, date)
        }

        /**
         * @param mediaPackageId
         * The unique id of the media package to delete.
         * @param date
         * The modification date.
         * @return Builds [AssetManagerItem] for deleting an episode from the asset manager.
         */
        fun deleteEpisode(mediaPackageId: String, date: Date): AssetManagerItem {
            return DeleteEpisode(mediaPackageId, date)
        }
    }
}
