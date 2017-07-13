/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the 'License'); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*jslint browser: true, nomen: true*/
/*global define*/
define(['require', 'jquery', 'underscore', 'backbone', 'basil', 'bootbox', 'engage/core'], function (require, $, _, Backbone, Basil, Bootbox, Engage) {
  'use strict';

  var insertIntoDOM = true;
  var PLUGIN_NAME = 'Engage Controls';
  var PLUGIN_TYPE = 'engage_controls';
  var PLUGIN_VERSION = '1.0';
  var PLUGIN_TEMPLATE_DESKTOP = Engage.controls_top ? 'templates/desktop_top.html' : 'templates/desktop_bottom.html';
  // provide this additional template if the controls are below the video to have content above the video
  var PLUGIN_TEMPLATE_DESKTOP_TOP_IFBOTTOM = Engage.controls_top ? 'none' : 'templates/desktop_top_ifbottom.html';
  var PLUGIN_TEMPLATE_EMBED = 'templates/embed.html';
  var PLUGIN_TEMPLATE_MOBILE = 'templates/mobile.html';
  var PLUGIN_STYLES_DESKTOP = [
    Engage.controls_top ? 'styles/desktop_top.css' : 'styles/desktop_bottom.css',
    'lib/jqueryui/jquery-ui.min.css'
  ];
  var PLUGIN_STYLES_EMBED = [
    'styles/embed.css',
    'lib/jqueryui/jquery-ui.min.css'
  ];
  var PLUGIN_STYLES_MOBILE = [
    'styles/mobile.css',
    'lib/jqueryui/jquery-ui.min.css'
  ];

  var basilOptions = {
    namespace: 'mhStorage'
  };
  Basil = new window.Basil(basilOptions);

  var plugin;
  var events = {
    play: new Engage.Event('Video:play', 'plays the video', 'both'),
    pause: new Engage.Event('Video:pause', 'pauses the video', 'both'),
    playPause: new Engage.Event('Video:playPause', '', 'trigger'),
    fullscreenEnable: new Engage.Event('Video:fullscreenEnable', '', 'both'),
    mute: new Engage.Event('Video:mute', '', 'both'),
    unmute: new Engage.Event('Video:unmute', '', 'both'),
    muteToggle: new Engage.Event('Video:muteToggle', '', 'both'),
    segmentMouseover: new Engage.Event('Segment:mouseOver', 'the mouse is over a segment', 'both'),
    segmentMouseout: new Engage.Event('Segment:mouseOut', 'the mouse is off a segment', 'both'),
    volumeSet: new Engage.Event('Video:volumeSet', '', 'both'),
    playbackRateChanged: new Engage.Event('Video:playbackRateChanged', 'The video playback rate changed', 'both'),
    fullscreenCancel: new Engage.Event('Video:fullscreenCancel', '', 'both'),
    sliderStart: new Engage.Event('Slider:start', '', 'trigger'),
    sliderStop: new Engage.Event('Slider:stop', '', 'trigger'),
    sliderMousein: new Engage.Event('Slider:mouseIn', 'the mouse entered the slider', 'trigger'),
    sliderMouseout: new Engage.Event('Slider:mouseOut', 'the mouse is off the slider', 'trigger'),
    sliderMousemove: new Engage.Event('Slider:mouseMoved', 'the mouse is moving over the slider', 'trigger'),
    seek: new Engage.Event('Video:seek', 'seek video to a given position in seconds', 'trigger'),
    seekLeft: new Engage.Event('Video:seekLeft', '', 'trigger'),
    seekRight: new Engage.Event('Video:seekRight', '', 'trigger'),
    nextChapter: new Engage.Event('Video:nextChapter', '', 'trigger'),
    previousChapter: new Engage.Event('Video:previousChapter', '', 'trigger'),
    customOKMessage: new Engage.Event('Notification:customOKMessage', 'a custom message with an OK button', 'trigger'),
    customSuccess: new Engage.Event('Notification:customSuccess', 'a custom success message', 'trigger'),
    customError: new Engage.Event('Notification:customError', 'an error occurred', 'trigger'),
    qualitySet: new Engage.Event('Video:qualitySet', '', 'trigger'),
    volumeUp: new Engage.Event('Video:volumeUp', '', 'handler'),
    volumeDown: new Engage.Event('Video:volumeDown', '', 'handler'),
    plugin_load_done: new Engage.Event('Core:plugin_load_done', '', 'handler'),
    fullscreenChange: new Engage.Event('Video:fullscreenChange', 'notices a fullscreen change', 'handler'),
    ready: new Engage.Event('Video:ready', 'all videos loaded successfully', 'both'),
    timeupdate: new Engage.Event('Video:timeupdate', 'notices a timeupdate', 'handler'),
    ended: new Engage.Event('Video:ended', 'end of the video', 'handler'),
    usingFlash: new Engage.Event('Video:usingFlash', 'flash is being used', 'handler'),
    mediaPackageModelError: new Engage.Event('MhConnection:mediaPackageModelError', '', 'handler'),
    aspectRatioSet: new Engage.Event('Video:aspectRatioSet', 'the aspect ratio has been calculated', 'handler'),
    isAudioOnly: new Engage.Event('Video:isAudioOnly', 'whether it`s audio only or not', 'handler'),
    videoFormatsFound: new Engage.Event('Video:videoFormatsFound', '', 'both'),
    numberOfVideodisplaysSet: new Engage.Event('Video:numberOfVideodisplaysSet', 'the number of videodisplays has been set', 'trigger'),
    focusVideo: new Engage.Event('Video:focusVideo', 'increases the size of one video', 'handler'),
    resetLayout: new Engage.Event('Video:resetLayout', 'resets the layout of the videodisplays', 'handler'),
    movePiP: new Engage.Event('Video:movePiP', 'moves the smaller picture over the larger to the different corners', 'handler'),
    togglePiP: new Engage.Event('Video:togglePiP', 'switches between PiP and next to each other layout', 'both'),
    setZoomLevel: new Engage.Event('Video:setZoomLevel', 'sets the zoom level', 'trigger'),
    zoomReset: new Engage.Event('Video:resetZoom', 'resets position and zoom level', 'trigger'),
    zoomChange: new Engage.Event('Video:zoomChange', 'zoom level has changed', 'handler'),
    toggleCaptions: new Engage.Event('Video:toggleCaptions', 'toggle captions', 'trigger'),
    captionsFound: new Engage.Event('Video:captionsFound', 'captions found', 'handler'),
    // events for mobile view
    switchVideo: new Engage.Event('Video:switch', 'switch the video', 'trigger'),
    showControls: new Engage.Event('Controls:show', 'show the controls', 'both'),
    hideControls: new Engage.Event('Controls:hide', 'hide the controls', 'both')
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
        template_topIfBottom: PLUGIN_TEMPLATE_DESKTOP_TOP_IFBOTTOM,
        events: events
      };
      isDesktopMode = true;
      break;
  }

  /* change these variables */
  var embedHeightOne = 280;
  var embedHeightTwo = 315;
  var embedHeightThree = 360;
  var embedHeightFour = 480;
  var embedHeightFive = 720;
  var min_segment_duration = 5000;
  var logoLink = false;
  var logo = plugin_path + 'images/logo.png';
  var showEmbed = true;
  // mobile mode: time after controls are hidden again in seconds
  var hideTimeout = 4;


  /* don't change these variables */
  var Utils;
  var volUpDown = 5.0;
  var storage_volume = 'volume';
  var storage_lastvolume = 'lastvolume';
  var storage_muted = 'muted';
  var storage_pip = 'pictureinpicture';
  var storage_pip_pos = 'pictureinpictureposition';
  var storage_focus_video = 'focusvideo';
  var jQueryUIPath = 'lib/jqueryui/jquery-ui';
  var id_zoomLevelIndicator = 'zoomLevelIndicator';
  var id_zoomReset = 'resetZoom';
  var id_zoomLevel1 = 'zoomLevel1';
  var id_zoomLevel2 = 'zoomLevel2';
  var id_zoomLevel3 = 'zoomLevel3';
  var id_captions_button = 'captions_button';
  var id_engage_controls = 'engage_controls';
  var id_engage_controls_topIfBottom = 'engage_controls_second';
  var id_slider = 'slider';
  var id_volumeSlider = 'volumeSlider';
  var id_volumeIcon = 'volumeIcon';
  var id_dropdownMenuPlaybackRate = 'dropdownMenuPlaybackRate';
  var id_playbackRate075 = 'playback075';
  var id_playbackRate100 = 'playback100';
  var id_playbackRate125 = 'playback125';
  var id_playbackRate150 = 'playback150';
  var id_playbackRate200 = 'playback200';
  var id_playbackRate300 = 'playback300';
  var id_pipIndicator = 'pipIndicator';
  var id_pipLeft = 'pipLeft';
  var id_pipRight = 'pipRight';
  var id_pipBeside = 'pipBeside';
  var id_pipOff = 'pipOff';
  var id_qualityIndicator = 'qualityIndicator';
  var id_playpause_controls = 'playpause_controls';
  var id_fullscreen_button = 'fullscreen_button';
  var id_embed_button = 'embed_button';
  var id_backward_button = 'backward_button';
  var id_forward_button = 'forward_button';
  var id_navigation_time = 'navigation_time';
  var id_navigation_time_current = 'navigation_time_current';
  var id_big_play_button = 'big_play_button';
  var id_play_button = 'play_button';
  var id_pause_button = 'pause_button';
  var id_unmute_button = 'unmute_button';
  var id_mute_button = 'mute_button';
  var id_segmentNo = 'segment_';
  var id_embed0 = 'embed0';
  var id_embed1 = 'embed1';
  var id_embed2 = 'embed2';
  var id_embed3 = 'embed3';
  var id_embed4 = 'embed4';
  var id_playbackRateIndicator = 'playbackRateIndicator';
  var id_playbackRemTime075 = 'playbackRemTime075';
  var id_playbackRemTime100 = 'playbackRemTime100';
  var id_playbackRemTime125 = 'playbackRemTime125';
  var id_playbackRemTime150 = 'playbackRemTime150';
  var id_playbackRemTime200 = 'playbackRemTime200';
  var id_playbackRemTime300 = 'playbackRemTime300';
  var id_loggedInNotLoggedIn = 'loggedInNotLoggedIn';
  var id_loginlogout = 'loginlogout';
  var id_str_loginlogout = 'str_loginlogout';
  var id_dropdownMenuLoginInfo = 'dropdownMenuLoginInfo';
  var class_dropdown = 'dropdown-toggle';
  var videosReady = false;
  var enableFullscreenButton = false;
  var currentTime = 0;
  var videoDataModelChange = 'change:videoDataModel';
  var infoMeChange = 'change:infoMe';
  var mediapackageChange = 'change:mediaPackage';
  var event_slidestart = 'slidestart';
  var event_slidestop = 'slidestop';
  var event_slide = 'slide';
  var plugin_path = '';
  var initCount = 5;
  if (isMobileMode) {
    initCount += 3;          // increase initCount, because mobile version loads 3 more libs
  }
  var inited = false;
  var isPlaying = false;
  var isSliding = false;
  var duration;
  var usingFlash = false;
  var isAudioOnly = false;
  var segments = {};
  var mediapackageError = false;
  var aspectRatioTriggered = false;
  var aspectRatioWidth;
  var aspectRatioHeight;
  var aspectRatio;
  var embedWidthOne;
  var embedWidthTwo;
  var embedWidthThree;
  var embedWidthFour;
  var embedWidthFive;
  var loggedIn = false;
  var username = 'Anonymous';
  var translations = new Array();
  var askedForLogin = false;
  var springSecurityLoginURL = '/j_spring_security_check';
  var springSecurityLogoutURL = '/j_spring_security_logout';
  var springLoggedInStrCheck = '<title>Opencast – Login Page</title>';
  var controlsViewTopIfBottom = undefined;
  var controlsView = undefined;
  var resolutions = undefined;
  var pipPos = 'left';
  var pipStatus = true;
  var numberVideos = 1;
  var currentFocusFlavor = 'focus.none';
  var videosInitialReadyness = true;
  // for mobile view
  var id_videoWrapper = 'video_wrapper';
  var id_gestureContainer = 'engage_video';
  var controlsVisible = true;
  var controlsTimer = null;
  var carousel = null;
  var captionsOn = false;

  function initTranslate(language, funcSuccess, funcError) {
    var path = Engage.getPluginPath('EngagePluginControls').replace(/(\.\.\/)/g, '');
    var jsonstr = window.location.origin + '/engage/theodul/' + path; // this solution is really bad, fix it...

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

  function login() {
    if (!askedForLogin) {
      askedForLogin = true;
      var username = 'User';
      var password = 'Password';

      Bootbox.dialog({
        title: translate('loginInformation', 'Log in'),
        message: '<form class="form-signin">' +
        '<h2 class="form-signin-heading">' + translate('enterUsernamePassword', 'Please enter your username and password') + '</h2>' +
        '<input id="username" type="text" class="form-control form-control-custom" name="username" placeholder="' + translate('username', 'Username') + '" required="true" autofocus="" />' +
        '<input id="password" type="password" class="form-control form-control-custom" name="password" placeholder="' + translate('password', 'Password') + '" required="true" />' +
        '<label class="checkbox">' +
        '<input type="checkbox" value="' + translate('rememberMe', 'Remember me') + '" id="rememberMe" name="rememberMe" checked> ' + translate('rememberMe', 'Remember me') +
        '</label>' +
        '</form>',
        buttons: {
          cancel: {
            label: translate('cancel', 'Cancel'),
            className: 'btn-default',
            callback: function () {
              askedForLogin = false;
            }
          },
          login: {
            label: translate('login', 'Log in'),
            className: 'btn-success',
            callback: function () {
              var username = $('#username').val().trim();
              var password = $('#password').val().trim();
              if ((username !== null) && (username.length > 0) && (password !== null) && (password.length > 0)) {
                $.ajax({
                  type: 'POST',
                  url: springSecurityLoginURL,
                  data: {
                    'j_username': username,
                    'j_password': password,
                    '_spring_security_remember_me': $('#rememberMe').is(':checked')
                  }
                }).done(function (msg) {
                  password = '';
                  if (msg.indexOf(springLoggedInStrCheck) == -1) {
                    Engage.trigger(events.customSuccess.getName(), translate('loginSuccessful', 'Successfully logged in. Please reload the page if the page does not reload automatically.'));
                    location.reload();
                  } else {
                    Engage.trigger(events.customSuccess.getName(), translate('loginFailed', 'Failed to log in.'));
                  }
                  askedForLogin = false;
                }).fail(function (msg) {
                  password = '';
                  Engage.trigger(events.customSuccess.getName(), translate('loginFailed', 'Failed to log in.'));
                  askedForLogin = false;
                });
              } else {
                askedForLogin = false;
              }
            }
          }
        },
        className: 'usernamePassword-modal',
        onEscape: function () {
          askedForLogin = false;
        },
        closeButton: false
      });
    }
  }

  function logout() {
    Engage.trigger(events.customSuccess.getName(), translate('loggingOut', 'You are being logged out, please wait a moment.'));
    $.ajax({
      type: 'GET',
      url: springSecurityLogoutURL,
    }).complete(function (msg) {
      location.reload();
      Engage.trigger(events.customSuccess.getName(), translate('logoutSuccessful', 'Successfully logged out. Please reload the page if the page does not reload automatically.'));
    });
  }

  function checkLoginStatus() {
    $('#' + id_loginlogout).unbind('click');
    if (Engage.model.get('infoMe').loggedIn) {
      loggedIn = true;
      username = Engage.model.get('infoMe').username;
      $('#' + id_loggedInNotLoggedIn).html(username);
      $('#' + id_str_loginlogout).html(translate('logout', 'Log out'));
      $('#' + id_loginlogout).click(logout);
    } else {
      loggedIn = false;
      username = 'Anonymous';
      $('#' + id_loggedInNotLoggedIn).html(translate('loggedOut', 'Logged out'));
      $('#' + id_str_loginlogout).html(translate('login', 'Log in'));
      $('#' + id_loginlogout).click(login);
    }
    $('#' + id_dropdownMenuLoginInfo).removeClass('disabled');
  }

  var ControlsView = Backbone.View.extend({
    el: $('#' + id_engage_controls), // every view has an element associated with it
    initialize: function (videoDataModel, template, plugin_path) {
      this.setElement($(plugin.container));
      this.model = videoDataModel;
      this.template = template;
      this.pluginPath = plugin_path;

      // bind the render function always to the view
      _.bindAll(this, 'render');
      // listen for changes of the model and bind the render function to this
      this.model.bind('change', this.render);
      this.render();
    },
    render: function () {
      if (!mediapackageError) {
        duration = parseInt(this.model.get('duration'));
        segments = Engage.model.get('mediaPackage').get('segments');

        var pipPosition = pipPos;
        if (!pipStatus) {
          pipPosition = 'beside';
        }

        segments = Utils.repairSegmentLength(segments, duration, min_segment_duration);

        if (Engage.model.get('meInfo')) {
          if (Engage.model.get('meInfo').get('logo_player')) {
            logo = Engage.model.get('meInfo').get('logo_player');
          }
          if (Engage.model.get('meInfo').get('link_mediamodule')) {
            logoLink = window.location.protocol + '//' + window.location.host + '/engage/ui/index.html'; // link to the media module
          }
          if (!Engage.model.get('meInfo').get('show_embed_links')) {
            showEmbed = false;
          }
        }
        var translatedQualities = new Array();
        if (resolutions) {
          for (var i = 0; i < resolutions.length; i++) {
            translatedQualities[resolutions[i]] = translate(resolutions[i], resolutions[i]);
          }
        }

        var tempVars = {
          plugin_path: this.pluginPath,
          startTime: Utils.formatSeconds(0),
          durationMS: (duration && (duration > 0)) ? duration : 1, // duration in ms
          duration: (duration ? Utils.formatSeconds(duration / 1000) : Utils.formatSeconds(0)), // formatted duration
          logoLink: logoLink,
          segments: segments,
          str_prevChapter: translate('prevChapter', 'Go to previous chapter'),
          str_nextChapter: translate('nextChapter', 'Go to next chapter'),
          str_playPauseVideo: translate('playPauseVideo', 'Play or pause the video'),
          str_playVideo: translate('playVideo', 'Play the video'),
          str_pauseVideo: translate('pauseVideo', 'Pause the video'),
          str_volumeSlider: translate('volumeSlider', 'Volume slider'),
          str_muteVolume: translate('muteVolume', 'Mute volume'),
          str_unmuteVolume: translate('unmuteVolume', 'Unmute Volume'),
          str_message_inputField: translate('message_inputField', 'Input field shows current video time. Can be edited.'),
          str_totalVideoLength: translate('totalVideoLength', 'Total length of the video:'),
          str_openMediaModule: translate('openMediaModule', 'Go to Media Module'),
          str_playbackRateButton: translate('playbackRateButton', 'Playback rate button. Select playback rate from dropdown.'),
          str_playbackRate: translate('playbackRate', 'Playback rate'),
          str_remainingTime: translate('remainingTime', 'remaining time'),
          str_embedButton: translate('embedButton', 'Embed Button. Select embed size from dropdown.'),
          loggedIn: false,
          str_checkingStatus: translate('checkingLoginStatus', 'Checking login status...'),
          str_loginLogout: translate('loginLogout', 'Login/Logout'),
          str_fullscreen: translate('fullscreen', 'Fullscreen'),
          str_qualityButton: translate('quality', 'Quality'),
          str_quality: translate('quality', 'Quality'),
          str_layoutButton: translate('layout', 'Layout'),
          str_pictureInPicture: translate('pictureInPicture', 'Picture in Picture'),
          str_left: translate('left', 'left'),
          str_right: translate('right', 'right'),
          str_beside: translate('beside', 'beside'),
          str_off: translate('off', 'off'),
          qualities: resolutions,
          pip_position: translate(pipPosition, pipPosition),
          translatedqualities: translatedQualities,
          hasqualities: resolutions !== undefined,
          hasmultiplevideos: (Engage.model.get('videoDataModel').get('ids').length > 1),
          controlsTop: Engage.controls_top,
          logo: logo,
          show_embed: showEmbed,
          str_zoomlevel: '100%',
          flash: usingFlash,
          // for mobile view
          numberofvideos: Engage.model.get('videoDataModel').get('ids').length,
          str_video: translate('video', 'Video'),
          str_of: translate('of', 'of'),
          str_showVolumeBar: translate('showVolumeBar', 'Show volume bar'),
          str_swipeHint: translate('swipeHint', 'Swipe for additional view')
        };

        // compile template and load it
        var template = _.template(this.template);
        this.$el.html(template(tempVars));

        initControlsEvents();

        if (isDesktopMode) {
          if (aspectRatioTriggered) {
            calculateEmbedAspectRatios();
            addEmbedRatioEvents();
          }
        }
        if (!isMobileMode) {
          if (tempVars.hasmultiplevideos) {
            addLayoutEvents();
          }
        }

        if (tempVars.hasqualities) {
          addQualityChangeEvents();
        }

        // query ready state of video, in case the ready event from
        // the video plugin was fired before the controls plugin was initialized
        Engage.trigger(plugin.events.ready.getName(), true);

        // init dropdown menus
        $('.' + class_dropdown).dropdown();

        ready();
        playPause();
        timeUpdate();
        addNonFlashEvents();
        checkLoginStatus();
      }
    }
  });

  // provide this additional view if the controls are below the video to have content above the video
  var ControlsViewTop_ifBottom = Backbone.View.extend({
    el: $('#' + id_engage_controls_topIfBottom), // every view has an element associated with it
    initialize: function (videoDataModel, template, plugin_path) {
      this.setElement($(plugin.containerSecondIfBottom));
      this.model = videoDataModel;
      this.template = template;
      this.pluginPath = plugin_path;

      // bind the render function always to the view
      _.bindAll(this, 'render');
      // listen for changes of the model and bind the render function to this
      this.model.bind('change', this.render);
      this.render();
    },
    render: function () {
      if (!mediapackageError) {
        if (Engage.model.get('meInfo')) {
          if (Engage.model.get('meInfo').get('logo_player')) {
            logo = Engage.model.get('meInfo').get('logo_player');
          }
          if (Engage.model.get('meInfo').get('link_mediamodule')) {
            logoLink = window.location.protocol + '//' + window.location.host + '/engage/ui/index.html'; // link to the media module
          }
          if (!Engage.model.get('meInfo').get('show_embed_links')) {
            showEmbed = false;
          }
        }
        var tempVars = {
          plugin_path: this.pluginPath,
          logoLink: logoLink,
          str_openMediaModule: translate('openMediaModule', 'Go to Media Module'),
          str_embedButton: translate('embedButton', 'Embed Button. Select embed size from dropdown.'),
          str_fullscreen: translate('fullscreen', 'Fullscreen'),
          loggedIn: false,
          str_checkingStatus: translate('checkingLoginStatus', 'Checking login status...'),
          str_loginLogout: translate('loginLogout', 'Login/Logout'),
          logo: logo,
          show_embed: showEmbed
        };

        // compile template and load into the html
        var template = _.template(this.template);
        this.$el.html(template(tempVars));
      }
    }
  });

  function addNonFlashEvents() {
    if (!mediapackageError && !usingFlash && !isAudioOnly) {
      // setup listeners for the playback rate
      $('#' + id_playbackRate075).click(function (e) {
        e.preventDefault();
        $('#' + id_playbackRateIndicator).html(Utils.getFormattedPlaybackRate(0.75));
        Engage.trigger(plugin.events.playbackRateChanged.getName(), 0.75);
      });
      $('#' + id_playbackRate100).click(function (e) {
        e.preventDefault();
        $('#' + id_playbackRateIndicator).html(Utils.getFormattedPlaybackRate(1.0));
        Engage.trigger(plugin.events.playbackRateChanged.getName(), 1.0);
      });
      $('#' + id_playbackRate125).click(function (e) {
        e.preventDefault();
        $('#' + id_playbackRateIndicator).html(Utils.getFormattedPlaybackRate(1.25));
        Engage.trigger(plugin.events.playbackRateChanged.getName(), 1.25);
      });
      $('#' + id_playbackRate150).click(function (e) {
        e.preventDefault();
        $('#' + id_playbackRateIndicator).html(Utils.getFormattedPlaybackRate(1.5));
        Engage.trigger(plugin.events.playbackRateChanged.getName(), 1.5);
      });
      $('#' + id_playbackRate200).click(function (e) {
        e.preventDefault();
        $('#' + id_playbackRateIndicator).html(Utils.getFormattedPlaybackRate(2.0));
        Engage.trigger(plugin.events.playbackRateChanged.getName(), 2.0);
      });
      $('#' + id_playbackRate300).click(function (e) {
        e.preventDefault();
        $('#' + id_playbackRateIndicator).html(Utils.getFormattedPlaybackRate(3.0));
        Engage.trigger(plugin.events.playbackRateChanged.getName(), 3.0);
      });
    }
  }

  function addQualityChangeEvents() {
    if (!mediapackageError) {
      for (var i = 0; i < resolutions.length; i++) {
        var quality = resolutions[i];
        addQualityListener(quality);
      }
      var q = Engage.model.get('quality');
      $('#' + id_qualityIndicator).html(q.charAt(0).toUpperCase() + q.substring(1));
    }
  }

  function addQualityListener(quality) {
    $('#quality' + quality).click(function (element) {
      element.preventDefault();
      $('#' + id_qualityIndicator).html(translate(quality, quality));
      Engage.trigger(plugin.events.qualitySet.getName(), quality);
    });
  }

  function addLayoutEvents() {
    $('#' + id_pipLeft).click(function (e) {
      e.preventDefault();
      $('.videoDisplay').trigger('click');
      $('#' + id_pipIndicator).html(translate('left', 'left'));
      Engage.trigger(plugin.events.movePiP.getName(), 'left');
      Engage.trigger(plugin.events.togglePiP.getName(), true);
    });
    $('#' + id_pipRight).click(function (e) {
      e.preventDefault();
      $('.videoDisplay').trigger('click');
      $('#' + id_pipIndicator).html(translate('right', 'right'));
      Engage.trigger(plugin.events.movePiP.getName(), 'right');
      Engage.trigger(plugin.events.togglePiP.getName(), true);
    });
    $('#' + id_pipBeside).click(function (e) {
      e.preventDefault();
      $('.videoDisplay').trigger('click');
      $('#' + id_pipIndicator).html(translate('beside', 'beside'));
      Engage.trigger(plugin.events.togglePiP.getName(), false);
    });
    $('#' + id_pipOff).click(function (e) {
      e.preventDefault();
      $('.videoDisplay').trigger('click');
      $('#' + id_pipIndicator).html(translate('off', 'off'));
      Engage.trigger(plugin.events.resetLayout.getName());
    });
  }

  function addZoomEvents() {
    if(!usingFlash) {
      $('#' + id_zoomLevelIndicator).html('100%');

      /* Events for Button */
      $('#' + id_zoomLevel1).click(function (event) {
        event.preventDefault();
        Engage.trigger(plugin.events.setZoomLevel.getName(), [1.5, true]);
      });

      $('#' + id_zoomLevel2).click(function (event) {
        event.preventDefault();
        Engage.trigger(plugin.events.setZoomLevel.getName(), [2.0, true]);
      });

      $('#' + id_zoomLevel3).click(function (event) {
        event.preventDefault();
        Engage.trigger(plugin.events.setZoomLevel.getName(), [2.5, true]);
      });

      $('#' + id_zoomReset).click(function (event) {
        event.preventDefault();
        Engage.trigger(plugin.events.zoomReset.getName(), true);
      });

      Engage.on(plugin.events.zoomChange.getName(), function (level) {
        level = Number(level).toFixed(2);
        level = (level * 100).toFixed(0);
        $('#' + id_zoomLevelIndicator).html(String(level) + '%');
      });
    }
  }

  function triggerEmbedMessage(ratioWidth, ratioHeight) {
    var str = window.location.href;
    if (str.indexOf('mode=desktop') == -1) {
      str += '&mode=embed';
    } else {
      str = Utils.replaceAll(str, 'mode=desktop', 'mode=embed');
    }
    var code = '<iframe src=\'' + str + '\' style=\'border:0px #FFFFFF none;\' name=\'Opencast media player\' scrolling=\'no\' frameborder=\'0\' marginheight=\'0px\' marginwidth=\'0px\' width=\'' + ratioWidth + '\' height=\'' + ratioHeight + '\' allowfullscreen=\'true\' webkitallowfullscreen=\'true\' mozallowfullscreen=\'true\'></iframe>';
    code = Utils.escapeHtml(code);
    Engage.trigger(plugin.events.customOKMessage.getName(), 'Copy the following code and paste it to the body of your html page: <div class=\'well well-sm well-alert\'>' + code + '</div>');
  }

  function addEmbedRatioEvents() {
    if (!mediapackageError) {
      // setup listeners for the embed buttons
      $('#' + id_embed0).click(function (e) {
        e.preventDefault();
        triggerEmbedMessage(embedWidthOne, embedHeightOne);
      });
      $('#' + id_embed1).click(function (e) {
        e.preventDefault();
        triggerEmbedMessage(embedWidthTwo, embedHeightTwo);
      });
      $('#' + id_embed2).click(function (e) {
        e.preventDefault();
        triggerEmbedMessage(embedWidthThree, embedHeightThree);
      });
      $('#' + id_embed3).click(function (e) {
        e.preventDefault();
        triggerEmbedMessage(embedWidthFour, embedHeightFour);
      });
      $('#' + id_embed4).click(function (e) {
        e.preventDefault();
        triggerEmbedMessage(embedWidthFive, embedHeightFive);
      });
    }
  }

  function loadStoredInitialValues() {
    var vol = Basil.get(storage_volume);
    if (vol) {
      Basil.set(storage_lastvolume, vol);
      Engage.trigger(plugin.events.volumeSet.getName(), vol / 100);
    }

    var muted = Basil.get(storage_muted);
    if (muted == 'true') {
      Engage.trigger(plugin.events.mute.getName());
    } else {
      Engage.trigger(plugin.events.unmute.getName());
    }

    if (Basil.get(storage_pip_pos) !== undefined && Basil.get(storage_pip_pos) !== null) {
      var pipPos = Basil.get(storage_pip_pos);
      Engage.trigger(plugin.events.movePiP.getName(), pipPos);
    } else {
      if (Engage.model.get('meInfo').get('layout') !== 'off') {
        var pipPos = Engage.model.get('meInfo').get('layout');
        Engage.trigger(plugin.events.movePiP.getName(), pipPos);
      }
    }
    if (Basil.get(storage_pip) !== undefined && Basil.get(storage_pip) !== null) {
      var pip = Basil.get(storage_pip);
      if (pip === false) {
        Engage.trigger(plugin.events.togglePiP.getName(), pip);
      }
    } else {
      if (Engage.model.get('meInfo').get('layout') === 'off' ||
        Engage.model.get('meInfo').get('layout') === 'beside') {
        Engage.trigger(plugin.events.togglePiP.getName(), false);
      } else {
        Engage.trigger(plugin.events.togglePiP.getName(), true);
      }
    }
    if (Basil.get(storage_focus_video) !== undefined && Basil.get(storage_focus_video) !== null) {
      var focusVideo = Basil.get(storage_focus_video);
      currentFocusFlavor = focusVideo;
    } else {
      if (Engage.model.get('meInfo').get('layout') !== 'off') {
        currentFocusFlavor = Engage.model.get('meInfo').get('focusedflavor');
      }
    }
  }

  function initControlsEvents() {
    if (!mediapackageError) {
      // disable not used buttons
      Utils.disable(id_backward_button);
      Utils.disable(id_forward_button);
      Utils.disable(id_play_button);
      Utils.greyOut(id_backward_button);
      Utils.greyOut(id_forward_button);
      Utils.greyOut(id_play_button);
      Utils.disable(id_navigation_time);
      $('#' + id_navigation_time_current).keyup(function (e) {
        e.preventDefault();
        // enter
        if (e.keyCode === 13) {
          $(this).blur();
          try {
            var time = Utils.getTimeInMilliseconds($(this).val());
            if (!isNaN(time)) {
              Engage.trigger(plugin.events.seek.getName(), time / 1000);
            }
          } catch (e) {
            Engage.trigger(plugin.events.seek.getName(), 0);
          }
        }
      });

      $('#' + id_slider).slider({
        range: 'min',
        min: 0,
        max: 1000,
        value: 0
      });

      $('#' + id_volumeSlider).slider({
        range: 'min',
        min: 0,
        max: 100,
        value: 100,
        change: function (event, ui) {}
      });

      // use as mute button in desktop mode
      if (!isMobileMode) {
        $('#' + id_volumeIcon).click(function () {
          var isMute = Basil.get(storage_muted);
          if (isMute == 'true') {
            Engage.trigger(plugin.events.unmute.getName());
            Basil.set(storage_muted, 'false');
          } else {
            Engage.trigger(plugin.events.mute.getName());
            Basil.set(storage_muted, 'true');
          }
        });
      }

      $('#' + id_playpause_controls).click(function (e) {
        e.stopPropagation();
        Engage.trigger(plugin.events.playPause.getName());
      });

      $('#' + id_forward_button).click(function () {
        if (segments && (segments.length > 0)) {
          Engage.trigger(plugin.events.nextChapter.getName());
        } else {
          Engage.trigger(plugin.events.seekRight.getName());
        }
      });

      $('#' + id_backward_button).click(function () {
        if (segments && (segments.length > 0)) {
          Engage.trigger(plugin.events.previousChapter.getName());
        } else {
          Engage.trigger(plugin.events.seekLeft.getName());
        }
      });

      $('#' + id_fullscreen_button).click(function (e) {
        e.preventDefault();

        $(this).toggleClass('active');
        var isInFullScreen = document.fullScreen ||
        document.mozFullScreen ||
        document.webkitIsFullScreen;
        if (!isInFullScreen) {
          Engage.trigger(plugin.events.fullscreenEnable.getName());
        } else if (isMobileMode) {
          Engage.trigger(plugin.events.fullscreenCancel.getName());
        }
      });

      $('#' + id_captions_button).click(function (e) {
        e.preventDefault();

        $(this).toggleClass('active');
        captionsOn  = !captionsOn;
        Engage.trigger(plugin.events.toggleCaptions.getName(), captionsOn);
      })
      // slider events
      $('#' + id_slider).on(event_slidestart, function (event, ui) {
        isSliding = true;
        Engage.trigger(plugin.events.sliderStart.getName(), ui.value);
      });
      $('#' + id_slider).on(event_slidestop, function (event, ui) {
        isSliding = false;
        Engage.trigger(plugin.events.sliderStop.getName(), ui.value);
      });
      $('#' + id_slider).mouseover(function (e) {
        e.preventDefault();
        Engage.trigger(plugin.events.sliderMousein.getName());
      }).mouseout(function (e) {
        e.preventDefault();
        Engage.trigger(plugin.events.sliderMouseout.getName());
      }).mousemove(function (e) {
        e.preventDefault();
        var currPos = e.clientX / ($('#' + id_slider).width() + $('#' + id_slider).offset().left);
        var dur = (duration && (duration > 0)) ? duration : 1;
        currPos = (currPos < 0) ? 0 : ((currPos > 1) ? 1 : currPos);
        Engage.trigger(plugin.events.sliderMousemove.getName(), currPos * dur);
      });
      // volume event
      $('#' + id_volumeSlider).on(event_slide, function (event, ui) {
        Engage.trigger(plugin.events.volumeSet.getName(), ui.value / 100);
        if (!isMobileMode) {
          if (ui.value === 0) {
            showMuteButton();
          } else {
            showUnmuteButton();
          }
        }
      });
      // check segments
      if (segments && (segments.length > 0)) {
        Engage.log('Controls: ' + segments.length + ' segments are available.');
        $.each(segments, function (i, v) {
          $('#' + id_segmentNo + i).click(function (e) {
            e.preventDefault();
            var time = parseInt($(this).children().html());
            if (!isNaN(time)) {
              Engage.trigger(plugin.events.seek.getName(), time / 1000);
            }
          });
          $('#' + id_segmentNo + i).mouseover(function (e) {
            e.preventDefault();
            Engage.trigger(plugin.events.segmentMouseover.getName(), i);
          }).mouseout(function (e) {
            e.preventDefault();
            Engage.trigger(plugin.events.segmentMouseout.getName(), i);
          });
        });
      }

      // register special events for mobile template
      if (isMobileMode) {
        $('#' + id_videoWrapper).hammer().bind('tap', function () {
          Engage.trigger(plugin.events.showControls.getName());
        });

        $('.videoDisplay').hammer().bind('tap', function () {
          if (!$(this).hasClass('active')) {
            var id = (+this.id.replace('videoDisplay','')) - 1;
            carousel.show(id);
          }
        });

        $('#' + id_big_play_button).click(function (e) {
          e.stopPropagation();
          Engage.trigger(plugin.events.play.getName(), false);
        });

        $('#' + id_volumeIcon).hammer().bind('tap', function () {
          // toggle volume slider in mobile/embed mode
          $(this).parent().toggleClass('active');
        });

        // create a VideoCarousel hammer.js instance for touch gesture support
        carousel = new VideoCarousel($('#' + id_gestureContainer));
      }
    }
  }

  function getVolume() {
    var isMute = Basil.get(storage_muted);
    if (isMute == 'true') {
      return 0;
    } else {
      var vol = $('#' + id_volumeSlider).slider('option', 'value');
      return vol;
    }
  }

  function calculateEmbedAspectRatios() {
    if ((aspectRatioWidth > 0) && (aspectRatioHeight > 0)) {
      embedWidthOne = Utils.getAspectRatioWidth(aspectRatioWidth, aspectRatioHeight, embedHeightOne);
      embedWidthTwo = Utils.getAspectRatioWidth(aspectRatioWidth, aspectRatioHeight, embedHeightTwo);
      embedWidthThree = Utils.getAspectRatioWidth(aspectRatioWidth, aspectRatioHeight, embedHeightThree);
      embedWidthFour = Utils.getAspectRatioWidth(aspectRatioWidth, aspectRatioHeight, embedHeightFour);
      embedWidthFive = Utils.getAspectRatioWidth(aspectRatioWidth, aspectRatioHeight, embedHeightFive);

      $('#' + id_embed0).html('Embed ' + embedWidthOne + 'x' + embedHeightOne);
      $('#' + id_embed1).html('Embed ' + embedWidthTwo + 'x' + embedHeightTwo);
      $('#' + id_embed2).html('Embed ' + embedWidthThree + 'x' + embedHeightThree);
      $('#' + id_embed3).html('Embed ' + embedWidthFour + 'x' + embedHeightFour);
      $('#' + id_embed4).html('Embed ' + embedWidthFive + 'x' + embedHeightFive);
    } else {
      embedWidthOne = 310;
      embedHeightOne = 70;

      $('#' + id_embed0).html('Embed ' + embedWidthOne + 'x' + embedHeightOne);
      Utils.removeParentIfElementExists(id_embed1);
      Utils.removeParentIfElementExists(id_embed2);
      Utils.removeParentIfElementExists(id_embed3);
      Utils.removeParentIfElementExists(id_embed4);
    }

    $('#' + id_embed_button).removeClass('disabled');
  }

  function ready() {
    if (videosReady) {
      loadStoredInitialValues();
      Utils.greyIn(id_play_button);
      Utils.enable(id_play_button);
      Utils.greyIn(id_forward_button);
      Utils.enable(id_forward_button);
      Utils.greyIn(id_backward_button);
      Utils.enable(id_backward_button);
      if (!isAudioOnly) {
        enableFullscreenButton = true;
        $('#' + id_fullscreen_button).removeClass('disabled');
      }

      if (isDesktopMode) {
        $('#' + id_pipIndicator).html(translate('off', 'off'));
        Engage.trigger(plugin.events.movePiP.getName(), pipPos);
        Engage.trigger(plugin.events.togglePiP.getName(), pipStatus);
      }

      if (videosInitialReadyness) {
        Engage.trigger(plugin.events.focusVideo.getName(), currentFocusFlavor);
        videosInitialReadyness = false;
      }
      if (Engage.model.get("captions")) {
        $("#" + id_captions_button).removeClass("disabled");
      }
    }
  }

  function playPause() {
    if (isPlaying) {
      if (isMobileMode) {
        $('#' + id_big_play_button).fadeOut();
      }
      $('#' + id_play_button).hide();
      $('#' + id_pause_button).show();
      if (!usingFlash && !isAudioOnly) {
        $('#' + id_dropdownMenuPlaybackRate).removeClass('disabled');
      }
    } else {
      if (isMobileMode) {
        $('#' + id_big_play_button).fadeIn();
      }
      $('#' + id_play_button).show();
      $('#' + id_pause_button).hide();
    }
  }

  function mute() {
    showMuteButton();
    Engage.trigger(plugin.events.volumeSet.getName(), 0);
  }

  function unmute() {
    showUnmuteButton();
    var vol = Basil.get(storage_lastvolume);
    if (vol) {
      Engage.trigger(plugin.events.volumeSet.getName(), vol / 100);
    } else {
      Engage.trigger(plugin.events.volumeSet.getName(), 1);
    }
  }

  function showUnmuteButton () {
    $('#' + id_unmute_button).show();
    $('#' + id_mute_button).hide();
  }

  function showMuteButton () {
    $('#' + id_unmute_button).hide();
    $('#' + id_mute_button).show();
  }

  function timeUpdate() {
    if (videosReady) {
      // set slider
      var duration = parseInt(Engage.model.get('videoDataModel').get('duration'));
      if (!isSliding && duration) {
        var normTime = (currentTime / (duration / 1000)) * 1000;
        $('#' + id_slider).slider('option', 'value', normTime);
        if (!$('#' + id_navigation_time_current).is(':focus')) {
          // distinguish between desktop and mobile, because in desktop mode
          // an input field is used
          if (isDesktopMode)
            $('#' + id_navigation_time_current).val(Utils.formatSeconds(currentTime));
          else
            $('#' + id_navigation_time_current).text(Utils.formatSeconds(currentTime));
        }
      }
      var val = Math.round((duration / 1000) - currentTime);
      val = ((val >= 0) && (val <= (duration / 1000))) ? val : '-';
      $('#' + id_playbackRemTime075).html(Utils.formatSeconds(!isNaN(val) ? (val / 0.75) : val));
      $('#' + id_playbackRemTime100).html(Utils.formatSeconds(!isNaN(val) ? (val) : val));
      $('#' + id_playbackRemTime125).html(Utils.formatSeconds(!isNaN(val) ? (val / 1.25) : val));
      $('#' + id_playbackRemTime150).html(Utils.formatSeconds(!isNaN(val) ? (val / 1.5) : val));
      $('#' + id_playbackRemTime200).html(Utils.formatSeconds(!isNaN(val) ? (val / 2.0) : val));
      $('#' + id_playbackRemTime300).html(Utils.formatSeconds(!isNaN(val) ? (val / 3.0) : val));
    } else {
      $('#' + id_slider).slider('option', 'value', 0);
    }
  }

  function VideoCarousel(container) {
    this.container = container;
    this.width = container.width();

    this.currentIndex = 0;
    this.length = Engage.model.get('videoDataModel').get('ids').length;

    this.hammer = new Hammer.Manager(this.container.get(0));
    this.hammer.add(new Hammer.Pan({ direction: Hammer.DIRECTION_HORIZONTAL, threshold: 10 }));
    this.hammer.on('panstart panmove panend pancancel', Hammer.bindFn(this.onPan, this));
  }

  VideoCarousel.prototype = {
    onPan: function (ev) {
      // remove animation class to stop css transitions from interfering with user input
      if (ev.type === 'panstart') {
        this.container.removeClass('animate');
      }

      // stick to the finger
      var delta = ev.deltaX;

      // slow down at the first and last pane
      if((this.currentIndex === 0 && ev.offsetDirection === Hammer.DIRECTION_RIGHT) ||
       (this.currentIndex === this.length - 1 && ev.offsetDirection === Hammer.DIRECTION_LEFT)) {
        delta *= 0.3;
      }

      this.width = this.container.width();
      var percent = (100 / this.width) * delta;
      var showIndex = this.currentIndex;

      if (ev.type === 'panend' || ev.type === 'pancancel') {
        if (Math.abs(percent) > 20 && ev.type === 'panend') {
          Engage.log('Switch video by pan');
          showIndex += (percent < 0) ? 1 : -1;
        }
        percent = 0;
        this.container.addClass('animate');
      }

      this.show(showIndex, percent);
    },

    show: function (showIndex, percent) {
      showIndex = Math.max(0, Math.min(showIndex, this.length - 1));
      percent = percent || 0;

      var pos = -(showIndex * 100) + percent;
      this.container.css({'transform': 'translateX(' + pos + '%)'});

      // if the pane should actually be switched
      if (showIndex !== this.currentIndex) {
        // remove old classes
        $('#' + id_engage_controls).removeClass('first last');
        // add 'first' or 'last' class to wrapper if it's the first or last video showing
        if (showIndex === 0 || showIndex === (this.length - 1)) {
          $('#' + id_engage_controls).addClass((showIndex === 0) ? 'first' : 'last');
        }

        $('#current_video_id').text(showIndex + 1);

        Engage.trigger(plugin.events.switchVideo.getName(), showIndex);
      }

      this.currentIndex = showIndex;
    }
  };

  /**
   * Initializes the plugin
   */
  function initPlugin() {
    // Check that the videoDataModel is available.
    if (! Engage.model.get('videoDataModel')) {
      Engage.on(videoDataModelChange, function() {
        Engage.log("Controls: videoDataModel available.");
        initPlugin();
      });
      window.setTimeout(function() {
        if (Engage.model.get('videoDataModel') && ! inited) {
          initPlugin();
        }
      }, 500);
      Engage.log("Controls: videoDataModel not available at start.");
      return;
    }
    // only init if plugin template was inserted into the DOM
    if (plugin.inserted && ! inited) {
      inited = true;
      Engage.on(plugin.events.videoFormatsFound.getName(), function (formatarr) {
        if (Array.isArray(formatarr)) {
          resolutions = formatarr;
          if (controlsViewTopIfBottom) {
            controlsViewTopIfBottom.render();
          }
          if (controlsView) {
            controlsView.render();
          }
          addQualityChangeEvents();
        }
      });

      // retrigger the event in case the videoFormatsFound event from the video plugin
      // was fired before the controls plugin was initialized
      // to make sure the quality dropdown menu is shown
      Engage.trigger(plugin.events.videoFormatsFound.getName(), true);

      Engage.on(plugin.events.numberOfVideodisplaysSet.getName(), function (number) {
        numberVideos = number;

        if (number > 1) {
          if (controlsViewTopIfBottom) {
            controlsViewTopIfBottom.render();
          }
          if (controlsView) {
            controlsView.render();
          }
          if (isDesktopMode) {
            addLayoutEvents();
          }
        }
      });
      Engage.on(plugin.events.aspectRatioSet.getName(), function (as) {
        if (as) {
          aspectRatioWidth = as[0] || 0;
          aspectRatioHeight = as[1] || 0;
          aspectRatio = as[2] || 0;
          aspectRatioTriggered = true;
          if (isDesktopMode) {
            calculateEmbedAspectRatios();
            addEmbedRatioEvents();
          }
        }
      });
      Engage.on(plugin.events.mediaPackageModelError.getName(), function (msg) {
        mediapackageError = true;
      });
      Engage.on(plugin.events.usingFlash.getName(), function (flash) {
        usingFlash = flash;
        addNonFlashEvents();
      });
      Engage.on(plugin.events.isAudioOnly.getName(), function (audio) {
        isAudioOnly = audio;
      });
      Engage.on(plugin.events.playbackRateChanged.getName(), function (pbr) {
        $('#' + id_playbackRateIndicator).html(Utils.getFormattedPlaybackRate(pbr));
      });
      Engage.on(plugin.events.volumeSet.getName(), function (volume) {
        $('#' + id_volumeSlider).slider('value', volume * 100);
        if ((volume * 100) > 1) {
          Basil.set(storage_lastvolume, volume * 100);
        }
        Basil.set(storage_volume, volume * 100);
        if (volume > 0) {
          Basil.set(storage_muted, 'false');
        } else {
          Basil.set(storage_muted, 'true');
        }

        // ui feedback in mobile mode
        if (isMobileMode) {
          var $el = $('#' + id_volumeIcon + ' span');
          if (volume === 0) {
            $el.removeClass('low');
            $el.addClass('muted');
          } else if (volume < 0.6) {
            $el.removeClass('muted');
            $el.addClass('low');
          } else {
            $el.removeClass('low muted');
          }
        }
      });
      Engage.on(plugin.events.volumeUp.getName(), function () {
        var vol = getVolume();
        if ((vol + volUpDown) <= 100) {
          Engage.trigger(plugin.events.volumeSet.getName(), (vol + volUpDown) / 100);
        } else {
          Engage.trigger(plugin.events.volumeSet.getName(), 1);
        }
        unmute();
      });
      Engage.on(plugin.events.volumeDown.getName(), function () {
        var vol = getVolume();
        if ((vol - volUpDown) > 0) {
          Engage.trigger(plugin.events.volumeSet.getName(), (vol - volUpDown) / 100);
          unmute();
        } else {
          Engage.trigger(plugin.events.mute.getName());
        }
      });
      Engage.on(plugin.events.ready.getName(), function (query) {
        if (query === true || videosReady === true) {
          return;
        }
        addZoomEvents();
        if (!mediapackageError) {
          videosReady = true;
          ready();
        }
      });
      Engage.on(plugin.events.play.getName(), function () {
        if (!mediapackageError && videosReady) {
          isPlaying = true;
          playPause();

          if (isMobileMode) {
            Engage.trigger(plugin.events.fullscreenEnable.getName());
            Engage.trigger(plugin.events.hideControls.getName());
            $('#swipe_hint').fadeOut();
          }
        }
      });
      Engage.on(plugin.events.pause.getName(), function (triggeredByMaster) {
        if (!mediapackageError && videosReady) {
          isPlaying = false;
          playPause();
        }
      });
      Engage.on(plugin.events.mute.getName(), function () {
        if (!mediapackageError) {
          mute();
        }
      });
      Engage.on(plugin.events.unmute.getName(), function () {
        if (!mediapackageError) {
          unmute();
        }
      });
      Engage.on(plugin.events.muteToggle.getName(), function () {
        if (!mediapackageError) {
          var muted = Basil.get(storage_muted);
          if (muted == 'true') {
            Engage.trigger(plugin.events.unmute.getName());
          } else {
            Engage.trigger(plugin.events.mute.getName());
          }
        }
      });
      Engage.on(plugin.events.fullscreenCancel.getName(), function () {
        if (isMobileMode && isPlaying) {
          Engage.trigger(plugin.events.pause.getName());
          Engage.trigger(plugin.events.showControls.getName());
        }
      });
      Engage.on(plugin.events.fullscreenChange.getName(), function () {
        var isInFullScreen = document.fullScreen || document.mozFullScreen || document.webkitIsFullScreen;
        if (!isInFullScreen) {
          Engage.trigger(plugin.events.fullscreenCancel.getName());
        }
      });
      Engage.on(plugin.events.timeupdate.getName(), function (_currentTime) {
        if (!mediapackageError) {
          currentTime = _currentTime;
          timeUpdate();
        }
      });
      Engage.on(plugin.events.ended.getName(), function () {
        if (!mediapackageError && videosReady) {
          Engage.trigger(plugin.events.pause);
          if (isMobileMode) {
            Engage.trigger(plugin.events.showControls.getName());
          }
        }
      });
      Engage.on(plugin.events.segmentMouseover.getName(), function (no) {
        if (!mediapackageError) {
          $('#' + id_segmentNo + no).addClass('segmentHover');
        }
      });
      Engage.on(plugin.events.segmentMouseout.getName(), function (no) {
        if (!mediapackageError) {
          $('#' + id_segmentNo + no).removeClass('segmentHover');
        }
      });

      // no pip in mobile mode
      if (!isMobileMode) {
        Engage.on(plugin.events.togglePiP.getName(), function (pip) {
          if (pip !== undefined) {
            Basil.set(storage_pip, pip);
            pipStatus = pip;
            if (currentFocusFlavor !== 'focus.none') {
              if (! pip) {
                $('#' + id_pipIndicator).html(translate('beside', 'beside'));
              } else {
                if (pipPos === 'left') {
                  $('#' + id_pipIndicator).html(translate('left', 'left'));
                } else {
                  $('#' + id_pipIndicator).html(translate('right', 'right'));
                }
              }
            }
          }
        });
        Engage.on(plugin.events.focusVideo.getName(), function (flavor) {
          if (flavor !== undefined && flavor !== null && flavor.indexOf('focus.') < 1) {
            Basil.set(storage_focus_video, flavor);
            var pip = Basil.get(storage_pip);
            currentFocusFlavor = flavor;
            if (pip === undefined || ! pip) {
              $('#' + id_pipIndicator).html(translate('beside', 'beside'));
            } else {
              if (pipPos === 'left') {
                $('#' + id_pipIndicator).html(translate('left', 'left'));
              } else {
                $('#' + id_pipIndicator).html(translate('right', 'right'));
              }
            }
          }
        });
        Engage.on(plugin.events.resetLayout.getName(), function () {
          Basil.set(storage_focus_video, 'focus.none');
          currentFocusFlavor = 'focus.none';
          $('#' + id_pipIndicator).html(translate('off', 'off'));
        });
        Engage.on(plugin.events.movePiP.getName(), function (pos) {
          if (pos !== undefined) {
            Basil.set(storage_pip_pos, pos);
            pipPos = pos;
            if (currentFocusFlavor !== 'focus.none') {
              if (pos === 'left') {
                $('#' + id_pipIndicator).html(translate('left', 'left'));
              } else {
                $('#' + id_pipIndicator).html(translate('right', 'right'));
              }
            }
          }
        });
        Engage.on(plugin.events.captionsFound.getName(), function () {
            $("#" + id_captions_button).removeClass("disabled");
        });
      }

      if (isMobileMode) {
        // register show- and hideControls event in mobile mode
        Engage.on(plugin.events.showControls.getName(), function () {
          if (!controlsVisible) {
            controlsVisible = true;
            $('#' + id_engage_controls).fadeIn();
            controlsTimer = Utils.timer.setup(function () {
              Engage.trigger(plugin.events.hideControls.getName());
            }, hideTimeout * 1000);
          } else {                            // when controls are visible
            if (controlsTimer) {
              controlsTimer.renew();
            }
          }
        });
        Engage.on(plugin.events.hideControls.getName(), function () {
          if (controlsVisible && isPlaying) {
            $('#' + id_engage_controls).fadeOut();
            controlsVisible = false;
          }
        });

        // add first class to video wrapper
        $('#' + id_engage_controls).addClass('first');
      }

      Engage.on(plugin.events.nextChapter.getName(), function () {
        if (segments && (segments.length > 0)) {
          var seekTime = Utils.nextSegmentStart(segments, currentTime);
          if (!isNaN(seekTime)) {
            Engage.trigger(plugin.events.seek.getName(), seekTime / 1000);
          }
        }
      });
      Engage.on(plugin.events.previousChapter.getName(), function () {
        if (segments && (segments.length > 0)) {
          var seekTime = Utils.previousSegmentStart(segments, currentTime);
          if (!isNaN(seekTime)) {
            Engage.trigger(plugin.events.seek.getName(), seekTime / 1000);
          }
        }
      });

      if (!Engage.controls_top && plugin.template_topIfBottom && (plugin.template_topIfBottom != 'none')) {
        controlsViewTopIfBottom = new ControlsViewTop_ifBottom(Engage.model.get('videoDataModel'), plugin.template_topIfBottom, plugin.pluginPath_topIfBottom);
      }
      controlsView = new ControlsView(Engage.model.get('videoDataModel'), plugin.template, plugin.pluginPath);

      loadStoredInitialValues();
    }
  }

  // init event
  Engage.log('Controls: Init');
  var relative_plugin_path = Engage.getPluginPath('EngagePluginControls');

  // listen on a change/set of the InfoMe model
  Engage.model.on(infoMeChange, function () {
    initCount -= 1;
    if (initCount == 0) {
      initPlugin();
    }
  });

  // listen on a change/set of the mediaPackage model
  Engage.model.on(mediapackageChange, function () {
    initCount -= 1;
    if (initCount == 0) {
      initPlugin();
    }
  });

  // all plugins loaded
  Engage.on(plugin.events.plugin_load_done.getName(), function () {
    Engage.log('Controls: Plugin load done');
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  // load jquery-ui lib
  require([relative_plugin_path + jQueryUIPath], function () {
    Engage.log('Controls: Lib jQuery UI loaded');

    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }

    // load jquery-ui touch-punch lib in mobile mode
    if (isMobileMode) {
      require([relative_plugin_path + 'lib/jquery.ui.touch-punch.min'], function () {
        Engage.log('Controls: Lib jQuery UI Touch Punch loaded');
        initCount -= 1;
        if (initCount <= 0) {
          initPlugin();
        }
      });
    }
  });

  // load hammer.js lib for gestures on mobile (touch enabled) devices
  if (isMobileMode) {
    require([relative_plugin_path + 'lib/hammer.min'], function () {
      Engage.log('Controls: Lib hammer.js loaded');
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }

      require([relative_plugin_path + 'lib/jquery.hammer'], function () {
        initCount -= 1;
        if (initCount <= 0) {
          initPlugin();
        }
      });
    });
  }

  // load utils class
  require([relative_plugin_path + "utils"], function(utils) {
      Engage.log("Controls: Utils class loaded");
      Utils = new utils();
      initTranslate(Engage.model.get("language"), function() {
          Engage.log("Controls: Successfully translated.");
          initCount -= 1;
          if (initCount <= 0) {
              initPlugin();
          }
      }, function() {
          Engage.log("Controls: Error translating...");
          initCount -= 1;
          if (initCount <= 0) {
              initPlugin();
          }
      });
  });

  return plugin;
});
