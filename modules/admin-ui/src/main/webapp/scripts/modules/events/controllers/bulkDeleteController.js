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
.controller('BulkDeleteCtrl', ['$scope', 'Modal', 'FormNavigatorService', 'Table', 'Notifications',
  'BulkDeleteResource', 'decorateWithTableRowSelection', 'SeriesHasEventsResource', 'SeriesConfigurationResource',
  function ($scope, Modal, FormNavigatorService, Table, Notifications, BulkDeleteResource,
    decorateWithTableRowSelection, SeriesHasEventsResource, SeriesConfigurationResource) {

    var createTable = function() {
          var result = {
            allSelected: true,
            rows: $scope.rows
          };
          decorateWithTableRowSelection(result);
          return result;
        },
        getSelected = function (rows) {
          var result = [];
          angular.forEach(rows, function(row) {
            if (row.selected) {
              result.push(row);
            }
          });
          return result;
        },
        countSelected = function (rows) {
          return getSelected(rows).length;
        };
    $scope.rows = Table.copySelected();
    $scope.allSelected = true; // by default, all records are selected
    $scope.table = createTable();

    var getSelectedSeriesIds = function () {
      var result = [];
      angular.forEach($scope.rows, function (row) {
        if (row.selected) {
          result.push(row.id);
        }
      });
      return result;
    };

    $scope.valid = function () {
      if (Table.resource.indexOf('series') >= 0) {
        return getSelectedSeriesIds().length > 0;
      } else {
        return countSelected($scope.table.rows) > 0;
      }
    };

    $scope.allowed = function () {
      var allowed = true;
      if (Table.resource.indexOf('series') >= 0 && !$scope.deleteSeriesWithEventsAllowed) {
        angular.forEach($scope.rows, function (row) {
          if (allowed && row.selected && row.hasEvents) {
            allowed = false;
          }
        });
      }
      return allowed;
    };

    $scope.submitButton = false;
    $scope.submit = function () {
      if ($scope.valid()) {
        $scope.submitButton = true;
        var resetSubmitButton = true,
            deleteIds = [],
            resource = Table.resource.indexOf('series') >= 0 ? 'series' : 'event',
            endpoint = Table.resource.indexOf('series') >= 0 ? 'deleteSeries' : 'deleteEvents',
            sourceNotification = resource === 'series' ? 'SERIES' : 'EVENTS';

        if (Table.resource.indexOf('series') >= 0) {
          deleteIds = getSelectedSeriesIds();
        } else {
          angular.forEach(getSelected($scope.table.rows), function (row) {
            deleteIds.push(row.id);
          });
        }
        if (deleteIds.length > 0) {
          resetSubmitButton = false;
          BulkDeleteResource.delete({}, {
            resource: resource,
            endpoint: endpoint,
            eventIds: deleteIds
          }, function () {
            $scope.submitButton = false;
            Table.deselectAll();
            Notifications.add('success', sourceNotification + '_DELETED');
            Modal.$scope.close();
          }, function () {
            $scope.submitButton = false;
            Notifications.add('error', sourceNotification + '_NOT_DELETED');
            Modal.$scope.close();
          });
        }
        if (resetSubmitButton) {
          // in this case, no callback would ever set submitButton to false again
          $scope.submitButton = false;
        }
      }

    };

    if (Table.resource.indexOf('series') >= 0) {
      SeriesConfigurationResource.get(function (data) {
        $scope.deleteSeriesWithEventsAllowed = data.deleteSeriesWithEventsAllowed;
      });
      angular.forEach($scope.rows, function(row) {
        SeriesHasEventsResource.get({id: row.id}, function (data) {
          row.hasEvents = data.hasEvents;
        });

      });
    }
  }]);
