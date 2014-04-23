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

/**
 * VideoEditorService properties that can be used to modify default processing values.
 */
public interface VideoEditorProperties {

  /** Custom audio (pre encoder) caps */
  String AUDIO_CAPS = "audio.caps";
  /** Custom audio encoder (gstreamer element) */
  String AUDIO_ENCODER = "audio.encoder";
  /** Custom audio encoder settings */
  String AUDIO_ENCODER_PROPERTIES = "audio.encoder.properties";

  /** Custom video (pre encoder) caps */
  String VIDEO_CAPS = "video.caps";
  /** Custom video encoder (gstreamer element) */
  String VIDEO_ENCODER = "video.encoder";
  /** Custom video encoder settings */
  String VIDEO_ENCODER_PROPERTIES = "video.encoder.properties";

  /** Custom muxer */
  String MUX = "mux";
  /** Custom muxer settings */
  String MUX_PROPERTIES = "mux.properties";

  /** Custom output file extension */
  String OUTPUT_FILE_EXTENSION = "outputfile.extension";
}
