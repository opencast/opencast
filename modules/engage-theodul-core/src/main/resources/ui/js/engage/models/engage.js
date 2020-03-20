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

// eslint-disable-next-line no-undef
define(['jquery', 'backbone', 'bowser', 'basil', 'engage/models/pluginInfo', 'engage/models/meInfo'],
  function($, Backbone, Bowser, Basil, PluginInfoModel, MeInfoModel) {
    'use strict';

    var PluginModelCollection = Backbone.Collection.extend({});

    var basilOptions = {
      namespace: 'mhStorage'
    };
    Basil = new window.Basil(basilOptions);

    /*
     * Main Model Prototype
     */
    var EngageModel = Backbone.Model.extend({
      initialize: function() {
        // parse url parameters
        let urlParams = {},
            searchParams = new URLSearchParams(window.location.search),
            entries = searchParams.entries();
        for(var pair of entries) {
          urlParams[pair[0]] = pair[1];
        }

        this.set('orientation', 'landscape');
        this.set('urlParameters', urlParams);

        // set players debug mode
        this.set('isDebug', this.get('urlParameters').debug == 'true');
        this.set('isEventDebug', this.get('urlParameters').debugEvents == 'true');
        // set autplay mode
        this.set('autoplay', this.get('urlParameters').autoplay == 'true');
        // set initial seek time
        if (this.get('urlParameters').time) {
          this.set('time', this.get('urlParameters').time);
        }
        // set quality
        if (this.get('urlParameters').quality) {
          this.set('quality', this.get('urlParameters').quality);
        }
        // check mode, if no mode param given try to discover browser
        if (this.get('urlParameters').mode == 'desktop') {
          this.set('mode', 'desktop');
        } else if (this.get('urlParameters').mode == 'embed') {
          this.set('mode', 'embed');
        } else if (this.get('urlParameters').mode == 'mobile') {
          this.set('mode', 'mobile');
        } else {
          this.set('mode', (Bowser.mobile) ? 'mobile' : 'desktop');
        }

        // Check for user setting 'Support unsupported browser'
        Basil.set('overrideBrowser', this.get('urlParameters').browser == 'all');

        // Check for user setting 'Preferred format'
        if (this.get('urlParameters').format != null) {
          Basil.set('preferredFormat', this.get('urlParameters').format);
        } else {
          Basil.set('preferredFormat', '');
        }

        /*
            if (window.console) {
                console.log('EngageModel: Player mode: ' + this.get('mode'));
            }
	    */
      },
      defaults: {
        'pluginsInfo': new PluginInfoModel(),
        'pluginModels': new PluginModelCollection(),
        'meInfo': new MeInfoModel(),
        'urlParameters': {},
        'language': 'en-US',
        'captions': false
      }
    });

    return EngageModel;
  });
