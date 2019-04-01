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
import org.opencastproject.util.IoSupport
import org.opencastproject.util.MimeType

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.net.URI
import java.net.URISyntaxException
import java.util.SortedSet
import java.util.TreeSet

import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlID
import javax.xml.bind.annotation.XmlTransient

/**
 * This class provides base functionality for media package elements.
 */
@XmlTransient
@XmlAccessorType(XmlAccessType.NONE)
abstract class AbstractMediaPackageElement : MediaPackageElement, Serializable {

    /** The element identifier  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getIdentifier
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setIdentifier
     */
    @XmlID
    @XmlAttribute(name = "id")
    override var identifier: String? = null

    /** The element's type whithin the manifest: Track, Catalog etc.  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getElementType
     */
    override var elementType: MediaPackageElement.Type? = null
        protected set

    /** The element's description  */
    protected var description: String? = null

    /** The element's mime type, e. g. 'audio/mp3'  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getMimeType
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setMimeType
     */
    @XmlElement(name = "mimetype")
    override var mimeType: MimeType? = null

    /** The element's type, e. g. 'track/slide'  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getFlavor
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setFlavor
     */
    @XmlAttribute(name = "type")
    override var flavor: MediaPackageElementFlavor? = null

    /** The tags  */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    protected var tags: SortedSet<String>? = TreeSet()

    /** The element's location  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getURI
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setURI
     */
    @XmlElement(name = "url")
    override var uri: URI? = null

    /** Size in bytes  */
    @XmlElement(name = "size")
    protected var size: Long? = null

    /** The element's checksum  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getChecksum
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setChecksum
     */
    @XmlElement(name = "checksum")
    override var checksum: Checksum? = null

    /** The parent media package  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getMediaPackage
     */
    /**
     * Sets the parent media package.
     *
     *
     * **Note** This method is only used by the media package and should not be called from elsewhere.
     *
     * @param mediaPackage
     * the parent media package
     */
    override var mediaPackage: MediaPackage? = null
        internal set

    /** The optional reference to other elements or series  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getReference
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setReference
     */
    @XmlAttribute(name = "ref")
    override var reference: MediaPackageReference? = null

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getElementDescription
     */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setElementDescription
     */
    override var elementDescription: String
        get() = if (description != null) description else uri!!.toString()
        set(name) {
            this.description = name
        }

    /** Needed by JAXB  */
    protected constructor() {}

    /**
     * Creates a new media package element.
     *
     * @param elementType
     * the type, e. g. Track, Catalog etc.
     * @param flavor
     * the flavor
     * @param uri
     * the elements location
     */
    protected constructor(elementType: MediaPackageElement.Type, flavor: MediaPackageElementFlavor, uri: URI) : this(null, elementType, flavor, uri, null, null, null) {}

    /**
     * Creates a new media package element.
     *
     * @param elementType
     * the type, e. g. Track, Catalog etc.
     * @param flavor
     * the flavor
     * @param uri
     * the elements location
     * @param size
     * the element size in bytes
     * @param checksum
     * the element checksum
     * @param mimeType
     * the element mime type
     */
    protected constructor(elementType: MediaPackageElement.Type, flavor: MediaPackageElementFlavor, uri: URI, size: Long?,
                          checksum: Checksum, mimeType: MimeType) : this(null, elementType, flavor, uri, size, checksum, mimeType) {
    }

