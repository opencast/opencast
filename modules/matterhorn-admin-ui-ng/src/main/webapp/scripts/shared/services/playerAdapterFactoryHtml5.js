angular.module('adminNg.services')
.factory('PlayerAdapterFactoryHTML5', ['PlayerAdapter', 'PlayerAdapterFactoryDefault', function (PlayerAdapter, PlayerAdapterFactoryDefault) {

    var PlayerAdapterFactoryHTML5 = function () {

        /**
         * Implementation of the player adapter for the HTML5 native player
         * @constructor
         * @alias module:player-adapter-HTML5.PlayerAdapterHTML5
         * @augments {module:player-adapter.PlayerAdapter}
         * @param {DOMElement} targetElement DOM Element representing the player
         */
        var PlayerAdapterHTML5 = function (targetElement) {
            'use strict';

            var defaultAdapter, self = this;

            this.eventMapping = PlayerAdapter.eventMapping();

            this.eventMapping
                .map(PlayerAdapter.EVENTS.PAUSE, 'pause')
                .map(PlayerAdapter.EVENTS.PLAY, 'play')
                .map(PlayerAdapter.EVENTS.READY, 'ready')
                .map(PlayerAdapter.EVENTS.TIMEUPDATE, 'timeupdate')
                .map(PlayerAdapter.EVENTS.DURATION_CHANGE, 'durationchange')
                .map(PlayerAdapter.EVENTS.CAN_PLAY, 'canplay');

            // Check if the given target Element is valid
            if (typeof targetElement === 'undefined' || targetElement === null) {
                throw 'The given target element must not be null and have to be a valid HTMLElement!';
            }

            /**
             * Id of the player adapter
             * @inner
             * @type {String}
             */
            this.id = 'PlayerAdapter' + targetElement.id;

            // =========================
            // INITIALIZATION
            // =========================
            
            /**
             * Register a listener listening to events of type. The event name will be translated from
             * API event (@see PlayerAdapter) to native events of the player implementation.
             *
             * @param type
             * @param listener
             */
            this.addListener = function (type, listener) {
                targetElement.addEventListener(self.eventMapping.resolveNativeName(type), listener);
            };

            // Instantiate DefaultAdapter and copy its methods to this adapter.
            defaultAdapter = PlayerAdapterFactoryDefault.create(targetElement);
            defaultAdapter.extend(this);
            defaultAdapter.registerDefaultListeners();
        };


        this.create = function (targetElement) {
            return new PlayerAdapterHTML5(targetElement);
        };
    };
    return new PlayerAdapterFactoryHTML5();

}]);
