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

import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.util.DateTimeSupport

import org.apache.commons.lang3.StringUtils
import org.codehaus.jettison.mapped.Configuration
import org.codehaus.jettison.mapped.MappedNamespaceConvention
import org.codehaus.jettison.mapped.MappedXMLStreamWriter
import org.w3c.dom.Document
import org.w3c.dom.Element

import java.io.OutputStream
import java.io.StringWriter
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedList

import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter

/**
 * Convenience implementation that supports serializing and deserializing media packages.
 */
object MediaPackageParser {

    /**
     * Serializes the media package to a string.
     *
     * @param mediaPackage
     * the media package
     * @return the serialized media package
     */
    fun getAsXml(mediaPackage: MediaPackage?): String {
        if (mediaPackage == null)
            throw IllegalArgumentException("Mediapackage must not be null")
        try {
            val marshaller = MediaPackageImpl.context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            val writer = StringWriter()
            marshaller.marshal(mediaPackage, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IllegalStateException(if (e.linkedException != null) e.linkedException else e)
        }

    }

    /**
     * Serializes the media package to a JSON string.
     *
     * @param mediaPackage
     * the media package
     * @return the serialized media package
     */
    fun getAsJSON(mediaPackage: MediaPackage?): String {
        if (mediaPackage == null) {
            throw IllegalArgumentException("Mediapackage must not be null")
        }
        try {
            val marshaller = MediaPackageImpl.context.createMarshaller()

            val config = Configuration()
            config.isSupressAtAttributes = true
            val con = MappedNamespaceConvention(config)
            val writer = StringWriter()
            val xmlStreamWriter = object : MappedXMLStreamWriter(con, writer) {
                @Throws(XMLStreamException::class)
                override fun writeStartElement(prefix: String, local: String, uri: String) {
                    super.writeStartElement("", local, "")
                }

                @Throws(XMLStreamException::class)
                override fun writeStartElement(uri: String, local: String) {
                    super.writeStartElement("", local, "")
                }

                @Throws(XMLStreamException::class)
                override fun setPrefix(pfx: String?, uri: String?) {
                }

                @Throws(XMLStreamException::class)
                override fun setDefaultNamespace(uri: String?) {
                }
            }

            marshaller.marshal(mediaPackage, xmlStreamWriter)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IllegalStateException(if (e.linkedException != null) e.linkedException else e)
        }

    }

    /** Serializes a media package to a [Document] without any further processing.  */
    fun getAsXmlDocument(mp: MediaPackage): Document {
        try {
            val marshaller = MediaPackageImpl.context.createMarshaller()
            val doc = newDocument()
            marshaller.marshal(mp, doc)
            return doc
        } catch (e: JAXBException) {
            return chuck(e)
        }

    }

    /** Create a new DOM document.  */
    private fun newDocument(): Document {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.isNamespaceAware = true
        try {
            return docBuilderFactory.newDocumentBuilder().newDocument()
        } catch (e: ParserConfigurationException) {
            return chuck(e)
        }

    }

    /**
     * Serializes the media package to a [org.w3c.dom.Document].
     *
     *
     * todo Implementation is currently defective since it misses various properties. See
     * http://opencast.jira.com/browse/MH-9489 Use [.getAsXmlDocument] instead if you do not need a
     * serializer.
     *
     * @param mediaPackage
     * the mediapackage
     * @param serializer
     * the serializer
     * @return the serialized media package
     * @throws MediaPackageException
     * if serializing fails
     */
    @Throws(MediaPackageException::class)
    fun getAsXml(mediaPackage: MediaPackage, serializer: MediaPackageSerializer): Document {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.isNamespaceAware = true

        var docBuilder: DocumentBuilder? = null
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder()
        } catch (e1: ParserConfigurationException) {
            throw MediaPackageException(e1)
        }

        val doc = docBuilder!!.newDocument()

        // Root element "mediapackage"
        val mpXml = doc.createElement("mediapackage")
        doc.appendChild(mpXml)

        // Handle
        if (mediaPackage.identifier != null)
            mpXml.setAttribute("id", mediaPackage.identifier.toString())

        // Start time
        if (mediaPackage.date != null && mediaPackage.date.time > 0)
            mpXml.setAttribute("start", DateTimeSupport.toUTC(mediaPackage.date.time))

        // Duration
        if (mediaPackage.duration != null)
            mpXml.setAttribute("duration", java.lang.Long.toString(mediaPackage.duration!!))

        // Separate the media package members
        val tracks = ArrayList<Track>()
        val attachments = ArrayList<Attachment>()
        val metadata = ArrayList<Catalog>()
        val others = ArrayList<MediaPackageElement>()

        // Sort media package elements
        for (e in mediaPackage.elements()) {
            if (e is Track)
                tracks.add(e)
            else if (e is Attachment)
                attachments.add(e)
            else if (e is Catalog)
                metadata.add(e)
            else
                others.add(e)
        }

        // Tracks
        if (tracks.size > 0) {
            val tracksNode = doc.createElement("media")
            Collections.sort(tracks)
            for (t in tracks) {
                tracksNode.appendChild(t.toManifest(doc, serializer))
            }
            mpXml.appendChild(tracksNode)
        }

        // Metadata
        if (metadata.size > 0) {
            val metadataNode = doc.createElement("metadata")
            Collections.sort(metadata)
            for (m in metadata) {
                metadataNode.appendChild(m.toManifest(doc, serializer))
            }
            mpXml.appendChild(metadataNode)
        }

        // Attachments
        if (attachments.size > 0) {
            val attachmentsNode = doc.createElement("attachments")
            Collections.sort(attachments)
            for (a in attachments) {
                attachmentsNode.appendChild(a.toManifest(doc, serializer))
            }
            mpXml.appendChild(attachmentsNode)
        }

        // Unclassified
        if (others.size > 0) {
            val othersNode = doc.createElement("unclassified")
            Collections.sort(others)
            for (e in others) {
                othersNode.appendChild(e.toManifest(doc, serializer))
            }
            mpXml.appendChild(othersNode)
        }

        return mpXml.ownerDocument
    }

