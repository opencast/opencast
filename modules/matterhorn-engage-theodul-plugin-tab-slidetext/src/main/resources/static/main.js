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
    var PLUGIN_NAME = "Slide text";
    var PLUGIN_TYPE = "engage_tab";
    var PLUGIN_VERSION = "0.1",
        PLUGIN_TEMPLATE = "template.html",
        PLUGIN_TEMPLATE_MOBILE = "template.html",
        PLUGIN_TEMPLATE_EMBED = "template.html",
        PLUGIN_STYLES = [
            "style.css"
        ],
        PLUGIN_STYLES_MOBILE = [
            "style.css"
        ],
        PLUGIN_STYLES_EMBED = [
            "style.css"
        ];

    var plugin;
    var events = {
        segmentMouseover: new Engage.Event("Segment:mouseOver", "the mouse is over a segment", "both"),
        segmentMouseout: new Engage.Event("Segment:mouseOut", "the mouse is off a segment", "both"),
        seek: new Engage.Event("Video:seek", "seek video to a given position in seconds", "trigger"),
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "", "handler")
    };

    // desktop, embed and mobile logic
    switch (Engage.model.get("mode")) {
        case "mobile":
            plugin = {
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES_MOBILE,
                template: PLUGIN_TEMPLATE_MOBILE,
                events: events,
                timeStrToSeconds: timeStrToSeconds
            };
            break;
        case "embed":
            plugin = {
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES_EMBED,
                template: PLUGIN_TEMPLATE_EMBED,
                events: events,
                timeStrToSeconds: timeStrToSeconds
            };
            break;
            // fallback to desktop/default mode
        case "desktop":
        default:
            plugin = {
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES,
                template: PLUGIN_TEMPLATE,
                events: events,
                timeStrToSeconds: timeStrToSeconds
            };
            break;
    }

    /* change these variables */

    /* don't change these variables */
    var TEMPLATE_TAB_CONTENT_ID = "engage_slidetext_tab_content";
    var html_snippet_id = "engage_slidetext_tab_content";
    var id_segmentNo = "tab_slidetext_segment_";
    var mediapackageChange = "change:mediaPackage";
    var initCount = 2;

    /**
     * Segment
     *
     * @param time
     * @param image_url
     */
    var Segment = function(time, image_url, text) {
        this.time = time;
        this.image_url = image_url;
        this.text = text;
    };

    /**
     * Returns the input time in milliseconds
     *
     * @param data data in the format ab:cd:ef
     * @return time from the data in milliseconds
     */
    function getTimeInMilliseconds(data) {
        if ((data !== undefined) && (data !== null) && (data != 0) && (data.length) && (data.indexOf(':') != -1)) {
            var values = data.split(':');
            // when the format is correct
            if (values.length == 3) {
                // try to convert to numbers
                var val0 = values[0] * 1;
                var val1 = values[1] * 1;
                var val2 = values[2] * 1;
                // check and parse the seconds
                if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2)) {
                    // convert hours, minutes and seconds to milliseconds
                    val0 *= 60 * 60 * 1000; // 1 hour = 60 minutes = 60 * 60 Seconds = 60 * 60 * 1000 milliseconds
                    val1 *= 60 * 1000; // 1 minute = 60 seconds = 60 * 1000 milliseconds
                    val2 *= 1000; // 1 second = 1000 milliseconds
                    return val0 + val1 + val2;
                }
            }
        }
        return 0;
    }

    /**
     * timeStrToSeconds
     *
     * @param timeStr
     */
    function timeStrToSeconds(timeStr) {
        var elements = timeStr.match(/([0-9]{2})/g);
        return parseInt(elements[0], 10) * 3600 + parseInt(elements[1], 10) * 60 + parseInt(elements[2], 10);
    }

    var SlidetextTabView = Backbone.View.extend({
        initialize: function(mediaPackageModel, template) {
            this.setElement($(plugin.container)); // every plugin view has it's own container associated with it
            this.model = mediaPackageModel;
            this.template = template;
            // bind the render function always to the view
            _.bindAll(this, "render");
            // listen for changes of the model and bind the render function to this
            this.model.bind("change", this.render);
        },
        render: function() {
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
                                if (getTimeInMilliseconds(time[0]) == parseInt(segmentInformation[i].time)) {
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
                segments: segments
            };
            // compile template and load into the html
            this.$el.html(_.template(this.template, tempVars));
            if (segments && (segments.length > 0)) {
                Engage.log("Tab:Slidetext: " + segments.length + " segments are available.");
                $.each(segments, function(i, v) {
                    $("#" + id_segmentNo + i).click(function(e) {
                        e.preventDefault();
                        var time = parseInt(timeStrToSeconds(v.time));
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
    });

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (plugin.inserted === true) {
            // create a new view with the media package model and the template
            new SlidetextTabView(Engage.model.get("mediaPackage"), plugin.template);
        }
    }

    // init event
    Engage.log("Tab:Slidetext: Init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginTabSlidetext');

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

    return plugin;
});
