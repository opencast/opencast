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
 * 
 */
/*jslint browser: true, nomen: true*/
/*global define*/
define(['require', 'jquery', 'underscore', 'backbone', 'engage/engage_core'], function(require, $, _, Backbone, Engage) {
    "use strict"; // strict mode in all our application
    var PLUGIN_NAME = "Engage Custom Matterhorn Endpoint Connection",
            PLUGIN_TYPE = "engage_custom",
            PLUGIN_VERSION = "0.1",
            PLUGIN_TEMPLATE = "none",
            PLUGIN_STYLES = ["", ""];
    var plugin = {
        name: PLUGIN_NAME,
        type: PLUGIN_TYPE,
        version: PLUGIN_VERSION,
        styles: PLUGIN_STYLES,
        template: PLUGIN_TEMPLATE
    };

    // local privates//

    var SEARCH_ENDPOINT = "/search/episode.json";
    var mediaPackageID = "";
    var mediaPackage; // Mediapackage data
    var mediaInfo; // media infos like video tracks and attachments

    // model prototypes //

    var MediaPackageModel = Backbone.Model.extend({
        urlRoot: SEARCH_ENDPOINT,
        initialize: function() {
            Engage.log("MhConnection: init MediaPackageModel");
            //request model data
            this.fetch({
                data: {id: mediaPackageID},
                success: function(model) {
                    var mediaPackage; // Mediapackage data
                    if (model.attributes && model.attributes['search-results'] && model.attributes['search-results'].result) {
                        mediaPackage = model.attributes['search-results'].result;
                        if (mediaPackage) {
                            //format silent the model data, see dublincore for reference names
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
                        model.trigger("change"); //one change event
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

    // plugin logic //

    Engage.log("MhConnection: init");
    var relative_plugin_path = Engage.getPluginPath('EngagePluginCustomMhConnection');
    Engage.log('MhConnection: relative plugin path ' + relative_plugin_path);

    // Get ID
    //mediaPackageID = Engage.urlParams.id;
    mediaPackageID = Engage.model.get("urlParameters").id;
    if (!mediaPackageID) {
        mediaPackageID = "";
    }

    // All plugins loaded lets init the models
    Engage.on("Core:plugin_load_done", function() {
        Engage.log("MhConnection: receive plugin load done");
        Engage.model.set("mediaPackage", new MediaPackageModel());
    });

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

    function callSearchEndpoint(callback) {
        $.ajax({
            url: SEARCH_ENDPOINT,
            data: {
                id: mediaPackageID
            },
            cache: false
        }).done(function(data) {
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

    // getter events
    Engage.on("MhConnection:getMediaInfo", function(callback) {
        // check if data is already loaded
        if (!mediaPackage && !mediaInfo) {
            // Get Infos from Search Endpoint
            callSearchEndpoint(function() {
                // trigger callback
                callback(mediaInfo);
            });
        } else {
            // trigger callback
            callback(mediaInfo);
        }
    });

    Engage.on("MhConnection:getMediaPackage", function(callback) {
        // check if data is already loaded
        if (!mediaPackage) {
            // Get Infos from Search Endpoint
            callSearchEndpoint(function() {
                // trigger callback
                callback(mediaPackage);
            });
        } else {
            // trigger callback
            callback(mediaPackage);
        }
    });

    return plugin;
});