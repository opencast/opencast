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
.controller('EventsCtrl', ['$scope', 'Table', 'EventsResource', 'ResourcesFilterResource',
    function ($scope, Table, EventsResource, ResourcesFilterResource) {
        // Configure the table service
        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'title',
                label: 'EVENTS.EVENTS.TABLE.TITLE'
            }, {
                name:  'presenter',
                label: 'EVENTS.EVENTS.TABLE.CREATORS'
            }, {
                name: 'series_name',
                label: 'EVENTS.EVENTS.TABLE.SERIES'
            }, {
                name:  'date',
                label: 'EVENTS.EVENTS.TABLE.DATE'
            }, {
                name:  'start_date',
                label: 'EVENTS.EVENTS.TABLE.START'
            }, {
                name:  'end_date',
                label: 'EVENTS.EVENTS.TABLE.STOP'
            }, {
                name:  'location',
                label: 'EVENTS.EVENTS.TABLE.LOCATION'
            }, {
                name: 'workflow_state',
                label: 'EVENTS.EVENTS.TABLE.WORKFLOW_STATE'
            }, {
                template: 'modules/events/partials/eventActionsCell.html',
                label:    'EVENTS.EVENTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'EVENTS.EVENTS.TABLE.CAPTION',
            resource:   'events',
            category:   'events',
            apiService: EventsResource,
            multiSelect: true
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            row.$delete();
        };
    }
]);
