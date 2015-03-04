/**
* Copyright 2009-2013 The Regents of the University of California
* Licensed under the Educational Community License, Version 2.0
* (the "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.osedu.org/licenses/ECL-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an "AS IS"
* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*
*/
'use strict';

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('BulkDeleteCtrl', ['$scope', 'Modal', 'Table', 'Notifications', 'BulkDeleteResource',
        function ($scope, Modal, Table, Notifications, BulkDeleteResource) {
    Notifications;
    $scope.rows = Table.getSelected();
    $scope.all = true; // by default, all records are selected

    var getSelectedEventIds = function () {
        var result = [];
        angular.forEach(Table.getSelected(), function (selected) {
            result.push(selected.id);
        });
        return result;
    };

    $scope.valid = function () {
        return Table.getSelected().length > 0;
    };

    $scope.submit = function () {
        if ($scope.valid()) {
            var selecteds = getSelectedEventIds(),
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
        Table.all = !Table.all;
        Table.toggleAllSelectionFlags();
    };

}]);
