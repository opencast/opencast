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

angular.module('adminNg.services')
.factory('NewEventSource', ['JsHelper', 'CaptureAgentsResource', 'ConflictCheckResource', 'Notifications', 'Language', '$translate', '$filter', 'underscore', '$timeout', 'localStorageService', 'AuthService', 'SchedulingHelperService',
    function (JsHelper, CaptureAgentsResource, ConflictCheckResource, Notifications, Language, $translate, $filter, _, $timeout, localStorageService, AuthService, SchedulingHelperService) {

    // -- constants ------------------------------------------------------------------------------------------------- --

    var NOTIFICATION_CONTEXT = 'events-form';
    var SCHEDULE_SINGLE = 'SCHEDULE_SINGLE';
    var SCHEDULE_MULTIPLE = 'SCHEDULE_MULTIPLE';
    var WEEKDAY_PREFIX = 'EVENTS.EVENTS.NEW.WEEKDAYS.';
    var UPLOAD = 'UPLOAD';

    var WEEKDAYS = {
        'MO': WEEKDAY_PREFIX + 'MO',
        'TU': WEEKDAY_PREFIX + 'TU',
        'WE': WEEKDAY_PREFIX + 'WE',
        'TH': WEEKDAY_PREFIX + 'TH',
        'FR': WEEKDAY_PREFIX + 'FR',
        'SA': WEEKDAY_PREFIX + 'SA',
        'SU': WEEKDAY_PREFIX + 'SU'
    };

    // -- instance -------------------------------------------------------------------------------------------------- --

    var Source = function () {
        var self = this;

        this.save = function () {
            self.ud.UPLOAD.metadata['start'] = self.startDate;
        };

        this.createStartDate = function () {
            self.startDate = {
                "id": "startDate",
                "label": "EVENTS.EVENTS.DETAILS.METADATA.START_DATE",
                "value": new Date(Date.now()).toISOString(),
                "type": "date",
                "readOnly": false,
                "required": false,
                "tabindex": 7
            };
        };

        self.isSourceState = true;

        this.defaultsSet = false;

        this.checkingConflicts = false;
        this.hasConflicts = false;
        this.conflicts = [];
        this.hasConflictingSettings = function () {
            return self.hasConflicts;
        };

        /* Get the current client timezone */
        self.tzOffset = (new Date()).getTimezoneOffset() / -60;
        self.tz = 'UTC' + (self.tzOffset < 0 ? '' : '+') + self.tzOffset;

        this.loadCaptureAgents = function () {
            CaptureAgentsResource.query({inputs: true}).$promise.then(function (data) {
              self.captureAgents = data.rows;
            });
        };
        this.loadCaptureAgents();

        this.reset = function (opts) {

            self.createStartDate();
            self.weekdays = _.clone(WEEKDAYS);
            self.ud = {
                UPLOAD: {
                    tracks: {},
                    metadata: {
                        start: self.startDate
                    }
                },
                SCHEDULE_SINGLE: {
                    device: {
                        inputMethods: {}
                    }
                },

                SCHEDULE_MULTIPLE: {
                    device: {
                        inputMethods: {}
                    },
                    weekdays: {},
                    presentableWeekdays: ''
                },

                type: "UPLOAD"
            };

            if (opts) {
                if (opts.resetDefaults) {
                  self.defaultsSet = !opts.resetDefaults;
                  return;
                }

                var singleKeys = ['duration', 'start', 'end', 'device']; //Only apply these default fields to SCHEDULE_SINGLE

                for (var key in opts) {
                    if (key === 'type') {
                      continue;
                    }

                    self.ud.SCHEDULE_MULTIPLE[key] = angular.copy(opts[key]);

                    if (singleKeys.indexOf(key) > -1) {
                      self.ud.SCHEDULE_SINGLE[key] = angular.copy(opts[key]);
                    }
                }

                if (opts.presentableWeekdays) {
                    self.ud.SCHEDULE_MULTIPLE.weekdays[opts.presentableWeekdays.toUpperCase()] = true;
                }

                if (opts.type) {
                    self.ud.type = opts.type;
                }

                self.checkConflicts();
            }
        };
        this.reset();

        this.changeType = function() {
            localStorageService.set('sourceSticky', getType());
        }

        this.sortedWeekdays = _.map(self.weekdays, function(day, index) {
            return { key: index, translation: day };
        });

        this.hours = JsHelper.initArray(24);
        this.minutes = JsHelper.initArray(60);

        this.roomChanged = function () {
            self.ud[self.ud.type].device.inputMethods = {};

            self.ud[self.ud.type]
                .device.inputs.forEach(function(input) {
                    self.ud[self.ud.type].device.inputMethods[input.id] = true;
                });
        };

        this.toggleWeekday = function (weekday) {
            var weekdays = self.ud[SCHEDULE_MULTIPLE].weekdays;
            if (_.has(weekdays, weekday)) {
                weekdays[weekday] = !weekdays[weekday];
            }
        };

        /*
         * Some internal utilities.
         */
         var fields = [
             'start.date',
             'start.hour',
             'start.minute',
             'duration.hour',
             'duration.minute',
             'end.date',
             'end.hour',
             'end.minute',
             'device.name'];

        var getType = function() { return self.ud.type; };
        var isDefined = function(value) { return !(_.isUndefined(value) || _.isNull(value)); };
        var validators = {
            later: function() { return true; },
            UPLOAD: function() {
                // test for any type of upload source (MH-12085)
                return Object.keys(self.ud.UPLOAD.tracks).length > 0;
            },
            SCHEDULE_SINGLE: function () {
                return !self.hasConflicts && _.every(fields, function(field) {
                    var fieldValue = JsHelper.getNested(self.ud[SCHEDULE_SINGLE], field);
                    return isDefined(fieldValue);
                });
            },
            SCHEDULE_MULTIPLE: function() {
                var isAllFieldsDefined = _.every(fields, function(field) {
                    var fieldValue = JsHelper.getNested(self.ud[SCHEDULE_MULTIPLE], field);
                    return isDefined(fieldValue);
                });

                return !self.hasConflicts && isAllFieldsDefined && self.atLeastOneRepetitionDayMarked();
            }
        };

        this.isScheduleSingle = function() { return getType() === SCHEDULE_SINGLE; };
        this.isScheduleMultiple = function() { return getType() === SCHEDULE_MULTIPLE; };
        this.isUpload = function() { return getType() === UPLOAD; };

        this.atLeastOneInputMethodDefined = function () {
            var inputMethods = self.ud[getType()].device.inputMethods;
            return isDefined(_.find(inputMethods, function(inputMethod) { return inputMethod; }));
        };

        this.atLeastOneRepetitionDayMarked = function () {
            return isDefined(_.find(self.ud[SCHEDULE_MULTIPLE].weekdays, function(weekday) { return weekday; }));
        };

        this.canPollConflicts = function () {
            var data = self.ud[getType()];

            var result = isDefined(data) && isDefined(data.start) &&
                isDefined(data.start.date) && data.start.date.length > 0 &&
                angular.isDefined(data.duration) &&
                angular.isDefined(data.duration.hour) && angular.isDefined(data.duration.minute) &&
                isDefined(data.device) &&
                isDefined(data.device.id) && data.device.id.length > 0;

            if (self.isScheduleMultiple() && result) {
                return angular.isDefined(data.end.date) &&
                    data.end.date.length > 0 &&
                    self.atLeastOneRepetitionDayMarked();
            } else {
                return result;
            }
        };

        // Sort source select options by short title
        this.translatedSourceShortTitle = function(asset) {
            return $filter('translateOverrideFallback')(asset, 'SHORT');
        }

        // Create the data array for use in the summary view
        this.updateUploadTracksForSummary =  function () {
            self.ud.trackuploadlistforsummary = [];
            var namemap = self.wizard.sharedData.uploadNameMap;
            angular.forEach(self.ud.UPLOAD.tracks, function ( value, key) {
                var item = {};
                var fileNames = [];
                item.id = key;
                item.title = namemap[key].title;
                angular.forEach(value, function (file) {
                    fileNames.push(file.name);
                });
                item.filename =  fileNames.join(", ");
                item.type = namemap[key].type;
                item.flavor = namemap[key].flavorType + "/" + namemap[key].flavorSubType;
                self.ud.trackuploadlistforsummary.push(item);
            });
        };

        this.checkConflicts = function () {

            // -- semaphore ----------------------------------------------------------------------------------------- --

            var acquire = function() { return !self.checkingConflicts && self.canPollConflicts(); };

            var release = function(conflicts) {

              self.hasConflicts = _.size(conflicts) > 0;

              while (self.conflicts.length > 0) { // remove displayed conflicts, existing ones will be added again in
                self.conflicts.pop();             // the next step.
              }

              if (self.hasConflicts) {
                angular.forEach(conflicts, function (d) {
                    self.conflicts.push({
                        title: d.title,
                        start: Language.formatDateTime('medium', d.start),
                        end: Language.formatDateTime('medium', d.end)
                    });
                    console.log ("Conflict: " + d.title + " Start: " + d.start + " End:" + d.end);
                });
              }

              self.updateWeekdays();
              self.checkValidity();
            };

            // -- ajax ---------------------------------------------------------------------------------------------- --

            if (acquire()) {
//                Notifications.remove(self.notification, NOTIFICATION_CONTEXT);

                var onSuccess = function () {
                  if (self.notification) {
                    Notifications.remove(self.notification, NOTIFICATION_CONTEXT);
                    self.notification = undefined;
                  }
                  release();
                };
                var onError = function (response) {

                    if (response.status === 409) {
                        if (!self.notification) {
                            self.notification = Notifications.add('error', 'CONFLICT_DETECTED', NOTIFICATION_CONTEXT, -1);
                        }

                        release(response.data);
                    } else {
                        // todo show general error
                        release();
                    }
                };

                var settings = self.ud[getType()];
                ConflictCheckResource.check(settings, onSuccess, onError);
            }
        };

        this.getStartDate = function() {
            var start = self.ud[getType()].start;
            return SchedulingHelperService.parseDate(start.date, start.hour, start.minute);
        };

        this.checkValidity = function () {
            var data = self.ud[getType()];

            if (self.alreadyEndedNotification) {
                Notifications.remove(self.alreadyEndedNotification, NOTIFICATION_CONTEXT);
            }
            // check if start is in the past and has already ended
            if (angular.isDefined(data.start) && angular.isDefined(data.start.hour)
                && angular.isDefined(data.start.minute) && angular.isDefined(data.start.date)
                && angular.isDefined(data.duration) && angular.isDefined(data.duration.hour)
                && angular.isDefined(data.duration.minute)) {
                var startDate = self.getStartDate();
                var endDate = new Date(startDate.getTime());
                endDate.setHours(endDate.getHours() + data.duration.hour, endDate.getMinutes() + data.duration.minute, 0, 0);
                var nowDate = new Date();
                if (endDate < nowDate) {
                    self.alreadyEndedNotification = Notifications.add('error', 'CONFLICT_ALREADY_ENDED',
                        NOTIFICATION_CONTEXT, -1);
                    self.hasConflicts = true;
                }
            }

            if (self.endBeforeStartNotification) {
                Notifications.remove(self.endBeforeStartNotification, NOTIFICATION_CONTEXT);
            }
            // check if end date is before start date
            if (angular.isDefined(data.start) && angular.isDefined(data.start.date)
                && angular.isDefined(data.end.date)) {
                var startDate = new Date(data.start.date);
                var endDate = new Date(data.end.date);
                if (endDate < startDate) {
                    self.endBeforeStartNotification = Notifications.add('error', 'CONFLICT_END_BEFORE_START',
                        NOTIFICATION_CONTEXT, -1);
                    self.hasConflicts = true;
                }
            }
        };

        /**
         * Update the presentation fo the weekdays for the summary
         */
        this.updateWeekdays = function () {
            var keyWeekdays = [];
            var keysOrder = [];
            var sortDay = function (day1, day2) {
                    return keysOrder[day1] - keysOrder[day2];
                };

            angular.forEach(self.sortedWeekdays, function (day, idx) {
                keysOrder[day.translation] = idx;
            });

            if (self.isScheduleMultiple()) {
                angular.forEach(self.ud.SCHEDULE_MULTIPLE.weekdays, function (weekday, index) {
                    if (weekday) {
                        keyWeekdays.push(self.weekdays[index]);
                    }
                 });
            }

            keyWeekdays.sort(sortDay);

            $translate(keyWeekdays).then(function (translations) {
                var translatedWeekdays = [];

                angular.forEach(translations, function(t) {
                    translatedWeekdays.push(t);
                });

                self.ud[SCHEDULE_MULTIPLE].presentableWeekdays = translatedWeekdays.join(',');
            });
        };

        this.getFormattedStartTime = function () {
            var time;

            if (!self.isUpload()) {
                var hour = self.ud[getType()].start.hour;
                var minute = self.ud[getType()].start.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = moment().hour(hour).minute(minute).toISOString();
                }
            }

            return time;
        };

        this.getFormattedEndTime = function () {
            var time;

            if (!self.isUpload()) {
                var hour = self.ud[getType()].end.hour;
                var minute = self.ud[getType()].end.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = moment().hour(hour).minute(minute).toISOString();
                }
            }

            return time;
        };

        this.getFormattedDuration = function () {

            var time;

            if (!self.isUpload()) {
                var hours = self.ud[getType()].duration.hour;
                var minutes = self.ud[getType()].duration.minute;

                if (hours < 10)   { hours = '0' + hours; }
                if (minutes < 10) { minutes = '0' + minutes; }

                return hours + ':' + minutes;
            }

            return time;
        };

        var getValidatorByType = function() {
            if (_.has(validators, getType())) {
                return validators[getType()];
            }
        };

        // Update summary when exiting this step
        // One-time update prevents an infinite loop in
        // summary's ng-repeat.
        this.ud.trackuploadlistforsummary = [];
        this.getTrackUploadSummary = function() {
            return this.ud.trackuploadlistforsummary;
        }
        this.onExitStep = function () {
            // update summary of selections
            this.updateUploadTracksForSummary();
        };

        this.isValid = function () {
            var validator = getValidatorByType();
            if(isDefined(validator)) {
                return validator();
            }

            return false;
        };

        this.getUserEntries = function () {
            return self.ud;
        };

        this.setDefaultsIfNeeded = function() {
                if (self.defaultsSet || !self.hasAgents()) {
                  return;
                }

                var defaults = {};
                AuthService.getUser().$promise.then(function (user) {
                    var orgProperties = user.org.properties;

                    //Variables needed to determine an event's start time
                    var startTime = orgProperties['admin.event.new.start_time'] || '08:00';
                    var cutoffTime = orgProperties['admin.event.new.end_time'] || '20:00';
                    var durationMins = parseInt(orgProperties['admin.event.new.duration'] || 55);
                    var intervalMins = parseInt(orgProperties['admin.event.new.interval'] || 60);

                    var chosenSlot = moment( moment().format('YYYY-MM-DD') + ' ' + startTime );
                    var endSlot =  moment( moment().format('YYYY-MM-DD') + ' ' + cutoffTime );
                    var dateNow = moment();
                    var timeDiff = dateNow.unix() - chosenSlot.unix();

                    // Find the next available timeslot for an event's start time
                    if (timeDiff > 0) {
                        var multiple = Math.ceil( timeDiff/(intervalMins * 60) );
                        chosenSlot.add(multiple * intervalMins, 'minute');
                        if (chosenSlot.unix() >= endSlot.unix()) {
                          // The slot would start after the defined cutoff time (too late in the day), so we
                          // use the day's start time on tomorrow
                          chosenSlot = moment( moment().format('YYYY-MM-DD') + ' ' + startTime ).add(1, 'day');
                        }
                    }
                    var endDateTime = moment( chosenSlot ).add(durationMins, 'minutes');

                    defaults.start = {
                                         date: chosenSlot.format('YYYY-MM-DD'),
                                         hour: parseInt(chosenSlot.format('H')),
                                         minute: parseInt(chosenSlot.format('mm'))
                                     };

                    defaults.duration = {
                                            hour: parseInt(durationMins / 60),
                                            minute: durationMins % 60
                                        };

                    defaults.end = {
                                         date: endDateTime.format('YYYY-MM-DD'),
                                         hour: parseInt(endDateTime.format('H')),
                                         minute: parseInt(endDateTime.format('mm'))
                                     };

                    defaults.presentableWeekdays = chosenSlot.format('dd');

                    if (self.captureAgents.length === 0) {
                        //No capture agents, so user can only upload files
                        defaults.type = UPLOAD;
                    }
                    else if (localStorageService.get('sourceSticky')) {
                        //auto-select previously chosen source
                        defaults.type = localStorageService.get('sourceSticky');
                    }

                    self.reset(defaults);
                    self.defaultsSet = true;
                });
        };

        this.onTemporalValueChange = function(type) {
            SchedulingHelperService.applyTemporalValueChange(self.ud[getType()], type, self.isScheduleSingle() );
            self.checkConflicts();
        }

        this.hasAgentAccess = function(agent, index, array) {
            return SchedulingHelperService.hasAgentAccess(agent.id);
        };

        this.hasAgents = function() {
            return angular.isDefined(self.captureAgents)
                && self.captureAgents.filter(
                       function(agent) {
                           return self.hasAgentAccess(agent, undefined, undefined)
                       }).length > 0;
        };

    };
    return new Source();
}]);
