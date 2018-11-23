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
 * @name ng.directive:adminNgEditableMultiSelect
 *
 * @description
 * Upon click on its label, this directive will display an <input> field and
 * currently selected values. They will be transformed back into a label on blur.
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
      <div admin-ng-editable-multi-select params="params" save="save" collection="collection"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableMultiSelect', ['$timeout', function ($timeout) {
  return {
    restrict: 'A',
    templateUrl: 'shared/partials/editableMultiSelect.html',
    replace: true,
    scope: {
      params:     '=',
      collection: '=',
      mixed:      '=',
      save:       '='
    },
    link: function (scope, element) {
      scope.data = {};
      scope.data.list = {};
      if (scope.params.id) {
        scope.data.list.id = scope.params.id;
      } else {
        scope.data.list.id = scope.params.name;
      }

      scope.enterEditMode = function () {
        scope.editMode = true;
        $timeout(function () {
          element.find('input').focus();
        });
      };

      scope.onBlur = function (event) {
        if (!scope.removedValue) {
          scope.leaveEditMode();
        } else {
          $timeout(function () {
            element.find('input').focus();
          });
        }
        delete scope.removedValue;
      };

      scope.leaveEditMode = function () {
        scope.editMode = false;
      };

      scope.keyUp = function (event) {
        if (event.keyCode === 13) {
          // ENTER
          if (angular.isDefined(scope.data.value)) {
            scope.addValue(scope.params.value, scope.data.value);
          }
          scope.data.value = '';
        } else if (event.keyCode === 27) {
          // ESC
          scope.leaveEditMode();
        }
        event.stopPropagation();
      };

      scope.addValue = function (model, value) {
        value = value.trim();

        if (scope.mixed || scope.collection[value]) {
          value = angular.isDefined(scope.collection[value]) ? scope.collection[value] : value;

          if (value && model.indexOf(value) === -1) {
            model.push(value);
          }
          scope.submit();
        }
      };

      scope.removeValue = function (model, value) {
        scope.removedValue = true;
        model.splice(model.indexOf(value), 1);
        scope.submit();
      };

      scope.submit = function () {
        scope.parseValues();
        if (angular.isDefined(scope.save)) {
          scope.save(scope.params.id);
        }
      };

      /**
       * This function parses the current values by removing extra whitespace and replacing values with those in the
       * collection.
       */
      scope.parseValues = function () {
        angular.forEach(scope.params.value, function(value) {
          scope.params.value[scope.params.value.indexOf(value)] =
            scope.params.value[scope.params.value.indexOf(value)].trim();
        });

        angular.forEach(scope.params.value, function(value) {
          if (angular.isDefined(scope.collection[value])) {
            scope.params.value[scope.params.value.indexOf(value)] = scope.collection[value];
          }
        });
      };

      scope.getText = function (value) {
        if (angular.isDefined(scope.collection[value])) {
          return scope.collection[value];
        } else {
          return value;
        }
      };
    }
  };
}]);
