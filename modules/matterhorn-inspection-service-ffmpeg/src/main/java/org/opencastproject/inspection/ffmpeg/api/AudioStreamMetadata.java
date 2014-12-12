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


/**
 * This class bundles technical information about an audio stream.
 */
public class AudioStreamMetadata extends StreamMetadata {

  private Integer channels;
  private String channelPositions;

  // Hz
  private Integer samplingRate;

  private Long samplingCount;

  private Float replayGain;
  private Float replayGainPeak;

  private Float interleaveVideoFrames;
  // ms
  private Integer interleaveDuration;
  // ms
  private Integer interleavePreload;

  public Integer getChannels() {
    return channels;
  }

  public void setChannels(Integer channels) {
    this.channels = channels;
  }

  public String getChannelPositions() {
    return channelPositions;
  }

  public void setChannelPositions(String channelPositions) {
    this.channelPositions = channelPositions;
  }

  public Integer getSamplingRate() {
    return samplingRate;
  }

  public void setSamplingRate(Integer samplingRate) {
    this.samplingRate = samplingRate;
  }

  public Long getSamplingCount() {
    return samplingCount;
  }

  public void setSamplingCount(Long samplingCount) {
    this.samplingCount = samplingCount;
  }

  public Float getReplayGain() {
    return replayGain;
  }

  public void setReplayGain(Float replayGain) {
    this.replayGain = replayGain;
  }

  public Float getReplayGainPeak() {
    return replayGainPeak;
  }

  public void setReplayGainPeak(Float replayGainPeak) {
    this.replayGainPeak = replayGainPeak;
  }

  public Float getInterleaveVideoFrames() {
    return interleaveVideoFrames;
  }

  public void setInterleaveVideoFrames(Float interleaveVideoFrames) {
    this.interleaveVideoFrames = interleaveVideoFrames;
  }

  public Integer getInterleaveDuration() {
    return interleaveDuration;
  }

  public void setInterleaveDuration(Integer interleaveDuration) {
    this.interleaveDuration = interleaveDuration;
  }

  public Integer getInterleavePreload() {
    return interleavePreload;
  }

  public void setInterleavePreload(Integer interleavePreload) {
    this.interleavePreload = interleavePreload;
  }

}
