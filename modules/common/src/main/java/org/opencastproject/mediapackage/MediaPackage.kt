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

import org.opencastproject.mediapackage.identifier.Id

import java.net.URI
import java.util.Date

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * Interface for a media package, which is a data container moving through the system, containing metadata, tracks and
 * attachments.
 */
@XmlJavaTypeAdapter(MediaPackageImpl.Adapter::class)
interface MediaPackage : Cloneable {

    /**
     * Returns the media package identifier.
     *
     * @return the identifier
     */
    var identifier: Id

    /**
     * Returns the title for the associated series, if any.
     *
     * @return The series title
     */
    var seriesTitle: String

    /**
     * Returns the title of the episode that this mediapackage represents.
     *
     * @return The episode title
     */
    var title: String

    /**
     * Returns the names of the institutions or people who created this mediapackage
     *
     * @return the creators of this mediapackage
     */
    val creators: Array<String>

    /**
     * Returns the series, if any, to which this mediapackage belongs
     *
     * @return the series
     */
    var series: String

    /**
     * The license for the content in this mediapackage
     *
     * @return the license
     */
    var license: String

    /**
     * Returns the names of the institutions or people who contributed to the content within this mediapackage
     *
     * @return the contributors
     */
    val contributors: Array<String>

    /**
     * Returns the language written and/or spoken in the media content of this mediapackage
     *
     * @return the language
     */
    var language: String

    /**
     * The keywords describing the subject(s) or categories describing the content of this mediapackage
     *
     * @return the subjects
     */
    val subjects: Array<String>

    /**
     * Returns the media package start time.
     *
     * @return the start time
     */
    var date: Date

    /**
     * Returns the media package duration in milliseconds or `null` if no duration is available.
     *
     * @return the duration
     */
    /**
     * Sets the duration of the media package in milliseconds. This method will throw an [IllegalStateException] if
     * tracks have been added to the mediapackage already. Also note that as soon as the first track is added, the
     * duration will be udpated according to the track's length.
     *
     * @param duration
     * the duration in milliseconds
     * @throws IllegalStateException
     * if the mediapackage already contains a track
     */
    var duration: Long?

    /**
     * Returns all of the elements.
     *
     * @return the elements
     */
    val elements: Array<MediaPackageElement>

    /**
     * Returns the tracks that are part of this media package.
     *
     * @return the tracks
     */
    val tracks: Array<Track>

    /**
     * Returns the attachments that are part of this media package.
     *
     * @return the attachments
     */
    val attachments: Array<Attachment>

    /**
     * Returns the presentations that are part of this media package.
     *
     * @return the attachments
     */
    val publications: Array<Publication>

    /**
     * Returns the catalogs associated with this media package.
     *
     * @return the catalogs
     */
    val catalogs: Array<Catalog>

    /**
     * Returns media package elements that are neither, attachments, catalogs nor tracks.
     *
     * @return the other media package elements
     */
    val unclassifiedElements: Array<MediaPackageElement>

    fun addCreator(creator: String)

    fun removeCreator(creator: String)

    fun addContributor(contributor: String)

    fun removeContributor(contributor: String)

    fun addSubject(subject: String)

    fun removeSubject(subject: String)

    /**
     * Returns `true` if the given element is part of the media package.
     *
     * @param element
     * the element
     * @return `true` if the element belongs to the media package
     */
    operator fun contains(element: MediaPackageElement): Boolean

    /**
     * Returns an iteration of the media package elements.
     *
     * @return the media package elements
     */
    fun elements(): Iterable<MediaPackageElement>

    /**
     * Returns the element that is identified by the given reference or `null` if no such element exists.
     *
     * @param reference
     * the reference
     * @return the element
     */
    fun getElementByReference(reference: MediaPackageReference): MediaPackageElement

    /**
     * Returns the element that is identified by the given identifier or `null` if no such element exists.
     *
     * @param id
     * the element identifier
     * @return the element
     */
    fun getElementById(id: String): MediaPackageElement

    /**
     * Returns the elements that are tagged with the given tag or an empty array if no such elements are found.
     *
     * @param tag
     * the tag
     * @return the elements
     */
    fun getElementsByTag(tag: String): Array<MediaPackageElement>

