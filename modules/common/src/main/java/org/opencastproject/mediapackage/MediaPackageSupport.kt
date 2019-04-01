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

package org.opencastproject.mediapackage

import com.entwinemedia.fn.Prelude.chuck
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.opencastproject.util.IoSupport.withResource
import org.opencastproject.util.data.Collections.list
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.functions.Booleans.not
import org.opencastproject.util.data.functions.Options.sequenceOpt
import org.opencastproject.util.data.functions.Options.toOption

import org.opencastproject.util.data.Effect
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Strings

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Arrays
import java.util.Collections

/** Utility class used for media package handling.  */
object MediaPackageSupport {

    private val NIL = java.util.Collections.EMPTY_LIST

    /** the logging facility provided by log4j  */
    private val logger = LoggerFactory.getLogger(MediaPackageSupport::class.java!!.getName())

    val getMediaPackageElementId: Function<MediaPackageElement, String> = object : Function<MediaPackageElement, String>() {
        override fun apply(mediaPackageElement: MediaPackageElement): String {
            return mediaPackageElement.identifier
        }
    }

    val getMediaPackageElementReferenceId: Function<MediaPackageElement, Option<String>> = object : Function<MediaPackageElement, Option<String>>() {
        override fun apply(mediaPackageElement: MediaPackageElement): Option<String> {
            return option(mediaPackageElement.reference).map(getReferenceId)
        }
    }

    val getReferenceId: Function<MediaPackageReference, String> = object : Function<MediaPackageReference, String>() {
        override fun apply(mediaPackageReference: MediaPackageReference): String {
            return mediaPackageReference.identifier
        }
    }

    /** Get the checksum from a media package element.  */
    val getChecksum: Function<MediaPackageElement, Option<String>> = object : Function<MediaPackageElement, Option<String>>() {
        override fun apply(mpe: MediaPackageElement): Option<String> {
            return option(mpe.checksum.value)
        }
    }

    /**
     * Function to extract the ID of a media package.
     *
     */
    @Deprecated("use {@link Fn#getId}")
    val getId: Function<MediaPackage, String> = object : Function<MediaPackage, String>() {
        override fun apply(mp: MediaPackage): String {
            return mp.identifier.toString()
        }
    }

    /**
     * Mode used when merging media packages.
     *
     *
     *
     *  * `Merge` assigns a new identifier in case of conflicts
     *  * `Replace` replaces elements in the target media package with matching identifier
     *  * `Skip` skips elements from the source media package with matching identifer
     *  * `Fail` fail in case of conflicting identifier
     *
     */
    enum class MergeMode {
        Merge, Replace, Skip, Fail
    }

    /**
     * Merges the contents of media package located at `sourceDir` into the media package located at
     * `targetDir`.
     *
     *
     * When choosing to move the media package element into the new place instead of copying them, the source media
     * package folder will be removed afterwards.
     *
     *
     * @param dest
     * the target media package directory
     * @param src
     * the source media package directory
     * @param mode
     * conflict resolution strategy in case of identical element identifier
     * @throws MediaPackageException
     * if an error occurs either accessing one of the two media packages or merging them
     */
    @Throws(MediaPackageException::class)
    fun merge(dest: MediaPackage, src: MediaPackage, mode: MergeMode): MediaPackage {
        try {
            for (e in src.elements()) {
                if (dest.getElementById(e.identifier) == null)
                    dest.add(e)
                else {
                    if (MergeMode.Replace == mode) {
                        logger.debug("Replacing element " + e.identifier + " while merging " + dest + " with " + src)
                        dest.remove(dest.getElementById(e.identifier))
                        dest.add(e)
                    } else if (MergeMode.Skip == mode) {
                        logger.debug("Skipping element " + e.identifier + " while merging " + dest + " with " + src)
                        continue
                    } else if (MergeMode.Merge == mode) {
                        logger.debug("Renaming element " + e.identifier + " while merging " + dest + " with " + src)
                        e.identifier = null
                        dest.add(e)
                    } else if (MergeMode.Fail == mode) {
                        throw MediaPackageException("Target media package " + dest + " already contains element with id "
                                + e.identifier)
                    }
                }
            }
        } catch (e: UnsupportedElementException) {
            throw MediaPackageException(e)
        }

        return dest
    }

