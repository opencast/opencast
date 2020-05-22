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
/*jslint browser: true, nomen: true*/
/*global define, CustomEvent*/
define(["jquery"], function($) {
    "use strict";

    var entityMap = {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': '&quot;',
        "'": '&#39;',
        "/": '&#x2F;'
    };

    function Utils() {
        // nothing to see here
    }

    Utils.prototype.escapeHtml = function(string) {
        return String(string).replace(/[&<>"'\/]/g, function(s) {
            return entityMap[s];
        });
    }

    Utils.prototype.getAspectRatioWidth = function(originalWidth, originalHeight, height) {
        var width = Math.round(height * originalWidth / originalHeight);
        return width;
    }

    Utils.prototype.getAspectRatioHeight = function(originalWidth, originalHeight, width) {
        var height = Math.round(originalHeight / originalWidth * width);
        return height;
    }

    Utils.prototype.escapeRegExp = function(string) {
        return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    }

    Utils.prototype.replaceAll = function(string, find, replace) {
        return string.replace(new RegExp(escapeRegExp(find), "g"), replace);
    }

    Utils.prototype.getFormattedPlaybackRate = function(rate) {
    return (rate * 100) + "%";
    }

    /**
     * Returns the input time in milliseconds
     *
     * @param data data in the format ab:cd:ef
     * @return time from the data in milliseconds
     */
    Utils.prototype.getTimeInMilliseconds = function(data) {
        if ((data != undefined) && (data != null) && (data != 0) && (data.length) && (data.indexOf(":") != -1)) {
            var values = data.split(":");
            // when the format is correct
            if (values.length == 3) {
                // try to convert to numbers
                var val0 = values[0] * 1;
                var val1 = values[1] * 1;
                var val2 = values[2] * 1;
                // check and parse the seconds
                if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2)) {
                    // convert hours, minutes and seconds to milliseconds
                    val0 *= 60 * 60 * 1000; // 1 hour = 60 minutes = 60 * 60 Seconds = 60 * 60 * 1000 milliseconds
                    val1 *= 60 * 1000; // 1 minute = 60 seconds = 60 * 1000 milliseconds
                    val2 *= 1000; // 1 second = 1000 milliseconds
                    return val0 + val1 + val2;
                }
            }
        }
        return 0;
    }

    /**
     * Returns the formatted seconds
     *
     * @param seconds seconds to format
     * @return formatted seconds
     */
    Utils.prototype.formatSeconds = function(seconds) {
        if (!seconds) {
            seconds = 0;
        }
        seconds = (seconds < 0) ? 0 : seconds;
        var result = "";
        if (parseInt(seconds / 3600) < 10) {
            result += "0";
        }
        result += parseInt(seconds / 3600);
        result += ":";
        if ((parseInt(seconds / 60) - parseInt(seconds / 3600) * 60) < 10) {
            result += "0";
        }
        result += parseInt(seconds / 60) - parseInt(seconds / 3600) * 60;
        result += ":";
        if (seconds % 60 < 10) {
            result += "0";
        }
        result += seconds % 60;
        if (result.indexOf(".") != -1) {
            result = result.substring(0, result.lastIndexOf(".")); // get rid of the .ms
        }
        return result;
    }

    /**
     * enable
     *
     * @param id
     */
    Utils.prototype.enable = function(id) {
        $("#" + id).removeAttr("disabled");
    }

    /**
     * disable
     *
     * @param id
     */
    Utils.prototype.disable = function(id) {
        $("#" + id).attr("disabled", "disabled");
    }

    Utils.prototype.removeParentIfElementExists = function(elemenId) {
        if ($("#" + elemenId) && $("#" + elemenId).parent()) {
            $("#" + elemenId).parent().remove();
        }
    }

    /**
     * greyIn
     *
     * @param id
     */
    Utils.prototype.greyIn = function(id) {
        $("#" + id).animate({
            opacity: 1.0
        });
    }

    /**
     * greyOut
     *
     * @param id
     */
    Utils.prototype.greyOut = function(id) {
        $("#" + id).animate({
            opacity: 0.5
        });
    }
    
    Utils.prototype.repairSegmentLength = function(segments, duration, min_segment_duration) {
        if (segments && duration) {
            var total = 0;
            var result = new Array();
            for (var i = 0; i < segments.length; i++) {
              if (segments[i].time < parseInt(duration)) {
                if (segments[i].duration) {
                    total += parseInt(segments[i].duration);
                    if (parseInt(segments[i].duration) < min_segment_duration) {
                        if (result.length === 0) {
                          result.push(segments[i]);
                        } else {
                          result[result.length - 1].duration = parseInt(result[result.length - 1].duration) + 
                                  parseInt(segments[i].duration);
                        }
                    } else {
                      result.push(segments[i]);
                    }
                }
              }
            }
            
            if (total > parseInt(duration)) {
                var diff = total - parseInt(duration);
                for (var i = result.length - 1; i >= 0; i-- ) {
                    if (parseInt(result[i].duration) > diff) {
                        result[i].duration = parseInt(result[i].duration) - diff;
                        break;
                    }
                }
            }
            if (total < parseInt(duration)) {
                var diff = parseInt(duration) - total;
                if (result[result.length - 1]) {
                    result[result.length - 1].duration = parseInt(result[result.length - 1].duration) + diff;
                }
            }
        }
        return result;
    }

    /**
     * get starttime next segment and 0 if the last segment has been reached
     *
     * @param id
     */
    Utils.prototype.nextSegmentStart = function(segments, currentTime) {
        for (var i = 0; i < segments.length; i++) {
            if (segments[i].time > currentTime * 1000) {
                return segments[i].time;
            }
        }
        return 0; // if currentTime is beyond last segment start
    }
    
        /**
     * get starttime next segment and 0 if the last segment has been reached
     *
     * @param id
     */
    Utils.prototype.previousSegmentStart = function(segments, currentTime) {
        for (var i = (segments.length - 1); i >= 0; i--) {
            // added limit that last segment can jump to previous segment and not only segment start
            if (segments[i].time < (currentTime * 1000) - 900) { 
                return segments[i].time;
            }
        }
        return 0; // jump only to the start
    }

    /**
     * Timer object, that can be renewed (to reset the delay).
     * @type {Object}
     */
    Utils.prototype.timer = {
        setup: function(callback, delay) {
            this.callback = function() {
                callback.call();
                this.timeoutID = undefined;
            }
            this.delay = delay;

            if (typeof this.timeoutID === "number") {
                  this.cancel();
            } else {
                this.timeoutID = window.setTimeout(this.callback.bind(this), this.delay);
            }
            return this;
        },

        renew: function() {
            window.clearTimeout(this.timeoutID);
            this.timeoutID = window.setTimeout(this.callback.bind(this), this.delay);
        },

        cancel: function() {
            window.clearTimeout(this.timeoutID);
            this.timeoutID = undefined;
        }
    };

    return Utils;
});
