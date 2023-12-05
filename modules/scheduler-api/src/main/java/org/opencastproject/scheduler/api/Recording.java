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
package org.opencastproject.scheduler.api;

/**
 * An in-memory construct to represent the state of a recording, and when it was last heard from.
 */
public interface Recording {

  /**
   * Gets the ID of the recording.
   *
   * @return The ID of the recording.
   */
  String getID();

  /**
   * Sets the state of the recording, and updates the time it was last heard from.
   *
   * @param newState
   *          The new state of the recording. This should be defined from
   *          {@link org.opencastproject.scheduler.api.RecordingState}. This can be equal to the current one if the
   *          goal is to update the timestamp.
   * @see RecordingState
   */
  void setState(String newState);

  /**
   * Gets the state of the recording.
   *
   * @return The state of the recording. This should be defined from
   *         {@link org.opencastproject.scheduler.api.RecordingState}.
   * @see RecordingState
   */
  String getState();

  /**
   * Gets the time at which the recording last checked in.
   *
   * @return The number of milliseconds since 1970 when the recording last checked in.
   */
  Long getLastCheckinTime();

}
