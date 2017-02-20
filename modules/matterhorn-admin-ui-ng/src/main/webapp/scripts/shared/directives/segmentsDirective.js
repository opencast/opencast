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

                return millis + (seconds * 1000) + (minutes * 60000) + (hours * 36000000);

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

                var index = scope.video.segments.indexOf(segment);

                if (scope.video.segments[index - 1]) {
                    scope.video.segments[index - 1].end = segment.end;
                    scope.video.segments.splice(index, 1);
                } else if (scope.video.segments[index + 1]) {
                    scope.video.segments[index + 1].start = segment.start;
                    scope.video.segments.splice(index, 1);
                }
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

                segment.deleted = !segment.deleted;
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

            scope.initHumanReadableTimes = function () {

              var n = 0;
              angular.forEach(scope.video.segments, function(segment, key) {
                segment.startTime = scope.formatMilliseconds(segment.start);
                segment.endTime = scope.formatMilliseconds(segment.end);
                scope.$watch('segment.start', function(new_time, old_time) {
                  console.log("Found new Start time:" + segment.start);
          //        segment.startTime = scope.formatMilliseconds(segment.start);
                });
                scope.$watch('segment.end', function(new_time, old_time) {
                  console.log("Found new End time:" + segment.end);
          //        segment.endTime = scope.formatMilliseconds(segment.start);
                });
              });

            };

            scope.updateStartTime = function (segment) {
              var newTime = scope.parseTime(segment.startTime);
              if (newTime && newTime !== segment.start) {
                var previousSegment = scope.getPreviousSegment(segment);
                segment.start = newTime;
                if (previousSegment) {
                  previousSegment.end = newTime;
                }
              }
            };

            scope.updateEndTime = function (segment) {
              var newTime = scope.parseTime(segment.endTime);
              if (newTime && newTime !== segment.end) {
                var nextSegment = scope.getNextSegment(segment);
                segment.end = newTime;
                if (nextSegment) {
                  nextSegment.start = newTime;
                }
              }
            };

            scope.getPreviousSegment = function (currentSegment) {
              var previousSegment;

              angular.forEach(scope.video.segments, function(segment, key) {
                if (currentSegment.start > segment.start) {
                  if (! previousSegment) {
                    previousSegment = segment;
                  } else if (previousSegment.start < segment.start) {
                    previousSegment = segment;
                  }
                }
              });

              return previousSegment;
            }

            scope.getNextSegment = function (currentSegment) {
              var nextSegment;

              angular.forEach(scope.video.segments, function(segment, key) {
                if (currentSegment.start < segment.start) {
                  if (! nextSegment) {
                    nextSegment = segment;
                  } else if (nextSegment.start > segment.start) {
                    nextSegment = segment;
                  }
                }
              });

              return nextSegment;
            }

            scope.initHumanReadableTimes();
        }
    };
}]);