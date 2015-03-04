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

// Controller for all recordings screens.
angular.module('adminNg.controllers')
.controller('RecordingsCtrl', ['$scope', 'Table', 'CaptureAgentsResource', 'ResourcesFilterResource',
    function ($scope, Table, CaptureAgentsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'status',
                label: 'RECORDINGS.RECORDINGS.TABLE.STATUS'
            }, {
                name:  'name',
                label: 'RECORDINGS.RECORDINGS.TABLE.NAME'
            }, {
                name:  'updated',
                label: 'RECORDINGS.RECORDINGS.TABLE.UPDATED'
            }, {
                name:  'blacklist_from',
                label: 'USERS.USERS.TABLE.BLACKLIST_FROM'
            }, {
                name:  'blacklist_to',
                label: 'USERS.USERS.TABLE.BLACKLIST_TO'
            //}, {
            //    template: 'modules/recordings/partials/recordingActionsCell.html',
            //    label:    'RECORDINGS.RECORDINGS.TABLE.ACTION',
            //    dontSort: true
            }],
            caption:    'RECORDINGS.RECORDINGS.TABLE.CAPTION',
            resource:   'recordings',
            category:   'recordings',
            apiService: CaptureAgentsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
    }
]);
