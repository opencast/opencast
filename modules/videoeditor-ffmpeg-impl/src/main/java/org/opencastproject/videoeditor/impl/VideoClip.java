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

package org.opencastproject.videoeditor.impl;

public class VideoClip {
  private final int srcId;
  private final long start;
  private long end;
  // if layout regions are supported, it will be resolved and stored here,
  // defaults to root layout
  private String region;

  /**
   * Video clip constructor.
   *
   * @param id Source identifier of the video clip
   * @param start Start time in milliseconds
   * @param end End time in milliseconds
   */
  public VideoClip(int id, long start, long end) {
    this.srcId = id;
    this.start = start;
    this.end = end;
  }

  /**
   * Update the video clip's end time.
   *
   * @param end New end time in milliseconds.
   */
  void setEnd(long end) {
    this.end = end;
  }

  void setRegion(String region) { // Regions are relative to root-layout,
    this.region = region;
  }

  public int getSrc() {
    return srcId;
  }

  /**
   * Get the video clip's start time in milliseconds.
   *
   * @return Start time in milliseconds.
   */
  public long getStartInMilliseconds() {
    return start;
  }

  /**
   * Get the video clip's start time in fractions of seconds.
   *
   * @return Start time in seconds.
   */
  public double getStartInSeconds() {
    return start / 1000.0;
  }

  /**
   * Get the video clip's end time in milliseconds.
   *
   * @return End time in milliseconds.
   */
  public long getEndInMilliseconds() {
    return end;
  }

  /**
   * Get the video clip's end time in fractions of seconds.
   *
   * @return End time in seconds.
   */
  public double getEndInSeconds() {
    return end / 1000.0;
  }

  public String getRegion() {
    return region;
  }

  /**
   * Get the video clip's duration in milliseconds.
   *
   * @return Duration in milliseconds.
   */
  public long getDurationInMilliseconds() {
    return end - start;
  }

  /**
   * Get the video clip's duration in fractions of seconds.
   *
   * @return Duration in seconds.
   */
  public double getDurationInSeconds() {
    return (end - start) / 1000.0;
  }

}
