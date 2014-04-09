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
    "use strict"; // strict mode in all our application
    var PLUGIN_NAME = "${plugin_name}";
    var PLUGIN_TYPE = "engage_${plugin_type}";
    var PLUGIN_VERSION = "${plugin_version}";
    var PLUGIN_TEMPLATE = "template.html";
    var PLUGIN_STYLES = ["style.css"];
    var plugin = {
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES,
        template: PLUGIN_TEMPLATE,
        pluginPath: "", //filled after core procession, see core reference
        container: "", //filled after core procession, see core reference
        events : {
          //put here your events
        }
    };
    
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
