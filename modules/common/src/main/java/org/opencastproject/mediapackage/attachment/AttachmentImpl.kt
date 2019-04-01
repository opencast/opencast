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

package org.opencastproject.mediapackage.attachment

import org.opencastproject.mediapackage.AbstractMediaPackageElement
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.util.Checksum
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.UnknownFileTypeException

import java.net.URI
import java.util.HashMap
import java.util.LinkedList

import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * Basic implementation of an attachment.
 */
@XmlType(name = "attachment", namespace = "http://mediapackage.opencastproject.org")
@XmlRootElement(name = "attachment", namespace = "http://mediapackage.opencastproject.org")
class AttachmentImpl : AbstractMediaPackageElement, Attachment {

    /** The object properties  */
    @XmlElement(name = "additionalProperties")
    @XmlJavaTypeAdapter(PropertiesXmlAdapter::class)
    protected var properties: Map<String, String>? = null

    /**
     * Needed by JAXB
     */
    constructor() : super(MediaPackageElement.Type.Attachment, null, null) {
        properties = HashMap()
    }

    /**
     * Creates an attachment.
     *
     * @param identifier
     * the attachment identifier
     * @param flavor
     * the attachment type
     * @param uri
     * the attachments location
     * @param size
     * the attachments size
     * @param checksum
     * the attachments checksum
     * @param mimeType
     * the attachments mime type
     */
    protected constructor(identifier: String?, flavor: MediaPackageElementFlavor?, uri: URI?, size: Long, checksum: Checksum?,
                          mimeType: MimeType?) : super(identifier, MediaPackageElement.Type.Attachment, flavor, uri, size, checksum, mimeType) {
        if (uri != null)
            try {
                this.mimeType = MimeTypes.fromURI(uri)
            } catch (e: UnknownFileTypeException) {
            }

    }

    /**
     * Creates an attachment.
     *
     * @param flavor
     * the attachment type
     * @param uri
     * the attachment location
     * @param size
     * the attachment size
     * @param checksum
     * the attachment checksum
     * @param mimeType
     * the attachment mime type
     */
    protected constructor(flavor: MediaPackageElementFlavor, uri: URI?, size: Long, checksum: Checksum, mimeType: MimeType) : super(MediaPackageElement.Type.Attachment, flavor, uri, size, checksum, mimeType) {
        if (uri != null)
            try {
                this.mimeType = MimeTypes.fromURI(uri)
            } catch (e: UnknownFileTypeException) {
            }

    }

    /**
     * Creates an attachment.
     *
     * @param identifier
     * the attachment identifier
     * @param uri
     * the attachments location
     */
    protected constructor(identifier: String, uri: URI) : this(identifier, null, uri, 0, null, null) {}

    /**
     * Creates an attachment.
     *
     * @param uri
     * the attachments location
     */
    protected constructor(uri: URI) : this(null, null, uri, 0, null, null) {}

    override fun getProperties(): Map<String, String> {
        if (properties == null)
            properties = HashMap()

        return properties
    }

    /**
     * JAXB properties xml adapter class.
     */
    private class PropertiesXmlAdapter : XmlAdapter<PropertiesAdapter, Map<String, String>>() {

        @Throws(Exception::class)
        override fun unmarshal(pa: PropertiesAdapter?): Map<String, String> {
            val properties = HashMap<String, String>()
            if (pa != null) {
                for (p in pa.propertiesList) {
                    properties[p.key] = p.value
                }
            }
            return properties
        }

        @Throws(Exception::class)
        override fun marshal(p: Map<String, String>?): PropertiesAdapter? {
            if (p == null || p.size == 0) return null

            val pa = PropertiesAdapter()
            for (key in p.keys) {
                pa.propertiesList.add(Property(key, p[key]))
            }
            return pa
        }
    }

    /**
     * Properties map to list of entries adapter class.
     */
    private class PropertiesAdapter @JvmOverloads internal constructor(@field:XmlElement(name = "property")
                                                                       private val propertiesList: List<Property> = LinkedList())

    /**
     * Properties entry adapter class.
     */
    private class Property {
        @XmlAttribute(name = "key")
        private val key: String
        @XmlValue
        private val value: String

        internal constructor() {
            // Default constructor
        }

        internal constructor(key: String, value: String) {
            this.key = key
            this.value = value
        }
    }

    class Adapter : XmlAdapter<AttachmentImpl, Attachment>() {
        @Throws(Exception::class)
        override fun marshal(mp: Attachment): AttachmentImpl {
            return mp as AttachmentImpl
        }

        @Throws(Exception::class)
        override fun unmarshal(mp: AttachmentImpl): Attachment {
            return mp
        }
    }

    companion object {

        /** Serial version UID  */
        private val serialVersionUID = 6626531251856698138L

        /**
         * Creates a new attachment from the url.
         *
         * @param uri
         * the attachment location
         * @return the attachment
         */
        fun fromURI(uri: URI): Attachment {
            return AttachmentImpl(uri)
        }
    }
}