    /**
     * Parses the media package and returns its object representation.
     *
     * @param xml
     * the serialized media package
     * @return the media package instance
     * @throws MediaPackageException
     * if de-serializing the media package fails
     */
    @Throws(MediaPackageException::class)
    fun getFromXml(xml: String): MediaPackage {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        return builder!!.loadFromXml(xml)
    }

    /**
     * Writes an xml representation of this MediaPackage to a stream.
     *
     * @param mediaPackage
     * the mediaPackage
     * @param out
     * The output stream
     * @param format
     * Whether to format the output for readability, or not (false gives better performance)
     * @throws MediaPackageException
     * if serializing or reading from a serialized media package fails
     */
    @Throws(MediaPackageException::class)
    fun getAsXml(mediaPackage: MediaPackage, out: OutputStream, format: Boolean) {
        try {
            val marshaller = MediaPackageImpl.context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format)
            marshaller.marshal(mediaPackage, out)
        } catch (e: JAXBException) {
            throw MediaPackageException(if (e.linkedException != null) e.linkedException else e)
        }

    }

    /**
     * Serializes media package list to a string.
     *
     * @param mediaPackages
     * media package list to be serialized
     * @return serialized media package list
     * @throws MediaPackageException
     * if serialization fails
     */
    @Throws(MediaPackageException::class)
    fun getArrayAsXml(mediaPackages: List<MediaPackage>): String {
        try {
            val builder = StringBuilder()
            if (mediaPackages.isEmpty())
                return builder.toString()
            builder.append(getAsXml(mediaPackages[0]))
            for (i in 1 until mediaPackages.size) {
                builder.append("###")
                builder.append(getAsXml(mediaPackages[i]))
            }
            return builder.toString()
        } catch (e: Exception) {
            if (e is MediaPackageException) {
                throw e
            } else {
                throw MediaPackageException(e)
            }
        }

    }

    /**
     * Parses the serialized media package list.
     *
     * @param xml
     * String to be parsed
     * @return parsed media package list
     * @throws MediaPackageException
     * if de-serialization fails
     */
    @Throws(MediaPackageException::class)
    fun getArrayFromXml(xml: String): List<MediaPackage> {
        try {
            val mediaPackages = LinkedList<MediaPackage>()
            if (StringUtils.isBlank(xml))
                return mediaPackages
            val xmlArray = xml.split("###".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            for (xmlElement in xmlArray) {
                mediaPackages.add(getFromXml(xmlElement.trim({ it <= ' ' })))
            }
            return mediaPackages
        } catch (e: Exception) {
            if (e is MediaPackageException) {
                throw e
            } else {
                throw MediaPackageException(e)
            }
        }

    }

}
/**
 * Private constructor to prohibit instances of this static utility class.
 */// Nothing to do
