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

// Controller for retracting a single event that has been published
angular.module('adminNg.controllers')
.controller('RetractEventCtrl', ['$scope', 'NewEventProcessing', 'TaskResource', 'Notifications',
function ($scope, NewEventProcessing, TaskResource, Notifications) {
    var onSuccess, onFailure;

    $scope.currentForm = 'generalForm';
    $scope.processing = NewEventProcessing.get('delete-event');
    $scope.$valid = false;
    $scope.valid = function () {
        $scope.$valid = angular.isDefined($scope.processing.ud.workflow.id);
    };

    $scope.getSubmitButtonState = function() {
      return ($scope.$valid && !$scope.submitButton) ? 'active' : 'disabled';
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
        var finalConfiguration = {};
        var eventId = $scope.$parent.resourceId;
        finalConfiguration[eventId] = $scope.processing.ud.workflow.selection.configuration;
        var payload = {
            workflow: $scope.processing.ud.workflow.id,
            configuration: finalConfiguration
        };
        TaskResource.save(payload, onSuccess, onFailure);
    };
}]);
