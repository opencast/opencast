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

// Controller for all recording blacklists.
angular.module('adminNg.controllers')
.controller('LocationblacklistsCtrl', ['$scope', 'Table', 'Notifications', 'LocationBlacklistsResource', 'ResourcesFilterResource',
    function ($scope, Table, Notifications, LocationBlacklistsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'resourceName',
                label: 'USERS.BLACKLISTS.TABLE.NAME'
            }, {
                name:  'date_from',
                label: 'USERS.BLACKLISTS.TABLE.DATE_FROM'
            }, {
                name:  'date_to',
                label: 'USERS.BLACKLISTS.TABLE.DATE_TO'
            }, {
                name:  'reason',
                label: 'USERS.BLACKLISTS.TABLE.REASON',
                translate: true
            }, {
                template: 'modules/recordings/partials/locationblacklistActionsCell.html',
                label:    'USERS.BLACKLISTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'RECORDINGS.BLACKLISTS.TABLE.CAPTION',
            resource:   'locationblacklists',
            category:   'recordings',
            apiService: LocationBlacklistsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            row.$delete({ id: row.id }, function () {
                Table.fetch();
                Notifications.add('success', 'LOCATION_BLACKLIST_DELETED');
            }, function () {
                Notifications.add('error', 'LOCATION_BLACKLIST_NOT_DELETED');
            });
        };
    }
]);
