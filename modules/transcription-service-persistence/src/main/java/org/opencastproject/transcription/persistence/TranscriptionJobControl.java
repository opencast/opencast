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
package org.opencastproject.transcription.persistence;

import java.util.Date;

public class TranscriptionJobControl {

  public enum Status {
    InProgress, Canceled, Error, TranscriptionComplete, Closed, Retry
  }

  // Media package id
  private String mediaPackageId;
  // Id of audio track element sent to service
  private String trackId;
  // This is the name of the submitted google speech operation(job)
  private String transcriptionJobId;
  // Transcription status, only completed after workflow to attach transcripts is dispatched
  private String status;
  // Date/time of google speech job creation
  private Date dateCreated;
  // Date/time that the transcription job is expected to be complete
  private Date dateExpected;
  // Date/time of google speech job completion
  private Date dateCompleted;
  // Duration of track
  private long trackDuration;
  // Transcription provider Id
  private long providerId;

  public TranscriptionJobControl(String mediaPackageId, String trackId, String transcriptionJobId, Date dateCreated,
          Date dateExpected, Date dateCompleted, String status, long trackDuration, long providerId) {
    super();
    this.mediaPackageId = mediaPackageId;
    this.trackId = trackId;
    this.transcriptionJobId = transcriptionJobId;
    this.dateCreated = dateCreated;
    this.dateExpected = dateExpected;
    this.dateCompleted = dateCompleted;
    this.status = status;
    this.trackDuration = trackDuration;
    this.providerId = providerId;
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

  public Date getDateExpected() {
    return dateExpected;
  }

  public void setDateExpected(Date dateExpected) {
    this.dateExpected = dateExpected;
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

  public long getProviderId() {
    return providerId;
  }

  public void setProviderId(long providerId) {
    this.providerId = providerId;
  }

}
