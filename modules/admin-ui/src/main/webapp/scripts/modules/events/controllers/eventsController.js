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
.controller('EventsCtrl', ['$scope', 'Stats', 'Table', 'EventsResource', 'ResourcesFilterResource', 'ResourcesListResource', 'Notifications', 'ResourceModal', 'ConfirmationModal', 'EventHasSnapshotsResource', 'RelativeDatesService',
    function ($scope, Stats, Table, EventsResource, ResourcesFilterResource, ResourcesListResource, Notifications, ResourceModal, ConfirmationModal, EventHasSnapshotsResource, RelativeDatesService) {

        $scope.stats = Stats;

        var statsConfiguration = ResourcesListResource.queryRecursive({ resource: 'STATS' });
        statsConfiguration.$promise.then(function (configuration) {

             var statsConfigurationValues = configuration.map(function(element) {
                 return element.value;
             });

             $scope.stats.configure({
                 stats: statsConfigurationValues,
                 resource:   'events',
                 apiService: EventsResource
            });
        });

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'title',
                label: 'EVENTS.EVENTS.TABLE.TITLE'
            }, {
                name:  'presenter',
                label: 'EVENTS.EVENTS.TABLE.PRESENTERS'
            }, {
                template: 'modules/events/partials/eventsSeriesCell.html',
                name:  'series_name',
                label: 'EVENTS.EVENTS.TABLE.SERIES'
            }, {
                template: 'modules/events/partials/eventsTechnicalDateCell.html',
                name:  'technical_date',
                label: 'EVENTS.EVENTS.TABLE.DATE'
            }, {
                name:  'technical_start',
                label: 'EVENTS.EVENTS.TABLE.START'
            }, {
                name:  'technical_end',
                label: 'EVENTS.EVENTS.TABLE.STOP'
            }, {
                template: 'modules/events/partials/eventsLocationCell.html',
                name:  'location',
                label: 'EVENTS.EVENTS.TABLE.LOCATION'
            }, {
                name:  'published',
                label: 'EVENTS.EVENTS.TABLE.PUBLISHED',
                template: 'modules/events/partials/publishedCell.html',
                dontSort: true
            }, {
                template: 'modules/events/partials/eventsStatusCell.html',
                name:  'event_status',
                label: 'EVENTS.EVENTS.TABLE.SCHEDULING_STATUS'
            }, {
                template: 'modules/events/partials/eventActionsCell.html',
                label:    'EVENTS.EVENTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'EVENTS.EVENTS.TABLE.CAPTION',
            resource:   'events',
            category:   'events',
            apiService: EventsResource,
            multiSelect: true,
            postProcessRow: function (row) {
                angular.forEach(row.publications, function (publication, index) {
                    if (angular.isDefined($scope.publicationChannels[publication.id])) {
                        var record = JSON.parse($scope.publicationChannels[publication.id]);
                        publication.label = record.label ? record.label : publication.name;
                        publication.icon = record.icon;
                        publication.hide = record.hide;
                        publication.description = record.description;
                        publication.order = record.order ? record.order : 999 + index;
                    } else {
                        publication.label = publication.name;
                        publication.order = 999 + index;
                    }
                });
                row.checkedDelete = function() {
                  EventHasSnapshotsResource.get({id: row.id},function(o) {
                    if ((angular.isUndefined(row.publications) || row.publications.length <= 0 || (row.publications.length == 1 && row.publications[0].id == "engage-live") || !o.hasSnapshots) && !row.has_preview )
                          // Works, opens simple modal
                          ConfirmationModal.show('confirm-modal',Table.delete,row);
                      else
                          // works, opens retract
                          ResourceModal.show('retract-published-event-modal',row.id);
                  });
                }
            }
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
        $scope.publicationChannels = ResourcesListResource.get({ resource: 'PUBLICATION.CHANNELS' });

        $scope.table.dateToFilterValue = function(dateString) {

            var from = RelativeDatesService.relativeToAbsoluteDate(0, 'days', true);
            var to = RelativeDatesService.relativeToAbsoluteDate(0, 'days', false);
            return from.toISOString() + "/" + to.toISOString();
        };

        $scope.table.delete = function (row) {
            EventsResource.delete({id: row.id}, function () {
                Table.fetch();
                Notifications.add('success', 'EVENTS_DELETED');
            }, function (error) {
                if (error.status === 401) {
                  Notifications.add('error', 'EVENTS_NOT_DELETED_NOT_AUTHORIZED');
                } else {
                  Notifications.add('error', 'EVENTS_NOT_DELETED');
                }
            });
        };

        $scope.$on('$destroy', function() {
            // stop polling event stats on an inactive tab
            $scope.stats.refreshScheduler.cancel();
        });
    }
]);
