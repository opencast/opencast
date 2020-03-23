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

/* global define */
define(['jquery', 'backbone', 'js-yaml.min'], function ($, Backbone, jsyaml) {
  'use strict';

  var configURL = '/ui/config/theodul/config.yml',
      ready = false,
      positioncontrols = '',
      config;
  /*
   * Model with information about the current user and the current MH configuration
   */
  var MeInfoModel = Backbone.Model.extend({
    urlRoot: '../../../info/me.json',
    initialize: function () {
      this.fetch({
        success: function (me) {
          var shortcuts = new Array(),
              shortcut_sequence = '',
              allowedTags,
              allowedFormats,
              mastervideotype = '',
              logo_mediamodule = '',
              logo_player = '',
              show_embed_link = false,
              hide_video_context_menu = false,
              layout = 'off',
              focusedflavor = 'presentation',
              matomo_server,
              matomo_site_id,
              matomo_heartbeat,
              matomo_notification,
              matomo_track_events;

          $.ajax({
            url: configURL,
            dataType: 'text',
            success: function (data) {
              var rawfile = data;
            }
          }).then(function (rawfile) {
            config = jsyaml.load(rawfile);
            me.set('config', config);
            me.set('allowedtags', config.allowedTags);
            me.set('allowedformats', config.allowedFormats);
            me.set('shortcuts', config.shortcuts);
            me.set('mastervideotype', config.mastervideotype);
            me.set('logo_mediamodule', config.logo_mediamodule);
            me.set('logo_player', config.logo);
            me.set('show_embed_links', config.show_embed_links);
            me.set('hide_video_context_menu', config.hide_video_context_menu);
            me.set('layout', config.layout);
            me.set('focusedflavor', config.focusedflavor);

            if (config.matomo) {
              me.set('matomo.server', config.matomo.server);
              me.set('matomo.site_id', config.matomo.site_id);
              me.set('matomo.heartbeat', config.matomo.heartbeat);
              me.set('matomo.notification', config.matomo.notification);
              me.set('matomo.track_events', config.matomo.track_events);
            }

            ready = true;
          });
        }
      });
    },
    ready: function () {
      return ready;
    },
    getPositionControls: function () {
      return config.positioncontrols;
    }
  });
  return MeInfoModel;
});
