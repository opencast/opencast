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
'use strict';

angular.module('adminNg.services')
.factory('HotkeysService', ['$q', 'IdentityResource', 'hotkeys',
  function ($q, IdentityResource, hotkeys) {
    var HotkeysService = function () {
      var me = this,
          identity,
          keyBindings,
          loading;

      this.keyBindings = {};

      this.loadHotkeys = function () {
        return $q(function (resolve, reject) {
          identity = IdentityResource.get();
          identity.$promise.then(function (info) {
            if (info && info.org && info.org.properties && info.org.properties) {
              var properties = info.org.properties;
              angular.forEach(Object.keys(properties), function (key) {
                if (key.indexOf('admin.shortcut.') >= 0) {
                  var keyIdentifier = key.substring('admin.shortcut.'.length),
                      value = properties[key];
                  me.keyBindings[keyIdentifier] = value;
                }
              });
              resolve();
            } else {
              reject(); // as no hotkeys have been loaded
            }
          }).catch(angular.noop);
        });
      };

      function activateHotkey(hotkeys, keyIdentifier, callback) {
        me.loading.then(function () {
          var key = me.keyBindings[keyIdentifier];
          if (key) {
            hotkeys.add({
              combo: key,
              // We abuse the `description` argument of `angular-hotkeys` a bit here,
              // since it is the only place it offers to store custom hotkey identifying
              // information, which we need in our custom cheat sheet template
              // to translate and group the keys.
              description: keyIdentifier,
              callback: callback
            });
          }
        });
      }

      this.activateHotkey = function (scope, keyIdentifier, callback) {
        activateHotkey(hotkeys.bindTo(scope), keyIdentifier, callback);
      };

      this.activateUniversalHotkey = function (keyIdentifier, callback) {
        activateHotkey(hotkeys, keyIdentifier, callback);
      };

      this.loading = this.loadHotkeys();
    };

    return new HotkeysService();
  }]);
