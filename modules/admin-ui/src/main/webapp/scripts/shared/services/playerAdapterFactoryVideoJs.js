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

angular.module('adminNg.services')
.factory('PlayerAdapterFactoryVIDEOJS', ['PlayerAdapter', 'PlayerAdapterFactoryDefault', function (PlayerAdapter, PlayerAdapterFactoryDefault) {

    var PlayerAdapterFactoryVideoJs = function () {

        /**
         * Implementation of the player adapter for the HTML5 native player
         * @constructor
         * @alias module:player-adapter-HTML5.PlayerAdapterVideoJs
         * @augments {module:player-adapter.PlayerAdapter}
         * @param {DOMElement} targetElement DOM Element representing the player
         */
        var PlayerAdapterVideoJs = function (targetElement) {
            'use strict';

            var defaultAdapter, eventMapping = PlayerAdapter.eventMapping();

            eventMapping
                .map(PlayerAdapter.EVENTS.PAUSE, 'pause')
                .map(PlayerAdapter.EVENTS.PLAY, 'play')
                .map(PlayerAdapter.EVENTS.READY, 'ready')
                .map(PlayerAdapter.EVENTS.TIMEUPDATE, 'timeupdate')
                .map(PlayerAdapter.EVENTS.DURATION_CHANGE, 'durationchange')
                .map(PlayerAdapter.EVENTS.CAN_PLAY, 'canplay')
                .map(PlayerAdapter.EVENTS.VOLUMECHANGE, 'volumechange');

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

            function initPlayer() {
                var myPlayer = videojs(targetElement);
                myPlayer.controls(false);
                myPlayer.controlBar.hide();
                myPlayer.dimensions('auto', 'auto');
                return myPlayer;
            }

            /**
             * Register a listener listening to events of type. The event name will be translated from
             * API event (@see PlayerAdapter) to native events of the player implementation.
             *
             * @param type
             * @param listener
             */
            this.addListener = function (type, listener) {
                targetElement.addEventListener(eventMapping.resolveNativeName(type), listener);
            };


            // =========================
            // INITIALIZATION
            // =========================

            // Instantiate DefaultAdapter and copy its methods to this adapter.
            defaultAdapter = PlayerAdapterFactoryDefault.create(targetElement);
            defaultAdapter.extend(this);
            defaultAdapter.registerDefaultListeners();

            initPlayer();

        };


        this.create = function (targetElement) {
            return new PlayerAdapterVideoJs(targetElement);
        };
    };
    return new PlayerAdapterFactoryVideoJs();

}]);
