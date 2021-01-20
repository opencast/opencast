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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('SeriesCtrl', ['$scope', 'Table', 'SeriesResource', 'ResourcesFilterResource', 'Notifications',
  function ($scope, Table, SeriesResource, ResourcesFilterResource, Notifications) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        template: 'modules/events/partials/seriesTitleCell.html',
        name:  'title',
        label: 'EVENTS.SERIES.TABLE.TITLE',
        sortable: true
      }, {
        template: 'modules/events/partials/seriesCreatorsCell.html',
        name:  'creator',
        label: 'EVENTS.SERIES.TABLE.ORGANIZERS',
        sortable: true
      }, {
        template: 'modules/events/partials/seriesContributorsCell.html',
        name:  'contributors',
        label: 'EVENTS.SERIES.TABLE.CONTRIBUTORS',
        sortable: true
      }, {
        name:  'createdDateTime',
        label: 'EVENTS.SERIES.TABLE.CREATED',
        sortable: true
      }, {
        template: 'modules/events/partials/seriesActionsCell.html',
        label:    'EVENTS.SERIES.TABLE.ACTION'
      }],
      caption:    'EVENTS.SERIES.TABLE.CAPTION',
      resource: 'series',
      category: 'events',
      apiService: SeriesResource,
      multiSelect: true
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.delete = function (row) {
      SeriesResource.delete({id: row.id}, function () {
        Table.fetch();
        Notifications.add('success', 'SERIES_DELETED');
      }, function () {
        Notifications.add('error', 'SERIES_NOT_DELETED');
      });
    };
  }
]);
