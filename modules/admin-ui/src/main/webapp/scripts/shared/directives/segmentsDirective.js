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
.directive('adminNgSegments', ['PlayerAdapter', '$document', 'VideoService', '$timeout',
function (PlayerAdapter, $document, VideoService, $timeout) {
    return {
        templateUrl: 'shared/partials/segments.html',
        priority: 0,
        scope: {
            player: '=',
            video: '='
        },
        link: function (scope, element) {

            /**
            * Formats time stamps to HH:MM:SS.sss
            *
            * @param {Number} ms is the time in milliseconds,
            * @param {Boolean} showMilliseconds should the milliseconds be displayed
            * @return {String} Formatted time string
           */
            scope.formatMilliseconds = function (ms, showMilliseconds) {

               if (isNaN(ms)) {
                   return '';
               }

               var date = new Date(ms),
                   pad = function (number, padding) {
                       return (new Array(padding + 1).join('0') + number)
                           .slice(-padding);
                   };

               if (typeof showMilliseconds === 'undefined') {
                   showMilliseconds = true;
               }

               return pad(date.getUTCHours(), 2) + ':' +
                   pad(date.getUTCMinutes(), 2) + ':' +
                   pad(date.getUTCSeconds(), 2) +
                   (showMilliseconds ? '.' + pad(date.getUTCMilliseconds(), 3) : '');
            };

            /**
             * Converts a string with a human readable time to ms
             *
             * @param {type} time in the format HH:MM:SS.sss
             * @returns {Number} time in ms
             */
            scope.parseTime = function (time) {
              if ( time !== undefined && time.length === 12) {
                var hours = parseInt(time.substring(0,2)),
                    minutes = parseInt(time.substring(3,5)),
                    seconds = parseInt(time.substring(6,8)),
                    millis = parseInt(time.substring(9));

                return millis + (seconds * 1000) + (minutes * 60000) + (hours * 3600000);
              }

            };

            /**
             * Returns an object that describes the css classes for a given segment.
             *
             * @param {Object} segment object
             * @return {Object} object with {class}: {boolean} values for CSS classes.
             */
            scope.getSegmentClass = function (segment) {
                return { deleted: segment.deleted, selected: segment.selected};
            };

            /**
             * Removes the given segment.
             *
             * The previous or, failing that, the next segment will take up
             * the space of the given segment.
             *
             * @param {Event} event that triggered the merge action
             * @param {Object} segment Segment object
             */
            scope.mergeSegment = function (event, segment) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (!scope.isRemovalAllowed(segment)) {
                    return;
                }

                var index = scope.video.segments.indexOf(segment);

                if (scope.video.segments[index - 1]) {
                    scope.video.segments[index - 1].end = segment.end;
                    scope.video.segments.splice(index, 1);
                } else if (scope.video.segments[index + 1]) {
                    scope.video.segments[index + 1].start = segment.start;
                    scope.video.segments.splice(index, 1);
                }
              scope.$root.$broadcast("segmentTimesUpdated");
            };

            /**
             * Toggle the deleted flag for a segment. Indicating if it should be used or not.
             *
             * @param {Event} event for checkbox link - stop the propogation
             * @param {Object} segment object on which the deleted variable will change
             */
            scope.toggleSegment = function (event, segment) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (angular.isUndefined(segment) || scope.video.segments.indexOf(segment) === -1) {
                    return;
                }

                if (scope.isRemovalAllowed(segment)) {
                    segment.deleted = !segment.deleted;
                    scope.$root.$broadcast("segmentToggled");
                }
            };

            /**
             * Sets the position marker to the start of the given segment.
             *
             * @param {Event} event details
             * @param {Object} segment Segment object
             */
            scope.skipToSegment = function (event, segment) {
                event.preventDefault();

                if (!segment.selected) {
                    scope.player.adapter.setCurrentTime(segment.start / 1000);
                }
            };

            scope.isRemovalAllowed = function(segment) {
                if (!segment) {
                    return false;
                }

                return (segment.deleted || scope.video.segments
                                               .filter(function(seg) {
                                                   return !seg.deleted;
                                               }).length > 1);
            };

            /**
             * Sets / Updates the human readable start and end times of the segments.
             */
            scope.setHumanReadableTimes = function () {
              angular.forEach(scope.video.segments, function(segment, key) {
                segment.startTime = scope.formatMilliseconds(segment.start);
                segment.endTime = scope.formatMilliseconds(segment.end);
              });
            };

            /*
             * Make sure that times are updates if needed.
             */
            scope.$root.$on("segmentTimesUpdated", function () {
              scope.setHumanReadableTimes();
              scope.$parent.setChanges(scope.segmentsChanged());
              scope.video.thumbnail.defaultThumbnailPositionChanged = scope.defaultThumbnailPositionChanged();
            });

            scope.$root.$on("segmentToggled", function () {
              scope.$parent.setChanges(scope.segmentsChanged());
              scope.video.thumbnail.defaultThumbnailPositionChanged = scope.defaultThumbnailPositionChanged();
            });

            scope.segmentsChanged = function() {
                if (scope.video.segments.length !== scope.$root.originalSegments.length) return true;
                return !scope.$root.originalSegments.every(function(curr, idx) {
                  return curr.start === scope.video.segments[idx].start
                      && curr.end === scope.video.segments[idx].end
                      && !curr.deleted === !scope.video.segments[idx].deleted;
                });
            };

            scope.defaultThumbnailPositionChanged = function () {
              if (!scope.video.thumbnail || !scope.video.thumbnail.type === 'DEFAULT') {
                return false;
              }
              var currentPosition = scope.$root.calculateDefaultThumbnailPosition(scope.video.segments, scope.video.thumbnail);
              var result = Math.abs(currentPosition - scope.$root.originalDefaultThumbnailPosition) > 0.001;
              if (result) {
                 scope.video.thumbnail.position = currentPosition;
              }
              return result;
            };

            // Position of the the default thumbnail within the uncut "source" video has to change upon cutting
            scope.$root.calculateDefaultThumbnailPosition = function (segments, thumbnail) {
              var duration = 0;
              for (var idx = 0; idx < segments.length; idx++) {
                var segment = segments[idx];
                if (segment.deleted) {
                  continue;
                }
                var start = segment.start / 1000.0;
                var end = segment.end / 1000.0;
                var segmentDuration = end - start;
                if (duration + segmentDuration > thumbnail.defaultPosition) {
                  return start + thumbnail.defaultPosition - duration;
                }
                duration += segmentDuration;
              }
              return thumbnail.defaultPosition;
            };

            /**
             * Checks if a time is within the valid boundaries
             * @param {type} time time to check
             * @returns {Boolean} true if time is > 0 and <video duration
             */
            scope.timeValid = function (time) {
              if (time >= 0 && time <= scope.video.duration) {
                return true;
              } else {
                return false;
              }
            };

            /**
             * Set a new Start time of a segment, other segments are changed or deleted accordingly
             * @param {type} segment that should change
             */

            scope.updateStartTime = function (segment) {
              var newTime = scope.parseTime(segment.startTime);
              if (newTime && newTime !== segment.start) {
                if (newTime > segment.end || ! scope.timeValid(newTime)) {
                  segment.startTime = scope.formatMilliseconds(segment.start);
                } else {
                  var previousSegment = scope.getPreviousSegment(segment);
                  var allow = scope.isRemovalAllowed(previousSegment);
                  segment.start = newTime;
                  while (previousSegment && allow && previousSegment.start > newTime) {
                    scope.removeSegment(previousSegment);
                    previousSegment = scope.getPreviousSegment(segment);
                    allow = scope.isRemovalAllowed(previousSegment);
                  }
                  if (!allow && previousSegment) {
                    if (previousSegment.start > newTime) {
                        segment.start = previousSegment.end;
                    }
                    else {
                        var endTime = Math.max(newTime, previousSegment.start + 1);
                        segment.start = previousSegment.end = endTime;
                    }
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                  else if (previousSegment) {
                    previousSegment.end = newTime;
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                }
              }
            };

            /**
             * Sets a new end time of a segment, other segments are changed or deleted accordingly
             * @param {type} segment that should change
             */
            scope.updateEndTime = function (segment) {
              var newTime = scope.parseTime(segment.endTime);
              if (newTime && newTime !== segment.end) {
                if (newTime < segment.start || ! scope.timeValid(newTime)) {
                  segment.endTime = scope.formatMilliseconds(segment.end);
                } else {
                  var nextSegment = scope.getNextSegment(segment);
                  var allow = scope.isRemovalAllowed(nextSegment);
                  segment.end = newTime;
                  while (nextSegment && allow && nextSegment.end < newTime) {
                    scope.removeSegment(nextSegment);
                    nextSegment = scope.getNextSegment(segment);
                    allow = scope.isRemovalAllowed(nextSegment);
                  }
                  if (!allow && nextSegment) {
                    if (nextSegment.end < newTime) {
                      segment.end = nextSegment.start;
                    }
                    else {
                      var startTime = Math.min(newTime, nextSegment.end - 1);
                      segment.end = nextSegment.start = startTime;
                    }
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                  else if (nextSegment) {
                    nextSegment.start = newTime;
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                }
              }
            };

            /**
             * Deletes a segment from the segment list. Times of other segments are not changed!
             * @param {type} segment that should be deleted
             */
            scope.removeSegment = function (segment) {
              if (segment) {
                var index = scope.video.segments.indexOf(segment);
                scope.video.segments.splice(index, 1);
              }
            };

            /**
             * Gets the segment previous to the provided segment.
             * @param {type} currentSegment the reference segment
             * @returns {unresolved} the previous segment or "undefinded" if current segment is the first
             */
            scope.getPreviousSegment = function (currentSegment) {
              var index = scope.video.segments.indexOf(currentSegment);
              if (index > 0)
                return scope.video.segments[index - 1];
            };

            /**
             * Gets the next segment to the provided segment
             * @param {type} currentSegment the reference segment
             * @returns {unresolved} the next segment or "undefined" if the current segment is the last.
             */
            scope.getNextSegment = function (currentSegment) {
              var index = scope.video.segments.indexOf(currentSegment);
              if (index < (scope.video.segments.length - 1))
                return scope.video.segments[index + 1];
            };

            scope.video.$promise.then(function () {
              // Take a snapshot of the original segments to track if we have changes
              scope.$root.originalSegments = angular.copy(scope.video.segments);

              // In case the default thumbnail is used, store the original position, to be able to check if the
              // thumbnail position has to be adjusted when cutting changes
              if (scope.video.thumbnail && scope.video.thumbnail.type === 'DEFAULT') {
                scope.$root.originalDefaultThumbnailPosition = scope.video.thumbnail.position;
              }

              scope.$root.$broadcast("segmentTimesUpdated");
            });
        }
    };
}]);
