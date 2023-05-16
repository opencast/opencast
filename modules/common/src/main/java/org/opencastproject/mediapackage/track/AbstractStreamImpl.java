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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.mediapackage.track;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Stream;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

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

  @XmlElement(name = "framecount")
  protected Long frameCount;

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

  public Long getFrameCount() {
    return frameCount;
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

  public void setFrameCount(Long frameCount) {
    this.frameCount = frameCount;
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

  protected static void partialFromManifest(AbstractStreamImpl stream, Node node, XPath xpath)
      throws IllegalStateException, XPathException {
    // Frame count
    try {
      String frameCount = (String) xpath.evaluate("framecount/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(frameCount)) {
        stream.frameCount = Long.valueOf(frameCount.trim());
      }
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Frame count was malformatted: " + e.getMessage());
    }

    // Device
    Device dev = new Device();
    dev.type = trimToNull((String) xpath.evaluate("device/@type", node, XPathConstants.STRING));
    dev.version = trimToNull((String) xpath.evaluate("device/@version", node, XPathConstants.STRING));
    dev.vendor = trimToNull((String) xpath.evaluate("device/@vendor", node, XPathConstants.STRING));
    if (dev.type != null || dev.version != null || dev.vendor != null) {
      stream.device = dev;
    }

    // Encoder
    Encoder enc = new Encoder();
    enc.type = trimToNull(((String) xpath.evaluate("encoder/@type", node, XPathConstants.STRING)));
    enc.version = trimToNull(((String) xpath.evaluate("encoder/@version", node, XPathConstants.STRING)));
    enc.vendor = trimToNull(((String) xpath.evaluate("encoder/@vendor", node, XPathConstants.STRING)));
    if (enc.type != null || enc.version != null || enc.vendor != null) {
      stream.encoder = enc;
    }
  }

  protected void addCommonManifestElements(Element node, Document document, MediaPackageSerializer serializer) {
    // Stream ID
    node.setAttribute("id", getIdentifier());

    // Frame count
    if (frameCount != null) {
      Element frameCountNode = document.createElement("framecount");
      frameCountNode.appendChild(document.createTextNode(Long.toString(frameCount)));
      node.appendChild(frameCountNode);
    }

    // Device
    Element deviceNode = document.createElement("device");
    boolean addChild = false;
    if (device.type != null) {
      deviceNode.setAttribute("type", device.type);
      addChild = true;
    }
    if (device.version != null) {
      deviceNode.setAttribute("version", device.version);
      addChild = true;
    }
    if (device.vendor != null) {
      deviceNode.setAttribute("vendor", device.vendor);
      addChild = true;
    }
    if (addChild) {
      node.appendChild(deviceNode);
    }

    // Encoder
    Element encoderNode = document.createElement("encoder");
    addChild = false;
    if (encoder.type != null) {
      encoderNode.setAttribute("type", encoder.type);
      addChild = true;
    }
    if (encoder.version != null) {
      encoderNode.setAttribute("version", encoder.version);
      addChild = true;
    }
    if (encoder.vendor != null) {
      encoderNode.setAttribute("vendor", encoder.vendor);
      addChild = true;
    }
    if (addChild)
      node.appendChild(encoderNode);
  }
}
