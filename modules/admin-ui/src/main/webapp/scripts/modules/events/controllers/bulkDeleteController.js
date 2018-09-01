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
    'BulkDeleteResource', 'NewEventProcessing', 'TaskResource', 'decorateWithTableRowSelection',
    'SeriesHasEventsResource', 'SeriesConfigurationResource',
        function ($scope, Modal, FormNavigatorService, Table, Notifications, BulkDeleteResource, NewEventProcessing,
                  TaskResource, decorateWithTableRowSelection, SeriesHasEventsResource, SeriesConfigurationResource) {

    var hasPublishedElements = function (currentEvent) {
        var publicationCount = 0;
        angular.forEach(currentEvent.publications, function() {
            publicationCount++;
        });

        return publicationCount > 0 && currentEvent.publications[0].id != "engage-live";
    },
    tableByPublicationStatus = function(isPublished) {
        var result = {
            allSelected: true,
            rows: (function () {
                var result = [];
                angular.forEach($scope.rows, function (row){
                    if (hasPublishedElements(row) === isPublished && row.selected) {
                        result.push(row);
                    }
                });
                return result;
            })()
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
    $scope.currentForm = 'deleteForm'; // By default start on the delete form
    $scope.processing = NewEventProcessing.get('delete-event');
    $scope.published = tableByPublicationStatus(true);
    $scope.unpublished = tableByPublicationStatus(false);
    $scope.navigateTo = function (targetForm, currentForm, requiredForms) {
        $scope.currentForm = FormNavigatorService.navigateTo(targetForm, currentForm, requiredForms);
    };

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
        var selectedCount;
        if (Table.resource.indexOf('series') >= 0) {
            selectedCount = 0;
            angular.forEach($scope.rows, function (row) {
                if (row.selected) {
                    selectedCount++;
                }
            });
            return selectedCount > 0;
        } else {
            selectedCount = countSelected($scope.unpublished.rows) + countSelected($scope.published.rows);
            if (countSelected($scope.published.rows) > 0) {
                return angular.isDefined($scope.processing.ud.workflow.id) && selectedCount > 0;
            } else {
                return selectedCount > 0;
            }
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
                angular.forEach(getSelected($scope.unpublished.rows), function (row) {
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
            if (Table.resource.indexOf('series') < 0 && countSelected($scope.published.rows) > 0) {
                var retractEventIds = [], payload;
                angular.forEach($scope.getPublishedEvents(), function (row) {
                    retractEventIds.push(row.id);
                });
                if (retractEventIds.length > 0) {
                    resetSubmitButton = false;
                    var configuration = $scope.processing.ud.workflow.selection.configuration;
                    var finalConfiguration = {};
                    // We have to duplicate the configuration for each event, since the "bulk start task"
                    // event wants per-event configuration since MH-12826.
                    for (var i = 0; i < retractEventIds.length; i++) {
                        finalConfiguration[retractEventIds[i]] = configuration;
                    }
                    payload = {
                        workflow: $scope.processing.ud.workflow.id,
                        configuration: finalConfiguration
                    };
                    TaskResource.save(payload, $scope.onSuccess, $scope.onFailure);
                }
            }
            if (resetSubmitButton) {
                // in this case, no callback would ever set submitButton to false again
                $scope.submitButton = false;
            }
        }

    };

    $scope.onSuccess = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('success', 'TASK_CREATED');
    };

    $scope.onFailure = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('error', 'TASK_NOT_CREATED', 'global', -1);
    };

    $scope.getPublishedEvents = function () {
        return getSelected($scope.published.rows);
    };

    $scope.getUnpublishedEvents = function () {
        return getSelected($scope.unpublished.rows);
    };

    $scope.toggleAllUnpublishedEvents = function () {
        angular.forEach($scope.rows, function (row) {
            if (!$scope.hasPublishedElements(row)) {
                row.selected = $scope.events.unpublished.selected;
            }
        });
    };

    if (Table.resource.indexOf('series') < 0) {
        $scope.events = {};
        $scope.events.published = {};
        $scope.events.published.has = $scope.published.rows.length > 0;
        $scope.events.published.selected = true;
        $scope.events.unpublished = {};
        $scope.events.unpublished.has = $scope.unpublished.rows.length > 0;
        $scope.events.unpublished.selected = true;
    }
    else {
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
