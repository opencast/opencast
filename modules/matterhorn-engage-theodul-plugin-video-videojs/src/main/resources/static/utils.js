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

    function Utils() {
        // nothing to see here
    }

    Utils.prototype.detectLanguage = function() {
        var language = navigator.language || navigator.userLanguage || navigator.browserLanguage || navigator.systemLanguage || "en";
        return language.replace(/\-.*/,'');
    }

    Utils.prototype.extractFlavorMainType = function(flavor) {
        var types = flavor.split("/");
        if (types.length > 0) {
            return types[0];
        }
        return "presenter" // fallback value, should never be returned, but does no harm 
    }

    Utils.prototype.getFlavorForVideoDisplay = function(videoDisplay) {
        if (videoDisplay === undefined) {
            return;
        }
        var data = $(videoDisplay).data("videodisplay");
        if (data === undefined) {
            return;
        }
        var values = data.split("_");
        if (values.length === 3) {
            return (values[2]);
        }        
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

    Utils.prototype.parseVideoResolution = function(resolution) {
        var res = resolution.match(/(\d+)x(\d+)/);
        res[1] = parseInt(res[1]);
        res[2] = parseInt(res[2]);
        return res;
    }

    // parameter from Basil.get("preferredFormat")
    Utils.prototype.preferredFormat = function(preferredFormat) {
        if (preferredFormat == null) {
            return null;
        }
        switch (preferredFormat) {
            case "hls":
                return "application/x-mpegURL";
            case "dash":
                return "application/dash+xml";
            case "rtmp":
                return "rtmp/mp4";
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "audio":
                return "audio/";
            default:
                return null;
        }
        return null;
    }

    Utils.prototype.escapeRegExp = function(string) {
        return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    }

    Utils.prototype.replaceAll = function(string, find, replace) {
        return string.replace(new RegExp(this.escapeRegExp(find), "g"), replace);
    }



    /**
     * @description Returns a time in the URL time format, e.g. 30m10s
     * @param data Time in the format ab:cd:ef
     * @return data in the URl time format when data correct, -1 else
     */
    Utils.prototype.getURLTimeFormat = function(data) {
        if ((data !== undefined) && (data !== null) && (data != 0) && (data.length) && (data.indexOf(':') != -1)) {
            var values = data.split(':');
            if (values.length == 3) {
                var val0 = values[0] * 1;
                var val1 = values[1] * 1;
                var val2 = values[2] * 1;
                if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2)) {
                    var valMin = val0 * 60 + val1;
                    return valMin + "m" + val2 + "s";
                }
            }
        }
        return -1;
    }

    /**
     * @description parses seconds
     *
     * Format: Minutes and seconds:  XmYs    or    YsXm    or    XmY
     *         Minutes only:         Xm
     *         Seconds only:         Ys      or    Y
     *
     * @return parsed seconds if parsing was successful, 0 else
     */
    Utils.prototype.parseSeconds = function(val) {
        if ((val !== undefined) && !(val == "")) {
            if (!isNaN(val)) {
                return val;
            }
            var tmpVal = val + "";
            var min = -1;
            var sec = -1;
            var charArr = tmpVal.split("");
            var tmp = "";
            for (var i = 0; i < charArr.length; ++i) {
                // minutes suffix detected
                if (charArr[i] == "m") {
                    if (!isNaN(tmp)) {
                        min = parseInt(tmp);
                    } else {
                        min = 0;
                    }
                    tmp = "";
                }
                // seconds suffix detected
                else if (charArr[i] == "s") {
                    if (!isNaN(tmp)) {
                        sec = parseInt(tmp);
                    } else {
                        sec = 0;
                    }
                    tmp = "";
                }
                // any number detected
                else if (!isNaN(charArr[i])) {
                    tmp += charArr[i];
                }
            }
            if (min < 0) {
                min = 0;
            }
            if (sec < 0) {
                // seconds without 's' suffix
                if (tmp != "") {
                    if (!isNaN(tmp)) {
                        sec = parseInt(tmp);
                    } else {
                        sec = 0;
                    }
                } else {
                    sec = 0;
                }
            }
            var ret = min * 60 + sec;
            if (!isNaN(ret)) {
                return ret;
            }
        }
        return 0;
    }

    return Utils;
});
