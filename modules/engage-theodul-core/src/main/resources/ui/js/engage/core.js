/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*jslint browser: true, nomen: true*/
/*global define, CustomEvent*/
define(["require", "jquery", "underscore", "backbone", "mousetrap", "bowser", "basil", "bootbox", "mousewheel", "engage/models/engage", "engage/event"], function (require, $, _, Backbone, Mousetrap, Bowser, Basil, Bootbox, Mousewheel, EngageModel, EngageEvent) {
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
    muteToggle: new EngageEvent("Video:muteToggle", "", "trigger"),
    volumeUp: new EngageEvent("Video:volumeUp", "", "trigger"),
    volumeDown: new EngageEvent("Video:volumeDown", "", "trigger"),
    customSuccess: new EngageEvent("Notification:customSuccess", "a custom success message", "trigger"),
    customError: new EngageEvent("Notification:customError", "an error occurred", "trigger"),
    seekLeft: new EngageEvent("Video:seekLeft", "", "trigger"),
    seekRight: new EngageEvent("Video:seekRight", "", "trigger"),
    playbackRateIncrease: new EngageEvent("Video:playbackRateIncrease", "", "trigger"),
    playbackRateDecrease: new EngageEvent("Video:playbackRateDecrease", "", "trigger"),
    autoplay: new EngageEvent("Video:autoplay", "", "trigger"),
    initialSeek: new EngageEvent("Video:initialSeek", "", "trigger"),
    mediaPackageModelError: new EngageEvent("MhConnection:mediaPackageModelError", "", "handler"),
    focusVideo: new EngageEvent("Video:focusVideo", "increases the size of one video", "handler"),
    movePiP: new EngageEvent("Video:movePiP", "moves the smaller picture over the larger to the different corners", "handler"),
    togglePiP: new EngageEvent("Video:togglePiP", "switches between PiP and next to each other layout", "handler"),
    moveUp: new EngageEvent("Video:moveUp", "moves video up", "trigger"),
    moveDown: new EngageEvent("Video:moveDown", "moves video down", "trigger"),
    moveLeft: new EngageEvent("Video:moveLeft", "moves video left", "trigger"),
    moveRight: new EngageEvent("Video:moveRight", "moves video right", "trigger"),
    moveHorizontal: new EngageEvent("Video:moveHorizontal", "move video horizontal", "trigger"),
    moveVertical: new EngageEvent("Video:moveVertical", "move video vertical", "trigger"),
    zoomIn: new EngageEvent("Video:zoomIn", "zooms in video", "trigger"),
    zoomOut: new EngageEvent("Video:zoomOut", "zooms out video", "trigger")
  };

  /* change these variables */
  var browser_minVersion_firefox = 24;
  var browser_minVersion_chrome = 30;
  var browser_minVersion_opera = 20;
  var browser_minVersion_safari = 7;
  var browser_minVersion_msie = 11;
  var browser_minVersion_msedge = 13;
  var zoom_wasd_step_size = 15;

  /* don't change these variables */
  var MeInfoModel;
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
  var id_volumeSlider = "volumeSlider";
  var id_btn_reloadPage = "btn_reloadPage";
  var id_btn_login = "btn_login";
  var id_btn_tryAnyway = "btn_tryAnyway";
  var id_customError = "customError";
  var id_min_firefox_version = "min-firefox-version";
  var id_min_chrome_version = "min-chrome-version";
  var id_min_opera_version = "min-opera-version";
  var id_min_safari_version = "min-safari-version";
  var id_min_msie_version = "min-msie-version";
  var id_min_msedge_version = "min-msedge-version";
  var class_loading = "loading";
  var plugins_loaded = {};
  var loadingDelay1 = 500;
  var loadingDelay2 = 1000;
  var errorCheckDelay = 15000;
  var mediapackageError = false;
  var numberOfPlugins = 0;
  var translationData = null;
  var loggedIn = false;
  var username = "Anonymous";
  var pip = true;
  var pipPos = "left";
  var askedForLogin = false;
  var springSecurityLoginURL = "/j_spring_security_check";
  var springLoggedInStrCheck = "<title>Opencast – Login Page</title>";
  // shortcuts
  var shortcut_playPause = "controls.playPause";
  var shortcut_seekLeft = "controls.seekLeft";
  var shortcut_seekRight = "controls.seekRight";
  var shortcut_playbackrateIncrease = "playbackrate.increase";
  var shortcut_playbackrateDecrease = "playbackrate.decrease";
  var shortcut_muteToggle = "volume.muteToggle";
  var shortcut_volDown = "volume.down";
  var shortcut_volUp = "volume.up";
  var shortcut_fullscreenEnable = "fullscreen.enable";
  var shortcut_fullscreenCancel = "fullscreen.cancel";
  var shortcut_jumpToBegin = "controls.jumpToBegin";
  var shortcut_prevChapter = "controls.prevChapter";
  var shortcut_nextChapter = "controls.nextChapter";
  var shortcut_prevFocus = "layout.focusPrev";
  var shortcut_nextFocus = "layout.focusNext";
  var shortcut_movePiP = "layout.movePiP";
  var shortcut_togglePiP = "layout.togglePiP";
  var shortcut_moveLeft = "zoom.moveLeft";
  var shortcut_moveRight = "zoom.moveRight";
  var shortcut_moveUp = "zoom.moveUp";
  var shortcut_moveDown = "zoom.moveDown";
  var shortcut_zoomIn = "zoom.in";
  var shortcut_zoomOut = "zoom.out";

  var basilOptions = {
    namespace: "mhStorage"
  };
  Basil = new window.Basil(basilOptions);

  function browserSupported() {
    if ((Basil.get("overrideBrowser") != null) && Basil.get("overrideBrowser")) {
      return true;
    }
    return (Bowser.firefox && Bowser.version >= browser_minVersion_firefox) ||
        (Bowser.chrome && Bowser.version >= browser_minVersion_chrome) ||
        (Bowser.opera && Bowser.version >= browser_minVersion_opera) ||
        (Bowser.safari && Bowser.version >= browser_minVersion_safari) ||
        (Bowser.msedge && Bowser.version >= browser_minVersion_msedge) ||
        (Bowser.msie && Bowser.version >= browser_minVersion_msie) ||
        (Bowser.mobile) || (Bowser.ios);
  }

  function setMinBrowserVersions() {
      $("#" + id_min_firefox_version).text(browser_minVersion_firefox);
      $("#" + id_min_chrome_version).text(browser_minVersion_chrome);
      $("#" + id_min_opera_version).text(browser_minVersion_opera);
      $("#" + id_min_safari_version).text(browser_minVersion_safari);
      $("#" + id_min_msedge_version).text(browser_minVersion_msedge);
      $("#" + id_min_msie_version).text(browser_minVersion_msie);
  }

  function detectLanguage() {
    return navigator.language || navigator.userLanguage || navigator.browserLanguage || navigator.systemLanguage || "en";
  }

  function getDefaultLanguage(language) {
      switch (language) {
          case "de": return "de-DE";
          case "da": return "da-DK";
          case "el": return "el-GR";
          case "en": return "en-US";
          case "es": return "es-ES";
          case "fr": return "fr-FR";
          case "gl": return "gl-ES";
          case "he": return "he-IL";
          case "it": return "it-IT";
          case "nl": return "nl-NL";
          case "pl": return "pl-PL";
          case "sl": return "sl-SI";
          case "sv": return "sv-SE";
          case "tr": return "tr-TR";
	  case "zh": return "zh-CN";
          default: return null;
      }
  }

  function initTranslate(language) {
    var jsonstr = "";
    var selectedLanguage = language;
    if (getDefaultLanguage(language) !== null) {
        selectedLanguage = getDefaultLanguage(language);
    }
    console.log("Controls: selecting language " + selectedLanguage);
    jsonstr += "language/" + selectedLanguage + ".json";
    $.ajax({
      url: jsonstr,
      dataType: "json",
      success: function (data) {
        if (data) {
          data.value_locale = selectedLanguage;
          translationData = data;
          engageCore.model.set("language", selectedLanguage);
        }
      }
    }).fail(function(msg) {
        engageCore.log("No language file found for " + selectedLanguage + ".");
        if (language !== getDefaultLanguage(language.replace(/\-.*/, '')) && language !== language.replace(/\-.*/, '')) {
           initTranslate(language.replace(/\-.*/, ''));
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

  function login() {
    if (!askedForLogin) {
      askedForLogin = true;
      var username = "User";
      var password = "Password";

      Bootbox.dialog({
        title: translate("loginInformation", "Log in"),
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
            callback: function () {
              askedForLogin = false;
            }
          },
          login: {
            label: translate("login", "Log in"),
            className: "btn-success",
            callback: function () {
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
                }).done(function (msg) {
                  password = "";
                  if (msg.indexOf(springLoggedInStrCheck) === -1) {
                    engageCore.trigger(events.customSuccess.getName(), translate("loginSuccessful", "Successfully logged in. Please reload the page if the page does not reload automatically."));
                    $("#" + id_btn_login).hide();
                    $("#" + id_btn_reloadPage).click(function (e) {
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
                }).fail(function () {
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
        onEscape: function () {
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
    $(document).keydown(function (e) {
      // space = 32, backspace = 8, page up = 73, page down = 33, enter = 13
      if (((e.keyCode === 32)
          || (e.keyCode === 8)
          || (e.keyCode === 33)
          || (e.keyCode === 34)
          || (e.keyCode === 13))
          && ((e.target !== $("input#username")[0])
          && (e.target !== $("input#password")[0])
          && (e.target !== $("input#navigation_time_current")[0]))) {
        return false;
      }
    });
    // process hardcoded keys
    $.each(MeInfoModel.get("shortcuts"), function (i, val) {
      switch (val.name) {
        case shortcut_seekLeft:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.seekLeft.getName());
          });
          break;
        case shortcut_seekRight:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.seekRight.getName());
          });
          break;
        case shortcut_playbackrateIncrease:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.playbackRateIncrease.getName());
          });
          break;
        case shortcut_playbackrateDecrease:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.playbackRateDecrease.getName());
          });
          break;
        case shortcut_nextChapter:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.nextChapter.getName());
          });
          break;
        case shortcut_fullscreenEnable:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.fullscreenEnable.getName());
          });
          break;
        case shortcut_fullscreenCancel:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.fullscreenCancel.getName());
          });
          break;
        case shortcut_jumpToBegin:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.seek.getName(), 0);
          });
          break;
        case shortcut_prevChapter:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.previousChapter.getName());
          });
          break;
        case shortcut_playPause:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.playPause.getName());
          });
          break;
        case shortcut_muteToggle:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.muteToggle.getName());
          });
          break;
        case shortcut_volDown:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.volumeDown.getName());
          });
          break;
        case shortcut_volUp:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.volumeUp.getName());
          });
          break;
        case shortcut_nextFocus:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.focusVideo.getName(), "focus.next");
          });
          break;
        case shortcut_prevFocus:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.focusVideo.getName(), "focus.prev");
          });
          break;
        case shortcut_movePiP:
          Mousetrap.bind(val.key, function () {
            if (pip) {
                if (pipPos === "left") {
                  pipPos = "right";
                } else {
                  pipPos = "left";
                }
                engageCore.trigger(events.movePiP.getName(), pipPos);
            }
          });
          break;
        case shortcut_togglePiP:
          Mousetrap.bind(val.key, function () {
            pip = !pip;
            engageCore.trigger(events.togglePiP.getName(), pip);
          });
          break;
        case shortcut_moveLeft:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.moveHorizontal.getName(), zoom_wasd_step_size);
          });
          break;
        case shortcut_moveRight:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.moveHorizontal.getName(), -zoom_wasd_step_size);
          });
          break;
        case shortcut_moveUp:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.moveVertical.getName(), zoom_wasd_step_size);
          });
          break;
        case shortcut_moveDown:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.moveVertical.getName(), -zoom_wasd_step_size);
          });
          break;
        case shortcut_zoomIn:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.zoomIn.getName(), true);
          });
          break;
        case shortcut_zoomOut:
          Mousetrap.bind(val.key, function () {
            engageCore.trigger(events.zoomOut.getName(), true);
          });
          break;
        default:
          break;
      }
    });
  }

  function checkAllPluginsloaded() {
    var all_plugins_loaded = true;
    $.each(plugins_loaded, function (plugin_index, plugin_value) {
      if (plugin_value === false) {
        all_plugins_loaded = false;
      }
    });
    return all_plugins_loaded;
  }

  function loadTemplate(plugin, plugin_name, plugin_path) {
    if (plugin.template !== "none") {
      // load template asynchronously
      $.get("engage/theodul/" + plugin_path + plugin.template, function (template) {
        var template_data = {};
        // add template data if not undefined
        if (plugin.template_data !== undefined) {
          template_data = plugin.template_data;
        }
        // add full plugin path to the template data
        template_data.plugin_path = "engage/theodul/" + plugin_path;
        // process the template using underscore and set it in the plugin obj
        var _template = _.template(template);
        plugin.templateProcessed = _template(template_data);
        plugin.template = template;
        plugin.pluginPath = "engage/theodul/" + plugin_path;
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
        plugins_loaded[plugin_name] = true;
        // check if all plugins are ready
        if (checkAllPluginsloaded()) {
          engageCore.pluginView.allPluginsLoaded();
          engageCore.trigger(events.plugin_load_done.getName());
        }
      });
    } else {
      plugins_loaded[plugin_name] = true;
      // check if all plugins are ready
      if (checkAllPluginsloaded()) {
        engageCore.pluginView.allPluginsLoaded();
        engageCore.trigger(events.plugin_load_done.getName());
      }
    }
  }

  function loadPlugin(plugin_path, plugin_name) {
    require([plugin_path + "main"], function (plugin) {
      // load styles in link tags via jquery
      if ($.isArray(plugin.styles)) {
        $.each(plugin.styles, function (style_index, style_path) {
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

      // we have to change that in the future... this is only for loading a second controls template to put above the video if the player controls are below it
      if (plugin.template_topIfBottom && plugin.template_topIfBottom !== "none") {
        // load template asynchronously
        $.get("engage/theodul/" + plugin_path + plugin.template_topIfBottom, function (template) {
          var template_data = {};
          // add template data if not undefined
          if (plugin.template_data_topIfBottom !== undefined) {
            template_data = plugin.template_data_topIfBottom;
          }
          // add full plugin path to the template data
          template_data.plugin_path = "engage/theodul/" + plugin_path;
          // process the template using underscore and set it in the plugin obj
          var _template = _.template(template);
          plugin.templateProcessed_topIfBottom = _template(template_data);
          plugin.template_topIfBottom = template;
          plugin.pluginPath_topIfBottom = "engage/theodul/" + plugin_path;
          loadTemplate(plugin, plugin_name, plugin_path);
        });
      } else {
        loadTemplate(plugin, plugin_name, plugin_path);
      }
    });
  }

  function throwEvents() {
    if (engageCore.model.get("autoplay")) {
      engageCore.trigger(events.autoplay.getName());
    }
    if (engageCore.model.get("time")) {
      engageCore.trigger(events.initialSeek.getName(), engageCore.model.get("time"));
    }
  }

  var EngageCore = Backbone.View.extend({
    el: $("#" + id_engage_view),
    Event: EngageEvent,
    // bind a key event as a string to given event
    bindKeyToEvent: function (shortcuts, event) {
      // only for EngageEvent objects
      if (event instanceof EngageEvent) {
        Mousetrap.bind(shortcut, function () {
          engageCore.trigger(event);
        });
      }
    },
    on: function (event, handler, context) {
      if (event instanceof EngageEvent) {
        this.dispatcher.on(event.getName(), handler, context);
      } else {
        this.dispatcher.on(event, handler, context);
      }
    },
    trigger: function (event, data) {
      if (event instanceof EngageEvent) {
        this.dispatcher.trigger(event.getName(), data);
      } else {
        this.dispatcher.trigger(event, data);
      }
    },
    log: function (data) {
      if (this.model.get("isDebug") && window.console) {
        console.log(data);
      }
    },
    group: function (block) {
      if (this.model.get("isDebug") && window.console) {
        console.group(block);
      }
    },
    groupEnd: function () {
      if (this.model.get("isDebug") && window.console) {
        console.groupEnd();
      }
    },
    getPluginPath: function (pluginName) {
      var evaluated_plugin_path = "";
      var pluginsInfos = engageCore.model.get("pluginsInfo");
      if (pluginsInfos) {
        var pluginList = pluginsInfos.get("pluginlist");
        if (pluginList && pluginList.plugins !== undefined) {
          var plugins = pluginList.plugins;
          if ($.isArray(plugins)) {
            $.each(plugins, function (index, value) {
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
    loadInfoMeModel: function (func) {
      require(["engage/models/meInfo"], function (me) {
        MeInfoModel = new me();
        // wait that me infos are loaded
        var intv = window.setInterval(function () {
          if (MeInfoModel.ready()) {
            window.clearInterval(intv);
            if (func && (typeof func === "function")) {
              func();
            }
          }
        }, 15);
      });
    },
    loadCoreUI: function () {
      // switch view template and css rules for current player mode
      // link tag for css file
      var cssLinkTag = $("<link>");
      var cssAttr = {
        type: "text/css",
        rel: "stylesheet"
      };
      engageCore.controls_top = MeInfoModel.getPositionControls() === "top"; // bottom else
      engageCore.log("Core: Position of the controls is " + (engageCore.controls_top ? "top" : "bottom"));

      // template obj
      var core_template = "none";
      // path to the require module with the view logic
      var view_logic_path = "";
      switch (engageCore.model.get("mode")) {
        case "embed":
          cssAttr.href = "css/core_embed_style.css";
          core_template = "templates/core_embed.html";
          view_logic_path = "engage/views/embed";
          engageCore.model.embed = true;
          break;
        case "mobile":
          cssAttr.href = "css/core_mobile_style.css";
          core_template = "templates/core_mobile.html";
          view_logic_path = "engage/views/mobile";
          engageCore.model.mobile = true;
          break;
        case "desktop":
        default:
          cssAttr.href = engageCore.controls_top ? "css/core_desktop_style_top.css" : "css/core_desktop_style_bottom.css";
          core_template = engageCore.controls_top ? "templates/core_desktop_top.html" : "templates/core_desktop_bottom.html";
          view_logic_path = "engage/views/desktop";
          engageCore.model.desktop = true;
          break;
      }
      cssLinkTag.attr(cssAttr);
      // add css to DOM
      $("head").append(cssLinkTag);
      // load js view logic via require, see files in views/
      require([view_logic_path], function (pluginView) {
        // link view logic to the core
        engageCore.pluginView = pluginView;
        engageCore.loadCoreTemplate(core_template);
      });
    },
    loadCoreTemplate: function (core_template) {
      // get core template
      $.get(core_template, function (template) {
        // set template, render it and add it to DOM
        engageCore.template = template;
        $(engageCore.el).html(_.template(template)).trigger("create"); // variables do not work in here!
        // run init function of the view
        engageCore.pluginView.initView();
        if (engageCore.model.get("pluginsInfo") && engageCore.model.browserSupported) {
          engageCore.loadPlugins();
          bindShortcutsToEvents(); // bind configured shortcuts to events
        } else {
          engageCore.trigger(events.plugin_load_done.getName());
        }
      });
    },
    loadPlugins: function () {
      engageCore.model.get("pluginsInfo").fetch({
        success: function (pluginInfos) {
          if ((pluginInfos.get("pluginlist").plugins !== undefined) && pluginInfos.get("pluginlist")) {
            numberOfPlugins = pluginInfos.get("pluginlist").plugins.length;
            if ($.isArray(pluginInfos.get("pluginlist").plugins)) {
              $.each(pluginInfos.get("pluginlist").plugins, function (index, value) {
                var plugin_name = value["name"];
                plugins_loaded[plugin_name] = false;
              });
              $.each(pluginInfos.get("pluginlist").plugins, function (index, value) {
                var plugin_name = value["name"];
                engageCore.log("Core: Loading plugin '" + plugin_name + "' from '" + ("../../../plugin/" + value["static-path"] + "/") + "'...");
                loadPlugin("../../../plugin/" + value["static-path"] + "/", plugin_name);
              });
            } else {
              var plugin_name = pluginInfos.get("pluginlist").plugins["name"];
              plugins_loaded[plugin_name] = false;
              engageCore.log("Core: Loading plugin '" + plugin_name + "' from '" + ("../../../plugin/" + pluginInfos.get("pluginlist").plugins["static-path"] + "/") + "'...");
              loadPlugin("../../../plugin/" + pluginInfos.get("pluginlist").plugins["static-path"] + "/", plugin_name);
            }
          }
        }
      });
    },
    initialize: function () {
      $("." + class_loading).show();
      $("#" + id_loading1).show();
      initTranslate(detectLanguage());
      setMinBrowserVersions();
      // the main core is our global event system
      this.dispatcher = _.clone(Backbone.Events);
      // link to the engage model
      this.model = new EngageModel();
      // listen to all events
      this.dispatcher.on("all", function (name) {
        if (engageCore.model.get("isEventDebug") && window.console) {
          console.log("[Core:Event] '" + name + "'");
        }
      });
      this.model.browserSupported = browserSupported();
      this.model.desktop = false;
      this.model.embed = false;
      this.model.mobile = false;
      // core init event
      this.dispatcher.on(events.coreInit.getName(), function () {
        engageCore.loadInfoMeModel(engageCore.loadCoreUI);
      });
      // load plugins done, hide loading and show content
      this.dispatcher.on(events.mediaPackageModelError.getName(), function (str) {
        mediapackageError = true;
        $("." + class_loading).hide().detach();
        $("#" + id_engage_view).hide().detach();
        $("#" + id_btn_reloadPage).hide();
        $("#" + id_customError_str).html(str);
        setCustomError = true;
        if (getLoginStatus() === 0) {
          $("#" + id_btn_login).click(login);
          $("#" + id_customError + ", #" + id_btn_login).show();
        } else {
          // TODO: Logged in as...
          $("#" + id_btn_login).hide();
        }
      });
      // load plugins done, hide loading and show content
      this.dispatcher.on(events.plugin_load_done.getName(), function () {
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
        window.setTimeout(function () {
          $("#" + id_loadingProgressbar2).css("width", "100%");
          window.setTimeout(function () {
            $("." + class_loading).hide().detach();
            if (engageCore.model.browserSupported) {
              $("#" + id_browserWarning).hide().detach();
              $("#" + id_engage_view).show();
              translateCoreHTML();
              if (engageCore.model.desktop) {
                window.setTimeout(function () {
                  if ($("#" + id_volumeSlider).html() === undefined) {
                    $("#" + id_btn_reloadPage).click(function (e) {
                      e.preventDefault();
                      location.reload();
                    });
                    $("#" + id_engage_view).hide().detach();
                    $("body").css("min-width", "");
                    $("#" + id_customError).show();
                  } else {
                    $("#" + id_customError + ", #" + id_btn_login).hide().detach();
                    throwEvents();
                  }
                }, errorCheckDelay);
              }
              // TODO: Error/loading checks for embed and mobile
            } else {
              $("#" + id_engage_view + ", #" + id_customError).hide().detach();
              $("body").css("min-width", "");
              $("#" + id_browserWarning).show();
              $("#" + id_btn_tryAnyway).click(function (e) {
                e.preventDefault();
                window.location.href = window.location.href + "&browser=all";
              });
            }
          }, loadingDelay2);
        }, loadingDelay1);
      });

      this.dispatcher.on(events.movePiP.getName(), function (pos) {
        pipPos = pos;
      });
      this.dispatcher.on(events.togglePiP.getName(), function (status) {
        pip = status;
      });
    }
  });

  var engageCore = new EngageCore();
  engageCore.trigger(events.coreInit.getName());
  return engageCore;
});