    /**
     * Creates a new media package element.
     *
     * @param id
     * the element identifier withing the package
     * @param elementType
     * the type, e. g. Track, Catalog etc.
     * @param flavor
     * the flavor
     * @param uri
     * the elements location
     * @param size
     * the element size in bytes
     * @param checksum
     * the element checksum
     * @param mimeType
     * the element mime type
     */
    protected constructor(id: String?, elementType: MediaPackageElement.Type?, flavor: MediaPackageElementFlavor, uri: URI,
                          size: Long?, checksum: Checksum?, mimeType: MimeType?) {
        if (elementType == null)
            throw IllegalArgumentException("Argument 'elementType' is null")
        this.identifier = id
        this.elementType = elementType
        this.flavor = flavor
        this.mimeType = mimeType
        this.uri = uri
        this.size = size
        this.checksum = checksum
        this.tags = TreeSet()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.addTag
     */
    override fun addTag(tag: String?) {
        if (tag == null)
            throw IllegalArgumentException("Tag must not be null")
        tags!!.add(tag)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.removeTag
     */
    override fun removeTag(tag: String?) {
        if (tag == null)
            return
        tags!!.remove(tag)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.containsTag
     */
    override fun containsTag(tag: String?): Boolean {
        return if (tag == null || tags == null) false else tags!!.contains(tag)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.containsTag
     */
    override fun containsTag(tags: Collection<String>?): Boolean {
        if (tags == null || tags.size == 0)
            return true
        for (tag in tags) {
            if (containsTag(tag))
                return true
        }
        return false
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getTags
     */
    override fun getTags(): Array<String> {
        return tags!!.toTypedArray<String>()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.clearTags
     */
    override fun clearTags() {
        if (tags != null)
            tags!!.clear()
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.getSize
     */
    override fun getSize(): Long {
        return if (size != null) size else -1
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.setSize
     */
    override fun setSize(size: Long) {
        this.size = size
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.referTo
     */
    override fun referTo(mediaPackage: MediaPackage) {
        referTo(MediaPackageReferenceImpl(mediaPackage))
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.referTo
     */
    override fun referTo(element: MediaPackageElement) {
        referTo(MediaPackageReferenceImpl(element))
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.referTo
     */
    override fun referTo(reference: MediaPackageReference) {
        // TODO: Check reference consistency
        this.reference = reference
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.clearReference
     */
    override fun clearReference() {
        this.reference = null
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElement.verify
     */
    @Throws(MediaPackageException::class)
    override fun verify() {
        // TODO: Check availability at url
        // TODO: Download (?) and check checksum
        // Checksum c = calculateChecksum();
        // if (checksum != null && !checksum.equals(c)) {
        // throw new MediaPackageException("Checksum mismatch for " + this);
        // }
        // checksum = c;
    }

    /**
     * @see java.lang.Comparable.compareTo
     */
    override fun compareTo(o: MediaPackageElement): Int {
        return uri!!.toString().compareTo(o.uri.toString())
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (obj !is MediaPackageElement)
            return false
        val e = obj as MediaPackageElement?
        if (mediaPackage != null && e!!.mediaPackage != null && mediaPackage != e.mediaPackage)
            return false
        if (identifier != null && identifier != e!!.identifier)
            return false
        return if (uri != null && uri != e!!.uri) false else true
    }

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (identifier == null) 0 else identifier!!.hashCode()
        result = prime * result + if (mediaPackage == null) 0 else mediaPackage!!.hashCode()
        result = prime * result + if (uri == null) 0 else uri!!.hashCode()
        return result
    }

    /**
     * @see org.opencastproject.mediapackage.ManifestContributor.toManifest
     */
    @Throws(MediaPackageException::class)
    override fun toManifest(document: Document, serializer: MediaPackageSerializer?): Node {
        val node = document.createElement(elementType!!.toString().toLowerCase())
        if (identifier != null)
            node.setAttribute("id", identifier)

        // Flavor
        if (flavor != null)
            node.setAttribute("type", flavor!!.toString())

        // Reference
        if (reference != null)
            if (mediaPackage == null || !reference!!.matches(MediaPackageReferenceImpl(mediaPackage)))
                node.setAttribute("ref", reference!!.toString())

        // Description
        if (description != null) {
            val descriptionNode = document.createElement("description")
            descriptionNode.appendChild(document.createTextNode(description))
            node.appendChild(descriptionNode)
        }

        // Tags
        if (tags!!.size > 0) {
            val tagsNode = document.createElement("tags")
            node.appendChild(tagsNode)
            for (tag in tags!!) {
                val tagNode = document.createElement("tag")
                tagsNode.appendChild(tagNode)
                tagNode.appendChild(document.createTextNode(tag))
            }
        }

        // Url
        val urlNode = document.createElement("url")
        val urlValue: String
        try {
            urlValue = serializer?.encodeURI(uri)?.toString() ?: uri!!.toString()
        } catch (e: URISyntaxException) {
            throw MediaPackageException(e)
        }

        urlNode.appendChild(document.createTextNode(urlValue))
        node.appendChild(urlNode)

        // MimeType
        if (mimeType != null) {
            val mimeNode = document.createElement("mimetype")
            mimeNode.appendChild(document.createTextNode(mimeType!!.toString()))
            node.appendChild(mimeNode)
        }

        // Size
        if (size != null && size != -1) {
            val sizeNode = document.createElement("size")
            sizeNode.appendChild(document.createTextNode(java.lang.Long.toString(size!!)))
            node.appendChild(sizeNode)
        }

        // Checksum
        if (checksum != null) {
            val checksumNode = document.createElement("checksum")
            checksumNode.setAttribute("type", checksum!!.type!!.name)
            checksumNode.appendChild(document.createTextNode(checksum!!.value))
            node.appendChild(checksumNode)
        }

        return node
    }

    /**
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        val s = if (description != null) description else uri!!.toString()
        return s.toLowerCase()
    }

    /**
     * Attention: The media package reference is not being cloned so that calling `getMediaPackage()` on the
     * clone yields null.
     */
    override fun clone(): Any {
        val out = ByteArrayOutputStream()
        var `in`: ByteArrayInputStream? = null
        try {
            val marshaller = MediaPackageImpl.context.createMarshaller()
            marshaller.marshal(this, out)
            val unmarshaller = MediaPackageImpl.context.createUnmarshaller()
            `in` = ByteArrayInputStream(out.toByteArray())
            return unmarshaller.unmarshal(`in`)
        } catch (e: JAXBException) {
            throw RuntimeException(if (e.linkedException != null) e.linkedException else e)
        } finally {
            IoSupport.closeQuietly(`in`)
        }
    }

    companion object {

        /** Serial version uid  */
        private const val serialVersionUID = 1L
    }

}
