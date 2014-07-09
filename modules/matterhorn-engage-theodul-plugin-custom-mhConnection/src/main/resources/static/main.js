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
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function (require, $, _, Backbone, Engage) {
    "use strict";
    var PLUGIN_NAME = "Engage Custom Matterhorn Endpoint Connection",
        PLUGIN_TYPE = "engage_custom",
        PLUGIN_VERSION = "0.1",
        PLUGIN_TEMPLATE = "none",
        PLUGIN_TEMPLATE_MOBILE = "none",
        PLUGIN_TEMPLATE_EMBED = "none",
        PLUGIN_STYLES = [
            ""
        ],
        PLUGIN_STYLES_MOBILE = [
            ""
        ],
        PLUGIN_STYLES_EMBED = [
            ""
        ];

    var plugin;
    var events = {
        plugin_load_done: new Engage.Event("Core:plugin_load_done", "when the core loaded the event successfully", "handler"),
        getMediaInfo: new Engage.Event("MhConnection:getMediaInfo", "", "handler"),
        getMediaPackage: new Engage.Event("MhConnection:getMediaPackage", "", "handler")
    };

    // desktop, embed and mobile logic
    switch (Engage.model.get("mode")) {
    case "desktop":
        plugin = {
            name: PLUGIN_NAME,
            type: PLUGIN_TYPE,
            version: PLUGIN_VERSION,
            styles: PLUGIN_STYLES,
            template: PLUGIN_TEMPLATE,
            events: events
        };
        break;
    case "mobile":
        plugin = {
            name: PLUGIN_NAME,
            type: PLUGIN_TYPE,
            version: PLUGIN_VERSION,
            styles: PLUGIN_STYLES_MOBILE,
            template: PLUGIN_TEMPLATE_MOBILE,
            events: events
        };
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
    }

    /* change these variables */
    var SEARCH_ENDPOINT = "/search/episode.json";
    var USERTRACKING_ENDPOINT = "/usertracking/footprint.json";

    /* don't change these variables */
    var mediaPackageID = "";
    var initCount = 1;
    var mediaPackage; // mediaPackage data
    var mediaInfo; // media info like video tracks and attachments

    var MediaPackageModel = Backbone.Model.extend({
        urlRoot: SEARCH_ENDPOINT,
        initialize: function () {
            Engage.log("MhConnection: init MediaPackageModel");
            this.update();
        },
        update: function () {
            // request model data
            this.fetch({
                data: {
                    id: mediaPackageID
                },
                success: function (model) {
                    var mediaPackage; // Mediapackage data
                    if (model.attributes && model.attributes['search-results'] && model.attributes['search-results'].result) {
                        mediaPackage = model.attributes['search-results'].result;
                        if (mediaPackage) {
                            // format silent the model data, see dublin core for reference names
                            if (mediaPackage.mediapackage.media.track)
                                model.attributes.tracks = mediaPackage.mediapackage.media.track;
                            if (mediaPackage.mediapackage.attachments.attachment)
                                model.attributes.attachments = mediaPackage.mediapackage.attachments.attachment;
                            if (mediaPackage.dcTitle)
                                model.attributes.title = mediaPackage.dcTitle;
                            if (mediaPackage.dcCreator)
                                model.attributes.creator = mediaPackage.dcCreator;
                            if (mediaPackage.dcCreated)
                                model.attributes.date = mediaPackage.dcCreated;
                            if (mediaPackage.dcDescription)
                                model.attributes.description = mediaPackage.dcDescription;
                            if (mediaPackage.dcSubject)
                                model.attributes.subject = mediaPackage.dcSubject;
                            if (mediaPackage.dcContributor)
                                model.attributes.contributor = mediaPackage.dcContributor;
                            if (mediaPackage.mediapackage.seriestitle)
                                model.attributes.series = mediaPackage.mediapackage.seriestitle;
                        }
                        model.trigger("change");
                    } else {
                        // TODO: error
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

    var FootprintModel = Backbone.Model.extend({
        defaults: {
            "position": 0,
            "views": 0
        }
    });

    var FootprintCollection = Backbone.Collection.extend({
        model: FootprintModel,
        url: USERTRACKING_ENDPOINT,
        initialize: function () {
            this.update();
        },
        update: function () {
            // request collection data
            this.fetch({
                data: {
                    id: mediaPackageID
                },
                success: function (collection) {
                    collection.trigger("change");
                }
            });
        },
        parse: function (response) {
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
        }
    }

    /**
     * callSearchEndpoint
     *
     * @param callback
     */
    function callSearchEndpoint(callback) {
        $.ajax({
            url: SEARCH_ENDPOINT,
            data: {
                id: mediaPackageID
            },
            cache: false
        }).done(function (data) {
            // split search results
            if (data && data['search-results'] && data['search-results'].result) {
                mediaPackage = data['search-results'].result;
                extractMediaInfo();
            } else {
                // TODO: error
            }
            callback();
        });
    }

    // init event
    Engage.log("MhConnection: Init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginCustomMhConnection');
    Engage.log('MhConnection: Relative plugin path: "' + relative_plugin_path + '"');

    // get ID
    mediaPackageID = Engage.model.get("urlParameters").id;
    if (!mediaPackageID) {
        mediaPackageID = "";
    }

    Engage.on(plugin.events.getMediaInfo.getName(), function (callback) {
        // check if data is already loaded
        if (!mediaPackage && !mediaInfo) {
            // Get Infos from Search Endpoint
            callSearchEndpoint(function () {
                // trigger callback
                callback(mediaInfo);
            });
        } else {
            // trigger callback
            callback(mediaInfo);
        }
    });

    Engage.on(plugin.events.getMediaPackage.getName(), function (callback) {
        // check if data is already loaded
        if (!mediaPackage) {
            // Get Infos from Search Endpoint
            callSearchEndpoint(function () {
                // trigger callback
                callback(mediaPackage);
            });
        } else {
            // trigger callback
            callback(mediaPackage);
        }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function () {
        Engage.log("MhConnection: Plugin load done");
        Engage.model.set("mediaPackage", new MediaPackageModel());
        Engage.model.set("footprints", new FootprintCollection());
        initCount -= 1;
        if (initCount <= 0) {
            // do something
        }
    });

    return plugin;
});
