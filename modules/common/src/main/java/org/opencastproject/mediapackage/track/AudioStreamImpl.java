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

import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackageSerializer;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Implementation of {@link org.opencastproject.mediapackage.AudioStream}.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "audio", namespace = "http://mediapackage.opencastproject.org")
public class AudioStreamImpl extends AbstractStreamImpl implements AudioStream {

  @XmlElement(name = "bitdepth")
  protected Integer bitdepth;

  @XmlElement(name = "channels")
  protected Integer channels;

  @XmlElement(name = "samplingrate")
  protected Integer samplingrate;

  @XmlElement(name = "bitrate")
  protected Float bitrate;

  @XmlElement(name = "peakleveldb")
  protected Float pkLevDb;

  @XmlElement(name = "rmsleveldb")
  protected Float rmsLevDb;

  @XmlElement(name = "rmspeakdb")
  protected Float rmsPkDb;

  public AudioStreamImpl() {
    this(UUID.randomUUID().toString());
  }

  public AudioStreamImpl(String identifier) {
    super(identifier);
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Element node = document.createElement("audio");
    addCommonManifestElements(node, document, serializer);

    // Channels
    if (channels != null) {
      Element channelsNode = document.createElement("channels");
      channelsNode.appendChild(document.createTextNode(channels.toString()));
      node.appendChild(channelsNode);
    }

    // Bit depth
    if (bitdepth != null) {
      Element bitdepthNode = document.createElement("bitdepth");
      bitdepthNode.appendChild(document.createTextNode(bitdepth.toString()));
      node.appendChild(bitdepthNode);
    }

    // Bit rate
    if (bitrate != null) {
      Element bitratenode = document.createElement("bitrate");
      bitratenode.appendChild(document.createTextNode(bitrate.toString()));
      node.appendChild(bitratenode);
    }

    // Sampling rate
    if (samplingrate != null) {
      Element samplingrateNode = document.createElement("samplingrate");
      samplingrateNode.appendChild(document.createTextNode(samplingrate.toString()));
      node.appendChild(samplingrateNode);
    }

    // Pk lev dB
    if (pkLevDb != null) {
      Element peakleveldbNode = document.createElement("peakleveldb");
      peakleveldbNode.appendChild(document.createTextNode(pkLevDb.toString()));
      node.appendChild(peakleveldbNode);
    }
    // RMS lev dB
    if (rmsLevDb != null) {
      Element rmsleveldbNode = document.createElement("rmsleveldb");
      rmsleveldbNode.appendChild(document.createTextNode(rmsLevDb.toString()));
      node.appendChild(rmsleveldbNode);
    }
    // RMS Pk dB
    if (rmsPkDb != null) {
      Element rmspeakdbNode = document.createElement("rmspeakdb");
      rmspeakdbNode.appendChild(document.createTextNode(rmsPkDb.toString()));
      node.appendChild(rmspeakdbNode);
    }
    return node;
  }

  /**
   * Create an audio stream from the XML manifest.
   *
   * @param streamIdHint
   *          stream ID that has to be used if the manifest does not provide one. This is the case when reading an old
   *          manifest.
   */
  public static AudioStreamImpl fromManifest(String streamIdHint, Node node, XPath xpath) throws IllegalStateException,
          XPathException {
    // Create stream
    String sid = (String) xpath.evaluate("@id", node, XPathConstants.STRING);
    if (StringUtils.isEmpty(sid)) {
      sid = streamIdHint;
    }
    AudioStreamImpl as = new AudioStreamImpl(sid);
    partialFromManifest(as, node, xpath);

    // bit depth
    try {
      String bd = (String) xpath.evaluate("bitdepth/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(bd))
        as.bitdepth = Integer.valueOf(bd.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit depth was malformatted: " + e.getMessage());
    }

    // channels
    try {
      String strChannels = (String) xpath.evaluate("channels/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(strChannels))
        as.channels = Integer.valueOf(strChannels.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Number of channels was malformatted: " + e.getMessage());
    }

    // sampling rate
    try {
      String sr = (String) xpath.evaluate("framerate/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(sr))
        as.samplingrate = Integer.valueOf(sr.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Frame rate was malformatted: " + e.getMessage());
    }

    // Bit rate
    try {
      String br = (String) xpath.evaluate("bitrate/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(br))
        as.bitrate = Float.valueOf(br.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit rate was malformatted: " + e.getMessage());
    }

    // Pk lev dB
    try {
      String pkLev = (String) xpath.evaluate("peakleveldb/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(pkLev))
        as.pkLevDb = Float.valueOf(pkLev.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Pk lev dB was malformatted: " + e.getMessage());
    }

    // RMS lev dB
    try {
      String rmsLev = (String) xpath.evaluate("rmsleveldb/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(rmsLev))
        as.rmsLevDb = Float.valueOf(rmsLev.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("RMS lev dB was malformatted: " + e.getMessage());
    }

    // RMS Pk dB
    try {
      String rmsPk = (String) xpath.evaluate("rmspeakdb/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(rmsPk))
        as.rmsPkDb = Float.valueOf(rmsPk.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("RMS Pk dB was malformatted: " + e.getMessage());
    }

    return as;
  }

  @Override
  public Integer getBitDepth() {
    return bitdepth;
  }

  @Override
  public Integer getChannels() {
    return channels;
  }

  @Override
  public Integer getSamplingRate() {
    return samplingrate;
  }

  @Override
  public Float getBitRate() {
    return bitrate;
  }

  @Override
  public Float getPkLevDb() {
    return pkLevDb;
  }

  @Override
  public Float getRmsLevDb() {
    return rmsLevDb;
  }

  @Override
  public Float getRmsPkDb() {
    return rmsPkDb;
  }

  // Setter

  public void setBitDepth(Integer bitdepth) {
    this.bitdepth = bitdepth;
  }

  public void setChannels(Integer channels) {
    this.channels = channels;
  }

  public void setSamplingRate(Integer samplingRate) {
    this.samplingrate = samplingRate;
  }

  public void setBitRate(Float bitRate) {
    this.bitrate = bitRate;
  }

  public void setPkLevDb(Float pkLevDb) {
    this.pkLevDb = pkLevDb;
  }

  public void setRmsLevDb(Float rmsLevDb) {
    this.rmsLevDb = rmsLevDb;
  }

  public void setRmsPkDb(Float rmsPkDb) {
    this.rmsPkDb = rmsPkDb;
  }

  @Override
  public void setCaptureDevice(String captureDevice) {
    this.device.type = captureDevice;
  }

  @Override
  public void setCaptureDeviceVersion(String captureDeviceVersion) {
    this.device.version = captureDeviceVersion;
  }

  @Override
  public void setCaptureDeviceVendor(String captureDeviceVendor) {
    this.device.vendor = captureDeviceVendor;
  }

  @Override
  public void setFormat(String format) {
    this.encoder.type = format;
  }

  @Override
  public void setFormatVersion(String formatVersion) {
    this.encoder.version = formatVersion;
  }

  @Override
  public void setEncoderLibraryVendor(String encoderLibraryVendor) {
    this.encoder.vendor = encoderLibraryVendor;
  }

}
