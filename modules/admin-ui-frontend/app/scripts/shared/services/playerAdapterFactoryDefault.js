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

angular.module('adminNg.services')
.factory('PlayerAdapterFactoryDefault', ['PlayerAdapter', function (PlayerAdapter) {
  var Factory = function () {
    /**
         * A default implementation of an adapter. Its purpose is to
         * track the state and delegate events from / to specific adapter
         * implementations.
         *
         * Usage: Instantiate an instance of this DefaultAdapter and copy
         * its capabilities to your adapter implementation by calling
         * #extend(this).
         */
    var DefaultAdapter = function (targetElement) {
      // keep a reference to this, for callbacks.
      var me = this, eventMapping = PlayerAdapter.eventMapping();

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

      // The state of adapter implementations must be delegated here.
      this.state = {
        /**
                 * The current player status
                 * @inner
                 * @type {module:player-adapter.PlayerAdapter.STATUS}
                 */
        status: PlayerAdapter.STATUS.INITIALIZING,
        /**
                 * Define if a play request has be done when the player was not ready
                 * @inner
                 * @type {Boolean}
                 */
        waitToPlay: false,
        /**
                 * Define if a the player has been initialized
                 * @inner
                 * @type {Boolean}
                 */
        initialized: false
      };

      /**
             * Registers all default events to the html 5 standard implementations.
             * Just override events from the specific adapter if needed.
             */
      this.registerDefaultListeners = function () {
        // register listeners
        targetElement.addEventListener('canplay', me.canPlay);

        targetElement.addEventListener('durationchange', me.canPlay);

        targetElement.addEventListener('play', function () {
          if (!me.state.initialized) {
            return;
          }

          me.state.status = PlayerAdapter.STATUS.PLAYING;
        });


        targetElement.addEventListener('playing', function () {
          me.state.status = PlayerAdapter.STATUS.PLAYING;
        });

        targetElement.addEventListener('pause', function () {
          if (!me.state.initialized) {
            return;
          }

          me.state.status = PlayerAdapter.STATUS.PAUSED;
        });

        targetElement.addEventListener('ended', function () {
          me.state.status = PlayerAdapter.STATUS.ENDED;
        });

        targetElement.addEventListener('seeking', function () {
          me.state.oldStatus = me.state.status;
          me.state.status = PlayerAdapter.STATUS.SEEKING;
        });

        targetElement.addEventListener('error', function () {
          me.state.status = PlayerAdapter.STATUS.ERROR_NETWORK;
        });
      };

      // =========================
      // ADAPTER API
      // =========================

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

      /**
             * Query whether the adapter is ready to play
             */
      this.ready = function () {
        return me.state.initialized;
      };

      /**
             * Play the video
             */
      this.play = function () {
        // Can the player start now?
        switch (me.state.status) {
        case PlayerAdapter.STATUS.LOADING:
          me.state.waitToPlay = true;
          break;
        default:
          // If yes, we play it
          targetElement.play();
          me.state.status = PlayerAdapter.STATUS.PLAYING;
          me.state.status.waitToPlay = false;
          break;
        }
      };

      this.canPlay = function () {
        // If duration is still not valid
        if (isNaN(me.getDuration()) || targetElement.readyState < 1) {
          return;
        }

        if (!me.state.initialized) {
          me.state.initialized = true;
        }

        if (me.state.waitToPlay) {
          me.play();
        }
      };

      /**
             * Pause the video
             */
      this.pause = function () {
        targetElement.pause();
      };

      /**
             * Set the current time of the video
             * @param {double} time The time to set in seconds
             */
      this.setCurrentTime = function (time) {
        if (time < 0) {
          time = 0;
        } else if (time > me.getDuration()) {
          time = me.getDuration();
        }
        targetElement.currentTime = time;
      };

      /**
             * Get the current time of the video
             */
      this.getCurrentTime = function () {
        return targetElement.currentTime;
      };

      /**
             * Returns the current time as an object containing hours, minutes, seconds and milliseconds.
             * @returns {{hours: number, minutes: number, seconds: number, milliseconds: number}}
             */
      this.getCurrentTimeObject = function () {
        var currentTimeMillis = this.getCurrentTime() * 1000,
            hours = Math.floor(currentTimeMillis / (3600 * 1000)),
            minutes = Math.floor((currentTimeMillis % (3600 * 1000)) / 60000),
            seconds = Math.floor((currentTimeMillis % (60000) / 1000)),
            milliseconds = Math.floor((currentTimeMillis % 1000));

        return {
          hours: hours,
          minutes: minutes,
          seconds: seconds,
          milliseconds: milliseconds
        };
      };

      /**
             * Get the URI of the currently displayed source
             */
      this.getCurrentSource = function () {
        return targetElement.currentSrc;
      };

      /**
             * Get the video duration
             */
      this.getDuration = function () {
        return targetElement.duration;
      };

      /**
             * Get the player status
             */
      this.getStatus = function () {
        return me.state.status;
      };

      this.toggleMute = function () {
        me.muted(! me.muted());
      };

      /**
             * Turns audio on or off and returns current status
             * @param {boolean} status if Audio is mute or not, if not set only status is returned
             * @returns {boolean} muted status of the player
             */
      this.muted = function (status) {
        if (status !== undefined) {
          targetElement.muted = status;
        }
        return targetElement.muted;
      };

      /**
             * Set and get the volume of the player
             * @param {int} volume volume of the player from 0 (mute) to 100 (max),
             *              if not set only returns current volume
             * @returns {int} value from 0 (mute) to 100 (max)
             */
      this.volume = function (volume) {
        if (volume !== undefined) {
          if (volume === 0) {
            me.muted(true);
          } else {
            me.muted(false);
          }
          targetElement.volume = volume / 100.0;
        }
        return parseInt(targetElement.volume * 100);
      };

      /**
             * Copies the API's default implementation methods to the target.
             */
      this.extend = function (target) {
        target.addListener = me.addListener;
        target.ready = me.ready;
        target.play = me.play;
        target.canPlay = me.canPlay;
        target.pause = me.pause;
        target.getCurrentSource = me.getCurrentSource;
        target.setCurrentTime = me.setCurrentTime;
        target.getCurrentTime = me.getCurrentTime;
        target.getFramerate = me.getFramerate;
        target.getCurrentTimeObject  = me.getCurrentTimeObject;
        target.getDuration = me.getDuration;
        target.getStatus = me.getStatus;
        target.muted = me.muted;
        target.volume = me.volume;
      };
      return this;
    };

    this.create = function (targetElement) {
      return new DefaultAdapter(targetElement);
    };
  };
  return new Factory();
}]);
