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
define(['require', 'jquery', 'underscore', 'backbone', 'basil', 'bowser', 'engage/core'], function(require, $, _, Backbone, Basil, Bowser, Engage) {
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
        ready: new Engage.Event('Video:ready', 'all videos loaded successfully', 'trigger'),
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
        aspectRatioSet: new Engage.Event('Video:aspectRatioSet', 'the aspect ratio has been calculated', 'trigger'),
        isAudioOnly: new Engage.Event('Video:isAudioOnly', 'whether it\'s audio only or not', 'trigger'),
        audioCodecNotSupported: new Engage.Event('Video:audioCodecNotSupported', 'when the audio codec seems not to be supported by the browser', 'trigger'),
        videoFormatsFound: new Engage.Event('Video:videoFormatsFound', '', 'trigger'),
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
        openVideo: new Engage.Event('Video:openVideo', 'opens a new videostream', 'handler')
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
    var videoPath = 'lib/video-js/video.min'; /* https://github.com/videojs/video.js/releases */
    var videojs_swf_path = 'lib/video-js/video-js.swf';
    var synchronizePath = 'lib/synchronize-min'; /* https://github.com/CallToPower/Synchronize.js */
    var mediaSourcesPath = 'lib/video-js/videojs-media-sources.min'; /* https://github.com/videojs/videojs-contrib-media-sources */
    var hlsPath = 'lib/video-js/videojs.hls.min'; /* https://github.com/videojs/videojs-contrib-hls */
    var videoAreaAspectRatio;
    var checkVideoDisplaySizeTimeout = 1500;
    var audioLoadTimeoutCheckDelay = 5000;
    var seekSeconds = 5;
    var interval_autoplay_ms = 1000;
    var interval_initialSeek_ms = 1000;
    var timeout_initialSeek_ms = 250;
    var timer_qualitychange = 1000;

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
    var aspectRatio = '';
    var singleVideoPaddingTop = '';
    var initCount = 7;
    var infoMeChange = 'change:infoMe';
    var mediapackageError = false;
    var videoDisplayNamePrefix = 'videojs_videodisplay_';
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
    var id_btn_video1 = 'btn-video1';
    var id_btn_video2 = 'btn-video2';
    var id_switchPlayer_value = 'switchPlayer-value';
    var id_audioDisplay = 'audioDisplay';
    var id_switchPlayers = 'switchPlayers';
    var class_vjs_switchPlayer = 'vjs-switchPlayer';
    var class_btn_video = 'btn-video';
    var class_vjs_menu_button = 'vjs-menu-button';
    var class_vjs_switchPlayer_value = 'vjs-switchPlayer-value';
    var class_vjs_menu = 'vjs-menu';
    var class_vjs_menu_content = 'vjs-menu-content';
    var class_vjs_menu_item = 'vjs-menu-item';
    var class_vjsposter = 'vjs-poster';
    var class_vjs_openInPlayer = 'vjs-openInPlayer';
    var class_vjs_control = 'vjs-control';
    var class_vjs_control_text = 'vjs-control-text';
    var class_vjs_mute_control = 'vjs-mute-control';
    var class_audio_wrapper = 'audio_wrapper';
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
    var loadDash = false;
    var loadHls = false;
    var flavors = '';
    var mimetypes = '';
    var translations = [];
    var videoDataView = undefined;
    var fullscreen = false;
    var videoDisplayClass = 'videoDisplay';
    
    var videoDefaultLayoutClass = 'videoDefaultLayout';
    var videoUnfocusedClass = 'videoUnfocusedPiP';
    var videoFocusedClass = 'videoFocusedPiP';
    var unfocusedPiPClass = 'videoUnfocusedPiP';
    var focusedPiPClass = 'videoFocusedPiP';
    var unfocusedClass = 'videoUnfocused';
    var focusedClass = 'videoFocused';
    var isPiP = true;
    var pipPos = 'left';
    
    var foundQualities = undefined;

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath('EngagePluginVideoVideoJS').replace(/(\.\.\/)/g, '');
        var jsonstr = window.location.origin + '/engage/theodul/' + path; // this solution is really bad, fix it...

        Engage.log('Controls: selecting language ' + language);
        jsonstr += 'language/' + language + '.json';
        $.ajax({
            url: jsonstr,
            dataType: 'json',
            success: function(data) {
                if (data) {
                    data.value_locale = language;
                    translations = data;
                    if (funcSuccess) {
                        funcSuccess(translations);
                    }
                } else {
                    if (funcError) {
                        funcError();
                    }
                }
            },
            error: function() {
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
    
    function filterTracksByTag(tracks, filterTags) {
        if (filterTags == undefined) {
            return tracks;
        }
        var filterTagsArray = filterTags.split(',');
        var newTracksArray = [];
        
        for (var i = 0; i < tracks.length; i++) {
            var found = false;
            for (var j = 0; j < tracks[i].tags.tag.length; j++) {
                for (var k = 0; k < filterTagsArray.length; k++) {
                    if (tracks[i].tags.tag[j] == filterTagsArray[k].trim()) {
                        found = true;
                        newTracksArray.push(tracks[i]);
                        break;
                    }
                }
                if (found) break;
            }
        }
        
        //avoid filtering to an empty list, better play something than nothing
        if (newTracksArray.length < 1 ) {
            return tracks;
        }
        return newTracksArray;
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
        var tagList = new Set(); // TODO: Set?
        
        for (var v in videoSources) {
            for (var i = 0; i < videoSources[v].length; i++) {
                for (var j = 0; j < videoSources[v][i].tags.tag.length; j++) {
                    if (keyword !== undefined) {
                        if (videoSources[v][i].tags.tag[j].indexOf(keyword) > 0) {
                            tagList.add(videoSources[v][i].tags.tag[j]);
                        }
                    } else {
                        tagList.add(videoSources[v][i].tags.tag[j]);
                    }
                } 
            }
        }
        
        return tagList;
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
        var qualitesList = [];
        var tagsList = getTags(videoSources, '-quality');
        tagsList.forEach(function(quality) {
            qualitesList.push(quality.substring(0, quality.indexOf('-quality')));
        });
        var tracks;
        for (var source in videoSources) {
            if (videoSources[source] !== undefined) {
                tracks = videoSources[source];
                break;
            }            
        }
        var sortedResolutionsList = [];
        for (var i = 0; i < qualitesList.length; i++){
            var currentTrack = filterTracksByTag(tracks, qualitesList[i] + '-quality')[0];
            sortedResolutionsList.push([qualitesList[i], currentTrack.resolution.substring(0, currentTrack.resolution.indexOf('x'))]);
        }
        sortedResolutionsList.sort(compareQuality);
        foundQualities = [];
        for (i = 0; i < sortedResolutionsList.length; i++){
            foundQualities.push(sortedResolutionsList[i][0]);
        }
        return foundQualities;
    }
    
    function compareQuality (a, b) {
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
        $(document).on(event_sjs_allPlayersReady, function() {
            videosReady = true;
            Engage.trigger(plugin.events.ready.getName());
        });
        $(document).on(event_sjs_playerLoaded, function() {
            Engage.trigger(plugin.events.playerLoaded.getName());
        });
        $(document).on(event_sjs_masterPlay, function() {
            Engage.trigger(plugin.events.play.getName(), true);
            pressedPlayOnce = true;
        });
        $(document).on(event_sjs_masterPause, function() {
            Engage.trigger(plugin.events.pause.getName(), true);
        });
        $(document).on(event_sjs_masterEnded, function() {
            Engage.trigger(plugin.events.ended.getName(), true);
        });
        $(document).on(event_sjs_masterTimeupdate, function(event, time) {
            Engage.trigger(plugin.events.timeupdate.getName(), time, true);
        });
        $(document).on(event_sjs_synchronizing, function() {
            Engage.trigger(plugin.events.synchronizing.getName());
        });
        $(document).on(event_sjs_buffering, function() {
            Engage.trigger(plugin.events.buffering.getName());
        });
        $(document).on(event_sjs_bufferedAndAutoplaying, function() {
            Engage.trigger(plugin.events.bufferedAndAutoplaying.getName());
        });
        $(document).on(event_sjs_bufferedButNotAutoplaying, function() {
            Engage.trigger(plugin.events.bufferedButNotAutoplaying.getName());
        });
    }

    function initSynchronize() {
        $(document).trigger(event_sjs_debug, Engage.model.get('isDebug'));
        $(document).trigger(event_sjs_stopBufferChecker);
    }

    function registerQualityChangeEvent() {
        Engage.on(plugin.events.qualitySet.getName(), function(q) {
            changeQuality(q);
        });
    }

    function changeQuality(q) {
        if (q) {
            var isPaused = videojs(globalVideoSource[0][0]).paused();
            Engage.trigger(plugin.events.pause.getName(), false);
            var quality = q + '-quality';
            Engage.model.set('quality', q);
            Engage.log('Setting quality to: ' + q);
            var tuples = getSortedVideosourcesArray(globalVideoSource);
            for (var i = 0; i < tuples.length; ++i) {
                var value = tuples[i][1];
                if (value[1][0]) {
                    var track = filterTracksByTag(value[1], quality);
                    if (track && track.length > 0) {
                        videojs(value[0]).src(track[0].src);
                    }
                }
            }
            if (pressedPlayOnce && (currentTime > 0)) {
                window.setTimeout(function() {
                    initSynchronize(false);
                    Engage.trigger(plugin.events.seek.getName(), currentTime);
                    if (! isPaused) {
                        Engage.trigger(plugin.events.play.getName());
                    }
                }, timer_qualitychange);
            } 
        }
    }

    function initializeVideoJsGlobally(videoDataView, videoDisplays, tuples) {
        Engage.log('Preparing video.js video displays globally.');
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
            .each(function(index) {
                if ((index % 2) == 1) {
                    $(videoDataView).css('float', 'right');
                }
            });
        for (var j = 0; j < videoDisplays.length; ++j) {
            $('#' + videoDisplays[j]).css('padding-top', (aspectRatio[2] / aspectRatio[1] * 100) + '%').addClass('auto-height');
            singleVideoPaddingTop = (aspectRatio[2] / aspectRatio[1] * 100) + '%';
        }
    }

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
        Engage.log('Rendering for desktop view');

        var tuples = getSortedVideosourcesArray(videoSources);

        initializeVideoJsGlobally(videoDataView, videoDisplays, tuples);

        var qualities = getQualities(videoSources);
        
        if (qualities.length > 1) {
            registerQualityChangeEvent();
            Engage.trigger(plugin.events.videoFormatsFound.getName(), qualities);
            changeQuality(Engage.model.get('quality'));
        }

        if ((aspectRatio != null) && (videoDisplays.length > 0)) {
            calculateAspectRatioForVideos(videoDataView, videoDisplays, aspectRatio);
        } else {
            Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
        }

        // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
        $('.' + class_vjsposter).show();

        Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

        if (videoDisplays.length > 0) {
            var nr = tuples.length;

            // set first videoDisplay as master
            registerEvents(isAudioOnly ? id_audioDisplay : videoDisplays[0], videoDisplays.length);

            if (nr >= 2) {
                synchronizeVideos(videoDisplays);
            } else {
                videosReady = true;
                if (!isAudioOnly) {
                    Engage.trigger(plugin.events.ready.getName());
                }
            }

            if (videoDataView.model.get('type') != 'audio') {
                $(window).resize(function() {
                    checkVideoDisplaySize();
                });
            }
        }
    }

    // TODO: this is just a temporary solution until the embed player has been designed and implemented
    function appendEmbedPlayer_switchPlayers() {
        $('.' + class_vjs_mute_control).after("<div id=\"" + id_btn_switchPlayer + "\" class=\"" + class_vjs_switchPlayer + " " + class_vjs_control + " " + class_vjs_menu_button + "\" role=\"button\" aria-live=\"polite\" tabindex=\"0\"></div>");
        $("#" + id_btn_switchPlayer).append(
            "<div class=\"vjs-control-content\">" +
            "<span class=\"" + class_vjs_control_text + "\">" + translate("switchPlayer", "Switch player") + "</span>" +
            "</div>" +
            "<div id=\"" + id_switchPlayer_value + "\" class=\"" + class_vjs_switchPlayer_value + "\">" +
            "Vid. 1" +
            "</div>" +
            "<div class=\"" + class_vjs_menu + "\">" +
            "<ul class=\"" + class_vjs_menu_content + "\">" +
            "<li id=\"" + id_btn_video1 + "\" aria-selected=\"true\" tabindex=\"0\" aria-live=\"polite\" role=\"button\" class=\"" + class_vjs_menu_item + " " + class_btn_video + "\">" + translate("video", "Video") + " 1</li>" +
            "<li id=\"" + id_btn_video2 + "\" aria-selected=\"false\" tabindex=\"0\" aria-live=\"polite\" role=\"button\" class=\"" + class_vjs_menu_item + " " + class_btn_video + "\">" + translate("video", "Video") + " 2</li>" +
            "</ul>" +
            "</div>"
        );
        $('#' + id_btn_video1).click(function() {
            $('#' + id_switchPlayer_value).html(translate('video_short', 'Vid.') + ' 1');
            if (currentlySelectedVideodisplay !== 0) {
                currentlySelectedVideodisplay = 0;
                videojs(globalVideoSource[0].id).src(globalVideoSource[0].src);
            }
        });
        $('#' + id_btn_video2).click(function() {
            $('#' + id_switchPlayer_value).html(translate('video_short', 'Vid.') + ' 2');
            if (currentlySelectedVideodisplay !== 1) {
                currentlySelectedVideodisplay = 1;
                videojs(globalVideoSource[1].id).src(globalVideoSource[1].src);
            }
        });
    }

    // TODO: this is just a temporary solution until the mobile player has been designed and implemented
    function appendMobilePlayer_switchPlayers() {
        $('#' + id_switchPlayers).html(
            translate('switchPlayer', 'Switch player') + ':' +
            '<ul class="nav nav-pills">' +
            '<li id="' + id_btn_video1 + '" role="presentation" class="active"><a href="#">' +
            translate("video_short", "Vid.") + ' 1' +
            '</a></li>' +
            '<li id="' + id_btn_video2 + '" role="presentation"><a href="#">' +
            translate("video_short", "Vid.") + ' 2' +
            '</a></li>' +
            '</ul>'
        );

        $('#' + id_btn_video1).click(function() {
            if (currentlySelectedVideodisplay !== 0) {
                $('#' + id_btn_video1).addClass('active');
                $('#' + id_btn_video2).removeClass('active');
                currentlySelectedVideodisplay = 0;
                videojs(globalVideoSource[0].id).src(globalVideoSource[0].src);
            }
        });
        $('#' + id_btn_video2).click(function() {
            if (currentlySelectedVideodisplay !== 1) {
                $('#' + id_btn_video1).removeClass('active');
                $('#' + id_btn_video2).addClass('active');
                currentlySelectedVideodisplay = 1;
                videojs(globalVideoSource[1].id).src(globalVideoSource[1].src);
            }
        });
    }

    // TODO: this is just a temporary solution until the embed player has been designed and implemented
    function appendEmbedPlayer_openInPlayer() {
        $('.' + class_vjs_mute_control).after("<div id=\"" + id_btn_openInPlayer + "\" class=\"" + class_vjs_openInPlayer + " " + class_vjs_control + "\" role=\"button\" aria-live=\"polite\" tabindex=\"0\"><div><span class=\"" + class_vjs_control_text + "\">" + translate("openInPlayer", "Open in player") + "</span></div></div>");
        $('.' + class_audio_wrapper).append("<a id=\"" + id_btn_openInPlayer + "\" href=\"#\">" + translate("openInPlayer", "Open in player") + "</a>");

        $('#' + id_btn_openInPlayer).click(function(e) {
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

        if ((videoDisplays.length > 1) && (globalVideoSource.length > 1)) {
            appendEmbedPlayer_switchPlayers();
        }
        appendEmbedPlayer_openInPlayer();

        if ((aspectRatio != null) && (videoDisplays.length > 0)) {
            aspectRatio[1] = parseInt(aspectRatio[1]);
            aspectRatio[2] = parseInt(aspectRatio[2]);
            Engage.log('Video: Aspect ratio: ' + aspectRatio[1] + 'x' + aspectRatio[2] + ' == ' + ((aspectRatio[2] / aspectRatio[1]) * 100));
            Engage.trigger(plugin.events.aspectRatioSet.getName(), aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100);
            $('.' + id_videoDisplayClass).css('width', '100%');
            for (i = 0; i < videoDisplays.length; ++i) {
                $('#' + videoDisplays[i]).css('padding-top', (aspectRatio[2] / aspectRatio[1] * 100) + '%').addClass('auto-height');
            }
        } else {
            Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
        }

        // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
        $('.' + class_vjsposter).show();
        Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

        if (videoDisplays.length > 0) {
            // set first videoDisplay as master
            registerEvents(isAudioOnly ? id_audioDisplay : videoDisplays[0], 1);

            videosReady = true;
            Engage.trigger(plugin.events.ready.getName());

            if (videoDataView.model.get('type') != 'audio') {
                $(window).resize(function() {
                    checkVideoDisplaySize();
                });
            }
        }
    }

    function renderMobile(videoDataView, videoSources, videoDisplays, aspectRatio) {
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

        if ((videoDisplays.length > 1) && (globalVideoSource.length > 1)) {
            appendMobilePlayer_switchPlayers();
        }
        // appendEmbedPlayer_openInPlayer();

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
            // set first videoDisplay as master
            registerEvents(isAudioOnly ? id_audioDisplay : videoDisplays[0], 1);

            videosReady = true;
            Engage.trigger(plugin.events.ready.getName());

            if (videoDataView.model.get('type') != 'audio') {
                $(window).resize(function() {
                    checkVideoDisplaySize();
                });
            }
        }
    }

    function calculateAspectRatio(videoSources) {
        Engage.log('Video: Calculating Aspect ratio');
        aspectRatio = null;
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
        Engage.log('Calculated aspect ratio: ' + aspectRatio);
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
        if ((isEmbedMode || isMobileMode) && !isAudioOnly) {
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
    }

    function prepareRenderingVideoDisplay(videoDataView) {
        if (loadHls) {
            require([relative_plugin_path + mediaSourcesPath], function() {
                Engage.log('Video: Lib videojs media sources loaded');
                require([relative_plugin_path + hlsPath], function() {
                    Engage.log('Video: Lib videojs HLS playback loaded');
                    renderVideoDisplay(videoDataView);
                });
            });
        } else {
            renderVideoDisplay(videoDataView);
        }
    }

    var VideoDataView = Backbone.View.extend({
        el: $('#' + id_engage_video),
        initialize: function(videoDataModel, template, videojs_swf) {
            this.setElement($(plugin.container));
            this.model = videoDataModel;
            this.template = template;
            this.videojs_swf = videojs_swf;
            _.bindAll(this, 'render');
            this.model.bind('change', this.render);
            this.render();
        },
        render: function() {
            prepareRenderingVideoDisplay(this);
        }
    });

    function initVideojsVideo(id, videoSource, videojs_swf) {
        Engage.log('Video: Initializing video.js-display \'' + id + '\'');

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
                    if (isEmbedMode || isMobileMode) {
                        videoOptions.controls = true;
                    }

                    Engage.log('Initializing video with ID \'' + id + '\'');

                    // init video.js
                    videojs(id, videoOptions, function() {
                        var videodisplay = this;
                        // set sources
                        videodisplay.src(videoSource);
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

        // Determin reasonable default for videoAreaAspectRatio as video width and height cannot be determined sometimes.
        var numberDisplays = 1;
        if ($('.' + videoDisplayClass) !== undefined && isDefaultLayout()) {
            numberDisplays = $('.' + videoDisplayClass).length;
        }
        videoAreaAspectRatio = 1.7 * numberDisplays;

        // Calculate real aspect ratio if possible

        var videoHeight = $engageVideoId.height();
        var videoWidth = $engageVideoId.width();
        if (! isDefaultLayout()) {
            videoHeight = $('.' + videoFocusedClass).height();
            if (isPiP) {
                videoWidth = $('.' + videoFocusedClass).width();
            }
        }
        if (videoWidth !== undefined && videoHeight !== undefined && 
            videoWidth > 0 && videoHeight > 0) {
            videoAreaAspectRatio = videoWidth / videoHeight;
        }
        checkVideoDisplaySize();
    }

    function checkVideoDisplaySize() {
        var $engageVideoId = $('#' + id_engage_video);
        var $videoUnfocused = $('.' + videoUnfocusedClass);

        var videoHeight = $engageVideoId.height();
        var controlsHeight = ($('#' + id_resize_container).height() - videoHeight) + 5;
        if (controlsHeight <= 0) {
            controlsHeight = $('#' + id_engageControls).height() + 30;
        }
         
        var maxVideoAreaHeight = $(window).height() - controlsHeight;
        
        if (videoAreaAspectRatio === undefined) {
            calculateVideoAreaAspectRatio();
        }
        
        var maxVideoAreaWidth = parseInt(maxVideoAreaHeight * videoAreaAspectRatio);
        var minVideoAreaHeight = parseInt(parseInt($engageVideoId.css('min-width')) / videoAreaAspectRatio);
        
        var minWidth = parseInt($engageVideoId.css('min-width'));
        if (maxVideoAreaWidth > minWidth) {
            if (maxVideoAreaWidth > $(window).width()) {
                $engageVideoId.css('max-width', ($(window).width() - 10) + 'px');
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
        
        if (! isDefaultLayout()) {
            if (isPiP) {
                var distance = 0;
                $videoUnfocused.each(function() {
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

    function registerEventsAudioOnly(videoDisplay) {
        var audioPlayer_id = $('#' + videoDisplay);
        var audioPlayer = audioPlayer_id[0];
        var audioLoadTimeout = window.setTimeout(function() {
            Engage.trigger(plugin.events.audioCodecNotSupported.getName());
            $('.' + class_audioDisplay).hide();
            $('.' + class_audioDisplayError).show();
        }, audioLoadTimeoutCheckDelay);
        audioPlayer_id.on('canplay', function() {
            videosReady = true;
            Engage.trigger(plugin.events.ready.getName());
            window.clearTimeout(audioLoadTimeout);
        });
        audioPlayer_id.on('play', function() {
            Engage.trigger(plugin.events.play.getName(), true);
            pressedPlayOnce = true;
        });
        audioPlayer_id.on('pause', function() {
            Engage.trigger(plugin.events.pause.getName(), true);
        });
        audioPlayer_id.on('ended', function() {
            Engage.trigger(plugin.events.ended.getName(), true);
        });
        audioPlayer_id.on('timeupdate', function() {
            Engage.trigger(plugin.events.timeupdate.getName(), audioPlayer.currentTime, true);
        });
        audioPlayer_id.on(event_html5player_volumechange, function() {
            Engage.trigger(plugin.events.volumechange.getName(), audioPlayer.volume * 100);
        });
        Engage.on(plugin.events.play.getName(), function(triggeredByMaster) {
            if (!triggeredByMaster && videosReady) {
                clearAutoplay();
                audioPlayer.play();
                pressedPlayOnce = true;
            }
        });
        Engage.on(plugin.events.autoplay.getName(), function() {
            interval_autoplay = window.setInterval(function() {
                if (pressedPlayOnce) {
                    clearAutoplay();
                } else if (videosReady) {
                    audioPlayer.play();
                    clearAutoplay();
                }
            }, interval_autoplay_ms);
        });
        Engage.on(plugin.events.initialSeek.getName(), function(e) {
            parsedSeconds = Utils.parseSeconds(e);
            interval_initialSeek = window.setInterval(function() {
                if (pressedPlayOnce) {
                    clearInitialSeek();
                } else if (videosReady) {
                    audioPlayer.play();
                    window.setTimeout(function() {
                        Engage.trigger(plugin.events.seek.getName(), parsedSeconds);
                    }, timeout_initialSeek_ms);
                    clearInitialSeek();
                }
            }, interval_initialSeek_ms);
        });
        Engage.on(plugin.events.pause.getName(), function(triggeredByMaster) {
            if (!triggeredByMaster && pressedPlayOnce) {
                clearAutoplay();
                audioPlayer.pause();
            }
        });
        Engage.on(plugin.events.playPause.getName(), function() {
            if (audioPlayer.paused()) {
                Engage.trigger(plugin.events.play.getName());
            } else {
                Engage.trigger(plugin.events.pause.getName());
            }
        });
        Engage.on(plugin.events.seekLeft.getName(), function() {
            if (pressedPlayOnce) {
                var currTime = audioPlayer.currentTime();
                if ((currTime - seekSeconds) >= 0) {
                    Engage.trigger(plugin.events.seek.getName(), currTime - seekSeconds);
                } else {
                    Engage.trigger(plugin.events.seek.getName(), 0);
                }
            }
        });
        Engage.on(plugin.events.seekRight.getName(), function() {
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
        Engage.on(plugin.events.playbackRateIncrease.getName(), function() {
            changePlaybackRate(0.125, videodisplayMaster);
        });
        Engage.on(plugin.events.playbackRateDecrease.getName(), function() {
            changePlaybackRate(-0.125, videodisplayMaster);
        });
        Engage.on(plugin.events.volumeSet.getName(), function(volume) {
            if ((volume >= 0) && (volume <= 1)) {
                Engage.log('Video: Volume changed to ' + volume);
                audioPlayer.volume = volume;
            }
        });
        Engage.on(plugin.events.volumeGet.getName(), function(callback) {
            callback(audioPlayer.volume);
        });
        Engage.on(plugin.events.timeupdate.getName(), function(time) {
            currentTime = time;
        });
        Engage.on(plugin.events.seek.getName(), function(time) {
            Engage.log('Video: Seek to ' + time);
            if (videosReady && pressedPlayOnce) {
                var duration = parseInt(Engage.model.get('videoDataModel').get('duration')) / 1000;
                if (duration && (time < duration)) {
                    audioPlayer.currentTime = time;
                } else {
                    Engage.trigger(plugin.events.customError.getName(), translate('givenTime', 'The given time') + ' (' + Utils.formatSeconds(time) + ') ' + translate('hasToBeSmallerThanDuration', 'has to be smaller than the duration') + ' (' + Utils.formatSeconds(duration) + ').');
                    Engage.trigger(plugin.events.timeupdate.getName(), audioPlayer.currentTime);
                }
            } else {
                if (!videosReady) {
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_waitToSetTime', 'Please wait until the video has been loaded to set a time.'));
                } else { // pressedPlayOnce
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_startPlayingToSetTime', 'Please start playing the video once to set a time.'));
                }
                Engage.trigger(plugin.events.timeupdate.getName(), 0);
            }
        });
        Engage.on(plugin.events.sliderStop.getName(), function(time) {
            Engage.log('Video: Slider stopped at ' + time);
            if (videosReady && pressedPlayOnce) {
                var duration = parseInt(Engage.model.get('videoDataModel').get('duration'));
                audioPlayer.currentTime = (time / 1000) * (duration / 1000);
            } else {
                if (!videosReady) {
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_waitToSeek', 'Please wait until the video has been loaded to seek.'));
                } else { // pressedPlayOnce
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_startPlayingToSeek', 'Please start playing the video once to seek.'));
                }
                Engage.trigger(plugin.events.timeupdate.getName(), 0);
            }
        });
        Engage.on(plugin.events.ended.getName(), function(time) {
            if (videosReady) {
                Engage.log('Video: Ended at ' + time);
                audioPlayer.pause();
                Engage.trigger(plugin.events.pause.getName());
                audioPlayer.currentTime = audioPlayer.duration;
            }
        });
    }

    function registerEventsVideo(videoDisplay, numberOfVideodisplays) {
        $(document).on('webkitfullscreenchange mozfullscreenchange fullscreenchange MSFullscreenChange', function() {
            fullscreen = !fullscreen;
            if (fullscreen) {
                Engage.trigger(plugin.events.fullscreenEnable.getName());
            } else {
                Engage.trigger(plugin.events.fullscreenCancel.getName());
            }
        });

        var videodisplayMaster = videojs(videoDisplay);
        var $videoDisplay = $('#' + videoDisplay);

        if (numberOfVideodisplays == 1) {
            videodisplayMaster.on('play', function() {
                $('.' + class_vjsposter).detach();
                Engage.trigger(plugin.events.play.getName(), true);
                pressedPlayOnce = true;
            });
            videodisplayMaster.on('pause', function() {
                Engage.trigger(plugin.events.pause.getName(), true);
            });
            videodisplayMaster.on('ended', function() {
                Engage.trigger(plugin.events.ended.getName(), true);
            });
            videodisplayMaster.on('timeupdate', function() {
                Engage.trigger(plugin.events.timeupdate.getName(), videodisplayMaster.currentTime(), true);
            });
        }
        $('#' + id_btn_fullscreenCancel).click(function(e) {
            e.preventDefault();
            Engage.trigger(plugin.events.fullscreenCancel.getName());
        });
        Engage.on(plugin.events.fullscreenEnable.getName(), function() {
            if (numberOfVideodisplays == 1) {
                videodisplayMaster.requestFullscreen();
                $('#' + videoDisplay).css('padding-top', '0px');
            } else if (!fullscreen) {
                var viewer = document.getElementById(id_engage_video_fullsceen_wrapper);
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
                    $(window).scroll(function() {
                        $(this).scrollTop(0);
                    });
                    $('#' + id_engage_video).css('z-index', 995).css('position', 'relative');
                    $('#' + id_page_cover).css('opacity', 0.9).fadeIn(300, function() {});
                    fullscreen = true;
                }
            }
            $videoDisplay.removeClass('vjs-controls-disabled').addClass('vjs-controls-enabled');
            $('.' + id_videoDisplayClass).css('max-width', $(window).height() * videoAreaAspectRatio);
        });
        Engage.on(plugin.events.fullscreenCancel.getName(), function() {
            if (numberOfVideodisplays == 1) {
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
                    $('#' + id_page_cover).css('opacity', 0.9).fadeOut(300, function() {
                        $('#' + id_engage_video).css('z-index', 0).css('position', '');
                    });
                    fullscreen = false;
                }
            }
            $videoDisplay.removeClass('vjs-controls-enabled').addClass('vjs-controls-disabled');
            $('.' + id_videoDisplayClass).css('max-width', '');
            checkVideoDisplaySize();
        });
        Engage.on(plugin.events.playbackRateChanged.getName(), function(rate) {
            if (pressedPlayOnce) {
                Engage.log('Video: Playback rate changed to rate ' + rate);
                videodisplayMaster.playbackRate(rate);
            }
        });
        Engage.on(plugin.events.play.getName(), function(triggeredByMaster) {
            if (!triggeredByMaster && videosReady) {
                $('.' + class_vjsposter).detach();
                clearAutoplay();
                videodisplayMaster.play();
                pressedPlayOnce = true;
            }
        });
        Engage.on(plugin.events.autoplay.getName(), function() {
            interval_autoplay = window.setInterval(function() {
                if (pressedPlayOnce) {
                    clearAutoplay();
                } else if (videosReady) {
                    videodisplayMaster.play();
                    clearAutoplay();
                }
            }, interval_autoplay_ms);
        });
        Engage.on(plugin.events.initialSeek.getName(), function(e) {
            parsedSeconds = Utils.parseSeconds(e);
            interval_initialSeek = window.setInterval(function() {
                if (pressedPlayOnce) {
                    clearInitialSeek();
                } else if (videosReady) {
                    videodisplayMaster.play();
                    window.setTimeout(function() {
                        Engage.trigger(plugin.events.seek.getName(), parsedSeconds);
                    }, timeout_initialSeek_ms);
                    clearInitialSeek();
                }
            }, interval_initialSeek_ms);
        });
        Engage.on(plugin.events.pause.getName(), function(triggeredByMaster) {
            if (!triggeredByMaster && pressedPlayOnce) {
                clearAutoplay();
                videodisplayMaster.pause();
            }
        });
        Engage.on(plugin.events.playPause.getName(), function() {
            if (videodisplayMaster.paused()) {
                Engage.trigger(plugin.events.play.getName());
            } else {
                Engage.trigger(plugin.events.pause.getName());
            }
        });
        Engage.on(plugin.events.seekLeft.getName(), function() {
            if (pressedPlayOnce) {
                var currTime = videodisplayMaster.currentTime();
                if ((currTime - seekSeconds) >= 0) {
                    Engage.trigger(plugin.events.seek.getName(), currTime - seekSeconds);
                } else {
                    Engage.trigger(plugin.events.seek.getName(), 0);
                }
            }
        });
        Engage.on(plugin.events.seekRight.getName(), function() {
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
        Engage.on(plugin.events.playbackRateIncrease.getName(), function() {
            changePlaybackRate(0.125, videodisplayMaster);
        });
        Engage.on(plugin.events.playbackRateDecrease.getName(), function() {
            changePlaybackRate(-0.125, videodisplayMaster);
        });
        Engage.on(plugin.events.volumeSet.getName(), function(volume) {
            if ((volume >= 0) && (volume <= 1)) {
                Engage.log('Video: Volume changed to ' + volume);
                videodisplayMaster.volume(volume);
            }
        });
        Engage.on(plugin.events.volumeGet.getName(), function(callback) {
            if (callback) {
                callback(videodisplayMaster.volume());
            }
        });
        Engage.on(plugin.events.timeupdate.getName(), function(time) {
            currentTime = time;
        });
        Engage.on(plugin.events.seek.getName(), function(time) {
            Engage.log('Video: Seek to ' + time);
            if (videosReady && pressedPlayOnce) {
                var duration = parseInt(Engage.model.get('videoDataModel').get('duration')) / 1000;
                if (duration && (time < duration)) {
                    videodisplayMaster.currentTime(time);
                } else {
                    Engage.trigger(plugin.events.customError.getName(), translate('givenTime', 'The given time') + ' (' + Utils.formatSeconds(time) + ') ' + translate('hasToBeSmallerThanDuration', 'has to be smaller than the duration') + ' (' + Utils.formatSeconds(duration) + ').');
                    Engage.trigger(plugin.events.timeupdate.getName(), videodisplayMaster.currentTime());
                }
            } else {
                if (!videosReady) {
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_waitToSetTime', 'Please wait until the video has been loaded to set a time.'));
                } else { // pressedPlayOnce
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_startPlayingToSetTime', 'Please start playing the video once to set a time.'));
                }
                Engage.trigger(plugin.events.timeupdate.getName(), 0);
            }
        });
        Engage.on(plugin.events.sliderStop.getName(), function(time) {
            if (videosReady && pressedPlayOnce) {
                var duration = parseInt(Engage.model.get('videoDataModel').get('duration'));
                var normTime = (time / 1000) * (duration / 1000);
                videodisplayMaster.currentTime(normTime);
            } else {
                if (!videosReady) {
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_waitToSeek', 'Please wait until the video has been loaded to seek.'));
                } else { // pressedPlayOnce
                    Engage.trigger(plugin.events.customNotification.getName(), translate('msg_startPlayingToSeek', 'Please start playing the video once to seek.'));
                }
                Engage.trigger(plugin.events.timeupdate.getName(), 0);
            }
        });
        Engage.on(plugin.events.ended.getName(), function() {
            if (videosReady) {
                Engage.log('Video: Video ended and ready');
                videodisplayMaster.pause();
                Engage.trigger(plugin.events.pause.getName());
                videodisplayMaster.currentTime(0);
                // videodisplayMaster.currentTime(videodisplayMaster.duration());
                // Engage.trigger(plugin.events.seek.getName(), 0);
            }
        });
        videodisplayMaster.on(event_html5player_volumechange, function() {
            Engage.trigger(plugin.events.volumechange.getName(), videodisplayMaster.volume());
        });
        videodisplayMaster.on(event_html5player_fullscreenchange, function() {
            Engage.trigger(plugin.events.fullscreenChange.getName());
        });

        var $videoDisplayClass = $('.' + id_videoDisplayClass);

        var numberDisplays = $videoDisplayClass.length;
        if (numberDisplays > 1) {
            $videoDisplayClass.on('click', function () {
                Engage.trigger(plugin.events.focusVideo.getName(), Utils.getFlavorForVideoDisplay(this));
            });

            Engage.on(plugin.events.focusVideo.getName(), function(display) {
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
                                Utils.getFlavorForVideoDisplay($videoDisplayClass.first()));
                            return;
                        } else {
                            Engage.trigger(plugin.events.focusVideo.getName(),
                                Utils.getFlavorForVideoDisplay($videoDisplayClass.last()));
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
                                    (display === 'focus.next' &&  i === vidDisp.length -1)) {
                                    Engage.log('Resetting videodisplay layout');
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
                        Engage.log('Resetting videodisplay layout');
                        Engage.trigger(plugin.events.resetLayout.getName());
                        return;
                    }
                }
                $videoDisplayClass.css('width', '');
                $videoDisplayClass.css('left', '');
                $videoDisplayClass.css('top', '');
                $videoDisplayClass.css('margin-left', '');
                $('#engage_video').css('height', '');
                $videoDisplayClass.removeClass(videoDefaultLayoutClass).addClass(videoUnfocusedClass);
                $('.' + videoUnfocusedClass).removeClass(videoFocusedClass);
                $(videoDiv).addClass(videoFocusedClass).removeClass(videoUnfocusedClass);


                if (isPiP) {
                    var distance = 0;
                    $('.' + videoUnfocusedClass).each(function() {
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
                    $('.' + videoUnfocusedClass).css('margin-left', marginLeft + '%');
                } else {
                    var height = $('.' + videoFocusedClass).height();
                    $('#engage_video').height(height + 10);
                }
                delayedCalculateVideoAreaAspectRatio();
            });

            Engage.on(plugin.events.resetLayout.getName(), function() {
                Engage.log('Video: received resetting layout');
                $('#engage_video').css('height', '');
                $videoDisplayClass.css('margin-left', '');
                $videoDisplayClass.css('width', '');
                $videoDisplayClass.css('left', '');
                $videoDisplayClass.css('top', '');
                $videoDisplayClass.css('margin-left', '');
                $videoDisplayClass.removeClass(videoFocusedClass).removeClass(videoUnfocusedClass).addClass(videoDefaultLayoutClass);
                var numberDisplays = $videoDisplayClass.length;
                $videoDisplayClass.css('width', (((1 / numberDisplays) * 100) - 0.5) + '%');
                delayedCalculateVideoAreaAspectRatio();
            });

            Engage.on(plugin.events.movePiP.getName(), function(pos) {
                var numberDisplays = $videoDisplayClass.length;
                if (numberDisplays <= 1) return;
                if (pos !== undefined) {
                    pipPos = pos;
                }
                if (! isPiP) {
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

            Engage.on(plugin.events.togglePiP.getName(), function(pip) {
                var numberDisplays = $videoDisplayClass.length;
                if (numberDisplays <= 1) return;

                Engage.log('Video: setting PiP to ' + pip);
                if ((pip && isPiP) || (! pip && ! isPiP)) {
                    return;
                }
                if (! pip) {
                    videoUnfocusedClass = unfocusedClass;
                    videoFocusedClass = focusedClass;
                    isPiP = false;
                    if (! isDefaultLayout()) {
                        $videoDisplayClass.css('margin-left', '');
                        $videoDisplayClass.css('width', '');
                        $videoDisplayClass.css('left', '');
                        $videoDisplayClass.css('top', '');
                        $('.' + unfocusedPiPClass).addClass(videoUnfocusedClass).removeClass(unfocusedPiPClass);
                        $('.' + focusedPiPClass).addClass(videoFocusedClass).removeClass(focusedPiPClass);
                        var height = $('.' + videoFocusedClass).height();
                        $('#engage_video').height(height + 10);
                    }
                } else {
                    videoUnfocusedClass = unfocusedPiPClass;
                    videoFocusedClass = focusedPiPClass;
                    isPiP = true;
                    if (! isDefaultLayout()) {
                        $('.' + unfocusedClass).addClass(videoUnfocusedClass).removeClass(unfocusedClass);
                        $('.' + focusedClass).addClass(videoFocusedClass).removeClass(focusedClass);
                        $('#engage_video').css('height', '');
                        var distance = 0;
                        $('.' + videoUnfocusedClass).each(function() {
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
                        $('.' + videoUnfocusedClass).css('margin-left', marginLeft + '%');

                    }
                }
                delayedCalculateVideoAreaAspectRatio();
            });
        }
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
        var found = undefined;
        $('.' + videoDisplayClass).each(function (){
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
        var allowedTags = Engage.model.get('meInfo').get('allowedtags');
        var allowedFormats = Engage.model.get('meInfo').get('allowedformats');

        videoSources.audio = [];

        for (var j = 0; j < flavorsArray.length; ++j) {
            videoSources[Utils.extractFlavorMainType(flavorsArray[j])] = [];
        }

        mediaInfo.tracks = filterTracksByFormat(filterTracksByTag(mediaInfo.tracks, allowedTags), allowedFormats);
        if (mediaInfo.tracks) {
            $(mediaInfo.tracks).each(function(i, track) {
                if (track.mimetype && track.type && acceptFormat(track)) {
                    if (track.mimetype.match(/video/g) || track.mimetype.match(/application/g) || track.mimetype.match(/rtmp/g)) {
                        hasVideo = true;
                        if (track.duration > duration) {
                            duration = track.duration;
                        }
                        var resolution = (track.video && track.video.resolution) ? track.video.resolution : '';
                        // filter for different video sources
                        Engage.log('Video: Adding video source: ' + track.url + ' (' + track.mimetype + ')');
                        if (track.mimetype == 'application/dash+xml') {
                            loadDash = true;
                        } else if (track.mimetype == 'application/x-mpegURL') {
                            loadHls = true;
                        }
                        videoSources[Utils.extractFlavorMainType(track.type)].push({
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
            $(mediaInfo.attachments).each(function(i, attachment) {
                if (attachment.mimetype &&
                    attachment.type &&
                    attachment.mimetype.match(/image/g) &&
                    attachment.type.match(/player/g)) {
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
            Engage.log('Extracted flavors: ' + flavors, mimetypes);
            Engage.log('Extracted mimetypes: ' + mimetypes);

            var flavorsArray = flavors.split(',');

            // look for video sources
            var videoSourcesAndDuration = extractVideoSourcesAndDuration(mediaInfo, flavorsArray);
            videoSources = videoSourcesAndDuration.videoSources;
            duration = videoSourcesAndDuration.duration;

            setVideoSourcePosters(mediaInfo, videoSources);

            videoDisplays = extractVideoDisplays(videoSources);
            Engage.log('Extracted video displays: ' + videoDisplays.join(', '));

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
        if (a === undefined || b === undefined || a [1][0] === undefined ||
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

    function initPlugin() {
        Engage.log('Video: Init Plugin');

        // only init if plugin template was inserted into the DOM
        if (plugin.inserted) {
            Engage.log('Video: Video Plugin inserted');
            // set path to swf player
            var videojs_swf = plugin.pluginPath + videojs_swf_path;
            Engage.log('Video: SWF path: ' + videojs_swf_path);
            Engage.model.on(videoDataModelChange, function() {
                Engage.log('The video data model changed, refreshing the view.');
                videoDataView = new VideoDataView(this.get('videoDataModel'), plugin.template, videojs_swf);
            });
            Engage.on(plugin.events.mediaPackageModelError.getName(), function() {
                mediapackageError = true;
            });
            Engage.model.get('mediaPackage').on('change', function() {
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
    Engage.model.on(mediapackageChange, function() {
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log('Video: Plugin load done');
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load utils class
    require([relative_plugin_path + 'utils'], function(utils) {
        Engage.log('Video: Utils class loaded');
        Utils = new utils();
        initTranslate(Utils.detectLanguage(), function() {
            Engage.log('Video: Successfully translated.');
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        }, function() {
            Engage.log('Video: Error translating...');
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });
    });

    // load videoData model
    require([relative_plugin_path + 'models/videoData'], function(model) {
        Engage.log('Video: VideoData model loaded');
        VideoDataModel = model;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load video.js lib
    require([relative_plugin_path + videoPath], function(videojs) {
        Engage.log('Video: Lib video loaded');
        window.videojs = videojs;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load synchronize.js lib
    require([relative_plugin_path + synchronizePath], function(synchronizejs) {
        Engage.log('Video: Lib synchronize loaded');
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // listen on a change/set of the infoMe model
    Engage.model.on(infoMeChange, function() {
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    return plugin;
});
