/*
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
    if (StringUtils.isEmpty(sid)) {
      sid = streamIdHint;
    }
    VideoStreamImpl vs = new VideoStreamImpl(sid);
    partialFromManifest(vs, node, xpath);

    // bit rate
    try {
      String strBitrate = (String) xpath.evaluate("bitrate/text()", node, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(strBitrate))
        vs.bitRate = Float.valueOf(strBitrate.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit rate was malformatted: " + e.getMessage());
    }

    // frame rate
    try {
      String strFrameRate = (String) xpath.evaluate("framerate/text()", node, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(strFrameRate))
        vs.frameRate = Float.valueOf(strFrameRate.trim());
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

    return vs;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Element node = document.createElement("video");
    addCommonManifestElements(node, document, serializer);

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
      return Integer.valueOf(s[0].trim());
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
      return Integer.valueOf(s[1].trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Resolution was malformatted: " + e.getMessage());
    }
  }

  @Override
  public ScanType getScanType() {
    return scanType != null ? scanType.type : null;
  }

  @Override
  public ScanOrder getScanOrder() {
    return scanType != null ? scanType.order : null;
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
