angular.module('adminNg.services')
.service('PlayerAdapter', [function () {

    /**
     * Possible player status
     * @readonly
     * @enum {number}
     */
    this.STATUS = {
        INITIALIZING: 0,
        LOADING: 1,
        SEEKING: 2,
        PAUSED: 3,
        PLAYING: 4,
        ENDED: 5,
        ERROR_NETWORK: 6,
        ERROR_UNSUPPORTED_MEDIA: 7
    };

    /**
     * Player adapter event
     * @readonly
     * @enum {string}
     */
    this.EVENTS = {
        PLAY: 'play',
        PAUSE: 'pause',
        SEEKING: 'seeking',
        READY: 'ready',
        TIMEUPDATE: 'timeupdate',
        ERROR: 'error',
        ENDED: 'ended',
        CAN_PLAY: 'canplay',
        DURATION_CHANGE: 'durationchange'
    };

    this.eventMapping = function () {
        var EventMapping = function () {
            var mapping = {};

            this.map = function (apiEvent, nativeEvent) {
                mapping[apiEvent] = nativeEvent;
                return this;
            };

            this.resolveNativeName = function (apiEvent) {
                var nativeEvent = mapping[apiEvent];
                if (nativeEvent === undefined) {
                    throw Error('native event for [' + apiEvent + '] not found');
                }
                return nativeEvent;
            };
        };

        return new EventMapping();
    };
}]);
