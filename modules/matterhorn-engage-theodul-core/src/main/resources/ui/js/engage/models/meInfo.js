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

    var prop_shortcut = "player.shortcut.";
    var prop_allowedtags = "player.allowedtags";
    var prop_allowedformats = "player.allowedformats";
    var prop_mastervideotype = "player.mastervideotype";
    var prop_positioncontrols = "player.positioncontrols";
    var ready = false;
    var positioncontrols = "";

    /*
     * Model with information about the current user and the current MH configuration
     */
    var MeInfoModel = Backbone.Model.extend({
        urlRoot: "../../../info/me.json",
        initialize: function() {
            this.fetch({
                success: function(me) {
                    var shortcuts = new Array();
                    var allowedTags;
                    var allowedFormats;
                    var mastervideotype = "";
                    if (me && me.attributes && me.attributes.org && me.attributes.org.properties) {
                        // extract shortcuts
                        $.each(me.attributes.org.properties, function(key, value) {
                            var name = key.substring(prop_shortcut.length, key.length);
                            // shortcuts
                            if ((key.indexOf(prop_shortcut) != -1) && (name.length > 0) && value) {
                                shortcuts.push({
                                    name: key.substring(prop_shortcut.length, key.length),
                                    key: value
                                });
                            }
                            // allowed tags on videos that should be played
                            else if ((key == prop_allowedtags) && value) {
                                allowedTags = value;
                            }
                            // formats that should be played
                            else if ((key == prop_allowedformats) && value) {
                                allowedFormats = value;
                            }
                            // master video type
                            else if ((key == prop_mastervideotype) && value) {
                                mastervideotype = value;
                            }
                            // controls position
                            else if ((key == prop_positioncontrols) && value) {
                                positioncontrols = value;
                            }
                        });
                    }
                    me.set("allowedtags", allowedTags);
                    me.set("allowedformats", allowedFormats);
                    me.set("shortcuts", shortcuts);
                    me.set("mastervideotype", mastervideotype);
                    ready = true;
                }
            });
        },
        ready: function() {
            return ready;
        },
        getPositionControls: function() {
            return positioncontrols;
        }
    });

    return MeInfoModel;
});
