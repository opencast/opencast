/**
 * Synchronize.js
 * Version 1.1.1
 *
 * Copyright 2013-2014 Denis Meyer
 */
(function ($) {
    var videoIds = [];
    var videoIdsReady = {};
    var videoIdsInit = {};
    var masterVidNumber = 0;
    var masterVideoId;
    var nrOfPlayersReady = 0;

    var lastSynch = 0;
    var synchInterval = 2000; // ms
    var synchGap = 1.0; // s

    var startClicked = false;

    var checkBuffer = true; // flag whether to check for the video buffers
    var bufferCheckerSet = false;
    var bufferChecker;
    var checkBufferInterval = 1000; // ms
    var playWhenBuffered = false;
    var ignoreNextPause = false;
    var hitPauseWhileBuffering = false;
    var bufferInterval = 1.5; // s

    var tryToPlayWhenBuffering = true; // flag for trying to play after N seconds of buffering
    var tryToPlayWhenBufferingTimer = null;
    var tryToPlayWhenBufferingMS = 10000;

    /**
     * Checks whether a number is in an interval [lower, upper]
     *
     * @param num the number to check
     * @param lower lower check
     * @param upper upper check
     * @return true if num is in [lower, upper]
     */
    function isInInterval(num, lower, upper) {
        if (!isNaN(num) && !isNaN(lower) && !isNaN(upper) && (lower <= upper)) {
            return ((num >= lower) && (num <= upper));
        } else {
            return false;
        }
    }

    /**
     * Returns the video object
     *
     * @param id the video element id
     * @return the video object
     */
    function getVideoObj(id) {
        if (id) {
            if (!useVideoJs()) {
                return $("#" + id);
            } else {
                return videojs(id);
            }
        } else {
            return undefined;
        }
    }

    /**
     * Returns the video
     *
     * @param id the video element id
     * @return the video object
     */
    function getVideo(id) {
        if (id) {
            if (!useVideoJs()) {
                return getVideoObj(id).get(0);
            } else {
                return videojs(id);
            }
        } else {
            return undefined;
        }
    }

    /**
     * Returns whether videojs is being used
     *
     * @return true when video.js is being used
     */
    function useVideoJs() {
        return !(typeof videojs === "undefined");
    }

    /**
     * Returns a video.js id
     *
     * @param videojsVideo the video.js video object
     * @return video.js id if videojsVideo is not undefined and video.js is being used
     */
    function getVideoId(videojsVideo) {
        if (useVideoJs() && videojsVideo) {
            return videojsVideo.Q;
        } else {
            return videojsVideo;
        }
    }

    /**
     * Play the video
     *
     * @param id video id
     * @return true if id is not undefined and video plays
     */
    function play(id) {
        if (id) {
            getVideo(id).play();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Mute the video
     *
     * @param id video id
     * @return true if id is not undefined
     */
    function mute(id) {
        if (id) {
            if (!useVideoJs()) {
                getVideo(id).muted = true;
            } else {
                getVideo(id).volume(0);
            }
        } else {
            return undefined;
        }
    }

    /**
     * Pause video
     *
     * @param id video id
     * @return true if id is not undefined
     */
    function pause(id) {
        if (id) {
            return getVideo(id).pause();
        } else {
            return false;
        }
    }

    /**
     * Check whether video is paused
     *
     * @param id video id
     * @return true when id is not undefined and video is paused
     */
    function isPaused(id) {
        if (id) {
            if (!useVideoJs()) {
                return getVideo(id).paused;
            } else {
                return getVideo(id).paused();
            }
        } else {
            return false;
        }
    }

    /**
     * Returns the video duration
     *
     * @param id video id
     * @return video duration if id is not undefined
     */
    function getDuration(id) {
        if (id) {
            if (!useVideoJs()) {
                return getVideo(id).duration;
            } else {
                return getVideo(id).duration();
            }
        } else {
            return -1;
        }
    }

    /**
     * Returns the current time in the video
     *
     * @param id video id
     * @return current time if id is not undefined
     */
    function getCurrentTime(id) {
        if (id) {
            if (!useVideoJs()) {
                return getVideo(id).currentTime;
            } else {
                return getVideo(id).currentTime();
            }
        } else {
            return -1;
        }
    }

    /**
     * Sets the current time in the video
     *
     * @param id video id
     * @param time the time to set
     * @return true if time has been set if id is not undefined
     */
    function setCurrentTime(id, time) {
        var duration = getDuration(id);
        if (id && (duration != -1) && !isNaN(time) && (time >= 0) && (time <= duration)) {
            if (!useVideoJs()) {
                getVideo(id).currentTime = time;
            } else {
                getVideo(id).currentTime(time);
            }
            return true;
        } else {
            setCurrentTime(id, duration);
            return false;
        }
    }

    /**
     * Returns the buffer timerange
     *
     * @param id video id
     * @return buffer timeranmge if id is not undefined
     */
    function getBufferTimeRange(id) {
        if (id) {
            if (!useVideoJs()) {
                return getVideo(id).buffered;
            } else {
                return getVideo(id).buffered();
            }
        } else {
            return undefined;
        }
    }

    /**
     * Synchronizes all slaves with the master
     */
    function synchronize() {
        var ct1 = getCurrentTime(masterVideoId);
        var ct2;
        for (var i = 0; i < videoIds.length; ++i) {
            if (videoIds[i] != masterVideoId) {
                ct2 = getCurrentTime(videoIds[i]);
                // currentTime in seconds!
                if ((ct1 != -1) && (ct2 != -1) && !isInInterval(ct2, ct1 - synchGap, ct1)) {
                    $(document).trigger("sjs:synchronizing", [ct1, videoIds[i]]);
                    if (!setCurrentTime(videoIds[i], ct1)) {
                        // pause(videoIds[i]);
                    } else {
                        play(videoIds[i]);
                    }
                }
            }
        }
    }

    /**
     * Registers master events on all slaves
     */
    function registerEvents() {
        if (allVideoIdsInitialized()) {
            var masterPlayer = getVideoObj(masterVideoId);

            masterPlayer.on("play", function () {
                $(document).trigger("sjs:masterPlay", [getCurrentTime(masterVideoId)]);
                hitPauseWhileBuffering = false;
                if (!bufferCheckerSet && checkBuffer) {
                    bufferCheckerSet = true;
                    setBufferChecker();
                }
                for (var i = 0; i < videoIds.length; ++i) {
                    if (videoIds[i] != masterVideoId) {
                        getVideoObj(videoIds[i]).on("play", function () {
                            mute(videoIds[i]);
                        });
                        play(videoIds[i]);
                    }
                }
            });

            masterPlayer.on("pause", function () {
                $(document).trigger("sjs:masterPause", [getCurrentTime(masterVideoId)]);
                hitPauseWhileBuffering = !ignoreNextPause && playWhenBuffered;
                ignoreNextPause = ignoreNextPause ? !ignoreNextPause : ignoreNextPause;
                for (var i = 0; i < videoIds.length; ++i) {
                    if (videoIds[i] != masterVideoId) {
                        pause(videoIds[i]);
                        synchronize();
                    }
                }
            });

            masterPlayer.on("ended", function () {
                $(document).trigger("sjs:masterEnded", [getDuration(masterVideoId)]);
                hitPauseWhileBuffering = true;
                for (var i = 0; i < videoIds.length; ++i) {
                    if (videoIds[i] != masterVideoId) {
                        synchronize();
                        pause(videoIds[i]);
                    }
                }
            });

            masterPlayer.on("timeupdate", function () {
                $(document).trigger("sjs:masterTimeupdate", [getCurrentTime(masterVideoId)]);
                hitPauseWhileBuffering = true;
                var now = Date.now();
                if (((now - lastSynch) > synchInterval) || isPaused(masterVideoId)) {
                    lastSynch = now;
                    var video;
                    var paused;
                    for (var i = 0; i < videoIds.length; ++i) {
                        if (videoIds[i] != masterVideoId) {
                            mute(videoIds[i]);
                            paused = isPaused(videoIds[i]);
                            synchronize();
                            if (paused || isPaused(masterVideoId)) {
                                pause(videoIds[i]);
                            }
                        }
                    }
                }
            });
        } else {
            for (var i = 0; i < videoIds.length; ++i) {
                pause(videoIds[i]);
            }
        }
    }

    /**
     * Checks every checkBufferInterval ms whether all videos have a buffer to continue playing.
     * If not:
     *   - player pauses automatically
     *   - starts automatically playing when enough has been buffered
     */
    function setBufferChecker() {
        bufferChecker = window.setInterval(function () {
            var allBuffered = true;

            var currTime = getCurrentTime(masterVideoId);

            for (var i = 0; i < videoIds.length; ++i) {
                var bufferedTimeRange = getBufferTimeRange(videoIds[i]);
                if (bufferedTimeRange) {
                    var duration = getDuration(videoIds[i]);
                    var currTimePlusBuffer = getCurrentTime(videoIds[i]) + bufferInterval;
                    var buffered = false;
                    for (j = 0;
                        (j < bufferedTimeRange.length) && !buffered; ++j) {
                        currTimePlusBuffer = (currTimePlusBuffer >= duration) ? duration : currTimePlusBuffer;
                        if (isInInterval(currTimePlusBuffer, bufferedTimeRange.start(j), bufferedTimeRange.end(j))) {
                            buffered = true;
                        }
                    }
                    allBuffered = allBuffered && buffered;
                } else {
                    // Do something?
                }
            }

            if (!allBuffered) {
                playWhenBuffered = true;
                ignoreNextPause = true;
                for (var i = 0; i < videoIds.length; ++i) {
                    pause(videoIds[i]);
                }
                hitPauseWhileBuffering = false;
                $(document).trigger("sjs:buffering", []);
            } else if (playWhenBuffered && !hitPauseWhileBuffering) {
                playWhenBuffered = false;
                play(masterVideoId);
                hitPauseWhileBuffering = false;
                $(document).trigger("sjs:bufferedAndAutoplaying", []);
            } else if (playWhenBuffered) {
                playWhenBuffered = false;
                $(document).trigger("sjs:bufferedButNotAutoplaying", []);
            }
        }, checkBufferInterval);
    }

    /**
     * Sets a master video id
     *
     * @param playerMasterVideoNumber the video number of the master video
     */
    function setMasterVideoId(playerMasterVideoNumber) {
        masterVidNumber = (playerMasterVideoNumber < videoIds.length) ? playerMasterVideoNumber : 0;
        masterVideoId = videoIds[masterVidNumber];
        $(document).trigger("sjs:masterSet", [masterVideoId]);
    }

    /**
     * Waits for data being loaded and calls a function
     *
     * @param id video id
     * @param func function to call after data has been loaded
     */
    function doWhenDataLoaded(id, func) {
        if (id && func) {
            //getVideoObj(id).on("loadeddata", function () {
                func(); // TODO
            //});
        }
    }

    /**
     * Checks whether all videos have been initialized
     *
     * @return true if all videos have been initialized, false else
     */
    function allVideoIdsInitialized() {
        if (!useVideoJs()) {
            return (nrOfPlayersReady == videoIds.length);
        } else {
            for (var i = 0; i < videoIds.length; ++i) {
                if (!videoIdsInit[videoIds[i]]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Checks whether all videos are ready
     *
     * @return true if all videos are ready, false else
     */
    function allVideoIdsReady() {
        if (!useVideoJs()) {
            return (nrOfPlayersReady == videoIds.length); // TODO
        } else {
            for (var i = 0; i < videoIds.length; ++i) {
                if (!videoIdsReady[videoIds[i]]) {
                    return false;
                }
            }
            return true;
        }
    }

    function initialPlay() {
        var myPlayer = this;
        // pause(getVideoId(this));
        for (var i = 0; i < videoIds.length; ++i) {
            pause(videoIds[i]);
        }
        startClicked = true;
    }

    function initialPause() {
        var myPlayer = this;
        // pause(getVideoId(this));
        for (var i = 0; i < videoIds.length; ++i) {
            pause(videoIds[i]);
        }
        startClicked = false;
    }

    function stopTryToPlayWhenBufferingTimer() {
        if (tryToPlayWhenBufferingTimer != null) {
            window.clearInterval(tryToPlayWhenBufferingTimer);
            tryToPlayWhenBufferingTimer = null;
        }
    }

    /**
     * @param masterVidNumber [0, n-1]
     * @param videoId1OrMediagroup
     * @param videoId2
     * @param videoId3 - videoIdN [optional]
     */
    $.synchronizeVideos = function (playerMasterVidNumber, videoId1OrMediagroup, videoId2) {
        var validIds = true;

        // check for mediagroups
        if ((arguments.length == 2)) {
            var videosInMediagroup = $("video[mediagroup=\"" + videoId1OrMediagroup + "\"]");
            for (var i = 0; i < videosInMediagroup.length; ++i) {
                var l = videoIds.length;
                videoIds[l] = videosInMediagroup[i].getAttribute("id");
                // hack for video.js: Remove added id string
                var videoJsIdAddition = "_html5_api";
                videoIds[l] = (useVideoJs() && (videoIds[l].indexOf(videoJsIdAddition) != -1)) ? (videoIds[l].substr(0, videoIds[l].length - videoJsIdAddition.length)) : videoIds[l];
                videoIdsReady[videoIds[i - 1]] = false;
                videoIdsInit[videoIds[i - 1]] = false;
            }
        } else {
            masterVidNumber = playerMasterVidNumber;
            for (var i = 1; i < arguments.length; ++i) {
                // check whether ids exist/are valid
                validIds = validIds && arguments[i] && ($("#" + arguments[i]).length);
                if (!validIds) {
                    $(document).trigger("sjs:invalidId", [arguments[i]]);
                } else {
                    videoIds[videoIds.length] = arguments[i];
                    videoIdsReady[videoIds[i - 1]] = false;
                    videoIdsInit[videoIds[i - 1]] = false;
                }
            }
        }

        if (validIds && (videoIds.length > 1)) {
            if (!useVideoJs()) {
                for (var i = 0; i < videoIds.length; ++i) {
                    $(document).trigger("sjs:idRegistered", [videoIds[i]]);
                    var plMVN = playerMasterVidNumber;

                    getVideoObj(videoIds[i]).on("play", initialPlay);
                    getVideoObj(videoIds[i]).on("pause", initialPause);

                    getVideoObj(videoIds[i]).ready(function () {
                        ++nrOfPlayersReady;

                        if (allVideoIdsInitialized()) {
                            setMasterVideoId(plMVN);
                            for (var i = 0; i < videoIds.length; ++i) {
                                getVideoObj(videoIds[i]).off("play", initialPlay);
                                getVideoObj(videoIds[i]).off("pause", initialPause);
                            }
                            registerEvents();
                            if (startClicked) {
                                play(masterVideoId);
                            }
                            $(document).trigger("sjs:allPlayersReady", []);
                        }
                    });
                }
            } else {
                for (var i = 0; i < videoIds.length; ++i) {
                    $(document).trigger("sjs:idRegistered", [videoIds[i]]);
                    var plMVN = playerMasterVidNumber;

                    getVideoObj(videoIds[i]).on("play", initialPlay);
                    getVideoObj(videoIds[i]).on("pause", initialPause);
                    getVideoObj(videoIds[i]).ready(function () {
                        var playerName = getVideoId(this);

                        videoIdsReady[playerName] = true;
                        doWhenDataLoaded(playerName, function () {
                            videoIdsInit[playerName] = true;

                            $(document).trigger("sjs:playerLoaded", [playerName]);

                            if (allVideoIdsInitialized()) {
                                setMasterVideoId(plMVN);
                                for (var i = 0; i < videoIds.length; ++i) {
                                    getVideoObj(videoIds[i]).off("play", initialPlay);
                                    getVideoObj(videoIds[i]).off("pause", initialPause);
                                }
                                registerEvents();
                                if (startClicked) {
                                    play(masterVideoId);
                                }
                                $(document).trigger("sjs:allPlayersReady", []);
                            }
                        });
                    });
                }
            }
        } else {
            $(document).trigger("sjs:notEnoughVideos", []);
        }

        if (tryToPlayWhenBuffering) {
            $(document).on("sjs:buffering", function (e) {
                tryToPlayWhenBufferingTimer = setInterval(function () {
                    if (allVideoIdsInitialized() && !hitPauseWhileBuffering) {
                        play(masterVideoId);
                    }
                }, tryToPlayWhenBufferingMS);
            });
            $(document).on("sjs:bufferedAndAutoplaying", function (e) {
                stopTryToPlayWhenBufferingTimer();
            });
            $(document).on("sjs:bufferedButNotAutoplaying", function (e) {
                stopTryToPlayWhenBufferingTimer();
            });
        }

        $(document).on("sjs:play", function (e) {
            if (allVideoIdsInitialized()) {
                play(masterVideoId);
            }
        });
        $(document).on("sjs:pause", function (e) {
            if (allVideoIdsInitialized()) {
                pause(masterVideoId);
            }
        });
        $(document).on("sjs:setCurrentTime", function (e, time) {
            if (allVideoIdsInitialized()) {
                setCurrentTime(masterVideoId, time);
            }
        });
        $(document).on("sjs:synchronize", function (e) {
            if (allVideoIdsInitialized()) {
                synchronize();
            }
        });
        $(document).on("sjs:startBufferChecker", function (e) {
            if (!bufferCheckerSet) {
                window.clearInterval(bufferChecker);
                bufferCheckerSet = true;
                setBufferChecker();
            }
        });
        $(document).on("sjs:stopBufferChecker", function (e) {
            window.clearInterval(bufferChecker);
            bufferCheckerSet = false;
        });
    }
})(jQuery);
