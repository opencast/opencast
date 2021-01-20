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

angular.module('adminNg.directives')
.directive('videoPlayer', ['PlayerAdapter', 'PlayerAdapterRepository', 'VideoService', '$timeout', 'HotkeysService',
  function (PlayerAdapter, PlayerAdapterRepository, VideoService, $timeout, HotkeysService) {

    return {
      restrict: 'A',
      priority: 10,
      templateUrl: 'shared/partials/player.html',
      scope: {
        player   : '=',
        video    : '=',
        controls : '@',
        subControls: '@'
      },
      link: function (scope, element, attrs) {
        function loadPlayerAdapter(element) {
          scope.player.adapter = PlayerAdapterRepository.
                    createNewAdapter(
                      attrs.adapter.toUpperCase(),
                      element
                    );

          scope.playing = false;
          scope.positionStart = false;
          scope.positionEnd = false;
          scope.muted = false;
          scope.volume = 100;
          scope.playButtonTooltip = 'VIDEO_TOOL.PLAYER.PLAY';
          scope.time = {
            hours: 0,
            minutes: 0,
            seconds: 0,
            milliseconds: 0
          };

          // Get the frame rate of the currently displayed preview
          // and store it in the scope for frame-by-frame scrubbing.
          function findFrameRate() {
            for (var i = 0; i < scope.video.previews.length; ++i) {
              if (scope.video.previews[i].uri === scope.player.adapter.getCurrentSource()) {
                scope.frameRate = scope.video.previews[i].frameRate;
                return;
              }
            }
          }
          if (scope.player.adapter.ready()) {
            findFrameRate();
          } else {
            scope.player.adapter.addListener(PlayerAdapter.EVENTS.CAN_PLAY, findFrameRate);
          }

          scope.player.adapter.addListener(PlayerAdapter.EVENTS.PAUSE, function () {
            scope.$apply(function () {
              scope.playing = false;
              scope.playButtonTooltip = 'VIDEO_TOOL.PLAYER.PLAY';
            });
          });

          scope.player.adapter.addListener(PlayerAdapter.EVENTS.PLAY, function () {
            scope.$apply(function () {
              scope.playing = true;
              scope.playButtonTooltip = 'VIDEO_TOOL.PLAYER.PAUSE';
            });
          });

          scope.player.adapter.addListener(PlayerAdapter.EVENTS.TIMEUPDATE, function () {
            // Expose the time to the view
            scope.time = scope.player.adapter.getCurrentTimeObject();

            scope.$apply();
          });

          scope.player.adapter.addListener(PlayerAdapter.EVENTS.VOLUMECHANGE, function () {
            scope.muted = scope.player.adapter.muted();
            scope.volume = scope.player.adapter.volume();
          });

          scope.$watch(function (){
            if (controlsVisible !== scope.controls) {
              controlsVisible = scope.controls;
              var videoObj = angular.element('#player')[0];
              if (videoObj) {
                if (scope.controls == 'true') {
                  videoObj.setAttribute('controls', 'controls');
                } else {
                  if (videoObj.hasAttribute('controls')) {
                    videoObj.removeAttribute('controls');
                  }
                }
              }
            }
          });
        }

        var controlsVisible;

        // Check if the player element is loaded,
        function checkPlayerElement(tries, callback) {
          if (tries > 0) {
            var element = angular.element('#' + attrs.playerRef)[0];
            if (element) {
              callback(element);
            } else {
              // Wait 100ms before to retry
              scope.checkTimeout = $timeout(function () {
                checkPlayerElement(tries - 1, callback);
              }, 100);
            }
          }
        }

        function getTimeInSeconds(time) {
          var millis = time.milliseconds;
          millis += time.seconds * 1000;
          millis += time.minutes * 60 * 1000;
          millis += time.hours * 60 * 60 * 1000;
          return millis / 1000;
        }

        scope.$on('$destroy', function () {
          $timeout.cancel(scope.checkTimeout);
          if (angular.isDefined(scope.player.adapter)) {
            scope.player.adapter.pause();
          }
        });

        scope.previousFrame = function () {
          if (!scope.frameRate) return;
          scope.player.adapter.setCurrentTime(
            scope.player.adapter.getCurrentTime() - 1 / scope.frameRate
          );
        };

        scope.nextFrame = function () {
          if (!scope.frameRate) return;
          scope.player.adapter.setCurrentTime(
            scope.player.adapter.getCurrentTime() + 1 / scope.frameRate
          );
        };

        scope.previousSegment = function () {
          var segment = VideoService.getCurrentSegment(scope.player, scope.video),
              index = scope.video.segments.indexOf(segment);
          if (scope.video.segments[index - 1]) {
            segment = scope.video.segments[index - 1];
          }
          scope.player.adapter.setCurrentTime(segment.start / 1000 );
        };

        scope.nextSegment = function () {
          var segment = VideoService.getCurrentSegment(scope.player, scope.video);
          scope.player.adapter.setCurrentTime(segment.end / 1000 );
        };

        scope.play = function () {
          if (scope.playing) {
            scope.player.adapter.pause();
          } else {
            scope.player.adapter.play();
          }
        };

        scope.pause = function () {
          scope.player.adapter.pause();
        };

        scope.changeTime = function (time) {
          scope.player.adapter.setCurrentTime(getTimeInSeconds(time));
        };

        scope.stepBackward = function () {
          var newTime = scope.player.adapter.getCurrentTime() - 10;
          scope.player.adapter.setCurrentTime(newTime);
        };

        scope.stepForward = function () {
          var newTime = scope.player.adapter.getCurrentTime() + 10;
          scope.player.adapter.setCurrentTime(newTime);
        };

        scope.toggleMute = function () {
          scope.player.adapter.muted(! scope.player.adapter.muted());
        };

        scope.setVolume = function () {
          scope.player.adapter.volume(scope.volume);
        };

        scope.volumeUp = function () {
          if (scope.volume + 10 <= 100) {
            scope.volume = scope.volume + 10;
          } else {
            scope.volume = 100;
          }
          scope.setVolume();
        };

        scope.volumeDown = function () {
          if (scope.volume - 10 >= 0) {
            scope.volume = scope.volume - 10;
          } else {
            scope.volume = 0;
          }
          scope.setVolume();
        };

        scope.subControls = angular.isDefined(scope.subControls) ? scope.subControls : 'true';

        // Check for the player (10 times) before to load the adapter
        checkPlayerElement(10, loadPlayerAdapter);

        HotkeysService.activateHotkey(scope, 'player.play_pause', function (event) {
          event.preventDefault();
          scope.play();
        });

        HotkeysService.activateHotkey(scope, 'player.previous_frame', function (event) {
          event.preventDefault();
          scope.pause();
          scope.previousFrame();
        });

        HotkeysService.activateHotkey(scope, 'player.next_frame', function (event) {
          event.preventDefault();
          scope.pause();
          scope.nextFrame();
        });

        HotkeysService.activateHotkey(scope, 'player.step_backward', function (event) {
          event.preventDefault();
          scope.stepBackward();
        });

        HotkeysService.activateHotkey(scope, 'player.step_forward', function (event) {
          event.preventDefault();
          scope.stepForward();
        });

        HotkeysService.activateHotkey(scope, 'player.previous_segment', function (event) {
          event.preventDefault();
          scope.previousSegment();
        });

        HotkeysService.activateHotkey(scope, 'player.next_segment', function (event) {
          event.preventDefault();
          scope.nextSegment();
        });

        HotkeysService.activateHotkey(scope, 'player.volume_up', function (event) {
          event.preventDefault();
          scope.volumeUp();
        });

        HotkeysService.activateHotkey(scope, 'player.volume_down', function (event) {
          event.preventDefault();
          scope.volumeDown();
        });

        HotkeysService.activateHotkey(scope, 'player.mute', function (event) {
          event.preventDefault();
          scope.toggleMute();
        });
      }
    };
  }]);
