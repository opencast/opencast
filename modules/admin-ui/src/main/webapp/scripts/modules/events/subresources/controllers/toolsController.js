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
.controller('ToolsCtrl', ['$scope', '$route', '$location', 'Storage', '$window', 'ToolsResource', 'Notifications', 'EventHelperService',
    function ($scope, $route, $location, Storage, $window, ToolsResource, Notifications, EventHelperService) {

        $scope.navigateTo = function (path) {
            $location.path(path).replace();
        };

        $scope.event    = EventHelperService;
        $scope.resource = $route.current.params.resource;
        $scope.tab      = $route.current.params.tab;
        if ($scope.tab === "editor") {
          $scope.area   = "segments";
        }
        $scope.id       = $route.current.params.itemId;

        $scope.event.eventId = $scope.id;

        $scope.unsavedChanges = false;

        $scope.setChanges = function(changed) {
            $scope.unsavedChanges = changed;
        };

        $scope.openTab = function (tab) {
            $scope.tab = tab;
            if ($scope.tab === "editor") {
              $scope.area = "segments";
            }

            // This fixes a problem where video playback breaks after switching tabs. Changing the location seems
            // to be destructive to the <video> element working together with opencast's external controls.
            var lastRoute, off;
            lastRoute = $route.current;
            off = $scope.$on('$locationChangeSuccess', function () {
                $route.current = lastRoute;
                off();
            });

            $scope.navigateTo('/events/' + $scope.resource + '/' + $scope.id + '/tools/' + tab);
        };

        $scope.openArea = function (area) {
            $scope.area = area;
        };

        // TODO Move the following to a VideoCtrl
        $scope.player = {};
        $scope.video  = ToolsResource.get({ id: $scope.id, tool: 'editor' });

        $scope.submitButton = false;
        $scope.submit = function () {
            $scope.submitButton = true;
            $scope.video.$save({ id: $scope.id, tool: $scope.tab }, function () {
                $scope.submitButton = false;
                if ($scope.video.workflow) {
                    Notifications.add('success', 'VIDEO_CUT_PROCESSING');
                    $location.url('/events/' + $scope.resource);
                } else {
                    Notifications.add('success', 'VIDEO_CUT_SAVED');
                }
                $scope.unsavedChanges = false;
            }, function () {
                $scope.submitButton = false;
                Notifications.add('error', 'VIDEO_CUT_NOT_SAVED', 'video-tools');
            });
        };

        $scope.leave = function () {
            Storage.put('pagination', $scope.resource, 'resume', true);
            $location.url('/events/' + $scope.resource);
        };
    }
]);
