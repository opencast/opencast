/*
 * jQuery mhPlayer by Denis Meyer (denmeyer@uni-osnabrueck.de)
 *
 * Copyright 2009-2013 The Regents of the University of California
 * Licensed under the Educational Community License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
(function ($) {
    /*****************************************************************************
     * Synchronize two Matterhorn html5 players
     ****************************************************************************/
    var lastSynch = Date.now();

    function getDuration(id) {
        if (id) {
            return $(id).get(0).duration;
        } else {
            return -1;
        }
    }

    function getCurrentTime(id) {
        if (id) {
            return $(id).get(0).currentTime;
        } else {
            return -1;
        }
    }

    function seek(id, time) {
        if (id && time) {
            $(id).get(0).currentTime = time;
        }
    }

    /*****************************************************************************
     * Matterhorn html5 player
     ****************************************************************************/
    $.fn.mhPlayer = function (options) {
        /***************************************************************************
         * default options
         **************************************************************************/
        var defaults = {
            theme: 'default',
            preview: '',
            autoplay: 'false',
            subtitle: '',
            controls: 'true',
            fps: 25,
            duration: 0
        };
        // match default options and given options
        var options = $.extend(defaults, options);
        return $.each(this, function (i, val) {
            /*************************************************************************
             * strings
             ************************************************************************/
            var strPlay = 'Play';
            var strPause = 'Pause';
            var strVolumeOn = 'Switch Volume On';
            var strVolumeOff = 'Switch Volume Off';
            var strFullscreen = 'Fullscreen';

            var strPrevFrame = "Previous Frame";
            var strNextFrame = "Next Frame";

            var strPrevMarker = "Previous Marker";
            var strNextMarker = "Next Marker";

            var strPlayPrePost = "Play at current playhead with 2s pre roll and 2s post roll excluding removed items";

            var strSplit = "split at current time";

            /*************************************************************************
             * class names
             ************************************************************************/
            var mainClass = 'mhVideoPlayer';
            var classVideoControls = 'video-controls';
            var classVideoPlay = 'video-play';
            var classVideoSeek = 'video-seek';
            var classVideoTimer = 'video-timer';
            var classFullscreen = 'video-fullscreen';
            var classVolumeBox = 'volume-box';
            var classVolumeSlider = 'volume-slider';
            var classVolumeButton = 'volume-button';

            var classNextFrame = "video-next-frame";
            var classPreviousFrame = "video-prev-frame";

            var classPreviousMarker = "video-previous-marker";
            var classNextMarker = "video-next-marker";

            var classPlayPrePost = "video-play-pre-post";

            var classSplit = "video-split-button";

            /*************************************************************************
             * variables
             ************************************************************************/
            var mhVideo = $(val);
            var video_duration = 0;
            var currenttime = 0;
            var video_volume = 1;
            var onSeekTimerInterval = 150;
            var seeksliding;
            var autoplay = options.autoplay == 'true';
            var controls = options.controls == 'true';
            if (options.preview != '') {
                mhVideo.prop("poster", options.preview);
            }
            var video_wrap = $('<div></div>').addClass(mainClass).addClass(options.theme).prop("id", "videoHolder");
            var video_controls = '';
            if (controls) {
                video_controls =
                    '<div class="' + classVideoControls + ' ' + $(val).attr('id') + '">' +
                    '<div class="videocontrolsDiv">' +
                    '<a class="' + classVideoPlay + '" title="' + strPlay + '"></a>' +
                    '<a class="' + classPreviousMarker + '" title="' + strPrevMarker + '"></a>' +
                    '<a class="' + classPreviousFrame + '" title="' + strPrevFrame + '"></a>' +
                    '<a class="' + classSplit + '" title="' + strSplit + '"></a>' +
                    '<a class="' + classPlayPrePost + '" title="' + strPlayPrePost + '"></a>' +
                    '<a class="' + classNextFrame + '" title="' + strNextFrame + '"></a>' +
                    '<a class="' + classNextMarker + '" title="' + strNextMarker + '"></a>' +
                    '</div>' +
                    '<div class="' + classVideoTimer + '"></div>' +
                    '<div class="' + classVolumeBox + '">' +
                    '<div class="' + classVolumeSlider + '"></div>' +
                    '<a class="' + classVolumeButton + '" title="' + strVolumeOn + '"></a>' +
                    '</div>' +
                    '<div class="' + classVideoSeek + '"></div>' +
                    '<div class="' + classFullscreen + '"></div>' +
                    '</div>';
            }
            mhVideo.wrap(video_wrap);
            mhVideo.after(video_controls);
            var video_container = mhVideo.parent('.' + mainClass);
            var video_controls = $('.' + classVideoControls, video_container);
            var play_btn = $('.' + classVideoPlay, video_container);
            var fullscreen_btn = $('.' + classFullscreen, video_container);
            var video_seek = $('.' + classVideoSeek, video_container);
            var video_timer = $('.' + classVideoTimer, video_container);
            var volume = $('.' + classVolumeSlider, video_container);
            var volume_btn = $('.' + classVolumeButton, video_container);

            var next_btn = $('.' + classNextFrame, video_container);
            var prev_btn = $('.' + classPreviousFrame, video_container);

            var next_marker_btn = $('.' + classNextMarker, video_container);
            var prev_marker_btn = $('.' + classPreviousMarker, video_container);

            var split_btn = $('.' + classSplit, video_container);
            var prePost_btn = $('.' + classPlayPrePost, video_container);

            video_controls.hide();

            mhVideo.on("canplay", initialSeek);

            /*************************************************************************
             * utitlity functions
             ************************************************************************/

            /**
             * formats given time in seconds to mm:ss
             *
             * @param seconds
             *          time to format in seconds
             * @return given time in seconds formatted to hh:MM:ss.mmmm
             */
            var formatTime = function (seconds) {
                if (typeof seconds == "string") {
                    seconds = parseFloat(seconds);
                }

                var h = "00";
                var m = "00";
                var s = "00";
                if (!isNaN(seconds) && (seconds >= 0)) {
                    var tmpH = Math.floor(seconds / 3600);
                    var tmpM = Math.floor((seconds - (tmpH * 3600)) / 60);
                    var tmpS = Math.floor(seconds - (tmpH * 3600) - (tmpM * 60));
                    var tmpMS = seconds - tmpS;
                    h = (tmpH < 10) ? "0" + tmpH : (Math.floor(seconds / 3600) + "");
                    m = (tmpM < 10) ? "0" + tmpM : (tmpM + "");
                    s = (tmpS < 10) ? "0" + tmpS : (tmpS + "");
                    ms = tmpMS + "";
                    var indexOfSDot = ms.indexOf(".");
                    if (indexOfSDot != -1) {
                        ms = ms.substr(indexOfSDot + 1, ms.length);
                    }
                    ms = ms.substr(0, 4);
                    while (ms.length < 4) {
                        ms += "0";
                    }
                }
                return h + ":" + m + ":" + s + "." + ms;
            };

            /**
             * updates the time on the video time field
             */
            var updateTime = function () {
                if (playerIsReady()) {
                    currenttime = mhVideo.prop('currentTime');
                    video_duration = mhVideo.prop('duration');
                    video_duration = video_duration != "Infinity" ? video_duration : options.duration;
                    video_timer.text(formatTime(currenttime) + "/" + formatTime(video_duration));
                }
            }

            /*************************************************************************
             * player functions
             ************************************************************************/

            /**
             * checks whether player is in ready state
             *
             * @return a boolean value if player is in ready state
             */
            var playerIsReady = function () {
                return mhVideo.prop('readyState') >= mhVideo.prop('HAVE_CURRENT_DATA');
            }

            /**
             * raw play
             */
            var play = function () {
                mhVideo.get(0).play();
            }

            /**
             * raw pause
             */
            var pause = function () {
                mhVideo.get(0).pause();
            }

            /*************************************************************************
             * ui changes
             ************************************************************************/

            /**
             * sets the ui play button
             */
            var setUiPlayButton = function () {
                play_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-play"
                    }
                });
                $('.' + classVideoPlay, video_controls).attr("title", strPlay);
            }

            /**
             * sets the ui pause button
             */
            var setUiPauseButton = function () {
                play_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-pause"
                    }
                });
                $('.' + classVideoPlay, video_controls).attr("title", strPause);
            }

            /**
             * sets the ui fullscreen button
             */
            var setUiFullscreenButton = function () {
                fullscreen_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-arrow-4-diag"
                    }
                });
                $('.' + classFullscreen, video_controls).attr("title", strFullscreen);
            }

            /**
             * sets the ui volume on button
             */
            var setUiVolumeOnButton = function () {
                volume_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-volume-on"
                    }
                });
                $('.' + classVolumeButton, video_controls).attr("title", strVolumeOff);
            }

            /**
             * sets the ui volume off button
             */
            var setUiVolumeOffButton = function () {
                volume_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-volume-off"
                    }
                });
                $('.' + classVolumeButton, video_controls).attr("title", strVolumeOn);
            }

            var setUiNextFrameButton = function () {
                next_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-arrowstop-1-e"
                    }
                });
            }

            var setUiPrevFrameButton = function () {
                prev_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-arrowstop-1-w"
                    }
                });
            }

            var setUIPrevMarkerButton = function () {
                prev_marker_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-seek-first"
                    }
                });
            }

            var setUINextMarkerButton = function () {
                next_marker_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-seek-end"
                    }
                });
            }

            var setUISplitButton = function () {
                split_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-scissors"
                    }
                });
            }

            var setUIPrePostButton = function () {
                prePost_btn.button({
                    text: false,
                    icons: {
                        primary: "ui-icon-refresh"
                    }
                })
            }

            /*************************************************************************
             * on-events
             ************************************************************************/

            /**
             * on play/pause
             */
            var onPlayPause = function () {
                if (mhVideo.prop('paused') == false) {
                    pause();
                } else {
                    play();
                }
            };

            /**
             * on seek update
             */
            var onSeekUpdate = function () {
                updateTime();
                currenttime = mhVideo.prop('currentTime');
                if (!seeksliding) {
                    video_seek.slider('value', currenttime);
                }
            };

            /**
             * on volume on/off
             */
            var onVolumeOnOff = function () {
                if (mhVideo.prop('muted')) {
                    mhVideo.prop('muted', false);
                    volume.slider('value', video_volume);
                    setUiVolumeOnButton()
                } else {
                    mhVideo.prop('muted', true);
                    volume.slider('value', '0');
                    setUiVolumeOffButton();
                }
            };

            function onNextFrame() {
                onSeekFrames(1);
            }

            function onPrevFrame() {
                onSeekFrames(-1);
            }

            var intv = null;
            var nr_of_frames = 0;
            var mouseDown = false;

            function initFrameButtonsMouseEvents() {
                next_btn.mousedown(function () {
                    mouseDown = true;
                    onSeekFrames(1);
                });
                prev_btn.mousedown(function () {
                    mouseDown = true;
                    onSeekFrames(-1);
                });
                next_btn.mouseup(function () {
                    mouseDown = false;
                });
                prev_btn.mouseup(function () {
                    mouseDown = false;
                });
            }

            function updateScrubberPosition() {
                var fps = options.fps;
                //var currentFrames = Math.round(video.currentTime * fps);
                var currentFrames = mhVideo.prop('currentTime') * fps;
                var newPos = (currentFrames + nr_of_frames) / fps;
                newPos = newPos + 0.00001; // FIXES A SAFARI SEEK ISSUE. myVdieo.currentTime = 0.04 would give SMPTE 00:00:00:00 wheras it should give 00:00:00:01

                mhVideo.prop('currentTime', newPos); // TELL THE PLAYER TO GO HERE
                updateTime();
                if (!mouseDown) {
                    window.clearInterval(intv);
                }
            }

            function onSeekFrames(_nr_of_frames) {
                nr_of_frames = _nr_of_frames;
                if (mhVideo.prop('paused') == false) {
                    pause();
                }
                if (mouseDown) {
                    intv = window.setInterval(updateScrubberPosition, 100);
                } else {
                    updateScrubberPosition();
                }
            }

            /*************************************************************************
             * binds
             ************************************************************************/
            mhVideo.bind('play', function () {
                setUiPauseButton();
            });
            mhVideo.bind('pause', function () {
                setUiPlayButton();
            });
            mhVideo.bind('ended', function () {
                pause();
                setUiPlayButton();
            });
            fullscreen_btn.hide();
            mhVideo.bind('timeupdate', onSeekUpdate);

            /*************************************************************************
             * slider
             ************************************************************************/

            /**
             * initial seek for the seek slider
             */
            var initialSeek = function () {
                if (playerIsReady()) {
                    updateTime();
                    video_seek.slider({
                        value: 0,
                        step: 0.01,
                        orientation: "horizontal",
                        range: "min",
                        max: video_duration,
                        animate: true,
                        slide: function (e, ui) {
                            seeksliding = true;
                            video_timer.text(formatTime(ui.value) + "/" + formatTime(video_duration));
                        },
                        stop: function (e, ui) {
                            seeksliding = false;
                            mhVideo.prop("currentTime", ui.value);
                            onSeekUpdate();
                        }
                    });
                    if (controls) {
                        video_controls.show();
                    }
                } else {
                    setTimeout(initialSeek, onSeekTimerInterval);
                }
            };

            volume.slider({
                value: 1,
                orientation: "vertical",
                range: "min",
                max: 1,
                step: 0.05,
                animate: true,
                slide: function (e, ui) {
                    mhVideo.prop('muted', false);
                    video_volume = ui.value;
                    mhVideo.prop('volume', video_volume);
                    setUiVolumeOnButton();
                }
            });

            /*************************************************************************
             * build ui
             ************************************************************************/
            initialSeek();
            setUiPlayButton();
            setUiFullscreenButton();
            setUiVolumeOnButton();
            setUiNextFrameButton();
            setUiPrevFrameButton();
            setUINextMarkerButton();
            setUIPrevMarkerButton();
            setUISplitButton();
            setUIPrePostButton();
            pause();
            // disable browser-specific controls
            mhVideo.removeAttr('controls');

            /*************************************************************************
             * clicks
             ************************************************************************/
            setUiPlayButton();
            play_btn.click(onPlayPause);
            mhVideo.click(onPlayPause);
            volume_btn.click(onVolumeOnOff);

            prev_btn.click(onPrevFrame);
            next_btn.click(onNextFrame);
            initFrameButtonsMouseEvents();

            /*************************************************************************
             * misc
             ************************************************************************/
            if (autoplay) {
                play();
            }
        });
    };
})(jQuery);
