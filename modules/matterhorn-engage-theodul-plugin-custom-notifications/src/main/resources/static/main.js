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
 * 
 */
/*jslint browser: true, nomen: true*/
/*global define*/
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function(require, $, _, Backbone, Engage) {
    "use strict";
    var PLUGIN_NAME = "Engage Custom Notifications",
            PLUGIN_TYPE = "engage_custom",
            PLUGIN_VERSION = "0.1",
            PLUGIN_TEMPLATE = "none",
            PLUGIN_STYLES = [
		"lib/alertify/alertify.core.css",
		"lib/alertify/alertify.default.css"
	    ];
    var plugin = {
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES,
        template: PLUGIN_TEMPLATE,
        events : {
          plugin_load_done : new Engage.Event("Core:plugin_load_done", "when the core loaded the event successfully", "handler"),
          ready : new Engage.Event("Video:ready", "all videos loaded successfully", "handler"),
          buffering : new Engage.Event("Video:buffering", "buffering a video", "handler"),
          bufferedAndAutoplaying : new Engage.Event("Video:bufferedAndAutoplaying", "buffering successful, was playing, autoplaying now", "handler"),
          bufferedButNotAutoplaying : new Engage.Event("Video:bufferedButNotAutoplaying", "buffering successful, was not playing, not autoplaying now", "handler")
	}
    };

    /* change these variables */
    var alertifyMessageDelay = 5000; // ms
    var alertifyDisplayDatetime = false;
    var alertifyPath = "lib/alertify/alertify";

    /* don't change these variables */
    var alertify;
    var initCount = 2;
    var videoLoaded = false;
    var videoBuffering = false;

    /* format today's date */
    Date.prototype.today = function () { 
	return ((this.getDate() < 10) ? "0" : "") + this.getDate() + "." + (((this.getMonth()+1) < 10) ? "0" : "") + (this.getMonth() + 1) + "." + this.getFullYear();
    }

    /* format current time */
    Date.prototype.timeNow = function () {
	return ((this.getHours() < 10) ? "0" : "") + this.getHours() + ":" + ((this.getMinutes() < 10) ? "0" : "") + this.getMinutes() + ":" + ((this.getSeconds() < 10) ? "0" : "") + this.getSeconds();
    }
    
    /**
     * Format the current date and time
     *
     * @return a formatted current date and time string
     */
    function getCurrentDateTime() {
	var date = new Date(); 
	var datetime = date.today() + ", " + date.timeNow();

	return datetime;
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
     * Initializes the plugin
     */
    function initPlugin() {
	alertify.init();
	alertify.set({ delay : alertifyMessageDelay });

	alertify.error(getAlertifyMessage("The video is now being loaded. Please wait a moment."));

	Engage.on(plugin.events.ready, function(callback) {
	    if(!videoLoaded) {
		videoLoaded = true;
		alertify.success(getAlertifyMessage("The video has been loaded successfully."));
	    }
	});
	Engage.on(plugin.events.buffering, function(callback) {
	    if(!videoBuffering) {
		videoBuffering = true;
		alertify.success(getAlertifyMessage("The video is currently buffering. Please wait a moment."));
	    }
	});
	Engage.on(plugin.events.bufferedAndAutoplaying, function(callback) {
	    if(videoBuffering) {
		videoBuffering = false;
		alertify.success(getAlertifyMessage("The video has been buffered successfully and is now autoplaying."));
	    }
	});
	Engage.on(plugin.events.bufferedButNotAutoplaying, function(callback) {
	    if(videoBuffering) {
		videoBuffering = false;
		alertify.success(getAlertifyMessage("The video has been buffered successfully."));
	    }
	});
    }

    // init event
    Engage.log("Notifications: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginCustomNotifications');
    Engage.log('Notifications: relative plugin path ' + relative_plugin_path);

    // load alertify.js lib
    require([relative_plugin_path + alertifyPath], function(_alertify) {
        Engage.log("Notifications: Loading 'alertify.js' done");
	alertify = _alertify;
        initCount -= 1;
        if (initCount == 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done, function() {
        Engage.log("Notifications: Plugin load done");
        initCount -= 1;
        if (initCount == 0) {
            initPlugin();
        }
    });

    return plugin;
});
