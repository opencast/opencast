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
.controller('LocationblacklistCtrl', ['$scope', 'NewLocationblacklistStates', 'LocationBlacklistsResource', 'Notifications', 'Modal', 'Table',
        function ($scope, NewLocationblacklistStates, LocationBlacklistsResource, Notifications, Modal, Table) {
    $scope.states = NewLocationblacklistStates.get();
    NewLocationblacklistStates.reset();

    // Populate user data if the blacklist is being edited
    if ($scope.action === 'edit') {
        LocationBlacklistsResource.get({ id: Modal.$scope.resourceId }, function (blacklist) {
            // Populate items step
            $scope.states[0].stateController.ud.items = [{
                id: blacklist.resourceId,
                name: blacklist.resourceName
            }];

            // Populate dates step
            var fromDateTime = new Date(blacklist.date_from_raw),
                toDateTime   = new Date(blacklist.date_to_raw);
            $scope.states[1].stateController.ud.fromDate =
                fromDateTime.toISOString().split('T')[0];
            $scope.states[1].stateController.ud.fromTime =
                fromDateTime.toISOString().split('T')[1].slice(0,5);
            $scope.states[1].stateController.ud.toDate =
                toDateTime.toISOString().split('T')[0];
            $scope.states[1].stateController.ud.toTime =
                toDateTime.toISOString().split('T')[1].slice(0,5);

            // Populate reason step
            $scope.states[2].stateController.ud.reason  = blacklist.reason;
            $scope.states[2].stateController.ud.comment = blacklist.comment;
        });
    }

    $scope.submit = function () {
        var userdata = {};
        angular.forEach($scope.states, function (state) {
            userdata[state.name] = state.stateController.ud;
        });

        if ($scope.action === 'edit') {
            LocationBlacklistsResource.update({ id: Modal.$scope.resourceId }, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'LOCATION_BLACKLIST_SAVED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'LOCATION_BLACKLIST_NOT_SAVED', 'blacklist-form');
            });
        } else {
            LocationBlacklistsResource.save({}, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'LOCATION_BLACKLIST_CREATED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'LOCATION_BLACKLIST_NOT_CREATED', 'blacklist-form');
            });
        }
    };
}]);
