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
.controller('StartStateCtrl', ['$scope', function ($scope) {

    $scope.loading = true;

    var showSuccessfulResults = function () {

        $scope.someFailed = false;

        for (var i = 0; i < $scope.events.length; i++){
            var id = $scope.events[i].id;

            $scope.events[i].status = 'BULK_ACTIONS.START_STATE.STATE.START_SUCCESS';
        }

        $scope.loading = false;
    }

    var showSomeFailedResults = function (response) {

        var data = response.data;

        $scope.someFailed = true;

        for (var i = 0; i < $scope.events.length; i++){
            var id = $scope.events[i].id;

            if (data[id].successful) {
                $scope.events[i].status = 'BULK_ACTIONS.START_STATE.STATE.START_SUCCESS';
            }
            else {
                $scope.events[i].status = 'BULK_ACTIONS.START_STATE.STATE.START_FAIL';
                $scope.events[i].error = data[id].message;
            }
        }
        $scope.loading = false;
    }

    $scope.events = [];

    for (var i = 0; i < $scope.params.rows.length; i++) {

        var event = {
            id: $scope.params.rows[i].id,
            title: $scope.params.rows[i].title,
            series: $scope.params.rows[i].series_name,
            status: 'BULK_ACTIONS.START_STATE.STATE.START_PENDING'
        }
        $scope.events[i] = event;
    }

    $scope.someFailed = false;
    $scope.params.promise.then(showSuccessfulResults, showSomeFailedResults);
}]);
