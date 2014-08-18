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
/*global define, CustomEvent*/
define(['require', 'jquery', 'underscore', 'backbone', 'mousetrap', 'bowser', 'engage/engage_model', 'engage/engage_tab_logic'], function(require, $, _, Backbone, Mousetrap, Bowser, EngageModel, EngageTabLogic) {
    "use strict";

    var hotkey_jumpToX = "jumpToX",
        hotkey_nextChapter = "nextChapter",
        hotkey_fullscreen = "fullscreen",
        hotkey_jumpToBegin = "jumpToBegin",
        hotkey_prevEpisode = "prevEpisode",
        hotkey_prevChapter = "prevChapter",
        hotkey_play = "play",
        hotkey_pause = "pause",
        hotkey_mute = "mute",
        hotkey_nextEpisode = "nextEpisode",
        hotkey_volDown = "volDown",
        hotkey_volUp = "volUp";

    var events = {
        plugin_load_done: new EngageEvent("Core:plugin_load_done", "when the core loaded the event successfully", "both"),
        coreInit: new EngageEvent("Core:init", "", "trigger"),
        jumpToX: new EngageEvent("Controls:jumpToX", "", "trigger"),
        nextChapter: new EngageEvent("Video:nextChapter", "", "trigger"),
        fullscreenEnable: new EngageEvent("Video:fullscreenEnable", "", "trigger"),
        jumpToBegin: new EngageEvent("Video:jumpToBegin", "", "trigger"),
        previousEpisode: new EngageEvent("Core:previousEpisode", "", "trigger"),
        previousChapter: new EngageEvent("Video:previousChapter", "", "trigger"),
        play: new EngageEvent("Video:play", "", "trigger"),
        pause: new EngageEvent("Video:pause", "", "trigger"),
        mute: new EngageEvent("Video:mute", "", "trigger"),
        nextEpisode: new EngageEvent("Core:nextEpisode", "", "trigger"),
        volumeUp: new EngageEvent("Video:volumeUp", "", "trigger"),
        volumeDown: new EngageEvent("Video:volumeDown", "", "trigger"),
    };

    function browserSupported() {
        return (Bowser.firefox && Bowser.version >= 24) || (Bowser.chrome && Bowser.version >= 30) || (Bowser.opera && Bowser.version >= 20) || (Bowser.safari && Bowser.version >= 7);
    }

    // global private core variables
    var plugins_loaded = {};

    // theodul core init
    if (window.console) {
        console.log("Core: Init");
    }

    // event prototype
    function EngageEvent(name, description, type) {
        var name = name;
        var description = description;
        var type = type;

        this.getName = (function() {
            return name;
        });

        this.getDescription = (function() {
            return description;
        });

        this.getType = (function() {
            return type;
        });

        this.toString = (function() {
            return name;
        });
    }

    // core main
    var EngageCore = Backbone.View.extend({
        el: $("#engage_view"),
        initialize: function() {
            // the main core is our global event system
            this.dispatcher = _.clone(Backbone.Events);
            // link to the engage model
            this.model = new EngageModel();
            // listen to all events
            this.dispatcher.on("all", function(name) {
                if (engageCore.model.get("isEventDebug")) {
                    engageCore.log("Event log: '" + name + "'");
                }
            });
            this.model.browserSupported = browserSupported();
            this.model.desktopOrEmbed = false;
            // core init event
            this.dispatcher.on(events.coreInit.getName(), function() {
                // switch view template and css rules for current player mode
                // link tag for css file
                var cssLinkTag = $("<link>");
                var cssAttr = {
                    type: 'text/css',
                    rel: 'stylesheet'
                };
                // template obj
                var core_template = "none";
                // path to the require module with the view logic
                var view_logic_path = "";
                switch (engageCore.model.get("mode")) {
                    case "desktop":
                        engageCore.model.desktopOrEmbed = true;
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
                        engageCore.model.desktopOrEmbed = true;
                        cssAttr.href = 'css/core_embed_style.css';
                        core_template = "templates/core_embed.html";
                        view_logic_path = "engage/engage_embed_view"
                        break;
                }
                cssLinkTag.attr(cssAttr);
                // add css to DOM
                $("head").append(cssLinkTag);
                // load js view logic via require, see files engage_<mode>_view.js
                require([view_logic_path], function(pluginView) {
                    // link view logic to the core
                    engageCore.pluginView = pluginView;
                    // get core template
                    $.get(core_template, function(template) {
                        // set template, render it and add it to DOM
                        engageCore.template = template;
                        $(engageCore.el).html(_.template(template));
                        // run init function of the view
                        engageCore.pluginView.initView();
                        if (!engageCore.model.desktopOrEmbed || (engageCore.model.desktopOrEmbed && engageCore.model.browserSupported)) {
                            // BEGIN LOAD PLUGINS
                            // fetch plugin information
                            engageCore.model.get('pluginsInfo').fetch({
                                success: function(pluginInfos) {
                                    // load plugin as requirejs module
                                    if (pluginInfos.get('pluginlist') && pluginInfos.get('pluginlist').plugins !== undefined) {
                                        if ($.isArray(pluginInfos.get('pluginlist').plugins)) {
                                            $.each(pluginInfos.get('pluginlist').plugins, function(index, value) {
                                                var plugin_name = value['name'];
                                                plugins_loaded[plugin_name] = false;
                                            });
                                            $.each(pluginInfos.get('pluginlist').plugins, function(index, value) {
                                                // load plugin
                                                var plugin_name = value['name'];
                                                engageCore.log("Core: Loading plugin '" + plugin_name + "' from '" + ('../../../plugin/' + value['static-path'] + '/') + "'...");
                                                loadPlugin('../../../plugin/' + value['static-path'] + '/', plugin_name);
                                            });
                                        } else {
                                            // load plugin
                                            var plugin_name = value['name'];
                                            plugins_loaded[plugin_name] = false;
                                            engageCore.log("Core: Loading plugin '" + plugin_name + "' from '" + ('../../../plugin/' + value['static-path'] + '/') + "'...");
                                            loadPlugin('../../../plugin/' + pluginInfos.get('pluginlist').plugins['static-path'] + '/', plugin_name);
                                        }
                                    }
                                }
                            });
                            // END LOAD PLUGINS
                            // wait that me infos are loaded
                            while (engageCore.model.get("meInfo").ready == false) {}
                            bindHotkeysToEvents(); // bind configured hotkeys to theodul events
                        } else {
                            engageCore.trigger(events.plugin_load_done.getName());
                        }
                    });
                });
            });
            // load plugins done, hide loading and show content
            this.dispatcher.on(events.plugin_load_done.getName(), function() {
                $(".loading").hide();
                if (!engageCore.model.desktopOrEmbed || (engageCore.model.desktopOrEmbed && engageCore.model.browserSupported)) {
                    $("#browserWarning").detach();
                    $("#engage_view").show();
                } else {
                    $("#engage_view").detach();
                    $("#browserWarning").show();
                }
            });
        },
        // bind a key event as a string to given theodul event
        bindKeyToEvent: function(hotkey, event) {
            // only for EngageEvent objects
            if (event instanceof EngageEvent) {
                Mousetrap.bind(hotkey, function() {
                    engageCore.trigger(event);
                });
            }
        },
        on: function(event, handler, context) {
            if (event instanceof EngageEvent) {
                this.dispatcher.on(event.getName(), handler, context);
            } else {
                this.dispatcher.on(event, handler, context);
            }
        },
        trigger: function(event, data) {
            if (event instanceof EngageEvent) {
                this.dispatcher.trigger(event.getName(), data);
            } else {
                this.dispatcher.trigger(event, data);
            }
        },
        Event: EngageEvent,
        log: function(data) {
            if (this.model.get("isDebug") && window.console) {
                console.log(data);
            }
        },
        getPluginPath: function(pluginName) {
            var evaluated_plugin_path = '';
            var pluginsInfos = engageCore.model.get('pluginsInfo');
            var pluginList = pluginsInfos.get('pluginlist');
            if (pluginList && pluginList.plugins !== undefined) {
                var plugins = pluginList.plugins;
                if ($.isArray(plugins)) {
                    $.each(plugins, function(index, value) {
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

    // create an engage view once the document has loaded
    var engageCore = new EngageCore();
    // fire init event
    engageCore.trigger(events.coreInit.getName());

    // BEGIN Private core functions

    //binds configured hotkeys(see MH org config) to corresponding theodul events
    function bindHotkeysToEvents() {
        // process hardcoded keys
        $.each(engageCore.model.get("meInfo").get("hotkeys"), function(i, val) {
            switch (val.name) {
                case hotkey_jumpToX:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.jumpToX.getName());
                    });
                    break;
                case hotkey_nextChapter:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.nextChapter.getName());
                    });
                    break;
                case hotkey_fullscreen:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.fullscreenEnable.getName());
                    });
                    break;
                case hotkey_jumpToBegin:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.jumpToBegin.getName());
                    });
                    break;
                case hotkey_prevEpisode:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.previousEpisode.getName());
                    });
                    break;
                case hotkey_prevChapter:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.previousChapter.getName());
                    });
                    break;
                case hotkey_play:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.play.getName());
                    });
                    break;
                case hotkey_pause:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.pause.getName());
                    });
                    break;
                case hotkey_mute:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.mute.getName());
                    });
                    break;
                case hotkey_nextEpisode:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.nextEpisode.getName());
                    });
                    break;
                case hotkey_volDown:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.volumeDown.getName());
                    });
                    break;
                case hotkey_volUp:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.volumeUp.getName());
                    });
                    break;
                default:
                    break;
            }
        });
        //process custom hotkeys
        $.each(engageCore.model.get("meInfo").get("hotkeysCustom"), function(i, val) {
            Mousetrap.bind(val.key, function() {
                engageCore.trigger(val.app + ":" + val.func); // trigger custom event
            });
        });
    }

    function checkAllPluginsloaded() {
        var all_plugins_loaded = true;
        $.each(plugins_loaded, function(plugin_index, plugin_value) {
            if (plugin_value === false) {
                all_plugins_loaded = false;
            }
        });
        return all_plugins_loaded;
    }

    function loadPlugin(plugin_path, plugin_name) {
            require([plugin_path + 'main'], function(plugin) {
                // load styles in link tags via jquery
                if ($.isArray(plugin.styles)) {
                    $.each(plugin.styles, function(style_index, style_path) {
                        if (style_path !== "") {
                            var link = $("<link>");
                            link.attr({
                                type: 'text/css',
                                rel: 'stylesheet',
                                href: 'engage/theodul/' + plugin_path + style_path
                            });
                            $("head").append(link);
                        }
                    });
                } else {
                    if (plugin.styles !== "") {
                        var link = $("<link>");
                        link.attr({
                            type: 'text/css',
                            rel: 'stylesheet',
                            href: 'engage/theodul/' + plugin_path + plugin.styles
                        });
                        $("head").append(link);
                    }
                }

                if (plugin.template !== "none") {
                    // load template asynchronously
                    $.get('engage/theodul/' + plugin_path + plugin.template, function(template) {
                        // empty data object
                        var template_data = {};
                        // add template if not undefined
                        if (plugin.template_data !== undefined) {
                            template_data = plugin.template_data;
                        }
                        // add full plugin path to the tmeplate data
                        template_data.plugin_path = 'engage/theodul/' + plugin_path;
                        // process the template using underscore and set it in the plugin obj
                        plugin.templateProcessed = _.template(template, template_data);
                        plugin.template = template;
                        plugin.pluginPath = 'engage/theodul/' + plugin_path;
                        // load the compiled HTML into the component
                        engageCore.pluginView.insertPlugin(plugin);
                        // plugin load done counter
                        plugins_loaded[plugin_name] = true;
                        // check if all plugins are ready
                        if (checkAllPluginsloaded() === true) {
                            engageCore.pluginView.allPluginsLoaded();
                            // trigger done event
                            engageCore.trigger(events.plugin_load_done.getName());
                        }
                    });
                } else {
                    plugins_loaded[plugin_name] = true;
                    // check if all plugins are ready
                    if (checkAllPluginsloaded() === true) {
                        engageCore.pluginView.allPluginsLoaded();
                        // trigger done event
                        engageCore.trigger(events.plugin_load_done.getName());
                    }
                }
            });
        } // END Private core functions

    return engageCore;
});
