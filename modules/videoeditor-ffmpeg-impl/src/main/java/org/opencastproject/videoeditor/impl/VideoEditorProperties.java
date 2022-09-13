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

package org.opencastproject.videoeditor.impl;

/**
 * VideoEditorService properties that can be used to modify default processing values.
 */
public interface VideoEditorProperties {

  /** audio encoder codec */
  String AUDIO_CODEC = "audio.codec";

  /** video codec */
  String VIDEO_CODEC = "video.codec";

  /** Custom output file extension */
  String OUTPUT_FILE_EXTENSION = "outputfile.extension";
  String FFMPEG_PROPERTIES = "ffmpeg.properties";
  String FFMPEG_PRESET = "ffmpeg.preset";
  String FFMPEG_SCALE_FILTER = "ffmpeg.scalefilter";
  String AUDIO_FADE = "audio.fade";
  String VIDEO_FADE = "video.fade";
  String DEFAULT_EXTENSION = ".mp4";
  String WEBVTT_EXTENSION = "vtt";
  long SUBTITLE_GRACE_PERIOD = 500; //ms
}
