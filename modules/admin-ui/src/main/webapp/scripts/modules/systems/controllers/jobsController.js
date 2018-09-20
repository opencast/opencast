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
angular.module('adminNg.controllers')
.controller('JobsCtrl', ['$scope', 'Table', 'JobsResource', 'ResourcesFilterResource',
  function ($scope, Table, JobsResource, ResourcesFilterResource) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'id',
        label: 'SYSTEMS.JOBS.TABLE.ID'
      }, {
        name:  'status',
        label: 'SYSTEMS.JOBS.TABLE.STATUS',
        translate: true
      }, {
        name:  'operation',
        label: 'SYSTEMS.JOBS.TABLE.OPERATION'
      }, {
        name:  'type',
        label: 'SYSTEMS.JOBS.TABLE.TYPE'
      }, {
        name:  'processingHost',
        label: 'SYSTEMS.JOBS.TABLE.HOST_NAME'
      }, {
        name:  'submitted',
        label: 'SYSTEMS.JOBS.TABLE.SUBMITTED'
      }, {
        name:  'started',
        label: 'SYSTEMS.JOBS.TABLE.STARTED'
      }, {
        name:  'creator',
        label: 'SYSTEMS.JOBS.TABLE.CREATOR'
        //}, {
        //    template: 'modules/systems/partials/jobActionsCell.html',
        //    label:    'SYSTEMS.JOBS.TABLE.ACTION',
        //    dontSort: true
      }],
      caption:    'SYSTEMS.JOBS.TABLE.CAPTION',
      resource:   'jobs',
      category:   'systems',
      apiService: JobsResource
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
  }
]);
