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

// Controller for all single series screens.
angular.module('adminNg.controllers')
.controller('ScheduleTaskCtrl', ['$scope', 'Table', 'FormNavigatorService', 'NewEventProcessing', 'TaskResource', 'Notifications',
function ($scope, Table, FormNavigatorService, NewEventProcessing, TaskResource, Notifications) {
    var onSuccess, onFailure;
    // make a shallow copy of selected main Table rows for our own use
    $scope.rows = [];
    angular.forEach(Table.getSelected(), function (row) {
        $scope.rows.push($.extend({}, row ));
    });
    $scope.navigateTo = function (targetForm, currentForm, requiredForms) {
        $scope.currentForm = FormNavigatorService.navigateTo(targetForm, currentForm, requiredForms);
    };
    $scope.currentForm = 'generalForm';
    $scope.processing = NewEventProcessing.get('tasks');

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
    
    onSuccess = function () {
    	$scope.submitButton = false;
        $scope.close();
        Notifications.add('success', 'TASK_CREATED');
    };

    onFailure = function () {
    	$scope.submitButton = false;
        $scope.close();
        Notifications.add('error', 'TASK_NOT_CREATED', 'global', -1);
    };

    $scope.submitButton = false;
    $scope.submit = function () {
    	$scope.submitButton = true;
        if ($scope.valid()) {
            var eventIds = getSelectedIds(), payload;
            payload = {
                workflows: $scope.processing.ud.workflow.id,
                configuration: $scope.processing.ud.workflow.selection.configuration,
                eventIds: eventIds
            };
            TaskResource.save(payload, onSuccess, onFailure);
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
    
    $scope.numSelected = function () {
        return getSelectedIds().length;
    };
}]);
