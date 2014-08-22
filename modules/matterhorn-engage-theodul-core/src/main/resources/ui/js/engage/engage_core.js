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
        mediaPackageModelError: new EngageEvent("MhConnection:mediaPackageModelError", "", "handler")
    };

    /* change these variables */
    var browser_minVersion_firefox = 24;
    var browser_minVersion_chrome = 30;
    var browser_minVersion_opera = 20;
    var browser_minVersion_safari = 7;
    var id_engage_view = "engage_view";
    var id_loading1 = "loading1";
    var id_loading2 = "loading2";
    var id_loadingProgressbar2 = "loadingProgressbar2";
    var id_browserWarning = "browserWarning";
    var id_volume = "volume";
    var id_btn_reloadPage = "btn_reloadPage";
    var id_customError = "customError";
    var id_customError_str = "customError_str";
    var class_loading = "loading";

    /* don't change these variables */
    var plugins_loaded = {};
    var loadingDelay1 = 500;
    var loadingDelay2 = 1000;
    var errorCheckDelay = 3500;
    var hotkey_jumpToX = "jumpToX";
    var hotkey_nextChapter = "nextChapter";
    var hotkey_fullscreen = "fullscreen";
    var hotkey_jumpToBegin = "jumpToBegin";
    var hotkey_prevEpisode = "prevEpisode";
    var hotkey_prevChapter = "prevChapter";
    var hotkey_play = "play";
    var hotkey_pause = "pause";
    var hotkey_mute = "mute";
    var hotkey_nextEpisode = "nextEpisode";
    var hotkey_volDown = "volDown";
    var hotkey_volUp = "volUp";
    var mediapackageError = false;

    function browserSupported() {
        return (Bowser.firefox && Bowser.version >= browser_minVersion_firefox) || (Bowser.chrome && Bowser.version >= browser_minVersion_chrome) || (Bowser.opera && Bowser.version >= browser_minVersion_opera) || (Bowser.safari && Bowser.version >= browser_minVersion_safari);
    }

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
        el: $("#" + id_engage_view),
        initialize: function() {
            $("." + class_loading).show();
            $("#" + id_loading1).show();
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
            this.model.desktop = false;
            this.model.embed = false;
            this.model.mobile = false;
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
                    case "mobile":
                        cssAttr.href = 'css/core_mobile_style.css';
                        core_template = "templates/core_mobile.html";
                        view_logic_path = "engage/engage_mobile_view";
                        engageCore.model.mobile = true;
                        break;
                    case "embed":
                        cssAttr.href = 'css/core_embed_style.css';
                        core_template = "templates/core_embed.html";
                        view_logic_path = "engage/engage_embed_view";
                        engageCore.model.embed = true;
                        break;
                    case "desktop":
                    default:
                        cssAttr.href = 'css/core_desktop_style.css';
                        core_template = "templates/core_desktop.html";
                        view_logic_path = "engage/engage_desktop_view";
                        engageCore.model.desktop = true;
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
                        if (!(engageCore.model.desktop || engageCore.model.embed) || ((engageCore.model.desktop || engageCore.model.embed) && engageCore.model.browserSupported)) {
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
            this.dispatcher.on(events.mediaPackageModelError.getName(), function(str) {
                mediapackageError = true;
                $("." + class_loading).hide().detach();
                $("#" + id_engage_view + ", #" + id_btn_reloadPage).hide().detach();
                $("#" + id_customError_str).html(str);
                $("#" + id_customError).show();
            });
            // load plugins done, hide loading and show content
            this.dispatcher.on(events.plugin_load_done.getName(), function() {
                if (!mediapackageError) {
                    $("#" + id_loading1).hide().detach();
                    $("#" + id_loading2).show();
                    window.setTimeout(function() {
                        $("#" + id_loadingProgressbar2).css("width", "100%");
                        window.setTimeout(function() {
                            $("." + class_loading).hide().detach();
                            if (!(engageCore.model.desktop || engageCore.model.embed) || ((engageCore.model.desktop || engageCore.model.embed) && engageCore.model.browserSupported)) {
                                $("#" + id_browserWarning).hide().detach();
                                $("#" + id_engage_view).show();
                                if (engageCore.model.desktop) {
                                    window.setTimeout(function() {
                                        if ($("#" + id_volume).html() == "") {
                                            $("#" + id_btn_reloadPage).click(function(e) {
                                                e.preventDefault();
                                                location.reload();
                                            });
                                            $("#" + id_engage_view).hide().detach();
                                            $("#" + id_customError).show();
                                        } else {
                                            $("#" + id_customError).detach();
                                        }
                                    }, errorCheckDelay);
                                }
                            } else {
                                $("#" + id_engage_view + ", #" + id_customError).hide().detach();
                                $("#" + browserWarning).show();
                            }
                        }, loadingDelay2);
                    }, loadingDelay1);
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
                        engageCore.trigger(events.play.getName(), false);
                    });
                    break;
                case hotkey_pause:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.pause.getName(), false);
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
