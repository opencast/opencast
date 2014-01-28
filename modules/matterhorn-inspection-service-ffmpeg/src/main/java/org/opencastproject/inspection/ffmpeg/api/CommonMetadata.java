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

import java.net.URL;
import java.util.Date;
import java.util.Locale;

/**
 * Common metadata for all kinds of media objects.
 */
public class CommonMetadata {

  protected String format;
  protected String formatInfo;
  protected URL formatURL;
  protected String formatVersion;
  protected String formatProfile;
  protected String formatSettingsSummary;

  protected String encoderApplication;
  protected URL encoderApplicationURL;
  protected URL encoderApplicationVendor;
  protected String encoderLibrary;
  protected URL encoderLibraryURL;
  protected String encoderLibraryInfo;
  protected String encoderLibraryVersion;
  protected String encoderLibraryVendor;
  protected String encoderLibraryReleaseDate;
  protected String encoderLibrarySettings;

  protected Boolean encrypted;

  protected Date encodedDate;
  protected Date taggedDate;

  protected String title;

  protected Locale language;

  // bytes
  protected Long size;

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getFormatInfo() {
    return formatInfo;
  }

  public void setFormatInfo(String formatInfo) {
    this.formatInfo = formatInfo;
  }

  public URL getFormatURL() {
    return formatURL;
  }

  public void setFormatURL(URL formatURL) {
    this.formatURL = formatURL;
  }

  public String getFormatVersion() {
    return formatVersion;
  }

  public void setFormatVersion(String formatVersion) {
    this.formatVersion = formatVersion;
  }

  public String getFormatProfile() {
    return formatProfile;
  }

  public void setFormatProfile(String formatProfile) {
    this.formatProfile = formatProfile;
  }

  public String getFormatSettingsSummary() {
    return formatSettingsSummary;
  }

  public void setFormatSettingsSummary(String formatSettingsSummary) {
    this.formatSettingsSummary = formatSettingsSummary;
  }

  public String getEncoderApplication() {
    return encoderApplication;
  }

  public void setEncoderApplication(String encoderApplication) {
    this.encoderApplication = encoderApplication;
  }

  public URL getEncoderApplicationURL() {
    return encoderApplicationURL;
  }

  public void setEncoderApplicationURL(URL encoderApplicationURL) {
    this.encoderApplicationURL = encoderApplicationURL;
  }

  public String getEncoderLibrary() {
    return encoderLibrary;
  }

  public void setEncoderLibrary(String encoderLibrary) {
    this.encoderLibrary = encoderLibrary;
  }

  public URL getEncoderLibraryURL() {
    return encoderLibraryURL;
  }

  public void setEncoderLibraryURL(URL encoderLibraryURL) {
    this.encoderLibraryURL = encoderLibraryURL;
  }

  public String getEncoderLibraryInfo() {
    return encoderLibraryInfo;
  }

  public void setEncoderLibraryInfo(String encoderLibraryInfo) {
    this.encoderLibraryInfo = encoderLibraryInfo;
  }

  public String getEncoderLibraryVersion() {
    return encoderLibraryVersion;
  }

  public void setEncoderLibraryVersion(String encoderLibraryVersion) {
    this.encoderLibraryVersion = encoderLibraryVersion;
  }

  public String getEncoderLibraryReleaseDate() {
    return encoderLibraryReleaseDate;
  }

  public void setEncoderLibraryReleaseDate(String encoderLibraryReleaseDate) {
    this.encoderLibraryReleaseDate = encoderLibraryReleaseDate;
  }

  public String getEncoderLibrarySettings() {
    return encoderLibrarySettings;
  }

  public void setEncoderLibrarySettings(String encoderLibrarySettings) {
    this.encoderLibrarySettings = encoderLibrarySettings;
  }

  public Boolean isEncrypted() {
    return encrypted;
  }

  public void setEncrypted(Boolean encrypted) {
    this.encrypted = encrypted;
  }

  public Date getEncodedDate() {
    return encodedDate;
  }

  public void setEncodedDate(Date encodedDate) {
    this.encodedDate = encodedDate;
  }

  public Date getTaggedDate() {
    return taggedDate;
  }

  public void setTaggedDate(Date taggedDate) {
    this.taggedDate = taggedDate;
  }

  /**
   * Returns the title of the media object.
   */
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the locale of the media object, usually only the language.
   */
  public Locale getLanguage() {
    return language;
  }

  public void setLanguage(Locale language) {
    this.language = language;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public URL getEncoderApplicationVendor() {
    return encoderApplicationVendor;
  }

  public void setEncoderApplicationVendor(URL encoderApplicationVendor) {
    this.encoderApplicationVendor = encoderApplicationVendor;
  }

  public String getEncoderLibraryVendor() {
    return encoderLibraryVendor;
  }

  public void setEncoderLibraryVendor(String encoderLibraryVendor) {
    this.encoderLibraryVendor = encoderLibraryVendor;
  }

}
