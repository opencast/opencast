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
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function(require, $, _, Backbone, Engage) {
    "use strict";
    var PLUGIN_NAME = "Engage VideoJS Videodisplay";
    var PLUGIN_TYPE = "engage_video";
    var PLUGIN_VERSION = "0.1",
        PLUGIN_TEMPLATE = "template.html",
        PLUGIN_TEMPLATE_MOBILE = "template_mobile.html",
        PLUGIN_TEMPLATE_EMBED = "template_embed.html",
        PLUGIN_STYLES = [
            "style.css",
            "lib/videojs/video-js.css"
        ],
        PLUGIN_STYLES_MOBILE = [
            "style_mobile.css",
            "lib/videojs/video-js.css"
        ],
        PLUGIN_STYLES_EMBED = [
            "style_embed.css",
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
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES,
                template: PLUGIN_TEMPLATE,
                events: events
            };
            isDesktopMode = true;
            break;
    }

    /* change these variables */
    var videoPath = "lib/videojs/video";
    var synchronizePath = "lib/synchronize";
    var videojs_swf_path = "lib/videojs/video-js.swf";
    var videoDisplaySizeFactor = 1.1;
    var videoDisplaySizeTimesCheck = 100; // the smaller the factor, the higher the times check!

    /* don't change these variables */
    var aspectRatio = "";
    var initCount = 4;
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
    var class_vjsposter = "vjs-poster";
    var class_vjs_openInPlayer = "vjs-openInPlayer";
    var class_vjs_control = "vjs-control";
    var class_vjs_control_text = "vjs-control-text";
    var class_vjs_mute_control = "vjs-mute-control";
    var videosReady = false;
    var pressedPlayOnce = false;
    var mediapackageChange = "change:mediaPackage";
    var engageModelChange = "change:videoDataModel";
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

    function escapeRegExp(string) {
        return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    }

    function replaceAll(string, find, replace) {
        return string.replace(new RegExp(escapeRegExp(find), 'g'), replace);
    }

    var VideoDataView = Backbone.View.extend({
        el: $("#" + id_engage_video), // every view has an element associated with it
        initialize: function(videoDataModel, template, videojs_swf) {
            this.setElement($(plugin.container)); // every plugin view has it's own container associated with it
            this.model = videoDataModel;
            this.template = template;
            this.videojs_swf = videojs_swf;
            // bind the render function always to the view
            _.bindAll(this, "render");
            // listen for changes of the model and bind the render function to this
            this.model.bind("change", this.render);
            this.render();
        },
        render: function() {
            var tempVars = {
                ids: this.model.get("ids")
            };
            if (isEmbedMode) {
                tempVars.id = this.model.get("ids")[0];
            }
            // compile template and load into the html
            this.$el.html(_.template(this.template, tempVars));

            var i = 0;
            var videoDisplays = this.model.get("ids");
            var videoSources = this.model.get("videoSources");

            if (!mediapackageError) {
                // get aspect ratio
                aspectRatio = null;
                var as1 = 0;
                for (var i = 0; i < videoDisplays.length; ++i) {
                    if (videoSources.presenter && videoSources.presenter[i] && videoSources.presenter[i].resolution) {
                        for (var j = 0; j < videoSources.presenter.length; ++j) {
                            var aspectRatio_tmp = videoSources.presenter[j].resolution;
                            var t_tmp = $.type(aspectRatio_tmp);
                            if ((t_tmp === 'string') && (/\d+x\d+/.test(aspectRatio_tmp))) {
                                aspectRatio_tmp = aspectRatio_tmp.match(/(\d+)x(\d+)/);
                                if ((aspectRatio == null) || (as1 < parseInt(aspectRatio_tmp[1]))) {
                                    as1 = parseInt(aspectRatio_tmp[1]);
                                    aspectRatio = videoSources.presenter[j].resolution;
                                }
                            }
                        }

                        var t = $.type(aspectRatio);

                        if ((t === 'string') && (/\d+x\d+/.test(aspectRatio))) {
                            aspectRatio = aspectRatio.match(/(\d+)x(\d+)/);
                            break;
                        }
                    }
                    // TODO: Same code as above...
                    else if (videoSources.presentation && videoSources.presentation[i] && videoSources.presentation[i].resolution) {
                        for (var j = 0; j < videoSources.presenter.length; ++j) {
                            var aspectRatio_tmp = videoSources.presenter[j].resolution;
                            var t_tmp = $.type(aspectRatio_tmp);
                            if ((t_tmp === 'string') && (/\d+x\d+/.test(aspectRatio_tmp))) {
                                aspectRatio_tmp = aspectRatio_tmp.match(/(\d+)x(\d+)/);
                                if ((aspectRatio == null) || (as1 < parseInt(aspectRatio_tmp[1]))) {
                                    as1 = parseInt(aspectRatio_tmp[1]);
                                    aspectRatio = videoSources.presenter[j].resolution;
                                }
                            }
                        }

                        var t = $.type(aspectRatio);

                        if ((t === 'string') && (/\d+x\d+/.test(aspectRatio))) {
                            aspectRatio = aspectRatio.match(/(\d+)x(\d+)/);
                            break;
                        }
                    }
                }

                if (isDesktopMode) {
                    for (var v in videoSources) {
                        if (videoSources[v].length > 0) {
                            initVideojsVideo(videoDisplays[i], videoSources[v], this.videojs_swf);
                            ++i;
                        }
                    }

                    if ((aspectRatio != null) && (videoDisplays.length > 0)) {
                        aspectRatio[1] = parseInt(aspectRatio[1]);
                        aspectRatio[2] = parseInt(aspectRatio[2]);
                        Engage.log("Video: Aspect ratio: " + aspectRatio[1] + "x" + aspectRatio[2] + " == " + ((aspectRatio[2] / aspectRatio[1]) * 100));
			Engage.trigger(plugin.events.aspectRatioSet.getName(), [aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100]);
                        $("." + id_videoDisplayClass).css("width", (((1 / videoDisplays.length) * 100) - 0.5) + "%");
                        $("." + id_videoDisplayClass).each(function(index) {
                            if ((index % 2) == 1) {
                                $(this).css("float", "right");
                            }
                        });
                        for (i = 0; i < videoDisplays.length; ++i) {
                            $("#" + videoDisplays[i]).css("padding-top", (aspectRatio[2] / aspectRatio[1] * 100) + "%").addClass("auto-height");
                        }
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
                        registerEvents(videoDisplays[0], videoDisplays.length);

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
                                    Engage.log("Video: Videodisplay " + vd + " is now being synchronized with the master videodisplay " + 0);
                                }
                                ++i;
                            }
                        } else {
                            videosReady = true;
                            Engage.trigger(plugin.events.ready.getName());
                        }

                        $(window).resize(function() {
                            checkVideoDisplaySize();
                        });
                    }
                } else if (isEmbedMode) {
                    for (var v in videoSources) {
                        if (videoSources[v].length > 0) {
                            initVideojsVideo(videoDisplays[i], videoSources[v], this.videojs_swf);
                            break; // just init the first video
                        }
                    }

                    $("." + class_vjs_mute_control).after("<div id=\"" + id_btn_openInPlayer + "\" class=\"" + class_vjs_openInPlayer + " " + class_vjs_control + "\" role=\"button\" aria-live=\"polite\" tabindex=\"0\"><div><span class=\"" + class_vjs_control_text + "\">Open in player</span></div></div>");

                    $("#" + id_btn_openInPlayer).click(function(e) {
                        e.preventDefault();
                        var str = window.location.href;
                        if (str.indexOf("mode=embed") == -1) {
                            str += "&mode=embed";
                        } else {
                            str = replaceAll(str, "mode=embed", "mode=desktop");
                        }
                        window.location = str;
                    });

                    if ((aspectRatio != null) && (videoDisplays.length > 0)) {
                        aspectRatio[1] = parseInt(aspectRatio[1]);
                        aspectRatio[2] = parseInt(aspectRatio[2]);
                        Engage.log("Video: Aspect ratio: " + aspectRatio[1] + "x" + aspectRatio[2] + " == " + ((aspectRatio[2] / aspectRatio[1]) * 100));
			Engage.trigger(plugin.events.aspectRatioSet.getName(), aspectRatio[1], aspectRatio[2], (aspectRatio[2] / aspectRatio[1]) * 100);
                        $("." + id_videoDisplayClass).css("width", "100%");
                        for (i = 0; i < videoDisplays.length; ++i) {
                            $("#" + videoDisplays[i]).css("padding-top", (aspectRatio[2] / aspectRatio[1] * 100) + "%").addClass("auto-height");
                        }
                    }

                    // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
                    $("." + class_vjsposter).show();
                    Engage.trigger(plugin.events.numberOfVideodisplaysSet.getName(), videoDisplays.length);

                    if (videoDisplays.length > 0) {
                        // set first videoDisplay as master
                        registerEvents(videoDisplays[0], 1);

                        videosReady = true;
                        Engage.trigger(plugin.events.ready.getName());

                        $(window).resize(function() {
                            checkVideoDisplaySize();
                        });
                    }
                }
                checkVideoDisplaySize();
            }
        }
    });

    var VideoDataModel = Backbone.Model.extend({
        initialize: function(ids, videoSources, duration) {
            Engage.log("Video: Init VideoDataModel");
            this.attributes.ids = ids;
            this.attributes.videoSources = videoSources;
            this.attributes.duration = duration;
        },
        defaults: {
            "ids": [],
            "videoSources": [],
            "isPlaying": false,
            "currentTime": -1,
            "duration": -1
        }
    });

    function initVideojsVideo(id, videoSource, videojs_swf) {
        Engage.log("Video: Initializing video.js-display: '" + id + "'");

        if (id) {
            if (videoSource) {
                var videoOptions = {
                    "controls": false,
                    "autoplay": false,
                    "preload": "auto",
                    "poster": videoSource.poster,
                    "loop": false,
                    "width": "100%",
                    "height": "100%" // ,
                        // "playbackRates": [0.5, 1, 1.5, 2]
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
                var flashComponentUsed = $("#" + id_generated_videojs_flash_component).length > 0;
                Engage.trigger(plugin.events.usingFlash.getName(), flashComponentUsed);
            } else {
                Engage.log("Video: Error: No video source available");
                $("#" + id_videojs_wrapper).html("No video sources available.");
            }
        } else {
            Engage.log("Video: Error: No ID available");
            $("#" + id_videojs_wrapper).html("No video available.");
        }
    }

    function checkVideoDisplaySize() {
        // make sure the video height is not greater than the window height
        $("#" + id_engageContent).css("max-width", "");
        for (var i = 0; i < videoDisplaySizeTimesCheck; ++i) {
            if ($(window).height() < ($("." + id_videojs_wrapperClass).position().top + $("." + id_videojs_wrapperClass).height())) {
                $("#" + id_engageContent).css("max-width", $("#" + id_engageContent).width() / videoDisplaySizeFactor);
            } else {
                break;
            }
        }
    }

    /**
     * Returns the formatted seconds
     *
     * @param seconds seconds to format
     * @return formatted seconds
     */
    function formatSeconds(seconds) {
        if (!seconds) {
            seconds = 0;
        }
        seconds = (seconds < 0) ? 0 : seconds;
        var result = "";
        if (parseInt(seconds / 3600) < 10) {
            result += "0";
        }
        result += parseInt(seconds / 3600);
        result += ":";
        if ((parseInt(seconds / 60) - parseInt(seconds / 3600) * 60) < 10) {
            result += "0";
        }
        result += parseInt(seconds / 60) - parseInt(seconds / 3600) * 60;
        result += ":";
        if (seconds % 60 < 10) {
            result += "0";
        }
        result += seconds % 60;
        if (result.indexOf(".") != -1) {
            result = result.substring(0, result.lastIndexOf(".")); // get rid of the .ms
        }
        return result;
    }

    function registerEvents(videoDisplay, numberOfVideodisplays) {
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
                $('body').css('overflow', 'hidden');
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
                $('body').css('overflow', 'auto');
                $(window).unbind('scroll');
                $("#" + id_page_cover).css("opacity", 0.9).fadeOut(300, function() {
                    $("#" + id_engage_video).css("z-index", 0).css("position", "");
                });
            }
        });
        Engage.on(plugin.events.playbackRateChanged.getName(), function(rate) {
            if (pressedPlayOnce) {
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
            theodulVideodisplayMaster.volume(percentAsDecimal);
        });
        Engage.on(plugin.events.volumeGet.getName(), function(callback) {
            callback(theodulVideodisplayMaster.volume());
        });
        Engage.on(plugin.events.seek.getName(), function(time) {
            if (videosReady && pressedPlayOnce) {
                var duration = parseInt(Engage.model.get("videoDataModel").get("duration")) / 1000;
                if (duration && (time < duration)) {
                    theodulVideodisplayMaster.currentTime(time);
                } else {
                    Engage.trigger(plugin.events.customError.getName(), "The given time (" + formatSeconds(time) + ") has to be smaller than the duration (" + formatSeconds(duration) + ").");
                    Engage.trigger(plugin.events.timeupdate.getName(), theodulVideodisplayMaster.currentTime());
                }
            } else {
                if (!videosReady) {
                    Engage.trigger(plugin.events.customNotification.getName(), "Please wait until the video has been loaded to set a time.");
                } else { // pressedPlayOnce
                    Engage.trigger(plugin.events.customNotification.getName(), "Please start playing the video once to set a time.");
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
                    Engage.trigger(plugin.events.customNotification.getName(), "Please wait until the video has been loaded to seek.");
                } else { // pressedPlayOnce
                    Engage.trigger(plugin.events.customNotification.getName(), "Please start playing the video once to seek.");
                }
                Engage.trigger(plugin.events.timeupdate.getName(), 0);
            }
        });
        Engage.on(plugin.events.ended.getName(), function(time) {
            if (videosReady) {
                theodulVideodisplayMaster.pause();
                Engage.trigger(plugin.events.pause.getName());
                theodulVideodisplayMaster.currentTime(theodulVideodisplayMaster.duration());
            }
        });
        theodulVideodisplayMaster.on(event_html5player_volumechange, function() {
            Engage.trigger(plugin.events.volumechange.getName(), theodulVideodisplayMaster.volume());
        });
        theodulVideodisplayMaster.on(event_html5player_fullscreenchange, function() {
            Engage.trigger(plugin.events.fullscreenChange.getName());
        });
    }

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if ((isDesktopMode || isEmbedMode) && plugin.inserted) {
            // set path to swf player
            var videojs_swf = plugin.pluginPath + videojs_swf_path;

            Engage.model.on(engageModelChange, function() {
                new VideoDataView(this.get("videoDataModel"), plugin.template, videojs_swf);
            });
            Engage.on(plugin.events.mediaPackageModelError.getName(), function(msg) {
                mediapackageError = true;
            });
            Engage.model.get("mediaPackage").on("change", function() {
                var mediaInfo = {};
                mediaInfo.tracks = this.get("tracks");
                mediaInfo.attachments = this.get("attachments");

                if ((mediaInfo.tracks.length > 0) && (mediaInfo.attachments.length > 0)) {
                    var videoDisplays = [];
                    var videoSources = [];
                    videoSources.presenter = [];
                    videoSources.presentation = [];

                    // look for video source
                    var duration = 0;
                    if (mediaInfo.tracks) {
                        $(mediaInfo.tracks).each(function(i, track) {
                            if (track.mimetype && track.type && track.mimetype.match(/video/g)) {
                                var resolution = (track.video && track.video.resolution) ? track.video.resolution : "";
                                // filter for different video sources
                                if (track.type.match(/presenter/g)) {
                                    if (track.duration > duration) {
                                        duration = track.duration;
                                    }
                                    videoSources.presenter.push({
                                        src: track.url,
                                        type: track.mimetype,
                                        typemh: track.type,
                                        resolution: resolution
                                    });
                                } else if (track.type.match(/presentation/g)) {
                                    if (track.duration > duration) {
                                        duration = track.duration;
                                    }
                                    videoSources.presentation.push({
                                        src: track.url,
                                        type: track.mimetype,
                                        typemh: track.type,
                                        resolution: resolution
                                    });
                                }
                            }
                        });
                    }
                    if (mediaInfo.attachments) {
                        $(mediaInfo.attachments).each(function(i, attachment) {
                            if (attachment.mimetype && attachment.type && attachment.mimetype.match(/image/g) && attachment.type.match(/player/g)) {
                                // filter for different video sources
                                if (attachment.type.match(/presenter/g)) {
                                    videoSources.presenter.poster = attachment.url;
                                }
                                if (attachment.type.match(/presentation/g)) {
                                    videoSources.presentation.poster = attachment.url;
                                }
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
            });
        }
    }

    if (isDesktopMode || isEmbedMode) {
        // init Event
        Engage.log("Video: Init");
        var relative_plugin_path = Engage.getPluginPath('EngagePluginVideoVideoJS');

        // load video.js lib
        require([relative_plugin_path + videoPath], function(videojs) {
            Engage.log("Video: Lib video loaded");
            initCount -= 1;
            if (initCount == 0) {
                initPlugin();
            }
        });

        // load synchronize.js lib
        require([relative_plugin_path + synchronizePath], function(videojs) {
            Engage.log("Video: Lib synchronize loaded");
            initCount -= 1;
            if (initCount == 0) {
                initPlugin();
            }
        });

        // listen on a change/set of the mediaPackage model
        Engage.model.on(mediapackageChange, function() {
            initCount -= 1;
            if (initCount == 0) {
                initPlugin();
            }
        });

        // all plugins loaded
        Engage.on(plugin.events.plugin_load_done.getName(), function() {
            Engage.log("Video: Plugin load done");
            initCount -= 1;
            if (initCount == 0) {
                initPlugin();
            }
        });
    }

    return plugin;
});
