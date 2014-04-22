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

package org.opencastproject.inspection.ffmpeg.api;

import org.opencastproject.mediapackage.track.Colorimetry;
import org.opencastproject.mediapackage.track.FrameRateMode;
import org.opencastproject.mediapackage.track.ScanOrder;
import org.opencastproject.mediapackage.track.ScanType;

/**
 * This class bundles technical information about a video stream.
 */
public class VideoStreamMetadata extends StreamMetadata {

  protected String formatSettingsBVOP;
  protected String formatSettingsCABAC;
  protected String formatSettingsQPel;
  protected String formatSettingsGMC;
  protected String formatSettingsMatrix;
  protected String formatSettingsRefFrames;
  protected String formatSettingsPulldown;

  protected Integer frameWidth;
  protected Integer frameHeight;
  protected Float pixelAspectRatio;
  protected Float displayAspectRatio;

  protected Float frameRate;
  protected Float frameRateMinimum;
  protected Float frameRateMaximum;
  protected FrameRateMode frameRateMode;

  protected Long frameCount;

  // PAL, NTSC
  protected String videoStandard;

  // bits / (pixel * frame)
  protected Float qualityFactor;

  protected ScanType scanType;
  protected ScanOrder scanOrder;

  protected Colorimetry colorimetry;

  public String getFormatSettingsBVOP() {
    return formatSettingsBVOP;
  }

  public void setFormatSettingsBVOP(String formatSettingsBVOP) {
    this.formatSettingsBVOP = formatSettingsBVOP;
  }

  public String getFormatSettingsCABAC() {
    return formatSettingsCABAC;
  }

  public void setFormatSettingsCABAC(String formatSettingsCABAC) {
    this.formatSettingsCABAC = formatSettingsCABAC;
  }

  public String getFormatSettingsQPel() {
    return formatSettingsQPel;
  }

  public void setFormatSettingsQPel(String formatSettingsQPel) {
    this.formatSettingsQPel = formatSettingsQPel;
  }

  public String getFormatSettingsGMC() {
    return formatSettingsGMC;
  }

  public void setFormatSettingsGMC(String formatSettingsGMC) {
    this.formatSettingsGMC = formatSettingsGMC;
  }

  public String getFormatSettingsMatrix() {
    return formatSettingsMatrix;
  }

  public void setFormatSettingsMatrix(String formatSettingsMatrix) {
    this.formatSettingsMatrix = formatSettingsMatrix;
  }

  public String getFormatSettingsRefFrames() {
    return formatSettingsRefFrames;
  }

  public void setFormatSettingsRefFrames(String formatSettingsRefFrames) {
    this.formatSettingsRefFrames = formatSettingsRefFrames;
  }

  public String getFormatSettingsPulldown() {
    return formatSettingsPulldown;
  }

  public void setFormatSettingsPulldown(String formatSettingsPulldown) {
    this.formatSettingsPulldown = formatSettingsPulldown;
  }

  /**
   * Returns the frame width in pixels.
   */
  public Integer getFrameWidth() {
    return frameWidth;
  }

  /**
   * Sets the frame width in pixels.
   */
  public void setFrameWidth(Integer frameWidth) {
    this.frameWidth = frameWidth;
  }

  /**
   * Returns the frame height in pixels.
   */
  public Integer getFrameHeight() {
    return frameHeight;
  }

  /**
   * Sets the frame height in pixels.
   */
  public void setFrameHeight(Integer frameHeight) {
    this.frameHeight = frameHeight;
  }

  /**
   * Gets the pixel aspect ratio.
   */
  public Float getPixelAspectRatio() {
    return pixelAspectRatio;
  }

  public void setPixelAspectRatio(Float pixelAspectRatio) {
    this.pixelAspectRatio = pixelAspectRatio;
  }

  public Float getDisplayAspectRatio() {
    return displayAspectRatio;
  }

  public void setDisplayAspectRatio(Float displayAspectRatio) {
    this.displayAspectRatio = displayAspectRatio;
  }

  /**
   * Returns the frame rate in frames per second.
   */
  public Float getFrameRate() {
    return frameRate;
  }

  /**
   * Sets the frame rate in frames per second.
   */
  public void setFrameRate(Float frameRate) {
    this.frameRate = frameRate;
  }

  public Float getFrameRateMinimum() {
    return frameRateMinimum;
  }

  public void setFrameRateMinimum(Float frameRateMinimum) {
    this.frameRateMinimum = frameRateMinimum;
  }

  public Float getFrameRateMaximum() {
    return frameRateMaximum;
  }

  public void setFrameRateMaximum(Float frameRateMaximum) {
    this.frameRateMaximum = frameRateMaximum;
  }

  public FrameRateMode getFrameRateMode() {
    return frameRateMode;
  }

  public void setFrameRateMode(FrameRateMode frameRateMode) {
    this.frameRateMode = frameRateMode;
  }

  public Long getFrameCount() {
    return frameCount;
  }

  public void setFrameCount(Long frameCount) {
    this.frameCount = frameCount;
  }

  public String getVideoStandard() {
    return videoStandard;
  }

  public void setVideoStandard(String videoStandard) {
    this.videoStandard = videoStandard;
  }

  public Float getQualityFactor() {
    return qualityFactor;
  }

  public void setQualityFactor(Float qualityFactor) {
    this.qualityFactor = qualityFactor;
  }

  public ScanType getScanType() {
    return scanType;
  }

  public void setScanType(ScanType scanType) {
    this.scanType = scanType;
  }

  public ScanOrder getScanOrder() {
    return scanOrder;
  }

  public void setScanOrder(ScanOrder scanOrder) {
    this.scanOrder = scanOrder;
  }

  public Colorimetry getColorimetry() {
    return colorimetry;
  }

  public void setColorimetry(Colorimetry colorimetry) {
    this.colorimetry = colorimetry;
  }
}
