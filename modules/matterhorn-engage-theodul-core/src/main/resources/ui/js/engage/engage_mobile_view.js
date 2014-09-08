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
/*global define, CustomEvent*/
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core', 'engage/engage_model', 'jquery.mobile'], function (require, $, _, Backbone, EngageCore, EngageModel) {
  //
  'use strict'; // strict mode in all our application
  //
  /*
   * Init logic function
   */
  var initMobileView = function(){
    //load jquery mobile css files
    var link = $("<link>");
    link.attr({
      type : 'text/css',
      rel : 'stylesheet',
      href : 'css/jqueryMobile/matterhorn-alpha-theme.min.css'
    });
    $("head").append(link);

    link = $("<link>");
    link.attr({
      type : 'text/css',
      rel : 'stylesheet',
      href : 'css/jqueryMobile/jquery.mobile.icons-1.4.2.css'
    });
    $("head").append(link);
    link = $("<link>");
    link.attr({
      type : 'text/css',
      rel : 'stylesheet',
      href : 'css/jqueryMobile/jquery.mobile.inline-png-1.4.2.css'
    });
    //$("head").append(link);
    link = $("<link>");
    link.attr({
      type : 'text/css',
      rel : 'stylesheet',
      href : 'css/jqueryMobile/jquery.mobile.inline-svg-1.4.2.css'
    });
    //$("head").append(link);
    link = $("<link>");
    link.attr({
      type : 'text/css',
      rel : 'stylesheet',
      href : 'css/jqueryMobile/jquery.mobile.structure-1.4.2.css'
    });
    $("head").append(link);
    link = $("<link>");
    link.attr({
      type : 'text/css',
      rel : 'stylesheet',
      href : 'css/jqueryMobile/jquery.mobile.theme-1.4.2.css'
    });
   // $("head").append(link);
    link = $("<link>");
    link.attr({
      type : 'text/css',
      rel : 'stylesheet',
      href : 'css/jqueryMobile/jquery.mobile-1.4.2.css'
    });
    //$("head").append(link);

    // Set Viewport for mobile Devices
    $("head").append('<meta name="viewport" content="width=device-width, initial-scale=1">');
  }
  
  /*
   * Logic to insert a plugin with name and type to the player in mobile mode
   */
  var insertPluginToDOM = function(plugin) {

    switch (plugin.type) {
    case "engage_controls":    
      $("#engage_controls").html(plugin.templateProcessed);
      plugin.inserted = true;
      plugin.container = "#engage_controls";
      break;
    case "engage_video":        
      $('#engage_video').html(plugin.templateProcessed);
      plugin.inserted = true;
      plugin.container = "#engage_video";
      break;        
    case "engage_tab":        
      break;
    case "engage_description":
      $('#engage_description').html(plugin.templateProcessed);
      plugin.inserted = true;
      plugin.container = "#engage_description";
      break;    
    case "engage_timeline":
      break; 
    default:
      plugin.inserted = false;
      plugin.container = "";
    }
  }
  
  /*
   * This function is triggered when all plugins are loaded and inserted into the DOM
   */
  var allPluginsLoadedEvent = function(){
    var orientation = "";
    $(window).on("orientationchange", function(event) {
      EngageCore.model.set("orientation", event.orientation);
    });
    $(window).orientationchange();
  }
  
  // public functions fo the module
  return {
    initView : initMobileView,
    insertPlugin : insertPluginToDOM,
    allPluginsLoaded : allPluginsLoadedEvent
  }  
});