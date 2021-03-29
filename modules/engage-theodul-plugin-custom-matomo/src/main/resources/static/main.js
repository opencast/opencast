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
/* global define, require, Piwik */
define(['jquery', 'backbone', 'engage/core'], function($, Backbone, Engage) {
  'use strict';

  var insertIntoDOM = false;
  var PLUGIN_NAME = 'Engage Plugin Custom Matomo';
  var PLUGIN_TYPE = 'engage_custom';
  var PLUGIN_VERSION = '1.0';
  var PLUGIN_TEMPLATE_DESKTOP = 'none';
  var PLUGIN_TEMPLATE_MOBILE = 'none';
  var PLUGIN_TEMPLATE_EMBED = 'none';
  var PLUGIN_STYLES_DESKTOP = [
    ''
  ];
  var PLUGIN_STYLES_EMBED = [
    ''
  ];
  var PLUGIN_STYLES_MOBILE = [
    ''
  ];

  var plugin;
  var events = {
    plugin_load_done: new Engage.Event('Core:plugin_load_done', '', 'handler'),
    customNotification: new Engage.Event('Notification:customNotification', 'a custom message', 'trigger'),
    mediaPackageModelError: new Engage.Event('MhConnection:mediaPackageModelError', '', 'handler'),
    mediaPackageLoaded: new Engage.Event('MhConnection:mediaPackageLoaded', 'A mediapackage has been loaded',
      'trigger'),
    play: new Engage.Event('Video:play', 'plays the video', 'both'),
    pause: new Engage.Event('Video:pause', 'pauses the video', 'both'),
    seek: new Engage.Event('Video:seek', 'seek video to a given position in seconds', 'both'),
    ended: new Engage.Event('Video:ended', 'end of the video', 'trigger'),
    fullscreenEnable: new Engage.Event('Video:fullscreenEnable', 'go to fullscreen', 'handler'),
    playbackRateChanged: new Engage.Event('Video:playbackRateChanged', 'The video playback rate changed', 'handler'),
    qualitySet: new Engage.Event('Video:qualitySet', '', 'handler'),
    focusVideo: new Engage.Event('Video:focusVideo', 'increases the size of one video', 'handler'),
    resetLayout: new Engage.Event('Video:resetLayout', 'resets the layout of the videodisplays', 'handler'),
    zoomChange: new Engage.Event('Video:zoomChange', 'zoom level has changed', 'trigger'),
    volumeSet: new Engage.Event('Video:volumeSet', 'set the volume', 'handler')
  };

  var isDesktopMode = false,
      isEmbedMode = false,
      isMobileMode = false;
  translations = [];

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

  /* don't change these variables */

  var server = Engage.model.get('meInfo').get('matomo.server'),
      tracker,
      initing = true,
      mediapackageError = false,
      allow_tracking = true;

  allow_tracking = ! isDoNotTrackStatus();

  if (server && allow_tracking) {
    if (server.substr(-1) != '/') server += '/';
    var siteId = parseInt(Engage.model.get('meInfo').get('matomo.site_id')),
        heartbeat = parseInt(Engage.model.get('meInfo').get('matomo.heartbeat')),
        notification,
        track_events = Engage.model.get('meInfo').get('matomo.track_events'),
        translations = [];

    if (track_events) {
      track_events = track_events.map(function (e) { return e.toLowerCase(); });
    }

    if (Engage.model.get('meInfo').get('matomo.notification'))
      notification = JSON.parse(Engage.model.get('meInfo').get('matomo.notification'));
    else notification = true;

    if (isNaN(siteId)) {
      siteId = 1;
    }

    if (isNaN(heartbeat)) {
      heartbeat = 0;
    }


    // load piwik lib from remote server
    require([server + 'piwik.js'], function(piwik) {
      Engage.log('Matomo: external piwik lib loaded');

      tracker = Piwik.getAsyncTracker( server + 'piwik.php', siteId );
      initTranslate(Engage.model.get('language'), function () {
        Engage.log('Piwik: Successfully translated.');
      }, function () {
        Engage.log('Piwik: Error translating...');
      });


      var mediapackage = Engage.model.get('mediaPackage'),
          last_zoom_update = 0,
          zoom_timeout = 2000,
          volume_changing = false,
          changed_volume = 1,
          last_event,
          last_value;

      if (tracker && mediapackage && mediapackage.get('ready')) {
        initTracker(tracker, mediapackage);
      }

      Engage.on(plugin.events.mediaPackageModelError.getName(), function() {
        mediapackageError = true;
      });

      if (tracker) {

        Engage.on(plugin.events.mediaPackageLoaded.getName(), function() {
          mediapackage = Engage.model.get('mediaPackage');
          if (mediapackage && mediapackage.get('ready')) {
            initTracker(tracker, mediapackage);
          }
        });

        Engage.on(plugin.events.qualitySet.getName(), function(q) {
          if (!trackEvent('quality') || (last_event === 'quality' && last_value === q)) return;
          if (!initing)
            tracker.trackEvent('Player.Settings','Quality', q);
          last_event = 'quality';
          last_value = q;
        });

        Engage.on(plugin.events.volumeSet.getName(), function(v) {
          if (!trackEvent('volume')) return;
          changed_volume = v;
          if (!initing && !volume_changing) {
            volume_changing = true;
            setTimeout(function () {
              tracker.trackEvent('Player.Settings','Volume', changed_volume);
              last_event = 'quality';
              last_value = undefined;
              volume_changing = false;
            }, 1000);
          }
        });

        Engage.on(plugin.events.ended.getName(), function() {
          if (!trackEvent('ended') || last_event === 'ended') return;
          tracker.trackEvent('Player.Status','Ended');
          last_event = 'ended';
          last_value = undefined;
        });

        Engage.on(plugin.events.play.getName(), function() {
          if (!trackEvent('play') || last_event === 'play') return;
          tracker.trackEvent('Player.Controls','Play');
          last_event = 'play';
          last_value = undefined;
        });

        Engage.on(plugin.events.pause.getName(), function() {
          if (!trackEvent('pause') || last_event === 'pause') return;
          tracker.trackEvent('Player.Controls','Pause');
          last_event = 'pause';
          last_value = undefined;
        });

        Engage.on(plugin.events.seek.getName(), function(time) {
          if (!trackEvent('seek') || (last_event === 'pause' && last_value === time)) return;
          tracker.trackEvent('Player.Controls','Seek', time);
          last_event = 'seek';
          last_value = time;
        });

        Engage.on(plugin.events.playbackRateChanged.getName(), function(speed) {
          if (!trackEvent('playbackrate') || (last_event === 'playbackrate' && last_value === speed)) return;
          tracker.trackEvent('Player.Controls','PlaybackRate', speed);
          last_event = 'playbackrate';
          last_value = speed;
        });

        Engage.on(plugin.events.fullscreenEnable.getName(), function() {
          if (!trackEvent('fullscreen') || last_event === 'fullscreen') return;
          tracker.trackEvent('Player.View','Fullscreen');
          last_event = 'fullscreen';
          last_value = undefined;
        });

        Engage.on(plugin.events.focusVideo.getName(), function(focus) {
          if (!trackEvent('focus')  || (last_event === 'focus' && last_value === 'focus')) return;
          if (!initing)
            tracker.trackEvent('Player.View','Focus', focus);
          last_event = 'focus';
          last_value = focus;
        });

        Engage.on(plugin.events.resetLayout.getName(), function(q) {
          if (!trackEvent('layout_reset') || last_event === 'layout_reset') return;
          if (!initing)
            tracker.trackEvent('Player.View','DefaultLayout');
          last_event = 'layout_reset';
          last_value = undefined;
        });

        Engage.on(plugin.events.zoomChange.getName(), function() {
          if (!trackEvent('zoom') || last_event === 'zoom') return;
          var now = new Date().getTime();
          if (now > (last_zoom_update + zoom_timeout)) {
            tracker.trackEvent('Player.View','Zoom');
            last_zoom_update = now;
            last_event = 'zoom';
            last_value = undefined;
          }
        });
      }

      function trackEvent(e) {
        if (! track_events) return false;

        if (track_events.indexOf(e) >= 0) return true;
      }

    });
    Engage.log('Matomo: Init');
  } else {
    Engage.log('Matomo: tracking not configured');
  }

  function initTracker(tracker, mediapackage) {
    if (tracker && mediapackage && !mediapackageError && mediapackage.get('ready')) {
      var seriesid = 'series not set',
          seriestitle = 'series not set',
          presenter = 'presenter unknown';

      if (mediapackage.get('seriesid') && mediapackage.get('seriesid').length > 0) {
        seriesid = mediapackage.get('seriesid');
      }
      if (mediapackage.get('series') && mediapackage.get('series').length > 0) {
        seriestitle = mediapackage.get('series');
      }
      if (mediapackage.get('creator') && mediapackage.get('creator').length > 0) {
        presenter = mediapackage.get('creator');
      }
      let eventStr = mediapackage.get('title') + ' (' + mediapackage.get('eventid') + ')';
      tracker.setCustomVariable(1, 'event', eventStr, 'page');
      tracker.setCustomVariable(2, 'series', seriestitle + ' (' + seriesid + ')', 'page');
      tracker.setCustomVariable(3, 'presenter', presenter, 'page');
      tracker.setCustomVariable(4, 'view_mode', Engage.model.get('mode'), 'page');
      tracker.setCustomVariable(5, 'client', 'Theodul Player');
      tracker.setDocumentTitle(mediapackage.get('title') + ' - ' + presenter);
      tracker.trackPageView(mediapackage.get('title') + ' - ' + presenter);
      if (Piwik && Piwik.MediaAnalytics) Piwik.MediaAnalytics.scanForMedia();
      if (heartbeat > 0) tracker.enableHeartBeatTimer(heartbeat);
      if (notification) {
        Engage.trigger(plugin.events.customNotification.getName(),
          translate('matomo_tracking',
            'Usage data will be collected with Matomo. '
            + 'You can use the Do-Not-Track settings of your browser to prevent this.'));
      }
      Engage.log('Matomo: Tracker initialized');
      setTimeout(function () {
        initing = false;
      }, 2000);
    }
  }

  function initTranslate(language, funcSuccess, funcError) {
    var path = Engage.getPluginPath('EngagePluginCustomMatomo').replace(/(\.\.\/)/g, '');
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

  function isDoNotTrackStatus() {
    if (window.navigator.doNotTrack == 1 || window.navigator.msDoNotTrack == 1 ) {
      return true;
    }
    return false;
  }

  return plugin;
});
