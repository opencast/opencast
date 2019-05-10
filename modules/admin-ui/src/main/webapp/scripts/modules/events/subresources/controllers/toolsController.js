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
.controller('ToolsCtrl', ['$scope', '$route', '$location', 'Storage', '$window', 'ToolsResource', 'Notifications',
  'EventHelperService',
  function ($scope, $route, $location, Storage, $window, ToolsResource, Notifications, EventHelperService) {
    var thumbnailErrorMessageId = null;
    var trackErrorMessageId = null;

    var LOCAL_CONTEXT = 'video-tools';

    $scope.navigateTo = function (path) {
      $location.path(path).replace();
    };

    $scope.event    = EventHelperService;
    $scope.resource = $route.current.params.resource;
    $scope.tab      = $route.current.params.tab;
    if ($scope.tab === 'editor') {
      $scope.area   = 'segments';
    }
    $scope.id       = $route.current.params.itemId;

    $scope.event.eventId = $scope.id;

    $scope.unsavedChanges = false;

    $scope.setChanges = function(changed) {
      $scope.unsavedChanges = changed;
    };

    $scope.calculateDefaultThumbnailPosition = function () {
      return $scope.$root.calculateDefaultThumbnailPosition($scope.video.segments, $scope.video.thumbnail);
    };

    $scope.getTrackFlavorType = function (selector) {
      if (selector === 'single') {
        return $scope.video.source_tracks[0].flavor.type;
      }
      var track = $scope.video.source_tracks.find(function (track) {
        return track.side === selector;
      });
      if (track) {
        return track.flavor.type;
      }
      return undefined;
    };

    $scope.changeThumbnail = function (file, track, position) {
      $scope.video.thumbnail.loading = true;
      ToolsResource.thumbnail(
        { id: $scope.id, tool: 'thumbnail' },
        { file: file, track: $scope.getTrackFlavorType(track), position: position },
        function(response) {
          $scope.video.thumbnail = response.thumbnail;
          $scope.video.thumbnail.defaultThumbnailPositionChanged = false;
          if (response.thumbnail && response.thumbnail.type === 'DEFAULT') {
            $scope.$root.originalDefaultThumbnailPosition = response.thumbnail.position;
          }
          if (thumbnailErrorMessageId !== null) {
            Notifications.remove(thumbnailErrorMessageId, LOCAL_CONTEXT);
            thumbnailErrorMessageId = null;
          }
          $scope.video.thumbnail.loading = false;
        }, function() {
          thumbnailErrorMessageId = Notifications.add('error', 'THUMBNAIL_CHANGE_FAILED', LOCAL_CONTEXT);
          $scope.video.thumbnail.loading = false;
        });
    };

    $scope.openTab = function (tab) {
      $scope.tab = tab;
      if ($scope.tab === 'editor') {
        $scope.area = 'segments';
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

    $scope.anyTrackSelected = function (type) {
      var selected = false;
      var present = false;
      if ($scope.video.source_tracks === undefined) {
        return false;
      }
      for(var i = 0; i < $scope.video.source_tracks.length; i++) {
        var t = $scope.video.source_tracks[i][type];
        if (t.present === true) {
          present = true;
        }
        if (t.present === true && t.hidden === false) {
          selected = true;
        }
      }
      // If we don't have any tracks at all, selecting none is valid
      if (present === false) {
        return true;
      }
      return selected;
    };

    $scope.trackClicked = function(index, type) {
      $scope.video.source_tracks[index][type].hidden = !$scope.video.source_tracks[index][type].hidden;
    };

    $scope.trackHasPreview = function(index, type) {
      return $scope.video.source_tracks[index][type].preview_image !== null;
    };

    $scope.trackIndexToName = function(index) {
      var flavor = $scope.video.source_tracks[index].flavor;
      return flavor.type;
    };

    $scope.tooManyAudios = function () {
      var audioTrackCount = 0;
      var videoTrackCount = 0;
      for(var i = 0; i < $scope.video.source_tracks.length; i++) {
        var t = $scope.video.source_tracks[i];
        if (t.audio.present === true && t.audio.hidden === false) {
          audioTrackCount++;
        }
        if (t.video.present === true && t.video.hidden === false) {
          videoTrackCount++;
        }
      }
      return audioTrackCount > videoTrackCount;
    };

    $scope.sanityCheckFlags = function() {
      return (!$scope.anyTrackSelected('video') || $scope.tooManyAudios()) || !$scope.anyTrackSelected('audio');
    };

    // TODO Move the following to a VideoCtrl
    $scope.player = {};
    $scope.video  = ToolsResource.get({ id: $scope.id, tool: 'editor' });

    $scope.activeSubmission = false;

    $scope.submit = function () {
      $scope.activeSubmission = true;
      $scope.video.thumbnail.loading = $scope.video.thumbnail && $scope.video.thumbnail.type &&
              ($scope.video.thumbnail.type === 'DEFAULT');
      // Remember $scope.video.workflow as $scope.video.$save will potentially overwrite this value
      var closeVideoEditor = $scope.video.workflow;
      $scope.video.$save({ id: $scope.id, tool: $scope.tab }, function (response) {
        $scope.activeSubmission = false;
        if (closeVideoEditor) {
          Notifications.add('success', 'VIDEO_CUT_PROCESSING');
          $location.url('/events/' + $scope.resource);
        } else {
          Notifications.add('success', 'VIDEO_CUT_SAVED');
        }
        $scope.unsavedChanges = false;
        if (response.segments) {
          $scope.$root.originalSegments = angular.copy(response.segments);
        }
        $scope.video.thumbnail.defaultThumbnailPositionChanged = false;
        if (response.thumbnail && response.thumbnail.type === 'DEFAULT') {
          $scope.$root.originalDefaultThumbnailPosition = response.thumbnail.position;
        }
        $scope.video.thumbnail.loading = false;
        if (trackErrorMessageId !== null) {
          Notifications.remove(trackErrorMessageId);
          trackErrorMessageId = null;
        }
      }, function () {
        $scope.activeSubmission = false;
        $scope.video.thumbnail.loading = false;
        trackErrorMessageId = Notifications.add('error', 'VIDEO_CUT_NOT_SAVED', LOCAL_CONTEXT);
      });
    };

    $scope.leave = function () {
      Storage.put('pagination', $scope.resource, 'resume', true);
      $location.url('/events/' + $scope.resource);
    };

    $scope.$on('$destroy', function () {
      Notifications.removeAll(LOCAL_CONTEXT);
    });
  }
]);
