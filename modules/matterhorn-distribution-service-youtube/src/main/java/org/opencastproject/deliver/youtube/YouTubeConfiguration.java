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

public class YouTubeConfiguration {

    private static YouTubeConfiguration configuration =
            new YouTubeConfiguration();

    public static YouTubeConfiguration getInstance() {
        return configuration;
    }

    private YouTubeConfiguration() {
    }

    /** Application client id. */
    private String client_id;

    /** Developer key. */
    private String developer_key;

    /** YouTube user id used to upload video. */
    private String user_id;

    /** Password associated with id. */
    private String password;

    /** Default category for uploads. */
    private String category;

    /** URL of upload service. */
    private String upload_url;

    /** Upload video as private. */
    private boolean video_private;

    // **** Accessors

    public String getClientId() {
        return client_id;
    }

    public void setClientId(String client_id) {
        this.client_id = client_id;
    }

    public String getDeveloperKey() {
        return developer_key;
    }

    public void setDeveloperKey(String developer_key) {
        this.developer_key = developer_key;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String user_id) {
        this.user_id = user_id;
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

    public String getUploadUrl() {
        return upload_url;
    }

    public void setUploadUrl(String upload_url) {
        this.upload_url = upload_url;
    }

    public boolean isVideoPrivate() {
        return video_private;
    }

    public void setVideoPrivate(boolean video_private) {
        this.video_private = video_private;
    }

    // **** String Representation


    @Override
    public String toString() {
        return "YouTubeConfiguration{" +
                "client_id='" + client_id + '\'' +
                ", developer_key='" + developer_key + '\'' +
                ", user_id='" + user_id + '\'' +
                ", password='" + password + '\'' +
                ", category='" + category + '\'' +
                ", upload_url='" + upload_url + '\'' +
                ", video_private=" + video_private +
                "}";
    }
}
