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
define(['require', 'jquery', 'underscore', 'backbone'], function (require, $, _, Backbone) {
  /*
   * BEGIN Prototypes
   */

  var PluginInfoModel = Backbone.Model.extend({
    // URL of the search enpoint
    urlRoot : PLUGIN_MANAGER_PATH,
    initialize : function () {

    },
    defaults : {
      "pluginlist" : {
        "plugins" : {}
      }
    }
  });
  
  var PluginModelCollection = Backbone.Collection.extend({
    
  });
  
  /*
   * END Prototypes
   */
  
  /*
   * Main Model Prototype
   */
  var EngageModel = Backbone.Model.extend({
    initialize: function() {
      // parse url parameters
      var match, pl = /\+/g, // Regex for replacing addition symbol
                              // with a space
      search = /([^&=]+)=?([^&]*)/g, decode = function (s) {
        return decodeURIComponent(s.replace(pl, " "));
      }, query = window.location.search.substring(1);

      var urlParams = {}; // stores url params
      while (match = search.exec(query)) {
        urlParams[decode(match[1])] = decode(match[2]);
      }
      this.set("urlParameters", urlParams);
      //set players debug mode
      if(this.get("urlParameters").debug === "true"){
        this.set("isDebug", true);
      }else{
        this.set("isDebug", false);
      }
    },
    /*
     * Public properties
     */
    defaults : {
      "pluginsInfo" : new PluginInfoModel(),
      "pluginModels" : new PluginModelCollection(),
      "urlParameters": {}
    }
  });

  return EngageModel;
});
