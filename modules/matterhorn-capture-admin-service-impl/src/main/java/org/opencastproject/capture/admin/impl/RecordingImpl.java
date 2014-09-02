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
package org.opencastproject.capture.admin.impl;

import org.opencastproject.capture.admin.api.Recording;

/**
 * An in-memory construct to represent the state of a recording, and when it was last heard from.
 */
public class RecordingImpl implements Recording {

  /**
   * The ID of the recording.
   */
  private String id;

  /**
   * The state of the recording. This should be defined from RecordingState.
   */
  private String state;

  /**
   * The time at which the recording last checked in with this service. Note that this is an absolute timestamp (ie,
   * milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in).
   */
  private Long lastHeardFrom;

  /**
   * Builds a representation of the recording.
   *
   * @param recordingID
   *          The ID of the recording.
   * @param recordingState
   *          The state of the recording. This should be defined from RecordingState.
   */
  public RecordingImpl(String recordingID, String recordingState) {
    id = recordingID;
    this.setState(recordingState);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Recording#getID()
   */
  public String getID() {
    return id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Recording#setState(java.lang.String)
   */
  public void setState(String newState) {
    state = newState;
    lastHeardFrom = System.currentTimeMillis();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Recording#getState()
   */
  public String getState() {
    return state;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Recording#getLastCheckinTime()
   */
  public Long getLastCheckinTime() {
    return lastHeardFrom;
  }
}
