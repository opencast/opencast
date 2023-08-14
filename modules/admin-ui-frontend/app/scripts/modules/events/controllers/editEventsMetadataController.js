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

// Controller for the "edit event metadata" modal
angular.module('adminNg.controllers')
  .controller('EditEventsMetadataCtrl', ['$scope', 'Table', 'EventsMetadataResource', '$timeout',
    'decorateWithTableRowSelection', 'Notifications',
    function ($scope, Table, EventsMetadataResource, $timeout, decorateWithTableRowSelection, Notifications) {

      $scope.allSelected = false;
      $scope.noEventsToEdit = false;
      $scope.submitting = false;
      $scope.currentForm = '';

      decorateWithTableRowSelection($scope);
      $scope.selectedRows = Table.copySelected();

      // get metadata for selected events
      $scope.loadMetadata = function (selectedIds) {
        EventsMetadataResource.get(selectedIds).$promise.then(function (data) {
          data = data.results;
          $scope.rows = data.metadata;
          $scope.eventIdsToUpdate = data.merged;

          $scope.eventsToUpdate = $scope.selectedRows.filter(function callback(row) {
            return data.merged.indexOf(row.id) >= 0;});

          $scope.metadataUpdatedFunctions = Array($scope.rows.length);
          $scope.oldValues = Array($scope.rows.length);
          $scope.requiredMetadata = {};

          // hook up tabindex and init some data
          var tabindex = 2;
          for (var i = 0; i < $scope.rows.length; i++) {
            var row = $scope.rows[i];
            row.tabindex = tabindex++;
            row.selected = false;
            row.saved = false; // this should really be called updated, but that's the way the editables call it

            if (row.required) {
              $scope.updateRequiredMetadata(row.id, row.value);
            }

            if (row.value instanceof Array) {
              $scope.oldValues[i] = row.value.slice(0);
            } else {
              $scope.oldValues[i] = row.value;
            }

            // wrap this so it works with editable
            $scope.metadataUpdatedFunctions[i] = function(i) {
              return function() {
                $scope.metadataUpdated(i);
              };
            }(i);
          }
          // any errors?
          if (data.runningWorkflow.length > 0 || data.notFound.length > 0) {
            $scope.setRequestErrors(data);
          } else {
            $scope.setForm('editMetadataForm');
          }
        }, function(response) {
          $scope.noEventsToEdit = true;
          if (response.data.results) {
            $scope.setRequestErrors(response.data.results);
          } else {
            $scope.currentForm = 'fatalErrorForm';
            $scope.fatalError = response.statusText;
          }
        });
      };

      $scope.updateRequiredMetadata = function(id, value) {
        if (angular.isDefined(value) && value != null && value.length > 0) {
          $scope.requiredMetadata[id] = true;
        } else {
          $scope.requiredMetadata[id] = false;
        }
      };

      $scope.setRequestErrors = function(data) {
        for (var i = 0; i < $scope.selectedRows.length; i++) {
          var row = $scope.selectedRows[i];
          var eventId = row.id;
          if (data.notFound && data.notFound.indexOf(eventId) >= 0) {
            row.error = 'BULK_ACTIONS.EDIT_EVENTS_METADATA.REQUEST_ERRORS.NOT_FOUND';
          }
          else if (data.runningWorkflow && data.runningWorkflow.indexOf(eventId) >= 0) {
            row.error = 'BULK_ACTIONS.EDIT_EVENTS_METADATA.REQUEST_ERRORS.RUNNING_WORKFLOW';
          }
        }
        $scope.setForm('requestErrorsForm');
        $scope.hasRequestErrors = true;
      };

      $scope.setForm = function (form) {
        $scope.currentForm = form;
      };

      $scope.isRequirementFulfilled = function(id) {
        return $scope.requiredMetadata[id];
      };

      $scope.metadataFieldValid = function ($index) {
        var row = $scope.rows[$index];
        return (row.saved || row.differentValues) // will result in new value for at least one event
            && (!row.required || $scope.isRequirementFulfilled(row.id)); // field is valid
      };

      $scope.metadataValid = function() {
        return (!$scope.submitting && $scope.hasAnySelected());
      };

      //react to any updated metadata
      $scope.metadataUpdated = function($index) {
        var row = $scope.rows[$index];
        var oldValue = $scope.oldValues[$index];

        $scope.updateRequiredMetadata(row.id, row.value);

        if ((row.value || oldValue) && row.value !== oldValue) {
          row.saved = true;
        } else {
          row.saved = false;
        }

        row.selected = $scope.metadataFieldValid($index);
      };

      $scope.submit = function () {
        $scope.submitting = true;

        // only update selected metadata
        var updatedMetadata = $scope.rows.filter(function callback(row) {
          return row.selected;
        });

        var updateData = {eventIds: $scope.eventIdsToUpdate, metadata: updatedMetadata};

        EventsMetadataResource.save(updateData).$promise.then(function () {
          $scope.submitting = false;
          $scope.deselectAndClose();
          Notifications.add('success', 'BULK_METADATA_UPDATE.ALL_EVENTS_UPDATED');
        },
        function (response) {
          var data = response.data.errors;

          // no data? unexpected error
          if (!angular.isDefined(data) || data == null || data.length == 0) {
            $scope.close();
            Notifications.add('error', 'BULK_METADATA_UPDATE.UNEXPECTED_ERROR');
          }
          else {
            $scope.someUpdatedSuccessfully = false;

            for (var i = 0; i < $scope.eventsToUpdate.length; i++) {
              var row = $scope.eventsToUpdate[i];
              var eventId = row.id;

              if (data.notFound && data.notFound.indexOf(eventId) >= 0) {
                row.error = 'BULK_ACTIONS.EDIT_EVENTS_METADATA.REQUEST_ERRORS.NOT_FOUND';
              }
              else if (data.updateFailures && data.updateFailures.indexOf(eventId) >= 0) {
                row.error = 'BULK_ACTIONS.EDIT_EVENTS_METADATA.UPDATE_FAILURES.UPDATE_FAILED';
              }
              else {
                $scope.someUpdatedSuccessfully = true;
              }
            }
            $scope.setForm('updateErrorsForm');
          }
          $scope.submitting = false;
        });
      };

      $scope.closeUpdateErrorsForm = function() {
        if ($scope.someUpdatedSuccessfully) {
          Notifications.add('warning', 'BULK_METADATA_UPDATE.SOME_EVENTS_NOT_UPDATED');
        }
        else {
          Notifications.add('error', 'BULK_METADATA_UPDATE.NO_EVENTS_UPDATED');
        }
        $scope.deselectAndClose();
      };

      $scope.deselectAndClose = function() {
        Table.deselectAll();
        $scope.close();
      };

      var selectedIds = $scope.selectedRows.map(function callback(x) { return x.id;});
      $scope.loadMetadata(selectedIds);
    }
  ]);
