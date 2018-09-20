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

angular.module('adminNg.controllers')
.controller('NewGroupCtrl', ['$scope', '$timeout', 'Table', 'NewGroupStates', 'ResourcesListResource', 'GroupsResource',
  'Notifications', 'Modal',
  function ($scope, $timeout, Table, NewGroupStates, ResourcesListResource, GroupsResource, Notifications, Modal) {

    $scope.states = NewGroupStates.get();

    $scope.submit = function () {
      $scope.group = {
        users       : [],
        roles       : [],
        name        : $scope.states[0].stateController.metadata.name,
        description : $scope.states[0].stateController.metadata.description
      };

      angular.forEach($scope.states[2].stateController.users.selected, function (value) {
        $scope.group.users.push(value.value);
      });

      angular.forEach($scope.states[1].stateController.roles.selected, function (value) {
        $scope.group.roles.push(value.name);
      });

      GroupsResource.create($scope.group, function () {
        // Fetching immediately does not work
        $timeout(function () {
          Table.fetch();
        }, 500);
        Modal.$scope.close();

        // Reset all states
        angular.forEach($scope.states, function(state)  {
          if (angular.isDefined(state.stateController.reset)) {
            state.stateController.reset();
          }
        });

        Notifications.add('success', 'GROUP_ADDED');

      }, function (response) {
        if (response.status === 409) {
          Notifications.add('error', 'GROUP_CONFLICT', 'add-group-form');
        } else {
          Notifications.add('error', 'GROUP_NOT_SAVED', 'add-group-form');
        }
      });

    };

    // Reload tab resource on tab changes
    $scope.$parent.$watch('tab', function (value) {
      angular.forEach($scope.states, function (state) {
        if (value === state.name && !angular.isUndefined(state.stateController.reload)) {
          state.stateController.reload();
        }
      });
    });
  }
]);