    /**
     * Returns `true` if the media package contains an element with the specified identifier.
     *
     * @param identifier
     * the identifier
     * @return `true` if the media package contains an element with this identifier
     */
    fun contains(identifier: String, mp: MediaPackage): Boolean {
        for (element in mp.elements) {
            if (element.identifier == identifier)
                return true
        }
        return false
    }

    /**
     * Extract the file name from a media package elements URI.
     *
     * @return the file name or none if it could not be determined
     */
    fun getFileName(mpe: MediaPackageElement): Opt<String> {
        val uri = mpe.uri
        return if (uri != null) {
            Opt.nul(FilenameUtils.getName(uri.toString())).bind(Strings.blankToNone)
        } else {
            Opt.none()
        }
    }

    fun getJsonInputStream(mp: MediaPackage): InputStream {
        return getInputStream(MediaPackageParser.getAsJSON(mp))
    }

    fun getXmlInputStream(mp: MediaPackage): InputStream {
        return getInputStream(MediaPackageParser.getAsXml(mp))
    }

    fun getJsonInputStream(catalog: XMLCatalog): InputStream {
        try {
            return getInputStream(catalog.toJson())
        } catch (e: IOException) {
            return chuck(e)
        }

    }

    fun getXmlInputStream(catalog: XMLCatalog): InputStream {
        try {
            return getInputStream(catalog.toXmlString())
        } catch (e: IOException) {
            return chuck(e)
        }

    }

    /**
     * Get a UTF-8 encoded input stream.
     */
    private fun getInputStream(s: String): InputStream {
        try {
            return IOUtils.toInputStream(s, "UTF-8")
        } catch (e: IOException) {
            return chuck(e)
        }

    }

    /** Immutable modification of a media package.  */
    fun modify(mp: MediaPackage, e: Effect<MediaPackage>): MediaPackage {
        val clone = mp.clone() as MediaPackage
        e.apply(clone)
        return clone
    }

    /**
     * Immutable modification of a media package element. Attention: The returned element loses its media package
     * membership (see [org.opencastproject.mediapackage.AbstractMediaPackageElement.clone])
     */
    fun <A : MediaPackageElement> modify(mpe: A, e: Effect<A>): A {
        val clone = mpe.clone() as A
        e.apply(clone)
        return clone
    }

    /**
     * Create a copy of the given media package.
     *
     *
     * ATTENTION: Copying changes the type of the media package elements, e.g. an element of
     * type `DublinCoreCatalog` will become a `CatalogImpl`.
     */
    fun copy(mp: MediaPackage): MediaPackage {
        return mp.clone() as MediaPackage
    }

    /** Create a copy of the given media package element.  */
    fun copy(mpe: MediaPackageElement): MediaPackageElement {
        return mpe.clone() as MediaPackageElement
    }

    /** Update a mediapackage element of a mediapackage. Mutates `mp`.  */
    fun updateElement(mp: MediaPackage, e: MediaPackageElement) {
        mp.removeElementById(e.identifier)
        mp.add(e)
    }

    /** [.updateElement] as en effect.  */
    fun updateElement(mp: MediaPackage): Effect<MediaPackageElement> {
        return object : Effect<MediaPackageElement>() {
            override fun run(e: MediaPackageElement) {
                updateElement(mp, e)
            }
        }
    }

    fun removeElements(es: List<MediaPackageElement>, mp: MediaPackage) {
        for (e in es) {
            mp.remove(e)
        }
    }

    fun removeElements(es: List<MediaPackageElement>): Effect<MediaPackage> {
        return object : Effect<MediaPackage>() {
            override fun run(mp: MediaPackage) {
                removeElements(es, mp)
            }
        }
    }

    /** Replaces all elements of `mp` with `es`. Mutates `mp`.  */
    fun replaceElements(mp: MediaPackage, es: List<MediaPackageElement>) {
        for (e in mp.elements)
            mp.remove(e)
        for (e in es)
            mp.add(e)
    }

    /** Filters and predicates to work with media package element collections.  */
    object Filters {

        val presentations = byType<Publication>(Publication::class.java)

        val attachments = byType<Attachment>(Attachment::class.java)

        val tracks = byType<Track>(Track::class.java)

        val catalogs = byType<Catalog>(Catalog::class.java)

