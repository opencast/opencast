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
package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.util.EqualsUtil;

/**
 * Business object for recording synchronization log
 */
public class SynchronizedRecording {

  /**
   * The possible synchronization status
   */
  public enum SynchronizationStatus {
    NEW, UPDATE, DELETION
  }

  /** The recording synchronization log id */
  private Long id;

  /** The status of the synchronized object */
  private SynchronizationStatus status;

  /** The synchronized recording */
  private Recording recording;

  /**
   * Creates a synchronization log for a recording
   * 
   * @param status
   *          the status
   * @param recording
   *          the recording
   */
  public SynchronizedRecording(SynchronizationStatus status, Recording recording) {
    this.status = status;
    this.recording = notNull(recording, "recording");
  }

  /**
   * Creates a synchronization log for a recording
   * 
   * @param id
   *          the id
   * @param status
   *          the status
   * @param recording
   *          the recording
   */
  public SynchronizedRecording(Long id, SynchronizationStatus status, Recording recording) {
    this(status, recording);
    this.id = id;
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the id of the recording synchronization log
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the id of the recording synchronization log
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the synchronization log status
   * 
   * @param status
   *          the synchronization log status
   */
  public void setStatus(SynchronizationStatus status) {
    this.status = status;
  }

  /**
   * Returns the synchronization log status
   * 
   * @return the synchronization log status
   */
  public SynchronizationStatus getStatus() {
    return this.status;
  }

  /**
   * Sets the recording related to this synchronization log
   * 
   * @param recording
   *          the recording
   */
  public void setRecording(Recording recording) {
    this.recording = notNull(recording, "recording");
  }

  /**
   * Returns the recording related to this synchronization log
   * 
   * @return the recording
   */
  public Recording getRecording() {
    return this.recording;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SynchronizedRecording sync = (SynchronizedRecording) o;
    return status.equals(sync.getStatus()) && recording.equals(sync.getRecording());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, status, recording);
  }

}
