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
 * @name ng.directive:callOnSearch
 *
 * @description
 * Call a function when searching in a dropdown menu
 * Used for pattern matching of roles
 *
 * @element field
 * The "callOnSearch" attribute contains a reference to the function to call with the current search text

 */
angular.module('adminNg.directives')
.directive('callOnSearch', ['$timeout', function ($timeout) {
  return {
    restrict: 'A',
    scope: {
      callOnSearch: '='
    },
    link : function($scope, $element) {

      if (angular.isDefined($scope.callOnSearch) && $scope.callOnSearch !== '') {
        $timeout(function() {
          var search = angular.element(angular.element($element).parent().children('.chosen-container')
                               .children('.chosen-drop').children('.chosen-search')
                               .children('input.chosen-search-input')['0']);
          search.bind('keyup.chosen paste.chosen', function () {
            if (angular.isDefined(this.value) && this.value !== '') {
              $scope.callOnSearch(this.value);
            }
          });
        });
      }
    }
  };
}]);
