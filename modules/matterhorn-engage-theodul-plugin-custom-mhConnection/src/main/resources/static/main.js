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
define(["require", "jquery", "backbone", "engage/core"], function(require, $, Backbone, Engage) {
    "use strict";

    var insertIntoDOM = true;
    var PLUGIN_NAME = "Engage Custom Matterhorn Endpoint Connection";
    var PLUGIN_TYPE = "engage_custom";
    var PLUGIN_VERSION = "1.0";
    var PLUGIN_TEMPLATE = "none";
    var PLUGIN_TEMPLATE_MOBILE = "none";
    var PLUGIN_TEMPLATE_EMBED = "none";
    var PLUGIN_STYLES = [
        ""
    ];
    var PLUGIN_STYLES_MOBILE = [
        ""
    ];
    var PLUGIN_STYLES_EMBED = [
        ""
    ];

    var plugin;
    var events = {
        mediaPackageModelError: new Engage.Event("MhConnection:mediaPackageModelError", "A mediapackage model error occured", "trigger"),
        mediaPackageModelInternalError: new Engage.Event("MhConnection:mediaPackageModelInternalError", "A mediapackage model error occured", "handler"),
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "when the core loaded the event successfully", "handler"),
        getMediaInfo: new Engage.Event("MhConnection:getMediaInfo", "", "handler"),
        getMediaPackage: new Engage.Event("MhConnection:getMediaPackage", "", "handler")
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
            break;
            isEmbedMode = true;
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
                styles: PLUGIN_STYLES,
                template: PLUGIN_TEMPLATE,
                events: events
            };
            isDesktopMode = true;
            break;
    }

    /* change these variables */
    var SEARCH_ENDPOINT = "/search/episode.json";

    /* don't change these variables */
    var Utils;
    var initCount = 6;
    var InfoMeModel;
    var MediaPackageModel;
    var ViewsModel;
    var FootprintCollection;
    var mediaPackageID = "";
    var mediaPackage; // mediaPackage data
    var mediaInfo; // media info like video tracks and attachments
    var translations = new Array();
    var initialized = false;

    function initTranslate(language, funcSuccess, funcError) {
        var path = Engage.getPluginPath("EngagePluginCustomMhConnection").replace(/(\.\.\/)/g, "");
        var jsonstr = window.location.origin + "/engage/theodul/" + path; // this solution is really bad, fix it...

        if (language == "de") {
            Engage.log("MHConnection: Chosing german translations");
            jsonstr += "language/de.json";
        } else { // No other languages supported, yet
            Engage.log("MHConnection: Chosing english translations");
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

    /**
     * extractMediaInfo
     */
    function extractMediaInfo() {
        if (mediaPackage) {
            mediaInfo = {};
            mediaInfo.tracks = mediaPackage.mediapackage.media.track;
            mediaInfo.attachments = mediaPackage.mediapackage.attachments.attachment;
            mediaInfo.title = mediaPackage.dcTitle;
            mediaInfo.creator = mediaPackage.dcCreator;
            mediaInfo.date = mediaPackage.dcCreated;
        } else {
            Engage.trigger(plugin.events.mediaPackageModelError.getName(), translate("error_noMediaInformationAvailable", "No media information are available."));
        }
    }

    /**
     * callSearchEndpoint
     *
     * @param callback
     */
    function callSearchEndpoint(callback) {
        if (callback === "function") {
            $.ajax({
                url: SEARCH_ENDPOINT,
                data: {
                    id: mediaPackageID
                },
                cache: false
            }).done(function(data) {
                // split search results
                if (data && data["search-results"] && data["search-results"].result) {
                    mediaPackage = data["search-results"].result;
                    extractMediaInfo();
                } else {
                    Engage.trigger(plugin.events.mediaPackageModelError.getName(), translate("error_endpointNotAvailable", "A requested search endpoint is currently not available."));
                }
                callback();
            });
        }
    }

    /**
     * Initialize the plugin
     */
    function initPlugin() {
        if (!initialized) {
            initialized = true;
            Engage.model.set("infoMe", new InfoMeModel());
            Engage.model.set("mediaPackage", new MediaPackageModel());
            Engage.model.set("views", new ViewsModel());
            Engage.model.set("footprints", new FootprintCollection());
        }
    }

    // init event
    Engage.log("MhConnection: Init");
    var relative_plugin_path = Engage.getPluginPath("EngagePluginCustomMhConnection");

    // get ID
    mediaPackageID = Engage.model.get("urlParameters").id;
    if (!mediaPackageID) {
        mediaPackageID = "";
    }

    Engage.on(plugin.events.mediaPackageModelInternalError.getName(), function() {
        Engage.trigger(events.mediaPackageModelError.getName(), translate("error_mediaPackageInformationNotLoaded", "There are two possible reasons for this error:<ul><li>The media is not available any more</li><li>The media is protected and you need to log in</li></ul>"));
    });

    Engage.on(plugin.events.getMediaInfo.getName(), function(callback) {
        if (callback === "function") {
            // check if data has already been loaded
            if (!mediaPackage && !mediaInfo) {
                callSearchEndpoint(function() {
                    callback(mediaInfo);
                });
            } else {
                callback(mediaInfo);
            }
        }
    });

    Engage.on(plugin.events.getMediaPackage.getName(), function(callback) {
        if (callback === "function") {
            // check if data has already been loaded
            if (!mediaPackage) {
                callSearchEndpoint(function() {
                    callback(mediaPackage);
                });
            } else {
                callback(mediaPackage);
            }
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("MhConnection: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load infoMe model
    require([relative_plugin_path + "models/infoMe"], function(model) {
        Engage.log("MhConnection: InfoMeModel loaded");
        InfoMeModel = model;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load mediaPackage model
    require([relative_plugin_path + "models/mediaPackage"], function(model) {
        Engage.log("MhConnection: MediaPackageModel loaded");
        MediaPackageModel = model;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load views model
    require([relative_plugin_path + "models/views"], function(model) {
        Engage.log("MhConnection: ViewsModel loaded");
        ViewsModel = model;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load footprint collection
    require([relative_plugin_path + "collections/footprint"], function(collection) {
        Engage.log("MhConnection: FootprintCollection loaded");
        FootprintCollection = collection;
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    // load utils class
    require([relative_plugin_path + "utils"], function(utils) {
        Engage.log("MhConnection: Utils class loaded");
        Utils = new utils();
        initTranslate(Utils.detectLanguage(), function() {
            Engage.log("MHConnection: Successfully translated.");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        }, function() {
            Engage.log("MHConnection: Error translating...");
            initCount -= 1;
            if (initCount <= 0) {
                initPlugin();
            }
        });
    });

    return plugin;
});
