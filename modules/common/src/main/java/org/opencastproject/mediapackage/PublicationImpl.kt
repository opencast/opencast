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

import org.opencastproject.util.MimeType

import java.net.URI
import java.util.ArrayList
import java.util.UUID

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.adapters.XmlAdapter

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "publication", namespace = "http://mediapackage.opencastproject.org")
@XmlRootElement(name = "publication", namespace = "http://mediapackage.opencastproject.org")
class PublicationImpl
/** JAXB constructor  */
() : AbstractMediaPackageElement(), Publication {

    @XmlAttribute(name = "channel", required = true)
    override val channel: String

    @XmlElementWrapper(name = "media")
    @XmlElement(name = "track")
    private val tracks = ArrayList<Track>()

    @XmlElementWrapper(name = "attachments")
    @XmlElement(name = "attachment")
    private val attachments = ArrayList<Attachment>()

    @XmlElementWrapper(name = "metadata")
    @XmlElement(name = "catalog")
    private val catalogs = ArrayList<Catalog>()

    override var flavor: MediaPackageElementFlavor
        get
        set(flavor) = throw UnsupportedOperationException("Unable to set the flavor of publications.")

    init {
        this.elementType = MediaPackageElement.Type.Publication
    }

    constructor(id: String, channel: String, uri: URI, mimeType: MimeType) : this() {
        uri = uri
        identifier = id
        mimeType = mimeType
        this.channel = channel
    }

    override fun getTracks(): Array<Track> {
        return tracks.toTypedArray<Track>()
    }

    override fun addTrack(track: Track) {
        // Check (uniqueness of) track identifier
        val id = track.identifier
        if (id == null) {
            track.identifier = createElementIdentifier()
        }
        tracks.add(track)
    }

    override fun getAttachments(): Array<Attachment> {
        return attachments.toTypedArray<Attachment>()
    }

    override fun addAttachment(attachment: Attachment) {
        // Check (uniqueness of) attachment identifier
        val id = attachment.identifier
        if (id == null) {
            attachment.identifier = createElementIdentifier()
        }
        attachments.add(attachment)
    }

    override fun removeAttachmentById(attachmentId: String) {
        attachments.removeIf { a -> a.identifier == attachmentId }
    }

    override fun getCatalogs(): Array<Catalog> {
        return catalogs.toTypedArray<Catalog>()
    }

    override fun addCatalog(catalog: Catalog) {
        // Check (uniqueness of) catalog identifier
        val id = catalog.identifier
        if (id == null) {
            catalog.identifier = createElementIdentifier()
        }
        catalogs.add(catalog)
    }

    /**
     * Returns a media package element identifier. The identifier will be unique within the media package.
     *
     * @return the element identifier
     */
    private fun createElementIdentifier(): String {
        return UUID.randomUUID().toString()
    }

    /** JAXB adapter  */
    class Adapter : XmlAdapter<PublicationImpl, Publication>() {
        @Throws(Exception::class)
        override fun marshal(e: Publication): PublicationImpl {
            return e as PublicationImpl
        }

        @Throws(Exception::class)
        override fun unmarshal(e: PublicationImpl): Publication {
            return e
        }
    }

    companion object {
        /** Serial version UID  */
        private val serialVersionUID = 11151970L

        fun publication(id: String, channel: String, uri: URI, mimeType: MimeType): Publication {
            return PublicationImpl(id, channel, uri, mimeType)
        }

        /**
         * Adds a [MediaPackageElement] to this publication by determining its type.
         *
         * @param publication
         * The [Publication] to add the [MediaPackageElement] to.
         * @param element
         * The [MediaPackageElement] to add. If it is not a [Attachment], [Catalog] or
         * [Track] it will not be added to the [Publication].
         */
        fun addElementToPublication(publication: Publication, element: MediaPackageElement) {
            if (MediaPackageElement.Type.Track == element.elementType) {
                publication.addTrack(element as Track)
            } else if (MediaPackageElement.Type.Catalog == element.elementType) {
                publication.addCatalog(element as Catalog)
            } else if (MediaPackageElement.Type.Attachment == element.elementType) {
                publication.addAttachment(element as Attachment)
            }
        }
    }
}
