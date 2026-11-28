/*
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
package org.opencastproject.liveschedule.api;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;

public interface LiveScheduleService {
  String CHANNEL_ID = "engage-live";

  /** Workflow configuration key marking a scheduled event as live */
  String PUBLISH_LIVE_PROPERTY = "publishLive";

  boolean createOrUpdateLiveEvent(String mpId, DublinCoreCatalog episodeDc) throws LiveScheduleException;

  boolean deleteLiveEvent(String mpId) throws LiveScheduleException;

  /**
   * Notify the service that the recording state of a live event has changed. The live publication is retracted once
   * the capture is over.
   *
   * @param mpId
   *          the media package identifier
   * @param recordingState
   *          the new recording state, see {@code org.opencastproject.scheduler.api.RecordingState}
   * @return true if the live publication has been retracted
   * @throws LiveScheduleException
   *           if retracting the live publication failed
   */
  boolean updateLiveEventRecordingState(String mpId, String recordingState) throws LiveScheduleException;

}
