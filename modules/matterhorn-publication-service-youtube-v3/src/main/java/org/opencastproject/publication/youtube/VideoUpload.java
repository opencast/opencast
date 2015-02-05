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
package org.opencastproject.publication.youtube;

import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

import java.io.File;

/**
 * Represents a YouTube video.
 *
 * @see com.google.api.services.youtube.model.Video
 * @author John Crossman
 */
public class VideoUpload {

  private final String title;
  private final String description;
  private final String privacyStatus;
  private final File videoFile;
  private final MediaHttpUploaderProgressListener progressListener;
  private final String[] tags;

  /**
   * @param title may not be {@code null}.
   * @param description may be {@code null}.
   * @param privacyStatus may not be {@code null}.
   * @param videoFile may not be {@code null}.
   * @param progressListener may be {@code null}.
   * @param tags may be {@code null}.
   */
  public VideoUpload(final String title, final String description, final String privacyStatus, final File videoFile, final MediaHttpUploaderProgressListener progressListener, final String... tags) {
    this.title = title;
    this.description = description;
    this.privacyStatus = privacyStatus;
    this.videoFile = videoFile;
    this.progressListener = progressListener;
    this.tags = tags;
  }

  /**
   * The video's title.
   * The value will not be {@code null}.
   */
  public String getTitle() {
    return title;
  }

  /**
   * The video's description.
   * The value may be {@code null}.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @see com.google.api.services.youtube.model.VideoStatus#setPrivacyStatus(String)
   * @return will not be {@code null}
   */
  public String getPrivacyStatus() {
    return privacyStatus;
  }

  /**
   * @see com.google.api.services.youtube.model.Video
   * @return will not be {@code null}
   */
  public File getVideoFile() {
    return videoFile;
  }

  /**
   * Real-time updates of upload status.
   * @return may be {@code null}
   */
  public MediaHttpUploaderProgressListener getProgressListener() {
    return progressListener;
  }

  /**
   * @see com.google.api.services.youtube.model.VideoSnippet#getTags()
   * @return may be {@code null}
   */
  public String[] getTags() {
    return tags;
  }
}
