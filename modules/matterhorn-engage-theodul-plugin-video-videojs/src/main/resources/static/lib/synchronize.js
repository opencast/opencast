/**
 * Copyright 2009-2011 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
(function($) {
    var player_master;
    var player_slave;
    var lastSynch = Date.now();
    var synchInterval = 2000;
    var vid1Init = false;
    var vid2Init = false;

    function pause(id) {
        if (id) {
            return videojs(id).pause();
        } else {
            return -1;
        }
    }

    function getDuration(id) {
        if (id) {
            return videojs(id).duration();
        } else {
            return -1;
        }
    }

    function getCurrentTime(id) {
        if (id) {
            return videojs(id).currentTime();
        } else {
            return -1;
        }
    }

    function seek(id, time) {
        if (id && time && (time >= 0) && (time <= getDuration(id))) {
            videojs(id).currentTime(time);
            return true;
        } else {
            videojs(id).currentTime(getDuration(id));
            return false;
        }
    }

    function isInInterval(num, lower, upper) {
        if (num && lower && upper && (lower <= upper)) {
            return ((num >= lower) && (num <= upper));
        } else {
            return false;
        }
    }

    function synch(id1, id2) {
        if (id1 && id2) {
            var ct1 = getCurrentTime(id1);
            var ct2 = getCurrentTime(id2);
            if ((ct1 != -1) && (ct2 != -1) && !isInInterval(ct2, ct1 - 2, ct1 + 2)) { // currentTime in seconds!
                if (!seek(id2, ct1 + 1)) { // seek to "+1" because of the video.js buffering
                    pause(id2);
                }
            }
        }
    }

    var synchronizeVideos_helper = function(videoId1, videoId2) {
        player_master.on("play", function() {
            player_slave.play();
            player_slave.volume(0);
            synch(videoId1, videoId2);
        });

        player_master.on("pause", function() {
            synch(videoId1, videoId2);
            player_slave.pause();
        });

        player_master.on("ended", function() {
            synch(videoId1, videoId2);
            player_slave.pause();
        });

        player_slave.on("ended", function() {
            player_slave.pause();
        });

        player_master.on("timeupdate", function() {
            var now = Date.now();
            var slave_paused = player_slave.paused();
            if (player_master.paused() || ((now - lastSynch) > synchInterval)) {
                synch(videoId1, videoId2);
                lastSynch = now;
                if (slave_paused) {
                    player_slave.pause();
                }
            }
        });
    }

    $.synchronizeVideos = function(videoId1, videoId2, vid1Master) {
        vid1Master = vid1Master || false;
        if (videoId1 && videoId2) {
            videojs(videoId1).ready(function() {
                vid1Init = true;
                if (vid1Master) {
                    player_master = this;
                } else {
                    player_slave = this;
                }
                if (vid2Init) {
                    synchronizeVideos_helper(videoId1, videoId2);
                }
            });
            videojs(videoId2).ready(function() {
                vid2Init = true;
                if (vid1Master) {
                    player_slave = this;
                } else {
                    player_master = this;
                }
                if (vid1Init) {
                    synchronizeVideos_helper(videoId1, videoId2);
                }
            });
        }
    }
})(jQuery);
