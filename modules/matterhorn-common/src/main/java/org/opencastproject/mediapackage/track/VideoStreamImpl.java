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
import org.opencastproject.mediapackage.VideoStream;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Implementation of {@link org.opencastproject.mediapackage.VideoStream}.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "video", namespace = "http://mediapackage.opencastproject.org")
public class VideoStreamImpl extends AbstractStreamImpl implements VideoStream {

  @XmlElement(name = "bitrate")
  protected Float bitRate;

  @XmlElement(name = "framerate")
  protected Float frameRate;

  @XmlElement(name = "resolution")
  protected String resolution;

  protected Integer frameWidth;
  protected Integer frameHeight;

  @XmlElement(name = "scantype")
  protected Scan scanType = null;

  @XmlType(name = "scantype")
  static class Scan {
    @XmlAttribute(name = "type")
    protected ScanType type;
    @XmlAttribute(name = "order")
    protected ScanOrder order;

    @Override
    public String toString() {
      return type.toString();
    }
  }

  public VideoStreamImpl() {
    this(UUID.randomUUID().toString());
  }

  public VideoStreamImpl(String identifier) {
    super(identifier);
  }

  /**
   * @param s
   */
  public VideoStreamImpl(VideoStreamImpl s) {
    this.bitRate = s.bitRate;
    this.device = s.device;
    this.encoder = s.encoder;
    this.frameRate = s.frameRate;
    this.identifier = s.identifier;
    this.resolution = s.resolution;
    this.scanType = s.scanType;
  }

