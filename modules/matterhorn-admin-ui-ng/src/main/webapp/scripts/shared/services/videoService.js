/**
 * Utility service bridging video and timeline functionality.
 */
angular.module('adminNg.services')
.factory('VideoService', [
    function () {

    var VideoService = function () {
        this.getCurrentSegment = function (player, video) {
            var matchingSegment,
                position = player.adapter.getCurrentTime() * 1000;
            angular.forEach(video.segments, function (segment) {
                if ((segment.start <= position) && (segment.end >= position)) {
                    matchingSegment = segment;
                }
            });
            return matchingSegment;
        };
    };

    return new VideoService();
}]);