        val isPublication = ofType<Publication>(Publication::class.java)

        val isNotPublication = not(isPublication)

        val hasChecksum: Function<MediaPackageElement, Boolean> = object : Function<MediaPackageElement, Boolean>() {
            override fun apply(e: MediaPackageElement): Boolean {
                return e.checksum != null
            }
        }

        val hasNoChecksum = not(hasChecksum)

        val hasVideo: Function<Track, Boolean> = object : Function<Track, Boolean>() {
            override fun apply(track: Track): Boolean {
                return track.hasVideo()
            }
        }

        val hasAudio: Function<Track, Boolean> = object : Function<Track, Boolean>() {
            override fun apply(track: Track): Boolean {
                return track.hasAudio()
            }
        }

        val hasNoVideo = not(hasVideo)

        val hasNoAudio = not(hasAudio)

        val matchesFlavor: Function<MediaPackageElementFlavor, Function<MediaPackageElement, Boolean>> = object : Function<MediaPackageElementFlavor, Function<MediaPackageElement, Boolean>>() {
            override fun apply(flavor: MediaPackageElementFlavor): Function<MediaPackageElement, Boolean> {
                return matchesFlavor(flavor)
            }
        }

        val isEpisodeAcl: Function<MediaPackageElement, Boolean> = object : Function<MediaPackageElement, Boolean>() {
            override fun apply(mpe: MediaPackageElement): Boolean {
                // match is commutative
                return MediaPackageElements.XACML_POLICY_EPISODE.matches(mpe.flavor)
            }
        }

        val isEpisodeDublinCore: Function<MediaPackageElement, Boolean> = object : Function<MediaPackageElement, Boolean>() {
            override fun apply(mpe: MediaPackageElement): Boolean {
                // match is commutative
                return MediaPackageElements.EPISODE.matches(mpe.flavor)
            }
        }

        val isSeriesDublinCore: Function<MediaPackageElement, Boolean> = object : Function<MediaPackageElement, Boolean>() {
            override fun apply(mpe: MediaPackageElement): Boolean {
                // match is commutative
                return MediaPackageElements.SERIES.matches(mpe.flavor)
            }
        }

        val isSmilCatalog: Function<MediaPackageElement, Boolean> = object : Function<MediaPackageElement, Boolean>() {
            override fun apply(mpe: MediaPackageElement): Boolean {
                // match is commutative
                return MediaPackageElements.SMIL.matches(mpe.flavor)
            }
        }

        // functions implemented for monadic bind in order to cast types

        fun <A : MediaPackageElement> byType(type: Class<A>): Function<MediaPackageElement, List<A>> {
            return object : Function<MediaPackageElement, List<A>>() {
                override fun apply(mpe: MediaPackageElement): List<A> {
                    return if (type.isAssignableFrom(mpe.javaClass)) list(mpe as A) else NIL as List<A>
                }
            }
        }

        fun byFlavor(
                flavor: MediaPackageElementFlavor): Function<MediaPackageElement, List<MediaPackageElement>> {
            return object : Function<MediaPackageElement, List<MediaPackageElement>>() {
                override fun apply(mpe: MediaPackageElement): List<MediaPackageElement> {
                    // match is commutative
                    return if (flavor.matches(mpe.flavor)) listOf<MediaPackageElement>(mpe) else emptyList<MediaPackageElement>()
                }
            }
        }

        fun byTags(tags: List<String>): Function<MediaPackageElement, List<MediaPackageElement>> {
            return object : Function<MediaPackageElement, List<MediaPackageElement>>() {
                override fun apply(mpe: MediaPackageElement): List<MediaPackageElement> {
                    return if (mpe.containsTag(tags)) listOf<MediaPackageElement>(mpe) else emptyList<MediaPackageElement>()
                }
            }
        }

        /** [MediaPackageElement.containsTag] as a function.  */
        fun ofTags(tags: List<String>): Function<MediaPackageElement, Boolean> {
            return object : Function<MediaPackageElement, Boolean>() {
                override fun apply(mpe: MediaPackageElement): Boolean? {
                    return mpe.containsTag(tags)
                }
            }
        }

        fun <A : MediaPackageElement> ofType(type: Class<A>): Function<MediaPackageElement, Boolean> {
            return object : Function<MediaPackageElement, Boolean>() {
                override fun apply(mpe: MediaPackageElement): Boolean? {
                    return type.isAssignableFrom(mpe.javaClass)
                }
            }
        }

