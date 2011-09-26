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
package org.opencastproject.capture.pipeline.bins;

import org.opencastproject.capture.CaptureParameters;

import java.util.Properties;

public class CaptureDeviceProperties {
  private String customProducer;
  private String customConsumer;
  private String codec;
  private String container;
  private String bitrate;
  private String framerate;
  private String bufferCount;
  private String bufferBytes;
  private String bufferTime;
  private boolean confidence;

  /**
   * A data class that processes the captureDevice and properties taken in by each Consumer and Producer and gathers the
   * salient properties. These properties are all settings for the GStreamer properties and are intended to be passed
   * through as is. Certain codecs for example have different ranges for bitrate and so it is important to check the
   * GStreamer docs for possible values.
   * 
   * @param captureDevice
   *          Details about the captureDevice such as location, codec, container, etc.
   * @param properties
   *          Confidence monitoring properties.
   **/
  public CaptureDeviceProperties(CaptureDevice captureDevice, Properties properties) {
    customProducer = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
            + CaptureParameters.CAPTURE_DEVICE_CUSTOM_PRODUCER);
    customConsumer = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
            + CaptureParameters.CAPTURE_DEVICE_CUSTOM_CONSUMER);
    codec = captureDevice.getProperties().getProperty("codec");
    container = captureDevice.getProperties().getProperty("container");
    bitrate = captureDevice.getProperties().getProperty("bitrate");
    framerate = captureDevice.getProperties().getProperty("framerate");
    bufferCount = captureDevice.getProperties().getProperty("bufferCount");
    bufferBytes = captureDevice.getProperties().getProperty("bufferBytes");
    bufferTime = captureDevice.getProperties().getProperty("bufferTime");
    if (properties != null && properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE) != null) {
      confidence = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));
    } else {
      confidence = false;
    }
  }

  public String getCustomProducer() {
    return customProducer;
  }
  
  public void setCustomSource(String customSource) {
    this.customProducer = customSource;
  }

  public String getCustomConsumer() {
    return customConsumer;
  }
  
  public void setCustomConsumer(String customConsumer) {
    this.customConsumer = customConsumer;
  }
  
  public String getCodec() {
    return codec;
  }

  public void setCodec(String codec) {
    this.codec = codec;
  }

  public String getContainer() {
    return container;
  }

  public void setContainer(String container) {
    this.container = container;
  }

  public String getBitrate() {
    return bitrate;
  }

  public void setBitrate(String bitrate) {
    this.bitrate = bitrate;
  }

  public String getFramerate() {
    return framerate;
  }

  public void setFramerate(String framerate) {
    this.framerate = framerate;
  }

  public String getBufferCount() {
    return bufferCount;
  }

  public void setBufferCount(String bufferCount) {
    this.bufferCount = bufferCount;
  }

  public String getBufferBytes() {
    return bufferBytes;
  }

  public void setBufferBytes(String bufferBytes) {
    this.bufferBytes = bufferBytes;
  }

  public String getBufferTime() {
    return bufferTime;
  }

  public void setBufferTime(String bufferTime) {
    this.bufferTime = bufferTime;
  }

  public boolean isConfidence() {
    return confidence;
  }

  public void setConfidence(boolean confidence) {
    this.confidence = confidence;
  }
}
