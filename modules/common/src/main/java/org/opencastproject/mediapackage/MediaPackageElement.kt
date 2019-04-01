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

import org.opencastproject.util.Checksum
import org.opencastproject.util.MimeType

import java.net.URI

/**
 * All classes that will be part of a media package must implement this interface.
 */
interface MediaPackageElement : ManifestContributor, Comparable<MediaPackageElement>, Cloneable {

    /**
     * Returns the element identifier.
     *
     * @return the element identifier, may be null
     */
    /**
     * Sets the element identifier.
     *
     * @param id
     * the new element identifier
     */
    var identifier: String

    /**
     * Returns the element's manifest type.
     *
     * @return the manifest type
     */
    val elementType: Type

    /**
     * Returns a human readable name for this media package element. If no name was provided, the filename is returned
     * instead.
     *
     * @return the element name
     */
    /**
     * Sets the element description of this media package element.
     *
     * @param description
     * the new element description
     */
    var elementDescription: String

    /**
     * Returns the tags for this media package element or an empty array if there are no tags.
     *
     * @return the tags
     */
    val tags: Array<String>

    /**
     * Returns the media package if the element has been added, `null` otherwise.
     *
     * @return the media package
     */
    val mediaPackage: MediaPackage

    /**
     * Returns a reference to another entitiy, both inside or outside the media package.
     *
     * @return the reference
     */
    /**
     * Sets the element reference.
     *
     * @param reference
     * the reference
     */
    var reference: MediaPackageReference

    /**
     * Returns a reference to the element location.
     *
     * @return the element location
     */
    /**
     * Sets the elements location.
     *
     * @param uri
     * the element location
     */
    var uri: URI

    /**
     * Returns the file's checksum.
     *
     * @return the checksum
     */
    /**
     * Sets the new checksum on this media package element.
     *
     * @param checksum
     * the checksum
     */
    var checksum: Checksum

    /**
     * Returns the element's mimetype as found in the ISO Mime Type Registrations.
     *
     *
     * For example, in case of motion jpeg slides, this method will return the mime type for `video/mj2`.
     *
     * @return the mime type
     */
    /**
     * Sets the mime type on this media package element.
     *
     * @param mimeType
     * the new mime type
     */
    var mimeType: MimeType

    /**
     * Returns the element's type as defined for the specific media package element.
     *
     *
     * For example, in case of a video track, the type could be `video/x-presentation`.
     *
     * @return the element flavor
     */
    /**
     * Sets the flavor on this media package element.
     *
     * @param flavor
     * the new flavor
     */
    var flavor: MediaPackageElementFlavor

    /**
     * Returns the number of bytes that are occupied by this media package element.
     *
     * @return the size
     */
    /**
     * Sets the file size in bytes
     *
     * @param size
     */
    var size: Long

    /**
     * The element type todo is the type definitely needed or can the flavor take its responsibilities?
     */
    enum class Type {
        Manifest, Timeline, Track, Catalog, Attachment, Publication, Other
    }

    /**
     * Tags the media package element with the given tag.
     *
     * @param tag
     * the tag
     */
    fun addTag(tag: String)

    /**
     * Removes the tag from the media package element.
     *
     * @param tag
     * the tag
     */
    fun removeTag(tag: String)

    /**
     * Returns `true` if the media package element contains the given tag.
     *
     * @param tag
     * the tag
     * @return `true` if the element is tagged
     */
    fun containsTag(tag: String): Boolean

    /**
     * Returns `true` if the media package element contains at least one of the given tags. If there are no
     * tags contained in the set, then the element is considered to match as well.
     *
     * @param tags
     * the set of tag
     * @return `true` if the element is tagged accordingly
     */
    fun containsTag(tags: Collection<String>): Boolean

    /** Removes all tags associated with this element  */
    fun clearTags()

    /**
     * Verifies the integrity of the media package element.
     *
     * @throws MediaPackageException
     * if the media package element is in an incosistant state
     */
    @Throws(MediaPackageException::class)
    fun verify()

    /**
     * Adds a reference to the media package `mediaPackage`.
     *
     *
     * Note that an element can only refer to one object. Therefore, any existing reference will be replaced.
     *
     * @param mediaPackage
     * the media package to refere to
     */
    fun referTo(mediaPackage: MediaPackage)

    /**
     * Adds a reference to the media package element `element`.
     *
     *
     * Note that an element can only refere to one object. Therefore, any existing reference will be replaced. Also note
     * that if this element is part of a media package, a consistency check will be made making sure the refered element
     * is also part of the same media package. If not, a [MediaPackageException] will be thrown.
     *
     * @param element
     * the element to refere to
     */
    fun referTo(element: MediaPackageElement)

    /**
     * Adds an arbitrary reference.
     *
     *
     * Note that an element can only have one reference. Therefore, any existing reference will be replaced. Also note
     * that if this element is part of a media package, a consistency check will be made making sure the refered element
     * is also part of the same media package. If not, a [MediaPackageException] will be thrown.
     *
     * @param reference
     * the reference
     */
    fun referTo(reference: MediaPackageReference)

    /**
     * Removes any reference.
     */
    fun clearReference()

    /**
     * Create a deep copy of this object.
     *
     * @return The copy
     */
    public override fun clone(): Any

}
