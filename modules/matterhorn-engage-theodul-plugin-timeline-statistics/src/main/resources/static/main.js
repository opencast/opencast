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
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function (require, $, _, Backbone, Engage) {
	var PLUGIN_NAME = "Timeline Usertracking Statistics";
	var PLUGIN_TYPE = "engage_timeline";
	var PLUGIN_VERSION = "0.1";
	var PLUGIN_TEMPLATE = "template.html";
	var PLUGIN_STYLES = ["style.css"];
  var plugin = {
      name: PLUGIN_NAME,
      type: PLUGIN_TYPE,
      version: PLUGIN_VERSION,
      styles: PLUGIN_STYLES,
      template: PLUGIN_TEMPLATE
  };
  
  var initCount = 6; //wait for 4 inits, plugin load done, mediapackage, 2 highchart libs
  
  var StatisticsTimelineView = Backbone.View.extend({
    initialize: function() {
        this.setElement($(plugin.container)); // Every plugin view has it's own container associated with it
        this.videoData = Engage.model.get("videoDataModel");
        this.footprints = Engage.model.get("footprints");
        this.template = plugin.template;
        //bound the render function always to the view
        _.bindAll(this, "render");
        //listen for changes of the model and bind the render function to this
        this.videoData.bind("change", this.render);
        this.footprints.bind("change", this.render);
        this.render();
    },
    render: function() {
        //format values
        var tempVars = {
            width: $(window).width(),
            height: "60"
        };
        // compile template and load into the html
        this.$el.html(_.template(this.template, tempVars));
        
        var duration = this.videoData.get("duration");
        
        //fill array 
        var data = new Array();
        var cView = 0;
        for(i=0;i<duration/1000;i++){
          _.each(this.footprints, function(element, index, list){
            if(this.footprints.at(index).get("position") == i)
              cView = this.footprints.at(index).get("views");
          }, this);
          data.push([i,cView]);
        }
        /*
        var labels = new Array();
        var data = new Array();
        for(i=0;i<100;i++){
          labels.push("");
          if(i>50 && i<80){
            data.push(5);
          }else if(i>0 && i<10){
            data.push(10);
          }else{
            data.push(0);
          }
        }*/
        var labels = new Array(); //chart label array
        var data = new Array(); //chart data array
        var int = (duration/1000)/500; //interval length
        var cTime = 0; //current time in process
        var tmpViews = 0; //views per interval
        var tmpViewsCount = 0; //view entry count per interval
        for(i=1;i<=500;i++){
          tmpViews = 0;
          tmpViewsCount = 0;
          for(j=1;j<=int;j++){ //real time loop
            cTime++;
            //Count Views for interval length
            _.each(this.footprints, function(element, index, list){
              if(this.footprints.at(index).get("position") == cTime)
                tmpViews += this.footprints.at(index).get("views");
                tmpViewsCount++;
            }, this);
          }
          //push chart data each point
          labels.push("");
          if(tmpViews != 0 && tmpViewsCount != 0){
            data.push(tmpViews/tmpViewsCount);
          }else{
            data.push(0);
          }       
        }

        var options = { 
            //Boolean - If we show the scale above the chart data     
            scaleOverlay : true,
            //Boolean - If we want to override with a hard coded scale
            scaleOverride : false,
            //** Required if scaleOverride is true **
            //Number - The number of steps in a hard coded scale
            scaleSteps : 1,
            //Number - The value jump in the hard coded scale
            scaleStepWidth : null,
            //Number - The scale starting value
            scaleStartValue : 0,
            //String - Colour of the scale line 
            scaleLineColor : "rgba(0,0,0,.1)",
            //Number - Pixel width of the scale line  
            scaleLineWidth : 1,
            //Boolean - Whether to show labels on the scale 
            scaleShowLabels : false,
            //Interpolated JS string - can access value
            scaleLabel : "<%=value%>",
            //String - Scale label font declaration for the scale label
            scaleFontFamily : "'Arial'",
            //Number - Scale label font size in pixels  
            scaleFontSize : 12,
            //String - Scale label font weight style  
            scaleFontStyle : "normal",
            //String - Scale label font colour  
            scaleFontColor : "#666",  
            ///Boolean - Whether grid lines are shown across the chart
            scaleShowGridLines : false,
            //String - Colour of the grid lines
            scaleGridLineColor : "rgba(0,0,0,.05)",
            //Number - Width of the grid lines
            scaleGridLineWidth : 1, 
            //Boolean - Whether the line is curved between points
            bezierCurve : true,
            //Boolean - Whether to show a dot for each point
            pointDot : false,
            //Number - Radius of each point dot in pixels
            pointDotRadius : 3,
            //Number - Pixel width of point dot stroke
            pointDotStrokeWidth : 1,
            //Boolean - Whether to show a stroke for datasets
            datasetStroke : false,
            //Number - Pixel width of dataset stroke
            datasetStrokeWidth : 1,
            //Boolean - Whether to fill the dataset with a colour
            datasetFill : true,
            //Boolean - Whether to animate the chart
            animation : false,
            //Number - Number of animation steps
            animationSteps : 60,
            //String - Animation easing effect
            animationEasing : "easeOutQuart",
            //Function - Fires when the animation is complete
            onAnimationComplete : null
          }

          var lineChartData = {
            labels : labels,
            datasets : [
              {
                fillColor : "rgba(151,187,205,0.5)",
                strokeColor : "rgba(151,187,205,1)",
                pointColor : "rgba(151,187,205,1)",
                pointStrokeColor : "#fff",
                data : data
              }
            ] 
          }

          this.chart = new Chart(document.getElementById("engage_timeline_statistics_chart").getContext("2d")).Line(lineChartData, options);
    }
  });


  
  function initPlugin() {
    Engage.log("Timeline: Statistics: init view");
    //create a new view with the media package model and the template
    //new StatisticsTimelineView(Engage.model.get("mediaPackage"), plugin.template);
    //new StatisticsTimelineView(Engage.model.get("videoDataModel"), plugin.template);
    new StatisticsTimelineView("");
  }
  
  var relative_plugin_path = Engage.getPluginPath('EngagePluginTimelineStatistics');
  Engage.log('Statistics: relative plugin path ' + relative_plugin_path);
  
	//Init Event
  Engage.log("Timeline: Statistics: init");
  
  Engage.model.on("change:mediaPackage", function() { // listen on a change/set of the mediaPackage model
    initCount -= 1;
    if (initCount === 0) {
        initPlugin();
    }
  });
  
  Engage.model.on("change:footprints", function() { 
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
  
  // load highchart lib
  require([relative_plugin_path + "lib/Chart.min"], function(videojs) {
      Engage.log("Statistics Timeline: Load Chart JS done");
      initCount -= 1;
      if (initCount === 0) {
          initPlugin();
      }
  });

  // Load moment.js lib
  require([relative_plugin_path + "lib/moment.min"], function(momentjs) {
      Engage.log("Description: load moment.min.js done");
      initCount -= 1;
      if (initCount === 0) {
          initPlugin();
      }
  });
  //All plugins loaded lets do some stuff
  Engage.on("Core:plugin_load_done", function() {   	
    initCount -= 1;
    if (initCount === 0) {
        initPlugin();
    }
  });
   
  return plugin;
});