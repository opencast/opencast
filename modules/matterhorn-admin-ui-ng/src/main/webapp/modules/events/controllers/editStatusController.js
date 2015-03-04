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
.controller('EditStatusCtrl', ['$scope', 'Modal', 'Table', 'OptoutsResource', 'Notifications',
        function ($scope, Modal, Table, OptoutsResource, Notifications) {
    var getSelectedIds = function () {
        var result = [];
        angular.forEach(Table.getSelected(), function (selected) {
            result.push(selected.id);
        });
        return result;
    };

    $scope.rows = Table.getSelected();
    $scope.all = true; // by default, all records are selected
    $scope.changeStatus = function (newStatus) {
        $scope.status = newStatus;
    };

    $scope.valid = function () {
        return Table.getSelected().length > 0 && angular.isDefined($scope.status);
    };

    $scope.submit = function () {
        var resource = Table.resource.indexOf('series') >= 0 ? 'series' : 'event';
        if ($scope.valid()) {
            OptoutsResource.save({
                resource: resource,
                eventIds: getSelectedIds(),
                optout: $scope.status
            }, function () {
                Notifications.add('success', 'EVENTS_UPDATED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'EVENTS_NOT_UPDATED');
                Modal.$scope.close();
            });
        }
    };

    $scope.toggleSelectAll = function () {
        Table.all = !Table.all;
        Table.toggleAllSelectionFlags();
    };

}]);
