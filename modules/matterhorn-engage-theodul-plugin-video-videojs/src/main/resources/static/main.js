/**
 * Copyright 2009-2011 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/*jslint browser: true, nomen: true*/
/*global define*/
define(["require", "jquery", "underscore", "backbone", "basil", "engage/core"], function(require, $, _, Backbone, Basil, Engage) {
    "use strict";

    var insertIntoDOM = true;
    var PLUGIN_NAME = "Engage VideoJS Videodisplay";
    var PLUGIN_TYPE = "engage_video";
    var PLUGIN_VERSION = "1.0";
    var PLUGIN_TEMPLATE_DESKTOP = "templates/desktop.html";
    var PLUGIN_TEMPLATE_MOBILE = "templates/mobile.html";
    var PLUGIN_TEMPLATE_EMBED = "templates/embed.html";
    var PLUGIN_STYLES_DESKTOP = [
        "styles/desktop.css",
        "lib/videojs/video-js.css"
    ];
    var PLUGIN_STYLES_EMBED = [
        "styles/embed.css",
        "lib/videojs/video-js.css"
    ];
    var PLUGIN_STYLES_MOBILE = [
        "styles/mobile.css",
        "lib/videojs/video-js.css"
    ];

    var plugin;
    var events = {
        play: new Engage.Event("Video:play", "plays the video", "both"),
        pause: new Engage.Event("Video:pause", "pauses the video", "both"),
        ready: new Engage.Event("Video:ready", "all videos loaded successfully", "trigger"),
        ended: new Engage.Event("Video:ended", "end of the video", "trigger"),
        playerLoaded: new Engage.Event("Video:playerLoaded", "player loaded successfully", "trigger"),
        synchronizing: new Engage.Event("Video:synchronizing", "synchronizing videos with the master video", "trigger"),
        buffering: new Engage.Event("Video:buffering", "video is buffering", "trigger"),
        bufferedAndAutoplaying: new Engage.Event("Video:bufferedAndAutoplaying", "buffering successful, was playing, autoplaying now", "trigger"),
        customNotification: new Engage.Event("Notification:customNotification", "a custom message", "trigger"),
        customError: new Engage.Event("Notification:customError", "an error occured", "trigger"),
        bufferedButNotAutoplaying: new Engage.Event("Video:bufferedButNotAutoplaying", "buffering successful, was not playing, not autoplaying now", "trigger"),
        timeupdate: new Engage.Event("Video:timeupdate", "timeupdate happened", "trigger"),
        volumechange: new Engage.Event("Video:volumechange", "volume change happened", "trigger"),
        fullscreenChange: new Engage.Event("Video:fullscreenChange", "fullscreen change happened", "trigger"),
        usingFlash: new Engage.Event("Video:usingFlash", "flash is being used", "trigger"),
        numberOfVideodisplaysSet: new Engage.Event("Video:numberOfVideodisplaysSet", "the number of videodisplays has been set", "trigger"),
        aspectRatioSet: new Engage.Event("Video:aspectRatioSet", "the aspect ratio has been calculated", "trigger"),
        isAudioOnly: new Engage.Event("Video:isAudioOnly", "whether it's audio only or not", "trigger"),
        audioCodecNotSupported: new Engage.Event("Video:audioCodecNotSupported", "when the audio codec seems not to be supported by the browser", "trigger"),
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "", "handler"),
        fullscreenEnable: new Engage.Event("Video:fullscreenEnable", "go to fullscreen", "handler"),
        fullscreenCancel: new Engage.Event("Video:fullscreenCancel", "cancel fullscreen", "handler"),
        volumeSet: new Engage.Event("Video:volumeSet", "set the volume", "handler"),
        volumeGet: new Engage.Event("Video:volumeGet", "get the volume", "handler"),
        sliderStop: new Engage.Event("Slider:stop", "slider stopped", "handler"),
        seek: new Engage.Event("Video:seek", "seek video to a given position in seconds", "handler"),
        playbackRateChanged: new Engage.Event("Video:playbackRateChanged", "The video playback rate changed", "handler"),
        mediaPackageModelError: new Engage.Event("MhConnection:mediaPackageModelError", "", "handler")
    };

    var isDesktopMode = false;
    var isEmbedMode = false;
    var isMobileMode = false;

    // desktop, embed and mobile logic
    switch (Engage.model.get("mode")) {
        case "mobile":
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
        case "embed":
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
        case "desktop":
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
    var videoPath = "lib/videojs/video";
    var synchronizePath = "lib/synchronize";
    var mediaSourcesPath = "lib/videojs/videojs-media-sources";
    var hlsPath = "lib/videojs/videojs.hls.min";
    var dashPath = "lib/videojs/dash.min";
    var dashPluginPath = "lib/videojs/videojs-tech-dashjs"
    var videojs_swf_path = "lib/videojs/video-js.swf";
    var videoDisplaySizeFactor = 1.1;
    var videoDisplaySizeTimesCheck = 100; // the smaller the factor, the higher the times check!
    var checkVideoDisplaySizeTimeout = 1500;
    var audioLoadTimeoutCheckDelay = 5000;

    /* don't change these variables */
    var Utils;
    var VideoDataModel;
    var isAudioOnly = false;
    var isUsingFlash = false;
    var aspectRatio = "";
    var initCount = 6;
    var mediapackageError = false;
    var videoDisplayNamePrefix = "videojs_videodisplay_";
    var id_engage_video = "engage_video";
    var id_videojs_wrapper = "videojs_wrapper";
    var id_videoDisplayClass = "videoDisplay";
    var id_engageContent = "engage_content";
    var id_videojs_wrapperClass = "videojs_wrapper";
    var id_engage_video = "engage_video";
    var id_page_cover = "page-cover";
    var id_btn_fullscreenCancel = "btn_fullscreenCancel";
    var id_generated_videojs_flash_component = "videojs_videodisplay_0_flash_api";
    var id_btn_openInPlayer = "btn_openInPlayer";
    var id_btn_switchPlayer = "btn_switchPlayer";
    var id_btn_video1 = "btn-video1";
    var id_btn_video2 = "btn-video2";
    var id_switchPlayer_value = "switchPlayer-value";
    var id_audioDisplay = "audioDisplay";
    var class_vjs_switchPlayer = "vjs-switchPlayer";
    var class_btn_video = "btn-video";
    var class_vjs_menu_button = "vjs-menu-button";
    var class_vjs_switchPlayer_value = "vjs-switchPlayer-value";
    var class_vjs_menu = "vjs-menu";
    var class_vjs_menu_content = "vjs-menu-content";
    var class_vjs_menu_item = "vjs-menu-item";
    var class_vjsposter = "vjs-poster";
    var class_vjs_openInPlayer = "vjs-openInPlayer";
    var class_vjs_control = "vjs-control";
    var class_vjs_control_text = "vjs-control-text";
    var class_vjs_mute_control = "vjs-mute-control";
    var class_audio_wrapper = "audio_wrapper";
    var class_audioDisplay = "audioDisplay";
    var class_audioDisplayError = "audioDisplayError";
    var videosReady = false;
    var pressedPlayOnce = false;
    var mediapackageChange = "change:mediaPackage";
    var videoDataModelChange = "change:videoDataModel";
    var event_html5player_volumechange = "volumechange";
    var event_html5player_fullscreenchange = "fullscreenchange";
    var event_sjs_allPlayersReady = "sjs:allPlayersReady";
    var event_sjs_playerLoaded = "sjs:playerLoaded";
    var event_sjs_masterPlay = "sjs:masterPlay";
    var event_sjs_masterPause = "sjs:masterPause";
    var event_sjs_masterEnded = "sjs:masterEnded";
    var event_sjs_masterTimeupdate = "sjs:masterTimeupdate";
    var event_sjs_synchronizing = "sjs:synchronizing";
    var event_sjs_buffering = "sjs:buffering";
    var event_sjs_bufferedAndAutoplaying = "sjs:bufferedAndAutoplaying";
    var event_sjs_bufferedButNotAutoplaying = "sjs:bufferedButNotAutoplaying";
    var event_sjs_isUsingFlash = "sjs:isUsingFlash";
    var event_sjs_debug = "sjs:debug";
    var currentlySelectedVideodisplay = 0;
    var globalVideoSource = new Array();
    var videoResultions = new Array();
    var loadDash = false;
    var loadHls = false;
    var flavors = "";
    var mimetypes = "";
    var translations = new Array();
    var videoDataView = undefined;

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath("EngagePluginVideoVideoJS").replace(/(\.\.\/)/g, "");
        var jsonstr = window.location.origin + "/engage/theodul/" + path; // this solution is really bad, fix it...

        if (language == "de") {
            Engage.log("Videodisplay: Chosing german translations");
            jsonstr += "language/de.json";
        } else { // No other languages supported, yet
            Engage.log("Videodisplay: Chosing english translations");
            jsonstr += "language/en.json";
        }
        $.ajax({
            url: jsonstr,
            dataType: "json",
            async: false,
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
            error: function(jqXHR, textStatus, errorThrown) {
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
        namespace: "mhStorage"
    };
    Basil = new window.Basil(basilOptions);

    function acceptFormat(track) {
        // TODO: throws a SyntaxError: JSON.parse: unexpected character at line 1 column 1 of the JSON data [basil.js:68]
        /*
        var preferredFormat = Basil.get("preferredFormat");
        if ((Utils.preferredFormat(preferredFormat) == null) || (mimetypes.indexOf(Utils.preferredFormat(preferredFormat)) == -1)) {
            return true; // preferred format is not available, accept all
        }
        return track.mimetype == Utils.preferredFormat(preferredFormat);
	*/
        return true;
    }

    function renderDesktop(videoDataView, videoSources, videoDisplays, aspectRatio) {
        var i = 0;
        for (var v in videoSources) {
            if ((videoSources[v].length > 0) && (videoDisplays.length > i)) {
                initVideojsVideo(videoDisplays[i], videoSources[v], videoDataView.videojs_swf);
                ++i;
            }
        }

        if ((aspectRatio != null) && (videoDisplays.length > 0)) {
            Engage.log("Video: Aspect ratio: " + aspectRatio[1] + "x" + aspectRatio[2] + " == " + ((aspectRatio[2] / aspectRatio[1]) * 100));
            Engage.trigger(plugin.events.aspectRatioSet.getName(), [aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100]);
            $("." + id_videoDisplayClass).css("width", (((1 / videoDisplays.length) * 100) - 0.5) + "%");
            $("." + id_videoDisplayClass).each(function(index) {
                if ((index % 2) == 1) {
                    $(videoDataView).css("float", "right");
                }
            });
            for (var i = 0; i < videoDisplays.length; ++i) {
                $("#" + videoDisplays[i]).css("padding-top", (aspectRatio[2] / aspectRatio[1] * 100) + "%").addClass("auto-height");
            }
        } else {
            Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
        }

        // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
        $("." + class_vjsposter).show();

        Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

        if (videoDisplays.length > 0) {
            var nr = 0;
            for (var v in videoSources) {
                if (videoSources[v].length > 0) {
                    ++nr;
                }
            }

            // set first videoDisplay as master
            registerEvents(isAudioOnly ? id_audioDisplay : videoDisplays[0], videoDisplays.length);

            if (nr >= 2) {
                // throw some important synchronize.js-events for other plugins
                $(document).on(event_sjs_allPlayersReady, function(event) {
                    videosReady = true;
                    Engage.trigger(plugin.events.ready.getName());
                });
                $(document).on(event_sjs_playerLoaded, function(event) {
                    Engage.trigger(plugin.events.playerLoaded.getName());
                });
                $(document).on(event_sjs_masterPlay, function(event) {
                    Engage.trigger(plugin.events.play.getName(), true);
                    pressedPlayOnce = true;
                });
                $(document).on(event_sjs_masterPause, function(event) {
                    Engage.trigger(plugin.events.pause.getName(), true);
                });
                $(document).on(event_sjs_masterEnded, function(event) {
                    Engage.trigger(plugin.events.ended.getName(), true);
                });
                $(document).on(event_sjs_masterTimeupdate, function(event, time) {
                    Engage.trigger(plugin.events.timeupdate.getName(), time, true);
                });
                $(document).on(event_sjs_synchronizing, function(event) {
                    Engage.trigger(plugin.events.synchronizing.getName());
                });
                $(document).on(event_sjs_buffering, function(event) {
                    Engage.trigger(plugin.events.buffering.getName());
                });
                $(document).on(event_sjs_bufferedAndAutoplaying, function(event) {
                    Engage.trigger(plugin.events.bufferedAndAutoplaying.getName());
                });
                $(document).on(event_sjs_bufferedButNotAutoplaying, function(event) {
                    Engage.trigger(plugin.events.bufferedButNotAutoplaying.getName());
                });

                var i = 0;
                for (var vd in videoDisplays) {
                    if (i > 0) {
                        // sync every other videodisplay with the master
                        $.synchronizeVideos(0, videoDisplays[0], videoDisplays[vd]);
                        Engage.log("Video: Videodisplay " + vd + " is now being synchronized with the master videodisplay");
                    }
                    ++i;
                }
                if (isUsingFlash) {
                    $(document).trigger(event_sjs_isUsingFlash, []);
                    $(document).trigger(event_sjs_debug, Engage.model.get("isDebug"));
                }
            } else {
                videosReady = true;
                if (!isAudioOnly) {
                    Engage.trigger(plugin.events.ready.getName());
                }
            }

            if (videoDataView.model.get("type") != "audio") {
                $(window).resize(function() {
                    checkVideoDisplaySize();
                });
            }
        }
    }

    function renderEmbed(videoDataView, videoSources, videoDisplays, aspectRatio) {
        var nrOfVideoSources = 0;
        var init = false;
        for (var v in videoSources) {
            if (videoSources[v].length > 0) {
                if (!init) { // just init the first video
                    init = true;
                    initVideojsVideo(videoDisplays[i], videoSources[v], videoDataView.videojs_swf);
                }
                globalVideoSource.push({
                    id: videoDisplays[i],
                    src: videoSources[v]
                });
            }
        }

        if ((videoDisplays.length > 1) && (globalVideoSource.length > 1)) {
            $("." + class_vjs_mute_control).after("<div id=\"" + id_btn_switchPlayer + "\" class=\"" + class_vjs_switchPlayer + " " + class_vjs_control + " " + class_vjs_menu_button + "\" role=\"button\" aria-live=\"polite\" tabindex=\"0\"></div>");
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
            $("#" + id_btn_video1).click(function(e) {
                $("#" + id_switchPlayer_value).html(translate("video_short", "Vid.") + " 1");
                if (!currentlySelectedVideodisplay == 0) {
                    currentlySelectedVideodisplay = 0;
                    videojs(globalVideoSource[0].id).src(globalVideoSource[0].src);
                }
            });
            $("#" + id_btn_video2).click(function(e) {
                $("#" + id_switchPlayer_value).html(translate("video_short", "Vid.") + " 2");
                if (!currentlySelectedVideodisplay == 1) {
                    currentlySelectedVideodisplay = 1;
                    videojs(globalVideoSource[1].id).src(globalVideoSource[1].src);
                }
            });
        }
        $("." + class_vjs_mute_control).after("<div id=\"" + id_btn_openInPlayer + "\" class=\"" + class_vjs_openInPlayer + " " + class_vjs_control + "\" role=\"button\" aria-live=\"polite\" tabindex=\"0\"><div><span class=\"" + class_vjs_control_text + "\">" + translate("openInPlayer", "Open in player") + "</span></div></div>");
        $("." + class_audio_wrapper).append("<a id=\"" + id_btn_openInPlayer + "\" href=\"#\">" + translate("openInPlayer", "Open in player") + "</a>");

        $("#" + id_btn_openInPlayer).click(function(e) {
            e.preventDefault();
            var str = window.location.href;
            if (str.indexOf("mode=embed") == -1) {
                str += "&mode=embed";
            } else {
                str = Utils.replaceAll(str, "mode=embed", "mode=desktop");
            }
            Engage.trigger(plugin.events.pause.getName(), false);
            window.open(str, "_blank");
        });

        if ((aspectRatio != null) && (videoDisplays.length > 0)) {
            aspectRatio[1] = parseInt(aspectRatio[1]);
            aspectRatio[2] = parseInt(aspectRatio[2]);
            Engage.log("Video: Aspect ratio: " + aspectRatio[1] + "x" + aspectRatio[2] + " == " + ((aspectRatio[2] / aspectRatio[1]) * 100));
            Engage.trigger(plugin.events.aspectRatioSet.getName(), aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100);
            $("." + id_videoDisplayClass).css("width", "100%");
            for (var i = 0; i < videoDisplays.length; ++i) {
                $("#" + videoDisplays[i]).css("padding-top", (aspectRatio[2] / aspectRatio[1] * 100) + "%").addClass("auto-height");
            }
        } else {
            Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
        }

        // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
        $("." + class_vjsposter).show();
        Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

        if (videoDisplays.length > 0) {
            // set first videoDisplay as master
            registerEvents(isAudioOnly ? id_audioDisplay : videoDisplays[0], 1);

            videosReady = true;
            Engage.trigger(plugin.events.ready.getName());

            if (videoDataView.model.get("type") != "audio") {
                $(window).resize(function() {
                    checkVideoDisplaySize();
                });
            }
        }
    }

    function renderMobile(videoDataView, videoSources, videoDisplays, aspectRatio) {
        Engage.log("Video: Render mobile mode");
        checkVideoDisplaySize();
        initMobileEvents();

        var i = 0;
        for (var v in videoSources) {
            if (videoSources[v].length > 0) {
                Engage.log("Init Video Display: " + v);
                initVideojsVideo(videoDisplays[i], videoSources[v], videoDataView.videojs_swf);
                ++i;
            }
        }

        Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

        if ((aspectRatio != null) && (videoDisplays.length > 0)) {
            Engage.log("Video: Aspect ratio: " + aspectRatio[1] + "x" + aspectRatio[2] + " == " + ((aspectRatio[2] / aspectRatio[1]) * 100));
            Engage.trigger(plugin.events.aspectRatioSet.getName(), aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100);
        } else {
            Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
        }

        // show poster
        $("." + class_vjsposter).show();

        if (videoDisplays.length > 0) {
            var nr = 0;
            for (var v in videoSources) {
                if (videoSources[v].length > 0) {
                    ++nr;
                }
            }

            // first as masterdisplay
            registerEvents(isAudioOnly ? id_audioDisplay : videoDisplays[0], videoDisplays.length);

            if (nr >= 2) {
                // throw some important synchronize.js-events for other plugins
                $(document).on(event_sjs_allPlayersReady, function(event) {
                    videosReady = true;
                    Engage.trigger(plugin.events.ready.getName());
                });
                $(document).on(event_sjs_playerLoaded, function(event) {
                    Engage.trigger(plugin.events.playerLoaded.getName());
                });
                $(document).on(event_sjs_masterPlay, function(event) {
                    Engage.trigger(plugin.events.play.getName(), true);
                    pressedPlayOnce = true;
                });
                $(document).on(event_sjs_masterPause, function(event) {
                    Engage.trigger(plugin.events.pause.getName(), true);
                });
                $(document).on(event_sjs_masterEnded, function(event) {
                    Engage.trigger(plugin.events.ended.getName(), true);
                });
                $(document).on(event_sjs_masterTimeupdate, function(event, time) {
                    Engage.trigger(plugin.events.timeupdate.getName(), time, true);
                });
                $(document).on(event_sjs_synchronizing, function(event) {
                    Engage.trigger(plugin.events.synchronizing.getName());
                });
                $(document).on(event_sjs_buffering, function(event) {
                    Engage.trigger(plugin.events.buffering.getName());
                });
                $(document).on(event_sjs_bufferedAndAutoplaying, function(event) {
                    Engage.trigger(plugin.events.bufferedAndAutoplaying.getName());
                });
                $(document).on(event_sjs_bufferedButNotAutoplaying, function(event) {
                    Engage.trigger(plugin.events.bufferedButNotAutoplaying.getName());
                });
                var i = 0;
                for (var vd in videoDisplays) {
                    if (i > 0) {
                        // sync every other videodisplay with the master
                        $.synchronizeVideos(0, videoDisplays[0], videoDisplays[vd]);
                        Engage.log("Video: Videodisplay " + vd + " is now being synchronized with the master videodisplay");
                    }
                    ++i;
                }
                if (isUsingFlash) {
                    $(document).trigger(event_sjs_isUsingFlash, []);
                    $(document).trigger(event_sjs_debug, Engage.model.get("isDebug"));
                }
            } else {
                videosReady = true;
                if (!isAudioOnly) {
                    Engage.trigger(plugin.events.ready.getName());
                }
            }
        }
        // Set Displays to correct size
        orderVideoDisplays(videoDisplays);
        checkVideoDisplaySize();
    }

    function renderVideoDisplay(videoDataView) {
        Engage.log("Rendering video displays");
        var src = (videoDataView.model.get("videoSources") && videoDataView.model.get("videoSources")["audio"]) ? videoDataView.model.get("videoSources")["audio"] : [];
        var tempVars = {
            ids: videoDataView.model.get("ids"),
            type: videoDataView.model.get("type"),
            sources: src,
            str_error_AudioCodecNotSupported: translate("error_AudioCodecNotSupported", "Error: The audio codec is not supported by this browser."),
            str_error_AudioElementNotSupported: translate("error_AudioElementNotSupported", "Error: Your browser does not support the audio element.")
        };
        if (isEmbedMode && !isAudioOnly) {
            tempVars.id = videoDataView.model.get("ids")[0];
        }
        // compile template and load into the html
        videoDataView.$el.html(_.template(videoDataView.template, tempVars));

        var i = 0;
        var videoDisplays = videoDataView.model.get("ids");
        var videoSources = videoDataView.model.get("videoSources");

        if (!mediapackageError) {
            // get aspect ratio
            Engage.log("Calculating Aspect ratio");
            aspectRatio = null;
            var as1 = 0;
            for (var flavor in videoResultions) {
                if ((aspectRatio == null) || (as1 < videoResultions[flavor])) {
                    as1 = videoResultions[flavor][1];
                    aspectRatio = videoResultions[flavor];
                }
            }
            for (var v in videoSources) {
                for (var j = 0; j < videoSources[v].length; ++j) {
                    var aspectRatio_tmp = videoSources[v][j].resolution;
                    var t_tmp = $.type(aspectRatio_tmp);
                    if ((t_tmp === "string") && (/\d+x\d+/.test(aspectRatio_tmp))) {
                        aspectRatio_tmp = aspectRatio_tmp.match(/(\d+)x(\d+)/);
                        if ((aspectRatio == null) || (as1 < parseInt(aspectRatio_tmp[1]))) {
                            as1 = parseInt(aspectRatio_tmp[1]);
                            aspectRatio = Utils.parseVideoResolution(videoSources[v][j].resolution);
                        }
                    }
                }
            }

            $(window).on("orientationchange", function(event) {
                Engage.log("Video: Device twisted");
                checkVideoDisplaySize();
                orderVideoDisplays(videoDisplays);
            });

            isAudioOnly = videoDataView.model.get("type") == "audio";
            Engage.trigger(plugin.events.isAudioOnly.getName(), isAudioOnly);

            if (isDesktopMode) {
                renderDesktop(videoDataView, videoSources, videoDisplays, aspectRatio);
            } else if (isEmbedMode && videoSources && videoDisplays && aspectRatio) {
                renderEmbed(videoDataView, videoSources, videoDisplays, aspectRatio);
            } else if (isMobileMode && videoSources && videoDisplays && aspectRatio) {
                renderMobile(videoDataView, videoSources, videoDisplays, aspectRatio);
            }
            if (videoDataView.model.get("type") != "audio") {
                checkVideoDisplaySize();
                window.setTimeout(checkVideoDisplaySize, checkVideoDisplaySizeTimeout);
            }
        }
    }

    function prepareRenderingVideoDisplay(videoDataView) {
        if (loadHls) {
            require([relative_plugin_path + mediaSourcesPath], function(videojsmedia) {
                Engage.log("Video: Lib videojs media sources loaded");
                require([relative_plugin_path + hlsPath], function(videojshls) {
                    Engage.log("Video: Lib videojs HLS playback loaded");
                    renderVideoDisplay(videoDataView);
                });
            });
        } else {
            renderVideoDisplay(videoDataView);
        }
    }

    var VideoDataView = Backbone.View.extend({
        el: $("#" + id_engage_video),
        initialize: function(videoDataModel, template, videojs_swf) {
            this.setElement($(plugin.container));
            this.model = videoDataModel;
            this.template = template;
            this.videojs_swf = videojs_swf;
            _.bindAll(this, "render");
            this.model.bind("change", this.render);
            this.render();
        },
        render: function() {
            prepareRenderingVideoDisplay(this);
        }
    });

    function initMobileEvents() {
        events.tapHold = new Engage.Event("Video:tapHold", "videoDisplay tapped", "both");
        events.resize = new Engage.Event("Video:resize", "videoDisplay is resized", "both");
        events.swipeLeft = new Engage.Event("Video:swipeLeft", "videoDisplay swiped", "both");
        events.deactivate = new Engage.Event("Video:deactivate", "videoDisplay deactivated", "both");
        events.activate = new Engage.Event("Video:activate", "videoDisplay activated", "both");
        events.resize = new Engage.Event("Video:resize", "videoDisplay resized", "both");

        Engage.on(plugin.events.deactivate.getName(), function(id) {
            Engage.log("Video: Deactivate: " + id);
        });

        $(".video-js").on('taphold', function(event) {
            Engage.log("Video: " + event);
            Engage.trigger(plugin.events.tapHold.getName(), event.currentTarget.id);
        });

        $(".video-js").on('swipeleft', function(event) {
            Engage.log("Video: " + event);
            Engage.trigger(plugin.events.swipeLeft.getName(), event.currentTarget.id);
        });
    }

    function initVideojsVideo(id, videoSource, videojs_swf) {
        Engage.log("Video: Initializing video.js-display '" + id + "'");

        if (id) {
            if (videoSource) {

                if (!isAudioOnly) {
                    var videoOptions = {
                        "controls": false,
                        "autoplay": false,
                        "preload": "auto",
                        "poster": videoSource.poster ? videoSource.poster : "",
                        "loop": false,
                        "width": "100%",
                        "height": "100%"
                    };
                    if (isEmbedMode) {
                        videoOptions.controls = true;
                    }

                    // init video.js
                    videojs(id, videoOptions, function() {
                        var theodulVideodisplay = this;
                        // set sources
                        theodulVideodisplay.src(videoSource);
                    });
                    // URL to the flash swf
                    if (videojs_swf) {
                        Engage.log("Video: Loaded flash component");
                        videojs.options.flash.swf = videojs_swf;
                    } else {
                        Engage.log("Video: No flash component loaded");
                    }
                    isUsingFlash = $("#" + id_generated_videojs_flash_component).length > 0;
                    Engage.trigger(plugin.events.usingFlash.getName(), isUsingFlash);
                }
            } else {
                Engage.log("Video: Error: No video source available");
                $("#" + id_videojs_wrapper).html("No video sources available.");
            }
        } else {
            Engage.log("Video: Error: No ID available");
            $("#" + id_videojs_wrapper).html("No video available.");
        }
    }

    function orderVideoDisplays(videoDisplays) {
        // max size of display
        // minus footer and header

        var maxHeight = window.screen.height - 120;
        var maxWidth = window.screen.width;

        Engage.log("$(window).height():" + $(window).height());
        Engage.log("$(window).width():" + $(window).width());

        Engage.log("window.screen.height:" + window.screen.height);
        Engage.log("window.screen.width:" + window.screen.width);
        Engage.log("window.innerHeight:" + window.innerHeight);
        Engage.log("window.innerWidth:" + window.innerWidth);


        var videoWidth = 0;
        var videoHeight = 0;

        var ratio = 0;
        Engage.log("Size of Displayarea (w/h): " + maxWidth + "/" + maxHeight);

        if ((aspectRatio != null) && (videoDisplays.length > 0)) {
            aspectRatio[1] = parseInt(aspectRatio[1]);
            aspectRatio[2] = parseInt(aspectRatio[2]);

            videoWidth = aspectRatio[1];
            videoHeight = aspectRatio[2];

            ratio = (aspectRatio[2] / aspectRatio[1])
            Engage.log("Calculated Ratio: " + ratio);

            Engage.trigger(plugin.events.aspectRatioSet.getName(), aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100);
        } else {
            Engage.trigger(plugin.events.aspectRatioSet.getName(), -1, -1, -1);
        }

        Engage.log("Videosize (w/h): " + videoWidth + "/" + videoHeight);

        if (Engage.model.get("orientation") == "landscape") {
            $('#videojs_wrapper').height(maxHeight - 120);
        } else {
            $('#videojs_wrapper').height(maxHeight - 120);
        }


        if (Engage.model.get("orientation") == "portrait") {
            if (videoDisplays.length > 1) {
                $('.mobileVideoBox').height((((1 / videoDisplays.length) * 100) - 0.5) + "%");
            } else {
                $('.mobileVideoBox').height(videoHeight);
            }
        };

        Engage.log("Set Videobox to (w/h): " + $('.mobileVideoBox').width() + "/" + $('.mobileVideoBox').height());
    }

    function checkVideoDisplaySize() {
        // make sure the video height is not greater than the window height
        if (Engage.model.get("mode") == "mobile") {
            var headerHeight = $('#mobile-header').height();
            var footerHeight = $('#mobile-footer').height();

            var total = headerHeight + footerHeight;
            if (Engage.model.get("orientation") == "portrait") {
                $("#" + id_engageContent).css("height", ($(window).height() - total) * 0.9);
                $("#" + id_engageContent).css("width", $(window).width() * 0.9);
            } else if (Engage.model.get("orientation") == "landscape") {
                $("#" + id_engageContent).css("height", ($(window).height() - total) * 0.9);
                $("#" + id_engageContent).css("width", $(window).width() * 0.9);
            };
        } else {
            $("#" + id_engageContent).css("max-width", "");
            for (var i = 0; i < videoDisplaySizeTimesCheck; ++i) {
                if ($(window).height() < ($("." + id_videojs_wrapperClass).position().top + $("." + id_videojs_wrapperClass).height())) {
                    $("#" + id_engageContent).css("max-width", $("#" + id_engageContent).width() / videoDisplaySizeFactor);
                } else {
                    break;
                }
            }
        }
    }

    function registerEvents(videoDisplay) {
        var theodulVideodisplay = videojs(videoDisplay);

        $(window).resize(function() {
            checkVideoDisplaySize();
        });

        Engage.on(plugin.events.play.getName(), function() {
            if (videosReady) {
                theodulVideodisplay.play();
            }
        });
        Engage.on(plugin.events.pause.getName(), function() {
            theodulVideodisplay.pause();
        });
    }

    function registerEvents(videoDisplay, numberOfVideodisplays) {
        if (isAudioOnly) {
            var audioPlayer_id = $("#" + videoDisplay);
            var audioPlayer = audioPlayer_id[0];
            var audioLoadTimeout = window.setTimeout(function() {
                Engage.trigger(plugin.events.audioCodecNotSupported.getName());
                $("." + class_audioDisplay).hide();
                $("." + class_audioDisplayError).show();
            }, audioLoadTimeoutCheckDelay);
            audioPlayer_id.on("canplay", function() {
                Engage.trigger(plugin.events.ready.getName());
                window.clearTimeout(audioLoadTimeout);
            });
            audioPlayer_id.on("play", function() {
                Engage.trigger(plugin.events.play.getName(), true);
                pressedPlayOnce = true;
            });
            audioPlayer_id.on("pause", function() {
                Engage.trigger(plugin.events.pause.getName(), true);
            });
            audioPlayer_id.on("ended", function() {
                Engage.trigger(plugin.events.ended.getName(), true);
            });
            audioPlayer_id.on("timeupdate", function() {
                Engage.trigger(plugin.events.timeupdate.getName(), audioPlayer.currentTime, true);
            });
            audioPlayer_id.on(event_html5player_volumechange, function() {
                Engage.trigger(plugin.events.volumechange.getName(), audioPlayer.volume * 100);
            });
            Engage.on(plugin.events.play.getName(), function(triggeredByMaster) {
                if (!triggeredByMaster && videosReady) {
                    audioPlayer.play();
                    pressedPlayOnce = true;
                }
            });
            Engage.on(plugin.events.pause.getName(), function(triggeredByMaster) {
                if (!triggeredByMaster && pressedPlayOnce) {
                    audioPlayer.pause();
                }
            });
            Engage.on(plugin.events.volumeSet.getName(), function(percentAsDecimal) {
                Engage.log("Video: Volume set to " + percentAsDecimal);
                if ((percentAsDecimal / 100) > 0.09) {
                    audioPlayer.volume = percentAsDecimal / 100;
                } else {
                    audioPlayer.volume = percentAsDecimal;
                }
            });
            Engage.on(plugin.events.volumeGet.getName(), function(callback) {
                callback(audioPlayer.volume);
            });
            Engage.on(plugin.events.seek.getName(), function(time) {
                Engage.log("Video: Seek to " + time);
                if (videosReady && pressedPlayOnce) {
                    var duration = parseInt(Engage.model.get("videoDataModel").get("duration")) / 1000;
                    if (duration && (time < duration)) {
                        audioPlayer.currentTime = time;
                    } else {
                        Engage.trigger(plugin.events.customError.getName(), translate("givenTime", "The given time") + " (" + Utils.formatSeconds(time) + ") " + translate("hasToBeSmallerThanDuration", "has to be smaller than the duration") + " (" + Utils.formatSeconds(duration) + ").");
                        Engage.trigger(plugin.events.timeupdate.getName(), audioPlayer.currentTime);
                    }
                } else {
                    if (!videosReady) {
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_waitToSetTime", "Please wait until the video has been loaded to set a time."));
                    } else { // pressedPlayOnce
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_startPlayingToSetTime", "Please start playing the video once to set a time."));
                    }
                    Engage.trigger(plugin.events.timeupdate.getName(), 0);
                }
            });
            Engage.on(plugin.events.sliderStop.getName(), function(time) {
                Engage.log("Video: Slider stopped at " + time);
                if (videosReady && pressedPlayOnce) {
                    var duration = parseInt(Engage.model.get("videoDataModel").get("duration"));
                    var normTime = (time / 1000) * (duration / 1000);
                    audioPlayer.currentTime = normTime;
                } else {
                    if (!videosReady) {
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_waitToSeek", "Please wait until the video has been loaded to seek."));
                    } else { // pressedPlayOnce
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_startPlayingToSeek", "Please start playing the video once to seek."));
                    }
                    Engage.trigger(plugin.events.timeupdate.getName(), 0);
                }
            });
            Engage.on(plugin.events.ended.getName(), function(time) {
                if (videosReady) {
                    Engage.log("Video: Ended at " + time);
                    audioPlayer.pause();
                    Engage.trigger(plugin.events.pause.getName());
                    audioPlayer.currentTime = audioPlayer.duration;
                }
            });
        } else {
            var theodulVideodisplayMaster = videojs(videoDisplay);

            if (numberOfVideodisplays == 1) {
                theodulVideodisplayMaster.on("play", function() {
                    Engage.trigger(plugin.events.play.getName(), true);
                    pressedPlayOnce = true;
                });
                theodulVideodisplayMaster.on("pause", function() {
                    Engage.trigger(plugin.events.pause.getName(), true);
                });
                theodulVideodisplayMaster.on("ended", function() {
                    Engage.trigger(plugin.events.ended.getName(), true);
                });
                theodulVideodisplayMaster.on("timeupdate", function() {
                    Engage.trigger(plugin.events.timeupdate.getName(), theodulVideodisplayMaster.currentTime(), true);
                });
            }
            $("#" + id_btn_fullscreenCancel).click(function(e) {
                e.preventDefault();
                Engage.trigger(plugin.events.fullscreenCancel.getName());
            });
            Engage.on(plugin.events.fullscreenEnable.getName(), function() {
                $("#" + videoDisplay).removeClass("vjs-controls-disabled").addClass("vjs-controls-enabled");
                if (numberOfVideodisplays == 1) {
                    theodulVideodisplayMaster.requestFullscreen();
                } else {
                    $(window).scrollTop(0);
                    $("body").css("overflow", "hidden");
                    $(window).scroll(function() {
                        $(this).scrollTop(0);
                    });
                    $("#" + id_engage_video).css("z-index", 995).css("position", "relative");
                    $("#" + id_page_cover).css("opacity", 0.9).fadeIn(300, function() {});
                }
            });
            Engage.on(plugin.events.fullscreenCancel.getName(), function() {
                $("#" + videoDisplay).removeClass("vjs-controls-enabled").addClass("vjs-controls-disabled");
                if (numberOfVideodisplays > 1) {
                    $("body").css("overflow", "auto");
                    $(window).unbind("scroll");
                    $("#" + id_page_cover).css("opacity", 0.9).fadeOut(300, function() {
                        $("#" + id_engage_video).css("z-index", 0).css("position", "");
                    });
                }
            });
            Engage.on(plugin.events.playbackRateChanged.getName(), function(rate) {
                if (pressedPlayOnce) {
                    Engage.log("Video: Playback rate changed to rate " + rate);
                    theodulVideodisplayMaster.playbackRate(rate);
                }
            });
            Engage.on(plugin.events.play.getName(), function(triggeredByMaster) {
                if (!triggeredByMaster && videosReady) {
                    theodulVideodisplayMaster.play();
                    pressedPlayOnce = true;
                }
            });
            Engage.on(plugin.events.pause.getName(), function(triggeredByMaster) {
                if (!triggeredByMaster && pressedPlayOnce) {
                    theodulVideodisplayMaster.pause();
                }
            });
            Engage.on(plugin.events.volumeSet.getName(), function(percentAsDecimal) {
                Engage.log("Video: Volume changed to " + percentAsDecimal);
                theodulVideodisplayMaster.volume(percentAsDecimal);
            });
            Engage.on(plugin.events.volumeGet.getName(), function(callback) {
                callback(theodulVideodisplayMaster.volume());
            });
            Engage.on(plugin.events.seek.getName(), function(time) {
                Engage.log("Video: Seek to " + time);
                if (videosReady && pressedPlayOnce) {
                    var duration = parseInt(Engage.model.get("videoDataModel").get("duration")) / 1000;
                    if (duration && (time < duration)) {
                        theodulVideodisplayMaster.currentTime(time);
                    } else {
                        Engage.trigger(plugin.events.customError.getName(), translate("givenTime", "The given time") + " (" + Utils.formatSeconds(time) + ") " + translate("hasToBeSmallerThanDuration", "has to be smaller than the duration") + " (" + Utils.formatSeconds(duration) + ").");
                        Engage.trigger(plugin.events.timeupdate.getName(), theodulVideodisplayMaster.currentTime());
                    }
                } else {
                    if (!videosReady) {
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_waitToSetTime", "Please wait until the video has been loaded to set a time."));
                    } else { // pressedPlayOnce
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_startPlayingToSetTime", "Please start playing the video once to set a time."));
                    }
                    Engage.trigger(plugin.events.timeupdate.getName(), 0);
                }
            });
            Engage.on(plugin.events.sliderStop.getName(), function(time) {
                if (videosReady && pressedPlayOnce) {
                    var duration = parseInt(Engage.model.get("videoDataModel").get("duration"));
                    var normTime = (time / 1000) * (duration / 1000);
                    theodulVideodisplayMaster.currentTime(normTime);
                } else {
                    if (!videosReady) {
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_waitToSeek", "Please wait until the video has been loaded to seek."));
                    } else { // pressedPlayOnce
                        Engage.trigger(plugin.events.customNotification.getName(), translate("msg_startPlayingToSeek", "Please start playing the video once to seek."));
                    }
                    Engage.trigger(plugin.events.timeupdate.getName(), 0);
                }
            });
            Engage.on(plugin.events.ended.getName(), function(time) {
                if (videosReady) {
                    Engage.log("Video: Video ended and ready");
                    theodulVideodisplayMaster.pause();
                    Engage.trigger(plugin.events.pause.getName());
                    theodulVideodisplayMaster.currentTime(0);
                    //theodulVideodisplayMaster.currentTime(theodulVideodisplayMaster.duration());
                    //Engage.trigger(plugin.events.seek.getName(), 0);
                }
            });
            theodulVideodisplayMaster.on(event_html5player_volumechange, function() {
                Engage.trigger(plugin.events.volumechange.getName(), theodulVideodisplayMaster.volume());
            });
            theodulVideodisplayMaster.on(event_html5player_fullscreenchange, function() {
                Engage.trigger(plugin.events.fullscreenChange.getName());
            });
        }
    }

    function setupStreams(tracks, attachments) {
        Engage.log("setting up streams");
        var mediaInfo = {};
        mediaInfo.tracks = tracks;
        mediaInfo.attachments = attachments;

        if (mediaInfo.tracks && (mediaInfo.tracks.length > 0)) {

            for (i = 0; i < mediaInfo.tracks.length; i++) {
                if (flavors.indexOf(mediaInfo.tracks[i].type) < 0) {
                    flavors += mediaInfo.tracks[i].type + ",";
                }

                //rtmp is treated different for video.js. Mimetype and 
                //url have to be changed                      
                if (mediaInfo.tracks[i].mimetype == "video/mp4" &&
                    (mediaInfo.tracks[i].url.indexOf("rtmp://") > -1 ||
                        mediaInfo.tracks[i].url.indexOf("RTMP://") > -1)) {
                    mediaInfo.tracks[i].mimetype = "rtmp/mp4";
                    mediaInfo.tracks[i].url = Utils.replaceAll(mediaInfo.tracks[i].url, "mp4:", "&mp4:");
                }

                //Adaptive streaming manifests don't have a resolution. Extract these from regular videos.
                if (mediaInfo.tracks[i].mimetype.match(/video/g) && mediaInfo.tracks[i] &&
                    mediaInfo.tracks[i].video && mediaInfo.tracks[i].video.resolution &&
                    videoResultions[Utils.extractFlavorMainType(mediaInfo.tracks[i].type)] == null) {
                    videoResultions[Utils.extractFlavorMainType(mediaInfo.tracks[i].type)] = Utils.parseVideoResolution(mediaInfo.tracks[i].video.resolution);
                }

                if (mimetypes.indexOf(mediaInfo.tracks[i].mimetype) < 0) {
                    mimetypes += mediaInfo.tracks[i].mimetype + ",";
                }
            }
            flavors = flavors.substring(0, flavors.length - 1);
            mimetypes = mimetypes.substring(0, mimetypes.length - 1);

            var flavorsArray = flavors.split(",");

            var videoDisplays = [];
            var videoSources = [];
            videoSources.audio = [];

            for (i = 0; i < flavorsArray.length; i++) {
                videoSources[Utils.extractFlavorMainType(flavorsArray[i])] = [];
            }

            var hasVideo = false
            var hasAudio = false;

            // look for video source
            var duration = 0;
            if (mediaInfo.tracks) {
                $(mediaInfo.tracks).each(function(i, track) {
                    if (track.mimetype && track.type && acceptFormat(track)) {
                        if (track.mimetype.match(/video/g) || track.mimetype.match(/application/g) || track.mimetype.match(/rtmp/g)) {
                            hasVideo = true;
                            if (track.duration > duration) {
                                duration = track.duration;
                            }
                            var resolution = (track.video && track.video.resolution) ? track.video.resolution : "";
                            // filter for different video sources
                            Engage.log("Adding video source: " + track.url + " (" + track.mimetype + ")");
                            if (track.mimetype == "application/dash+xml") {
                                loadDash = true;
                            }
                            if (track.mimetype == "application/x-mpegURL") {
                                loadHls = true;
                            }
                            videoSources[Utils.extractFlavorMainType(track.type)].push({
                                src: track.url,
                                type: track.mimetype,
                                typemh: track.type,
                                resolution: resolution
                            });
                        } else if (track.mimetype.match(/audio/g)) {
                            hasAudio = true;
                            if (track.duration > duration) {
                                duration = track.duration;
                            }
                            videoSources.audio.push({
                                src: track.url,
                                type: track.mimetype,
                                typemh: track.type
                            });
                        }
                    }
                });

                if (!hasVideo) {
                    for (i = 0; i < videoSources.length; i++) {
                        if (videoSources[i] !== videoSources.audio) {
                            delete videoSources.flavor;
                        }
                    }
                }

                if (hasVideo || !hasAudio) {
                    delete videoSources.audio;
                }
            }
            if (mediaInfo.attachments && (mediaInfo.attachments.length > 0)) {
                $(mediaInfo.attachments).each(function(i, attachment) {
                    if (attachment.mimetype && attachment.type && attachment.mimetype.match(/image/g) && attachment.type.match(/player/g)) {
                        // filter for different video sources
                        videoSources[Utils.extractFlavorMainType(attachment.type)]["poster"] = attachment.url;
                    }
                });
            }
            var i = 0;
            for (var v in videoSources) {
                if (videoSources[v].length > 0) {
                    var name = videoDisplayNamePrefix.concat(i);
                    videoDisplays.push(name);
                    ++i;
                }
            }
            Engage.model.set("videoDataModel", new VideoDataModel(videoDisplays, videoSources, duration));
        }
    }

    function initPlugin() {
        Engage.log("init Plugin " + initCount);
        // only init if plugin template was inserted into the DOM
        if (plugin.inserted) {
            Engage.log("Video Plugin inserted");
            // set path to swf player
            var videojs_swf = plugin.pluginPath + videojs_swf_path;
            Engage.log('SWF Pfad: ' + videojs_swf_path);
            Engage.model.on(videoDataModelChange, function() {
                videoDataView = new VideoDataView(this.get("videoDataModel"), plugin.template, videojs_swf);
            });
            Engage.on(plugin.events.mediaPackageModelError.getName(), function(msg) {
                mediapackageError = true;
            });
            Engage.model.get("mediaPackage").on("change", function() {
                setupStreams(this.get("tracks"), this.get("attachments"));
            });
            if (Engage.model.get("mediaPackage").get("tracks")) {
                Engage.log("Mediapackage already available.")
                setupStreams(Engage.model.get("mediaPackage").get("tracks"), Engage.model.get("mediaPackage").get("attachments"));
            }
        }
    }

    // init Event
    Engage.log("Video: Init");
    var relative_plugin_path = Engage.getPluginPath("EngagePluginVideoVideoJS");

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function() {
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("Video: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load utils class
    require([relative_plugin_path + "utils"], function(utils) {
        Engage.log("Video: Utils class loaded");
        Utils = new utils();
        initTranslate(Utils.detectLanguage(), function() {
            Engage.log("Videodisplay: Successfully translated.");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        }, function() {
            Engage.log("Videodisplay: Error translating...");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });
    });

    // load videoData model
    require([relative_plugin_path + "models/videoData"], function(model) {
        Engage.log("Video: VideoData model loaded");
        VideoDataModel = model;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load video.js lib
    require([relative_plugin_path + videoPath], function(videojs) {
        Engage.log("Video: Lib video loaded");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load synchronize.js lib
    require([relative_plugin_path + synchronizePath], function(videojs) {
        Engage.log("Video: Lib synchronize loaded");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    return plugin;
});

