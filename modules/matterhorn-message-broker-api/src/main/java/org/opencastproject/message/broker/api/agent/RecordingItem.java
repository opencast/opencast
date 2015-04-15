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
package org.opencastproject.message.broker.api.agent;

import java.io.Serializable;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a recording queue.
 */
public class RecordingItem implements Serializable {

  private static final long serialVersionUID = -3213675986929522484L;

  public static final String RECORDING_QUEUE_PREFIX = "RECORDING.";

  public static final String RECORDING_QUEUE = RECORDING_QUEUE_PREFIX + "QUEUE";

  private final String eventId;
  private final String state;
  private final Long lastHeardFrom;
  private final Type type;

  public enum Type {
    Update, Delete
  };

  /**
   * @param eventId
   *          The event id
   * @param state
   *          The recording state
   * @param lastHeardFrom
   *          The recording last heard from date
   * @return Builds {@link RecordingItem} for updating a recording.
   */
  public static RecordingItem updateRecording(String eventId, String state, Long lastHeardFrom) {
    return new RecordingItem(eventId, state, lastHeardFrom);
  }

  /**
   * @param eventId
   *          The unique id of the recording to delete.
   * @return Builds {@link RecordingItem} for deleting a recording.
   */
  public static RecordingItem delete(String eventId) {
    return new RecordingItem(eventId);
  }

  /**
   * Constructor to build an update recording {@link RecordingItem}.
   *
   * @param eventId
   *          The event id
   * @param state
   *          The recording state
   * @param lastHeardFrom
   *          The recording last heard from date
   */
  public RecordingItem(String eventId, String state, Long lastHeardFrom) {
    this.eventId = eventId;
    this.state = state;
    this.lastHeardFrom = lastHeardFrom;
    this.type = Type.Update;
  }

  /**
   * Constructor to build a delete recording {@link RecordingItem}.
   *
   * @param eventId
   *          The id of the recording to delete.
   */
  public RecordingItem(String eventId) {
    this.eventId = eventId;
    this.state = null;
    this.lastHeardFrom = null;
    this.type = Type.Delete;
  }

  public String getEventId() {
    return eventId;
  }

  public String getState() {
    return state;
  }

  public Long getLastHeardFrom() {
    return lastHeardFrom;
  }

  public Type getType() {
    return type;
  }

}
