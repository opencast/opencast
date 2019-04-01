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
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.UnsupportedElementException
import org.opencastproject.mediapackage.track.AudioStreamImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
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
import javax.xml.xpath.XPathException
import javax.xml.xpath.XPathExpressionException

/**
 * This implementation of the [MediaPackageElementBuilderPlugin] recognizes video tracks and provides the
 * functionality of reading it on behalf of the media package.
 */
class TrackBuilderPlugin : AbstractElementBuilderPlugin() {

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): Boolean {
        return type == MediaPackageElement.Type.Track
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(elementNode: Node): Boolean {
        var name = elementNode.nodeName
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1)
        }
        return name.equals(MediaPackageElement.Type.Track.toString(), ignoreCase = true)
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.accept
     */
    override fun accept(uri: URI, type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): Boolean {
        return MediaPackageElement.Type.Track == type
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.elementFromURI
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromURI(uri: URI): MediaPackageElement {
        logger.trace("Creating track from $uri")
        return TrackImpl.fromURI(uri)
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.newElement
     */
    override fun newElement(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement {
        val track = TrackImpl()
        track.flavor = flavor
        return track
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.elementFromManifest
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromManifest(elementNode: Node, serializer: MediaPackageSerializer): MediaPackageElement {

        var id: String? = null
        var mimeType: MimeType? = null
        var flavor: MediaPackageElementFlavor? = null
        var transport: TrackImpl.StreamingProtocol? = null
        var reference: String? = null
        var url: URI? = null
        var size: Long = -1
        var checksum: Checksum? = null

        try {
            // id
            id = xpath.evaluate("@id", elementNode, XPathConstants.STRING) as String

            // url
            url = serializer.decodeURI(URI(xpath.evaluate("url/text()", elementNode).trim { it <= ' ' }))

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

            // transport
            val transportValue = xpath.evaluate("@transport", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(transportValue))
                transport = TrackImpl.StreamingProtocol.valueOf(transportValue)

            // checksum
            val checksumValue = xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING) as String
            val checksumType = xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
                checksum = Checksum.create(checksumType.trim { it <= ' ' }, checksumValue.trim { it <= ' ' })

            // mimetype
            val mimeTypeValue = xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(mimeTypeValue))
                mimeType = MimeTypes.parseMimeType(mimeTypeValue)

            //
            // Build the track

            val track = TrackImpl.fromURI(url)

            if (StringUtils.isNotBlank(id))
                track.identifier = id

            // Add url
            track.uri = url

            // Add reference
            if (StringUtils.isNotEmpty(reference))
                track.referTo(MediaPackageReferenceImpl.fromString(reference))

            // Set size
            if (size > 0)
                track.size = size

            // Set checksum
            if (checksum != null)
                track.checksum = checksum

            // Set mimetpye
            if (mimeType != null)
                track.mimeType = mimeType

            if (flavor != null)
                track.flavor = flavor

            //set transport
            if (transport != null)
                track.transport = transport

            // description
            val description = xpath.evaluate("description/text()", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotBlank(description))
                track.elementDescription = description.trim { it <= ' ' }

            // tags
            val tagNodes = xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET) as NodeList
            for (i in 0 until tagNodes.length) {
                track.addTag(tagNodes.item(i).textContent)
            }

            // duration
            try {
                val strDuration = xpath.evaluate("duration/text()", elementNode, XPathConstants.STRING) as String
                if (StringUtils.isNotEmpty(strDuration)) {
                    val duration = java.lang.Long.parseLong(strDuration.trim { it <= ' ' })
                    track.duration = duration
                }
            } catch (e: NumberFormatException) {
                throw UnsupportedElementException("Duration of track $url is malformatted")
            }

            // is live
            val strLive = xpath.evaluate("live/text()", elementNode, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(strLive)) {
                val live = java.lang.Boolean.parseBoolean(strLive.trim { it <= ' ' })
                track.isLive = live
            }

            // audio settings
            val audioSettingsNode = xpath.evaluate("audio", elementNode, XPathConstants.NODE) as Node
            if (audioSettingsNode != null && audioSettingsNode.hasChildNodes()) {
                try {
                    val `as` = AudioStreamImpl.fromManifest(createStreamID(track), audioSettingsNode, xpath)
                    track.addStream(`as`)
                } catch (e: IllegalStateException) {
                    throw UnsupportedElementException("Illegal state encountered while reading audio settings from " + url
                            + ": " + e.message)
                } catch (e: XPathException) {
                    throw UnsupportedElementException("Error while parsing audio settings from " + url + ": "
                            + e.message)
                }

            }

            // video settings
            val videoSettingsNode = xpath.evaluate("video", elementNode, XPathConstants.NODE) as Node
            if (videoSettingsNode != null && videoSettingsNode.hasChildNodes()) {
                try {
                    val vs = VideoStreamImpl.fromManifest(createStreamID(track), videoSettingsNode, xpath)
                    track.addStream(vs)
                } catch (e: IllegalStateException) {
                    throw UnsupportedElementException("Illegal state encountered while reading video settings from " + url
                            + ": " + e.message)
                } catch (e: XPathException) {
                    throw UnsupportedElementException("Error while parsing video settings from " + url + ": "
                            + e.message)
                }

            }

            return track
        } catch (e: XPathExpressionException) {
            throw UnsupportedElementException("Error while reading track information from manifest: " + e.message)
        } catch (e: NoSuchAlgorithmException) {
            throw UnsupportedElementException("Unsupported digest algorithm: " + e.message)
        } catch (e: URISyntaxException) {
            throw UnsupportedElementException("Error while reading presenter track " + url + ": " + e.message)
        }

    }

    private fun createStreamID(track: Track): String {
        return "stream-" + (track.streams.size + 1)
    }

    override fun toString(): String {
        return "Track Builder Plugin"
    }

    companion object {

        /**
         * the logging facility provided by log4j
         */
        private val logger = LoggerFactory.getLogger(TrackBuilderPlugin::class.java!!)
    }

}
