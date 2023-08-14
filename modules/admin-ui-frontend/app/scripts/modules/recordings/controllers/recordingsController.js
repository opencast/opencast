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

// Controller for all recordings screens.
angular.module('adminNg.controllers')
.controller('RecordingsCtrl', ['$scope', 'Table', 'CaptureAgentsResource', 'ResourcesFilterResource', 'Notifications',
  'Modal', function ($scope, Table, CaptureAgentsResource, ResourcesFilterResource, Notifications, Modal) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'status',
        template: 'modules/recordings/partials/recordingStatusCell.html',
        label: 'RECORDINGS.RECORDINGS.TABLE.STATUS',
        translate: true,
        sortable: true
      }, {
        template: 'modules/recordings/partials/recordingsNameCell.html',
        name:  'name',
        label: 'RECORDINGS.RECORDINGS.TABLE.NAME',
        sortable: true
      }, {
        name:  'updated',
        label: 'RECORDINGS.RECORDINGS.TABLE.UPDATED',
        sortable: true
      }, {
        template: 'modules/recordings/partials/recordingActionsCell.html',
        label:    'RECORDINGS.RECORDINGS.TABLE.ACTION'
      }],
      caption:    'RECORDINGS.RECORDINGS.TABLE.CAPTION',
      resource:   'recordings',
      category:   'recordings',
      apiService: CaptureAgentsResource
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.delete = function (row) {
      CaptureAgentsResource.delete({target: row.name}, function () {
        Table.fetch();
        Modal.$scope.close();
        Notifications.add('success', 'LOCATION_DELETED');
      }, function (error) {
        if (error.status === 401) {
          Notifications.add('error', 'LOCATION_NOT_DELETED_NOT_AUTHORIZED');
        } else {
          Notifications.add('error', 'LOCATION_NOT_DELETED');
        }
      });
    };
  }
]);
