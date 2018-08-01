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

'use strict';

// Provides general utility functions
angular.module('adminNg.services')
.factory('JsHelper', [
    function () {
        var weekdaysArray = [
            {
                key: 'MO',
                translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.MO',
                translationLong: 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.MO',
            },
            {
                key: 'TU',
                translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.TU',
                translationLong: 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.TU',
            },
            {
                key: 'WE',
                translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.WE',
                translationLong: 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.WE',
            },
            {
                key: 'TH',
                translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.TH',
                translationLong: 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.TH',
            },
            {
                key: 'FR',
                translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.FR',
                translationLong: 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.FR',
            },
            {
                key: 'SA',
                translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.SA',
                translationLong: 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.SA',
            },
            {
                key: 'SU',
                translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.SU',
                translationLong: 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.SU',
            }
        ];
        return {
            getWeekDays: function () {
                return weekdaysArray;
            },

            weekdayTranslation: function(d, longTranslation) {
                for (var i = 0; i < weekdaysArray.length; i++) {
                    if (weekdaysArray[i].key === d) {
                        if (longTranslation === true) {
                            return weekdaysArray[i].translationLong;
                        } else {
                            return weekdaysArray[i].translation;
                        }
                    }
                }
                return null;
            },

            filter: function(array, callback) {
                var result = [];
                angular.forEach(array, function(v) {
                    if (callback(v)) {
                        result.push(v);
                    }
                });
                return result;
            },

            removeNulls: function(obj) {
                var propNames = Object.getOwnPropertyNames(obj);
                for (var i = 0; i < propNames.length; i++) {
                    var propName = propNames[i];
                    if (obj[propName] === null || obj[propName] === undefined) {
                        delete obj[propName];
                    }
                }
            },

            arrayContains: function(array, v) {
                for(var i = 0; i < array.length; i++) {
                    if (array[i] === v) {
                        return true;
                    }
                }
                return false;
            },

            getTimeZoneName: function() {
                return Intl.DateTimeFormat().resolvedOptions().timeZone;
            },

            mapFunction: function(array, callback) {
                var result = [];
                angular.forEach(array, function(v) {
                    result.push(callback(v));
                });
                return result;
            },

            map: function (array, key) {
                var i, result = [];
                for (i = 0; i < array.length; i++) {
                    result.push(array[i][key]);
                }
                return result;
            },

            /*
             * Creates an array containing the numberOfElements, starting from zero.
             */
            initArray: function (numberOfElements) {
                var i, result = [];
                for (i = 0; i < numberOfElements; i++) {
                    if (i < 10) {
                        result.push({
                            index: i,
                            value: '0' + i
                        });
                    }
                    else {
                        result.push({
                            index: i,
                            value: '' + i
                        });
                    }
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
             * Opposite as method toZuluTimeString
             * @param  {String} date as iso string
             * @return {object}      Date object with date, hour, minute:
             *                            { 
             *                              date: '2014-07-08',
             *                              hour: '11',
             *                              minute: '33'
             *                            }
             */
            zuluTimeToDateObject: function (date) {
                    var hour = date.getHours(),
                        minute = date.getMinutes();

                    return {
                        date: $.datepicker.formatDate('yy-mm-dd', date),
                        minute: minute.length === 1 ? '0' + minute : minute,
                        hour: hour.length === 1 ? '0' + hour : hour
                    };
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
                var date,
                    weekdays = '',
                    weekdaysOffset = 0,
                    indexWeekdays = {},
                    weekdaysList = this.getWeekDays(),
                    dateParts = this.getDateParts(data.start);

                // Create a date object to translate it to UTC
                date = new Date(dateParts.year, dateParts.month, dateParts.day, data.start.hour, data.start.minute);

                if (data.weekdays) {
                    angular.forEach(weekdaysList, function(day, index) {
                        indexWeekdays[day.key] = index;
                    });

                    // Check if the weekdays need to be shifted because of timezone change
                    if (date.getUTCDate() > dateParts.day) {
                        weekdaysOffset = +1;
                    } else if (date.getUTCDate() < dateParts.day) {
                        weekdaysOffset = -1;
                    }

                    angular.forEach(data.weekdays, function (active, weekday) {
                        if (active) {
                            if (weekdays.length !== 0) {
                                weekdays += ',';
                            }
                            var idx = indexWeekdays[weekday.length > 2 ? weekday.substr(-2) : weekday] + weekdaysOffset;

                            // Check Sunday to Monday, Monday to Sunday
                            idx = (idx > 6 ? 0 : (idx < 0 ? 6 : idx));

                            weekdays += weekdaysList[idx].key.substr(-2);
                        }
                    });
                }

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
