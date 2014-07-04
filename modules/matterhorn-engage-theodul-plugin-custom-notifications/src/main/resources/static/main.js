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
    "use strict"; // strict mode in all our application
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
          ready : new Engage.Event("Video:ready", "all videos loaded successfully", "handler"),
          buffering : new Engage.Event("Video:buffering", "buffering a video", "handler"),
          bufferedAndAutoplaying : new Engage.Event("Video:bufferedAndAutoplaying", "buffering successful, was playing, autoplaying now", "handler"),
          bufferedButNotAutoplaying : new Engage.Event("Video:bufferedButNotAutoplaying", "buffering successful, was not playing, not autoplaying now", "handler")
	}
    };

    var alertify;
    var initCount = 2;
    var videoloaded = false;

    function initPlugin() {
	alertify.error("The video is now being loaded. Please wait a moment.");

	Engage.on(plugin.events.ready, function(callback) {
	    if(!videoloaded) {
		videoloaded = true;
		alertify.success("The video has been loaded successfully.");
	    }
	});
	Engage.on(plugin.events.buffering, function(callback) {
	    alertify.success("The video is currently buffering. Please wait a moment.");
	});
	Engage.on(plugin.events.bufferedAndAutoplaying, function(callback) {
	    alertify.success("The video has been buffered successfully and is now autoplaying.");
	});
	Engage.on(plugin.events.bufferedButNotAutoplaying, function(callback) {
	    alertify.success("The video has been buffered successfully.");
	});
    }

    // init Event
    Engage.log("Notifications: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginCustomNotifications');
    Engage.log('Notifications: relative plugin path ' + relative_plugin_path);

    // load alertify.js lib
    require([relative_plugin_path + "lib/alertify/alertify"], function(_alertify) {
        Engage.log("Notifications: Load alertify.js done");
	alertify = _alertify;
        initCount -= 1;
        if (initCount == 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on("Core:plugin_load_done", function() {
        Engage.log("Notifications: Plugin load done");
        initCount -= 1;
        if (initCount == 0) {
            initPlugin();
        }
    });

    return plugin;
});
