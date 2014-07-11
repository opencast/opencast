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
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function (require, $, _, Backbone, Engage) {
    var PLUGIN_NAME = "Engage VideoJS Videodisplay";
    var PLUGIN_TYPE = "engage_video";
    var PLUGIN_VERSION = "0.1",
        PLUGIN_TEMPLATE = "template.html",
        PLUGIN_TEMPLATE_MOBILE = "template.html",
        PLUGIN_TEMPLATE_EMBED = "template.html",
        PLUGIN_STYLES = [
            "style.css",
            "lib/videojs/video-js.css"
        ],
        PLUGIN_STYLES_MOBILE = [
            "style.css",
            "lib/videojs/video-js.css"
        ],
        PLUGIN_STYLES_EMBED = [
            "style.css",
            "lib/videojs/video-js.css"
        ];

    var plugin;
    var events = {
        play: new Engage.Event("Video:play", "plays the video", "both"),
        pause: new Engage.Event("Video:pause", "pauses the video", "both"),
        ready: new Engage.Event("Video:ready", "all videos loaded successfully", "trigger"),
        ended: new Engage.Event("Video:ended", "end of the video", "trigger"),
        playerLoaded: new Engage.Event("Video:playerLoaded", "player loaded successfully", "trigger"),
        masterPlay: new Engage.Event("Video:masterPlay", "master video play", "trigger"),
        masterEnded: new Engage.Event("Video:masterEnded", "master video ended", "trigger"),
        masterTimeupdate: new Engage.Event("Video:masterTimeupdate", "master video timeupdate", "trigger"),
        synchronizing: new Engage.Event("Video:synchronizing", "synchronizing videos with the master video", "trigger"),
        buffering: new Engage.Event("Video:buffering", "video is buffering", "trigger"),
        bufferedAndAutoplaying: new Engage.Event("Video:bufferedAndAutoplaying", "buffering successful, was playing, autoplaying now", "trigger"),
        bufferedButNotAutoplaying: new Engage.Event("Video:bufferedButNotAutoplaying", "buffering successful, was not playing, not autoplaying now", "trigger"),
        timeupdate: new Engage.Event("Video:timeupdate", "timeupdate happened", "trigger"),
        volumechange: new Engage.Event("Video:volumechange", "volume change happened", "trigger"),
        fullscreenChange: new Engage.Event("Video:fullscreenChange", "fullscreen change happened", "trigger"),
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "", "handler"),
        fullscreenEnable: new Engage.Event("Video:fullscreenEnable", "go to fullscreen", "handler"),
        fullscreenCancel: new Engage.Event("Video:fullscreenCancel", "cancel fullscreen", "handler"),
        volumeSet: new Engage.Event("Video:volumeSet", "set the volume", "handler"),
        volumeGet: new Engage.Event("Video:volumeGet", "get the volume", "handler"),
        sliderStop: new Engage.Event("Slider:stop", "slider stopped", "handler"),
        seek: new Engage.Event("Video:seek", "seek video to a given position in seconds", "handler")
    };

    // desktop, embed and mobile logic
    switch (Engage.model.get("mode")) {
    case "desktop":
        plugin = {
            name: PLUGIN_NAME,
            type: PLUGIN_TYPE,
            version: PLUGIN_VERSION,
            styles: PLUGIN_STYLES,
            template: PLUGIN_TEMPLATE,
            events: events
        };
        break;
    case "mobile":
        plugin = {
            name: PLUGIN_NAME,
            type: PLUGIN_TYPE,
            version: PLUGIN_VERSION,
            styles: PLUGIN_STYLES_MOBILE,
            template: PLUGIN_TEMPLATE_MOBILE,
            events: events
        };
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
        break;
    }

    /* change these variables */
    var videoPath = "lib/videojs/video";
    var synchronizePath = "lib/synchronize";

    /* don't change these variables */
    var initCount = 4;
    var videoDisplayNamePrefix = "videojs_videodisplay_";
    var class_vjsposter = "vjs-poster";
    var id_engage_video = "engage_video";
    var id_videojs_wrapper = "videojs_wrapper";
    var videosReady = false;
    var mediapackageChange = "change:mediaPackage";

    var VideoDataView = Backbone.View.extend({
        el: $("#" + id_engage_video), // every view has an element associated with it
        initialize: function (videoDataModel, template, videojs_swf) {
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
        render: function () {
            // format values
            var tempVars = {
                ids: this.model.get("ids")
            };
            // compile template and load into the html
            this.$el.html(_.template(this.template, tempVars));

            var i = 0;
            var videoDisplays = this.model.get("ids");
            var videoSources = this.model.get("videoSources");
            for (var v in videoSources) {
                if (videoSources[v].length > 0) {
                    initVideojsVideo(videoDisplays[i], videoSources[v], this.videojs_swf);
                    ++i;
                }
            }
            // small hack for the posters: A poster is only being displayed when controls=true, so do it manually
            $("." + class_vjsposter).show();

            if (videoDisplays.length > 0) {
                // set first videoDisplay as master
                registerEvents(videoDisplays[0]);

                var nr = 0;
                for (var v in videoSources) {
                    if (videoSources[v].length > 0) {
                        ++nr;
                    }
                }

                if (nr >= 2) {
                    // throw some important synchronize.js-events for other plugins
                    $(document).on("sjs:allPlayersReady", function (event) {
                        videosReady = true;
                        Engage.trigger(plugin.events.ready.getName());
                    });
                    $(document).on("sjs:playerLoaded", function (event) {
                        Engage.trigger(plugin.events.playerLoaded.getName());
                    });
                    $(document).on("sjs:masterPlay", function (event) {
                        Engage.trigger(plugin.events.masterPlay.getName());
                    });
                    $(document).on("sjs:masterEnded", function (event) {
                        Engage.trigger(plugin.events.masterEnded.getName());
                    });
                    $(document).on("sjs:masterTimeupdate", function (event) {
                        Engage.trigger(plugin.events.masterTimeupdate.getName());
                    });
                    $(document).on("sjs:synchronizing", function (event) {
                        Engage.trigger(plugin.events.synchronizing.getName());
                    });
                    $(document).on("sjs:buffering", function (event) {
                        Engage.trigger(plugin.events.buffering.getName());
                    });
                    $(document).on("sjs:bufferedAndAutoplaying", function (event) {
                        Engage.trigger(plugin.events.bufferedAndAutoplaying.getName());
                    });
                    $(document).on("sjs:bufferedButNotAutoplaying", function (event) {
                        Engage.trigger(plugin.events.bufferedButNotAutoplaying.getName());
                    });

                    var i = 0;
                    for (var vd in videoDisplays) {
                        if (i > 0) {
                            // sync every other videodisplay with the master
                            $.synchronizeVideos(0, videoDisplays[0], videoDisplays[vd]);
                            Engage.log("Videodisplay " + vd + " is now being synchronized with the master videodisplay " + 0);
                        }
                        ++i;
                    }
                } else {
                    videosReady = true;
                    Engage.trigger(plugin.events.ready.getName());
                }
            }
        }
    });

    var VideoDataModel = Backbone.Model.extend({
        initialize: function (ids, videoSources, duration) {
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
        Engage.log("Initializing video.js-display: " + id);
        Engage.log("Initializing video source: ");
        Engage.log(videoSource);

        if (id) {
            if (videoSource) {
                var videoOptions = {
                    "controls": false,
                    "autoplay": false,
                    "preload": "auto",
                    "poster": videoSource.poster,
                    "loop": false,
                    "width": 640,
                    "height": 480
                };

                // init videoJS
                videojs(id, videoOptions, function () {
                    var theodulVideodisplay = this;
                    // set sources
                    theodulVideodisplay.src(videoSource);
                });
                // URL to the Flash SWF
                if (videojs_swf) {
                    Engage.log("Loaded flash component");
                    videojs.options.flash.swf = videojs_swf;
                } else {
                    Engage.log("No flash component loaded");
                }
            } else {
                Engage.log("Error: No video source available");
                $("#" + id_videojs_wrapper).html("No video sources available.");
            }
        } else {
            Engage.log("Error: No ID available");
            $("#" + id_videojs_wrapper).html("No video available.");
        }
    }

    function registerEvents(videoDisplay) {
        var theodulVideodisplay = videojs(videoDisplay);
        Engage.on(plugin.events.play.getName(), function () {
            if (videosReady) {
                theodulVideodisplay.play();
            }
        });
        Engage.on(plugin.events.pause.getName(), function () {
            theodulVideodisplay.pause();
        });
        Engage.on(plugin.events.fullscreenEnable.getName(), function () {
            $("#" + videoDisplay).removeClass("vjs-controls-disabled").addClass("vjs-controls-enabled");
            theodulVideodisplay.requestFullScreen();
        });
        Engage.on(plugin.events.fullscreenCancel.getName(), function () {
            $("#" + videoDisplay).removeClass("vjs-controls-enabled").addClass("vjs-controls-disabled");
            theodulVideodisplay.fullscreenCancel();
        });
        Engage.on(plugin.events.volumeSet.getName(), function (percentAsDecimal) {
            theodulVideodisplay.volume(percentAsDecimal);
        });
        Engage.on(plugin.events.volumeGet.getName(), function (callback) {
            callback(theodulVideodisplay.volume());
        });
        Engage.on(plugin.events.seek.getName(), function (time) {
            if (videosReady) {
                theodulVideodisplay.currentTime(time);
            }
        });
        Engage.on(plugin.events.sliderStop.getName(), function (time) {
            if (videosReady) {
                var duration = Engage.model.get("videoDataModel").get("duration");
                var normTime = (time / 1000) * (duration / 1000);
                theodulVideodisplay.currentTime(normTime);
            }
        });
        theodulVideodisplay.on("timeupdate", function () {
            if (videosReady) {
                Engage.log("CurrentTime while timeupdate: " + theodulVideodisplay.currentTime());
                Engage.trigger(plugin.events.timeupdate.getName(), theodulVideodisplay.currentTime());
            }
        });
        theodulVideodisplay.on("volumechange", function () {
            Engage.trigger(plugin.events.volumechange.getName(), theodulVideodisplay.volume());
        });
        theodulVideodisplay.on("fullscreenchange", function () {
            Engage.trigger(plugin.events.fullscreenChange.getName());
        });
        theodulVideodisplay.on("ended", function () {
            if (videosReady) {
                Engage.trigger(plugin.events.ended.getName());
                theodulVideodisplay.pause();
		Engage.trigger(plugin.events.pause.getName());
                theodulVideodisplay.currentTime(theodulVideodisplay.duration());
            }
        });
    }

    function initPlugin() {
        //only init if plugin template was inserted into the DOM
        if (plugin.inserted === true) {
            // set path to swf player
            var videojs_swf = plugin.pluginPath + "lib/videojs/video-js.swf";

            Engage.model.on("change:videoDataModel", function () {
                new VideoDataView(this.get("videoDataModel"), plugin.template, videojs_swf);
            });
            Engage.model.get("mediaPackage").on("change", function () {
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
                        $(mediaInfo.tracks).each(function (i, track) {
                            if (track.mimetype && track.type && track.mimetype.match(/video/g)) {
                                // filter for different video sources
                                if (track.type.match(/presenter/g)) {
                                    if (track.duration > duration) {
                                        duration = track.duration;
                                    }
                                    videoSources.presenter.push({
                                        src: track.url,
                                        type: track.mimetype,
                                        typemh: track.type
                                    });
                                } else if (track.type.match(/presentation/g)) {
                                    if (track.duration > duration) {
                                        duration = track.duration;
                                    }
                                    videoSources.presentation.push({
                                        src: track.url,
                                        type: track.mimetype,
                                        typemh: track.type
                                    });
                                }
                            }
                        });
                    }
                    if (mediaInfo.attachments) {
                        $(mediaInfo.attachments).each(function (i, attachment) {
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

    // init Event
    Engage.log("Video: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginVideoVideoJS');
    Engage.log('Video: Relative plugin path: "' + relative_plugin_path + '"');

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function () {
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    // load video.js lib
    require([relative_plugin_path + videoPath], function (videojs) {
        Engage.log("Video: Lib video loaded");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    // load synchronize.js lib
    require([relative_plugin_path + synchronizePath], function (videojs) {
        Engage.log("Video: Lib synchronize loaded");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function () {
        Engage.log("Video: Plugin load done");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    return plugin;
});
