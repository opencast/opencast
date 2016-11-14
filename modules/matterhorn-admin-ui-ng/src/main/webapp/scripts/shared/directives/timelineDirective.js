angular.module('adminNg.directives')
.directive('adminNgTimeline', ['PlayerAdapter', '$document', 'VideoService', '$timeout',
function (PlayerAdapter, $document, VideoService, $timeout) {
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
                scope.selectSegment(segment);

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
             * Display the current zoom level ms value into human readable value
             * in the existing drop down > overriding the display HTML
             *
             * @param {Number} the ms value of the current zoom level
             */
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
                return (10000 - scope.video.duration) / 100 * scope.zoomLevel +
                    scope.video.duration;
            };

            scope.$watch(function (scope) { return scope.video.duration },
               function () {
                   scope.zoomValue = scope.getZoomValue();
                   scope.zoomOffset = scope.getZoomOffset();
                   scope.zoomFieldOffset = scope.getZoomFieldOffset();
               });

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

            /**
             * On change of zoom range slider updates the appropriate zoom select option
             */
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

                    // Load the waveform image into the zoom timeline if it exists
                    jss.set('.field-of-vision::before', {
                        'background-image': 'url(' + track.waveform + ')'
                    });
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

            /**
             * The display classes to use for the zoom range element.
             *
             * @return {Object} object with {class}: {boolean} values for CSS classes.
             */
            scope.getZoomClass = function () {

                var field = angular.element('.field'),
                    width = field.width();
                scope.fieldSmall = (width <= 190);

                return { 'active': (field.data('active') == true), 'small': scope.fieldSmall };
            }

            /**
             * Removes the given segment.
             *
             * The previous or, failing that, the next segment will take up
             * the space of the given segment.
             *
             * @param {Object} segment Segment object
             */
            scope.mergeSegment = function (event, segment) {
                event.preventDefault();
                event.stopPropagation();

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
             * @param {Event} click event for checkbox link - stop the propogation
             * @param {Object} segment object on which the deleted variable will change
             */
            scope.toggleSegment = function (event, segment) {
                event.preventDefault();
                event.stopPropagation();

                segment.deleted = !segment.deleted;
            };

            /**
             * Split the segment at this position
             *
             * The previous or, failing that, the next segment will take up
             * the space of the given segment.
             *
             * @param {Object} segment Segment object
             */
            scope.splitSegment = function () {
              var segment = VideoService.getCurrentSegment(scope.player, scope.video),
                  position = Math.floor(scope.player.adapter.getCurrentTime() * 1000),
                  newSegment = angular.copy(segment);

              // Shrink original segment
              segment.end = position;

              // Add additional segment containing the second half of the
              // original segment.
              newSegment.start = position;

              // Deselect the previous segment as the cursor is at the start
              // of the new one.
              delete segment.selected;

              // Insert new segment
              scope.video.segments.push(newSegment);

              // Sort array by start attribute
              scope.video.segments.sort(function (a, b) {
                  return a.start - b.start;
              });
            };

            /**
             * Catch all method to track the mouse movement on the page,
             * to calculate the movement of elements properly.
             */
            $document.mousemove(function(e) {
                $document.mx = document.all ? window.event.clientX : e.pageX;
                $document.my = document.all ? window.event.clientY : e.pageY;
            });


            /**
             * Mouseup event handler to finish up the move action for:
             * 1. Timeline handle
             * 2. Zoom field
             * 3. Segment start handle
             */
            $document.mouseup(function () {

              // Timeline mouse events
                if (scope.canMove) {
                  scope.canMoveTimeline = false;
                  element.find('.field-of-vision .field').removeClass('active');
                  scope.player.adapter.setCurrentTime(scope.position / 1000);
                }

              // Timeline position - handle
                if (scope.canMove) {
                  scope.canMove = false;
                  element.find('#cursor .handle').data('active', false);
                  scope.player.adapter.setCurrentTime(scope.position / 1000);

                  // show small cut button below timeline handle
                  element.find('#cursor .arrow_box').show();

                  if (scope.timer) $timeout.cancel( scope.timer );
                  scope.timer = $timeout(
                        function() {
                            // hide cut window
                            element.find('#cursor .arrow_box').hide();
                        },
                        60000 //  1 min
                    );
                }

              // Segment start handle
                if (scope.movingSegment) {

                  var track = element.find('.segments'),
                      topTrack = track.parent(),
                      segment = scope.movingSegment.data('segment'),
                      index = scope.video.segments.indexOf(segment);

                  var pxPosition = scope.movingSegment.parent().offset().left + parseInt(scope.movingSegment.css('left'),10) - topTrack.offset().left + 3;
                      position = Math.floor((pxPosition / track.width() * scope.video.duration) + scope.zoomFieldOffset);

                  if (position < 0) position = 0;
                  if (position >= scope.video.duration) position = scope.video.duration;

                  if (position >= segment.end) {
                    // pulled start point of segment past end of start
                    // so we flip it
                    segment.start = segment.end;
                    segment.end = position;
                  } else {
                    segment.start = position;
                  }

                  // update the segments around the one that was changed
                  if (index - 1 >= 0) {

                      var before = scope.video.segments[index - 1];
                      before.end = segment.start;

                      if (before.end - before.start <= 0) {
                        // empty segment
                        segment.start = before.start;
                        scope.video.segments.splice(index - 1, 1);
                      }
                  }

                  // Sort array by start attribute
                  scope.video.segments.sort(function (a, b) {
                      return a.start - b.start;
                  });
                  index = scope.video.segments.indexOf(segment);

                  if (index + 1 < scope.video.segments.length) {
                      var after = scope.video.segments[index + 1];
                      after.start = segment.end;

                      if (after.end - after.start <= 0) {
                        // empty segment
                        segment.end = after.end;
                        scope.video.segments.splice(index + 1, 1);
                      }
                  }

                  scope.movingSegment.removeClass('active');
                  scope.movingSegment.css('left', '-4px');
                  scope.movingSegment = null;

                  // Sort array by start attribute
                  scope.video.segments.sort(function (a, b) {
                      return a.start - b.start;
                  });

                  if (segment.end - segment.start <= 4) {
                      // i'm really small so should probably not exist anymore
                      scope.mergeSegment(segment);
                  }
                }

                // Clean-up of mousemove handlers
                $document.unbind('mousemove', scope.movePlayHead);
                $document.unbind('mousemove', scope.moveTimeline);
                $document.unbind('mousemove', scope.moveSegment);
            });

            /**
             * Clicking on the playtrack moves the timeline handle to that position.
             *
             * @param {Event} event Event that triggered this method.
             */
            scope.clickPlayTrack = function (event) {
                event.preventDefault();
                var el = $(event.target);

                if (el.attr('id') == 'cursor-track') {

                  var position = (event.clientX - el.offset().left) / el.width() * scope.zoomValue + scope.zoomFieldOffset;

                  // Limit position to the length of the video
                  if (position > scope.video.duration) {
                      position = scope.video.duration;
                  }
                  if (position < 0) {
                      position = 0;
                  }

                  scope.player.adapter.setCurrentTime(position / 1000);

                  // show small cut button below timeline handle
                  element.find('#cursor .arrow_box').show();

                  if (scope.timer) $timeout.cancel( scope.timer );
                  scope.timer = $timeout(
                        function() {
                            // hide cut window
                            element.find('#cursor .arrow_box').hide();
                        },
                        60000 //  1 min
                    );
                }
            }

            /**
             * Sets the timeline handle cursor position depending on the mouse coordinates.
             *
             * @param {Event} the mousemove event details.
             */
            scope.movePlayHead = function (event) {
                event.preventDefault();
                if (!scope.canMove) { return; }

                var track = element.find('.timeline-track'),
                    handle = element.find('#cursor .handle'),
                    position = ($document.mx - handle.data('dx') - track.offset().left) / track.width() * scope.video.duration;

                // Limit position to the length of the video
                if (position > scope.video.duration) {
                    position = scope.video.duration;
                }
                if (position < 0) {
                    position = 0;
                }

                scope.position = position;
                scope.$apply(function () {
                    scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';
                });
            };

            /**
             * The mousedown event handler to initiate the dragging of the
             * timeline handle.
             * Adds a listener on mousemove (movePlayHead)
             *
             * @param {Event} the mousedown events that inits the mousemove actions.
             */
            scope.dragPlayhead = function (event) {
                event.preventDefault();
                scope.canMove = true;

                var handle = element.find('#cursor .handle');

                handle.data('dx', $document.mx - handle.offset().left);
                handle.data('dy', $document.my - handle.offset().top);
                handle.addClass('active');

                // Register global mouse move callback
                $document.mousemove(scope.movePlayHead);
            };

            /**
             * Sets the zoomFieldOffset corresponding to the position of the zoom field
             * in the field-of-vision as the mouse drag event unfolds.
             *
             * @param {Event} the mousemove event details.
             */
            scope.moveTimeline = function (event) {
                event.preventDefault();
                if (!scope.canMoveTimeline) { return; }

                var track = element.find('.field-of-vision'),
                    shuttle = element.find('.field-of-vision .field'),
                    nx = $document.mx - shuttle.data('dx');

                if (nx <= 0) nx = 0;
                if (nx >= shuttle.data('end')) nx = shuttle.data('end');

                var percentage = nx / shuttle.data('track_width') * 100;

                shuttle.css('left', percentage +'%');
                scope.zoomFieldOffset = (scope.video.duration * percentage) / 100;
                scope.position = (scope.zoomFieldOffset * scope.video.duration) / (scope.video.duration - scope.zoomValue);

                if (isNaN(scope.position) || (scope.position < 0)) scope.position = 0;
                if (scope.position > scope.video.duration) scope.position = scope.video.duration;

                scope.from = scope.zoomFieldOffset;
                scope.to = scope.zoomFieldOffset + scope.zoomValue;
                shuttle.find(':first-child').html( scope.formatMilliseconds(scope.from) );
                shuttle.find(':last-child').html( scope.formatMilliseconds(scope.to) );
            };

            /**
             * The mousedown event handler to initiate the dragging of the
             * zoom timeline field.
             * Adds a listener on mousemove (moveTimeline)
             *
             * @param {Event} the mousedown events that inits the mousemove actions.
             */
            scope.dragTimeline = function (event) {
                event.preventDefault();

                scope.canMoveTimeline = true;

                var track = element.find('.field-of-vision'),
                    shuttle = element.find('.field-of-vision .field');

                shuttle.data('dx', $document.mx - shuttle.offset().left);
                shuttle.data('dy', $document.my - shuttle.offset().top);
                shuttle.data('track_width', track.width());
                shuttle.data('shuttle_width', shuttle.width());
                shuttle.data('end', track.width() - shuttle.width());
                shuttle.data('active', true);

                // Register global mouse move callback
                $document.mousemove(scope.moveTimeline);
            }

            /**
             * Updates the segment start position indicator according to the mouse movement.
             *
             * @param {Event} the mousemove event details.
             */
            scope.moveSegment= function (event) {
                event.preventDefault();
                if (!scope.movingSegment) { return; }

                var nx = $document.mx - scope.movingSegment.data('dx') - scope.movingSegment.data('px');

                if (nx <= scope.movingSegment.data('track_left')) nx = scope.movingSegment.data('track_left');
                if (nx >= scope.movingSegment.data('end')) nx = scope.movingSegment.data('end');

                scope.movingSegment.css('left', nx);
            }

            /**
             * The mousedown event handler for the segment start handle of a segment.
             * Adds a listener on mousemove (moveSegment)
             *
             * @param {Event} the mousedown events that inits the mousemove actions.
             */
            scope.dragSegement = function (event, segment) {
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
              handle.data('end', track.width() + track.offset().left - handle.parent().offset().left);
              handle.data('segment', segment);
              handle.addClass('active');

              scope.movingSegment = handle;

              // Register global mouse move callback
              $document.mousemove(scope.moveSegment);
            }

            /**
             * Sets the position marker to the start of the given segment.
             *
             * @param {Object} segment Segment object
             */
            scope.skipToSegment = function (event, segment) {
                event.preventDefault();

                if (!segment.selected) {
                    scope.player.adapter.setCurrentTime(segment.start / 1000);
                    scope.position = segment.start;
                    scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';
                    scope.selectSegment(segment);
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

                // cancel timer for the small cut button below timeline handle
                if (scope.timer) $timeout.cancel( scope.timer );
            });

        }
    };
}]);
