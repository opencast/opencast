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
define(['require', 'jquery', 'underscore', 'backbone', 'mousetrap', 'engage/engage_model', 'engage/engage_tab_logic'], function (require, $, _, Backbone, Mousetrap, EngageModel, EngageTabLogic) {
  //
  "use strict"; // strict mode in all our application
  //

  //Global private core variables
  var plugins_loaded = {};

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
      // Core Initialize Event
      this.dispatcher.on("Core:init", function () {
        //switch view template and css rules for current player mode
        //link tag for css file
        var cssLinkTag = $("<link>");
        var cssAttr = {
            type : 'text/css',
            rel : 'stylesheet'
        };
        //template obj
        var core_template = "none";
        //path to the require module with the view logic
        var view_logic_path = "";
        switch(engageCore.model.get("mode")){
        case "desktop":
            cssAttr.href = 'css/core_desktop_style.css';
            core_template = "templates/core_desktop.html";
            view_logic_path = "engage/engage_desktop_view"
            break;
        case "mobile":
            cssAttr.href = 'css/core_mobile_style.css';
            core_template = "templates/core_mobile.html";
            view_logic_path = "engage/engage_mobile_view"
            break;
        case "embed":
            cssAttr.href = 'css/core_embed_style.css';
            core_template = "templates/core_embed.html";
            view_logic_path = "engage/engage_embed_view"
            break;
        }
        cssLinkTag.attr(cssAttr);
        //add css to DOM
        $("head").append(cssLinkTag);
        //load js view logic via require, see files engage_<mode>_view.js
        require([view_logic_path], function(pluginView) {
          //link view logic to the core
          engageCore.pluginView = pluginView;
          //Get Core template
          $.get(core_template, function (template) {
            //set template, render it and add it to DOM
            engageCore.template = template;
            $(engageCore.el).html(_.template(template));
            //run init function of the view
            engageCore.pluginView.initView();
            /*BEGIN LOAD PLUGINS*/
            // fetch plugin information
            engageCore.model.get('pluginsInfo').fetch({
              success : function (pluginInfos) {
                // load plugin as requirejs module
                if (pluginInfos.get('pluginlist') && pluginInfos.get('pluginlist').plugins !== undefined) {
                  if ($.isArray(pluginInfos.get('pluginlist').plugins)) {
                    $.each(pluginInfos.get('pluginlist').plugins, function (index, value) {
                      var plugin_name = value['name'];
                      plugins_loaded[plugin_name] = false;
                    });
                    $.each(pluginInfos.get('pluginlist').plugins, function (index, value) {
                      // load plugin
                      var plugin_name = value['name'];
                      loadPlugin('../../../plugin/' + value['static-path'] + '/', plugin_name);
                    });
                  } else {
                    // load plugin
                    var plugin_name = value['name'];
                    plugins_loaded[plugin_name] = false;
                    loadPlugin('../../../plugin/' + pluginInfos.get('pluginlist').plugins['static-path'] + '/', plugin_name);
                  }
                }
              }
            });
            /*END LOAD PLUGINS*/
            //wait that me infos are loaded
            while(engageCore.model.get("meInfo").ready === false){}
            bindHotkeysToEvents(); //bind configured hotkeys to theodul events
          });
        });
      });
      // load plugins done, hide loading and show content
      this.dispatcher.on("Core:plugin_load_done", function () {
        $(".loading").hide();
        $("#engage_view").show();
      });
    },
    //bind a key event as a string to given theodul event
    bindKeyToEvent : function (hotkey, event) {
      //only for EngageEvent objects
      if(event instanceof EngageEvent){
        Mousetrap.bind(hotkey, function(){
          engageCore.trigger(event);
        });        
      }       
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
    },
    getPluginPath : function (pluginName) {
       var evaluated_plugin_path = '';
       var pluginsInfos = engageCore.model.get('pluginsInfo');
       var pluginList = pluginsInfos.get('pluginlist');
       if (pluginList && pluginList.plugins !== undefined) {
         var plugins = pluginList.plugins;
         if ($.isArray(plugins)) {
           $.each(plugins, function (index, value) {
             if (value['name'] === pluginName) {
               evaluated_plugin_path = '../../../plugin/' + value['static-path'] + '/';
             }
           });
         } else {
           evaluated_plugin_path = '../../../plugin/' + value['static-path'] + '/';
         }
       }
       return evaluated_plugin_path;
     }
  });

  // Create an engage view once the document has loaded
  var engageCore = new EngageCore();
  // Fire init event
  engageCore.trigger("Core:init");

  /*
   * BEGIN Private core functions
   */
  
  //binds configured hotkeys(see MH org config) to corresponding theodul events
  function bindHotkeysToEvents(){
    //process hardcoded keys
    $.each(engageCore.model.get("meInfo").get("hotkeys"), function(i, val){
      switch(val.name){
      case "jumpToX":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Controls:jumpToX");
        });
        break;
      case "nextChapter":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:nextChapter");
        });
        break;
      case "fullscreen":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:goFullscreen");
        });
        break;
      case "jumpToBegin":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:jumpToBegin");
        });
        break;
      case "prevEpisode":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Core:previousEpisode");
        });
        break;
      case "prevChapter":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:previousChapter");
        });
        break;
      case "pause":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:pause");
        });
        break;
      case "mute":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:mute");
        });
        break;
      case "nextEpisode":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Core:nextEpisode");
        });
        break;
      case "volDown":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:volumeDown");
        });
        break;
      case "volUp":
        Mousetrap.bind(val.key, function(){
          engageCore.trigger("Video:volumeUp");
        });
        break;
      default:
        break;
      }
    });
    //process custom hotkeys
    $.each(engageCore.model.get("meInfo").get("hotkeysCustom"), function(i, val){
      Mousetrap.bind(val.key, function(){
        engageCore.trigger(val.app+":"+val.func); //trigger specific custom event
      });      
    });
  }

  function checkAllPluginsloaded() {
    var all_plugins_loaded = true;
    $.each(plugins_loaded, function (plugin_index, plugin_value) {
      if(plugin_value === false) {
         all_plugins_loaded = false;
      }
    });
    return all_plugins_loaded;
  }

  function loadPlugin(plugin_path, plugin_name) {

    require([ plugin_path + 'main' ], function (plugin) {
      // load styles in link tags via jquery
      if ($.isArray(plugin.styles)) {
        $.each(plugin.styles, function (style_index, style_path) {
          if (style_path !== "") {
            var link = $("<link>");
            link.attr({
              type : 'text/css',
              rel : 'stylesheet',
              href : 'engage/theodul/' + plugin_path + style_path
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
            href : 'engage/theodul/' + plugin_path + plugin.styles
          });
          $("head").append(link);
        }
      }

      if (plugin.template !== "none") {
        // load template async
        $.get('engage/theodul/' + plugin_path + plugin.template, function (template) {
          // empty data object
          var template_data = {};
          // add template if not undefined
          if (plugin.template_data !== undefined) {
            template_data = plugin.template_data;
          }          
          // add full plugin path to the tmeplate data
          template_data.plugin_path =  'engage/theodul/' + plugin_path;
          // Process the template using underscore and set it in the plugin obj
          plugin.templateProcessed = _.template(template, template_data);
          plugin.template = template;
          plugin.pluginPath = 'engage/theodul/' + plugin_path;
          // Load the compiled HTML into the component
          engageCore.pluginView.insertPlugin(plugin);
          // plugin load done counter
          plugins_loaded[plugin_name] = true;
          // Check if all plugins are ready
          if (checkAllPluginsloaded() === true) {
            engageCore.pluginView.allPluginsLoaded();
            // Trigger done event
            engageCore.trigger("Core:plugin_load_done");
          }
        });
      } else {
        plugins_loaded[plugin_name] = true;
        // Check if all plugins are ready
        if (checkAllPluginsloaded() === true) {
          engageCore.pluginView.allPluginsLoaded();
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