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

  var PLUGIN_NAME = "Slide Text";
  var PLUGIN_TYPE = "engage_tab";
  var PLUGIN_VERSION = "0.1";
  var PLUGIN_TEMPLATE = "template.html";
  var PLUGIN_STYLES = ["style.css"];

  var TEMPLATE_TAB_CONTENT_ID = "#engage_slidetext_tab_content";
  var segments=[];

  function Segment(time,image_url) {
    this.time = time;
    this.image_url = image_url;
  }

  //Init Event
  Engage.log("Tab:Slidetext: init");
  var relative_plugin_path = Engage.getPluginPath('EngagePluginTabSlidetext');
  Engage.log('Tab:Slidetext: relative plugin path ' + relative_plugin_path);
  //Load other needed JS stuff with Require
  //require(["./js/bootstrap/js/bootstrap.js"]);
  //require(["./js/jqueryui/jquery-ui.min.js"]);

  //All plugins loaded lets do some stuff
  Engage.on("Core:plugin_load_done", function() {
    Engage.log("Tab:Slidetext: receive plugin load done");
  });

  Engage.model.on("change:mediaPackage", function() { // listen on a change/set of the mediaPackage model
    Engage.log("Tab:SlideText: change:mediaPackage event");
    initPlugin();
  });

  function initPlugin() {
    Engage.log("TabSlideText: initalizing plugin");
    Engage.model.get("mediaPackage").on("change", function() {
      var attachments = this.get("attachments");
      if(attachments) {
        $(attachments).each(function(index, attachment) {
          if (attachment.mimetype && attachment.type && attachment.type.match(/presentation\/segment\+preview/g) && attachment.mimetype.match(/image/g)) {
            var time = attachment.ref.match(/([0-9]{2}:[0-9]{2}:[0-9]{2})/g);
            segments.push(new Segment(time, attachment.url));
          }
        });
        segments.sort(function(a, b){
          return new Date("1970/1/1 " + a.time) - new Date("1970/1/1 " + b.time);
        });
        if (segments.length > 0) {
          $(TEMPLATE_TAB_CONTENT_ID).empty();
          $(segments).each(function(index, segment) {
            var html_snippet = "";
            html_snippet += "<div id=\"#tab_slidetext_segment_" + index + "\">";
            html_snippet += segment.time;
            html_snippet += "  <img src=\"" + segment.image_url + "\"/>";
            html_snippet += "</div>";
            $(TEMPLATE_TAB_CONTENT_ID).append(html_snippet);
          });
        }
      }
    });
  }

  return {
    name: PLUGIN_NAME,
    type: PLUGIN_TYPE,
    version: PLUGIN_VERSION,
    styles: PLUGIN_STYLES,
    template: PLUGIN_TEMPLATE
  };
});