angular.module('adminNg.services')
.factory('NewEventSource', ['JsHelper', 'CaptureAgentsResource', 'ConflictCheckResource', 'Notifications', 'Language', '$translate',
    function (JsHelper, CaptureAgentsResource, ConflictCheckResource, Notifications, Language, $translate) {
    var Source = function () {
        var me = this,
            NOTIFICATION_CONTEXT = 'events-form';


        /* Get the current client timezone */
        var tzOffset = (new Date()).getTimezoneOffset() / -60;
        me.tz = 'UTC' + (tzOffset < 0 ? '' : '+') + tzOffset;

        CaptureAgentsResource.query({inputs: true}).$promise.then(function (data) {
            me.captureAgents = data.rows;
        });

        this.ud = {
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
            }
        };

        this.weekdays = {
            'MO': 'EVENTS.EVENTS.NEW.WEEKDAYS.MO',
            'TU': 'EVENTS.EVENTS.NEW.WEEKDAYS.TU',
            'WE': 'EVENTS.EVENTS.NEW.WEEKDAYS.WE',
            'TH': 'EVENTS.EVENTS.NEW.WEEKDAYS.TH',
            'FR': 'EVENTS.EVENTS.NEW.WEEKDAYS.FR',
            'SA': 'EVENTS.EVENTS.NEW.WEEKDAYS.SA',
            'SU': 'EVENTS.EVENTS.NEW.WEEKDAYS.SU'
        };

        this.sortedWeekdays = [];

        angular.forEach(me.weekdays, function (day, index) {
            me.sortedWeekdays.push({
                key: index,
                translation: day
            });
        });

        this.conflicts = [];

        /*
         * Creates an array containing the numberOfElements, starting from zero.
         */
        this.initArray = function (numberOfElements) {
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
        };

        this.hours = this.initArray(24);
        this.minutes = this.initArray(60);

        this.roomChanged = function () {
            me.ud[me.ud.type].device.inputMethods = {};
        };

        this.toggleWeekday = function (weekday) {
            var wds = me.ud.SCHEDULE_MULTIPLE.weekdays;
            if (wds[weekday]) {
                wds[weekday] = false;
            } else {
                wds[weekday] = true;
            }
        };

        /*
         * Some internal utilities.
         */
        this.isSingleSectionValid = function () {
            var required = ['SCHEDULE_SINGLE.start.date', 'SCHEDULE_SINGLE.start.hour', 'SCHEDULE_SINGLE.start.minute',
                'SCHEDULE_SINGLE.duration.hour', 'SCHEDULE_SINGLE.duration.minute', 'SCHEDULE_SINGLE.device.name'];
            if (me.userdataHasAllValues(required)) {
                return true;
            }
            return false;
        };
        this.isMultipleSectionValid = function () {
            var required = ['SCHEDULE_MULTIPLE.start.date', 'SCHEDULE_MULTIPLE.start.hour', 'SCHEDULE_MULTIPLE.start.minute',
                'SCHEDULE_MULTIPLE.duration.hour', 'SCHEDULE_MULTIPLE.duration.minute', 'SCHEDULE_MULTIPLE.device.name'];
            if (me.userdataHasAllValues(required) &&
                    me.atLeastOneRepetitionDayMarked()) {
                return true;
            }
            return false;
        };
        this.userdataHasAllValues = function (required) {
            var result = true;
            angular.forEach(required, function (item) {
                var nestedObject = JsHelper.getNested(me.ud, item);
                if (nestedObject === null || angular.isUndefined(nestedObject)) {
                    result = false;
                }
            });
            return result;
        };
        this.atLeastOneInputMethodDefined = function () {
            var result = false, inputMethods;
            inputMethods = me.ud.type === 'SCHEDULE_SINGLE' ? me.ud.SCHEDULE_SINGLE.device.inputMethods : me.ud.SCHEDULE_MULTIPLE.device.inputMethods;
            angular.forEach(inputMethods, function (value) {
                if (value === true) {
                    result = true;
                    return;
                }
            });
            return result;
        };
        this.atLeastOneRepetitionDayMarked = function () {
            var result = false;
            angular.forEach(me.ud.SCHEDULE_MULTIPLE.weekdays, function (weekday) {
                if (weekday) {
                    result = true;
                }
            });
            return result;
        };

        this.readyToPollConflicts = function () {
            var data = me.ud[me.ud.type], result;
            result = angular.isDefined(data) && angular.isDefined(data.start) &&
                angular.isDefined(data.start.date) && data.start.date.length > 0 &&
                angular.isDefined(data.device) &&
                angular.isDefined(data.device.id) && data.device.id.length > 0;

            if (me.ud.type === 'SCHEDULE_MULTIPLE' && result) {
                result = angular.isDefined(data.end) &&
                    data.end.length > 0 && angular.isDefined(data.duration) &&
                    angular.isDefined(data.duration.hour) && angular.isDefined(data.duration.minute) &&
                    me.atLeastOneRepetitionDayMarked();
            }

            return result;
        };

        this.checkConflicts = function () {
            if (me.readyToPollConflicts()) {
                ConflictCheckResource.check(me.ud[me.ud.type], me.noConflictsDetected, me.conflictsDetected);
                me.updateWeekdays();
            }
        };

        /**
         * Update the presentation fo the weekdays for the summary
         */
        this.updateWeekdays = function () {
            var keyWeekdays = [],
                keysOrder = [],
                sortDay = function (day1, day2) {
                    return keysOrder[day1] - keysOrder[day2];
                };

            angular.forEach(me.sortedWeekdays, function (day, idx) {
                keysOrder[day.translation] = idx;
            });
            
            if (me.ud.type === 'SCHEDULE_MULTIPLE') {
                angular.forEach(me.ud.SCHEDULE_MULTIPLE.weekdays, function (weekday, index) {
                    if (weekday) {
                        keyWeekdays.push(me.weekdays[index]);                    
                    }
                 });
            }

            keyWeekdays.sort(sortDay);

            $translate(keyWeekdays).then(function (translations) {
                var translatedWeekdays = [];

                angular.forEach(translations, function(t) {
                    translatedWeekdays.push(t);
                });

                me.ud.SCHEDULE_MULTIPLE.presentableWeekdays = translatedWeekdays.join(',');
            });
        };

        this.changeType = function () {
            me.reset();
        };

        this.noConflictsDetected = function () {
            while (me.conflicts.length > 0) {
                me.conflicts.pop();
            }
        };

        this.conflictsDetected = function (response) {
            if (response.status === 409) {
                if (me.notification) {
                    Notifications.remove(me.notification, NOTIFICATION_CONTEXT);
                }
                me.notification = Notifications.add('error', 'CONFLICT_DETECTED', NOTIFICATION_CONTEXT);
                var data = response.data;
                angular.forEach(data, function (d) {
                    var timeArr = d.time.trim().split(';');
                    me.conflicts.push({
                        title: d.title,
                        start: Language.toLocalTime(timeArr[0].substr(6, timeArr[0].length)),
                        end: Language.toLocalTime(timeArr[1].substr(5, timeArr[1].length))
                    });
                });
            }
        };

        this.getFormatedStartTime = function () {
            var time,
                hour,
                minute;

            if (me.ud.type !== 'UPLOAD') {
                hour = me.ud[me.ud.type].start.hour;
                minute = me.ud[me.ud.type].start.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = JsHelper.humanizeTime(hour, minute);
                }
            }

            return time;
        };        

        this.getFormatedDuration = function () {
            var time,
                hour,
                minute;

            if (me.ud.type !== 'UPLOAD') {
                hour = me.ud[me.ud.type].duration.hour;
                minute = me.ud[me.ud.type].duration.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = JsHelper.secondsToTime(((hour * 60) + minute) * 60);
                }
            }

            return time;
        };

        this.isValid = function () {

            if (me.ud.type === 'later') {
                return true; // no validation needed
            }
            else if (me.ud.type === 'UPLOAD') {
                return (angular.isDefined(me.ud.upload.segmentable) || angular.isDefined(me.ud.upload.nonSegmentable) || angular.isDefined(me.ud.upload.audioOnly));
            }
            else if (me.conflicts.length === 0) {
                if (me.ud.type === 'SCHEDULE_SINGLE') {
                    return me.isSingleSectionValid();
                }
                if (me.ud.type === 'SCHEDULE_MULTIPLE') {
                    return me.isMultipleSectionValid();
                }
            }
            return false;
        };

        this.reset = function () {
            me.ud.upload = {},
                
            me.ud.SCHEDULE_SINGLE = {
                device: {
                    inputMethods: {}
                }
            };
                
            me.ud.SCHEDULE_MULTIPLE = {
                device: {
                    inputMethods: {}
                },
                weekdays: {},
                presentableWeekdays: ''
            };
        };

        this.getUserEntries = function () {
            return me.ud;
        };
    };
    return new Source();
}]);
