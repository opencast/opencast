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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('BulkDeleteCtrl', ['$scope', 'Modal', 'Table', 'Notifications', 'BulkDeleteResource',
        function ($scope, Modal, Table, Notifications, BulkDeleteResource) {
    Notifications;
    // make a shallow copy of selected main Table rows for our own use
    $scope.rows = [];
    angular.forEach(Table.getSelected(), function (row) {
        $scope.rows.push($.extend({}, row ));
    });
    $scope.all = true; // by default, all records are selected

    var getSelectedIds = function () {
        var result = [];
        angular.forEach($scope.rows, function (row) {
            if(row.selected) {
                result.push(row.id);
            }
        });
        return result;
    };

    $scope.valid = function () {
        return getSelectedIds().length > 0;
    };

    $scope.submit = function () {
        if ($scope.valid()) {
            var selecteds = getSelectedIds(),
            resource = Table.resource.indexOf('series') >= 0 ? 'series' : 'event',
            endpoint = Table.resource.indexOf('series') >= 0 ? 'deleteSeries' : 'deleteEvents';
            BulkDeleteResource.delete({}, {
                resource: resource,
                endpoint: endpoint,
                eventIds: selecteds
            }, function () {
                Notifications.add('success', 'EVENTS_DELETED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'EVENTS_NOT_DELETED');
                Modal.$scope.close();
            });
        }
    };

    $scope.toggleSelectAll = function () {
        if ($scope.all) {
            angular.forEach($scope.rows, function (row) {
                row.selected = true;
            });
        } else {
            angular.forEach($scope.rows, function (row) {
                row.selected = false;
            });
        }
    };

}]);
