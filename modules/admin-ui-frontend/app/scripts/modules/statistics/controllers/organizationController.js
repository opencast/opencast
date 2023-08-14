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

angular.module('adminNg.controllers')
.controller('OrganizationCtrl', ['$scope', 'StatisticsResource', 'IdentityResource', '$route',
  'StatisticsReusable',
  function ($scope, StatisticsResource, IdentityResource, $route, StatisticsReusable) {
    $scope.resource = $route.current.params.resource;

    var me = IdentityResource.get();
    me.$promise.then(function (data) {
      $scope.resourceId = data.org.id;
      $scope.statReusable = StatisticsReusable.createReusableStatistics(
        'organization',
        $scope.resourceId,
        undefined);
    });

    $scope.statisticsCsvFileName = function (statsTitle) {
      var sanitizedStatsTitle = statsTitle.replace(/[^0-9a-z]/gi, '_').toLowerCase();
      return 'export_organization_' + $scope.resourceId + '_' + sanitizedStatsTitle + '.csv';
    };

    $scope.statReusable = null;
  }
]);