        /** Filters publications to channel `channelId`.  */
        fun ofChannel(channelId: String): Function<Publication, Boolean> {
            return object : Function<Publication, Boolean>() {
                override fun apply(p: Publication): Boolean? {
                    return p.channel == channelId
                }
            }
        }

        /** Check if mediapackage element has any of the given tags.  */
        fun hasTagAny(tags: List<String>): Function<MediaPackageElement, Boolean> {
            return object : Function<MediaPackageElement, Boolean>() {
                override fun apply(mpe: MediaPackageElement): Boolean? {
                    return mpe.containsTag(tags)
                }
            }
        }

        fun hasTag(tag: String): Function<MediaPackageElement, Boolean> {
            return object : Function<MediaPackageElement, Boolean>() {
                override fun apply(mpe: MediaPackageElement): Boolean? {
                    return mpe.containsTag(tag)
                }
            }
        }

        /**
         * Return true if the element has a flavor that matches `flavor`.
         *
         * @see MediaPackageElementFlavor.matches
         */
        fun matchesFlavor(flavor: MediaPackageElementFlavor): Function<MediaPackageElement, Boolean> {
            return object : Function<MediaPackageElement, Boolean>() {
                override fun apply(mpe: MediaPackageElement): Boolean? {
                    // match is commutative
                    return flavor.matches(mpe.flavor)
                }
            }
        }

        /** [MediaPackageElementFlavor.matches] as a function.  */
        fun matches(flavor: MediaPackageElementFlavor): Function<MediaPackageElementFlavor, Boolean> {
            return object : Function<MediaPackageElementFlavor, Boolean>() {
                override fun apply(f: MediaPackageElementFlavor): Boolean? {
                    return f.matches(flavor)
                }
            }
        }
    }

    /**
     * Basic sanity checking for media packages.
     *
     * <pre>
     * // media package is ok
     * sanityCheck(mp).isNone()
    </pre> *
     *
     * @return none if the media package is a healthy condition, some([error_msgs]) otherwise
     */
    fun sanityCheck(mp: MediaPackage): Option<List<String>> {
        val errors = sequenceOpt(list(toOption(mp.identifier != null, "no ID"),
                toOption(mp.identifier != null && isNotBlank(mp.identifier.toString()), "blank ID")))
        return if (errors.getOrElse(NIL).size == 0) Option.none() else errors
    }

    /** To be used in unit tests.  */
    fun loadFromClassPath(path: String): MediaPackage {
        return withResource(MediaPackageSupport::class.java!!.getResourceAsStream(path),
                object : Function.X<InputStream, MediaPackage>() {
                    @Throws(MediaPackageException::class)
                    public override fun xapply(`is`: InputStream): MediaPackage {
                        return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()!!.loadFromXml(`is`)
                    }
                })
    }

    /**
     * Media package must have a title and contain tracks in order to be published.
     *
     * @param mp
     * the media package
     * @return `true` if the media package can be published
     */
    fun isPublishable(mp: MediaPackage): Boolean {
        return !isBlank(mp.title) && mp.hasTracks()
    }

    /** Functions on media packages.  */
    object Fn {

        /** Function to extract the ID of a media package.  */
        val getId: Function<MediaPackage, String> = object : Function<MediaPackage, String>() {
            override fun apply(mp: MediaPackage): String {
                return mp.identifier.toString()
            }
        }

        val getElements: Function<MediaPackage, List<MediaPackageElement>> = object : Function<MediaPackage, List<MediaPackageElement>>() {
            override fun apply(a: MediaPackage): List<MediaPackageElement> {
                return Arrays.asList(*a.elements)
            }
        }

        val getTracks: Function<MediaPackage, List<Track>> = object : Function<MediaPackage, List<Track>>() {
            override fun apply(a: MediaPackage): List<Track> {
                return Arrays.asList(*a.tracks)
            }
        }

        val getPublications: Function<MediaPackage, List<Publication>> = object : Function<MediaPackage, List<Publication>>() {
            override fun apply(a: MediaPackage): List<Publication> {
                return Arrays.asList(*a.publications)
            }
        }
    }
}
/** Disable construction of this utility class  */