    /**
     * Returns the elements that are tagged with any of the given tags or an empty array if no such elements are found. If
     * any of the tags in the `tags` collection start with a '-' character, any elements matching the tag will
     * be excluded from the returned MediaPackageElement[]. If `tags` is empty or null, all elements are
     * returned.
     *
     * @param tags
     * the tags
     * @return the elements
     */
    fun getElementsByTags(tags: Collection<String>): Array<MediaPackageElement>

    /**
     * Returns all elements of this media package with the given flavor.
     *
     * @return the media package elements
     */
    fun getElementsByFlavor(flavor: MediaPackageElementFlavor): Array<MediaPackageElement>

    /**
     * Returns the track identified by `trackId` or `null` if that track doesn't exists.
     *
     * @param trackId
     * the track identifier
     * @return the tracks
     */
    fun getTrack(trackId: String): Track

    /**
     * Returns the tracks that are tagged with the given tag or an empty array if no such tracks are found.
     *
     * @param tag
     * the tag
     * @return the tracks
     */
    fun getTracksByTag(tag: String): Array<Track>

    /**
     * Returns the tracks that are tagged with any of the given tags or an empty array if no such elements are found. If
     * any of the tags in the `tags` collection start with a '-' character, any elements matching the tag will
     * be excluded from the returned Track[]. If `tags` is empty or null, all tracks are returned.
     *
     * @param tags
     * the tags
     * @return the tracks
     */
    fun getTracksByTags(tags: Collection<String>): Array<Track>

    /**
     * Returns the tracks that are part of this media package and match the given flavor as defined in [Track].
     *
     * @param flavor
     * the track's flavor
     * @return the tracks with the specified flavor
     */
    fun getTracks(flavor: MediaPackageElementFlavor): Array<Track>

    /**
     * Returns the tracks that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param reference
     * the reference
     * @return the tracks with the specified reference
     */
    fun getTracks(reference: MediaPackageReference): Array<Track>

    /**
     * Returns the tracks that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param reference
     * the reference
     * @param includeDerived
     * `true` to also include derived elements
     * @return the tracks with the specified reference
     */
    fun getTracks(reference: MediaPackageReference, includeDerived: Boolean): Array<Track>

    /**
     * Returns the tracks that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param flavor
     * the element flavor
     * @param reference
     * the reference
     * @return the tracks with the specified reference
     */
    fun getTracks(flavor: MediaPackageElementFlavor, reference: MediaPackageReference): Array<Track>

    /**
     * Returns `true` if the media package contains media tracks of any kind.
     *
     * @return `true` if the media package contains tracks
     */
    fun hasTracks(): Boolean

    /**
     * Returns the attachment identified by `attachmentId` or `null` if that attachment doesn't
     * exists.
     *
     * @param attachmentId
     * the attachment identifier
     * @return the attachments
     */
    fun getAttachment(attachmentId: String): Attachment

    /**
     * Returns the attachments that are tagged with the given tag or an empty array if no such attachments are found.
     *
     * @param tag
     * the tag
     * @return the attachments
     */
    fun getAttachmentsByTag(tag: String): Array<Attachment>

    /**
     * Returns the attachments that are tagged with any of the given tags or an empty array if no such attachments are
     * found. If any of the tags in the `tags` collection start with a '-' character, any elements matching the
     * tag will be excluded from the returned Attachment[]. If `tags` is empty or null, all attachments are
     * returned.
     *
     * @param tags
     * the tags
     * @return the attachments
     */
    fun getAttachmentsByTags(tags: Collection<String>): Array<Attachment>

    /**
     * Returns the attachments that are part of this media package and match the specified flavor.
     *
     * @param flavor
     * the attachment flavor
     * @return the attachments
     */
    fun getAttachments(flavor: MediaPackageElementFlavor): Array<Attachment>

    /**
     * Returns the attachments that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param reference
     * the reference
     * @return the attachments with the specified reference
     */
    fun getAttachments(reference: MediaPackageReference): Array<Attachment>

    /**
     * Returns the attachments that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param reference
     * the reference
     * @param includeDerived
     * `true` to also include derived elements
     * @return the attachments with the specified reference
     */
    fun getAttachments(reference: MediaPackageReference, includeDerived: Boolean): Array<Attachment>

