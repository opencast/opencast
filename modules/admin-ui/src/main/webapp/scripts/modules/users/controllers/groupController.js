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
.controller('GroupCtrl', ['$scope', 'AuthService', 'UserRolesResource', 'ResourcesListResource', 'GroupResource',
  'GroupsResource', 'Notifications', 'Modal',
  function ($scope, AuthService, UserRolesResource, ResourcesListResource, GroupResource, GroupsResources,
    Notifications, Modal) {

    var reloadSelectedUsers = function () {
      $scope.group.$promise.then(function() {
        $scope.user.all.$promise.then(function() {
          // Now that we have the user users and the group users populate the selected and available
          $scope.user.selected = $scope.user.all.filter(function (user) {
            var foundUser = $scope.group.users.find(function (groupUser) {
              return groupUser.username === user.name;
            });
            return foundUser !== undefined;
          });
          $scope.user.available = $scope.user.all.filter(function (user) {
            var foundUser = $scope.user.selected.find(function (selectedUser) {
              return selectedUser.name === user.name;
            });
            return foundUser === undefined;
          });
        }).catch(angular.noop);
      }).catch(angular.noop);
    };

    var reloadSelectedRoles = function () {
      $scope.group.$promise.then(function() {
        $scope.role.available.$promise.then(function() {
          // Now that we have the user roles and the available roles populate the selected and available
          $scope.role.selected = [];
          angular.forEach($scope.group.roles, function (role) {
            $scope.role.selected.push({name: role, value: role});
          });
          // Filter the selected from the available list
          $scope.role.available = _.filter($scope.role.available, function(role) {
            return !_.findWhere($scope.role.selected, {name: role.name});
          });
        }).catch(angular.noop);
      }).catch(angular.noop);
    };

    var reloadRoles = function () {
      $scope.role = {
        available: UserRolesResource.query({limit: 0, offset: 0, filter: 'role_target:USER'}),
        selected:  [],
        i18n: 'USERS.GROUPS.DETAILS.ROLES',
        searchable: true
      };
      reloadSelectedRoles();
    };

    var reloadUsers = function (current_user) {
      $scope.orgProperties = {};
      if (angular.isDefined(current_user)
        && angular.isDefined(current_user.org)
        && angular.isDefined(current_user.org.properties))
      {
        $scope.orgProperties = current_user.org.properties;
      }
      $scope.user = {
        all: ResourcesListResource.query({
          resource: $scope.orgProperties['adminui.user.listname'] || 'USERS.NAME.AND.USERNAME'}),
        available: [],
        selected:  [],
        i18n: 'USERS.GROUPS.DETAILS.USERS',
        searchable: true
      };
      reloadSelectedUsers();
    };

    if ($scope.action === 'edit') {
      $scope.group = GroupResource.get({ id: $scope.resourceId }, function () {
        reloadSelectedRoles();
        reloadSelectedUsers();
      });
    }

    $scope.submit = function () {
      $scope.group.users = [];
      $scope.group.roles = [];

      angular.forEach($scope.user.selected, function (item) {
        $scope.group.users.push(item.name);
      });

      angular.forEach($scope.role.selected, function (item) {
        $scope.group.roles.push(item.name);
      });

      if ($scope.action === 'edit') {
        GroupResource.save({ id: $scope.group.id }, $scope.group, function () {
          Notifications.add('success', 'GROUP_UPDATED');
          Modal.$scope.close();
        }, function () {
          Notifications.add('error', 'GROUP_NOT_SAVED', 'group-form');
        });
      } else {
        GroupsResources.create($scope.group, function () {
          Notifications.add('success', 'GROUP_ADDED');
          Modal.$scope.close();
        }, function (response) {
          if(response.status === 409) {
            Notifications.add('error', 'GROUP_CONFLICT', 'group-form');
          } else {
            Notifications.add('error', 'GROUP_NOT_SAVED', 'group-form');
          }
        });
      }
    };

    $scope.$on('change', function (event, id) {
      $scope.$broadcast('clear');
      $scope.group = GroupResource.get({ id: id }, function () {
        reloadRoles();
        reloadSelectedUsers();
      });
    });

    $scope.getSubmitButtonState = function () {
      return $scope.groupForm.$valid ? 'active' : 'disabled';
    };

    reloadRoles();
    AuthService.getUser().$promise.then(function(current_user) {
      reloadUsers(current_user);
    }).catch(angular.noop);
  }
]);
