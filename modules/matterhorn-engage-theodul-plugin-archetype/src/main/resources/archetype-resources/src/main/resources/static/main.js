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
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function(require, $, _, Backbone, Engage) {
    "use strict"; // strict mode in all our application
    var PLUGIN_NAME = "Basic Engage Description";
    var PLUGIN_TYPE = "engage_description";
    var PLUGIN_VERSION = "0.1";
    var PLUGIN_TEMPLATE = "template.html";
    var PLUGIN_STYLES = ["style.css"];
    var plugin = {
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES,
        template: PLUGIN_TEMPLATE
    };
    //privates //

    var initCount = 3; //wait for three inits, moment lib, plugin load done and mediapackage
    var id_engage_description = "engage_description";

    // view //

    var DescriptionView = Backbone.View.extend({
        el: $("#" + id_engage_description), // Every view has a element associated with it
        initialize: function(mediaPackageModel, template) {
            this.model = mediaPackageModel;
            this.template = template;
            //bound the render function always to the view
            _.bindAll(this, "render");
            //listen for changes of the model and bind the render function to this
            this.model.bind("change", this.render);
        },
        render: function() {
            //format values
            var tempVars = {
                title: this.model.get("title"),
                creator: this.model.get("creator"),
                date: this.model.get("date")
            };
            //try to format the date
            if (moment(tempVars.date) !== null) {
                tempVars.date = moment(this.model.get("date")).format("MMMM Do YYYY");
            }
            // compile template and load into the html
            this.$el.html(_.template(this.template, tempVars));
        }
    });

    function initPlugin() {
        //create a new view with the media package model and the template
        new DescriptionView(Engage.model.get("mediaPackage"), plugin.template);
    }

    //inits
    Engage.log("Description: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginDescription');
    Engage.log('Description: relative plugin path ' + relative_plugin_path);

    Engage.model.on("change:mediaPackage", function() { // listen on a change/set of the mediaPackage model
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });
    // Load moment.js lib
    require([relative_plugin_path + 'lib/moment.min'], function(momentjs) {
        Engage.log("Description: load moment.min.js done");
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });
    //All plugins loaded
    Engage.on("Core:plugin_load_done", function() {
        initCount -= 1;
        if (initCount === 0) {
            initPlugin();
        }
    });

    return plugin;
});