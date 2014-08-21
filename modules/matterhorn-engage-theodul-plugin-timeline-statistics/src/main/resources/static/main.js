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
    "use strict";
    var PLUGIN_NAME = "Timeline Usertracking Statistics";
    var PLUGIN_TYPE = "engage_timeline";
    var PLUGIN_VERSION = "0.1",
        PLUGIN_TEMPLATE = "template.html",
        PLUGIN_TEMPLATE_MOBILE = "template_mobile.html",
        PLUGIN_TEMPLATE_EMBED = "template_embed.html",
        PLUGIN_STYLES = [
            "style.css"
        ],
        PLUGIN_STYLES_MOBILE = [
            "style_mobile.css"
        ],
        PLUGIN_STYLES_EMBED = [
            "style_embed.css"
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
    var renderEveryTimes = 10;
    var chartPath = "lib/Chart";
    var timelineplugin_opened = "Engage:timelineplugin_opened";
    var chartOptions = {
        // Boolean - Whether to animate the chart
        animation: false,
        // Number - Number of animation steps
        animationSteps: 60,
        // String - Animation easing effect
        animationEasing: "easeOutQuart",
        // Boolean - If we should show the scale at all
        showScale: false,
        // Boolean - If we want to override with a hard coded scale
        scaleOverride: false,
        // ** Required if scaleOverride is true **
        // Number - The number of steps in a hard coded scale
        scaleSteps: 1,
        // Number - The value jump in the hard coded scale
        scaleStepWidth: null,
        // Number - The scale starting value
        scaleStartValue: 0,
        // String - Colour of the scale line
        scaleLineColor: "rgba(0,0,0,.1)",
        // Number - Pixel width of the scale line
        scaleLineWidth: 1,
        // Boolean - Whether to show labels on the scale
        scaleShowLabels: false,
        // Interpolated JS string - can access value
        scaleLabel: "<%=value%>",
        // Boolean - Whether the scale should stick to integers, not floats even if drawing space is there
        scaleIntegersOnly: true,
        // Boolean - Whether the scale should start at zero, or an order of magnitude down from the lowest value
        scaleBeginAtZero: true,
        // String - Scale label font declaration for the scale label
        scaleFontFamily: "'Helvetica Neue', 'Helvetica', 'Arial', sans-serif",
        // Number - Scale label font size in pixels
        scaleFontSize: 12,
        // String - Scale label font weight style
        scaleFontStyle: "normal",
        // String - Scale label font colour
        scaleFontColor: "#666",
        // Boolean - whether or not the chart should be responsive and resize when the browser does.
        responsive: false,
        // Boolean - whether to maintain the starting aspect ratio or not when responsive, if set to false, will take up entire container
        maintainAspectRatio: false,
        // Boolean - Determines whether to draw tooltips on the canvas or not
        showTooltips: false,
        // Array - Array of string names to attach tooltip events
        tooltipEvents: ["mousemove", "touchstart", "touchmove"],
        // String - Tooltip background colour
        tooltipFillColor: "rgba(0,0,0,0.8)",
        // String - Tooltip label font declaration for the scale label
        tooltipFontFamily: "'Helvetica Neue', 'Helvetica', 'Arial', sans-serif",
        // Number - Tooltip label font size in pixels
        tooltipFontSize: 14,
        // String - Tooltip font weight style
        tooltipFontStyle: "normal",
        // String - Tooltip label font colour
        tooltipFontColor: "#fff",
        // String - Tooltip title font declaration for the scale label
        tooltipTitleFontFamily: "'Helvetica Neue', 'Helvetica', 'Arial', sans-serif",
        // Number - Tooltip title font size in pixels
        tooltipTitleFontSize: 14,
        // String - Tooltip title font weight style
        tooltipTitleFontStyle: "bold",
        // String - Tooltip title font colour
        tooltipTitleFontColor: "#fff",
        // Number - pixel width of padding around tooltip text
        tooltipYPadding: 6,
        // Number - pixel width of padding around tooltip text
        tooltipXPadding: 6,
        // Number - Size of the caret on the tooltip
        tooltipCaretSize: 8,
        // Number - Pixel radius of the tooltip border
        tooltipCornerRadius: 6,
        // Number - Pixel offset from point x to tooltip edge
        tooltipXOffset: 10,
        // String - Template string for single tooltips
        tooltipTemplate: "<%if (label){%><%=label%>: <%}%><%= value %>",
        // String - Template string for single tooltips
        multiTooltipTemplate: "<%= value %>",
        // Function - Will fire on animation progression.
        onAnimationProgress: function() {},
        // Function - Will fire on animation completion.
        onAnimationComplete: function() {}
    }
    var chartLineOptions = {
        // Boolean - Whether grid lines are shown across the chart
        scaleShowGridLines: false,
        // String - Colour of the grid lines
        scaleGridLineColor: "rgba(0,0,0,.05)",
        // Number - Width of the grid lines
        scaleGridLineWidth: 1,
        // Boolean - Whether the line is curved between points
        bezierCurve: true,
        // Number - Tension of the bezier curve between points
        bezierCurveTension: 0.4,
        // Boolean - Whether to show a dot for each point
        pointDot: false,
        // Number - Radius of each point dot in pixels
        pointDotRadius: 4,
        // Number - Pixel width of point dot stroke
        pointDotStrokeWidth: 1,
        // Number - amount extra to add to the radius to cater for hit detection outside the drawn point
        pointHitDetectionRadius: 20,
        // Boolean - Whether to show a stroke for datasets
        datasetStroke: false,
        // Number - Pixel width of dataset stroke
        datasetStrokeWidth: 2,
        // Boolean - Whether to fill the dataset with a colour
        datasetFill: true,
    }

    /* don't change these variables */
    var mediapackageChange = "change:mediaPackage";
    var footprintChange = "change:footprints";
    var videoDataModelChange = "change:videoDataModel";
    var initCount = 5;
    var intLen = 500;
    var statisticsTimelineView;
    var renderEveryTimes_count = 0;
    var data; // chart data array
    var lineChartData;

    function setSize() {
        $("#engage_timeline_statistics_chart").attr("width", $(window).width() - 40).attr("height", 60).css({
            "width": $(window).width() - 40,
            "height": 60
        });
    }

    function rerender() {
        setSize();
        if (statisticsTimelineView && statisticsTimelineView.videoData) {
            var duration = parseInt(statisticsTimelineView.videoData.get("duration"));

            if (duration && (duration > 0)) {
                --renderEveryTimes_count;
                if (renderEveryTimes_count <= 0) {
                    renderEveryTimes_count = renderEveryTimes;

                    duration /= 1000;
                    data = new Array();
                    var labels = new Array(); // chart label array
                    var tmpViews = 0; // views per interval
                    var tmpViewsCount = 0; // view entry count per interval
                    for (var cTime = 0; cTime <= duration; ++cTime) {
                        tmpViews = 0;
                        _.each(statisticsTimelineView.footprints, function(value, key, list) {
                            value = list.at(key);
                            if (value.get("position") == cTime) {
                                tmpViews += value.get("views");
                                return false; // break the foreach-loop
                            }
                        });
                        // push chart data each point
                        labels.push("");
                        data.push(tmpViews);

                        lineChartData = {
                            labels: labels,
                            datasets: [{
                                fillColor: "rgba(151,187,205,0.5)",
                                strokeColor: "rgba(151,187,205,1)",
                                pointColor: "rgba(151,187,205,1)",
                                pointStrokeColor: "#FFFFFF",
                                data: data
                            }]
                        }
                    }
                }

                if (lineChartData) {
                    statisticsTimelineView.chart = new Chart(document.getElementById("engage_timeline_statistics_chart").getContext("2d")).Line(lineChartData, chartLineOptions);
                    statisticsTimelineView.chart.update();
                }
            }
        }
    }

    var StatisticsTimelineView = Backbone.View.extend({
        initialize: function() {
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
            $(window).resize(function() {
                rerender();
            });
        },
        render: function() {
            if (this.videoData && this.footprints) {
                var tempVars = {
                    width: $(window).width() - 40,
                    height: "60"
                };
                // compile template and load into the html
                this.$el.html(_.template(this.template, tempVars));
                rerender();
                if (this.chart && this.chart.update) {
                    this.chart.update();
                }
            }
        }
    });

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (plugin.inserted === true) {
            Chart.defaults.global = chartOptions;
            statisticsTimelineView = new StatisticsTimelineView("");

            Engage.on(timelineplugin_opened, function() {
                rerender();
            });
        }
    }

    // init event
    Engage.log("Timeline:Statistics: Init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginTimelineStatistics');

    Engage.model.on(footprintChange, function() {
        initCount -= 1;
        if (initCount == 0) {
            initPlugin();
        }
    });

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function() {
        initCount -= 1;
        if (initCount == 0) {
            initPlugin();
        }
    });

    Engage.model.on(videoDataModelChange, function() {
        initCount -= 1;
        if (initCount == 0) {
            initPlugin();
        }
    });

    // load highchart lib
    require([relative_plugin_path + chartPath], function(videojs) {
        Engage.log("Timeline:Statistics: Lib chart loaded");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("Timeline:Statistics: Plugin load done");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    return plugin;
});
