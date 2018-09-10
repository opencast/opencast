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
.controller('VideoEditCtrl', ['$scope', '$translate', 'PlayerAdapter', 'VideoService', 'HotkeysService', 'Notifications',
    function ($scope, $translate, PlayerAdapter, VideoService, HotkeysService, Notifications) {

        var NOTIFICATION_CONTEXT = 'video-editor-event-access';
        var notificationId = 0;

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

            // Notify segmentsDirective about changed segment times
            $scope.$root.$broadcast("segmentTimesUpdated");
        };

        $scope.clearSelectedSegment = function () {

            angular.forEach($scope.video.segments, function (segment) {
                if (segment.selected) {

                    var index = $scope.video.segments.indexOf(segment);

                    if ($scope.video.segments[index + 1]) {
                        $scope.video.segments[index + 1].start = segment.start;
                        $scope.video.segments.splice(index, 1);
                    } else if ($scope.video.segments[index - 1]) {
                        $scope.video.segments[index - 1].end = segment.end;
                        $scope.video.segments.splice(index, 1);
                    }
                }
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

        $scope.replayEndOfSegment = function () {
            var segment = VideoService.getCurrentSegment($scope.player, $scope.video);
            segment.replay = true;
            $scope.player.adapter.setCurrentTime((segment.end/1000) - 2);
            if ($scope.player.adapter.getStatus() !== PlayerAdapter.STATUS.PLAYING) {
                $scope.player.adapter.play();
            }
        };

        $scope.replayPreRoll = function () {
            var segment = VideoService.getPreviousActiveSegment($scope.player, $scope.video);
            var currentSegment = VideoService.getCurrentSegment($scope.player, $scope.video);
            currentSegment.replay = true;
            segment.replay = true;
            $scope.player.adapter.setCurrentTime((segment.end/1000) - 2);
            if ($scope.player.adapter.getStatus() !== PlayerAdapter.STATUS.PLAYING) {
                $scope.player.adapter.play();
            }
        };

        HotkeysService.activateHotkey($scope, 'editor.split_at_current_time', function (event) {
            event.preventDefault();
            $scope.split();
        });

        HotkeysService.activateHotkey($scope, 'editor.cut_selected_segment', function (event) {
            event.preventDefault();
            $scope.cut();
        });

        HotkeysService.activateHotkey($scope, 'editor.play_current_segment', function (event) {
            event.preventDefault();
            $scope.replay();
        });

        HotkeysService.activateHotkey($scope, 'editor.clear_list', function (event) {
            event.preventDefault();
            $scope.clearSegments();
        });

        HotkeysService.activateHotkey($scope, 'editor.play_current_segment_with_pre-roll', function (event) {
            event.preventDefault();
            $scope.replayPreRoll();
        });

        HotkeysService.activateHotkey($scope, 'editor.play_ending_of_current_segment', function (event) {
            event.preventDefault();
            $scope.replayEndOfSegment();
        });

        $scope.$on('ACTIVE_TRANSACTION', function () {
            if (!$scope.activeTransaction) {
                $scope.activeTransaction = true;
                notificationId = Notifications.add('warning', 'ACTIVE_TRANSACTION', NOTIFICATION_CONTEXT, -1);
            }
        });

        $scope.$on('NO_ACTIVE_TRANSACTION', function () {
            if ($scope.activeTransaction) {
                $scope.activeTransaction = false;
                if (notificationId) {
                    Notifications.remove(notificationId, NOTIFICATION_CONTEXT);
                    notificationId = 0;
                }
            }
        });

        // This shows a confirmation dialog when the user leaves the editor while he has unsaved changes
        $scope.onUnload = function (event) {
            if (!$scope.unsavedChanges) return undefined;
            var answer = confirm(window.unloadConfirmMsg);
            if (!answer) {
               event.preventDefault();
            }
            event.returnValue = window.unloadConfirmMsg;
            return window.unloadConfirmMsg;
        };

        // register listeners to show confirmation dialog when user leaves editor with unsaved changes
        window.addEventListener('beforeunload', $scope.onUnload);
        $scope.$on('$locationChangeStart', $scope.onUnload);
        $scope.$on('$destroy', function () {
            window.removeEventListener('beforeunload', $scope.onUnload);
        });

        $translate('VIDEO_TOOL.WARNING_UNSAVED').then(function (translation) {
            window.unloadConfirmMsg = translation;
        });
    }
]);
