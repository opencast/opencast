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
import org.opencastproject.mediapackage.Stream

import org.w3c.dom.Document
import org.w3c.dom.Node

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlID
import javax.xml.bind.annotation.XmlTransient
import javax.xml.bind.annotation.XmlType

@XmlTransient
@XmlAccessorType(XmlAccessType.NONE)
abstract class AbstractStreamImpl : Stream {

    @XmlID
    @XmlAttribute(name = "id")
    override var identifier: String

    @XmlElement(name = "device")
    protected var device = Device()

    @XmlElement(name = "encoder")
    protected var encoder = Encoder()

    @XmlElement(name = "framecount")
    override var frameCount: Long? = null

    var captureDevice: String?
        get() = device.type
        set(capturedevice) {
            this.device.type = capturedevice
        }

    var captureDeviceVersion: String?
        get() = device.version
        set(capturedeviceVersion) {
            this.device.version = capturedeviceVersion
        }

    var captureDeviceVendor: String?
        get() = device.vendor
        set(captureDeviceVendor) {
            this.device.vendor = captureDeviceVendor
        }

    var format: String?
        get() = encoder.type
        set(format) {
            this.encoder.type = format
        }

    var formatVersion: String?
        get() = encoder.version
        set(formatVersion) {
            this.encoder.version = formatVersion
        }

    var encoderLibraryVendor: String?
        get() = encoder.vendor
        set(encoderLibraryVendor) {
            this.encoder.vendor = encoderLibraryVendor
        }

    @XmlType(name = "device")
    internal class Device {
        @XmlAttribute(name = "type")
        var type: String? = null
        @XmlAttribute(name = "version")
        var version: String? = null
        @XmlAttribute(name = "vendor")
        var vendor: String? = null
    }

    @XmlType(name = "encoder")
    internal class Encoder {
        @XmlAttribute(name = "type")
        var type: String? = null
        @XmlAttribute(name = "version")
        var version: String? = null
        @XmlAttribute(name = "vendor")
        var vendor: String? = null
    }

    protected constructor() {}

    protected constructor(identifier: String) {
        this.identifier = identifier
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.ManifestContributor.toManifest
     */
    override fun toManifest(document: Document, serializer: MediaPackageSerializer): Node {
        throw RuntimeException("unable to serialize $this")
    }
}
