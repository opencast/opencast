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
                    me.ready = true;
                    var hotkeys = new Array(); // hardcoded hotkeys
                    var customHotkeys = new Array(); // custom hotkeys
                    // extract hotkey infos
                    $.each(me.attributes.org.properties, function(key, value) { // hardcoded regex
                        if (key.match("theodul.hotkey.(pause|jumpToBegin|nextChapter|prevChapter|volUp|volDown|mute|jumpToX|fullscreen|nextEpisode|prevEpisode)") !== null) {
                            hotkeys.push({
                                name: key.substring(15, key.length),
                                key: value
                            })
                        } else if (key.match("theodul.hotkey.") !== null) { // else hotkeys are customs
                            var cSplit = key.substring(15, key.length).split("."); // cut key string and split into app(0) and func(1) string, examp. theodul.hotkey.annotation.add
                            customHotkeys.push({
                                app: cSplit[0],
                                func: cSplit[1],
                                key: value
                            });
                        }
                    });
                    me.set("hotkeys", hotkeys);
                    me.set("hotkeysCustom", customHotkeys);
                }
            });
        }
    });

    return MeInfoModel;
});

