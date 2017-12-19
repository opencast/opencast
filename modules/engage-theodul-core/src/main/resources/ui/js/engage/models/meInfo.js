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
define(['jquery', 'backbone'], function($, Backbone) {
    "use strict";

    var prop_shortcut = "player.shortcut.",
        prop_shortcut_sequence = "player.shortcut-sequence",
        prop_allowedtags = "player.allowedtags",
        prop_allowedformats = "player.allowedformats",
        prop_mastervideotype = "player.mastervideotype",
        prop_positioncontrols = "player.positioncontrols",
        prop_layout = "player.layout",
        prop_focusedflavor = "player.focusedflavor",
        prop_logo_player = "logo_player",
        prop_logo_mediamodule = "logo_mediamodule",
        prop_link_mediamodule = "link_mediamodule",
        prop_show_embed_link = "show_embed_links",
        prop_piwik_server = "player.piwik.server",
        prop_piwik_site_id = "player.piwik.site_id",
        prop_piwik_heartbeat = "player.piwik.heartbeat",
        prop_piwik_track_events = "player.piwik.track_events",
        prop_hide_video_context_menu = "player.hide_video_context_menu",
        ready = false,
        positioncontrols = "";

    /*
     * Model with information about the current user and the current MH configuration
     */
    var MeInfoModel = Backbone.Model.extend({
        urlRoot: "../../../info/me.json",
        initialize: function() {
            this.fetch({
                success: function(me) {
                    var shortcuts = new Array(),
                        shortcut_sequence = "",
                        allowedTags,
                        allowedFormats,
                        mastervideotype = "",
                        logo_mediamodule = "",
                        logo_player = "",
                        link_mediamodule = false,
                        show_embed_link = false,
                        hide_video_context_menu = false,
                        layout = "off",
                        focusedflavor = "presentation",
                        piwik_server,
                        piwik_site_id,
                        piwik_heartbeat,
                        piwik_track_events;
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
                            // the seuence in which shortcuts should be presented
                            else if ((key == prop_shortcut_sequence) && value) {
                                shortcut_sequence = value;
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
                            // controls position
                            else if ((key == prop_layout) && value) {
                                layout = value;
                            }
                            // controls position
                            else if ((key == prop_focusedflavor) && value) {
                                focusedflavor = value;
                            }
                            // player logo
                            else if ((key == prop_logo_mediamodule) && value) {
                                logo_mediamodule = value;
                            }
                            // small logo
                            else if ((key == prop_logo_player) && value) {
                                logo_player = value;
                            }
                            // link to Media Modul
                            else if ((key == prop_link_mediamodule) && value) {
                                if (value.trim() == "true") link_mediamodule = true;
                            }
                            // show embed links
                            else if ((key == prop_show_embed_link) && value) {
                                if (value.trim() == "true") show_embed_link = true;
                            }
                            // hide video context menu
                            else if ((key == prop_hide_video_context_menu) && value) {
                              if (value.trim() == "true") hide_video_context_menu = true;
                            }
                            // Piwik-Settings
                            else if ((key == prop_piwik_server) && value) {
                              piwik_server = value;
                            }
                            else if ((key == prop_piwik_site_id) && value) {
                              piwik_site_id = value;
                            }
                            else if ((key == prop_piwik_heartbeat) && value) {
                              piwik_heartbeat = value;
                            }
                            else if ((key == prop_piwik_track_events) && value) {
                              piwik_track_events = value;
                            }
                        });
                    }
                    me.set("allowedtags", allowedTags);
                    me.set("allowedformats", allowedFormats);
                    me.set("shortcuts", shortcuts);
                    me.set("mastervideotype", mastervideotype);
                    me.set("logo_mediamodule", logo_mediamodule);
                    me.set("logo_player", logo_player);
                    me.set("link_mediamodule", link_mediamodule);
                    me.set("show_embed_links", show_embed_link);
                    me.set("hide_video_context_menu", hide_video_context_menu);
                    me.set("shortcut-sequence", shortcut_sequence);
                    me.set("layout", layout);
                    me.set("focusedflavor", focusedflavor);
                    me.set("piwik.server", piwik_server);
                    me.set("piwik.site_id", piwik_site_id);
                    me.set("piwik.heartbeat", piwik_heartbeat);
                    me.set("piwik.track_events", piwik_track_events);
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
