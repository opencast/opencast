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
package org.opencastproject.videoeditor.impl;

public class VideoClip {
  private final int srcId;
  private final double start;
  private double end;
  private String region; // if layout regions are supported, it will be resolved and stored here, defaults to root layout

  public VideoClip(int indx, double start, double end) {
    this.srcId = indx;
    this.start = start;
    this.end = end;
  }

  void setEnd(double newend) {
    this.end = newend;
  }

  void setRegion(String region) { // Regions are relative to root-layout,
    this.region = region;
  }

  public int getSrc() { return srcId; }
  public double getStart() { return start; }
  public double getEnd() { return end; }
  public String getRegion() { return region; }
  public double getDuration() { return end - start; }

}
