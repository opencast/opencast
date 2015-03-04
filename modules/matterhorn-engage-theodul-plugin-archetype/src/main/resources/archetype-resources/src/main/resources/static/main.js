/**
 *  Copyright 2009-2011 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
/*jslint browser: true, nomen: true*/
/*global define*/
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function(require, $ , _, Backbone, Engage) {
    "use strict";

    var PLUGIN_NAME = "${plugin_name}";
    var PLUGIN_TYPE = "engage_${plugin_type}";
    var PLUGIN_VERSION = "${plugin_version}";
    var PLUGIN_TEMPLATE_DESKTOP = "templates/desktop.html";
    var PLUGIN_STYLES_DESKTOP = [
	"styles/desktop.css"
    ];
    var plugin;
    var events = {
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "", "handler")
        // put your events here
    }

    var isDesktopMode = false;
    var isEmbedMode = false;
    var isMobileMode = false;

    // desktop, embed and mobile logic
    switch (Engage.model.get("mode")) {
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
    
#if( $plugin_type == "custom" )
#include("templates/customPlugin.vtl")
#elseif( $plugin_type == "controls" )
#include("templates/controlsPlugin.vtl")
#elseif( $plugin_type == "timeline" )
#include("templates/timelinePlugin.vtl")
#elseif( $plugin_type == "video" )
#include("templates/videoPlugin.vtl")
#elseif( $plugin_type == "description" )
#include("templates/labelPlugin.vtl")
#elseif( $plugin_type == "tab" )
#include("templates/tabPlugin.vtl")
#end
