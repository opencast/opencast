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

/**
 * @ngdoc directive
 * @name ng.directive:adminNgEditableSingleSelect
 *
 * @description
 * Upon click on its label, this directive will display an <select> field
 * which will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`.
 * The "collection" attribute contains a hash of objects (or a promise thereof)
 * which maps values to their labels.
 * The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-single-select params="params" save="save" collection="collection"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableSingleSelect', ['$timeout', '$filter', function ($timeout, $filter) {
  return {
    restrict: 'A',
    templateUrl: 'shared/partials/editableSingleSelect.html',
    replace: true,
    scope: {
      params:     '=',
      collection: '=',
      ordered:    '=',
      save:       '='
    },
    link: function (scope, element) {

      var mapToArray = function (map, translate) {
        var array = [];

        angular.forEach(map, function (mapValue, mapKey) {
          array.push({
            label: translate ? $filter('translate')(mapKey) : mapKey,
            value: mapValue
          });
        });

        return $filter('orderBy')(array, 'label');
      };

      var mapToArrayOrdered = function (map, translate) {
        var array = [];

        angular.forEach(map, function (mapValue, mapKey) {
          var entry = JSON.parse(mapKey);
          if (entry.selectable || scope.params.value === mapValue) {
            array.push({
              label: entry,
              value: mapValue
            });
          }
        });
        array.sort(function(a, b) {
          return a.label.order - b.label.order;
        });
        return array.map(function (entry) {
          return {
            label: translate ? $filter('translate')(entry.label.label) : entry.label.label,
            value: entry.value
          };
        });
      };

      scope.oldValue = scope.params.value;

      //transform map to array so that orderBy can be used
      scope.collection = scope.ordered ? mapToArrayOrdered(scope.collection, scope.params.translatable) :
        mapToArray(scope.collection, scope.params.translatable);

      scope.editMode = false;

      scope.$on('$destroy', function () {
        $timeout.cancel(scope.submitTimer);
        $timeout.cancel(scope.focusTimer);
      });

      element.on('chosen:hiding_dropdown', 'select', function(evt, params) {
        scope.leaveEditMode();
      });

      // get tab event before chosen can swallow it so we don't have to hit tab twice
      element[0].addEventListener('keydown', function (event) {
        if (event.key === 'Tab') {

          var shift = event.getModifierState('Shift');
          var alt = event.getModifierState('Alt');
          var control = event.getModifierState('Control');

          var nextElement = null;
          var nextTabIndex = null;

          var currentTabIndex = document.activeElement.tabIndex;

          $('i.edit[tabindex]').each(function(index, element) {

            var tabIndex = element.tabIndex;

            if ((shift && tabIndex < currentTabIndex && (tabIndex > nextTabIndex || !nextTabIndex)) ||
            (!(shift || alt || control) &&  tabIndex > currentTabIndex && (tabIndex < nextTabIndex || !nextTabIndex))) {

              nextElement = element;
              nextTabIndex = tabIndex;
            }
          });

          if (nextElement && nextTabIndex >= 0) {
            nextElement.focus();
          }
        }
      }, true);

      scope.keyUp = function (event) {
        if (event.key === 'Escape' || event.key === 'Esc') {
          scope.leaveEditMode();
          event.stopPropagation();
        }
      };

      scope.getLabel = function () {
        var label = '';

        if (scope.collection.length === 0) {
          label = 'SELECT_NO_OPTIONS_AVAILABLE';
        }
        else if (scope.params.value === '' || scope.params.value === null || angular.isUndefined(scope.params.value)) {
          label = 'SELECT_NO_OPTION_SELECTED';
        }
        else {
          angular.forEach(scope.collection, function (obj) {
            if (obj.value === scope.params.value) {
              label = obj.label;
            }
          });
        }

        return label;
      };

      scope.submit = function () {
        // Wait until the change of the value propagated to the parent's metadata object.
        scope.submitTimer = $timeout(function () {
          if (scope.params.value !== scope.oldValue) {
            scope.save(scope.params.id);
            scope.oldValue = scope.params.value;
          }
        });
      };

      scope.enterEditMode = function (event) {
        if (event) {
          event.stopPropagation();
        }
        if (scope.editMode == false && scope.collection.length != 0) {

          scope.editMode = true;
          scope.focusTimer = $timeout(function () {
            element.find('select').trigger('chosen:open');
          });
        }
      };

      scope.leaveEditMode = function () {
        scope.editMode = false;
      };
    }
  };
}]);
