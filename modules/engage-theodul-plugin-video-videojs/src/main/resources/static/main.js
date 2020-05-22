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
/*jslint browser: true, nomen: true*/
/*global define*/
define(['require', 'jquery', 'underscore', 'backbone', 'basil', 'bowser', 'engage/core'], function (require, $, _, Backbone, Basil, Bowser, Engage) {
  'use strict';

  var insertIntoDOM = true;
  var PLUGIN_NAME = 'Engage VideoJS Videodisplay';
  var PLUGIN_TYPE = 'engage_video';
  var PLUGIN_VERSION = '1.0';
  var PLUGIN_TEMPLATE_DESKTOP = 'templates/desktop.html';
  var PLUGIN_TEMPLATE_EMBED = 'templates/embed.html';
  var PLUGIN_TEMPLATE_MOBILE = 'templates/mobile.html';
  var PLUGIN_STYLES_DESKTOP = [
    'styles/desktop.css',
    'lib/video-js/video-js.min.css'
  ];
  var PLUGIN_STYLES_EMBED = [
    'styles/embed.css',
    'lib/video-js/video-js.min.css'
  ];
  var PLUGIN_STYLES_MOBILE = [
    'styles/mobile.css',
    'lib/video-js/video-js.min.css'
  ];

  var plugin;
  var events = {
    play: new Engage.Event('Video:play', 'plays the video', 'both'),
    pause: new Engage.Event('Video:pause', 'pauses the video', 'both'),
    seek: new Engage.Event('Video:seek', 'seek video to a given position in seconds', 'both'),
    ready: new Engage.Event('Video:ready', 'all videos loaded successfully', 'both'),
    ended: new Engage.Event('Video:ended', 'end of the video', 'trigger'),
    playerLoaded: new Engage.Event('Video:playerLoaded', 'player loaded successfully', 'trigger'),
    synchronizing: new Engage.Event('Video:synchronizing', 'synchronizing videos with the master video', 'trigger'),
    buffering: new Engage.Event('Video:buffering', 'video is buffering', 'trigger'),
    bufferedAndAutoplaying: new Engage.Event('Video:bufferedAndAutoplaying', 'buffering successful, was playing, autoplaying now', 'trigger'),
    customNotification: new Engage.Event('Notification:customNotification', 'a custom message', 'trigger'),
    customError: new Engage.Event('Notification:customError', 'an error occured', 'trigger'),
    bufferedButNotAutoplaying: new Engage.Event('Video:bufferedButNotAutoplaying', 'buffering successful, was not playing, not autoplaying now', 'trigger'),
    timeupdate: new Engage.Event('Video:timeupdate', 'timeupdate happened', 'trigger'),
    volumechange: new Engage.Event('Video:volumechange', 'volume change happened', 'trigger'),
    fullscreenChange: new Engage.Event('Video:fullscreenChange', 'fullscreen change happened', 'trigger'),
    usingFlash: new Engage.Event('Video:usingFlash', 'flash is being used', 'trigger'),
    numberOfVideodisplaysSet: new Engage.Event('Video:numberOfVideodisplaysSet', 'the number of videodisplays has been set', 'trigger'),
    aspectRatioSet: new Engage.Event('Video:aspectRatioSet', 'the aspect ratio has been calculated', 'both'),
    isAudioOnly: new Engage.Event('Video:isAudioOnly', 'whether it´s audio only or not', 'trigger'),
    audioCodecNotSupported: new Engage.Event('Video:audioCodecNotSupported', 'when the audio codec seems not to be supported by the browser', 'trigger'),
    videoFormatsFound: new Engage.Event('Video:videoFormatsFound', '', 'both'),
    playPause: new Engage.Event('Video:playPause', '', 'handler'),
    plugin_load_done: new Engage.Event('Core:plugin_load_done', '', 'handler'),
    fullscreenEnable: new Engage.Event('Video:fullscreenEnable', 'go to fullscreen', 'handler'),
    fullscreenCancel: new Engage.Event('Video:fullscreenCancel', 'cancel fullscreen', 'handler'),
    volumeSet: new Engage.Event('Video:volumeSet', 'set the volume', 'handler'),
    volumeGet: new Engage.Event('Video:volumeGet', 'get the volume', 'handler'),
    sliderStop: new Engage.Event('Slider:stop', 'slider stopped', 'handler'),
    playbackRateChanged: new Engage.Event('Video:playbackRateChanged', 'The video playback rate changed', 'handler'),
    playbackRateIncrease: new Engage.Event('Video:playbackRateIncrease', '', 'handler'),
    playbackRateDecrease: new Engage.Event('Video:playbackRateDecrease', '', 'handler'),
    mediaPackageModelError: new Engage.Event('MhConnection:mediaPackageModelError', '', 'handler'),
    seekLeft: new Engage.Event('Video:seekLeft', '', 'handler'),
    seekRight: new Engage.Event('Video:seekRight', '', 'handler'),
    autoplay: new Engage.Event('Video:autoplay', '', 'handler'),
    initialSeek: new Engage.Event('Video:initialSeek', '', 'handler'),
    qualitySet: new Engage.Event('Video:qualitySet', '', 'handler'),
    focusVideo: new Engage.Event('Video:focusVideo', 'increases the size of one video', 'handler'),
    resetLayout: new Engage.Event('Video:resetLayout', 'resets the layout of the videodisplays', 'handler'),
    movePiP: new Engage.Event('Video:movePiP', 'moves the smaller picture over the larger to the different corners', 'handler'),
    togglePiP: new Engage.Event('Video:togglePiP', 'switches between PiP and next to each other layout', 'handler'),
    closeVideo: new Engage.Event('Video:closeVideo', 'closes one videostream', 'handler'),
    openVideo: new Engage.Event('Video:openVideo', 'opens a new videostream', 'handler'),
    moveUp: new Engage.Event('Video:moveUp', 'moves video up', 'handler'),
    moveDown: new Engage.Event('Video:moveDown', 'moves video down', 'handler'),
    moveLeft: new Engage.Event('Video:moveLeft', 'moves video left', 'handler'),
    moveRight: new Engage.Event('Video:moveRight', 'moves video right', 'handler'),
    setZoomLevel: new Engage.Event('Video:setZoomLevel', 'sets the zoom level', 'both'),
    zoomReset: new Engage.Event('Video:resetZoom', 'resets position and zoom level', 'handler'),
    moveHorizontal: new Engage.Event('Video:moveHorizontal', 'move video horizontal', 'handler'),
    moveVertical: new Engage.Event('Video:moveVertical', 'move video vertical', 'handler'),
    zoomIn: new Engage.Event('Video:zoomIn', 'zooms in video', 'handler'),
    zoomOut: new Engage.Event('Video:zoomOut', 'zooms out video', 'handler'),
    zoomChange: new Engage.Event('Video:zoomChange', 'zoom level has changed', 'trigger'),
    switchVideo: new Engage.Event('Video:switch', 'switch the video', 'handler'),
    toggleCaptions: new Engage.Event('Video:toggleCaptions', 'toggle captions', 'handler'),
    captionsFound: new Engage.Event('Video:captionsFound', 'captions found', 'handler')
  };

  var isDesktopMode = false;
  var isEmbedMode = false;
  var isMobileMode = false;

  // desktop, embed and mobile logic
  switch (Engage.model.get('mode')) {
    case 'embed':
      plugin = {
        insertIntoDOM: insertIntoDOM,
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES_EMBED,
        template: PLUGIN_TEMPLATE_EMBED,
        events: events
      };
      isEmbedMode = true;
      break;
    case 'mobile':
      plugin = {
        insertIntoDOM: insertIntoDOM,
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES_MOBILE,
        template: PLUGIN_TEMPLATE_MOBILE,
        events: events
      };
      isMobileMode = true;
      break;
    case 'desktop':
    default:
      plugin = {
        insertIntoDOM: insertIntoDOM,
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES_DESKTOP,
        template: PLUGIN_TEMPLATE_DESKTOP,
        events: events
      };
      isDesktopMode = true;
      break;
  }

  /* change these variables */
  var videoPath = 'lib/video-js/video.min';
  /* https://github.com/videojs/video.js/releases */
  var videojs_swf_path = 'lib/video-js/video-js.swf';
  var synchronizePath = 'lib/synchronize-min';
  /* https://github.com/CallToPower/Synchronize.js */
  var hlsPath = 'lib/video-js/videojs-contrib-hls.min';
  /* https://github.com/videojs/videojs-contrib-hls */
  var dashPath = 'lib/video-js/dash.all.min';
  var dashPluginPath = 'lib/video-js/videojs-dash.min';
  /* https://github.com/videojs/videojs-contrib-dash */
  var videoAreaAspectRatio;
  var checkVideoDisplaySizeTimeout = 1500;
  var audioLoadTimeoutCheckDelay = 5000;
  var seekSeconds = 5;
  var interval_autoplay_ms = 1000;
  var interval_initialSeek_ms = 1000;
  var timeout_initialSeek_ms = 250;
  var timer_qualitychange = 1000;
  var zoom_step_size = 0.2;
  var decimal_places = 3;

  /* don't change these variables */
  var currentTime = 0;
  var Utils;
  var parsedSeconds = 0;
  var interval_autoplay;
  var interval_initialSeek;
  var VideoDataModel;
  var isAudioOnly = false;
  var isUsingFlash = false;
  var mastervideotype = '';
  var aspectRatio = null;
  var singleVideoPaddingTop = '56.25%';
  var initCount = 7;
  var videoDisplayReady = 0;
  var infoMeChange = 'change:infoMe';
  var mediapackageError = false;
  var videoDisplayNamePrefix = 'videojs_videodisplay_';
  var id_video_wrapper = 'video_wrapper';
  var id_engage_video = 'engage_video';
  var id_videojs_wrapper = 'videojs_wrapper';
  var id_videoDisplayClass = 'videoDisplay';
  var id_engageControls = 'engage_controls';
  var id_resize_container = 'engage_resize_container';
  var id_engage_video_fullsceen_wrapper = 'fullscreen_video_wrapper';
  var id_page_cover = 'page-cover';
  var id_btn_fullscreenCancel = 'btn_fullscreenCancel';
  var id_generated_videojs_flash_component = 'videojs_videodisplay_0_flash_api';
  var id_btn_openInPlayer = 'btn_openInPlayer';
  var id_btn_switchPlayer = 'btn_switchPlayer';
  var id_switchPlayer_value = 'switchPlayer-value';
  var id_audioDisplay = 'audioDisplay';
  var class_vjs_switchPlayer = 'vjs-switchPlayer';
  var class_btn_video = 'btn-video';
  var class_vjs_switchPlayer_value = 'vjs-switchPlayer-value';
  var class_vjs_menu_content = 'vjs-menu-content';
  var class_vjs_menu_item = 'vjs-menu-item';
  var class_vjsposter = 'vjs-poster';
  var class_vjs_openInPlayer = 'vjs-openInPlayer';
  var class_vjs_control_text = 'vjs-control-text';
  var class_vjs_remaining_time = 'vjs-remaining-time';
  var class_audioDisplay = 'audioDisplay';
  var class_audioDisplayError = 'audioDisplayError';
  var videosReady = false;
  var pressedPlayOnce = false;
  var mediapackageChange = 'change:mediaPackage';
  var videoDataModelChange = 'change:videoDataModel';
  var event_html5player_volumechange = 'volumechange';
  var event_html5player_fullscreenchange = 'fullscreenchange';
  var event_sjs_allPlayersReady = 'sjs:allPlayersReady';
  var event_sjs_playerLoaded = 'sjs:playerLoaded';
  var event_sjs_masterPlay = 'sjs:masterPlay';
  var event_sjs_masterPause = 'sjs:masterPause';
  var event_sjs_masterEnded = 'sjs:masterEnded';
  var event_sjs_masterTimeupdate = 'sjs:masterTimeupdate';
  var event_sjs_synchronizing = 'sjs:synchronizing';
  var event_sjs_buffering = 'sjs:buffering';
  var event_sjs_bufferedAndAutoplaying = 'sjs:bufferedAndAutoplaying';
  var event_sjs_bufferedButNotAutoplaying = 'sjs:bufferedButNotAutoplaying';
  var event_sjs_debug = 'sjs:debug';
  var event_sjs_stopBufferChecker = 'sjs:stopBufferChecker';
  var currentlySelectedVideodisplay = 0;
  var globalVideoSource = [];
  var videoResultions = [];
  var loadHls = false;
  var loadDash = false;
  var flavors = '';
  var mimetypes = '';
  var translations = [];
  var videoDataView = undefined;
  var fullscreen = false;
  var videoDisplayClass = 'videoDisplay';
  var qualities = null;
  var videodisplayMaster = null;
  var videoDefaultLayoutClass = 'videoDefaultLayout';
  var videoUnfocusedClass = 'videoUnfocusedPiP';
  var videoFocusedClass = 'videoFocusedPiP';
  var unfocusedPiPClass = 'videoUnfocusedPiP';
  var focusedPiPClass = 'videoFocusedPiP';
  var unfocusedClass = 'videoUnfocused';
  var focusedClass = 'videoFocused';
  var isPiP = true;
  var pipPos = 'left';
  var activeCaption = undefined;
  var overlayTimer;

  var foundQualities = undefined;
  var sortedResolutionsList = undefined;
  var zoomTimeout = 500;

  function initTranslate(language, funcSuccess, funcError) {
    var path = Engage.getPluginPath('EngagePluginVideoVideoJS').replace(/(\.\.\/)/g, '');
    /* this solution is really bad, fix it... */
    var jsonstr = window.location.origin + '/engage/theodul/' + path;

    Engage.log('Controls: selecting language ' + language);
    jsonstr += 'language/' + language + '.json';
    $.ajax({
      url: jsonstr,
      dataType: 'json',
      success: function (data) {
        if (data) {
          data.value_locale = language;
          translations = data;
          if (funcSuccess) {
            funcSuccess(translations);
          }
        } else if (funcError) {
          funcError();
        }
      },
      error: function () {
        if (funcError) {
          funcError();
        }
      }
    });
  }

  function translate(str, strIfNotFound) {
    return (translations[str] != undefined) ? translations[str] : strIfNotFound;
  }

  var basilOptions = {
    namespace: 'mhStorage'
  };
  Basil = new window.Basil(basilOptions);

  function acceptFormat(track) {
    var preferredFormat = Basil.get('preferredFormat');
    if (preferredFormat && (preferredFormat != null)) {
      var preferredFormat_checked = Utils.preferredFormat(preferredFormat);
      // preferred format is not available
      if ((preferredFormat_checked == null) || (mimetypes.indexOf(preferredFormat_checked) == -1)) {
        return true; // accept all
      }
      return track.mimetype == preferredFormat_checked;
    }
    return true;
  }

  function filterTracksByTag(tracks, filterTags, strict = false) {
    if (filterTags == undefined) {
      return tracks;
    }
    var newTracksArray = [];

    for (var i = 0; i < tracks.length; i++) {
      var found = false,
          number_of_tags = tracks[i].tags ? tracks[i].tags.tag.length : 0;
      for (var j = 0; j < number_of_tags; j++) {
        for (var k = 0; k < filterTags.length; k++) {
          if (tracks[i].tags.tag[j] == filterTags[k].trim()) {
            found = true;
            newTracksArray.push(tracks[i]);
            break;
          }
        }
        if (found) break;
      }
    }

    // avoid filtering to an empty list, better play something than nothing
    if (newTracksArray.length < 1 && !strict) {
      return tracks;
    }
    return newTracksArray;
  }

  function getTrackResolutionWidth(track) {
    return track.resolution.split('x')[1]
  }

  function getTrackClosestToResolution(tracks, quality) {
    // match quality tag to resolution
    var desiredWidth;
    for (var i = 0; i < sortedResolutionsList.length; i++) {
      if (sortedResolutionsList[i][0] == quality) {
        desiredWidth = sortedResolutionsList[i][1];
        break;
      }
    }

    if (desiredWidth == undefined) {
      // This should not happen. quality should be found in sortedResolutionsList
      // after it got populated by getQualities.
      return tracks[0];
    }

    // find track
    var widthAbsDifferences = tracks.map(function (track) {
      return Math.abs(getTrackResolutionWidth(track) - desiredWidth);
    });

    var closestIndex = widthAbsDifferences.indexOf(Math.min(...widthAbsDifferences));
    return tracks[closestIndex];
  }

  /**
   * Lookup all tags that are in use
   * @param {type} videoSources, List of still used tracks
   * @param {type} keyword, substing that should be included in the tag
   * @returns {Array}
   */
  function getTags(videoSources, keyword) {
    if (videoSources === undefined) {
      return;
    }
    var tagList = [];

    for (var v in videoSources) {
      for (var i = 0; i < videoSources[v].length; i++) {
        var number_of_tags = videoSources[v][i].tags ? videoSources[v][i].tags.tag.length : 0;
        for (var j = 0; j < number_of_tags; j++) {
          if (keyword !== undefined) {
            if (videoSources[v][i].tags.tag[j].indexOf(keyword) > 0) {
              tagList.push(videoSources[v][i].tags.tag[j]);
            }
          } else {
            tagList.push(videoSources[v][i].tags.tag[j]);
          }
        }
      }
    }

    return _.uniq(tagList);
  }

  /**
   * Find the different video qualities
   * @param {type} videoSources videoSources that are still in use
   * @returns {undefined}
   *
   */
  function getQualities(videoSources) {
    // using a cache for qualities, as they probably do not change
    if (foundQualities) {
      return foundQualities;
    }
    var tagsList = getTags(videoSources, '-quality');
    var qualitiesList = [];
    tagsList.forEach(function(quality) {
      qualitiesList.push(quality.substring(0, quality.indexOf('-quality')));
    });
    var tracks = [];
    for (var source in videoSources) {
      if (videoSources[source] !== undefined) {
        tracks = tracks.concat(videoSources[source]);
      }
    }
    sortedResolutionsList = [];
    sortedResolutionsList = _.map(qualitiesList, function(quality) {
      var currentTrack = filterTracksByTag(tracks, [quality + '-quality'])[0];
      if (currentTrack !== undefined && currentTrack.resolution !== undefined)
        return [quality, getTrackResolutionWidth(currentTrack)];
    });
    sortedResolutionsList.sort(compareQuality);
    sortedResolutionsList.reverse();
    foundQualities = [];
    for (var i = 0; i < sortedResolutionsList.length; ++i) {
      foundQualities.push(sortedResolutionsList[i][0]);
    }
    return foundQualities;
  }

  function compareQuality(a, b) {
    if (a && b) {
      if (parseInt(a[1]) == parseInt(b[1])) {
        return 0;
      }
      return parseInt(a[1]) > parseInt(b[1]) ? 1 : -1;
    }
    return 0;
  }

  function filterTracksByFormat(tracks, filterFormats) {
    if (filterFormats == undefined) {
      return tracks;
    }
    var filterFormatsArray = filterFormats.split(',');
    var newTracksArray = [];

    for (var i = 0; i < tracks.length; i++) {
      for (var j = 0; j < filterFormatsArray.length; j++) {
        var formatMimeType = Utils.preferredFormat(filterFormatsArray[j].trim());
        if (formatMimeType == undefined) return tracks; // if illegal mimetypes are configured ignore config
        if (tracks[i].mimetype == formatMimeType) {
          newTracksArray.push(tracks[i]);
          break;
        }
      }
    }

    return newTracksArray;
  }

  function registerSynchronizeEvents() {
    // throw some important synchronize.js-events for other plugins
    $(document)
      .on(event_sjs_allPlayersReady, function () {
        videosReady = true;
        Engage.trigger(plugin.events.ready.getName());
      })
      .on(event_sjs_playerLoaded, function () {
        Engage.trigger(plugin.events.playerLoaded.getName());
      })
      .on(event_sjs_masterPlay, function () {
        Engage.trigger(plugin.events.play.getName(), true);
        pressedPlayOnce = true;
      })
      .on(event_sjs_masterPause, function () {
        Engage.trigger(plugin.events.pause.getName(), true);
      })
      .on(event_sjs_masterEnded, function () {
        Engage.trigger(plugin.events.ended.getName(), true);
      })
      .on(event_sjs_masterTimeupdate, function (event, time) {
        Engage.trigger(plugin.events.timeupdate.getName(), time, true);
      })
      .on(event_sjs_synchronizing, function () {
        Engage.trigger(plugin.events.synchronizing.getName());
      })
      .on(event_sjs_buffering, function () {
        Engage.trigger(plugin.events.buffering.getName());
      })
      .on(event_sjs_bufferedAndAutoplaying, function () {
        Engage.trigger(plugin.events.bufferedAndAutoplaying.getName());
      })
      .on(event_sjs_bufferedButNotAutoplaying, function () {
        Engage.trigger(plugin.events.bufferedButNotAutoplaying.getName());
      });
  }

  function initSynchronize() {
    $(document)
      .trigger(event_sjs_debug, Engage.model.get('isDebug'))
      .trigger(event_sjs_stopBufferChecker);
  }

  function initQualities(videoDataView) {
    qualities = getQualities(videoDataView.model.get('videoSources'));

    if (qualities.length > 1) {
      Engage.on(plugin.events.qualitySet.getName(), function (q) {
        changeQuality(q);
      });
      Engage.trigger(plugin.events.videoFormatsFound.getName(), qualities);
      if (!Engage.model.get('quality')) {
        if (isMobileMode) {
          Engage.model.set('quality', qualities[qualities.length - 1]);
        } else {
          Engage.model.set('quality', qualities[Math.floor(qualities.length / 2)]);
        }
      }
      changeQuality(Engage.model.get('quality'));
    }
  }

  function changeQuality(quality) {
    if (quality) {
      var isPaused = videodisplayMaster.paused();
      Engage.trigger(plugin.events.pause.getName(), false);
      var qualityTag = quality + '-quality';
      Engage.model.set('quality', quality);
      Engage.log('Video: Setting quality to: ' + quality);
      var tuples = getSortedVideosourcesArray(globalVideoSource);
      for (var i = 0; i < tuples.length; ++i) {
        var value = tuples[i][1];
        if (value[1][0]) {
          var track = filterTracksByTag(value[1], [qualityTag], true);
          if (track.length == 0) {
            // couldn't find track with exact tag; search with resolution
            track = [getTrackClosestToResolution(value[1], quality)];
          }
          videojs(value[0]).src(track[0].src);
        }
      }
      if (pressedPlayOnce && (currentTime > 0)) {
        window.setTimeout(function () {
          initSynchronize(false);
          Engage.trigger(plugin.events.seek.getName(), currentTime);
          if (!isPaused) {
            Engage.trigger(plugin.events.play.getName());
          }
        }, timer_qualitychange);
      }
    }
  }

  function registerZoomLevelEvents() {
    if (isUsingFlash) {
      Engage.log('Video: Zoom for Flash is not supported');
      return;
    }

    var selector = 'video';
    var lastEvent = null;
    var wheelEvent = null;
    var videoFocused = true;
    var singleVideo = true;
    var mapSelector = '#fullscreen_video_wrapper';
    var minimapVisible = false;
    var zoomLevels = [];
    var ratio = aspectRatio[2] / aspectRatio[1];
    var id = Engage.model.get('urlParameters').id;
    var flag = 0;

    /* Hides Minimap, e.g. when zoom < 1 */
    function hideMinimap() {
      $('#indicator').remove();
      minimapVisible = false;
    }

    /* Shows Minimap when it's not already displayed */
    function showMinimap() {
      if ($(selector) === undefined || $(selector)[0] === undefined) {
        return;
      }

      var zoom = $(selector)[0].style.transform.replace(/[a-z]*/, '');
      zoom = zoom.replace('(', '');
      zoom = zoom.replace(')', '');

      if (Number(zoom) <= 1) {
        return;
      }

      if ($(selector).length == 1) {
        $(mapSelector).children().first().append('<canvas id="indicator"></canvas>');
        var minimapWidth = $('#indicator').width();

        var c = document.getElementById('indicator');
        var ctx = c.getContext('2d');

        var mapWidth = minimapWidth / zoom;
        var mapHeight = (minimapWidth * ratio) / zoom;

        ctx.fillStyle = '#FFFFFF';

        ctx.fillRect(minimapWidth / 2 - mapWidth / 2, (minimapWidth * ratio) / 2 - mapHeight / 2, mapWidth, mapHeight);

        minimapVisible = true;
        updateMinimap();
      }
    }

    /* Redraws Minimap, e.g. when other display is focused */
    function redrawMinimap() {
      hideMinimap();
      showMinimap();
    }

    /* Updates Minimap, e.g. when moving video */
    function updateMinimap() {
      var zoom = $(selector)[0].style.transform.replace(/[a-z]*/, '');
      zoom = zoom.replace('(', '');
      zoom = zoom.replace(')', '');

      if (Number(zoom) <= 1) {
        return;
      }
      var h = $(mapSelector).height();
      var w = $(mapSelector).width();
      var minimapWidth = $('#indicator').width();

      var left = $(selector).css('left').replace('px', '');
      var top = $(selector).css('top').replace('px', '');

      var hDiff = (h * zoom - h) / 2;
      var wDiff = (w * zoom - w) / 2;

      var relHDiff = top / hDiff;
      var relWDiff = left / wDiff;

      var mapWidth = minimapWidth / zoom;
      var mapHeight = (minimapWidth * ratio) / zoom;

      var c = document.getElementById('indicator');

      if (c == undefined) {
        return;
      }
      // reset drawings
      c.width = $('#indicator').width();
      c.height = $('#indicator').height();

      var ctx = c.getContext('2d');
      ctx.fillStyle = '#FFFFFF';
      // calculate Position and draw it onto indicator
      var x = (minimapWidth / 2 - mapWidth / 2) - ((minimapWidth / 2 - mapWidth / 2) * relWDiff);
      var y = (minimapWidth * ratio) / 2 - mapHeight / 2 - (((minimapWidth * ratio) / 2 - mapHeight / 2) * relHDiff);
      ctx.fillRect(x, y, mapWidth, mapHeight);
    }

    function isFocused() {
      return Basil.get('focusvideo') != 'focus.none';
    }

    Engage.on(plugin.events.numberOfVideodisplaysSet.getName(), function (number) {
      var videoDisplays = $('.' + videoDisplayClass);
      if (Engage.model.get('meInfo').get('hide_video_context_menu')) {
        videoDisplays.on('contextmenu', function (e) {
          e.preventDefault();
        });
      }
      if (number > 1) {
        selector = '.videoFocused video';
        videoFocused = false;
        singleVideo = false;
        videoDisplays.on('click', function () {
          if (flag == 0) {
            Engage.trigger(plugin.events.focusVideo.getName(), Utils.getFlavorForVideoDisplay(this));
          }
        });
      }
    });

    Engage.on(plugin.events.togglePiP.getName(), function (pip) {
      if (pip && videoFocused) {
        selector = '.videoFocusedPiP video';
        mapSelector = '.videoFocusedPiP';
        setTimeout(redrawMinimap, zoomTimeout);
      } else if (!pip && videoFocused) {
        selector = '.videoFocused video';
        mapSelector = '.videoFocused';
        setTimeout(redrawMinimap, zoomTimeout);
      } else {
        selector = 'video';
      }
    });

    Engage.on(plugin.events.resetLayout.getName(), function (v) {
      videoFocused = false;
      selector = 'video';
      if (!singleVideo) {
        hideMinimap();
      }
    });

    Engage.on(plugin.events.focusVideo.getName(), function (v) {
      if (isPiP && !videoFocused) {
        videoFocused = true;
        selector = '.videoFocusedPiP video';
        mapSelector = '.videoFocusedPiP';
        if (isFocused()) {
          setTimeout(showMinimap, zoomTimeout);
        }
      } else if (!isPiP && !videoFocused) {
        selector = '.videoFocused video';
        videoFocused = true;
        mapSelector = '.videoFocused';
        if (isFocused()) {
          setTimeout(showMinimap, zoomTimeout);
        }
      } else if (!isPiP && videoFocused) {
        // Toggle non-PiP displays or leave focused mode while nonPiP
        if (singleVideo) {
          // While video with one display loaded this could occur
          videoFocused = false;
          selector = 'video';
          mapSelector = '.videoDisplay';
          setTimeout(showMinimap, zoomTimeout);
        } else {
          Engage.trigger(plugin.events.setZoomLevel.getName(), [1.0, true]);
          selector = '.videoFocused video';
          setTimeout(redrawMinimap, zoomTimeout);
        }
      } else if (isPiP && videoFocused) {
        // Toggle PiP displays or leave focused mode while PiP
        if (singleVideo) {
          // While video with one display loaded this could occur
          videoFocused = false;
          selector = 'video';
          mapSelector = '.videoDisplay';
          setTimeout(showMinimap, zoomTimeout);
        } else {
          // Reset before unfocus
          Engage.trigger(plugin.events.setZoomLevel.getName(), [1.0, true]);
          selector = '.videoFocusedPiP video';
          setTimeout(redrawMinimap, zoomTimeout);
        }
      } else {
        selector = 'video';
        hideMinimap();
      }
      // move display
      Engage.trigger(plugin.events.setZoomLevel.getName(), [1.0, true, true]);
    });

    $(selector).on('mousewheel', function (event) {
      if (wheelEvent != null) {
        if (event.timeStamp - wheelEvent.timeStamp < 30) {
          event.preventDefault();
          return;
        }
      }
      // scrolling stays available
      if (selector == 'video' && !singleVideo) {
        return;
      }
      // calculate mouse position
      var parentOffset = $(this).parent().offset();
      var relX = event.pageX - parentOffset.left;
      var relY = event.pageY - parentOffset.top;

      var vX = ($(this).width() / 2);
      var vY = ($(this).height() / 2);

      var xdiff = relX - vX;
      var ydiff = relY - vY;

      // zoom in
      if (event.shiftKey) {
        event.preventDefault();
        if (event.deltaY > 0) {
          Engage.trigger(events.setZoomLevel.getName(), [zoom_step_size]);
          // move towards mouse position
          var z = zoomLevels[zoomLevels.indexOf($(selector)[0].id) + 1];

          moveHorizontal(-((xdiff / 5) / z));
          moveVertical(-((ydiff / 5) / z));
        }

        // zoom out
        if (event.deltaY < 0) {
          Engage.trigger(events.setZoomLevel.getName(), [-zoom_step_size]);
        }
      } else {
        // show zoom overlay
        var overlay = document.getElementById('overlay'),
            overlaytext = document.getElementById('overlaytext'),
            videodisplay = document.getElementById('engage_video');
        overlaytext.innerText = translate('scroll_overlay_text', 'Use shift + scroll to zoom');
        overlay.style.display = 'block';
        overlay.style.top = videodisplay.offsetTop + 'px';
        overlay.style.height = videodisplay.offsetHeight + 'px';
        overlayTimer = setTimeout(function() {
          document.getElementById('overlay').style.display = 'none';
        }, 1500);
      }

      wheelEvent = event;
    });

    $(selector).mousedown(function () {
      flag = 0;
      $(selector).mousemove(function (event) {
        if (lastEvent != null) {
          flag = 1;
          var x_move = lastEvent.pageX - event.pageX;
          var y_move = lastEvent.pageY - event.pageY;
          Engage.trigger(plugin.events.moveHorizontal.getName(), -x_move);
          Engage.trigger(plugin.events.moveVertical.getName(), -y_move);
        }

        lastEvent = event;
      });
      $('body').mouseup(function () {
        $(selector).off('mousemove');
        lastEvent = null;
      });
    });

    Engage.on(plugin.events.moveHorizontal.getName(), function (step) {
      moveHorizontal(step);
    });

    Engage.on(plugin.events.moveVertical.getName(), function (step) {
      moveVertical(step);
    });

    function moveHorizontal(step) {
      if (videoFocused || singleVideo) {
        var offset = $(selector).css('left');
        var left = $(selector).position().left / 2;

        offset = offset.replace('px', '');
        offset = Number(offset);

        if (step > 0 && Math.abs($(selector).position().left) < step) {
          // Shift right, but too far
          step = Math.abs($(selector).position().left);
        }

        if (step < 0 && (offset + step < left)) {
          // Shift left but too far
          step = (left - offset);
        }

        if (!(($(selector).position().left + step) > 0) && !((offset + step) < left)) {
          $(selector).css('left', (offset + step) + 'px');
        }

        updateMinimap();
      }
    }

    function moveVertical(step) {
      if (videoFocused || singleVideo) {
        var top = $(selector).position().top / 2;
        var offset = $(selector).css('top');

        offset = offset.replace('px', '');
        offset = Number(offset);

        if (step > 0 && (Math.abs($(selector).position().top) < step)) {
          step = Math.abs($(selector).position().top);
        }

        if (step < 0 && (offset + step < top)) {
          step = (top - offset);
        }

        if (!((offset + step) < top) && !(($(selector).position().top + step) > 0)) {
          $(selector).css('top', (offset + step) + 'px');
        }

        updateMinimap();
      }
    }

    Engage.on(plugin.events.setZoomLevel.getName(), function (data) {
      var level = data[0];
      var fixed = data[1];
      var moveOnly = data[2];

      fixed = typeof fixed !== 'undefined' ? fixed : false;
      moveOnly = typeof moveOnly !== 'undefined' ? moveOnly : false;

      if ($(selector)[0] === undefined || level === undefined) {
        return;
      }

      if (zoomLevels.indexOf($(selector)[0].id) == -1) {
        if (1.0 + level >= 1.0) {
          if (!fixed) {
            level = (1.0 + level);
          }
          zoomLevels.push($(selector)[0].id, Math.abs(level));
        }
      } else {
        var before = parseFloat(zoomLevels[(zoomLevels.indexOf($(selector)[0].id) + 1)]);
        if ((before + level) >= 1.0) {
          if (!fixed) {
            level = (before + level);
          }
        }
      }

      if (Number(level).toFixed(decimal_places) == Number(1).toFixed(decimal_places) && minimapVisible) {
        hideMinimap();
      }

      if (Number(level).toFixed(decimal_places) >= Number(1).toFixed(decimal_places) && (videoFocused || singleVideo)) {
        var topTrans = Number($(selector).css('top').replace('px', ''));
        var leftTrans = Number($(selector).css('left').replace('px', ''));

        var biggerThenOne = Number(level).toFixed(decimal_places) > Number(1).toFixed(decimal_places);

        var leftOffset = ($(selector).width() * level - $(selector).width()) / 2;
        leftOffset = leftOffset - Math.abs(leftTrans);
        if (leftOffset < 0) {
          if (leftTrans > 0) {
            Engage.trigger(plugin.events.moveHorizontal.getName(), leftOffset);
          }
          if (leftTrans < 0) {
            Engage.trigger(plugin.events.moveHorizontal.getName(), -leftOffset);
          }
        }

        var topOffset = ($(selector).height() * level - $(selector).height()) / 2;
        topOffset = topOffset - Math.abs(topTrans);
        if (topOffset < 0) {
          if (topTrans > 0) {
            Engage.trigger(plugin.events.moveVertical.getName(), topOffset);
          }
          if (topTrans < 0) {
            Engage.trigger(plugin.events.moveVertical.getName(), -topOffset);
          }
        }

        var zoomLevel = Number(level).toFixed(decimal_places);

        if (!moveOnly) {
          $(selector)[0].style.transform = 'scale(' + zoomLevel + ')';
          zoomLevels[(zoomLevels.indexOf($(selector)[0].id) + 1)] = parseFloat(Number(level).toFixed(decimal_places));
          Engage.trigger(plugin.events.zoomChange.getName(), zoomLevel);

          if (!minimapVisible && biggerThenOne) {
            showMinimap();
          } else if (minimapVisible && biggerThenOne) {
            updateMinimap();
          }
        }
      }
    });

    Engage.on(plugin.events.zoomReset.getName(), function () {
      var tmpSelector = selector;
      for (var i = 0; i < zoomLevels.length; i++) {
        selector = $('#' + zoomLevels[i])[0];
        Engage.trigger(plugin.events.setZoomLevel.getName(), [1.0, true]);
        i++;
      }
      selector = tmpSelector;
    });

    Engage.on(plugin.events.zoomIn.getName(), function () {
      Engage.trigger(plugin.events.setZoomLevel.getName(), [zoom_step_size]);
    });

    Engage.on(plugin.events.zoomOut.getName(), function () {
      Engage.trigger(plugin.events.setZoomLevel.getName(), [-zoom_step_size]);
    });

    $(window).resize(function () {
      Engage.trigger(plugin.events.setZoomLevel.getName(), [1.0, true, true]);
      showMinimap();
    });
  }

  function initializeVideoJsGlobally(videoDataView, videoDisplays, tuples) {
    Engage.log('Video: Preparing video.js video displays globally.');
    for (var i = 0; i < tuples.length; ++i) {
      var value = tuples[i][1];

      globalVideoSource.push([videoDisplays[i], value]);

      initVideojsVideo(videoDisplays[i], value, videoDataView.videojs_swf);
    }
  }

  function calculateAspectRatioForVideos(videoDataView, videoDisplays, aspectRatio) {
    Engage.log('Video: Aspect ratio: ' + aspectRatio[1] + 'x' + aspectRatio[2] + ' == ' + ((aspectRatio[1] / aspectRatio[2]) * 100));
    Engage.trigger(plugin.events.aspectRatioSet.getName(), [aspectRatio[1], aspectRatio[2], (aspectRatio[1] / aspectRatio[2]) * 100]);
    $('.' + id_videoDisplayClass)
      .css('width', (((1 / videoDisplays.length) * 100) - 0.5) + '%')
      .each(function (index) {
        if ((index % 2) === 1) {
          $(videoDataView).css('float', 'right');
        }
      });
    for (var j = 0; j < videoDisplays.length; ++j) {
      $('#' + videoDisplays[j]).css('padding-top', (aspectRatio[2] / aspectRatio[1] * 100) + '%').addClass('auto-height');
      singleVideoPaddingTop = (aspectRatio[2] / aspectRatio[1] * 100) + '%';
    }
  }

  Engage.on(plugin.events.aspectRatioSet.getName(), function (param) {
    if (param === undefined && aspectRatio) {
      Engage.trigger(plugin.events.aspectRatioSet.getName(), [aspectRatio[1], aspectRatio[2], (aspectRatio[1] / aspectRatio[2]) * 100]);
    }
  })

  function synchronizeVideos(videoDisplays) {
    registerSynchronizeEvents();

    var cnt = 0;
    for (var vd in videoDisplays) {
      if (cnt > 0) {
        // sync every other videodisplay with the master
        $.synchronizeVideos(0, videoDisplays[0], videoDisplays[vd]);
        Engage.log('Video: Videodisplay ' + vd + ' is now being synchronized with the master videodisplay');
      }
      ++cnt;
    }
    initSynchronize();
  }

  function renderDesktop(videoDataView, videoSources, videoDisplays, aspectRatio) {
    Engage.log('Video: Rendering for desktop view');

    var tuples = getSortedVideosourcesArray(videoSources);

    initializeVideoJsGlobally(videoDataView, videoDisplays, tuples);

    /* set first videoDisplay as master */
    var videoDisplay = isAudioOnly ? id_audioDisplay : videoDisplays[0];
    videodisplayMaster = videojs(videoDisplay);

    initQualities(videoDataView);

    if ((aspectRatio != null) && (videoDisplays.length > 0)) {
      calculateAspectRatioForVideos(videoDataView, videoDisplays, aspectRatio);
      registerZoomLevelEvents();
    } else {
      Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
    }

    // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
    $('.' + class_vjsposter).show();

    Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

    if (videoDisplays.length > 0) {
      var nr = tuples.length;

      registerEvents(videoDisplay, videoDisplays.length);

      if (nr >= 2) {
        synchronizeVideos(videoDisplays);
      } else {
        videosReady = true;
        if (!isAudioOnly) {
          Engage.trigger(plugin.events.ready.getName());
        }
      }

      if (videoDataView.model.get('type') !== 'audio') {
        $(window).resize(function () {
          checkVideoDisplaySize();
        });
      }
    }
  }

  function appendEmbedPlayer_switchPlayers(videoDisplays) {
    $('.' + class_vjs_remaining_time).after("<div id=\"" + id_btn_switchPlayer + "\" class=\"" + class_vjs_switchPlayer + " vjs-menu-button vjs-menu-button-popup vjs-control vjs-button\" tabindex=\"0\" role=\"menuitem\" aria-live=\"polite\" aria-expanded=\"false\" aria-haspopup=\"true\">");

    var uls = '';
    for (var i = 0; i < videoDisplays.length; ++i) {
      uls += "<li id=\"btn-video" + (i + 1) + "\" class=\"vjs-menu-item " + class_vjs_menu_item + " " + class_btn_video + "\" tabindex=\"-1\" role=\"menuitem\" aria-live=\"polite\">" + translate("video", "Video") + " " + (i + 1) + "</li>";
    }

    $('#' + id_btn_switchPlayer).append(
      "<div class=\"vjs-menu\" role=\"presentation\">" +
      "<ul class=\"" + class_vjs_menu_content + "\" role=\"menu\">" +
      uls +
      "</ul>" +
      "</div>" +
      "<span class=\"" + class_vjs_control_text + "\">" + translate("switchPlayer", "Switch player") + "</span>" +
      "<div id=\"" + id_switchPlayer_value + "\" class=\"" + class_vjs_switchPlayer_value + "\">" + "Vid. 1" + "</div>"
    );

    for (var j = 0; j < videoDisplays.length; ++j) {
      $('#btn-video' + (j + 1)).click(function (k) {
        return function () {
          $('#' + id_switchPlayer_value).html(translate('video_short', 'Vid.') + ' ' + (k + 1));
          currentlySelectedVideodisplay = k;
          videojs(globalVideoSource[0].id).src(globalVideoSource[k].src);
        };
      }(j));
    }
  }

  function appendEmbedPlayer_openInPlayer() {
    $('.' + class_vjs_remaining_time).after('<button id="' + id_btn_openInPlayer + '" class="' + class_vjs_openInPlayer + ' vjs-control vjs-button" type="button" aria-live="polite"></button>');
    $('.' + class_vjs_openInPlayer).append('<span class="' + class_vjs_control_text + ' vjs-control-text">' + translate('openInPlayer', 'Open in player') + '</span>');

    $('#' + id_btn_openInPlayer).click(function (e) {
      e.preventDefault();
      var str = window.location.href;
      if (str.indexOf('mode=embed') == -1) {
        str += '&mode=embed';
      } else {
        str = Utils.replaceAll(str, 'mode=embed', 'mode=desktop');
      }
      Engage.trigger(plugin.events.pause.getName(), false);
      window.open(str, '_blank');
    });
  }

  function renderEmbed(videoDataView, videoSources, videoDisplays, aspectRatio) {
    Engage.log('Video: Rendering for embeded view');
    var init = false;

    var tuples = getSortedVideosourcesArray(videoSources);
    for (var i = 0; i < tuples.length; ++i) {
      var value = tuples[i][1];

      if (!init) { // just init the first video
        init = true;
        initVideojsVideo(videoDisplays[i], value, videoDataView.videojs_swf);
      }
      globalVideoSource.push({
        id: videoDisplays[0],
        src: value
      });
    }

    /* set first videoDisplay as master */
    var videoDisplay = isAudioOnly ? id_audioDisplay : videoDisplays[0];
    videodisplayMaster = videojs(videoDisplay);

    if ((videoDisplays.length > 1) && (globalVideoSource.length > 1)) {
      appendEmbedPlayer_switchPlayers(videoDisplays);
    }
    appendEmbedPlayer_openInPlayer();

    if ((aspectRatio != null) && (videoDisplays.length > 0)) {
      aspectRatio[1] = parseInt(aspectRatio[1]);
      aspectRatio[2] = parseInt(aspectRatio[2]);
      Engage.log('Video: Aspect ratio: ' + aspectRatio[1] + 'x' + aspectRatio[2] + ' == ' + ((aspectRatio[2] / aspectRatio[1]) * 100));
      Engage.trigger(plugin.events.aspectRatioSet.getName(), [aspectRatio[1], aspectRatio[2], (aspectRatio[1] / aspectRatio[2]) * 100]);
      $('.' + id_videoDisplayClass).css('width', '100%');
      for (var i = 0; i < videoDisplays.length; ++i) {
        $('#' + videoDisplays[i]).css('padding-top', (aspectRatio[2] / aspectRatio[1] * 100) + '%').addClass('auto-height');
        singleVideoPaddingTop = (aspectRatio[2] / aspectRatio[1] * 100) + '%';
      }
    } else {
      Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
    }

    // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
    $('.' + class_vjsposter).show();
    Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

    if (videoDisplays.length > 0) {
      registerEvents(videoDisplay, 1);

      videosReady = true;
      Engage.trigger(plugin.events.ready.getName());

      if (videoDataView.model.get('type') != 'audio') {
        $(window).resize(function () {
          checkVideoDisplaySize();
        });
      }
    }
  }

  function renderMobile(videoDataView, videoSources, videoDisplays, aspectRatio) {
    var tuples = getSortedVideosourcesArray(videoSources);
    initializeVideoJsGlobally(videoDataView, videoDisplays, tuples);

    /* set first videoDisplay as master */
    var videoDisplay = isAudioOnly ? id_audioDisplay : videoDisplays[0];
    videodisplayMaster = videojs(videoDisplay);

    initQualities(videoDataView);

    if ((aspectRatio != null) && (videoDisplays.length > 0)) {
      aspectRatio[1] = parseInt(aspectRatio[1]);
      aspectRatio[2] = parseInt(aspectRatio[2]);
      Engage.log('Video: Aspect ratio: ' + aspectRatio[1] + 'x' + aspectRatio[2] + ' == ' + ((aspectRatio[2] / aspectRatio[1]) * 100));
      Engage.trigger(plugin.events.aspectRatioSet.getName(), aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100);
      $('.' + id_videoDisplayClass).css('width', '100%');
      for (var j = 0; j < videoDisplays.length; ++j) {
        $('#' + videoDisplays[j]).css('padding-top', (aspectRatio[2] / aspectRatio[1] * 100) + '%').addClass('auto-height');
      }
    } else {
      Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
    }

    // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
    $('.' + class_vjsposter).show();
    Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

    if (videoDisplays.length > 0) {
      registerEvents(videoDisplay, videoDisplays.length);

      videosReady = true;
      Engage.trigger(plugin.events.ready.getName());

      if (videoDataView.model.get('type') != 'audio') {
        $(window).resize(function () {
          checkVideoDisplaySize();
        });
      }
    }
  }

  function calculateAspectRatio(videoSources) {
    Engage.log('Video: Calculating Aspect ratio');
    var as1 = 0;
    for (var flavor in videoResultions) {
      if ((aspectRatio == null) || (as1 < videoResultions[flavor])) {
        as1 = videoResultions[flavor][1];
        aspectRatio = videoResultions[flavor];
        id_generated_videojs_flash_component = 'videojs_videodisplay_' + flavor + '_flash_api';
      }
    }
    for (var v in videoSources) {
      for (var j = 0; j < videoSources[v].length; ++j) {
        var aspectRatio_tmp = videoSources[v][j].resolution;
        var t_tmp = $.type(aspectRatio_tmp);
        if ((t_tmp === 'string') && (/\d+x\d+/.test(aspectRatio_tmp))) {
          aspectRatio_tmp = aspectRatio_tmp.match(/(\d+)x(\d+)/);
          if ((aspectRatio == null) || (as1 < parseInt(aspectRatio_tmp[1]))) {
            as1 = parseInt(aspectRatio_tmp[1]);
            aspectRatio = Utils.parseVideoResolution(videoSources[v][j].resolution);
          }
        }
      }
    }
    Engage.log('Video: Calculated aspect ratio: ' + aspectRatio);
  }

  function renderVideoDisplay(videoDataView) {
    Engage.log('Video: Rendering video displays');
    var videoDisplays = videoDataView.model.get('ids');
    var videoSources = videoDataView.model.get('videoSources');

    var src = (videoSources && videoSources['audio']) ? videoSources['audio'] : [];
    var tempVars = {
      ids: videoDataView.model.get('ids'),
      type: videoDataView.model.get('type'),
      sources: src,
      str_error_AudioCodecNotSupported: translate('error_AudioCodecNotSupported', 'Error: The audio codec is not supported by this browser.'),
      str_error_AudioElementNotSupported: translate('error_AudioElementNotSupported', 'Error: Your browser does not support the audio element.')
    };
    if (isEmbedMode && !isAudioOnly) {
      tempVars.id = videoDataView.model.get('ids')[0];
    }

    // compile template and load into the html
    var template = _.template(videoDataView.template);
    videoDataView.$el.html(template(tempVars));

    if (!mediapackageError) {
      calculateAspectRatio(videoSources);

      isAudioOnly = videoDataView.model.get('type') == 'audio';
      Engage.trigger(plugin.events.isAudioOnly.getName(), isAudioOnly);

      if (videoSources && videoDisplays) {
        if (isEmbedMode) {
          renderEmbed(videoDataView, videoSources, videoDisplays, aspectRatio);
        } else if (isMobileMode) {
          renderMobile(videoDataView, videoSources, videoDisplays, aspectRatio);
        } else { // isDesktopMode
          renderDesktop(videoDataView, videoSources, videoDisplays, aspectRatio);
        }
        if (videoDataView.model.get('type') != 'audio') {
          delayedCalculateVideoAreaAspectRatio();
        }
      }
    }
    console.log(videodisplayMaster);

    loadAndAppendCaptions(videoDataView);
  }

  function prepareRenderingVideoDisplay(videoDataView) {
    if (loadHls) videoDisplayReady++;
    if (loadDash) videoDisplayReady++;
    if (loadHls) {
      require([relative_plugin_path + hlsPath], function () {
        Engage.log('Video: Lib videojs HLS playback loaded');
        videoDisplayReady--;
        renderVideoDisplayIfReady(videoDataView);
      });
    }
    if (loadDash) {
      require([relative_plugin_path + dashPath], function () {
        require([relative_plugin_path + dashPluginPath], function () {
          Engage.log('Video: Lib videojs DASH playback loaded');
          videoDisplayReady--;
          renderVideoDisplayIfReady(videoDataView);
        });
      });
    }

    renderVideoDisplayIfReady(videoDataView);
  }

  function renderVideoDisplayIfReady(videoDataView) {
    if (videoDisplayReady === 0) {
      renderVideoDisplay(videoDataView);
    }
  }

  var VideoDataView = Backbone.View.extend({
    el: $('#' + id_engage_video),
    initialize: function (videoDataModel, template, videojs_swf) {
      this.setElement($(plugin.container));
      this.model = videoDataModel;
      this.template = template;
      this.videojs_swf = videojs_swf;
      _.bindAll(this, 'render');
      this.model.bind('change', this.render);
      this.render();
    },
    render: function () {
      prepareRenderingVideoDisplay(this);
    }
  });

  function initVideojsVideo(id, videoSource, videojs_swf) {
    Engage.log('Video: Initializing video.js-display ' + id);

    if (id) {
      if (videoSource) {
        if (!isAudioOnly) {
          var videoOptions = {
            controls: false,
            autoplay: false,
            preload: 'auto',
            poster: videoSource.poster ? videoSource.poster : '',
            loop: false,
            width: '100%',
            height: '100%'
          };
          if (isEmbedMode) {
            videoOptions.controls = true;
          }

          // init video.js
          videojs(id, videoOptions, function () {
            var videodisplay = this;
            if (videoSource.length == 1 || foundQualities == undefined || foundQualities.length == 0) {
              // set sources if there
              //   * is only a single video (other video sets could have quality tags)
              //   * are no quality tags in any video set
              videodisplay.src(videoSource);
            }
          });

          // URL to the flash swf
          if (videojs_swf) {
            Engage.log('Video: Loaded flash component');
            videojs.options.flash.swf = videojs_swf;
          } else {
            Engage.log('Video: No flash component loaded');
          }
          isUsingFlash = $('#' + id_generated_videojs_flash_component).length > 0;
          Engage.trigger(plugin.events.usingFlash.getName(), isUsingFlash);
        }
      } else {
        Engage.log('Video: Error: No video source available');
        $('#' + id_videojs_wrapper).html('No video sources available.');
      }
    } else {
      Engage.log('Video: Error: No ID available');
      $('#' + id_videojs_wrapper).html('No video available.');
    }
  }

  function delayedCalculateVideoAreaAspectRatio() {
    calculateVideoAreaAspectRatio();
    window.setTimeout(calculateVideoAreaAspectRatio, checkVideoDisplaySizeTimeout);
  }

  function calculateVideoAreaAspectRatio() {
    var $engageVideoId = $('#' + id_engage_video);
    var oldAspectRatio = videoAreaAspectRatio;

    var videoHeight = $engageVideoId.height();
    var videoWidth = $engageVideoId.width();
    if (videoWidth !== undefined && videoHeight !== undefined &&
        videoWidth === 0 && videoHeight === 0) {
        return;
    }
    if (isEmbedMode) {
      if (videoWidth !== undefined && videoHeight !== undefined &&
          videoWidth > 0 && videoHeight > 0) {
        videoAreaAspectRatio = videoWidth / videoHeight;
      }
    } else if (!isDefaultLayout()) {
      videoHeight = $('.' + videoFocusedClass).height();
      if (isPiP) {
        videoWidth = $('.' + videoFocusedClass).width();
      }
    }
    if (videoWidth !== undefined && videoHeight !== undefined &&
        videoWidth > 0 && videoHeight > 0) {
      videoAreaAspectRatio = videoWidth / videoHeight;
    }

    if (videoAreaAspectRatio !== oldAspectRatio) {
      checkVideoDisplaySize();
    }
  }

  function checkVideoDisplaySize() {
    var $engageVideoId = $('#' + id_engage_video);
    var $videoUnfocused = $('.' + videoUnfocusedClass);

    var videoHeight = $engageVideoId.height();

    if (!isMobileMode) {
      var controlsHeight = ($('#' + id_resize_container).height() - videoHeight) + 5;
      if (controlsHeight <= 0) {
        controlsHeight = $('#' + id_engageControls).height() + 30;
      }
      var maxVideoAreaHeight = $(window).height() - controlsHeight;
    } else {
      var maxVideoAreaHeight = $(window).height();
    }

    if (isEmbedMode) {
      maxVideoAreaHeight = $(window).height();
    }

    if (videoAreaAspectRatio === undefined && !isMobileMode) {
      calculateVideoAreaAspectRatio();
    }

    if (!isMobileMode) {
      var maxVideoAreaWidth = parseInt(maxVideoAreaHeight * videoAreaAspectRatio);
      var minVideoAreaHeight = parseInt(parseInt($engageVideoId.css('min-width')) / videoAreaAspectRatio);
    } else {
      var maxVideoAreaWidth = parseInt(maxVideoAreaHeight * (aspectRatio[1] / aspectRatio[2]));
      var minVideoAreaHeight = parseInt(parseInt($engageVideoId.css('min-width')) / (aspectRatio[1] / aspectRatio[2]));
    }

    var minWidth = parseInt($engageVideoId.css('min-width'));
    if (maxVideoAreaWidth > minWidth) {
      if (maxVideoAreaWidth > $(window).width()) {
        $engageVideoId.css('max-width', $(window).width() + 'px');
      } else {
        $engageVideoId.css('max-width', maxVideoAreaWidth + 'px');
      }
    } else {
      $engageVideoId.css('max-width', minWidth + 'px');
    }
    $engageVideoId.css('min-height', minVideoAreaHeight + 'px');
    if (maxVideoAreaHeight > minVideoAreaHeight) {
      $engageVideoId.css('max-height', maxVideoAreaHeight + 'px');
    } else {
      $engageVideoId.css('max-height', minVideoAreaHeight + 'px');
    }

    if (!isDefaultLayout()) {
      if (isPiP) {
        var distance = 0;
        $videoUnfocused.each(function () {
          var width = $(this).width();
          var height = $(this).height();
          $(this).css('left', 0 - (width / 2) + 'px');
          $(this).css('top', distance - (height / 2) + 'px');
          distance = distance + height + 10;
        });
        var marginLeft;
        if (pipPos === 'left') {
          marginLeft = 12;
        } else {
          marginLeft = 88;
        }
        $videoUnfocused.css('margin-left', marginLeft + '%');
      } else {
        $engageVideoId.height($('.' + videoFocusedClass).height());
      }
    }
  }

  function clearAutoplay() {
    window.clearInterval(interval_autoplay);
  }

  function clearInitialSeek() {
    window.clearInterval(interval_initialSeek);
  }

  function changePlaybackRate(value, videodisplayMaster) {
    if (pressedPlayOnce) {
      var rate = videodisplayMaster.playbackRate();
      Engage.trigger(plugin.events.playbackRateChanged.getName(), (rate + value));
    }
  }

  function startAudioPlayer(audio) {
    clearAutoplay();
    audio.play();
    pressedPlayOnce = true;
  }

  function registerEventsAudioOnly(videoDisplay) {
    var video_display = $('#' + videoDisplay);
    var audioPlayer_id = video_display.find('audio');
    var audioPlayer = video_display[0].player;
    var audioLoadTimeout = window.setTimeout(function () {
      Engage.trigger(plugin.events.audioCodecNotSupported.getName());
      $('.' + class_audioDisplay).hide();
      $('.' + class_audioDisplayError).show();
    }, audioLoadTimeoutCheckDelay);
    audioPlayer_id.on('canplay', function () {
      videosReady = true;
      Engage.trigger(plugin.events.ready.getName());
      window.clearTimeout(audioLoadTimeout);
    });
    audioPlayer_id.on('play', function () {
      Engage.trigger(plugin.events.play.getName(), true);
      pressedPlayOnce = true;
    });
    audioPlayer_id.on('pause', function () {
      Engage.trigger(plugin.events.pause.getName(), true);
    });
    audioPlayer_id.on('ended', function () {
      Engage.trigger(plugin.events.ended.getName(), true);
    });
    audioPlayer_id.on('timeupdate', function () {
      Engage.trigger(plugin.events.timeupdate.getName(), audioPlayer.currentTime, true);
    });
    audioPlayer_id.on(event_html5player_volumechange, function () {
      Engage.trigger(plugin.events.volumechange.getName(), audioPlayer.volume * 100);
    });
    Engage.on(plugin.events.play.getName(), function (triggeredByMaster) {
      if (!triggeredByMaster && videosReady) {
        startAudioPlayer(audioPlayer);
      }
    });
    Engage.on(plugin.events.autoplay.getName(), function () {
      interval_autoplay = window.setInterval(function () {
        if (pressedPlayOnce) {
          clearAutoplay();
        } else if (videosReady) {
          audioPlayer.play();
          clearAutoplay();
        }
      }, interval_autoplay_ms);
    });
    Engage.on(plugin.events.initialSeek.getName(), function (e) {
      parsedSeconds = Utils.parseSeconds(e);
      interval_initialSeek = window.setInterval(function () {
        if (pressedPlayOnce) {
          clearInitialSeek();
        } else if (videosReady) {
          audioPlayer.play();
          window.setTimeout(function () {
            Engage.trigger(plugin.events.seek.getName(), parsedSeconds);
          }, timeout_initialSeek_ms);
          clearInitialSeek();
        }
      }, interval_initialSeek_ms);
    });
    Engage.on(plugin.events.pause.getName(), function (triggeredByMaster) {
      if (!triggeredByMaster && pressedPlayOnce) {
        clearAutoplay();
        audioPlayer.pause();
      }
    });
    Engage.on(plugin.events.playPause.getName(), function () {
      if (audioPlayer.paused()) {
        Engage.trigger(plugin.events.play.getName());
      } else {
        Engage.trigger(plugin.events.pause.getName());
      }
    });
    Engage.on(plugin.events.seekLeft.getName(), function () {
      if (pressedPlayOnce) {
        var currTime = audioPlayer.currentTime();
        if ((currTime - seekSeconds) >= 0) {
          Engage.trigger(plugin.events.seek.getName(), currTime - seekSeconds);
        } else {
          Engage.trigger(plugin.events.seek.getName(), 0);
        }
      }
    });
    Engage.on(plugin.events.seekRight.getName(), function () {
      if (pressedPlayOnce) {
        var currTime = audioPlayer.currentTime();
        var duration = parseInt(Engage.model.get('videoDataModel').get('duration')) / 1000;
        if (duration && ((currTime + seekSeconds) < duration)) {
          Engage.trigger(plugin.events.seek.getName(), currTime + seekSeconds);
        } else {
          Engage.trigger(plugin.events.seek.getName(), duration);
        }
      }
    });
    Engage.on(plugin.events.playbackRateIncrease.getName(), function () {
      changePlaybackRate(0.125, videodisplayMaster);
    });
    Engage.on(plugin.events.playbackRateDecrease.getName(), function () {
      changePlaybackRate(-0.125, videodisplayMaster);
    });
    Engage.on(plugin.events.volumeSet.getName(), function (volume) {
      if ((volume >= 0) && (volume <= 1)) {
        Engage.log('Video: Volume changed to ' + volume);
        audioPlayer.volume = volume;
      }
    });
    Engage.on(plugin.events.volumeGet.getName(), function (callback) {
      callback(audioPlayer.volume);
    });
    Engage.on(plugin.events.timeupdate.getName(), function (time) {
      currentTime = time;
    });
    Engage.on(plugin.events.seek.getName(), function (time) {
      Engage.log('Video: Seek to ' + time);
      if (videosReady) {
        if (! pressedPlayOnce) {
            startAudioPlayer(audioPlayer);
        }
        var duration = parseInt(Engage.model.get('videoDataModel').get('duration')) / 1000;
        if (duration && (time < duration)) {
          audioPlayer.currentTime = time;
        } else {
          Engage.trigger(plugin.events.customError.getName(), translate('givenTime', 'The given time') + ' (' + Utils.formatSeconds(time) + ') ' + translate('hasToBeSmallerThanDuration', 'has to be smaller than the duration') + ' (' + Utils.formatSeconds(duration) + ').');
          Engage.trigger(plugin.events.timeupdate.getName(), audioPlayer.currentTime);
        }
      } else {
        Engage.trigger(plugin.events.customNotification.getName(), translate('msg_waitToSetTime', 'Please wait until the video has been loaded to set a time.'));
        Engage.trigger(plugin.events.timeupdate.getName(), 0);
      }
    });
    Engage.on(plugin.events.sliderStop.getName(), function (time) {
      Engage.log('Video: Slider stopped at ' + time);
      if (videosReady) {
        if (! pressedPlayOnce) {
            startAudioPlayer(audioPlayer);
        }
        var duration = parseInt(Engage.model.get('videoDataModel').get('duration'));
        audioPlayer.currentTime = (time / 1000) * (duration / 1000);
      } else {
        Engage.trigger(plugin.events.customNotification.getName(), translate('msg_startPlayingToSeek', 'Please start playing the video once to seek.'));
        Engage.trigger(plugin.events.timeupdate.getName(), 0);
      }
    });
    Engage.on(plugin.events.ended.getName(), function (time) {
      if (videosReady) {
        Engage.log('Video: Ended at ' + time);
        audioPlayer.pause();
        Engage.trigger(plugin.events.pause.getName());
        audioPlayer.currentTime = audioPlayer.duration;
      }
    });
  }

  function startVideoPlayer(video) {
    $('.' + class_vjsposter).detach();
    clearAutoplay();
    video.play();
    pressedPlayOnce = true;
  }

  function registerEventsVideo(videoDisplay, numberOfVideodisplays) {
    $(document).on('webkitfullscreenchange mozfullscreenchange fullscreenchange MSFullscreenChange', function () {
      fullscreen = !fullscreen;
      if (fullscreen) {
        Engage.trigger(plugin.events.fullscreenEnable.getName());
      } else {
        Engage.trigger(plugin.events.fullscreenCancel.getName());
      }
    });

    var $videoDisplay = $('#' + videoDisplay);

    if (!isMobileMode) {
      videodisplayMaster.on('play', function () {
          startVideoPlayer(videodisplayMaster);
      });
      videodisplayMaster.on('pause', function () {
          Engage.trigger(plugin.events.pause.getName(), true);
      });
      videodisplayMaster.on('ended', function () {
          Engage.trigger(plugin.events.ended.getName(), true);
      });
      videodisplayMaster.on('timeupdate', function () {
          Engage.trigger(plugin.events.timeupdate.getName(), videodisplayMaster.currentTime(), true);
      });
    } else {
      // To get rid of the undesired "click on poster to play" functionality,
      // we remove all event listeners attached to the vjs posters by cloning the dom element.
      $('.' + class_vjsposter).replaceWith(function () {
        return $(this).clone();
      });

      // register events on every video display in mobile mode
      // because only one display is playing at the same time
      Engage.model.get('videoDataModel').get('ids').forEach(function (id) {
        videojs(id).on('play', function () {
          Engage.trigger(plugin.events.play.getName(), true);
        });
        videojs(id).on('ended', function () {
          Engage.trigger(plugin.events.ended.getName(), true);
        });
        videojs(id).on('timeupdate', function () {
          Engage.trigger(plugin.events.timeupdate.getName(), videodisplayMaster.currentTime(), true);
        });
      });
    }

    $('#' + id_btn_fullscreenCancel).click(function (e) {
      e.preventDefault();
      Engage.trigger(plugin.events.fullscreenCancel.getName());
    });

    Engage.on(plugin.events.fullscreenEnable.getName(), function () {
      if (numberOfVideodisplays === 1) {
        videodisplayMaster.requestFullscreen();
        $('#' + videoDisplay).css('padding-top', '0px');
      } else if (!fullscreen) {
        if (!isMobileMode) {
          var viewer = document.getElementById(id_engage_video_fullsceen_wrapper);
        } else {
          var viewer = document.getElementById(id_video_wrapper);
        }
        if (viewer.mozRequestFullScreen) {
          viewer.mozRequestFullScreen();
        } else if (viewer.webkitRequestFullscreen) {
          viewer.webkitRequestFullscreen();
        } else if (viewer.requestFullscreen) {
          viewer.requestFullscreen();
        } else if (viewer.msRequestFullscreen) {
          viewer.msRequestFullscreen();
        } else {
          $(window).scrollTop(0);
          $('body').css('overflow', 'hidden');
          $(window).scroll(function () {
            $(this).scrollTop(0);
          });
          $('#' + id_engage_video).css('z-index', 995).css('position', 'relative');
          $('#' + id_page_cover).css('opacity', 0.9).fadeIn(300);
          fullscreen = true;
        }
      }
      if (!isMobileMode) {
        $('#' + videoDisplay).removeClass('vjs-controls-disabled').addClass('vjs-controls-enabled');
        $('.' + id_videoDisplayClass).css('max-width', $(window).height() * videoAreaAspectRatio);
      }
    });
    Engage.on(plugin.events.fullscreenCancel.getName(), function () {
      if (numberOfVideodisplays === 1) {
        $('#' + videoDisplay).css('padding-top', singleVideoPaddingTop);
      }
      if (fullscreen && (numberOfVideodisplays > 1)) {
        if (document.mozCancelFullScreen) {
          document.mozCancelFullScreen();
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        } else if (document.exitFullscreen) {
          document.exitFullscreen();
        } else if (document.msExitFullscreen) {
          document.msExitFullscreen();
        } else {
          $('body').css('overflow', 'auto');
          $(window).unbind('scroll');
          $('#' + id_page_cover).css('opacity', 0.9).fadeOut(300, function () {
            $('#' + id_engage_video).css('z-index', 0).css('position', '');
          });
          fullscreen = false;
        }
      }
      if(!isMobileMode) {
        $('#' + videoDisplay).removeClass('vjs-controls-enabled').addClass('vjs-controls-disabled');
      }
      $('.' + id_videoDisplayClass).css('max-width', '');
      checkVideoDisplaySize();
    });

    Engage.on(plugin.events.playbackRateChanged.getName(), function (rate) {
      if (pressedPlayOnce) {
        Engage.log('Video: Playback rate changed to rate ' + rate);
        videodisplayMaster.playbackRate(rate);
      }
    });

    Engage.on(plugin.events.play.getName(), function (triggeredByMaster) {
      if (!triggeredByMaster && videosReady) {
          startVideoPlayer(videodisplayMaster);
      }
    });

    Engage.on(plugin.events.autoplay.getName(), function () {
      interval_autoplay = window.setInterval(function () {
        if (pressedPlayOnce) {
          clearAutoplay();
        } else if (videosReady) {
          videodisplayMaster.play();
          clearAutoplay();
        }
      }, interval_autoplay_ms);
    });

    Engage.on(plugin.events.initialSeek.getName(), function (e) {
      parsedSeconds = Utils.parseSeconds(e);
      interval_initialSeek = window.setInterval(function () {
        if (pressedPlayOnce) {
          clearInitialSeek();
        } else if (videosReady) {
          videodisplayMaster.play();
          window.setTimeout(function () {
            Engage.trigger(plugin.events.seek.getName(), parsedSeconds);
          }, timeout_initialSeek_ms);
          clearInitialSeek();
        }
      }, interval_initialSeek_ms);
    });

    Engage.on(plugin.events.pause.getName(), function (triggeredByMaster) {
      if (!triggeredByMaster && pressedPlayOnce) {
        clearAutoplay();
        videodisplayMaster.pause();
      }
    });

    Engage.on(plugin.events.playPause.getName(), function () {
      if (videodisplayMaster.paused()) {
        Engage.trigger(plugin.events.play.getName());
      } else {
        Engage.trigger(plugin.events.pause.getName());
        if (isMobileMode && fullscreen) {
          Engage.trigger(plugin.events.fullscreenCancel.getName());
        }
      }
    });
    Engage.on(plugin.events.seekLeft.getName(), function () {
      if (pressedPlayOnce) {
        var currTime = videodisplayMaster.currentTime();
        if ((currTime - seekSeconds) >= 0) {
          Engage.trigger(plugin.events.seek.getName(), currTime - seekSeconds);
        } else {
          Engage.trigger(plugin.events.seek.getName(), 0);
        }
      }
    });

    Engage.on(plugin.events.seekRight.getName(), function () {
      if (pressedPlayOnce) {
        var currTime = videodisplayMaster.currentTime();
        var duration = parseInt(Engage.model.get('videoDataModel').get('duration')) / 1000;
        if (duration && ((currTime + seekSeconds) < duration)) {
          Engage.trigger(plugin.events.seek.getName(), currTime + seekSeconds);
        } else {
          Engage.trigger(plugin.events.seek.getName(), duration);
        }
      }
    });

    Engage.on(plugin.events.playbackRateIncrease.getName(), function () {
      changePlaybackRate(0.125, videodisplayMaster);
    });

    Engage.on(plugin.events.playbackRateDecrease.getName(), function () {
      changePlaybackRate(-0.125, videodisplayMaster);
    });

    Engage.on(plugin.events.volumeSet.getName(), function (volume) {
      if ((volume >= 0) && (volume <= 1)) {
        Engage.log('Video: Volume changed to ' + volume);
        videodisplayMaster.volume(volume);
      }
    });

    Engage.on(plugin.events.volumeGet.getName(), function (callback) {
      if (callback) {
        callback(videodisplayMaster.volume());
      }
    });

    Engage.on(plugin.events.timeupdate.getName(), function (time) {
      currentTime = time;
    });

    Engage.on(plugin.events.seek.getName(), function (time) {
      Engage.log('Video: Seek to ' + time);
      if (videosReady) {
        if (!pressedPlayOnce) {
          startVideoPlayer(videodisplayMaster);
        }
        var duration = parseInt(Engage.model.get('videoDataModel').get('duration')) / 1000;
        if (duration && (time < duration)) {
          videodisplayMaster.currentTime(time);
        } else {
          Engage.trigger(plugin.events.customError.getName(), translate('givenTime', 'The given time') + ' (' + Utils.formatSeconds(time) + ') ' + translate('hasToBeSmallerThanDuration', 'has to be smaller than the duration') + ' (' + Utils.formatSeconds(duration) + ').');
          Engage.trigger(plugin.events.timeupdate.getName(), videodisplayMaster.currentTime());
        }
      } else {
        Engage.trigger(plugin.events.customNotification.getName(), translate('msg_waitToSetTime', 'Please wait until the video has been loaded to set a time.'));
        Engage.trigger(plugin.events.timeupdate.getName(), 0);
      }
    });

    Engage.on(plugin.events.sliderStop.getName(), function (time) {
      if (videosReady) {
        if (!pressedPlayOnce) {
          Engage.trigger(plugin.events.play.getName(), false);
        }
        var duration = parseInt(Engage.model.get('videoDataModel').get('duration'));
        var normTime = (time / 1000) * (duration / 1000);
        videodisplayMaster.currentTime(normTime);
      } else {
        Engage.trigger(plugin.events.customNotification.getName(), translate('msg_startPlayingToSeek', 'Please start playing the video once to seek.'));
        Engage.trigger(plugin.events.timeupdate.getName(), 0);
      }
    });

    Engage.on(plugin.events.ended.getName(), function () {
      if (videosReady) {
        Engage.log('Video: Video ended and ready');
        videodisplayMaster.pause();
        Engage.trigger(plugin.events.pause.getName());
        videodisplayMaster.currentTime(0);
        if (isMobileMode) {
          Engage.trigger(plugin.events.fullscreenCancel.getName());
        }
      }
    });

    videodisplayMaster.on(event_html5player_volumechange, function () {
        Engage.trigger(plugin.events.volumechange.getName(), videodisplayMaster.volume());
    });
    videodisplayMaster.on(event_html5player_fullscreenchange, function () {
        Engage.trigger(plugin.events.fullscreenChange.getName());
    });

    var $videoDisplayClass = $('.' + id_videoDisplayClass);

    if (!isMobileMode) {
      Engage.on(plugin.events.focusVideo.getName(), function (display) {
        Engage.log('Video: received focusing video ' + display);
        var videoDiv;

        if (display === undefined || display === 'focus.none') {
          Engage.trigger(plugin.events.resetLayout.getName());
          return;
        }

        if (display === 'focus.next' || display === 'focus.prev') {
          if (isDefaultLayout()) {
            if (display === 'focus.next') {
              Engage.trigger(plugin.events.focusVideo.getName(),
                Utils.getFlavorForVideoDisplay($('.' + videoDisplayClass).first()));
              return;
            } else {
              Engage.trigger(plugin.events.focusVideo.getName(),
                Utils.getFlavorForVideoDisplay($('.' + videoDisplayClass).last()));
              return;
            }
          } else {
            var vidDisp = $videoDisplayClass;
            var selectNext = false;
            var last;
            var i = 0;
            for (var elem in vidDisp) {
              if (selectNext) {
                Engage.trigger(plugin.events.focusVideo.getName(),
                  Utils.getFlavorForVideoDisplay($(vidDisp[elem])));
                return;
              } else if ($(vidDisp[elem]).hasClass(videoFocusedClass)) {
                if ((display === 'focus.prev' && last === undefined) ||
                  (display === 'focus.next' && i === vidDisp.length - 1)) {
                  Engage.log('Video: Resetting videodisplay layout');
                  Engage.trigger(plugin.events.resetLayout.getName());
                  return;
                } else if (display === 'focus.next') {
                  selectNext = true;
                } else {
                  Engage.trigger(plugin.events.focusVideo.getName(),
                    Utils.getFlavorForVideoDisplay(last));
                  return;
                }
              }
              last = $(vidDisp[elem]);
              i++;
            }
          }
        } else {
          videoDiv = getDivForFlavor(display);
          if (videoDiv === undefined) {
            Engage.trigger(plugin.events.resetLayout.getName());
            return;
          }
          if ($(videoDiv).hasClass(videoFocusedClass)) {
            Engage.log('Video: Resetting videodisplay layout');
            Engage.trigger(plugin.events.resetLayout.getName());
            return;
          }
        }
        $videoDisplayClass.css({
          'width': '',
          'left': '',
          'top': '',
          'margin-left': ''
        });
        $('#engage_video').css('height', '');
        $videoDisplayClass.removeClass(videoDefaultLayoutClass).addClass(videoUnfocusedClass);
        $('.' + videoUnfocusedClass).removeClass(videoFocusedClass);
        $(videoDiv).addClass(videoFocusedClass).removeClass(videoUnfocusedClass);


        if (isPiP) {
          var distance = 0;
          $('.' + videoUnfocusedClass).each(function () {
            var width = $(this).width();
            var height = $(this).height();
            $(this).css({
              'left': 0 - (width / 2) + 'px',
              'top': distance - (height / 2) + 'px'
            });
            distance = distance + height + 10;
          });
          var marginLeft;
          if (pipPos === 'left') {
            marginLeft = 12;
          } else {
            marginLeft = 88;
          }
          $('.' + videoUnfocusedClass).css('margin-left', marginLeft + '%');
        } else {
          var height = $('.' + videoFocusedClass).height();
          $('#engage_video').height(height + 10);
        }

        delayedCalculateVideoAreaAspectRatio();
      });

      Engage.on(plugin.events.resetLayout.getName(), function () {
        Engage.log('Video: received resetting layout');
        $('#engage_video').css('height', '');
        $videoDisplayClass.css({
          'width': '',
          'left': '',
          'top': '',
          'margin-left': ''
        });
        $videoDisplayClass.removeClass(videoFocusedClass).removeClass(videoUnfocusedClass).addClass(videoDefaultLayoutClass);
        var numberDisplays = $videoDisplayClass.length;
        $videoDisplayClass.css('width', (((1 / numberDisplays) * 100) - 0.5) + '%');
        delayedCalculateVideoAreaAspectRatio();
      });

      Engage.on(plugin.events.movePiP.getName(), function (pos) {
        var numberDisplays = $('.' + videoDisplayClass).length;
        if (numberDisplays <= 1) return;
        if (pos !== undefined) {
          pipPos = pos;
        }
        if (!isPiP) {
          return;
        }
        Engage.log('Video: moving PiP');
        var marginLeft;
        if (pipPos === 'right') {
          marginLeft = 88;
          pipPos = 'right';
        } else {
          marginLeft = 12;
          pipPos = 'left';
        }
        $('.' + videoUnfocusedClass).css('margin-left', marginLeft + '%');

        delayedCalculateVideoAreaAspectRatio();
      });

      Engage.on(plugin.events.togglePiP.getName(), function (pip) {
        var numberDisplays = $videoDisplayClass.length;
        if (numberDisplays <= 1) return;

        Engage.log('Video: setting PiP to ' + pip);
        if ((pip && isPiP) || (!pip && !isPiP)) {
          return;
        }
        if (!pip) {
          videoUnfocusedClass = unfocusedClass;
          videoFocusedClass = focusedClass;
          isPiP = false;
          if (!isDefaultLayout()) {
            $videoDisplayClass.css({
              'width': '',
              'left': '',
              'top': '',
              'margin-left': ''
            });
            $('.' + unfocusedPiPClass).addClass(videoUnfocusedClass).removeClass(unfocusedPiPClass);
            $('.' + focusedPiPClass).addClass(videoFocusedClass).removeClass(focusedPiPClass);
            var height = $('.' + videoFocusedClass).height();
            $('#engage_video').height(height + 10);
          }
        } else {
          videoUnfocusedClass = unfocusedPiPClass;
          videoFocusedClass = focusedPiPClass;
          isPiP = true;
          if (!isDefaultLayout()) {
            $('.' + unfocusedClass).addClass(videoUnfocusedClass).removeClass(unfocusedClass);
            $('.' + focusedClass).addClass(videoFocusedClass).removeClass(focusedClass);
            $('#engage_video').css('height', '');
            var distance = 0;
            $('.' + videoUnfocusedClass).each(function () {
              var width = $(this).width();
              var height = $(this).height();
              $(this).css({
                'left': 0 - (width / 2) + 'px',
                'top': distance - (height / 2) + 'px'
              });
              distance = distance + height + 10;
            });
            var marginLeft;
            if (pipPos === 'left') {
              marginLeft = 12;
            } else {
              marginLeft = 88;
            }
            $('.' + videoUnfocusedClass).css('margin-left', marginLeft + '%');
          }
        }
        delayedCalculateVideoAreaAspectRatio();

      });
    }

    /* event used to switch between videos in single video display (e.g. mobile) mode */
    Engage.on(plugin.events.switchVideo.getName(), function (id) {
      /* check if current video is paused */
      var isPaused = videodisplayMaster.paused();

      $('#' + id_videoDisplayClass + (currentlySelectedVideodisplay+1)).removeClass('active');

      /* assign currentlySelectedVideodisplay in the available bounds */
      var n = globalVideoSource.length;
      currentlySelectedVideodisplay = Math.max(0, Math.min(id, n-1));

      var oldVideodisplayMaster = videodisplayMaster;
      videodisplayMaster = videojs(Engage.model.get('videoDataModel').get('ids')[currentlySelectedVideodisplay]);

      /* synchronize videos */
      if (pressedPlayOnce) {
        Engage.trigger(plugin.events.seek.getName(), currentTime);
        if (!isPaused) {
          oldVideodisplayMaster.pause();
          videodisplayMaster.play();
        }
      }

      $('#' + id_videoDisplayClass + (currentlySelectedVideodisplay+1)).addClass('active');

      Engage.log('Switched to video ' + currentlySelectedVideodisplay);
    });

    /* listen on ready event with query argument */
    Engage.on(plugin.events.ready.getName(), function (query) {
      if (query === true) {
        Engage.trigger(plugin.events.ready.getName());
      }
    });
    /* listen on videoFormatsFound event with query argument */
    Engage.on(plugin.events.videoFormatsFound.getName(), function (query) {
      if (query === true && qualities.length > 1) {
        Engage.trigger(plugin.events.videoFormatsFound.getName(), qualities);
      }
    });
  }

  function registerEvents(videoDisplay, numberOfVideodisplays) {
    if (isAudioOnly) {
      registerEventsAudioOnly(videoDisplay, numberOfVideodisplays);
    } else {
      registerEventsVideo(videoDisplay, numberOfVideodisplays);
    }
  }

  function isDefaultLayout() {
    return $('.' + videoDefaultLayoutClass).length > 0;
  }

  function getDivForFlavor(flavor) {
    var found;
    $('.' + videoDisplayClass).each(function () {
      if (Utils.getFlavorForVideoDisplay(this) === flavor) {
        found = this;
        return;
      }
    });
    return found;
  }

  function extractFlavorsAndMimetypes(mediaInfo) {
    var flavors = '';
    var mimetypes = '';

    if (mediaInfo.tracks && (mediaInfo.tracks.length > 0)) {
      for (var k = 0; k < mediaInfo.tracks.length; ++k) {
        if (flavors.indexOf(mediaInfo.tracks[k].type) < 0) {
          flavors += mediaInfo.tracks[k].type + ',';
        }

        // rtmp is treated differently for video.js. Mimetype and URL have to be changed
        if ((mediaInfo.tracks[k].mimetype == 'video/mp4') &&
            (mediaInfo.tracks[k].url.toLowerCase().indexOf('rtmp://') > -1)) {
          mediaInfo.tracks[k].mimetype = 'rtmp/mp4';
          mediaInfo.tracks[k].url = Utils.replaceAll(mediaInfo.tracks[k].url, 'mp4:', '&mp4:');
        }

        // adaptive streaming manifests don't have a resolution. Extract these from regular videos
        if (mediaInfo.tracks[k].mimetype.match(/video/g) && mediaInfo.tracks[k] &&
            mediaInfo.tracks[k].video && mediaInfo.tracks[k].video.resolution &&
            videoResultions[Utils.extractFlavorMainType(mediaInfo.tracks[k].type)] == null) {
          videoResultions[Utils.extractFlavorMainType(mediaInfo.tracks[k].type)] = Utils.parseVideoResolution(mediaInfo.tracks[k].video.resolution);
        }

        if (mimetypes.indexOf(mediaInfo.tracks[k].mimetype) < 0) {
          mimetypes += mediaInfo.tracks[k].mimetype + ',';
        }
      }
    }

    var allowedTags = Engage.model.get('meInfo').get('allowedtags');
    var allowedFormats = Engage.model.get('meInfo').get('allowedformats');
    mediaInfo.tracks = filterTracksByFormat(filterTracksByTag(mediaInfo.tracks, allowedTags), allowedFormats);

    return {
      flavors: flavors.substring(0, flavors.length - 1),
      mimetypes: mimetypes.substring(0, mimetypes.length - 1)
    }
  }

  function extractVideoSourcesAndDuration(mediaInfo, flavorsArray) {
    var videoSources = [];
    var duration = 0;
    var hasAudio = false;
    var hasVideo = false;
    videoSources.audio = [];

    for (var j = 0; j < flavorsArray.length; ++j) {
      videoSources[Utils.extractFlavorMainType(flavorsArray[j])] = [];
    }

    if (mediaInfo.tracks) {
      $(mediaInfo.tracks).each(function (i, track) {
        if (track.mimetype && track.type && acceptFormat(track)) {
          if (track.mimetype.match(/video/g) || track.mimetype.match(/application/g) || track.mimetype.match(/rtmp/g)) {
            hasVideo = true;
            if (track.duration > duration) {
              duration = track.duration;
            }
            var resolution = (track.video && track.video.resolution) ? track.video.resolution : '';
            // filter for different video sources
            var mainFlavor = Utils.extractFlavorMainType(track.type);
            Engage.log('Video: Adding video source: ' + track.url + ' (' + track.mimetype + ') for flavor ' + mainFlavor );
            if (track.mimetype === 'application/dash+xml') {
              if (Utils.checkIfMimeTypeAvailableForFlavor(videoSources, 'application/dash+xml', mainFlavor)) return; //patch for broken Distribution Service that may contain Adaptive Streaming format multiple times
              track = Utils.removeQualityTag(track);
              loadDash = true;
            } else if (track.mimetype === 'application/x-mpegURL') {
              if (Utils.checkIfMimeTypeAvailableForFlavor(videoSources, 'application/x-mpegURL', mainFlavor)) return; //patch for broken Distribution Service that may contain Adaptive Streaming format multiple times
              track = Utils.removeQualityTag(track);
              loadHls = true;
            }
            videoSources[mainFlavor].push({
              src: track.url,
              type: track.mimetype,
              typemh: track.type,
              resolution: resolution,
              tags: track.tags
            });
          } else if (track.mimetype.match(/audio/g)) {
            hasAudio = true;
            if (track.duration > duration) {
              duration = track.duration;
            }
            videoSources.audio.push({
              src: track.url,
              type: track.mimetype,
              typemh: track.type,
              tags: track.tags
            });
          }
        }
      });

      if (!hasVideo) {
        for (var i = 0; i < videoSources.length; ++i) {
          if (videoSources[i] !== videoSources.audio) {
            delete videoSources.flavor;
          }
        }
      }

      if (hasVideo || !hasAudio) {
        delete videoSources.audio;
      }
    }

    return {
      videoSources: videoSources,
      duration: duration
    };
  }

  function extractVideoDisplays(videoSources) {
    var videoDisplays = [];

    for (var v in videoSources) {
      if (videoSources[v].length > 0) {
        var name = videoDisplayNamePrefix.concat(v);
        videoDisplays.push(name);
      }
    }

    return videoDisplays;
  }

  function setVideoSourcePosters(mediaInfo, videoSources) {
    if (mediaInfo.attachments && (mediaInfo.attachments.length > 0)) {
      $(mediaInfo.attachments).each(function (i, attachment) {
        if (attachment.mimetype &&
            attachment.type &&
            attachment.url &&
            attachment.mimetype.match(/image/g) &&
            attachment.type.match(/player/g) &&
            videoSources[Utils.extractFlavorMainType(attachment.type)]) {
          // filter for different video sources
          videoSources[Utils.extractFlavorMainType(attachment.type)]['poster'] = attachment.url;
        }
      });
    }
  }

  function setupStreams(tracks, attachments) {
    Engage.log('Video: Setting up streams');

    var mediaInfo = {};
    var videoSources;
    var videoDisplays;
    var duration = 0;

    mastervideotype = Engage.model.get('meInfo').get('mastervideotype').toLowerCase();
    Engage.log('Video: Master video type is \'' + mastervideotype + '\'');

    mediaInfo.tracks = tracks;
    mediaInfo.attachments = attachments;

    if (mediaInfo.tracks && (mediaInfo.tracks.length > 0)) {
      var flavorsAndMimetypes = extractFlavorsAndMimetypes(mediaInfo);
      flavors = flavorsAndMimetypes.flavors;
      mimetypes = flavorsAndMimetypes.mimetypes;
      Engage.log('Video: Extracted flavors: ' + flavors, mimetypes);
      Engage.log('Video: Extracted mimetypes: ' + mimetypes);

      var flavorsArray = flavors.split(',');

      // look for video sources
      var videoSourcesAndDuration = extractVideoSourcesAndDuration(mediaInfo, flavorsArray);
      videoSources = videoSourcesAndDuration.videoSources;
      duration = videoSourcesAndDuration.duration;

      setVideoSourcePosters(mediaInfo, videoSources);

      videoDisplays = extractVideoDisplays(videoSources);
      Engage.log('Video: Extracted video displays: ' + videoDisplays.join(', '));

      Engage.model.set('videoDataModel', new VideoDataModel(videoDisplays, videoSources, duration));
    }
  }

  function getSortedVideosourcesArray(videoSources) {
    var tuples = [];

    for (var key in videoSources) {
      tuples.push([key, videoSources[key]]);
    }

    tuples.sort(compareVideoSources);

    return tuples;
  }

  function compareVideoSources(a, b) {
    if (a === undefined || b === undefined || a[1][0] === undefined ||
      b[1][0] === undefined) {
      return 0;
    }
    var s1 = a[1][0].typemh;
    var s2 = b[1][0].typemh;
    if (s1 == mastervideotype) {
      return -1;
    } else if (s2 == mastervideotype) {
      return 1;
    }

    return 0;
  }

  /**
   * Try to load captions for video
   * @returns {undefined}
   */
  function loadAndAppendCaptions(videoDataView) {
    Engage.log("Video: Loading Captions.");
    var tracks        = Engage.model.get('mediaPackage').get('tracks');
    var attachments   = Engage.model.get('mediaPackage').get('attachments')
    var videoDisplays = videoDataView.model.get('ids');
    var captionsURL   = null;

    // Load from attachment
    for(var a in attachments) {
      if(attachments[a].mimetype == "text/vtt") {
        Engage.log("Found caption in attachments.");
        captionsURL = attachments[a].url;
        Engage.model.set("captions", true);
        Engage.trigger(plugin.events.captionsFound.getName());
      }
    }

    // Load from track
    for(var a in tracks) {
      if(tracks[a].mimetype == "text/vtt") {
        Engage.log("Found caption in tracks");
        captionsURL = tracks[a].url;
        Engage.model.set("captions", true);
        Engage.trigger(plugin.events.captionsFound.getName());
      }
    }

    if(captionsURL == null) {
      return;
    }

    $.each(videoDisplays, function(i, j){
      var caption = videojs(j).addRemoteTextTrack({
        kind: 'caption',
        language: 'en',
        label: 'Caption',
        src: captionsURL,
        mode: "hidden"
      }, true);
    });

    activeCaption = videojs(videoDisplays[0]).remoteTextTracks()[0];

    Engage.on(plugin.events.toggleCaptions.getName(), function(data) {
      if(data) {
        activeCaption.mode = "showing";
      } else {
        activeCaption.mode = "hidden";
      }
    });

    Engage.on(plugin.events.captionsFound.getName(), function (data) {
      var captionMode = activeCaption.mode;
      activeCaption.mode = "hidden";
      activeCaption = videojs("videojs_videodisplay_" + data).textTracks()[0];
      activeCaption.mode = captionMode;
      if(data == "none") {
        console.warn("none " + data);
      } else {
        console.warn("else " + data);
      }
    });
  }

  function initPlugin() {
    Engage.log('Video: Init Plugin');

    // only init if plugin template was inserted into the DOM
    if (plugin.inserted) {
      Engage.log('Video: Video Plugin inserted');
      // set path to swf player
      var videojs_swf = plugin.pluginPath + videojs_swf_path;
      Engage.log('Video: SWF path: ' + videojs_swf_path);
      Engage.model.on(videoDataModelChange, function () {
        Engage.log('Video: The video data model changed, refreshing the view.');
        videoDataView = new VideoDataView(this.get('videoDataModel'), plugin.template, videojs_swf);
      });
      Engage.on(plugin.events.mediaPackageModelError.getName(), function () {
        mediapackageError = true;
      });
      Engage.model.get('mediaPackage').on('change', function () {
        setupStreams(this.get('tracks'), this.get('attachments'));
      });
      if (Engage.model.get('mediaPackage').get('tracks')) {
        Engage.log('Video: Mediapackage already available.');
        setupStreams(Engage.model.get('mediaPackage').get('tracks'), Engage.model.get('mediaPackage').get('attachments'));
      }
    }
  }

  // init Event
  Engage.log('Video: Init');
  var relative_plugin_path = Engage.getPluginPath('EngagePluginVideoVideoJS');

  // listen on a change/set of the mediaPackage model
  Engage.model
    .on(mediapackageChange, function () {
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }
    })
    .on(infoMeChange, function () {
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }
    });

  // all plugins loaded
  Engage.on(plugin.events.plugin_load_done.getName(), function () {
    Engage.log('Video: Plugin load done');
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });
  requirejs.config({
    waitSeconds: 60
  });

  // load utils class
  require([relative_plugin_path + 'utils'], function (utils) {
    Engage.log('Video: Utils class loaded');
    Utils = new utils();
    initTranslate(Engage.model.get("language"), function () {
      Engage.log('Video: Successfully translated.');
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }
    }, function () {
      Engage.log('Video: Error translating...');
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }
    });
  });

  // load videoData model
  require([relative_plugin_path + 'models/videoData'], function (model) {
    Engage.log('Video: VideoData model loaded');
    VideoDataModel = model;
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  // load video.js lib
  require([relative_plugin_path + videoPath], function (videojs) {
    Engage.log('Video: Lib video loaded');
    window.videojs = videojs;
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  // load synchronize.js lib
  require([relative_plugin_path + synchronizePath], function (synchronizejs) {
    Engage.log('Video: Lib synchronize loaded');
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  return plugin;
})
