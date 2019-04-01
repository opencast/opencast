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

import org.opencastproject.mediapackage.MediaPackageSupport.Filters.presentations
import org.opencastproject.util.data.Monadics.mlist

import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.identifier.Id
import org.opencastproject.mediapackage.identifier.IdBuilder
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.IoSupport

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashSet
import java.util.TreeSet
import java.util.UUID

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Unmarshaller
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * Default implementation for a media media package.
 */
@XmlType(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org", propOrder = ["title", "series", "seriesTitle", "creators", "contributors", "subjects", "license", "language", "tracks", "catalogs", "attachments", "publications"])
@XmlRootElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
class MediaPackageImpl
/**
 * Creates a media package object with the media package identifier.
 *
 * @param id
 * the media package identifier
 */
@JvmOverloads internal constructor(id: Id = idBuilder.createNew()) : MediaPackage {

    /** List of observers  */
    private val observers = ArrayList<MediaPackageObserver>()

    /** The media package element builder, may remain `null`  */
    private var mediaPackageElementBuilder: MediaPackageElementBuilder? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTitle
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setTitle
     */
    @XmlElement(name = "title")
    override var title: String? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getSeriesTitle
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setSeriesTitle
     */
    @XmlElement(name = "seriestitle")
    override var seriesTitle: String? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getLanguage
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setLanguage
     */
    @XmlElement(name = "language")
    override var language: String? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getSeries
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setSeries
     */
    @XmlElement(name = "series")
    override var series: String? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getLicense
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setLicense
     */
    @XmlElement(name = "license")
    override var license: String? = null

    @XmlElementWrapper(name = "creators")
    @XmlElement(name = "creator")
    private var creators: MutableSet<String>? = null

    @XmlElementWrapper(name = "contributors")
    @XmlElement(name = "contributor")
    private var contributors: MutableSet<String>? = null

    @XmlElementWrapper(name = "subjects")
    @XmlElement(name = "subject")
    private var subjects: MutableSet<String>? = null

    /** The media package's identifier  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getIdentifier
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setIdentifier
     */
    @get:XmlAttribute(name = "id")
    override var identifier: Id? = null

    /** The start date and time  */
    private var startTime = 0L

    /** The media package duration  */
    private var duration: Long? = null

    /** The media package's other (uncategorized) files  */
    private val elements = ArrayList<MediaPackageElement>()

    /** Number of tracks  */
    private var tracks = 0

    /** Number of metadata catalogs  */
    private var catalogs = 0

    /** Number of attachments  */
    private var attachments = 0

    /** Numer of unclassified elements  */
    private var others = 0

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getDate
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setDate
     */
    override var date: Date?
        get() = Date(startTime)
        set(date) = if (date != null)
            this.startTime = date.time
        else
            this.startTime = 0

    /**
     * Returns the recording time in utc format.
     *
     * @return the recording time
     */
    /**
     * Sets the date and time of recording in utc format.
     *
     * @param startTime
     * the start time
     */
    var startDateAsString: String?
        @XmlAttribute(name = "start")
        get() = if (startTime == 0L) null else DateTimeSupport.toUTC(startTime)
        set(startTime) = if (startTime != null && "0" != startTime && !startTime.isEmpty()) {
            try {
                this.startTime = DateTimeSupport.fromUTC(startTime)
            } catch (e: Exception) {
                logger.info("Unable to parse start time {}", startTime)
            }

        } else {
            this.startTime = 0
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getUnclassifiedElements
     */
    override val unclassifiedElements: Array<MediaPackageElement>
        get() = getUnclassifiedElements(null)

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachments
     */
    override var publications: Array<Publication>
        @XmlElementWrapper(name = "publications")
        @XmlElement(name = "publication")
        get() = mlist(elements).bind(presentations).value().toTypedArray<Publication>()
        internal set(publications) {
            val newPublications = Arrays.asList(*publications)
            val oldPublications = Arrays.asList(*publications)
            for (oldp in oldPublications) {
                if (!newPublications.contains(oldp)) {
                    remove(oldp)
                }
            }
            for (newp in newPublications) {
                if (!oldPublications.contains(newp)) {
                    add(newp)
                }
            }
        }

    init {
        this.identifier = id
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getDuration
     */
    @XmlAttribute(name = "duration")
    override fun getDuration(): Long? {
        if (duration == null && hasTracks()) {
            for (t in getTracks()) {
                if (t.duration != null) {
                    if (duration == null || duration < t.duration)
                        duration = t.duration
                }
            }
        }
        return duration
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.setDuration
     */
    @Throws(IllegalStateException::class)
    override fun setDuration(duration: Long?) {
        if (hasTracks())
            throw IllegalStateException(
                    "The duration is determined by the length of the tracks and cannot be set manually")
        this.duration = duration
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.elements
     */
    override fun elements(): Iterable<MediaPackageElement> {
        return Arrays.asList(*getElements())
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getElements
     */
    override fun getElements(): Array<MediaPackageElement> {
        return elements.toTypedArray<MediaPackageElement>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getElementByReference
     */
    override fun getElementByReference(reference: MediaPackageReference): MediaPackageElement? {
        for (e in this.elements) {
            if (!reference.type.equals(e.elementType.toString(), ignoreCase = true))
                continue
            if (reference.identifier == e.identifier)
                return e
        }
        return null
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.getElementById
     */
    override fun getElementById(id: String): MediaPackageElement? {
        for (element in getElements()) {
            if (id == element.identifier)
                return element
        }
        return null
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.getElementById
     */
    override fun getElementsByTag(tag: String): Array<MediaPackageElement> {
        val result = ArrayList<MediaPackageElement>()
        for (element in getElements()) {
            if (element.containsTag(tag)) {
                result.add(element)
            }
        }
        return result.toTypedArray<MediaPackageElement>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getElementsByTags
     */
    override fun getElementsByTags(tags: Collection<String>?): Array<MediaPackageElement> {
        if (tags == null || tags.isEmpty())
            return getElements()
        val keep = HashSet<String>()
        val lose = HashSet<String>()
        for (tag in tags) {
            if (StringUtils.isBlank(tag))
                continue
            if (tag.startsWith(NEGATE_TAG_PREFIX)) {
                lose.add(tag.substring(NEGATE_TAG_PREFIX.length))
            } else {
                keep.add(tag)
            }
        }
        val result = ArrayList<MediaPackageElement>()
        for (element in getElements()) {
            var add = false
            for (elementTag in element.tags) {
                if (lose.contains(elementTag)) {
                    add = false
                    break
                } else if (keep.contains(elementTag)) {
                    add = true
                }
            }
            if (add) {
                result.add(element)
            }
        }
        return result.toTypedArray<MediaPackageElement>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachmentsByTags
     */
    override fun getAttachmentsByTags(tags: Collection<String>): Array<Attachment> {
        val matchingElements = getElementsByTags(tags)
        val attachments = ArrayList<Attachment>()
        for (element in matchingElements) {
            if (Attachment.TYPE == element.elementType) {
                attachments.add(element as Attachment)
            }
        }
        return attachments.toTypedArray<Attachment>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalogsByTags
     */
    override fun getCatalogsByTags(tags: Collection<String>): Array<Catalog> {
        val matchingElements = getElementsByTags(tags)
        val catalogs = ArrayList<Catalog>()
        for (element in matchingElements) {
            if (Catalog.TYPE == element.elementType) {
                catalogs.add(element as Catalog)
            }
        }
        return catalogs.toTypedArray<Catalog>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTracksByTags
     */
    override fun getTracksByTags(tags: Collection<String>): Array<Track> {
        val matchingElements = getElementsByTags(tags)
        val tracks = ArrayList<Track>()
        for (element in matchingElements) {
            if (Track.TYPE == element.elementType) {
                tracks.add(element as Track)
            }
        }
        return tracks.toTypedArray<Track>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getElementsByFlavor
     */
    override fun getElementsByFlavor(flavor: MediaPackageElementFlavor?): Array<MediaPackageElement> {
        if (flavor == null)
            throw IllegalArgumentException("Flavor cannot be null")

        val elements = ArrayList<MediaPackageElement>()
        for (element in getElements()) {
            if (flavor.matches(element.flavor))
                elements.add(element)
        }
        return elements.toTypedArray<MediaPackageElement>()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.contains
     */
    override fun contains(element: MediaPackageElement?): Boolean {
        if (element == null)
            throw IllegalArgumentException("Media package element must not be null")
        return elements.contains(element)
    }

    /**
     * Returns `true` if the media package contains an element with the specified identifier.
     *
     * @param identifier
     * the identifier
     * @return `true` if the media package contains an element with this identifier
     */
    internal operator fun contains(identifier: String): Boolean {
        for (element in getElements()) {
            if (element.identifier == identifier)
                return true
        }
        return false
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.add
     */
    override fun add(catalog: Catalog) {
        integrateCatalog(catalog)
        addInternal(catalog)
        fireElementAdded(catalog)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.add
     */
    override fun add(track: Track) {
        integrateTrack(track)
        addInternal(track)
        fireElementAdded(track)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.add
     */
    override fun add(attachment: Attachment) {
        integrateAttachment(attachment)
        addInternal(attachment)
        fireElementAdded(attachment)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalog
     */
    override fun getCatalog(catalogId: String): Catalog? {
        synchronized(elements) {
            for (e in elements) {
                if (e.identifier == catalogId && e is Catalog)
                    return e
            }
        }
        return null
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalogs
     */
    @XmlElementWrapper(name = "metadata")
    @XmlElement(name = "catalog")
    override fun getCatalogs(): Array<Catalog> {
        val catalogs = loadCatalogs()
        return catalogs.toTypedArray<Catalog>()
    }

    internal fun setCatalogs(catalogs: Array<Catalog>) {
        val newCatalogs = Arrays.asList(*catalogs)
        val oldCatalogs = Arrays.asList(*getCatalogs())
        // remove any catalogs not in this array
        for (existing in oldCatalogs) {
            if (!newCatalogs.contains(existing)) {
                remove(existing)
            }
        }
        for (newCatalog in newCatalogs) {
            if (!oldCatalogs.contains(newCatalog)) {
                add(newCatalog)
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalogsByTag
     */
    override fun getCatalogsByTag(tag: String): Array<Catalog> {
        val result = ArrayList<Catalog>()
        synchronized(elements) {
            for (e in elements) {
                if (e is Catalog && e.containsTag(tag))
                    result.add(e)
            }
        }
        return result.toTypedArray<Catalog>()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalogs
     */
    override fun getCatalogs(flavor: MediaPackageElementFlavor?): Array<Catalog> {
        if (flavor == null)
            throw IllegalArgumentException("Unable to filter by null criterion")

        // Go through catalogs and remove those that don't match
        val catalogs = loadCatalogs()
        val candidates = ArrayList(catalogs)
        for (c in catalogs) {
            if (c.flavor == null || !c.flavor.matches(flavor)) {
                candidates.remove(c)
            }
        }
        return candidates.toTypedArray<Catalog>()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalogs
     */
    override fun getCatalogs(reference: MediaPackageReference): Array<Catalog> {
        return getCatalogs(reference, false)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalogs
     */
    override fun getCatalogs(reference: MediaPackageReference?, includeDerived: Boolean): Array<Catalog> {
        if (reference == null)
            throw IllegalArgumentException("Unable to filter by null reference")

        // Go through catalogs and remove those that don't match
        val catalogs = loadCatalogs()
        val candidates = ArrayList(catalogs)
        for (c in catalogs) {
            var r: MediaPackageReference? = c.reference
            if (!reference.matches(r)) {
                var indirectHit = false

                // Create a reference that will match regardless of properties
                val elementRef = MediaPackageReferenceImpl(reference.type, reference.identifier)

                // Try to find a derived match if possible
                while (includeDerived && r != null) {
                    if (r.matches(elementRef)) {
                        indirectHit = true
                        break
                    }
                    r = getElement(r)!!.reference
                }

                if (!indirectHit)
                    candidates.remove(c)
            }
        }

        return candidates.toTypedArray<Catalog>()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.getCatalogs
     */
    override fun getCatalogs(flavor: MediaPackageElementFlavor?, reference: MediaPackageReference?): Array<Catalog> {
        if (flavor == null)
            throw IllegalArgumentException("Unable to filter by null criterion")
        if (reference == null)
            throw IllegalArgumentException("Unable to filter by null reference")

        // Go through catalogs and remove those that don't match
        val catalogs = loadCatalogs()
        val candidates = ArrayList(catalogs)
        for (c in catalogs) {
            if (flavor != c.flavor || c.reference != null && !c.reference.matches(reference)) {
                candidates.remove(c)
            }
        }
        return candidates.toTypedArray<Catalog>()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.hasCatalogs
     */
    override fun hasCatalogs(): Boolean {
        synchronized(elements) {
            for (e in elements) {
                if (e is Catalog)
                    return true
            }
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTrack
     */
    override fun getTrack(trackId: String): Track? {
        synchronized(elements) {
            for (e in elements) {
                if (e.identifier == trackId && e is Track)
                    return e
            }
        }
        return null
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTracks
     */
    @XmlElementWrapper(name = "media")
    @XmlElement(name = "track")
    override fun getTracks(): Array<Track> {
        val tracks = loadTracks()
        return tracks.toTypedArray<Track>()
    }

    internal fun setTracks(tracks: Array<Track>) {
        val newTracks = Arrays.asList(*tracks)
        val oldTracks = Arrays.asList(*getTracks())
        // remove any catalogs not in this array
        for (existing in oldTracks) {
            if (!newTracks.contains(existing)) {
                remove(existing)
            }
        }
        for (newTrack in newTracks) {
            if (!oldTracks.contains(newTrack)) {
                add(newTrack)
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTracksByTag
     */
    override fun getTracksByTag(tag: String): Array<Track> {
        val result = ArrayList<Track>()
        synchronized(elements) {
            for (e in elements) {
                if (e is Track && e.containsTag(tag))
                    result.add(e)
            }
        }
        return result.toTypedArray<Track>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTracks
     */
    override fun getTracks(flavor: MediaPackageElementFlavor?): Array<Track> {
        if (flavor == null)
            throw IllegalArgumentException("Unable to filter by null criterion")

        // Go through tracks and remove those that don't match
        val tracks = loadTracks()
        val candidates = ArrayList(tracks)
        for (a in tracks) {
            if (a.flavor == null || !a.flavor.matches(flavor)) {
                candidates.remove(a)
            }
        }
        return candidates.toTypedArray<Track>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTracks
     */
    override fun getTracks(reference: MediaPackageReference): Array<Track> {
        return getTracks(reference, false)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTracks
     */
    override fun getTracks(reference: MediaPackageReference?, includeDerived: Boolean): Array<Track> {
        if (reference == null)
            throw IllegalArgumentException("Unable to filter by null reference")

        // Go through tracks and remove those that don't match
        val tracks = loadTracks()
        val candidates = ArrayList(tracks)
        for (t in tracks) {
            var r: MediaPackageReference? = t.reference
            if (!reference.matches(r)) {
                var indirectHit = false

                // Create a reference that will match regardless of properties
                val elementRef = MediaPackageReferenceImpl(reference.type, reference.identifier)

                // Try to find a derived match if possible
                while (includeDerived && r != null) {
                    if (r.matches(elementRef)) {
                        indirectHit = true
                        break
                    }
                    r = getElement(r)!!.reference
                }

                if (!indirectHit)
                    candidates.remove(t)
            }
        }

        return candidates.toTypedArray<Track>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getTracks
     */
    override fun getTracks(flavor: MediaPackageElementFlavor?, reference: MediaPackageReference?): Array<Track> {
        if (flavor == null)
            throw IllegalArgumentException("Unable to filter by null criterion")
        if (reference == null)
            throw IllegalArgumentException("Unable to filter by null reference")

        // Go through tracks and remove those that don't match
        val tracks = loadTracks()
        val candidates = ArrayList(tracks)
        for (a in tracks) {
            if (flavor != a.flavor || !reference.matches(a.reference)) {
                candidates.remove(a)
            }
        }
        return candidates.toTypedArray<Track>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.hasTracks
     */
    override fun hasTracks(): Boolean {
        synchronized(elements) {
            for (e in elements) {
                if (e is Track)
                    return true
            }
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getUnclassifiedElements
     */
    override fun getUnclassifiedElements(flavor: MediaPackageElementFlavor?): Array<MediaPackageElement> {
        val unclassifieds = ArrayList<MediaPackageElement>()
        synchronized(elements) {
            for (e in elements) {
                if (e !is Attachment && e !is Catalog && e !is Track) {
                    if (flavor == null || flavor == e.flavor) {
                        unclassifieds.add(e)
                    }
                }
            }
        }
        return unclassifieds.toTypedArray<MediaPackageElement>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.hasUnclassifiedElements
     */
    override fun hasUnclassifiedElements(type: MediaPackageElementFlavor?): Boolean {
        if (type == null)
            return others > 0
        synchronized(elements) {
            for (e in elements) {
                if (e !is Attachment && e !is Catalog && e !is Track) {
                    if (type == e.flavor) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.hasUnclassifiedElements
     */
    override fun hasUnclassifiedElements(): Boolean {
        return hasUnclassifiedElements(null)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.addObserver
     */
    override fun addObserver(observer: MediaPackageObserver) {
        synchronized(observers) {
            observers.add(observer)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachment
     */
    override fun getAttachment(attachmentId: String): Attachment? {
        synchronized(elements) {
            for (e in elements) {
                if (e.identifier == attachmentId && e is Attachment)
                    return e
            }
        }
        return null
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachments
     */
    @XmlElementWrapper(name = "attachments")
    @XmlElement(name = "attachment")
    override fun getAttachments(): Array<Attachment> {
        val attachments = loadAttachments()
        return attachments.toTypedArray<Attachment>()
    }

    internal fun setAttachments(catalogs: Array<Attachment>) {
        val newAttachments = Arrays.asList(*catalogs)
        val oldAttachments = Arrays.asList(*getAttachments())
        // remove any catalogs not in this array
        for (existing in oldAttachments) {
            if (!newAttachments.contains(existing)) {
                remove(existing)
            }
        }
        for (newAttachment in newAttachments) {
            if (!oldAttachments.contains(newAttachment)) {
                add(newAttachment)
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachmentsByTag
     */
    override fun getAttachmentsByTag(tag: String): Array<Attachment> {
        val result = ArrayList<Attachment>()
        synchronized(elements) {
            for (e in elements) {
                if (e is Attachment && e.containsTag(tag))
                    result.add(e)
            }
        }
        return result.toTypedArray<Attachment>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachments
     */
    override fun getAttachments(flavor: MediaPackageElementFlavor?): Array<Attachment> {
        if (flavor == null)
            throw IllegalArgumentException("Unable to filter by null criterion")

        // Go through attachments and remove those that don't match
        val attachments = loadAttachments()
        val candidates = ArrayList(attachments)
        for (a in attachments) {
            if (a.flavor == null || !a.flavor.matches(flavor)) {
                candidates.remove(a)
            }
        }
        return candidates.toTypedArray<Attachment>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachments
     */
    override fun getAttachments(reference: MediaPackageReference): Array<Attachment> {
        return getAttachments(reference, false)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachments
     */
    override fun getAttachments(reference: MediaPackageReference?, includeDerived: Boolean): Array<Attachment> {
        if (reference == null)
            throw IllegalArgumentException("Unable to filter by null reference")

        // Go through attachments and remove those that don't match
        val attachments = loadAttachments()
        val candidates = ArrayList(attachments)
        for (a in attachments) {
            var r: MediaPackageReference? = a.reference
            if (!reference.matches(r)) {
                var indirectHit = false

                // Create a reference that will match regardless of properties
                val elementRef = MediaPackageReferenceImpl(reference.type, reference.identifier)

                // Try to find a derived match if possible
                while (includeDerived && getElement(r) != null && r != null) {
                    if (r.matches(elementRef)) {
                        indirectHit = true
                        break
                    }
                    r = getElement(r)!!.reference
                }

                if (!indirectHit)
                    candidates.remove(a)
            }
        }
        return candidates.toTypedArray<Attachment>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getAttachments
     */
    override fun getAttachments(flavor: MediaPackageElementFlavor?, reference: MediaPackageReference?): Array<Attachment> {
        if (flavor == null)
            throw IllegalArgumentException("Unable to filter by null criterion")
        if (reference == null)
            throw IllegalArgumentException("Unable to filter by null reference")

        // Go through attachments and remove those that don't match
        val attachments = loadAttachments()
        val candidates = ArrayList(attachments)
        for (a in attachments) {
            if (flavor != a.flavor || !reference.matches(a.reference)) {
                candidates.remove(a)
            }
        }
        return candidates.toTypedArray<Attachment>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.hasAttachments
     */
    override fun hasAttachments(): Boolean {
        synchronized(elements) {
            for (e in elements) {
                if (e is Attachment)
                    return true
            }
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.removeElementById
     */
    override fun removeElementById(id: String): MediaPackageElement? {
        val element = getElementById(id) ?: return null
        remove(element)
        return element
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.remove
     */
    override fun remove(element: MediaPackageElement) {
        removeElement(element)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.remove
     */
    override fun remove(attachment: Attachment) {
        removeElement(attachment)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.remove
     */
    override fun remove(catalog: Catalog) {
        removeElement(catalog)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.remove
     */
    override fun remove(track: Track) {
        duration = null
        removeElement(track)
    }

    /**
     * Removes an element from the media package
     *
     * @param element
     * the media package element
     */
    internal fun removeElement(element: MediaPackageElement) {
        removeInternal(element)
        fireElementRemoved(element)
        if (element is AbstractMediaPackageElement) {
            element.mediaPackage = null
        }
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.removeObserver
     */
    override fun removeObserver(observer: MediaPackageObserver) {
        synchronized(observers) {
            observers.remove(observer)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.add
     */
    override fun add(url: URI?): MediaPackageElement {
        if (url == null)
            throw IllegalArgumentException("Argument 'url' may not be null")

        if (mediaPackageElementBuilder == null) {
            mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        }
        val element = mediaPackageElementBuilder!!.elementFromURI(url)
        integrate(element)
        addInternal(element)
        fireElementAdded(element)
        return element
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.add
     */
    override fun add(uri: URI?, type: Type?, flavor: MediaPackageElementFlavor): MediaPackageElement {
        if (uri == null)
            throw IllegalArgumentException("Argument 'url' may not be null")
        if (type == null)
            throw IllegalArgumentException("Argument 'type' may not be null")

        if (mediaPackageElementBuilder == null) {
            mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        }
        val element = mediaPackageElementBuilder!!.elementFromURI(uri, type, flavor)
        integrate(element)
        addInternal(element)
        fireElementAdded(element)
        return element
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.add
     */
    override fun add(element: MediaPackageElement) {
        if (element.elementType == MediaPackageElement.Type.Track && element is Track) {
            integrateTrack(element)
        } else if (element.elementType == MediaPackageElement.Type.Catalog && element is Catalog) {
            integrateCatalog(element)
        } else if (element.elementType == MediaPackageElement.Type.Attachment && element is Attachment) {
            integrateAttachment(element)
        } else {
            integrate(element)
        }
        addInternal(element)
        fireElementAdded(element)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.addDerived
     */
    override fun addDerived(derivedElement: MediaPackageElement, sourceElement: MediaPackageElement) {
        addDerived(derivedElement, sourceElement, null)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.addDerived
     */
    override fun addDerived(derivedElement: MediaPackageElement?, sourceElement: MediaPackageElement?,
                            properties: Map<String, String>?) {
        if (derivedElement == null)
            throw IllegalArgumentException("The derived element is null")
        if (sourceElement == null)
            throw IllegalArgumentException("The source element is null")
        if (!contains(sourceElement))
            throw IllegalStateException("The sourceElement needs to be part of the media package")

        derivedElement.referTo(sourceElement)
        addInternal(derivedElement)

        if (properties != null) {
            val ref = derivedElement.reference
            for ((key, value) in properties) {
                ref.setProperty(key, value)
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getDerived
     */
    override fun getDerived(sourceElement: MediaPackageElement?, derivateFlavor: MediaPackageElementFlavor?): Array<MediaPackageElement> {
        if (sourceElement == null)
            throw IllegalArgumentException("Source element cannot be null")
        if (derivateFlavor == null)
            throw IllegalArgumentException("Derivate flavor cannot be null")

        val reference = MediaPackageReferenceImpl(sourceElement)
        val elements = ArrayList<MediaPackageElement>()
        for (element in getElements()) {
            if (derivateFlavor == element.flavor && reference == element.reference)
                elements.add(element)
        }
        return elements.toTypedArray<MediaPackageElement>()
    }

    /**
     * Notify observers of a removed media package element.
     *
     * @param element
     * the removed element
     */
    private fun fireElementAdded(element: MediaPackageElement) {
        synchronized(observers) {
            for (o in observers) {
                try {
                    o.elementAdded(element)
                } catch (th: Throwable) {
                    logger.error("MediaPackageOberserver $o throw exception while processing callback", th)
                }

            }
        }
    }

    /**
     * Notify observers of a removed media package element.
     *
     * @param element
     * the removed element
     */
    private fun fireElementRemoved(element: MediaPackageElement) {
        synchronized(observers) {
            for (o in observers) {
                try {
                    o.elementRemoved(element)
                } catch (th: Throwable) {
                    logger.error("MediaPackageObserver $o threw exception while processing callback", th)
                }

            }
        }
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.renameTo
     */
    override fun renameTo(identifier: Id) {
        this.identifier = identifier
    }

    /**
     * Integrates the element into the media package. This mainly involves moving the element into the media package file
     * structure.
     *
     * @param element
     * the element to integrate
     */
    private fun integrate(element: MediaPackageElement) {
        if (element is AbstractMediaPackageElement)
            element.mediaPackage = this
    }

    /**
     * Integrates the catalog into the media package. This mainly involves moving the catalog into the media package file
     * structure.
     *
     * @param catalog
     * the catalog to integrate
     */
    private fun integrateCatalog(catalog: Catalog) {
        // Check (uniqueness of) catalog identifier
        val id = catalog.identifier
        if (id == null || contains(id)) {
            catalog.identifier = createElementIdentifier()
        }
        integrate(catalog)
    }

    /**
     * Integrates the track into the media package. This mainly involves moving the track into the media package file
     * structure.
     *
     * @param track
     * the track to integrate
     */
    private fun integrateTrack(track: Track) {
        // Check (uniqueness of) track identifier
        val id = track.identifier
        if (id == null || contains(id)) {
            track.identifier = createElementIdentifier()
        }
        duration = null
        integrate(track)
    }

    /**
     * Integrates the attachment into the media package. This mainly involves moving the attachment into the media package
     * file structure.
     *
     * @param attachment
     * the attachment to integrate
     */
    private fun integrateAttachment(attachment: Attachment) {
        // Check (uniqueness of) attachment identifier
        val id = attachment.identifier
        if (id == null || contains(id)) {
            attachment.identifier = createElementIdentifier()
        }
        integrate(attachment)
    }

    /**
     * Returns a media package element identifier with the given prefix and the given number or a higher one as the
     * suffix. The identifier will be unique within the media package.
     *
     * @param prefix
     * the identifier prefix
     * @param count
     * the number
     * @return the element identifier
     */
    private fun createElementIdentifier(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackage.verify
     */
    @Throws(MediaPackageException::class)
    override fun verify() {
        for (e in getElements()) {
            e.verify()
        }
    }

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return identifier!!.hashCode()
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        return if (obj is MediaPackage) {
            identifier == obj.identifier
        } else false
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.clone
     */
    override fun clone(): Any {
        try {
            val xml = MediaPackageParser.getAsXml(this)
            return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()!!.loadFromXml(xml)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    /**
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return if (identifier != null)
            identifier!!.toString()
        else
            "Unknown media package"
    }

    /**
     * A JAXB adapter that allows the [MediaPackage] interface to be un/marshalled
     */
    class Adapter : XmlAdapter<MediaPackageImpl, MediaPackage>() {
        @Throws(Exception::class)
        override fun marshal(mp: MediaPackage): MediaPackageImpl {
            return mp as MediaPackageImpl
        }

        @Throws(Exception::class)
        override fun unmarshal(mp: MediaPackageImpl): MediaPackage {
            return mp
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getContributors
     */
    override fun getContributors(): Array<String> {
        return if (contributors == null) arrayOf() else contributors!!.toTypedArray<String>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getCreators
     */
    override fun getCreators(): Array<String> {
        return if (creators == null) arrayOf() else creators!!.toTypedArray<String>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.getSubjects
     */
    override fun getSubjects(): Array<String> {
        return if (subjects == null) arrayOf() else subjects!!.toTypedArray<String>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.addContributor
     */
    override fun addContributor(contributor: String) {
        if (contributors == null)
            contributors = TreeSet()
        contributors!!.add(contributor)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.addCreator
     */
    override fun addCreator(creator: String) {
        if (creators == null)
            creators = TreeSet()
        creators!!.add(creator)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.addSubject
     */
    override fun addSubject(subject: String) {
        if (subjects == null)
            subjects = TreeSet()
        subjects!!.add(subject)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.removeContributor
     */
    override fun removeContributor(contributor: String) {
        if (contributors != null)
            contributors!!.remove(contributor)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.removeCreator
     */
    override fun removeCreator(creator: String) {
        if (creators != null)
            creators!!.remove(creator)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackage.removeSubject
     */
    override fun removeSubject(subject: String) {
        if (subjects != null)
            subjects!!.remove(subject)
    }

    /**
     * Returns the media package element that matches the given reference.
     *
     * @param reference
     * the reference
     * @return the element
     */
    internal fun getElement(reference: MediaPackageReference?): MediaPackageElement? {
        if (reference == null)
            return null
        for (e in elements) {
            if (e.identifier == reference.identifier)
                return e
        }
        return null
    }

    /**
     * Registers a new media package element with this manifest.
     *
     * @param element
     * the new element
     */
    internal fun addInternal(element: MediaPackageElement?) {
        if (element == null)
            throw IllegalArgumentException("Media package element must not be null")
        var id: String? = null
        if (elements.add(element)) {
            if (element is Track) {
                tracks++
                id = "track-$tracks"
                val duration = element.duration
                // Todo Do not demand equal durations for now... This is an issue that has to be discussed further
                // if (this.duration > 0 && this.duration != duration)
                // throw new MediaPackageException("Track " + element + " cannot be added due to varying duration (" + duration
                // +
                // " instead of " + this.duration +")");
                // else
                if (this.duration == null)
                    this.duration = duration
            } else if (element is Attachment) {
                attachments++
                id = "attachment-$attachments"
            } else if (element is Catalog) {
                catalogs++
                id = "catalog-$catalogs"
            } else {
                others++
                id = "unknown-$others"
            }
        }

        // Check if element has an id
        if (element.identifier == null) {
            if (element is AbstractMediaPackageElement) {
                element.identifier = id
            } else
                throw UnsupportedElementException(element, "Found unkown element without id")
        }
    }

    /**
     * Removes the media package element from the manifest.
     *
     * @param element
     * the element to remove
     */
    internal fun removeInternal(element: MediaPackageElement?) {
        if (element == null)
            throw IllegalArgumentException("Media package element must not be null")
        if (elements.remove(element)) {
            if (element is Track) {
                tracks--
                if (tracks == 0)
                    duration = null
            } else if (element is Attachment)
                attachments--
            else if (element is Catalog)
                catalogs--
            else
                others--
        }
    }

    /**
     * Extracts the list of tracks from the media package.
     *
     * @return the tracks
     */
    private fun loadTracks(): Collection<Track> {
        val tracks = ArrayList<Track>()
        synchronized(elements) {
            for (e in elements) {
                if (e is Track) {
                    tracks.add(e)
                }
            }
        }
        return tracks
    }

    /**
     * Extracts the list of catalogs from the media package.
     *
     * @return the catalogs
     */
    private fun loadCatalogs(): Collection<Catalog> {
        val catalogs = ArrayList<Catalog>()
        synchronized(elements) {
            for (e in elements) {
                if (e is Catalog) {
                    catalogs.add(e)
                }
            }
        }
        return catalogs
    }

    /**
     * Extracts the list of attachments from the media package.
     *
     * @return the attachments
     */
    private fun loadAttachments(): Collection<Attachment> {
        val attachments = ArrayList<Attachment>()
        synchronized(elements) {
            for (e in elements) {
                if (e is Attachment) {
                    attachments.add(e)
                }
            }
        }
        return attachments
    }

    companion object {

        /** the logging facility provided by log4j  */
        private val logger = LoggerFactory.getLogger(MediaPackageImpl::class.java!!.getName())

        /**
         * The prefix indicating that a tag should be excluded from a search for elements using
         * [.getElementsByTags]
         */
        val NEGATE_TAG_PREFIX = "-"

        /** Context for serializing and deserializing  */
        internal val context: JAXBContext

        /** id builder, for internal use only  */
        private val idBuilder = UUIDIdBuilderImpl()

        init {
            try {
                context = JAXBContext.newInstance("org.opencastproject.mediapackage", MediaPackageImpl::class.java!!.getClassLoader())
            } catch (e: JAXBException) {
                throw RuntimeException(e)
            }

        }

        /**
         * Unmarshals XML representation of a MediaPackage via JAXB.
         *
         * @param xml
         * the serialized xml string
         * @return the deserialized media package
         * @throws MediaPackageException
         */
        @Throws(MediaPackageException::class)
        fun valueOf(xml: String): MediaPackageImpl {
            try {
                return MediaPackageImpl.valueOf(IOUtils.toInputStream(xml, "UTF-8"))
            } catch (e: IOException) {
                throw MediaPackageException(e)
            }

        }

        /**
         * Reads the media package from the input stream.
         *
         * @param xml
         * the input stream
         * @return the deserialized media package
         */
        @Throws(MediaPackageException::class)
        fun valueOf(xml: InputStream): MediaPackageImpl {
            try {
                val unmarshaller = context.createUnmarshaller()
                return unmarshaller.unmarshal<MediaPackageImpl>(StreamSource(xml), MediaPackageImpl::class.java).value
            } catch (e: JAXBException) {
                throw MediaPackageException(if (e.linkedException != null) e.linkedException else e)
            } finally {
                IoSupport.closeQuietly(xml)
            }
        }

        /**
         * Reads the media package from an xml node.
         *
         * @param xml
         * the node
         * @return the deserialized media package
         */
        @Throws(MediaPackageException::class)
        fun valueOf(xml: Node): MediaPackageImpl {
            var `in`: InputStream? = null
            var out: ByteArrayOutputStream? = null
            try {
                val unmarshaller = context.createUnmarshaller()

                // Serialize the media package
                val domSource = DOMSource(xml)
                out = ByteArrayOutputStream()
                val result = StreamResult(out)
                val transformer = TransformerFactory.newInstance().newTransformer()
                transformer.transform(domSource, result)
                `in` = ByteArrayInputStream(out.toByteArray())

                return unmarshaller.unmarshal<MediaPackageImpl>(StreamSource(`in`), MediaPackageImpl::class.java).value
            } catch (e: Exception) {
                throw MediaPackageException("Error deserializing media package node", e)
            } finally {
                IoSupport.closeQuietly(`in`)
                IoSupport.closeQuietly(out)
            }
        }
    }

}
/**
 * Creates a media package object.
 */
