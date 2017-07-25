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
.controller('EventsCtrl', ['$scope', 'Stats', 'Table', 'EventsResource', 'ResourcesFilterResource', 'ResourcesListResource', 'Notifications',
    function ($scope, Stats, Table, EventsResource, ResourcesFilterResource, ResourcesListResource, Notifications) {
        // Configure the table service
        $scope.stats = Stats;
        $scope.stats.configure({
            stats: [
            {filters: [{name: 'status',
                        filter: 'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.SCHEDULED'}],
             description: 'DASHBOARD.SCHEDULED'},
            {filters: [{name: 'status',
                        filter: 'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.RECORDING'}],
             description: 'DASHBOARD.RECORDING'},
            {filters: [{name: 'status',
                        filter:'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSING'}],
             description: 'DASHBOARD.RUNNING'},
            {filters: [{name: 'status',
                        filter:'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSING_FAILURE'}],
             description: 'DASHBOARD.FAILED'},
            {filters: [{name: 'comments',
                        filter:'FILTERS.EVENTS.COMMENTS.LABEL',
                        value: 'OPEN'},
                       {name: 'status',
                        filter: 'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSED'}],
             description: 'DASHBOARD.FINISHED_WITH_COMMENTS'},
            {filters: [{name: 'status',
                        filter:'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSED'}],
             description: 'DASHBOARD.FINISHED'}
            ],
            resource:   'events',
            apiService: EventsResource
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
                angular.forEach(row.publications, function (publication) {
                    if (angular.isDefined($scope.publicationChannelLabels[publication.id])) {
                        publication.label = $scope.publicationChannelLabels[publication.id];
                    } else {
                        publication.label = publication.name;
                    }
                });
            }
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
        $scope.publicationChannelLabels = ResourcesListResource.get({ resource: 'PUBLICATION.CHANNEL.LABELS' });

        $scope.table.delete = function (row) {
            EventsResource.delete({id: row.id}, function () {
                Table.fetch();
                Notifications.add('success', 'EVENTS_DELETED');
            }, function () {
                Notifications.add('error', 'EVENTS_NOT_DELETED');
            });
        };

        $scope.$on('$destroy', function() {
            // stop polling event stats on an inactive tab
            $scope.stats.refreshScheduler.cancel();
        });
    }
]);