  /**
   * Create a video stream from the XML manifest.
   *
   * @param streamIdHint
   *          stream ID that has to be used if the manifest does not provide one. This is the case when reading an old
   *          manifest.
   */
  public static VideoStreamImpl fromManifest(String streamIdHint, Node node, XPath xpath) throws IllegalStateException,
          XPathException {
    // Create stream
    String sid = (String) xpath.evaluate("@id", node, XPathConstants.STRING);
    if (StringUtils.isEmpty(sid))
      sid = streamIdHint;
    VideoStreamImpl vs = new VideoStreamImpl(sid);

    // bit rate
    try {
      String strBitrate = (String) xpath.evaluate("bitrate/text()", node, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(strBitrate))
        vs.bitRate = new Float(strBitrate.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit rate was malformatted: " + e.getMessage());
    }

    // frame rate
    try {
      String strFrameRate = (String) xpath.evaluate("framerate/text()", node, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(strFrameRate))
        vs.frameRate = new Float(strFrameRate.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Frame rate was malformatted: " + e.getMessage());
    }

    // resolution
    String res = (String) xpath.evaluate("resolution/text()", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(res)) {
      vs.resolution = res;
    }

    // interlacing
    String scanType = (String) xpath.evaluate("scantype/@type", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(scanType)) {
      if (vs.scanType == null)
        vs.scanType = new Scan();
      vs.scanType.type = ScanType.fromString(scanType);
    }

    String scanOrder = (String) xpath.evaluate("interlacing/@order", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(scanOrder)) {
      if (vs.scanType == null)
        vs.scanType = new Scan();
      vs.scanType.order = ScanOrder.fromString(scanOrder);
    }
    // device
    String deviceType = (String) xpath.evaluate("device/@type", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(deviceType)) {
      if (vs.device == null)
        vs.device = new Device();
      vs.device.type = deviceType;
    }

    String deviceVersion = (String) xpath.evaluate("device/@version", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(deviceVersion)) {
      if (vs.device == null)
        vs.device = new Device();
      vs.device.version = deviceVersion;
    }

    String deviceVendor = (String) xpath.evaluate("device/@vendor", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(deviceVendor)) {
      if (vs.device == null)
        vs.device = new Device();
      vs.device.vendor = deviceVendor;
    }

    // encoder
    String encoderType = (String) xpath.evaluate("encoder/@type", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(encoderType)) {
      if (vs.encoder == null)
        vs.encoder = new Encoder();
      vs.encoder.type = encoderType;
    }

    String encoderVersion = (String) xpath.evaluate("encoder/@version", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(encoderVersion)) {
      if (vs.encoder == null)
        vs.encoder = new Encoder();
      vs.encoder.version = encoderVersion;
    }

    String encoderVendor = (String) xpath.evaluate("encoder/@vendor", node, XPathConstants.STRING);
    if (StringUtils.isNotEmpty(encoderVendor)) {
      if (vs.encoder == null)
        vs.encoder = new Encoder();
      vs.encoder.vendor = encoderVendor;
    }

    return vs;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Element node = document.createElement("video");
    // Stream ID
    node.setAttribute("id", getIdentifier());

    // device
    Element deviceNode = document.createElement("device");
    boolean hasAttr = false;
    if (device.type != null) {
      deviceNode.setAttribute("type", device.type);
      hasAttr = true;
    }
    if (device.version != null) {
      deviceNode.setAttribute("version", device.version);
      hasAttr = true;
    }
    if (device.vendor != null) {
      deviceNode.setAttribute("vendor", device.vendor);
      hasAttr = true;
    }
    if (hasAttr)
      node.appendChild(deviceNode);

    // encoder
    Element encoderNode = document.createElement("encoder");
    hasAttr = false;
    if (encoder.type != null) {
      encoderNode.setAttribute("type", encoder.type);
      hasAttr = true;
    }
    if (encoder.version != null) {
      encoderNode.setAttribute("version", encoder.version);
      hasAttr = true;
    }
    if (encoder.vendor != null) {
      encoderNode.setAttribute("vendor", encoder.vendor);
      hasAttr = true;
    }
    if (hasAttr)
      node.appendChild(encoderNode);

    // Resolution
    Element resolutionNode = document.createElement("resolution");
    resolutionNode.appendChild(document.createTextNode(resolution));
    node.appendChild(resolutionNode);

    // Interlacing
    if (scanType != null) {
      Element interlacingNode = document.createElement("scantype");
      interlacingNode.setAttribute("type", scanType.toString());
      if (scanType.order != null)
        interlacingNode.setAttribute("order", scanType.order.toString());
      node.appendChild(interlacingNode);
    }

    // Bit rate
    if (bitRate != null) {
      Element bitrateNode = document.createElement("bitrate");
      bitrateNode.appendChild(document.createTextNode(bitRate.toString()));
      node.appendChild(bitrateNode);
    }

    // Frame rate
    if (frameRate != null) {
      Element framerateNode = document.createElement("framerate");
      framerateNode.appendChild(document.createTextNode(frameRate.toString()));
      node.appendChild(framerateNode);
    }

    return node;
  }

  @Override
  public Float getBitRate() {
    return bitRate;
  }

  @Override
  public Float getFrameRate() {
    return frameRate;
  }

  @Override
  public Integer getFrameWidth() {
    try {
      String[] s = resolution.trim().split("x");
      if (s.length != 2)
        throw new IllegalStateException("video size must be of the form <hsize>x<vsize>, found " + resolution);
      return new Integer(s[0].trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Resolution was malformatted: " + e.getMessage());
    }
  }

  @Override
  public Integer getFrameHeight() {
    try {
      String[] s = resolution.trim().split("x");
      if (s.length != 2)
        throw new IllegalStateException("video size must be of the form <hsize>x<vsize>, found " + resolution);
      return new Integer(s[1].trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Resolution was malformatted: " + e.getMessage());
    }
  }

  @Override
  public ScanType getScanType() {
    return scanType.type;
  }

  @Override
  public ScanOrder getScanOrder() {
    return scanType.order;
  }

  // Setter

  public void setBitRate(Float bitRate) {
    this.bitRate = bitRate;
  }

  public void setFrameRate(Float frameRate) {
    this.frameRate = frameRate;
  }

  public void setFrameWidth(Integer frameWidth) {
    this.frameWidth = frameWidth;
    if (frameWidth != null && frameHeight != null)
      updateResolution();
  }

  public void setFrameHeight(Integer frameHeight) {
    this.frameHeight = frameHeight;
    if (frameWidth != null && frameHeight != null)
      updateResolution();
  }

  private void updateResolution() {
    resolution = frameWidth.toString() + "x" + frameHeight.toString();
  }

  public void setScanType(ScanType scanType) {
    if (scanType == null)
      return;
    if (this.scanType == null)
      this.scanType = new Scan();
    this.scanType.type = scanType;
  }

  public void setScanOrder(ScanOrder scanOrder) {
    if (scanOrder == null)
      return;
    if (this.scanType == null)
      this.scanType = new Scan();
    this.scanType.order = scanOrder;
  }
}
