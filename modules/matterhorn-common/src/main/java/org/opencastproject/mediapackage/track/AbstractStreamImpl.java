/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.mediapackage.track;

import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlTransient
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractStreamImpl implements Stream {

  @XmlID
  @XmlAttribute(name = "id")
  protected String identifier;

  @XmlElement(name = "device")
  protected Device device = new Device();

  @XmlElement(name = "encoder")
  protected Encoder encoder = new Encoder();

  @XmlType(name = "device")
  static class Device {
    @XmlAttribute(name = "type")
    protected String type;
    @XmlAttribute(name = "version")
    protected String version;
    @XmlAttribute(name = "vendor")
    protected String vendor;
  }

  @XmlType(name = "encoder")
  static class Encoder {
    @XmlAttribute(name = "type")
    protected String type;
    @XmlAttribute(name = "version")
    protected String version;
    @XmlAttribute(name = "vendor")
    protected String vendor;
  }

  protected AbstractStreamImpl() {
  }

  protected AbstractStreamImpl(String identifier) {
    this.identifier = identifier;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getCaptureDevice() {
    return device.type;
  }

  public String getCaptureDeviceVersion() {
    return device.version;
  }

  public String getCaptureDeviceVendor() {
    return device.vendor;
  }

  public String getFormat() {
    return encoder.type;
  }

  public String getFormatVersion() {
    return encoder.version;
  }

  public String getEncoderLibraryVendor() {
    return encoder.vendor;
  }

  public void setCaptureDevice(String capturedevice) {
    this.device.type = capturedevice;
  }

  public void setCaptureDeviceVersion(String capturedeviceVersion) {
    this.device.version = capturedeviceVersion;
  }

  public void setCaptureDeviceVendor(String captureDeviceVendor) {
    this.device.vendor = captureDeviceVendor;
  }

  public void setFormat(String format) {
    this.encoder.type = format;
  }

  public void setFormatVersion(String formatVersion) {
    this.encoder.version = formatVersion;
  }

  public void setEncoderLibraryVendor(String encoderLibraryVendor) {
    this.encoder.vendor = encoderLibraryVendor;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    throw new RuntimeException("unable to serialize " + this);
  }
}
