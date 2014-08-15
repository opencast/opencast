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
    var PLUGIN_NAME = "Timeline Usertracking Statistics";
    var PLUGIN_TYPE = "engage_timeline";
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
    var chartPath = "lib/Chart";

    /* don't change these variables */
    var mediapackageChange = "change:mediaPackage";
    var footprintChange = "change:footprints";
    var videoDataModelChange = "change:videoDataModel";
    var initCount = 5;

	function setSize() {
		$("#engage_timeline_statistics_chart").attr("width", $(window).width() - 40).attr("height", 60).css({
			"width": $(window).width() - 40,
			"height": 60
		});
	}

    var StatisticsTimelineView = Backbone.View.extend({
        initialize: function () {
            this.setElement($(plugin.container)); // every plugin view has it's own container associated with it
            this.videoData = Engage.model.get("videoDataModel");
            this.footprints = Engage.model.get("footprints");
            this.template = plugin.template;
            // bind the render function always to the view
            _.bindAll(this, "render");
            // listen for changes of the model and bind the render function to this
            this.videoData.bind("change", this.render);
            this.footprints.bind("change", this.render);
            this.render();
			var _this = this;
			$(window).resize(function() {
            	_this.render();
			});
        },
        render: function () {
            var tempVars = {
                width: $(window).width() - 40,
                height: "60"
            };
            // compile template and load into the html
            this.$el.html(_.template(this.template, tempVars));

			setSize();

            var duration = this.videoData.get("duration");

            // fill array 
            var data = new Array();
            var cView = 0;
            for (i = 0; i < duration / 1000; i++) {
                _.each(this.footprints, function (element, index, list) {
                    if (this.footprints.at(index).get("position") == i)
                        cView = this.footprints.at(index).get("views");
                }, this);
                data.push([i, cView]);
            }
            var labels = new Array(); // chart label array
            var data = new Array(); // chart data array
            var intvl = (duration / 1000) / 500; // interval length
            var cTime = 0; // current time in process
            var tmpViews = 0; // views per interval
            var tmpViewsCount = 0; // view entry count per interval
            for (i = 1; i <= 500; i++) {
                tmpViews = 0;
                tmpViewsCount = 0;
                for (j = 1; j <= intvl; j++) { //real time loop
                    cTime++;
                    // count views for interval length
                    _.each(this.footprints, function (element, index, list) {
                        if (this.footprints.at(index).get("position") == cTime)
                            tmpViews += this.footprints.at(index).get("views");
                        tmpViewsCount++;
                    }, this);
                }
                // push chart data each point
                labels.push("");
                if (tmpViews != 0 && tmpViewsCount != 0) {
                    data.push(tmpViews / tmpViewsCount);
                } else {
                    data.push(0);
                }
            }

            var options = {
                // whether scale above the chart data     
                scaleOverlay: true,
                // whether override with a hard coded scale
                scaleOverride: false,
                //** required if scaleOverride is true **
                // the number of steps in a hard coded scale
                scaleSteps: 1,
                // the value jump in the hard coded scale
                scaleStepWidth: null,
                // the scale starting value
                scaleStartValue: 0,
                // colour of the scale line 
                scaleLineColor: "rgba(0,0,0,.1)",
                // pixel width of the scale line  
                scaleLineWidth: 1,
                // whether to show labels on the scale 
                scaleShowLabels: false,
                // interpolated JS string - can access value
                scaleLabel: "<%=value%>",
                // scale label font declaration for the scale label
                scaleFontFamily: "'Arial'",
                // scale label font size in pixels  
                scaleFontSize: 12,
                // scale label font weight style  
                scaleFontStyle: "normal",
                // scale label font color  
                scaleFontColor: "#666",
                // whether grid lines are shown across the chart
                scaleShowGridLines: false,
                // color of the grid lines
                scaleGridLineColor: "rgba(0,0,0,.05)",
                // width of the grid lines
                scaleGridLineWidth: 1,
                // whether the line is curved between points
                bezierCurve: true,
                // whether to show a dot for each point
                pointDot: false,
                // radius of each point dot in pixels
                pointDotRadius: 3,
                // pixel width of point dot stroke
                pointDotStrokeWidth: 1,
                // whether to show a stroke for datasets
                datasetStroke: false,
                // pixel width of dataset stroke
                datasetStrokeWidth: 1,
                // whether to fill the dataset with a colour
                datasetFill: true,
                // whether to animate the chart
                animation: false,
                // number of animation steps
                animationSteps: 60,
                // animation easing effect
                animationEasing: "easeOutQuart",
                // function to fire when the animation is complete
                onAnimationComplete: function(){}
            }

            var lineChartData = {
                labels: labels,
                datasets: [{
                    fillColor: "rgba(151,187,205,0.5)",
                    strokeColor: "rgba(151,187,205,1)",
                    pointColor: "rgba(151,187,205,1)",
                    pointStrokeColor: "#FFFFFF",
                    data: data
                }]
            }

            this.chart = new Chart(document.getElementById("engage_timeline_statistics_chart").getContext("2d")).Line(lineChartData, options);
        }
    });

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (plugin.inserted === true) {
            Engage.log("Timeline:Statistics: init view");
            // create a new view with the media package model and the template
            // new StatisticsTimelineView(Engage.model.get("mediaPackage"), plugin.template);
            // new StatisticsTimelineView(Engage.model.get("videoDataModel"), plugin.template);
            new StatisticsTimelineView("");
        }
    }

    // init event
    Engage.log("Timeline:Statistics: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginTimelineStatistics');
    Engage.log('Timeline:Statistics: Relative plugin path: "' + relative_plugin_path + '"');

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function () {
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    Engage.model.on(footprintChange, function () {
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    Engage.model.on(videoDataModelChange, function () {
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    // load highchart lib
    require([relative_plugin_path + chartPath], function (videojs) {
        Engage.log("Timeline:Statistics: Lib chart loaded");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function () {
        Engage.log("Timeline:Statistics: Plugin load done");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    return plugin;
});
