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

package org.opencastproject.inspection.ffmpeg.api;

import org.opencastproject.mediapackage.track.BitRateMode;

/**
 * Common metadata for all kind of temporal media.
 */
public abstract class TemporalMetadata extends CommonMetadata {

  // ms
  protected Long duration;

  protected BitRateMode bitRateMode;
  // b/s
  protected Float bitRate;
  protected Float bitRateMinimum;
  protected Float bitRateMaximum;
  protected Float bitRateNominal;

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public BitRateMode getBitRateMode() {
    return bitRateMode;
  }

  public void setBitRateMode(BitRateMode bitRateMode) {
    this.bitRateMode = bitRateMode;
  }

  /** Returns the bit rate in bits per second. */
  public Float getBitRate() {
    return bitRate;
  }

  public void setBitRate(Float bitRate) {
    this.bitRate = bitRate;
  }

  /** Returns the maximum bit rate in bits per second. */
  public Float getBitRateMinimum() {
    return bitRateMinimum;
  }

  public void setBitRateMinimum(Float bitRateMinimum) {
    this.bitRateMinimum = bitRateMinimum;
  }

  public Float getBitRateMaximum() {
    return bitRateMaximum;
  }

  public void setBitRateMaximum(Float bitRateMaximum) {
    this.bitRateMaximum = bitRateMaximum;
  }

  /**
   * Returns the nominal bit rate in bits per second.
   */
  public Float getBitRateNominal() {
    return bitRateNominal;
  }

  public void setBitRateNominal(Float bitRateNominal) {
    this.bitRateNominal = bitRateNominal;
  }
}
