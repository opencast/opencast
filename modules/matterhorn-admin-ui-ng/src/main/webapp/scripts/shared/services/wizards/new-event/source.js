angular.module('adminNg.services')
.factory('NewEventSource', ['JsHelper', 'CaptureAgentsResource', 'ConflictCheckResource', 'Notifications', 'Language', '$translate', 'underscore', '$timeout', 'localStorageService', 'AuthService',
    function (JsHelper, CaptureAgentsResource, ConflictCheckResource, Notifications, Language, $translate, _, $timeout, localStorageService, AuthService) {

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
            self.weekdays = _.clone(WEEKDAYS);
            self.ud = {
                upload: {},

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

                var singleKeys = ['duration', 'start', 'device'];                        //Only apply these default fields to SCHEDULE_SINGLE

                for (var key in opts) {
                    if (key === 'type') {
                      continue;
                    }

                    self.ud.SCHEDULE_MULTIPLE[key] = opts[key];

                    if (singleKeys.indexOf(key) > -1) {
                      self.ud.SCHEDULE_SINGLE[key] = opts[key];
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
             'device.name'];

        var getType = function() { return self.ud.type; };
        var isDefined = function(value) { return !(_.isUndefined(value) || _.isNull(value)); };
        var validators = {
            later: function() { return true; },
            UPLOAD: function() {
                return isDefined(self.ud.upload.segmentable) || isDefined(self.ud.upload.nonSegmentable) || isDefined(self.ud.upload.audioOnly);
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
                return angular.isDefined(data.end) &&
                    data.end.length > 0 &&
                    self.atLeastOneRepetitionDayMarked();
            } else {
                return result;
            }
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
                var dateArray = data.start.date.split("-");
                var startDate = new Date(dateArray[0], parseInt(dateArray[1]) - 1, dateArray[2], data.start.hour, data.start.minute);
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
                && angular.isDefined(data.end)) {
                var startDate = new Date(data.start.date);
                var endDate = new Date(data.end);
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

        this.getFormatedStartTime = function () {
            var time;

            if (!self.isUpload()) {
                var hour = self.ud[getType()].start.hour;
                var minute = self.ud[getType()].start.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = JsHelper.humanizeTime(hour, minute);
                }
            }

            return time;
        };

        this.getFormatedDuration = function () {
            var time;

            if (!self.isUpload()) {
                var hour = self.ud[getType()].duration.hour;
                var minute = self.ud[getType()].duration.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = JsHelper.secondsToTime(((hour * 60) + minute) * 60);
                }
            }

            return time;
        };

        var getValidatorByType = function() {
            if (_.has(validators, getType())) {
                return validators[getType()];
            }
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
                if (self.defaultsSet) {
                  return;
                }

                var defaults = {};
                AuthService.getUser().$promise.then(function (user) {
                    var orgProperties = user.org.properties;

                    //Variables needed to determine an event's start time
                    var startTime = orgProperties['admin.event.new.start_time'] || '08:00';
                    var endTime = orgProperties['admin.event.new.end_time'] || '20:00';
                    var durationMins = parseInt(orgProperties['admin.event.new.duration'] || 55);
                    var intervalMins = parseInt(orgProperties['admin.event.new.interval'] || 60);

                    var chosenSlot = moment( moment().format('YYYY-MM-DD') + ' ' + startTime );
                    var endSlot =  moment( moment().format('YYYY-MM-DD') + ' ' + endTime );
                    var dateNow = moment();
                    var timeDiff = dateNow.unix() - chosenSlot.unix();

                    //Find the next available timeslot for an event's start time
                    if (timeDiff > 0) {
                        var multiple = Math.ceil( timeDiff/(intervalMins * 60) );
                        chosenSlot.add(multiple * intervalMins, 'minute');
                        if (chosenSlot.unix() >= endSlot.unix()) {
                            chosenSlot = moment( moment().format('YYYY-MM-DD') + ' ' + startTime )
                                             .add(1, 'day');
                        }
                    }

                    defaults.start = {
                                         date: chosenSlot.format('YYYY-MM-DD'),
                                         hour: parseInt(chosenSlot.format('H')),
                                         minute: parseInt(chosenSlot.format('mm'))
                                     }

                    defaults.duration = {
                                            hour: parseInt(durationMins / 60),
                                            minute: durationMins % 60
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
    };
    return new Source();
}]);
