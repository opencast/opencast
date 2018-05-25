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
package org.opencastproject.composer.api;

import java.util.Comparator;

/* #DCE OPC-29
 * This is used to drive processEdit/processSmil after editing
 */

/*
 * This structure is used to build a list of clips for concatenation, track is the src video/audio and
 * srcId is a bit redundant, but needed for convenience.
 * By default, it is all video
 */
// #DCE added natural ordering by start time
public class VideoClip  implements Comparable<VideoClip>, Comparator<VideoClip> {
  private final int srcId;
  private final long start;
  private long end;

  /* indx is the order of the input src tracks */
  public VideoClip(int indx, double start, double end) {
    this.srcId = indx; // optional if only one clip, use track -> trim!
    this.start = (long) (start * 1000);
    this.end = (long) (end * 1000);
  }

  public VideoClip(int indx, long start, long end) {
    this.srcId = indx; // optional if only one clip, use track -> trim!
    this.start = start;
    this.end = end;
  }

  public void setEnd(double newend) {
    this.end = (long) (newend * 1000.0);
  }

  public void setEnd(long newend) {
    this.end = newend;
  }

  public int getSrc() {
    return srcId;
  }

  public double getStart() {
    return start / 1000.0;
  }

  public double getEnd() {
    return end / 1000.0;
  }

  public long getStartMS() {
    return start;
  }

  public long getEndMS() {
    return end;
  }

  public double getDuration() {
    return (end - start) / 1000.0;
  }

  public long getDurationMS() {
    return end - start;
  }


  @Override
  public String toString() {
    return "VideoClip [srcId=" + srcId + ", start=" + start + ", end=" + end + "]";
  }

  // [MATT-2095-stat-s-139-lecture-2-error-res] #DCE - Natural order of the clips is by start time
  @Override
  public int compareTo(VideoClip other) {
    return (int) (start - other.getStartMS());
  }

  @Override
  public int compare(VideoClip o1, VideoClip o2) {
    return (int) (o1.getStartMS() - o2.getStartMS());
  }

}
