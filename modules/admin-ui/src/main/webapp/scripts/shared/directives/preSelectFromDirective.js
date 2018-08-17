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

angular.module('adminNg.directives')
.directive('preSelectFrom', [ 'underscore',
function (_) {

    return {
        restrict: 'A',
        require: ['ngModel'],
        template: '',
        replace: false,
        link: function ($scope, $element, $attr, ngModel) {

            if(angular.isUndefined($attr.preSelectFrom)) {
                console.error('directive preSelectFrom requires a value');
            }

            var unregister = $scope.$watch($attr.preSelectFrom, function (options) {
                if (!_.isUndefined(options)) {

                    // fix angular resource objects
                    if (_.has(options, 'toJSON')) {
                        options = options.toJSON();
                    }

                    if (_.size(options) === 1 && _.size(ngModel) > 0) { // supports objects and arrays
                        var valueToSet = options[_.keys(options)[0]];
                        // There's an additional attribute you can set: "pre-select-from-value", which
                        // sets the value not to the "v" in the array itself, but to "v.value". This
                        // is currently used in the metadata fields for certain dialogs.
                        if (!angular.isUndefined($attr.preSelectFromValue)) {
                            valueToSet = valueToSet.value;
                        }
                        ngModel[0].$setViewValue(valueToSet, 'myevent');
                        unregister();
                    }
                }
            });
        }
    };
}]);
