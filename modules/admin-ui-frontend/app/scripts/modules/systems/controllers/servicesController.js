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
.controller('ServicesCtrl', ['$scope', 'Table', 'ServicesResource', 'ServiceResource', 'ResourcesFilterResource',
  function ($scope, Table, ServicesResource, ServiceResource, ResourcesFilterResource) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'status',
        label: 'SYSTEMS.SERVICES.TABLE.STATUS',
        translate: true,
        sortable: true
      }, {
        name:  'name',
        label: 'SYSTEMS.SERVICES.TABLE.NAME',
        sortable: true
      }, {
        name:  'hostname',
        label: 'SYSTEMS.SERVICES.TABLE.HOST_NAME',
        sortable: true
      }, {
        name:  'nodeName',
        label: 'SYSTEMS.SERVICES.TABLE.NODE_NAME',
        sortable: true
      }, {
        name:  'completed',
        label: 'SYSTEMS.SERVICES.TABLE.COMPLETED',
        sortable: true
      }, {
        name:  'running',
        label: 'SYSTEMS.SERVICES.TABLE.RUNNING',
        sortable: true
      }, {
        name:  'queued',
        label: 'SYSTEMS.SERVICES.TABLE.QUEUED',
        sortable: true
      }, {
        name:  'meanRunTime',
        label: 'SYSTEMS.SERVICES.TABLE.MEAN_RUN_TIME',
        sortable: true
      }, {
        name:  'meanQueueTime',
        label: 'SYSTEMS.SERVICES.TABLE.MEAN_QUEUE_TIME',
        sortable: true
      }, {
        template: 'modules/systems/partials/serviceActionsCell.html',
        label:    'SYSTEMS.SERVICES.TABLE.ACTION'
      }],
      caption:    'SYSTEMS.SERVICES.TABLE.CAPTION',
      resource:   'services',
      category:   'systems',
      apiService: ServicesResource,
      sorter:     {'sorter':{'services':{'status':{'name':'status','priority':0,'order':'DESC'}}}}
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.sanitize = function (hostname, serviceType) {
      ServiceResource.sanitize({
        host: hostname,
        serviceType: serviceType
      }, function () {
        $scope.table.fetch();
      });
    };
  }
]);
