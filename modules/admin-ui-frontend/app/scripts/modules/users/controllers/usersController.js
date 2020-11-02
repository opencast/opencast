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
.controller('UsersCtrl', ['$rootScope', '$scope', 'Table', 'UsersResource', 'UserResource', 'ResourcesFilterResource',
  'Notifications', 'Modal',
  function ($rootScope, $scope, Table, UsersResource, UserResource, ResourcesFilterResource, Notifications, Modal) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'name',
        label: 'USERS.USERS.TABLE.NAME',
        sortable: true
      }, {
        name:  'username',
        label: 'USERS.USERS.TABLE.USERNAME',
        sortable: true
      }, {
        name:  'email',
        label: 'USERS.USERS.TABLE.EMAIL',
        sortable: true
      }, {
        name:  'roles',
        label: 'USERS.USERS.TABLE.ROLES',
        sortable: true
      }, {
        name:  'provider',
        label: 'USERS.USERS.TABLE.PROVIDER',
        sortable: true
      }, {
        template: 'modules/users/partials/userActionsCell.html',
        label:    'USERS.USERS.TABLE.ACTION'
      }],
      caption:    'USERS.USERS.TABLE.CAPTION',
      resource:   'users',
      category:   'users',
      apiService: UsersResource
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.delete = function (row) {
      UserResource.delete({username: row.id}, function () {
        Table.fetch();
        Modal.$scope.close();
        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
        Notifications.add('success', 'USER_DELETED');
      }, function () {
        Notifications.add('error', 'USER_NOT_DELETED');
      });
    };

    $rootScope.$on('user_changed', function() {
      $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
    });

  }
]);
