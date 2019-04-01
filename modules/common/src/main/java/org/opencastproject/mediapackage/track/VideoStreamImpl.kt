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

import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.mediapackage.VideoStream

import org.apache.commons.lang3.StringUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

import java.util.UUID

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlType
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathException

/**
 * Implementation of [org.opencastproject.mediapackage.VideoStream].
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "video", namespace = "http://mediapackage.opencastproject.org")
class VideoStreamImpl @JvmOverloads constructor(identifier: String = UUID.randomUUID().toString()) : AbstractStreamImpl(identifier), VideoStream {

    // Setter

    @XmlElement(name = "bitrate")
    override var bitRate: Float? = null

    @XmlElement(name = "framerate")
    override var frameRate: Float? = null

    @XmlElement(name = "resolution")
    protected var resolution: String

    protected var frameWidth: Int? = null
    protected var frameHeight: Int? = null

    @XmlElement(name = "scantype")
    protected var scanType: Scan? = null

    override var scanOrder: ScanOrder?
        get() = if (scanType != null) scanType!!.order else null
        set(scanOrder) {
            if (scanOrder == null)
                return
            if (this.scanType == null)
                this.scanType = Scan()
            this.scanType!!.order = scanOrder
        }

    @XmlType(name = "scantype")
    internal class Scan {
        @XmlAttribute(name = "type")
        var type: ScanType? = null
        @XmlAttribute(name = "order")
        var order: ScanOrder? = null

        override fun toString(): String {
            return type!!.toString()
        }
    }

    /**
     * @see org.opencastproject.mediapackage.ManifestContributor.toManifest
     */
    override fun toManifest(document: Document, serializer: MediaPackageSerializer): Node {
        val node = document.createElement("video")
        // Stream ID
        node.setAttribute("id", identifier)

        // Frame count
        if (frameCount != null) {
            val frameCountNode = document.createElement("framecount")
            frameCountNode.appendChild(document.createTextNode(java.lang.Long.toString(frameCount!!)))
            node.appendChild(frameCountNode)
        }

        // device
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

        // encoder
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

        // Resolution
        val resolutionNode = document.createElement("resolution")
        resolutionNode.appendChild(document.createTextNode(resolution))
        node.appendChild(resolutionNode)

        // Interlacing
        if (scanType != null) {
            val interlacingNode = document.createElement("scantype")
            interlacingNode.setAttribute("type", scanType!!.toString())
            if (scanType!!.order != null)
                interlacingNode.setAttribute("order", scanType!!.order!!.toString())
            node.appendChild(interlacingNode)
        }

        // Bit rate
        if (bitRate != null) {
            val bitrateNode = document.createElement("bitrate")
            bitrateNode.appendChild(document.createTextNode(bitRate!!.toString()))
            node.appendChild(bitrateNode)
        }

        // Frame rate
        if (frameRate != null) {
            val framerateNode = document.createElement("framerate")
            framerateNode.appendChild(document.createTextNode(frameRate!!.toString()))
            node.appendChild(framerateNode)
        }

        return node
    }

    override fun getFrameWidth(): Int? {
        try {
            val s = resolution.trim { it <= ' ' }.split("x".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            if (s.size != 2)
                throw IllegalStateException("video size must be of the form <hsize>x<vsize>, found $resolution")
            return Int(s[0].trim({ it <= ' ' }))
        } catch (e: NumberFormatException) {
            throw IllegalStateException("Resolution was malformatted: " + e.message)
        }

    }

    override fun getFrameHeight(): Int? {
        try {
            val s = resolution.trim { it <= ' ' }.split("x".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            if (s.size != 2)
                throw IllegalStateException("video size must be of the form <hsize>x<vsize>, found $resolution")
            return Int(s[1].trim({ it <= ' ' }))
        } catch (e: NumberFormatException) {
            throw IllegalStateException("Resolution was malformatted: " + e.message)
        }

    }

    override fun getScanType(): ScanType? {
        return if (scanType != null) scanType!!.type else null
    }

    fun setFrameWidth(frameWidth: Int?) {
        this.frameWidth = frameWidth
        if (frameWidth != null && frameHeight != null)
            updateResolution()
    }

    fun setFrameHeight(frameHeight: Int?) {
        this.frameHeight = frameHeight
        if (frameWidth != null && frameHeight != null)
            updateResolution()
    }

    private fun updateResolution() {
        resolution = frameWidth!!.toString() + "x" + frameHeight!!.toString()
    }

    fun setScanType(scanType: ScanType?) {
        if (scanType == null)
            return
        if (this.scanType == null)
            this.scanType = Scan()
        this.scanType!!.type = scanType
    }

    companion object {

        /**
         * Create a video stream from the XML manifest.
         *
         * @param streamIdHint
         * stream ID that has to be used if the manifest does not provide one. This is the case when reading an old
         * manifest.
         */
        @Throws(IllegalStateException::class, XPathException::class)
        fun fromManifest(streamIdHint: String, node: Node, xpath: XPath): VideoStreamImpl {
            // Create stream
            var sid = xpath.evaluate("@id", node, XPathConstants.STRING) as String
            if (StringUtils.isEmpty(sid))
                sid = streamIdHint
            val vs = VideoStreamImpl(sid)

            // Frame count
            try {
                val frameCount = xpath.evaluate("framecount/text()", node, XPathConstants.STRING) as String
                if (!StringUtils.isBlank(frameCount))
                    vs.frameCount = Long(frameCount.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Frame count was malformatted: " + e.message)
            }

            // bit rate
            try {
                val strBitrate = xpath.evaluate("bitrate/text()", node, XPathConstants.STRING) as String
                if (StringUtils.isNotEmpty(strBitrate))
                    vs.bitRate = Float(strBitrate.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Bit rate was malformatted: " + e.message)
            }

            // frame rate
            try {
                val strFrameRate = xpath.evaluate("framerate/text()", node, XPathConstants.STRING) as String
                if (StringUtils.isNotEmpty(strFrameRate))
                    vs.frameRate = Float(strFrameRate.trim { it <= ' ' })
            } catch (e: NumberFormatException) {
                throw IllegalStateException("Frame rate was malformatted: " + e.message)
            }

            // resolution
            val res = xpath.evaluate("resolution/text()", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(res)) {
                vs.resolution = res
            }

            // interlacing
            val scanType = xpath.evaluate("scantype/@type", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(scanType)) {
                if (vs.scanType == null)
                    vs.scanType = Scan()
                vs.scanType!!.type = ScanType.fromString(scanType)
            }

            val scanOrder = xpath.evaluate("interlacing/@order", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(scanOrder)) {
                if (vs.scanType == null)
                    vs.scanType = Scan()
                vs.scanType!!.order = ScanOrder.fromString(scanOrder)
            }
            // device
            val deviceType = xpath.evaluate("device/@type", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(deviceType)) {
                if (vs.device == null)
                    vs.device = AbstractStreamImpl.Device()
                vs.device.type = deviceType
            }

            val deviceVersion = xpath.evaluate("device/@version", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(deviceVersion)) {
                if (vs.device == null)
                    vs.device = AbstractStreamImpl.Device()
                vs.device.version = deviceVersion
            }

            val deviceVendor = xpath.evaluate("device/@vendor", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(deviceVendor)) {
                if (vs.device == null)
                    vs.device = AbstractStreamImpl.Device()
                vs.device.vendor = deviceVendor
            }

            // encoder
            val encoderType = xpath.evaluate("encoder/@type", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(encoderType)) {
                if (vs.encoder == null)
                    vs.encoder = AbstractStreamImpl.Encoder()
                vs.encoder.type = encoderType
            }

            val encoderVersion = xpath.evaluate("encoder/@version", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(encoderVersion)) {
                if (vs.encoder == null)
                    vs.encoder = AbstractStreamImpl.Encoder()
                vs.encoder.version = encoderVersion
            }

            val encoderVendor = xpath.evaluate("encoder/@vendor", node, XPathConstants.STRING) as String
            if (StringUtils.isNotEmpty(encoderVendor)) {
                if (vs.encoder == null)
                    vs.encoder = AbstractStreamImpl.Encoder()
                vs.encoder.vendor = encoderVendor
            }

            return vs
        }
    }
}
