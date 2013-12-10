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
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_model'], function (require, $, _, Backbone, EngageModel) {
  //
  "use strict"; // strict mode in all our application
  //

  //Global private core variables
  var plugin_count = 0;

  //Theodul Core init
  if (window.console) {
    console.log("Core: Init");
  }

  //Event prototype
  function EngageEvent(name, description, type){
    var name = name;
    var description = description;
    var type = type;
    
    this.getName = (function(){
      return name;
    });
    
    this.getDescription = (function(){
      return description;
    });
    
    this.getType = (function(){
      return type;
    });
    
    this.toString = (function(){
      return name;
    });
  }
  
  /*
   * Main core
   */ 
  var EngageCore = Backbone.View.extend({
    el : $("#engage_view"),
    initialize : function () {
      // The main core is our global event system
      this.dispatcher = _.clone(Backbone.Events);
      //link to the engage model
      this.model = new EngageModel();
      // Watch on all events
      this.dispatcher.on("all", function (name) {
        if(engageCore.model.get("isDebug")){
          engageCore.log("EventLog: " + name + " occurs!");
        }   
      });
      // load Stream Event
      this.dispatcher.on("Core:init", function () {
        // fetch plugin information
        engageCore.model.get('pluginsInfo').fetch({
          success : function (pluginInfos) {
            // load plugin as requirejs module
            if (pluginInfos.get('pluginlist') && pluginInfos.get('pluginlist').plugins !== undefined) {
              if ($.isArray(pluginInfos.get('pluginlist').plugins)) {
                plugin_count = pluginInfos.get('pluginlist').plugins.length;
                $.each(pluginInfos.get('pluginlist').plugins, function (index, value) {
                  // load plugin
                  loadPlugin(PLUGIN_PATH + value['static-path']);
                });
              } else {
                plugin_count = 1;
                // load plugin
                loadPlugin(PLUGIN_PATH + pluginInfos.get('pluginlist').plugins['static-path']);
              }
            }
          }
        });
      });
      //describe timeline extensions
      $(".expand_button").click(function() {
        $(".expanded_content").slideToggle("fast");
        $(".pulldown_image").toggleClass("rotate180");
      });
      // load plugins done, hide loading and show content
      this.dispatcher.on("Core:plugin_load_done", function () {
        $(".loading").hide();
        $("#engage_view").show();
      });
    },
    on : function (event, handler, context) {
      if(event instanceof EngageEvent){
        this.dispatcher.on(event.getName(), handler, context);
      }else{
        this.dispatcher.on(event, handler, context);
      }  
    },
    trigger : function (event, data) {
      if(event instanceof EngageEvent){
        this.dispatcher.trigger(event.getName(), data);
      }else{
        this.dispatcher.trigger(event, data);
      }     
    },
    Event : EngageEvent,
    log : function (data) {
      if(this.model.get("isDebug")){
        if (window.console) {
          console.log(data);
        }        
      }
    }
  });

  // Create an engage view once the document has loaded
  var engageCore = new EngageCore();
  // Fire init event
  engageCore.trigger("Core:init");

  /*
   * BEGIN Private core functions
   */ 
  function addPluginLogic() {
    // first tab is on startup active
    $('#engage_tab_nav li:first').addClass("active");
    $('#engage_tab_content div:first').addClass("active");
    // click listener to change tab
    $('#engage_tab_nav a').click(function (e) {
      e.preventDefault();
      $(this).tab('show');
    });
  }

  function insertProcessedTemplate(processed_template, plugin_type, plugin_name) {
    var container = "";
    switch (plugin_type) {
    case "engage_controls":       
      $("#engage_controls").html(processed_template);
      container = "#engage_controls";
      break;  
    case "engage_video":        
      $("#engage_video").html(processed_template);
      container = "#engage_video";
      break;        
    case "engage_tab":        
      var tab_ref = plugin_name.replace(/ /g, "_");
      // insert tab navigation line
      var tabNavTag = '<li><a href="#engage_' + tab_ref + '_tab">' + plugin_name + '</a></li>';
      $("#engage_tab_nav").prepend(tabNavTag);
      // insert tab content
      var tabTag = '<div class="tab-pane" id="engage_' + tab_ref + '_tab">' + processed_template + '</div>';
      $("#engage_tab_content").prepend(tabTag);
      container = "#engage_" + tab_ref + "_tab";
      break;
    case "engage_description":
      $("#engage_description").html(processed_template);
      container = "#engage_description";
      break;      
    default:
    }
    return container;
  }
  
  function loadPlugin(plugin_path) {

    require([ plugin_path + "/main.js" ], function (plugin) {
      // load styles in link tags via jquery
      if ($.isArray(plugin.styles)) {
        $.each(plugin.styles, function (style_index, style_path) {
          if (style_path !== "") {
            var link = $("<link>");
            link.attr({
              type : 'text/css',
              rel : 'stylesheet',
              href : plugin_path + "/" + style_path
            });
            $("head").append(link);
          }
        });
      } else {
        if (plugin.styles !== "") {
          var link = $("<link>");
          link.attr({
            type : 'text/css',
            rel : 'stylesheet',
            href : plugin_path + "/" + plugin.styles
          });
          $("head").append(link);
        }
      }

      if (plugin.template !== "none") {
        // load template async
        $.get(plugin_path + "/" + plugin.template, function (template) {
          // empty data object
          var template_data = {};
          // add template if not undefined
          if (plugin.template_data !== undefined) {
            template_data = plugin.template_data;
          }          
          // add full plugin path to the tmeplate data
          template_data.plugin_path = plugin_path;
          // Process the template using underscore
          var processed_template = _.template(template, template_data);
          // Load the compiled HTML into the component
          plugin.container = insertProcessedTemplate(processed_template, plugin.type, plugin.name);
          plugin.template = template;
          plugin.pluginPath = plugin_path;
          // plugin load done counter
          plugin_count -= 1;
          if (plugin_count === 0) {
            addPluginLogic();
            // Trigger done event
            engageCore.trigger("Core:plugin_load_done");
          }
        });
      } else {
        plugin_count -= 1;
        if (plugin_count === 0) {
          addPluginLogic();
          // Trigger done event
          engageCore.trigger("Core:plugin_load_done");
        }
      }
    });

  }
  /*
   * END Private core functions
   */ 

  return engageCore;
});