    /**
     * Returns the attachments that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param flavor
     * the element flavor
     * @param reference
     * the reference
     * @return the attachments with the specified reference
     */
    fun getAttachments(flavor: MediaPackageElementFlavor, reference: MediaPackageReference): Array<Attachment>

    /**
     * Returns `true` if the media package contains attachments of any kind.
     *
     * @return `true` if the media package contains attachments
     */
    fun hasAttachments(): Boolean

    /**
     * Returns the catalog identified by `catalogId` or `null` if that catalog doesn't exists.
     *
     * @param catalogId
     * the catalog identifier
     * @return the catalogs
     */
    fun getCatalog(catalogId: String): Catalog

    /**
     * Returns the catalogs that are tagged with the given tag or an empty array if no such catalogs are found.
     *
     * @param tag
     * the tag
     * @return the catalogs
     */
    fun getCatalogsByTag(tag: String): Array<Catalog>

    /**
     * Returns the catalogs that are tagged with any of the given tags or an empty array if no such elements are found. If
     * any of the tags in the `tags` collection start with a '-' character, any elements matching the tag will
     * be excluded from the returned Catalog[]. If `tags` is empty or null, all catalogs are returned.
     *
     * @param tags
     * the tags
     * @return the catalogs
     */
    fun getCatalogsByTags(tags: Collection<String>): Array<Catalog>

    /**
     * Returns the catalogs associated with this media package that matches the specified flavor.
     *
     * @param flavor
     * the catalog type
     * @return the media package catalogs
     */
    fun getCatalogs(flavor: MediaPackageElementFlavor): Array<Catalog>

    /**
     * Returns the catalogs that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param reference
     * the reference
     * @return the catalogs with the specified reference
     */
    fun getCatalogs(reference: MediaPackageReference): Array<Catalog>

    /**
     * Returns the catalogs that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param reference
     * the reference
     * @param includeDerived
     * `true` to also include derived elements
     * @return the catalogs with the specified reference
     */
    fun getCatalogs(reference: MediaPackageReference, includeDerived: Boolean): Array<Catalog>

    /**
     * Returns the catalogs that are part of this media package and are refering to the element identified by
     * `reference`.
     *
     * @param flavor
     * the element flavor
     * @param reference
     * the reference
     * @return the catalogs with the specified reference
     */
    fun getCatalogs(flavor: MediaPackageElementFlavor, reference: MediaPackageReference): Array<Catalog>

    /**
     * Returns `true` if the media package contains catalogs of any kind.
     *
     * @return `true` if the media package contains catalogs
     */
    fun hasCatalogs(): Boolean

    /**
     * Returns media package elements that are neither, attachments, catalogs nor tracks but have the given element
     * flavor.
     *
     * @param flavor
     * the element flavor
     * @return the other media package elements
     */
    fun getUnclassifiedElements(flavor: MediaPackageElementFlavor): Array<MediaPackageElement>

    /**
     * Returns `true` if the media package contains unclassified elements.
     *
     * @return `true` if the media package contains unclassified elements
     */
    fun hasUnclassifiedElements(): Boolean

    /**
     * Returns `true` if the media package contains unclassified elements matching the specified element type.
     *
     * @param flavor
     * element flavor of the unclassified element
     * @return `true` if the media package contains unclassified elements
     */
    fun hasUnclassifiedElements(flavor: MediaPackageElementFlavor): Boolean

    /**
     * Adds an arbitrary [URI] to this media package, utilizing a [MediaPackageBuilder] to create a suitable
     * media package element out of the url. If the content cannot be recognized as being either a metadata catalog or
     * multimedia track, it is added as an attachment.
     *
     * @param uri
     * the element location
     */
    fun add(uri: URI): MediaPackageElement

