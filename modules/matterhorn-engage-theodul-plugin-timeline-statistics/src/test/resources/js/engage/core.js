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
/*jslint browser: true*/
/*global define*/
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_model'], function (require, $, _, Backbone, EngageModel) {

  //Event prototype
  function EngageEvent(name, description, type){

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

  var EngageCore = Backbone.View.extend({
    initialize : function () {
      // The main core is our global event system
      this.dispatcher = _.clone(Backbone.Events);
      //link to the engage model
      this.model = new EngageModel();
    },

    urlParams: {
      id: "123"
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
      return '';
    }
  });

  var engageCore = new EngageCore();
  return engageCore;
});
