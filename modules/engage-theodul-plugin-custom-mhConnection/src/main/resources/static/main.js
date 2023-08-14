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
/*global define*/
define(['require', 'jquery', 'backbone', 'engage/core'], function(require, $, Backbone, Engage) {
  'use strict';

  var insertIntoDOM = true;
  var PLUGIN_NAME = 'Engage Custom Endpoint Connection';
  var PLUGIN_TYPE = 'engage_custom';
  var PLUGIN_VERSION = '1.0';
  var PLUGIN_TEMPLATE_DESKTOP = 'none';
  var PLUGIN_TEMPLATE_MOBILE = 'none';
  var PLUGIN_TEMPLATE_EMBED = 'none';
  var PLUGIN_STYLES_DESKTOP = [
    ''
  ];
  var PLUGIN_STYLES_EMBED = [
    ''
  ];
  var PLUGIN_STYLES_MOBILE = [
    ''
  ];

  var plugin;
  var events = {
    mediaPackageModelError: new Engage.Event('MhConnection:mediaPackageModelError',
      'A mediapackage model error occured', 'trigger'),
    mediaPackageModelInternalError: new Engage.Event('MhConnection:mediaPackageModelInternalError',
      'A mediapackage model error occured', 'handler'),
    plugin_load_done: new Engage.Event('Core:plugin_load_done',
      'when the core loaded the event successfully', 'handler'),
    getMediaInfo: new Engage.Event('MhConnection:getMediaInfo', '', 'handler'),
    getMediaPackage: new Engage.Event('MhConnection:getMediaPackage', '', 'handler')
  };

  var isDesktopMode = false;
  var isEmbedMode = false;
  var isMobileMode = false;

  // desktop, embed and mobile logic
  switch (Engage.model.get('mode')) {
  case 'embed':
    plugin = {
      insertIntoDOM: insertIntoDOM,
      name: PLUGIN_NAME,
      type: PLUGIN_TYPE,
      version: PLUGIN_VERSION,
      styles: PLUGIN_STYLES_EMBED,
      template: PLUGIN_TEMPLATE_EMBED,
      events: events
    };
    break;
  case 'mobile':
    plugin = {
      insertIntoDOM: insertIntoDOM,
      name: PLUGIN_NAME,
      type: PLUGIN_TYPE,
      version: PLUGIN_VERSION,
      styles: PLUGIN_STYLES_MOBILE,
      template: PLUGIN_TEMPLATE_MOBILE,
      events: events
    };
    isMobileMode = true;
    break;
  case 'desktop':
  default:
    plugin = {
      insertIntoDOM: insertIntoDOM,
      name: PLUGIN_NAME,
      type: PLUGIN_TYPE,
      version: PLUGIN_VERSION,
      styles: PLUGIN_STYLES_DESKTOP,
      template: PLUGIN_TEMPLATE_DESKTOP,
      events: events
    };
    isDesktopMode = true;
    break;
  }

  /* change these variables */
  var SEARCH_ENDPOINT = '/search/episode.json';

  /* don't change these variables */
  var initCount = 5;
  var InfoMeModel;
  var MediaPackageModel;
  var ViewsModel;
  var FootprintCollection;
  var mediaPackageID = '';
  var mediaPackage; // mediaPackage data
  var mediaInfo; // media info like video tracks and attachments
  var translations = new Array();
  var initialized = false;

  function initTranslate(language) {
    var path = Engage.getPluginPath('EngagePluginCustomMhConnection').replace(/(\.\.\/)/g, '');
    var jsonstr = window.location.origin + '/engage/theodul-deprecated/' + path; // this soln is really bad, fix it...

    Engage.log('Controls: selecting language ' + language);
    jsonstr += 'language/' + language + '.json';
    $.ajax({
      url: jsonstr,
      dataType: 'json',
      success: function(data) {
        if (data) {
          data.value_locale = language;
          translations = data;
        }
      }
    });
  }

  function translate(str, strIfNotFound) {
    return (translations[str] != undefined) ? translations[str] : strIfNotFound;
  }

  function extractMediaInfo() {
    if (mediaPackage) {
      mediaInfo = {};
      mediaInfo.tracks = mediaPackage.mediapackage.media.track;
      mediaInfo.attachments = mediaPackage.mediapackage.attachments.attachment;
      mediaInfo.title = mediaPackage.dcTitle;
      mediaInfo.creator = mediaPackage.dcCreator;
      mediaInfo.date = mediaPackage.dcCreated;
    } else {
      Engage.trigger(plugin.events.mediaPackageModelError.getName(),
        translate('error_noMediaInformationAvailable', 'No media information are available.'));
    }
  }

  /**
     * callSearchEndpoint
     *
     * @param callback
     */
  function callSearchEndpoint(callback) {
    if (callback === 'function') {
      $.ajax({
        url: SEARCH_ENDPOINT,
        data: {
          id: mediaPackageID
        },
        cache: false
      }).done(function(data) {
        // split search results
        if (data && data['search-results'] && data['search-results'].result) {
          mediaPackage = data['search-results'].result;
          extractMediaInfo();
        } else {
          Engage.trigger(plugin.events.mediaPackageModelError.getName(),
            translate('error_endpointNotAvailable', 'A requested search endpoint is currently not available.'));
        }
        callback();
      });
    }
  }

  /**
     * Initialize the plugin
     */
  function initPlugin() {
    if (!initialized) {
      initialized = true;
      initTranslate(Engage.model.get('language'));
      Engage.model.set('infoMe', new InfoMeModel());
      Engage.model.set('mediaPackage', new MediaPackageModel());
      Engage.model.set('views', new ViewsModel());
      Engage.model.set('footprints', new FootprintCollection());
    }
  }

  // init event
  Engage.log('MhConnection: Init');
  var relative_plugin_path = Engage.getPluginPath('EngagePluginCustomMhConnection');

  // get mediaPackage ID
  mediaPackageID = Engage.model.get('urlParameters').id;
  if (!mediaPackageID) {
    mediaPackageID = '';
  }

  Engage.on(plugin.events.mediaPackageModelInternalError.getName(), function() {
    var msg = translate('error_mediaPackageInformationNotLoaded', 'There are two possible reasons for this error');
    var rsn1 = translate('error_mediaPackageInformationNotLoaded_reason1', 'The media is not available any more');
    var rsn2 = translate('error_mediaPackageInformationNotLoaded_reason2',
      'The media is protected and you need to log in');
    msg += '<ul><li>' + rsn1 + '</li><li>' + rsn2 + '</li></ul>';
    Engage.trigger(events.mediaPackageModelError.getName(), msg);
  });

  Engage.on(plugin.events.getMediaInfo.getName(), function(callback) {
    if (callback === 'function') {
      if (!mediaPackage && !mediaInfo) {
        callSearchEndpoint(function() {
          callback(mediaInfo);
        });
      } else {
        callback(mediaInfo);
      }
    }
  });

  Engage.on(plugin.events.getMediaPackage.getName(), function(callback) {
    if (callback === 'function') {
      if (!mediaPackage) {
        callSearchEndpoint(function() {
          callback(mediaPackage);
        });
      } else {
        callback(mediaPackage);
      }
    }
  });

  // all plugins loaded
  Engage.on(plugin.events.plugin_load_done.getName(), function() {
    Engage.log('MhConnection: Plugin load done');
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  // load infoMe model
  require([relative_plugin_path + 'models/infoMe'], function(model) {
    Engage.log('MhConnection: InfoMeModel loaded');
    InfoMeModel = model;
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  // load mediaPackage model
  require([relative_plugin_path + 'models/mediaPackage'], function(model) {
    Engage.log('MhConnection: MediaPackageModel loaded');
    MediaPackageModel = model;
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  // load views model
  require([relative_plugin_path + 'models/views'], function(model) {
    Engage.log('MhConnection: ViewsModel loaded');
    ViewsModel = model;
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  // load footprint collection
  require([relative_plugin_path + 'collections/footprint'], function(collection) {
    Engage.log('MhConnection: FootprintCollection loaded');
    FootprintCollection = collection;
    initCount -= 1;
    if (initCount <= 0) {
      initPlugin();
    }
  });

  return plugin;
});
