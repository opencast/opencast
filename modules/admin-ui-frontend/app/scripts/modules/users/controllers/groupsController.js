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
.controller('GroupsCtrl', ['$scope', 'Table', 'Modal', 'GroupsResource', 'GroupResource', 'ResourcesFilterResource',
  'Notifications',
  function ($scope, Table, Modal, GroupsResource, GroupResource, ResourcesFilterResource, Notifications) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'name',
        label: 'USERS.GROUPS.TABLE.NAME',
        sortable: true
      }, {
        name:  'description',
        label: 'USERS.GROUPS.TABLE.DESCRIPTION',
        sortable: true
      }, {
        name:  'role',
        label: 'USERS.GROUPS.TABLE.ROLE',
        sortable: true
      }, {
        template: 'modules/users/partials/groupActionsCell.html',
        label:    'USERS.USERS.TABLE.ACTION'
      }],
      caption:    'USERS.GROUPS.TABLE.CAPTION',
      resource:   'groups',
      category:   'users',
      apiService: GroupsResource
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.delete = function (row) {
      GroupResource.delete({id: row.id}, function () {
        Table.fetch();
        Modal.$scope.close();
        Notifications.add('success', 'GROUP_DELETED');
      }, function () {
        Notifications.add('error', 'GROUP_NOT_DELETED');
      });
    };
  }
]);
