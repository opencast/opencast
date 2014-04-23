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

package org.opencastproject.videoeditor.gstreamer;

/**
 * Gstreamer elements.
 */
public interface GstreamerElements {

  // gstreamer-core
  String CAPSFILTER = "capsfilter";
  String FAKESINK = "fakesink";
  String FILESINK = "filesink";
  String FILESRC = "filesrc";
  String IDENTITY = "identity";
  String QUEUE = "queue";

  // gstreamer-plugins-base
  String AUDIOCONVERT = "audioconvert";
  String AUDIORATE = "audiorate";
  String AUDIORESAMPLE = "audioresample";
  String DECODEBIN = "decodebin";
  String DECODEBIN2 = "decodebin2";
  String FFMPEGCOLORSPACE = "ffmpegcolorspace";
  String VIDEORATE = "videorate";

  // gstreamer-plugins-good
  String AUTOAUDIOSINK = "autoaudiosink";
  String AUTOVIDEOSINK = "autovideosink";
  String CUTTER = "cutter";
  String MP4MUX = "mp4mux";

  // gstreamer-plugins-bad
  String FAAC = "faac";

  // gstreamer-plugins ugly
  String X264ENC = "x264enc";

  // gstreamer-gnonlin
  String GNL_COMPOSITION = "gnlcomposition";
  String GNL_FILESOURCE = "gnlfilesource";
}
