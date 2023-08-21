/*
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
.controller('ToolsCtrl', ['$scope', '$route', '$location', 'Storage', '$window',
  'ToolsResource', 'Notifications', 'EventHelperService', 'MetadataSaveService',
  'PlayerAdapter', '$q',
  function ($scope, $route, $location, Storage, $window, ToolsResource,
    Notifications, EventHelperService, MetadataSaveService, PlayerAdapter, $q) {

    var thumbnailErrorMessageId = null;
    var trackErrorMessageId = null;

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

    $scope.changeThumbnailPreviewJump = function(track, position) {
      $scope.player.adapter.addListener(PlayerAdapter.EVENTS.SEEKED, function() {
        $scope.changeThumbnailPreview(track);
      }, { once: true });
      $scope.player.adapter.setCurrentTime(position);
    };

    $scope.changeThumbnailPreview = function (track) {
      var position = $scope.player.adapter.getCurrentTime();

      // generate preview image
      var canvas = document.createElement('canvas');
      var video = document.getElementById('player');
      var vidWidth = video.videoWidth;
      var vidHeight = video.videoHeight;
      if (vidWidth > vidHeight) {
        canvas.width = 640;
        canvas.height = 640 * vidHeight / vidWidth;
      } else {
        canvas.height = 480;
        canvas.width = 480 * vidWidth / vidHeight;
      }
      var ctx = canvas.getContext('2d');
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

      ctx.font = '80px Noto Sans';
      ctx.fillStyle = 'gray';
      ctx.textAlign = 'center';
      ctx.fillText('@' + position.toFixed(2) , canvas.width / 2, canvas.height / 2);

      var preview_data = canvas.toDataURL();
      var preview_bin = atob(preview_data.split(',')[1]);
      var preview_type = preview_data.split(':')[1].split(';')[0];
      var preview_ab = new ArrayBuffer(preview_bin.length);
      var preview_ia = new Uint8Array(preview_ab);
      for (var i = 0; i < preview_bin.length; i++) {
        preview_ia[i] = preview_bin.charCodeAt(i);
      }
      var preview_blob = new Blob([preview_ab], {type: preview_type});

      $scope.changeThumbnail(undefined, track, position, preview_blob);
    };

    $scope.changeThumbnail = function (file, track, position, previewfile) {
      $scope.video.thumbnail.loading = true;
      ToolsResource.thumbnail(
        { id: $scope.id, tool: 'thumbnail' },
        { file: file, track: $scope.getTrackFlavorType(track), position: position, previewfile: previewfile },
        function(response) {
          $scope.video.thumbnail = response.thumbnail;
          $scope.video.thumbnail.defaultThumbnailPositionChanged = false;
          if (response.thumbnail && response.thumbnail.type === 'DEFAULT') {
            $scope.$root.originalDefaultThumbnailPosition = response.thumbnail.position;
          }
          if (thumbnailErrorMessageId !== null) {
            Notifications.remove(thumbnailErrorMessageId);
            thumbnailErrorMessageId = null;
          }
          $scope.video.thumbnail.loading = false;
        }, function() {
          thumbnailErrorMessageId = Notifications.add('error', 'THUMBNAIL_CHANGE_FAILED');
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

    $scope.submit = function (catalogs, commonMetadataCatalog) {
      $scope.activeSubmission = true;
      $scope.video.thumbnail.loading = $scope.video.thumbnail && $scope.video.thumbnail.type &&
              ($scope.video.thumbnail.type === 'DEFAULT');
      // Remember $scope.video.workflow as $scope.video.$save will potentially overwrite this value
      var closeVideoEditor = $scope.video.workflow;
      var metadataSavePromises = MetadataSaveService.save(
        catalogs,
        commonMetadataCatalog,
        $scope.id);

      var saveEditorState = function() {
        $scope.video.$save({ id: $scope.id, tool: $scope.tab }, function (response) {
          $scope.activeSubmission = false;
          if (closeVideoEditor) {
            Notifications.add('success', 'VIDEO_CUT_PROCESSING');
            Storage.put('pagination', $scope.resource, 'resume', true);
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
          trackErrorMessageId = Notifications.add('error', 'VIDEO_CUT_NOT_SAVED');
        });
      };

      // Save the editor state, no matter what happened to the metadata save op
      $q.all(metadataSavePromises).then(saveEditorState, saveEditorState);
    };

    $scope.leave = function () {
      Storage.put('pagination', $scope.resource, 'resume', true);
      $location.url('/events/' + $scope.resource);
    };
  }
]);
