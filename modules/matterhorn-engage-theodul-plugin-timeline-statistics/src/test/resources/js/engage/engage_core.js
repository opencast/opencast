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
define(['require', 'jquery', 'underscore', 'backbone'], function (require, $, _, Backbone) {

  var EngageCore = Backbone.View.extend({
    urlParams: {
      id: "123"
    },
    log: function (msg) {
      if (window.console) {
        console.log(msg);
      }
    }
  });

  var engageCore = new EngageCore();
  //engageCore.trigger("Core:init");
  //engageCore.trigger("Core:plugin_load_done");
  return engageCore;
});