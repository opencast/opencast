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

angular.module('adminNg.controllers')
.controller('HotkeyCheatSheetCtrl', ['$scope',
  function ($scope) {
    $scope.$watchCollection('hotkeys', function (newHotkeys, oldHotkeys, scope) {
      // This function groups the functions by the first part of their key identifier

      // While managing both a list and a hash table of the groups seems redundant,
      // we do it to retain the order in which the hotkeys were added (i.e. the groups
      // are ordered by the point in time their first hotkey was added), while also
      // making this function efficient.
      var keyGroups = scope.keyGroups = [];
      var groupIndex = {};
      angular.forEach(newHotkeys, function (hotkey) {
        // Ignore keys without a description
        if (hotkey.description === '$$undefined$$') return;

        // Manage our collection bin and index
        var group = hotkey.description.split('.')[0];
        if (!(group in groupIndex)) {
          groupIndex[group] = [];
          keyGroups.push({
            name: group,
            keys: groupIndex[group]
          });
        }

        // Construct a "view model" for the hotkey
        var viewHotkey = {
          description: hotkey.description,
          combos: []
        };
        // We extract the actual keys from a "chord string" like `'a+b'`.
        angular.forEach(hotkey.combo, function (combo) {
          var chords = [];
          angular.forEach(combo.split(/\s/), function (chord) {
            var keys = [];
            angular.forEach(chord.split('+'), function (key) {
              // Translate generic `mod` key to the current platform
              if (key === 'mod') {
                if (/mac/i.test(window.navigator.platform)) {
                  keys.push('command');
                } else {
                  keys.push('ctrl');
                }
              } else {
                keys.push(key);
              }
            });
            chords.push(keys);
          });
          viewHotkey.combos.push(chords);
        });
        groupIndex[group].push(viewHotkey);
      });
    });
  }
]);
