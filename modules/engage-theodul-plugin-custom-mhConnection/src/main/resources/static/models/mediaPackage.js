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
define(["backbone", "engage/core"], function(Backbone, Engage) {
    "use strict";

    var events = {
        mediaPackageModelInternalError: new Engage.Event("MhConnection:mediaPackageModelInternalError", "A mediapackage model error occured", "trigger"),
        mediaPackageLoaded: new Engage.Event("MhConnection:mediaPackageLoaded", "A mediapackage has been loaded", "trigger")
    };

    var SEARCH_ENDPOINT = "/search/episode.json";

    var mediaPackageID = '';
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('id')) {
      mediaPackageID = urlParams.get('id');
    }

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
                                if (mediaPackage.mediapackage.series) {
                                    model.attributes.seriesid = mediaPackage.mediapackage.series;
                                }
                            }
                            if (mediaPackage.dcTitle) {
                                model.attributes.title = mediaPackage.dcTitle;
                            }
                            if (mediaPackage.mediapackage.creators) {
                                model.attributes.creator = (Array.isArray(mediaPackage.mediapackage.creators.creator)
                                    ? mediaPackage.mediapackage.creators.creator
                                    : [mediaPackage.mediapackage.creators.creator])
                                    .join(', ');
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
                            model.attributes.eventid = mediaPackageID;
                            model.attributes.ready = true;
                        }
                        model.trigger("change");
                        Engage.trigger(events.mediaPackageLoaded.getName());
                        Engage.log("MhConnection: Mediapackage Data change event thrown");
                    } else {
                        Engage.log("MhConnection: Mediapackage data not loaded successfully");
                        Engage.trigger(events.mediaPackageModelInternalError.getName());
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
            "attachments": {},
            "ready" : false
        }
    });

    return MediaPackageModel;
});
