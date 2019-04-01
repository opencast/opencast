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


package org.opencastproject.mediapackage.elementbuilder

import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.UnsupportedElementException
import org.opencastproject.util.Checksum
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import java.net.URI
import java.net.URISyntaxException
import java.security.NoSuchAlgorithmException

import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException

class PublicationBuilderPlugin : AbstractElementBuilderPlugin() {

    override fun accept(type: Type, flavor: MediaPackageElementFlavor): Boolean {
        return type == MediaPackageElement.Type.Publication
    }

    override fun accept(uri: URI, type: Type, flavor: MediaPackageElementFlavor): Boolean {
        return MediaPackageElement.Type.Publication == type
    }

    override fun accept(elementNode: Node): Boolean {
        var name = elementNode.nodeName
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1)
        }
        return name.equals(MediaPackageElement.Type.Publication.toString(), ignoreCase = true)
    }

    @Throws(UnsupportedElementException::class)
    override fun elementFromURI(uri: URI): MediaPackageElement {
        // TODO Auto-generated method stub
        logger.trace("Creating publication element from $uri")
        val publication = PublicationImpl()
        publication.uri = uri
        return publication
    }

    @Throws(UnsupportedElementException::class)
    override fun elementFromManifest(elementNode: Node,
                                     serializer: MediaPackageSerializer): MediaPackageElement {

        var id: String? = null
        var mimeType: MimeType? = null
        var flavor: MediaPackageElementFlavor? = null
        var reference: String? = null
        var channel: String? = null
        var url: URI? = null
        var size: Long = -1
        var checksum: Checksum? = null

        try {
            // id
            id = xpath.evaluate("@id", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isEmpty(id)) {
                throw UnsupportedElementException("Unvalid or missing id argument!")
            }

            // url
            url = serializer.decodeURI(URI(xpath.evaluate("url/text()", elementNode).trim { it <= ' ' }))

            // channel
            channel = xpath.evaluate("@channel", elementNode).trim { it <= ' ' }
            if (StringUtils.isEmpty(channel)) {
                throw UnsupportedElementException("Unvalid or missing channel argument!")
            }

            // reference
            reference = xpath.evaluate("@ref", elementNode, XPathConstants.STRING) as String

            // size
            val trackSize = xpath.evaluate("size/text()", elementNode).trim { it <= ' ' }
            if ("" != trackSize)
                size = java.lang.Long.parseLong(trackSize)

            // flavor
            val flavorValue = xpath.evaluate("@type", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(flavorValue))
                flavor = MediaPackageElementFlavor.parseFlavor(flavorValue)

            // checksum
            val checksumValue = xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING) as String
            val checksumType = xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
                checksum = Checksum.create(checksumType.trim { it <= ' ' }, checksumValue.trim { it <= ' ' })

            // mimetype
            val mimeTypeValue = xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(mimeTypeValue)) {
                mimeType = MimeTypes.parseMimeType(mimeTypeValue)
            } else {
                throw UnsupportedElementException("Unvalid or missing mimetype argument!")
            }

            // Build the publication element
            val publication = PublicationImpl(id, channel, url, mimeType)

            if (StringUtils.isNotBlank(id))
                publication.identifier = id

            // Add url
            publication.uri = url

            // Add reference
            if (StringUtils.isNotEmpty(reference))
                publication.referTo(MediaPackageReferenceImpl.fromString(reference))

            // Set size
            if (size > 0)
                publication.size = size

            // Set checksum
            if (checksum != null)
                publication.checksum = checksum

            // Set mimetpye
            if (mimeType != null)
                publication.mimeType = mimeType

            if (flavor != null)
                publication.flavor = flavor

            // description
            val description = xpath.evaluate("description/text()", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotBlank(description))
                publication.elementDescription = description.trim { it <= ' ' }

            // tags
            val tagNodes = xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET) as NodeList
            for (i in 0 until tagNodes.length) {
                publication.addTag(tagNodes.item(i).textContent)
            }

            return publication
        } catch (e: XPathExpressionException) {
            throw UnsupportedElementException("Error while reading track information from manifest: " + e.message)
        } catch (e: NoSuchAlgorithmException) {
            throw UnsupportedElementException("Unsupported digest algorithm: " + e.message)
        } catch (e: URISyntaxException) {
            throw UnsupportedElementException("Error while reading presenter track " + url + ": " + e.message)
        }

    }

    override fun newElement(type: Type,
                            flavor: MediaPackageElementFlavor): MediaPackageElement {
        val element = PublicationImpl()
        element.flavor = flavor
        return element
    }

    companion object {

        /**
         * the logging facility provided by log4j
         */
        private val logger = LoggerFactory.getLogger(PublicationBuilderPlugin::class.java!!)
    }

}
