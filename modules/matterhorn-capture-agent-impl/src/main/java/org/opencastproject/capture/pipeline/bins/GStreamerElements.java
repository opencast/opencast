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

/** List of constant types for GStreamer Elements. **/
public interface GStreamerElements {
  /** Source Module: gstreamer **/
  String CAPSFILTER = "capsfilter";
  String FAKESINK = "fakesink";
  String FAKESRC = "fakesrc";
  String FILESINK = "filesink";
  String FILESRC = "filesrc";
  String IDENTITY = "identity";
  String QUEUE = "queue";
  String TEE = "tee";

  /** Source Module: gst-plugins-base **/
  String APPSRC = "appsrc";
  String APPSINK = "appsink";
  String AUDIOCONVERT = "audioconvert";
  String AUDIOTESTSRC = "audiotestsrc";
  String DECODEBIN = "decodebin";
  String FFMPEGCOLORSPACE = "ffmpegcolorspace";
  String TIMEOVERLAY = "timeoverlay";
  String V4LSRC = "v4lsrc";
  String VIDEORATE = "videorate";
  String VIDEOTESTSRC = "videotestsrc";
  String VOLUME = "volume";
  // Available only in Linux
  String ALSASRC = "alsasrc";
  String XVIMAGESINK = "xvimagesink";

  /** Source Module: gst-plugins-good **/
  String DEINTERLACE = "deinterlace";
  String DEINTERLEAVE = "deinterleave";
  String DV1394SRC = "dv1394src";
  String DVDEC = "dvdec";
  String DVDEMUX = "dvdemux";
  String JPEGENC = "jpegenc";
  String LEVEL = "level";
  String MULTIFILESINK = "multifilesink";
  String MULTIFILESRC = "multifilesrc";
  String PNGDEC = "pngdec";
  String PULSESRC = "pulsesrc";
  String RTPBIN = "gstrtpbin";
  String RTPH264PAY = "rtph264pay";
  String RTPMP4APAY = "rtpmp4apay";
  String RTPMP4GPAY = "rtpmp4gpay";
  String RTPMPAPAY = "rtpmpapay";
  String RTPMPVPAY = "rtpmpvpay";
  String UDPSINK = "udpsink";
  String UDPSRC = "udpsrc";
  String V4L2SRC = "v4l2src";

  /** Source Module: gst-plugins-bad **/
  String FAAC = "faac";
  String INPUT_SELECTOR = "input-selector";
  String MP4MUX = "mp4mux";
  String MPEG2ENC = "mpeg2enc";
  String MPEGPSDEMUX = "mpegpsdemux";
  String MPEGPSMUX = "mpegpsmux";
  String MPEGVIDEOPARSE = "mpegvideoparse";

  /** Source Module: gst-plugins-ugly **/
  String MPEG2DEC = "mpeg2dec";
  String TWOLAME = "twolame";
  String X264ENC = "x264enc";

  /** Source Module: gst-ffmpeg **/
  String FFDEINTERLACE = "ffdeinterlace";
  String FFENC_MPEG2VIDEO = "ffenc_mpeg2video";
  String FFVIDEOSCALE = "ffvideoscale";
}
