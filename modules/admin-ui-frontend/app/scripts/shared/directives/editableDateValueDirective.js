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
 * @name ng.directive:adminNgEditableDateValue
 *
 * @description
 * Upon click on its label, this directive will display an <input> field
 * which will be supported by a date picker.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`. The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
 <doc:example>
 <doc:source>
 <div admin-ng-editable-date params="params" save="save"></div>
 </doc:source>
 </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableDateValue', ['$timeout', function ($timeout) {
  return {
    restrict: 'A',
    templateUrl: 'shared/partials/editableDateValue.html',
    replace: true,
    scope: {
      params: '=',
      save: '='
    },
    link: function (scope, element) {
      scope.enterEditMode = function () {
        // Store the original value for later comparision or undo
        if (!angular.isDefined(scope.original)) {
          scope.original = scope.params.value;
        }
        scope.editMode = true;
        scope.focusTimer = $timeout(function () {
          element.find('input').focus();
        });
      };

      scope.keyUp = function (event) {
        if (event.keyCode === 27) {
          // Restore original value on ESC
          scope.params.value = scope.original;
          scope.editMode = false;
          // Prevent the modal from closing.
          event.stopPropagation();
        }
        if (event.keyCode === 13) {
          scope.submit();
        }
      };

      scope.submit = function () {
        // Prevent submission if value has not changed.
        if (scope.params.value === scope.original) { return; }

        scope.editMode = false;

        scope.save(scope.params.id, function () {
          scope.original = scope.params.value;
        });
      };

      scope.$on('$destroy', function () {
        $timeout.cancel(scope.focusTimer);
      });
    }
  };
}]);
