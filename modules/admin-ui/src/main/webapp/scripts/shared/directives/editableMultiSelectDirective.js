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
                scope.parseValues();
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                    element.find('input').focus();
                });
            };

            scope.leaveEditMode = function () {
                scope.storeValues();
                scope.editMode = false;
                scope.value = '';
            };

            scope.addValue = function (model, value) {
                if (value && model.indexOf(value) === -1) {
                    model.push(value);
                    scope.editMode = false;
                }
                scope.submit();
            };

            scope.removeValue = function (model, value) {
                model.splice(model.indexOf(value), 1);
                scope.submit();
            };

            scope.storeValues = function () {
              scope.parseValues();
              if (scope.mixed || scope.collection[scope.value]) {
                  var newValue = angular.isDefined(scope.collection[scope.value]) ? scope.collection[scope.value] : scope.value;
                  scope.addValue(scope.params.value, newValue);
              }
            };

            scope.keyUp = function (event) {
                var value = event.target.value;
                if (angular.isDefined(scope.value)) {
                    scope.value = scope.value.trim();
                }
                if (event.keyCode === 13) {
                    // ENTER
                    scope.storeValues();
                } else if (event.keyCode === 27) {
                    // ESC
                    scope.editMode = false;
                } else if (value.length >= 2) {
                    // TODO update the collection
                    scope.collection = scope.collection;
                }
                event.stopPropagation();
            };

            scope.getText = function (value) {
                if (angular.isDefined(scope.collection[value])) {
                    return scope.collection[value];
                } else {
                    return value;
                }
            };

            /**
             * This function parses the current values by removing extra whitespace and replacing values with those in the collection.
             */
            scope.parseValues = function () {
                scope.trimValues();
                scope.findCollectionValue();
            };

            /**
             * This function trims the whitespace from all of the values.
             */
            scope.trimValues = function () {
               angular.forEach(scope.params.value, function(value) {
                   scope.params.value[scope.params.value.indexOf(value)] = scope.params.value[scope.params.value.indexOf(value)].trim();
               });
            };

            /**
             * This function replaces all of the current values with those in the collection.
             */
            scope.findCollectionValue = function() {
                angular.forEach(scope.params.value, function(value) {
                    if (angular.isDefined(scope.collection[value])) {
                        scope.params.value[scope.params.value.indexOf(value)] = scope.collection[value];
                    }
                });
            };

            scope.submit = function () {
                scope.parseValues();
                if (angular.isDefined(scope.save)) {
                    scope.save(scope.params.id);
                }
                scope.editMode = false;
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
       }
    };
}]);
