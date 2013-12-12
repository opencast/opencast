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
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function(require, $, _, Backbone, Engage) {
    "use strict"; // strict mode in all our application
    var PLUGIN_NAME = "Engage Controls",
            PLUGIN_TYPE = "engage_controls",
            PLUGIN_VERSION = "0.1",
            PLUGIN_TEMPLATE = "template.html",
            PLUGIN_STYLES = [
        "style.css",
        "js/bootstrap/css/bootstrap.css",
        "js/bootstrap/css/bootstrap-responsive.css",
        "js/jqueryui/themes/base/jquery-ui.css"
    ];
    var plugin = {
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES,
        template: PLUGIN_TEMPLATE
    };
    var plugin_path = "";

    var initCount = 4; //init resource count
    var isPlaying = false;
    var isSliding = false;
    var isMuted = false;

    var class_expand_button = "expand_button";
    var class_expanded_content = "expanded_content";
    var class_pulldown_image = "pulldown_image";
    var id_engage_controls = "engage_controls";
    var id_slider = "slider";
    var id_volume = "volume";
    var id_volumeIcon = "volumeIcon";
    var id_playpause_controls = "playpause_controls";
    var id_fullscreen_button = "fullscreen_button";
    var id_backward_button = "backward_button";
    var id_forward_button = "forward_button";
    var id_navigation_time = "navigation_time";
    var id_navigation_time_current = "navigation_time_current";
    var id_play_button = "play_button";
    var id_pause_button = "pause_button";
    var id_unmuted_button = "unmuted_button";
    var id_muted_button = "muted_button";

    var ControlsView = Backbone.View.extend({
        el: $("#" + id_engage_controls), // every view has an element associated with it
        initialize: function(videoDataModel, template, plugin_path) {
            this.setElement($(plugin.container)); // every plugin view has it's own container associated with it
            this.model = videoDataModel;
            this.template = template;
            this.pluginPath = plugin_path;
            // bind the render function always to the view
            _.bindAll(this, "render");
            // listen for changes of the model and bind the render function to this
            this.model.bind("change", this.render);
            this.render();
        },
        render: function() {
            var duration = this.model.get("duration");
            // format values
            var tempVars = {
                plugin_path: this.pluginPath,
                startTime: formatSeconds(0),
                duration: (duration ? formatSeconds(duration / 1000) : formatSeconds(0)),
                logoLink: ""
            };
            // compile template and load into the html
            this.$el.html(_.template(this.template, tempVars));
            initControlsEvents();
        }
    });

    /**
     * @description Returns the Input Time in Milliseconds
     * @param data Data in the Format ab:cd:ef
     * @return Time from the Data in Milliseconds
     */
    function getTimeInMilliseconds(data) {
        if ((data !== undefined)
                && (data !== null)
                && (data != 0)
                && (data.length)
                && (data.indexOf(':') != -1)) {
            var values = data.split(':');
            // If the Format is correct
            if (values.length == 3) {
                // Try to convert to Numbers
                var val0 = values[0] * 1;
                var val1 = values[1] * 1;
                var val2 = values[2] * 1;
                // Check and parse the Seconds
                if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2)) {
                    // Convert Hours, Minutes and Seconds to Milliseconds
                    val0 *= 60 * 60 * 1000; // 1 Hour = 60 Minutes = 60 * 60 Seconds = 60 * 60 * 1000 Milliseconds
                    val1 *= 60 * 1000; // 1 Minute = 60 Seconds = 60 * 1000 Milliseconds
                    val2 *= 1000; // 1 Second = 1000 Milliseconds
                    // Add the Milliseconds and return it
                    return val0 + val1 + val2;
                }
            }
        }
        return 0;
    }

    /**
     * @description Returns formatted Seconds
     * @param seconds Seconds to format
     * @return formatted Seconds
     */
    function formatSeconds(seconds) {
        if (!seconds) {
            seconds = 0;
        }
        seconds = (seconds < 0) ? 0 : seconds;
        var result = "";
        if (parseInt(seconds / 3600) < 10) {
            result += "0";
        }
        result += parseInt(seconds / 3600);
        result += ":";
        if ((parseInt(seconds / 60) - parseInt(seconds / 3600) * 60) < 10) {
            result += "0";
        }
        result += parseInt(seconds / 60) - parseInt(seconds / 3600) * 60;
        result += ":";
        if (seconds % 60 < 10) {
            result += "0";
        }
        result += seconds % 60;
        if (result.indexOf(".") != -1) {
            result = result.substring(0, result.lastIndexOf(".")); // get rid of the .ms
        }
        return result;
    }

    function disable(id) {
        $("#" + id).attr("disabled", "disabled");
    }

    function greyOut(id) {
        $("#" + id).animate({opacity: 0.5});
    }

    function initControlsEvents() {
        // disable not used buttons
        disable(id_backward_button);
        disable(id_forward_button);
        greyOut(id_backward_button);
        greyOut(id_forward_button);
        disable(id_navigation_time);
        $("#" + id_navigation_time_current).keyup(function(e) {
            // pressed enter
            if (e.keyCode == 13) {
                var time = getTimeInMilliseconds($(this).val()) / 1000;
                var duration = Engage.model.get("videoDataModel").get("duration");
                if (duration && (time <= duration)) {
                    var videoDisplay = Engage.model.get("videoDataModel").get("ids")[0];
                    videojs(videoDisplay).currentTime(time);
                }
            }
        });

        $("#" + id_slider).slider({
            range: "min",
            min: 0,
            max: 1000,
            value: 0
        });

        $("#" + id_volume).slider({
            range: "min",
            min: 1,
            max: 100,
            value: 100,
            change: function(event, ui) {
                Engage.trigger("Video:setVolume", (ui.value) / 100);
            }
        });

        $("#" + id_volumeIcon).click(function() {
            if (isMuted) {
                Engage.trigger("Video:unmuted");
            } else {
                Engage.trigger("Video:muted");
            }
        });

        $("#" + id_playpause_controls).click(function() {
            if (isPlaying) {
                Engage.trigger("Video:pause");
            } else {
                Engage.trigger("Video:play");
            }
        });

        $("." + class_expand_button).click(function() {
            $("." + class_expanded_content).slideToggle("fast");
            $("." + class_pulldown_image).toggleClass("rotate180");
        });

        $("#" + id_fullscreen_button).click(function() {
            var isInFullScreen = document.fullScreen ||
                    document.mozFullScreen ||
                    document.webkitIsFullScreen;
            // just trigger the go event
            if (!isInFullScreen) {
                Engage.trigger("Video:goFullscreen");
            }
        });

        // slider events
        $("#" + id_slider).on("slidestart", function(event, ui) {
            isSliding = true;
            Engage.trigger("Slider:start", ui.value);
        });
        $("#" + id_slider).on("slidestop", function(event, ui) {
            isSliding = false;
            Engage.trigger("Slider:stop", ui.value);
        });
        $("#" + id_volume).on("slidestop", function(event, ui) {
            Engage.trigger("Video:unmuted");
        });
    }

    function getVolume() {
        if (isMuted) {
            return 0;
        } else {
            var vol = $("#" + id_volume).slider("option", "value");
            return vol;
        }
    }

    //local function
    function initPlugin() {

      new ControlsView(Engage.model.get("videoDataModel"), plugin.template, plugin.pluginPath);

        Engage.on("Video:play", function() {
            $("#" + id_play_button).hide();
            $("#" + id_pause_button).show();
            isPlaying = true;
        });
        Engage.on("Video:pause", function() {
            $("#" + id_play_button).show();
            $("#" + id_pause_button).hide();
            isPlaying = false;
        });
        Engage.on("Video:muted", function() {
            $("#" + id_unmuted_button).hide();
            $("#" + id_muted_button).show();
            isMuted = true;
            Engage.trigger("Video:setVolume", 0);
        });
        Engage.on("Video:unmuted", function() {
            $("#" + id_unmuted_button).show();
            $("#" + id_muted_button).hide();
            isMuted = false;
            Engage.trigger("Video:setVolume", getVolume());
        });
        Engage.on("Video:fullscreenChange", function() {
            var isInFullScreen = document.fullScreen ||
                    document.mozFullScreen ||
                    document.webkitIsFullScreen;
            // just trigger the cancel event
            if (!isInFullScreen) {
                Engage.trigger("Video:cancelFullscreen");
            }
        });

        Engage.on("Video:timeupdate", function(currentTime) {
            // set slider
            var duration = Engage.model.get("videoDataModel").get("duration");
            if (!isSliding && duration) {
                var normTime = (currentTime / (duration / 1000)) * 1000;
                $("#" + id_slider).slider("option", "value", normTime);
                if (!$("#" + id_navigation_time_current).is(":focus")) {
                    // set time
                    $("#" + id_navigation_time_current).val(formatSeconds(currentTime));
                }
            }
        });
        Engage.on("Video:ended", function() {
            Engage.trigger("Video:pause");
        });
    }

    //local logic

    //Init Event
    Engage.log("Controls: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginControls');
    Engage.log('Controls: relative plugin path ' + relative_plugin_path);
    //Load other needed JS stuff with Require
    require([relative_plugin_path + 'js/bootstrap/js/bootstrap'], function() {
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });
    require([relative_plugin_path + 'js/jqueryui/jquery-ui.min'], function() {
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });
    
    Engage.model.on("change:videoDataModel", function() {
      initCount -= 1;
      if (initCount === 0) {
          initPlugin();
      }      
    });

    //All plugins loaded lets do some stuff
    Engage.on("Core:plugin_load_done", function() {
        Engage.log("Controls: receive plugin load done");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    return plugin;
});