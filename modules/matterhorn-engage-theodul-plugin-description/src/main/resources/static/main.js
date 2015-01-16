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
define(["require", "jquery", "underscore", "backbone", "engage/core", "moment"], function(require, $, _, Backbone, Engage, Moment) {
    "use strict";

    var PLUGIN_NAME = "Basic Engage Description";
    var PLUGIN_TYPE = "engage_description";
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

    switch (Engage.model.get("mode")) {
        case "mobile":
            plugin = {
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES_MOBILE,
                template: PLUGIN_TEMPLATE_MOBILE,
                events: events
            };
            isMobileMode = true;
            break;
        case "embed":
            plugin = {
                name: PLUGIN_NAME,
                type: PLUGIN_TYPE,
                version: PLUGIN_VERSION,
                styles: PLUGIN_STYLES_EMBED,
                template: PLUGIN_TEMPLATE_EMBED,
                events: events
            };
            isEmbedMode = true;
            break;
        case "desktop":
        default:
            plugin = {
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

    /* don't change these variables */
    var Utils;
    var initCount = 3;
    var id_engage_description = "engage_description";
    var mediapackageChange = "change:mediaPackage";
    var mediapackageError = false;
    var translations = new Array();
    var locale = "en";
    var dateFormat = "MMMM Do YYYY";

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath("EngagePluginDescription").replace(/(\.\.\/)/g, "");
        var jsonstr = window.location.origin + "/engage/theodul/" + path; // this solution is really bad, fix it...

        if (language == "de") {
            Engage.log("Description: Chosing german translations");
            jsonstr += "language/de.json";
        } else { // No other languages supported, yet
            Engage.log("Description: Chosing english translations");
            jsonstr += "language/en.json";
        }
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

    var DescriptionView = Backbone.View.extend({
        el: $("#" + id_engage_description),
        initialize: function(mediaPackageModel, template) {
            this.model = mediaPackageModel;
            this.template = template;
            _.bindAll(this, "render");
            this.model.bind("change", this.render);
            this.render();
        },
        render: function() {
            if (!mediapackageError) {
                var tempVars = {
                    title: this.model.get("title"),
                    creator: this.model.get("creator"),
                    date: this.model.get("date"),
                    str_videoTitle: translate("videoTitle", "Video title"),
                    str_creator: translate("creator", "Creator"),
                    str_date: translate("date", "Date")
                };
                Moment.locale(locale, {
                    // customizations
                });
                // try to format the date
                if (Moment(tempVars.date) != null) {
                    tempVars.date = Moment(tempVars.date).format(dateFormat);
                }
                // compile template and load into the html
                this.$el.html(_.template(this.template, tempVars));
            }
        }
    });

    function initPlugin() {
        if ((isDesktopMode || isMobileMode) && plugin.inserted) {
            var descriptionView = new DescriptionView(Engage.model.get("mediaPackage"), plugin.template);
            Engage.on(plugin.events.mediaPackageModelError.getName(), function(msg) {
                mediapackageError = true;
            });
        }
    }

    if (isDesktopMode || isMobileMode) {
        // init event
        var relative_plugin_path = Engage.getPluginPath("EngagePluginDescription");

        // listen on a change/set of the mediaPackage model
        Engage.model.on(mediapackageChange, function() {
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });

        // all plugins loaded
        Engage.on(plugin.events.plugin_load_done.getName(), function() {
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });

        // load utils class
        require([relative_plugin_path + "utils"], function(utils) {
            Engage.log("Description: Utils class loaded");
            Utils = new utils();
            initTranslate(Utils.detectLanguage(), function() {
                Engage.log("Description: Successfully translated.");
                locale = translate("value_locale", locale);
                dateFormat = translate("value_dateFormatFull", dateFormat);
                initCount -= 1;
                if (initCount <= 0) {
                    initPlugin();
                }
            }, function() {
                Engage.log("Description: Error translating...");
                initCount -= 1;
                if (initCount <= 0) {
                    initPlugin();
                }
            });
        });
    }

    return plugin;
});
