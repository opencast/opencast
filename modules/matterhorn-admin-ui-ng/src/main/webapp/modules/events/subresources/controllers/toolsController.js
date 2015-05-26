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
.controller('ToolsCtrl', ['$scope', '$route', '$location', '$window', 'ToolsResource', 'Notifications',
    function ($scope, $route, $location, $window, ToolsResource, Notifications) {

        $scope.navigateTo = function (path) {
            // FIMXE When changing tabs, video playback breaks. Using playback
            // controls after a tab change works for audio, but there is no
            // video. Perhaps it results in an orphaned <video> element.
            //
            // The following hack prevents a racing condition between setting
            // the path and a reload by preventing the path change from
            // triggering a render sync before the reload takes place.
            var lastRoute, off;
            lastRoute = $route.current;
            off = $scope.$on('$locationChangeSuccess', function () {
                $route.current = lastRoute;
                off();
                $window.location.reload();
            });
            $location.path(path).replace();
        };

        $scope.resource = $route.current.params.resource;
        $scope.tab      = $route.current.params.tab;
        $scope.id       = $route.current.params.itemId;

        $scope.openTab = function (tab) {
            $scope.navigateTo('events/' + $scope.resource + '/' +
                $scope.id + '/tools/' + tab);
        };

        // TODO Move the following to a VideoCtrl
        $scope.player = {};
        $scope.video  = ToolsResource.get({ id: $scope.id, tool: 'editor' });

        $scope.submit = function () {
            $scope.video.$save({ id: $scope.id, tool: $scope.tab }, function () {
                if ($scope.video.workflow) {
                    Notifications.add('success', 'VIDEO_CUT_PROCESSING');
                } else {
                    Notifications.add('success', 'VIDEO_CUT_SAVED');
                }
                $location.url('/events/' + $scope.resource);
            }, function () {
                Notifications.add('error', 'VIDEO_CUT_NOT_SAVED', 'video-tools');
            });
        };
    }
]);
