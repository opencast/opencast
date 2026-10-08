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
package org.opencastproject.basicstatistics;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Type of a raw statistics event.
 *
 * Each type has a database ID, a fixed integer that is used for storage, and an API name, which is used in the HTTP
 * APIs. The Java constant names are neither, so they can be renamed freely.
 *
 * Assigned database IDs must never be changed or reused, as that would require a database migration. Changing an API
 * name is a breaking API change.
 */
public enum EventType {
  PAGE_VISIT(1, "page-visit", EventPayload.PageVisit.class), // a page dedicated to this item was opened.
  VIDEO_PLAY(2, "video:play", null), // user has clicked "play" on a video to start watching
  VIDEO_PAUSE(3, "video:pause", EventPayload.VideoPause.class), // user has paused video playback
  VIDEO_RESUME(4, "video:resume", EventPayload.VideoResume.class), // user has resumed video playback
  VIDEO_SEEK(5, "video:seek", EventPayload.VideoSeek.class), // user jumped to somewhere in the video.
  VIDEO_WATCHED(6, "video:watched", EventPayload.VideoWatched.class), // the user has fully watched part of the video
  FETCH_FILE(7, "fetch-file", EventPayload.FetchFile.class); // a file was (partially) downloaded

  private static final Map<Short, EventType> BY_DB_ID = Arrays.stream(values())
      .collect(Collectors.toMap(EventType::getDbId, Function.identity()));
  private static final Map<String, EventType> BY_API_NAME = Arrays.stream(values())
      .collect(Collectors.toMap(EventType::getApiName, Function.identity()));

  private final short dbId;
  private final String apiName;
  private final Class<? extends EventPayload> payloadType;

  EventType(int dbId, String apiName, Class<? extends EventPayload> payloadType) {
    this.dbId = (short) dbId;
    this.apiName = apiName;
    this.payloadType = payloadType;
  }

  public short getDbId() {
    return dbId;
  }

  public String getApiName() {
    return apiName;
  }

  /**
   * @return the class of the payload events of this type carry, or empty if they have none
   */
  public Optional<Class<? extends EventPayload>> getPayloadType() {
    return Optional.ofNullable(payloadType);
  }

  /**
   * @return the type with the given database ID, or empty if there is none
   */
  public static Optional<EventType> fromDbId(short dbId) {
    return Optional.ofNullable(BY_DB_ID.get(dbId));
  }

  /**
   * @return the type with the given API name, or empty if there is none
   */
  public static Optional<EventType> fromApiName(String apiName) {
    return Optional.ofNullable(BY_API_NAME.get(apiName));
  }
}
