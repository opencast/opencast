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
.controller('ToolsCtrl', ['$scope', '$interval', '$route', '$location', '$window', 'ToolsResource', 'Notifications', 'EventHelperService',
    function ($scope, $interval, $route, $location, $window, ToolsResource, Notifications, EventHelperService) {

        $scope.event    = EventHelperService;
        $scope.resource = $route.current.params.resource;
        $scope.tab      = $route.current.params.tab;
        if ($scope.tab === "editor") {
          $scope.area   = "segments";
        } else if ($scope.tab === "playback") {
          $scope.area   = "metadata";
        }
        $scope.id       = $route.current.params.itemId;

        $scope.event.eventId = $scope.id;

        $scope.unsavedChanges = false;

        $scope.navigateTo = function (path) {
            ToolsResource.release({id: $scope.id, tool: 'lock'});
            $location.path(path).replace();
        };

        $scope.setChanges = function(changed) {
            $scope.unsavedChanges = changed;
        };

        $scope.openTab = function (tab) {
            $scope.tab = tab;
            if ($scope.tab === "editor") {
              $scope.area   = "segments";
            } else if ($scope.tab === "playback") {
              $scope.area   = "metadata";
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
        $scope.video  = ToolsResource.get({ id: $scope.id, tool: 'editor' }, function () {
          if ($scope.video.status === 'locked' ) {
            $scope.video.user = $scope.video.lockingUser.name + " (" + $scope.video.lockingUser.email + ")";
            if ($scope.video.editWhenLocked) {
              Notifications.addWithParams('warning', 'VIDEO_CURRENTLY_EDITED_BY', {user: $scope.video.user}, 'global', -1);
            } else {
              Notifications.addWithParams('error', 'VIDEO_EDIT_LOCKED_MINS', {user: $scope.video.user, minutes: $scope.video.lockedTime}, 'global', 10000);
              $location.url('/events/' + $scope.resource);
            }
          }
          if ($scope.video.status === 'no preview' ) {
            Notifications.add('error', 'VIDEO_LOCKED_NO_PREVIEW', 'global', 10000);
            $location.url('/events/' + $scope.resource);
          }
        });

        $scope.autosave = function () {
            $scope.video.autosave = true;
            $scope.video.$save({id: $scope.id, tool: $scope.tab}, function () {
                Notifications.add('success', 'VIDEO_CUT_SAVED_AUTO');
            });
        };
        $scope.stopTime = $interval($scope.autosave, 1740000);

        $scope.submitButton = false;
        $scope.release = function() {
          ToolsResource.release({id: $scope.id, tool: 'lock'});
        };
        $scope.submit = function () {
            $scope.video.autosave = false;
            $scope.submitButton = true;
            $scope.video.$save({ id: $scope.id, tool: $scope.tab }, function () {
                $scope.submitButton = false;
                if ($scope.video.workflow) {
                    Notifications.add('success', 'VIDEO_CUT_PROCESSING');
                    $location.url('/events/' + $scope.resource);
                } else {
                    $scope.video.autosave = true;
                    Notifications.add('success', 'VIDEO_CUT_SAVED');
                }
                $scope.unsavedChanges = false;
            }, function () {
                $scope.submitButton = false;
                Notifications.add('error', 'VIDEO_CUT_NOT_SAVED', 'video-tools');
            });
        };
        $window.onbeforeunload = function () {
          // Have to delete lock with synch call
          var request = new XMLHttpRequest();
          request.open('DELETE', 'tools/' + $scope.id + '/lock.json', false);
          request.send(null);
          if (request.status === 200) {
            console.log('lock freed');
          }
        };
    }
]);
