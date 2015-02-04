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
define(['jquery', 'backbone'], function($, Backbone) {
    "use strict";

    /*
     * Model with information about the current user and the current MH configuration
     */
    var MeInfoModel = Backbone.Model.extend({
        urlRoot: "../../../info/me.json",
        initialize: function() {
            this.ready = false;
            this.fetch({
                success: function(me) {
                    var shortcuts = new Array();
                    if (me && me.attributes && me.attributes.org && me.attributes.org.properties) {
                        // extract shortcuts
                        $.each(me.attributes.org.properties, function(key, value) {
                            var prop = "player.shortcut.";
                            var name = key.substring(prop.length, key.length);
                            if ((key.indexOf(prop) != -1) && (name.length > 0) && value) {
                                shortcuts.push({
                                    name: key.substring(prop.length, key.length),
                                    key: value
                                });
                            }
                        });
                    }
                    me.set("shortcuts", shortcuts);
                    me.ready = true;
                }
            });
        }
    });

    return MeInfoModel;
});
