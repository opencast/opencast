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
    var PLUGIN_NAME = "Description";
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
            // fallback to desktop/default mode
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
            break;
    }

    /* change these variables */
    var class_tabGroupItem = "tab-group-item";

    /* don't change these variables */
    var mediapackageChange = "change:mediaPackage";
    var initCount = 2;

    // parse a date in yyyy-mm-ddThh:mm:ss:timezone format, e.g. 2014-07-26T15:37:00+02:00
    function parseDate(input) {
        if (!input || (input == "")) {
            return input;
        }
        var parts = input.split('T');
        if (parts.length < 2) {
            return input;
        }
        var dateParts = parts[0].split('-');
        if (dateParts.length < 3) {
            return input;
        }
        var timeParts = parts[1].split(':');
        if (timeParts.length < 3) {
            return input;
        }
        var timePartsLast = timeParts[2].split('+');
        if (timeParts.length < 2) {
            return input;
        }
        // new Date(year, month [, day [, hours[, minutes[, seconds[, ms]]]]])
        return new Date(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1], timePartsLast[0]); // note: months are 0-based
    }

    var month_names = new Array("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
    var day_names = new Array("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    function formatDate(date) {
        if (!date || (date == "")) {
            return date;
        }
        var h = (date.getHours() < 10) ? ("0" + date.getHours()) : date.getHours();
        var m = (date.getMinutes() < 10) ? ("0" + date.getMinutes()) : date.getMinutes();
        var s = (date.getSeconds() < 10) ? ("0" + date.getSeconds()) : date.getSeconds();
        return (day_names[date.getDay() - 1] + ", " + month_names[date.getMonth()] + " " + date.getDate() + " " + date.getFullYear() + ", " + h + ":" + m + ":" + s);
    }

    var DescriptionTabView = Backbone.View.extend({
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
            var date = formatDate(parseDate(this.model.get("date")));
            date = (date && (date != "")) ? date : "";
            // format values
            var tempVars = {
                description: this.model.get("description"),
                creator: this.model.get("creator"),
                title: this.model.get("title"),
                series: this.model.get("series"),
                contributor: this.model.get("contributor"),
                date: date
            };
            if (!tempVars.creator) {
                tempVars.creator = "";
            }
            if (!tempVars.description) {
                tempVars.description = "";
            }
            if (!tempVars.title) {
                tempVars.title = "";
            }
            if (!tempVars.series) {
                tempVars.series = "";
            }
            if (!tempVars.contributor) {
                tempVars.contributor = "";
            }
            if (!tempVars.date) {
                tempVars.date = "";
            }
            // compile template and load into the html
            this.$el.html(_.template(this.template, tempVars));
        }
    });

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (plugin.inserted === true) {
            // create a new view with the media package model and the template
            new DescriptionTabView(Engage.model.get("mediaPackage"), plugin.template);
        }
    }

    // init event
    Engage.log("Tab:Description: Init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginTabDescription');
    Engage.log('Tab:Description: Relative plugin path: "' + relative_plugin_path + '"');

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function() {
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("Tab:Description: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    return plugin;
});
