/**
 * Synchronize.js
 * Version 1.2.1
 *
 *  Copyright (C) 2014 Denis Meyer, calltopower88@googlemail.com
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
(function($) {
    var debug = false;

    var videoIds = [];
    var videoIdsReady = {};
    var videoIdsInit = {};
    var masterVidNumber = 0;
    var masterVideoId;
    var nrOfPlayersReady = 0;

    var lastSynch = 0;
    var synchInterval = 1500; // ms

    // prevent that a slave video lags before even starting to synchronize
    // if a slave video is ahead it will always be synchronized
    var synchGap = 1.0; // s

    // maximum gap that is accepted before seeking (higher playback rate to fill the gap)
    var maxGap = 5.0; // s

    // seek ahead for synchronized displays, as the master continues to play
    // while the slave is seeking
    var seekAhead = 0.25; // s
    var pauseDelayThreshold = seekAhead + 0.05;

    var synchDelayThreshold = 0.0;

    var startClicked = false;

    var checkBuffer = true; // flag whether to check for the video buffers
    var bufferCheckerSet = false;
    var bufferChecker;
    var checkBufferInterval = 1000; // ms
    var playWhenBuffered = false;
    var ignoreNextPause = false;
    var hitPauseWhileBuffering = false;
    var bufferInterval = 2; // s

    var tryToPlayWhenBuffering = true; // flag for trying to play after n seconds of buffering
    var tryToPlayWhenBufferingTimer = null;
    var tryToPlayWhenBufferingMS = 10000;

    var receivedEventLoadeddata = false;
    var receivedEventLoadeddata_interval = null;
    var receivedEventLoadeddata_waitTimeout = 5000; // ms

    var waitingForSync = [];

    var usingFlash = false;

    function log(vals) {
        if (debug && window.console) {
            console.log(vals);
        }
    }

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
            log("SJS: [getVideoObj] Undefined video element id '" + id + "'");
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
            log("SJS: [getVideo] Undefined video element id '" + id + "'");
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
            var id = videojsVideo.id();
            return (id != "") ? id : videojsVideo;
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
            log("SJS: [play] Playing video element id '" + id + "'");
            getVideo(id).play();
            return true;
        } else {
            log("SJS: [play] Undefined video element id '" + id + "'");
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
            log("SJS: [mute] Muting video element id '" + id + "'");
            if (!useVideoJs()) {
                getVideo(id).muted = true;
            } else {
                getVideo(id).volume(0);
            }
        } else {
            log("SJS: [mute] Undefined video element id '" + id + "'");
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
            log("SJS: [pause] Pausing video element id '" + id + "'");
            return getVideo(id).pause();
        } else {
            log("SJS: [pause] Undefined video element id '" + id + "'");
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
            log("SJS: [isPaused] Undefined video element id '" + id + "'");
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
            log("SJS: [getDuration] Undefined video element id '" + id + "'");
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
            log("SJS: [getCurrentTime] Undefined video element id '" + id + "'");
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
        if (id) {
            var duration = getDuration(id);
            if ((duration != -1) && !isNaN(time) && (time >= 0) && (time <= duration)) {
                if (!useVideoJs()) {
                    getVideo(id).currentTime = time;
                } else {
                    getVideo(id).currentTime(time);
                }
                return true;
            } else {
                log("SJS: [setCurrentTime] Could not set time for video element id '" + id + "'");
                setCurrentTime(id, duration);
                return false;
            }
        } else {
            log("SJS: [setCurrentTime] Undefined video element id '" + id + "'");
            return false;
        }
    }

    /**
     * Returns the current playback rate of the video
     *
     * @param id video id
     * @return current time if id is not undefined
     */
    function getPlaybackRate(id) {
        if (id) {
            if (!useVideoJs()) {
                return getVideo(id).playbackRate;
            } else {
                return getVideo(id).playbackRate();
            }
        } else {
            log("SJS: [getPlaybackRate] Undefined video element id '" + id + "'");
            return 1.0;
        }
    }

    /**
     * Sets the playback rate for the video
     *
     * @param id video id
     * @param rate the speed at which the video plays
     * @return true if rate has been set if id is not undefined
     */
    function setPlaybackRate(id, rate) {
        if (id) {
            if (!useVideoJs()) {
                getVideo(id).playbackRate = rate;
            } else {
                getVideo(id).playbackRate(rate);
            }
            return true;
        } else {
            log("SJS: [setPlaybackRate] Undefined video element id '" + id + "'");
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
            log("SJS: [getBufferTimeRange] Undefined video element id '" + id + "'");
            return undefined;
        }
    }

    /**
     * Check whether a video element is in synch with the master
     *
     * @param id video id
     * @return 0 if video element is in synch with the master, a time else
     */
    function getSynchDelay(videoId) {
        var ctMaster = getCurrentTime(masterVideoId); // current time in seconds
        var ct = getCurrentTime(videoId); // current time in seconds
        if ((ctMaster != -1) && (ct != -1) && !isInInterval(ct, ctMaster - synchGap, ctMaster)) {
            return ct - ctMaster; // time difference
        }
        return 0.0; // delay is acceptable
    }

    /**
     * Synchronizes all slaves with the master
     */
    function synchronize() {
        // for all video ids
        for (var i = 0; i < videoIds.length; ++i) {
            // except the master video
            if (videoIds[i] != masterVideoId) {
                var doSeek = false;
                var synchDelay = getSynchDelay(videoIds[i]);
                // if not using flash
                if (!usingFlash) {
                    if (synchDelay > synchDelayThreshold) {
                        if (synchDelay < maxGap) {
                            $(document).trigger("sjs:synchronizing", [getCurrentTime(masterVideoId), videoIds[i]]);
                            // set a slower playback rate for the video to let the master video catch up
                            log("SJS: [synchronize] Decreasing playback rate of video element id '" + videoIds[i] + "' from " + getPlaybackRate(videoIds[i]) + " to " + (getPlaybackRate(masterVideoId) - 0.5));
                            setPlaybackRate(videoIds[i], (getPlaybackRate(masterVideoId) - 0.5));
                        } else {
                            $(document).trigger("sjs:synchronizing", [getCurrentTime(masterVideoId), videoIds[i]]);
                            // set playback rate back to normal
                            setPlaybackRate(videoIds[i], getPlaybackRate(masterVideoId));
                            // pause video shortly
                            pause(videoIds[i]);
                        }
                    } else if (synchDelay < synchDelayThreshold) {
                        if (synchDelay < maxGap) {
                            $(document).trigger("sjs:synchronizing", [getCurrentTime(masterVideoId), videoIds[i]]);
                            // set a faster playback rate for the video to catch up to the master video
                            log("SJS: [synchronize] Increasing playback rate of video element id '" + videoIds[i] + "' from " + getPlaybackRate(videoIds[i]) + " to " + (getPlaybackRate(masterVideoId) + 0.5));
                            setPlaybackRate(videoIds[i], (getPlaybackRate(masterVideoId) + 0.5));
                        } else {
                            // set playback rate back to normal
                            setPlaybackRate(videoIds[i], getPlaybackRate(masterVideoId));
                            // mark for seeking
                            doSeek = true;
                        }
                    }
                    // everything is fine
                    else if (!isPaused(masterVideoId) && !waitingForSync[videoIds[i]]) {
                        // play the video
                        log("SJS: [synchronize] Playing video element id '" + videoIds[i] + "'");
                        play(videoIds[i]);
                    }
                }
                // if using flash
                else if (usingFlash) {
                    if ((Math.abs(synchDelay) > synchDelayThreshold) && (Math.abs(synchDelay) > pauseDelayThreshold)) {
                        doSeek = true;
                    }
                    // everything is fine
                    else if (!isPaused(masterVideoId) && !waitingForSync[videoIds[i]]) {
                        // play the video
                        log("SJS: [synchronize] Playing video element id '" + videoIds[i] + "'");
                        play(videoIds[i]);
                    }
                }
                // if marked for seeking
                if (doSeek) {
                    $(document).trigger("sjs:synchronizing", [getCurrentTime(masterVideoId), videoIds[i]]);
                    log("SJS: [synchronize] Seeking video element id '" + videoIds[i] + "': " + (getCurrentTime(masterVideoId) + seekAhead));
                    if (setCurrentTime(videoIds[i], getCurrentTime(masterVideoId) + seekAhead)) {
                        play(videoIds[i]);
                        if (!isPaused(masterVideoId) && !waitingForSync[videoIds[i]]) {
                            log("SJS: [synchronize] Playing video element id '" + videoIds[i] + "' after seeking");
                            play(videoIds[i]);
                        } else {
                            log("SJS: [synchronize] Pausing video element id '" + videoIds[i] + "' after seeking");
                            pause(videoIds[i]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Pause a video stream for a certain delay
     *
     * @param videoId ID of the video id to synch
     * @param delay delay
     */
    function syncPause(videoId, delay) {
        log("SJS: [syncPause] Synchronizing. Pausing video id '" + videoId + "' for " + (delay / getPlaybackRate(masterVideoId)) + "s");
        waitingForSync[videoId] = true;
        pause(videoId);
        setTimeout(function() {
            waitingForSync[videoId] = false;
            if (!isPaused(masterVideoId)) {
                play(videoId);
                log("SJS: [syncPause] Synchronizing. Continuing to play video id '" + videoId + "' after " + delay + "s");
            } else {
                log("SJS: [syncPause] Still pausing video id '" + videoId + "' after " + delay + "s, as the master video is paused, too");
            }
        }, ((delay / getPlaybackRate(masterVideoId)) * 1000));
    }

    /**
     * Registers master events on all slaves
     */
    function registerEvents() {
        if (allVideoIdsInitialized()) {
            var masterPlayer = getVideoObj(masterVideoId);

            usingFlash = $("#" + masterVideoId + "_flash_api").length != 0;

            masterPlayer.on("play", function() {
                log("SJS: Master received 'play' event");
                $(document).trigger("sjs:masterPlay", [getCurrentTime(masterVideoId)]);
                hitPauseWhileBuffering = false;
                if (!bufferCheckerSet && checkBuffer) {
                    bufferCheckerSet = true;
                    setBufferChecker();
                }
                for (var i = 0; i < videoIds.length; ++i) {
                    if (videoIds[i] != masterVideoId) {
                        play(videoIds[i]);
                        mute(videoIds[i]);
                    }
                }
            });

            masterPlayer.on("pause", function() {
                log("SJS: Master received 'pause' event");
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

            masterPlayer.on("ratechange", function() {
                log("SJS: Master received 'ratechange' event");
                $(document).trigger("sjs:masterPlaybackRateChanged", [getPlaybackRate(masterVideoId)]);
                for (var i = 0; i < videoIds.length; ++i) {
                    if (videoIds[i] != masterVideoId) {
                        setPlaybackRate(videoIds[i], getPlaybackRate(masterVideoId));
                    }
                }
            });

            masterPlayer.on("ended", function() {
                log("SJS: Master received 'ended' event");
                $(document).trigger("sjs:masterEnded", [getDuration(masterVideoId)]);
                hitPauseWhileBuffering = true;
                for (var i = 0; i < videoIds.length; ++i) {
                    if (videoIds[i] != masterVideoId) {
                        synchronize();
                        pause(videoIds[i]);
                    }
                }
            });

            masterPlayer.on("timeupdate", function() {
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
                            /*
							if (paused || isPaused(masterVideoId)) {
                            pause(videoIds[i]);
                            }
							*/
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
        bufferChecker = window.setInterval(function() {
            var allBuffered = true;

            var currTime = getCurrentTime(masterVideoId);

            for (var i = 0; i < videoIds.length; ++i) {
                var bufferedTimeRange = getBufferTimeRange(videoIds[i]);
                if (bufferedTimeRange) {
                    var duration = getDuration(videoIds[i]);
                    var currTimePlusBuffer = getCurrentTime(videoIds[i]) + bufferInterval;
                    var buffered = false;
                    for (var j = 0;
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
        if (id != "") {
            getVideoObj(id).on("loadeddata", function() {
                receivedEventLoadeddata = true;
                if (func) {
                    func();
                }
            });
        } else {
            log("SJS: [doWhenDataLoaded] Undefined video element id '" + id + "'");
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

    /**
     * Initial play
     */
    function initialPlay() {
        var myPlayer = this;
        for (var i = 0; i < videoIds.length; ++i) {
            pause(videoIds[i]);
        }
        startClicked = true;
    }

    /**
     * Initial pause
     */
    function initialPause() {
        var myPlayer = this;
        for (var i = 0; i < videoIds.length; ++i) {
            pause(videoIds[i]);
        }
        startClicked = false;
    }

    /**
     * Stop try to play when buffering timer
     */
    function stopTryToPlayWhenBufferingTimer() {
        if (tryToPlayWhenBufferingTimer != null) {
            window.clearInterval(tryToPlayWhenBufferingTimer);
            tryToPlayWhenBufferingTimer = null;
        }
    }

    /**
     * Main
     *
     * @param masterVidNumber [0, n-1]
     * @param videoId1OrMediagroup
     * @param videoId2
     * @param videoId3 - videoIdN [optional]
     */
    $.synchronizeVideos = function(playerMasterVidNumber, videoId1OrMediagroup, videoId2) {
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

                    getVideoObj(videoIds[i]).ready(function() {
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
                    getVideoObj(videoIds[i]).ready(function() {
                        var playerName = getVideoId(this);

                        videoIdsReady[playerName] = true;
                        doWhenDataLoaded(playerName, function() {
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

                        receivedEventLoadeddata_interval = window.setInterval(function() {
                            if (!receivedEventLoadeddata) {
                                for (var i = 0; i < videoIds.length; ++i) {
                                    getVideoObj(videoIds[i]).trigger("loadeddata");
                                }
                            } else {
                                window.clearInterval(receivedEventLoadeddata_interval);
                                receivedEventLoadeddata_interval = null;
                            }
                        }, receivedEventLoadeddata_waitTimeout);
                    });
                }
            }
        } else {
            log("SJS: Not enough videos");
            $(document).trigger("sjs:notEnoughVideos", []);
        }

        if (tryToPlayWhenBuffering) {
            $(document).on("sjs:buffering", function(e) {
                log("SJS: Received 'sjs:buffering' event");
                tryToPlayWhenBufferingTimer = setInterval(function() {
                    if (allVideoIdsInitialized() && !hitPauseWhileBuffering) {
                        play(masterVideoId);
                    }
                }, tryToPlayWhenBufferingMS);
            });
            $(document).on("sjs:bufferedAndAutoplaying", function(e) {
                log("SJS: Received 'sjs:bufferedAndAutoplaying' event");
                stopTryToPlayWhenBufferingTimer();
            });
            $(document).on("sjs:bufferedButNotAutoplaying", function(e) {
                log("SJS: Received 'sjs:bufferedButNotAutoplaying' event");
                stopTryToPlayWhenBufferingTimer();
            });
        }

        $(document).on("sjs:play", function(e) {
            log("SJS: Received 'sjs:play' event");
            if (allVideoIdsInitialized()) {
                play(masterVideoId);
            }
        });
        $(document).on("sjs:pause", function(e) {
            log("SJS: Received 'sjs:pause' event");
            if (allVideoIdsInitialized()) {
                pause(masterVideoId);
            }
        });
        $(document).on("sjs:setCurrentTime", function(e, time) {
            log("SJS: Received 'sjs:setCurrentTime' event");
            if (allVideoIdsInitialized()) {
                setCurrentTime(masterVideoId, time);
            }
        });
        $(document).on("sjs:debug", function(e, _debug) {
            log("SJS: Received 'sjs:debug' event");
            debug = _debug;
        });
        $(document).on("sjs:synchronize", function(e) {
            log("SJS: Received 'sjs:synchronize' event");
            if (allVideoIdsInitialized()) {
                synchronize();
            }
        });
        $(document).on("sjs:startBufferChecker", function(e) {
            log("SJS: Received 'sjs:startBufferChecker' event");
            if (!bufferCheckerSet) {
                window.clearInterval(bufferChecker);
                bufferCheckerSet = true;
                setBufferChecker();
            }
        });
        $(document).on("sjs:stopBufferChecker", function(e) {
            log("SJS: Received 'sjs:stopBufferChecker' event");
            window.clearInterval(bufferChecker);
            bufferCheckerSet = false;
        });
    }
})(jQuery);
