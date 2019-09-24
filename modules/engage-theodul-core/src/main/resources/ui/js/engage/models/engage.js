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
        var match, pl = /\+/g, // regex for replacing addition symbol
            // with a space
            search = /([^&=]+)=?([^&]*)/g,
            decode = function(s) {
              return decodeURIComponent(s.replace(pl, ' '));
            },
            query = window.location.search.substring(1);

        var urlParams = {}; // stores url params
        while (match == search.exec(query)) {
          urlParams[decode(match[1])] = decode(match[2]);
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
          switch (this.get('urlParameters').quality) {
          case 'low':
            this.set('quality', 'low');
            break;
          case 'medium':
            this.set('quality', 'medium');
            break;
          case 'high':
            this.set('quality', 'high');
            break;
          default:
            this.set('quality', 'medium');
            break;
          }
        } else {
          this.set('quality', 'medium');
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
