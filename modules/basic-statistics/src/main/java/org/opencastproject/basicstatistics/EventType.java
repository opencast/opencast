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

public enum EventType {
  PAGE_VISIT("PV"), // a page dedicated to this item was opened.
  VIDEO_PLAY("V:PLAY"), // user has clicked "play" on a video to start watching
  VIDEO_PAUSE("V:PAUSE"), // user has paused video playback
  VIDEO_RESUME("V:RESUME"), // user has resumed video playback
  VIDEO_SEEK("V:SEEK"), // user jumped to somewhere in the video.
  VIDEO_WATCHED("V:WATCHED"), // the user has fully watched part of the video
  FETCH_FILE("FF"); // a file was (partially) downloaded

  private String code;

  EventType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
