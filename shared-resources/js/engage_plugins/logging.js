/**
 *  Copyright 2009_2011 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL_2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Logging
 */
Opencast.logging = (function ()
{
  var ANALYTICS_DATA_AJAX_FAILED = "ANALYTICS_DATA_AJAX_FAILED",  //The ajax loading the analytics for the sparkline has failed
      ANALYTICS_INIT_AJAX_FAILED = "ANALYTICS_INIT_AJAX_FAILED",  //The ajax checking to see if there is analytics data has failed
      ANNOTATION_CHAPTER_AJAX_FAILED = "ANNOTATION_CHAPTER_AJAX_FAILED",
      ANNOTATION_INIT_AJAX_FAILED = "ANNOTATION_INIT_AJAX_FAILED",
      CLOSED_CAPTIONS = "CLOSED_CAPTIONS",  //The closed captions have been turned on
      DESCRIPTION_AJAX_DATA_FAILED = "DESCRIPTION_AJAX_DATA_FAILED",  //The ajax requesting general data about the episode has failed
      DESCRIPTION_AJAX_VIEWS_FAILED = "DESCRIPTION_AJAX_VIEWS_FAILED",  //The ajax requesting the number of view for this episode has failed
      EMAIL = "EMAIL",  //The mediapackage has been shared via email
      EMBED_DETAILED_LOGGING_AJAX_FAILED = "EMBED_DETAILED_LOGGING_AJAX_FAILED",  //The call to turn on detailed logging has failed in the embed player
      EMBED_SEARCH_AJAX_FAILED = "EMBED_SEARCH_AJAX_FAILED",  //The search ajax in embed mode has failed in some way
      EMBED_STARTUP = "EMBED_STARTUP",  //The embed player has started
      FAST_FORWARD = "FAST_FORWARD",  //The fast forward button has been pressed
      FOOTPRINT = "FOOTPRINT",  //The normal footprint event
      GENERATE_EMBED = "GENERATE_EMBED-", //An embed link has been generated for the size after the dash
      HEARTBEAT = "HEARTBEAT",  //The heartbeat fires every n second, currently 30
      HIDE_NOTES = "HIDE_NOTES",
      HIDE_TIME_LAYER = "HIDE_TIME_LAYER",
      HIDE_TRANSCRIPT = "HIDE_TRANSCRIPT",
      MUTE = "MUTE",  //The sound has been muted
      NORMAL_DETAILED_LOGGING_AJAX_FAILED = "NORMAL_DETAILED_LOGGING_AJAX_FAILED",  //The call to turn on the detailed logging has failed in the normal player
      NORMAL_SEARCH_AJAX_FAILED = "NORMAL_SEARCH_AJAX_FAILED",
      NORMAL_STARTUP = "NORMAL_STARTUP",  //The normal engage player has started
      PAUSE = "PAUSE",  //The playback has been paused
      PLAY = "PLAY",  //The playback has started/resumed
      RESIZE_TO = "RESIZE_TO-", //The window has been resized
      REWIND = "REWIND",  //The rewind button has been pressed
      SEARCH_AJAX_FAILED = "SEARCH_AJAX_FAILED",  //The search ajax fired, but encountered an error
      SEARCH = "SEARCH-", //The search ajax fired for the text after the dash
      SEEK = "SEEK",  //The player is seeking
      SEEK_SEGMENT = "SEEK_SEGMENT",  //The player is seeking to a segment
      SEGMENTS_AJAX_FAILED = "SEGMENTS_AJAX_FAILED",  //The ajax loading the data for the segments tab has failed
      SEGMENTS_TEXT_AJAX_FAILED = "SEGMENTS_TEXT_AJAX_FAILED",  //The ajax loading the data for the text segment tab has failed
      SEGMENT_UI_AJAX_FAILED = "SEGMENT_UI_AJAX_FAILED",  //The ajax loading the segmentation display in the player has failed
      SERIES_INFO_AJAX_FAILED = "SERIES_INFO_AJAX_FAILED",  //The ajax request for general series information has failed
      SERIES_EPISODES_AJAX_FAILED = "SERIES_EPISODES_AJAX_FAILED",  //The ajax request for more episodes in the current series has failed
      SERIES_DROPDOWN_AJAX_FAILED = "SERIES_DROPDOWN_AJAX_FAILED",  //The ajax loading the series dropdown data has failed
      SERIES_PAGE_AJAX_FAILED = "SERIES_PAGE_AJAX_FAILED",  //The ajax loading the series tab data has failed
      SET_VOLUME = "SET_VOLUME-", //The volume has been set to the level after the dash
      SHOW_ANALYTICS = "SHOW_ANALYTICS",  //The playback analytics were being shown
      SHOW_ANNOTATIONS = "SHOW_ANNOTATIONS",
      SHOW_DESCRIPTION = "SHOW_DESCRIPTION",  //The description tab was activated
      SHOW_EMBED = "SHOW_EMBED",  //The embed generation dialog was shown
      SHOW_NOTES = "SHOW_NOTES",
      SHOW_SEGMENTS = "SHOW_SEGMENTS",  //The segments tab was activated
      SHOW_SHARE = "SHOW_SHARE",  //The share option dialog was shown
      SHOW_SHORTCUTS = "SHOW_SHORTCUTS",  //The keyboard shortcut dialog was shown
      SHOW_TEXT_SEGMENTS = "SHOW_TEXT_SEGMENTS",  //The segment text tab was activated
      SHOW_TIME_LAYER = "SHOW_TIME_LAYER",
      SHOW_TRANSCRIPT = "SHOW_TRANSCRIPT",
      SKIP_BACKWARD = "SKIP_BACKWARD",  //The last-slide button was pressed
      SKIP_FORWARD = "SKIP_FORWARD",  //The next-slide button was pressed
      UNMUTE = "UNMUTE",  //The sound has been unmuted
      VALID_EDIT_TIME = "VALID_EDIT_TIME",  //The time editor was used successfully
      VIDEOSIZE_AUDIO = "VIDEOSIZE_AUDIO",  //The player is only playing back the audio (?)
      VIDEOSIZE_BIG_LEFT = "VIDEOSIZE_BIG_LEFT",  //The player showing the left stream at a larger size
      VIDEOSIZE_BIG_RIGHT = "VIDEOSIZE_BIG_RIGHT",  //The player is showing the right stream at a larger size
      VIDEOSIZE_LEFT_ONLY = "VIDEOSIZE_LEFT_ONLY",  //The player is only showing the left stream
      VIDEOSIZE_MULTI = "VIDEOSIZE_MULTI",  //The player is showing the streams at an equal size
      VIDEOSIZE_RIGHT_ONLY = "VIDEOSIZE_RIGHT_ONLY",  //The player is only showing the right stream
      VIDEOSIZE_SINGLE = "VIDEOSIZE_SINGLE";  //The player is only showing one stream (embed?)

    return {
      ANALYTICS_DATA_AJAX_FAILED: ANALYTICS_DATA_AJAX_FAILED,
      ANALYTICS_INIT_AJAX_FAILED: ANALYTICS_INIT_AJAX_FAILED,
      ANNOTATION_CHAPTER_AJAX_FAILED: ANNOTATION_CHAPTER_AJAX_FAILED,
      ANNOTATION_INIT_AJAX_FAILED: ANNOTATION_INIT_AJAX_FAILED,
      CLOSED_CAPTIONS: CLOSED_CAPTIONS,
      DESCRIPTION_AJAX_DATA_FAILED: DESCRIPTION_AJAX_DATA_FAILED,
      DESCRIPTION_AJAX_VIEWS_FAILED: DESCRIPTION_AJAX_VIEWS_FAILED,
      EMAIL: EMAIL,
      EMBED_DETAILED_LOGGING_AJAX_FAILED: EMBED_DETAILED_LOGGING_AJAX_FAILED,
      EMBED_SEARCH_AJAX_FAILED: EMBED_SEARCH_AJAX_FAILED,
      EMBED_STARTUP: EMBED_STARTUP,
      EMBED_STARTUP: EMBED_STARTUP,
      FAST_FORWARD: FAST_FORWARD,
      FOOTPRINT: FOOTPRINT,
      GENERATE_EMBED: GENERATE_EMBED,
      HEARTBEAT: HEARTBEAT,
      HIDE_NOTES: HIDE_NOTES,
      HIDE_TIME_LAYER: HIDE_TIME_LAYER,
      HIDE_TRANSCRIPT: HIDE_TRANSCRIPT,
      MUTE: MUTE,
      NORMAL_DETAILED_LOGGING_AJAX_FAILED: NORMAL_DETAILED_LOGGING_AJAX_FAILED,
      NORMAL_SEARCH_AJAX_FAILED: NORMAL_SEARCH_AJAX_FAILED,
      NORMAL_STARTUP: NORMAL_STARTUP,
      PAUSE: PAUSE,
      PLAY: PLAY,
      RESIZE_TO: RESIZE_TO,
      REWIND: REWIND,
      SEARCH_AJAX_FAILED: SEARCH_AJAX_FAILED,
      SEARCH: SEARCH,
      SEEK: SEEK,
      SEEK_SEGMENT: SEEK_SEGMENT,
      SEGMENTS_AJAX_FAILED: SEGMENTS_AJAX_FAILED,
      SEGMENTS_TEXT_AJAX_FAILED: SEGMENTS_TEXT_AJAX_FAILED,
      SEGMENT_UI_AJAX_FAILED: SEGMENT_UI_AJAX_FAILED,
      SERIES_INFO_AJAX_FAILED: SERIES_INFO_AJAX_FAILED,
      SERIES_EPISODES_AJAX_FAILED: SERIES_EPISODES_AJAX_FAILED,
      SERIES_DROPDOWN_AJAX_FAILED: SERIES_DROPDOWN_AJAX_FAILED,
      SERIES_PAGE_AJAX_FAILED: SERIES_PAGE_AJAX_FAILED,
      SET_VOLUME: SET_VOLUME,
      SHOW_ANALYTICS: SHOW_ANALYTICS,
      SHOW_ANNOTATIONS: SHOW_ANNOTATIONS,
      SHOW_DESCRIPTION: SHOW_DESCRIPTION,
      SHOW_EMBED: SHOW_EMBED,
      SHOW_NOTES: SHOW_NOTES,
      SHOW_SEGMENTS: SHOW_SEGMENTS,
      SHOW_SHARE: SHOW_SHARE,
      SHOW_SHORTCUTS: SHOW_SHORTCUTS,
      SHOW_TEXT_SEGMENTS: SHOW_TEXT_SEGMENTS,
      SHOW_TIME_LAYER: SHOW_TIME_LAYER,
      SHOW_TRANSCRIPT: SHOW_TRANSCRIPT,
      SKIP_BACKWARD: SKIP_BACKWARD,
      SKIP_FORWARD: SKIP_FORWARD,
      UNMUTE: UNMUTE,
      VALID_EDIT_TIME: VALID_EDIT_TIME,
      VIDEOSIZE_AUDIO: VIDEOSIZE_AUDIO,
      VIDEOSIZE_BIG_LEFT: VIDEOSIZE_BIG_LEFT,
      VIDEOSIZE_BIG_RIGHT: VIDEOSIZE_BIG_RIGHT,
      VIDEOSIZE_LEFT_ONLY: VIDEOSIZE_LEFT_ONLY,
      VIDEOSIZE_MULTI: VIDEOSIZE_MULTI,
      VIDEOSIZE_RIGHT_ONLY: VIDEOSIZE_RIGHT_ONLY,
      VIDEOSIZE_SINGLE: VIDEOSIZE_SINGLE
    };

}());
