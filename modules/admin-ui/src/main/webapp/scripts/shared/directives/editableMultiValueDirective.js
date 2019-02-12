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
 * @name ng.directive:adminNgEditableMultiValue
 *
 * @description
 * Upon click on its label, this directive will display an <input> field and
 * currently selected values. They will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`.
 * The "save" attribute is a reference to a save function used to persist the
 * values.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-multi-value params="params" save="save"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableMultiValue', ['$timeout', function ($timeout) {
  return {
    restrict: 'A',
    templateUrl: 'shared/partials/editableMultiValue.html',
    replace: true,
    scope: {
      params:     '=',
      save:       '='
    },
    link: function (scope, element) {
      scope.data = {};

      scope.enterEditMode = function () {
        scope.editMode = true;
        $timeout(function () {
          element.find('input').focus();
        });
      };

      scope.onBlur = function () {
        if (!scope.removedValue) {
          scope.addCurrentValue();
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

      scope.keyUp = function (event) {
        if (event.keyCode === 13) {
          // ENTER
          scope.addCurrentValue();
          scope.data.value = '';
        } else if (event.keyCode === 27) {
          // ESC
          scope.leaveEditMode();
        }
        event.stopPropagation();
      };

      scope.addCurrentValue = function () {
        if (angular.isDefined(scope.data.value) && scope.data.value) {
          scope.addValue(scope.params.value, scope.data.value);
        }
      };

      scope.addValue = function (model, value) {
        value = value.trim();

        if (value && model.indexOf(value) === -1) {
          model.push(value);
          scope.submit();
        }
      };

      scope.removeValue = function (model, value) {
        scope.removedValue = true;
        model.splice(model.indexOf(value), 1);
        scope.submit();
      };



      scope.submit = function () {
        if (angular.isDefined(scope.save)) {
          scope.save(scope.params.id);
        }
      };
    }
  };
}]);
