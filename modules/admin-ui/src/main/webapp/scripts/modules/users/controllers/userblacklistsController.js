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
.controller('UserblacklistsCtrl', ['$scope', 'Table', 'Notifications', 'UserBlacklistsResource', 'ResourcesFilterResource',
    function ($scope, Table, Notifications, UserBlacklistsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'resourceName',
                label: 'USERS.BLACKLISTS.TABLE.NAME'
            }, {
                name:  'date_from',
                label: 'USERS.BLACKLISTS.TABLE.DATE_FROM',
                dontSort: true
            }, {
                name:  'date_to',
                label: 'USERS.BLACKLISTS.TABLE.DATE_TO',
                dontSort: true
            }, {
                name:  'reason',
                label: 'USERS.BLACKLISTS.TABLE.REASON',
                translate: true
            }, {
                template: 'modules/users/partials/userblacklistActionsCell.html',
                label:    'USERS.BLACKLISTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'USERS.BLACKLISTS.TABLE.CAPTION',
            resource:   'userblacklists',
            category:   'users',
            apiService: UserBlacklistsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            row.$delete({ id: row.id }, function () {
                Table.fetch();
                Notifications.add('success', 'USER_BLACKLIST_DELETED');
            }, function () {
                Notifications.add('error', 'USER_BLACKLIST_NOT_DELETED');
            });
        };
    }
]);
