/**
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


package org.opencastproject.inspection.ffmpeg.api;


import org.opencastproject.util.MimeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates technical metadata of media containers, usually media files.
 * <p>
 * Each property may return null, which means that it could not be determined.
 */
public class MediaContainerMetadata extends TemporalMetadata {

  private List<VideoStreamMetadata> vMetadata = new ArrayList<VideoStreamMetadata>();
  private List<AudioStreamMetadata> aMetadata = new ArrayList<AudioStreamMetadata>();

  private String fileName;
  private String fileExtension;
  private Boolean interleaved;
  private MimeType mimeType;
  private Boolean adaptiveMaster;

  /**
   * Returns metadata for all contained video streams.
   *
   * @return the metadata or an empty list
   */
  public List<VideoStreamMetadata> getVideoStreamMetadata() {
    return vMetadata;
  }

  /**
   * Returns metadata for all contained audio streams.
   *
   * @return the metadata or an empty list
   */
  public List<AudioStreamMetadata> getAudioStreamMetadata() {
    return aMetadata;
  }

  /**
   * Checks if any video metadata is present.
   */
  public boolean hasVideoStreamMetadata() {
    return vMetadata.size() > 0;
  }

  /**
   * Checks if any audio metadata is present.
   */
  public boolean hasAudioStreamMetadata() {
    return aMetadata.size() > 0;
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Returns the file name, e.g. <code>metropolis.mov</code>
   */
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Returns the file extension, e.g. <code>mov</code>
   */
  public String getFileExtension() {
    return fileExtension;
  }

  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  /**
   * Checks if contained audio and video streams are multiplexed.
   */
  public Boolean isInterleaved() {
    return interleaved;
  }

  public void setInterleaved(Boolean interleaved) {
    this.interleaved = interleaved;
  }

  /**
   * Returns the mimeType of the file
   * 
   * @return mimetype
   */
  public MimeType getMimeType() {
    return mimeType;
  }

  public void setMimeType(MimeType mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Looks for adaptive master file
   * 
   * @return true if this is an adaptiveMaster file
   */
  public Boolean getAdaptiveMaster() {
    return adaptiveMaster;
  }

  public void setAdaptiveMaster(Boolean adaptiveMaster) {
    this.adaptiveMaster = adaptiveMaster;
  }

}
