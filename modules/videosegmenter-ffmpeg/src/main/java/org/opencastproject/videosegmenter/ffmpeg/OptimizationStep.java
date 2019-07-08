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

package org.opencastproject.videosegmenter.ffmpeg;

import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Segment;

import java.util.LinkedList;

/**
 * An optimization step is one step in the optimization of the number of segments.
 * This class stores parameters of such an optimization step and calculates error and
 * absolute error of optimization
 *
 */
public class OptimizationStep implements Comparable<OptimizationStep> {

  private float changesThreshold;
  private float error;
  private float errorAbs;
  private int segmentNum;
  private int prefNum;
  private Mpeg7Catalog mpeg7;
  private LinkedList<Segment> segments;

  /**
   * creates a new optimization step with given parameters
   *
   * @param changesThreshold
   * @param segNum
   * @param prefNum
   * @param mpeg7
   * @param segments unfiltered list of segments
   */
  public OptimizationStep(float changesThreshold, int segNum, int prefNum, Mpeg7Catalog mpeg7,
          LinkedList<Segment> segments) {
    this.changesThreshold = changesThreshold;
    this.segmentNum = segNum;
    this.prefNum = prefNum;
    this.mpeg7 = mpeg7;
    this.segments = segments;
    calcErrors();
  }

  /**
   *  creates a new optimization step with default values
   */
  public OptimizationStep() {
    changesThreshold = 0.0f;
    segmentNum = 1;
    prefNum = 1;
    mpeg7 = null;
    segments = null;
    calcErrors();
  }

  /**
   * calculate error of optimization and absolute error of optimization
   */
  private void calcErrors() {
    error = (float)(segmentNum - prefNum) / (float)prefNum;
    errorAbs = Math.abs(error);
  }

  /**
   * get changesThreshold
   *
   * @return changesThreshold
   */
  public float getChangesThreshold() {
    return changesThreshold;
  }

  /**
   * get error of optimization
   *
   * @return error error of optimization
   */
  public float getError() {
    return error;
  }

  /**
   * get absolute error
   *
   * @return errorAbs absolute error
   */
  public float getErrorAbs() {
    return errorAbs;
  }

  /**
   * get number of segments
   *
   * @return segmentNum number of segments
   */
  public int getSegmentNum() {
    return segmentNum;
  }

  /**
   * set number of segments
   *
   * @param segNum number of segments
   */
  public void setSegmentNumAndRecalcErrors(int segNum) {
    segmentNum = segNum;
    calcErrors();
  }

  /**
   * get Mpeg7Catalog with segments
   *
   * @return mpeg7 Mpeg7Catalog with segments
   */
  public Mpeg7Catalog getMpeg7() {
    return mpeg7;
  }

  /**
   * get list of segments
   *
   * @return segments  list of segments
   */
  public LinkedList<Segment> getSegments() {
    return segments;
  }

  /**
   * calculates error from given number of segments and preferred number of
   * segments
   *
   * @param segmentNum number of segments
   * @param prefNum preferred number of segments
   * @return
   */
  public static float calculateError(int segmentNum, int prefNum) {
    return (float)(segmentNum - prefNum) / (float)prefNum;
  }

  /**
   * calculates absolute error from given number of segments and preferred
   * number of segments
   *
   * @param segmentNum number of segments
   * @param prefNum preferred number of segments
   * @return
   */
  public static float calculateErrorAbs(int segmentNum, int prefNum) {
    return Math.abs((float)(segmentNum - prefNum) / (float)prefNum);
  }

/**
 * With this method a list of OptimizationSteps can be sorted such that the smallest
 * positive error is the first element of the list and the smallest negative
 * error is the last element of the list
 *
 * @param o the OptimizationStep to be compared.
 * @return a negative integer or a positive integer as this OptimizationStep should be placed to the left or right of
 * the specified OptimizationStep or zero if their errors are equal.
 */
  @Override
  public int compareTo(OptimizationStep o) {
    if (error == o.getError()) {
      return 0;
    }
    // positive
    if (error >= 0) {
      // if other error is negative put to the left of it
      if (o.getError() < 0) {
        return -1;
      } else {
        // if other error is also positive, compare errors, so that smaller positive error will be left
        if (error < o.getError()) {
          return -1;
        } else {
          return 1;
        }
      }
    // negative
    } else {
      // if other error is positive put to the right of it
      if (o.getError() >= 0) {
        return 1;
      } else {
        // if other error is also negative, compare errors, so that smaller negative error will be right
        if (error < o.getError()) {
          return -1;
        } else {
          return 1;
        }
      }
    }
  }
}
