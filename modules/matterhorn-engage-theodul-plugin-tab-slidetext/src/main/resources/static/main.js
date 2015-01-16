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
define(["require", "jquery", "underscore", "backbone", "engage/core"], function(require, $, _, Backbone, Engage) {
    "use strict";

    var PLUGIN_NAME = "Slide text";
    var PLUGIN_TYPE = "engage_tab";
    var PLUGIN_VERSION = "1.0";
    var PLUGIN_TEMPLATE_DESKTOP = "templates/desktop.html";
    var PLUGIN_TEMPLATE_MOBILE = "templates/mobile.html";
    var PLUGIN_TEMPLATE_EMBED = "templates/embed.html";
    var PLUGIN_STYLES_DESKTOP = [
        "styles/desktop.css"
    ];
    var PLUGIN_STYLES_MOBILE = [
        "styles/mobile.css"
    ];
    var PLUGIN_STYLES_EMBED = [
        "styles/embed.css"
    ];

    var plugin;
    var events = {
        segmentMouseover: new Engage.Event("Segment:mouseOver", "the mouse is over a segment", "both"),
        segmentMouseout: new Engage.Event("Segment:mouseOut", "the mouse is off a segment", "both"),
        seek: new Engage.Event("Video:seek", "seek video to a given position in seconds", "trigger"),
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "", "handler"),
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
                styles: PLUGIN_STYLES_DESKTOP,
                template: PLUGIN_TEMPLATE_DESKTOP,
                events: events
            };
            isDesktopMode = true;
            break;
    }

    /* don't change these variables */
    var Utils;
    var TEMPLATE_TAB_CONTENT_ID = "engage_slidetext_tab_content";
    var html_snippet_id = "engage_slidetext_tab_content";
    var id_segmentNo = "tab_slidetext_segment_";
    var mediapackageChange = "change:mediaPackage";
    var initCount = 4;
    var mediapackageError = false;
    var translations = new Array();
    var Segment;

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath("EngagePluginTabSlidetext").replace(/(\.\.\/)/g, "");
        var jsonstr = window.location.origin + "/engage/theodul/" + path; // this solution is really bad, fix it...

        if (language == "de") {
            Engage.log("Tab:Slidetext: Chosing german translations");
            jsonstr += "language/de.json";
        } else { // No other languages supported, yet
            Engage.log("Tab:Slidetext: Chosing english translations");
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

    var SlidetextTabView = Backbone.View.extend({
        initialize: function(mediaPackageModel, template) {
            this.setElement($(plugin.container)); // every plugin view has it"s own container associated with it
            this.model = mediaPackageModel;
            this.template = template;
            // bind the render function always to the view
            _.bindAll(this, "render");
            // listen for changes of the model and bind the render function to this
            this.model.bind("change", this.render);
        },
        render: function() {
            if (!mediapackageError) {
                var segments = [];
                var segmentInformation = this.model.get("segments");
                var attachments = this.model.get("attachments");
                if (segmentInformation && attachments && (segmentInformation.length > 0) && (attachments.length > 0)) {
                    // extract segments which type is "segment+preview" out of the model
                    $(attachments).each(function(index, attachment) {
                        if (attachment.mimetype && attachment.type && attachment.type.match(/presentation\/segment\+preview/g) && attachment.mimetype.match(/image/g)) {
                            // pull time string out of the ref property
                            // (e.g. "ref": "track:4ea9108d-c1df-4d8e-b729-e7c75c87519e;time=T00:00:00:0F1000")
                            var time = attachment.ref.match(/([0-9]{2}:[0-9]{2}:[0-9]{2})/g);
                            if (time.length > 0) {
                                var si = "No slide text available.";
                                for (var i = 0; i < segmentInformation.length; ++i) {
                                    if (Utils.getTimeInMilliseconds(time[0]) == parseInt(segmentInformation[i].time)) {
                                        si = segmentInformation[i].text;
                                        break;
                                    }
                                }
                                segments.push(new Segment(time[0], attachment.url, si));
                            } else {
                                Engage.log("Tab:Slidetext: Error on time evaluation for segment with url: " + attachment.url);
                            }
                        }
                    });
                    if (segments.length > 0) {
                        // sort segments ascending by time
                        segments.sort(function(a, b) {
                            return new Date("1970/1/1 " + a.time) - new Date("1970/1/1 " + b.time);
                        });
                    }
                }
                var tempVars = {
                    segments: segments,
                    str_segment: translate("segment", "Segment"),
                    str_noSlidesAvailable: translate("noSlidesAvailable", "No slides available.")
                };
                // compile template and load into the html
                this.$el.html(_.template(this.template, tempVars));
                if (segments && (segments.length > 0)) {
                    Engage.log("Tab:Slidetext: " + segments.length + " segments are available.");
                    $.each(segments, function(i, v) {
                        $("#" + id_segmentNo + i).click(function(e) {
                            e.preventDefault();
                            var time = parseInt(Utils.timeStrToSeconds(v.time));
                            if (!isNaN(time)) {
                                Engage.trigger(plugin.events.seek.getName(), time);
                            }
                        });
                        $("#" + id_segmentNo + i).mouseover(function(e) {
                            e.preventDefault();
                            Engage.trigger(plugin.events.segmentMouseover.getName(), i);
                        }).mouseout(function(e) {
                            e.preventDefault();
                            Engage.trigger(plugin.events.segmentMouseout.getName(), i);
                        });
                    });
                }
                Engage.on(plugin.events.segmentMouseover.getName(), function(no) {
                    $("#" + id_segmentNo + no).removeClass("mediaColor").addClass("mediaColor-hover");
                });
                Engage.on(plugin.events.segmentMouseout.getName(), function(no) {
                    $("#" + id_segmentNo + no).removeClass("mediaColor-hover").addClass("mediaColor");
                });
            }
        }
    });

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (plugin.inserted) {
            // create a new view with the media package model and the template
            var slidetextTabView = new SlidetextTabView(Engage.model.get("mediaPackage"), plugin.template);
            Engage.on(plugin.events.mediaPackageModelError.getName(), function(msg) {
                mediapackageError = true;
            });
        }
    }

    // init event
    Engage.log("Tab:Slidetext: Init");
    var relative_plugin_path = Engage.getPluginPath("EngagePluginTabSlidetext");

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function() {
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("Tab:Slidetext: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load segment class
    require([relative_plugin_path + "segment"], function(segment) {
        Engage.log("Tab:Slidetext: Segment class loaded");
        Segment = segment;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load utils class
    require([relative_plugin_path + "utils"], function(utils) {
        Engage.log("Tab:Slidetext: Utils class loaded");
        Utils = new utils();
	plugin.timeStrToSeconds = Utils.timeStrToSeconds;
        initTranslate(Utils.detectLanguage(), function() {
            Engage.log("Tab:Slidetext: Successfully translated.");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        }, function() {
            Engage.log("Notifications: Error translating...");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });
    });

    return plugin;
});
