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
.directive('adminNgSelectBox', [function () {
  return {
    restrict: 'E',
    templateUrl: 'shared/partials/selectContainer.html',
    scope: {
      resource: '=',
      groupBy:  '@',
      display: '@',
      disabled: '=',
      loader: '=',
      ignore: '@',
      height: '@'
    },
    controller: function ($scope) {

      if (!$scope.display) {
        $scope.display = 'name';
      }
      $scope.searchField = '';

      $scope.customFilter = function () {

        return function (item) {

          var result = true;
          if (!angular.isUndefined($scope.ignore)) {
            result = !(item[$scope.display].substring(0, ($scope.ignore).length) ===  $scope.ignore);
          }

          if (result && ($scope.searchField != '')) {
            result = (item[$scope.display].toLowerCase().indexOf($scope.searchField.toLowerCase()) >= 0);
          }

          return result;
        };
      };

      $scope.$on('clear', function () {
        $scope.searchField = '';
      });

      $scope.customSelectedFilter = function () {

        return function (item) {
          var result = true;

          if (!angular.isUndefined($scope.ignore)) {
            result = !(item.name.substring(0, ($scope.ignore).length) ===  $scope.ignore);
          }

          return result;
        };
      };

      $scope.move = function (key, from, to) {
        var j = 0;
        for (; j < from.length; j++) {
          if (from[j].name === key) {
            to.push(from[j]);
            from.splice(j, 1);
            return;
          }
        }
      };

      $scope.add = function () {
        if (angular.isUndefined(this.markedForAddition)) { return; }

        for (var i = 0; i < this.markedForAddition.length; i++) {
          this.move(this.markedForAddition[i].name, this.resource.available, this.resource.selected);
        }

        this.markedForAddition.splice(0);
      };

      $scope.remove = function () {
        if (angular.isUndefined(this.markedForRemoval)) { return; }

        for (var i = 0; i < this.markedForRemoval.length; i++) {
          this.move(this.markedForRemoval[i].name, this.resource.selected, this.resource.available);
        }

        this.markedForRemoval.splice(0);
      };

      $scope.loadMore = function() {
        $scope.loader();
      };

      $scope.getHeight = function() {
        return {height: $scope.resource.searchable ? 'calc(' + $scope.height + ' + 3em)' : $scope.height};
      };
    }
  };
}]);
