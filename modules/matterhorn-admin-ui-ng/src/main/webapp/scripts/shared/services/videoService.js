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
        this.getPreviousActiveSegment = function (player, video) {
            var matchingSegment,
                previousSegment,
                position = player.adapter.getCurrentTime() * 1000;
            angular.forEach(video.segments, function (segment) {
                if ((segment.start <= position) && (segment.end >= position)) {
                    matchingSegment = previousSegment;
                }
                if (!segment.deleted) {
                  previousSegment = segment;
                }
            });
            return matchingSegment;
        };
        // get the next active segment including the current segment.
        this.getNextActiveSegment = function (player, video) {
            var matchingSegment,
                foundCurrentSegment,
                position = player.adapter.getCurrentTime() * 1000;
            angular.forEach(video.segments, function (segment) {
                if ((segment.start <= position) && (segment.end >= position)) {
                    foundCurrentSegment = true;
                }
                if (foundCurrentSegment && ! matchingSegment && !segment.deleted) {
                  matchingSegment = segment;
                }
            });
            return matchingSegment;
        };
    };

    return new VideoService();
}]);
