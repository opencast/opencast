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

    Utils.prototype.detectLanguage = function() {
        var language = navigator.language || navigator.userLanguage || navigator.browserLanguage || navigator.systemLanguage || "en";
        return language.replace(/\-.*/,'');
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

    return Utils;
});
