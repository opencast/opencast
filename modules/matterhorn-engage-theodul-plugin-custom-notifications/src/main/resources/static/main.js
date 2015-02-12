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
define(["require", "jquery", "backbone", "engage/core"], function(require, $, Backbone, Engage) {
    "use strict";

    var insertIntoDOM = true;
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
        isAudioOnly: new Engage.Event("Video:isAudioOnly", "whether it's audio only or not", "handler"),
        audioCodecNotSupported: new Engage.Event("Video:audioCodecNotSupported", "when the audio codec seems not to be supported by the browser", "handler")
    };

    var isDesktopMode = false;
    var isEmbedMode = false;
    var isMobileMode = false;

    // desktop, embed and mobile logic
    switch (Engage.model.get("mode")) {
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
        case "desktop":
        default:
            plugin = {
                insertIntoDOM: insertIntoDOM,
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

    /* don't change these variables */
    var Utils;
    var isAudioOnly = false;
    var alertify;
    var mediapackageError = false;
    var codecError = false;
    var initCount = 3;
    var videoLoaded = false;
    var videoLoadMsgDisplayed = false;
    var videoBuffering = false;
    var translations = new Array();
    var locale = "en";
    var dateFormat = "MMMM Do YYYY, h:mm:ss a";

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath("EngagePluginCustomNotifications").replace(/(\.\.\/)/g, "");
        var jsonstr = window.location.origin + "/engage/theodul/" + path; // this solution is really bad, fix it...

        if (language == "de") {
            Engage.log("Notifications: Chosing german translations");
            jsonstr += "language/de.json";
        } else { // No other languages supported, yet
            Engage.log("Notifications: Chosing english translations");
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

    /**
     * Format a message for alertify
     *
     * @param msg message to format
     * @return the formatted message
     */
    function getAlertifyMessage(msg) {
        return (alertifyDisplayDatetime ? (Utils.getCurrentDateTime(locale) + ": ") : "") + msg;
    }

    /**
     * Initialize the plugin
     */
    function initPlugin() {
        alertify.init();
        alertify.set({
            delay: alertifyMessageDelay
        });

        window.setTimeout(function() {
            if (!videoLoaded && !mediapackageError && !codecError) {
                videoLoadMsgDisplayed = true;
                if (!isAudioOnly) {
                    alertify.error(getAlertifyMessage(translate("error_videoLoading", "The video is loading. Please wait a moment.")));
                } else {
                    alertify.error(getAlertifyMessage(translate("error_audioLoading", "The audio is loading. Please wait a moment.")));
                }
            }
        }, alertifyVideoLoadMessageThreshold);
        Engage.on(plugin.events.isAudioOnly.getName(), function(audio) {
            isAudioOnly = audio;
        });
        Engage.on(plugin.events.ready.getName(), function() {
            if (!videoLoaded && videoLoadMsgDisplayed && !mediapackageError && !codecError) {
                if (!isAudioOnly) {
                    alertify.success(getAlertifyMessage(translate("msg_videoLoadedSuccessfully", "The video has been loaded successfully.")));
                } else {
                    alertify.success(getAlertifyMessage(translate("msg_audioLoadedSuccessfully", "The audio has been loaded successfully.")));
                }
            }
            videoLoaded = true;
        });
        Engage.on(plugin.events.buffering.getName(), function() {
            if (!videoBuffering && !mediapackageError && !codecError) {
                videoBuffering = true;
                alertify.success(getAlertifyMessage(translate("msg_videoBuffering", "The video is currently buffering. Please wait a moment.")));
            }
        });
        Engage.on(plugin.events.bufferedAndAutoplaying.getName(), function() {
            if (videoBuffering && !mediapackageError && !codecError) {
                videoBuffering = false;
                alertify.success(getAlertifyMessage(translate("msg_videoBufferedSuccessfullyAndAutoplaying", "The video has been buffered successfully and is now autoplaying.")));
            }
        });
        Engage.on(plugin.events.bufferedButNotAutoplaying.getName(), function() {
            if (videoBuffering && !mediapackageError && !codecError) {
                videoBuffering = false;
                alertify.success(getAlertifyMessage(translate("msg_videoBufferedSuccessfully", "The video has been buffered successfully.")));
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
            alertify.error(getAlertifyMessage(translate("error", "Error") + ": " + msg));
        });
        Engage.on(plugin.events.audioCodecNotSupported.getName(), function() {
            codecError = true;
            alertify.error(getAlertifyMessage(translate("error_AudioCodecNotSupported", "Error: The audio codec is not supported by this browser")));
        });
    }

    // init event
    Engage.log("Notifications: Init");
    var relative_plugin_path = Engage.getPluginPath("EngagePluginCustomNotifications");

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("Notifications: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load utils class
    require([relative_plugin_path + "utils"], function(utils) {
        Engage.log("Notifications: Utils class loaded");
        Utils = new utils();
        initTranslate(Utils.detectLanguage(), function() {
            Engage.log("Notifications: Successfully translated.");
            locale = translate("value_locale", locale);
            dateFormat = translate("value_dateFormatFull", dateFormat);
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

    // load alertify lib
    require([relative_plugin_path + alertifyPath], function(_alertify) {
        Engage.log("Notifications: Lib alertify loaded");
        alertify = _alertify;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    return plugin;
});
