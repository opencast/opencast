angular.module('adminNg.directives')
.directive('videoPlayer', ['PlayerAdapter', 'PlayerAdapterRepository', 'VideoService', '$timeout',
    function (PlayerAdapter, PlayerAdapterRepository, VideoService, $timeout) {

    return {
        restrict: 'A',
        priority: 10,
        templateUrl: 'shared/partials/player.html',
        scope: {
            player   : '=',
            video    : '=',
            controls : '@'
        },
        link: function (scope, element, attrs) {
            function loadPlayerAdapter(element) {
                scope.player.adapter = PlayerAdapterRepository.
                    findByAdapterTypeAndElementId(
                        attrs.adapter.toUpperCase(),
                        element
                    );

                scope.playing = false;
                scope.positionStart = false;
                scope.positionEnd = false;
                scope.time = {
                    hours: 0,
                    minutes: 0,
                    seconds: 0,
                    milliseconds: 0
                };

                scope.player.adapter.addListener(PlayerAdapter.EVENTS.PAUSE, function () {
                    scope.$apply(function () {
                        scope.playing = false;
                    });
                });

                scope.player.adapter.addListener(PlayerAdapter.EVENTS.PLAY, function () {
                    scope.$apply(function () {
                        scope.playing = true;
                    });
                });

                scope.player.adapter.addListener(PlayerAdapter.EVENTS.TIMEUPDATE, function () {
                    // Expose the time to the view
                    scope.time = scope.player.adapter.getCurrentTimeObject();

                    scope.$apply();
                });
            }

            // Check if the player element is loaded,
            function checkPlayerElement(tries, callback) {
                if (tries > 0) {
                    var element = angular.element('#' + attrs.playerRef)[0];
                    if (element) {
                        callback(element);
                    } else {
                        // Wait 100ms before to retry
                        scope.checkTimeout = $timeout(function() {
                            checkPlayerElement(tries - 1, callback);
                        }, 100);
                    }
                }
            }

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.checkTimeout);
            });

            scope.previousFrame = function () {
                scope.player.adapter.previousFrame();
            };

            scope.nextFrame = function () {
                scope.player.adapter.nextFrame();
            };

            scope.previousSegment = function () {
                var segment = VideoService.getCurrentSegment(scope.player, scope.video),
                    index = scope.video.segments.indexOf(segment);
                if (scope.video.segments[index-1]) {
                    segment = scope.video.segments[index-1];
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
                }
                else {
                    scope.player.adapter.play();
                }
            };

            // Check for the player (10 times) before to load the adapter
            checkPlayerElement(10, loadPlayerAdapter);
        }
    };
}]);
