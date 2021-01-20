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
 * The "delimiter" attribute specifies at what character the input string
 * is to be split into multiple individual values.
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

      scope.onBlur = function () {
        if (!scope.removedValue) {
          scope.addValue();
          scope.leaveEditMode();
        } else {
          $timeout(function () {
            element.find('input').focus();
          });
        }
        delete scope.removedValue;
      };

      scope.leaveEditMode = function () {
        scope.data.value = '';
        scope.editMode = false;
      };

      scope.removeValue = function (index) {
        scope.removedValue = true;
        scope.params.value.splice(index, 1);
        scope.submit();
      };

      scope.addValue = function () {
        if (!scope.data.value) {
          return;
        }

        var modelUpdated = false;
        var value = scope.data.value;
        var model = scope.params.value;
        var newValues = scope.params.delimiter
          ? value.split(scope.params.delimiter)
          : [value];

        var previousValues = model.slice();
        var failed = false;

        angular.forEach(newValues, function (newValue) {
          if (failed) {
            return;
          }

          var parseResult = scope.parseValue(newValue);
          if (!parseResult.value) {
            return;
          }
          if (scope.mixed || parseResult.found) {
            if (parseResult.value && model.indexOf(parseResult.value) < 0) {
              model.push(parseResult.value);
              modelUpdated = true;
            }
          } else {
            failed = true;
          }
        });

        if (failed) {
          scope.params.value = previousValues;
        } else if (modelUpdated === true) {
          scope.submit();
        }
      };

      scope.keyUp = function (event) {
        if (event.keyCode === 13) {
          // ENTER
          scope.addValue();
          scope.data.value = '';
        } else if (event.keyCode === 27) {
          // ESC
          scope.leaveEditMode();
        }
        event.stopPropagation();
      };

      scope.submit = function () {
        if (angular.isDefined(scope.save)) {
          scope.save(scope.params.id);
        }
      };

      /**
       * This function parses the current values by removing extra whitespace and replacing values with those in the
       * collection.
       */
      scope.parseValues = function () {
        angular.forEach(scope.params.value, function (value, index) {
          scope.params.value[index] = scope.parseValue(value).value;
        });
      };

      scope.parseValue = function (value) {
        value = value.trim();
        return scope.getText(value);
      };

      scope.getText = function (value) {
        var collectionValue = scope.collection[value];
        if (angular.isDefined(collectionValue)) {
          return {
            found: true,
            value: collectionValue
          };
        } else {
          return {
            found: false,
            value: value
          };
        }
      };

      scope.parseValues();
      scope.$watchCollection('collection', scope.parseValues);
    }
  };
}]);
