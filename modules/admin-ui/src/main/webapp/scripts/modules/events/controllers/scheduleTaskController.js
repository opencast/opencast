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
.controller('ScheduleTaskCtrl', ['$scope', 'Table', 'NewEventProcessing', 'TaskResource',
    'Notifications', 'decorateWithTableRowSelection', 'Modal',
function ($scope, Table, NewEventProcessing, TaskResource, Notifications, decorateWithTableRowSelection, Modal) {
    $scope.rows = Table.copySelected();
    $scope.allSelected = true; // by default, all rows are selected
    $scope.test = false;
    $scope.currentForm = 'generalForm';
    $scope.processing = NewEventProcessing.get('tasks');

    $scope.valid = function () {
        return $scope.getSelectedIds().length > 0;
    };

    $scope.submitButton = false;
    $scope.submit = function () {

        $scope.submitButton = true;

        if ($scope.valid()) {

            var eventIds = $scope.getSelectedIds();

            var payload = {
                workflow: $scope.processing.ud.workflow.id,
                configuration: $scope.processing.getWorkflowConfig(),
                eventIds: eventIds
            };

            var answer = TaskResource.save(payload);

            var params = {
                rows: $scope.getSelected(),
                promise: answer.$promise
            }

            Modal.show('start-state-modal', params);

            $scope.submitButton = false;
            $scope.close();
            Table.deselectAll();
        }
    };
    decorateWithTableRowSelection($scope);
}]);
