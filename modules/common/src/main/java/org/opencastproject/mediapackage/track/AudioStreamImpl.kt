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


package org.opencastproject.mediapackage.track

import org.opencastproject.mediapackage.AudioStream
import org.opencastproject.mediapackage.MediaPackageSerializer

import org.apache.commons.lang3.StringUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

import java.util.UUID

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlType
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathException

/**
 * Implementation of [org.opencastproject.mediapackage.AudioStream].
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "audio", namespace = "http://mediapackage.opencastproject.org")
class AudioStreamImpl @JvmOverloads constructor(identifier: String = UUID.randomUUID().toString()) : AbstractStreamImpl(identifier), AudioStream {

    // Setter

    @XmlElement(name = "bitdepth")
    override var bitDepth: Int? = null

    @XmlElement(name = "channels")
    override var channels: Int? = null

    @XmlElement(name = "samplingrate")
    override var samplingRate: Int? = null

    @XmlElement(name = "bitrate")
    override var bitRate: Float? = null

    @XmlElement(name = "peakleveldb")
    override var pkLevDb: Float? = null

    @XmlElement(name = "rmsleveldb")
    override var rmsLevDb: Float? = null

    @XmlElement(name = "rmspeakdb")
    override var rmsPkDb: Float? = null

    override var captureDevice: String
        get() = super.captureDevice
        set(captureDevice) {
            this.device.type = captureDevice
        }

    override var captureDeviceVersion: String
        get() = super.captureDeviceVersion
        set(captureDeviceVersion) {
            this.device.version = captureDeviceVersion
        }

    override var captureDeviceVendor: String
        get() = super.captureDeviceVendor
        set(captureDeviceVendor) {
            this.device.vendor = captureDeviceVendor
        }

    override var format: String
        get() = super.format
        set(format) {
            this.encoder.type = format
        }

    override var formatVersion: String
        get() = super.formatVersion
        set(formatVersion) {
            this.encoder.version = formatVersion
        }

    override var encoderLibraryVendor: String
        get() = super.encoderLibraryVendor
        set(encoderLibraryVendor) {
            this.encoder.vendor = encoderLibraryVendor
        }

    /**
     * @see org.opencastproject.mediapackage.ManifestContributor.toManifest
     */
    override fun toManifest(document: Document, serializer: MediaPackageSerializer): Node {
        val node = document.createElement("audio")
        // Stream ID
        node.setAttribute("id", identifier)

        // Frame count
        if (frameCount != null) {
            val frameCountNode = document.createElement("framecount")
            frameCountNode.appendChild(document.createTextNode(java.lang.Long.toString(frameCount!!)))
            node.appendChild(frameCountNode)
        }

        // Device
        val deviceNode = document.createElement("device")
        var hasAttr = false
        if (device.type != null) {
            deviceNode.setAttribute("type", device.type)
            hasAttr = true
        }
        if (device.version != null) {
            deviceNode.setAttribute("version", device.version)
            hasAttr = true
        }
        if (device.vendor != null) {
            deviceNode.setAttribute("vendor", device.vendor)
            hasAttr = true
        }
        if (hasAttr)
            node.appendChild(deviceNode)

        // Encoder
        val encoderNode = document.createElement("encoder")
        hasAttr = false
        if (encoder.type != null) {
            encoderNode.setAttribute("type", encoder.type)
            hasAttr = true
        }
        if (encoder.version != null) {
            encoderNode.setAttribute("version", encoder.version)
            hasAttr = true
        }
        if (encoder.vendor != null) {
            encoderNode.setAttribute("vendor", encoder.vendor)
            hasAttr = true
        }
        if (hasAttr)
            node.appendChild(encoderNode)

        // Channels
        if (channels != null) {
            val channelsNode = document.createElement("channels")
            channelsNode.appendChild(document.createTextNode(channels!!.toString()))
            node.appendChild(channelsNode)
        }

        // Bit depth
        if (bitDepth != null) {
            val bitdepthNode = document.createElement("bitdepth")
            bitdepthNode.appendChild(document.createTextNode(bitDepth!!.toString()))
            node.appendChild(bitdepthNode)
        }

        // Bit rate
        if (bitRate != null) {
            val bitratenode = document.createElement("bitrate")
            bitratenode.appendChild(document.createTextNode(bitRate!!.toString()))
            node.appendChild(bitratenode)
        }

        // Sampling rate
        if (samplingRate != null) {
            val samplingrateNode = document.createElement("samplingrate")
            samplingrateNode.appendChild(document.createTextNode(samplingRate!!.toString()))
            node.appendChild(samplingrateNode)
        }

        // Pk lev dB
        if (pkLevDb != null) {
            val peakleveldbNode = document.createElement("peakleveldb")
            peakleveldbNode.appendChild(document.createTextNode(pkLevDb!!.toString()))
            node.appendChild(peakleveldbNode)
        }
        // RMS lev dB
        if (rmsLevDb != null) {
            val rmsleveldbNode = document.createElement("rmsleveldb")
            rmsleveldbNode.appendChild(document.createTextNode(rmsLevDb!!.toString()))
            node.appendChild(rmsleveldbNode)
        }
        // RMS Pk dB
        if (rmsPkDb != null) {
            val rmspeakdbNode = document.createElement("rmspeakdb")
            rmspeakdbNode.appendChild(document.createTextNode(rmsPkDb!!.toString()))
            node.appendChild(rmspeakdbNode)
        }
        return node
    }

    companion object {

        /**
         * Create an audio stream from the XML manifest.
         *
         * @param streamIdHint
         * stream ID that has to be used if the manifest does not provide one. This is the case when reading an old
         * manifest.
         */
        @Throws(IllegalStateException::class, XPathException::class)
        fun fromManifest(streamIdHint: String, node: Node, xpath: XPath): AudioStreamImpl {
            // Create stream
            var sid = xpath.evaluate("@id", node, XPathConstants.STRING) as String
            if (StringUtils.isEmpty(sid))
                sid = streamIdHint
            val `as` = AudioStreamImpl(sid)

            // Frame count
            try {
                val frameCount = xpath.evaluate("framecount/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(frameCount))
                    `as`.frameCount = Long(frameCount.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Frame count was malformatted: " + e.message)
            }

            // bit depth
            try {
                val bd = xpath.evaluate("bitdepth/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(bd))
                    `as`.bitDepth = Int(bd.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Bit depth was malformatted: " + e.message)
            }

            // channels
            try {
                val strChannels = xpath.evaluate("channels/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(strChannels))
                    `as`.channels = Int(strChannels.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Number of channels was malformatted: " + e.message)
            }

            // sampling rate
            try {
                val sr = xpath.evaluate("framerate/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(sr))
                    `as`.samplingRate = Int(sr.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Frame rate was malformatted: " + e.message)
            }

            // Bit rate
            try {
                val br = xpath.evaluate("bitrate/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(br))
                    `as`.bitRate = Float(br.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Bit rate was malformatted: " + e.message)
            }

            // Pk lev dB
            try {
                val pkLev = xpath.evaluate("peakleveldb/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(pkLev))
                    `as`.pkLevDb = Float(pkLev.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Pk lev dB was malformatted: " + e.message)
            }

            // RMS lev dB
            try {
                val rmsLev = xpath.evaluate("rmsleveldb/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(rmsLev))
                    `as`.rmsLevDb = Float(rmsLev.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("RMS lev dB was malformatted: " + e.message)
            }

            // RMS Pk dB
            try {
                val rmsPk = xpath.evaluate("rmspeakdb/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(rmsPk))
                    `as`.rmsPkDb = Float(rmsPk.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("RMS Pk dB was malformatted: " + e.message)
            }

            // device
            val captureDevice = xpath.evaluate("device/@type", node, XPathConstants.STRING) as String
            if (!StringUtils.isBlank(captureDevice))
                `as`.device.type = captureDevice
            val captureDeviceVersion = xpath.evaluate("device/@version", node, XPathConstants.STRING) as String
            if (!StringUtils.isBlank(captureDeviceVersion))
                `as`.device.version = captureDeviceVersion
            val captureDeviceVendor = xpath.evaluate("device/@vendor", node, XPathConstants.STRING) as String
            if (!StringUtils.isBlank(captureDeviceVendor))
                `as`.device.vendor = captureDeviceVendor

            // encoder
            val format = xpath.evaluate("encoder/@type", node, XPathConstants.STRING) as String
            if (!StringUtils.isBlank(format))
                `as`.encoder.type = format
            val formatVersion = xpath.evaluate("encoder/@version", node, XPathConstants.STRING) as String
            if (!StringUtils.isBlank(formatVersion))
                `as`.encoder.version = formatVersion
            val encoderLibraryVendor = xpath.evaluate("encoder/@vendor", node, XPathConstants.STRING) as String
            if (!StringUtils.isBlank(encoderLibraryVendor))
                `as`.encoder.vendor = encoderLibraryVendor

            return `as`
        }
    }

}
