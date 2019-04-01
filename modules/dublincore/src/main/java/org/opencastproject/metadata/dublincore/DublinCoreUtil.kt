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

import com.entwinemedia.fn.Equality.eq
import com.entwinemedia.fn.Prelude.chuck
import com.entwinemedia.fn.Stream.`$`

import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageSupport
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry
import org.opencastproject.util.Checksum
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.ImmutableListWrapper
import com.entwinemedia.fn.data.Opt

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import kotlin.collections.Map.Entry

/** Utility functions for DublinCores.  */
object DublinCoreUtil {
    private val logger = LoggerFactory.getLogger(DublinCoreUtil::class.java)

    private val toString = object : Fn<Any, String>() {
        override fun apply(o: Any): String {
            return o.toString()
        }
    }

    /**
     * Load the episode DublinCore catalog contained in a media package.
     *
     * @return the catalog or none if the media package does not contain an episode DublinCore
     */
    fun loadEpisodeDublinCore(ws: Workspace, mp: MediaPackage): Opt<DublinCoreCatalog> {
        return loadDublinCore(ws, mp, MediaPackageSupport.Filters.isEpisodeDublinCore.toFn())
    }

    /**
     * Load the series DublinCore catalog contained in a media package.
     *
     * @return the catalog or none if the media package does not contain a series DublinCore
     */
    fun loadSeriesDublinCore(ws: Workspace, mp: MediaPackage): Opt<DublinCoreCatalog> {
        return loadDublinCore(ws, mp, MediaPackageSupport.Filters.isSeriesDublinCore.toFn())
    }

    /**
     * Load a DublinCore catalog of a media package identified by predicate `p`.
     *
     * @return the catalog or none if no media package element matches predicate `p`.
     */
    fun loadDublinCore(ws: Workspace, mp: MediaPackage,
                       p: Fn<MediaPackageElement, Boolean>): Opt<DublinCoreCatalog> {
        return `$`(*mp.elements).filter(p).head().map(object : Fn<MediaPackageElement, DublinCoreCatalog>() {
            override fun apply(mpe: MediaPackageElement): DublinCoreCatalog {
                return loadDublinCore(ws, mpe)
            }
        })
    }

    /**
     * Load the DublinCore catalog identified by `mpe`. Throws an exception if it does not exist or cannot be
     * loaded by any reason.
     *
     * @return the catalog
     */
    fun loadDublinCore(workspace: Workspace, mpe: MediaPackageElement): DublinCoreCatalog {
        val uri = mpe.getURI()
        logger.debug("Loading DC catalog from {}", uri)
        try {
            workspace.read(uri).use { `in` -> return DublinCores.read(`in`) }
        } catch (e: Exception) {
            logger.error("Unable to load metadata from catalog '{}'", mpe, e)
            return chuck(e)
        }

    }

    /**
     * Define equality on DublinCoreCatalogs. Two DublinCores are considered equal if they have the same properties and if
     * each property has the same values in the same order.
     *
     *
     * Note: As long as http://opencast.jira.com/browse/MH-8759 is not fixed, the encoding scheme of values is not
     * considered.
     *
     *
     * Implementation Note: DublinCores should not be compared by their string serialization since the ordering of
     * properties is not defined and cannot be guaranteed between serializations.
     */
    fun equals(a: DublinCoreCatalog, b: DublinCoreCatalog): Boolean {
        val av = a.values
        val bv = b.values
        if (av.size == bv.size) {
            for ((key, value) in av) {
                if (!eq(value, bv[key]))
                    return false
            }
            return true
        } else {
            return false
        }
    }

    /** Return a sorted list of all catalog entries.  */
    fun getPropertiesSorted(dc: DublinCoreCatalog): List<CatalogEntry> {
        val properties = ArrayList(dc.properties)
        Collections.sort(properties)
        val entries = ArrayList<CatalogEntry>()
        for (property in properties) {
            Collections.addAll(entries, *dc.getValues(property))
        }
        return ImmutableListWrapper(entries)
    }

    /** Calculate an MD5 checksum for a DublinCore catalog.  */
    fun calculateChecksum(dc: DublinCoreCatalog): Checksum {
        // Use 0 as a word separator. This is safe since none of the UTF-8 code points
        // except \u0000 contains a null byte when converting to a byte array.
        val sep = byteArrayOf(0)
        val md =
        // consider all DublinCore properties
                `$`(getPropertiesSorted(dc))
                        .bind(object : Fn<CatalogEntry, Stream<String>>() {
                            override fun apply(entry: CatalogEntry): Stream<String> {
                                // get attributes, sorted and serialized as [name, value, name, value, ...]
                                val attributesSorted = `$`(entry.getAttributes().entries)
                                        .sort { o1, o2 -> o1.key.compareTo(o2.key) }
                                        .bind(object : Fn<Entry<EName, String>, Stream<String>>() {
                                            override fun apply(attribute: Entry<EName, String>): Stream<String> {
                                                return `$`(attribute.key.toString(), attribute.value)
                                            }
                                        })
                                return `$`<String>(entry.eName.toString(), entry.value).append(attributesSorted)
                            }
                        })
                        // consider the root tag
                        .append(Opt.nul(dc.rootTag).map(toString))
                        // digest them
                        .foldl(mkMd5MessageDigest(), object : Fn2<MessageDigest, String, MessageDigest>() {
                            override fun apply(digest: MessageDigest, s: String): MessageDigest {
                                digest.update(s.toByteArray(StandardCharsets.UTF_8))
                                // add separator byte (see definition above)
                                digest.update(sep)
                                return digest
                            }
                        })
        try {
            return Checksum.create("md5", Checksum.convertToHex(md.digest()))
        } catch (e: NoSuchAlgorithmException) {
            return chuck(e)
        }

    }

    private fun mkMd5MessageDigest(): MessageDigest {
        try {
            return MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            logger.error("Unable to create md5 message digest")
            return chuck(e)
        }

    }
}
