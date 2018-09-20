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


      //transform map to array so that orderBy can be used
      scope.collection = scope.ordered ? mapToArrayOrdered(scope.collection, scope.params.translatable) :
        mapToArray(scope.collection, scope.params.translatable);

      scope.submit = function () {
        // Wait until the change of the value propagated to the parent's
        // metadata object.
        scope.submitTimer = $timeout(function () {
          scope.save(scope.params.id);
        });
        scope.editMode = false;
      };

      scope.getLabel = function (searchedValue) {
        var label;

        angular.forEach(scope.collection, function (obj) {
          if (obj.value === searchedValue) {
            label = obj.label;
          }
        });

        return label;
      };

      scope.$on('$destroy', function () {
        $timeout.cancel(scope.submitTimer);
      });

      scope.enterEditMode = function () {
        // Store the original value for later comparision or undo
        if (!angular.isDefined(scope.original)) {
          scope.original = scope.params.value;
        }
        scope.editMode = true;
        scope.focusTimer = $timeout(function () {
          if ($('[chosen]')) {
            element.find('select').trigger('chosen:activate');
          }
        });
      };

      scope.leaveEditMode = function () {
        // does not work currently, as angular chose does not support ng-blur yet. But it does not break anything
        scope.editMode = false;
      };
    }
  };
}]);
