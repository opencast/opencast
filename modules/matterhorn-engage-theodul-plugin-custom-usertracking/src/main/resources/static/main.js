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
/*global define*/
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function(require, $, _, Backbone, Engage) {
    "use strict"; // strict mode in all our application
    var PLUGIN_NAME = "Engage Plugin Custom Usertracking",
            PLUGIN_TYPE = "engage_custom",
            PLUGIN_VERSION = "0.1",
            PLUGIN_TEMPLATE = "none",
            PLUGIN_STYLES = ["", ""];
    var plugin = {
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES,
        template: PLUGIN_TEMPLATE,
        events : {
          timeupdate : new Engage.Event("Video:timeupdate", "notices a timeupdate", "handler")
        }
    };

    // local privates//

    var initCount = 3; //init resource count
    var USERTRACKING_ENDPOINT = "/usertracking";
    var lastFootprint = undefined;
    var mediapackageID;

    // model prototypes //


    // plugin logic //

    Engage.log("Usertracking: init");

    //local function
    function initPlugin() {
      //Set Mediapackage ID
      mediapackageID = Engage.model.get("urlParameters").id;
      if (!mediapackageID) {
        mediapackageID = "";
        return;
      }
      
      /*
      Engage.on(plugin.events.timeupdate.getName(), function(currentTime) {
        //add footprint each rounded timeupdate
        var cTime = Math.round(currentTime);
        if(lastFootprint != undefined){
          if(lastFootprint != cTime){
            lastFootprint = cTime;
            Engage.log("Usertracking: footprint at "+cTime);
            //put to mh endpoint
            $.ajax({
              url: USERTRACKING_ENDPOINT,
              data: {id: mediapackageID, in: cTime, out: cTime+1, type: "FOOTPRINT"},
              type: 'PUT',
              success: function(result) {
                  //update current footprint model
                  Engage.model.get("footprints").update();
                }
            });
          }
        }else{
          lastFootprint = cTime;
        }
      });  
      */    
    }
    
    // All plugins loaded
    Engage.on("Core:plugin_load_done", function() {
        Engage.log("Usertracking: receive plugin load done");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });
    //Mediapackage model created
    Engage.model.on("change:mediaPackage", function() {
      initCount -= 1;
      if (initCount === 0) {
          initPlugin();
      }      
    });
    //Footprints model created
    Engage.model.on("change:footprints", function() {
      initCount -= 1;
      if (initCount === 0) {
          initPlugin();
      }      
    });

    return plugin;
});
