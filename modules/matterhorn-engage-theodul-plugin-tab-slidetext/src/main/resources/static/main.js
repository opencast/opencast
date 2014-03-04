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
  //
  "use strict"; // strict mode in all our application
  //
  var matterhorn_engage_theodul_plugin_tab_slidetext = {};

  (function(plugin) {
    plugin.name = "Slide Text";
    plugin.type = "engage_tab";
    plugin.version = "0.1";
    plugin.template = "template.html";
    plugin.styles = ["style.css"];

    var TEMPLATE_TAB_CONTENT_ID = "engage_slidetext_tab_content";
    var segments=[];

    var Segment = function (time,image_url) {
      this.time = time;
      this.image_url = image_url;
    };

    plugin.timeStrToSeconds = function (timeStr) {
      var elements = timeStr.match(/([0-9]{2})/g);
      //var timeSeconds = 0;
      //$(elements).each(function(index, element) {
      //   timeSeconds += (parseInt(element,10) * Math.pow(60, elements.length-index-1));
      //});
      //return timeSeconds;
      return parseInt(elements[0],10) * 3600 + parseInt(elements[1],10) * 60 + parseInt(elements[2],10);
    };

    plugin.initPlugin = function () {
      Engage.log("TabSlideText: initializing plugin");
      Engage.model.get("mediaPackage").on("change", function() {
        var attachments = this.get("attachments");
        if(attachments) {
          // Extract segments which type is "segment+preview" out of the model
          $(attachments).each(function(index, attachment) {
            if (attachment.mimetype && attachment.type && attachment.type.match(/presentation\/segment\+preview/g) && attachment.mimetype.match(/image/g)) {
              // Pull time string out of the ref property
              // (e.g. "ref": "track:4ea9108d-c1df-4d8e-b729-e7c75c87519e;time=T00:00:00:0F1000")
              var time = attachment.ref.match(/([0-9]{2}:[0-9]{2}:[0-9]{2})/g);
              if (time.length > 0) {
                segments.push(new Segment(time[0], attachment.url));
              } else {
                Engage.log("Failure on time evaluation for segment with url: " + attachment.url);
              }
            }
          });
          // Sort segments ascending by time
          segments.sort(function(a, b){
            return new Date("1970/1/1 " + a.time) - new Date("1970/1/1 " + b.time);
          });
          // Building html snippet for a segment and inject each in the template
          if (segments.length > 0) {
            $("#" + TEMPLATE_TAB_CONTENT_ID).empty();
            $(segments).each(function(index, segment) {
              var html_snippet = "";
              var html_snippet_id = "tab_slidetext_segment_" + index;
              var segment_name = "Segment " + index;
              html_snippet += "<div class=\"media\" id=\"" + html_snippet_id + "\">";
              html_snippet += "  <img class=\"media-object pull-left\" src=\"" + segment.image_url + "\" alt=\"" + segment_name + "\">";
              html_snippet += "  <div class=\"media-body\">";
              html_snippet += "    <h4 class=\"media-heading\">" + segment_name + "</h4>";
              html_snippet += "    " + segment.time;
              html_snippet += "  </div>";
              html_snippet += "</div>";
              $("#" + TEMPLATE_TAB_CONTENT_ID).append(html_snippet);

              // Add click handler to each segment (slide)
              $("#" + html_snippet_id).click(function() {
                //console.log("clicked:" + html_snippet_id + " at " + plugin.timeStrToSeconds(segment.time));
                Engage.trigger("Video:seek", plugin.timeStrToSeconds(segment.time));
              });
            });
          }
        }
      });
    };

    //Init Event
    Engage.log("Tab:Slidetext: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginTabSlidetext');
    Engage.log('Tab:Slidetext: relative plugin path ' + relative_plugin_path);

    //All plugins loaded lets do some stuff
    Engage.on("Core:plugin_load_done", function() {
      Engage.log("Tab:Slidetext: receive plugin load done");
    });

    Engage.model.on("change:mediaPackage", function() { // listen on a change/set of the mediaPackage model
      Engage.log("Tab:SlideText: change:mediaPackage event");
      plugin.initPlugin();
    });

  })(matterhorn_engage_theodul_plugin_tab_slidetext);

  return matterhorn_engage_theodul_plugin_tab_slidetext;
});
