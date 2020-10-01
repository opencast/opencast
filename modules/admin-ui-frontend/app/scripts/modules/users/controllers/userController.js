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
.controller('UserCtrl', ['$rootScope', '$scope', 'Table', 'UserRolesResource', 'UserResource', 'UsersResource',
  'JsHelper', 'Notifications', 'Modal', 'AuthService', 'underscore',
  function ($rootScope, $scope, Table, UserRolesResource, UserResource, UsersResource, JsHelper, Notifications, Modal,
    AuthService, _) {
    $scope.manageable = true;
    $scope.roleSlice = 100;
    // Note that the initial offset is the same size as the initial slice so that the *next* slice starts at the right
    // place
    $scope.roleOffset = $scope.roleSlice;
    var loading = false;
    // Should the External Roles tab be visible
    var showExternalRoles = false;

    var EXTERNAL_ROLE_DISPLAY = 'adminui.user.external_role_display';

    AuthService.getUser().$promise.then(function(user) {
      $scope.currentUser = user;

      if (angular.isDefined(user.org.properties[EXTERNAL_ROLE_DISPLAY])) {
        $scope.showExternalRoles = user.org.properties[EXTERNAL_ROLE_DISPLAY] === 'true';
      }
    }).catch(angular.noop);

    $scope.role = {
      available: [],
      external: [],
      selected:  [],
      derived: [],
      i18n: 'USERS.USERS.DETAILS.ROLES',
      searchable: true
    };

    $scope.searchFieldExternal = '';
    $scope.searchFieldEffective = '';

    $scope.getMoreRoles = function() {
      if (loading)
        return;

      loading = true;
      UserRolesResource.query({
        limit: $scope.roleSlice,
        offset: $scope.roleOffset,
        filter: 'role_target:USER'
      }).$promise.then(function (data) {
        $scope.role.available = $scope.role.available.concat(data);
        $scope.roleOffset = $scope.roleOffset + $scope.roleSlice;
      }, this).catch(
        angular.noop
      ).finally(function () {
        loading = false;
      });
    };

    $scope.groupSort = function(role) {

      var result = 10;

      switch (role.type) {
      case 'SYSTEM':      result = 0; break;
      case 'GROUP':       result = 1; break;
      case 'EXTERNAL_GROUP':    result = 2; break;
      case 'INTERNAL':    result = 3; break;
      case 'DERIVED':     result = 4; break;
      case 'EXTERNAL':    result = 5; break;

      default: result = 10; break;
      }

      return result;
    };

    $scope.clearSearchFieldExternal = function () {
      $scope.searchFieldExternal = '';
    };

    $scope.clearSearchFieldEffective = function () {
      $scope.searchFieldEffective = '';
    };

    $scope.customEffectiveFilter = function () {

      return function (item) {

        var result = true; //(item ? !(item.name.substring(0, ('ROLE_USER_').length) === 'ROLE_USER_') : true);
        if (result && ($scope.searchField != '')) {

          result = (item.name.toLowerCase().indexOf($scope.searchFieldEffective.toLowerCase()) >= 0);
        }

        return result;
      };
    };

    if ($scope.action === 'edit') {
      fetchChildResources($scope.resourceId);
    } else if ($scope.action === 'add') {
      $scope.role.available = UserRolesResource.query({limit: $scope.roleSlice, offset: 0, filter: 'role_target:USER'});
    }


    $scope.submit = function () {
      $scope.user.roles = [];

      angular.forEach($scope.role.selected, function (value) {
        $scope.user.roles.push({'name': value.value, 'type': value.type});
      });

      if ($scope.action === 'edit') {
        $scope.user.$update({ username: $scope.user.username }, function () {
          Notifications.add('success', 'USER_UPDATED');
          Modal.$scope.close();
          $rootScope.$emit('user_changed');
        }, function () {
          Notifications.add('error', 'USER_NOT_SAVED', 'user-form');
        });
      } else {
        UsersResource.create({ }, $scope.user, function () {
          Table.fetch();
          Modal.$scope.close();
          Notifications.add('success', 'USER_ADDED');
          Modal.$scope.close();
          $rootScope.$emit('user_changed');
        }, function () {
          Notifications.add('error', 'USER_NOT_SAVED', 'user-form');
        });
      }
    };

    /**
       * This private function updates the scope with the data of a given user.
       *
       * @param id the id of the user
       */
    function fetchChildResources(id) {
      $scope.role.available = UserRolesResource.query({limit: $scope.roleSlice, offset: 0, filter: 'role_target:USER'});
      $scope.user = UserResource.get({ username: id });
      $scope.user.$promise.then(function () {
        $scope.manageable = $scope.user.manageable;
        if (!$scope.manageable) {
          Notifications.add('warning', 'USER_NOT_MANAGEABLE', 'user-form');
        }

        $scope.role.available.$promise.then(function() {

          // Now that we have the user roles and the available roles populate the selected and available
          angular.forEach($scope.user.roles, function (role) {

            if (role.type == 'INTERNAL' || role.type == 'GROUP') {
              $scope.role.selected.push({name: role.name, value: role.name, type: role.type});
            }

            if (role.type == 'EXTERNAL' || role.type == 'EXTERNAL_GROUP') {
              $scope.role.external.push({name: role.name, value: role.name, type: role.type});
            }

            if (role.type == 'SYSTEM' || role.type == 'DERIVED') {
              $scope.role.derived.push({name: role.name, value: role.name, type: role.type});
            }
          });

          // Filter the selected from the available list
          $scope.role.available = _.filter($scope.role.available,
            function(role){ return !_.findWhere($scope.role.selected, {name: role.name}); });
        }).catch(angular.noop);
      }).catch(angular.noop);
    }

    $scope.$on('change', function (event, id) {
      // empty arrays that are going to be repopulated
      $scope.role.selected = [];
      $scope.role.derived = [];
      $scope.role.external = [];
      // clear search fields and notifications when navigating between users
      $scope.$broadcast('clear');
      Notifications.removeAll('user-form');

      fetchChildResources(id);
    });

    $scope.$on('clear', function () {
      $scope.searchFieldExternal = '';
      $scope.searchFieldEffective = '';
    });

    $scope.getHeight = function () {

      return { height : $scope.role.searchable ? '26em' : '30em' };
    };

    $scope.getSubmitButtonState = function () {
      return $scope.userForm.$valid && $scope.manageable ? 'active' : 'disabled';
    };

    // Retrieve a list of user so the form can be validated for user
    // uniqueness.
    $scope.users = [];
    UsersResource.query(function (users) {
      $scope.users = JsHelper.map(users.rows, 'username');
    });

    $scope.checkUserUniqueness = function () {
      $scope.userForm.username.$setValidity('uniqueness',
        $scope.users.indexOf($scope.user.username) > -1 ? false : true);
    };
  }
]);
