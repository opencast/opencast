/**
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

'use strict';

// Provides general utility functions
angular.module('adminNg.services')
.factory('JsHelper', [
    function () {
        return {
            map: function (array, key) {
                var i, result = [];
                for (i = 0; i < array.length; i++) {
                    result.push(array[i][key]);
                }
                return result;
            },

            /**
             * Finds nested properties in object literals in a graceful manner.
             * Usage: JsHelper.getNested(r, 'mediapackage', 'creators', 'creator');
             * @param r: The object which should contain the nested property.
             * all other params are the path to the wanted object.
             */
            getNested: function (targetObject, pathToProperty) {
                if (angular.isUndefined(targetObject)) {
                    throw 'illegal method call, I need at least two arguments';
                }
                var pathAsArray = pathToProperty.split('.'), i = 0, obj = targetObject;

                for (; i < pathAsArray.length; i++) {
                    if (!obj.hasOwnProperty(pathAsArray[i])) {
                        return null;
                    }
                    obj = obj[pathAsArray[i]];
                }
                return obj;
            },

            isEmpty: function (object) {
                var result = true;
                angular.forEach(object, function () {
                    result = false;
                    return;
                });
                return result;
            },

            /**
             * Checks if a potentially nested property exists in the targetObject.
             * Example: isPropertyDefined({inner: {property: 'prop'}}, 'inner.property') should return true.
             */
            isPropertyDefined: function (targetObject, pathToProperty) {
                var result = this.getNested(targetObject, pathToProperty);
                return (result !== null && angular.isDefined(result));
            },

            calculateStopTime: function (dateTimeString, duration) {
                if (!angular.isString(duration)) {
                    return '';
                }
                var d = new Date(dateTimeString),
                    unix = d.getTime(),
                    newUnix = unix + parseInt(duration, 10),
                    newDate = new Date(newUnix);
                return newDate.toISOString();
            },

            secondsToTime: function (duration) {
                var hours, minutes, seconds;
                hours   = Math.floor(duration / 3600);
                minutes = Math.floor((duration - (hours * 3600)) / 60);
                seconds = duration % 60;

                if (hours < 10)   { hours = '0' + hours; }
                if (minutes < 10) { minutes = '0' + minutes; }
                if (seconds < 10) { seconds = '0' + seconds; }

                return hours + ':' + minutes + ':' + seconds;
            },

            humanizeTime: function (hour, minute) {
                return moment().hour(hour).minute(minute).format('LT');
            },

            /**
             * Transform the UTC time string ('HH:mm') to a date object
             * @param  {String} utcTimeString the UTC time string
             * @return {Date}               the date object based on the string
             */
            parseUTCTimeString: function (utcTimeString) {
                    var dateUTC = new Date(0),
                        timeParts;

                    timeParts = utcTimeString.split(':');
                    if (timeParts.length === 2) {
                        dateUTC.setUTCHours(parseInt(timeParts[0]));
                        dateUTC.setUTCMinutes(parseInt(timeParts[1]));
                    }

                    return dateUTC;
            },

            /**
             * Converts obj to a Zulu Time String representation.
             *
             *  @param obj:
             *     { date: '2014-07-08',
             *       hour: '11',
             *       minute: '33'
             *     }
             *
             *  @param duration:
             *      Optionally, a duration { hour: '02', minute: '10' } can be added
             *      to the obj's time.
             */
            toZuluTimeString: function (obj, duration) {
                var momentDate,
                    dateParts = this.getDateParts(obj);
                    
                if (obj.hour) {
                    dateParts.hour = obj.hour;
                    dateParts.minute = obj.minute;
                }

                momentDate = moment(dateParts);

                if (duration) {
                    momentDate.add(parseInt(duration.hour, 10), 'h').add(parseInt(duration.minute, 10), 'm');
                }

                return momentDate.toISOString().replace('.000', '');
            },

            /**
             * Splits a string or object with a date member into its year, month and day parts.
             * Months lose 1 to keep it consistent with Javascript date object.
             *
             *  @param obj:
             *     { date: '2015-01-02' } or "2015-01-02"
             */
            getDateParts: function(obj) {
                var dateStr = obj,
                    dateParts;

                if (angular.isDefined(obj.date) ) {
                    dateStr = obj.date;
                }

                dateParts = dateStr.split('-');

                return {
                    year  : parseInt(dateParts[0], 10),
                    month : parseInt(dateParts[1], 10) - 1,
                    day   : parseInt(dateParts[2], 10)
                };
            },

            /**
             * Checks a string to see if it is empty or undefined.
             */
            stringIsEmpty: function(str) {
                return (!str || (angular.isString(str) && 0 === str.length));
            },

            /**
             * Assembles an iCalendar RRULE (repetition instruction) for the
             * given user input.
             */
            assembleRrule: function (data) {
                var weekdays = '';
                angular.forEach(data.weekdays, function (active, weekday) {
                    if (active) {
                        if (weekdays.length !== 0) {
                            weekdays += ',';
                        }
                        weekdays += weekday.substr(-2);
                    }
                });

                var date, dateParts;
                dateParts = this.getDateParts(data.start);
                // Create a date object to translate it to UTC
                date = new Date(dateParts.year, dateParts.month, dateParts.day, data.start.hour, data.start.minute);
                return 'FREQ=WEEKLY;BYDAY=' + weekdays +
                    ';BYHOUR=' + date.getUTCHours() +
                    ';BYMINUTE=' + date.getUTCMinutes();
            },

            replaceBooleanStrings: function (metadata) {
                angular.forEach(metadata, function (md) {
                    angular.forEach(md.fields, function (field) {
                        if (field.type === 'boolean') {
                            if (field.value === 'true') {
                                field.value = true;
                            }
                            else if (field.value === 'false') {
                                field.value = false;
                            }
                            else {
                                throw 'unknown boolean value';
                            }
                        }
                    });
                });
            }
        };
    }
]);
