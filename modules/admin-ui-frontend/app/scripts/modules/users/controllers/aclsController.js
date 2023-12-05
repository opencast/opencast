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
.controller('AclsCtrl', ['$scope', 'Table', 'AclsResource', 'AclResource', 'ResourcesFilterResource', 'Notifications',
  function ($scope, Table, AclsResource, AclResource, ResourcesFilterResource, Notifications) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'name',
        label: 'USERS.ACLS.TABLE.NAME',
        sortable: true
        //}, {
        //    name:  'created',
        //    label: 'USERS.ACLS.TABLE.CREATED'
        //}, {
        //    name:  'creator',
        //    label: 'USERS.ACLS.TABLE.CREATOR'
        //}, {
        //    name:  'in_use',
        //    label: 'USERS.ACLS.TABLE.IN_USE'
      }, {
        template: 'modules/users/partials/aclActionsCell.html',
        label:    'USERS.ACLS.TABLE.ACTION'
      }],
      caption:    'USERS.ACLS.TABLE.CAPTION',
      resource:   'acls',
      category:   'users',
      apiService: AclsResource
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.delete = function (row) {
      AclResource.delete({id: row.id}, function () {
        Table.fetch();
        Notifications.add('success', 'ACL_DELETED');
      }, function () {
        Notifications.add('error', 'ACL_NOT_DELETED');
      });
    };
  }
]);