    /**
     * Adds an arbitrary [URI] to this media package, utilizing a [MediaPackageBuilder] to create a suitable
     * media package element out of the url. If the content cannot be recognized as being either a metadata catalog or
     * multimedia track, it is added as an attachment.
     *
     * @param uri
     * the element location
     * @param type
     * the element type
     * @param flavor
     * the element flavor
     */
    fun add(uri: URI, type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement

    /**
     * Adds an arbitrary [MediaPackageElement] to this media package.
     *
     * @param element
     * the element
     */
    fun add(element: MediaPackageElement)

    /**
     * Adds a track to this media package, actually *moving* the underlying file in the filesystem. Use this method
     * *only* if you do not need the track in its originial place anymore.
     *
     *
     * Depending on the implementation, this method may provide significant performance benefits over copying the track.
     *
     * @param track
     * the track
     */
    fun add(track: Track)

    /**
     * Removes the element with the given identifier from the mediapackage and returns it.
     *
     * @param id
     * the element identifier
     */
    fun removeElementById(id: String): MediaPackageElement

    /**
     * Removes the track from the media package.
     *
     * @param track
     * the track
     */
    fun remove(track: Track)

    /**
     * Adds catalog information to this media package.
     *
     * @param catalog
     * the catalog
     */
    fun add(catalog: Catalog)

    /**
     * Removes the catalog from the media package.
     *
     * @param catalog
     * the catalog
     */
    fun remove(catalog: Catalog)

    /**
     * Adds an attachment to this media package.
     *
     * @param attachment
     * the attachment
     */
    fun add(attachment: Attachment)

    /**
     * Removes an arbitrary media package element.
     *
     * @param element
     * the media package element
     */
    fun remove(element: MediaPackageElement)

    /**
     * Removes the attachment from the media package.
     *
     * @param attachment
     * the attachment
     */
    fun remove(attachment: Attachment)

    /**
     * Adds an element to this media package that represents a derived version of `sourceElement`. Examples of
     * a derived element could be an encoded version of a track or a converted version of a time text captions file.
     *
     *
     * This method will add `derviedElement` to the media package and add a reference to the original element
     * `sourceElement`. Make sure that `derivedElement` features the right flavor, so that you are
     * later able to look up derived work using [.getDerived].
     *
     * @param derivedElement
     * the derived element
     * @param sourceElement
     * the source element
     */
    fun addDerived(derivedElement: MediaPackageElement, sourceElement: MediaPackageElement)

    /**
     * Adds an element to this media package that represents a derived version of `sourceElement`. Examples of
     * a derived element could be an encoded version of a track or a converted version of a time text captions file.
     *
     *
     * This method will add `derviedElement` to the media package and add a reference to the original element
     * `sourceElement`. Make sure that `derivedElement` features the right flavor, so that you are
     * later able to look up derived work using [.getDerived].
     *
     * @param derivedElement
     * the derived element
     * @param sourceElement
     * the source element
     * @param properties
     * properties for the reference that is being created
     */
    fun addDerived(derivedElement: MediaPackageElement, sourceElement: MediaPackageElement, properties: Map<String, String>)

    /**
     * Returns those media package elements that are derivates of `sourceElement` and feature the flavor
     * `derivateFlavor`. Using this method, you could easily look up e. g. flash-encoded versions of the
     * presenter track or converted versions of a time text captions file.
     *
     * @param sourceElement
     * the original track, catalog or attachment
     * @param derivateFlavor
     * the derivate flavor you are looking for
     * @return the derivates
     */
    fun getDerived(sourceElement: MediaPackageElement, derivateFlavor: MediaPackageElementFlavor): Array<MediaPackageElement>

    /**
     * Adds `observer` to the list of observers of this media package.
     *
     * @param observer
     * the observer
     */
    fun addObserver(observer: MediaPackageObserver)

    /**
     * Removes `observer` from the list of observers of this media package.
     *
     * @param observer
     * the observer
     */
    fun removeObserver(observer: MediaPackageObserver)

    /**
     * Verifies the media package consistency by checking the media package elements for mimetypes and checksums.
     *
     * @throws MediaPackageException
     * if an error occurs while checking the media package
     */
    @Throws(MediaPackageException::class)
    fun verify()

    /**
     * Renames the media package to the new identifier.
     *
     * @param identifier
     * the identifier TODO @return `true` if the media package could be renamed
     */
    fun renameTo(identifier: Id)

    /**
     * Creates a deep copy of the media package.
     *
     * @return the cloned media package
     */
    public override fun clone(): Any

}
