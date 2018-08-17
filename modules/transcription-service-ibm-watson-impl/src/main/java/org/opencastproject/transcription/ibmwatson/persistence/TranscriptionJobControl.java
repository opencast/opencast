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
package org.opencastproject.transcription.ibmwatson.persistence;

import java.util.Date;

public final class TranscriptionJobControl {
  public enum Status {
    Progress, Canceled, Error, TranscriptionComplete, Closed
  }

  // Media package id
  private String mediaPackageId;
  // Id of audio track element sent to service
  private String trackId;
  // This is the id of the ibm watson service job
  private String transcriptionJobId;
  // Transcription status, only completed after workflow to attach transcripts is dispatched
  private String status;
  // Date/time of ibm watson job creation
  private Date dateCreated;
  // Date/time of ibm watson job completion
  private Date dateCompleted;
  // Duration of track
  private long trackDuration;

  public TranscriptionJobControl(String mediaPackageId, String trackId, String transcriptionJobId, Date dateCreated,
          Date dateCompleted, String status, long trackDuration) {
    super();
    this.mediaPackageId = mediaPackageId;
    this.trackId = trackId;
    this.transcriptionJobId = transcriptionJobId;
    this.dateCreated = dateCreated;
    this.dateCompleted = dateCompleted;
    this.status = status;
    this.trackDuration = trackDuration;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public void setMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }

  public String getTrackId() {
    return trackId;
  }

  public void setTrackId(String trackId) {
    this.trackId = trackId;
  }

  public String getTranscriptionJobId() {
    return transcriptionJobId;
  }

  public void setTranscriptionJobId(String transcriptionJobId) {
    this.transcriptionJobId = transcriptionJobId;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  public Date getDateCompleted() {
    return dateCompleted;
  }

  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public long getTrackDuration() {
    return trackDuration;
  }

  public void setTrackDuration(long trackDuration) {
    this.trackDuration = trackDuration;
  }

}
