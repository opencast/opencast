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
define(['jquery', 'backbone', 'engage/core', 'engage/models/engage'], function($, Backbone, EngageCore, EngageModel) {
  'use strict';

  var id_engage_controls = 'engage_controls';
  var id_engage_video = 'engage_video';
  var id_engage_tab = 'engage_tab';
  var id_engage_description = 'engage_description';
  var id_engage_timeline = 'engage_timeline';

  /*
     * Init logic function
     */
  var initMobileView = function() {

  };

  /*
     * Logic to insert a plugin with name and type to the player in mobile mode
     */
  var insertPluginToDOM = function(plugin) {

    switch (plugin.type) {
    case id_engage_video:
      $('#' + id_engage_video).html(plugin.templateProcessed);
      plugin.inserted = true;
      plugin.container = '#' + id_engage_video;
      break;
    case id_engage_controls:
      $('#' + id_engage_controls).html(plugin.templateProcessed);
      plugin.inserted = true;
      plugin.container = '#' + id_engage_controls;
      break;
    case id_engage_description:
      $('#' + id_engage_description).html(plugin.templateProcessed);
      plugin.inserted = true;
      plugin.container = '#' + id_engage_description;
      break;
    case id_engage_tab:
    case id_engage_timeline:
    default:
      plugin.inserted = false;
      plugin.container = '';
    }
  };

  /*
     * This function is triggered when all plugins are loaded and inserted into the DOM
     */
  var allPluginsLoadedEvent = function() {

  };

  // public functions fo the module
  return {
    initView: initMobileView,
    insertPlugin: insertPluginToDOM,
    allPluginsLoaded: allPluginsLoadedEvent
  };
});

