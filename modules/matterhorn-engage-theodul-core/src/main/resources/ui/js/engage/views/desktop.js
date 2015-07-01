/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*jslint browser: true, nomen: true*/
/*global define, CustomEvent*/
define(["jquery", "engage/core", "engage/models/engage", "engage/tab_logic"], function($, EngageCore, EngageModel, EngageTabLogic) {
    "use strict";

    /* change these variables */
    var id_engage_timeline_expand_btn = "engage_timeline_expand_btn";
    var id_engage_timeline_plugin = "engage_timeline_plugin";
    var id_engage_timeline_expand_btn_img = "engage_timeline_expand_btn_img";
    var id_engage_controls = "engage_controls";
    var id_engage_controls_secondIfBottom = "engage_controls_second"; // second container for controls above the video if player controls are below
    var id_engage_video = "engage_video";
    var id_engage_tab = "engage_tab";
    var id_engage_tab_split1 = "engage_";
    var id_engage_tab_split2 = "_tab";
    var id_engage_tab_nav = "engage_tab_nav";
    var id_engage_tab_content = "engage_tab_content";
    var id_engage_description = "engage_description";
    var id_engage_timeline = "engage_timeline";
    var class_engage_timeline_expand_btn_rotate180 = "engage_timeline_expand_btn_rotate180";
    var class_tab_pane = "tab-pane";

    /* don't change these variables */
    var timelineplugin_opened = "Engage:timelineplugin_opened";
    var timelineplugin_closed = "Engage:timelineplugin_closed";

    var isControlsPlugin = function(str) {
        return str == id_engage_controls;
    }

    var isVideoPlugin = function(str) {
        return str == id_engage_video;
    }

    var isTabPlugin = function(str) {
        return str == id_engage_tab;
    }

    var isDescriptionPlugin = function(str) {
        return str == id_engage_description;
    }

    var isTimelinePlugin = function(str) {
        return str == id_engage_timeline;
    }

    var removeControls = function() {
        $("#" + id_engage_controls).detach();
    }

    var removeVideo = function() {
        $("#" + id_engage_video).detach();
    }

    var removeTab = function() {
        $("#" + id_engage_tab_nav + ", #" + id_engage_tab_content).detach();
    }

    var removeDescription = function() {
        $("#" + id_engage_description).detach();
    }

    var removeTimeline = function() {
        $("#" + id_engage_timeline_expand_btn + ", #" + id_engage_timeline_plugin).detach();
    }

    /*
     * init logic function
     */
    var initDesktopView = function() {
        // build timeline plugins
        $("#" + id_engage_timeline_expand_btn).click(function() {
            $("#" + id_engage_timeline_plugin).slideToggle("fast");
            $("#" + id_engage_timeline_expand_btn_img).toggleClass(class_engage_timeline_expand_btn_rotate180);
            if ($("#" + id_engage_timeline_expand_btn_img).hasClass(class_engage_timeline_expand_btn_rotate180)) {
                EngageCore.trigger(timelineplugin_opened);
            } else {
                EngageCore.trigger(timelineplugin_closed);
            }
        });
    }

    /*
     * logic to insert a plugin with name and type to the player in desktop mode
     */
    var insertPluginToDOM = function(plugin, plugin_name, translationData) {
        // switch plugin type to insert the plugin to the right DOM element and execute custom view code
        switch (plugin.type) {
            case id_engage_controls:
                $("#" + id_engage_controls).html(plugin.templateProcessed);
                plugin.inserted = true;
                plugin.container = "#" + id_engage_controls;
                plugin.containerSecondIfBottom = "#" + id_engage_controls_secondIfBottom;
                break;
            case id_engage_video:
                $("#" + id_engage_video).html(plugin.templateProcessed);
                plugin.inserted = true;
                plugin.container = "#" + id_engage_video;
                break;
            case id_engage_tab:
                var tab_ref = plugin.name.replace(/ /g, "_");
                // insert tab navigation line
                var tabNavTag = "<li><a href=\"#" + id_engage_tab_split1 + tab_ref + id_engage_tab_split2 + "\"><div id=\"engage_tab_" + plugin.name.replace(/\s/g,"_") + "\">" + (((translationData != null) && (translationData[plugin_name] != undefined)) ? translationData[plugin_name] : plugin.name) + "</div></a></li>";
                $("#" + id_engage_tab_nav).prepend(tabNavTag);
                // insert tab content
                var tabTag = "<div class=\"" + class_tab_pane + "\" id=\"" + id_engage_tab_split1 + tab_ref + id_engage_tab_split2 + "\">" + plugin.templateProcessed + "</div>";
                $("#" + id_engage_tab_content).prepend(tabTag);
                plugin.inserted = true;
                plugin.container = "#" + id_engage_tab_split1 + tab_ref + id_engage_tab_split2;
                break;
            case id_engage_description:
                $("#" + id_engage_description).html(plugin.templateProcessed);
                plugin.inserted = true;
                plugin.container = "#" + id_engage_description;
                break;
            case id_engage_timeline:
                $("#" + id_engage_timeline_plugin).html(plugin.templateProcessed);
                plugin.inserted = true;
                plugin.container = "#" + id_engage_timeline_plugin;
                break;
            default:
                plugin.inserted = false;
                plugin.container = "";
        }
    }

    /*
     * triggered when all plugins have been loaded and inserted into the DOM
     */
    var allPluginsLoadedEvent = function() {
        // add tab sorted tab logic to the view
        EngageTabLogic("tabs", id_engage_tab_nav);
    }

    // public functions for the module
    return {
        isControlsPlugin: isControlsPlugin,
        isVideoPlugin: isVideoPlugin,
        isTabPlugin: isTabPlugin,
        isDescriptionPlugin: isDescriptionPlugin,
        isTimelinePlugin: isTimelinePlugin,
        removeControls: removeControls,
        removeVideo: removeVideo,
        removeTab: removeTab,
        removeDescription: removeDescription,
        removeTimeline: removeTimeline,
        initView: initDesktopView,
        insertPlugin: insertPluginToDOM,
        allPluginsLoaded: allPluginsLoadedEvent
    }
});
