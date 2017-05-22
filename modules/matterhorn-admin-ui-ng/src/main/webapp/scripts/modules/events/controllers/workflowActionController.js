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

// Controller for workflow actions (pause or abort in case of errors)
angular.module('adminNg.controllers')
.controller('WorkflowActionCtrl', ['$scope', 'Table', 'Notifications', 'Modal', 'EventWorkflowActionResource',
    function ($scope, Table, Notifications, Modal, EventWorkflowActionResource) { 
        $scope.table = Table;
        
        $scope.workflowAction = function (action) {
        	EventWorkflowActionResource.save({id: Modal.$scope.resourceId, action: action}, function () {
                Table.fetch();
                Notifications.add('success', 'PROCESSING_ACTION_' + action);
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'PROCESSING_NOT_ACTION_' + action);
                Modal.$scope.close();
            });
        };
    }
]);
