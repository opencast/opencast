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

/**
 * The event data (payload) of a raw event. Each event type with a payload has its own record, which
 * validates its fields on construction. A payload object that exists is therefore always valid.
 */
public interface EventPayload {

  /** A page dedicated to an item was opened. */
  record PageVisit(String url) implements EventPayload {
    public PageVisit {
      require(url != null, "missing 'url'");
    }
  }

  /** The user paused video playback. */
  record VideoPause(Long at) implements EventPayload {
    public VideoPause {
      require(at != null && at >= 0, "invalid 'at'");
    }
  }

  /** The user resumed video playback. */
  record VideoResume(Long at) implements EventPayload {
    public VideoResume {
      require(at != null && at >= 0, "invalid 'at'");
    }
  }

  /** The user jumped to somewhere in the video. */
  record VideoSeek(Long to) implements EventPayload {
    public VideoSeek {
      require(to != null && to >= 0, "invalid 'to'");
    }
  }

  /** The user has fully watched part of the video. */
  record VideoWatched(Long from, Long to) implements EventPayload {
    public VideoWatched {
      require(from != null && from >= 0, "invalid 'from'");
      require(to != null && to >= from, "invalid 'to'");
    }
  }

  /**
   * A file was (partially) downloaded.
   *
   * @param elem (file) element ID, which is the path segment after the video ID
   * @param from start of the byte range of what was downloaded. Non-range-requests specify 0.
   * @param to end of the byte range of what was downloaded. null if the request does not specify an end
   */
  record FetchFile(String elem, Long from, Long to) implements EventPayload {
    public FetchFile {
      require(elem != null, "missing 'elem'");
      require(from != null && from >= 0, "invalid 'from'");
      require(to == null || to >= from, "invalid 'to'");
    }
  }

  private static void require(boolean ok, String message) {
    if (!ok) {
      throw new IllegalArgumentException(message);
    }
  }
}
