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
    $scope.rows = Table.getSelected();
    $scope.navigateTo = function (targetForm, currentForm, requiredForms) {
        $scope.currentForm = FormNavigatorService.navigateTo(targetForm, currentForm, requiredForms);
    };
    $scope.currentForm = 'generalForm';
    $scope.processing = NewEventProcessing.get('tasks');
    
    onSuccess = function () {
        $scope.close();
        Notifications.add('success', 'TASK_CREATED');
    };

    onFailure = function () {
        $scope.close();
        Notifications.add('error', 'TASK_NOT_CREATED', 'global', -1);
    };

    $scope.submit = function () {
        var eventIds = [], payload;
        angular.forEach($scope.rows, function (row) {
            eventIds.push(row.id);
        });
        payload = {
            workflows: $scope.processing.ud.workflow.id,
            configuration: $scope.processing.ud.workflow.selection.configuration,
            eventIds: eventIds
        };
        TaskResource.save(payload, onSuccess, onFailure);
    };
}]);
