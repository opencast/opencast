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
/*global require*/
define(['jquery', 'underscore', 'backbone', 'engage/core'], function ($, _, Backbone, Engage) {
  'use strict';

  var insertIntoDOM = false;
  var PLUGIN_NAME = 'Downloads';
  var PLUGIN_TYPE = 'engage_tab';
  var PLUGIN_VERSION = '1.0';
  var PLUGIN_TEMPLATE_DESKTOP = 'templates/desktop.html';
  var PLUGIN_STYLES_DESKTOP = [
    'styles/desktop.css'
  ];

  var isDesktopMode = Engage.model.get('mode') === 'desktop';

  // The following code block contains the logic for the download tab in the theodul player.
  // The download tab should only be displayed if it's explicitly allowed in the 'config.yml' file and
  // when the user has a role that matches a specific regex. The regex is also configured in the 'config.yml'.
  const video_download_allowed = Engage.model.get('meInfo').get('allow_video_download');
  if (video_download_allowed) {
    const userRoles = Engage.model.get('meInfo').get('roles');
    const roleRegex = new RegExp(Engage.model.get('meInfo').get('allowed_roles_for_video_download'));
    const roleMatches = userRoles.some(function (role) {
      return roleRegex.exec(role);
    });
    if (roleMatches) {
      insertIntoDOM = true;
    }
  }

  var events = {
    plugin_load_done: new Engage.Event('Core:plugin_load_done', '', 'handler'),
    mediaPackageModelError: new Engage.Event('MhConnection:mediaPackageModelError', '', 'handler')
  };

  var plugin = {
    insertIntoDOM: insertIntoDOM,
    name: PLUGIN_NAME,
    type: PLUGIN_TYPE,
    version: PLUGIN_VERSION,
    styles: PLUGIN_STYLES_DESKTOP,
    template: PLUGIN_TEMPLATE_DESKTOP,
    events: events
  };

  /* don't change these variables */
  var viewsModelChange = 'change:views';
  var mediapackageChange = 'change:mediaPackage';
  var initCount = 4;
  var mediapackageError = false;
  var translations = new Array();
  var Utils;


  function initTranslate(language, funcSuccess, funcError) {
    var path = Engage.getPluginPath('EngagePluginTabDownloads').replace(/(\.\.\/)/g, '');
    Engage.log('Tab:Downloads: Choosing ' + language + ' translations');
    const languageURL = `/engage/theodul/${path}language/${language}.json`;

    $.ajax({
      url: languageURL,
      dataType: 'json',
      async: false,
      success: function (data) {
        if (data) {
          data.value_locale = language;
          translations = data;
          if (funcSuccess) {
            funcSuccess(translations);
          }
        } else {
          if (funcError) {
            funcError();
          }
        }
      },
      error: function (jqXHR, textStatus, errorThrown) {
        if (funcError) {
          funcError();
        }
      }
    });
  }

  function translate(str, strIfNotFound) {
    return (translations[str] != undefined) ? translations[str] : strIfNotFound;
  }

  var DownloadsTabView = Backbone.View.extend({
    initialize: function (mediaPackageModel, template) {
      this.setElement($(plugin.container)); // every plugin view has it's own container associated with it
      this.model = mediaPackageModel;
      this.template = template;
      // bind the render function always to the view
      _.bindAll(this, 'render');
      // listen for changes of the model and bind the render function to this
      this.model.bind('change', this.render);
    },
    render: function () {
      if (!mediapackageError) {
        var src = {};
        _.each(this.model.get('tracks'), function (item) {

        });
        var tempVars = {
          str_type: translate('str_type', 'Type'),
          str_mimetype: translate('str_mimetype', 'Mimetype'),
          str_resolution: translate('str_resolution', 'Resolution'),
          str_download: translate('str_download', 'Download'),
          tracks: this.model.get('tracks'),
          str_downloads: translate('downloads', 'Downloads')
        };

        var template = _.template(this.template);
        this.$el.html(template(tempVars));

        $('#engage_tab_' + plugin.name.replace(/\s/g, '_')).text(tempVars.str_downloads);
      }
    }
  });

  function initPlugin() {
    // only init if plugin template was inserted into the DOM
    if (isDesktopMode && plugin.inserted) {
      Engage.log('Tab:Downloads initialized');
      var downloadsTabView = new DownloadsTabView(Engage.model.get('mediaPackage'), plugin.template);
      Engage.on(plugin.events.mediaPackageModelError.getName(), function (msg) {
        mediapackageError = true;
      });
      Engage.model.get('views').on('change', function () {
        downloadsTabView.render();
      });
      downloadsTabView.render();
    }
  }

  if (isDesktopMode) {
    // init event
    Engage.log('Tab:Downloads: Init');
    var relative_plugin_path = Engage.getPluginPath('EngagePluginTabDownloads');

    // load utils class

    require([relative_plugin_path + 'utils'], function (utils) {
      Engage.log('Tab:Downloads: Utils class loaded');
      Utils = new utils();
      initTranslate(Utils.detectLanguage(), function () {
        Engage.log('Tab:Downloads: Successfully translated.');
        initCount -= 1;
        if (initCount <= 0) {
          initPlugin();
        }
      }, function () {
        Engage.log('Tab:Downloads: Error translating...');
        initCount -= 1;
        if (initCount <= 0) {
          initPlugin();
        }
      });
    });

    Engage.model.on(viewsModelChange, function () {
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }
    });

    // listen on a change/set of the mediaPackage model
    Engage.model.on(mediapackageChange, function () {
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }
    });

    // all plugins loaded
    Engage.on(plugin.events.plugin_load_done.getName(), function () {
      Engage.log('Tab:Downloads: Plugin load done');
      initCount -= 1;
      if (initCount <= 0) {
        initPlugin();
      }
    });
  }

  return plugin;
});
