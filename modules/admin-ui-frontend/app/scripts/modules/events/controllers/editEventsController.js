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
  .controller('EditEventsCtrl', ['$scope', 'Table', 'Notifications', 'EventBulkEditResource', 'ResourcesListResource',
    'CaptureAgentsResource', 'EventsSchedulingResource', 'JsHelper', 'SchedulingHelperService', 'WizardHandler',
    'Language', '$translate', 'decorateWithTableRowSelection','$timeout', 'Modal',
    function ($scope, Table, Notifications, EventBulkEditResource, ResourcesListResource, CaptureAgentsResource,
      EventsSchedulingResource, JsHelper, SchedulingHelperService, WizardHandler, Language, $translate,
      decorateWithTableRowSelection, $timeout, Modal) {
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

      // Get available series (use SeriesListProvider here since request is currently much faster)
      $scope.seriesResults = {};

      $scope.seriesLoaded = ResourcesListResource.query({ resource: 'SERIES' }).$promise.then(function (series) {
        series.forEach(function(element) {
          $scope.seriesResults[element.value] = element.name;
        });
      }).catch(angular.noop);

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
        if (me.notificationInvalidDate) {
          Notifications.remove(me.notificationInvalidDate, SCHEDULING_CONTEXT);
          me.notificationInvalidDate = undefined;
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
      }).catch(angular.noop);

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

      // For a given event id, get its scheduling data
      var getSchedulingEvent = function(id) {
        for(var i = 0; i < $scope.schedulingSingle.length; i++) {
          var row = $scope.schedulingSingle[i];
          if (row.eventId === id) {
            return row;
          }
        }
        return null;

      };

      // Convert a Javascript, sunday-based weekday to the corresponding
      // OC weekday object
      var jsWeekdayToOcKey = function(d) {
        // Javascript week days start at sunday (so 0=SU), so we have to roll over.
        return JsHelper.getWeekDays()[(d + 6) % 7];
      };

      var getWeekdayString = function(date) {
        return jsWeekdayToOcKey(new Date(date).getDay()).key;
      };

      // Iterate over all (selected) events (via the rows) and get the
      // specific metadata element. Returns the empty string if the
      // value is ambiguous between the rows.
      // A weekday can be passed to filter specific weekdays
      var getMetadataPart = function(getter, weekday) {
        var result = null;
        for (var i = 0; i < $scope.rows.length; i++) {
          var row = $scope.rows[i];
          if (!row.selected) {
            continue;
          }
          if (!angular.isUndefined(weekday)) {
            // If we've got a weekday, we have to retrieve the event's start date from the scheduling
            // information
            var schedulingEvent = getSchedulingEvent(row.id);
            if (getWeekdayString(schedulingEvent.start.date) !== weekday) {
              continue;
            }
          }
          var val = getter(row);
          if (!angular.isDefined(val)) {
            val = null;
          }
          if (result === null) {
            result = val;
          } else if (result !== val) {
            return '';
          }
        }
        if (result === null) {
          return '';
        } else {
          return result;
        }
      };

      // Return if the event with the given ID is selected
      var isSelected = function(id) {
        return JsHelper.arrayContains($scope.getSelectedIds(), id);
      };

      // see "getMetadataPart", but this one is for scheduling information
      // A weekday can be passed in order to filter specific weekdays
      var getSchedulingPart = function(getter, weekday) {
        var result = { ambiguous: false, value: null };
        angular.forEach($scope.schedulingSingle, function(value) {
          if (!isSelected(value.eventId)) {
            return;
          }
          if (!angular.isUndefined(weekday) && getWeekdayString(value.start.date) !== weekday) {
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

      $scope.hours = JsHelper.initArray(24);
      $scope.minutes = JsHelper.initArray(60);
      $scope.weekdays = JsHelper.getWeekDays();

      // Get scheduling information for the events
      $scope.scheduling = {};
      $scope.schedulingSingle = EventsSchedulingResource.bulkGet({
        eventIds: JsHelper.mapFunction($scope.rows, function(v) { return v.id; }),
        ignoreNonScheduled: true
      });

      $scope.onTemporalValueChange = function(weekday, type) {
        SchedulingHelperService.applyTemporalValueChange($scope.scheduling[weekday], type, false);
        var startTime = $scope.scheduling[weekday].start.hour * 60 + $scope.scheduling[weekday].start.minute;
        var endTime = $scope.scheduling[weekday].end.hour * 60 + $scope.scheduling[weekday].end.minute;
        var conflictsBefore = $scope.hasInvalidDates();
        if (endTime < startTime) {
          $scope.invalidDates[weekday] = true;
          if (!conflictsBefore) {
            me.notificationInvalidDate = Notifications.add('error', 'CONFLICT_END_BEFORE_START', SCHEDULING_CONTEXT);
          }
        } else {
          $scope.invalidDates[weekday] = false;
        }
        if (!$scope.hasInvalidDates() && me.notificationInvalidDate) {
          Notifications.remove(me.notificationInvalidDate, SCHEDULING_CONTEXT);
          me.notificationInvalidDate = undefined;
        }
      };

      $scope.hasInvalidDates = function() {
        for (var i = 0; i < $scope.weekdays.length; i++) {
          if ($scope.invalidDates[$scope.weekdays[i].key]) {
            return true;
          }
        }
        return false;
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
      var postprocessScheduling = function(weekday) {
        var scheduling = $.extend(true, {}, $scope.scheduling[weekday]);
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

      var eventIdsForWeekday = function(weekday) {
        var result = [];
        angular.forEach($scope.schedulingSingle, function(value) {
          if (!isSelected(value.eventId)) {
            return;
          }
          if (getWeekdayString(value.start.date) === weekday) {
            result.push(value.eventId);
          }
        });
        return result;
      };

      $scope.checkConflicts = function () {
        if ($scope.conflictCheckingEnabled === false) {
          return;
        }
        return new Promise(function(resolve, reject) {
          $scope.checkingConflicts = true;
          var weekdays = $scope.validWeekdays();
          var payload = [];
          for (var i = 0; i < weekdays.length; i++) {
            var wd = weekdays[i];
            payload.push({
              events: eventIdsForWeekday(wd),
              scheduling: postprocessScheduling(wd)
            });
          }
          EventBulkEditResource.conflicts(payload, me.noConflictsDetected, me.conflictsDetected);
        });
      };

      $scope.checkingConflicts = false;
      $scope.invalidDates = {};

      $scope.nextWizardStep = function() {
        WizardHandler.wizard('editEventsWz').next();
      };

      var getterForMetadata = function(metadataId) {
        switch (metadataId) {
        case 'title':
          return function(row) { return row.title; };
        case 'isPartOf':
          return function(row) { return row.series_id; };
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
        if (me.notificationInvalidDate) {
          Notifications.remove(me.notificationInvalidDate, SCHEDULING_CONTEXT);
          me.notificationInvalidDate = undefined;
        }

        $scope.conflictCheckingEnabled = true;

        angular.forEach($scope.schedulingSingle, function(value) {
          if (!isSelected(value.eventId)) {
            return;
          }
          var weekday = getWeekdayString(value.start.date);
          if (weekday in $scope.scheduling) {
            return;
          }

          $scope.scheduling[weekday] = {
            timezone: JsHelper.getTimeZoneName(),
            location: getCaById(getMetadataPart(function(row) { return row.agent_id; }, weekday)),
            start: {
              hour: getSchedulingPart(function(entry) { return entry.start.hour; }, weekday),
              minute: getSchedulingPart(function(entry) { return entry.start.minute; }, weekday)
            },
            end: {
              hour: getSchedulingPart(function(entry) { return entry.end.hour; }, weekday),
              minute: getSchedulingPart(function(entry) { return entry.end.minute; }, weekday)
            },
            duration: {
              hour: getSchedulingPart(function(entry) { return entry.duration.hour; }, weekday),
              minute: getSchedulingPart(function(entry) { return entry.duration.minute; }, weekday)
            },
            weekday: weekday
          };
        });

        return $scope.seriesLoaded.then(function() {

          $scope.metadataRows = [
            {
              id: 'title',
              label: 'EVENTS.EVENTS.DETAILS.METADATA.TITLE',
              readOnly: false,
              required: false,
              type: 'text',
              value: getMetadataPart(getterForMetadata('title'))
            },
            {
              id: 'isPartOf',
              collection: $scope.seriesResults,
              label: 'EVENTS.EVENTS.DETAILS.METADATA.SERIES',
              readOnly: false,
              required: false,
              translatable: false,
              type: 'text',
              value: getMetadataPart(getterForMetadata('isPartOf'))
            }
          ];

          $scope.nextWizardStep();
        });
      };

      $scope.metadataRows = [];

      $scope.saveField = function() {
      // Nothing to do here yet, but the "save" attribute is non-optional.
      };

      $scope.valid = function () {
        return $scope.getSelectedIds().length > 0;
      };

      $scope.nonSchedule = function(row) {
        return row.event_status_raw !== 'EVENTS.EVENTS.STATUS.SCHEDULED';
      };

      $scope.nonScheduleSelected = function() {
        return JsHelper.filter($scope.getSelected(), $scope.nonSchedule).length > 0;
      };

      $scope.translateWeekdayLong = function(wd) {
        return $translate.instant(JsHelper.weekdayTranslation(wd, true));
      };

      $scope.numberOfEventsForWeekday = function(wd) {
        var result = 0;
        angular.forEach($scope.schedulingSingle, function(value) {
          if (!isSelected(value.eventId)) {
            return;
          }
          if (getWeekdayString(value.start.date) === wd) {
            result++;
          }
        });
        return result;
      };

      $scope.eventOrEvents = function(wd) {
        var key;
        if ($scope.numberOfEventsForWeekday(wd) === 1) {
          key = 'BULK_ACTIONS.EDIT_EVENTS.EDIT.EVENT';
        } else {
          key = 'BULK_ACTIONS.EDIT_EVENTS.EDIT.EVENTS';
        }
        return $translate.instant(key);
      };

      $scope.rowsValid = function() {
        return !$scope.nonScheduleSelected() && $scope.hasAnySelected() && $scope.hasAllAgentsAccess();
      };

      $scope.validWeekdays = function() {
        return Object.keys($scope.scheduling);
      };

      $scope.generateEventSummariesAndContinue = function() {
        $scope.eventSummaries = [];
        angular.forEach($scope.schedulingSingle, function(value) {
          if (!isSelected(value.eventId)) {
            return;
          }

          var changes = [];
          var jsDateBefore = new Date(value.start.date);
          var ocWeekdayStruct = jsWeekdayToOcKey(jsDateBefore.getDay());
          var scheduling = $scope.scheduling[ocWeekdayStruct.key];

          if (scheduling.location !== null &&
              scheduling.location.id !== null &&
              scheduling.location.id !== value.agentId) {
            changes.push({
              type: 'EVENTS.EVENTS.TABLE.LOCATION',
              previous: value.agentId,
              next: scheduling.location.id
            });
          }

          if (scheduling.weekday !== null && ocWeekdayStruct.key !== scheduling.weekday) {
            var dayOfWeekPrevious = $translate.instant(ocWeekdayStruct.translationLong);
            var datePrevious = Language.format('medium', jsDateBefore.toISOString(), 'date');
            var germanWeekdayNext = (JsHelper.weekdayByKey(scheduling.weekday).jsWeekday + 6) % 7;
            var germanWeekdayBefore = (jsDateBefore.getDay() + 6) % 7;
            var jsDateNext = new Date(jsDateBefore.getTime());
            jsDateNext.setHours(24 * (germanWeekdayNext - germanWeekdayBefore));
            var dayOfWeekNext = $translate.instant(JsHelper.weekdayTranslation( scheduling.weekday, true));
            var dateNext = Language.format('medium', jsDateNext.toISOString(), 'date');
            changes.push({
              type: 'EVENTS.EVENTS.TABLE.WEEKDAY',
              // Might be better to actually use the promise rather than using instant,
              // but it's difficult with the two-way binding here.
              previous: dayOfWeekPrevious + ', ' + datePrevious,
              next: dayOfWeekNext + ', ' + dateNext
            });
          }

          var row = getRowForId(value.eventId);
          angular.forEach($scope.metadataRows, function(metadata) {
            var rowValue = getterForMetadata(metadata.id)(row);
            if (!angular.isDefined(rowValue)) {
              rowValue = '';
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
            if (metadata.value !== '' && metadata.value !== rowValue) {
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
              parseInt(date.substring(5,7)) - 1,
              parseInt(date.substring(8,10)),
              hour,
              minute);
            return Language.formatTime('short', jsDate.toISOString());
          };

          // Little helper function so we can treat "start" and "end" the same.
          var formatPart = function(schedObj, valObj, translation) {
            if (schedObj.hour !== null
              && schedObj.hour !== valObj.hour
              || schedObj.minute !== null
              && schedObj.minute !== valObj.minute) {
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
                type: 'EVENTS.EVENTS.TABLE.' + translation,
                previous: oldTime,
                next: newTime
              });
            }
          };

          formatPart(scheduling.start, value.start, 'START');
          formatPart(scheduling.end, value.end, 'END');

          if (changes.length > 0) {
            $scope.eventSummaries.push({
              title: row.title,
              changes: changes
            });
          }
        });
        $timeout(function() {
          $scope.nextWizardStep();
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
        var metadata = {
          flavor: 'dublincore/episode',
          title: 'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
          fields: JsHelper.filter(
            $scope.metadataRows,
            function(row) {
              // Search for "hack" in this file for an explanation of this typeof magic.
              return angular.isDefined(row.value)
                && row.value !== null
                && typeof row.value !== 'object'
                && row.value !== '';
            })
        };
        var weekdays = $scope.validWeekdays();
        var payload = [];
        for (var i = 0; i < weekdays.length; i++) {
          var wd = weekdays[i];
          payload.push({
            events: eventIdsForWeekday(wd),
            metadata: metadata,
            scheduling: postprocessScheduling(wd)
          });
        }
        if ($scope.valid()) {
          EventBulkEditResource.update(payload, onSuccess, onFailure);
        }
      };

      $scope.hasAgentAccess = function (agent) {
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
