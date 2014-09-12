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
define(["require", "jquery", "underscore", "backbone", "engage/engage_core", "moment"], function(require, $, _, Backbone, Engage, Moment) {
    "use strict";
    var PLUGIN_NAME = "Engage Custom Notifications";
    var PLUGIN_TYPE = "engage_custom";
    var PLUGIN_VERSION = "1.0";
    var PLUGIN_TEMPLATE = "none";
    var PLUGIN_TEMPLATE_MOBILE = "none";
    var PLUGIN_TEMPLATE_EMBED = "none";
    var PLUGIN_STYLES = [
        "lib/alertify/alertify.css",
        "lib/alertify/alertify-bootstrap-3.css"
    ];
    var PLUGIN_STYLES_MOBILE = [
        "lib/alertify/alertify.css",
        "lib/alertify/alertify-bootstrap-3.css"
    ];
    var PLUGIN_STYLES_EMBED = [
        "lib/alertify/alertify.css",
        "lib/alertify/alertify-bootstrap-3.css"
    ];

    var plugin;
    var events = {
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "when the core loaded the event successfully", "handler"),
        ready: new Engage.Event("Video:ready", "all videos loaded successfully", "handler"),
        buffering: new Engage.Event("Video:buffering", "buffering a video", "handler"),
        customNotification: new Engage.Event("Notification:customNotification", "a custom message", "handler"),
        customSuccess: new Engage.Event("Notification:customSuccess", "a custom success message", "handler"),
        customError: new Engage.Event("Notification:customError", "an error occurred", "handler"),
        customOKMessage: new Engage.Event("Notification:customOKMessage", "a custom message with an OK button", "handler"),
        bufferedAndAutoplaying: new Engage.Event("Video:bufferedAndAutoplaying", "buffering successful, was playing, autoplaying now", "handler"),
        bufferedButNotAutoplaying: new Engage.Event("Video:bufferedButNotAutoplaying", "buffering successful, was not playing, not autoplaying now", "handler"),
        mediaPackageModelError: new Engage.Event("MhConnection:mediaPackageModelError", "", "handler"),
        isAudioOnly: new Engage.Event("Video:isAudioOnly", "whether it's audio only or not", "handler")
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
    var alertifyMessageDelay = 5000; // ms
    var alertifyVideoLoadMessageThreshold = 2000; // ms
    var alertifyDisplayDatetime = false;
    var alertifyPath = "lib/alertify/alertify";

    /* don"t change these variables */
    var isAudioOnly = false;
    var alertify;
    var mediapackageError = false;
    var initCount = 2;
    var videoLoaded = false;
    var videoLoadMsgDisplayed = false;
    var videoBuffering = false;

    /**
     * Format the current date and time
     *
     * @return a formatted current date and time string
     */
    function getCurrentDateTime() {
        var date = new Date();

        // try to format the date
        if (Moment(date) != null) {
            date = Moment(new Date()).format("MMMM Do YYYY, h:mm:ss a");
        }
        return date;
    }

    /**
     * Format a message for alertify
     *
     * @param msg message to format
     * @return the formatted message
     */
    function getAlertifyMessage(msg) {
        return (alertifyDisplayDatetime ? (getCurrentDateTime() + ": ") : "") + msg;
    }

    /**
     * Initialize the plugin
     */
    function initPlugin() {
        Moment.locale("en", {
            // customizations
        });

        alertify.init();
        alertify.set({
            delay: alertifyMessageDelay
        });

        window.setTimeout(function() {
            if (!videoLoaded && !mediapackageError) {
                videoLoadMsgDisplayed = true;
                if (!isAudioOnly) {
                    alertify.error(getAlertifyMessage("The video is loading. Please wait a moment."));
                } else {
                    alertify.error(getAlertifyMessage("The audio is loading. Please wait a moment."));
                }
            }
        }, alertifyVideoLoadMessageThreshold);

        Engage.on(plugin.events.isAudioOnly.getName(), function(audio) {
            isAudioOnly = true;
        });
        Engage.on(plugin.events.ready.getName(), function() {
            if (!videoLoaded && videoLoadMsgDisplayed && !mediapackageError) {
                if (!isAudioOnly) {
                    alertify.success(getAlertifyMessage("The video has been loaded successfully."));
                } else {
                    alertify.success(getAlertifyMessage("The audio has been loaded successfully."));
                }
            }
            videoLoaded = true;
        });
        Engage.on(plugin.events.buffering.getName(), function() {
            if (!videoBuffering && !mediapackageError) {
                videoBuffering = true;
                alertify.success(getAlertifyMessage("The video is currently buffering. Please wait a moment."));
            }
        });
        Engage.on(plugin.events.bufferedAndAutoplaying.getName(), function() {
            if (videoBuffering && !mediapackageError) {
                videoBuffering = false;
                alertify.success(getAlertifyMessage("The video has been buffered successfully and is now autoplaying."));
            }
        });
        Engage.on(plugin.events.bufferedButNotAutoplaying.getName(), function() {
            if (videoBuffering && !mediapackageError) {
                videoBuffering = false;
                alertify.success(getAlertifyMessage("The video has been buffered successfully."));
            }
        });
        Engage.on(plugin.events.customNotification.getName(), function(msg) {
            alertify.log(getAlertifyMessage(msg));
        });
        Engage.on(plugin.events.customSuccess.getName(), function(msg) {
            alertify.success(getAlertifyMessage(msg));
        });
        Engage.on(plugin.events.customError.getName(), function(msg) {
            alertify.error(getAlertifyMessage(msg));
        });
        Engage.on(plugin.events.customOKMessage.getName(), function(msg) {
            alertify.alert(getAlertifyMessage(msg));
        });
        Engage.on(plugin.events.mediaPackageModelError.getName(), function(msg) {
            mediapackageError = true;
            alertify.error(getAlertifyMessage("Error: " + msg));
        });
    }

    // init event
    Engage.log("Notifications: Init");
    var relative_plugin_path = Engage.getPluginPath("EngagePluginCustomNotifications");

    // load alertify lib
    require([relative_plugin_path + alertifyPath], function(_alertify) {
        Engage.log("Notifications: Lib alertify loaded");
        alertify = _alertify;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("Notifications: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    return plugin;
});

