/**
 *  Copyright 2009-2011 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

var Opencast = Opencast || {};

/**
 * Date helper module
 * @namespace Opencast
 * @class Date_Helper
 * @dependency moment.min.js
 */

Opencast.Date_Helper = (function(){
    var that = {};

    /**
     * @method formatSeconds
     * @param {Number} seconds
     * @return {String} formatted duration
     */

    that.formatSeconds = function(seconds)
    {
        var duration = (seconds > 0) ? moment.duration(seconds, 'seconds') : moment.duration(0, 'seconds');
        duration.hours = (duration.hours() < 10) ? "0"+duration.hours() : duration.hours();
        duration.minutes = (duration.minutes() < 10) ? "0"+duration.minutes() : duration.minutes();
        duration.seconds = (Math.round(duration.seconds()) < 10) ? "0"+Math.round(duration.seconds()) : Math.round(duration.seconds());
        return duration.hours+":"+duration.minutes+":"+duration.seconds;
    };

    /**
     * @method getDateString
     * @param {Date} date
     * @return {String} date string
     */

    that.getDateString = function(date)
    {
        return moment(date).format("ddd, MMM D YYYY");
    };

    /**
     * @method getTimeString
     * @param {Date} date
     * @return {String} time string
     */

    that.getTimeString = function(date)
    {
        return moment(date).format('HH:mm');
    };

    /**
     * @method dateStringToDate
     * @param {String} date string
     * @return {Date} date object
     */

    that.dateStringToDate = function(dcc)
    {
        return moment(dcc)._d;
    };

    return that;
})();
