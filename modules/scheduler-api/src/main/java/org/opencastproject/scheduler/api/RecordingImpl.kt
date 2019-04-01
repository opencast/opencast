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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.scheduler.api


/**
 * An in-memory construct to represent the state of a recording, and when it was last heard from.
 */
class RecordingImpl : Recording {

    /**
     * The ID of the recording.
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.scheduler.api.Recording.getID
     */
    override var id: String? = null
        private set

    /**
     * The state of the recording. This should be defined from RecordingState.
     */
    private var state: String? = null

    /**
     * The time at which the recording last checked in with this service. Note that this is an absolute timestamp (ie,
     * milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in).
     */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.scheduler.api.Recording.getLastCheckinTime
     */
    override var lastCheckinTime: Long? = null
        private set

    /**
     * Builds a representation of the recording.
     *
     * @param recordingID
     * The ID of the recording.
     * @param recordingState
     * The state of the recording. This should be defined from RecordingState.
     * @param lastHeard
     * The time at which the recording last checked in
     */
    constructor(recordingID: String, recordingState: String, lastHeard: Long) {
        id = recordingID
        state = recordingState
        lastCheckinTime = lastHeard
    }

    /**
     * Builds a representation of the recording.
     *
     * @param recordingID
     * The ID of the recording.
     * @param recordingState
     * The state of the recording. This should be defined from RecordingState.
     */
    constructor(recordingID: String, recordingState: String) {
        id = recordingID
        this.setState(recordingState)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.scheduler.api.Recording.setState
     */
    override fun setState(newState: String) {
        state = newState
        lastCheckinTime = System.currentTimeMillis()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.scheduler.api.Recording.getState
     */
    override fun getState(): String? {
        return state
    }
}
