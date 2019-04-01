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

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.CatalogImpl
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.MediaPackageSerializer
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

import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * This implementation of the [MediaPackageElementBuilderPlugin] recognizes metadata catalogs and provides the
 * functionality of reading it on behalf of the media package.
 */
class CatalogBuilderPlugin : MediaPackageElementBuilderPlugin {

    /** The xpath facility  */
    protected var xpath = XPathFactory.newInstance().newXPath()

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): Boolean {
        return type == MediaPackageElement.Type.Catalog
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(elementNode: Node): Boolean {
        var name = elementNode.nodeName
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1)
        }
        return name.equals(MediaPackageElement.Type.Catalog.toString(), ignoreCase = true)
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(uri: URI, type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): Boolean {
        return MediaPackageElement.Type.Catalog == type
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.elementFromURI
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromURI(uri: URI): MediaPackageElement {
        logger.trace("Creating video track from $uri")
        return CatalogImpl.fromURI(uri)
    }

    override fun toString(): String {
        return "Indefinite Catalog Builder Plugin"
    }

    protected fun catalogFromManifest(id: String, uri: URI): Catalog {
        val cat = CatalogImpl.fromURI(uri)
        cat.identifier = id
        return cat
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.destroy
     */
    override fun destroy() {}

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.elementFromManifest
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromManifest(elementNode: Node, serializer: MediaPackageSerializer): MediaPackageElement {
        var id: String? = null
        var flavor: String? = null
        var url: URI? = null
        var size: Long = -1
        var checksum: Checksum? = null
        var mimeType: MimeType? = null
        var reference: String? = null

        try {
            // id
            id = xpath.evaluate("@id", elementNode, XPathConstants.STRING) as String

            // url
            url = serializer.decodeURI(URI(xpath.evaluate("url/text()", elementNode).trim { it <= ' ' }))

            // flavor
            flavor = xpath.evaluate("@type", elementNode, XPathConstants.STRING) as String

            // reference
            reference = xpath.evaluate("@ref", elementNode, XPathConstants.STRING) as String

            // size
            val documentSize = xpath.evaluate("size/text()", elementNode).trim { it <= ' ' }
            if ("" != documentSize)
                size = java.lang.Long.parseLong(documentSize)

            // checksum
            val checksumValue = xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING) as String
            val checksumType = xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
                checksum = Checksum.create(checksumType.trim { it <= ' ' }, checksumValue.trim { it <= ' ' })

            // mimetype
            val mimeTypeValue = xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(mimeTypeValue))
                mimeType = MimeTypes.parseMimeType(mimeTypeValue)

            // create the catalog
            val dc = CatalogImpl.fromURI(url)
            if (StringUtils.isNotEmpty(id))
                dc.identifier = id

            // Add url
            dc.uri = url

            // Add flavor
            if (flavor != null)
                dc.flavor = MediaPackageElementFlavor.parseFlavor(flavor)

            // Add reference
            if (StringUtils.isNotEmpty(reference))
                dc.referTo(MediaPackageReferenceImpl.fromString(reference))

            // Set size
            if (size > 0)
                dc.size = size

            // Set checksum
            if (checksum != null)
                dc.checksum = checksum

            // Set Mimetype
            if (mimeType != null)
                dc.mimeType = mimeType

            // Tags
            val tagNodes = xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET) as NodeList
            for (i in 0 until tagNodes.length) {
                dc.addTag(tagNodes.item(i).textContent)
            }

            return dc
        } catch (e: XPathExpressionException) {
            throw UnsupportedElementException("Error while reading catalog information from manifest: " + e.message)
        } catch (e: NoSuchAlgorithmException) {
            throw UnsupportedElementException("Unsupported digest algorithm: " + e.message)
        } catch (e: URISyntaxException) {
            throw UnsupportedElementException("Error while reading dublin core catalog " + url + ": " + e.message)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.newElement
     */
    override fun newElement(type: Type, flavor: MediaPackageElementFlavor): MediaPackageElement {
        val cat = CatalogImpl.newInstance()
        cat.flavor = flavor
        return cat
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.init
     */
    @Throws(Exception::class)
    override fun init() {
    }

    companion object {

        /**
         * the logging facility provided by log4j
         */
        private val logger = LoggerFactory.getLogger(CatalogBuilderPlugin::class.java!!)
    }

}
