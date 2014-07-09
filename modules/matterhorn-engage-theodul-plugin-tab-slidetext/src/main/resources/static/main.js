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
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "", "handler")
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
            events: events,
            timeStrToSeconds: timeStrToSeconds
        };
        break;
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
    }

    /* change these variables */
    // nothing here...

    /* don't change these variables */
    var TEMPLATE_TAB_CONTENT_ID = "engage_slidetext_tab_content";
    var segments = [];
    var mediapackageChange = "change:mediaPackage";
    var initCount = 2;

    /**
     * Segment
     *
     * @param time
     * @param image_url
     */
    var Segment = function (time, image_url) {
        this.time = time;
        this.image_url = image_url;
    };

    /**
     * timeStrToSeconds
     *
     * @param timeStr
     */
    function timeStrToSeconds(timeStr) {
        var elements = timeStr.match(/([0-9]{2})/g);
        return parseInt(elements[0], 10) * 3600 + parseInt(elements[1], 10) * 60 + parseInt(elements[2], 10);
    }

    /**
     * Initialize the plugin
     */
    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (plugin.inserted === true) {
            Engage.log("TabSlideText: initializing plugin");
            Engage.model.get("mediaPackage").on("change", function () {
                var attachments = this.get("attachments");
                if (attachments) {
                    // extract segments which type is "segment+preview" out of the model
                    $(attachments).each(function (index, attachment) {
                        if (attachment.mimetype && attachment.type && attachment.type.match(/presentation\/segment\+preview/g) && attachment.mimetype.match(/image/g)) {
                            // pull time string out of the ref property
                            // (e.g. "ref": "track:4ea9108d-c1df-4d8e-b729-e7c75c87519e;time=T00:00:00:0F1000")
                            var time = attachment.ref.match(/([0-9]{2}:[0-9]{2}:[0-9]{2})/g);
                            if (time.length > 0) {
                                segments.push(new Segment(time[0], attachment.url));
                            } else {
                                Engage.log("Failure on time evaluation for segment with url: " + attachment.url);
                            }
                        }
                    });
                    // sort segments ascending by time
                    segments.sort(function (a, b) {
                        return new Date("1970/1/1 " + a.time) - new Date("1970/1/1 " + b.time);
                    });
                    // building html snippet for a segment and inject each in the template
                    if (segments.length > 0) {
                        $("#" + TEMPLATE_TAB_CONTENT_ID).empty();
                        $(segments).each(function (index, segment) {
                            var html_snippet = "";
                            var html_snippet_id = "tab_slidetext_segment_" + index;
                            var segment_name = "Segment " + index;
                            html_snippet += "<div class=\"media\" id=\"" + html_snippet_id + "\">";
                            html_snippet += "  <img class=\"media-object pull-left\" src=\"" + segment.image_url + "\" alt=\"" + segment_name + "\">";
                            html_snippet += "  <div class=\"media-body\">";
                            html_snippet += "    <h4 class=\"media-heading\">" + segment_name + "</h4>";
                            html_snippet += "    " + segment.time;
                            html_snippet += "  </div>";
                            html_snippet += "</div>";
                            $("#" + TEMPLATE_TAB_CONTENT_ID).append(html_snippet);

                            // add click handler to each segment (slide)
                            $("#" + html_snippet_id).click(function () {
                                Engage.trigger("Video:seek", timeStrToSeconds(segment.time));
                            });
                        });
                    }
                }
            });
        }
    }

    // init event
    Engage.log("Tab:Slidetext: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginTabSlidetext');
    Engage.log('Tab:Slidetext: Relative plugin path: "' + relative_plugin_path + '"');

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function () {
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function () {
        Engage.log("Tab:Slidetext: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    return plugin;
});
