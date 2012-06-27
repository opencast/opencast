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

package org.opencastproject.inspection.impl.api;


/**
 * Common metadata for all kind of embedded media streams.
 */
public class StreamMetadata extends TemporalMetadata {

  protected String alignment;

  protected Long delay;

  protected Float compressionRatio;

  protected String captureDevice;
  protected String captureDeviceVersion;
  protected String captureDeviceVendor;
  protected String captureDeviceURL;
  protected String captureDeviceInfo;

  // bits
  private Integer resolution;

  public String getAlignment() {
    return alignment;
  }

  public void setAlignment(String alignment) {
    this.alignment = alignment;
  }

  public Long getDelay() {
    return delay;
  }

  public void setDelay(Long delay) {
    this.delay = delay;
  }

  public Float getCompressionRatio() {
    return compressionRatio;
  }

  public void setCompressionRatio(Float compressionRatio) {
    this.compressionRatio = compressionRatio;
  }

  public String getCaptureDevice() {
    return captureDevice;
  }

  public void setCaptureDevice(String captureDevice) {
    this.captureDevice = captureDevice;
  }

  public String getCaptureDeviceVersion() {
    return captureDeviceVersion;
  }

  public void setCaptureDeviceVersion(String captureDeviceVersion) {
    this.captureDeviceVersion = captureDeviceVersion;
  }

  public String getCaptureDeviceVendor() {
    return captureDeviceVendor;
  }

  public void setCaptureDeviceVendor(String captureDeviceVendor) {
    this.captureDeviceVendor = captureDeviceVendor;
  }

  public String getCaptureDeviceURL() {
    return captureDeviceURL;
  }

  public void setCaptureDeviceURL(String captureDeviceURL) {
    this.captureDeviceURL = captureDeviceURL;
  }

  public String getCaptureDeviceInfo() {
    return captureDeviceInfo;
  }

  public void setCaptureDeviceInfo(String captureDeviceInfo) {
    this.captureDeviceInfo = captureDeviceInfo;
  }

  /** Returns the resolution in bits. Usually one of 8, 16, 20 or 24. */
  public Integer getResolution() {
    return resolution;
  }

  /** Sets the resolution in bits. Usually one of 8, 16, 20 or 24. */
  public void setResolution(Integer resolution) {
    this.resolution = resolution;
  }
}
