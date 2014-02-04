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
package org.opencastproject.capture.pipeline.bins;

/**
 * Constant class that defines all of the GStreamerProperties that we use to prevent typos and increase readability.
 **/
public interface GStreamerProperties {
  /** General Properties **/
  String ASYNC = "async";
  String SINK = "sink";
  String SRC = "src";
  String SRCTEMPLATE = "src%d";
  String VIDEO = "video";
  String SYNC = "sync";
  String IS_LIVE = "is-live";

  /** Source Properties **/
  String DEVICE = "device";

  /** Queue and FileSink properties. **/
  String LOCATION = "location";

  /** Queue Properties **/
  String MAX_SIZE_BUFFERS = "max-size-buffers";
  String MAX_SIZE_BYTES = "max-size-bytes";
  String MAX_SIZE_TIME = "max-size-time";

  /** Encoder Properties **/
  String BITRATE = "bitrate";

  /** x264 Properties **/
  String INTERLACED = "interlaced";
  String NOISE_REDUCTION = "noise-reduction";
  String PASS = "pass";
  String PROFILE = "profile";
  String QP_MIN = "qp-min";
  String QP_MAX = "qp-max";
  String QUANTIZER = "quantizer";
  String SPEED_PRESET = "speed-preset";

  /** Caps Properties **/
  String AUDIO_X_RAW_INT = "audio/x-raw-int";
  String VIDEO_X_RAW_YUV = "video/x-raw-yuv";
  String FRAMERATE = "framerate";
  String CAPS = "caps";

  /** Video Test Src Properties **/
  String PATTERN = "pattern";
  String WIDTH = "width";
  String HEIGHT = "height";

  /** Other Properties **/
  String DO_TIMESTAP = "do-timestamp";
  String BLOCK = "block";
  String SINGLE_SEGMENT = "single-segment";
  String EMIT_SIGNALS = "emit-signals";
  String DROP = "drop";
  String MAX_BUFFERS = "max-buffers";
  String MESSAGE = "message";
  String INTERVAL = "interval";
}
