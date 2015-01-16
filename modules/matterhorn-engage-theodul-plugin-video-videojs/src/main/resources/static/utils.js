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
        return navigator.language || navigator.userLanguage || navigator.browserLanguage || navigator.systemLanguage || "en";
    }

    Utils.prototype.extractFlavorMainType = function(flavor) {
        var types = flavor.split("/");
        if (types.length > 0) {
            return types[0];
        }
        return "presenter" // fallback value, should never be returned, but does no harm 
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
        /*
	  if(preferredFormat == null) {
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
	*/
        return null;
    }

    Utils.prototype.escapeRegExp = function(string) {
        return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    }

    Utils.prototype.replaceAll = function(string, find, replace) {
        return string.replace(new RegExp(this.escapeRegExp(find), "g"), replace);
    }

    return Utils;
});
