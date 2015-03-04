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
/*global define, CustomEvent*/
define(["jquery", "backbone", "engage/core", "../models/footprint"], function($, Backbone, Engage, FootprintModel) {
    "use strict";

    var USERTRACKING_ENDPOINT = "/usertracking";
    var USERTRACKING_ENDPOINT_FOOTPRINTS = "/footprint.json";

    var mediaPackageID = Engage.model.get("urlParameters").id;
    if (!mediaPackageID) {
        mediaPackageID = "";
    }

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

    return FootprintCollection;
});
