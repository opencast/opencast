angular.module('adminNg.directives')
.directive('adminNgTimeline', ['PlayerAdapter', '$document', 'VideoService',
function (PlayerAdapter, $document, VideoService) {
    return {
        templateUrl: 'shared/partials/timeline.html',
        priority: 0,
        scope: {
            player: '=',
            video: '='
        },
        link: function (scope, element) {
            var replaySegment = {};

            scope.position = 0;
            scope.positionStyle = 0;
            scope.widthPerSegment = 0;

            scope.ZoomSelectOptions = [
                { name: 'All', time: 0 }
                , { name: '10 m', time: 600000 }
                , { name: '5 m', time: 300000 }
                , { name: '1 m', time: 60000 }
                , { name: '30 s', time: 30000 }
            ];

            scope.zoomLevel = 0;
            scope.zoomValue = 0;
            scope.zoomSelected = scope.ZoomSelectOptions[0];
            scope.zoomOffset = 0;
            scope.zoomFieldOffset = 0;

            scope.from = 0;
            scope.to = 0;

            scope.formatMilliseconds = utils.formatMilliseconds;

            scope.player.adapter.addListener(PlayerAdapter.EVENTS.TIMEUPDATE, function () {
                scope.position = scope.player.adapter.getCurrentTime() * 1000;
                scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';

                var segment = VideoService.getCurrentSegment(scope.player, scope.video);

                // Mark current segment as selected
                //scope.selectSegment(segment);

                // Stop play back when switching from a replayed segment to
                // the next.
                if (replaySegment.replay && !segment.replay) {
                    scope.player.adapter.pause();
                    scope.player.adapter.setCurrentTime(replaySegment.start / 1000);
                    replaySegment.replay = false;
                    return;
                }
                if (segment.replay) {
                    replaySegment = segment;
                }

                // Skip deleted segments while playing
                if (segment.deleted && scope.player.adapter.getStatus() === PlayerAdapter.STATUS.PLAYING) {
                    scope.player.adapter.setCurrentTime(segment.end / 1000);
                }
            });

            /**
             * Initialize the display values and set zoomLevel
             */
            scope.init = function () {
                console.log('init');

                scope.from = 0;
                scope.to = scope.getZoomValue();

                console.log(scope.zoomLevel + ' ' + scope.zoomValue + ' ' + scope.video.duration)
            }

            scope.displayZoomLevel = function (ms) {

                var date = new Date(ms),
                    st = '&#8776; ';

                if (date.getUTCHours() > 0) {
                    st += (date.getUTCHours() + (date.getUTCMinutes() >= 30 ? 1 : 0)) + ' h';
                }
                else if (date.getUTCMinutes() > 0) {
                    st += (date.getUTCMinutes() + (date.getUTCSeconds() >= 30 ? 1 : 0)) + ' m';
                } else {
                    st += date.getUTCSeconds() + ' s';
                }
                console.log('Display: ' + st);

                var dropdown_text = element.find('.zoom-control .chosen-container > a > span');
                if (dropdown_text) {
                    dropdown_text.html(st);
                }
            }

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

                var zoom = 1,
                    absoluteSize = segment.end - segment.start,
                    relativeSize = absoluteSize / scope.video.duration,
                    scaledSize = relativeSize * zoom;

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

                console.log('ZV: ' + scope.zoomLevel + ' ' + scope.video.duration);

                return (10000 - scope.video.duration) / 100 * scope.zoomLevel +
                    scope.video.duration;
            };

            scope.$watch(function (scope) { return scope.position },
               function () {
                   scope.zoomOffset = scope.getZoomOffset();
                   scope.zoomFieldOffset = scope.getZoomFieldOffset();
               });

            scope.$watch(function (scope) { return scope.zoomLevel },
                function () {
                    scope.zoomValue = scope.getZoomValue();
                    scope.zoomOffset = scope.getZoomOffset();
                    scope.zoomFieldOffset = scope.getZoomFieldOffset();

                    console.log('w ZV: ' + scope.zoomValue);
                    if (scope.zoomSelected == "") scope.displayZoomLevel(scope.zoomValue);
                });

            scope.$watch(function (scope) { return scope.zoomSelected },
                function () {

                    if (typeof (scope.zoomSelected) === 'object') {

                        if (scope.zoomSelected.time != 0) {
                            scope.zoomLevel = (scope.zoomSelected.time - scope.video.duration) / ((10000 - scope.video.duration) / 100);
                        } else {
                            scope.zoomLevel = 0;
                        }
                    }
                });

            scope.changeZoomLevel = function () {

                if (scope.zoomValue > 0) {
                    scope.zoomSelected = "";
                } else {
                    scope.zoomSelected = scope.ZoomSelectOptions[0];
                }
            }

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

                var style = {
                    width: (scope.video.duration * 100 / scope.zoomValue) + '%',
                    left: (scope.zoomOffset * -100 / scope.video.duration) + '%',
                    'overflow-x': 'hidden'
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

                scope.from = scope.zoomFieldOffset;
                scope.to = scope.zoomFieldOffset + scope.zoomValue;

                var style = {
                    width: (scope.zoomValue * 100 / scope.video.duration) + '%',
                    left: (scope.zoomFieldOffset * 100 / scope.video.duration) + '%'
                };

                return style;
            };

            scope.getZoomClass = function () {

                var width = angular.element('.field').width();
                scope.fieldSmall = (width <= 190);

                return { 'active': scope.field_active, 'small': scope.fieldSmall };
            }

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
                if (scope.video.segments[index - 1]) {
                    scope.video.segments[index - 1].end = segment.end;
                    scope.video.segments.splice(index, 1);
                } else if (scope.video.segments[index + 1]) {
                    scope.video.segments[index + 1].start = segment.start;
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

                console.log(scope.zoomFieldOffset);

                scope.$apply(function () {
                    scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';
                });
            };

            $document.mousemove(function(e) {
                $document.mx = document.all ? window.event.clientX : e.pageX;
                $document.my = document.all ? window.event.clientY : e.pageY;
            });

            // Stop dragging the cursor as soon as the mouse button is
            // released.
            $document.mouseup(function () {
                scope.canMove = false;
                scope.canMoveTimeline = false;
                scope.movingSegment = null;

                scope.player.adapter.setCurrentTime(scope.position / 1000);
                $document.unbind('mousemove', scope.move);
                $document.unbind('mousemove', scope.moveTimeline);
                $document.unbind('mousemove', scope.moveTimeline);
            });

            /**
             * Callback to a mouse-down event initiating a drag of the
             * position handle.
             *
             * @param {Event} event Event that triggered this method.
             */
            scope.drag = function (event) {
                event.preventDefault();
                scope.canMove = true;

                // Register global mouse move callback
                $document.mousemove(scope.move);
            };

            /**
             * Sets the cursor position depending on the mouse coordinates.
             *
             * @param {Event} event Event that triggered this method.
             */
            scope.moveTimeline = function (event) {
                event.preventDefault();
                if (!scope.canMoveTimeline) { return; }

                var track = element.find('.field-of-vision'),
                    shuttle = element.find('.field-of-vision .field'),
                    nx = $document.mx - shuttle.data('dx'),
                    track_width = track.width(),
                    shuttle_width = shuttle.width(),
                    end = track_width - shuttle_width;

                if (nx <= 0) nx = 0;
                if (nx >= end) nx = end;

                var per_display = nx / track_width * 100,
                    per_position = (nx - (shuttle_width/2)) / track_width * 100;

                shuttle.css('left', per_display +'%');

                scope.zoomFieldOffset = (scope.video.duration * per_position) / 100;
                scope.position = (scope.zoomFieldOffset * scope.video.duration) / (scope.video.duration - scope.zoomValue);

                //scope.from = scope.zoomFieldOffset;
                //scope.to = scope.zoomFieldOffset + scope.zoomValue;

                console.log(scope.zoomFieldOffset +' '+ $document.mx +' '+ shuttle.data('dx') +' '+ nx);
            };

            scope.dragTimeline = function (event) {
                event.preventDefault();

                scope.canMoveTimeline = true;

                var track = element.find('.field-of-vision'),
                    shuttle = element.find('.field-of-vision .field');

                shuttle.data('dx', $document.mx - shuttle.offset().left);
                shuttle.data('dy', $document.my - shuttle.offset().top);

                // Register global mouse move callback
                $document.mousemove(scope.moveTimeline);
            }

            /**
             *
             *
             * @param {Event} event Event that triggered this method.
             */
            scope.moveSegment= function (event) {
                event.preventDefault();
                if (!scope.movingSegment) { return; }

                var nx = $document.mx - scope.movingSegment.data('dx') - scope.movingSegment.data('px');

                if (nx <= scope.movingSegment.data('track_left')) nx = scope.movingSegment.data('track_left');
                if (nx >= scope.movingSegment.data('end')) nx = scope.movingSegment.data('end');

                scope.movingSegment.css('left', nx);
                console.log (scope.movingSegment.data('end') +' '+ nx);
            }

            scope.dragSegement = function (event) {
              event.preventDefault();

              scope.movingSegment = true;

              var handle = $(event.currentTarget),
                  track = element.find('.segments').parent();

              handle.data('dx', $document.mx - handle.offset().left);
              handle.data('dy', $document.my - handle.offset().top);
              handle.data('px', handle.parent().offset().left);
              handle.data('py', handle.parent().offset().top);
              handle.data('track_left', (handle.data('px') *-1) + track.offset().left - 4);
              handle.data('track_width', track.width());
              handle.data('shuttle_width', handle.width());
              handle.data('end', track.width() - handle.width() + track.offset().left - handle.parent().offset().left);

              scope.movingSegment = handle;

              console.log ($document.mx +' '+ scope.movingSegment.data('dx'));

              // Register global mouse move callback
              $document.mousemove(scope.moveSegment);
            }

            /**
             * Sets the position marker to the start of the given segment.
             *
             * @param {Object} segment Segment object
             *
            scope.skipToSegment = function (segment) {
                if (!segment.selected) {
                    scope.player.adapter.setCurrentTime(segment.start / 1000);
                }
            };

            /**
             * Marks the given segment as selected.
             *
             * @param {Object} segment Segment object
             *
            scope.selectSegment = function (segment) {
                angular.forEach(scope.video.segments, function (segment) {
                    segment.selected = false;
                });
                segment.selected = true;
            };
            */

            scope.$on('$destroy', function () {
                $document.unbind('mouseup');
                $document.unbind('mousemove');
            });

            scope.init();
        }
    };
}]);
