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
define(["jquery", "backbone", "engage/core"], function($, Backbone, Engage) {
    "use strict";

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
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "when the core loaded the event successfully", "handler"),
        getMediaInfo: new Engage.Event("MhConnection:getMediaInfo", "", "handler"),
        getMediaPackage: new Engage.Event("MhConnection:getMediaPackage", "", "handler")
    };

    var isDesktopMode = false;
    var isEmbedMode = false;
    var isMobileMode = false;

    // desktop, embed and mobile logic
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
            break;
            isEmbedMode = true;
        case "desktop":
        default:
            plugin = {
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
    var USERTRACKING_ENDPOINT = "/usertracking";
    var USERTRACKING_ENDPOINT_FOOTPRINTS = "/footprint.json";
    var USERTRACKING_ENDPOINT_STATS = "/stats.json";
    var INFO_ME_ENDPOINT = "/info/me.json";

    /* don't change these variables */
    var mediaPackageID = "";
    var initCount = 2;
    var mediaPackage; // mediaPackage data
    var mediaInfo; // media info like video tracks and attachments
    var translations = new Array();
    var initialized = false;

    function detectLanguage() {
        return navigator.language || navigator.userLanguage || navigator.browserLanguage || navigator.systemLanguage || "en";
    }

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

    var InfoMeModel = Backbone.Model.extend({
        urlRoot: INFO_ME_ENDPOINT,
        initialize: function() {
            Engage.log("MhConnection: Init InfoMe model");
            this.update();
        },
        update: function() {
            Engage.log("MhConnection: Updating InfoMe model");
            // request model data
            this.fetch({
                data: {},
                success: function(model) {
                    model.loggedIn = false;
                    model.username = "Anonymous";
                    model.roles = [];
                    var attr = model.attributes;
                    if (attr.username) {
                        Engage.log("Username found: " + attr.username);
                        model.username = attr.username;
                    } else {
                        Engage.log("No username found.");
                    }
                    if (attr.roles && (attr.roles.length > 0)) {
                        model.roles = attr.roles;
                        var notAnonymous = false;
                        for (var i = 0; i < attr.roles.length; ++i) {
                            if (attr.roles[i] != "ROLE_ANONYMOUS") {
                                notAnonymous = true;
                            }
                        }
                        model.loggedIn = notAnonymous;
                        if (notAnonymous) {
                            Engage.log("User has one or more roles.");
                        } else {
                            Engage.log("User has no role.");
                        }
                    } else {
                        Engage.log("Error: No roles found.");
                    }
                    model.trigger("change");
                }
            });
        },
        defaults: {}
    });

    var MediaPackageModel = Backbone.Model.extend({
        urlRoot: SEARCH_ENDPOINT,
        initialize: function() {
            Engage.log("MhConnection: Init MediaPackage model");
            this.update();
        },
        update: function() {
            Engage.log("MhConnection: Updating MediaPackage model");
            // request model data
            this.fetch({
                data: {
                    id: mediaPackageID
                },
                success: function(model) {
                    var mediaPackage; // mediapackage data
                    if (model.attributes && model.attributes["search-results"] && model.attributes["search-results"].result) {
                        mediaPackage = model.attributes["search-results"].result;
                        if (mediaPackage) {
                            // format the model data, see dublin core for reference names
                            if (mediaPackage.mediapackage) {
                                if (mediaPackage.mediapackage.media && mediaPackage.mediapackage.media.track) {
                                    if (!mediaPackage.mediapackage.media.track.length) {
                                        model.attributes.tracks = new Array();
                                        model.attributes.tracks.push(mediaPackage.mediapackage.media.track);
                                    } else {
                                        model.attributes.tracks = mediaPackage.mediapackage.media.track;
                                    }
                                }
                                if (mediaPackage.mediapackage.attachments.attachment) {
                                    if (!mediaPackage.mediapackage.attachments.attachment.length) {
                                        model.attributes.attachments = new Array();
                                        model.attributes.attachments.push(mediaPackage.mediapackage.attachments.attachment);
                                    } else {
                                        model.attributes.attachments = mediaPackage.mediapackage.attachments.attachment;
                                    }
                                }
                                if (mediaPackage.mediapackage.seriestitle) {
                                    model.attributes.series = mediaPackage.mediapackage.seriestitle;
                                }
                            }
                            if (mediaPackage.dcTitle) {
                                model.attributes.title = mediaPackage.dcTitle;
                            }
                            if (mediaPackage.dcCreator) {
                                model.attributes.creator = mediaPackage.dcCreator;
                            }
                            if (mediaPackage.dcCreated) {
                                model.attributes.date = mediaPackage.dcCreated;
                            }
                            if (mediaPackage.dcDescription) {
                                model.attributes.description = mediaPackage.dcDescription;
                            }
                            if (mediaPackage.dcSubject) {
                                model.attributes.subject = mediaPackage.dcSubject;
                            }
                            if (mediaPackage.dcContributor) {
                                model.attributes.contributor = mediaPackage.dcContributor;
                            }
                            if (mediaPackage.segments && mediaPackage.segments.segment) {
                                model.attributes.segments = mediaPackage.segments.segment;
                            }
                        }
                        model.trigger("change");
                        Engage.log("Mediapackage Data change event thrown");
                    } else {
                        Engage.log("Mediapackage data not loaded successfully");
                        Engage.trigger(plugin.events.mediaPackageModelError.getName(), translate("error_mediaPackageInformationNotLoaded", "There are two possible reasons for this error:<ul><li>The media is not available any more</li><li>The media is protected and you need to log in</li></ul>"));
                    }
                }
            });
        },
        defaults: {
            "title": "",
            "creator": "",
            "date": "",
            "description": "",
            "subject": "",
            "tracks": {},
            "attachments": {}
        }
    });

    var ViewsModel = Backbone.Model.extend({
        urlRoot: USERTRACKING_ENDPOINT + USERTRACKING_ENDPOINT_STATS,
        initialize: function() {
            Engage.log("MhConnection: Init Views model");
            this.put();
        },
        put: function() {
            Engage.log("MhConnection: Adding user to viewers");
            var thisModel = this;
            $.ajax({
                type: "PUT",
                url: USERTRACKING_ENDPOINT,
                data: {
                    id: mediaPackageID,
                    in : 0,
                    out: 0,
                    type: "VIEWS"
                },
                success: function(result) {
                    thisModel.update();
                }
            });
        },
        update: function() {
            // request model data
            Engage.log("MhConnection: Updating views model");
            this.fetch({
                data: {
                    id: mediaPackageID
                },
                success: function(model) {
                    model.trigger("change");
                }
            });
        },
        defaults: {
            "stats": {
                "views": 0
            }
        }
    });

    var FootprintModel = Backbone.Model.extend({
        defaults: {
            "position": 0,
            "views": 0
        }
    });

    var FootprintCollection = Backbone.Collection.extend({
        model: FootprintModel,
        url: USERTRACKING_ENDPOINT + USERTRACKING_ENDPOINT_FOOTPRINTS,
        initialize: function() {
            this.update();
        },
        put: function(from, to) {
            Engage.log("MhConnection: Setting footprint at " + from);
            var thisModel = this;
            // put to mh endpoint
            $.ajax({
                type: "PUT",
                url: USERTRACKING_ENDPOINT,
                data: {
                    id: mediaPackageID,
                    in : from,
                    out: to,
                    type: "FOOTPRINT"
                },
                success: function(result) {
                    // update current footprint model
                    thisModel.update();
                }
            });
        },
        update: function() {
            // request collection data
            this.fetch({
                data: {
                    id: mediaPackageID
                },
                success: function(collection) {
                    collection.trigger("change");
                }
            });
        },
        parse: function(response) {
            return response.footprints.footprint;
        }
    });

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
	if(callback === "function") {
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
	if(!initialized) {
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

    Engage.on(plugin.events.getMediaInfo.getName(), function(callback) {
	if(callback === "function") {
            // check if data is already loaded
            if (!mediaPackage && !mediaInfo) {
		// get info from search endpoint
		callSearchEndpoint(function() {
                    // trigger callback
                    callback(mediaInfo);
		});
            } else {
		// trigger callback
		callback(mediaInfo);
            }
	}
    });

    Engage.on(plugin.events.getMediaPackage.getName(), function(callback) {
	if(callback === "function") {
            // check if data is already loaded
            if (!mediaPackage) {
		// get info from search endpoint
		callSearchEndpoint(function() {
                    // trigger callback
                    callback(mediaPackage);
		});
            } else {
		// trigger callback
		callback(mediaPackage);
            }
	}
    });

    // init translation
    initTranslate(detectLanguage(), function() {
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

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function() {
        Engage.log("MhConnection: Plugin load done");
        initCount -= 1;
        if (initCount <= 0) {
            initPlugin();
        }
    });

    return plugin;
});
