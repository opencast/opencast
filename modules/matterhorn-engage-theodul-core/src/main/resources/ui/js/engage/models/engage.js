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
define(['jquery', 'backbone', 'bowser', 'basil', 'engage/models/pluginInfo', 'engage/models/meInfo'], function($, Backbone, Bowser, Basil, PluginInfoModel, MeInfoModel) {
    "use strict";

    var PluginModelCollection = Backbone.Collection.extend({});

    var basilOptions = {
        namespace: 'mhStorage'
    };
    Basil = new window.Basil(basilOptions);

    /*
     * Main Model Prototype
     */
    var EngageModel = Backbone.Model.extend({
        initialize: function() {
            // parse url parameters
            var match, pl = /\+/g, // regex for replacing addition symbol
                // with a space
                search = /([^&=]+)=?([^&]*)/g,
                decode = function(s) {
                    return decodeURIComponent(s.replace(pl, " "));
                },
                query = window.location.search.substring(1);

            var urlParams = {}; // stores url params
            while (match = search.exec(query)) {
                urlParams[decode(match[1])] = decode(match[2]);
            }

            this.set("orientation", "landscape");

            this.set("urlParameters", urlParams);
            // set players debug mode
            if (this.get("urlParameters").debug == "true") {
                this.set("isDebug", true);
            } else {
                this.set("isDebug", false);
            }
            if (this.get("urlParameters").debugEvents == "true") {
                this.set("isEventDebug", true);
            } else {
                this.set("isEventDebug", false);
            }
            // check mode, if no mode param given try to discover browser
            if (this.get("urlParameters").mode == "desktop") {
                this.set("mode", "desktop");
            } else if (this.get("urlParameters").mode == "embed") {
                this.set("mode", "embed");
            } else if (this.get("urlParameters").mode == "mobile") {
                this.set("mode", "mobile");
            } else {
                this.set("mode", (Bowser.mobile || Bowser.tablet) ? "mobile" : "desktop");
            }

            // Check for user setting "Support unsupported browser"
            if (Basil.get("overrideBrowser") == null) {
                Basil.set("overrideBrowser", this.get("urlParameters").browser == "all"); 
            };

            // Check for user setting "Preferred format"
            Basil.set("preferredFormat", this.get("urlParameters").format);

            if (window.console) {
                console.log("EngageModel: Player mode: " + this.get("mode"));
            }
        },
        defaults: {
            "pluginsInfo": new PluginInfoModel(),
            "pluginModels": new PluginModelCollection(),
            "meInfo": new MeInfoModel(),
            "urlParameters": {}
        }
    });

    return EngageModel;
});
