angular.module('adminNg.directives')
.directive('adminNgTimeline', ['PlayerAdapter', '$document', 'VideoService',
function (PlayerAdapter, $document, VideoService) {
    return {
        templateUrl: 'shared/partials/timeline.html',
        priority: 0,
        scope: {
            player: '=',
            video:  '='
        },
        link: function (scope, element) {
            var replaySegment = {};

            scope.position = 0;
            scope.positionStyle = 0;
            scope.widthPerSegment = 0;
            scope.zoomLevel = 0;

            scope.player.adapter.addListener(PlayerAdapter.EVENTS.TIMEUPDATE, function () {
                scope.position = scope.player.adapter.getCurrentTime() * 1000;
                scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';

                var segment = VideoService.getCurrentSegment(scope.player, scope.video);

                // Mark current segment as selected
                scope.selectSegment(segment);

                // Stop play back when switching from a replayed segment to
                // the next.
                if (replaySegment.replay && !segment.replay) {
                    scope.player.adapter.pause();
                    scope.player.adapter.setCurrentTime(replaySegment.start/1000);
                    replaySegment.replay = false;
                    return;
                }
                if (segment.replay) {
                    replaySegment = segment;
                }

                // Skip deleted segments while playing
                if (segment.deleted && scope.player.adapter.getStatus() === PlayerAdapter.STATUS.PLAYING) {
                    scope.player.adapter.setCurrentTime(segment.end/1000);
                }
            });

            /**
             * Formats time stamps to HH:MM:SS.sss
             *
             * @param {Number} Time in milliseconds
             * @return {String} Formatted time string
             */
            scope.formatMilliseconds = function (ms) {
                var date = new Date(ms),
                    pad = function (number, padding) {
                        return (new Array(padding + 1).join('0') + number)
                            .slice(-padding);
                    };
                return pad(date.getUTCHours(), 2) + ':' +
                    pad(date.getUTCMinutes(), 2) + ':' +
                    pad(date.getUTCSeconds(), 2) + '.' +
                    pad(date.getUTCMilliseconds(), 3);
            };

            /**
             * Calculates the relative width of a track segment.
             *
             * @param {Object} segment Segment object
             * @return {String} Width of the segment in percent
             */
            scope.getSegmentWidth = function (segment) {
                if (angular.isUndefined(scope.video.duration)) {
                    return 0;
                }

                var zoom         = 1,
                    absoluteSize = segment.end - segment.start,
                    relativeSize = absoluteSize / scope.video.duration,
                    scaledSize   = relativeSize * zoom;

                return (scaledSize * 100) + '%';
            };

            /**
             * Returns the visible amount of milliseconds for the current zoom level.
             *
             * The zoom slider provides values from 0 (zoomed out) to 100
             * (fully zoomed in). When zoomed out, the entire video should
             * be visible whereas when fully zoomed in, 10s should be visible.
             *
             * These constraints can be derived to the following linear equation:
             *
             *                10,000 - duration
             * y(zoomlevel) = ----------------- * zoomlevel + duration
             *                       100
             *
             * @return {Number} Visible interval in milliseconds
             */
            scope.getZoomValue = function () {
                return (10000 - scope.video.duration) / 100 * scope.zoomLevel +
                    scope.video.duration;
            };

            /**
             * Returns the offset for the currently visible portion.
             *
             * Based on the following linear equation.
             *
             *          duration
             * y(pos) = -------- * pos - pos
             *           zoom
             *
             * @return {Number} Relative offset
             */
            scope.getZoomOffset = function () {
                return scope.position * scope.video.duration / scope.zoomValue -
                    scope.position;
            };

            /**
             * Returns a style for the given segment.
             *
             * Applies track background and zoom parameters.
             *
             * @param {Object} track Current segment object
             * @return {Object} ng-style compatible hash
             */
            scope.getSegmentStyle = function (track) {
                if (angular.isUndefined(scope.video.duration)) {
                    return {};
                }

                // Cache the zoom value and position
                scope.zoomValue = scope.getZoomValue();
                scope.zoomOffset = scope.getZoomOffset();
                scope.from = scope.zoomOffset;
                scope.to = scope.zoomOffset + scope.zoomValue;

                var style = {
                    width: (scope.video.duration * 100 / scope.zoomValue) + '%',
                    left:  (scope.zoomOffset * -100 / scope.video.duration) + '%'
                };

                if (track.waveform) {
                    style['background-image'] = 'url(' + track.waveform + ')';
                }

                return style;
            };

            /**
             * Calculates the offset for the zoom field of vision.
             *
             * Based on the following linear equation:
             *
             *          duration - zoom
             * f(pos) = --------------- * pos
             *             duration
             *
             * @return {Number} Offset in milliseconds
             */
            scope.getZoomFieldOffset = function () {
                return (scope.video.duration - scope.zoomValue) *
                        scope.position / scope.video.duration;
            };

            /**
             * Returns a style for the zoom field of vision.
             *
             * @return {Object} ng-style compatible hash
             */
            scope.getZoomStyle = function () {
                if (angular.isUndefined(scope.video.duration)) {
                    return {};
                }
                var style = {
                    width: (scope.zoomValue * 100 / scope.video.duration) + '%',
                    left:  (scope.getZoomFieldOffset() * 100 / scope.video.duration) + '%'
                };

                return style;
            };

            /**
             * Removes the given segment.
             *
             * The previous or, failing that, the next segment will take up
             * the sapce of the given segment.
             *
             * @param {Object} segment Segment object
             */
            scope.mergeSegment = function (segment) {
                var index = scope.video.segments.indexOf(segment);
                if (scope.video.segments[index-1]) {
                    scope.video.segments[index-1].end = segment.end;
                    scope.video.segments.splice(index, 1);
                } else if (scope.video.segments[index+1]) {
                    scope.video.segments[index+1].start = segment.start;
                    scope.video.segments.splice(index, 1);
                }
            };

            /**
             * Sets the cursor position depending on the mouse coordinates.
             *
             * @param {Event} event Event that triggered this method.
             */
            scope.move = function (event) {
                event.preventDefault();
                if (!scope.canMove) { return; }

                var track = element.find('.timeline-track'),
                    position = (event.clientX - track.offset().left) / track.width();

                scope.position = position * scope.video.duration;

                // Limit position to the length of the video
                if (scope.position > scope.video.duration) {
                    scope.position = scope.video.duration;
                }
                if (scope.position < 0) {
                    scope.position = 0;
                }

                scope.$apply(function () {
                    scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';
                });
            };

            // Stop dragging the cursor as soon as the mouse button is
            // released.
            $document.mouseup(function () {
                scope.canMove = false;
                scope.player.adapter.setCurrentTime(scope.position / 1000);
                $document.unbind('mousemove', scope.move);
            });

            /**
             * Callback to a mouse-down event initiating a drag of the
             *
             * @param {Event} event Event that triggered this method.
             * position handle.
             */
            scope.drag = function (event) {
                event.preventDefault();
                scope.canMove = true;

                // Register global mouse move callback
                $document.mousemove(scope.move);
            };

            /**
             * Sets the position marker to the start of the given segment.
             *
             * @param {Object} segment Segment object
             */
            scope.skipToSegment = function (segment) {
                if (!segment.selected) {
                    scope.player.adapter.setCurrentTime(segment.start / 1000);
                }
            };

            /**
             * Marks the given segment as selected.
             *
             * @param {Object} segment Segment object
             */
            scope.selectSegment = function (segment) {
                angular.forEach(scope.video.segments, function (segment) {
                    segment.selected = false;
                });
                segment.selected = true;
            };

            scope.$on('$destroy', function () {
                $document.unbind('mouseup');
                $document.unbind('mousemove');
            });
       }
    };
}]);
