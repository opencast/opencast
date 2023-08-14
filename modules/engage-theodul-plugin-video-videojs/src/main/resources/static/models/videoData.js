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
define(["backbone"], function(Backbone) {
    "use strict";

    var VideoDataModel = Backbone.Model.extend({
        initialize: function(ids, videoSources, duration) {
            this.attributes.ids = ids;
            this.attributes.type = videoSources.audio ? "audio" : "video";
            this.attributes.videoSources = videoSources;
            this.attributes.duration = duration;
        },
        defaults: {
            "ids": [],
            "type": "video",
            "videoSources": [],
            "isPlaying": false,
            "currentTime": -1,
            "duration": -1,
            "ready": false
        }
    });

    return VideoDataModel;
});
