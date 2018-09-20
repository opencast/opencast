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
.controller('ServersCtrl', ['$scope', 'Table', 'ServersResource', 'ServiceResource', 'ResourcesFilterResource',
  function ($scope, Table, ServersResource, ServiceResource, ResourcesFilterResource) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:     'online',
        template: 'modules/systems/partials/serverStatusCell.html',
        label:    'SYSTEMS.SERVERS.TABLE.STATUS'
      }, {
        name:  'hostname',
        label: 'SYSTEMS.SERVERS.TABLE.HOST_NAME'
      }, {
        name:  'cores',
        label: 'SYSTEMS.SERVERS.TABLE.CORES'
      }, {
        name:  'completed',
        label: 'SYSTEMS.SERVERS.TABLE.COMPLETED'
      }, {
        name:  'running',
        label: 'SYSTEMS.SERVERS.TABLE.RUNNING'
      }, {
        name:  'queued',
        label: 'SYSTEMS.SERVERS.TABLE.QUEUED'
      }, {
        name:  'meanRunTime',
        label: 'SYSTEMS.SERVERS.TABLE.MEAN_RUN_TIME'
      }, {
        name:  'meanQueueTime',
        label: 'SYSTEMS.SERVERS.TABLE.MEAN_QUEUE_TIME'
      }, {
        name:     'maintenance',
        template: 'modules/systems/partials/serverMaintenanceCell.html',
        label:    'SYSTEMS.SERVERS.TABLE.MAINTENANCE'
        //}, {
        //    template: 'modules/systems/partials/serverActionsCell.html',
        //    label:    'SYSTEMS.SERVERS.TABLE.ACTION',
        //    dontSort: true
      }],
      caption:    'SYSTEMS.SERVERS.TABLE.CAPTION',
      resource:   'servers',
      category:   'systems',
      apiService: ServersResource
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.setMaintenanceMode = function (host, maintenance) {
      ServiceResource.setMaintenanceMode({
        host: host,
        maintenance: maintenance
      }, function () {
        $scope.table.fetch();
      });
    };
  }
]);
