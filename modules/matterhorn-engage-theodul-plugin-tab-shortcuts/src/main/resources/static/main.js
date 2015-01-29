4
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
define(["jquery", "underscore", "backbone", "engage/core"], function($, _, Backbone, Engage) {
    "use strict";

    var insertIntoDOM = true;
    var PLUGIN_NAME = "Shortcuts";
    var PLUGIN_TYPE = "engage_tab";
    var PLUGIN_VERSION = "1.0";
    var PLUGIN_TEMPLATE_DESKTOP = "templates/desktop.html";
    var PLUGIN_TEMPLATE_MOBILE = "templates/mobile.html";
    var PLUGIN_TEMPLATE_EMBED = "templates/embed.html";
    var PLUGIN_STYLES_DESKTOP = [
        "styles/desktop.css"
    ];
    var PLUGIN_STYLES_MOBILE = [
        "styles/mobile.css"
    ];
    var PLUGIN_STYLES_EMBED = [
        "styles/embed.css"
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
    var shortcuts = new Array();
    var initCount = 4;
    var infoMeChange = "change:infoMe";
    var mediapackageChange = "change:mediaPackage";
    var translations = new Array();
    var mediapackageError = false;
    var shortcutsParsed = false;
    var Utils;

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath("EngagePluginTabShortcuts").replace(/(\.\.\/)/g, "");
        var jsonstr = window.location.origin + "/engage/theodul/" + path; // this solution is really bad, fix it...

        if (language == "de") {
            Engage.log("Tab:Shortcuts: Chosing german translations");
            jsonstr += "language/de.json";
        } else { // No other languages supported, yet
            Engage.log("Tab:Shortcuts: Chosing english translations");
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

    var ShortcutsTabView = Backbone.View.extend({
        initialize: function(mediaPackageModel, template) {
            this.setElement($(plugin.container));
            this.model = mediaPackageModel;
            this.template = template;
            // bind the render function always to the view
            _.bindAll(this, "render");
            // listen for changes of the model and bind the render function to this
            this.model.bind("change", this.render);
        },
        render: function() {
            if (!mediapackageError) {
                prepareShortcuts();
                var tempVars = {
                    str_displayShortcuts: translate("displayShortcuts", "Display shortcuts"),
                    str_noShortcutsAvailable: translate("noShortcutsAvailable", "No shortcuts available"),
                    str_shortcutName: translate(name, "Shortcut name"),
                    str_shortcut: translate(name, "Shortcut"),
                    shortcuts: shortcuts
                };
                // compile template and load into the html
                this.$el.html(_.template(this.template, tempVars));
            }
        }
    });

    function translateKeyboardCombination(comb, split) {
        if (comb.indexOf(split) != -1) {
            var spl = comb.split(split);
            var ret = "";
            for (var i = 0; i < spl.length; ++i) {
                if (i != 0) {
                    ret += "+";
                }
                // filter the mod modifier
                if (spl[i] == "mod") {
                    if (navigator.platform && (navigator.platform.indexOf("Mac") != -1)) {
                        ret += translate("modMac", "cmd");
                    } else {
                        ret += translate("modOther", "ctrl");
                    }
                } else {
                    ret += translate(spl[i], spl[i]);
                }
            }
            return ret;
        }
        return translate(comb, comb);
    }

    function prepareShortcuts() {
        if (!shortcutsParsed) {
            var scuts = Engage.model.get("meInfo").get("hotkeys");
            if (scuts) {
                scuts.sort(Utils.shortcutsCompare);
                $.each(scuts, function(i, v) {
                    shortcuts.push({
                        name: translate(v.name, v.name),
                        val: translateKeyboardCombination(v.key, "+")
                    });
                });
                shortcutsParsed = true;
            }
        }
    }

    function initPlugin() {
        // only init if plugin template was inserted into the DOM
        if (isDesktopMode && plugin.inserted) {
            // create a new view with the media package model and the template
            var shortcutsTabView = new ShortcutsTabView(Engage.model.get("mediaPackage"), plugin.template);
            Engage.on(plugin.events.mediaPackageModelError.getName(), function(msg) {
                mediapackageError = true;
            });
        }
    }

    if (isDesktopMode) {
        // init event
        Engage.log("Tab:Shortcuts: Init");
        var relative_plugin_path = Engage.getPluginPath("EngagePluginTabShortcuts");

        // load utils class
        require([relative_plugin_path + "utils"], function(utils) {
            Engage.log("Tab:Shortcuts: Utils class loaded");
            Utils = new utils();
            initTranslate(Utils.detectLanguage(), function() {
                Engage.log("Tab:Shortcuts: Successfully translated.");
                initCount -= 1;
                if (initCount <= 0) {
                    initPlugin();
                }
            }, function() {
                Engage.log("Tab:Shortcuts: Error translating...");
                initCount -= 1;
                if (initCount <= 0) {
                    initPlugin();
                }
            });
        });

        // listen on a change/set of the mediaPackage model
        Engage.model.on(mediapackageChange, function() {
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });

        // listen on a change/set of the infoMe model
        Engage.model.on(infoMeChange, function() {
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });

        // all plugins loaded
        Engage.on(plugin.events.plugin_load_done.getName(), function() {
            Engage.log("Tab:Shortcuts: Plugin load done");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });
    }

    return plugin;
});
