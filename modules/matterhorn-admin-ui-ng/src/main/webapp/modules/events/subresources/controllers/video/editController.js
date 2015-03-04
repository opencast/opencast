/**
* Copyright 2009-2013 The Regents of the University of California
* Licensed under the Educational Community License, Version 2.0
* (the "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.osedu.org/licenses/ECL-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an "AS IS"
* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*
*/
'use strict';

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('VideoEditCtrl', ['$scope', 'PlayerAdapter', 'VideoService',
    function ($scope, PlayerAdapter, VideoService) {

        $scope.split = function () {
            var segment = VideoService.getCurrentSegment($scope.player, $scope.video),
                position = Math.floor($scope.player.adapter.getCurrentTime() * 1000),
                newSegment = angular.copy(segment);

            // Shrink original segment
            segment.end = position;

            // Add additional segment containing the second half of the
            // original segment.
            newSegment.start = position;

            // Deselect the previous segment as the cursor is at the start
            // of the new one.
            delete segment.selected;

            // Insert new segment
            $scope.video.segments.push(newSegment);

            // Sort array by start attribute
            $scope.video.segments.sort(function (a, b) {
                return a.start - b.start;
            });
        };

        $scope.clearSegments = function () {
            $scope.video.segments.splice(1, $scope.video.segments.length - 1);
            $scope.video.segments[0].end = $scope.video.duration;
        };

        $scope.cut = function () {
            angular.forEach($scope.video.segments, function (segment) {
                if (segment.selected) {
                    segment.deleted = !segment.deleted;
                }
            });
        };

        $scope.replay = function () {
            var segment = VideoService.getCurrentSegment($scope.player, $scope.video);
            segment.replay = true;
            $scope.player.adapter.setCurrentTime(segment.start/1000);
            if ($scope.player.adapter.getStatus() !== PlayerAdapter.STATUS.PLAYING) {
                $scope.player.adapter.play();
            }
        };
    }
]);
