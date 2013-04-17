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
package org.opencastproject.deliver.youtube;

/**
 * Singleton with YouTube configuration information.
 */
public final class YouTubeConfiguration {

  private static YouTubeConfiguration configuration = new YouTubeConfiguration();

  public static YouTubeConfiguration getInstance() {
    return configuration;
  }

  private YouTubeConfiguration() {
  }

  /** Application client id. */
  private String clientId;

  /** Developer key. */
  private String developerKey;

  /** YouTube user id used to upload video. */
  private String userId;

  /** Password associated with id. */
  private String password;

  /** Default category for uploads. */
  private String category;

  /** Default keywords for uploads. */
  private String keywords;

  /** URL of upload service. */
  private String uploadUrl;

  /** Upload video as private. */
  private boolean videoPrivate;

  // **** Accessors

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getDeveloperKey() {
    return developerKey;
  }

  public void setDeveloperKey(String developerKey) {
    this.developerKey = developerKey;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }

  public String getUploadUrl() {
    return uploadUrl;
  }

  public void setUploadUrl(String uploadUrl) {
    this.uploadUrl = uploadUrl;
  }

  public boolean isVideoPrivate() {
    return videoPrivate;
  }

  public void setVideoPrivate(boolean videoPrivate) {
    this.videoPrivate = videoPrivate;
  }

  // **** String Representation

  @Override
  public String toString() {
    return "YouTubeConfiguration{" + "client_id='" + clientId + '\'' + ", developer_key='" + developerKey + '\''
            + ", user_id='" + userId + '\'' + ", password='" + password + '\'' + ", category='" + category + '\''
            + ", upload_url='" + uploadUrl + '\'' + ", video_private=" + videoPrivate + "}";
  }
}
