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
define(["jquery", "backbone", "engage/core"], function($, Backbone, Engage) {
    "use strict";

    var ViewsModel = Backbone.Model.extend({
        initialize: function() {
            Engage.log("MhConnection: Init empty Views model");
        },
        defaults: {
            "stats": {
                "views": 0
            }
        }
    });

    /* TODO: Wait for the new usertracking service...

    var USERTRACKING_ENDPOINT = "/usertracking";
    var USERTRACKING_ENDPOINT_STATS = "/stats.json";

    var mediaPackageID = Engage.model.get("urlParameters").id;
    if (!mediaPackageID) {
        mediaPackageID = "";
    }

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

    */

    return ViewsModel;
});
