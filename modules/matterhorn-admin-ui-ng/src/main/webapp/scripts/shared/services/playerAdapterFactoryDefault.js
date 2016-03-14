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
            var me = this;

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
                targetElement.currentTime = time;
            };

            /**
             * Get the current time of the video
             */
            this.getCurrentTime = function () {
                return targetElement.currentTime;
            };

            /**
             * Takes the player to the next frame. The step is calculated by 1/framerate.
             * @throws Error if called at end of player
             * @throws Error if called in status PLAYING
             */
            this.nextFrame = function () {

                var currentTime = me.getCurrentTime();

                if (me.state.status === PlayerAdapter.STATUS.PLAYING) {
                    throw new Error('In state playing calls to previousFrame() are not possible.');
                }

                if (currentTime >= me.getDuration()) {
                    throw new Error('At end of video calls to nextFrame() are not possible.');
                }

                me.setCurrentTime(currentTime + 1 / me.getFramerate());
            };

            /**
             * Takes the player to the previous frame. The step is calculated by 1/framerate.
             * @throws Error if called at start of player
             * @throws Error if called in status PLAYING
             */
            this.previousFrame = function () {

                var currentTime = me.getCurrentTime();

                if (me.state.status === PlayerAdapter.STATUS.PLAYING) {
                    throw new Error('In state playing calls to previousFrame() are not possible.');
                }

                if (currentTime === 0) {
                    throw new Error('At start of video calls to previosFrame() are not possible.');
                }

                me.setCurrentTime(currentTime - 1 / me.getFramerate());
            };


            /**
             * TODO find a way to find out framerate
             *
             * @returns {number}
             */
            this.getFramerate = function () {
                return 30;
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

            /**
             * Copies the API's default implementation methods to the target.
             */
            this.extend = function (target) {
                target.play = me.play;
                target.canPlay = me.canPlay;
                target.pause = me.pause;
                target.setCurrentTime = me.setCurrentTime;
                target.getCurrentTime = me.getCurrentTime;
                target.nextFrame = me.nextFrame;
                target.previousFrame = me.previousFrame;
                target.getFramerate = me.getFramerate;
                target.getCurrentTimeObject  = me.getCurrentTimeObject;
                target.getDuration = me.getDuration;
                target.getStatus = me.getStatus;
            };
            return this;
        };

        this.create = function (targetElement) {
            return new DefaultAdapter(targetElement);
        };
    };
    return new Factory();
}]);
