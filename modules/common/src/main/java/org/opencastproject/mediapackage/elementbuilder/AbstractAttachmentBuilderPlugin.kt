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

import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.mediapackage.UnsupportedElementException
import org.opencastproject.mediapackage.attachment.AttachmentImpl
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

/**
 * This implementation of the [MediaPackageElementBuilderPlugin] recognizes attachments and provides utility
 * methods for creating media package element representations for them.
 */
abstract class AbstractAttachmentBuilderPlugin
/**
 * Creates a new attachment plugin builder that will accept attachments with the given flavor.
 *
 * @param flavor
 * the attachment flavor
 */
@JvmOverloads constructor(flavor: MediaPackageElementFlavor? = null) : AbstractElementBuilderPlugin() {

    /** The candidate type  */
    protected var type: MediaPackageElement.Type = MediaPackageElement.Type.Attachment

    /** The flavor to look for  */
    protected var flavor: MediaPackageElementFlavor? = null

    init {
        this.flavor = flavor
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(uri: URI, type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): Boolean {
        return accept(type, flavor)
    }

    /**
     * This implementation of `accept` tests for the element type (attachment).
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(type: MediaPackageElement.Type?, flavor: MediaPackageElementFlavor): Boolean {
        return if (this.flavor != null && this.flavor != flavor) false else type == null || MediaPackageElement.Type.Attachment.toString().equals(type.toString(), ignoreCase = true)
    }

    /**
     * This implementation of `accept` tests for the correct node type (attachment).
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(elementNode: Node): Boolean {
        try {
            // Test for attachment
            var nodeName = elementNode.nodeName
            if (nodeName.contains(":")) {
                nodeName = nodeName.substring(nodeName.indexOf(":") + 1)
            }
            if (!MediaPackageElement.Type.Attachment.toString().equals(nodeName, ignoreCase = true))
                return false
            // Check flavor
            if (this.flavor != null) {
                val nodeFlavor = xpath.evaluate("@type", elementNode, XPathConstants.STRING) as String
                if (!flavor!!.eq(nodeFlavor))
                    return false
            }
            // Check mime type
            if (mimeTypes != null && mimeTypes!!.size > 0) {
                val nodeMimeType = xpath.evaluate("mimetype", elementNode, XPathConstants.STRING) as String
                val mimeType = MimeTypes.parseMimeType(nodeMimeType)
                if (!mimeTypes!!.contains(mimeType))
                    return false
            }

            return true
        } catch (e: XPathExpressionException) {
            logger.warn("Error while reading attachment flavor from manifest: " + e.message)
            return false
        }

    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElementBuilder.newElement
     */
    override fun newElement(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement {
        val attachment = AttachmentImpl()
        attachment.flavor = flavor
        return attachment
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.elementFromManifest
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromManifest(elementNode: Node, serializer: MediaPackageSerializer): MediaPackageElement {

        var id: String? = null
        var attachmentFlavor: String? = null
        var reference: String? = null
        var uri: URI? = null
        var size: Long = -1
        var checksum: Checksum? = null
        var mimeType: MimeType? = null

        try {
            // id
            id = xpath.evaluate("@id", elementNode, XPathConstants.STRING) as String

            // flavor
            attachmentFlavor = xpath.evaluate("@type", elementNode, XPathConstants.STRING) as String

            // reference
            reference = xpath.evaluate("@ref", elementNode, XPathConstants.STRING) as String

            // url
            uri = serializer.decodeURI(URI(xpath.evaluate("url/text()", elementNode).trim { it <= ' ' }))

            // size
            val attachmentSize = xpath.evaluate("size/text()", elementNode).trim { it <= ' ' }
            if ("" != attachmentSize)
                size = java.lang.Long.parseLong(attachmentSize)

            // checksum
            val checksumValue = xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING) as String
            val checksumType = xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
                checksum = Checksum.create(checksumType.trim { it <= ' ' }, checksumValue.trim { it <= ' ' })

            // mimetype
            val mimeTypeValue = xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(mimeTypeValue))
                mimeType = MimeTypes.parseMimeType(mimeTypeValue)

            // create the attachment
            val attachment = AttachmentImpl.fromURI(uri) as AttachmentImpl

            if (StringUtils.isNotEmpty(id))
                attachment.identifier = id

            // Add url
            attachment.uri = uri

            // Add reference
            if (StringUtils.isNotEmpty(reference))
                attachment.referTo(MediaPackageReferenceImpl.fromString(reference))

            // Add type/flavor information
            if (StringUtils.isNotEmpty(attachmentFlavor)) {
                try {
                    val flavor = MediaPackageElementFlavor.parseFlavor(attachmentFlavor)
                    attachment.flavor = flavor
                } catch (e: IllegalArgumentException) {
                    logger.warn("Unable to read attachment flavor: " + e.message)
                }

            }

            // Set the size
            if (size > 0)
                attachment.size = size

            // Set checksum
            if (checksum != null)
                attachment.checksum = checksum

            // Set mimetype
            if (mimeType != null)
                attachment.mimeType = mimeType

            // Set the description
            val description = xpath.evaluate("description/text()", elementNode)
            if (StringUtils.isNotEmpty(description))
                attachment.elementDescription = description.trim { it <= ' ' }

            // Set tags
            val tagNodes = xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET) as NodeList
            for (i in 0 until tagNodes.length) {
                attachment.addTag(tagNodes.item(i).textContent)
            }

            return specializeAttachment(attachment)
        } catch (e: XPathExpressionException) {
            throw UnsupportedElementException("Error while reading attachment from manifest: " + e.message)
        } catch (e: NoSuchAlgorithmException) {
            throw UnsupportedElementException("Unsupported digest algorithm: " + e.message)
        } catch (e: URISyntaxException) {
            throw UnsupportedElementException("Error while reading attachment file " + uri + ": " + e.message)
        }

    }

    /**
     * Utility method that returns an attachment object from the given url.
     *
     * @param uri
     * the element location
     * @return an attachment object
     * @throws UnsupportedElementException
     * if the attachment cannto be read
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromURI(uri: URI): MediaPackageElement {
        logger.trace("Creating attachment from $uri")
        return specializeAttachment(AttachmentImpl.fromURI(uri))
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElementBuilder.elementFromURI
     */
    @Throws(UnsupportedElementException::class)
    fun elementFromURI(uri: URI, type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement {
        return elementFromURI(uri)
    }

    /**
     * Overwrite this method in order to return a specialization of the attachment. This implementation just returns the
     * attachment that is was given.
     *
     * @param attachment
     * the general attachment representation
     * @return a specialized attachment
     * @throws UnsupportedElementException
     * if the attachment fails to be specialized
     */
    @Throws(UnsupportedElementException::class)
    protected fun specializeAttachment(attachment: Attachment): Attachment {
        return attachment
    }

    /**
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return "Attachment Builder Plugin"
    }

    companion object {

        /** the logging facility provided by log4j  */
        private val logger = LoggerFactory.getLogger(AbstractAttachmentBuilderPlugin::class.java!!)
    }

}
/**
 * Creates a new attachment plugin builder that will accept attachments with any flavor.
 */
