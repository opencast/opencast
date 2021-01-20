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
.controller('ThemesCtrl', ['$scope', 'Table', 'ThemesResource', 'ThemeResource', 'ResourcesFilterResource',
  'Notifications',
  function ($scope, Table, ThemesResource, ThemeResource, ResourcesFilterResource, Notifications) {

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'name',
        label: 'CONFIGURATION.THEMES.TABLE.NAME',
        sortable: true
      }, {
        name:  'description',
        label: 'CONFIGURATION.THEMES.TABLE.DESCRIPTION',
        sortable: true
      },  {
        name:  'creator',
        label: 'CONFIGURATION.THEMES.TABLE.CREATOR',
        sortable: true
      }, {
        name:  'creation_date',
        label: 'CONFIGURATION.THEMES.TABLE.CREATED',
        sortable: true
      },
      /*
             * Temporarily disabled
             *
             *{
             *    name:  'default',
             *    label: 'CONFIGURATION.THEMES.TABLE.DEFAULT'
             *},
             */
      {
        template: 'modules/configuration/partials/themesActionsCell.html',
        label:    'CONFIGURATION.THEMES.TABLE.ACTION'
      }],
      caption:    'CONFIGURATION.THEMES.TABLE.CAPTION',
      resource:   'themes',
      category:   'configuration',
      apiService: ThemesResource
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

    $scope.table.delete = function (row) {
      ThemeResource.delete({id: row.id}, function () {
        Table.fetch();
        Notifications.add('success', 'THEME_DELETED');
      }, function () {
        Notifications.add('error', 'THEME_NOT_DELETED', 'user-form');
      });
    };

  }
]);
