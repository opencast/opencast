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

// Controller for the "edit scheduled events" wizard
angular.module('adminNg.controllers')
    .controller('EditEventsCtrl', ['$scope', 'Table', 'Notifications', 'EventBulkEditResource', 'SeriesResource', 'CaptureAgentsResource', 'EventsSchedulingResource', 'JsHelper', 'SchedulingHelperService', 'WizardHandler', 'Language', '$translate', 'decorateWithTableRowSelection','$timeout', 'Modal',
function ($scope, Table, Notifications, EventBulkEditResource, SeriesResource, CaptureAgentsResource, EventsSchedulingResource, JsHelper, SchedulingHelperService, WizardHandler, Language, $translate, decorateWithTableRowSelection, $timeout, Modal) {
    var me = this;
    var SCHEDULING_CONTEXT = 'event-scheduling';

    // Conflict checking should only be done and be enabled once we
    // get to the second wizard stage.
    // However, since we're checking conflicts "on change" for the
    // input fields in the form, we are inadvertantly checking them
    // once the list of capture agent arrives as well (since that
    // apparently constitutes a "change"). This happens way earlier
    // than the second wizard step, so we protect against early checks
    // here.
    $scope.conflictCheckingEnabled = false;
    $scope.rows = Table.copySelected();
    $scope.conflicts = [];
    $scope.eventSummaries = [];
    // by default, all rows are selected
    $scope.allSelected = true;
    $scope.test = false;
    $scope.currentForm = 'generalForm';
    /* Get the current client timezone */
    var tzOffset = (new Date()).getTimezoneOffset() / -60;
    $scope.tz = 'UTC' + (tzOffset < 0 ? '-' : '+') + tzOffset;

    // Get available series
    $scope.seriesResults = {};
    SeriesResource.query().$promise.then(function(results) {
        angular.forEach(results.rows, function(row) {
            $scope.seriesResults[row.title] = row.id;
        });
    });

    $scope.keyUp = function (event) {
        switch (event.keyCode) {
        case 27:
            $scope.close();
            break;
        }
    };

    $scope.close = function() {
        if (me.notificationConflict) {
            Notifications.remove(me.notificationConflict, SCHEDULING_CONTEXT);
        }
        Modal.$scope.close();
    };

    // Given a series id, get me the title (we need this for the summary prettification)
    var seriesTitleForId = function(id) {
        var result = null;
        angular.forEach($scope.seriesResults, function(value, key) {
            if (value === id) {
                result = key;
            }
        });
        return result;
    };

    // Get available capture agents
    $scope.captureAgents = [];
    CaptureAgentsResource.query({inputs: true}).$promise.then(function (data) {
        $scope.captureAgents = data.rows;
    });

    // Given a capture agent id, get me the whole capture agent from the list
    var getCaById = function(agentId) {
        if (agentId === null) {
            return null;
        }
        for (var i = 0; i < $scope.captureAgents.length; i++) {
            if ($scope.captureAgents[i].id === agentId) {
                return $scope.captureAgents[i];
            }
        }
        return null;
    };

    // Given an event id, get me the row
    var getRowForId = function(eventId) {
        for (var i = 0; i < $scope.rows.length; i++) {
            var row = $scope.rows[i];
            if (row.id === eventId) {
                return row;
            }
        }
        return null;
    };

    // Given an event id, get me the title
    var eventTitleForId = function(id) {
        return getRowForId(id).title;
    };

    // Iterate over all (selected) events (via the rows) and get the
    // specific metadata element. Returns the empty string if the
    // value is ambiguous between the rows.
    var getMetadataPart = function(getter) {
        var result = null;
        for (var i = 0; i < $scope.rows.length; i++) {
            var row = $scope.rows[i];
            if (!row.selected) {
                continue;
            }
            var val = getter(row);
            if (!angular.isDefined(val)) {
                val = null;
            }
            if (result === null) {
                result = val;
            } else if (result !== val) {
                return "";
            }
        }
        if (result === null) {
            return "";
        } else {
            return result;
        }
    };

    // Return if the event with the given ID is selected
    var isSelected = function(id) {
        return JsHelper.arrayContains($scope.getSelectedIds(), id);
    };

    // see "getMetadataPart", but this one is for scheduling information
    var getSchedulingPart = function(getter) {
        var result = { ambiguous: false, value: null };
        angular.forEach($scope.schedulingSingle, function(value) {
            if (!isSelected(value.eventId)) {
                return;
            }
            var val = getter(value);
            if (result.ambiguous === false) {
                if (result.value === null) {
                    result.value = val;
                } else if (result.value !== val) {
                    result.ambiguous = true;
                    result.value = null;
                }
            }
        });
        if (result.ambiguous === true) {
            return null;
        } else {
            return result.value;
        }
    };

    // Convert a Javascript, sunday-based weekday to the corresponding
    // OC weekday object
    var fromJsWeekday = function(d) {
        // Javascript week days start at sunday (so 0=SU), so we have to roll over.
        return JsHelper.getWeekDays()[(d + 6) % 7];
    };

    $scope.hours = JsHelper.initArray(24);
    $scope.minutes = JsHelper.initArray(60);
    $scope.weekdays = JsHelper.getWeekDays();

    // Get scheduling information for the events
    $scope.scheduling = {};
    $scope.schedulingSingle = EventsSchedulingResource.bulkGet({
            eventIds: JsHelper.mapFunction($scope.rows, function(v) { return v.id; }),
            ignoreNonScheduled: true
        });

    $scope.onTemporalValueChange = function(type) {
        SchedulingHelperService.applyTemporalValueChange($scope.scheduling, type, false);
    };

    this.clearConflicts = function () {
        $scope.conflicts = [];
        if (me.notificationConflict) {
            Notifications.remove(me.notificationConflict, SCHEDULING_CONTEXT);
            me.notificationConflict = undefined;
        }
    };

    this.conflictsDetected = function (response) {
        me.clearConflicts();
        if (response.status === 409) {
            me.notificationConflict = Notifications.add('error', 'CONFLICT_BULK_DETECTED', SCHEDULING_CONTEXT);
            angular.forEach(response.data, function (data) {
                angular.forEach(data.conflicts, function(conflict) {
                    $scope.conflicts.push({
                        eventId: eventTitleForId(data.eventId),
                        title: conflict.title,
                        start: Language.formatDateTime('medium', conflict.start),
                        end: Language.formatDateTime('medium', conflict.end)
                    });
                });
            });
        }
        $scope.checkingConflicts = false;
    };

    this.noConflictsDetected = function () {
        me.clearConflicts();
        $scope.checkingConflicts = false;
        $scope.generateEventSummariesAndContinue();
    };

    // What we send to the server is slightly different than what we
    // internally use for the forms. This function returns the
    // "cleaned up" result.
    var postprocessScheduling = function() {
        var scheduling = $.extend(true, {}, $scope.scheduling);
        JsHelper.removeNulls(scheduling);
        JsHelper.removeNulls(scheduling.start);
        JsHelper.removeNulls(scheduling.end);
        if ($.isEmptyObject(scheduling.start)) {
            delete scheduling.start;
        }
        if ($.isEmptyObject(scheduling.end)) {
            delete scheduling.end;
        }
        if (angular.isDefined(scheduling.location)) {
            scheduling.agentId = scheduling.location.id;
            delete scheduling.location;
        }
        delete scheduling.duration;
        return scheduling;
    };

    $scope.checkConflicts = function () {
        if ($scope.conflictCheckingEnabled === false) {
            return;
        }
        $scope.checkingConflicts = true;
        var payload = {
            events: $scope.getSelectedIds(),
            scheduling: postprocessScheduling()
        };
        EventBulkEditResource.conflicts(payload, me.noConflictsDetected, me.conflictsDetected);
    };

    $scope.checkingConflicts = false;

    var nextWizardStep = function() {
        WizardHandler.wizard("editEventsWz").next();
    };

    var getterForMetadata = function(metadataId) {
        switch (metadataId) {
        case 'title':
            return function(row) { return row.title; };
        case 'isPartOf':
            return function(row) { return row.series_id; };
        default:
            console.log('There is no getter for metadata item "'+metadataId+'"');
        }
    };

    var prettifyMetadata = function(metadataId, value) {
        if (value === null) {
            return value;
        } else if (metadataId === 'isPartOf') {
            return seriesTitleForId(value);
        } else {
            return value;
        }
    };


    // This is triggered after the user selected some events in the first wizard step
    $scope.clearFormAndContinue = function() {
        $scope.conflictCheckingEnabled = true;
        $scope.metadataRows = [
            {
                id: "title",
                label: "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
                readOnly: false,
                required: false,
                type: "text",
                value: getMetadataPart(getterForMetadata('title'))
            },
            {
                id: "isPartOf",
                collection: $scope.seriesResults,
                label: "EVENTS.EVENTS.DETAILS.METADATA.SERIES",
                readOnly: false,
                required: false,
                translatable: false,
                type: "text",
                value: getMetadataPart(getterForMetadata('isPartOf'))
            },
        ];
        $scope.scheduling = {
            timezone: JsHelper.getTimeZoneName(),
            location: getCaById(getMetadataPart(function(row) { return row.agent_id; })),
            start: {
                hour: getSchedulingPart(function(entry) { return entry.start.hour; }),
                minute: getSchedulingPart(function(entry) { return entry.start.minute; })
            },
            end: {
                hour: getSchedulingPart(function(entry) { return entry.end.hour; }),
                minute: getSchedulingPart(function(entry) { return entry.end.minute; })
            },
            duration: {
                hour: getSchedulingPart(function(entry) { return entry.duration.hour; }),
                minute: getSchedulingPart(function(entry) { return entry.duration.minute; })
            },
            weekday: getSchedulingPart(function(entry) { return fromJsWeekday(new Date(entry.start.date).getDay()).key; })
        };
        nextWizardStep();
    };

    $scope.metadataRows = [];

    $scope.saveField = function(arg, callback) {
        // Nothing to do here yet, but the "save" attribute is non-optional.
    };

    $scope.valid = function () {
        return $scope.getSelectedIds().length > 0;
    };

    $scope.nonSchedule = function(row) {
        return row.source !== 'SCHEDULE';
    };

    $scope.nonScheduleSelected = function() {
        return JsHelper.filter($scope.getSelected(),function (r) { return r.source !== 'SCHEDULE'; }).length > 0;
    };

    $scope.rowsValid = function() {
        return !$scope.nonScheduleSelected() && $scope.hasAnySelected() && $scope.hasAllAgentsAccess();
    };

    $scope.generateEventSummariesAndContinue = function() {
        $scope.eventSummaries = [];
        angular.forEach($scope.schedulingSingle, function(value) {
            if (!isSelected(value.eventId)) {
                return;
            }

            var changes = [];

            if ($scope.scheduling.location !== null && $scope.scheduling.location.id !== null && $scope.scheduling.location.id !== value.agentId) {
                changes.push({
                    type: 'EVENTS.EVENTS.TABLE.LOCATION',
                    previous: value.agentId,
                    next: $scope.scheduling.location.id
                });
            }

            var valueWeekDay = fromJsWeekday(new Date(value.start.date).getDay());
            if ($scope.scheduling.weekday !== null && valueWeekDay.key !== $scope.scheduling.weekday) {
                changes.push({
                    type: 'EVENTS.EVENTS.TABLE.WEEKDAY',
                    // Might be better to actually use the promise rather than using instant,
                    // but it's difficult with the two-way binding here.
                    previous: $translate.instant(valueWeekDay.translationLong),
                    next: $translate.instant(JsHelper.weekdayTranslation($scope.scheduling.weekday, true))
                });
            }

            var row = getRowForId(value.eventId);
            angular.forEach($scope.metadataRows, function(metadata) {
                var rowValue = getterForMetadata(metadata.id)(row);
                if (!angular.isDefined(rowValue)) {
                    rowValue = "";
                }
                // This is an very subtle hack to circumvent MH-12876,
                // so I'll have to explain: Normally, we could just
                // test if "metadata.value" is equal to "rowValue",
                // and if so, there's no difference for that field.
                // Done...
                //
                // ...However, there are drop-downs. "Series" is a
                // drop-down, for example. And an event might have no
                // series assigned to it, so the drop-down is
                // "unselected". Now, when the form with the
                // unselected series drop-down (i.e. its value set to
                // the empty string) is constructed, it automatically
                // resets its value to the first series available. And
                // here's the hack: This first "mis-assignment"
                // doesn't set the value to the series ID. Instead, it
                // assigns itself a whole object, with label and
                // id. This we can test and then assume the value is
                // just "null". If you then change the drop-down,
                // we'll get a string and not an object.
                // Phew...
                // Better leave this as a separate "if" as a reminder
                // and for easy removal later.
                if (typeof metadata.value === 'object') {
                    return;
                }
                if (metadata.value !== "" && metadata.value !== rowValue) {
                    var prettyRow = prettifyMetadata(metadata.id, rowValue);
                    var prettyMeta = prettifyMetadata(metadata.id, metadata.value);
                    changes.push({
                        type: metadata.label,
                        previous: prettyRow,
                        next: prettyMeta
                    });
                }
            });

            var formatTimeHourMinute = function (date, hour, minute) {
                // This somewhat deliberately ignores the time zone,
                // since it's only for display.
                var jsDate = new Date(
                    parseInt(date.substring(0, 4)),
                    parseInt(date.substring(5,7))-1,
                    parseInt(date.substring(8,10)),
                    hour,
                    minute);
                return Language.formatTime('short', jsDate.toISOString());
            };

            // Little helper function so we can treat "start" and "end" the same.
            var formatPart = function(schedObj, valObj, translation) {
                if (schedObj.hour !== null && schedObj.hour !== valObj.hour || schedObj.minute !== null && schedObj.minute !== valObj.minute) {
                    var oldTime = formatTimeHourMinute(valObj.date, valObj.hour, valObj.minute);
                    var newHour = valObj.hour;
                    if (schedObj.hour !== null && schedObj.hour !== valObj.hour) {
                        newHour = schedObj.hour;
                    }
                    var newMinute = valObj.minute;
                    if (schedObj.minute !== null && schedObj.minute !== valObj.minute) {
                        newMinute = schedObj.minute;
                    }
                    var newTime = formatTimeHourMinute(valObj.date, newHour, newMinute);
                    changes.push({
                        type: 'EVENTS.EVENTS.TABLE.'+translation,
                        previous: oldTime,
                        next: newTime
                    });
                }
            };

            formatPart($scope.scheduling.start, value.start, 'START');
            formatPart($scope.scheduling.end, value.end, 'END');

            if (changes.length > 0) {
                $scope.eventSummaries.push({
                    title: row.title,
                    changes: changes
                });
            }
        });
        $timeout(function() {
            nextWizardStep();
        });
    };

    $scope.noChanges = function() {
        return $scope.eventSummaries.length === 0;
    };

    $scope.hasConflicts = function() {
        return $scope.conflicts.length > 0;
    };

    var onSuccess = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('success', 'EVENTS_UPDATED_ALL');
        Table.deselectAll();
    };

    var onFailure = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('error', 'EVENTS_NOT_UPDATED', 'global', -1);
    };

    $scope.nextButtonText = function() {
        if ($scope.checkingConflicts) {
            return 'BULK_ACTIONS.EDIT_EVENTS.CONFLICT_CHECK_RUNNING';
        } else {
            return 'WIZARD.NEXT_STEP';
        }
    };

    $scope.submitButton = false;
    $scope.submit = function () {
        $scope.submitButton = true;
        var payload = {
            metadata: {
                flavor: "dublincore/episode",
                title: "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE",
                fields: JsHelper.filter(
                    $scope.metadataRows,
                    function(row) {
                        // Search for "hack" in this file for an explanation of this typeof magic.
                        return angular.isDefined(row.value) && row.value !== null && typeof row.value !== 'object' && row.value !== '';
                    })
            },
            scheduling: postprocessScheduling(),
            events: $scope.getSelectedIds()
        };
        if ($scope.valid()) {
            EventBulkEditResource.update(payload, onSuccess, onFailure);
        }
    };

    $scope.hasAgentAccess = function (agent, index, array) {
        return SchedulingHelperService.hasAgentAccess(agent.id);
    };

    $scope.noAgentAccess = function (row) {
        return !$scope.nonSchedule(row) && !SchedulingHelperService.hasAgentAccess(row.agent_id);
    };

    $scope.hasAllAgentsAccess = function () {
        for (var i = 0; i < $scope.rows.length; i++) {
            var row = $scope.rows[i];
            if (!row.selected || $scope.nonSchedule(row)) {
                continue;
            }
            if (!SchedulingHelperService.hasAgentAccess(row.agent_id)) {
                return false;
            }
        }
        return true;
    };

    decorateWithTableRowSelection($scope);
}]);
