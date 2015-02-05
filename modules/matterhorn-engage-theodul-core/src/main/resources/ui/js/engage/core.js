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
define(["require", "jquery", "underscore", "backbone", "mousetrap", "bowser", "basil", "bootbox", "engage/models/engage", "engage/event"], function(require, $, _, Backbone, Mousetrap, Bowser, Basil, Bootbox, EngageModel, EngageEvent) {
    "use strict";

    var events = {
        plugin_load_done: new EngageEvent("Core:plugin_load_done", "when the core loaded the event successfully", "both"),
        coreInit: new EngageEvent("Core:init", "", "trigger"),
        nextChapter: new EngageEvent("Video:nextChapter", "", "trigger"),
        fullscreenEnable: new EngageEvent("Video:fullscreenEnable", "", "trigger"),
        fullscreenCancel: new EngageEvent("Video:fullscreenCancel", "", "trigger"),
        seek: new EngageEvent("Video:seek", "seek video to a given position in seconds", "trigger"),
        previousChapter: new EngageEvent("Video:previousChapter", "", "trigger"),
        playPause: new EngageEvent("Video:playPause", "", "trigger"),
        mute: new EngageEvent("Video:mute", "", "trigger"),
        volumeUp: new EngageEvent("Video:volumeUp", "", "trigger"),
        volumeDown: new EngageEvent("Video:volumeDown", "", "trigger"),
        customSuccess: new EngageEvent("Notification:customSuccess", "a custom success message", "trigger"),
        customError: new EngageEvent("Notification:customError", "an error occurred", "trigger"),
        seekLeft: new EngageEvent("Video:seekLeft", "", "trigger"),
        seekRight: new EngageEvent("Video:seekRight", "", "trigger"),
        playbackRateIncrease: new EngageEvent("Video:playbackRateIncrease", "", "trigger"),
        playbackRateDecrease: new EngageEvent("Video:playbackRateDecrease", "", "trigger"),
        mediaPackageModelError: new EngageEvent("MhConnection:mediaPackageModelError", "", "handler")
    };

    /* change these variables */
    var browser_minVersion_firefox = 24;
    var browser_minVersion_chrome = 30;
    var browser_minVersion_opera = 20;
    var browser_minVersion_safari = 7;
    var browser_minVersion_msie = 9;
    var path_language_de = "language/de.json";
    var path_language_en = "language/en.json";

    /* don't change these variables */
    var setCustomError = false; // just for displaying purposes!
    var pluginControlsInserted = false;
    var pluginVideoInserted = false;
    var pluginTabInserted = false;
    var pluginDescriptionInserted = false;
    var pluginTimelineInserted = false;
    var id_str_error = "str_error";
    var id_customError_str = "customError_str";
    var id_str_reloadPage = "str_reloadPage";
    var id_str_login = "str_login";
    var id_engage_view = "engage_view";
    var id_loading1 = "loading1";
    var id_loading2 = "loading2";
    var id_loadingProgressbar2 = "loadingProgressbar2";
    var id_browserWarning = "browserWarning";
    var id_volume = "volume";
    var id_btn_reloadPage = "btn_reloadPage";
    var id_btn_login = "btn_login";
    var id_btn_tryAnyway = "btn_tryAnyway";
    var id_customError = "customError";
    var id_customError_str = "customError_str";
    var class_loading = "loading";
    var plugins_loaded = {};
    var loadingDelay1 = 500;
    var loadingDelay2 = 1000;
    var errorCheckDelay = 3500;
    var mediapackageError = false;
    var numberOfPlugins = 0;
    var translationData = null;
    var loggedIn = false;
    var username = "Anonymous";
    var askedForLogin = false;
    var springSecurityLoginURL = "/j_spring_security_check";
    var springLoggedInStrCheck = "<title>Opencast Matterhorn – Login Page</title>";
    // shortcuts
    var shortcut_playPause = "playPause";
    var shortcut_seekLeft = "seekLeft";
    var shortcut_seekRight = "seekRight";
    var shortcut_playbackrateIncrease = "playbackrateIncrease";
    var shortcut_playbackrateDecrease = "playbackrateDecrease";
    var shortcut_mute = "mute";
    var shortcut_volDown = "volDown";
    var shortcut_volUp = "volUp";
    var shortcut_fullscreenEnable = "fullscreenEnable";
    var shortcut_fullscreenCancel = "fullscreenCancel";
    var shortcut_jumpToBegin = "jumpToBegin";
    var shortcut_prevChapter = "prevChapter";
    var shortcut_nextChapter = "nextChapter";

    var basilOptions = {
        namespace: "mhStorage"
    };
    Basil = new window.Basil(basilOptions);

    function browserSupported() {
        if ((Basil.get("overrideBrowser") != null) && Basil.get("overrideBrowser")) {
            // console.log("Core: User setting - Support unsupported browser: " + Basil.get("overrideBrowser"));
            return true;
        }
        return (Bowser.firefox && Bowser.version >= browser_minVersion_firefox) ||
            (Bowser.chrome && Bowser.version >= browser_minVersion_chrome) ||
            (Bowser.opera && Bowser.version >= browser_minVersion_opera) ||
            (Bowser.safari && Bowser.version >= browser_minVersion_safari) ||
            (Bowser.msie && Bowser.version >= browser_minVersion_msie);
    }

    function detectLanguage() {
        return navigator.language || navigator.userLanguage || navigator.browserLanguage || navigator.systemLanguage || "en";
    }

    function initTranslate(language) {
        var jsonstr = "";

        if (language == "de") {
            // console.log("Core: Chosing german translations");
            jsonstr += path_language_de;
        } else { // No other languages supported, yet
            // console.log("Core: Chosing english translations");
            jsonstr += path_language_en;
        }
        $.ajax({
            url: jsonstr,
            dataType: "json",
            async: false,
            success: function(data) {
                if (data) {
                    data.value_locale = language;
                    translationData = data;
                }
            }
        });
    }

    function translateCoreHTML() {
        $("#" + id_str_error).html(translate("error", "Error"));
        if (!setCustomError) {
            $("#" + id_customError_str).html(translate("error_unknown", "An error occurred. Please reload the page."));
            setCustomError = false;
        }
        $("#" + id_str_reloadPage).html(translate("reloadPage", "Reload page"));
        $("#" + id_str_login).html(translate("login", "Log in"));
    }

    function translate(str, strIfNotFound) {
        if (!str || (str.length <= 0)) {
            return strIfNotFound;
        }
        return ((translationData != null) && (translationData[str] != undefined)) ? translationData[str] : strIfNotFound;
    }

    /*
    if (window.console) {
        console.log("Core: Init");
    }
    */

    function login() {
        if (!askedForLogin) {
            askedForLogin = true;
            var username = "User";
            var password = "Password";

            Bootbox.dialog({
                title: translate("login", "Log in"),
                message: '<form class="form-signin">' +
                    '<h2 class="form-signin-heading">' + translate("enterUsernamePassword", "Please enter your username and password") + '</h2>' +
                    '<input id="username" type="text" class="form-control form-control-custom" name="username" placeholder="' + translate("username", "Username") + '" required="true" autofocus="" />' +
                    '<input id="password" type="password" class="form-control form-control-custom" name="password" placeholder="' + translate("password", "Password") + '" required="true" />' +
                    '<label class="checkbox">' +
                    '<input type="checkbox" value="' + translate("rememberMe", "Remember me") + '" id="rememberMe" name="rememberMe" checked> ' + translate("rememberMe", "Remember me") +
                    '</label>' +
                    '</form>',
                buttons: {
                    cancel: {
                        label: translate("cancel", "Cancel"),
                        className: "btn-default",
                        callback: function() {
                            askedForLogin = false;
                        }
                    },
                    login: {
                        label: translate("login", "Log in"),
                        className: "btn-success",
                        callback: function() {
                            var username = $("#username").val().trim();
                            var password = $("#password").val().trim();
                            if ((username !== null) && (username.length > 0) && (password !== null) && (password.length > 0)) {
                                $.ajax({
                                    type: "POST",
                                    url: springSecurityLoginURL,
                                    data: {
                                        "j_username": username,
                                        "j_password": password,
                                        "_spring_security_remember_me": $("#rememberMe").is(":checked")
                                    }
                                }).done(function(msg) {
                                    password = "";
                                    if (msg.indexOf(springLoggedInStrCheck) == -1) {
                                        engageCore.trigger(events.customSuccess.getName(), translate("loginSuccessful", "Successfully logged in. Please reload the page if the page does not reload automatically."));
                                        $("#" + id_btn_login).hide();
                                        $("#" + id_btn_reloadPage).click(function(e) {
                                            e.preventDefault();
                                            location.reload();
                                        });
                                        $("#" + id_btn_reloadPage).show();
                                        location.reload();
                                    } else {
                                        engageCore.trigger(events.customError.getName(), translate("loginFailed", "Failed to log in."));
                                        setCustomError = true;
                                    }
                                    askedForLogin = false;
                                }).fail(function(msg) {
                                    password = "";
                                    engageCore.trigger(events.customError.getName(), translate("loginFailed", "Failed to log in."));
                                    setCustomError = true;
                                    askedForLogin = false;
                                });
                            } else {
                                askedForLogin = false;
                            }
                        }
                    }
                },
                className: "usernamePassword-modal",
                onEscape: function() {
                    askedForLogin = false;
                },
                closeButton: false
            });
        }
    }

    function getLoginStatus() {
        if (engageCore.model.get("infoMe")) {
            if (engageCore.model.get("infoMe").loggedIn) {
                loggedIn = true;
                username = engageCore.model.get("infoMe").username;
            } else {
                loggedIn = false;
                username = "Anonymous";
            }
            return loggedIn ? 1 : 0;
        }
        return -1;
    }

    // binds configured shortcuts (see MH org config) to corresponding events
    function bindShortcutsToEvents() {
        // disable scrolling when pressing the space bar
        $(document).keydown(function(e) {
            // space = 32, backspace = 8, page up = 73, page down = 81, enter = 10, carriage return = 13
            if ((e.keyCode == 32) || (e.keyCode == 8) || (e.keyCode == 73) || (e.keyCode == 81) || (e.keyCode == 10) || (e.keyCode == 13)) {
                return false;
            }
        });
        // process hardcoded keys
        $.each(engageCore.model.get("meInfo").get("shortcuts"), function(i, val) {
            switch (val.name) {
                case shortcut_seekLeft:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.seekLeft.getName());
                    });
                    break;
                case shortcut_seekRight:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.seekRight.getName());
                    });
                    break;
                case shortcut_playbackrateIncrease:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.playbackRateIncrease.getName());
                    });
                    break;
                case shortcut_playbackrateDecrease:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.playbackRateDecrease.getName());
                    });
                    break;
                case shortcut_nextChapter:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.nextChapter.getName());
                    });
                    break;
                case shortcut_fullscreenEnable:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.fullscreenEnable.getName());
                    });
                    break;
                case shortcut_fullscreenCancel:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.fullscreenCancel.getName());
                    });
                    break;
                case shortcut_jumpToBegin:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.seek.getName(), 0);
                    });
                    break;
                case shortcut_prevChapter:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.previousChapter.getName());
                    });
                    break;
                case shortcut_playPause:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.playPause.getName());
                    });
                    break;
                case shortcut_mute:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.mute.getName());
                    });
                    break;
                case shortcut_volDown:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.volumeDown.getName());
                    });
                    break;
                case shortcut_volUp:
                    Mousetrap.bind(val.key, function() {
                        engageCore.trigger(events.volumeUp.getName());
                    });
                    break;
                default:
                    break;
            }
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
        require([plugin_path + "main"], function(plugin) {
            // load styles in link tags via jquery
            if ($.isArray(plugin.styles)) {
                $.each(plugin.styles, function(style_index, style_path) {
                    if (style_path !== "") {
                        var link = $("<link>");
                        link.attr({
                            type: "text/css",
                            rel: "stylesheet",
                            href: "engage/theodul/" + plugin_path + style_path
                        });
                        $("head").append(link);
                    }
                });
            } else {
                if (plugin.styles !== "") {
                    var link = $("<link>");
                    link.attr({
                        type: "text/css",
                        rel: "stylesheet",
                        href: "engage/theodul/" + plugin_path + plugin.styles
                    });
                    $("head").append(link);
                }
            }

            if (plugin.template !== "none") {
                // load template asynchronously
                $.get("engage/theodul/" + plugin_path + plugin.template, function(template) {
                    // empty data object
                    var template_data = {};
                    // add template if not undefined
                    if (plugin.template_data !== undefined) {
                        template_data = plugin.template_data;
                    }
                    // add full plugin path to the tmeplate data
                    template_data.plugin_path = "engage/theodul/" + plugin_path;
                    // process the template using underscore and set it in the plugin obj
                    plugin.templateProcessed = _.template(template, template_data);
                    plugin.template = template;
                    plugin.pluginPath = "engage/theodul/" + plugin_path;
                    plugin.insertIntoDOM = plugin.insertIntoDOM ? true : false;
                    if (plugin.insertIntoDOM) {
                        // load the compiled HTML into the component
                        engageCore.pluginView.insertPlugin(plugin, plugin_name, translationData);
                        if (engageCore.model.desktop) {
                            if (engageCore.pluginView.isControlsPlugin(plugin.type)) {
                                pluginControlsInserted = true;
                            }
                            if (engageCore.pluginView.isVideoPlugin(plugin.type)) {
                                pluginVideoInserted = true;
                            }
                            if (engageCore.pluginView.isTabPlugin(plugin.type)) {
                                pluginTabInserted = true;
                            }
                            if (engageCore.pluginView.isDescriptionPlugin(plugin.type)) {
                                pluginDescriptionInserted = true;
                            }
                            if (engageCore.pluginView.isTimelinePlugin(plugin.type)) {
                                pluginTimelineInserted = true;
                            }
                        }
                    }
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
    }

    // core main
    var EngageCore = Backbone.View.extend({
        el: $("#" + id_engage_view),
        Event: EngageEvent,
        // bind a key event as a string to given event
        bindKeyToEvent: function(shortcuts, event) {
            // only for EngageEvent objects
            if (event instanceof EngageEvent) {
                Mousetrap.bind(shortcut, function() {
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
        log: function(data) {
            if (this.model.get("isDebug") && window.console) {
                console.log(data);
            }
        },
        getPluginPath: function(pluginName) {
            var evaluated_plugin_path = "";
            var pluginsInfos = engageCore.model.get("pluginsInfo");
            if (pluginsInfos) {
                var pluginList = pluginsInfos.get("pluginlist");
                if (pluginList && pluginList.plugins !== undefined) {
                    var plugins = pluginList.plugins;
                    if ($.isArray(plugins)) {
                        $.each(plugins, function(index, value) {
                            if (value["name"] === pluginName) {
                                evaluated_plugin_path = "../../../plugin/" + value["static-path"] + "/";
                            }
                        });
                    } else {
                        evaluated_plugin_path = "../../../plugin/" + plugins["static-path"] + "/";
                    }
                }
            }
            return evaluated_plugin_path;
        },
        initialize: function() {
            $("." + class_loading).show();
            $("#" + id_loading1).show();
            initTranslate(detectLanguage());
            // the main core is our global event system
            this.dispatcher = _.clone(Backbone.Events);
            // link to the engage model
            this.model = new EngageModel();
            // listen to all events
            this.dispatcher.on("all", function(name) {
                if (engageCore.model.get("isEventDebug") && window.console) {
                    console.log("[Core:Event] '" + name + "'");
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
                    type: "text/css",
                    rel: "stylesheet"
                };
                // template obj
                var core_template = "none";
                // path to the require module with the view logic
                var view_logic_path = "";
                switch (engageCore.model.get("mode")) {
                    case "mobile":
                        cssAttr.href = "css/core_mobile_style.css";
                        core_template = "templates/core_mobile.html";
                        view_logic_path = "engage/views/mobile";
                        engageCore.model.mobile = true;
                        break;
                    case "embed":
                        cssAttr.href = "css/core_embed_style.css";
                        core_template = "templates/core_embed.html";
                        view_logic_path = "engage/views/embed";
                        engageCore.model.embed = true;
                        break;
                    case "desktop":
                    default:
                        cssAttr.href = "css/core_desktop_style.css";
                        core_template = "templates/core_desktop.html";
                        view_logic_path = "engage/views/desktop";
                        engageCore.model.desktop = true;
                        break;
                }
                cssLinkTag.attr(cssAttr);
                // add css to DOM
                $("head").append(cssLinkTag);
                // load js view logic via require, see files in views/
                require([view_logic_path], function(pluginView) {
                    // link view logic to the core
                    engageCore.pluginView = pluginView;
                    // get core template
                    $.get(core_template, function(template) {
                        // set template, render it and add it to DOM
                        engageCore.template = template;
                        $(engageCore.el).html(_.template(template)).trigger("create"); // variables do not work in here!
                        // run init function of the view
                        engageCore.pluginView.initView();
                        if (engageCore.model.get("pluginsInfo") && engageCore.model.mobile || !(engageCore.model.desktop || engageCore.model.embed) || ((engageCore.model.desktop || engageCore.model.embed) && engageCore.model.browserSupported)) {
                            // BEGIN LOAD PLUGINS
                            // fetch plugin information
                            engageCore.model.get("pluginsInfo").fetch({
                                success: function(pluginInfos) {
                                    // load plugin as requirejs module
                                    if ((pluginInfos.get("pluginlist").plugins != undefined) && pluginInfos.get("pluginlist")) {
                                        numberOfPlugins = pluginInfos.get("pluginlist").plugins.length;
                                        if ($.isArray(pluginInfos.get("pluginlist").plugins)) {
                                            $.each(pluginInfos.get("pluginlist").plugins, function(index, value) {
                                                var plugin_name = value["name"];
                                                plugins_loaded[plugin_name] = false;
                                            });
                                            $.each(pluginInfos.get("pluginlist").plugins, function(index, value) {
                                                // load plugin
                                                var plugin_name = value["name"];
                                                engageCore.log("Core: Loading plugin '" + plugin_name + "' from '" + ("../../../plugin/" + value["static-path"] + "/") + "'...");
                                                loadPlugin("../../../plugin/" + value["static-path"] + "/", plugin_name);
                                            });
                                        } else {
                                            // load plugin
                                            var plugin_name = pluginInfos.get("pluginlist").plugins["name"];
                                            plugins_loaded[plugin_name] = false;
                                            engageCore.log("Core: Loading plugin '" + plugin_name + "' from '" + ("../../../plugin/" + pluginInfos.get("pluginlist").plugins["static-path"] + "/") + "'...");
                                            loadPlugin("../../../plugin/" + pluginInfos.get("pluginlist").plugins["static-path"] + "/", plugin_name);
                                        }
                                    }
                                }
                            });
                            // END LOAD PLUGINS
                            // wait that me infos are loaded
                            while (engageCore.model.get("meInfo").ready == false) {}
                            bindShortcutsToEvents(); // bind configured shortcuts to events
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
                $("#" + id_engage_view).hide().detach();
                $("#" + id_btn_reloadPage).hide();
                $("#" + id_customError_str).html(str);
                setCustomError = true;
                if (getLoginStatus() == 0) {
                    $("#" + id_btn_login).click(login);
                    $("#" + id_customError + ", #" + id_btn_login).show();
                } else {
                    // TODO: Logged in as...
                    $("#" + id_btn_login).hide();
                }
            });
            // load plugins done, hide loading and show content
            this.dispatcher.on(events.plugin_load_done.getName(), function() {
                if (engageCore.model.desktop) {
                    if (!pluginControlsInserted) {
                        engageCore.log("Core: No controls plugin inserted. Removing the container.");
                        engageCore.pluginView.removeControls();
                    }
                    if (!pluginVideoInserted) {
                        engageCore.log("Core: No video plugin inserted. Removing the container.");
                        engageCore.pluginView.removeVideo();
                    }
                    if (!pluginTabInserted) {
                        engageCore.log("Core: No tab plugin inserted. Removing the container.");
                        engageCore.pluginView.removeTab();
                    }
                    if (!pluginDescriptionInserted) {
                        engageCore.log("Core: No description plugin inserted. Removing the container.");
                        engageCore.pluginView.removeDescription();
                    }
                    if (!pluginTimelineInserted) {
                        engageCore.log("Core: No timeline plugin inserted. Removing the container.");
                        engageCore.pluginView.removeTimeline();
                    }
                }

                $("#" + id_loading1).hide().detach();
                $("#" + id_loading2).show();
                window.setTimeout(function() {
                    $("#" + id_loadingProgressbar2).css("width", "100%");
                    window.setTimeout(function() {
                        $("." + class_loading).hide().detach();
                        if (engageCore.model.browserSupported) {
                            $("#" + id_browserWarning).hide().detach();
                            $("#" + id_engage_view).show();
                            translateCoreHTML();
                            if (engageCore.model.desktop) {
                                window.setTimeout(function() {
                                    if ($("#" + id_volume).html() === undefined) {
                                        $("#" + id_btn_reloadPage).click(function(e) {
                                            e.preventDefault();
                                            location.reload();
                                        });
                                        $("#" + id_engage_view).hide().detach();
                                        $("body").css("min-width", "");
                                        $("#" + id_customError).show();
                                    } else {
                                        $("#" + id_customError + ", #" + id_btn_login).hide().detach();
                                    }
                                }, errorCheckDelay);
                            }
                            // TODO: Error/loading checks for embed and mobile
                        } else {
                            $("#" + id_engage_view + ", #" + id_customError).hide().detach();
                            $("body").css("min-width", "");
                            $("#" + id_browserWarning).show();
                            $("#" + id_btn_tryAnyway).click(function(e) {
                                e.preventDefault();
                                window.open(window.location.href + "&browser=all");
                            });
                        }
                    }, loadingDelay2);
                }, loadingDelay1);
            });
        }
    });

    var engageCore = new EngageCore();
    engageCore.trigger(events.coreInit.getName());
    return engageCore;
});
