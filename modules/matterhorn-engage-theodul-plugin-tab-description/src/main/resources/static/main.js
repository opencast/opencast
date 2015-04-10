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
 */
/*jslint browser: true, nomen: true*/
/*global define*/
define(["jquery", "underscore", "backbone", "engage/core", "moment"], function($, _, Backbone, Engage, Moment) {
    "use strict";

    var insertIntoDOM = true;
    var PLUGIN_NAME = "Description";
    var PLUGIN_TYPE = "engage_tab";
    var PLUGIN_VERSION = "1.0";
    var PLUGIN_TEMPLATE_DESKTOP = "templates/desktop.html";
    var PLUGIN_TEMPLATE_MOBILE = "templates/mobile.html";
    var PLUGIN_TEMPLATE_EMBED = "templates/embed.html";
    var PLUGIN_STYLES_DESKTOP = [
        "styles/desktop.css"
    ];
    var PLUGIN_STYLES_EMBED = [
        "styles/embed.css"
    ];
    var PLUGIN_STYLES_MOBILE = [
        "styles/mobile.css"
    ];

    var plugin;
    var events = {
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "", "handler"),
        mediaPackageModelError: new Engage.Event("MhConnection:mediaPackageModelError", "", "handler")
    };

    var isDesktopMode = false;
    var isEmbedMode = false;
    var isMobileMode = false;

    // desktop, embed and mobile logic
    switch (Engage.model.get("mode")) {
        case "embed":
            plugin = {
                insertIntoDOM: insertIntoDOM,
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES_EMBED,
                template: PLUGIN_TEMPLATE_EMBED,
                events: events
            };
            isEmbedMode = true;
            break;
        case "mobile":
            plugin = {
                insertIntoDOM: insertIntoDOM,
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES_MOBILE,
                template: PLUGIN_TEMPLATE_MOBILE,
                events: events
            };
            isMobileMode = true;
            break;
        case "desktop":
        default:
            plugin = {
                insertIntoDOM: insertIntoDOM,
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES_DESKTOP,
                template: PLUGIN_TEMPLATE_DESKTOP,
                events: events
            };
            isDesktopMode = true;
            break;
    }

    /* change these variables */
    var class_tabGroupItem = "tab-group-item";

    /* don't change these variables */
    var viewsModelChange = "change:views";
    var mediapackageChange = "change:mediaPackage";
    var initCount = 4;
    var mediapackageError = false;
    var translations = new Array();
    var locale = "en";
    var dateFormat = "MMMM Do YYYY, h:mm:ss a";
    var Utils;

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath("EngagePluginTabDescription").replace(/(\.\.\/)/g, "");
        var jsonstr = window.location.origin + "/engage/theodul/" + path; // this solution is really bad, fix it...

        Engage.log("Controls: selecting language " + language);
        jsonstr += "language/" + language + ".json";
        $.ajax({
            url: jsonstr,
            dataType: "json",
            async: false,
            success: function(data) {
                if (data) {
                    data.value_locale = language;
                    translations = data;
                    if (funcSuccess) {
                        funcSuccess(translations);
                    }
                } else {
                    if (funcError) {
                        funcError();
                    }
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if (funcError) {
                    funcError();
                }
            }
        });
    }

    function translate(str, strIfNotFound) {
        return (translations[str] != undefined) ? translations[str] : strIfNotFound;
    }

    var DescriptionTabView = Backbone.View.extend({
        initialize: function(mediaPackageModel, template) {
            this.setElement($(plugin.container)); // every plugin view has it's own container associated with it
            this.model = mediaPackageModel;
            this.template = template;
            // bind the render function always to the view
            _.bindAll(this, "render");
            // listen for changes of the model and bind the render function to this
            this.model.bind("change", this.render);
        },
        render: function() {
            if (!mediapackageError) {
                var tempVars = {
                    description: this.model.get("description"),
                    creator: this.model.get("creator"),
                    title: this.model.get("title"),
                    series: this.model.get("series"),
                    contributor: this.model.get("contributor"),
                    date: this.model.get("date"),
                    views: Engage.model.get("views") ? Engage.model.get("views").get("stats").views : "",
                    str_title: translate("title", "Title"),
                    str_noTitle: translate("noTitle", "No title"),
                    str_creator: translate("creator", "Creator"),
                    str_contributor: translate("presenter", "Contributor"),
                    str_views: translate("views", "Views"),
                    str_series: translate("series", "Series"),
                    str_recordingDate: translate("recordingDate", "Recording date"),
                    str_description: translate("description", "Description"),
                    str_noDescriptionAvailable: translate("noDescriptionAvailable", "No description available.")
                };
                // try to format the date
                Moment.locale(locale, {
                    // customizations
                });
                if (Moment(tempVars.date) != null) {
                    tempVars.date = Moment(tempVars.date).format(dateFormat);
                }
                if (!tempVars.creator) {
                    tempVars.creator = "";
                }
                if (!tempVars.description) {
                    tempVars.description = "";
                }
                if (!tempVars.title) {
                    tempVars.title = "";
                }
                if (!tempVars.series) {
                    tempVars.series = "";
                }
                if (!tempVars.contributor) {
                    tempVars.contributor = "";
                }
                if (!tempVars.date) {
                    tempVars.date = "";
                }
                if (!tempVars.views) {
                    tempVars.views = "-";
                }
                // compile template and load into the html
                this.$el.html(_.template(this.template, tempVars));
                /*
        	    $(".description-item").mouseover(function() {
        	        $(this).removeClass("description-itemColor").addClass("description-itemColor-hover");
        	    }).mouseout(function() {
        	        $(this).removeClass("description-itemColor-hover").addClass("description-itemColor");
        	    });
        	*/
            }
        }
    });

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (isDesktopMode && plugin.inserted) {
            // create a new view with the media package model and the template
            var descriptionTabView = new DescriptionTabView(Engage.model.get("mediaPackage"), plugin.template);
            Engage.on(plugin.events.mediaPackageModelError.getName(), function(msg) {
                mediapackageError = true;
            });
            Engage.model.get("views").on("change", function() {
                descriptionTabView.render();
            });
            descriptionTabView.render();
        }
    }

    if (isDesktopMode) {
        // init event
        Engage.log("Tab:Description: Init");
        var relative_plugin_path = Engage.getPluginPath("EngagePluginTabDescription");

        // load utils class
        require([relative_plugin_path + "utils"], function(utils) {
            Engage.log("Tab:Description: Utils class loaded");
            Utils = new utils();
            initTranslate(Utils.detectLanguage(), function() {
                Engage.log("Tab:Description: Successfully translated.");
                initCount -= 1;
                if (initCount <= 0) {
                    initPlugin();
                }
            }, function() {
                Engage.log("Tab:Description: Error translating...");
                initCount -= 1;
                if (initCount <= 0) {
                    initPlugin();
                }
            });
        });

        Engage.model.on(viewsModelChange, function() {
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });

        // listen on a change/set of the mediaPackage model
        Engage.model.on(mediapackageChange, function() {
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });

        // all plugins loaded
        Engage.on(plugin.events.plugin_load_done.getName(), function() {
            Engage.log("Tab:Description: Plugin load done");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });
    }

    return plugin;
});
