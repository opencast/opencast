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

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Log progress of a YouTube video upload.
 *
 * @author John Crossman
 */
public class UploadProgressListener implements MediaHttpUploaderProgressListener {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final File file;
  private final String episodeName;
  private final String seriesTitle;
  private boolean complete;

  /**
   * @param episodeName may not be {@code null}
   * @param seriesTitle may be {@code null}
   * @param file may not be {@code null}
   */
  public UploadProgressListener(final String episodeName, final String seriesTitle, final File file) {
    this.file = file;
    complete = false;
    this.episodeName = episodeName;
    this.seriesTitle = seriesTitle;
  }

  @Override
  public void progressChanged(final MediaHttpUploader uploader) throws IOException {
    final MediaHttpUploader.UploadState uploadState = uploader.getUploadState();
    final String describeProgress;
    switch (uploadState) {
      case INITIATION_STARTED:
        describeProgress = "Initiating YouTube publish";
        break;
      case INITIATION_COMPLETE:
      case MEDIA_IN_PROGRESS:
        final String percentComplete = "%" + uploader.getProgress() * 100 + " complete";
        describeProgress = "Uploading " + file.getAbsolutePath() + " to YouTube (" + percentComplete + ")";
        break;
      case NOT_STARTED:
        describeProgress = "Waiting to start YouTube.";
        break;
      case MEDIA_COMPLETE:
        describeProgress = "YouTube publication is complete.";
        complete = true;
        break;
      default:
        describeProgress = "Warning: No formal description for upload state: " + uploadState;
    }
    logger.info(describeProgress + "(Episode name: " + episodeName + "; Series title: " + seriesTitle + ')');
  }

  public boolean isComplete() {
    return complete;
  }

}
